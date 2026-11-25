package com.hamzaapps.sitecalc.ads

import android.app.Activity
import android.content.Context
import android.os.Build
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.hamzaapps.sitecalc.billing.Monetization
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Consent (UMP / GDPR) and ads. Ads are only requested after consent allows it,
 * and never for Pro users.
 */
class AdsManager(context: Context) {
    private val appContext = context.applicationContext
    private val initialized = AtomicBoolean(false)
    private var interstitial: InterstitialAd? = null
    private var lastInterstitialAt = 0L

    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    /** Ads SDKs are not available in local JVM tests. */
    private val disabled = Build.FINGERPRINT == "robolectric"

    fun gatherConsent(activity: Activity, isPro: Boolean) {
        if (disabled || isPro) return
        val info = UserMessagingPlatform.getConsentInformation(activity)
        info.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    onConsentReady(info)
                }
            },
            { onConsentReady(info) },
        )
        // Consent from a previous session may already allow ads.
        onConsentReady(info)
    }

    private fun onConsentReady(info: ConsentInformation) {
        _privacyOptionsRequired.value =
            info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (info.canRequestAds() && initialized.compareAndSet(false, true)) {
            MobileAds.initialize(appContext) {}
            _canRequestAds.value = true
            loadInterstitial()
        }
    }

    fun showPrivacyOptions(activity: Activity) {
        if (disabled) return
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { }
    }

    private fun loadInterstitial() {
        if (!_canRequestAds.value || interstitial != null) return
        InterstitialAd.load(
            appContext,
            Monetization.INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                }
            },
        )
    }

    /**
     * Shows an interstitial after a natural break (saving a calculation), at most once every
     * few minutes and never during the first launch.
     */
    fun maybeShowInterstitial(activity: Activity, isPro: Boolean, launchCount: Int) {
        if (disabled || isPro || !_canRequestAds.value) return
        if (launchCount < Monetization.INTERSTITIAL_MIN_LAUNCHES) return
        val now = System.currentTimeMillis()
        if (now - lastInterstitialAt < Monetization.INTERSTITIAL_MIN_INTERVAL_MS) return
        val ad = interstitial ?: run {
            loadInterstitial()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                loadInterstitial()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitial = null
                loadInterstitial()
            }
        }
        lastInterstitialAt = now
        ad.show(activity)
    }
}
