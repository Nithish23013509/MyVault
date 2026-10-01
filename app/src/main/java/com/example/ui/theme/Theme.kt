package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VaultDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF003822),
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = Color(0xFFA7F3D0),

    secondary = CyanAccent,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF083344),
    onSecondaryContainer = Color(0xFFCFFAFE),

    tertiary = AmberWarning,
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),

    background = VaultBgDark,
    onBackground = VaultTextPrimary,
    surface = VaultSurfaceDark,
    onSurface = VaultTextPrimary,
    surfaceVariant = VaultSurfaceElevated,
    onSurfaceVariant = VaultTextSecondary,
    outline = VaultBorder,
    error = RoseDanger,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to secure stealth dark theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VaultDarkColorScheme,
        typography = Typography,
        content = content
    )
}
