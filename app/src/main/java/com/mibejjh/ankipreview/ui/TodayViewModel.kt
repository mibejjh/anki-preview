package com.mibejjh.ankipreview.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mibejjh.ankipreview.data.anki.AnkiRepository
import com.mibejjh.ankipreview.data.model.Deck
import com.mibejjh.ankipreview.data.model.TodayPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 오늘 카드 화면의 Ui 상태. */
sealed interface TodayUiState {
    data object Loading : TodayUiState
    data class Success(val plan: TodayPlan) : TodayUiState
    data class Error(val message: String) : TodayUiState
}

/**
 * 오늘 카드를 조회하고 UI 상태를 노출하는 ViewModel.
 * [AnkiRepository] 를 생성자 주입받아 실제 구현체 교체가 쉽다.
 */
class TodayViewModel(
    private val repository: AnkiRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TodayUiState>(TodayUiState.Loading)
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private val _fontScale = MutableStateFlow(1f)
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    /** 선택 가능한 전체 덱 목록. */
    private val _allDecks = MutableStateFlow<List<Deck>>(emptyList())
    val allDecks: StateFlow<List<Deck>> = _allDecks.asStateFlow()

    /** 선택된 덱 id 집합. 빈 집합이면 전체 덱을 의미한다. */
    private val _selectedDeckIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedDeckIds: StateFlow<Set<Long>> = _selectedDeckIds.asStateFlow()

    init {
        loadDecks()
        load()
    }

    /** 전체 덱 목록을 불러온다. */
    private fun loadDecks() {
        viewModelScope.launch {
            _allDecks.value = try {
                if (repository.isAvailable()) repository.getDecks() else emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    /** 오늘 카드 계획을 다시 불러온다. */
    fun load() {
        _uiState.value = TodayUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                if (!repository.isAvailable()) {
                    TodayUiState.Error(
                        "AnkiDroid 3rd party API를 사용할 수 없습니다. " +
                            "AnkiDroid 설정 > 고급 > 'AnkiDroid API 사용'을 켜주세요.",
                    )
                } else {
                    TodayUiState.Success(repository.getTodayPlan(_selectedDeckIds.value))
                }
            } catch (e: Exception) {
                TodayUiState.Error(e.message ?: "오늘 카드를 불러오지 못했습니다.")
            }
        }
    }

    /** 덱 선택을 토글한다. */
    fun toggleDeck(deckId: Long) {
        val current = _selectedDeckIds.value
        _selectedDeckIds.value = if (deckId in current) current - deckId else current + deckId
        load()
    }

    /** 모든 덱을 선택한다 (빈 집합 = 전체). */
    fun selectAll() {
        _selectedDeckIds.value = emptySet()
        load()
    }

    /** 선택을 모두 해제한다. */
    fun clearSelection() {
        _selectedDeckIds.value = emptySet()
        load()
    }

    /** 목록 글자 크기 배율을 설정한다. */
    fun setFontScale(scale: Float) {
        _fontScale.value = scale.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
    }

    companion object {
        const val MIN_FONT_SCALE = 0.8f
        const val MAX_FONT_SCALE = 3f

        /** [AnkiRepository] 를 주입하는 팩토리. 앱 진입점에서 실제/가짜 저장소를 공급한다. */
        fun factory(repository: AnkiRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TodayViewModel(repository) as T
            }
        }
    }
}