package com.pt.glancewords.data.datasource

import com.pt.glancewords.domain.model.SheetId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okio.BufferedSource
import okio.FileSystem
import okio.Path

internal interface WordsLocalDataSource {
    suspend fun getWords(sheetId: SheetId): List<CSVLine>?
    suspend fun storeWords(sheetId: SheetId, words: List<CSVLine>)
    suspend fun deleteWords(sheetId: SheetId)
}

internal class FileWordsLocalDataSource(
    private val fileSystem: FileSystem,
    private val spreadsheetsDirectory: Path,
    private val ioDispatcher: CoroutineDispatcher
) : WordsLocalDataSource {

    override suspend fun getWords(sheetId: SheetId) = withContext(ioDispatcher) {
        getFilePath(sheetId)
            .takeIf(fileSystem::exists)
            ?.let { path -> fileSystem.read(path, BufferedSource::readUtf8).split("\r\n").map(::CSVLine) }
    }

    override suspend fun storeWords(sheetId: SheetId, words: List<CSVLine>) {
        withContext(ioDispatcher) {
            val targetPath = getFilePath(sheetId)
            val parentDirectory = checkNotNull(targetPath.parent) // Should not be null since we're requiring non-null `spreadsheetsDirectory`
            if (!fileSystem.exists(parentDirectory)) {
                fileSystem.createDirectories(parentDirectory)
            }
            fileSystem.write(targetPath) { writeUtf8(words.joinToString(separator = "\r\n", transform = CSVLine::value)) }
        }
    }

    override suspend fun deleteWords(sheetId: SheetId) {
        fileSystem.delete(getFilePath(sheetId))
    }

    private fun getFilePath(sheetId: SheetId) = spreadsheetsDirectory / "${sheetId.value}.csv"
}