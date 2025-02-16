package com.pt.glancewords.logging

interface Logger {

    fun e(tag: String?, message: String?, throwable: Throwable?)
    fun e(tag: String?, message: String)
    fun d(tag: String?, message: String)
}

fun Logger.e(obj: Any, throwable: Throwable?) = e(obj::class.qualifiedName, null, throwable)

fun Logger.e(obj: Any, message: String) = e(obj::class.qualifiedName, message, null)

fun Logger.d(obj: Any, message: String) = d(obj::class.qualifiedName, message)