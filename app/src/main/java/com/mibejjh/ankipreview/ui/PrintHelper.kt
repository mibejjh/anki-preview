package com.mibejjh.ankipreview.ui

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.mibejjh.ankipreview.data.model.NoteTable

/**
 * 노트 테이블을 HTML로 만들어 WebView + PrintManager 로 PDF 인쇄한다.
 */
object PrintHelper {

    /**
     * 선택된 덱의 테이블을 인쇄한다.
     * @param tables      표시할 노트 테이블 목록
     * @param hiddenFields 덱별 숨긴 필드 인덱스 (인쇄에서도 제외)
     */
    fun print(context: Context, tables: List<NoteTable>, hiddenFields: Map<Long, Set<Int>>) {
        if (tables.isEmpty()) return
        val html = buildHtml(tables, hiddenFields)
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val adapter = view?.createPrintDocumentAdapter("Anki Preview") ?: return
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                printManager.print(
                    "Anki Preview",
                    adapter,
                    PrintAttributes.Builder().build(),
                )
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private fun buildHtml(tables: List<NoteTable>, hiddenFields: Map<Long, Set<Int>>): String {
        val sb = StringBuilder()
        sb.append("<html><head><meta charset=\"UTF-8\"><style>")
        sb.append("body { font-family: 'Noto Sans KR', sans-serif; margin: 16px; }")
        sb.append("h2 { margin: 20px 0 8px; font-size: 18px; }")
        sb.append("table { border-collapse: collapse; width: 100%; margin-bottom: 24px; }")
        sb.append("th, td { border: 1px solid #999; padding: 6px 8px; text-align: left; vertical-align: top; font-size: 13px; }")
        sb.append("th { background: #eee; font-weight: bold; }")
        sb.append("tr:nth-child(even) td { background: #fafafa; }")
        sb.append("</style></head><body>")
        tables.forEach { table ->
            val visible = table.fieldNames.indices.filter { it !in (hiddenFields[table.deckId] ?: emptySet()) }
            if (visible.isEmpty()) return@forEach
            sb.append("<h2>").append(escape(table.deckName)).append("</h2>")
            sb.append("<table><thead><tr>")
            visible.forEach { sb.append("<th>").append(escape(table.fieldNames[it])).append("</th>") }
            sb.append("</tr></thead><tbody>")
            table.rows.forEach { row ->
                sb.append("<tr>")
                visible.forEach { sb.append("<td>").append(escape(row.fieldValues.getOrElse(it) { "" })).append("</td>") }
                sb.append("</tr>")
            }
            sb.append("</tbody></table>")
        }
        sb.append("</body></html>")
        return sb.toString()
    }

    private fun escape(raw: String): String = raw
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
