package org.igye.memoryrefresh.unit.instrumentation

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.igye.memoryrefresh.dto.domain.Tag
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

    @Test
    fun readAllTags_returns_tags_ordered_by_name() {
        //given
        insert(repo = repo, table = tg, listOf(
            listOf(tg.id to 5, tg.name to "A", tg.createdAt to 0),
            listOf(tg.id to 4, tg.name to "C", tg.createdAt to 0),
            listOf(tg.id to 3, tg.name to "B", tg.createdAt to 0),
            listOf(tg.id to 2, tg.name to "E", tg.createdAt to 0),
            listOf(tg.id to 1, tg.name to "D", tg.createdAt to 0),
        ))

        //when
        val allTags: List<Tag> = dm.readAllTags().data!!

        //then
        Assert.assertEquals(5, allTags.size)
        Assert.assertEquals("A", allTags[0].name)
        Assert.assertEquals("B", allTags[1].name)
        Assert.assertEquals("C", allTags[2].name)
        Assert.assertEquals("D", allTags[3].name)
        Assert.assertEquals("E", allTags[4].name)
    }

    @Test
    fun getCardToTagMapping_returns_result_as_expected_when_last_card_has_one_tag() {
        //given
        val tagId1 = createTag(tagId = 1, name = "t1")
        val tagId2 = createTag(tagId = 2, name = "t2")
        val tagId3 = createTag(tagId = 3, name = "t3")
        val cardId1 = createCard(cardId = 1L).id
        val cardId2 = createCard(cardId = 2L).id
        val cardId3 = createCard(cardId = 3L).id

        insert(repo = repo, table = ctg, listOf(
            listOf(ctg.cardId to cardId1, ctg.tagId to tagId1),
            listOf(ctg.cardId to cardId2, ctg.tagId to tagId1),
            listOf(ctg.cardId to cardId3, ctg.tagId to tagId2),
            listOf(ctg.cardId to cardId2, ctg.tagId to tagId3),
            listOf(ctg.cardId to cardId1, ctg.tagId to tagId2),
        ))

        //when
        val mapping = dm.getCardToTagMapping().data!!

        //then
        Assert.assertEquals(3, mapping.size)
        Assert.assertEquals(listOf(tagId1, tagId2), mapping[cardId1])
        Assert.assertEquals(listOf(tagId1, tagId3), mapping[cardId2])
        Assert.assertEquals(listOf(tagId2), mapping[cardId3])
    }

    @Test
    fun getCardToTagMapping_returns_result_as_expected_when_last_card_has_two_tags() {
        //given
        val tagId1 = createTag(tagId = 1, name = "t1")
        val tagId2 = createTag(tagId = 2, name = "t2")
        val tagId3 = createTag(tagId = 3, name = "t3")
        val cardId1 = createCard(cardId = 1L).id
        val cardId2 = createCard(cardId = 2L).id
        val cardId3 = createCard(cardId = 3L).id

        insert(repo = repo, table = ctg, listOf(
            listOf(ctg.cardId to cardId1, ctg.tagId to tagId1),
            listOf(ctg.cardId to cardId2, ctg.tagId to tagId1),
            listOf(ctg.cardId to cardId3, ctg.tagId to tagId2),
            listOf(ctg.cardId to cardId2, ctg.tagId to tagId3),
            listOf(ctg.cardId to cardId1, ctg.tagId to tagId2),
            listOf(ctg.cardId to cardId3, ctg.tagId to tagId3),
        ))

        //when
        val mapping = dm.getCardToTagMapping().data!!

        //then
        Assert.assertEquals(3, mapping.size)
        Assert.assertEquals(listOf(tagId1, tagId2), mapping[cardId1])
        Assert.assertEquals(listOf(tagId1, tagId3), mapping[cardId2])
        Assert.assertEquals(listOf(tagId2,tagId3), mapping[cardId3])
    }

    @Test
    fun getCardToTagMapping_returns_result_as_expected_when_first_card_has_one_tag() {
        //given
        val tagId1 = createTag(tagId = 1, name = "t1")
        val tagId2 = createTag(tagId = 2, name = "t2")
        val tagId3 = createTag(tagId = 3, name = "t3")
        val cardId1 = createCard(cardId = 1L).id
        val cardId2 = createCard(cardId = 2L).id
        val cardId3 = createCard(cardId = 3L).id

        insert(repo = repo, table = ctg, listOf(
            listOf(ctg.cardId to cardId2, ctg.tagId to tagId1),
            listOf(ctg.cardId to cardId3, ctg.tagId to tagId2),
            listOf(ctg.cardId to cardId2, ctg.tagId to tagId3),
            listOf(ctg.cardId to cardId1, ctg.tagId to tagId2),
            listOf(ctg.cardId to cardId3, ctg.tagId to tagId3),
        ))

        //when
        val mapping = dm.getCardToTagMapping().data!!

        //then
        Assert.assertEquals(3, mapping.size)
        Assert.assertEquals(listOf(tagId2), mapping[cardId1])
        Assert.assertEquals(listOf(tagId1, tagId3), mapping[cardId2])
        Assert.assertEquals(listOf(tagId2,tagId3), mapping[cardId3])
    }

    @Test
    fun getCardToTagMapping_returns_result_as_expected_when_middle_card_has_one_tag() {
        //given
        val tagId1 = createTag(tagId = 1, name = "t1")
        val tagId2 = createTag(tagId = 2, name = "t2")
        val tagId3 = createTag(tagId = 3, name = "t3")
        val cardId1 = createCard(cardId = 1L).id
        val cardId2 = createCard(cardId = 2L).id
        val cardId3 = createCard(cardId = 3L).id

        insert(repo = repo, table = ctg, listOf(
            listOf(ctg.cardId to cardId1, ctg.tagId to tagId1),
            listOf(ctg.cardId to cardId2, ctg.tagId to tagId3),
            listOf(ctg.cardId to cardId3, ctg.tagId to tagId2),
            listOf(ctg.cardId to cardId3, ctg.tagId to tagId3),
            listOf(ctg.cardId to cardId1, ctg.tagId to tagId2),
        ))

        //when
        val mapping = dm.getCardToTagMapping().data!!

        //then
        Assert.assertEquals(3, mapping.size)
        Assert.assertEquals(listOf(tagId1, tagId2), mapping[cardId1])
        Assert.assertEquals(listOf(tagId3), mapping[cardId2])
        Assert.assertEquals(listOf(tagId2,tagId3), mapping[cardId3])
    }
}