#!/usr/bin/env python3
"""Refreshes the front page's data modules inside content/posts.json.

* ``rankings``: the top models by the Artificial Analysis Intelligence Index.
  Uses the official API when ``AA_API_KEY`` is set (free key, attribution
  required); with ``--from-page`` it reads the public leaderboard instead.
* ``radar``: the newest models listed on OpenRouter's public models API,
  marking stealth (unannounced) models, which OpenRouter files under
  ``stealth/``.

Only facts are stored (names, makers, scores, dates) with the source and the
date checked, and the app always shows the attribution. Nothing is invented: a
source that cannot be read leaves the previous module unchanged.

    python3 scripts/update-extras.py [--from-page] [--posts content/posts.json]
"""
import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import sys
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
UA = "AI-Brief-editorial-bot/1.0 (+https://github.com/Caceras/ai-news-kotlin)"
AA_PAGE = "https://artificialanalysis.ai/leaderboards/models"
AA_API = "https://artificialanalysis.ai/api/v2/data/llms/models"
OPENROUTER = "https://openrouter.ai/api/v1/models"
RANKED = 10
LOGOS = ROOT / "content/media/logos"
LOGO_URL = "https://raw.githubusercontent.com/Caceras/ai-news-kotlin/main/content/media/logos/"


def logo(maker):
    """Our hosted copy of a maker's logo, when we have one."""
    slug = re.sub(r"[^a-z0-9]+", "-", maker.lower()).strip("-")
    return LOGO_URL + slug + ".png" if (LOGOS / f"{slug}.png").exists() else None
RADAR = 8


def fetch(url, headers=None):
    request = urllib.request.Request(url, headers={"User-Agent": UA, **(headers or {})})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read().decode("utf-8")


def base_name(name):
    """'Claude Opus 5.5 (Max, Default Fallback)' -> 'Claude Opus 5.5'."""
    return re.sub(r"\s*\(.*\)\s*$", "", name).strip()


def rank(models):
    best = {}
    for m in models:
        score = m.get("score")
        if score is None:
            continue
        key = base_name(m["name"])
        if key not in best or score > best[key]["score"]:
            best[key] = {**m, "name": key}
    top = sorted(best.values(), key=lambda m: -m["score"])[:RANKED]
    return [{k: v for k, v in {"rank": i + 1, "name": m["name"], "maker": m["maker"], "score": round(m["score"], 1),
             "openWeights": bool(m.get("openWeights")), "logoUrl": logo(m["maker"])}.items() if v is not None}
            for i, m in enumerate(top)]


def models_from_api(key):
    data = json.loads(fetch(AA_API, {"x-api-key": key}))["data"]
    return [{"name": m["name"], "maker": (m.get("model_creator") or {}).get("name", ""),
             "score": (m.get("evaluations") or {}).get("artificial_analysis_intelligence_index"),
             "openWeights": False} for m in data]


def models_from_page():
    page = fetch(AA_PAGE)
    chunks = re.findall(r'self\.__next_f\.push\(\[1,"(.*?)"\]\)</script>', page, re.S)
    text = "".join(json.loads('"' + c + '"') for c in chunks)
    start = text.index('"initialModels":') + len('"initialModels":')
    depth = 0
    for end in range(start, len(text)):
        depth += {"[": 1, "]": -1}.get(text[end], 0)
        if depth == 0:
            break
    raw = json.loads(text[start:end + 1])
    return [{"name": m["name"], "maker": m.get("modelCreatorName", ""), "score": m.get("intelligenceIndex"),
             "openWeights": m.get("isOpenWeights", False)} for m in raw if not m.get("deprecated")]


def rankings(from_page):
    key = os.environ.get("AA_API_KEY")
    if key:
        models = models_from_api(key)
    elif from_page:
        models = models_from_page()
    else:
        return None
    entries = rank(models)
    if len(entries) < 5:
        raise ValueError("Too few ranked models; keeping the previous table")
    return {
        "title": "Top AI models",
        "metric": "Artificial Analysis Intelligence Index",
        "source": "Artificial Analysis",
        "sourceUrl": AA_PAGE,
        "checkedAt": now(),
        "entries": entries,
    }


def first_sentence(text, limit=150):
    text = re.sub(r"\[([^\]]+)\]\([^)]+\)", r"\1", (text or "").replace("\n", " ")).strip()
    text = text.replace("`", "")
    sentence = re.split(r"(?<=[.!?])\s", text, maxsplit=1)[0]
    return sentence if len(sentence) <= limit else sentence[: limit - 1].rsplit(" ", 1)[0] + "…"


def radar():
    models = json.loads(fetch(OPENROUTER))["data"]
    seen = set()
    entries = []
    for m in sorted(models, key=lambda m: -m.get("created", 0)):
        ident = m["id"]
        family = ident.split(":")[0]
        if ident.startswith("openrouter/") or family in seen:
            continue
        seen.add(family)
        stealth = ident.startswith("stealth/") or "stealth" in (m.get("description") or "").lower()
        name = m.get("name", ident)
        maker, _, model = name.partition(": ")
        entries.append({
            "name": model or name,
            "maker": "Unannounced" if stealth else (maker if model else ident.split("/")[0]),
            "listedAt": datetime.fromtimestamp(m["created"], timezone.utc).strftime("%Y-%m-%d"),
            "note": first_sentence(m.get("description")),
            "url": f"https://openrouter.ai/{family}",
            "stealth": stealth,
        })
        if len(entries) == RADAR:
            break
    # Stealth models first: they are the story.
    entries.sort(key=lambda e: not e["stealth"])
    return {
        "title": "On the radar",
        "source": "OpenRouter",
        "sourceUrl": "https://openrouter.ai/models",
        "checkedAt": now(),
        "entries": entries,
    }


def now():
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--from-page", action="store_true")
    parser.add_argument("--posts", default=str(ROOT / "content/posts.json"))
    args = parser.parse_args()
    path = Path(args.posts)
    document = json.loads(path.read_text())
    extras = document.setdefault("extras", {})
    changed = False
    for name, build in (("rankings", lambda: rankings(args.from_page)), ("radar", radar)):
        try:
            module = build()
        except Exception as error:  # A source being down keeps yesterday's module.
            print(f"{name}: kept previous ({error})", file=sys.stderr)
            continue
        if module is None:
            print(f"{name}: no source configured, kept previous", file=sys.stderr)
            continue
        old = {k: v for k, v in extras.get(name, {}).items() if k != "checkedAt"}
        new = {k: v for k, v in module.items() if k != "checkedAt"}
        if old != new:
            extras[name] = module
            changed = True
            print(f"{name}: updated")
    if changed:
        path.write_text(json.dumps(document, ensure_ascii=False, indent=2) + "\n")


if __name__ == "__main__":
    main()
