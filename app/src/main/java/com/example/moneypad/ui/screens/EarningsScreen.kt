package com.example.moneypad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneypad.data.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarningsScreen(viewModel: EarningsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showUpgradeDialog by remember { mutableStateOf(false) }
    var showUpgradeSuccess by remember { mutableStateOf<String?>(null) }

    val isDark = MaterialTheme.colorScheme.background == Color(0xFF121212)
    val balanceColor = if (isDark) Color(0xFFFFD700) else Color(0xFF6200EE)

    val conversionRate = 60.0
    val coinToPhpRate = 0.01 // 100 coins = 1 peso => 1 coin = 0.01 peso

    val authorUsd = uiState.user?.authorIncome ?: 0.0
    val authorPhp = authorUsd * conversionRate

    val readerCoins = uiState.user?.readerCoins ?: 0
    val readerPhp = readerCoins * coinToPhpRate
    val readerUsd = readerPhp / conversionRate

    val totalUsd = authorUsd + readerUsd
    val totalPhp = authorPhp + readerPhp

    val uniqueViews = uiState.myPublishedStories.sumOf { it.uniqueViews }
    val repeatedViews = uiState.myPublishedStories.sumOf { it.repeatedViews }

    val clipboard = LocalClipboardManager.current
    val appLink = "https://moneypad.app/join/${uiState.user?.username}"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Earnings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Balance Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Current Balance",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$${String.format("%.2f", totalUsd)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = balanceColor
                        )
                        Text(
                            text = "≈ ₱${String.format("%.2f", totalPhp)}",
                            fontSize = 16.sp,
                            color = balanceColor.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Categories
            item {
                Text("Earning Categories", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            // Author Income
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Create, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Author Income", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Earn $0.03 (₱1.80) for every 100 unique views on your published stories.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Income", fontSize = 12.sp, color = Color.Gray)
                                Text("$${String.format("%.3f", authorUsd)} (₱${String.format("%.2f", authorPhp)})", fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Unique Views", fontSize = 12.sp, color = Color.Gray)
                                Text("$uniqueViews", fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Repeated Views", fontSize = 12.sp, color = Color.Gray)
                                Text("$repeatedViews", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Reader Coins
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reader Coins", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Earn coins by reading! 100 Coins = ₱1.00",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Coins", fontSize = 12.sp, color = Color.Gray)
                                Text("%,d".format(readerCoins), fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("PHP Value", fontSize = 12.sp, color = Color.Gray)
                                Text("₱${String.format("%.2f", readerPhp)}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Invitations
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color(0xFFE91E63))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Invite & Earn Rewards", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Get more coins when your friends read! 📖✨\n" +
                            "• 📖 Reads 5 pages = 15 Coins\n" +
                            "• 📖 Reads 15 pages = 30 Coins\n" +
                            "• 📖 Reads 25 pages = 50 Coins\n" +
                            "• 📖 Reads 40 pages = 80 Coins\n" +
                            "• 📖 Reads 80 pages = 120 Coins\n" +
                            "• 📖 Reads 110 pages = 180 Coins\n" +
                            "(Total of 4.75 pesos/reader)",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "💡 Tip: The more they read, the more you earn! Invite your friends now! 🚀",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("✨ Author Referral Bonus ✨", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Earn 5% Commission FOREVER! 💸\n" +
                            "Get a share every time your invited author withdraws their earnings!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = appLink,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { clipboard.setText(AnnotatedString(appLink)) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy link", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // Actions
            item {
                Text("Actions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { showWithdrawDialog = true },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Withdraw Funds")
                    }
                    OutlinedButton(
                        onClick = { showUpgradeDialog = true },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upgrade Plan")
                    }
                }
            }

            // Transactions
            item {
                Text("Transaction History", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            
            if (uiState.transactions.isEmpty()) {
                item {
                    Text("No transactions yet.", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                }
            } else {
                items(uiState.transactions) { tx ->
                    TransactionItem(transaction = tx)
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showWithdrawDialog) {
        WithdrawDialog(
            onDismiss = { showWithdrawDialog = false },
            onConfirm = { amount, method, info, source ->
                viewModel.withdraw(amount, method, info, source)
                showWithdrawDialog = false
            },
            authorPhp = authorPhp,
            readerPhp = readerPhp
        )
    }

    if (showUpgradeSuccess != null) {
        AlertDialog(
            onDismissRequest = { showUpgradeSuccess = null },
            title = { Text("Success") },
            text = { Text("Successfully upgraded to ${showUpgradeSuccess} plan!") },
            confirmButton = {
                Button(onClick = { showUpgradeSuccess = null }) {
                    Text("OK")
                }
            }
        )
    }

    if (showUpgradeDialog) {
        UpgradePlanDialog(
            onDismiss = { showUpgradeDialog = false },
            onConfirm = { plan -> 
                showUpgradeDialog = false
                showUpgradeSuccess = plan
            }
        )
    }
}

@Composable
fun UpgradePlanDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var selectedPlan by remember { mutableStateOf("Weekly") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upgrade Plan") },
        text = {
            Column {
                Text("Select a subscription plan:", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    RadioButton(selected = selectedPlan == "Weekly", onClick = { selectedPlan = "Weekly" })
                    Text("Weekly - ₱50.00")
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    RadioButton(selected = selectedPlan == "Monthly", onClick = { selectedPlan = "Monthly" })
                    Text("Monthly - ₱180.00")
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    RadioButton(selected = selectedPlan == "Yearly", onClick = { selectedPlan = "Yearly" })
                    Text("Yearly - ₱1800.00")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedPlan) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun WithdrawDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String, String) -> Unit,
    authorPhp: Double,
    readerPhp: Double
) {
    var source by remember { mutableStateOf("AUTHOR") } // "AUTHOR" or "READER"
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("GCash") }
    var accountInfo by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showProcessingAlert by remember { mutableStateOf(false) }

    if (showProcessingAlert) {
        AlertDialog(
            onDismissRequest = { showProcessingAlert = false },
            title = { Text("Request Submitted") },
            text = { Text("Your withdrawal request has been received and will be processed within 2 to 14 days.") },
            confirmButton = {
                Button(onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    onConfirm(amt, method, accountInfo, source)
                    showProcessingAlert = false
                }) { Text("OK") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Withdraw Funds") },
        text = {
            Column {
                Text("Select Source:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = source == "AUTHOR", onClick = { source = "AUTHOR"; errorMessage = null })
                    Text("Author Income", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(selected = source == "READER", onClick = { source = "READER"; errorMessage = null })
                    Text("Reader Coins", fontSize = 13.sp)
                }
                
                val currentBalance = if (source == "AUTHOR") authorPhp else readerPhp
                val minWithdrawal = if (source == "AUTHOR") 59.95 else 50.0
                
                Text(
                    text = "Available: ₱${String.format("%.2f", currentBalance)}",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Min withdrawal: ₱${String.format("%.2f", minWithdrawal)}",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Select Method:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = method == "GCash", onClick = { method = "GCash"; errorMessage = null })
                        Text("GCash", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        RadioButton(selected = method == "PayMaya", onClick = { method = "PayMaya"; errorMessage = null })
                        Text("PayMaya", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = method == "PayPal", onClick = { method = "PayPal"; errorMessage = null })
                        Text("PayPal", fontSize = 13.sp)
                    }
                }
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { 
                        amount = it
                        errorMessage = null
                    },
                    label = { Text("Amount (PHP)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage != null && errorMessage?.contains("Amount") == true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = accountInfo,
                    onValueChange = { 
                        if (method == "PayPal") {
                            accountInfo = it
                            errorMessage = null
                        } else {
                            val digitsOnly = it.filter { char -> char.isDigit() }
                            if (digitsOnly.length <= 11) {
                                accountInfo = digitsOnly
                                errorMessage = null
                            }
                        }
                    },
                    label = { 
                        Text(
                            when (method) {
                                "GCash" -> "GCash Number"
                                "PayMaya" -> "PayMaya Number"
                                else -> "PayPal Email"
                            }
                        ) 
                    },
                    placeholder = { 
                        Text(
                            if (method == "PayPal") "e.g. email@example.com" else "e.g. 09123456789"
                        ) 
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (method == "PayPal") KeyboardType.Email else KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage != null && (errorMessage?.contains("Number") == true || errorMessage?.contains("Email") == true)
                )
                
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                val phoneRegex = "^09\\d{9}$".toRegex()
                val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
                val currentBalance = if (source == "AUTHOR") authorPhp else readerPhp
                val minWithdrawal = if (source == "AUTHOR") 59.95 else 50.0
                
                when {
                    amt < minWithdrawal -> errorMessage = "Minimum withdrawal is ₱${String.format("%.2f", minWithdrawal)}"
                    amt > currentBalance -> errorMessage = "Insufficient balance"
                    method != "PayPal" && !accountInfo.matches(phoneRegex) -> errorMessage = "Enter a valid 11-digit number starting with 09"
                    method == "PayPal" && !accountInfo.matches(emailRegex) -> errorMessage = "Enter a valid Email address"
                    else -> {
                        showProcessingAlert = true
                    }
                }
            }) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun TransactionItem(transaction: Transaction) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(transaction.method, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(transaction.accountInfo, color = Color.Gray, fontSize = 12.sp)
                Text(dateFormat.format(Date(transaction.timestamp)), color = Color.Gray, fontSize = 12.sp)
            }
            Text(
                text = "-₱${String.format("%.2f", transaction.amount)}",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
