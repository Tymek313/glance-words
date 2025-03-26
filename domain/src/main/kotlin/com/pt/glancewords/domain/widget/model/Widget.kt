package com.pt.glancewords.domain.widget.model

import com.pt.glancewords.domain.sheet.model.Sheet

data class Widget(
    val id: WidgetId,
    val sheet: Sheet
)

@JvmInline
value class WidgetId(val value: Int)
