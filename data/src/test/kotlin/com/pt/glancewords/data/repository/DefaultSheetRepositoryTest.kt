package com.pt.glancewords.data.repository

import com.pt.glancewords.data.database.Database
import com.pt.glancewords.data.fixture.DB_SHEET
import com.pt.glancewords.data.fixture.EXISTING_SHEET
import com.pt.glancewords.data.fixture.NEW_SHEET
import com.pt.glancewords.data.fixture.SHEET_SPREADSHEET_ID
import com.pt.glancewords.data.fixture.randomDbSheet
import com.pt.glancewords.data.mapper.SheetMapper
import com.pt.glancewords.data.utility.createTestDatabase
import com.pt.glancewords.domain.model.SheetId
import com.pt.testcommon.fixture.randomInstant
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DefaultSheetRepositoryTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: DefaultSheetRepository
    private lateinit var database: Database
    private lateinit var fakeSheetMapper: SheetMapper

    private val everyMapToDomain get() = every { fakeSheetMapper.mapToDomain(DB_SHEET.copy(id = 1)) }
    private val everyMapToDomainFromNewSheet get() = every { fakeSheetMapper.mapToDomain(newSheet = NEW_SHEET, sheetId = SheetId(2)) }
    private val everyMapToDb get() = every { fakeSheetMapper.mapToDb(NEW_SHEET) }

    private val mappedDomainSheet = EXISTING_SHEET.copy(id = SheetId(1))
    private val mappedDbSheet = DB_SHEET.copy(id = 2)

    @Before
    fun setUp() {
        database = createTestDatabase()
        fakeSheetMapper = mockk()
        repository = DefaultSheetRepository(database.dbSheetQueries, fakeSheetMapper, dispatcher)
    }

    @Test
    fun `when sheets are requested_then stored sheets are returned`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(DB_SHEET)
        everyMapToDomain returns mappedDomainSheet

        val sheets = repository.getSheets()

        assertEquals(mappedDomainSheet, sheets.singleOrNull())
    }

    @Test
    fun `when sheets are requested_given sheet was never updated_then returned sheet does not contain last updated date`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(DB_SHEET)
        everyMapToDomain returns mappedDomainSheet.copy(lastUpdatedAt = null)

        val sheets = repository.getSheets()

        assertNull(sheets.first().lastUpdatedAt)
    }

    @Test
    fun `when sheet is added_then it is stored in database`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(randomDbSheet())
        everyMapToDb returns mappedDbSheet
        everyMapToDomainFromNewSheet returns EXISTING_SHEET

        repository.addSheet(NEW_SHEET)

        val sheets = database.dbSheetQueries.getAll().executeAsList()
        assertEquals(2, sheets.size)
        assertEquals(mappedDbSheet, sheets.single { it.id == 2 })
    }

    @Test
    fun `when sheet is added_given transaction succeeded_then sheet with updated id is returned`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(randomDbSheet())
        everyMapToDb returns mappedDbSheet
        everyMapToDomainFromNewSheet returns EXISTING_SHEET

        val updatedSheet = repository.addSheet(NEW_SHEET)

        assertEquals(EXISTING_SHEET.id, updatedSheet.id)
    }

    @Test
    fun `when sheet is requested by spreadsheet id_given it exists_sheet is returned`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(DB_SHEET)
        everyMapToDomain returns mappedDomainSheet

        val sheet = repository.getBySheetSpreadsheetId(SHEET_SPREADSHEET_ID)

        assertEquals(mappedDomainSheet, sheet)
    }


    @Test
    fun `when sheet is requested by spreadsheet id_given it does not exist_null is returned`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(randomDbSheet())

        val sheet = repository.getBySheetSpreadsheetId(SHEET_SPREADSHEET_ID)

        assertNull(sheet)
    }

    @Test
    fun `when last update is updated for the widget_then it is updated in the database`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(randomDbSheet())
        database.dbSheetQueries.insert(DB_SHEET)
        val updatedInstant = randomInstant()

        repository.updateLastUpdatedAt(sheetId = SheetId(1), lastUpdatedAt = updatedInstant)

        val sheets = database.dbSheetQueries.getAll().executeAsList()
        val updatedLastUpdatedAt = sheets.first { it.id == 1 }.last_updated_at
        val unchangedLastUpdatedAt = sheets.first { it.id == 2 }.last_updated_at
        assertEquals(updatedInstant.epochSecond, updatedLastUpdatedAt)
        assertEquals(DB_SHEET.last_updated_at, unchangedLastUpdatedAt)
    }
}