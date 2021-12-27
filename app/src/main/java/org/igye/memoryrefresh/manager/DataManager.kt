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
                repositoryManager.tagsStat.tagsCouldChange()
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
        }.apply(toBeResponse(DELETE_TAG))
    }

    data class CreateTranslateCardArgs(val textToTranslate:String, val translation:String, val tagIds: Set<Long> = emptySet())
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
            repositoryManager.tagsStat.tagsCouldChange()
            val repo = getRepo()
            return repo.writableDatabase.doInTransactionTry {
                createCard(cardType = CardType.TRANSLATION, tagIds = args.tagIds).map { cardId ->
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
    @BeMethod
    @Synchronized
    fun readTranslateCardById(args: ReadTranslateCardByIdArgs): BeRespose<TranslateCard> {
        return readTranslateCardById(cardId = args.cardId).apply(toBeResponse(GET_TRANSLATE_CARD_BY_ID))
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
        val cardId:Long, val textToTranslate:String? = null, val translation:String? = null,
        val delay: String? = null, val recalculateDelay: Boolean = false
    )
    @BeMethod
    @Synchronized
    fun updateTranslateCard(args: UpdateTranslateCardArgs): BeRespose<Unit> {
        val repo = getRepo()
        return repo.writableDatabase.doInTransactionTry {
            readTranslateCardById(cardId = args.cardId).map { existingCard: TranslateCard ->
                val newTextToTranslate = args.textToTranslate?.trim()?:existingCard.textToTranslate
                val newTranslation = args.translation?.trim()?:existingCard.translation
                val newDelay = args.delay?.trim()?:existingCard.schedule.delay
                if (newTextToTranslate.isEmpty()) {
                    throw MemoryRefreshException(errCode = EDIT_TRANSLATE_CARD_TEXT_TO_TRANSLATE_IS_EMPTY, msg = "Text to translate should not be empty.")
                } else if (newTranslation.isEmpty()) {
                    throw MemoryRefreshException(errCode = EDIT_TRANSLATE_CARD_TRANSLATION_IS_EMPTY, msg = "Translation should not be empty.")
                } else if (newDelay.isEmpty()) {
                    throw MemoryRefreshException(errCode = EDIT_TRANSLATE_CARD_DELAY_IS_EMPTY, msg = "Delay should not be empty.")
                }
                if (newTextToTranslate != existingCard.textToTranslate || newTranslation != existingCard.translation) {
                    repo.translationCards.update(cardId = args.cardId, textToTranslate = newTextToTranslate, translation = newTranslation)
                }
                if (args.recalculateDelay || newDelay != existingCard.schedule.delay) {
                    val randomFactor = 0.85 + Random.nextDouble(from = 0.0, until = 0.30001)
                    val nextAccessInMillis = (Utils.delayStrToMillis(newDelay) * randomFactor).toLong()
                    val timestamp = clock.instant().toEpochMilli()
                    val nextAccessAt = timestamp + nextAccessInMillis
                    repo.cardsSchedule.update(
                        timestamp = timestamp,
                        cardId = args.cardId,
                        delay = newDelay,
                        randomFactor = randomFactor,
                        nextAccessInMillis = nextAccessInMillis,
                        nextAccessAt = nextAccessAt
                    )
                }
                Unit
            }
        }.apply(toBeResponse(EDIT_TRANSLATE_CARD_EXCEPTION))
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
            repositoryManager.tagsStat.tagsCouldChange()
            repo.cardsSchedule.delete(cardId = cardId)
            repo.cards.delete(id = cardId)
        }
    }

    private val selectCurrScheduleForCardQuery =
        "select ${s.updatedAt}, ${s.delay}, ${s.nextAccessInMillis}, ${s.nextAccessAt} from $s where ${s.cardId} = ?"
    private val selectCurrScheduleForCardQueryСolumnNames = arrayOf(s.updatedAt, s.delay, s.nextAccessInMillis, s.nextAccessAt)
    @Synchronized
    private fun selectCurrScheduleForCard(cardId: Long): Try<CardSchedule> {
        return getRepo().readableDatabase.doInTransaction {
            select(
                query = selectCurrScheduleForCardQuery,
                args = arrayOf(cardId.toString()),
                columnNames = selectCurrScheduleForCardQueryСolumnNames,
                rowMapper = {
                    CardSchedule(
                        cardId = cardId,
                        updatedAt = it.getLong(),
                        delay = it.getString(),
                        nextAccessInMillis = it.getLong(),
                        nextAccessAt = it.getLong()
                    )
                }
            ).rows[0]
        }
    }

    private val selectTranslateCardByIdQuery = "select ${t.textToTranslate}, ${t.translation} from $t where ${t.cardId} = ?"
    private val selectTranslateCardByIdQueryColumnNames = arrayOf(t.textToTranslate, t.translation)
    @Synchronized
    private fun readTranslateCardById(cardId: Long): Try<TranslateCard> {
        return getRepo().readableDatabase.doInTransaction {
            val currTime = clock.instant().toEpochMilli()
            select(
                query = selectTranslateCardByIdQuery,
                args = arrayOf(cardId.toString()),
                columnNames = selectTranslateCardByIdQueryColumnNames,
                rowMapper = {
                    val schedule = selectCurrScheduleForCard(cardId = cardId).get()
                    TranslateCard(
                        id = cardId,
                        textToTranslate = it.getString(),
                        translation = it.getString(),
                        schedule = schedule,
                        timeSinceLastCheck = Utils.millisToDurationStr(currTime - schedule.updatedAt),
                    )
                }
            ).rows[0]
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
    private fun createCard(cardType: CardType, tagIds: Set<Long>): Try<Long> {
        repositoryManager.tagsStat.tagsCouldChange()
        val repo = getRepo()
        return repo.writableDatabase.doInTransaction {
            val currTime = clock.instant().toEpochMilli()
            val cardId = repo.cards.insert(cardType = cardType)
            repo.cardsSchedule.insert(cardId = cardId, timestamp = currTime, delay = "0m", randomFactor = 1.0, nextAccessInMillis = 0, nextAccessAt = currTime)
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

}