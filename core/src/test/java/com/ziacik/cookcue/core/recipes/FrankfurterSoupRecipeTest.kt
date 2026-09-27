package com.ziacik.cookcue.core.recipes

import com.ziacik.cookcue.core.model.TaskKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrankfurterSoupRecipeTest {
	private val recipe = FrankfurterSoupRecipe.recipe

	@Test
	fun recipeIsNonCreamyAndUsesBouillon() {
		val ingredients = recipe.ingredients.associate { it.name to it.amount }

		assertEquals(2, recipe.servings)
		assertEquals("2 ks (cca 150 g)", ingredients["frankfurtské párky"])
		assertEquals("cca 250 g", ingredients["zemiaky"])
		assertEquals("4 strúčiky", ingredients["cesnak"])
		assertEquals("1 PL", ingredients["hladká múka"])
		assertEquals("1 PL", ingredients["gulášové korenie"])
		assertEquals("1 kocka", ingredients["bujón"])
		assertEquals("750 ml", ingredients["voda"])
		assertFalse(ingredients.keys.any { it.contains("smot", ignoreCase = true) })
	}

	@Test
	fun sausagesAreBrownedAndRemovedBeforeOnionBase() {
		val brownSausages = recipe.tasks.single { it.id == "brown-sausages" }
		val removeSausages = recipe.tasks.single { it.id == "remove-sausages" }
		val sauteOnion = recipe.tasks.single { it.id == "saute-onion" }

		assertTrue("prep-sausages" in brownSausages.dependsOn)
		assertTrue("brown-sausages" in removeSausages.dependsOn)
		assertTrue("prep-onion" in sauteOnion.dependsOn)
	}

	@Test
	fun flourGarlicSpiceAndBouillonFollowTheIntendedOrder() {
		val flour = recipe.tasks.single { it.id == "add-flour" }
		val garlicSpice = recipe.tasks.single { it.id == "garlic-spice" }
		val liquid = recipe.tasks.single { it.id == "add-bouillon-water" }

		assertTrue("saute-onion" in flour.dependsOn)
		assertTrue("add-flour" in garlicSpice.dependsOn)
		assertTrue("garlic-spice" in liquid.dependsOn)
		assertTrue(liquid.instruction.contains("1 kocku bujónu"))
	}

	@Test
	fun potatoPrepRunsDuringBaseSimmer() {
		val baseSimmer = recipe.tasks.single { it.id == "base-simmer" }
		val prepPotatoes = recipe.tasks.single { it.id == "prep-potatoes" }

		assertEquals(TaskKind.WAIT, baseSimmer.kind)
		assertEquals(15 * 60L, baseSimmer.durationSeconds)
		assertTrue("wait-for-boil" in prepPotatoes.dependsOn)
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
	fun brownedSausagesAndRemainingGarlicReturnAtTheEnd() {
		val finish = recipe.tasks.single { it.id == "finish-soup" }

		assertTrue("potatoes-ready" in finish.dependsOn)
		assertTrue("prep-finish" in finish.dependsOn)
		assertTrue(finish.instruction.contains("opečené párky"))
		assertTrue(finish.instruction.contains("zostávajúce 2 strúčiky cesnaku"))
	}
}
