"""Render illustrated overview, details and status bar media. Requires Pillow; no account data is read."""

from html import escape
from math import cos, pi, sin
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "docs" / "images"
WIDTH, HEIGHT, SCALE = 1280, 1320, 2
BACKGROUND = "#1E1F22"
PANEL = "#2B2D30"
TEXT = "#DFE1E5"
MUTED = "#A6A9B1"
GREEN = "#64CD87"
AMBER = "#FFB94B"
TRACK = "#3D4148"

image = Image.new("RGB", (WIDTH * SCALE, HEIGHT * SCALE), BACKGROUND)
draw = ImageDraw.Draw(image)
svg = [
    f'<svg xmlns="http://www.w3.org/2000/svg" width="{WIDTH}" height="{HEIGHT}" viewBox="0 0 {WIDTH} {HEIGHT}">',
    '<title>Agents Usage illustrated overview with example balances</title>',
    '<desc>Compact usage bars for OpenAI, JetBrains AI and GitHub Copilot, a weekly quest recap, and status indicators. '
    'This is an illustration, not a live account screenshot.</desc>',
]


def rectangle(x, y, width, height, color, radius=0):
    box = tuple(int(value * SCALE) for value in (x, y, x + width, y + height))
    draw.rounded_rectangle(box, radius=radius * SCALE, fill=color)
    svg.append(f'<rect x="{x}" y="{y}" width="{width}" height="{height}" '
               f'rx="{radius}" fill="{color}"/>')


def label(x, y, value, size=24, color=TEXT, bold=False, right=False):
    # y is the top of the text, shared by both outputs.
    filename = "segoeuib.ttf" if bold else "segoeui.ttf"
    font = ImageFont.truetype(str(Path("C:/Windows/Fonts") / filename), size * SCALE)
    draw.text((x * SCALE, y * SCALE), value, font=font, fill=color, anchor="rt" if right else "lt")
    svg.append(f'<text x="{x}" y="{y}" fill="{color}" font-family="Segoe UI, sans-serif" '
               f'font-size="{size}" font-weight="{700 if bold else 400}" '
               f'dominant-baseline="text-before-edge" text-anchor="{"end" if right else "start"}">'
               f'{escape(value)}</text>')


def circle(x, y, radius, color):
    draw.ellipse(tuple(int(v * SCALE) for v in (x - radius, y - radius, x + radius, y + radius)), fill=color)
    svg.append(f'<circle cx="{x}" cy="{y}" r="{radius}" fill="{color}"/>')


def line(points, color=MUTED, width=2):
    draw.line([(round(x * SCALE), round(y * SCALE)) for x, y in points], fill=color, width=width * SCALE)
    svg.append(f'<polyline points="{" ".join(f"{x},{y}" for x, y in points)}" '
               f'fill="none" stroke="{color}" stroke-width="{width}"/>')


def row(y, title, percent, remaining, consumed=False):
    balance = 100 - percent if consumed else percent
    color = GREEN if balance >= 50 else (AMBER if balance >= 20 else "#FF6E6E")
    label(132, y, title, size=24, color=MUTED)
    rectangle(362, y + 7, 530, 16, TRACK)
    if percent > 0:
        rectangle(362, y + 7, round(530 * percent / 100), 16, color)
    label(1148, y, remaining, size=24, color=color, bold=True, right=True)


def provider(y, name, plan, accent):
    rectangle(100, y - 12, 1080, 183, PANEL, radius=8)
    rectangle(106, y - 3, 4, 165, accent, radius=2)
    label(132, y, name, size=26, bold=True)
    rectangle(1025, y, 123, 30, TRACK, radius=4)
    label(1087, y + 2, plan, size=20, color=MUTED, right=True)


def facts(y, entries):
    font = ImageFont.truetype(str(Path("C:/Windows/Fonts/segoeui.ttf")), 18 * SCALE)
    for index, (name, value) in enumerate(entries):
        x = 132 if index % 2 == 0 else 680
        row_y = y + index // 2 * 26
        key = name + ":"
        label(x, row_y, key, size=18, color=MUTED)
        label(x + font.getlength(key) / SCALE + 9, row_y, value, size=18, bold=True)
    label(132, y + 54, "More details", size=18, color=MUTED)


def start_preview(width, height, title, description):
    global WIDTH, HEIGHT, image, draw, svg
    WIDTH, HEIGHT = width, height
    image = Image.new("RGB", (WIDTH * SCALE, HEIGHT * SCALE), BACKGROUND)
    draw = ImageDraw.Draw(image)
    svg = [
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{WIDTH}" height="{HEIGHT}" viewBox="0 0 {WIDTH} {HEIGHT}">',
        f'<title>{escape(title)}</title>',
        f'<desc>{escape(description)} This is an illustration with example balances, not a live account screenshot.</desc>',
    ]


def save_preview(name):
    OUTPUT.mkdir(parents=True, exist_ok=True)
    (OUTPUT / f"{name}.svg").write_text("\n".join(svg + ["</svg>"]) + "\n", encoding="utf-8")
    image.resize((WIDTH, HEIGHT), Image.Resampling.LANCZOS).save(OUTPUT / f"{name}.png", optimize=True)
    print(f"Preview written: {name}.png / .svg")


def preview_heading(subtitle):
    rectangle(100, 64, 62, 62, "#111827", radius=12)
    label(114, 74, "AI", size=30, bold=True)
    for x, color in ((114, "#5EEAD4"), (129, "#60A5FA"), (144, "#A78BFA")):
        rectangle(x, 113, 10, 3, color, radius=1)
    label(182, 64, "Agents Usage", size=42, bold=True)
    label(184, 117, subtitle, size=24, color=MUTED)


def text_width(value, size=18, bold=False):
    filename = "segoeuib.ttf" if bold else "segoeui.ttf"
    return ImageFont.truetype(str(Path("C:/Windows/Fonts") / filename), size * SCALE).getlength(value) / SCALE


def detail_card(x, name, plan, accent, bars, summary, details):
    rectangle(x, 210, 450, 764, PANEL, radius=8)
    rectangle(x + 5, 223, 4, 738, accent, radius=2)
    label(x + 18, 232, name, size=26, bold=True)
    rectangle(x + 344, 233, 88, 28, TRACK, radius=4)
    label(x + 388 + text_width(plan) / 2, 236, plan, size=18, color=MUTED, right=True)
    for index, (title, percent, value, consumed) in enumerate(bars):
        y = 280 + index * 32
        balance = 100 - percent if consumed else percent
        color = GREEN if balance >= 50 else AMBER if balance >= 20 else "#FF6E6E"
        label(x + 18, y, title, size=18, color=MUTED)
        rectangle(x + 180, y + 5, 126, 12, TRACK)
        if percent:
            rectangle(x + 180, y + 5, round(126 * percent / 100), 12, color)
        label(x + 432, y, value, size=18, color=color, bold=True, right=True)
    for index, (key, value) in enumerate(summary):
        cell_x = x + (18 if index % 2 == 0 else 232)
        y = 368 + index // 2 * 28
        label(cell_x, y, key + ":", size=17, color=MUTED)
        label(cell_x + text_width(key + ":", size=17) + 7, y, value, size=17, bold=True)
    for index, (key, value) in enumerate(details):
        y = 452 + index * 34
        label(x + 18, y, key + ":", size=18, color=MUTED)
        label(x + 18 + text_width(key + ":") + 7, y, value, size=18, bold=True)
    label(x + 18, 452 + len(details) * 34 + 12, "Fewer details", size=18, color=MUTED)


def status_bar(y, groups):
    rectangle(100, y, 1080, 68, PANEL, radius=6)
    x = 124
    for parts in groups:
        for value, color in parts:
            label(x, y + 17, value, size=28, color=color)
            x += text_width(value, size=28)
        x += 26


rectangle(0, 0, WIDTH, HEIGHT, BACKGROUND)
rectangle(100, 64, 62, 62, "#111827", radius=12)
label(114, 74, "AI", size=30, bold=True)
for x, color in ((114, "#5EEAD4"), (129, "#60A5FA"), (144, "#A78BFA")):
    rectangle(x, 113, 10, 3, color, radius=1)
label(182, 64, "Agents Usage", size=42, bold=True)
label(184, 117, "Usage overview and status indicators", size=24, color=MUTED)

label(132, 197, "Agents Usage", size=23, bold=True)
# Vector toolbar marks avoid platform-dependent symbol fonts.
line([(1068 + 10 * cos(t * pi / 18), 211 + 10 * sin(t * pi / 18)) for t in range(3, 34)])
line([(1077, 198), (1077, 207), (1068, 207)])
circle(1132, 211, 8, MUTED)
circle(1132, 211, 5, BACKGROUND)
for tooth in range(8):
    angle = tooth * pi / 4
    line([(1132 + r * cos(angle), 211 + r * sin(angle)) for r in (7, 12)], width=3)

provider(257, "OpenAI", "Plus", "#39AE99")
row(288, "5 hours", 78, "78%")
row(318, "This week", 42, "42%")
facts(349, [("Credits", "120"), ("Updated", "09:00"), ("5h reset", "16:30"), ("Week reset", "Oct 8 09:00")])

provider(454, "JetBrains AI", "AI Pro", "#9D77DC")
row(485, "Subscription", 65, "6.5 / 10")
row(515, "Top-up", 80, "4 / 5")
facts(546, [("Credits left", "10.5"), ("Used", "4.5"), ("Total", "15"), ("Reset", "Oct 20 09:00")])

provider(651, "GitHub Copilot", "Pro", "#5297E6")
row(682, "Premium requests", 38, "38%", consumed=True)
row(712, "Chat", 0, "Unlimited", consumed=True)
facts(743, [("Used", "114 / 300"), ("Available", "186 / 300"), ("Reset", "Nov 1 00:00"), ("Reported", "09:00")])

rectangle(100, 844, 1080, 292, PANEL, radius=8)
label(124, 858, "WEEKLY QUESTS", size=19, color="#D1B5F2", bold=True)
line([(278, 865), (284, 871), (290, 865)], color="#D1B5F2", width=2)
label(124, 888, "YOUR AI PARTY", size=14, color=MUTED, bold=True)
for x, text, color in ((124, "● OpenAI  78%", "#39AE99"), (290, "● JetBrains AI  70%", "#9D77DC"),
                       (500, "● Copilot  38%", "#5297E6")):
    label(x, 908, text, size=17, color=color, bold=True)
label(124, 934, "Forecast  ·  Copilot may reach 20% in about 3 days", size=17, color=MUTED)

rectangle(116, 963, 1048, 78, "#34303B", radius=6)
label(132, 973, "THE QUOTA WRAITH  ·  SHIELD CRACKED", size=16, color="#C397FA", bold=True)
label(1148, 973, "82% HP", size=16, color=MUTED, bold=True, right=True)
rectangle(132, 1000, 1016, 10, TRACK, radius=5)
rectangle(132, 1000, 833, 10, "#9D77DC", radius=5)
label(132, 1017, '"The Wraith gathers at the edge of the portal."', size=15, color=MUTED)

label(124, 1053, "LATEST BATTLE EVENTS", size=14, color=MUTED, bold=True)
circle(132, 1084, 4, "#C397FA")
label(145, 1075, "The Wraith · your agents used quota; the boss has taken", size=16, color="#C397FA", bold=True)
label(145, 1095, "12% damage this week", size=16, color="#C397FA", bold=True)
label(132, 1117, "·  Copilot hit for 8 quota points", size=16, color=MUTED)

rectangle(100, 1160, 1080, 46, PANEL, radius=6)
for x, parts in (
    (124, [("OpenAi | D=", TEXT), ("78%", GREEN), (" - W=", TEXT), ("42%", AMBER)]),
    (600, [("JetBrainAi | ", TEXT), ("70%", GREEN)]),
    (948, [("Copilot | ", TEXT), ("38%", GREEN)]),
):
    font = ImageFont.truetype(str(Path("C:/Windows/Fonts/segoeui.ttf")), 23 * SCALE)
    for text, color in parts:
        label(x, 1170, text, size=23, color=color)
        x += font.getlength(text) / SCALE

label(100, 1256, "Illustrated preview · Example balances · Copilot shows consumed quota", size=20, color=MUTED)
save_preview("agents-usage-overview")

start_preview(1600, 1080, "Agents Usage expanded agent details",
              "OpenAI, JetBrains AI and GitHub Copilot details shown side by side for readability.")
preview_heading("Expanded agent details · Quotas, reset dates and refresh status")
detail_card(100, "OpenAI", "Plus", "#39AE99",
            [("5 hours", 78, "78%", False), ("This week", 42, "42%", False)],
            [("Credits", "120"), ("Updated", "09:00"), ("5h reset", "16:30"), ("Week reset", "Oct 8 09:00")],
            [("Subscription", "Plus"), ("5h left", "78%"), ("Week left", "42%"),
             ("5h resets at", "Oct 3, 16:30"), ("Week resets at", "Oct 8, 09:00"),
             ("Refresh", "Every 60 seconds"), ("Status", "Usage available")])
detail_card(575, "JetBrains AI", "AI Pro", "#9D77DC",
            [("Subscription", 65, "6.5 / 10", False), ("Top-up", 80, "4 / 5", False)],
            [("Credits left", "10.5"), ("Used", "4.5"), ("Total", "15"), ("Reset", "Oct 20 09:00")],
            [("Subscription", "AI Pro"), ("Subscription left", "6.5 / 10 credits"),
             ("Top-up left", "4 / 5 credits"), ("Available", "70%"), ("Resets at", "Oct 20, 09:00"),
             ("Refresh", "Every 60 seconds"), ("Status", "Balance available")])
detail_card(1050, "GitHub Copilot", "Pro", "#5297E6",
            [("Premium requests", 38, "38%", True), ("Chat", 0, "Unlimited", True)],
            [("Used", "114 / 300"), ("Available", "186 / 300"), ("Reset", "Nov 1 00:00"), ("Reported", "09:00")],
            [("Subscription", "Pro"), ("Premium requests used", "114 / 300"),
             ("Premium requests left", "186 / 300"), ("Chat used", "Unlimited"), ("Chat left", "Unlimited"),
             ("Resets at", "Nov 1, 00:00"), ("Last report", "Oct 3, 09:00"),
             ("Refresh", "Every 60 seconds"), ("Status", "Report available")])
label(100, 1022, "Illustrated preview · Example balances · Expanded views arranged side by side", size=20, color=MUTED)
save_preview("agents-usage-details")

start_preview(1280, 720, "Agents Usage status bar",
              "Colored quota percentages, adjacent agent widgets and an example with JetBrains AI hidden.")
preview_heading("Status bar · Colored percentages and adjacent agent widgets")
openai_parts = [("OpenAi | D=", TEXT), ("78%", GREEN), (" - W=", TEXT), ("42%", AMBER)]
jetbrains_parts = [("JetBrainAi | ", TEXT), ("70%", GREEN)]
copilot_parts = [("Copilot | ", TEXT), ("38%", GREEN)]
label(100, 208, "All selected agents", size=23, bold=True)
status_bar(252, [openai_parts, jetbrains_parts, copilot_parts])
label(100, 346, "OpenAI / JetBrains AI: remaining quota · Copilot: consumed quota", size=22, color=MUTED)
label(100, 436, "JetBrains AI hidden — remaining agents stay together", size=23, bold=True)
status_bar(480, [openai_parts, copilot_parts])
label(100, 652, "Illustrated preview · Example balances · No usage dots", size=20, color=MUTED)
save_preview("agents-usage-statusbar")
