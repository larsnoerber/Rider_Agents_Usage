"""Render the illustrated Marketplace preview. Requires Pillow; no account data is read."""

from html import escape
from math import cos, pi, sin
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "docs" / "images"
WIDTH, HEIGHT, SCALE = 1280, 980, 2
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
    '<desc>Compact usage bars for OpenAI, JetBrains AI and GitHub Copilot, plus status indicators. '
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

rectangle(100, 844, 1080, 46, PANEL, radius=6)
for x, parts in (
    (124, [("OpenAi | D=", TEXT), ("78%", GREEN), (" - W=", TEXT), ("42%", AMBER)]),
    (600, [("JetBrainAi | ", TEXT), ("70%", GREEN)]),
    (948, [("Copilot | ", TEXT), ("38%", GREEN)]),
):
    font = ImageFont.truetype(str(Path("C:/Windows/Fonts/segoeui.ttf")), 23 * SCALE)
    for text, color in parts:
        label(x, 854, text, size=23, color=color)
        x += font.getlength(text) / SCALE

label(100, 928, "Illustrated preview · Example balances · Copilot shows consumed quota", size=20, color=MUTED)
svg.append("</svg>")

OUTPUT.mkdir(parents=True, exist_ok=True)
(OUTPUT / "agents-usage-overview.svg").write_text("\n".join(svg) + "\n", encoding="utf-8")
image.resize((WIDTH, HEIGHT), Image.Resampling.LANCZOS).save(OUTPUT / "agents-usage-overview.png", optimize=True)
print(f"Preview written to {OUTPUT}")
