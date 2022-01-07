package org.igye.memoryrefresh.dto.domain

data class TranslateCardHistResp(
    val isHistoryFull: Boolean,
    val dataHistory: List<TranslateCardHistRecord>,
)
