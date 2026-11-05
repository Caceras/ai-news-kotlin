# Publishing original posts

The launch edition carries original writing published by AI Brief. External RSS
feeds are disabled until their reuse permissions are checked.

**Delivery status (4 October 2026):** the [publishing workflow](https://github.com/Caceras/ai-news-kotlin/actions/workflows/publish-post.yml), nine-article edition and featured image files are live on GitHub. The mobile editor is committed under `docs/editor.html`; GitHub Pages activation is still denied with HTTP 403 because the connection lacks the required site-settings access. Use the workflow directly while Pages is unavailable.

## Publish from your phone: the issue form

This is the newsroom while the app is on Google Play. No app update is needed.

1. Open **[New issue → Publish a story](https://github.com/Caceras/ai-news-kotlin/issues/new?template=publish-story.yml)**
   in the GitHub app or a browser.
2. Pick the story type (News, Breaking, Explainer, Opinion, Guide, Sponsored), the topic, and fill in the
   headline, deck, story and sources. Paste pictures straight into the Pictures, Author photo or Story fields.
3. Submit. The **Publish from issue** workflow checks the story, saves the pictures (resized, as WebP, with
   location and camera data removed) into `content/media/`, commits `content/posts.json`, comments
   ✅ or ❌ with the reason, and closes the issue.
4. Readers see it on their next refresh, usually within five minutes.

**To correct a story,** edit that issue and save; the same story is updated and dated "Updated …".
**To take it down,** edit the issue and set **Action** to **Unpublish**.
**To schedule,** fill in **Publish at** with a UTC time.

Only issues opened and edited by the repository owner are acted on. Anyone else's are ignored.

Each story type has one fixed look, described in [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md). The checks that stop a
story are: headline up to 120 characters, deck up to 220, kicker up to 24, at least one source link, a named
person on every Opinion column, a named sponsor on every Sponsored post (and only there), a collage only on
Breaking news (up to three pictures).

The older **Publish a briefing** workflow (paste JSON from `docs/editor.html` into Run workflow) still works.

Keep no more than 200 posts in the active edition. Ask Claude to archive older writing when needed; Git
history and previously saved device copies keep it.

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
| `imageUrl` | | Must be `https://`. The cover. |
| `format` | | `NEWS` (default), `BREAKING`, `EXPLAINER`, `OPINION`, `GUIDE`, `SPONSORED`. Unknown values read as `NEWS`. |
| `kicker` | | Red lead-in before the headline, at most 24 characters. |
| `gallery` | | Breaking only: up to two more `https://` pictures for the collage. |
| `author`, `authorRole`, `authorImageUrl` | | Columns show the portrait and role. |
| `theme` | | Guide and Sponsored card colour: `SKY`, `LILAC`, `MINT`, `PEACH`, `BLUSH`, `OCEAN`, `INDIGO`, `NAVY`. |
| `sponsor` | Sponsored only | Who paid. A sponsored post without it is never shown. |
| `updatedAt` | | Set automatically when a story is revised. |
| `sourceIssue` | | The issue a story was published from; editing that issue revises it. |
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

In the issue form and the editor, a picture on its own line in the story
(`![caption](https://…)`) becomes an image block.

```
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
