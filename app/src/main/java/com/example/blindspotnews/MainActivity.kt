package com.example.blindspotnews

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.blindspotnews.ui.ScreenOne
import com.example.blindspotnews.ui.ScreenTwo

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "screen_one") {
        composable("screen_one") {
            ScreenOneWithButton(navController)
        }
        composable("screen_two") {
            ScreenTwoWithButton(navController)
        }
    }
}

@Composable
fun ScreenOneWithButton(navController: NavHostController) {
    Button(onClick = { navController.navigate("screen_two") }) {
        Text("Go to Screen Two")
    }
}

@Composable
fun ScreenTwoWithButton(navController: NavHostController) {
    Button(onClick = { navController.navigate("screen_one") }) {
        Text("Go back to Screen One")
    }
}
