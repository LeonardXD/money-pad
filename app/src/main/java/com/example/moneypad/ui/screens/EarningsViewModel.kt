package com.example.moneypad.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneypad.data.MoneyPadRepository
import com.example.moneypad.data.model.Story
import com.example.moneypad.data.model.Transaction
import com.example.moneypad.data.model.User
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EarningsUiState(
    val user: User? = null,
    val transactions: List<Transaction> = emptyList(),
    val myPublishedStories: List<Story> = emptyList(),
    val isWithdrawing: Boolean = false,
    val error: String? = null
)

class EarningsViewModel(private val repository: MoneyPadRepository) : ViewModel() {

    val uiState: StateFlow<EarningsUiState> = combine(
        repository.getUser(repository.currentUserId),
        repository.getTransactions(repository.currentUserId),
        repository.getPublishedStoriesByAuthor(repository.currentUserId)
    ) { user, transactions, myPublishedStories ->
        EarningsUiState(user = user, transactions = transactions, myPublishedStories = myPublishedStories)
    }.stateIn(viewModelScope, SharingStarted.Lazily, EarningsUiState())

    // source is either "AUTHOR" or "READER"
    fun withdraw(amount: Double, method: String, accountInfo: String, source: String) {
        viewModelScope.launch {
            if (source == "AUTHOR" && amount < 59.95) return@launch
            if (source == "READER") {
                val minWithdrawal = when (method) {
                    "PayPal" -> 30.0
                    "PayMaya" -> 40.0
                    "GCash" -> 50.0
                    else -> 50.0
                }
                if (amount < minWithdrawal) return@launch
            }
            
            // Deduct balance and create transaction
            repository.withdraw(amount, method, accountInfo)
        }
    }
}
