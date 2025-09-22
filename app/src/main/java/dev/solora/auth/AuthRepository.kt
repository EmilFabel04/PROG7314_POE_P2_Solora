package dev.solora.auth

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "auth")

class AuthRepository(private val context: Context) {
    private val KEY_TOKEN = stringPreferencesKey("token")
    private val KEY_NAME = stringPreferencesKey("name")

    val token: Flow<String?> = context.dataStore.data.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[KEY_TOKEN] }

    suspend fun login(email: String, password: String): Boolean {
        // Stub success: generate fake token
        context.dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = "token_${System.currentTimeMillis()}"
            prefs[KEY_NAME] = email.substringBefore('@')
        }
        return true
    }

    suspend fun register(name: String, email: String, password: String): Boolean {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = "token_${System.currentTimeMillis()}"
            prefs[KEY_NAME] = name
        }
        return true
    }

    suspend fun logout() {
        context.dataStore.edit { it.remove(KEY_TOKEN) }
    }
}


