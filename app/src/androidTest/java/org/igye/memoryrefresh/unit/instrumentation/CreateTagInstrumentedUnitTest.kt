package org.igye.memoryrefresh.unit.instrumentation

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.igye.memoryrefresh.manager.DataManager.CreateTagArgs
import org.igye.memoryrefresh.testutils.InstrumentedTestBase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreateTagInstrumentedUnitTest: InstrumentedTestBase() {

    @Test
    fun createTag_creates_new_tag() {
        //given
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val t = repo.tags
        val expectedTag1Name = "t1"
        val expectedTag2Name = "t2"
        val expectedTag3Name = "t3"
        testClock.setFixedTime(1000)

        //when
        val time1 = testClock.plus(2000)
        val tagId1 = dm.createTag(CreateTagArgs(name = expectedTag1Name)).data!!
        val time2 = testClock.plus(2000)
        val tagId2 = dm.createTag(CreateTagArgs(name = "  $expectedTag2Name   ")).data!!
        val time3 = testClock.plus(2000)
        val tagId3 = dm.createTag(CreateTagArgs(name = "\t $expectedTag3Name \t")).data!!

        //then
        assertTableContent(repo = repo, table = t, exactMatch = true, matchColumn = t.id, expectedRows = listOf(
            listOf(t.id to tagId1, t.createdAt to time1, t.name to expectedTag1Name),
            listOf(t.id to tagId2, t.createdAt to time2, t.name to expectedTag2Name),
            listOf(t.id to tagId3, t.createdAt to time3, t.name to expectedTag3Name),
        ))
    }

    @Test
    fun createTag_doesnt_allow_to_save_tag_with_same_name() {
        //given
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val t = repo.tags
        testClock.setFixedTime(1000)
        insert(repo = repo, table = t, rows = listOf(
            listOf(t.id to 1, t.createdAt to 1000, t.name to "ttt")
        ))
        assertTableContent(repo = repo, table = t, exactMatch = true, matchColumn = t.id, expectedRows = listOf(
            listOf(t.id to 1, t.createdAt to 1000, t.name to "ttt"),
        ))

        //when
        val err = dm.createTag(CreateTagArgs(name = "ttt")).err!!

        //then
        assertEquals("A tag with name 'ttt' already exists.", err.msg)

        assertTableContent(repo = repo, table = t, exactMatch = true, matchColumn = t.id, expectedRows = listOf(
            listOf(t.id to 1, t.createdAt to 1000, t.name to "ttt"),
        ))
    }
}