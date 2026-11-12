#!/usr/bin/env python3
"""
Builds the 3D customer model: one table of boxes (parts) is the single source of truth for
  * the Java model definition  (client/CustomerModelDef.java)
  * the texture atlas painting (textures/entity/customer/skin<N>/<job>.png)
  * a front/back preview sheet (tools/preview/customer_preview.png)

Part coordinates follow the humanoid convention: y points down, feet at y = 24.
"""
import os, random, math
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAVA = os.path.join(ROOT, "src/main/java/com/ysh/tycoonrogue/client/CustomerModelDef.java")
TEX = os.path.join(ROOT, "src/main/resources/assets/tycoonrogue/textures/entity/customer")
PREVIEW = os.path.join(ROOT, "tools/preview")

SKINS = [(255, 224, 196), (243, 200, 164), (222, 170, 128), (192, 136, 98),
         (152, 100, 68), (110, 72, 50), (236, 190, 170)]

# name, parent, color key, (w,h,d), (x,y,z) of box min, grow, pose (ox,oy,oz,rx,ry,rz), uvkey, mirror
P = []
def part(name, parent, ck, dims, org, grow=0.0, pose=(0, 0, 0, 0, 0, 0), uv=None, mirror=False):
    P.append(dict(name=name, parent=parent, ck=ck, dims=dims, org=org, grow=grow, pose=pose,
                  uv=uv or name, mirror=mirror))

# ---- core body ----
part("head", "", "head", (8, 8, 8), (-4, -8, -4))
part("body", "", "body", (8, 12, 4), (-4, 0, -2))
part("right_arm", "", "arm", (4, 12, 4), (-3, -2, -2), pose=(-5, 2, 0, 0, 0, 0))
part("left_arm", "", "arm", (4, 12, 4), (-1, -2, -2), pose=(5, 2, 0, 0, 0, 0), uv="right_arm", mirror=True)
part("right_leg", "", "leg", (4, 12, 4), (-2, 0, -2), pose=(-1.9, 12, 0, 0, 0, 0))
part("left_leg", "", "leg", (4, 12, 4), (-2, 0, -2), pose=(1.9, 12, 0, 0, 0, 0), uv="right_leg", mirror=True)

# ---- head details ----
part("nose", "head", "nose", (2, 2, 1), (-1, -4, -5))
part("ear_r", "head", "ear", (1, 3, 2), (-5, -4, -1), uv="ear")
part("ear_l", "head", "ear", (1, 3, 2), (4, -4, -1), uv="ear")
part("lids", "head", "lids", (6, 2, 1), (-3, -5, -5), 0.01)
# hair (grey texture, tinted at render time)
part("hair_cap", "head", "hair", (8, 3, 8), (-4, -8, -4), 0.45)
part("hair_fringe", "head", "hair", (8, 2, 1), (-4, -8, -4), 0.6)
part("hair_back", "head", "hair", (8, 8, 2), (-4, -8, 2), 0.45)
part("hair_side_l", "head", "hair", (1, 7, 3), (3, -6, 0), 0.5, uv="hair_side")
part("hair_side_r", "head", "hair", (1, 7, 3), (-4, -6, 0), 0.5, uv="hair_side")
part("hair_bun", "head", "hair", (4, 4, 4), (-2, -12, 0))
part("hair_spike_a", "head", "hair", (2, 4, 2), (-3, -11, -3), uv="hair_spike")
part("hair_spike_b", "head", "hair", (2, 4, 2), (1, -11, -3), uv="hair_spike")
part("hair_spike_c", "head", "hair", (2, 4, 2), (-1, -12, 0), uv="hair_spike")
part("hair_tail", "head", "hair", (2, 9, 2), (-1, 0, -1), pose=(0, -6.5, 4.5, 0.15, 0, 0))
HAIR = ["hair_cap", "hair_fringe", "hair_back", "hair_side_l", "hair_side_r", "hair_bun",
        "hair_spike_a", "hair_spike_b", "hair_spike_c", "hair_tail"]
HAIR_STYLES = [  # 0 bald, 1 short, 2 long, 3 bun, 4 spiky, 5 ponytail
    [], ["hair_cap", "hair_fringe"],
    ["hair_cap", "hair_fringe", "hair_back", "hair_side_l", "hair_side_r"],
    ["hair_cap", "hair_fringe", "hair_bun"],
    ["hair_cap", "hair_spike_a", "hair_spike_b", "hair_spike_c"],
    ["hair_cap", "hair_fringe", "hair_tail"]]
# hats
part("cap_dome", "head", "hat", (8, 3, 8), (-4, -11, -4), 0.55)
part("cap_visor", "head", "hat2", (8, 1, 4), (-4, -8, -8))
part("chef_band", "head", "hat", (8, 2, 8), (-4, -10, -4), 0.6)
part("chef_puff", "head", "hat", (8, 5, 8), (-4, -15, -4), 1.3)
part("straw_brim", "head", "hat2", (14, 1, 14), (-7, -8.5, -7))
part("straw_crown", "head", "hat", (8, 3, 8), (-4, -11, -4), 0.5)
part("hh_dome", "head", "hat", (8, 3, 8), (-4, -11, -4), 0.6)
part("hh_brim", "head", "hat2", (10, 1, 5), (-5, -8.5, -9))
part("hh_ridge", "head", "hat2", (2, 1, 10), (-1, -12, -5))
part("lamp", "head", "lamp", (3, 3, 2), (-1.5, -10.5, -6.6))
part("goggle_strap", "head", "strap", (8, 2, 8), (-4, -7, -4), 0.35)
part("lens_l", "head", "glass", (3, 2, 1), (0.5, -7, -5.4), uv="lens")
part("lens_r", "head", "glass", (3, 2, 1), (-3.5, -7, -5.4), uv="lens")
part("glasses", "head", "glasses", (8, 2, 1), (-4, -5, -5.2), 0.1)
part("helm_dome", "head", "hat", (8, 4, 8), (-4, -12, -4), 0.65)
part("helm_rim", "head", "hat2", (8, 1, 8), (-4, -8, -4), 0.7)
part("helm_nasal", "head", "hat2", (1, 4, 1), (-0.5, -8, -5.4), 0.1)
part("plume", "head", "plume", (2, 3, 8), (-1, -15, -4))
part("horn_l", "head", "bone", (1, 4, 1), (-0.5, -4, -0.5), pose=(4.8, -10, 0, 0, 0, -0.7), uv="horn")
part("horn_r", "head", "bone", (1, 4, 1), (-0.5, -4, -0.5), pose=(-4.8, -10, 0, 0, 0, 0.7), uv="horn")
# reveals (fixed look, shown by the customer's disguise state)
part("mask_low", "head", "mask", (8, 3, 8), (-4, -3, -4), 0.35)
part("mask_eyes", "head", "maskeyes", (8, 2, 8), (-4, -5, -4), 0.3)
part("beanie", "head", "mask", (8, 3, 8), (-4, -11, -4), 0.6)
part("shades", "head", "shades", (8, 2, 1), (-4, -5, -5.2), 0.15)
part("head_band", "head", "headset", (8, 1, 2), (-4, -9, -1), 0.55)
part("cup_l", "head", "headset", (2, 3, 3), (4.2, -5, -1.5), uv="cup")
part("cup_r", "head", "headset", (2, 3, 3), (-6.2, -5, -1.5), uv="cup")
part("mic_boom", "head", "headset", (1, 1, 5), (4.2, -2.5, -5.5))
part("mic_tip", "head", "mic", (3, 1, 1), (1.5, -2.5, -6.1))
REVEAL = {1: ["mask_low", "mask_eyes", "beanie"], 2: ["shades"],
          3: ["head_band", "cup_l", "cup_r", "mic_boom", "mic_tip"]}

# ---- body details ----
part("coat", "body", "coat", (8, 12, 4), (-4, 0, -2), 0.35)
part("apron", "body", "apron", (8, 10, 1), (-4, 2, -3), 0.15)
part("bib", "body", "apron", (6, 6, 1), (-3, 1, -2.9), 0.15)
part("strap_l", "body", "apron", (1, 2, 5), (1.5, -0.2, -2.5), 0.1, uv="strap")
part("strap_r", "body", "apron", (1, 2, 5), (-2.5, -0.2, -2.5), 0.1, uv="strap")
part("scarf", "body", "scarf", (8, 2, 4), (-4, -1, -2), 0.55)
part("scarf_tail", "body", "scarf", (3, 6, 1), (-3.5, 1, -3), 0.1)
part("belt", "body", "belt", (8, 2, 4), (-4, 9, -2), 0.4)
part("pouch_l", "body", "pouch", (2, 3, 2), (3.2, 9.5, -1), uv="pouch")
part("pouch_r", "body", "pouch", (2, 3, 2), (-5.2, 9.5, -1), uv="pouch")
part("buckle", "body", "gold", (2, 2, 1), (-1, 9, -3), 0.05)
part("backpack", "body", "pack", (6, 8, 3), (-3, 2, 2), 0.1)
part("badge", "body", "gold", (2, 2, 1), (1, 2.5, -3))
part("pad_r", "right_arm", "metal", (5, 2, 5), (-3.5, -3, -2.5), uv="pad")
part("pad_l", "left_arm", "metal", (5, 2, 5), (-1.5, -3, -2.5), uv="pad")

C = lambda *a: tuple(a)
DEFAULT_COLORS = dict(mask=C(28, 28, 32), shades=C(20, 20, 24), headset=C(36, 38, 46), mic=C(200, 40, 50),
                      lamp=C(255, 232, 120), glass=C(120, 205, 235), gold=C(235, 190, 70), bone=C(228, 220, 196),
                      metal=C(150, 156, 168), plume=C(190, 40, 40), belt=C(92, 60, 36), pouch=C(110, 76, 46),
                      pack=C(120, 84, 52), strap=C(60, 50, 44))

JOBS = {
    "farmer": dict(shirt=C(196, 74, 62), pattern="plaid", pants=C(86, 112, 160), boots=C(88, 62, 40), sleeve=12,
                   hat=C(230, 196, 100), hat2=C(212, 176, 84), apron=C(70, 104, 156), coat_detail="none",
                   parts=["straw_brim", "straw_crown", "bib", "strap_l", "strap_r"], hat_on=True),
    "blacksmith": dict(shirt=C(112, 96, 88), pants=C(58, 52, 50), boots=C(40, 34, 32), sleeve=5, glove=C(54, 46, 42),
                       apron=C(112, 70, 40), strap=C(40, 40, 46), glass=C(150, 215, 230), coat_detail="none",
                       parts=["goggle_strap", "lens_l", "lens_r", "apron", "belt", "pouch_l", "pouch_r", "buckle"],
                       hat_on=False),
    "chef": dict(shirt=C(244, 244, 246), pants=C(70, 74, 86), boots=C(36, 36, 40), sleeve=12, hat=C(250, 250, 252),
                 hat2=C(230, 230, 236), coat=C(246, 246, 248), coat_detail="double", scarf=C(210, 60, 60),
                 parts=["chef_band", "chef_puff", "coat", "scarf"], hat_on=True),
    "barista": dict(shirt=C(236, 230, 218), pants=C(52, 50, 58), boots=C(40, 34, 30), sleeve=12,
                    hat=C(110, 78, 56), hat2=C(90, 62, 44), apron=C(84, 58, 42), coat_detail="none",
                    parts=["cap_dome", "cap_visor", "apron"], hat_on=True),
    "historian": dict(shirt=C(232, 224, 208), pants=C(64, 58, 52), boots=C(52, 40, 32), sleeve=12,
                      coat=C(98, 64, 52), coat_detail="waistcoat", scarf=C(150, 40, 54),
                      parts=["glasses", "coat", "scarf", "scarf_tail"], hat_on=False),
    "mechanic": dict(shirt=C(60, 100, 172), pants=C(52, 88, 156), boots=C(40, 40, 46), sleeve=12, glove=C(110, 112, 120),
                     hat=C(200, 56, 52), hat2=C(170, 44, 42), coat=C(58, 98, 170), coat_detail="zip",
                     parts=["cap_dome", "cap_visor", "coat", "belt", "pouch_l", "pouch_r", "buckle"], hat_on=True),
    "pilot": dict(shirt=C(156, 126, 90), pants=C(150, 128, 92), boots=C(66, 44, 30), sleeve=12, glove=C(96, 62, 40),
                  hat=C(104, 68, 42), hat2=C(86, 56, 36), coat=C(120, 78, 46), coat_detail="fur", scarf=C(240, 240, 244),
                  strap=C(70, 54, 40), glass=C(150, 200, 235),
                  parts=["cap_dome", "cap_visor", "goggle_strap", "lens_l", "lens_r", "coat", "scarf", "scarf_tail"],
                  hat_on=True),
    "miner": dict(shirt=C(132, 112, 84), pants=C(86, 78, 66), boots=C(54, 44, 36), sleeve=12, glove=C(80, 70, 60),
                  hat=C(238, 196, 52), hat2=C(214, 174, 42), coat=C(240, 124, 36), coat_detail="stripes",
                  pack=C(110, 82, 56),
                  parts=["hh_dome", "hh_brim", "hh_ridge", "lamp", "coat", "backpack", "belt", "pouch_l", "pouch_r"],
                  hat_on=True),
    "builder": dict(shirt=C(212, 200, 176), pants=C(90, 100, 120), boots=C(60, 46, 34), sleeve=5, glove=C(190, 150, 70),
                    hat=C(246, 214, 40), hat2=C(222, 190, 30), coat=C(208, 232, 56), coat_detail="stripes",
                    parts=["hh_dome", "hh_brim", "hh_ridge", "coat", "belt", "pouch_l", "pouch_r", "buckle"],
                    hat_on=True),
    "soda_vendor": dict(shirt=C(250, 250, 250), pattern="stripes", pants=C(52, 56, 92), boots=C(230, 230, 232),
                        sleeve=5, hat=C(224, 52, 62), hat2=C(250, 250, 250), apron=C(224, 52, 62),
                        scarf=C(224, 52, 62), coat_detail="none",
                        parts=["cap_dome", "cap_visor", "apron", "scarf"], hat_on=True),
    "raider": dict(shirt=C(72, 66, 66), pants=C(54, 48, 44), boots=C(34, 30, 28), sleeve=12, glove=C(40, 36, 36),
                   hat=C(96, 98, 106), hat2=C(76, 78, 86), coat=C(70, 66, 74), coat_detail="armor",
                   metal=C(128, 130, 140), plume=C(160, 30, 30),
                   parts=["helm_dome", "helm_rim", "helm_nasal", "horn_l", "horn_r", "coat", "pad_r", "pad_l", "belt", "buckle"],
                   hat_on=True),
    "guard": dict(shirt=C(46, 70, 130), pants=C(40, 52, 92), boots=C(30, 30, 38), sleeve=12, glove=C(60, 62, 74),
                  hat=C(170, 176, 190), hat2=C(140, 146, 160), coat=C(52, 88, 160), coat_detail="tabard",
                  metal=C(172, 178, 192), plume=C(200, 48, 48),
                  parts=["helm_dome", "helm_rim", "helm_nasal", "plume", "coat", "pad_r", "pad_l", "badge", "belt", "buckle"],
                  hat_on=True),
}
JOB_ORDER = ["farmer", "blacksmith", "chef", "barista", "historian", "mechanic", "pilot", "miner", "builder",
             "soda_vendor", "raider", "guard"]


# ---------------- atlas layout ----------------
def region_size(dims):
    w, h, d = dims
    return 2 * (d + w), d + h

def layout():
    uvs = {}
    items = {}
    for p in P:
        items.setdefault(p["uv"], p["dims"])
    order = sorted(items, key=lambda k: -region_size(items[k])[1])
    W = 128
    x = y = shelf = 0
    for k in order:
        rw, rh = region_size(items[k])
        if x + rw > W:
            x, y, shelf = 0, y + shelf, 0
        uvs[k] = (x, y)
        x += rw
        shelf = max(shelf, rh)
    H = y + shelf
    return uvs, items, W, ((H + 15) // 16) * 16

UVS, UVDIMS, TW, TH = layout()

def faces(uvkey):
    w, h, d = UVDIMS[uvkey]
    u, v = UVS[uvkey]
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


# ---------------- painting ----------------
def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)

class Painter:
    def __init__(self, job, skin, seed):
        self.job = JOBS[job]
        self.cols = dict(DEFAULT_COLORS)
        self.cols.update({k: v for k, v in self.job.items() if isinstance(v, tuple)})
        self.skin = SKINS[skin]
        for key in ("coat", "hat", "hat2", "apron", "scarf"):
            self.cols.setdefault(key, self.job["shirt"])
        self.job.setdefault("coat_detail", "none")
        self.img = Image.new("RGBA", (TW, TH), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.img)
        self.rnd = random.Random(seed)

    def fill(self, r, c, noise=3):
        x, y, w, h = r
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                n = self.rnd.randint(-noise, noise)
                self.img.putpixel((xx, yy), tuple(max(0, min(255, v + n)) for v in c) + (255,))

    def px(self, x, y, c, a=255):
        if 0 <= x < TW and 0 <= y < TH:
            self.img.putpixel((x, y), tuple(c) + (a,))

    def box(self, uvkey, base, front=None, noise=3):
        f = faces(uvkey)
        tones = {"top": 1.12, "bottom": 0.72, "right": 0.88, "left": 0.88, "back": 0.9, "front": 1.0}
        for k, r in f.items():
            self.fill(r, shade(base, tones[k]), noise)
        # soft edge darkening on the sides/front to give the cuboid some depth
        for k in ("front", "back", "left", "right"):
            x, y, w, h = f[k]
            if h > 2:
                for xx in range(x, x + w):
                    self.px(xx, y + h - 1, shade(base, 0.8 * tones[k]))
        return f

    # ----- specific parts -----
    def head(self, hair):
        f = self.box("head", self.skin, noise=2)
        x, y, w, h = f["front"]
        iris, pupil, white = (62, 84, 144), (28, 34, 58), (250, 250, 250)
        # left eye (cols 1-2) and right eye (cols 5-6), whites on the outer side
        self.px(x + 1, y + 3, white); self.px(x + 2, y + 3, iris)
        self.px(x + 1, y + 4, iris); self.px(x + 2, y + 4, pupil)
        self.px(x + 5, y + 3, iris); self.px(x + 6, y + 3, white)
        self.px(x + 5, y + 4, pupil); self.px(x + 6, y + 4, iris)
        for ex in (1, 2, 5, 6):  # eyebrows
            self.px(x + ex, y + 2, hair)
        self.px(x + 3, y + 6, (170, 84, 84)); self.px(x + 4, y + 6, (170, 84, 84))
        blush = (min(255, self.skin[0] + 6), int(self.skin[1] * 0.87), int(self.skin[2] * 0.85))
        for cx in (1, 6):
            self.px(x + cx, y + 5, blush)
        # ears of the head box itself are plain skin; shadow under the chin
        bx, by, bw, bh = f["bottom"]
        self.fill(f["bottom"], shade(self.skin, 0.6), 2)

    def body(self):
        j = self.job
        shirt = j["shirt"]
        f = self.box("body", shirt)
        x, y, w, h = f["front"]
        pattern = j.get("pattern")
        if pattern == "plaid":
            for yy in range(h):
                for xx in range(w):
                    if yy % 4 == 1 or xx % 4 == 1:
                        self.px(x + xx, y + yy, shade(shirt, 0.78))
                    if yy % 4 == 1 and xx % 4 == 1:
                        self.px(x + xx, y + yy, shade(shirt, 0.6))
        if pattern == "stripes":
            for yy in range(h):
                if yy % 3 == 0:
                    for xx in range(w):
                        self.px(x + xx, y + yy, (224, 52, 62))
        for xx in (3, 4):  # collar notch showing skin
            self.px(x + xx, y, self.skin); self.px(x + xx, y + 1, self.skin) if xx in (3, 4) and False else None
        self.px(x + 3, y + 1, self.skin); self.px(x + 4, y + 1, self.skin)
        self.px(x + 2, y, shade(shirt, 1.25)); self.px(x + 5, y, shade(shirt, 1.25))
        for yy in (3, 5, 7):
            self.px(x + 3, y + yy, shade(shirt, 0.65))
        # belt and trousers shade
        belt = self.cols["belt"]
        for yy in (9, 10):
            for xx in range(w):
                self.px(x + xx, y + yy, belt)
        self.px(x + 3, y + 9, self.cols["gold"]); self.px(x + 4, y + 9, self.cols["gold"])
        self.px(x + 3, y + 10, self.cols["gold"]); self.px(x + 4, y + 10, self.cols["gold"])
        # lower body is trousers colour
        for yy in (11,):
            for xx in range(w):
                self.px(x + xx, y + yy, shade(j["pants"], 1.0))
        bx, by, bw, bh = f["back"]
        for xx in range(bw):
            self.px(bx + xx, by + 9, belt); self.px(bx + xx, by + 10, belt)
            self.px(bx + xx, by + 11, j["pants"])

    def arm(self):
        j = self.job
        shirt, sleeve = j["shirt"], j["sleeve"]
        glove = j.get("glove")
        f = self.box("right_arm", shirt)
        for k in ("front", "back", "left", "right"):
            x, y, w, h = f[k]
            for yy in range(h):
                if yy >= sleeve and yy < h - 2 or (yy >= h - 2 and not glove):
                    c = self.skin
                elif yy >= h - 2 and glove:
                    c = glove
                else:
                    c = shirt
                shd = {"front": 1.0, "back": 0.9, "left": 0.85, "right": 0.88}[k]
                for xx in range(w):
                    n = self.rnd.randint(-2, 2)
                    self.px(x + xx, y + yy, tuple(max(0, min(255, int(v * shd) + n)) for v in c))
            if sleeve < h - 2:
                for xx in range(w):
                    self.px(x + xx, y + sleeve - 1, shade(shirt, 0.8))  # cuff line
        self.fill(f["top"], shade(shirt, 1.1), 2)
        self.fill(f["bottom"], glove if glove else shade(self.skin, 0.75), 2)

    def leg(self):
        j = self.job
        f = self.box("right_leg", j["pants"])
        for k in ("front", "back", "left", "right"):
            x, y, w, h = f[k]
            for yy in range(h - 3, h):
                for xx in range(w):
                    n = self.rnd.randint(-2, 2)
                    c = j["boots"] if yy < h - 1 else shade(j["boots"], 0.6)
                    self.px(x + xx, y + yy, tuple(max(0, min(255, v + n)) for v in c))
        self.fill(f["bottom"], shade(j["boots"], 0.55), 2)

    def solid(self, uvkey, base, front_hook=None, noise=4):
        f = self.box(uvkey, base, noise=noise)
        if front_hook:
            front_hook(f)
        return f

    def paint_part(self, p, hair):
        ck = p["ck"]
        uv = p["uv"]
        if getattr(self, "_done", None) is None:
            self._done = set()
        if uv in self._done:
            return
        self._done.add(uv)
        j = self.job
        col = self.cols.get(ck, (128, 128, 128))
        if ck == "head":
            self.head(hair)
        elif ck == "body":
            self.body()
        elif ck == "arm":
            self.arm()
        elif ck == "leg":
            self.leg()
        elif ck in ("nose", "ear"):
            self.box(uv, shade(self.skin, 0.96), noise=2)
        elif ck == "lids":
            self.box(uv, shade(self.skin, 0.98), noise=1)
        elif ck == "hair":
            self.box(uv, (176, 176, 176), noise=22)
            for k, r in faces(uv).items():  # strand streaks
                x, y, w, h = r
                for xx in range(x, x + w, 2):
                    for yy in range(y, y + h):
                        self.px(xx, yy, shade((176, 176, 176), 0.82 + 0.1 * ((yy + xx) % 3)))
        elif ck == "coat":
            self.coat(uv)
        elif ck == "apron":
            self.box(uv, col, noise=3)
            f = faces(uv); x, y, w, h = f["front"]
            if uv == "apron" and w >= 6:
                for xx in range(1, w - 1):
                    self.px(x + xx, y + h - 3, shade(col, 0.8))
                for yy in range(h - 5, h - 2):
                    self.px(x + 1, y + yy, shade(col, 0.8)); self.px(x + w - 2, y + yy, shade(col, 0.8))
        elif ck == "glasses":
            self.box(uv, (40, 36, 34), noise=0)
            x, y, w, h = faces(uv)["front"]
            for yy in range(h):
                for xx in range(w):
                    inner = (1 <= xx <= 2 or 5 <= xx <= 6) and (yy == 0 or yy == 1) and False
                    if xx in (1, 2, 5, 6):
                        self.px(x + xx, y + yy, (0, 0, 0), 0)  # clear lens
                    elif xx == 3 or xx == 4:
                        self.px(x + xx, y + yy, (40, 36, 34) if yy == 0 else (0, 0, 0), 255 if yy == 0 else 0)
            # frame rows
            for xx in (1, 2, 5, 6):
                self.px(x + xx, y, (40, 36, 34)); self.px(x + xx, y + h - 1, (40, 36, 34))
        elif ck == "shades":
            self.box(uv, (24, 24, 30), noise=0)
            x, y, w, h = faces(uv)["front"]
            self.px(x + 1, y, (120, 140, 170)); self.px(x + 5, y, (120, 140, 170))
            for xx in range(w):
                self.px(x + xx, y, (24, 24, 30)) if xx not in (1, 5) else None
        elif ck == "maskeyes":
            self.box(uv, self.cols["mask"], noise=2)
            x, y, w, h = faces(uv)["front"]
            for ex in (1, 2, 5, 6):  # eye holes
                for yy in range(h):
                    self.px(x + ex, y + yy, (0, 0, 0), 0)
        elif ck == "glass":
            self.box(uv, self.cols["glass"], noise=4)
            x, y, w, h = faces(uv)["front"]
            self.px(x, y, (255, 255, 255)); self.px(x + 1, y, (255, 255, 255))
            for xx in range(w):
                self.px(x + xx, y + h - 1, (30, 40, 50))
        elif ck == "lamp":
            self.box(uv, col, noise=6)
        elif ck == "plume":
            self.box(uv, col, noise=10)
        else:
            self.box(uv, col, noise=4)

    def coat(self, uv):
        j = self.job
        base = self.cols["coat"]
        f = self.box(uv, base, noise=4)
        x, y, w, h = f["front"]
        d = j["coat_detail"]
        if d == "double":
            for yy in (3, 5, 7):
                self.px(x + 2, y + yy, (170, 170, 176)); self.px(x + 5, y + yy, (170, 170, 176))
            for yy in range(1, 11):
                self.px(x + 3, y + yy, shade(base, 0.85)); self.px(x + 4, y + yy, shade(base, 0.85))
        elif d == "waistcoat":
            for yy in range(h):
                self.px(x + 3, y + yy, (30, 20, 16)); self.px(x + 4, y + yy, (30, 20, 16))
            for yy in (3, 6, 9):
                self.px(x + 2, y + yy, (220, 190, 90))
            for xx in (3, 4):
                self.px(x + xx, y, (232, 224, 208))
        elif d == "zip":
            for yy in range(h):
                self.px(x + 3, y + yy, (60, 64, 72))
            for xx in range(1, 4):
                self.px(x + xx, y + 5, shade(base, 0.7)); self.px(x + 8 - xx - 1, y + 5, shade(base, 0.7))
        elif d == "fur":
            for xx in range(w):
                for yy in range(2):
                    self.px(x + xx, y + yy, (240, 238, 232) if (xx + yy) % 2 == 0 else (214, 210, 202))
            for yy in range(2, h):
                self.px(x + 3, y + yy, shade(base, 0.7)); self.px(x + 4, y + yy, shade(base, 0.7))
            for yy in range(h):
                self.px(x + w - 1, y + yy, shade(base, 0.7)) if False else None
        elif d == "stripes":
            for xx in range(w):
                self.px(x + xx, y + 5, (230, 232, 226)); self.px(x + xx, y + 6, (230, 232, 226))
                self.px(x + xx, y + 9, (230, 232, 226))
            for yy in range(h):
                self.px(x + 3, y + yy, shade(base, 0.8)); self.px(x + 4, y + yy, shade(base, 0.8))
                self.px(x + 1, y + yy, (230, 232, 226)) ; self.px(x + 6, y + yy, (230, 232, 226))
            bx, by, bw, bh = f["back"]
            for xx in range(bw):
                self.px(bx + xx, by + 5, (230, 232, 226)); self.px(bx + xx, by + 6, (230, 232, 226))
        elif d == "armor":
            for yy in range(h):
                for xx in range(w):
                    if (xx + yy) % 5 == 0:
                        self.px(x + xx, y + yy, shade(base, 1.25))
            for yy in range(h):
                self.px(x + 3, y + yy, (30, 28, 32)); self.px(x + 4, y + yy, (30, 28, 32))
            for yy in (2, 5, 8):
                self.px(x + 1, y + yy, (170, 172, 182)); self.px(x + 6, y + yy, (170, 172, 182))
        elif d == "tabard":
            for yy in range(h):
                self.px(x + 3, y + yy, (232, 200, 80)); self.px(x + 4, y + yy, (232, 200, 80))
            for yy in range(0, 3):
                for xx in range(w):
                    self.px(x + xx, y + yy, (170, 176, 190))
            self.px(x + 3, y + 6, (200, 48, 48)); self.px(x + 4, y + 6, (200, 48, 48))
            self.px(x + 3, y + 7, (200, 48, 48)); self.px(x + 4, y + 7, (200, 48, 48))
            for xx in range(w):
                self.px(x + xx, y + h - 1, (232, 200, 80))


def paint_all(job, skin):
    pr = Painter(job, skin, seed=hash((job, skin)) & 0xFFFF)
    hair = (60, 40, 30)
    for p in P:
        pr.paint_part(p, hair)
    return pr.img


# ---------------- Java ----------------
def fmt(v):
    s = ("%.4f" % v).rstrip("0").rstrip(".")
    return (s if s not in ("", "-0") else "0") + "F"

def java():
    out = []
    a = out.append
    a("package com.ysh.tycoonrogue.client;\n")
    a("import net.minecraft.client.model.geom.builders.CubeDeformation;")
    a("import net.minecraft.client.model.geom.builders.CubeListBuilder;")
    a("import net.minecraft.client.model.geom.builders.LayerDefinition;")
    a("import net.minecraft.client.model.geom.builders.MeshDefinition;")
    a("import net.minecraft.client.model.geom.builders.PartDefinition;")
    a("import net.minecraft.client.model.geom.PartPose;\n")
    a("import java.util.Map;\n")
    a("/** GENERATED by tools/gen_customer_model.py. Do not edit by hand. */")
    a("public final class CustomerModelDef {")
    a("    private CustomerModelDef() {\n    }\n")
    a("    public static final int SKIN_COUNT = %d;" % len(SKINS))
    a("    public static final String[] HAIR_PARTS = {%s};" % ", ".join('"head/%s"' % h for h in HAIR))
    styles = ", ".join("{%s}" % ", ".join(str(HAIR.index(h)) for h in s) for s in HAIR_STYLES)
    a("    public static final int[][] HAIR_STYLES = {%s};" % styles)
    a("    public static final int TAIL_INDEX = %d;" % HAIR.index("hair_tail"))
    path = {}
    for p in P:
        path[p["name"]] = (path[p["parent"]] + "/" if p["parent"] else "") + p["name"]
    core = {"head", "body", "right_arm", "left_arm", "right_leg", "left_leg"}
    acc = [path[p["name"]] for p in P if p["name"] not in core and p["name"] not in HAIR]
    a("    /** Every accessory part (hair excluded); hidden unless the job or disguise shows it. */")
    a("    public static final String[] ACCESSORY_PARTS = {%s};" % ", ".join('"%s"' % x for x in acc))
    a("    public static final String[] ALL_PARTS = {%s};" % ", ".join('"%s"' % path[p["name"]] for p in P))
    a("    public static final Map<String, String[]> JOB_PARTS = Map.ofEntries(")
    ents = []
    for job in JOB_ORDER:
        ents.append('            Map.entry("%s", new String[]{%s})' % (
            job, ", ".join('"%s"' % path[n] for n in JOBS[job]["parts"])))
    a(",\n".join(ents) + ");")
    a("    public static final java.util.Set<String> HAT_JOBS = java.util.Set.of(%s);" % ", ".join(
        '"%s"' % j for j in JOB_ORDER if JOBS[j]["hat_on"]))
    for k, names in REVEAL.items():
        a("    public static final String[] REVEAL_%d = {%s};" % (k, ", ".join('"%s"' % path[n] for n in names)))
    a("    public static final String LIDS = \"%s\";\n" % path["lids"])
    a("    public static LayerDefinition create() {")
    a("        MeshDefinition mesh = new MeshDefinition();")
    a("        PartDefinition root = mesh.getRoot();")
    for p in P:
        u, v = UVS[p["uv"]]
        w, h, d = p["dims"]
        x, y, z = p["org"]
        px, py, pz, rx, ry, rz = p["pose"]
        parent = "root" if not p["parent"] else "p_" + p["parent"]
        var = "p_" + p["name"]
        mir = ".mirror()" if p["mirror"] else ""
        a("        PartDefinition %s = %s.addOrReplaceChild(\"%s\", CubeListBuilder.create().texOffs(%d, %d)%s.addBox(%s, %s, %s, %s, %s, %s, new CubeDeformation(%s)), PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s));"
          % (var, parent, p["name"], u, v, mir, fmt(x), fmt(y), fmt(z), fmt(w), fmt(h), fmt(d), fmt(p["grow"]),
             fmt(px), fmt(py), fmt(pz), fmt(rx), fmt(ry), fmt(rz)))
    a("        return LayerDefinition.create(mesh, %d, %d);" % (TW, TH))
    a("    }")
    a("}")
    return "\n".join(out) + "\n"


# ---------------- preview ----------------
def preview(imgs):
    """Front and back view of each job from the textures (flat; ignores rotated parts)."""
    S = 4
    cell_w, cell_h = 26 * S, 48 * S
    sheet = Image.new("RGBA", (cell_w * len(imgs) * 2, cell_h), (70, 80, 96, 255))
    for i, (job, img) in enumerate(imgs):
        for side in (0, 1):
            ox = (i * 2 + side) * cell_w + 13 * S
            oy = 14 * S
            draw = []
            parts = ["head", "body", "right_arm", "left_arm", "right_leg", "left_leg"] + \
                    [p["name"] for p in P if p["name"] in JOBS[job]["parts"] and p["name"] not in
                     ("horn_l", "horn_r")] + HAIR_STYLES[1][:2] + (["hair_back"] if side else [])
            hatless = not JOBS[job]["hat_on"]
            for p in P:
                if p["name"] not in parts:
                    continue
                if p["name"] in HAIR and not hatless and p["name"] not in ("hair_cap", "hair_fringe", "hair_back"):
                    continue
                pp = p["pose"]
                par = {"": (0, 0), "head": (0, 0), "body": (0, 0), "right_arm": (-5, 2), "left_arm": (5, 2)}
                bx = par.get(p["parent"], (0, 0))
                x, y, z = p["org"]
                w, h, d = p["dims"]
                g = p["grow"]
                px0 = pp[0] + (bx[0] if p["parent"] in ("right_arm", "left_arm") else 0) + x - g
                py0 = pp[1] + (bx[1] if p["parent"] in ("right_arm", "left_arm") else 0) + y - g
                if p["parent"] == "" and p["name"] in ("right_arm", "left_arm", "right_leg", "left_leg"):
                    pass
                zc = z + (d if side == 0 else 0)
                draw.append((-zc if side == 0 else zc, p, px0, py0, w + 2 * g, h + 2 * g))
            draw.sort(key=lambda t: t[0])
            for _, p, px0, py0, ww, hh in draw:
                f = faces(p["uv"])["front" if side == 0 else "back"]
                region = img.crop((f[0], f[1], f[0] + f[2], f[1] + f[3]))
                if side == 1:
                    region = region.transpose(Image.FLIP_LEFT_RIGHT)
                if p["mirror"] and side == 0:
                    region = region.transpose(Image.FLIP_LEFT_RIGHT)
                region = region.resize((max(1, int(ww * S)), max(1, int(hh * S))), Image.NEAREST)
                x0 = ox + int(px0 * S) if side == 0 else ox + int((-px0 - ww) * S)
                sheet.alpha_composite(region, (max(0, x0), max(0, oy + int((py0 + 8) * S) - 8 * S)))
    return sheet


def main():
    os.makedirs(PREVIEW, exist_ok=True)
    with open(JAVA, "w") as fh:
        fh.write(java())
    prev = []
    for s in range(len(SKINS)):
        d = os.path.join(TEX, "skin%d" % s)
        os.makedirs(d, exist_ok=True)
        for job in JOB_ORDER:
            img = paint_all(job, s)
            img.save(os.path.join(d, job + ".png"))
            if s == 0:
                prev.append((job, img))
    preview(prev).save(os.path.join(PREVIEW, "customer_preview.png"))
    print("atlas %dx%d, %d parts, %d jobs x %d skins" % (TW, TH, len(P), len(JOB_ORDER), len(SKINS)))


if __name__ == "__main__":
    main()
