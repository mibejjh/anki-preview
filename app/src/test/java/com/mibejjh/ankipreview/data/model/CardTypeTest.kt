package com.mibejjh.ankipreview.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardTypeTest {

    @Test
    fun `fromCode maps known codes`() {
        assertEquals(CardType.NEW, CardType.fromCode(0))
        assertEquals(CardType.LEARNING, CardType.fromCode(1))
        assertEquals(CardType.REVIEW, CardType.fromCode(2))
        assertEquals(CardType.RELEARNING, CardType.fromCode(3))
    }

    @Test
    fun `fromCode returns null for unknown code`() {
        assertNull(CardType.fromCode(99))
        assertNull(CardType.fromCode(-1))
    }
}