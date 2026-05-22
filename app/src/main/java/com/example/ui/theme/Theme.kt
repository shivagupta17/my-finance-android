package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = Purple80,
  onPrimary = OnPrimaryDark,
  primaryContainer = PrimaryPurple,
  onPrimaryContainer = PrimaryLight,
  secondary = PurpleGrey80,
  tertiary = Pink80,
  background = Color(0xFF141218),
  onBackground = Color(0xFFE6E1E5),
  surface = Color(0xFF1D1B20),
  onSurface = Color(0xFFE6E1E5),
  surfaceVariant = Color(0xFF49454F),
  onSurfaceVariant = Color(0xFFCAC4D0),
  outline = Color(0xFF938F99)
)

private val LightColorScheme = lightColorScheme(
  primary = PrimaryPurple,
  onPrimary = Color.White,
  primaryContainer = PrimaryLight,
  onPrimaryContainer = OnPrimaryDark,
  secondary = PurpleGrey40,
  onSecondary = Color.White,
  tertiary = Pink40,
  onTertiary = Color.White,
  background = BackgroundLight,
  onBackground = TextDark,
  surface = BackgroundLight,
  onSurface = TextDark,
  surfaceVariant = SurfaceVariantLight,
  onSurfaceVariant = TextLightGrey,
  outline = BorderGrey
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is disabled by default to force the Professional Polish Theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
