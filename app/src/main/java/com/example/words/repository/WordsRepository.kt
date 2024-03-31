package com.example.words.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.DataFilter
import com.google.api.services.sheets.v4.model.GetSpreadsheetByDataFilterRequest
import com.google.api.services.sheets.v4.model.GridRange
import com.google.api.services.sheets.v4.model.Spreadsheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException

private const val FILENAME = "words.csv"

class WordsRepository(private val sheets: Sheets) {

    suspend fun load100RandomFromRemote(spreadsheetId: String, sheetId: Int): List<Pair<String, String>>? = withContext(Dispatchers.IO) {
        val spreadsheet = try {
            loadSpreadsheet(spreadsheetId, sheetId)
        } catch (e: IOException) {
            return@withContext null
        }
        println(spreadsheet)
        spreadsheet?.sheets?.firstOrNull()?.data?.firstOrNull()?.rowData
            ?.mapNotNull { row ->
                val firstValue = row.getValues()?.get(0)?.effectiveValue?.stringValue
                val secondValue = row.getValues()?.get(1)?.effectiveValue?.stringValue
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

    private fun loadSpreadsheet(spreadsheetId: String, sheetId: Int): Spreadsheet? = sheets.spreadsheets().getByDataFilter(
        spreadsheetId,
        GetSpreadsheetByDataFilterRequest().apply {
            includeGridData = true
            dataFilters = listOf(
                DataFilter().apply {
                    gridRange = GridRange().apply {
                        this.sheetId = sheetId
                        startColumnIndex = 0
                        endColumnIndex = 2
                    }
                }
            )
        }
    ).execute()

    private fun csvLineToLanguagePair(line: String): Pair<String, String> = line
        .split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*\$)".toRegex())
        .run { get(0) to getOrNull(1).orEmpty() }

    private fun Sequence<Pair<String, String>>.filterHashValues() = filterNot { it.first == "#VALUE!" || it.second == "#VALUE!" }

    suspend fun copyToLocalFile(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.openFileOutput(FILENAME, Context.MODE_PRIVATE).use { targetFile ->
                context.contentResolver.openInputStream(uri)?.use { sourceFile ->
                    sourceFile.copyTo(targetFile)
                }
            }
            true
        } catch (e: FileNotFoundException) {
            Log.e(javaClass.name, "Could copy a file", e)
            false
        }
    }
}