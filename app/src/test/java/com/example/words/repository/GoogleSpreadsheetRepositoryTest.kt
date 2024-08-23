package com.example.words.repository

import com.example.words.datasource.GoogleSpreadsheetDataSource
import com.example.words.model.SpreadsheetSheet
import com.example.words.randomInt
import com.example.words.randomString
import com.google.api.services.sheets.v4.model.Sheet
import com.google.api.services.sheets.v4.model.SheetProperties
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class GoogleSpreadsheetRepositoryTest {

    private lateinit var fakeGoogleSpreadsheetDataSource: GoogleSpreadsheetDataSource
    private lateinit var repository: GoogleSpreadsheetRepository

    @Before
    fun setup() {
        fakeGoogleSpreadsheetDataSource = mockk()
        repository = GoogleSpreadsheetRepository(fakeGoogleSpreadsheetDataSource)
    }

    @Test
    fun `when spreadsheet sheets are fetched_given there are sheets_they are returned`() = runTest {
        val spreadsheetId = randomString()
        val sheetId = randomInt()
        val sheetTitle = randomString()
        coEvery { fakeGoogleSpreadsheetDataSource.getSpreadsheets(spreadsheetId) } returns listOf(
            Sheet().setProperties(SheetProperties().setSheetId(sheetId).setTitle(sheetTitle))
        )

        val sheets = repository.fetchSpreadsheetSheets(spreadsheetId)

        assertEquals(listOf(SpreadsheetSheet(sheetId, sheetTitle)), sheets)
        coVerify { fakeGoogleSpreadsheetDataSource.getSpreadsheets(spreadsheetId) }
    }
}