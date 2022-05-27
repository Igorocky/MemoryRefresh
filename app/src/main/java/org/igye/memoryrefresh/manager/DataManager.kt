package org.igye.memoryrefresh.manager

import android.database.sqlite.SQLiteConstraintException
import org.igye.memoryrefresh.ErrorCode.*
import org.igye.memoryrefresh.common.BeMethod
import org.igye.memoryrefresh.common.MemoryRefreshException
import org.igye.memoryrefresh.common.Utils
import org.igye.memoryrefresh.common.Utils.delayStrToMillis
import org.igye.memoryrefresh.common.Utils.multiplyDelay
import org.igye.memoryrefresh.database.CardType
import org.igye.memoryrefresh.database.TranslationCardDirection
import org.igye.memoryrefresh.database.doInTransaction
import org.igye.memoryrefresh.database.select
import org.igye.memoryrefresh.dto.common.BeErr
import org.igye.memoryrefresh.dto.common.BeRespose
import org.igye.memoryrefresh.dto.domain.*
import java.time.Clock
import kotlin.random.Random


class DataManager(
    private val clock: Clock,
    private val repositoryManager: RepositoryManager,
    private val settingsManager: SettingsManager,
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
            return BeRespose(
                errCode = SAVE_NEW_TAG,
                errHandler = { ex ->
                    if (ex is SQLiteConstraintException && (ex.message?:"").contains("UNIQUE constraint failed: TAGS.NAME")) {
                        throw MemoryRefreshException(
                            errCode = SAVE_NEW_TAG_NAME_IS_NOT_UNIQUE,
                            msg = "A tag with name '$name' already exists."
                        )
                    } else {
                        throw ex
                    }
                }
            ) {
                val repo = getRepo()
                repo.writableDatabase.doInTransaction {
                    tagsStat.tagsCouldChange()
                    repo.tags.insert(name = name)
                }
            }
        }
    }

    private val readAllTagsQuery = "select ${tg.id}, ${tg.name} from $tg order by ${tg.name}"
    private val readAllTagsColumnNames = arrayOf(tg.id, tg.name)
    @BeMethod
    @Synchronized
    fun readAllTags(): BeRespose<List<Tag>> {
        return BeRespose(READ_ALL_TAGS) {
            getRepo().readableDatabase.select(
                query = readAllTagsQuery,
                columnNames = readAllTagsColumnNames
            ) {
                Tag(id = it.getLong(), name = it.getString())
            }.rows
        }
    }

    private val getCardToTagMappingQuery = "select ${ctg.cardId}, ${ctg.tagId} from $ctg order by ${ctg.cardId}"
    @BeMethod
    @Synchronized
    fun getCardToTagMapping(): BeRespose<Map<Long,List<Long>>> {
        return BeRespose(GET_CARD_TO_TAG_MAPPING) {
            var cardId: Long? = null
            val tagIds = ArrayList<Long>(10)
            val result = HashMap<Long,List<Long>>()
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
        }
    }

    data class UpdateTagArgs(val tagId:Long, val name:String)
    @BeMethod
    fun updateTag(args:UpdateTagArgs): BeRespose<Tag> {
        val newName = args.name.trim()
        return if (newName.isBlank()) {
            BeRespose(err = BeErr(code = UPDATE_TAG_NAME_IS_EMPTY.code, msg = "Name of a tag should not be empty."))
        } else {
            return BeRespose(
                errCode = UPDATE_TAG,
                errHandler = { ex ->
                    throw if (ex is SQLiteConstraintException && (ex.message?:"").contains("UNIQUE constraint failed: TAGS.NAME")) {
                        MemoryRefreshException(
                            errCode = UPDATE_TAG_NAME_IS_NOT_UNIQUE,
                            msg = "A tag with name '$newName' already exists."
                        )
                    } else {
                        ex
                    }
                }
            ) {
                val repo = getRepo()
                repo.writableDatabase.doInTransaction {
                    repo.tags.update(id = args.tagId, name = newName)
                    Tag(
                        id = args.tagId,
                        name = newName
                    )
                }
            }
        }
    }

    data class DeleteTagArgs(val tagId:Long)
    @BeMethod
    fun deleteTag(args:DeleteTagArgs): BeRespose<Unit> {
        return BeRespose(
            errCode = DELETE_TAG,
            errHandler = { ex ->
                throw if (ex is SQLiteConstraintException && (ex.message?:"").contains("FOREIGN KEY constraint failed")) {
                    MemoryRefreshException(
                        errCode = DELETE_TAG_TAG_IS_USED,
                        msg = "Cannot delete tag because it is referenced by at least one card."
                    )
                } else {
                    ex
                }
            }
        ) {
            val repo = getRepo()
            repo.writableDatabase.doInTransaction {
                repo.tags.delete(id = args.tagId)
            }
        }
    }

    data class CreateTranslateCardArgs(
        val textToTranslate:String,
        val translation:String,
        val tagIds: Set<Long> = emptySet(),
        val paused: Boolean = false,
        val direction: TranslationCardDirection,
        val createReversedCard: Boolean = false,
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
            return BeRespose(SAVE_NEW_TRANSLATE_CARD_EXCEPTION) {
                repo.writableDatabase.doInTransaction {
                    val cardId = createCard(cardType = CardType.TRANSLATION, tagIds = args.tagIds, paused = args.paused)
                    repo.translationCards.insert(
                        cardId = cardId,
                        textToTranslate = textToTranslate,
                        translation = translation,
                        direction = args.direction,
                    )
                    if (args.createReversedCard) {
                        createTranslateCard(args.copy(direction = args.direction.reverse(), createReversedCard = false))
                    }
                    cardId
                }
            }
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
            c.${c.lastCheckedAt},
            (? - s.${s.nextAccessAt} ) * 1.0 / (case when s.${s.nextAccessInMillis} = 0 then 1 else s.${s.nextAccessInMillis} end),
            (select group_concat(ctg.${ctg.tagId}) from $ctg ctg where ctg.${ctg.cardId} = c.${c.id}) as tagIds,
            s.${s.origDelay},
            s.${s.delay},
            s.${s.nextAccessInMillis},
            t.${t.textToTranslate}, 
            t.${t.translation}, 
            t.${t.direction} 
        from 
            $c c
            left join $s s on c.${c.id} = s.${s.cardId}
            left join $t t on c.${c.id} = t.${t.cardId}
        where c.${c.id} = ?
    """.trimIndent()
    @BeMethod
    @Synchronized
    fun readTranslateCardById(args: ReadTranslateCardByIdArgs): BeRespose<TranslateCard> {
        return BeRespose(READ_TRANSLATE_CARD_BY_ID) {
            getRepo().readableDatabase.doInTransaction {
                val currTime = clock.instant().toEpochMilli()
                select(query = readTranslateCardByIdQuery, args = arrayOf(currTime.toString(), args.cardId.toString())){
                    val cardId = it.getLong()
                    val updatedAt = it.getLong()
                    val nextAccessAt = it.getLong()
                    TranslateCard(
                        id = cardId,
                        createdAt = it.getLong(),
                        paused = it.getLong() == 1L,
                        timeSinceLastCheck = Utils.millisToDurationStr(currTime - it.getLong()),
                        overdue = it.getDouble(),
                        tagIds = (it.getStringOrNull()?:"").splitToSequence(",").filter { it.isNotBlank() }.map { it.toLong() }.toList(),
                        schedule = CardSchedule(
                            cardId = cardId,
                            updatedAt = updatedAt,
                            origDelay = it.getString(),
                            delay = it.getString(),
                            nextAccessInMillis = it.getLong(),
                            nextAccessAt = nextAccessAt,
                        ),
                        activatesIn = if (nextAccessAt - currTime >= 0) Utils.millisToDurationStr(nextAccessAt - currTime) else "-",
                        textToTranslate = it.getString(),
                        translation = it.getString(),
                        direction = TranslationCardDirection.fromInt(it.getLong()),
                    )
                }.rows[0]
            }
        }
    }

    @BeMethod
    @Synchronized
    fun readTranslateCardsByFilter(args: ReadTranslateCardsByFilterArgs): BeRespose<ReadTranslateCardsByFilterResp> {
        return BeRespose(READ_TRANSLATE_CARD_BY_FILTER) {
            readTranslateCardsByFilterInner(args)
        }
    }

    data class ReadTranslateCardHistoryArgs(val cardId:Long)
    private val getValidationHistoryQuery =
        "select ${l.recId}, ${l.cardId}, ${l.timestamp}, ${l.translation}, ${l.matched} from $l where ${l.cardId} = ? order by ${l.timestamp} desc"
    private val getDataHistoryQuery =
        "select ${t.ver.verId}, ${t.cardId}, ${t.ver.timestamp}, ${t.textToTranslate}, ${t.translation} from ${t.ver} where ${t.cardId} = ? order by ${t.ver.timestamp} desc"
    @BeMethod
    @Synchronized
    fun readTranslateCardHistory(args: ReadTranslateCardHistoryArgs): BeRespose<TranslateCardHistResp> {
        return BeRespose(GET_TRANSLATE_CARD_HISTORY) {
            getRepo().writableDatabase.doInTransaction {
                val card: TranslateCard = readTranslateCardById(ReadTranslateCardByIdArgs(cardId = args.cardId)).data!!
                val cardIdArgs = arrayOf(args.cardId.toString())
                val validationHistory = ArrayList(select(
                    query = getValidationHistoryQuery,
                    args = cardIdArgs,
                    rowMapper = {
                        TranslateCardValidationHistRecord(
                            recId = it.getLong(),
                            cardId = it.getLong(),
                            timestamp = it.getLong(),
                            actualDelay = "",
                            translation = it.getString(),
                            isCorrect = it.getLong() == 1L,
                        )
                    }
                ).rows)
                val dataHistory: ArrayList<TranslateCardHistRecord> = ArrayList(select(
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
                ).rows)
                prepareTranslateCardHistResp(card, dataHistory, validationHistory)
            }
        }
    }

    data class SelectTopOverdueTranslateCardsArgs(
        val tagIdsToInclude: Set<Long>? = null,
        val tagIdsToExclude: Set<Long>? = null,
        val translationLengthLessThan: Long? = null,
        val translationLengthGreaterThan: Long? = null,
        val rowsLimit: Long = 5,
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
            rowsLimit = args.rowsLimit
        )
        return BeRespose(READ_TOP_OVERDUE_TRANSLATE_CARDS) {
            val overdueCards = readTranslateCardsByFilterInner(filterArgs).cards
            if (overdueCards.isEmpty()) {
                val waitingCards = readTranslateCardsByFilterInner(
                    filterArgs.copy(
                        overdueGreaterEq = null,
                        rowsLimit = 1,
                        sortBy = TranslateCardSortBy.NEXT_ACCESS_AT,
                        sortDir = SortDirection.ASC
                    )
                ).cards
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
        }
    }

    data class UpdateTranslateCardArgs(
        val cardId:Long,
        val paused: Boolean? = null,
        val delay: String? = null,
        val recalculateDelay: Boolean = false,
        val tagIds: Set<Long>? = null,
        val textToTranslate:String? = null,
        val translation:String? = null,
        val direction: TranslationCardDirection? = null,
        val updateReversedCardId: Boolean = false,
        val reversedCardId: Long? = null,
    )
    private val updateTranslateCardQuery = "select ${t.textToTranslate}, ${t.translation}, ${t.direction} from $t where ${t.cardId} = ?"
    private val updateTranslateCardQueryColumnNames = arrayOf(t.textToTranslate, t.translation, t.direction)
    @BeMethod
    @Synchronized
    fun updateTranslateCard(args: UpdateTranslateCardArgs): BeRespose<Unit> {
        return BeRespose(UPDATE_TRANSLATE_CARD_EXCEPTION) {
            val repo = getRepo()
            repo.writableDatabase.doInTransaction {
                updateCard(cardId = args.cardId, delay = args.delay, recalculateDelay = args.recalculateDelay, tagIds = args.tagIds, paused = args.paused)
                var existingTextToTranslate: String? = null
                var existingTranslation: String? = null
                var existingDirection: TranslationCardDirection? = null
                select(
                    query = updateTranslateCardQuery,
                    args = arrayOf(args.cardId.toString()),
                    columnNames = updateTranslateCardQueryColumnNames,
                ) {
                    existingTextToTranslate = it.getString()
                    existingTranslation = it.getString()
                    existingDirection = TranslationCardDirection.fromInt(it.getLong())
                }
                val newTextToTranslate: String = args.textToTranslate?.trim()?:existingTextToTranslate!!
                val newTranslation: String = args.translation?.trim()?:existingTranslation!!
                val newDirection: TranslationCardDirection = args.direction?:existingDirection!!
                if (newTextToTranslate.isEmpty()) {
                    throw MemoryRefreshException(errCode = UPDATE_TRANSLATE_CARD_TEXT_TO_TRANSLATE_IS_EMPTY, msg = "Text to translate should not be empty.")
                } else if (newTranslation.isEmpty()) {
                    throw MemoryRefreshException(errCode = UPDATE_TRANSLATE_CARD_TRANSLATION_IS_EMPTY, msg = "Translation should not be empty.")
                }
                if (
                    newTextToTranslate != existingTextToTranslate
                    || newTranslation != existingTranslation
                    || newDirection != existingDirection
                ) {
                    repo.translationCards.update(
                        cardId = args.cardId,
                        textToTranslate = newTextToTranslate,
                        translation = newTranslation,
                        direction = newDirection,
                    )
                }
            }
        }
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
            return BeRespose(VALIDATE_TRANSLATE_CARD_EXCEPTION) {
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
                    repo.cards.updateLastChecked(id = args.cardId, lastCheckedAt = clock.instant().toEpochMilli())
                    ValidateTranslateCardAnswerResp(
                        isCorrect = translationIsCorrect,
                        answer = expectedTranslation
                    )
                }
            }
        }
    }

    data class DeleteTranslateCardArgs(val cardId:Long)
    @BeMethod
    @Synchronized
    fun deleteTranslateCard(args: DeleteTranslateCardArgs): BeRespose<Unit> {
        return BeRespose(DELETE_TRANSLATE_CARD_EXCEPTION) {
            val repo = getRepo()
            repo.writableDatabase.doInTransaction {
                repo.translationCards.delete(cardId = args.cardId)
                deleteCard(cardId = args.cardId)
            }
        }
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
    private fun createCard(cardType: CardType, paused: Boolean, tagIds: Set<Long>): Long {
        tagsStat.tagsCouldChange()
        val repo = getRepo()
        return repo.writableDatabase.doInTransaction {
            val currTime = clock.instant().toEpochMilli()
            val cardId = repo.cards.insert(cardType = cardType, paused = paused)
            repo.cardsSchedule.insert(cardId = cardId, timestamp = currTime, origDelay = "1s", delay = "1s", randomFactor = 1.0, nextAccessInMillis = 1000, nextAccessAt = currTime+1000)
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
            val existingCard = readCardById(cardId = cardId)
            var origDelay = delay?.trim()?:existingCard.schedule.origDelay
            var newDelay = delay?.trim()?:existingCard.schedule.delay
            if (newDelay.isEmpty()) {
                throw MemoryRefreshException(errCode = UPDATE_CARD_DELAY_IS_EMPTY, msg = "Delay should not be empty.")
            }
            if (recalculateDelay || newDelay != existingCard.schedule.delay) {
                if (newDelay.startsWith("x")) {
                    newDelay = multiplyDelay(existingCard.schedule.delay, newDelay)
                }
                val randomFactor = 0.85 + Random.nextDouble(from = 0.0, until = 0.30001)
                val maxDelay = settingsManager.readMaxDelay().data!!
                val maxDelayMillis: Long = delayStrToMillis(maxDelay)
                var nextAccessInMillis = (delayStrToMillis(newDelay) * randomFactor).toLong()
                if (maxDelayMillis < nextAccessInMillis) {
                    val randomFactor = 0.9 + Random.nextDouble(from = 0.0, until = 0.1)
                    nextAccessInMillis = (maxDelayMillis * randomFactor).toLong()
                    newDelay = maxDelay
                    origDelay += ",max=$maxDelay"
                }
                val timestamp = clock.instant().toEpochMilli()
                val nextAccessAt = timestamp + nextAccessInMillis
                repo.cardsSchedule.update(
                    timestamp = timestamp,
                    cardId = cardId,
                    origDelay = origDelay,
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
                repo.cards.updatePaused(id = cardId, paused = paused)
            }
        }
    }

    private val readCardByIdQuery = """
        select
            c.${c.id},
            c.${c.type},
            c.${c.paused},
            c.${c.lastCheckedAt},
            (select group_concat(ctg.${ctg.tagId}) from $ctg ctg where ctg.${ctg.cardId} = c.${c.id}) as tagIds,
            s.${s.updatedAt},
            s.${s.origDelay},
            s.${s.delay},
            s.${s.nextAccessInMillis},
            s.${s.nextAccessAt}
        from 
            $c c
            left join $s s on c.${c.id} = s.${s.cardId}
        where c.${c.id} = ?
    """.trimIndent()
    @Synchronized
    private fun readCardById(cardId: Long): Card {
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
                        lastCheckedAt = it.getLong(),
                        tagIds = (it.getStringOrNull()?:"").splitToSequence(",").filter { it.isNotBlank() }.map { it.toLong() }.toList(),
                        schedule = CardSchedule(
                            cardId = cardId,
                            updatedAt = it.getLong(),
                            origDelay = it.getString(),
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
        val nextAccessFrom: Long? = null,
        val nextAccessTill: Long? = null,
        val rowsLimit: Long? = null,
        val sortBy: TranslateCardSortBy? = null,
        val sortDir: SortDirection? = null,
    )
    @Synchronized
    private fun readTranslateCardsByFilterInner(args: ReadTranslateCardsByFilterArgs): ReadTranslateCardsByFilterResp {
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
        if (args.nextAccessFrom != null) {
            whereFilters.add("s.${s.nextAccessAt} >= ${args.nextAccessFrom}")
        }
        if (args.nextAccessTill != null) {
            whereFilters.add("s.${s.nextAccessAt} <= ${args.nextAccessTill}")
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
                c.${c.lastCheckedAt},
                $overdueFormula overdue,
                c.tagIds,
                s.${s.origDelay},
                s.${s.delay},
                s.${s.nextAccessInMillis},
                t.${t.textToTranslate}, 
                t.${t.translation},
                t.${t.direction}
            from
                (
                    select
                        c.${c.id},
                        c.${c.createdAt},
                        max(c.${c.paused}) ${c.paused},
                        max(c.${c.lastCheckedAt}) ${c.lastCheckedAt},
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
                    timeSinceLastCheck = Utils.millisToDurationStr(currTime - it.getLong()),
                    overdue = it.getDouble(),
                    tagIds = (it.getStringOrNull()?:"").splitToSequence(",").filter { it.isNotBlank() }.map { it.toLong() }.toList(),
                    schedule = CardSchedule(
                        cardId = cardId,
                        updatedAt = updatedAt,
                        origDelay = it.getString(),
                        delay = it.getString(),
                        nextAccessInMillis = it.getLong(),
                        nextAccessAt = nextAccessAt,
                    ),
                    activatesIn = if (nextAccessAt - currTime >= 0) Utils.millisToDurationStr(nextAccessAt - currTime) else "-",
                    textToTranslate = it.getString(),
                    translation = it.getString(),
                    direction = TranslationCardDirection.fromInt(it.getLong()),
                )
            }.rows
            ReadTranslateCardsByFilterResp(cards = result)
        }
    }

    private fun prepareTranslateCardHistResp(
        card: TranslateCard,
        dataHistory: MutableList<TranslateCardHistRecord>,
        validationHistory: MutableList<TranslateCardValidationHistRecord>
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
        for (i in 0 .. validationHistory.size-2) {
            val validationRec = validationHistory.removeAt(i)
            validationHistory.add(
                i,
                validationRec.copy(
                    actualDelay = Utils.millisToDurationStr(validationRec.timestamp - validationHistory[i].timestamp)
                )
            )
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