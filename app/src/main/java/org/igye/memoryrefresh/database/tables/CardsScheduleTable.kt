package org.igye.memoryrefresh.database.tables

import android.database.sqlite.SQLiteDatabase
import org.igye.memoryrefresh.ErrorCode
import org.igye.memoryrefresh.common.MemoryRefreshException
import org.igye.memoryrefresh.database.TableWithVersioning
import java.time.Clock

class CardsScheduleTable(private val clock: Clock, private val cards: CardsTable): TableWithVersioning(name = "CARDS_SCHEDULE") {
    val cardId = "CARD_ID"
    val updatedAt = "UPDATED_AT"
    val delay = "DELAY"
    val randomFactor = "RAND"
    val nextAccessInMillis = "NEXT_ACCESS_IN_MILLIS"
    val nextAccessAt = "NEXT_ACCESS_AT"

    override fun create(db: SQLiteDatabase) {
        db.execSQL("""
                CREATE TABLE $this (
                    $cardId integer unique references $cards(${cards.id}) on update restrict on delete restrict,
                    $updatedAt integer not null,
                    $delay text not null,
                    $randomFactor real not null,
                    $nextAccessInMillis integer not null,
                    $nextAccessAt integer not null
                )
        """)
        db.execSQL("""
                CREATE TABLE $ver (
                    ${ver.verId} integer primary key,
                    ${ver.timestamp} integer not null,
                    
                    $cardId integer not null,
                    $updatedAt integer not null,
                    $delay text not null,
                    $randomFactor real not null,
                    $nextAccessInMillis integer not null,
                    $nextAccessAt integer not null
                )
        """)
    }

    interface InsertStmt {operator fun invoke(cardId: Long, timestamp: Long, delay: String, randomFactor: Double, nextAccessInMillis: Long, nextAccessAt: Long): Long }
        lateinit var insert: InsertStmt
    interface UpdateStmt {operator fun invoke(cardId: Long, timestamp: Long, delay: String, randomFactor: Double, nextAccessInMillis: Long, nextAccessAt: Long): Int}
        lateinit var update: UpdateStmt
    interface DeleteStmt {operator fun invoke(cardId: Long): Int }
        lateinit var delete: DeleteStmt

    override fun prepareStatements(db: SQLiteDatabase) {
        val self = this
        val stmtVer = db.compileStatement("insert into $ver (${ver.timestamp},$cardId,$updatedAt,$delay,$randomFactor,$nextAccessInMillis,$nextAccessAt) " +
                "select ?, $cardId, $updatedAt, $delay, $randomFactor, $nextAccessInMillis, $nextAccessAt from $self where $cardId = ?")
        fun saveCurrentVersion(cardId: Long, timestamp: Long) {
            stmtVer.bindLong(1, timestamp)
            stmtVer.bindLong(2, cardId)
            if (stmtVer.executeUpdateDelete() != 1) {
                throw MemoryRefreshException(msg = "stmtVer.executeUpdateDelete() != 1", errCode = ErrorCode.GENERAL)
            }
        }
        insert = object : InsertStmt {
            val stmt = db.compileStatement("insert into $self ($cardId,$updatedAt,$delay,$randomFactor,$nextAccessInMillis,$nextAccessAt) values (?,?,?,?,?,?)")
            override fun invoke(cardId: Long, timestamp: Long, delay: String, randomFactor: Double, nextAccessInMillis: Long, nextAccessAt: Long): Long {
                stmt.bindLong(1, cardId)
                stmt.bindLong(2, timestamp)
                stmt.bindString(3, delay)
                stmt.bindDouble(4, randomFactor)
                stmt.bindLong(5, nextAccessInMillis)
                stmt.bindLong(6, nextAccessAt)
                return stmt.executeInsert()
            }
        }
        update = object : UpdateStmt {
            private val stmt = db.compileStatement(
                "update $self set $updatedAt = ?, $delay = ?, $randomFactor = ?, $nextAccessInMillis = ?, $nextAccessAt = ?  where $cardId = ?")
            override fun invoke(cardId: Long, timestamp: Long, delay: String, randomFactor: Double, nextAccessInMillis: Long, nextAccessAt: Long): Int {
                saveCurrentVersion(cardId = cardId, timestamp = timestamp)
                stmt.bindLong(1, timestamp)
                stmt.bindString(2, delay)
                stmt.bindDouble(3, randomFactor)
                stmt.bindLong(4, nextAccessInMillis)
                stmt.bindLong(5, nextAccessAt)
                stmt.bindLong(6, cardId)
                return stmt.executeUpdateDelete()
            }

        }
        delete = object : DeleteStmt {
            private val stmt = db.compileStatement("delete from $self where $cardId = ?")
            override fun invoke(cardId: Long): Int {
                saveCurrentVersion(cardId = cardId, timestamp = clock.instant().toEpochMilli())
                stmt.bindLong(1, cardId)
                return stmt.executeUpdateDelete()
            }
        }
    }
}