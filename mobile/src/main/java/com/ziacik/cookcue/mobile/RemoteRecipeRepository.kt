package com.ziacik.cookcue.mobile

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.ziacik.cookcue.core.model.Recipe
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

internal object RemoteRecipeRepository {
	private const val INDEX_URL =
		"https://raw.githubusercontent.com/ziacik/cookcue/master/recipes/index.json"
	private const val RECIPE_BASE_URL =
		"https://raw.githubusercontent.com/ziacik/cookcue/master/recipes/"
	private const val CATALOG_SCHEMA_VERSION = 1
	private const val CONNECT_TIMEOUT_MS = 5_000
	private const val READ_TIMEOUT_MS = 10_000

	private data class CatalogEntry(
		val id: String,
		val version: Int,
		val file: String,
		val minAppVersion: Int,
	)

	fun loadCached(context: Context): List<Recipe> {
		val cacheDir = cacheDir(context)
		val indexFile = File(cacheDir, "index.json")
		if (!indexFile.isFile) {
			return emptyList()
		}

		return runCatching {
			val entries = decodeCatalog(indexFile.readText())
			entries.mapNotNull { entry ->
				if (entry.minAppVersion > BuildConfig.VERSION_CODE) {
					return@mapNotNull null
				}

				val file = cachedRecipeFile(cacheDir, entry)
				if (!file.isFile) {
					return@mapNotNull null
				}

				runCatching {
					RecipeJsonCodec.decode(file.readText()).also { recipe ->
						require(recipe.id == entry.id) {
							"Recipe id '${recipe.id}' does not match catalog id '${entry.id}'."
						}
					}
				}.getOrNull()
			}
		}.getOrElse { emptyList() }
	}

	fun refresh(
		context: Context,
		onUpdated: (List<Recipe>) -> Unit,
	) {
		val appContext = context.applicationContext
		Thread(
			{
				val recipes = runCatching {
					refreshBlocking(appContext)
				}.getOrNull() ?: return@Thread

				Handler(Looper.getMainLooper()).post {
					onUpdated(recipes)
				}
			},
			"CookCue-recipe-refresh",
		).start()
	}

	private fun refreshBlocking(context: Context): List<Recipe> {
		val indexJson = downloadUtf8(INDEX_URL)
		val entries = decodeCatalog(indexJson)
		val cacheDir = cacheDir(context).also(File::mkdirs)

		val recipes = entries.mapNotNull { entry ->
			if (entry.minAppVersion > BuildConfig.VERSION_CODE) {
				return@mapNotNull null
			}

			val cacheFile = cachedRecipeFile(cacheDir, entry)
			val recipeJson = if (cacheFile.isFile) {
				cacheFile.readText()
			} else {
				downloadUtf8(RECIPE_BASE_URL + validatedFileName(entry.file)).also { downloaded ->
					val decoded = RecipeJsonCodec.decode(downloaded)
					require(decoded.id == entry.id) {
						"Recipe id '${decoded.id}' does not match catalog id '${entry.id}'."
					}
					writeAtomic(cacheFile, downloaded)
				}
			}

			RecipeJsonCodec.decode(recipeJson).also { recipe ->
				require(recipe.id == entry.id) {
					"Recipe id '${recipe.id}' does not match catalog id '${entry.id}'."
				}
			}
		}

		writeAtomic(File(cacheDir, "index.json"), indexJson)
		pruneOldRecipeFiles(cacheDir, entries)
		return recipes
	}

	private fun decodeCatalog(json: String): List<CatalogEntry> {
		val root = JSONObject(json)
		require(root.getInt("schemaVersion") == CATALOG_SCHEMA_VERSION) {
			"Unsupported recipe catalog schema version."
		}

		val array = root.getJSONArray("recipes")
		val entries = List(array.length()) { index ->
			val item = array.getJSONObject(index)
			CatalogEntry(
				id = item.getString("id"),
				version = item.getInt("version"),
				file = validatedFileName(item.getString("file")),
				minAppVersion = item.optInt("minAppVersion", 1),
			)
		}

		require(entries.map { it.id }.distinct().size == entries.size) {
			"Recipe catalog contains duplicate ids."
		}
		require(entries.all { it.id.isNotBlank() && it.version > 0 && it.minAppVersion > 0 })
		return entries
	}

	private fun cachedRecipeFile(
		cacheDir: File,
		entry: CatalogEntry,
	): File {
		val safeId = entry.id.replace(Regex("[^A-Za-z0-9._-]"), "_")
		return File(cacheDir, "$safeId-v${entry.version}.json")
	}

	private fun validatedFileName(file: String): String {
		require(file.isNotBlank())
		require(".." !in file && '/' !in file && '\\' !in file && "://" !in file) {
			"Recipe catalog contains an invalid file name."
		}
		return file
	}

	private fun cacheDir(context: Context): File {
		return File(context.filesDir, "remote-recipes")
	}

	private fun writeAtomic(
		target: File,
		content: String,
	) {
		target.parentFile?.mkdirs()
		val temporary = File(target.parentFile, target.name + ".tmp")
		temporary.writeText(content)
		if (!temporary.renameTo(target)) {
			target.writeText(content)
			temporary.delete()
		}
	}

	private fun pruneOldRecipeFiles(
		cacheDir: File,
		entries: List<CatalogEntry>,
	) {
		val keep = entries
			.map { cachedRecipeFile(cacheDir, it).name }
			.toSet() + "index.json"

		cacheDir.listFiles()
			.orEmpty()
			.filter { it.isFile && it.name !in keep && !it.name.endsWith(".tmp") }
			.forEach(File::delete)
	}

	private fun downloadUtf8(url: String): String {
		val connection = (URL(url).openConnection() as HttpURLConnection).apply {
			connectTimeout = CONNECT_TIMEOUT_MS
			readTimeout = READ_TIMEOUT_MS
			requestMethod = "GET"
			setRequestProperty("Accept", "application/json")
			setRequestProperty("User-Agent", "CookCue/${BuildConfig.VERSION_NAME}")
		}

		return try {
			val status = connection.responseCode
			require(status in 200..299) {
				"HTTP $status while downloading recipe data."
			}
			connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
		} finally {
			connection.disconnect()
		}
	}
}
