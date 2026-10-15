package ru.tuvlin.tyv_rus_android

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.tuvlin.tyv_rus_android.ui.renderDictionaryHtml

@RunWith(AndroidJUnit4::class)
class DictionaryHtmlTest {
    private fun repository(): DictionaryRepository {
        AppCtx.app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        return DictionaryRepository()
    }

    private fun assertColor(text: Spanned, phrase: String, color: Int) {
        val start = text.toString().indexOf(phrase)
        assertTrue("Missing phrase: $phrase", start >= 0)
        assertTrue("Lost color for $phrase", text.getSpans(start, start + phrase.length,
            ForegroundColorSpan::class.java).any { it.foregroundColor == color })
    }

    @Test fun tavaarKeepsMeaningsAndExamplesOnSeparateLinesWithOriginalColors() = runBlocking {
        val article = repository().item(10043)
        assertEquals("таваар", article.first)
        val text = renderDictionaryHtml(article.second) {}
        val lines = text.toString().lines().map { it.trim() }.filter { it.isNotEmpty() }
        assertEquals(5, lines.size)
        assertTrue(lines[0], lines[0].startsWith("1) медлительный"))
        assertEquals("таваар кижи медлительный человек", lines[1])
        assertEquals("2) спокойный || спокойно", lines[2])
        assertEquals("таваар чуртталга спокойная жизнь", lines[3])
        assertEquals("таваар удуур спать спокойно", lines[4])
        assertColor(text, "таваар кижи", Color.rgb(138, 43, 226))
        assertColor(text, "медлительный человек", Color.rgb(128, 128, 128))
        assertFalse(text.toString().startsWith("\n"))
    }

    @Test fun crossReferencesStayClickableWithoutLinkingTheNextDefinition() = runBlocking {
        val article = repository().item(281)
        val opened = mutableListOf<String>()
        val text = renderDictionaryHtml(article.second + "</br>следующее значение", opened::add)
        val links = text.getSpans(0, text.length, ClickableSpan::class.java)
        assertEquals(setOf("мажаалай", "хайыракан"), links.map {
            text.subSequence(text.getSpanStart(it), text.getSpanEnd(it)).toString()
        }.toSet())
        val next = text.toString().indexOf("следующее значение")
        assertEquals(0, text.getSpans(next, text.length, ClickableSpan::class.java).size)
        val view = View(InstrumentationRegistry.getInstrumentation().targetContext)
        links.forEach { it.onClick(view) }
        assertEquals(setOf("мажаалай", "хайыракан"), opened.toSet())
    }

    @Test fun standardHtmlStylesAndBreakVariantsArePreserved() {
        val text = renderDictionaryHtml("<b>слово</b><br/><i>помета</i><BR><u>пример</u>") {}
        assertEquals("слово\nпомета\nпример", text.toString())
        assertTrue(text.getSpans(0, 5, StyleSpan::class.java).any { it.style == Typeface.BOLD })
        assertTrue(text.getSpans(6, 12, StyleSpan::class.java).any { it.style == Typeface.ITALIC })
        assertEquals(1, text.getSpans(13, 19, UnderlineSpan::class.java).size)
    }

    @Test fun legacyPronunciationAndIndentedExamplesRemainReadable() = runBlocking {
        val article = repository().item(282)
        val text = renderDictionaryHtml(article.second) {}
        assertColor(text, "{аъдыг}", Color.rgb(0, 100, 0))
        val example = renderDictionaryHtml("значение<m3><font color=\"#8A2BE2\">пример</font>") {}
        assertEquals("значение\n\u00a0\u00a0\u00a0пример", example.toString())
    }
}
