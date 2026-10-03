package com.fincahernandez.gestionpecuaria.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GreenLight,
    onPrimary = GreenDark,
    secondary = GreenLight,
    tertiary = AmberAccent,
    onTertiary = Color(0xFF211B00)
)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenContainer,
    onPrimaryContainer = GreenDark,
    secondary = GreenDark,
    tertiary = AmberAccent,
    onTertiary = Color(0xFF211B00),
    background = CreamBackground,
    surface = NeutralSurface,
    surfaceVariant = NeutralVariant,
    outline = NeutralOutline,
    error = ErrorRed
)

@Composable
fun GestionPecuariaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
