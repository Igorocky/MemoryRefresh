package org.igye.memoryrefresh.dto.domain

import org.igye.memoryrefresh.database.CardType
import org.igye.memoryrefresh.database.TranslationCardDirection

data class TranslateCard(
    val id: Long,
    val createdAt: Long,
    val paused: Boolean,
    val tagIds: List<Long>,
    val schedule: CardSchedule,
    val timeSinceLastCheck: String,
    val activatesIn: String,
    val overdue: Double,
    val textToTranslate:String,
    val translation:String,
    val direction:TranslationCardDirection,
) {
    val type = CardType.TRANSLATION
}
