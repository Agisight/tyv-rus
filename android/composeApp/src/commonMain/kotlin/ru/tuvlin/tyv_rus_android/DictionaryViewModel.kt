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
    val entries: List<DictionaryEntry> = emptyList(),
    val direction: SearchDirection = SearchDirection.TUVAN,
    val mode: AppMode = AppMode.DICTIONARY,
    val selected: Int? = null,
    val selectedWord: String = "",
    val selectedHtml: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

data class DictionaryReply(
    val id: Int,
    val question: String,
    val answer: String = "",
    val entries: List<DictionaryEntry> = emptyList(),
    val examples: List<String> = emptyList(),
    val translationText: String = question,
    val russianQuery: Boolean = true
)
data class AssistantState(val replies: List<DictionaryReply> = emptyList(), val isLoading: Boolean = false)

class DictionaryViewModel(private val repo: DictionaryRepository = DictionaryRepository()) : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state
    private val _assistant = MutableStateFlow(AssistantState())
    val assistant: StateFlow<AssistantState> = _assistant
    private var searchJob: Job? = null
    private var articleJob: Job? = null
    private var questionJob: Job? = null
    private var nextReplyId = 1
    var listIndex: Int = 0; private set
    var listOffset: Int = 0; private set

    init { onQueryChange("") }
    fun saveListPosition(index: Int, offset: Int) { listIndex = index; listOffset = offset }
    fun setMode(mode: AppMode) { _state.update { it.copy(mode = mode) } }
    fun setDirection(direction: SearchDirection) {
        _state.update { it.copy(direction = direction) }
        onQueryChange(_state.value.query)
    }

    fun onQueryChange(newQuery: String) {
        searchJob?.cancel()
        val direction = _state.value.direction
        _state.update { it.copy(query = newQuery, isLoading = true, error = null) }
        searchJob = viewModelScope.launch {
            try {
                if (newQuery.isNotBlank()) delay(120)
                val entries = repo.lookup(newQuery, direction == SearchDirection.RUSSIAN)
                _state.update { it.copy(entries = entries, items = entries.map { e -> e.id to e.word }, isLoading = false) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.update { it.copy(isLoading = false, error = "Не удалось загрузить словарь.") } }
        }
    }

    fun ask(question: String) {
        val value = question.trim().take(300)
        if (value.isEmpty() || _assistant.value.isLoading) return
        val id = nextReplyId++
        _assistant.update { it.copy(replies = it.replies + DictionaryReply(id, value), isLoading = true) }
        questionJob = viewModelScope.launch {
            try {
                val plan = parseDictionaryQuestion(value)
                var russian = plan?.russian ?: true
                val entries = when (plan?.russian) {
                    true -> repo.lookup(plan.query, true)
                    false -> repo.lookup(plan.query, false)
                    null -> if (plan == null) emptyList() else {
                        val tuvan = repo.lookup(plan.query, false)
                        if (tuvan.isEmpty()) repo.lookup(plan.query, true) else { russian = false; tuvan }
                    }
                }.let { matches ->
                    val exact = if (!russian && plan != null) matches.filter { it.word.equals(plan.query, ignoreCase = true) } else emptyList()
                    (exact.ifEmpty { matches }).take(4)
                }
                val examples = if (plan?.kind == QuestionKind.EXAMPLES) entries.flatMap { it.examples }.distinct().take(4) else emptyList()
                val answer = when {
                    plan == null -> "Попробуйте спросить: «Что означает таваар?», «Как по-тувински медведь?» или «Покажи примеры со словом таваар». Я ищу по словарю и не составляю произвольные переводы фраз."
                    entries.isEmpty() -> "По запросу «${plan.query}» ничего не нашлось. Попробуйте начальную форму слова или более короткий запрос."
                    plan.kind == QuestionKind.EXAMPLES && examples.isEmpty() -> "Для «${plan.query}» нашлись статьи, но отдельных примеров в них нет. Можно открыть полное значение."
                    plan.kind == QuestionKind.EXAMPLES -> "Примеры из словаря для «${plan.query}»:"
                    plan.kind == QuestionKind.TRANSLATION -> "Вот что нашлось по русскому запросу «${plan.query}». Выберите подходящее значение:"
                    else -> "Нашлись словарные статьи для «${plan.query}»:"
                }
                val reply = DictionaryReply(id, value, answer, entries, examples, plan?.query ?: value, russian)
                _assistant.update { it.copy(replies = it.replies.map { old -> if (old.id == id) reply else old }, isLoading = false) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                _assistant.update { it.copy(replies = it.replies.map { old -> if (old.id == id) old.copy(answer = "Не удалось выполнить поиск. Попробуйте ещё раз.") else old }, isLoading = false) }
            }
        }
    }

    fun clearConversation() {
        questionJob?.cancel()
        _assistant.value = AssistantState()
    }

    fun openByHead(word: String) {
        articleJob?.cancel()
        articleJob = viewModelScope.launch {
            try {
                val matches = repo.search(word).filter { it.second.equals(word.trim(), ignoreCase = true) }
                if (matches.size == 1) selectArticle(matches.single().first)
                else {
                    _state.update { it.copy(selected = null, mode = AppMode.DICTIONARY, direction = SearchDirection.TUVAN) }
                    onQueryChange(word)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.update { it.copy(selected = null, error = "Не удалось открыть статью.") } }
        }
    }

    fun onSelect(id: Int) {
        articleJob?.cancel()
        articleJob = viewModelScope.launch {
            try { selectArticle(id) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.update { it.copy(error = "Не удалось открыть статью.") } }
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
