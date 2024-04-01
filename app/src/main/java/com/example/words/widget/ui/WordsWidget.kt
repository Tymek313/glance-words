package com.example.words.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextDefaults
import androidx.glance.text.TextStyle
import com.example.glancewords.R
import com.example.words.widget.WidgetState

private val defaultTextStyle
    @Composable
    get() = TextDefaults.defaultTextStyle.copy(fontSize = TextUnit(18f, TextUnitType.Sp), color = GlanceTheme.colors.onBackground)

@Composable
fun WordsWidgetContent(widgetState: WidgetState, onWidgetClick: () -> Unit) {
    GlanceTheme {
        Box(
            contentAlignment = Alignment.Center,
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.widgetBackground)
                .padding(6.dp)
        ) {
            when (widgetState) {
                WidgetState.InProgress -> CircularProgressIndicator()
                WidgetState.Failure -> Error(onReload)
                is WidgetState.Success -> WordList(words = widgetState.words, onItemClick = onWidgetClick)
            }
        }
    }
}

@Composable
private fun Error(onReload: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        WordsText(LocalContext.current.getString(R.string.could_not_load_words))
        Button(text = "Reload", onClick = onReload, modifier = GlanceModifier.padding(top = 16.dp))
    }
}

@Composable
private fun WordList(words: List<Pair<String, String>>, modifier: GlanceModifier = GlanceModifier, onItemClick: () -> Unit) {
    LazyColumn(modifier) {
        items(words) { (englishWord, polishWord) ->
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(R.drawable.no_ripple, onItemClick)
                    .background(imageProvider = ImageProvider(R.drawable.rounded_background), colorFilter = ColorFilter.tint(GlanceTheme.colors.surface))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val rowModifier = GlanceModifier.defaultWeight()
                    // Without setting padding and background glance puts them on some items (bug)
                    .padding(0.dp)
                    .background(Color.Transparent)
                val style = defaultTextStyle.copy(color = GlanceTheme.colors.onSurface)
                WordsText(text = englishWord, rowModifier, style)
                Spacer(GlanceModifier.width(4.dp))
                WordsText(text = polishWord, rowModifier, style)
            }
        }
    }
}

@Composable
fun WordsText(text: String, modifier: GlanceModifier = GlanceModifier, style: TextStyle = defaultTextStyle, maxLines: Int = Int.MAX_VALUE) {
    Text(text, modifier, style, maxLines)
}