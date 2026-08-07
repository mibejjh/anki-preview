package com.mibejjh.ankipreview.data.anki

import com.mibejjh.ankipreview.data.model.Card
import com.mibejjh.ankipreview.data.model.CardType
import com.mibejjh.ankipreview.data.model.Deck
import org.junit.Assert.assertEquals
import org.junit.Test

class TodayPlanAssemblerTest {

    private val deck = Deck(
        id = 1,
        name = "영어::중급",
        newCount = 2,
        learnCount = 1,
        reviewCount = 3,
        newPerDayLimit = 20,
        reviewPerDayLimit = 200,
    )

    private fun card(id: Long, type: CardType, deckId: Long = 1) = Card(
        id = id,
        noteId = id,
        ord = 0,
        cardName = "Card 1",
        deckId = deckId,
        deckName = "영어::중급",
        type = type,
        queue = 0,
        due = 0L,
        interval = 0L,
        reps = 0,
        lapses = 0,
        question = "word",
        answer = "뜻",
        questionSimple = "word",
        answerSimple = "뜻",
        answerPure = "뜻",
    )

    @Test
    fun `assembles plan grouping cards by deck`() {
        val newCards = listOf(card(1, CardType.NEW), card(2, CardType.NEW), card(3, CardType.NEW))
        val learn = listOf(card(4, CardType.LEARNING))
        val review = listOf(card(5, CardType.REVIEW), card(6, CardType.REVIEW))
        val plan = TodayPlanAssembler.assemble(
            decksWithCounts = listOf(deck to DeckCounts(1, 3, 2)),
            cardsByDeck = mapOf(1L to newCards + learn + review),
            dateKey = "2026-08-07",
        )
        assertEquals(1, plan.decks.size)
        val dp = plan.decks.first()
        // new trimmed to 2 (ids 1,2), learn 1, review 2
        assertEquals(5, dp.cards.size)
        assertEquals(2, dp.newCount)
        assertEquals(1, dp.learnCount)
        assertEquals(2, dp.reviewCount)
        // 신규가 앞에, 생성순 정렬
        assertEquals(listOf(1L, 2L, 4L, 5L, 6L), dp.cards.map { it.id })
    }

    @Test
    fun `trims new cards to daily limit preserving creation order`() {
        val manyNew = (1..10).map { card(it.toLong(), CardType.NEW) }
        val plan = TodayPlanAssembler.assemble(
            decksWithCounts = listOf(deck.copy(newCount = 3) to DeckCounts(0, 0, 3)),
            cardsByDeck = mapOf(1L to manyNew),
        )
        assertEquals(listOf(1L, 2L, 3L), plan.decks.first().cards.map { it.id })
    }

    @Test
    fun `omits decks with no selected cards`() {
        val plan = TodayPlanAssembler.assemble(
            decksWithCounts = listOf(deck to DeckCounts(0, 0, 0)),
            cardsByDeck = emptyMap(),
        )
        assertEquals(0, plan.decks.size)
    }

    @Test
    fun `filters decks by deckIds`() {
        val deckA = deck
        val deckB = deck.copy(id = 2, name = "스페인어::기초")
        val cardsForA = listOf(card(1, CardType.NEW), card(2, CardType.LEARNING), card(3, CardType.REVIEW))
        val plan = TodayPlanAssembler.assemble(
            decksWithCounts = listOf(
                deckA to DeckCounts(1, 1, 1),
                deckB to DeckCounts(1, 1, 1),
            ),
            cardsByDeck = mapOf(1L to cardsForA, 2L to cardsForA),
            deckIds = setOf(1L),
        )
        assertEquals(1, plan.decks.size)
        assertEquals("영어::중급", plan.decks.first().deck.name)
    }

    @Test
    fun `empty deckIds yields empty plan`() {
        val plan = TodayPlanAssembler.assemble(
            decksWithCounts = listOf(deck to DeckCounts(1, 1, 1)),
            cardsByDeck = mapOf(1L to listOf(card(1, CardType.NEW))),
            deckIds = emptySet(),
        )
        assertEquals(0, plan.decks.size)
    }

    @Test
    fun `counts total cards across decks`() {
        val cardsForA = listOf(card(1, CardType.NEW), card(2, CardType.REVIEW))
        val cardsForB = listOf(card(11, CardType.NEW, deckId = 2), card(12, CardType.REVIEW, deckId = 2))
        val plan = TodayPlanAssembler.assemble(
            decksWithCounts = listOf(
                deck to DeckCounts(0, 1, 1),
                deck.copy(id = 2, name = "B") to DeckCounts(0, 1, 1),
            ),
            cardsByDeck = mapOf(1L to cardsForA, 2L to cardsForB),
        )
        assertEquals(4, plan.totalCards)
    }
}