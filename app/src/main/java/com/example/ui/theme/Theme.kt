package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FullDarkColorScheme = darkColorScheme(
    primary = PrimaryAmber,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF332000),
    onPrimaryContainer = Color(0xFFFFD599),
    secondary = PrimaryBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = Color(0xFFBFDBFE),
    tertiary = PrimaryPurple,
    background = DarkBackground, // PITCH BLACK 0xFF000000
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    error = AccentRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = PrimaryAmber,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF92400E),
    tertiary = PrimaryPurple,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = AccentRed,
    onError = Color.White
)

private val CandyColorScheme = lightColorScheme(
    primary = CandyPink,
    onPrimary = Color.White,
    primaryContainer = CandyPastelPink,
    onPrimaryContainer = CandyTextPrimary,
    secondary = CandyBerry,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E8FF),
    onSecondaryContainer = Color(0xFF581C87),
    tertiary = CandyMint,
    background = CandyBackground,
    onBackground = CandyTextPrimary,
    surface = CandySurface,
    onSurface = CandyTextPrimary,
    surfaceVariant = CandySurfaceVariant,
    onSurfaceVariant = CandyTextSecondary,
    outline = CandyBorder,
    error = AccentRed,
    onError = Color.White
)

private val EmeraldColorScheme = lightColorScheme(
    primary = EmeraldGreen,
    onPrimary = Color.White,
    primaryContainer = EmeraldLight,
    onPrimaryContainer = EmeraldDark,
    secondary = LuxeGold,
    onSecondary = Color.Black,
    secondaryContainer = LuxeAmberLight,
    onSecondaryContainer = LuxeDark,
    tertiary = SecondaryTeal,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = AccentRed,
    onError = Color.White
)

private val SunsetColorScheme = lightColorScheme(
    primary = SunsetOrange,
    onPrimary = Color.White,
    primaryContainer = SunsetLight,
    onPrimaryContainer = SunsetDark,
    secondary = PrimaryAmber,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF92400E),
    tertiary = RoseGoldPrimary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = AccentRed,
    onError = Color.White
)

private val CyberpunkColorScheme = darkColorScheme(
    primary = NeonViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B0764),
    onPrimaryContainer = Color(0xFFE9D5FF),
    secondary = NeonCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFA5F3FC),
    tertiary = CandyPink,
    background = NeonBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF13111C),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E1A2E),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF2E2844),
    error = AccentRed,
    onError = Color.White
)

private val RoseGoldColorScheme = lightColorScheme(
    primary = RoseGoldPrimary,
    onPrimary = Color.White,
    primaryContainer = RoseGoldLight,
    onPrimaryContainer = RoseGoldDark,
    secondary = LuxeGold,
    onSecondary = Color.Black,
    tertiary = CandyBerry,
    background = Color(0xFFFFF9FA),
    onBackground = Color(0xFF4C0519),
    surface = Color.White,
    onSurface = Color(0xFF4C0519),
    surfaceVariant = Color(0xFFFFF1F2),
    onSurfaceVariant = Color(0xFF9F1239),
    outline = Color(0xFFFECDD3),
    error = AccentRed,
    onError = Color.White
)

private val CrimsonColorScheme = lightColorScheme(
    primary = CrimsonRed,
    onPrimary = Color.White,
    primaryContainer = CrimsonLight,
    onPrimaryContainer = CrimsonDark,
    secondary = PrimaryAmber,
    onSecondary = Color.Black,
    tertiary = PrimaryPurple,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = AccentRed,
    onError = Color.White
)

private val ForestColorScheme = lightColorScheme(
    primary = ForestMoss,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCFCE7),
    onPrimaryContainer = ForestDark,
    secondary = ForestSand,
    onSecondary = Color(0xFF78350F),
    tertiary = SecondaryTeal,
    background = Color(0xFFF9FAF8),
    onBackground = Color(0xFF142018),
    surface = Color.White,
    onSurface = Color(0xFF142018),
    surfaceVariant = Color(0xFFEDF2EC),
    onSurfaceVariant = Color(0xFF374E3E),
    outline = Color(0xFFD4DFD2),
    error = AccentRed,
    onError = Color.White
)

private val MidnightColorScheme = darkColorScheme(
    primary = MidnightIndigo,
    onPrimary = Color.White,
    primaryContainer = MidnightDark,
    onPrimaryContainer = MidnightLight,
    secondary = NeonCyan,
    onSecondary = Color.Black,
    tertiary = PrimaryAmber,
    background = Color(0xFF0A0A16),
    onBackground = Color(0xFFEEF2FF),
    surface = Color(0xFF111124),
    onSurface = Color(0xFFEEF2FF),
    surfaceVariant = Color(0xFF1C1C36),
    onSurfaceVariant = Color(0xFFA5B4FC),
    outline = Color(0xFF2E2E54),
    error = AccentRed,
    onError = Color.White
)

private val GoldenLuxeColorScheme = darkColorScheme(
    primary = LuxeGold,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF451A03),
    onPrimaryContainer = Color(0xFFFDE68A),
    secondary = PrimaryAmber,
    onSecondary = Color.Black,
    tertiary = SecondaryTeal,
    background = Color(0xFF0C0A09),
    onBackground = Color(0xFFFAFAF9),
    surface = Color(0xFF1C1917),
    onSurface = Color(0xFFFAFAF9),
    surfaceVariant = Color(0xFF292524),
    onSurfaceVariant = Color(0xFFD6D3D1),
    outline = Color(0xFF44403C),
    error = AccentRed,
    onError = Color.White
)

private val LavenderMintColorScheme = lightColorScheme(
    primary = LavenderPrimary,
    onPrimary = Color.White,
    primaryContainer = LavenderLight,
    onPrimaryContainer = Color(0xFF4C1D95),
    secondary = LavenderMintAccent,
    onSecondary = Color.White,
    tertiary = CandyBerry,
    background = Color(0xFFFAF9FF),
    onBackground = Color(0xFF1E1B4B),
    surface = Color.White,
    onSurface = Color(0xFF1E1B4B),
    surfaceVariant = Color(0xFFF3E8FF),
    onSurfaceVariant = Color(0xFF6B21A8),
    outline = Color(0xFFDDD6FE),
    error = AccentRed,
    onError = Color.White
)

private val PureGoldenColorScheme = darkColorScheme(
    primary = Color(0xFFFFD700),
    onPrimary = Color(0xFF1C1300),
    primaryContainer = Color(0xFF4D3800),
    onPrimaryContainer = Color(0xFFFFE082),
    secondary = Color(0xFFFFB300),
    onSecondary = Color.Black,
    tertiary = Color(0xFFFFA000),
    background = Color(0xFF110E08),
    onBackground = Color(0xFFFFF8E7),
    surface = Color(0xFF1C170E),
    onSurface = Color(0xFFFFF8E7),
    surfaceVariant = Color(0xFF2C2417),
    onSurfaceVariant = Color(0xFFFFE8A3),
    outline = Color(0xFF6B531E),
    error = AccentRed,
    onError = Color.White
)

private val CherryColorScheme = darkColorScheme(
    primary = Color(0xFFE91E63),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF560027),
    onPrimaryContainer = Color(0xFFFFD8E4),
    secondary = Color(0xFFC2185B),
    onSecondary = Color.White,
    tertiary = Color(0xFFFF4081),
    background = Color(0xFF13060B),
    onBackground = Color(0xFFFFF0F5),
    surface = Color(0xFF220B15),
    onSurface = Color(0xFFFFF0F5),
    surfaceVariant = Color(0xFF331321),
    onSurfaceVariant = Color(0xFFFFB2D1),
    outline = Color(0xFF662243),
    error = AccentRed,
    onError = Color.White
)

enum class AppThemeMode(val displayName: String) {
    AUTO_DAILY("Daily Auto-Theme 🔄 (Changes Daily)"),
    LIGHT("Classic Blue (Ocean)"),
    DARK("AMOLED Pitch Black 🌙"),
    GOLDEN("Royal Pure Golden ⚜️ (New)"),
    CHERRY("Cherry Blossom & Ruby 🍒 (New)"),
    CANDY("Sweet Candy Pop 🍭"),
    ROYAL_EMERALD("Royal Emerald Luxe 🌿"),
    SUNSET_ORANGE("Sunset Amber Warm 🌅"),
    NEON_CYBERPUNK("Neon Cyberpunk ⚡"),
    ROSE_GOLD("Rose Gold Luxury ✨"),
    DEEP_CRIMSON("Deep Ruby Crimson 🍷"),
    FOREST_EARTH("Forest Earth Organic 🍃"),
    MIDNIGHT_INDIGO("Midnight Indigo 🌌"),
    GOLDEN_LUXE("Golden Luxe Onyx 👑"),
    LAVENDER_MINT("Lavender & Fresh Mint 💜")
}

fun getAutoDailyTheme(): AppThemeMode {
    val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
    val pool = listOf(
        AppThemeMode.GOLDEN,
        AppThemeMode.CHERRY,
        AppThemeMode.LIGHT,
        AppThemeMode.ROYAL_EMERALD,
        AppThemeMode.SUNSET_ORANGE,
        AppThemeMode.NEON_CYBERPUNK,
        AppThemeMode.ROSE_GOLD,
        AppThemeMode.DEEP_CRIMSON,
        AppThemeMode.FOREST_EARTH,
        AppThemeMode.MIDNIGHT_INDIGO,
        AppThemeMode.GOLDEN_LUXE,
        AppThemeMode.LAVENDER_MINT,
        AppThemeMode.CANDY,
        AppThemeMode.DARK
    )
    return pool[dayOfYear % pool.size]
}

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.AUTO_DAILY,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val effectiveMode = if (themeMode == AppThemeMode.AUTO_DAILY) getAutoDailyTheme() else themeMode
    val colorScheme = when (effectiveMode) {
        AppThemeMode.GOLDEN -> PureGoldenColorScheme
        AppThemeMode.CHERRY -> CherryColorScheme
        AppThemeMode.CANDY -> CandyColorScheme
        AppThemeMode.DARK -> FullDarkColorScheme
        AppThemeMode.LIGHT -> if (darkTheme) FullDarkColorScheme else LightColorScheme
        AppThemeMode.ROYAL_EMERALD -> EmeraldColorScheme
        AppThemeMode.SUNSET_ORANGE -> SunsetColorScheme
        AppThemeMode.NEON_CYBERPUNK -> CyberpunkColorScheme
        AppThemeMode.ROSE_GOLD -> RoseGoldColorScheme
        AppThemeMode.DEEP_CRIMSON -> CrimsonColorScheme
        AppThemeMode.FOREST_EARTH -> ForestColorScheme
        AppThemeMode.MIDNIGHT_INDIGO -> MidnightColorScheme
        AppThemeMode.GOLDEN_LUXE -> GoldenLuxeColorScheme
        AppThemeMode.LAVENDER_MINT -> LavenderMintColorScheme
        AppThemeMode.AUTO_DAILY -> PureGoldenColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
