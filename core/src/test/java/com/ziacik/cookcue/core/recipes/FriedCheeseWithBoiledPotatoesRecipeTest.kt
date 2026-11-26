package com.ziacik.cookcue.core.recipes

import com.ziacik.cookcue.core.model.TaskKind
import com.ziacik.cookcue.core.scheduler.Scheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FriedCheeseWithBoiledPotatoesRecipeTest {
	private val recipe = FriedCheeseWithBoiledPotatoesRecipe.recipe

	@Test
	fun recipeHasExpectedSingleServingIngredients() {
		val ingredients = recipe.ingredients.associate { it.name to it.amount }

		assertEquals(1, recipe.servings)
		assertEquals("150 g", ingredients["eidam"])
		assertEquals("300 g", ingredients["zemiaky"])
		assertEquals("1 ks", ingredients["vajce"])
		assertTrue(ingredients.keys.any { it.contains("strúhanka") })
	}

	@Test
	fun cheeseUsesDoubleCoatingToReduceLeaking() {
		val breadCheese = recipe.tasks.single { it.id == "bread-cheese" }

		assertTrue(breadCheese.instruction.contains("vajce → strúhanka → vajce → strúhanka"))
	}

	@Test
	fun potatoDonenessIsStateDrivenAndRetryable() {
		val potatoCook = recipe.tasks.single { it.id == "potatoes-minimum-cook" }
		val potatoCheck = recipe.tasks.single { it.id == "potatoes-ready" }

		assertEquals(10 * 60L, potatoCook.durationSeconds)
		assertTrue(potatoCook.allowEarlyCompletion)
		assertTrue("wait-for-potato-boil" in potatoCook.dependsOn)

		assertEquals(TaskKind.EVENT, potatoCheck.kind)
		assertEquals("MÄKKÉ", potatoCheck.actionLabel)
		assertEquals("EŠTE NIE", potatoCheck.retryActionLabel)
		assertEquals(3 * 60L, potatoCheck.retryAfterSeconds)
	}

	@Test
	fun endingPotatoTimerEarlyStillRequiresDonenessConfirmation() {
		val scheduler = Scheduler()
		val schedule = scheduler.schedule(recipe)
		val originalWait = schedule.single { it.task.id == "potatoes-minimum-cook" }
		val shortened = scheduler.schedule(
			recipe = recipe,
			durationOverrides = mapOf(originalWait.task.id to 1L),
			startOverrides = mapOf(originalWait.task.id to originalWait.startSeconds),
		)
		val wait = shortened.single { it.task.id == originalWait.task.id }
		val check = shortened.single { it.task.id == "potatoes-ready" }
		val drain = shortened.single { it.task.id == "drain-potatoes" }

		assertEquals(1L, wait.endSeconds - wait.startSeconds)
		assertEquals(wait.endSeconds, check.startSeconds)
		assertEquals(TaskKind.EVENT, check.task.kind)
		assertTrue("potatoes-ready" in drain.task.dependsOn)
		assertTrue(drain.startSeconds >= check.endSeconds)
	}

	@Test
	fun cheesePrepRunsWhilePotatoesCookButFryingWaitsForPotatoes() {
		val schedule = Scheduler().schedule(recipe)
		val potatoCook = schedule.single { it.task.id == "potatoes-minimum-cook" }
		val breadCheese = schedule.single { it.task.id == "bread-cheese" }
		val heatOil = recipe.tasks.single { it.id == "heat-oil" }

		assertTrue(breadCheese.startSeconds < potatoCook.endSeconds)
		assertTrue("potatoes-ready" in heatOil.dependsOn)
		assertTrue("bread-cheese" in heatOil.dependsOn)
	}
}
