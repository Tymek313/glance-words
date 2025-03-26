package com.pt.glancewords.app.widget

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

interface ReshuffleNotifier {
    val reshuffleEvents: ReceiveChannel<Unit>

    suspend fun emitReshuffle()
}

class DefaultReshuffleNotifier : ReshuffleNotifier {

    private val channel = Channel<Unit>(Channel.CONFLATED)

    override val reshuffleEvents: ReceiveChannel<Unit> = channel

    override suspend fun emitReshuffle() {
        channel.send(Unit)
    }
}
