package com.ziacik.cookcue.core.scheduler

import com.ziacik.cookcue.core.model.CookingTask
import com.ziacik.cookcue.core.model.ItineraryTiming
import com.ziacik.cookcue.core.model.Recipe
import com.ziacik.cookcue.core.model.ScheduleMode
import com.ziacik.cookcue.core.model.TimeWindow
import org.junit.Assert.assertEquals
import org.junit.Test

class ItinerarySchedulerTest {
	private val scheduler = ItineraryScheduler()
	private val epoch = 1_000_000L

	@Test
	fun fixedEventStaysPutWhenFlexibleStopRunsLong() {
		val recipe = recipe(
			CookingTask(
				id = "visit",
				title = "Visit",
				durationSeconds = 60,
				itineraryTiming = windows(0, 400),
			),
			CookingTask(
				id = "fixed",
				title = "Fixed",
				durationSeconds = 20,
				itineraryTiming = ItineraryTiming(
					fixedStartOptionsEpochSeconds = listOf(epoch + 100),
				),
			),
			CookingTask(
				id = "after",
				title = "After",
				durationSeconds = 60,
				dependsOn = setOf("visit"),
				itineraryTiming = windows(0, 400),
			),
		)

		val schedule = scheduler.schedule(
			recipe = recipe,
			durationOverrides = mapOf("visit" to 110),
			startOverrides = mapOf("visit" to 0),
		)

		assertEquals(0L, schedule.single { it.task.id == "visit" }.startSeconds)
		assertEquals(100L, schedule.single { it.task.id == "fixed" }.startSeconds)
		assertEquals(120L, schedule.single { it.task.id == "after" }.startSeconds)
	}

	@Test
	fun flexibleTaskMovesToLaterWindowWhenItNoLongerFits() {
		val recipe = recipe(
			CookingTask(
				id = "long",
				title = "Long",
				durationSeconds = 170,
				itineraryTiming = windows(0, 180),
			),
			CookingTask(
				id = "overflow",
				title = "Overflow",
				durationSeconds = 30,
				dependsOn = setOf("long"),
				itineraryTiming = ItineraryTiming(
					availabilityWindows = listOf(
						TimeWindow(epoch, epoch + 180),
						TimeWindow(epoch + 300, epoch + 500),
					),
				),
			),
		)

		val schedule = scheduler.schedule(recipe)

		assertEquals(300L, schedule.single { it.task.id == "overflow" }.startSeconds)
	}

	@Test
	fun missedFixedShowingMovesToNextOption() {
		val recipe = recipe(
			CookingTask(
				id = "show",
				title = "Show",
				durationSeconds = 20,
				itineraryTiming = ItineraryTiming(
					fixedStartOptionsEpochSeconds = listOf(epoch + 100, epoch + 200),
				),
			),
		)

		val schedule = scheduler.schedule(
			recipe = recipe,
			elapsedSeconds = 121,
		)

		assertEquals(200L, schedule.single().startSeconds)
	}

	private fun windows(start: Long, end: Long): ItineraryTiming {
		return ItineraryTiming(
			availabilityWindows = listOf(
				TimeWindow(epoch + start, epoch + end),
			),
		)
	}

	private fun recipe(vararg tasks: CookingTask): Recipe {
		return Recipe(
			id = "test",
			title = "Test",
			servings = 1,
			ingredients = emptyList(),
			resourceCapacities = emptyMap(),
			tasks = tasks.toList(),
			scheduleMode = ScheduleMode.ITINERARY,
			scheduleStartEpochSeconds = epoch,
		)
	}
}
