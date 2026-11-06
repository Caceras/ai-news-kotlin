#!/usr/bin/env python3
"""Validate the edition, or publish a draft supplied by the owner workflow."""
import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parents[1]
POSTS = ROOT / "content/posts.json"
CATEGORIES = {"RESEARCH", "PRODUCTS", "POLICY", "BUILDERS", "ALL"}
# Story formats and card themes the app knows. Each has one fixed look in the
# app (docs/DESIGN_SYSTEM.md); a post chooses one and never carries styling.
FORMATS = {"NEWS", "BREAKING", "EXPLAINER", "OPINION", "GUIDE", "SPONSORED", "REVIEW"}
THEMES = {"SKY", "LILAC", "MINT", "PEACH", "OCEAN", "INDIGO", "BLUSH", "NAVY"}
# Headline-style limits: short enough to read at a glance on a phone.
TITLE_LIMIT = 120
DECK_LIMIT = 220
KICKER_LIMIT = 24


def require(condition, message):
    if not condition:
        raise ValueError(message)


def text(value, label, limit=20000):
    require(isinstance(value, str) and value.strip(), f"{label} is required")
    require(len(value) <= limit, f"{label} is too long")
    return value.strip()


def https_url(value):
    value = text(value, "URL", 2048)
    parsed = urlsplit(value)
    require(parsed.scheme == "https" and parsed.hostname and not parsed.username,
            "Source and image URLs must be public HTTPS URLs without credentials")
    return value


def instant(value):
    date = datetime.fromisoformat(text(value, "Publication date").replace("Z", "+00:00"))
    require(date.tzinfo is not None, "Publication date needs a timezone")
    return date.astimezone(timezone.utc)


def validate(document):
    require(isinstance(document, dict) and document.get("version") == 1,
            "The edition must use version 1")
    posts = document.get("posts")
    require(isinstance(posts, list) and 1 <= len(posts) <= 200,
            "The edition must contain 1 to 200 posts")
    ids = set()
    for post in posts:
        require(isinstance(post, dict), "Each post must be an object")
        identifier = text(post.get("id"), "Post ID", 120)
        require(re.fullmatch(r"[a-z0-9][a-z0-9-]+", identifier), "Post ID is invalid")
        require(identifier not in ids, f"Duplicate post ID: {identifier}")
        ids.add(identifier)
        text(post.get("title"), "Headline", TITLE_LIMIT)
        text(post.get("summary"), "Deck", DECK_LIMIT)
        author = text(post.get("author"), "Author", 80)
        require(post.get("category") in CATEGORIES, "Unknown category")
        published = instant(post.get("publishedAt"))
        if post.get("imageUrl"):
            https_url(post["imageUrl"])
        validate_presentation(post, author, published)
        blocks = post.get("blocks")
        require(isinstance(blocks, list) and blocks, "A post needs a readable body")
        sources = 0
        body_length = 0
        for block in blocks:
            require(isinstance(block, dict), "Each block must be an object")
            kind = block.get("type")
            if kind in {"paragraph", "heading", "quote"}:
                body_length += len(text(block.get("text"), "Body text"))
            elif kind == "bullets":
                require(isinstance(block.get("items"), list) and block["items"], "Empty bullet list")
                body_length += sum(len(text(item, "Bullet")) for item in block["items"])
            elif kind == "video":
                https_url(block.get("url"))
                text(block.get("title"), "Video title", 200)
                if block.get("imageUrl"):
                    https_url(block["imageUrl"])
            elif kind in {"image", "link"}:
                https_url(block.get("url"))
                if kind == "link":
                    text(block.get("text"), "Source label", 200)
                    sources += 1
            else:
                raise ValueError(f"Unknown block type: {kind}")
        # These are our editorial safeguards, not Google Play word-count rules.
        require(body_length >= 120, "Write a useful body, not only a headline")
        require(sources > 0, "Include at least one source readers can check")
    if "extras" in document:
        validate_extras(document["extras"])
    return document


# A story that calls itself a demo is invented news. Google Play's News and
# misleading-content rules apply to everything the edition carries, so design
# previews belong in snapshots, never in the live edition.
PLACEHOLDER = re.compile(r"not real news|placeholder story|demo story|lorem ipsum", re.IGNORECASE)


def validate_presentation(post, author, published):
    """The rules each story format must meet before it can be published."""
    kind = post.get("format", "NEWS")
    prose = " ".join([post.get("title", ""), post.get("summary", "")] +
                     [block.get("text", "") for block in post.get("blocks") or []
                      if isinstance(block, dict) and isinstance(block.get("text"), str)])
    require(not PLACEHOLDER.search(prose), "Publish real stories only, not demos or placeholders")
    require(kind in FORMATS, f"Unknown story type: {kind}")
    if post.get("kicker") is not None:
        text(post["kicker"], "Kicker", KICKER_LIMIT)
    gallery = post.get("gallery", [])
    require(isinstance(gallery, list) and len(gallery) <= 2, "Add at most two extra collage pictures")
    for url in gallery:
        https_url(url)
    if gallery:
        require(kind == "BREAKING", "Only breaking news uses a picture collage")
        require(post.get("imageUrl"), "A collage needs a cover picture first")
    if post.get("authorImageUrl"):
        https_url(post["authorImageUrl"])
    if post.get("authorRole") is not None:
        text(post["authorRole"], "Author role", 80)
    if post.get("theme") is not None:
        require(post["theme"] in THEMES, f"Unknown card theme: {post['theme']}")
    if kind == "OPINION":
        require("editorial" not in author.lower(), "A column is signed by a named person, not the desk")
    if kind == "SPONSORED":
        # Google Play and readers both need paid content labelled with who paid.
        text(post.get("sponsor"), "Sponsor", 80)
    else:
        require(not post.get("sponsor"), "Only sponsored posts name a sponsor")
    if post.get("updatedAt"):
        require(instant(post["updatedAt"]) > published, "An update must come after publication")
    if kind == "REVIEW":
        require(isinstance(post.get("rating"), int) and 1 <= post["rating"] <= 5, "A review is rated 1 to 5 stars")
    else:
        require(post.get("rating") is None, "Only reviews carry a rating")
    if post.get("imageCredit") is not None:
        text(post["imageCredit"], "Image credit", 240)
    series = post.get("series")
    if series is not None:
        require(isinstance(series, dict), "A series must be an object")
        require(re.fullmatch(r"[a-z0-9][a-z0-9-]{0,59}", str(series.get("id", ""))), "Series ID is invalid")
        text(series.get("title"), "Series title", 60)
        count, part = series.get("of"), series.get("part")
        require(isinstance(count, int) and 2 <= count <= 12, "A series has 2 to 12 parts")
        require(isinstance(part, int) and 1 <= part <= count, "Series part is out of range")


def validate_extras(extras):
    """The front page's data modules must always say where their figures come from."""
    require(isinstance(extras, dict), "Extras must be an object")
    for name in ("rankings", "radar"):
        module = extras.get(name)
        if module is None:
            continue
        require(isinstance(module, dict), f"{name} must be an object")
        text(module.get("source"), f"{name} source", 80)
        https_url(module.get("sourceUrl"))
        entries = module.get("entries")
        require(isinstance(entries, list) and 1 <= len(entries) <= 20, f"{name} needs 1 to 20 entries")
        for entry in entries:
            text(entry.get("name"), f"{name} entry name", 200)
            if name == "radar":
                https_url(entry.get("url"))
            else:
                require(isinstance(entry.get("score"), (int, float)), "A ranked model needs a score")


def body_blocks(body):
    blocks = []
    # The public editor sends real newlines in JSON, including on a phone.
    for paragraph in re.split(r"\n\s*\n", text(body, "Article body")):
        paragraph = paragraph.strip()
        picture = re.fullmatch(r"!\[([^\]]*)\]\((https://[^)\s]+)\)", paragraph)
        if picture:
            caption = picture.group(1).strip()
            block = {"type": "image", "url": https_url(picture.group(2))}
            # GitHub names a pasted picture "image" or "Screenshot …"; that is not a caption.
            if caption and caption.lower() != "image" and not caption.lower().startswith("screenshot"):
                block["caption"] = caption
            blocks.append(block)
        elif paragraph.startswith("## "):
            blocks.append({"type": "heading", "text": paragraph[3:].strip()})
        elif paragraph.startswith("> "):
            blocks.append({"type": "quote", "text": paragraph[2:].strip()})
        elif all(line.startswith("- ") for line in paragraph.splitlines()):
            blocks.append({"type": "bullets", "items": [line[2:] for line in paragraph.splitlines()]})
        else:
            blocks.append({"type": "paragraph", "text": paragraph})
    return blocks


def iso(date):
    return date.replace(microsecond=0).isoformat().replace("+00:00", "Z")


def build_post(draft, identifier, date):
    sources = draft.get("sources")
    require(isinstance(sources, list) and sources, "Add at least one source")
    blocks = body_blocks(draft.get("body"))
    blocks.append({"type": "heading", "text": "Sources & further reading"})
    for source in sources:
        require(isinstance(source, dict), "Invalid source")
        url = https_url(source.get("url"))
        label = source.get("text") or urlsplit(url).hostname
        blocks.append({"type": "link", "text": text(label, "Source label", 200), "url": url})
    post = {
        "id": identifier, "title": text(draft.get("title"), "Headline", TITLE_LIMIT),
        "summary": text(draft.get("summary"), "Deck", DECK_LIMIT),
        "author": text(draft.get("author") or "AI Brief editorial", "Author", 80),
        "category": draft.get("category", "ALL"),
        "format": draft.get("format", "NEWS"),
        "publishedAt": iso(date),
        "blocks": blocks,
    }
    for key in ("imageUrl", "kicker", "gallery", "authorImageUrl", "authorRole", "theme", "sponsor",
                "sourceIssue"):
        if draft.get(key):
            post[key] = draft[key]
    return post


def write(document):
    validate(document)
    # Replacement is atomic; a failed validation never touches the live file.
    temporary = POSTS.with_suffix(".json.tmp")
    temporary.write_text(json.dumps(document, ensure_ascii=False, indent=2) + "\n")
    temporary.replace(POSTS)


def find(document, draft):
    """The published post this draft revises: by its ID, or by the issue it came from."""
    for index, post in enumerate(document["posts"]):
        if draft.get("id") and post["id"] == draft["id"]:
            return index
        if draft.get("sourceIssue") and post.get("sourceIssue") == draft["sourceIssue"]:
            return index
    return None


def publish(draft, now=None):
    """Adds a new post, or revises the one it names. Returns (post ID, "published" or "updated")."""
    require(isinstance(draft, dict), "The copied briefing is invalid")
    now = now or datetime.now(timezone.utc)
    document = json.loads(POSTS.read_text())
    existing = find(document, draft)
    if existing is None:
        date = instant(draft["publishedAt"]) if draft.get("publishedAt") else now
        title = text(draft.get("title"), "Headline", TITLE_LIMIT)
        slug = re.sub(r"[^a-z0-9]+", "-", title.lower()).strip("-")[:72] or "briefing"
        identifier = date.strftime("%Y-%m-%d-%H%M%S-") + slug
        require(all(p["id"] != identifier for p in document["posts"]),
                "This briefing is already published; edit it rather than creating a duplicate")
        document["posts"].insert(0, build_post(draft, identifier, date))
        write(document)
        print(f"Validated and added {identifier}")
        return identifier, "published"
    old = document["posts"][existing]
    published = instant(draft["publishedAt"]) if draft.get("publishedAt") else instant(old["publishedAt"])
    post = build_post(draft, old["id"], published)
    # A revision of something readers have already seen is dated; a scheduled
    # post that has not appeared yet is simply replaced.
    if published < now:
        post["updatedAt"] = iso(now)
    document["posts"][existing] = post
    document["posts"].sort(key=lambda p: p["publishedAt"], reverse=True)
    write(document)
    print(f"Validated and updated {old['id']}")
    return old["id"], "updated"


def unpublish(identifier=None, source_issue=None):
    document = json.loads(POSTS.read_text())
    index = find(document, {"id": identifier, "sourceIssue": source_issue})
    require(index is not None, "There is no published story to remove")
    removed = document["posts"].pop(index)
    write(document)
    print(f"Removed {removed['id']}")
    return removed["id"]


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--validate", action="store_true")
    args = parser.parse_args()
    try:
        if args.validate:
            document = validate(json.loads(POSTS.read_text()))
            print(f"Validated {len(document['posts'])} sourced posts")
        else:
            publish(json.loads(os.environ["POST_DRAFT_JSON"]))
    except (ValueError, KeyError, TypeError) as error:
        raise SystemExit(f"Cannot publish: {error}")
