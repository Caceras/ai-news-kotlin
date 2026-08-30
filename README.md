# AI Brief

A native Android artificial intelligence news reader built with Kotlin and Jetpack
Compose, designed around a quiet, typography-first aesthetic borrowed from
minimalist editorial print.

**Using the app on a phone?** Start with [docs/PHONE_WORKFLOW.md](docs/PHONE_WORKFLOW.md)
— it covers installing, updating, and requesting changes without touching any of
the tooling described below.

---

## Design philosophy

- **Typography-first** — whitespace and type hierarchy carry the structure; no cards, no chrome.
- **Editorial pairing** — stories list as date and headline, scannable without visual noise.
- **True ink dark mode** — a `#0E0E0D` dark palette and `#FFFFFF` light palette tuned for long reading, with `1.6x` line height.
- **Zero layout shift** — images load into aspect-ratio-reserved containers so text never bounces.
- **Invisible polish** — haptic ticks, crossfade transitions, and warm startup from local cache.

Anything added to the app is expected to disappear into this language rather than
announce itself. The update banner is a case in point: one line of text, not a dialog.

---

## Architecture

- **100% Kotlin, Jetpack Compose, Material 3** — unidirectional data flow over `StateFlow`, edge-to-edge, Android API 26–36.
- **Curated live sources** — RSS ingestion from MIT News, Google AI Research, Hugging Face, and VentureBeat.
- **Resilient offline behaviour** — instant warm start from a persistent JSON cache, cache merging that protects healthy articles through partial network failures, and a curated offline edition when there is no connectivity at all.
- **Two-tier image caching** — an in-memory `LruCache` over a persistent disk cache keyed by SHA-256.
- **Device-local shelf** — saved articles live on the device; no account, ever.
- **Privacy by default** — no analytics, no advertising SDKs, no user data collection.

### Source layout

```
app/src/main/java/com/caceras/aibrief/
├── MainActivity.kt          Compose UI: feed, saved, about, article reader
├── NewsViewModel.kt         Feed state
├── data/NewsRepository.kt   RSS fetch, parsing, caching, saved articles
├── ui/                      Theme and shared components
└── update/                  Direct-install self-updater (sideload builds only)
```

---

## Build variants

Three variants come out of one source tree. They differ only in signing and in
whether the self-updater is present.

| Variant | Purpose | Signing key | Self-updater | Minified |
|---|---|---|---|---|
| `debug` | Local development | Android debug key | No | No |
| `sideload` | Direct install on a test phone | `signing/sideload.jks` (committed) | Yes | Yes |
| `release` | Google Play artifact | Upload key via `keystore.properties` | No | Yes |

`sideload` is created with `initWith(release)`, so what gets tested on a phone
matches what would ship in optimisation and debuggability.

### Why a signing key is committed

Android only allows an app to update in place when the new APK is signed with the
same key as the installed one. CI runners are ephemeral, so a generated debug key
would differ on every run and every update would fail with
`INSTALL_FAILED_UPDATE_INCOMPATIBLE`.

`signing/sideload.jks` therefore exists to give the test channel a stable identity.
Its password is in `app/build.gradle.kts` and it is deliberately public. It carries
no trust: it signs nothing that reaches the Play Store, and the Play upload key is
a separate private key that is never committed.

---

## Release pipeline

`.github/workflows/android.yml` runs on every push.

1. **verify** — unit tests and Android lint against the `sideload` variant.
2. **publish** — assembles the signed APK, writes an `update.json` manifest beside it, uploads both as build artifacts, and on `main` publishes them as a GitHub Release.

Merging to `main` is the entire publish action. The installed app polls
`releases/latest/download/update.json` and offers whatever it finds there.

Version identity lives in `gradle.properties` (`aibrief.versionName`,
`aibrief.versionCodeBase`) and is read by both Gradle and the workflow, so a
published manifest cannot describe a version its APK does not carry. The build
number comes from the workflow run number, which guarantees a strictly increasing
`versionCode`.

---

## Development

### Prerequisites

- JDK 17 (Temurin / OpenJDK)
- Android SDK with API 36 platform and build-tools

### Commands

```bash
# Unit tests
./gradlew testSideloadUnitTest

# Android lint
./gradlew lintSideload

# Direct-install APK (unsigned version code unless -PbuildNumber is passed)
./gradlew assembleSideload

# Google Play bundle — requires keystore.properties
./gradlew bundleRelease
```

---

## Google Play release

1. Copy `keystore.properties.example` to `keystore.properties`.
2. Point `storeFile`, `storePassword`, `keyAlias`, and `keyPassword` at your private upload keystore. Neither file is ever committed.
3. Run `./gradlew bundleRelease` to produce `app/build/outputs/bundle/release/app-release.aab`.
4. Work through [PLAY_RELEASE_CHECKLIST.md](PLAY_RELEASE_CHECKLIST.md).
5. Publish [PRIVACY.md](PRIVACY.md) at a stable HTTPS URL and link it in Play Console.
