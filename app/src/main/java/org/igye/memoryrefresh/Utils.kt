package org.igye.memoryrefresh

import android.content.Context
import com.google.gson.Gson
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

object Utils {
    private val gson = Gson()

    fun <E> isEmpty(col: Collection<E>?): Boolean = col?.isEmpty()?:true
    fun <E> isNotEmpty(col: Collection<E>?): Boolean = !isEmpty(col)
    fun getBackupsDir(context: Context): File = createDirIfNotExists(File(context.filesDir, "backup"))
    fun getKeystoreDir(context: Context): File = createDirIfNotExists(File(context.filesDir, "keystore"))
    fun <T> strToObj(str:String, classOfT: Class<T>): T = gson.fromJson(str, classOfT)
    fun objToStr(obj:Any): String = gson.toJson(obj)

    fun createMethodMap(jsInterfaces: List<Any>): Map<String, (String) -> String> {
        val resultMap = HashMap<String, (String) -> String>()
        jsInterfaces.forEach{ jsInterface ->
            jsInterface.javaClass.methods.asSequence()
                .filter { it.getAnnotation(BeMethod::class.java) != null }
                .forEach { method ->
                    if (resultMap.containsKey(method.name)) {
                        throw MemoryRefreshException("resultMap.containsKey('${method.name}')")
                    } else {
                        resultMap.put(method.name) { argStr ->
                            val parameterTypes = method.parameterTypes
                            gson.toJson(
                                if (parameterTypes.isNotEmpty()) {
                                    method.invoke(jsInterface, gson.fromJson(argStr, parameterTypes[0]))
                                } else {
                                    method.invoke(jsInterface)
                                }
                            )
                        }
                    }
                }
        }
        return resultMap.toMap()
    }

    private fun createDirIfNotExists(dir: File): File {
        if (!dir.exists()) {
            dir.mkdir()
        }
        return dir
    }

    fun getIpAddress(): String? {
        val interfaces: List<NetworkInterface> = Collections.list(NetworkInterface.getNetworkInterfaces())
        for (intf in interfaces) {
            val addrs: List<InetAddress> = Collections.list(intf.getInetAddresses())
            for (addr in addrs) {
                if (!addr.isLoopbackAddress()) {
                    val sAddr: String = addr.getHostAddress()
                    if (sAddr.indexOf(':') < 0) {
                        return sAddr
                    }
                }
            }
        }
        return "???.???.???.???"
    }

    fun replace(content: String, pattern: Pattern, replacement: (Matcher) -> String?): String {
        val matcher = pattern.matcher(content)
        val newContent = StringBuilder()
        var prevEnd = 0
        while (matcher.find()) {
            newContent.append(content, prevEnd, matcher.start())
            val replacementValue = replacement(matcher)
            if (replacementValue != null) {
                newContent.append(replacementValue)
            } else {
                newContent.append(matcher.group(0))
            }
            prevEnd = matcher.end()
        }
        newContent.append(content, prevEnd, content.length)
        return newContent.toString()
    }
}