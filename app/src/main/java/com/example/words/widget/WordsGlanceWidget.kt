package com.example.words.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.example.words.DependencyContainer
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.util.Locale

class WordsGlanceWidget : GlanceAppWidget() {

    // Other modes have double click trigger bug https://issuetracker.google.com/issues/327475242
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val viewModel = createViewModel(context, id, context.applicationContext as DependencyContainer)

        provideContent {
            val scope = rememberCoroutineScope()
            WordsWidgetContent(
                widgetState = viewModel.wordsState.collectAsState(WidgetState.InProgress).value,
                widgetDetailsState = viewModel.widgetDetailsState.collectAsState(WidgetDetailsState.Empty).value,
                onReload = viewModel::reshuffleWords,
                onSynchronize = { scope.launch { viewModel.synchronizeWords() } }
            )
        }
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        super.onDelete(context, glanceId)
        createViewModel(context, glanceId, context.applicationContext as DependencyContainer).deleteWidget()
    }

    private fun createViewModel(context: Context, widgetId: GlanceId, diContainer: DependencyContainer) = WordsWidgetViewModel(
        GlanceAppWidgetManager(context).getAppWidgetId(widgetId),
        diContainer.getWidgetSettingsRepository(),
        diContainer.getWordsSynchronizer(),
        diContainer.getWordsRepository(),
        diContainer.getLogger(),
        Locale.getDefault(),
        ZoneId.systemDefault()
    )
}