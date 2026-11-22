package com.ziacik.cookcue.core.scheduler

import com.ziacik.cookcue.core.model.Recipe
import com.ziacik.cookcue.core.model.ScheduleMode
import com.ziacik.cookcue.core.model.ScheduledTask
import com.ziacik.cookcue.core.model.TaskKind

/**
 * Preparation may be interrupted before heat or a timed WAIT/EVENT starts.
 * Hands-on resources such as "cook" and "sink" are safe to interrupt.
 * Unknown resources are conservatively treated as non-pausable.
 */
object PreparationPausePolicy {
	private val interruptibleResources = setOf("cook", "sink")

	fun canPause(
		recipe: Recipe,
		activeActions: List<ScheduledTask>,
		background: List<ScheduledTask>,
		pendingEvents: List<ScheduledTask>,
		startedTaskIds: Set<String>,
	): Boolean {
		if (
			recipe.scheduleMode != ScheduleMode.COOKING ||
			activeActions.isEmpty() ||
			background.isNotEmpty() ||
			pendingEvents.isNotEmpty()
		) return false

		fun interruptible(task: ScheduledTask): Boolean =
			task.task.kind == TaskKind.ACTIVE &&
				task.task.resources.all { it.resource in interruptibleResources }

		if (!activeActions.all(::interruptible)) return false

		return recipe.tasks
			.asSequence()
			.filter { it.id in startedTaskIds }
			.all { task ->
				task.kind == TaskKind.ACTIVE &&
					task.resources.all { it.resource in interruptibleResources }
			}
	}
}

object PreparationPauseClock {
	fun freeze(nowMillis: Long, startedAtMillis: Long): Long =
		(nowMillis - startedAtMillis).coerceAtLeast(0)

	fun resumeStart(nowMillis: Long, frozenElapsedMillis: Long): Long =
		nowMillis - frozenElapsedMillis.coerceAtLeast(0)
}
