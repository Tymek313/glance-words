package com.example.words.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.words.logging.Logger
import com.example.words.synchronization.WordsSynchronizer

class WorkFactory(
    private val wordsSynchronizer: WordsSynchronizer,
    private val logger: Logger,
) : WorkerFactory() {
    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? {
        val workerClass = Class.forName(workerClassName)
        return if (workerClass.isAssignableFrom(SynchronizeWordsWorker::class.java)) {
            SynchronizeWordsWorker(appContext, workerParameters, wordsSynchronizer, logger)
        } else null
    }
}