package com.learnsy.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnsy.admin.data.Lesson
import com.learnsy.admin.data.LessonRepository
import com.learnsy.admin.data.Question
import com.learnsy.admin.data.emptyTF
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SaveStatus { IDLE, PENDING, SAVING, SAVED, ERROR, DUP_BLOCKED }

data class LessonEditorUiState(
    val lessonId: String? = null,
    val title: String = "",
    val subject: String = "Tiếng Anh",
    val password: String = "",
    val timerLimit: Int = 0,
    val questions: List<Question> = listOf(emptyTF()),
    val titleDupWarn: Boolean = false,
    val saveStatus: SaveStatus = SaveStatus.IDLE,
    val lastError: String? = null
)

// Tương đương toàn bộ khối state + auto-save effect trong app.jsx (dòng ~272-540).
// Khác biệt chủ đích so với bản JS:
// - Không cần _isLoadingLesson/_isSaving ref-guard phức tạp: ViewModel chỉ có
//   1 lesson đang mở tại 1 thời điểm, load() luôn chạy xong trước khi user
//   gõ được gì (UI hiện loading), nên không có race condition kiểu React double-render.
// - Không cần retry loadRetryTick: load lesson bằng suspend fun trực tiếp từ Supabase,
//   không phụ thuộc lessonsRef đồng bộ từ list màn hình khác.
class LessonEditorViewModel(
    private val repo: LessonRepository = LessonRepository(),
    private val allLessonsProvider: () -> List<Lesson> = { emptyList() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(LessonEditorUiState())
    val uiState: StateFlow<LessonEditorUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null

    fun loadLesson(lesson: Lesson) {
        autoSaveJob?.cancel()
        _uiState.value = LessonEditorUiState(
            lessonId = lesson.id,
            title = lesson.title,
            subject = lesson.subject,
            password = lesson.password,
            timerLimit = lesson.timerLimit,
            questions = lesson.questions.ifEmpty { listOf(emptyTF()) }
        )
    }

    fun newLesson() {
        autoSaveJob?.cancel()
        _uiState.value = LessonEditorUiState()
    }

    fun setTitle(title: String) {
        _uiState.update { it.copy(title = title) }
        checkDup()
        scheduleAutoSave()
    }

    fun setSubject(subject: String) {
        _uiState.update { it.copy(subject = subject) }
        scheduleAutoSave()
    }

    fun setPassword(password: String) {
        _uiState.update { it.copy(password = password) }
        scheduleAutoSave()
    }

    fun setTimerLimit(minutes: Int) {
        _uiState.update { it.copy(timerLimit = minutes) }
        scheduleAutoSave()
    }

    fun setQuestions(questions: List<Question>) {
        _uiState.update { it.copy(questions = questions) }
        scheduleAutoSave()
    }

    fun addQuestion(q: Question) {
        _uiState.update { it.copy(questions = it.questions + q) }
        scheduleAutoSave()
    }

    // Tương đương manualSave() trong app.jsx — lưu ngay lập tức, bỏ qua debounce 800ms.
    // Dùng khi người dùng muốn chắc chắn bài đã lưu trước khi thoát (VD: chuẩn bị tắt app).
    fun manualSave() {
        val lessonId = _uiState.value.lessonId ?: return
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            val s = _uiState.value
            if (repo.isDuplicateTitle(s.title, s.lessonId, allLessonsProvider())) {
                _uiState.update { it.copy(saveStatus = SaveStatus.DUP_BLOCKED, lastError = "Tên bài tập bị trùng") }
                return@launch
            }
            _uiState.update { it.copy(saveStatus = SaveStatus.SAVING) }
            try {
                repo.save(
                    Lesson(
                        id = lessonId,
                        title = s.title,
                        subject = s.subject,
                        password = s.password,
                        timerLimit = s.timerLimit,
                        questions = s.questions
                    )
                )
                _uiState.update { it.copy(saveStatus = SaveStatus.SAVED, lastError = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(saveStatus = SaveStatus.ERROR, lastError = e.message) }
            }
        }
    }

    fun removeQuestion(id: String) {
        _uiState.update { it.copy(questions = it.questions.filter { q -> q.id != id }) }
        scheduleAutoSave()
    }

    // Tương đương check trùng tên real-time khi gõ title (effect titleDupWarn)
    private fun checkDup() {
        val s = _uiState.value
        val dup = repo.isDuplicateTitle(s.title, s.lessonId, allLessonsProvider())
        _uiState.update { it.copy(titleDupWarn = dup) }
    }

    // Tương đương debounce 800ms trước khi upsert lên Supabase
    private fun scheduleAutoSave() {
        val lessonId = _uiState.value.lessonId ?: return
        autoSaveJob?.cancel()
        _uiState.update { it.copy(saveStatus = SaveStatus.PENDING) }
        autoSaveJob = viewModelScope.launch {
            delay(800)

            val s = _uiState.value
            // Chặn lưu nếu tên trùng — không upsert lên Supabase
            if (repo.isDuplicateTitle(s.title, s.lessonId, allLessonsProvider())) {
                _uiState.update {
                    it.copy(saveStatus = SaveStatus.DUP_BLOCKED, lastError = "Tên bài tập bị trùng")
                }
                return@launch
            }

            _uiState.update { it.copy(saveStatus = SaveStatus.SAVING) }
            try {
                repo.save(
                    Lesson(
                        id = lessonId,
                        title = s.title,
                        subject = s.subject,
                        password = s.password,
                        timerLimit = s.timerLimit,
                        questions = s.questions
                    )
                )
                _uiState.update { it.copy(saveStatus = SaveStatus.SAVED, lastError = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(saveStatus = SaveStatus.ERROR, lastError = e.message) }
            }
        }
    }

    override fun onCleared() {
        autoSaveJob?.cancel()
    }
}
