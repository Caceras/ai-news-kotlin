# AI Brief — 4 October 2026

Three original explainers, with a distinct featured illustration for each:

| Article | Topic | Reading time |
| --- | --- | --- |
| Before an AI agent acts, decide what it may do | Builders | About 4 minutes |
| When AI answers from your files, follow the evidence | Research | About 4 minutes |
| What an image's Content Credentials can tell you | Policy | About 4 minutes |

The articles use original wording and link to primary documentation. They are
educational features, with hypothetical examples clearly identified. They do
not report a new product launch or claim that we tested a commercial product.
The SynthID discussion explicitly refers to Google's original 2023 explanation.

## Files

- `articles/`: editable Markdown manuscripts.
- `drafts/`: complete JSON submissions for the existing publishing workflow.
- `new-posts.json`: the three articles in the Android reader's block format.
- `ready-to-publish-posts.json`: the same articles plus all six existing posts.
- `editorial.json`: bylines, summaries, topics, image descriptions and claim checks.
- `source-checks.json`: source URLs, successful HTTP checks and retrieval hashes.
- `../../media/2026-10-04-*.webp`: web-ready featured images, approximately 150–230 KB each.

Every article includes source links and a disclosure of AI-assisted writing and
AI-generated featured artwork. Source checks were performed on 4 October 2026;
this is not a claim of independent reporting or human editorial review.

## Publish this edition

Image files need to be uploaded before the app is pointed to them. The prepared
payloads use the repository's public raw HTTPS addresses under `content/media/`.
Publication is complete only after those addresses return the actual images and
the main `content/posts.json` contains the new articles.

With working GitHub write access, commit and push the media and this edition,
verify the three image URLs, then replace `content/posts.json` with
`ready-to-publish-posts.json`, validate and push. The existing app receives the
edition on refresh after the content cache updates; no APK update is required.

Alternatively, upload the media first and run the existing Publish a briefing
workflow once for each file in `drafts/`. Choose one publication method. The
draft timestamps and IDs are consistent with the prepared edition, so an exact
repeat submission is rejected by the workflow's duplicate guard.

The active `content/posts.json` is kept intact until public image hosting is
verified. The presence of these files alone does not mean the edition is live.

## Artwork

The three images were generated for AI Brief using the image-generation tool on
4 October 2026. They use a consistent Nordic editorial palette of oak, ivory,
forest green and navy. No attached reference photograph, logo or third-party
image was used as an input asset. Each scene is conceptual artwork, including
the photographic prints shown in the provenance illustration.

The downloadable delivery includes original PNGs and optimised WebP copies.
Format conversion and resizing are for web delivery; they are not a claim that
the converted files contain verifiable C2PA credentials. Keep the supplied
AI-generated captions with the artwork when publishing it.
