package com.ziacik.cookcue.mobile

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ziacik.cookcue.core.model.Recipe
import com.ziacik.cookcue.core.model.ScheduledTask
import com.ziacik.cookcue.core.model.TaskKind
import com.ziacik.cookcue.core.recipes.BeanSoupRecipe
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
	val recipe: Recipe = BeanSoupRecipe.recipe

	private val scheduler = Scheduler()

	var durationOverrides by mutableStateOf<Map<String, Long>>(emptyMap())
		private set

	var startedAt by mutableStateOf<Long?>(null)
		private set

	fun start() {
		durationOverrides = emptyMap()
		startedAt = SystemClock.elapsedRealtime()
	}

	fun restore(
		startedAt: Long?,
		durationOverrides: Map<String, Long>,
	) {
		this.startedAt = startedAt
		this.durationOverrides = durationOverrides
	}

	fun confirmEvent(taskId: String) {
		val snapshot = snapshot()
		val event = snapshot.pendingEvents.firstOrNull { it.task.id == taskId } ?: return
		val actualDuration = (snapshot.elapsedSeconds - event.startSeconds).coerceAtLeast(1)
		durationOverrides = durationOverrides + (taskId to actualDuration)
	}

	fun previous() {
		val target = snapshot().previousAction ?: return
		jumpTo(target)
	}

	fun next() {
		val target = snapshot().nextAction ?: return
		jumpTo(target)
	}

	fun snapshot(now: Long = SystemClock.elapsedRealtime()): MobileSessionSnapshot {
		val schedule = scheduler.schedule(
			recipe = recipe,
			durationOverrides = durationOverrides,
		)
		val start = startedAt
		val elapsedSeconds = start?.let { ((now - it) / 1000).coerceAtLeast(0) } ?: 0

		val pendingEvents = if (start == null) {
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

		val running = if (start == null) {
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
		val navigationIndex = if (start == null || actionSteps.isEmpty()) {
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
