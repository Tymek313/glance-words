package com.pt.glancewords.data.datasource

import com.pt.glancewords.domain.model.WidgetId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okio.BufferedSource
import okio.FileSystem
import okio.Path

internal interface WordsLocalDataSource {
    suspend fun getWords(widgetId: WidgetId): List<CSVLine>?
    suspend fun storeWords(widgetId: WidgetId, words: List<CSVLine>)
    suspend fun deleteWords(widgetId: WidgetId)
}

internal class FileWordsLocalDataSource(
    private val fileSystem: FileSystem,
    private val spreadsheetsDirectory: Path,
    private val ioDispatcher: CoroutineDispatcher
) : WordsLocalDataSource {

    override suspend fun getWords(widgetId: WidgetId) = withContext(ioDispatcher) {
        getFilePath(widgetId)
            .takeIf(fileSystem::exists)
            ?.let { path -> fileSystem.read(path, BufferedSource::readUtf8).split("\r\n").map(::CSVLine) }
    }

    override suspend fun storeWords(widgetId: WidgetId, words: List<CSVLine>) {
        withContext(ioDispatcher) {
            val targetPath = getFilePath(widgetId)
            val parentDirectory = checkNotNull(targetPath.parent) // Should not be null since we're requiring non-null `spreadsheetsDirectory`
            if (!fileSystem.exists(parentDirectory)) {
                fileSystem.createDirectories(parentDirectory)
            }
            fileSystem.write(targetPath) { writeUtf8(words.joinToString(separator = "\r\n", transform = CSVLine::value)) }
        }
    }

    override suspend fun deleteWords(widgetId: WidgetId) {
        fileSystem.delete(getFilePath(widgetId))
    }

    private fun getFilePath(widgetId: WidgetId) = spreadsheetsDirectory / "${widgetId.value}.csv"
}