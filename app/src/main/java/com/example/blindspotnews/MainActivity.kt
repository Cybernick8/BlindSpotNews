package com.example.blindspotnews

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.blindspotnews.ui.ScreenOne
import com.example.blindspotnews.ui.ScreenOutput
import com.example.blindspotnews.ui.ScreenTwo
import com.example.blindspotnews.ui.ProfileScreen
import com.google.firebase.FirebaseApp
import com.example.blindspotnews.ui.AnalysisScreen
import com.example.blindspotnews.ui.LoginScreen
import com.example.blindspotnews.ui.AppColors


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        setContent {
            // Force a light background for visibility with black text
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = AppColors.background()
            ) {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            LoginScreen(navController)
        }

        composable("analysis_test") {
            AnalysisScreen(navController)
        }

        // Original screens we were testing with
        composable("screen_one") { ScreenOne(navController) }
        composable("screen_two") { ScreenTwo(navController) }
        composable("screen_output") { ScreenOutput(navController) }
        composable("profile") { ProfileScreen(navController) }
    }
}