package com.example.words.data.repository

import com.example.domain.model.SheetId
import com.example.domain.model.Widget
import com.example.domain.repository.SheetRepository
import com.example.testcommon.coroutines.collectToListInBackground
import com.example.words.data.fixture.DB_SHEET
import com.example.words.data.fixture.DB_WIDGET
import com.example.words.data.fixture.WIDGET_WITH_EXISTING_SHEET
import com.example.words.data.fixture.randomDbSheet
import com.example.words.data.fixture.randomExistingSheet
import com.example.words.data.fixture.randomWidgetId
import com.example.words.data.fixture.randomWidgetWithNewSheet
import com.example.words.database.Database
import com.example.words.database.DbWidget
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
        database = com.example.words.data.utility.createTestDatabase()
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
        database.dbSheetQueries.insert(DB_SHEET)
        database.dbWidgetQueries.insert(DB_WIDGET.copy(sheet_id = nextStoredSheetId))

        val widgetEmissions = collectToListInBackground(repository.observeWidget(WIDGET_WITH_EXISTING_SHEET.id))

        assertEquals(
            WIDGET_WITH_EXISTING_SHEET.withSheetId(nextStoredSheetId),
            widgetEmissions.single()
        )
    }

    @Test
    fun `when widget is observed_given widget has never been updated_then widget containing no last updated date is emitted`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(randomDbSheet().copy(last_updated_at = null))
        database.dbWidgetQueries.insert(DB_WIDGET.copy(sheet_id = nextStoredSheetId))

        val widgetEmissions = collectToListInBackground(repository.observeWidget(WIDGET_WITH_EXISTING_SHEET.id))

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
        val widget = randomWidgetWithNewSheet()
            .copy(sheet = randomExistingSheet().copy(id = SheetId(storedSheetCount)))
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

    private fun Widget.withSheetId(id: Int) = copy(sheet = sheet.copy(id = SheetId(id)))
}