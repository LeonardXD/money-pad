package com.example.moneypad.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Payments
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Explore : BottomNavItem("explore", "Explore", Icons.Default.Explore)
    object Library : BottomNavItem("library", "Library", Icons.AutoMirrored.Filled.LibraryBooks)
    object Write : BottomNavItem("write", "Write", Icons.Default.Create)
    object Earnings : BottomNavItem("earnings", "Earnings", Icons.Default.Payments)
    object Profile : BottomNavItem("profile", "Profile", Icons.Default.AccountCircle)
}

val bottomNavItems = listOf(
    BottomNavItem.Explore,
    BottomNavItem.Library,
    BottomNavItem.Write,
    BottomNavItem.Earnings,
    BottomNavItem.Profile
)
