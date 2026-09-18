package com.ziacik.cookcue.core.scheduler

import com.ziacik.cookcue.core.model.CookProfile
import com.ziacik.cookcue.core.model.CookingTask
import com.ziacik.cookcue.core.model.Recipe
import com.ziacik.cookcue.core.model.ResourceRequirement
import com.ziacik.cookcue.core.model.ScheduledTask

class Scheduler {
	fun schedule(
		recipe: Recipe,
		profile: CookProfile = CookProfile.Default,
		durationOverrides: Map<String, Long> = emptyMap(),
	): List<ScheduledTask> {
		validate(recipe, durationOverrides)

		val tasksById = recipe.tasks.associateBy { it.id }
		val children = recipe.tasks
			.flatMap { task -> task.dependsOn.map { dependency -> dependency to task.id } }
			.groupBy({ it.first }, { it.second })

		fun durationFor(task: CookingTask): Long {
			return durationOverrides[task.id] ?: profile.durationFor(task)
		}

		val criticalPathCache = mutableMapOf<String, Long>()

		fun criticalPath(id: String): Long {
			return criticalPathCache.getOrPut(id) {
				val task = tasksById.getValue(id)
				durationFor(task) + (children[id]?.maxOfOrNull(::criticalPath) ?: 0L)
			}
		}

		val scheduledById = mutableMapOf<String, ScheduledTask>()
		val scheduled = mutableListOf<ScheduledTask>()
		val remaining = recipe.tasks.toMutableList()

		while (remaining.isNotEmpty()) {
			val ready = remaining.filter { task ->
				task.dependsOn.all(scheduledById::containsKey)
			}

			check(ready.isNotEmpty()) {
				"Recipe contains a dependency cycle."
			}

			val task = ready.maxBy { criticalPath(it.id) }

			val dependenciesFinishedAt = task.dependsOn
				.maxOfOrNull { scheduledById.getValue(it).endSeconds }
				?: 0L

			val duration = durationFor(task)
			val start = earliestFeasibleStart(
				task = task,
				duration = duration,
				notBefore = dependenciesFinishedAt,
				scheduled = scheduled,
				capacities = recipe.resourceCapacities,
			)

			val item = ScheduledTask(
				task = task,
				startSeconds = start,
				endSeconds = start + duration,
			)

			scheduled += item
			scheduledById[task.id] = item
			remaining.remove(task)
		}

		return scheduled.sortedWith(
			compareBy<ScheduledTask> { it.startSeconds }
				.thenBy { it.endSeconds }
				.thenBy { it.task.id }
		)
	}

	private fun earliestFeasibleStart(
		task: CookingTask,
		duration: Long,
		notBefore: Long,
		scheduled: List<ScheduledTask>,
		capacities: Map<String, Int>,
	): Long {
		val candidates = buildSet {
			add(notBefore)
			scheduled
				.asSequence()
				.map { it.endSeconds }
				.filter { it >= notBefore }
				.forEach(::add)
		}.sorted()

		return candidates.first { candidate ->
			fits(task, duration, candidate, scheduled, capacities)
		}
	}

	private fun fits(
		task: CookingTask,
		duration: Long,
		start: Long,
		scheduled: List<ScheduledTask>,
		capacities: Map<String, Int>,
	): Boolean {
		val end = start + duration
		val overlapping = scheduled.filter { it.startSeconds < end && it.endSeconds > start }

		val boundaries = buildSet {
			add(start)
			add(end)
			overlapping.forEach {
				add(maxOf(start, it.startSeconds))
				add(minOf(end, it.endSeconds))
			}
		}.sorted()

		for ((segmentStart, segmentEnd) in boundaries.zipWithNext()) {
			if (segmentStart == segmentEnd) {
				continue
			}

			for (requirement in task.resources) {
				val capacity = capacities.getValue(requirement.resource)
				val alreadyUsed = overlapping
					.asSequence()
					.filter { it.startSeconds < segmentEnd && it.endSeconds > segmentStart }
					.sumOf { it.task.unitsOf(requirement.resource) }

				if (alreadyUsed + requirement.units > capacity) {
					return false
				}
			}
		}

		return true
	}

	private fun CookingTask.unitsOf(resource: String): Int {
		return resources
			.filter { it.resource == resource }
			.sumOf(ResourceRequirement::units)
	}

	private fun validate(
		recipe: Recipe,
		durationOverrides: Map<String, Long>,
	) {
		require(recipe.tasks.map { it.id }.distinct().size == recipe.tasks.size) {
			"Task ids must be unique."
		}

		val ids = recipe.tasks.mapTo(mutableSetOf()) { it.id }

		durationOverrides.forEach { (taskId, duration) ->
			require(taskId in ids) {
				"Duration override references missing task '$taskId'."
			}
			require(duration > 0) {
				"Duration override for '$taskId' must be positive."
			}
		}

		recipe.resourceCapacities.forEach { (resource, capacity) ->
			require(resource.isNotBlank())
			require(capacity > 0)
		}

		recipe.tasks.forEach { task ->
			task.dependsOn.forEach { dependency ->
				require(dependency in ids) {
					"Task '${task.id}' depends on missing task '$dependency'."
				}
			}

			task.resources.forEach { requirement ->
				val capacity = recipe.resourceCapacities[requirement.resource]
					?: error("Missing capacity for resource '${requirement.resource}'.")
				require(requirement.units <= capacity) {
					"Task '${task.id}' needs too much of '${requirement.resource}'."
				}
			}
		}
	}
}
