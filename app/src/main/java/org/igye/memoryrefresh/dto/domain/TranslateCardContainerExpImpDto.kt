package org.igye.memoryrefresh.dto.domain

data class TranslateCardContainerExpImpDto(
    val version: Int,
    val cards: List<TranslateCardExpImpDto>,
) {
    val type = "TranslateCards"
}
