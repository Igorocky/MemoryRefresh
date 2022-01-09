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

        assertTableContent(repo = repo, table = ctg, expectedRows = listOf())

        //when
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = cardId, tagIds = setOf(tagId1,tagId2,tagId3)))

        //then
        assertTableContent(repo = repo, table = ctg, expectedRows = listOf(
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

        assertTableContent(repo = repo, table = ctg, expectedRows = listOf(
            listOf(ctg.cardId to cardId, ctg.tagId to tagId1),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId2),
        ))

        //when
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = cardId, tagIds = setOf(tagId1,tagId2,tagId3,tagId4)))

        //then
        assertTableContent(repo = repo, table = ctg, expectedRows = listOf(
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

        assertTableContent(repo = repo, table = ctg, expectedRows = listOf(
            listOf(ctg.cardId to cardId, ctg.tagId to tagId1),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId2),
        ))

        //when
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = cardId, tagIds = emptySet()))

        //then
        assertTableContent(repo = repo, table = ctg, expectedRows = listOf())
    }

    @Test
    fun updateTranslateCard_doesnt_modify_tags_if_tagIds_is_null_in_the_request() {
        //given
        val tagId1 = dm.createTag(CreateTagArgs(name = "A")).data!!
        val tagId2 = dm.createTag(CreateTagArgs(name = "B")).data!!
        val cardId = dm.createTranslateCard(CreateTranslateCardArgs(
            textToTranslate = "X", translation = "x", tagIds = setOf(tagId1, tagId2)
        )).data!!

        assertTableContent(repo = repo, table = ctg, expectedRows = listOf(
            listOf(ctg.cardId to cardId, ctg.tagId to tagId1),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId2),
        ))

        //when
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = cardId, tagIds = null))

        //then
        assertTableContent(repo = repo, table = ctg, expectedRows = listOf(
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

        assertTableContent(repo = repo, table = ctg, expectedRows = listOf())

        //when
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = cardId, tagIds = emptySet()))

        //then
        assertTableContent(repo = repo, table = ctg, expectedRows = listOf())
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

        assertTableContent(repo = repo, table = ctg, expectedRows = listOf(
            listOf(ctg.cardId to cardId, ctg.tagId to tagId1),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId2),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId3),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId4),
        ))

        //when
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = cardId, tagIds = setOf(tagId5,tagId6,tagId3,tagId4)))

        //then
        assertTableContent(repo = repo, table = ctg, expectedRows = listOf(
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
        val pausedBeforeUpdate = false
        val pausedAfterUpdate = true
        val createTime = testClock.currentMillis()
        val cardId = dm.createTranslateCard(CreateTranslateCardArgs(
            textToTranslate = textToTranslateBeforeUpdate, translation = translationBeforeUpdate, tagIds = setOf(tagId1, tagId2, tagId3, tagId4), paused = pausedBeforeUpdate
        )).data!!

        assertTableContent(repo = repo, table = c, expectedRows = listOf(
            listOf(c.id to cardId, c.paused to (if (pausedBeforeUpdate) 1 else 0)),
        ))
        assertTableContent(repo = repo, table = c.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, expectedRows = listOf(
            listOf(t.cardId to cardId, t.textToTranslate to textToTranslateBeforeUpdate, t.translation to translationBeforeUpdate),
        ))
        assertTableContent(repo = repo, table = t.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = s, expectedRows = listOf(
            listOf(s.cardId to cardId, s.updatedAt to createTime, s.delay to "1s"),
        ))
        assertTableContent(repo = repo, table = s.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = ctg, expectedRows = listOf(
            listOf(ctg.cardId to cardId, ctg.tagId to tagId1),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId2),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId3),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId4),
        ))

        //when
        val updateTime = testClock.plus(4000)
        assertTrue(pausedBeforeUpdate != pausedAfterUpdate)
        dm.updateTranslateCard(UpdateTranslateCardArgs(
            cardId = cardId,
            textToTranslate = textToTranslateAfterUpdate,
            translation = translationAfterUpdate,
            delay = "5m",
            tagIds = setOf(tagId5,tagId6,tagId3,tagId4),
            paused = pausedAfterUpdate
        ))

        //then
        assertTableContent(repo = repo, table = c, expectedRows = listOf(
            listOf(c.id to cardId, c.paused to (if (pausedAfterUpdate) 1 else 0)),
        ))
        assertTableContent(repo = repo, table = c.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, expectedRows = listOf(
            listOf(t.cardId to cardId, t.textToTranslate to textToTranslateAfterUpdate, t.translation to translationAfterUpdate),
        ))
        assertTableContent(repo = repo, table = t.ver, expectedRows = listOf(
            listOf(t.cardId to cardId, t.ver.timestamp to updateTime, t.textToTranslate to textToTranslateBeforeUpdate, t.translation to translationBeforeUpdate),
        ))

        assertTableContent(repo = repo, table = s, expectedRows = listOf(
            listOf(s.cardId to cardId, s.updatedAt to updateTime, s.delay to "5m"),
        ))
        assertTableContent(repo = repo, table = s.ver, expectedRows = listOf(
            listOf(s.cardId to cardId, s.ver.timestamp to updateTime, s.cardId to cardId, s.updatedAt to createTime, s.delay to "1s"),
        ))

        assertTableContent(repo = repo, table = ctg, expectedRows = listOf(
            listOf(ctg.cardId to cardId, ctg.tagId to tagId5),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId6),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId3),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId4),
        ))
    }

    @Test
    fun updateTranslateCard_returns_an_error_and_doesnt_update_parameters_if_incorrect_delay_was_specified() {
        //given
        val tagId1 = dm.createTag(CreateTagArgs(name = "A")).data!!
        val tagId2 = dm.createTag(CreateTagArgs(name = "B")).data!!
        val tagId3 = dm.createTag(CreateTagArgs(name = "C")).data!!
        val textToTranslateBeforeUpdate = "X"
        val textToTranslateAfterUpdate = "Y"
        val translationBeforeUpdate = "x"
        val translationAfterUpdate = "y"
        val pausedBeforeUpdate = false
        val pausedAfterUpdate = true
        val delayBeforeUpdate = "5m"
        val delayAfterUpdate = "77"
        val createTime = testClock.currentMillis()
        val cardId = dm.createTranslateCard(CreateTranslateCardArgs(
            textToTranslate = textToTranslateBeforeUpdate, translation = translationBeforeUpdate, tagIds = setOf(tagId1, tagId2), paused = pausedBeforeUpdate
        )).data!!
        val preUpdateTime = testClock.plus(457465)
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = cardId, delay = delayBeforeUpdate));

        assertTableContent(repo = repo, table = c, expectedRows = listOf(
            listOf(c.id to cardId, c.paused to (if (pausedBeforeUpdate) 1 else 0)),
        ))
        assertTableContent(repo = repo, table = c.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = s, expectedRows = listOf(
            listOf(s.cardId to cardId, s.updatedAt to preUpdateTime, s.delay to delayBeforeUpdate),
        ))
        assertTableContent(repo = repo, table = s.ver, expectedRows = listOf(
            listOf(s.cardId to cardId, s.updatedAt to createTime, s.delay to "1s"),
        ))

        assertTableContent(repo = repo, table = t, expectedRows = listOf(
            listOf(t.cardId to cardId, t.textToTranslate to textToTranslateBeforeUpdate, t.translation to translationBeforeUpdate),
        ))
        assertTableContent(repo = repo, table = t.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = ctg, expectedRows = listOf(
            listOf(ctg.cardId to cardId, ctg.tagId to tagId1),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId2),
        ))

        //when
        val updateTime = testClock.plus(4000)
        val err = dm.updateTranslateCard(
            UpdateTranslateCardArgs(
                cardId = cardId,
                textToTranslate = textToTranslateAfterUpdate,
                translation = translationAfterUpdate,
                delay = "123",
                tagIds = setOf(tagId2, tagId3),
                paused = pausedAfterUpdate
            )
        ).err!!

        //then
        assertEquals(17, err.code)
        assertEquals("Pause duration '123' is in incorrect format.", err.msg)

        assertTableContent(repo = repo, table = c, expectedRows = listOf(
            listOf(c.id to cardId, c.paused to (if (pausedBeforeUpdate) 1 else 0)),
        ))
        assertTableContent(repo = repo, table = c.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = s, expectedRows = listOf(
            listOf(s.cardId to cardId, s.updatedAt to preUpdateTime, s.delay to delayBeforeUpdate),
        ))
        assertTableContent(repo = repo, table = s.ver, expectedRows = listOf(
            listOf(s.cardId to cardId, s.updatedAt to createTime, s.delay to "1s"),
        ))

        assertTableContent(repo = repo, table = t, expectedRows = listOf(
            listOf(t.cardId to cardId, t.textToTranslate to textToTranslateBeforeUpdate, t.translation to translationBeforeUpdate),
        ))
        assertTableContent(repo = repo, table = t.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = ctg, expectedRows = listOf(
            listOf(ctg.cardId to cardId, ctg.tagId to tagId1),
            listOf(ctg.cardId to cardId, ctg.tagId to tagId2),
        ))
    }

    @Test
    fun updateTranslateCard_doesnt_modify_paused_flag_if_it_is_not_specified_in_the_request() {
        //given
        val textToTranslateBeforeUpdate = "X"
        val textToTranslateAfterUpdate = "Y"
        val translationBeforeUpdate = "x"
        val cardId1 = dm.createTranslateCard(CreateTranslateCardArgs(
            textToTranslate = textToTranslateBeforeUpdate, translation = translationBeforeUpdate, paused = true
        )).data!!
        val cardId2 = dm.createTranslateCard(CreateTranslateCardArgs(
            textToTranslate = textToTranslateBeforeUpdate, translation = translationBeforeUpdate, paused = false
        )).data!!

        assertTableContent(repo = repo, table = c, expectedRows = listOf(
            listOf(c.id to cardId1, c.paused to 1),
            listOf(c.id to cardId2, c.paused to 0),
        ))

        assertTableContent(repo = repo, table = t, expectedRows = listOf(
            listOf(t.cardId to cardId1, t.textToTranslate to textToTranslateBeforeUpdate, t.translation to translationBeforeUpdate),
            listOf(t.cardId to cardId2, t.textToTranslate to textToTranslateBeforeUpdate, t.translation to translationBeforeUpdate),
        ))
        assertTableContent(repo = repo, table = t.ver, expectedRows = listOf())

        //when
        dm.updateTranslateCard(UpdateTranslateCardArgs(
            cardId = cardId1,
            textToTranslate = textToTranslateAfterUpdate
        ))
        dm.updateTranslateCard(UpdateTranslateCardArgs(
            cardId = cardId2,
            textToTranslate = textToTranslateAfterUpdate
        ))

        //then
        assertTableContent(repo = repo, table = c, expectedRows = listOf(
            listOf(c.id to cardId1, c.paused to 1),
            listOf(c.id to cardId2, c.paused to 0),
        ))
        assertTableContent(repo = repo, table = c.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, expectedRows = listOf(
            listOf(t.cardId to cardId1, t.textToTranslate to textToTranslateAfterUpdate, t.translation to translationBeforeUpdate),
            listOf(t.cardId to cardId2, t.textToTranslate to textToTranslateAfterUpdate, t.translation to translationBeforeUpdate),
        ))
        assertTableContent(repo = repo, table = t.ver, expectedRows = listOf(
            listOf(t.cardId to cardId1, t.textToTranslate to textToTranslateBeforeUpdate, t.translation to translationBeforeUpdate),
            listOf(t.cardId to cardId2, t.textToTranslate to textToTranslateBeforeUpdate, t.translation to translationBeforeUpdate),
        ))
    }

    @Test
    fun updateTranslateCard_modifies_paused_flag_from_false_to_true() {
        //given
        val textToTranslateBeforeUpdate = "X"
        val translationBeforeUpdate = "x"
        val pausedBeforeUpdate = false
        val pausedAfterUpdate = true
        val cardId = dm.createTranslateCard(CreateTranslateCardArgs(
            textToTranslate = textToTranslateBeforeUpdate, translation = translationBeforeUpdate, paused = pausedBeforeUpdate
        )).data!!

        assertTableContent(repo = repo, table = c, expectedRows = listOf(
            listOf(c.id to cardId, c.paused to 0),
        ))

        //when
        dm.updateTranslateCard(UpdateTranslateCardArgs(
            cardId = cardId,
            paused = pausedAfterUpdate
        ))

        //then
        assertTableContent(repo = repo, table = c, expectedRows = listOf(
            listOf(c.id to cardId, c.paused to 1),
        ))
        assertTableContent(repo = repo, table = c.ver, expectedRows = listOf())

    }

    @Test
    fun updateTranslateCard_modifies_paused_flag_from_true_to_false() {
        //given
        val textToTranslateBeforeUpdate = "X"
        val translationBeforeUpdate = "x"
        val pausedBeforeUpdate = true
        val pausedAfterUpdate = false
        val cardId = dm.createTranslateCard(CreateTranslateCardArgs(
            textToTranslate = textToTranslateBeforeUpdate, translation = translationBeforeUpdate, paused = pausedBeforeUpdate
        )).data!!

        assertTableContent(repo = repo, table = c, expectedRows = listOf(
            listOf(c.id to cardId, c.paused to 1),
        ))

        //when
        dm.updateTranslateCard(UpdateTranslateCardArgs(
            cardId = cardId,
            paused = pausedAfterUpdate
        ))

        //then
        assertTableContent(repo = repo, table = c, expectedRows = listOf(
            listOf(c.id to cardId, c.paused to 0),
        ))
        assertTableContent(repo = repo, table = c.ver, expectedRows = listOf())
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
        assertTableContent(repo = repo, table = l, expectedRows = listOf())

        //when
        testClock.setFixedTime(baseTime)
        val time1 = testClock.instant().toEpochMilli()
        val actualResp = dm.validateTranslateCard(ValidateTranslateCardArgs(cardId = expectedCardId, userProvidedTranslation = "\ta   "))

        //then
        val actualValidationResults = actualResp.data!!
        assertTrue(actualValidationResults.isCorrect)
        assertEquals("a", actualValidationResults.answer)
        assertTableContent(repo = repo, table = l, expectedRows = listOf(
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
        assertTableContent(repo = repo, table = l, expectedRows = listOf())

        //when
        testClock.setFixedTime(baseTime)
        val time1 = testClock.currentMillis()
        val actualResp = dm.validateTranslateCard(ValidateTranslateCardArgs(cardId = expectedCardId, userProvidedTranslation = "b"))

        //then
        val actualValidationResults = actualResp.data!!
        assertFalse(actualValidationResults.isCorrect)
        assertEquals("a", actualValidationResults.answer)
        assertTableContent(repo = repo, table = l, expectedRows = listOf(
            listOf(l.timestamp to time1, l.cardId to expectedCardId, l.translation to "b", l.matched to 0L)
        ))
    }

}