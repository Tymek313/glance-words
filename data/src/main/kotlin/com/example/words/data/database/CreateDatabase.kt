package com.example.words.data.database

import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.example.words.database.Database
import com.example.words.database.DbSheet
import com.example.words.database.DbWidget

fun createDatabase(driver: SqlDriver) = Database(
    driver,
    DbSheetAdapter = DbSheet.Adapter(idAdapter = IntColumnAdapter, sheet_idAdapter = IntColumnAdapter),
    DbWidgetAdapter = DbWidget.Adapter(idAdapter = IntColumnAdapter, sheet_idAdapter = IntColumnAdapter)
)