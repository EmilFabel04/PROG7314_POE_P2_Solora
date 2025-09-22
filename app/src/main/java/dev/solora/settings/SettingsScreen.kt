package dev.solora.settings

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.solora.R
import dev.solora.i18n.I18n
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreenContent() {
    val ctx = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(I18n.Lang.EN) }
    val labels = mapOf(
        I18n.Lang.EN to stringResource(id = R.string.english),
        I18n.Lang.AF to stringResource(id = R.string.afrikaans),
        I18n.Lang.XH to stringResource(id = R.string.xhosa)
    )

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(id = R.string.language))
        Box {
            TextField(
                value = labels[selected] ?: "",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.clickable { expanded = true }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                I18n.Lang.values().forEach { lang ->
                    DropdownMenuItem(text = { Text(labels[lang] ?: lang.name) }, onClick = {
                        expanded = false
                        selected = lang
                        CoroutineScope(Dispatchers.IO).launch { LangStore.save(ctx, lang) }
                        applyLanguage(ctx, lang)
                    })
                }
            }
        }
    }
}

private fun applyLanguage(context: Context, lang: I18n.Lang) {
    I18n.wrap(context, lang)
    (context as? Activity)?.recreate()
}


