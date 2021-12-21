package org.igye.memoryrefresh

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import org.igye.memoryrefresh.ErrorCode.*
import org.igye.memoryrefresh.common.Try
import org.igye.memoryrefresh.database.*
import org.igye.memoryrefresh.dto.Backup
import org.igye.memoryrefresh.dto.BeErr
import org.igye.memoryrefresh.dto.BeRespose
import org.igye.memoryrefresh.dto.TranslateCard
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


class DataManager(
    private val context: Context,
    private val clock: Clock,
    private val repositoryProvider: () -> Repository,
) {
    val shareFile: AtomicReference<((Uri) -> Unit)?> = AtomicReference(null)
    private val repo: AtomicReference<Repository> = AtomicReference(repositoryProvider())
    fun getRepo() = repo.get()

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX").withZone(ZoneId.from(ZoneOffset.UTC))

    data class SaveNewTranslateCardArgs(val textToTranslate:String, val translation:String)
    @BeMethod
    @Synchronized
    fun saveNewTranslateCard(args:SaveNewTranslateCardArgs): BeRespose<TranslateCard> {
        val textToTranslate = args.textToTranslate.trim()
        val translation = args.translation.trim()
        return if (textToTranslate.isBlank()) {
            BeRespose(err = BeErr(code = SAVE_NEW_TRANSLATE_CARD_TEXT_TO_TRANSLATE_IS_EMPTY.code, msg = "Text to translate should not be empty."))
        } else if (translation.isBlank()) {
            BeRespose(err = BeErr(code = SAVE_NEW_TRANSLATE_CARD_TRANSLATION_IS_EMPTY.code, msg = "Translation should not be empty."))
        } else {
            val repo = getRepo()
            repo.writableDatabase.doInTransaction {
                val cardId = repo.cards.insertStmt(cardType = CardType.TRANSLATION)
                val currTime = clock.instant().toEpochMilli()
                repo.cardsSchedule.insertStmt(cardId = cardId, lastAccessedAt = currTime, nextAccessInSec = 0, nextAccessAt = currTime)
                repo.translationCards.insertStmt(cardId = cardId, textToTranslate = textToTranslate, translation = translation)
                TranslateCard(
                    id = cardId,
                    textToTranslate = textToTranslate,
                    translation = translation,
                    lastAccessedAt = currTime,
                    nextAccessInSec = 0,
                    nextAccessAt = currTime,
                )
            }.apply(toBeResponse(SAVE_NEW_TRANSLATE_CARD_EXCEPTION))
        }
    }

    data class EditTranslateCardArgs(val cardId:Long, val textToTranslate:String, val translation:String)
    @BeMethod
    @Synchronized
    fun editTranslateCard(args:EditTranslateCardArgs): BeRespose<TranslateCard> {
        val textToTranslate = args.textToTranslate.trim()
        val translation = args.translation.trim()
        return if (textToTranslate.isBlank()) {
            BeRespose(err = BeErr(code = EDIT_TRANSLATE_CARD_TEXT_TO_TRANSLATE_IS_EMPTY.code, msg = "Text to translate should not be empty."))
        } else if (translation.isBlank()) {
            BeRespose(err = BeErr(code = EDIT_TRANSLATE_CARD_TRANSLATION_IS_EMPTY.code, msg = "Translation should not be empty."))
        } else {
            val repo = getRepo()
            repo.writableDatabase.doInTransactionTry {
                selectTranslateCardById(cardId = args.cardId).map { existingCard: TranslateCard ->
                    if (existingCard.textToTranslate == textToTranslate && existingCard.translation == translation) {
                        existingCard
                    } else {
                        repo.translationCards.updateStmt(cardId = args.cardId, textToTranslate = textToTranslate, translation = translation)
                        existingCard.copy(textToTranslate = textToTranslate, translation = translation)
                    }
                }
            }.apply(toBeResponse(EDIT_TRANSLATE_CARD_EXCEPTION))
        }
    }

    @Synchronized
    private fun selectTranslateCardById(cardId: Long): Try<TranslateCard> {
        val repo = getRepo()
        return repo.readableDatabase.doInTransaction {
            val t = repo.translationCards
            val s = repo.cardsSchedule
            val queryArgs = arrayOf(cardId.toString())
            val (textToTranslate, translation) = repo.select(
                query = "select ${t.textToTranslate}, ${t.translation} from $t where ${t.cardId} = ?",
                args = queryArgs,
                columnNames = arrayOf(t.textToTranslate, t.translation),
                rowMapper = { arrayOf(it.getString(), it.getString()) }
            ).rows[0]
            val (lastAccessedAt, nextAccessInSec, nextAccessAt) = repo.select(
                query = "select ${s.lastAccessedAt}, ${s.nextAccessInSec}, ${s.nextAccessAt} from $s where ${s.cardId} = ?",
                args = queryArgs,
                columnNames = arrayOf(s.lastAccessedAt, s.nextAccessInSec, s.nextAccessAt),
                rowMapper = { arrayOf(it.getLong(), it.getLong(), it.getLong()) }
            ).rows[0]
            TranslateCard(
                id = cardId,
                textToTranslate = textToTranslate,
                translation = translation,
                lastAccessedAt = lastAccessedAt,
                nextAccessInSec = nextAccessInSec,
                nextAccessAt = nextAccessAt,
            )
        }
    }

    private fun <T> toBeResponse(errCode: ErrorCode): (Try<T>) -> BeRespose<T> = {
        it
            .map { BeRespose(data = it) }
            .getIfSuccessOrElse {
                BeRespose(err = BeErr(code = errCode.code, msg = it.message ?: it.javaClass.canonicalName))
            }
    }

    @BeMethod
    fun doBackup(): BeRespose<Backup> {
        val dbName = getRepo().dbName
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
            repo.set(repositoryProvider())
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
        getRepo().close()
    }

    private val backupDir = Utils.getBackupsDir(context)

    private fun createBackupFileName(dbPath: File): String {
        return "${dbPath.name}-backup-${dateTimeFormatter.format(clock.instant()).replace(":","-")}"
    }
}