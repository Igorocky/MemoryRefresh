package org.igye.memoryrefresh.unit.instrumentation

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.igye.memoryrefresh.testutils.InstrumentedTestBase
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReadTagInstrumentedUnitTest: InstrumentedTestBase() {
    @Test
    fun readAllTags_returns_all_tags() {
        //given
        insert(repo = repo, table = tg, listOf(
            listOf(tg.id to 1, tg.name to "A", tg.createdAt to 0),
            listOf(tg.id to 3, tg.name to "C", tg.createdAt to 0),
            listOf(tg.id to 2, tg.name to "B", tg.createdAt to 0),
        ))

        //when
        val allTags = dm.readAllTags().data!!

        //then
        Assert.assertEquals(3, allTags.size)
        val idToName = allTags.map { it.id to it.name }.toMap()
        Assert.assertEquals("A", idToName[1])
        Assert.assertEquals("B", idToName[2])
        Assert.assertEquals("C", idToName[3])
    }
}