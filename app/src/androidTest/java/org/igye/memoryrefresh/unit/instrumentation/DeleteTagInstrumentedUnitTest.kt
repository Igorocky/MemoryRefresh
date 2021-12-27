package org.igye.memoryrefresh.unit.instrumentation

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.igye.memoryrefresh.manager.DataManager.CreateTagArgs
import org.igye.memoryrefresh.manager.DataManager.DeleteTagArgs
import org.igye.memoryrefresh.testutils.InstrumentedTestBase
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeleteTagInstrumentedUnitTest: InstrumentedTestBase() {

    @Test
    fun deleteTag_deletes_tag() {
        //given
        val expectedTag1Name = "t1"
        val expectedTag2Name = "t2"
        val expectedTag3Name = "t3"

        val time1 = testClock.plus(2000)
        val tagId1 = dm.createTag(CreateTagArgs(name = expectedTag1Name)).data!!
        val time2 = testClock.plus(2000)
        val tagId2 = dm.createTag(CreateTagArgs(name = "  $expectedTag2Name   ")).data!!
        val time3 = testClock.plus(2000)
        val tagId3 = dm.createTag(CreateTagArgs(name = "\t $expectedTag3Name \t")).data!!

        assertTableContent(repo = repo, table = tg, exactMatch = true, matchColumn = tg.id, expectedRows = listOf(
            listOf(tg.id to tagId1, tg.createdAt to time1, tg.name to expectedTag1Name),
            listOf(tg.id to tagId2, tg.createdAt to time2, tg.name to expectedTag2Name),
            listOf(tg.id to tagId3, tg.createdAt to time3, tg.name to expectedTag3Name),
        ))

        //when
        testClock.plus(2000)
        val tagDeletionResult = dm.deleteTag(DeleteTagArgs(tagId = tagId2)).data!!

        //then
        assertTrue(tagDeletionResult)

        assertTableContent(repo = repo, table = tg, exactMatch = true, matchColumn = tg.id, expectedRows = listOf(
            listOf(tg.id to tagId1, tg.createdAt to time1, tg.name to expectedTag1Name),
            listOf(tg.id to tagId3, tg.createdAt to time3, tg.name to expectedTag3Name),
        ))
    }

    @Test
    fun deleteTag_doesnt_delete_tag_if_there_is_at_least_one_card_using_this_tag() {
        TODO()
    }

}