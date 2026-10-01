package com.ziacik.cookcue.mobile

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.ziacik.cookcue.core.model.CookingTask
import com.ziacik.cookcue.core.model.LocationProximitySensor
import com.ziacik.cookcue.core.model.SensorActivationMode
import java.util.Locale

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
	private val geocoder = Geocoder(appContext, Locale.getDefault())
	private val mainHandler = Handler(Looper.getMainLooper())
	private val resolvedLocations = mutableMapOf<String, Location?>()

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
		resolveQueries(tasks)

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

	private fun resolveQueries(tasks: List<CookingTask>) {
		val queries = tasks
			.flatMap { task -> task.sensors.filterIsInstance<LocationProximitySensor>() }
			.map { it.locationQuery }
			.distinct()
			.filterNot(resolvedLocations::containsKey)

		if (queries.isEmpty()) {
			return
		}

		Thread(
			{
				queries.forEach { query ->
					val target = resolveQuery(query)
					mainHandler.post {
						resolvedLocations[query] = target
					}
				}
			},
			"CookCue-geocode",
		).start()
	}

	@Suppress("DEPRECATION")
	private fun resolveQuery(query: String): Location? {
		if (!Geocoder.isPresent()) {
			return null
		}

		val address = runCatching {
			geocoder.getFromLocationName(query, 1)?.firstOrNull()
		}.getOrNull() ?: return null

		return Location("cookcue-recipe").apply {
			latitude = address.latitude
			longitude = address.longitude
		}
	}

	private fun evaluate(current: Location) {
		val suggestion = tasks
			.asSequence()
			.flatMap { task ->
				task.sensors
					.filterIsInstance<LocationProximitySensor>()
					.asSequence()
					.mapNotNull { sensor ->
						val target = resolvedLocations[sensor.locationQuery] ?: return@mapNotNull null
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
	}
}
