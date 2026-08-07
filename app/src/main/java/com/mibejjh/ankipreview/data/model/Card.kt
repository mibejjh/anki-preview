package com.mibejjh.ankipreview.data.model

/**
 * AnkiDroid ContentProvider 로 조회한 카드 1장.
 * [AnkiRepository] 의 계약 산출물로, UI 레이어가 직접 소비한다.
 */
data class Card(
    val id: Long,
    val noteId: Long,
    val ord: Int,
    val cardName: String,
    val deckId: Long,
    val deckName: String,
    val type: CardType?,
    val queue: Int,
    val due: Long,
    val interval: Long,
    val reps: Int,
    val lapses: Int,
    /** 질문(영어 단어/앞면) - 스타일 포함 가능 */
    val question: String,
    /** 답변(뜻/뒷면) */
    val answer: String,
    /** 스타일(CSS) 제거된 질문 */
    val questionSimple: String,
    /** 스타일(CSS) 제거된 답변 */
    val answerSimple: String,
    /** 질문 중복 제거된 순수 답변 */
    val answerPure: String,
) {
    /** 카드 유형 배지 표시용 라벨 */
    val typeLabel: String
        get() = when (type) {
            CardType.NEW -> "신규"
            CardType.LEARNING -> "학습"
            CardType.REVIEW -> "복습"
            CardType.RELEARNING -> "재학습"
            null -> "알 수 없음"
        }
}