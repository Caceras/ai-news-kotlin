"""Renders the three README app screens for both colour schemes.

Every dimension here is the value the app actually uses, read from MainActivity
(28dp content padding, 44dp nav gap, a 125dp date column, 22dp between rows) and
from ui/theme/Theme.kt for colour and type. 1dp is drawn as 1px.

Each screen shows exactly one state. In particular the update screen shows the
banner's Available state only: a build that is offered and a build that is
downloading are different states and never appear together.
"""
import sys

THEMES = {
    "light": dict(bg="#FFFFFF", ink="#111111", muted="#6E6D68", divider="#EEEEEC",
                  outline="#DCDCD8", backdrop="#F7F7F5",
                  shadow="0 1px 3px rgba(17,17,17,.05), 0 12px 34px rgba(17,17,17,.07)"),
    "dark": dict(bg="#0E0E0D", ink="#EDEDE8", muted="#8E8D88", divider="#222220",
                 outline="#333330", backdrop="#181816", shadow="none"),
}

SANS = "'Helvetica Neue', Helvetica, 'Liberation Sans', Arial, sans-serif"

# Seed headlines are the app's own offline edition (data/NewsRepository.kt).
FEED = [
    ("August 17, 2026", "The practical question for AI teams is no longer whether to use models, but where judgment stays human."),
    ("August 17, 2026", "Small models are becoming serious tools for private, local work."),
    ("August 16, 2026", "What an AI evaluation should reveal before a product reaches real people."),
    ("August 16, 2026", "The next AI product race will be decided by trust, not just capability."),
    ("August 15, 2026", "Open weights are expanding the set of people who can build with modern AI."),
]

CATEGORIES = ["all", "research", "products", "policy", "builders"]


def nav(active):
    return "".join(
        f'<span class="nav {"on" if t == active else "off"}">{t}</span>'
        for t in ("home", "saved", "about")
    )


def categories(active="all"):
    return "".join(
        f'<span class="cat {"on" if c == active else "off"}">{c}</span>'
        for c in CATEGORIES
    )


def feed_rows(items):
    return "".join(
        f'<div class="row"><div class="date">{d}</div><div class="title">{t}</div></div>'
        for d, t in items
    )


HEADER = (
    '<div class="h1">AI Brief</div>'
    '<div class="lede">A calm, text-first read on artificial intelligence '
    'models, research, and policy.</div>'
    f'<div class="cats">{categories()}</div>'
)

# Screen 1 — the feed.
SCREEN_FEED = f'<div class="nav-row">{nav("home")}</div>{HEADER}{feed_rows(FEED)}'

# Screen 2 — the reader. Nav shows no active tab, matching activeTab = null.
SCREEN_READER = f'''<div class="nav-row">{nav(None)}</div>
<div class="read-h1">Small models are becoming serious tools for private, local work.</div>
<div class="meta">August 17, 2026 &nbsp;&#8226;&nbsp; AI Brief &nbsp;&#8226;&nbsp; 4 min read</div>
<div class="prose">Efficiency gains are bringing capable language and
multimodal systems closer to the device, changing the tradeoff between privacy,
cost, and speed.</div>
<div class="prose">A model that runs locally never sends the work anywhere. For
notes, drafts, and anything covered by a duty of confidence, that single property
matters more than a few points of benchmark performance.</div>
<div class="prose">The gap that remains is tooling. Running a small model is
straightforward; evaluating one against the job it is actually doing is still
the harder half.</div>
<div class="rule"></div>
<div class="actions"><span class="act">save</span><span class="act">share</span><span class="act">open original</span></div>'''

# Screen 3 — the in-app updater, Available state only.
SCREEN_UPDATE = f'''<div class="nav-row">{nav("home")}</div>
<div class="banner">
  <div class="banner-text">
    <div class="banner-label">new build ready &nbsp;&#183;&nbsp; 2.1.0 (14)</div>
    <div class="banner-detail">Direct-install update loop</div>
  </div>
  <div class="banner-actions"><span class="act-strong">install</span><span class="act-weak">later</span></div>
</div>
{HEADER}{feed_rows(FEED)}'''

SCREENS = [("Feed", SCREEN_FEED), ("Reader", SCREEN_READER), ("In-app update", SCREEN_UPDATE)]

PAGE = '''<!doctype html><html><head><meta charset="utf-8"><style>
*{{box-sizing:border-box;margin:0;padding:0}}
html,body{{background:{backdrop};font-family:{sans};-webkit-font-smoothing:antialiased}}
.stage{{display:flex;gap:44px;padding:60px 56px 0}}
figure{{width:390px}}
.phone{{width:390px;height:844px;background:{bg};border:1px solid {outline};
  border-radius:36px;overflow:hidden;box-shadow:{shadow}}}
.inner{{padding:44px 28px 0}}
.nav-row{{display:flex;gap:28px;margin-bottom:44px}}
.nav{{font-size:16px;line-height:22px}}
.cat{{font-size:16px;line-height:22px}}
.on{{color:{ink};font-weight:500}}
.off{{color:{muted};font-weight:400}}
.h1{{font-size:32px;line-height:38px;font-weight:700;letter-spacing:-.8px;color:{ink}}}
.lede{{font-size:16.5px;line-height:26.5px;color:{ink};margin-top:14px;max-width:520px}}
.cats{{display:flex;gap:20px;margin-top:28px;margin-bottom:36px}}
.row{{display:flex;align-items:flex-start;margin-bottom:22px}}
.date{{width:125px;flex:none;font-size:14.5px;line-height:22px;color:{muted}}}
.title{{flex:1;font-size:16px;line-height:24px;color:{ink};padding-left:12px}}
.read-h1{{font-size:32px;line-height:38px;font-weight:700;letter-spacing:-.8px;color:{ink}}}
.meta{{font-size:14.5px;line-height:22px;color:{muted};margin-top:14px}}
.prose{{font-size:16.5px;line-height:26.5px;color:{ink};margin-top:26px}}
.rule{{height:1px;background:{divider};margin-top:36px}}
.actions{{display:flex;gap:24px;margin-top:28px}}
.act{{font-size:16px;color:{ink}}}
.banner{{display:flex;align-items:center;margin-bottom:28px}}
.banner-text{{flex:1}}
.banner-label{{font-size:16px;line-height:22px;color:{ink}}}
.banner-detail{{font-size:14.5px;line-height:22px;color:{muted};margin-top:4px}}
.banner-actions{{display:flex;gap:20px;padding-left:24px;flex:none}}
.act-strong{{font-size:16px;color:{ink};font-weight:500}}
.act-weak{{font-size:16px;color:{muted}}}
figcaption{{margin-top:22px;text-align:center;font-size:11.5px;letter-spacing:2.6px;
  color:{muted};text-transform:uppercase}}
</style></head><body><div class="stage">{figures}</div></body></html>
'''

theme = sys.argv[1]
colours = THEMES[theme]
figures = "".join(
    f'<figure><div class="phone"><div class="inner">{body}</div></div>'
    f'<figcaption>{caption}</figcaption></figure>'
    for caption, body in SCREENS
)
print(PAGE.format(sans=SANS, figures=figures, **colours))
