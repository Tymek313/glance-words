package com.example.words.data.schema

import com.example.testcommon.fixture.randomInt
import com.example.words.data.fixture.randomDbSheet
import com.example.words.data.utility.createTestDatabase
import com.example.words.database.DbWidget
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DbWidgetDeleteLastReferencedSheetTriggerTest {

    @Test
    fun `when widget is deleted_given the related sheet is referenced by any widget_then referenced sheet is not deleted`() {
        val database = createTestDatabase()
        val dbWidgetId = randomInt()
        val dbSheet = randomDbSheet()
        database.dbSheetQueries.insert(dbSheet)
        database.dbWidgetQueries.insert(DbWidget(id = dbWidgetId, sheet_id = 1))

        database.dbWidgetQueries.delete(dbWidgetId)

        assertTrue(database.dbSheetQueries.getAll().executeAsList().isEmpty())
    }

    @Test
    fun `when widget is deleted_given the related sheet is not referenced by any widget_then referenced sheet is deleted`() {
        val database = createTestDatabase()
        val dbWidgetId = randomInt()
        val dbSheet = randomDbSheet()
        database.dbSheetQueries.insert(dbSheet)
        database.dbWidgetQueries.insert(DbWidget(id = dbWidgetId, sheet_id = 1))
        database.dbWidgetQueries.insert(DbWidget(id = randomInt(), sheet_id = 1))

        database.dbWidgetQueries.delete(dbWidgetId)

        assertFalse(database.dbSheetQueries.getAll().executeAsList().isEmpty())
    }

    @Test
    fun `when widget is deleted_given the related sheet is not referenced by any widget_then sheets other than referenced one are not deleted`() {
        val database = createTestDatabase()
        val dbWidgetId = randomInt()
        val dbSheet = randomDbSheet()
        val dbSheetOther = randomDbSheet()
        database.dbSheetQueries.insert(dbSheet)
        database.dbSheetQueries.insert(dbSheetOther)
        database.dbWidgetQueries.insert(DbWidget(id = dbWidgetId, sheet_id = 1))

        database.dbWidgetQueries.delete(dbWidgetId)

        assertTrue(database.dbSheetQueries.getAll().executeAsList().size == 1)
    }
}