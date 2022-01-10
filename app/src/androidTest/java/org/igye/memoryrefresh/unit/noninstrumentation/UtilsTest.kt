package org.igye.memoryrefresh.unit.noninstrumentation

import org.igye.memoryrefresh.common.Utils.MILLIS_IN_DAY
import org.igye.memoryrefresh.common.Utils.MILLIS_IN_HOUR
import org.igye.memoryrefresh.common.Utils.MILLIS_IN_MONTH
import org.igye.memoryrefresh.common.Utils.correctDelayCoefIfNeeded
import org.igye.memoryrefresh.common.Utils.delayStrToMillis
import org.igye.memoryrefresh.common.Utils.millisToDurationStr
import org.igye.memoryrefresh.common.Utils.multiplyDelay
import org.junit.Assert
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
        Assert.assertEquals("0s", millisToDurationStr(instantToMillis(now.plusMillis(100))
                - instantToMillis(now)))
        Assert.assertEquals("1s", millisToDurationStr(instantToMillis(now.plusSeconds(1))
                - instantToMillis(now)))
        Assert.assertEquals("1m 3s", millisToDurationStr(instantToMillis(now.plusSeconds(63))
                - instantToMillis(now)))
        Assert.assertEquals("1m 18s", millisToDurationStr(instantToMillis(now.plusSeconds(78))
                - instantToMillis(now)))
        Assert.assertEquals("2m 0s", millisToDurationStr(instantToMillis(now.plusSeconds(120))
                - instantToMillis(now)))
        Assert.assertEquals("1h 1m", millisToDurationStr((instantToMillis(now.plus(1, ChronoUnit.HOURS).plusSeconds(75))
                - instantToMillis(now))))
        Assert.assertEquals("1d 0h", millisToDurationStr((instantToMillis(now.plus(1, ChronoUnit.DAYS).plusSeconds(75))
                - instantToMillis(now))))
        Assert.assertEquals("1d 1h", millisToDurationStr((instantToMillis(now.plus(1, ChronoUnit.DAYS).plus(119, ChronoUnit.MINUTES))
                - instantToMillis(now))))
        Assert.assertEquals("1M 0d", millisToDurationStr((instantToMillis(now.plus(30, ChronoUnit.DAYS).plus(119, ChronoUnit.MINUTES))
                - instantToMillis(now))))
        Assert.assertEquals("1M 4d", millisToDurationStr((instantToMillis(now.plus(34, ChronoUnit.DAYS))
                - instantToMillis(now))))
        Assert.assertEquals("- 1M 4d", millisToDurationStr((instantToMillis(now) - instantToMillis(now.plus(34, ChronoUnit.DAYS)))))
        Assert.assertEquals("- 1d 1h",
            millisToDurationStr((instantToMillis(now) - instantToMillis(now.plus(1, ChronoUnit.DAYS).plus(119, ChronoUnit.MINUTES)))))
    }

    @Test
    fun delayStrToMillis_correctly_translates_strings_to_millis() {
        Assert.assertEquals(0L, delayStrToMillis("0s"))
        Assert.assertEquals(0L, delayStrToMillis("0m"))
        Assert.assertEquals(0L, delayStrToMillis("0h"))
        Assert.assertEquals(0L, delayStrToMillis("0d"))
        Assert.assertEquals(0L, delayStrToMillis("0M"))
        Assert.assertEquals(1_000L, delayStrToMillis("1s"))
        Assert.assertEquals(50_000L, delayStrToMillis("50s"))
        Assert.assertEquals(60_000L, delayStrToMillis("1m"))
        Assert.assertEquals(300_000L, delayStrToMillis("5m"))
        Assert.assertEquals(MILLIS_IN_HOUR, delayStrToMillis("1h"))
        Assert.assertEquals(MILLIS_IN_HOUR*12, delayStrToMillis("12h"))
        Assert.assertEquals(MILLIS_IN_DAY, delayStrToMillis("1d"))
        Assert.assertEquals(MILLIS_IN_DAY*10, delayStrToMillis("10d"))
        Assert.assertEquals(MILLIS_IN_MONTH, delayStrToMillis("1M"))
        Assert.assertEquals(MILLIS_IN_MONTH*3, delayStrToMillis("3M"))
        Assert.assertEquals(MILLIS_IN_MONTH*3+ MILLIS_IN_DAY*16+ MILLIS_IN_HOUR*11, delayStrToMillis("3M 16d 11h"))
    }

    @Test
    fun correctDelayCoefIfNeeded_returns_expected_results() {
        Assert.assertEquals("", correctDelayCoefIfNeeded(""))
        Assert.assertEquals("", correctDelayCoefIfNeeded("x"))
        Assert.assertEquals("", correctDelayCoefIfNeeded("v"))
        Assert.assertEquals("", correctDelayCoefIfNeeded("1"))
        Assert.assertEquals("", correctDelayCoefIfNeeded("1."))
        Assert.assertEquals("", correctDelayCoefIfNeeded("1.3"))
        Assert.assertEquals("", correctDelayCoefIfNeeded("."))
        Assert.assertEquals("", correctDelayCoefIfNeeded(".3"))
        Assert.assertEquals("x1", correctDelayCoefIfNeeded("x1"))
        Assert.assertEquals("x14", correctDelayCoefIfNeeded("x14"))
        Assert.assertEquals("x14", correctDelayCoefIfNeeded("x14."))
        Assert.assertEquals("x14.3", correctDelayCoefIfNeeded("x14.3"))
        Assert.assertEquals("x14.3", correctDelayCoefIfNeeded("x14.31"))
        Assert.assertEquals("x14.4", correctDelayCoefIfNeeded("x14.35"))
        Assert.assertEquals("x14.3", correctDelayCoefIfNeeded("x14.313455"))
        Assert.assertEquals("x0.3", correctDelayCoefIfNeeded("x0.31"))
    }

    @Test
    fun multiplyDelay_returns_expected_results() {
        Assert.assertEquals("1s", multiplyDelay("0s", "x1.5"))
        Assert.assertEquals("2s", multiplyDelay("1s", "x1.5"))
        Assert.assertEquals("2s", multiplyDelay("1s", "x2"))
        Assert.assertEquals("3s", multiplyDelay("1s", "x3"))
        Assert.assertEquals("2d", multiplyDelay("1d", "x1.5"))
        Assert.assertEquals("9d", multiplyDelay("8d", "x1.2"))

        Assert.assertEquals("9d", multiplyDelay("8d", "x1.1"))
        Assert.assertEquals("9d", multiplyDelay("8d", "x1.2"))
        Assert.assertEquals("10d", multiplyDelay("8d", "x1.3"))
        Assert.assertEquals("11d", multiplyDelay("8d", "x1.4"))
        Assert.assertEquals("12d", multiplyDelay("8d", "x1.5"))
        Assert.assertEquals("12d", multiplyDelay("8d", "x1.6"))
        Assert.assertEquals("13d", multiplyDelay("8d", "x1.7"))
        Assert.assertEquals("14d", multiplyDelay("8d", "x1.8"))
        Assert.assertEquals("15d", multiplyDelay("8d", "x1.9"))
        Assert.assertEquals("16d", multiplyDelay("8d", "x2"))

        Assert.assertEquals("4s", multiplyDelay("5s", "x0.9"))
        Assert.assertEquals("3s", multiplyDelay("4s", "x0.9"))
        Assert.assertEquals("2s", multiplyDelay("3s", "x0.9"))
        Assert.assertEquals("1s", multiplyDelay("2s", "x0.9"))
        Assert.assertEquals("1s", multiplyDelay("1s", "x0.9"))

        Assert.assertEquals("54m", multiplyDelay("1h", "x0.9"))
        Assert.assertEquals("48m", multiplyDelay("54m", "x0.9"))
    }

    private fun instantToMillis(inst: Instant): Long = inst.toEpochMilli()
}