package com.learnsy.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnsy.admin.data.Lesson
import com.learnsy.admin.data.LessonFilter
import com.learnsy.admin.data.LessonRepository
import com.learnsy.admin.data.SortBy
import com.learnsy.admin.data.CardBlur
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LessonListUiState(
    val lessons: List<Lesson> = emptyList(),
    val loading: Boolean = false,
    val filter: LessonFilter = LessonFilter.ALL,
    val sortBy: SortBy = SortBy.NEWEST,
    val searchQuery: String = "",
    // Tương đương cardBlur trong app.jsx — làm mờ nội dung câu hỏi trên card
    // danh sách bài, dùng khi demo trước lớp để học sinh không đọc trộm đề.
    val cardBlur: CardBlur = CardBlur.OFF,
    val error: String? = null
)

// Tương đương phần load lessons + addLesson/deleteLesson/dupLesson trong app.jsx
class LessonListViewModel(
    private val repo: LessonRepository = LessonRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LessonListUiState())
    val uiState: StateFlow<LessonListUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.update { it.copy(loading = true) }
        viewModelScope.launch {
            try {
                val lessons = repo.fetchAll()
                _uiState.update { it.copy(lessons = lessons, loading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun setFilter(f: LessonFilter) = _uiState.update { it.copy(filter = f) }
    fun setSortBy(s: SortBy) = _uiState.update { it.copy(sortBy = s) }
    fun setSearchQuery(q: String) = _uiState.update { it.copy(searchQuery = q) }
    fun setCardBlur(b: CardBlur) = _uiState.update { it.copy(cardBlur = b) }

    // Trả về danh sách đã lọc + sắp xếp — tương đương chain filter().sort() trong JSX
    fun filteredSortedLessons(): List<Lesson> {
        val s = _uiState.value
        return s.lessons
            .filter { l ->
                when (s.filter) {
                    LessonFilter.ALL -> true
                    LessonFilter.ENGLISH -> l.subject == "Tiếng Anh"
                    LessonFilter.OTHER -> l.subject != "Tiếng Anh"
                }
            }
            .filter { l ->
                s.searchQuery.isBlank() ||
                    l.title.contains(s.searchQuery, ignoreCase = true) ||
                    l.subject.contains(s.searchQuery, ignoreCase = true)
            }
            .let { list ->
                when (s.sortBy) {
                    SortBy.NAME -> list.sortedWith(compareBy(java.text.Collator.getInstance(java.util.Locale("vi"))) { it.title })
                    SortBy.COUNT -> list.sortedByDescending { it.questions.size }
                    SortBy.OLDEST -> list.sortedBy { it.id }
                    SortBy.NEWEST -> list.sortedByDescending { it.id }
                }
            }
    }

    // Tương đương addLesson() — trả về id bài mới để caller điều hướng sang màn edit
    fun createLesson(onCreated: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val lesson = repo.create()
                _uiState.update { it.copy(lessons = it.lessons + lesson) }
                onCreated(lesson.id)
            } catch (e: Exception) {
                onError(e.message ?: "Không tạo được bài")
            }
        }
    }

    fun deleteLesson(id: String) {
        viewModelScope.launch {
            try {
                repo.delete(id)
                _uiState.update { it.copy(lessons = it.lessons.filter { l -> l.id != id }) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun duplicateLesson(lesson: Lesson, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val existingTitles = _uiState.value.lessons
                    .filter { it.id != lesson.id }
                    .map { it.title.trim().lowercase() }
                    .toSet()
                val uniqueTitle = repo.makeUniqueTitle("${lesson.title} (bản sao)", existingTitles)
                val dup = repo.duplicate(lesson, uniqueTitle)
                _uiState.update { it.copy(lessons = it.lessons + dup) }
                onDone(true)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
                onDone(false)
            }
        }
    }
}
