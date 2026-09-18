package com.ziacik.cookcue.core.model

data class ResourceRequirement(
	val resource: String,
	val units: Int = 1,
) {
	init {
		require(resource.isNotBlank())
		require(units > 0)
	}
}

enum class TaskKind {
	ACTIVE,
	WAIT,
	EVENT,
}

enum class Skill {
	GENERAL,
	KNIFE,
	PEELING,
}

data class CookingTask(
	val id: String,
	val title: String,
	val durationSeconds: Long,
	val dependsOn: Set<String> = emptySet(),
	val resources: Set<ResourceRequirement> = emptySet(),
	val kind: TaskKind = TaskKind.ACTIVE,
	val skill: Skill = Skill.GENERAL,
	val instruction: String = title,
	val tips: List<String> = emptyList(),
	val actionLabel: String? = null,
) {
	init {
		require(id.isNotBlank())
		require(title.isNotBlank())
		require(durationSeconds > 0)
		if (kind == TaskKind.EVENT) {
			require(!actionLabel.isNullOrBlank()) {
				"Event task '$id' must define actionLabel."
			}
		}
	}
}

data class Ingredient(
	val name: String,
	val amount: String,
)

data class TroubleshootingTip(
	val problem: String,
	val advice: String,
)

data class Recipe(
	val id: String,
	val title: String,
	val description: String = "",
	val servings: Int,
	val ingredients: List<Ingredient>,
	val resourceCapacities: Map<String, Int>,
	val tasks: List<CookingTask>,
	val troubleshooting: List<TroubleshootingTip> = emptyList(),
)

data class CookProfile(
	val speedBySkill: Map<Skill, Double> = emptyMap(),
) {
	fun durationFor(task: CookingTask): Long {
		if (task.kind != TaskKind.ACTIVE) {
			return task.durationSeconds
		}

		val multiplier = speedBySkill[task.skill] ?: 1.0
		require(multiplier > 0.0)
		return (task.durationSeconds * multiplier).toLong().coerceAtLeast(1)
	}

	companion object {
		val Default = CookProfile()
	}
}

data class ScheduledTask(
	val task: CookingTask,
	val startSeconds: Long,
	val endSeconds: Long,
)
