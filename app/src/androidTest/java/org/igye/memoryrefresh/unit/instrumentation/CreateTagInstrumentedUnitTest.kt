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
        val expectedTag1Name = "t1"
        val expectedTag2Name = "t2"
        val expectedTag3Name = "t3"

        //when
        val time1 = testClock.plus(2000)
        val tagId1 = dm.createTag(CreateTagArgs(name = expectedTag1Name)).data!!
        val time2 = testClock.plus(2000)
        val tagId2 = dm.createTag(CreateTagArgs(name = "  $expectedTag2Name   ")).data!!
        val time3 = testClock.plus(2000)
        val tagId3 = dm.createTag(CreateTagArgs(name = "\t $expectedTag3Name \t")).data!!

        //then
        assertTableContent(repo = repo, table = tg, matchColumn = tg.id, expectedRows = listOf(
            listOf(tg.id to tagId1, tg.createdAt to time1, tg.name to expectedTag1Name),
            listOf(tg.id to tagId2, tg.createdAt to time2, tg.name to expectedTag2Name),
            listOf(tg.id to tagId3, tg.createdAt to time3, tg.name to expectedTag3Name),
        ))
    }

    @Test
    fun createTag_doesnt_allow_to_save_tag_with_same_name() {
        //given
        insert(repo = repo, table = tg, rows = listOf(
            listOf(tg.id to 1, tg.createdAt to 1000, tg.name to "ttt")
        ))
        assertTableContent(repo = repo, table = tg, matchColumn = tg.id, expectedRows = listOf(
            listOf(tg.id to 1, tg.createdAt to 1000, tg.name to "ttt"),
        ))

        //when
        val err = dm.createTag(CreateTagArgs(name = "ttt")).err!!

        //then
        assertEquals("A tag with name 'ttt' already exists.", err.msg)

        assertTableContent(repo = repo, table = tg, matchColumn = tg.id, expectedRows = listOf(
            listOf(tg.id to 1, tg.createdAt to 1000, tg.name to "ttt"),
        ))
    }
}