package org.igye.memoryrefresh.database.tables

import android.database.sqlite.SQLiteDatabase
import org.igye.memoryrefresh.common.Utils
import org.igye.memoryrefresh.database.TableWithVersioning
import java.time.Clock

class NoteCardsTable(
    private val clock: Clock,
    private val cards: CardsTable,
): TableWithVersioning(name = "NOTE_CARDS") {
    val cardId = "CARD_ID"
    val text = "TEXT"

    override fun create(db: SQLiteDatabase) {
        db.execSQL("""
                CREATE TABLE ${this} (
                    $cardId integer unique references ${cards}(${cards.id}) on update restrict on delete restrict,
                    $text text not null
                )
        """)
        db.execSQL("""
                CREATE TABLE $ver (
                    ${ver.verId} integer primary key autoincrement,
                    ${ver.timestamp} integer not null,
                    
                    $cardId integer not null,
                    $text text not null
                )
        """)
    }

    interface InsertStmt {operator fun invoke(cardId: Long, text: String): Long } lateinit var insert: InsertStmt
    interface UpdateStmt {operator fun invoke(cardId: Long, text: String): Int} lateinit var update: UpdateStmt
    interface DeleteStmt {operator fun invoke(cardId: Long): Int } lateinit var delete: DeleteStmt

    override fun prepareStatements(db: SQLiteDatabase) {
        val self = this
        val stmtVer = db.compileStatement("insert into $ver (${ver.timestamp},$cardId,$text) " +
                "select ?, $cardId, $text from $self where $cardId = ?")
        fun saveCurrentVersion(cardId: Long) {
            stmtVer.bindLong(1, clock.instant().toEpochMilli())
            stmtVer.bindLong(2, cardId)
            Utils.executeInsert(self.ver, stmtVer)
        }
        insert = object : InsertStmt {
            val stmt = db.compileStatement("insert into $self ($cardId,$text) values (?,?)")
            override fun invoke(cardId: Long, text: String): Long {
                stmt.bindLong(1, cardId)
                stmt.bindString(2, text)
                return Utils.executeInsert(self, stmt)
            }
        }
        update = object : UpdateStmt {
            private val stmt = db.compileStatement("update $self set $text = ?  where $cardId = ?")
            override fun invoke(cardId: Long, text: String): Int {
                saveCurrentVersion(cardId = cardId)
                stmt.bindString(1, text)
                stmt.bindLong(2, cardId)
                return Utils.executeUpdateDelete(self, stmt, 1)
            }

        }
        delete = object : DeleteStmt {
            private val stmt = db.compileStatement("delete from $self where $cardId = ?")
            override fun invoke(cardId: Long): Int {
                saveCurrentVersion(cardId = cardId)
                stmt.bindLong(1, cardId)
                return Utils.executeUpdateDelete(self, stmt, 1)
            }
        }
    }
}