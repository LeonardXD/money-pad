package com.example.moneypad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneypad.data.model.Transaction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarningsScreen(viewModel: EarningsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showWithdrawDialog by remember { mutableStateOf(false) }

    // Colors
    val isDark = isSystemInDarkTheme()
    val balanceColor = if (isDark) Color(0xFFFFD700) else Color(0xFF6200EE)

    val conversionRate = 60.0 // USD to PHP
    val coinToPhpRate = 0.001 // 1000 coins = 1 PHP

    // Calculate dynamic totals for the UI based on user's db fields
    val authorUsd = uiState.user?.authorIncome ?: 0.0
    val authorPhp = authorUsd * conversionRate

    val readerCoins = uiState.user?.readerCoins ?: 0
    val readerPhp = readerCoins * coinToPhpRate
    val readerUsd = readerPhp / conversionRate

    val totalUsd = authorUsd + readerUsd
    val totalPhp = authorPhp + readerPhp

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Balance Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Current Balance: $${String.format("%.3f", totalUsd)} = ${String.format("%.3f", totalPhp)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = balanceColor
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Author's Income: $${String.format("%.3f", authorUsd)} = ${String.format("%.3f", authorPhp)}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Reader's Income: %,d coin = ${String.format("%.3f", readerPhp)}".format(readerCoins),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Withdrawal Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Withdrawal", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("Available Methods: GCash, PayMaya", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showWithdrawDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = totalPhp > 0
                    ) {
                        Text("Withdraw Funds")
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Instructions
                    Text("📤 Withdrawal Instructions", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("1. Tap the 'Withdraw Funds' button above.", fontSize = 14.sp, color = Color.Gray)
                    Text("2. Enter the amount you wish to withdraw.", fontSize = 14.sp, color = Color.Gray)
                    Text("3. Choose your preferred withdrawal method (GCash or PayMaya).", fontSize = 14.sp, color = Color.Gray)
                    Text("4. Enter your account details correctly.", fontSize = 14.sp, color = Color.Gray)
                    Text("5. Confirm the transaction and wait for processing.", fontSize = 14.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Notice
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("⏳ Processing Time Notice", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("• Expected arrival: 2 to 14 days", fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("🕒 Withdrawal Process Details:", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("• Withdrawals from 6 AM to 3 PM will be processed within the same day (except weekends and holidays, which may vary).", fontSize = 14.sp)
                            Text("• Requests made from 3 PM to 5 AM will be processed the next day.", fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("⚠️ Delays may occur as all withdrawals are manually reviewed and processed.", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showWithdrawDialog) {
        WithdrawDialog(
            onDismiss = { showWithdrawDialog = false },
            onConfirm = { amount, method, info ->
                viewModel.withdraw(amount, method, info)
                showWithdrawDialog = false
            },
            currentBalance = totalPhp
        )
    }
}

@Composable
fun WithdrawDialog(onDismiss: () -> Unit, onConfirm: (Double, String, String) -> Unit, currentBalance: Double) {
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("GCash") }
    var accountInfo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Withdraw Funds") },
        text = {
            Column {
                Text("Select Method:", fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = method == "GCash", onClick = { method = "GCash" })
                    Text("GCash")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = method == "PayMaya", onClick = { method = "PayMaya" })
                    Text("PayMaya")
                }
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (PHP)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = accountInfo,
                    onValueChange = { accountInfo = it },
                    label = { Text(if (method == "GCash") "GCash Number" else "PayMaya Number") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0 && amt <= currentBalance && accountInfo.isNotBlank()) {
                        onConfirm(amt, method, accountInfo)
                    }
                }
            ) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
