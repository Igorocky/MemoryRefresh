package org.igye.memoryrefresh.dto.domain

import org.igye.memoryrefresh.database.CardType

data class TranslateCard(
    val id: Long,
    val createdAt: Long,
    val paused: Boolean,
    val tagIds: List<Long>,
    val schedule: CardSchedule,
    val timeSinceLastCheck: String,
    val overdue: Double,
    val textToTranslate:String,
    val translation:String
) {
    val type = CardType.TRANSLATION
}
