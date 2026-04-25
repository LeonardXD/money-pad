package com.example.moneypad.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneypad.data.MoneyPadRepository
import com.example.moneypad.data.model.Transaction
import com.example.moneypad.data.model.User
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EarningsUiState(
    val user: User? = null,
    val transactions: List<Transaction> = emptyList(),
    val isWithdrawing: Boolean = false,
    val error: String? = null
)

class EarningsViewModel(private val repository: MoneyPadRepository) : ViewModel() {

    val uiState: StateFlow<EarningsUiState> = combine(
        repository.getUser(repository.currentUserId),
        repository.getTransactions(repository.currentUserId)
    ) { user, transactions ->
        EarningsUiState(user = user, transactions = transactions)
    }.stateIn(viewModelScope, SharingStarted.Lazily, EarningsUiState())

    fun withdraw(amount: Double, method: String, accountInfo: String) {
        viewModelScope.launch {
            if (amount < 0.10) {
                // Should be handled in UI validation too
                return@launch
            }
            val balance = uiState.value.user?.balance ?: 0.0
            if (amount > balance) {
                // Error handling
                return@launch
            }
            repository.withdraw(amount, method, accountInfo)
        }
    }
}
