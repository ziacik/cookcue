package com.ziacik.cookcue.core.recipes

import com.ziacik.cookcue.core.model.TaskKind
import com.ziacik.cookcue.core.scheduler.Scheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParboiledFriesRecipeTest {
	private val recipe = ParboiledFriesRecipe.recipe

	@Test
	fun waterHeatsWhilePotatoesAreRinsed() {
		val schedule = Scheduler().schedule(recipe)
		val rinse = schedule.single { it.task.id == "rinse-potatoes" }
		val water = schedule.single { it.task.id == "water-boiling" }

		assertTrue(rinse.startSeconds < water.endSeconds)
		assertTrue(water.startSeconds < rinse.endSeconds)
	}

	@Test
	fun dryingTakesTenMinutesIncludingDrainingAndFryerHeatsInParallel() {
		val schedule = Scheduler().schedule(recipe)
		val drain = schedule.single { it.task.id == "drain-spread" }
		val dry = schedule.single { it.task.id == "dry-fries" }
		val heat = schedule.single { it.task.id == "heat-fryer-155" }

		assertEquals(60L, drain.task.durationSeconds)
		assertEquals(9 * 60L, dry.task.durationSeconds)
		assertEquals(10 * 60L, dry.endSeconds - drain.startSeconds)
		assertEquals(dry.startSeconds, heat.startSeconds)
		assertTrue(heat.endSeconds <= dry.endSeconds)
	}

	@Test
	fun firstFryStartsOnlyAfterBasketLoading() {
		val load = recipe.tasks.single { it.id == "load-first-basket" }
		val firstFry = recipe.tasks.single { it.id == "first-fry" }

		assertEquals(60L, load.durationSeconds)
		assertEquals(setOf("load-first-basket"), firstFry.dependsOn)
		assertEquals(6 * 60L, firstFry.durationSeconds)
		assertEquals(TaskKind.WAIT, firstFry.kind)
	}

	@Test
	fun secondHeatAndCoolingRunTogetherForFiveMinutes() {
		val configured = ParboiledFriesRecipe.create(
			FryerTimingProfile(
				coldTo155Seconds = 7 * 60,
				from155To180Seconds = 5 * 60,
			)
		)
		val schedule = Scheduler().schedule(configured)
		val cool = schedule.single { it.task.id == "cool-fries" }
		val heat = schedule.single { it.task.id == "heat-fryer-180" }
		val lower = schedule.single { it.task.id == "lower-second-basket" }

		assertEquals(cool.startSeconds, heat.startSeconds)
		assertEquals(5 * 60L, cool.task.durationSeconds)
		assertEquals(5 * 60L, heat.task.durationSeconds)
		assertTrue(lower.startSeconds >= cool.endSeconds)
		assertTrue(lower.startSeconds >= heat.endSeconds)
	}

	@Test
	fun fryerWarmupTimesAreConfigurable() {
		val configured = ParboiledFriesRecipe.create(
			FryerTimingProfile(
				coldTo155Seconds = 8 * 60,
				from155To180Seconds = 4 * 60,
			)
		)

		assertEquals(
			8 * 60L,
			configured.tasks.single { it.id == "heat-fryer-155" }.durationSeconds,
		)
		assertEquals(
			4 * 60L,
			configured.tasks.single { it.id == "heat-fryer-180" }.durationSeconds,
		)
	}

	@Test
	fun parboilCheckIsRetryable() {
		val check = recipe.tasks.single { it.id == "parboil-check" }

		assertEquals(TaskKind.EVENT, check.kind)
		assertEquals("EŠTE NIE", check.retryActionLabel)
		assertEquals(60L, check.retryAfterSeconds)
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
