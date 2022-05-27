package org.igye.memoryrefresh.unit.instrumentation

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.igye.memoryrefresh.database.TranslationCardDirection.FOREIGN_NATIVE
import org.igye.memoryrefresh.database.TranslationCardDirection.NATIVE_FOREIGN
import org.igye.memoryrefresh.manager.DataManager.*
import org.igye.memoryrefresh.testutils.InstrumentedTestBase
import org.junit.Assert
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
            CreateTranslateCardArgs(textToTranslate = " $expectedTextToTranslate\t", translation = "  \t$expectedTranslation    \t  ", direction = NATIVE_FOREIGN)
        ).data!!
        val translateCard = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = translateCardId)).data!!

        //then
        assertEquals(expectedTextToTranslate, translateCard.textToTranslate)
        assertEquals(expectedTranslation, translateCard.translation)
        assertEquals(NATIVE_FOREIGN, translateCard.direction)
        assertEquals("1s", translateCard.schedule.origDelay)
        assertEquals("1s", translateCard.schedule.delay)
        assertEquals(1000, translateCard.schedule.nextAccessInMillis)
        assertEquals(time1+1000, translateCard.schedule.nextAccessAt)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to translateCard.id, c.type to TR_TP, c.createdAt to time1, c.paused to 0)
        ))
        assertTableContent(repo = repo, table = c.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = tg, expectedRows = listOf())
        assertTableContent(repo = repo, table = ctg, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to translateCard.id, t.textToTranslate to expectedTextToTranslate, t.translation to expectedTranslation, t.direction to NATIVE_FOREIGN)
        ))
        assertTableContent(repo = repo, table = t.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to translateCard.id, s.delay to "1s", s.nextAccessInMillis to 1000L, s.nextAccessAt to time1+1000)
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
                tagIds = setOf(tagId1, tagId2),
                direction = FOREIGN_NATIVE
            )
        ).data!!
        val translateCard = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = translateCardId)).data!!

        //then
        assertEquals(expectedTextToTranslate, translateCard.textToTranslate)
        assertEquals(expectedTranslation, translateCard.translation)
        assertEquals(FOREIGN_NATIVE, translateCard.direction)
        assertEquals("1s", translateCard.schedule.origDelay)
        assertEquals("1s", translateCard.schedule.delay)
        assertEquals(1000, translateCard.schedule.nextAccessInMillis)
        assertEquals(time1+1000, translateCard.schedule.nextAccessAt)

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
            listOf(t.cardId to translateCard.id, t.textToTranslate to expectedTextToTranslate, t.translation to expectedTranslation, t.direction to FOREIGN_NATIVE)
        ))
        assertTableContent(repo = repo, table = t.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to translateCard.id, s.delay to "1s", s.nextAccessInMillis to 1000L, s.nextAccessAt to time1+1000)
        ))
        assertTableContent(repo = repo, table = s.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = l, expectedRows = listOf())
    }

    @Test
    fun createTranslateCard_creates_unpaused_card_if_paused_flag_was_not_specified() {
        //given
        val time1 = testClock.currentMillis()

        //when
        val translateCardId = dm.createTranslateCard(
            CreateTranslateCardArgs(
                textToTranslate = "a",
                translation = "b",
                direction = NATIVE_FOREIGN
            )
        ).data!!
        val translateCard = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = translateCardId)).data!!

        //then
        Assert.assertFalse(translateCard.paused)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to translateCard.id, c.type to TR_TP, c.createdAt to time1, c.paused to 0)
        ))
    }

    @Test
    fun createTranslateCard_creates_unpaused_card_if_paused_flag_was_specified_as_false() {
        //given
        val time1 = testClock.currentMillis()

        //when
        val translateCardId = dm.createTranslateCard(
            CreateTranslateCardArgs(
                textToTranslate = "a",
                translation = "b",
                paused = false,
                direction = NATIVE_FOREIGN
            )
        ).data!!
        val translateCard = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = translateCardId)).data!!

        //then
        Assert.assertFalse(translateCard.paused)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to translateCard.id, c.type to TR_TP, c.createdAt to time1, c.paused to 0)
        ))
    }

    @Test
    fun createTranslateCard_creates_paused_card_if_paused_flag_was_specified_as_true() {
        //given
        val time1 = testClock.currentMillis()

        //when
        val translateCardId = dm.createTranslateCard(
            CreateTranslateCardArgs(
                textToTranslate = "a",
                translation = "b",
                paused = true,
                direction = NATIVE_FOREIGN
            )
        ).data!!
        val translateCard = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = translateCardId)).data!!

        //then
        Assert.assertTrue(translateCard.paused)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to translateCard.id, c.type to TR_TP, c.createdAt to time1, c.paused to 1)
        ))
    }

    @Test
    fun createTranslateCard_creates_reversed_card_if_createReversedCard_is_true() {
        //given
        val expectedTextToTranslate = "345thrhrwfgh"
        val expectedTranslation = "sfgjghi842453"
        val time1 = testClock.currentMillis()
        val tagId1 = dm.createTag(CreateTagArgs("t1")).data!!
        val tagId2 = dm.createTag(CreateTagArgs("t2")).data!!

        //when
        val translateCardId1 = dm.createTranslateCard(
            CreateTranslateCardArgs(
                textToTranslate = expectedTextToTranslate,
                translation = expectedTranslation,
                tagIds = setOf(tagId1, tagId2),
                direction = NATIVE_FOREIGN,
                createReversedCard = true
            )
        ).data!!

        //then
        val cards = dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
            textToTranslateContains = expectedTextToTranslate
        )).data!!.cards
        assertEquals(2, cards.size)
        val translateCard1 = cards.find { it.id == translateCardId1 }!!
        val translateCard2 = cards.find { it.id != translateCardId1 }!!
        val translateCardId2 = translateCard2.id

        assertEquals(expectedTextToTranslate, translateCard1.textToTranslate)
        assertEquals(expectedTranslation, translateCard1.translation)
        assertEquals(NATIVE_FOREIGN, translateCard1.direction)
        assertEquals("1s", translateCard1.schedule.origDelay)
        assertEquals("1s", translateCard1.schedule.delay)
        assertEquals(1000, translateCard1.schedule.nextAccessInMillis)
        assertEquals(time1+1000, translateCard1.schedule.nextAccessAt)

        assertEquals(expectedTextToTranslate, translateCard2.textToTranslate)
        assertEquals(expectedTranslation, translateCard2.translation)
        assertEquals(FOREIGN_NATIVE, translateCard2.direction)
        assertEquals("1s", translateCard2.schedule.origDelay)
        assertEquals("1s", translateCard2.schedule.delay)
        assertEquals(1000, translateCard2.schedule.nextAccessInMillis)
        assertEquals(time1+1000, translateCard2.schedule.nextAccessAt)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to translateCard1.id, c.type to TR_TP, c.createdAt to time1),
            listOf(c.id to translateCard2.id, c.type to TR_TP, c.createdAt to time1),
        ))
        assertTableContent(repo = repo, table = c.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = tg, expectedRows = listOf(
            listOf(tg.id to tagId1, tg.createdAt to time1, tg.name to "t1"),
            listOf(tg.id to tagId2, tg.createdAt to time1, tg.name to "t2"),
        ))
        assertTableContent(repo = repo, table = ctg, expectedRows = listOf(
            listOf(ctg.cardId to translateCardId1, ctg.tagId to tagId1),
            listOf(ctg.cardId to translateCardId1, ctg.tagId to tagId2),
            listOf(ctg.cardId to translateCardId2, ctg.tagId to tagId1),
            listOf(ctg.cardId to translateCardId2, ctg.tagId to tagId2),
        ))

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to translateCard1.id, t.textToTranslate to expectedTextToTranslate, t.translation to expectedTranslation, t.direction to NATIVE_FOREIGN),
            listOf(t.cardId to translateCard2.id, t.textToTranslate to expectedTextToTranslate, t.translation to expectedTranslation, t.direction to FOREIGN_NATIVE),
        ))
        assertTableContent(repo = repo, table = t.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to translateCard1.id, s.delay to "1s", s.nextAccessInMillis to 1000L, s.nextAccessAt to time1+1000),
            listOf(s.cardId to translateCard2.id, s.delay to "1s", s.nextAccessInMillis to 1000L, s.nextAccessAt to time1+1000),
        ))
        assertTableContent(repo = repo, table = s.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = l, expectedRows = listOf())
    }

    @Test
    fun createTranslateCard_doesnt_create_reversed_card_if_createReversedCard_is_false() {
        //given
        val expectedTextToTranslate = "345thrhrwfgh"
        val expectedTranslation = "sfgjghi842453"
        val time1 = testClock.currentMillis()
        val tagId1 = dm.createTag(CreateTagArgs("t1")).data!!
        val tagId2 = dm.createTag(CreateTagArgs("t2")).data!!

        //when
        val translateCardId1 = dm.createTranslateCard(
            CreateTranslateCardArgs(
                textToTranslate = expectedTextToTranslate,
                translation = expectedTranslation,
                tagIds = setOf(tagId1, tagId2),
                direction = NATIVE_FOREIGN,
                createReversedCard = false
            )
        ).data!!

        //then
        val cards = dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
            textToTranslateContains = expectedTextToTranslate
        )).data!!.cards
        assertEquals(1, cards.size)
    }

    @Test
    fun createTranslateCard_doesnt_create_reversed_card_if_createReversedCard_is_not_specified() {
        //given
        val expectedTextToTranslate = "345thrhrwfgh"
        val expectedTranslation = "sfgjghi842453"
        val time1 = testClock.currentMillis()
        val tagId1 = dm.createTag(CreateTagArgs("t1")).data!!
        val tagId2 = dm.createTag(CreateTagArgs("t2")).data!!

        //when
        val translateCardId1 = dm.createTranslateCard(
            CreateTranslateCardArgs(
                textToTranslate = expectedTextToTranslate,
                translation = expectedTranslation,
                tagIds = setOf(tagId1, tagId2),
                direction = NATIVE_FOREIGN,
            )
        ).data!!

        //then
        val cards = dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
            textToTranslateContains = expectedTextToTranslate
        )).data!!.cards
        assertEquals(1, cards.size)
    }
}