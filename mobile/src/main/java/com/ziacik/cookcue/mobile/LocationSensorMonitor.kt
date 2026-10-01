package com.ziacik.cookcue.mobile

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import com.ziacik.cookcue.core.model.CookingTask
import com.ziacik.cookcue.core.model.LocationProximitySensor
import com.ziacik.cookcue.core.model.SensorActivationMode

data class NearbyTaskSuggestion(
	val taskId: String,
	val taskTitle: String,
	val distanceMeters: Float,
	val activationMode: SensorActivationMode,
)

class LocationSensorMonitor(
	context: Context,
) : LocationListener {
	private val appContext = context.applicationContext
	private val locationManager =
		appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

	private var tasks: List<CookingTask> = emptyList()
	private var onSuggestion: (NearbyTaskSuggestion?) -> Unit = {}

	@SuppressLint("MissingPermission")
	fun start(
		tasks: List<CookingTask>,
		onSuggestion: (NearbyTaskSuggestion?) -> Unit,
	) {
		stop()
		this.tasks = tasks
		this.onSuggestion = onSuggestion
		listOf(
			LocationManager.NETWORK_PROVIDER,
			LocationManager.GPS_PROVIDER,
		).forEach { provider ->
			runCatching {
				if (locationManager.isProviderEnabled(provider)) {
					locationManager.requestLocationUpdates(
						provider,
						LOCATION_UPDATE_INTERVAL_MS,
						LOCATION_UPDATE_DISTANCE_METERS,
						this,
						Looper.getMainLooper(),
					)
					locationManager.getLastKnownLocation(provider)?.let(::evaluate)
				}
			}
		}
	}

	fun stop() {
		runCatching {
			locationManager.removeUpdates(this)
		}
		tasks = emptyList()
		onSuggestion(null)
	}

	override fun onLocationChanged(location: Location) {
		evaluate(location)
	}

	override fun onProviderEnabled(provider: String) = Unit

	override fun onProviderDisabled(provider: String) = Unit

	@Deprecated("Deprecated in Android SDK")
	override fun onStatusChanged(
		provider: String?,
		status: Int,
		extras: Bundle?,
	) = Unit

	private fun evaluate(current: Location) {
		val ageMillis =
			(SystemClock.elapsedRealtimeNanos() - current.elapsedRealtimeNanos) / 1_000_000
		if (ageMillis > MAX_LOCATION_AGE_MILLIS) {
			return
		}
		if (current.hasAccuracy() && current.accuracy > MAX_LOCATION_ACCURACY_METERS) {
			return
		}

		val suggestion = tasks
			.asSequence()
			.flatMap { task ->
				task.sensors
					.filterIsInstance<LocationProximitySensor>()
					.asSequence()
					.mapNotNull { sensor ->
						val target = Location("cookcue-recipe").apply {
							latitude = sensor.latitude
							longitude = sensor.longitude
						}
						val distance = current.distanceTo(target)
						if (distance > sensor.radiusMeters) {
							return@mapNotNull null
						}
						NearbyTaskSuggestion(
							taskId = task.id,
							taskTitle = task.title,
							distanceMeters = distance,
							activationMode = sensor.activationMode,
						)
					}
			}
			.minByOrNull(NearbyTaskSuggestion::distanceMeters)

		onSuggestion(suggestion)
	}

	private companion object {
		const val LOCATION_UPDATE_INTERVAL_MS = 10_000L
		const val LOCATION_UPDATE_DISTANCE_METERS = 10f
		const val MAX_LOCATION_AGE_MILLIS = 2 * 60 * 1000L
		const val MAX_LOCATION_ACCURACY_METERS = 100f
	}
}
