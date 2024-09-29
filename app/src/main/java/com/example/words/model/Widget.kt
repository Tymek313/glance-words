package com.example.words.model

data class Widget(
    val id: WidgetId,
    val sheet: Sheet
) {
    @JvmInline
    value class WidgetId(val value: Int)
}