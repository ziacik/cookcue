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
