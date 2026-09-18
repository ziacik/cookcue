package com.ziacik.cookcue.wear

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.ziacik.cookcue.core.sync.DataLayerProtocol

class WearDataLayerService : WearableListenerService() {
	override fun onDataChanged(dataEvents: DataEventBuffer) {
		try {
			for (event in dataEvents) {
				if (
					event.type == DataEvent.TYPE_CHANGED &&
					event.dataItem.uri.path == DataLayerProtocol.SESSION_PATH
				) {
					WatchSessionStore.update(
						DataMapItem.fromDataItem(event.dataItem).dataMap
					)
				}
			}
		} finally {
			dataEvents.release()
		}
	}
}
