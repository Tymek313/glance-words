package com.pt.glancewords.data.datasource

import com.pt.glancewords.domain.model.SheetSpreadsheetId
import com.pt.glancewords.logging.Logger
import com.pt.glancewords.logging.e
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.io.IOException

internal interface WordsRemoteDataSource {
    suspend fun getWords(sheetSpreadsheetId: SheetSpreadsheetId): List<CSVLine>?
}

internal class GoogleWordsRemoteDataSource(private val client: HttpClient, private val logger: Logger) : WordsRemoteDataSource {

    override suspend fun getWords(sheetSpreadsheetId: SheetSpreadsheetId): List<CSVLine>? {
        val (spreadsheetId, sheetId) = sheetSpreadsheetId
        return try {
            client.get("https://docs.google.com/spreadsheets/d/$spreadsheetId/export?format=csv&gid=$sheetId")
                .bodyAsText()
                .takeIf { it.isNotBlank() }
                ?.split("\r\n")
                ?.removedEmptyTrailingValues()
                ?.map(::CSVLine)
                ?: emptyList()
        } catch (e: IOException) {
            logger.e(this, e)
            null
        }
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
