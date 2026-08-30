# Publishing original posts

AI Brief carries two kinds of story. Most are relayed from public news feeds.
The rest are **original posts**, written for the app itself, and this is how they
are published.

---

## The one thing worth knowing

A post is a change to a single file — `content/posts.json` — and nothing else.

The app reads that file directly from the repository every time the feed
refreshes. No build runs, no release is cut, and nothing is installed on the
phone. Publish a post and it is there on the next refresh.

That splits the project into two speeds, which is the whole point:

| Changing | What happens | How long |
|---|---|---|
| **What the app says** — a post | A file is committed | Seconds, no update |
| **What the app is** — code, layout, behaviour | A build, then one tap on `install` | Minutes |

---

## Scheduling

`publishedAt` is both the date shown on the story and its release time.

A post dated in the future is carried in the file but withheld until that moment
passes, after which it appears on its own. Nothing runs on a schedule to make
this happen — the app simply never shows a post that is not yet due — so
scheduling costs nothing and cannot fail while you are asleep.

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
      "author": "Rikard Caceras",
      "category": "PRODUCTS",
      "publishedAt": "2026-08-30T16:00:00Z",
      "blocks": [
        { "type": "paragraph", "text": "..." }
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
| `summary` | | Standfirst under the headline. |
| `author` | | Omit for no byline. |
| `category` | | `RESEARCH`, `PRODUCTS`, `POLICY`, `BUILDERS`, or `ALL`. Anything unrecognised falls back to `ALL`. |
| `imageUrl` | | Must be `https://`. |
| `blocks` | | The body. Without it the post opens as a blank page. |

A post missing `id`, `title`, or a valid `publishedAt` is skipped rather than
shown broken. `app/src/test/.../PublishedPostsFileTest.kt` parses the real file
on every build and fails if a post would silently vanish, so a bad edit is
caught before it reaches the phone.

---

## Block types

Blocks are the body of a post. Each one renders in type the app already defines,
so a post can add new writing but never new styling — which is what keeps every
post looking like it belongs.

```json
{ "type": "paragraph", "text": "Body copy." }

{ "type": "heading",   "text": "A section heading." }

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
