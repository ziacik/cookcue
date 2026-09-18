package com.ziacik.cookcue.mobile

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CookCueBerry = Color(0xFF9E3A5A)
val CookCueBerryBright = Color(0xFFF07FA2)
val CookCueEspresso = Color(0xFF24191D)
val CookCueCream = Color(0xFFFFF8F5)
val CookCueSand = Color(0xFFF1E4E8)
val CookCueHerb = Color(0xFF5F6B56)
val CookCueHoney = Color(0xFFD3A64A)

private val LightColors = lightColorScheme(
	primary = CookCueBerry,
	onPrimary = Color.White,
	primaryContainer = Color(0xFFFBD9E3),
	onPrimaryContainer = Color(0xFF3C0719),
	secondary = CookCueHerb,
	onSecondary = Color.White,
	secondaryContainer = Color(0xFFDDE8C9),
	onSecondaryContainer = Color(0xFF18220D),
	tertiary = Color(0xFF74505B),
	onTertiary = Color.White,
	tertiaryContainer = Color(0xFFF2DCE3),
	onTertiaryContainer = Color(0xFF32131D),
	background = CookCueCream,
	onBackground = CookCueEspresso,
	surface = Color(0xFFFFFBFA),
	onSurface = CookCueEspresso,
	surfaceVariant = CookCueSand,
	onSurfaceVariant = Color(0xFF735F66),
	outline = Color(0xFF957E86),
)

private val DarkColors = darkColorScheme(
	primary = CookCueBerryBright,
	onPrimary = Color(0xFF3E1020),
	primaryContainer = Color(0xFF6E263F),
	onPrimaryContainer = Color(0xFFFBD9E3),
	secondary = Color(0xFFB8C89D),
	onSecondary = Color(0xFF253313),
	secondaryContainer = Color(0xFF3A4928),
	onSecondaryContainer = Color(0xFFDDE8C9),
	tertiary = Color(0xFFFFB95F),
	onTertiary = Color(0xFF482900),
	tertiaryContainer = Color(0xFF684000),
	onTertiaryContainer = Color(0xFFF2DCE3),
	background = Color(0xFF191315),
	onBackground = Color(0xFFF7EDF1),
	surface = Color(0xFF20171A),
	onSurface = Color(0xFFF7EDF1),
	surfaceVariant = Color(0xFF36272C),
	onSurfaceVariant = Color(0xFFD8C1C9),
	outline = Color(0xFFA38A93),
)

@Composable
fun CookCueTheme(content: @Composable () -> Unit) {
	MaterialTheme(
		colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
		content = content,
	)
}
