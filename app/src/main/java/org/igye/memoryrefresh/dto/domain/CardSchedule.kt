package org.igye.memoryrefresh.dto.domain

data class CardSchedule(
    val cardId: Long,
    val updatedAt: Long,
    val origDelay: String,
    val delay: String,
    val nextAccessInMillis: Long,
    val nextAccessAt: Long,
)
