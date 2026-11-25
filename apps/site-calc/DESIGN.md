# Site Calc — design spec

- **App name:** Site Calc
- **Package:** `com.hamzaapps.sitecalc`
- **Value proposition:** Fast, offline, metric material and cost estimates for builders, so you can quote a client on site in under a minute.

## Personas
1. **Fundi / small contractor (East Africa, Middle East):** quotes jobs on site, usually offline, thinks in bags of cement, blocks and iron sheets, and needs a cost to show the client.
2. **Self-builder / homeowner:** building their own house in stages and wants to know how much cement, sand and steel to buy and what it will cost.
3. **Civil engineering student / site supervisor:** quick checks of standard quantities.

## Core user flows
1. **Quick estimate:** Calculators → Concrete → enter L, W, thickness → results update live (volume, bags, sand, gravel, water).
2. **Costed quote:** in any calculator open *Prices (optional)* → enter the local cement/sand price once (remembered) → see the estimated cost → *Save to project* → new project "Ahmed's house".
3. **Share the quote:** Projects → open project → total cost → Share (WhatsApp text) / CSV / PDF (Pro).

## Screens
| Screen | Contents |
|---|---|
| Calculators (home) | Intro line; adaptive grid of 8 calculator cards (icon, name, description) |
| Calculator | Description, option chips (shape, mix, coats, diameter, roof type), numeric fields with unit suffixes and validation, collapsible prices, live results card with key lines and cost, disclaimer, Save/Update button, banner ad |
| Save dialog | Item name, choose an existing project or a new one (free limit 2), project name |
| Projects | List with item count and total; empty state; New project FAB |
| Project detail | Name + menu (rename, delete), Share / CSV / PDF chips, total cost card, item list (tap to edit, delete with confirm), banner ad |
| Settings | Pro card, currency, cement bag size (25/42.5/50 kg), theme, backup export/import, ad privacy choices (when required), rate, privacy policy, version |

## Calculators and formulas
| Calculator | Formula (metric) |
|---|---|
| Concrete | V = L·W·T (slab), W·D·H·n (square column), π(d/2)²·H·n (round); dry = 1.54·V; cement kg = dry/parts·1440; sand/gravel by ratio; water = 0.5 L/kg cement |
| Blocks | net area = L·H − openings; blocks = area / ((l+j)(h+j)); mortar = area·t − blocks·l·h·t; dry = 1.33·mortar |
| Plaster | wet = area·t; dry = 1.27·wet; split by mix |
| Tiles | ⌈area / tile area · (1+waste)⌉, boxes = ⌈tiles / per box⌉ |
| Paint | L = (area − openings)·coats / coverage; cans = ⌈L / can⌉ |
| Rebar | kg = d²/162 · total length; stock bars = ⌈total / stock length⌉ |
| Excavation | bank = L·W·D; loose = bank·(1+swell); trucks = ⌈loose / capacity⌉ |
| Roofing | per row = ⌈length / cover width⌉; rows = ⌈(slope − lap)/(sheet − lap)⌉; × sides |

Whole items (bags, blocks, boxes, cans, bars, trucks, sheets) are rounded up. Every result shows an "estimates only" disclaimer.

## Data model (Room, on device)
- `projects(id, name, createdAt)`
- `items(id, projectId → projects (cascade), type, title, inputsJson, optionsJson, summary, cost, createdAt)`
- DataStore: theme, currency, bag size, price per material, cached Pro flag, launch count.

## Monetization
- **AdMob:** adaptive banner on secondary screens only (calculator, project detail), never on the home grid. Interstitial only after saving a calculation (a natural break), at most once every 4 minutes, never during the first launch. Google UMP consent form (GDPR) before any ad request, and an "Ad privacy choices" entry in Settings when required.
- **Pro, one-time (`pro_unlock`), suggested €3.99:** removes all ads, unlimited projects (free: 2), PDF reports.
- Test ad IDs are used until you replace them in `billing/Monetization.kt` and `app/build.gradle.kts`.

## Visual identity
- Material 3, safety orange `#B85000` primary, steel blue-grey secondary, warm background; full dark scheme.
- Adaptive icon: yellow hard hat over a ruler on safety orange, with monochrome layer. Splash screen API.

## Languages
English (all UI strings in `strings.xml`). RTL-ready layout; French, Arabic or Swahili can be added as `values-*` folders.
