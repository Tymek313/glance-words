package com.example.words.repository

import com.example.words.datasource.DefaultGoogleSpreadsheetDataSource
import com.example.words.datasource.GoogleSpreadsheetDataSource
import com.example.words.model.SpreadsheetSheet
import com.google.api.services.sheets.v4.model.Sheet
import com.google.api.services.sheets.v4.model.SheetProperties
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.UUID
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNull

@MockKExtension.ConfirmVerification
class GoogleSpreadsheetRepositoryTest {

    private lateinit var googleSpreadsheetDataSource: GoogleSpreadsheetDataSource
    private lateinit var repository: GoogleSpreadsheetRepository

    @Before
    fun setup() {
        googleSpreadsheetDataSource = mockk()
        repository = GoogleSpreadsheetRepository(googleSpreadsheetDataSource)
    }

    @Test
    fun `when spreadsheet sheets are fetched_given there are sheets_they are returned`() = runTest {
        val spreadsheetId = UUID.randomUUID().toString()
        val sheetId = Random.nextInt()
        val sheetTitle = UUID.randomUUID().toString()
        coEvery { googleSpreadsheetDataSource.getSpreadsheets(spreadsheetId) } returns listOf(
            Sheet().setProperties(SheetProperties().setSheetId(sheetId).setTitle(sheetTitle))
        )

        val sheets = repository.fetchSpreadsheetSheets(spreadsheetId)

        assertEquals(listOf(SpreadsheetSheet(sheetId, sheetTitle)) ,sheets)
        coVerify { googleSpreadsheetDataSource.getSpreadsheets(spreadsheetId) }
    }

    @Test
    fun `when spreadsheet sheets are fetched_given there are no sheets_null is returned`() = runTest {
        val spreadsheetId = UUID.randomUUID().toString()
        coEvery { googleSpreadsheetDataSource.getSpreadsheets(spreadsheetId) } returns null

        val sheets = repository.fetchSpreadsheetSheets(spreadsheetId)

        assertNull(sheets)
        coVerify { googleSpreadsheetDataSource.getSpreadsheets(spreadsheetId) }
    }
}