# Getting AI Brief on your phone, and keeping it current

This is the guide for using the app, not for building it. It assumes you never
want to touch a branch, a pull request, or a build command — and you don't have to.

---

## The short version

1. You say what should change.
2. The change is written and verified — the full test and lint suite is run
   before anything is published, so a broken build never reaches your phone.
3. A few minutes later your phone shows **new build ready** at the top of the feed.
4. You tap **install**, and you're on the new version.

That's the whole loop. Step 1 is yours; the rest happens on its own. There are no
branches, no pull requests, and nothing for you to approve or merge.

---

## One-time setup

Two things to do, both on your phone, both only ever done once.

> **Already done:** the repository is public. That is what makes the rest free —
> unlimited build minutes, a download link that never asks you to log in, and
> in-app updates that need no password embedded in the app. The repository holds
> no passwords, no keys, and no personal data.

### 1. Install the app

Open this link on your phone:

```
https://github.com/Caceras/ai-news-kotlin/releases/latest/download/ai-brief.apk
```

Android will ask whether to allow installing apps from your browser. Allow it,
then open the downloaded file.

Bookmark that link. It always points at the newest build, so it also works as a
manual fallback if you ever need one.

### 2. Allow AI Brief to install its own updates

The first time you tap **install** in the app, Android asks whether AI Brief may
install apps. Grant it once. Afterwards every update is a single tap, and the
update continues automatically the moment you come back from that settings screen.

---

## How updating works

The app checks for a newer build when it starts and whenever you tap **home**.
If one exists, a single quiet line appears above the headlines:

```
new build ready  ·  2.1.0 (14)          install   later
```

- **install** — downloads the build and opens Android's installer. Your saved
  articles are kept.
- **later** — hides the line until the next check. Nothing is downloaded.

There is no forced update, no notification, and no interruption while reading.

### Confirming which build you're on

**about** → the bottom line shows `Version 2.1.0 (build 14)`. The build number
increases by at least one every time something is published, so it is the
quickest way to confirm a change actually reached your phone.

---

## Asking for changes

Describe what you want in plain language — what you saw, and what it should do
instead. Screenshots help but aren't required. Useful examples:

> The date column takes too much width on my phone, the headlines get squeezed.

> Tapping a saved article should open it, but nothing happens.

> I want the categories row gone from the top of the feed entirely.

You never need to mention files, branches, or code. If a request is ambiguous
enough that two readings would produce different apps, you'll be asked before
work starts rather than after.

---

## Publishing your own writing

The feed relays other people's reporting. You can also publish your own posts,
and those work on a completely different clock:

| You change | What you do | When it appears |
|---|---|---|
| A post | Tell me the post | Next time you tap **home** — seconds, no update |
| The app itself | Tell me what to change | A build, then one tap on **install** |

A post never needs a new build, because the app reads its posts straight from the
repository each time the feed refreshes.

### Asking for a post

Say what you want to publish and roughly how it should read. You do not need to
write it in any particular format — headings, quotes, bullets and images are all
available, and I put it into the right shape.

> Post something about small models running on-device. Open with why privacy is
> the real argument, pull out a quote about benchmarks not being the point, and
> end with three bullets on what is still missing.

### Scheduling one

Say when it should go live and it is published in advance but stays invisible
until then:

> Same post, but have it appear at 08:00 on Tuesday.

Nothing runs on a timer to make that work — the app simply never shows a post
that is not yet due — so a scheduled post cannot fail overnight.

### Changing or removing one

> Change the headline on the on-device models post.

> Take down the post from last Tuesday.

Both take effect on the next refresh.

---

## When something looks wrong

**The update line never appears.** The app only offers updates it can see. Check
`about` for your build number and compare it against the newest release on the
repository's Releases page. If they match, you're already current.

**The install fails.** Android refuses an update signed by a different key than
the installed app. This should not happen — every direct-install build is signed
with the same key on purpose. If it does, uninstall AI Brief and install fresh
from the link above. Saved articles are stored on the device and will be lost.

**The app shows old news.** Pull the feed by tapping **home**, which refreshes.
If the network is unavailable, the app deliberately shows the last stories it
cached rather than an empty screen.

---

## What this costs

Nothing. Public repositories get unlimited free build minutes, GitHub Releases
host the APK at no charge, and the app has no server behind it.

The one future cost is Google's **one-time $25 developer registration fee**,
payable only if and when you decide to publish on the Play Store. Everything
described on this page works without it, indefinitely.

---

## How this relates to the Play Store build

Two different builds come out of the same source:

| | Direct install (this guide) | Google Play |
|---|---|---|
| Build type | `sideload` | `release` |
| Signing key | Shared key in the repository | Private upload key, never committed |
| Self-updater | Included | Removed entirely |
| Optimisation | Identical | Identical |

They are deliberately identical in everything that affects how the app looks and
performs, so testing the direct-install build is a genuine test of what would ship.

The one thing to know: because the two are signed with different keys, a Play
Store install cannot upgrade over a direct-install one. On the day you publish,
uninstall the test build first. That is a single event, not a recurring chore.
