package ru.tuvlin.tyv_rus_android

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DictionaryRepositoryTest {
    companion object {
        @BeforeClass @JvmStatic
        fun installLegacyFixture() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            AppCtx.app = context.applicationContext
            // A prior installation may contain a different cached schema. It must not
            // override the new bundled dictionary or lose the new search index.
            val old = context.getDatabasePath("tyv_rus_dict.db")
            old.parentFile!!.mkdirs()
            SQLiteDatabase.openOrCreateDatabase(old, null).use {
                it.execSQL("CREATE TABLE IF NOT EXISTS entry(head TEXT, html TEXT)")
                it.execSQL("DELETE FROM entry")
                it.execSQL("INSERT INTO entry VALUES ('obsolete-fixture', 'old content')")
            }
        }
    }

    @Test fun freshDictionaryPreservesEveryArticleAndRepairsFirstHeading() = runBlocking {
        val repo = DictionaryRepository()
        val heads = repo.search("")
        assertEquals(16627, repo.size())
        assertEquals(16627, heads.size)
        assertEquals(heads.size, heads.map { it.first }.toSet().size)
        assertTrue(heads.all { it.second.isNotBlank() })
        assertEquals("а", repo.item(1).first)
        assertTrue(repo.item(1).second.contains("союз"))
        assertTrue(repo.search("obsolete-fixture").isEmpty())
        for (head in listOf(heads.first(), heads[heads.size / 2], heads.last())) {
            val item = repo.item(head.first)
            assertEquals(head.second, item.first)
            assertTrue(item.second.isNotBlank())
        }
    }

    @Test fun homonymsRetainSeparateIdsAndFullDefinitions() = runBlocking {
        val repo = DictionaryRepository()
        val homonyms = repo.search("адыг").filter { it.second == "адыг" }
        assertEquals(2, homonyms.size)
        assertEquals(2, homonyms.map { it.first }.toSet().size)
        val definitions = homonyms.map { repo.item(it.first).second }
        assertEquals(2, definitions.toSet().size)
    }

    @Test fun tuvanLettersAndAbbreviationsIgnoreCase() = runBlocking {
        val repo = DictionaryRepository()
        for (prefix in listOf("а", "ө", "ң", "ү", "кно", "ссрэ")) {
            val lower = repo.search(prefix)
            assertTrue("No fixture rows for $prefix", lower.isNotEmpty())
            assertEquals(lower, repo.search(prefix.uppercase()))
        }
    }

    @Test fun percentAndUnderscoreAreLiteralInsteadOfSqlWildcards() = runBlocking {
        val repo = DictionaryRepository()
        assertTrue(repo.search("%").isEmpty())
        assertTrue(repo.search("_").isEmpty())
        assertTrue(repo.search("а%").isEmpty())
    }

    @Test fun preparedDatabaseUsesAnIndexForPrefixSearch() = runBlocking {
        DictionaryRepository().search("ө")
        val file = dictionaryFile(AppCtx.app)
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            assertEquals(DICTIONARY_REVISION, db.version)
            db.rawQuery(
                "EXPLAIN QUERY PLAN SELECT rowid, word_phrase FROM dictionary " +
                    "WHERE search_key >= ? AND search_key < ? ORDER BY search_key, rowid",
                arrayOf("ө", prefixUpperBound("ө"))
            ).use { cursor ->
                val plan = buildList { while (cursor.moveToNext()) add(cursor.getString(3)) }.joinToString()
                assertTrue(plan, plan.contains("SEARCH") && plan.contains("idx_dictionary_search"))
                assertFalse(plan, plan.contains("TEMP B-TREE"))
            }
        }
    }
}
