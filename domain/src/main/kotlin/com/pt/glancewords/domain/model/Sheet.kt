package com.pt.glancewords.domain.model

import java.time.Instant

data class Sheet(
    val id: SheetId,
    val sheetSpreadsheetId: SheetSpreadsheetId,
    val name: String,
    val lastUpdatedAt: Instant?
)

@JvmInline
value class SheetId(val value: Int)

data class NewSheet(
    val sheetSpreadsheetId: SheetSpreadsheetId,
    val name: String
)

data class SheetSpreadsheetId(val spreadsheetId: String, val sheetId: Int)
