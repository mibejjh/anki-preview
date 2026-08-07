package com.mibejjh.ankipreview.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/** 오늘 목록 공유 및 AnkiDroid 실행 관련 유틸. */
object AnkiActions {

    const val ANKIDROID_PACKAGE = "com.ichi2.anki"

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
}