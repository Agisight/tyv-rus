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
    @Test fun flexibleQuestionsSupportBothDirectionsAndWordOrder() {
        val russian = listOf(
            "Как будет по-русски 'чигир'?", "Как по-русски чигир?", "Как чигир переводится на русский?",
            "Переведи чигир на русский язык", "Подскажи, как будет по-русски чигир", "чигир по-русски",
            "Пожалуйста, переведи слово «чигир» на русский", "Что означает слово чигир?", "Что такое чигир?",
            "Объясни мне слово чигир", "чигир что значит?"
        )
        for (q in russian) {
            val plan = parseDictionaryQuestion(q)
            assertNotNull(q, plan)
            assertEquals(q, "чигир", plan!!.query)
            assertEquals(q, false, plan.russian)
        }
        for (q in listOf("Как будет медведь по-тувински?", "Переведи слово медведь на тувинский", "медведь на тувинском языке", "Скажи, как сказать по-тувински медведь")) {
            val plan = parseDictionaryQuestion(q)!!
            assertEquals(q, "медведь", plan.query)
            assertEquals(q, true, plan.russian)
        }
        assertNull(parseDictionaryQuestion("Напиши сочинение о природе Тувы"))
    }
    @Test fun followUpExamplesReuseThePreviousWordAndSpellingHintsStayConservative() {
        val previous = DictionaryQuestion("чигир", false, QuestionKind.SEARCH)
        for (q in listOf("А примеры?", "Покажи примеры", "А ещё примеры?")) {
            val plan = parseDictionaryQuestion(q, previous)!!
            assertEquals(q, "чигир", plan.query)
            assertEquals(q, QuestionKind.EXAMPLES, plan.kind)
        }
        assertEquals(1, spellingDistance("чигри", "чигир"))
        assertEquals(1, spellingDistance("чигирь", "чигир"))
        assertTrue(spellingDistance("медведь", "чигир") > 2)
    }
    @Test fun russianTranslationFromTheScreenshotAndFollowUpExamplesWork() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var vm: DictionaryViewModel
            scenario.onActivity {
                vm = ViewModelProvider(it)[DictionaryViewModel::class.java]
                vm.ask("Как будет по-русски 'чигир'?")
            }
            val first = withTimeout(15000) { vm.assistant.first { it.replies.size == 1 && !it.isLoading } }.replies.single()
            assertFalse(first.russianQuery)
            assertEquals("чигир", first.entries.single().word)
            assertTrue(first.entries.single().preview.contains("сахар"))
            assertTrue(first.answer.contains("по-русски"))
            scenario.onActivity { vm.ask("А примеры?") }
            val examples = withTimeout(15000) { vm.assistant.first { it.replies.size == 2 && !it.isLoading } }.replies.last()
            assertTrue(examples.examples.any { it.contains("ак чигир") })
            scenario.onActivity { vm.ask("Как по-русски чигри?") }
            val hint = withTimeout(15000) { vm.assistant.first { it.replies.size == 3 && !it.isLoading } }.replies.last()
            assertTrue(hint.answer.contains("Возможно"))
            assertTrue(hint.entries.any { it.word == "чигир" })
        }
    }
    @Test fun requestTyposAreRepairedWithoutChangingQuotedTerms() {
        for (q in listOf("Кка будт по-руски 'чигир'?", "Перведи чигир на русскй", "Что озночает чигир?", "Подскжи как будт по-русски чигир")) {
            val plan = parseDictionaryQuestion(q)
            assertNotNull(q, plan)
            assertEquals(q, "чигир", plan!!.query)
            assertEquals(q, false, plan.russian)
        }
        val russian = parseDictionaryQuestion("Как будт по-тувинкси медвдь?")!!
        assertEquals(true, russian.russian)
        assertEquals("медвдь", russian.query)
        val quoted = parseDictionaryQuestion("Перведи 'будт' на русский")!!
        assertEquals("будт", quoted.query)
        assertEquals("будт", repairQuestionSpelling("будт"))
        val followUp = parseDictionaryQuestion("А прмиеры?", DictionaryQuestion("чигир", false, QuestionKind.SEARCH))!!
        assertEquals("чигир", followUp.query)
        assertEquals(QuestionKind.EXAMPLES, followUp.kind)
    }
    @Test fun spellingSuggestionsSupportRussianTuvanAndStandaloneQueries() = runBlocking {
        val repo = DictionaryRepository()
        val russian = repo.suggest("медвдь", true)
        assertTrue(russian.toString(), russian.any { it.query == "медведь" && it.entries.any { e -> e.word == "адыг" } })
        assertTrue(repo.suggest("медвдеь", true).any { it.query == "медведь" })
        val tuvan = repo.suggest("чигри", false)
        assertTrue(tuvan.any { it.query == "чигир" })
        assertTrue(repo.suggest("%_\"", true).isEmpty())
        assertTrue(repo.suggest("абракадабраабракадабра", false).isEmpty())
    }
    @Test fun typoQuestionsAndBothSearchModesReturnLabelledRealResults() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var vm: DictionaryViewModel
            scenario.onActivity {
                vm = ViewModelProvider(it)[DictionaryViewModel::class.java]
                vm.ask("Кка будт по-тувинкси медвдь?")
            }
            val reply = withTimeout(20000) { vm.assistant.first { it.replies.size == 1 && !it.isLoading } }.replies.single()
            assertTrue(reply.answer, reply.answer.contains("медведь"))
            assertTrue(reply.answer, reply.answer.contains("Возможно"))
            assertTrue(reply.entries.any { it.word == "адыг" })
            assertTrue(reply.russianQuery)
            scenario.onActivity { vm.ask("чигри") }
            val bare = withTimeout(20000) { vm.assistant.first { it.replies.size == 2 && !it.isLoading } }.replies.last()
            assertFalse(bare.russianQuery)
            assertTrue(bare.entries.any { it.word == "чигир" })
            scenario.onActivity { vm.setDirection(SearchDirection.RUSSIAN); vm.onQueryChange("медвдь") }
            val ru = withTimeout(20000) { vm.state.first { it.query == "медвдь" && !it.isLoading } }
            assertTrue(ru.spellingHint.orEmpty().contains("медведь"))
            assertTrue(ru.entries.any { it.word == "адыг" })
            scenario.onActivity { vm.setDirection(SearchDirection.TUVAN); vm.onQueryChange("чигри") }
            val tyv = withTimeout(20000) { vm.state.first { it.query == "чигри" && !it.isLoading } }
            assertTrue(tyv.spellingHint.orEmpty().contains("чигир"))
            assertTrue(tyv.entries.any { it.word == "чигир" })
        }
    }
    @Test fun ambiguousSpellingDoesNotBecomeConfirmedConversationContext() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var vm: DictionaryViewModel
            scenario.onActivity {
                vm = ViewModelProvider(it)[DictionaryViewModel::class.java]
                vm.ask("Что означает чигир?")
            }
            withTimeout(20000) { vm.assistant.first { it.replies.size == 1 && !it.isLoading } }
            scenario.onActivity { vm.ask("Как по-русски агыг?") }
            val hint = withTimeout(20000) { vm.assistant.first { it.replies.size == 2 && !it.isLoading } }.replies.last()
            assertTrue(hint.answer, hint.isSuggestion)
            assertTrue(hint.entries.map { it.word }.toSet().size > 1)
            scenario.onActivity { vm.ask("А примеры?") }
            val followUp = withTimeout(20000) { vm.assistant.first { it.replies.size == 3 && !it.isLoading } }.replies.last()
            assertTrue(followUp.entries.isEmpty())
            assertTrue(followUp.examples.isEmpty())
        }
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
