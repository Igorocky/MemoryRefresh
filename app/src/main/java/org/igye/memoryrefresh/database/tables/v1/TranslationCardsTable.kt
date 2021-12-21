package org.igye.memoryrefresh.database.tables.v1

import android.database.sqlite.SQLiteDatabase
import org.igye.memoryrefresh.MemoryRefreshException
import org.igye.memoryrefresh.database.ChangeType
import org.igye.memoryrefresh.database.TableWithVersioning
import java.time.Clock

class TranslationCardsTable(
    private val clock: Clock,
    private val cards: CardsTable,
): TableWithVersioning(name = "TRANSLATION_CARDS") {
    val cardId = "CARD_ID"
    val textToTranslate = "TEXT_TO_TRANSLATE"
    val translation = "TRANSLATION"

    override fun create(db: SQLiteDatabase) {
        db.execSQL("""
                CREATE TABLE ${this} (
                    $cardId integer unique references ${cards}(${cards.id}) on update restrict on delete restrict,
                    $textToTranslate text not null,
                    $translation text not null
                )
        """)
        db.execSQL("""
                CREATE TABLE $ver (
                    ${ver.verId} integer primary key,
                    ${ver.timestamp} integer not null,
                    ${ver.changeType} integer not null,
                    
                    $cardId integer not null,
                    $textToTranslate text not null,
                    $translation text not null
                )
        """)
    }

    interface InsertStmt {operator fun invoke(cardId: Long, textToTranslate: String, translation: String): Long }
        lateinit var insertStmt: InsertStmt
    interface UpdateStmt {operator fun invoke(cardId: Long, textToTranslate: String, translation: String): Int}
        lateinit var updateStmt: UpdateStmt
    interface DeleteStmt {operator fun invoke(cardId: Long): Int }
        lateinit var deleteStmt: DeleteStmt

    override fun prepareStatements(db: SQLiteDatabase) {
        val self = this
        val stmtVer = db.compileStatement("insert into $ver (${ver.timestamp},${ver.changeType},$cardId,$textToTranslate,$translation) " +
                "select ?, ?, $cardId, $textToTranslate, $translation from $self where $cardId = ?")
        fun saveCurrentVersion(cardId: Long, changeType: ChangeType) {
            stmtVer.bindLong(1, clock.instant().toEpochMilli())
            stmtVer.bindLong(2, changeType.intValue)
            stmtVer.bindLong(3, cardId)
            if (stmtVer.executeUpdateDelete() != 1) {
                throw MemoryRefreshException("stmtVer.executeUpdateDelete() != 1")
            }
        }
        insertStmt = object : InsertStmt {
            val stmt = db.compileStatement("insert into $self ($cardId,$textToTranslate,$translation) values (?,?,?)")
            override fun invoke(cardId: Long, textToTranslate: String, translation: String): Long {
                stmt.bindLong(1, cardId)
                stmt.bindString(2, textToTranslate)
                stmt.bindString(3, translation)
                return stmt.executeInsert()
            }
        }
        updateStmt = object : UpdateStmt {
            private val stmt = db.compileStatement("update $self set $textToTranslate = ?, $translation = ?  where $cardId = ?")
            override fun invoke(cardId: Long, textToTranslate: String, translation: String): Int {
                saveCurrentVersion(cardId = cardId, changeType = ChangeType.UPDATE)
                stmt.bindString(1, textToTranslate)
                stmt.bindString(2, translation)
                stmt.bindLong(3, cardId)
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