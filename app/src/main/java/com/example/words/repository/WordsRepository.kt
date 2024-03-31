package com.example.words.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.api.services.sheets.v4.Sheets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

private const val FILENAME = "words.csv"

class WordsRepository(private val sheets: Sheets) {

    suspend fun load100RandomFromRemote(file: InputStream): List<Pair<String, String>>? = withContext(Dispatchers.IO) {
        val valueRange = try {
            sheets.spreadsheets().values().get("1-OKOwZKU7X_zs9Wr34dzd4ns2BKuIQBJUGx3m_0kspA", "B2!A:B").execute()
        } catch (e: IOException) {
            return@withContext null
        }
//        val valueRange = client.spreadsheets().getByDataFilter(
//            "1-OKOwZKU7X_zs9Wr34dzd4ns2BKuIQBJUGx3m_0kspA",
//            GetSpreadsheetByDataFilterRequest().apply {
//                includeGridData = true
//                dataFilters = listOf(
//                    DataFilter().apply {
//                        gridRange = GridRange().apply {
//                            sheetId = 1640466707
//                            startRowIndex = 0
//                            startColumnIndex = 0
//                            endColumnIndex = 1
//                            endRowIndex = 5
//                        }
//                    }
//                )
//            }
//        ).execute()
        println(valueRange)
        listOf("1" to "1")
        valueRange.getValues()
            .shuffled()
            .filter { values -> values.none { it == "#VALUE!" } }
            .take(100)
            .map { it.first().toString() to it[1].toString() }
    }

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