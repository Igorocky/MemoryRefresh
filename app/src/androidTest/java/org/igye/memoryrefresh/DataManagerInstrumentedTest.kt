package org.igye.memoryrefresh

import android.database.Cursor
import android.database.Cursor.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.igye.memoryrefresh.DataManager.*
import org.igye.memoryrefresh.database.CardType
import org.igye.memoryrefresh.database.ChangeType
import org.igye.memoryrefresh.database.Repository
import org.igye.memoryrefresh.database.tables.CardsScheduleTable
import org.igye.memoryrefresh.database.tables.CardsTable
import org.igye.memoryrefresh.database.tables.TranslationCardsLogTable
import org.igye.memoryrefresh.database.tables.TranslationCardsTable
import org.igye.memoryrefresh.dto.TranslateCard
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class DataManagerInstrumentedTest {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    val testClock = TestClock(1000)
    val TR_TP = CardType.TRANSLATION.intValue

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
        val repo = dm.getRepo()
        val c = repo.cards
        val t = repo.translationCards
        val s = repo.cardsSchedule
        val l = repo.translationCardsLog
        val expectedTextToTranslate = "A"
        val expectedTranslation = "a"
        val time1 = 1000L
        testClock.setFixedTime(time1)

        //when
        val actualTranslateCardResp = dm.saveNewTranslateCard(
            SaveNewTranslateCardArgs(textToTranslate = " $expectedTextToTranslate\t", translation = "  \t$expectedTranslation    \t  ")
        )

        //then
        val translateCard: TranslateCard = actualTranslateCardResp.data!!
        assertEquals(expectedTextToTranslate, translateCard.textToTranslate)
        assertEquals(expectedTranslation, translateCard.translation)
        assertEquals(time1, translateCard.lastAccessedAt)
        assertEquals(0, translateCard.nextAccessInSec)
        assertEquals(time1, translateCard.nextAccessAt)

        assertTableContent(repo = repo, tableName = c.name, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to translateCard.id, c.type to TR_TP, c.createdAt to time1)
        ))
        assertTableContent(repo = repo, tableName = c.ver.name, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = t.name, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to translateCard.id, t.textToTranslate to expectedTextToTranslate, t.translation to expectedTranslation)
        ))
        assertTableContent(repo = repo, tableName = t.ver.name, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = s.name, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to translateCard.id, s.lastAccessedAt to time1, s.nextAccessInSec to 0L, s.nextAccessAt to time1)
        ))
        assertTableContent(repo = repo, tableName = s.ver.name, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = l.name, exactMatch = true, expectedRows = listOf())
    }

    @Test
    fun test_scenario_1_create_card_and_edit_it_twice() {
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
        val timeCrt = 1000L
        val timeEdt1 = timeCrt + 5000
        val timeEdt2 = timeEdt1 + 5000

        //when: create a new translation card
        testClock.setFixedTime(timeCrt)
        val responseAfterCreate = dm.saveNewTranslateCard(
            SaveNewTranslateCardArgs(textToTranslate = expectedTextToTranslate1, translation = expectedTranslation1)
        )

        //then: a new card is created successfully
        val actualCreatedCard: TranslateCard = responseAfterCreate.data!!
        assertEquals(expectedTextToTranslate1, actualCreatedCard.textToTranslate)
        assertEquals(expectedTranslation1, actualCreatedCard.translation)
        assertEquals(timeCrt, actualCreatedCard.lastAccessedAt)
        assertEquals(0, actualCreatedCard.nextAccessInSec)
        assertEquals(timeCrt, actualCreatedCard.nextAccessAt)

        assertTableContent(repo = repo, tableName = c.name, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to actualCreatedCard.id, c.type to TR_TP, c.createdAt to timeCrt)
        ))
        assertTableContent(repo = repo, tableName = c.ver.name, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = t.name, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to actualCreatedCard.id, t.textToTranslate to expectedTextToTranslate1, t.translation to expectedTranslation1)
        ))
        assertTableContent(repo = repo, tableName = t.ver.name, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = s.name, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to actualCreatedCard.id, s.lastAccessedAt to timeCrt, s.nextAccessInSec to 0L, s.nextAccessAt to timeCrt)
        ))
        assertTableContent(repo = repo, tableName = s.ver.name, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = l.name, exactMatch = true, expectedRows = listOf())

        //when: edit the card but provide same values
        testClock.setFixedTime(timeEdt1)
        val responseAfterEdit1 = dm.editTranslateCard(
            EditTranslateCardArgs(cardId = actualCreatedCard.id, textToTranslate = "$expectedTextToTranslate1  ", translation = "\t$expectedTranslation1")
        )

        //then: the card stays in the same state - no actual edit was done
        val translateCardAfterEdit1: TranslateCard = responseAfterEdit1.data!!
        assertEquals(expectedTextToTranslate1, translateCardAfterEdit1.textToTranslate)
        assertEquals(expectedTranslation1, translateCardAfterEdit1.translation)
        assertEquals(timeCrt, translateCardAfterEdit1.lastAccessedAt)
        assertEquals(0, translateCardAfterEdit1.nextAccessInSec)
        assertEquals(timeCrt, translateCardAfterEdit1.nextAccessAt)

        assertTableContent(repo = repo, tableName = c.name, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to translateCardAfterEdit1.id, c.type to TR_TP, c.createdAt to timeCrt)
        ))
        assertTableContent(repo = repo, tableName = c.ver.name, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = t.name, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to translateCardAfterEdit1.id, t.textToTranslate to expectedTextToTranslate1, t.translation to expectedTranslation1)
        ))
        assertTableContent(repo = repo, tableName = t.ver.name, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = s.name, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to translateCardAfterEdit1.id, s.lastAccessedAt to timeCrt, s.nextAccessInSec to 0L, s.nextAccessAt to timeCrt)
        ))
        assertTableContent(repo = repo, tableName = s.ver.name, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = l.name, exactMatch = true, expectedRows = listOf())

        //when: provide new values when editing the card
        testClock.setFixedTime(timeEdt2)
        val responseAfterEdit2 = dm.editTranslateCard(
            EditTranslateCardArgs(cardId = actualCreatedCard.id, textToTranslate = "  $expectedTextToTranslate2  ", translation = "\t$expectedTranslation2  ")
        )

        //then: the values of card are updated and the previous version of the card is saved to the corresponding VER table
        val translateCardAfterEdit2: TranslateCard = responseAfterEdit2.data!!
        assertEquals(expectedTextToTranslate2, translateCardAfterEdit2.textToTranslate)
        assertEquals(expectedTranslation2, translateCardAfterEdit2.translation)
        assertEquals(timeCrt, translateCardAfterEdit2.lastAccessedAt)
        assertEquals(0, translateCardAfterEdit2.nextAccessInSec)
        assertEquals(timeCrt, translateCardAfterEdit2.nextAccessAt)

        assertTableContent(repo = repo, tableName = c.name, matchColumn = c.id, expectedRows = listOf(
            listOf(c.id to translateCardAfterEdit2.id, c.type to TR_TP, c.createdAt to timeCrt)
        ))
        assertTableContent(repo = repo, tableName = c.ver.name, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = t.name, matchColumn = t.cardId, expectedRows = listOf(
            listOf(t.cardId to translateCardAfterEdit2.id, t.textToTranslate to expectedTextToTranslate2, t.translation to expectedTranslation2)
        ))
        assertTableContent(repo = repo, tableName = t.ver.name, exactMatch = true, expectedRows = listOf(
            listOf(t.cardId to translateCardAfterEdit2.id, t.textToTranslate to expectedTextToTranslate1, t.translation to expectedTranslation1,
                t.ver.timestamp to timeEdt2, t.ver.changeType to ChangeType.UPDATE.intValue)
        ))

        assertTableContent(repo = repo, tableName = s.name, matchColumn = s.cardId, expectedRows = listOf(
            listOf(s.cardId to translateCardAfterEdit2.id, s.lastAccessedAt to timeCrt, s.nextAccessInSec to 0L, s.nextAccessAt to timeCrt)
        ))
        assertTableContent(repo = repo, tableName = s.ver.name, exactMatch = true, expectedRows = listOf())

        assertTableContent(repo = repo, tableName = l.name, exactMatch = true, expectedRows = listOf())
    }

    private fun assertTableContent(
        repo: Repository, tableName: String, exactMatch: Boolean = false, matchColumn: String = "", expectedRows: List<List<Pair<String,Any?>>>
    ) {
        val allData = readAllDataFrom(repo, tableName)
        if (exactMatch) {
            assertTrue(expectedRows.size == allData.size)
        } else {
            assertTrue(expectedRows.size <= allData.size)
        }
        for (expectedRow in expectedRows) {
            if (count(expected = expectedRow, allData = allData) != 1) {
                fail("Data comparison failed for table $tableName\n" + formatActualAndExpected(allData = allData, expected = expectedRow, matchColumn = matchColumn))
            }
        }
    }

    private fun formatActualAndExpected(allData: List<Map<String, Any?>>, expected: List<Pair<String, Any?>>, matchColumn: String): String {
        val filteredData = if (matchColumn.length > 0) {
            filter(allData = allData, columnName = matchColumn, expectedValue = expected.toMap()[matchColumn])
        } else {
            allData
        }
        val sortOrder: Map<String, Int> = expected.asSequence().mapIndexed{ i, (name,_) -> name to i}.toMap()
        return "Expected:\n" +
                format(expected) + "\n" +
                "Actual:\n" +
                filteredData.asSequence().map { format(sort(it.toList(), sortOrder)) }.joinToString(separator = "\n")

    }

    private fun sort(values: List<Pair<String, Any?>>, sortOrder: Map<String, Int>): List<Pair<String, Any?>> {
        return values.sortedBy { sortOrder[it.first]?:Int.MAX_VALUE }
    }

    private fun format(values: List<Pair<String, Any?>>): String {
        return values.asSequence().map { "${it.first}=${it.second}" }.joinToString(separator = ", ")
    }

    private fun filter(allData: List<Map<String, Any?>>, columnName: String, expectedValue: Any?): List<Map<String, Any?>> {
        return allData.asSequence()
            .filter { it.containsKey(columnName) && Objects.equals(it[columnName], expectedValue) }
            .toList()
    }

    private fun count(expected: List<Pair<String,Any?>>, allData: List<Map<String, Any?>>): Int {
        var result = 0
        for (actualRow in allData) {
            if (compare(expected = expected, actual = actualRow)) {
                result++
            }
        }
        return result
    }

    private fun compare(expected: List<Pair<String,Any?>>, actual: Map<String,Any?>): Boolean {
        for (expectedColumn in expected) {
            val columnName = expectedColumn.first
            if (!actual.containsKey(columnName)) {
                return false
            }
            if (!Objects.equals(expectedColumn.second, actual[columnName])) {
                return false
            }
        }
        return true
    }

    private fun readAllDataFrom(repo: Repository, tableName: String): List<Map<String,Any?>> {
        val result = ArrayList<Map<String,Any?>>()
        repo.readableDatabase.rawQuery("select * from $tableName", null).use { cursor ->
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast) {
                    result.add(readRow(cursor))
                    cursor.moveToNext()
                }
            }
        }
        return result
    }

    private fun readRow(cursor: Cursor): Map<String,Any?> {
        val res = HashMap<String,Any?>()
        for (i in 0 until cursor.columnCount) {
            res[cursor.getColumnName(i)] = readColumnValue(cursor, i)
        }
        return res
    }

    private fun readColumnValue(cursor: Cursor, columnIndex: Int): Any? {
        val type = cursor.getType(columnIndex)
        return when(type) {
            FIELD_TYPE_NULL -> null
            FIELD_TYPE_INTEGER -> cursor.getLong(columnIndex)
            FIELD_TYPE_STRING -> cursor.getString(columnIndex)
            else -> throw MemoryRefreshException("Unexpected type '$type'")
        }
    }

    private fun createInmemoryDataManager(): DataManager {
        val cards = CardsTable(clock = testClock)
        val cardsSchedule = CardsScheduleTable(clock = testClock, cards = cards)
        val translationCards = TranslationCardsTable(clock = testClock, cards = cards)
        val translationCardsLog = TranslationCardsLogTable(clock = testClock, translationCards = translationCards)

        return DataManager(
            context = appContext,
            clock = testClock,
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