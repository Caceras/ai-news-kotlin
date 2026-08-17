# AI Brief

A native Android AI news reader built with Kotlin and Jetpack Compose. It presents a quiet, text-first daily feed with public RSS sources, a resilient offline edition, saved reads, sharing, and direct links to original reporting.

## Run

1. Open this folder in Android Studio.
2. Set the Android SDK path when prompted.
3. Run the `app` configuration on an Android 8.0+ device or emulator.

## Release

The app targets Android API 36 and has version `2.0.0` / code `2`. Release builds require a local upload key so an unsigned artifact can never be uploaded by mistake.

1. Copy `keystore.properties.example` to `keystore.properties`.
2. Create or point `storeFile` at a private PKCS12 upload key and fill the remaining values.
3. Run `./gradlew bundleRelease` to produce `app/build/outputs/bundle/release/app-release.aab`.

Keep the key and its properties outside Git and back them up securely. The app requests only network access for public news feeds and stores saved articles locally on the device.

Before the Play submission, host `PRIVACY.md` at a public URL and complete `PLAY_RELEASE_CHECKLIST.md` in Play Console.
