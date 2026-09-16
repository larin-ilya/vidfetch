package app.vidfetch.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// VidFetch brand accent (indigo) kept, softened with Takram-like warm neutrals.
val Accent = Color(0xFF5B5FE9)
val AccentSoft = Color(0xFF6F74FF)
val Ok = Color(0xFF3DA987)
val Warn = Color(0xFFD9A441)
val Err = Color(0xFFD65C5C)

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFECECFB),
    onPrimaryContainer = Color(0xFF2A2B62),
    secondary = Color(0xFF6E6B63),
    onSecondary = Color.White,
    background = Color(0xFFF6F5F2),          // soft warm gray (Takram calm base)
    onBackground = Color(0xFF20201E),
    surface = Color(0xFFFFFEFB),
    onSurface = Color(0xFF20201E),
    surfaceVariant = Color(0xFFEFEDE7),
    onSurfaceVariant = Color(0xFF6E6B63),
    outline = Color(0xFFE2DFD7),
    outlineVariant = Color(0xFFECE9E2),
    error = Err,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = AccentSoft,
    onPrimary = Color(0xFF14152B),
    primaryContainer = Color(0xFF2C2E4D),
    onPrimaryContainer = Color(0xFFDDE0FF),
    secondary = Color(0xFFA6A39A),
    onSecondary = Color(0xFF1A1A18),
    background = Color(0xFF16161A),
    onBackground = Color(0xFFECEAE4),
    surface = Color(0xFF1E1E22),
    onSurface = Color(0xFFECEAE4),
    surfaceVariant = Color(0xFF2A2A2F),
    onSurfaceVariant = Color(0xFFA6A39A),
    outline = Color(0xFF34343A),
    outlineVariant = Color(0xFF2C2C31),
    error = Color(0xFFFF8A8A),
    onError = Color(0xFF2A0A0B),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun VidFetchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
