package org.igye.memoryrefresh.dto.domain

data class TranslateCardValidationHistRecord(
    val cardId: Long,
    val recId: Long,
    val timestamp: Long,
    val translation:String,
    val isCorrect: Boolean,
)