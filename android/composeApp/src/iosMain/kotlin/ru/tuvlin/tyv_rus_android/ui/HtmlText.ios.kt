package ru.tuvlin.tyv_rus_android.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSURL
import platform.UIKit.UIColor
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun HtmlText(html: String, modifier: Modifier, onRefClick: (String) -> Unit) {
    UIKitView(
        modifier = modifier,
        factory = {
            val config = WKWebViewConfiguration()
            WKWebView(frame = CGRectZero.readValue(), configuration = config).apply {
                setOpaque(false)
                setBackgroundColor(UIColor.clearColor)
                scrollView.bounces = true
            }
        },
        update = { webView ->
            // Подадим небольшой CSS, чтобы наследовать системный шрифт/цвет темы
            val styled = """
                <html>
                  <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                      body { font: -apple-system-body; color: #000000; margin:0; padding:0; }
                      a { color: #0A84FF; } /* iOS system blue */
                    </style>
                  </head>
                  <body>$html</body>
                </html>
            """.trimIndent()
            webView.loadHTMLString(styled, baseURL = null as NSURL?)
        }
    )
}
