package com.ziacik.cookcue.core.scheduler

import com.ziacik.cookcue.core.model.ScheduledTask
import com.ziacik.cookcue.core.model.TaskKind

/** A pending recheck of an EVENT (not a WAIT task in the recipe schedule). */
data class EventRetryCountdown(
	val event: ScheduledTask,
	val untilSeconds: Long,
) {
	val durationSeconds: Long = requireNotNull(event.task.retryAfterSeconds)

	fun elapsedSeconds(nowSeconds: Long): Long =
		(nowSeconds - (untilSeconds - durationSeconds)).coerceIn(0L, durationSeconds)
}

object EventRetryCountdowns {
	fun active(
		schedule: List<ScheduledTask>,
		retryUntilById: Map<String, Long>,
		elapsedSeconds: Long,
		inactiveTaskIds: Set<String> = emptySet(),
	): List<EventRetryCountdown> = schedule.mapNotNull { scheduled ->
		if (
			scheduled.task.kind != TaskKind.EVENT ||
			scheduled.task.id in inactiveTaskIds ||
			scheduled.task.retryAfterSeconds == null
		) {
			return@mapNotNull null
		}
		val until = retryUntilById[scheduled.task.id] ?: return@mapNotNull null
		if (until <= elapsedSeconds) return@mapNotNull null
		EventRetryCountdown(scheduled, until)
	}.sortedBy(EventRetryCountdown::untilSeconds)
}
