package com.learnsy.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnsy.admin.data.ListeningItem
import com.learnsy.admin.data.ListeningRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ListeningFilter { ALL, HAS_WORD_BOX, HAS_TFNM, NO_WORD_BOX }
enum class ListeningSort { ORDER, CREATED, BLANKS }

data class ListeningListUiState(
    val items: List<ListeningItem> = emptyList(),
    val loading: Boolean = true,
    val loadError: Boolean = false,
    val searchQuery: String = "",
    val filter: ListeningFilter = ListeningFilter.ALL,
    val sortBy: ListeningSort = ListeningSort.ORDER,
    val bulkMode: Boolean = false,
    val selected: Set<String> = emptySet()
)

// Tương đương phần list state trong ListeningManager (listening-panel.jsx)
class ListeningListViewModel(
    private val repo: ListeningRepository = ListeningRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListeningListUiState())
    val uiState: StateFlow<ListeningListUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.update { it.copy(loading = true, loadError = false) }
        viewModelScope.launch {
            try {
                val items = repo.fetchAll()
                _uiState.update { it.copy(items = items, loading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, loadError = true) }
            }
        }
    }

    fun setSearch(q: String) = _uiState.update { it.copy(searchQuery = q) }
    fun setFilter(f: ListeningFilter) = _uiState.update { it.copy(filter = f) }
    fun setSortBy(s: ListeningSort) = _uiState.update { it.copy(sortBy = s) }

    fun toggleBulkMode() = _uiState.update {
        it.copy(bulkMode = !it.bulkMode, selected = if (it.bulkMode) emptySet() else it.selected)
    }

    fun toggleSelect(id: String) = _uiState.update {
        val s = it.selected.toMutableSet()
        if (id in s) s.remove(id) else s.add(id)
        it.copy(selected = s)
    }

    fun selectAll() = _uiState.update { it.copy(selected = displayItems().map { i -> i.id }.toSet()) }
    fun deselectAll() = _uiState.update { it.copy(selected = emptySet()) }

    fun displayItems(): List<ListeningItem> {
        val s = _uiState.value
        var list = s.items

        list = when (s.filter) {
            ListeningFilter.ALL -> list
            ListeningFilter.HAS_WORD_BOX -> list.filter { it.wordBox.isNotEmpty() }
            ListeningFilter.HAS_TFNM -> list.filter { it.statements.isNotEmpty() }
            ListeningFilter.NO_WORD_BOX -> list.filter { it.wordBox.isEmpty() }
        }

        if (s.searchQuery.isNotBlank()) {
            val q = s.searchQuery.lowercase()
            list = list.filter {
                it.text.lowercase().contains(q) ||
                    it.tags.any { t -> t.lowercase().contains(q) } ||
                    it.answers.any { a -> a.lowercase().contains(q) }
            }
        }

        list = when (s.sortBy) {
            ListeningSort.BLANKS -> list.sortedByDescending { it.answers.size }
            ListeningSort.CREATED -> list.sortedByDescending { it.createdAt }
            ListeningSort.ORDER -> list
        }

        return list
    }

    fun deleteItem(id: String, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                repo.delete(id)
                _uiState.update {
                    it.copy(items = it.items.filter { i -> i.id != id }, selected = it.selected - id)
                }
                onDone(true, "Đã xoá câu Listening")
            } catch (e: Exception) {
                onDone(false, "Xoá thất bại: ${e.message}")
            }
        }
    }

    fun bulkDelete(onDone: (Boolean, String) -> Unit) {
        val ids = _uiState.value.selected.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                repo.bulkDelete(ids)
                _uiState.update {
                    it.copy(
                        items = it.items.filter { i -> i.id !in ids },
                        selected = emptySet(),
                        bulkMode = false
                    )
                }
                onDone(true, "Đã xoá ${ids.size} câu")
            } catch (e: Exception) {
                onDone(false, "Xoá thất bại: ${e.message}")
            }
        }
    }

    fun duplicateItem(item: ListeningItem, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val dup = repo.duplicate(item, _uiState.value.items)
                _uiState.update { it.copy(items = it.items + dup) }
                onDone(true, "✓ Đã nhân đôi câu Listening")
            } catch (e: Exception) {
                onDone(false, "Nhân đôi thất bại: ${e.message}")
            }
        }
    }

    fun reorder(srcId: String, targetId: String) {
        if (srcId == targetId) return
        val list = _uiState.value.items.toMutableList()
        val srcIdx = list.indexOfFirst { it.id == srcId }
        val tgtIdx = list.indexOfFirst { it.id == targetId }
        if (srcIdx < 0 || tgtIdx < 0) return
        val moved = list.removeAt(srcIdx)
        list.add(tgtIdx, moved)
        val reindexed = list.mapIndexed { i, it -> it.copy(sortOrder = i) }
        _uiState.update { it.copy(items = reindexed) }
        viewModelScope.launch { repo.persistOrder(reindexed.map { it.id }) }
    }

    fun replaceItem(item: ListeningItem) {
        _uiState.update { st -> st.copy(items = st.items.map { if (it.id == item.id) item else it }) }
    }

    fun importItems(items: List<ListeningItem>, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val refreshed = repo.importItems(items, _uiState.value.items)
                _uiState.update { it.copy(items = refreshed) }
                onDone(true, "✓ Đã import ${items.size} câu")
            } catch (e: Exception) {
                onDone(false, "Import thất bại: ${e.message}")
            }
        }
    }
}
