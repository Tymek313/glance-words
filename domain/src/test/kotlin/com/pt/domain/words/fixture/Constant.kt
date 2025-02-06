package com.pt.domain.words.fixture

import com.pt.glancewords.domain.model.Sheet
import com.pt.glancewords.domain.model.SheetSpreadsheetId
import com.pt.glancewords.domain.model.Widget
import com.pt.testcommon.fixture.randomInstant
import com.pt.testcommon.fixture.randomInt
import com.pt.testcommon.fixture.randomString

val instantFixture = randomInstant()
val widgetIdFixture = randomWidgetId()
val existingSheetFixture = Sheet(
    id = randomSheetId(),
    sheetSpreadsheetId = SheetSpreadsheetId(randomString(), randomInt()),
    name = randomString(),
    lastUpdatedAt = instantFixture
)
val widgetWithExistingSheetFixture = Widget(
    id = randomWidgetId(),
    sheet = existingSheetFixture
)
