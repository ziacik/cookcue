package com.ziacik.cookcue.core.scheduler

import com.ziacik.cookcue.core.model.CookingTask
import com.ziacik.cookcue.core.model.Recipe
import com.ziacik.cookcue.core.model.ScheduleMode
import com.ziacik.cookcue.core.model.ScheduledTask

class ItineraryScheduler {
	fun schedule(
		recipe: Recipe,
		durationOverrides: Map<String, Long> = emptyMap(),
		startOverrides: Map<String, Long> = emptyMap(),
		satisfiedTaskIds: Set<String> = emptySet(),
		elapsedSeconds: Long = 0,
		earliestUnscheduledStartSeconds: Long = 0,
	): List<ScheduledTask> {
		require(recipe.scheduleMode == ScheduleMode.ITINERARY)
		val epochStart = requireNotNull(recipe.scheduleStartEpochSeconds)

		val ids = recipe.tasks.map { it.id }.toSet()
		require(ids.size == recipe.tasks.size) {
			"Itinerary task ids must be unique."
		}
		durationOverrides.forEach { (id, duration) ->
			require(id in ids && duration > 0)
		}
		startOverrides.forEach { (id, start) ->
			require(id in ids && start >= 0)
		}
		require(satisfiedTaskIds.all(ids::contains))

		val scheduledById = linkedMapOf<String, ScheduledTask>()
		val occupied = mutableListOf<ScheduledTask>()

		fun durationFor(task: CookingTask): Long {
			return durationOverrides[task.id] ?: task.durationSeconds
		}

		fun pin(
			task: CookingTask,
			startSeconds: Long,
			occupiesTime: Boolean = true,
		) {
			val item = ScheduledTask(
				task = task,
				startSeconds = startSeconds,
				endSeconds = startSeconds + durationFor(task),
			)
			scheduledById[task.id] = item
			if (occupiesTime) {
				occupied += item
			}
		}

		// Completed/running tasks keep their actual start. Fixed events are anchors that never
		// move because a flexible stop took longer. If an event offers multiple fixed starts,
		// pick the first showing that has not already finished.
		recipe.tasks.forEach { task ->
			val explicitStart = startOverrides[task.id]
			if (explicitStart != null) {
				pin(
					task = task,
					startSeconds = explicitStart,
					occupiesTime = task.id !in satisfiedTaskIds,
				)
				return@forEach
			}

			val options = task.itineraryTiming
				?.fixedStartOptionsEpochSeconds
				.orEmpty()
			if (options.isNotEmpty()) {
				val relativeOptions = options.map { it - epochStart }
				val duration = durationFor(task)
				val selected = relativeOptions.firstOrNull {
					it + duration + FIXED_OPTION_GRACE_SECONDS > elapsedSeconds
				} ?: relativeOptions.last()
				pin(
					task = task,
					startSeconds = selected,
					occupiesTime = task.id !in satisfiedTaskIds,
				)
			}
		}

		val remaining = recipe.tasks
			.filterNot { scheduledById.containsKey(it.id) }
			.toMutableList()

		while (remaining.isNotEmpty()) {
			val task = remaining.firstOrNull { candidate ->
				candidate.dependsOn.all(scheduledById::containsKey)
			} ?: error("Itinerary contains a dependency cycle or an invalid fixed dependency.")

			val dependencyEnd = task.dependsOn
				.asSequence()
				.filterNot(satisfiedTaskIds::contains)
				.maxOfOrNull { scheduledById.getValue(it).endSeconds }
				?: 0L
			val notBefore = maxOf(dependencyEnd, earliestUnscheduledStartSeconds)
			val duration = durationFor(task)

			val windows = task.itineraryTiming
				?.availabilityWindows
				.orEmpty()
			require(windows.isNotEmpty()) {
				"Flexible itinerary task '${task.id}' must define availability windows."
			}

			val start = windows
				.asSequence()
				.map { window ->
					(window.startEpochSeconds - epochStart) to
						(window.endEpochSeconds - epochStart)
				}
				.filter { (_, end) -> end > notBefore }
				.mapNotNull { (windowStart, windowEnd) ->
					findSlot(
						duration = duration,
						notBefore = maxOf(notBefore, windowStart),
						windowEnd = windowEnd,
						occupied = occupied,
					)
				}
				.firstOrNull()
				?: error("No itinerary window can fit task '${task.id}'.")

			pin(
				task = task,
				startSeconds = start,
				occupiesTime = task.id !in satisfiedTaskIds,
			)
			remaining.remove(task)
		}

		return scheduledById.values.sortedWith(
			compareBy<ScheduledTask> { it.startSeconds }
				.thenBy { it.endSeconds }
				.thenBy { it.task.id }
		)
	}

	private companion object {
		const val FIXED_OPTION_GRACE_SECONDS = 10 * 60L
	}

	private fun findSlot(
		duration: Long,
		notBefore: Long,
		windowEnd: Long,
		occupied: List<ScheduledTask>,
	): Long? {
		var candidate = notBefore
		while (candidate + duration <= windowEnd) {
			val collision = occupied
				.asSequence()
				.filter { it.startSeconds < candidate + duration && it.endSeconds > candidate }
				.minByOrNull { it.startSeconds }

			if (collision == null) {
				return candidate
			}

			candidate = maxOf(candidate, collision.endSeconds)
		}

		return null
	}
}
