package com.ziacik.cookcue.wear

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
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
		while (state.started) {
			now = SystemClock.elapsedRealtime()
			delay(500)
		}
	}

	val elapsedSinceSync = if (state.receivedAtElapsedRealtime == 0L) {
		0
	} else {
		((now - state.receivedAtElapsedRealtime) / 1000).coerceAtLeast(0)
	}
	val currentRemaining = (state.currentRemainingSeconds - elapsedSinceSync).coerceAtLeast(0)
	val backgroundRemaining = (state.backgroundRemainingSeconds - elapsedSinceSync).coerceAtLeast(0)
	val nextIn = (state.nextInSeconds - elapsedSinceSync).coerceAtLeast(0)

	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(14.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center,
	) {
		Text("CookCue")
		Spacer(Modifier.height(6.dp))

		when {
			!state.synced -> {
				Text("Pripájam sa k telefónu…")
				Spacer(Modifier.height(8.dp))
				Button(
					onClick = {
						WearActionSender.send(
							context,
							DataLayerProtocol.ACTION_REQUEST_STATE,
						)
					},
				) {
					Text("Obnoviť")
				}
			}

			!state.started -> {
				Text("Varenie ešte nie je spustené")
				Spacer(Modifier.height(8.dp))
				Button(
					onClick = {
						WearActionSender.send(
							context,
							DataLayerProtocol.ACTION_START,
						)
					},
				) {
					Text("SPUSTIŤ")
				}
			}

			state.eventTaskId.isNotBlank() -> {
				Text(state.eventTitle)
				Spacer(Modifier.height(4.dp))
				Text(state.eventInstruction)
				if (state.eventTips.isNotBlank()) {
					Spacer(Modifier.height(4.dp))
					Text("Rada: " + state.eventTips.replace("\n", " · "))
				}
				Spacer(Modifier.height(8.dp))
				Button(
					onClick = {
						WearActionSender.send(
							context,
							DataLayerProtocol.ACTION_CONFIRM_EVENT,
							state.eventTaskId,
						)
					},
				) {
					Text(state.eventActionLabel)
				}
			}

			state.currentTitle.isNotBlank() -> {
				Text(state.currentTitle)
				Spacer(Modifier.height(4.dp))
				Text(state.currentInstruction)
				if (currentRemaining > 0) {
					Spacer(Modifier.height(4.dp))
					Text(formatRemaining(currentRemaining))
				}
				Spacer(Modifier.height(8.dp))
				Button(
					onClick = {
						WearActionSender.send(
							context,
							DataLayerProtocol.ACTION_NEXT,
						)
					},
					enabled = state.canNext,
				) {
					Text("HOTOVO")
				}
			}

			state.backgroundTitle.isNotBlank() -> {
				Text(state.backgroundTitle)
				Spacer(Modifier.height(4.dp))
				Text(formatRemaining(backgroundRemaining))
			}

			else -> {
				Text("Momentálne od teba nič netreba")
			}
		}

		if (state.started) {
			Spacer(Modifier.height(10.dp))
			Row(
				horizontalArrangement = Arrangement.spacedBy(6.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Button(
					onClick = {
						WearActionSender.send(
							context,
							DataLayerProtocol.ACTION_PREVIOUS,
						)
					},
					enabled = state.canPrevious,
				) {
					Text("←")
				}
				Button(
					onClick = {
						WearActionSender.send(
							context,
							DataLayerProtocol.ACTION_NEXT,
						)
					},
					enabled = state.canNext,
				) {
					Text("→")
				}
			}

			if (state.nextTitle.isNotBlank()) {
				Spacer(Modifier.height(6.dp))
				Text(
					"Ďalej: " + state.nextTitle + " o " + formatRemaining(nextIn)
				)
			}
		}
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
