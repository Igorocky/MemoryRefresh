package org.igye.memoryrefresh.database

import android.database.sqlite.SQLiteDatabase

abstract class Table(val tableName: String) {
    open fun create(db: SQLiteDatabase) {}
    open fun prepareStatements(db: SQLiteDatabase) {}
    override fun toString() = tableName
}