package com.ziacik.cookcue.core.recipes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BeanSoupRecipeTest {
	@Test
	fun canonicalIngredientsDoNotLoseCriticalDetails() {
		val ingredients = BeanSoupRecipe.recipe.ingredients.associate { it.name to it.amount }

		assertEquals("120 g suchej", ingredients["malá červená fazuľa"])
		assertEquals("150 g", ingredients["údená klobása"])
		assertEquals("1/3 malej", ingredients["mrkva"])
		assertEquals("1/2 kocky", ingredients["zeleninový bujón"])
		assertEquals("1/4 ČL", ingredients["8 % ocot"])
		assertTrue("petržlen" !in ingredients)
	}
}
