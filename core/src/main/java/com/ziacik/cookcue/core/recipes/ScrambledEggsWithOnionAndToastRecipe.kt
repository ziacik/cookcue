package com.ziacik.cookcue.core.recipes

import com.ziacik.cookcue.core.model.CookingTask
import com.ziacik.cookcue.core.model.Ingredient
import com.ziacik.cookcue.core.model.Recipe
import com.ziacik.cookcue.core.model.ResourceRequirement
import com.ziacik.cookcue.core.model.Skill
import com.ziacik.cookcue.core.model.TaskKind
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
		description = "Pre 1 osobu · praženica s toastami opečenými v toastovači a natretými maslom",
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
			COOK to 1,
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
				id = "heat-fat",
				title = "Rozohrej panvicu",
				durationSeconds = 60,
				dependsOn = setOf("prep-onion"),
				resources = uses(COOK, PAN, BURNER),
				instruction = "Daj panvicu na stredný výkon a pridaj 1 ČL masla alebo oleja. Nechaj tuk zohriať, ale maslo nenechaj zhnednúť.",
			),
			CookingTask(
				id = "saute-onion-first",
				title = "Začni restovať cibuľu",
				durationSeconds = 2 * 60,
				dependsOn = setOf("heat-fat"),
				resources = uses(COOK, PAN, BURNER),
				instruction = "Pridaj nakrájanú cibuľu a restuj ju 2 minúty na strednom výkone. Občas premiešaj.",
			),
			CookingTask(
				id = "start-toast",
				title = "Daj toasty do toastovača",
				durationSeconds = 60,
				dependsOn = setOf("saute-onion-first"),
				resources = uses(COOK, TOASTER),
				instruction = "Vlož 2 krajce toastového chleba do toastovača a spusti opekanie na bežný stupeň.",
			),
			CookingTask(
				id = "toast",
				title = "Toasty sa opekajú",
				durationSeconds = 2 * 60,
				dependsOn = setOf("start-toast"),
				resources = uses(TOASTER),
				kind = TaskKind.WAIT,
				instruction = "Nechaj toasty opiecť v toastovači.",
			),
			CookingTask(
				id = "saute-onion-finish",
				title = "Dorestuj cibuľu",
				durationSeconds = 2 * 60,
				dependsOn = setOf("start-toast"),
				resources = uses(COOK, PAN, BURNER),
				instruction = "Pokračuj v restovaní cibule ďalšie približne 2 minúty. Občas premiešaj. Keď je mäkká a prestane voňať surovo, odstav panvicu zo sporáka, aby sa cibuľa počas natierania toastov nespálila.",
				tips = listOf(
					"Ak začne rýchlo hnednúť, stiahni výkon.",
				),
			),
			CookingTask(
				id = "butter-toast",
				title = "Natri toasty maslom",
				durationSeconds = 3 * 60,
				dependsOn = setOf("toast", "saute-onion-finish"),
				resources = uses(COOK),
				instruction = "Vyber opečené toasty a ešte teplé ich natri maslom.",
			),
			CookingTask(
				id = "add-eggs",
				title = "Pridaj vajcia a miešaj",
				durationSeconds = 2 * 60,
				dependsOn = setOf("butter-toast"),
				resources = uses(COOK, PAN, BURNER),
				instruction = "Vráť panvicu na stredný výkon. Rozbi 2 vajcia priamo do panvice k cibuli, hneď ich osol malou štipkou soli a začni miešať. Stiahni na stredne nízky výkon. Keď už vajcia nie sú tekuté, ale stále sú mäkké a trochu lesklé, daj panvicu zo sporáka a až vtedy potvrď HOTOVO.",
				tips = listOf(
					"Nečakaj, kým bude praženica na panvici úplne suchá. Vajcia ešte chvíľu dôjdu zvyškovým teplom.",
				),
			),
			CookingTask(
				id = "finish",
				title = "Dochuť a podávaj",
				durationSeconds = 2 * 60,
				dependsOn = setOf("add-eggs"),
				resources = uses(COOK, PAN),
				instruction = "Ochutnaj. Ak treba, už len jemne dosoľ a pridaj čierne korenie. Hneď podávaj s maslovými toastami.",
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
			TroubleshootingTip(
				problem = "Toasty sú príliš tmavé",
				advice = "Vyber ich hneď a nabudúce nastav toastovač o stupeň nižšie.",
			),
		),
	)
}
