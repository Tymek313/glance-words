package com.example.words.database.schema

import com.example.words.database.DbWidget
import com.example.words.database.utility.createTestDatabase
import com.example.words.randomDbSheet
import com.example.words.randomInt
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
        val dbSheetId = database.dbSheetQueries.getLastId().executeAsOne().toInt()
        database.dbWidgetQueries.insert(DbWidget(id = dbWidgetId, sheet_id = dbSheetId))

        database.dbWidgetQueries.delete(dbWidgetId)

        assertTrue(database.dbSheetQueries.getAll().executeAsList().isEmpty())
    }

    @Test
    fun `when widget is deleted_given the related sheet is not referenced by any widget_then referenced sheet is deleted`() {
        val database = createTestDatabase()
        val dbWidgetId = randomInt()
        val dbSheet = randomDbSheet()
        database.dbSheetQueries.insert(dbSheet)
        val dbSheetId = database.dbSheetQueries.getLastId().executeAsOne().toInt()
        database.dbWidgetQueries.insert(DbWidget(id = dbWidgetId, sheet_id = dbSheetId))
        database.dbWidgetQueries.insert(DbWidget(id = randomInt(), sheet_id = dbSheetId))

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
        val dbSheetId = database.dbSheetQueries.getLastId().executeAsOne().toInt()
        database.dbSheetQueries.insert(dbSheetOther)
        database.dbWidgetQueries.insert(DbWidget(id = dbWidgetId, sheet_id = dbSheetId))

        database.dbWidgetQueries.delete(dbWidgetId)

        assertTrue(database.dbSheetQueries.getAll().executeAsList().size == 1)
    }
}