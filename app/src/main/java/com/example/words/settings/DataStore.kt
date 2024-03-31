package com.example.words.settings

import android.content.Context
import androidx.datastore.dataStore

val Context.settingsSataStore by dataStore("settings.pb", SettingsSerializer)