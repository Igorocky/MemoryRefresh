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
        assertEquals("0m", actualCreatedCard.schedule.delay)
        assertEquals(0, actualCreatedCard.schedule.nextAccessInMillis)
        assertEquals(timeCrt, actualCreatedCard.schedule.nextAccessAt)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to actualCreatedCard.id, c.type to TR_TP, c.createdAt to timeCrt)
        ))
        assertTableContent(repo = repo, table = c.ver, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to actualCreatedCard.id, t.textToTranslate to expectedTextToTranslate1, t.translation to expectedTranslation1)
        ))
        assertTableContent(repo = repo, table = t.ver, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to actualCreatedCard.id, s.delay to "0m", s.nextAccessInMillis to 0L, s.nextAccessAt to timeCrt)
        ))
        assertTableContent(repo = repo, table = s.ver, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = l, exactMatch = true, expectedRows = listOf())

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
        assertEquals("0m", translateCardAfterEdit1.schedule.delay)
        assertEquals(0, translateCardAfterEdit1.schedule.nextAccessInMillis)
        assertEquals(timeCrt, translateCardAfterEdit1.schedule.nextAccessAt)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to translateCardAfterEdit1.id, c.type to TR_TP, c.createdAt to timeCrt)
        ))
        assertTableContent(repo = repo, table = c.ver, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to translateCardAfterEdit1.id, t.textToTranslate to expectedTextToTranslate1, t.translation to expectedTranslation1)
        ))
        assertTableContent(repo = repo, table = t.ver, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to translateCardAfterEdit1.id, s.delay to "0m", s.nextAccessInMillis to 0L, s.nextAccessAt to timeCrt)
        ))
        assertTableContent(repo = repo, table = s.ver, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = l, exactMatch = true, expectedRows = listOf())

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
        assertEquals("0m", translateCardAfterEdit2.schedule.delay)
        assertEquals(0, translateCardAfterEdit2.schedule.nextAccessInMillis)
        assertEquals(timeCrt, translateCardAfterEdit2.schedule.nextAccessAt)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to translateCardAfterEdit2.id, c.type to TR_TP, c.createdAt to timeCrt)
        ))
        assertTableContent(repo = repo, table = c.ver, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to translateCardAfterEdit2.id, t.textToTranslate to expectedTextToTranslate2, t.translation to expectedTranslation2)
        ))
        assertTableContent(repo = repo, table = t.ver, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to translateCardAfterEdit2.id, t.textToTranslate to expectedTextToTranslate1, t.translation to expectedTranslation1,
                t.ver.timestamp to timeEdt2)
        ))

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to translateCardAfterEdit2.id, s.delay to "0m", s.nextAccessInMillis to 0L, s.nextAccessAt to timeCrt)
        ))
        assertTableContent(repo = repo, table = s.ver, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = l, exactMatch = true, expectedRows = listOf())
    }

    @Test
    fun test_scenario_2() {
        //given
        assertTableContent(repo = repo, table = c, exactMatch = true, expectedRows = listOf())
        assertTableContent(repo = repo, table = c.ver, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, exactMatch = true, expectedRows = listOf())
        assertTableContent(repo = repo, table = t.ver, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = s, exactMatch = true, expectedRows = listOf())
        assertTableContent(repo = repo, table = s.ver, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = l, exactMatch = true, expectedRows = listOf())

        //when: 1. get next card when there are no cards at all
        val resp1 = dm.getNextCardToRepeat().data!!

        //then: response contains empty "wait" time
        assertEquals(0, resp1.cardsRemain)
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
        assertEquals("0m", createCard1Resp.schedule.delay)
        assertEquals(0, createCard1Resp.schedule.nextAccessInMillis)
        assertEquals(time2, createCard1Resp.schedule.nextAccessAt)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2)
        ))
        assertTableContent(repo = repo, table = c.ver, matchColumn = c.id, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1")
        ))
        assertTableContent(repo = repo, table = t.ver, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time2, s.delay to "0m", s.nextAccessInMillis to 0L, s.nextAccessAt to time2)
        ))
        assertTableContent(repo = repo, table = s.ver, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = l, matchColumn = l.cardId, exactMatch = true, expectedRows = listOf())

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
        val nextCard1Resp1 = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = card1Id)).data!!

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

        assertTableContent(repo = repo, table = c, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2)
        ))
        assertTableContent(repo = repo, table = c.ver, matchColumn = c.id, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1")
        ))
        assertTableContent(repo = repo, table = t.ver, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time2, s.delay to "0m", s.nextAccessInMillis to 0L, s.nextAccessAt to time2)
        ))
        assertTableContent(repo = repo, table = s.ver, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = l, matchColumn = l.cardId, exactMatch = true, expectedRows = listOf(
            listOf(l.cardId to card1Id, l.timestamp to time5, l.translation to "card1", l.matched to 1)
        ))

        //when 6. set delay for card1
        val time6 = testClock.plus(1, ChronoUnit.MINUTES)
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = card1Id, recalculateDelay = true, delay = "1d"))
        val setDelayCard1Resp1 = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = card1Id)).data!!

        //then
        assertEquals(card1Id, card1Id)
        assertEquals("1d", setDelayCard1Resp1.schedule.delay)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2)
        ))
        assertTableContent(repo = repo, table = c.ver, matchColumn = c.id, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1")
        ))
        assertTableContent(repo = repo, table = t.ver, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d")
        ))
        assertTableContent(repo = repo, table = s.ver, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0m")
        ))

        assertTableContent(repo = repo, table = l, matchColumn = l.cardId, exactMatch = true, expectedRows = listOf(
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

        assertTableContent(repo = repo, table = c, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2)
        ))
        assertTableContent(repo = repo, table = c.ver, matchColumn = c.id, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+")
        ))
        assertTableContent(repo = repo, table = t.ver, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.ver.timestamp to time7, t.textToTranslate to "karta1", t.translation to "card1")
        ))

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d")
        ))
        assertTableContent(repo = repo, table = s.ver, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0m")
        ))

        assertTableContent(repo = repo, table = l, matchColumn = l.cardId, exactMatch = true, expectedRows = listOf(
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
        assertEquals("0m", createCard2Resp.schedule.delay)
        assertEquals(0, createCard2Resp.schedule.nextAccessInMillis)
        assertEquals(time8, createCard2Resp.schedule.nextAccessAt)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2),
            listOf(c.id to card2Id, c.type to TR_TP, c.createdAt to time8),
        ))
        assertTableContent(repo = repo, table = c.ver, matchColumn = c.id, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+"),
            listOf(t.cardId to card2Id, t.textToTranslate to "karta2", t.translation to "card2"),
        ))
        assertTableContent(repo = repo, table = t.ver, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.ver.timestamp to time7, t.textToTranslate to "karta1", t.translation to "card1"),
        ))

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d"),
            listOf(s.cardId to card2Id, s.updatedAt to time8, s.delay to "0m"),
        ))
        assertTableContent(repo = repo, table = s.ver, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0m"),
        ))

        assertTableContent(repo = repo, table = l, matchColumn = l.cardId, exactMatch = true, expectedRows = listOf(
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
        val nextCard2Resp1 = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = card2Id)).data!!

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

        assertTableContent(repo = repo, table = c, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2),
            listOf(c.id to card2Id, c.type to TR_TP, c.createdAt to time8),
        ))
        assertTableContent(repo = repo, table = c.ver, matchColumn = c.id, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+"),
            listOf(t.cardId to card2Id, t.textToTranslate to "karta2", t.translation to "card2"),
        ))
        assertTableContent(repo = repo, table = t.ver, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.ver.timestamp to time7, t.textToTranslate to "karta1", t.translation to "card1"),
        ))

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d"),
            listOf(s.cardId to card2Id, s.updatedAt to time8, s.delay to "0m"),
        ))
        assertTableContent(repo = repo, table = s.ver, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0m"),
        ))

        assertTableContent(repo = repo, table = l, matchColumn = l.cardId, exactMatch = true, expectedRows = listOf(
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

        assertTableContent(repo = repo, table = c, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2),
            listOf(c.id to card2Id, c.type to TR_TP, c.createdAt to time8),
        ))
        assertTableContent(repo = repo, table = c.ver, matchColumn = c.id, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+"),
            listOf(t.cardId to card2Id, t.textToTranslate to "karta2", t.translation to "card2"),
        ))
        assertTableContent(repo = repo, table = t.ver, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.ver.timestamp to time7, t.textToTranslate to "karta1", t.translation to "card1"),
        ))

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d"),
            listOf(s.cardId to card2Id, s.updatedAt to time12, s.delay to "5m"),
        ))
        assertTableContent(repo = repo, table = s.ver, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0m"),
            listOf(s.ver.timestamp to time12, s.cardId to card2Id, s.updatedAt to time8, s.delay to "0m"),
        ))

        assertTableContent(repo = repo, table = l, matchColumn = l.cardId, exactMatch = true, expectedRows = listOf(
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
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = card2Id, textToTranslate = "karta2+"))
        val updTextToTranslateCard2Resp1 = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = card2Id)).data!!

        //then
        assertEquals(card2Id, updTextToTranslateCard2Resp1.id)
        assertEquals("karta2+", updTextToTranslateCard2Resp1.textToTranslate)
        assertEquals("card2", updTextToTranslateCard2Resp1.translation)
        assertEquals("5m", updTextToTranslateCard2Resp1.schedule.delay)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2),
            listOf(c.id to card2Id, c.type to TR_TP, c.createdAt to time8),
        ))
        assertTableContent(repo = repo, table = c.ver, matchColumn = c.id, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+"),
            listOf(t.cardId to card2Id, t.textToTranslate to "karta2+", t.translation to "card2"),
        ))
        assertTableContent(repo = repo, table = t.ver, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.ver.timestamp to time7, t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1"),
            listOf(t.ver.timestamp to time14, t.cardId to card2Id, t.textToTranslate to "karta2", t.translation to "card2"),
        ))

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d"),
            listOf(s.cardId to card2Id, s.updatedAt to time12, s.delay to "5m"),
        ))
        assertTableContent(repo = repo, table = s.ver, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0m"),
            listOf(s.ver.timestamp to time12, s.cardId to card2Id, s.updatedAt to time8, s.delay to "0m"),
        ))

        assertTableContent(repo = repo, table = l, matchColumn = l.cardId, exactMatch = true, expectedRows = listOf(
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

        assertTableContent(repo = repo, table = c, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2),
            listOf(c.id to card2Id, c.type to TR_TP, c.createdAt to time8),
        ))
        assertTableContent(repo = repo, table = c.ver, matchColumn = c.id, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+"),
            listOf(t.cardId to card2Id, t.textToTranslate to "karta2+", t.translation to "card2"),
        ))
        assertTableContent(repo = repo, table = t.ver, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.ver.timestamp to time7, t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1"),
            listOf(t.ver.timestamp to time14, t.cardId to card2Id, t.textToTranslate to "karta2", t.translation to "card2"),
        ))

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d"),
            listOf(s.cardId to card2Id, s.updatedAt to time12, s.delay to "5m"),
        ))
        assertTableContent(repo = repo, table = s.ver, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0m"),
            listOf(s.ver.timestamp to time12, s.cardId to card2Id, s.updatedAt to time8, s.delay to "0m"),
        ))

        assertTableContent(repo = repo, table = l, matchColumn = l.cardId, exactMatch = true, expectedRows = listOf(
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

        assertTableContent(repo = repo, table = c, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2),
            listOf(c.id to card2Id, c.type to TR_TP, c.createdAt to time8),
        ))
        assertTableContent(repo = repo, table = c.ver, matchColumn = c.id, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+"),
            listOf(t.cardId to card2Id, t.textToTranslate to "karta2+", t.translation to "card2"),
        ))
        assertTableContent(repo = repo, table = t.ver, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.ver.timestamp to time7, t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1"),
            listOf(t.ver.timestamp to time14, t.cardId to card2Id, t.textToTranslate to "karta2", t.translation to "card2"),
        ))

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d"),
            listOf(s.cardId to card2Id, s.updatedAt to time18, s.delay to "5m"),
        ))
        assertTableContent(repo = repo, table = s.ver, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0m"),
            listOf(s.ver.timestamp to time12, s.cardId to card2Id, s.updatedAt to time8, s.delay to "0m"),
            listOf(s.ver.timestamp to time18, s.cardId to card2Id, s.updatedAt to time12, s.delay to "5m"),
        ))

        assertTableContent(repo = repo, table = l, matchColumn = l.cardId, exactMatch = true, expectedRows = listOf(
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
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = card1Id, delay = "1d"))
        val setDelayCard1Resp2 = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = card1Id)).data!!

        //then
        assertEquals(card1Id, setDelayCard1Resp2.id)
        assertEquals("1d", setDelayCard1Resp2.schedule.delay)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2),
            listOf(c.id to card2Id, c.type to TR_TP, c.createdAt to time8),
        ))
        assertTableContent(repo = repo, table = c.ver, matchColumn = c.id, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+"),
            listOf(t.cardId to card2Id, t.textToTranslate to "karta2+", t.translation to "card2"),
        ))
        assertTableContent(repo = repo, table = t.ver, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.ver.timestamp to time7, t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1"),
            listOf(t.ver.timestamp to time14, t.cardId to card2Id, t.textToTranslate to "karta2", t.translation to "card2"),
        ))

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d"),
            listOf(s.cardId to card2Id, s.updatedAt to time18, s.delay to "5m"),
        ))
        assertTableContent(repo = repo, table = s.ver, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0m"),
            listOf(s.ver.timestamp to time12, s.cardId to card2Id, s.updatedAt to time8, s.delay to "0m"),
            listOf(s.ver.timestamp to time18, s.cardId to card2Id, s.updatedAt to time12, s.delay to "5m"),
        ))

        assertTableContent(repo = repo, table = l, matchColumn = l.cardId, exactMatch = true, expectedRows = listOf(
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
        dm.updateTranslateCard(UpdateTranslateCardArgs(cardId = card1Id, delay = "0m"))
        val setDelayCard1Resp3 = dm.readTranslateCardById(ReadTranslateCardByIdArgs(cardId = card1Id)).data!!

        //then
        assertEquals(card1Id, setDelayCard1Resp3.id)
        assertEquals("0m", setDelayCard1Resp3.schedule.delay)

        assertTableContent(repo = repo, table = c, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to card1Id, c.type to TR_TP, c.createdAt to time2),
            listOf(c.id to card2Id, c.type to TR_TP, c.createdAt to time8),
        ))
        assertTableContent(repo = repo, table = c.ver, matchColumn = c.id, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+"),
            listOf(t.cardId to card2Id, t.textToTranslate to "karta2+", t.translation to "card2"),
        ))
        assertTableContent(repo = repo, table = t.ver, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.ver.timestamp to time7, t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1"),
            listOf(t.ver.timestamp to time14, t.cardId to card2Id, t.textToTranslate to "karta2", t.translation to "card2"),
        ))

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to card1Id, s.updatedAt to time23, s.delay to "0m"),
            listOf(s.cardId to card2Id, s.updatedAt to time18, s.delay to "5m"),
        ))
        assertTableContent(repo = repo, table = s.ver, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0m"),
            listOf(s.ver.timestamp to time23, s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d"),
            listOf(s.ver.timestamp to time12, s.cardId to card2Id, s.updatedAt to time8, s.delay to "0m"),
            listOf(s.ver.timestamp to time18, s.cardId to card2Id, s.updatedAt to time12, s.delay to "5m"),
        ))

        assertTableContent(repo = repo, table = l, matchColumn = l.cardId, exactMatch = true, expectedRows = listOf(
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

        assertTableContent(repo = repo, table = c, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.id to card2Id, c.type to TR_TP, c.createdAt to time8),
        ))
        assertTableContent(repo = repo, table = c.ver, matchColumn = c.id, exactMatch = true, expectedRows = listOf(
            listOf(c.ver.timestamp to time25, c.id to card1Id, c.type to TR_TP, c.createdAt to time2),
        ))

        assertTableContent(repo = repo, table = t, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to card2Id, t.textToTranslate to "karta2+", t.translation to "card2"),
        ))
        assertTableContent(repo = repo, table = t.ver, matchColumn = t.cardId, exactMatch = true, expectedRows = listOf(
            listOf(t.ver.timestamp to time7, t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1"),
            listOf(t.ver.timestamp to time25, t.cardId to card1Id, t.textToTranslate to "karta1", t.translation to "card1+"),
            listOf(t.ver.timestamp to time14, t.cardId to card2Id, t.textToTranslate to "karta2", t.translation to "card2"),
        ))

        assertTableContent(repo = repo, table = s, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.cardId to card2Id, s.updatedAt to time18, s.delay to "5m"),
        ))
        assertTableContent(repo = repo, table = s.ver, matchColumn = s.cardId, exactMatch = true, expectedRows = listOf(
            listOf(s.ver.timestamp to time6, s.cardId to card1Id, s.updatedAt to time2, s.delay to "0m"),
            listOf(s.ver.timestamp to time23, s.cardId to card1Id, s.updatedAt to time6, s.delay to "1d"),
            listOf(s.ver.timestamp to time25, s.cardId to card1Id, s.updatedAt to time23, s.delay to "0m"),
            listOf(s.ver.timestamp to time12, s.cardId to card2Id, s.updatedAt to time8, s.delay to "0m"),
            listOf(s.ver.timestamp to time18, s.cardId to card2Id, s.updatedAt to time12, s.delay to "5m"),
        ))

        assertTableContent(repo = repo, table = l, matchColumn = l.cardId, exactMatch = true, expectedRows = listOf(
            listOf(l.cardId to card1Id, l.timestamp to time5, l.translation to "card1", l.matched to 1),
            listOf(l.cardId to card2Id, l.timestamp to time11, l.translation to "card2-inc", l.matched to 0),
            listOf(l.cardId to card2Id, l.timestamp to time17, l.translation to "card2", l.matched to 1),
        ))

        //when: 26. request history for card2
        val card2History = dm.readTranslateCardHistory(ReadTranslateCardHistoryArgs(cardId = card2Id)).data!!.historyRecords

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
    fun getNextCardToRepeat_returns_random_card_if_there_few_cards_with_same_overdue() {
        //given
        val expectedCardId1 = 1236L
        val expectedCardId2 = 1244L
        val baseTime = 1_000
        val timeElapsed = 27_000
        fun createCardRecord(cardId: Long) = listOf(c.id to cardId, c.type to TR_TP, c.createdAt to 0)
        insert(repo = repo, table = c, rows = listOf(
            createCardRecord(cardId = expectedCardId1),
            createCardRecord(cardId = expectedCardId2),
        ))
        fun createScheduleRecord(cardId: Long, nextAccessIn: Int) =
            listOf(s.cardId to cardId, s.updatedAt to 0, s.delay to "1m", s.randomFactor to 1.0, s.nextAccessInMillis to nextAccessIn, s.nextAccessAt to baseTime + nextAccessIn)
        insert(repo = repo, table = s, rows = listOf(
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
            listOf(s.cardId to cardId, s.updatedAt to 0, s.delay to "0m", s.randomFactor to 1.0, s.nextAccessInMillis to 0, s.nextAccessAt to 0)
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