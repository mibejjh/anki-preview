package com.mibejjh.ankipreview.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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
    val hiddenMaskFields by viewModel.hiddenMaskFields.collectAsStateWithLifecycle()
    val revealedNoteIds by viewModel.revealedNoteIds.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showFontDialog by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (!isLandscape) {
                TopAppBar(
                    title = { },
                    actions = {
                        HeaderActions(
                            fontScale = fontScale,
                            onFontClick = { showFontDialog = true },
                            onRefresh = viewModel::load,
                            onPrint = {
                                (uiState as? TodayUiState.Success)?.let {
                                    PrintHelper.print(context, it.tables, hiddenFields)
                                }
                            },
                            onLaunchAnki = { AnkiActions.launchAnkiDroid(context) },
                        )
                    },
                )
            }
        },
    ) { innerPadding ->
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                // 좌측 제어판
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.35f)
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HeaderActions(
                            fontScale = fontScale,
                            onFontClick = { showFontDialog = true },
                            onRefresh = viewModel::load,
                            onPrint = {
                                (uiState as? TodayUiState.Success)?.let {
                                    PrintHelper.print(context, it.tables, hiddenFields)
                                }
                            },
                            onLaunchAnki = { AnkiActions.launchAnkiDroid(context) },
                        )
                    }
                    Text("덱 선택", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))
                    allDecks.forEach { deck ->
                        FilterChip(
                            selected = deck.id in selectedDeckIds,
                            onClick = { viewModel.toggleDeck(deck.id) },
                            label = { Text("${deck.name} (${deck.totalDue})", style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("글자 크기", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))
                    FontSizeSlider(scale = fontScale, onScaleChange = viewModel::setFontScale)
                }
                // 우측 테이블
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.65f),
                ) {
                    when (val state = uiState) {
                        is TodayUiState.Loading -> LoadingState()
                        is TodayUiState.Error -> ErrorState(message = state.message, onRetry = viewModel::load)
                        is TodayUiState.Success -> TodayContent(
                            tables = state.tables,
                            hiddenFields = hiddenFields,
                            fontScale = fontScale,
                            onToggleField = viewModel::toggleField,
                            hiddenMaskFields = hiddenMaskFields,
                            revealedNoteIds = revealedNoteIds,
                            onToggleColumnMask = viewModel::toggleColumnMask,
                            onToggleRevealRow = viewModel::toggleRevealRow,
                            emptyMessage = if (selectedDeckIds.isEmpty()) "덱을 선택하세요" else "표시할 노트가 없습니다",
                        )
                    }
                }
            }
            // 바텀시트
            if (showFontDialog) {
                ModalBottomSheet(
                    onDismissRequest = { showFontDialog = false },
                    sheetState = rememberModalBottomSheetState(),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                        Text("글자 크기", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(16.dp))
                        FontSizeSlider(scale = fontScale, onScaleChange = viewModel::setFontScale)
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                DeckFilterRow(
                    decks = allDecks,
                    selectedDeckIds = selectedDeckIds,
                    onToggleDeck = viewModel::toggleDeck,
                )
                // 폰트 크기 조절 바텀시트
                if (showFontDialog) {
                    ModalBottomSheet(
                        onDismissRequest = { showFontDialog = false },
                        sheetState = rememberModalBottomSheetState(),
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                            Text("글자 크기", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(16.dp))
                            FontSizeSlider(scale = fontScale, onScaleChange = viewModel::setFontScale)
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
                when (val state = uiState) {
                    is TodayUiState.Loading -> LoadingState()
                    is TodayUiState.Error -> ErrorState(message = state.message, onRetry = viewModel::load)
                    is TodayUiState.Success -> TodayContent(
                        tables = state.tables,
                        hiddenFields = hiddenFields,
                        fontScale = fontScale,
                        onToggleField = viewModel::toggleField,
                        hiddenMaskFields = hiddenMaskFields,
                        revealedNoteIds = revealedNoteIds,
                        onToggleColumnMask = viewModel::toggleColumnMask,
                        onToggleRevealRow = viewModel::toggleRevealRow,
                        emptyMessage = if (selectedDeckIds.isEmpty()) "덱을 선택하세요" else "표시할 노트가 없습니다",
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.HeaderActions(
    fontScale: Float,
    onFontClick: () -> Unit,
    onRefresh: () -> Unit,
    onPrint: () -> Unit,
    onLaunchAnki: () -> Unit,
) {
    IconButton(onClick = onFontClick) {
        Text(
            text = "${String.format("%.1f", fontScale)}x",
            style = MaterialTheme.typography.labelMedium,
        )
    }
    IconButton(onClick = onRefresh) {
        Icon(Icons.Default.Refresh, contentDescription = "새로고침")
    }
    IconButton(onClick = onPrint) {
        Icon(Icons.Default.Print, contentDescription = "인쇄")
    }
    IconButton(onClick = onLaunchAnki) {
        Icon(Icons.Default.PlayArrow, contentDescription = "AnkiDroid에서 학습")
    }
}

@Composable
private fun DeckFilterRow(
    decks: List<Deck>,
    selectedDeckIds: Set<Long>,
    onToggleDeck: (Long) -> Unit,
) {
    if (decks.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
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
    hiddenMaskFields: Map<Long, Set<Int>>,
    revealedNoteIds: Set<Long>,
    onToggleColumnMask: (Long, Int) -> Unit,
    onToggleRevealRow: (Long) -> Unit,
    emptyMessage: String,
) {
    if (tables.isEmpty()) {
        EmptyState(emptyMessage)
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
                    maskedFields = hiddenMaskFields[table.deckId] ?: emptySet(),
                    revealedNoteIds = revealedNoteIds,
                    onToggleColumnMask = { onToggleColumnMask(table.deckId, it) },
                    onToggleRevealRow = onToggleRevealRow,
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
    maskedFields: Set<Int>,
    revealedNoteIds: Set<Long>,
    onToggleColumnMask: (Int) -> Unit,
    onToggleRevealRow: (Long) -> Unit,
) {
    val visibleIndices = table.fieldNames.indices.filter { it !in hiddenFields }
    val hasMaskedColumns = maskedFields.isNotEmpty()
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
            // 헤더 행 (열 이름이 가리기/보이기 토글) 
            Row { 
                visibleIndices.forEach { i -> 
                    TableCell( 
                        text = table.fieldNames[i], 
                        isHeader = true, 
                        fontScale = fontScale, 
                        hidden = i in maskedFields, 
                        onClick = { onToggleColumnMask(i) }, 
                    ) 
                } 
            }
            // 데이터 행
            table.rows.forEach { row ->
                val rowRevealed = row.noteId in revealedNoteIds
                Row(
                    modifier = if (hasMaskedColumns) {
                        Modifier.clickable { onToggleRevealRow(row.noteId) }
                    } else {
                        Modifier
                    },
                ) {
                    visibleIndices.forEach { i ->
                        val cellHidden = i in maskedFields && !rowRevealed
                        TableCell(
                            text = if (cellHidden) "?" else row.fieldValues.getOrElse(i) { "" },
                            isHeader = false,
                            fontScale = fontScale,
                            hidden = cellHidden,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TableCell(text: String, isHeader: Boolean, fontScale: Float, hidden: Boolean = false, onClick: (() -> Unit)? = null) {
    val bg = if (isHeader) {
        if (hidden) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        else MaterialTheme.colorScheme.surfaceVariant
    } else if (hidden) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (isHeader && hidden) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
    } else if (hidden) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        color = bg,
        modifier = Modifier.width(CELL_WIDTH)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
    ) {
        Text(
            text = text,
            style = if (isHeader) {
                MaterialTheme.typography.bodyMedium.copy(
                    fontSize = (14 * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hidden) contentColor else MaterialTheme.colorScheme.onSurface,
                )
            } else {
                MaterialTheme.typography.bodyMedium.copy(
                    fontSize = (14 * fontScale).sp,
                    color = if (hidden) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
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
private fun EmptyState(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
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
