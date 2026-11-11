package com.ziacik.cookcue.core.model

/**
 * Expands a reusable, sequential cycle into scheduler-compatible tasks.
 * A phase can be an EVENT (manual gate), WAIT (exact timer), or ACTIVE step.
 * The expansion is deterministic, so session progress survives app restarts.
 */
object RepeatCycleExpander {
	fun expand(
		id: String,
		title: String,
		count: Int,
		steps: List<CookingTask>,
		dependsOn: Set<String> = emptySet(),
	): List<CookingTask> {
		require(id.isNotBlank() && title.isNotBlank())
		require(count in 1..100) { "Repeat count must be between 1 and 100." }
		require(steps.isNotEmpty()) { "A cycle needs at least one step." }
		require(steps.map { it.id }.distinct().size == steps.size) {
			"Cycle step ids must be unique."
		}
		require(steps.all { it.dependsOn.isEmpty() }) {
			"Cycle phases are sequential; steps cannot declare independent dependencies."
		}
		require(steps.none { it.repeatInfo != null }) { "Nested cycles are unsupported." }

		return buildList {
			var previousId: String? = null
			for (round in 1..count) {
				for (step in steps) {
					val expandedId = id + "/r" + round + "/" + step.id
					add(
						step.copy(
							id = expandedId,
							title = id.uppercase() + " · " + step.title + " · " + round + "/" + count,
							dependsOn = previousId?.let { setOf(it) } ?: dependsOn,
							repeatInfo = RepeatInfo(id, title, round, count),
						)
					)
					previousId = expandedId
				}
			}
		}
	}
}
