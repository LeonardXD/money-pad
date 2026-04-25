package com.example.moneypad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.moneypad.ui.auth.LoginScreen
import com.example.moneypad.ui.auth.SignupScreen
import com.example.moneypad.ui.main.MainScreen
import com.example.moneypad.ui.theme.MoneyPadTheme

import androidx.compose.runtime.remember
import com.example.moneypad.data.AppDatabase
import com.example.moneypad.data.MoneyPadRepository
import com.example.moneypad.ui.ViewModelFactory
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.moneypad.ui.theme.ThemeViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val database = remember { AppDatabase.getDatabase(context) }
            val repository = remember { MoneyPadRepository(database.moneyPadDao()) }
            val factory = remember { ViewModelFactory(repository) }
            val themeViewModel: ThemeViewModel = viewModel(factory = factory)
            val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()
            
            LaunchedEffect(Unit) {
                repository.initUser()
            }

            MoneyPadTheme(darkTheme = isDarkTheme) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MoneyPadApp(factory, themeViewModel)
                }
            }
        }
    }
}

@Composable
fun MoneyPadApp(factory: ViewModelFactory, themeViewModel: ThemeViewModel) {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onNavigateToSignup = { navController.navigate("signup") },
                onLoginSuccess = { 
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                viewModel = viewModel(factory = factory)
            )
        }
        composable("signup") {
            SignupScreen(
                onNavigateToLogin = { navController.navigate("login") },
                onSignupSuccess = { 
                    navController.navigate("login") {
                        popUpTo("signup") { inclusive = true }
                    }
                },
                viewModel = viewModel(factory = factory)
            )
        }
        composable("main") {
            MainScreen(
                factory = factory,
                themeViewModel = themeViewModel,
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
