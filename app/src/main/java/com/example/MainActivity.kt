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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.LandingScreen
import com.example.ui.screens.LandownerDashboardScreen
import com.example.ui.screens.ParkerDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ParkFlowViewModel

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
                    ParkFlowApp()
                }
            }
        }
    }
}

@Composable
fun ParkFlowApp() {
    val navController = rememberNavController()
    val viewModel: ParkFlowViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "landing"
    ) {
        composable("landing") {
            LandingScreen(
                onNavigateToAuth = {
                    navController.navigate("auth")
                }
            )
        }

        composable("auth") {
            AuthScreen(
                viewModel = viewModel,
                onAuthSuccess = {
                    val user = viewModel.currentUser.value
                    if (user != null) {
                        if (user.role == "PARKER") {
                            navController.navigate("parker_dashboard") {
                                popUpTo("landing") { inclusive = false }
                            }
                        } else {
                            navController.navigate("owner_dashboard") {
                                popUpTo("landing") { inclusive = false }
                            }
                        }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("parker_dashboard") {
            ParkerDashboardScreen(
                viewModel = viewModel,
                onLogout = {
                    navController.navigate("landing") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("owner_dashboard") {
            LandownerDashboardScreen(
                viewModel = viewModel,
                onLogout = {
                    navController.navigate("landing") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
