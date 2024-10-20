package com.example.words.widget

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface ReshuffleNotifier {
    val shouldReshuffle: Flow<Boolean>

    suspend fun emitReshuffle()
}

class DefaultReshuffleNotifier : ReshuffleNotifier {

    private val flow = MutableSharedFlow<Boolean>(replay = 1).apply { tryEmit(false) }

    override val shouldReshuffle: Flow<Boolean> = flow.asSharedFlow()

    override suspend fun emitReshuffle() {
        flow.emit(true)
        flow.emit(false)
    }
}