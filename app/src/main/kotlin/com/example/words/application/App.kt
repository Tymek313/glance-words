package com.example.words.application

import android.app.Application
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.example.words.data.di.dataModule
import com.example.words.di.appModule
import com.example.words.notification.NotificationChannel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin

class App : Application(), KoinComponent {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startKoin {
            androidLogger()
            androidContext(this@App)
            workManagerFactory()
            modules(dataModule, appModule)
        }
    }

    private fun createNotificationChannels() {
        val notificationManager = NotificationManagerCompat.from(this)
        NotificationChannel.entries.forEach { channel ->
            notificationManager.createNotificationChannel(
                channel.createAndroidChannel(::getString)
            )
        }
    }
}

val Context.dependencyContainer
    get() = applicationContext as KoinComponent