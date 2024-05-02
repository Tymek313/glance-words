package com.example.words.repository

import com.example.words.model.Widget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

interface WordsRepository {
    fun observeRandomWords(widgetId: Widget.WidgetId): Flow<List<Pair<String, String>>?>
    suspend fun synchronizeWords(request: SynchronizationRequest)

    class SynchronizationRequest(val widgetId: Widget.WidgetId, val spreadsheetId: String, val sheetId: Int)
}

class DefaultWordsRepository(private val spreadsheetsDirectory: File) : WordsRepository {

    private val synchronizationUpdates = MutableSharedFlow<SpreadsheetUpdate>()

    override fun observeRandomWords(widgetId: Widget.WidgetId): Flow<List<Pair<String, String>>?> = flow {
        val cachedFile = getTargetFile(widgetId)
        if (cachedFile.exists()) {
            emit(linesToShuffledPairs(cachedFile.readLines()))
        }
        emitAll(synchronizationUpdates.filter { it.widgetId == widgetId }.map { it.words })
    }

    override suspend fun synchronizeWords(request: WordsRepository.SynchronizationRequest) {
        withContext(Dispatchers.Default) {
            synchronizationUpdates.emit(
                SpreadsheetUpdate(
                    widgetId = request.widgetId,
                    words = linesToShuffledPairs(
                        downloadCachingWordsSpreadsheet(getTargetFile(request.widgetId), request.spreadsheetId, request.sheetId)
                    )
                )
            )
        }
    }

    private fun getTargetFile(widgetId: Widget.WidgetId) = File(spreadsheetsDirectory, "spreadsheets${File.separatorChar}$widgetId.csv")

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

    private class SpreadsheetUpdate(val widgetId: Widget.WidgetId, val words: List<Pair<String, String>>)
}