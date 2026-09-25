package com.ziacik.cookcue.core.recipes

import com.ziacik.cookcue.core.model.TaskKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParboiledFriesRecipeTest {
	private val recipe = ParboiledFriesRecipe.recipe

	@Test
	fun recipeUsesParboilAndDoubleFryMethod() {
		val parboil = recipe.tasks.single { it.id == "parboil" }
		val firstFry = recipe.tasks.single { it.id == "first-fry" }
		val secondFry = recipe.tasks.single { it.id == "second-fry" }

		assertEquals(TaskKind.WAIT, parboil.kind)
		assertEquals(4 * 60L, parboil.durationSeconds)
		assertTrue(firstFry.instruction.contains("5–6 minút"))
		assertTrue(secondFry.instruction.contains("2–3 minúty"))
	}

	@Test
	fun parboilCheckIsRetryable() {
		val check = recipe.tasks.single { it.id == "parboil-check" }

		assertEquals(TaskKind.EVENT, check.kind)
		assertEquals("EŠTE NIE", check.retryActionLabel)
		assertEquals(60L, check.retryAfterSeconds)
	}

	@Test
	fun friesDryAndCoolBetweenCookingStages() {
		val airDry = recipe.tasks.single { it.id == "air-dry" }
		val firstFry = recipe.tasks.single { it.id == "first-fry" }
		val cool = recipe.tasks.single { it.id == "cool-fries" }
		val secondFry = recipe.tasks.single { it.id == "second-fry" }

		assertEquals(TaskKind.WAIT, airDry.kind)
		assertTrue("heat-oil-low" in firstFry.dependsOn)
		assertEquals(TaskKind.WAIT, cool.kind)
		assertTrue("heat-oil-high" in secondFry.dependsOn)
	}

	@Test
	fun recipeHasExpectedSingleServingIngredients() {
		val ingredients = recipe.ingredients.associate { it.name to it.amount }

		assertEquals(1, recipe.servings)
		assertEquals("300 g", ingredients["zemiaky typu C"])
		assertTrue(ingredients.keys.any { it.contains("ocot") })
		assertTrue(ingredients.keys.any { it.contains("olej") })
	}
}
