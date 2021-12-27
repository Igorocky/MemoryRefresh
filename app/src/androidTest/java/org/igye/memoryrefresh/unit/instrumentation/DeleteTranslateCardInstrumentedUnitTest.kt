package org.igye.memoryrefresh.unit.instrumentation

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.igye.memoryrefresh.manager.DataManager.CreateTranslateCardArgs
import org.igye.memoryrefresh.manager.DataManager.DeleteTranslateCardArgs
import org.igye.memoryrefresh.testutils.InstrumentedTestBase
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeleteTranslateCardInstrumentedUnitTest: InstrumentedTestBase() {

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
        val cardId = dm.createTranslateCard(
            CreateTranslateCardArgs(textToTranslate = expectedTextToTranslate, translation = expectedTranslation)
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
        val cardId1 = dm.createTranslateCard(
            CreateTranslateCardArgs(textToTranslate = expectedTextToTranslate1, translation = expectedTranslation1)
        ).data!!
        val timeDeleted1 = testClock.plus(1000)
        dm.deleteTranslateCard(DeleteTranslateCardArgs(cardId = cardId1))

        //when
        val timeCreated2 = testClock.plus(1000)
        val cardId2 = dm.createTranslateCard(
            CreateTranslateCardArgs(textToTranslate = expectedTextToTranslate2, translation = expectedTranslation2)
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

}