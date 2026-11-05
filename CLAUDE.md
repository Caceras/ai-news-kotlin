# Notes for automated runs

Read this first on every run. Keep it short and current.

## Goal and rules

- Bring AI Brief (`com.caceras.aibrief`) to Google Play release quality: lean, fast, policy-compliant. Fix Play-review risks before adding features.
- Work on the session branch, open a PR, and merge it yourself with **rebase** once these pass locally with 0 test failures and 0 lint errors:
  `./gradlew testReleaseUnitTest lintRelease assembleInternalSharing`
- **Start each run by checking open PRs.** Earlier runs have left gated PRs open and unmerged. Verify each one (check out the branch, run the gates, check review comments) and merge it before starting new work.
- After merging, confirm the **Android** workflow on `main` is green and a new release (`build-<versionCode>`) is published.
- A docs-only change (`**.md`, `docs/**`, `content/**`) does not trigger the Android workflow and produces no new release. That is expected.
- Never claim Play Console actions, uploads or approvals. Only the owner can do those.

## Local build setup (cloud container)

The container has JDK 21 and no Android SDK. Gradle pins Kotlin's JVM target to 17, so JDK 21 works.

```bash
mkdir -p ~/android-sdk/cmdline-tools && cd ~/android-sdk
curl -sSfLo ct.zip https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip
unzip -q ct.zip -d cmdline-tools && mv cmdline-tools/cmdline-tools cmdline-tools/latest
yes | cmdline-tools/latest/bin/sdkmanager --sdk_root=$HOME/android-sdk "platforms;android-36" "build-tools;35.0.0" "platform-tools"
echo "sdk.dir=$HOME/android-sdk" > <repo>/local.properties   # git-ignored
```

Maven Central (`repo.maven.apache.org`) returns **HTTP 429** from this container, and the Gradle plugin portal redirects there. Use Google's Maven Central mirror through a user-level init script so the repo is unchanged:

```groovy
// ~/.gradle/init.d/mirror.gradle
def mirror = 'https://maven-central.storage-download.googleapis.com/maven2/'
def swap = { repos ->
  repos.removeAll { it instanceof MavenArtifactRepository &&
    (it.url.toString().contains('repo.maven.apache.org') || it.url.toString().contains('plugins.gradle.org')) }
  repos.maven { url mirror }
}
settingsEvaluated { s ->
  swap(s.pluginManagement.repositories)
  swap(s.dependencyResolutionManagement.repositories)
}
```

A cold build of the gates takes about 3 minutes.

## Checking results

- Tests: sum `tests`/`failures` across `app/build/test-results/testReleaseUnitTest/*.xml`.
- Lint: count `severity="Error"` and `severity="Fatal"` in `app/build/reports/lint-results-release.xml`.
- Workflow status: query it with the GitHub MCP `actions_list` tool. Unauthenticated `curl` polling of `api.github.com` from the container returns nothing useful, so a Monitor built on it stays silent. The Android run takes about 6 minutes.
- Play artifact: `python3 scripts/check-play-artifact.py app/build/outputs/apk/internalSharing/app-internalSharing.apk` checks that no self-update code or install permission is present.

## Owner-only blockers (already reported, do not re-notify)

- Play Console account verification, upload key, and the four `PLAY_*` repository secrets for **Build signed Play bundle**. See `PLAY_RELEASE_CHECKLIST.md`.
- GitHub Pages is not enabled, so the privacy and contact links point to `PRIVACY.md` and `CONTACT.md` on GitHub.

## Code rules learned

- Never read disk (assets, SharedPreferences, cache files) on the main thread at startup. `NewsViewModel` loads the device copy on `Dispatchers.IO` and exposes `isLoading`; keep new startup work there.
- The same applies to taps: `toggleSaved` updates the shelf in memory and persists on `Dispatchers.IO` under a `Mutex`. Do not call `NewsRepository` disk methods from UI callbacks.
- Close every `HttpURLConnection` in a `finally`, including early returns.
- A `git fetch origin main <branch>` aborts entirely if the session branch does not exist on the remote yet, leaving `origin/main` stale. Fetch `main` on its own.
- Keep `res/mipmap-anydpi-v26`. Lint flags it as `ObsoleteSdkInt`, but renaming it to `mipmap-anydpi` breaks resource linking (`mipmap/ic_launcher not found`).
- Every external intent tells the reader when nothing can handle it (`startOrExplain` in `MainActivity`, a toast in `PostBlockContent`). Do not swallow `ActivityNotFoundException` silently.
