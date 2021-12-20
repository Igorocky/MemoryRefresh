package org.igye.memoryrefresh.database.tables.v1

import android.database.sqlite.SQLiteDatabase
import org.igye.memoryrefresh.MemoryRefreshException
import org.igye.memoryrefresh.database.CardType
import org.igye.memoryrefresh.database.ChangeType
import org.igye.memoryrefresh.database.TableWithVersioning
import java.time.Instant

class CardsTable: TableWithVersioning(name = "CARDS") {
    val id = "ID"
    val type = "TYPE"
    val createdAt = "CREATED_AT"

    override fun create(db: SQLiteDatabase) {
        db.execSQL("""
                CREATE TABLE $this (
                    $id integer primary key,
                    $type integer not null,
                    $createdAt integer not null
                )
        """)
        db.execSQL("""
                CREATE TABLE $ver (
                    ${ver.verId} integer primary key,
                    ${ver.timestamp} integer not null,
                    ${ver.changeType} integer not null,
                    
                    $id integer not null,
                    $type integer not null,
                    $createdAt integer not null
                )
        """)
    }

    interface InsertStmt {operator fun invoke(cardType: CardType): Long } lateinit var insertStmt: InsertStmt
    interface UpdateStmt {operator fun invoke(id:Long, cardType: CardType): Int} lateinit var updateStmt: UpdateStmt
    interface DeleteStmt {operator fun invoke(id:Long): Int } lateinit var deleteStmt: DeleteStmt

    override fun prepareStatements(db: SQLiteDatabase) {
        val self = this
        val stmtVer = db.compileStatement("insert into $ver (${ver.timestamp},${ver.changeType},$id,$type,$createdAt) " +
                "select ?, ?, $id, $type, $createdAt from $self where $id = ?")
        fun saveCurrentVersion(id: Long, changeType: ChangeType) {
            stmtVer.bindLong(1, Instant.now().toEpochMilli())
            stmtVer.bindLong(2, changeType.intValue)
            stmtVer.bindLong(3, id)
            if (stmtVer.executeUpdateDelete() != 1) {
                throw MemoryRefreshException("stmtVer.executeUpdateDelete() != 1")
            }
        }
        insertStmt = object : InsertStmt {
            val stmt = db.compileStatement("insert into $self ($type,$createdAt) values (?,?)")
            override fun invoke(cardType: CardType): Long {
                val currTime = Instant.now().toEpochMilli()
                stmt.bindLong(1, cardType.intValue)
                stmt.bindLong(2, currTime)
                return stmt.executeInsert()
            }
        }
        updateStmt = object : UpdateStmt {
            private val stmt = db.compileStatement("update $self set $type = ? where $id = ?")
            override fun invoke(id: Long, cardType: CardType): Int {
                saveCurrentVersion(id = id, changeType = ChangeType.UPDATE)
                stmt.bindLong(1, cardType.intValue)
                stmt.bindLong(2, id)
                return stmt.executeUpdateDelete()
            }

        }
        deleteStmt = object : DeleteStmt {
            private val stmt = db.compileStatement("delete from $self where $id = ?")
            override fun invoke(id:Long): Int {
                saveCurrentVersion(id = id, changeType = ChangeType.DELETE)
                stmt.bindLong(1, id)
                return stmt.executeUpdateDelete()
            }
        }
    }
}