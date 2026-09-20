package com.ziacik.cookcue.mobile

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CookCueWine = Color(0xFF5B1120)
val CookCueRose = Color(0xFFA0525A)
val CookCueCream = Color(0xFFFFFAF5)
val CookCuePaper = Color(0xFFF5ECE5)
val CookCueSand = Color(0xFFEADFD6)
val CookCueTaupe = Color(0xFFC9B79E)
val CookCueInk = Color(0xFF2D292A)
val CookCueHerb = Color(0xFF63705B)
val CookCueHoney = Color(0xFFC7A66A)

private val LightColors = lightColorScheme(
	primary = CookCueWine,
	onPrimary = Color.White,
	primaryContainer = Color(0xFFF3E3E6),
	onPrimaryContainer = Color(0xFF3A0A14),
	secondary = CookCueHerb,
	onSecondary = Color.White,
	secondaryContainer = Color(0xFFE9EDE4),
	onSecondaryContainer = Color(0xFF252C20),
	tertiary = Color(0xFF8A6532),
	onTertiary = Color.White,
	tertiaryContainer = Color(0xFFF4E9D8),
	onTertiaryContainer = Color(0xFF38270E),
	background = CookCueCream,
	onBackground = CookCueInk,
	surface = Color(0xFFFFFDFC),
	onSurface = CookCueInk,
	surfaceVariant = CookCuePaper,
	onSurfaceVariant = Color(0xFF6D6061),
	outline = CookCueSand,
)

private val DarkColors = darkColorScheme(
	primary = Color(0xFFF0A0B1),
	onPrimary = Color(0xFF4A1020),
	primaryContainer = Color(0xFF651F34),
	onPrimaryContainer = Color(0xFFFFD9E2),
	secondary = Color(0xFFC0CBB6),
	onSecondary = Color(0xFF293124),
	secondaryContainer = Color(0xFF3B4435),
	onSecondaryContainer = Color(0xFFDDE7D6),
	tertiary = Color(0xFFE3C386),
	onTertiary = Color(0xFF3B2A0D),
	tertiaryContainer = Color(0xFF554019),
	onTertiaryContainer = Color(0xFFF8E5BB),
	background = Color(0xFF171214),
	onBackground = Color(0xFFF6ECEE),
	surface = Color(0xFF201719),
	onSurface = Color(0xFFF6ECEE),
	surfaceVariant = Color(0xFF302629),
	onSurfaceVariant = Color(0xFFD3C2C5),
	outline = Color(0xFF55474A),
)

@Composable
fun CookCueTheme(content: @Composable () -> Unit) {
	MaterialTheme(
		colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
		content = content,
	)
}
