package com.ziacik.cookcue.wear

import android.content.Context
import com.google.android.gms.wearable.Wearable
import com.ziacik.cookcue.core.sync.DataLayerProtocol

object WearActionSender {
	fun send(
		context: Context,
		action: String,
		taskId: String = "",
	) {
		Wearable
			.getNodeClient(context)
			.connectedNodes
			.addOnSuccessListener { nodes ->
				val payload = DataLayerProtocol.encodeAction(action, taskId)
				nodes.forEach { node ->
					Wearable
						.getMessageClient(context)
						.sendMessage(
							node.id,
							DataLayerProtocol.ACTION_PATH,
							payload,
						)
				}
			}
	}
}
