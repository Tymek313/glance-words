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
import com.example.words.settings.WidgetSettings
import kotlinx.coroutines.launch

class WordsGlanceWidget : GlanceAppWidget() {

    // Other modes have double click trigger bug https://issuetracker.google.com/issues/327475242
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val diContainer = context.applicationContext as DependencyContainer
        val viewModel = WordsWidgetViewModel(
            GlanceAppWidgetManager(context).getAppWidgetId(id),
            diContainer.getWidgetSettingsRepository(),
            diContainer.getWordsSynchronizer(),
            diContainer.getWordsRepository()
        )

        provideContent {
            val scope = rememberCoroutineScope()
            WordsWidgetContent(
                widgetState = viewModel.wordsState.collectAsState(WidgetState.InProgress).value,
                widgetDetailsState = viewModel.widgetDetailsState.collectAsState(WidgetDetailsState.Empty).value,
                onReload = viewModel::reloadWords,
                onSynchronize = { scope.launch { viewModel.synchronizeWords() } }
            )
        }
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        super.onDelete(context, glanceId)
        val widgetId = WidgetSettings.WidgetId(GlanceAppWidgetManager(context).getAppWidgetId(glanceId))
        val diContainer = context.applicationContext as DependencyContainer
        diContainer.getWidgetSettingsRepository().deleteWidget(widgetId)
    }
}