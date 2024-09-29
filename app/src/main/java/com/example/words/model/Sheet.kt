package com.example.words.model

import java.time.Instant

data class Sheet(
    val id: SheetId,
    val sheetSpreadsheetId: SheetSpreadsheetId,
    val name: String,
    val lastUpdatedAt: Instant?
)

@JvmInline
value class SheetId(val value: Int) {

    companion object {
        val None = SheetId(-1)
    }
}

data class SheetSpreadsheetId(val spreadsheetId: String, val sheetId: Int)
