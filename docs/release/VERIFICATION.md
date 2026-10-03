# Release verification

Prepared and checked on 3 October 2026. App source: **cab39cfa54ddf9e282506f4290f740c26511afca**. Version **2.2.1 / 114**, package **com.caceras.aibrief**, min SDK 26, target SDK 36. Later documentation and screenshot commits do not change the app code.

## Completed checks

- Release unit tests: 33 passed, 0 failures. Sideload unit tests: 33 passed, 0 failures.
- Publishing tests: 4 passed; invalid and duplicate drafts leave the edition unchanged. Six launch posts validate.
- Release and sideload lint: 0 errors; dependency/style advisory warnings remain.
- Internal-sharing and sideload APK assembly: successful. Internal-sharing signature verified.
- Internal-sharing manifest: Internet plus AndroidX's generated signature-level receiver permission; no REQUEST_INSTALL_PACKAGES or update FileProvider.
- Bundled native graphics-path libraries: ELF LOAD alignment 16 KB. APK zip-alignment verification at 16 KB: passed. This is static compatibility evidence, not physical 16 KB-device execution.
- Six complete, original AI-assisted explainers bundled in assets/posts.json; every primary-source URL returned HTTP 200 in the source receipt. Editorial accuracy still requires the publisher's review.
- Sideload certificate matches prior GitHub build 111 and local build 112, preserving direct phone-test update identity.
- Mobile editor: browser-tested at 390px width; valid briefing copied, no horizontal overflow or page errors. Local contact/privacy routes render without login; public Pages deployment has not happened.
- Store icon: 512×512 RGBA PNG. Feature graphic: 1024×500 RGB PNG. Four real Android screenshots: 1080×1920 RGB PNG, in docs/play/screenshots.

## Native reader evidence

The minified, non-debuggable internal-sharing APK was installed on an Android 10/API 29 x86_64 emulator with a 4 KB page size. The installed APK was read back and its SHA-256 matched the exact final candidate, which passed fresh offline launch, native article reading, source display, saving/removing-control state, saved persistence after process recreation, About/privacy text, body search, and rendering at 200% text scale. No AI Brief crash was present in the captured AndroidRuntime log. See android-device.json for the individual assertions.

The previous iteration’s visual inspection found text scrolling under system bars; version 113 fixes the scrolling viewport using safe drawing insets. Screenshots were recaptured on version 114. Version 114 introduces the compact masthead, Newsreader/DM Sans typography, photographic topic artwork, persistent bottom navigation and focused search list. See [the design refinement](../DESIGN_REFINEMENT.md).

The local software emulator initially stalled during installation and showed a System UI ANR dialog. After a clean device restart and clearing that system dialog, the exact installed APK completed the final checks. These environment failures are not counted as app passes or app crashes.

**Dark mode was not exercised:** this older emulator locks night mode and returns “Night mode: no” after the requested switch. The check records this limitation instead of counting a light screenshot as a dark-mode pass. Verify dark mode on a supported current Android device.

The local Android 16 software emulator's system server repeatedly died before app installation. An accelerated API 36 workflow is prepared but cannot run until GitHub writes are restored. Current-Android execution, real-phone checks, landscape/rotation, TalkBack, external browser/email/share handoffs and a Play pre-launch report remain submission gates. The above smoke checks do not claim complete core-app-quality coverage.

## Delivery and Console status

Both version-114 APKs are uploaded in the connected owner's Drive. The internal-sharing file was downloaded again and its SHA-256 matched the local APK; both APKs were downloaded again and their SHA-256 values matched the local files. delivery.json contains the exact URLs, hashes and app-source commit.

GitHub returns HTTP 403 “Resource not accessible by integration” for pushes and API writes. The source and workflows are committed locally; no new GitHub release, Pages site or authenticated publishing workflow is live. Public contact/privacy/editor URLs must be deployed and checked before production.

Owner/Console evidence still required: verified account contact details, confirmed internal-upload receipt, cleared inactivity/package-verification notices, production eligibility, correct private upload key and signed production AAB, accurate declarations, editorial review, required genuine closed testing and any Google review/approval. None is implied by the local checks.
