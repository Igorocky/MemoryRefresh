package org.igye.memoryrefresh.tools

import org.igye.memoryrefresh.common.Utils
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.regex.Pattern.compile

fun main() {
    Tools.release()
}

object Tools {
    private val DEV_APP_ID = "org.igye.memoryrefresh.dev"
    private val RELEASE_APP_ID = "org.igye.memoryrefresh"

    private val DEV_APP_NAME = "DEV-MemoryRefresh"
    private val RELEASE_APP_NAME = "MemoryRefresh"

    private val DEV_FILE_PROVIDER_NAME = "org.igye.memoryrefresh.fileprovider.dev"
    private val RELEASE_FILE_PROVIDER_NAME = "org.igye.memoryrefresh.fileprovider"

    private val DEV_APP_BACKGROUND_COLOR = "<body style=\"background-color: #d5f5e6\">"
    private val RELEASE_APP_BACKGROUND_COLOR = "<body>"

    private val indexHtmlFilePath = "./app/src/main/assets/index.html"

    fun release() {
        checkWorkingDirectory()
        val releaseVersion = getCurrVersionName()
        changeNamesFromDevToRelease(releaseVersion)
        val tagName = "Release-$releaseVersion"
        commit(tagName)
        buildProject()
        tag(tagName)
        incProjectVersion()
        changeNamesFromReleaseToDev(releaseVersion)
        val newDevVersion = getCurrVersionName()
        commit("Increase version from ${releaseVersion} to ${newDevVersion}")
        println("Done.")
    }

    private fun changeNamesFromDevToRelease(releaseVersion: String) {
        changeApplicationId(DEV_APP_ID, RELEASE_APP_ID)
        changeApplicationName(DEV_APP_NAME, RELEASE_APP_NAME)
        changeFileProviderName(DEV_FILE_PROVIDER_NAME, RELEASE_FILE_PROVIDER_NAME)
        changeAppBackgroundColor(DEV_APP_BACKGROUND_COLOR, RELEASE_APP_BACKGROUND_COLOR)
        changeApplicationVersionInAppContainer(releaseVersion, isRelease = true)
    }

    private fun changeNamesFromReleaseToDev(releaseVersion: String) {
        changeApplicationId(RELEASE_APP_ID, DEV_APP_ID)
        changeApplicationName(RELEASE_APP_NAME, DEV_APP_NAME)
        changeFileProviderName(RELEASE_FILE_PROVIDER_NAME, DEV_FILE_PROVIDER_NAME)
        changeAppBackgroundColor(RELEASE_APP_BACKGROUND_COLOR, DEV_APP_BACKGROUND_COLOR)
        changeApplicationVersionInAppContainer(releaseVersion, isRelease = false)
    }

    private fun changeApplicationVersionInAppContainer(releaseVersion:String, isRelease:Boolean) {
        val oldVersion = if (isRelease) "1.0" else releaseVersion
        val newVersion = if (isRelease) releaseVersion else "1.0"
        replaceSubstringInFile(
            file = File("./app/src/main/java/org/igye/memoryrefresh/config/AppContainer.kt"),
            oldValue = "private val appVersion = \"$oldVersion\"",
            newValue = "private val appVersion = \"$newVersion\""
        )
    }

    private fun changeApplicationId(from:String, to:String) {
        replaceSubstringInFile(File("./app/build.gradle"), from, to)
    }

    private fun changeApplicationName(from:String, to:String) {
        replaceSubstringInFile(File("./app/src/main/res/values/strings.xml"), from, to)
    }

    private fun changeFileProviderName(from:String, to:String) {
        replaceSubstringInFile(File("./app/src/main/AndroidManifest.xml"), from, to)
        replaceSubstringInFile(File("./app/src/main/java/org/igye/memoryrefresh/manager/RepositoryManager.kt"), from, to)
    }

    private fun changeAppBackgroundColor(from:String, to:String) {
        replaceSubstringInFile(File(indexHtmlFilePath), from, to)
    }

    private fun checkWorkingDirectory() {
        log("checkFiles")
        runCommand(
            "git status",
            compile("nothing to commit, working tree clean")
        ) ?: throw RuntimeException("Working directory is not clean.")
    }

    private fun buildProject() {
        log("buildProject")
        val result: Pair<String?, Matcher?>? = runCommand(
            "gradle assembleRelease",
            compile("(.*BUILD SUCCESSFUL in.*)|(.*BUILD FAILED.*)")
        )
        if (result == null || result.first?.contains("BUILD FAILED")?:true) {
            throw RuntimeException("Project build failed.")
        }
    }

    private fun commit(commitMessage: String) {
        val exitCode = runCommandForExitValue("git commit -a -m \"$commitMessage\"")
        if (0 != exitCode) {
            throw RuntimeException("exitCode = $exitCode")
        }
    }

    private fun tag(tagName: String) {
        val exitCode = runCommandForExitValue("git tag $tagName")
        if (0 != exitCode) {
            throw RuntimeException("exitCode = $exitCode")
        }
    }

    private fun getCurrVersionName(): String {
        val matcher = compile(".*versionName \"(\\d+\\.\\d+)\".*", Pattern.DOTALL).matcher(File("./app/build.gradle").readText())
        if (!matcher.matches()) {
            throw RuntimeException("Cannot extract curent version.")
        } else {
            return matcher.group(1)
        }
    }

    private fun incProjectVersion(): String {
        log("incProjectVersion")
        val appBuildGradleFile = File("./app/build.gradle")
        var newVersion: String? = null
        replace(
            srcFile = appBuildGradleFile,
            pattern = compile("versionCode (\\d+)|versionName \"(\\d+)\\.(\\d+)\""),
            dstFile = appBuildGradleFile
        ) { matcher ->
            if (matcher.group().startsWith("versionCode ")) {
                "versionCode ${matcher.group(1).toLong()+1}"
            } else if (matcher.group().startsWith("versionName ")) {
                newVersion = "${matcher.group(2)}.${matcher.group(3).toLong()+1}"
                "versionName \"$newVersion\""
            } else {
                null
            }
        }
        if (newVersion == null) {
            throw RuntimeException("Failed to increase project version.")
        }
        val appContainerFile = File("./app/src/main/java/org/igye/memoryrefresh/config/AppContainer.kt")
        var numOfMatches = 0
        replace(
            srcFile = appContainerFile,
            pattern = compile("val appVersion = \"([\\d\\.]+)\""),
            dstFile = appContainerFile
        ) { matcher ->
            numOfMatches++
            "private val appVersion = \"$newVersion\""
        }
        if (numOfMatches != 1) {
            throw RuntimeException("Failed to increase project version in AppContainer.")
        }
        return newVersion!!
    }

    private fun replace(srcFile: File, pattern: Pattern, dstFile: File, replacement: (Matcher) -> String?) {
        val newContent: String = Utils.replace(srcFile.readText(), pattern, replacement)
        dstFile.parentFile.mkdirs()
        dstFile.writeText(newContent)
    }

    private fun replaceSubstringInFile(file: File, oldValue: String, newValue: String) {
        file.writeText(file.readText().replace(oldValue, newValue))
    }

    private fun runCommand(command: String, pattern: Pattern): Pair<String, Matcher>? {
        return startProcess(command) { process, processOutput ->
            val result = readTill(processOutput, pattern)
            process.destroy()
            result
        }
    }

    private fun runCommandForExitValue(command: String): Int {
        return startProcess(command) { process, processOutput ->
            readTill(processOutput, null)
            process.waitFor()
        }
    }

    private fun <T> startProcess(command: String, outputConsumer: (Process, BufferedReader) -> T): T {
        log("Command: $command")
        val builder = ProcessBuilder("cmd.exe", "/c", command)
        builder.redirectErrorStream(true)
        val proc: Process = builder.start()
        BufferedReader(InputStreamReader(proc.inputStream)).use { reader ->
            return outputConsumer(proc, reader)
        }
    }

    private fun readTill(reader: BufferedReader, pattern: Pattern?): Pair<String, Matcher>? {
        val lines: MutableList<String?> = ArrayList()
        var matcher: Matcher? = null
        var line: String?
        do {
            line = reader.readLine()
            log(line)
            lines.add(line)
            if (line == null) {
                return null
            }
            if (pattern != null) {
                matcher = pattern.matcher(line)
            }
        } while (line != null && (matcher == null || !matcher.matches()))
        return Pair(line!!, matcher!!)
    }

    private fun log(msg: String) {
        println("release>>> $msg")
    }
}