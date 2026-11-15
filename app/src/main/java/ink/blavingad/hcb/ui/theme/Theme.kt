package ink.blavingad.hcb.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ink.blavingad.hcb.R
import ink.blavingad.hcb.ThemeMode

object HackClub {
    val Red = Color(0xFFEC3750)
    val Green = Color(0xFF33D6A6)
}

/** Fallback (Android < 12): Hack Club red scheme. Android 12+ always uses the phone's wallpaper colours. */
private val BrandLight = lightColorScheme(
    primary = Color(0xFFB91C3B), onPrimary = Color.White, primaryContainer = Color(0xFFFFDADB), onPrimaryContainer = Color(0xFF40000F),
    secondary = Color(0xFF775657), secondaryContainer = Color(0xFFFFDADB), onSecondaryContainer = Color(0xFF2C1516),
    tertiary = Color(0xFF755A2F), tertiaryContainer = Color(0xFFFFDDB1), onTertiaryContainer = Color(0xFF291800),
    surface = Color(0xFFFFF8F7), surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFFF0F0),
    surfaceContainer = Color(0xFFFCEAEA), surfaceContainerHigh = Color(0xFFF6E4E4), surfaceContainerHighest = Color(0xFFF0DEDE),
)
private val BrandDark = darkColorScheme(
    primary = Color(0xFFFFB2B6), onPrimary = Color(0xFF67001D), primaryContainer = Color(0xFF91002C), onPrimaryContainer = Color(0xFFFFDADB),
    secondary = Color(0xFFE6BDBD), secondaryContainer = Color(0xFF5D3F40), onSecondaryContainer = Color(0xFFFFDADB),
    tertiary = Color(0xFFE5C18D), tertiaryContainer = Color(0xFF5B421A), onTertiaryContainer = Color(0xFFFFDDB1),
    surface = Color(0xFF1A1111), surfaceContainerLowest = Color(0xFF140C0C), surfaceContainerLow = Color(0xFF221919),
    surfaceContainer = Color(0xFF271D1D), surfaceContainerHigh = Color(0xFF322828), surfaceContainerHighest = Color(0xFF3D3232),
)

private fun flexFamily(width: Float) = FontFamily(
    (100..1000 step 100).map { w ->
        Font(R.font.roboto_flex, weight = FontWeight(w),
            variationSettings = FontVariation.Settings(FontVariation.weight(w), FontVariation.width(width)))
    },
)
val RobotoFlex = flexFamily(100f)
val RobotoFlexWide = flexFamily(118f)

private val AppTypography: Typography = Typography().let { base ->
    fun TextStyle.flex() = copy(fontFamily = RobotoFlex)
    val t = Typography(
        displayLarge = base.displayLarge.flex(), displayMedium = base.displayMedium.flex(), displaySmall = base.displaySmall.flex(),
        headlineLarge = base.headlineLarge.flex(), headlineMedium = base.headlineMedium.flex(), headlineSmall = base.headlineSmall.flex(),
        titleLarge = base.titleLarge.flex(), titleMedium = base.titleMedium.flex(), titleSmall = base.titleSmall.flex(),
        bodyLarge = base.bodyLarge.flex(), bodyMedium = base.bodyMedium.flex(), bodySmall = base.bodySmall.flex(),
        labelLarge = base.labelLarge.flex(), labelMedium = base.labelMedium.flex(), labelSmall = base.labelSmall.flex(),
    )
    t.copy(
        displaySmall = t.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        displayLargeEmphasized = t.displayLarge.copy(fontFamily = RobotoFlexWide, fontWeight = FontWeight.Black, letterSpacing = (-2).sp),
        displayMediumEmphasized = t.displayMedium.copy(fontFamily = RobotoFlexWide, fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp),
        displaySmallEmphasized = t.displaySmall.copy(fontFamily = RobotoFlexWide, fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
        headlineLargeEmphasized = t.headlineLarge.copy(fontFamily = RobotoFlexWide, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
        headlineMediumEmphasized = t.headlineMedium.copy(fontFamily = RobotoFlexWide, fontWeight = FontWeight.ExtraBold),
        headlineSmallEmphasized = t.headlineSmall.copy(fontFamily = RobotoFlexWide, fontWeight = FontWeight.ExtraBold),
        titleLargeEmphasized = t.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
        titleMediumEmphasized = t.titleMedium.copy(fontWeight = FontWeight.Bold),
        titleSmallEmphasized = t.titleSmall.copy(fontWeight = FontWeight.Bold),
        labelLargeEmphasized = t.labelLarge.copy(fontWeight = FontWeight.Bold),
        bodyLargeEmphasized = t.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
        bodyMediumEmphasized = t.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp), extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun HcbTheme(mode: ThemeMode = ThemeMode.System, amoled: Boolean = false, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.System -> isSystemInDarkTheme(); ThemeMode.Light -> false; ThemeMode.Dark -> true }
    val ctx = LocalContext.current
    val scheme = remember(dark, amoled) {
        val base = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        } else if (dark) BrandDark else BrandLight
        // pure black for OLED: surfaces go to #000, containers stay slightly lifted so cards still read
        if (dark && amoled) base.copy(
            background = androidx.compose.ui.graphics.Color.Black, surface = androidx.compose.ui.graphics.Color.Black,
            surfaceContainerLowest = androidx.compose.ui.graphics.Color.Black,
            surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFF0B0B0C),
            surfaceContainer = androidx.compose.ui.graphics.Color(0xFF111113),
            surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFF19191B),
            surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFF222225),
        ) else base
    }
    MaterialExpressiveTheme(colorScheme = scheme, motionScheme = MotionScheme.expressive(), shapes = AppShapes, typography = AppTypography, content = content)
}
