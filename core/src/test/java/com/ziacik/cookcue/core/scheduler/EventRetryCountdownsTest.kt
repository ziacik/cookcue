package com.ziacik.cookcue.core.scheduler

import com.ziacik.cookcue.core.model.CookingTask
import com.ziacik.cookcue.core.model.ScheduledTask
import com.ziacik.cookcue.core.model.TaskKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventRetryCountdownsTest {
	private fun event(id: String, retrySeconds: Long = 120) = ScheduledTask(
		task = CookingTask(
			id = id,
			title = "Check rice",
			durationSeconds = 30,
			kind = TaskKind.EVENT,
			actionLabel = "READY",
			retryActionLabel = "NOT YET",
			retryAfterSeconds = retrySeconds,
		),
		startSeconds = 10,
		endSeconds = 40,
	)

	@Test
	fun deferredEventKeepsItsOriginalTwoMinuteTimer() {
		val countdown = EventRetryCountdowns.active(
			schedule = listOf(event("rice")),
			retryUntilById = mapOf("rice" to 245L),
			elapsedSeconds = 125L,
		).single()

		assertEquals("rice", countdown.event.task.id)
		assertEquals(120L, countdown.durationSeconds)
		assertEquals(0L, countdown.elapsedSeconds(125L))
		assertEquals(73L, countdown.elapsedSeconds(198L))
		assertEquals(119L, countdown.elapsedSeconds(244L))
	}

	@Test
	fun countdownDisappearsAtDeadlineSoEventCanReappear() {
		val schedule = listOf(event("rice"))
		val until = mapOf("rice" to 245L)
		assertEquals(1, EventRetryCountdowns.active(schedule, until, 244L).size)
		assertTrue(EventRetryCountdowns.active(schedule, until, 245L).isEmpty())
	}

	@Test
	fun completedAndBlockedEventsNeverShowRetryCountdown() {
		val schedule = listOf(event("rice"), event("water", 30))
		val until = mapOf("rice" to 245L, "water" to 155L)
		val countdowns = EventRetryCountdowns.active(
			schedule = schedule,
			retryUntilById = until,
			elapsedSeconds = 125L,
			inactiveTaskIds = setOf("rice"),
		)
		assertEquals(listOf("water"), countdowns.map { it.event.task.id })
	}

	@Test
	fun noRetryWithoutDeferral() {
		assertTrue(EventRetryCountdowns.active(listOf(event("rice")), emptyMap(), 125L).isEmpty())
	}
}
