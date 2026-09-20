package com.ziacik.cookcue.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
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
	val notificationPermissionLauncher = rememberLauncherForActivityResult(
		ActivityResultContracts.RequestPermission(),
	) {}
	val recipe = CookingSessionController.recipe
	val selectedRecipeId = CookingSessionController.selectedRecipeId
	val startedAt = CookingSessionController.startedAt
	val durationOverrides = CookingSessionController.durationOverrides
	val eventDeferredUntil = CookingSessionController.eventDeferredUntil
	val userActionVersion = CookingSessionController.userActionVersion

	var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
	var showStopCookingDialog by remember { mutableStateOf(false) }

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
	val current = snapshot.currentAction
	val primaryWait = snapshot.background.minByOrNull { it.endSeconds }
	val displayedWait = if (current == null) primaryWait else null
	val activeTaskId = current?.task?.id ?: displayedWait?.task?.id
	val activeStepIndex = snapshot.schedule.indexOfFirst { it.task.id == activeTaskId }
	val secondaryBackground = snapshot.background.filterNot {
		it.task.id == displayedWait?.task?.id
	}
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

	if (showStopCookingDialog) {
		AlertDialog(
			onDismissRequest = { showStopCookingDialog = false },
			title = {
				Text("Ukončiť aktuálne varenie?")
			},
			text = {
				Text("Rozrobený postup sa ukončí a vrátiš sa na výber receptu.")
			},
			confirmButton = {
				TextButton(
					onClick = {
						showStopCookingDialog = false
						CookingSessionController.stop()
						persistAndSync()
					},
				) {
					Text("UKONČIŤ")
				}
			},
			dismissButton = {
				TextButton(
					onClick = { showStopCookingDialog = false },
				) {
					Text("SPÄŤ")
				}
			},
		)
	}

	Scaffold(
		containerColor = MaterialTheme.colorScheme.background,
	) { padding ->
		LazyColumn(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding),
			contentPadding = PaddingValues(bottom = 28.dp),
		) {
			item {
				CookCueTopBar(
					recipeTitle = recipe.title,
					showRecipesAction = snapshot.started,
					onRecipes = { showStopCookingDialog = true },
				)
			}

			if (!snapshot.started) {
				item {
					Column(
						modifier = Modifier.padding(horizontal = 20.dp),
					) {
						Spacer(Modifier.height(18.dp))
						SectionTitle("Recept")
						Spacer(Modifier.height(8.dp))
						CookingSessionController.availableRecipes.forEach { option ->
							if (option.id == selectedRecipeId) {
								Button(
									onClick = {},
									modifier = Modifier.fillMaxWidth(),
									shape = RoundedCornerShape(12.dp),
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
									shape = RoundedCornerShape(12.dp),
								) {
									Text(option.title)
								}
							}
							Spacer(Modifier.height(6.dp))
						}
						Spacer(Modifier.height(12.dp))
						Text(
							text = recipe.title,
							style = MaterialTheme.typography.headlineLarge.copy(
								fontFamily = FontFamily.Serif,
								fontWeight = FontWeight.SemiBold,
							),
						)
						Spacer(Modifier.height(5.dp))
						Text(
							text = recipe.description,
							style = MaterialTheme.typography.bodyLarge,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
						)
						Spacer(Modifier.height(18.dp))
						PreCookingPanel(
							note = recipe.preCookingNote,
							onStart = {
								if (
									Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
									context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
									PackageManager.PERMISSION_GRANTED
								) {
									notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
								}
								CookingSessionController.start()
								persistAndSync()
							},
						)
						Spacer(Modifier.height(22.dp))
						SectionTitle("Ingrediencie")
						Spacer(Modifier.height(8.dp))
						IngredientList(
							ingredients = recipe.ingredients.map { it.amount to it.name },
						)
					}
				}
			} else {
				item {
					Column(
						modifier = Modifier.padding(horizontal = 16.dp),
					) {
						Spacer(Modifier.height(12.dp))
						when {
							current != null -> {
								val elapsed =
									(snapshot.elapsedSeconds - current.startSeconds).coerceAtLeast(0)
								CurrentStepCard(
									title = current.task.title,
									instruction = current.task.instruction,
									tips = current.task.tips,
									estimateSeconds = current.task.durationSeconds,
									elapsedSeconds = elapsed,
									stepNumber = activeStepIndex.takeIf { it >= 0 }?.plus(1),
									stepCount = snapshot.schedule.size,
									actionLabel = "HOTOVO",
									onAction = {
										CookingSessionController.completeAction(current.task.id)
										persistAndSync()
									},
								)
							}

							displayedWait != null -> {
								val elapsed =
									(snapshot.elapsedSeconds - displayedWait.startSeconds).coerceAtLeast(0)
								val duration =
									(displayedWait.endSeconds - displayedWait.startSeconds)
										.coerceAtLeast(1)
								CurrentStepCard(
									title = displayedWait.task.title,
									instruction = displayedWait.task.instruction,
									tips = displayedWait.task.tips,
									estimateSeconds = duration,
									elapsedSeconds = elapsed,
									stepNumber = activeStepIndex.takeIf { it >= 0 }?.plus(1),
									stepCount = snapshot.schedule.size,
									actionLabel = null,
									onAction = {},
								)
							}

							else -> {
								IdleCard()
							}
						}
					}
				}

				items(snapshot.pendingEvents, key = { "event-" + it.task.id }) { event ->
					Column(
						modifier = Modifier.padding(horizontal = 16.dp),
					) {
						Spacer(Modifier.height(12.dp))
						PendingEventCard(
							title = event.task.title,
							instruction = event.task.instruction,
							tips = event.task.tips,
							actionLabel = event.task.actionLabel ?: "HOTOVO",
							retryActionLabel = event.task.retryActionLabel,
							retryAfterSeconds = event.task.retryAfterSeconds,
							onDefer = {
								CookingSessionController.deferEvent(event.task.id)
								persistAndSync()
							},
							onConfirm = {
								CookingSessionController.confirmEvent(event.task.id)
								persistAndSync()
							},
						)
					}
				}

				if (secondaryBackground.isNotEmpty()) {
					item {
						Column(
							modifier = Modifier.padding(horizontal = 20.dp),
						) {
							Spacer(Modifier.height(16.dp))
							CompactTimers(
								items = secondaryBackground.map {
									it.task.title to formatRemaining(
										it.endSeconds - snapshot.elapsedSeconds,
									)
								},
							)
						}
					}
				}

				snapshot.nextScheduled?.let { next ->
					item {
						Column(
							modifier = Modifier.padding(horizontal = 20.dp),
						) {
							Spacer(Modifier.height(16.dp))
							NextStepRow(
								title = next.task.title,
								time = "o " + formatRemaining(
									next.startSeconds - snapshot.elapsedSeconds,
								),
							)
						}
					}
				}

				item {
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(horizontal = 20.dp, vertical = 14.dp),
						horizontalArrangement = Arrangement.spacedBy(10.dp),
					) {
						OutlinedButton(
							onClick = {
								CookingSessionController.previous()
								persistAndSync()
							},
							enabled = snapshot.previousAction != null,
							modifier = Modifier.weight(1f),
							shape = RoundedCornerShape(12.dp),
						) {
							Text("← PREDOŠLÝ")
						}
						OutlinedButton(
							onClick = {
								CookingSessionController.next()
								persistAndSync()
							},
							enabled = snapshot.nextAction != null,
							modifier = Modifier.weight(1f),
							shape = RoundedCornerShape(12.dp),
						) {
							Text("ĎALŠÍ →")
						}
					}
				}
			}

			item {
				Column(
					modifier = Modifier.padding(horizontal = 20.dp),
				) {
					Spacer(Modifier.height(8.dp))
					SectionTitle("Plán")
					Text(
						text = "Všetko krok za krokom. CookCue stráži čas.",
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
					Spacer(Modifier.height(12.dp))
				}
			}

			itemsIndexed(
				items = snapshot.schedule,
				key = { _, item -> "plan-" + item.task.id },
			) { index, item ->
				PlanRow(
					index = index,
					item = item,
					active = item.task.id == activeTaskId,
					isLast = index == snapshot.schedule.lastIndex,
				)
			}

			item {
				Column(
					modifier = Modifier.padding(horizontal = 20.dp),
				) {
					Spacer(Modifier.height(24.dp))
					SectionTitle("Krízová pomoc")
					Text(
						text = "Keď varenie nejde podľa plánu, tu nájdeš rýchlu záchranu.",
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
					Spacer(Modifier.height(8.dp))
				}
			}

			itemsIndexed(
				items = recipe.troubleshooting,
				key = { _, tip -> tip.problem },
			) { index, tip ->
				TroubleRow(
					title = tip.problem,
					advice = tip.advice,
					showDivider = index != recipe.troubleshooting.lastIndex,
				)
			}
		}
	}
}

@Composable
private fun CookCueTopBar(
	recipeTitle: String,
	showRecipesAction: Boolean,
	onRecipes: () -> Unit,
) {
	Column {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 18.dp, vertical = 11.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			CueMark(30.dp)
			Spacer(Modifier.width(10.dp))
			Text(
				text = recipeTitle,
				style = MaterialTheme.typography.titleMedium.copy(
					fontFamily = FontFamily.Serif,
					fontWeight = FontWeight.SemiBold,
				),
				modifier = Modifier.weight(1f),
			)
			if (showRecipesAction) {
				TextButton(
					onClick = onRecipes,
					contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
				) {
					Text(
						text = "RECEPTY",
						style = MaterialTheme.typography.labelMedium,
						fontWeight = FontWeight.Bold,
					)
				}
			}
		}
		HorizontalDivider(
			color = MaterialTheme.colorScheme.outline.copy(alpha = 0.75f),
		)
	}
}

@Composable
private fun CueMark(size: Dp) {
	val wine = MaterialTheme.colorScheme.primary
	Box(modifier = Modifier.size(size)) {
		Canvas(modifier = Modifier.fillMaxSize()) {
			val stroke = size.toPx() * 0.19f
			val radius = size.toPx() * 0.30f
			val centerX = this.size.width * 0.48f
			val centerY = this.size.height * 0.53f

			drawArc(
				color = wine,
				startAngle = -67f,
				sweepAngle = 255f,
				useCenter = false,
				topLeft = Offset(
					centerX - radius,
					centerY - radius,
				),
				size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
				style = Stroke(
					width = stroke,
					cap = StrokeCap.Butt,
				),
			)

			drawCircle(
				color = CookCueHoney,
				radius = this.size.width * 0.105f,
				center = Offset(
					this.size.width * 0.48f,
					this.size.height * 0.54f,
				),
			)

			val rayColor = CookCueHoney
			val rayStroke = this.size.width * 0.032f
			listOf(
				Offset(0.61f, 0.37f) to Offset(0.68f, 0.18f),
				Offset(0.66f, 0.39f) to Offset(0.75f, 0.22f),
				Offset(0.71f, 0.42f) to Offset(0.82f, 0.27f),
			).forEach { (from, to) ->
				drawLine(
					color = rayColor,
					start = Offset(this.size.width * from.x, this.size.height * from.y),
					end = Offset(this.size.width * to.x, this.size.height * to.y),
					strokeWidth = rayStroke,
					cap = StrokeCap.Butt,
				)
			}

			val block = androidx.compose.ui.graphics.Path().apply {
				moveTo(this@Canvas.size.width * 0.74f, this@Canvas.size.height * 0.42f)
				lineTo(this@Canvas.size.width * 0.84f, this@Canvas.size.height * 0.27f)
				lineTo(this@Canvas.size.width * 0.93f, this@Canvas.size.height * 0.34f)
				lineTo(this@Canvas.size.width * 0.82f, this@Canvas.size.height * 0.49f)
				close()
			}
			drawPath(
				path = block,
				color = CookCueHoney,
			)
		}
	}
}

@Composable
private fun PreCookingPanel(
	note: String?,
	onStart: () -> Unit,
) {
	Surface(
		modifier = Modifier.fillMaxWidth(),
		color = MaterialTheme.colorScheme.surfaceVariant,
		shape = RoundedCornerShape(18.dp),
	) {
		Column(modifier = Modifier.padding(18.dp)) {
		if (note != null) {
				SmallLabel("PRED VARENÍM")
				Spacer(Modifier.height(7.dp))
				Text(
					text = note,
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
				Spacer(Modifier.height(16.dp))
			}
			Button(
				onClick = onStart,
				modifier = Modifier.fillMaxWidth(),
				shape = RoundedCornerShape(12.dp),
				colors = ButtonDefaults.buttonColors(
					containerColor = MaterialTheme.colorScheme.primary,
				),
			) {
				Text(
					text = "▶  SPUSTIŤ VARENIE",
					fontWeight = FontWeight.Bold,
					modifier = Modifier.padding(vertical = 3.dp),
				)
			}
		}
	}
}

@Composable
private fun IngredientList(
	ingredients: List<Pair<String, String>>,
) {
	Surface(
		modifier = Modifier.fillMaxWidth(),
		color = MaterialTheme.colorScheme.surface,
		shape = RoundedCornerShape(16.dp),
		border = BorderStroke(
			1.dp,
			MaterialTheme.colorScheme.outline.copy(alpha = 0.75f),
		),
	) {
		Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 5.dp)) {
			ingredients.forEachIndexed { index, ingredient ->
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(vertical = 10.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					Box(
						modifier = Modifier
							.size(24.dp)
							.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
						contentAlignment = Alignment.Center,
					) {
						Box(
							modifier = Modifier
								.size(7.dp)
								.background(MaterialTheme.colorScheme.primary, CircleShape),
						)
					}
					Spacer(Modifier.width(10.dp))
					Text(
						text = ingredient.second,
						style = MaterialTheme.typography.bodyMedium,
						modifier = Modifier.weight(1f),
					)
					Text(
						text = ingredient.first,
						style = MaterialTheme.typography.bodyMedium,
						fontWeight = FontWeight.Medium,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
				}
				if (index != ingredients.lastIndex) {
					HorizontalDivider(
						color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
					)
				}
			}
		}
	}
}

@Composable
private fun CurrentStepCard(
	title: String,
	instruction: String,
	tips: List<String>,
	estimateSeconds: Long,
	elapsedSeconds: Long,
	stepNumber: Int?,
	stepCount: Int,
	actionLabel: String?,
	onAction: () -> Unit,
) {
	Surface(
		modifier = Modifier.fillMaxWidth(),
		color = MaterialTheme.colorScheme.surface,
		shape = RoundedCornerShape(20.dp),
		border = BorderStroke(
			1.dp,
			MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
		),
		shadowElevation = 2.dp,
	) {
		Column(modifier = Modifier.padding(18.dp)) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Surface(
					color = MaterialTheme.colorScheme.primaryContainer,
					shape = RoundedCornerShape(50),
				) {
					Text(
						text = "TERAZ",
						style = MaterialTheme.typography.labelMedium,
						fontWeight = FontWeight.Bold,
						color = MaterialTheme.colorScheme.primary,
						modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
					)
				}
				Spacer(Modifier.weight(1f))
				if (stepNumber != null && stepCount > 0) {
					Text(
						text = "Krok $stepNumber z $stepCount",
						style = MaterialTheme.typography.labelMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
				}
			}

			Spacer(Modifier.height(13.dp))
			Text(
				text = title,
				style = MaterialTheme.typography.headlineMedium.copy(
					fontFamily = FontFamily.Serif,
					fontWeight = FontWeight.SemiBold,
				),
				lineHeight = 33.sp,
			)
			Spacer(Modifier.height(6.dp))
			Text(
				text = instruction,
				style = MaterialTheme.typography.bodyLarge,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)

			Spacer(Modifier.height(16.dp))
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
				Spacer(Modifier.height(13.dp))
				Surface(
					modifier = Modifier.fillMaxWidth(),
					color = MaterialTheme.colorScheme.surfaceVariant,
					shape = RoundedCornerShape(12.dp),
				) {
					Column(modifier = Modifier.padding(12.dp)) {
						tips.forEachIndexed { index, tip ->
							Text(
								text = if (index == 0) "Malý tip · $tip" else tip,
								style = MaterialTheme.typography.bodySmall,
								color = MaterialTheme.colorScheme.onSurfaceVariant,
							)
							if (index != tips.lastIndex) {
								Spacer(Modifier.height(5.dp))
							}
						}
					}
				}
			}

			if (actionLabel != null) {
				Spacer(Modifier.height(14.dp))
				Button(
					onClick = onAction,
					modifier = Modifier.fillMaxWidth(),
					shape = RoundedCornerShape(12.dp),
					colors = ButtonDefaults.buttonColors(
						containerColor = MaterialTheme.colorScheme.primary,
					),
				) {
					Text(
						text = "✓  $actionLabel",
						fontWeight = FontWeight.Bold,
						modifier = Modifier.padding(vertical = 4.dp),
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
	val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
	val progressColor = if (overdue) {
		MaterialTheme.colorScheme.tertiary
	} else {
		MaterialTheme.colorScheme.primary
	}

	Box(
		modifier = Modifier.size(128.dp),
		contentAlignment = Alignment.Center,
	) {
		Canvas(modifier = Modifier.fillMaxSize()) {
			drawCircle(
				color = trackColor,
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
				text = if (overdue) {
					formatRemaining(elapsedSeconds)
				} else {
					formatRemaining(remaining)
				},
				style = MaterialTheme.typography.headlineMedium.copy(
					fontFamily = FontFamily.Serif,
					fontWeight = FontWeight.Bold,
				),
				color = MaterialTheme.colorScheme.primary,
			)
			Text(
				text = if (overdue) "trvá" else "z " + formatRemaining(estimateSeconds),
				style = MaterialTheme.typography.labelSmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}
	}
}

@Composable
private fun IdleCard() {
	Surface(
		modifier = Modifier.fillMaxWidth(),
		color = MaterialTheme.colorScheme.surfaceVariant,
		shape = RoundedCornerShape(18.dp),
	) {
		Column(modifier = Modifier.padding(18.dp)) {
			SmallLabel("TERAZ")
			Spacer(Modifier.height(7.dp))
			Text(
				text = "Momentálne od teba nič netreba",
				style = MaterialTheme.typography.titleLarge.copy(
					fontFamily = FontFamily.Serif,
					fontWeight = FontWeight.SemiBold,
				),
			)
			Spacer(Modifier.height(4.dp))
			Text(
				text = "CookCue stráži čas.",
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}
	}
}

@Composable
private fun PendingEventCard(
	title: String,
	instruction: String,
	tips: List<String>,
	actionLabel: String,
	retryActionLabel: String?,
	retryAfterSeconds: Long?,
	onDefer: () -> Unit,
	onConfirm: () -> Unit,
) {
	Surface(
		modifier = Modifier.fillMaxWidth(),
		color = MaterialTheme.colorScheme.tertiaryContainer,
		shape = RoundedCornerShape(18.dp),
	) {
		Column(modifier = Modifier.padding(17.dp)) {
			SmallLabel(
				text = "ČAKÁ NA TEBA",
				color = MaterialTheme.colorScheme.tertiary,
			)
			Spacer(Modifier.height(6.dp))
			Text(
				text = title,
				style = MaterialTheme.typography.titleLarge.copy(
					fontFamily = FontFamily.Serif,
					fontWeight = FontWeight.SemiBold,
				),
			)
			Spacer(Modifier.height(4.dp))
			Text(
				text = instruction,
				style = MaterialTheme.typography.bodyMedium,
			)
			tips.forEach { tip ->
				Spacer(Modifier.height(5.dp))
				Text(
					text = "• $tip",
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onTertiaryContainer,
				)
			}
			Spacer(Modifier.height(12.dp))
			if (retryAfterSeconds != null && retryActionLabel != null) {
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.spacedBy(10.dp),
				) {
					OutlinedButton(
						onClick = onDefer,
						modifier = Modifier.weight(1f),
						shape = RoundedCornerShape(12.dp),
					) {
						Text(
							text = retryActionLabel,
							fontWeight = FontWeight.Bold,
						)
					}
					Button(
						onClick = onConfirm,
						modifier = Modifier.weight(1f),
						shape = RoundedCornerShape(12.dp),
					) {
						Text(
							text = actionLabel,
							fontWeight = FontWeight.Bold,
						)
					}
				}
				Spacer(Modifier.height(7.dp))
				Text(
					text = "Ak ešte nie, skontrolujeme znova o " +
						formatRemaining(retryAfterSeconds) + ".",
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onTertiaryContainer,
				)
			} else {
				Button(
					onClick = onConfirm,
					modifier = Modifier.fillMaxWidth(),
					shape = RoundedCornerShape(12.dp),
				) {
					Text(
						text = actionLabel,
						fontWeight = FontWeight.Bold,
					)
				}
			}
		}
	}
}

@Composable
private fun CompactTimers(
	items: List<Pair<String, String>>,
) {
	Column {
		SmallLabel(
			text = "BEŽÍ NA POZADÍ",
			color = MaterialTheme.colorScheme.secondary,
		)
		Spacer(Modifier.height(7.dp))
		items.forEachIndexed { index, item ->
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(vertical = 6.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Text(
					text = item.first,
					style = MaterialTheme.typography.bodyMedium,
					modifier = Modifier.weight(1f),
				)
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

@Composable
private fun NextStepRow(
	title: String,
	time: String,
) {
	Surface(
		modifier = Modifier.fillMaxWidth(),
		color = MaterialTheme.colorScheme.surfaceVariant,
		shape = RoundedCornerShape(14.dp),
	) {
		Row(
			modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			SmallLabel("ĎALEJ")
			Spacer(Modifier.width(11.dp))
			Text(
				text = title,
				style = MaterialTheme.typography.bodyMedium,
				fontWeight = FontWeight.Medium,
				modifier = Modifier.weight(1f),
			)
			Text(
				text = time,
				style = MaterialTheme.typography.bodyMedium,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.primary,
			)
		}
	}
}

@Composable
private fun SectionTitle(text: String) {
	Text(
		text = text,
		style = MaterialTheme.typography.headlineSmall.copy(
			fontFamily = FontFamily.Serif,
			fontWeight = FontWeight.SemiBold,
		),
	)
}

@Composable
private fun SmallLabel(
	text: String,
	color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
	Text(
		text = text,
		style = MaterialTheme.typography.labelSmall,
		fontWeight = FontWeight.Bold,
		color = color,
		letterSpacing = 1.0.sp,
	)
}

@Composable
private fun PlanRow(
	index: Int,
	item: ScheduledTask,
	active: Boolean,
	isLast: Boolean,
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 20.dp),
		verticalAlignment = Alignment.Top,
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			Box(
				modifier = Modifier
					.size(28.dp)
					.background(
						if (active) {
							MaterialTheme.colorScheme.primary
						} else {
							if (index == 0) CookCueHerb else CookCueTaupe
						},
						CircleShape,
					),
				contentAlignment = Alignment.Center,
			) {
				Text(
					text = (index + 1).toString(),
					style = MaterialTheme.typography.labelSmall,
					fontWeight = FontWeight.Bold,
					color = Color.White,
				)
			}
			if (!isLast) {
				Box(
					modifier = Modifier
						.width(1.dp)
						.height(if (active) 72.dp else 60.dp)
						.background(MaterialTheme.colorScheme.outline),
				)
			}
		}

		Spacer(Modifier.width(12.dp))

		if (active) {
			Surface(
				modifier = Modifier
					.weight(1f)
					.padding(bottom = 8.dp),
				color = MaterialTheme.colorScheme.surface,
				shape = RoundedCornerShape(14.dp),
				border = BorderStroke(
					1.dp,
					MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
				),
			) {
				PlanRowContent(item, active = true)
			}
		} else {
			Box(
				modifier = Modifier
					.weight(1f)
					.padding(bottom = 8.dp),
			) {
				PlanRowContent(item, active = false)
			}
		}
	}
}

@Composable
private fun PlanRowContent(
	item: ScheduledTask,
	active: Boolean,
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(
				horizontal = if (active) 13.dp else 0.dp,
				vertical = if (active) 10.dp else 7.dp,
			),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Column(modifier = Modifier.weight(1f)) {
			Text(
				text = item.task.title,
				style = MaterialTheme.typography.bodyMedium,
				fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
				color = MaterialTheme.colorScheme.onSurface,
			)
			Spacer(Modifier.height(2.dp))
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
		if (active) {
			Text(
				text = "▶",
				style = MaterialTheme.typography.labelLarge,
				color = MaterialTheme.colorScheme.primary,
			)
		}
	}
}

@Composable
private fun TroubleRow(
	title: String,
	advice: String,
	showDivider: Boolean,
) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 20.dp),
	) {
		Row(
			modifier = Modifier.padding(vertical = 11.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			Box(
				modifier = Modifier
					.size(28.dp)
					.background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
				contentAlignment = Alignment.Center,
			) {
				Box(
					modifier = Modifier
						.size(7.dp)
						.background(MaterialTheme.colorScheme.primary, CircleShape),
				)
			}
			Spacer(Modifier.width(11.dp))
			Column(modifier = Modifier.weight(1f)) {
				Text(
					text = title,
					style = MaterialTheme.typography.bodyMedium,
					fontWeight = FontWeight.SemiBold,
				)
				Spacer(Modifier.height(2.dp))
				Text(
					text = advice,
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
		}
		if (showDivider) {
			HorizontalDivider(
				modifier = Modifier.padding(start = 39.dp),
				color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
			)
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
