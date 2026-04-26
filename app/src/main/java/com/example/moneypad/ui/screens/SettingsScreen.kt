package com.example.moneypad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneypad.data.model.User
import com.example.moneypad.ui.screens.STORY_GENRES
import com.example.moneypad.ui.theme.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ProfileViewModel,
    themeViewModel: ThemeViewModel,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val user by viewModel.user.collectAsState()
    val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        user?.let { u ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item { ProfileInfoSection(u, viewModel) }
                item { PasswordSection(viewModel) }
                item { AppearanceSection(isDarkTheme, themeViewModel) }
                item { InviteSection(u) }
                item { DownloadsSection() }
                item { LogoutSection(viewModel, onLogout) }
            }
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

// ── Profile Info ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileInfoSection(user: User, viewModel: ProfileViewModel) {
    var username by remember(user.username) { mutableStateOf(user.username) }
    var birthday by remember(user.birthday) { mutableStateOf(user.birthday) }
    var gender by remember(user.gender) { mutableStateOf(user.gender) }
    var selectedGenres by remember(user.preferredGenres) {
        mutableStateOf(
            if (user.preferredGenres.isBlank()) setOf()
            else user.preferredGenres.split(",").map { it.trim() }.toSet()
        )
    }
    var genderExpanded by remember { mutableStateOf(false) }
    var genreExpanded by remember { mutableStateOf(false) }
    var usernameError by remember { mutableStateOf<String?>(null) }

    val genders = listOf("Male", "Female", "Non-binary", "Prefer not to say")
    val settingsResult by viewModel.settingsResult.collectAsState(initial = null)

    LaunchedEffect(settingsResult) {
        settingsResult?.let {
            if (it.isFailure) usernameError = it.exceptionOrNull()?.message
            else usernameError = null
        }
    }

    SettingsCard(title = "Profile Information", icon = Icons.Default.Person) {
        // Email — read-only
        OutlinedTextField(
            value = user.email,
            onValueChange = {},
            label = { Text("Email") },
            readOnly = true,
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Username — editable
        OutlinedTextField(
            value = username,
            onValueChange = { username = it; usernameError = null },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = usernameError != null,
            supportingText = usernameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Birthday
        OutlinedTextField(
            value = birthday,
            onValueChange = { birthday = it },
            label = { Text("Birthday (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text("e.g. 2000-01-15") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Gender dropdown
        ExposedDropdownMenuBox(expanded = genderExpanded, onExpandedChange = { genderExpanded = it }) {
            OutlinedTextField(
                value = gender,
                onValueChange = {},
                readOnly = true,
                label = { Text("Gender") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                genders.forEach { g ->
                    DropdownMenuItem(text = { Text(g) }, onClick = { gender = g; genderExpanded = false })
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Preferred genres (max 5)
        ExposedDropdownMenuBox(expanded = genreExpanded, onExpandedChange = { genreExpanded = it }) {
            OutlinedTextField(
                value = if (selectedGenres.isEmpty()) "" else selectedGenres.joinToString(", "),
                onValueChange = {},
                readOnly = true,
                label = { Text("Preferred Genres (up to 5)") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genreExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Select genres for recommendations") }
            )
            ExposedDropdownMenu(expanded = genreExpanded, onDismissRequest = { genreExpanded = false }) {
                STORY_GENRES.forEach { genre ->
                    val checked = genre in selectedGenres
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = checked, onCheckedChange = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(genre)
                            }
                        },
                        onClick = {
                            selectedGenres = if (checked) {
                                selectedGenres - genre
                            } else if (selectedGenres.size < 5) {
                                selectedGenres + genre
                            } else selectedGenres
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.saveSettings(
                    username.trim(),
                    birthday.trim(),
                    gender,
                    selectedGenres.joinToString(",")
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Save Changes")
        }
    }
}

// ── Password ──────────────────────────────────────────────────────────────────

@Composable
private fun PasswordSection(viewModel: ProfileViewModel) {
    var current by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var currentVisible by remember { mutableStateOf(false) }
    var newVisible by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    val passwordResult by viewModel.passwordResult.collectAsState(initial = null)

    LaunchedEffect(passwordResult) {
        passwordResult?.let {
            if (it.isSuccess) {
                successMsg = "Password changed successfully"
                current = ""; newPass = ""; confirm = ""
                errorMsg = null
            } else {
                errorMsg = it.exceptionOrNull()?.message ?: "Failed"
                successMsg = null
            }
        }
    }

    SettingsCard(title = "Change Password", icon = Icons.Default.Lock) {
        if (errorMsg != null) {
            Text(errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (successMsg != null) {
            Text(successMsg!!, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = current,
            onValueChange = { current = it; errorMsg = null },
            label = { Text("Current Password") },
            visualTransformation = if (currentVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { currentVisible = !currentVisible }) {
                    Icon(
                        if (currentVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            isError = errorMsg != null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = newPass,
            onValueChange = { newPass = it },
            label = { Text("New Password") },
            visualTransformation = if (newVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { newVisible = !newVisible }) {
                    Icon(
                        if (newVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it },
            label = { Text("Confirm New Password") },
            visualTransformation = PasswordVisualTransformation(),
            isError = confirm.isNotBlank() && confirm != newPass,
            supportingText = if (confirm.isNotBlank() && confirm != newPass) {
                { Text("Passwords don't match", color = MaterialTheme.colorScheme.error) }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                when {
                    current.isBlank() || newPass.isBlank() ->
                        errorMsg = "All fields are required"
                    newPass.length !in 8..16 ->
                        errorMsg = "Password must be 8–16 characters"
                    !newPass.any { it.isUpperCase() } || !newPass.any { it.isLowerCase() } ||
                            !newPass.any { it.isDigit() } || !newPass.any { !it.isLetterOrDigit() } ->
                        errorMsg = "Password needs uppercase, lowercase, number & symbol"
                    newPass != confirm ->
                        errorMsg = "Passwords do not match"
                    else -> viewModel.changePassword(current, newPass)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) { Text("Update Password") }
    }
}

// ── Appearance ────────────────────────────────────────────────────────────────

@Composable
private fun AppearanceSection(isDarkTheme: Boolean, themeViewModel: ThemeViewModel) {
    SettingsCard(title = "Appearance", icon = Icons.Default.Palette) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Dark Mode", fontSize = 15.sp)
            Switch(
                checked = isDarkTheme,
                onCheckedChange = { themeViewModel.toggleTheme() }
            )
        }
    }
}

// ── Invite / Referral ─────────────────────────────────────────────────────────

@Composable
private fun InviteSection(user: User) {
    val clipboard = LocalClipboardManager.current
    val appLink = "https://moneypad.app/join/${user.username}"

    val msg1 = "Do you like to earn while writing and reading stories at the same time? Join me now on $appLink"
    val msg2 = "Hey! I found an app that makes you earn while writing and reading stories. Join my earning journey on $appLink"

    SettingsCard(title = "Invite Users", icon = Icons.Default.PersonAdd) {
        Text(
            "Share to Earn More! Invite users to maximize your earnings.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "You'll get a 10% share of their earnings whenever they withdraw.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Your referral: @${user.username}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { clipboard.setText(AnnotatedString(user.username)) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy username", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Total invites: ${user.referralCount}",
            fontSize = 13.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { clipboard.setText(AnnotatedString(msg1)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Copy Invite Message 1", fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { clipboard.setText(AnnotatedString(msg2)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Copy Invite Message 2", fontSize = 13.sp)
        }
    }
}

// ── Downloads ─────────────────────────────────────────────────────────────────

@Composable
private fun DownloadsSection() {
    SettingsCard(title = "Downloads", icon = Icons.Default.Download) {
        Text(
            "Downloaded stories for offline reading will appear here.",
            fontSize = 14.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.OfflinePin,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("No downloads yet", color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}

// ── Logout ────────────────────────────────────────────────────────────────────

@Composable
private fun LogoutSection(viewModel: ProfileViewModel, onLogout: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirm = false
                        viewModel.logout()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Logout") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Button(
        onClick = { showConfirm = true },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
    ) {
        Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Logout", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }

    Spacer(modifier = Modifier.height(24.dp))
}

// ── Shared card wrapper ───────────────────────────────────────────────────────

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}