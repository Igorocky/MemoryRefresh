package org.igye.memoryrefresh.unit.instrumentation

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.igye.memoryrefresh.manager.DataManager.CreateTranslateCardArgs
import org.igye.memoryrefresh.manager.DataManager.DeleteTranslateCardArgs
import org.igye.memoryrefresh.testutils.InstrumentedTestBase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeleteTranslateCardInstrumentedUnitTest: InstrumentedTestBase() {

    @Test
    fun deleteTranslateCard_deletes_translate_card() {
        //given
        val expectedTextToTranslate = "A"
        val expectedTranslation = "a"
        val timeCreated = testClock.currentMillis()
        val cardId = dm.createTranslateCard(
            CreateTranslateCardArgs(textToTranslate = expectedTextToTranslate, translation = expectedTranslation)
        ).data!!

        //when
        val timeDeleted = testClock.plus(1000)
        val deleteTranslateCardResp = dm.deleteTranslateCard(DeleteTranslateCardArgs(cardId = cardId))

        //then
        assertNotNull(deleteTranslateCardResp.data!!)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf())
        assertTableContent(repo = repo, table = c.ver, expectedRows = listOf(
            listOf(c.ver.timestamp to timeDeleted, c.id to cardId, c.type to TR_TP, c.createdAt to timeCreated)
        ))

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf())
        assertTableContent(repo = repo, table = t.ver, expectedRows = listOf(
            listOf(t.ver.timestamp to timeDeleted, t.cardId to cardId, t.textToTranslate to expectedTextToTranslate, t.translation to expectedTranslation)
        ))

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf())
        assertTableContent(repo = repo, table = s.ver, expectedRows = listOf(
            listOf(s.ver.timestamp to timeDeleted, s.cardId to cardId, s.delay to "1s", s.nextAccessInMillis to 1000L, s.nextAccessAt to timeCreated+1000)
        ))

        assertTableContent(repo = repo, table = l, expectedRows = listOf())
    }

    @Test
    fun deleteTranslateCard_ids_of_deleted_cards_are_not_reused() {
        //given
        val expectedTextToTranslate1 = "A"
        val expectedTranslation1 = "a"
        val expectedTextToTranslate2 = "B"
        val expectedTranslation2 = "b"
        val timeCreated1 = testClock.currentMillis()
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

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to cardId2, c.type to TR_TP, c.createdAt to timeCreated2)
        ))
        assertTableContent(repo = repo, table = c.ver, expectedRows = listOf(
            listOf(c.ver.timestamp to timeDeleted1, c.id to cardId1, c.type to TR_TP, c.createdAt to timeCreated1)
        ))

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to cardId2, t.textToTranslate to expectedTextToTranslate2, t.translation to expectedTranslation2)
        ))
        assertTableContent(repo = repo, table = t.ver, expectedRows = listOf(
            listOf(t.ver.timestamp to timeDeleted1, t.cardId to cardId1, t.textToTranslate to expectedTextToTranslate1, t.translation to expectedTranslation1)
        ))

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to cardId2, s.updatedAt to timeCreated2, s.delay to "1s", s.nextAccessInMillis to 1000L, s.nextAccessAt to timeCreated2+1000)
        ))
        assertTableContent(repo = repo, table = s.ver, expectedRows = listOf(
            listOf(s.ver.timestamp to timeDeleted1, s.cardId to cardId1, s.updatedAt to timeCreated1, s.delay to "1s", s.nextAccessInMillis to 1000L, s.nextAccessAt to timeCreated1+1000)
        ))

        assertTableContent(repo = repo, table = l, expectedRows = listOf())
    }

}