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
		description = "Pre 2 osoby · bez smotany · opečené párky, zemiaky a paprikový základ s bujónom",
		servings = 2,
		ingredients = listOf(
			Ingredient("frankfurtské párky", "2 ks (cca 150 g)"),
			Ingredient("zemiaky", "cca 250 g"),
			Ingredient("cibuľa", "1 väčšia ks"),
			Ingredient("cesnak", "4 strúčiky"),
			Ingredient("hladká múka", "1 PL"),
			Ingredient("gulášové korenie", "1 PL"),
			Ingredient("bujón", "1 kocka"),
			Ingredient("voda", "750 ml"),
			Ingredient("olej", "1 PL"),
			Ingredient("petržlenová vňať", "1 PL nasekanej alebo podľa chuti"),
			Ingredient("soľ", "podľa chuti, opatrne kvôli bujónu"),
		),
		resourceCapacities = mapOf(
			COOK to 1,
			POT to 1,
			BURNER to 1,
		),
		tasks = listOf(
			CookingTask(
				id = "prep-sausages",
				title = "Nakrájaj párky",
				durationSeconds = 4 * 60,
				resources = uses(COOK),
				skill = Skill.KNIFE,
				instruction = "2 frankfurtské párky nakrájaj na kolieska hrubé približne 5–8 mm.",
			),
			CookingTask(
				id = "brown-sausages",
				title = "Opeč párky",
				durationSeconds = 5 * 60,
				dependsOn = setOf("prep-sausages"),
				resources = uses(COOK, POT, BURNER),
				instruction = "Do hrnca daj 1 PL oleja a rozohrej ho na strednom výkone. Pridaj párky a opekaj ich za občasného miešania, kým na viacerých miestach jemne nezhnednú.",
			),
			CookingTask(
				id = "remove-sausages",
				title = "Vyber párky bokom",
				durationSeconds = 2 * 60,
				dependsOn = setOf("brown-sausages"),
				resources = uses(COOK, POT),
				instruction = "Opečené párky vyber dierovanou lyžicou alebo lyžicou na tanier. Tuk a opečenú chuť nechaj v hrnci.",
			),
			CookingTask(
				id = "prep-onion",
				title = "Nakrájaj cibuľu",
				durationSeconds = 5 * 60,
				dependsOn = setOf("remove-sausages"),
				resources = uses(COOK),
				skill = Skill.KNIFE,
				instruction = "Ošúp 1 väčšiu cibuľu a nakrájaj ju nadrobno.",
			),
			CookingTask(
				id = "saute-onion",
				title = "Pomaly opeč cibuľu",
				durationSeconds = 6 * 60,
				dependsOn = setOf("prep-onion"),
				resources = uses(COOK, POT, BURNER),
				instruction = "Daj cibuľu do toho istého hrnca a opekaj ju na strednom až stredne nízkom výkone, kým zmäkne a jemne zozlatne. Priebežne miešaj.",
				tips = listOf(
					"Ak sa dno začína pripaľovať, pridaj 1–2 PL vody a oškrab vareškou opečené kúsky zo dna.",
				),
			),
			CookingTask(
				id = "add-flour",
				title = "Pridaj múku",
				durationSeconds = 2 * 60,
				dependsOn = setOf("saute-onion"),
				resources = uses(COOK, POT, BURNER),
				instruction = "Prisyp 1 PL hladkej múky a asi 1 minútu ju s cibuľou miešaj a opekaj na miernejšom výkone. Nesmie sa pripáliť.",
			),
			CookingTask(
				id = "garlic-spice",
				title = "Pridaj cesnak a gulášové korenie",
				durationSeconds = 2 * 60,
				dependsOn = setOf("add-flour"),
				resources = uses(COOK, POT, BURNER),
				instruction = "Pretlač priamo do hrnca 2 strúčiky cesnaku a pridaj 1 PL gulášového korenia. Miešaj iba asi 20–30 sekúnd a hneď pokračuj zaliatím.",
				tips = listOf(
					"Korenený základ nenechávaj dlho nasucho na horúcom dne, aby nezhorkol.",
				),
			),
			CookingTask(
				id = "add-bouillon-water",
				title = "Zalej vodou a pridaj bujón",
				durationSeconds = 4 * 60,
				dependsOn = setOf("garlic-spice"),
				resources = uses(COOK, POT, BURNER),
				instruction = "Postupne prilievaj 750 ml vody a pritom stále miešaj, aby sa múka rozmiešala bez hrudiek. Pridaj 1 kocku bujónu a premiešaj odo dna.",
			),
			CookingTask(
				id = "wait-for-boil",
				title = "Priveď základ do varu",
				durationSeconds = 7 * 60,
				dependsOn = setOf("add-bouillon-water"),
				resources = uses(POT, BURNER),
				kind = TaskKind.EVENT,
				instruction = "Keď polievka začne normálne vrieť, potvrď to. Potom stiahni výkon na mierny var.",
				actionLabel = "VRIE",
			),
			CookingTask(
				id = "base-simmer",
				title = "Povar základ 15 minút",
				durationSeconds = 15 * 60,
				dependsOn = setOf("wait-for-boil"),
				resources = uses(POT, BURNER),
				kind = TaskKind.WAIT,
				instruction = "Nechaj základ 15 minút mierne prebublávať. Počas toho pripravíš zemiaky a zvyšný cesnak.",
			),
			CookingTask(
				id = "prep-potatoes",
				title = "Ošúp a nakrájaj zemiaky",
				durationSeconds = 9 * 60,
				dependsOn = setOf("wait-for-boil"),
				resources = uses(COOK),
				skill = Skill.PEELING,
				instruction = "Ošúp približne 250 g zemiakov a nakrájaj ich na kocky asi 1,5 cm. Snaž sa o podobnú veľkosť, aby zmäkli naraz.",
			),
			CookingTask(
				id = "prep-finish",
				title = "Priprav cesnak a petržlen",
				durationSeconds = 4 * 60,
				dependsOn = setOf("prep-potatoes"),
				resources = uses(COOK),
				instruction = "Ošúp zostávajúce 2 strúčiky cesnaku a nechaj ich pripravené pri lise. Ak používaš čerstvú petržlenovú vňať, nasekaj približne 1 PL.",
			),
			CookingTask(
				id = "add-potatoes",
				title = "Pridaj zemiaky",
				durationSeconds = 2 * 60,
				dependsOn = setOf("base-simmer", "prep-potatoes"),
				resources = uses(COOK, POT, BURNER),
				instruction = "Pridaj nakrájané zemiaky do polievky, premiešaj a nechaj znovu rozbehnúť mierny var.",
			),
			CookingTask(
				id = "potatoes-minimum-cook",
				title = "Var zemiaky aspoň 12 minút",
				durationSeconds = 12 * 60,
				dependsOn = setOf("add-potatoes"),
				resources = uses(POT, BURNER),
				kind = TaskKind.WAIT,
				instruction = "Zemiaky var miernym varom aspoň 12 minút. Potom skontroluješ, či sú mäkké.",
			),
			CookingTask(
				id = "potatoes-ready",
				title = "Skontroluj zemiaky",
				durationSeconds = 3 * 60,
				dependsOn = setOf("potatoes-minimum-cook"),
				resources = uses(POT, BURNER),
				kind = TaskKind.EVENT,
				instruction = "Pichni vidličkou do jednej z väčších kociek. Ide vidlička ľahko dnu a zemiak už nemá tvrdý stred?",
				actionLabel = "ÁNO",
				retryActionLabel = "NIE",
				retryAfterSeconds = 3 * 60,
			),
			CookingTask(
				id = "finish-soup",
				title = "Vráť párky a pridaj cesnak",
				durationSeconds = 3 * 60,
				dependsOn = setOf("potatoes-ready", "prep-finish"),
				resources = uses(COOK, POT, BURNER),
				instruction = "Vráť do hrnca opečené párky a pretlač zostávajúce 2 strúčiky cesnaku. Premiešaj a nechaj veľmi mierne povariť 2–3 minúty.",
			),
			CookingTask(
				id = "season-finish",
				title = "Dochuť a pridaj petržlen",
				durationSeconds = 3 * 60,
				dependsOn = setOf("finish-soup"),
				resources = uses(COOK, POT),
				instruction = "Vypni sporák. Ochutnaj a až teraz podľa potreby dosoľ — bujón aj párky už soľ obsahujú. Pridaj petržlenovú vňať a premiešaj.",
			),
			CookingTask(
				id = "rest",
				title = "Nechaj polievku postáť",
				durationSeconds = 3 * 60,
				dependsOn = setOf("season-finish"),
				resources = uses(POT),
				kind = TaskKind.WAIT,
				instruction = "Nechaj polievku 3 minúty postáť, potom ešte raz ochutnaj a podávaj.",
			),
		),
		troubleshooting = listOf(
			TroubleshootingTip(
				problem = "Zemiaky sú stále tvrdé",
				advice = "Pokračuj vo varení po 3 minútach a znovu ich skontroluj vidličkou.",
			),
			TroubleshootingTip(
				problem = "V polievke sú hrudky múky",
				advice = "Skús ich rozmiešať metličkou. Nabudúce prilievaj vodu do múčneho základu postupne a stále miešaj.",
			),
			TroubleshootingTip(
				problem = "Polievka je príliš hustá",
				advice = "Pridávaj po troške horúcu vodu, vždy premiešaj a znovu ochutnaj.",
			),
			TroubleshootingTip(
				problem = "Polievka je príliš riedka",
				advice = "Var ju ešte pár minút bez pokrievky. Múka aj zemiaky ju pri ďalšom varení trochu zahustia.",
			),
			TroubleshootingTip(
				problem = "Polievka je príliš slaná",
				advice = "Zrieď ju trochou horúcej vody. Ďalší bujón už nepridávaj.",
			),
			TroubleshootingTip(
				problem = "Základ sa pripaľuje",
				advice = "Stiahni výkon a pridaj 1–2 PL vody. Opečené kúsky zo dna uvoľni vareškou, ale ak sú čierne a horké, nezoškrabuj ich do polievky.",
			),
		),
	)
}
