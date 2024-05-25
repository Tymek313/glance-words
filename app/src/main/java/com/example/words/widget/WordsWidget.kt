package com.example.words.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDefaults
import androidx.glance.text.TextStyle
import com.example.glancewords.R
import com.example.words.model.WordPair

private val defaultTextStyle
    @Composable
    get() = TextDefaults.defaultTextStyle.copy(fontSize = TextUnit(18f, TextUnitType.Sp), color = GlanceTheme.colors.onBackground)

private val smallTextStyle
    @Composable
    get() = defaultTextStyle.copy(fontSize = TextUnit(13f, TextUnitType.Sp))

private val smallBoldTextStyle
    @Composable
    get() = smallTextStyle.copy(fontWeight = FontWeight.Bold)

@Composable
fun WordsWidgetContent(widgetState: WidgetState, widgetDetailsState: WidgetDetailsState, onReload: () -> Unit, onSynchronize: () -> Unit) {
    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.widgetBackground)
                .padding(top = 6.dp, start = 6.dp, end = 6.dp, bottom = 2.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                when (widgetState) {
                    WidgetState.InProgress -> CircularProgressIndicator()
                    WidgetState.Failure -> Error(onReload)
                    is WidgetState.Success -> WordList(words = widgetState.words, onItemClick = onReload)
                }
            }
            Footer(widgetDetailsState, onSynchronize)
        }
    }
}

@Composable
private fun Footer(widgetDetailsState: WidgetDetailsState, onSynchronize: () -> Unit) {
    val isWidgetLarge = LocalSize.current == WordsWidgetSizes.LARGE
    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        val modifier = GlanceModifier.defaultWeight()
        Row(horizontalAlignment = Alignment.Start, verticalAlignment = Alignment.CenterVertically, modifier = modifier.clickable(onSynchronize)) {
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = null,
                modifier = GlanceModifier.size(16.dp),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onBackground)
            )
            WordsText(
                text = widgetDetailsState.lastUpdatedAt,
                style = smallTextStyle,
                maxLines = 1,
                modifier = GlanceModifier.run {
                    val vertical = if (isWidgetLarge) 8.dp else 4.dp
                    padding(start = 2.dp, top = vertical, bottom = vertical)
                }
            )
        }
        Box(modifier, contentAlignment = Alignment.CenterEnd) { WordsText(text = widgetDetailsState.sheetName, style = smallBoldTextStyle) }
    }
}

@Composable
private fun Error(onReload: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        WordsText(LocalContext.current.getString(R.string.could_not_load_words))
        Spacer(GlanceModifier.height(16.dp))
        Button(text = "Reload", onClick = onReload)
    }
}

@Composable
private fun WordList(words: List<WordPair>, modifier: GlanceModifier = GlanceModifier, onItemClick: () -> Unit) {
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