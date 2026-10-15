package ru.tuvlin.tyv_rus_android.ui

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View
import androidx.core.text.HtmlCompat

private val breakTag = Regex("""<\s*/?\s*br\s*/?\s*>""", RegexOption.IGNORE_CASE)
private val pronunciationTag = Regex("""<c\s+darkgreen\s*>""", RegexOption.IGNORE_CASE)
private val indentedLineTag = Regex("""<m3\s*>""", RegexOption.IGNORE_CASE)
private val referenceLabel = Regex("""(смотри\s*:|сравни\s*с\s*:?)""", RegexOption.IGNORE_CASE)
private val referenceWord = Regex("""[\p{L}ʼ'\-]+""")

internal fun renderDictionaryHtml(html: String, onRefClick: (String) -> Unit): Spanned {
    // The original dictionary uses closing </br> tags for line breaks and a few
    // legacy dictionary tags. Normalize them before Android parses the HTML.
    val normalized = html
        .replace(breakTag, "<br>")
        .replace(pronunciationTag, "<font color=\"#006400\">")
        .replace(indentedLineTag, "<br>&nbsp;&nbsp;&nbsp;")
    val parsed = HtmlCompat.fromHtml(normalized, HtmlCompat.FROM_HTML_MODE_LEGACY)
    val text = SpannableString(parsed.trim())
    val plain = text.toString()

    referenceLabel.findAll(plain).forEach { label ->
        val start = label.range.last + 1
        val end = plain.indexOf('\n', start).let { if (it == -1) plain.length else it }
        referenceWord.findAll(plain, startIndex = start)
            .takeWhile { it.range.first < end }
            .forEach { match ->
                val word = match.value
                text.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) = onRefClick(word)
                }, match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
    }
    return text
}
