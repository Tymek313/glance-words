package com.example.words.database

import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.SqlDriver

fun createDatabase(driver: SqlDriver) = Database(
    driver,
    DbSheetAdapter = DbSheet.Adapter(idAdapter = IntColumnAdapter, sheet_idAdapter = IntColumnAdapter),
    DbWidgetAdapter = DbWidget.Adapter(idAdapter = IntColumnAdapter, sheet_idAdapter = IntColumnAdapter)
)