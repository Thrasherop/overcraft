"""Adds to a .glb the animation clips another .glb of the same model has and it lacks, each channel moved to the node of
the same name (a channel whose node it doesn't have is dropped). For a prop two heroes' emotes share: Sombra's Browsing
and Sigma's Magic Pumpkins both carry their things on the carrier 1024E, each with a clip of its own (B9A7, AE48), and a
rebuild of one hero's props would otherwise drop the other's clip (tools/build_asset_pack.py props).

    py tools/merge_glb_clips.py <into.glb> <from.glb>

Rewrites <into.glb> in place (its binary chunk grows by the copied clips' data); prints the clips added.
"""
import json
import struct
import sys
from pathlib import Path


def read(path):
    data = Path(path).read_bytes()
    magic, _, _ = struct.unpack_from("<III", data, 0)
    if magic != 0x46546C67:
        sys.exit(f"{path}: not a glb")
    json_len, _ = struct.unpack_from("<II", data, 12)
    doc = json.loads(data[20:20 + json_len])
    pos = 20 + json_len
    bin_len, _ = struct.unpack_from("<II", data, pos)
    return doc, bytearray(data[pos + 8:pos + 8 + bin_len])


def main():
    into, src = sys.argv[1], sys.argv[2]
    doc, blob = read(into)
    other, other_blob = read(src)
    have = {a.get("name") for a in doc.get("animations", [])}
    node_of = {n.get("name"): i for i, n in enumerate(doc["nodes"])}
    added = []
    for anim in other.get("animations", []):
        if anim.get("name") in have:
            continue
        samplers, channels, remap = [], [], {}

        def copy_accessor(i):
            a = dict(other["accessors"][i])
            view = other["bufferViews"][a.pop("bufferView")]
            start = view.get("byteOffset", 0)
            chunk = other_blob[start:start + view["byteLength"]]
            while len(blob) % 4:
                blob.append(0)
            doc["bufferViews"].append({"buffer": 0, "byteOffset": len(blob), "byteLength": len(chunk)})
            blob.extend(chunk)
            a["bufferView"] = len(doc["bufferViews"]) - 1
            doc["accessors"].append(a)
            return len(doc["accessors"]) - 1

        for ch in anim["channels"]:
            name = other["nodes"][ch["target"]["node"]].get("name")
            if name not in node_of:
                continue
            s = ch["sampler"]
            if s not in remap:
                sampler = dict(anim["samplers"][s])
                sampler["input"] = copy_accessor(sampler["input"])
                sampler["output"] = copy_accessor(sampler["output"])
                samplers.append(sampler)
                remap[s] = len(samplers) - 1
            channels.append({"sampler": remap[s], "target": {"node": node_of[name], "path": ch["target"]["path"]}})
        if channels:
            doc.setdefault("animations", []).append({"name": anim.get("name"), "samplers": samplers, "channels": channels})
            added.append(anim.get("name"))
    while len(blob) % 4:
        blob.append(0)
    doc["buffers"][0]["byteLength"] = len(blob)
    text = json.dumps(doc, separators=(",", ":")).encode("utf8")
    text += b" " * (-len(text) % 4)
    out = struct.pack("<III", 0x46546C67, 2, 12 + 8 + len(text) + 8 + len(blob))
    out += struct.pack("<II", len(text), 0x4E4F534A) + text + struct.pack("<II", len(blob), 0x004E4942) + bytes(blob)
    Path(into).write_bytes(out)
    print(f"{into}: added {added}")


if __name__ == "__main__":
    main()
