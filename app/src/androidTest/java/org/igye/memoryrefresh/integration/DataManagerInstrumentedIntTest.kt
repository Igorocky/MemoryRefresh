package org.igye.memoryrefresh.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.igye.memoryrefresh.common.Utils
import org.igye.memoryrefresh.common.Utils.MILLIS_IN_HOUR
import org.igye.memoryrefresh.common.Utils.MILLIS_IN_MINUTE
import org.igye.memoryrefresh.database.CardType
import org.igye.memoryrefresh.dto.domain.TranslateCard
import org.igye.memoryrefresh.manager.DataManager.*
import org.igye.memoryrefresh.testutils.InstrumentedTestBase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.temporal.ChronoUnit

@RunWith(AndroidJUnit4::class)
class DataManagerInstrumentedIntTest: InstrumentedTestBase() {

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("org.igye.memoryrefresh.dev", appContext.packageName)
    }

    @Test
    fun test_scenario_1_create_card_and_edit_it_twice() {
        //given
        val expectedTextToTranslate1 = "A"
        val expectedTranslation1 = "a"
        val expectedTextToTranslate2 = "B"
        val expectedTranslation2 = "b"

        //when: create a new translation card
        val timeCrt = testClock.currentMillis()
        val actualCreatedCardId = dm.createTranslateCard(
            CreateTranslateCardArgs(textToTranslate = expectedTextToTranslate1, translation = expectedTranslation1)
        ).data!!
        val actualCreatedCard = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = actualCreatedCardId)).data!!

        //then: a new card is created successfully
        assertEquals(expectedTextToTranslate1, actualCreatedCard.textToTranslate)
        assertEquals(expectedTranslation1, actualCreatedCard.translation)
        assertEquals("0s", actualCreatedCard.schedule.delay)
        assertEquals(0, actualCreatedCard.schedule.nextAccessInMillis)
        assertEquals(timeCrt, actualCreatedCard.schedule.nextAccessAt)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to actualCreatedCard.id, c.type to TR_TP, c.createdAt to timeCrt)
        ))
        assertTableContent(repo = repo, table = c.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to actualCreatedCard.id, t.textToTranslate to expectedTextToTranslate1, t.translation to expectedTranslation1)
        ))
        assertTableContent(repo = repo, table = t.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to actualCreatedCard.id, s.delay to "0s", s.nextAccessInMillis to 0L, s.nextAccessAt to timeCrt)
        ))
        assertTableContent(repo = repo, table = s.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = l, expectedRows = listOf())

        //when: edit the card but provide same values
        testClock.plus(5000)
        dm.updateTranslateCard(
            UpdateTranslateCardArgs(cardId = actualCreatedCard.id, textToTranslate = "$expectedTextToTranslate1  ", translation = "\t$expectedTranslation1")
        )
        val responseAfterEdit1 = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = actualCreatedCard.id))

        //then: the card stays in the same state - no actual edit was done
        val translateCardAfterEdit1: TranslateCard = responseAfterEdit1.data!!
        assertEquals(expectedTextToTranslate1, translateCardAfterEdit1.textToTranslate)
        assertEquals(expectedTranslation1, translateCardAfterEdit1.translation)
        assertEquals("0s", translateCardAfterEdit1.schedule.delay)
        assertEquals(0, translateCardAfterEdit1.schedule.nextAccessInMillis)
        assertEquals(timeCrt, translateCardAfterEdit1.schedule.nextAccessAt)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to translateCardAfterEdit1.id, c.type to TR_TP, c.createdAt to timeCrt)
        ))
        assertTableContent(repo = repo, table = c.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to translateCardAfterEdit1.id, t.textToTranslate to expectedTextToTranslate1, t.translation to expectedTranslation1)
        ))
        assertTableContent(repo = repo, table = t.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to translateCardAfterEdit1.id, s.delay to "0s", s.nextAccessInMillis to 0L, s.nextAccessAt to timeCrt)
        ))
        assertTableContent(repo = repo, table = s.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = l, expectedRows = listOf())

        //when: provide new values when editing the card
        val timeEdt2 = testClock.plus(5000)
        dm.updateTranslateCard(
            UpdateTranslateCardArgs(cardId = actualCreatedCard.id, textToTranslate = "  $expectedTextToTranslate2  ", translation = "\t$expectedTranslation2  ")
        )
        val responseAfterEdit2 = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = actualCreatedCard.id))

        //then: the values of card are updated and the previous version of the card is saved to the corresponding VER table
        val translateCardAfterEdit2: TranslateCard = responseAfterEdit2.data!!
        assertEquals(expectedTextToTranslate2, translateCardAfterEdit2.textToTranslate)
        assertEquals(expectedTranslation2, translateCardAfterEdit2.translation)
        assertEquals("0s", translateCardAfterEdit2.schedule.delay)
        assertEquals(0, translateCardAfterEdit2.schedule.nextAccessInMillis)
        assertEquals(timeCrt, translateCardAfterEdit2.schedule.nextAccessAt)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to translateCardAfterEdit2.id, c.type to TR_TP, c.createdAt to timeCrt)
        ))
        assertTableContent(repo = repo, table = c.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to translateCardAfterEdit2.id, t.textToTranslate to expectedTextToTranslate2, t.translation to expectedTranslation2)
        ))
        assertTableContent(repo = repo, table = t.ver, expectedRows = listOf(
            listOf(t.cardId to translateCardAfterEdit2.id, t.textToTranslate to expectedTextToTranslate1, t.translation to expectedTranslation1,
                t.ver.timestamp to timeEdt2)
        ))

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to translateCardAfterEdit2.id, s.delay to "0s", s.nextAccessInMillis to 0L, s.nextAccessAt to timeCrt)
        ))
        assertTableContent(repo = repo, table = s.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = l, expectedRows = listOf())
    }

    @Test
    fun test_scenario_2() {
        /*
        1. get next card when there are no cards at all

        2. create a new card1
        3. get next card (card1)
        4. get translate card by card1 id
        5. validate answer for card1 (a user provided correct answer)
        6. set delay for card1

        7. update translation for card1

        8. create a new card2
        9. get next card (card2)
        10. get translate card by card2 id
        11. validate answer for card2  (a user provided incorrect answer)
        12. set delay for card2

        13. get next card after small amount of time (no cards returned)

        14. update textToTranslate for card2

        15. get next card (card2)
        16. get translate card by card2 id
        17. validate answer for card2 (a user provided correct answer)
        18. set delay for card2

        19. get next card after small amount of time (no cards returned)
        20. get next card (card2)
        21. correct schedule for card1 (provide the same value, no change expected)
        22. get next card (card2)
        23. correct schedule for card1 (provide new value)
        24. get next card (card1)

        25. delete card1

        26. request history for card2
        * */

        //given
        assertTableContent(repo = repo, table = c, expectedRows = listOf())
        assertTableContent(repo = repo, table = c.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, expectedRows = listOf())
        assertTableContent(repo = repo, table = t.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = s, expectedRows = listOf())
        assertTableContent(repo = repo, table = s.ver, expectedRows = listOf())

        assertTableContent(repo = repo, table = l, expectedRows = listOf())

        //when: 1. get next card when there are no cards at all
        val resp1 = dm.selectTopOverdueTranslateCards().data!!

        //then: response contains empty "wait" time
        assertEquals(0, resp1.cards.size)
        assertTrue(resp1.nextCardIn.isEmpty())

        //when: 2. create a new card1
        val time2 = testClock.plus(1, ChronoUnit.MINUTES)
        val card1Id = dm.createTranslateCard(
            CreateTranslateCardArgs(textToTranslate = "karta1", translation = "card1")
        ).data!!
        val createCard1Resp = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = card1Id)).data!!

        //then
        assertEquals("karta1", createCard1Resp.textToTranslate)
        assertEquals("card1", createCard1Resp.translation)
        assertEquals("0s", createCard1Resp.schedule.delay)
        assertEquals(0, createCard1Resp.schedule.nextAccessInMillis)
        assertEquals(time2, createCard1Resp.schedule.nextAccessAt)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2, c.paused to 0, c.lastCheckedAt to time2)
        ))
        assertTableContent(repo = repo, table = c.ver, matchColumn = c.id, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1")
        ))
        assertTableContent(repo = repo, table = t.ver, matchColumn = t.cardId, expectedRows = listOf())

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time2, s.delay to "0s", s.nextAccessInMillis to 0L, s.nextAccessAt to time2)
        ))
        assertTableContent(repo = repo, table = s.ver, matchColumn = s.cardId, expectedRows = listOf())

        assertTableContent(repo = repo, table = l, matchColumn = l.cardId, expectedRows = listOf())

        //when: 3. get next card
        testClock.plus(1, ChronoUnit.MINUTES)
        val nextCardResp1 = dm.selectTopOverdueTranslateCards().data!!

        //then
        assertEquals(card1Id, nextCardResp1.cards[0].id)
        assertEquals(1, nextCardResp1.cards.size)

        //when: 4. get translate card by card1 id
        testClock.plus(1, ChronoUnit.SECONDS)
        val nextCard1Resp1 = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = card1Id)).data!!

        //then
        assertEquals(card1Id, nextCard1Resp1.id)
        assertEquals(CardType.TRANSLATION, nextCard1Resp1.type)
        assertEquals("karta1", nextCard1Resp1.textToTranslate)
        assertEquals("0s", nextCard1Resp1.schedule.delay)
        assertEquals(time2, nextCard1Resp1.schedule.updatedAt)

        //when: 5. validate answer for card1 (a user provided correct answer)
        val time5 = testClock.plus(1, ChronoUnit.MINUTES)
        val validateCard1Resp1 = dm.validateTranslateCard(ValidateTranslateCardArgs(cardId = card1Id, userProvidedTranslation = "card1")).data!!

        //then
        assertEquals("card1", validateCard1Resp1.answer)
        assertTrue(validateCard1Resp1.isCorrect)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2, c.paused to 0, c.lastCheckedAt to time5)
        ))
        assertTableContent(repo = repo, table = c.ver, matchColumn = c.id, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1")
        ))
        assertTableContent(repo = repo, table = t.ver, matchColumn = t.cardId, expectedRows = listOf())

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time2, s.delay to "0s", s.nextAccessInMillis to 0L, s.nextAccessAt to time2)
        ))
        assertTableContent(repo = repo, table = s.ver, matchColumn = s.cardId, expectedRows = listOf())

        assertTableContent(repo = repo, table = l, matchColumn = l.cardId, expectedRows = listOf(
            listOf(l.cardId to card1Id, l.timestamp to time5, l.translation to "card1", l.matched to 1)
        ))

        //when 6. set delay for card1
        val time6 = testClock.plus(1, ChronoUnit.MINUTES)
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = card1Id, recalculateDelay = true, delay = "1d"))
        val setDelayCard1Resp1 = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = card1Id)).data!!

        //then
        assertEquals(card1Id, card1Id)
        assertEquals("1d", setDelayCard1Resp1.schedule.delay)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2, c.paused to 0, c.lastCheckedAt to time5)
        ))
        assertTableContent(repo = repo, table = c.ver, matchColumn = c.id, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1")
        ))
        assertTableContent(repo = repo, table = t.ver, matchColumn = t.cardId, expectedRows = listOf())

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d")
        ))
        assertTableContent(repo = repo, table = s.ver, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0s")
        ))

        assertTableContent(repo = repo, table = l, matchColumn = l.cardId, expectedRows = listOf(
            listOf(l.cardId to card1Id, l.timestamp to time5, l.translation to "card1", l.matched to 1)
        ))

        //when: 7. update translation for card1
        val time7 = testClock.plus(1, ChronoUnit.MINUTES)
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = card1Id, translation = "card1+"))
        val updTranslationCard1Resp1 = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = card1Id)).data!!

        //then
        assertEquals(card1Id, updTranslationCard1Resp1.id)
        assertEquals("karta1", updTranslationCard1Resp1.textToTranslate)
        assertEquals("card1+", updTranslationCard1Resp1.translation)
        assertEquals("1d", updTranslationCard1Resp1.schedule.delay)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2, c.paused to 0, c.lastCheckedAt to time5)
        ))
        assertTableContent(repo = repo, table = c.ver, matchColumn = c.id, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+")
        ))
        assertTableContent(repo = repo, table = t.ver, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.ver.timestamp to time7, t.textToTranslate to "karta1", t.translation to "card1")
        ))

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d")
        ))
        assertTableContent(repo = repo, table = s.ver, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0s")
        ))

        assertTableContent(repo = repo, table = l, matchColumn = l.cardId, expectedRows = listOf(
            listOf(l.cardId to card1Id, l.timestamp to time5, l.translation to "card1", l.matched to 1)
        ))

        //when: 8. create a new card2
        val time8 = testClock.plus(1, ChronoUnit.MINUTES)
        val card2Id = dm.createTranslateCard(
            CreateTranslateCardArgs(textToTranslate = "karta2", translation = "card2")
        ).data!!
        val createCard2Resp = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = card2Id)).data!!

        //then
        assertEquals("karta2", createCard2Resp.textToTranslate)
        assertEquals("card2", createCard2Resp.translation)
        assertEquals("0s", createCard2Resp.schedule.delay)
        assertEquals(0, createCard2Resp.schedule.nextAccessInMillis)
        assertEquals(time8, createCard2Resp.schedule.nextAccessAt)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2, c.paused to 0, c.lastCheckedAt to time5),
            listOf(c.id to card2Id, c.type to TR_TP, c.createdAt to time8, c.paused to 0, c.lastCheckedAt to time8),
        ))
        assertTableContent(repo = repo, table = c.ver, matchColumn = c.id, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+"),
            listOf(t.cardId to card2Id, t.textToTranslate to "karta2", t.translation to "card2"),
        ))
        assertTableContent(repo = repo, table = t.ver, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.ver.timestamp to time7, t.textToTranslate to "karta1", t.translation to "card1"),
        ))

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d"),
            listOf(s.cardId to card2Id, s.updatedAt to time8, s.delay to "0s"),
        ))
        assertTableContent(repo = repo, table = s.ver, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0s"),
        ))

        assertTableContent(repo = repo, table = l, matchColumn = l.cardId, expectedRows = listOf(
            listOf(l.cardId to card1Id, l.timestamp to time5, l.translation to "card1", l.matched to 1),
        ))

        //when: 9. get next card (card2)
        testClock.plus(1, ChronoUnit.MINUTES)
        val nextCardResp2 = dm.selectTopOverdueTranslateCards().data!!

        //then
        assertEquals(card2Id, nextCardResp2.cards[0].id)
        assertEquals(1, nextCardResp2.cards.size)

        //when: 10. get translate card by card2 id
        testClock.plus(1, ChronoUnit.SECONDS)
        val nextCard2Resp1 = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = card2Id)).data!!

        //then
        assertEquals(card2Id, nextCard2Resp1.id)
        assertEquals(CardType.TRANSLATION, nextCard2Resp1.type)
        assertEquals("karta2", nextCard2Resp1.textToTranslate)
        assertEquals("0s", nextCard2Resp1.schedule.delay)
        assertEquals(time8, nextCard2Resp1.schedule.updatedAt)

        //when: 11. validate answer for card2  (a user provided incorrect answer)
        val time11 = testClock.plus(1, ChronoUnit.MINUTES)
        val validateCard2Resp1 = dm.validateTranslateCard(ValidateTranslateCardArgs(cardId = card2Id, userProvidedTranslation = "card2-inc")).data!!

        //then
        assertEquals("card2", validateCard2Resp1.answer)
        assertFalse(validateCard2Resp1.isCorrect)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2, c.paused to 0, c.lastCheckedAt to time5),
            listOf(c.id to card2Id, c.type to TR_TP, c.createdAt to time8, c.paused to 0, c.lastCheckedAt to time11),
        ))
        assertTableContent(repo = repo, table = c.ver, matchColumn = c.id, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+"),
            listOf(t.cardId to card2Id, t.textToTranslate to "karta2", t.translation to "card2"),
        ))
        assertTableContent(repo = repo, table = t.ver, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.ver.timestamp to time7, t.textToTranslate to "karta1", t.translation to "card1"),
        ))

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d"),
            listOf(s.cardId to card2Id, s.updatedAt to time8, s.delay to "0s"),
        ))
        assertTableContent(repo = repo, table = s.ver, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0s"),
        ))

        assertTableContent(repo = repo, table = l, matchColumn = l.cardId, expectedRows = listOf(
            listOf(l.cardId to card1Id, l.timestamp to time5, l.translation to "card1", l.matched to 1),
            listOf(l.cardId to card2Id, l.timestamp to time11, l.translation to "card2-inc", l.matched to 0),
        ))

        //when: 12. set delay for card2
        val time12 = testClock.plus(1, ChronoUnit.MINUTES)
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = card2Id, recalculateDelay = true, delay = "5m")).data!!
        val setDelayCard2Resp1 = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = card2Id)).data!!

        //then
        assertEquals(card2Id, setDelayCard2Resp1.id)
        assertEquals("5m", setDelayCard2Resp1.schedule.delay)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2, c.paused to 0, c.lastCheckedAt to time5),
            listOf(c.id to card2Id, c.type to TR_TP, c.createdAt to time8, c.paused to 0, c.lastCheckedAt to time11),
        ))
        assertTableContent(repo = repo, table = c.ver, matchColumn = c.id, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+"),
            listOf(t.cardId to card2Id, t.textToTranslate to "karta2", t.translation to "card2"),
        ))
        assertTableContent(repo = repo, table = t.ver, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.ver.timestamp to time7, t.textToTranslate to "karta1", t.translation to "card1"),
        ))

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d"),
            listOf(s.cardId to card2Id, s.updatedAt to time12, s.delay to "5m"),
        ))
        assertTableContent(repo = repo, table = s.ver, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0s"),
            listOf(s.ver.timestamp to time12, s.cardId to card2Id, s.updatedAt to time8, s.delay to "0s"),
        ))

        assertTableContent(repo = repo, table = l, matchColumn = l.cardId, expectedRows = listOf(
            listOf(l.cardId to card1Id, l.timestamp to time5, l.translation to "card1", l.matched to 1),
            listOf(l.cardId to card2Id, l.timestamp to time11, l.translation to "card2-inc", l.matched to 0),
        ))

        //when: 13. get next card after small amount of time (no cards returned)
        testClock.plus(1, ChronoUnit.MINUTES)
        val nextCardResp3 = dm.selectTopOverdueTranslateCards().data!!

        //then
        assertEquals(0, nextCardResp3.cards.size)
        assertTrue(setOf("3m","4m","5m",).contains(nextCardResp3.nextCardIn.split(" ")[0]))

        //when: 14. update textToTranslate for card2
        val time14 = testClock.plus(1, ChronoUnit.MINUTES)
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = card2Id, textToTranslate = "karta2+"))
        val updTextToTranslateCard2Resp1 = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = card2Id)).data!!

        //then
        assertEquals(card2Id, updTextToTranslateCard2Resp1.id)
        assertEquals("karta2+", updTextToTranslateCard2Resp1.textToTranslate)
        assertEquals("card2", updTextToTranslateCard2Resp1.translation)
        assertEquals("5m", updTextToTranslateCard2Resp1.schedule.delay)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2, c.paused to 0, c.lastCheckedAt to time5),
            listOf(c.id to card2Id, c.type to TR_TP, c.createdAt to time8, c.paused to 0, c.lastCheckedAt to time11),
        ))
        assertTableContent(repo = repo, table = c.ver, matchColumn = c.id, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+"),
            listOf(t.cardId to card2Id, t.textToTranslate to "karta2+", t.translation to "card2"),
        ))
        assertTableContent(repo = repo, table = t.ver, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.ver.timestamp to time7, t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1"),
            listOf(t.ver.timestamp to time14, t.cardId to card2Id, t.textToTranslate to "karta2", t.translation to "card2"),
        ))

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d"),
            listOf(s.cardId to card2Id, s.updatedAt to time12, s.delay to "5m"),
        ))
        assertTableContent(repo = repo, table = s.ver, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0s"),
            listOf(s.ver.timestamp to time12, s.cardId to card2Id, s.updatedAt to time8, s.delay to "0s"),
        ))

        assertTableContent(repo = repo, table = l, matchColumn = l.cardId, expectedRows = listOf(
            listOf(l.cardId to card1Id, l.timestamp to time5, l.translation to "card1", l.matched to 1),
            listOf(l.cardId to card2Id, l.timestamp to time11, l.translation to "card2-inc", l.matched to 0),
        ))

        //when: 15. get next card (card2)
        testClock.plus(5, ChronoUnit.MINUTES)
        val nextCardResp4 = dm.selectTopOverdueTranslateCards().data!!

        //then
        assertEquals(card2Id, nextCardResp4.cards[0].id)
        assertEquals(1, nextCardResp4.cards.size)

        //when: 16. get translate card by card2 id
        testClock.plus(1, ChronoUnit.SECONDS)
        val nextCard2Resp2 = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = card2Id)).data!!

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

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2, c.paused to 0, c.lastCheckedAt to time5),
            listOf(c.id to card2Id, c.type to TR_TP, c.createdAt to time8, c.paused to 0, c.lastCheckedAt to time17),
        ))
        assertTableContent(repo = repo, table = c.ver, matchColumn = c.id, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+"),
            listOf(t.cardId to card2Id, t.textToTranslate to "karta2+", t.translation to "card2"),
        ))
        assertTableContent(repo = repo, table = t.ver, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.ver.timestamp to time7, t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1"),
            listOf(t.ver.timestamp to time14, t.cardId to card2Id, t.textToTranslate to "karta2", t.translation to "card2"),
        ))

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d"),
            listOf(s.cardId to card2Id, s.updatedAt to time12, s.delay to "5m"),
        ))
        assertTableContent(repo = repo, table = s.ver, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0s"),
            listOf(s.ver.timestamp to time12, s.cardId to card2Id, s.updatedAt to time8, s.delay to "0s"),
        ))

        assertTableContent(repo = repo, table = l, matchColumn = l.cardId, expectedRows = listOf(
            listOf(l.cardId to card1Id, l.timestamp to time5, l.translation to "card1", l.matched to 1),
            listOf(l.cardId to card2Id, l.timestamp to time11, l.translation to "card2-inc", l.matched to 0),
            listOf(l.cardId to card2Id, l.timestamp to time17, l.translation to "card2", l.matched to 1),
        ))

        //when: 18. set delay for card2
        val time18 = testClock.plus(1, ChronoUnit.MINUTES)
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = card2Id, recalculateDelay = true, delay = "5m"))
        val setDelayCard2Resp2 = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = card2Id)).data!!

        //then
        assertEquals(card2Id, setDelayCard2Resp2.id)
        assertEquals("5m", setDelayCard2Resp2.schedule.delay)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2, c.paused to 0, c.lastCheckedAt to time5),
            listOf(c.id to card2Id, c.type to TR_TP, c.createdAt to time8, c.paused to 0, c.lastCheckedAt to time17),
        ))
        assertTableContent(repo = repo, table = c.ver, matchColumn = c.id, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+"),
            listOf(t.cardId to card2Id, t.textToTranslate to "karta2+", t.translation to "card2"),
        ))
        assertTableContent(repo = repo, table = t.ver, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.ver.timestamp to time7, t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1"),
            listOf(t.ver.timestamp to time14, t.cardId to card2Id, t.textToTranslate to "karta2", t.translation to "card2"),
        ))

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d"),
            listOf(s.cardId to card2Id, s.updatedAt to time18, s.delay to "5m"),
        ))
        assertTableContent(repo = repo, table = s.ver, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0s"),
            listOf(s.ver.timestamp to time12, s.cardId to card2Id, s.updatedAt to time8, s.delay to "0s"),
            listOf(s.ver.timestamp to time18, s.cardId to card2Id, s.updatedAt to time12, s.delay to "5m"),
        ))

        assertTableContent(repo = repo, table = l, matchColumn = l.cardId, expectedRows = listOf(
            listOf(l.cardId to card1Id, l.timestamp to time5, l.translation to "card1", l.matched to 1),
            listOf(l.cardId to card2Id, l.timestamp to time11, l.translation to "card2-inc", l.matched to 0),
            listOf(l.cardId to card2Id, l.timestamp to time17, l.translation to "card2", l.matched to 1),
        ))

        //when: 19. get next card after small amount of time (no cards returned)
        testClock.plus(1, ChronoUnit.SECONDS)
        val nextCardResp5 = dm.selectTopOverdueTranslateCards().data!!

        //then
        assertEquals(0, nextCardResp5.cards.size)
        assertTrue(setOf("3m","4m","5m",).contains(nextCardResp5.nextCardIn.split(" ")[0]))

        //when: 20. get next card (card2)
        testClock.plus(10, ChronoUnit.MINUTES)
        val nextCardResp6 = dm.selectTopOverdueTranslateCards().data!!

        //then
        assertEquals(card2Id, nextCardResp6.cards[0].id)
        assertEquals(1, nextCardResp6.cards.size)

        //when: 21. correct schedule for card1 (provide the same value, no change expected)
        val time21 = testClock.plus(10, ChronoUnit.SECONDS)
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = card1Id, delay = "1d"))
        val setDelayCard1Resp2 = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = card1Id)).data!!

        //then
        assertEquals(card1Id, setDelayCard1Resp2.id)
        assertEquals("1d", setDelayCard1Resp2.schedule.delay)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2, c.paused to 0, c.lastCheckedAt to time5),
            listOf(c.id to card2Id, c.type to TR_TP, c.createdAt to time8, c.paused to 0, c.lastCheckedAt to time17),
        ))
        assertTableContent(repo = repo, table = c.ver, matchColumn = c.id, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+"),
            listOf(t.cardId to card2Id, t.textToTranslate to "karta2+", t.translation to "card2"),
        ))
        assertTableContent(repo = repo, table = t.ver, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.ver.timestamp to time7, t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1"),
            listOf(t.ver.timestamp to time14, t.cardId to card2Id, t.textToTranslate to "karta2", t.translation to "card2"),
        ))

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d"),
            listOf(s.cardId to card2Id, s.updatedAt to time18, s.delay to "5m"),
        ))
        assertTableContent(repo = repo, table = s.ver, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0s"),
            listOf(s.ver.timestamp to time12, s.cardId to card2Id, s.updatedAt to time8, s.delay to "0s"),
            listOf(s.ver.timestamp to time18, s.cardId to card2Id, s.updatedAt to time12, s.delay to "5m"),
        ))

        assertTableContent(repo = repo, table = l, matchColumn = l.cardId, expectedRows = listOf(
            listOf(l.cardId to card1Id, l.timestamp to time5, l.translation to "card1", l.matched to 1),
            listOf(l.cardId to card2Id, l.timestamp to time11, l.translation to "card2-inc", l.matched to 0),
            listOf(l.cardId to card2Id, l.timestamp to time17, l.translation to "card2", l.matched to 1),
        ))

        //when: 22. get next card (card2)
        testClock.plus(10, ChronoUnit.SECONDS)
        val nextCardResp7 = dm.selectTopOverdueTranslateCards().data!!

        //then
        assertEquals(card2Id, nextCardResp7.cards[0].id)
        assertEquals(1, nextCardResp7.cards.size)

        //when: 23. correct schedule for card1 (provide new value)
        val time23 = testClock.plus(10, ChronoUnit.SECONDS)
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = card1Id, delay = "0s"))
        val setDelayCard1Resp3 = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = card1Id)).data!!

        //then
        assertEquals(card1Id, setDelayCard1Resp3.id)
        assertEquals("0s", setDelayCard1Resp3.schedule.delay)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2, c.paused to 0, c.lastCheckedAt to time5),
            listOf(c.id to card2Id, c.type to TR_TP, c.createdAt to time8, c.paused to 0, c.lastCheckedAt to time17),
        ))
        assertTableContent(repo = repo, table = c.ver, matchColumn = c.id, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+"),
            listOf(t.cardId to card2Id, t.textToTranslate to "karta2+", t.translation to "card2"),
        ))
        assertTableContent(repo = repo, table = t.ver, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.ver.timestamp to time7, t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1"),
            listOf(t.ver.timestamp to time14, t.cardId to card2Id, t.textToTranslate to "karta2", t.translation to "card2"),
        ))

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time23, s.delay to "0s"),
            listOf(s.cardId to card2Id, s.updatedAt to time18, s.delay to "5m"),
        ))
        assertTableContent(repo = repo, table = s.ver, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0s"),
            listOf(s.ver.timestamp to time23, s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d"),
            listOf(s.ver.timestamp to time12, s.cardId to card2Id, s.updatedAt to time8, s.delay to "0s"),
            listOf(s.ver.timestamp to time18, s.cardId to card2Id, s.updatedAt to time12, s.delay to "5m"),
        ))

        assertTableContent(repo = repo, table = l, matchColumn = l.cardId, expectedRows = listOf(
            listOf(l.cardId to card1Id, l.timestamp to time5, l.translation to "card1", l.matched to 1),
            listOf(l.cardId to card2Id, l.timestamp to time11, l.translation to "card2-inc", l.matched to 0),
            listOf(l.cardId to card2Id, l.timestamp to time17, l.translation to "card2", l.matched to 1),
        ))

        //when: 24. get next card (card1)
        testClock.plus(10, ChronoUnit.SECONDS)
        val nextCardResp8 = dm.selectTopOverdueTranslateCards().data!!

        //then
        assertEquals(card1Id, nextCardResp8.cards[0].id)
        assertEquals(2, nextCardResp8.cards.size)

        //when: 25. delete card1
        val time25 = testClock.plus(10, ChronoUnit.MINUTES)
        val deleteTranslateCard = dm.deleteTranslateCard(DeleteTranslateCardArgs(cardId = card1Id))
        val deleteCard1Resp = deleteTranslateCard.data!!

        //then
        assertTrue(deleteCard1Resp)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to card2Id, c.type to TR_TP, c.createdAt to time8, c.paused to 0, c.lastCheckedAt to time17),
        ))
        assertTableContent(repo = repo, table = c.ver, matchColumn = c.id, expectedRows = listOf(
            listOf(c.ver.timestamp to time25, c.id to card1Id, c.type to TR_TP, c.createdAt to time2),
        ))

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to card2Id, t.textToTranslate to "karta2+", t.translation to "card2"),
        ))
        assertTableContent(repo = repo, table = t.ver, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.ver.timestamp to time7, t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1"),
            listOf(t.ver.timestamp to time25, t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+"),
            listOf(t.ver.timestamp to time14, t.cardId to card2Id, t.textToTranslate to "karta2", t.translation to "card2"),
        ))

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to card2Id, s.updatedAt to time18, s.delay to "5m"),
        ))
        assertTableContent(repo = repo, table = s.ver, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0s"),
            listOf(s.ver.timestamp to time23, s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d"),
            listOf(s.ver.timestamp to time25, s.cardId to card1Id, s.updatedAt to time23, s.delay to "0s"),
            listOf(s.ver.timestamp to time12, s.cardId to card2Id, s.updatedAt to time8, s.delay to "0s"),
            listOf(s.ver.timestamp to time18, s.cardId to card2Id, s.updatedAt to time12, s.delay to "5m"),
        ))

        assertTableContent(repo = repo, table = l, matchColumn = l.cardId, expectedRows = listOf(
            listOf(l.cardId to card1Id, l.timestamp to time5, l.translation to "card1", l.matched to 1),
            listOf(l.cardId to card2Id, l.timestamp to time11, l.translation to "card2-inc", l.matched to 0),
            listOf(l.cardId to card2Id, l.timestamp to time17, l.translation to "card2", l.matched to 1),
        ))

        //when: 26. request history for card2
        val card2History = dm.readTranslateCardHistory(ReadTranslateCardHistoryArgs(cardId = card2Id)).data!!

        //then
        assertEquals(2, card2History.dataHistory.size)

        assertEquals(time14, card2History.dataHistory[0].timestamp)
        assertEquals("karta2+", card2History.dataHistory[0].textToTranslate)
        assertEquals("card2", card2History.dataHistory[0].translation)
        assertEquals(1, card2History.dataHistory[0].validationHistory.size)

        assertEquals(time17, card2History.dataHistory[0].validationHistory[0].timestamp)
        assertEquals("card2", card2History.dataHistory[0].validationHistory[0].translation)
        assertTrue(card2History.dataHistory[0].validationHistory[0].isCorrect)

        assertEquals(time8, card2History.dataHistory[1].timestamp)
        assertEquals("karta2", card2History.dataHistory[1].textToTranslate)
        assertEquals("card2", card2History.dataHistory[1].translation)
        assertEquals(1, card2History.dataHistory[1].validationHistory.size)

        assertEquals(time11, card2History.dataHistory[1].validationHistory[0].timestamp)
        assertEquals("card2-inc", card2History.dataHistory[1].validationHistory[0].translation)
        assertFalse(card2History.dataHistory[1].validationHistory[0].isCorrect)
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

    private fun recalculationOfDelayShuoldBeEvenlyDistributedInsideOfPlusMinusRange(
        delayStr: String, baseDurationMillis: Long, bucketWidthMillis: Long
    ) {
        //given
        init()
        val cardId = 12L
        insert(repo = repo, table = c, rows = listOf(
            listOf(c.id to cardId, c.type to TR_TP, c.createdAt to 0)
        ))
        insert(repo = repo, table = s, rows = listOf(
            listOf(s.cardId to cardId, s.updatedAt to 0, s.delay to "0s", s.randomFactor to 1.0, s.nextAccessInMillis to 0, s.nextAccessAt to 0)
        ))
        insert(repo = repo, table = t, rows = listOf(
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
            dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = cardId, delay = delayStr, recalculateDelay = true))
            val beRespose = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = cardId))
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

        val allSchedules = readAllDataFrom(repo, s.ver).filter { it[s.delay] != "0s" }
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