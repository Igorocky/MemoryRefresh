package org.igye.memoryrefresh

import android.database.Cursor
import android.database.Cursor.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.igye.memoryrefresh.manager.DataManager.*
import org.igye.memoryrefresh.ErrorCode.ERROR_IN_TEST
import org.igye.memoryrefresh.common.Utils.MILLIS_IN_HOUR
import org.igye.memoryrefresh.common.Utils.MILLIS_IN_MINUTE
import org.igye.memoryrefresh.common.Utils.MILLIS_IN_SECOND
import org.igye.memoryrefresh.common.MemoryRefreshException
import org.igye.memoryrefresh.common.Utils
import org.igye.memoryrefresh.database.CardType
import org.igye.memoryrefresh.database.Repository
import org.igye.memoryrefresh.database.tables.*
import org.igye.memoryrefresh.dto.domain.TranslateCard
import org.igye.memoryrefresh.manager.DataManager
import org.igye.memoryrefresh.manager.RepositoryManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.temporal.ChronoUnit
import java.util.*

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
        assertEquals("0m", translateCard.schedule.delay)
        assertEquals(0, translateCard.schedule.nextAccessInMillis)
        assertEquals(time1, translateCard.schedule.nextAccessAt)

        assertTableContent(repo = repo, tableName = c.tableName, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to translateCard.id, c.type to TR_TP, c.createdAt to time1)
        ))
        assertTableContent(repo = repo, tableName = c.ver.tableName, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = t.tableName, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to translateCard.id, t.textToTranslate to expectedTextToTranslate, t.translation to expectedTranslation)
        ))
        assertTableContent(repo = repo, tableName = t.ver.tableName, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = s.tableName, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to translateCard.id, s.delay to "0m", s.nextAccessInMillis to 0L, s.nextAccessAt to time1)
        ))
        assertTableContent(repo = repo, tableName = s.ver.tableName, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = l.tableName, exactMatch = true, expectedRows = listOf())
    }

    @Test
    fun deleteTranslateCard_deletes_translate_card() {
        //given
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val c = repo.cards
        val t = repo.translationCards
        val s = repo.cardsSchedule
        val l = repo.translationCardsLog
        val expectedTextToTranslate = "A"
        val expectedTranslation = "a"
        testClock.setFixedTime(1000)
        val timeCreated = testClock.instant().toEpochMilli()
        val cardId = dm.saveNewTranslateCard(
            SaveNewTranslateCardArgs(textToTranslate = expectedTextToTranslate, translation = expectedTranslation)
        ).data!!.id

        //when
        val timeDeleted = testClock.plus(1000)
        val deleteTranslateCardResp = dm.deleteTranslateCard(DeleteTranslateCardArgs(cardId = cardId))

        //then
        assertTrue(deleteTranslateCardResp.data!!)

        assertTableContent(repo = repo, tableName = c.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf())
        assertTableContent(repo = repo, tableName = c.ver.tableName, exactMatch = true, expectedRows = listOf(
            listOf(c.ver.timestamp to timeDeleted, c.id to cardId, c.type to TR_TP, c.createdAt to timeCreated)
        ))

        assertTableContent(repo = repo, tableName = t.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf())
        assertTableContent(repo = repo, tableName = t.ver.tableName, exactMatch = true, expectedRows = listOf(
            listOf(t.ver.timestamp to timeDeleted, t.cardId to cardId, t.textToTranslate to expectedTextToTranslate, t.translation to expectedTranslation)
        ))

        assertTableContent(repo = repo, tableName = s.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf())
        assertTableContent(repo = repo, tableName = s.ver.tableName, exactMatch = true, expectedRows = listOf(
            listOf(s.ver.timestamp to timeDeleted, s.cardId to cardId, s.delay to "0m", s.nextAccessInMillis to 0L, s.nextAccessAt to timeCreated)
        ))

        assertTableContent(repo = repo, tableName = l.tableName, exactMatch = true, expectedRows = listOf())
    }

    @Test
    fun deleteTranslateCard_ids_of_deleted_cards_are_not_reused() {
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
        testClock.setFixedTime(1000)
        val timeCreated1 = testClock.instant().toEpochMilli()
        val cardId1 = dm.saveNewTranslateCard(
            SaveNewTranslateCardArgs(textToTranslate = expectedTextToTranslate1, translation = expectedTranslation1)
        ).data!!.id
        val timeDeleted1 = testClock.plus(1000)
        dm.deleteTranslateCard(DeleteTranslateCardArgs(cardId = cardId1))

        //when
        val timeCreated2 = testClock.plus(1000)
        val cardId2 = dm.saveNewTranslateCard(
            SaveNewTranslateCardArgs(textToTranslate = expectedTextToTranslate2, translation = expectedTranslation2)
        ).data!!.id

        //then
        assertNotEquals(cardId1, cardId2)

        assertTableContent(repo = repo, tableName = c.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to cardId2, c.type to TR_TP, c.createdAt to timeCreated2)
        ))
        assertTableContent(repo = repo, tableName = c.ver.tableName, exactMatch = true, expectedRows = listOf(
            listOf(c.ver.timestamp to timeDeleted1, c.id to cardId1, c.type to TR_TP, c.createdAt to timeCreated1)
        ))

        assertTableContent(repo = repo, tableName = t.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to cardId2, t.textToTranslate to expectedTextToTranslate2, t.translation to expectedTranslation2)
        ))
        assertTableContent(repo = repo, tableName = t.ver.tableName, exactMatch = true, expectedRows = listOf(
            listOf(t.ver.timestamp to timeDeleted1, t.cardId to cardId1, t.textToTranslate to expectedTextToTranslate1, t.translation to expectedTranslation1)
        ))

        assertTableContent(repo = repo, tableName = s.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to cardId2, s.updatedAt to timeCreated2, s.delay to "0m", s.nextAccessInMillis to 0L, s.nextAccessAt to timeCreated2)
        ))
        assertTableContent(repo = repo, tableName = s.ver.tableName, exactMatch = true, expectedRows = listOf(
            listOf(s.ver.timestamp to timeDeleted1, s.cardId to cardId1, s.updatedAt to timeCreated1, s.delay to "0m", s.nextAccessInMillis to 0L, s.nextAccessAt to timeCreated1)
        ))

        assertTableContent(repo = repo, tableName = l.tableName, exactMatch = true, expectedRows = listOf())
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
        assertEquals("0m", actualCreatedCard.schedule.delay)
        assertEquals(0, actualCreatedCard.schedule.nextAccessInMillis)
        assertEquals(timeCrt, actualCreatedCard.schedule.nextAccessAt)

        assertTableContent(repo = repo, tableName = c.tableName, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to actualCreatedCard.id, c.type to TR_TP, c.createdAt to timeCrt)
        ))
        assertTableContent(repo = repo, tableName = c.ver.tableName, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = t.tableName, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to actualCreatedCard.id, t.textToTranslate to expectedTextToTranslate1, t.translation to expectedTranslation1)
        ))
        assertTableContent(repo = repo, tableName = t.ver.tableName, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = s.tableName, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to actualCreatedCard.id, s.delay to "0m", s.nextAccessInMillis to 0L, s.nextAccessAt to timeCrt)
        ))
        assertTableContent(repo = repo, tableName = s.ver.tableName, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = l.tableName, exactMatch = true, expectedRows = listOf())

        //when: edit the card but provide same values
        testClock.plus(5000)
        val responseAfterEdit1 = dm.updateTranslateCard(
            UpdateTranslateCardArgs(cardId = actualCreatedCard.id, textToTranslate = "$expectedTextToTranslate1  ", translation = "\t$expectedTranslation1")
        )

        //then: the card stays in the same state - no actual edit was done
        val translateCardAfterEdit1: TranslateCard = responseAfterEdit1.data!!
        assertEquals(expectedTextToTranslate1, translateCardAfterEdit1.textToTranslate)
        assertEquals(expectedTranslation1, translateCardAfterEdit1.translation)
        assertEquals("0m", translateCardAfterEdit1.schedule.delay)
        assertEquals(0, translateCardAfterEdit1.schedule.nextAccessInMillis)
        assertEquals(timeCrt, translateCardAfterEdit1.schedule.nextAccessAt)

        assertTableContent(repo = repo, tableName = c.tableName, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to translateCardAfterEdit1.id, c.type to TR_TP, c.createdAt to timeCrt)
        ))
        assertTableContent(repo = repo, tableName = c.ver.tableName, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = t.tableName, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to translateCardAfterEdit1.id, t.textToTranslate to expectedTextToTranslate1, t.translation to expectedTranslation1)
        ))
        assertTableContent(repo = repo, tableName = t.ver.tableName, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = s.tableName, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to translateCardAfterEdit1.id, s.delay to "0m", s.nextAccessInMillis to 0L, s.nextAccessAt to timeCrt)
        ))
        assertTableContent(repo = repo, tableName = s.ver.tableName, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = l.tableName, exactMatch = true, expectedRows = listOf())

        //when: provide new values when editing the card
        testClock.plus(5000)
        val timeEdt2 = testClock.instant().toEpochMilli()
        val responseAfterEdit2 = dm.updateTranslateCard(
            UpdateTranslateCardArgs(cardId = actualCreatedCard.id, textToTranslate = "  $expectedTextToTranslate2  ", translation = "\t$expectedTranslation2  ")
        )

        //then: the values of card are updated and the previous version of the card is saved to the corresponding VER table
        val translateCardAfterEdit2: TranslateCard = responseAfterEdit2.data!!
        assertEquals(expectedTextToTranslate2, translateCardAfterEdit2.textToTranslate)
        assertEquals(expectedTranslation2, translateCardAfterEdit2.translation)
        assertEquals("0m", translateCardAfterEdit2.schedule.delay)
        assertEquals(0, translateCardAfterEdit2.schedule.nextAccessInMillis)
        assertEquals(timeCrt, translateCardAfterEdit2.schedule.nextAccessAt)

        assertTableContent(repo = repo, tableName = c.tableName, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to translateCardAfterEdit2.id, c.type to TR_TP, c.createdAt to timeCrt)
        ))
        assertTableContent(repo = repo, tableName = c.ver.tableName, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = t.tableName, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to translateCardAfterEdit2.id, t.textToTranslate to expectedTextToTranslate2, t.translation to expectedTranslation2)
        ))
        assertTableContent(repo = repo, tableName = t.ver.tableName, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to translateCardAfterEdit2.id, t.textToTranslate to expectedTextToTranslate1, t.translation to expectedTranslation1,
                t.ver.timestamp to timeEdt2)
        ))

        assertTableContent(repo = repo, tableName = s.tableName, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to translateCardAfterEdit2.id, s.delay to "0m", s.nextAccessInMillis to 0L, s.nextAccessAt to timeCrt)
        ))
        assertTableContent(repo = repo, tableName = s.ver.tableName, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = l.tableName, exactMatch = true, expectedRows = listOf())
    }

    @Test
    fun updateTranslateCard_should_correctly_apply_random_permutation_to_actual_delay() {
        recalculationOfDelayShuoldBeEvenlyDistributedInsideOfPlusMinusRange(
            delayStr = "1h",
            baseDurationMillis = MILLIS_IN_HOUR,
            bucketWidthMillis = 2 * MILLIS_IN_MINUTE
        )
        recalculationOfDelayShuoldBeEvenlyDistributedInsideOfPlusMinusRange(
            delayStr = "15d",
            baseDurationMillis = 15 * Utils.MILLIS_IN_DAY,
            bucketWidthMillis = 12 * MILLIS_IN_HOUR
        )
        recalculationOfDelayShuoldBeEvenlyDistributedInsideOfPlusMinusRange(
            delayStr = "60M",
            baseDurationMillis = 60 * Utils.MILLIS_IN_MONTH,
            bucketWidthMillis = 2 * Utils.MILLIS_IN_MONTH
        )
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
        insert(repo = repo, tableName = c.tableName, rows = listOf(
            listOf(c.id to expectedCardId, c.type to TR_TP, c.createdAt to 0)
        ))
        insert(repo = repo, tableName = s.tableName, rows = listOf(
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
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val c = repo.cards
        val s = repo.cardsSchedule
        val expectedCardId = 1L
        val baseTime = 27000
        insert(repo = repo, tableName = c.tableName, rows = listOf(
            listOf(c.id to expectedCardId, c.type to TR_TP, c.createdAt to 0)
        ))
        insert(repo = repo, tableName = s.tableName, rows = listOf(
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
        insert(repo = repo, tableName = c.tableName, rows = listOf(
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
        insert(repo = repo, tableName = s.tableName, rows = listOf(
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
        insert(repo = repo, tableName = c.tableName, rows = listOf(
            createCardRecord(cardId = expectedCardId),
        ))
        fun createScheduleRecord(cardId: Long, nextAccessIn: Int) =
            listOf(s.cardId to cardId, s.updatedAt to 0, s.delay to "1m", s.randomFactor to 1.0, s.nextAccessInMillis to nextAccessIn, s.nextAccessAt to baseTime + nextAccessIn)
        insert(repo = repo, tableName = s.tableName, rows = listOf(
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
        insert(repo = repo, tableName = c.tableName, rows = listOf(
            createCardRecord(cardId = expectedCardId),
        ))
        fun createScheduleRecord(cardId: Long, nextAccessIn: Int) =
            listOf(s.cardId to cardId, s.updatedAt to 0, s.delay to "1m", s.randomFactor to 1.0, s.nextAccessInMillis to nextAccessIn, s.nextAccessAt to baseTime + nextAccessIn)
        insert(repo = repo, tableName = s.tableName, rows = listOf(
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
        insert(repo = repo, tableName = c.tableName, rows = listOf(
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
        insert(repo = repo, tableName = s.tableName, rows = listOf(
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
        insert(repo = repo, tableName = c.tableName, rows = listOf(
            createCardRecord(cardId = expectedCardId1),
            createCardRecord(cardId = expectedCardId2),
        ))
        fun createScheduleRecord(cardId: Long, nextAccessIn: Int) =
            listOf(s.cardId to cardId, s.updatedAt to 0, s.delay to "1m", s.randomFactor to 1.0, s.nextAccessInMillis to nextAccessIn, s.nextAccessAt to baseTime + nextAccessIn)
        insert(repo = repo, tableName = s.tableName, rows = listOf(
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
        insert(repo = repo, tableName = c.tableName, rows = listOf(
            createCardRecord(cardId = expectedCardId),
        ))
        insert(repo = repo, tableName = t.tableName, rows = listOf(
            listOf(t.cardId to expectedCardId, t.textToTranslate to "A", t.translation to " a\t"),
        ))
        assertTableContent(repo = repo, tableName = l.tableName, exactMatch = true, expectedRows = listOf())

        //when
        testClock.setFixedTime(baseTime)
        val time1 = testClock.instant().toEpochMilli()
        val actualResp = dm.validateTranslateCard(DataManager.ValidateTranslateCardArgs(cardId = expectedCardId, userProvidedTranslation = "\ta   "))

        //then
        val actualValidationResults = actualResp.data!!
        assertTrue(actualValidationResults.isCorrect)
        assertEquals("a", actualValidationResults.answer)
        assertTableContent(repo = repo, tableName = l.tableName, exactMatch = true, expectedRows = listOf(
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
        insert(repo = repo, tableName = c.tableName, rows = listOf(
            createCardRecord(cardId = expectedCardId),
        ))
        insert(repo = repo, tableName = t.tableName, rows = listOf(
            listOf(t.cardId to expectedCardId, t.textToTranslate to "A", t.translation to " a\t"),
        ))
        assertTableContent(repo = repo, tableName = l.tableName, exactMatch = true, expectedRows = listOf())

        //when
        testClock.setFixedTime(baseTime)
        val time1 = testClock.instant().toEpochMilli()
        val actualResp = dm.validateTranslateCard(DataManager.ValidateTranslateCardArgs(cardId = expectedCardId, userProvidedTranslation = "b"))

        //then
        val actualValidationResults = actualResp.data!!
        assertFalse(actualValidationResults.isCorrect)
        assertEquals("a", actualValidationResults.answer)
        assertTableContent(repo = repo, tableName = l.tableName, exactMatch = true, expectedRows = listOf(
            listOf(l.timestamp to time1, l.cardId to expectedCardId, l.translation to "b", l.matched to 0L)
        ))
    }

    @Test
    fun getTranslateCardHistory_returns_history_of_a_translate_card() {
        //given
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val l = repo.translationCardsLog
        val baseTime = 1_000

        testClock.setFixedTime(baseTime)
        val cardId = dm.saveNewTranslateCard(SaveNewTranslateCardArgs(textToTranslate = "A", translation = "a")).data!!.id

        val validationTime1 = testClock.plus(3, ChronoUnit.MINUTES)
        assertTrue(dm.validateTranslateCard(ValidateTranslateCardArgs(cardId = cardId, userProvidedTranslation = "a")).data!!.isCorrect)

        val validationTime2 = testClock.plus(3, ChronoUnit.MINUTES)
        assertFalse(dm.validateTranslateCard(ValidateTranslateCardArgs(cardId = cardId, userProvidedTranslation = "1")).data!!.isCorrect)

        val validationTime3 = testClock.plus(3, ChronoUnit.MINUTES)
        assertTrue(dm.validateTranslateCard(ValidateTranslateCardArgs(cardId = cardId, userProvidedTranslation = "a")).data!!.isCorrect)

        val allData = readAllDataFrom(repo, l.tableName)

        //when
        val actualHistory = dm.getTranslateCardHistory(GetTranslateCardHistoryArgs(cardId = cardId)).data!!.historyRecords

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
    fun test_scenario_2() {
        //given
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val c = repo.cards
        val t = repo.translationCards
        val s = repo.cardsSchedule
        val l = repo.translationCardsLog

        assertTableContent(repo = repo, tableName = c.tableName, exactMatch = true, expectedRows = listOf())
        assertTableContent(repo = repo, tableName = c.ver.tableName, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = t.tableName, exactMatch = true, expectedRows = listOf())
        assertTableContent(repo = repo, tableName = t.ver.tableName, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = s.tableName, exactMatch = true, expectedRows = listOf())
        assertTableContent(repo = repo, tableName = s.ver.tableName, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = l.tableName, exactMatch = true, expectedRows = listOf())

        //when: 1. get next card when there are no cards at all
        testClock.setFixedTime(1000L)
        val resp1 = dm.getNextCardToRepeat().data!!

        //then: response contains empty "wait" time
        assertEquals(0, resp1.cardsRemain)
        assertTrue(resp1.nextCardIn.isEmpty())

        //when: 2. create a new card1
        val time2 = testClock.plus(1, ChronoUnit.MINUTES)
        val createCard1Resp = dm.saveNewTranslateCard(
            SaveNewTranslateCardArgs(textToTranslate = "karta1", translation = "card1")
        ).data!!

        //then
        val card1Id = createCard1Resp.id
        assertEquals("karta1", createCard1Resp.textToTranslate)
        assertEquals("card1", createCard1Resp.translation)
        assertEquals("0m", createCard1Resp.schedule.delay)
        assertEquals(0, createCard1Resp.schedule.nextAccessInMillis)
        assertEquals(time2, createCard1Resp.schedule.nextAccessAt)

        assertTableContent(repo = repo, tableName = c.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2)
        ))
        assertTableContent(repo = repo, tableName = c.ver.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = t.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1")
        ))
        assertTableContent(repo = repo, tableName = t.ver.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = s.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time2, s.delay to "0m", s.nextAccessInMillis to 0L, s.nextAccessAt to time2)
        ))
        assertTableContent(repo = repo, tableName = s.ver.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = l.tableName, matchColumn = l.cardId, exactMatch = true, expectedRows = listOf())

        //when: 3. get next card
        testClock.plus(1, ChronoUnit.MINUTES)
        val nextCardResp1 = dm.getNextCardToRepeat().data!!

        //then
        assertEquals(card1Id, nextCardResp1.cardId)
        assertEquals(CardType.TRANSLATION, nextCardResp1.cardType)
        assertEquals(1, nextCardResp1.cardsRemain)
        assertEquals(true, nextCardResp1.isCardsRemainExact)

        //when: 4. get translate card by card1 id
        testClock.plus(1, ChronoUnit.SECONDS)
        val nextCard1Resp1 = dm.getTranslateCardById(GetTranslateCardByIdArgs(cardId = card1Id)).data!!

        //then
        assertEquals(card1Id, nextCard1Resp1.id)
        assertEquals(CardType.TRANSLATION, nextCard1Resp1.type)
        assertEquals("karta1", nextCard1Resp1.textToTranslate)
        assertEquals("0m", nextCard1Resp1.schedule.delay)
        assertEquals(time2, nextCard1Resp1.schedule.updatedAt)

        //when: 5. validate answer for card1 (a user provided correct answer)
        val time5 = testClock.plus(1, ChronoUnit.MINUTES)
        val validateCard1Resp1 = dm.validateTranslateCard(ValidateTranslateCardArgs(cardId = card1Id, userProvidedTranslation = "card1")).data!!

        //then
        assertEquals("card1", validateCard1Resp1.answer)
        assertTrue(validateCard1Resp1.isCorrect)

        assertTableContent(repo = repo, tableName = c.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2)
        ))
        assertTableContent(repo = repo, tableName = c.ver.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = t.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1")
        ))
        assertTableContent(repo = repo, tableName = t.ver.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = s.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time2, s.delay to "0m", s.nextAccessInMillis to 0L, s.nextAccessAt to time2)
        ))
        assertTableContent(repo = repo, tableName = s.ver.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = l.tableName, matchColumn = l.cardId, exactMatch = true, expectedRows = listOf(
            listOf(l.cardId to card1Id, l.timestamp to time5, l.translation to "card1", l.matched to 1)
        ))

        //when 6. set delay for card1
        val time6 = testClock.plus(1, ChronoUnit.MINUTES)
        val setDelayCard1Resp1 = dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = card1Id, recalculateDelay = true, delay = "1d")).data!!

        //then
        assertEquals(card1Id, setDelayCard1Resp1.id)
        assertEquals("1d", setDelayCard1Resp1.schedule.delay)

        assertTableContent(repo = repo, tableName = c.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2)
        ))
        assertTableContent(repo = repo, tableName = c.ver.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = t.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1")
        ))
        assertTableContent(repo = repo, tableName = t.ver.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = s.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d")
        ))
        assertTableContent(repo = repo, tableName = s.ver.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0m")
        ))

        assertTableContent(repo = repo, tableName = l.tableName, matchColumn = l.cardId, exactMatch = true, expectedRows = listOf(
            listOf(l.cardId to card1Id, l.timestamp to time5, l.translation to "card1", l.matched to 1)
        ))

        //when: 7. update translation for card1
        val time7 = testClock.plus(1, ChronoUnit.MINUTES)
        val updTranslationCard1Resp1 = dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = card1Id, translation = "card1+")).data!!

        //then
        assertEquals(card1Id, updTranslationCard1Resp1.id)
        assertEquals("karta1", updTranslationCard1Resp1.textToTranslate)
        assertEquals("card1+", updTranslationCard1Resp1.translation)
        assertEquals("1d", updTranslationCard1Resp1.schedule.delay)

        assertTableContent(repo = repo, tableName = c.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2)
        ))
        assertTableContent(repo = repo, tableName = c.ver.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = t.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+")
        ))
        assertTableContent(repo = repo, tableName = t.ver.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.ver.timestamp to time7, t.textToTranslate to "karta1", t.translation to "card1")
        ))

        assertTableContent(repo = repo, tableName = s.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d")
        ))
        assertTableContent(repo = repo, tableName = s.ver.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0m")
        ))

        assertTableContent(repo = repo, tableName = l.tableName, matchColumn = l.cardId, exactMatch = true, expectedRows = listOf(
            listOf(l.cardId to card1Id, l.timestamp to time5, l.translation to "card1", l.matched to 1)
        ))

        //when: 8. create a new card2
        val time8 = testClock.plus(1, ChronoUnit.MINUTES)
        val createCard2Resp = dm.saveNewTranslateCard(
            SaveNewTranslateCardArgs(textToTranslate = "karta2", translation = "card2")
        ).data!!

        //then
        val card2Id = createCard2Resp.id
        assertEquals("karta2", createCard2Resp.textToTranslate)
        assertEquals("card2", createCard2Resp.translation)
        assertEquals("0m", createCard2Resp.schedule.delay)
        assertEquals(0, createCard2Resp.schedule.nextAccessInMillis)
        assertEquals(time8, createCard2Resp.schedule.nextAccessAt)

        assertTableContent(repo = repo, tableName = c.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2),
            listOf(c.id to card2Id, c.type to TR_TP, c.createdAt to time8),
        ))
        assertTableContent(repo = repo, tableName = c.ver.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = t.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+"),
            listOf(t.cardId to card2Id, t.textToTranslate to "karta2", t.translation to "card2"),
        ))
        assertTableContent(repo = repo, tableName = t.ver.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.ver.timestamp to time7, t.textToTranslate to "karta1", t.translation to "card1"),
        ))

        assertTableContent(repo = repo, tableName = s.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d"),
            listOf(s.cardId to card2Id, s.updatedAt to time8, s.delay to "0m"),
        ))
        assertTableContent(repo = repo, tableName = s.ver.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0m"),
        ))

        assertTableContent(repo = repo, tableName = l.tableName, matchColumn = l.cardId, exactMatch = true, expectedRows = listOf(
            listOf(l.cardId to card1Id, l.timestamp to time5, l.translation to "card1", l.matched to 1),
        ))

        //when: 9. get next card (card2)
        testClock.plus(1, ChronoUnit.MINUTES)
        val nextCardResp2 = dm.getNextCardToRepeat().data!!

        //then
        assertEquals(card2Id, nextCardResp2.cardId)
        assertEquals(CardType.TRANSLATION, nextCardResp2.cardType)
        assertEquals(1, nextCardResp2.cardsRemain)
        assertEquals(true, nextCardResp2.isCardsRemainExact)

        //when: 10. get translate card by card2 id
        testClock.plus(1, ChronoUnit.SECONDS)
        val nextCard2Resp1 = dm.getTranslateCardById(GetTranslateCardByIdArgs(cardId = card2Id)).data!!

        //then
        assertEquals(card2Id, nextCard2Resp1.id)
        assertEquals(CardType.TRANSLATION, nextCard2Resp1.type)
        assertEquals("karta2", nextCard2Resp1.textToTranslate)
        assertEquals("0m", nextCard2Resp1.schedule.delay)
        assertEquals(time8, nextCard2Resp1.schedule.updatedAt)

        //when: 11. validate answer for card2  (a user provided incorrect answer)
        val time11 = testClock.plus(1, ChronoUnit.MINUTES)
        val validateCard2Resp1 = dm.validateTranslateCard(ValidateTranslateCardArgs(cardId = card2Id, userProvidedTranslation = "card2-inc")).data!!

        //then
        assertEquals("card2", validateCard2Resp1.answer)
        assertFalse(validateCard2Resp1.isCorrect)

        assertTableContent(repo = repo, tableName = c.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2),
            listOf(c.id to card2Id, c.type to TR_TP, c.createdAt to time8),
        ))
        assertTableContent(repo = repo, tableName = c.ver.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = t.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+"),
            listOf(t.cardId to card2Id, t.textToTranslate to "karta2", t.translation to "card2"),
        ))
        assertTableContent(repo = repo, tableName = t.ver.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.ver.timestamp to time7, t.textToTranslate to "karta1", t.translation to "card1"),
        ))

        assertTableContent(repo = repo, tableName = s.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d"),
            listOf(s.cardId to card2Id, s.updatedAt to time8, s.delay to "0m"),
        ))
        assertTableContent(repo = repo, tableName = s.ver.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0m"),
        ))

        assertTableContent(repo = repo, tableName = l.tableName, matchColumn = l.cardId, exactMatch = true, expectedRows = listOf(
            listOf(l.cardId to card1Id, l.timestamp to time5, l.translation to "card1", l.matched to 1),
            listOf(l.cardId to card2Id, l.timestamp to time11, l.translation to "card2-inc", l.matched to 0),
        ))

        //when: 12. set delay for card2
        val time12 = testClock.plus(1, ChronoUnit.MINUTES)
        val setDelayCard2Resp1 = dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = card2Id, recalculateDelay = true, delay = "5m")).data!!

        //then
        assertEquals(card2Id, setDelayCard2Resp1.id)
        assertEquals("5m", setDelayCard2Resp1.schedule.delay)

        assertTableContent(repo = repo, tableName = c.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2),
            listOf(c.id to card2Id, c.type to TR_TP, c.createdAt to time8),
        ))
        assertTableContent(repo = repo, tableName = c.ver.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = t.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+"),
            listOf(t.cardId to card2Id, t.textToTranslate to "karta2", t.translation to "card2"),
        ))
        assertTableContent(repo = repo, tableName = t.ver.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.ver.timestamp to time7, t.textToTranslate to "karta1", t.translation to "card1"),
        ))

        assertTableContent(repo = repo, tableName = s.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d"),
            listOf(s.cardId to card2Id, s.updatedAt to time12, s.delay to "5m"),
        ))
        assertTableContent(repo = repo, tableName = s.ver.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0m"),
            listOf(s.ver.timestamp to time12, s.cardId to card2Id, s.updatedAt to time8, s.delay to "0m"),
        ))

        assertTableContent(repo = repo, tableName = l.tableName, matchColumn = l.cardId, exactMatch = true, expectedRows = listOf(
            listOf(l.cardId to card1Id, l.timestamp to time5, l.translation to "card1", l.matched to 1),
            listOf(l.cardId to card2Id, l.timestamp to time11, l.translation to "card2-inc", l.matched to 0),
        ))

        //when: 13. get next card after small amount of time (no cards returned)
        testClock.plus(1, ChronoUnit.MINUTES)
        val nextCardResp3 = dm.getNextCardToRepeat().data!!

        //then
        assertEquals(0, nextCardResp3.cardsRemain)
        assertTrue(setOf("3m","4m","5m",).contains(nextCardResp3.nextCardIn.split(" ")[0]))

        //when: 14. update textToTranslate for card2
        val time14 = testClock.plus(1, ChronoUnit.MINUTES)
        val updTextToTranslateCard2Resp1 = dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = card2Id, textToTranslate = "karta2+")).data!!

        //then
        assertEquals(card2Id, updTextToTranslateCard2Resp1.id)
        assertEquals("karta2+", updTextToTranslateCard2Resp1.textToTranslate)
        assertEquals("card2", updTextToTranslateCard2Resp1.translation)
        assertEquals("5m", updTextToTranslateCard2Resp1.schedule.delay)

        assertTableContent(repo = repo, tableName = c.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2),
            listOf(c.id to card2Id, c.type to TR_TP, c.createdAt to time8),
        ))
        assertTableContent(repo = repo, tableName = c.ver.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = t.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+"),
            listOf(t.cardId to card2Id, t.textToTranslate to "karta2+", t.translation to "card2"),
        ))
        assertTableContent(repo = repo, tableName = t.ver.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.ver.timestamp to time7, t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1"),
            listOf(t.ver.timestamp to time14, t.cardId to card2Id, t.textToTranslate to "karta2", t.translation to "card2"),
        ))

        assertTableContent(repo = repo, tableName = s.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d"),
            listOf(s.cardId to card2Id, s.updatedAt to time12, s.delay to "5m"),
        ))
        assertTableContent(repo = repo, tableName = s.ver.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0m"),
            listOf(s.ver.timestamp to time12, s.cardId to card2Id, s.updatedAt to time8, s.delay to "0m"),
        ))

        assertTableContent(repo = repo, tableName = l.tableName, matchColumn = l.cardId, exactMatch = true, expectedRows = listOf(
            listOf(l.cardId to card1Id, l.timestamp to time5, l.translation to "card1", l.matched to 1),
            listOf(l.cardId to card2Id, l.timestamp to time11, l.translation to "card2-inc", l.matched to 0),
        ))

        //when: 15. get next card (card2)
        testClock.plus(5, ChronoUnit.MINUTES)
        val nextCardResp4 = dm.getNextCardToRepeat().data!!

        //then
        assertEquals(card2Id, nextCardResp4.cardId)
        assertEquals(CardType.TRANSLATION, nextCardResp4.cardType)
        assertEquals(1, nextCardResp4.cardsRemain)
        assertEquals(true, nextCardResp4.isCardsRemainExact)

        //when: 16. get translate card by card2 id
        testClock.plus(1, ChronoUnit.SECONDS)
        val nextCard2Resp2 = dm.getTranslateCardById(GetTranslateCardByIdArgs(cardId = card2Id)).data!!

        //then
        assertEquals(card2Id, nextCard2Resp2.id)
        assertEquals(CardType.TRANSLATION, nextCard2Resp2.type)
        assertEquals("karta2+", nextCard2Resp2.textToTranslate)
        assertEquals("5m", nextCard2Resp2.schedule.delay)
        assertEquals(time12, nextCard2Resp2.schedule.updatedAt)

        //when: 17. validate answer for card2 (a user provided correct answer)
        val time17 = testClock.plus(1, ChronoUnit.MINUTES)
        val validateCard2Resp2 = dm.validateTranslateCard(ValidateTranslateCardArgs(cardId = card2Id, userProvidedTranslation = "card2")).data!!

        //then
        assertEquals("card2", validateCard2Resp2.answer)
        assertTrue(validateCard2Resp2.isCorrect)

        assertTableContent(repo = repo, tableName = c.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2),
            listOf(c.id to card2Id, c.type to TR_TP, c.createdAt to time8),
        ))
        assertTableContent(repo = repo, tableName = c.ver.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = t.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+"),
            listOf(t.cardId to card2Id, t.textToTranslate to "karta2+", t.translation to "card2"),
        ))
        assertTableContent(repo = repo, tableName = t.ver.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.ver.timestamp to time7, t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1"),
            listOf(t.ver.timestamp to time14, t.cardId to card2Id, t.textToTranslate to "karta2", t.translation to "card2"),
        ))

        assertTableContent(repo = repo, tableName = s.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d"),
            listOf(s.cardId to card2Id, s.updatedAt to time12, s.delay to "5m"),
        ))
        assertTableContent(repo = repo, tableName = s.ver.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0m"),
            listOf(s.ver.timestamp to time12, s.cardId to card2Id, s.updatedAt to time8, s.delay to "0m"),
        ))

        assertTableContent(repo = repo, tableName = l.tableName, matchColumn = l.cardId, exactMatch = true, expectedRows = listOf(
            listOf(l.cardId to card1Id, l.timestamp to time5, l.translation to "card1", l.matched to 1),
            listOf(l.cardId to card2Id, l.timestamp to time11, l.translation to "card2-inc", l.matched to 0),
            listOf(l.cardId to card2Id, l.timestamp to time17, l.translation to "card2", l.matched to 1),
        ))

        //when: 18. set delay for card2
        val time18 = testClock.plus(1, ChronoUnit.MINUTES)
        val setDelayCard2Resp2 = dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = card2Id, recalculateDelay = true, delay = "5m")).data!!

        //then
        assertEquals(card2Id, setDelayCard2Resp2.id)
        assertEquals("5m", setDelayCard2Resp2.schedule.delay)

        assertTableContent(repo = repo, tableName = c.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2),
            listOf(c.id to card2Id, c.type to TR_TP, c.createdAt to time8),
        ))
        assertTableContent(repo = repo, tableName = c.ver.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = t.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+"),
            listOf(t.cardId to card2Id, t.textToTranslate to "karta2+", t.translation to "card2"),
        ))
        assertTableContent(repo = repo, tableName = t.ver.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.ver.timestamp to time7, t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1"),
            listOf(t.ver.timestamp to time14, t.cardId to card2Id, t.textToTranslate to "karta2", t.translation to "card2"),
        ))

        assertTableContent(repo = repo, tableName = s.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d"),
            listOf(s.cardId to card2Id, s.updatedAt to time18, s.delay to "5m"),
        ))
        assertTableContent(repo = repo, tableName = s.ver.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0m"),
            listOf(s.ver.timestamp to time12, s.cardId to card2Id, s.updatedAt to time8, s.delay to "0m"),
            listOf(s.ver.timestamp to time18, s.cardId to card2Id, s.updatedAt to time12, s.delay to "5m"),
        ))

        assertTableContent(repo = repo, tableName = l.tableName, matchColumn = l.cardId, exactMatch = true, expectedRows = listOf(
            listOf(l.cardId to card1Id, l.timestamp to time5, l.translation to "card1", l.matched to 1),
            listOf(l.cardId to card2Id, l.timestamp to time11, l.translation to "card2-inc", l.matched to 0),
            listOf(l.cardId to card2Id, l.timestamp to time17, l.translation to "card2", l.matched to 1),
        ))

        //when: 19. get next card after small amount of time (no cards returned)
        testClock.plus(1, ChronoUnit.SECONDS)
        val nextCardResp5 = dm.getNextCardToRepeat().data!!

        //then
        assertEquals(0, nextCardResp5.cardsRemain)
        assertTrue(setOf("3m","4m","5m",).contains(nextCardResp5.nextCardIn.split(" ")[0]))

        //when: 20. get next card (card2)
        testClock.plus(10, ChronoUnit.MINUTES)
        val nextCardResp6 = dm.getNextCardToRepeat().data!!

        //then
        assertEquals(card2Id, nextCardResp6.cardId)
        assertEquals(CardType.TRANSLATION, nextCardResp6.cardType)
        assertEquals(1, nextCardResp6.cardsRemain)
        assertEquals(true, nextCardResp6.isCardsRemainExact)

        //when: 21. correct schedule for card1 (provide the same value, no change expected)
        val time21 = testClock.plus(10, ChronoUnit.SECONDS)
        val setDelayCard1Resp2 = dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = card1Id, delay = "1d")).data!!

        //then
        assertEquals(card1Id, setDelayCard1Resp2.id)
        assertEquals("1d", setDelayCard1Resp2.schedule.delay)

        assertTableContent(repo = repo, tableName = c.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2),
            listOf(c.id to card2Id, c.type to TR_TP, c.createdAt to time8),
        ))
        assertTableContent(repo = repo, tableName = c.ver.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = t.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+"),
            listOf(t.cardId to card2Id, t.textToTranslate to "karta2+", t.translation to "card2"),
        ))
        assertTableContent(repo = repo, tableName = t.ver.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.ver.timestamp to time7, t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1"),
            listOf(t.ver.timestamp to time14, t.cardId to card2Id, t.textToTranslate to "karta2", t.translation to "card2"),
        ))

        assertTableContent(repo = repo, tableName = s.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d"),
            listOf(s.cardId to card2Id, s.updatedAt to time18, s.delay to "5m"),
        ))
        assertTableContent(repo = repo, tableName = s.ver.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0m"),
            listOf(s.ver.timestamp to time12, s.cardId to card2Id, s.updatedAt to time8, s.delay to "0m"),
            listOf(s.ver.timestamp to time18, s.cardId to card2Id, s.updatedAt to time12, s.delay to "5m"),
        ))

        assertTableContent(repo = repo, tableName = l.tableName, matchColumn = l.cardId, exactMatch = true, expectedRows = listOf(
            listOf(l.cardId to card1Id, l.timestamp to time5, l.translation to "card1", l.matched to 1),
            listOf(l.cardId to card2Id, l.timestamp to time11, l.translation to "card2-inc", l.matched to 0),
            listOf(l.cardId to card2Id, l.timestamp to time17, l.translation to "card2", l.matched to 1),
        ))

        //when: 22. get next card (card2)
        testClock.plus(10, ChronoUnit.SECONDS)
        val nextCardResp7 = dm.getNextCardToRepeat().data!!

        //then
        assertEquals(card2Id, nextCardResp7.cardId)
        assertEquals(CardType.TRANSLATION, nextCardResp7.cardType)
        assertEquals(1, nextCardResp7.cardsRemain)
        assertEquals(true, nextCardResp7.isCardsRemainExact)

        //when: 23. correct schedule for card1 (provide new value)
        val time23 = testClock.plus(10, ChronoUnit.SECONDS)
        val setDelayCard1Resp3 = dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = card1Id, delay = "0m")).data!!

        //then
        assertEquals(card1Id, setDelayCard1Resp3.id)
        assertEquals("0m", setDelayCard1Resp3.schedule.delay)

        assertTableContent(repo = repo, tableName = c.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2),
            listOf(c.id to card2Id, c.type to TR_TP, c.createdAt to time8),
        ))
        assertTableContent(repo = repo, tableName = c.ver.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = t.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+"),
            listOf(t.cardId to card2Id, t.textToTranslate to "karta2+", t.translation to "card2"),
        ))
        assertTableContent(repo = repo, tableName = t.ver.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.ver.timestamp to time7, t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1"),
            listOf(t.ver.timestamp to time14, t.cardId to card2Id, t.textToTranslate to "karta2", t.translation to "card2"),
        ))

        assertTableContent(repo = repo, tableName = s.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time23, s.delay to "0m"),
            listOf(s.cardId to card2Id, s.updatedAt to time18, s.delay to "5m"),
        ))
        assertTableContent(repo = repo, tableName = s.ver.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0m"),
            listOf(s.ver.timestamp to time23, s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d"),
            listOf(s.ver.timestamp to time12, s.cardId to card2Id, s.updatedAt to time8, s.delay to "0m"),
            listOf(s.ver.timestamp to time18, s.cardId to card2Id, s.updatedAt to time12, s.delay to "5m"),
        ))

        assertTableContent(repo = repo, tableName = l.tableName, matchColumn = l.cardId, exactMatch = true, expectedRows = listOf(
            listOf(l.cardId to card1Id, l.timestamp to time5, l.translation to "card1", l.matched to 1),
            listOf(l.cardId to card2Id, l.timestamp to time11, l.translation to "card2-inc", l.matched to 0),
            listOf(l.cardId to card2Id, l.timestamp to time17, l.translation to "card2", l.matched to 1),
        ))

        //when: 24. get next card (card1)
        testClock.plus(10, ChronoUnit.SECONDS)
        val nextCardResp8 = dm.getNextCardToRepeat().data!!

        //then
        assertEquals(card1Id, nextCardResp8.cardId)
        assertEquals(CardType.TRANSLATION, nextCardResp8.cardType)
        assertEquals(2, nextCardResp8.cardsRemain)
        assertEquals(true, nextCardResp8.isCardsRemainExact)

        //when: 25. delete card1
        val time25 = testClock.plus(10, ChronoUnit.MINUTES)
        val deleteTranslateCard = dm.deleteTranslateCard(DeleteTranslateCardArgs(cardId = card1Id))
        val deleteCard1Resp = deleteTranslateCard.data!!

        //then
        assertTrue(deleteCard1Resp)

        assertTableContent(repo = repo, tableName = c.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to card2Id, c.type to TR_TP, c.createdAt to time8),
        ))
        assertTableContent(repo = repo, tableName = c.ver.tableName, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.ver.timestamp to time25, c.id to card1Id, c.type to TR_TP, c.createdAt to time2),
        ))

        assertTableContent(repo = repo, tableName = t.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card2Id, t.textToTranslate to "karta2+", t.translation to "card2"),
        ))
        assertTableContent(repo = repo, tableName = t.ver.tableName, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.ver.timestamp to time7, t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1"),
            listOf(t.ver.timestamp to time25, t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+"),
            listOf(t.ver.timestamp to time14, t.cardId to card2Id, t.textToTranslate to "karta2", t.translation to "card2"),
        ))

        assertTableContent(repo = repo, tableName = s.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to card2Id, s.updatedAt to time18, s.delay to "5m"),
        ))
        assertTableContent(repo = repo, tableName = s.ver.tableName, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0m"),
            listOf(s.ver.timestamp to time23, s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d"),
            listOf(s.ver.timestamp to time25, s.cardId to card1Id, s.updatedAt to time23, s.delay to "0m"),
            listOf(s.ver.timestamp to time12, s.cardId to card2Id, s.updatedAt to time8, s.delay to "0m"),
            listOf(s.ver.timestamp to time18, s.cardId to card2Id, s.updatedAt to time12, s.delay to "5m"),
        ))

        assertTableContent(repo = repo, tableName = l.tableName, matchColumn = l.cardId, exactMatch = true, expectedRows = listOf(
            listOf(l.cardId to card1Id, l.timestamp to time5, l.translation to "card1", l.matched to 1),
            listOf(l.cardId to card2Id, l.timestamp to time11, l.translation to "card2-inc", l.matched to 0),
            listOf(l.cardId to card2Id, l.timestamp to time17, l.translation to "card2", l.matched to 1),
        ))

        //when: 26. request history for card2
        val card2History = dm.getTranslateCardHistory(GetTranslateCardHistoryArgs(cardId = card2Id)).data!!.historyRecords

        //then
        assertEquals(2, card2History.size)

        assertEquals(card2Id, card2History[0].cardId)
        assertEquals(time17, card2History[0].timestamp)
        assertEquals("card2", card2History[0].translation)
        assertEquals(true, card2History[0].isCorrect)

        assertEquals(card2Id, card2History[1].cardId)
        assertEquals(time11, card2History[1].timestamp)
        assertEquals("card2-inc", card2History[1].translation)
        assertEquals(false, card2History[1].isCorrect)
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
                is Double -> insertStmt.bindDouble(++idx, it)
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
            if (!equals(expectedColumn.second, actual[columnName])) {
                return false
            }
        }
        return true
    }

    private fun equals(expected: Any?, actual: Any?): Boolean {
        if (expected is Integer && actual is Long) {
            return expected.toLong() == actual
        } else {
            return Objects.equals(expected, actual)
        }
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
            FIELD_TYPE_FLOAT -> cursor.getDouble(columnIndex)
            else -> throw MemoryRefreshException(msg = "Unexpected type '$type'", errCode = ERROR_IN_TEST)
        }
    }

    private fun createInmemoryDataManager(): DataManager {
        val cards = CardsTable(clock = testClock)
        val cardsSchedule = CardsScheduleTable(clock = testClock, cards = cards)
        val translationCards = TranslationCardsTable(clock = testClock, cards = cards)
        val translationCardsLog = TranslationCardsLogTable(clock = testClock)
        val tags = TagsTable(clock = testClock)
        val cardToTag = CardToTagTable(clock = testClock, cards = cards, tags = tags)

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
                        tags = tags,
                        cardToTag = cardToTag,
                    )
                }
            )
        )
    }

    private fun recalculationOfDelayShuoldBeEvenlyDistributedInsideOfPlusMinusRange(
        delayStr: String, baseDurationMillis: Long, bucketWidthMillis: Long
    ) {
        //given
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val c = repo.cards
        val s = repo.cardsSchedule
        val t = repo.translationCards
        val cardId = 12L
        insert(repo = repo, tableName = c.tableName, rows = listOf(
            listOf(c.id to cardId, c.type to TR_TP, c.createdAt to 0)
        ))
        insert(repo = repo, tableName = s.tableName, rows = listOf(
            listOf(s.cardId to cardId, s.updatedAt to 0, s.delay to "0m", s.randomFactor to 1.0, s.nextAccessInMillis to 0, s.nextAccessAt to 0)
        ))
        insert(repo = repo, tableName = t.tableName, rows = listOf(
            listOf(t.cardId to cardId, t.textToTranslate to "A", t.translation to "B")
        ))

        val proc = 0.15
        val left: Long = (baseDurationMillis * (1.0 - proc)).toLong()
        val right: Long = (baseDurationMillis * (1.0 + proc)).toLong()
        val range = right - left
        val expectedNumOfBuckets: Int = Math.round(range * 1.0 / bucketWidthMillis).toInt()
        val counts = HashMap<Int, Int>()
        val expectedAvg = 500
        val numOfCalcs = expectedNumOfBuckets * expectedAvg

        //when
        for (i in 0 until numOfCalcs) {
            val beRespose = dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = cardId, delay = delayStr, recalculateDelay = true))
            val schedule = beRespose.data!!.schedule
            val actualDelay = schedule.nextAccessInMillis
            assertEquals(testClock.instant().toEpochMilli() + actualDelay, schedule.nextAccessAt)
            val diff = actualDelay - left
            var bucketNum: Int = (diff / bucketWidthMillis).toInt()
            if (bucketNum == expectedNumOfBuckets) {
                bucketNum = expectedNumOfBuckets - 1
            }
            inc(counts, bucketNum)
        }

        //then
        if (expectedNumOfBuckets != counts.size) {
            printCounts(counts)
        }
        assertEquals(expectedNumOfBuckets, counts.size)
        for ((bucketIdx, cnt) in counts) {
            val deltaPct: Double = Math.abs((expectedAvg - cnt) / (expectedAvg * 1.0))
            if (deltaPct > 0.2) {
                printCounts(counts)
                fail(
                    "bucketIdx = " + bucketIdx + ", expectedAvg = " + expectedAvg
                            + ", actualCount = " + cnt + ", deltaPct = " + deltaPct
                )
            }
        }

        val allSchedules = readAllDataFrom(repo, s.ver.tableName).filter { it[s.delay] != "0m" }
        assertEquals(numOfCalcs-1, allSchedules.size)
        assertEquals(
            numOfCalcs-1,
            allSchedules.filter {
                assertEquals(baseDurationMillis*(it[s.randomFactor] as Double), (it[s.nextAccessInMillis] as Long).toDouble(), 1.0)
                assertEquals((it[s.updatedAt] as Long) + (it[s.nextAccessInMillis] as Long), it[s.nextAccessAt])
                true
            }.size
        )
    }

    private fun inc(counts: MutableMap<Int, Int>, key: Int) {
        var cnt = counts[key]
        if (cnt == null) {
            cnt = 0
        }
        counts[key] = cnt + 1
    }

    private fun printCounts(counts: Map<Int, Int>) {
        counts.keys.stream().sorted().forEach { key: Int -> println(key.toString() + " -> " + counts[key]) }
    }
}