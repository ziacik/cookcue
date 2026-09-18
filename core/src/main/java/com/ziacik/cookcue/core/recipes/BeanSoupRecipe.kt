package com.ziacik.cookcue.core.recipes

import com.ziacik.cookcue.core.model.CookingTask
import com.ziacik.cookcue.core.model.Ingredient
import com.ziacik.cookcue.core.model.Recipe
import com.ziacik.cookcue.core.model.ResourceRequirement
import com.ziacik.cookcue.core.model.Skill
import com.ziacik.cookcue.core.model.TaskKind

object BeanSoupRecipe {
	private const val COOK = "cook"
	private const val POT = "large-pot"
	private const val PAN = "pan"
	private const val BURNER = "burner"
	private const val SINK = "sink"

	private fun uses(vararg resources: String): Set<ResourceRequirement> {
		return resources.map { ResourceRequirement(it) }.toSet()
	}

	val recipe = Recipe(
		id = "bean-soup",
		title = "Naša fazuľová polievka",
		servings = 2,
		ingredients = listOf(
			Ingredient("malá červená fazuľa", "120 g suchej"),
			Ingredient("údená klobása", "150 g"),
			Ingredient("cibuľa", "1 malá"),
			Ingredient("cesnak", "2 strúčiky"),
			Ingredient("mrkva", "1 malá"),
			Ingredient("zemiak", "1 malý"),
			Ingredient("bobkový list", "1 ks"),
			Ingredient("nové korenie", "3 guľôčky"),
			Ingredient("sladká paprika", "1/2 ČL"),
			Ingredient("majorán", "1/2 ČL"),
			Ingredient("masť alebo olej", "1 PL"),
			Ingredient("voda", "cca 800 ml"),
			Ingredient("soľ a čierne korenie", "podľa chuti"),
		),
		resourceCapacities = mapOf(
			COOK to 1,
			POT to 1,
			PAN to 1,
			BURNER to 2,
			SINK to 1,
		),
		tasks = listOf(
			CookingTask(
				id = "rinse",
				title = "Zlej a opláchni namočenú fazuľu",
				durationSeconds = 2 * 60,
				resources = uses(COOK, SINK),
				instruction = "Fazuľa má byť vopred namočená aspoň 6 hodín.",
			),
			CookingTask(
				id = "start-beans",
				title = "Daj fazuľu variť",
				durationSeconds = 2 * 60,
				dependsOn = setOf("rinse"),
				resources = uses(COOK, POT, BURNER),
				instruction = "Pridaj cca 800 ml čerstvej vody, bobkový list a 3 guľôčky nového korenia.",
			),
			CookingTask(
				id = "beans-simmer",
				title = "Fazuľa sa varí",
				durationSeconds = 45 * 60,
				dependsOn = setOf("start-beans"),
				resources = uses(POT, BURNER),
				kind = TaskKind.WAIT,
				instruction = "Var miernym varom, kým je fazuľa takmer mäkká.",
			),
			CookingTask(
				id = "prep-sausage",
				title = "Nakrájaj klobásu",
				durationSeconds = 2 * 60,
				dependsOn = setOf("start-beans"),
				resources = uses(COOK),
				skill = Skill.KNIFE,
			),
			CookingTask(
				id = "prep-carrot",
				title = "Očisti a nakrájaj mrkvu",
				durationSeconds = 3 * 60,
				dependsOn = setOf("start-beans"),
				resources = uses(COOK),
				skill = Skill.PEELING,
			),
			CookingTask(
				id = "prep-potato",
				title = "Ošúp a nakrájaj zemiak",
				durationSeconds = 4 * 60,
				dependsOn = setOf("start-beans"),
				resources = uses(COOK),
				skill = Skill.PEELING,
			),
			CookingTask(
				id = "prep-onion",
				title = "Očisti a nakrájaj cibuľu",
				durationSeconds = 3 * 60,
				dependsOn = setOf("start-beans"),
				resources = uses(COOK),
				skill = Skill.KNIFE,
			),
			CookingTask(
				id = "brown-sausage",
				title = "Opeč klobásu",
				durationSeconds = 4 * 60,
				dependsOn = setOf("prep-sausage"),
				resources = uses(COOK, PAN, BURNER),
				instruction = "Na masti alebo oleji opeč klobásu a odlož ju bokom.",
			),
			CookingTask(
				id = "saute-onion",
				title = "Orestuj cibuľu",
				durationSeconds = 4 * 60,
				dependsOn = setOf("prep-onion", "brown-sausage"),
				resources = uses(COOK, PAN, BURNER),
			),
			CookingTask(
				id = "prep-garlic",
				title = "Priprav cesnak",
				durationSeconds = 60,
				dependsOn = setOf("saute-onion"),
				resources = uses(COOK),
				skill = Skill.KNIFE,
			),
			CookingTask(
				id = "garlic-paprika",
				title = "Cesnak a paprika",
				durationSeconds = 40,
				dependsOn = setOf("prep-garlic"),
				resources = uses(COOK, PAN, BURNER),
				instruction = "Cesnak nechaj asi 20 sekúnd, prisyp papriku a hneď podlej trochou vody z fazule.",
			),
			CookingTask(
				id = "combine",
				title = "Spoj všetko v hrnci",
				durationSeconds = 2 * 60,
				dependsOn = setOf(
					"beans-simmer",
					"garlic-paprika",
					"prep-carrot",
					"prep-potato",
				),
				resources = uses(COOK, POT),
				instruction = "Pridaj obsah panvice, mrkvu, zemiak a opečenú klobásu.",
			),
			CookingTask(
				id = "finish-simmer",
				title = "Dovar polievku",
				durationSeconds = 18 * 60,
				dependsOn = setOf("combine"),
				resources = uses(POT, BURNER),
				kind = TaskKind.WAIT,
				instruction = "Var približne 15–20 minút, kým je zemiak mäkký.",
			),
			CookingTask(
				id = "season",
				title = "Dochuť a pridaj majorán",
				durationSeconds = 2 * 60,
				dependsOn = setOf("finish-simmer"),
				resources = uses(COOK, POT),
				instruction = "Osoľ, okoreň, vypni oheň a pridaj majorán.",
			),
		),
	)
}
