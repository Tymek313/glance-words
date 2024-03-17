package com.example.glancewords.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException

private const val FILENAME = "words.csv"

object WordsRepository {

    suspend fun getWords(context: Context): List<Pair<String, String>>? = withContext(Dispatchers.IO) {
        if (File(context.filesDir, "words.csv").exists()) {
            context.openFileInput("words.csv").bufferedReader().use { reader ->
                reader.useLines { lines ->
                    lines.map(::csvLineToLanguagePair).filterHashValues().toList()
                }
            }
        } else {
            null
        }
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