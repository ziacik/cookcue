package com.ziacik.cookcue.mobile

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.ziacik.cookcue.core.sync.DataLayerProtocol

data class TransitionSignal(
	val id: Long,
	val title: String,
	val text: String,
)

object MobileSessionSync {
	fun publish(
		context: Context,
		transition: TransitionSignal? = null,
	) {
		val snapshot = CookingSessionController.snapshot()
		val current = snapshot.currentAction
		val background = snapshot.background.firstOrNull()
		val event = snapshot.pendingEvents.firstOrNull()
		val next = snapshot.nextScheduled

		val request = PutDataMapRequest.create(DataLayerProtocol.SESSION_PATH)
		request.dataMap.apply {
			putLong(DataLayerProtocol.KEY_VERSION, System.currentTimeMillis())
			putBoolean(DataLayerProtocol.KEY_STARTED, snapshot.started)
			putBoolean(DataLayerProtocol.KEY_COMPLETED, snapshot.completed)
			putString(DataLayerProtocol.KEY_RECIPE_TITLE, CookingSessionController.recipe.title)

			putString(DataLayerProtocol.KEY_CURRENT_TASK_ID, current?.task?.id.orEmpty())
			putString(DataLayerProtocol.KEY_CURRENT_TITLE, current?.task?.title.orEmpty())
			putString(DataLayerProtocol.KEY_CURRENT_INSTRUCTION, current?.task?.instruction.orEmpty())
			putString(DataLayerProtocol.KEY_CURRENT_TIPS, current?.task?.tips?.joinToString("\n").orEmpty())
			val currentElapsed = current
				?.let { (snapshot.elapsedSeconds - it.startSeconds).coerceAtLeast(0) }
				?: 0
			val currentEstimate = current?.task?.durationSeconds ?: 0
			putLong(
				DataLayerProtocol.KEY_CURRENT_REMAINING_SECONDS,
				(currentEstimate - currentElapsed).coerceAtLeast(0),
			)
			putLong(
				DataLayerProtocol.KEY_CURRENT_ESTIMATE_SECONDS,
				currentEstimate,
			)
			putLong(
				DataLayerProtocol.KEY_CURRENT_ELAPSED_SECONDS,
				currentElapsed,
			)

			putString(DataLayerProtocol.KEY_BACKGROUND_TITLE, background?.task?.title.orEmpty())
			putLong(
				DataLayerProtocol.KEY_BACKGROUND_REMAINING_SECONDS,
				background?.let { (it.endSeconds - snapshot.elapsedSeconds).coerceAtLeast(0) } ?: 0,
			)
			putLong(
				DataLayerProtocol.KEY_BACKGROUND_ESTIMATE_SECONDS,
				background?.let { (it.endSeconds - it.startSeconds).coerceAtLeast(0) } ?: 0,
			)

			putString(DataLayerProtocol.KEY_EVENT_TASK_ID, event?.task?.id.orEmpty())
			putString(DataLayerProtocol.KEY_EVENT_TITLE, event?.task?.title.orEmpty())
			putString(DataLayerProtocol.KEY_EVENT_INSTRUCTION, event?.task?.instruction.orEmpty())
			putString(DataLayerProtocol.KEY_EVENT_TIPS, event?.task?.tips?.joinToString("\n").orEmpty())
			putString(DataLayerProtocol.KEY_EVENT_ACTION_LABEL, event?.task?.actionLabel.orEmpty())
			putString(
				DataLayerProtocol.KEY_EVENT_RETRY_ACTION_LABEL,
				event?.task?.retryActionLabel.orEmpty(),
			)
			putLong(
				DataLayerProtocol.KEY_EVENT_RETRY_AFTER_SECONDS,
				event?.task?.retryAfterSeconds ?: 0,
			)

			putString(DataLayerProtocol.KEY_NEXT_TITLE, next?.task?.title.orEmpty())
			putLong(
				DataLayerProtocol.KEY_NEXT_IN_SECONDS,
				next?.let { (it.startSeconds - snapshot.elapsedSeconds).coerceAtLeast(0) } ?: 0,
			)

			putBoolean(DataLayerProtocol.KEY_CAN_PREVIOUS, snapshot.previousAction != null)
			putBoolean(DataLayerProtocol.KEY_CAN_NEXT, snapshot.nextAction != null)

			putLong(DataLayerProtocol.KEY_TRANSITION_ID, transition?.id ?: 0)
			putString(DataLayerProtocol.KEY_TRANSITION_TITLE, transition?.title.orEmpty())
			putString(DataLayerProtocol.KEY_TRANSITION_TEXT, transition?.text.orEmpty())
		}

		Wearable
			.getDataClient(context)
			.putDataItem(request.asPutDataRequest().setUrgent())
	}
}
