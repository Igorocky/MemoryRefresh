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
class ReadNoteCardInstrumentedUnitTest: InstrumentedTestBase() {

    @Test
    fun readNoteCardById_returns_all_tags_of_the_card() {
        //given
        val tagId1 = dm.createTag(CreateTagArgs("t1")).data!!
        val tagId2 = dm.createTag(CreateTagArgs("t2")).data!!
        val tagId3 = dm.createTag(CreateTagArgs("t3")).data!!
        val cardId = dm.createNoteCard(
            CreateNoteCardArgs(
            text = "a", tagIds = setOf(tagId1, tagId3)
        )
        ).data!!

        //when
        val actualCard = dm.readNoteCardById(ReadNoteCardByIdArgs(cardId = cardId)).data!!

        //then
        assertEquals(2, actualCard.tagIds.size)
        assertTrue(actualCard.tagIds.contains(tagId1))
        assertTrue(actualCard.tagIds.contains(tagId3))
    }

    @Test
    fun readNoteCardById_returns_correct_values_for_each_parameter() {
        //given
        val tagId1 = createTag(tagId = 1, name = "t1")
        val tagId2 = createTag(tagId = 2, name = "t2")
        val tagId3 = createTag(tagId = 3, name = "t3")

        val expectedCardId1 = 1L
        val createTime1 = testClock.currentMillis()
        val lastCheckedAt1 = createTime1 + 12664
        val updatedAt1 = createTime1 + 55332
        val origDelay1 = "5895654"
        val delay1 = "88fgdfd"
        val nextAccessInMillis1 = 376444L
        val nextAccessAt1 = createTime1 + MILLIS_IN_MINUTE
        val text1 = "snsndfhg73456"

        val expectedCardId2 = 2L
        val createTime2 = testClock.plus(2, ChronoUnit.HOURS)
        val lastCheckedAt2 = createTime2 + 7756
        val updatedAt2 = createTime2 + 88565
        val origDelay2 = "sdfsvf44"
        val delay2 = "467567hh"
        val nextAccessInMillis2 = 667863L
        val nextAccessAt2 = createTime2 + MILLIS_IN_HOUR
        val text2 = "dfjksd7253df"

        insert(repo = repo, table = c, rows = listOf(
            listOf(c.id to expectedCardId1, c.type to NC_TP, c.createdAt to createTime1, c.paused to 0, c.lastCheckedAt to lastCheckedAt1),
            listOf(c.id to expectedCardId2, c.type to NC_TP, c.createdAt to createTime2, c.paused to 1, c.lastCheckedAt to lastCheckedAt2),
        ))
        insert(repo = repo, table = s, rows = listOf(
            listOf(s.cardId to expectedCardId1, s.updatedAt to updatedAt1, s.origDelay to origDelay1, s.delay to delay1, s.randomFactor to 1.0, s.nextAccessInMillis to nextAccessInMillis1, s.nextAccessAt to nextAccessAt1),
            listOf(s.cardId to expectedCardId2, s.updatedAt to updatedAt2, s.origDelay to origDelay2, s.delay to delay2, s.randomFactor to 1.0, s.nextAccessInMillis to nextAccessInMillis2, s.nextAccessAt to nextAccessAt2),
        ))
        insert(repo = repo, table = n, rows = listOf(
            listOf(n.cardId to expectedCardId1, n.text to text1),
            listOf(n.cardId to expectedCardId2, n.text to text2),
        ))
        insert(repo = repo, table = ctg, rows = listOf(
            listOf(ctg.cardId to expectedCardId1, ctg.tagId to tagId1),
            listOf(ctg.cardId to expectedCardId1, ctg.tagId to tagId2),
            listOf(ctg.cardId to expectedCardId2, ctg.tagId to tagId2),
            listOf(ctg.cardId to expectedCardId2, ctg.tagId to tagId3),
        ))

        //when
        val readTime = testClock.plus(2, ChronoUnit.MINUTES)
        val card1 = dm.readNoteCardById(ReadNoteCardByIdArgs(cardId = expectedCardId1)).data!!
        val card2 = dm.readNoteCardById(ReadNoteCardByIdArgs(cardId = expectedCardId2)).data!!

        //then
        assertEquals(expectedCardId1, card1.id)
        assertEquals(createTime1, card1.createdAt)
        assertEquals(false, card1.paused)
        assertEquals(setOf(tagId1, tagId2), card1.tagIds.toSet())
        assertEquals(expectedCardId1, card1.schedule.cardId)
        assertEquals(updatedAt1, card1.schedule.updatedAt)
        assertEquals(origDelay1, card1.schedule.origDelay)
        assertEquals(delay1, card1.schedule.delay)
        assertEquals(nextAccessInMillis1, card1.schedule.nextAccessInMillis)
        assertEquals(nextAccessAt1, card1.schedule.nextAccessAt)
        assertEquals(Utils.millisToDurationStr(readTime - lastCheckedAt1), card1.timeSinceLastCheck)
        assertEquals("-", card1.activatesIn)
        assertEquals( (readTime - nextAccessAt1) / (nextAccessInMillis1 * 1.0), card1.overdue, 0.000001)
        assertEquals(text1, card1.text)

        assertEquals(expectedCardId2, card2.id)
        assertEquals(createTime2, card2.createdAt)
        assertEquals(true, card2.paused)
        assertEquals(setOf(tagId3, tagId2), card2.tagIds.toSet())
        assertEquals(expectedCardId2, card2.schedule.cardId)
        assertEquals(updatedAt2, card2.schedule.updatedAt)
        assertEquals(origDelay2, card2.schedule.origDelay)
        assertEquals(delay2, card2.schedule.delay)
        assertEquals(nextAccessInMillis2, card2.schedule.nextAccessInMillis)
        assertEquals(nextAccessAt2, card2.schedule.nextAccessAt)
        assertEquals(Utils.millisToDurationStr(readTime - lastCheckedAt2), card2.timeSinceLastCheck)
        assertEquals(Utils.millisToDurationStr(nextAccessAt2 - readTime), card2.activatesIn)
        assertEquals( (readTime - nextAccessAt2) / (nextAccessInMillis2 * 1.0), card2.overdue, 0.000001)
    }

    @Test
    fun readNoteCardById_returns_empty_collection_of_tag_ids_if_card_doesnt_have_tags() {
        //given
        val tagId1 = dm.createTag(CreateTagArgs("t1")).data!!
        val tagId2 = dm.createTag(CreateTagArgs("t2")).data!!
        val tagId3 = dm.createTag(CreateTagArgs("t3")).data!!
        val cardId = dm.createNoteCard(CreateNoteCardArgs(
            text = "a", tagIds = setOf()
        )).data!!

        //when
        val actualCard = dm.readNoteCardById(ReadNoteCardByIdArgs(cardId = cardId)).data!!

        //then
        assertEquals(0, actualCard.tagIds.size)
    }

    @Test
    fun selectTopOverdueNoteCards_returns_correct_results_when_only_one_card_is_present_in_the_database() {
        //given
        val expectedCardId = 1L
        val baseTime = 27000
        insert(repo = repo, table = c, rows = listOf(
            listOf(c.id to expectedCardId, c.type to NC_TP, c.createdAt to 0, c.lastCheckedAt to 0)
        ))
        insert(repo = repo, table = s, rows = listOf(
            listOf(s.cardId to expectedCardId, s.updatedAt to 0, s.origDelay to "1m", s.delay to "1m", s.randomFactor to 1.0, s.nextAccessInMillis to 100, s.nextAccessAt to baseTime + 100)
        ))
        insert(repo = repo, table = n, rows = listOf(
            listOf(n.cardId to expectedCardId, n.text to "0")
        ))

        //when
        testClock.setFixedTime(baseTime + 145)
        val actualTopOverdueCards = dm.selectTopOverdueNoteCards().data!!

        //then
        val actualOverdue = actualTopOverdueCards.cards
        assertEquals(1, actualOverdue.size)
        assertEquals(expectedCardId, actualOverdue[0].id)
        assertEquals(0.45, actualOverdue[0].overdue, 0.000001)
    }

    @Test
    fun selectTopOverdueNoteCards_doesnt_return_cards_without_overdue() {
        //given
        val expectedCardId = 1L
        val baseTime = 27000
        insert(repo = repo, table = c, rows = listOf(
            listOf(c.id to expectedCardId, c.type to NC_TP, c.createdAt to 0, c.lastCheckedAt to 0)
        ))
        insert(repo = repo, table = s, rows = listOf(
            listOf(s.cardId to expectedCardId, s.updatedAt to 0, s.origDelay to "1m", s.delay to "1m", s.randomFactor to 1.0, s.nextAccessInMillis to 100, s.nextAccessAt to baseTime + 100)
        ))
        insert(repo = repo, table = n, rows = listOf(
            listOf(n.cardId to expectedCardId, n.text to "0")
        ))

        //when
        testClock.setFixedTime(baseTime + 99)
        val actualTopOverdueCards = dm.selectTopOverdueNoteCards().data!!

        //then
        val actualOverdue = actualTopOverdueCards.cards
        assertEquals(0, actualOverdue.size)
    }

    @Test
    fun selectTopOverdueNoteCards_doesnt_return_cards_when_there_are_no_cards_in_the_database() {
        //given
        val baseTime = 27000

        //when
        testClock.setFixedTime(baseTime + 99)
        val selectTopOverdueNoteCards = dm.selectTopOverdueNoteCards()
        val actualTopOverdueCards = selectTopOverdueNoteCards.data!!

        //then
        val actualOverdue = actualTopOverdueCards.cards
        assertEquals(0, actualOverdue.size)
    }

    @Test
    fun selectTopOverdueNoteCards_selects_cards_correctly_when_there_are_many_cards() {
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
        fun createCardRecord(cardId: Long) = listOf(c.id to cardId, c.type to NC_TP, c.createdAt to 0, c.lastCheckedAt to 0)
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
            listOf(s.cardId to cardId, s.updatedAt to 0, s.origDelay to "1m", s.delay to "1m", s.randomFactor to 1.0, s.nextAccessInMillis to nextAccessIn, s.nextAccessAt to baseTime + nextAccessIn)
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
        fun createNoteRecord(cardId: Long) =
            listOf(n.cardId to cardId, n.text to "0")
        insert(repo = repo, table = n, rows = listOf(
            createNoteRecord(cardId = cardIdWithoutOverdue1),
            createNoteRecord(cardId = cardIdWithLargeOverdue),
            createNoteRecord(cardId = cardIdWithoutOverdue2),
            createNoteRecord(cardId = cardIdWithZeroOverdue),
            createNoteRecord(cardId = cardIdWithBigOverdue),
            createNoteRecord(cardId = cardIdWithoutOverdue3),
            createNoteRecord(cardId = cardIdWithSmallOverdue),
            createNoteRecord(cardId = cardIdWithoutOverdue4),
        ))

        //when
        testClock.setFixedTime(baseTime + timeElapsed)
        val actualTopOverdueCards = dm.selectTopOverdueNoteCards().data!!

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
    fun selectTopOverdueNoteCards_doesnt_return_paused_cards() {
        //given
        val cardId1 = 1L
        val cardId2 = 2L
        val cardId3 = 3L
        val cardId4 = 4L
        val cardId5 = 5L
        fun createCardRecord(cardId: Long, paused: Int) = listOf(c.id to cardId, c.paused to paused, c.type to NC_TP, c.createdAt to 0, c.lastCheckedAt to 0)
        insert(repo = repo, table = c, rows = listOf(
            createCardRecord(cardId = cardId1, paused = 0),
            createCardRecord(cardId = cardId2, paused = 1),
            createCardRecord(cardId = cardId3, paused = 0),
            createCardRecord(cardId = cardId4, paused = 1),
            createCardRecord(cardId = cardId5, paused = 0),
        ))
        fun createScheduleRecord(cardId: Long) =
            listOf(s.cardId to cardId, s.updatedAt to 0, s.origDelay to "1m", s.delay to "1m", s.randomFactor to 1.0, s.nextAccessInMillis to 1, s.nextAccessAt to 1)
        insert(repo = repo, table = s, rows = listOf(
            createScheduleRecord(cardId = cardId1),
            createScheduleRecord(cardId = cardId2),
            createScheduleRecord(cardId = cardId3),
            createScheduleRecord(cardId = cardId4),
            createScheduleRecord(cardId = cardId5),
        ))
        fun createNoteRecord(cardId: Long) =
            listOf(n.cardId to cardId, n.text to "0")
        insert(repo = repo, table = n, rows = listOf(
            createNoteRecord(cardId = cardId1),
            createNoteRecord(cardId = cardId2),
            createNoteRecord(cardId = cardId3),
            createNoteRecord(cardId = cardId4),
            createNoteRecord(cardId = cardId5),
        ))

        //when
        testClock.setFixedTime(1000)
        val actualTopOverdueCards = dm.selectTopOverdueNoteCards().data!!

        //then
        val actualOverdue = actualTopOverdueCards.cards
        assertEquals(3, actualOverdue.size)

        val actualIds = actualOverdue.map { it.id }.toSet()
        assertTrue(actualIds.contains(cardId1))
        assertTrue(actualIds.contains(cardId3))
        assertTrue(actualIds.contains(cardId5))

        assertFalse(actualIds.contains(cardId2))
        assertFalse(actualIds.contains(cardId4))
    }

    @Test
    fun selectTopOverdueNoteCards_returns_time_to_wait_str() {
        //given
        val expectedCardId = 1236L
        val baseTime = 1_000
        val timeElapsed = 27_000
        fun createCardRecord(cardId: Long) = listOf(c.id to cardId, c.type to NC_TP, c.createdAt to 0, c.lastCheckedAt to 0)
        insert(repo = repo, table = c, rows = listOf(
            createCardRecord(cardId = expectedCardId),
        ))
        fun createScheduleRecord(cardId: Long, nextAccessIn: Int) =
            listOf(s.cardId to cardId, s.updatedAt to 0, s.origDelay to "1m", s.delay to "1m", s.randomFactor to 1.0, s.nextAccessInMillis to nextAccessIn, s.nextAccessAt to baseTime + nextAccessIn)
        insert(repo = repo, table = s, rows = listOf(
            createScheduleRecord(cardId = expectedCardId, nextAccessIn = (timeElapsed + 2*MILLIS_IN_HOUR + 3*MILLIS_IN_MINUTE + 39*MILLIS_IN_SECOND).toInt()),
        ))
        fun createNoteRecord(cardId: Long) =
            listOf(n.cardId to cardId, n.text to "0")
        insert(repo = repo, table = n, rows = listOf(
            createNoteRecord(cardId = expectedCardId),
        ))

        //when
        testClock.setFixedTime(baseTime + timeElapsed)
        val cardToRepeatResp = dm.selectTopOverdueNoteCards().data!!

        //then
        assertEquals(0, cardToRepeatResp.cards.size)
        assertEquals("2h 3m", cardToRepeatResp.nextCardIn)
    }

    @Test
    fun selectTopOverdueNoteCards_returns_empty_time_to_wait_str_if_there_are_no_cards_at_all() {
        //given
        val baseTime = 1_000
        val timeElapsed = 27_000

        //when
        testClock.setFixedTime(baseTime + timeElapsed)
        val cardToRepeatResp = dm.selectTopOverdueNoteCards().data!!

        //then
        assertEquals(0, cardToRepeatResp.cards.size)
        assertEquals("", cardToRepeatResp.nextCardIn)
    }

    @Test
    fun selectTopOverdueNoteCards_returns_only_note_cards_when_there_are_both_note_and_translate_cards_in_the_database() {
        //given
        val expectedNoteCardId = 1L
        val expectedTranslateCardId = 2L
        val baseTime = 27000

        insert(repo = repo, table = c, rows = listOf(
            listOf(c.id to expectedNoteCardId, c.type to NC_TP, c.createdAt to 0, c.lastCheckedAt to 0)
        ))
        insert(repo = repo, table = s, rows = listOf(
            listOf(s.cardId to expectedNoteCardId, s.updatedAt to 0, s.origDelay to "1m", s.delay to "1m", s.randomFactor to 1.0, s.nextAccessInMillis to 100, s.nextAccessAt to baseTime + 100)
        ))
        insert(repo = repo, table = n, rows = listOf(
            listOf(n.cardId to expectedNoteCardId, n.text to "0")
        ))

        insert(repo = repo, table = c, rows = listOf(
            listOf(c.id to expectedTranslateCardId, c.type to TR_TP, c.createdAt to 0, c.lastCheckedAt to 0)
        ))
        insert(repo = repo, table = s, rows = listOf(
            listOf(s.cardId to expectedTranslateCardId, s.updatedAt to 0, s.origDelay to "1m", s.delay to "1m", s.randomFactor to 1.0, s.nextAccessInMillis to 100, s.nextAccessAt to baseTime + 100)
        ))
        insert(repo = repo, table = t, rows = listOf(
            listOf(t.cardId to expectedTranslateCardId, t.textToTranslate to "0", t.translation to "0")
        ))

        //when
        testClock.setFixedTime(baseTime + 145)
        val actualTopOverdueCards = dm.selectTopOverdueNoteCards().data!!

        //then
        val actualOverdue = actualTopOverdueCards.cards
        assertEquals(1, actualOverdue.size)
        assertEquals(expectedNoteCardId, actualOverdue[0].id)
        assertEquals(0.45, actualOverdue[0].overdue, 0.000001)
    }

    @Test
    fun readNoteCardHistory_when_there_are_few_data_history_records() {
        //given
        val createTime1 = testClock.currentMillis()
        val cardId = dm.createNoteCard(CreateNoteCardArgs(text = "A")).data!!

        val updateTime5 = testClock.plus(5, ChronoUnit.MINUTES)
        dm.updateNoteCard(UpdateNoteCardArgs(cardId = cardId, text = "B"))

        val updateTime9 = testClock.plus(9, ChronoUnit.MINUTES)
        dm.updateNoteCard(UpdateNoteCardArgs(cardId = cardId, text = "C"))

        //when
        val actualHistory = dm.readNoteCardHistory(ReadNoteCardHistoryArgs(cardId = cardId)).data!!

        //then
        assertEquals(3, actualHistory.dataHistory.size)

        assertEquals(updateTime9, actualHistory.dataHistory[0].timestamp)
        assertEquals("C", actualHistory.dataHistory[0].text)

        assertEquals(updateTime5, actualHistory.dataHistory[1].timestamp)
        assertEquals("B", actualHistory.dataHistory[1].text)

        assertEquals(createTime1, actualHistory.dataHistory[2].timestamp)
        assertEquals("A", actualHistory.dataHistory[2].text)
    }

    @Test
    fun readNoteCardHistory_when_there_are_no_data_changes() {
        //given
        val createTime1 = testClock.currentMillis()
        val cardId = dm.createNoteCard(CreateNoteCardArgs(text = "A")).data!!

        //when
        val actualHistory = dm.readNoteCardHistory(ReadNoteCardHistoryArgs(cardId = cardId)).data!!

        //then
        assertEquals(1, actualHistory.dataHistory.size)

        assertEquals(createTime1, actualHistory.dataHistory[0].timestamp)
        assertEquals("A", actualHistory.dataHistory[0].text)
    }

    @Test
    fun readNoteCardHistory_when_there_is_one_data_change() {
        //given
        val createTime1 = testClock.currentMillis()
        val cardId = dm.createNoteCard(CreateNoteCardArgs(text = "A")).data!!

        val updateTime5 = testClock.plus(5, ChronoUnit.MINUTES)
        dm.updateNoteCard(UpdateNoteCardArgs(cardId = cardId, text = "B"))

        //when
        val actualHistory = dm.readNoteCardHistory(ReadNoteCardHistoryArgs(cardId = cardId)).data!!

        //then
        assertEquals(2, actualHistory.dataHistory.size)

        assertEquals(updateTime5, actualHistory.dataHistory[0].timestamp)
        assertEquals("B", actualHistory.dataHistory[0].text)

        assertEquals(createTime1, actualHistory.dataHistory[1].timestamp)
        assertEquals("A", actualHistory.dataHistory[1].text)
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

        //search by 0 tags - all cards are returned
        assertSearchResult(
            listOf(card1, card2, card3, card4),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                tagIdsToInclude = setOf()
            ))
        )

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

        //search by 0 tags - all cards are returned
        assertSearchResult(
            listOf(card1, card2, card3, card4),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                tagIdsToExclude = setOf()
            ))
        )

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
    fun readTranslateCardsByFilter_filters_by_nextAccessFrom() {
        val card1 = createCard(cardId = 1L)
        testClock.plus(MILLIS_IN_MINUTE)
        val card2 = createCard(cardId = 2L)
        testClock.plus(MILLIS_IN_MINUTE)
        val card3 = createCard(cardId = 3L)
        testClock.plus(MILLIS_IN_MINUTE)
        val card4 = createCard(cardId = 4L)
        testClock.plus(MILLIS_IN_MINUTE)
        val card5 = createCard(cardId = 5L)
        testClock.plus(MILLIS_IN_MINUTE)

        assertTrue(card1.schedule.nextAccessAt < card2.schedule.nextAccessAt - 1000)
        assertTrue(card2.schedule.nextAccessAt < card3.schedule.nextAccessAt - 1000)
        assertTrue(card3.schedule.nextAccessAt < card4.schedule.nextAccessAt - 1000)
        assertTrue(card4.schedule.nextAccessAt < card5.schedule.nextAccessAt - 1000)

        assertSearchResult(
            listOf(card3, card4, card5),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                nextAccessFrom = card3.schedule.nextAccessAt
            )),
            skipTimeSinceLastCheck = true,
            skipOverdue = true
        )

        assertSearchResult(
            listOf(card4, card5),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                nextAccessFrom = card3.schedule.nextAccessAt+1
            )),
            skipTimeSinceLastCheck = true,
            skipOverdue = true
        )
    }

    @Test
    fun readTranslateCardsByFilter_filters_by_nextAccessTill() {
        val card1 = createCard(cardId = 1L)
        testClock.plus(MILLIS_IN_MINUTE)
        val card2 = createCard(cardId = 2L)
        testClock.plus(MILLIS_IN_MINUTE)
        val card3 = createCard(cardId = 3L)
        testClock.plus(MILLIS_IN_MINUTE)
        val card4 = createCard(cardId = 4L)
        testClock.plus(MILLIS_IN_MINUTE)
        val card5 = createCard(cardId = 5L)
        testClock.plus(MILLIS_IN_MINUTE)

        assertTrue(card1.schedule.nextAccessAt < card2.schedule.nextAccessAt - 1000)
        assertTrue(card2.schedule.nextAccessAt < card3.schedule.nextAccessAt - 1000)
        assertTrue(card3.schedule.nextAccessAt < card4.schedule.nextAccessAt - 1000)
        assertTrue(card4.schedule.nextAccessAt < card5.schedule.nextAccessAt - 1000)

        assertSearchResult(
            listOf(card1, card2, card3),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                nextAccessTill = card3.schedule.nextAccessAt
            )),
            skipTimeSinceLastCheck = true,
            skipOverdue = true
        )

        assertSearchResult(
            listOf(card1, card2),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                nextAccessTill = card3.schedule.nextAccessAt-1
            )),
            skipTimeSinceLastCheck = true,
            skipOverdue = true
        )
    }

    @Test
    fun readTranslateCardsByFilter_filters_by_nextAccessFrom_and_nextAccessTill() {
        val card1 = createCard(cardId = 1L)
        testClock.plus(MILLIS_IN_MINUTE)
        val card2 = createCard(cardId = 2L)
        testClock.plus(MILLIS_IN_MINUTE)
        val card3 = createCard(cardId = 3L)
        testClock.plus(MILLIS_IN_MINUTE)
        val card4 = createCard(cardId = 4L)
        testClock.plus(MILLIS_IN_MINUTE)
        val card5 = createCard(cardId = 5L)
        testClock.plus(MILLIS_IN_MINUTE)

        assertTrue(card1.schedule.nextAccessAt < card2.schedule.nextAccessAt - 1000)
        assertTrue(card2.schedule.nextAccessAt < card3.schedule.nextAccessAt - 1000)
        assertTrue(card3.schedule.nextAccessAt < card4.schedule.nextAccessAt - 1000)
        assertTrue(card4.schedule.nextAccessAt < card5.schedule.nextAccessAt - 1000)

        assertSearchResult(
            listOf(card2, card3, card4),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                nextAccessFrom = card2.schedule.nextAccessAt,
                nextAccessTill = card4.schedule.nextAccessAt,
            )),
            skipTimeSinceLastCheck = true,
            skipOverdue = true
        )

        assertSearchResult(
            listOf(card3),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                nextAccessFrom = card2.schedule.nextAccessAt+1,
                nextAccessTill = card4.schedule.nextAccessAt-1,
            )),
            skipTimeSinceLastCheck = true,
            skipOverdue = true
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

        val cardsSortedByNextAccessAtAsc = listOf(card1,card2,card3,card4,card5).sortedBy { it.schedule.nextAccessAt }
        assertEquals(
            cardsSortedByNextAccessAtAsc.size,
            cardsSortedByNextAccessAtAsc.map { it.schedule.nextAccessAt }.toSet().size
        )

        assertSearchResult(
            cardsSortedByNextAccessAtAsc,
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                sortBy = TranslateCardSortBy.NEXT_ACCESS_AT
            )),
            matchOrder = true
        )

        assertSearchResult(
            cardsSortedByNextAccessAtAsc,
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                sortBy = TranslateCardSortBy.NEXT_ACCESS_AT,
                sortDir = SortDirection.ASC
            )),
            matchOrder = true
        )

        assertSearchResult(
            cardsSortedByNextAccessAtAsc.reversed(),
            dm.readTranslateCardsByFilter(ReadTranslateCardsByFilterArgs(
                sortBy = TranslateCardSortBy.NEXT_ACCESS_AT,
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

    private fun assertSearchResult(
        expected: List<TranslateCard>,
        actual: BeRespose<ReadTranslateCardsByFilterResp>,
        matchOrder:Boolean = false,
        skipTimeSinceLastCheck: Boolean = false,
        skipOverdue: Boolean = false,
    ) {
        val actualCardsList = actual.data!!.cards
        assertEquals(expected.size, actualCardsList.size)
        var cnt = 0
        if (matchOrder) {
            for (i in expected.indices) {
                assertTranslateCardsEqual(expected[i], actualCardsList[i], skipTimeSinceLastCheck = skipTimeSinceLastCheck, skipOverdue = skipOverdue)
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
                    assertTranslateCardsEqual(expected[i], actualCard, skipTimeSinceLastCheck = skipTimeSinceLastCheck, skipOverdue = skipOverdue)
                }
                cnt++
            }
        }
        assertEquals(expected.size, cnt)
    }
}