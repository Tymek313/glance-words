package com.example.words.googlesheets

import android.content.res.Resources
import com.example.glancewords.R
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

interface GoogleSheetsProvider {
    suspend fun getGoogleSheets(): Sheets
}

class CachingGoogleSheetsProvider(
    private val resources: Resources,
    private val ioDispatcher: CoroutineDispatcher
) : GoogleSheetsProvider {
    private var sheets: Sheets? = null

    override suspend fun getGoogleSheets(): Sheets = sheets ?: withContext(ioDispatcher) {
        Sheets.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            HttpCredentialsAdapter(GoogleCredentials.fromStream(resources.openRawResource(R.raw.google_sheets_credentials)))
        ).build().also { sheets = it }
    }
}