package org.igye.memoryrefresh.manager

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import org.igye.memoryrefresh.ErrorCode
import org.igye.memoryrefresh.common.BeMethod
import org.igye.memoryrefresh.common.Try
import org.igye.memoryrefresh.common.Utils
import org.igye.memoryrefresh.database.Repository
import org.igye.memoryrefresh.database.TagsStat
import org.igye.memoryrefresh.dto.common.Backup
import org.igye.memoryrefresh.dto.common.BeRespose
import java.io.File
import java.io.FileOutputStream
import java.time.Clock
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream


class RepositoryManager(
    private val context: Context,
    private val clock: Clock,
    private val repositoryProvider: () -> Repository,
) {
    val shareFile: AtomicReference<((Uri) -> Unit)?> = AtomicReference(null)
    private val repo: AtomicReference<Repository> = AtomicReference(repositoryProvider())
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX").withZone(ZoneId.from(ZoneOffset.UTC))
    val tagsStat = TagsStat(repositoryManager = this)

    @Synchronized
    fun getRepo(): Repository {
        return repo.get()
    }

    @BeMethod
    @Synchronized
    fun doBackup(): BeRespose<Backup> {
        val dbName = getRepo().dbName
        val dbVersionNumber: Int = getRepo().readableDatabase.version
        try {
            getRepo().close()
            val databasePath: File = context.getDatabasePath(dbName)
            val backupFileName = createBackupFileName(databasePath, dbVersionNumber)
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
            repo.set(repositoryProvider())
        }
    }

    @BeMethod
    @Synchronized
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
    @Synchronized
    fun restoreFromBackup(args: RestoreFromBackupArgs): BeRespose<String> {
        tagsStat.reset()
        val dbName = getRepo().dbName
        val databasePath: File = context.getDatabasePath(dbName)
        val backupFile = File(backupDir, args.backupName)
        try {
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
            repo.set(repositoryProvider())
        }
    }

    data class DeleteBackupArgs(val backupName:String)
    @BeMethod
    @Synchronized
    fun deleteBackup(args: DeleteBackupArgs): BeRespose<List<Backup>> {
        File(backupDir, args.backupName).delete()
        return listAvailableBackups()
    }

    data class ShareBackupArgs(val backupName:String)
    @BeMethod
    @Synchronized
    fun shareBackup(args: ShareBackupArgs): BeRespose<Unit?> {
        return BeRespose(ErrorCode.SHARE_BACKUP) {
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "org.igye.memoryrefresh.fileprovider.dev",
                File(backupDir, args.backupName)
            )
            shareFile.get()?.invoke(fileUri)
        }
    }

    fun close() {
        tagsStat.reset()
        getRepo().close()
    }

    private val backupDir = Utils.getBackupsDir(context)

    private fun createBackupFileName(dbPath: File, dbVersionNumber: Int): String {
        return "${dbPath.name}-v${dbVersionNumber}-backup-${dateTimeFormatter.format(clock.instant()).replace(":","-")}"
    }
}