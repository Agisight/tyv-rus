package ru.tuvlin.tyv_rus_android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val query: String = "",
    val items: List<Pair<Int, String>> = emptyList(),
    val selected: Int? = null,
    val selectedWord: String = "",
    val selectedHtml: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

class DictionaryViewModel(
    private val repo: DictionaryRepository = DictionaryRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state
    private var searchJob: Job? = null
    private var articleJob: Job? = null

    var listIndex: Int = 0; private set
    var listOffset: Int = 0; private set

    init {
        onQueryChange("")
    }

    fun saveListPosition(index: Int, offset: Int) { listIndex = index; listOffset = offset }

    fun onQueryChange(newQuery: String) {
        searchJob?.cancel()
        _state.update { it.copy(query = newQuery, isLoading = true, error = null) }
        searchJob = viewModelScope.launch {
            try {
                if (newQuery.isNotBlank()) delay(120)
                val items = repo.search(newQuery)
                _state.update { it.copy(items = items, isLoading = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false, error = "Не удалось загрузить словарь.") }
            }
        }
    }

    fun openByHead(word: String) {
        articleJob?.cancel()
        articleJob = viewModelScope.launch {
            try {
                val matches = repo.search(word).filter { it.second.equals(word.trim(), ignoreCase = true) }
                if (matches.size == 1) {
                    selectArticle(matches.single().first)
                } else {
                    // Show all homonyms so a cross-reference cannot hide another article.
                    _state.update { it.copy(selected = null) }
                    onQueryChange(word)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(selected = null, error = "Не удалось открыть статью.") }
            }
        }
    }

    fun onSelect(id: Int) {
        articleJob?.cancel()
        articleJob = viewModelScope.launch {
            try {
                selectArticle(id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(error = "Не удалось открыть статью.") }
            }
        }
    }

    private suspend fun selectArticle(id: Int) {
        val (word, html) = repo.item(id)
        _state.update { it.copy(selected = id, selectedWord = word, selectedHtml = html, error = null) }
    }

    fun onBack() {
        articleJob?.cancel()
        _state.update { it.copy(selected = null, selectedWord = "", selectedHtml = "") }
    }
}
