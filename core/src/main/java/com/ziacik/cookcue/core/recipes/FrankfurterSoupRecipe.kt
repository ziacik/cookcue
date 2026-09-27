package com.ziacik.cookcue.core.recipes

import com.ziacik.cookcue.core.model.CookingTask
import com.ziacik.cookcue.core.model.Ingredient
import com.ziacik.cookcue.core.model.Recipe
import com.ziacik.cookcue.core.model.ResourceRequirement
import com.ziacik.cookcue.core.model.Skill
import com.ziacik.cookcue.core.model.TaskKind
import com.ziacik.cookcue.core.model.TroubleshootingTip

object FrankfurterSoupRecipe {
	private const val COOK = "cook"
	private const val POT = "pot"
	private const val BURNER = "burner"

	private fun uses(vararg resources: String): Set<ResourceRequirement> {
		return resources.map { ResourceRequirement(it) }.toSet()
	}

	val recipe = Recipe(
		id = "frankfurter-soup",
		title = "Frankfurtská polievka",
		description = "Pre 2 osoby · zemiaky, frankfurtské párky a jemná smotanová zátrepka",
		servings = 2,
		ingredients = listOf(
			Ingredient("frankfurtské párky", "2 ks (cca 150 g)"),
			Ingredient("stredné zemiaky", "2 ks (cca 300 g)"),
			Ingredient("menšia cibuľa", "1 ks"),
			Ingredient("cesnak", "1 strúčik"),
			Ingredient("olej alebo masť", "1 PL"),
			Ingredient("sladká mletá paprika", "1 ČL"),
			Ingredient("hladká múka", "1 PL"),
			Ingredient("smotana na varenie", "100 ml"),
			Ingredient("zeleninový alebo hovädzí bujón", "1/2 kocky"),
			Ingredient("voda", "700 ml"),
			Ingredient("sušený majorán", "1/2 ČL"),
			Ingredient("drvená rasca", "1/4 ČL"),
			Ingredient("soľ", "podľa chuti"),
			Ingredient("mleté čierne korenie", "podľa chuti"),
		),
		resourceCapacities = mapOf(
			COOK to 1,
			POT to 1,
			BURNER to 1,
		),
		tasks = listOf(
			CookingTask(
				id = "prep-onion-garlic",
				title = "Nakrájaj cibuľu a priprav cesnak",
				durationSeconds = 6 * 60,
				resources = uses(COOK),
				skill = Skill.KNIFE,
				instruction = "Ošúp 1 menšiu cibuľu a nakrájaj ju nadrobno. Ošúp 1 strúčik cesnaku a nechaj ho pripravený pri lise alebo noži.",
			),
			CookingTask(
				id = "heat-fat",
				title = "Rozohrej tuk",
				durationSeconds = 60,
				dependsOn = setOf("prep-onion-garlic"),
				resources = uses(COOK, POT, BURNER),
				instruction = "Do hrnca daj 1 PL oleja alebo masti a rozohrej na strednom výkone.",
			),
			CookingTask(
				id = "saute-onion",
				title = "Orestuj cibuľu",
				durationSeconds = 4 * 60,
				dependsOn = setOf("heat-fat"),
				resources = uses(COOK, POT, BURNER),
				instruction = "Pridaj nakrájanú cibuľu a restuj ju na strednom výkone. Občas premiešaj. Hotová je, keď zmäkne a prestane voňať surovo.",
			),
			CookingTask(
				id = "garlic-paprika",
				title = "Pridaj cesnak, rascu a papriku",
				durationSeconds = 90,
				dependsOn = setOf("saute-onion"),
				resources = uses(COOK, POT, BURNER),
				instruction = "Pretlač cesnak priamo do hrnca a miešaj asi 20 sekúnd. Pridaj 1/4 ČL drvenej rasce a 1 ČL sladkej papriky. Premiešaj iba pár sekúnd a hneď pokračuj ďalším krokom.",
				tips = listOf(
					"Paprika sa ľahko spáli a zhorkne, preto ju nenechávaj na tuku samú.",
				),
			),
			CookingTask(
				id = "add-liquid",
				title = "Zalej vodou",
				durationSeconds = 3 * 60,
				dependsOn = setOf("garlic-paprika"),
				resources = uses(COOK, POT, BURNER),
				instruction = "Hneď nalej 700 ml vody, pridaj 1/2 kocky bujónu a premiešaj odo dna. Zvýš výkon, aby sa polievka začala zohrievať k varu.",
			),
			CookingTask(
				id = "wait-for-boil",
				title = "Priveď základ do varu",
				durationSeconds = 7 * 60,
				dependsOn = setOf("add-liquid"),
				resources = uses(POT, BURNER),
				kind = TaskKind.EVENT,
				instruction = "Keď začne voda normálne vrieť, potvrď to. Kým sa zohrieva, appka ťa môže poslať pripraviť zemiaky.",
				actionLabel = "VRIE",
			),
			CookingTask(
				id = "prep-potatoes",
				title = "Ošúp a nakrájaj zemiaky",
				durationSeconds = 8 * 60,
				dependsOn = setOf("add-liquid"),
				resources = uses(COOK),
				skill = Skill.PEELING,
				instruction = "Ošúp 2 stredné zemiaky a nakrájaj ich na kocky približne 1,5 cm. Nemusia byť dokonale rovnaké, ale vyhni sa veľkým kusom, ktoré by sa varili výrazne dlhšie.",
			),
			CookingTask(
				id = "add-potatoes",
				title = "Pridaj zemiaky",
				durationSeconds = 2 * 60,
				dependsOn = setOf("wait-for-boil", "prep-potatoes"),
				resources = uses(COOK, POT, BURNER),
				instruction = "Do vriaceho základu pridaj nakrájané zemiaky. Premiešaj, po opätovnom rozbehnutí varu stiahni výkon na mierny var.",
			),
			CookingTask(
				id = "potatoes-minimum-cook",
				title = "Var zemiaky aspoň 12 minút",
				durationSeconds = 12 * 60,
				dependsOn = setOf("add-potatoes"),
				resources = uses(POT, BURNER),
				kind = TaskKind.WAIT,
				instruction = "Zemiaky nechaj mierne vrieť. Počas tohto času pripravíš párky a zátrepku.",
			),
			CookingTask(
				id = "prep-sausages",
				title = "Nakrájaj párky",
				durationSeconds = 4 * 60,
				dependsOn = setOf("add-potatoes"),
				resources = uses(COOK),
				skill = Skill.KNIFE,
				instruction = "2 frankfurtské párky nakrájaj na kolieska hrubé asi 5–8 mm.",
			),
			CookingTask(
				id = "mix-slurry",
				title = "Priprav smotanovú zátrepku",
				durationSeconds = 5 * 60,
				dependsOn = setOf("add-potatoes"),
				resources = uses(COOK),
				instruction = "Do misky nalej 100 ml studenej smotany na varenie. Prisyp 1 PL hladkej múky a metličkou alebo vidličkou rozmiešaj úplne dohladka, bez hrudiek.",
				tips = listOf(
					"Múku pridávaj do studenej smotany, nie priamo do horúcej polievky.",
				),
			),
			CookingTask(
				id = "potatoes-ready",
				title = "Skontroluj zemiaky",
				durationSeconds = 3 * 60,
				dependsOn = setOf("potatoes-minimum-cook"),
				resources = uses(POT, BURNER),
				kind = TaskKind.EVENT,
				instruction = "Pichni vidličkou do väčšej kocky zemiaka. Ide dnu ľahko a zemiak už nemá tvrdý stred?",
				actionLabel = "ÁNO",
				retryActionLabel = "NIE",
				retryAfterSeconds = 3 * 60,
			),
			CookingTask(
				id = "add-sausages",
				title = "Pridaj párky",
				durationSeconds = 2 * 60,
				dependsOn = setOf("potatoes-ready", "prep-sausages"),
				resources = uses(COOK, POT, BURNER),
				instruction = "Pridaj nakrájané párky a premiešaj. Nechaj polievku ďalej mierne vrieť.",
			),
			CookingTask(
				id = "heat-sausages",
				title = "Prehrej párky",
				durationSeconds = 3 * 60,
				dependsOn = setOf("add-sausages"),
				resources = uses(POT, BURNER),
				kind = TaskKind.WAIT,
				instruction = "Párky nechaj v polievke 3 minúty prehriať. Netreba ich variť dlho.",
			),
			CookingTask(
				id = "add-slurry",
				title = "Zahusti polievku",
				durationSeconds = 5 * 60,
				dependsOn = setOf("heat-sausages", "mix-slurry"),
				resources = uses(COOK, POT, BURNER),
				instruction = "Do misky so zátrepkou pridaj za stáleho miešania 1 naberačku horúcej polievky. Potom zátrepku pomaly lej do hrnca a stále miešaj. Dobre premiešaj aj pri dne.",
				tips = listOf(
					"Temperovanie horúcou polievkou zníži riziko hrudiek a prudkého zrazenia smotany.",
				),
			),
			CookingTask(
				id = "simmer-thickened",
				title = "Nechaj múku prevariť",
				durationSeconds = 4 * 60,
				dependsOn = setOf("add-slurry"),
				resources = uses(POT, BURNER),
				kind = TaskKind.WAIT,
				instruction = "Nechaj polievku 4 minúty veľmi mierne prebublávať. Občas premiešaj, aby sa nič neprichytilo ku dnu.",
			),
			CookingTask(
				id = "season",
				title = "Dochuť a pridaj majorán",
				durationSeconds = 4 * 60,
				dependsOn = setOf("simmer-thickened"),
				resources = uses(COOK, POT),
				instruction = "Vypni sporák. Ochutnaj a podľa potreby pridaj soľ a mleté čierne korenie. 1/2 ČL majoránu rozdrv medzi prstami alebo dlaňami, pridaj do polievky a premiešaj.",
			),
			CookingTask(
				id = "rest",
				title = "Nechaj polievku postáť",
				durationSeconds = 3 * 60,
				dependsOn = setOf("season"),
				resources = uses(POT),
				kind = TaskKind.WAIT,
				instruction = "Nechaj polievku 3 minúty postáť, aby sa chute spojili a hustota ustálila.",
			),
			CookingTask(
				id = "final-taste",
				title = "Ešte raz ochutnaj a podávaj",
				durationSeconds = 2 * 60,
				dependsOn = setOf("rest"),
				resources = uses(COOK, POT),
				instruction = "Ešte raz ochutnaj. Ak sedí soľ, korenie aj hustota, môžeš podávať. Hodí sa k nej chlieb alebo rožok.",
			),
		),
		troubleshooting = listOf(
			TroubleshootingTip(
				problem = "Zemiaky sú stále tvrdé",
				advice = "Var ďalej po 3 minútach a znova skús väčšiu kocku vidličkou. Zátrepku pridaj až po zmäknutí zemiakov.",
			),
			TroubleshootingTip(
				problem = "V zátrepke sú hrudky",
				advice = "Pred pridaním do hrnca ju dôkladne rozmiešaj metličkou alebo prelej cez sitko.",
			),
			TroubleshootingTip(
				problem = "Polievka je príliš hustá",
				advice = "Po troške dolej horúcu vodu, premiešaj a po každom doliatí znova skontroluj chuť.",
			),
			TroubleshootingTip(
				problem = "Polievka je príliš riedka",
				advice = "Nechaj ju ešte pár minút mierne vrieť bez pokrievky. Po odstavení ešte trochu zhustne.",
			),
			TroubleshootingTip(
				problem = "Polievka je príliš slaná",
				advice = "Zrieď ju trochou horúcej vody alebo smotany. Ďalší bujón už nepridávaj.",
			),
			TroubleshootingTip(
				problem = "Paprika zhorkla",
				advice = "Spálenú papriku sa spoľahlivo opraviť nedá. Nabudúce ju na tuku miešaj iba pár sekúnd a hneď zalej vodou.",
			),
		),
	)
}
