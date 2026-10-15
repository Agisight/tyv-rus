package ru.tuvlin.tyv_rus_android

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        AppCtx.app = applicationContext

        setContent {
            App(
                onShareArticle = { title, html -> shareDictionary(articleShareContent(title, html)) },
                onShareReply = { reply -> shareDictionary(replyShareContent(reply)) }
            )
        }
    }

    private fun shareDictionary(content: DictionaryShareText) {
        startActivity(Intent.createChooser(dictionaryShareIntent(content), "Поделиться"))
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}