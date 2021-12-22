package org.igye.memoryrefresh.database.tables

import android.database.sqlite.SQLiteDatabase
import org.igye.memoryrefresh.ErrorCode
import org.igye.memoryrefresh.MemoryRefreshException
import org.igye.memoryrefresh.database.TableWithVersioning
import java.time.Clock

class CardsScheduleTable(private val clock: Clock, private val cards: CardsTable): TableWithVersioning(name = "CARDS_SCHEDULE") {
    val cardId = "CARD_ID"
    val delay = "DELAY"
    val nextAccessInMillis = "NEXT_ACCESS_IN_MILLIS"
    val nextAccessAt = "NEXT_ACCESS_AT"

    override fun create(db: SQLiteDatabase) {
        db.execSQL("""
                CREATE TABLE $this (
                    $cardId integer unique references $cards(${cards.id}) on update restrict on delete restrict,
                    $delay text not null,
                    $nextAccessInMillis integer not null,
                    $nextAccessAt integer not null
                )
        """)
        db.execSQL("""
                CREATE TABLE $ver (
                    ${ver.verId} integer primary key,
                    ${ver.timestamp} integer not null,
                    
                    $cardId integer not null,
                    $delay text not null,
                    $nextAccessInMillis integer not null,
                    $nextAccessAt integer not null
                )
        """)
    }

    interface InsertStmt {operator fun invoke(cardId: Long, delay: String, nextAccessInMillis: Long, nextAccessAt: Long): Long }
        lateinit var insertStmt: InsertStmt
    interface UpdateStmt {operator fun invoke(cardId: Long, delay: String, nextAccessInMillis: Long, nextAccessAt: Long): Int}
        lateinit var updateStmt: UpdateStmt
    interface DeleteStmt {operator fun invoke(cardId: Long): Int }
        lateinit var deleteStmt: DeleteStmt

    override fun prepareStatements(db: SQLiteDatabase) {
        val self = this
        val stmtVer = db.compileStatement("insert into $ver (${ver.timestamp},$cardId,$delay,$nextAccessInMillis,$nextAccessAt) " +
                "select ?, $cardId, $delay, $nextAccessInMillis, $nextAccessAt from $self where $cardId = ?")
        fun saveCurrentVersion(cardId: Long) {
            stmtVer.bindLong(1, clock.instant().toEpochMilli())
            stmtVer.bindLong(2, cardId)
            if (stmtVer.executeUpdateDelete() != 1) {
                throw MemoryRefreshException(msg = "stmtVer.executeUpdateDelete() != 1", errCode = ErrorCode.GENERAL)
            }
        }
        insertStmt = object : InsertStmt {
            val stmt = db.compileStatement("insert into $self ($cardId,$delay,$nextAccessInMillis,$nextAccessAt) values (?,?,?,?)")
            override fun invoke(cardId: Long, delay: String, nextAccessInMillis: Long, nextAccessAt: Long): Long {
                stmt.bindLong(1, cardId)
                stmt.bindString(2, delay)
                stmt.bindLong(3, nextAccessInMillis)
                stmt.bindLong(4, nextAccessAt)
                return stmt.executeInsert()
            }
        }
        updateStmt = object : UpdateStmt {
            private val stmt = db.compileStatement("update $self set $delay = ?, $nextAccessInMillis = ?, $nextAccessAt = ?  where $cardId = ?")
            override fun invoke(cardId: Long, delay: String, nextAccessInMillis: Long, nextAccessAt: Long): Int {
                saveCurrentVersion(cardId = cardId)
                stmt.bindString(1, delay)
                stmt.bindLong(2, nextAccessInMillis)
                stmt.bindLong(3, nextAccessAt)
                stmt.bindLong(4, cardId)
                return stmt.executeUpdateDelete()
            }

        }
        deleteStmt = object : DeleteStmt {
            private val stmt = db.compileStatement("delete from $self where $cardId = ?")
            override fun invoke(cardId: Long): Int {
                saveCurrentVersion(cardId = cardId)
                stmt.bindLong(1, cardId)
                return stmt.executeUpdateDelete()
            }
        }
    }
}