package org.igye.memoryrefresh.dto.domain

import org.igye.memoryrefresh.database.CardType

open class Card(
    val id: Long,
    val type: CardType,
)
