package com.example.words.repository

import com.example.words.database.Database
import com.example.words.database.DbSheet
import com.example.words.database.utility.createTestDatabase
import com.example.words.model.Sheet
import com.example.words.model.SheetId
import com.example.words.model.SheetSpreadsheetId
import com.example.words.randomDbSheet
import com.example.words.randomInstant
import com.example.words.randomSheet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DefaultSheetRepositoryTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: DefaultSheetRepository
    private lateinit var database: Database

    @Before
    fun setUp() {
        database = createTestDatabase()
        repository = DefaultSheetRepository(database.dbSheetQueries, dispatcher)
    }

    @Test
    fun `when sheets are requested_then stored sheets are returned`() = runTest(dispatcher) {
        val dbSheet = randomDbSheet()
        val expectedSheet = createDomainSheetFrom(dbSheet).copy(id = SheetId(1))
        database.dbSheetQueries.insert(dbSheet)

        val sheets = repository.getSheets()

        assertEquals(expectedSheet, sheets.singleOrNull())
    }

    @Test
    fun `when sheets are requested_given sheet was never updated_then returned sheet does not contain last updated date`() = runTest(dispatcher) {
        val dbSheet = randomDbSheet().copy(last_updated_at = null)
        database.dbSheetQueries.insert(dbSheet)

        val sheets = repository.getSheets()

        assertNull(sheets.first().lastUpdatedAt)
    }

    @Test
    fun `when sheet is added_then it is stored in database`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(randomDbSheet())
        val sheet = randomSheet()
        val expectedDbSheet = createDbSheetFrom(sheet).copy(id = 2)

        repository.addSheet(sheet)

        val sheets = database.dbSheetQueries.getAll().executeAsList()
        assertEquals(2, sheets.size)
        assertEquals(expectedDbSheet, sheets.singleOrNull { it.id == 2 })
    }

    @Test
    fun `when sheet is added_given it was never updated_then stored sheet does not contain last updated date`() = runTest(dispatcher) {
        val sheet = randomSheet().copy(lastUpdatedAt = null)

        repository.addSheet(sheet)

        val sheets = database.dbSheetQueries.getAll().executeAsList()
        assertNull(sheets.first().last_updated_at)
    }

    @Test
    fun `when sheet is added_then sheet with updated id is returned`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(randomDbSheet())
        val sheet = randomSheet()

        val updatedSheet = repository.addSheet(sheet)

        assertEquals(2, updatedSheet.id.value)
    }

    @Test
    fun `when last update is updated for the widget_then it is updated in the database`() = runTest(dispatcher) {
        val dbSheet = randomDbSheet()
        database.dbSheetQueries.insert(dbSheet)
        val updatedInstant = randomInstant()

        repository.updateLastUpdatedAt(sheetId = SheetId(1), lastUpdatedAt = updatedInstant)

        val sheets = database.dbSheetQueries.getAll().executeAsList()
        val storedLastUpdatedAt = sheets.find { it.id == 1 }?.last_updated_at
        assertEquals(updatedInstant.epochSecond, storedLastUpdatedAt)
    }

    private fun createDomainSheetFrom(dbSheet: DbSheet) = Sheet(
        id = dbSheet.id.let(::SheetId),
        sheetSpreadsheetId = dbSheet.run { SheetSpreadsheetId(spreadsheet_id, sheet_id) },
        name = dbSheet.name,
        lastUpdatedAt = Instant.ofEpochSecond(dbSheet.last_updated_at!!)
    )

    private fun createDbSheetFrom(sheet: Sheet) = DbSheet(
        id = sheet.id.value,
        name = sheet.name,
        spreadsheet_id = sheet.sheetSpreadsheetId.spreadsheetId,
        sheet_id = sheet.sheetSpreadsheetId.sheetId,
        last_updated_at = sheet.lastUpdatedAt?.epochSecond
    )
}