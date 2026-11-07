#!/usr/bin/env python3
"""Turn a "Publish a story" issue form into a published post.

The owner fills in the form on GitHub (phone or desktop) and submits it. The
workflow passes the issue body here as data, never as shell code. This script
reads the form, saves pasted pictures into content/media as small WebP files
without metadata, and publishes, updates or removes the story through the
same validated publisher the other workflow uses.

Editing the issue later revises the same story; choosing "Unpublish" removes it.
"""
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path
import re
import sys
import urllib.request
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parents[1]
MEDIA = ROOT / "content/media"
RAW_BASE = "https://raw.githubusercontent.com/Caceras/ai-news-kotlin/main/content/media/"
MAX_DOWNLOAD = 15 * 1024 * 1024
NO_RESPONSE = "_No response_"

spec = importlib.util.spec_from_file_location("publisher", Path(__file__).with_name("publish-post.py"))
publisher = importlib.util.module_from_spec(spec)
spec.loader.exec_module(publisher)

FORMATS = {"news": "NEWS", "breaking": "BREAKING", "explainer": "EXPLAINER", "opinion": "OPINION",
           "guide": "GUIDE", "sponsored": "SPONSORED"}


def sections(body):
    """Issue forms render as "### Label" followed by the answer."""
    found = {}
    for block in re.split(r"^### ", body.replace("\r\n", "\n"), flags=re.M)[1:]:
        label, _, value = block.partition("\n")
        value = value.strip()
        found[label.strip().lower()] = "" if value == NO_RESPONSE else value
    return found


def first_word(value):
    return value.split()[0].lower() if value.strip() else ""


def picture_urls(value):
    """URLs of pictures pasted or linked in a field: markdown images, <img> tags or bare links."""
    urls = re.findall(r"!\[[^\]]*\]\((https://[^)\s]+)\)", value)
    urls += re.findall(r"<img[^>]+src=\"(https://[^\"]+)\"", value)
    urls += [u for u in re.findall(r"(?<![(\"])(https://\S+)", value) if u not in urls]
    return list(dict.fromkeys(urls))


def fetch(url, token):
    request = urllib.request.Request(url, headers={"User-Agent": "ai-brief-publisher"})
    host = urlsplit(url).hostname or ""
    # Attachments on github.com may need the token; it is never sent on to the storage host it redirects to.
    if token and (host == "github.com" or host.endswith(".githubusercontent.com")):
        request.add_unredirected_header("Authorization", f"Bearer {token}")
    with urllib.request.urlopen(request, timeout=30) as response:
        data = response.read(MAX_DOWNLOAD + 1)
    publisher.require(len(data) <= MAX_DOWNLOAD, "A picture is larger than 15 MB")
    return data


def save_picture(url, name, token, square=False):
    """Stores a picture as WebP at most 1600 px wide, with location and camera data stripped."""
    from PIL import Image, ImageOps
    try:
        image = Image.open(io.BytesIO(fetch(url, token)))
        image = ImageOps.exif_transpose(image).convert("RGB")
    except Exception as error:  # noqa: BLE001 - any unreadable picture gets the same clear message
        raise ValueError(f"Could not read the picture at {url}: {error}") from error
    if square:
        side = min(image.size)
        image = ImageOps.fit(image, (side, side)).resize((min(side, 640),) * 2)
    else:
        image.thumbnail((1600, 1600))
    encoded = io.BytesIO()
    image.save(encoded, "WEBP", quality=82, method=6)
    data = encoded.getvalue()
    # The name carries a hash of the picture. Phones keep downloaded pictures
    # by address, so a picture replaced by editing the issue must get a new
    # address, or readers would keep seeing the old one.
    MEDIA.mkdir(parents=True, exist_ok=True)
    target = MEDIA / f"{name}-{hashlib.sha256(data).hexdigest()[:10]}.webp"
    target.write_bytes(data)
    return RAW_BASE + target.name


def issue_media(number):
    """Pictures in content/media that were saved from issue [number]."""
    return list(MEDIA.glob(f"issue-{number}-*.webp"))


def remove_unused_media(number, draft=None):
    """Deletes this issue's pictures that the story no longer shows, such as a replaced
    or withdrawn picture, so a picture we stop using is not kept in the edition."""
    used = json.dumps(draft) if draft else ""
    for path in issue_media(number):
        if RAW_BASE + path.name not in used:
            path.unlink()


def sources(value):
    """One source per line: "Label | https://..." or just the link."""
    found = []
    for line in value.splitlines():
        line = line.strip().lstrip("-* ").strip()
        if not line:
            continue
        label, _, url = line.rpartition("|") if "|" in line else ("", "", line)
        found.append({"text": label.strip() or None, "url": url.strip()})
    return found


def draft_from_issue(body, number, token):
    form = sections(body)
    kind = FORMATS.get(first_word(form.get("story type", "")), "NEWS")
    stem = f"issue-{number}"
    covers = picture_urls(form.get("pictures", ""))
    publisher.require(len(covers) <= 3, "Add at most three pictures")
    if kind != "BREAKING":
        covers = covers[:1]
    saved = [save_picture(url, f"{stem}-{index + 1}", token) for index, url in enumerate(covers)]
    portrait = picture_urls(form.get("author photo", ""))
    story = form.get("story", "")
    # A picture tag pasted on its own line becomes the markdown form the publisher reads.
    story = re.sub(r"<img[^>]+src=\"(https://[^\"]+)\"[^>]*>", r"![](\1)", story)
    # Pictures pasted into the story are stored with the rest of the edition.
    inline = re.findall(r"!\[[^\]]*\]\((https://[^)\s]+)\)", story)
    publisher.require(len(inline) <= 6, "Add at most six pictures inside the story")
    for index, url in enumerate(dict.fromkeys(inline)):
        story = story.replace(url, save_picture(url, f"{stem}-inline-{index + 1}", token))
    draft = {
        "sourceIssue": number,
        "format": kind,
        "category": (form.get("topic") or "ALL").split()[0].upper(),
        "title": form.get("headline"),
        "kicker": form.get("kicker") or None,
        "summary": form.get("deck"),
        "author": form.get("author") or None,
        "authorRole": form.get("author role") or None,
        "body": story,
        "sources": sources(form.get("sources", "")),
        "theme": form.get("card colour", "").split()[0].upper() if form.get("card colour") else None,
        "sponsor": form.get("sponsor") or None,
        "publishedAt": form.get("publish at") or None,
        "imageUrl": saved[0] if saved else None,
        "gallery": saved[1:],
        "authorImageUrl": save_picture(portrait[0], f"{stem}-author", token, square=True) if portrait else None,
    }
    if draft["category"] not in publisher.CATEGORIES:
        draft["category"] = "ALL"
    return draft


def main():
    number = int(os.environ["ISSUE_NUMBER"])
    body = os.environ["ISSUE_BODY"]
    token = os.environ.get("GITHUB_TOKEN")
    result = Path(os.environ.get("RESULT_FILE", "publish-result.md"))
    try:
        form = sections(body)
        if first_word(form.get("action", "")) == "unpublish":
            identifier = publisher.unpublish(source_issue=number)
            remove_unused_media(number)
            message = f"Removed `{identifier}`. It disappears from the app on the next refresh."
        else:
            draft = draft_from_issue(body, number, token)
            identifier, outcome = publisher.publish(draft)
            remove_unused_media(number, draft)
            message = (f"{outcome.capitalize()} `{identifier}`. Readers see it on their next refresh, "
                       "usually within five minutes. Edit this issue to revise the story, or set "
                       "Action to Unpublish to remove it.")
        result.write_text(f"✅ {message}\n")
        print(message)
    except (ValueError, KeyError, TypeError) as error:
        result.write_text(f"❌ Not published: {error}\n\nFix the issue form and save it to try again.\n")
        print(f"Cannot publish: {error}")
        sys.exit(1)


if __name__ == "__main__":
    main()
