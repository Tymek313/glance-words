package com.pt.glancewords.domain.words.repository

import com.pt.glancewords.domain.sheet.model.SheetId
import com.pt.glancewords.domain.sheet.model.SheetSpreadsheetId
import com.pt.glancewords.domain.words.model.WordPair
import kotlinx.coroutines.flow.Flow

interface WordsRepository {
    fun observeWords(sheetId: SheetId): Flow<List<WordPair>>
    suspend fun synchronizeWords(sheetId: SheetId, sheetSpreadsheetId: SheetSpreadsheetId): Boolean
}
