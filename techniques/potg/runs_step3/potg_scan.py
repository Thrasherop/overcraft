"""Play of the Game in the client data (offline, read-only, build 154088): every statescript graph's
STU_0FC6DAEA action (it carries a PlayOfTheGameData: highlight type, id, value, targets, description), and each
highlight type it names (STU_C25281C3: description string, its list, two floats). Usage: potg_scan.py <out json>"""
import sys, json, time
sys.path.insert(0, r"E:\Overwatch Archive\research\kiriko-anim\scripts")
import owinit as o
import System, DataTool
import TankLib.STU as S
import TankLib.STU.Types as T
from DataTool.Helper import IO, STUHelper

TF = DataTool.Program.TrackedFiles


def var(v):
    """A config var as text: its type and, for the common constant kinds, its value."""
    if v is None:
        return None
    out = {"type": v.GetType().Name}
    for f in v.GetType().GetFields():
        try:
            x = f.GetValue(v)
            if isinstance(x, (int, float, str, bool)):
                out[f.Name] = x
            elif x is not None and hasattr(x, "GUID"):
                out[f.Name] = o.name(int(x.GUID)) if int(x.GUID) else 0
            elif x is not None and x.GetType().Name.startswith("teStructuredDataAssetRef"):
                out[f.Name] = o.name(int(x.GUID))
            elif x is not None and x.GetType().IsSubclassOf(S.STUInstance):
                out[f.Name] = var(x)
        except Exception as ex:
            out[f.Name] = "?"
    return out


graphs = list(TF[System.UInt16(0x01B)])
print("graphs", len(graphs), flush=True)
found = []
types = {}
t0 = time.time()
for i, g in enumerate(graphs):
    try:
        stu = STUHelper.OpenSTUSafe(System.UInt64(o.u64(g)))
        if stu is None:
            continue
        for inst in stu.Instances:
            if inst is not None and inst.GetType().Name == "STU_0FC6DAEA":
                d = inst.m_POTGData
                ht = int(d.m_371085C1.GUID) if d is not None and d.m_371085C1 is not None else 0
                found.append({"graph": o.name(g), "type": o.name(ht) if ht else None, "id": var(d.m_id),
                              "value": var(d.m_value), "targets": var(d.m_targets), "description": var(d.m_description),
                              "enum": int(d.m_41B10FF2)})
                if ht:
                    types.setdefault(ht, 0)
                    types[ht] += 1
    except Exception as ex:
        pass
    if i % 1000 == 0:
        print(i, round(time.time() - t0), "s", len(found), flush=True)
typeinfo = {}
for ht in types:
    try:
        inst = STUHelper.GetInstance[T.STU_C25281C3](System.UInt64(ht))
        desc = IO.GetString(inst.m_description) if inst is not None else None
        typeinfo[o.name(ht)] = {"uses": types[ht], "description": desc,
                               "list": [o.name(int(x.GUID)) for x in (inst.m_91590545 or [])] if inst is not None else None,
                               "f69A20070": inst.m_69A20070 if inst is not None else None,
                               "f5C712614": inst.m_5C712614 if inst is not None else None}
    except Exception as ex:
        typeinfo[o.name(ht)] = {"uses": types[ht], "error": str(ex)}
json.dump({"actions": found, "types": typeinfo}, open(sys.argv[1], "w"), indent=1)
print(json.dumps(typeinfo, indent=1))
print("actions", len(found))
