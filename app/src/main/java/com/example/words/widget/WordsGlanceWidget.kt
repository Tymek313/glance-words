package com.example.words.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.collectAsState
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
        val wordsRepository = diContainer.getWordsRepository()
        val wordsSynchronizer = diContainer.getWordsSynchronizer()

        val (widgetState, shuffleWords) = createWidgetState(widgetSettings, wordsRepository)

        provideContent {
            val scope = rememberCoroutineScope()
            WordsWidgetContent(
                widgetState = widgetState.collectAsState(initial = WidgetState.InProgress).value,
                sheetName = widgetSettings.map { settings -> settings?.sheetName.orEmpty() }.collectAsState(initial = "").value,
                lastUpdatedAt = widgetSettings.map { settings ->
                    settings?.lastUpdatedAt?.let {
                        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                            .withZone(ZoneId.systemDefault())
                            .format(it)
                    }.orEmpty()
                }.collectAsState(initial = "").value,
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

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun createWidgetState(widgetSettings: Flow<WidgetSettings?>, repository: WordsRepository): WidgetStateProvider {
        val shouldRefresh = MutableStateFlow(false)
        return WidgetStateProvider(
            widgetState = widgetSettings
                .filterNotNull()
                .combine(shouldRefresh) { widget, _ -> widget }
                .flatMapLatest { widget -> repository.observeRandomWords(widget.spreadsheetId, widget.sheetId) }
                .catch { Log.e(javaClass.name, "", it); emit(null) }
                .map { words -> if (words == null) WidgetState.Failure else WidgetState.Success(words) },
            shuffleWords = { shouldRefresh.value = !shouldRefresh.value }
        )
    }
}

private class WidgetStateProvider(val widgetState: Flow<WidgetState>, val shuffleWords: () -> Unit) {
    operator fun component1() = widgetState
    operator fun component2() = shuffleWords
}

sealed interface WidgetState {
    data object InProgress : WidgetState
    data object Failure : WidgetState
    class Success(val words: List<Pair<String, String>>) : WidgetState
}