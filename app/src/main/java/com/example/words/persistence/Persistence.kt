package com.example.words.persistence

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow

interface Persistence<T> {
    val data: Flow<T>
    suspend fun updateData(transform: suspend (T) -> T): T
}

class DataStorePersistence<T>(private val dataStore: DataStore<T>): Persistence<T> {
    override val data: Flow<T> = dataStore.data
    override suspend fun updateData(transform: suspend (T) -> T) = dataStore.updateData(transform)
}