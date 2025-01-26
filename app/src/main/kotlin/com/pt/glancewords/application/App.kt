package com.pt.glancewords.application

import android.app.Application
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.pt.glancewords.data.di.dataModule
import com.pt.glancewords.di.appModule
import com.pt.glancewords.notification.NotificationChannel
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