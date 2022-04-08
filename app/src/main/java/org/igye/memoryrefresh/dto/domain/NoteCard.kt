package org.igye.memoryrefresh.dto.domain

import org.igye.memoryrefresh.database.CardType

data class NoteCard(
    val id: Long,
    val createdAt: Long,
    val paused: Boolean,
    val tagIds: List<Long>,
    val schedule: CardSchedule,
    val timeSinceLastCheck: String,
    val activatesIn: String,
    val overdue: Double,
    val text:String,
) {
    val type = CardType.NOTE
}
