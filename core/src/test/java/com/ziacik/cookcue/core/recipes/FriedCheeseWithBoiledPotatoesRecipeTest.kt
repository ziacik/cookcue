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
		val potatoCheck = recipe.tasks.single { it.id == "potatoes-ready" }

		assertEquals(TaskKind.EVENT, potatoCheck.kind)
		assertEquals("MÄKKÉ", potatoCheck.actionLabel)
		assertEquals("EŠTE NIE", potatoCheck.retryActionLabel)
		assertEquals(3 * 60L, potatoCheck.retryAfterSeconds)
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
