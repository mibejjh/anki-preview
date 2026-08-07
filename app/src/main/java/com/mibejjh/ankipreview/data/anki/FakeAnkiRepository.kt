package com.mibejjh.ankipreview.data.anki

import com.mibejjh.ankipreview.data.model.Card
import com.mibejjh.ankipreview.data.model.CardType
import com.mibejjh.ankipreview.data.model.Deck
import com.mibejjh.ankipreview.data.model.DeckPlan
import com.mibejjh.ankipreview.data.model.TodayPlan
import java.time.LocalDate

/**
 * AnkiDroid 없이 UI를 빌드/시연하기 위한 가짜 레포지토리.
 * 샘플 영어 단어 카드를 덱별로 반환한다.
 */
class FakeAnkiRepository : AnkiRepository {

    private val decks: List<Deck> = listOf(
        Deck(
            id = 1001,
            name = "영어::중급",
            newCount = 3,
            learnCount = 2,
            reviewCount = 5,
            newPerDayLimit = 20,
            reviewPerDayLimit = 200,
        ),
        Deck(
            id = 1002,
            name = "영어::고급",
            newCount = 2,
            learnCount = 1,
            reviewCount = 4,
            newPerDayLimit = 20,
            reviewPerDayLimit = 200,
        ),
        // 맞춤학습(동적/filtered) 덱
        Deck(
            id = 1003,
            name = "맞춤학습::오답집중",
            newCount = 1,
            learnCount = 1,
            reviewCount = 3,
            newPerDayLimit = 20,
            reviewPerDayLimit = 200,
            isDynamic = true,
        ),
    )

    private val cards: List<Card> = buildList {
        // 영어::중급
        add(card(101, 1001, "영어::중급", "abundant", "풍부한", CardType.NEW))
        add(card(102, 1001, "영어::중급", "benevolent", "자애로운", CardType.NEW))
        add(card(103, 1001, "영어::중급", "candid", "솔직한", CardType.NEW))
        add(card(104, 1001, "영어::중급", "diligent", "부지런한", CardType.LEARNING))
        add(card(105, 1001, "영어::중급", "eloquent", "유창한", CardType.LEARNING))
        add(card(106, 1001, "영어::중급", "fruitful", "수확이 많은", CardType.REVIEW))
        add(card(107, 1001, "영어::중급", "genuine", "진짜의", CardType.REVIEW))
        add(card(108, 1001, "영어::중급", "hinder", "방해하다", CardType.REVIEW))
        add(card(109, 1001, "영어::중급", "immense", "거대한", CardType.REVIEW))
        add(card(110, 1001, "영어::중급", "jovial", "쾌활한", CardType.REVIEW))
        // 영어::고급
        add(card(201, 1002, "영어::고급", "meticulous", "꼼꼼한", CardType.NEW))
        add(card(202, 1002, "영어::고급", "nonchalant", "무심한", CardType.NEW))
        add(card(203, 1002, "영어::고급", "obsolete", "쓸모없는", CardType.LEARNING))
        add(card(204, 1002, "영어::고급", "poignant", "뼈저린", CardType.REVIEW))
        add(card(205, 1002, "영어::고급", "quintessential", "전형적인", CardType.REVIEW))
        add(card(206, 1002, "영어::고급", "resilient", "회복력 있는", CardType.REVIEW))
        add(card(207, 1002, "영어::고급", "subtle", "미묘한", CardType.REVIEW))
        // 맞춤학습(동적) 덱
        add(card(301, 1003, "맞춤학습::오답집중", "ambiguous", "모호한", CardType.NEW))
        add(card(302, 1003, "맞춤학습::오답집중", "coherent", "일관된", CardType.LEARNING))
        add(card(303, 1003, "맞춤학습::오답집중", "deteriorate", "악화되다", CardType.REVIEW))
        add(card(304, 1003, "맞춤학습::오답집중", "elaborate", "정교한", CardType.REVIEW))
        add(card(305, 1003, "맞춤학습::오답집중", "feasible", "실현 가능한", CardType.REVIEW))
    }

    override fun isAvailable(): Boolean = true

    override fun isAnkiDroidInstalled(): Boolean = true

    override suspend fun getDecks(): List<Deck> = decks

    override suspend fun getTodayPlan(deckIds: Set<Long>?): TodayPlan {
        val plan = TodayPlanAssembler.assemble(
            decksWithCounts = decks.map { it to DeckCounts(it.learnCount, it.reviewCount, it.newCount) },
            cardsByDeck = cards.groupBy { it.deckId },
            deckIds = deckIds,
            generatedAt = System.currentTimeMillis(),
            dateKey = LocalDate.now().toString(),
        )
        return plan
    }

    override suspend fun getCards(deckId: Long): List<Card> =
        cards.filter { it.deckId == deckId }

    private fun card(
        id: Long,
        deckId: Long,
        deckName: String,
        word: String,
        meaning: String,
        type: CardType,
    ) = Card(
        id = id,
        noteId = id,
        ord = 0,
        cardName = "Card 1",
        deckId = deckId,
        deckName = deckName,
        type = type,
        queue = when (type) {
            CardType.NEW -> 0
            CardType.LEARNING, CardType.RELEARNING -> 1
            CardType.REVIEW -> 2
        },
        due = 0L,
        interval = 0L,
        reps = 0,
        lapses = 0,
        question = word,
        answer = meaning,
        questionSimple = word,
        answerSimple = meaning,
        answerPure = meaning,
    )
}