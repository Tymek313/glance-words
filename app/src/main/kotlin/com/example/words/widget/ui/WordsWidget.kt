package com.example.words.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
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
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import com.example.domain.model.WordPair
import com.example.glancewords.R
import com.example.words.widget.WidgetUiState
import com.example.words.widget.ui.actions.LaunchWidgetSynchronizationWorkAction
import com.example.words.widget.ui.actions.ReshuffleAction
import com.example.words.widget.ui.components.WidgetText
import com.example.words.widget.ui.components.defaultTextStyle
import com.example.words.widget.ui.components.smallBoldTextStyle
import com.example.words.widget.ui.components.smallTextStyle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun WordsWidgetContent(uiState: WidgetUiState) {
    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.widgetBackground)
                .padding(top = 6.dp, start = 6.dp, end = 6.dp, bottom = 2.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                if(uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    WordList(words = uiState.words)
                }
            }
            Footer(uiState)
        }
    }
}

@Composable
private fun WordList(words: List<WordPair>, modifier: GlanceModifier = GlanceModifier) {
    LazyColumn(modifier) {
        items(words) { (originalWord, translatedWord) ->
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionRunCallback<ReshuffleAction>(), R.drawable.no_ripple)
                    .background(imageProvider = ImageProvider(R.drawable.rounded_background), colorFilter = ColorFilter.tint(GlanceTheme.colors.surface))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val rowModifier = GlanceModifier.defaultWeight()
                val style = defaultTextStyle.copy(color = GlanceTheme.colors.onSurface)
                WidgetText(text = originalWord, rowModifier, style)
                Spacer(GlanceModifier.width(4.dp))
                WidgetText(text = translatedWord, rowModifier, style)
            }
        }
    }
}

@Composable
private fun Footer(widgetUiState: WidgetUiState) {
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
                text = widgetUiState.lastUpdatedAt,
                style = smallTextStyle,
                maxLines = 1,
                modifier = GlanceModifier.run {
                    val vertical = if (isWidgetLarge) 8.dp else 4.dp
                    padding(start = 2.dp, top = vertical, bottom = vertical)
                }
            )
        }
        Box(modifier, contentAlignment = Alignment.CenterEnd) { WidgetText(text = widgetUiState.sheetName, style = smallBoldTextStyle) }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Composable
@Preview(400, 200)
private fun WordsWidgetContentSuccessPreview() {
    WordsWidgetContent(
        uiState = WidgetUiState(
            sheetName = "Sheet name",
            lastUpdatedAt = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
                .withZone(ZoneId.systemDefault())
                .format(Instant.now()),
            words = listOf(
                WordPair(original = "Original item 1", translated = "Translated item 1"),
                WordPair(original = "Original item 2", translated = "Translated item 2"),
                WordPair(original = "Very very long original item 3", translated = "Very very long ranslated item 3"),
            )
        )
    )
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Composable
@Preview(400, 200)
private fun WordsWidgetContentLoadingPreview() {
    WordsWidgetContent(
        uiState = WidgetUiState(
            sheetName = "Sheet name",
            lastUpdatedAt = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
                .withZone(ZoneId.systemDefault())
                .format(Instant.now()),
            isLoading = true
        )
    )
}