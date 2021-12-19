package org.igye.taggednotes

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

typealias TagStat = Map<Long,Long>
class TagsStatHolder(
    private val repositoryGetter: () -> Repository
) {
    private val t = DB_NAMES
    private val changeCnt = AtomicInteger(0)
    private val tagStat = AtomicReference<TagStat>(null)

    fun reset() {
        tagStat.set(null)
    }

    fun tagsCouldChange() {
        changeCnt.incrementAndGet()
    }

    fun getTagStat(): TagStat {
        if (tagStat.get() == null || changeCnt.get() > 100) {
            synchronized(this) {
                if (tagStat.get() == null || changeCnt.get() > 100) {
                    tagStat.set(
                        repositoryGetter.invoke().select(
                            query = "select ${t.noteToTag.tagId} id, count(1) cnt from ${t.noteToTag} group by ${t.noteToTag.tagId}",
                            columnNames = listOf("id", "cnt")
                        ) {
                            Pair(it.getLong(), it.getLong())
                        }.second.toMap()
                    )
                    changeCnt.set(0)
                }
            }
        }
        return tagStat.get()
    }
}