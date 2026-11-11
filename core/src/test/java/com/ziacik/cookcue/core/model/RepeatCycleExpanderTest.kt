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
}
