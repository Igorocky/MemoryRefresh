package org.igye.memoryrefresh.database.tables

import android.database.sqlite.SQLiteDatabase
import org.igye.memoryrefresh.common.Utils
import org.igye.memoryrefresh.database.Table
import java.time.Clock

class CardToTagTable(
    private val clock: Clock,
    private val cards: CardsTable,
    private val tags: TagsTable,
): Table(tableName = "CARD_TO_TAG") {
    val cardId = "CARD_ID"
    val tagId = "TAG_ID"

    override fun create(db: SQLiteDatabase) {
        db.execSQL("""
                CREATE TABLE $this (
                    $cardId integer references $cards(${cards.id}) on update restrict on delete restrict,
                    $tagId integer references $tags(${tags.id}) on update restrict on delete restrict
                )
        """)
        db.execSQL("""
                CREATE UNIQUE INDEX IDX_${this}_${tagId}_${cardId} on $this ( $tagId, $cardId )
        """)
        db.execSQL("""
                CREATE INDEX IDX_${this}_${cardId} on $this ( $cardId )
        """)
    }

    interface InsertStmt {operator fun invoke(cardId: Long, tagId: Long): Long } lateinit var insert: InsertStmt
    interface DeleteStmt {operator fun invoke(cardId: Long, tagId: Long? = null): Int } lateinit var delete: DeleteStmt

    override fun prepareStatements(db: SQLiteDatabase) {
        val self = this
        insert = object : InsertStmt {
            val stmt = db.compileStatement("insert into $self ($cardId,$tagId) values (?,?)")
            override fun invoke(cardId: Long, tagId: Long): Long {
                stmt.bindLong(1, cardId)
                stmt.bindLong(2, tagId)
                return Utils.executeInsert(self, stmt)
            }
        }
        delete = object : DeleteStmt {
            private val stmtDeleteByCardAndTag = db.compileStatement("delete from $self where $cardId = ? and $tagId = ?")
            private val stmtDeleteByCard = db.compileStatement("delete from $self where $cardId = ?")
            override fun invoke(cardId: Long, tagId: Long?): Int {
                if (tagId != null) {
                    stmtDeleteByCardAndTag.bindLong(1, cardId)
                    stmtDeleteByCardAndTag.bindLong(2, tagId)
                    return stmtDeleteByCardAndTag.executeUpdateDelete()
                } else {
                    stmtDeleteByCard.bindLong(1, cardId)
                    return stmtDeleteByCard.executeUpdateDelete()
                }
            }
        }
    }
}