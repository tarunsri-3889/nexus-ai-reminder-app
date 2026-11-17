package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val NexusDarkColorScheme = darkColorScheme(
    primary = ElectricTeal,
    onPrimary = ObsidianNavy,
    primaryContainer = TealContainer,
    onPrimaryContainer = ElectricTeal,
    secondary = SkyCyan,
    onSecondary = ObsidianNavy,
    secondaryContainer = ElevatedNavy,
    onSecondaryContainer = TextPrimary,
    tertiary = AmberSignal,
    onTertiary = ObsidianNavy,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AmberSignal,
    background = ObsidianNavy,
    onBackground = TextPrimary,
    surface = MidnightNavy,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceNavy,
    onSurfaceVariant = TextSecondary,
    outline = BorderNavy,
    outlineVariant = ElevatedNavy,
    error = CoralAlert,
    onError = TextPrimary,
    errorContainer = CoralContainer,
    onErrorContainer = CoralAlert
)

val NexusShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun NexusTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NexusDarkColorScheme,
        typography = NexusTypography,
        shapes = NexusShapes,
        content = content
    )
}
