package com.example.words.database.utility

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.words.database.Database
import com.example.words.database.createDatabase
import java.util.Properties

fun createTestDatabase() = createDatabase(
    JdbcSqliteDriver(
        url = JdbcSqliteDriver.IN_MEMORY,
        schema = Database.Schema,
        properties = Properties().apply { put("foreign_keys", "true") }
    )
)