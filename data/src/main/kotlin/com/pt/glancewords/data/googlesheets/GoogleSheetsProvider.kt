package com.pt.glancewords.data.googlesheets

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal interface GoogleSheetsProvider {
    suspend fun getGoogleSheets(): Sheets
}

internal class CachingGoogleSheetsProvider(
    private val ioDispatcher: CoroutineDispatcher
) : GoogleSheetsProvider {

    private val mutex = Mutex()
    private var sheets: Sheets? = null

    override suspend fun getGoogleSheets(): Sheets = mutex.withLock {
        sheets ?: withContext(ioDispatcher) {
            Sheets.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                HttpCredentialsAdapter(GoogleCredentials.fromStream(javaClass.classLoader.getResourceAsStream("google_sheets_credentials.json")))
            ).build().also { sheets = it }
        }
    }
}