package com.example.blindspotnews

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.blindspotnews.ui.ScreenOne
import com.example.blindspotnews.ui.ScreenOutput
import com.example.blindspotnews.ui.ProfileScreen
import com.google.firebase.FirebaseApp
import com.example.blindspotnews.ui.AnalysisScreen
import com.example.blindspotnews.ui.LoginScreen
import com.example.blindspotnews.ui.AppColors
import androidx.navigation.navArgument
import android.content.Intent
import androidx.navigation.NavType
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue



class MainActivity : ComponentActivity() {

    private var sharedUrl by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        sharedUrl = extractSharedUrl(intent)

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = AppColors.background()
            ) {
                AppNavigation(sharedUrl = sharedUrl)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedUrl = extractSharedUrl(intent)
    }

    private fun extractSharedUrl(intent: Intent): String {
        if (intent.action != Intent.ACTION_SEND) return ""
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return ""
        val urlRegex = Regex("https?://\\S+")
        return urlRegex.find(text)?.value ?: text.trim()
    }
}

@Composable
fun AppNavigation(sharedUrl: String = "") {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            LoginScreen(navController = navController, sharedUrl = sharedUrl)
        }

        composable(
            "analysis_test?url={url}",
            arguments = listOf(navArgument("url") {
                type = NavType.StringType
                defaultValue = ""
            })
        ) { backStackEntry ->
            AnalysisScreen(
                navController = navController,
                autoUrl = backStackEntry.arguments?.getString("url") ?: ""
            )
        }

        composable("screen_one") { ScreenOne(navController) }
        composable("screen_output") { ScreenOutput(navController) }
        composable("profile") { ProfileScreen(navController) }
    }
}