package com.ziacik.cookcue.wear

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
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
	val backgroundRemaining = (state.backgroundRemainingSeconds - elapsedSinceSync).coerceAtLeast(0)
	val nextIn = (state.nextInSeconds - elapsedSinceSync).coerceAtLeast(0)

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(CookCueEspresso),
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.verticalScroll(rememberScrollState())
				.padding(horizontal = 16.dp, vertical = 9.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			WearBrandHeader()
			Spacer(Modifier.height(6.dp))

			when {
				!state.synced -> {
					StatusPill("PRIPÁJAM")
					Spacer(Modifier.height(8.dp))
					Text(
						text = "Hľadám telefón…",
						color = CookCueCream,
						style = MaterialTheme.typography.titleMedium,
						textAlign = TextAlign.Center,
					)
					Spacer(Modifier.height(12.dp))
					PrimaryWearButton(
						text = "OBNOVIŤ",
						onClick = {
							WearActionSender.send(
								context,
								DataLayerProtocol.ACTION_REQUEST_STATE,
							)
						},
					)
				}

				!state.started -> {
					StatusPill("PRIPRAVENÉ")
					Spacer(Modifier.height(8.dp))
					Text(
						text = "Varenie ešte nie je spustené",
						color = CookCueCream,
						style = MaterialTheme.typography.titleMedium,
						textAlign = TextAlign.Center,
						maxLines = 2,
					)
					Spacer(Modifier.height(12.dp))
					PrimaryWearButton(
						text = "SPUSTIŤ",
						onClick = {
							WearActionSender.send(
								context,
								DataLayerProtocol.ACTION_START,
							)
						},
					)
				}

				state.eventTaskId.isNotBlank() -> {
					StatusPill("ČAKÁ NA TEBA")
					Spacer(Modifier.height(7.dp))
					StepTitle(state.eventTitle)
					if (state.eventInstruction.isNotBlank()) {
						Spacer(Modifier.height(4.dp))
						InstructionText(state.eventInstruction)
					}
					if (state.eventTips.isNotBlank()) {
						Spacer(Modifier.height(7.dp))
						HintText(state.eventTips.replace("\n", " · "))
					}
					Spacer(Modifier.height(12.dp))
					PrimaryWearButton(
						text = state.eventActionLabel,
						onClick = {
							WearActionSender.send(
								context,
								DataLayerProtocol.ACTION_CONFIRM_EVENT,
								state.eventTaskId,
							)
						},
					)
				}

				state.currentTitle.isNotBlank() -> {
					StatusPill("TERAZ")
					Spacer(Modifier.height(7.dp))
					StepTitle(state.currentTitle)
					if (state.currentInstruction.isNotBlank()) {
						Spacer(Modifier.height(3.dp))
						InstructionText(state.currentInstruction)
					}
					Spacer(Modifier.height(8.dp))
					WearProgressDial(
						estimateSeconds = state.currentEstimateSeconds,
						elapsedSeconds = currentElapsed,
					)
					Spacer(Modifier.height(10.dp))
					PrimaryWearButton(
						text = "✓  HOTOVO",
						onClick = {
							WearActionSender.send(
								context,
								DataLayerProtocol.ACTION_COMPLETE_ACTIVE,
								state.currentTaskId,
							)
						},
					)
				}

				state.backgroundTitle.isNotBlank() -> {
					StatusPill("BEŽÍ")
					Spacer(Modifier.height(7.dp))
					StepTitle(state.backgroundTitle)
					Spacer(Modifier.height(10.dp))
					WearRemainingDial(backgroundRemaining)
				}

				else -> {
					StatusPill("POKOJ")
					Spacer(Modifier.height(8.dp))
					Text(
						text = "Momentálne od teba nič netreba",
						color = CookCueCream,
						style = MaterialTheme.typography.titleMedium,
						textAlign = TextAlign.Center,
						maxLines = 2,
					)
				}
			}

			if (state.started) {
				if (state.nextTitle.isNotBlank()) {
					Spacer(Modifier.height(9.dp))
					NextStepInfo(
						title = state.nextTitle,
						nextIn = nextIn,
					)
				}

				Spacer(Modifier.height(9.dp))
				WearNavigation(
					canPrevious = state.canPrevious,
					canNext = state.canNext,
					onPrevious = {
						WearActionSender.send(
							context,
							DataLayerProtocol.ACTION_PREVIOUS,
						)
					},
					onNext = {
						WearActionSender.send(
							context,
							DataLayerProtocol.ACTION_NEXT,
						)
					},
				)
			}
		}
	}
}

@Composable
private fun WearBrandHeader() {
	Row(
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(5.dp),
	) {
		Box(modifier = Modifier.size(17.dp)) {
			Canvas(modifier = Modifier.fillMaxSize()) {
				drawArc(
					color = CookCueWine,
					startAngle = 45f,
					sweepAngle = 275f,
					useCenter = false,
					style = Stroke(
						width = 2.4.dp.toPx(),
						cap = StrokeCap.Round,
					),
				)
				drawCircle(
					color = CookCueHoney,
					radius = 1.7.dp.toPx(),
					center = Offset(this.size.width * 0.76f, this.size.height * 0.20f),
				)
			}
		}
		Text(
			text = "CookCue",
			color = CookCueCream,
			style = MaterialTheme.typography.labelMedium,
			fontWeight = FontWeight.Bold,
		)
	}
}

@Composable
private fun StatusPill(text: String) {
	Box(
		modifier = Modifier
			.background(
				color = CookCueSurfaceRaised,
				shape = RoundedCornerShape(50),
			)
			.padding(horizontal = 9.dp, vertical = 4.dp),
		contentAlignment = Alignment.Center,
	) {
		Text(
			text = text,
			color = CookCueWine,
			style = MaterialTheme.typography.labelSmall,
			fontWeight = FontWeight.ExtraBold,
			letterSpacing = 0.7.sp,
		)
	}
}

@Composable
private fun StepTitle(text: String) {
	Text(
		text = text,
		color = CookCueCream,
		style = MaterialTheme.typography.titleMedium,
		fontWeight = FontWeight.Bold,
		textAlign = TextAlign.Center,
		maxLines = 2,
		overflow = TextOverflow.Ellipsis,
		lineHeight = 19.sp,
	)
}

@Composable
private fun InstructionText(text: String) {
	Text(
		text = text,
		color = CookCueMuted,
		style = MaterialTheme.typography.bodySmall,
		textAlign = TextAlign.Center,
		maxLines = 2,
		overflow = TextOverflow.Ellipsis,
		lineHeight = 15.sp,
	)
}

@Composable
private fun HintText(text: String) {
	Box(
		modifier = Modifier
			.fillMaxWidth()
			.background(
				color = CookCueSurface,
				shape = RoundedCornerShape(12.dp),
			)
			.padding(horizontal = 10.dp, vertical = 7.dp),
		contentAlignment = Alignment.Center,
	) {
		Text(
			text = text,
			color = CookCueHerb,
			style = MaterialTheme.typography.labelSmall,
			textAlign = TextAlign.Center,
			maxLines = 2,
			overflow = TextOverflow.Ellipsis,
		)
	}
}

@Composable
private fun WearProgressDial(
	estimateSeconds: Long,
	elapsedSeconds: Long,
) {
	val remaining = (estimateSeconds - elapsedSeconds).coerceAtLeast(0)
	val overdue = elapsedSeconds > estimateSeconds
	val progress = if (estimateSeconds <= 0) {
		0f
	} else {
		(elapsedSeconds.toFloat() / estimateSeconds.toFloat()).coerceIn(0f, 1f)
	}
	val progressColor = if (overdue) CookCueHoney else CookCueWine

	Box(
		modifier = Modifier.size(104.dp),
		contentAlignment = Alignment.Center,
	) {
		Canvas(modifier = Modifier.fillMaxSize()) {
			drawCircle(
				color = CookCueLine,
				style = Stroke(width = 5.dp.toPx()),
			)
			drawArc(
				color = progressColor,
				startAngle = -90f,
				sweepAngle = 360f * progress,
				useCenter = false,
				style = Stroke(
					width = 5.dp.toPx(),
					cap = StrokeCap.Round,
				),
			)
		}
		Column(horizontalAlignment = Alignment.CenterHorizontally) {
			Text(
				text = if (overdue) {
					formatRemaining(elapsedSeconds)
				} else {
					formatRemaining(remaining)
				},
				color = CookCueCream,
				style = MaterialTheme.typography.titleLarge,
				fontWeight = FontWeight.Bold,
			)
			Text(
				text = if (overdue) "trvá" else "zostáva",
				color = CookCueMuted,
				style = MaterialTheme.typography.labelSmall,
			)
		}
	}
}

@Composable
private fun WearRemainingDial(remainingSeconds: Long) {
	Box(
		modifier = Modifier.size(112.dp),
		contentAlignment = Alignment.Center,
	) {
		Canvas(modifier = Modifier.fillMaxSize()) {
			drawCircle(
				color = CookCueLine,
				style = Stroke(width = 5.dp.toPx()),
			)
			drawArc(
				color = CookCueWine,
				startAngle = -90f,
				sweepAngle = 286f,
				useCenter = false,
				style = Stroke(
					width = 5.dp.toPx(),
					cap = StrokeCap.Round,
				),
			)
		}
		Column(horizontalAlignment = Alignment.CenterHorizontally) {
			Text(
				text = formatRemaining(remainingSeconds),
				color = CookCueCream,
				style = MaterialTheme.typography.titleLarge,
				fontWeight = FontWeight.Bold,
			)
			Text(
				text = "zostáva",
				color = CookCueMuted,
				style = MaterialTheme.typography.labelSmall,
			)
		}
	}
}

@Composable
private fun NextStepInfo(
	title: String,
	nextIn: Long,
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.background(
				color = CookCueSurface,
				shape = RoundedCornerShape(12.dp),
			)
			.padding(horizontal = 10.dp, vertical = 7.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Text(
			text = "ĎALEJ",
			color = CookCueWine,
			style = MaterialTheme.typography.labelSmall,
			fontWeight = FontWeight.Bold,
		)
		Spacer(Modifier.width(7.dp))
		Text(
			text = title,
			color = CookCueCream,
			style = MaterialTheme.typography.labelSmall,
			modifier = Modifier.weight(1f),
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
		)
		Text(
			text = "o " + formatRemaining(nextIn),
			color = CookCueMuted,
			style = MaterialTheme.typography.labelSmall,
		)
	}
}

@Composable
private fun WearNavigation(
	canPrevious: Boolean,
	canNext: Boolean,
	onPrevious: () -> Unit,
	onNext: () -> Unit,
) {
	Row(
		horizontalArrangement = Arrangement.spacedBy(18.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		NavButton(
			text = "←",
			enabled = canPrevious,
			onClick = onPrevious,
		)
		NavButton(
			text = "→",
			enabled = canNext,
			onClick = onNext,
		)
	}
}

@Composable
private fun NavButton(
	text: String,
	enabled: Boolean,
	onClick: () -> Unit,
) {
	Button(
		onClick = onClick,
		enabled = enabled,
		modifier = Modifier.size(42.dp),
		colors = ButtonDefaults.buttonColors(
			containerColor = CookCueSurfaceRaised,
			contentColor = CookCueCream,
		),
		shape = CircleShape,
	) {
		Text(
			text = text,
			style = MaterialTheme.typography.titleMedium,
			fontWeight = FontWeight.Bold,
		)
	}
}

@Composable
private fun PrimaryWearButton(
	text: String,
	onClick: () -> Unit,
) {
	Button(
		onClick = onClick,
		modifier = Modifier
			.fillMaxWidth()
			.height(42.dp),
		colors = ButtonDefaults.buttonColors(
			containerColor = CookCueWineDeep,
			contentColor = CookCueCream,
		),
		shape = RoundedCornerShape(16.dp),
	) {
		Text(
			text = text,
			fontWeight = FontWeight.Bold,
		)
	}
}

private fun formatRemaining(seconds: Long): String {
	val safe = seconds.coerceAtLeast(0)
	val minutes = safe / 60
	val remainder = safe % 60
	return when {
		minutes > 0 && remainder > 0 -> minutes.toString() + "m " + remainder + "s"
		minutes > 0 -> minutes.toString() + "m"
		else -> remainder.toString() + "s"
	}
}
