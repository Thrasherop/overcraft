"""Reads a glb: each animation's first translation key of the named node, against its rest translation. py -I glbroot.py <glb> <node>"""
import json, struct, sys
data = open(sys.argv[1], "rb").read()
jl = struct.unpack_from("<I", data, 12)[0]
gj = json.loads(data[20:20 + jl])
binoff = 20 + jl + 8
names = [n.get("name") for n in gj["nodes"]]
idx = names.index(sys.argv[2])
print("rest", gj["nodes"][idx].get("translation"))

def acc(i):
    a = gj["accessors"][i]
    bv = gj["bufferViews"][a["bufferView"]]
    off = binoff + bv.get("byteOffset", 0) + a.get("byteOffset", 0)
    n = {"SCALAR": 1, "VEC3": 3, "VEC4": 4}[a["type"]]
    return struct.unpack_from("<%df" % n, data, off)

for an in gj.get("animations", []):
    for ch in an["channels"]:
        if ch["target"].get("node") == idx and ch["target"]["path"] == "translation":
            s = an["samplers"][ch["sampler"]]
            print(an.get("name"), [round(v, 3) for v in acc(s["output"])])
