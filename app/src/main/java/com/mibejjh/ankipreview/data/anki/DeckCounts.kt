package com.mibejjh.ankipreview.data.anki

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * 오늘 예정 카드 수. AnkiDroid `deck_count` JSON 형식은 `[학습, 복습, 신규]` 순서의
 * 3개 정수 배열이며, 스케줄러가 이미 일일 한도를 반영해 계산한 값이다.
 */
data class DeckCounts(
    val learnCount: Int,
    val reviewCount: Int,
    val newCount: Int,
)

/**
 * 덱 일일 한도. AnkiDroid `options`(deck config) JSON에서 `new.perDay`, `rev.perDay` 추출.
 */
data class DeckLimits(
    val newPerDay: Int,
    val reviewPerDay: Int,
)

/**
 * AnkiDroid `deck_count` JSON 문자열(`"[학습,복습,신규]"`)을 파싱한다.
 * 파싱 불가/빈 값이면 null.
 */
fun parseDeckCounts(raw: String?): DeckCounts? {
    if (raw.isNullOrBlank()) return null
    return try {
        val arr = JSONArray(raw)
        DeckCounts(
            learnCount = arr.optInt(0, 0),
            reviewCount = arr.optInt(1, 0),
            newCount = arr.optInt(2, 0),
        )
    } catch (e: JSONException) {
        null
    }
}

/** 덱 config JSON(`options`)에서 신규/복습 일일 한도를 파싱한다. 실패 시 0. */
fun parseDeckLimits(optionsJson: String?): DeckLimits {
    if (optionsJson.isNullOrBlank()) return DeckLimits(0, 0)
    return try {
        val obj = JSONObject(optionsJson)
        DeckLimits(
            newPerDay = obj.optJSONObject("new")?.optInt("perDay", 0) ?: 0,
            reviewPerDay = obj.optJSONObject("rev")?.optInt("perDay", 0) ?: 0,
        )
    } catch (e: JSONException) {
        DeckLimits(0, 0)
    }
}