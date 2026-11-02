"""Gives an exported .glb's additive clips neutral keys where the game's clip keys nothing, in place (#75), so a clip that
owm_survey.py exported before it knew the additive flag needs no Blender re-export.

    py tools/neutralize_additive.py <file.glb> <clip map> <the model's DataTool Animations dir> [--dry-run]

An additive clip is one in an Animations/<flags>/<n>/ folder whose flags have bit 0x4 (4, 6, 20, 36, 132: see
techniques/animation/additive_clips.md). The glTF exporter writes every bone into every clip and fills the channels a clip
doesn't key with the bone's rest pose; as a delta that adds the rest offset and turn a second time. owm_survey.py keys such
channels neutral itself now (offset 0, no turn), but only did it for folders starting "4", so the 36-* and 132-* clips
(Lúcio's wall-ride and reload leans, Jetpack Cat's melee) came out with rest values. This rewrites those channels'
values to neutral, byte for byte in the binary chunk (same accessors, same key times), after checking each one holds the
rest pose it should. The game's own keys (read from the .owanimclip) are never touched. Root bones are left as they are,
as owm_survey.py leaves them. Overwatch must not be running while the Animations dir is read (CLAUDE.md).
"""
import ast
import json
import math
import re
import struct
import sys
from pathlib import Path

ADDON_BONES = Path.home() / "AppData/Roaming/Blender Foundation/Blender/5.2/extensions/user_default/io_scene_owm/BoneUtil.py"


def bone_names():
    """The add-on's bone renames (bone_0011 -> Head ...): the .owanimclip has the raw names, the glb the renamed ones."""
    if not ADDON_BONES.exists():
        sys.exit(f"no {ADDON_BONES} (the io_scene_owm add-on's bone names)")
    tree = ast.parse(ADDON_BONES.read_text(encoding="utf8"))
    for node in tree.body:
        if isinstance(node, ast.Assign) and any(getattr(t, "id", None) == "boneDict" for t in node.targets):
            return ast.literal_eval(node.value)
    sys.exit("no boneDict in " + str(ADDON_BONES))


def keyed_tracks(path, rename):
    """{glb bone name: set of 'T' / 'R' / 'S' it keys} from a .owanimclip (the add-on's OWAnimClipFormat 2.0)."""
    d = path.read_bytes()
    _major, _minor, nbones, _fps, _duration = struct.unpack_from("<HHIfI", d, 0)
    p = 16
    out = {}
    for _ in range(nbones):
        b1 = d[p]
        p += 1
        n = b1 % 128
        if b1 > 128:
            n += d[p] * 128
            p += 1
        name = d[p:p + n].decode("utf8")
        p += n
        (ntracks,) = struct.unpack_from("<I", d, p)
        p += 4
        keys = set()
        for _ in range(ntracks):
            ttype, nkeys, ncomp = struct.unpack_from("<III", d, p)
            p += 12 + nkeys * (4 + 4 * ncomp)
            if nkeys and ttype < 3:
                keys.add("TRS"[ttype])
        out[rename.get(name, name)] = keys
    return out


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    dry = "--dry-run" in sys.argv
    glb_path, map_path, anims = Path(args[0]), Path(args[1]), Path(args[2])
    data = glb_path.read_bytes()
    magic, version, _length = struct.unpack_from("<III", data, 0)
    if magic != 0x46546C67:
        sys.exit(f"{glb_path}: not a glb")
    json_len, _ = struct.unpack_from("<II", data, 12)
    doc = json.loads(data[20:20 + json_len])
    bin_len, _ = struct.unpack_from("<II", data, 20 + json_len)
    blob = bytearray(data[28 + json_len:28 + json_len + bin_len])
    nodes = doc["nodes"]
    parent = {c: i for i, n in enumerate(nodes) for c in n.get("children", [])}
    joints = {j for s in doc.get("skins", []) for j in s["joints"]}
    rename = bone_names()

    wanted = {}
    for line in map_path.read_text(encoding="utf8").splitlines():
        m = re.match(r"\s*(\d+)-(\d+)_([0-9A-Fa-f]+)\s*=\s*(\S+)", line.split("#", 1)[0])
        if m and int(m.group(1)) & 4:
            wanted[m.group(4)] = anims / m.group(1) / m.group(2) / f"{m.group(3)}.owanimclip"

    # The neutral keys go in new accessors at the end of the binary chunk (the exporter shares one accessor between
    # channels with the same values, so the old ones can't be overwritten for one channel): one per (width, key count).
    extra = bytearray()
    view = len(doc["bufferViews"])
    made = {}

    def neutral_accessor(width, count):
        if (width, count) not in made:
            made[(width, count)] = len(doc["accessors"])
            doc["accessors"].append({"bufferView": view, "byteOffset": len(extra), "componentType": 5126,
                                     "count": count, "type": "VEC3" if width == 3 else "VEC4"})
            extra.extend(struct.pack("<" + "f" * (width * count), *(NEUTRAL[width] * count)))
        return made[(width, count)]

    total = 0
    for anim in doc.get("animations", []):
        raw = wanted.get(anim["name"])
        if raw is None:
            continue
        if not raw.exists():
            print(f"  {anim['name']}: no {raw}, skipped")
            continue
        keyed = keyed_tracks(raw, rename)
        done = kept = 0
        for ch in anim["channels"]:
            node = ch["target"]["node"]
            path = {"translation": "T", "rotation": "R"}.get(ch["target"]["path"])
            if path is None or node not in joints or parent.get(node) not in joints:
                continue  # scale (the additive layers don't use it) / a root bone (owm_survey.py leaves those)
            name = nodes[node].get("name")
            if path in keyed.get(name, set()):
                kept += 1
                continue
            sampler = anim["samplers"][ch["sampler"]]
            acc = doc["accessors"][sampler["output"]]
            bv = doc["bufferViews"][acc["bufferView"]]
            if acc["componentType"] != 5126 or bv.get("byteStride"):
                sys.exit(f"{anim['name']} {name}: not tightly packed floats")
            width = 3 if path == "T" else 4
            at = bv.get("byteOffset", 0) + acc.get("byteOffset", 0)
            values = struct.unpack_from("<" + "f" * (width * acc["count"]), blob, at)
            neutral = NEUTRAL[width]
            rest = nodes[node].get("translation", [0, 0, 0]) if path == "T" else nodes[node].get("rotation", [0, 0, 0, 1])
            near_neutral = True
            for k in range(acc["count"]):
                v = values[k * width:(k + 1) * width]
                if path == "R":
                    off_rest, off_neutral = 1 - abs(sum(a * b for a, b in zip(v, rest))), 1 - abs(v[3])
                else:
                    off_rest, off_neutral = math.dist(v, rest), math.dist(v, neutral)
                near_neutral = near_neutral and off_neutral <= TOLERANCE[path]
                if min(off_rest, off_neutral) > TOLERANCE[path]:
                    sys.exit(f"{anim['name']} {name} {path}: key {k} {v} is neither its rest {rest} nor neutral; "
                             "the game doesn't key it, so the exporter should have written one of them")
            if near_neutral:
                continue  # already neutral (owm_survey.py keyed it so)
            sampler_copy = dict(sampler, output=neutral_accessor(width, acc["count"]))
            anim["samplers"].append(sampler_copy)  # a sampler of its own: the old one may serve other channels
            ch["sampler"] = len(anim["samplers"]) - 1
            done += 1
            if "--verbose" in sys.argv:
                print(f"    {name} {path}: rest -> neutral")
        total += done
        print(f"  {anim['name']}: {done} channels made neutral, {kept} keyed by the game kept "
              f"({raw.parent.parent.name}-{raw.parent.name}_{raw.stem})")
    print(f"{glb_path.name}: {total} channels {'would be ' if dry else ''}rewritten")
    if dry or not total:
        return
    while len(blob) % 4:
        blob.append(0)
    doc["bufferViews"].append({"buffer": 0, "byteOffset": len(blob), "byteLength": len(extra)})
    blob.extend(extra)
    doc["buffers"][0]["byteLength"] = len(blob)
    text = json.dumps(doc, separators=(",", ":")).encode("utf8")
    text += b" " * (-len(text) % 4)
    out = struct.pack("<III", magic, version, 12 + 8 + len(text) + 8 + len(blob))
    out += struct.pack("<II", len(text), 0x4E4F534A) + text + struct.pack("<II", len(blob), 0x004E4942) + bytes(blob)
    glb_path.write_bytes(out)


NEUTRAL = {3: (0.0, 0.0, 0.0), 4: (0.0, 0.0, 0.0, 1.0)}
TOLERANCE = {"T": 1e-3, "R": 1e-5}  # 1 mm; 1 - |q.q'| for about half a degree

main()
