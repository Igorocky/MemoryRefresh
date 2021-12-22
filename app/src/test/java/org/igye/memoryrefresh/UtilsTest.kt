package org.igye.memoryrefresh

import org.igye.memoryrefresh.Utils.millisToDurationStr
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

    private fun instantToMillis(inst: Instant): Long = inst.toEpochMilli()
}