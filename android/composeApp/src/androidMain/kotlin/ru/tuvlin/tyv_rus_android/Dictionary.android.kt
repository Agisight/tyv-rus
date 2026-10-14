package ru.tuvlin.tyv_rus_android

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.Normalizer
import java.util.Locale

internal object AppCtx {
    lateinit var app: Context
}

// Increment when the bundled dictionary or its prepared search schema changes.
internal const val DICTIONARY_REVISION = 1
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

/** A process-wide read-only connection; initialization always happens on Dispatchers.IO. */
private object DictionaryDatabase {
    val db: SQLiteDatabase by lazy {
        val context = AppCtx.app
        val destination = dictionaryFile(context)
        val valid = destination.exists() && runCatching {
            SQLiteDatabase.openDatabase(destination.path, null, SQLiteDatabase.OPEN_READONLY).use {
                it.version == DICTIONARY_REVISION &&
                    it.rawQuery("SELECT search_key FROM dictionary LIMIT 1", null).use { c ->
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

    private fun prepare(file: File) {
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            db.beginTransaction()
            try {
                db.execSQL("ALTER TABLE dictionary ADD COLUMN search_key TEXT")
                db.compileStatement(
                    "UPDATE dictionary SET word_phrase = ?, search_key = ? WHERE rowid = ?"
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
                            update.bindLong(3, id)
                            update.executeUpdateDelete()
                        }
                    }
                }
                db.execSQL("CREATE INDEX idx_dictionary_search ON dictionary(search_key)")
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
}
