package com.pt.glancewords.domain.repository

import com.pt.glancewords.domain.model.SheetId
import com.pt.glancewords.domain.model.SheetSpreadsheetId
import com.pt.glancewords.domain.model.WordPair
import kotlinx.coroutines.flow.Flow

interface WordsRepository {
    fun observeWords(sheetId: SheetId): Flow<List<WordPair>>
    suspend fun synchronizeWords(sheetId: SheetId, sheetSpreadsheetId: SheetSpreadsheetId): Boolean
    suspend fun deleteWords(sheetId: SheetId)
}