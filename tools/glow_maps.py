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
- shader 0x32 (light cards, no colour image at all): its image in 0xCA8ECF6A or 0xD569F1CA as it is (Juno's gun's face 4E48C, Ana's
  rifle's sight 38704), else its mask (0x42BB2842, 0x35BC18D4) x its colour ramp 0x5118206D's brightest row.
"""
import json
import os
import struct
import sys
from pathlib import Path

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
            # the heroes after D.Va were never listed, so the glows run wrote no table for them (Junkrat's build report, #91). Not
            # Tracer: her body's 7E12 (shader D9) bakes her face, arms and chest lit (its 0xBCBE747D isn't an emission mask there)
            "pharah", "cassidy", "junkrat"}


def glowing_model(stem):
    if stem in MODELS_BY_NAME or stem in HERO_IDS:
        return True
    if stem.endswith("_1p") and stem[:-3] in HERO_IDS or stem.endswith("_weapon") and stem[:-7] in HERO_IDS:
        return True
    table = json.loads((ROOT / "src" / "main" / "resources" / "assets" / "overcraft" / "weapon_parts.json").read_text(encoding="utf8"))
    return any(isinstance(rows, list) and any(row["model"] == stem for row in rows) for rows in table.values())


def owmat(path):
    """A material .owmat (the add-on's OWMATFormat 3.0): its shader, {key: texture path}, its strength (or 1)."""
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
    for _ in range(statics):
        key, length = struct.unpack_from("<II", data, pos)
        pos += 8
        if key in STRENGTHS and length >= 4:
            strength = struct.unpack_from("<f", data, pos)[0]
        pos += length
    return shader, textures, strength


def glb_materials(path):
    """The material names a .glb's meshes use."""
    data = path.read_bytes()
    size = struct.unpack_from("<I", data, 12)[0]
    js = json.loads(data[20:20 + size])
    used = {p["material"] for m in js.get("meshes", []) for p in m["primitives"] if "material" in p}
    meshes = {p["material"]: m.get("name", "") for m in js.get("meshes", []) for p in m["primitives"] if "material" in p}
    return {js["materials"][i]["name"].split(".")[0]: meshes[i] for i in used}


def rgb(path):
    return Image.open(path).convert("RGB")


def real(textures, key):
    path = textures.get(key)
    return path if path is not None and path.stem not in BLANK and path.exists() else None


def bake(shader, textures):
    """The material's added light as an RGB image, or None when it has none."""
    for key in MASKS:
        mask = real(textures, key)
        if mask is not None:
            colour = real(textures, ALBEDO[0])
            return multiply(rgb(colour), rgb(mask)) if colour else rgb(mask)
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
    for model in sorted(MODELS.glob("*.glb")):
        if not glowing_model(model.stem):
            continue
        table = {}
        for name, mesh in sorted(glb_materials(model).items()):
            if mesh.startswith(("holo_", "holoc_")) or name not in materials:
                continue
            shader, textures, strength = owmat(materials[name])
            coloured = hair(name, shader, textures)
            if coloured is not None:
                coloured.save(COLOURS / f"{name.lower()}.png")
                print(f"{model.stem} {name}: hair colours baked")
            if only_colours:
                continue
            image = bake(shader, textures)
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
