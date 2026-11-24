package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = DarkFlamePrimary,
    onPrimary = DarkFlameOnPrimary,
    primaryContainer = DarkFlamePrimaryContainer,
    onPrimaryContainer = DarkFlameOnPrimaryContainer,
    secondary = DarkSaffronSecondary,
    onSecondary = DarkSaffronOnSecondary,
    secondaryContainer = DarkSaffronSecondaryContainer,
    onSecondaryContainer = DarkSaffronOnSecondaryContainer,
    tertiary = DarkEmeraldTertiary,
    onTertiary = DarkEmeraldOnTertiary,
    tertiaryContainer = DarkEmeraldTertiaryContainer,
    onTertiaryContainer = DarkEmeraldOnTertiaryContainer,
    background = EspressoBackground,
    onBackground = CreamOnDark,
    surface = EspressoSurface,
    onSurface = CreamOnDark,
    surfaceVariant = EspressoSurfaceVariant,
    onSurfaceVariant = EspressoOnSurfaceVariant,
    outline = EspressoOutline,
    outlineVariant = EspressoOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = FlamePrimary,
    onPrimary = FlameOnPrimary,
    primaryContainer = FlamePrimaryContainer,
    onPrimaryContainer = FlameOnPrimaryContainer,
    secondary = SaffronSecondary,
    onSecondary = SaffronOnSecondary,
    secondaryContainer = SaffronSecondaryContainer,
    onSecondaryContainer = SaffronOnSecondaryContainer,
    tertiary = EmeraldTertiary,
    onTertiary = EmeraldOnTertiary,
    tertiaryContainer = EmeraldTertiaryContainer,
    onTertiaryContainer = EmeraldOnTertiaryContainer,
    background = CreamBackground,
    onBackground = EspressoOnBackground,
    surface = CreamSurface,
    onSurface = EspressoOnBackground,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = WarmOnSurfaceVariant,
    outline = WarmOutline,
    outlineVariant = WarmOutlineVariant,
    error = ErrorRed,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight
)

val FlameShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FlamePosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = FlameShapes,
        content = content
    )
}
