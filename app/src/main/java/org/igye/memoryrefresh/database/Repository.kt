package org.igye.memoryrefresh.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction
import org.igye.memoryrefresh.database.tables.CardsScheduleTable
import org.igye.memoryrefresh.database.tables.CardsTable
import org.igye.memoryrefresh.database.tables.TranslationCardsLogTable
import org.igye.memoryrefresh.database.tables.TranslationCardsTable

class Repository(
    context: Context,
    val dbName: String?,
    val cards: CardsTable,
    val cardsSchedule: CardsScheduleTable,
    val translationCards: TranslationCardsTable,
    val translationCardsLog: TranslationCardsLogTable,
) : SQLiteOpenHelper(context, dbName, null, 1) {
    private val allTables = listOf(cards, cardsSchedule, translationCards, translationCardsLog)

    override fun onCreate(db: SQLiteDatabase) {
        db.transaction {
            allTables.forEach { it.create(db) }
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        TODO("Not yet implemented")
    }

    override fun onConfigure(db: SQLiteDatabase?) {
        super.onConfigure(db)
        db!!.setForeignKeyConstraintsEnabled(true)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        allTables.forEach { it.prepareStatements(db) }
    }
}

