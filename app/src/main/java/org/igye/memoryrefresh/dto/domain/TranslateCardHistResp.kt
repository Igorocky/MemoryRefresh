package org.igye.memoryrefresh.dto.domain

data class TranslateCardHistResp(
    val historyRecords: List<TranslateCardHistRecord>,
    val isHistoryFull: Boolean
)
