package com.pt.glancewords.data.datasource

import com.pt.glancewords.data.CSVWordPairMapper
import com.pt.glancewords.domain.model.SheetSpreadsheetId
import com.pt.glancewords.domain.model.WordPair
import com.pt.glancewords.logging.Logger
import com.pt.glancewords.logging.e
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.io.IOException

internal interface WordsRemoteDataSource {
    suspend fun getWords(sheetSpreadsheetId: SheetSpreadsheetId): List<WordPair>?
}

internal class GoogleWordsRemoteDataSource(
    private val client: HttpClient,
    private val mapper: CSVWordPairMapper,
    private val logger: Logger
) : WordsRemoteDataSource {

    override suspend fun getWords(sheetSpreadsheetId: SheetSpreadsheetId): List<WordPair>? {
        val (spreadsheetId, sheetId) = sheetSpreadsheetId
        return try {
            client.get("https://docs.google.com/spreadsheets/d/$spreadsheetId/export?format=csv&gid=$sheetId")
                .bodyAsText()
                .let(mapper::map)
        } catch (e: IOException) {
            logger.e(this, e)
            null
        }
    }
}
