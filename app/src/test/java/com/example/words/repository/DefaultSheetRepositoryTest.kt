package com.example.words.repository

import com.example.words.database.Database
import com.example.words.database.utility.createTestDatabase
import com.example.words.fixture.dbSheetFixture
import com.example.words.fixture.existingSheetFixture
import com.example.words.fixture.randomDbSheet
import com.example.words.fixture.randomInstant
import com.example.words.fixture.randomNewSheet
import com.example.words.model.SheetId
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

    @Before
    fun setUp() {
        database = createTestDatabase()
        repository = DefaultSheetRepository(database.dbSheetQueries, dispatcher)
    }

    @Test
    fun `when sheets are requested_then stored sheets are returned`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(dbSheetFixture)

        val sheets = repository.getSheets()

        assertEquals(existingSheetFixture.copy(id = SheetId(1)), sheets.singleOrNull())
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
        val sheet = randomNewSheet()
        val expectedDbSheet = dbSheetFixture.copy(id = 2)

        repository.addSheet(sheet)

        val sheets = database.dbSheetQueries.getAll().executeAsList()
        assertEquals(2, sheets.size)
        assertEquals(expectedDbSheet, sheets.singleOrNull { it.id == 2 })
    }

    @Test
    fun `when sheet is added_given it was never updated_then stored sheet does not contain last updated date`() = runTest(dispatcher) {
        val sheet = randomNewSheet().copy(lastUpdatedAt = null)

        repository.addSheet(sheet)

        val sheets = database.dbSheetQueries.getAll().executeAsList()
        assertNull(sheets.first().last_updated_at)
    }

    @Test
    fun `when sheet is added_then sheet with updated id is returned`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(randomDbSheet())

        val updatedSheet = repository.addSheet(randomNewSheet())

        assertEquals(2, updatedSheet.id.value)
    }

    @Test
    fun `when last update is updated for the widget_then it is updated in the database`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(randomDbSheet())
        val updatedInstant = randomInstant()

        repository.updateLastUpdatedAt(sheetId = SheetId(1), lastUpdatedAt = updatedInstant)

        val sheets = database.dbSheetQueries.getAll().executeAsList()
        val storedLastUpdatedAt = sheets.find { it.id == 1 }?.last_updated_at
        assertEquals(updatedInstant.epochSecond, storedLastUpdatedAt)
    }
}