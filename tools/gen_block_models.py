#!/usr/bin/env python3
"""
Detailed block and item models (multi-element, like Blockbench models) with painted 64x64 materials.
Writes textures/block/mat_*.png, textures/item/*.png, models/block/*.json, models/item/*.json,
blockstates/*.json and tools/preview/models.json (the same data, for the web preview).
"""
import json, math, os, random
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src/main/resources/assets/tycoonrogue")
TB = os.path.join(RES, "textures/block")
TI = os.path.join(RES, "textures/item")
PREV = os.path.join(ROOT, "tools/preview")
for d in (TB, TI, PREV, os.path.join(RES, "models/block"), os.path.join(RES, "models/item"), os.path.join(RES, "blockstates")):
    os.makedirs(d, exist_ok=True)
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
rnd = random.Random(7)
S = 64


def clamp(v):
    return max(0, min(255, int(v)))

def mul(c, f):
    return tuple(clamp(v * f) for v in c[:3])

def new(w=S, h=S, base=(0, 0, 0)):
    im = Image.new("RGBA", (w, h), base + (255,))
    return im, im.load()

def noise(px, w, h, amt, base_fn):
    for y in range(h):
        for x in range(w):
            c = base_fn(x, y)
            n = rnd.randint(-amt, amt)
            px[x, y] = tuple(clamp(v + n) for v in c) + (255,)


def planks(name, base, seam=(0, 0, 0), plank_h=16, grain=0.10, polish=0.0):
    im, px = new()
    def f(x, y):
        row = y // plank_h
        shift = (row * 23) % S
        g = math.sin((x + shift) * 0.35 + math.sin(y * 0.9) * 1.6) * grain
        tone = 1.0 + g + (row % 2) * 0.05
        if y % plank_h == 0:
            return mul(seam or base, 0.55)
        if (x + shift) % 32 == 0:
            return mul(base, 0.6)
        if y % plank_h == 1 and polish:
            return mul(base, tone * (1 + polish))
        return mul(base, tone)
    noise(px, S, S, 4, f)
    im.save(os.path.join(TB, "mat_%s.png" % name))

def metal(name, base, rivets=True, brushed=True):
    im, px = new()
    def f(x, y):
        t = 1.0 + (math.sin(y * 1.7) * 0.04 if brushed else 0)
        return mul(base, t)
    noise(px, S, S, 3, f)
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, S - 1, S - 1], outline=mul(base, 1.3) + (255,))
    d.rectangle([1, 1, S - 2, S - 2], outline=mul(base, 0.6) + (255,))
    if rivets:
        for (x, y) in ((4, 4), (S - 5, 4), (4, S - 5), (S - 5, S - 5)):
            d.ellipse([x - 1, y - 1, x + 1, y + 1], fill=mul(base, 1.5) + (255,))
    im.save(os.path.join(TB, "mat_%s.png" % name))

def brass(name="brass"):
    im, px = new()
    def f(x, y):
        t = 1.0 + 0.18 * math.sin((x + y) * 0.35) + 0.08 * math.sin(y * 0.8)
        return mul((226, 178, 64), t)
    noise(px, S, S, 4, f)
    ImageDraw.Draw(im).rectangle([0, 0, S - 1, S - 1], outline=(150, 105, 30, 255))
    im.save(os.path.join(TB, "mat_%s.png" % name))

def felt(name, base):
    im, px = new()
    noise(px, S, S, 7, lambda x, y: mul(base, 1.0 + 0.08 * math.sin(x * 2.1) * math.sin(y * 2.3)))
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, S - 1, S - 1], outline=mul(base, 0.5) + (255,))
    im.save(os.path.join(TB, "mat_%s.png" % name))

def cloth(name):
    im, px = new()
    def f(x, y):
        stripe = (x // 8) % 2
        base = (206, 54, 52) if stripe == 0 else (246, 240, 226)
        return mul(base, 1.0 + 0.05 * math.sin(y * 1.5))
    noise(px, S, S, 4, f)
    im.save(os.path.join(TB, "mat_%s.png" % name))

def crate(name):
    im, px = new()
    def f(x, y):
        edge = x < 5 or x > S - 6 or y < 5 or y > S - 6
        diag = abs((x - y)) < 4 or abs((x + y) - S) < 4
        base = (172, 124, 70)
        if edge or diag:
            return mul(base, 1.1 + 0.05 * math.sin(y))
        return mul(base, 0.85 + 0.08 * math.sin(x * 0.8))
    noise(px, S, S, 4, f)
    ImageDraw.Draw(im).rectangle([0, 0, S - 1, S - 1], outline=(70, 46, 24, 255))
    im.save(os.path.join(TB, "mat_%s.png" % name))

def jar(name, liquid):
    im, px = new()
    def f(x, y):
        edge = x < 8 or x > S - 9 or y < 6 or y > S - 7
        if edge:
            return (190, 220, 224)
        return mul(liquid, 0.8 + 0.4 * (y / S) + 0.1 * math.sin(x * 0.25))
    noise(px, S, S, 4, f)
    d = ImageDraw.Draw(im)
    d.rectangle([14, 12, 20, 40], fill=(255, 255, 255, 120))
    d.rectangle([0, 0, S - 1, 4], fill=(120, 84, 48, 255))
    im.save(os.path.join(TB, "mat_%s.png" % name))

def paper(name):
    im, px = new()
    noise(px, S, S, 3, lambda x, y: (240, 236, 222) if y % 6 else (170, 190, 220))
    im.save(os.path.join(TB, "mat_%s.png" % name))

def leather(name, base):
    im, px = new()
    noise(px, S, S, 9, lambda x, y: mul(base, 1.0 + 0.1 * math.sin(x * 0.6) * math.cos(y * 0.7)))
    d = ImageDraw.Draw(im)
    d.rectangle([2, 2, S - 3, S - 3], outline=mul(base, 1.4) + (255,))
    im.save(os.path.join(TB, "mat_%s.png" % name))

def glass(name, tint=(170, 215, 235)):
    im, px = new()
    noise(px, S, S, 3, lambda x, y: mul(tint, 0.9 + 0.2 * (1 - y / S)))
    d = ImageDraw.Draw(im)
    d.line([8, 6, 8, 40], fill=(255, 255, 255, 255), width=2)
    im.save(os.path.join(TB, "mat_%s.png" % name))

def screen():
    im = Image.new("RGBA", (S, S), (10, 28, 24, 255))
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, S - 1, S - 1], outline=(70, 90, 100, 255))
    d.rectangle([2, 2, S - 3, 12], fill=(24, 120, 90, 255))
    f = ImageFont.truetype(FONT, 9)
    d.text((5, 3), "TYCOON", font=f, fill=(220, 255, 235, 255))
    # bar chart
    for i, h in enumerate((10, 16, 13, 24, 30, 36)):
        x = 6 + i * 9
        d.rectangle([x, 56 - h, x + 5, 56], fill=(60 + i * 25, 230 - i * 10, 120, 255))
    d.line([5, 57, 58, 57], fill=(160, 220, 190, 255))
    d.text((6, 16), "$ 128", font=ImageFont.truetype(FONT, 11), fill=(255, 224, 120, 255))
    for y in range(0, S, 3):
        d.line([2, y, S - 3, y], fill=(0, 0, 0, 40))
    im.save(os.path.join(TB, "mat_screen.png"))

def sign():
    im, px = new()
    noise(px, S, S, 4, lambda x, y: mul((98, 62, 34), 1.0 + 0.1 * math.sin(y * 1.2 + math.sin(x * 0.2) * 2)))
    d = ImageDraw.Draw(im)
    d.rectangle([1, 1, S - 2, S - 2], outline=(226, 178, 64, 255), width=2)
    f = ImageFont.truetype(FONT, 17)
    d.text((6, 20), "SHOP", font=f, fill=(246, 214, 110, 255))
    d.text((8, 42), "OPEN", font=ImageFont.truetype(FONT, 10), fill=(120, 230, 150, 255))
    im.save(os.path.join(TB, "mat_sign.png"))

def keys():
    im, px = new()
    noise(px, S, S, 3, lambda x, y: (46, 48, 56))
    d = ImageDraw.Draw(im)
    for cy in (6, 22, 38, 54):
        for cx in (8, 24, 40):
            d.rectangle([cx - 5, cy - 5, cx + 5, cy + 5], fill=(214, 216, 222, 255), outline=(120, 124, 132, 255))
    im.save(os.path.join(TB, "mat_keys.png"))

def coins():
    im, px = new()
    def f(x, y):
        return mul((236, 190, 70), 1.0 + (0.25 if y % 8 < 2 else -0.1 * (y % 8 == 6)))
    noise(px, S, S, 4, f)
    im.save(os.path.join(TB, "mat_coins.png"))

def holo():
    im = Image.new("RGBA", (S, S), (60, 220, 255, 255))
    px = im.load()
    for y in range(S):
        for x in range(S):
            t = 0.7 + 0.3 * math.sin((x + y) * 0.4)
            px[x, y] = (clamp(70 * t), clamp(220 * t), clamp(255 * t), 255)
    d = ImageDraw.Draw(im)
    for y in range(0, S, 4):
        d.line([0, y, S, y], fill=(200, 255, 255, 90))
    im.save(os.path.join(TB, "mat_holo.png"))

def phone_screen():
    w, h = 48, 112
    im = Image.new("RGBA", (w, h), (18, 24, 44, 255))
    d = ImageDraw.Draw(im)
    for y in range(h):
        d.line([0, y, w, y], fill=(24 + y // 6, 40 + y // 5, 90 + y // 3, 255))
    f = ImageFont.truetype(FONT, 14)
    d.text((8, 8), "12:30", font=f, fill=(255, 255, 255, 255))
    for i, col in enumerate(((235, 90, 80), (90, 190, 255), (255, 200, 70))):
        y = 34 + i * 22
        d.rounded_rectangle([3, y, w - 4, y + 18], radius=3, fill=(250, 250, 252, 235))
        d.rectangle([5, y + 3, 11, y + 9], fill=col + (255,))
        d.line([14, y + 5, w - 8, y + 5], fill=(60, 60, 70, 255), width=2)
        d.line([14, y + 11, w - 14, y + 11], fill=(150, 150, 160, 255), width=1)
    d.rounded_rectangle([14, h - 8, w - 15, h - 5], radius=1, fill=(255, 255, 255, 200))
    im.save(os.path.join(TI, "phone_screen.png"))
    # plain body material
    b = Image.new("RGBA", (16, 16), (26, 30, 40, 255))
    ImageDraw.Draw(b).rectangle([0, 0, 15, 15], outline=(70, 78, 96, 255))
    b.save(os.path.join(TI, "phone_body.png"))


def make_textures():
    planks("wood_dark", (96, 58, 32), plank_h=16)
    planks("wood_light", (190, 140, 82), plank_h=32, grain=0.07, polish=0.1)
    metal("steel", (70, 76, 90))
    brass()
    felt("felt", (30, 112, 70))
    cloth("awning")
    crate("crate")
    jar("jar_red", (210, 60, 60)); jar("jar_green", (70, 190, 90)); jar("jar_blue", (70, 120, 220)); jar("jar_gold", (236, 190, 70))
    paper("paper")
    leather("leather_red", (140, 40, 44))
    glass("glass")
    screen(); sign(); keys(); coins(); holo(); phone_screen()


# ---------------- model helpers ----------------
def el(frm, to, tex, faces=None, rot=None, only=None, **uv):
    e = {"from": list(frm), "to": list(to), "faces": {}}
    if rot:
        e["rotation"] = {"origin": rot[0], "axis": rot[1], "angle": rot[2]}
    names = only or ["north", "south", "east", "west", "up", "down"]
    for n in names:
        face = {"texture": "#" + tex}
        if n in uv:
            face["uv"] = uv[n]
        e["faces"][n] = face
    if faces:
        for n, f in faces.items():
            e["faces"][n] = f
    return e

def cube(frm, to, tex, rot=None):
    return el(frm, to, tex, rot=rot)

TEXTURES_BLOCK = ["wood_dark", "wood_light", "steel", "brass", "felt", "awning", "crate", "jar_red", "jar_green", "jar_blue",
                  "jar_gold", "paper", "leather_red", "glass", "screen", "sign", "keys", "coins"]

def tex_map(names):
    t = {n: "tycoonrogue:block/mat_" + n for n in names}
    t["particle"] = "tycoonrogue:block/mat_wood_dark"
    return t

DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, -1.5, 0], "scale": [0.36, 0.36, 0.36]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.24, 0.24, 0.24]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, -1, 0], "scale": [0.34, 0.34, 0.34]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.28, 0.28, 0.28]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 1, 0], "scale": [0.34, 0.34, 0.34]},
}


def counter_model():
    E = []
    E.append(cube([0, 0, 0], [16, 2, 16], "steel"))  # plinth
    E.append(cube([0.5, 2, 0.5], [15.5, 12, 15.5], "wood_dark"))  # cabinet
    for x0, x1 in ((1.5, 5), (5.75, 10.25), (11, 14.5)):  # three drawers / doors on the front
        E.append(cube([x0, 3, -0.5], [x1, 11, 0.5], "wood_light"))
        mid = (x0 + x1) / 2
        E.append(cube([mid - 1.2, 6.6, -1.2], [mid + 1.2, 7.4, -0.5], "brass"))
    E.append(cube([-0.5, 12, -0.5], [16.5, 13.5, 16.5], "wood_light"))  # counter top
    E.append(cube([1, 13.5, 1], [15, 13.7, 15], "felt"))  # green leather inlay
    E.append(cube([-0.5, 11.6, -0.7], [16.5, 12.3, -0.4], "brass"))  # brass front trim
    # cash register
    E.append(cube([3, 13.7, 7], [13, 17.5, 14], "steel"))
    E.append(cube([3.5, 14, 5.4], [12.5, 14.8, 7], "steel"))
    E.append(el([4, 14.8, 5.4], [12, 15.2, 7], "keys", up=[0, 0, 16, 16], only=["up", "north"]))
    E.append(el([4, 17.5, 9.5], [12, 23.5, 10.7], "steel", rot=([8, 17.5, 10], "x", -22.5)))
    E.append(el([4.4, 17.9, 9.0], [11.6, 23.1, 9.55], "screen", rot=([8, 17.5, 10], "x", -22.5), only=["north"], north=[0, 0, 16, 16]))
    E.append(cube([5, 17.5, 11], [11, 19, 13], "steel"))
    # cash drawer front
    E.append(cube([4, 13.7, 5], [12, 14.2, 5.4], "brass"))
    # service bell
    E.append(cube([12.3, 13.7, 2], [14.7, 14.7, 4.4], "brass"))
    E.append(cube([12.9, 14.7, 2.6], [14.1, 15.4, 3.8], "brass"))
    E.append(cube([13.3, 15.4, 3.0], [13.7, 15.9, 3.4], "leather_red"))
    # ledger
    E.append(cube([1.5, 13.7, 2], [6.5, 14.4, 6], "leather_red", rot=([4, 13.7, 4], "y", 22.5)))
    E.append(cube([1.8, 14.4, 2.3], [6.2, 14.7, 5.7], "paper", rot=([4, 13.7, 4], "y", 22.5)))
    # pen cup + pen
    E.append(cube([9.2, 13.7, 2.4], [10.8, 15.4, 4], "glass"))
    E.append(cube([9.8, 15.4, 2.9], [10.2, 17.2, 3.3], "brass", rot=([10, 15.4, 3.1], "z", 12)))
    E.append(cube([6.8, 15.4, 8.5], [8.2, 16.6, 9.5], "coins"))
    return {"credit": "generated by tools/gen_block_models.py", "texture_size": [64, 64],
            "textures": tex_map(TEXTURES_BLOCK), "elements": E, "display": DISPLAY}


def desk_model():
    E = []
    E.append(cube([0, 0, 0], [16, 2, 16], "steel"))
    E.append(cube([0.5, 2, 0.5], [15.5, 10, 15.5], "wood_dark"))
    E.append(cube([1, 3, -0.5], [15, 9, 0.5], "crate"))
    E.append(cube([-0.5, 10, -0.5], [16.5, 11.5, 16.5], "wood_light"))
    E.append(cube([-0.5, 9.7, -0.7], [16.5, 10.3, -0.4], "brass"))
    # back shelf unit
    E.append(cube([0, 11.5, 13], [1.6, 24, 15.5], "wood_dark"))
    E.append(cube([14.4, 11.5, 13], [16, 24, 15.5], "wood_dark"))
    E.append(cube([1.6, 11.5, 14.6], [14.4, 24, 15.6], "wood_dark"))
    E.append(cube([0, 15.5, 11], [16, 16.7, 15.5], "wood_light"))
    E.append(cube([0, 20.5, 11], [16, 21.7, 15.5], "wood_light"))
    jars = ["jar_red", "jar_green", "jar_blue", "jar_gold"]
    for row, base in ((0, 16.7), (1, 21.7)):
        for i in range(4):
            x = 2.2 + i * 3.4
            E.append(cube([x, base, 12], [x + 2.6, base + 3.2 + (i % 2) * 0.6, 14.6], jars[(i + row * 2) % 4]))
            E.append(cube([x + 0.7, base + 3.2 + (i % 2) * 0.6, 12.7], [x + 1.9, base + 3.9 + (i % 2) * 0.6, 13.9], "wood_light"))
    # awning
    E.append(el([-1, 24.2, 3], [17, 25.2, 16], "awning", rot=([8, 24.2, 16], "x", 20)))
    for i in range(6):
        x = -1 + i * 3
        E.append(cube([x, 22.9 - 0.0, 1.9], [x + 3, 24.1, 2.4], "awning", rot=([8, 24, 2], "x", 20)))
    E.append(cube([-0.7, 20.5, 1.6], [0.1, 24.3, 2.4], "brass"))
    E.append(cube([15.9, 20.5, 1.6], [16.7, 24.3, 2.4], "brass"))
    # hanging sign on the front of the counter top
    E.append(el([4.5, 11.5, 4], [11.5, 16, 4.8], "wood_dark", only=["east", "west", "up", "down", "south"]))
    E.append(el([4.5, 11.5, 3.9], [11.5, 16, 4.2], "sign", only=["north"], north=[0, 0, 16, 16]))
    # coin stacks + crate with apples
    for dx, h in ((11.5, 1.5), (13.2, 2.4), (14.9, 1.0)):
        E.append(cube([dx - 0.6, 11.5, 2.4], [dx + 0.6, 11.5 + h, 3.6], "coins"))
    E.append(cube([1.6, 11.5, 2.6], [5.4, 14.4, 6.4], "crate"))
    E.append(cube([2.0, 14.4, 3.0], [5.0, 15.2, 6.0], "leather_red"))
    return {"credit": "generated by tools/gen_block_models.py", "texture_size": [64, 64],
            "textures": tex_map(TEXTURES_BLOCK), "elements": E, "display": DISPLAY}


def phone_model():
    E = []
    t = {"body": "tycoonrogue:item/phone_body", "scr": "tycoonrogue:item/phone_screen", "particle": "tycoonrogue:item/phone_body",
         "lens": "tycoonrogue:block/mat_glass", "case": "tycoonrogue:block/mat_steel"}
    E.append(cube([4.6, 0.8, 7.2], [11.4, 15.2, 8.6], "body"))
    E.append(el([5.0, 1.4, 7.0], [11.0, 14.6, 7.3], "scr", only=["north"], north=[0, 0, 16, 16]))
    E.append(cube([4.6, 14.4, 7.2], [11.4, 15.2, 8.6], "case"))
    E.append(cube([5.4, 12.6, 8.6], [8.2, 14.6, 9.2], "case"))
    E.append(cube([5.9, 13.1, 9.2], [6.9, 14.1, 9.4], "lens"))
    E.append(cube([7.0, 13.1, 9.2], [7.8, 13.9, 9.4], "lens"))
    # tiny stand so it can be dropped upright
    E.append(cube([5.5, 0, 7.6], [10.5, 0.8, 8.4], "case"))
    disp = {"gui": {"rotation": [18, -28, 0], "translation": [0, -0.5, 0], "scale": [0.9, 0.9, 0.9]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.55, 0.55, 0.55]},
            "fixed": {"rotation": [0, 180, 0], "translation": [0, -1, 0], "scale": [0.9, 0.9, 0.9]},
            "thirdperson_righthand": {"rotation": [0, -90, 40], "translation": [0, 3, 1], "scale": [0.6, 0.6, 0.6]},
            "firstperson_righthand": {"rotation": [0, -90, 20], "translation": [1.5, 2, 2], "scale": [0.7, 0.7, 0.7]}}
    return {"credit": "generated by tools/gen_block_models.py", "textures": t, "elements": E, "display": disp}


def holo_model():
    E = []
    t = {"steel": "tycoonrogue:block/mat_steel", "brass": "tycoonrogue:block/mat_brass", "holo": "tycoonrogue:block/mat_holo",
         "particle": "tycoonrogue:block/mat_steel", "glass": "tycoonrogue:block/mat_glass"}
    E.append(cube([3.5, 0, 3.5], [12.5, 1.6, 12.5], "steel"))
    E.append(cube([4.8, 1.6, 4.8], [11.2, 2.6, 11.2], "brass"))
    E.append(cube([6.3, 2.6, 6.3], [9.7, 3.6, 9.7], "steel"))
    E.append(cube([7.3, 3.6, 7.3], [8.7, 4.0, 8.7], "holo"))
    E.append(cube([7.6, 4.0, 7.6], [8.4, 8.2, 8.4], "holo"))  # beam
    E.append(cube([6.0, 8.4, 6.0], [10.0, 12.4, 10.0], "holo", rot=([8, 10.4, 8], "y", 45)))
    E.append(cube([6.0, 8.4, 6.0], [10.0, 12.4, 10.0], "holo", rot=([8, 10.4, 8], "x", 45)))
    E.append(cube([6.5, 8.9, 6.5], [9.5, 11.9, 9.5], "glass", rot=([8, 10.4, 8], "z", 45)))
    disp = {"gui": {"rotation": [28, 38, 0], "translation": [0, -1, 0], "scale": [1.0, 1.0, 1.0]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]},
            "fixed": {"rotation": [0, 180, 0], "translation": [0, -1, 0], "scale": [1, 1, 1]},
            "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.6, 0.6, 0.6]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 3, 2], "scale": [0.7, 0.7, 0.7]}}
    return {"credit": "generated by tools/gen_block_models.py", "textures": t, "elements": E, "display": disp}


def write(path, obj):
    with open(os.path.join(RES, path), "w") as f:
        json.dump(obj, f, indent=1)

def main():
    make_textures()
    models = {"tycoon_counter": counter_model(), "supply_desk": desk_model(), "phone": phone_model(), "hologram_guide": holo_model()}
    write("models/block/tycoon_counter.json", models["tycoon_counter"])
    write("models/block/supply_desk.json", models["supply_desk"])
    write("models/item/phone.json", models["phone"])
    write("models/item/hologram_guide.json", models["hologram_guide"])
    # facing variants (the front of the model is north)
    for name in ("tycoon_counter", "supply_desk"):
        write("blockstates/%s.json" % name, {"variants": {
            "facing=north": {"model": "tycoonrogue:block/" + name},
            "facing=east": {"model": "tycoonrogue:block/" + name, "y": 90},
            "facing=south": {"model": "tycoonrogue:block/" + name, "y": 180},
            "facing=west": {"model": "tycoonrogue:block/" + name, "y": 270}}})
    # preview data with the textures inlined
    import base64
    tex = {}
    for n in TEXTURES_BLOCK:
        tex["tycoonrogue:block/mat_" + n] = base64.b64encode(open(os.path.join(TB, "mat_%s.png" % n), "rb").read()).decode()
    for n in ("phone_screen", "phone_body"):
        tex["tycoonrogue:item/" + n] = base64.b64encode(open(os.path.join(TI, n + ".png"), "rb").read()).decode()
    for n in ("steel", "brass", "holo", "glass"):
        tex["tycoonrogue:block/mat_" + n] = base64.b64encode(open(os.path.join(TB, "mat_%s.png" % n), "rb").read()).decode()
    json.dump({"models": models, "tex": tex}, open(os.path.join(PREV, "models.json"), "w"), separators=(",", ":"))
    print("models:", {k: len(v["elements"]) for k, v in models.items()})

if __name__ == "__main__":
    main()
