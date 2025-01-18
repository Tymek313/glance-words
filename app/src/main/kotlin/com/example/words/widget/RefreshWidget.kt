package com.example.words.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.example.domain.model.Widget

suspend fun refreshWidget(context: Context, widgetId: Widget.WidgetId) {
    WordsGlanceWidget().update(context, GlanceAppWidgetManager(context).getGlanceIdBy(widgetId.value))
}