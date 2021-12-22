package org.igye.memoryrefresh.dto.domain

data class CardSchedule(
    val cardId: Long,
    val delay: String,
    val nextAccessInMillis: Long,
    val nextAccessAt: Long,
)
