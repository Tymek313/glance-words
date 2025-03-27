package com.pt.glancewords.data.widget.repository

import com.pt.glancewords.data.database.Database
import com.pt.glancewords.data.database.GetById
import com.pt.glancewords.data.fixture.randomDbSheet
import com.pt.glancewords.data.fixture.randomDbWidget
import com.pt.glancewords.data.fixture.randomWidgetId
import com.pt.glancewords.data.utility.createTestDatabase
import com.pt.glancewords.data.widget.mapper.WidgetMapper
import com.pt.glancewords.domain.sheet.model.Sheet
import com.pt.glancewords.domain.sheet.model.SheetId
import com.pt.glancewords.domain.sheet.model.SheetSpreadsheetId
import com.pt.glancewords.domain.widget.model.Widget
import com.pt.glancewords.domain.widget.model.WidgetId
import com.pt.glancewords.testcommon.coroutines.collectToListInBackground
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
import kotlin.test.assertTrue

class DatabaseWidgetRepositoryTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: DatabaseWidgetRepository
    private lateinit var fakeWidgetMapper: WidgetMapper
    private lateinit var database: Database

    private val everyMapToDomain get() = every { fakeWidgetMapper.mapToDomain(STORED_DB_WIDGET_GET_BY_ID) }

    private fun databaseWidgetIsMapped() = everyMapToDomain returns STORED_WIDGET
    private fun widgetIsMapped(sheetId: SheetId) = every { fakeWidgetMapper.mapToDb(NEW_WIDGET_ID, sheetId) } returns NEW_DB_WIDGET

    @Before
    fun setUp() {
        database = createTestDatabase()
        fakeWidgetMapper = mockk()
        repository = DatabaseWidgetRepository(database.dbWidgetQueries, fakeWidgetMapper, dispatcher)
        database.dbSheetQueries.insert(randomDbSheet())
        database.dbWidgetQueries.insert(randomDbWidget().copy(sheet_id = 1))
    }

    @Test
    fun `when widget is requested_given widget does not exist_then null is returned`() = runTest(dispatcher) {
        val widget = repository.getWidget(randomWidgetId())

        assertNull(widget)
    }

    @Test
    fun `when widget is requested_given widget exists_then widget is returned`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(STORED_DB_SHEET)
        database.dbWidgetQueries.insert(STORED_DB_WIDGET)
        databaseWidgetIsMapped()

        val widget = repository.getWidget(STORED_WIDGET.id)

        assertEquals(STORED_WIDGET, widget)
    }

    @Test
    fun `when widget is observed_given widget does not exist_then null is emitted`() = runTest(dispatcher) {
        val widgetEmissions = collectToListInBackground(repository.observeWidget(randomWidgetId()))

        assertNull(widgetEmissions.single())
    }

    @Test
    fun `when widget is observed_given widget exists_then widget is emitted`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(STORED_DB_SHEET)
        database.dbWidgetQueries.insert(STORED_DB_WIDGET)
        databaseWidgetIsMapped()

        val widgetEmissions = collectToListInBackground(repository.observeWidget(STORED_WIDGET.id))

        assertEquals(STORED_WIDGET, widgetEmissions.single())
    }

    @Test
    fun `when widget is added_then widget is stored in the database`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(STORED_DB_SHEET)
        widgetIsMapped(STORED_SHEET.id)

        repository.addWidget(NEW_WIDGET_ID, STORED_SHEET.id)

        val storedWidgets = database.dbWidgetQueries.getAll().executeAsList()
        assertEquals(2, storedWidgets.size, "Database should contain both added and already stored widgets")
        val addedWidget = storedWidgets.find { it.id == NEW_DB_WIDGET.id }
        assertEquals(NEW_DB_WIDGET.id, addedWidget?.id)
        assertEquals(STORED_DB_SHEET.id, addedWidget?.sheet_id)
    }

    @Test
    fun `when widget is deleted_then it is deleted from database`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(STORED_DB_SHEET)
        database.dbWidgetQueries.insert(STORED_DB_WIDGET)

        repository.deleteWidget(STORED_WIDGET.id)

        val widgets = database.dbWidgetQueries.getAll().executeAsList()
        assertEquals(1, widgets.size, "Other widgets should not be deleted")
        assertTrue(widgets.none { it.id == STORED_DB_WIDGET.id }, "Expected widget has not been deleted")
    }

    private companion object {
        val STORED_DB_SHEET = randomDbSheet().copy(id = 2)
        val NEW_DB_WIDGET = randomDbWidget().copy(sheet_id = STORED_DB_SHEET.id)
        val NEW_WIDGET_ID = WidgetId(NEW_DB_WIDGET.id)
        val STORED_DB_WIDGET = randomDbWidget().copy(sheet_id = STORED_DB_SHEET.id)
        val STORED_SHEET = STORED_DB_SHEET.run {
            Sheet(
                id = SheetId(id),
                sheetSpreadsheetId = SheetSpreadsheetId(spreadsheet_id, sheet_id),
                name = name,
                lastUpdatedAt = Instant.ofEpochSecond(last_updated_at!!)
            )
        }
        val STORED_WIDGET = STORED_DB_WIDGET.run {
            Widget(id = WidgetId(id), sheet = STORED_SHEET)
        }
        val STORED_DB_WIDGET_GET_BY_ID = GetById(
            id = STORED_DB_WIDGET.id,
            sheet_id = STORED_DB_SHEET.id,
            s_spreadsheet_id = STORED_DB_SHEET.spreadsheet_id,
            s_sheet_id = STORED_DB_SHEET.sheet_id,
            s_name = STORED_DB_SHEET.name,
            s_last_updated_at = STORED_DB_SHEET.last_updated_at
        )
    }
}