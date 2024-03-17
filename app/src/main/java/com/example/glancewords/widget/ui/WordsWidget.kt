package com.example.glancewords.widget.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextDefaults
import androidx.glance.text.TextStyle
import com.example.glancewords.R
import com.example.glancewords.repository.CachingWordsRepository
import com.example.glancewords.widget.WordsWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun WordsWidget() {
    GlanceTheme {
        WordsWidgetContent()
    }
}

@Composable
fun WordsWidgetContent() {
    val widgetState by loadWidgetState(LocalContext.current)

    Column(GlanceModifier.fillMaxSize().appWidgetBackground().background(GlanceTheme.colors.widgetBackground).padding(8.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = GlanceModifier.fillMaxWidth().defaultWeight().padding(bottom = 8.dp)) {
            when (val state = widgetState) {
                WidgetState.InProgress -> CircularProgressIndicator()
                WidgetState.Failure -> WordsText("Words file not found")
                is WidgetState.Success -> WordList(state.words)
            }
        }
        Button(
            modifier = GlanceModifier.fillMaxWidth(),
            text = "Update",
            onClick = actionRunCallback<UpdateWidget>()
        )
    }
}

@Composable
private fun loadWidgetState(context: Context): State<WidgetState> {
    return produceState<WidgetState>(initialValue = WidgetState.InProgress) {
        withContext(Dispatchers.IO) {
            value = CachingWordsRepository.getWords(context)?.shuffled()
                ?.let(WidgetState::Success)
                ?: WidgetState.Failure
        }
    }
}

@Composable
private fun WordList(words: List<Pair<String, String>>, modifier: GlanceModifier = GlanceModifier) {
    LazyColumn(modifier) {
        items(words) { (englishWord, polishWord) ->
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(imageProvider = ImageProvider(R.drawable.rounded_background), colorFilter = ColorFilter.tint(GlanceTheme.colors.surface))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val rowModifier = GlanceModifier.defaultWeight()
                    // Without setting padding and background glance puts them on some items (bug)
                    .padding(0.dp)
                    .background(Color.Transparent)
                val style = TextDefaults.defaultTextStyle.copy(color = GlanceTheme.colors.onSurface)
                WordsText(text = englishWord, rowModifier, style)
                WordsText(text = polishWord, rowModifier, style)
            }
        }
    }
}

private sealed interface WidgetState {
    data object InProgress : WidgetState
    data object Failure : WidgetState
    class Success(val words: List<Pair<String, String>>) : WidgetState
}

@Composable
fun WordsText(text: String, modifier: GlanceModifier = GlanceModifier, style: TextStyle = TextDefaults.defaultTextStyle, maxLines: Int = Int.MAX_VALUE) {
    Text(text, modifier, style = style.copy(color = GlanceTheme.colors.onBackground), maxLines)
}

class UpdateWidget : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WordsWidget().update(context, glanceId)
    }
}
