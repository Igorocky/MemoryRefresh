package org.igye.memoryrefresh.dto.domain

import org.igye.memoryrefresh.database.CardType
import org.igye.memoryrefresh.database.TranslationCardDirection

data class TranslateCardExpImpDto(
    val paused: Boolean,
    val tags: Set<String>,
    val textToTranslate:String,
    val translation:String,
    val direction:TranslationCardDirection,
) {
    val type = CardType.TRANSLATION
}
