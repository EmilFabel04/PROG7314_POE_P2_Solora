package dev.solora.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.solora.i18n.I18n
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

object LangStore {
    private val KEY_LANG = stringPreferencesKey("lang")

    fun flow(context: Context): Flow<I18n.Lang> =
        context.settingsDataStore.data.map { prefs ->
            prefs[KEY_LANG]?.let { value ->
                runCatching { I18n.Lang.valueOf(value) }.getOrDefault(I18n.Lang.EN)
            } ?: I18n.Lang.EN
        }

    suspend fun save(context: Context, lang: I18n.Lang) {
        context.settingsDataStore.edit { it[KEY_LANG] = lang.name }
    }
}


