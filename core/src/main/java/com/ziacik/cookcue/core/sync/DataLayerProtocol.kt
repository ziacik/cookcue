package com.ziacik.cookcue.core.sync

object DataLayerProtocol {
	const val SESSION_PATH = "/cookcue/session"
	const val ACTION_PATH = "/cookcue/action"

	const val ACTION_REQUEST_STATE = "request_state"
	const val ACTION_START = "start"
	const val ACTION_PREVIOUS = "previous"
	const val ACTION_NEXT = "next"
	const val ACTION_CONFIRM_EVENT = "confirm_event"

	const val KEY_VERSION = "version"
	const val KEY_STARTED = "started"
	const val KEY_CURRENT_TASK_ID = "current_task_id"
	const val KEY_CURRENT_TITLE = "current_title"
	const val KEY_CURRENT_INSTRUCTION = "current_instruction"
	const val KEY_CURRENT_TIPS = "current_tips"
	const val KEY_CURRENT_REMAINING_SECONDS = "current_remaining_seconds"
	const val KEY_BACKGROUND_TITLE = "background_title"
	const val KEY_BACKGROUND_REMAINING_SECONDS = "background_remaining_seconds"
	const val KEY_EVENT_TASK_ID = "event_task_id"
	const val KEY_EVENT_TITLE = "event_title"
	const val KEY_EVENT_INSTRUCTION = "event_instruction"
	const val KEY_EVENT_TIPS = "event_tips"
	const val KEY_EVENT_ACTION_LABEL = "event_action_label"
	const val KEY_NEXT_TITLE = "next_title"
	const val KEY_NEXT_IN_SECONDS = "next_in_seconds"
	const val KEY_CAN_PREVIOUS = "can_previous"
	const val KEY_CAN_NEXT = "can_next"

	fun encodeAction(
		action: String,
		taskId: String = "",
	): ByteArray {
		return (action + "\n" + taskId).toByteArray(Charsets.UTF_8)
	}

	fun decodeAction(payload: ByteArray): Pair<String, String> {
		val text = payload.toString(Charsets.UTF_8)
		val action = text.substringBefore('\n')
		val taskId = text.substringAfter('\n', "")
		return action to taskId
	}
}
