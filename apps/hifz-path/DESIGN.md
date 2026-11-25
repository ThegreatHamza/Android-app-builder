# Hifz Path — design spec

- **App name:** Hifz Path
- **Package:** `com.hamzaapps.hifzpath`
- **Value proposition:** A calm, offline daily planner for memorizing the Quran. It tells you what to memorize and what to revise today using the classic sabaq / sabqi / manzil method.

## Personas
1. **Self-learner (16–40):** memorizing on their own after work or school. Needs a daily plan and a reminder of which old pages are getting weak.
2. **Parent:** tracks one or more children who study at a madrasa and revises with them at home (Pro: several students).
3. **Quran teacher:** tracks a small class on one phone (Pro: several students). Often has poor connectivity, so the app must work fully offline.

## Core user flows
1. **First launch → plan:** enter name → choose start (Al-Fatiha or An-Nas) → daily amount (¼–2 pages) → revision cycle (7–60 days) → Today screen.
2. **Daily session:** Today → tap *Memorized* on the new page → rate Weak/Good/Strong → tap *Revised* on each sabqi and manzil page → streak and progress update.
3. **Record existing hifz:** Surahs tab → tap a surah → *Mark as already memorized* (or Mushaf tab → juz menu → mark whole juz). Those pages join the manzil rotation.

## Screens
| Screen | Contents |
|---|---|
| Setup | Welcome text, name field (validated), plan editor (direction radios, amount chips, cycle chips), Start button |
| Today | Progress card (done/total, streak, pages memorized); three sections (Sabaq, Sabqi, Manzil) of page rows with surah names and juz; action button or check + Undo; rating dialog; empty states |
| Mushaf | Juz chips (1–30), juz summary + menu (mark known / clear), colour legend, grid of page tiles coloured by status and strength; bottom sheet per page (status segmented buttons, strength, revision count, log revision) |
| Surahs | Search (name, meaning, Arabic, number), 114 rows with number badge, transliteration, meaning, ayahs, page range, Arabic name, progress bar; action dialog |
| Progress | 6 stat cards, completion estimate, 7-day bar chart, 12-week heatmap (Pro), per-juz progress bars |
| Students | List, switch, rename, delete (confirm), add (Pro beyond 1) |
| Settings | Pro card, plan editor, theme, backup export/import, rate, privacy policy, version, data credit |

## Data model (Room, on device)
- `students(id, name, direction, newQuartersPerDay, cycleDays, recentDays, createdEpochDay)`
- `page_progress(studentId, page, status[1 learning, 2 memorized], strength[1–3], memorizedEpochDay?, lastReviewedEpochDay?, reviewCount)`: primary key (studentId, page), cascade delete
- `review_log(id, studentId, page, epochDay, kind[0 new, 1 review], quality)`
- DataStore: theme, active student, cached Pro flag.
- Quran metadata (114 surahs, page ranges, 30 juz starts) is bundled, generated from the MIT-licensed `quran-meta` dataset (Madani 604-page mushaf). No Quran text is bundled.

## Planning logic (`PlanGenerator`)
- **Sabaq:** next ⌈amount⌉ pages in the chosen direction; *learning* pages first.
- **Sabqi:** pages memorized in the last 7 days (not today), every day.
- **Manzil:** older pages; daily quota = ⌈old pages ÷ cycle days⌉; ordered by `(days since last revision + 1) × weight` (weak 3, good 2, strong 1).
- Work done today stays in the list as done, so the list never jumps.

## Monetization
- **No ads.** Ads are a poor fit for a worship app and would hurt reviews.
- **Pro, one-time (`pro_unlock`), suggested €2.99:** unlimited student profiles and the 12-week activity calendar. Core tracking, planning and backup stay free.
- Purchase checked on the device with Play Billing and cached for offline use.

## Visual identity
- Material 3, emerald `#0F6B4F` primary with gold `#B7862A` accent, warm off-white background; full dark scheme.
- Adaptive icon: open mushaf with gold crescent and star on emerald (plus monochrome layer for themed icons). Splash screen API.
- Default Material type scale; Arabic surah names shown in `titleMedium`.

## Languages
English (all UI strings in `strings.xml`). Layout supports RTL, so Arabic, Somali or French can be added as `values-ar`, `values-so` or `values-fr`.
