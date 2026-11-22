package com.ziacik.cookcue.core.scheduler

import com.ziacik.cookcue.core.model.CookingTask
import com.ziacik.cookcue.core.model.Recipe
import com.ziacik.cookcue.core.model.ResourceRequirement
import com.ziacik.cookcue.core.model.ScheduledTask
import com.ziacik.cookcue.core.model.TaskKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreparationPauseTest {
	private val prep = CookingTask(
		id = "prep", title = "Chop potatoes", durationSeconds = 120,
		resources = setOf(ResourceRequirement("cook"), ResourceRequirement("sink")),
	)
	private val heating = CookingTask(
		id = "heat", title = "Heat pan", durationSeconds = 90,
		resources = setOf(ResourceRequirement("cook"), ResourceRequirement("burner")),
	)
	private val waiting = CookingTask(
		id = "wait", title = "Cook", durationSeconds = 300, kind = TaskKind.WAIT,
		resources = setOf(ResourceRequirement("burner")),
	)
	private val recipe = Recipe(
		id = "test", title = "Test", servings = 1, ingredients = emptyList(),
		resourceCapacities = mapOf("cook" to 1, "sink" to 1, "burner" to 1),
		tasks = listOf(prep, heating, waiting),
	)
	private fun scheduled(task: CookingTask) =
		ScheduledTask(task, startSeconds = 0, endSeconds = task.durationSeconds)

	@Test
	fun preparationCanBePausedBeforeHeatingStarts() {
		assertTrue(PreparationPausePolicy.canPause(
			recipe, listOf(scheduled(prep)), emptyList(), emptyList(), setOf("prep"),
		))
	}

	@Test
	fun cannotPauseAfterHeatWasStartedEvenIfCurrentActionIsPrep() {
		assertFalse(PreparationPausePolicy.canPause(
			recipe, listOf(scheduled(prep)), emptyList(), emptyList(), setOf("heat"),
		))
	}

	@Test
	fun cannotPauseWithAnActiveCountdownOrHeatingAction() {
		assertFalse(PreparationPausePolicy.canPause(
			recipe, listOf(scheduled(prep)), listOf(scheduled(waiting)), emptyList(), emptySet(),
		))
		assertFalse(PreparationPausePolicy.canPause(
			recipe, listOf(scheduled(heating)), emptyList(), emptyList(), emptySet(),
		))
	}

	@Test
	fun frozenTimeRemainsExactAcrossAnArbitraryPauseDuration() {
		val frozen = PreparationPauseClock.freeze(nowMillis = 125_678, startedAtMillis = 20_000)
		val resumedAt = PreparationPauseClock.resumeStart(
			nowMillis = 600_000, frozenElapsedMillis = frozen,
		)
		assertEquals(105_678L, frozen)
		assertEquals(105_678L, 600_000 - resumedAt)
	}
}
