package com.pt.glancewords.notification

import androidx.annotation.StringRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.pt.glancewords.R

enum class NotificationChannel(val id: String, @StringRes private val nameResId: Int) {
    WIDGET_SYNCHRONIZATION(id = "SynchronizeWordsWorker", nameResId = R.string.widget_synchronization);

    fun createAndroidChannel(getString: (nameResId: Int) -> String): NotificationChannelCompat {
        return NotificationChannelCompat.Builder(id, NotificationManagerCompat.IMPORTANCE_NONE)
            .setName(getString(nameResId))
            .build()
    }
}
