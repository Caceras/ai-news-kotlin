"""Generates the README hero wordmark for both colour schemes from one template.

Both files come from this script so the light and dark banners can never drift
apart. Colours are the app's own, read from ui/theme/Theme.kt: the light pair is
#111111 on #FFFFFF, the dark pair #EDEDE8 on #0E0E0D.

Vertical rhythm is set so the space above the wordmark's cap height matches the
space below the colophon, which centres the block optically rather than by box.
"""
import os

OUT_DIR = os.path.dirname(os.path.abspath(__file__))

THEMES = {
    "light": dict(bg="#FFFFFF", ink="#111111", muted="#6E6D68", rule="#DCDCD8"),
    "dark": dict(bg="#0E0E0D", ink="#EDEDE8", muted="#8E8D88", rule="#333330"),
}

W, H = 1280, 352
SANS = "Helvetica Neue, Helvetica, Inter, Arial, sans-serif"

# Baselines. The wordmark's cap top lands at 72; the colophon descends to ~283,
# leaving an equal 72 of air top and bottom.
WORDMARK_Y, TAGLINE_Y, RULE_Y, COLOPHON_Y = 141, 187, 243, 279
RULE_INSET = 160

TEMPLATE = '''<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}" role="img" aria-label="AI Brief — a calm, text-first read on artificial intelligence">
  <rect width="{w}" height="{h}" fill="{bg}"/>
  <g font-family="{sans}" text-anchor="middle">
    <text x="{cx}" y="{wordmark_y}" fill="{ink}" font-size="96" font-weight="700" letter-spacing="-4">AI Brief</text>
    <text x="{cx}" y="{tagline_y}" fill="{muted}" font-size="21" letter-spacing="0.2">A calm, text-first read on artificial intelligence.</text>
    <line x1="{rx1}" y1="{rule_y}" x2="{rx2}" y2="{rule_y}" stroke="{rule}" stroke-width="1"/>
    <text x="{cx}" y="{colophon_y}" fill="{muted}" font-size="13" letter-spacing="3.4">KOTLIN &#183; JETPACK COMPOSE &#183; MATERIAL 3 &#183; ANDROID 26&#8211;36</text>
  </g>
</svg>
'''

for name, colours in THEMES.items():
    svg = TEMPLATE.format(
        w=W, h=H, cx=W // 2,
        rx1=RULE_INSET, rx2=W - RULE_INSET,
        wordmark_y=WORDMARK_Y, tagline_y=TAGLINE_Y,
        rule_y=RULE_Y, colophon_y=COLOPHON_Y,
        sans=SANS, **colours
    )
    path = os.path.join(OUT_DIR, f"hero-{name}.svg")
    with open(path, "w") as fh:
        fh.write(svg)
    print(f"wrote {path}")
