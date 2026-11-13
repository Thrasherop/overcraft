"""Play of the Game in the client data (offline, read-only, build 154088): walks every statescript graph's instances,
inline ones included, for the action that carries a PlayOfTheGameData (STU_0FC6DAEA), and reads each highlight type it
names (STU_C25281C3). Usage: potg_scan2.py <out json> [max graphs]"""
import sys, json, time
sys.path.insert(0, r"E:\Overwatch Archive\research\kiriko-anim\scripts")
import owinit as o
import System, DataTool
import TankLib.STU as S
import TankLib.STU.Types as T
from DataTool.Helper import IO, STUHelper

TF = DataTool.Program.TrackedFiles
STUI = System.Type.GetType("TankLib.STU.STUInstance, TankLib")
FIELDS = {}


def fields(t):
    if t not in FIELDS:
        FIELDS[t] = [f for f in t.GetFields() if not f.FieldType.IsPrimitive and f.FieldType != System.String("").GetType()]
    return FIELDS[t]


def walk(x, seen, out, depth=0):
    if x is None or depth > 40:
        return
    t = x.GetType()
    if t.IsArray:
        for e in x:
            walk(e, seen, out, depth + 1)
        return
    if not STUI.IsAssignableFrom(t):
        return
    h = System.Runtime.CompilerServices.RuntimeHelpers.GetHashCode(x)
    if h in seen:
        return
    seen.add(h)
    if t.Name == "STU_0FC6DAEA":
        out.append(x)
    for f in fields(t):
        try:
            walk(f.GetValue(x), seen, out, depth + 1)
        except Exception:
            pass


def var(v):
    if v is None:
        return None
    d = {"type": v.GetType().Name}
    for f in v.GetType().GetFields():
        try:
            x = f.GetValue(v)
            if x is None:
                continue
            if isinstance(x, (int, float, str, bool)):
                d[f.Name] = x
            elif hasattr(x, "GUID"):
                d[f.Name] = o.name(int(x.GUID))
            elif STUI.IsAssignableFrom(x.GetType()):
                d[f.Name] = var(x)
        except Exception:
            pass
    return d


graphs = list(TF[System.UInt16(0x01B)])
limit = int(sys.argv[2]) if len(sys.argv) > 2 else len(graphs)
print("graphs", len(graphs), flush=True)
found, types = [], {}
t0 = time.time()
for i, g in enumerate(graphs[:limit]):
    try:
        stu = STUHelper.OpenSTUSafe(System.UInt64(o.u64(g)))
        if stu is None:
            continue
        out, seen = [], set()
        for inst in stu.Instances:
            walk(inst, seen, out)
        for a in out:
            d = a.m_POTGData
            ht = int(d.m_371085C1.GUID) if d is not None and d.m_371085C1 is not None else 0
            found.append({"graph": o.name(g), "type": o.name(ht) if ht else None, "id": var(d.m_id), "value": var(d.m_value),
                          "targets": var(d.m_targets), "description": var(d.m_description), "enum": str(d.m_41B10FF2)})
            if ht:
                types[ht] = types.get(ht, 0) + 1
    except Exception as ex:
        pass
    if i % 500 == 0:
        print(i, round(time.time() - t0), "s", len(found), flush=True)
typeinfo = {}
for ht in types:
    try:
        inst = STUHelper.GetInstance[T.STU_C25281C3](System.UInt64(ht))
        typeinfo[o.name(ht)] = {"uses": types[ht], "description": IO.GetString(inst.m_description) if inst else None,
                               "list": [o.name(int(x.GUID)) for x in (inst.m_91590545 or [])] if inst else None,
                               "f69A20070": inst.m_69A20070 if inst else None, "f5C712614": inst.m_5C712614 if inst else None}
    except Exception as ex:
        typeinfo[o.name(ht)] = {"uses": types[ht], "error": str(ex)}
json.dump({"actions": found, "types": typeinfo}, open(sys.argv[1], "w"), indent=1)
print(json.dumps(typeinfo, indent=1))
print("actions", len(found))
