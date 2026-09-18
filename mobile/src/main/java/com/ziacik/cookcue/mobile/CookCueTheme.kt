package com.ziacik.cookcue.mobile

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CookCuePaprika = Color(0xFFC8542E)
val CookCuePaprikaBright = Color(0xFFFF8A5B)
val CookCueEspresso = Color(0xFF2A1B16)
val CookCueCream = Color(0xFFFFF8F1)
val CookCueSand = Color(0xFFF3E5D8)
val CookCueHerb = Color(0xFF5C6B4C)
val CookCueHoney = Color(0xFFF2B84B)

private val LightColors = lightColorScheme(
	primary = CookCuePaprika,
	onPrimary = Color.White,
	primaryContainer = Color(0xFFFFDBCD),
	onPrimaryContainer = Color(0xFF3A0B00),
	secondary = CookCueHerb,
	onSecondary = Color.White,
	secondaryContainer = Color(0xFFDDE8C9),
	onSecondaryContainer = Color(0xFF18220D),
	tertiary = Color(0xFF8A5D22),
	onTertiary = Color.White,
	tertiaryContainer = Color(0xFFFFDDB2),
	onTertiaryContainer = Color(0xFF2C1700),
	background = CookCueCream,
	onBackground = CookCueEspresso,
	surface = Color(0xFFFFFBF7),
	onSurface = CookCueEspresso,
	surfaceVariant = CookCueSand,
	onSurfaceVariant = Color(0xFF75645C),
	outline = Color(0xFF9A877E),
)

private val DarkColors = darkColorScheme(
	primary = CookCuePaprikaBright,
	onPrimary = Color(0xFF4B1608),
	primaryContainer = Color(0xFF71301E),
	onPrimaryContainer = Color(0xFFFFDBCD),
	secondary = Color(0xFFB8C89D),
	onSecondary = Color(0xFF253313),
	secondaryContainer = Color(0xFF3A4928),
	onSecondaryContainer = Color(0xFFDDE8C9),
	tertiary = Color(0xFFFFB95F),
	onTertiary = Color(0xFF482900),
	tertiaryContainer = Color(0xFF684000),
	onTertiaryContainer = Color(0xFFFFDDB2),
	background = Color(0xFF1B1512),
	onBackground = Color(0xFFF7EDE5),
	surface = Color(0xFF211915),
	onSurface = Color(0xFFF7EDE5),
	surfaceVariant = Color(0xFF352823),
	onSurfaceVariant = Color(0xFFD8C2B8),
	outline = Color(0xFFA58F85),
)

@Composable
fun CookCueTheme(content: @Composable () -> Unit) {
	MaterialTheme(
		colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
		content = content,
	)
}
