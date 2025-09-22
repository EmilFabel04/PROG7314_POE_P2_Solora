package dev.solora.settings

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.solora.R
import dev.solora.i18n.I18n
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreenContent() {
    val ctx = LocalContext.current
    val persistedLang by LangStore.flow(ctx).collectAsState(initial = I18n.Lang.EN)
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(persistedLang) }
    var pushEnabled by remember { mutableStateOf(true) }
    var summaryEmail by remember { mutableStateOf(true) }

    LaunchedEffect(persistedLang) { selected = persistedLang }

    val labels = mapOf(
        I18n.Lang.EN to stringResource(id = R.string.english),
        I18n.Lang.AF to stringResource(id = R.string.afrikaans),
        I18n.Lang.XH to stringResource(id = R.string.xhosa)
    )

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Notifications", fontWeight = FontWeight.SemiBold)
                SettingsToggle(title = "Push notifications", checked = pushEnabled, onCheckedChange = { pushEnabled = it })
                SettingsToggle(title = "Summary emails", checked = summaryEmail, onCheckedChange = { summaryEmail = it })
            }
        }
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Language", fontWeight = FontWeight.SemiBold)
                TextField(
                    value = labels[selected] ?: "",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { expanded = true }) {
                            Icon(painterResource(id = R.drawable.solora_logo), contentDescription = null)
                        }
                    }
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
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("About", fontWeight = FontWeight.SemiBold)
                ListItem(headlineContent = { Text("Version") }, supportingContent = { Text("1.0.0") })
                Divider()
                ListItem(headlineContent = { Text("Support") }, supportingContent = { Text("support@solora.co.za") })
            }
        }
    }
}

@Composable
private fun SettingsToggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun applyLanguage(context: Context, lang: I18n.Lang) {
    I18n.wrap(context, lang)
    (context as? Activity)?.recreate()
}
