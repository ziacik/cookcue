package com.ziacik.cookcue.core.scheduler

import com.ziacik.cookcue.core.model.CookingTask
import com.ziacik.cookcue.core.model.ScheduledTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveTaskClockTest {
	private val onion = CookingTask("onion", "Saute onion", 300)

	@Test
	fun aNewTimerStartsWhenTheActionActuallyAppears() {
		val planned = ScheduledTask(onion, startSeconds = 120, endSeconds = 420)
		val observed = ActiveTaskClock.newlyStarted(
			activeActions = listOf(planned),
			elapsedSeconds = 350,
			recordedStarts = emptyMap(),
		)

		// A 5-minute onion timer must show 0:00 elapsed, not 3:50.
		assertEquals(350L, observed["onion"])
	}

	@Test
	fun recomputingTheScheduleNeverResetsAnAlreadyStartedTimer() {
		val recalculated = ScheduledTask(onion, startSeconds = 30, endSeconds = 330)
		val newStarts = ActiveTaskClock.newlyStarted(
			activeActions = listOf(recalculated),
			elapsedSeconds = 390,
			recordedStarts = mapOf("onion" to 350),
		)

		assertTrue(newStarts.isEmpty())
	}

	@Test
	fun aRunningTimersVisibleStartNeverMovesOnReschedule() {
		val reprojected = ScheduledTask(onion, startSeconds = 540, endSeconds = 840)
		val active = ActiveTaskClock.withRecordedStart(reprojected, mapOf("onion" to 350))
		assertEquals(350L, active.startSeconds)
		assertEquals(650L, active.endSeconds)
	}

	@Test
	fun aTaskMayBeginAtItsOriginalStartWithoutArtificialDelay() {
		val planned = ScheduledTask(onion, startSeconds = 100, endSeconds = 400)
		val observed = ActiveTaskClock.newlyStarted(
			activeActions = listOf(planned),
			elapsedSeconds = 100,
			recordedStarts = emptyMap(),
		)

		assertEquals(100L, observed["onion"])
	}
}
