package org.igye.memoryrefresh.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.igye.memoryrefresh.ErrorCode
import org.igye.memoryrefresh.ErrorCode.NO_CARDS_TO_IMPORT
import org.igye.memoryrefresh.ErrorCode.STALE_CARDS_TO_IMPORT
import org.igye.memoryrefresh.common.BeMethod
import org.igye.memoryrefresh.common.MemoryRefreshException
import org.igye.memoryrefresh.common.Utils
import org.igye.memoryrefresh.database.CardType
import org.igye.memoryrefresh.dto.common.BeErr
import org.igye.memoryrefresh.dto.common.BeRespose
import org.igye.memoryrefresh.dto.common.SharedFileType
import org.igye.memoryrefresh.dto.common.SharedFileType.*
import org.igye.memoryrefresh.dto.domain.TranslateCardContainerExpImpDto
import org.igye.memoryrefresh.manager.DataManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.ZipInputStream

class SharedFileReceiverViewModel(appContext: Context, beThreadPool: ExecutorService, val dataManager: DataManager): WebViewViewModel(
    appContext = appContext,
    rootReactComponent = "SharedFileReceiver",
    beThreadPool = beThreadPool,
    javascriptInterface = listOf(dataManager)
) {
    @Volatile lateinit var sharedFileUri: String
    @Volatile lateinit var onClose: () -> Unit

    private val cardsToImport = AtomicReference<Pair<String, TranslateCardContainerExpImpDto>?>()

    @BeMethod
    fun closeSharedFileReceiver(): BeRespose<Boolean> {
        onClose()
        return BeRespose(data = true)
    }

    @BeMethod
    @Synchronized
    fun getSharedFileInfo(): BeRespose<Map<String, Any?>> {
        return BeRespose(ErrorCode.GET_SHARED_FILE_INFO) {
            val fileName = getFileName(sharedFileUri)
            val fileType = getFileType(fileName)
            val cardsCollection = parseImportCardsCollection(sharedFileUri)
            cardsToImport.set(sharedFileUri to cardsCollection)
            mapOf(
                "uri" to sharedFileUri,
                "name" to fileName,
                "type" to fileType,
                "importTranslateCardsInfo" to when (fileType) {
                    EXPORTED_CARDS -> {
                        dataManager.getImportTranslateCardsInfo(cardsCollection)
                    }
                    else -> null
                }
            )
        }
    }

    data class SaveSharedFileArgs(val fileUri: String, val fileType: SharedFileType, val fileName: String)
    @BeMethod
    fun saveSharedFile(args: SaveSharedFileArgs): BeRespose<Any> {
        return if (args.fileUri != sharedFileUri) {
            BeRespose(err = BeErr(code = ErrorCode.UNEXPECTED_SHARED_FILE_URI.code, msg = "fileInfo.uri != sharedFileUri"))
        } else {
            BeRespose(data = copyFile(fileUri = args.fileUri, fileName = args.fileName, fileType = args.fileType))
        }
    }

    data class ImportTranslateCardsArgs(val fileUri: String, val paused: Boolean = false, val additionalTags: Set<Long>)
    @BeMethod
    @Synchronized
    fun importTranslateCards(args: ImportTranslateCardsArgs): BeRespose<Int> {
        return BeRespose(ErrorCode.IMPORT_TRANSLATE_CARDS) {
            val uriAndCards = cardsToImport.get()
            if (uriAndCards == null) {
                throw MemoryRefreshException(msg = "uriAndCards", errCode = NO_CARDS_TO_IMPORT)
            }
            val (actualUri, cardsCollection) = uriAndCards
            if (actualUri != args.fileUri) {
                throw MemoryRefreshException(msg = "actualUri != args.fileUri", errCode = STALE_CARDS_TO_IMPORT)
            } else {
                cardsToImport.set(null)
            }
            dataManager.importTranslateCards(
                cardsContainer = cardsCollection,
                paused = args.paused,
                additionalTags = args.additionalTags
            )
        }
    }

    private fun getFileName(uri: String): String {
        appContext.contentResolver.query(Uri.parse(uri), null, null, null, null)!!.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            return cursor.getString(nameIndex)
        }
    }

    private fun getFileType(fileName: String): SharedFileType {
        return if (fileName.endsWith(".bks")) {
            KEYSTORE
        } else if (fileName.endsWith(".zip")) {
            BACKUP
        } else if (fileName.endsWith(".mrz")) {
            EXPORTED_CARDS
        } else {
            throw MemoryRefreshException(msg = "unsupported file type.", errCode = ErrorCode.UNSUPPORTED_FILE_TYPE)
        }
    }

    private fun copyFile(fileUri: String, fileType: SharedFileType, fileName: String): Long {
        val destinationDir = when(fileType) {
            BACKUP -> Utils.getBackupsDir(appContext)
            KEYSTORE -> Utils.getKeystoreDir(appContext)
            EXPORTED_CARDS -> Utils.getExportDir(appContext)
        }
        if (fileType == KEYSTORE) {
            Utils.getKeystoreDir(appContext).listFiles().forEach(File::delete)
        }
        FileInputStream(appContext.contentResolver.openFileDescriptor(Uri.parse(fileUri), "r")!!.fileDescriptor).use{ inp ->
            FileOutputStream(File(destinationDir, fileName)).use { out ->
                return inp.copyTo(out)
            }
        }
    }

    @Synchronized
    private fun parseImportCardsCollection(fileUri: String): TranslateCardContainerExpImpDto {
        return FileInputStream(appContext.contentResolver.openFileDescriptor(Uri.parse(fileUri), "r")!!.fileDescriptor).use{ inp ->
            val cardsContainer = ZipInputStream(inp).use { zipFile ->
                zipFile.nextEntry
                Utils.strToObj(
                    zipFile.readBytes().toString(java.nio.charset.StandardCharsets.UTF_8),
                    TranslateCardContainerExpImpDto::class.java
                )
            }
            if (cardsContainer.version != 1) {
                throw MemoryRefreshException(
                    msg = "cards.version != 1",
                    errCode = ErrorCode.IMPORT_TRANSLATE_CARDS_UNSUPPORTED_VERSION
                )
            }
            val cardTypes = cardsContainer.cards.asSequence().map { it.type }.toSet()
            if (cardTypes.size != 1 || cardTypes.first() != CardType.TRANSLATION) {
                throw MemoryRefreshException(
                    msg = "Unsupported card type: ${cardTypes.first()}",
                    errCode = ErrorCode.IMPORT_TRANSLATE_CARDS_UNSUPPORTED_CARD_TYPE
                )
            }
            cardsContainer
        }
    }

}
