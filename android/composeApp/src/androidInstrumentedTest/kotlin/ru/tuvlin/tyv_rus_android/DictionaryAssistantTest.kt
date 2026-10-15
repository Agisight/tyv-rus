package ru.tuvlin.tyv_rus_android

import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DictionaryAssistantTest {
    companion object {
        @BeforeClass @JvmStatic fun prepareDictionary() = runBlocking {
            AppCtx.app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
            assertEquals(16627, DictionaryRepository().size())
        }
    }
    @Test fun russianLookupFindsBearWithPreviewAndRealExamples() = runBlocking {
        val entries = DictionaryRepository().lookup("медведь", true)
        assertTrue(entries.isNotEmpty())
        assertTrue(entries.first().preview.contains("медведь"))
        val bear = entries.first { it.word == "адыг" }
        assertTrue(bear.preview.contains("медведь"))
        assertTrue(bear.examples.any { it.contains("медвежонок") })
        assertEquals(entries, DictionaryRepository().lookup("МЕДВЕДЬ", true))
        assertTrue(DictionaryRepository().lookup("медвед", true).any { it.word == "адыг" })
    }
    @Test fun russianSearchNormalizesYoAndTreatsQuerySyntaxAsText() = runBlocking {
        val repo = DictionaryRepository()
        val entries = repo.lookup("ребёнок", true)
        assertTrue(entries.isNotEmpty())
        assertEquals(entries, repo.lookup("РЕБЕНОК", true))
        assertTrue(repo.lookup("%_\"", true).isEmpty())
        assertTrue(repo.lookup("медведь OR несуществующееслово", true).isEmpty())
    }
    @Test fun tuvanLookupKeepsEveryStableArticleAndAddsPreviews() = runBlocking {
        val repo = DictionaryRepository()
        assertEquals(repo.search(""), repo.lookup("", false).map { it.id to it.word })
        val homonyms = repo.lookup("адыг", false).filter { it.word == "адыг" }
        assertEquals(2, homonyms.size)
        assertEquals(2, homonyms.map { it.preview }.toSet().size)
        assertTrue(homonyms.all { it.preview.isNotBlank() })
    }
    @Test fun russianPreviewShowsTheMatchingMeaning() = runBlocking {
        val entry = DictionaryRepository().lookup("спокойный", true).first { it.word == "таваар" }
        assertTrue(entry.preview, entry.preview.contains("спокойный"))
    }
    @Test fun russianLookupUsesTheFullTextIndex() {
        SQLiteDatabase.openDatabase(dictionaryFile(AppCtx.app).path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("EXPLAIN QUERY PLAN SELECT d.rowid FROM dictionary d JOIN dictionary_fts f ON f.docid=d.rowid WHERE dictionary_fts MATCH ?", arrayOf("\"медведь\"*")).use { cursor ->
                val plan = buildList { while(cursor.moveToNext()) add(cursor.getString(3)) }.joinToString()
                assertTrue(plan, plan.contains("VIRTUAL TABLE INDEX"))
            }
        }
    }
    @Test fun questionsExtractTheRequestedWordAndTheTranslationDirection() {
        val translate = parseDictionaryQuestion("Как по-тувински «медведь»?")!!
        assertEquals("медведь", translate.query)
        assertEquals(true, translate.russian)
        assertEquals(QuestionKind.TRANSLATION, translate.kind)
        val meaning = parseDictionaryQuestion("Что означает таваар?")!!
        assertEquals("таваар", meaning.query)
        assertEquals(false, meaning.russian)
        val examples = parseDictionaryQuestion("Покажи примеры со словом таваар")!!
        assertEquals("таваар", examples.query)
        assertEquals(QuestionKind.EXAMPLES, examples.kind)
        assertNull(parseDictionaryQuestion("Напиши сочинение о природе Тувы"))
    }
    @Test fun externalTranslateUrlEncodesTheTextAndKeepsTheDirection() {
        val ru = Uri.parse(googleTranslateUrl("медведь & друг?", true))
        assertEquals("translate.google.com", ru.host)
        assertEquals("ru", ru.getQueryParameter("sl"))
        assertEquals("tyv", ru.getQueryParameter("tl"))
        assertEquals("медведь & друг?", ru.getQueryParameter("text"))
        val tyv = Uri.parse(googleTranslateUrl("таваар", false))
        assertEquals("tyv", tyv.getQueryParameter("sl"))
        assertEquals("ru", tyv.getQueryParameter("tl"))
    }
    @Test fun conversationUsesDictionarySourcesAndSurvivesArticleNavigation() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var vm: DictionaryViewModel
            scenario.onActivity {
                vm = ViewModelProvider(it)[DictionaryViewModel::class.java]
                vm.setMode(AppMode.ASK)
                vm.ask("Как по-тувински медведь?")
            }
            val first = withTimeout(15000) { vm.assistant.first { it.replies.size == 1 && !it.isLoading } }.replies.single()
            assertTrue(first.entries.any { it.word == "адыг" })
            assertTrue(first.russianQuery)
            scenario.onActivity { vm.ask("Покажи примеры со словом таваар") }
            val second = withTimeout(15000) { vm.assistant.first { it.replies.size == 2 && !it.isLoading } }.replies.last()
            assertTrue(second.examples.any { it.contains("таваар кижи") && it.contains("медлительный человек") })
            assertFalse(second.russianQuery)
            scenario.onActivity { vm.onSelect(second.entries.first().id) }
            withTimeout(15000) { vm.state.first { it.selected != null } }
            scenario.onActivity { vm.onBack() }
            assertEquals(AppMode.ASK, vm.state.value.mode)
            assertEquals(2, vm.assistant.value.replies.size)
            scenario.onActivity { vm.clearConversation() }
            assertTrue(vm.assistant.value.replies.isEmpty())
        }
    }
    @Test fun dictionaryModeCanSearchByRussianTranslation() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var vm: DictionaryViewModel
            scenario.onActivity {
                vm = ViewModelProvider(it)[DictionaryViewModel::class.java]
                vm.setDirection(SearchDirection.RUSSIAN)
                vm.onQueryChange("медведь")
            }
            val state = withTimeout(15000) { vm.state.first { it.query == "медведь" && !it.isLoading } }
            assertNull(state.error)
            assertTrue(state.entries.any { it.word == "адыг" })
        }
    }
}
