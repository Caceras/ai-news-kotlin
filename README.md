# AI Brief

A native Android artificial intelligence news and research reader built with Kotlin and Jetpack Compose. AI Brief is designed with a quiet, typography-first aesthetic inspired by minimalist editorial print design.

---

## Design Philosophy

- **Typography-First**: Uncluttered, text-forward layout where whitespace and typography structure the reading experience.
- **Editorial Pairing**: Posts are listed with clean Date + Title alignment, letting readers scan developments without card clutter or visual noise.
- **True Ink Dark Mode**: Custom `#0E0E0D` dark palette and `#FFFFFF` light palette tuned for extended reading comfort with exact `1.6x` line-height ratios.
- **Zero Layout Shift**: Asynchronous images preload into aspect-ratio-reserved containers, ensuring smooth 60fps scrolling without text bouncing.
- **Invisible Polish**: Tactile haptic ticks, fluid crossfade transitions, and instant warm startup from local cache.

---

## Architecture & Features

- **100% Kotlin & Modern Jetpack Compose**: Built with Material 3, unidirectional data flow (`StateFlow`), and edge-to-edge support (Android API 26–36).
- **Curated Live Sources**: Direct RSS ingestion from MIT News, Google AI Research, Hugging Face, and VentureBeat.
- **Resilient Offline Architecture**:
  - Instant warm startup from persistent local JSON cache.
  - Smart cache merging that protects existing healthy articles during partial network failures.
  - Curated offline editorial edition when opening the app with no network connectivity.
- **Two-Tier Image Caching**: Memory `LruCache` paired with persistent disk caching and SHA-256 key hashing for instant repeat image renders.
- **Device-Local Shelf**: Save articles directly to device storage with zero account creation required.
- **Privacy by Default**: Zero analytics trackers, zero advertising SDKs, zero user data collection.

---

## Development

### Prerequisites
- JDK 17 (Temurin / OpenJDK)
- Android SDK with API 36 platforms and build-tools
- Gradle 8.11.1 (via bundled `./gradlew`)

### Build & Test Commands

```bash
# Run unit tests
./gradlew testDebugUnitTest

# Run Android Lint
./gradlew lintDebug

# Build Debug APK
./gradlew assembleDebug

# Build Signed Release App Bundle (AAB) for Google Play
./gradlew bundleRelease
```

---

## Google Play Release

1. Copy `keystore.properties.example` to `keystore.properties`.
2. Configure `storeFile`, `storePassword`, `keyAlias`, and `keyPassword` pointing to your private PKCS12 upload keystore (never committed to version control).
3. Run `./gradlew bundleRelease` to generate the production artifact at `app/build/outputs/bundle/release/app-release.aab`.
4. Review `PLAY_RELEASE_CHECKLIST.md` for pre-submission steps and compliance details.
5. Review `PRIVACY.md` for the public privacy policy text.
