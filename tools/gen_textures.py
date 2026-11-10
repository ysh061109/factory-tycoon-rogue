#!/usr/bin/env python3
"""
Generates the job outfit textures for the Customer entity.

Each texture is a 64x64 transparent overlay that uses the vanilla villager UV layout,
so it is drawn on top of the vanilla villager + biome skin.

Run:  python3 tools/gen_textures.py
Output: src/main/resources/assets/tycoonrogue/textures/entity/customer/job/<job>.png
"""
import os
import sys

from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                   "assets", "tycoonrogue", "textures", "entity", "customer", "job")

# UV regions (x0, y0, x1, y1), end exclusive, from the vanilla villager model.
HAT_TOP = (40, 0, 48, 8)
HAT_FRONT = (40, 8, 48, 18)
HAT_RIGHT = (32, 8, 40, 18)
HAT_LEFT = (48, 8, 56, 18)
HAT_BACK = (56, 8, 64, 18)
HEAD_FRONT = (8, 8, 16, 18)

JACKET_FRONT = (6, 44, 14, 64)
JACKET_RIGHT = (0, 44, 6, 64)
JACKET_LEFT = (14, 44, 20, 64)
JACKET_BACK = (20, 44, 28, 64)
JACKET_TOP = (6, 38, 14, 44)

ARMS_CROSSED = (44, 22, 64, 30)   # the folded arms across the chest
ARM_HAND = (40, 38, 56, 50)       # the extra arm box

LEG_FRONT = (4, 26, 8, 38)
LEG_ALL = (0, 22, 16, 38)


def rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def shade(color, f):
    return tuple(max(0, min(255, int(c * f))) for c in color[:3]) + (255,)


class Canvas:
    def __init__(self):
        self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.img)

    def fill(self, region, color):
        x0, y0, x1, y1 = region
        self.d.rectangle([x0, y0, x1 - 1, y1 - 1], fill=color)

    def px(self, region, dx, dy, color):
        self.img.putpixel((region[0] + dx, region[1] + dy), color)

    def rect(self, region, dx, dy, w, h, color):
        x = region[0] + dx
        y = region[1] + dy
        self.d.rectangle([x, y, x + w - 1, y + h - 1], fill=color)

    def stripes(self, region, color, step=4):
        x0, y0, x1, y1 = region
        for y in range(y0, y1, step):
            self.d.line([(x0, y), (x1 - 1, y)], fill=color)

    def save(self, name):
        os.makedirs(OUT, exist_ok=True)
        self.img.save(os.path.join(OUT, name + ".png"))


def coat(c, main, trim, pants):
    """Coat on the jacket layer, sleeves on the arms, trousers on the legs."""
    for region in (JACKET_FRONT, JACKET_RIGHT, JACKET_LEFT, JACKET_BACK, JACKET_TOP):
        c.fill(region, main)
    # darker sides give the coat some depth
    c.fill(JACKET_RIGHT, shade(main, 0.85))
    c.fill(JACKET_LEFT, shade(main, 0.85))
    c.fill(JACKET_BACK, shade(main, 0.9))
    # hem and collar trim
    c.rect(JACKET_FRONT, 0, 18, 8, 2, trim)
    c.rect(JACKET_BACK, 0, 18, 8, 2, trim)
    c.rect(JACKET_FRONT, 0, 0, 8, 1, trim)
    c.fill(ARMS_CROSSED, main)
    c.fill(ARM_HAND, main)
    c.stripes((44, 28, 64, 30), trim, 1)
    c.fill(LEG_ALL, pants)
    c.fill((8, 26, 12, 38), shade(pants, 0.9))


def hat_cap(c, main, brim):
    c.fill(HAT_TOP, main)
    for region in (HAT_FRONT, HAT_RIGHT, HAT_LEFT, HAT_BACK):
        c.rect(region, 0, 0, region[2] - region[0], 3, main)
    c.rect(HAT_FRONT, 0, 3, 8, 1, brim)


def hat_hair(c, main):
    c.fill(HAT_TOP, main)
    for region in (HAT_RIGHT, HAT_LEFT, HAT_BACK):
        c.rect(region, 0, 0, region[2] - region[0], 4, main)
    c.rect(HAT_FRONT, 0, 0, 8, 2, main)


def hat_tall(c, main, band):
    """Tall chef-style hat."""
    c.fill(HAT_TOP, main)
    for region in (HAT_FRONT, HAT_RIGHT, HAT_LEFT, HAT_BACK):
        c.rect(region, 0, 0, region[2] - region[0], 5, main)
    for region in (HAT_FRONT, HAT_RIGHT, HAT_LEFT, HAT_BACK):
        c.rect(region, 0, 4, region[2] - region[0], 1, band)


def apron(c, color, strap):
    c.rect(JACKET_FRONT, 1, 4, 6, 14, color)
    c.rect(JACKET_FRONT, 1, 4, 6, 1, strap)
    c.rect(JACKET_FRONT, 3, 9, 2, 3, shade(color, 0.85))  # pocket


def build_farmer():
    c = Canvas()
    coat(c, rgb("#3f7a3a"), rgb("#2d5a2a"), rgb("#4a3b2a"))
    c.rect(JACKET_FRONT, 0, 2, 2, 12, rgb("#2d5a2a"))   # overall straps
    c.rect(JACKET_FRONT, 6, 2, 2, 12, rgb("#2d5a2a"))
    # straw hat
    c.fill(HAT_TOP, rgb("#d9b44a"))
    for region in (HAT_FRONT, HAT_RIGHT, HAT_LEFT, HAT_BACK):
        c.rect(region, 0, 0, region[2] - region[0], 4, rgb("#d9b44a"))
        c.rect(region, 0, 3, region[2] - region[0], 1, rgb("#b08a2e"))
    return c


def build_blacksmith():
    c = Canvas()
    coat(c, rgb("#5a5a62"), rgb("#3a3a40"), rgb("#2c2c30"))
    apron(c, rgb("#7a4a26"), rgb("#4a2c14"))
    hat_hair(c, rgb("#2a1e16"))
    return c


def build_chef():
    c = Canvas()
    coat(c, rgb("#f2f2ee"), rgb("#c9c9c4"), rgb("#3a3a46"))
    c.rect(JACKET_FRONT, 3, 3, 1, 8, rgb("#c9c9c4"))   # coat buttons line
    c.rect(JACKET_FRONT, 4, 3, 1, 8, rgb("#c9c9c4"))
    for y in (4, 7, 10):
        c.px(JACKET_FRONT, 2, y, rgb("#8a8a86"))
        c.px(JACKET_FRONT, 5, y, rgb("#8a8a86"))
    hat_tall(c, rgb("#fafaf6"), rgb("#d6d6d0"))
    return c


def build_barista():
    c = Canvas()
    coat(c, rgb("#6b4a2e"), rgb("#4a3220"), rgb("#2b2b33"))
    apron(c, rgb("#2f2f33"), rgb("#18181a"))
    c.rect(JACKET_FRONT, 2, 6, 4, 3, rgb("#c9a56a"))   # little cup badge
    hat_cap(c, rgb("#2f2f33"), rgb("#18181a"))
    return c


def build_historian():
    c = Canvas()
    coat(c, rgb("#7a5a3a"), rgb("#4a3420"), rgb("#4a4034"))
    c.rect(JACKET_FRONT, 3, 0, 2, 6, rgb("#a22b2b"))   # tie
    c.rect(JACKET_FRONT, 3, 6, 2, 1, rgb("#7d1f1f"))
    # round glasses over the eyes
    c.rect(HEAD_FRONT, 1, 4, 3, 2, rgb("#1b1b1b"))
    c.rect(HEAD_FRONT, 4, 4, 3, 2, rgb("#1b1b1b"))
    c.rect(HEAD_FRONT, 2, 4, 1, 1, rgb("#bfe3f2"))
    c.rect(HEAD_FRONT, 5, 4, 1, 1, rgb("#bfe3f2"))
    hat_hair(c, rgb("#9a9a9a"))   # grey hair
    return c


def build_mechanic():
    c = Canvas()
    coat(c, rgb("#2f5fa8"), rgb("#1f3f78"), rgb("#2f5fa8"))
    c.rect(JACKET_FRONT, 2, 6, 4, 4, rgb("#e8d37a"))   # patch
    c.px(JACKET_FRONT, 3, 7, rgb("#2f5fa8"))
    c.rect(JACKET_FRONT, 0, 14, 8, 1, rgb("#1f3f78"))
    c.rect(ARMS_CROSSED, 0, 0, 20, 1, rgb("#7a7a7a"))  # oil smear
    hat_cap(c, rgb("#c0392b"), rgb("#8a281e"))
    return c


def build_pilot():
    c = Canvas()
    coat(c, rgb("#7b6a4a"), rgb("#4f432b"), rgb("#3b3b3b"))
    c.rect(JACKET_FRONT, 0, 0, 8, 2, rgb("#d8d0b8"))   # scarf
    c.rect(JACKET_FRONT, 5, 2, 2, 5, rgb("#d8d0b8"))
    c.rect(JACKET_FRONT, 1, 8, 3, 3, rgb("#d8b34a"))   # wings badge
    # aviator goggles over the eyes
    c.rect(HEAD_FRONT, 0, 3, 8, 1, rgb("#3a2a1a"))
    c.rect(HEAD_FRONT, 1, 4, 3, 2, rgb("#9ad3e8"))
    c.rect(HEAD_FRONT, 4, 4, 3, 2, rgb("#9ad3e8"))
    hat_cap(c, rgb("#5a4a30"), rgb("#3a2e1c"))
    return c


def build_miner():
    c = Canvas()
    coat(c, rgb("#d8892a"), rgb("#9a5f18"), rgb("#33353a"))
    c.stripes(JACKET_FRONT, rgb("#e8e8d8"), 5)         # reflective stripes
    hat_cap(c, rgb("#e8c53a"), rgb("#a88a1c"))
    c.rect(HAT_FRONT, 3, 0, 2, 2, rgb("#fff6b0"))      # headlamp
    return c


def build_builder():
    c = Canvas()
    coat(c, rgb("#c8742a"), rgb("#8a4f1a"), rgb("#4a5a78"))
    c.rect(JACKET_FRONT, 0, 2, 8, 1, rgb("#e8e8d8"))
    c.rect(JACKET_FRONT, 0, 12, 8, 1, rgb("#e8e8d8"))
    hat_cap(c, rgb("#f0e040"), rgb("#b0a420"))
    return c


def build_soda_vendor():
    c = Canvas()
    coat(c, rgb("#c0392b"), rgb("#8a2a1f"), rgb("#2a2a30"))
    c.rect(JACKET_FRONT, 1, 5, 6, 2, rgb("#f2f2ee"))   # white wave stripe
    c.rect(JACKET_FRONT, 2, 7, 4, 1, rgb("#f2f2ee"))
    hat_cap(c, rgb("#f2f2ee"), rgb("#c0392b"))
    return c


def build_raider():
    """Openly armed raider: tactical helmet, balaclava, goggles, armoured vest, pouches, boots."""
    c = Canvas()
    dark = rgb("#1d1f22")
    olive = rgb("#3b4232")
    coat(c, olive, dark, rgb("#26282b"))
    # plate-carrier vest over the coat
    c.rect(JACKET_FRONT, 0, 1, 8, 10, rgb("#15171a"))
    c.rect(JACKET_FRONT, 1, 2, 6, 8, rgb("#22252a"))
    # magazine pouches
    for x in (1, 3, 5):
        c.rect(JACKET_FRONT, x, 8, 2, 3, rgb("#34393f"))
        c.rect(JACKET_FRONT, x, 8, 2, 1, rgb("#5a606a"))
    # chest light + belt with buckle
    c.px(JACKET_FRONT, 1, 3, rgb("#d33a2c"))
    c.rect(JACKET_FRONT, 0, 12, 8, 1, rgb("#0c0d0f"))
    c.rect(JACKET_FRONT, 3, 12, 2, 1, rgb("#b8b8b8"))
    # armour plate on the back and shoulder pads on the sleeves
    c.rect(JACKET_BACK, 1, 2, 6, 9, rgb("#22252a"))
    c.rect(ARMS_CROSSED, 0, 0, 20, 2, rgb("#15171a"))
    # knee pads and boots
    c.rect(LEG_FRONT, 0, 5, 4, 2, rgb("#15171a"))
    c.rect(LEG_FRONT, 0, 9, 4, 3, rgb("#0c0d0f"))
    c.rect((8, 26, 12, 38), 0, 9, 4, 3, rgb("#0c0d0f"))
    # balaclava over the lower face and goggles over the eyes
    c.rect(HEAD_FRONT, 0, 6, 8, 4, dark)
    c.rect(HEAD_FRONT, 0, 3, 8, 3, rgb("#0c0d0f"))
    c.rect(HEAD_FRONT, 1, 4, 2, 2, rgb("#d33a2c"))     # glowing lenses
    c.rect(HEAD_FRONT, 5, 4, 2, 2, rgb("#d33a2c"))
    # helmet: covers the top and sides, with a chin strap and a light on the front
    c.fill(HAT_TOP, rgb("#2a2d31"))
    for region in (HAT_FRONT, HAT_RIGHT, HAT_LEFT, HAT_BACK):
        c.rect(region, 0, 0, region[2] - region[0], 5, rgb("#2a2d31"))
        c.rect(region, 0, 4, region[2] - region[0], 1, rgb("#101214"))
    c.rect(HAT_FRONT, 3, 1, 2, 2, rgb("#e8e0a8"))
    for region in (HAT_RIGHT, HAT_LEFT):
        c.rect(region, 3, 5, 2, 5, rgb("#101214"))
    return c


def build_reveal_robber():
    """Shown after a disguised robber strikes: black eye mask and red bandana."""
    c = Canvas()
    # Rows 0-4 of the face can be hidden under a hat, so the mask sits on rows 5 and down.
    c.rect(HEAD_FRONT, 0, 5, 8, 2, rgb("#0c0c0c"))
    c.rect(HEAD_FRONT, 1, 5, 2, 1, rgb("#f2f2f2"))
    c.rect(HEAD_FRONT, 5, 5, 2, 1, rgb("#f2f2f2"))
    c.rect(HEAD_FRONT, 0, 7, 8, 3, rgb("#7a1f1f"))
    return c


def build_reveal_scammer():
    """Shown after a scammer strikes: dark sunglasses and a gold chain."""
    c = Canvas()
    c.rect(HEAD_FRONT, 0, 5, 8, 2, rgb("#0c0c0c"))
    c.rect(HEAD_FRONT, 1, 5, 2, 1, rgb("#3a3a50"))
    c.rect(HEAD_FRONT, 5, 5, 2, 1, rgb("#3a3a50"))
    c.rect(JACKET_FRONT, 1, 1, 6, 1, rgb("#e0b030"))
    return c


BUILDERS = {
    "farmer": build_farmer,
    "blacksmith": build_blacksmith,
    "chef": build_chef,
    "barista": build_barista,
    "historian": build_historian,
    "mechanic": build_mechanic,
    "pilot": build_pilot,
    "miner": build_miner,
    "builder": build_builder,
    "soda_vendor": build_soda_vendor,
    "raider": build_raider,
}


def main():
    for name, fn in BUILDERS.items():
        fn().save(name)
    parent = os.path.join(OUT, "..")
    for name, fn in (("reveal_robber", build_reveal_robber), ("reveal_scammer", build_reveal_scammer)):
        fn().img.save(os.path.join(parent, name + ".png"))
    print("wrote", len(BUILDERS), "job textures and 2 reveal overlays to", os.path.abspath(OUT))


if __name__ == "__main__":
    sys.exit(main())
