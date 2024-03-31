package com.example.words.settings

import android.content.Context
import androidx.datastore.dataStore

val Context.settingsDataStore by dataStore("settings.pb", SettingsSerializer)