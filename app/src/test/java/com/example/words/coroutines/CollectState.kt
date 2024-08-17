package com.example.words.coroutines

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope

fun <T> TestScope.collectToListInBackground(flow: Flow<T>): List<T> {
    val list = ArrayList<T>()
    backgroundScope.launch { flow.toList(list) }
    return list
}