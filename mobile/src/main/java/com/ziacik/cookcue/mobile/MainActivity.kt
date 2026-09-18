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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ziacik.cookcue.core.model.Recipe
import com.ziacik.cookcue.core.model.ScheduledTask
import com.ziacik.cookcue.core.model.TaskKind
import com.ziacik.cookcue.core.recipes.BeanSoupRecipe
import com.ziacik.cookcue.core.scheduler.Scheduler
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
	val recipe = BeanSoupRecipe.recipe
	var durationOverrides by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
	val schedule = remember(durationOverrides) {
		Scheduler().schedule(
			recipe = recipe,
			durationOverrides = durationOverrides,
		)
	}

	var startedAt by remember { mutableStateOf<Long?>(null) }
	var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

	LaunchedEffect(startedAt) {
		while (startedAt != null) {
			now = SystemClock.elapsedRealtime()
			delay(500)
		}
	}

	val elapsedSeconds = startedAt?.let { ((now - it) / 1000).coerceAtLeast(0) } ?: 0

	val pendingEvents = if (startedAt == null) {
		emptyList()
	} else {
		schedule.filter {
			it.task.kind == TaskKind.EVENT &&
				it.startSeconds <= elapsedSeconds &&
				it.task.id !in durationOverrides
		}
	}
	val blockedIds = blockedTaskIds(
		recipe = recipe,
		roots = pendingEvents.mapTo(mutableSetOf()) { it.task.id },
	)

	val running = if (startedAt == null) {
		emptyList()
	} else {
		schedule.filter {
			it.task.kind != TaskKind.EVENT &&
				it.task.id !in blockedIds &&
				elapsedSeconds in it.startSeconds until it.endSeconds
		}
	}

	val currentAction = running.firstOrNull {
		it.task.kind == TaskKind.ACTIVE &&
			it.task.resources.any { resource -> resource.resource == "cook" }
	}
	val background = running.filter { it !== currentAction }

	val actionSteps = schedule.filter {
		it.task.kind == TaskKind.ACTIVE &&
			it.task.resources.any { resource -> resource.resource == "cook" } &&
			it.task.id !in blockedIds
	}
	val navigationIndex = if (startedAt == null || actionSteps.isEmpty()) {
		-1
	} else {
		actionSteps.indexOfLast { it.startSeconds <= elapsedSeconds }.coerceAtLeast(0)
	}
	val previousAction = actionSteps.getOrNull(navigationIndex - 1)
	val nextAction = actionSteps.getOrNull(navigationIndex + 1)

	val nextScheduled = schedule.firstOrNull {
		it.task.kind != TaskKind.EVENT &&
			it.task.id !in blockedIds &&
			it.startSeconds > elapsedSeconds
	}

	val jumpTo: (ScheduledTask) -> Unit = { target ->
		val currentNow = SystemClock.elapsedRealtime()
		now = currentNow
		startedAt = currentNow - target.startSeconds * 1000
	}

	val confirmEvent: (ScheduledTask) -> Unit = { event ->
		val actualDuration = (elapsedSeconds - event.startSeconds).coerceAtLeast(1)
		durationOverrides = durationOverrides + (event.task.id to actualDuration)
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
					text = "120 g fazule · prvý CookCue scenár",
					style = MaterialTheme.typography.bodyMedium,
				)
				Spacer(Modifier.height(16.dp))

				if (startedAt == null) {
					Text(
						text = "Fazuľa má byť pred štartom už namočená aspoň 6 hodín.",
						style = MaterialTheme.typography.bodyLarge,
					)
					Spacer(Modifier.height(12.dp))
					Button(
						onClick = {
							durationOverrides = emptyMap()
							now = SystemClock.elapsedRealtime()
							startedAt = now
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
								text = currentAction?.task?.title ?: "Momentálne od teba nič netreba",
								style = MaterialTheme.typography.headlineSmall,
							)
							currentAction?.let {
								Spacer(Modifier.height(4.dp))
								Text(it.task.instruction)
								Spacer(Modifier.height(8.dp))
								Text("Plánovane ešte " + formatRemaining(it.endSeconds - elapsedSeconds))
							}
						}
					}

					pendingEvents.forEach { event ->
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
								Spacer(Modifier.height(12.dp))
								Button(
									onClick = { confirmEvent(event) },
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
							onClick = { previousAction?.let(jumpTo) },
							enabled = previousAction != null,
						) {
							Text("← PREDOŠLÝ")
						}
						Button(
							onClick = { nextAction?.let(jumpTo) },
							enabled = nextAction != null,
						) {
							Text("ĎALŠÍ →")
						}
					}

					if (background.isNotEmpty()) {
						Spacer(Modifier.height(10.dp))
						Text(
							text = background.joinToString("\n") {
								it.task.title + ": " + formatRemaining(it.endSeconds - elapsedSeconds)
							},
							style = MaterialTheme.typography.bodyMedium,
						)
					}

					nextScheduled?.let {
						Spacer(Modifier.height(10.dp))
						Text(
							text = "Ďalej podľa plánu: " + it.task.title + " o " + formatRemaining(it.startSeconds - elapsedSeconds),
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

			items(schedule, key = { it.task.id }) { item ->
				TimelineItem(item)
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
			if (item.task.kind == TaskKind.EVENT) {
				Spacer(Modifier.height(4.dp))
				Text(
					text = "Ďalší časovač začne až po potvrdení: " + item.task.actionLabel,
					style = MaterialTheme.typography.labelMedium,
				)
			}
		}
	}
}

private fun blockedTaskIds(
	recipe: Recipe,
	roots: Set<String>,
): Set<String> {
	if (roots.isEmpty()) {
		return emptySet()
	}

	val children = recipe.tasks
		.flatMap { task -> task.dependsOn.map { dependency -> dependency to task.id } }
		.groupBy({ it.first }, { it.second })

	val blocked = mutableSetOf<String>()
	val queue = ArrayDeque<String>()
	roots.forEach(queue::addLast)

	while (queue.isNotEmpty()) {
		val current = queue.removeFirst()
		children[current].orEmpty().forEach { child ->
			if (blocked.add(child)) {
				queue.addLast(child)
			}
		}
	}

	return blocked
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
