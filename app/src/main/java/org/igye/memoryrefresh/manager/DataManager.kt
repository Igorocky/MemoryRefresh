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
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
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

    private val readAllTagsQuery = "select ${tg.id}, ${tg.name} from $tg order by ${tg.name}"
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

    private val getCardToTagMappingQuery = "select ${ctg.cardId}, ${ctg.tagId} from $ctg order by ${ctg.cardId}"
    @BeMethod
    @Synchronized
    fun getCardToTagMapping(): BeRespose<Map<Long,List<Long>>> {
        var cardId: Long? = null
        val tagIds = ArrayList<Long>(10)
        val result = HashMap<Long,List<Long>>()
        return Try {
            getRepo().readableDatabase.select(
                query = getCardToTagMappingQuery,
            ) {
                val cid = it.getLong()
                if (cardId == null) {
                    cardId = cid
                }
                if (cardId != null && cardId != cid) {
                    result[cardId!!] = ArrayList(tagIds)
                    cardId = cid
                    tagIds.clear()
                }
                tagIds.add(it.getLong())
            }
            if (cardId != null) {
                result[cardId!!] = ArrayList(tagIds)
            }
            result
        }.apply(toBeResponse(GET_CARD_TO_TAG_MAPPING))
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
    fun deleteTag(args:DeleteTagArgs): BeRespose<Unit> {
        val repo = getRepo()
        return repo.writableDatabase.doInTransaction {
            repo.tags.delete(id = args.tagId)
            Unit
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

    data class ReadTranslateCardByIdArgs(val cardId: Long)
    private val readTranslateCardByIdQuery = """
        select
            c.${c.id},
            s.${s.updatedAt},
            s.${s.nextAccessAt},
            c.${c.createdAt},
            c.${c.paused},
            (? - s.${s.nextAccessAt} ) * 1.0 / (case when s.${s.nextAccessInMillis} = 0 then 1 else s.${s.nextAccessInMillis} end),
            (select group_concat(ctg.${ctg.tagId}) from $ctg ctg where ctg.${ctg.cardId} = c.${c.id}) as tagIds,
            s.${s.delay},
            s.${s.nextAccessInMillis},
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
            select(query = readTranslateCardByIdQuery, args = arrayOf(currTime.toString(), args.cardId.toString())){
                val cardId = it.getLong()
                val updatedAt = it.getLong()
                val nextAccessAt = it.getLong()
                TranslateCard(
                    id = cardId,
                    createdAt = it.getLong(),
                    paused = it.getLong() == 1L,
                    overdue = it.getDouble(),
                    tagIds = (it.getStringOrNull()?:"").splitToSequence(",").filter { it.isNotBlank() }.map { it.toLong() }.toList(),
                    schedule = CardSchedule(
                        cardId = cardId,
                        updatedAt = updatedAt,
                        delay = it.getString(),
                        nextAccessInMillis = it.getLong(),
                        nextAccessAt = nextAccessAt,
                    ),
                    timeSinceLastCheck = Utils.millisToDurationStr(currTime - updatedAt),
                    activatesIn = Utils.millisToDurationStr(nextAccessAt - currTime),
                    textToTranslate = it.getString(),
                    translation = it.getString(),
                )
            }.rows[0]
        }.apply(toBeResponse(READ_TRANSLATE_CARD_BY_ID))
    }

    @BeMethod
    @Synchronized
    fun readTranslateCardsByFilter(args: ReadTranslateCardsByFilterArgs): BeRespose<ReadTranslateCardsByFilterResp> {
        return readTranslateCardsByFilterInner(args).apply(toBeResponse(READ_TRANSLATE_CARD_BY_FILTER))
    }

    data class ReadTranslateCardHistoryArgs(val cardId:Long)
    private val getValidationHistoryQuery =
        "select ${l.recId}, ${l.cardId}, ${l.timestamp}, ${l.translation}, ${l.matched} from $l where ${l.cardId} = ? order by ${l.timestamp} desc"
    private val getDataHistoryQuery =
        "select ${t.ver.verId}, ${t.cardId}, ${t.ver.timestamp}, ${t.textToTranslate}, ${t.translation} from ${t.ver} where ${t.cardId} = ? order by ${t.ver.timestamp} desc"
    @BeMethod
    @Synchronized
    fun readTranslateCardHistory(args: ReadTranslateCardHistoryArgs): BeRespose<TranslateCardHistResp> {
        return getRepo().writableDatabase.doInTransaction {
            val card: TranslateCard = readTranslateCardById(ReadTranslateCardByIdArgs(cardId = args.cardId)).data!!
            val cardIdArgs = arrayOf(args.cardId.toString())
            val validationHistory: List<TranslateCardValidationHistRecord> = select(
                query = getValidationHistoryQuery,
                args = cardIdArgs,
                rowMapper = {
                    TranslateCardValidationHistRecord(
                        recId = it.getLong(),
                        cardId = it.getLong(),
                        timestamp = it.getLong(),
                        translation = it.getString(),
                        isCorrect = it.getLong() == 1L,
                    )
                }
            ).rows
            val dataHistory: ArrayList<TranslateCardHistRecord> = ArrayList(
                select(
                    query = getDataHistoryQuery,
                    args = cardIdArgs,
                    rowMapper = {
                        TranslateCardHistRecord(
                            verId = it.getLong(),
                            cardId = it.getLong(),
                            timestamp = it.getLong(),
                            textToTranslate = it.getString(),
                            translation = it.getString(),
                            validationHistory = ArrayList()
                        )
                    }
                ).rows
            )
            prepareTranslateCardHistResp(card, dataHistory, validationHistory)
        }.apply(toBeResponse(GET_TRANSLATE_CARD_HISTORY))
    }

    data class SelectTopOverdueTranslateCardsArgs(
        val tagIdsToInclude: Set<Long>? = null,
        val tagIdsToExclude: Set<Long>? = null,
        val translationLengthLessThan: Long? = null,
        val translationLengthGreaterThan: Long? = null,
    )
    @BeMethod
    @Synchronized
    fun selectTopOverdueTranslateCards(args: SelectTopOverdueTranslateCardsArgs = SelectTopOverdueTranslateCardsArgs()): BeRespose<ReadTopOverdueTranslateCardsResp> {
        val filterArgs = ReadTranslateCardsByFilterArgs(
            paused = false,
            overdueGreaterEq = 0.0,
            tagIdsToInclude = args.tagIdsToInclude,
            tagIdsToExclude = args.tagIdsToExclude,
            translationLengthLessThan = args.translationLengthLessThan,
            translationLengthGreaterThan = args.translationLengthGreaterThan,
            sortBy = TranslateCardSortBy.OVERDUE,
            sortDir = SortDirection.DESC,
            rowsLimit = 20
        )
        return readTranslateCardsByFilterInner(filterArgs).map { it.cards }.map { overdueCards ->
            if (overdueCards.isEmpty()) {
                val waitingCards = readTranslateCardsByFilterInner(
                    filterArgs.copy(
                        overdueGreaterEq = null,
                        rowsLimit = 1,
                        sortBy = TranslateCardSortBy.NEXT_ACCESS_AT,
                        sortDir = SortDirection.ASC
                    )
                ).get().cards
                if (waitingCards.isEmpty()) {
                    ReadTopOverdueTranslateCardsResp()
                } else {
                    ReadTopOverdueTranslateCardsResp(
                        nextCardIn = Utils.millisToDurationStr(waitingCards[0].schedule.nextAccessAt - clock.instant().toEpochMilli())
                    )
                }
            } else {
                ReadTopOverdueTranslateCardsResp(cards = overdueCards)
            }
        }.apply(toBeResponse(READ_TOP_OVERDUE_TRANSLATE_CARDS))
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

    data class ReadTranslateCardsByFilterArgs(
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
        val overdueGreaterEq: Double? = null,
        val rowsLimit: Long? = null,
        val sortBy: TranslateCardSortBy? = null,
        val sortDir: SortDirection? = null,
    )
    @Synchronized
    private fun readTranslateCardsByFilterInner(args: ReadTranslateCardsByFilterArgs): Try<ReadTranslateCardsByFilterResp> {
        val tagIdsToInclude: List<Long>? = args.tagIdsToInclude?.toList()
        val leastUsedTagId: Long? = if (tagIdsToInclude == null || tagIdsToInclude.isEmpty()) {
            null
        } else {
            tagsStat.getLeastUsedTagId(tagIdsToInclude)
        }
        val currTime = clock.instant().toEpochMilli()
        val overdueFormula = "(($currTime - s.${s.nextAccessAt} ) * 1.0 / (case when s.${s.nextAccessInMillis} = 0 then 1 else s.${s.nextAccessInMillis} end))"
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
        if (args.overdueGreaterEq != null) {
            whereFilters.add("$overdueFormula >= ${args.overdueGreaterEq}")
        }
        var orderBy = ""
        if (args.sortBy != null) {
            orderBy = "order by " + when (args.sortBy) {
                TranslateCardSortBy.TIME_CREATED -> "c.${c.createdAt}"
                TranslateCardSortBy.OVERDUE -> overdueFormula
                TranslateCardSortBy.NEXT_ACCESS_AT -> "s.${s.nextAccessAt}"
            } + " " + (args.sortDir?:SortDirection.ASC)
        }
        val rowNumLimit = if (args.rowsLimit == null) "" else "limit ${args.rowsLimit}"

        var query = """
            select
                c.${c.id},
                s.${s.updatedAt},
                s.${s.nextAccessAt},
                c.${c.createdAt},
                c.${c.paused},
                $overdueFormula overdue,
                c.tagIds,
                s.${s.delay},
                s.${s.nextAccessInMillis},
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
            $orderBy
            $rowNumLimit
        """.trimIndent()

        return getRepo().readableDatabase.doInTransaction {
            val currTime = clock.instant().toEpochMilli()
            val result = select(query = query, args = queryArgs.toTypedArray()) {
                val cardId = it.getLong()
                val updatedAt = it.getLong()
                val nextAccessAt = it.getLong()
                TranslateCard(
                    id = cardId,
                    createdAt = it.getLong(),
                    paused = it.getLong() == 1L,
                    overdue = it.getDouble(),
                    tagIds = (it.getStringOrNull()?:"").splitToSequence(",").filter { it.isNotBlank() }.map { it.toLong() }.toList(),
                    schedule = CardSchedule(
                        cardId = cardId,
                        updatedAt = updatedAt,
                        delay = it.getString(),
                        nextAccessInMillis = it.getLong(),
                        nextAccessAt = nextAccessAt,
                    ),
                    timeSinceLastCheck = Utils.millisToDurationStr(currTime - updatedAt),
                    activatesIn = Utils.millisToDurationStr(nextAccessAt - currTime),
                    textToTranslate = it.getString(),
                    translation = it.getString(),
                )
            }.rows
            ReadTranslateCardsByFilterResp(cards = result)
        }
    }

    private fun prepareTranslateCardHistResp(
        card: TranslateCard,
        dataHistory: ArrayList<TranslateCardHistRecord>,
        validationHistory: List<TranslateCardValidationHistRecord>
    ): TranslateCardHistResp {
        dataHistory.add(0, TranslateCardHistRecord(
            verId = -1,
            cardId = card.id,
            timestamp = if (dataHistory.isEmpty()) card.createdAt else dataHistory[0].timestamp,
            textToTranslate = card.textToTranslate,
            translation = card.translation,
            validationHistory = ArrayList()
        ))
        for (i in 1 .. dataHistory.size-2) {
            val dataHistRec = dataHistory.removeAt(i)
            dataHistory.add(i, dataHistRec.copy(timestamp = dataHistory[i].timestamp))
        }
        if (dataHistory.isNotEmpty()) {
            val lastDataHistRec = dataHistory.removeLast()
            dataHistory.add(lastDataHistRec.copy(timestamp = card.createdAt))
        }

        var dataIdx = 0
        for (validation in validationHistory) {
            while (validation.timestamp < dataHistory[dataIdx].timestamp) {
                dataIdx++
            }
            dataHistory[dataIdx].validationHistory.add(validation)
        }
        return TranslateCardHistResp(
            isHistoryFull = true,
            dataHistory = dataHistory
        )
    }

}