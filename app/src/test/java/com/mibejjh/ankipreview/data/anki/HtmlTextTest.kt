package com.mibejjh.ankipreview.data.anki

import com.mibejjh.ankipreview.data.model.NoteRow
import com.mibejjh.ankipreview.data.model.NoteTable
import org.junit.Assert.assertEquals
import org.junit.Test

class HtmlTextTest {

    @Test
    fun `strips html tags`() {
        assertEquals("hello world", stripHtml("<b>hello</b> <i>world</i>"))
    }

    @Test
    fun `strips hr and answer tags`() {
        assertEquals("word meaning", stripHtml("word<hr id=answer>meaning"))
    }

    @Test
    fun `decodes html entities`() {
        assertEquals("a & b < c > d", stripHtml("a &amp; b &lt; c &gt; d"))
        assertEquals("nbsp space", stripHtml("nbsp&nbsp;space"))
        assertEquals("I'm fine", stripHtml("I&#x27;m fine"))
    }

    @Test
    fun `collapses whitespace`() {
        assertEquals("a b c", stripHtml("a   b\n\t c"))
    }

    @Test
    fun `empty input stays empty`() {
        assertEquals("", stripHtml(""))
        assertEquals("", stripHtml("   "))
    }
}

class NoteTableTest {

    @Test
    fun `note table holds field names and rows`() {
        val table = NoteTable(
            deckId = 1L,
            deckName = "영어",
            fieldNames = listOf("Word", "Meaning"),
            rows = listOf(NoteRow(101, listOf("abundant", "풍부한"))),
        )
        assertEquals(2, table.fieldNames.size)
        assertEquals(1, table.rows.size)
        assertEquals("abundant", table.rows.first().fieldValues[0])
    }
}
