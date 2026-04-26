package com.example.moneypad.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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

    val isDark = MaterialTheme.colorScheme.background == Color(0xFF121212)
    val balanceColor = if (isDark) Color(0xFFFFD700) else Color(0xFF6200EE)

    val conversionRate = 60.0
    val coinToPhpRate = 0.001

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
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$${String.format("%.3f", totalUsd)}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = balanceColor
                        )
                        Text(
                            text = "≈ ₱${String.format("%.2f", totalPhp)}",
                            fontSize = 16.sp,
                            color = balanceColor.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Author's Income", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "$${String.format("%.3f", authorUsd)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            VerticalDivider(
                                modifier = Modifier.height(36.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Reader's Coins", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "%,d coins".format(readerCoins),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Withdrawal Button
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Withdrawal",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Available Methods: GCash, PayMaya",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showWithdrawDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = totalPhp > 0
                    ) {
                        Text("Withdraw Funds")
                    }
                }
            }

            // Instructions Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "📤 Withdrawal Instructions",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        listOf(
                            "1. Tap the 'Withdraw Funds' button above.",
                            "2. Enter the amount you wish to withdraw.",
                            "3. Choose your preferred method (GCash or PayMaya).",
                            "4. Enter your account details correctly.",
                            "5. Confirm the transaction and wait for processing."
                        ).forEach { step ->
                            Text(
                                text = step,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Notice Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "⏳ Processing Time Notice",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "• Expected arrival: 2 to 14 days",
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "🕒 Withdrawal Process Details:",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "• Withdrawals from 6 AM to 3 PM will be processed within the same day (except weekends and holidays).",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "• Requests from 3 PM to 5 AM will be processed the next day.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "⚠️ All withdrawals are manually reviewed and processed. Delays may occur.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
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
            currentBalancePhp = totalPhp
        )
    }
}

@Composable
fun WithdrawDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String) -> Unit,
    currentBalancePhp: Double
) {
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("GCash") }
    var accountInfo by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showProcessingAlert by remember { mutableStateOf(false) }

    if (showProcessingAlert) {
        AlertDialog(
            onDismissRequest = { showProcessingAlert = false },
            title = { Text("Request Submitted") },
            text = { Text("Your withdrawal request has been received and will be processed within 2 to 14 days. Thank you for your patience!") },
            confirmButton = {
                Button(onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    onConfirm(amt, method, accountInfo)
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
                Text("Minimum withdrawal: ₱60.00 ($1.00)", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Select Method:", fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = method == "GCash", onClick = { method = "GCash"; errorMessage = null })
                    Text("GCash")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = method == "PayMaya", onClick = { method = "PayMaya"; errorMessage = null })
                    Text("PayMaya")
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
                        if (it.length <= 11) {
                            accountInfo = it
                            errorMessage = null
                        }
                    },
                    label = { Text(if (method == "GCash") "GCash Number (09xxxxxxxx)" else "PayMaya Number (09xxxxxxxx)") },
                    placeholder = { Text("09123456789") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage != null && errorMessage?.contains("Number") == true
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
                
                when {
                    amt < 60.0 -> errorMessage = "Minimum withdrawal is ₱60.00"
                    amt > currentBalancePhp -> errorMessage = "Insufficient balance"
                    !accountInfo.matches(phoneRegex) -> errorMessage = "Enter a valid 11-digit number starting with 09"
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