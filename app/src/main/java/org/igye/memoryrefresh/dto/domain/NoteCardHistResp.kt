package org.igye.memoryrefresh.dto.domain

data class NoteCardHistResp(
    val isHistoryFull: Boolean,
    val dataHistory: List<NoteCardHistRecord>,
)
