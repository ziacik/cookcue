package com.ziacik.cookcue.wear

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.ziacik.cookcue.core.sync.DataLayerProtocol
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContent {
			MaterialTheme {
				WearCookCueScreen()
			}
		}
	}

	override fun onResume() {
		super.onResume()
		WearActionSender.send(
			this,
			DataLayerProtocol.ACTION_REQUEST_STATE,
		)
	}
}

@Composable
private fun WearCookCueScreen() {
	val context = LocalContext.current
	val state = WatchSessionStore.snapshot
	var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

	LaunchedEffect(state.receivedAtElapsedRealtime, state.started) {
		var ticks = 0
		while (state.started) {
			now = SystemClock.elapsedRealtime()
			ticks++
			if (ticks % 10 == 0) {
				WearActionSender.send(
					context,
					DataLayerProtocol.ACTION_REQUEST_STATE,
				)
			}
			delay(500)
		}
	}

	val elapsedSinceSync = if (state.receivedAtElapsedRealtime == 0L) {
		0
	} else {
		((now - state.receivedAtElapsedRealtime) / 1000).coerceAtLeast(0)
	}
	val currentElapsed = state.currentElapsedSeconds + elapsedSinceSync
	val backgroundRemaining =
		(state.backgroundRemainingSeconds - elapsedSinceSync).coerceAtLeast(0)
	val backgroundEstimate = maxOf(
		state.backgroundEstimateSeconds,
		backgroundRemaining,
	)
	val backgroundElapsed =
		(backgroundEstimate - backgroundRemaining).coerceAtLeast(0)

	val swipeModifier = if (state.started) {
		Modifier.pointerInput(state.canPrevious, state.canNext) {
			var totalDrag = 0f
			detectHorizontalDragGestures(
				onDragStart = {
					totalDrag = 0f
				},
				onHorizontalDrag = { change, dragAmount ->
					change.consume()
					totalDrag += dragAmount
				},
				onDragEnd = {
					when {
						totalDrag > 40f && state.canPrevious -> {
							WearActionSender.send(
								context,
								DataLayerProtocol.ACTION_PREVIOUS,
							)
						}

						totalDrag < -40f && state.canNext -> {
							WearActionSender.send(
								context,
								DataLayerProtocol.ACTION_NEXT,
							)
						}
					}
				},
			)
		}
	} else {
		Modifier
	}

	Box(
		modifier = Modifier
			.fillMaxSize()
			.then(swipeModifier)
			.background(
				Brush.radialGradient(
					colors = listOf(
						CookCueSurface.copy(alpha = 0.72f),
						CookCueEspresso,
					),
					radius = 230f,
				),
			),
		contentAlignment = Alignment.Center,
	) {
		when {
			!state.synced -> {
				SimpleState(
					title = "CookCue",
					message = "Hľadám telefón…",
					buttonText = "OBNOVIŤ",
					onClick = {
						WearActionSender.send(
							context,
							DataLayerProtocol.ACTION_REQUEST_STATE,
						)
					},
				)
			}

			!state.started -> {
				SimpleState(
					title = state.recipeTitle.ifBlank { "CookCue" },
					message = "Varenie ešte nie je spustené",
					buttonText = "SPUSTIŤ",
					onClick = {
						WearActionSender.send(
							context,
							DataLayerProtocol.ACTION_START,
						)
					},
				)
			}

			state.eventTaskId.isNotBlank() -> {
				EventState(
					recipeTitle = state.recipeTitle.ifBlank { "CookCue" },
					title = state.eventTitle,
					actionLabel = state.eventActionLabel.ifBlank { "HOTOVO" },
					onConfirm = {
						WearActionSender.send(
							context,
							DataLayerProtocol.ACTION_CONFIRM_EVENT,
							state.eventTaskId,
						)
					},
				)
			}

			state.currentTitle.isNotBlank() -> {
				TimerState(
					recipeTitle = state.recipeTitle.ifBlank { "CookCue" },
					taskTitle = state.currentTitle,
					estimateSeconds = state.currentEstimateSeconds,
					elapsedSeconds = currentElapsed,
					showDone = true,
					onDone = {
						WearActionSender.send(
							context,
							DataLayerProtocol.ACTION_COMPLETE_ACTIVE,
							state.currentTaskId,
						)
					},
				)
			}

			state.backgroundTitle.isNotBlank() -> {
				TimerState(
					recipeTitle = state.recipeTitle.ifBlank { "CookCue" },
					taskTitle = state.backgroundTitle,
					estimateSeconds = backgroundEstimate,
					elapsedSeconds = backgroundElapsed,
					showDone = false,
					onDone = {},
				)
			}

			else -> {
				SimpleState(
					title = state.recipeTitle.ifBlank { "CookCue" },
					message = "Momentálne od teba nič netreba",
					buttonText = null,
					onClick = {},
				)
			}
		}
	}
}

@Composable
private fun TimerState(
	recipeTitle: String,
	taskTitle: String,
	estimateSeconds: Long,
	elapsedSeconds: Long,
	showDone: Boolean,
	onDone: () -> Unit,
) {
	val safeEstimate = estimateSeconds.coerceAtLeast(1)

	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(horizontal = 14.dp, vertical = 8.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		Text(
			text = recipeTitle,
			color = CookCueCream.copy(alpha = 0.92f),
			fontSize = 8.sp,
			fontWeight = FontWeight.Medium,
			textAlign = TextAlign.Center,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
			letterSpacing = 0.15.sp,
		)

		Spacer(Modifier.height(3.dp))

		WearArcTimer(
			estimateSeconds = safeEstimate,
			elapsedSeconds = elapsedSeconds,
		)

		Spacer(Modifier.height(2.dp))

		Row(
			verticalAlignment = Alignment.CenterVertically,
		) {
			FlameGlyph(
				modifier = Modifier.size(11.dp),
			)
			Spacer(Modifier.width(4.dp))
			Text(
				text = taskTitle,
				color = CookCueCream,
				fontSize = 9.sp,
				fontWeight = FontWeight.SemiBold,
				textAlign = TextAlign.Center,
				maxLines = 2,
				overflow = TextOverflow.Ellipsis,
				lineHeight = 11.sp,
			)
		}

		if (showDone) {
			Spacer(Modifier.height(6.dp))
			RoundDoneButton(onClick = onDone)
		}
	}
}

@Composable
private fun WearArcTimer(
	estimateSeconds: Long,
	elapsedSeconds: Long,
) {
	val safeEstimate = estimateSeconds.coerceAtLeast(1)
	val remaining = (safeEstimate - elapsedSeconds).coerceAtLeast(0)
	val overdue = elapsedSeconds > safeEstimate
	val elapsedProgress =
		(elapsedSeconds.toFloat() / safeEstimate.toFloat()).coerceIn(0f, 1f)
	val shownSeconds = if (overdue) elapsedSeconds else remaining
	val totalLabel = if (overdue) {
		"nad odhad " + formatClock(safeEstimate)
	} else {
		"z " + formatClock(safeEstimate)
	}

	Box(
		modifier = Modifier.size(104.dp),
		contentAlignment = Alignment.Center,
	) {
		Canvas(modifier = Modifier.fillMaxSize()) {
			val stroke = 6.dp.toPx()
			val shadowStroke = 9.dp.toPx()

			drawArc(
				color = CookCueLine.copy(alpha = 0.35f),
				startAngle = 135f,
				sweepAngle = 270f,
				useCenter = false,
				style = Stroke(
					width = shadowStroke,
					cap = StrokeCap.Butt,
				),
			)
			drawArc(
				color = CookCueCream.copy(alpha = 0.94f),
				startAngle = 135f,
				sweepAngle = 270f,
				useCenter = false,
				style = Stroke(
					width = stroke,
					cap = StrokeCap.Butt,
				),
			)
			drawArc(
				brush = Brush.sweepGradient(
					colors = listOf(
						CookCueWineDeep,
						CookCueWine,
						CookCueWineDeep,
					),
					center = center,
				),
				startAngle = 135f,
				sweepAngle = 270f * elapsedProgress,
				useCenter = false,
				style = Stroke(
					width = stroke,
					cap = StrokeCap.Butt,
				),
			)

			val marker = Path().apply {
				moveTo(center.x, center.y - 30.dp.toPx())
				cubicTo(
					center.x - 3.5.dp.toPx(),
					center.y - 25.dp.toPx(),
					center.x - 2.dp.toPx(),
					center.y - 21.dp.toPx(),
					center.x,
					center.y - 20.dp.toPx(),
				)
				cubicTo(
					center.x + 2.dp.toPx(),
					center.y - 21.dp.toPx(),
					center.x + 3.5.dp.toPx(),
					center.y - 25.dp.toPx(),
					center.x,
					center.y - 30.dp.toPx(),
				)
				close()
			}
			drawPath(
				path = marker,
				color = CookCueWineDeep,
			)
		}

		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			Text(
				text = formatClock(shownSeconds),
				color = CookCueCream,
				fontFamily = FontFamily.Serif,
				fontSize = 27.sp,
				fontWeight = FontWeight.Normal,
				lineHeight = 28.sp,
				letterSpacing = (-0.4).sp,
			)
			Text(
				text = totalLabel,
				color = CookCueMuted,
				fontSize = 7.sp,
				lineHeight = 8.sp,
				letterSpacing = 0.2.sp,
			)
		}
	}
}

@Composable
private fun FlameGlyph(modifier: Modifier = Modifier) {
	Canvas(modifier = modifier) {
		val w = size.width
		val h = size.height

		val outer = Path().apply {
			moveTo(w * 0.52f, h * 0.05f)
			cubicTo(w * 0.58f, h * 0.26f, w * 0.82f, h * 0.32f, w * 0.80f, h * 0.56f)
			cubicTo(w * 0.78f, h * 0.82f, w * 0.63f, h * 0.96f, w * 0.48f, h * 0.96f)
			cubicTo(w * 0.26f, h * 0.96f, w * 0.10f, h * 0.80f, w * 0.13f, h * 0.58f)
			cubicTo(w * 0.16f, h * 0.39f, w * 0.30f, h * 0.28f, w * 0.35f, h * 0.13f)
			cubicTo(w * 0.40f, h * 0.28f, w * 0.50f, h * 0.30f, w * 0.52f, h * 0.05f)
			close()
		}
		drawPath(
			path = outer,
			brush = Brush.verticalGradient(
				colors = listOf(
					CookCueHoney,
					CookCueWine,
				),
			),
		)

		val inner = Path().apply {
			moveTo(w * 0.50f, h * 0.44f)
			cubicTo(w * 0.62f, h * 0.55f, w * 0.63f, h * 0.72f, w * 0.51f, h * 0.82f)
			cubicTo(w * 0.39f, h * 0.72f, w * 0.39f, h * 0.57f, w * 0.50f, h * 0.44f)
			close()
		}
		drawPath(
			path = inner,
			color = CookCueCream.copy(alpha = 0.9f),
		)
	}
}

@Composable
private fun RoundDoneButton(onClick: () -> Unit) {
	Button(
		onClick = onClick,
		modifier = Modifier.size(36.dp),
		colors = ButtonDefaults.buttonColors(
			containerColor = CookCueWineDeep,
			contentColor = CookCueCream,
		),
		shape = CircleShape,
	) {
		CheckGlyph(
			modifier = Modifier.size(15.dp),
		)
	}
}

@Composable
private fun CheckGlyph(modifier: Modifier = Modifier) {
	Canvas(modifier = modifier) {
		val path = Path().apply {
			moveTo(size.width * 0.18f, size.height * 0.52f)
			lineTo(size.width * 0.42f, size.height * 0.74f)
			lineTo(size.width * 0.82f, size.height * 0.27f)
		}
		drawPath(
			path = path,
			color = CookCueCream,
			style = Stroke(
				width = 2.1.dp.toPx(),
				cap = StrokeCap.Round,
			),
		)
	}
}

@Composable
private fun EventState(
	recipeTitle: String,
	title: String,
	actionLabel: String,
	onConfirm: () -> Unit,
) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(horizontal = 18.dp, vertical = 9.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		Text(
			text = recipeTitle,
			color = CookCueCream.copy(alpha = 0.92f),
			fontSize = 8.sp,
			fontWeight = FontWeight.Medium,
			textAlign = TextAlign.Center,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
		)

		Spacer(Modifier.height(10.dp))

		EventGlyph(
			modifier = Modifier.size(56.dp),
		)

		Spacer(Modifier.height(5.dp))

		Text(
			text = title,
			color = CookCueCream,
			fontFamily = FontFamily.Serif,
			fontSize = 13.sp,
			fontWeight = FontWeight.Medium,
			textAlign = TextAlign.Center,
			maxLines = 2,
			overflow = TextOverflow.Ellipsis,
			lineHeight = 15.sp,
		)

		Spacer(Modifier.height(8.dp))

		RoundDoneButton(onClick = onConfirm)

		Spacer(Modifier.height(4.dp))

		Text(
			text = actionLabel,
			color = CookCueMuted,
			fontSize = 7.sp,
			textAlign = TextAlign.Center,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
		)
	}
}

@Composable
private fun EventGlyph(modifier: Modifier = Modifier) {
	Canvas(modifier = modifier) {
		drawArc(
			color = CookCueLine.copy(alpha = 0.38f),
			startAngle = 145f,
			sweepAngle = 250f,
			useCenter = false,
			style = Stroke(
				width = 7.dp.toPx(),
				cap = StrokeCap.Round,
			),
		)
		drawArc(
			brush = Brush.sweepGradient(
				colors = listOf(
					CookCueWineDeep,
					CookCueWine,
					CookCueWineDeep,
				),
				center = center,
			),
			startAngle = 145f,
			sweepAngle = 118f,
			useCenter = false,
			style = Stroke(
				width = 7.dp.toPx(),
				cap = StrokeCap.Round,
			),
		)

		val dotRadius = 4.dp.toPx()
		drawCircle(
			color = CookCueHoney,
			radius = dotRadius,
			center = Offset(center.x, center.y - 4.dp.toPx()),
		)
		drawCircle(
			color = CookCueEspresso,
			radius = dotRadius * 0.42f,
			center = Offset(center.x, center.y - 4.dp.toPx()),
		)
	}
}

@Composable
private fun SimpleState(
	title: String,
	message: String,
	buttonText: String?,
	onClick: () -> Unit,
) {
	Column(
		modifier = Modifier.padding(horizontal = 22.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		Text(
			text = title,
			color = CookCueCream,
			fontSize = 9.sp,
			fontWeight = FontWeight.Medium,
			textAlign = TextAlign.Center,
		)
		Spacer(Modifier.height(12.dp))
		Text(
			text = message,
			color = CookCueCream,
			fontFamily = FontFamily.Serif,
			fontSize = 13.sp,
			fontWeight = FontWeight.Medium,
			textAlign = TextAlign.Center,
			maxLines = 3,
		)
		if (buttonText != null) {
			Spacer(Modifier.height(12.dp))
			Button(
				onClick = onClick,
				modifier = Modifier
					.fillMaxWidth()
					.height(34.dp),
				colors = ButtonDefaults.buttonColors(
					containerColor = CookCueWineDeep,
					contentColor = CookCueCream,
				),
				shape = RoundedCornerShape(16.dp),
			) {
				Text(
					text = buttonText,
					fontSize = 8.sp,
					fontWeight = FontWeight.Bold,
					letterSpacing = 0.4.sp,
				)
			}
		}
	}
}

private fun formatClock(seconds: Long): String {
	val safe = seconds.coerceAtLeast(0)
	val hours = safe / 3600
	val minutes = (safe % 3600) / 60
	val remainder = safe % 60

	return if (hours > 0) {
		hours.toString() + ":" +
			minutes.toString().padStart(2, '0') + ":" +
			remainder.toString().padStart(2, '0')
	} else {
		minutes.toString() + ":" + remainder.toString().padStart(2, '0')
	}
}
