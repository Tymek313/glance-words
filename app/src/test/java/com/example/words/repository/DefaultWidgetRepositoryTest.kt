package com.example.words.repository

import com.example.words.coroutines.collectToListInBackground
import com.example.words.database.Database
import com.example.words.database.DbWidget
import com.example.words.database.utility.createTestDatabase
import com.example.words.dbSheetFixture
import com.example.words.model.SheetId
import com.example.words.model.Widget
import com.example.words.randomDbSheet
import com.example.words.randomWidgetId
import com.example.words.randomWidgetWithNewSheet
import com.example.words.sheetFixture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DefaultWidgetRepositoryTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: DefaultWidgetRepository
    private lateinit var fakeSheetRepository: SheetRepository
    private lateinit var database: Database

    @Before
    fun setUp() {
        database = createTestDatabase()
        fakeSheetRepository = DefaultSheetRepository(database.dbSheetQueries, dispatcher)
        repository = DefaultWidgetRepository(database.dbWidgetQueries, fakeSheetRepository, dispatcher)
    }

    @Test
    fun `when widget is observed_given widget does not exist_then null is returned`() = runTest(dispatcher) {
        val widgetEmissions = collectToListInBackground(repository.observeWidget(randomWidgetId()))

        assertNull(widgetEmissions.single())
    }

    @Test
    fun `when widget is observed_given widget has been updated_then widget is emitted`() = runTest(dispatcher) {
        val widgetId = randomWidgetId()
        database.dbSheetQueries.insert(dbSheetFixture)
        val dbSheetId = database.dbSheetQueries.getLastId().executeAsOne().toInt()
        val expectedWidget = Widget(id = widgetId, sheet = sheetFixture.copy(id = SheetId(1)))
        database.dbWidgetQueries.insert(DbWidget(id = widgetId.value, sheet_id = dbSheetId))

        val widgetEmissions = collectToListInBackground(repository.observeWidget(widgetId))

        assertEquals(expectedWidget, widgetEmissions.singleOrNull())
    }

    @Test
    fun `when widget is observed_given widget has never been updated_then widget containing no last updated date is emitted`() = runTest(dispatcher) {
        val widgetId = randomWidgetId()
        val dbSheet = randomDbSheet().copy(last_updated_at = null)
        database.dbSheetQueries.insert(dbSheet)
        val dbSheetId = database.dbSheetQueries.getLastId().executeAsOne().toInt()
        database.dbWidgetQueries.insert(DbWidget(id = widgetId.value, sheet_id = dbSheetId))

        val widgetEmissions = collectToListInBackground(repository.observeWidget(widgetId))

        assertNull(widgetEmissions.single()!!.sheet.lastUpdatedAt)
    }

    @Test
    fun `when widget is added_then it is stored in the database`() = runTest(dispatcher) {
        val widget = randomWidgetWithNewSheet()

        repository.addWidget(widget)

        assertNotNull(database.dbWidgetQueries.getById(widget.id.value))
    }

    @Test
    fun `when widget is deleted_then it is removed from persistence`() = runTest(dispatcher) {
        val widgetId = randomWidgetId()
        val dbSheet = randomDbSheet()
        database.dbSheetQueries.insert(dbSheet)
        val dbSheetId = database.dbSheetQueries.getLastId().executeAsOne().toInt()
        database.dbWidgetQueries.insert(DbWidget(id = widgetId.value, sheet_id = dbSheetId))

        repository.deleteWidget(widgetId)

        assertNull(database.dbWidgetQueries.getById(widgetId.value).executeAsOneOrNull())
    }
}