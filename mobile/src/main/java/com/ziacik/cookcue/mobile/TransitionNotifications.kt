package com.ziacik.cookcue.mobile

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

data class TransitionCue(
	val key: String,
	val title: String,
	val text: String,
)

fun MobileSessionSnapshot.transitionCue(): TransitionCue? {
	pendingEvents.firstOrNull()?.let { event ->
		return TransitionCue(
			key = "event:" + event.task.id,
			title = event.task.title,
			text = event.task.instruction,
		)
	}

	currentAction?.let { action ->
		return TransitionCue(
			key = "active:" + action.task.id,
			title = action.task.title,
			text = action.task.instruction,
		)
	}

	background.minByOrNull { it.endSeconds }?.let { wait ->
		return TransitionCue(
			key = "wait:" + wait.task.id,
			title = wait.task.title,
			text = wait.task.instruction,
		)
	}

	return null
}

object MobileTransitionNotifier {
	private const val CHANNEL_ID = "cooking_transitions"
	private const val NOTIFICATION_ID = 2001

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

	fun notify(
		context: Context,
		cue: TransitionCue,
	) {
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
			.setContentTitle(cue.title)
			.setContentText(cue.text)
			.setStyle(Notification.BigTextStyle().bigText(cue.text))
			.setContentIntent(contentIntent)
			.setAutoCancel(true)
			.setCategory(Notification.CATEGORY_REMINDER)
			.setPriority(Notification.PRIORITY_HIGH)
			.setLocalOnly(true)
			.build()

		context
			.getSystemService(NotificationManager::class.java)
			.notify(NOTIFICATION_ID, notification)
	}
}
