package org.igye.memoryrefresh.dto.domain

import org.igye.memoryrefresh.database.CardType

data class GetNextCardToRepeatResp(
    val cardId: Long = -1,
    val cardType: CardType = CardType.TRANSLATION,
    val cardsRemain: Int = 0,
    val isCardsRemainExact: Boolean = true,
    val nextCardIn: String = "",
)
