package com.example.words.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class WordsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = WordsGlanceWidget()
}