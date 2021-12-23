package org.igye.memoryrefresh

import org.igye.memoryrefresh.common.Utils.MILLIS_IN_DAY
import org.igye.memoryrefresh.common.Utils.MILLIS_IN_HOUR
import org.igye.memoryrefresh.common.Utils.MILLIS_IN_MONTH
import org.igye.memoryrefresh.common.Utils.delayStrToMillis
import org.igye.memoryrefresh.common.Utils.millisToDurationStr
import org.junit.Assert
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class UtilsTest {
    @Test
    open fun millisToDurationStr_produces_expected_results() {
        val now = Instant.now()
        Assert.assertEquals("1m", millisToDurationStr(instantToMillis(now.plusSeconds(63))
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
        Assert.assertEquals(0L, delayStrToMillis("0m"))
        Assert.assertEquals(0L, delayStrToMillis("0h"))
        Assert.assertEquals(0L, delayStrToMillis("0d"))
        Assert.assertEquals(0L, delayStrToMillis("0M"))
        Assert.assertEquals(60_000L, delayStrToMillis("1m"))
        Assert.assertEquals(300_000L, delayStrToMillis("5m"))
        Assert.assertEquals(MILLIS_IN_HOUR, delayStrToMillis("1h"))
        Assert.assertEquals(MILLIS_IN_HOUR*12, delayStrToMillis("12h"))
        Assert.assertEquals(MILLIS_IN_DAY, delayStrToMillis("1d"))
        Assert.assertEquals(MILLIS_IN_DAY*10, delayStrToMillis("10d"))
        Assert.assertEquals(MILLIS_IN_MONTH, delayStrToMillis("1M"))
        Assert.assertEquals(MILLIS_IN_MONTH*3, delayStrToMillis("3M"))
    }

    private fun instantToMillis(inst: Instant): Long = inst.toEpochMilli()
}