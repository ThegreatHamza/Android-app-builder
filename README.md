# Android App Builder

Offline-first Android apps with no backend, built with Kotlin, Jetpack Compose and Material 3.
Each app in `apps/` is a separate Gradle project.

| App | Package | What it does | Monetization |
|---|---|---|---|
| [Hifz Path](apps/hifz-path) | `com.hamzaapps.hifzpath` | Daily Quran memorization planner (sabaq / sabqi / manzil), page, surah and juz tracking | No ads · Pro one-time unlock |
| [Site Calc](apps/site-calc) | `com.hamzaapps.sitecalc` | 8 metric construction calculators with cost estimates and project quotes | AdMob banner + interstitial · Pro removes ads |

Each app folder contains `DESIGN.md`, `STORE_LISTING.md` and `PRIVACY_POLICY.md`.

## Build locally
Requirements: JDK 17+ and the Android SDK (platform 36, build-tools 36).

```bash
cd apps/hifz-path            # or apps/site-calc
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew testDebugUnitTest lintRelease assembleRelease bundleRelease
```

Release signing reads `keystore.properties` (never committed) or the `KEYSTORE_PATH` / `KS_PASS` environment variables:

```properties
storeFile=/absolute/path/to/release.jks
storePassword=...
keyAlias=release
keyPassword=...
```

## Build on GitHub
`.github/workflows/android-apps.yml` tests and builds both apps on every push that changes `apps/`.
Add two repository secrets so the APKs are signed with your key:

- `KEYSTORE_BASE64`: output of `base64 -w0 release.jks`
- `KS_PASS`: the keystore password

Push a tag such as `v1.0.0` to publish the APK and AAB files on a GitHub Release.

## Before publishing on Google Play
1. **Site Calc:** replace the test AdMob IDs in `billing/Monetization.kt` and the AdMob app ID in `app/build.gradle.kts`.
2. Create the one-time in-app product `pro_unlock` in Play Console for each app.
3. Host each `PRIVACY_POLICY.md` publicly (GitHub Pages or Vercel) and update `PRIVACY_POLICY_URL` in `Monetization.kt`.
4. Upload the `.aab`, add screenshots and copy the text from `STORE_LISTING.md`.
