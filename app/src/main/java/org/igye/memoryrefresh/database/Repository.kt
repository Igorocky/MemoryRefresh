package org.igye.memoryrefresh.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction
import org.igye.memoryrefresh.ErrorCode
import org.igye.memoryrefresh.common.MemoryRefreshException
import org.igye.memoryrefresh.database.tables.*

class Repository(
    context: Context,
    val dbName: String?,
    val cards: CardsTable,
    val cardsSchedule: CardsScheduleTable,
    val translationCards: TranslationCardsTable,
    val translationCardsLog: TranslationCardsLogTable,
    val tags: TagsTable,
    val cardToTag: CardToTagTable
) : SQLiteOpenHelper(context, dbName, null, 2) {
    private val allTables = listOf(cards, cardsSchedule, translationCards, translationCardsLog, tags, cardToTag)

    override fun onCreate(db: SQLiteDatabase) {
        db.transaction {
            allTables.forEach { it.create(db) }
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (newVersion < oldVersion) {
            throw MemoryRefreshException(
                msg = "Downgrade of the database is not supported.",
                errCode = ErrorCode.DOWNGRADE_IS_NOT_SUPPORTED
            )
        }
        var version = oldVersion
        while (version < newVersion) {
            incVersion(db, version)
            version++
        }
    }

    override fun onConfigure(db: SQLiteDatabase?) {
        super.onConfigure(db)
        db!!.setForeignKeyConstraintsEnabled(true)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        allTables.forEach { it.prepareStatements(db) }
    }

    private fun incVersion(db: SQLiteDatabase, oldVersion: Int) {
        if (oldVersion == 1) {
            upgradeFromV1ToV2(db)
        } else {
            throw MemoryRefreshException(
                msg = "Upgrade for a database of version $oldVersion is not implemented.",
                errCode = ErrorCode.UPGRADE_IS_NOT_IMPLEMENTED
            )
        }
    }

    private fun upgradeFromV1ToV2(db: SQLiteDatabase) {
        db.execSQL("""
                ALTER TABLE $cards ADD COLUMN ${cards.paused} integer not null check (${cards.paused} in (0,1)) default 0
        """.trimIndent())
        tags.create(db)
        cardToTag.create(db)
    }
}

