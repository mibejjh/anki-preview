package com.mibejjh.ankipreview.data.anki

import com.mibejjh.ankipreview.data.model.Card
import com.mibejjh.ankipreview.data.model.CardType
import com.mibejjh.ankipreview.data.model.Deck
import com.mibejjh.ankipreview.data.model.DeckPlan
import com.mibejjh.ankipreview.data.model.TodayPlan

/**
 * 순수 조립 로직. 덱별 카드 목록과 오늘 예정 수(DeckCounts)를 받아
 * TodayPlan을 만든다. JVM 단위 테스트 대상.
 */
object TodayPlanAssembler {

    /**
     * @param decksWithCounts 덱과 그 덱의 오늘 예정 수. counts는 스케줄러 산출값.
     * @param cardsByDeck     덱 id → 카드 목록 (신규/학습/복습 포함, 미정렬 허용)
     * @param deckIds         선택된 덱 id 집합. null이면 전체, 빈 집합이면 빈 계획.
     */
    fun assemble(
        decksWithCounts: List<Pair<Deck, DeckCounts>>,
        cardsByDeck: Map<Long, List<Card>>,
        deckIds: Set<Long>? = null,
        generatedAt: Long = System.currentTimeMillis(),
        dateKey: String = "",
    ): TodayPlan {
        val plans = decksWithCounts.mapNotNull { (deck, counts) ->
            if (!matchesFilter(deck.id, deckIds)) return@mapNotNull null
            val selected = selectCards(deck, counts, cardsByDeck[deck.id].orEmpty())
            if (selected.isEmpty()) null else DeckPlan(deck, selected)
        }
        return TodayPlan(generatedAt = generatedAt, dateKey = dateKey, decks = plans)
    }

    private fun matchesFilter(deckId: Long, deckIds: Set<Long>?): Boolean =
        deckIds == null || deckId in deckIds

    /**
     * 한 덱의 카드 목록에서 신규/학습/복습을 각각 오늘 예정 수만큼 잘라 합친다.
     * 신규는 생성 순(id)으로, 학습·복습은 우선순위대로 정렬 후 선취.
     */
    private fun selectCards(deck: Deck, counts: DeckCounts, allCards: List<Card>): List<Card> {
        val newCards = allCards
            .filter { it.type == CardType.NEW }
            .sortedBy { it.id }
            .take(counts.newCount)
        val learningCards = allCards
            .filter { it.type == CardType.LEARNING || it.type == CardType.RELEARNING }
            .take(counts.learnCount)
        val reviewCards = allCards
            .filter { it.type == CardType.REVIEW }
            .take(counts.reviewCount)
        return newCards + learningCards + reviewCards
    }
}