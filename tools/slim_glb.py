"""A slimmer copy of the asset pack's models (owner, 2026-11-18: "Lets try 1 & 2 ... Make it a build option so we can easily go
back"). Used by `py tools/build_asset_pack.py zip --slim`; the plain `zip` is untouched, so going back is building without it.

1. Textures: every embedded PNG larger than MAX_TEXTURE px a side is halved until it fits (one headless Blender run resizes all
   of them; Blender is the pack builder's own tool). The 4096 x 4096 body textures were 1.7 GB of the 3.4 GB pack, and each costs
   64 MB of graphics memory where a 2048 one costs 16.
2. The animation index: the models' JSON was 0.87 GB, mostly one accessor and one bufferView per animation channel. Identical
   keyframe-time tracks are shared (most channels of a clip have the same times), the accessors nothing uses any more are dropped,
   samplers drop the default "LINEAR" (SkinnedModel reads a missing interpolation as linear), and every non-image, non-strided
   bufferView is packed into one (Glb reads an accessor's own byteOffset into its view).

Meshes, skins and every value stay byte for byte; only where they sit and how they're indexed changes.
"""
import json
import os
import struct
import subprocess
import tempfile

MAX_TEXTURE = 2048
BLENDER = r"C:\Program Files\Blender Foundation\Blender 5.2\blender.exe"
_RESIZE = r'''
import bpy, sys
args = sys.argv[sys.argv.index("--") + 1:]
mx = int(args[0])
for p in args[1:]:
    img = bpy.data.images.load(p)
    w, h = img.size
    while w > mx or h > mx:
        w //= 2
        h //= 2
    img.scale(w, h)
    img.filepath_raw = p
    img.file_format = "PNG"
    img.save()
'''
PNG = b"\x89PNG\r\n\x1a\n"


def read(data):
    json_len = struct.unpack_from("<I", data, 12)[0]
    gltf = json.loads(data[20:20 + json_len])
    bin_len = struct.unpack_from("<I", data, 20 + json_len)[0]
    binary = data[20 + json_len + 8:20 + json_len + 8 + bin_len]
    return gltf, binary


def write(gltf, binary):
    js = json.dumps(gltf, separators=(",", ":")).encode()
    while len(js) % 4:
        js += b" "
    while len(binary) % 4:
        binary += b"\0"
    total = 12 + 8 + len(js) + 8 + len(binary)
    return (struct.pack("<III", 0x46546C67, 2, total) + struct.pack("<II", len(js), 0x4E4F534A) + js
            + struct.pack("<II", len(binary), 0x004E4942) + binary)


def view_bytes(gltf, binary, v):
    view = gltf["bufferViews"][v]
    start = view.get("byteOffset", 0)
    return binary[start:start + view["byteLength"]]


def big_images(gltf, binary, mx=MAX_TEXTURE):
    """The bufferViews of embedded PNGs larger than mx a side."""
    out = []
    for img in gltf.get("images", []):
        v = img.get("bufferView")
        if v is None:
            continue
        blob = view_bytes(gltf, binary, v)
        if blob[:8] == PNG and max(struct.unpack(">II", blob[16:24])) > mx:
            out.append(v)
    return out


def resize_all(paths, mx=MAX_TEXTURE):
    """Resizes the PNG files in place, all in one headless Blender run."""
    if not paths:
        return
    with tempfile.NamedTemporaryFile("w", suffix=".py", delete=False) as f:
        f.write(_RESIZE)
        script = f.name
    try:
        subprocess.run([BLENDER, "-b", "--factory-startup", "--python", script, "--", str(mx)] + list(paths),
                       check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    finally:
        os.remove(script)


def accessor_bytes(gltf, binary, a):
    acc = gltf["accessors"][a]
    if "bufferView" not in acc or acc.get("sparse"):
        return None
    view = gltf["bufferViews"][acc["bufferView"]]
    if view.get("byteStride"):
        return None
    comps = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4, "MAT2": 4, "MAT3": 9, "MAT4": 16}[acc["type"]]
    size = {5120: 1, 5121: 1, 5122: 2, 5123: 2, 5125: 4, 5126: 4}[acc["componentType"]]
    start = view.get("byteOffset", 0) + acc.get("byteOffset", 0)
    return acc["componentType"], acc["type"], acc["count"], binary[start:start + acc["count"] * comps * size]


def slim(data, resized=None):
    """The slim copy of one .glb's bytes. resized: {bufferView index: new PNG bytes} for its big images."""
    gltf, binary = read(data)
    resized = resized or {}
    accessors = gltf.get("accessors", [])

    # 2a. share identical keyframe-time tracks (sampler inputs)
    first = {}
    remap = {}
    for anim in gltf.get("animations", []):
        for s in anim.get("samplers", []):
            a = s["input"]
            key = accessor_bytes(gltf, binary, a)
            if key is None:
                continue
            if key in first:
                remap[a] = first[key]
            else:
                first[key] = a
            s["input"] = remap.get(a, a)
            if s.get("interpolation") == "LINEAR":
                del s["interpolation"]

    # 2b. drop accessors nothing uses any more, renumbering every reference
    used = set()
    for mesh in gltf.get("meshes", []):
        for p in mesh.get("primitives", []):
            used.update(p.get("attributes", {}).values())
            if "indices" in p:
                used.add(p["indices"])
            for t in p.get("targets", []):
                used.update(t.values())
    for skin in gltf.get("skins", []):
        if "inverseBindMatrices" in skin:
            used.add(skin["inverseBindMatrices"])
    for anim in gltf.get("animations", []):
        for s in anim.get("samplers", []):
            used.update((s["input"], s["output"]))
    order = sorted(used)
    new_index = {old: i for i, old in enumerate(order)}
    gltf["accessors"] = [accessors[i] for i in order]
    for mesh in gltf.get("meshes", []):
        for p in mesh.get("primitives", []):
            p["attributes"] = {k: new_index[v] for k, v in p.get("attributes", {}).items()}
            if "indices" in p:
                p["indices"] = new_index[p["indices"]]
            if "targets" in p:
                p["targets"] = [{k: new_index[v] for k, v in t.items()} for t in p["targets"]]
    for skin in gltf.get("skins", []):
        if "inverseBindMatrices" in skin:
            skin["inverseBindMatrices"] = new_index[skin["inverseBindMatrices"]]
    for anim in gltf.get("animations", []):
        for s in anim.get("samplers", []):
            s["input"] = new_index[s["input"]]
            s["output"] = new_index[s["output"]]

    # 1 + 2c. rebuild the binary: images (resized where given) and strided views as their own views, every other accessor's
    # data packed into one shared view, each at a 4-byte boundary
    old_views = gltf["bufferViews"]
    image_views = {img["bufferView"] for img in gltf.get("images", []) if "bufferView" in img}
    out = bytearray()
    views = []

    def align():
        while len(out) % 4:
            out.append(0)

    view_map = {}
    for v in sorted(image_views | {acc["bufferView"] for acc in gltf["accessors"]
                                   if "bufferView" in acc and old_views[acc["bufferView"]].get("byteStride")}):
        blob = resized.get(v) or view_bytes(gltf, binary, v)
        align()
        nv = dict(old_views[v])
        nv["byteOffset"] = len(out)
        nv["byteLength"] = len(blob)
        out += blob
        view_map[v] = len(views)
        views.append(nv)
    shared_start = None
    for acc in gltf["accessors"]:
        v = acc.get("bufferView")
        if v is None or v in view_map:
            if v is not None:
                acc["bufferView"] = view_map[v]
            continue
        old = old_views[v]
        start = old.get("byteOffset", 0) + acc.get("byteOffset", 0)
        comps = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4, "MAT2": 4, "MAT3": 9, "MAT4": 16}[acc["type"]]
        size = {5120: 1, 5121: 1, 5122: 2, 5123: 2, 5125: 4, 5126: 4}[acc["componentType"]]
        blob = binary[start:start + acc["count"] * comps * size]
        align()
        if shared_start is None:
            shared_start = len(out)
        acc["byteOffset"] = len(out) - shared_start
        acc["bufferView"] = -1  # the shared view, numbered below
        out += blob
    if shared_start is not None:
        align()
        shared = {"buffer": 0, "byteOffset": shared_start, "byteLength": len(out) - shared_start}
        for acc in gltf["accessors"]:
            if acc.get("bufferView") == -1:
                acc["bufferView"] = len(views)
        views.append(shared)
    for img in gltf.get("images", []):
        if "bufferView" in img:
            img["bufferView"] = view_map[img["bufferView"]]
    gltf["bufferViews"] = views
    gltf["buffers"] = [{"byteLength": len(out)}]
    return write(gltf, bytes(out))


def slim_files(paths, tmp_dir):
    """{path: slim bytes} for these .glb files (all held at once: for a few; the pack's zip uses iter_slim)."""
    return dict(iter_slim(paths, tmp_dir))


def iter_slim(paths, tmp_dir):
    """(path, slim bytes) for each .glb, one at a time: every big image first resized in one Blender run, then each model read,
    rebuilt and handed on, so only one model is in memory."""
    plans = {}
    to_resize = []
    for path in paths:
        gltf, binary = read(open(path, "rb").read())
        files = {}
        for v in big_images(gltf, binary):
            p = os.path.join(tmp_dir, f"{len(to_resize)}.png")
            open(p, "wb").write(view_bytes(gltf, binary, v))
            files[v] = p
            to_resize.append(p)
        plans[path] = files
    resize_all(to_resize)
    for path, files in plans.items():
        yield path, slim(open(path, "rb").read(), {v: open(p, "rb").read() for v, p in files.items()})
        for p in files.values():
            os.remove(p)
