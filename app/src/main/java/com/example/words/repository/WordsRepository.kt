package com.example.words.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

interface WordsRepository {
    fun observeRandomWords(spreadsheetId: String, sheetId: Int): Flow<List<Pair<String, String>>?>

    suspend fun synchronizeWords(spreadsheetId: String, sheetId: Int)
}

class DefaultWordsRepository(private val spreadsheetsDirectory: File) : WordsRepository {

    private val newlySynchronizedWords = Channel<List<Pair<String, String>>>()

    override fun observeRandomWords(spreadsheetId: String, sheetId: Int): Flow<List<Pair<String, String>>?> = flow {
        val cachedFile = getTargetFile(spreadsheetId, sheetId)
        if (cachedFile.exists()) {
            emit(linesToShuffledPairs(cachedFile.readLines()))
        }
        for (words in newlySynchronizedWords) {
            emit(words)
        }
    }

    override suspend fun synchronizeWords(spreadsheetId: String, sheetId: Int) {
        withContext(Dispatchers.Default) {
            newlySynchronizedWords.send(
                linesToShuffledPairs(
                    downloadCachingWordsSpreadsheet(getTargetFile(spreadsheetId, sheetId), spreadsheetId, sheetId)
                )
            )
        }
    }

    private fun getTargetFile(spreadsheetId: String, sheetId: Int) =
        File(spreadsheetsDirectory, "spreadsheets${File.separatorChar}${spreadsheetId}_$sheetId.csv")

    private fun linesToShuffledPairs(lines: List<String>): List<Pair<String, String>> = lines.map(::csvLineToLanguagePair).shuffled().take(50)

    private suspend fun downloadCachingWordsSpreadsheet(targetFile: File, spreadsheetId: String, sheetId: Int): List<String> = withContext(Dispatchers.IO) {
        val sourceCsv = URL("https://docs.google.com/spreadsheets/d/$spreadsheetId/export?format=csv&gid=$sheetId")
            .openStream()
            .bufferedReader()
            .use { it.readLines() }
            .toMutableList()
            .apply { removeEmptyTrailingValues() }

        targetFile.apply {
            if (!exists()) {
                parentFile?.mkdirs()
                createNewFile()
            }
        }

        FileOutputStream(targetFile)
            .bufferedWriter()
            .use { targetCsvStream -> sourceCsv.forEach(targetCsvStream::appendLine) }

        sourceCsv
    }

    private fun MutableList<String>.removeEmptyTrailingValues() {
        val iterator = listIterator(size)
        while (iterator.hasPrevious()) {
            val line = iterator.previous()
            if (line.contains("#VALUE!")) {
                iterator.remove()
            } else {
                break
            }
        }
    }

    private fun csvLineToLanguagePair(line: String): Pair<String, String> = line
        .split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*\$)".toRegex())
        .run { get(0) to getOrNull(1).orEmpty() }
}