package com.hanghub.app.ui.theme

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanghub.app.R

// ═══════════════════════════════════════════════════════════════════════════
// MARK: Vibe System — matches tokens.jsx VIBES exactly
// ═══════════════════════════════════════════════════════════════════════════

enum class HangVibe(
    val label: String,
    val emoji: String,
    val bgLight: Color,
    val inkLight: Color,
    val bgDark: Color,
    val inkDark: Color,
) {
    COZY(
        "Cozy", "☕",
        Color(0xFFF5E6D3), Color(0xFF7A4A1E),
        Color(0xFF3A2A1E), Color(0xFFF5D7B4)
    ),
    ROMANTIC(
        "Romantic", "💕",
        Color(0xFFFADBE4), Color(0xFF8A1F4A),
        Color(0xFF3E1E2E), Color(0xFFF7B8CF)
    ),
    ADVENTUROUS(
        "Adventurous", "🗻",
        Color(0xFFD6EEDF), Color(0xFF1C5A3A),
        Color(0xFF1E3029), Color(0xFF9CE0BA)
    ),
    FORMAL(
        "Formal", "🎩",
        Color(0xFFDEE1EE), Color(0xFF2C334D),
        Color(0xFF1C1F2C), Color(0xFFB6BEDA)
    ),
    FRIENDLY(
        "Friendly", "👋",
        Color(0xFFFBE8CB), Color(0xFF8A5510),
        Color(0xFF3A2C19), Color(0xFFF3CB8A)
    ),
    CHILL(
        "Chill", "😌",
        Color(0xFFD8E9F1), Color(0xFF194A5E),
        Color(0xFF1E2E38), Color(0xFF9AC6D9)
    ),
    PARTY(
        "Party", "🎉",
        Color(0xFFF0D9F3), Color(0xFF6D1E72),
        Color(0xFF38213A), Color(0xFFE4A8EC)
    ),
    CULTURAL(
        "Cultural", "🎭",
        Color(0xFFEDE5CC), Color(0xFF5A4A14),
        Color(0xFF2F2A1C), Color(0xFFD8C780)
    );

    fun bg(dark: Boolean) = if (dark) bgDark else bgLight
    fun ink(dark: Boolean) = if (dark) inkDark else inkLight
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: Status
// ═══════════════════════════════════════════════════════════════════════════

enum class UserStatus(val label: String) {
    FREE("free now"),
    BUSY("busy now"),
    OFFLINE("offline");

    fun color(dark: Boolean): Color = when (this) {
        FREE    -> if (dark) Color(0xFF5CE592) else Color(0xFF1EAE5E)
        BUSY    -> if (dark) Color(0xFFFFB347) else Color(0xFFE07A14)
        OFFLINE -> if (dark) Color(0xFF6E6978) else Color(0xFF9C93A8)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: Color Palette — light + dark
// ═══════════════════════════════════════════════════════════════════════════

data class HHColors(
    val accent: Color,
    val accentSoft: Color,
    val accentInk: Color,
    val onAccent: Color,
    val bg: Color,
    val bgElev: Color,
    val surface: Color,
    val surfaceHi: Color,
    val stroke: Color,
    val strokeHi: Color,
    val ink: Color,
    val inkMute: Color,
    val inkDim: Color,
    val statusFree: Color,
    val statusBusy: Color,
    val statusOffline: Color,
    val isDark: Boolean,
)

private val LightColors = HHColors(
    accent       = Color(0xFF7A3FBD),
    accentSoft   = Color(0xFFF1E8FA),
    accentInk    = Color(0xFF4A1F82),
    onAccent     = Color(0xFFFFFFFF),
    bg           = Color(0xFFF6F1E8),
    bgElev       = Color(0xFFFBF7EE),
    surface      = Color(0xFFFFFFFF),
    surfaceHi    = Color(0xFFFFFDF7),
    stroke       = Color(0x141E142D),
    strokeHi     = Color(0x241E142D),
    ink          = Color(0xFF1E142D),
    inkMute      = Color(0x9E1E142D),
    inkDim       = Color(0x611E142D),
    statusFree   = Color(0xFF1EAE5E),
    statusBusy   = Color(0xFFE07A14),
    statusOffline= Color(0xFF9C93A8),
    isDark       = false,
)

private val DarkColors = HHColors(
    accent       = Color(0xFFA97EE0),
    accentSoft   = Color(0xFF3A1F5A),
    accentInk    = Color(0xFFDBBEF7),
    onAccent     = Color(0xFF15101B),
    bg           = Color(0xFF141019),
    bgElev       = Color(0xFF1C1823),
    surface      = Color(0xFF252030),
    surfaceHi    = Color(0xFF2E2838),
    stroke       = Color(0x14FFFFFF),
    strokeHi     = Color(0x24FFFFFF),
    ink          = Color(0xFFF4EFE8),
    inkMute      = Color(0xA8F4EFE8),
    inkDim       = Color(0x6BF4EFE8),
    statusFree   = Color(0xFF5CE592),
    statusBusy   = Color(0xFFFFB347),
    statusOffline= Color(0xFF6E6978),
    isDark       = true,
)

// ═══════════════════════════════════════════════════════════════════════════
// MARK: Radii
// ═══════════════════════════════════════════════════════════════════════════

object HHRadius {
    val xs:   Dp = 8.dp
    val sm:   Dp = 12.dp
    val md:   Dp = 16.dp
    val lg:   Dp = 22.dp
    val xl:   Dp = 28.dp
    val pill: Dp = 999.dp
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: Spacing
// ═══════════════════════════════════════════════════════════════════════════

object HHSpacing {
    val tight: Dp = 6.dp
    val xs:    Dp = 8.dp
    val sm:    Dp = 12.dp
    val md:    Dp = 16.dp
    val lg:    Dp = 22.dp
    val xl:    Dp = 28.dp
    val xxl:   Dp = 40.dp
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: Typography — Fraunces display + system body + mono
// ═══════════════════════════════════════════════════════════════════════════

val FrauncesFamily = FontFamily(
    Font(R.font.fraunces_72pt_semibold, weight = FontWeight.SemiBold),
    Font(R.font.fraunces_72pt_bold, weight = FontWeight.Bold),
    Font(R.font.fraunces_72pt_semibolditalic, weight = FontWeight.SemiBold, style = FontStyle.Italic),
)

object HHType {
    fun display(size: Int = 30) = TextStyle(
        fontFamily  = FrauncesFamily,
        fontWeight  = FontWeight.SemiBold,
        fontSize    = size.sp,
        letterSpacing = (-0.02).sp,
        lineHeight  = (size * 1.1).sp,
    )
    val title   = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = (-0.4).sp)
    val body    = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp)
    val bodyMd  = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    val bodySm  = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp)
    val caption = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp)
    val mono    = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
    val monoSm  = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
    val monoXs  = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp,  fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: Motion
// ═══════════════════════════════════════════════════════════════════════════

object HHMotion {
    fun <T> subtle(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.84f, stiffness = 400f)

    fun <T> expressive(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.72f, stiffness = 260f)

    val subtleDp   = spring<Dp>(dampingRatio = 0.84f, stiffness = 400f)
    val dismiss    = tween<Float>(durationMillis = 240, easing = FastOutLinearInEasing)
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: CompositionLocal — provides HHColors down the tree
// ═══════════════════════════════════════════════════════════════════════════

val LocalHHColors = staticCompositionLocalOf { LightColors }

@Composable
fun HangHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val hhColors = if (darkTheme) DarkColors else LightColors

    // wire into Material3 so system components adapt too
    val m3Colors = if (darkTheme) darkColorScheme(
        primary   = hhColors.accent,
        onPrimary = hhColors.onAccent,
        background= hhColors.bg,
        surface   = hhColors.surface,
    ) else lightColorScheme(
        primary   = hhColors.accent,
        onPrimary = hhColors.onAccent,
        background= hhColors.bg,
        surface   = hhColors.surface,
    )

    CompositionLocalProvider(LocalHHColors provides hhColors) {
        MaterialTheme(colorScheme = m3Colors, content = content)
    }
}

// Convenience accessor
val hh: HHColors
    @Composable get() = LocalHHColors.current
