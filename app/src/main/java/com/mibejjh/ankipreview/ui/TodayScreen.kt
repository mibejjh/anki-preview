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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mibejjh.ankipreview.data.model.Deck
import com.mibejjh.ankipreview.data.model.NoteRow
import com.mibejjh.ankipreview.data.model.NoteTable

/**
 * 오늘 카드 화면. ViewModel 을 주입받아 상태를 수집한다.
 * @param repositoryProvider 실제 저장소를 공급하는 팩토리.
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
    val hiddenFields by viewModel.hiddenFields.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                        (uiState as? TodayUiState.Success)?.let {
                            PrintHelper.print(context, it.tables, hiddenFields)
                        }
                    }) {
                        Icon(Icons.Default.Print, contentDescription = "인쇄")
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
                    tables = state.tables,
                    hiddenFields = hiddenFields,
                    fontScale = fontScale,
                    onToggleField = viewModel::toggleField,
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
    tables: List<NoteTable>,
    hiddenFields: Map<Long, Set<Int>>,
    fontScale: Float,
    onToggleField: (Long, Int) -> Unit,
) {
    if (tables.isEmpty()) {
        EmptyState()
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "총 ${tables.sumOf { it.rows.size }}장 · ${tables.size}개 덱",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        tables.forEach { table ->
            item(key = "table-${table.deckId}") {
                NoteTableSection(
                    table = table,
                    hiddenFields = hiddenFields[table.deckId] ?: emptySet(),
                    fontScale = fontScale,
                    onToggleField = { onToggleField(table.deckId, it) },
                )
            }
        }
    }
}

@Composable
private fun NoteTableSection(
    table: NoteTable,
    hiddenFields: Set<Int>,
    fontScale: Float,
    onToggleField: (Int) -> Unit,
) {
    val visibleIndices = table.fieldNames.indices.filter { it !in hiddenFields }
    Column {
        Text(
            text = table.deckName,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = (22 * fontScale).sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        // 필드(열) 선택 칩
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            table.fieldNames.forEachIndexed { index, name ->
                FilterChip(
                    selected = index !in hiddenFields,
                    onClick = { onToggleField(index) },
                    label = { Text(name) },
                )
            }
        }
        if (visibleIndices.isEmpty()) {
            Text(
                text = "표시할 필드를 선택하세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            return
        }
        // 테이블 (헤더+행이 함께 가로 스크롤)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            Row {
                visibleIndices.forEach { i ->
                    TableCell(
                        text = table.fieldNames[i],
                        isHeader = true,
                        fontScale = fontScale,
                    )
                }
            }
            table.rows.forEach { row ->
                Row {
                    visibleIndices.forEach { i ->
                        TableCell(
                            text = row.fieldValues.getOrElse(i) { "" },
                            isHeader = false,
                            fontScale = fontScale,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TableCell(text: String, isHeader: Boolean, fontScale: Float) {
    val bg = if (isHeader) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    Surface(
        color = bg,
        modifier = Modifier.width(CELL_WIDTH),
    ) {
        Text(
            text = text,
            style = if (isHeader) {
                MaterialTheme.typography.bodyMedium.copy(
                    fontSize = (14 * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                MaterialTheme.typography.bodyMedium.copy(fontSize = (14 * fontScale).sp)
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }
}

private val CELL_WIDTH = 150.dp

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
            text = "표시할 노트가 없습니다",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "덱을 선택하거나 새 카드를 추가해 보세요.",
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
