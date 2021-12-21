package org.igye.memoryrefresh.database.tables.v1

import android.database.sqlite.SQLiteDatabase
import org.igye.memoryrefresh.MemoryRefreshException
import org.igye.memoryrefresh.database.ChangeType
import org.igye.memoryrefresh.database.TableWithVersioning
import java.time.Clock

class CardsScheduleTable(private val clock: Clock, private val cards: CardsTable): TableWithVersioning(name = "CARDS_SCHEDULE") {
    val cardId = "CARD_ID"
    val lastAccessedAt = "LAST_ACCESSED_AT"
    val nextAccessInSec = "NEXT_ACCESS_IN_SEC"
    val nextAccessAt = "NEXT_ACCESS_AT"

    override fun create(db: SQLiteDatabase) {
        db.execSQL("""
                CREATE TABLE $this (
                    $cardId integer unique references $cards(${cards.id}) on update restrict on delete restrict,
                    $lastAccessedAt integer not null,
                    $nextAccessInSec integer not null,
                    $nextAccessAt integer not null
                )
        """)
        db.execSQL("""
                CREATE TABLE $ver (
                    ${ver.verId} integer primary key,
                    ${ver.timestamp} integer not null,
                    ${ver.changeType} integer not null,
                    
                    $cardId integer not null,
                    $lastAccessedAt integer not null,
                    $nextAccessInSec integer not null,
                    $nextAccessAt integer not null
                )
        """)
    }

    interface InsertStmt {operator fun invoke(cardId: Long, lastAccessedAt: Long, nextAccessInSec: Long, nextAccessAt: Long): Long }
        lateinit var insertStmt: InsertStmt
    interface UpdateStmt {operator fun invoke(cardId: Long, lastAccessedAt: Long, nextAccessInSec: Long, nextAccessAt: Long): Int}
        lateinit var updateStmt: UpdateStmt
    interface DeleteStmt {operator fun invoke(cardId: Long): Int }
        lateinit var deleteStmt: DeleteStmt

    override fun prepareStatements(db: SQLiteDatabase) {
        val self = this
        val stmtVer = db.compileStatement("insert into $ver (${ver.timestamp},${ver.changeType},$cardId,$lastAccessedAt,$nextAccessInSec,$nextAccessAt) " +
                "select ?, ?, $cardId, $lastAccessedAt, $nextAccessInSec, $nextAccessAt from $self where $cardId = ?")
        fun saveCurrentVersion(cardId: Long, changeType: ChangeType) {
            stmtVer.bindLong(1, clock.instant().toEpochMilli())
            stmtVer.bindLong(2, changeType.intValue)
            stmtVer.bindLong(3, cardId)
            if (stmtVer.executeUpdateDelete() != 1) {
                throw MemoryRefreshException("stmtVer.executeUpdateDelete() != 1")
            }
        }
        insertStmt = object : InsertStmt {
            val stmt = db.compileStatement("insert into $self ($cardId,$lastAccessedAt,$nextAccessInSec,$nextAccessAt) values (?,?,?,?)")
            override fun invoke(cardId: Long, lastAccessedAt: Long, nextAccessInSec: Long, nextAccessAt: Long): Long {
                stmt.bindLong(1, cardId)
                stmt.bindLong(2, lastAccessedAt)
                stmt.bindLong(3, nextAccessInSec)
                stmt.bindLong(4, nextAccessAt)
                return stmt.executeInsert()
            }
        }
        updateStmt = object : UpdateStmt {
            private val stmt = db.compileStatement("update $self set $lastAccessedAt = ?, $nextAccessInSec = ?, $nextAccessAt = ?  where $cardId = ?")
            override fun invoke(cardId: Long, lastAccessedAt: Long, nextAccessInSec: Long, nextAccessAt: Long): Int {
                saveCurrentVersion(cardId = cardId, changeType = ChangeType.UPDATE)
                stmt.bindLong(1, lastAccessedAt)
                stmt.bindLong(2, nextAccessInSec)
                stmt.bindLong(3, nextAccessAt)
                stmt.bindLong(4, cardId)
                return stmt.executeUpdateDelete()
            }

        }
        deleteStmt = object : DeleteStmt {
            private val stmt = db.compileStatement("delete from $self where $cardId = ?")
            override fun invoke(cardId: Long): Int {
                saveCurrentVersion(cardId = cardId, changeType = ChangeType.DELETE)
                stmt.bindLong(1, cardId)
                return stmt.executeUpdateDelete()
            }
        }
    }
}