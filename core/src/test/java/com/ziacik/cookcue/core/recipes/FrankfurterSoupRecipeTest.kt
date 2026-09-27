package com.ziacik.cookcue.core.recipes

import com.ziacik.cookcue.core.model.TaskKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrankfurterSoupRecipeTest {
	private val recipe = FrankfurterSoupRecipe.recipe

	@Test
	fun recipeHasExpectedIngredientsAndServingCount() {
		val ingredients = recipe.ingredients.associate { it.name to it.amount }

		assertEquals(2, recipe.servings)
		assertEquals("2 ks (cca 150 g)", ingredients["frankfurtské párky"])
		assertEquals("2 ks (cca 300 g)", ingredients["stredné zemiaky"])
		assertEquals("100 ml", ingredients["smotana na varenie"])
		assertEquals("1 PL", ingredients["hladká múka"])
		assertEquals("700 ml", ingredients["voda"])
	}

	@Test
	fun heatingLiquidAndPotatoPrepCanOverlap() {
		val waitForBoil = recipe.tasks.single { it.id == "wait-for-boil" }
		val prepPotatoes = recipe.tasks.single { it.id == "prep-potatoes" }

		assertEquals(TaskKind.EVENT, waitForBoil.kind)
		assertTrue("add-liquid" in waitForBoil.dependsOn)
		assertTrue("add-liquid" in prepPotatoes.dependsOn)
	}

	@Test
	fun sausageAndSlurryPrepHappenWhilePotatoesCook() {
		val prepSausages = recipe.tasks.single { it.id == "prep-sausages" }
		val mixSlurry = recipe.tasks.single { it.id == "mix-slurry" }

		assertTrue("add-potatoes" in prepSausages.dependsOn)
		assertTrue("add-potatoes" in mixSlurry.dependsOn)
	}

	@Test
	fun potatoDonenessIsStateDriven() {
		val potatoesReady = recipe.tasks.single { it.id == "potatoes-ready" }

		assertEquals(TaskKind.EVENT, potatoesReady.kind)
		assertEquals("ÁNO", potatoesReady.actionLabel)
		assertEquals("NIE", potatoesReady.retryActionLabel)
		assertEquals(3 * 60L, potatoesReady.retryAfterSeconds)
	}

	@Test
	fun slurryComesOnlyAfterPotatoesAndSausages() {
		val addSausages = recipe.tasks.single { it.id == "add-sausages" }
		val addSlurry = recipe.tasks.single { it.id == "add-slurry" }

		assertTrue("potatoes-ready" in addSausages.dependsOn)
		assertTrue("heat-sausages" in addSlurry.dependsOn)
		assertTrue("mix-slurry" in addSlurry.dependsOn)
	}
}
