package com.ziacik.cookcue.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
	val notificationPermissionLauncher = rememberLauncherForActivityResult(
		ActivityResultContracts.RequestPermission(),
	) {}
	val state = WatchSessionStore.snapshot
	var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

	val keepScreenAwake =
		state.started && (state.currentTaskId.isNotBlank() || state.eventTaskId.isNotBlank())

	LaunchedEffect(keepScreenAwake) {
		val window = (context as? ComponentActivity)?.window ?: return@LaunchedEffect
		if (keepScreenAwake) {
			window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		} else {
			window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		}
	}

	LaunchedEffect(state.started) {
		if (
			state.started &&
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
			context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
			PackageManager.PERMISSION_GRANTED
		) {
			notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
		}
	}

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

	val backgroundTotal = state.backgroundEstimateSeconds
	val backgroundRemaining =
		(state.backgroundRemainingSeconds - elapsedSinceSync).coerceAtLeast(0)
	val backgroundElapsed = if (backgroundTotal > 0) {
		(backgroundTotal - backgroundRemaining).coerceAtLeast(0)
	} else {
		0
	}

	val swipeModifier = if (state.started) {
		Modifier.pointerInput(state.canPrevious, state.canNext) {
			var totalDrag = 0f
			detectHorizontalDragGestures(
				onDragStart = { totalDrag = 0f },
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
						CookCueSurface.copy(alpha = 0.64f),
						CookCueEspresso,
					),
					radius = 240f,
				),
			),
		contentAlignment = Alignment.Center,
	) {
		when {
			!state.synced -> {
				SimpleState(
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
					message = "Varenie ešte nie je spustené",
					buttonText = "SPUSTIŤ",
					onClick = {
						if (
							Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
							context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
							PackageManager.PERMISSION_GRANTED
						) {
							notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
						}
						WearActionSender.send(
							context,
							DataLayerProtocol.ACTION_START,
						)
					},
				)
			}

			state.eventTaskId.isNotBlank() -> {
				EventState(
					title = state.eventTitle,
					actionLabel = state.eventActionLabel.ifBlank { "HOTOVO" },
					retryActionLabel = state.eventRetryActionLabel,
					retryAfterSeconds = state.eventRetryAfterSeconds,
					onDefer = {
						WearActionSender.send(
							context,
							DataLayerProtocol.ACTION_DEFER_EVENT,
							state.eventTaskId,
						)
					},
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
					taskTitle = state.currentTitle,
					totalSeconds = state.currentEstimateSeconds,
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
					taskTitle = state.backgroundTitle,
					totalSeconds = backgroundTotal,
					elapsedSeconds = backgroundElapsed,
					showDone = false,
					onDone = {},
				)
			}

			else -> {
				SimpleState(
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
	taskTitle: String,
	totalSeconds: Long,
	elapsedSeconds: Long,
	showDone: Boolean,
	onDone: () -> Unit,
) {
	val hasTotal = totalSeconds > 0
	val safeTotal = totalSeconds.coerceAtLeast(1)
	val overdue = hasTotal && elapsedSeconds > safeTotal
	val remaining = if (hasTotal) {
		(safeTotal - elapsedSeconds).coerceAtLeast(0)
	} else {
		0
	}
	val shownSeconds = if (overdue) elapsedSeconds else remaining

	Box(
		modifier = Modifier.fillMaxSize(),
		contentAlignment = Alignment.Center,
	) {
		FullScreenProgressRing(
			totalSeconds = totalSeconds,
			elapsedSeconds = elapsedSeconds,
		)

		Column(
			modifier = Modifier.padding(horizontal = 30.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			Text(
				text = if (hasTotal) formatClock(shownSeconds) else "--:--",
				color = CookCueCream,
				fontFamily = FontFamily.Serif,
				fontSize = 34.sp,
				fontWeight = FontWeight.Normal,
				lineHeight = 35.sp,
				letterSpacing = (-0.5).sp,
			)

			Text(
				text = when {
					overdue -> "trvá"
					hasTotal -> "z " + formatClock(totalSeconds)
					else -> "čakám na celkový čas"
				},
				color = CookCueMuted,
				fontSize = 9.sp,
				lineHeight = 10.sp,
				letterSpacing = 0.15.sp,
			)

			Spacer(Modifier.height(9.dp))

			Row(
				verticalAlignment = Alignment.CenterVertically,
			) {
				FlameGlyph(
					modifier = Modifier.size(12.dp),
				)
				Spacer(Modifier.width(5.dp))
				Text(
					text = taskTitle,
					color = CookCueCream,
					fontSize = 11.sp,
					fontWeight = FontWeight.SemiBold,
					textAlign = TextAlign.Center,
					maxLines = 2,
					overflow = TextOverflow.Ellipsis,
					lineHeight = 13.sp,
				)
			}

			if (showDone) {
				Spacer(Modifier.height(12.dp))
				RoundDoneButton(onClick = onDone)
			}
		}
	}
}

@Composable
private fun FullScreenProgressRing(
	totalSeconds: Long,
	elapsedSeconds: Long,
) {
	val progress = if (totalSeconds > 0) {
		(elapsedSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
	} else {
		0f
	}

	Canvas(
		modifier = Modifier
			.fillMaxSize()
			.padding(7.dp),
	) {
		val stroke = 7.dp.toPx()

		drawCircle(
			color = CookCueLine.copy(alpha = 0.72f),
			style = Stroke(
				width = stroke,
				cap = StrokeCap.Round,
			),
		)

		if (progress > 0f) {
			drawArc(
				brush = Brush.sweepGradient(
					colors = listOf(
						CookCueWineDeep,
						CookCueWine,
						CookCueWineDeep,
					),
					center = center,
				),
				startAngle = -90f,
				sweepAngle = 360f * progress,
				useCenter = false,
				style = Stroke(
					width = stroke,
					cap = StrokeCap.Round,
				),
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
			color = CookCueCream.copy(alpha = 0.88f),
		)
	}
}

@Composable
private fun RoundDoneButton(onClick: () -> Unit) {
	Button(
		onClick = onClick,
		modifier = Modifier.size(40.dp),
		colors = ButtonDefaults.buttonColors(
			containerColor = CookCueWineDeep,
			contentColor = CookCueCream,
		),
		shape = CircleShape,
	) {
		CheckGlyph(
			modifier = Modifier.size(17.dp),
		)
	}
}

@Composable
private fun CheckGlyph(modifier: Modifier = Modifier) {
	Canvas(modifier = modifier) {
		val path = Path().apply {
			moveTo(size.width * 0.17f, size.height * 0.52f)
			lineTo(size.width * 0.42f, size.height * 0.74f)
			lineTo(size.width * 0.83f, size.height * 0.25f)
		}

		drawPath(
			path = path,
			color = CookCueCream,
			style = Stroke(
				width = 2.2.dp.toPx(),
				cap = StrokeCap.Round,
			),
		)
	}
}

@Composable
private fun EventState(
	title: String,
	actionLabel: String,
	retryActionLabel: String,
	retryAfterSeconds: Long,
	onDefer: () -> Unit,
	onConfirm: () -> Unit,
) {
	Box(
		modifier = Modifier.fillMaxSize(),
		contentAlignment = Alignment.Center,
	) {
		Canvas(
			modifier = Modifier
				.fillMaxSize()
				.padding(7.dp),
		) {
			drawCircle(
				color = CookCueLine.copy(alpha = 0.55f),
				style = Stroke(
					width = 7.dp.toPx(),
					cap = StrokeCap.Round,
				),
			)
		}

		Column(
			modifier = Modifier.padding(horizontal = 30.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			Text(
				text = "ČAKÁ NA TEBA",
				color = CookCueWine,
				fontSize = 8.sp,
				fontWeight = FontWeight.Bold,
				letterSpacing = 0.7.sp,
			)

			Spacer(Modifier.height(7.dp))

			Text(
				text = title,
				color = CookCueCream,
				fontFamily = FontFamily.Serif,
				fontSize = 15.sp,
				fontWeight = FontWeight.Medium,
				textAlign = TextAlign.Center,
				maxLines = 3,
				overflow = TextOverflow.Ellipsis,
				lineHeight = 17.sp,
			)

			Spacer(Modifier.height(12.dp))

			if (retryAfterSeconds > 0 && retryActionLabel.isNotBlank()) {
				Row(
					horizontalArrangement = Arrangement.spacedBy(18.dp),
					verticalAlignment = Alignment.Top,
				) {
					EventChoice(
						label = retryActionLabel,
						symbol = "×",
						onClick = onDefer,
					)
					EventChoice(
						label = actionLabel,
						symbol = "✓",
						onClick = onConfirm,
					)
				}
				Spacer(Modifier.height(5.dp))
				Text(
					text = "znova o " + formatCompactDuration(retryAfterSeconds),
					color = CookCueMuted,
					fontSize = 8.sp,
					textAlign = TextAlign.Center,
				)
			} else {
				RoundDoneButton(onClick = onConfirm)
				Spacer(Modifier.height(5.dp))
				Text(
					text = actionLabel,
					color = CookCueMuted,
					fontSize = 8.sp,
					textAlign = TextAlign.Center,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
				)
			}
		}
	}
}

@Composable
private fun EventChoice(
	label: String,
	symbol: String,
	onClick: () -> Unit,
) {
	Column(horizontalAlignment = Alignment.CenterHorizontally) {
		Button(
			onClick = onClick,
			modifier = Modifier.size(40.dp),
			colors = ButtonDefaults.buttonColors(
				containerColor = CookCueWineDeep,
				contentColor = CookCueCream,
			),
			shape = CircleShape,
		) {
			Text(
				text = symbol,
				fontSize = 18.sp,
				fontWeight = FontWeight.Medium,
			)
		}
		Spacer(Modifier.height(3.dp))
		Text(
			text = label,
			color = CookCueMuted,
			fontSize = 8.sp,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
		)
	}
}

@Composable
private fun SimpleState(
	message: String,
	buttonText: String?,
	onClick: () -> Unit,
) {
	Column(
		modifier = Modifier.padding(horizontal = 26.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		Text(
			text = message,
			color = CookCueCream,
			fontFamily = FontFamily.Serif,
			fontSize = 15.sp,
			fontWeight = FontWeight.Medium,
			textAlign = TextAlign.Center,
			maxLines = 3,
			lineHeight = 17.sp,
		)

		if (buttonText != null) {
			Spacer(Modifier.height(13.dp))
			Button(
				onClick = onClick,
				modifier = Modifier
					.width(112.dp)
					.height(38.dp),
				colors = ButtonDefaults.buttonColors(
					containerColor = CookCueWineDeep,
					contentColor = CookCueCream,
				),
				shape = RoundedCornerShape(18.dp),
			) {
				Text(
					text = buttonText,
					fontSize = 9.sp,
					fontWeight = FontWeight.Bold,
					letterSpacing = 0.35.sp,
				)
			}
		}
	}
}

private fun formatCompactDuration(seconds: Long): String {
	val safe = seconds.coerceAtLeast(0)
	val minutes = safe / 60
	val remainder = safe % 60
	return when {
		minutes > 0 && remainder > 0 -> minutes.toString() + "m " + remainder + "s"
		minutes > 0 -> minutes.toString() + "m"
		else -> remainder.toString() + "s"
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
