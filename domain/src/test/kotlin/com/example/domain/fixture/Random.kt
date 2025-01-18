package com.example.domain.fixture

import com.example.domain.model.SheetId
import com.example.domain.model.Widget
import com.example.testcommon.fixture.randomInt

fun randomWidgetId() = Widget.WidgetId(randomInt())

fun randomSheetId() = SheetId(randomInt())