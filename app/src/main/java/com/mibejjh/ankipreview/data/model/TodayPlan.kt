package com.mibejjh.ankipreview.data.model

/**
 * 오늘 학습할 카드 계획. 덱별로 그룹화된 카드 목록을 담는다.
 */
data class TodayPlan(
    val generatedAt: Long,
    val dateKey: String,
    val decks: List<DeckPlan>,
) {
    val totalCards: Int get() = decks.sumOf { it.cards.size }
}

/**
 * 한 덱의 오늘 카드 계획.
 */
data class DeckPlan(
    val deck: Deck,
    val cards: List<Card>,
) {
    val newCount: Int get() = cards.count { it.type == CardType.NEW }
    val learnCount: Int get() = cards.count { it.type == CardType.LEARNING }
    val reviewCount: Int get() = cards.count { it.type == CardType.REVIEW }
}