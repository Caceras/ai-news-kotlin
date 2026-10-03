# AI Brief on your phone

Describe changes in plain language. Codex handles implementation, verification and the repository's release process, then sends a build-specific download link.

## Install and update the phone-test app

For the current prepared edition, [download phone-test version 114 from Drive](https://drive.google.com/file/d/13fyPtv6XPSJdiF5wdOuPhIZgYExjO4D4/view?usp=drivesdk). GitHub currently denies repository writes, so this version has not been published there.

After repository publication resumes, use **ai-brief.apk** from the verified build-specific GitHub release. The rolling fallback is:

https://github.com/Caceras/ai-news-kotlin/releases/latest/download/ai-brief.apk

Android may ask to allow installation from your browser. The direct-install test edition checks GitHub for updates at launch and when you tap Latest or Refresh. If a new build is offered, tap install and complete Android's installer. The existing sideload signing key is preserved so normal test updates retain saved articles.

If an update is refused, tell Codex the exact message and build number before uninstalling: saved articles are local and uninstalling deletes them. The app does not promise that a differently signed Play/internal-sharing installation can update the sideload one in place.

## Publish daily

The editor and authenticated workflow are prepared locally. They become available after GitHub repository writes are restored and the Pages site is deployed. See the launch plan for delivery status.

Tell Codex **“Publish this AI Brief article”** with your writing and checked source links. Add the author/topic and a publication time if you want to schedule it. There is no required writing format and no app update.

For independent publishing, use [the mobile editor](https://caceras.github.io/ai-news-kotlin/editor.html). Copy the completed briefing, open publishing, select Run workflow on main, paste into Briefing and run it. A green check confirms publication. Refresh the app after the content cache updates, which can take a few minutes.

The launch edition uses our own writing; external publisher feeds are disabled. Review any AI-assisted draft and link to primary sources. Use only cover images you own or have permission to publish. Scheduled text is public in the repository before its display time, so keep confidential drafts elsewhere.

## Reader controls

- Latest opens the edition. Refresh checks for new writing.
- The search action searches headlines and article bodies on your device.
- Topics filter Research, Products, Policy and Builders.
- Open a story, tap its bookmark, then find it in Saved.
- Source links open in your browser; Share uses Android's share sheet.
- About contains publisher, contact/corrections and privacy links, plus the installed version.

If offline, the app shows bundled or cached original writing and labels that state. Saved article bodies remain readable without an account.

## Google Play delivery

**ai-brief-internal-sharing.apk** is the separate file for Play Console's Internal app sharing upload page. It has no self-updater; Google re-signs uploads on that route. It is not a production-track artifact.

A Play testing/production release uses a **signed AAB** with the correct private upload key and Play App Signing. The public test key is never the production upload key. See [the launch plan](PLAY_LAUNCH_PLAN.md) for today's account actions, closed-testing rules, declarations and the submission gate.
