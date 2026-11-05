#!/usr/bin/env python3
"""Draws AI Brief's story covers in one house style.

Every cover is a flat, cut-paper poster: a single brand colour field, a halftone
fade, the slanted AI Brief block, and one bold motif that names the story at a
glance (the EU's stars and a clock for an AI Act deadline, a phone and sound
waves for a cloned voice). Shapes carry a hard navy offset, like a print slightly
out of register. Series get their own artwork instead, so they never look like
the everyday covers.

    python3 scripts/editorial-art/covers.py <out-dir> [cover-id ...]

Needs Pillow.
"""
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
FONT = ROOT / "app/src/main/res/font/dm_sans_extrabold.ttf"
SERIF = ROOT / "app/src/main/res/font/newsreader_semibold.ttf"

W, H = 1600, 1000
SS = 2  # drawn at twice the size, then reduced, for clean edges

NAVY = (10, 31, 68)
DEEP = (6, 18, 42)
BLUE = (47, 107, 255)
SKY = (127, 165, 255)
RED = (215, 38, 46)
ORANGE = (232, 131, 58)
PAPER = (241, 242, 244)
WARM = (251, 241, 230)
WHITE = (255, 255, 255)
INK = (11, 13, 18)
MINT = (174, 226, 205)
YELLOW = (255, 204, 0)  # only for the EU's stars


def s(v):
    return int(round(v * SS))


class Cover:
    def __init__(self, background, halftone=None, grid=True):
        self.bg = background
        self.img = Image.new("RGB", (s(W), s(H)), background)
        self.d = ImageDraw.Draw(self.img)
        self.shadow = DEEP if sum(background) > 200 else (0, 8, 24)
        if sum(background) > 600:
            self.shadow = NAVY
        if grid:
            self._grid()
        self._halftone(halftone or shade(background, -0.12 if sum(background) > 300 else 0.18))

    def _grid(self):
        line = shade(self.bg, -0.05 if sum(self.bg) > 300 else 0.07)
        for x in range(0, W + 1, 80):
            self.d.line([(s(x), 0), (s(x), s(H))], fill=line, width=s(1))
        for y in range(0, H + 1, 80):
            self.d.line([(0, s(y)), (s(W), s(y))], fill=line, width=s(1))

    def _halftone(self, colour):
        step = 26
        for y in range(0, H + step, step):
            for x in range(0, W + step, step):
                # Dots grow towards the bottom-left corner.
                t = max(0.0, 1 - math.hypot(x / W, (H - y) / H * 1.1) / 1.05)
                r = 11 * t ** 1.4
                if r > 0.8:
                    ox = (step / 2) if (y // step) % 2 else 0
                    self.circle(x + ox, y, r, colour, shadow=False)

    # Shapes. Each draws its navy offset first unless shadow=False.
    def _both(self, draw, shadow, offset=16):
        if shadow:
            draw(offset, self.shadow)
        draw(0, None)

    def poly(self, pts, fill, shadow=True, outline=None, width=0):
        def draw(o, col):
            self.d.polygon([(s(x + o), s(y + o)) for x, y in pts], fill=col or fill,
                           outline=None if col else outline, width=s(width) if not col else 0)
        self._both(draw, shadow)

    def rect(self, x0, y0, x1, y1, fill, r=0, shadow=True, outline=None, width=0):
        def draw(o, col):
            self.d.rounded_rectangle([s(x0 + o), s(y0 + o), s(x1 + o), s(y1 + o)], radius=s(r),
                                     fill=col or fill, outline=None if col else outline,
                                     width=s(width) if not col else 0)
        self._both(draw, shadow)

    def circle(self, cx, cy, r, fill, shadow=True, outline=None, width=0):
        def draw(o, col):
            self.d.ellipse([s(cx - r + o), s(cy - r + o), s(cx + r + o), s(cy + r + o)],
                           fill=col or fill, outline=None if col else outline,
                           width=s(width) if not col else 0)
        self._both(draw, shadow)

    def ring(self, cx, cy, r, width, colour, shadow=True):
        def draw(o, col):
            self.d.ellipse([s(cx - r + o), s(cy - r + o), s(cx + r + o), s(cy + r + o)],
                           outline=col or colour, width=s(width))
        self._both(draw, shadow)

    def arc(self, cx, cy, r, start, end, width, colour, shadow=True):
        def draw(o, col):
            self.d.arc([s(cx - r + o), s(cy - r + o), s(cx + r + o), s(cy + r + o)], start, end,
                       fill=col or colour, width=s(width))
        self._both(draw, shadow)

    def line(self, pts, width, colour, shadow=True, round_caps=True):
        def draw(o, col):
            p = [(s(x + o), s(y + o)) for x, y in pts]
            self.d.line(p, fill=col or colour, width=s(width), joint="curve")
            if round_caps:
                for x, y in (p[0], p[-1]):
                    rr = s(width) / 2
                    self.d.ellipse([x - rr, y - rr, x + rr, y + rr], fill=col or colour)
        self._both(draw, shadow)

    def text(self, x, y, value, size, fill, font=FONT, anchor="mm", shadow=False):
        f = ImageFont.truetype(str(font), s(size))

        def draw(o, col):
            self.d.text((s(x + o), s(y + o)), value, font=f, fill=col or fill, anchor=anchor)
        self._both(draw, shadow)

    def star(self, cx, cy, r, fill, shadow=False):
        pts = []
        for i in range(10):
            a = -math.pi / 2 + i * math.pi / 5
            rr = r if i % 2 == 0 else r * 0.4
            pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
        self.poly(pts, fill, shadow=shadow)

    def slant(self, x, y, w, h, fill, shadow=False):
        lean = h * 0.45
        self.poly([(x + lean, y), (x + w, y), (x + w - lean, y + h), (x, y + h)], fill, shadow=shadow)

    def paste(self, image, x, y, shadow=True):
        """Pastes an RGBA cut-out at (x, y), in final-size pixels, with the offset shadow."""
        image = image.resize((s(image.width), s(image.height)), Image.LANCZOS)
        if shadow:
            alpha = image.split()[3]
            sh = Image.new("RGB", image.size, self.shadow)
            self.img.paste(sh, (s(x + 18), s(y + 18)), alpha)
        self.img.paste(image, (s(x), s(y)), image.split()[3])

    def save(self, path):
        out = self.img.resize((W, H), Image.LANCZOS)
        out.save(path, "WEBP", quality=84, method=6)


def shade(c, amount):
    if amount >= 0:
        return tuple(int(v + (255 - v) * amount) for v in c)
    return tuple(int(v * (1 + amount)) for v in c)


# Reusable motifs -----------------------------------------------------------

def document(c, x, y, w, h, fill=WHITE, lines=6, ink=None, highlight=None, fold=True):
    ink = ink or shade(NAVY, 0.55)
    c.rect(x, y, x + w, y + h, fill, r=14)
    if fold:
        c.poly([(x + w - 70, y), (x + w, y + 70), (x + w - 70, y + 70)], shade(fill, -0.12), shadow=False)
    gap = (h - 120) / max(lines, 1)
    for i in range(lines):
        ly = y + 70 + i * gap
        lw = w - 90 - (60 if i % 3 == 2 else 0)
        col = highlight if highlight and i in (1, 2) else ink
        c.rect(x + 45, ly, x + 45 + lw, ly + 16, col, r=8, shadow=False)


def magnifier(c, cx, cy, r, rim=NAVY, glass=None):
    angle = math.radians(45)
    hx, hy = cx + r * math.cos(angle), cy + r * math.sin(angle)
    c.line([(hx, hy), (hx + r * 0.9, hy + r * 0.9)], r * 0.32, rim)
    if glass:
        c.circle(cx, cy, r, glass, shadow=False)
    c.ring(cx, cy, r, r * 0.2, rim)


def bubble(c, x, y, w, h, fill, tail="left"):
    c.rect(x, y, x + w, y + h, fill, r=h * 0.32)
    if tail == "left":
        c.poly([(x + w * 0.16, y + h - 4), (x + w * 0.08, y + h + 70), (x + w * 0.34, y + h - 4)], fill)
    else:
        c.poly([(x + w * 0.84, y + h - 4), (x + w * 0.92, y + h + 70), (x + w * 0.66, y + h - 4)], fill)
    c.rect(x, y, x + w, y + h, fill, r=h * 0.32, shadow=False)


def clock(c, cx, cy, r, face=WHITE, rim=NAVY, hand=NAVY, accent=RED, hours=10, minutes=10):
    c.circle(cx, cy, r, face)
    c.ring(cx, cy, r, r * 0.11, rim, shadow=False)
    for i in range(12):
        a = math.radians(i * 30 - 90)
        r0, r1 = r * 0.72, r * 0.82
        c.line([(cx + r0 * math.cos(a), cy + r0 * math.sin(a)), (cx + r1 * math.cos(a), cy + r1 * math.sin(a))],
               r * 0.05, rim, shadow=False)
    ha = math.radians((hours % 12) * 30 + minutes * 0.5 - 90)
    ma = math.radians(minutes * 6 - 90)
    c.line([(cx, cy), (cx + r * 0.42 * math.cos(ha), cy + r * 0.42 * math.sin(ha))], r * 0.09, hand, shadow=False)
    c.line([(cx, cy), (cx + r * 0.64 * math.cos(ma), cy + r * 0.64 * math.sin(ma))], r * 0.06, accent, shadow=False)
    c.circle(cx, cy, r * 0.07, rim, shadow=False)


def eu_stars(c, cx, cy, r, star=YELLOW, field=None):
    if field:
        c.circle(cx, cy, r * 1.38, field)
    for i in range(12):
        a = math.radians(i * 30 - 90)
        c.star(cx + r * math.cos(a), cy + r * math.sin(a), r * 0.17, star)


def padlock(c, cx, cy, size, body=NAVY, shackle=NAVY, open_=False, keyhole=WHITE):
    sw = size * 0.16
    r = size * 0.34
    lift = size * 0.22 if open_ else 0
    top = cy - size * 0.5 - lift
    c.arc(cx, top, r, 180, 360, sw, shackle)
    # Right leg runs into the body; an open lock's left leg stops short in the air.
    c.line([(cx + r, top), (cx + r, cy - size * 0.1)], sw, shackle)
    c.line([(cx - r, top), (cx - r, top + (size * 0.12 if open_ else size * 0.4 + lift))], sw, shackle)
    c.rect(cx - size * 0.5, cy - size * 0.15, cx + size * 0.5, cy + size * 0.6, body, r=size * 0.08)
    c.circle(cx, cy + size * 0.15, size * 0.09, keyhole, shadow=False)
    c.rect(cx - size * 0.035, cy + size * 0.15, cx + size * 0.035, cy + size * 0.38, keyhole, shadow=False)


def phone(c, x, y, w, h, body=NAVY, screen=SKY):
    c.rect(x, y, x + w, y + h, body, r=w * 0.16)
    c.rect(x + w * 0.08, y + h * 0.08, x + w * 0.92, y + h * 0.86, screen, r=w * 0.06, shadow=False)
    c.rect(x + w * 0.38, y + h * 0.9, x + w * 0.62, y + h * 0.93, shade(body, 0.4), r=6, shadow=False)


def waves(c, cx, cy, count, colour, start=60, step=55, spread=50, width=22):
    for i in range(count):
        r = start + i * step
        c.arc(cx, cy, r, -spread, spread, width, colour)


def warning(c, cx, cy, size, fill=YELLOW, mark=INK):
    h = size * math.sqrt(3) / 2
    pts = [(cx, cy - h * 0.66), (cx + size / 2, cy + h * 0.34), (cx - size / 2, cy + h * 0.34)]
    c.poly(pts, fill)
    c.text(cx, cy + h * 0.06, "!", size * 0.5, mark, shadow=False)


def chip(c, cx, cy, size, body=NAVY, pins=SKY, label=None, label_colour=WHITE):
    half = size / 2
    for i in range(5):
        o = -half + size * (0.18 + i * 0.16)
        for (x0, y0, x1, y1) in [(cx + o - 8, cy - half - 34, cx + o + 8, cy - half),
                                 (cx + o - 8, cy + half, cx + o + 8, cy + half + 34),
                                 (cx - half - 34, cy + o - 8, cx - half, cy + o + 8),
                                 (cx + half, cy + o - 8, cx + half + 34, cy + o + 8)]:
            c.rect(x0, y0, x1, y1, pins, r=4, shadow=False)
    c.rect(cx - half, cy - half, cx + half, cy + half, body, r=size * 0.1)
    c.rect(cx - half * 0.6, cy - half * 0.6, cx + half * 0.6, cy + half * 0.6, shade(body, 0.18), r=size * 0.05,
           shadow=False)
    if label:
        c.text(cx, cy, label, size * 0.26, label_colour, shadow=False)


def bars(c, x, y, w, h, values, colours, axis=NAVY):
    n = len(values)
    bw = w / (n * 1.6)
    for i, (v, col) in enumerate(zip(values, colours)):
        bx = x + i * bw * 1.6 + bw * 0.3
        c.rect(bx, y + h * (1 - v), bx + bw, y + h, col, r=8)
    c.line([(x, y + h + 6), (x + w, y + h + 6)], 12, axis, shadow=False)


def iso_cube(c, cx, cy, size, top, left, right):
    hx = size * 0.866
    hy = size * 0.5
    t = [(cx, cy - size), (cx + hx, cy - size + hy), (cx, cy - size + 2 * hy), (cx - hx, cy - size + hy)]
    lft = [(cx - hx, cy - size + hy), (cx, cy - size + 2 * hy), (cx, cy + hy * 2), (cx - hx, cy + hy)]
    rgt = [(cx + hx, cy - size + hy), (cx, cy - size + 2 * hy), (cx, cy + hy * 2), (cx + hx, cy + hy)]
    c.poly(t + [t[0]], top, shadow=True)
    c.poly(lft, left, shadow=False)
    c.poly(rgt, right, shadow=False)
    c.poly(t, top, shadow=False)


def eye(c, cx, cy, w, fill=WHITE, iris=BLUE, outline=NAVY):
    h = w * 0.5
    pts = []
    for i in range(41):
        t = i / 40
        x = cx - w / 2 + w * t
        pts.append((x, cy - h * math.sin(math.pi * t)))
    for i in range(41):
        t = 1 - i / 40
        x = cx - w / 2 + w * t
        pts.append((x, cy + h * math.sin(math.pi * t)))
    c.poly(pts, fill)
    c.circle(cx, cy, h * 0.62, iris, shadow=False)
    c.circle(cx, cy, h * 0.28, outline, shadow=False)
    c.circle(cx + h * 0.18, cy - h * 0.18, h * 0.1, WHITE, shadow=False)


def frame(c, x, y, w, h, mat=WHITE, sky=SKY, hill=BLUE, sun=ORANGE):
    c.rect(x, y, x + w, y + h, mat, r=10)
    ix0, iy0, ix1, iy1 = x + 28, y + 28, x + w - 28, y + h - 28
    c.rect(ix0, iy0, ix1, iy1, sky, shadow=False)
    c.circle(ix0 + (ix1 - ix0) * 0.72, iy0 + (iy1 - iy0) * 0.3, (iy1 - iy0) * 0.13, sun, shadow=False)
    c.poly([(ix0, iy1), (ix0 + (ix1 - ix0) * 0.38, iy0 + (iy1 - iy0) * 0.42), (ix0 + (ix1 - ix0) * 0.7, iy1)],
           hill, shadow=False)
    c.poly([(ix0 + (ix1 - ix0) * 0.45, iy1), (ix0 + (ix1 - ix0) * 0.75, iy0 + (iy1 - iy0) * 0.58), (ix1, iy1)],
           shade(hill, -0.3), shadow=False)


def badge(c, cx, cy, r, fill=BLUE, mark="✓", mark_colour=WHITE, font=FONT):
    pts = []
    for i in range(24):
        a = math.radians(i * 15)
        rr = r if i % 2 == 0 else r * 0.88
        pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
    c.poly(pts, fill)
    c.text(cx, cy + r * 0.04, mark, r * 1.0, mark_colour, font=font, shadow=False)


def check(c, cx, cy, size, colour):
    c.line([(cx - size * 0.5, cy), (cx - size * 0.15, cy + size * 0.35), (cx + size * 0.55, cy - size * 0.4)],
           size * 0.22, colour)


def cross(c, cx, cy, size, colour):
    c.line([(cx - size / 2, cy - size / 2), (cx + size / 2, cy + size / 2)], size * 0.22, colour)
    c.line([(cx + size / 2, cy - size / 2), (cx - size / 2, cy + size / 2)], size * 0.22, colour, shadow=False)


def key(c, cx, cy, size, colour):
    c.ring(cx, cy, size * 0.3, size * 0.14, colour)
    c.line([(cx + size * 0.3, cy), (cx + size * 1.25, cy)], size * 0.14, colour)
    c.line([(cx + size * 0.95, cy), (cx + size * 0.95, cy + size * 0.25)], size * 0.12, colour, shadow=False)
    c.line([(cx + size * 1.15, cy), (cx + size * 1.15, cy + size * 0.2)], size * 0.12, colour, shadow=False)


def toggle(c, x, y, w, on, track_on=BLUE, track_off=shade(NAVY, 0.55), knob=WHITE):
    h = w * 0.5
    c.rect(x, y, x + w, y + h, track_on if on else track_off, r=h / 2)
    kx = x + w - h / 2 if on else x + h / 2
    c.circle(kx, y + h / 2, h * 0.38, knob, shadow=False)


# Covers ---------------------------------------------------------------------

def eu_ai_act(out):
    c = Cover(NAVY)
    c.slant(90, 120, 420, 70, RED)
    eu_stars(c, 640, 500, 250, field=(0, 51, 153))
    eu_stars(c, 640, 500, 250)
    c.rect(1150, 300, 1250, 360, RED, r=10)  # stopwatch crown
    c.circle(1060, 405, 46, RED)
    c.circle(1340, 405, 46, RED)
    clock(c, 1200, 620, 230, face=WHITE, rim=NAVY, hand=NAVY, accent=RED, hours=12, minutes=0)
    c.save(out)


def hallucination(out):
    c = Cover(PAPER)
    c.slant(100, 110, 380, 64, BLUE)
    bubble(c, 230, 260, 640, 380, NAVY)
    c.text(550, 450, "1867?", 150, WHITE, shadow=False)
    # Dice: a guess, not a fact.
    c.rect(980, 330, 1280, 630, RED, r=40)
    for (dx, dy) in [(0.25, 0.25), (0.75, 0.75), (0.5, 0.5), (0.75, 0.25), (0.25, 0.75)]:
        c.circle(980 + 300 * dx, 330 + 300 * dy, 26, WHITE, shadow=False)
    c.text(1300, 790, "?", 260, BLUE)
    c.save(out)


def prompt_injection(out):
    c = Cover(BLUE)
    c.slant(90, 110, 380, 64, NAVY)
    document(c, 250, 220, 520, 640, fill=WHITE, lines=7, highlight=RED)
    # A syringe pushing hidden text into the page.
    c.line([(1350, 300), (900, 560)], 70, WHITE)
    c.line([(1350, 300), (1240, 363)], 70, NAVY, shadow=False)
    c.line([(905, 557), (790, 623)], 14, NAVY, shadow=False)
    c.line([(1420, 250), (1330, 410)], 26, NAVY)
    c.rect(560, 640, 860, 700, RED, r=12)
    c.text(710, 670, "IGNORE ALL…", 34, WHITE, shadow=False)
    c.save(out)


def context_window(out):
    c = Cover(WARM, halftone=shade(ORANGE, 0.55))
    c.slant(100, 110, 380, 64, ORANGE)
    c.rect(330, 210, 1270, 860, WHITE, r=22)
    c.rect(330, 210, 1270, 290, NAVY, r=22, shadow=False)
    c.rect(330, 250, 1270, 290, NAVY, shadow=False)
    for i, col in enumerate([RED, ORANGE, MINT]):
        c.circle(380 + i * 46, 250, 14, col, shadow=False)
    for i in range(8):
        y = 330 + i * 62
        fade = max(0.0, 1 - i / 8) if i < 3 else 0
        col = shade(NAVY, 0.15 + 0.75 * fade) if i < 3 else (shade(NAVY, 0.3) if i % 2 else BLUE)
        c.rect(390 + (i % 2) * 220, y, 1200 - (i % 3) * 120, y + 30, col, r=12, shadow=False)
    c.poly([(1270, 300), (1420, 220), (1420, 380)], RED)  # older text slipping out
    c.save(out)


def model_card(out):
    c = Cover((214, 221, 255))
    c.slant(100, 110, 380, 64, NAVY)
    # A swing tag.
    c.poly([(420, 260), (1080, 260), (1200, 540), (1080, 820), (420, 820)], WHITE)
    c.circle(1090, 540, 34, (214, 221, 255), shadow=False)
    c.line([(1120, 540), (1400, 380)], 12, NAVY)
    c.rect(480, 320, 820, 360, NAVY, r=10, shadow=False)
    bars(c, 480, 420, 520, 300, [0.9, 0.55, 0.75, 0.35], [BLUE, SKY, BLUE, RED])
    c.save(out)


def cloned_voice(out):
    c = Cover(NAVY)
    c.slant(90, 110, 380, 64, RED)
    phone(c, 560, 180, 330, 650, body=WHITE, screen=SKY)
    c.circle(725, 420, 80, NAVY, shadow=False)
    c.rect(645, 520, 805, 640, NAVY, r=60, shadow=False)
    waves(c, 900, 505, 3, WHITE, start=90, step=70)
    waves(c, 540, 505, 0, WHITE)
    for i in range(3):
        c.arc(560, 505, 90 + i * 70, 130, 230, 22, SKY)
    warning(c, 1240, 330, 230)
    c.save(out)


def chat_privacy(out):
    c = Cover((205, 236, 222))
    c.slant(100, 110, 380, 64, NAVY)
    bubble(c, 230, 250, 560, 230, WHITE)
    bubble(c, 540, 540, 560, 230, BLUE, tail="right")
    for i in range(3):
        c.rect(290, 320 + i * 50, 700 - i * 90, 344 + i * 50, shade(NAVY, 0.55), r=10, shadow=False)
        c.rect(600, 610 + i * 50, 1020 - i * 120, 634 + i * 50, WHITE, r=10, shadow=False)
    eye(c, 1250, 330, 320)
    padlock(c, 1250, 690, 230, body=NAVY)
    c.save(out)


def rag_finetune(out):
    c = Cover(PAPER)
    c.slant(100, 110, 380, 64, BLUE)
    for i in range(3):
        document(c, 200 + i * 40, 230 + i * 40, 340, 440, fill=WHITE if i < 2 else SKY, lines=5)
    c.line([(680, 520), (930, 520)], 34, NAVY)
    c.poly([(930, 470), (1010, 520), (930, 570)], NAVY)
    chip(c, 1230, 520, 260, body=NAVY, pins=BLUE, label="LLM")
    c.save(out)


def detectors(out):
    c = Cover((255, 225, 220))
    c.slant(100, 110, 380, 64, RED)
    document(c, 260, 220, 560, 650, fill=WHITE, lines=8)
    magnifier(c, 900, 470, 200, rim=NAVY, glass=(230, 240, 255))
    c.text(900, 470, "AI?", 120, RED, shadow=False)
    cross(c, 1300, 760, 150, RED)
    check(c, 1320, 330, 170, BLUE)
    c.save(out)


def copyright_ai(out):
    c = Cover(NAVY)
    c.slant(90, 110, 380, 64, ORANGE)
    frame(c, 230, 240, 640, 520)
    c.circle(1150, 500, 250, WHITE)
    c.text(1150, 520, "©", 360, NAVY, shadow=False)
    # A paintbrush across the frame: the human hand.
    c.line([(760, 820), (1010, 600)], 40, ORANGE)
    c.poly([(1000, 560), (1080, 520), (1050, 640), (980, 640)], RED)
    c.save(out)


def agent_permissions(out):
    c = Cover(BLUE)
    c.slant(90, 110, 380, 64, NAVY)
    c.rect(260, 220, 820, 840, WHITE, r=28)
    labels = [True, False, True, False]
    for i, on in enumerate(labels):
        y = 290 + i * 135
        c.rect(320, y + 20, 560, y + 46, shade(NAVY, 0.55), r=12, shadow=False)
        toggle(c, 620, y, 140, on)
    key(c, 1050, 520, 240, WHITE)
    c.save(out)


def grounded_answers(out):
    c = Cover(WARM, halftone=shade(ORANGE, 0.55))
    c.slant(100, 110, 380, 64, NAVY)
    document(c, 250, 210, 520, 660, fill=WHITE, lines=8, highlight=YELLOW)
    bubble(c, 900, 300, 470, 250, NAVY, tail="right")
    c.text(1135, 425, "[1] [2]", 70, WHITE, shadow=False)
    c.line([(770, 400), (900, 420)], 12, BLUE)
    c.circle(770, 400, 18, BLUE)
    c.save(out)


def provenance(out):
    c = Cover(PAPER)
    c.slant(100, 110, 380, 64, BLUE)
    frame(c, 260, 220, 760, 560)
    badge(c, 1030, 760, 140, fill=NAVY, mark="cr", font=FONT)
    c.text(1340, 360, "?", 240, RED)
    c.save(out)


def trust(out):
    c = Cover(SKY)
    c.slant(100, 110, 380, 64, NAVY)
    # A balance: confidence on one side, evidence on the other.
    c.line([(800, 260), (800, 820)], 26, NAVY)
    c.rect(640, 820, 960, 860, NAVY, r=10)
    c.line([(420, 380), (1180, 300)], 22, NAVY)
    c.circle(800, 340, 30, RED)
    for x, y, col in [(420, 380, WHITE), (1180, 300, WHITE)]:
        c.line([(x - 110, y + 220), (x, y), (x + 110, y + 220)], 8, NAVY, shadow=False)
        c.arc(x, y + 220, 120, 0, 180, 20, col)
    check(c, 1180, 470, 110, BLUE)
    c.text(420, 560, "?", 140, RED, shadow=False)
    c.save(out)


def model_scores(out):
    c = Cover(NAVY)
    c.slant(90, 110, 380, 64, BLUE)
    bars(c, 230, 300, 760, 520, [0.95, 0.7, 0.82, 0.5, 0.62], [WHITE, SKY, BLUE, SKY, WHITE], axis=SKY)
    magnifier(c, 1220, 450, 170, rim=WHITE, glass=shade(NAVY, 0.15))
    c.text(1220, 450, "%", 150, WHITE, shadow=False)
    c.save(out)


def open_weights(out):
    c = Cover((230, 226, 255))
    c.slant(100, 110, 380, 64, NAVY)
    padlock(c, 560, 640, 360, body=BLUE, shackle=NAVY, open_=True)
    # A kettlebell: the "weights".
    c.arc(1150, 430, 110, 180, 360, 46, NAVY)
    c.line([(1040, 430), (1040, 480)], 46, NAVY, shadow=False)
    c.line([(1260, 430), (1260, 480)], 46, NAVY, shadow=False)
    c.circle(1150, 640, 200, NAVY)
    c.text(1150, 650, "W", 150, WHITE, shadow=False)
    c.save(out)


def oversight(out):
    c = Cover(WARM, halftone=shade(ORANGE, 0.55))
    c.slant(100, 110, 380, 64, ORANGE)
    # A person (head and shoulders) beside a big approve button.
    c.circle(520, 380, 130, NAVY)
    c.poly([(330, 800), (360, 600), (520, 540), (680, 600), (710, 800)], NAVY)
    c.circle(1080, 560, 250, RED)
    c.circle(1080, 540, 250, shade(RED, 0.15), shadow=False)
    check(c, 1080, 540, 220, WHITE)
    c.save(out)


def small_models(out):
    c = Cover(PAPER)
    c.slant(100, 110, 380, 64, BLUE)
    iso_cube(c, 560, 640, 320, SKY, BLUE, NAVY)
    iso_cube(c, 1170, 690, 150, (255, 214, 170), ORANGE, (190, 92, 30))
    # Speed lines: the small model gets there first.
    for i, w in enumerate([200, 140, 240]):
        c.line([(1000 - w, 520 + i * 70), (1000, 520 + i * 70)], 18, NAVY)
    c.save(out)


def check_source(out):
    c = Cover((205, 236, 222))
    c.slant(100, 110, 380, 64, NAVY)
    document(c, 230, 220, 520, 640, fill=WHITE, lines=7)
    # Chain link.
    for (cx, col) in [(950, NAVY), (1130, BLUE)]:
        c.rect(cx - 130, 470, cx + 130, 590, None or col, r=60)
        c.rect(cx - 90, 505, cx + 90, 555, (205, 236, 222), r=25, shadow=False)
    magnifier(c, 1220, 330, 120, rim=NAVY, glass=WHITE)
    check(c, 1220, 330, 100, BLUE)
    c.save(out)


def her_review(out):
    """For a review of Her: an earpiece and a voice, in the film's warm reds."""
    c = Cover((200, 34, 52), halftone=(230, 70, 80))
    c.slant(100, 110, 380, 64, WHITE)
    c.circle(800, 520, 150, WHITE)
    c.circle(800, 520, 70, (200, 34, 52), shadow=False)
    c.circle(800, 520, 30, WHITE, shadow=False)
    for i in range(3):
        c.arc(800, 520, 230 + i * 80, -40, 40, 26, WHITE)
        c.arc(800, 520, 230 + i * 80, 140, 220, 26, WHITE)
    c.save(out)


def turing_test(out):
    """Can a machine think? Two hidden players, one question."""
    c = Cover(NAVY)
    c.slant(90, 110, 380, 64, ORANGE)
    for x, label in [(260, "A"), (900, "B")]:
        c.rect(x, 280, x + 440, 620, WHITE, r=24)
        c.rect(x + 30, 310, x + 410, 560, SKY, r=12, shadow=False)
        c.text(x + 220, 435, label, 150, NAVY)
        c.rect(x + 160, 620, x + 280, 700, WHITE, shadow=False)
    c.text(800, 830, "?", 170, ORANGE)
    c.save(out)


def chinese_room(out):
    """A closed room with a slot: symbols in, symbols out."""
    c = Cover(WARM, halftone=shade(ORANGE, 0.55))
    c.slant(100, 110, 380, 64, NAVY)
    c.rect(520, 230, 1080, 860, NAVY)
    c.rect(690, 520, 910, 560, ORANGE, r=8, shadow=False)
    for x, y, col in [(300, 420, WHITE), (330, 660, WHITE), (1290, 400, ORANGE), (1310, 640, ORANGE)]:
        c.rect(x - 75, y - 75, x + 75, y + 75, col, r=12)
        c.ring(x, y, 34, 12, NAVY, shadow=False)
    c.text(800, 400, "?", 170, WHITE)
    c.save(out)


def astra(out):
    """Breaking: a model release. A rocket-bright star over the grid."""
    c = Cover(DEEP)
    for i in range(5):
        c.ring(800, 500, 120 + i * 90, 6, (40, 70, 140), shadow=False)
    c.star(800, 500, 230, WHITE, shadow=True)
    c.star(800, 500, 120, BLUE)
    c.save(out)


COVERS = {
    "turing-test": turing_test,
    "chinese-room": chinese_room,
    "astra": astra,
    "her-review": her_review,
    "eu-ai-act": eu_ai_act,
    "why-chatbots-guess": hallucination,
    "prompt-injection": prompt_injection,
    "context-window": context_window,
    "model-card": model_card,
    "cloned-voice": cloned_voice,
    "chat-privacy": chat_privacy,
    "retrieval-or-fine-tuning": rag_finetune,
    "ai-text-detectors": detectors,
    "who-owns-ai-made-work": copyright_ai,
    "agent-permissions": agent_permissions,
    "grounded-answers": grounded_answers,
    "image-provenance": provenance,
    "trust-ai": trust,
    "model-scores": model_scores,
    "open-weights": open_weights,
    "human-oversight": oversight,
    "small-models": small_models,
    "check-the-source": check_source,
}
if __name__ == "__main__":
    out_dir = Path(sys.argv[1])
    out_dir.mkdir(parents=True, exist_ok=True)
    wanted = set(sys.argv[2:])
    for name, draw in COVERS.items():
        if not wanted or name in wanted:
            draw(out_dir / f"{name}.webp")
            print("drew", name)
