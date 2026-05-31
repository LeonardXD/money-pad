package com.example.moneypad.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneypad.data.MoneyPadRepository
import com.example.moneypad.data.model.Story
import com.example.moneypad.data.model.Transaction
import com.example.moneypad.data.model.User
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val WATCH_AD_COOLDOWN_SECONDS = 60

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

    private val _watchAdCooldownSeconds = MutableStateFlow(0)
    val watchAdCooldownSeconds: StateFlow<Int> = _watchAdCooldownSeconds.asStateFlow()

    private val _isClaimingWatchAdReward = MutableStateFlow(false)
    val isClaimingWatchAdReward: StateFlow<Boolean> = _isClaimingWatchAdReward.asStateFlow()

    private var watchAdCooldownJob: Job? = null

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
                val minWithdrawal = 3.0
                val readerBalance = (uiState.value.user?.readerCoins ?: 0.0) * 0.01
                if (amount < minWithdrawal) return@launch
                if (amount > readerBalance) return@launch
            }
            
            // Deduct balance and create transaction
            repository.withdraw(amount, method, accountInfo, source)
        }
    }

    fun claimWatchAdReward(onResult: (Boolean) -> Unit) {
        if (_watchAdCooldownSeconds.value > 0 || _isClaimingWatchAdReward.value) {
            onResult(false)
            return
        }

        startWatchAdCooldown()
        viewModelScope.launch {
            _isClaimingWatchAdReward.value = true
            val success = repository.recordRewardedAdWatch()
            _isClaimingWatchAdReward.value = false

            onResult(success)
        }
    }

    private fun startWatchAdCooldown() {
        watchAdCooldownJob?.cancel()
        _watchAdCooldownSeconds.value = WATCH_AD_COOLDOWN_SECONDS
        watchAdCooldownJob = viewModelScope.launch {
            while (_watchAdCooldownSeconds.value > 0) {
                delay(1000)
                _watchAdCooldownSeconds.value = (_watchAdCooldownSeconds.value - 1).coerceAtLeast(0)
            }
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

    fun upgradeAdFree(plan: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val success = when (plan) {
                "90MIN" -> repository.upgradeToAdFree90Min()
                "PERMANENT" -> repository.upgradeToAdFreePermanent()
                else -> false
            }
            if (success) onSuccess()
            else onError("Upgrade failed. Check your balance.")
        }
    }
}
