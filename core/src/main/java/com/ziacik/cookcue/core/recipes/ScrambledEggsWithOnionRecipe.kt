package com.ziacik.cookcue.core.recipes

import com.ziacik.cookcue.core.model.CookingTask
import com.ziacik.cookcue.core.model.Ingredient
import com.ziacik.cookcue.core.model.Recipe
import com.ziacik.cookcue.core.model.ResourceRequirement
import com.ziacik.cookcue.core.model.Skill
import com.ziacik.cookcue.core.model.TroubleshootingTip

object ScrambledEggsWithOnionRecipe {
	private const val COOK = "cook"
	private const val PAN = "pan"
	private const val BURNER = "burner"

	private fun uses(vararg resources: String): Set<ResourceRequirement> {
		return resources.map { ResourceRequirement(it) }.toSet()
	}

	val recipe = Recipe(
		id = "scrambled-eggs-onion",
		title = "Praženica s cibuľou",
		description = "Pre 1 osobu · jednoduchá klasická praženica",
		servings = 1,
		ingredients = listOf(
			Ingredient("vajcia", "2 ks"),
			Ingredient("malá cibuľa", "1/2 ks"),
			Ingredient("maslo alebo olej", "1 ČL"),
			Ingredient("soľ", "podľa chuti"),
			Ingredient("čierne korenie", "podľa chuti"),
		),
		resourceCapacities = mapOf(
			COOK to 1,
			PAN to 1,
			BURNER to 1,
		),
		tasks = listOf(
			CookingTask(
				id = "prep-onion",
				title = "Nakrájaj cibuľu",
				durationSeconds = 4 * 60,
				resources = uses(COOK),
				skill = Skill.KNIFE,
				instruction = "Ošúp 1/2 malej cibule a nakrájaj ju nadrobno.",
			),
			CookingTask(
				id = "heat-fat",
				title = "Rozohrej panvicu",
				durationSeconds = 60,
				dependsOn = setOf("prep-onion"),
				resources = uses(COOK, PAN, BURNER),
				instruction = "Daj panvicu na stredný výkon a pridaj 1 ČL masla alebo oleja. Nechaj tuk zohriať, ale maslo nenechaj zhnednúť.",
			),
			CookingTask(
				id = "saute-onion",
				title = "Orestuj cibuľu",
				durationSeconds = 4 * 60,
				dependsOn = setOf("heat-fat"),
				resources = uses(COOK, PAN, BURNER),
				instruction = "Pridaj nakrájanú cibuľu a restuj ju na strednom výkone. Občas premiešaj. Hotová je, keď zmäkne a prestane voňať surovo.",
				tips = listOf(
					"Ak začne rýchlo hnednúť, stiahni výkon.",
				),
			),
			CookingTask(
				id = "add-eggs",
				title = "Pridaj vajcia",
				durationSeconds = 2 * 60,
				dependsOn = setOf("saute-onion"),
				resources = uses(COOK, PAN, BURNER),
				instruction = "Rozbi 2 vajcia priamo do panvice k cibuli. Hneď ich osol malou štipkou soli a vareškou alebo stierkou rozmiešaj bielky so žĺtkami, aby sa soľ rovnomerne rozložila.",
			),
			CookingTask(
				id = "scramble",
				title = "Miešaj praženicu",
				durationSeconds = 2 * 60,
				dependsOn = setOf("add-eggs"),
				resources = uses(COOK, PAN, BURNER),
				instruction = "Stiahni na stredne nízky výkon a pomaly miešaj. Keď už vajcia nie sú tekuté, ale stále sú mäkké a trochu lesklé, daj panvicu zo sporáka a potvrď HOTOVO.",
				tips = listOf(
					"Nečakaj, kým bude na panvici úplne suchá. Vajcia ešte chvíľu dôjdu zvyškovým teplom.",
				),
			),
			CookingTask(
				id = "finish",
				title = "Dochuť a podávaj",
				durationSeconds = 2 * 60,
				dependsOn = setOf("scramble"),
				resources = uses(COOK, PAN),
				instruction = "Ochutnaj. Ak treba, už len jemne dosoľ a pridaj čierne korenie. Hneď podávaj.",
			),
		),
		troubleshooting = listOf(
			TroubleshootingTip(
				problem = "Praženica je príliš suchá",
				advice = "Hneď ju daj z panvice na tanier. Nabudúce ju odstav o trochu skôr; na horúcej panvici ďalej tuhne.",
			),
			TroubleshootingTip(
				problem = "Praženica je ešte príliš tekutá",
				advice = "Vráť ju na nízky výkon a miešaj ešte 20–30 sekúnd.",
			),
			TroubleshootingTip(
				problem = "Cibuľa sa pripaľuje",
				advice = "Stiahni výkon a pridaj malú kvapku oleja alebo kúsok masla.",
			),
		),
	)
}
