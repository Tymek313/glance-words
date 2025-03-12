package com.pt.glancewords.data.datasource

import com.google.api.services.sheets.v4.model.Sheet
import com.pt.glancewords.data.googlesheets.GoogleSheetsProvider
import com.pt.glancewords.logging.Logger
import com.pt.glancewords.logging.e
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal interface GoogleSpreadsheetDataSource {
    suspend fun getSpreadsheets(id: String): List<Sheet>?
}

internal class DefaultGoogleSpreadsheetDataSource(
    private val sheetsProvider: GoogleSheetsProvider,
    private val logger: Logger,
    private val ioDispatcher: CoroutineDispatcher
) : GoogleSpreadsheetDataSource {
    override suspend fun getSpreadsheets(id: String): List<Sheet>? = withContext(ioDispatcher) {
        try {
            sheetsProvider.getGoogleSheets().spreadsheets().get(id).execute()?.sheets ?: emptyList()
        } catch (e: IOException) {
            logger.e(this, e)
            null
        }
    }
}
