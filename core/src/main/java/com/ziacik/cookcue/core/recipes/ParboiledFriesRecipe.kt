package com.ziacik.cookcue.core.recipes

import com.ziacik.cookcue.core.model.CookingTask
import com.ziacik.cookcue.core.model.Ingredient
import com.ziacik.cookcue.core.model.Recipe
import com.ziacik.cookcue.core.model.ResourceRequirement
import com.ziacik.cookcue.core.model.Skill
import com.ziacik.cookcue.core.model.TaskKind
import com.ziacik.cookcue.core.model.TroubleshootingTip

data class FryerTimingProfile(
	val coldTo155Seconds: Long = 5 * 60,
	val from155To180Seconds: Long = 5 * 60,
) {
	init {
		require(coldTo155Seconds > 0)
		require(from155To180Seconds > 0)
	}
}

object ParboiledFriesRecipe {
	private const val COOK = "cook"
	private const val POT = "pot"
	private const val BURNER = "burner"
	private const val SINK = "sink"
	private const val FRYER = "fryer"

	private fun uses(vararg resources: String): Set<ResourceRequirement> {
		return resources.map { ResourceRequirement(it) }.toSet()
	}

	val recipe: Recipe = create()

	fun create(fryerTiming: FryerTimingProfile = FryerTimingProfile()): Recipe {
		return Recipe(
			id = "parboiled-fries",
			title = "Hranolky s predvarením",
			description = "Pre 1 osobu · predvarené a dvakrát vyprážané vo fritéze",
			servings = 1,
			ingredients = listOf(
				Ingredient("zemiaky typu C", "300 g"),
				Ingredient("ocot 5 %", "1 ČL na 1 l vody"),
				Ingredient("olej na vyprážanie", "podľa fritézy"),
				Ingredient("soľ", "podľa chuti"),
			),
			resourceCapacities = mapOf(
				COOK to 1,
				POT to 1,
				BURNER to 1,
				SINK to 1,
				FRYER to 1,
			),
			preCookingNote = "Najlepšie fungujú múčnaté zemiaky typu C. Krájaj ich približne na 8–10 mm. Recept počíta s klasickou olejovou fritézou.",
			tasks = listOf(
				CookingTask(
					id = "prep-potatoes",
					title = "Ošúp a nakrájaj zemiaky",
					durationSeconds = 12 * 60,
					resources = uses(COOK, SINK),
					skill = Skill.PEELING,
					instruction = "Ošúp 300 g zemiakov a nakrájaj ich na hranolky hrubé približne 8–10 mm. Snaž sa, aby boli podobne hrubé.",
				),
				CookingTask(
					id = "start-water",
					title = "Daj variť vodu s octom",
					durationSeconds = 2 * 60,
					dependsOn = setOf("prep-potatoes"),
					resources = uses(COOK, POT, BURNER),
					instruction = "Do hrnca daj dosť vody, aby boli hranolky ponorené. Na každý 1 l vody pridaj 1 ČL 5 % octu a približne 1/2 ČL soli. Zapni vysoký výkon.",
				),
				CookingTask(
					id = "water-boiling",
					title = "Voda sa zohrieva",
					durationSeconds = 5 * 60,
					dependsOn = setOf("start-water"),
					resources = uses(POT, BURNER),
					kind = TaskKind.EVENT,
					instruction = "Voda sa zohrieva. Medzitým pokračuj s hranolkami. Keď začne výrazne vrieť, potvrď to.",
					actionLabel = "VODA VRIE",
				),
				CookingTask(
					id = "rinse-potatoes",
					title = "Opláchni hranolky",
					durationSeconds = 3 * 60,
					dependsOn = setOf("start-water"),
					resources = uses(COOK, SINK),
					instruction = "Kým sa voda zohrieva, hranolky dôkladne opláchni v studenej vode, aby sa z povrchu zmyla voľná vrstva škrobu. Nechaj ich odkvapkať.",
				),
				CookingTask(
					id = "add-potatoes",
					title = "Pridaj hranolky",
					durationSeconds = 60,
					dependsOn = setOf("water-boiling", "rinse-potatoes"),
					resources = uses(COOK, POT, BURNER),
					instruction = "Opatrne vlož opláchnuté hranolky do vriacej vody. Jemne premiešaj. Po pridaní voda na chvíľu prestane vrieť.",
				),
				CookingTask(
					id = "water-reboiling",
					title = "Počkaj na opätovný var",
					durationSeconds = 2 * 60,
					dependsOn = setOf("add-potatoes"),
					resources = uses(POT, BURNER),
					kind = TaskKind.EVENT,
					instruction = "Keď voda s hranolkami znovu začne poriadne vrieť, potvrď to. Až od tohto momentu počítame predvarenie.",
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
					instruction = "Vyber jeden hranolček. Povrch má byť mäkký a trochu krehký, stred ešte pevný a hranolček musí držať tvar. Sedí to?",
					actionLabel = "ÁNO",
					retryActionLabel = "EŠTE NIE",
					retryAfterSeconds = 60,
				),
				CookingTask(
					id = "drain-spread",
					title = "Zlej a rozlož hranolky",
					durationSeconds = 60,
					dependsOn = setOf("parboil-check"),
					resources = uses(COOK, POT, SINK),
					instruction = "Hranolky opatrne zlej a rozlož ich v jednej vrstve na čistú utierku alebo papierové utierky. Jemne odsaj viditeľnú vodu.",
					tips = listOf(
						"Pred vložením do oleja musia byť na povrchu suché.",
					),
				),
				CookingTask(
					id = "dry-fries",
					title = "Nechaj hranolky schnúť",
					durationSeconds = 9 * 60,
					dependsOn = setOf("drain-spread"),
					kind = TaskKind.WAIT,
					instruction = "Nechaj hranolky rozložené schnúť. Spolu so zlievaním a rozkladaním počítame približne 10 minút od vytiahnutia z vody.",
				),
				CookingTask(
					id = "heat-fryer-155",
					title = "Nahrej fritézu na 155 °C",
					durationSeconds = fryerTiming.coldTo155Seconds,
					dependsOn = setOf("drain-spread"),
					resources = uses(FRYER),
					kind = TaskKind.WAIT,
					instruction = "Počas schnutia zapni fritézu a nastav 155 °C. Recept počíta s časom nahrievania ${fryerTiming.coldTo155Seconds / 60} min.",
				),
				CookingTask(
					id = "load-first-basket",
					title = "Naplň kôš fritézy",
					durationSeconds = 60,
					dependsOn = setOf("dry-fries", "heat-fryer-155"),
					resources = uses(COOK, FRYER),
					instruction = "Suché hranolky vlož do koša fritézy. Nepreplň ho; hranolky potrebujú priestor. Keď je kôš naplnený a pripravený, spusti prvé smaženie.",
				),
				CookingTask(
					id = "first-fry",
					title = "Prvé smaženie – 155 °C",
					durationSeconds = 6 * 60,
					dependsOn = setOf("load-first-basket"),
					resources = uses(FRYER),
					kind = TaskKind.WAIT,
					instruction = "Spusť kôš do oleja a smaž 6 minút pri 155 °C. Čas sa počíta až od momentu, keď je naplnený kôš vo fritéze.",
				),
				CookingTask(
					id = "lift-first-basket",
					title = "Zdvihni kôš",
					durationSeconds = 60,
					dependsOn = setOf("first-fry"),
					resources = uses(COOK, FRYER),
					instruction = "Zdvihni kôš nad olej a nechaj hranolky krátko odkvapkať. Zatiaľ ich nechaj v koši.",
				),
				CookingTask(
					id = "cool-fries",
					title = "Nechaj hranolky 5 minút odpočívať",
					durationSeconds = 5 * 60,
					dependsOn = setOf("lift-first-basket"),
					kind = TaskKind.WAIT,
					instruction = "Hranolky nechaj približne 5 minút odpočívať v zdvihnutom koši.",
				),
				CookingTask(
					id = "heat-fryer-180",
					title = "Dohrej fritézu na 180 °C",
					durationSeconds = fryerTiming.from155To180Seconds,
					dependsOn = setOf("lift-first-basket"),
					resources = uses(FRYER),
					kind = TaskKind.WAIT,
					instruction = "Hneď nastav fritézu na 180 °C. Ohrieva sa počas toho, ako hranolky odpočívajú. Recept počíta s časom ${fryerTiming.from155To180Seconds / 60} min.",
				),
				CookingTask(
					id = "lower-second-basket",
					title = "Spusť kôš na druhé smaženie",
					durationSeconds = 30,
					dependsOn = setOf("cool-fries", "heat-fryer-180"),
					resources = uses(COOK, FRYER),
					instruction = "Keď sú hranolky odpočinuté a fritéza má 180 °C, spusti kôš späť do oleja.",
				),
				CookingTask(
					id = "second-fry",
					title = "Druhé smaženie – 180 °C",
					durationSeconds = 3 * 60,
					dependsOn = setOf("lower-second-basket"),
					resources = uses(FRYER),
					kind = TaskKind.WAIT,
					instruction = "Smaž približne 2–3 minúty pri 180 °C, kým sú hranolky zlaté a chrumkavé. Sleduj farbu, ku koncu sa mení rýchlo.",
				),
				CookingTask(
					id = "salt-serve",
					title = "Vyber, osoľ a podávaj",
					durationSeconds = 2 * 60,
					dependsOn = setOf("second-fry"),
					resources = uses(COOK, FRYER),
					instruction = "Zdvihni kôš, nechaj hranolky krátko odkvapkať, vysyp ich a ešte horúce osoľ. Hneď podávaj.",
				),
			),
			troubleshooting = listOf(
				TroubleshootingTip(
					problem = "Hranolky sa pri predvarení rozpadajú",
					advice = "Hneď ich zlej. Nabudúce ich var kratšie alebo nakrájaj o trochu hrubšie. Ocot vo vode pomáha povrchu držať tvar.",
				),
				TroubleshootingTip(
					problem = "Po prvom smažení už hnednú",
					advice = "Prvá fáza ich má hlavne dovariť vnútri, nie zafarbiť. Skontroluj, že fritéza je nastavená približne na 155 °C.",
				),
				TroubleshootingTip(
					problem = "Hranolky sú mastné a mäkké",
					advice = "Kôš mohol byť príliš plný alebo bola teplota nízka. Pri druhom smažení použi 180 °C a nechaj oleju priestor obtekať hranolky.",
				),
				TroubleshootingTip(
					problem = "Hranolky tmavnú príliš rýchlo",
					advice = "Druhé smaženie ukonči skôr. Rozhoduje farba a chrumkavosť, nie nutne celé 3 minúty.",
				),
			),
		)
	}
}
