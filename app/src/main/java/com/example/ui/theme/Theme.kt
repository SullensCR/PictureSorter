package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = SleekDarkPrimaryBlue,
    secondary = SleekDarkBlueBG,
    onSecondary = SleekDarkBlueText,
    tertiary = SleekDarkRedBG,
    onTertiary = SleekDarkRedText,
    background = SleekDarkBackground,
    surface = SleekDarkSurface,
    onBackground = SleekDarkTextPrimary,
    onSurface = SleekDarkTextPrimary,
    surfaceVariant = SleekDarkNavBarBG,
    onSurfaceVariant = SleekDarkTextSecondary,
    outline = SleekDarkNeutralVariant
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SleekPrimaryBlue,
    secondary = SleekBlueLightBG,
    onSecondary = SleekBlueDarkText,
    tertiary = SleekRedLightBG,
    onTertiary = SleekRedDarkText,
    background = SleekBackground,
    surface = SleekSurfaceWhite,
    onBackground = SleekTextPrimary,
    onSurface = SleekTextPrimary,
    surfaceVariant = SleekNavBarBG,
    onSurfaceVariant = SleekTextSecondary,
    outline = SleekNeutralVariant
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic system color by default to preserve the exact Sleek Interface branding
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
