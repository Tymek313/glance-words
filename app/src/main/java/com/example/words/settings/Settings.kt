package com.example.words.settings

import java.time.Instant

data class WidgetSettings(
    val widgetId: WidgetId,
    val spreadsheetId: String,
    val sheetId: Int,
    val sheetName: String,
    val lastUpdatedAt: Instant?
) {
    @JvmInline
    value class WidgetId(val value: Int)
}