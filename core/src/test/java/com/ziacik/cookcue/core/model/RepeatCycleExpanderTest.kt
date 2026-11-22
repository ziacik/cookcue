package com.ziacik.cookcue.core.model

import com.ziacik.cookcue.core.scheduler.Scheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RepeatCycleExpanderTest {
	private val ready = CookingTask(
		id = "ready",
		title = "Priprav polohu",
		kind = TaskKind.EVENT,
		durationSeconds = 1,
		actionLabel = "POLOHA PRIPRAVENÁ",
	)
	private val hold = CookingTask(
		id = "hold",
		title = "Výdrž",
		kind = TaskKind.WAIT,
		durationSeconds = 5,
	)

	@Test
	fun threeRepetitionsHaveStableIdsAndSequentialDependencies() {
		val expanded = RepeatCycleExpander.expand(
			id = "exercise",
			title = "Cvik",
			count = 3,
			steps = listOf(ready, hold),
			dependsOn = setOf("introduction"),
		)

		assertEquals(6, expanded.size)
		assertEquals("exercise/r1/ready", expanded.first().id)
		assertEquals("exercise/r3/hold", expanded.last().id)
		assertEquals(setOf("introduction"), expanded.first().dependsOn)
		expanded.zipWithNext().forEach { (previous, next) ->
			assertEquals(setOf(previous.id), next.dependsOn)
		}
		assertEquals(listOf(1, 1, 2, 2, 3, 3), expanded.map { it.repeatInfo?.repetition })
		assertTrue(expanded.all { it.repeatInfo?.count == 3 })

		val introduction = ready.copy(id = "introduction", title = "Úvod", repeatInfo = null)
		val scheduled = Scheduler().schedule(
			Recipe(
				id = "exercise",
				title = "Cvik",
				servings = 1,
				ingredients = emptyList(),
				resourceCapacities = emptyMap(),
				tasks = listOf(introduction) + expanded,
			)
		)
		assertEquals(19L, scheduled.last().endSeconds)
		assertEquals(5L, scheduled[2].task.durationSeconds)
	}

	@Test
	fun cyclesRequirePositiveCountAndNonemptyDistinctSequentialSteps() {
		assertThrows(IllegalArgumentException::class.java) {
			RepeatCycleExpander.expand("exercise", "Cvik", 0, listOf(ready))
		}
		assertThrows(IllegalArgumentException::class.java) {
			RepeatCycleExpander.expand("exercise", "Cvik", 3, emptyList())
		}
		assertThrows(IllegalArgumentException::class.java) {
			RepeatCycleExpander.expand("exercise", "Cvik", 3, listOf(ready, ready))
		}
		assertThrows(IllegalArgumentException::class.java) {
			RepeatCycleExpander.expand(
				"exercise",
				"Cvik",
				3,
				listOf(ready.copy(dependsOn = setOf("external"))),
			)
		}
	}

	@Test
	fun flexibleWaitCanFinishEarlyWithoutSkippingTheNextEvent() {
		val setup = CookingTask(id = "setup", title = "Setup", durationSeconds = 10)
		val phases = listOf(
			CookingTask(id = "spread", title = "Spread", durationSeconds = 5),
			CookingTask(
				id = "side",
				title = "Fry one side",
				durationSeconds = 100,
				kind = TaskKind.WAIT,
				allowEarlyCompletion = true,
			),
			CookingTask(
				id = "flip",
				title = "Flip",
				durationSeconds = 1,
				kind = TaskKind.EVENT,
				actionLabel = "FLIPPED",
			),
		)
		val tasks = listOf(setup) + RepeatCycleExpander.expand(
			id = "pancakes",
			title = "Pancakes",
			count = 2,
			steps = phases,
			dependsOn = setOf("setup"),
		)
		val recipe = Recipe(
			id = "pancakes",
			title = "Pancakes",
			servings = 1,
			ingredients = emptyList(),
			resourceCapacities = emptyMap(),
			tasks = tasks,
		)
		assertTrue(tasks.single { it.id == "pancakes/r1/side" }.allowEarlyCompletion)
		val original = Scheduler().schedule(recipe)
		assertEquals(115L, original.single { it.task.id == "pancakes/r1/flip" }.startSeconds)
		val shortened = Scheduler().schedule(
			recipe = recipe,
			durationOverrides = mapOf("pancakes/r1/side" to 40L),
		)
		assertEquals(55L, shortened.single { it.task.id == "pancakes/r1/flip" }.startSeconds)
		assertEquals(56L, shortened.single { it.task.id == "pancakes/r2/spread" }.startSeconds)
	}

	@Test
	fun onlyWaitTasksCanAllowEarlyCompletion() {
		assertThrows(IllegalArgumentException::class.java) {
			CookingTask(
				id = "exact-action",
				title = "Action",
				durationSeconds = 10,
				allowEarlyCompletion = true,
			)
		}
	}
}
