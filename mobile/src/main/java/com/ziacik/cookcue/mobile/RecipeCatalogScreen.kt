package com.ziacik.cookcue.mobile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ziacik.cookcue.core.model.Recipe
import com.ziacik.cookcue.core.model.ScheduleMode
import java.util.Locale

/**
 * The catalog is a destination in its own right, not a collection of buttons
 * above the recipe details. Its content comes exclusively from the online catalog.
 */
@Composable
internal fun RecipeCatalogScreen(
	recipes: List<Recipe>,
	catalogLoading: Boolean,
	catalogError: Boolean,
	onSelect: (String) -> Unit,
	onRefresh: () -> Unit,
) {
	var query by rememberSaveable { mutableStateOf("") }
	val visibleRecipes = recipes
		.filter { recipe ->
			query.isBlank() ||
				recipe.title.contains(query.trim(), ignoreCase = true) ||
				recipe.description.contains(query.trim(), ignoreCase = true)
		}
		.sortedBy { it.title.lowercase(Locale.getDefault()) }

	Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
		LazyColumn(
			modifier = Modifier.fillMaxSize().padding(padding),
			contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 36.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			item {
				Column(modifier = Modifier.padding(bottom = 12.dp)) {
					Text(
						text = "CookCue",
						style = MaterialTheme.typography.headlineLarge,
						fontWeight = FontWeight.Bold,
						color = MaterialTheme.colorScheme.primary,
					)
					Text(
						text = "Čo dnes pripravíme?",
						style = MaterialTheme.typography.titleMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						modifier = Modifier.padding(top = 5.dp),
					)
				}
			}
			item {
				OutlinedTextField(
					value = query,
					onValueChange = { query = it },
					placeholder = { Text("Hľadať medzi receptami") },
					singleLine = true,
					shape = RoundedCornerShape(16.dp),
					modifier = Modifier.fillMaxWidth(),
				)
			}
			item {
				Row(
					modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					Text(
						text = if (query.isBlank()) "RECEPTY · ${recipes.size}" else "VÝSLEDKY · ${visibleRecipes.size}",
						style = MaterialTheme.typography.labelMedium,
						fontWeight = FontWeight.Bold,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						modifier = Modifier.weight(1f),
					)
					if (catalogError || catalogLoading) {
						TextButton(onClick = onRefresh, enabled = !catalogLoading) {
							Text(if (catalogLoading) "Načítavam…" else "Obnoviť")
						}
					}
				}
			}
			if (catalogError && recipes.isNotEmpty()) {
				item {
					Text(
						text = "Aktualizácia sa nepodarila. Zobrazené sú uložené recepty.",
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.error,
					)
				}
			}
			if (recipes.isEmpty()) {
				item {
					Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
						Text(
							text = if (catalogLoading) "Načítavam online katalóg…" else if (catalogError) {
								"Nepodarilo sa načítať katalóg. Skontroluj internetové pripojenie."
							} else {
								"Katalóg je zatiaľ prázdny."
							},
							style = MaterialTheme.typography.bodyLarge,
						)
						if (!catalogLoading) {
							TextButton(onClick = onRefresh) { Text("Skúsiť znova") }
						}
					}
				}
			} else if (visibleRecipes.isEmpty()) {
				item {
					Text(
						text = "Taký recept tu zatiaľ nie je.",
						style = MaterialTheme.typography.bodyLarge,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						modifier = Modifier.padding(vertical = 18.dp),
					)
				}
			}
			items(visibleRecipes, key = { it.id }) { recipe ->
				RecipeCatalogItem(recipe = recipe, onClick = { onSelect(recipe.id) })
			}
		}
	}
}

@Composable
private fun RecipeCatalogItem(
	recipe: Recipe,
	onClick: () -> Unit,
) {
	val itinerary = recipe.scheduleMode == ScheduleMode.ITINERARY
	Surface(
		modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
		shape = RoundedCornerShape(20.dp),
		color = MaterialTheme.colorScheme.surface,
		border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
	) {
		Row(
			modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
				Text(
					text = if (itinerary) "ITINERÁR" else "VARENIE",
					style = MaterialTheme.typography.labelSmall,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.secondary,
				)
				Text(
					text = recipe.title,
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.SemiBold,
					color = MaterialTheme.colorScheme.onSurface,
				)
				if (recipe.description.isNotBlank()) {
					Text(
						text = recipe.description,
						maxLines = 2,
						overflow = TextOverflow.Ellipsis,
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
				}
				Text(
					text = "${recipe.tasks.size} krokov" +
						if (!itinerary && recipe.servings > 0) "  ·  ${recipe.servings} porcií" else "",
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.padding(top = 3.dp),
				)
			}
			Spacer(Modifier.width(12.dp))
			Text(
				text = "›",
				style = MaterialTheme.typography.headlineMedium,
				color = MaterialTheme.colorScheme.primary,
			)
		}
	}
}
