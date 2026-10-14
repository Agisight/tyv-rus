package ru.tuvlin.tyv_rus_android

// Тот же публичный API, что и был, только теперь это expect.
// Реализация будет на Android.
expect class DictionaryRepository() {
    fun size(): Int
    fun item(i: Int): Pair<String, String>          // (head, html)
    fun search(q: String): List<Pair<Int, String>>  // (index, head)
}
