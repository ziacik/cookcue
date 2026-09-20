package com.ziacik.cookcue.wear

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.wearable.DataMap
import com.ziacik.cookcue.core.sync.DataLayerProtocol

data class WatchSessionSnapshot(
	val synced: Boolean = false,
	val started: Boolean = false,
	val currentTaskId: String = "",
	val currentTitle: String = "",
	val currentInstruction: String = "",
	val currentTips: String = "",
	val currentRemainingSeconds: Long = 0,
	val currentEstimateSeconds: Long = 0,
	val currentElapsedSeconds: Long = 0,
	val backgroundTitle: String = "",
	val backgroundRemainingSeconds: Long = 0,
	val eventTaskId: String = "",
	val eventTitle: String = "",
	val eventInstruction: String = "",
	val eventTips: String = "",
	val eventActionLabel: String = "",
	val eventRetryActionLabel: String = "",
	val eventRetryAfterSeconds: Long = 0,
	val nextTitle: String = "",
	val nextInSeconds: Long = 0,
	val canPrevious: Boolean = false,
	val canNext: Boolean = false,
	val receivedAtElapsedRealtime: Long = 0,
)

object WatchSessionStore {
	var snapshot by mutableStateOf(WatchSessionSnapshot())
		private set

	fun update(dataMap: DataMap) {
		snapshot = WatchSessionSnapshot(
			synced = true,
			started = dataMap.getBoolean(DataLayerProtocol.KEY_STARTED),
			currentTaskId = dataMap.getString(DataLayerProtocol.KEY_CURRENT_TASK_ID).orEmpty(),
			currentTitle = dataMap.getString(DataLayerProtocol.KEY_CURRENT_TITLE).orEmpty(),
			currentInstruction = dataMap.getString(DataLayerProtocol.KEY_CURRENT_INSTRUCTION).orEmpty(),
			currentTips = dataMap.getString(DataLayerProtocol.KEY_CURRENT_TIPS).orEmpty(),
			currentRemainingSeconds = dataMap.getLong(DataLayerProtocol.KEY_CURRENT_REMAINING_SECONDS),
			currentEstimateSeconds = dataMap.getLong(DataLayerProtocol.KEY_CURRENT_ESTIMATE_SECONDS),
			currentElapsedSeconds = dataMap.getLong(DataLayerProtocol.KEY_CURRENT_ELAPSED_SECONDS),
			backgroundTitle = dataMap.getString(DataLayerProtocol.KEY_BACKGROUND_TITLE).orEmpty(),
			backgroundRemainingSeconds = dataMap.getLong(DataLayerProtocol.KEY_BACKGROUND_REMAINING_SECONDS),
			eventTaskId = dataMap.getString(DataLayerProtocol.KEY_EVENT_TASK_ID).orEmpty(),
			eventTitle = dataMap.getString(DataLayerProtocol.KEY_EVENT_TITLE).orEmpty(),
			eventInstruction = dataMap.getString(DataLayerProtocol.KEY_EVENT_INSTRUCTION).orEmpty(),
			eventTips = dataMap.getString(DataLayerProtocol.KEY_EVENT_TIPS).orEmpty(),
			eventActionLabel = dataMap.getString(DataLayerProtocol.KEY_EVENT_ACTION_LABEL).orEmpty(),
			eventRetryActionLabel = dataMap
				.getString(DataLayerProtocol.KEY_EVENT_RETRY_ACTION_LABEL)
				.orEmpty(),
			eventRetryAfterSeconds = dataMap.getLong(
				DataLayerProtocol.KEY_EVENT_RETRY_AFTER_SECONDS,
			),
			nextTitle = dataMap.getString(DataLayerProtocol.KEY_NEXT_TITLE).orEmpty(),
			nextInSeconds = dataMap.getLong(DataLayerProtocol.KEY_NEXT_IN_SECONDS),
			canPrevious = dataMap.getBoolean(DataLayerProtocol.KEY_CAN_PREVIOUS),
			canNext = dataMap.getBoolean(DataLayerProtocol.KEY_CAN_NEXT),
			receivedAtElapsedRealtime = SystemClock.elapsedRealtime(),
		)
	}
}
