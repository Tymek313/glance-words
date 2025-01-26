package com.pt.glancewords.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.pt.glancewords.domain.model.Widget

suspend fun refreshWidget(context: Context, widgetId: Widget.WidgetId) {
    WordsGlanceWidget().update(context, GlanceAppWidgetManager(context).getGlanceIdBy(widgetId.value))
}