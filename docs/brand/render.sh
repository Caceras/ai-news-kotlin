#!/usr/bin/env bash
#
# Regenerates every brand asset in this directory.
#
# Usage:  docs/brand/render.sh [path-to-chromium]
#
# The hero is pure SVG and needs nothing but Python. The screen mockups are laid
# out in HTML and rasterised with Chromium, which needs two workarounds:
#
#   1. At --force-device-scale-factor=2 Chromium paints only part of the
#      viewport, silently dropping the bottom row of the page. The page is
#      therefore rendered into a window far taller than the content.
#   2. That leaves a tall band of empty backdrop, so the result is cropped back
#      to the content box afterwards.
#
# Check the output by eye afterwards: a dropped caption row or a clipped phone
# will not fail this script.
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
chromium="${1:-${CHROMIUM:-chromium}}"

if ! command -v "$chromium" >/dev/null 2>&1 && [ ! -x "$chromium" ]; then
  echo "Chromium not found. Pass its path: docs/brand/render.sh /path/to/chromium" >&2
  exit 1
fi

python3 "$here/gen_hero.py"

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

# 1370 = 56px page padding either side + three 390px phones + two 44px gaps.
for theme in light dark; do
  python3 "$here/gen_screens.py" "$theme" > "$work/screens-$theme.html"

  "$chromium" --headless --disable-gpu --no-sandbox --hide-scrollbars \
    --force-device-scale-factor=2 --window-size=1370,1240 \
    --screenshot="$work/raw-$theme.png" "file://$work/screens-$theme.html" 2>/dev/null

  python3 "$here/crop_to_content.py" \
    "$work/raw-$theme.png" "$here/screens-$theme.png" "$theme"
done
