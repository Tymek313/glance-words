package com.example.words.datasource

import com.example.words.model.SheetSpreadsheetId
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

interface WordsRemoteDataSource {
    suspend fun getWords(sheetSpreadsheetId: SheetSpreadsheetId): List<CSVLine>
}

class GoogleWordsRemoteDataSource(private val client: HttpClient): WordsRemoteDataSource {
    override suspend fun getWords(sheetSpreadsheetId: SheetSpreadsheetId): List<CSVLine> {
        val (spreadsheetId, sheetId) = sheetSpreadsheetId
        return client.get("https://docs.google.com/spreadsheets/d/$spreadsheetId/export?format=csv&gid=$sheetId")
            .bodyAsText()
            .takeIf { it.isNotBlank() }
            ?.split("\r\n")
            ?.removedEmptyTrailingValues()
            ?.map(::CSVLine)
            ?: emptyList()
    }

    private fun List<String>.removedEmptyTrailingValues(): List<String> {
        val mutableList = toMutableList()
        val iterator = mutableList.listIterator(size)
        while (iterator.hasPrevious()) {
            val line = iterator.previous()
            if (line.contains("#VALUE!")) {
                iterator.remove()
            } else {
                break
            }
        }
        return mutableList
    }
}