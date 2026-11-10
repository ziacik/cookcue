package com.ziacik.cookcue.core.recipes

import com.ziacik.cookcue.core.model.CookingTask
import com.ziacik.cookcue.core.model.Ingredient
import com.ziacik.cookcue.core.model.Recipe
import com.ziacik.cookcue.core.model.ResourceRequirement
import com.ziacik.cookcue.core.model.Skill
import com.ziacik.cookcue.core.model.TaskKind
import com.ziacik.cookcue.core.model.TroubleshootingTip

object FriedCheeseWithBoiledPotatoesRecipe {
	private const val COOK = "cook"
	private const val POT = "pot"
	private const val PAN = "pan"
	private const val BURNER = "burner"
	private const val SINK = "sink"

	private fun uses(vararg resources: String): Set<ResourceRequirement> {
		return resources.map { ResourceRequirement(it) }.toSet()
	}

	val recipe = Recipe(
		id = "fried-cheese-boiled-potatoes",
		title = "Vyprážaný eidam s varenými zemiakmi",
		description = "Pre 1 osobu · dvojitý trojobal, aby syr menej vytekal",
		servings = 1,
		ingredients = listOf(
			Ingredient("eidam", "150 g"),
			Ingredient("zemiaky", "300 g"),
			Ingredient("vajce", "1 ks"),
			Ingredient("hladká múka", "cca 3 PL"),
			Ingredient("strúhanka", "cca 6 PL"),
			Ingredient("olej na vyprážanie", "cca 200–250 ml podľa panvice"),
			Ingredient("maslo", "1 ČL, voliteľné na zemiaky"),
			Ingredient("soľ", "podľa chuti"),
		),
		resourceCapacities = mapOf(
			COOK to 1,
			POT to 1,
			PAN to 1,
			BURNER to 1,
			SINK to 1,
		),
		preCookingNote = "Eidam nechaj až do obaľovania v chladničke. Studený syr sa pri vyprážaní správa lepšie.",
		tasks = listOf(
			CookingTask(
				id = "prep-potatoes",
				title = "Ošúp a nakrájaj zemiaky",
				durationSeconds = 7 * 60,
				resources = uses(COOK, SINK),
				skill = Skill.PEELING,
				instruction = "Ošúp 300 g zemiakov a nakrájaj ich na približne rovnako veľké kúsky, asi 3–4 cm. Opláchni ich studenou vodou.",
			),
			CookingTask(
				id = "start-potatoes",
				title = "Daj zemiaky variť",
				durationSeconds = 3 * 60,
				dependsOn = setOf("prep-potatoes"),
				resources = uses(COOK, POT, BURNER),
				instruction = "Daj zemiaky do hrnca, zalej ich studenou vodou asi 2 cm nad zemiaky a pridaj približne 1/2 ČL soli. Zapni sporák na vysoký výkon.",
			),
			CookingTask(
				id = "wait-for-potato-boil",
				title = "Priveď zemiaky do varu",
				durationSeconds = 5 * 60,
				dependsOn = setOf("start-potatoes"),
				resources = uses(POT, BURNER),
				kind = TaskKind.EVENT,
				instruction = "Keď voda začne vrieť, potvrď to a stiahni výkon tak, aby zemiaky pokojne vreli.",
				actionLabel = "VODA VRIE",
			),
			CookingTask(
				id = "potatoes-minimum-cook",
				title = "Var zemiaky 12 minút",
				durationSeconds = 12 * 60,
				dependsOn = setOf("wait-for-potato-boil"),
				resources = uses(POT, BURNER),
				kind = TaskKind.WAIT,
				instruction = "Var zemiaky miernym varom. Po 12 minútach ich začneš skúšať vidličkou.",
			),
			CookingTask(
				id = "potatoes-ready",
				title = "Skontroluj zemiaky",
				durationSeconds = 3 * 60,
				dependsOn = setOf("potatoes-minimum-cook"),
				resources = uses(POT, BURNER),
				kind = TaskKind.EVENT,
				instruction = "Pichni vidličku do najväčšieho kúsku. Vošla dnu ľahko bez tvrdého stredu?",
				actionLabel = "MÄKKÉ",
				retryActionLabel = "EŠTE NIE",
				retryAfterSeconds = 3 * 60,
			),
			CookingTask(
				id = "prep-cheese",
				title = "Priprav eidam",
				durationSeconds = 3 * 60,
				dependsOn = setOf("start-potatoes"),
				resources = uses(COOK),
				instruction = "Vyber 150 g eidamu z chladničky až teraz. Ak je v jednom hrubom kuse, priprav plátok hrubý približne 1–1,5 cm. Povrch osušíš papierovou utierkou, aby sa naň trojobal dobre chytil.",
			),
			CookingTask(
				id = "setup-breading",
				title = "Priprav trojobal",
				durationSeconds = 4 * 60,
				dependsOn = setOf("prep-cheese"),
				resources = uses(COOK),
				instruction = "Priprav tri taniere: hladká múka, rozšľahané vajce a strúhanka. Vajce rozšľahaj vidličkou so štipkou soli.",
			),
			CookingTask(
				id = "bread-cheese",
				title = "Dvakrát obaľ eidam",
				durationSeconds = 8 * 60,
				dependsOn = setOf("setup-breading"),
				resources = uses(COOK),
				instruction = "Eidam obaľ takto: múka → vajce → strúhanka → vajce → strúhanka. Pri druhom vajci a strúhanke dôkladne obaľ hlavne hrany a rohy.",
				tips = listOf(
					"Do strúhanky syr netlač príliš silno, ale skontroluj, že nikde neostalo holé miesto.",
					"Dvojitá vrstva vajca a strúhanky výrazne znižuje riziko, že syr pri vyprážaní vytečie.",
				),
			),
			CookingTask(
				id = "drain-potatoes",
				title = "Zlej a dochuť zemiaky",
				durationSeconds = 4 * 60,
				dependsOn = setOf("potatoes-ready"),
				resources = uses(COOK, POT, SINK),
				instruction = "Zlej vodu zo zemiakov. Vráť ich do teplého hrnca, podľa chuti dosoľ a voliteľne pridaj 1 ČL masla. Prikry ich, aby zostali teplé.",
			),
			CookingTask(
				id = "heat-oil",
				title = "Rozohrej olej",
				durationSeconds = 3 * 60,
				dependsOn = setOf("potatoes-ready", "bread-cheese", "drain-potatoes"),
				resources = uses(COOK, PAN, BURNER),
				instruction = "Do panvice nalej olej približne do výšky 5–8 mm a rozohrej ho na stredne vysokom výkone. Ak máš teplomer, cieľ je približne 170–175 °C. Bez teplomera vhoď štipku strúhanky: má hneď začať svižne bublať, ale nesmie okamžite tmavnúť.",
				tips = listOf(
					"Ak olej dymí, je príliš horúci. Panvicu na chvíľu odstav a potom pokračuj na nižšom výkone.",
				),
			),
			CookingTask(
				id = "fry-first-side",
				title = "Vyprážaj prvú stranu",
				durationSeconds = 2 * 60,
				dependsOn = setOf("heat-oil"),
				resources = uses(COOK, PAN, BURNER),
				instruction = "Opatrne polož obalený eidam do horúceho oleja. Vyprážaj približne 1,5–2 minúty, kým je spodná strana zlatá. Syr zbytočne neposúvaj.",
			),
			CookingTask(
				id = "fry-second-side",
				title = "Otoč a vypraž druhú stranu",
				durationSeconds = 2 * 60,
				dependsOn = setOf("fry-first-side"),
				resources = uses(COOK, PAN, BURNER),
				instruction = "Eidam opatrne otoč širokou obracačkou a vyprážaj druhú stranu približne 1,5–2 minúty dozlatista. Ak začne syr niekde vytekať, hneď ho vyber.",
			),
			CookingTask(
				id = "serve",
				title = "Nechaj odkvapkať a podávaj",
				durationSeconds = 3 * 60,
				dependsOn = setOf("fry-second-side"),
				resources = uses(COOK),
				instruction = "Vyber eidam na papierovú utierku a nechaj ho asi 30 sekúnd odkvapkať. Hneď podávaj s teplými varenými zemiakmi.",
			),
		),
		troubleshooting = listOf(
			TroubleshootingTip(
				problem = "Syr vyteká",
				advice = "Hneď ho vyber z oleja. Nabudúce dôkladnejšie obaľ hrany dvojitou vrstvou vajca a strúhanky a vyprážaj studený syr v dostatočne horúcom oleji.",
			),
			TroubleshootingTip(
				problem = "Obal tmavne príliš rýchlo",
				advice = "Olej je príliš horúci. Stiahni výkon alebo panvicu na chvíľu odstav.",
			),
			TroubleshootingTip(
				problem = "Obal je mastný a bledý",
				advice = "Olej bol pravdepodobne málo horúci. Pred ďalším vyprážaním ho nechaj viac zohriať.",
			),
			TroubleshootingTip(
				problem = "Zemiaky sú tvrdé",
				advice = "Var ich ďalej a skúšaj znova po 3 minútach. Rozhoduje mäkkosť, nie presný čas.",
			),
		),
	)
}
