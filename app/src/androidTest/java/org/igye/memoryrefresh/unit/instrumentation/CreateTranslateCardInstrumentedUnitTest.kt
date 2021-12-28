package org.igye.memoryrefresh.unit.instrumentation

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.igye.memoryrefresh.manager.DataManager.*
import org.igye.memoryrefresh.testutils.InstrumentedTestBase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreateTranslateCardInstrumentedUnitTest: InstrumentedTestBase() {

    @Test
    fun createTranslateCard_saves_new_translate_card_without_tags() {
        //given
        val expectedTextToTranslate = "A"
        val expectedTranslation = "a"
        val time1 = testClock.currentMillis()

        //when
        val translateCardId = dm.createTranslateCard(
            CreateTranslateCardArgs(textToTranslate = " $expectedTextToTranslate\t", translation = "  \t$expectedTranslation    \t  ")
        ).data!!
        val translateCard = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = translateCardId)).data!!

        //then
        assertEquals(expectedTextToTranslate, translateCard.textToTranslate)
        assertEquals(expectedTranslation, translateCard.translation)
        assertEquals("0s", translateCard.schedule.delay)
        assertEquals(0, translateCard.schedule.nextAccessInMillis)
        assertEquals(time1, translateCard.schedule.nextAccessAt)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to translateCard.id, c.type to TR_TP, c.createdAt to time1, c.paused to 0)
        ))
        assertTableContent(repo = repo, table = c.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = tg, expectedRows = listOf())
        assertTableContent(repo = repo, table = ctg, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to translateCard.id, t.textToTranslate to expectedTextToTranslate, t.translation to expectedTranslation)
        ))
        assertTableContent(repo = repo, table = t.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to translateCard.id, s.delay to "0s", s.nextAccessInMillis to 0L, s.nextAccessAt to time1)
        ))
        assertTableContent(repo = repo, table = s.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = l, expectedRows = listOf())
    }

    @Test
    fun createTranslateCard_saves_new_translate_card_with_tags() {
        //given
        val expectedTextToTranslate = "A"
        val expectedTranslation = "a"
        val time1 = testClock.currentMillis()
        val tagId1 = dm.createTag(CreateTagArgs("t1")).data!!
        val tagId2 = dm.createTag(CreateTagArgs("t2")).data!!

        //when
        val translateCardId = dm.createTranslateCard(
            CreateTranslateCardArgs(
                textToTranslate = " $expectedTextToTranslate\t",
                translation = "  \t$expectedTranslation    \t  ",
                tagIds = setOf(tagId1, tagId2)
            )
        ).data!!
        val translateCard = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = translateCardId)).data!!

        //then
        assertEquals(expectedTextToTranslate, translateCard.textToTranslate)
        assertEquals(expectedTranslation, translateCard.translation)
        assertEquals("0s", translateCard.schedule.delay)
        assertEquals(0, translateCard.schedule.nextAccessInMillis)
        assertEquals(time1, translateCard.schedule.nextAccessAt)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to translateCard.id, c.type to TR_TP, c.createdAt to time1)
        ))
        assertTableContent(repo = repo, table = c.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = tg, expectedRows = listOf(
            listOf(tg.id to tagId1, tg.createdAt to time1, tg.name to "t1"),
            listOf(tg.id to tagId2, tg.createdAt to time1, tg.name to "t2"),
        ))
        assertTableContent(repo = repo, table = ctg, expectedRows = listOf(
            listOf(ctg.cardId to translateCardId, ctg.tagId to tagId1),
            listOf(ctg.cardId to translateCardId, ctg.tagId to tagId2),
        ))

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to translateCard.id, t.textToTranslate to expectedTextToTranslate, t.translation to expectedTranslation)
        ))
        assertTableContent(repo = repo, table = t.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to translateCard.id, s.delay to "0s", s.nextAccessInMillis to 0L, s.nextAccessAt to time1)
        ))
        assertTableContent(repo = repo, table = s.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = l, expectedRows = listOf())
    }

    @Test
    fun createTranslateCard_creates_unpaused_card_if_paused_flag_was_not_specified() {
        TODO()
    }

    @Test
    fun createTranslateCard_creates_unpaused_card_if_paused_flag_was_specified_as_false() {
        TODO()
    }

    @Test
    fun createTranslateCard_creates_paused_card_if_paused_flag_was_specified_as_true() {
        TODO()
    }

}