package com.pt.glancewords.data.repository

import com.google.api.services.sheets.v4.model.Sheet
import com.google.api.services.sheets.v4.model.SheetProperties
import com.pt.glancewords.data.datasource.GoogleSpreadsheetDataSource
import com.pt.glancewords.domain.model.SpreadsheetSheet
import com.pt.testcommon.fixture.randomInt
import com.pt.testcommon.fixture.randomString
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class GoogleSpreadsheetRepositoryTest {

    private lateinit var fakeGoogleSpreadsheetDataSource: GoogleSpreadsheetDataSource
    private lateinit var repository: GoogleSpreadsheetRepository

    private val everyGetSpreadsheets get() = coEvery { fakeGoogleSpreadsheetDataSource.getSpreadsheets(SPREADSHEET_ID_FIXTURE) }

    @Before
    fun setup() {
        fakeGoogleSpreadsheetDataSource = mockk()
        repository = GoogleSpreadsheetRepository(fakeGoogleSpreadsheetDataSource)
    }

    @Test
    fun `when spreadsheet sheets are fetched_given there are sheets_they are returned`() = runTest {
        googleSheetsAreFetched()

        val sheets = repository.fetchSpreadsheetSheets(SPREADSHEET_ID_FIXTURE)

        assertEquals(listOf(SpreadsheetSheet(SHEET_ID_FIXTURE, SHEET_TITLE_FIXTURE)), sheets)
    }

    private fun googleSheetsAreFetched() {
        everyGetSpreadsheets returns listOf(GOOGLE_SHEET_FIXTURE)
    }

    private companion object {
        val SPREADSHEET_ID_FIXTURE = randomString()
        val SHEET_ID_FIXTURE = randomInt()
        val SHEET_TITLE_FIXTURE = randomString()
        val GOOGLE_SHEET_FIXTURE: Sheet = Sheet().setProperties(SheetProperties().setSheetId(SHEET_ID_FIXTURE).setTitle(SHEET_TITLE_FIXTURE))
    }
}