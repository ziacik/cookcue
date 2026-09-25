package com.ziacik.cookcue.core.recipes

import com.ziacik.cookcue.core.model.CookingTask
import com.ziacik.cookcue.core.model.Ingredient
import com.ziacik.cookcue.core.model.Recipe
import com.ziacik.cookcue.core.model.ResourceRequirement
import com.ziacik.cookcue.core.model.Skill
import com.ziacik.cookcue.core.model.TaskKind
import com.ziacik.cookcue.core.model.TroubleshootingTip

object ParboiledFriesRecipe {
	private const val COOK = "cook"
	private const val POT = "pot"
	private const val BURNER = "burner"
	private const val SINK = "sink"

	private fun uses(vararg resources: String): Set<ResourceRequirement> {
		return resources.map { ResourceRequirement(it) }.toSet()
	}

	val recipe = Recipe(
		id = "parboiled-fries",
		title = "Hranolky s predvarením",
		description = "Pre 1 osobu · predvarené a dvakrát vyprážané, chrumkavé zvonka a mäkké vnútri",
		servings = 1,
		ingredients = listOf(
			Ingredient("zemiaky typu C", "300 g"),
			Ingredient("ocot 5 %", "1 ČL na 1 l vody"),
			Ingredient("olej na vyprážanie", "cca 700–900 ml podľa hrnca"),
			Ingredient("soľ", "podľa chuti"),
		),
		resourceCapacities = mapOf(
			COOK to 1,
			POT to 1,
			BURNER to 1,
			SINK to 1,
		),
		preCookingNote = "Najlepšie fungujú múčnaté zemiaky typu C. Hranolky krájaj približne na 8–10 mm, aby sa predvarili aj vypražili rovnomerne.",
		tasks = listOf(
			CookingTask(
				id = "prep-potatoes",
				title = "Ošúp a nakrájaj zemiaky",
				durationSeconds = 12 * 60,
				resources = uses(COOK, SINK),
				skill = Skill.PEELING,
				instruction = "Ošúp 300 g zemiakov a nakrájaj ich na hranolky hrubé približne 8–10 mm. Snaž sa, aby boli podobne hrubé; veľmi tenké kúsky by sa pri predvarení rozpadávali.",
			),
			CookingTask(
				id = "rinse-potatoes",
				title = "Opláchni hranolky",
				durationSeconds = 3 * 60,
				dependsOn = setOf("prep-potatoes"),
				resources = uses(COOK, SINK),
				instruction = "Hranolky dôkladne opláchni v studenej vode, aby sa z povrchu zmyla voľná vrstva škrobu. Nechaj ich odkvapkať.",
			),
			CookingTask(
				id = "start-water",
				title = "Daj variť vodu s octom",
				durationSeconds = 3 * 60,
				dependsOn = setOf("rinse-potatoes"),
				resources = uses(COOK, POT, BURNER),
				instruction = "Do hrnca daj dosť vody, aby boli hranolky neskôr ponorené. Na každý 1 l vody pridaj 1 ČL 5 % octu a približne 1/2 ČL soli. Zapni vysoký výkon.",
			),
			CookingTask(
				id = "water-boiling",
				title = "Počkaj, kým voda vrie",
				durationSeconds = 5 * 60,
				dependsOn = setOf("start-water"),
				resources = uses(POT, BURNER),
				kind = TaskKind.EVENT,
				instruction = "Keď voda výrazne vrie, potvrď to. Hrniec nechaj odkrytý a výkon zatiaľ vysoký.",
				actionLabel = "VODA VRIE",
			),
			CookingTask(
				id = "add-potatoes",
				title = "Pridaj hranolky",
				durationSeconds = 60,
				dependsOn = setOf("water-boiling"),
				resources = uses(COOK, POT, BURNER),
				instruction = "Opatrne vlož hranolky do vriacej vody. Premiešaj ich len jemne, aby sa nezlepili. Po pridaní voda na chvíľu prestane vrieť.",
			),
			CookingTask(
				id = "water-reboiling",
				title = "Počkaj na opätovný var",
				durationSeconds = 2 * 60,
				dependsOn = setOf("add-potatoes"),
				resources = uses(POT, BURNER),
				kind = TaskKind.EVENT,
				instruction = "Počkaj, kým voda s hranolkami znovu začne poriadne vrieť. Až potom začne predvárací čas.",
				actionLabel = "ZNOVU VRIE",
			),
			CookingTask(
				id = "parboil",
				title = "Predvar hranolky 4 minúty",
				durationSeconds = 4 * 60,
				dependsOn = setOf("water-reboiling"),
				resources = uses(POT, BURNER),
				kind = TaskKind.WAIT,
				instruction = "Var ich odkryté približne 4 minúty. Povrch má zmäknúť, ale hranolky sa nesmú rozpadávať.",
			),
			CookingTask(
				id = "parboil-check",
				title = "Skontroluj predvarenie",
				durationSeconds = 60,
				dependsOn = setOf("parboil"),
				resources = uses(POT, BURNER),
				kind = TaskKind.EVENT,
				instruction = "Vyber jeden hranolček. Povrch má byť mäkký a trochu krehký, ale stred ešte pevný a hranolček musí držať tvar. Sedí to?",
				actionLabel = "ÁNO",
				retryActionLabel = "EŠTE NIE",
				retryAfterSeconds = 60,
			),
			CookingTask(
				id = "drain-dry",
				title = "Zlej a poriadne vysuš",
				durationSeconds = 5 * 60,
				dependsOn = setOf("parboil-check"),
				resources = uses(COOK, POT, SINK),
				instruction = "Hranolky opatrne zlej, rozlož ich v jednej vrstve na čistú utierku alebo papierové utierky a nechaj odpariť paru. Jemne ich dosuš. Pred olejom musia byť na povrchu čo najsuchšie. Hrniec tiež úplne vysuš.",
				tips = listOf(
					"Voda a horúci olej sú nebezpečná kombinácia. Mokré hranolky do oleja nedávaj.",
				),
			),
			CookingTask(
				id = "air-dry",
				title = "Nechaj hranolky preschnúť",
				durationSeconds = 10 * 60,
				dependsOn = setOf("drain-dry"),
				kind = TaskKind.WAIT,
				instruction = "Nechaj hranolky rozložené približne 10 minút. Povrch má byť suchý a matný, nie mokrý.",
			),
			CookingTask(
				id = "heat-oil-low",
				title = "Rozohrej olej na 150–160 °C",
				durationSeconds = 4 * 60,
				dependsOn = setOf("air-dry"),
				resources = uses(COOK, POT, BURNER),
				kind = TaskKind.EVENT,
				instruction = "Do úplne suchého hrnca nalej olej tak, aby mali hranolky priestor plávať. Rozohrej ho na 150–160 °C. S teplomerom sa riaď teplotou; bez teplomera skús jeden hranolček — má okolo neho stabilne bublať, ale nesmie rýchlo hnednúť.",
				actionLabel = "150–160 °C",
			),
			CookingTask(
				id = "first-fry",
				title = "Prvé vyprážanie",
				durationSeconds = 6 * 60,
				dependsOn = setOf("heat-oil-low"),
				resources = uses(COOK, POT, BURNER),
				instruction = "Opatrne vlož hranolky do oleja a vyprážaj približne 5–6 minút. Majú zmäknúť a zostať bledé alebo len veľmi jemne sfarbené. Ak sa v hrnci tlačia na sebe, vyprážaj ich radšej na dve dávky.",
			),
			CookingTask(
				id = "drain-first-fry",
				title = "Vyber a nechaj odkvapkať",
				durationSeconds = 2 * 60,
				dependsOn = setOf("first-fry"),
				resources = uses(COOK, POT, BURNER),
				instruction = "Hranolky vyber dierovanou lyžicou na mriežku alebo papierové utierky. Sporák vypni alebo olej odstav z horúcej platne.",
			),
			CookingTask(
				id = "cool-fries",
				title = "Nechaj hranolky vychladnúť",
				durationSeconds = 15 * 60,
				dependsOn = setOf("drain-first-fry"),
				kind = TaskKind.WAIT,
				instruction = "Nechaj predvyprážané hranolky aspoň 15 minút odpočívať. Povrch pri chladnutí spevnie a druhé vyprážanie ich potom spraví chrumkavejšie.",
			),
			CookingTask(
				id = "heat-oil-high",
				title = "Rozohrej olej na 185–190 °C",
				durationSeconds = 4 * 60,
				dependsOn = setOf("cool-fries"),
				resources = uses(COOK, POT, BURNER),
				kind = TaskKind.EVENT,
				instruction = "Znovu rozohrej olej, tentoraz na 185–190 °C. Bez teplomera má skúšobný hranolček po vložení hneď intenzívne bublať a postupne zlatnúť, nie sa okamžite páliť.",
				actionLabel = "185–190 °C",
			),
			CookingTask(
				id = "second-fry",
				title = "Druhé vyprážanie",
				durationSeconds = 3 * 60,
				dependsOn = setOf("heat-oil-high"),
				resources = uses(COOK, POT, BURNER),
				instruction = "Vlož hranolky späť do horúceho oleja a vyprážaj približne 2–3 minúty, kým sú výrazne zlaté a chrumkavé. Sleduj farbu — v tejto fáze sa mení rýchlo.",
			),
			CookingTask(
				id = "salt-serve",
				title = "Osoľ a podávaj",
				durationSeconds = 2 * 60,
				dependsOn = setOf("second-fry"),
				resources = uses(COOK),
				instruction = "Hranolky vyber, nechaj krátko odkvapkať a ešte horúce ich osoľ. Hneď podávaj.",
			),
		),
		troubleshooting = listOf(
			TroubleshootingTip(
				problem = "Hranolky sa pri predvarení rozpadajú",
				advice = "Hneď ich zlej. Nabudúce ich var kratšie alebo nakrájaj o trochu hrubšie. Ocot vo vode pomáha povrchu držať tvar.",
			),
			TroubleshootingTip(
				problem = "Po prvom vyprážaní už hnednú",
				advice = "Olej je príliš horúci. Prvá fáza má hranolky hlavne uvariť vnútri, nie zafarbiť. Stiahni teplotu bližšie k 150 °C.",
			),
			TroubleshootingTip(
				problem = "Hranolky sú mastné a mäkké",
				advice = "Najčastejšie bol olej príliš studený alebo bolo v hrnci priveľa hranoliek naraz. Pri druhej fáze drž približne 185–190 °C a nepreplň hrniec.",
			),
			TroubleshootingTip(
				problem = "Hranolky tmavnú príliš rýchlo",
				advice = "Olej je príliš horúci. Hranolky vyber a nechaj olej trochu vychladnúť, potom pokračuj pri nižšej teplote.",
			),
		),
	)
}
