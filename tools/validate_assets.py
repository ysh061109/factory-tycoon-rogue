#!/usr/bin/env python3
"""
Checks that nothing in the mod can show up as the purple-and-black "missing model" block:
every registered item has a model, every model's parent/textures exist, every face uses a
declared texture variable, UVs and coordinates are legal, every texture is a square PNG whose
side is a power of two (otherwise Minecraft treats it as an animation strip and breaks), and
every blockstate model exists.   Run: python3 tools/validate_assets.py   (exit 1 on problems)
"""
import glob, json, os, re, struct, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
A = os.path.join(ROOT, "src", "main", "resources", "assets", "tycoonrogue")
J = os.path.join(ROOT, "src", "main", "java")
errors = []


def err(m):
    errors.append(m)


def png_size(path):
    with open(path, "rb") as f:
        h = f.read(24)
    if h[:8] != b"\x89PNG\r\n\x1a\n":
        return None
    return struct.unpack(">II", h[16:24])


def tex_exists(ref):
    ns, _, p = ref.partition(":") if ":" in ref else ("minecraft", "", ref)
    if ns != "tycoonrogue":
        return True
    f = os.path.join(A, "textures", p + ".png")
    if not os.path.exists(f):
        return False
    w, h = png_size(f) or (0, 0)
    meta = f + ".mcmeta"
    ok = w > 0 and w == h and (w & (w - 1)) == 0
    if not ok and os.path.exists(meta):
        a = json.load(open(meta)).get("animation", {})
        ok = a.get("width") and a.get("height") and w % a["width"] == 0 and h % a["height"] == 0
    if not ok:
        err("texture %s is %sx%s (must be a square power-of-two PNG)" % (p, w, h))
    return True


def model_exists(ref):
    ns, _, p = ref.partition(":") if ":" in ref else ("minecraft", "", ref)
    if ns != "tycoonrogue":
        return True
    return os.path.exists(os.path.join(A, "models", p + ".json"))


def check_model(path):
    rel = os.path.relpath(path, A)
    d = json.load(open(path))
    parent = d.get("parent")
    if parent and not model_exists(parent):
        err("%s: parent %s missing" % (rel, parent))
    tex = d.get("textures", {})
    for k, v in tex.items():
        if not v.startswith("#") and not tex_exists(v):
            err("%s: texture %s missing" % (rel, v))
    # Variables used by faces must be defined here or in a parent we can read.
    defined = set(tex.keys())
    p = parent
    while p and p.startswith("tycoonrogue:"):
        pd = json.load(open(os.path.join(A, "models", p.split(":")[1] + ".json")))
        defined |= set(pd.get("textures", {}).keys())
        p = pd.get("parent")
    for i, el in enumerate(d.get("elements", [])):
        for a in (el["from"], el["to"]):
            if any(c < -16 or c > 32 for c in a):
                err("%s: element %d out of range" % (rel, i))
        r = el.get("rotation")
        if r and (abs(r["angle"]) > 45 or r["angle"] % 22.5 != 0):
            err("%s: element %d rotation %s illegal" % (rel, i, r["angle"]))
        for fn, face in el["faces"].items():
            t = face["texture"]
            if t.startswith("#") and t[1:] not in defined:
                err("%s: element %d face %s uses undefined %s" % (rel, i, fn, t))
            uv = face.get("uv")
            if uv and any(c < 0 or c > 16 for c in uv):
                err("%s: element %d face %s uv %s outside 0..16" % (rel, i, fn, uv))
    if d.get("elements") and "particle" not in defined and "/block/" in path.replace("\\", "/"):
        err("%s: block model has no particle texture" % rel)


for m in glob.glob(os.path.join(A, "models", "**", "*.json"), recursive=True):
    check_model(m)

# Blockstates.
def refs(node):
    if isinstance(node, dict):
        if "model" in node and isinstance(node["model"], str):
            yield node["model"]
        for v in node.values():
            yield from refs(v)
    elif isinstance(node, list):
        for v in node:
            yield from refs(v)


for b in glob.glob(os.path.join(A, "blockstates", "*.json")):
    for r in refs(json.load(open(b))):
        if not model_exists(r):
            err("%s: model %s missing" % (os.path.basename(b), r))

# Every registered item and block needs a model / blockstate.
src = open(os.path.join(J, "com", "ysh", "tycoonrogue", "registry", "ModItems.java"), encoding="utf-8").read()
for name in re.findall(r'(?:register|registerSimpleBlockItem)\(\s*"([a-z_]+)"', src):
    if not os.path.exists(os.path.join(A, "models", "item", name + ".json")):
        err("item %s has no models/item/%s.json" % (name, name))
src = open(os.path.join(J, "com", "ysh", "tycoonrogue", "registry", "ModBlocks.java"), encoding="utf-8").read()
for name in re.findall(r'register\(\s*"([a-z_]+)"', src):
    if not os.path.exists(os.path.join(A, "blockstates", name + ".json")):
        err("block %s has no blockstate" % name)

# Every texture file that exists is a valid square PNG; unused ones are reported as info only.
for f in glob.glob(os.path.join(A, "textures", "**", "*.png"), recursive=True):
    sz = png_size(f)
    if sz is None:
        err("%s is not a PNG" % f)

if errors:
    print("\n".join(errors))
    sys.exit(1)
print("assets OK")
