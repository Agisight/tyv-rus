package ru.tuvlin.tyv_rus_android

data class DictionaryShareText(val title: String, val plainText: String, val htmlText: String)

private fun escapeShareHtml(text: String): String = text.replace("&", "&amp;")
    .replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")

internal class DictionaryShareBuilder(private val title: String) {
    private val plain = StringBuilder(title)
    private val rich = StringBuilder("<html><body><h2>${escapeShareHtml(title)}</h2>")
    fun paragraph(text: String) {
        if (text.isBlank()) return
        plain.append("\n\n").append(text.trim())
        rich.append("<p>").append(escapeShareHtml(text.trim()).replace("\n", "<br>")).append("</p>")
    }
    fun heading(text: String) {
        plain.append("\n\n").append(text)
        rich.append("<h3>").append(escapeShareHtml(text)).append("</h3>")
    }
    fun example(example: DictionaryExample) {
        plain.append("\n\nТыва: ").append(example.tuvan).append("\nРусский: ").append(example.russian)
        rich.append("<p><b>Тыва:</b> ").append(escapeShareHtml(example.tuvan))
            .append("<br><b>Русский:</b> ").append(escapeShareHtml(example.russian)).append("</p>")
    }
    fun build(): DictionaryShareText {
        val source = "Источник: Сөстүк · Тувинско-русский словарь"
        return DictionaryShareText(title, "$plain\n\n—\n$source",
            "$rich<hr><p>${escapeShareHtml(source)}</p></body></html>")
    }
}

fun replyShareContent(reply: DictionaryReply): DictionaryShareText {
    val builder = DictionaryShareBuilder("Сөстүк — ${reply.translationText}")
    builder.paragraph("Вопрос: ${reply.question}")
    builder.paragraph(reply.answer)
    val included = mutableSetOf<DictionaryExample>()
    reply.examples.forEach { if (included.add(it)) builder.example(it) }
    reply.entries.forEach { entry ->
        builder.heading(entry.word)
        builder.paragraph(entry.preview)
        if (reply.examples.isEmpty()) entry.examples.firstOrNull()?.let { if (included.add(it)) builder.example(it) }
    }
    return builder.build()
}
