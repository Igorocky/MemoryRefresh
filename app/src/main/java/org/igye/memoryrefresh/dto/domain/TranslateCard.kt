package org.igye.memoryrefresh.dto.domain

import org.igye.memoryrefresh.database.CardType

data class TranslateCard(
    val id: Long,
    val textToTranslate:String,
    val translation:String,
    val schedule: CardSchedule,
) {
    val type = CardType.TRANSLATION
}
