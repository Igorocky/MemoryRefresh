package org.igye.memoryrefresh.database.tables

import android.database.sqlite.SQLiteDatabase
import org.igye.memoryrefresh.common.Utils
import org.igye.memoryrefresh.database.TableWithVersioning
import org.igye.memoryrefresh.database.TranslationCardDirection
import org.igye.memoryrefresh.database.TranslationCardDirection.FOREIGN_NATIVE
import org.igye.memoryrefresh.database.TranslationCardDirection.NATIVE_FOREIGN
import java.time.Clock

class TranslationCardsTable(
    private val clock: Clock,
    private val cards: CardsTable,
): TableWithVersioning(name = "TRANSLATION_CARDS") {
    val cardId = "CARD_ID"
    val textToTranslate = "TEXT_TO_TRANSLATE"
    val translation = "TRANSLATION"
    val direction = "DIRECTION"
    val reversedCardId = "REVERSED_CARD_ID"

    override fun create(db: SQLiteDatabase) {
        db.execSQL("""
                CREATE TABLE ${this} (
                    $cardId integer unique references ${cards}(${cards.id}) on update restrict on delete restrict,
                    $textToTranslate text not null,
                    $translation text not null,
                    $direction integer not null check ($direction in (${FOREIGN_NATIVE.intValue}, ${NATIVE_FOREIGN.intValue})),
                    $reversedCardId integer references ${cards}(${cards.id}) on update set null on delete set null
                )
        """)
        db.execSQL("""
                CREATE TABLE $ver (
                    ${ver.verId} integer primary key autoincrement,
                    ${ver.timestamp} integer not null,
                    
                    $cardId integer not null,
                    $textToTranslate text not null,
                    $translation text not null,
                    $direction integer not null check ($direction in (${FOREIGN_NATIVE.intValue}, ${NATIVE_FOREIGN.intValue}))
                )
        """)
    }

    interface InsertStmt {operator fun invoke(cardId: Long, textToTranslate: String, translation: String, direction: TranslationCardDirection, reversedCardId: Long?): Long } lateinit var insert: InsertStmt
    interface UpdateStmt {operator fun invoke(cardId: Long, textToTranslate: String, translation: String, direction: TranslationCardDirection, reversedCardId: Long?): Int} lateinit var update: UpdateStmt
    interface DeleteStmt {operator fun invoke(cardId: Long): Int } lateinit var delete: DeleteStmt

    override fun prepareStatements(db: SQLiteDatabase) {
        val self = this
        val stmtVer = db.compileStatement("insert into $ver (${ver.timestamp},$cardId,$textToTranslate,$translation,$direction) " +
                "select ?, $cardId, $textToTranslate, $translation, $direction from $self where $cardId = ?")
        fun saveCurrentVersion(cardId: Long) {
            stmtVer.bindLong(1, clock.instant().toEpochMilli())
            stmtVer.bindLong(2, cardId)
            Utils.executeInsert(self.ver, stmtVer)
        }
        insert = object : InsertStmt {
            val stmt = db.compileStatement("insert into $self ($cardId,$textToTranslate,$translation,$direction,$reversedCardId) values (?,?,?,?,?)")
            override fun invoke(cardId: Long, textToTranslate: String, translation: String, direction: TranslationCardDirection, reversedCardId: Long?): Long {
                stmt.bindLong(1, cardId)
                stmt.bindString(2, textToTranslate)
                stmt.bindString(3, translation)
                stmt.bindLong(4, direction.intValue)
                if (reversedCardId == null) {
                    stmt.bindNull(5)
                } else {
                    stmt.bindLong(5, reversedCardId)
                }
                return Utils.executeInsert(self, stmt)
            }
        }
        update = object : UpdateStmt {
            private val stmt = db.compileStatement("update $self set $textToTranslate = ?, $translation = ?, $direction = ?, $reversedCardId = ?  where $cardId = ?")
            override fun invoke(cardId: Long, textToTranslate: String, translation: String, direction: TranslationCardDirection, reversedCardId: Long?): Int {
                saveCurrentVersion(cardId = cardId)
                stmt.bindString(1, textToTranslate)
                stmt.bindString(2, translation)
                stmt.bindLong(3, direction.intValue)
                if (reversedCardId == null) {
                    stmt.bindNull(4)
                } else {
                    stmt.bindLong(4, reversedCardId)
                }
                stmt.bindLong(5, cardId)
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