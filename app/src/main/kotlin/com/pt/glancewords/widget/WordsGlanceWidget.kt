package com.pt.glancewords.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.pt.glancewords.widget.ui.WordsWidgetContent
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

class WordsGlanceWidget : GlanceAppWidget(), KoinComponent {

    // Other modes have double click trigger bug https://issuetracker.google.com/issues/327475242
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val viewModel = get<WordsWidgetViewModel> { parametersOf(id) }

        provideContent {
            WordsWidgetContent(uiState = viewModel.uiState.collectAsState(WidgetUiState(isLoading = true)).value)
        }
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        super.onDelete(context, glanceId)
        val viewModel = get<WordsWidgetViewModel> { parametersOf(glanceId) }
        viewModel.deleteWidget()
    }
}

class WordsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = WordsGlanceWidget()
}
