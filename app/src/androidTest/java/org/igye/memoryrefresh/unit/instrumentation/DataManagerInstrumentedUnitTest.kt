package org.igye.memoryrefresh.unit.instrumentation

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.igye.memoryrefresh.common.InstrumentedTestBase
import org.igye.memoryrefresh.common.Utils.MILLIS_IN_HOUR
import org.igye.memoryrefresh.common.Utils.MILLIS_IN_MINUTE
import org.igye.memoryrefresh.common.Utils.MILLIS_IN_SECOND
import org.igye.memoryrefresh.database.CardType
import org.igye.memoryrefresh.manager.DataManager.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.temporal.ChronoUnit

@RunWith(AndroidJUnit4::class)
class DataManagerInstrumentedUnitTest: InstrumentedTestBase() {

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("org.igye.memoryrefresh.dev", appContext.packageName)
    }

    @Test
    fun saveNewTranslateCard_saves_new_translate_card_without_tags() {
        //given
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val c = repo.cards
        val tg = repo.tags
        val ctg = repo.cardToTag
        val t = repo.translationCards
        val s = repo.cardsSchedule
        val l = repo.translationCardsLog
        val expectedTextToTranslate = "A"
        val expectedTranslation = "a"
        val time1 = 1000L
        testClock.setFixedTime(time1)

        //when
        val translateCardId = dm.saveNewTranslateCard(
            SaveNewTranslateCardArgs(textToTranslate = " $expectedTextToTranslate\t", translation = "  \t$expectedTranslation    \t  ")
        ).data!!
        val translateCard = dm.getTranslateCardById(GetTranslateCardByIdArgs(cardId = translateCardId)).data!!

        //then
        assertEquals(expectedTextToTranslate, translateCard.textToTranslate)
        assertEquals(expectedTranslation, translateCard.translation)
        assertEquals("0m", translateCard.schedule.delay)
        assertEquals(0, translateCard.schedule.nextAccessInMillis)
        assertEquals(time1, translateCard.schedule.nextAccessAt)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to translateCard.id, c.type to TR_TP, c.createdAt to time1)
        ))
        assertTableContent(repo = repo, table = c.ver, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = tg, exactMatch = true, expectedRows = listOf())
        assertTableContent(repo = repo, table = ctg, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to translateCard.id, t.textToTranslate to expectedTextToTranslate, t.translation to expectedTranslation)
        ))
        assertTableContent(repo = repo, table = t.ver, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to translateCard.id, s.delay to "0m", s.nextAccessInMillis to 0L, s.nextAccessAt to time1)
        ))
        assertTableContent(repo = repo, table = s.ver, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = l, exactMatch = true, expectedRows = listOf())
    }

    @Test
    fun saveNewTranslateCard_saves_new_translate_card_with_tags() {
        //given
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val c = repo.cards
        val tg = repo.tags
        val ctg = repo.cardToTag
        val t = repo.translationCards
        val s = repo.cardsSchedule
        val l = repo.translationCardsLog
        val expectedTextToTranslate = "A"
        val expectedTranslation = "a"
        val time1 = 1000L
        testClock.setFixedTime(time1)
        val tagId1 = dm.saveNewTag(SaveNewTagArgs("t1")).data!!
        val tagId2 = dm.saveNewTag(SaveNewTagArgs("t2")).data!!

        //when
        val translateCardId = dm.saveNewTranslateCard(
            SaveNewTranslateCardArgs(
                textToTranslate = " $expectedTextToTranslate\t",
                translation = "  \t$expectedTranslation    \t  ",
                tagIds = setOf(tagId1, tagId2)
            )
        ).data!!
        val translateCard = dm.getTranslateCardById(GetTranslateCardByIdArgs(cardId = translateCardId)).data!!

        //then
        assertEquals(expectedTextToTranslate, translateCard.textToTranslate)
        assertEquals(expectedTranslation, translateCard.translation)
        assertEquals("0m", translateCard.schedule.delay)
        assertEquals(0, translateCard.schedule.nextAccessInMillis)
        assertEquals(time1, translateCard.schedule.nextAccessAt)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to translateCard.id, c.type to TR_TP, c.createdAt to time1)
        ))
        assertTableContent(repo = repo, table = c.ver, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = tg, exactMatch = true, expectedRows = listOf(
            listOf(tg.id to tagId1, tg.createdAt to time1, tg.name to "t1"),
            listOf(tg.id to tagId2, tg.createdAt to time1, tg.name to "t2"),
        ))
        assertTableContent(repo = repo, table = ctg, exactMatch = true, expectedRows = listOf(
            listOf(ctg.cardId to translateCardId, ctg.tagId to tagId1),
            listOf(ctg.cardId to translateCardId, ctg.tagId to tagId2),
        ))

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to translateCard.id, t.textToTranslate to expectedTextToTranslate, t.translation to expectedTranslation)
        ))
        assertTableContent(repo = repo, table = t.ver, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to translateCard.id, s.delay to "0m", s.nextAccessInMillis to 0L, s.nextAccessAt to time1)
        ))
        assertTableContent(repo = repo, table = s.ver, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = l, exactMatch = true, expectedRows = listOf())
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
        ).data!!

        //when
        val timeDeleted = testClock.plus(1000)
        val deleteTranslateCardResp = dm.deleteTranslateCard(DeleteTranslateCardArgs(cardId = cardId))

        //then
        assertTrue(deleteTranslateCardResp.data!!)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, exactMatch = true, expectedRows = listOf())
        assertTableContent(repo = repo, table = c.ver, exactMatch = true, expectedRows = listOf(
            listOf(c.ver.timestamp to timeDeleted, c.id to cardId, c.type to TR_TP, c.createdAt to timeCreated)
        ))

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf())
        assertTableContent(repo = repo, table = t.ver, exactMatch = true, expectedRows = listOf(
            listOf(t.ver.timestamp to timeDeleted, t.cardId to cardId, t.textToTranslate to expectedTextToTranslate, t.translation to expectedTranslation)
        ))

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf())
        assertTableContent(repo = repo, table = s.ver, exactMatch = true, expectedRows = listOf(
            listOf(s.ver.timestamp to timeDeleted, s.cardId to cardId, s.delay to "0m", s.nextAccessInMillis to 0L, s.nextAccessAt to timeCreated)
        ))

        assertTableContent(repo = repo, table = l, exactMatch = true, expectedRows = listOf())
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
        ).data!!
        val timeDeleted1 = testClock.plus(1000)
        dm.deleteTranslateCard(DeleteTranslateCardArgs(cardId = cardId1))

        //when
        val timeCreated2 = testClock.plus(1000)
        val cardId2 = dm.saveNewTranslateCard(
            SaveNewTranslateCardArgs(textToTranslate = expectedTextToTranslate2, translation = expectedTranslation2)
        ).data!!

        //then
        assertNotEquals(cardId1, cardId2)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to cardId2, c.type to TR_TP, c.createdAt to timeCreated2)
        ))
        assertTableContent(repo = repo, table = c.ver, exactMatch = true, expectedRows = listOf(
            listOf(c.ver.timestamp to timeDeleted1, c.id to cardId1, c.type to TR_TP, c.createdAt to timeCreated1)
        ))

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to cardId2, t.textToTranslate to expectedTextToTranslate2, t.translation to expectedTranslation2)
        ))
        assertTableContent(repo = repo, table = t.ver, exactMatch = true, expectedRows = listOf(
            listOf(t.ver.timestamp to timeDeleted1, t.cardId to cardId1, t.textToTranslate to expectedTextToTranslate1, t.translation to expectedTranslation1)
        ))

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to cardId2, s.updatedAt to timeCreated2, s.delay to "0m", s.nextAccessInMillis to 0L, s.nextAccessAt to timeCreated2)
        ))
        assertTableContent(repo = repo, table = s.ver, exactMatch = true, expectedRows = listOf(
            listOf(s.ver.timestamp to timeDeleted1, s.cardId to cardId1, s.updatedAt to timeCreated1, s.delay to "0m", s.nextAccessInMillis to 0L, s.nextAccessAt to timeCreated1)
        ))

        assertTableContent(repo = repo, table = l, exactMatch = true, expectedRows = listOf())
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
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val c = repo.cards
        val s = repo.cardsSchedule
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
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val c = repo.cards
        val s = repo.cardsSchedule
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
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val c = repo.cards
        val s = repo.cardsSchedule
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
        insert(repo = repo, table = c, rows = listOf(
            createCardRecord(cardId = expectedCardId),
        ))
        insert(repo = repo, table = t, rows = listOf(
            listOf(t.cardId to expectedCardId, t.textToTranslate to "A", t.translation to " a\t"),
        ))
        assertTableContent(repo = repo, table = l, exactMatch = true, expectedRows = listOf())

        //when
        testClock.setFixedTime(baseTime)
        val time1 = testClock.instant().toEpochMilli()
        val actualResp = dm.validateTranslateCard(ValidateTranslateCardArgs(cardId = expectedCardId, userProvidedTranslation = "\ta   "))

        //then
        val actualValidationResults = actualResp.data!!
        assertTrue(actualValidationResults.isCorrect)
        assertEquals("a", actualValidationResults.answer)
        assertTableContent(repo = repo, table = l, exactMatch = true, expectedRows = listOf(
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
        insert(repo = repo, table = c, rows = listOf(
            createCardRecord(cardId = expectedCardId),
        ))
        insert(repo = repo, table = t, rows = listOf(
            listOf(t.cardId to expectedCardId, t.textToTranslate to "A", t.translation to " a\t"),
        ))
        assertTableContent(repo = repo, table = l, exactMatch = true, expectedRows = listOf())

        //when
        testClock.setFixedTime(baseTime)
        val time1 = testClock.instant().toEpochMilli()
        val actualResp = dm.validateTranslateCard(ValidateTranslateCardArgs(cardId = expectedCardId, userProvidedTranslation = "b"))

        //then
        val actualValidationResults = actualResp.data!!
        assertFalse(actualValidationResults.isCorrect)
        assertEquals("a", actualValidationResults.answer)
        assertTableContent(repo = repo, table = l, exactMatch = true, expectedRows = listOf(
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
        val cardId = dm.saveNewTranslateCard(SaveNewTranslateCardArgs(textToTranslate = "A", translation = "a")).data!!

        val validationTime1 = testClock.plus(3, ChronoUnit.MINUTES)
        assertTrue(dm.validateTranslateCard(ValidateTranslateCardArgs(cardId = cardId, userProvidedTranslation = "a")).data!!.isCorrect)

        val validationTime2 = testClock.plus(3, ChronoUnit.MINUTES)
        assertFalse(dm.validateTranslateCard(ValidateTranslateCardArgs(cardId = cardId, userProvidedTranslation = "1")).data!!.isCorrect)

        val validationTime3 = testClock.plus(3, ChronoUnit.MINUTES)
        assertTrue(dm.validateTranslateCard(ValidateTranslateCardArgs(cardId = cardId, userProvidedTranslation = "a")).data!!.isCorrect)

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
    fun saveNewTag_creates_new_tag() {
        //given
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val t = repo.tags
        val expectedTag1Name = "t1"
        val expectedTag2Name = "t2"
        val expectedTag3Name = "t3"
        testClock.setFixedTime(1000)

        //when
        val time1 = testClock.plus(2000)
        val tagId1 = dm.saveNewTag(SaveNewTagArgs(name = expectedTag1Name)).data!!
        val time2 = testClock.plus(2000)
        val tagId2 = dm.saveNewTag(SaveNewTagArgs(name = "  $expectedTag2Name   ")).data!!
        val time3 = testClock.plus(2000)
        val tagId3 = dm.saveNewTag(SaveNewTagArgs(name = "\t $expectedTag3Name \t")).data!!

        //then
        assertTableContent(repo = repo, table = t, exactMatch = true, matchColumn = t.id, expectedRows = listOf(
            listOf(t.id to tagId1, t.createdAt to time1, t.name to expectedTag1Name),
            listOf(t.id to tagId2, t.createdAt to time2, t.name to expectedTag2Name),
            listOf(t.id to tagId3, t.createdAt to time3, t.name to expectedTag3Name),
        ))
    }

    @Test
    fun saveNewTag_doesnt_allow_to_save_tag_with_same_name() {
        //given
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val t = repo.tags
        testClock.setFixedTime(1000)
        insert(repo = repo, table = t, rows = listOf(
            listOf(t.id to 1, t.createdAt to 1000, t.name to "ttt")
        ))
        assertTableContent(repo = repo, table = t, exactMatch = true, matchColumn = t.id, expectedRows = listOf(
            listOf(t.id to 1, t.createdAt to 1000, t.name to "ttt"),
        ))

        //when
        val err = dm.saveNewTag(SaveNewTagArgs(name = "ttt")).err!!

        //then
        assertEquals("A tag with name 'ttt' already exists.", err.msg)

        assertTableContent(repo = repo, table = t, exactMatch = true, matchColumn = t.id, expectedRows = listOf(
            listOf(t.id to 1, t.createdAt to 1000, t.name to "ttt"),
        ))
    }

    @Test
    fun updateTag_updates_tag() {
        //given
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val t = repo.tags
        val expectedTag1Name = "t1"
        val expectedTag2Name = "t2"
        val expectedTag3Name = "t3"
        testClock.setFixedTime(1000)

        val time1 = testClock.plus(2000)
        val tagId1 = dm.saveNewTag(SaveNewTagArgs(name = expectedTag1Name)).data!!
        val time2 = testClock.plus(2000)
        val tagId2 = dm.saveNewTag(SaveNewTagArgs(name = "  $expectedTag2Name   ")).data!!
        val time3 = testClock.plus(2000)
        val tagId3 = dm.saveNewTag(SaveNewTagArgs(name = "\t $expectedTag3Name \t")).data!!

        assertTableContent(repo = repo, table = t, exactMatch = true, matchColumn = t.id, expectedRows = listOf(
            listOf(t.id to tagId1, t.createdAt to time1, t.name to expectedTag1Name),
            listOf(t.id to tagId2, t.createdAt to time2, t.name to expectedTag2Name),
            listOf(t.id to tagId3, t.createdAt to time3, t.name to expectedTag3Name),
        ))

        //when
        testClock.plus(2000)
        val tag4 = dm.updateTag(UpdateTagArgs(tagId = tagId2, name = "  ddssdd  ")).data!!

        //then
        assertEquals("ddssdd", tag4.name)

        assertTableContent(repo = repo, table = t, exactMatch = true, matchColumn = t.id, expectedRows = listOf(
            listOf(t.id to tagId1, t.createdAt to time1, t.name to expectedTag1Name),
            listOf(t.id to tagId2, t.createdAt to time2, t.name to "ddssdd"),
            listOf(t.id to tagId3, t.createdAt to time3, t.name to expectedTag3Name),
        ))
    }

    @Test
    fun updateTag_doesnt_allow_to_update_tag_with_same_name() {
        //given
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val t = repo.tags
        val expectedTag1Name = "t1"
        val expectedTag2Name = "t2"
        val expectedTag3Name = "t3"
        testClock.setFixedTime(1000)

        val time1 = testClock.plus(2000)
        val tagId1 = dm.saveNewTag(SaveNewTagArgs(name = expectedTag1Name)).data!!
        val time2 = testClock.plus(2000)
        val tagId2 = dm.saveNewTag(SaveNewTagArgs(name = "  $expectedTag2Name   ")).data!!
        val time3 = testClock.plus(2000)
        val tagId3 = dm.saveNewTag(SaveNewTagArgs(name = "\t $expectedTag3Name \t")).data!!

        assertTableContent(repo = repo, table = t, exactMatch = true, matchColumn = t.id, expectedRows = listOf(
            listOf(t.id to tagId1, t.createdAt to time1, t.name to expectedTag1Name),
            listOf(t.id to tagId2, t.createdAt to time2, t.name to expectedTag2Name),
            listOf(t.id to tagId3, t.createdAt to time3, t.name to expectedTag3Name),
        ))

        //when
        val err = dm.updateTag(UpdateTagArgs(tagId = tagId2, name = expectedTag3Name)).err!!

        //then
        assertEquals("A tag with name '$expectedTag3Name' already exists.", err.msg)

        assertTableContent(repo = repo, table = t, exactMatch = true, matchColumn = t.id, expectedRows = listOf(
            listOf(t.id to tagId1, t.createdAt to time1, t.name to expectedTag1Name),
            listOf(t.id to tagId2, t.createdAt to time2, t.name to expectedTag2Name),
            listOf(t.id to tagId3, t.createdAt to time3, t.name to expectedTag3Name),
        ))
    }

    @Test
    fun deleteTag_deletes_tag() {
        //given
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val t = repo.tags
        val expectedTag1Name = "t1"
        val expectedTag2Name = "t2"
        val expectedTag3Name = "t3"
        testClock.setFixedTime(1000)

        val time1 = testClock.plus(2000)
        val tagId1 = dm.saveNewTag(SaveNewTagArgs(name = expectedTag1Name)).data!!
        val time2 = testClock.plus(2000)
        val tagId2 = dm.saveNewTag(SaveNewTagArgs(name = "  $expectedTag2Name   ")).data!!
        val time3 = testClock.plus(2000)
        val tagId3 = dm.saveNewTag(SaveNewTagArgs(name = "\t $expectedTag3Name \t")).data!!

        assertTableContent(repo = repo, table = t, exactMatch = true, matchColumn = t.id, expectedRows = listOf(
            listOf(t.id to tagId1, t.createdAt to time1, t.name to expectedTag1Name),
            listOf(t.id to tagId2, t.createdAt to time2, t.name to expectedTag2Name),
            listOf(t.id to tagId3, t.createdAt to time3, t.name to expectedTag3Name),
        ))

        //when
        testClock.plus(2000)
        val tagDeletionResult = dm.deleteTag(DeleteTagArgs(tagId = tagId2)).data!!

        //then
        assertTrue(tagDeletionResult)

        assertTableContent(repo = repo, table = t, exactMatch = true, matchColumn = t.id, expectedRows = listOf(
            listOf(t.id to tagId1, t.createdAt to time1, t.name to expectedTag1Name),
            listOf(t.id to tagId3, t.createdAt to time3, t.name to expectedTag3Name),
        ))
    }
}