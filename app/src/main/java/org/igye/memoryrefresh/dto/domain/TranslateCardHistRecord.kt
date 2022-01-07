package org.igye.memoryrefresh.dto.domain

data class TranslateCardHistRecord(
    val cardId: Long,
    val verId: Long,
    val timestamp: Long,
    val textToTranslate: String,
    val translation: String,
    val validationHistory: MutableList<TranslateCardValidationHistRecord>,
)