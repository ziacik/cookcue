package com.ziacik.cookcue.mobile

import android.os.Handler
import android.os.Looper
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.ziacik.cookcue.core.sync.DataLayerProtocol

class MobileDataLayerService : WearableListenerService() {
	private val mainHandler = Handler(Looper.getMainLooper())

	override fun onMessageReceived(messageEvent: MessageEvent) {
		if (messageEvent.path != DataLayerProtocol.ACTION_PATH) {
			return
		}

		val (action, taskId) = DataLayerProtocol.decodeAction(messageEvent.data)

		mainHandler.post {
			MobileSessionPersistence.ensureLoaded(applicationContext)

			when (action) {
				DataLayerProtocol.ACTION_REQUEST_STATE -> Unit
				DataLayerProtocol.ACTION_START -> CookingSessionController.start()
				DataLayerProtocol.ACTION_PREVIOUS -> CookingSessionController.previous()
				DataLayerProtocol.ACTION_NEXT -> CookingSessionController.next()
				DataLayerProtocol.ACTION_CONFIRM_EVENT -> CookingSessionController.confirmEvent(taskId)
				DataLayerProtocol.ACTION_DEFER_EVENT -> CookingSessionController.deferEvent(taskId)
				DataLayerProtocol.ACTION_COMPLETE_ACTIVE -> CookingSessionController.completeAction(taskId)
				else -> return@post
			}

			if (action != DataLayerProtocol.ACTION_REQUEST_STATE) {
				MobileSessionPersistence.save(applicationContext)
			}
			MobileSessionSync.publish(applicationContext)
		}
	}
}
