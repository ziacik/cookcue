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
	val currentRemaining = (state.currentEstimateSeconds - currentElapsed).coerceAtLeast(0)
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
				.padding(horizontal = 18.dp, vertical = 10.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center,
		) {
			WearBrandHeader()
			Spacer(Modifier.height(8.dp))

			when {
				!state.synced -> {
					StatusLabel("PRIPÁJAM")
					Text(
						text = "Hľadám telefón…",
						color = CookCueCream,
						style = MaterialTheme.typography.titleMedium,
						textAlign = TextAlign.Center,
					)
					Spacer(Modifier.height(10.dp))
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
					StatusLabel("PRIPRAVENÉ")
					Text(
						text = "Varenie ešte nie je spustené",
						color = CookCueCream,
						style = MaterialTheme.typography.titleMedium,
						textAlign = TextAlign.Center,
					)
					Spacer(Modifier.height(10.dp))
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
					StatusLabel("ČAKÁ NA TEBA")
					Text(
						text = state.eventTitle,
						color = CookCueCream,
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.Bold,
						textAlign = TextAlign.Center,
						maxLines = 2,
						overflow = TextOverflow.Ellipsis,
					)
					Spacer(Modifier.height(4.dp))
					Text(
						text = state.eventInstruction,
						color = CookCueMuted,
						style = MaterialTheme.typography.bodySmall,
						textAlign = TextAlign.Center,
						maxLines = 3,
						overflow = TextOverflow.Ellipsis,
					)
					if (state.eventTips.isNotBlank()) {
						Spacer(Modifier.height(5.dp))
						Text(
							text = "TIP · " + state.eventTips.replace("\n", " · "),
							color = CookCueHerb,
							style = MaterialTheme.typography.labelSmall,
							textAlign = TextAlign.Center,
							maxLines = 2,
							overflow = TextOverflow.Ellipsis,
						)
					}
					Spacer(Modifier.height(10.dp))
					if (
						state.eventRetryAfterSeconds > 0 &&
						state.eventRetryActionLabel.isNotBlank()
					) {
						Row(
							modifier = Modifier.fillMaxWidth(),
							horizontalArrangement = Arrangement.spacedBy(8.dp),
						) {
							Button(
								onClick = {
									WearActionSender.send(
										context,
										DataLayerProtocol.ACTION_DEFER_EVENT,
										state.eventTaskId,
									)
								},
								modifier = Modifier.weight(1f),
								colors = ButtonDefaults.buttonColors(
									containerColor = CookCueSand,
									contentColor = CookCueCream,
								),
								shape = RoundedCornerShape(18.dp),
							) {
								Text(
									text = state.eventRetryActionLabel,
									fontWeight = FontWeight.Bold,
								)
							}
							Button(
								onClick = {
									WearActionSender.send(
										context,
										DataLayerProtocol.ACTION_CONFIRM_EVENT,
										state.eventTaskId,
									)
								},
								modifier = Modifier.weight(1f),
								colors = ButtonDefaults.buttonColors(
									containerColor = CookCueBerry,
									contentColor = CookCueCream,
								),
								shape = RoundedCornerShape(18.dp),
							) {
								Text(
									text = state.eventActionLabel,
									fontWeight = FontWeight.Bold,
								)
							}
						}
						Spacer(Modifier.height(5.dp))
						Text(
							text = "NIE → znova o " +
								formatRemaining(state.eventRetryAfterSeconds),
							color = CookCueMuted,
							style = MaterialTheme.typography.labelSmall,
							textAlign = TextAlign.Center,
						)
					} else {
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
				}

				state.currentTitle.isNotBlank() -> {
					StatusLabel("TERAZ")
					Text(
						text = state.currentTitle,
						color = CookCueCream,
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.Bold,
						textAlign = TextAlign.Center,
						maxLines = 2,
						overflow = TextOverflow.Ellipsis,
					)
					Spacer(Modifier.height(4.dp))
					Text(
						text = state.currentInstruction,
						color = CookCueMuted,
						style = MaterialTheme.typography.bodySmall,
						textAlign = TextAlign.Center,
						maxLines = 3,
						overflow = TextOverflow.Ellipsis,
					)
					Spacer(Modifier.height(8.dp))
					WearTimerDial(
						estimateSeconds = state.currentEstimateSeconds,
						elapsedSeconds = currentElapsed,
					)
					Spacer(Modifier.height(8.dp))
					PrimaryWearButton(
						text = "HOTOVO",
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
					StatusLabel("BEŽÍ")
					Text(
						text = state.backgroundTitle,
						color = CookCueCream,
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.Bold,
						textAlign = TextAlign.Center,
						maxLines = 2,
						overflow = TextOverflow.Ellipsis,
					)
					Spacer(Modifier.height(8.dp))
					Text(
						text = formatRemaining(backgroundRemaining),
						color = CookCueBerry,
						style = MaterialTheme.typography.displaySmall,
						fontWeight = FontWeight.Bold,
					)
				}

				else -> {
					StatusLabel("POKOJ")
					Text(
						text = "Momentálne od teba nič netreba",
						color = CookCueCream,
						style = MaterialTheme.typography.titleMedium,
						textAlign = TextAlign.Center,
					)
				}
			}

			if (state.started) {
				Spacer(Modifier.height(10.dp))
				Row(
					horizontalArrangement = Arrangement.spacedBy(8.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					SecondaryWearButton(
						text = "←",
						enabled = state.canPrevious,
						onClick = {
							WearActionSender.send(
								context,
								DataLayerProtocol.ACTION_PREVIOUS,
							)
						},
					)
					SecondaryWearButton(
						text = "→",
						enabled = state.canNext,
						onClick = {
							WearActionSender.send(
								context,
								DataLayerProtocol.ACTION_NEXT,
							)
						},
					)
				}

				if (state.nextTitle.isNotBlank()) {
					Spacer(Modifier.height(8.dp))
					Text(
						text = "Ďalej · " + state.nextTitle + " · o " + formatRemaining(nextIn),
						color = CookCueMuted,
						style = MaterialTheme.typography.labelSmall,
						textAlign = TextAlign.Center,
						maxLines = 2,
						overflow = TextOverflow.Ellipsis,
					)
				}
			}
		}
	}
}

@Composable
private fun WearBrandHeader() {
	Row(
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(6.dp),
	) {
		Box(modifier = Modifier.size(22.dp)) {
			Canvas(modifier = Modifier.fillMaxSize()) {
				drawArc(
					color = CookCueBerry,
					startAngle = 45f,
					sweepAngle = 275f,
					useCenter = false,
					style = Stroke(
						width = 3.dp.toPx(),
						cap = StrokeCap.Round,
					),
				)
				drawCircle(
					color = CookCueHoney,
					radius = 2.2.dp.toPx(),
					center = Offset(this.size.width * 0.76f, this.size.height * 0.20f),
				)
			}
		}
		Text(
			text = "CookCue",
			color = CookCueCream,
			style = MaterialTheme.typography.labelLarge,
			fontWeight = FontWeight.Bold,
		)
	}
}

@Composable
private fun StatusLabel(text: String) {
	Text(
		text = text,
		color = CookCueBerry,
		style = MaterialTheme.typography.labelMedium,
		fontWeight = FontWeight.ExtraBold,
	)
	Spacer(Modifier.height(4.dp))
}

@Composable
private fun WearTimerDial(
	estimateSeconds: Long,
	elapsedSeconds: Long,
) {
	val remaining = (estimateSeconds - elapsedSeconds).coerceAtLeast(0)
	val progress = if (estimateSeconds <= 0) {
		0f
	} else {
		(elapsedSeconds.toFloat() / estimateSeconds.toFloat()).coerceIn(0f, 1f)
	}

	Box(
		modifier = Modifier.size(88.dp),
		contentAlignment = Alignment.Center,
	) {
		Canvas(modifier = Modifier.fillMaxSize()) {
			drawCircle(
				color = CookCueSand,
				style = Stroke(width = 6.dp.toPx()),
			)
			drawArc(
				color = if (elapsedSeconds > estimateSeconds) CookCueHoney else CookCueBerry,
				startAngle = -90f,
				sweepAngle = 360f * progress,
				useCenter = false,
				style = Stroke(
					width = 6.dp.toPx(),
					cap = StrokeCap.Round,
				),
			)
		}
		Column(horizontalAlignment = Alignment.CenterHorizontally) {
			Text(
				text = if (remaining > 0) {
					formatRemaining(remaining)
				} else {
					formatRemaining(elapsedSeconds)
				},
				color = CookCueCream,
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.Bold,
			)
			Text(
				text = if (remaining > 0) "ešte asi" else "trvá",
				color = CookCueMuted,
				style = MaterialTheme.typography.labelSmall,
			)
		}
	}
}

@Composable
private fun PrimaryWearButton(
	text: String,
	onClick: () -> Unit,
) {
	Button(
		onClick = onClick,
		modifier = Modifier.fillMaxWidth(),
		colors = ButtonDefaults.buttonColors(
			containerColor = CookCueBerry,
			contentColor = CookCueCream,
		),
		shape = RoundedCornerShape(18.dp),
	) {
		Text(
			text = text,
			fontWeight = FontWeight.Bold,
		)
	}
}

@Composable
private fun SecondaryWearButton(
	text: String,
	enabled: Boolean,
	onClick: () -> Unit,
) {
	Button(
		onClick = onClick,
		enabled = enabled,
		modifier = Modifier
			.width(58.dp)
			.height(38.dp),
		colors = ButtonDefaults.buttonColors(
			containerColor = CookCueSand,
			contentColor = CookCueCream,
		),
		shape = RoundedCornerShape(18.dp),
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
