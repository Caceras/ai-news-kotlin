# Release verification

Prepared and checked on 3 October 2026. App source: **9e1ddb9c8d4e86bc835a16e2e95aa0dfaa827282**. Version **2.2.0 / 113**, package **com.caceras.aibrief**, min SDK 26, target SDK 36. Later documentation and screenshot commits do not change the app code.

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

The minified, non-debuggable internal-sharing APK was installed on an Android 10/API 29 x86_64 emulator with a 4 KB page size. The exact final candidate passed fresh offline launch, native article reading, source display, saving/removing-control state, saved persistence after process recreation, About/privacy text, body search, and rendering at 200% text scale. No AI Brief crash was present in the captured AndroidRuntime log. See android-device.json for the individual assertions.

Visual inspection found text scrolling under system bars; version 113 fixes the scrolling viewport using safe drawing insets. Screenshots were recaptured on that version. An earlier search-check failure was a test selector expecting a result above the fold; the check now scrolls to the result.

**Dark mode was not exercised:** this older emulator locks night mode and returns “Night mode: no” after the requested switch. The check records this limitation instead of counting a light screenshot as a dark-mode pass. Verify dark mode on a supported current Android device.

The local Android 16 software emulator's system server repeatedly died before app installation. An accelerated API 36 workflow is prepared but cannot run until GitHub writes are restored. Current-Android execution, real-phone checks, landscape/rotation, TalkBack, external browser/email/share handoffs and a Play pre-launch report remain submission gates. The above smoke checks do not claim complete core-app-quality coverage.

## Delivery and Console status

Both version-113 APKs are uploaded in the connected owner's Drive. The internal-sharing file was downloaded again and its SHA-256 matched the local APK; phone-test metadata was read back and its size matched. delivery.json contains the exact URLs, hashes and app-source commit.

GitHub returns HTTP 403 “Resource not accessible by integration” for pushes and API writes. The source and workflows are committed locally; no new GitHub release, Pages site or authenticated publishing workflow is live. Public contact/privacy/editor URLs must be deployed and checked before production.

Owner/Console evidence still required: verified account contact details, confirmed internal-upload receipt, cleared inactivity/package-verification notices, production eligibility, correct private upload key and signed production AAB, accurate declarations, editorial review, required genuine closed testing and any Google review/approval. None is implied by the local checks.
