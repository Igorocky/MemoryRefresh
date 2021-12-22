package org.igye.memoryrefresh.dto.domain

data class CardSchedule(
    val cardId: Long,
    val lastAccessedAt: Long,
    val nextAccessInSec: Long,
    val nextAccessAt: Long,
)
