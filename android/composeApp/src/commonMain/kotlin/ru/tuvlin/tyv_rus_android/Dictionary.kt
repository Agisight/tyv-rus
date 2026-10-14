package ru.tuvlin.tyv_rus_android

expect class DictionaryRepository() {
    suspend fun size(): Int
    suspend fun item(id: Int): Pair<String, String> // (head, html), identified by database row ID
    suspend fun search(q: String): List<Pair<Int, String>> // (stable article ID, head)
}
