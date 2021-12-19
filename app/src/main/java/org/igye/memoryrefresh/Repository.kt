package org.igye.memoryrefresh

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.core.database.sqlite.transaction
import java.lang.Exception
import java.time.Instant

class Repository(context: Context, dbName: String?) : SQLiteOpenHelper(context, dbName, null, 1) {
    val t = DB_NAMES
    override fun onCreate(db: SQLiteDatabase) {
        db.transaction {
            execSQL("""
                    CREATE TABLE ${t.tags} (
                        ${t.tags.id} integer primary key,
                        ${t.tags.createdAt} integer not null,
                        ${t.tags.name} text unique not null
                    )
            """)
            execSQL("""
                    CREATE TABLE ${t.notes} (
                        ${t.notes.id} integer primary key,
                        ${t.notes.createdAt} integer not null,
                        ${t.notes.text} text not null,
                        ${t.notes.isDeleted} integer not null default 0
                    )
            """)
            execSQL("""
                    CREATE TABLE ${t.noteToTag} (
                        ${t.noteToTag.noteId} integer references ${t.notes}(${t.notes.id}) on update restrict on delete restrict,
                        ${t.noteToTag.tagId} integer references ${t.tags}(${t.tags.id}) on update restrict on delete restrict
                    )
            """)
            execSQL("""
                    CREATE UNIQUE INDEX idx_${t.noteToTag}_${t.noteToTag.tagId}_${t.noteToTag.noteId} on ${t.noteToTag} (
                        ${t.noteToTag.tagId}, ${t.noteToTag.noteId}
                    )
            """)
            execSQL("""
                    CREATE INDEX idx_${t.noteToTag}_${t.noteToTag.noteId} on ${t.noteToTag} (
                        ${t.noteToTag.noteId}
                    )
            """)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        TODO("Not yet implemented")
    }

    override fun onConfigure(db: SQLiteDatabase?) {
        super.onConfigure(db)
        db!!.setForeignKeyConstraintsEnabled(true)
    }

    interface InsertTagStmt {fun exec(name: String): Tag} var insertTagStmt: InsertTagStmt? = null
    interface UpdateTagStmt {fun exec(id: Long, name: String): Int} var updateTagStmt: UpdateTagStmt? = null
    interface DeleteTagStmt {fun exec(id: Long): Int} var deleteTagStmt: DeleteTagStmt? = null
    interface InsertNoteStmt {fun exec(text: String): Note} var insertNoteStmt: InsertNoteStmt? = null
    interface InsertNoteToTagStmt {fun exec(noteId: Long, tagId: Long): Long} var insertNoteToTagStmt: InsertNoteToTagStmt? = null
    interface DeleteNoteToTagStmt {fun exec(noteId: Long): Int} var deleteNoteToTagStmt: DeleteNoteToTagStmt? = null

    override fun onOpen(db: SQLiteDatabase?) {
        super.onOpen(db)
        insertTagStmt = object : InsertTagStmt {
            val stmt = db!!.compileStatement("insert into ${t.tags} (${t.tags.createdAt},${t.tags.name}) values (?,?)")
            override fun exec(name: String): Tag {
                val createdAt = Instant.now().toEpochMilli()
                stmt.bindLong(1, createdAt)
                stmt.bindString(2, name)
                return Tag(id = stmt.executeInsert(), createdAt = createdAt, name = name)
            }

        }
        updateTagStmt = object : UpdateTagStmt {
            private val stmt = db!!.compileStatement("update ${t.tags} set ${t.tags.name} = ? where ${t.tags.id} = ?")
            override fun exec(id: Long, name: String): Int {
                stmt.bindString(1, name)
                stmt.bindLong(2, id)
                return stmt.executeUpdateDelete()
            }
        }
        deleteTagStmt = object : DeleteTagStmt {
            private val stmt = db!!.compileStatement("delete from ${t.tags} where ${t.tags.id} = ?")
            override fun exec(id: Long): Int {
                stmt.bindLong(1, id)
                return stmt.executeUpdateDelete()
            }
        }
        insertNoteStmt = object : InsertNoteStmt {
            val stmt = db!!.compileStatement("insert into ${t.notes} (${t.notes.createdAt},${t.notes.text}) values (?,?)")
            override fun exec(text: String): Note {
                val createdAt = Instant.now().toEpochMilli()
                stmt.bindLong(1, createdAt)
                stmt.bindString(2, text)
                return Note(id = stmt.executeInsert(), createdAt = createdAt, text = text, tagIds = emptyList())
            }

        }
        insertNoteToTagStmt = object : InsertNoteToTagStmt {
            val stmt = db!!.compileStatement("insert into ${t.noteToTag} (${t.noteToTag.noteId},${t.noteToTag.tagId}) values (?,?)")
            override fun exec(noteId: Long, tagId: Long): Long {
                stmt.bindLong(1, noteId)
                stmt.bindLong(2, tagId)
                return stmt.executeInsert()
            }
        }
        deleteNoteToTagStmt = object : DeleteNoteToTagStmt {
            val stmt = db!!.compileStatement("delete from ${t.noteToTag} where ${t.noteToTag.noteId} = ?")
            override fun exec(noteId: Long): Int {
                stmt.bindLong(1, noteId)
                return stmt.executeUpdateDelete()
            }

        }
    }

    interface SelectedRow { fun getLong():Long fun getLongOrNull():Long? fun getString():String fun getStringOrNull():String? }
    fun <T> select(
        query:String,
        args:Array<String>? = null,
        rowsMax:Long? = null,
        columnNames:List<String>,
        rowMapper:(SelectedRow) -> T,
    ): Pair<Boolean, List<T>> {
        return readableDatabase.rawQuery(
            query,
            args
        ).use { cursor ->
            val result = ArrayList<T>()
            if (cursor.moveToFirst()) {
                val columnIndexes = columnNames.map { cursor.getColumnIndexOrThrow(it) }
                while (!cursor.isAfterLast && (rowsMax == null || result.size < rowsMax)) {
                    result.add(rowMapper(object : SelectedRow{
                        private var curColumn = 0
                        override fun getLong(): Long {
                            return cursor.getLong(columnIndexes[curColumn++])
                        }
                        override fun getString():String {
                            return cursor.getString(columnIndexes[curColumn++])
                        }
                        override fun getLongOrNull(): Long? {
                            return cursor.getLongOrNull(columnIndexes[curColumn++])
                        }
                        override fun getStringOrNull():String? {
                            return cursor.getStringOrNull(columnIndexes[curColumn++])
                        }
                    }))
                    cursor.moveToNext()
                }
            }
            Pair(cursor.isAfterLast, result)
        }
    }
}

inline fun <T> SQLiteDatabase.doInTransaction(
    exceptionHandler: ((Exception) -> BeRespose<T>?) = { null },
    errCode: Int? = null,
    body: SQLiteDatabase.() -> BeRespose<T>
): BeRespose<T> {
    beginTransaction()
    try {
        val result = body()
        if (result.err == null) {
            setTransactionSuccessful()
        }
        return result
    } catch (ex: Exception) {
        val res = exceptionHandler(ex)
        if (res != null) {
            return res
        } else if (errCode != null) {
            return BeRespose(err = BeErr(code = errCode, msg = "${ex.javaClass.simpleName} ${ex.message?:"..."}"))
        } else {
            throw ex
        }
    } finally {
        endTransaction()
    }
}

object DB_V1 {
    val tags = TagsTable
    object TagsTable {
        override fun toString() = "TAGS"
        val id = "id"
        val createdAt = "createdAt"
        val name = "name"
    }

    val notes = NotesTable
    object NotesTable {
        override fun toString() = "NOTES"
        val id = "id"
        val createdAt = "createdAt"
        val isDeleted = "isDeleted"
        val text = "text"
    }

    val noteToTag = NotesToTagsTable
    object NotesToTagsTable {
        override fun toString() = "NOTES_TO_TAGS"
        val noteId = "note_id"
        val tagId = "tag_id"
    }
}

val DB_NAMES = DB_V1