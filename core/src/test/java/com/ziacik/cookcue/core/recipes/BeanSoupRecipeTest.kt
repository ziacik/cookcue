package com.ziacik.cookcue.core.recipes

import com.ziacik.cookcue.core.model.TaskKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BeanSoupRecipeTest {
	private val recipe = BeanSoupRecipe.recipe

	@Test
	fun canonicalIngredientsMatchFinalRecipe() {
		val ingredients = recipe.ingredients.associate { it.name to it.amount }

		assertEquals("120 g", ingredients["suchá červená fazuľa"])
		assertEquals("150 g", ingredients["údená klobása"])
		assertEquals("1/3 ks", ingredients["menšia mrkva"])
		assertEquals("1/2 kocky", ingredients["zeleninový bujón"])
		assertEquals("1/4 ČL", ingredients["kvasný liehový ocot (8 %)"])
		assertEquals(
			"1/2 ČL na začiatok dochucovania; podľa chuti až cca 3/4 ČL",
			ingredients["soľ"],
		)
		assertEquals(
			"800 ml na začiatok + horúca voda na dolievanie",
			ingredients["voda"],
		)
		assertFalse(ingredients.keys.any { it.contains("petrž", ignoreCase = true) })
	}

	@Test
	fun bouillonAndSoakingStayInCorrectSteps() {
		val rinse = recipe.tasks.single { it.id == "rinse" }
		val startBeans = recipe.tasks.single { it.id == "start-beans" }

		assertTrue(rinse.instruction.contains("8–12 hodín"))
		assertTrue(startBeans.instruction.contains("1/2 kocky zeleninového bujónu"))
		assertTrue(startBeans.instruction.contains("800 ml"))
	}

	@Test
	fun beanAndFinalCookingAreStateDriven() {
		val beanCheck = recipe.tasks.single { it.id == "beans-nearly-ready" }
		val beanReady = recipe.tasks.single { it.id == "beans-ready" }
		val finalReady = recipe.tasks.single { it.id == "final-ready" }

		assertEquals(TaskKind.EVENT, beanCheck.kind)
		assertEquals("FAZUĽA JE SKORO HOTOVÁ", beanCheck.actionLabel)
		assertEquals(TaskKind.EVENT, beanReady.kind)
		assertEquals("FAZUĽA JE MÄKKÁ", beanReady.actionLabel)
		assertEquals(TaskKind.EVENT, finalReady.kind)
		assertEquals("VŠETKO JE MÄKKÉ", finalReady.actionLabel)
	}

	@Test
	fun sausageStartsOnlyWhenBeansAreNearlyReady() {
		val sausage = recipe.tasks.single { it.id == "brown-sausage" }
		assertTrue("beans-nearly-ready" in sausage.dependsOn)
	}

	@Test
	fun waterAdjustmentAndFiveMinuteRestArePreserved() {
		val adjustWater = recipe.tasks.single { it.id == "adjust-water" }
		val rest = recipe.tasks.single { it.id == "rest" }

		assertTrue(adjustWater.instruction.contains("100–150 ml"))
		assertEquals(5 * 60L, rest.durationSeconds)
	}

	@Test
	fun troubleshootingMatchesFinalRecipe() {
		assertEquals(7, recipe.troubleshooting.size)
		assertTrue(recipe.troubleshooting.any {
			it.problem == "Mdlá chuť" && it.advice.contains("až potom ďalší ocot")
		})
		assertTrue(recipe.troubleshooting.any {
			it.problem == "Príliš slaná" && it.advice.contains("Ďalší bujón nepridávaj")
		})
		assertTrue(recipe.troubleshooting.any {
			it.problem == "Príliš kyslá / veľa octu" &&
				it.advice.contains("sódy bikarbóny")
		})
	}
}
