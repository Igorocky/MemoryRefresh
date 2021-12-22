package org.igye.memoryrefresh.database

import android.database.sqlite.SQLiteDatabase
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import org.igye.memoryrefresh.common.Try

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

data class SelectedRows<T>(val allRawsRead: Boolean, val rows: List<T>)

fun <T> SQLiteDatabase.select(
    query:String,
    args:Array<String>? = null,
    rowsMax:Int? = null,
    columnNames:Array<String>,
    rowMapper:(SelectedRow) -> T,
): SelectedRows<T> {
    return rawQuery(query, args).use { cursor ->
        val result = ArrayList<T>()
        if (cursor.moveToFirst()) {
            val columnIndexes = columnNames.map { cursor.getColumnIndexOrThrow(it) }
            while (!cursor.isAfterLast && (rowsMax == null || result.size < rowsMax)) {
                result.add(rowMapper(object : SelectedRow{
                    private var curColumn = 0
                    override fun getLong(): Long {
                        return cursor.getLong(columnIndexes[curColumn++])
                    }
                    override fun getString():String {
                        return cursor.getString(columnIndexes[curColumn++])
                    }
                    override fun getDouble():Double {
                        return cursor.getDouble(columnIndexes[curColumn++])
                    }
                    override fun getLongOrNull(): Long? {
                        return cursor.getLongOrNull(columnIndexes[curColumn++])
                    }
                    override fun getStringOrNull():String? {
                        return cursor.getStringOrNull(columnIndexes[curColumn++])
                    }
                    override fun getDoubleOrNull():Double? {
                        return cursor.getDoubleOrNull(columnIndexes[curColumn++])
                    }
                }))
                cursor.moveToNext()
            }
        }
        SelectedRows(allRawsRead = cursor.isAfterLast, rows = result)
    }
}