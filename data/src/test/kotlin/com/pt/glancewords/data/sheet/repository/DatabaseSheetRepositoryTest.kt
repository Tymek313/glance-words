package com.pt.glancewords.data.sheet.repository

import com.pt.glancewords.data.database.Database
import com.pt.glancewords.data.fixture.randomDbSheet
import com.pt.glancewords.data.fixture.randomSheetSpreadsheetId
import com.pt.glancewords.data.sheet.mapper.SheetMapper
import com.pt.glancewords.data.utility.createTestDatabase
import com.pt.glancewords.domain.sheet.model.NewSheet
import com.pt.glancewords.domain.sheet.model.Sheet
import com.pt.glancewords.domain.sheet.model.SheetId
import com.pt.glancewords.domain.sheet.model.SheetSpreadsheetId
import com.pt.glancewords.testcommon.fixture.randomInstant
import com.pt.glancewords.testcommon.fixture.randomString
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DatabaseSheetRepositoryTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: DatabaseSheetRepository
    private lateinit var database: Database
    private lateinit var fakeSheetMapper: SheetMapper

    private val everyMapToDomain get() = every { fakeSheetMapper.mapToDomain(STORED_DB_SHEET) }
    private val everyMapToDomainFromNewSheet get() = every { fakeSheetMapper.mapToDomain(newSheet = NEW_SHEET, sheetId = SheetId(2)) }
    private val everyMapToDb get() = every { fakeSheetMapper.mapToDb(NEW_SHEET) }

    @Before
    fun setUp() {
        database = createTestDatabase()
        fakeSheetMapper = mockk()
        repository = DatabaseSheetRepository(database.dbSheetQueries, fakeSheetMapper, dispatcher)
        database.dbSheetQueries.insert(randomDbSheet())
    }

    @Test
    fun `when sheet is added_then it is stored in database`() = runTest(dispatcher) {
        everyMapToDb returns STORED_DB_SHEET
        everyMapToDomainFromNewSheet returns STORED_SHEET

        repository.addSheet(NEW_SHEET)

        val sheets = database.dbSheetQueries.getAll().executeAsList()
        assertEquals(2, sheets.size)
        assertEquals(STORED_DB_SHEET, sheets.single { it.id == STORED_DB_SHEET.id })
    }

    @Test
    fun `when sheet is added_then sheet with updated id is returned`() = runTest(dispatcher) {
        everyMapToDb returns STORED_DB_SHEET
        everyMapToDomainFromNewSheet returns STORED_SHEET

        val updatedSheet = repository.addSheet(NEW_SHEET)

        assertEquals(STORED_SHEET.id, updatedSheet.id)
    }

    @Test
    fun `when sheet is requested by spreadsheet id_given it exists_sheet is returned`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(STORED_DB_SHEET)
        everyMapToDomain returns STORED_SHEET

        val sheet = repository.getBySheetSpreadsheetId(STORED_SHEET.sheetSpreadsheetId)

        assertEquals(STORED_SHEET, sheet)
    }

    @Test
    fun `when sheet is requested by spreadsheet id_given it does not exist_null is returned`() = runTest(dispatcher) {
        val sheet = repository.getBySheetSpreadsheetId(randomSheetSpreadsheetId())

        assertNull(sheet)
    }

    @Test
    fun `when last update is updated for the widget_then it is updated in the database`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(STORED_DB_SHEET)
        val updatedInstant = randomInstant()

        repository.updateLastUpdatedAt(sheetId = SheetId(1), lastUpdatedAt = updatedInstant)

        val sheets = database.dbSheetQueries.getAll().executeAsList()
        val updatedLastUpdatedAt = sheets.first { it.id == 1 }.last_updated_at
        val unchangedLastUpdatedAt = sheets.first { it.id == 2 }.last_updated_at
        assertEquals(updatedInstant.epochSecond, updatedLastUpdatedAt)
        assertEquals(STORED_DB_SHEET.last_updated_at, unchangedLastUpdatedAt)
    }

    @Test
    fun `when sheet is deleted_then it is deleted from database`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(STORED_DB_SHEET)

        repository.deleteSheet(STORED_SHEET.id)

        assertNull(database.dbSheetQueries.getAll().executeAsList().find { it.id == STORED_DB_SHEET.id })
    }

    private companion object {
        val STORED_DB_SHEET = randomDbSheet().copy(id = 2)
        val STORED_SHEET = STORED_DB_SHEET.run {
            Sheet(
                id = SheetId(id),
                sheetSpreadsheetId = SheetSpreadsheetId(spreadsheet_id, sheet_id),
                name = name,
                lastUpdatedAt = Instant.ofEpochSecond(last_updated_at!!)
            )
        }
        val NEW_SHEET = NewSheet(
            sheetSpreadsheetId = randomSheetSpreadsheetId(),
            name = randomString(),
        )
    }
}