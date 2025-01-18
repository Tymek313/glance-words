package com.example.words.data.repository

import com.example.domain.model.SheetId
import com.example.testcommon.fixture.randomInstant
import com.example.words.data.fixture.dbSheetFixture
import com.example.words.data.fixture.existingSheetFixture
import com.example.words.data.fixture.newSheetFixture
import com.example.words.data.fixture.randomDbSheet
import com.example.words.data.fixture.sheetSpreadsheetIdFixture
import com.example.words.data.mapper.SheetMapper
import com.example.words.data.utility.createTestDatabase
import com.example.words.database.Database
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

    private val everyMapToDomain get() = every { fakeSheetMapper.mapToDomain(dbSheetFixture.copy(id = 1)) }
    private val everyMapToDb get() = every { fakeSheetMapper.mapToDb(newSheetFixture) }

    private val mappedDomainSheet = existingSheetFixture.copy(id = SheetId(1))
    private val mappedDbSheet = dbSheetFixture.copy(id = 2)

    @Before
    fun setUp() {
        database = createTestDatabase()
        fakeSheetMapper = mockk()
        repository = DefaultSheetRepository(database.dbSheetQueries, fakeSheetMapper, dispatcher)
    }

    @Test
    fun `when sheets are requested_then stored sheets are returned`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(dbSheetFixture)
        everyMapToDomain returns mappedDomainSheet

        val sheets = repository.getSheets()

        assertEquals(mappedDomainSheet, sheets.singleOrNull())
    }

    @Test
    fun `when sheets are requested_given sheet was never updated_then returned sheet does not contain last updated date`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(dbSheetFixture)
        everyMapToDomain returns mappedDomainSheet.copy(lastUpdatedAt = null)

        val sheets = repository.getSheets()

        assertNull(sheets.first().lastUpdatedAt)
    }

    @Test
    fun `when sheet is added_then it is stored in database`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(randomDbSheet())
        everyMapToDb returns mappedDbSheet

        repository.addSheet(newSheetFixture)

        val sheets = database.dbSheetQueries.getAll().executeAsList()
        assertEquals(2, sheets.size)
        assertEquals(mappedDbSheet, sheets.single { it.id == 2 })
    }

    @Test
    fun `when sheet is added_then sheet with updated id is returned`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(randomDbSheet())
        everyMapToDb returns mappedDbSheet

        val updatedSheet = repository.addSheet(newSheetFixture)

        assertEquals(2, updatedSheet.id.value)
    }

    @Test
    fun `when sheet is requested by spreadsheet id_given it exists_sheet is returned`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(dbSheetFixture)
        everyMapToDomain returns mappedDomainSheet

        val sheet = repository.getBySheetSpreadsheetId(sheetSpreadsheetIdFixture)

        assertEquals(mappedDomainSheet, sheet)
    }


    @Test
    fun `when sheet is requested by spreadsheet id_given it does not exist_null is returned`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(randomDbSheet())

        val sheet = repository.getBySheetSpreadsheetId(sheetSpreadsheetIdFixture)

        assertNull(sheet)
    }

    @Test
    fun `when last update is updated for the widget_then it is updated in the database`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(randomDbSheet())
        database.dbSheetQueries.insert(dbSheetFixture)
        val updatedInstant = randomInstant()

        repository.updateLastUpdatedAt(sheetId = SheetId(1), lastUpdatedAt = updatedInstant)

        val sheets = database.dbSheetQueries.getAll().executeAsList()
        val updatedLastUpdatedAt = sheets.first { it.id == 1 }.last_updated_at
        val unchangedLastUpdatedAt = sheets.first { it.id == 2 }.last_updated_at
        assertEquals(updatedInstant.epochSecond, updatedLastUpdatedAt)
        assertEquals(dbSheetFixture.last_updated_at, unchangedLastUpdatedAt)
    }
}