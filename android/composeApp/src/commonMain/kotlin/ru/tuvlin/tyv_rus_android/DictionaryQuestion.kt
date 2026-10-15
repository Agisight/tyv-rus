package ru.tuvlin.tyv_rus_android

enum class QuestionKind { MEANING, TRANSLATION, EXAMPLES, SEARCH }
data class DictionaryQuestion(val query: String, val russian: Boolean?, val kind: QuestionKind)

private fun questionTerm(text: String): String = text.trim().trim(' ', '«', '»', '“', '”', '"', '\'', '?', '!', '.', ':', ',', '—', '-')
private val languageRequest = Regex(
    """\b(?:по[-\s]+(тувински|русски)|на\s+(тувинск(?:ом|ий)|русск(?:ом|ий))(?:\s+язык(?:е)?)?)\b""",
    RegexOption.IGNORE_CASE
)
private val exampleRequest = Regex("""\b(?:пример(?:ы|ов)?|употреблени[ея])\b""", RegexOption.IGNORE_CASE)
private val requestPrefixes = listOf(
    """^(?:а\s+)?(?:пожалуйста[,!]?\s+|подскажи(?:те)?(?:\s+мне)?[,!]?\s+|скажи(?:те)?(?:\s+мне)?[,!]?\s+|можешь(?:\s+ли)?(?:\s+мне)?\s+|можете(?:\s+ли)?(?:\s+мне)?\s+)""",
    """^(?:а\s+)?(?:как\s+(?:(?:это\s+)?(?:будет|сказать|перевести|переводится|называется)|переводится)|что\s+(?:значит|означает|такое)|какой\s+перевод|как\s+понять)\b\s*""",
    """^(?:а\s+)?(?:переведи(?:те)?|перевести|перевод|объясни(?:те)?|объяснить|значение|найди(?:те)?|найти|покажи(?:те)?|приведи(?:те)?|дай(?:те)?)\b\s*""",
    """^(?:мне\s+|слово\s+|слова\s+|выражение\s+|фразу\s+|примеры?\s*|употреблени[ея]\s*|со\s+словом\s+|для\s+слова\s+)"""
).map { Regex(it, RegexOption.IGNORE_CASE) }

/** Parse dictionary requests; never treat an arbitrary instruction as a translation. */
fun parseDictionaryQuestion(question: String, previous: DictionaryQuestion? = null): DictionaryQuestion? {
    val text = questionTerm(question.replace(Regex("\\s+"), " "))
    if (text.isEmpty()) return null
    val language = languageRequest.find(text)
    val direction = language?.value?.lowercase()?.let { !it.contains("рус") }
    val kind = when {
        exampleRequest.containsMatchIn(text) -> QuestionKind.EXAMPLES
        language != null || Regex("""\b(?:переведи(?:те)?|перевести|перевод(?:ится)?)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) -> QuestionKind.TRANSLATION
        Regex("""\b(?:значит|означает|такое|значение|объясни(?:те)?|понять)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) -> QuestionKind.MEANING
        else -> QuestionKind.SEARCH
    }
    var term = questionTerm(languageRequest.replace(text, " "))
    var recognized = language != null
    if (language != null) term = questionTerm(Regex("""^как(?:\s+это)?\s+(?!(?:будет|сказать|перевести|переводится|называется)\b)""", RegexOption.IGNORE_CASE).replaceFirst(term, ""))
    if (language != null) term = questionTerm(Regex("""\s+переводится$""", RegexOption.IGNORE_CASE).replace(term, ""))
    if (kind == QuestionKind.EXAMPLES) term = questionTerm(Regex("""^(?:а\s+)?(?:ещ[её]\s+)?""", RegexOption.IGNORE_CASE).replaceFirst(term, ""))
    repeat(8) {
        val before = term
        for (prefix in requestPrefixes) term = questionTerm(prefix.replaceFirst(term, ""))
        if (before != term) recognized = true
    }
    term = questionTerm(Regex("""[,\s]+пожалуйста$""", RegexOption.IGNORE_CASE).replace(term, ""))
    if (kind == QuestionKind.EXAMPLES) {
        val ending = Regex("""\s+(?:примеры?(?:\s+употребления)?)$""", RegexOption.IGNORE_CASE)
        if (ending.containsMatchIn(term)) { term = questionTerm(ending.replace(term, "")); recognized = true }
    }
    val suffix = Regex("""\s+(?:это\s+)?(?:что\s+(?:значит|означает)|как\s+переводится)$""", RegexOption.IGNORE_CASE)
    if (suffix.containsMatchIn(term)) { term = questionTerm(suffix.replace(term, "")); recognized = true }
    if (recognized) {
        val quoted = Regex("""[«“"']([^»”"']+)[»”"']""").find(question)
        if (quoted != null) term = questionTerm(quoted.groupValues[1])
        if (term.isEmpty() || term.lowercase() in listOf("а", "еще", "ещё")) {
            return previous?.copy(russian = direction ?: previous.russian, kind = kind)
        }
        if (term.split(' ').size <= 8) return DictionaryQuestion(term,
            direction ?: if (kind == QuestionKind.MEANING) false else null, kind)
        return null
    }
    return if (term.split(' ').size <= 3 && kind == QuestionKind.SEARCH)
        DictionaryQuestion(term, null, kind) else null
}

/** A conservative spelling hint, including one adjacent-letter transposition. */
internal fun spellingDistance(a: String, b: String): Int {
    val rows = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) rows[i][0] = i
    for (j in 0..b.length) rows[0][j] = j
    for (i in 1..a.length) for (j in 1..b.length) {
        rows[i][j] = minOf(rows[i - 1][j] + 1, rows[i][j - 1] + 1,
            rows[i - 1][j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
        if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1])
            rows[i][j] = minOf(rows[i][j], rows[i - 2][j - 2] + 1)
    }
    return rows[a.length][b.length]
}

fun googleTranslateUrl(text: String, russian: Boolean = true): String {
    val encoded = buildString {
        for (byte in text.encodeToByteArray()) {
            val value = byte.toInt() and 255
            if (value in 65..90 || value in 97..122 || value in 48..57 || value in listOf(45, 46, 95, 126)) append(value.toChar())
            else append('%').append(value.toString(16).uppercase().padStart(2, '0'))
        }
    }
    return "https://translate.google.com/?sl=${if (russian) "ru" else "tyv"}&tl=${if (russian) "tyv" else "ru"}&op=translate&text=$encoded"
}
