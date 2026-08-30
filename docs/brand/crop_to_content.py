"""Trims a rendered screens PNG down to its content box.

The page is deliberately rendered into an over-tall window (see render.sh), so
the raw screenshot carries a band of empty backdrop underneath the captions.
This finds the last row containing anything other than backdrop and keeps a
fixed margin below it.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from pngtool import crop_rows, load

# Backdrop is the app's own surfaceVariant in each scheme.
BACKDROP = {"light": 0xF7, "dark": 0x18}

# 60 CSS px at a device scale factor of 2, mirroring the air above the phones.
BOTTOM_MARGIN_PX = 120


def main(raw_path, out_path, theme):
    backdrop = BACKDROP[theme]
    width, height, bytes_per_px, pixels = load(raw_path)

    def has_ink(y):
        return any(
            abs(pixels[(y * width + x) * bytes_per_px] - backdrop) > 12
            for x in range(0, width, 2)
        )

    last_ink = max(y for y in range(height) if has_ink(y))
    crop_rows(raw_path, out_path, last_ink + BOTTOM_MARGIN_PX)
    print(f"wrote {out_path}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], sys.argv[3])
