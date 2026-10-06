package com.ziacik.cookcue.core.recipes

import com.ziacik.cookcue.core.model.CookingTask
import com.ziacik.cookcue.core.model.Ingredient
import com.ziacik.cookcue.core.model.Recipe
import com.ziacik.cookcue.core.model.ResourceRequirement
import com.ziacik.cookcue.core.model.Skill
import com.ziacik.cookcue.core.model.TroubleshootingTip

object ScrambledEggsWithOnionAndToastRecipe {
	private const val COOK = "cook"
	private const val PAN = "pan"
	private const val BURNER = "burner"
	private const val TOASTER = "toaster"

	private fun uses(vararg resources: String): Set<ResourceRequirement> {
		return resources.map { ResourceRequirement(it) }.toSet()
	}

	val recipe = Recipe(
		id = "scrambled-eggs-onion-toast",
		title = "Praženica s cibuľou + maslové toasty",
		description = "Pre 1 osobu · praženica a toasty ako dve skutočne paralelné vetvy",
		servings = 1,
		ingredients = listOf(
			Ingredient("vajcia", "2 ks"),
			Ingredient("malá cibuľa", "1/2 ks"),
			Ingredient("maslo alebo olej na panvicu", "1 ČL"),
			Ingredient("toastový chlieb", "2 krajce"),
			Ingredient("maslo na toasty", "podľa chuti"),
			Ingredient("soľ", "podľa chuti"),
			Ingredient("čierne korenie", "podľa chuti"),
		),
		resourceCapacities = mapOf(
			COOK to 2,
			PAN to 1,
			BURNER to 1,
			TOASTER to 1,
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
				id = "cook-onion",
				title = "Rozohrej panvicu a orestuj cibuľu",
				durationSeconds = 5 * 60,
				dependsOn = setOf("prep-onion"),
				resources = uses(COOK, PAN, BURNER),
				instruction = "Rozohrej tuk, pridaj cibuľu a restuj ju, kým zmäkne. Občas premiešaj. HOTOVO daj až keď je pripravená na vajcia.",
			),
			CookingTask(
				id = "make-toasts",
				title = "Opeč a natri toasty",
				durationSeconds = 4 * 60,
				dependsOn = setOf("prep-onion"),
				resources = uses(COOK, TOASTER),
				instruction = "Daj 2 krajce do toastovača. Keď vyskočia, natri ich maslom. HOTOVO daj až keď sú oba natreté.",
			),
			CookingTask(
				id = "cook-eggs",
				title = "Pridaj vajcia a miešaj",
				durationSeconds = 2 * 60,
				dependsOn = setOf("cook-onion"),
				resources = uses(COOK, PAN, BURNER),
				instruction = "Pridaj 2 vajcia, hneď ich osol a miešaj na stredne nízkom výkone. Keď už nie sú tekuté, ale stále sú mäkké a lesklé, daj panvicu zo sporáka a potvrď HOTOVO.",
				tips = listOf(
					"Ak toasty ešte nie sú hotové, vajcia kvôli nim nenechávaj na panvici dlhšie.",
				),
			),
			CookingTask(
				id = "finish",
				title = "Dochuť a podávaj",
				durationSeconds = 90,
				dependsOn = setOf("cook-eggs", "make-toasts"),
				resources = uses(COOK, PAN),
				instruction = "Keď sú hotové praženica aj toasty, dochuť praženicu a hneď podávaj.",
			),
		),
		troubleshooting = listOf(
			TroubleshootingTip("Praženica je príliš suchá", "Hneď ju daj z panvice na tanier. Nabudúce ju odstav skôr."),
			TroubleshootingTip("Praženica je ešte príliš tekutá", "Vráť ju na nízky výkon a miešaj ešte 20–30 sekúnd."),
			TroubleshootingTip("Cibuľa sa pripaľuje", "Stiahni výkon a pridaj malú kvapku oleja alebo kúsok masla."),
			TroubleshootingTip("Toasty sú príliš tmavé", "Nabudúce nastav toastovač o stupeň nižšie."),
		),
	)
}
