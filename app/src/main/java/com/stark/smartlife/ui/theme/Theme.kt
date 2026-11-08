package com.stark.smartlife.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.stark.smartlife.data.ThemeMode

val IronRed = Color(0xFFB3261E)
val IronRedBright = Color(0xFFFF3B30)
val StarkGold = Color(0xFFD4AF37)
val ReactorBlue = Color(0xFF38BDF8)
val ReactorCore = Color(0xFFE0F7FF)

/** Extra palette beyond Material 3 roles, for HUD drawing. */
data class HudColors(
    val reactor: Color,
    val gold: Color,
    val red: Color,
    val line: Color,
    val panel: Brush,
    val background: Brush,
)

val LocalHud = staticCompositionLocalOf {
    HudColors(ReactorBlue, StarkGold, IronRed, Color.White, Brush.verticalGradient(listOf(Color.Black, Color.Black)),
        Brush.verticalGradient(listOf(Color.Black, Color.Black)))
}

private val darkScheme = darkColorScheme(
    primary = IronRedBright, onPrimary = Color.White,
    secondary = StarkGold, onSecondary = Color.Black,
    tertiary = ReactorBlue, onTertiary = Color.Black,
    background = Color(0xFF0A0B0E), onBackground = Color(0xFFE8EAED),
    surface = Color(0xFF14161B), onSurface = Color(0xFFE8EAED),
    surfaceVariant = Color(0xFF1E2128), onSurfaceVariant = Color(0xFFB0B5BD),
    outline = Color(0xFF3A3F49),
)

private val starkScheme = darkColorScheme(
    primary = IronRedBright, onPrimary = Color.White,
    secondary = StarkGold, onSecondary = Color.Black,
    tertiary = ReactorBlue, onTertiary = Color.Black,
    background = Color(0xFF050608), onBackground = Color(0xFFF2F4F7),
    surface = Color(0xFF0E1015), onSurface = Color(0xFFF2F4F7),
    surfaceVariant = Color(0xFF181B22), onSurfaceVariant = Color(0xFFC0C5CD),
    outline = StarkGold.copy(alpha = 0.55f),
)

private val lightScheme = lightColorScheme(
    primary = IronRed, onPrimary = Color.White,
    secondary = Color(0xFF8A6D0B), onSecondary = Color.White,
    tertiary = Color(0xFF0369A1), onTertiary = Color.White,
    background = Color(0xFFF4F4F6), onBackground = Color(0xFF14161B),
    surface = Color.White, onSurface = Color(0xFF14161B),
    surfaceVariant = Color(0xFFE6E7EB), onSurfaceVariant = Color(0xFF4A4F59),
    outline = Color(0xFFB8BCC4),
)

private val starkTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = 1.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = 0.8.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, letterSpacing = 1.2.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 0.8.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 1.5.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 1.4.sp),
)

@Composable
fun StarkTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        else -> true
    }
    val scheme = when {
        mode == ThemeMode.STARK -> starkScheme
        dark -> darkScheme
        else -> lightScheme
    }
    val hud = if (dark) {
        HudColors(
            reactor = ReactorBlue, gold = StarkGold, red = IronRedBright,
            line = if (mode == ThemeMode.STARK) StarkGold.copy(alpha = 0.6f) else Color(0xFF3A3F49),
            panel = Brush.verticalGradient(listOf(Color(0xFF1B1E25), Color(0xFF101217))),
            background = Brush.verticalGradient(
                if (mode == ThemeMode.STARK) listOf(Color(0xFF14090B), Color(0xFF050608), Color(0xFF05101A))
                else listOf(Color(0xFF16181D), Color(0xFF0A0B0E)),
            ),
        )
    } else {
        HudColors(
            reactor = Color(0xFF0369A1), gold = Color(0xFF8A6D0B), red = IronRed, line = Color(0xFFB8BCC4),
            panel = Brush.verticalGradient(listOf(Color.White, Color(0xFFF0F1F4))),
            background = Brush.verticalGradient(listOf(Color(0xFFF8F8FA), Color(0xFFE9EAEE))),
        )
    }
    CompositionLocalProvider(LocalHud provides hud) {
        MaterialTheme(colorScheme = scheme, typography = starkTypography, content = content)
    }
}
