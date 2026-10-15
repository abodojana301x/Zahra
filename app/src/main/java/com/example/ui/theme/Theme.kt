package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ChatGPTLightColorScheme = lightColorScheme(
  primary = GptAccentBlue,
  onPrimary = Color.White,
  primaryContainer = GptPlusPillBgLight,
  onPrimaryContainer = GptPlusPillTextLight,
  secondary = GptAccentBlue,
  onSecondary = Color.White,
  background = GptLightBg,
  onBackground = GptLightTextPrimary,
  surface = GptLightSurface,
  onSurface = GptLightTextPrimary,
  surfaceVariant = GptLightInputCapsule,
  onSurfaceVariant = GptLightTextSecondary,
  outline = GptLightBorder,
  outlineVariant = GptLightDivider
)

private val ChatGPTDarkColorScheme = darkColorScheme(
  primary = GptAccentBlue,
  onPrimary = Color.White,
  primaryContainer = GptPlusPillBgDark,
  onPrimaryContainer = GptPlusPillTextDark,
  secondary = GptAccentBlue,
  onSecondary = Color.White,
  background = GptDarkBg,
  onBackground = GptDarkTextPrimary,
  surface = GptDarkSurface,
  onSurface = GptDarkTextPrimary,
  surfaceVariant = GptDarkInputCapsule,
  onSurfaceVariant = GptDarkTextSecondary,
  outline = GptDarkBorder,
  outlineVariant = GptDarkDivider
)

@Composable
fun ZahraTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val colors = if (darkTheme) ChatGPTDarkColorScheme else ChatGPTLightColorScheme

  MaterialTheme(
    colorScheme = colors,
    typography = Typography,
    content = content
  )
}
