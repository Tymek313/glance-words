package com.example.words.repository

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

object SheetsProvider {
    lateinit var sheets: Sheets
        private set

    suspend fun initialize(credentialsFileStreamProvider: (filename: String) -> InputStream ) = withContext(Dispatchers.IO) {
        val credentials = credentialsFileStreamProvider("credentials.json").use { stream -> HttpCredentialsAdapter(GoogleCredentials.fromStream(stream)) }
        sheets = Sheets.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credentials).build()
    }
}