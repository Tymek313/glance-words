package com.example.words.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import com.example.words.widget.ui.WordsWidgetContent

class WordsGlanceWidget: GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WordsWidgetContent()
        }
    }
}
/*
1. Read remote spreadsheet, get all entries for two columns shuffle 100 entries and display them.
2. Add new entry on button click
 */