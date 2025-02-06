package com.pt.domain.words.fixture

import com.pt.glancewords.domain.model.SheetId
import com.pt.glancewords.domain.model.WidgetId
import com.pt.testcommon.fixture.randomInt

fun randomWidgetId() = WidgetId(randomInt())

fun randomSheetId() = SheetId(randomInt())