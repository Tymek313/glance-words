package com.example.domain.model

import java.time.Instant

class Sheet private constructor(
    val id: SheetId,
    val sheetSpreadsheetId: SheetSpreadsheetId,
    val name: String,
    val lastUpdatedAt: Instant?
) {
    override fun toString() = "Sheet(id=$id, sheetSpreadsheet=$sheetSpreadsheetId, name=$name, lastUpdatedAt=$lastUpdatedAt)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Sheet

        if (id != other.id) return false
        if (sheetSpreadsheetId != other.sheetSpreadsheetId) return false
        if (name != other.name) return false
        if (lastUpdatedAt != other.lastUpdatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + sheetSpreadsheetId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (lastUpdatedAt?.hashCode() ?: 0)
        return result
    }

    fun copy(
        id: SheetId = this.id,
        sheetSpreadsheetId: SheetSpreadsheetId = this.sheetSpreadsheetId,
        name: String = this.name,
        lastUpdatedAt: Instant? = this.lastUpdatedAt
    ) = Sheet(id, sheetSpreadsheetId, name, lastUpdatedAt)

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
