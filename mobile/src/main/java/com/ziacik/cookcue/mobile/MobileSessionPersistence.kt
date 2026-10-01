package com.ziacik.cookcue.mobile

import android.content.Context
import android.os.SystemClock

object MobileSessionPersistence {
	private const val PREFS = "cookcue-session"
	private const val KEY_RECIPE_ID = "recipe-id"
	private const val KEY_STARTED_AT = "started-at"
	private const val KEY_STARTED_WALL_CLOCK = "started-wall-clock"
	private const val KEY_DURATION_OVERRIDES = "duration-overrides"
	private const val KEY_TASK_START_OVERRIDES = "task-start-overrides"
	private const val KEY_EVENT_DEFERRED_UNTIL = "event-deferred-until"

	@Volatile
	private var loaded = false

	fun ensureLoaded(context: Context) {
		if (loaded) {
			return
		}

		synchronized(this) {
			if (loaded) {
				return
			}

			val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
			val recipeId = prefs.getString(KEY_RECIPE_ID, null)
			val storedStartedAt = prefs.getLong(KEY_STARTED_AT, -1L)
			val startedAt = storedStartedAt
				.takeIf { it >= 0L && it <= SystemClock.elapsedRealtime() }
			val storedWallClock = prefs.getLong(KEY_STARTED_WALL_CLOCK, -1L)
			val startedWallClock = storedWallClock.takeIf { it > 0L }
			val overrides = decodeOverrides(
				prefs.getString(KEY_DURATION_OVERRIDES, null).orEmpty()
			)
			val taskStartOverrides = decodeOverrides(
				prefs.getString(KEY_TASK_START_OVERRIDES, null).orEmpty()
			)
			val eventDeferredUntil = decodeOverrides(
				prefs.getString(KEY_EVENT_DEFERRED_UNTIL, null).orEmpty()
			)

			CookingSessionController.restore(
				recipeId = recipeId,
				startedAt = startedAt,
				sessionStartedWallClockMillis = startedWallClock,
				durationOverrides = overrides,
				taskStartOverrides = taskStartOverrides,
				eventDeferredUntil = eventDeferredUntil,
			)
			loaded = true
		}
	}

	fun save(context: Context) {
		loaded = true
		val overrides = encodeMap(CookingSessionController.durationOverrides)
		val taskStartOverrides = encodeMap(CookingSessionController.taskStartOverrides)
		val eventDeferredUntil = encodeMap(CookingSessionController.eventDeferredUntil)

		context
			.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
			.edit()
			.putString(KEY_RECIPE_ID, CookingSessionController.selectedRecipeId)
			.putLong(
				KEY_STARTED_AT,
				CookingSessionController.startedAt ?: -1L,
			)
			.putLong(
				KEY_STARTED_WALL_CLOCK,
				CookingSessionController.sessionStartedWallClockMillis ?: -1L,
			)
			.putString(KEY_DURATION_OVERRIDES, overrides)
			.putString(KEY_TASK_START_OVERRIDES, taskStartOverrides)
			.putString(KEY_EVENT_DEFERRED_UNTIL, eventDeferredUntil)
			.apply()
	}

	private fun encodeMap(value: Map<String, Long>): String {
		return value.entries.joinToString(";") { (taskId, number) ->
			taskId + "=" + number
		}
	}

	private fun decodeOverrides(value: String): Map<String, Long> {
		if (value.isBlank()) {
			return emptyMap()
		}

		return value
			.split(';')
			.mapNotNull { item ->
				val separator = item.indexOf('=')
				if (separator <= 0 || separator == item.lastIndex) {
					return@mapNotNull null
				}

				val taskId = item.substring(0, separator)
				val duration = item.substring(separator + 1).toLongOrNull()
					?: return@mapNotNull null
				taskId to duration
			}
			.toMap()
	}
}
