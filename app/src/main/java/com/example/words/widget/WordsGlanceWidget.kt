package com.example.words.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.example.words.application.dependencyContainer
import com.example.words.widget.ui.WordsWidgetContent

class WordsGlanceWidget : GlanceAppWidget() {

    // Other modes have double click trigger bug https://issuetracker.google.com/issues/327475242
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val viewModel = context.dependencyContainer.getWordsWidgetViewModel(id)

        provideContent {
            WordsWidgetContent(uiState = viewModel.uiState.collectAsState(WidgetUiState(isLoading = true)).value)
        }
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        super.onDelete(context, glanceId)
        context.dependencyContainer.getWordsWidgetViewModel(glanceId).deleteWidget()
    }
}

class WordsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = WordsGlanceWidget()
}
