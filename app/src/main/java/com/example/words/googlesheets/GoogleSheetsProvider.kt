package com.example.words.googlesheets

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.InputStream

interface GoogleSheetsProvider {
    suspend fun getGoogleSheets(): Sheets
}

class CachingGoogleSheetsProvider(
    private val ioDispatcher: CoroutineDispatcher,
    private val getCredentialsBytes: suspend () -> InputStream
) : GoogleSheetsProvider {
    private var sheets: Sheets? = null

    override suspend fun getGoogleSheets(): Sheets = sheets ?: withContext(ioDispatcher) {
        Sheets.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            HttpCredentialsAdapter(GoogleCredentials.fromStream(getCredentialsBytes()))
        ).build().also { sheets = it }
    }
}