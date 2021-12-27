package org.igye.memoryrefresh.testutils

import android.database.Cursor
import android.database.Cursor.*
import androidx.test.platform.app.InstrumentationRegistry
import org.igye.memoryrefresh.ErrorCode.ERROR_IN_TEST
import org.igye.memoryrefresh.common.MemoryRefreshException
import org.igye.memoryrefresh.database.CardType
import org.igye.memoryrefresh.database.Repository
import org.igye.memoryrefresh.database.Table
import org.igye.memoryrefresh.database.tables.*
import org.igye.memoryrefresh.manager.DataManager
import org.igye.memoryrefresh.manager.RepositoryManager
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import java.util.*

open class InstrumentedTestBase {
    protected val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    protected val testClock = TestClock(1000)
    protected val TR_TP = CardType.TRANSLATION.intValue

    protected fun insert(repo: Repository, table: Table, rows: List<List<Pair<String,Any?>>>) {
        val query = """
            insert into $table (${rows[0].map { it.first }.joinToString(separator = ", ")}) 
            values ${rows.map {row -> row.map { "?" }.joinToString(prefix = "(", separator = ",", postfix = ")")}.joinToString(separator = ",") }
            """.trimIndent()
        val insertStmt = repo.writableDatabase.compileStatement(query)
        var idx = 0
        rows.flatMap { it }.map { it.second }.forEach {
            when (it) {
                is Long -> insertStmt.bindLong(++idx, it)
                is Int -> insertStmt.bindLong(++idx, it.toLong())
                is Double -> insertStmt.bindDouble(++idx, it)
                is String -> insertStmt.bindString(++idx, it)
            }
        }
        insertStmt.executeUpdateDelete()
    }

    protected fun assertTableContent(
        repo: Repository, table: Table, exactMatch: Boolean = false, matchColumn: String = "", expectedRows: List<List<Pair<String,Any?>>>
    ) {
        val allData = readAllDataFrom(repo, table.tableName)
        if (exactMatch) {
            assertTrue(expectedRows.size == allData.size)
        } else {
            assertTrue(expectedRows.size <= allData.size)
        }
        for (expectedRow in expectedRows) {
            if (count(expected = expectedRow, allData = allData) != 1) {
                fail("Data comparison failed for table $table\n" + formatActualAndExpected(allData = allData, expected = expectedRow, matchColumn = matchColumn))
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
            if (!equals(expectedColumn.second, actual[columnName])) {
                return false
            }
        }
        return true
    }

    private fun equals(expected: Any?, actual: Any?): Boolean {
        if (expected is Integer && actual is Long) {
            return expected.toLong() == actual
        } else {
            return Objects.equals(expected, actual)
        }
    }

    protected fun readAllDataFrom(repo: Repository, tableName: String): List<Map<String,Any?>> {
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
            FIELD_TYPE_FLOAT -> cursor.getDouble(columnIndex)
            else -> throw MemoryRefreshException(msg = "Unexpected type '$type'", errCode = ERROR_IN_TEST)
        }
    }

    protected fun createInmemoryDataManager(): DataManager {
        val cards = CardsTable(clock = testClock)
        val cardsSchedule = CardsScheduleTable(clock = testClock, cards = cards)
        val translationCards = TranslationCardsTable(clock = testClock, cards = cards)
        val translationCardsLog = TranslationCardsLogTable(clock = testClock)
        val tags = TagsTable(clock = testClock)
        val cardToTag = CardToTagTable(clock = testClock, cards = cards, tags = tags)

        return DataManager(
            clock = testClock,
            repositoryManager = RepositoryManager(
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
                        tags = tags,
                        cardToTag = cardToTag,
                    )
                }
            )
        )
    }

    private fun inc(counts: MutableMap<Int, Int>, key: Int) {
        var cnt = counts[key]
        if (cnt == null) {
            cnt = 0
        }
        counts[key] = cnt + 1
    }
}