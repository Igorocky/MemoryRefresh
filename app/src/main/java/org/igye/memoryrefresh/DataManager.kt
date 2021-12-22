package org.igye.memoryrefresh

import org.igye.memoryrefresh.ErrorCode.*
import org.igye.memoryrefresh.common.Try
import org.igye.memoryrefresh.database.*
import org.igye.memoryrefresh.dto.common.BeErr
import org.igye.memoryrefresh.dto.common.BeRespose
import org.igye.memoryrefresh.dto.domain.CardOverdue
import org.igye.memoryrefresh.dto.domain.CardSchedule
import org.igye.memoryrefresh.dto.domain.GetNextCardToRepeatResp
import org.igye.memoryrefresh.dto.domain.TranslateCard
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
                repo.cardsSchedule.insertStmt(cardId = cardId, lastAccessedAt = currTime, nextAccessInMillis = 0, nextAccessAt = currTime)
                repo.translationCards.insertStmt(cardId = cardId, textToTranslate = textToTranslate, translation = translation)
                selectTranslateCardById(cardId = cardId)
            }.apply(toBeResponse(SAVE_NEW_TRANSLATE_CARD_EXCEPTION))
        }
    }

    data class EditTranslateCardArgs(val cardId:Long, val textToTranslate:String, val translation:String)
    @BeMethod
    @Synchronized
    fun editTranslateCard(args:EditTranslateCardArgs): BeRespose<TranslateCard> {
        val textToTranslate = args.textToTranslate.trim()
        val translation = args.translation.trim()
        return if (textToTranslate.isBlank()) {
            BeRespose(err = BeErr(code = EDIT_TRANSLATE_CARD_TEXT_TO_TRANSLATE_IS_EMPTY.code, msg = "Text to translate should not be empty."))
        } else if (translation.isBlank()) {
            BeRespose(err = BeErr(code = EDIT_TRANSLATE_CARD_TRANSLATION_IS_EMPTY.code, msg = "Translation should not be empty."))
        } else {
            val repo = getRepo()
            repo.writableDatabase.doInTransactionTry {
                selectTranslateCardById(cardId = args.cardId).map { existingCard: TranslateCard ->
                    if (existingCard.textToTranslate == textToTranslate && existingCard.translation == translation) {
                        existingCard
                    } else {
                        repo.translationCards.updateStmt(cardId = args.cardId, textToTranslate = textToTranslate, translation = translation)
                        existingCard.copy(textToTranslate = textToTranslate, translation = translation)
                    }
                }
            }.apply(toBeResponse(EDIT_TRANSLATE_CARD_EXCEPTION))
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
                GetNextCardToRepeatResp(cardsRemain = 0, nextCardIn = selectMinNextAccessAt().map { Utils.millisToDurationStr(it) }.orElse(""))
            }
        }.apply(toBeResponse(GET_NEXT_CARD_TO_REPEAT))
    }

    private val selectCurrScheduleForCardQuery =
        "select ${s.lastAccessedAt}, ${s.nextAccessInMillis}, ${s.nextAccessAt} from $s where ${s.cardId} = ?"
    @Synchronized
    private fun selectCurrScheduleForCard(cardId: Long): Try<CardSchedule> {
        return getRepo().readableDatabase.doInTransaction {
            select(
                query = selectCurrScheduleForCardQuery,
                args = arrayOf(cardId.toString()),
                columnNames = arrayOf(s.lastAccessedAt, s.nextAccessInMillis, s.nextAccessAt),
                rowMapper = {
                    CardSchedule(
                        cardId = cardId,
                        lastAccessedAt = it.getLong(),
                        nextAccessInSec = it.getLong(),
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
                case when ? /*1 currTime*/ < ${s.nextAccessAt} then -1.0 else (? /*2 currTime*/ - ${s.nextAccessAt} ) * 1.0 / ${s.nextAccessInMillis} end overdue
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
                BeRespose(err = BeErr(code = errCode.code, msg = it.message ?: it.javaClass.canonicalName))
            }
    }

}