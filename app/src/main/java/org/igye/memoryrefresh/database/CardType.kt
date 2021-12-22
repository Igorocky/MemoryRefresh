package org.igye.memoryrefresh.database

import org.igye.memoryrefresh.ErrorCode
import org.igye.memoryrefresh.MemoryRefreshException

enum class CardType(val intValue: Long) {
    TRANSLATION(intValue = 1);

    companion object {
        fun fromInt(intValue: Long): CardType {
            for (value in values()) {
                if (value.intValue == intValue) {
                    return value
                }
            }
            throw MemoryRefreshException(msg = "Unexpected CardType code of '$intValue'", errCode = ErrorCode.GENERAL)
        }
    }
}