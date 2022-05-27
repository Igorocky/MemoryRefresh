package org.igye.memoryrefresh.unit.instrumentation

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.igye.memoryrefresh.common.Utils.MILLIS_IN_DAY
import org.igye.memoryrefresh.database.TranslationCardDirection.FOREIGN_NATIVE
import org.igye.memoryrefresh.database.TranslationCardDirection.NATIVE_FOREIGN
import org.igye.memoryrefresh.database.select
import org.igye.memoryrefresh.manager.DataManager.*
import org.igye.memoryrefresh.manager.SettingsManager
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
        val cardId = dm.createTranslateCard(CreateTranslateCardArgs(textToTranslate = "X", translation = "x", direction = FOREIGN_NATIVE)).data!!

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
            textToTranslate = "X", translation = "x", tagIds = setOf(tagId1, tagId2), direction = FOREIGN_NATIVE
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
            textToTranslate = "X", translation = "x", tagIds = setOf(tagId1, tagId2), direction = FOREIGN_NATIVE
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
            textToTranslate = "X", translation = "x", tagIds = setOf(tagId1, tagId2), direction = FOREIGN_NATIVE
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
            textToTranslate = "X", translation = "x", direction = FOREIGN_NATIVE
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
            textToTranslate = "X", translation = "x", tagIds = setOf(tagId1, tagId2, tagId3, tagId4), direction = FOREIGN_NATIVE
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
        val directionBeforeUpdate = NATIVE_FOREIGN
        val directionAfterUpdate = FOREIGN_NATIVE
        val createTime = testClock.currentMillis()
        val cardId = dm.createTranslateCard(CreateTranslateCardArgs(
            textToTranslate = textToTranslateBeforeUpdate,
            translation = translationBeforeUpdate,
            tagIds = setOf(tagId1, tagId2, tagId3, tagId4),
            paused = pausedBeforeUpdate,
            direction = directionBeforeUpdate,
        )).data!!

        assertTableContent(repo = repo, table = c, expectedRows = listOf(
            listOf(c.id to cardId, c.paused to (if (pausedBeforeUpdate) 1 else 0)),
        ))
        assertTableContent(repo = repo, table = c.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, expectedRows = listOf(
            listOf(t.cardId to cardId, t.textToTranslate to textToTranslateBeforeUpdate, t.translation to translationBeforeUpdate, t.direction to directionBeforeUpdate.intValue),
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
            paused = pausedAfterUpdate,
            direction = directionAfterUpdate,
        ))

        //then
        assertTableContent(repo = repo, table = c, expectedRows = listOf(
            listOf(c.id to cardId, c.paused to (if (pausedAfterUpdate) 1 else 0)),
        ))
        assertTableContent(repo = repo, table = c.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, expectedRows = listOf(
            listOf(t.cardId to cardId, t.textToTranslate to textToTranslateAfterUpdate, t.translation to translationAfterUpdate, t.direction to directionAfterUpdate.intValue),
        ))
        assertTableContent(repo = repo, table = t.ver, expectedRows = listOf(
            listOf(t.cardId to cardId, t.ver.timestamp to updateTime, t.textToTranslate to textToTranslateBeforeUpdate, t.translation to translationBeforeUpdate, t.direction to directionBeforeUpdate.intValue),
        ))

        assertTableContent(repo = repo, table = s, expectedRows = listOf(
            listOf(s.cardId to cardId, s.updatedAt to updateTime, s.origDelay to "5m", s.delay to "5m"),
        ))
        assertTableContent(repo = repo, table = s.ver, expectedRows = listOf(
            listOf(s.cardId to cardId, s.ver.timestamp to updateTime, s.cardId to cardId, s.updatedAt to createTime, s.origDelay to "1s", s.delay to "1s"),
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
        val createTime = testClock.currentMillis()
        val cardId = dm.createTranslateCard(CreateTranslateCardArgs(
            textToTranslate = textToTranslateBeforeUpdate,
            translation = translationBeforeUpdate,
            tagIds = setOf(tagId1, tagId2),
            paused = pausedBeforeUpdate,
            direction = FOREIGN_NATIVE
        )).data!!
        val preUpdateTime = testClock.plus(457465)
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = cardId, delay = delayBeforeUpdate));

        assertTableContent(repo = repo, table = c, expectedRows = listOf(
            listOf(c.id to cardId, c.paused to (if (pausedBeforeUpdate) 1 else 0)),
        ))
        assertTableContent(repo = repo, table = c.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = s, expectedRows = listOf(
            listOf(s.cardId to cardId, s.updatedAt to preUpdateTime, s.origDelay to delayBeforeUpdate, s.delay to delayBeforeUpdate),
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
            listOf(s.cardId to cardId, s.updatedAt to preUpdateTime, s.origDelay to delayBeforeUpdate, s.delay to delayBeforeUpdate),
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
    fun updateTranslateCard_updates_schedule_correctly_if_new_delay_was_specified_in_a_form_of_a_coefficient() {
        //given
        val createTime = testClock.currentMillis()
        val cardId = dm.createTranslateCard(CreateTranslateCardArgs(textToTranslate = "X", translation = "x", direction = FOREIGN_NATIVE)).data!!
        val preUpdateTime = testClock.plus(457465)
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = cardId, delay = "4d"))
        val rnd1 = repo.readableDatabase.select("select ${s.randomFactor} from $s where ${s.cardId} = $cardId") { it.getDouble() }.rows[0]

        assertTableContent(repo = repo, table = s, expectedRows = listOf(
            listOf(s.cardId to cardId, s.updatedAt to preUpdateTime, s.origDelay to "4d", s.delay to "4d",
                s.nextAccessInMillis to (rnd1*4*MILLIS_IN_DAY).toLong(), s.nextAccessAt to preUpdateTime+(rnd1*4*MILLIS_IN_DAY).toLong()),
        ))
        assertTableContent(repo = repo, table = s.ver, expectedRows = listOf(
            listOf(s.cardId to cardId, s.updatedAt to createTime, s.origDelay to "1s", s.delay to "1s",
                s.nextAccessInMillis to 1000, s.nextAccessAt to createTime+1000),
        ))

        //when
        val updateTime = testClock.plus(34676)
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = cardId, delay = "x2.5"))

        //then
        val rnd2 = repo.readableDatabase.select("select ${s.randomFactor} from $s where ${s.cardId} = $cardId") { it.getDouble() }.rows[0]
        val expectedDelayMillis = 10* MILLIS_IN_DAY
        assertTableContent(repo = repo, table = s, expectedRows = listOf(
            listOf(s.cardId to cardId, s.updatedAt to updateTime, s.randomFactor to rnd2,
                s.origDelay to "x2.5", s.delay to "10d",
                s.nextAccessInMillis to (expectedDelayMillis*rnd2).toLong(),
                s.nextAccessAt to updateTime+(expectedDelayMillis*rnd2).toLong()),
        ))
        assertTableContent(repo = repo, table = s.ver, expectedRows = listOf(
            listOf(s.cardId to cardId, s.updatedAt to createTime, s.origDelay to "1s", s.delay to "1s",
                s.nextAccessInMillis to 1000, s.nextAccessAt to createTime+1000),
            listOf(s.cardId to cardId, s.updatedAt to preUpdateTime, s.origDelay to "4d", s.delay to "4d",
                s.nextAccessInMillis to (rnd1*4* MILLIS_IN_DAY).toLong(), s.nextAccessAt to preUpdateTime+(rnd1*4* MILLIS_IN_DAY).toLong()),
        ))
    }

    @Test
    fun updateTranslateCard_doesnt_modify_paused_flag_if_it_is_not_specified_in_the_request() {
        //given
        val textToTranslateBeforeUpdate = "X"
        val textToTranslateAfterUpdate = "Y"
        val translationBeforeUpdate = "x"
        val cardId1 = dm.createTranslateCard(CreateTranslateCardArgs(
            textToTranslate = textToTranslateBeforeUpdate, translation = translationBeforeUpdate, paused = true, direction = FOREIGN_NATIVE
        )).data!!
        val cardId2 = dm.createTranslateCard(CreateTranslateCardArgs(
            textToTranslate = textToTranslateBeforeUpdate, translation = translationBeforeUpdate, paused = false, direction = FOREIGN_NATIVE
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
            textToTranslate = textToTranslateBeforeUpdate, translation = translationBeforeUpdate, paused = pausedBeforeUpdate, direction = FOREIGN_NATIVE
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
            textToTranslate = textToTranslateBeforeUpdate, translation = translationBeforeUpdate, paused = pausedBeforeUpdate, direction = FOREIGN_NATIVE
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
    fun updateTranslateCard_doesnt_set_delay_more_than_maximum_allowed() {
        //given
        val cardId = dm.createTranslateCard(CreateTranslateCardArgs(textToTranslate = "A", translation = "a", direction = FOREIGN_NATIVE)).data!!
        sm.updateMaxDelay(SettingsManager.UpdateMaxDelayArgs("300d"))

        val delay = "100d"
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = cardId, delay = delay, recalculateDelay = true))
        val translateCardAfterUpdate1 = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = cardId)).data
        val nextAccessInMillis1 = translateCardAfterUpdate1!!.schedule.nextAccessInMillis
        assertTrue(100*MILLIS_IN_DAY*0.85 <= nextAccessInMillis1 && nextAccessInMillis1 <= 100*MILLIS_IN_DAY*1.15)
        assertEquals("100d", translateCardAfterUpdate1.schedule.origDelay)

        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = cardId, delay = delay, recalculateDelay = true))
        val translateCardAfterUpdate2 = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = cardId)).data
        val nextAccessInMillis2 = translateCardAfterUpdate2!!.schedule.nextAccessInMillis
        assertTrue(100*MILLIS_IN_DAY*0.85 <= nextAccessInMillis2 && nextAccessInMillis2 <= 100*MILLIS_IN_DAY*1.15)
        assertEquals("100d", translateCardAfterUpdate2.schedule.origDelay)

        //when
        sm.updateMaxDelay(SettingsManager.UpdateMaxDelayArgs("30d"))
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = cardId, delay = delay, recalculateDelay = true))

        //then
        val translateCardWithAutoCorrectedDelay =
            dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = cardId)).data!!
        val nextAccessInMillis3 = translateCardWithAutoCorrectedDelay.schedule.nextAccessInMillis
        assertTrue(30*MILLIS_IN_DAY*0.9 <= nextAccessInMillis3 && nextAccessInMillis3 <= 30*MILLIS_IN_DAY)
        assertEquals("100d,max=30d", translateCardWithAutoCorrectedDelay.schedule.origDelay)
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
            listOf(t.cardId to expectedCardId, t.textToTranslate to "A", t.translation to " a\t", t.direction to NATIVE_FOREIGN),
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
            listOf(t.cardId to expectedCardId, t.textToTranslate to "A", t.translation to " a\t", t.direction to NATIVE_FOREIGN),
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

    @Test
    fun updateTranslateCard_doesnt_modify_direction_if_it_is_not_specified_in_the_request() {
        //given
        val textToTranslateBeforeUpdate = "X"
        val textToTranslateAfterUpdate = "Y"
        val translationBeforeUpdate = "x"
        val cardId1 = dm.createTranslateCard(CreateTranslateCardArgs(
            textToTranslate = textToTranslateBeforeUpdate, translation = translationBeforeUpdate, paused = true, direction = NATIVE_FOREIGN
        )).data!!
        val cardId2 = dm.createTranslateCard(CreateTranslateCardArgs(
            textToTranslate = textToTranslateBeforeUpdate, translation = translationBeforeUpdate, paused = false, direction = FOREIGN_NATIVE
        )).data!!

        assertTableContent(repo = repo, table = c, expectedRows = listOf(
            listOf(c.id to cardId1, c.paused to 1),
            listOf(c.id to cardId2, c.paused to 0),
        ))

        assertTableContent(repo = repo, table = t, expectedRows = listOf(
            listOf(t.cardId to cardId1, t.textToTranslate to textToTranslateBeforeUpdate, t.translation to translationBeforeUpdate, t.direction to NATIVE_FOREIGN),
            listOf(t.cardId to cardId2, t.textToTranslate to textToTranslateBeforeUpdate, t.translation to translationBeforeUpdate, t.direction to FOREIGN_NATIVE),
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
            listOf(t.cardId to cardId1, t.textToTranslate to textToTranslateAfterUpdate, t.translation to translationBeforeUpdate, t.direction to NATIVE_FOREIGN),
            listOf(t.cardId to cardId2, t.textToTranslate to textToTranslateAfterUpdate, t.translation to translationBeforeUpdate, t.direction to FOREIGN_NATIVE),
        ))
        assertTableContent(repo = repo, table = t.ver, expectedRows = listOf(
            listOf(t.cardId to cardId1, t.textToTranslate to textToTranslateBeforeUpdate, t.translation to translationBeforeUpdate, t.direction to NATIVE_FOREIGN),
            listOf(t.cardId to cardId2, t.textToTranslate to textToTranslateBeforeUpdate, t.translation to translationBeforeUpdate, t.direction to FOREIGN_NATIVE),
        ))
    }

    @Test
    fun updateTranslateCard_modifies_direction_from_NATIVE_FOREIGN_to_FOREIGN_NATIVE() {
        //given
        val textToTranslateBeforeUpdate = "X"
        val translationBeforeUpdate = "x"
        val directionBeforeUpdate = NATIVE_FOREIGN
        val directionAfterUpdate = FOREIGN_NATIVE
        val cardId = dm.createTranslateCard(CreateTranslateCardArgs(
            textToTranslate = textToTranslateBeforeUpdate, translation = translationBeforeUpdate, direction = directionBeforeUpdate
        )).data!!

        assertTableContent(repo = repo, table = t, expectedRows = listOf(
            listOf(t.cardId to cardId, t.direction to directionBeforeUpdate),
        ))

        //when
        dm.updateTranslateCard(UpdateTranslateCardArgs(
            cardId = cardId,
            direction = directionAfterUpdate
        ))

        //then
        assertTableContent(repo = repo, table = t, expectedRows = listOf(
            listOf(t.cardId to cardId, t.direction to directionAfterUpdate),
        ))
        assertTableContent(repo = repo, table = t.ver, expectedRows = listOf(
            listOf(t.cardId to cardId, t.direction to directionBeforeUpdate),
        ))
    }

    @Test
    fun updateTranslateCard_modifies_direction_from_FOREIGN_NATIVE_to_NATIVE_FOREIGN() {
        //given
        val textToTranslateBeforeUpdate = "X"
        val translationBeforeUpdate = "x"
        val directionBeforeUpdate = FOREIGN_NATIVE
        val directionAfterUpdate = NATIVE_FOREIGN
        val cardId = dm.createTranslateCard(CreateTranslateCardArgs(
            textToTranslate = textToTranslateBeforeUpdate, translation = translationBeforeUpdate, direction = directionBeforeUpdate
        )).data!!

        assertTableContent(repo = repo, table = t, expectedRows = listOf(
            listOf(t.cardId to cardId, t.direction to directionBeforeUpdate),
        ))

        //when
        dm.updateTranslateCard(UpdateTranslateCardArgs(
            cardId = cardId,
            direction = directionAfterUpdate
        ))

        //then
        assertTableContent(repo = repo, table = t, expectedRows = listOf(
            listOf(t.cardId to cardId, t.direction to directionAfterUpdate),
        ))
        assertTableContent(repo = repo, table = t.ver, expectedRows = listOf(
            listOf(t.cardId to cardId, t.direction to directionBeforeUpdate),
        ))
    }
}