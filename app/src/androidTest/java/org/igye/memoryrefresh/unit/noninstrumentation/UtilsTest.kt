package org.igye.memoryrefresh.unit.noninstrumentation

import org.igye.memoryrefresh.common.Utils.MILLIS_IN_DAY
import org.igye.memoryrefresh.common.Utils.MILLIS_IN_HOUR
import org.igye.memoryrefresh.common.Utils.MILLIS_IN_MONTH
import org.igye.memoryrefresh.common.Utils.correctDelayCoefIfNeeded
import org.igye.memoryrefresh.common.Utils.delayStrToMillis
import org.igye.memoryrefresh.common.Utils.extractUltimatePath
import org.igye.memoryrefresh.common.Utils.millisToDurationStr
import org.igye.memoryrefresh.common.Utils.multiplyDelay
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.time.Instant
import java.time.temporal.ChronoUnit

@RunWith(JUnit4::class)
class UtilsTest {
    @Test
    open fun millisToDurationStr_produces_expected_results() {
        val now = Instant.now()
        assertEquals("0s", millisToDurationStr(instantToMillis(now.plusMillis(100))
                - instantToMillis(now)))
        assertEquals("1s", millisToDurationStr(instantToMillis(now.plusSeconds(1))
                - instantToMillis(now)))
        assertEquals("1m 3s", millisToDurationStr(instantToMillis(now.plusSeconds(63))
                - instantToMillis(now)))
        assertEquals("1m 18s", millisToDurationStr(instantToMillis(now.plusSeconds(78))
                - instantToMillis(now)))
        assertEquals("2m 0s", millisToDurationStr(instantToMillis(now.plusSeconds(120))
                - instantToMillis(now)))
        assertEquals("1h 1m", millisToDurationStr((instantToMillis(now.plus(1, ChronoUnit.HOURS).plusSeconds(75))
                - instantToMillis(now))))
        assertEquals("1d 0h", millisToDurationStr((instantToMillis(now.plus(1, ChronoUnit.DAYS).plusSeconds(75))
                - instantToMillis(now))))
        assertEquals("1d 1h", millisToDurationStr((instantToMillis(now.plus(1, ChronoUnit.DAYS).plus(119, ChronoUnit.MINUTES))
                - instantToMillis(now))))
        assertEquals("1M 0d", millisToDurationStr((instantToMillis(now.plus(30, ChronoUnit.DAYS).plus(119, ChronoUnit.MINUTES))
                - instantToMillis(now))))
        assertEquals("1M 4d", millisToDurationStr((instantToMillis(now.plus(34, ChronoUnit.DAYS))
                - instantToMillis(now))))
        assertEquals("- 1M 4d", millisToDurationStr((instantToMillis(now) - instantToMillis(now.plus(34, ChronoUnit.DAYS)))))
        assertEquals("- 1d 1h",
            millisToDurationStr((instantToMillis(now) - instantToMillis(now.plus(1, ChronoUnit.DAYS).plus(119, ChronoUnit.MINUTES)))))
    }

    @Test
    fun delayStrToMillis_correctly_translates_strings_to_millis() {
        assertEquals(0L, delayStrToMillis("0s"))
        assertEquals(0L, delayStrToMillis("0m"))
        assertEquals(0L, delayStrToMillis("0h"))
        assertEquals(0L, delayStrToMillis("0d"))
        assertEquals(0L, delayStrToMillis("0M"))
        assertEquals(1_000L, delayStrToMillis("1s"))
        assertEquals(50_000L, delayStrToMillis("50s"))
        assertEquals(60_000L, delayStrToMillis("1m"))
        assertEquals(300_000L, delayStrToMillis("5m"))
        assertEquals(MILLIS_IN_HOUR, delayStrToMillis("1h"))
        assertEquals(MILLIS_IN_HOUR*12, delayStrToMillis("12h"))
        assertEquals(MILLIS_IN_DAY, delayStrToMillis("1d"))
        assertEquals(MILLIS_IN_DAY*10, delayStrToMillis("10d"))
        assertEquals(MILLIS_IN_MONTH, delayStrToMillis("1M"))
        assertEquals(MILLIS_IN_MONTH*3, delayStrToMillis("3M"))
        assertEquals(MILLIS_IN_MONTH*3+ MILLIS_IN_DAY*16+ MILLIS_IN_HOUR*11, delayStrToMillis("3M 16d 11h"))
    }

    @Test
    fun correctDelayCoefIfNeeded_returns_expected_results() {
        assertEquals("", correctDelayCoefIfNeeded(""))
        assertEquals("", correctDelayCoefIfNeeded("x"))
        assertEquals("", correctDelayCoefIfNeeded("v"))
        assertEquals("", correctDelayCoefIfNeeded("1"))
        assertEquals("", correctDelayCoefIfNeeded("1."))
        assertEquals("", correctDelayCoefIfNeeded("1.3"))
        assertEquals("", correctDelayCoefIfNeeded("."))
        assertEquals("", correctDelayCoefIfNeeded(".3"))
        assertEquals("x1", correctDelayCoefIfNeeded("x1"))
        assertEquals("x14", correctDelayCoefIfNeeded("x14"))
        assertEquals("x14", correctDelayCoefIfNeeded("x14."))
        assertEquals("x14.3", correctDelayCoefIfNeeded("x14.3"))
        assertEquals("x14.3", correctDelayCoefIfNeeded("x14.31"))
        assertEquals("x14.4", correctDelayCoefIfNeeded("x14.35"))
        assertEquals("x14.3", correctDelayCoefIfNeeded("x14.313455"))
        assertEquals("x0.3", correctDelayCoefIfNeeded("x0.31"))
        assertEquals("1s", correctDelayCoefIfNeeded("1s"))
        assertEquals("7d", correctDelayCoefIfNeeded("7d"))
    }

    @Test
    fun multiplyDelay_returns_expected_results() {
        assertEquals("1s", multiplyDelay("0s", "x1.5"))
        assertEquals("2s", multiplyDelay("1s", "x1.5"))
        assertEquals("2s", multiplyDelay("1s", "x2"))
        assertEquals("3s", multiplyDelay("1s", "x3"))
        assertEquals("2d", multiplyDelay("1d", "x1.5"))
        assertEquals("9d", multiplyDelay("8d", "x1.2"))

        assertEquals("9d", multiplyDelay("8d", "x1.1"))
        assertEquals("9d", multiplyDelay("8d", "x1.2"))
        assertEquals("10d", multiplyDelay("8d", "x1.3"))
        assertEquals("11d", multiplyDelay("8d", "x1.4"))
        assertEquals("12d", multiplyDelay("8d", "x1.5"))
        assertEquals("12d", multiplyDelay("8d", "x1.6"))
        assertEquals("13d", multiplyDelay("8d", "x1.7"))
        assertEquals("14d", multiplyDelay("8d", "x1.8"))
        assertEquals("15d", multiplyDelay("8d", "x1.9"))
        assertEquals("16d", multiplyDelay("8d", "x2"))

        assertEquals("4s", multiplyDelay("5s", "x0.9"))
        assertEquals("3s", multiplyDelay("4s", "x0.9"))
        assertEquals("2s", multiplyDelay("3s", "x0.9"))
        assertEquals("1s", multiplyDelay("2s", "x0.9"))
        assertEquals("1s", multiplyDelay("1s", "x0.9"))

        assertEquals("54m", multiplyDelay("1h", "x0.9"))
        assertEquals("48m", multiplyDelay("54m", "x0.9"))
    }

    @Test
    fun extractUltimatePath_returns_expected_results() {
        assertEquals("path/1", extractUltimatePath("path/1"))
        assertEquals("path/1", extractUltimatePath("v1.0/path/1"))
        assertEquals("/path/1", extractUltimatePath("/path/1"))
        assertEquals("/path/1", extractUltimatePath("/v1.0/path/1"))
    }

    private fun instantToMillis(inst: Instant): Long = inst.toEpochMilli()
}