package com.pt.glancewords.logging

import android.util.Log

class DefaultLogger : Logger {
    override fun e(tag: String?, message: String?, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }

    override fun e(tag: String?, message: String) {
        Log.e(tag, message)
    }

    override fun w(tag: String?, message: String?, throwable: Throwable?) {
        Log.w(tag, message, throwable)
    }

    override fun d(tag: String?, message: String) {
        Log.d(tag, message)
    }
}
