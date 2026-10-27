"""Checks a held prop's hardpoint against what the emote does: how near a point on the prop (in the prop's own space)
comes to the hero's mouth over each clip, and whether the prop's +Y is up then. A contact the emote must make (a sip, a
bite) tells a right offset from a wrong one. Reads the hero's exported .glb (its clips and bone frames; Overwatch's
own frames: techniques/animation/animation_import.md); needs numpy.

    py tools/emote_contacts.py <hero .glb> <bone> <x,y,z on the prop> <qx,qy,qz,qw offset> <clip> [<clip> ...]

Kiriko's Skateboard tea (000A on Weapon_R; the straw's top 0.3 m up the cup):
    py tools/emote_contacts.py local-assets/pack/assets/overcraft/hero_models/kiriko.glb Weapon_R 0,0.3,0 0,0,0,1 emote_skateboard_8
    -> 0.031 m at 7.27 s (her sip), cup up 0.78; with Lucio's offset 1,0,0,0: 0.555 m, upside down.
The mouth is taken as 8 cm in front of the Head bone (ours, rough).
"""
import json, struct, sys, math
import numpy as np
glb, bone, tip, hq = sys.argv[1], sys.argv[2], [float(x) for x in sys.argv[3].split(",")], [float(x) for x in sys.argv[4].split(",")]
d = open(glb, "rb").read()
n = struct.unpack_from("<I", d, 12)[0]
doc = json.loads(d[20:20 + n])
binoff = 20 + n + 8
def acc(i):
    a = doc["accessors"][i]; v = doc["bufferViews"][a["bufferView"]]
    comp = {"SCALAR": 1, "VEC3": 3, "VEC4": 4}[a["type"]]
    off = binoff + v.get("byteOffset", 0) + a.get("byteOffset", 0)
    return np.frombuffer(d, dtype=np.float32, count=a["count"] * comp, offset=off).reshape(a["count"], comp)
nodes = doc["nodes"]
parent = {}
for i, nd in enumerate(nodes):
    for c in nd.get("children", []): parent[c] = i
by = {nd.get("name"): i for i, nd in enumerate(nodes)}
def qmat(q):
    x, y, z, w = q
    return np.array([[1-2*(y*y+z*z), 2*(x*y-z*w), 2*(x*z+y*w)], [2*(x*y+z*w), 1-2*(x*x+z*z), 2*(y*z-x*w)], [2*(x*z-y*w), 2*(y*z+x*w), 1-2*(x*x+y*y)]])
anims = {a["name"]: a for a in doc["animations"]}
def sample(anim, t):
    out = {}
    for ch in anim["channels"]:
        s = anim["samplers"][ch["sampler"]]
        ts = acc(s["input"])[:, 0]; vs = acc(s["output"])
        i = max(0, min(len(ts) - 1, int(np.searchsorted(ts, t, side="right")) - 1))
        j = min(i + 1, len(ts) - 1)
        f = 0 if j == i else min(1, max(0, (t - ts[i]) / (ts[j] - ts[i])))
        v = vs[i] * (1 - f) + vs[j] * f
        out[(ch["target"]["node"], ch["target"]["path"])] = v
    return out
def world(node, pose):
    M = np.eye(4)
    chain = []
    k = node
    while k is not None:
        chain.append(k); k = parent.get(k)
    for k in reversed(chain):
        nd = nodes[k]
        t = pose.get((k, "translation"), nd.get("translation", [0, 0, 0]))
        r = pose.get((k, "rotation"), nd.get("rotation", [0, 0, 0, 1]))
        L = np.eye(4); L[:3, :3] = qmat(r / np.linalg.norm(r)); L[:3, 3] = t
        M = M @ L
    return M
H = np.eye(4); H[:3, :3] = qmat(hq)
for clip in sys.argv[5:]:
    a = anims[clip]
    end = max(acc(s["input"])[-1, 0] for s in a["samplers"])
    best = None
    for k in range(int(end * 15) + 1):
        t = k / 15
        pose = sample(a, t)
        W = world(by[bone], pose) @ H
        p = W @ np.array(tip + [1])
        head = world(by["Head"], pose)
        mouth = head @ np.array([0, 0.0, 0.08, 1])  # in front of the head bone (ours, rough)
        dist = np.linalg.norm(p[:3] - mouth[:3])
        up = (W[:3, :3] @ np.array([0, 1, 0]))[1]
        if best is None or dist < best[0]: best = (dist, t, up)
    print(clip, "closest tip-to-mouth %.3f m at %.2f s (prop +Y world up-component %.2f)" % best)
