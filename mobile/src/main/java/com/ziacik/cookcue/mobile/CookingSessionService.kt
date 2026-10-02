package com.ziacik.cookcue.mobile

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.location.LocationManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.ziacik.cookcue.core.model.LocationProximitySensor
import com.ziacik.cookcue.core.model.ScheduleMode
import com.ziacik.cookcue.core.model.SensorActivationMode
import com.ziacik.cookcue.core.model.TaskKind

class CookingSessionService : Service() {
	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
	private var monitorJob: Job? = null
	private var transitionInitialized = false
	private var previousTransitionKey: String? = null
	private var previousSilentTransitionVersion = 0L
	private var previousOverdueReminderKey: String? = null
	private var previousCompleted = false
	private var previousOngoingText: String? = null
	private val locationSensorMonitor by lazy { LocationSensorMonitor(applicationContext) }
	private var monitoredLocationTaskIds: Set<String> = emptySet()

	override fun onCreate() {
		super.onCreate()
		ensureSessionChannel()
		MobileTransitionNotifier.ensureChannel(this)
		CookingSessionController.initialize(applicationContext)
		MobileSessionPersistence.ensureLoaded(applicationContext)
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		MobileSessionPersistence.ensureLoaded(applicationContext)

		if (
			CookingSessionController.startedAt == null ||
			CookingSessionController.paused
		) {
			stopSelf()
			return START_NOT_STICKY
		}

		val snapshot = CookingSessionController.snapshot()
		startAsForeground(snapshot)

		if (monitorJob?.isActive != true) {
			monitorJob = scope.launch {
				monitorSession()
			}
		}

		return START_STICKY
	}

	override fun onDestroy() {
		monitorJob?.cancel()
		stopLocationMonitoring()
		scope.cancel()
		super.onDestroy()
	}

	override fun onBind(intent: Intent?): IBinder? = null

	private suspend fun monitorSession() {
		while (currentCoroutineContext().isActive) {
			val snapshot = CookingSessionController.snapshot()
			if (!snapshot.started || snapshot.paused) {
				stopSelf()
				return
			}

			syncLocationMonitoring(snapshot)
			updateOngoingNotification(snapshot)
			handleTransition(snapshot)
			handleCompletion(snapshot)
			handleOverdueReminder(snapshot)

			delay(500)
		}
	}

	private fun syncLocationMonitoring(snapshot: MobileSessionSnapshot) {
		if (
			!snapshot.started ||
			CookingSessionController.recipe.scheduleMode != ScheduleMode.ITINERARY ||
			checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
				PackageManager.PERMISSION_GRANTED ||
			!locationServicesEnabled()
		) {
			stopLocationMonitoring()
			return
		}

		val eligibleTasks = CookingSessionController.recipe.tasks.filter { task ->
			val progress = snapshot.taskProgress[task.id]
			task.kind == TaskKind.ACTIVE &&
				(progress == TaskProgress.PENDING || progress == TaskProgress.DEFERRED) &&
				task.sensors.any { it is LocationProximitySensor }
		}
		val ids = eligibleTasks.mapTo(linkedSetOf()) { it.id }
		if (ids.isEmpty()) {
			stopLocationMonitoring()
			return
		}
		if (ids == monitoredLocationTaskIds) {
			return
		}

		monitoredLocationTaskIds = ids
		locationSensorMonitor.start(eligibleTasks, ::handleNearbySuggestion)
	}

	private fun handleNearbySuggestion(match: NearbyTaskSuggestion?) {
		if (match == null) {
			CookingSessionController.clearNearbySuggestion()
			return
		}
		if (CookingSessionController.isNearbySuggestionSnoozed(match.taskId)) {
			CookingSessionController.clearNearbySuggestion()
			return
		}

		val snapshot = CookingSessionController.snapshot()
		val progress = snapshot.taskProgress[match.taskId]
		if (progress != TaskProgress.PENDING && progress != TaskProgress.DEFERRED) {
			CookingSessionController.clearNearbySuggestion()
			return
		}

		if (
			match.activationMode == SensorActivationMode.AUTO_ACTIVATE &&
			snapshot.currentAction == null
		) {
			CookingSessionController.activateTask(
				taskId = match.taskId,
				silentTransition = false,
			)
			CookingSessionController.clearNearbySuggestion()
			MobileSessionPersistence.save(applicationContext)
			MobileSessionSync.publish(applicationContext)
			return
		}

		val changed = CookingSessionController.updateNearbySuggestion(match)
		if (changed && !CookingSessionController.appVisible) {
			MobileTransitionNotifier.notify(
				this,
				TransitionCue(
					key = "nearby:" + match.taskId,
					title = "Si blízko: " + match.taskTitle,
					text = "Otvor CookCue a aktivuj túto zastávku.",
				),
			)
		}
	}

	private fun stopLocationMonitoring() {
		if (monitoredLocationTaskIds.isEmpty()) {
			CookingSessionController.clearNearbySuggestion()
			return
		}
		monitoredLocationTaskIds = emptySet()
		locationSensorMonitor.stop()
		CookingSessionController.clearNearbySuggestion()
	}

	private fun handleTransition(snapshot: MobileSessionSnapshot) {
		val cue = snapshot.transitionCue()
		val silentTransitionVersion = CookingSessionController.silentTransitionVersion

		if (!transitionInitialized) {
			transitionInitialized = true
			previousTransitionKey = cue?.key
			previousSilentTransitionVersion = silentTransitionVersion
			return
		}

		val changedStep = cue?.key != previousTransitionKey
		val changedSilently =
			silentTransitionVersion != previousSilentTransitionVersion

		if (changedStep && !changedSilently && cue != null) {
			sendCue(cue)
		}

		previousTransitionKey = cue?.key
		previousSilentTransitionVersion = silentTransitionVersion
	}

	private fun handleCompletion(snapshot: MobileSessionSnapshot) {
		if (snapshot.completed && !previousCompleted) {
			sendCue(
				TransitionCue(
					key = "completed",
					title = if (isItinerary()) "Itinerár je hotový" else "Varenie je hotové",
					text = if (isItinerary()) {
						"Všetky zastávky sú vybavené. Ukonči itinerár v CookCue."
					} else {
						"Všetky kroky sú dokončené. Ukonči varenie v CookCue."
					},
				),
			)
		}
		previousCompleted = snapshot.completed
	}

	private fun handleOverdueReminder(snapshot: MobileSessionSnapshot) {
		if (isItinerary() || snapshot.paused) {
			previousOverdueReminderKey = null
			return
		}

		val current = snapshot.currentAction
		if (current == null) {
			previousOverdueReminderKey = null
			return
		}

		val elapsed = (snapshot.elapsedSeconds - current.startSeconds).coerceAtLeast(0)
		if (elapsed < current.task.durationSeconds) {
			previousOverdueReminderKey = null
			return
		}

		val reminderSlot = (elapsed - current.task.durationSeconds) / 60
		val reminderKey = current.task.id + ":" + reminderSlot
		if (reminderKey == previousOverdueReminderKey) {
			return
		}
		previousOverdueReminderKey = reminderKey

		sendCue(
			TransitionCue(
				key = "overdue:" + reminderKey,
				title = "Skontroluj: " + current.task.title,
				text = "Odhadovaný čas už uplynul. Pozri, či je krok hotový. " +
					current.task.instruction,
			),
		)
	}

	private fun sendCue(cue: TransitionCue) {
		val transitionId = System.currentTimeMillis()
		MobileTransitionNotifier.notify(this, cue)
		MobileSessionSync.publish(
			this,
			TransitionSignal(
				id = transitionId,
				title = cue.title,
				text = cue.text,
			),
		)
	}

	private fun startAsForeground(snapshot: MobileSessionSnapshot) {
		val notification = buildSessionNotification(snapshot)

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			val recipe = CookingSessionController.recipe
			val locationMode =
				recipe.scheduleMode == ScheduleMode.ITINERARY &&
					recipe.tasks.any { task ->
						task.sensors.any { it is LocationProximitySensor }
					} &&
					checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
						PackageManager.PERMISSION_GRANTED &&
					locationServicesEnabled()
			if (locationMode) {
				try {
					startForeground(
						SESSION_NOTIFICATION_ID,
						notification,
						ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
					)
				} catch (_: SecurityException) {
					startForeground(
						SESSION_NOTIFICATION_ID,
						notification,
						ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
					)
				}
			} else {
				startForeground(
					SESSION_NOTIFICATION_ID,
					notification,
					ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
				)
			}
		} else {
			startForeground(SESSION_NOTIFICATION_ID, notification)
		}

		previousOngoingText = sessionText(snapshot)
	}

	private fun updateOngoingNotification(snapshot: MobileSessionSnapshot) {
		val text = sessionText(snapshot)
		if (text == previousOngoingText) {
			return
		}
		previousOngoingText = text

		getSystemService(NotificationManager::class.java)
			.notify(SESSION_NOTIFICATION_ID, buildSessionNotification(snapshot))
	}

	private fun buildSessionNotification(snapshot: MobileSessionSnapshot): Notification {
		val contentIntent = PendingIntent.getActivity(
			this,
			0,
			Intent(this, MainActivity::class.java),
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
		)

		return Notification.Builder(this, SESSION_CHANNEL_ID)
			.setSmallIcon(android.R.drawable.ic_popup_sync)
			.setContentTitle(CookingSessionController.recipe.title)
			.setContentText(sessionText(snapshot))
			.setContentIntent(contentIntent)
			.setOngoing(true)
			.setOnlyAlertOnce(true)
			.setCategory(Notification.CATEGORY_PROGRESS)
			.build()
	}

	private fun sessionText(snapshot: MobileSessionSnapshot): String {
		if (snapshot.completed) {
			return if (isItinerary()) {
				"Itinerár je hotový — ukonči itinerár"
			} else {
				"Varenie je hotové — ukonči varenie"
			}
		}
		snapshot.pendingEvents.firstOrNull()?.let {
			return "Skontroluj: " + it.task.title
		}
		snapshot.currentAction?.let {
			return "Teraz: " + it.task.title
		}
		snapshot.background.minByOrNull { it.endSeconds }?.let {
			return "Čakám: " + it.task.title
		}
		return if (isItinerary()) {
			"CookCue stráži itinerár"
		} else {
			"CookCue stráži varenie"
		}
	}

	private fun isItinerary(): Boolean {
		return CookingSessionController.recipe.scheduleMode == ScheduleMode.ITINERARY
	}

	private fun locationServicesEnabled(): Boolean {
		val manager = getSystemService(LocationManager::class.java)
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			manager.isLocationEnabled
		} else {
			listOf(
				LocationManager.GPS_PROVIDER,
				LocationManager.NETWORK_PROVIDER,
			).any { provider ->
				runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
			}
		}
	}

	private fun ensureSessionChannel() {
		val channel = NotificationChannel(
			SESSION_CHANNEL_ID,
			"Prebiehajúca aktivita",
			NotificationManager.IMPORTANCE_LOW,
		).apply {
			description = "Udržiava aktívny recept alebo itinerár spoľahlivo spustený."
			setSound(null, null)
			enableVibration(false)
		}
		getSystemService(NotificationManager::class.java)
			.createNotificationChannel(channel)
	}

	companion object {
		private const val SESSION_CHANNEL_ID = "cooking_session"
		private const val SESSION_NOTIFICATION_ID = 2100

		fun syncRunningState(context: Context) {
			val appContext = context.applicationContext
			val intent = Intent(appContext, CookingSessionService::class.java)

			if (
				CookingSessionController.startedAt != null &&
				!CookingSessionController.paused
			) {
				try {
					appContext.startForegroundService(intent)
				} catch (_: RuntimeException) {
					// A background-start restriction can reject this on newer Android versions.
					// The next foreground app launch will start the service again.
				}
			} else {
				appContext.stopService(intent)
			}
		}
	}
}
