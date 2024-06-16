package com.example.words.logging

import android.util.Log

interface Logger {

    fun e(tag: String?, message: String?, throwable: Throwable?)
    fun e(tag: String?, message: String)
}

class DefaultLogger: Logger {
    override fun e(tag: String?, message: String?, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }

    override fun e(tag: String?, message: String) {
        Log.e(tag, message)
    }
}

fun Logger.e(tag: String?, throwable: Throwable?) = e(tag, null, throwable)