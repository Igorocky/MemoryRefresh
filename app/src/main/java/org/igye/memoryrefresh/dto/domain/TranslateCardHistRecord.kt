package org.igye.memoryrefresh.dto.domain

data class TranslateCardHistRecord(
    val recId: Long,
    val cardId: Long,
    val timestamp: Long,
    val translation:String,
    val isCorrect: Boolean,
)
