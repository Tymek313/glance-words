package com.example.words.model

import java.time.Instant

data class Widget(
    val id: WidgetId,
    val spreadsheetId: String,
    val sheetId: Int,
    val sheetName: String,
    val lastUpdatedAt: Instant?
) {
    @JvmInline
    value class WidgetId(val value: Int)
}