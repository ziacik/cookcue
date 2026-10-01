package com.ziacik.cookcue.mobile

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ziacik.cookcue.core.model.Recipe
import com.ziacik.cookcue.core.model.ScheduleMode
import com.ziacik.cookcue.core.model.ScheduledTask
import com.ziacik.cookcue.core.model.TaskKind
import com.ziacik.cookcue.core.recipes.BeanSoupRecipe
import com.ziacik.cookcue.core.recipes.FriedCheeseWithBoiledPotatoesRecipe
import com.ziacik.cookcue.core.recipes.FrankfurterSoupRecipe
import com.ziacik.cookcue.core.recipes.ParboiledFriesRecipe
import com.ziacik.cookcue.core.recipes.ScrambledEggsWithOnionAndToastRecipe
import com.ziacik.cookcue.core.recipes.ScrambledEggsWithOnionRecipe
import com.ziacik.cookcue.core.scheduler.ItineraryScheduler
import com.ziacik.cookcue.core.scheduler.Scheduler

enum class TaskProgress {
	PENDING,
	ACTIVE,
	COMPLETED,
	SKIPPED,
}

data class MobileSessionSnapshot(
	val started: Boolean,
	val completed: Boolean,
	val elapsedSeconds: Long,
	val schedule: List<ScheduledTask>,
	val currentAction: ScheduledTask?,
	val background: List<ScheduledTask>,
	val pendingEvents: List<ScheduledTask>,
	val previousAction: ScheduledTask?,
	val nextAction: ScheduledTask?,
	val nextScheduled: ScheduledTask?,
	val taskProgress: Map<String, TaskProgress>,
)

object CookingSessionController {
	private val bundledRecipes: List<Recipe> = listOf(
		BeanSoupRecipe.recipe,
		ScrambledEggsWithOnionRecipe.recipe,
		ScrambledEggsWithOnionAndToastRecipe.recipe,
		FriedCheeseWithBoiledPotatoesRecipe.recipe,
		FrankfurterSoupRecipe.recipe,
		ParboiledFriesRecipe.recipe,
	)

	var availableRecipes by mutableStateOf(bundledRecipes)
		private set

	private var remoteRecipesInitialized = false
	private var pendingRemoteRecipes: List<Recipe>? = null

	fun initialize(context: Context) {
		if (remoteRecipesInitialized) {
			return
		}
		remoteRecipesInitialized = true

		installRemoteRecipes(RemoteRecipeRepository.loadCached(context))
		RemoteRecipeRepository.refresh(context) { recipes ->
			if (startedAt == null) {
				installRemoteRecipes(recipes)
			} else {
				pendingRemoteRecipes = recipes
			}
		}
	}

	private fun installRemoteRecipes(remoteRecipes: List<Recipe>) {
		val merged = linkedMapOf<String, Recipe>()
		bundledRecipes.forEach { merged[it.id] = it }
		remoteRecipes.forEach { merged[it.id] = it }
		availableRecipes = merged.values.toList()

		if (availableRecipes.none { it.id == selectedRecipeId }) {
			selectedRecipeId = bundledRecipes.first().id
		}
	}

	var selectedRecipeId by mutableStateOf(BeanSoupRecipe.recipe.id)
		private set

	val recipe: Recipe
		get() = availableRecipes.first { it.id == selectedRecipeId }

	private val scheduler = Scheduler()
	private val itineraryScheduler = ItineraryScheduler()

	fun selectRecipe(recipeId: String) {
		if (startedAt != null || availableRecipes.none { it.id == recipeId }) {
			return
		}

		selectedRecipeId = recipeId
		durationOverrides = emptyMap()
		taskStartOverrides = emptyMap()
		skippedTaskIds = emptySet()
		activeTaskOverrideId = null
		activeTaskOverrideStartedAtSeconds = null
		eventDeferredUntil = emptyMap()
		markSilentTransition()
	}

	var durationOverrides by mutableStateOf<Map<String, Long>>(emptyMap())
		private set

	var taskStartOverrides by mutableStateOf<Map<String, Long>>(emptyMap())
		private set

	var sessionStartedWallClockMillis by mutableStateOf<Long?>(null)
		private set

	var startedAt by mutableStateOf<Long?>(null)
		private set

	var eventDeferredUntil by mutableStateOf<Map<String, Long>>(emptyMap())
		private set

	var skippedTaskIds by mutableStateOf<Set<String>>(emptySet())
		private set

	var activeTaskOverrideId by mutableStateOf<String?>(null)
		private set

	var activeTaskOverrideStartedAtSeconds by mutableStateOf<Long?>(null)
		private set

	var userActionVersion by mutableStateOf(0L)
		private set

	var silentTransitionVersion by mutableStateOf(0L)
		private set

	private fun markUserAction() {
		userActionVersion += 1
	}

	private fun markSilentTransition() {
		silentTransitionVersion += 1
		markUserAction()
	}

	fun start() {
		durationOverrides = emptyMap()
		taskStartOverrides = emptyMap()
		skippedTaskIds = emptySet()
		activeTaskOverrideId = null
		activeTaskOverrideStartedAtSeconds = null
		eventDeferredUntil = emptyMap()
		sessionStartedWallClockMillis = System.currentTimeMillis()
		startedAt = SystemClock.elapsedRealtime()
		markUserAction()
	}

	fun stop() {
		startedAt = null
		sessionStartedWallClockMillis = null
		durationOverrides = emptyMap()
		taskStartOverrides = emptyMap()
		skippedTaskIds = emptySet()
		activeTaskOverrideId = null
		activeTaskOverrideStartedAtSeconds = null
		eventDeferredUntil = emptyMap()
		markUserAction()

		pendingRemoteRecipes?.let { recipes ->
			pendingRemoteRecipes = null
			installRemoteRecipes(recipes)
		}
	}

	fun restore(
		recipeId: String?,
		startedAt: Long?,
		sessionStartedWallClockMillis: Long? = null,
		durationOverrides: Map<String, Long>,
		taskStartOverrides: Map<String, Long> = emptyMap(),
		skippedTaskIds: Set<String> = emptySet(),
		activeTaskOverrideId: String? = null,
		activeTaskOverrideStartedAtSeconds: Long? = null,
		eventDeferredUntil: Map<String, Long> = emptyMap(),
	) {
		selectedRecipeId = availableRecipes
			.firstOrNull { it.id == recipeId }
			?.id
			?: BeanSoupRecipe.recipe.id

		val itineraryCanResume =
			recipe.scheduleMode == ScheduleMode.ITINERARY &&
				sessionStartedWallClockMillis != null
		this.startedAt = startedAt ?: if (itineraryCanResume) {
			SystemClock.elapsedRealtime()
		} else {
			null
		}
		this.sessionStartedWallClockMillis = sessionStartedWallClockMillis
		this.durationOverrides = durationOverrides
		this.taskStartOverrides = taskStartOverrides
		this.skippedTaskIds = skippedTaskIds
		this.activeTaskOverrideId = activeTaskOverrideId
		this.activeTaskOverrideStartedAtSeconds = activeTaskOverrideStartedAtSeconds
		this.eventDeferredUntil = eventDeferredUntil
		markUserAction()
	}

	fun completeAction(taskId: String) {
		val snapshot = snapshot()
		val action = snapshot.currentAction
			?.takeIf { it.task.id == taskId }
			?: return

		val actualDuration = (snapshot.elapsedSeconds - action.startSeconds).coerceAtLeast(1)
		durationOverrides = durationOverrides + (taskId to actualDuration)
		taskStartOverrides = taskStartOverrides + (taskId to action.startSeconds)
		skippedTaskIds = skippedTaskIds - taskId
		if (activeTaskOverrideId == taskId) {
			activeTaskOverrideId = null
			activeTaskOverrideStartedAtSeconds = null
		}
		markUserAction()
	}

	fun skipAction(taskId: String) {
		val snapshot = snapshot()
		val action = snapshot.currentAction
			?.takeIf { it.task.id == taskId && it.task.optional }
			?: return

		durationOverrides = durationOverrides + (taskId to 1L)
		taskStartOverrides = taskStartOverrides + (taskId to action.startSeconds)
		skippedTaskIds = skippedTaskIds + taskId
		if (activeTaskOverrideId == taskId) {
			activeTaskOverrideId = null
			activeTaskOverrideStartedAtSeconds = null
		}
		markUserAction()
	}

	fun activateTask(
		taskId: String,
		silentTransition: Boolean = true,
	) {
		if (recipe.scheduleMode != ScheduleMode.ITINERARY || startedAt == null) {
			return
		}

		val task = recipe.tasks.firstOrNull {
			it.id == taskId && it.kind == TaskKind.ACTIVE
		} ?: return
		if (task.id in durationOverrides) {
			return
		}

		val elapsedSeconds = currentElapsedSeconds()
		activeTaskOverrideId = task.id
		activeTaskOverrideStartedAtSeconds = elapsedSeconds
		if (silentTransition) {
			markSilentTransition()
		} else {
			markUserAction()
		}
	}

	fun confirmEvent(taskId: String) {
		val snapshot = snapshot()
		val event = snapshot.pendingEvents.firstOrNull { it.task.id == taskId } ?: return
		val actualDuration = if (recipe.scheduleMode == ScheduleMode.ITINERARY) {
			event.task.durationSeconds
		} else {
			(snapshot.elapsedSeconds - event.startSeconds).coerceAtLeast(1)
		}
		durationOverrides = durationOverrides + (taskId to actualDuration)
		taskStartOverrides = taskStartOverrides + (taskId to event.startSeconds)
		eventDeferredUntil = eventDeferredUntil - taskId
		markUserAction()
	}

	fun deferEvent(taskId: String) {
		val snapshot = snapshot()
		val event = snapshot.pendingEvents.firstOrNull { it.task.id == taskId } ?: return
		val retryAfterSeconds = event.task.retryAfterSeconds ?: return

		eventDeferredUntil = eventDeferredUntil + (
			taskId to snapshot.elapsedSeconds + retryAfterSeconds
		)
		markSilentTransition()
	}

	fun previous() {
		val snapshot = snapshot()
		if (snapshot.currentAction != null || snapshot.pendingEvents.isNotEmpty()) {
			return
		}

		val target = snapshot.previousAction ?: return
		jumpTo(target)
		markSilentTransition()
	}

	fun next() {
		val snapshot = snapshot()
		if (snapshot.currentAction != null || snapshot.pendingEvents.isNotEmpty()) {
			return
		}

		val target = snapshot.nextAction ?: return
		jumpTo(target)
		markSilentTransition()
	}

	fun snapshot(now: Long = SystemClock.elapsedRealtime()): MobileSessionSnapshot {
		val start = startedAt
		val elapsedSeconds = when {
			start == null -> 0
			recipe.scheduleMode == ScheduleMode.ITINERARY -> {
				val epochStart = requireNotNull(recipe.scheduleStartEpochSeconds)
				(System.currentTimeMillis() / 1000 - epochStart).coerceAtLeast(0)
			}
			else -> ((now - start) / 1000).coerceAtLeast(0)
		}
		val schedule = if (start == null) {
			baseSchedule(elapsedSeconds)
		} else {
			liveSchedule(elapsedSeconds)
		}

		val unconfirmedManualTaskIds = recipe.tasks
			.asSequence()
			.filter { it.kind == TaskKind.ACTIVE || it.kind == TaskKind.EVENT }
			.map { it.id }
			.filterNot(durationOverrides::containsKey)
			.toSet()

		val blockedIds = blockedTaskIds(
			recipe = recipe,
			roots = unconfirmedManualTaskIds,
		)

		val pendingEvents = if (start == null) {
			emptyList()
		} else {
			schedule.filter {
				it.task.kind == TaskKind.EVENT &&
					it.task.id !in durationOverrides &&
					it.task.id !in blockedIds &&
					it.startSeconds <= elapsedSeconds &&
					eventIsDue(it.task.id, elapsedSeconds)
			}
		}

		val currentAction = if (start == null) {
			null
		} else {
			activeTaskOverrideId
				?.let { taskId ->
					val startedAtSeconds = activeTaskOverrideStartedAtSeconds
					val planned = schedule.firstOrNull {
						it.task.id == taskId &&
							it.task.kind == TaskKind.ACTIVE &&
							it.task.id !in durationOverrides
					}
					if (planned != null && startedAtSeconds != null) {
						planned.copy(
							startSeconds = startedAtSeconds,
							endSeconds = startedAtSeconds + planned.task.durationSeconds,
						)
					} else {
						null
					}
				}
				?: schedule
					.asSequence()
					.filter {
						it.task.kind == TaskKind.ACTIVE &&
							it.task.id !in durationOverrides &&
							it.task.id !in blockedIds &&
							it.startSeconds <= elapsedSeconds
					}
					.minWithOrNull(
						compareBy<ScheduledTask> { it.startSeconds }
							.thenBy { it.task.id }
					)
		}

		val background = if (start == null) {
			emptyList()
		} else {
			schedule.filter {
				it.task.kind == TaskKind.WAIT &&
					it.task.id !in blockedIds &&
					elapsedSeconds in it.startSeconds until it.endSeconds
			}
		}

		val actionSteps = schedule.filter {
			it.task.kind == TaskKind.ACTIVE &&
				it.task.resources.any { resource -> resource.resource == "cook" } &&
				it.task.id !in blockedIds
		}
		val navigationIndex = if (start == null || actionSteps.isEmpty()) {
			-1
		} else {
			actionSteps.indexOfLast { it.startSeconds <= elapsedSeconds }.coerceAtLeast(0)
		}

		val navigationAllowed =
			recipe.scheduleMode != ScheduleMode.ITINERARY &&
				currentAction == null &&
				pendingEvents.isEmpty()
		val previousAction = if (navigationAllowed) {
			actionSteps.getOrNull(navigationIndex - 1)
		} else {
			null
		}
		val nextAction = if (navigationAllowed) {
			actionSteps.getOrNull(navigationIndex + 1)
		} else {
			null
		}

		val nextScheduled = schedule.firstOrNull {
			it.task.id !in blockedIds &&
				it.startSeconds > elapsedSeconds
		}

		val activeIds = buildSet {
			currentAction?.task?.id?.let(::add)
			pendingEvents.mapTo(this) { it.task.id }
		}
		val taskProgress = recipe.tasks.associate { task ->
			task.id to when {
				task.id in skippedTaskIds -> TaskProgress.SKIPPED
				task.id in durationOverrides -> TaskProgress.COMPLETED
				task.id in activeIds -> TaskProgress.ACTIVE
				else -> TaskProgress.PENDING
			}
		}

		val completed =
			start != null &&
				unconfirmedManualTaskIds.isEmpty() &&
				currentAction == null &&
				pendingEvents.isEmpty() &&
				background.isEmpty() &&
				nextScheduled == null

		return MobileSessionSnapshot(
			started = start != null,
			completed = completed,
			elapsedSeconds = elapsedSeconds,
			schedule = schedule,
			currentAction = currentAction,
			background = background,
			pendingEvents = pendingEvents,
			previousAction = previousAction,
			nextAction = nextAction,
			nextScheduled = nextScheduled,
			taskProgress = taskProgress,
		)
	}

	private fun currentElapsedSeconds(): Long {
		val start = startedAt ?: return 0
		return if (recipe.scheduleMode == ScheduleMode.ITINERARY) {
			val epochStart = requireNotNull(recipe.scheduleStartEpochSeconds)
			(System.currentTimeMillis() / 1000 - epochStart).coerceAtLeast(0)
		} else {
			((SystemClock.elapsedRealtime() - start) / 1000).coerceAtLeast(0)
		}
	}

	private fun baseSchedule(elapsedSeconds: Long): List<ScheduledTask> {
		return if (recipe.scheduleMode == ScheduleMode.ITINERARY) {
			itineraryScheduler.schedule(
				recipe = recipe,
				durationOverrides = durationOverrides,
				startOverrides = taskStartOverrides,
				elapsedSeconds = elapsedSeconds,
				earliestUnscheduledStartSeconds = itinerarySessionStartOffset(),
			)
		} else {
			scheduler.schedule(
				recipe = recipe,
				durationOverrides = durationOverrides,
			)
		}
	}

	private fun liveSchedule(elapsedSeconds: Long): List<ScheduledTask> {
		if (recipe.scheduleMode == ScheduleMode.ITINERARY) {
			return liveItinerarySchedule(elapsedSeconds)
		}

		var schedule = scheduler.schedule(
			recipe = recipe,
			durationOverrides = durationOverrides,
		)

		repeat(8) {
			val unconfirmedManualTaskIds = recipe.tasks
				.asSequence()
				.filter { it.kind == TaskKind.ACTIVE || it.kind == TaskKind.EVENT }
				.map { it.id }
				.filterNot(durationOverrides::containsKey)
				.toSet()
			val blockedIds = blockedTaskIds(
				recipe = recipe,
				roots = unconfirmedManualTaskIds,
			)

			val currentAction = schedule
				.asSequence()
				.filter {
					it.task.kind == TaskKind.ACTIVE &&
						it.task.id !in durationOverrides &&
						it.task.id !in blockedIds &&
						it.startSeconds <= elapsedSeconds
				}
				.minWithOrNull(
					compareBy<ScheduledTask> { it.startSeconds }
						.thenBy { it.task.id }
				)

			val pendingEvents = schedule.filter {
				it.task.kind == TaskKind.EVENT &&
					it.task.id !in durationOverrides &&
					it.task.id !in blockedIds &&
					it.startSeconds <= elapsedSeconds &&
					eventIsDue(it.task.id, elapsedSeconds)
			}

			val liveOverrides = durationOverrides.toMutableMap()
			(listOfNotNull(currentAction) + pendingEvents).forEach { gate ->
				val elapsedForTask = (elapsedSeconds - gate.startSeconds + 1).coerceAtLeast(1)
				val liveDuration = maxOf(
					gate.task.durationSeconds,
					elapsedForTask,
				)
				liveOverrides[gate.task.id] = liveDuration
			}

			val nextSchedule = scheduler.schedule(
				recipe = recipe,
				durationOverrides = liveOverrides,
			)

			if (
				nextSchedule.map { it.task.id to it.startSeconds } ==
				schedule.map { it.task.id to it.startSeconds }
			) {
				return nextSchedule
			}

			schedule = nextSchedule
		}

		return schedule
	}

	private fun liveItinerarySchedule(elapsedSeconds: Long): List<ScheduledTask> {
		var schedule = itineraryScheduler.schedule(
			recipe = recipe,
			durationOverrides = durationOverrides,
			startOverrides = taskStartOverrides,
			elapsedSeconds = elapsedSeconds,
			earliestUnscheduledStartSeconds = itinerarySessionStartOffset(),
		)

		// A manual jump pauses the previously scheduled active step. Do not keep extending
		// that old step while the user is intentionally visiting a different one.
		if (activeTaskOverrideId != null) {
			return schedule
		}

		repeat(8) {
			val unconfirmedManualTaskIds = recipe.tasks
				.asSequence()
				.filter { it.kind == TaskKind.ACTIVE || it.kind == TaskKind.EVENT }
				.map { it.id }
				.filterNot(durationOverrides::containsKey)
				.toSet()
			val blockedIds = blockedTaskIds(
				recipe = recipe,
				roots = unconfirmedManualTaskIds,
			)

			val currentAction = schedule
				.asSequence()
					.filter {
						it.task.kind == TaskKind.ACTIVE &&
							it.task.id !in durationOverrides &&
							it.task.id !in blockedIds &&
							it.startSeconds <= elapsedSeconds
					}
					.minWithOrNull(
						compareBy<ScheduledTask> { it.startSeconds }
							.thenBy { it.task.id }
					)
				?: return schedule

			val elapsedForTask =
				(elapsedSeconds - currentAction.startSeconds + 1).coerceAtLeast(1)
			val liveOverrides = durationOverrides + (
				currentAction.task.id to maxOf(
					currentAction.task.durationSeconds,
					elapsedForTask,
				)
			)
			val liveStarts = taskStartOverrides + (
				currentAction.task.id to currentAction.startSeconds
			)

			val nextSchedule = itineraryScheduler.schedule(
				recipe = recipe,
				durationOverrides = liveOverrides,
				startOverrides = liveStarts,
				elapsedSeconds = elapsedSeconds,
				earliestUnscheduledStartSeconds = itinerarySessionStartOffset(),
			)

			if (
				nextSchedule.map { Triple(it.task.id, it.startSeconds, it.endSeconds) } ==
				schedule.map { Triple(it.task.id, it.startSeconds, it.endSeconds) }
			) {
				return nextSchedule
			}
			schedule = nextSchedule
		}

		return schedule
	}

	private fun itinerarySessionStartOffset(): Long {
		val epochStart = recipe.scheduleStartEpochSeconds ?: return 0
		val sessionEpochSeconds = (sessionStartedWallClockMillis ?: return 0) / 1000
		return (sessionEpochSeconds - epochStart).coerceAtLeast(0)
	}

	private fun eventIsDue(
		taskId: String,
		elapsedSeconds: Long,
	): Boolean {
		return (eventDeferredUntil[taskId] ?: Long.MIN_VALUE) <= elapsedSeconds
	}

	private fun jumpTo(target: ScheduledTask) {
		val now = SystemClock.elapsedRealtime()
		startedAt = now - target.startSeconds * 1000
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
}
