package org.igye.memoryrefresh.database.tables

import android.database.sqlite.SQLiteDatabase
import org.igye.memoryrefresh.common.Utils
import org.igye.memoryrefresh.database.CardType
import org.igye.memoryrefresh.database.TableWithVersioning
import java.time.Clock

class CardsTable(private val clock: Clock): TableWithVersioning(name = "CARDS") {
    val id = "ID"
    val type = "TYPE"
    val createdAt = "CREATED_AT"
    val paused = "PAUSED"
    val lastCheckedAt = "LAST_CHK_AT"

    override fun create(db: SQLiteDatabase) {
        db.execSQL("""
                CREATE TABLE $this (
                    $id integer primary key autoincrement,
                    $type integer not null,
                    $createdAt integer not null,
                    $paused integer not null check ($paused in (0,1)) default 0,
                    $lastCheckedAt integer not null
                )
        """)
        db.execSQL("""
                CREATE TABLE $ver (
                    ${ver.verId} integer primary key autoincrement,
                    ${ver.timestamp} integer not null,
                    
                    $id integer not null,
                    $type integer not null,
                    $createdAt integer not null
                )
        """)
    }

    interface InsertStmt {operator fun invoke(cardType: CardType, paused: Boolean): Long } lateinit var insert: InsertStmt
    interface UpdatePausedStmt {operator fun invoke(id:Long, paused: Boolean): Int} lateinit var updatePaused: UpdatePausedStmt
    interface UpdateLastCheckedStmt {operator fun invoke(id:Long, lastCheckedAt: Long): Int} lateinit var updateLastChecked: UpdateLastCheckedStmt
    interface DeleteStmt {operator fun invoke(id:Long): Int } lateinit var delete: DeleteStmt

    override fun prepareStatements(db: SQLiteDatabase) {
        val self = this
        val stmtVer = db.compileStatement("insert into $ver (${ver.timestamp},$id,$type,$createdAt) " +
                "select ?, $id, $type, $createdAt from $self where $id = ?")
        fun saveCurrentVersion(id: Long) {
            stmtVer.bindLong(1, clock.instant().toEpochMilli())
            stmtVer.bindLong(2, id)
            Utils.executeInsert(self.ver, stmtVer)
        }
        insert = object : InsertStmt {
            val stmt = db.compileStatement("insert into $self ($type,$createdAt,$paused,$lastCheckedAt) values (?,?,?,?)")
            override fun invoke(cardType: CardType, paused: Boolean): Long {
                val currTime = clock.instant().toEpochMilli()
                stmt.bindLong(1, cardType.intValue)
                stmt.bindLong(2, currTime)
                stmt.bindLong(3, if (paused) 1 else 0)
                stmt.bindLong(4, currTime)
                return Utils.executeInsert(self, stmt)
            }
        }
        updatePaused = object : UpdatePausedStmt {
            private val stmt = db.compileStatement("update $self set $paused = ? where $id = ?")
            override fun invoke(id: Long, paused: Boolean): Int {
                stmt.bindLong(1, if (paused) 1 else 0)
                stmt.bindLong(2, id)
                return Utils.executeUpdateDelete(self, stmt, 1)
            }
        }
        updateLastChecked = object : UpdateLastCheckedStmt {
            private val stmt = db.compileStatement("update $self set $lastCheckedAt = ? where $id = ?")
            override fun invoke(id: Long, lastCheckedAt: Long): Int {
                stmt.bindLong(1, lastCheckedAt)
                stmt.bindLong(2, id)
                return Utils.executeUpdateDelete(self, stmt, 1)
            }
        }
        delete = object : DeleteStmt {
            private val stmt = db.compileStatement("delete from $self where $id = ?")
            override fun invoke(id:Long): Int {
                saveCurrentVersion(id = id)
                stmt.bindLong(1, id)
                return Utils.executeUpdateDelete(self, stmt, 1)
            }
        }
    }
}