package com.example.words.model

import java.time.Instant

@Suppress("DataClassPrivateConstructor")
data class Sheet private constructor(
    val id: SheetId,
    val sheetSpreadsheetId: SheetSpreadsheetId,
    val name: String,
    val lastUpdatedAt: Instant?
) {

    companion object {
        fun createNew(sheetSpreadsheetId: SheetSpreadsheetId, name: String) = Sheet(SheetId.None, sheetSpreadsheetId, name, lastUpdatedAt = null)

        fun createExisting(id: SheetId, sheetSpreadsheetId: SheetSpreadsheetId, name: String, lastUpdatedAt: Instant?) =
            Sheet(id, sheetSpreadsheetId, name, lastUpdatedAt)
    }
}

@JvmInline
value class SheetId(val value: Int) {

    companion object {
        val None = SheetId(-1)
    }
}

data class SheetSpreadsheetId(val spreadsheetId: String, val sheetId: Int)
