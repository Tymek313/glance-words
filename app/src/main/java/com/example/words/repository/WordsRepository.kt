package com.example.words.repository

import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.DataFilter
import com.google.api.services.sheets.v4.model.GetSpreadsheetByDataFilterRequest
import com.google.api.services.sheets.v4.model.GridRange
import com.google.api.services.sheets.v4.model.Spreadsheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class WordsRepository(private val sheets: Sheets) {

    suspend fun load100RandomFromRemote(spreadsheetId: String, sheetId: Int): List<Pair<String, String>>? = withContext(Dispatchers.IO) {
        val spreadsheet = try {
            loadSpreadsheet(spreadsheetId, sheetId)
        } catch (e: IOException) {
            return@withContext null
        }
        spreadsheet?.sheets?.firstOrNull()?.data?.firstOrNull()?.rowData
            ?.mapNotNull { row ->
                val firstValue = row.getValues()?.getOrNull(0)?.effectiveValue?.stringValue
                val secondValue = row.getValues()?.getOrNull(1)?.effectiveValue?.stringValue
                if(firstValue != null && secondValue != null) {
                    firstValue to secondValue
                } else {
                    null
                }
            }
            ?.filter { it.first != "#VALUE!" && it.second != "#VALUE!" }
            ?.shuffled()
            ?.take(100)
    }

    private fun loadSpreadsheet(spreadsheetId: String, sheetId: Int): Spreadsheet? {
        val filters = listOf(
            DataFilter().apply {
                gridRange = GridRange().apply {
                    this.sheetId = sheetId
                    startColumnIndex = 0
                    endColumnIndex = 2
                }
            }
        )
        return sheets.spreadsheets().getByDataFilter(
            spreadsheetId,
            GetSpreadsheetByDataFilterRequest().apply {
                includeGridData = true
                dataFilters = filters
            }
        ).execute()
    }
}