package com.sk0711.graph.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sk0711.graph.graph.TimeWindow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "sk0711_graph_settings",
)

object AppSettings {

    private val DEFAULT_TIME_WINDOW = stringPreferencesKey("default_time_window")

    fun defaultTimeWindowFlow(context: Context): Flow<TimeWindow> =
        context.applicationContext.settingsDataStore.data.map { prefs ->
            prefs[DEFAULT_TIME_WINDOW]
                ?.let { runCatching { TimeWindow.valueOf(it) }.getOrNull() }
                ?: TimeWindow.FIVE_MIN
        }

    suspend fun setDefaultTimeWindow(context: Context, window: TimeWindow) {
        context.applicationContext.settingsDataStore.edit { it[DEFAULT_TIME_WINDOW] = window.name }
    }
}
