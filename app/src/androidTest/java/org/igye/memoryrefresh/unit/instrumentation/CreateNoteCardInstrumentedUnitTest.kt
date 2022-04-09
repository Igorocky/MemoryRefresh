package org.igye.memoryrefresh.unit.instrumentation

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.igye.memoryrefresh.manager.DataManager.*
import org.igye.memoryrefresh.testutils.InstrumentedTestBase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreateNoteCardInstrumentedUnitTest: InstrumentedTestBase() {

    @Test
    fun createNoteCard_saves_new_note_card_without_tags() {
        //given
        val expectedText = "\tsh dj dfhjd afg4fg "
        val time1 = testClock.currentMillis()

        //when
        val createNoteCard = dm.createNoteCard(
            CreateNoteCardArgs(text = expectedText)
        )
        val noteCardId = createNoteCard.data!!
        val noteCard = dm.readNoteCardById(ReadNoteCardByIdArgs(cardId = noteCardId)).data!!

        //then
        assertEquals(expectedText.trim(), noteCard.text)
        assertEquals("1s", noteCard.schedule.delay)
        assertEquals(1000, noteCard.schedule.nextAccessInMillis)
        assertEquals(time1+1000, noteCard.schedule.nextAccessAt)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to noteCard.id, c.type to NC_TP, c.createdAt to time1, c.paused to 0)
        ))
        assertTableContent(repo = repo, table = c.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = tg, expectedRows = listOf())
        assertTableContent(repo = repo, table = ctg, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf())
        assertTableContent(repo = repo, table = t.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = n, matchColumn = n.cardId, expectedRows = listOf(
            listOf(n.cardId to noteCard.id, n.text to expectedText.trim())
        ))
        assertTableContent(repo = repo, table = n.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to noteCard.id, s.delay to "1s", s.nextAccessInMillis to 1000L, s.nextAccessAt to time1+1000)
        ))
        assertTableContent(repo = repo, table = s.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = l, expectedRows = listOf())
    }

    @Test
    fun createNoteCard_saves_new_note_card_with_tags() {
        //given
        val expectedText = "afg sdf sdhhh sg df"
        val time1 = testClock.currentMillis()
        val tagId1 = dm.createTag(CreateTagArgs("t1")).data!!
        val tagId2 = dm.createTag(CreateTagArgs("t2")).data!!

        //when
        val noteCardId = dm.createNoteCard(
            CreateNoteCardArgs(
                text = expectedText,
                tagIds = setOf(tagId1, tagId2)
            )
        ).data!!
        val noteCard = dm.readNoteCardById(ReadNoteCardByIdArgs(cardId = noteCardId)).data!!

        //then
        assertEquals(expectedText, noteCard.text)
        assertEquals("1s", noteCard.schedule.origDelay)
        assertEquals("1s", noteCard.schedule.delay)
        assertEquals(1000, noteCard.schedule.nextAccessInMillis)
        assertEquals(time1+1000, noteCard.schedule.nextAccessAt)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to noteCard.id, c.type to NC_TP, c.createdAt to time1)
        ))
        assertTableContent(repo = repo, table = c.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = tg, expectedRows = listOf(
            listOf(tg.id to tagId1, tg.createdAt to time1, tg.name to "t1"),
            listOf(tg.id to tagId2, tg.createdAt to time1, tg.name to "t2"),
        ))
        assertTableContent(repo = repo, table = ctg, expectedRows = listOf(
            listOf(ctg.cardId to noteCardId, ctg.tagId to tagId1),
            listOf(ctg.cardId to noteCardId, ctg.tagId to tagId2),
        ))

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf())
        assertTableContent(repo = repo, table = t.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = n, matchColumn = n.cardId, expectedRows = listOf(
            listOf(n.cardId to noteCard.id, n.text to expectedText)
        ))
        assertTableContent(repo = repo, table = n.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to noteCard.id, s.origDelay to "1s", s.delay to "1s", s.nextAccessInMillis to 1000L, s.nextAccessAt to time1+1000)
        ))
        assertTableContent(repo = repo, table = s.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = l, expectedRows = listOf())
    }

    @Test
    fun createNoteCard_creates_unpaused_card_if_paused_flag_was_not_specified() {
        //given
        val time1 = testClock.currentMillis()

        //when
        val noteCardId = dm.createNoteCard(
            CreateNoteCardArgs(
                text = "a",
            )
        ).data!!
        val noteCard = dm.readNoteCardById(ReadNoteCardByIdArgs(cardId = noteCardId)).data!!

        //then
        assertFalse(noteCard.paused)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to noteCard.id, c.type to NC_TP, c.createdAt to time1, c.paused to 0)
        ))
    }

    @Test
    fun createNoteCard_creates_unpaused_card_if_paused_flag_was_specified_as_false() {
        //given
        val time1 = testClock.currentMillis()

        //when
        val noteCardId = dm.createNoteCard(
            CreateNoteCardArgs(
                text = "a",
                paused = false
            )
        ).data!!
        val noteCard = dm.readNoteCardById(ReadNoteCardByIdArgs(cardId = noteCardId)).data!!

        //then
        assertFalse(noteCard.paused)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to noteCard.id, c.type to NC_TP, c.createdAt to time1, c.paused to 0)
        ))
    }

    @Test
    fun createNoteCard_creates_paused_card_if_paused_flag_was_specified_as_true() {
        //given
        val time1 = testClock.currentMillis()

        //when
        val noteCardId = dm.createNoteCard(
            CreateNoteCardArgs(
                text = "a",
                paused = true
            )
        ).data!!
        val noteCard = dm.readNoteCardById(ReadNoteCardByIdArgs(cardId = noteCardId)).data!!

        //then
        assertTrue(noteCard.paused)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to noteCard.id, c.type to NC_TP, c.createdAt to time1, c.paused to 1)
        ))
    }

}