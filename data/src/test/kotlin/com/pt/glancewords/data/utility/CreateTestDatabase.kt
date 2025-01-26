package com.pt.glancewords.data.utility

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.pt.glancewords.data.database.createDatabase
import com.pt.glancewords.database.Database
import java.util.Properties

fun createTestDatabase() = createDatabase(
    JdbcSqliteDriver(
        url = JdbcSqliteDriver.IN_MEMORY,
        schema = Database.Schema,
        properties = Properties().apply { put("foreign_keys", "true") }
    )
)