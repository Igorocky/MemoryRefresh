package org.igye.memoryrefresh.dto

data class TranslateCard(
    val id: Long,
    val textToTranslate:String,
    val translation:String,
    val lastAccessedAt: Long,
    val nextAccessInSec: Long,
    val nextAccessAt: Long,
)
