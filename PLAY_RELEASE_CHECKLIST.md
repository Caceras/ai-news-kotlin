# AI Brief Play release checklist

For the researched plan and official sources, read [docs/PLAY_LAUNCH_PLAN.md](docs/PLAY_LAUNCH_PLAN.md). A local build is not evidence of Play submission or account rescue.

## Today's inactivity action

- [ ] Contact email verified and saved in **Developer account > Account details**.
- [ ] Contact phone number verified and saved in the same account.
- [ ] **ai-brief-internal-sharing.apk** uploaded and confirmed through **Internal app sharing**, or a correctly signed AAB rolled out on Internal testing. Retain receipt, timestamp and link.
- [ ] Recheck the inactivity issue. If unresolved, contact developer support immediately with receipts; do not assume an extension.
- [ ] Check Android developer verification for **com.caceras.aibrief**, separately from the screenshot's **Com.caceras** draft.

## Production eligibility and signing

- [ ] Confirm account type, creation date and production-access status.
- [ ] If applicable: 12 genuine closed testers continuously opted in for 14 days, feedback recorded, issues addressed, and production access approved.
- [ ] Use the established private upload key if this package has prior Play uploads. For a new identity, securely create/back up the upload key and enroll in Play App Signing.
- [ ] Configure keystore.properties locally or the four PLAY_* repository secrets for **Build signed Play bundle**.
- [ ] Choose a versionCode greater than prior Play uploads and verify the package is **com.caceras.aibrief**.
- [ ] Build the signed **release AAB**; the internal-sharing APK and sideload APK are different delivery artifacts.
- [ ] Check signature, certificate, version, target SDK, native-library compatibility and SHA-256.
- [ ] Verify **REQUEST_INSTALL_PACKAGES** and the sideload updater/provider are absent from the Play artifact, and native libraries support 16 KB pages (`scripts/check-play-artifact.py`, run automatically by both build workflows).

## Publication and policy

- [ ] News & Magazines category and self-declaration completed truthfully.
- [ ] Every article shows its publisher, date and author when available; original posts include working primary-source links.
- [ ] Launch content is useful and current; a real routine exists for new original writing. Offline explainers are visibly labeled.
- [ ] Text, photographs, logos and graphics are owned or licensed. RSS availability alone is not permission. This includes pictures pasted into the issue form and breaking-news collages.
- [ ] Paid content is published only as **Sponsored** (the app labels it with the sponsor on the card and in the story, and never leads with it). If you ever sell sponsorships, answer the Play Console **Ads** declaration accordingly.
- [ ] Opinion columns are signed by a named person; the app labels them Opinion with an "author's own views" note.
- [ ] In-app contact link and news declaration both use **https://github.com/Caceras/ai-news-kotlin/blob/main/CONTACT.md**.
- [ ] Support email **rikicaceras@gmail.com** is correct and monitored.
- [ ] Privacy policy loads publicly at **https://github.com/Caceras/ai-news-kotlin/blob/main/PRIVACY.md**, and the app links it.
- [ ] Data safety is completed from actual app and hosting data practices. Do not blindly select “No”: audit IP/request handling by GitHub and any image host.
- [ ] Ads, app access, actual target audience, IARC questionnaire and every applicable Console “Needs attention” declaration completed accurately.
- [ ] No reviewer login is needed. Use [docs/STORE_LISTING.md](docs/STORE_LISTING.md) for access instructions and listing copy.

## Store assets

- [ ] Title **AI Brief** (under 30 characters), short description at most 80 characters, full description under 4,000 characters.
- [ ] App icon: **512 × 512**, 32-bit PNG, at most 1,024 KB; follow Play icon rules.
- [ ] Feature graphic: **1024 × 500**, JPEG or 24-bit PNG without alpha.
- [ ] **At least two actual app screenshots** are required. Four 1080 × 1920 portrait screenshots are recommended for promotional eligibility, not the minimum listing requirement.
- [x] Screenshots retaken after the November 2026 redesign: five 1080 × 1920 RGB PNGs in `docs/play/screenshots`, rendered from the real published edition (`content/posts.json`) by `scripts/render-snapshots.sh` (`StoreSnaps`). Re-render after a design change and upload them in Play Console (owner).
- [ ] Screenshots show the real release candidate, no invented UI/reviews/awards. Dimensions are 320–3840 px and maximum side is at most twice the minimum side.

## Final quality gate

- [ ] Unit tests and release lint passed on the candidate.
- [ ] Fresh/offline launch, refresh failure, all topics/search, article body/source links, save/remove/share, About/contact/privacy checked.
- [ ] Back, browser return, rotation, large text, light/dark mode, background/resume and state restoration checked on actual app builds.
- [ ] Owner checks the phone-test build on a real phone; switching to a differently signed Play installation is planned before uninstalling any existing app with saved data.
- [ ] Signed release AAB installed from a Play testing track; inspect pre-launch report if generated and fix material issues.
- [ ] Recheck public URLs and declarations immediately before submission. Submit only when the above production gates are satisfied.

No production-signed AAB, completed Play declarations, testing eligibility or Google approval should be reported until corresponding evidence exists.
