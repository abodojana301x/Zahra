package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ZahraDarkColorScheme = darkColorScheme(
  primary = ZahraCyan,
  onPrimary = Color.Black,
  primaryContainer = ZahraViolet,
  onPrimaryContainer = Color.White,
  secondary = ZahraViolet,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFF2E1065),
  onSecondaryContainer = Color(0xFFE9D5FF),
  tertiary = ZahraPink,
  onTertiary = Color.Black,
  background = ZahraBackground,
  onBackground = ZahraTextPrimary,
  surface = ZahraSurface,
  onSurface = ZahraTextPrimary,
  surfaceVariant = ZahraGlassCard,
  onSurfaceVariant = ZahraTextSecondary,
  outline = ZahraGlassBorder
)

@Composable
fun ZahraTheme(
  content: @Composable () -> Unit
) {
  // Always use ZahraDarkColorScheme for true OLED Liquid Glass experience
  MaterialTheme(
    colorScheme = ZahraDarkColorScheme,
    typography = Typography,
    content = content
  )
}
