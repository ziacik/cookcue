package com.ziacik.cookcue.mobile

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ziacik.cookcue.core.model.ScheduledTask
import com.ziacik.cookcue.core.model.TaskKind
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContent {
			MaterialTheme {
				CookCueScreen()
			}
		}
	}
}

@Composable
private fun CookCueScreen() {
	val context = LocalContext.current
	val recipe = CookingSessionController.recipe
	val startedAt = CookingSessionController.startedAt
	val durationOverrides = CookingSessionController.durationOverrides

	var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

	LaunchedEffect(startedAt) {
		while (startedAt != null) {
			now = SystemClock.elapsedRealtime()
			delay(500)
		}
	}

	LaunchedEffect(Unit) {
		MobileSessionPersistence.ensureLoaded(context)
		MobileSessionSync.publish(context)
	}

	val snapshot = remember(now, startedAt, durationOverrides) {
		CookingSessionController.snapshot(now)
	}

	fun persistAndSync() {
		MobileSessionPersistence.save(context)
		MobileSessionSync.publish(context)
	}

	Scaffold { padding ->
		LazyColumn(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding),
			contentPadding = PaddingValues(20.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			item {
				Text(
					text = recipe.title,
					style = MaterialTheme.typography.headlineMedium,
				)
				Spacer(Modifier.height(4.dp))
				Text(
					text = recipe.description,
					style = MaterialTheme.typography.bodyMedium,
				)
				Spacer(Modifier.height(16.dp))

				Card(modifier = Modifier.fillMaxWidth()) {
					Column(modifier = Modifier.padding(16.dp)) {
						Text(
							text = "Ingrediencie",
							style = MaterialTheme.typography.titleLarge,
						)
						Spacer(Modifier.height(8.dp))
						recipe.ingredients.forEach { ingredient ->
							Text("• " + ingredient.amount + " " + ingredient.name)
						}
					}
				}

				Spacer(Modifier.height(16.dp))

				if (!snapshot.started) {
					Text(
						text = "Pred varením: 120 g suchej fazule namoč na 8–12 hodín vo veľkom množstve studenej vody.",
						style = MaterialTheme.typography.bodyLarge,
					)
					Spacer(Modifier.height(12.dp))
					Button(
						onClick = {
							CookingSessionController.start()
							persistAndSync()
						},
						modifier = Modifier.fillMaxWidth(),
					) {
						Text("SPUSTIŤ VARENIE")
					}
				} else {
					Card(modifier = Modifier.fillMaxWidth()) {
						Column(modifier = Modifier.padding(18.dp)) {
							Text(
								text = "TERAZ",
								style = MaterialTheme.typography.labelLarge,
							)
							Spacer(Modifier.height(6.dp))
							Text(
								text = snapshot.currentAction?.task?.title ?: "Momentálne od teba nič netreba",
								style = MaterialTheme.typography.headlineSmall,
							)
							snapshot.currentAction?.let {
								Spacer(Modifier.height(4.dp))
								Text(it.task.instruction)
								it.task.tips.forEach { tip ->
									Spacer(Modifier.height(6.dp))
									Text(
										text = "Rada: " + tip,
										style = MaterialTheme.typography.bodyMedium,
									)
								}
								Spacer(Modifier.height(8.dp))
								Text("Plánovane ešte " + formatRemaining(it.endSeconds - snapshot.elapsedSeconds))
							}
						}
					}

					snapshot.pendingEvents.forEach { event ->
						Spacer(Modifier.height(10.dp))
						Card(modifier = Modifier.fillMaxWidth()) {
							Column(modifier = Modifier.padding(18.dp)) {
								Text(
									text = "ČAKÁM NA STAV",
									style = MaterialTheme.typography.labelLarge,
								)
								Spacer(Modifier.height(6.dp))
								Text(
									text = event.task.title,
									style = MaterialTheme.typography.titleLarge,
								)
								Spacer(Modifier.height(4.dp))
								Text(event.task.instruction)
								event.task.tips.forEach { tip ->
									Spacer(Modifier.height(6.dp))
									Text(
										text = "Rada: " + tip,
										style = MaterialTheme.typography.bodyMedium,
									)
								}
								Spacer(Modifier.height(12.dp))
								Button(
									onClick = {
										CookingSessionController.confirmEvent(event.task.id)
										persistAndSync()
									},
									modifier = Modifier.fillMaxWidth(),
								) {
									Text(event.task.actionLabel ?: "HOTOVO")
								}
							}
						}
					}

					Spacer(Modifier.height(10.dp))
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween,
					) {
						OutlinedButton(
							onClick = {
								CookingSessionController.previous()
								persistAndSync()
							},
							enabled = snapshot.previousAction != null,
						) {
							Text("← PREDOŠLÝ")
						}
						Button(
							onClick = {
								CookingSessionController.next()
								persistAndSync()
							},
							enabled = snapshot.nextAction != null,
						) {
							Text("ĎALŠÍ →")
						}
					}

					if (snapshot.background.isNotEmpty()) {
						Spacer(Modifier.height(10.dp))
						Text(
							text = snapshot.background.joinToString("\n") {
								it.task.title + ": " + formatRemaining(it.endSeconds - snapshot.elapsedSeconds)
							},
							style = MaterialTheme.typography.bodyMedium,
						)
					}

					snapshot.nextScheduled?.let {
						Spacer(Modifier.height(10.dp))
						Text(
							text = "Ďalej podľa plánu: " + it.task.title + " o " + formatRemaining(it.startSeconds - snapshot.elapsedSeconds),
							style = MaterialTheme.typography.bodyMedium,
						)
					}
				}

				Spacer(Modifier.height(18.dp))
				Text(
					text = "Plán",
					style = MaterialTheme.typography.titleLarge,
				)
			}

			items(snapshot.schedule, key = { it.task.id }) { item ->
				TimelineItem(item)
			}

			item {
				Spacer(Modifier.height(12.dp))
				Text(
					text = "Krízová pomoc",
					style = MaterialTheme.typography.titleLarge,
				)
			}

			items(recipe.troubleshooting, key = { it.problem }) { tip ->
				Card(modifier = Modifier.fillMaxWidth()) {
					Column(modifier = Modifier.padding(16.dp)) {
						Text(
							text = tip.problem,
							style = MaterialTheme.typography.titleMedium,
						)
						Spacer(Modifier.height(4.dp))
						Text(tip.advice)
					}
				}
			}
		}
	}
}

@Composable
private fun TimelineItem(item: ScheduledTask) {
	Card(modifier = Modifier.fillMaxWidth()) {
		Column(modifier = Modifier.padding(16.dp)) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
			) {
				Text(
					text = formatOffset(item.startSeconds),
					style = MaterialTheme.typography.labelLarge,
				)
				Text(
					text = if (item.task.kind == TaskKind.EVENT) {
						"odhad ~" + formatRemaining(item.endSeconds - item.startSeconds)
					} else {
						formatRemaining(item.endSeconds - item.startSeconds)
					},
					style = MaterialTheme.typography.labelMedium,
				)
			}

			Spacer(Modifier.height(6.dp))
			Text(
				text = item.task.title,
				style = MaterialTheme.typography.titleMedium,
			)
			Text(
				text = item.task.instruction,
				style = MaterialTheme.typography.bodyMedium,
			)
			item.task.tips.forEach { tip ->
				Spacer(Modifier.height(6.dp))
				Text(
					text = "Rada: " + tip,
					style = MaterialTheme.typography.bodySmall,
				)
			}
			if (item.task.kind == TaskKind.EVENT) {
				Spacer(Modifier.height(4.dp))
				Text(
					text = "Pokračovanie závisí od potvrdenia: " + item.task.actionLabel,
					style = MaterialTheme.typography.labelMedium,
				)
			}
		}
	}
}

private fun formatOffset(seconds: Long): String {
	val hours = seconds / 3600
	val minutes = (seconds % 3600) / 60
	return if (hours > 0) "T+" + hours + "h " + minutes + "m" else "T+" + minutes + "m"
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
