package com.ziacik.cookcue.core.recipes

import com.ziacik.cookcue.core.model.CookingTask
import com.ziacik.cookcue.core.model.Ingredient
import com.ziacik.cookcue.core.model.Recipe
import com.ziacik.cookcue.core.model.ResourceRequirement
import com.ziacik.cookcue.core.model.Skill
import com.ziacik.cookcue.core.model.TaskKind
import com.ziacik.cookcue.core.model.TroubleshootingTip

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
		title = "Fazuľová polievka s klobásou",
		description = "Pre 2 osoby · tekutá polievka, nie prívarok · podrobný postup pre začiatočníka",
		servings = 2,
		ingredients = listOf(
			Ingredient("suchá červená fazuľa", "120 g"),
			Ingredient("údená klobása", "150 g"),
			Ingredient("menšia cibuľa", "1 ks"),
			Ingredient("cesnak", "2 strúčiky"),
			Ingredient("menšia mrkva", "1/3 ks"),
			Ingredient("malý zemiak", "1 ks"),
			Ingredient("bobkový list + nové korenie", "1 list + 3 guľky"),
			Ingredient("zeleninový bujón", "1/2 kocky"),
			Ingredient("sladká mletá paprika", "1/2 ČL"),
			Ingredient("majorán", "1/2 ČL"),
			Ingredient("kvasný liehový ocot (8 %)", "1/4 ČL"),
			Ingredient("soľ", "1/2 ČL na začiatok dochucovania; podľa chuti až cca 3/4 ČL"),
			Ingredient("čierne korenie", "podľa chuti"),
			Ingredient("olej alebo masť", "trochu"),
			Ingredient("voda", "800 ml na začiatok + horúca voda na dolievanie"),
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
				title = "Zlej a opláchni fazuľu",
				durationSeconds = 2 * 60,
				resources = uses(COOK, SINK),
				instruction = "120 g suchej fazule má byť namočených 8–12 hodín vo veľkom množstve studenej vody. Namáčaciu vodu vylej a fazuľu opláchni.",
			),
			CookingTask(
				id = "start-beans",
				title = "Daj fazuľu variť",
				durationSeconds = 60,
				dependsOn = setOf("rinse"),
				resources = uses(COOK, POT, BURNER),
				instruction = "Do hrnca daj namočenú fazuľu, 800 ml čistej vody, 1/2 kocky zeleninového bujónu, 1 bobkový list a 3 guľky nového korenia. Zapni sporák.",
			),
			CookingTask(
				id = "wait-for-boil",
				title = "Priveď fazuľu do varu",
				durationSeconds = 4 * 60,
				dependsOn = setOf("start-beans"),
				resources = uses(POT, BURNER),
				kind = TaskKind.EVENT,
				instruction = "Keď voda začne vrieť, potvrď to. Potom stiahni výkon tak, aby voda pokojne a stále bublala.",
				actionLabel = "VODA VRIE",
			),
			CookingTask(
				id = "beans-minimum-cook",
				title = "Var fazuľu aspoň 40 minút",
				durationSeconds = 40 * 60,
				dependsOn = setOf("wait-for-boil"),
				resources = uses(POT, BURNER),
				kind = TaskKind.WAIT,
				instruction = "Var ju miernym, stálym varom. Čas je iba orientačný; po 40 minútach ju začneš kontrolovať.",
				tips = listOf(
					"Hrniec môže byť prikrytý alebo mierne pootvorený, aby sa zbytočne nestrácala voda.",
					"Fazuľa je pripravená až vtedy, keď vôbec nechrumká, nemá tvrdý stred a už teraz by si ju normálne chcel jesť.",
					"Ak po 50 minútach ešte nie je mäkká, jednoducho var ďalej. Prudší var nepomôže.",
				),
			),
			CookingTask(
				id = "beans-nearly-ready",
				title = "Skontroluj fazuľu",
				durationSeconds = 10 * 60,
				dependsOn = setOf("beans-minimum-cook"),
				resources = uses(POT, BURNER),
				kind = TaskKind.EVENT,
				instruction = "Ochutnaj fazuľu. Ak je ešte zjavne tvrdá, var ďalej. Potvrď až vtedy, keď je už skoro hotová a je čas začať opekať klobásu.",
				actionLabel = "FAZUĽA JE SKORO HOTOVÁ",
			),
			CookingTask(
				id = "beans-ready",
				title = "Fazuľa dobieha",
				durationSeconds = 5 * 60,
				dependsOn = setOf("beans-nearly-ready"),
				resources = uses(POT, BURNER),
				kind = TaskKind.EVENT,
				instruction = "Počas práce s panvicou fazuľu ďalej mierne var. Keď už vôbec nechrumká a nemá tvrdý stred, potvrď to.",
				actionLabel = "FAZUĽA JE MÄKKÁ",
			),
			CookingTask(
				id = "prep-carrot",
				title = "Nakrájaj mrkvu",
				durationSeconds = 3 * 60,
				dependsOn = setOf("start-beans"),
				resources = uses(COOK),
				skill = Skill.PEELING,
				instruction = "1/3 menšej mrkvy nakrájaj na kolieska hrubé približne 2–3 mm.",
			),
			CookingTask(
				id = "prep-potato",
				title = "Nakrájaj zemiak",
				durationSeconds = 4 * 60,
				dependsOn = setOf("start-beans"),
				resources = uses(COOK),
				skill = Skill.PEELING,
				instruction = "1 malý zemiak nakrájaj na kocky približne 1–1,5 cm.",
			),
			CookingTask(
				id = "prep-onion",
				title = "Nakrájaj cibuľu",
				durationSeconds = 3 * 60,
				dependsOn = setOf("start-beans"),
				resources = uses(COOK),
				skill = Skill.KNIFE,
				instruction = "1 menšiu cibuľu nakrájaj nadrobno.",
			),
			CookingTask(
				id = "prep-garlic",
				title = "Pretlač cesnak",
				durationSeconds = 60,
				dependsOn = setOf("start-beans"),
				resources = uses(COOK),
				instruction = "2 strúčiky cesnaku pretlač lisom.",
			),
			CookingTask(
				id = "prep-sausage",
				title = "Nakrájaj klobásu",
				durationSeconds = 2 * 60,
				dependsOn = setOf("start-beans"),
				resources = uses(COOK),
				skill = Skill.KNIFE,
				instruction = "150 g klobásy nakrájaj na kolieska; väčšie kolieska pokojne prekroj ešte napoly.",
			),
			CookingTask(
				id = "brown-sausage",
				title = "Opeč klobásu",
				durationSeconds = 4 * 60,
				dependsOn = setOf("beans-nearly-ready", "prep-sausage"),
				resources = uses(COOK, PAN, BURNER),
				instruction = "Rozohrej panvicu na strednom výkone. Daj trochu oleja alebo masti a opeč klobásu, kým zavonia, trochu zhnedne a pustí tuk. Klobásu vyber na tanier.",
				tips = listOf(
					"Tuk z panvice nevylievaj.",
				),
			),
			CookingTask(
				id = "saute-onion",
				title = "Orestuj cibuľu",
				durationSeconds = 4 * 60,
				dependsOn = setOf("prep-onion", "brown-sausage"),
				resources = uses(COOK, PAN, BURNER),
				instruction = "Na tuku z klobásy orestuj cibuľu. Hotová je, keď je mäkká, nechrumká a nevonia surovo.",
				tips = listOf(
					"Farbu cibule veľmi nerieš – tuk z klobásy ju môže výrazne zafarbiť.",
				),
			),
			CookingTask(
				id = "garlic-paprika",
				title = "Pridaj cesnak a papriku",
				durationSeconds = 40,
				dependsOn = setOf("prep-garlic", "saute-onion"),
				resources = uses(COOK, PAN, BURNER),
				instruction = "Pridaj 2 pretlačené strúčiky cesnaku a miešaj len 20–30 sekúnd. Pridaj 1/2 ČL sladkej papriky a prakticky hneď 1–2 PL vody z hrnca. Premiešaj.",
				tips = listOf(
					"Paprika sa nesmie spáliť, inak zhorkne.",
				),
			),
			CookingTask(
				id = "combine",
				title = "Spoj všetko v hrnci",
				durationSeconds = 2 * 60,
				dependsOn = setOf(
					"beans-ready",
					"garlic-paprika",
					"prep-carrot",
					"prep-potato",
				),
				resources = uses(COOK, POT),
				instruction = "Celý obsah panvice prelej do hrnca. Vráť 150 g opečenej klobásy. Pridaj 1/3 mrkvy a 1 malý zemiak. Premiešaj.",
			),
			CookingTask(
				id = "adjust-water",
				title = "Nastav množstvo vody",
				durationSeconds = 2 * 60,
				dependsOn = setOf("combine"),
				resources = uses(COOK, POT),
				instruction = "Ak ingrediencie trčia nad hladinu, dolej horúcu vodu. Začni približne 100–150 ml, hneď premiešaj a pozri. Ak treba, hneď pridaj ďalšiu dávku; medzi dolievaniami nemusíš čakať.",
				tips = listOf(
					"Horúcu vodu môžeš najprv naliať do použitej panvice, zakrúžiť ňou a až potom preliať do hrnca. Zoberieš tým z panvice zvyšky opečenej chuti.",
					"Cieľ: všetko je ponorené, okolo ingrediencií je dosť vývaru a výsledok vyzerá ako polievka, nie prívarok.",
				),
			),
			CookingTask(
				id = "final-cook-min",
				title = "Var všetko spolu aspoň 15 minút",
				durationSeconds = 15 * 60,
				dependsOn = setOf("adjust-water"),
				resources = uses(POT, BURNER),
				kind = TaskKind.WAIT,
				instruction = "Nechaj všetko spolu mierne vrieť. Po 15 minútach skontroluješ mäkkosť.",
				tips = listOf(
					"V tejto fáze nechaj hrniec radšej bez pokrievky. Ak sa voda stráca príliš rýchlo, polož pokrievku nakrivo.",
				),
			),
			CookingTask(
				id = "final-ready",
				title = "Skontroluj finálnu mäkkosť",
				durationSeconds = 5 * 60,
				dependsOn = setOf("final-cook-min"),
				resources = uses(POT, BURNER),
				kind = TaskKind.EVENT,
				instruction = "Hotovo je, keď je fazuľa úplne mäkká, mrkva mäkká a do zemiaka ide vidlička ľahko bez odporu. Ak nie, var ďalej a potvrď až potom.",
				actionLabel = "VŠETKO JE MÄKKÉ",
			),
			CookingTask(
				id = "season",
				title = "Finálne dochuť",
				durationSeconds = 3 * 60,
				dependsOn = setOf("final-ready"),
				resources = uses(COOK, POT),
				instruction = "Vypni sporák. Pridaj 1/2 ČL soli, premiešaj a ochutnaj. Ak treba, pridávaj po troške; celkovo približne do 3/4 ČL. Pridaj čierne korenie – na začiatok asi 2–3 otočenia mlynčeka. 1/2 ČL majoránu rozdrv medzi prstami alebo dlaňami, nasyp do polievky a premiešaj. Pridaj 1/4 ČL kvasného liehového octu (8 %), premiešaj a ochutnaj.",
				tips = listOf(
					"Ocot nemá byť cítiť ako kyslosť. Má len zvýrazniť ostatné chute.",
				),
			),
			CookingTask(
				id = "rest",
				title = "Nechaj polievku postáť",
				durationSeconds = 5 * 60,
				dependsOn = setOf("season"),
				resources = uses(POT),
				kind = TaskKind.WAIT,
				instruction = "Nechaj polievku 5 minút postáť.",
			),
			CookingTask(
				id = "final-taste",
				title = "Ešte raz ochutnaj",
				durationSeconds = 60,
				dependsOn = setOf("rest"),
				resources = uses(COOK, POT),
				instruction = "Po 5 minútach ešte raz ochutnaj a podľa potreby použi krízovú pomoc nižšie.",
			),
		),
		troubleshooting = listOf(
			TroubleshootingTip(
				problem = "Príliš hustá",
				advice = "Dolej horúcu vodu, premiešaj a znova ochutnaj soľ.",
			),
			TroubleshootingTip(
				problem = "Príliš riedka",
				advice = "Pár minút var bez pokrievky; prípadne rozpuč pár fazúľ o stenu hrnca.",
			),
			TroubleshootingTip(
				problem = "Fazuľa tvrdá",
				advice = "Var ďalej. Čas je iba orientačný.",
			),
			TroubleshootingTip(
				problem = "Zemiak tvrdý",
				advice = "Pokračuj vo varení po 5 minútach a skúšaj vidličkou.",
			),
			TroubleshootingTip(
				problem = "Mdlá chuť",
				advice = "Najprv trochu soli, potom korenie alebo majorán; až potom ďalší ocot.",
			),
			TroubleshootingTip(
				problem = "Príliš slaná",
				advice = "Zrieď horúcou vodou. Ďalší bujón nepridávaj.",
			),
			TroubleshootingTip(
				problem = "Príliš kyslá / veľa octu",
				advice = "Dolej trochu horúcej vody, premiešaj a znova ochutnaj. Ak sa chuť príliš zriedi, pridaj štipku soli a majoránu. Cukor nepridávaj. Ak je polievka stále výrazne kyslá, ako poslednú záchranu pridaj naozaj malú štipku sódy bikarbóny, premiešaj a znovu ochutnaj.",
			),
		),
	)
}
