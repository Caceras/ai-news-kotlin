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
        text(post.get("title"), "Title", 180)
        text(post.get("summary"), "Summary", 500)
        text(post.get("author"), "Author", 120)
        require(post.get("category") in CATEGORIES, "Unknown category")
        instant(post.get("publishedAt"))
        if post.get("imageUrl"):
            https_url(post["imageUrl"])
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
    return document


def body_blocks(body):
    blocks = []
    # The public editor sends real newlines in JSON, including on a phone.
    for paragraph in re.split(r"\n\s*\n", text(body, "Article body")):
        paragraph = paragraph.strip()
        if paragraph.startswith("## "):
            blocks.append({"type": "heading", "text": paragraph[3:].strip()})
        elif paragraph.startswith("> "):
            blocks.append({"type": "quote", "text": paragraph[2:].strip()})
        elif all(line.startswith("- ") for line in paragraph.splitlines()):
            blocks.append({"type": "bullets", "items": [line[2:] for line in paragraph.splitlines()]})
        else:
            blocks.append({"type": "paragraph", "text": paragraph})
    return blocks


def publish(draft):
    require(isinstance(draft, dict), "The copied briefing is invalid")
    title = text(draft.get("title"), "Title", 180)
    date = instant(draft["publishedAt"]) if draft.get("publishedAt") else datetime.now(timezone.utc)
    slug = re.sub(r"[^a-z0-9]+", "-", title.lower()).strip("-")[:72] or "briefing"
    identifier = date.strftime("%Y-%m-%d-%H%M%S-") + slug
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
        "id": identifier, "title": title,
        "summary": text(draft.get("summary"), "Summary", 500),
        "author": text(draft.get("author") or "AI Brief editorial", "Author", 120),
        "category": draft.get("category", "ALL"),
        "publishedAt": date.replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "blocks": blocks,
    }
    if draft.get("imageUrl"):
        post["imageUrl"] = https_url(draft["imageUrl"])
    document = json.loads(POSTS.read_text())
    require(all(p["id"] != identifier for p in document["posts"]),
            "This briefing is already published; edit it rather than creating a duplicate")
    document["posts"].insert(0, post)
    validate(document)
    # Replacement is atomic; a failed validation never touches the live file.
    temporary = POSTS.with_suffix(".json.tmp")
    temporary.write_text(json.dumps(document, ensure_ascii=False, indent=2) + "\n")
    temporary.replace(POSTS)
    print(f"Validated and added {identifier}")


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
