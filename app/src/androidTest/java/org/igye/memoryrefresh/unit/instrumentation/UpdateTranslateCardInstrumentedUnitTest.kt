package org.igye.memoryrefresh.unit.instrumentation

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.igye.memoryrefresh.manager.DataManager.*
import org.igye.memoryrefresh.testutils.InstrumentedTestBase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UpdateTranslateCardInstrumentedUnitTest: InstrumentedTestBase() {

    @Test
    fun updateTranslateCard_adds_new_tags_to_card_without_tags() {
        //given
        val tagId1 = dm.createTag(CreateTagArgs(name = "A")).data!!
        val tagId2 = dm.createTag(CreateTagArgs(name = "B")).data!!
        val tagId3 = dm.createTag(CreateTagArgs(name = "C")).data!!
        val cardId = dm.createTranslateCard(CreateTranslateCardArgs(textToTranslate = "X", translation = "x")).data!!

        assertTableContent(repo = repo, table = ctg, exactMatch = true, expectedRows = listOf())

        //when
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = cardId, tagIds = setOf(tagId1,tagId2,tagId3)))

        //then
        assertTableContent(repo = repo, table = ctg, exactMatch = true, expectedRows = listOf(
            listOf(ctg.cardId to cardId, ctg.tagId to tagId1),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId2),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId3),
        ))
    }

    @Test
    fun updateTranslateCard_adds_new_tags_to_card_with_tags() {
        //given
        val tagId1 = dm.createTag(CreateTagArgs(name = "A")).data!!
        val tagId2 = dm.createTag(CreateTagArgs(name = "B")).data!!
        val tagId3 = dm.createTag(CreateTagArgs(name = "C")).data!!
        val tagId4 = dm.createTag(CreateTagArgs(name = "D")).data!!
        val cardId = dm.createTranslateCard(CreateTranslateCardArgs(
            textToTranslate = "X", translation = "x", tagIds = setOf(tagId1, tagId2)
        )).data!!

        assertTableContent(repo = repo, table = ctg, exactMatch = true, expectedRows = listOf(
            listOf(ctg.cardId to cardId, ctg.tagId to tagId1),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId2),
        ))

        //when
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = cardId, tagIds = setOf(tagId1,tagId2,tagId3,tagId4)))

        //then
        assertTableContent(repo = repo, table = ctg, exactMatch = true, expectedRows = listOf(
            listOf(ctg.cardId to cardId, ctg.tagId to tagId1),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId2),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId3),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId4),
        ))
    }

    @Test
    fun updateTranslateCard_removes_all_tags_from_card_with_tags() {
        //given
        val tagId1 = dm.createTag(CreateTagArgs(name = "A")).data!!
        val tagId2 = dm.createTag(CreateTagArgs(name = "B")).data!!
        val cardId = dm.createTranslateCard(CreateTranslateCardArgs(
            textToTranslate = "X", translation = "x", tagIds = setOf(tagId1, tagId2)
        )).data!!

        assertTableContent(repo = repo, table = ctg, exactMatch = true, expectedRows = listOf(
            listOf(ctg.cardId to cardId, ctg.tagId to tagId1),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId2),
        ))

        //when
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = cardId, tagIds = emptySet()))

        //then
        assertTableContent(repo = repo, table = ctg, exactMatch = true, expectedRows = listOf())
    }

    @Test
    fun updateTranslateCard_doesnt_modify_tags_if_tagIds_is_null_in_the_request() {
        //given
        val tagId1 = dm.createTag(CreateTagArgs(name = "A")).data!!
        val tagId2 = dm.createTag(CreateTagArgs(name = "B")).data!!
        val cardId = dm.createTranslateCard(CreateTranslateCardArgs(
            textToTranslate = "X", translation = "x", tagIds = setOf(tagId1, tagId2)
        )).data!!

        assertTableContent(repo = repo, table = ctg, exactMatch = true, expectedRows = listOf(
            listOf(ctg.cardId to cardId, ctg.tagId to tagId1),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId2),
        ))

        //when
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = cardId, tagIds = null))

        //then
        assertTableContent(repo = repo, table = ctg, exactMatch = true, expectedRows = listOf(
            listOf(ctg.cardId to cardId, ctg.tagId to tagId1),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId2),
        ))
    }

    @Test
    fun updateTranslateCard_doesnt_fail_when_requested_to_removes_all_tags_from_card_without_tags() {
        //given
        val cardId = dm.createTranslateCard(CreateTranslateCardArgs(
            textToTranslate = "X", translation = "x"
        )).data!!

        assertTableContent(repo = repo, table = ctg, exactMatch = true, expectedRows = listOf())

        //when
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = cardId, tagIds = emptySet()))

        //then
        assertTableContent(repo = repo, table = ctg, exactMatch = true, expectedRows = listOf())
    }

    @Test
    fun updateTranslateCard_adds_few_new_and_removes_few_existing_and_doesnt_touch_few_existing_tags_for_card_with_tags() {
        //given
        val tagId1 = dm.createTag(CreateTagArgs(name = "A")).data!!
        val tagId2 = dm.createTag(CreateTagArgs(name = "B")).data!!
        val tagId3 = dm.createTag(CreateTagArgs(name = "C")).data!!
        val tagId4 = dm.createTag(CreateTagArgs(name = "D")).data!!
        val tagId5 = dm.createTag(CreateTagArgs(name = "E")).data!!
        val tagId6 = dm.createTag(CreateTagArgs(name = "F")).data!!
        val cardId = dm.createTranslateCard(CreateTranslateCardArgs(
            textToTranslate = "X", translation = "x", tagIds = setOf(tagId1, tagId2, tagId3, tagId4)
        )).data!!

        assertTableContent(repo = repo, table = ctg, exactMatch = true, expectedRows = listOf(
            listOf(ctg.cardId to cardId, ctg.tagId to tagId1),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId2),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId3),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId4),
        ))

        //when
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = cardId, tagIds = setOf(tagId5,tagId6,tagId3,tagId4)))

        //then
        assertTableContent(repo = repo, table = ctg, exactMatch = true, expectedRows = listOf(
            listOf(ctg.cardId to cardId, ctg.tagId to tagId5),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId6),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId3),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId4),
        ))
    }

    @Test
    fun updateTranslateCard_updates_all_parameters_simultaneously() {
        //given
        val tagId1 = dm.createTag(CreateTagArgs(name = "A")).data!!
        val tagId2 = dm.createTag(CreateTagArgs(name = "B")).data!!
        val tagId3 = dm.createTag(CreateTagArgs(name = "C")).data!!
        val tagId4 = dm.createTag(CreateTagArgs(name = "D")).data!!
        val tagId5 = dm.createTag(CreateTagArgs(name = "E")).data!!
        val tagId6 = dm.createTag(CreateTagArgs(name = "F")).data!!
        val textToTranslateBeforeUpdate = "X"
        val textToTranslateAfterUpdate = "Y"
        val translationBeforeUpdate = "x"
        val translationAfterUpdate = "y"
        val createTime = testClock.currentMillis()
        val cardId = dm.createTranslateCard(CreateTranslateCardArgs(
            textToTranslate = textToTranslateBeforeUpdate, translation = translationBeforeUpdate, tagIds = setOf(tagId1, tagId2, tagId3, tagId4)
        )).data!!

        assertTableContent(repo = repo, table = t, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to cardId, t.textToTranslate to textToTranslateBeforeUpdate, t.translation to translationBeforeUpdate),
        ))
        assertTableContent(repo = repo, table = t.ver, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = s, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to cardId, s.updatedAt to createTime, s.delay to "0s"),
        ))
        assertTableContent(repo = repo, table = s.ver, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = ctg, exactMatch = true, expectedRows = listOf(
            listOf(ctg.cardId to cardId, ctg.tagId to tagId1),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId2),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId3),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId4),
        ))

        //when
        val updateTime = testClock.plus(4000)
        dm.updateTranslateCard(UpdateTranslateCardArgs(
            cardId = cardId,
            textToTranslate = textToTranslateAfterUpdate,
            translation = translationAfterUpdate,
            delay = "5m",
            tagIds = setOf(tagId5,tagId6,tagId3,tagId4)
        ))

        //then
        assertTableContent(repo = repo, table = t, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to cardId, t.textToTranslate to textToTranslateAfterUpdate, t.translation to translationAfterUpdate),
        ))
        assertTableContent(repo = repo, table = t.ver, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to cardId, t.ver.timestamp to updateTime, t.textToTranslate to textToTranslateBeforeUpdate, t.translation to translationBeforeUpdate),
        ))

        assertTableContent(repo = repo, table = s, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to cardId, s.updatedAt to updateTime, s.delay to "5m"),
        ))
        assertTableContent(repo = repo, table = s.ver, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to cardId, s.ver.timestamp to updateTime, s.cardId to cardId, s.updatedAt to createTime, s.delay to "0s"),
        ))

        assertTableContent(repo = repo, table = ctg, exactMatch = true, expectedRows = listOf(
            listOf(ctg.cardId to cardId, ctg.tagId to tagId5),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId6),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId3),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId4),
        ))
    }

    @Test
    fun validateTranslateCard_returns_expected_response_when_translation_is_correct() {
        //given
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
        val time1 = testClock.currentMillis()
        val actualResp = dm.validateTranslateCard(ValidateTranslateCardArgs(cardId = expectedCardId, userProvidedTranslation = "b"))

        //then
        val actualValidationResults = actualResp.data!!
        assertFalse(actualValidationResults.isCorrect)
        assertEquals("a", actualValidationResults.answer)
        assertTableContent(repo = repo, table = l, exactMatch = true, expectedRows = listOf(
            listOf(l.timestamp to time1, l.cardId to expectedCardId, l.translation to "b", l.matched to 0L)
        ))
    }

}