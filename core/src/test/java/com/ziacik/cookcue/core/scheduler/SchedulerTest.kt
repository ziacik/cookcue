package com.ziacik.cookcue.core.scheduler

import com.ziacik.cookcue.core.model.CookProfile
import com.ziacik.cookcue.core.model.CookingTask
import com.ziacik.cookcue.core.model.Recipe
import com.ziacik.cookcue.core.model.ResourceRequirement
import com.ziacik.cookcue.core.model.ScheduledTask
import com.ziacik.cookcue.core.model.Skill
import com.ziacik.cookcue.core.model.TaskKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulerTest {
	private val scheduler = Scheduler()

	@Test
	fun twoHumanTasksCannotOverlap() {
		val result = scheduler.schedule(
			recipe(
				capacities = mapOf("cook" to 1),
				CookingTask("onion", "Cut onion", 60, resources = uses("cook")),
				CookingTask("carrot", "Cut carrot", 60, resources = uses("cook")),
			),
		)

		assertFalse(overlaps(result[0], result[1]))
		assertEquals(120, result.maxOf { it.endSeconds })
	}

	@Test
	fun humanPrepCanOverlapUnattendedCooking() {
		val result = scheduler.schedule(
			recipe(
				capacities = mapOf("cook" to 1, "pot" to 1, "burner" to 1),
				CookingTask(
					"simmer",
					"Simmer",
					600,
					resources = uses("pot", "burner"),
					kind = TaskKind.WAIT,
				),
				CookingTask("chop", "Chop", 120, resources = uses("cook")),
			),
		)

		val simmer = result.single { it.task.id == "simmer" }
		val chop = result.single { it.task.id == "chop" }

		assertTrue(overlaps(simmer, chop))
		assertEquals(0, simmer.startSeconds)
		assertEquals(0, chop.startSeconds)
	}

	@Test
	fun slowerKnifeSkillChangesOnlyHumanWork() {
		val result = scheduler.schedule(
			recipe(
				capacities = mapOf("cook" to 1, "pot" to 1),
				CookingTask(
					"wait",
					"Wait",
					300,
					resources = uses("pot"),
					kind = TaskKind.WAIT,
				),
				CookingTask(
					"chop",
					"Chop",
					60,
					resources = uses("cook"),
					skill = Skill.KNIFE,
				),
			),
			CookProfile(speedBySkill = mapOf(Skill.KNIFE to 2.0)),
		)

		assertEquals(300, result.single { it.task.id == "wait" }.endSeconds)
		assertEquals(120, result.single { it.task.id == "chop" }.endSeconds)
	}

	@Test
	fun eventDurationOverrideMovesDependentTimer() {
		val source = recipe(
			capacities = mapOf("pot" to 1, "burner" to 1),
			CookingTask(
				id = "boil",
				title = "Wait for boil",
				durationSeconds = 240,
				resources = uses("pot", "burner"),
				kind = TaskKind.EVENT,
				actionLabel = "BOILING",
			),
			CookingTask(
				id = "simmer",
				title = "Simmer",
				durationSeconds = 2700,
				dependsOn = setOf("boil"),
				resources = uses("pot", "burner"),
				kind = TaskKind.WAIT,
			),
		)

		val estimate = scheduler.schedule(source)
		assertEquals(240, estimate.single { it.task.id == "simmer" }.startSeconds)

		val actual = scheduler.schedule(
			recipe = source,
			durationOverrides = mapOf("boil" to 420),
		)
		assertEquals(420, actual.single { it.task.id == "simmer" }.startSeconds)
		assertEquals(3120, actual.single { it.task.id == "simmer" }.endSeconds)
	}


	@Test
	fun delayedParallelTaskDoesNotShortenIndependentCookingTask() {
		val source = recipe(
			capacities = mapOf(
				"cook" to 2,
				"pan" to 1,
				"burner" to 1,
				"toaster" to 1,
			),
			CookingTask(
				id = "onion",
				title = "Cook onion",
				durationSeconds = 300,
				resources = uses("cook", "pan", "burner"),
			),
			CookingTask(
				id = "toast",
				title = "Make toast",
				durationSeconds = 240,
				resources = uses("cook", "toaster"),
			),
			CookingTask(
				id = "eggs",
				title = "Cook eggs",
				durationSeconds = 120,
				dependsOn = setOf("onion"),
				resources = uses("cook", "pan", "burner"),
			),
			CookingTask(
				id = "finish",
				title = "Serve",
				durationSeconds = 60,
				dependsOn = setOf("eggs", "toast"),
				resources = uses("cook"),
			),
		)

		val result = scheduler.schedule(
			recipe = source,
			durationOverrides = mapOf("toast" to 480),
		)
		val onion = result.single { it.task.id == "onion" }
		val toast = result.single { it.task.id == "toast" }
		val eggs = result.single { it.task.id == "eggs" }
		val finish = result.single { it.task.id == "finish" }

		assertEquals(onion.endSeconds, eggs.startSeconds)
		assertEquals(120, eggs.endSeconds - eggs.startSeconds)
		assertTrue(eggs.startSeconds < toast.endSeconds)
		assertTrue(finish.startSeconds >= eggs.endSeconds)
		assertTrue(finish.startSeconds >= toast.endSeconds)
	}


	@Test
	fun realStartIsNotLostWhenPreviousStepFinishesEarly() {
		val source = recipe(
			capacities = mapOf("cook" to 1, "pan" to 1),
			CookingTask("prep", "Prepare ingredients", 240, resources = uses("cook")),
			CookingTask(
				"onion", "Saute onion", 300,
				dependsOn = setOf("prep"),
				resources = uses("cook", "pan"),
			),
			CookingTask(
				"paprika", "Add paprika", 90,
				dependsOn = setOf("onion"),
				resources = uses("cook", "pan"),
			),
		)

		val planned = scheduler.schedule(source)
		assertEquals(240L, planned.single { it.task.id == "onion" }.startSeconds)

		// Prep was completed in one second, but the onion was only started
		// at second 100. Its 5-minute timer must not inherit 99 elapsed seconds.
		val live = scheduler.schedule(
			recipe = source,
			durationOverrides = mapOf("prep" to 1),
			startOverrides = mapOf("prep" to 0, "onion" to 100),
		)
		assertEquals(100L, live.single { it.task.id == "onion" }.startSeconds)
		assertEquals(400L, live.single { it.task.id == "paprika" }.startSeconds)

		val finishedEarly = scheduler.schedule(
			recipe = source,
			durationOverrides = mapOf("prep" to 1, "onion" to 30),
			startOverrides = mapOf("prep" to 0, "onion" to 100),
		)
		assertEquals(130L, finishedEarly.single { it.task.id == "paprika" }.startSeconds)
	}

	@Test
	fun actualStartOfPanWorkDoesNotShortenIndependentSimmer() {
		val source = recipe(
			capacities = mapOf("cook" to 1, "pan" to 1, "pot" to 1, "burner" to 2),
			CookingTask(
				"simmer", "Simmer broth", 600,
				kind = TaskKind.WAIT, resources = uses("pot", "burner"),
			),
			CookingTask(
				"onion", "Saute onion", 300,
				resources = uses("cook", "pan", "burner"),
			),
		)
		val actual = scheduler.schedule(
			recipe = source,
			startOverrides = mapOf("onion" to 420),
		)
		assertEquals(0L, actual.single { it.task.id == "simmer" }.startSeconds)
		assertEquals(600L, actual.single { it.task.id == "simmer" }.endSeconds)
		assertEquals(420L, actual.single { it.task.id == "onion" }.startSeconds)
	}

	private fun recipe(
		capacities: Map<String, Int>,
		vararg tasks: CookingTask,
	) = Recipe(
		id = "test",
		title = "Test",
		servings = 1,
		ingredients = emptyList(),
		resourceCapacities = capacities,
		tasks = tasks.toList(),
	)

	private fun uses(vararg resources: String): Set<ResourceRequirement> {
		return resources.map { ResourceRequirement(it) }.toSet()
	}

	private fun overlaps(a: ScheduledTask, b: ScheduledTask): Boolean {
		return a.startSeconds < b.endSeconds && b.startSeconds < a.endSeconds
	}
}
