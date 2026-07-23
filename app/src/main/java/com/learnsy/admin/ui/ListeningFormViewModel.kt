package com.learnsy.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnsy.admin.data.ListeningItem
import com.learnsy.admin.data.ListeningRepository
import com.learnsy.admin.data.ListeningStatement
import com.learnsy.admin.data.cleanListeningStr
import com.learnsy.admin.data.countBlanks
import com.learnsy.admin.data.genListeningId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class ListeningFormUiState(
    val editingId: String? = null,
    val text: String = "",
    val wordBox: List<String> = emptyList(),
    val shuffleWordBox: Boolean = false,
    val answers: List<String> = emptyList(),
    val statements: List<ListeningStatement> = emptyList(),
    val shuffleStatements: Boolean = false,
    val tags: List<String> = emptyList(),
    val saving: Boolean = false,
    val pendingMismatchConfirm: Boolean = false
)

// Tương đương phần form state trong ListeningManager (listening-panel.jsx)
class ListeningFormViewModel(
    private val repo: ListeningRepository = ListeningRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListeningFormUiState())
    val uiState: StateFlow<ListeningFormUiState> = _uiState.asStateFlow()

    fun resetForm() {
        _uiState.value = ListeningFormUiState()
    }

    fun openForEdit(item: ListeningItem) {
        _uiState.value = ListeningFormUiState(
            editingId = item.id,
            text = item.text,
            wordBox = item.wordBox,
            shuffleWordBox = item.shuffleWordBox,
            answers = item.answers,
            statements = item.statements,
            shuffleStatements = item.shuffleStatements,
            tags = item.tags
        )
    }

    fun setText(text: String) = _uiState.update { it.copy(text = text) }
    fun setShuffleWordBox(v: Boolean) = _uiState.update { it.copy(shuffleWordBox = v) }
    fun setShuffleStatements(v: Boolean) = _uiState.update { it.copy(shuffleStatements = v) }

    fun addWord(word: String, onDup: () -> Unit) {
        val w = word.trim()
        if (w.isEmpty()) return
        val s = _uiState.value
        if (s.wordBox.any { it.equals(w, ignoreCase = true) }) { onDup(); return }
        _uiState.update { it.copy(wordBox = it.wordBox + w) }
    }
    fun removeWord(index: Int) = _uiState.update { it.copy(wordBox = it.wordBox.filterIndexed { i, _ -> i != index }) }
    fun updateWord(index: Int, value: String) =
        _uiState.update { it.copy(wordBox = it.wordBox.mapIndexed { i, w -> if (i == index) value else w }) }

    fun addAnswer() = _uiState.update { it.copy(answers = it.answers + "") }
    fun updateAnswer(index: Int, value: String) =
        _uiState.update { it.copy(answers = it.answers.mapIndexed { i, a -> if (i == index) value else a }) }
    fun removeAnswer(index: Int) = _uiState.update { it.copy(answers = it.answers.filterIndexed { i, _ -> i != index }) }

    fun syncBlanksFromText(onResult: (Int) -> Unit) {
        val n = countBlanks(_uiState.value.text)
        if (n == 0) { onResult(0); return }
        _uiState.update { st ->
            val next = (0 until n).map { i -> st.answers.getOrElse(i) { "" } }
            st.copy(answers = next)
        }
        onResult(n)
    }

    fun suggestWordBoxFromAnswers(onResult: (Int) -> Unit) {
        val s = _uiState.value
        val newWords = s.answers.map { it.trim() }.filter { it.isNotEmpty() }
        if (newWords.isEmpty()) { onResult(-1); return }
        var added = 0
        val next = s.wordBox.toMutableList()
        newWords.forEach { w ->
            if (next.none { it.equals(w, ignoreCase = true) }) { next.add(w); added++ }
        }
        _uiState.update { it.copy(wordBox = next) }
        onResult(added)
    }

    fun addStatement() = _uiState.update { it.copy(statements = it.statements + ListeningStatement()) }
    fun updateStatementText(index: Int, text: String) =
        _uiState.update { it.copy(statements = it.statements.mapIndexed { i, s -> if (i == index) s.copy(statement = text) else s }) }
    fun updateStatementAnswer(index: Int, answer: String) =
        _uiState.update { it.copy(statements = it.statements.mapIndexed { i, s -> if (i == index) s.copy(answer = answer) else s }) }
    fun removeStatement(index: Int) =
        _uiState.update { it.copy(statements = it.statements.filterIndexed { i, _ -> i != index }) }

    fun moveStatement(index: Int, dir: Int) {
        val s = _uiState.value.statements.toMutableList()
        val j = index + dir
        if (j < 0 || j >= s.size) return
        val tmp = s[index]; s[index] = s[j]; s[j] = tmp
        _uiState.update { it.copy(statements = s) }
    }

    fun addTag(tag: String) {
        val t = tag.trim()
        if (t.isEmpty() || t in _uiState.value.tags) return
        _uiState.update { it.copy(tags = it.tags + t) }
    }
    fun removeTag(tag: String) = _uiState.update { it.copy(tags = it.tags.filter { x -> x != tag }) }

    fun requestSave(
        allItems: List<ListeningItem>,
        onDupText: () -> Unit,
        onEmptyText: () -> Unit,
        onMismatchConfirmNeeded: (blanks: Int, answers: Int) -> Unit,
        onSaved: (ListeningItem, isNew: Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        val s = _uiState.value
        if (s.saving) return
        val cleanText = cleanListeningStr(s.text)
        if (cleanText.isEmpty()) { onEmptyText(); return }

        if (repo.isDuplicateText(cleanText, s.editingId, allItems)) { onDupText(); return }

        val blankCount = countBlanks(cleanText)
        val cleanAnswers = s.answers.map { cleanListeningStr(it) }.filter { it.isNotEmpty() }

        if (blankCount > 0 && blankCount != cleanAnswers.size && !s.pendingMismatchConfirm) {
            onMismatchConfirmNeeded(blankCount, cleanAnswers.size)
            return
        }

        doSave(allItems, onSaved, onError)
    }

    fun confirmSaveAnyway(allItems: List<ListeningItem>, onSaved: (ListeningItem, isNew: Boolean) -> Unit, onError: (String) -> Unit) {
        doSave(allItems, onSaved, onError)
    }

    private fun doSave(
        allItems: List<ListeningItem>,
        onSaved: (ListeningItem, isNew: Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        val s = _uiState.value
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                val cleanText = cleanListeningStr(s.text)
                val cleanWordBox = s.wordBox.map { cleanListeningStr(it) }.filter { it.isNotEmpty() }
                val cleanAnswers = s.answers.map { cleanListeningStr(it) }.filter { it.isNotEmpty() }
                val cleanStatements = s.statements
                    .filter { it.statement.isNotBlank() }
                    .map { it.copy(statement = cleanListeningStr(it.statement), answer = cleanListeningStr(it.answer)) }
                val cleanTags = s.tags.filter { it.isNotBlank() }

                val isNew = s.editingId == null
                val item = if (!isNew) {
                    ListeningItem(
                        id = s.editingId!!,
                        text = cleanText, wordBox = cleanWordBox, answers = cleanAnswers,
                        statements = cleanStatements, shuffleStatements = s.shuffleStatements,
                        shuffleWordBox = s.shuffleWordBox, tags = cleanTags
                    ).also { repo.update(it) }
                } else {
                    val sortMax = allItems.maxOfOrNull { it.sortOrder } ?: 0
                    ListeningItem(
                        id = genListeningId(), text = cleanText, wordBox = cleanWordBox,
                        answers = cleanAnswers, statements = cleanStatements,
                        shuffleStatements = s.shuffleStatements, shuffleWordBox = s.shuffleWordBox,
                        tags = cleanTags, sortOrder = sortMax + 1, createdAt = Instant.now().toString()
                    ).also { repo.create(it) }
                }

                _uiState.update { it.copy(saving = false, pendingMismatchConfirm = false) }
                resetForm()
                onSaved(item, isNew)
            } catch (e: Exception) {
                _uiState.update { it.copy(saving = false) }
                onError(e.message ?: "Lỗi không xác định")
            }
        }
    }
}
