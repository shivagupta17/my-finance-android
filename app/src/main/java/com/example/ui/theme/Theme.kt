package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val ObsidianDarkColorScheme = darkColorScheme(
  primary = ObsidianDarkTokens.AccentMint,
  onPrimary = Color(0xFF003822),
  primaryContainer = Color(0xFF004D30),
  onPrimaryContainer = ObsidianDarkTokens.AccentMint,
  secondary = ObsidianDarkTokens.AccentBlue,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFF1E283C),
  onSecondaryContainer = Color(0xFFD6E3FF),
  tertiary = ObsidianDarkTokens.AccentViolet,
  onTertiary = Color.White,
  background = ObsidianDarkTokens.Canvas,
  onBackground = ObsidianDarkTokens.TextPrimary,
  surface = ObsidianDarkTokens.GlassCard,
  onSurface = ObsidianDarkTokens.TextPrimary,
  surfaceVariant = ObsidianDarkTokens.GlassSurface,
  onSurfaceVariant = ObsidianDarkTokens.TextSecondary,
  outline = ObsidianDarkTokens.Outline,
  outlineVariant = ObsidianDarkTokens.OutlineVariant,
  error = ObsidianDarkTokens.AccentCrimson,
  onError = Color.White,
  errorContainer = Color(0xFF5A1020),
  onErrorContainer = Color(0xFFFFD6DB)
)

private val ObsidianLightColorScheme = lightColorScheme(
  primary = ObsidianLightTokens.AccentMint,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFD1F5E4),
  onPrimaryContainer = Color(0xFF00452A),
  secondary = ObsidianLightTokens.AccentBlue,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFDBEAFE),
  onSecondaryContainer = Color(0xFF1E3A8A),
  tertiary = ObsidianLightTokens.AccentViolet,
  onTertiary = Color.White,
  background = ObsidianLightTokens.Canvas,
  onBackground = ObsidianLightTokens.TextPrimary,
  surface = ObsidianLightTokens.GlassCard,
  onSurface = ObsidianLightTokens.TextPrimary,
  surfaceVariant = ObsidianLightTokens.GlassSurface,
  onSurfaceVariant = ObsidianLightTokens.TextSecondary,
  outline = ObsidianLightTokens.Outline,
  outlineVariant = ObsidianLightTokens.OutlineVariant,
  error = ObsidianLightTokens.AccentCrimson,
  onError = Color.White,
  errorContainer = Color(0xFFFEE2E2),
  onErrorContainer = Color(0xFF991B1B)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> ObsidianDarkColorScheme
      else -> ObsidianLightColorScheme
    }

  CompositionLocalProvider(LocalThemeIsDark provides darkTheme) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}
