# AI Brief

A native Kotlin/Jetpack Compose publication about artificial intelligence. The editorial design uses serif headlines, an image-led feature, compact story rows, an off-white reading surface, topic panels and navy accents. Original writing can be published daily without rebuilding the Android app.

**Today's account action:** verify the Play account's contact email and phone, then confirm an internal upload before the actual deadline. Google explicitly permits Internal app sharing/testing when the app is not ready for public release. See the [researched launch plan](docs/PLAY_LAUNCH_PLAN.md) and [release checklist](PLAY_RELEASE_CHECKLIST.md). A GitHub build is not evidence of Play submission or account rescue.

**Delivery status (4 October 2026):** the redesigned source, publishing workflow and nine-article edition are published on GitHub. Three original features have verified public featured images. The [publishing workflow](https://github.com/Caceras/ai-news-kotlin/actions/workflows/publish-post.yml) is available. GitHub Pages activation still rejects the connection with HTTP 403, so the hosted editor/privacy/contact links below need site-settings access. The verified version 114 APK remains available in Drive. No Play Console upload or account-warning clearance is claimed.

## Reading and publishing

- Original, dated explainers and briefings with linked primary sources.
- Native article reading, topic filters, on-device text search, local saving and Android sharing.
- A useful first offline installation, cached editions and explicit cached/offline labels.
- No reader account, advertising SDKs or analytics trackers. Normal hosting requests are disclosed in the [privacy policy](PRIVACY.md).
- [Publisher/contact page](https://caceras.github.io/ai-news-kotlin/contact.html) and [privacy page](https://caceras.github.io/ai-news-kotlin/privacy.html).

Publish through [the mobile editor](https://caceras.github.io/ai-news-kotlin/editor.html), or ask Codex to publish your writing with source links. The authenticated publisher workflow validates and updates content/posts.json; the app reads it at runtime. Content-only changes do not trigger Android builds. See [the daily publishing guide](docs/CONTENT.md).

External RSS code is retained but disabled in the launch edition. Check publisher reuse permissions and update privacy/Data safety declarations before enabling it. Bundled editorial illustrations are original AI-generated artwork, labeled in the reader. Headlines and text use bundled Newsreader and DM Sans fonts.

## Delivery identities

| Variant | Purpose | Signing | Self-updater |
|---|---|---|---|
| sideload | Existing direct phone-test channel | Preserved signing/sideload.jks, intentionally low-trust/public | Yes |
| internalSharing | Play Console Internal app sharing upload | Debug/test key; Google re-signs uploads | No |
| release | Play testing tracks and production AAB | Private upload key via keystore.properties or repository secrets | No |

Application ID: **com.caceras.aibrief**. Minimum SDK **26**, compile/target SDK **36**, Kotlin **2.1.21**, Android Gradle Plugin **8.10.1**, JDK **17**, Gradle **8.11.1**.

The test signing key is not a Play key. In-place phone-test updates preserve the package, certificate and increasing version code. A different Play/internal-sharing certificate cannot update that installation in place.

## Build and verify

```bash
python3 scripts/test_publishing.py
python3 scripts/publish-post.py --validate
./gradlew testReleaseUnitTest lintRelease
./gradlew assembleSideload assembleInternalSharing -PbuildNumber=12
./gradlew bundleRelease  # requires the private Play upload key
```

Versions come from gradle.properties; trusted main builds add the Actions run number to the version-code base. Development follows the repository's existing direct-main model. The Android workflow validates, publishes a signed phone-test APK and a separate internal-sharing APK, and exposes update.json for the existing sideload updater.

The manual **Build signed Play bundle** workflow needs PLAY_KEYSTORE_BASE64, PLAY_STORE_PASSWORD, PLAY_KEY_ALIAS, and PLAY_KEY_PASSWORD. It produces an AAB artifact for review and upload; it does not silently publish to Play.

See [store listing/reviewer text](docs/STORE_LISTING.md), [phone workflow](docs/PHONE_WORKFLOW.md), and [release evidence](docs/release/VERIFICATION.md). Google approval and a cleared inactivity notice must be checked in the actual Console account.
