package org.igye.memoryrefresh

import android.content.Context
import android.database.sqlite.SQLiteStatement
import android.net.Uri
import androidx.core.content.FileProvider
import org.igye.memoryrefresh.Utils.isNotEmpty
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream


class DataManager(
    private val context: Context,
    private val dbName: String? = "tagged-notes-db",
) {
    val shareFile: AtomicReference<((Uri) -> Unit)?> = AtomicReference(null)
    private val t = DB_NAMES
    private val repo: AtomicReference<Repository> = AtomicReference(createNewRepo())
    fun getRepo() = repo.get()
    private val tagStat = TagsStatHolder(repositoryGetter = this::getRepo)

    private val ERR_CREATE_TAG_NAME_EMPTY = 101
    private val ERR_CREATE_TAG_NAME_DUPLICATED = 102
    private val ERR_CREATE_TAG_NEGATIVE_NEW_ID = 103
    private val ERR_CREATE_TAG = 104

    private val ERR_CREATE_NOTE_TEXT_EMPTY = 111
    private val ERR_CREATE_NOTE_NEGATIVE_NEW_ID = 112
    private val ERR_CREATE_NOTE_TAG_REF_NEGATIVE_NEW_ID = 113
    private val ERR_CREATE_NOTE = 114

    private val ERR_GET_TAGS = 201
    private val ERR_GET_NOTES = 202

    private val ERR_UPDATE_TAG_NAME_EMPTY = 301
    private val ERR_UPDATE_TAG_NAME_DUPLICATED = 302
    private val ERR_UPDATE_TAG = 304

    private val ERR_UPDATE_NOTE = 305
    private val ERR_UPDATE_NOTE_TEXT_EMPTY = 306
    private val ERR_UPDATE_NOTE_CNT_IS_NOT_ONE = 307
    private val ERR_UPDATE_NOTE_TAG_REF_NEGATIVE_NEW_ID = 308

    private val ERR_DELETE_TAG = 401

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX").withZone(ZoneId.from(ZoneOffset.UTC))

    data class SaveNewTagArgs(val name:String)
    @BeMethod
    fun saveNewTag(args:SaveNewTagArgs): BeRespose<Tag> {
        val name = args.name.replace(" ", "")
        return if (name.isBlank()) {
            BeRespose(err = BeErr(code = ERR_CREATE_TAG_NAME_EMPTY, msg = "Name of a new tag should not be empty."))
        } else {
            getRepo().writableDatabase.doInTransaction(
                exceptionHandler = {
                    if (it.message?.contains("UNIQUE constraint failed: ${t.tags}.${t.tags.name}") ?: false) {
                        BeRespose(err = BeErr(code = ERR_CREATE_TAG_NAME_DUPLICATED, msg = "'${name}' tag already exists."))
                    } else null
                },
                errCode = ERR_CREATE_TAG
            ) {
                val newTag = getRepo().insertTagStmt!!.exec(name = name)
                if (newTag.id == -1L) {
                    BeRespose(err = BeErr(code = ERR_CREATE_TAG_NEGATIVE_NEW_ID, msg = "newId == -1"))
                } else {
                    tagStat.tagsCouldChange()
                    BeRespose(data = newTag)
                }
            }
        }
    }

    data class UpdateTagArgs(val id:Long, val name: String)
    @BeMethod
    fun updateTag(args:UpdateTagArgs): BeRespose<Int> {
        val name = args.name.replace(" ", "")
        return if (name.isBlank()) {
            BeRespose(err = BeErr(code = ERR_UPDATE_TAG_NAME_EMPTY, msg = "Name of a tag must not be empty."))
        } else {
            getRepo().writableDatabase.doInTransaction(
                exceptionHandler = {
                    if (it.message?.contains("UNIQUE constraint failed: ${t.tags}.${t.tags.name}") ?: false) {
                        BeRespose(err = BeErr(code = ERR_UPDATE_TAG_NAME_DUPLICATED, msg = "'${name}' tag already exists."))
                    } else null
                },
                errCode = ERR_UPDATE_TAG
            ) {
                BeRespose(data = getRepo().updateTagStmt!!.exec(id=args.id,name=name))
            }
        }
    }

    data class DeleteTagArgs(val id:Long)
    @BeMethod
    fun deleteTag(args:DeleteTagArgs): BeRespose<Int> {
        return getRepo().writableDatabase.doInTransaction(errCode = ERR_DELETE_TAG) {
            tagStat.tagsCouldChange()
            BeRespose(data = getRepo().deleteTagStmt!!.exec(args.id))
        }
    }

    data class GetTagsArgs(val nameContains:String? = null)
    @BeMethod
    fun getTags(params:GetTagsArgs): BeRespose<List<Tag>> {
        val query = StringBuilder()
        val args = ArrayList<String>()
        query.append("select ${t.tags.id}, ${t.tags.createdAt}, ${t.tags.name} from ${t.tags}")
        if (params.nameContains != null) {
            query.append(" where lower(${t.tags.name}) like ?")
            args.add("%${params.nameContains.lowercase()}%")
        }
        query.append(" order by ${t.tags.name}")
        return getRepo().readableDatabase.doInTransaction(errCode = ERR_GET_TAGS) {
            BeRespose(data = getRepo().select(
                query = query.toString(),
                args = args.toTypedArray(),
                columnNames = listOf(t.tags.id, t.tags.createdAt, t.tags.name),
                rowMapper = {Tag(
                    id = it.getLong(),
                    createdAt = it.getLong(),
                    name = it.getString(),
                )}
            ).second)
        }
    }

    data class SaveNewNoteArgs(val text:String, val tagIds: List<Long>)
    @BeMethod
    fun saveNewNote(args:SaveNewNoteArgs): BeRespose<Note> {
        return if (args.text.isBlank()) {
            BeRespose(err = BeErr(code = ERR_CREATE_NOTE_TEXT_EMPTY, msg = "Note's content should not be empty."))
        } else {
            tagStat.tagsCouldChange()
            getRepo().writableDatabase.doInTransaction(errCode = ERR_CREATE_NOTE) transaction@{
                val newNote = getRepo().insertNoteStmt!!.exec(args.text)
                if (newNote.id == -1L) {
                    BeRespose(err = BeErr(code = ERR_CREATE_NOTE_NEGATIVE_NEW_ID, "newId == -1"))
                } else {
                    args.tagIds.forEach { tagId ->
                        if (getRepo().insertNoteToTagStmt!!.exec(newNote.id,tagId) == -1L) {
                            return@transaction BeRespose<Note>(err = BeErr(code = ERR_CREATE_NOTE_TAG_REF_NEGATIVE_NEW_ID, "noteToTag.newId == -1"))
                        }
                    }
                    BeRespose(data = newNote.copy(tagIds = args.tagIds))
                }
            }
        }
    }

    data class GetNotesArgs(val tagIdsToInclude: Set<Long>? = null, val tagIdsToExclude: Set<Long>? = null, val searchInDeleted: Boolean = false, val rowsMax: Long = 100)
    @BeMethod
    fun getNotes(args:GetNotesArgs): BeRespose<ListOfItems<Note>> {
        fun getLeastUsedTagId(tagIds: Set<Long>): Long {
            val stat = tagStat.getTagStat()
            return tagIds.minByOrNull { stat[it]?:0 }!!
        }
        val query = StringBuilder(
            """select n.${t.notes.id}, 
                max(n.${t.notes.createdAt}) as ${t.notes.createdAt}, 
                max(n.${t.notes.isDeleted}) as ${t.notes.isDeleted}, 
                max(n.${t.notes.text}) as ${t.notes.text},
                (select group_concat(t.${t.noteToTag.tagId}) from ${t.noteToTag} t where t.${t.noteToTag.noteId} = n.${t.notes.id}) as tagIds
                from ${t.notes} n
                """
        )
        var leastUsedTagId: Long? = null
        if (isNotEmpty(args.tagIdsToInclude)) {
            leastUsedTagId = getLeastUsedTagId(args.tagIdsToInclude!!)
            query.append(" inner join ${t.noteToTag} nt1 ")
            query.append(" on n.${t.notes.id} = nt1.${t.noteToTag.noteId} and nt1.${t.noteToTag.tagId} = ${getLeastUsedTagId(args.tagIdsToInclude!!)}")
        }
        if (isNotEmpty(args.tagIdsToInclude) || isNotEmpty(args.tagIdsToExclude)) {
            query.append(" inner join ${t.noteToTag} nt on n.${t.notes.id} = nt.${t.noteToTag.noteId}")
        }
        val whereFilters = ArrayList<String>()
        whereFilters.add("n.${t.notes.isDeleted} ${if(args.searchInDeleted)  "!= 0" else "= 0"}")
        query.append(whereFilters.joinToString(prefix = " where ", separator = " and "))
        query.append(" group by n.${t.notes.id}")
        if (isNotEmpty(args.tagIdsToInclude) && args.tagIdsToInclude!!.size > 1 || isNotEmpty(args.tagIdsToExclude)) {
            val havingFilters = ArrayList<String>()
            fun addTagCondition(tagId:Long,include:Boolean) = " group_concat(':'||nt.${t.noteToTag.tagId}||':') ${if (include) "" else "not"} like '%:'||${tagId}||':%'"
            if (isNotEmpty(args.tagIdsToInclude)) {
                havingFilters.addAll((args.tagIdsToInclude!!-leastUsedTagId!!)?.map { addTagCondition(it, true) }!!)
            }
            if (isNotEmpty(args.tagIdsToExclude)) {
                havingFilters.addAll(args.tagIdsToExclude?.map { addTagCondition(it, false) }!!)
            }
            query.append(havingFilters.joinToString(prefix = " having ", separator = " and "))
        }
        return getRepo().readableDatabase.doInTransaction(errCode = ERR_GET_NOTES) {
            val (allRowsFetched, result) = getRepo().select(
                query = query.toString(),
                rowsMax = args.rowsMax,
                columnNames = listOf(t.notes.id, t.notes.createdAt, t.notes.isDeleted, t.notes.text, "tagIds"),
                rowMapper = {
                    Note(
                        id = it.getLong(),
                        createdAt = it.getLong(),
                        isDeleted = if (it.getLong() == 0L) false else true,
                        text = it.getString(),
                        tagIds = it.getString().splitToSequence(",").map { it.toLong() }.toList()
                    )
                }
            )
            BeRespose(data = ListOfItems(complete = allRowsFetched, items = result))
        }
    }

    @BeMethod
    fun getRemainingTagIds(args:GetNotesArgs): BeRespose<Set<Long>> {
        return getNotes(args.copy(rowsMax = Long.MAX_VALUE))
            .mapData { it.items.asSequence().flatMap { it.tagIds }.toSet() }
    }

    data class UpdateNoteArgs(val id:Long, val text:String? = null, val isDeleted: Boolean? = null, val tagIds: List<Long>? = null)
    @BeMethod
    fun updateNote(params:UpdateNoteArgs): BeRespose<Int> {
        return if (params.text == null && params.isDeleted == null && params.tagIds == null) {
            BeRespose(data = 0)
        } else if (params.text != null && params.text.isBlank()) {
            BeRespose(err = BeErr(code = ERR_UPDATE_NOTE_TEXT_EMPTY, msg = "Content of a note must not be empty."))
        } else {
            tagStat.tagsCouldChange()
            getRepo().writableDatabase.doInTransaction(errCode = ERR_UPDATE_NOTE) {
                if (params.text != null || params.isDeleted != null) {
                    val text = params.text?.trim()
                    val query = StringBuilder("update ${t.notes} set ")
                    val updateParts = ArrayList<String>()
                    val args = ArrayList<(SQLiteStatement,Int) -> Unit>()
                    if (text != null) {
                        updateParts.add("${t.notes.text} = ?")
                        args.add { stmt, idx -> stmt.bindString(idx, text) }
                    }
                    if (params.isDeleted != null) {
                        updateParts.add("${t.notes.isDeleted} = ?")
                        args.add { stmt, idx -> stmt.bindLong(idx, if (params.isDeleted) 1 else 0) }
                    }
                    query.append(updateParts.joinToString(separator = ", "))
                    query.append(" where id = ?")
                    args.add { stmt, idx -> stmt.bindLong(idx, params.id) }
                    val updatedCnt = compileStatement(query.toString()).use { stmt: SQLiteStatement ->
                        args.forEachIndexed { index, binder -> binder(stmt,index+1) }
                        stmt.executeUpdateDelete()
                    }
                    if (updatedCnt != 1) {
                        return@doInTransaction BeRespose(err = BeErr(code = ERR_UPDATE_NOTE_CNT_IS_NOT_ONE, msg = "updatedCnt != 1"))
                    }
                }
                if (params.tagIds != null) {
                    getRepo().deleteNoteToTagStmt!!.exec(params.id)
                    params.tagIds.forEach { tagId ->
                        if (getRepo().insertNoteToTagStmt!!.exec(params.id,tagId) == -1L) {
                            return@doInTransaction BeRespose(err = BeErr(code = ERR_UPDATE_NOTE_TAG_REF_NEGATIVE_NEW_ID, "noteToTag.newId == -1"))
                        }
                    }
                }
                BeRespose(data = 1)
            }
        }
    }

    @BeMethod
    fun doBackup(): BeRespose<Backup> {
        try {
            getRepo().close()
            val databasePath: File = context.getDatabasePath(dbName)
            val backupFileName = createBackupFileName(databasePath)
            val backupFile = File(backupDir, backupFileName + ".zip")

            return ZipOutputStream(FileOutputStream(backupFile)).use { zipOut ->
                val backupZipEntry = ZipEntry(backupFileName)
                zipOut.putNextEntry(backupZipEntry)
                databasePath.inputStream().use { dbData ->
                    dbData.copyTo(zipOut)
                }
                zipOut.closeEntry()
                BeRespose(data = Backup(name = backupFile.name, size = backupFile.length()))
            }
        } finally {
            repo.set(createNewRepo())
        }
    }

    @BeMethod
    fun listAvailableBackups(): BeRespose<List<Backup>> {
        return if (!backupDir.exists()) {
            BeRespose(data = emptyList())
        } else {
            BeRespose(
                data = backupDir.listFiles().asSequence()
                    .sortedBy { -it.lastModified() }
                    .map { Backup(name = it.name, size = it.length()) }
                    .toList()
            )
        }
    }

    data class RestoreFromBackupArgs(val backupName:String)
    @BeMethod
    fun restoreFromBackup(args:RestoreFromBackupArgs): BeRespose<String> {
        val databasePath: File = context.getDatabasePath(dbName)
        val backupFile = File(backupDir, args.backupName)
        try {
            tagStat.reset()
            getRepo().close()
            val zipFile = ZipFile(backupFile)
            val entries = zipFile.entries()
            val entry = entries.nextElement()
            zipFile.getInputStream(entry).use { inp ->
                FileOutputStream(databasePath).use { out ->
                    inp.copyTo(out)
                }
            }
            return BeRespose(data = "The database was restored from the backup ${args.backupName}")
        } finally {
            repo.set(createNewRepo())
        }
    }

    data class DeleteBackupArgs(val backupName:String)
    @BeMethod
    fun deleteBackup(args:DeleteBackupArgs): BeRespose<List<Backup>> {
        File(backupDir, args.backupName).delete()
        return listAvailableBackups()
    }

    data class ShareBackupArgs(val backupName:String)
    @BeMethod
    fun shareBackup(args:ShareBackupArgs): BeRespose<Unit> {
        val fileUri: Uri = FileProvider.getUriForFile(
            context,
            "org.igye.MemoryRefresh.fileprovider",
            File(backupDir, args.backupName)
        )
        shareFile.get()?.invoke(fileUri)
        return BeRespose(data = Unit)
    }

    fun close() {
        tagStat.reset()
        getRepo().close()
    }

    private val backupDir = Utils.getBackupsDir(context)

    private fun createNewRepo() = Repository(context, dbName)

    private fun createBackupFileName(dbPath: File): String {
        return "${dbPath.name}-backup-${dateTimeFormatter.format(Instant.now()).replace(":","-")}"
    }
}