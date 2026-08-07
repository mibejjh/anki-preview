package com.mibejjh.ankipreview.data.anki

/**
 * HTML 태그/엔티티를 제거해 순수 텍스트로 만든다. JVM 단위 테스트 대상.
 */
fun stripHtml(raw: String): String {
    var s = raw.replace(Regex("<[^>]+>"), " ")
    s = s.replace("&nbsp;", " ").replace("&amp;", "&")
        .replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&#39;", "'")
        .replace("&#x27;", "'").replace("&#x2F;", "/")
    return s.replace(Regex("\\s+"), " ").trim()
}
