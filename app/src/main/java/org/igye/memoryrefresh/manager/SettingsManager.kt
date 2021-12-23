package org.igye.memoryrefresh.manager

import android.content.Context
import org.igye.memoryrefresh.common.Utils
import org.igye.memoryrefresh.dto.common.AppSettings
import org.igye.memoryrefresh.dto.common.HttpServerSettings
import java.io.File
import java.io.FileOutputStream

class SettingsManager(
    private val context: Context,
) {
    private val applicationSettingsFileName = "settings.json"
    private val settingsFile = File(context.filesDir, applicationSettingsFileName)

    fun getApplicationSettings(): AppSettings {
        if (!settingsFile.exists()) {
            saveApplicationSettings(AppSettings(httpServerSettings = HttpServerSettings()))
        }
        return Utils.strToObj(settingsFile.readText(), AppSettings::class.java)
    }

    fun saveApplicationSettings(appSettings: AppSettings) {
        FileOutputStream(settingsFile).use {
            it.write(Utils.objToStr(appSettings).toByteArray())
        }
    }

    fun getKeyStorFile(): File? {
        var result: File? = null
        for (keyStor in Utils.getKeystoreDir(context).listFiles()) {
            if (result == null) {
                result = keyStor
            } else {
                keyStor.delete()
            }
        }
        return result
    }

    fun getHttpServerSettings(): HttpServerSettings {
        val appSettings = getApplicationSettings()
        return appSettings.httpServerSettings.copy(keyStoreName = getKeyStorFile()?.name?:"")
    }

    fun saveHttpServerSettings(httpServerSettings: HttpServerSettings): HttpServerSettings {
        val appSettings = getApplicationSettings()
        saveApplicationSettings(appSettings.copy(httpServerSettings = httpServerSettings))
        return getHttpServerSettings()
    }
}