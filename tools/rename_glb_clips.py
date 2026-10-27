"""Renames animation clips inside a .glb in place (its JSON chunk only; the binary data is untouched), so a clip map
fix doesn't need a full Blender re-export. Keep tools/clipmaps/ in step: it's what a re-export uses.

    py tools/rename_glb_clips.py <file.glb> <old>=<new> [<old>=<new> ...]

Renames are applied at once (a=b b=a swaps). Fails if a name is missing or two clips would share a name.
"""
import json
import struct
import sys
from pathlib import Path


def main():
    path = Path(sys.argv[1])
    renames = dict(arg.split("=", 1) for arg in sys.argv[2:])
    data = path.read_bytes()
    magic, version, _ = struct.unpack_from("<III", data, 0)
    if magic != 0x46546C67:
        sys.exit(f"{path}: not a glb")
    json_len, json_type = struct.unpack_from("<II", data, 12)
    doc = json.loads(data[20:20 + json_len])
    rest = data[20 + json_len:]
    names = [a.get("name") for a in doc.get("animations", [])]
    missing = [old for old in renames if old not in names]
    if missing:
        sys.exit(f"{path}: no clip named {missing}")
    for anim in doc["animations"]:
        anim["name"] = renames.get(anim["name"], anim["name"])
    after = [a["name"] for a in doc["animations"]]
    clashes = sorted({n for n in after if after.count(n) > 1})
    if clashes:
        sys.exit(f"{path}: two clips would be named {clashes}")
    chunk = json.dumps(doc, separators=(",", ":")).encode("utf8")
    chunk += b" " * (-len(chunk) % 4)
    out = struct.pack("<III", magic, version, 12 + 8 + len(chunk) + len(rest)) + struct.pack("<II", len(chunk), json_type)
    path.write_bytes(out + chunk + rest)
    for old, new in renames.items():
        print(f"{path.name}: {old} -> {new}")


if __name__ == "__main__":
    main()
