package org.igye.memoryrefresh.database

import android.database.sqlite.SQLiteDatabase
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import org.igye.memoryrefresh.common.Try

fun <T> SQLiteDatabase.doInTransactionEx(body: SQLiteDatabase.() -> T): T {
    return doInTransactionTry { Try { body() } }.get()
}

fun <T> SQLiteDatabase.doInTransaction(body: SQLiteDatabase.() -> T): Try<T> {
    return doInTransactionTry { Try { body() } }
}

fun <T> SQLiteDatabase.doInTransactionTry(body: SQLiteDatabase.() -> Try<T>): Try<T> {
    beginTransaction()
    try {
        val result = body()
        if (result.isSuccess()) {
            setTransactionSuccessful()
        }
        return result
    } finally {
        endTransaction()
    }
}

interface SelectedRow {
    fun getLong():Long
    fun getLongOrNull():Long?
    fun getString():String
    fun getStringOrNull():String?
    fun getDouble(): Double
    fun getDoubleOrNull(): Double?
}

interface SelectedRowInner: SelectedRow {
    fun reset()
}

data class SelectedRows<T>(val allRawsRead: Boolean, val rows: List<T>)

fun <T> SQLiteDatabase.select(
    query:String,
    args:Array<String>? = null,
    rowsMax:Int? = null,
    columnNames:Array<String>? = null,
    rowMapper:(SelectedRow) -> T,
): SelectedRows<T> {
    return rawQuery(query, args).use { cursor ->
        val result = ArrayList<T>()
        if (cursor.moveToFirst()) {
            val selectedRow = if (columnNames != null) {
                val columnIndexes = columnNames.map { cursor.getColumnIndexOrThrow(it) }
                object : SelectedRowInner {
                    override fun reset() {
                        curColumn = 0
                    }
                    private var curColumn = 0
                    override fun getLong(): Long = cursor.getLong(columnIndexes[curColumn++])
                    override fun getString(): String = cursor.getString(columnIndexes[curColumn++])
                    override fun getDouble(): Double = cursor.getDouble(columnIndexes[curColumn++])
                    override fun getLongOrNull(): Long? = cursor.getLongOrNull(columnIndexes[curColumn++])
                    override fun getStringOrNull(): String? = cursor.getStringOrNull(columnIndexes[curColumn++])
                    override fun getDoubleOrNull(): Double? = cursor.getDoubleOrNull(columnIndexes[curColumn++])
                }
            } else {
                object : SelectedRowInner {
                    override fun reset() {
                        curColumn = 0
                    }
                    private var curColumn = 0
                    override fun getLong(): Long = cursor.getLong(curColumn++)
                    override fun getString(): String = cursor.getString(curColumn++)
                    override fun getDouble(): Double = cursor.getDouble(curColumn++)
                    override fun getLongOrNull(): Long? = cursor.getLongOrNull(curColumn++)
                    override fun getStringOrNull(): String? = cursor.getStringOrNull(curColumn++)
                    override fun getDoubleOrNull(): Double? = cursor.getDoubleOrNull(curColumn++)
                }
            }
            while (!cursor.isAfterLast && (rowsMax == null || result.size < rowsMax)) {
                selectedRow.reset()
                result.add(rowMapper(selectedRow))
                cursor.moveToNext()
            }
        }
        SelectedRows(allRawsRead = cursor.isAfterLast, rows = result)
    }
}