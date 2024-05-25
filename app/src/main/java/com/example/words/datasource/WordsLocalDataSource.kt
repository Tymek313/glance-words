package com.example.words.datasource

import com.example.words.model.Widget
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okio.BufferedSource
import okio.FileSystem
import okio.Path

interface WordsLocalDataSource {
    suspend fun getWords(widgetId: Widget.WidgetId): List<CSVLine>?
    suspend fun storeWords(widgetId: Widget.WidgetId, words: List<CSVLine>)
}

class FileWordsLocalDataSource(
    private val fileSystem: FileSystem,
    private val spreadsheetsDirectory: Path,
    private val ioDispatcher: CoroutineDispatcher
) : WordsLocalDataSource {

    override suspend fun getWords(widgetId: Widget.WidgetId) = withContext(ioDispatcher) {
        getFilePath(widgetId)
            .takeIf(fileSystem::exists)
            ?.let { path -> fileSystem.read(path, BufferedSource::readUtf8).split("\r\n").map(::CSVLine) }
    }

    override suspend fun storeWords(widgetId: Widget.WidgetId, words: List<CSVLine>) {
        withContext(ioDispatcher) {
            val targetPath = getFilePath(widgetId)
            targetPath.parent?.let { parentDirectory ->
                if (!fileSystem.exists(parentDirectory)) {
                    fileSystem.createDirectories(parentDirectory)
                }
            }
            fileSystem.write(targetPath) { writeUtf8(words.joinToString(separator = "\r\n", transform = CSVLine::value)) }
        }
    }

    private fun getFilePath(widgetId: Widget.WidgetId) = spreadsheetsDirectory / "${widgetId.value}.csv"
}