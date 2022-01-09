package org.igye.memoryrefresh.common

import android.content.Context
import android.database.sqlite.SQLiteStatement
import com.google.gson.Gson
import org.igye.memoryrefresh.ErrorCode
import org.igye.memoryrefresh.database.Table
import org.igye.memoryrefresh.dto.common.BeErr
import org.igye.memoryrefresh.dto.common.BeRespose
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.InetAddress
import java.net.NetworkInterface
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
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

    fun createMethodMap(jsInterfaces: List<Any>, filter: (BeMethod) -> Boolean = {true}): Map<String, (String) -> String> {
        val resultMap = HashMap<String, (String) -> String>()
        jsInterfaces.forEach{ jsInterface ->
            jsInterface.javaClass.methods.asSequence()
                .filter {
                    val beMethod = it.getAnnotation(BeMethod::class.java)
                    beMethod != null && filter(beMethod)
                }
                .forEach { method ->
                    if (resultMap.containsKey(method.name)) {
                        throw MemoryRefreshException(
                            msg = "resultMap.containsKey('${method.name}')",
                            errCode = ErrorCode.DUPLICATED_BE_METHOD_NAME
                        )
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

    val MILLIS_IN_SECOND: Long = 1000
    val SECONDS_IN_MINUTE: Long = 60
    val MINUTES_IN_HOUR: Long = 60
    val HOURS_IN_DAY: Long = 24
    val DAYS_IN_MONTH: Long = 30
    val MILLIS_IN_MINUTE: Long = MILLIS_IN_SECOND * SECONDS_IN_MINUTE
    val MILLIS_IN_HOUR: Long = MILLIS_IN_MINUTE * MINUTES_IN_HOUR
    val MILLIS_IN_DAY: Long = MILLIS_IN_HOUR * HOURS_IN_DAY
    val MILLIS_IN_MONTH: Long = MILLIS_IN_DAY * DAYS_IN_MONTH
    private val DURATION_UNITS = charArrayOf('M', 'd', 'h', 'm', 's')
    fun millisToDurationStr(millis: Long): String {
        var diff = millis
        val sb = StringBuilder()
        if (diff < 0) {
            sb.append("- ")
        }
        val months: Long = Math.abs(diff / MILLIS_IN_MONTH)
        diff = diff % MILLIS_IN_MONTH
        val days: Long = Math.abs(diff / MILLIS_IN_DAY)
        diff = diff % MILLIS_IN_DAY
        val hours: Long = Math.abs(diff / MILLIS_IN_HOUR)
        diff = diff % MILLIS_IN_HOUR
        val minutes: Long = Math.abs(diff / MILLIS_IN_MINUTE)
        diff = diff % MILLIS_IN_MINUTE
        val seconds: Long = Math.abs(diff / MILLIS_IN_SECOND)
        val parts = longArrayOf(months, days, hours, minutes, seconds)
        var idx = 0
        while (idx < parts.size && parts[idx] == 0L) {
            idx++
        }
        if (idx == parts.size) {
            return "0s"
        }
        sb.append(parts[idx]).append(DURATION_UNITS[idx])
        if (idx < parts.size - 1) {
            idx++
            sb.append(" ").append(parts[idx]).append(DURATION_UNITS[idx])
        }
        return sb.toString()
    }

    fun delayStrToMillis(pauseDuration: String): Long {
        return pauseDuration.split(" ").asSequence().map { delayStrToMillisInner(it) }.sum()
    }

    private val attemptDelayPattern = Pattern.compile("^(\\d+)(M|d|h|m|s)$")
    private fun delayStrToMillisInner(pauseDuration: String): Long {
        val matcher = attemptDelayPattern.matcher(pauseDuration)
        if (!matcher.matches()) {
            throw MemoryRefreshException(
                msg = "Pause duration '$pauseDuration' is in incorrect format.",
                errCode = ErrorCode.PAUSE_DURATION_IS_IN_INCORRECT_FORMAT
            )
        }
        var amount = matcher.group(1).toLong()
        if (amount > 365) {
            throw MemoryRefreshException(
                msg = "Delay duration of '$amount' is too big.",
                errCode = ErrorCode.DELAY_DURATION_IS_TOO_BIG
            )
        }
        var unit = matcher.group(2)
        if ("M" == unit) {
            amount *= 30
            unit = "d"
        }
        return getChronoUnit(unit).getDuration().getSeconds() * amount * 1000
    }

    private val delayCoefPattern = Pattern.compile("^(?:x(\\d+)(?:\\.(\\d*))?)?$")
    private fun delayCoefToBigDeciaml(coef: String): BigDecimal? {
        val matcher = delayCoefPattern.matcher(coef)
        if (!matcher.matches()) {
            return null
        }
        val integerPart = matcher.group(1)
        val rationalPart = matcher.group(2)
        if (integerPart == null) {
            return null
        }
        return BigDecimal(
            "$integerPart.${if (rationalPart == null || rationalPart == "") "0" else rationalPart}"
        ).setScale(if (rationalPart == null || rationalPart == "") 0 else 1, RoundingMode.HALF_UP)
    }

    fun correctDelayCoefIfNeeded(coef: String): String {
        val bd = delayCoefToBigDeciaml(coef)
        if (bd == null) {
            return ""
        } else {
            return "x" + bd
        }
    }

    fun multiplyDelay(base:String, coef:String): String {
        val baseMillis = delayStrToMillis(base)
        val bd = delayCoefToBigDeciaml(coef)
        val newMillis = BigDecimal(baseMillis).multiply(bd).setScale(0, RoundingMode.HALF_UP).toLong()
        return millisToDurationStr(newMillis)
    }

    fun <T> toBeResponse(errCode: ErrorCode): (Try<T>) -> BeRespose<T> = {
        it
            .map { BeRespose(data = it) }
            .getIfSuccessOrElse {
//                if (true) throw it
                BeRespose(
                    err = BeErr(
                        code = (if (it is MemoryRefreshException) it.errCode.code else null)?:errCode.code,
                        msg = if (it is MemoryRefreshException) it.msg else ("${it.message} (${it.javaClass.canonicalName})")
                    )
                )
            }
    }

    fun executeInsert(table: Table, stmt: SQLiteStatement): Long {
        val id = stmt.executeInsert()
        if (id < 0) {
            throw MemoryRefreshException(
                errCode = ErrorCode.ERROR_INSERTING_A_RECORD_TO_A_TABLE,
                msg = "Negative id was returned when inserting a record to $table table."
            )
        } else {
            return id
        }
    }

    fun executeUpdateDelete(table: Table, stmt: SQLiteStatement, expectedNumberOfRows: Int? = null): Int {
        val count = stmt.executeUpdateDelete()
        if (expectedNumberOfRows != null && count != expectedNumberOfRows) {
            throw MemoryRefreshException(
                errCode = ErrorCode.ERROR_UPDATING_OR_DELETING_FROM_A_TABLE,
                msg = "Number of rows updated/deleted from $table doesn't match expected value."
            )
        } else {
            return count
        }
    }

    private fun getChronoUnit(unit: String): TemporalUnit {
        return when (unit) {
            "s" -> ChronoUnit.SECONDS
            "m" -> ChronoUnit.MINUTES
            "h" -> ChronoUnit.HOURS
            "d" -> ChronoUnit.DAYS
            else -> throw MemoryRefreshException(msg = "Unrecognized time interval unit: $unit", errCode = ErrorCode.UNRECOGNIZED_TIME_INTERVAL_UNIT)
        }
    }
}