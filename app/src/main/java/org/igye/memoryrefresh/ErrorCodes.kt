package org.igye.memoryrefresh

enum class ErrorCode(val code: Long) {
    DELAY_DURATION_IS_TOO_BIG(12L),
    VALIDATE_TRANSLATE_CARD_EXCEPTION(11L),
    VALIDATE_TRANSLATE_CARD_TRANSLATION_IS_EMPTY(10L),
    GET_TRANSLATE_CARD_BY_ID(9L),
    GET_NEXT_CARD_TO_REPEAT(8L),
    EDIT_TRANSLATE_CARD_EXCEPTION(7L),
    EDIT_TRANSLATE_CARD_DELAY_IS_EMPTY(6L),
    EDIT_TRANSLATE_CARD_TRANSLATION_IS_EMPTY(5L),
    EDIT_TRANSLATE_CARD_TEXT_TO_TRANSLATE_IS_EMPTY(4L),
    SAVE_NEW_TRANSLATE_CARD_EXCEPTION(3L),
    SAVE_NEW_TRANSLATE_CARD_TRANSLATION_IS_EMPTY(2L),
    SAVE_NEW_TRANSLATE_CARD_TEXT_TO_TRANSLATE_IS_EMPTY(1L),
    GENERAL(-1L)

//    init {
//        println("values().size = ${values().size}")
//        if (values().asSequence().map { it.code }.toSet().size != values().size) {
//            throw MemoryRefreshException("values().asSequence().map { it.code }.toSet().size != values().size")
//        }
//    }
}