package com.ziacik.cookcue.mobile

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziacik.cookcue.core.model.ScheduledTask
import com.ziacik.cookcue.core.model.TaskKind
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContent {
			CookCueTheme {
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
	val primaryWait = snapshot.background.minByOrNull { it.endSeconds }
	val current = snapshot.currentAction
	val displayedWait = if (current == null) primaryWait else null
	val secondaryBackground = snapshot.background.filterNot {
		it.task.id == displayedWait?.task?.id
	}

	fun persistAndSync() {
		MobileSessionPersistence.save(context)
		MobileSessionSync.publish(context)
	}

	Scaffold(
		containerColor = MaterialTheme.colorScheme.background,
	) { padding ->
		LazyColumn(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding),
			contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
			verticalArrangement = Arrangement.spacedBy(18.dp),
		) {
			item {
				BrandHeader()
				Spacer(Modifier.height(18.dp))
				Text(
					text = recipe.title,
					style = MaterialTheme.typography.headlineLarge,
					color = MaterialTheme.colorScheme.onBackground,
				)
				Spacer(Modifier.height(6.dp))
				Text(
					text = recipe.description,
					style = MaterialTheme.typography.bodyLarge,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}

			if (!snapshot.started) {
				item {
					SectionHeading(
						kicker = "PRED VARENÍM",
						title = "Ingrediencie",
					)
					Spacer(Modifier.height(10.dp))
					IngredientList(
						ingredients = recipe.ingredients.map { it.amount to it.name },
					)
				}

				item {
					PreCookingPanel(
						onStart = {
							CookingSessionController.start()
							persistAndSync()
						},
					)
				}
			} else {
				item {
					when {
						current != null -> {
							val elapsed =
								(snapshot.elapsedSeconds - current.startSeconds).coerceAtLeast(0)
							NowPanel(
								title = current.task.title,
								instruction = current.task.instruction,
								tips = current.task.tips,
								estimateSeconds = current.task.durationSeconds,
								elapsedSeconds = elapsed,
								showDone = true,
								onDone = {
									CookingSessionController.completeAction(current.task.id)
									persistAndSync()
								},
							)
						}

						displayedWait != null -> {
							val elapsed =
								(snapshot.elapsedSeconds - displayedWait.startSeconds).coerceAtLeast(0)
							val duration =
								(displayedWait.endSeconds - displayedWait.startSeconds).coerceAtLeast(1)
							NowPanel(
								title = displayedWait.task.title,
								instruction = displayedWait.task.instruction,
								tips = displayedWait.task.tips,
								estimateSeconds = duration,
								elapsedSeconds = elapsed,
								showDone = false,
								onDone = {},
							)
						}

						else -> IdlePanel()
					}
				}

				items(snapshot.pendingEvents, key = { "event-" + it.task.id }) { event ->
					PendingEventPanel(
						title = event.task.title,
						instruction = event.task.instruction,
						tips = event.task.tips,
						actionLabel = event.task.actionLabel ?: "HOTOVO",
						onConfirm = {
							CookingSessionController.confirmEvent(event.task.id)
							persistAndSync()
						},
					)
				}

				if (secondaryBackground.isNotEmpty()) {
					item {
						BackgroundTimers(
							items = secondaryBackground.map {
								it.task.title to formatRemaining(
									it.endSeconds - snapshot.elapsedSeconds,
								)
							},
						)
					}
				}

				snapshot.nextScheduled?.let { next ->
					item {
						NextStepStrip(
							title = next.task.title,
							time = "o " + formatRemaining(
								next.startSeconds - snapshot.elapsedSeconds,
							),
						)
					}
				}

				item {
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.spacedBy(10.dp),
					) {
						OutlinedButton(
							onClick = {
								CookingSessionController.previous()
								persistAndSync()
							},
							enabled = snapshot.previousAction != null,
							modifier = Modifier.weight(1f),
							shape = RoundedCornerShape(16.dp),
						) {
							Text("←  PREDOŠLÝ")
						}
						Button(
							onClick = {
								CookingSessionController.next()
								persistAndSync()
							},
							enabled = snapshot.nextAction != null,
							modifier = Modifier.weight(1f),
							shape = RoundedCornerShape(16.dp),
						) {
							Text("ĎALŠÍ  →")
						}
					}
				}
			}

			item {
				SectionHeading(
					kicker = "KROK ZA KROKOM",
					title = "Plán",
				)
			}

			itemsIndexed(
				items = snapshot.schedule,
				key = { _, item -> "plan-" + item.task.id },
			) { index, item ->
				val activeTaskId = current?.task?.id ?: displayedWait?.task?.id
				TimelineItem(
					index = index,
					item = item,
					active = item.task.id == activeTaskId,
					isLast = index == snapshot.schedule.lastIndex,
				)
			}

			item {
				SectionHeading(
					kicker = "KEĎ SA NIEČO POKAZÍ",
					title = "Krízová pomoc",
				)
			}

			itemsIndexed(
				items = recipe.troubleshooting,
				key = { _, tip -> tip.problem },
			) { index, tip ->
				TroubleRow(
					title = tip.problem,
					advice = tip.advice,
				)
				if (index != recipe.troubleshooting.lastIndex) {
					HorizontalDivider(
						modifier = Modifier.padding(top = 14.dp),
						color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
					)
				}
			}

			item {
				Spacer(Modifier.height(8.dp))
			}
		}
	}
}

@Composable
private fun BrandHeader() {
	Row(
		modifier = Modifier.fillMaxWidth(),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.SpaceBetween,
	) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(11.dp),
		) {
			CueMark(38.dp)
			Column {
				Text(
					text = "CookCue",
					style = MaterialTheme.typography.titleLarge,
					color = MaterialTheme.colorScheme.onBackground,
				)
				Text(
					text = "varenie bez chaosu",
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
		}
		Text(
			text = "CC",
			style = MaterialTheme.typography.labelSmall,
			color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
			letterSpacing = 1.8.sp,
		)
	}
}

@Composable
private fun CueMark(size: Dp) {
	val primary = MaterialTheme.colorScheme.primary
	Box(modifier = Modifier.size(size)) {
		Canvas(modifier = Modifier.fillMaxSize()) {
			drawArc(
				color = primary,
				startAngle = 45f,
				sweepAngle = 275f,
				useCenter = false,
				style = Stroke(
					width = 4.5.dp.toPx(),
					cap = StrokeCap.Round,
				),
			)
			drawCircle(
				color = CookCueHoney,
				radius = 3.4.dp.toPx(),
				center = Offset(this.size.width * 0.76f, this.size.height * 0.20f),
			)
		}
	}
}

@Composable
private fun IngredientList(
	ingredients: List<Pair<String, String>>,
) {
	Column {
		ingredients.forEachIndexed { index, ingredient ->
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(vertical = 9.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Text(
					text = ingredient.first,
					style = MaterialTheme.typography.bodyMedium,
					fontWeight = FontWeight.SemiBold,
					color = MaterialTheme.colorScheme.primary,
					modifier = Modifier.width(96.dp),
				)
				Text(
					text = ingredient.second,
					style = MaterialTheme.typography.bodyLarge,
					color = MaterialTheme.colorScheme.onSurface,
				)
			}
			if (index != ingredients.lastIndex) {
				HorizontalDivider(
					color = MaterialTheme.colorScheme.outline.copy(alpha = 0.72f),
				)
			}
		}
	}
}

@Composable
private fun PreCookingPanel(
	onStart: () -> Unit,
) {
	Surface(
		modifier = Modifier.fillMaxWidth(),
		color = MaterialTheme.colorScheme.surfaceVariant,
		shape = RoundedCornerShape(26.dp),
	) {
		Column(modifier = Modifier.padding(22.dp)) {
			EditorialLabel("PRIPRAV SI NÁSkok".uppercase())
			Spacer(Modifier.height(8.dp))
			Text(
				text = "Fazuľu namoč vopred",
				style = MaterialTheme.typography.headlineSmall,
			)
			Spacer(Modifier.height(6.dp))
			Text(
				text = "120 g suchej fazule namoč na 8–12 hodín vo veľkom množstve studenej vody.",
				style = MaterialTheme.typography.bodyLarge,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
			Spacer(Modifier.height(18.dp))
			Button(
				onClick = onStart,
				modifier = Modifier.fillMaxWidth(),
				colors = ButtonDefaults.buttonColors(
					containerColor = MaterialTheme.colorScheme.primary,
				),
				shape = RoundedCornerShape(18.dp),
			) {
				Text(
					text = "SPUSTIŤ VARENIE",
					fontWeight = FontWeight.Bold,
					modifier = Modifier.padding(vertical = 4.dp),
				)
			}
		}
	}
}

@Composable
private fun NowPanel(
	title: String,
	instruction: String,
	tips: List<String>,
	estimateSeconds: Long,
	elapsedSeconds: Long,
	showDone: Boolean,
	onDone: () -> Unit,
) {
	Surface(
		modifier = Modifier.fillMaxWidth(),
		color = MaterialTheme.colorScheme.surface,
		shape = RoundedCornerShape(30.dp),
		border = BorderStroke(
			1.dp,
			MaterialTheme.colorScheme.outline.copy(alpha = 0.9f),
		),
		shadowElevation = 3.dp,
	) {
		Column(modifier = Modifier.padding(24.dp)) {
			Box(
				modifier = Modifier
					.width(46.dp)
					.height(4.dp)
					.background(
						MaterialTheme.colorScheme.primary,
						RoundedCornerShape(99.dp),
					),
			)
			Spacer(Modifier.height(17.dp))
			EditorialLabel("TERAZ")
			Spacer(Modifier.height(8.dp))
			Text(
				text = title,
				style = MaterialTheme.typography.headlineLarge,
				color = MaterialTheme.colorScheme.onSurface,
			)
			Spacer(Modifier.height(7.dp))
			Text(
				text = instruction,
				style = MaterialTheme.typography.bodyLarge,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
			Spacer(Modifier.height(22.dp))

			Box(
				modifier = Modifier.fillMaxWidth(),
				contentAlignment = Alignment.Center,
			) {
				TimerDial(
					estimateSeconds = estimateSeconds,
					elapsedSeconds = elapsedSeconds,
				)
			}

			if (tips.isNotEmpty()) {
				Spacer(Modifier.height(22.dp))
				Column(
					verticalArrangement = Arrangement.spacedBy(9.dp),
				) {
					tips.forEach { tip ->
						Row(
							horizontalArrangement = Arrangement.spacedBy(10.dp),
							verticalAlignment = Alignment.Top,
						) {
							Box(
								modifier = Modifier
									.padding(top = 7.dp)
									.size(5.dp)
									.background(
										MaterialTheme.colorScheme.tertiary,
										CircleShape,
									),
							)
							Text(
								text = tip,
								style = MaterialTheme.typography.bodyMedium,
								color = MaterialTheme.colorScheme.onSurfaceVariant,
							)
						}
					}
				}
			}

			if (showDone) {
				Spacer(Modifier.height(22.dp))
				Button(
					onClick = onDone,
					modifier = Modifier.fillMaxWidth(),
					colors = ButtonDefaults.buttonColors(
						containerColor = MaterialTheme.colorScheme.primary,
					),
					shape = RoundedCornerShape(18.dp),
				) {
					Text(
						text = "✓  HOTOVO",
						fontWeight = FontWeight.Bold,
						modifier = Modifier.padding(vertical = 5.dp),
					)
				}
			}
		}
	}
}

@Composable
private fun TimerDial(
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
	val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.46f)
	val progressColor = if (overdue) {
		MaterialTheme.colorScheme.tertiary
	} else {
		MaterialTheme.colorScheme.primary
	}

	Box(
		modifier = Modifier.size(154.dp),
		contentAlignment = Alignment.Center,
	) {
		Canvas(modifier = Modifier.fillMaxSize()) {
			drawCircle(
				color = track,
				style = Stroke(width = 8.dp.toPx()),
			)
			drawArc(
				color = progressColor,
				startAngle = -90f,
				sweepAngle = 360f * progress,
				useCenter = false,
				style = Stroke(
					width = 8.dp.toPx(),
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
				style = MaterialTheme.typography.displaySmall,
				color = progressColor,
			)
			Spacer(Modifier.height(1.dp))
			Text(
				text = if (overdue) "trvá" else "zostáva",
				style = MaterialTheme.typography.labelMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				letterSpacing = 0.4.sp,
			)
			Text(
				text = "odhad " + formatRemaining(estimateSeconds),
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
			)
		}
	}
}

@Composable
private fun IdlePanel() {
	Surface(
		modifier = Modifier.fillMaxWidth(),
		color = MaterialTheme.colorScheme.surfaceVariant,
		shape = RoundedCornerShape(24.dp),
	) {
		Column(modifier = Modifier.padding(22.dp)) {
			EditorialLabel("TERAZ")
			Spacer(Modifier.height(7.dp))
			Text(
				text = "Momentálne od teba nič netreba",
				style = MaterialTheme.typography.headlineSmall,
			)
			Spacer(Modifier.height(4.dp))
			Text(
				text = "CookCue stráži čas. Ty môžeš na chvíľu vypnúť.",
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}
	}
}

@Composable
private fun PendingEventPanel(
	title: String,
	instruction: String,
	tips: List<String>,
	actionLabel: String,
	onConfirm: () -> Unit,
) {
	Surface(
		modifier = Modifier.fillMaxWidth(),
		color = MaterialTheme.colorScheme.tertiaryContainer,
		shape = RoundedCornerShape(24.dp),
	) {
		Column(modifier = Modifier.padding(20.dp)) {
			EditorialLabel(
				text = "ČAKÁ NA TEBA",
				color = MaterialTheme.colorScheme.tertiary,
			)
			Spacer(Modifier.height(7.dp))
			Text(
				text = title,
				style = MaterialTheme.typography.headlineSmall,
			)
			Spacer(Modifier.height(5.dp))
			Text(
				text = instruction,
				style = MaterialTheme.typography.bodyLarge,
			)
			tips.forEach { tip ->
				Spacer(Modifier.height(8.dp))
				Text(
					text = "• " + tip,
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onTertiaryContainer,
				)
			}
			Spacer(Modifier.height(16.dp))
			Button(
				onClick = onConfirm,
				modifier = Modifier.fillMaxWidth(),
				shape = RoundedCornerShape(17.dp),
			) {
				Text(
					text = actionLabel,
					fontWeight = FontWeight.Bold,
				)
			}
		}
	}
}

@Composable
private fun BackgroundTimers(
	items: List<Pair<String, String>>,
) {
	Surface(
		modifier = Modifier.fillMaxWidth(),
		color = MaterialTheme.colorScheme.surfaceVariant,
		shape = RoundedCornerShape(22.dp),
	) {
		Column(modifier = Modifier.padding(18.dp)) {
			EditorialLabel(
				text = "BEŽÍ NA POZADÍ",
				color = MaterialTheme.colorScheme.secondary,
			)
			Spacer(Modifier.height(10.dp))
			items.forEachIndexed { index, item ->
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(vertical = 5.dp),
					horizontalArrangement = Arrangement.SpaceBetween,
				) {
					Text(
						text = item.first,
						style = MaterialTheme.typography.bodyMedium,
						modifier = Modifier.weight(1f),
					)
					Spacer(Modifier.width(14.dp))
					Text(
						text = item.second,
						style = MaterialTheme.typography.bodyMedium,
						fontWeight = FontWeight.Bold,
						color = MaterialTheme.colorScheme.secondary,
					)
				}
				if (index != items.lastIndex) {
					HorizontalDivider(
						color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
					)
				}
			}
		}
	}
}

@Composable
private fun NextStepStrip(
	title: String,
	time: String,
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(vertical = 2.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Box(
			modifier = Modifier
				.width(3.dp)
				.height(42.dp)
				.background(
					MaterialTheme.colorScheme.primary,
					RoundedCornerShape(99.dp),
				),
		)
		Spacer(Modifier.width(12.dp))
		Column(modifier = Modifier.weight(1f)) {
			EditorialLabel("ĎALEJ")
			Spacer(Modifier.height(2.dp))
			Text(
				text = title,
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.SemiBold,
			)
		}
		Text(
			text = time,
			style = MaterialTheme.typography.bodyMedium,
			fontWeight = FontWeight.Bold,
			color = MaterialTheme.colorScheme.primary,
		)
	}
}

@Composable
private fun SectionHeading(
	kicker: String,
	title: String,
) {
	Column {
		EditorialLabel(kicker)
		Spacer(Modifier.height(5.dp))
		Text(
			text = title,
			style = MaterialTheme.typography.headlineMedium,
			color = MaterialTheme.colorScheme.onBackground,
		)
		Spacer(Modifier.height(8.dp))
		HorizontalDivider(
			color = MaterialTheme.colorScheme.outline,
		)
	}
}

@Composable
private fun EditorialLabel(
	text: String,
	color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
	Text(
		text = text,
		style = MaterialTheme.typography.labelMedium,
		fontWeight = FontWeight.Bold,
		color = color,
		letterSpacing = 1.25.sp,
	)
}

@Composable
private fun TimelineItem(
	index: Int,
	item: ScheduledTask,
	active: Boolean,
	isLast: Boolean,
) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		verticalAlignment = Alignment.Top,
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			Box(
				modifier = Modifier
					.size(30.dp)
					.background(
						if (active) {
							MaterialTheme.colorScheme.primary
						} else {
							MaterialTheme.colorScheme.surfaceVariant
						},
						CircleShape,
					),
				contentAlignment = Alignment.Center,
			) {
				Text(
					text = (index + 1).toString(),
					style = MaterialTheme.typography.labelMedium,
					fontWeight = FontWeight.Bold,
					color = if (active) {
						MaterialTheme.colorScheme.onPrimary
					} else {
						MaterialTheme.colorScheme.onSurfaceVariant
					},
				)
			}
			if (!isLast) {
				Box(
					modifier = Modifier
						.width(1.dp)
						.height(74.dp)
						.background(MaterialTheme.colorScheme.outline),
				)
			}
		}

		Spacer(Modifier.width(14.dp))

		Column(
			modifier = Modifier
				.weight(1f)
				.padding(bottom = if (isLast) 0.dp else 14.dp),
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically,
			) {
				Text(
					text = formatOffset(item.startSeconds),
					style = MaterialTheme.typography.labelMedium,
					fontWeight = FontWeight.Bold,
					color = if (active) {
						MaterialTheme.colorScheme.primary
					} else {
						MaterialTheme.colorScheme.onSurfaceVariant
					},
				)
				Text(
					text = if (item.task.kind == TaskKind.EVENT) {
						"odhad ~" + formatRemaining(item.endSeconds - item.startSeconds)
					} else {
						formatRemaining(item.endSeconds - item.startSeconds)
					},
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
			Spacer(Modifier.height(4.dp))
			Text(
				text = item.task.title,
				style = MaterialTheme.typography.titleMedium,
				fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
				color = MaterialTheme.colorScheme.onSurface,
			)
			Spacer(Modifier.height(3.dp))
			Text(
				text = item.task.instruction,
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
			if (item.task.kind == TaskKind.EVENT) {
				Spacer(Modifier.height(5.dp))
				Text(
					text = "Pokračovanie čaká na potvrdenie",
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.tertiary,
				)
			}
		}
	}
}

@Composable
private fun TroubleRow(
	title: String,
	advice: String,
) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(vertical = 2.dp),
	) {
		Row(
			modifier = Modifier.fillMaxWidth(),
			verticalAlignment = Alignment.CenterVertically,
		) {
			Box(
				modifier = Modifier
					.size(8.dp)
					.background(MaterialTheme.colorScheme.primary, CircleShape),
			)
			Spacer(Modifier.width(11.dp))
			Text(
				text = title,
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.SemiBold,
				modifier = Modifier.weight(1f),
			)
		}
		Spacer(Modifier.height(6.dp))
		Text(
			text = advice,
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			modifier = Modifier.padding(start = 19.dp),
		)
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
