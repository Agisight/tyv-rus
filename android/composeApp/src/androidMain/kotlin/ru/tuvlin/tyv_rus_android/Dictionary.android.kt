package ru.tuvlin.tyv_rus_android

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.text.style.ForegroundColorSpan
import android.text.Spanned
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.text.HtmlCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import java.io.File
import java.io.IOException
import java.text.Normalizer
import java.util.Locale

internal object AppCtx {
    lateinit var app: Context
}

// Increment when the bundled dictionary or its prepared search schema changes.
internal const val DICTIONARY_REVISION = 3
internal const val DICTIONARY_ASSET =
    "composeResources/ru.tuvlin.tyv_rus_android/files/tyv_rus_dict.db"

internal fun dictionaryFile(context: Context): File =
    File(context.noBackupFilesDir, "dictionary/tyv_rus_dict-$DICTIONARY_REVISION.db")

internal fun normalizeSearch(text: String): String =
    Normalizer.normalize(text.trim(), Normalizer.Form.NFC).lowercase(Locale.ROOT)

/** Exclusive upper bound in SQLite's Unicode binary order; wildcard characters stay literal. */
internal fun prefixUpperBound(prefix: String): String? {
    var end = prefix.length
    while (end > 0) {
        val start = prefix.offsetByCodePoints(end, -1)
        val point = prefix.codePointAt(start)
        if (point < Character.MAX_CODE_POINT) {
            val next = if (point == 0xD7FF) 0xE000 else point + 1
            return prefix.substring(0, start) + String(Character.toChars(next))
        }
        end = start
    }
    return null
}


private val markupBreak = Regex("""<\s*/?\s*br\s*/?\s*>|<m3\s*>""", RegexOption.IGNORE_CASE)
private val markupTag = Regex("<[^>]*>")
private val inlineSpace = Regex("[ \\t\\r]+")
private val metadataLine = Regex("""(?:[ⅠⅡⅢⅣⅤⅥⅦⅧⅨⅩIVX\d.\s]+|\{.*\})""")
private val searchToken = Regex("""[\p{L}\p{N}]+""")

internal fun articlePlainText(html: String): String {
    val normalized = html.replace(markupBreak, "\n")
    val plain = if ('&' in normalized) {
        HtmlCompat.fromHtml(normalized.replace("\n", "<br>"), HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
    } else normalized.replace(markupTag, "")
    return plain.lineSequence().map { it.replace(inlineSpace, " ").trim() }
        .filter(String::isNotEmpty).joinToString("\n")
}

/** Language boundaries come from the original purple example spans, never from spaces. */
internal fun articleExamples(html: String, limit: Int = Int.MAX_VALUE): List<DictionaryExample> = buildList {
    if (limit <= 0) return@buildList
    for (line in html.lineSequence().flatMap { it.split(markupBreak).asSequence() }) {
        if (!line.contains("#8A2BE2", ignoreCase = true)) continue
        val text = HtmlCompat.fromHtml(line, HtmlCompat.FROM_HTML_MODE_LEGACY)
        for (example in styledDictionaryExamples(text)) {
            add(example)
            if (size >= limit) return@buildList
        }
    }
}

internal fun styledDictionaryExamples(text: Spanned): List<DictionaryExample> = buildList {
    val spans = text.getSpans(0, text.length, ForegroundColorSpan::class.java)
        .filter { it.foregroundColor and 0xFFFFFF == 0x8A2BE2 }
        .map { text.getSpanStart(it) until text.getSpanEnd(it) }
        .filter { !it.isEmpty() }.sortedBy { it.first }
    val groups = mutableListOf<IntRange>()
    for (span in spans) {
        val previous = groups.lastOrNull()
        if (previous != null && (span.first <= previous.last + 1 ||
                text.subSequence(previous.last + 1, span.first).isBlank())) {
            groups[groups.lastIndex] = previous.first..maxOf(previous.last, span.last)
        } else groups.add(span)
    }
    for ((index, span) in groups.withIndex()) {
        val tuvan = text.subSequence(span.first, span.last + 1).toString().replace(inlineSpace, " ").trim()
        val end = groups.getOrNull(index + 1)?.first ?: text.length
        val russian = text.subSequence(span.last + 1, end).toString().replace(inlineSpace, " ").trim()
        if (tuvan.isNotEmpty() && russian.isNotEmpty()) add(DictionaryExample(tuvan, russian))
    }

}

private fun encodeExamples(examples: List<DictionaryExample>): String = JSONArray().apply {
    examples.forEach { put(JSONObject().put("tyv", it.tuvan).put("ru", it.russian)) }
}.toString()

private fun decodeExamples(json: String): List<DictionaryExample> {
    val array = JSONArray(json)
    return List(array.length()) { index ->
        val item = array.getJSONObject(index)
        DictionaryExample(item.getString("tyv"), item.getString("ru"))
    }
}

/** A process-wide read-only connection; initialization always happens on Dispatchers.IO. */
private object DictionaryDatabase {
    val db: SQLiteDatabase by lazy {
        val context = AppCtx.app
        val destination = dictionaryFile(context)
        val valid = destination.exists() && runCatching {
            SQLiteDatabase.openDatabase(destination.path, null, SQLiteDatabase.OPEN_READONLY).use {
                it.version == DICTIONARY_REVISION &&
                    it.rawQuery("SELECT search_key, preview, example_text, plain_text FROM dictionary LIMIT 1", null).use { c ->
                        c.moveToFirst() && !c.isNull(0)
                    }
            }
        }.getOrDefault(false)

        if (!valid) {
            check(destination.parentFile!!.isDirectory || destination.parentFile!!.mkdirs())
            val temporary = File.createTempFile("dictionary-", ".db", destination.parentFile)
            try {
                context.assets.open(DICTIONARY_ASSET).use { input ->
                    temporary.outputStream().use { output -> input.copyTo(output) }
                }
                prepare(temporary)
                // Readers never see an incompletely copied/indexed database.
                if (!temporary.renameTo(destination)) {
                    throw IOException("Could not install the prepared dictionary")
                }
            } finally {
                temporary.delete()
            }
        }
        SQLiteDatabase.openDatabase(destination.path, null, SQLiteDatabase.OPEN_READONLY)
    }

    // Build once on demand. No extra copy of the dictionary is bundled or written.
    val headingVocabulary: Map<Int, List<String>> by lazy {
        db.rawQuery("SELECT search_key FROM dictionary", null).use { cursor ->
            buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) }
                .groupBy(String::length)
        }
    }
    val meaningVocabulary: Map<Int, List<String>> by lazy {
        db.rawQuery("SELECT plain_text FROM dictionary", null).use { cursor ->
            buildSet {
                while (cursor.moveToNext()) searchToken.findAll(cursor.getString(0)).forEach { match ->
                    if (match.value.length in 4..32) add(match.value)
                }
            }.groupBy(String::length)
        }
    }

    private fun prepare(file: File) {
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            db.beginTransaction()
            try {
                db.execSQL("ALTER TABLE dictionary ADD COLUMN search_key TEXT")
                db.execSQL("ALTER TABLE dictionary ADD COLUMN preview TEXT")
                db.execSQL("ALTER TABLE dictionary ADD COLUMN example_text TEXT")
                db.execSQL("ALTER TABLE dictionary ADD COLUMN plain_text TEXT")
                db.compileStatement(
                    "UPDATE dictionary SET word_phrase = ?, search_key = ?, preview = ?, example_text = ?, plain_text = ? WHERE rowid = ?"
                ).use { update ->
                    db.rawQuery("SELECT rowid, word_phrase, html_text FROM dictionary", null).use { rows ->
                        while (rows.moveToNext()) {
                            val id = rows.getLong(0)
                            var head = rows.getString(1)
                            // The original first article has an empty heading. Its text and the
                            // legacy word list both identify it as the conjunction "а".
                            if (id == 1L && head.isBlank() &&
                                rows.getString(2).lineSequence().first().trim() == "а") {
                                head = "а"
                            }
                            check(head.isNotBlank()) { "Dictionary article $id has no heading" }
                            update.bindString(1, head)
                            update.bindString(2, normalizeSearch(head))
                            val html = rows.getString(2)
                            val lines = articlePlainText(html).lines().filter { it.isNotBlank() }
                            val preview = lines.firstOrNull {
                                it != head && !it.matches(metadataLine)
                            }.orEmpty().take(240)
                            val examples = articleExamples(html, limit = 3)
                            update.bindString(3, preview)
                            update.bindString(4, encodeExamples(examples))
                            update.bindString(5, normalizeSearch(lines.joinToString(" ")).replace('ё', 'е'))
                            update.bindLong(6, id)
                            update.executeUpdateDelete()
                        }
                    }
                }
                db.execSQL("CREATE INDEX idx_dictionary_search ON dictionary(search_key)")
                db.execSQL("CREATE VIRTUAL TABLE dictionary_fts USING fts4(word_phrase, plain_text, content='dictionary', tokenize=unicode61)")
                db.execSQL("INSERT INTO dictionary_fts(dictionary_fts) VALUES('rebuild')")
                db.version = DICTIONARY_REVISION
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }
}

actual class DictionaryRepository actual constructor() {
    actual suspend fun size(): Int = withContext(Dispatchers.IO) {
        DictionaryDatabase.db.rawQuery("SELECT COUNT(*) FROM dictionary", null).use {
            it.moveToFirst()
            it.getInt(0)
        }
    }

    actual suspend fun item(id: Int): Pair<String, String> = withContext(Dispatchers.IO) {
        DictionaryDatabase.db.rawQuery(
            "SELECT word_phrase, html_text FROM dictionary WHERE rowid = ?",
            arrayOf(id.toString())
        ).use {
            require(it.moveToFirst()) { "Unknown dictionary article: $id" }
            it.getString(0) to it.getString(1)
        }
    }

    actual suspend fun search(q: String): List<Pair<Int, String>> = withContext(Dispatchers.IO) {
        val key = normalizeSearch(q)
        val upper = prefixUpperBound(key)
        val (where, args) = when {
            key.isEmpty() -> "" to emptyArray<String>()
            upper == null -> " WHERE search_key >= ?" to arrayOf(key)
            else -> " WHERE search_key >= ? AND search_key < ?" to arrayOf(key, upper)
        }
        DictionaryDatabase.db.rawQuery(
            "SELECT rowid, word_phrase FROM dictionary$where ORDER BY search_key, rowid", args
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getInt(0) to cursor.getString(1))
            }
        }
    }

    actual suspend fun lookup(q: String, russian: Boolean): List<DictionaryEntry> = withContext(Dispatchers.IO) {
        val key = normalizeSearch(q)
        val db = DictionaryDatabase.db
        val sql: String
        val args: Array<String>
        val tokens = if (russian) searchToken.findAll(key.replace('ё', 'е')).map { it.value }.toList() else emptyList()
        if (russian) {
            if (tokens.isEmpty()) return@withContext emptyList()
            val match = tokens.joinToString(" AND ") { "\"$it*\"" }
            sql = "SELECT d.rowid, d.word_phrase, d.preview, d.example_text, d.html_text FROM dictionary d " +
                "JOIN dictionary_fts f ON f.docid = d.rowid WHERE dictionary_fts MATCH ? " +
                "ORDER BY d.search_key, d.rowid LIMIT 120"
            args = arrayOf(match)
        } else {
            val upper = prefixUpperBound(key)
            val (where, values) = when {
                key.isEmpty() -> "" to emptyArray<String>()
                upper == null -> " WHERE search_key >= ?" to arrayOf(key)
                else -> " WHERE search_key >= ? AND search_key < ?" to arrayOf(key, upper)
            }
            sql = "SELECT rowid, word_phrase, preview, example_text FROM dictionary$where ORDER BY search_key, rowid"
            args = values
        }
        val entries = db.rawQuery(sql, args).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val preview = if (russian) articlePlainText(cursor.getString(4)).lineSequence().firstOrNull { line ->
                        val words = searchToken.findAll(normalizeSearch(line).replace('ё', 'е')).map { it.value }.toList()
                        tokens.all { token -> words.any { it.startsWith(token) } }
                    }?.take(240) ?: cursor.getString(2) else cursor.getString(2)
                    add(DictionaryEntry(cursor.getInt(0), cursor.getString(1), preview,
                        decodeExamples(cursor.getString(3))))
                }
            }
        }
        val rankKey = key.replace('ё', 'е')
        if (russian) entries.sortedByDescending {
            val preview = normalizeSearch(it.preview).replace('ё', 'е')
            when {
                preview == rankKey -> 3
                preview.startsWith(rankKey) -> 2
                preview.contains(rankKey) -> 1
                else -> 0
            }
        } else entries
    }
    actual suspend fun suggest(q: String, russian: Boolean): List<SpellingSuggestion> {
        val key = normalizeSearch(q).let { if (russian) it.replace('ё', 'е') else it }
        if (key.length !in 4..32 || !key.all(Char::isLetter)) return emptyList()
        val vocabulary = withContext(Dispatchers.IO) {
            if (russian) DictionaryDatabase.meaningVocabulary else DictionaryDatabase.headingVocabulary
        }
        val candidates = withContext(Dispatchers.Default) {
            val limit = if (key.length > 7) 2 else 1
            val scored = buildList {
                for (length in key.length - limit..key.length + limit) {
                    for (word in vocabulary[length].orEmpty()) {
                        coroutineContext.ensureActive()
                        val distance = spellingDistance(key, word)
                        if (distance in 1..limit) add(word to distance)
                    }
                }
            }
            val best = scored.minOfOrNull { it.second }
            scored.filter { it.second == best }.sortedBy { it.first }.take(4).map { it.first }
        }
        return candidates.mapNotNull { corrected ->
            val entries = lookup(corrected, russian).let { found ->
                if (russian) found.take(4) else found.filter { normalizeSearch(it.word) == corrected }
            }
            if (entries.isEmpty()) null else SpellingSuggestion(corrected, entries)
        }
    }

}
