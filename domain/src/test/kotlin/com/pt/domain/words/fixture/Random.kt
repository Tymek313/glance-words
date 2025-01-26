package com.pt.domain.words.fixture

import com.pt.glancewords.domain.model.SheetId
import com.pt.glancewords.domain.model.Widget
import com.pt.testcommon.fixture.randomInt

fun randomWidgetId() = Widget.WidgetId(randomInt())

fun randomSheetId() = SheetId(randomInt())