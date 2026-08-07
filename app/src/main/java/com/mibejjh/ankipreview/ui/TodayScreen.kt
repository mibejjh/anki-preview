package com.mibejjh.ankipreview.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mibejjh.ankipreview.data.model.Card
import com.mibejjh.ankipreview.data.model.CardType
import com.mibejjh.ankipreview.data.model.Deck
import com.mibejjh.ankipreview.data.model.DeckPlan
import com.mibejjh.ankipreview.data.model.TodayPlan
import com.mibejjh.ankipreview.ui.theme.LearnBadge
import com.mibejjh.ankipreview.ui.theme.NewBadge
import com.mibejjh.ankipreview.ui.theme.RelearnBadge
import com.mibejjh.ankipreview.ui.theme.ReviewBadge

/**
 * 오늘 카드 화면. ViewModel 을 주입받아 상태를 수집한다.
 * @param repositoryProvider 실제 저장소를 공급하는 팩토리 (기본값은 Fake).
 */
@Composable
fun TodayRoute(
    repositoryProvider: () -> com.mibejjh.ankipreview.data.anki.AnkiRepository,
    modifier: Modifier = Modifier,
) {
    val viewModel: TodayViewModel = viewModel(factory = TodayViewModel.factory(repositoryProvider()))
    TodayScreen(viewModel = viewModel, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val allDecks by viewModel.allDecks.collectAsStateWithLifecycle()
    val selectedDeckIds by viewModel.selectedDeckIds.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val tts = remember { TtsManager(context) }
    DisposableEffect(Unit) {
        tts.init()
        onDispose { tts.shutdown() }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("오늘의 카드") },
                actions = {
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                    }
                    IconButton(onClick = {
                        (uiState as? TodayUiState.Success)?.let { AnkiActions.shareTodayPlan(context, it.plan) }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "공유 / 인쇄")
                    }
                    IconButton(onClick = { AnkiActions.launchAnkiDroid(context) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "AnkiDroid에서 학습")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            FontSizeSlider(
                scale = fontScale,
                onScaleChange = viewModel::setFontScale,
            )
            DeckFilterRow(
                decks = allDecks,
                selectedDeckIds = selectedDeckIds,
                onToggleDeck = viewModel::toggleDeck,
                onSelectAll = viewModel::selectAll,
            )
            when (val state = uiState) {
                is TodayUiState.Loading -> LoadingState()
                is TodayUiState.Error -> ErrorState(message = state.message, onRetry = viewModel::load)
                is TodayUiState.Success -> TodayContent(
                    plan = state.plan,
                    fontScale = fontScale,
                    onPronounce = tts::speak,
                )
            }
        }
    }
}

@Composable
private fun DeckFilterRow(
    decks: List<Deck>,
    selectedDeckIds: Set<Long>,
    onToggleDeck: (Long) -> Unit,
    onSelectAll: () -> Unit,
) {
    if (decks.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedDeckIds.isEmpty(),
            onClick = onSelectAll,
            label = { Text("전체") },
        )
        decks.forEach { deck ->
            FilterChip(
                selected = deck.id in selectedDeckIds,
                onClick = { onToggleDeck(deck.id) },
                label = { Text("${deck.name} (${deck.totalDue})") },
            )
        }
    }
}

@Composable
private fun FontSizeSlider(scale: Float, onScaleChange: (Float) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("A", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        Slider(
            value = scale,
            onValueChange = onScaleChange,
            valueRange = TodayViewModel.MIN_FONT_SCALE..TodayViewModel.MAX_FONT_SCALE,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        Text(
            "A",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun TodayContent(
    plan: TodayPlan,
    fontScale: Float,
    onPronounce: (String) -> Unit,
) {
    if (plan.decks.isEmpty() || plan.totalCards == 0) {
        EmptyState()
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = "총 ${plan.totalCards}장 · ${plan.decks.size}개 덱",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        plan.decks.forEach { deckPlan ->
            item(key = "deck-${deckPlan.deck.id}") {
                DeckSectionHeader(deckPlan = deckPlan, fontScale = fontScale)
            }
            items(deckPlan.cards, key = { "card-${it.id}" }) { card ->
                CardRow(card = card, fontScale = fontScale, onPronounce = onPronounce)
            }
        }
    }
}

@Composable
private fun DeckSectionHeader(deckPlan: DeckPlan, fontScale: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = deckPlan.deck.name,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = (22 * fontScale).sp,
                fontWeight = FontWeight.Bold,
            ),
            modifier = Modifier.weight(1f),
        )
        TypeCountBadge(count = deckPlan.newCount, label = "신규", color = NewBadge)
        Spacer(Modifier.width(4.dp))
        TypeCountBadge(count = deckPlan.learnCount, label = "학습", color = LearnBadge)
        Spacer(Modifier.width(4.dp))
        TypeCountBadge(count = deckPlan.reviewCount, label = "복습", color = ReviewBadge)
    }
}

@Composable
private fun TypeCountBadge(count: Int, label: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        color = color.copy(alpha = 0.18f),
        contentColor = color,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = "$label $count",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun CardRow(
    card: Card,
    fontScale: Float,
    onPronounce: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TypeBadge(type = card.type)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = card.questionSimple.ifBlank { card.question },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = (22 * fontScale).sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                Text(
                    text = card.answerSimple.ifBlank { card.answer },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = (17 * fontScale).sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            IconButton(onClick = { onPronounce(card.questionSimple.ifBlank { card.question }) }) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "발음",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun TypeBadge(type: CardType?) {
    val (label, color) = when (type) {
        CardType.NEW -> "신규" to NewBadge
        CardType.LEARNING -> "학습" to LearnBadge
        CardType.REVIEW -> "복습" to ReviewBadge
        CardType.RELEARNING -> "재학습" to RelearnBadge
        null -> "?" to MaterialTheme.colorScheme.outline
    }
    Surface(
        color = color.copy(alpha = 0.18f),
        contentColor = color,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "오늘 예정된 카드가 없습니다",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "덱에 새 카드를 추가하거나 복습을 예약해 보세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "카드를 불러오지 못했습니다",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "AnkiDroid가 설치되어 있고, 설정 > 고급 > 'AnkiDroid API 사용'이 켜져 있는지 확인하세요.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        TextButton(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
            @Suppress("DEPRECATION")
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("다시 시도")
        }
    }
}