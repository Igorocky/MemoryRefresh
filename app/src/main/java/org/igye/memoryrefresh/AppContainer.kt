package org.igye.memoryrefresh

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.igye.memoryrefresh.database.Repository
import org.igye.memoryrefresh.database.tables.v1.CardsScheduleTable
import org.igye.memoryrefresh.database.tables.v1.CardsTable
import org.igye.memoryrefresh.database.tables.v1.TranslationCardsLogTable
import org.igye.memoryrefresh.database.tables.v1.TranslationCardsTable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AppContainer(
    val context: Context,
    val dbName: String = "memory-refresh-db"
) {
    val beThreadPool: ExecutorService = Executors.newFixedThreadPool(4)
    val dataManager = DataManager(context = context, repositoryProvider = {createNewRepo()})
    val settingsManager = SettingsManager(context = context)
    val httpsServerManager = HttpsServerManager(appContext = context, settingsManager = settingsManager, javascriptInterface = listOf(dataManager))

    val cards = CardsTable()
    val cardsSchedule = CardsScheduleTable(cards = cards)
    val translationCards = TranslationCardsTable(cards = cards)
    val translationCardsLog = TranslationCardsLogTable(translationCards = translationCards)

    fun createNewRepo(): Repository {
        return Repository(
            context = context,
            dbName = dbName,
            cards = cards,
            cardsSchedule = cardsSchedule,
            translationCards = translationCards,
            translationCardsLog = translationCardsLog,
        )
    }

    val viewModelFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return if (modelClass.isAssignableFrom(MainActivityViewModel::class.java)) {
                createMainActivityViewModel() as T
            } else if (modelClass.isAssignableFrom(SharedFileReceiverViewModel::class.java)) {
                createSharedFileReceiverViewModel() as T
            } else {
                null as T
            }
        }
    }

    private fun createMainActivityViewModel(): MainActivityViewModel {
        return MainActivityViewModel(
            appContext = context,
            dataManager = dataManager,
            httpsServerManager = httpsServerManager,
            beThreadPool = beThreadPool
        )
    }

    private fun createSharedFileReceiverViewModel(): SharedFileReceiverViewModel {
        return SharedFileReceiverViewModel(
            appContext = context,
            beThreadPool = beThreadPool
        )
    }
}