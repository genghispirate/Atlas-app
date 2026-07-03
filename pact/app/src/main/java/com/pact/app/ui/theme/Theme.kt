package com.pact.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Pact is deliberately dark-only: calm, low-stimulation, consistent.
// The palette is deep indigo with a periwinkle primary and mint success.

val Ink = Color(0xFF0B0E17)
val Surface1 = Color(0xFF131828)
val Surface2 = Color(0xFF1C2337)
val Surface3 = Color(0xFF252E47)
val Periwinkle = Color(0xFF9BB0FF)
val PeriwinkleDim = Color(0xFF5B6CB8)
val Mint = Color(0xFF6FE0B8)
val Amber = Color(0xFFF5C97B)
val Rose = Color(0xFFFF7A85)
val TextPrimary = Color(0xFFE8ECFA)
val TextSecondary = Color(0xFF9AA3BF)
val TextTertiary = Color(0xFF636D8C)

private val PactColors = darkColorScheme(
    primary = Periwinkle,
    onPrimary = Ink,
    primaryContainer = Surface3,
    onPrimaryContainer = TextPrimary,
    secondary = Mint,
    onSecondary = Ink,
    background = Ink,
    onBackground = TextPrimary,
    surface = Surface1,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF39415C),
    error = Rose,
    onError = Ink,
)

private val PactTypography = Typography(
    displaySmall = TextStyle(
        fontSize = 34.sp, lineHeight = 40.sp,
        fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontSize = 26.sp, lineHeight = 32.sp,
        fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp
    ),
    headlineSmall = TextStyle(
        fontSize = 22.sp, lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp
    ),
    titleMedium = TextStyle(
        fontSize = 17.sp, lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontSize = 15.sp, lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal
    ),
    labelLarge = TextStyle(
        fontSize = 15.sp, lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp, lineHeight = 16.sp,
        fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp
    ),
)

private val PactShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun PactTheme(content: @Composable () -> Unit) {
    isSystemInDarkTheme() // dark-only by design; keep the call to document intent
    MaterialTheme(
        colorScheme = PactColors,
        typography = PactTypography,
        shapes = PactShapes,
        content = content,
    )
}
