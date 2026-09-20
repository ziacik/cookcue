package com.ziacik.cookcue.wear

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.ziacik.cookcue.core.sync.DataLayerProtocol

class WearDataLayerService : WearableListenerService() {
	override fun onCreate() {
		super.onCreate()
		WearTransitionNotifier.ensureChannel(this)
	}

	override fun onDataChanged(dataEvents: DataEventBuffer) {
		try {
			for (event in dataEvents) {
				if (
					event.type == DataEvent.TYPE_CHANGED &&
					event.dataItem.uri.path == DataLayerProtocol.SESSION_PATH
				) {
					val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
					WatchSessionStore.update(dataMap)
					WearTransitionNotifier.notifyIfNew(
						context = this,
						transitionId = dataMap.getLong(DataLayerProtocol.KEY_TRANSITION_ID),
						title = dataMap.getString(DataLayerProtocol.KEY_TRANSITION_TITLE).orEmpty(),
						text = dataMap.getString(DataLayerProtocol.KEY_TRANSITION_TEXT).orEmpty(),
					)
				}
			}
		} finally {
			dataEvents.release()
		}
	}
}
