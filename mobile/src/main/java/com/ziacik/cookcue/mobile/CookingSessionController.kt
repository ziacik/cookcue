package com.ziacik.cookcue.mobile

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ziacik.cookcue.core.model.Recipe
import com.ziacik.cookcue.core.model.ScheduledTask
import com.ziacik.cookcue.core.model.TaskKind
import com.ziacik.cookcue.core.recipes.BeanSoupRecipe
import com.ziacik.cookcue.core.recipes.ScrambledEggsWithOnionRecipe
import com.ziacik.cookcue.core.scheduler.Scheduler

data class MobileSessionSnapshot(
	val started: Boolean,
	val elapsedSeconds: Long,
	val schedule: List<ScheduledTask>,
	val currentAction: ScheduledTask?,
	val background: List<ScheduledTask>,
	val pendingEvents: List<ScheduledTask>,
	val previousAction: ScheduledTask?,
	val nextAction: ScheduledTask?,
	val nextScheduled: ScheduledTask?,
)

object CookingSessionController {
	val availableRecipes: List<Recipe> = listOf(
		BeanSoupRecipe.recipe,
		ScrambledEggsWithOnionRecipe.recipe,
	)

	var selectedRecipeId by mutableStateOf(BeanSoupRecipe.recipe.id)
		private set

	val recipe: Recipe
		get() = availableRecipes.first { it.id == selectedRecipeId }

	private val scheduler = Scheduler()

	fun selectRecipe(recipeId: String) {
		if (startedAt != null || availableRecipes.none { it.id == recipeId }) {
			return
		}

		selectedRecipeId = recipeId
		durationOverrides = emptyMap()
		eventDeferredUntil = emptyMap()
		markUserAction()
	}

	var durationOverrides by mutableStateOf<Map<String, Long>>(emptyMap())
		private set

	var startedAt by mutableStateOf<Long?>(null)
		private set

	var eventDeferredUntil by mutableStateOf<Map<String, Long>>(emptyMap())
		private set

	var userActionVersion by mutableStateOf(0L)
		private set

	private fun markUserAction() {
		userActionVersion += 1
	}

	fun start() {
		durationOverrides = emptyMap()
		eventDeferredUntil = emptyMap()
		startedAt = SystemClock.elapsedRealtime()
		markUserAction()
	}

	fun stop() {
		startedAt = null
		durationOverrides = emptyMap()
		eventDeferredUntil = emptyMap()
		markUserAction()
	}

	fun restore(
		recipeId: String?,
		startedAt: Long?,
		durationOverrides: Map<String, Long>,
		eventDeferredUntil: Map<String, Long> = emptyMap(),
	) {
		selectedRecipeId = availableRecipes
			.firstOrNull { it.id == recipeId }
			?.id
			?: BeanSoupRecipe.recipe.id
		this.startedAt = startedAt
		this.durationOverrides = durationOverrides
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
		markUserAction()
	}

	fun confirmEvent(taskId: String) {
		val snapshot = snapshot()
		val event = snapshot.pendingEvents.firstOrNull { it.task.id == taskId } ?: return
		val actualDuration = (snapshot.elapsedSeconds - event.startSeconds).coerceAtLeast(1)
		durationOverrides = durationOverrides + (taskId to actualDuration)
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
		markUserAction()
	}

	fun previous() {
		val snapshot = snapshot()
		if (snapshot.currentAction != null || snapshot.pendingEvents.isNotEmpty()) {
			return
		}

		val target = snapshot.previousAction ?: return
		jumpTo(target)
		markUserAction()
	}

	fun next() {
		val snapshot = snapshot()
		if (snapshot.currentAction != null || snapshot.pendingEvents.isNotEmpty()) {
			return
		}

		val target = snapshot.nextAction ?: return
		jumpTo(target)
		markUserAction()
	}

	fun snapshot(now: Long = SystemClock.elapsedRealtime()): MobileSessionSnapshot {
		val start = startedAt
		val elapsedSeconds = start?.let { ((now - it) / 1000).coerceAtLeast(0) } ?: 0
		val schedule = if (start == null) {
			scheduler.schedule(
				recipe = recipe,
				durationOverrides = durationOverrides,
			)
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
			schedule
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

		val navigationAllowed = currentAction == null && pendingEvents.isEmpty()
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

		return MobileSessionSnapshot(
			started = start != null,
			elapsedSeconds = elapsedSeconds,
			schedule = schedule,
			currentAction = currentAction,
			background = background,
			pendingEvents = pendingEvents,
			previousAction = previousAction,
			nextAction = nextAction,
			nextScheduled = nextScheduled,
		)
	}

	private fun liveSchedule(elapsedSeconds: Long): List<ScheduledTask> {
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
