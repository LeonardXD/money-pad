package com.example.moneypad.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneypad.data.MoneyPadRepository
import com.example.moneypad.data.model.Story
import com.example.moneypad.data.model.Transaction
import com.example.moneypad.data.model.User
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import java.util.UUID

data class EarningsUiState(
    val user: User? = null,
    val transactions: List<Transaction> = emptyList(),
    val myPublishedStories: List<Story> = emptyList(),
    val referralCoinsAccumulated: Int = 0,
    val referralCommission: Double = 0.0,
    val referralWithdrawals: Double = 0.0,
    val isWithdrawing: Boolean = false,
    val error: String? = null
) {
    val referralBalance: Double get() = (referralCoinsAccumulated * 0.01) + referralCommission - referralWithdrawals
}

class EarningsViewModel(private val repository: MoneyPadRepository) : ViewModel() {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<EarningsUiState> = repository.getUser(repository.currentUserId)
        .flatMapLatest { user ->
            if (user == null) flowOf(EarningsUiState())
            else {
                combine(
                    repository.getTransactions(repository.currentUserId),
                    repository.getPublishedStoriesByAuthor(repository.currentUserId),
                    repository.getTotalReferralCoins(user.username),
                    repository.getReferralAuthorWithdrawals(user.username),
                    repository.getTotalWithdrawalsBySource(repository.currentUserId, "REFERRAL")
                ) { transactions: List<Transaction>, stories: List<Story>, coins: Int, authorWithdrawals: Double, refWithdrawals: Double ->
                    EarningsUiState(
                        user = user,
                        transactions = transactions,
                        myPublishedStories = stories,
                        referralCoinsAccumulated = coins,
                        referralCommission = authorWithdrawals * 0.05,
                        referralWithdrawals = refWithdrawals
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EarningsUiState())

    // source is either "AUTHOR" or "READER"
    fun withdraw(amount: Double, method: String, accountInfo: String, source: String) {
        viewModelScope.launch {
            if (source == "AUTHOR") {
                val user = uiState.value.user
                val threshold = if (user?.isVerified == true) 20.0 else 59.95
                if (amount < threshold) return@launch
            }
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
            repository.withdraw(amount, method, accountInfo, source)
        }
    }

    fun updateReferrer(referrerUsername: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.updateReferrer(referrerUsername)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun claimReferralReward() {
        viewModelScope.launch {
            repository.claimReferralReward()
        }
    }
}
