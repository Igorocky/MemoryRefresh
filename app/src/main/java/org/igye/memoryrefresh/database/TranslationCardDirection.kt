package org.igye.memoryrefresh.database

import org.igye.memoryrefresh.ErrorCode
import org.igye.memoryrefresh.common.MemoryRefreshException

enum class TranslationCardDirection(val intValue: Long) {
    FOREIGN_NATIVE(intValue = 0),
    NATIVE_FOREIGN(intValue = 1);

    companion object {
        fun fromInt(intValue: Long): TranslationCardDirection {
            for (value in values()) {
                if (value.intValue == intValue) {
                    return value
                }
            }
            throw MemoryRefreshException(
                msg = "Unexpected TranslationCardDirection code of '$intValue'",
                errCode = ErrorCode.UNEXPECTED_TRANSLATION_CARD_DIRECTION_CODE
            )
        }
    }
}