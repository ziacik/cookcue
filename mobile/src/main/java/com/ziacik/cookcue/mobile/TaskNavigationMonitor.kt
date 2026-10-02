package com.ziacik.cookcue.mobile

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import kotlin.math.roundToInt

data class TaskNavigationReading(
	val distanceMeters: Float? = null,
	val relativeBearingDegrees: Float? = null,
	val headingDegrees: Float? = null,
	val targetBearingDegrees: Float? = null,
	val accuracyMeters: Float? = null,
)

class TaskNavigationMonitor(
	context: Context,
	private val targetLatitude: Double,
	private val targetLongitude: Double,
	private val onReading: (TaskNavigationReading) -> Unit,
) : LocationListener, SensorEventListener {
	private val appContext = context.applicationContext
	private val locationManager =
		appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
	private val sensorManager =
		appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
	private val rotationSensor =
		sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
	private val target = Location("cookcue-task").apply {
		latitude = targetLatitude
		longitude = targetLongitude
	}

	private var currentLocation: Location? = null
	private var headingDegrees: Float? = null

	@SuppressLint("MissingPermission")
	fun start() {
		rotationSensor?.let { sensor ->
			sensorManager.registerListener(
				this,
				sensor,
				SensorManager.SENSOR_DELAY_UI,
			)
		}

		listOf(
			LocationManager.GPS_PROVIDER,
			LocationManager.NETWORK_PROVIDER,
		).forEach { provider ->
			runCatching {
				locationManager.requestLocationUpdates(
					provider,
					LOCATION_UPDATE_INTERVAL_MS,
					LOCATION_UPDATE_DISTANCE_METERS,
					this,
					Looper.getMainLooper(),
				)
				if (locationManager.isProviderEnabled(provider)) {
					locationManager.getLastKnownLocation(provider)?.let(::onLocationChanged)
				}
			}
		}
		emit()
	}

	fun stop() {
		runCatching { locationManager.removeUpdates(this) }
		sensorManager.unregisterListener(this)
	}

	override fun onLocationChanged(location: Location) {
		if (!isUsable(location)) {
			return
		}

		val previous = currentLocation
		currentLocation = when {
			previous == null -> location
			location.elapsedRealtimeNanos >= previous.elapsedRealtimeNanos -> location
			else -> previous
		}
		emit()
	}

	override fun onSensorChanged(event: SensorEvent) {
		if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) {
			return
		}

		val rotationMatrix = FloatArray(9)
		val orientation = FloatArray(3)
		SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
		SensorManager.getOrientation(rotationMatrix, orientation)
		headingDegrees = normalizeDegrees(
			Math.toDegrees(orientation[0].toDouble()).toFloat(),
		)
		emit()
	}

	override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

	override fun onProviderEnabled(provider: String) = Unit

	override fun onProviderDisabled(provider: String) = Unit

	@Deprecated("Deprecated in Android SDK")
	override fun onStatusChanged(
		provider: String?,
		status: Int,
		extras: Bundle?,
	) = Unit

	private fun emit() {
		val location = currentLocation
		val heading = headingDegrees
		if (location == null) {
			onReading(
				TaskNavigationReading(
					headingDegrees = heading,
				)
			)
			return
		}

		val targetBearing = normalizeDegrees(location.bearingTo(target))
		val relativeBearing = heading?.let { currentHeading ->
			normalizeSignedDegrees(targetBearing - currentHeading)
		}

		onReading(
			TaskNavigationReading(
				distanceMeters = location.distanceTo(target),
				relativeBearingDegrees = relativeBearing,
				headingDegrees = heading,
				targetBearingDegrees = targetBearing,
				accuracyMeters = location.accuracy.takeIf { location.hasAccuracy() },
			)
		)
	}

	private fun isUsable(location: Location): Boolean {
		val ageMillis =
			(SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000
		if (ageMillis > MAX_LOCATION_AGE_MILLIS) {
			return false
		}
		return !location.hasAccuracy() || location.accuracy <= MAX_LOCATION_ACCURACY_METERS
	}

	private fun normalizeDegrees(value: Float): Float {
		return ((value % 360f) + 360f) % 360f
	}

	private fun normalizeSignedDegrees(value: Float): Float {
		val normalized = normalizeDegrees(value)
		return if (normalized > 180f) normalized - 360f else normalized
	}

	private companion object {
		const val LOCATION_UPDATE_INTERVAL_MS = 1_500L
		const val LOCATION_UPDATE_DISTANCE_METERS = 1f
		const val MAX_LOCATION_AGE_MILLIS = 2 * 60 * 1000L
		const val MAX_LOCATION_ACCURACY_METERS = 100f
	}
}
