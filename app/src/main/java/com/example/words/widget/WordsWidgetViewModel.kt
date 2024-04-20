package com.example.words.widget

import android.util.Log
import com.example.words.repository.WidgetSettingsRepository
import com.example.words.repository.WordsRepository
import com.example.words.repository.WordsSynchronizer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class WordsWidgetViewModel(
    private val appWidgetId: Int,
    widgetSettingsRepository: WidgetSettingsRepository,
    private val wordsSynchronizer: WordsSynchronizer,
    private val wordsRepository: WordsRepository
) {
    private val shouldReload = MutableStateFlow(false)
    private val isLoadingFlow = MutableStateFlow(false)
    private val widgetSettings = widgetSettingsRepository.observeSettings(appWidgetId).filterNotNull()

    val widgetDetailsState: Flow<WidgetDetailsState> = widgetSettings.map { widgetSettings ->
        WidgetDetailsState(
            sheetName = widgetSettings.sheetName,
            lastUpdatedAt = widgetSettings.lastUpdatedAt?.let { lastUpdatedAt ->
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                    .withZone(ZoneId.systemDefault())
                    .format(lastUpdatedAt)
            }.orEmpty()
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val wordsState: Flow<WidgetState> = widgetSettings
        .filterNotNull()
        .distinctUntilChanged { old, new -> old.spreadsheetId == new.spreadsheetId && old.sheetId == new.sheetId }
        .combine(shouldReload) { widget, _ -> widget }
        .flatMapLatest { widget ->
            wordsRepository.observeRandomWords(widget.spreadsheetId, widget.sheetId)
                .map { words -> if (words == null) WidgetState.Failure else WidgetState.Success(words) }
                .onEach { isLoadingFlow.value = false }
        }
        .combine(isLoadingFlow) { widgetState, isLoading ->
            if (isLoading) {
                WidgetState.InProgress
            } else {
                widgetState
            }
        }
        .catch { Log.e(javaClass.name, "", it); emit(WidgetState.Failure) }

    fun reloadWords() {
        shouldReload.value = !shouldReload.value
    }

    suspend fun synchronizeWords() {
        isLoadingFlow.value = true
        wordsSynchronizer.synchronizeWords(appWidgetId)
    }
}

data class WidgetDetailsState(
    val sheetName: String,
    val lastUpdatedAt: String,
) {
    companion object {
        val Empty = WidgetDetailsState(sheetName = "", lastUpdatedAt = "")
    }
}

sealed interface WidgetState {
    data object InProgress : WidgetState
    data object Failure : WidgetState
    class Success(val words: List<Pair<String, String>>) : WidgetState
}