package com.macsense.ai.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dawDataStore by preferencesDataStore(name = "daw_prefs")

/**
 * Thin DataStore wrapper that persists lightweight DAW UI preferences across app restarts.
 * Currently only backs [com.macsense.ai.ui.viewmodel.ViewMode], but is the natural home for
 * any future "remember last choice" style settings (e.g. last active global mode tab).
 */
class DawPreferences(private val context: Context) {

    /** Emits the persisted view mode name ("VERTICAL" / "HORIZONTAL"), or null if never set. */
    val viewModeFlow: Flow<String?> = context.dawDataStore.data.map { prefs ->
        prefs[VIEW_MODE_KEY]
    }

    suspend fun setViewMode(mode: String) {
        context.dawDataStore.edit { prefs ->
            prefs[VIEW_MODE_KEY] = mode
        }
    }

    companion object {
        private val VIEW_MODE_KEY = stringPreferencesKey("view_mode")
    }
}
