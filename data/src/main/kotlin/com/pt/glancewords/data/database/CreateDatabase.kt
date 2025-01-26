package com.pt.glancewords.data.database

import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.pt.glancewords.database.Database
import com.pt.glancewords.database.DbSheet
import com.pt.glancewords.database.DbWidget

fun createDatabase(driver: SqlDriver) = Database(
    driver,
    DbSheetAdapter = DbSheet.Adapter(idAdapter = IntColumnAdapter, sheet_idAdapter = IntColumnAdapter),
    DbWidgetAdapter = DbWidget.Adapter(idAdapter = IntColumnAdapter, sheet_idAdapter = IntColumnAdapter)
)