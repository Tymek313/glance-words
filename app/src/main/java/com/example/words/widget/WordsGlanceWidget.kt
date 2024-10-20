package com.example.words.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.example.words.DependencyContainer
import com.example.words.model.Widget
import com.example.words.widget.ui.WordsWidgetContent
import java.time.ZoneId
import java.util.Locale

class WordsGlanceWidget : GlanceAppWidget() {

    // Other modes have double click trigger bug https://issuetracker.google.com/issues/327475242
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val viewModel = createViewModel(context, id, context.applicationContext as DependencyContainer)

        provideContent {
            WordsWidgetContent(uiState = viewModel.uiState.collectAsState(WidgetUiState(isLoading = true)).value)
        }
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        super.onDelete(context, glanceId)
        createViewModel(context, glanceId, context.applicationContext as DependencyContainer).deleteWidget()
    }

    private fun createViewModel(context: Context, widgetId: GlanceId, diContainer: DependencyContainer) = WordsWidgetViewModel(
        Widget.WidgetId(GlanceAppWidgetManager(context).getAppWidgetId(widgetId)),
        diContainer.widgetRepository,
        diContainer.wordsRepository,
        diContainer.widgetLoadingStateNotifier,
        diContainer.logger,
        Locale.getDefault(),
        ZoneId.systemDefault(),
        diContainer.reshuffleNotifier
    )
}

class WordsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = WordsGlanceWidget()
}
