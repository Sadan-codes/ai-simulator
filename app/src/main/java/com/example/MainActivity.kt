package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.MainViewModel
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InterviewReportScreen
import com.example.ui.screens.InterviewSetupScreen
import com.example.ui.screens.LiveInterviewScreen
import com.example.ui.screens.ResumeUploadScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    InterviewAppNavigation()
                }
            }
        }
    }
}

@Composable
fun InterviewAppNavigation() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateSetup = { navController.navigate("setup") },
                onNavigateResume = { navController.navigate("resume") },
                onNavigateHistory = { navController.navigate("history") },
                onNavigateReport = { sessionId -> navController.navigate("report/$sessionId") }
            )
        }

        composable("resume") {
            ResumeUploadScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("setup") {
            InterviewSetupScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onSessionCreated = { sessionId ->
                    navController.navigate("interview/$sessionId") {
                        popUpTo("setup") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "interview/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
            LiveInterviewScreen(
                viewModel = viewModel,
                sessionId = sessionId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateReport = { reportSessionId ->
                    navController.navigate("report/$reportSessionId") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = "report/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
            InterviewReportScreen(
                viewModel = viewModel,
                sessionId = sessionId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("history") {
            HistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateReport = { sessionId -> navController.navigate("report/$sessionId") }
            )
        }
    }
}
