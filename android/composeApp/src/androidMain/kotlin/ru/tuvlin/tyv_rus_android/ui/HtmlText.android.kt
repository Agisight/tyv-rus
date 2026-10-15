package ru.tuvlin.tyv_rus_android.ui

import android.graphics.text.LineBreaker
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView

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
            tv.text = renderDictionaryHtml(html, onRefClick)
            tv.setTextColor(cs.onSurface.toArgb())
            tv.textSize = body.fontSize.value
            tv.movementMethod = LinkMovementMethod.getInstance()
            tv.isSingleLine = false
            tv.setHorizontallyScrolling(false)
            tv.breakStrategy = LineBreaker.BREAK_STRATEGY_SIMPLE
        }
    )
}