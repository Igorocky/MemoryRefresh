package org.igye.memoryrefresh.database.tables.v1

import android.database.sqlite.SQLiteDatabase
import org.igye.memoryrefresh.database.Table
import java.time.Instant

class TranslationCardsLogTable(private val translationCards: TranslationCardsTable): Table(name = "TRANSLATION_CARDS_LOG") {
    val histRecId = "REC_ID"
    val timestamp = "TIMESTAMP"
    val cardId = "CARD_ID"
    val translation = "TRANSLATION"

    override fun create(db: SQLiteDatabase) {
        db.execSQL("""
                CREATE TABLE $this (
                    $histRecId integer primary key,
                    $timestamp integer not null,
                    $cardId integer references $translationCards(${translationCards.cardId}) on update restrict on delete no action,
                    $translation text not null
                )
        """)
    }

    interface InsertStmt {operator fun invoke(cardId: Long, translation: String): Long } lateinit var insertStmt: InsertStmt

    override fun prepareStatements(db: SQLiteDatabase) {
        val self = this
        insertStmt = object : InsertStmt {
            val stmt = db.compileStatement("insert into $self ($timestamp,$cardId,$translation) values (?,?,?)")
            override fun invoke(cardId: Long, translation: String): Long {
                val currTime = Instant.now().toEpochMilli()
                stmt.bindLong(1, currTime)
                stmt.bindLong(2, cardId)
                stmt.bindString(3, translation)
                return stmt.executeInsert()
            }
        }
    }
}