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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
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
	val selectedRecipeId = CookingSessionController.selectedRecipeId
	val startedAt = CookingSessionController.startedAt
	val durationOverrides = CookingSessionController.durationOverrides
	val eventDeferredUntil = CookingSessionController.eventDeferredUntil
	val userActionVersion = CookingSessionController.userActionVersion

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

	val snapshot = remember(
		now,
		startedAt,
		durationOverrides,
		eventDeferredUntil,
		selectedRecipeId,
	) {
		CookingSessionController.snapshot(now)
	}
	val primaryWait = snapshot.background.minByOrNull { it.endSeconds }
	val transitionCue = snapshot.transitionCue()
	var transitionInitialized by remember { mutableStateOf(false) }
	var previousTransitionKey by remember { mutableStateOf<String?>(null) }
	var previousUserActionVersion by remember { mutableLongStateOf(userActionVersion) }

	LaunchedEffect(snapshot.started, transitionCue?.key, userActionVersion) {
		if (!snapshot.started) {
			transitionInitialized = false
			previousTransitionKey = null
			previousUserActionVersion = userActionVersion
			return@LaunchedEffect
		}

		if (!transitionInitialized) {
			transitionInitialized = true
			previousTransitionKey = transitionCue?.key
			previousUserActionVersion = userActionVersion
			return@LaunchedEffect
		}

		val changedByUser = userActionVersion != previousUserActionVersion
		val changedStep = transitionCue?.key != previousTransitionKey
		if (changedStep && !changedByUser && transitionCue != null) {
			val transitionId = System.currentTimeMillis()
			MobileTransitionNotifier.notify(context, transitionCue)
			MobileSessionSync.publish(
				context,
				TransitionSignal(
					id = transitionId,
					title = transitionCue.title,
					text = transitionCue.text,
				),
			)
		}

		previousTransitionKey = transitionCue?.key
		previousUserActionVersion = userActionVersion
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
			contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
			verticalArrangement = Arrangement.spacedBy(14.dp),
		) {
			item {
				BrandHeader()

				if (!snapshot.started) {
					Spacer(Modifier.height(16.dp))
					Text(
						text = "Recept",
						style = MaterialTheme.typography.labelLarge,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						fontWeight = FontWeight.Bold,
					)
					Spacer(Modifier.height(8.dp))
					CookingSessionController.availableRecipes.forEach { option ->
						if (option.id == selectedRecipeId) {
							Button(
								onClick = {},
								modifier = Modifier.fillMaxWidth(),
								shape = RoundedCornerShape(14.dp),
							) {
								Text(option.title)
							}
						} else {
							OutlinedButton(
								onClick = {
									CookingSessionController.selectRecipe(option.id)
									persistAndSync()
								},
								modifier = Modifier.fillMaxWidth(),
								shape = RoundedCornerShape(14.dp),
							) {
								Text(option.title)
							}
						}
						Spacer(Modifier.height(6.dp))
					}
				}

				Spacer(Modifier.height(10.dp))
				Text(
					text = recipe.title,
					style = MaterialTheme.typography.headlineMedium,
					fontWeight = FontWeight.Bold,
				)
				Spacer(Modifier.height(4.dp))
				Text(
					text = recipe.description,
					style = MaterialTheme.typography.bodyLarge,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}

			item {
				Card(
					modifier = Modifier.fillMaxWidth(),
					colors = CardDefaults.cardColors(
						containerColor = MaterialTheme.colorScheme.surface,
					),
					border = BorderStroke(
						1.dp,
						MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
					),
					shape = RoundedCornerShape(22.dp),
				) {
					Column(modifier = Modifier.padding(18.dp)) {
						Text(
							text = "Ingrediencie",
							style = MaterialTheme.typography.titleLarge,
							fontWeight = FontWeight.SemiBold,
						)
						Spacer(Modifier.height(10.dp))
						recipe.ingredients.forEachIndexed { index, ingredient ->
							Row(
								modifier = Modifier.fillMaxWidth(),
								verticalAlignment = Alignment.CenterVertically,
							) {
								Box(
									modifier = Modifier
										.size(7.dp)
										.background(
											MaterialTheme.colorScheme.primary,
											CircleShape,
										),
								)
								Spacer(Modifier.width(10.dp))
								Text(
									text = ingredient.amount,
									style = MaterialTheme.typography.bodyMedium,
									fontWeight = FontWeight.SemiBold,
									modifier = Modifier.width(92.dp),
								)
								Text(
									text = ingredient.name,
									style = MaterialTheme.typography.bodyMedium,
								)
							}
							if (index != recipe.ingredients.lastIndex) {
								Spacer(Modifier.height(8.dp))
							}
						}
					}
				}
			}

			if (!snapshot.started) {
				item {
					Card(
						modifier = Modifier.fillMaxWidth(),
						colors = CardDefaults.cardColors(
							containerColor = MaterialTheme.colorScheme.secondaryContainer,
						),
						shape = RoundedCornerShape(22.dp),
					) {
						Column(modifier = Modifier.padding(18.dp)) {
							recipe.preCookingNote?.let { note ->
								Text(
									text = "PRED VARENÍM",
									style = MaterialTheme.typography.labelLarge,
									color = MaterialTheme.colorScheme.secondary,
									fontWeight = FontWeight.Bold,
								)
								Spacer(Modifier.height(6.dp))
								Text(
									text = note,
									style = MaterialTheme.typography.bodyLarge,
								)
								Spacer(Modifier.height(16.dp))
							}
							Button(
								onClick = {
									CookingSessionController.start()
									persistAndSync()
								},
								modifier = Modifier.fillMaxWidth(),
								colors = ButtonDefaults.buttonColors(
									containerColor = MaterialTheme.colorScheme.primary,
								),
								shape = RoundedCornerShape(16.dp),
							) {
								Text(
									text = "SPUSTIŤ VARENIE",
									fontWeight = FontWeight.Bold,
								)
							}
						}
					}
				}
			} else {
				item {
					val current = snapshot.currentAction

					Card(
						modifier = Modifier.fillMaxWidth(),
						colors = CardDefaults.cardColors(
							containerColor = MaterialTheme.colorScheme.primaryContainer,
						),
						shape = RoundedCornerShape(26.dp),
					) {
						Column(modifier = Modifier.padding(20.dp)) {
							Text(
								text = "TERAZ",
								style = MaterialTheme.typography.labelLarge,
								color = MaterialTheme.colorScheme.primary,
								fontWeight = FontWeight.ExtraBold,
							)
							Spacer(Modifier.height(8.dp))

							if (current == null) {
								if (primaryWait == null) {
									Text(
										text = "Momentálne od teba nič netreba",
										style = MaterialTheme.typography.titleLarge,
										fontWeight = FontWeight.SemiBold,
									)
								} else {
									val waitElapsed =
										(snapshot.elapsedSeconds - primaryWait.startSeconds).coerceAtLeast(0)
									val waitDuration =
										(primaryWait.endSeconds - primaryWait.startSeconds).coerceAtLeast(1)

									Row(
										modifier = Modifier.fillMaxWidth(),
										horizontalArrangement = Arrangement.spacedBy(16.dp),
										verticalAlignment = Alignment.CenterVertically,
									) {
										Column(modifier = Modifier.weight(1f)) {
											Text(
												text = primaryWait.task.title,
												style = MaterialTheme.typography.headlineSmall,
												fontWeight = FontWeight.Bold,
											)
											Spacer(Modifier.height(6.dp))
											Text(
												text = primaryWait.task.instruction,
												style = MaterialTheme.typography.bodyLarge,
											)
										}
										TimerDial(
											estimateSeconds = waitDuration,
											elapsedSeconds = waitElapsed,
										)
									}

									primaryWait.task.tips.forEach { tip ->
										Spacer(Modifier.height(10.dp))
										Text(
											text = "TIP  $tip",
											style = MaterialTheme.typography.bodyMedium,
											color = MaterialTheme.colorScheme.secondary,
										)
									}
								}
							} else {
								val actionElapsed =
									(snapshot.elapsedSeconds - current.startSeconds).coerceAtLeast(0)

								Row(
									modifier = Modifier.fillMaxWidth(),
									horizontalArrangement = Arrangement.spacedBy(16.dp),
									verticalAlignment = Alignment.CenterVertically,
								) {
									Column(modifier = Modifier.weight(1f)) {
										Text(
											text = current.task.title,
											style = MaterialTheme.typography.headlineSmall,
											fontWeight = FontWeight.Bold,
										)
										Spacer(Modifier.height(6.dp))
										Text(
											text = current.task.instruction,
											style = MaterialTheme.typography.bodyLarge,
										)
									}
									TimerDial(
										estimateSeconds = current.task.durationSeconds,
										elapsedSeconds = actionElapsed,
									)
								}

								current.task.tips.forEach { tip ->
									Spacer(Modifier.height(10.dp))
									Surface(
										color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
										shape = RoundedCornerShape(14.dp),
									) {
										Row(
											modifier = Modifier.padding(
												horizontal = 12.dp,
												vertical = 10.dp,
											),
											horizontalArrangement = Arrangement.spacedBy(8.dp),
										) {
											Text(
												text = "TIP",
												style = MaterialTheme.typography.labelMedium,
												color = MaterialTheme.colorScheme.primary,
												fontWeight = FontWeight.Bold,
											)
											Text(
												text = tip,
												style = MaterialTheme.typography.bodyMedium,
											)
										}
									}
								}

								Spacer(Modifier.height(16.dp))
								Button(
									onClick = {
										CookingSessionController.completeAction(current.task.id)
										persistAndSync()
									},
									modifier = Modifier.fillMaxWidth(),
									colors = ButtonDefaults.buttonColors(
										containerColor = MaterialTheme.colorScheme.primary,
									),
									shape = RoundedCornerShape(16.dp),
								) {
									Text(
										text = "HOTOVO",
										fontWeight = FontWeight.ExtraBold,
									)
								}
							}
						}
					}
				}

				items(snapshot.pendingEvents, key = { "event-" + it.task.id }) { event ->
					Card(
						modifier = Modifier.fillMaxWidth(),
						colors = CardDefaults.cardColors(
							containerColor = MaterialTheme.colorScheme.tertiaryContainer,
						),
						shape = RoundedCornerShape(22.dp),
					) {
						Column(modifier = Modifier.padding(18.dp)) {
							Text(
								text = "ČAKÁ NA TEBA",
								style = MaterialTheme.typography.labelLarge,
								color = MaterialTheme.colorScheme.tertiary,
								fontWeight = FontWeight.Bold,
							)
							Spacer(Modifier.height(6.dp))
							Text(
								text = event.task.title,
								style = MaterialTheme.typography.titleLarge,
								fontWeight = FontWeight.Bold,
							)
							Spacer(Modifier.height(4.dp))
							Text(event.task.instruction)
							event.task.tips.forEach { tip ->
								Spacer(Modifier.height(8.dp))
								Text(
									text = "TIP  $tip",
									style = MaterialTheme.typography.bodyMedium,
									color = MaterialTheme.colorScheme.onTertiaryContainer,
								)
							}
							Spacer(Modifier.height(14.dp))
							val retryAfterSeconds = event.task.retryAfterSeconds
							if (retryAfterSeconds != null) {
								Row(
									modifier = Modifier.fillMaxWidth(),
									horizontalArrangement = Arrangement.spacedBy(10.dp),
								) {
									OutlinedButton(
										onClick = {
											CookingSessionController.deferEvent(event.task.id)
											persistAndSync()
										},
										modifier = Modifier.weight(1f),
										shape = RoundedCornerShape(16.dp),
									) {
										Text(
											text = event.task.retryActionLabel ?: "NIE",
											fontWeight = FontWeight.Bold,
										)
									}
									Button(
										onClick = {
											CookingSessionController.confirmEvent(event.task.id)
											persistAndSync()
										},
										modifier = Modifier.weight(1f),
										shape = RoundedCornerShape(16.dp),
									) {
										Text(
											text = event.task.actionLabel ?: "ÁNO",
											fontWeight = FontWeight.Bold,
										)
									}
								}
								Spacer(Modifier.height(8.dp))
								Text(
									text = "Ak ešte nie, skontrolujeme znova o " +
										formatRemaining(retryAfterSeconds) + ".",
									style = MaterialTheme.typography.bodySmall,
									color = MaterialTheme.colorScheme.onTertiaryContainer,
								)
							} else {
								Button(
									onClick = {
										CookingSessionController.confirmEvent(event.task.id)
										persistAndSync()
									},
									modifier = Modifier.fillMaxWidth(),
									shape = RoundedCornerShape(16.dp),
								) {
									Text(
										text = event.task.actionLabel ?: "HOTOVO",
										fontWeight = FontWeight.Bold,
									)
								}
							}
						}
					}
				}

				val secondaryBackground = snapshot.background.filterNot {
					it.task.id == primaryWait?.task?.id
				}
				if (secondaryBackground.isNotEmpty()) {
					item {
						Card(
							modifier = Modifier.fillMaxWidth(),
							colors = CardDefaults.cardColors(
								containerColor = MaterialTheme.colorScheme.secondaryContainer,
							),
							shape = RoundedCornerShape(18.dp),
						) {
							Column(modifier = Modifier.padding(16.dp)) {
								Text(
									text = "BEŽÍ NA POZADÍ",
									style = MaterialTheme.typography.labelMedium,
									color = MaterialTheme.colorScheme.secondary,
									fontWeight = FontWeight.Bold,
								)
								Spacer(Modifier.height(8.dp))
								secondaryBackground.forEachIndexed { index, background ->
									Row(
										modifier = Modifier.fillMaxWidth(),
										horizontalArrangement = Arrangement.SpaceBetween,
									) {
										Text(
											text = background.task.title,
											style = MaterialTheme.typography.bodyMedium,
											modifier = Modifier.weight(1f),
										)
										Spacer(Modifier.width(12.dp))
										Text(
											text = formatRemaining(
												background.endSeconds - snapshot.elapsedSeconds,
											),
											style = MaterialTheme.typography.bodyMedium,
											fontWeight = FontWeight.Bold,
										)
									}
									if (index != secondaryBackground.lastIndex) {
										Spacer(Modifier.height(7.dp))
									}
								}
							}
						}
					}
				}

				snapshot.nextScheduled?.let { next ->
					item {
						Surface(
							modifier = Modifier.fillMaxWidth(),
							color = MaterialTheme.colorScheme.surfaceVariant,
							shape = RoundedCornerShape(16.dp),
						) {
							Row(
								modifier = Modifier.padding(14.dp),
								horizontalArrangement = Arrangement.spacedBy(10.dp),
								verticalAlignment = Alignment.CenterVertically,
							) {
								Text(
									text = "ĎALEJ",
									style = MaterialTheme.typography.labelMedium,
									color = MaterialTheme.colorScheme.primary,
									fontWeight = FontWeight.Bold,
								)
								Text(
									text = next.task.title,
									style = MaterialTheme.typography.bodyMedium,
									modifier = Modifier.weight(1f),
								)
								Text(
									text = "o " + formatRemaining(
										next.startSeconds - snapshot.elapsedSeconds,
									),
									style = MaterialTheme.typography.bodyMedium,
									fontWeight = FontWeight.Bold,
								)
							}
						}
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
							shape = RoundedCornerShape(14.dp),
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
							shape = RoundedCornerShape(14.dp),
						) {
							Text("ĎALŠÍ  →")
						}
					}
				}
			}

			item {
				SectionHeading("Plán")
			}

			items(snapshot.schedule, key = { "plan-" + it.task.id }) { item ->
				TimelineItem(item)
			}

			item {
				SectionHeading("Krízová pomoc")
			}

			items(recipe.troubleshooting, key = { it.problem }) { tip ->
				Card(
					modifier = Modifier.fillMaxWidth(),
					colors = CardDefaults.cardColors(
						containerColor = MaterialTheme.colorScheme.surface,
					),
					border = BorderStroke(
						1.dp,
						MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
					),
					shape = RoundedCornerShape(18.dp),
				) {
					Column(modifier = Modifier.padding(16.dp)) {
						Text(
							text = tip.problem,
							style = MaterialTheme.typography.titleMedium,
							fontWeight = FontWeight.SemiBold,
						)
						Spacer(Modifier.height(5.dp))
						Text(
							text = tip.advice,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
						)
					}
				}
			}
		}
	}
}

@Composable
private fun BrandHeader() {
	Row(
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(10.dp),
	) {
		CueMark(34.dp)
		Column {
			Text(
				text = "CookCue",
				style = MaterialTheme.typography.titleLarge,
				fontWeight = FontWeight.ExtraBold,
			)
			Text(
				text = "varenie bez chaosu",
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}
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
					width = 4.dp.toPx(),
					cap = StrokeCap.Round,
				),
			)
			drawCircle(
				color = CookCueHoney,
				radius = 3.2.dp.toPx(),
				center = Offset(this.size.width * 0.76f, this.size.height * 0.20f),
			)
		}
	}
}

@Composable
private fun TimerDial(
	estimateSeconds: Long,
	elapsedSeconds: Long,
) {
	val remaining = (estimateSeconds - elapsedSeconds).coerceAtLeast(0)
	val progress = if (estimateSeconds <= 0) {
		0f
	} else {
		(elapsedSeconds.toFloat() / estimateSeconds.toFloat()).coerceIn(0f, 1f)
	}
	val track = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
	val progressColor = if (elapsedSeconds > estimateSeconds) {
		MaterialTheme.colorScheme.tertiary
	} else {
		MaterialTheme.colorScheme.primary
	}

	Box(
		modifier = Modifier.size(104.dp),
		contentAlignment = Alignment.Center,
	) {
		Canvas(modifier = Modifier.fillMaxSize()) {
			drawCircle(
				color = track,
				style = Stroke(width = 7.dp.toPx()),
			)
			drawArc(
				color = progressColor,
				startAngle = -90f,
				sweepAngle = 360f * progress,
				useCenter = false,
				style = Stroke(
					width = 7.dp.toPx(),
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
				style = MaterialTheme.typography.titleLarge,
				fontWeight = FontWeight.ExtraBold,
			)
			Text(
				text = if (remaining > 0) "ešte asi" else "trvá",
				style = MaterialTheme.typography.labelSmall,
				color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
			)
		}
	}
}

@Composable
private fun SectionHeading(text: String) {
	Column {
		Spacer(Modifier.height(4.dp))
		Text(
			text = text,
			style = MaterialTheme.typography.titleLarge,
			fontWeight = FontWeight.Bold,
		)
		Spacer(Modifier.height(4.dp))
		HorizontalDivider(
			color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
		)
	}
}

@Composable
private fun TimelineItem(item: ScheduledTask) {
	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surface,
		),
		border = BorderStroke(
			1.dp,
			MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
		),
		shape = RoundedCornerShape(18.dp),
	) {
		Column(modifier = Modifier.padding(16.dp)) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically,
			) {
				Surface(
					color = MaterialTheme.colorScheme.surfaceVariant,
					shape = RoundedCornerShape(10.dp),
				) {
					Text(
						text = formatOffset(item.startSeconds),
						style = MaterialTheme.typography.labelLarge,
						fontWeight = FontWeight.Bold,
						modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
					)
				}
				Text(
					text = if (item.task.kind == TaskKind.EVENT) {
						"odhad ~" + formatRemaining(item.endSeconds - item.startSeconds)
					} else {
						formatRemaining(item.endSeconds - item.startSeconds)
					},
					style = MaterialTheme.typography.labelMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}

			Spacer(Modifier.height(10.dp))
			Text(
				text = item.task.title,
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.SemiBold,
			)
			Spacer(Modifier.height(3.dp))
			Text(
				text = item.task.instruction,
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
			item.task.tips.forEach { tip ->
				Spacer(Modifier.height(7.dp))
				Text(
					text = "TIP  $tip",
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.secondary,
				)
			}
			if (item.task.kind == TaskKind.EVENT) {
				Spacer(Modifier.height(8.dp))
				Text(
					text = "Pokračovanie čaká na tvoje potvrdenie",
					style = MaterialTheme.typography.labelMedium,
					color = MaterialTheme.colorScheme.tertiary,
					fontWeight = FontWeight.SemiBold,
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
