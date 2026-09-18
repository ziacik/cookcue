package com.ziacik.cookcue.mobile

import android.content.Context
import android.os.SystemClock

object MobileSessionPersistence {
	private const val PREFS = "cookcue-session"
	private const val KEY_STARTED_AT = "started-at"
	private const val KEY_DURATION_OVERRIDES = "duration-overrides"

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
			val storedStartedAt = prefs.getLong(KEY_STARTED_AT, -1L)
			val startedAt = storedStartedAt
				.takeIf { it >= 0L && it <= SystemClock.elapsedRealtime() }
			val overrides = decodeOverrides(
				prefs.getString(KEY_DURATION_OVERRIDES, null).orEmpty()
			)

			CookingSessionController.restore(
				startedAt = startedAt,
				durationOverrides = overrides,
			)
			loaded = true
		}
	}

	fun save(context: Context) {
		loaded = true
		val overrides = CookingSessionController.durationOverrides
			.entries
			.joinToString(";") { (taskId, duration) ->
				taskId + "=" + duration
			}

		context
			.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
			.edit()
			.putLong(
				KEY_STARTED_AT,
				CookingSessionController.startedAt ?: -1L,
			)
			.putString(KEY_DURATION_OVERRIDES, overrides)
			.apply()
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
