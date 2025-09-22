package dev.solora.settings

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.solora.i18n.I18n
import dev.solora.R

@OptIn(ExperimentalMaterial3Api::class)
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
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            TextField(
                value = labels[selected] ?: "",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                I18n.Lang.values().forEach { lang ->
                    DropdownMenuItem(text = { Text(labels[lang] ?: lang.name) }, onClick = {
                        expanded = false
                        selected = lang
                        applyLanguage(ctx, lang)
                    })
                }
            }
        }
    }
}

private fun applyLanguage(context: Context, lang: I18n.Lang) {
    val wrapped = I18n.wrap(context, lang)
    (context as? Activity)?.recreate()
}


