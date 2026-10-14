package ru.tuvlin.tyv_rus_android

import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DictionaryScreenTest {
    private suspend fun awaitState(
        vm: DictionaryViewModel,
        predicate: (UiState) -> Boolean
    ): UiState = withTimeout(10_000) { vm.state.first(predicate) }

    @Test fun screenLoadsAndRapidInputOnlyShowsLatestResults() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var vm: DictionaryViewModel
            scenario.onActivity { vm = ViewModelProvider(it)[DictionaryViewModel::class.java] }
            val initial = awaitState(vm) { !it.isLoading }
            assertNull(initial.error)
            assertEquals(16627, initial.items.size)
            scenario.onActivity {
                vm.onQueryChange("а")
                vm.onQueryChange("Ө")
                vm.onQueryChange("Ү")
            }
            val result = awaitState(vm) { it.query == "Ү" && !it.isLoading }
            assertNull(result.error)
            assertTrue(result.items.isNotEmpty())
            assertEquals(DictionaryRepository().search("ү"), result.items)
        }
    }

    @Test fun screenOpensBothHomonymsAndCrossReferenceOffersBothArticles() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var vm: DictionaryViewModel
            scenario.onActivity { vm = ViewModelProvider(it)[DictionaryViewModel::class.java] }
            awaitState(vm) { !it.isLoading }
            scenario.onActivity { vm.onQueryChange("адыг") }
            val result = awaitState(vm) { it.query == "адыг" && !it.isLoading }
            val homonyms = result.items.filter { it.second == "адыг" }
            assertEquals(2, homonyms.size)
            val definitions = mutableSetOf<String>()
            for ((id, head) in homonyms) {
                scenario.onActivity { vm.onSelect(id) }
                val article = awaitState(vm) { it.selected == id }
                assertEquals(head, article.selectedWord)
                assertTrue(article.selectedHtml.isNotBlank())
                definitions.add(article.selectedHtml)
                scenario.onActivity { vm.onBack() }
            }
            assertEquals(2, definitions.size)
            scenario.onActivity { vm.onSelect(homonyms.first().first) }
            awaitState(vm) { it.selected != null }
            scenario.onActivity { vm.openByHead("адыг") }
            val linked = awaitState(vm) { it.selected == null && it.query == "адыг" && !it.isLoading }
            assertEquals(2, linked.items.count { it.second == "адыг" })
        }
    }
}
