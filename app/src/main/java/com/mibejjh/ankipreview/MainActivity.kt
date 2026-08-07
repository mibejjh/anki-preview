package com.mibejjh.ankipreview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mibejjh.ankipreview.ui.theme.AnkiPreviewTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnkiPreviewTheme {
                // Step 1 플레이스홀더. 실제 화면은 ui worktree에서 구현.
                PlaceholderScreen()
            }
        }
    }
}

@Composable
fun PlaceholderScreen(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Anki Preview",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "계약 정의 완료 — UI/데이터 구현 준비됨",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlaceholderPreview() {
    AnkiPreviewTheme { PlaceholderScreen() }
}