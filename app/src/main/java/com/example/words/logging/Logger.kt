package com.example.words.logging

import android.util.Log

interface Logger {

    fun e(tag: String?, message: String?, throwable: Throwable?)
}

class DefaultLogger: Logger {
    override fun e(tag: String?, message: String?, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }
}

fun Logger.e(tag: String?, throwable: Throwable?) = e(tag, null, throwable)