package com.pt.glancewords.domain.fixture

import com.pt.glancewords.domain.sheet.model.SheetId
import com.pt.glancewords.domain.widget.model.WidgetId
import com.pt.glancewords.testcommon.fixture.randomInt

fun randomWidgetId() = WidgetId(randomInt())

fun randomSheetId() = SheetId(randomInt())