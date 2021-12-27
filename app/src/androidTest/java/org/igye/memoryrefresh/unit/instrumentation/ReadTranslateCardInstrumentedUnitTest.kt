package org.igye.memoryrefresh.unit.instrumentation

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.igye.memoryrefresh.common.Utils.MILLIS_IN_HOUR
import org.igye.memoryrefresh.common.Utils.MILLIS_IN_MINUTE
import org.igye.memoryrefresh.common.Utils.MILLIS_IN_SECOND
import org.igye.memoryrefresh.database.CardType
import org.igye.memoryrefresh.manager.DataManager.*
import org.igye.memoryrefresh.testutils.InstrumentedTestBase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.temporal.ChronoUnit

@RunWith(AndroidJUnit4::class)
class ReadTranslateCardInstrumentedUnitTest: InstrumentedTestBase() {

    @Test
    fun selectTopOverdueCards_returns_correct_results_when_only_one_card_is_present_in_the_database() {
        //given
        val expectedCardId = 1L
        val expectedCardType = CardType.TRANSLATION
        val baseTime = 27000
        insert(repo = repo, table = c, rows = listOf(
            listOf(c.id to expectedCardId, c.type to TR_TP, c.createdAt to 0)
        ))
        insert(repo = repo, table = s, rows = listOf(
            listOf(s.cardId to expectedCardId, s.updatedAt to 0, s.delay to "1m", s.randomFactor to 1.0, s.nextAccessInMillis to 100, s.nextAccessAt to baseTime + 100)
        ))

        //when
        testClock.setFixedTime(baseTime + 145)
        val actualTopOverdueCards = dm.selectTopOverdueCards(30)

        //then
        val actualOverdue = actualTopOverdueCards.get().rows
        assertEquals(1, actualOverdue.size)
        assertEquals(expectedCardId, actualOverdue[0].cardId)
        assertEquals(expectedCardType, actualOverdue[0].cardType)
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

        //when
        testClock.setFixedTime(baseTime + 99)
        val actualTopOverdueCards = dm.selectTopOverdueCards(30)

        //then
        val actualOverdue = actualTopOverdueCards.get().rows
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

        //when
        testClock.setFixedTime(baseTime + timeElapsed)
        val actualTopOverdueCards = dm.selectTopOverdueCards(30)

        //then
        val actualOverdue = actualTopOverdueCards.get().rows
        assertEquals(4, actualOverdue.size)

        val actualIds = actualOverdue.map { it.cardId }.toSet()
        assertFalse(actualIds.contains(cardIdWithoutOverdue1))
        assertFalse(actualIds.contains(cardIdWithoutOverdue2))
        assertFalse(actualIds.contains(cardIdWithoutOverdue3))
        assertFalse(actualIds.contains(cardIdWithoutOverdue4))

        assertEquals(cardIdWithLargeOverdue, actualOverdue[0].cardId)
        assertEquals(26.0, actualOverdue[0].overdue, 0.0001)

        assertEquals(cardIdWithBigOverdue, actualOverdue[1].cardId)
        assertEquals(0.5882, actualOverdue[1].overdue, 0.0001)

        assertEquals(cardIdWithSmallOverdue, actualOverdue[2].cardId)
        assertEquals(0.0385, actualOverdue[2].overdue, 0.0001)

        assertEquals(cardIdWithZeroOverdue, actualOverdue[3].cardId)
        assertEquals(0.0, actualOverdue[3].overdue, 0.000001)

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

        //when
        testClock.setFixedTime(baseTime + timeElapsed)
        val cardToRepeatResp = dm.getNextCardToRepeat()

        //then
        val nextCard = cardToRepeatResp.data!!
        assertEquals(expectedCardId, nextCard.cardId)
        assertEquals(CardType.TRANSLATION, nextCard.cardType)
        assertEquals(1, nextCard.cardsRemain)
        assertTrue(nextCard.isCardsRemainExact)
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

        //when
        testClock.setFixedTime(baseTime + timeElapsed)
        val cardToRepeatResp = dm.getNextCardToRepeat()

        //then
        val nextCard = cardToRepeatResp.data!!
        assertEquals(0, nextCard.cardsRemain)
        assertEquals("2h 3m", nextCard.nextCardIn)
    }

    @Test
    fun getNextCardToRepeat_returns_empty_time_to_wait_str_if_there_are_no_cards_at_all() {
        //given
        val baseTime = 1_000
        val timeElapsed = 27_000

        //when
        testClock.setFixedTime(baseTime + timeElapsed)
        val cardToRepeatResp = dm.getNextCardToRepeat()

        //then
        val nextCard = cardToRepeatResp.data!!
        assertEquals(0, nextCard.cardsRemain)
        assertEquals("", nextCard.nextCardIn)
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

        //when
        testClock.setFixedTime(baseTime + timeElapsed)
        val cardToRepeatResp = dm.getNextCardToRepeat()

        //then
        val nextCard = cardToRepeatResp.data!!
        assertEquals(cardIdWithLargeOverdue, nextCard.cardId)
        assertEquals(CardType.TRANSLATION, nextCard.cardType)
        assertEquals(4, nextCard.cardsRemain)
        assertTrue(nextCard.isCardsRemainExact)
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
}