package org.igye.memoryrefresh.manager

import android.database.sqlite.SQLiteConstraintException
import org.igye.memoryrefresh.ErrorCode.*
import org.igye.memoryrefresh.common.*
import org.igye.memoryrefresh.common.Utils.toBeResponse
import org.igye.memoryrefresh.database.*
import org.igye.memoryrefresh.dto.common.BeErr
import org.igye.memoryrefresh.dto.common.BeRespose
import org.igye.memoryrefresh.dto.domain.*
import java.time.Clock
import java.util.*
import kotlin.random.Random


class DataManager(
    private val clock: Clock,
    private val repositoryManager: RepositoryManager,
) {
    fun getRepo() = repositoryManager.getRepo()

    private val c = getRepo().cards
    private val s = getRepo().cardsSchedule
    private val t = getRepo().translationCards
    private val l = getRepo().translationCardsLog
    private val tg = getRepo().tags
    private val ctg = getRepo().cardToTag

    private val tagsStat = repositoryManager.tagsStat

    data class CreateTagArgs(val name:String)
    @BeMethod
    @Synchronized
    fun createTag(args:CreateTagArgs): BeRespose<Long> {
        val name = args.name.trim()
        return if (name.isBlank()) {
            BeRespose(err = BeErr(code = SAVE_NEW_TAG_NAME_IS_EMPTY.code, msg = "Name of a new tag should not be empty."))
        } else {
            val repo = getRepo()
            repo.writableDatabase.doInTransaction {
                tagsStat.tagsCouldChange()
                repo.tags.insert(name = name)
            }
                .ifErrorThen { throwable ->
                    Failure(
                        if (throwable is SQLiteConstraintException && (throwable.message?:"").contains("UNIQUE constraint failed: TAGS.NAME")) {
                            MemoryRefreshException(
                                errCode = SAVE_NEW_TAG_NAME_IS_NOT_UNIQUE,
                                msg = "A tag with name '$name' already exists."
                            )
                        } else {
                            throwable
                        }
                    )
                }
                .apply(toBeResponse(SAVE_NEW_TAG))
        }
    }

    private val readAllTagsQuery = "select ${tg.id}, ${tg.name} from $tg"
    private val readAllTagsColumnNames = arrayOf(tg.id, tg.name)
    @BeMethod
    @Synchronized
    fun readAllTags(): BeRespose<List<Tag>> {
        val repo = getRepo()
        return Try {
            repo.readableDatabase.select(
                query = readAllTagsQuery,
                columnNames = readAllTagsColumnNames
            ) {
                Tag(id = it.getLong(), name = it.getString())
            }
        }
            .map { it.rows }
            .apply(toBeResponse(READ_ALL_TAGS))
    }

    data class UpdateTagArgs(val tagId:Long, val name:String)
    @BeMethod
    fun updateTag(args:UpdateTagArgs): BeRespose<Tag> {
        val newName = args.name.trim()
        return if (newName.isBlank()) {
            BeRespose(err = BeErr(code = UPDATE_TAG_NAME_IS_EMPTY.code, msg = "Name of a tag should not be empty."))
        } else {
            val repo = getRepo()
            repo.writableDatabase.doInTransaction {
                repo.tags.update(id = args.tagId, name = newName)
                Tag(
                    id = args.tagId,
                    name = newName
                )
            }
                .ifErrorThen { throwable ->
                    Failure(
                        if (throwable is SQLiteConstraintException && (throwable.message?:"").contains("UNIQUE constraint failed: TAGS.NAME")) {
                            MemoryRefreshException(
                                errCode = UPDATE_TAG_NAME_IS_NOT_UNIQUE,
                                msg = "A tag with name '$newName' already exists."
                            )
                        } else {
                            throwable
                        }
                    )
                }
                .apply(toBeResponse(UPDATE_TAG))
        }
    }

    data class DeleteTagArgs(val tagId:Long)
    @BeMethod
    fun deleteTag(args:DeleteTagArgs): BeRespose<Boolean> {
        val repo = getRepo()
        return repo.writableDatabase.doInTransaction {
            repo.tags.delete(id = args.tagId)
            true
        }
            .ifErrorThen { throwable ->
                Failure(
                    if (throwable is SQLiteConstraintException && (throwable.message?:"").contains("FOREIGN KEY constraint failed")) {
                        MemoryRefreshException(
                            errCode = DELETE_TAG_TAG_IS_USED,
                            msg = "Cannot delete tag because it is referenced by at least one card."
                        )
                    } else {
                        throwable
                    }
                )
            }
            .apply(toBeResponse(DELETE_TAG))
    }

    data class CreateTranslateCardArgs(
        val textToTranslate:String,
        val translation:String,
        val tagIds: Set<Long> = emptySet(),
        val paused: Boolean = false,
    )
    @BeMethod
    @Synchronized
    fun createTranslateCard(args: CreateTranslateCardArgs): BeRespose<Long> {
        val textToTranslate = args.textToTranslate.trim()
        val translation = args.translation.trim()
        if (textToTranslate.isBlank()) {
            return BeRespose(err = BeErr(code = SAVE_NEW_TRANSLATE_CARD_TEXT_TO_TRANSLATE_IS_EMPTY.code, msg = "Text to translate should not be empty."))
        } else if (translation.isBlank()) {
            return BeRespose(err = BeErr(code = SAVE_NEW_TRANSLATE_CARD_TRANSLATION_IS_EMPTY.code, msg = "Translation should not be empty."))
        } else {
            tagsStat.tagsCouldChange()
            val repo = getRepo()
            return repo.writableDatabase.doInTransactionTry {
                createCard(cardType = CardType.TRANSLATION, tagIds = args.tagIds, paused = args.paused).map { cardId ->
                    repo.translationCards.insert(cardId = cardId, textToTranslate = textToTranslate, translation = translation)
                    cardId
                }
            }.apply(toBeResponse(SAVE_NEW_TRANSLATE_CARD_EXCEPTION))
        }
    }

    @BeMethod
    @Synchronized
    fun getNextCardToRepeat(): BeRespose<GetNextCardToRepeatResp> {
        return selectTopOverdueCards(maxCardsNum = Int.MAX_VALUE).map { cardsToRepeat ->
            val rows = cardsToRepeat.rows
            if (rows.isNotEmpty()) {
                val topCards = rows.asSequence().filter { it.overdue >= rows[0].overdue }.toList()
                val nextCard = topCards[Random.nextInt(topCards.size)]
                GetNextCardToRepeatResp(
                    cardId = nextCard.cardId,
                    cardType = nextCard.cardType,
                    cardsRemain = rows.size,
                    isCardsRemainExact = cardsToRepeat.allRawsRead
                )
            } else {
                val currTime = clock.instant().toEpochMilli()
                GetNextCardToRepeatResp(
                    cardsRemain = 0,
                    nextCardIn = selectMinNextAccessAt().map { Utils.millisToDurationStr(it - currTime) }.orElse("")
                )
            }
        }.apply(toBeResponse(GET_NEXT_CARD_TO_REPEAT))
    }

    data class ReadTranslateCardByIdArgs(val cardId: Long)
    private val readTranslateCardByIdQuery = """
        select
            c.${c.id},
            s.${s.updatedAt},
            c.${c.createdAt},
            c.${c.paused},
            (select group_concat(ctg.${ctg.tagId}) from $ctg ctg where ctg.${ctg.cardId} = c.${c.id}) as tagIds,
            s.${s.delay},
            s.${s.nextAccessInMillis},
            s.${s.nextAccessAt},
            t.${t.textToTranslate}, 
            t.${t.translation} 
        from 
            $c c
            left join $s s on c.${c.id} = s.${s.cardId}
            left join $t t on c.${c.id} = t.${t.cardId}
        where c.${c.id} = ?
    """.trimIndent()
    @BeMethod
    @Synchronized
    fun readTranslateCardById(args: ReadTranslateCardByIdArgs): BeRespose<TranslateCard> {
        return getRepo().readableDatabase.doInTransaction {
            val currTime = clock.instant().toEpochMilli()
            select(query = readTranslateCardByIdQuery, args = arrayOf(args.cardId.toString())){
                val cardId = it.getLong()
                val updatedAt = it.getLong()
                TranslateCard(
                    id = cardId,
                    createdAt = it.getLong(),
                    paused = it.getLong() == 1L,
                    tagIds = (it.getStringOrNull()?:"").splitToSequence(",").filter { it.isNotBlank() }.map { it.toLong() }.toList(),
                    schedule = CardSchedule(
                        cardId = cardId,
                        updatedAt = updatedAt,
                        delay = it.getString(),
                        nextAccessInMillis = it.getLong(),
                        nextAccessAt = it.getLong(),
                    ),
                    timeSinceLastCheck = Utils.millisToDurationStr(currTime - updatedAt),
                    textToTranslate = it.getString(),
                    translation = it.getString(),
                )
            }.rows[0]
        }.apply(toBeResponse(READ_TRANSLATE_CARD_BY_ID))
    }

    data class ReadTranslateCardsByFilter(
        val tagIdsToInclude: Set<Long>? = null,
        val tagIdsToExclude: Set<Long>? = null,
        val paused: Boolean? = null,
        val textToTranslateContains: String? = null,
        val textToTranslateLengthLessThan: Long? = null,
        val textToTranslateLengthGreaterThan: Long? = null,
        val translationContains: String? = null,
        val translationLengthLessThan: Long? = null,
        val translationLengthGreaterThan: Long? = null,
        val createdFrom: Long? = null,
        val createdTill: Long? = null,
        val rowsLimit: Long? = null,
        val sortBy: TranslateCardSortBy? = null,
        val sortDir: SortDirection? = null,
    )
    @BeMethod
    @Synchronized
    fun readTranslateCardsByFilter(args: ReadTranslateCardsByFilter): BeRespose<ReadTranslateCardsByFilterResp> {
        val tagIdsToInclude: List<Long>? = args.tagIdsToInclude?.toList()
        val leastUsedTagId: Long? = if (tagIdsToInclude == null || tagIdsToInclude.isEmpty()) {
            null
        } else {
            tagsStat.getLeastUsedTagId(tagIdsToInclude)
        }
        fun havingFilterForTag(tagId:Long, include: Boolean) =
            "max(case when ctg.${ctg.tagId} = $tagId then 1 else 0 end) = ${if (include) "1" else "0"}"
        fun havingFilterForTags(tagIds:Sequence<Long>, include: Boolean) =
            tagIds.map { havingFilterForTag(tagId = it, include = include) }.joinToString(" and ")
        val havingFilters = ArrayList<String>()
        if (tagIdsToInclude != null && tagIdsToInclude.size > 1) {
            havingFilters.add(havingFilterForTags(
                tagIds = tagIdsToInclude.asSequence().filter { it != leastUsedTagId },
                include = true
            ))
        }
        if (args.tagIdsToExclude != null && args.tagIdsToExclude.isNotEmpty()) {
            havingFilters.add(havingFilterForTags(
                tagIds = args.tagIdsToExclude.asSequence(),
                include = false
            ))
        }
        val whereFilters = ArrayList<String>()
        val queryArgs = ArrayList<String>()
        if (args.paused != null) {
            whereFilters.add("c.${c.paused} = ${if(args.paused) 1 else 0}")
        }
        if (args.textToTranslateContains != null) {
            whereFilters.add("lower(t.${t.textToTranslate}) like ?")
            queryArgs.add("%${args.textToTranslateContains.lowercase()}%")
        }
        if (args.textToTranslateLengthLessThan != null) {
            whereFilters.add("length(t.${t.textToTranslate}) < ${args.textToTranslateLengthLessThan}")
        }
        if (args.textToTranslateLengthGreaterThan != null) {
            whereFilters.add("length(t.${t.textToTranslate}) > ${args.textToTranslateLengthGreaterThan}")
        }
        if (args.translationContains != null) {
            whereFilters.add("lower(t.${t.translation}) like ?")
            queryArgs.add("%${args.translationContains.lowercase()}%")
        }
        if (args.translationLengthLessThan != null) {
            whereFilters.add("length(t.${t.translation}) < ${args.translationLengthLessThan}")
        }
        if (args.translationLengthGreaterThan != null) {
            whereFilters.add("length(t.${t.translation}) > ${args.translationLengthGreaterThan}")
        }
        if (args.createdFrom != null) {
            whereFilters.add("c.${c.createdAt} >= ${args.createdFrom}")
        }
        if (args.createdTill != null) {
            whereFilters.add("c.${c.createdAt} <= ${args.createdTill}")
        }

        var query = """
            select
                c.${c.id},
                s.${s.updatedAt},
                c.${c.createdAt},
                c.${c.paused},
                c.tagIds,
                s.${s.delay},
                s.${s.nextAccessInMillis},
                s.${s.nextAccessAt},
                t.${t.textToTranslate}, 
                t.${t.translation}
            from
                (
                    select
                        c.${c.id},
                        c.${c.createdAt},
                        max(c.${c.paused}) ${c.paused},
                        group_concat(ctg.${ctg.tagId}) as tagIds
                    from $c c left join $ctg ctg on c.${c.id} = ctg.${ctg.cardId}
                        ${if (leastUsedTagId == null) "" else "inner join $ctg tg_incl on c.${c.id} = tg_incl.${ctg.cardId} and tg_incl.${ctg.tagId} = $leastUsedTagId"}
                    group by c.${c.id}
                    ${if (havingFilters.isEmpty()) "" else havingFilters.joinToString(prefix = "having ", separator = " and ")}
                ) c
                left join $s s on c.${c.id} = s.${s.cardId}
                left join $t t on c.${c.id} = t.${t.cardId}
            ${if (whereFilters.isEmpty()) "" else whereFilters.joinToString(prefix = "where ", separator = " and ")}
        """.trimIndent()

        return getRepo().readableDatabase.doInTransaction {
            val currTime = clock.instant().toEpochMilli()
            val result = select(query = query, args = queryArgs.toTypedArray()) {
                val cardId = it.getLong()
                val updatedAt = it.getLong()
                TranslateCard(
                    id = cardId,
                    createdAt = it.getLong(),
                    paused = it.getLong() == 1L,
                    tagIds = (it.getStringOrNull()?:"").splitToSequence(",").filter { it.isNotBlank() }.map { it.toLong() }.toList(),
                    schedule = CardSchedule(
                        cardId = cardId,
                        updatedAt = updatedAt,
                        delay = it.getString(),
                        nextAccessInMillis = it.getLong(),
                        nextAccessAt = it.getLong(),
                    ),
                    timeSinceLastCheck = Utils.millisToDurationStr(currTime - updatedAt),
                    textToTranslate = it.getString(),
                    translation = it.getString(),
                )
            }.rows
            ReadTranslateCardsByFilterResp(cards = result)
        }.apply(toBeResponse(READ_TRANSLATE_CARD_BY_FILTER))
    }

    data class ReadTranslateCardHistoryArgs(val cardId:Long)
    private val getTranslateCardHistoryQuery =
        "select ${l.recId}, ${l.cardId}, ${l.timestamp}, ${l.translation}, ${l.matched} from $l where ${l.cardId} = ? order by ${l.timestamp} desc"
    private val getTranslateCardHistoryQueryColumnNames = arrayOf(l.recId, l.cardId, l.timestamp, l.translation, l.matched)
    @BeMethod
    @Synchronized
    fun readTranslateCardHistory(args: ReadTranslateCardHistoryArgs): BeRespose<TranslateCardHistResp> {
        return getRepo().writableDatabase.doInTransaction {
            val historyRecords = select(
                rowsMax = 30,
                query = getTranslateCardHistoryQuery,
                args = arrayOf(args.cardId.toString()),
                columnNames = getTranslateCardHistoryQueryColumnNames,
                rowMapper = {
                    TranslateCardHistRecord(
                        recId = it.getLong(),
                        cardId = it.getLong(),
                        timestamp = it.getLong(),
                        translation = it.getString(),
                        isCorrect = it.getLong() == 1L,
                    )
                }
            )
            TranslateCardHistResp(
                historyRecords = historyRecords.rows,
                isHistoryFull = historyRecords.allRawsRead
            )
        }.apply(toBeResponse(GET_TRANSLATE_CARD_HISTORY))
    }

    data class UpdateTranslateCardArgs(
        val cardId:Long,
        val paused: Boolean? = null,
        val delay: String? = null,
        val recalculateDelay: Boolean = false,
        val tagIds: Set<Long>? = null,
        val textToTranslate:String? = null,
        val translation:String? = null
    )
    private val updateTranslateCardQuery = "select ${t.textToTranslate}, ${t.translation} from $t where ${t.cardId} = ?"
    private val updateTranslateCardQueryColumnNames = arrayOf(t.textToTranslate, t.translation)
    @BeMethod
    @Synchronized
    fun updateTranslateCard(args: UpdateTranslateCardArgs): BeRespose<Unit> {
        val repo = getRepo()
        return repo.writableDatabase.doInTransaction {
            updateCard(cardId = args.cardId, delay = args.delay, recalculateDelay = args.recalculateDelay, tagIds = args.tagIds, paused = args.paused)
            val (existingTextToTranslate: String, existingTranslation: String) = select(
                query = updateTranslateCardQuery,
                args = arrayOf(args.cardId.toString()),
                columnNames = updateTranslateCardQueryColumnNames,
            ) {
                listOf(it.getString(), it.getString())
            }.rows[0]
            val newTextToTranslate = args.textToTranslate?.trim()?:existingTextToTranslate
            val newTranslation = args.translation?.trim()?:existingTranslation
            if (newTextToTranslate.isEmpty()) {
                throw MemoryRefreshException(errCode = UPDATE_TRANSLATE_CARD_TEXT_TO_TRANSLATE_IS_EMPTY, msg = "Text to translate should not be empty.")
            } else if (newTranslation.isEmpty()) {
                throw MemoryRefreshException(errCode = UPDATE_TRANSLATE_CARD_TRANSLATION_IS_EMPTY, msg = "Translation should not be empty.")
            }
            if (newTextToTranslate != existingTextToTranslate || newTranslation != existingTranslation) {
                repo.translationCards.update(cardId = args.cardId, textToTranslate = newTextToTranslate, translation = newTranslation)
            }

            Unit
        }.apply(toBeResponse(UPDATE_TRANSLATE_CARD_EXCEPTION))
    }

    data class ValidateTranslateCardArgs(val cardId:Long, val userProvidedTranslation:String)
    private val validateTranslateCardQuery = "select ${t.translation} expectedTranslation from $t where ${t.cardId} = ?"
    private val validateTranslateCardQueryColumnNames = arrayOf("expectedTranslation")
    @BeMethod
    @Synchronized
    fun validateTranslateCard(args: ValidateTranslateCardArgs): BeRespose<ValidateTranslateCardAnswerResp> {
        val userProvidedTranslation = args.userProvidedTranslation.trim()
        return if (userProvidedTranslation.isBlank()) {
            BeRespose(err = BeErr(code = VALIDATE_TRANSLATE_CARD_TRANSLATION_IS_EMPTY.code, msg = "Translation should not be empty."))
        } else {
            val repo = getRepo()
            repo.writableDatabase.doInTransaction {
                val expectedTranslation = select(
                    query = validateTranslateCardQuery,
                    args = arrayOf(args.cardId.toString()),
                    columnNames = validateTranslateCardQueryColumnNames,
                    rowMapper = {it.getString()}
                ).rows[0].trim()
                val translationIsCorrect = userProvidedTranslation == expectedTranslation
                repo.translationCardsLog.insert(
                    cardId = args.cardId,
                    translation = userProvidedTranslation,
                    matched = translationIsCorrect
                )
                ValidateTranslateCardAnswerResp(
                    isCorrect = translationIsCorrect,
                    answer = expectedTranslation
                )
            }.apply(toBeResponse(VALIDATE_TRANSLATE_CARD_EXCEPTION))
        }
    }

    data class DeleteTranslateCardArgs(val cardId:Long)
    @BeMethod
    @Synchronized
    fun deleteTranslateCard(args: DeleteTranslateCardArgs): BeRespose<Boolean> {
        val repo = getRepo()
        return repo.writableDatabase.doInTransaction {
            repo.translationCards.delete(cardId = args.cardId)
            deleteCard(cardId = args.cardId)
            true
        }.apply(toBeResponse(DELETE_TRANSLATE_CARD_EXCEPTION))
    }

    //------------------------------------------------------------------------------------------------------------------

    @Synchronized
    private fun deleteCard(cardId: Long) {
        val repo = getRepo()
        repo.writableDatabase.doInTransaction {
            repo.cardToTag.delete(cardId = cardId)
            tagsStat.tagsCouldChange()
            repo.cardsSchedule.delete(cardId = cardId)
            repo.cards.delete(id = cardId)
        }
    }

    private val selectMinNextAccessAtQuery = "select count(1) cnt, min(${s.nextAccessAt}) nextAccessAt from $s"
    private val selectMinNextAccessAtQueryColumnNames = arrayOf("cnt", "nextAccessAt")
    @Synchronized
    private fun selectMinNextAccessAt(): Optional<Long> {
        return getRepo().readableDatabase.doInTransaction {
            select(
                query = selectMinNextAccessAtQuery,
                columnNames = selectMinNextAccessAtQueryColumnNames,
                rowMapper = { Pair(it.getLong(), it.getLong()) }
            )
        }
            .map { if (it.rows[0].first == 0L) Optional.empty() else Optional.of(it.rows[0].second) }
            .get()
    }

    @Synchronized
    private fun createCard(cardType: CardType, paused: Boolean, tagIds: Set<Long>): Try<Long> {
        tagsStat.tagsCouldChange()
        val repo = getRepo()
        return repo.writableDatabase.doInTransaction {
            val currTime = clock.instant().toEpochMilli()
            val cardId = repo.cards.insert(cardType = cardType, paused = paused)
            repo.cardsSchedule.insert(cardId = cardId, timestamp = currTime, delay = "0s", randomFactor = 1.0, nextAccessInMillis = 0, nextAccessAt = currTime)
            tagIds.forEach { repo.cardToTag.insert(cardId = cardId, tagId = it) }
            cardId
        }
    }

    private val selectTopOverdueCardsQuery = """
        select * from ( 
            select
                ${c.id} cardId,
                ${c.type} cardType,
                case 
                    when ? /*1 currTime*/ < ${s.nextAccessAt} 
                        then -1.0 
                    else 
                        (? /*2 currTime*/ - ${s.nextAccessAt} ) * 1.0 / (case when ${s.nextAccessInMillis} = 0 then 1 else ${s.nextAccessInMillis} end) 
                end overdue
            from $c left join $s on ${c.id} = ${s.cardId}
        )
        where overdue >= 0
        order by overdue desc
    """.trimIndent()
    private val selectTopOverdueCardsQueryColumnNames = arrayOf("cardId", "cardType", "overdue")
    @Synchronized
    fun selectTopOverdueCards(maxCardsNum: Int): Try<SelectedRows<CardOverdue>> {
        return getRepo().readableDatabase.doInTransaction {
            val currTimeStr = clock.instant().toEpochMilli().toString()
            select(
                query = selectTopOverdueCardsQuery,
                args = arrayOf(currTimeStr, currTimeStr),
                columnNames = selectTopOverdueCardsQueryColumnNames,
                rowMapper = {
                    CardOverdue(
                        cardId = it.getLong(),
                        cardType = CardType.fromInt(it.getLong()),
                        overdue = it.getDouble()
                    )
                },
                rowsMax = maxCardsNum
            )
        }
    }

    @Synchronized
    private fun updateCard(
        cardId:Long,
        tagIds: Set<Long>?,
        paused: Boolean?,
        delay: String?,
        recalculateDelay: Boolean
    ) {
        val repo = getRepo()
        repo.writableDatabase.doInTransaction {
            readCardById(cardId = cardId).map { existingCard: Card ->
                val newDelay = delay?.trim()?:existingCard.schedule.delay
                if (newDelay.isEmpty()) {
                    throw MemoryRefreshException(errCode = UPDATE_CARD_DELAY_IS_EMPTY, msg = "Delay should not be empty.")
                }
                if (recalculateDelay || newDelay != existingCard.schedule.delay) {
                    val randomFactor = 0.85 + Random.nextDouble(from = 0.0, until = 0.30001)
                    val nextAccessInMillis = (Utils.delayStrToMillis(newDelay) * randomFactor).toLong()
                    val timestamp = clock.instant().toEpochMilli()
                    val nextAccessAt = timestamp + nextAccessInMillis
                    repo.cardsSchedule.update(
                        timestamp = timestamp,
                        cardId = cardId,
                        delay = newDelay,
                        randomFactor = randomFactor,
                        nextAccessInMillis = nextAccessInMillis,
                        nextAccessAt = nextAccessAt
                    )
                }
                if (tagIds != null && tagIds != existingCard.tagIds.toSet()) {
                    repo.cardToTag.delete(cardId = cardId)
                    tagIds.forEach { repo.cardToTag.insert(cardId = cardId, tagId = it) }
                }
                if (paused != null && paused != existingCard.paused) {
                    repo.cards.update(id = cardId, cardType = existingCard.type, paused = paused)
                }
                Unit
            }
        }
    }

    private val readCardByIdQuery = """
        select
            c.${c.id},
            c.${c.type},
            c.${c.paused},
            (select group_concat(ctg.${ctg.tagId}) from $ctg ctg where ctg.${ctg.cardId} = c.${c.id}) as tagIds,
            s.${s.updatedAt},
            s.${s.delay},
            s.${s.nextAccessInMillis},
            s.${s.nextAccessAt}
        from 
            $c c
            left join $s s on c.${c.id} = s.${s.cardId}
        where c.${c.id} = ?
    """.trimIndent()
    @Synchronized
    private fun readCardById(cardId: Long): Try<Card> {
        return getRepo().readableDatabase.doInTransaction {
            select(
                query = readCardByIdQuery,
                args = arrayOf(cardId.toString()),
                rowMapper = {
                    val cardId = it.getLong()
                    Card(
                        id = cardId,
                        type = CardType.fromInt(it.getLong()),
                        paused = it.getLong() == 1L,
                        tagIds = (it.getStringOrNull()?:"").splitToSequence(",").filter { it.isNotBlank() }.map { it.toLong() }.toList(),
                        schedule = CardSchedule(
                            cardId = cardId,
                            updatedAt = it.getLong(),
                            delay = it.getString(),
                            nextAccessInMillis = it.getLong(),
                            nextAccessAt = it.getLong(),
                        )
                    )
                }
            ).rows[0]
        }
    }

}