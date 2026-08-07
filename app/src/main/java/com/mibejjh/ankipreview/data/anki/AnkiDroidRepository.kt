package com.mibejjh.ankipreview.data.anki

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import com.mibejjh.ankipreview.data.model.Card
import com.mibejjh.ankipreview.data.model.CardType
import com.mibejjh.ankipreview.data.model.Deck
import com.mibejjh.ankipreview.data.model.TodayPlan
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AnkiDroid 3rd party ContentProvider API 구현.
 * [AnkiRepository] 계약을 ContentResolver 로 조회한다.
 *
 * 참고: https://github.com/ankidroid/Anki-Android/blob/main/api/src/main/java/com/ichi2/anki/FlashCardsContract.kt
 */
class AnkiDroidRepository(
    private val context: Context,
) : AnkiRepository {

    private val contentResolver: ContentResolver get() = context.contentResolver

    override fun isAnkiDroidInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(ANKIDROID_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    override fun isAvailable(): Boolean {
        if (!isAnkiDroidInstalled()) return false
        return try {
            contentResolver.query(DECK_URI, arrayOf(DECK_ID), null, null, null)
                ?.use { it.count >= 0 } ?: false
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun getDecks(): List<Deck> = withContext(Dispatchers.IO) {
        queryDecks()
    }

    override suspend fun getTodayPlan(deckIds: Set<Long>?): TodayPlan = withContext(Dispatchers.IO) {
        buildTodayPlan(deckIds)
    }

    override suspend fun getCards(deckId: Long): List<Card> = withContext(Dispatchers.IO) {
        val deckNames = queryDecks().associate { it.id to it.name }
        queryDueCards(deckNames).filter { it.deckId == deckId }
    }

    // region decks

    private fun queryDecks(): List<Deck> {
        val decks = mutableListOf<Deck>()
        contentResolver.query(DECK_URI, null, null, null, null)?.use { c ->
            val idCol = c.getColumnIndex(DECK_ID)
            val nameCol = c.getColumnIndex(DECK_NAME)
            val countCol = c.getColumnIndex(DECK_COUNTS)
            val optionsCol = c.getColumnIndex(OPTIONS)
            val dynCol = c.getColumnIndex(DECK_DYN)
            while (c.moveToNext()) {
                val counts = parseDeckCounts(c.getString(countCol))
                val limits = parseDeckLimits(c.getString(optionsCol))
                decks += Deck(
                    id = c.getLong(idCol),
                    name = c.getString(nameCol) ?: "",
                    newCount = counts?.newCount ?: 0,
                    learnCount = counts?.learnCount ?: 0,
                    reviewCount = counts?.reviewCount ?: 0,
                    newPerDayLimit = limits.newPerDay,
                    reviewPerDayLimit = limits.reviewPerDay,
                    isDynamic = c.getInt(dynCol) == 1,
                )
            }
        }
        return decks
    }

    // endregion

    // region today plan

    private fun buildTodayPlan(deckIds: Set<Long>?): TodayPlan {
        val decks = queryDecks()
        val deckNames = decks.associate { it.id to it.name }
        val dueCards = queryDueCards(deckNames)
        val cardsByDeck = dueCards.groupBy { it.deckId }
        return TodayPlanAssembler.assemble(
            decksWithCounts = decks.map { it to DeckCounts(it.learnCount, it.reviewCount, it.newCount) },
            cardsByDeck = cardsByDeck,
            deckIds = deckIds,
            generatedAt = System.currentTimeMillis(),
            dateKey = LocalDate.now().toString(),
        )
    }

    /**
     * 오늘 예정 카드를 검색한다.
     * - `is:due`  : 복습(및 일부 학습) 카드
     * - `is:learn`: 학습/재학습 카드
     * - `is:new`  : 신규 카드 (일일 한도는 TodayPlanAssembler 가 적용)
     */
    private fun queryDueCards(deckNames: Map<Long, String>): List<Card> {
        val merged = LinkedHashMap<Long, Card>()
        QUERY_TERMS.forEach { term ->
            queryCards(term, deckNames).forEach { card -> merged[card.id] = card }
        }
        return merged.values.toList()
    }

    // endregion

    // region cards

    private fun queryCards(search: String, deckNames: Map<Long, String>): List<Card> {
        val result = mutableListOf<Card>()
        contentResolver.query(CARDS_URI, CARD_PROJECTION, search, null, null)?.use { c ->
            val idCol = c.getColumnIndex(CARD_ID)
            val noteCol = c.getColumnIndex(NOTE_ID)
            val ordCol = c.getColumnIndex(CARD_ORD)
            val nameCol = c.getColumnIndex(CARD_NAME)
            val deckCol = c.getColumnIndex(DECK_ID)
            val typeCol = c.getColumnIndex(CARD_TYPE)
            val queueCol = c.getColumnIndex(RAW_QUEUE)
            val dueCol = c.getColumnIndex(RAW_DUE)
            val ivlCol = c.getColumnIndex(INTERVAL)
            val repsCol = c.getColumnIndex(REPS)
            val lapsCol = c.getColumnIndex(LAPSES)
            val qCol = c.getColumnIndex(QUESTION)
            val aCol = c.getColumnIndex(ANSWER)
            val qsCol = c.getColumnIndex(QUESTION_SIMPLE)
            val asCol = c.getColumnIndex(ANSWER_SIMPLE)
            val apCol = c.getColumnIndex(ANSWER_PURE)
            while (c.moveToNext()) {
                val deckId = c.getLong(deckCol)
                result += Card(
                    id = c.getLong(idCol),
                    noteId = c.getLong(noteCol),
                    ord = c.getInt(ordCol),
                    cardName = c.getString(nameCol) ?: "",
                    deckId = deckId,
                    deckName = deckNames[deckId] ?: "",
                    type = CardType.fromCode(c.getInt(typeCol)),
                    queue = c.getInt(queueCol),
                    due = c.getLong(dueCol),
                    interval = c.getLong(ivlCol),
                    reps = c.getInt(repsCol),
                    lapses = c.getInt(lapsCol),
                    question = c.getString(qCol) ?: "",
                    answer = c.getString(aCol) ?: "",
                    questionSimple = c.getString(qsCol) ?: "",
                    answerSimple = c.getString(asCol) ?: "",
                    answerPure = c.getString(apCol) ?: "",
                )
            }
        }
        return result
    }

    // endregion

    private companion object {
        const val ANKIDROID_PACKAGE = "com.ichi2.anki"
        const val AUTHORITY = "com.ichi2.anki.flashcards"
        val CARDS_URI: Uri = Uri.parse("content://$AUTHORITY/cards")
        val DECK_URI: Uri = Uri.parse("content://$AUTHORITY/decks")

        // Card columns
        const val CARD_ID = "_id"
        const val NOTE_ID = "note_id"
        const val CARD_ORD = "ord"
        const val CARD_NAME = "card_name"
        const val DECK_ID = "deck_id"
        const val CARD_TYPE = "type"
        const val RAW_QUEUE = "queue"
        const val RAW_DUE = "due"
        const val INTERVAL = "interval"
        const val REPS = "reps"
        const val LAPSES = "lapses"
        const val QUESTION = "question"
        const val ANSWER = "answer"
        const val QUESTION_SIMPLE = "question_simple"
        const val ANSWER_SIMPLE = "answer_simple"
        const val ANSWER_PURE = "answer_pure"

        // Deck columns
        const val DECK_NAME = "deck_name"
        const val DECK_COUNTS = "deck_count"
        const val OPTIONS = "options"
        const val DECK_DYN = "deck_dyn"

        val CARD_PROJECTION: Array<String> = arrayOf(
            CARD_ID, NOTE_ID, CARD_ORD, CARD_NAME, DECK_ID,
            CARD_TYPE, RAW_QUEUE, RAW_DUE, INTERVAL, REPS, LAPSES,
            QUESTION, ANSWER, QUESTION_SIMPLE, ANSWER_SIMPLE, ANSWER_PURE,
        )

        val QUERY_TERMS: List<String> = listOf("is:due", "is:learn", "is:new")
    }
}