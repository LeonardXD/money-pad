package com.example.moneypad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.NavType
import com.example.moneypad.data.AppDatabase
import com.example.moneypad.data.MoneyPadRepository
import com.example.moneypad.ui.ViewModelFactory
import com.example.moneypad.ui.auth.LoginScreen
import com.example.moneypad.ui.auth.SignupScreen
import com.example.moneypad.ui.main.MainScreen
import com.example.moneypad.ui.theme.MoneyPadTheme
import com.example.moneypad.ui.theme.ThemeViewModel
import androidx.core.view.WindowCompat
import com.example.moneypad.ads.AdManager
import com.example.moneypad.data.model.User
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        AdManager.initialize(this)

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                AdManager.showAppOpenAdIfAvailable(this@MainActivity)
            }
        })

        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val database = remember { AppDatabase.getDatabase(context) }
            val repository = remember { MoneyPadRepository(context, database.moneyPadDao()) }
            val factory = remember { ViewModelFactory(repository) }
            val themeViewModel: ThemeViewModel = viewModel(factory = factory)
            val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()

            val currentUser by remember(repository.currentUserId) { repository.getCurrentUser() }.collectAsState(initial = null)

            LaunchedEffect(currentUser) {
                AdManager.setUser(currentUser)
            }

            // Determine start destination
            val startDestination = remember(currentUser) {
                if (repository.hasActiveSession()) {
                    if (currentUser != null && currentUser?.onboardingCompleted == false) {
                        "onboarding"
                    } else {
                        "main"
                    }
                } else {
                    "login"
                }
            }

            LaunchedEffect(Unit) {
                repository.initUser()
            }

            val timeUntilNextAd by AdManager.timeUntilNextAd.collectAsState()
            LaunchedEffect(timeUntilNextAd) {
                if (timeUntilNextAd <= 0) {
                    AdManager.checkAndShowTimerAd(this@MainActivity)
                }
            }

            MoneyPadTheme(darkTheme = isDarkTheme) {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    if (repository.hasActiveSession() && currentUser == null) {
                         // Loading state or just show a blank screen while loading user
                    } else {
                         MoneyPadApp(factory, themeViewModel, startDestination)
                    }
                }
            }
        }
    }
}

@Composable
fun MoneyPadApp(factory: ViewModelFactory, themeViewModel: ThemeViewModel, startDestination: String) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                onNavigateToSignup = { navController.navigate("signup") },
                onLoginSuccess = { requiresOnboarding ->
                    val dest = if (requiresOnboarding) "onboarding" else "main"
                    navController.navigate(dest) {
                        popUpTo("login") { inclusive = true }
                    }
                },
                viewModel = viewModel(factory = factory)
            )
        }
        composable(
            route = "signup?referrer={referrer}",
            deepLinks = listOf(
                navDeepLink { uriPattern = "https://moneypad.app/join/{referrer}" },
                navDeepLink { uriPattern = "moneypad://join/{referrer}" }
            ),
            arguments = listOf(navArgument("referrer") { type = NavType.StringType; nullable = true })
        ) { backStackEntry ->
            val viewModel: com.example.moneypad.ui.auth.SignupViewModel = viewModel(factory = factory)
            val referrer = backStackEntry.arguments?.getString("referrer")

            LaunchedEffect(referrer) {
                if (!referrer.isNullOrBlank() && viewModel.uiState.value.referrerUsername.isBlank()) {
                    viewModel.onReferrerUsernameChange(referrer)
                }
            }

            SignupScreen(
                onNavigateToLogin = { navController.navigate("login") },
                onSignupSuccess = {
                    navController.navigate("login") {
                        popUpTo("signup?referrer={referrer}") { inclusive = true }
                    }
                },
                viewModel = viewModel
            )
        }
        composable("onboarding") {
            com.example.moneypad.ui.auth.OnboardingScreen(
                onOnboardingComplete = {
                    navController.navigate("main") {
                        popUpTo("onboarding") { inclusive = true }
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
