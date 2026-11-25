package com.hamzaapps.hifzpath.billing

/**
 * Monetization settings.
 *
 * Hifz Path has no ads: it is free, with a one-time "Pro" unlock.
 * TODO: create a one-time in-app product with this exact ID in Play Console
 * (Monetize > Products > In-app products) before publishing.
 */
object Monetization {
    const val PRO_PRODUCT_ID = "pro_unlock"

    /** Number of student profiles available without Pro. */
    const val FREE_STUDENT_LIMIT = 1

    // TODO: replace with the public URL where you host PRIVACY_POLICY.md (GitHub Pages, Vercel, ...).
    const val PRIVACY_POLICY_URL = "https://github.com/ThegreatHamza/Android-app-builder/blob/main/apps/hifz-path/PRIVACY_POLICY.md"
}
