package org.igye.memoryrefresh.dto.domain

import org.igye.memoryrefresh.database.CardType

data class CardOverdue(
    val cardId: Long,
    val cardType: CardType,
    val overdue: Double
)
