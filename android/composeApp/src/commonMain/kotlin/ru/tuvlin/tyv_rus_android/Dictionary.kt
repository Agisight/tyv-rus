package ru.tuvlin.tyv_rus_android

data class DictionaryEntry(
    val id: Int,
    val word: String,
    val preview: String,
    val examples: List<String> = emptyList()
)

data class SpellingSuggestion(val query: String, val entries: List<DictionaryEntry>)

enum class SearchDirection { TUVAN, RUSSIAN }
enum class AppMode { DICTIONARY, ASK }

expect class DictionaryRepository() {
    suspend fun size(): Int
    suspend fun item(id: Int): Pair<String, String>
    suspend fun search(q: String): List<Pair<Int, String>>
    suspend fun lookup(q: String, russian: Boolean): List<DictionaryEntry>
    suspend fun suggest(q: String, russian: Boolean): List<SpellingSuggestion>
}
