package com.ziacik.cookcue.wear

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import kotlin.math.abs

object WearTransitionNotifier {
	private const val CHANNEL_ID = "cooking_transitions"
	private const val NOTIFICATION_ID = 3001
	private const val PREFS = "transition_notifications"
	private const val KEY_LAST_TRANSITION_ID = "last_transition_id"
	private const val MAX_TRANSITION_AGE_MS = 30_000L

	fun ensureChannel(context: Context) {
		val manager = context.getSystemService(NotificationManager::class.java)
		val channel = NotificationChannel(
			CHANNEL_ID,
			"Prechody pri varení",
			NotificationManager.IMPORTANCE_HIGH,
		).apply {
			description = "Upozornenia, keď CookCue automaticky prejde na ďalší krok."
			enableVibration(true)
		}
		manager.createNotificationChannel(channel)
	}

	fun notifyIfNew(
		context: Context,
		transitionId: Long,
		title: String,
		text: String,
	) {
		if (transitionId <= 0 || title.isBlank()) {
			return
		}

		val now = System.currentTimeMillis()
		if (abs(now - transitionId) > MAX_TRANSITION_AGE_MS) {
			return
		}

		val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
		if (preferences.getLong(KEY_LAST_TRANSITION_ID, 0) == transitionId) {
			return
		}
		preferences.edit().putLong(KEY_LAST_TRANSITION_ID, transitionId).apply()

		if (
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
			context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
			PackageManager.PERMISSION_GRANTED
		) {
			return
		}

		ensureChannel(context)

		val contentIntent = PendingIntent.getActivity(
			context,
			0,
			Intent(context, MainActivity::class.java),
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
		)

		val notification = Notification.Builder(context, CHANNEL_ID)
			.setSmallIcon(android.R.drawable.ic_popup_reminder)
			.setContentTitle(title)
			.setContentText(text)
			.setStyle(Notification.BigTextStyle().bigText(text))
			.setContentIntent(contentIntent)
			.setAutoCancel(true)
			.setCategory(Notification.CATEGORY_REMINDER)
			.setPriority(Notification.PRIORITY_HIGH)
			.build()

		context
			.getSystemService(NotificationManager::class.java)
			.notify(NOTIFICATION_ID, notification)
	}
}
