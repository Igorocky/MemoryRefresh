package org.igye.memoryrefresh

import org.igye.memoryrefresh.ErrorCode.*
import org.igye.memoryrefresh.common.Try
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

    data class SaveNewTranslateCardArgs(val textToTranslate:String, val translation:String)
    @BeMethod
    @Synchronized
    fun saveNewTranslateCard(args:SaveNewTranslateCardArgs): BeRespose<TranslateCard> {
        val textToTranslate = args.textToTranslate.trim()
        val translation = args.translation.trim()
        return if (textToTranslate.isBlank()) {
            BeRespose(err = BeErr(code = SAVE_NEW_TRANSLATE_CARD_TEXT_TO_TRANSLATE_IS_EMPTY.code, msg = "Text to translate should not be empty."))
        } else if (translation.isBlank()) {
            BeRespose(err = BeErr(code = SAVE_NEW_TRANSLATE_CARD_TRANSLATION_IS_EMPTY.code, msg = "Translation should not be empty."))
        } else {
            val repo = getRepo()
            repo.writableDatabase.doInTransactionTry {
                val cardId = repo.cards.insertStmt(cardType = CardType.TRANSLATION)
                val currTime = clock.instant().toEpochMilli()
                repo.cardsSchedule.insertStmt(cardId = cardId, timestamp = currTime, delay = "0m", randomFactor = 1.0, nextAccessInMillis = 0, nextAccessAt = currTime)
                repo.translationCards.insertStmt(cardId = cardId, textToTranslate = textToTranslate, translation = translation)
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
    fun updateTranslateCard(args:UpdateTranslateCardArgs): BeRespose<TranslateCard> {
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
                    repo.translationCards.updateStmt(cardId = args.cardId, textToTranslate = newTextToTranslate, translation = newTranslation)
                    dataWasUpdated = true
                }
                if (args.recalculateDelay == true || newDelay != existingCard.schedule.delay) {
                    val randomFactor = 0.85 + Random.nextDouble(from = 0.0, until = 0.30001)
                    val nextAccessInMillis = (Utils.delayStrToMillis(newDelay) * randomFactor).toLong()
                    val timestamp = clock.instant().toEpochMilli()
                    val nextAccessAt = timestamp + nextAccessInMillis
                    repo.cardsSchedule.updateStmt(
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
                GetNextCardToRepeatResp(cardsRemain = 0, nextCardIn = selectMinNextAccessAt().map { Utils.millisToDurationStr(it) }.orElse(""))
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
    @BeMethod
    @Synchronized
    fun validateTranslateCard(args:ValidateTranslateCardArgs): BeRespose<ValidateTranslateCardAnswerResp> {
        val userProvidedTranslation = args.userProvidedTranslation.trim()
        return if (userProvidedTranslation.isBlank()) {
            BeRespose(err = BeErr(code = VALIDATE_TRANSLATE_CARD_TRANSLATION_IS_EMPTY.code, msg = "Translation should not be empty."))
        } else {
            val repo = getRepo()
            repo.writableDatabase.doInTransaction {
                val expectedTranslation = select(
                    query = validateTranslateCardQuery,
                    args = arrayOf(args.cardId.toString()),
                    columnNames = arrayOf("expectedTranslation"),
                    rowMapper = {it.getString()}
                ).rows[0].trim()
                val translationIsCorrect = userProvidedTranslation == expectedTranslation
                repo.translationCardsLog.insertStmt(
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

    private val selectCurrScheduleForCardQuery =
        "select ${s.delay}, ${s.nextAccessInMillis}, ${s.nextAccessAt} from $s where ${s.cardId} = ?"
    @Synchronized
    private fun selectCurrScheduleForCard(cardId: Long): Try<CardSchedule> {
        return getRepo().readableDatabase.doInTransaction {
            select(
                query = selectCurrScheduleForCardQuery,
                args = arrayOf(cardId.toString()),
                columnNames = arrayOf(s.delay, s.nextAccessInMillis, s.nextAccessAt),
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
    @Synchronized
    private fun selectTranslateCardById(cardId: Long): Try<TranslateCard> {
        return getRepo().readableDatabase.doInTransaction {
            select(
                query = selectTranslateCardByIdQuery,
                args = arrayOf(cardId.toString()),
                columnNames = arrayOf(t.textToTranslate, t.translation),
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
    @Synchronized
    private fun selectMinNextAccessAt(): Optional<Long> {
        return getRepo().readableDatabase.doInTransaction {
            select(
                query = selectMinNextAccessAtQuery,
                columnNames = arrayOf("cnt", "nextAccessAt"),
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
    @Synchronized
    fun selectTopOverdueCards(maxCardsNum: Int): Try<SelectedRows<CardOverdue>> {
        return getRepo().readableDatabase.doInTransaction {
            val currTimeStr = clock.instant().toEpochMilli().toString()
            select(
                query = selectTopOverdueCardsQuery,
                args = arrayOf(currTimeStr, currTimeStr),
                columnNames = arrayOf("cardId", "cardType", "overdue"),
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