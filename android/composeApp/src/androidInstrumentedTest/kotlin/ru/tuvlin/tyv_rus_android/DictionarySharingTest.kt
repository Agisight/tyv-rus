package ru.tuvlin.tyv_rus_android

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DictionarySharingTest {
    private fun repo(): DictionaryRepository {
        AppCtx.app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        return DictionaryRepository()
    }
    @Test fun sharedArticleKeepsBothMeaningsAndLabelsEveryExample() = runBlocking {
        val (word, html) = repo().item(10043)
        val content = articleShareContent(word, html)
        assertTrue(content.plainText.startsWith("таваар\n\n1) медлительный"))
        assertTrue(content.plainText.contains("2) спокойный || спокойно"))
        assertTrue(content.plainText.contains("Тыва: таваар кижи\nРусский: медлительный человек"))
        assertTrue(content.plainText.contains("Тыва: таваар удуур\nРусский: спать спокойно"))
        assertEquals(3, Regex("Тыва:").findAll(content.plainText).count())
        assertFalse(content.plainText.contains("<font"))
        assertTrue(content.htmlText.contains("<b>Тыва:</b>"))
        assertTrue(content.plainText.endsWith("Источник: Сөстүк · Тувинско-русский словарь"))
    }
    @Test fun sharingTheFullArticleIncludesExamplesBeyondListPreview() = runBlocking {
        val repository = repo()
        val entry = repository.lookup("чигир", false).single { it.word == "чигир" }
        val article = repository.item(entry.id)
        val content = articleShareContent(article.first, article.second)
        assertEquals(3, entry.examples.size)
        assertEquals(9, Regex("Тыва:").findAll(content.plainText).count())
        assertTrue(content.plainText.contains("Русский: мёд"))
        assertTrue(content.plainText.contains("2) сладость"))
    }
    @Test fun sharingReplyKeepsItsQuestionAndDoesNotRepeatExamples() {
        val example = DictionaryExample("таваар кижи", "медлительный человек")
        val reply = DictionaryReply(1, "Покажи примеры со словом таваар", "Примеры из словаря для «таваар»:",
            entries = listOf(DictionaryEntry(10043, "таваар", "1) медлительный", listOf(example))),
            examples = listOf(example), translationText = "таваар", russianQuery = false)
        val content = replyShareContent(reply)
        assertTrue(content.plainText.contains("Вопрос: Покажи примеры со словом таваар"))
        assertEquals(1, Regex("Тыва: таваар кижи").findAll(content.plainText).count())
        assertTrue(content.plainText.contains("таваар\n\n1) медлительный"))
    }
    @Test fun richShareEscapesUserTextButPlainShareKeepsItLiteral() {
        val reply = DictionaryReply(1, "<script>& 'слово'", "Ничего <опасного>", translationText = "<слово>")
        val content = replyShareContent(reply)
        assertTrue(content.plainText.contains("<script>& 'слово'"))
        assertFalse(content.htmlText.contains("<script>"))
        assertTrue(content.htmlText.contains("&lt;script&gt;&amp;"))
    }
    @Test fun shareIntentSuppliesPlainAndRichTextToTheSystemChooser() {
        val content = replyShareContent(DictionaryReply(1, "чигир", "Сахар", translationText = "чигир"))
        val send = dictionaryShareIntent(content)
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("text/plain", send.type)
        assertEquals(content.plainText, send.getStringExtra(Intent.EXTRA_TEXT))
        assertEquals(content.htmlText, send.getStringExtra(Intent.EXTRA_HTML_TEXT))
        assertEquals(content.title, send.getStringExtra(Intent.EXTRA_SUBJECT))
        val chooser = Intent.createChooser(send, "Поделиться")
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
    }
}
