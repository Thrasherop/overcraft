"""Which asset type holds the highlight types (STU_C25281C3), and what each says (offline, read-only, build 154088)."""
import sys, json
sys.path.insert(0, r"E:\Overwatch Archive\research\kiriko-anim\scripts")
import owinit as o
import System, DataTool
from DataTool.Helper import STUHelper, IO
TF = DataTool.Program.TrackedFiles
hits = {}
for kv in TF:
    t = int(kv.Key)
    files = list(kv.Value)
    if not files:
        continue
    for g in files[:2]:
        try:
            stu = STUHelper.OpenSTUSafe(System.UInt64(o.u64(g)))
            if stu is None or stu.Instances.Count == 0 or stu.Instances[0] is None:
                continue
            name = stu.Instances[0].GetType().Name
            if name in ("STU_C25281C3", "STU_6BE90C5C") or "Highlight" in name or "POTG" in name or "PlayOf" in name:
                hits.setdefault(hex(t), {"root": name, "count": len(files)})
        except Exception:
            pass
print(json.dumps(hits, indent=1))
out = []
for th, h in hits.items():
    for g in list(TF[System.UInt16(int(th, 16))]):
        stu = STUHelper.OpenSTUSafe(System.UInt64(o.u64(g)))
        r = stu.Instances[0]
        d = {"file": o.name(g), "root": r.GetType().Name}
        for f in r.GetType().GetFields():
            v = f.GetValue(r)
            try:
                if v is None:
                    continue
                if isinstance(v, (int, float, str, bool)):
                    d[f.Name] = v
                elif hasattr(v, "GUID"):
                    s = None
                    try:
                        s = IO.GetString(v)
                    except Exception:
                        pass
                    d[f.Name] = s if s else o.name(int(v.GUID))
                elif v.GetType().IsArray:
                    d[f.Name] = [o.name(int(x.GUID)) if hasattr(x, "GUID") else (x if isinstance(x, (int, float, str)) else x.GetType().Name) for x in v]
                else:
                    d[f.Name] = v.GetType().Name
            except Exception as ex:
                d[f.Name] = "?"
        out.append(d)
json.dump(out, open(sys.argv[1], "w"), indent=1)
print(json.dumps(out, indent=1)[:6000])
