package org.igye.memoryrefresh

enum class ErrorCode(val code: Long) {
    STALE_CARDS_TO_IMPORT(61L),
    NO_CARDS_TO_IMPORT(60L),
    GET_SHARED_FILE_INFO(59L),
    IMPORT_TRANSLATE_CARDS_UNSUPPORTED_CARD_TYPE(58L),
    IMPORT_TRANSLATE_CARDS_UNSUPPORTED_VERSION(57L),
    IMPORT_TRANSLATE_CARDS(56L),
    EXPORT_TRANSLATE_CARDS(55L),
    COULD_NOT_DELETE_EXPORT_DIR(54L),
    UNEXPECTED_TRANSLATION_CARD_DIRECTION_CODE(53L),
    READ_DEFAULT_DELAY_COEFS(52L),
    UPDATE_DEFAULT_DELAY_COEFS(51L),
    READ_MAX_DELAY(50L),
    UPDATE_MAX_DELAY(49L),
    READ_DELAY_COEFS(48L),
    UPDATE_DELAY_COEFS(47L),
    DATABASE_VERSION_MISMATCH(46L),
    UPGRADE_IS_NOT_IMPLEMENTED(45L),
    DOWNGRADE_IS_NOT_SUPPORTED(44L),
    GET_CARD_TO_TAG_MAPPING(43L),
    READ_TOP_OVERDUE_TRANSLATE_CARDS(42L),
    READ_TRANSLATE_CARD_BY_FILTER(41L),
    READ_TRANSLATE_CARD_BY_ID(40L),
    DELETE_TAG_TAG_IS_USED(39L),
    READ_ALL_TAGS(38L),
    UPDATE_TAG_NAME_IS_NOT_UNIQUE(37L),
    SAVE_NEW_TAG_NAME_IS_NOT_UNIQUE(36L),
    DELETE_TAG(35L),
    UPDATE_TAG(34L),
    UPDATE_TAG_NAME_IS_EMPTY(33L),
    SAVE_NEW_TAG(32L),
    ERROR_UPDATING_OR_DELETING_FROM_A_TABLE(31L),
    ERROR_INSERTING_A_RECORD_TO_A_TABLE(30L),
    SAVE_NEW_TAG_NAME_IS_EMPTY(29L),
    UNSUPPORTED_FILE_TYPE(28L),
    UNEXPECTED_CARD_TYPE_CODE(24L),
    DUPLICATED_BE_METHOD_NAME(23L),
    UNEXPECTED_SHARED_FILE_URI(22L),
    ERROR_WHILE_STARTING_HTTP_SERVER(21L),
    HTTP_SERVER_IS_ALREADY_RUNNING(20L),
    KEY_STORE_IS_NOT_DEFINED(19L),
    UNRECOGNIZED_TIME_INTERVAL_UNIT(18L),
    PAUSE_DURATION_IS_IN_INCORRECT_FORMAT(17L),
    SHARE_FILE(16L),
    BACKEND_METHOD_WAS_NOT_FOUND(15L),
    GET_TRANSLATE_CARD_HISTORY(14L),
    DELETE_TRANSLATE_CARD_EXCEPTION(13L),
    DELAY_DURATION_IS_TOO_BIG(12L),
    VALIDATE_TRANSLATE_CARD_EXCEPTION(11L),
    VALIDATE_TRANSLATE_CARD_TRANSLATION_IS_EMPTY(10L),
    GET_TRANSLATE_CARD_BY_ID(9L),
    GET_NEXT_CARD_TO_REPEAT(8L),
    UPDATE_TRANSLATE_CARD_EXCEPTION(7L),
    UPDATE_CARD_DELAY_IS_EMPTY(6L),
    UPDATE_TRANSLATE_CARD_TRANSLATION_IS_EMPTY(5L),
    UPDATE_TRANSLATE_CARD_TEXT_TO_TRANSLATE_IS_EMPTY(4L),
    SAVE_NEW_TRANSLATE_CARD_EXCEPTION(3L),
    SAVE_NEW_TRANSLATE_CARD_TRANSLATION_IS_EMPTY(2L),
    SAVE_NEW_TRANSLATE_CARD_TEXT_TO_TRANSLATE_IS_EMPTY(1L),
    ERROR_IN_TEST(-1L)

//    init {
//        println("values().size = ${values().size}")
//        if (values().asSequence().map { it.code }.toSet().size != values().size) {
//            throw MemoryRefreshException("values().asSequence().map { it.code }.toSet().size != values().size")
//        }
//    }
}