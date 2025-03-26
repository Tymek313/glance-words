package com.pt.glancewords.data.words.datasource

import app.cash.sqldelight.coroutines.asFlow
import com.pt.glancewords.data.database.DbWordPairQueries
import com.pt.glancewords.domain.sheet.model.SheetId
import com.pt.glancewords.domain.words.model.WordPair
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal interface WordsLocalDataSource {
    fun observeWords(sheetId: SheetId): Flow<List<WordPair>>
    suspend fun storeWords(sheetId: SheetId, words: List<WordPair>)
}

internal class DatabaseWordsLocalDataSource(
    private val database: DbWordPairQueries,
    private val ioDispatcher: CoroutineDispatcher
) : WordsLocalDataSource {

    override fun observeWords(sheetId: SheetId) = database.getBySheetId(sheetId.value, ::WordPair)
        .asFlow()
        .map { it.executeAsList() }

    override suspend fun storeWords(sheetId: SheetId, words: List<WordPair>) {
        withContext(ioDispatcher) {
            database.transaction {
                words.forEach { wordPair -> database.insertOrIgnore(sheetId.value, wordPair.original, wordPair.translated) }
            }
        }
    }
}
