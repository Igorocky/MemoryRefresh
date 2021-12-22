package org.igye.memoryrefresh

import android.database.Cursor
import android.database.Cursor.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.igye.memoryrefresh.DataManager.EditTranslateCardArgs
import org.igye.memoryrefresh.DataManager.SaveNewTranslateCardArgs
import org.igye.memoryrefresh.database.CardType
import org.igye.memoryrefresh.database.Repository
import org.igye.memoryrefresh.database.tables.CardsScheduleTable
import org.igye.memoryrefresh.database.tables.CardsTable
import org.igye.memoryrefresh.database.tables.TranslationCardsLogTable
import org.igye.memoryrefresh.database.tables.TranslationCardsTable
import org.igye.memoryrefresh.dto.domain.TranslateCard
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import kotlin.collections.HashMap

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class DataManagerInstrumentedTest {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    val testClock = TestClock(1000)
    val TR_TP = CardType.TRANSLATION.intValue

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("org.igye.memoryrefresh.dev", appContext.packageName)
    }

    @Test
    fun saveNewTranslateCard_saves_new_translate_card() {
        //given
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val c = repo.cards
        val t = repo.translationCards
        val s = repo.cardsSchedule
        val l = repo.translationCardsLog
        val expectedTextToTranslate = "A"
        val expectedTranslation = "a"
        val time1 = 1000L
        testClock.setFixedTime(time1)

        //when
        val actualTranslateCardResp = dm.saveNewTranslateCard(
            SaveNewTranslateCardArgs(textToTranslate = " $expectedTextToTranslate\t", translation = "  \t$expectedTranslation    \t  ")
        )

        //then
        val translateCard: TranslateCard = actualTranslateCardResp.data!!
        assertEquals(expectedTextToTranslate, translateCard.textToTranslate)
        assertEquals(expectedTranslation, translateCard.translation)
        assertEquals(time1, translateCard.schedule.lastAccessedAt)
        assertEquals(0, translateCard.schedule.nextAccessInSec)
        assertEquals(time1, translateCard.schedule.nextAccessAt)

        assertTableContent(repo = repo, tableName = c.name, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to translateCard.id, c.type to TR_TP, c.createdAt to time1)
        ))
        assertTableContent(repo = repo, tableName = c.ver.name, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = t.name, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to translateCard.id, t.textToTranslate to expectedTextToTranslate, t.translation to expectedTranslation)
        ))
        assertTableContent(repo = repo, tableName = t.ver.name, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = s.name, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to translateCard.id, s.lastAccessedAt to time1, s.nextAccessInMillis to 0L, s.nextAccessAt to time1)
        ))
        assertTableContent(repo = repo, tableName = s.ver.name, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = l.name, exactMatch = true, expectedRows = listOf())
    }

    @Test
    fun test_scenario_1_create_card_and_edit_it_twice() {
        //given
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val c = repo.cards
        val t = repo.translationCards
        val s = repo.cardsSchedule
        val l = repo.translationCardsLog
        val expectedTextToTranslate1 = "A"
        val expectedTranslation1 = "a"
        val expectedTextToTranslate2 = "B"
        val expectedTranslation2 = "b"

        //when: create a new translation card
        testClock.setFixedTime(1000L)
        val timeCrt = testClock.instant().toEpochMilli()
        val responseAfterCreate = dm.saveNewTranslateCard(
            SaveNewTranslateCardArgs(textToTranslate = expectedTextToTranslate1, translation = expectedTranslation1)
        )

        //then: a new card is created successfully
        val actualCreatedCard: TranslateCard = responseAfterCreate.data!!
        assertEquals(expectedTextToTranslate1, actualCreatedCard.textToTranslate)
        assertEquals(expectedTranslation1, actualCreatedCard.translation)
        assertEquals(timeCrt, actualCreatedCard.schedule.lastAccessedAt)
        assertEquals(0, actualCreatedCard.schedule.nextAccessInSec)
        assertEquals(timeCrt, actualCreatedCard.schedule.nextAccessAt)

        assertTableContent(repo = repo, tableName = c.name, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to actualCreatedCard.id, c.type to TR_TP, c.createdAt to timeCrt)
        ))
        assertTableContent(repo = repo, tableName = c.ver.name, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = t.name, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to actualCreatedCard.id, t.textToTranslate to expectedTextToTranslate1, t.translation to expectedTranslation1)
        ))
        assertTableContent(repo = repo, tableName = t.ver.name, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = s.name, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to actualCreatedCard.id, s.lastAccessedAt to timeCrt, s.nextAccessInMillis to 0L, s.nextAccessAt to timeCrt)
        ))
        assertTableContent(repo = repo, tableName = s.ver.name, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = l.name, exactMatch = true, expectedRows = listOf())

        //when: edit the card but provide same values
        testClock.plus(5000)
        val responseAfterEdit1 = dm.editTranslateCard(
            EditTranslateCardArgs(cardId = actualCreatedCard.id, textToTranslate = "$expectedTextToTranslate1  ", translation = "\t$expectedTranslation1")
        )

        //then: the card stays in the same state - no actual edit was done
        val translateCardAfterEdit1: TranslateCard = responseAfterEdit1.data!!
        assertEquals(expectedTextToTranslate1, translateCardAfterEdit1.textToTranslate)
        assertEquals(expectedTranslation1, translateCardAfterEdit1.translation)
        assertEquals(timeCrt, translateCardAfterEdit1.schedule.lastAccessedAt)
        assertEquals(0, translateCardAfterEdit1.schedule.nextAccessInSec)
        assertEquals(timeCrt, translateCardAfterEdit1.schedule.nextAccessAt)

        assertTableContent(repo = repo, tableName = c.name, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to translateCardAfterEdit1.id, c.type to TR_TP, c.createdAt to timeCrt)
        ))
        assertTableContent(repo = repo, tableName = c.ver.name, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = t.name, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to translateCardAfterEdit1.id, t.textToTranslate to expectedTextToTranslate1, t.translation to expectedTranslation1)
        ))
        assertTableContent(repo = repo, tableName = t.ver.name, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = s.name, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to translateCardAfterEdit1.id, s.lastAccessedAt to timeCrt, s.nextAccessInMillis to 0L, s.nextAccessAt to timeCrt)
        ))
        assertTableContent(repo = repo, tableName = s.ver.name, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = l.name, exactMatch = true, expectedRows = listOf())

        //when: provide new values when editing the card
        testClock.plus(5000)
        val timeEdt2 = testClock.instant().toEpochMilli()
        val responseAfterEdit2 = dm.editTranslateCard(
            EditTranslateCardArgs(cardId = actualCreatedCard.id, textToTranslate = "  $expectedTextToTranslate2  ", translation = "\t$expectedTranslation2  ")
        )

        //then: the values of card are updated and the previous version of the card is saved to the corresponding VER table
        val translateCardAfterEdit2: TranslateCard = responseAfterEdit2.data!!
        assertEquals(expectedTextToTranslate2, translateCardAfterEdit2.textToTranslate)
        assertEquals(expectedTranslation2, translateCardAfterEdit2.translation)
        assertEquals(timeCrt, translateCardAfterEdit2.schedule.lastAccessedAt)
        assertEquals(0, translateCardAfterEdit2.schedule.nextAccessInSec)
        assertEquals(timeCrt, translateCardAfterEdit2.schedule.nextAccessAt)

        assertTableContent(repo = repo, tableName = c.name, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to translateCardAfterEdit2.id, c.type to TR_TP, c.createdAt to timeCrt)
        ))
        assertTableContent(repo = repo, tableName = c.ver.name, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = t.name, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to translateCardAfterEdit2.id, t.textToTranslate to expectedTextToTranslate2, t.translation to expectedTranslation2)
        ))
        assertTableContent(repo = repo, tableName = t.ver.name, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to translateCardAfterEdit2.id, t.textToTranslate to expectedTextToTranslate1, t.translation to expectedTranslation1,
                t.ver.timestamp to timeEdt2)
        ))

        assertTableContent(repo = repo, tableName = s.name, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to translateCardAfterEdit2.id, s.lastAccessedAt to timeCrt, s.nextAccessInMillis to 0L, s.nextAccessAt to timeCrt)
        ))
        assertTableContent(repo = repo, tableName = s.ver.name, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = l.name, exactMatch = true, expectedRows = listOf())
    }

    @Test
    fun selectTopOverdueCards_returns_correct_results_when_only_one_card_is_present_in_the_database() {
        //given
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val c = repo.cards
        val s = repo.cardsSchedule
        val expectedCardId = 1L
        val expectedCardType = CardType.TRANSLATION
        val baseTime = 27000
        insert(repo = repo, tableName = c.name, rows = listOf(
            listOf(c.id to expectedCardId, c.type to TR_TP, c.createdAt to 0)
        ))
        insert(repo = repo, tableName = s.name, rows = listOf(
            listOf(s.cardId to expectedCardId, s.lastAccessedAt to baseTime, s.nextAccessInMillis to 100, s.nextAccessAt to baseTime + 100)
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
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val c = repo.cards
        val s = repo.cardsSchedule
        val expectedCardId = 1L
        val baseTime = 27000
        insert(repo = repo, tableName = c.name, rows = listOf(
            listOf(c.id to expectedCardId, c.type to TR_TP, c.createdAt to 0)
        ))
        insert(repo = repo, tableName = s.name, rows = listOf(
            listOf(s.cardId to expectedCardId, s.lastAccessedAt to baseTime, s.nextAccessInMillis to 100, s.nextAccessAt to baseTime + 100)
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
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val c = repo.cards
        val s = repo.cardsSchedule
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
        insert(repo = repo, tableName = c.name, rows = listOf(
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
            listOf(s.cardId to cardId, s.lastAccessedAt to baseTime, s.nextAccessInMillis to nextAccessIn, s.nextAccessAt to baseTime + nextAccessIn)
        insert(repo = repo, tableName = s.name, rows = listOf(
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
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val c = repo.cards
        val s = repo.cardsSchedule
        val expectedCardId = 1236L
        val baseTime = 1_000
        val timeElapsed = 27_000
        fun createCardRecord(cardId: Long) = listOf(c.id to cardId, c.type to TR_TP, c.createdAt to 0)
        insert(repo = repo, tableName = c.name, rows = listOf(
            createCardRecord(cardId = expectedCardId),
        ))
        fun createScheduleRecord(cardId: Long, nextAccessIn: Int) =
            listOf(s.cardId to cardId, s.lastAccessedAt to baseTime, s.nextAccessInMillis to nextAccessIn, s.nextAccessAt to baseTime + nextAccessIn)
        insert(repo = repo, tableName = s.name, rows = listOf(
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
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val c = repo.cards
        val s = repo.cardsSchedule
        val expectedCardId = 1236L
        val baseTime = 1_000
        val timeElapsed = 27_000
        fun createCardRecord(cardId: Long) = listOf(c.id to cardId, c.type to TR_TP, c.createdAt to 0)
        insert(repo = repo, tableName = c.name, rows = listOf(
            createCardRecord(cardId = expectedCardId),
        ))
        fun createScheduleRecord(cardId: Long, nextAccessIn: Int) =
            listOf(s.cardId to cardId, s.lastAccessedAt to baseTime, s.nextAccessInMillis to nextAccessIn, s.nextAccessAt to baseTime + nextAccessIn)
        insert(repo = repo, tableName = s.name, rows = listOf(
            createScheduleRecord(cardId = expectedCardId, nextAccessIn = (timeElapsed+2*Utils.MILLIS_IN_HOUR+3*Utils.MILLIS_IN_MINUTE+39*Utils.MILLIS_IN_SECOND).toInt()),
        ))

        //when
        testClock.setFixedTime(baseTime + timeElapsed)
        val cardToRepeatResp = dm.getNextCardToRepeat()

        //then
        val nextCard = cardToRepeatResp.data!!
        assertEquals(0, nextCard.cardsRemain)
        assertEquals("2h 4m", nextCard.nextCardIn)
    }

    @Test
    fun getNextCardToRepeat_returns_empty_time_to_wait_str_if_there_are_no_cards_at_all() {
        //given
        val dm = createInmemoryDataManager()
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
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val c = repo.cards
        val s = repo.cardsSchedule
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
        insert(repo = repo, tableName = c.name, rows = listOf(
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
            listOf(s.cardId to cardId, s.lastAccessedAt to baseTime, s.nextAccessInMillis to nextAccessIn, s.nextAccessAt to baseTime + nextAccessIn)
        insert(repo = repo, tableName = s.name, rows = listOf(
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
    fun getNextCardToRepeat_returns_random_card_if_there_few_cards_with_same_overdue() {
        //given
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val c = repo.cards
        val s = repo.cardsSchedule
        val expectedCardId1 = 1236L
        val expectedCardId2 = 1244L
        val baseTime = 1_000
        val timeElapsed = 27_000
        fun createCardRecord(cardId: Long) = listOf(c.id to cardId, c.type to TR_TP, c.createdAt to 0)
        insert(repo = repo, tableName = c.name, rows = listOf(
            createCardRecord(cardId = expectedCardId1),
            createCardRecord(cardId = expectedCardId2),
        ))
        fun createScheduleRecord(cardId: Long, nextAccessIn: Int) =
            listOf(s.cardId to cardId, s.lastAccessedAt to baseTime, s.nextAccessInMillis to nextAccessIn, s.nextAccessAt to baseTime + nextAccessIn)
        insert(repo = repo, tableName = s.name, rows = listOf(
            createScheduleRecord(cardId = expectedCardId1, nextAccessIn = timeElapsed-1_000),
            createScheduleRecord(cardId = expectedCardId2, nextAccessIn = timeElapsed-1_000),
        ))
        val counts = HashMap<Long,Int>()
        counts[expectedCardId1] = 0
        counts[expectedCardId2] = 0

        //when
        testClock.setFixedTime(baseTime + timeElapsed)
        for (i in 1..1000) {
            val cnt = counts[dm.getNextCardToRepeat().data!!.cardId]!!
            counts[dm.getNextCardToRepeat().data!!.cardId] = cnt + 1
        }

        //then
        assertTrue(counts[expectedCardId1]!! > 400)
        assertTrue(counts[expectedCardId2]!! > 400)
    }

    @Test
    fun validateTranslateCard_returns_expected_response_when_translation_is_correct() {
        //given
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val c = repo.cards
        val t = repo.translationCards
        val l = repo.translationCardsLog
        val expectedCardId = 1236L
        val baseTime = 1_000
        fun createCardRecord(cardId: Long) = listOf(c.id to cardId, c.type to TR_TP, c.createdAt to 0)
        insert(repo = repo, tableName = c.name, rows = listOf(
            createCardRecord(cardId = expectedCardId),
        ))
        insert(repo = repo, tableName = t.name, rows = listOf(
            listOf(t.cardId to expectedCardId, t.textToTranslate to "A", t.translation to " a\t"),
        ))
        assertTableContent(repo = repo, tableName = l.name, exactMatch = true, expectedRows = listOf())

        //when
        testClock.setFixedTime(baseTime)
        val time1 = testClock.instant().toEpochMilli()
        val actualResp = dm.validateTranslateCard(DataManager.ValidateTranslateCardArgs(cardId = expectedCardId, userProvidedTranslation = "\ta   "))

        //then
        val actualValidationResults = actualResp.data!!
        assertTrue(actualValidationResults.isCorrect)
        assertEquals("a", actualValidationResults.answer)
        assertTableContent(repo = repo, tableName = l.name, exactMatch = true, expectedRows = listOf(
            listOf(l.timestamp to time1, l.cardId to expectedCardId, l.translation to "a", l.matched to 1L)
        ))
    }

    @Test
    fun validateTranslateCard_returns_expected_response_when_translation_is_incorrect() {
        //given
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val c = repo.cards
        val t = repo.translationCards
        val l = repo.translationCardsLog
        val expectedCardId = 1236L
        val baseTime = 1_000
        fun createCardRecord(cardId: Long) = listOf(c.id to cardId, c.type to TR_TP, c.createdAt to 0)
        insert(repo = repo, tableName = c.name, rows = listOf(
            createCardRecord(cardId = expectedCardId),
        ))
        insert(repo = repo, tableName = t.name, rows = listOf(
            listOf(t.cardId to expectedCardId, t.textToTranslate to "A", t.translation to " a\t"),
        ))
        assertTableContent(repo = repo, tableName = l.name, exactMatch = true, expectedRows = listOf())

        //when
        testClock.setFixedTime(baseTime)
        val time1 = testClock.instant().toEpochMilli()
        val actualResp = dm.validateTranslateCard(DataManager.ValidateTranslateCardArgs(cardId = expectedCardId, userProvidedTranslation = "b"))

        //then
        val actualValidationResults = actualResp.data!!
        assertFalse(actualValidationResults.isCorrect)
        assertEquals("a", actualValidationResults.answer)
        assertTableContent(repo = repo, tableName = l.name, exactMatch = true, expectedRows = listOf(
            listOf(l.timestamp to time1, l.cardId to expectedCardId, l.translation to "b", l.matched to 0L)
        ))
    }

    private fun insert(repo: Repository, tableName: String, rows: List<List<Pair<String,Any?>>>) {
        val query = """
            insert into $tableName (${rows[0].map { it.first }.joinToString(separator = ", ")}) 
            values ${rows.map {row -> row.map { "?" }.joinToString(prefix = "(", separator = ",", postfix = ")")}.joinToString(separator = ",") }
            """.trimIndent()
        val insertStmt = repo.writableDatabase.compileStatement(query)
        var idx = 0
        rows.flatMap { it }.map { it.second }.forEach {
            when (it) {
                is Long -> insertStmt.bindLong(++idx, it)
                is Int -> insertStmt.bindLong(++idx, it.toLong())
                is String -> insertStmt.bindString(++idx, it)
            }
        }
        insertStmt.executeUpdateDelete()
    }

    private fun assertTableContent(
        repo: Repository, tableName: String, exactMatch: Boolean = false, matchColumn: String = "", expectedRows: List<List<Pair<String,Any?>>>
    ) {
        val allData = readAllDataFrom(repo, tableName)
        if (exactMatch) {
            assertTrue(expectedRows.size == allData.size)
        } else {
            assertTrue(expectedRows.size <= allData.size)
        }
        for (expectedRow in expectedRows) {
            if (count(expected = expectedRow, allData = allData) != 1) {
                fail("Data comparison failed for table $tableName\n" + formatActualAndExpected(allData = allData, expected = expectedRow, matchColumn = matchColumn))
            }
        }
    }

    private fun formatActualAndExpected(allData: List<Map<String, Any?>>, expected: List<Pair<String, Any?>>, matchColumn: String): String {
        val filteredData = if (matchColumn.length > 0) {
            filter(allData = allData, columnName = matchColumn, expectedValue = expected.toMap()[matchColumn])
        } else {
            allData
        }
        val sortOrder: Map<String, Int> = expected.asSequence().mapIndexed{ i, (name,_) -> name to i}.toMap()
        return "Expected:\n" +
                format(expected) + "\n" +
                "Actual:\n" +
                filteredData.asSequence().map { format(sort(it.toList(), sortOrder)) }.joinToString(separator = "\n")

    }

    private fun sort(values: List<Pair<String, Any?>>, sortOrder: Map<String, Int>): List<Pair<String, Any?>> {
        return values.sortedBy { sortOrder[it.first]?:Int.MAX_VALUE }
    }

    private fun format(values: List<Pair<String, Any?>>): String {
        return values.asSequence().map { "${it.first}=${it.second}" }.joinToString(separator = ", ")
    }

    private fun filter(allData: List<Map<String, Any?>>, columnName: String, expectedValue: Any?): List<Map<String, Any?>> {
        return allData.asSequence()
            .filter { it.containsKey(columnName) && Objects.equals(it[columnName], expectedValue) }
            .toList()
    }

    private fun count(expected: List<Pair<String,Any?>>, allData: List<Map<String, Any?>>): Int {
        var result = 0
        for (actualRow in allData) {
            if (compare(expected = expected, actual = actualRow)) {
                result++
            }
        }
        return result
    }

    private fun compare(expected: List<Pair<String,Any?>>, actual: Map<String,Any?>): Boolean {
        for (expectedColumn in expected) {
            val columnName = expectedColumn.first
            if (!actual.containsKey(columnName)) {
                return false
            }
            if (!Objects.equals(expectedColumn.second, actual[columnName])) {
                return false
            }
        }
        return true
    }

    private fun readAllDataFrom(repo: Repository, tableName: String): List<Map<String,Any?>> {
        val result = ArrayList<Map<String,Any?>>()
        repo.readableDatabase.rawQuery("select * from $tableName", null).use { cursor ->
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast) {
                    result.add(readRow(cursor))
                    cursor.moveToNext()
                }
            }
        }
        return result
    }

    private fun readRow(cursor: Cursor): Map<String,Any?> {
        val res = HashMap<String,Any?>()
        for (i in 0 until cursor.columnCount) {
            res[cursor.getColumnName(i)] = readColumnValue(cursor, i)
        }
        return res
    }

    private fun readColumnValue(cursor: Cursor, columnIndex: Int): Any? {
        val type = cursor.getType(columnIndex)
        return when(type) {
            FIELD_TYPE_NULL -> null
            FIELD_TYPE_INTEGER -> cursor.getLong(columnIndex)
            FIELD_TYPE_STRING -> cursor.getString(columnIndex)
            else -> throw MemoryRefreshException("Unexpected type '$type'")
        }
    }

    private fun createInmemoryDataManager(): DataManager {
        val cards = CardsTable(clock = testClock)
        val cardsSchedule = CardsScheduleTable(clock = testClock, cards = cards)
        val translationCards = TranslationCardsTable(clock = testClock, cards = cards)
        val translationCardsLog = TranslationCardsLogTable(clock = testClock, translationCards = translationCards)

        return DataManager(
            clock = testClock,
            repositoryManager = RepositoryManager(
                context = appContext,
                clock = testClock,
                repositoryProvider = {
                    Repository(
                        context = appContext,
                        dbName = null,
                        cards = cards,
                        cardsSchedule = cardsSchedule,
                        translationCards = translationCards,
                        translationCardsLog = translationCardsLog,
                    )
                }
            )
        )
    }
}