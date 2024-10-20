package com.example.words.logging

import android.util.Log

interface Logger {

    fun e(tag: String?, message: String?, throwable: Throwable?)
    fun e(tag: String?, message: String)
    fun d(tag: String?, message: String)
}

class DefaultLogger: Logger {
    override fun e(tag: String?, message: String?, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }

    override fun e(tag: String?, message: String) {
        Log.e(tag, message)
    }

    override fun d(tag: String?, message: String) {
        Log.d(tag, message)
    }
}

fun Logger.e(obj: Any, throwable: Throwable?) = e(obj::class.qualifiedName, null, throwable)

fun Logger.d(obj: Any, message: String) = d(obj::class.qualifiedName, message)
