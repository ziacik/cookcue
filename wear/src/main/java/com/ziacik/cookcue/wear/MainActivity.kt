package com.ziacik.cookcue.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.ziacik.cookcue.core.model.TaskKind
import com.ziacik.cookcue.core.recipes.BeanSoupRecipe
import com.ziacik.cookcue.core.scheduler.Scheduler

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContent {
			MaterialTheme {
				val schedule = remember { Scheduler().schedule(BeanSoupRecipe.recipe) }
				val interactiveSteps = remember(schedule) {
					schedule.filter {
						it.task.kind == TaskKind.EVENT ||
							(
								it.task.kind == TaskKind.ACTIVE &&
									it.task.resources.any { resource -> resource.resource == "cook" }
							)
					}
				}
				var currentIndex by remember { mutableStateOf(0) }
				val currentTask = interactiveSteps[currentIndex]

				Column(
					modifier = Modifier
						.fillMaxSize()
						.padding(20.dp),
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.Center,
				) {
					Text("CookCue")
					Text(currentTask.task.title)
					Text(currentTask.task.instruction)
					Row(
						horizontalArrangement = Arrangement.spacedBy(6.dp),
						verticalAlignment = Alignment.CenterVertically,
					) {
						Button(
							onClick = { currentIndex-- },
							enabled = currentIndex > 0,
						) {
							Text("←")
						}
						Button(
							onClick = {
								if (currentIndex < interactiveSteps.lastIndex) {
									currentIndex++
								}
							},
							enabled = currentIndex < interactiveSteps.lastIndex,
						) {
							Text(
								if (currentTask.task.kind == TaskKind.EVENT) {
									currentTask.task.actionLabel ?: "✓"
								} else {
									"✓"
								}
							)
						}
						Button(
							onClick = { currentIndex++ },
							enabled = currentIndex < interactiveSteps.lastIndex,
						) {
							Text("→")
						}
					}
				}
			}
		}
	}
}
