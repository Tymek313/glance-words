package com.example.words.repository

import com.example.words.coroutines.collectToListInBackground
import com.example.words.database.Database
import com.example.words.database.DbWidget
import com.example.words.database.utility.createTestDatabase
import com.example.words.fixture.dbSheetFixture
import com.example.words.fixture.dbWidgetFixture
import com.example.words.fixture.randomDbSheet
import com.example.words.fixture.randomExistingSheet
import com.example.words.fixture.randomWidgetId
import com.example.words.fixture.randomWidgetWithNewSheet
import com.example.words.fixture.widgetWithExistingSheetFixture
import com.example.words.model.SheetId
import com.example.words.model.Widget
import io.mockk.coEvery
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

    private fun insertSheetsToDb(): Int {
        val storedSheetCount = Random.nextInt(2, 10)
        repeat(storedSheetCount) { database.dbSheetQueries.insert(randomDbSheet()) }
        return storedSheetCount
    }

    @Test
    fun `when widget is observed_given widget does not exist_then null is returned`() = runTest(dispatcher) {
        val widgetEmissions = collectToListInBackground(repository.observeWidget(randomWidgetId()))

        assertNull(widgetEmissions.single())
    }

    @Test
    fun `when widget is observed_given widget has been updated_then widget is emitted`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(dbSheetFixture)
        database.dbWidgetQueries.insert(dbWidgetFixture.copy(sheet_id = nextStoredSheetId))

        val widgetEmissions = collectToListInBackground(repository.observeWidget(widgetWithExistingSheetFixture.id))

        assertEquals(
            widgetWithExistingSheetFixture.withSheetId(nextStoredSheetId),
            widgetEmissions.single()
        )
    }

    @Test
    fun `when widget is observed_given widget has never been updated_then widget containing no last updated date is emitted`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(randomDbSheet().copy(last_updated_at = null))
        database.dbWidgetQueries.insert(dbWidgetFixture.copy(sheet_id = nextStoredSheetId))

        val widgetEmissions = collectToListInBackground(repository.observeWidget(widgetWithExistingSheetFixture.id))

        assertNull(widgetEmissions.single()!!.sheet.lastUpdatedAt)
    }

    @Test
    fun `when widget is added_given sheet does not exist_then newly added widget is stored in the database`() = runTest(dispatcher) {
        val widget = randomWidgetWithNewSheet()
        coEvery { fakeSheetRepository.getBySheetSpreadsheetId(widget.sheet.sheetSpreadsheetId) } returns null
        coEvery { fakeSheetRepository.addSheet(widget.sheet) } returns randomExistingSheet().copy(id = SheetId(storedSheetCount))

        repository.addWidget(widget)

        val storedWidget = database.dbWidgetQueries.getById(widget.id.value).executeAsOne()
        assertEquals(widget.id.value, storedWidget.id, "id")
        assertEquals(storedSheetCount, storedWidget.sheet_id, "sheet_id")
    }

    @Test
    fun `when widget is added_given sheet exists_then existing widget is stored in the database`() = runTest(dispatcher) {
        val widget = randomWidgetWithNewSheet()
        coEvery { fakeSheetRepository.getBySheetSpreadsheetId(widget.sheet.sheetSpreadsheetId) } answers {
            randomExistingSheet().copy(id = SheetId(storedSheetCount))
        }

        repository.addWidget(widget)

        val storedWidget = database.dbWidgetQueries.getById(widget.id.value).executeAsOne()
        assertEquals(widget.id.value, storedWidget.id)
        assertEquals(storedSheetCount, storedWidget.sheet_id)
    }

    @Test
    fun `when widget is added_then widget with updated sheet is returned`() = runTest(dispatcher) {
        val widget = randomWidgetWithNewSheet().copy(sheet = randomExistingSheet().copy(id = SheetId(storedSheetCount)))
        coEvery { fakeSheetRepository.getBySheetSpreadsheetId(widget.sheet.sheetSpreadsheetId) } answers {
            if (Random.nextBoolean()) widget.sheet else null
        }
        coEvery { fakeSheetRepository.addSheet(widget.sheet) } returns widget.sheet

        val updatedWidget = repository.addWidget(widget)

        assertEquals(widget, updatedWidget)
    }

    @Test
    fun `when widget is deleted_then it is deleted from database`() = runTest(dispatcher) {
        val widgetId = randomWidgetId()
        database.dbWidgetQueries.insert(DbWidget(id = widgetId.value, sheet_id = storedSheetCount))

        repository.deleteWidget(widgetId)

        assertNull(database.dbWidgetQueries.getById(widgetId.value).executeAsOneOrNull())
    }

    fun Widget.withSheetId(id: Int) = copy(sheet = sheet.copy(id = SheetId(id)))
}