package org.igye.memoryrefresh

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.igye.memoryrefresh.DataManager.*
import org.igye.memoryrefresh.database.Repository
import org.igye.memoryrefresh.database.tables.v1.CardsScheduleTable
import org.igye.memoryrefresh.database.tables.v1.CardsTable
import org.igye.memoryrefresh.database.tables.v1.TranslationCardsLogTable
import org.igye.memoryrefresh.database.tables.v1.TranslationCardsTable
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class DataManagerInstrumentedTest {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext

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

        //when
        val actualTranslateCardResp = dm.saveNewTranslateCard(SaveNewTranslateCardArgs(textToTranslate = "A", translation = "a"))

        //then
        val translateCard = actualTranslateCardResp.data!!
        assertEquals("A", translateCard.textToTranslate)
    }

    private fun createInmemoryDataManager(): DataManager {
        val cards = CardsTable()
        val cardsSchedule = CardsScheduleTable(cards = cards)
        val translationCards = TranslationCardsTable(cards = cards)
        val translationCardsLog = TranslationCardsLogTable(translationCards = translationCards)

        return DataManager(
            context = appContext,
            repositoryProvider = {
                Repository(
                    context = appContext,
                    dbName = null,
                    cards = cards,
                    cardsSchedule = cardsSchedule,
                    translationCards = translationCards,
                    translationCardsLog = translationCardsLog,
                )
            }
        )
    }
}