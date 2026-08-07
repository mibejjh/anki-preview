package com.mibejjh.ankipreview.data.anki

import com.mibejjh.ankipreview.data.model.Card
import com.mibejjh.ankipreview.data.model.Deck
import com.mibejjh.ankipreview.data.model.TodayPlan

/**
 * AnkiDroid 3rd party API(ContentProvider) 접근 계약.
 * 구현체는 data-layer worktree 에서 작성한다.
 */
interface AnkiRepository {

    /** AnkiDroid 설치 및 API 활성화 여부를 반환한다. */
    fun isAvailable(): Boolean

    /** AnkiDroid 패키지가 설치되어 있는지 여부. */
    fun isAnkiDroidInstalled(): Boolean

    /** 모든 덱과 오늘 예정 카드 수를 반환한다. */
    suspend fun getDecks(): List<Deck>

    /**
     * 오늘 학습할 카드 계획을 반환한다.
     * @param deckFilter 특정 덱만 조회할 경우 덱 이름(부분 일치) 또는 null(전체)
     */
    suspend fun getTodayPlan(deckFilter: String? = null): TodayPlan

    /** 특정 덱의 모든 예정 카드를 반환한다 (델범위). */
    suspend fun getCards(deckId: Long): List<Card>
}