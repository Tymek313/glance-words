package com.example.words.application

import android.app.Application
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.words.di.DefaultDependencyContainer
import com.example.words.di.DependencyContainer
import com.example.words.notification.NotificationChannel
import com.example.words.work.WorkFactory

class App(
    private val dependencyContainer: DefaultDependencyContainer = DefaultDependencyContainer()
) : Application(), DependencyContainer by dependencyContainer {

    override fun onCreate() {
        dependencyContainer.initialize(appContext = this)
        createNotificationChannels()
        initializeWorkManager()
        super.onCreate()
    }

    private fun createNotificationChannels() {
        val notificationManager = NotificationManagerCompat.from(this)
        NotificationChannel.entries.forEach { channel ->
            notificationManager.createNotificationChannel(
                channel.createAndroidChannel(::getString)
            )
        }
    }

    private fun initializeWorkManager() {
        val configuration = Configuration.Builder()
            .setWorkerFactory(WorkFactory(dependencyContainer.wordsSynchronizer, dependencyContainer.logger))
            .build()
        WorkManager.initialize(this, configuration)
    }
}

val Context.dependencyContainer
    get() = applicationContext as DependencyContainer