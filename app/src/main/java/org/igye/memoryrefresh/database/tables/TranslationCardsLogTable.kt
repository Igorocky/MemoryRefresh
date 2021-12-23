package org.igye.memoryrefresh.database.tables

import android.database.sqlite.SQLiteDatabase
import org.igye.memoryrefresh.database.Table
import java.time.Clock

class TranslationCardsLogTable(
    private val clock: Clock,
    private val translationCards: TranslationCardsTable
): Table(name = "TRANSLATION_CARDS_LOG") {
    val recId = "REC_ID"
    val timestamp = "TIMESTAMP"
    val cardId = "CARD_ID"
    val translation = "TRANSLATION"
    val matched = "MATCHED"

    override fun create(db: SQLiteDatabase) {
        db.execSQL("""
                CREATE TABLE $this (
                    $recId integer primary key,
                    $timestamp integer not null,
                    $cardId integer not null,
                    $translation text not null,
                    $matched integer not null check ($matched in (0,1))
                )
        """)
        db.execSQL("""
                CREATE INDEX IDX_${this}_CARD_ID on $this ( $cardId )
        """)
    }

    interface InsertStmt {operator fun invoke(cardId: Long, translation: String, matched: Boolean): Long } lateinit var insert: InsertStmt

    override fun prepareStatements(db: SQLiteDatabase) {
        val self = this
        insert = object : InsertStmt {
            val stmt = db.compileStatement("insert into $self ($timestamp,$cardId,$translation,$matched) values (?,?,?,?)")
            override fun invoke(cardId: Long, translation: String, matched: Boolean): Long {
                val currTime = clock.instant().toEpochMilli()
                stmt.bindLong(1, currTime)
                stmt.bindLong(2, cardId)
                stmt.bindString(3, translation)
                stmt.bindLong(4, if (matched) 1 else 0)
                return stmt.executeInsert()
            }
        }
    }
}