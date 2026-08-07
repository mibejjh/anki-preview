package com.mibejjh.ankipreview.data.anki

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeckCountsParserTest {

    @Test
    fun `parseDeckCounts parses AnkiDroid learn-review-new array`() {
        val counts = parseDeckCounts("[12, 34, 5]")
        assertEquals(DeckCounts(learnCount = 12, reviewCount = 34, newCount = 5), counts)
    }

    @Test
    fun `parseDeckCounts handles zero counts`() {
        val counts = parseDeckCounts("[0,0,0]")
        assertEquals(DeckCounts(0, 0, 0), counts)
    }

    @Test
    fun `parseDeckCounts returns null for blank input`() {
        assertNull(parseDeckCounts(null))
        assertNull(parseDeckCounts(""))
        assertNull(parseDeckCounts("   "))
    }

    @Test
    fun `parseDeckCounts returns null for malformed json`() {
        assertNull(parseDeckCounts("not json"))
        assertNull(parseDeckCounts("{}"))
        assertNull(parseDeckCounts("[1,2")) // terminat되지 않은 배열
    }

    @Test
    fun `parseDeckCounts defaults missing indexes to zero`() {
        val counts = parseDeckCounts("[7]")
        assertEquals(DeckCounts(learnCount = 7, reviewCount = 0, newCount = 0), counts)
    }

    @Test
    fun `parseDeckLimits extracts new and rev perDay`() {
        val json = """{"name":"Default","new":{"perDay":20},"rev":{"perDay":200},"lapse":{}}"""
        assertEquals(DeckLimits(newPerDay = 20, reviewPerDay = 200), parseDeckLimits(json))
    }

    @Test
    fun `parseDeckLimits handles missing sections`() {
        val json = """{"name":"Default"}"""
        assertEquals(DeckLimits(0, 0), parseDeckLimits(json))
    }

    @Test
    fun `parseDeckLimits returns zeros for blank or invalid`() {
        assertEquals(DeckLimits(0, 0), parseDeckLimits(null))
        assertEquals(DeckLimits(0, 0), parseDeckLimits(""))
        assertEquals(DeckLimits(0, 0), parseDeckLimits("garbage"))
    }
}