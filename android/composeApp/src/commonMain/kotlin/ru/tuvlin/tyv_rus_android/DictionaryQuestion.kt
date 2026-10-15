package ru.tuvlin.tyv_rus_android

enum class QuestionKind { MEANING, TRANSLATION, EXAMPLES, SEARCH }
data class DictionaryQuestion(val query: String, val russian: Boolean?, val kind: QuestionKind)

private fun questionTerm(text: String): String = text.trim().trim(' ', '«', '»', '“', '”', '"', '\'', '?', '!', '.', ':')

fun parseDictionaryQuestion(question: String): DictionaryQuestion? {
    val text = question.trim()
    val patterns = listOf(
        Triple("""^как\s+(?:(?:будет|сказать|перевести)\s+)?(?:по[- ]тувински|на\s+тувинском(?:\s+языке)?)\s*[:—-]?\s*(.+)$""", true, QuestionKind.TRANSLATION),
        Triple("""^(?:что\s+(?:значит|означает)|объясни|значение(?:\s+слова)?)\s+(.+)$""", false, QuestionKind.MEANING),
        Triple("""^(?:покажи|приведи|дай)\s+примеры?(?:\s+(?:со\s+словом|для\s+слова|употребления(?:\s+слова)?|слова))?\s+(.+)$""", false, QuestionKind.EXAMPLES),
        Triple("""^(?:переведи|перевод(?:\s+слова)?)\s+(.+)$""", true, QuestionKind.TRANSLATION)
    )
    for ((pattern, russian, kind) in patterns) {
        Regex(pattern, RegexOption.IGNORE_CASE).matchEntire(text)?.let {
            val term = questionTerm(it.groupValues[1])
            if (term.isNotEmpty()) return DictionaryQuestion(term, russian, kind)
        }
    }
    val term = questionTerm(text)
    return if (term.isNotEmpty() && term.split(Regex("\\s+")).size <= 3)
        DictionaryQuestion(term, null, QuestionKind.SEARCH) else null
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
