package org.igye.memoryrefresh.manager

import org.igye.memoryrefresh.*
import org.igye.memoryrefresh.ErrorCode.*
import org.igye.memoryrefresh.common.BeMethod
import org.igye.memoryrefresh.common.MemoryRefreshException
import org.igye.memoryrefresh.common.Try
import org.igye.memoryrefresh.common.Utils
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

    data class SaveNewTranslateCardArgs(val textToTranslate:String, val translation:String)
    @BeMethod
    @Synchronized
    fun saveNewTranslateCard(args: SaveNewTranslateCardArgs): BeRespose<TranslateCard> {
        val textToTranslate = args.textToTranslate.trim()
        val translation = args.translation.trim()
        return if (textToTranslate.isBlank()) {
            BeRespose(err = BeErr(code = SAVE_NEW_TRANSLATE_CARD_TEXT_TO_TRANSLATE_IS_EMPTY.code, msg = "Text to translate should not be empty."))
        } else if (translation.isBlank()) {
            BeRespose(err = BeErr(code = SAVE_NEW_TRANSLATE_CARD_TRANSLATION_IS_EMPTY.code, msg = "Translation should not be empty."))
        } else {
            val repo = getRepo()
            repo.writableDatabase.doInTransactionTry {
                val cardId = repo.cards.insert(cardType = CardType.TRANSLATION)
                val currTime = clock.instant().toEpochMilli()
                repo.cardsSchedule.insert(cardId = cardId, timestamp = currTime, delay = "0m", randomFactor = 1.0, nextAccessInMillis = 0, nextAccessAt = currTime)
                repo.translationCards.insert(cardId = cardId, textToTranslate = textToTranslate, translation = translation)
                selectTranslateCardById(cardId = cardId)
            }.apply(toBeResponse(SAVE_NEW_TRANSLATE_CARD_EXCEPTION))
        }
    }

    data class UpdateTranslateCardArgs(
        val cardId:Long, val textToTranslate:String? = null, val translation:String? = null,
        val delay: String? = null, val recalculateDelay: Boolean? = null
    )
    @BeMethod
    @Synchronized
    fun updateTranslateCard(args: UpdateTranslateCardArgs): BeRespose<TranslateCard> {
        val repo = getRepo()
        return repo.writableDatabase.doInTransactionTry {
            selectTranslateCardById(cardId = args.cardId).map { existingCard: TranslateCard ->
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
                var dataWasUpdated = false
                if (newTextToTranslate != existingCard.textToTranslate || newTranslation != existingCard.translation) {
                    repo.translationCards.update(cardId = args.cardId, textToTranslate = newTextToTranslate, translation = newTranslation)
                    dataWasUpdated = true
                }
                if (args.recalculateDelay == true || newDelay != existingCard.schedule.delay) {
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
                    dataWasUpdated = true
                }
                if (dataWasUpdated) {
                    selectTranslateCardById(cardId = existingCard.id).get()
                } else {
                    existingCard
                }
            }
        }.apply(toBeResponse(EDIT_TRANSLATE_CARD_EXCEPTION))
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

    data class GetTranslateCardByIdArgs(val cardId: Long)
    @BeMethod
    @Synchronized
    fun getTranslateCardById(args: GetTranslateCardByIdArgs): BeRespose<TranslateCard> {
        return selectTranslateCardById(cardId = args.cardId).apply(toBeResponse(GET_TRANSLATE_CARD_BY_ID))
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
            repo.cardsSchedule.delete(cardId = args.cardId)
            repo.cards.delete(id = args.cardId)
            true
        }.apply(toBeResponse(DELETE_TRANSLATE_CARD_EXCEPTION))
    }


    data class GetTranslateCardHistoryArgs(val cardId:Long)
    private val getTranslateCardHistoryQuery =
        "select ${l.recId}, ${l.cardId}, ${l.timestamp}, ${l.translation}, ${l.matched} from $l where ${l.cardId} = ? order by ${l.timestamp} desc"
    private val getTranslateCardHistoryQueryColumnNames = arrayOf(l.recId, l.cardId, l.timestamp, l.translation, l.matched)
    @BeMethod
    @Synchronized
    fun getTranslateCardHistory(args: GetTranslateCardHistoryArgs): BeRespose<TranslateCardHistResp> {
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

    private val selectCurrScheduleForCardQuery =
        "select ${s.delay}, ${s.nextAccessInMillis}, ${s.nextAccessAt} from $s where ${s.cardId} = ?"
    private val selectCurrScheduleForCardQueryСolumnNames = arrayOf(s.delay, s.nextAccessInMillis, s.nextAccessAt)
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
    private fun selectTranslateCardById(cardId: Long): Try<TranslateCard> {
        return getRepo().readableDatabase.doInTransaction {
            select(
                query = selectTranslateCardByIdQuery,
                args = arrayOf(cardId.toString()),
                columnNames = selectTranslateCardByIdQueryColumnNames,
                rowMapper = {
                    TranslateCard(
                        id = cardId,
                        textToTranslate = it.getString(),
                        translation = it.getString(),
                        schedule = selectCurrScheduleForCard(cardId = cardId).get()
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

    private fun <T> toBeResponse(errCode: ErrorCode): (Try<T>) -> BeRespose<T> = {
        it
            .map { BeRespose(data = it) }
            .getIfSuccessOrElse {
                BeRespose(
                    err = BeErr(
                        code = (if (it is MemoryRefreshException) it.errCode.code else null)?:errCode.code,
                        msg = it.javaClass.canonicalName + ": " + it.message
                    )
                )
            }
    }

}