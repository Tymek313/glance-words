package com.pt.glancewords.data.widget.repository

import com.pt.glancewords.data.database.Database
import com.pt.glancewords.data.database.DbWidget
import com.pt.glancewords.data.database.GetById
import com.pt.glancewords.data.fixture.DB_SHEET
import com.pt.glancewords.data.fixture.DB_WIDGET
import com.pt.glancewords.data.fixture.WIDGET_WITH_EXISTING_SHEET
import com.pt.glancewords.data.fixture.randomDbSheet
import com.pt.glancewords.data.fixture.randomWidgetId
import com.pt.glancewords.data.utility.createTestDatabase
import com.pt.glancewords.data.widget.mapper.WidgetMapper
import com.pt.glancewords.domain.sheet.model.SheetId
import com.pt.glancewords.domain.widget.model.Widget
import com.pt.glancewords.testcommon.coroutines.collectToListInBackground
import io.mockk.every
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

class DatabaseWidgetRepositoryTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: DatabaseWidgetRepository
    private lateinit var fakeWidgetMapper: WidgetMapper
    private lateinit var database: Database
    private var storedSheetCount: Int by Delegates.notNull()
    private val nextStoredSheetId get() = storedSheetCount + 1

    @Before
    fun setUp() {
        database = createTestDatabase()
        fakeWidgetMapper = mockk()
        repository = DatabaseWidgetRepository(database.dbWidgetQueries, fakeWidgetMapper, dispatcher)
        storedSheetCount = insertSheetsToDb()
    }

    private fun insertSheetsToDb(): Int {
        val storedSheetCount = Random.nextInt(2, 10)
        repeat(storedSheetCount) { database.dbSheetQueries.insert(randomDbSheet()) }
        return storedSheetCount
    }

    @Test
    fun `when widget is requested_given widget does not exist_then null is returned`() = runTest(dispatcher) {
        val widget = repository.getWidget(randomWidgetId())

        assertNull(widget)
    }

    @Test
    fun `when widget is requested_given widget does not exist_then widget is returned`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(DB_SHEET)
        database.dbWidgetQueries.insert(DB_WIDGET.copy(sheet_id = nextStoredSheetId))
        every {
            fakeWidgetMapper.mapToDomain(
                GetById(
                    id = DB_WIDGET.id,
                    sheet_id = nextStoredSheetId,
                    s_spreadsheet_id = DB_SHEET.spreadsheet_id,
                    s_sheet_id = DB_SHEET.sheet_id,
                    s_name = DB_SHEET.name,
                    s_last_updated_at = DB_SHEET.last_updated_at
                )
            )
        } returns WIDGET_WITH_EXISTING_SHEET

        val widget = repository.getWidget(WIDGET_WITH_EXISTING_SHEET.id)

        assertEquals(WIDGET_WITH_EXISTING_SHEET, widget)
    }

    @Test
    fun `when widget is observed_given widget does not exist_then null is emitted`() = runTest(dispatcher) {
        val widgetEmissions = collectToListInBackground(repository.observeWidget(randomWidgetId()))

        assertNull(widgetEmissions.single())
    }

    @Test
    fun `when widget is observed_given widget has been updated_then widget is emitted`() = runTest(dispatcher) {
        database.dbSheetQueries.insert(DB_SHEET)
        database.dbWidgetQueries.insert(DB_WIDGET.copy(sheet_id = nextStoredSheetId))
        every {
            fakeWidgetMapper.mapToDomain(
                GetById(
                    id = DB_WIDGET.id,
                    sheet_id = nextStoredSheetId,
                    s_spreadsheet_id = DB_SHEET.spreadsheet_id,
                    s_sheet_id = DB_SHEET.sheet_id,
                    s_name = DB_SHEET.name,
                    s_last_updated_at = DB_SHEET.last_updated_at
                )
            )
        } returns WIDGET_WITH_EXISTING_SHEET

        val widgetEmissions = collectToListInBackground(repository.observeWidget(WIDGET_WITH_EXISTING_SHEET.id))

        assertEquals(WIDGET_WITH_EXISTING_SHEET, widgetEmissions.single())
    }

    @Test
    fun `when widget is added_then existing widget is stored in the database`() = runTest(dispatcher) {
        val widgetId = randomWidgetId()
        val sheetId = SheetId(storedSheetCount)
        every { fakeWidgetMapper.mapToDb(widgetId, sheetId) } returns DbWidget(widgetId.value, sheetId.value)

        repository.addWidget(widgetId, sheetId)

        val storedWidget = database.dbWidgetQueries.getById(widgetId.value).executeAsOne()
        assertEquals(widgetId.value, storedWidget.id)
        assertEquals(storedSheetCount, storedWidget.sheet_id)
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