package com.example.words.repository

import com.example.words.coroutines.collectToListInBackground
import com.example.words.database.Database
import com.example.words.database.DbWidget
import com.example.words.database.utility.createTestDatabase
import com.example.words.dbSheetFixture
import com.example.words.existingSheetFixture
import com.example.words.model.SheetId
import com.example.words.model.Widget
import com.example.words.randomDbSheet
import com.example.words.randomExistingSheet
import com.example.words.randomWidgetId
import com.example.words.randomWidgetWithNewSheet
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.properties.Delegates
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DefaultWidgetRepositoryTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: DefaultWidgetRepository
    private lateinit var fakeSheetRepository: SheetRepository
    private lateinit var database: Database
    private var storedSheetCount: Int by Delegates.notNull()
    private val nextStoredSheetId get() = storedSheetCount + 1

    @Before
    fun setUp() {
        database = createTestDatabase()
        fakeSheetRepository = mockk()
        repository = DefaultWidgetRepository(database.dbWidgetQueries, fakeSheetRepository, dispatcher)
        storedSheetCount = insertSheetsToDb()
    }

    @Test
    fun `when widget is observed_given widget does not exist_then null is returned`() = runTest(dispatcher) {
        val widgetEmissions = collectToListInBackground(repository.observeWidget(randomWidgetId()))

        assertNull(widgetEmissions.single())
    }

    @Test
    fun `when widget is observed_given widget has been updated_then widget is emitted`() = runTest(dispatcher) {
        val widgetId = randomWidgetId()
        val expectedWidget = Widget(id = widgetId, sheet = existingSheetFixture.copy(id = SheetId(nextStoredSheetId)))
        database.dbSheetQueries.insert(dbSheetFixture)
        database.dbWidgetQueries.insert(DbWidget(id = widgetId.value, sheet_id = nextStoredSheetId))

        val widgetEmissions = collectToListInBackground(repository.observeWidget(widgetId))

        assertEquals(expectedWidget, widgetEmissions.singleOrNull())
    }

    @Test
    fun `when widget is observed_given widget has never been updated_then widget containing no last updated date is emitted`() = runTest(dispatcher) {
        val widgetId = randomWidgetId()
        database.dbSheetQueries.insert(randomDbSheet().copy(last_updated_at = null))
        database.dbWidgetQueries.insert(DbWidget(id = widgetId.value, sheet_id = nextStoredSheetId))

        val widgetEmissions = collectToListInBackground(repository.observeWidget(widgetId))

        assertNull(widgetEmissions.single()!!.sheet.lastUpdatedAt)
    }

    @Test
    fun `when widget is added_given sheet does not exist_then sheet is added`() = runTest(dispatcher) {
        val widget = randomWidgetWithNewSheet()
        database.dbSheetQueries.insert(dbSheetFixture)
        coEvery { fakeSheetRepository.getBySheetSpreadsheetId(widget.sheet.sheetSpreadsheetId) } returns null
        coEvery { fakeSheetRepository.addSheet(widget.sheet) } returns existingSheetFixture.copy(id = SheetId(nextStoredSheetId))

        repository.addWidget(widget)

        coVerify { fakeSheetRepository.addSheet(widget.sheet) }
    }

    @Test
    fun `when widget is added_given sheet does not exist_then widget is stored in the database`() = runTest(dispatcher) {
        val widget = randomWidgetWithNewSheet()
        val addedSheetId = SheetId(Random.nextInt(1, storedSheetCount))
        coEvery { fakeSheetRepository.getBySheetSpreadsheetId(widget.sheet.sheetSpreadsheetId) } returns null
        coEvery { fakeSheetRepository.addSheet(widget.sheet) } returns randomExistingSheet().copy(id = addedSheetId)

        repository.addWidget(widget)

        val storedWidget = database.dbWidgetQueries.getById(widget.id.value).executeAsOne()
        assertEquals(widget.id.value, storedWidget.id)
        assertEquals(addedSheetId.value, storedWidget.sheet_id)
    }

    @Test
    fun `when widget is added_given sheet exists_then widget is stored in the database`() = runTest(dispatcher) {
        val widget = randomWidgetWithNewSheet()
        val addedSheetId = Random.nextInt(1, storedSheetCount)
        coEvery { fakeSheetRepository.getBySheetSpreadsheetId(widget.sheet.sheetSpreadsheetId) } answers {
            randomExistingSheet().copy(id = SheetId(addedSheetId))
        }

        repository.addWidget(widget)

        val storedWidget = database.dbWidgetQueries.getById(widget.id.value).executeAsOne()
        assertEquals(widget.id.value, storedWidget.id)
        assertEquals(addedSheetId, storedWidget.sheet_id)
    }

    @Test
    fun `when widget is added_then widget with updated sheet is returned`() = runTest(dispatcher) {
        val widget = randomWidgetWithNewSheet()
        val createdSheet = existingSheetFixture.copy(id = SheetId(nextStoredSheetId))
        database.dbSheetQueries.insert(dbSheetFixture)
        coEvery { fakeSheetRepository.getBySheetSpreadsheetId(widget.sheet.sheetSpreadsheetId) } answers {
            if (Random.nextBoolean()) createdSheet else null
        }
        coEvery { fakeSheetRepository.addSheet(widget.sheet) } returns createdSheet

        val updatedWidget = repository.addWidget(widget)

        assertEquals(widget.copy(sheet = createdSheet), updatedWidget)
    }

    @Test
    fun `when widget is deleted_then it is removed from persistence`() = runTest(dispatcher) {
        val widgetId = randomWidgetId()
        database.dbWidgetQueries.insert(DbWidget(id = widgetId.value, sheet_id = 1))

        repository.deleteWidget(widgetId)

        assertNull(database.dbWidgetQueries.getById(widgetId.value).executeAsOneOrNull())
    }

    private fun insertSheetsToDb(): Int {
        val storedSheetCount = Random.nextInt(2, 10)
        repeat(storedSheetCount) { database.dbSheetQueries.insert(randomDbSheet()) }
        return storedSheetCount
    }
}