package com.example.domain.fixture

import com.example.domain.model.Sheet
import com.example.domain.model.SheetSpreadsheetId
import com.example.domain.model.Widget
import com.example.testcommon.fixture.randomInstant
import com.example.testcommon.fixture.randomInt
import com.example.testcommon.fixture.randomString

val instantFixture = randomInstant()
val widgetIdFixture = randomWidgetId()
val existingSheetFixture = Sheet.createExisting(
    id = randomSheetId(),
    sheetSpreadsheetId = SheetSpreadsheetId(randomString(), randomInt()),
    name = randomString(),
    lastUpdatedAt = instantFixture
)
val widgetWithExistingSheetFixture = Widget(
    id = randomWidgetId(),
    sheet = existingSheetFixture
)
