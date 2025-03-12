package com.pt.glancewords.widget.ui

import android.os.Build
import androidx.annotation.DimenRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.cornerRadius
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
import com.pt.glancewords.R
import com.pt.glancewords.domain.model.WordPair
import com.pt.glancewords.widget.WidgetUiState
import com.pt.glancewords.widget.ui.actions.LaunchWidgetSynchronizationWorkAction
import com.pt.glancewords.widget.ui.actions.ReshuffleAction
import com.pt.glancewords.widget.ui.components.WidgetText
import com.pt.glancewords.widget.ui.components.bodyLargeTextStyle
import com.pt.glancewords.widget.ui.components.labelSmallBoldTextStyle
import com.pt.glancewords.widget.ui.components.labelSmallTextStyle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun WordsWidgetContent(uiState: WidgetUiState) {
    GlanceTheme {
        Scaffold(
            horizontalPadding = glanceDimensionResource(R.dimen.widget_container_padding),
            modifier = GlanceModifier.padding(
                top = glanceDimensionResource(R.dimen.widget_container_padding)
            )
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Box(contentAlignment = Alignment.Center, modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator()
                    } else {
                        WordList(words = uiState.words)
                    }
                }
                Footer(uiState)
            }
        }
    }
}

@Composable
private fun WordList(words: List<WordPair>, modifier: GlanceModifier = GlanceModifier) {
    LazyColumn(modifier.listCornerRadius()) {
        items(words) { (originalWord, translatedWord) ->
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionRunCallback<ReshuffleAction>(), R.drawable.widget_background_no_ripple)
                    .background(
                        imageProvider = ImageProvider(R.drawable.widget_background_list_item),
                        // Color filter is required. Attribute inside the drawable is not applied for some reason.
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.secondaryContainer)
                    )
                    .padding(
                        top = R.dimen.widget_list_item_padding_top,
                        start = R.dimen.widget_list_item_padding_horizontal,
                        end = R.dimen.widget_list_item_padding_horizontal,
                        bottom = R.dimen.widget_list_item_padding_bottom
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val rowModifier = GlanceModifier.defaultWeight()
                val style = bodyLargeTextStyle.copy(color = GlanceTheme.colors.onSecondaryContainer)
                WidgetText(text = originalWord, rowModifier, style)
                Spacer(GlanceModifier.width(R.dimen.widget_list_item_margin))
                WidgetText(text = translatedWord, rowModifier, style)
            }
        }
    }
}

@Composable
private fun Footer(widgetUiState: WidgetUiState) {
    val lastUpdatedAt = remember(widgetUiState.lastUpdatedAt) {
        widgetUiState.lastUpdatedAt?.let { lastUpdatedAt ->
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(Locale.getDefault())
                .withZone(ZoneId.systemDefault())
                .format(lastUpdatedAt)
        }.orEmpty()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier.fillMaxWidth().padding(R.dimen.widget_footer_padding)
    ) {
        Row(
            horizontalAlignment = Alignment.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.defaultWeight().clickable(actionRunCallback<LaunchWidgetSynchronizationWorkAction>())
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = null,
                modifier = GlanceModifier.size(R.dimen.widget_refresh_icon_size),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface)
            )
            WidgetText(
                text = lastUpdatedAt,
                style = labelSmallTextStyle.copy(color = GlanceTheme.colors.onSurface),
                maxLines = 1,
                modifier = GlanceModifier.padding(start = R.dimen.widget_refresh_icon_margin)
            )
        }
        Box(contentAlignment = Alignment.CenterEnd) {
            WidgetText(
                text = widgetUiState.sheetName,
                style = labelSmallBoldTextStyle.copy(color = GlanceTheme.colors.onSurface)
            )
        }
    }
}

@Composable
private fun GlanceModifier.listCornerRadius(): GlanceModifier {
    if (Build.VERSION.SDK_INT < 31) {
        return this
    }

    return this.cornerRadius(glanceDimensionResource(android.R.dimen.system_app_widget_inner_radius))
}

@Composable
private fun glanceDimensionResource(@DimenRes resId: Int): Dp {
    val resources = LocalContext.current.resources
    val dimensionInPixels = resources.getDimension(resId)
    return Dp(dimensionInPixels / resources.displayMetrics.density)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Composable
@Preview(400, 200)
private fun WordsWidgetContentSuccessPreview() {
    WordsWidgetContent(
        uiState = WidgetUiState(
            sheetName = "Sheet name",
            lastUpdatedAt = Instant.now(),
            words = listOf(
                WordPair(original = "Original item 1", translated = "Translated item 1"),
                WordPair(original = "Original item 2", translated = "Translated item 2"),
                WordPair(original = "Very very long original item 3", translated = "Very very long ranslated item 3")
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
            lastUpdatedAt = Instant.now(),
            isLoading = true
        )
    )
}
