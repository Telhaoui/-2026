package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CosmicColorScheme = darkColorScheme(
    primary = StarCyan,
    onPrimary = DarkSpaceBackground,
    secondary = MeteorAmber,
    onSecondary = DarkSpaceBackground,
    tertiary = NebulaePurple,
    onTertiary = DarkSpaceBackground,
    background = DarkSpaceBackground,
    surface = SpaceCardBackground,
    onBackground = TextComet,
    onSurface = TextComet,
    surfaceVariant = SpaceCardBackground,
    onSurfaceVariant = TextDust,
    outline = TextDust
)

private val NightObservationColorScheme = darkColorScheme(
    primary = NightActiveRed,
    onPrimary = NightSolidBlack,
    secondary = NightActiveRed,
    onSecondary = NightSolidBlack,
    tertiary = NightMidRed,
    onTertiary = NightSolidBlack,
    background = NightSolidBlack,
    surface = NightDeepRedCard,
    onBackground = NightActiveRed,
    onSurface = NightActiveRed,
    surfaceVariant = NightDeepRedCard,
    onSurfaceVariant = NightOffRedText,
    outline = NightBorderRed
)

@Composable
fun MyApplicationTheme(
    isNightObservationMode: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (isNightObservationMode) {
        NightObservationColorScheme
    } else {
        CosmicColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

