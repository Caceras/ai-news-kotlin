# Publishing original posts

The launch edition carries original writing published by AI Brief. External RSS
feeds are disabled until their reuse permissions are checked.

**Delivery status (4 October 2026):** the [publishing workflow](https://github.com/Caceras/ai-news-kotlin/actions/workflows/publish-post.yml), nine-article edition and featured image files are live on GitHub. The mobile editor is committed under `docs/editor.html`; GitHub Pages activation is still denied with HTTP 403 because the connection lacks the required site-settings access. Use the workflow directly while Pages is unavailable.

## Publish without editing code

Tell Codex **“Publish this AI Brief article”** with your text, byline, topic and
checked source links. Codex validates and commits the edition; no Android update
is needed. You can also use [the mobile editor](https://caceras.github.io/ai-news-kotlin/editor.html):
copy the completed briefing, open publishing, choose Run workflow on main, paste
into Briefing and run it. A green check confirms publication.

The workflow validates before writing and rejects duplicate submissions and
invalid source links. The web editor prepares clipboard data only; publication
requires the owner's GitHub access. Readers need no account.

Keep no more than 200 posts in the active edition. Ask Codex to archive older writing when needed; Git history and previously saved device copies can retain it.

The sections below describe the content format for developers.

---

## The one thing worth knowing

A post is a change to a single file — `content/posts.json` — and nothing else.

The app reads that file directly from the repository every time the feed
refreshes. No build runs, no release is cut, and nothing is installed on the
phone. Publish a post and it appears on a refresh once GitHub’s content cache has updated, which can take a few minutes.

That splits the project into two speeds, which is the whole point:

| Changing | What happens | How long |
|---|---|---|
| **What the app says** — a post | A file is committed | After refresh and content-cache propagation, no update |
| **What the app is** — code, layout, behaviour | A build, then one tap on `install` | Minutes |

---

## Scheduling

`publishedAt` is both the date shown on the story and its release time.

A post dated in the future is carried in the file but withheld until that moment
passes, after which it appears on its own. Nothing runs on a schedule to make
this happen — the app simply never shows a post that is not yet due — so
scheduling needs no background server task. Device time and a later refresh still matter.
Future-dated writing is already public in the repository; keep confidential drafts outside it.

Dates are UTC, in the form `2026-08-30T16:00:00Z`.

---

## What a post looks like

```json
{
  "version": 1,
  "posts": [
    {
      "id": "2026-08-30-welcome",
      "title": "The headline, shown in the feed and at the top of the post.",
      "summary": "One or two lines of standfirst, shown under the headline.",
      "author": "AI Brief editorial",
      "category": "PRODUCTS",
      "publishedAt": "2026-08-30T16:00:00Z",
      "blocks": [
        { "type": "paragraph", "text": "Explain the evidence in your own words, say what it means for a reader, and distinguish a measured result from a claim. Review the source before publishing." },
        { "type": "link", "text": "Primary source: NIST", "url": "https://www.nist.gov/itl/ai-risk-management-framework" }
      ]
    }
  ]
}
```

| Field | Required | Notes |
|---|---|---|
| `id` | ✅ | Any stable string. Dating it keeps the file readable. Changing it republishes the post as a new story. |
| `title` | ✅ | |
| `publishedAt` | ✅ | UTC instant. Also the schedule — see above. |
| `summary` | ✅ | Standfirst under the headline. |
| `author` | ✅ | Byline or publication name. |
| `category` | | `RESEARCH`, `PRODUCTS`, `POLICY`, `BUILDERS`, or `ALL`. Anything unrecognised falls back to `ALL`. |
| `imageUrl` | | Must be `https://`. |
| `blocks` | ✅ | A readable body and at least one HTTPS source link. |

A post missing `id`, `title`, or a valid `publishedAt` is skipped rather than
shown broken. `app/src/test/.../PublishedPostsFileTest.kt` parses the real file
on every build and fails if a post would silently vanish, so a bad edit is
caught during Android validation. The publishing workflow also validates before committing; direct JSON edits bypass that pre-publication gate.

---

## Block types

Blocks are the body of a post. Each one renders in type the app already defines,
so a post can add new writing but never new styling — which is what keeps every
post looking like it belongs.

```json
{ "type": "paragraph", "text": "Body copy." }

{ "type": "heading",   "text": "A section heading." }

{ "type": "link",      "text": "Primary source", "url": "https://example.com/source" }

{ "type": "quote",     "text": "A pulled quote.",
                       "attribution": "Optional line beneath it" }

{ "type": "bullets",   "items": ["First", "Second", "Third"] }

{ "type": "image",     "url": "https://example.com/picture.jpg",
                       "caption": "Optional caption" }
```

Rules the parser enforces:

- **Empty text is dropped.** A paragraph of spaces renders nothing, so it is
  removed rather than left as a gap.
- **Images must be HTTPS.** The app downloads and displays them, so a plaintext
  URL is refused for the same reason the updater refuses one.
- **Unknown block types are skipped, and the rest of the post still renders.**
  This matters because posts are fetched at runtime: a phone on an older build
  can be handed a post using a block type that build predates. Skipping it
  degrades gracefully instead of failing the whole post.

---

## Images

Images need a stable HTTPS address. The simplest home for them is this
repository — commit the file under `content/media/` and reference it as:

```
https://raw.githubusercontent.com/Caceras/ai-news-kotlin/main/content/media/<file>
```

That costs nothing and needs no image host or account.

---

## How a post reads differently from a relayed story

An original post has no external article behind it, so the reader adapts:

- The **read the original source** button is hidden — there is nothing to open.
- The footer reads *Written for AI Brief* rather than the attribution notice
  used for relayed reporting.
- The body is the post's blocks rather than a summary of someone else's piece.

Original posts are merged into the same feed as the news, ordered by date, and
are deliberately added **before** the article cap is applied — a busy news day
can never push your own writing out of the feed.

---

## If the file is broken

A malformed `content/posts.json` yields no posts rather than an error: the feed
is still full of live reporting, and a broken post file must not be able to take
the app down. The cost of that safety is silence, which is exactly why the build
parses the real file and fails loudly instead.
