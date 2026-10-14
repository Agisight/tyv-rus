package ru.tuvlin.tyv_rus_android.ui

import android.graphics.text.LineBreaker
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat

@Composable
actual fun HtmlText(
    html: String,
    modifier: Modifier,
    onRefClick: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val body = MaterialTheme.typography.titleLarge

    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                setTextIsSelectable(true)
                setTextColor(cs.onSurface.toArgb())
                textSize = body.fontSize.value
                movementMethod = LinkMovementMethod.getInstance()
                linksClickable = true
                setLineSpacing(0f, 1.15f)
                breakStrategy = LineBreaker.BREAK_STRATEGY_SIMPLE
                isSingleLine = false
                setHorizontallyScrolling(false)
            }
        },
        update = { tv ->
            // парсим html как есть
            val spanned = HtmlCompat.fromHtml(
                html,
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

            val text = SpannableString(spanned)
            val plain = text.toString()

            val seeRe = "(?i)(смотри\\s*:|сравни\\s*с\\s*:?)".toRegex()
            seeRe.findAll(plain).forEach { m ->
                val start = m.range.last + 1
                val end = plain.indexOf('\n', start).let { if (it == -1) plain.length else it }

                val wordRe = "[\\p{L}ʼ'\\-]+".toRegex()
                wordRe.findAll(plain, startIndex = start)
                    .takeWhile { it.range.first < end }
                    .forEach { w ->
                        val word = w.value
                        text.setSpan(object : ClickableSpan() {
                            override fun onClick(widget: View) {
                                onRefClick(word)
                            }
                        }, w.range.first, w.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
            }

            tv.text = text
            tv.movementMethod = LinkMovementMethod.getInstance()
            tv.isSingleLine = false
            tv.setHorizontallyScrolling(false)
            tv.breakStrategy = LineBreaker.BREAK_STRATEGY_SIMPLE
        }
    )
}