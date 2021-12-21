package org.igye.memoryrefresh

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalUnit

class TestClock(private var fixedTime: ZonedDateTime) : Clock() {

    constructor(epochMilli: Long): this(millisToZdt(epochMilli))

    fun setFixedTime(fixedTime: ZonedDateTime) {
        this.fixedTime = fixedTime
    }

    fun setFixedTime(epochMilli: Long) {
        this.fixedTime = millisToZdt(epochMilli)
    }

    fun plus(amountToAdd: Long, unit: TemporalUnit?) {
        fixedTime = fixedTime.plus(amountToAdd, unit)
    }

    fun setFixedTime(instant: Instant?) {
        fixedTime = ZonedDateTime.ofInstant(instant, fixedTime.zone)
    }

    fun setFixedTime(
        year: Int, month: Int, dayOfMonth: Int,
        hour: Int, minute: Int, second: Int
    ) {
        fixedTime = ZonedDateTime.of(
            year, month, dayOfMonth, hour, minute, second, 0, ZoneId.of("UTC")
        )
    }

    override fun getZone(): ZoneId {
        return fixedTime.zone
    }

    override fun withZone(zone: ZoneId): Clock {
        return TestClock(fixedTime.withZoneSameInstant(zone))
    }

    override fun instant(): Instant {
        return fixedTime.toInstant()
    }

    companion object {
        private fun millisToZdt(epochMilli: Long) = Instant.ofEpochMilli(epochMilli).atZone(ZoneId.systemDefault())
    }
}