@file:OptIn(InternalResourceApi::class)

package ru.tuvlin.tyv_rus_android

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.readResourceBytes
import java.io.File
import java.util.Locale

// Храним applicationContext. Установим в MainActivity.onCreate().
internal object AppCtx {
    lateinit var app: Context
}

/** Копируем БД из composeResources/files в .../databases/ один раз. */
@OptIn(ExperimentalResourceApi::class)
private fun ensureDbCopiedOnce(dbName: String = "tyv_rus_dict.db"): String {
    val ctx = AppCtx.app
    val dstFile = ctx.getDatabasePath(dbName)      // .../databases/tyv_rus_dict.db
    if (!dstFile.exists()) {
        dstFile.parentFile?.mkdirs()
        // readResourceBytes доступен в common/Android, не требует suspend
        val bytes = runBlocking { readResourceBytes("files/$dbName") }
        File(dstFile.absolutePath).outputStream().use { it.write(bytes) }
    }
    return dstFile.absolutePath
}

actual class DictionaryRepository actual constructor() {

    private val db: SQLiteDatabase by lazy {
        val path = ensureDbCopiedOnce("tyv_rus_dict.db")
        SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)
    }

    // Кэш заголовков (индекс → head), чтобы сохранить прежнюю логику индексов
    private val heads: List<String> by lazy {
        db.rawQuery(
            "SELECT head FROM entry ORDER BY lower(head) ASC", emptyArray()
        ).use { c ->
            val list = ArrayList<String>(c.count.coerceAtLeast(1024))
            val idx = c.getColumnIndexOrThrow("head")
            while (c.moveToNext()) list.add(c.getString(idx))
            list
        }
    }

    actual fun size(): Int = heads.size

    actual fun item(i: Int): Pair<String, String> {
        val w = heads.getOrNull(i) ?: return "" to ""
        val html = db.rawQuery(
            "SELECT html FROM entry WHERE head = ? LIMIT 1", arrayOf(w)
        ).use { c -> if (c.moveToFirst()) c.getString(0) else "" }
        return w to html
    }

    actual fun search(q: String): List<Pair<Int, String>> {
        if (q.isBlank()) return heads.mapIndexed { i, w -> i to w }

        val like = "${q.lowercase(Locale.ROOT)}%"   // префиксный поиск
        val rows = db.rawQuery(
            "SELECT head FROM entry WHERE lower(head) LIKE ? ORDER BY lower(head) ASC",
            arrayOf(like)
        ).use { c ->
            val out = ArrayList<String>(minOf(c.count, 500))
            while (c.moveToNext()) out.add(c.getString(0))
            out
        }

        val indexOf = heads.withIndex().associate { it.value to it.index }
        return rows.mapNotNull { h -> indexOf[h]?.let { it to h } }
    }
}
