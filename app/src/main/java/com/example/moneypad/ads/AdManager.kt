package com.example.moneypad.ads

import android.app.Activity
import android.content.Context
import com.example.moneypad.data.model.User
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

object AdManager {
    private var interstitialAd: InterstitialAd? = null
    private var appOpenAd: AppOpenAd? = null
    private var isAdLoading = false
    private var isAppOpenAdLoading = false
    private var loadTime: Long = 0

    private var partsReadCount = 0
    private val _timeUntilNextAd = MutableStateFlow(300) // 5 minutes in seconds
    val timeUntilNextAd = _timeUntilNextAd.asStateFlow()

    private var currentUser: User? = null
    private val _currentUserFlow = MutableStateFlow<User?>(null)
    val currentUserFlow = _currentUserFlow.asStateFlow()

    private val _isAdShowing = MutableStateFlow(false)
    val isAdShowing = _isAdShowing.asStateFlow()

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Test Ad Unit IDs
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395912"
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

    fun initialize(context: Context) {
        MobileAds.initialize(context) {}
        loadInterstitial(context)
        loadAppOpenAd(context)
        startTimer()
    }

    fun setUser(user: User?) {
        this.currentUser = user
        _currentUserFlow.value = user
    }

    fun isAdFree(): Boolean {
        val user = currentUser ?: return false
        if (user.isAdFreePermanently) return true
        if (user.adFreeUntil > System.currentTimeMillis()) return true
        return false
    }

    private fun loadInterstitial(context: Context) {
        if (isAdLoading || interstitialAd != null) return
        isAdLoading = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, INTERSTITIAL_AD_UNIT_ID, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                interstitialAd = null
                isAdLoading = false
            }
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
                isAdLoading = false
            }
        })
    }

    private fun loadAppOpenAd(context: Context) {
        if (isAppOpenAdLoading || isAppOpenAdAvailable()) return
        isAppOpenAdLoading = true
        val adRequest = AdRequest.Builder().build()
        AppOpenAd.load(context, APP_OPEN_AD_UNIT_ID, adRequest, object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                appOpenAd = ad
                isAppOpenAdLoading = false
                loadTime = Date().time
            }
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                isAppOpenAdLoading = false
            }
        })
    }

    private fun isAppOpenAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    fun showAppOpenAdIfAvailable(activity: Activity) {
        if (isAdFree()) return
        if (!isAppOpenAdAvailable()) {
            loadAppOpenAd(activity)
            return
        }
        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                _isAdShowing.value = true
            }
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                _isAdShowing.value = false
                loadAppOpenAd(activity)
            }
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                _isAdShowing.value = false
                loadAppOpenAd(activity)
            }
        }
        appOpenAd?.show(activity)
    }

    fun onPartRead(activity: Activity) {
        if (isAdFree()) return
        partsReadCount++
        if (partsReadCount >= 2) {
            showInterstitialIfReady(activity)
            partsReadCount = 0
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                delay(1000)
                if (_timeUntilNextAd.value > 0) {
                    _timeUntilNextAd.value--
                }
            }
        }
    }

    fun checkAndShowTimerAd(activity: Activity) {
        if (isAdFree()) return
        if (_timeUntilNextAd.value <= 0) {
            showInterstitialIfReady(activity) {
                _timeUntilNextAd.value = 300
            }
        }
    }

    fun showInterstitialIfReady(activity: Activity, onAdClosed: (() -> Unit)? = null) {
        if (isAdFree()) {
            onAdClosed?.invoke()
            return
        }
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    _isAdShowing.value = true
                }
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    _isAdShowing.value = false
                    loadInterstitial(activity)
                    onAdClosed?.invoke()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    _isAdShowing.value = false
                    loadInterstitial(activity)
                    onAdClosed?.invoke()
                }
            }
            interstitialAd?.show(activity)
        } else {
            loadInterstitial(activity)
            onAdClosed?.invoke()
        }
    }

    fun showRewardInterstitialIfReady(
        activity: Activity,
        onAdClosed: () -> Unit,
        onUnavailable: () -> Unit
    ) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    _isAdShowing.value = true
                }

                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    _isAdShowing.value = false
                    loadInterstitial(activity)
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    _isAdShowing.value = false
                    loadInterstitial(activity)
                    onUnavailable()
                }
            }
            interstitialAd?.show(activity)
        } else {
            loadInterstitial(activity)
            onUnavailable()
        }
    }
}
