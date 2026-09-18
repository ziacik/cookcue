package com.ziacik.cookcue.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.ziacik.cookcue.core.recipes.BeanSoupRecipe
import com.ziacik.cookcue.core.scheduler.Scheduler

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContent {
			MaterialTheme {
				val firstTask = Scheduler().schedule(BeanSoupRecipe.recipe).first()

				Column(
					modifier = Modifier
						.fillMaxSize()
						.padding(20.dp),
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.Center,
				) {
					Text("CookCue")
					Text(firstTask.task.title)
					Text(firstTask.task.instruction)
					Button(onClick = {}) {
						Text("HOTOVO")
					}
				}
			}
		}
	}
}
