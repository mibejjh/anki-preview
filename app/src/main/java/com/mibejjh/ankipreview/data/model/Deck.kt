package com.mibejjh.ankipreview.data.model

/**
 * 덱과 오늘 예정 카드 수(counts).
 * `DECK_COUNTS` 및 `OPTIONS`(daily limits)를 반영한 값.
 */
data class Deck(
    val id: Long,
    val name: String,
    /** 오늘 예정된 신규 카드 수 (일일 한도 반영) */
    val newCount: Int,
    /** 오늘 예정된 학습 카드 수 */
    val learnCount: Int,
    /** 오늘 예정된 복습 카드 수 */
    val reviewCount: Int,
    /** 신규 일일 한도 */
    val newPerDayLimit: Int,
    /** 복습 일일 한도 */
    val reviewPerDayLimit: Int,
    /** 하위 덱 포함 여부 */
    val isDynamic: Boolean = false,
) {
    val totalDue: Int get() = newCount + learnCount + reviewCount
}