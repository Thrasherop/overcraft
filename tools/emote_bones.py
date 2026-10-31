"""Where a hero's bones are over an emote clip, from the exported .glb (Overwatch's own frames): each bone's place in
her model's space every so often, and the distances between them. For checking two props on two hardpoints against each
other (Ana's teabag on 000B going into her teacup on 00C9) and a hardpoint against a body part, before trusting a frame.

    py tools/emote_bones.py <hero .glb> <clip> <bone> [<bone> ...] [--every=0.2]

Prints, per time, each bone's place (x, y, z: +X her left, +Y up, +Z ahead) and the distance from the first bone to
each of the others. Same reading of the .glb as tools/emote_contacts.py.
"""
import json
import struct
import sys

import numpy as np

args = [a for a in sys.argv[1:] if not a.startswith("--")]
every = float(next((a.split("=", 1)[1] for a in sys.argv[1:] if a.startswith("--every=")), "0.2"))
glb, clip, bones = args[0], args[1], args[2:]
d = open(glb, "rb").read()
n = struct.unpack_from("<I", d, 12)[0]
doc = json.loads(d[20:20 + n])
binoff = 20 + n + 8


def acc(i):
    a = doc["accessors"][i]
    v = doc["bufferViews"][a["bufferView"]]
    comp = {"SCALAR": 1, "VEC3": 3, "VEC4": 4}[a["type"]]
    off = binoff + v.get("byteOffset", 0) + a.get("byteOffset", 0)
    return np.frombuffer(d, dtype=np.float32, count=a["count"] * comp, offset=off).reshape(a["count"], comp)


nodes = doc["nodes"]
parent = {c: i for i, nd in enumerate(nodes) for c in nd.get("children", [])}
by = {nd.get("name"): i for i, nd in enumerate(nodes)}


def qmat(q):
    x, y, z, w = q
    return np.array([[1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w)],
                     [2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w)],
                     [2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y)]])


def sample(anim, t):
    out = {}
    for ch in anim["channels"]:
        s = anim["samplers"][ch["sampler"]]
        ts = acc(s["input"])[:, 0]
        vs = acc(s["output"])
        i = max(0, min(len(ts) - 1, int(np.searchsorted(ts, t, side="right")) - 1))
        j = min(i + 1, len(ts) - 1)
        f = 0 if j == i else min(1, max(0, (t - ts[i]) / (ts[j] - ts[i])))
        out[(ch["target"]["node"], ch["target"]["path"])] = vs[i] * (1 - f) + vs[j] * f
    return out


def world(node, pose):
    chain = []
    k = node
    while k is not None:
        chain.append(k)
        k = parent.get(k)
    M = np.eye(4)
    for k in reversed(chain):
        nd = nodes[k]
        t = pose.get((k, "translation"), nd.get("translation", [0, 0, 0]))
        r = pose.get((k, "rotation"), nd.get("rotation", [0, 0, 0, 1]))
        L = np.eye(4)
        L[:3, :3] = qmat(np.asarray(r) / np.linalg.norm(r))
        L[:3, 3] = t
        M = M @ L
    return M


anim = next(a for a in doc["animations"] if a["name"] == clip)
end = max(acc(s["input"])[-1, 0] for s in anim["samplers"])
t = 0.0
while t <= end + 1e-6:
    pose = sample(anim, t)
    places = [world(by[b], pose)[:3, 3] for b in bones]
    line = "%5.2f s  " % t + "  ".join("%s (%.2f %.2f %.2f)" % (b, *p) for b, p in zip(bones, places))
    if len(places) > 1:
        line += "  | " + "  ".join("%s-%s %.3f" % (bones[0], b, np.linalg.norm(places[0] - p)) for b, p in zip(bones[1:], places[1:]))
    print(line)
    t += every
