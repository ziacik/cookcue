package com.ziacik.cookcue.core.scheduler

import com.ziacik.cookcue.core.model.ScheduledTask

/**
 * A planning estimate is not a stopwatch. An ACTIVE action may only become
 * visible after earlier steps are finished or skipped, so its timer begins
 * when it is first observed as actionable, never at a past planned start.
 */
object ActiveTaskClock {
	/**
	 * The clock is authoritative for a running action, even if an unrelated
	 * dependency causes the scheduler to project a different start later.
	 */
	fun withRecordedStart(
		planned: ScheduledTask,
		recordedStarts: Map<String, Long>,
	): ScheduledTask {
		val actualStart = recordedStarts[planned.task.id] ?: return planned
		return planned.copy(
			startSeconds = actualStart,
			endSeconds = actualStart + (planned.endSeconds - planned.startSeconds),
		)
	}

	fun newlyStarted(
		activeActions: List<ScheduledTask>,
		elapsedSeconds: Long,
		recordedStarts: Map<String, Long>,
	): Map<String, Long> {
		return activeActions
			.filterNot { recordedStarts.containsKey(it.task.id) }
			.associate { it.task.id to maxOf(it.startSeconds, elapsedSeconds) }
	}
}
