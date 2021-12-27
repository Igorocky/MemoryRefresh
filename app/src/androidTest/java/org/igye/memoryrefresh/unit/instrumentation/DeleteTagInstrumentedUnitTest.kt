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
        val dm = createInmemoryDataManager()
        val repo = dm.getRepo()
        val t = repo.tags
        val expectedTag1Name = "t1"
        val expectedTag2Name = "t2"
        val expectedTag3Name = "t3"
        testClock.setFixedTime(1000)

        val time1 = testClock.plus(2000)
        val tagId1 = dm.createTag(CreateTagArgs(name = expectedTag1Name)).data!!
        val time2 = testClock.plus(2000)
        val tagId2 = dm.createTag(CreateTagArgs(name = "  $expectedTag2Name   ")).data!!
        val time3 = testClock.plus(2000)
        val tagId3 = dm.createTag(CreateTagArgs(name = "\t $expectedTag3Name \t")).data!!

        assertTableContent(repo = repo, table = t, exactMatch = true, matchColumn = t.id, expectedRows = listOf(
            listOf(t.id to tagId1, t.createdAt to time1, t.name to expectedTag1Name),
            listOf(t.id to tagId2, t.createdAt to time2, t.name to expectedTag2Name),
            listOf(t.id to tagId3, t.createdAt to time3, t.name to expectedTag3Name),
        ))

        //when
        testClock.plus(2000)
        val tagDeletionResult = dm.deleteTag(DeleteTagArgs(tagId = tagId2)).data!!

        //then
        assertTrue(tagDeletionResult)

        assertTableContent(repo = repo, table = t, exactMatch = true, matchColumn = t.id, expectedRows = listOf(
            listOf(t.id to tagId1, t.createdAt to time1, t.name to expectedTag1Name),
            listOf(t.id to tagId3, t.createdAt to time3, t.name to expectedTag3Name),
        ))
    }

}