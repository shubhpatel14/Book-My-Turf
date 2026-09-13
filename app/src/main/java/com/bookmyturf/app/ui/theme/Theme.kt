package com.bookmyturf.app.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.bookmyturf.app.R

val Ink = Color(0xFF08090B)
val Charcoal = Color(0xFF121418)
val Steel = Color(0xFF1B1F24)
val Line = Color(0xFF2A2E34)
val Lime = Color(0xFFB8F500)
val LimeDark = Color(0xFF7CA600)
val Orange = Color(0xFFFF7A18)
val TextPrimary = Color(0xFFF5F7F2)
val TextMuted = Color(0xFF9CA39A)
val Danger = Color(0xFFFF5C5C)
val PulsePurple = Color(0xFF6C42F5)
val PulsePurpleDark = Color(0xFF32127A)
val PulseLavender = Color(0xFFAE98FF)
val WarmWhite = Color(0xFFFFF8EA)

val AthleticDisplay = FontFamily(Font(R.font.bebas_neue, weight = FontWeight.Normal))
val ManropeUi = FontFamily(
    Font(R.font.manrope, weight = FontWeight.Normal),
    Font(R.font.manrope, weight = FontWeight.Medium),
    Font(R.font.manrope, weight = FontWeight.Bold),
    Font(R.font.manrope, weight = FontWeight.ExtraBold)
)

private val DarkColors = darkColorScheme(
    primary = Lime,
    onPrimary = Ink,
    secondary = Orange,
    background = Ink,
    onBackground = TextPrimary,
    surface = Charcoal,
    onSurface = TextPrimary,
    surfaceVariant = Steel,
    onSurfaceVariant = TextMuted,
    outline = Line,
    error = Danger
)

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = AthleticDisplay, fontWeight = FontWeight.Normal, fontSize = 58.sp, lineHeight = 54.sp, letterSpacing = 1.1.sp),
    displayMedium = TextStyle(fontFamily = AthleticDisplay, fontWeight = FontWeight.Normal, fontSize = 44.sp, lineHeight = 42.sp, letterSpacing = .8.sp),
    headlineLarge = TextStyle(fontFamily = AthleticDisplay, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 36.sp, letterSpacing = .7.sp),
    headlineMedium = TextStyle(fontFamily = AthleticDisplay, fontWeight = FontWeight.Normal, fontSize = 29.sp, lineHeight = 30.sp, letterSpacing = .55.sp),
    titleLarge = TextStyle(fontFamily = AthleticDisplay, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 25.sp, letterSpacing = .5.sp),
    titleMedium = TextStyle(fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp),
    bodyLarge = TextStyle(fontFamily = ManropeUi, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = ManropeUi, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 0.8.sp),
    labelMedium = TextStyle(fontFamily = ManropeUi, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.55.sp)
)

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(12),
    large = androidx.compose.foundation.shape.RoundedCornerShape(16),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(20)
)

@Composable
fun BookMyTurfTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    DisposableEffect(view) {
        if (!view.isInEditMode) {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
        onDispose { }
    }
    MaterialTheme(colorScheme = DarkColors, typography = AppTypography, shapes = AppShapes, content = content)
}
