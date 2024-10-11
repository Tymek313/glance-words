package com.example.words.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
import androidx.glance.appwidget.action.actionRunCallback
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
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import com.example.glancewords.R
import com.example.words.model.WordPair
import com.example.words.widget.WidgetDetailsState
import com.example.words.widget.WidgetWordsState
import com.example.words.widget.ui.components.WidgetText
import com.example.words.widget.ui.components.defaultTextStyle
import com.example.words.widget.ui.components.smallBoldTextStyle
import com.example.words.widget.ui.components.smallTextStyle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun WordsWidgetContent(widgetWordsState: WidgetWordsState, widgetDetailsState: WidgetDetailsState, onReload: () -> Unit) {
    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.widgetBackground)
                .padding(top = 6.dp, start = 6.dp, end = 6.dp, bottom = 2.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                when (widgetWordsState) {
                    WidgetWordsState.Loading -> CircularProgressIndicator()
                    WidgetWordsState.Failure -> Error(onReload)
                    is WidgetWordsState.Success -> WordList(words = widgetWordsState.words, onItemClick = onReload)
                }
            }
            Footer(widgetDetailsState)
        }
    }
}

@Composable
private fun Footer(widgetDetailsState: WidgetDetailsState) {
    val isWidgetLarge = LocalSize.current == WordsWidgetSizes.LARGE
    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        val modifier = GlanceModifier.defaultWeight()
        Row(
            horizontalAlignment = Alignment.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.clickable(actionRunCallback<LaunchWidgetSynchronizationWorkAction>())
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = null,
                modifier = GlanceModifier.size(16.dp),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onBackground)
            )
            WidgetText(
                text = widgetDetailsState.lastUpdatedAt,
                style = smallTextStyle,
                maxLines = 1,
                modifier = GlanceModifier.run {
                    val vertical = if (isWidgetLarge) 8.dp else 4.dp
                    padding(start = 2.dp, top = vertical, bottom = vertical)
                }
            )
        }
        Box(modifier, contentAlignment = Alignment.CenterEnd) { WidgetText(text = widgetDetailsState.sheetName, style = smallBoldTextStyle) }
    }
}

@Composable
private fun Error(onReload: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        WidgetText(LocalContext.current.getString(R.string.could_not_load_words))
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
                WidgetText(text = englishWord, rowModifier, style)
                Spacer(GlanceModifier.width(4.dp))
                WidgetText(text = polishWord, rowModifier, style)
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Composable
@Preview(400, 200)
private fun WordsWidgetContentSuccessPreview() {
    WordsWidgetContent(
        widgetWordsState = WidgetWordsState.Success(
            words = listOf(
                WordPair(original = "Original item 1", translated = "Translated item 1"),
                WordPair(original = "Original item 2", translated = "Translated item 2"),
                WordPair(original = "Original item 3", translated = "Translated item 3"),
            )
        ),
        widgetDetailsState = WidgetDetailsState(
            sheetName = "Sheet name",
            lastUpdatedAt = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
                .withZone(ZoneId.systemDefault())
                .format(Instant.now())
        ),
        onReload = {}
    )
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Composable
@Preview(400, 200)
private fun WordsWidgetContentLoadingPreview() {
    WordsWidgetContent(
        widgetWordsState = WidgetWordsState.Loading,
        widgetDetailsState = WidgetDetailsState(
            sheetName = "Sheet name",
            lastUpdatedAt = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
                .withZone(ZoneId.systemDefault())
                .format(Instant.now())
        ),
        onReload = {}
    )
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Composable
@Preview(400, 200)
private fun WordsWidgetContentFailurePreview() {
    WordsWidgetContent(
        widgetWordsState = WidgetWordsState.Failure,
        widgetDetailsState = WidgetDetailsState(
            sheetName = "Sheet name",
            lastUpdatedAt = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
                .withZone(ZoneId.systemDefault())
                .format(Instant.now())
        ),
        onReload = {}
    )
}