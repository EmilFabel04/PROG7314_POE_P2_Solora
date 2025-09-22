package dev.solora.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.solora.i18n.I18n
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.langDataStore by preferencesDataStore(name = "lang")

object LangStore {
    private val KEY = stringPreferencesKey("code")
    fun flow(context: Context): Flow<I18n.Lang> = context.langDataStore.data.map { prefs ->
        when (prefs[KEY]) {
            "af" -> I18n.Lang.AF
            "xh" -> I18n.Lang.XH
            else -> I18n.Lang.EN
        }
    }
    suspend fun save(context: Context, lang: I18n.Lang) { context.langDataStore.edit { it[KEY] = when (lang) { I18n.Lang.AF -> "af"; I18n.Lang.XH -> "xh"; else -> "en" } } }
}


