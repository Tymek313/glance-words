package com.example.words.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.provideContent
import com.example.words.WidgetSettings
import com.example.words.repository.SheetsProvider
import com.example.words.repository.WordsRepository
import com.example.words.settings.settingsDataStore
import com.example.words.widget.ui.WordsWidgetContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

class WordsGlanceWidget : GlanceAppWidget() {

    private val wordsRepository = WordsRepository(SheetsProvider.sheets)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val widgetSettings = context.settingsDataStore.data.map { settings -> settings.widgetsList.find { it.widgetId == appWidgetId } }
        val (widgetState, shuffleWords) = createWidgetState(widgetSettings)

        provideContent {
            WordsWidgetContent(
                widgetState = widgetState.collectAsState(initial = WidgetState.InProgress).value,
                sheetName = widgetSettings.map { it?.sheetName.orEmpty() }.collectAsState(initial = "").value,
                onReload = shuffleWords
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

    private fun createWidgetState(widgetSettings: Flow<WidgetSettings?>): WidgetStateProvider {
        val shouldRefresh = MutableStateFlow(false)
        return WidgetStateProvider(
            widgetState = widgetSettings
                .filterNotNull()
                .combine(shouldRefresh) { widget, _ -> wordsRepository.load100RandomFromRemote(widget.spreadsheetId, widget.sheetId) }
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