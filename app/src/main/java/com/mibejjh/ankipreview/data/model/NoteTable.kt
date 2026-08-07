package com.mibejjh.ankipreview.data.model

/**
 * 한 덱의 노트를 테이블(탐색기) 형태로 표현한 모델.
 * 행 = 노트, 열 = 노트 타입의 필드.
 */
data class NoteTable(
    val deckId: Long,
    val deckName: String,
    /** 노트 타입의 필드명 목록 (열 헤더). */
    val fieldNames: List<String>,
    /** 노트 행 목록. */
    val rows: List<NoteRow>,
)

/**
 * 테이블의 한 행(노트 1개). [fieldValues]는 [NoteTable.fieldNames]와 인덱스가 정렬된다.
 */
data class NoteRow(
    val noteId: Long,
    val fieldValues: List<String>,
)
