package com.ziacik.cookcue.mobile

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val CookCueBerry = Color(0xFF8C2948)
val CookCueBerrySoft = Color(0xFFB85C78)
val CookCueEspresso = Color(0xFF25191C)
val CookCueCream = Color(0xFFFFFAF7)
val CookCuePaper = Color(0xFFF7EFEA)
val CookCueLine = Color(0xFFE7DAD5)
val CookCueHerb = Color(0xFF66705A)
val CookCueHoney = Color(0xFFC8A15A)

private val LightColors = lightColorScheme(
	primary = CookCueBerry,
	onPrimary = Color.White,
	primaryContainer = Color(0xFFF8E8EC),
	onPrimaryContainer = Color(0xFF39101D),
	secondary = CookCueHerb,
	onSecondary = Color.White,
	secondaryContainer = Color(0xFFEDF0E8),
	onSecondaryContainer = Color(0xFF20261B),
	tertiary = Color(0xFF8A6532),
	onTertiary = Color.White,
	tertiaryContainer = Color(0xFFF6EBD9),
	onTertiaryContainer = Color(0xFF34230B),
	background = CookCueCream,
	onBackground = CookCueEspresso,
	surface = Color(0xFFFFFDFC),
	onSurface = CookCueEspresso,
	surfaceVariant = CookCuePaper,
	onSurfaceVariant = Color(0xFF6E5E62),
	outline = CookCueLine,
)

private val DarkColors = darkColorScheme(
	primary = Color(0xFFF19AB5),
	onPrimary = Color(0xFF4B1427),
	primaryContainer = Color(0xFF5C2034),
	onPrimaryContainer = Color(0xFFFFD9E5),
	secondary = Color(0xFFC2CBB5),
	onSecondary = Color(0xFF2D3526),
	secondaryContainer = Color(0xFF3C4435),
	onSecondaryContainer = Color(0xFFDFE7D5),
	tertiary = Color(0xFFE0BD7B),
	onTertiary = Color(0xFF3B2A0D),
	tertiaryContainer = Color(0xFF554018),
	onTertiaryContainer = Color(0xFFF6E3BB),
	background = Color(0xFF171214),
	onBackground = Color(0xFFF7ECEF),
	surface = Color(0xFF201719),
	onSurface = Color(0xFFF7ECEF),
	surfaceVariant = Color(0xFF302629),
	onSurfaceVariant = Color(0xFFD4C3C7),
	outline = Color(0xFF55464A),
)

private val BaseTypography = Typography()
private val CookCueTypography = BaseTypography.copy(
	displaySmall = BaseTypography.displaySmall.copy(
		fontFamily = FontFamily.Serif,
		fontWeight = FontWeight.SemiBold,
		letterSpacing = (-0.4).sp,
	),
	headlineLarge = BaseTypography.headlineLarge.copy(
		fontFamily = FontFamily.Serif,
		fontWeight = FontWeight.SemiBold,
		letterSpacing = (-0.35).sp,
	),
	headlineMedium = BaseTypography.headlineMedium.copy(
		fontFamily = FontFamily.Serif,
		fontWeight = FontWeight.SemiBold,
		letterSpacing = (-0.25).sp,
	),
	headlineSmall = BaseTypography.headlineSmall.copy(
		fontFamily = FontFamily.Serif,
		fontWeight = FontWeight.SemiBold,
	),
	titleLarge = BaseTypography.titleLarge.copy(
		fontFamily = FontFamily.Serif,
		fontWeight = FontWeight.SemiBold,
	),
)

@Composable
fun CookCueTheme(content: @Composable () -> Unit) {
	MaterialTheme(
		colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
		typography = CookCueTypography,
		content = content,
	)
}
