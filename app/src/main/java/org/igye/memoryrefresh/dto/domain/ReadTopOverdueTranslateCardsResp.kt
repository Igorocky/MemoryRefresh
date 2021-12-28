package org.igye.memoryrefresh.dto.domain

data class ReadTopOverdueTranslateCardsResp(
    val cards: List<TranslateCard> = emptyList(),
    val nextCardIn: String = "",
)
