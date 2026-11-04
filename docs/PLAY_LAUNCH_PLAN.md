# AI Brief: account rescue and first Play release

Prepared 3 October 2026 for `Caceras/ai-news-kotlin`, package **`com.caceras.aibrief`**.

The immediate goal is to complete Google's inactivity actions before the account's cutoff. The production goal is a useful, regularly updated native publication with accurate declarations and evidence of testing. No developer can promise first-review acceptance, but the plan below addresses the specific avoidable rejection risks in this repository.

## Delivery status

As of 4 October 2026, the redesigned source and publishing workflows are published on GitHub, with a nine-article edition and verified public featured images for the three new features. The refined version 2.2.1 / 114 internal-sharing APK is available in Drive. It uses the compact editorial layout, bundled Newsreader/DM Sans fonts and photographic topic artwork. The contact/privacy pages and daily editor are committed under docs, but GitHub Pages activation still returns HTTP 403 (`Resource not accessible by integration`); the connection lacks site-settings access. The owner must enable Pages from main, /docs, and the public URLs must be verified before production. No Play Console upload or account-warning clearance is claimed.

## 1. Do these account actions today

1. Sign in to the account showing the warning. In **Developer account > Account details**, verify the **contact email and phone number**, then **Save**. Save evidence of both verified states.
2. If AI Brief has no entry in this account yet, create it in Play Console and complete its initial setup first. Then download the prepared **[AI Brief Internal app sharing APK](https://drive.google.com/file/d/1hxK1ojphsmx3oTsHuyB97-J_KtF_Tkbt/view?usp=drivesdk)** from your Drive. Upload that file through [Internal app sharing](https://play.google.com/console/internal-app-sharing), then select **Confirm upload**. Save the upload confirmation, timestamp and resulting link.
3. Return to **Policy status > Issue details** and check the inactivity notice. Keep the closure email: its exact cutoff controls, and the screenshots show only a date. Do not assume a particular midnight timezone.
4. If the warning persists, Internal app sharing is unavailable, or verification fails, use Play Console Help to contact developer support immediately with the account ID, closure notice, verification evidence and upload receipt. Use the draft below. Do not rely on an unanswered support request as a granted extension.
5. Separately inspect **Android developer verification** for this exact package. The screenshot's `Com.caceras` draft is a different package. Do not rename this app to match it, replace its key, or delete the draft to address inactivity.

**Why an internal upload is an appropriate deadline action:** Google's [inactive-account guidance][1] requires both contact verification and an app upload/publication action. It explicitly says: “If you’re not ready to make your app public, use Internal app sharing, Internal testing, or Closed testing…” and then instructs developers to upload a new bundle or APK. This is more useful for today's deadline than waiting for a public production review.

**What that action proves:** an internal upload is evidence that the action named in the inactivity guidance was completed. It is not production approval and does not guarantee the notice has been cleared; verify the actual account state. A local build, GitHub release, draft listing, or uploaded verification certificate alone does not complete the two inactivity tasks.

Internal app sharing accepts APKs or bundles signed with **any key** and Google re-signs them with its own internal-sharing certificate [2]. This is why the prepared APK can be uploaded without choosing a production key. Internal-sharing uploads cannot be promoted into testing-track or production releases. Its links expire after 60 days and allow up to 100 downloads; testers may need access to the app's Play listing/testing track to install [2]. Use the normal GitHub phone-test APK for direct phone testing meanwhile.

If Internal app sharing cannot be completed, the alternative is a properly signed **AAB on Internal testing**. That requires the correct private upload key and Play App Signing setup. Internal testing can start before the full app setup is complete and normally makes an uploaded release available quickly, although first links/changes can take several hours [3]. Save the rollout receipt, not merely an unsent draft.

### Developer-support draft

> My developer account shows an inactivity closure deadline of 3 October 2026. Account ID: [ID]. I verified the contact email and phone number at [time and timezone] and confirmed an Internal app sharing upload for com.caceras.aibrief at [time and timezone]. Upload receipt/link: [evidence]. Your inactive-account Help article permits Internal app sharing when an app is not ready for public release. The warning currently [describe the actual state]. Please confirm whether the required inactivity actions are recognized and advise how to resolve any remaining issue before the cutoff. I attach the closure notice and verification/upload confirmations.

Only claim actions that have actually completed. This is a draft for the owner to send; no support message has been sent by Codex.

## 2. Choose the production path from the account's actual status

| Account or app condition | Production path | Can a new production launch be promised today? |
|---|---|---|
| An existing published app is already on this account | Update that app with its established package and signing identity if it is the appropriate app. AI Brief remains a separate app unless its identity already matches. | Review still controls timing. |
| An older personal account or an organization account with production access | Complete app setup, internal testing and declarations, then submit the signed AAB. | No. Google says some reviews take up to seven days or longer [5]. |
| Personal account created after **13 November 2023** without production access | Run a **closed test with at least 12 testers opted in continuously for 14 days**, then apply for production access [4]. Recruit real intended users and retain their feedback. | No, unless the required test and access approval have already completed. |
| Identity or package verification is incomplete | Finish the required verification in Console as a separate workstream. | A build cannot fix verification. |

Internal sharing and Internal testing do not substitute for the required 12-person closed test. Internal testers must leave the internal test before joining the closed test; they are not automatically counted in both [3]. Recruit extra testers to allow for dropouts, keep at least 12 continuously opted in, and record real engagement, issues and fixes. Do not buy superficial opt-ins or fabricate the production-access questionnaire.

The [current verification guide][6] says existing verified Play identities usually satisfy identity verification, 99% of Play packages are automatically registered, and new Play app creation registers and links its package. The [current policy-deadlines page][7] also specifies **30 September 2026** for registering remaining Play packages to avoid global Play removal. Outside-Play install enforcement begins in Brazil, Indonesia, Singapore and Thailand on that date, with broader rollout later [8]. Check the Console state for the actual package; registration and inactivity are separate requirements.

## 3. The launch product

**AI Brief is an independent AI publication, not an in-app generative chatbot.** The first edition contains six original, dated, sourced explainers. The publisher can add original news briefings daily. Sources open in a browser; complete original article bodies are readable natively and can be saved offline.

The two visual references inform the design: serif headlines, an image-led feature, compact thumbnail rows, a horizontal topic strip, off-white page surfaces and navy editorial panels. The brand and artwork are AI Brief's own; neither reference's logo nor its photographs is included.

| First-release feature | Value to the reader | Implementation |
|---|---|---|
| Featured article and editorial rows | A clear starting point and readable browsing | Native Jetpack Compose; full article titles and dates |
| Topic filters and text search | Find useful reading within Research, Products, Policy and Builders | Search stays on the device and includes article bodies |
| Original article reader and linked sources | Useful content without leaving the app; evidence can be checked | Typed paragraph/heading/quote/list/link blocks |
| Saved shelf | Keep original article text without an account | Private local storage; no sync or tracking |
| Bundled and cached editions | Useful on a first offline installation and after connection failures | Bundled posts plus a cached editorial document |
| Refresh and accurate offline label | Distinguish downloaded writing from the offline edition | Explicit refresh action and cached/offline status |
| Publisher, corrections, contact and privacy | Transparency and a way to resolve errors | About screen and public HTTPS pages |

These features address Google's [limited-functionality policy][9]: stable, responsive apps need meaningful content and mobile utility. A native reader is not automatically exempt from that policy; the full experience must actually work. The six launch explainers are an editorial readiness choice, not a Google-mandated article count or word minimum.

External RSS feeds are disabled for the launch edition. The existing feed implementation is preserved, but use it only after checking the relevant publishers' reuse permissions and updating privacy/Data safety disclosures. A public RSS endpoint and source attribution do not independently grant rights to republish article text, logos or photos [10]. The launch artwork is an original AI-generated illustration; the reader labels it as illustration rather than a photograph of an event.

## 4. Daily publishing without rebuilding

Open [the editor](https://caceras.github.io/ai-news-kotlin/editor.html). Fill in the headline, introduction, byline, topic, body and source links. Copy the briefing, open publishing, choose **Run workflow** on **main**, paste into the Briefing field and run it. A green workflow result confirms the commit. Refresh the app after GitHub's content cache updates, which can take a few minutes.

An equally simple option is to tell Codex: **“Publish this AI Brief article,”** followed by the text, author, topic and checked source links. Codex can validate and commit the editorial file through the connected repository. A content-only change does not trigger the Android release workflow.

The publisher uses GitHub authentication; readers need no account. There is no publishing credential or API key in the app or public editor. The web editor only prepares a clipboard payload. An authenticated workflow validates the writing and source links before atomically updating `content/posts.json`.

| Daily routine | Suggested approach |
|---|---|
| Find a story | Start with an original paper, documentation, model card, official release or regulator statement. |
| Write | Explain what changed, why it matters, the evidence and the uncertainty in your own words. A useful 250–600-word briefing is an editorial target, not a Play requirement. |
| Verify | Open the links, check names/dates/figures and distinguish announcements from measured results. Review any AI-assisted draft before publishing. |
| Publish | Use the editor/workflow or ask Codex to publish. No APK, AAB or Play submission is needed for a normal article update. |
| Check | Refresh the app, open the article, inspect the byline/date and test its source links. |
| Maintain | Correct errors in the same article, answer support mail, and keep publication current. Archive older work with truthful dates. |

Scheduling uses an optional publication timestamp converted to UTC. A scheduled article appears on a later app refresh once it is due; it is not a push notification. Future-dated content is already public in this public repository, so keep confidential/embargoed drafts outside it until publication is appropriate. Previously saved article copies can remain on readers' devices after an editorial correction.

For current-events/news apps Google expects regularly updated content **not more than 30 days old**; other news/magazine apps should have regularly updated content **not more than three months old** [11]. The latest edition needs a real maintenance routine. Do not keep an old offline fallback labeled as live news or re-date old writing to simulate an update.

## 5. Console declarations and listing

Use [STORE_LISTING.md](STORE_LISTING.md) for ready-to-copy listing and reviewer text, and [PLAY_RELEASE_CHECKLIST.md](../PLAY_RELEASE_CHECKLIST.md) for the final submission gate.

| Field | Recommended entry or required action |
|---|---|
| App name | **AI Brief** |
| Package | **com.caceras.aibrief**; preserve it |
| App category | **News & Magazines**; complete the news self-declaration truthfully [11,12] |
| Publisher/contact URL | **https://github.com/Caceras/ai-news-kotlin/blob/main/CONTACT.md**; use the same URL in the news declaration and in-app contact link |
| Support email | **rikicaceras@gmail.com**, already used in the repository; owner must confirm it is monitored |
| Privacy URL | **https://github.com/Caceras/ai-news-kotlin/blob/main/PRIVACY.md**; active, public, non-geofenced HTML with no login |
| Ads | **No** for the current code; there are no ad SDKs |
| App access | All reader functionality is available without login or special credentials. The editorial management account is outside the reader app. |
| Target audience | Recommend adults interested in AI research, tools and policy. Choose actual intended ages honestly; selecting adults does not override the app's content or presentation. |
| Content rating | Complete the IARC questionnaire from actual article content and functionality; do not guess a rating or invent missing answers. |
| News declaration | Complete every question about the publication and parent entity using real owner details. Original/third-party sources, dates, contact details and update frequency must match the app. |
| Other declarations | Complete every relevant “Needs attention” item presented by Console. Do not declare features such as health functionality or generative AI interaction merely because an explainer mentions them. |

### Data safety: complete from evidence, not a slogan

The earlier checklist's unconditional “Select No” was unsafe. The code has no accounts, ads, analytics, location access, advertising identifiers or uploaded bookmarks/search queries. However, downloading the public edition exposes **IP address and request metadata to GitHub**, and custom cover images can introduce another host. Google's [Data safety guidance][13] says off-device transmission includes third-party handling and that IP declarations depend on actual use. Ephemeral processing still belongs in the form; it is not automatically “no collection.”

Before a closed/open/production release, review GitHub's hosting practices and any selected image host, classify any in-scope data and purposes, and make the form match the privacy policy. Do not claim that normal HTTPS encrypts data from the receiving host, that IP addresses never leave the phone, or that provider logs are deleted when someone uninstalls. Use the service-provider sharing exception only if its conditions actually apply. Reader-initiated sharing/email have separate user-initiated-action rules [13].

The updated privacy policy discloses local storage, hosting requests, external links, support correspondence, retention/deletion and the difference between the Play edition and the sideload updater. All apps still need an accessible privacy policy even if the final form correctly reports no in-scope user-data collection [14]. No reader accounts are created, so an account-deletion flow is not applicable to the current reader. Do not add an invented account just to fill a Console field.

Apps exclusively on Internal testing are exempt from the Data safety section [13]; that exemption is useful for testing today and is not a production exemption.

## 6. Signed production artifact

The repository retains three distinct delivery identities:

| Build | Purpose | Signing | Package installer/updater |
|---|---|---|---|
| `sideload` | Update the existing phone-test app through GitHub | Existing stable low-trust test key; preserved | Included for this direct-install channel |
| `internalSharing` | Upload today through the Internal app sharing page | Local debug/test certificate; Google re-signs the uploaded artifact | Excluded |
| `release` | Internal/closed/open testing tracks and production | **Private Play upload key** plus Play App Signing | Excluded |

The Play release is an **AAB**, not the GitHub sideload APK. For a new Play app, choose and securely back up its private upload key and enroll in Play App Signing. For an existing Play identity, use its established upload key or Google's upload-key reset process; do not generate an arbitrary replacement. A signing certificate's public fingerprint or certificate file is not its private key.

The manual **Build signed Play bundle** workflow produces a signed AAB after four repository secrets have been configured: `PLAY_KEYSTORE_BASE64`, `PLAY_STORE_PASSWORD`, `PLAY_KEY_ALIAS`, `PLAY_KEY_PASSWORD`. Secrets are consumed as environment data, signing files are removed at the end, and the AAB is kept as an Actions artifact rather than being silently published to Play. The local alternative remains `keystore.properties` plus `bundleRelease`.

Use a version code above every previously uploaded Play version for this package. The current phone-test version is 113; the signed Play workflow accepts an explicit code checked against Console. Google Play monotonicity applies to prior Play uploads, not to an unrelated package. Switching between differently signed sideload, internal-sharing and Play installations cannot be treated as a normal in-place update; test on a separate device or plan local-data migration before switching channels.

Current compile/target SDK is **36**, minimum SDK is **26**. Google's [target-API page][15] requires Android 16/API 36 for ordinary new app/update submissions from **31 August 2026**. The artifact also needs 16 KB page compatibility: current Android guidance describes the Play requirement and an update block from **1 February 2027** [16]. This build's native AndroidX graphics-path libraries have 16 KB ELF alignment and its APK passes 16 KB zip-alignment verification. This static check is not a claim of execution on a physical 16 KB device.

## 7. First-review quality gate

Google's [core app quality guidelines][17] inform the practical checks below. Complete the tests on the actual release candidate, not a mock screenshot or a different package variant.

1. **Install and cold launch:** fresh install, first offline launch, slow/no network, unavailable editorial endpoint. A readable bundled edition must remain usable, without a crash or a misleading live label.
2. **Main task:** open the feature, browse remaining articles, filter every topic, search by title/body, read source links, share, save and remove an article. The reader must deliver real content rather than an empty placeholder.
3. **State:** return from a browser, rotate, switch tabs, use Back, background/resume, and recreate the process. Retain saved articles and appropriate reading/navigation state.
4. **Layout and accessibility:** a compact phone, a larger window/landscape, light/dark mode, large text and TalkBack. Check readable contrast, meaningful labels, selected-tab state, at least 48 dp action targets, edge-to-edge insets and absence of clipped controls.
5. **Release content:** dates/bylines/source links are present; offline explainers are labeled; contact and privacy links load in a signed-out browser; artwork is owned/licensed and illustrations are truthful.
6. **Artifact:** correct package/version/target SDK, expected certificate, valid signature, no package-install permission in the Play edition, compatible native libraries. Compare the exact artifact checksum and source commit before handing it over.
7. **Play evidence:** upload the production-signed AAB to a testing track, wait for a [pre-launch report][18] if generated, and resolve crashes/ANRs, accessibility failures and obvious crawler errors before production. Google runs these tests subject to lab capacity; no generated report has yet been claimed here.
8. **Real users:** record testing feedback and fixes. For a qualifying new personal account, complete the genuine 12-tester/14-day closed test and production-access review before submitting production.

Pre-launch automation does not establish editorial accuracy, licensing, accurate Data safety declarations, or every real-device interaction. The owner should also install the phone-test APK on an actual phone and try the reader before public launch.

## 8. Submission order and realistic timing

**Today:** complete the two inactivity tasks and package-verification checks; keep receipts. Do not wait for final production artwork or a long closed test before completing the account actions that Google explicitly permits through internal upload.

**Next:** check the tested design/content on a real phone, confirm publisher/contact details and image rights, settle hosting/Data safety declarations, configure the correct private upload key, and build the signed AAB. Upload it to Internal testing, verify the test install and inspect pre-launch feedback.

**Then:** finish store metadata, actual screenshots and all required declarations. If the account requires the 14-day closed test, run it with engaged testers, fix reported issues, and apply for production access. Otherwise submit production once the gates pass.

**After approval:** use the approved artifact for rollout, monitor Android vitals and reviews, keep the contact mailbox monitored, and continue publishing original dated content. Apply future code changes as signed app updates; normal article updates use the editorial workflow.

Do not promise same-day production review. Google states reviews can take **up to seven days or longer** for some accounts [5], and production-access applications normally take seven days or less but can take longer [4]. Repeatedly sending a changed candidate before its checks are complete adds avoidable uncertainty. The best first submission is the smallest useful edition with complete, honest declarations and evidence of real testing.

## Official sources reviewed

Sources were retrieved on 3 October 2026. Recheck policy pages and the account's Console tasks immediately before public submission because dates and account-specific obligations can change.

| Reference | Official source | Used for |
|---|---|---|
| [1] | [Closure of inactive developer accounts][1] | Both required actions; internal-upload allowance |
| [2] | [Share app bundles and APKs internally][2] | Any signing key, re-signing, expiry, upload/install limits |
| [3] | [Set up an open, closed, or internal test][3] | Track differences, test availability, internal/closed opt-in |
| [4] | [Testing requirements for new personal accounts][4] | 12 testers, 14 continuous days, engagement/access review |
| [5] | [Publish your app][5] | Review timing and publishing statuses |
| [6] | [Register on Google Play Console][6] | Identity and exact-package registration |
| [7] | [Policy deadlines][7] | 30 September 2026 registration deadline |
| [8] | [Android developer verification][8] | Regional outside-Play enforcement timeline |
| [9] | [Functionality, Content, and User Experience][9] | Useful content, stability and minimum functionality |
| [10] | [Intellectual Property][10] | Rights to text, branding and images |
| [11] | [Requirements for news and magazine apps][11] | Contact URL, dates, attribution, content freshness |
| [12] | [News & Magazines policy][12] | News declaration and publication transparency |
| [13] | [Data safety form guidance][13] | Off-device data, IP usage, sharing exceptions, internal-test exemption |
| [14] | [User Data policy][14] | Privacy link/text, retention/deletion and account applicability |
| [15] | [Target API level requirements][15] | API 36 deadline for 2026 |
| [16] | [Support 16 KB page sizes][16] | Native-library and packaging compatibility |
| [17] | [Core app quality guidelines][17] | Lifecycle, navigation, accessibility, layout and reliability |
| [18] | [Pre-launch reports][18] | Device-lab checks and their limits |
| [19] | [Preview asset requirements][19] | Icon, feature graphic, screenshot requirements vs recommendations |
| [20] | [AI-Generated Content policy][20] | Distinguish AI-topic editorial reading from in-app prompt-driven generation |
| [21] | [Prepare your app for review][21] | App content declarations, access and review information |

[1]: https://support.google.com/googleplay/android-developer/answer/11605267?hl=en
[2]: https://support.google.com/googleplay/android-developer/answer/9844679?hl=en
[3]: https://support.google.com/googleplay/android-developer/answer/9845334?hl=en
[4]: https://support.google.com/googleplay/android-developer/answer/14151465?hl=en
[5]: https://support.google.com/googleplay/android-developer/answer/9859751?hl=en
[6]: https://developer.android.com/developer-verification/guides/google-play-console
[7]: https://support.google.com/googleplay/android-developer/answer/9934569?hl=en
[8]: https://developer.android.com/developer-verification
[9]: https://support.google.com/googleplay/android-developer/answer/9898783?hl=en
[10]: https://support.google.com/googleplay/android-developer/answer/9888072?hl=en
[11]: https://support.google.com/googleplay/android-developer/answer/10523915?hl=en
[12]: https://support.google.com/googleplay/android-developer/answer/9935326?hl=en
[13]: https://support.google.com/googleplay/android-developer/answer/10787469?hl=en
[14]: https://support.google.com/googleplay/android-developer/answer/10144311?hl=en
[15]: https://support.google.com/googleplay/android-developer/answer/11926878?hl=en
[16]: https://developer.android.com/guide/practices/page-sizes
[17]: https://developer.android.com/docs/quality-guidelines/core-app-quality
[18]: https://support.google.com/googleplay/android-developer/answer/9842757?hl=en
[19]: https://support.google.com/googleplay/android-developer/answer/9866151?hl=en
[20]: https://support.google.com/googleplay/android-developer/answer/13985936?hl=en
[21]: https://support.google.com/googleplay/android-developer/answer/9859455?hl=en
