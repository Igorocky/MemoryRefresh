package org.igye.memoryrefresh

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AppContainer(private val context: Context) {
    val beThreadPool: ExecutorService = Executors.newFixedThreadPool(4)
    val dataManager = DataManager(context = context)
    val settingsManager = SettingsManager(context = context)
    val httpsServerManager = HttpsServerManager(appContext = context, settingsManager = settingsManager, javascriptInterface = listOf(dataManager))

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

    fun createMainActivityViewModel(): MainActivityViewModel {
        return MainActivityViewModel(
            appContext = context,
            dataManager = dataManager,
            httpsServerManager = httpsServerManager,
            beThreadPool = beThreadPool
        )
    }

    fun createSharedFileReceiverViewModel(): SharedFileReceiverViewModel {
        return SharedFileReceiverViewModel(
            appContext = context,
            beThreadPool = beThreadPool
        )
    }
}