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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
			.background(CookCueEspresso),
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
	val remaining = (safeEstimate - elapsedSeconds).coerceAtLeast(0)

	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(horizontal = 14.dp, vertical = 8.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		Text(
			text = recipeTitle,
			color = CookCueCream,
			fontSize = 10.sp,
			fontWeight = FontWeight.Medium,
			textAlign = TextAlign.Center,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
		)

		Spacer(Modifier.height(4.dp))

		WearArcTimer(
			estimateSeconds = safeEstimate,
			elapsedSeconds = elapsedSeconds,
		)

		Spacer(Modifier.height(1.dp))

		Text(
			text = "🔥  $taskTitle",
			color = CookCueCream,
			fontSize = 11.sp,
			fontWeight = FontWeight.SemiBold,
			textAlign = TextAlign.Center,
			maxLines = 2,
			overflow = TextOverflow.Ellipsis,
			lineHeight = 13.sp,
		)

		if (showDone) {
			Spacer(Modifier.height(5.dp))
			RoundDoneButton(onClick = onDone)
		} else {
			Spacer(Modifier.height(3.dp))
			Text(
				text = if (remaining > 0) "beží" else "hotovo",
				color = CookCueMuted,
				fontSize = 8.sp,
			)
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
	val progress = if (overdue) {
		1f
	} else {
		(elapsedSeconds.toFloat() / safeEstimate.toFloat()).coerceIn(0f, 1f)
	}
	val shownSeconds = if (overdue) elapsedSeconds else remaining
	val totalLabel = if (overdue) {
		"nad odhad " + formatClock(safeEstimate)
	} else {
		"z " + formatClock(safeEstimate)
	}

	Box(
		modifier = Modifier.size(108.dp),
		contentAlignment = Alignment.Center,
	) {
		Canvas(modifier = Modifier.fillMaxSize()) {
			drawArc(
				color = CookCueCream.copy(alpha = 0.92f),
				startAngle = 135f,
				sweepAngle = 270f,
				useCenter = false,
				style = Stroke(
					width = 6.dp.toPx(),
					cap = StrokeCap.Butt,
				),
			)
			drawArc(
				color = CookCueWineDeep,
				startAngle = 135f,
				sweepAngle = 270f * progress,
				useCenter = false,
				style = Stroke(
					width = 6.dp.toPx(),
					cap = StrokeCap.Butt,
				),
			)
		}

		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			Text(
				text = formatClock(shownSeconds),
				color = CookCueCream,
				fontFamily = FontFamily.Serif,
				fontSize = 30.sp,
				fontWeight = FontWeight.Medium,
				lineHeight = 31.sp,
			)
			Text(
				text = totalLabel,
				color = CookCueMuted,
				fontSize = 9.sp,
				lineHeight = 10.sp,
			)
		}
	}
}

@Composable
private fun RoundDoneButton(onClick: () -> Unit) {
	Button(
		onClick = onClick,
		modifier = Modifier.size(42.dp),
		colors = ButtonDefaults.buttonColors(
			containerColor = CookCueWineDeep,
			contentColor = CookCueCream,
		),
		shape = CircleShape,
	) {
		Text(
			text = "✓",
			fontSize = 20.sp,
			fontWeight = FontWeight.Medium,
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
			.padding(horizontal = 18.dp, vertical = 12.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		Text(
			text = recipeTitle,
			color = CookCueCream,
			fontSize = 10.sp,
			fontWeight = FontWeight.Medium,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
		)
		Spacer(Modifier.height(28.dp))
		Text(
			text = title,
			color = CookCueCream,
			fontSize = 14.sp,
			fontWeight = FontWeight.SemiBold,
			textAlign = TextAlign.Center,
			maxLines = 3,
			overflow = TextOverflow.Ellipsis,
			lineHeight = 17.sp,
		)
		Spacer(Modifier.height(12.dp))
		Button(
			onClick = onConfirm,
			modifier = Modifier.size(46.dp),
			colors = ButtonDefaults.buttonColors(
				containerColor = CookCueWineDeep,
				contentColor = CookCueCream,
			),
			shape = CircleShape,
		) {
			Text(
				text = if (actionLabel.equals("HOTOVO", ignoreCase = true)) "✓" else "›",
				fontSize = 20.sp,
				fontWeight = FontWeight.Medium,
			)
		}
		if (!actionLabel.equals("HOTOVO", ignoreCase = true)) {
			Spacer(Modifier.height(4.dp))
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
			fontSize = 11.sp,
			fontWeight = FontWeight.Medium,
			textAlign = TextAlign.Center,
		)
		Spacer(Modifier.height(12.dp))
		Text(
			text = message,
			color = CookCueCream,
			fontSize = 13.sp,
			fontWeight = FontWeight.SemiBold,
			textAlign = TextAlign.Center,
			maxLines = 3,
		)
		if (buttonText != null) {
			Spacer(Modifier.height(12.dp))
			Button(
				onClick = onClick,
				modifier = Modifier
					.fillMaxWidth()
					.height(38.dp),
				colors = ButtonDefaults.buttonColors(
					containerColor = CookCueWineDeep,
					contentColor = CookCueCream,
				),
				shape = RoundedCornerShape(14.dp),
			) {
				Text(
					text = buttonText,
					fontSize = 10.sp,
					fontWeight = FontWeight.Bold,
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
