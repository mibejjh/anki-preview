package com.mibejjh.ankipreview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mibejjh.ankipreview.data.anki.AnkiDroidRepository
import com.mibejjh.ankipreview.data.anki.AnkiRepository
import com.mibejjh.ankipreview.ui.TodayRoute
import com.mibejjh.ankipreview.ui.theme.AnkiPreviewTheme

class MainActivity : ComponentActivity() {

    /**
     * 오늘 카드 공급 저장소.
     * 실제 AnkiDroid ContentProvider 구현체를 사용한다.
     * AnkiDroid 미설치/API 비활성 시 ViewModel 이 안내 문구를 표시한다.
     */
    private val repositoryProvider: () -> AnkiRepository by lazy {
        { AnkiDroidRepository(applicationContext) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnkiPreviewTheme {
                TodayRoute(repositoryProvider = repositoryProvider)
            }
        }
    }
}