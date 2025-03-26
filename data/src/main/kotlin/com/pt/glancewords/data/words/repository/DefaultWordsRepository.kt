package com.pt.glancewords.data.words.repository

import com.pt.glancewords.data.words.datasource.WordsLocalDataSource
import com.pt.glancewords.data.words.datasource.WordsRemoteDataSource
import com.pt.glancewords.domain.sheet.model.SheetId
import com.pt.glancewords.domain.sheet.model.SheetSpreadsheetId
import com.pt.glancewords.domain.words.model.WordPair
import com.pt.glancewords.domain.words.repository.WordsRepository
import kotlinx.coroutines.flow.Flow

internal class DefaultWordsRepository(
    private val remoteDataSource: WordsRemoteDataSource,
    private val localDataSource: WordsLocalDataSource
) : WordsRepository {

    override fun observeWords(sheetId: SheetId): Flow<List<WordPair>> = localDataSource.observeWords(sheetId)

    override suspend fun synchronizeWords(sheetId: SheetId, sheetSpreadsheetId: SheetSpreadsheetId): Boolean {
        val remoteWordPairs = remoteDataSource.getWords(sheetSpreadsheetId)

        return if (remoteWordPairs == null) {
            false
        } else {
            localDataSource.storeWords(sheetId, remoteWordPairs)
            true
        }
    }
}
