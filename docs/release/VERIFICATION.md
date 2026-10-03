# Release verification

Local checks on 3 October 2026:

- Release unit tests: 33 passed, 0 failures.
- Publishing workflow tests: 4 passed; invalid/duplicate drafts leave the edition unchanged.
- Release lint: 0 errors; dependency/style advisory warnings remain.
- Internal-sharing and sideload APK assembly: successful.
- Internal-sharing APK: signature verified, package com.caceras.aibrief, version 2.2.0/code 112, min SDK 26, target SDK 36.
- Internal-sharing manifest: Internet plus AndroidX's generated signature-level receiver permission. No REQUEST_INSTALL_PACKAGES or update FileProvider.
- All bundled native graphics-path libraries: ELF LOAD alignment 16 KB. APK zip alignment verification at 16 KB: passed.
- Six complete, sourced articles bundled in assets/posts.json. Every primary-source URL returned HTTP 200 in the source checks.
- Sideload certificate matches the prior build 111, preserving phone-test update identity.
- Mobile editor: browser-tested at 390px width; valid briefing copied, no horizontal overflow, no page errors. Local contact/privacy routes render and require no login; public deployment is still blocked.
- Store icon: 512×512 RGBA PNG; feature graphic: 1024×500 RGB PNG. Actual Android screenshots are pending runtime verification.

The local software emulator's Android system server repeatedly died before app installation. Its errors name Android system processes, not AI Brief. An accelerated GitHub check is prepared but cannot start while repository writes are denied (HTTP 403). A lighter local Android 9 emulator has booted; native checks are in progress. Do not treat the above static/build checks as a completed device test.

Still requiring owner/Console evidence: verified Play contact details and internal-upload receipt, cleared inactivity/verification notices, production eligibility, private upload key, signed production AAB, accurate declarations, real-phone testing and any Play pre-launch report/approval. None is claimed by the build checks.
