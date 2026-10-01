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

enum class ScheduleMode {
	COOKING,
	ITINERARY,
}

data class TimeWindow(
	val startEpochSeconds: Long,
	val endEpochSeconds: Long,
) {
	init {
		require(endEpochSeconds > startEpochSeconds)
	}
}

data class ItineraryTiming(
	val fixedStartOptionsEpochSeconds: List<Long> = emptyList(),
	val availabilityWindows: List<TimeWindow> = emptyList(),
) {
	init {
		require(fixedStartOptionsEpochSeconds == fixedStartOptionsEpochSeconds.sorted()) {
			"Fixed start options must be sorted."
		}
		require(fixedStartOptionsEpochSeconds.distinct().size == fixedStartOptionsEpochSeconds.size) {
			"Fixed start options must be unique."
		}
	}
}

data class TaskLink(
	val label: String,
	val url: String,
) {
	init {
		require(label.isNotBlank())
		require(url.isNotBlank())
	}
}

enum class SensorActivationMode {
	SUGGEST,
	AUTO_ACTIVATE,
}

sealed interface TaskSensor

data class LocationProximitySensor(
	val latitude: Double,
	val longitude: Double,
	val radiusMeters: Float = 80f,
	val activationMode: SensorActivationMode = SensorActivationMode.SUGGEST,
) : TaskSensor {
	init {
		require(latitude in -90.0..90.0)
		require(longitude in -180.0..180.0)
		require(radiusMeters > 0f)
	}
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
	val retryActionLabel: String? = null,
	val retryAfterSeconds: Long? = null,
	val optional: Boolean = false,
	val links: List<TaskLink> = emptyList(),
	val sensors: List<TaskSensor> = emptyList(),
	val itineraryTiming: ItineraryTiming? = null,
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

		if (retryAfterSeconds != null || retryActionLabel != null) {
			require(kind == TaskKind.EVENT) {
				"Only event task '$id' can define a retry action."
			}
			require(retryAfterSeconds != null && retryAfterSeconds > 0) {
				"Retryable event task '$id' must define a positive retryAfterSeconds."
			}
			require(!retryActionLabel.isNullOrBlank()) {
				"Retryable event task '$id' must define retryActionLabel."
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
	val preCookingNote: String? = null,
	val troubleshooting: List<TroubleshootingTip> = emptyList(),
	val scheduleMode: ScheduleMode = ScheduleMode.COOKING,
	val scheduleStartEpochSeconds: Long? = null,
	val scheduleTimeZoneId: String? = null,
) {
	init {
		if (scheduleMode == ScheduleMode.ITINERARY) {
			require(scheduleStartEpochSeconds != null) {
				"Itinerary recipe '$id' must define scheduleStartEpochSeconds."
			}
		}
	}
}

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
