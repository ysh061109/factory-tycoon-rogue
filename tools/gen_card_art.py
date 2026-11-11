#!/usr/bin/env python3
"""
Draws the fantasy card images: one ornate frame per rarity and one illustration per card.

Run:  python3 tools/gen_card_art.py
Output: src/main/resources/assets/tycoonrogue/textures/gui/card/frame_<rarity>.png   (116x168)
        src/main/resources/assets/tycoonrogue/textures/gui/card/art/<card id>.png    (92x58)

Illustrations are pixel art: drawn on a 46x29 grid with an outline and a drop shadow, then
enlarged 2x without smoothing. To add a card, add an ICONS entry with the card's id.
"""
import math
import os
import random
import sys

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_cards  # noqa: E402

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources",
                   "assets", "tycoonrogue", "textures", "gui", "card")

W, H = 46, 29


def rgb(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def lighten(c, f):
    return tuple(min(255, int(v + (255 - v) * f)) for v in c[:3]) + (c[3],)


def darken(c, f):
    return tuple(int(v * (1 - f)) for v in c[:3]) + (c[3],)


# rarity -> (main, light, dark, bg top, bg bottom, glow)
PAL = {
    "common": dict(main=rgb("#b8bec8"), bg1=rgb("#27364a"), bg2=rgb("#0f1a26"), glow=rgb("#6fa8d8")),
    "rare": dict(main=rgb("#4fa3ff"), bg1=rgb("#1d3f7a"), bg2=rgb("#0a1636"), glow=rgb("#5fd0ff")),
    "legendary": dict(main=rgb("#ffb92e"), bg1=rgb("#6a2a7a"), bg2=rgb("#1d0a2c"), glow=rgb("#ffd96a")),
}
GEMS = {"common": 1, "rare": 2, "legendary": 3}

GOLD = rgb("#ffcf3f")
GOLD_D = rgb("#c08a10")
WHITE = rgb("#f4f6fa")
GRAY = rgb("#9aa3b2")
GRAY_D = rgb("#5d6676")
RED = rgb("#e04848")
RED_D = rgb("#a02828")
BLUE = rgb("#4fa3ff")
BROWN = rgb("#8a5a2c")
BROWN_D = rgb("#5a3a18")
GREEN = rgb("#52c26a")
CYAN = rgb("#6fe8ff")
PINK = rgb("#ff5fa8")
ORANGE = rgb("#ff9a2e")
CREAM = rgb("#f3e6c4")
BLACK = rgb("#16121c")


# ---------------------------------------------------------------- icons (drawn on a 46x29 layer)

def heart(d, cx, cy, s=1.0, color=RED, hi=True):
    r = 4 * s
    d.ellipse([cx - 2 * r + r * 0.4, cy - r, cx + r * 0.4, cy + r * 0.9], fill=color)
    d.ellipse([cx - r * 0.4, cy - r, cx + 2 * r - r * 0.4, cy + r * 0.9], fill=color)
    d.polygon([(cx - 2 * r + r * 0.55, cy + r * 0.35), (cx + 2 * r - r * 0.55, cy + r * 0.35), (cx, cy + 2.6 * r)], fill=color)
    if hi:
        d.rectangle([cx - r * 1.3, cy - r * 0.55, cx - r * 0.8, cy - r * 0.2], fill=lighten(color, 0.6))


def star(d, cx, cy, r, color=GOLD, inner=0.45):
    pts = []
    for i in range(10):
        ang = -math.pi / 2 + i * math.pi / 5
        rad = r if i % 2 == 0 else r * inner
        pts.append((cx + math.cos(ang) * rad, cy + math.sin(ang) * rad))
    d.polygon(pts, fill=color)


def sparkle(d, cx, cy, r, color=WHITE):
    d.polygon([(cx, cy - r), (cx + r * 0.25, cy - r * 0.25), (cx + r, cy), (cx + r * 0.25, cy + r * 0.25),
               (cx, cy + r), (cx - r * 0.25, cy + r * 0.25), (cx - r, cy), (cx - r * 0.25, cy - r * 0.25)], fill=color)


def car(d, ox=0, oy=0, s=1.0, body=RED):
    def P(x, y):
        return (ox + x * s, oy + y * s)
    d.polygon([P(6, 18), P(6, 13), P(14, 11), P(18, 6), P(30, 6), P(35, 11), P(41, 13), P(41, 18)], fill=body)
    d.polygon([P(18, 7), P(21, 7), P(21, 11), P(15, 11)], fill=rgb("#bfe8ff"))
    d.polygon([P(23, 7), P(29, 7), P(33, 11), P(23, 11)], fill=rgb("#bfe8ff"))
    d.rectangle([P(6, 16)[0], P(6, 16)[1], P(41, 16)[0], P(41, 17)[1]], fill=darken(body, 0.3))
    for wx in (14, 33):
        cx, cy = P(wx, 19)
        d.ellipse([cx - 4 * s, cy - 4 * s, cx + 4 * s, cy + 4 * s], fill=BLACK)
        d.ellipse([cx - 1.6 * s, cy - 1.6 * s, cx + 1.6 * s, cy + 1.6 * s], fill=GRAY)
    d.rectangle([P(39, 13)[0], P(39, 13)[1], P(41, 14)[0], P(41, 14)[1]], fill=rgb("#ffe680"))


def plane(d):
    d.ellipse([6, 11, 40, 19], fill=WHITE)
    d.polygon([(36, 12), (44, 8), (44, 11), (40, 15)], fill=GRAY)
    d.polygon([(6, 12), (3, 6), (7, 6), (12, 12)], fill=GRAY)
    d.polygon([(20, 14), (28, 14), (21, 26), (15, 26)], fill=BLUE)
    d.polygon([(20, 15), (28, 15), (20, 3), (15, 3)], fill=rgb("#7fbfff"))
    for x in range(14, 34, 5):
        d.rectangle([x, 13, x + 1, 14], fill=rgb("#4a6a90"))
    d.ellipse([36, 12, 41, 16], fill=rgb("#bfe8ff"))


def icon_megaphone(d):
    d.polygon([(10, 11), (28, 4), (28, 23), (10, 18)], fill=ORANGE)
    d.polygon([(10, 11), (28, 4), (28, 8), (10, 14)], fill=lighten(ORANGE, 0.4))
    d.ellipse([25, 4, 32, 23], fill=rgb("#ffd36b"))
    d.rectangle([12, 18, 17, 25], fill=BROWN)
    d.rectangle([5, 12, 10, 17], fill=GRAY_D)
    for i, r in enumerate((6, 10, 14)):
        d.arc([30, 14 - r, 30 + r * 1.2, 14 + r], -55, 55, fill=WHITE, width=1)


def icon_tools(d):
    d.line([(10, 25), (28, 7)], fill=BROWN, width=3)
    d.polygon([(23, 3), (34, 6), (36, 12), (31, 13), (26, 9)], fill=GRAY)
    d.polygon([(23, 3), (34, 6), (33, 8), (24, 6)], fill=lighten(GRAY, 0.5))
    d.line([(36, 25), (18, 7)], fill=BROWN_D, width=3)
    d.arc([8, 1, 28, 21], 200, 290, fill=GRAY, width=4)
    d.arc([8, 1, 28, 21], 220, 270, fill=WHITE, width=1)


def icon_chef(d):
    for box in ([10, 4, 21, 15], [17, 1, 29, 13], [25, 4, 36, 15]):
        d.ellipse(box, fill=WHITE)
    d.rectangle([13, 11, 33, 23], fill=WHITE)
    d.rectangle([13, 20, 33, 23], fill=rgb("#d5dae4"))
    d.line([(18, 12), (18, 19)], fill=rgb("#d5dae4"))
    d.line([(24, 12), (24, 19)], fill=rgb("#d5dae4"))
    d.line([(29, 12), (29, 19)], fill=rgb("#d5dae4"))
    sparkle(d, 38, 6, 3, GOLD)


def icon_service(d):
    heart(d, 23, 11, 1.6)
    d.polygon([(10, 22), (14, 18), (18, 22), (14, 26)], fill=GOLD)
    sparkle(d, 36, 8, 3, WHITE)
    sparkle(d, 9, 8, 2, CYAN)


def icon_neon(d):
    d.rounded_rectangle([7, 5, 39, 22], radius=3, fill=rgb("#201530"), outline=PINK, width=2)
    d.rectangle([22, 22, 24, 27], fill=GRAY_D)
    try:
        font = ImageFont.load_default()
        d.text((11, 8), "OPEN", fill=CYAN, font=font)
    except Exception:
        pass
    d.line([(10, 19), (36, 19)], fill=PINK)


def icon_book(d):
    d.polygon([(6, 9), (22, 12), (22, 25), (6, 22)], fill=CREAM)
    d.polygon([(40, 9), (24, 12), (24, 25), (40, 22)], fill=lighten(CREAM, 0.2))
    d.polygon([(6, 22), (22, 25), (24, 25), (40, 22), (40, 25), (23, 28), (6, 25)], fill=BROWN)
    d.line([(23, 12), (23, 26)], fill=BROWN_D, width=1)
    for y in (14, 17, 20):
        d.line([(9, y - 1), (19, y + 1)], fill=GRAY)
        d.line([(27, y + 1), (37, y - 1)], fill=GRAY)
    d.line([(34, 2), (26, 13)], fill=WHITE, width=2)
    d.polygon([(26, 13), (24, 17), (28, 14)], fill=GOLD_D)


def icon_viral(d):
    d.rounded_rectangle([15, 2, 31, 27], radius=3, fill=BLACK, outline=GRAY, width=1)
    d.rectangle([17, 6, 29, 23], fill=rgb("#2d6cdf"))
    heart(d, 23, 12, 1.0, RED)
    heart(d, 36, 8, 0.7, PINK)
    heart(d, 9, 12, 0.6, PINK)
    heart(d, 38, 20, 0.5, RED)
    d.rectangle([20, 20, 26, 21], fill=WHITE)


def icon_shield(d):
    pts = [(23, 2), (38, 6), (36, 19), (23, 28), (10, 19), (8, 6)]
    d.polygon(pts, fill=GOLD_D)
    d.polygon([(23, 5), (35, 8), (33, 18), (23, 25), (13, 18), (11, 8)], fill=rgb("#3b6fb0"))
    d.polygon([(23, 5), (35, 8), (34, 12), (23, 12)], fill=rgb("#5f95d6"))
    d.rectangle([21, 8, 25, 21], fill=WHITE)
    d.rectangle([16, 12, 30, 15], fill=WHITE)


def icon_gem(d):
    d.polygon([(11, 10), (17, 4), (29, 4), (35, 10), (23, 26)], fill=CYAN)
    d.polygon([(17, 4), (29, 4), (26, 10), (20, 10)], fill=lighten(CYAN, 0.6))
    d.polygon([(11, 10), (20, 10), (23, 26)], fill=rgb("#3ab0d0"))
    d.line([(20, 10), (23, 26)], fill=WHITE)
    d.line([(26, 10), (23, 26)], fill=rgb("#2a90b0"))
    sparkle(d, 38, 6, 3, WHITE)
    sparkle(d, 8, 20, 2, GOLD)


def icon_coffee(d):
    d.polygon([(11, 10), (31, 10), (29, 25), (13, 25)], fill=WHITE)
    d.ellipse([11, 7, 31, 14], fill=rgb("#6a3a1c"))
    d.ellipse([13, 8, 29, 12], fill=rgb("#8a5230"))
    d.arc([28, 12, 38, 22], -90, 90, fill=WHITE, width=2)
    d.ellipse([9, 24, 33, 28], fill=GRAY)
    for x, ph in ((16, 0), (22, 2), (27, 1)):
        d.arc([x - 2, 0 + ph, x + 3, 7 + ph], 90, 270, fill=rgb("#dfe6f0"), width=1)
    d.rectangle([14, 17, 28, 18], fill=rgb("#c8505a"))


def icon_cola(d):
    d.polygon([(19, 4), (27, 4), (27, 10), (31, 14), (31, 26), (15, 26), (15, 14), (19, 10)], fill=rgb("#5a2a1a"))
    d.rectangle([19, 2, 27, 4], fill=RED)
    d.rectangle([15, 15, 31, 22], fill=RED)
    d.rectangle([15, 17, 31, 19], fill=WHITE)
    d.line([(17, 12), (17, 24)], fill=rgb("#a06a50"))
    for x, y in ((8, 8), (11, 14), (36, 10), (39, 18), (6, 20)):
        d.ellipse([x, y, x + 3, y + 3], outline=WHITE)


def icon_car(d):
    car(d, 0, 2, 1.0)


def icon_hourglass_bolt(d):
    d.polygon([(26, 1), (12, 16), (21, 16), (16, 28), (34, 11), (25, 11)], fill=rgb("#ffe15a"))
    d.polygon([(26, 1), (22, 6), (27, 6), (25, 11)], fill=WHITE)
    d.arc([2, 5, 16, 19], 0, 360, fill=CYAN, width=1)
    d.line([(9, 8), (9, 12)], fill=CYAN)
    d.line([(9, 12), (12, 13)], fill=CYAN)


def icon_magnate(d):
    d.rectangle([11, 3, 25, 16], fill=BLACK)
    d.ellipse([7, 14, 29, 20], fill=BLACK)
    d.rectangle([11, 12, 25, 15], fill=RED)
    d.rectangle([13, 4, 15, 11], fill=rgb("#3a3248"))
    for i, (x, y) in enumerate(((30, 24), (34, 21), (38, 24))):
        for k in range(3):
            d.ellipse([x - 4, y - k * 3 - 2, x + 4, y - k * 3 + 2], fill=GOLD, outline=GOLD_D)
    sparkle(d, 40, 8, 3, GOLD)


def icon_castle(d):
    d.rectangle([8, 12, 38, 27], fill=GRAY)
    d.rectangle([8, 12, 38, 14], fill=lighten(GRAY, 0.4))
    for x in range(8, 38, 6):
        d.rectangle([x, 8, x + 3, 12], fill=GRAY)
    d.rectangle([5, 6, 13, 27], fill=rgb("#8a93a4"))
    d.rectangle([33, 6, 41, 27], fill=rgb("#8a93a4"))
    for x in (5, 9, 13):
        d.rectangle([x - 1, 3, x + 1, 6], fill=rgb("#8a93a4"))
    for x in (33, 37, 41):
        d.rectangle([x - 1, 3, x + 1, 6], fill=rgb("#8a93a4"))
    d.rectangle([20, 17, 26, 27], fill=BLACK)
    d.ellipse([20, 14, 26, 20], fill=BLACK)
    d.rectangle([22, 19, 24, 27], fill=GOLD)
    d.line([(23, 1), (23, 8)], fill=BROWN_D)
    d.polygon([(23, 1), (30, 3), (23, 5)], fill=RED)
    d.rectangle([8, 17, 10, 21], fill=rgb("#2a3040"))
    d.rectangle([36, 17, 38, 21], fill=rgb("#2a3040"))


def icon_star(d):
    star(d, 23, 15, 13, GOLD)
    star(d, 23, 15, 8, lighten(GOLD, 0.5), 0.5)
    sparkle(d, 6, 7, 3, WHITE)
    sparkle(d, 40, 8, 3, WHITE)
    sparkle(d, 38, 24, 2, CYAN)
    sparkle(d, 8, 23, 2, PINK)


def icon_showroom(d):
    d.ellipse([4, 21, 42, 28], fill=rgb("#8fb4d8"))
    d.ellipse([10, 22, 36, 27], fill=rgb("#cfe6ff"))
    sparkle(d, 23, 12, 11, WHITE)
    sparkle(d, 10, 8, 4, CYAN)
    sparkle(d, 37, 7, 4, CYAN)
    sparkle(d, 36, 18, 3, GOLD)
    sparkle(d, 10, 17, 2, GOLD)


def icon_car_museum(d):
    d.rectangle([5, 2, 41, 26], fill=GOLD_D)
    d.rectangle([7, 4, 39, 24], fill=GOLD)
    d.rectangle([9, 6, 37, 22], fill=rgb("#1a2438"))
    car(d, 6, 4, 0.7, rgb("#4fa3ff"))
    d.line([(10, 7), (14, 7)], fill=rgb("#506080"))


def icon_hangar(d):
    d.arc([2, 8, 44, 40], 180, 360, fill=GRAY, width=3)
    d.rectangle([2, 24, 44, 27], fill=GRAY_D)
    plane(d)


def icon_tour(d):
    d.line([(14, 2), (14, 26)], fill=BROWN, width=2)
    d.polygon([(15, 3), (36, 8), (15, 14)], fill=RED)
    d.polygon([(15, 3), (36, 8), (30, 6), (15, 5)], fill=lighten(RED, 0.4))
    d.ellipse([28, 15, 38, 25], fill=BLUE)
    d.polygon([(28, 21), (38, 21), (33, 28)], fill=BLUE)
    d.ellipse([31, 18, 35, 22], fill=WHITE)
    d.ellipse([8, 25, 20, 28], fill=GRAY_D)


def icon_trophy(d):
    d.polygon([(14, 3), (32, 3), (30, 15), (23, 19), (16, 15)], fill=GOLD)
    d.arc([5, 4, 17, 14], 90, 270, fill=GOLD, width=2)
    d.arc([29, 4, 41, 14], -90, 90, fill=GOLD, width=2)
    d.rectangle([21, 18, 25, 23], fill=GOLD_D)
    d.rectangle([15, 23, 31, 27], fill=BROWN)
    d.rectangle([17, 24, 29, 25], fill=GOLD)
    d.polygon([(14, 3), (18, 3), (19, 12), (17, 13)], fill=lighten(GOLD, 0.5))
    star(d, 23, 10, 4, WHITE)
    sparkle(d, 6, 22, 3, PINK)
    sparkle(d, 40, 20, 3, CYAN)
    sparkle(d, 38, 4, 2, WHITE)


ICONS = {
    "word_of_mouth": icon_megaphone, "tool_rush": icon_tools, "kitchen_rush": icon_chef,
    "polite_service": icon_service, "neon_sign": icon_neon, "history_corner": icon_book,
    "viral_post": icon_viral, "security_guard": icon_shield, "vip_lounge": icon_gem,
    "coffee_craze": icon_coffee, "auto_show": icon_car, "rush_hour": icon_hourglass_bolt,
    "tycoon_magnate": icon_magnate, "fort_knox": icon_castle, "celebrity_visit": icon_star,
    "polished_showroom": icon_showroom, "car_museum": icon_car_museum, "airplane_hangar": icon_hangar,
    "tour_guide": icon_tour, "grand_expo": icon_trophy,
}


# ---- small story elements that tie an illustration to what the card does ----

def villager(d, x, y, skin=rgb("#c79a77")):
    """A tiny customer head, 6x7 pixels."""
    d.rectangle([x, y, x + 5, y + 6], fill=skin)
    d.rectangle([x + 2, y + 3, x + 3, y + 6], fill=darken(skin, 0.2))
    d.point([(x + 1, y + 2), (x + 4, y + 2)], fill=rgb("#2d6a2d"))
    d.rectangle([x, y - 1, x + 5, y], fill=BROWN_D)


def emerald(d, x, y, s=1.0):
    pts = [(x, y - 4 * s), (x + 3 * s, y - 1 * s), (x + 3 * s, y + 2 * s), (x, y + 4 * s), (x - 3 * s, y + 2 * s), (x - 3 * s, y - 1 * s)]
    d.polygon(pts, fill=rgb("#2fd36b"))
    d.polygon([(x, y - 4 * s), (x + 3 * s, y - 1 * s), (x, y), (x - 3 * s, y - 1 * s)], fill=rgb("#8dffb0"))


def thief(d, x, y):
    """A masked robber head with a red warning mark."""
    villager(d, x, y, rgb("#b08868"))
    d.rectangle([x, y + 1, x + 5, y + 3], fill=BLACK)
    d.point([(x + 1, y + 2), (x + 4, y + 2)], fill=WHITE)


def crowd(d, n=3):
    for i in range(n):
        villager(d, 2 + i * 7, 22 + (i % 2))


def with_extra(base, extra):
    def draw(d):
        base(d)
        extra(d)
    return draw


def x_crowd(d):
    for i in range(3):
        villager(d, 30 + i * 5 - 2, 22 + (i % 2))


def x_money(d):
    emerald(d, 6, 23, 1.2)
    emerald(d, 14, 25, 0.9)


def x_thieves(d):
    thief(d, 3, 21)
    d.line([(11, 21), (16, 27)], fill=RED, width=2)
    d.line([(16, 21), (11, 27)], fill=RED, width=2)


ICONS.update({
    "word_of_mouth": with_extra(icon_megaphone, x_crowd),
    "neon_sign": with_extra(icon_neon, crowd),
    "celebrity_visit": with_extra(icon_star, crowd),
    "polite_service": with_extra(icon_service, x_money),
    "tool_rush": with_extra(icon_tools, x_money),
    "kitchen_rush": with_extra(icon_chef, x_money),
    "vip_lounge": with_extra(icon_gem, x_money),
    "rush_hour": with_extra(icon_hourglass_bolt, x_money),
    "tycoon_magnate": with_extra(icon_magnate, lambda d: emerald(d, 7, 24, 1.1)),
    "security_guard": with_extra(icon_shield, x_thieves),
    "fort_knox": with_extra(icon_castle, lambda d: thief(d, 38, 22) if False else None),
})


def outline(layer, color=rgb("#0d0a12")):
    """One pixel dark outline around everything drawn on the layer."""
    w, h = layer.size
    px = layer.load()
    out = Image.new("RGBA", layer.size, (0, 0, 0, 0))
    op = out.load()
    for y in range(h):
        for x in range(w):
            if px[x, y][3] > 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (1, -1), (-1, 1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and px[nx, ny][3] > 0:
                    op[x, y] = color
                    break
    out.alpha_composite(layer)
    return out


def threshold_alpha(layer):
    """Removes the soft edge PIL leaves on shapes, so pixel art stays crisp."""
    px = layer.load()
    for y in range(layer.size[1]):
        for x in range(layer.size[0]):
            r, g, b, a = px[x, y]
            px[x, y] = (r, g, b, 255 if a >= 128 else 0)


def make_art(card_id, rarity):
    pal = PAL[rarity]
    rnd = random.Random(card_id)
    bg = Image.new("RGBA", (W, H))
    bp = bg.load()
    cx, cy = W / 2, H / 2 + 1
    for y in range(H):
        t = y / (H - 1)
        base = tuple(int(pal["bg1"][i] * (1 - t) + pal["bg2"][i] * t) for i in range(3))
        for x in range(W):
            dist = math.hypot((x - cx) / 1.6, y - cy) / 14.0
            glow = max(0.0, 1.0 - dist) ** 2 * 0.55
            col = tuple(min(255, int(base[i] * (1 - glow) + pal["glow"][i] * glow)) for i in range(3))
            bp[x, y] = col + (255,)
    d = ImageDraw.Draw(bg)
    if rarity == "legendary":
        for i in range(10):
            a0 = i * math.pi / 5
            a1 = a0 + math.pi / 18
            d.polygon([(cx, cy), (cx + math.cos(a0) * 40, cy + math.sin(a0) * 40),
                       (cx + math.cos(a1) * 40, cy + math.sin(a1) * 40)], fill=(255, 230, 140, 40))
    for _ in range(9 if rarity != "common" else 5):
        x, y = rnd.randrange(1, W - 1), rnd.randrange(1, H - 1)
        if abs(x - cx) < 14 and abs(y - cy) < 10:
            continue
        bp[x, y] = lighten(pal["glow"], 0.5)
        if rnd.random() < 0.4:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if 0 <= x + dx < W and 0 <= y + dy < H:
                    bp[x + dx, y + dy] = lighten(pal["glow"], 0.2)

    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ICONS[card_id](ImageDraw.Draw(layer))
    threshold_alpha(layer)
    layer = outline(layer)

    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sp = shadow.load()
    lp = layer.load()
    for y in range(H - 1):
        for x in range(W):
            if lp[x, y][3] > 0 and sp[x, y + 1][3] == 0:
                sp[x, y + 1] = (0, 0, 0, 90)
    bg.alpha_composite(shadow)
    bg.alpha_composite(layer)
    return bg.resize((W * 2, H * 2), Image.NEAREST)


# ---------------------------------------------------------------- frames (116x168)

def make_frame(rarity):
    fw, fh = 116, 168
    pal = PAL[rarity]
    main = pal["main"]
    hi, lo = lighten(main, 0.55), darken(main, 0.45)
    img = Image.new("RGBA", (fw, fh), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    ink = rgb("#0d0a12")
    d.rounded_rectangle([0, 0, fw - 1, fh - 1], radius=6, fill=ink)
    d.rounded_rectangle([2, 2, fw - 3, fh - 3], radius=5, fill=main)
    d.rounded_rectangle([3, 3, fw - 4, fh - 4], radius=4, fill=lo)
    d.rounded_rectangle([4, 4, fw - 5, fh - 5], radius=3, fill=rgb("#1b1426"))
    # bevel highlights
    d.line([(6, 2), (fw - 7, 2)], fill=hi)
    d.line([(2, 6), (2, fh - 7)], fill=hi)
    # parchment texture dots
    rnd = random.Random(rarity)
    for _ in range(140):
        x, y = rnd.randrange(6, fw - 6), rnd.randrange(6, fh - 6)
        d.point((x, y), fill=rgb("#241a32"))
    # top plate (rarity name goes here)
    d.rounded_rectangle([16, 5, fw - 17, 21], radius=3, fill=ink)
    d.rounded_rectangle([17, 6, fw - 18, 20], radius=3, fill=darken(main, 0.25))
    d.rounded_rectangle([18, 7, fw - 19, 19], radius=2, fill=main)
    for sx in (1, -1):
        px0 = 16 if sx == 1 else fw - 17
        d.polygon([(px0, 8), (px0 - 8 * sx, 13), (px0, 18)], fill=ink)
        d.polygon([(px0 - 1 * sx, 10), (px0 - 6 * sx, 13), (px0 - 1 * sx, 16)], fill=main)
    # art window
    d.rectangle([9, 23, 106, 86], fill=ink)
    d.rectangle([10, 24, 105, 85], fill=main)
    d.rectangle([11, 25, 104, 84], fill=lo)
    d.rectangle([12, 26, 103, 83], fill=(0, 0, 0, 0))  # transparent: the illustration shows through
    d.line([(10, 24), (105, 24)], fill=hi)
    # corner gems on art window
    for gx, gy in ((10, 24), (105, 24), (10, 85), (105, 85)):
        d.polygon([(gx, gy - 3), (gx + 3, gy), (gx, gy + 3), (gx - 3, gy)], fill=ink)
        d.polygon([(gx, gy - 2), (gx + 2, gy), (gx, gy + 2), (gx - 2, gy)], fill=hi)
    # name ribbon
    d.polygon([(2, 90), (12, 90), (12, 108), (2, 108), (7, 99)], fill=ink)
    d.polygon([(fw - 3, 90), (fw - 13, 90), (fw - 13, 108), (fw - 3, 108), (fw - 8, 99)], fill=ink)
    d.rectangle([11, 88, fw - 12, 109], fill=ink)
    d.rectangle([12, 89, fw - 13, 108], fill=main)
    d.rectangle([12, 90, fw - 13, 107], fill=rgb("#2a1f3a"))
    d.line([(12, 89), (fw - 13, 89)], fill=hi)
    # description panel
    d.rectangle([9, 112, 106, 163], fill=ink)
    d.rectangle([10, 113, 105, 162], fill=lo)
    d.rectangle([11, 114, 104, 161], fill=rgb("#120d1a"))
    # gems on the bottom edge
    n = GEMS[rarity]
    start = fw // 2 - (n - 1) * 7
    for i in range(n):
        gx = start + i * 14
        d.polygon([(gx, 161), (gx + 4, 165), (gx, 169 - 1), (gx - 4, 165)], fill=ink)
        d.polygon([(gx, 162), (gx + 3, 165), (gx, 167), (gx - 3, 165)], fill=main)
        d.polygon([(gx, 162), (gx + 1, 165), (gx, 165), (gx - 1, 164)], fill=WHITE)
    # corner ornaments
    for ox, oy in ((5, 5), (fw - 6, 5), (5, fh - 6), (fw - 6, fh - 6)):
        d.polygon([(ox, oy - 3), (ox + 3, oy), (ox, oy + 3), (ox - 3, oy)], fill=ink)
        d.polygon([(ox, oy - 2), (ox + 2, oy), (ox, oy + 2), (ox - 2, oy)], fill=hi)
    return img


def main():
    os.makedirs(os.path.join(OUT, "art"), exist_ok=True)
    for rarity in PAL:
        make_frame(rarity).save(os.path.join(OUT, "frame_%s.png" % rarity))
    count = 0
    ids = set()
    for cid, rarity, *_ in gen_cards.CARDS:
        ids.add(cid)
        if cid not in ICONS:
            raise SystemExit("no icon for card " + cid)
        make_art(cid, rarity).save(os.path.join(OUT, "art", cid + ".png"))
        count += 1
    for cid in ICONS:
        if cid not in ids:
            raise SystemExit("icon without a card: " + cid)
    print("wrote 3 frames and %d card illustrations to %s" % (count, os.path.abspath(OUT)))


if __name__ == "__main__":
    main()
