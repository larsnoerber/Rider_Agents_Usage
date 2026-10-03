"""Render the illustrated Marketplace preview. Requires Pillow; no account data is read."""

from html import escape
from math import cos, pi, sin
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "docs" / "images"
WIDTH, HEIGHT, SCALE = 1280, 800, 2
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
    '<svg xmlns="http://www.w3.org/2000/svg" width="1280" height="800" viewBox="0 0 1280 800">',
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


def row(y, title, percent, remaining):
    color = GREEN if percent >= 50 else AMBER
    label(132, y, title, size=24, color=MUTED)
    rectangle(362, y + 7, 530, 16, TRACK)
    rectangle(362, y + 7, round(530 * percent / 100), 16, color)
    label(1148, y, remaining, size=24, color=color, bold=True, right=True)


def provider(y, name, plan, plan_x):
    label(132, y, name, size=26, bold=True)
    label(plan_x, y + 2, plan, size=24, color=MUTED)


rectangle(0, 0, WIDTH, HEIGHT, BACKGROUND)
rectangle(100, 64, 62, 62, "#111827", radius=12)
label(114, 74, "AI", size=30, bold=True)
for x, color in ((114, "#5EEAD4"), (129, "#60A5FA"), (144, "#A78BFA")):
    rectangle(x, 113, 10, 3, color, radius=1)
label(182, 64, "Agents Usage", size=42, bold=True)
label(184, 117, "Usage overview and status indicators", size=24, color=MUTED)

rectangle(100, 180, 1080, 474, PANEL, radius=8)
label(132, 197, "Agents Usage", size=23, bold=True)
rectangle(100, 240, 1080, 1, TRACK)

provider(260, "OpenAI", "Plus", 239)
# Vector toolbar marks avoid platform-dependent symbol fonts.
line([(1068 + 10 * cos(t * pi / 18), 277 + 10 * sin(t * pi / 18)) for t in range(3, 34)])
line([(1077, 264), (1077, 273), (1068, 273)])
circle(1132, 277, 8, MUTED)
circle(1132, 277, 5, PANEL)
for tooth in range(8):
    angle = tooth * pi / 4
    line([(1132 + r * cos(angle), 277 + r * sin(angle)) for r in (7, 12)], width=3)
row(305, "5 hours", 78, "78%")
row(341, "This week", 42, "42%")

provider(391, "JetBrains AI", "AI Pro", 299)
row(436, "Subscription", 65, "6.5 / 10")
row(472, "Top-up", 80, "4 / 5")

provider(522, "GitHub Copilot", "Pro", 333)
row(567, "Premium requests", 62, "187 / 300")
row(603, "Chat", 100, "Unlimited")

rectangle(100, 672, 1080, 46, PANEL, radius=6)
for x, text, color in (
    (124, "OpenAi D 78% - W 42%", AMBER),
    (568, "JetbrainAi 70%", GREEN),
    (936, "Copilot 62%", GREEN),
):
    circle(x, 695, 5, color)
    label(x + 15, 682, text, size=23)

label(100, 749, "Illustrated preview · Example balances · Available quotas depend on your plan", size=20, color=MUTED)
svg.append("</svg>")

OUTPUT.mkdir(parents=True, exist_ok=True)
(OUTPUT / "agents-usage-overview.svg").write_text("\n".join(svg) + "\n", encoding="utf-8")
image.resize((WIDTH, HEIGHT), Image.Resampling.LANCZOS).save(OUTPUT / "agents-usage-overview.png", optimize=True)
print(f"Preview written to {OUTPUT}")
