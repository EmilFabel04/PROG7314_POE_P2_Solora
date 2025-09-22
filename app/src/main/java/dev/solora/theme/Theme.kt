package dev.solora.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Orange = Color(0xFFFF7A00)
private val OrangeDark = Color(0xFFE66900)
private val GrayBg = Color(0xFFF7F7F7)

private val LightColors: ColorScheme = lightColorScheme(
    primary = Orange,
    onPrimary = Color.White,
    primaryContainer = OrangeDark,
    onPrimaryContainer = Color.White,
    background = GrayBg,
    surface = Color.White,
    onSurface = Color(0xFF1E1E1E)
)

@Composable
fun SoloraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}


