package org.igye.memoryrefresh.dto.domain

data class ReadTopOverdueNoteCardsResp(
    val cards: List<NoteCard> = emptyList(),
    val nextCardIn: String = "",
)
