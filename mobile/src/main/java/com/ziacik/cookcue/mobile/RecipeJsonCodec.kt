package com.ziacik.cookcue.mobile

import com.ziacik.cookcue.core.model.CookingTask
import com.ziacik.cookcue.core.model.Ingredient
import com.ziacik.cookcue.core.model.Recipe
import com.ziacik.cookcue.core.model.ResourceRequirement
import com.ziacik.cookcue.core.model.Skill
import com.ziacik.cookcue.core.model.TaskKind
import com.ziacik.cookcue.core.model.TroubleshootingTip
import org.json.JSONArray
import org.json.JSONObject

internal object RecipeJsonCodec {
	private const val SCHEMA_VERSION = 1

	fun decode(json: String): Recipe {
		val root = JSONObject(json)
		require(root.optInt("schemaVersion", SCHEMA_VERSION) == SCHEMA_VERSION) {
			"Unsupported recipe schema version."
		}

		val recipe = Recipe(
			id = root.getString("id"),
			title = root.getString("title"),
			description = root.optString("description", ""),
			servings = root.getInt("servings"),
			ingredients = root.getJSONArray("ingredients").mapObjects { ingredient ->
				Ingredient(
					name = ingredient.getString("name"),
					amount = ingredient.getString("amount"),
				)
			},
			resourceCapacities = root.getJSONObject("resourceCapacities").toIntMap(),
			tasks = root.getJSONArray("tasks").mapObjects(::decodeTask),
			preCookingNote = root.optionalString("preCookingNote"),
			troubleshooting = root.optJSONArray("troubleshooting")
				?.mapObjects { tip ->
					TroubleshootingTip(
						problem = tip.getString("problem"),
						advice = tip.getString("advice"),
					)
				}
				.orEmpty(),
		)

		validate(recipe)
		return recipe
	}

	private fun decodeTask(task: JSONObject): CookingTask {
		return CookingTask(
			id = task.getString("id"),
			title = task.getString("title"),
			durationSeconds = task.getLong("durationSeconds"),
			dependsOn = task.optJSONArray("dependsOn")?.toStringSet().orEmpty(),
			resources = task.optJSONArray("resources")
				?.let(::decodeResources)
				.orEmpty(),
			kind = TaskKind.valueOf(task.optString("kind", TaskKind.ACTIVE.name)),
			skill = Skill.valueOf(task.optString("skill", Skill.GENERAL.name)),
			instruction = task.optString("instruction", task.getString("title")),
			tips = task.optJSONArray("tips")?.toStringList().orEmpty(),
			actionLabel = task.optionalString("actionLabel"),
			retryActionLabel = task.optionalString("retryActionLabel"),
			retryAfterSeconds = task.optionalLong("retryAfterSeconds"),
		)
	}

	private fun decodeResources(array: JSONArray): Set<ResourceRequirement> {
		return buildSet {
			for (index in 0 until array.length()) {
				when (val value = array.get(index)) {
					is String -> add(ResourceRequirement(value))
					is JSONObject -> add(
						ResourceRequirement(
							resource = value.getString("resource"),
							units = value.optInt("units", 1),
						)
					)
					else -> error("Invalid resource at index $index.")
				}
			}
		}
	}

	private fun validate(recipe: Recipe) {
		require(recipe.id.isNotBlank())
		require(recipe.title.isNotBlank())
		require(recipe.servings > 0)
		require(recipe.tasks.isNotEmpty())

		val taskIds = recipe.tasks.map { it.id }
		require(taskIds.size == taskIds.toSet().size) {
			"Recipe '${recipe.id}' contains duplicate task ids."
		}

		val knownTaskIds = taskIds.toSet()
		recipe.tasks.forEach { task ->
			require(task.id !in task.dependsOn) {
				"Task '${task.id}' cannot depend on itself."
			}
			val unknownDependencies = task.dependsOn - knownTaskIds
			require(unknownDependencies.isEmpty()) {
				"Task '${task.id}' has unknown dependencies: $unknownDependencies"
			}
		}
	}

	private fun JSONObject.optionalString(key: String): String? {
		return if (!has(key) || isNull(key)) null else getString(key)
	}

	private fun JSONObject.optionalLong(key: String): Long? {
		return if (!has(key) || isNull(key)) null else getLong(key)
	}

	private fun JSONObject.toIntMap(): Map<String, Int> {
		val result = linkedMapOf<String, Int>()
		val keys = keys()
		while (keys.hasNext()) {
			val key = keys.next()
			result[key] = getInt(key)
		}
		return result
	}

	private fun JSONArray.toStringList(): List<String> {
		return List(length()) { index -> getString(index) }
	}

	private fun JSONArray.toStringSet(): Set<String> {
		return toStringList().toSet()
	}

	private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> {
		return List(length()) { index -> transform(getJSONObject(index)) }
	}
}
