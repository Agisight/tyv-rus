package ru.tuvlin.tyv_rus_android

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class UiState(
    val query: String = "",
    val items: List<Pair<Int, String>> = emptyList(),
    val selected: Int? = null,
    val selectedWord: String = "",
    val selectedHtml: String = ""
)

class DictionaryViewModel(
    private val repo: DictionaryRepository = DictionaryRepository()
) {
    private val _state = MutableStateFlow(UiState(items = repo.search("")))
    val state: StateFlow<UiState> = _state

    var listIndex: Int = 0; private set
    var listOffset: Int = 0; private set

    fun saveListPosition(index: Int, offset: Int) { listIndex = index; listOffset = offset }

    fun onQueryChange(newQuery: String) {
        _state.update { it.copy(query = newQuery, items = repo.search(newQuery)) }
    }

    fun openByHead(word: String) {
        // ищем точное совпадение
        val idx = repo.search(word)
            .firstOrNull { it.second.equals(word, ignoreCase = true) }
            ?.first
        if (idx != null) {
            onSelect(idx)
        } else {
            // fallback: просто покажем результаты поиска
            onQueryChange(word)
        }
    }

    fun onSelect(index: Int) {
        val (word, def) = repo.item(index)
        _state.update { it.copy(selected = index, selectedWord = word, selectedHtml = def) }
    }

    fun onBack() {
        _state.update { it.copy(selected = null, selectedWord = "", selectedHtml = "") }
    }
}
