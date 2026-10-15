package ru.tuvlin.tyv_rus_android

import android.content.Intent
import android.text.SpannableString
import ru.tuvlin.tyv_rus_android.ui.renderDictionaryHtml

internal fun articleShareContent(title: String, html: String): DictionaryShareText {
    val builder = DictionaryShareBuilder(title)
    val styled = renderDictionaryHtml(html) {}
    var offset = 0
    var first = true
    for (rawLine in styled.toString().split('\n')) {
        val line = rawLine.trim()
        val examples = styledDictionaryExamples(SpannableString(styled.subSequence(offset, offset + rawLine.length)))
        if (examples.isNotEmpty()) examples.forEach(builder::example)
        else if (line.isNotEmpty() && !(first && line == title)) builder.paragraph(line)
        if (line.isNotEmpty()) first = false
        offset += rawLine.length + 1
    }
    return builder.build()
}

internal fun dictionaryShareIntent(content: DictionaryShareText): Intent = Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_TITLE, content.title)
    putExtra(Intent.EXTRA_SUBJECT, content.title)
    putExtra(Intent.EXTRA_TEXT, content.plainText)
    putExtra(Intent.EXTRA_HTML_TEXT, content.htmlText)
}
