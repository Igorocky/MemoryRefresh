package org.igye.memoryrefresh.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction
import org.igye.memoryrefresh.ErrorCode
import org.igye.memoryrefresh.common.MemoryRefreshException
import org.igye.memoryrefresh.database.TranslationCardDirection.FOREIGN_NATIVE
import org.igye.memoryrefresh.database.TranslationCardDirection.NATIVE_FOREIGN
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
) : SQLiteOpenHelper(context, dbName, null, DATABASE_VERSION) {
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
        val actualDbVersion: Int = db.select("PRAGMA user_version") { it.getLong() }.rows[0].toInt()
        if (actualDbVersion != DATABASE_VERSION) {
            throw MemoryRefreshException(
                msg = "Database version mismatch: expected $DATABASE_VERSION, actual $actualDbVersion.",
                errCode = ErrorCode.DATABASE_VERSION_MISMATCH
            )
        }
        allTables.forEach { it.prepareStatements(db) }
    }

    private fun incVersion(db: SQLiteDatabase, oldVersion: Int) {
        if (oldVersion == 1) {
            upgradeFromV1ToV2(db)
        } else if (oldVersion == 2) {
            upgradeFromV2ToV3(db)
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
        db.execSQL("""
                ALTER TABLE $cards ADD COLUMN ${cards.lastCheckedAt} integer not null default 1641594003597
        """.trimIndent())
        tags.create(db)
        cardToTag.create(db)

        recreateTable(
            db = db,
            tableName = cards.ver.tableName,
            createTableBody = """ (
                            ${cards.ver.verId} integer primary key autoincrement,
                            ${cards.ver.timestamp} integer not null,
                            ${cards.id} integer not null,
                            ${cards.type} integer not null,
                            ${cards.createdAt} integer not null
                        ) """.trimIndent(),
            oldColumnNames = listOf(
                cards.ver.verId,
                cards.ver.timestamp,
                cards.id,
                cards.type,
                cards.createdAt,
            )
        )

        recreateTable(
            db = db,
            tableName = cardsSchedule.ver.tableName,
            createTableBody = """ (
                            ${cardsSchedule.ver.verId} integer primary key autoincrement,
                            ${cardsSchedule.ver.timestamp} integer not null,
                            ${cardsSchedule.cardId} integer not null,
                            ${cardsSchedule.updatedAt} integer not null,
                            ${cardsSchedule.delay} text not null,
                            ${cardsSchedule.randomFactor} real not null,
                            ${cardsSchedule.nextAccessInMillis} integer not null,
                            ${cardsSchedule.nextAccessAt} integer not null
                        ) """.trimIndent(),
            oldColumnNames = listOf(
                cardsSchedule.ver.verId,
                cardsSchedule.ver.timestamp,
                cardsSchedule.cardId,
                cardsSchedule.updatedAt,
                cardsSchedule.delay,
                cardsSchedule.randomFactor,
                cardsSchedule.nextAccessInMillis,
                cardsSchedule.nextAccessAt,
            )
        )

        recreateTable(
            db = db,
            tableName = translationCards.ver.tableName,
            createTableBody = """ (
                    ${translationCards.ver.verId} integer primary key autoincrement,
                    ${translationCards.ver.timestamp} integer not null,
                    ${translationCards.cardId} integer not null,
                    ${translationCards.textToTranslate} text not null,
                    ${translationCards.translation} text not null
                ) """.trimIndent(),
            oldColumnNames = listOf(
                translationCards.ver.verId,
                translationCards.ver.timestamp,
                translationCards.cardId,
                translationCards.textToTranslate,
                translationCards.translation,
            )
        )

        recreateTable(
            db = db,
            tableName = translationCardsLog.tableName,
            createTableBody = """ (
                    ${translationCardsLog.recId} integer primary key autoincrement,
                    ${translationCardsLog.timestamp} integer not null,
                    ${translationCardsLog.cardId} integer not null,
                    ${translationCardsLog.translation} text not null,
                    ${translationCardsLog.matched} integer not null check (${translationCardsLog.matched} in (0,1))
                ) """.trimIndent(),
            oldColumnNames = listOf(
                translationCardsLog.recId,
                translationCardsLog.timestamp,
                translationCardsLog.cardId,
                translationCardsLog.translation,
                translationCardsLog.matched,
            ),
            reconstructIndexes = listOf(
                "CREATE INDEX IDX_${translationCardsLog}_CARD_ID on $translationCardsLog ( ${translationCardsLog.cardId}, ${translationCardsLog.timestamp} desc )"
            )
        )

    }

    private fun upgradeFromV2ToV3(db: SQLiteDatabase) {
        db.execSQL("""
                ALTER TABLE ${translationCards} ADD COLUMN ${translationCards.direction} integer not null check (${translationCards.direction} in (${FOREIGN_NATIVE.intValue}, ${NATIVE_FOREIGN.intValue})) default ${NATIVE_FOREIGN.intValue}
        """.trimIndent())
        recreateTable(
            db = db,
            tableName = translationCards.tableName,
            createTableBody = """ (
                    ${translationCards.cardId} integer unique references ${cards}(${cards.id}) on update restrict on delete restrict,
                    ${translationCards.textToTranslate} text not null,
                    ${translationCards.translation} text not null,
                    ${translationCards.direction} integer not null check (${translationCards.direction} in (${FOREIGN_NATIVE.intValue}, ${NATIVE_FOREIGN.intValue})),
                    ${translationCards.reversedCardId} integer references ${cards}(${cards.id}) on update set null on delete set null
                ) """.trimIndent(),
            oldColumnNames = listOf(
                translationCards.cardId,
                translationCards.textToTranslate,
                translationCards.translation,
                translationCards.direction,
            )
        )

        db.execSQL("""
                ALTER TABLE ${translationCards.ver} ADD COLUMN ${translationCards.direction} integer not null check (${translationCards.direction} in (${FOREIGN_NATIVE.intValue}, ${NATIVE_FOREIGN.intValue})) default ${NATIVE_FOREIGN.intValue}
        """.trimIndent())
        recreateTable(
            db = db,
            tableName = translationCards.ver.tableName,
            createTableBody = """ (
                    ${translationCards.ver.verId} integer primary key autoincrement,
                    ${translationCards.ver.timestamp} integer not null,
                    ${translationCards.cardId} integer not null,
                    ${translationCards.textToTranslate} text not null,
                    ${translationCards.translation} text not null,
                    ${translationCards.direction} integer not null check (${translationCards.direction} in (${FOREIGN_NATIVE.intValue}, ${NATIVE_FOREIGN.intValue}))
                ) """.trimIndent(),
            oldColumnNames = listOf(
                translationCards.ver.verId,
                translationCards.ver.timestamp,
                translationCards.cardId,
                translationCards.textToTranslate,
                translationCards.translation,
                translationCards.direction,
            )
        )

        db.execSQL("""
                ALTER TABLE $cardsSchedule ADD COLUMN ${cardsSchedule.origDelay} text
        """.trimIndent())
        db.execSQL("""
                update $cardsSchedule set ${cardsSchedule.origDelay} = '-' where ${cardsSchedule.origDelay} is null
        """.trimIndent())
        recreateTable(
            db = db,
            tableName = cardsSchedule.tableName,
            createTableBody = """ (
                            ${cardsSchedule.cardId} integer unique references $cards(${cards.id}) on update restrict on delete restrict,
                            ${cardsSchedule.updatedAt} integer not null,
                            ${cardsSchedule.origDelay} text not null,
                            ${cardsSchedule.delay} text not null,
                            ${cardsSchedule.randomFactor} real not null,
                            ${cardsSchedule.nextAccessInMillis} integer not null,
                            ${cardsSchedule.nextAccessAt} integer not null
                        ) """.trimIndent(),
            oldColumnNames = listOf(
                cardsSchedule.cardId,
                cardsSchedule.updatedAt,
                cardsSchedule.origDelay,
                cardsSchedule.delay,
                cardsSchedule.randomFactor,
                cardsSchedule.nextAccessInMillis,
                cardsSchedule.nextAccessAt,
            )
        )

        db.execSQL("""
                ALTER TABLE ${cardsSchedule.ver} ADD COLUMN ${cardsSchedule.origDelay} text
        """.trimIndent())
        db.execSQL("""
                update ${cardsSchedule.ver} set ${cardsSchedule.origDelay} = '-' where ${cardsSchedule.origDelay} is null
        """.trimIndent())
        recreateTable(
            db = db,
            tableName = cardsSchedule.ver.tableName,
            createTableBody = """ (
                    ${cardsSchedule.ver.verId} integer primary key autoincrement,
                    ${cardsSchedule.ver.timestamp} integer not null,
                    ${cardsSchedule.cardId} integer not null,
                    ${cardsSchedule.updatedAt} integer not null,
                    ${cardsSchedule.origDelay} text not null,
                    ${cardsSchedule.delay} text not null,
                    ${cardsSchedule.randomFactor} real not null,
                    ${cardsSchedule.nextAccessInMillis} integer not null,
                    ${cardsSchedule.nextAccessAt} integer not null
                ) """.trimIndent(),
            oldColumnNames = listOf(
                cardsSchedule.ver.verId,
                cardsSchedule.ver.timestamp,
                cardsSchedule.cardId,
                cardsSchedule.updatedAt,
                cardsSchedule.origDelay,
                cardsSchedule.delay,
                cardsSchedule.randomFactor,
                cardsSchedule.nextAccessInMillis,
                cardsSchedule.nextAccessAt,
            )
        )
    }

    /**
     * https://www.sqlite.org/lang_altertable.html
     *     -> 6. Making Other Kinds Of Table Schema Changes
     */
    private fun recreateTable(
        db: SQLiteDatabase,
        tableName: String,
        createTableBody: String,
        oldColumnNames: List<String>,
        newColumnNames: List<String>? = null,
        reconstructIndexes: List<String>? = null,
    ) {
        try {
            //1. If foreign key constraints are enabled, disable them using PRAGMA foreign_keys=OFF.
            db.execSQL("PRAGMA foreign_keys=OFF")
            //2. Start a transaction.
            db.doInTransaction {
                //3. Remember the format of all indexes, triggers, and views associated with table X.
                // This information will be needed in step 8 below. One way to do this is to run a query like
                // the following: SELECT type, sql FROM sqlite_schema WHERE tbl_name='X'.
                // NOTE: This step is not used in this implementation because of 'reconstructIndexes' parameter.

                //4. Use CREATE TABLE to construct a new table "new_X" that is in the desired revised format of table X.
                // Make sure that the name "new_X" does not collide with any existing table name, of course.
                execSQL("CREATE TABLE new_$tableName $createTableBody")

                //5. Transfer content from X into new_X using a statement like: INSERT INTO new_X SELECT ... FROM X.
                execSQL("""
                    INSERT INTO new_$tableName (${(newColumnNames?:oldColumnNames).joinToString(separator = ",")}) SELECT ${oldColumnNames.joinToString(separator = ",")} FROM $tableName
                """.trimIndent())

                //6. Drop the old table X: DROP TABLE X.
                execSQL("DROP TABLE $tableName")

                //7. Change the name of new_X to X using: ALTER TABLE new_X RENAME TO X.
                execSQL("ALTER TABLE new_$tableName RENAME TO $tableName")

                //8. Use CREATE INDEX, CREATE TRIGGER, and CREATE VIEW to reconstruct indexes, triggers, and views
                // associated with table X. Perhaps use the old format of the triggers, indexes, and views saved from
                // step 3 above as a guide, making changes as appropriate for the alteration.
                reconstructIndexes?.forEach { execSQL(it) }

                //9. If any views refer to table X in a way that is affected by the schema change, then drop those views
                // using DROP VIEW and recreate them with whatever changes are necessary to accommodate the schema
                // change using CREATE VIEW.

                //10. If foreign key constraints were originally enabled then run PRAGMA foreign_key_check to verify
                // that the schema change did not break any foreign key constraints.
                execSQL("PRAGMA foreign_key_check")

                //11. Commit the transaction started in step 2.
            }
        } finally {
            //12. If foreign keys constraints were originally enabled, reenable them now.
            db.execSQL("PRAGMA foreign_keys=ON")
        }
    }

    companion object {
        const val DATABASE_VERSION = 2
    }
}

