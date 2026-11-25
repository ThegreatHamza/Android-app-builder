package com.hamzaapps.sitecalc.billing

/**
 * All monetization IDs in one place.
 *
 * The ad unit IDs below are Google's official TEST IDs: they are safe to use while developing
 * and never earn money. Before publishing:
 *  1. TODO: replace BANNER_ID and INTERSTITIAL_ID with your AdMob ad unit IDs.
 *  2. TODO: replace the AdMob APP ID in app/build.gradle.kts (manifestPlaceholders["admobAppId"]).
 *  3. TODO: create a one-time in-app product "pro_unlock" in Play Console.
 */
object Monetization {
    const val BANNER_ID = "ca-app-pub-3940256099942544/9214589741" // TODO: replace with your AdMob banner ID
    const val INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712" // TODO: replace with your AdMob interstitial ID

    const val PRO_PRODUCT_ID = "pro_unlock"

    /** Projects available without Pro. */
    const val FREE_PROJECT_LIMIT = 2

    /** Minimum time between interstitials, and never during the first app launch. */
    const val INTERSTITIAL_MIN_INTERVAL_MS = 4 * 60 * 1000L
    const val INTERSTITIAL_MIN_LAUNCHES = 2

    // TODO: replace with the public URL where you host PRIVACY_POLICY.md.
    const val PRIVACY_POLICY_URL = "https://github.com/ThegreatHamza/Android-app-builder/blob/main/apps/site-calc/PRIVACY_POLICY.md"
}
