package com.mibejjh.ankipreview.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.mibejjh.ankipreview.data.model.TodayPlan

/** 오늘 목록 공유(인쇄/내보내기) 및 AnkiDroid 실행 관련 유틸. */
object AnkiActions {

    const val ANKIDROID_PACKAGE = "com.ichi2.anki"

    /** 오늘 카드 목록을 plain text 로 만들어 공유 시트(인쇄/클라우드 등)를 띄운다. */
    fun shareTodayPlan(context: Context, plan: TodayPlan) {
        val text = buildString {
            appendLine("Anki Preview — 오늘의 카드 ($dateLabel)")
            appendLine("총 ${plan.totalCards}장")
            plan.decks.forEach { deckPlan ->
                appendLine()
                appendLine("## ${deckPlan.deck.name} (${deckPlan.cards.size}장)")
                deckPlan.cards.forEach { card ->
                    appendLine("- ${card.questionSimple} : ${card.answerSimple}")
                }
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Anki Preview 오늘의 카드 $dateLabel")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "공유 / 인쇄"))
    }

    /** AnkiDroid 학습 화면을 실행한다(베스트 에포트). */
    fun launchAnkiDroid(context: Context) {
        val pm = context.packageManager
        val intent = resolveLaunchIntent(pm)
            ?: Intent(Intent.ACTION_MAIN).putExtra(Intent.EXTRA_PACKAGE_NAME, ANKIDROID_PACKAGE)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // 사용자가 AnkiDroid 미설치/비활성화한 경우 무시(베스트 에포트)
        }
    }

    private fun resolveLaunchIntent(pm: PackageManager): Intent? =
        pm.getLaunchIntentForPackage(ANKIDROID_PACKAGE)

    private val dateLabel: String
        get() = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
}