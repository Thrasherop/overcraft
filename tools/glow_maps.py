"""Bakes the heroes' glows from the data into the local asset pack, no Blender needed: what the .glb can't carry (glTF keeps only
the colour image: tools/owm_survey.py gltf_materials), read from each material's own .owmat (the player's own extraction; never
committed). For every model in hero_models/ (the heroes, their view models, their weapons and parts):

    py tools/glow_maps.py <extraction root> [<extraction root> ...]     (each root holds Heroes/<Hero>/Skin/*/*/Models/*/Materials)

writes hero_models/<model>_glow.json ({material: {"texture": <id>, "strength": s}}) and hero_models/glow/<id>.png, an opaque image
whose colour is the light the material adds. The mod draws it as added light at full bright on the piece's own UVs
(HeroModelRenderer.submitModel), so glows hold at night and pieces with no colour image still show.

What a glow is, by the shader's slot (io_scene_owm's shader_metadata names them; techniques/rendering/rendering.md "Glows"):
- 0xBCBE747D "Emission" and 0x5ACB436A "Skin Emission", grey masks: mask x the material's colour image (0xAD10F602), as the mods'
  colour image is the glow's colour in every map looked at (Sombra's lit pistol parts, Ana's rifle, Jetpack Cat's jetpack);
- 0xEDCD5483 "Decal ... Emission" (shader 0x22): the same over the decal's colour 0x665648E9;
- 0x0403F89A, shader 0x26's glow layer: its own colours (the slot Juno's helmet and Sombra's tablet use);
- an emission mask laid on another UV layer than the colour image (static 0xC7526E90 "UVLayer Emission", the add-on's UVMap<n>, 1-based;
  Tracer's body 7E12 says 2: her chronal accelerator's discs and rings packed in TEXCOORD_1 over a 512 mask): moved onto the piece's
  own UVs (TEXCOORD_0) through the model's triangles first (`transfer`), then x the colour image as above;
- shader 0x32 (light cards, no colour image at all): its image in 0xCA8ECF6A or 0xD569F1CA as it is (Juno's gun's face 4E48C, Ana's
  rifle's sight 38704), else its mask (0x42BB2842, 0x35BC18D4) x its colour ramp 0x5118206D's brightest row.
"""
import json
import os
import struct
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageChops

ROOT = Path(__file__).resolve().parent.parent
PACK = Path(os.environ["OC_ASSET_PACK"]) if os.environ.get("OC_ASSET_PACK") else ROOT / "local-assets" / "pack"
MODELS = PACK / "assets" / "overcraft" / "hero_models"
GLOW = MODELS / "glow"
MASKS = {0xBCBE747D, 0x5ACB436A}
ALBEDO = (0xAD10F602, 0xAD10F602)
DECAL_EMISSION, DECAL_COLOUR = 0xEDCD5483, 0x665648E9
GLOW_LAYER = 0x0403F89A
CARD_IMAGES = (0xCA8ECF6A, 0xD569F1CA, 0xFFD569F1)
CARD_MASKS = (0x42BB2842, 0x35BC18D4)
RAMP = 0x5118206D
# strength parameters the add-on knows ("Emission Strength")
STRENGTHS = {3250491852, 2446772623}
# the emission mask's UV layer, 1-based (io_scene_owm shader_metadata StaticInputs 3344068240 "UVLayer Emission"; it remaps only when != 1)
EMISSION_UV = 0xC7526E90
# ...but the static alone doesn't say it: moved through their layer 2/3, D.Va's 98C0/A5D8, Lucio's 801D, Mercy's staff 7FDB and Pharah's
# gun A0AA went dark (4-21% of the image lit before, under 0.5% after), and their UV0 bakes were played right. So only the materials
# whose other layer was looked at and lines up with the mask (Tracer's 7E12: TEXCOORD_1 drawn over 20473 puts her accelerator's two
# discs on the circles, its rings on the bands, her harness on the leg lines; through TEXCOORD_0 the mask lit her face and chest).
MOVED_UV = {"000000007E12"}
BLANK = {"00000000000A", "00000000000B", "00000000000C", "000000004791"}
MAX_SIZE = 512
# Hair cards whose colour image is black: the hair shader (0xD9) colours them itself (Lúcio's dreads 8052: 2123A is black with the strands' cut
# in its alpha; its other slots are no colour we can name: 2123B, a flat teal, reads as a flow map, the ramp 800263259A9D isn't his hair's
# brown in any row), and the .glb took the black image. Baked to hero_models/colour/<material>.png: the tint of the dreads' own solid mesh
# beside them (ours, picked from his model), shaded by the card's 0xBCBE747D mask (bright at the root; the share is ours), its alpha the
# colour image's, used by HeroModels in place of the black one (#105). `glow_maps.py --colours <root>` writes only these.
HAIR_SHADER = 0xD9
HAIR_SHADE = 0xBCBE747D
COLOURS = MODELS / "colour"
HAIR_SHADE_FLOOR = 0.62
HAIR_TINTS = {"000000008052": (74, 48, 44)}
# Screens whose image holds their states in its channels (Juno's gun 4E48C: red the frame, green the happy face, blue the angry; its look
# by the ramp 800957B5CD42, a flat grey, so the colour is ours): written as the frame + one face, and the frame + the other as "alt".
# Glass over a screen: a flat-colour shell on the screen's own bounds (Juno's gun FBC0, 85 v over FB30's 40 v; shader 0x26, its colour and
# glow the same flat 1E38C). Drawn solid it hid her face and read white; so drawn only as this faint added light (ours).
SHADER_26_STRENGTH = 0.25
# A grey card's colour where its ramp says nothing (flat): Brigitte's shield's 479 v (6B00: image 14D13, the bracket and core), ours from the barrier's cyan.
CARD_TINTS = {"000000006B00": (0.45, 0.85, 1.0)}
# D.Va's MEKA canopy A5D9 (FBB0 submesh 18, over her seated head): shader 0x26 as Juno's glass; drawn solid it hid her face in every
# view and emote (^o^, Heart Power), so the same faint added light (techniques/emotes/dva_emotes.md)
GLASS = {"00000000FBC0": 0.1, "00000000A5D9": 0.1}
SCREEN_FACES = {"00000000FB30": (0.62, 0.95, 1.0)}
# A mask this dim everywhere adds nothing a player sees (out of 255).
DIM = 12
# The models with glows, by exact name: a prefix list ate juno_cell and brigitte_shield (critique_parts A4). Heroes, their view models and their weapons
# by the pattern, the parts from weapon_parts.json, and these.
MODELS_BY_NAME = {"mercy_staff", "mercy_blaster", "ana_pistol", "brigitte_flail_head", "brigitte_shield", "sombra_beacon", "sigma_barrier_orb",
                  "lifeweaver_platform", "lifeweaver_seed", "lifeweaver_tree", "projectile_kunai", "projectile_ofuda", "projectile_suzu",
                  "projectile_lifeweaver_seed", "tracer_weapon"}
HERO_IDS = {"kiriko", "lucio", "jetpack_cat", "mercy", "lifeweaver", "ana", "sombra", "brigitte", "juno", "sojourn", "sigma", "dva", "dva_pilot",
            # the heroes after D.Va were never listed, so the glows run wrote no table for them (Junkrat's build report, #91). Tracer's
            # body 7E12 lit her face, arms and chest until its mask was read on its own UV layer (EMISSION_UV, #91)
            "pharah", "cassidy", "junkrat", "tracer"}


def glowing_model(stem):
    if stem in MODELS_BY_NAME or stem in HERO_IDS:
        return True
    if stem.endswith("_1p") and stem[:-3] in HERO_IDS or stem.endswith("_weapon") and stem[:-7] in HERO_IDS:
        return True
    table = json.loads((ROOT / "src" / "main" / "resources" / "assets" / "overcraft" / "weapon_parts.json").read_text(encoding="utf8"))
    return any(isinstance(rows, list) and any(row["model"] == stem for row in rows) for rows in table.values())


def owmat(path):
    """A material .owmat (the add-on's OWMATFormat 3.0): its shader, {key: texture path}, its strength (or 1), its emission mask's UV
    layer (1-based, or 1)."""
    data = path.read_bytes()
    count, statics, shader = struct.unpack_from("<QQI", data, 8)
    pos = 28
    textures = {}
    for _ in range(count):
        b1 = data[pos]
        pos += 1
        n = b1 % 128
        if b1 > 128:
            n += data[pos] * 128
            pos += 1
        name = data[pos:pos + n].decode("utf8")
        pos += n
        (key,) = struct.unpack_from("<I", data, pos)
        pos += 4
        textures[key] = (path.parent / name.replace("\\", "/")).resolve()
    strength = 1.0
    layer = 1
    for _ in range(statics):
        key, length = struct.unpack_from("<II", data, pos)
        pos += 8
        if key in STRENGTHS and length >= 4:
            strength = struct.unpack_from("<f", data, pos)[0]
        if key == EMISSION_UV and length >= 4:
            layer = struct.unpack_from("<I", data, pos)[0] or 1
        pos += length
    return shader, textures, strength, layer


def glb_materials(path):
    """The material names a .glb's meshes use."""
    data = path.read_bytes()
    size = struct.unpack_from("<I", data, 12)[0]
    js = json.loads(data[20:20 + size])
    used = {p["material"] for m in js.get("meshes", []) for p in m["primitives"] if "material" in p}
    meshes = {p["material"]: m.get("name", "") for m in js.get("meshes", []) for p in m["primitives"] if "material" in p}
    return {js["materials"][i]["name"].split(".")[0]: meshes[i] for i in used}


def glb_uvs(path, material, layer):
    """[(TEXCOORD_0, TEXCOORD_<layer-1>, triangles)] for each primitive of the .glb drawn with that material that holds that layer."""
    data = path.read_bytes()
    size = struct.unpack_from("<I", data, 12)[0]
    js = json.loads(data[20:20 + size])
    blob = data[20 + size + 8:]

    def accessor(i):
        a = js["accessors"][i]
        view = js["bufferViews"][a["bufferView"]]
        comps = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4}[a["type"]]
        kind = {5126: np.float32, 5125: np.uint32, 5123: np.uint16, 5121: np.uint8}[a["componentType"]]
        width = comps * np.dtype(kind).itemsize
        stride = view.get("byteStride") or width
        start = view.get("byteOffset", 0) + a.get("byteOffset", 0)
        raw = np.frombuffer(blob, np.uint8, (a["count"] - 1) * stride + width, start)
        rows = np.lib.stride_tricks.as_strided(raw, (a["count"], width), (stride, 1)).copy()
        return rows.view(kind).reshape(-1, comps)

    other = f"TEXCOORD_{layer - 1}"
    found = []
    for mesh in js.get("meshes", []):
        for prim in mesh["primitives"]:
            attrs = prim["attributes"]
            if "material" not in prim or other not in attrs or "indices" not in prim:
                continue
            if js["materials"][prim["material"]]["name"].split(".")[0] != material:
                continue
            found.append((accessor(attrs["TEXCOORD_0"]), accessor(attrs[other]), accessor(prim["indices"]).reshape(-1, 3)))
    return found


def transfer(mask_path, pieces, size=MAX_SIZE):
    """A grey mask laid out on another UV layer, redrawn on TEXCOORD_0: each triangle filled in TEXCOORD_0 space, the mask read where
    each pixel falls in the other layer (barycentric). Triangles whose bounds in the other layer hold no light are skipped."""
    mask = np.asarray(Image.open(mask_path).convert("L"), dtype=np.float32)
    mh, mw = mask.shape
    out = np.zeros((size, size), np.float32)
    for uv0, uvn, tris in pieces:
        for tri in tris:
            q = uvn[tri]
            lo_n, hi_n = q.min(0), q.max(0)
            box = mask[max(int(lo_n[1] * mh), 0):int(np.ceil(hi_n[1] * mh)) + 1, max(int(lo_n[0] * mw), 0):int(np.ceil(hi_n[0] * mw)) + 1]
            if box.size == 0 or box.max() < DIM:
                continue
            p = uv0[tri].astype(np.float64) * size
            lo, hi = np.floor(p.min(0)).astype(int), np.ceil(p.max(0)).astype(int)
            xs, ys = np.meshgrid(np.arange(lo[0], hi[0] + 1) + 0.5, np.arange(lo[1], hi[1] + 1) + 0.5)
            d = (p[1, 1] - p[2, 1]) * (p[0, 0] - p[2, 0]) + (p[2, 0] - p[1, 0]) * (p[0, 1] - p[2, 1])
            if abs(d) < 1e-9:
                continue
            w0 = ((p[1, 1] - p[2, 1]) * (xs - p[2, 0]) + (p[2, 0] - p[1, 0]) * (ys - p[2, 1])) / d
            w1 = ((p[2, 1] - p[0, 1]) * (xs - p[2, 0]) + (p[0, 0] - p[2, 0]) * (ys - p[2, 1])) / d
            w2 = 1 - w0 - w1
            # a little past the edges too, so a piece's border texels aren't left dark
            inside = (w0 >= -0.05) & (w1 >= -0.05) & (w2 >= -0.05)
            u = w0 * q[0, 0] + w1 * q[1, 0] + w2 * q[2, 0]
            v = w0 * q[0, 1] + w1 * q[1, 1] + w2 * q[2, 1]
            value = mask[np.clip((v * mh).astype(int), 0, mh - 1), np.clip((u * mw).astype(int), 0, mw - 1)]
            xi, yi = (xs - 0.5).astype(int) % size, (ys - 0.5).astype(int) % size
            np.maximum.at(out, (yi[inside], xi[inside]), value[inside])
    grey = Image.fromarray(out.clip(0, 255).astype(np.uint8), "L")
    return Image.merge("RGB", (grey, grey, grey))


def rgb(path):
    return Image.open(path).convert("RGB")


def real(textures, key):
    path = textures.get(key)
    return path if path is not None and path.stem not in BLANK and path.exists() else None


def bake(shader, textures, moved=None):
    """The material's added light as an RGB image, or None when it has none. `moved`: its emission mask already put on TEXCOORD_0
    (`transfer`), used in place of the mask image."""
    for key in MASKS:
        mask = real(textures, key)
        if mask is not None:
            grey = moved if key == 0xBCBE747D and moved is not None else rgb(mask)
            colour = real(textures, ALBEDO[0])
            return multiply(rgb(colour), grey) if colour else grey
    decal = real(textures, DECAL_EMISSION)
    if decal is not None:
        colour = real(textures, DECAL_COLOUR)
        return multiply(rgb(colour), rgb(decal)) if colour else rgb(decal)
    if shader == 0x26 and real(textures, GLOW_LAYER) is not None:
        return rgb(real(textures, GLOW_LAYER))
    if shader == 0x32:
        for key in CARD_IMAGES:
            if real(textures, key) is not None:
                return rgb(real(textures, key))
        mask = next((real(textures, k) for k in CARD_MASKS if real(textures, k) is not None), None)
        ramp = real(textures, RAMP)
        if mask is not None and ramp is not None:
            strip = rgb(ramp)
            brightest = max(strip.getdata(), key=sum)
            return multiply(rgb(mask), Image.new("RGB", strip.size, brightest))
    return None


def hair(name, shader, textures):
    """A black hair card's colours (HAIR_SHADER above), or None."""
    if shader != HAIR_SHADER or name not in HAIR_TINTS or not all(k in textures for k in (HAIR_SHADE, ALBEDO[0])):
        return None
    albedo = Image.open(textures[ALBEDO[0]]).convert("RGBA")
    if max(band[1] for band in albedo.getextrema()[:3]) > 8:
        return None
    colour = Image.new("RGB", albedo.size, HAIR_TINTS[name])
    shade = Image.open(textures[HAIR_SHADE]).convert("L").resize(albedo.size, Image.BILINEAR)
    low, high = shade.getextrema()
    ramp = shade.point(lambda v: int(255 * (HAIR_SHADE_FLOOR + (1 - HAIR_SHADE_FLOOR) * (v - low) / max(high - low, 1))))
    out = ImageChops.multiply(colour, Image.merge("RGB", (ramp, ramp, ramp))).convert("RGBA")
    out.putalpha(albedo.getchannel("A"))
    return out


def multiply(a, b):
    size = (min(max(a.width, b.width), MAX_SIZE), min(max(a.height, b.height), MAX_SIZE))
    return ImageChops.multiply(a.resize(size), b.resize(size))


def index(roots):
    """Material id -> its .owmat, over every model folder of the extractions."""
    found = {}
    for root in roots:
        for path in Path(root).glob("Heroes/*/Skin/*/*/Models/*/Materials/*.owmat"):
            found.setdefault(path.stem, path)
    print(f"{len(found)} materials in {len(roots)} extractions")
    return found


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    only_colours = sys.argv[1] == "--colours"
    materials = index(sys.argv[2:] if only_colours else sys.argv[1:])
    GLOW.mkdir(parents=True, exist_ok=True)
    COLOURS.mkdir(parents=True, exist_ok=True)
    # masks moved onto TEXCOORD_0, by material: one image serves every model that shares the material, so each model's triangles add
    # to it (Tracer's 3P body and 1P arms both draw 7E12)
    moved = {}
    for model in sorted(MODELS.glob("*.glb")):
        if not glowing_model(model.stem):
            continue
        table = {}
        for name, mesh in sorted(glb_materials(model).items()):
            if mesh.startswith(("holo_", "holoc_")) or name not in materials:
                continue
            shader, textures, strength, layer = owmat(materials[name])
            if name in MOVED_UV and layer != 1 and real(textures, 0xBCBE747D) is not None:
                pieces = glb_uvs(model, name, layer)
                if not pieces:
                    print(f"{model.stem} {name}: its emission is on UV layer {layer}, which the .glb lacks: skipped")
                    continue
                grey = transfer(real(textures, 0xBCBE747D), pieces)
                moved[name] = ImageChops.lighter(moved[name], grey) if name in moved else grey
                print(f"{model.stem} {name}: emission moved from UV layer {layer}")
            coloured = hair(name, shader, textures)
            if coloured is not None:
                coloured.save(COLOURS / f"{name.lower()}.png")
                print(f"{model.stem} {name}: hair colours baked")
            if only_colours:
                continue
            image = bake(shader, textures, moved.get(name))
            if image is None:
                continue
            brightest = max(v for band in image.getextrema() for v in band)
            if brightest < DIM:
                continue
            image.thumbnail((MAX_SIZE, MAX_SIZE))
            if name in CARD_TINTS:
                image = Image.merge("RGB", [band.point(lambda v, c=c: int(v * c)) for band, c in zip(image.split(), CARD_TINTS[name])])
            table[name] = {"texture": name.lower(), "strength": round(min(max(strength, 0.0), 2.0), 3)}
            if shader == 0x26:
                # its glow layer is its colour image again (Lúcio's amp visor 811C, Mercy's 7FDC): added at any strength over the piece's own
                # colour it read flat white where the plain piece had detail (played), so a faint share (ours)
                table[name]["strength"] = SHADER_26_STRENGTH
            if name in GLASS:
                table[name].update(strength=GLASS[name], glass=True)
            if name in SCREEN_FACES:
                r, g, b = image.split()
                tint = SCREEN_FACES[name]
                for face, out in ((g, name.lower()), (b, name.lower() + "_alt")):
                    grey = ImageChops.lighter(r, face)
                    Image.merge("RGB", [grey.point(lambda v, c=c: int(v * c)) for c in tint]).save(GLOW / f"{out}.png")
                table[name]["alt"] = name.lower() + "_alt"
            else:
                image.save(GLOW / f"{name.lower()}.png")
            print(f"{model.stem} {name} shader {shader:X} brightest {brightest} strength {strength:.2f}")
        if only_colours:
            continue
        target = MODELS / f"{model.stem}_glow.json"
        if table:
            target.write_text(json.dumps(table, indent=1) + "\n", encoding="utf8")
        elif target.exists():
            target.unlink()


if __name__ == "__main__":
    main()
