package com.example.domain.model

data class Widget(
    val id: WidgetId,
    val sheet: Sheet
) {
    @JvmInline
    value class WidgetId(val value: Int)
}