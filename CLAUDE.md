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
- Play artifact: `python3 scripts/check-play-artifact.py app/build/outputs/apk/internalSharing/app-internalSharing.apk` checks that no self-update code or install permission is present, and that every native library supports 16 KB pages (Play requirement for Android 15+ targets).
- Source-link audits: plain `curl` gets bot-blocked (FTC returns 404, doi.org and oscars.org 403, YouTube 429). Confirm a suspect link with WebFetch before changing content.

## Working style the owner wants

- Ship in small, frequent merges so changes are visible on the phone; do not batch 20+ minutes of work. Pre-launch: speed over polish.
- Images the owner pastes inline are not saved as files; ask for files when they matter.

## Owner-only blockers (already reported, do not re-notify)

- `AA_API_KEY` secret for daily model rankings; polls / most read / comments need a server and a privacy-policy + Data safety change (owner decision pending).

- Play Console account verification, upload key, and the four `PLAY_*` repository secrets for **Build signed Play bundle**. See `PLAY_RELEASE_CHECKLIST.md`.
- GitHub Pages is not enabled, so the privacy and contact links point to `PRIVACY.md` and `CONTACT.md` on GitHub.

## Design and publishing (November 2026 redesign)

- The look is a strict design system: `docs/DESIGN_SYSTEM.md`. Colours, fonts, type, shapes and spacing live only in `ui/theme/Theme.kt`; `DesignRulesTest` fails the build on raw values elsewhere. Add a token instead of an exception.
- Story types (`StoryFormat`: NEWS, BREAKING, EXPLAINER, OPINION, GUIDE, SPONSORED, REVIEW) each have one card and one reader header. Reviews carry `rating` 1–5. Front-page order is `FeedLayout.compose`, covered by `FeedLayoutTest`: bleed lead (headline on the picture), pair, "Latest" four, designed card, rankings, series rails, radar, reviews, opinion, more stories, guides.
- Posts can carry `series` ({id, title, part, of}), `imageCredit`, and `video` blocks (still + play button; opens outside the app).
- Data modules live under `extras` in `content/posts.json` (`rankings`, `radar`), refreshed by `scripts/update-extras.py` and `.github/workflows/update-extras.yml` (daily). Rankings need the `AA_API_KEY` secret (owner); `--from-page` reads the public leaderboard once. Every module must name its source.
- Story covers: `scripts/editorial-art/covers.py` (flat house style, venv Pillow). Series get their own artwork, never the house style. Only use photos we can license; the owner rejected cut-out photo collages.
- Bottom tabs are Latest, Explore (saved stories, series, rankings, radar), About. Saved is a section of Explore.
- The owner publishes from the **Publish a story** issue form (`.github/ISSUE_TEMPLATE/publish-story.yml` → `publish-from-issue.yml` → `scripts/issue_post.py` → `scripts/publish-post.py`). Editing the issue revises the story; Action = Unpublish removes it. Owner-only.
- See screens without a device: `SNAPSHOT_PYTHON=<venv>/bin/python scripts/render-snapshots.sh <out>` (Paparazzi in a throwaway copy, about 3 minutes; covers load from `content/media`). Read PNGs with a venv Pillow; the system Pillow is broken.
- Play screenshots come from `StoreSnaps` in the same script (`store-*.png`, 1080 × 1920 RGB, real edition only, never invented headlines). Copy them to `docs/play/screenshots` after a visible design change. Paparazzi shrinks to 1000 px unless `useDeviceResolution = true`.
- Breaking lasts `BREAKING_WINDOW` (24 h from `updatedAt ?: publishedAt`). `AiBriefApp` maps stale breaking stories to NEWS (`NewsArticle.asOf`) for every screen; check `isBreakingAt`, never `format == BREAKING` with a clock of your own.
- Sponsored posts must name a sponsor (parser drops them otherwise) and never lead. Opinion must be signed by a person.

## Code rules learned

- Never read disk (assets, SharedPreferences, cache files) on the main thread at startup. `NewsViewModel` loads the device copy on `Dispatchers.IO` and exposes `isLoading`; keep new startup work there.
- The same applies to taps: `toggleSaved` updates the shelf in memory and persists on `Dispatchers.IO` under a `Mutex`. Do not call `NewsRepository` disk methods from UI callbacks.
- Close every `HttpURLConnection` in a `finally`, including early returns.
- A `git fetch origin main <branch>` aborts entirely if the session branch does not exist on the remote yet, leaving `origin/main` stale. Fetch `main` on its own.
- Keep `res/mipmap-anydpi-v26`. Lint flags it as `ObsoleteSdkInt`, but renaming it to `mipmap-anydpi` breaks resource linking (`mipmap/ic_launcher not found`).
- Every external intent tells the reader when nothing can handle it (`startOrExplain` in `MainActivity`, a toast in `PostBlockContent`). Do not swallow `ActivityNotFoundException` silently.
- Returning to the app refreshes the edition only after 30 minutes (`refreshIfStale`, monotonic clock). Do not refresh on every resume.
- `AsyncArticleImage` seeds its state from `ArticleImageCache.cached` so memory-cached pictures draw in the first frame. Keep that when changing the loader.
- `ArticleImageCache.onTrimMemory` (called from `MainActivity.onTrimMemory`) halves decoded images when the UI is hidden and drops them in the background. Route any new in-memory cache through the same hook.
- Cards must grow with large system text, not clip it: use `heightIn(min = …)` for a designed shape, never a fixed `aspectRatio` on a box holding text. Sponsor disclosures (`SponsorPill`, sponsored `Tag`) wrap and are never ellipsized. `scripts/render-snapshots.sh` renders a `largeText` (fontScale 2) snapshot to check.
- The edition fetch sends the stored ETag (`OwnerPostsValidator`); a 304 reuses the cached document. Store or clear the ETag together with `OWNER_POSTS_KEY`, never one without the other.
- Material 3 here is 1.3 (BOM 2025.04.01): pull-to-refresh is `@ExperimentalMaterial3Api` and `PullToRefreshBox` has no `enabled`; use `Modifier.pullToRefresh(..., enabled = …)` plus `PullToRefreshDefaults.Indicator`. Snackbars take `inverseSurface`/`inversePrimary` from `materialColors` in `Theme.kt` (masthead navy, `mastheadAction`).
- Opened stories are remembered on the device (`ReadHistory`, `NewsUiState.readIds`, persisted on `Dispatchers.IO` like the shelf). `Headline` mutes read headlines through `LocalReadStories`, provided only around the tab pager, never the reader. Up Next prefers unread stories. PRIVACY.md and `docs/privacy.html` mention this list; update both if more local data is kept.
- Never run two Gradle builds on the repo at once (e.g. a background setup build plus a gate run): they corrupt `app/build`. If that happens, `rm -rf app/build` and rerun.
