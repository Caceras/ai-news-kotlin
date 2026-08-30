# Google Play Store Release Checklist

## 1. News and Magazine Policy Compliance
- [ ] **In-App Contact Information**: Verify that the in-app "About" screen shows the support email (`rikicaceras@gmail.com`) and that the same contact URL/email is entered in Play Console.
- [ ] **Publisher Provenance**: Verify each story displays its original publisher name (e.g., MIT News, Google AI, Hugging Face, VentureBeat), author byline when available, publication date, and a direct link to the original source.
- [ ] **Editorial Notes Attribution**: Verify offline brief items are clearly labeled and not misrepresented as real-time wire feeds.
- [ ] **Console Declaration**: Under **Policy and programs > App content > News and Magazine apps**, complete the declaration indicating this is a news reader app.

## 2. Privacy Policy & Data Safety
- [ ] **Host Privacy Policy**: Host `PRIVACY.md` at a stable public HTTPS URL (e.g. GitHub Pages or website).
- [ ] **Play Console Privacy Link**: Under **App content > Privacy Policy**, submit the hosted URL.
- [ ] **Data Safety Questionnaire**:
  - **Data collection**: Select **No** (The app does not collect or share any user data).
  - **Data encryption**: All network traffic uses HTTPS / TLS encryption (`usesCleartextTraffic="false"`).
  - **Account creation**: No account required.
  - **Tracking**: No third-party tracking or advertising SDKs included.

## 3. Store Listing & Assets
- [ ] **App Title**: AI Brief
- [ ] **Short Description** (max 80 chars): A quiet, minimal reader for artificial intelligence research and news.
- [ ] **Full Description**: Editorial overview highlighting typography-first reading, live research feeds, offline reading shelf, and privacy by design.
- [ ] **App Icon**: 512 × 512 PNG (32-bit color, no transparency, max 1024KB).
- [ ] **Feature Graphic**: 1024 × 500 JPG or PNG.
- [ ] **Phone Screenshots**: At least 4 high-resolution screenshots capturing the Home Feed, Category Filter, Saved Shelf, and Article Reader view in both Light and Dark modes.

## 4. Release Build & Upload
- [ ] **Register a Play developer account**: a one-time $25 fee to Google. Nothing else in this project costs money.
- [ ] **Create the upload key** (once, and never commit it):
  ```bash
  keytool -genkeypair -keystore upload-keystore.jks -storetype PKCS12 \
    -alias upload -keyalg RSA -keysize 2048 -validity 10950
  ```
- [ ] **Configure signing**: copy `keystore.properties.example` to `keystore.properties` and fill in the four values.
- [ ] **Generate Release Bundle**:
  ```bash
  ./gradlew bundleRelease
  ```
- [ ] **Verify Artifact**:
  - Path: `app/build/outputs/bundle/release/app-release.aab`
  - Version name from `aibrief.versionName` in `gradle.properties`
  - Signed with the upload key, **not** the sideload key
- [ ] **Confirm the Play build carries no sideload traces**: the AAB must not declare `REQUEST_INSTALL_PACKAGES`, which Google Play restricts. It is declared only in `app/src/sideload/AndroidManifest.xml` and so cannot reach the `release` variant. Verify after building:
  ```bash
  # Expect no output.
  bundletool dump manifest --bundle app/build/outputs/bundle/release/app-release.aab \
    | grep REQUEST_INSTALL_PACKAGES
  ```
- [ ] **Closed Testing Track**: Upload the AAB to Internal or Closed Testing to verify live device performance before production rollout.

## 5. Migrating Off the Direct-Install Build
- [ ] **Uninstall the sideload build from every test device first.** The Play artifact is signed with a different key, so Android will refuse to install it over a direct-install copy. Saved articles are device-local and will not survive the switch.
- [ ] **Confirm `versionCode` continuity**: the Play track must start above the highest `versionCode` ever published to the direct-install channel, so bump `aibrief.versionCodeBase` in `gradle.properties` if the two sequences would otherwise collide.
