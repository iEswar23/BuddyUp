package io.github.ieswar23.buddyup.ui.theme

import androidx.compose.ui.graphics.Color

// Brand
val Coral = Color(0xFFFF7A59)
val CoralDeep = Color(0xFFE2583B)
val Peach = Color(0xFFFFD8C7)
val PeachLight = Color(0xFFFFEDE5)
val Teal = Color(0xFF14A79D)
val TealDeep = Color(0xFF0B7D75)
val Mint = Color(0xFFB8F0E9)
val Sunshine = Color(0xFFFFC857)
val OnlineGreen = Color(0xFF2BC48A)

// Light scheme
val LightPrimary = Color(0xFFD9573A)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFFFDBCF)
val LightOnPrimaryContainer = Color(0xFF3B0C00)
val LightSecondary = Color(0xFF0E8A82)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFBDF2EA)
val LightOnSecondaryContainer = Color(0xFF00201D)
val LightTertiary = Color(0xFF8A5A00)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFFDDB0)
val LightOnTertiaryContainer = Color(0xFF2C1700)
val LightBackground = Color(0xFFFFF8F5)
val LightOnBackground = Color(0xFF231917)
val LightSurface = Color(0xFFFFF8F5)
val LightOnSurface = Color(0xFF231917)
val LightSurfaceVariant = Color(0xFFF5DED6)
val LightOnSurfaceVariant = Color(0xFF53433F)
val LightOutline = Color(0xFF85736E)
val LightOutlineVariant = Color(0xFFD8C2BB)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFFFF1EC)
val LightSurfaceContainer = Color(0xFFFCEAE4)
val LightSurfaceContainerHigh = Color(0xFFF7E4DE)
val LightSurfaceContainerHighest = Color(0xFFF1DFD9)
val LightError = Color(0xFFBA1A1A)

// Dark scheme
val DarkPrimary = Color(0xFFFF9A7F)
val DarkOnPrimary = Color(0xFF4A1405)
val DarkPrimaryContainer = Color(0xFF7A2A16)
val DarkOnPrimaryContainer = Color(0xFFFFDBCF)
val DarkSecondary = Color(0xFF63D7CB)
val DarkOnSecondary = Color(0xFF003733)
val DarkSecondaryContainer = Color(0xFF00504A)
val DarkOnSecondaryContainer = Color(0xFFA9F2E8)
val DarkTertiary = Color(0xFFFFBA4F)
val DarkOnTertiary = Color(0xFF4A2800)
val DarkTertiaryContainer = Color(0xFF693C00)
val DarkOnTertiaryContainer = Color(0xFFFFDDB0)
val DarkBackground = Color(0xFF1A1210)
val DarkOnBackground = Color(0xFFF1DFD9)
val DarkSurface = Color(0xFF1A1210)
val DarkOnSurface = Color(0xFFF1DFD9)
val DarkSurfaceVariant = Color(0xFF53433F)
val DarkOnSurfaceVariant = Color(0xFFD8C2BB)
val DarkOutline = Color(0xFFA08C86)
val DarkOutlineVariant = Color(0xFF53433F)
val DarkSurfaceContainerLowest = Color(0xFF140D0B)
val DarkSurfaceContainerLow = Color(0xFF231917)
val DarkSurfaceContainer = Color(0xFF271D1B)
val DarkSurfaceContainerHigh = Color(0xFF322825)
val DarkSurfaceContainerHighest = Color(0xFF3D322F)
val DarkError = Color(0xFFFFB4AB)

/** Gradient pairs used for initials avatars and meetup banners; picked deterministically by id. */
val AvatarGradients: List<Pair<Color, Color>> = listOf(
    Color(0xFFFF9A76) to Color(0xFFF2604A),
    Color(0xFF4FD1C5) to Color(0xFF0E8A82),
    Color(0xFFB794F4) to Color(0xFFED64A6),
    Color(0xFFFFC857) to Color(0xFFFF7A59),
    Color(0xFF63B3ED) to Color(0xFF14A79D),
    Color(0xFFF687B3) to Color(0xFFFF7A59),
    Color(0xFF9AE6B4) to Color(0xFF2F9E78),
    Color(0xFFA3BFFA) to Color(0xFF7F5AF0),
)

fun gradientFor(seed: String): Pair<Color, Color> =
    AvatarGradients[(seed.hashCode() and Int.MAX_VALUE) % AvatarGradients.size]
