package com.example.myapplication.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.myapplication.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.PREF_NAME)

class PreferencesRepository(private val context: Context) {
    
    private val selectedModeKey = stringPreferencesKey(Constants.PREF_SELECTED_MODE)
    private val serverIpKey = stringPreferencesKey(Constants.PREF_SERVER_IP)
    private val serverPortKey = intPreferencesKey(Constants.PREF_SERVER_PORT)
    private val backgroundColorKey = intPreferencesKey(Constants.PREF_BACKGROUND_COLOR)
    
    val selectedModeFlow: Flow<Constants.AppMode?> = context.dataStore.data.map { preferences ->
        preferences[selectedModeKey]?.let { Constants.AppMode.valueOf(it) }
    }
    
    suspend fun saveSelectedMode(mode: Constants.AppMode) {
        context.dataStore.edit { preferences ->
            preferences[selectedModeKey] = mode.name
        }
    }
    
    suspend fun getSelectedMode(): Constants.AppMode? {
        return context.dataStore.data.map { preferences ->
            preferences[selectedModeKey]?.let { Constants.AppMode.valueOf(it) }
        }.first()
    }
    
    val serverIpFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[serverIpKey]
    }
    
    suspend fun saveServerIp(ip: String) {
        context.dataStore.edit { preferences ->
            preferences[serverIpKey] = ip
        }
    }
    
    suspend fun getServerIp(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[serverIpKey]
        }.first()
    }
    
    suspend fun saveServerPort(port: Int) {
        context.dataStore.edit { preferences ->
            preferences[serverPortKey] = port
        }
    }
    
    suspend fun getServerPort(): Int {
        return context.dataStore.data.map { preferences ->
            preferences[serverPortKey] ?: Constants.DEFAULT_PORT
        }.first() ?: Constants.DEFAULT_PORT
    }
    
    suspend fun saveBackgroundColor(color: Int) {
        context.dataStore.edit { preferences ->
            preferences[backgroundColorKey] = color
        }
    }
    
    suspend fun getBackgroundColor(): Int {
        return context.dataStore.data.map { preferences ->
            preferences[backgroundColorKey] ?: -1
        }.first() ?: -1
    }
    
    suspend fun clear() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
