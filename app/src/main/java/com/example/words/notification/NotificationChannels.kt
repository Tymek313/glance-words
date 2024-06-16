package com.example.words.notification

import androidx.annotation.StringRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.example.glancewords.R

object NotificationChannels {
    val WIDGET_SYNCHRONIZATION = NotificationChannel(id = "SynchronizeWordsWorker", nameResId = R.string.widget_synchronization)

    class NotificationChannel(val id: String, @StringRes private val nameResId: Int) {
        fun createChannel(getString: (nameResId: Int) -> String): NotificationChannelCompat {
            return NotificationChannelCompat.Builder(WIDGET_SYNCHRONIZATION.id, NotificationManagerCompat.IMPORTANCE_NONE)
                .setName(getString(WIDGET_SYNCHRONIZATION.nameResId))
                .build()
        }
    }
}
