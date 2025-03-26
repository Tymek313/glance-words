package com.pt.glancewords.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.pt.glancewords.domain.widget.model.WidgetId

suspend fun refreshWidget(context: Context, widgetId: WidgetId) {
    WordsGlanceWidget().update(context, GlanceAppWidgetManager(context).getGlanceIdBy(widgetId.value))
}
