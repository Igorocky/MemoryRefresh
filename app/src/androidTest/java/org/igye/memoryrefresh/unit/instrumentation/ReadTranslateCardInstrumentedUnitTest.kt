package org.igye.memoryrefresh.unit.instrumentation

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.igye.memoryrefresh.common.Utils
import org.igye.memoryrefresh.common.Utils.MILLIS_IN_HOUR
import org.igye.memoryrefresh.common.Utils.MILLIS_IN_MINUTE
import org.igye.memoryrefresh.common.Utils.MILLIS_IN_SECOND
import org.igye.memoryrefresh.dto.common.BeRespose
import org.igye.memoryrefresh.dto.domain.*
import org.igye.memoryrefresh.manager.DataManager.*
import org.igye.memoryrefresh.testutils.InstrumentedTestBase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.temporal.ChronoUnit

@RunWith(AndroidJUnit4::class)
class ReadTranslateCardInstrumentedUnitTest: InstrumentedTestBase() {

    @Test
    fun readTranslateCardById_returns_all_tags_of_the_card() {
        //given
        val tagId1 = dm.createTag(CreateTagArgs("t1")).data!!
        val tagId2 = dm.createTag(CreateTagArgs("t2")).data!!
        val tagId3 = dm.createTag(CreateTagArgs("t3")).data!!
        val cardId = dm.createTranslateCard(CreateTranslateCardArgs(
            textToTranslate = "a", translation = "b", tagIds = setOf(tagId1, tagId3)
        )).data!!

        //when
        val actualCard = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = cardId)).data!!

        //then
        assertEquals(2, actualCard.tagIds.size)
        assertTrue(actualCard.tagIds.contains(tagId1))
        assertTrue(actualCard.tagIds.contains(tagId3))
    }

    @Test
    fun TODO_readTranslateCardById_returns_correct_values_for_each_parameter() {
        //consider both cases - paused=true and paused=false
        TODO()
    }

    @Test
    fun TODO_readTranslateCardById_returns_empty_collection_of_tag_ids_if_card_doesnt_have_tags() {
        TODO()
    }

    @Test
    fun selectTopOverdueCards_returns_correct_results_when_only_one_card_is_present_in_the_database() {
        //given
        val expectedCardId = 1L
        val baseTime = 27000
        insert(repo = repo, table = c, rows = listOf(
            listOf(c.id to expectedCardId, c.type to TR_TP, c.createdAt to 0)
        ))
        insert(repo = repo, table = s, rows = listOf(
            listOf(s.cardId to expectedCardId, s.updatedAt to 0, s.delay to "1m", s.randomFactor to 1.0, s.nextAccessInMillis to 100, s.nextAccessAt to baseTime + 100)
        ))
        insert(repo = repo, table = t, rows = listOf(
            listOf(t.cardId to expectedCardId, t.textToTranslate to "0", t.translation to "0")
        ))

        //when
        testClock.setFixedTime(baseTime + 145)
        val actualTopOverdueCards = dm.selectTopOverdueTranslateCards().data!!

        //then
        val actualOverdue = actualTopOverdueCards.cards
        assertEquals(1, actualOverdue.size)
        assertEquals(expectedCardId, actualOverdue[0].id)
        assertEquals(0.45, actualOverdue[0].overdue, 0.000001)
    }

    @Test
    fun selectTopOverdueCards_doesnt_return_cards_without_overdue() {
        //given
        val expectedCardId = 1L
        val baseTime = 27000
        insert(repo = repo, table = c, rows = listOf(
            listOf(c.id to expectedCardId, c.type to TR_TP, c.createdAt to 0)
        ))
        insert(repo = repo, table = s, rows = listOf(
            listOf(s.cardId to expectedCardId, s.updatedAt to 0, s.delay to "1m", s.randomFactor to 1.0, s.nextAccessInMillis to 100, s.nextAccessAt to baseTime + 100)
        ))
        insert(repo = repo, table = t, rows = listOf(
            listOf(t.cardId to expectedCardId, t.textToTranslate to "0", t.translation to "0")
        ))

        //when
        testClock.setFixedTime(baseTime + 99)
        val actualTopOverdueCards = dm.selectTopOverdueTranslateCards().data!!

        //then
        val actualOverdue = actualTopOverdueCards.cards
        assertEquals(0, actualOverdue.size)
    }

    @Test
    fun selectTopOverdueCards_selects_cards_correctly_when_there_are_many_cards() {
        //given
        val cardIdWithoutOverdue1 = 1L
        val cardIdWithBigOverdue = 2L
        val cardIdWithZeroOverdue = 3L
        val cardIdWithoutOverdue2 = 4L
        val cardIdWithSmallOverdue = 5L
        val cardIdWithoutOverdue3 = 6L
        val cardIdWithLargeOverdue = 7L
        val cardIdWithoutOverdue4 = 8L
        val baseTime = 1_000
        val timeElapsed = 27_000
        fun createCardRecord(cardId: Long) = listOf(c.id to cardId, c.type to TR_TP, c.createdAt to 0)
        insert(repo = repo, table = c, rows = listOf(
            createCardRecord(cardId = cardIdWithoutOverdue1),
            createCardRecord(cardId = cardIdWithBigOverdue),
            createCardRecord(cardId = cardIdWithZeroOverdue),
            createCardRecord(cardId = cardIdWithoutOverdue2),
            createCardRecord(cardId = cardIdWithSmallOverdue),
            createCardRecord(cardId = cardIdWithoutOverdue3),
            createCardRecord(cardId = cardIdWithLargeOverdue),
            createCardRecord(cardId = cardIdWithoutOverdue4),
        ))
        fun createScheduleRecord(cardId: Long, nextAccessIn: Int) =
            listOf(s.cardId to cardId, s.updatedAt to 0, s.delay to "1m", s.randomFactor to 1.0, s.nextAccessInMillis to nextAccessIn, s.nextAccessAt to baseTime + nextAccessIn)
        insert(repo = repo, table = s, rows = listOf(
            createScheduleRecord(cardId = cardIdWithoutOverdue1, nextAccessIn = timeElapsed+1_000),
            createScheduleRecord(cardId = cardIdWithLargeOverdue, nextAccessIn = timeElapsed-26_000),
            createScheduleRecord(cardId = cardIdWithoutOverdue2, nextAccessIn = timeElapsed+10_000),
            createScheduleRecord(cardId = cardIdWithZeroOverdue, nextAccessIn = timeElapsed),
            createScheduleRecord(cardId = cardIdWithBigOverdue, nextAccessIn = timeElapsed-10_000),
            createScheduleRecord(cardId = cardIdWithoutOverdue3, nextAccessIn = timeElapsed+20_000),
            createScheduleRecord(cardId = cardIdWithSmallOverdue, nextAccessIn = timeElapsed-1_000),
            createScheduleRecord(cardId = cardIdWithoutOverdue4, nextAccessIn = timeElapsed+2_000),
        ))
        fun createTranslationRecord(cardId: Long) =
            listOf(t.cardId to cardId, t.textToTranslate to "0", t.translation to "0")
        insert(repo = repo, table = t, rows = listOf(
            createTranslationRecord(cardId = cardIdWithoutOverdue1),
            createTranslationRecord(cardId = cardIdWithLargeOverdue),
            createTranslationRecord(cardId = cardIdWithoutOverdue2),
            createTranslationRecord(cardId = cardIdWithZeroOverdue),
            createTranslationRecord(cardId = cardIdWithBigOverdue),
            createTranslationRecord(cardId = cardIdWithoutOverdue3),
            createTranslationRecord(cardId = cardIdWithSmallOverdue),
            createTranslationRecord(cardId = cardIdWithoutOverdue4),
        ))

        //when
        testClock.setFixedTime(baseTime + timeElapsed)
        val actualTopOverdueCards = dm.selectTopOverdueTranslateCards().data!!

        //then
        val actualOverdue = actualTopOverdueCards.cards
        assertEquals(4, actualOverdue.size)

        val actualIds = actualOverdue.map { it.id }.toSet()
        assertFalse(actualIds.contains(cardIdWithoutOverdue1))
        assertFalse(actualIds.contains(cardIdWithoutOverdue2))
        assertFalse(actualIds.contains(cardIdWithoutOverdue3))
        assertFalse(actualIds.contains(cardIdWithoutOverdue4))

        assertEquals(cardIdWithLargeOverdue, actualOverdue[0].id)
        assertEquals(26.0, actualOverdue[0].overdue, 0.0001)

        assertEquals(cardIdWithBigOverdue, actualOverdue[1].id)
        assertEquals(0.5882, actualOverdue[1].overdue, 0.0001)

        assertEquals(cardIdWithSmallOverdue, actualOverdue[2].id)
        assertEquals(0.0385, actualOverdue[2].overdue, 0.0001)

        assertEquals(cardIdWithZeroOverdue, actualOverdue[3].id)
        assertEquals(0.0, actualOverdue[3].overdue, 0.000001)

    }

    @Test
    fun TODO_selectTopOverdueCards_doesnt_return_paused_cards() {
        TODO()
    }

    @Test
    fun getNextCardToRepeat_returns_correct_card_if_there_is_one_card_only_in_the_database() {
        //given
        val expectedCardId = 1236L
        val baseTime = 1_000
        val timeElapsed = 27_000
        fun createCardRecord(cardId: Long) = listOf(c.id to cardId, c.type to TR_TP, c.createdAt to 0)
        insert(repo = repo, table = c, rows = listOf(
            createCardRecord(cardId = expectedCardId),
        ))
        fun createScheduleRecord(cardId: Long, nextAccessIn: Int) =
            listOf(s.cardId to cardId, s.updatedAt to 0, s.delay to "1m", s.randomFactor to 1.0, s.nextAccessInMillis to nextAccessIn, s.nextAccessAt to baseTime + nextAccessIn)
        insert(repo = repo, table = s, rows = listOf(
            createScheduleRecord(cardId = expectedCardId, nextAccessIn = timeElapsed-1_000),
        ))
        fun createTranslationRecord(cardId: Long) =
            listOf(t.cardId to cardId, t.textToTranslate to "0", t.translation to "0")
        insert(repo = repo, table = t, rows = listOf(
            createTranslationRecord(cardId = expectedCardId),
        ))

        //when
        testClock.setFixedTime(baseTime + timeElapsed)
        val cardToRepeatResp = dm.selectTopOverdueTranslateCards()

        //then
        val nextCards = cardToRepeatResp.data!!.cards
        val nextCard = nextCards[0]
        assertEquals(expectedCardId, nextCard.id)
        assertEquals(1, nextCards.size)
    }

    @Test
    fun getNextCardToRepeat_returns_time_to_wait_str() {
        //given
        val expectedCardId = 1236L
        val baseTime = 1_000
        val timeElapsed = 27_000
        fun createCardRecord(cardId: Long) = listOf(c.id to cardId, c.type to TR_TP, c.createdAt to 0)
        insert(repo = repo, table = c, rows = listOf(
            createCardRecord(cardId = expectedCardId),
        ))
        fun createScheduleRecord(cardId: Long, nextAccessIn: Int) =
            listOf(s.cardId to cardId, s.updatedAt to 0, s.delay to "1m", s.randomFactor to 1.0, s.nextAccessInMillis to nextAccessIn, s.nextAccessAt to baseTime + nextAccessIn)
        insert(repo = repo, table = s, rows = listOf(
            createScheduleRecord(cardId = expectedCardId, nextAccessIn = (timeElapsed + 2*MILLIS_IN_HOUR + 3*MILLIS_IN_MINUTE + 39*MILLIS_IN_SECOND).toInt()),
        ))
        fun createTranslationRecord(cardId: Long) =
            listOf(t.cardId to cardId, t.textToTranslate to "0", t.translation to "0")
        insert(repo = repo, table = t, rows = listOf(
            createTranslationRecord(cardId = expectedCardId),
        ))

        //when
        testClock.setFixedTime(baseTime + timeElapsed)
        val cardToRepeatResp = dm.selectTopOverdueTranslateCards().data!!

        //then
        assertEquals(0, cardToRepeatResp.cards.size)
        assertEquals("2h 3m", cardToRepeatResp.nextCardIn)
    }

    @Test
    fun getNextCardToRepeat_returns_empty_time_to_wait_str_if_there_are_no_cards_at_all() {
        //given
        val baseTime = 1_000
        val timeElapsed = 27_000

        //when
        testClock.setFixedTime(baseTime + timeElapsed)
        val cardToRepeatResp = dm.selectTopOverdueTranslateCards().data!!

        //then
        assertEquals(0, cardToRepeatResp.cards.size)
        assertEquals("", cardToRepeatResp.nextCardIn)
    }

    @Test
    fun getNextCardToRepeat_returns_correct_card_if_there_are_many_cards_in_the_database() {
        //given
        val cardIdWithoutOverdue1 = 1L
        val cardIdWithBigOverdue = 2L
        val cardIdWithZeroOverdue = 3L
        val cardIdWithoutOverdue2 = 4L
        val cardIdWithSmallOverdue = 5L
        val cardIdWithoutOverdue3 = 6L
        val cardIdWithLargeOverdue = 7L
        val cardIdWithoutOverdue4 = 8L
        val baseTime = 1_000
        val timeElapsed = 27_000
        fun createCardRecord(cardId: Long) = listOf(c.id to cardId, c.type to TR_TP, c.createdAt to 0)
        insert(repo = repo, table = c, rows = listOf(
            createCardRecord(cardId = cardIdWithoutOverdue1),
            createCardRecord(cardId = cardIdWithBigOverdue),
            createCardRecord(cardId = cardIdWithZeroOverdue),
            createCardRecord(cardId = cardIdWithoutOverdue2),
            createCardRecord(cardId = cardIdWithSmallOverdue),
            createCardRecord(cardId = cardIdWithoutOverdue3),
            createCardRecord(cardId = cardIdWithLargeOverdue),
            createCardRecord(cardId = cardIdWithoutOverdue4),
        ))
        fun createScheduleRecord(cardId: Long, nextAccessIn: Int) =
            listOf(s.cardId to cardId, s.updatedAt to 0, s.delay to "1m", s.randomFactor to 1.0, s.nextAccessInMillis to nextAccessIn, s.nextAccessAt to baseTime + nextAccessIn)
        insert(repo = repo, table = s, rows = listOf(
            createScheduleRecord(cardId = cardIdWithoutOverdue1, nextAccessIn = timeElapsed+1_000),
            createScheduleRecord(cardId = cardIdWithLargeOverdue, nextAccessIn = timeElapsed-26_000),
            createScheduleRecord(cardId = cardIdWithoutOverdue2, nextAccessIn = timeElapsed+10_000),
            createScheduleRecord(cardId = cardIdWithZeroOverdue, nextAccessIn = timeElapsed),
            createScheduleRecord(cardId = cardIdWithBigOverdue, nextAccessIn = timeElapsed-10_000),
            createScheduleRecord(cardId = cardIdWithoutOverdue3, nextAccessIn = timeElapsed+20_000),
            createScheduleRecord(cardId = cardIdWithSmallOverdue, nextAccessIn = timeElapsed-1_000),
            createScheduleRecord(cardId = cardIdWithoutOverdue4, nextAccessIn = timeElapsed+2_000),
        ))
        fun createTranslationRecord(cardId: Long) =
            listOf(t.cardId to cardId, t.textToTranslate to "0", t.translation to "0")
        insert(repo = repo, table = t, rows = listOf(
            createTranslationRecord(cardId = cardIdWithoutOverdue1),
            createTranslationRecord(cardId = cardIdWithLargeOverdue),
            createTranslationRecord(cardId = cardIdWithoutOverdue2),
            createTranslationRecord(cardId = cardIdWithZeroOverdue),
            createTranslationRecord(cardId = cardIdWithBigOverdue),
            createTranslationRecord(cardId = cardIdWithoutOverdue3),
            createTranslationRecord(cardId = cardIdWithSmallOverdue),
            createTranslationRecord(cardId = cardIdWithoutOverdue4),
        ))

        //when
        testClock.setFixedTime(baseTime + timeElapsed)
        val cardToRepeatResp = dm.selectTopOverdueTranslateCards().data!!

        //then
        val nextCard = cardToRepeatResp.cards[0]
        assertEquals(cardIdWithLargeOverdue, nextCard.id)
        assertEquals(4, cardToRepeatResp.cards.size)
    }

    @Test
    fun TODO_getNextCardToRepeat_doesnt_return_paused_cards() {
        TODO()
    }

    @Test
    fun getTranslateCardHistory_returns_history_of_a_translate_card() {
        //given
        val baseTime = 1_000

        testClock.setFixedTime(baseTime)
        val cardId = dm.createTranslateCard(CreateTranslateCardArgs(textToTranslate = "A", translation = "a")).data!!

        val validationTime1 = testClock.plus(3, ChronoUnit.MINUTES)
        assertTrue(dm.validateTranslateCard(ValidateTranslateCardArgs(cardId = cardId, userProvidedTranslation = "a")).data!!.isCorrect)

        val validationTime2 = testClock.plus(3, ChronoUnit.MINUTES)
        assertFalse(dm.validateTranslateCard(ValidateTranslateCardArgs(cardId = cardId, userProvidedTranslation = "1")).data!!.isCorrect)

        val validationTime3 = testClock.plus(3, ChronoUnit.MINUTES)
        assertTrue(dm.validateTranslateCard(ValidateTranslateCardArgs(cardId = cardId, userProvidedTranslation = "a")).data!!.isCorrect)

        //when
        val actualHistory = dm.readTranslateCardHistory(ReadTranslateCardHistoryArgs(cardId = cardId)).data!!.historyRecords

        //then
        assertEquals(3, actualHistory.size)

        assertEquals(cardId, actualHistory[0].cardId)
        assertEquals(validationTime3, actualHistory[0].timestamp)
        assertEquals("a", actualHistory[0].translation)
        assertEquals(true, actualHistory[0].isCorrect)

        assertEquals(cardId, actualHistory[1].cardId)
        assertEquals(validationTime2, actualHistory[1].timestamp)
        assertEquals("1", actualHistory[1].translation)
        assertEquals(false, actualHistory[1].isCorrect)

        assertEquals(cardId, actualHistory[2].cardId)
        assertEquals(validationTime1, actualHistory[2].timestamp)
        assertEquals("a", actualHistory[2].translation)
        assertEquals(true, actualHistory[2].isCorrect)
    }

    @Test
    fun readTranslateCardsByFilter_returns_all_cards_if_no_filters_were_specified() {
        //given
        val tagId1 = createTag(tagId = 1, name = "t1")
        val tagId2 = createTag(tagId = 2, name = "t2")
        val tagId3 = createTag(tagId = 3, name = "t3")
        val card1 = createCard(cardId = 1L, tagIds = listOf(tagId1, tagId2))
        val card2 = createCard(cardId = 2L, tagIds = listOf())
        val card3 = createCard(cardId = 3L, tagIds = listOf(tagId2, tagId3))

        //when
        val foundCards = dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs())

        //then
        assertSearchResult(listOf(card1, card2, card3), foundCards)
    }

    @Test
    fun readTranslateCardsByFilter_filters_by_tags_to_include() {
        val tagId1 = createTag(tagId = 1, name = "t1")
        val tagId2 = createTag(tagId = 2, name = "t2")
        val tagId3 = createTag(tagId = 3, name = "t3")
        val tagId4 = createTag(tagId = 4, name = "t4")
        val tagId5 = createTag(tagId = 5, name = "t5")
        val tagId6 = createTag(tagId = 6, name = "t6")
        val card1 = createCard(cardId = 1L, tagIds = listOf(tagId1, tagId2))
        val card2 = createCard(cardId = 2L, tagIds = listOf())
        val card3 = createCard(cardId = 3L, tagIds = listOf(tagId2, tagId3), mapper = {it.copy(paused = !it.paused)})
        val card4 = createCard(cardId = 4L, tagIds = listOf(tagId4, tagId5, tagId6))

        //search by one tag
        assertSearchResult(
            listOf(card1, card3),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                tagIdsToInclude = setOf(tagId2)
            ))
        )

        //search by two tags
        assertSearchResult(
            listOf(card3),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                tagIdsToInclude = setOf(tagId3, tagId2)
            ))
        )

        //search by three tags - empty result
        assertSearchResult(
            listOf(),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                tagIdsToInclude = setOf(tagId1, tagId2, tagId3)
            ))
        )

        //search by three tags - non-empty result
        assertSearchResult(
            listOf(card4),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                tagIdsToInclude = setOf(tagId4, tagId5, tagId6)
            ))
        )
    }

    @Test
    fun readTranslateCardsByFilter_filters_by_tags_to_exclude() {
        val tagId1 = createTag(tagId = 1, name = "t1")
        val tagId2 = createTag(tagId = 2, name = "t2")
        val tagId3 = createTag(tagId = 3, name = "t3")
        val tagId4 = createTag(tagId = 4, name = "t4")
        val tagId5 = createTag(tagId = 5, name = "t5")
        val tagId6 = createTag(tagId = 6, name = "t6")
        val card1 = createCard(cardId = 1L, tagIds = listOf(tagId1, tagId2))
        val card2 = createCard(cardId = 2L, tagIds = listOf())
        val card3 = createCard(cardId = 3L, tagIds = listOf(tagId2, tagId3), mapper = {it.copy(paused = !it.paused)})
        val card4 = createCard(cardId = 4L, tagIds = listOf(tagId4, tagId5, tagId6))

        //search by one tag
        assertSearchResult(
            listOf(card1, card2, card4),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                tagIdsToExclude = setOf(tagId3)
            ))
        )

        //search by two tags
        assertSearchResult(
            listOf(card2, card4),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                tagIdsToExclude = setOf(tagId3, tagId2)
            ))
        )

        //search by three tags - non-empty result
        assertSearchResult(
            listOf(card2),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                tagIdsToExclude = setOf(tagId1, tagId2, tagId4)
            ))
        )
    }

    @Test
    fun readTranslateCardsByFilter_filters_by_few_tags_to_include_and_few_tags_to_exclude() {
        val tagId1 = createTag(tagId = 1, name = "t1")
        val tagId2 = createTag(tagId = 2, name = "t2")
        val tagId3 = createTag(tagId = 3, name = "t3")
        val tagId4 = createTag(tagId = 4, name = "t4")
        val tagId5 = createTag(tagId = 5, name = "t5")
        val tagId6 = createTag(tagId = 6, name = "t6")
        val card1 = createCard(cardId = 1L, tagIds = listOf(tagId4, tagId2))
        val card2 = createCard(cardId = 2L, tagIds = listOf(tagId2, tagId3))
        val card3 = createCard(cardId = 3L, tagIds = listOf(tagId2, tagId3, tagId6), mapper = {it.copy(paused = !it.paused)})
        val card4 = createCard(cardId = 4L, tagIds = listOf(tagId5, tagId6))

        assertSearchResult(
            listOf(card2, card3),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                tagIdsToInclude = setOf(tagId2, tagId3),
                tagIdsToExclude = setOf(tagId4, tagId5),
            ))
        )
    }

    @Test
    fun readTranslateCardsByFilter_filters_by_paused() {
        val card1 = createCard(cardId = 1L, mapper = {it.copy(paused = false)})
        val card2 = createCard(cardId = 2L, mapper = {it.copy(paused = true)})
        val card3 = createCard(cardId = 3L, mapper = {it.copy(paused = false)})
        val card4 = createCard(cardId = 4L, mapper = {it.copy(paused = true)})

        assertSearchResult(
            listOf(card1, card3),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                paused = false
            ))
        )

        assertSearchResult(
            listOf(card2, card4),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                paused = true
            ))
        )
    }

    @Test
    fun readTranslateCardsByFilter_filters_by_textToTranslateContains() {
        val card1 = createCard(cardId = 1L, mapper = {it.copy(textToTranslate = "ubcu")})
        val card2 = createCard(cardId = 2L, mapper = {it.copy(textToTranslate = "dddd")})
        val card3 = createCard(cardId = 3L, mapper = {it.copy(textToTranslate = "ffff")})
        val card4 = createCard(cardId = 4L, mapper = {it.copy(textToTranslate = "aBCd")})

        assertSearchResult(
            listOf(card1, card4),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                textToTranslateContains = "Bc"
            ))
        )
    }

    @Test
    fun readTranslateCardsByFilter_filters_by_paused_and_textToTranslateContains() {
        val card1 = createCard(cardId = 1L, mapper = {it.copy(paused = true, textToTranslate = "ubcu")})
        val card2 = createCard(cardId = 2L, mapper = {it.copy(paused = false, textToTranslate = "dddd")})
        val card3 = createCard(cardId = 3L, mapper = {it.copy(paused = false, textToTranslate = "ffff")})
        val card4 = createCard(cardId = 4L, mapper = {it.copy(paused = false, textToTranslate = "aBCd")})

        assertSearchResult(
            listOf(card4),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                paused = false,
                textToTranslateContains = "Bc"
            ))
        )
    }

    @Test
    fun readTranslateCardsByFilter_filters_by_tags_and_paused_and_textToTranslateContains() {
        val tagId1 = createTag(tagId = 1, name = "t1")
        val tagId2 = createTag(tagId = 2, name = "t2")
        val tagId3 = createTag(tagId = 3, name = "t3")
        val tagId4 = createTag(tagId = 4, name = "t4")
        val tagId5 = createTag(tagId = 5, name = "t5")
        val tagId6 = createTag(tagId = 6, name = "t6")
        val card1 = createCard(cardId = 1L, mapper = {it.copy(paused = false, textToTranslate = "ubcu")})
        val card2 = createCard(cardId = 2L, mapper = {it.copy(paused = true, textToTranslate = "dddd")})
        val card3 = createCard(cardId = 3L, mapper = {it.copy(paused = true, textToTranslate = "ffff")})
        val card4 = createCard(cardId = 4L, mapper = {it.copy(paused = true, textToTranslate = "aBCd")}, tagIds = listOf(tagId1))
        val card5 = createCard(cardId = 5L, mapper = {it.copy(paused = true, textToTranslate = "aBCd")}, tagIds = listOf(tagId1,tagId2,tagId3))
        val card6 = createCard(cardId = 6L, mapper = {it.copy(paused = true, textToTranslate = "aBCd")}, tagIds = listOf(tagId1,tagId2))

        assertSearchResult(
            listOf(card6),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                paused = true,
                textToTranslateContains = "Bc",
                tagIdsToInclude = setOf(tagId1,tagId2),
                tagIdsToExclude = setOf(tagId3)
            ))
        )
    }

    @Test
    fun readTranslateCardsByFilter_filters_by_textToTranslateLengthLessThan() {
        val card1 = createCard(cardId = 1L, mapper = {it.copy(textToTranslate = "1")})
        val card2 = createCard(cardId = 2L, mapper = {it.copy(textToTranslate = "12")})
        val card3 = createCard(cardId = 3L, mapper = {it.copy(textToTranslate = "123")})
        val card4 = createCard(cardId = 4L, mapper = {it.copy(textToTranslate = "1234")})
        val card5 = createCard(cardId = 5L, mapper = {it.copy(textToTranslate = "12345")})

        assertSearchResult(
            listOf(card1, card2, card3),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                textToTranslateLengthLessThan = 4
            ))
        )
    }

    @Test
    fun readTranslateCardsByFilter_filters_by_textToTranslateLengthGreaterThan() {
        val card1 = createCard(cardId = 1L, mapper = {it.copy(textToTranslate = "1")})
        val card2 = createCard(cardId = 2L, mapper = {it.copy(textToTranslate = "12")})
        val card3 = createCard(cardId = 3L, mapper = {it.copy(textToTranslate = "123")})
        val card4 = createCard(cardId = 4L, mapper = {it.copy(textToTranslate = "1234")})
        val card5 = createCard(cardId = 5L, mapper = {it.copy(textToTranslate = "12345")})

        assertSearchResult(
            listOf(card5),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                textToTranslateLengthGreaterThan = 4
            ))
        )
    }

    @Test
    fun readTranslateCardsByFilter_filters_by_textToTranslateLengthGreaterThan_and_textToTranslateLengthLessThan() {
        val card1 = createCard(cardId = 1L, mapper = {it.copy(textToTranslate = "1")})
        val card2 = createCard(cardId = 2L, mapper = {it.copy(textToTranslate = "12")})
        val card3 = createCard(cardId = 3L, mapper = {it.copy(textToTranslate = "123")})
        val card4 = createCard(cardId = 4L, mapper = {it.copy(textToTranslate = "1234")})
        val card5 = createCard(cardId = 5L, mapper = {it.copy(textToTranslate = "12345")})

        assertSearchResult(
            listOf(card4),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                textToTranslateLengthGreaterThan = 3,
                textToTranslateLengthLessThan = 5,
            ))
        )
    }

    @Test
    fun readTranslateCardsByFilter_filters_by_translationContains() {
        val card1 = createCard(cardId = 1L, mapper = {it.copy(translation = "ubcu")})
        val card2 = createCard(cardId = 2L, mapper = {it.copy(translation = "dddd")})
        val card3 = createCard(cardId = 3L, mapper = {it.copy(translation = "ffff")})
        val card4 = createCard(cardId = 4L, mapper = {it.copy(translation = "aBCd")})

        assertSearchResult(
            listOf(card1, card4),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                translationContains = "Bc"
            ))
        )
    }

    @Test
    fun readTranslateCardsByFilter_filters_by_translationLengthLessThan() {
        val card1 = createCard(cardId = 1L, mapper = {it.copy(translation = "1")})
        val card2 = createCard(cardId = 2L, mapper = {it.copy(translation = "12")})
        val card3 = createCard(cardId = 3L, mapper = {it.copy(translation = "123")})
        val card4 = createCard(cardId = 4L, mapper = {it.copy(translation = "1234")})
        val card5 = createCard(cardId = 5L, mapper = {it.copy(translation = "12345")})

        assertSearchResult(
            listOf(card1, card2, card3),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                translationLengthLessThan = 4
            ))
        )
    }

    @Test
    fun readTranslateCardsByFilter_filters_by_translationLengthGreaterThan() {
        val card1 = createCard(cardId = 1L, mapper = {it.copy(translation = "1")})
        val card2 = createCard(cardId = 2L, mapper = {it.copy(translation = "12")})
        val card3 = createCard(cardId = 3L, mapper = {it.copy(translation = "123")})
        val card4 = createCard(cardId = 4L, mapper = {it.copy(translation = "1234")})
        val card5 = createCard(cardId = 5L, mapper = {it.copy(translation = "12345")})

        assertSearchResult(
            listOf(card5),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                translationLengthGreaterThan = 4
            ))
        )
    }

    @Test
    fun readTranslateCardsByFilter_filters_by_translationLengthGreaterThan_and_translationLengthLessThan() {
        val card1 = createCard(cardId = 1L, mapper = {it.copy(translation = "1")})
        val card2 = createCard(cardId = 2L, mapper = {it.copy(translation = "12")})
        val card3 = createCard(cardId = 3L, mapper = {it.copy(translation = "123")})
        val card4 = createCard(cardId = 4L, mapper = {it.copy(translation = "1234")})
        val card5 = createCard(cardId = 5L, mapper = {it.copy(translation = "12345")})

        assertSearchResult(
            listOf(card4),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                translationLengthGreaterThan = 3,
                translationLengthLessThan = 5,
            ))
        )
    }

    @Test
    fun readTranslateCardsByFilter_filters_by_createdFrom() {
        val card1 = createCard(cardId = 1L, mapper = {it.copy(createdAt = 1L)})
        val card2 = createCard(cardId = 2L, mapper = {it.copy(createdAt = 2L)})
        val card3 = createCard(cardId = 3L, mapper = {it.copy(createdAt = 3L)})
        val card4 = createCard(cardId = 4L, mapper = {it.copy(createdAt = 4L)})
        val card5 = createCard(cardId = 5L, mapper = {it.copy(createdAt = 5L)})

        assertSearchResult(
            listOf(card3, card4, card5),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                createdFrom = 3
            ))
        )
    }

    @Test
    fun readTranslateCardsByFilter_filters_by_createdTill() {
        val card1 = createCard(cardId = 1L, mapper = {it.copy(createdAt = 1L)})
        val card2 = createCard(cardId = 2L, mapper = {it.copy(createdAt = 2L)})
        val card3 = createCard(cardId = 3L, mapper = {it.copy(createdAt = 3L)})
        val card4 = createCard(cardId = 4L, mapper = {it.copy(createdAt = 4L)})
        val card5 = createCard(cardId = 5L, mapper = {it.copy(createdAt = 5L)})

        assertSearchResult(
            listOf(card1, card2, card3),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                createdTill = 3
            ))
        )
    }

    @Test
    fun readTranslateCardsByFilter_filters_by_createdFrom_and_createdTill() {
        val card1 = createCard(cardId = 1L, mapper = {it.copy(createdAt = 1L)})
        val card2 = createCard(cardId = 2L, mapper = {it.copy(createdAt = 2L)})
        val card3 = createCard(cardId = 3L, mapper = {it.copy(createdAt = 3L)})
        val card4 = createCard(cardId = 4L, mapper = {it.copy(createdAt = 4L)})
        val card5 = createCard(cardId = 5L, mapper = {it.copy(createdAt = 5L)})

        assertSearchResult(
            listOf(card2, card3, card4),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                createdFrom = 2,
                createdTill = 4
            ))
        )
    }

    @Test
    fun readTranslateCardsByFilter_filters_by_overdueGreaterEq() {
        val card1 = createCard(cardId = 1L, mapper = {it.copy(overdue = -0.5)})
        val card2 = createCard(cardId = 2L, mapper = {it.copy(overdue = -0.1)})
        val card3 = createCard(cardId = 3L, mapper = {it.copy(overdue = 0.0)})
        val card4 = createCard(cardId = 4L, mapper = {it.copy(overdue = 0.5)})
        val card5 = createCard(cardId = 5L, mapper = {it.copy(overdue = 1.0)})

        assertSearchResult(
            listOf(card3,card4,card5),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                overdueGreaterEq = 0.0
            ))
        )
    }

    @Test
    fun readTranslateCardsByFilter_sorts_according_to_sortBy() {
        val card1 = createCard(cardId = 1L, mapper = {it.copy(overdue = -0.5, createdAt = 4)})
        val card2 = createCard(cardId = 2L, mapper = {it.copy(overdue = -0.1, createdAt = 1)})
        val card3 = createCard(cardId = 3L, mapper = {it.copy(overdue = 0.0, createdAt = 5)})
        val card4 = createCard(cardId = 4L, mapper = {it.copy(overdue = 0.5, createdAt = 3)})
        val card5 = createCard(cardId = 5L, mapper = {it.copy(overdue = 1.0, createdAt = 2)})

        assertSearchResult(
            listOf(card1,card2,card3,card4,card5),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                sortBy = TranslateCardSortBy.OVERDUE
            )),
            matchOrder = true
        )

        assertSearchResult(
            listOf(card1,card2,card3,card4,card5),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                sortBy = TranslateCardSortBy.OVERDUE,
                sortDir = SortDirection.ASC
            )),
            matchOrder = true
        )

        assertSearchResult(
            listOf(card5,card4,card3,card2,card1),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                sortBy = TranslateCardSortBy.OVERDUE,
                sortDir = SortDirection.DESC
            )),
            matchOrder = true
        )

        assertSearchResult(
            listOf(card2,card5,card4,card1,card3),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                sortBy = TranslateCardSortBy.TIME_CREATED
            )),
            matchOrder = true
        )

        assertSearchResult(
            listOf(card2,card5,card4,card1,card3),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                sortBy = TranslateCardSortBy.TIME_CREATED,
                sortDir = SortDirection.ASC
            )),
            matchOrder = true
        )

        assertSearchResult(
            listOf(card3,card1,card4,card5,card2),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                sortBy = TranslateCardSortBy.TIME_CREATED,
                sortDir = SortDirection.DESC
            )),
            matchOrder = true
        )
    }

    @Test
    fun readTranslateCardsByFilter_limits_number_of_rows_according_to_rowsLimit() {
        val card1 = createCard(cardId = 1L, mapper = {it.copy(overdue = -0.5, createdAt = 4)})
        val card2 = createCard(cardId = 2L, mapper = {it.copy(overdue = -0.1, createdAt = 1)})
        val card3 = createCard(cardId = 3L, mapper = {it.copy(overdue = 0.0, createdAt = 5)})
        val card4 = createCard(cardId = 4L, mapper = {it.copy(overdue = 0.5, createdAt = 3)})
        val card5 = createCard(cardId = 5L, mapper = {it.copy(overdue = 1.0, createdAt = 2)})

        assertSearchResult(
            listOf(card1,card2,card3,card4,card5),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                sortBy = TranslateCardSortBy.OVERDUE
            )),
            matchOrder = true
        )

        assertSearchResult(
            listOf(card1,card2,card3,card4,card5),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                sortBy = TranslateCardSortBy.OVERDUE,
                rowsLimit = 100
            )),
            matchOrder = true
        )

        assertSearchResult(
            listOf(card1,card2,card3,card4,card5),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                sortBy = TranslateCardSortBy.OVERDUE,
                rowsLimit = 5
            )),
            matchOrder = true
        )

        assertSearchResult(
            listOf(card1,card2,card3,card4),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                sortBy = TranslateCardSortBy.OVERDUE,
                rowsLimit = 4
            )),
            matchOrder = true
        )

        assertSearchResult(
            listOf(card1,card2),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                sortBy = TranslateCardSortBy.OVERDUE,
                rowsLimit = 2
            )),
            matchOrder = true
        )

        assertSearchResult(
            listOf(card1),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                sortBy = TranslateCardSortBy.OVERDUE,
                rowsLimit = 1
            )),
            matchOrder = true
        )
    }

    private fun createCard(cardId: Long, tagIds: List<Long> = emptyList(), mapper: (TranslateCard) -> TranslateCard = {it}): TranslateCard {
        val createdAt = 1000 * cardId + 1
        val updatedAt = 10000 * cardId + 1
        val currTime = testClock.currentMillis()
        val nextAccessInMillis = MILLIS_IN_HOUR * cardId + 2
        val modifiedCard = mapper(
            TranslateCard(
                id = cardId,
                createdAt = createdAt,
                paused = false,
                tagIds = tagIds,
                schedule = CardSchedule(
                    cardId = cardId,
                    updatedAt = updatedAt,
                    delay = "delay-" + cardId,
                    nextAccessInMillis = nextAccessInMillis,
                    nextAccessAt = updatedAt + nextAccessInMillis,
                ),
                timeSinceLastCheck = Utils.millisToDurationStr(currTime - updatedAt),
                overdue = 0.0,
                textToTranslate = "textToTranslate-" + cardId,
                translation = "translation-" + cardId,
            )
        )
        val finalCard = modifiedCard.copy(
            schedule = modifiedCard.schedule.copy(
                nextAccessAt = (currTime - modifiedCard.overdue * modifiedCard.schedule.nextAccessInMillis).toLong()
            )
        )
        createTranslateCard(finalCard)
        return finalCard
    }

    private fun assertSearchResult(expected: List<TranslateCard>, actual: BeRespose<ReadTranslateCardsByFilterResp>, matchOrder:Boolean = false) {
        val actualCardsList = actual.data!!.cards
        assertEquals(expected.size, actualCardsList.size)
        var cnt = 0
        if (matchOrder) {
            for (i in expected.indices) {
                assertTranslateCardsEqual(expected[i], actualCardsList[i])
                cnt++
            }
        } else {
            val cardsMap = actualCardsList.map { it.id to it }.toMap()
            for (i in expected.indices) {
                val id = expected[i].id
                val actualCard = cardsMap[id]
                if (actualCard == null) {
                    fail("Missing cardId=$id in actual result.")
                } else {
                    assertTranslateCardsEqual(expected[i], actualCard)
                }
                cnt++
            }
        }
        assertEquals(expected.size, cnt)
    }
}