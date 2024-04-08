package com.example.words.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.provideContent
import com.example.words.DependencyContainer
import com.example.words.repository.WordsRepository
import com.example.words.settings.WidgetSettings
import com.example.words.settings.settingsDataStore
import com.example.words.widget.ui.WordsWidgetContent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class WordsGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)

        val diContainer = context.applicationContext as DependencyContainer
        val widgetSettingsRepository = diContainer.getWidgetSettingsRepository()
        val widgetSettings = widgetSettingsRepository.observeSettings(appWidgetId)
        val wordsSynchronizer = diContainer.getWordsSynchronizer()

        val (widgetState, shuffleWords) = WidgetStateProvider(widgetSettings, diContainer.getWordsRepository())

        provideContent {
            val scope = rememberCoroutineScope()
            val widgetSettingsState by widgetSettings.collectAsState(initial = null)

            WordsWidgetContent(
                widgetState = widgetState.collectAsState(initial = WidgetState.InProgress).value,
                sheetName = widgetSettingsState?.sheetName.orEmpty(),
                lastUpdatedAt = widgetSettingsState?.lastUpdatedAt?.let {
                    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                        .withZone(ZoneId.systemDefault())
                        .format(it)
                }.orEmpty(),
                onReload = shuffleWords,
                onSynchronize = { scope.launch { wordsSynchronizer.synchronizeWords(appWidgetId) } }
            )
        }
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        super.onDelete(context, glanceId)
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        context.settingsDataStore.updateData { settings ->
            settings.toBuilder()
                .removeWidgets(settings.widgetsList.indexOfFirst { it.widgetId == appWidgetId })
                .build()
        }
    }
}

private class WidgetStateProvider(widgetSettings: Flow<WidgetSettings?>, repository: WordsRepository) {
    val shouldRefresh = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val widgetState: StateFlow<WidgetState> = widgetSettings
        .filterNotNull()
        .combine(shouldRefresh) { widget, _ -> widget }
        .flatMapLatest { widget ->
            flow {
                emit(WidgetState.InProgress)
                emitAll(
                    repository.observeRandomWords(widget.spreadsheetId, widget.sheetId).map { words ->
                        if (words == null) WidgetState.Failure else WidgetState.Success(words)
                    }
                )
            }
        }
        .catch { Log.e(javaClass.name, "", it); emit(WidgetState.Failure) }
        .stateIn()

    fun shuffleWords() {
        shouldRefresh.value = !shouldRefresh.value
    }

    fun setIsLoading() {

    }

    operator fun component1() = widgetState
    operator fun component2() = shuffleWords
}

sealed interface WidgetState {
    data object InProgress : WidgetState
    data object Failure : WidgetState
    class Success(val words: List<Pair<String, String>>) : WidgetState
}