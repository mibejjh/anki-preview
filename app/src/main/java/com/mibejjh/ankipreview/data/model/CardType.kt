package com.mibejjh.ankipreview.data.model

/**
 * Anki 카드 유형 (backend type 코드).
 * [docs](https://docs.ankiweb.net/getting-started.html#card-states)
 *
 * @property code anki cards 테이블의 type 열 값
 */
enum class CardType(val code: Int) {
    NEW(0),
    LEARNING(1),
    REVIEW(2),
    RELEARNING(3);

    companion object {
        fun fromCode(code: Int): CardType? = entries.firstOrNull { it.code == code }
    }
}