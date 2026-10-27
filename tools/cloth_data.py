"""Reads a hero's cloth from the game data (offline, read-only; Overwatch must be closed) and writes what the mod needs
to move it as Overwatch does: src/main/resources/assets/overcraft/cloth/<hero>.json.

    <research venv python> tools/cloth_data.py <hero> <hero model .owmdl>

Runs under the research environment's Python (pythonnet + TankLib, like tools/emote_data.py); Blender reads the
model's bone names.

What Overwatch does (Kiriko, 2026-10-27): her coat is Blizzard's "Domino" cloth (the model's CLTH chunk, which TankLib
parses). Its 342 points (6 pieces: 45, 45, 98, 98, 28, 28) each follow one cloth bone (278 cloth_ bones; the other 64
are the pieces' top rows) and are placed by up to four driver bones with weights: her body (Hips, Thigh, Knee, Foot,
Arm, Elbow, Spine_3, bone_0054) and 38 coat bones of their own (bone_14CF-14EC, bone_2FD8-2FE0) that her clips
animate by hand: about 90 clips each, 48-51 of her emote clips. The cloth_ bones themselves are keyed in no clip (2 of
278, cloth_0180/0181). So a cloth bone's place is its rest place carried by its drivers, as skinning carries a vertex.

What it writes: for each cloth bone, its drivers by name and weight ("drivers"), and its vertical parent's bone ("up",
the point above it in the cloth's grid, or null for one hanging from a top-row point).
"""
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RESEARCH = Path(r"E:\Overwatch Archive\research\kiriko-anim\scripts")
BLENDER = Path(r"C:\Program Files\Blender Foundation\Blender 5.2\blender.exe")

BONES_PY = r'''
import sys, json, addon_utils, importlib
addon_utils.enable("bl_ext.user_default.io_scene_owm", default_set=True)
r = importlib.import_module("bl_ext.user_default.io_scene_owm.readers.OWModelReader")
m = r.read(sys.argv[sys.argv.index("--") + 1])
print("BONES " + json.dumps([b.name for b in m.refPoseBones]))
'''


def bone_names(model):
    """The model's bones in skeleton order (the order the cloth's bone indices use)."""
    script = ROOT / "build" / "cloth_bones.py"
    script.parent.mkdir(exist_ok=True)
    script.write_text(BONES_PY, encoding="utf8")
    r = subprocess.run([str(BLENDER), "--background", "--factory-startup", "--python", str(script), "--", str(model)],
                       capture_output=True, text=True)
    for line in r.stdout.splitlines():
        if line.startswith("BONES "):
            return json.loads(line[6:])
    sys.exit("couldn't read the bone names: " + r.stderr[-2000:])


def main():
    hero, hero_model = sys.argv[1].lower(), Path(sys.argv[2])
    names = bone_names(hero_model)
    sys.path.insert(0, str(RESEARCH))
    import owinit as o  # stops if Overwatch or Battle.net runs
    import System
    import TankLib
    import DataTool
    from System.Reflection import BindingFlags
    F = BindingFlags.Public | BindingFlags.NonPublic | BindingFlags.Instance

    def field(obj, name):
        return obj.GetType().GetField(name, F).GetValue(obj)

    def chunks(g):
        return list(TankLib.teChunkedData(o.TH.OpenFile(System.UInt64(int(g)))).Chunks)

    # the model's full id (DataTool exported it as <idx>.<type>): the variant with a skeleton
    idx, typ = hero_model.parent.name.split(".")
    model = None
    for kv in DataTool.Program.TrackedFiles:
        for x in kv.Value:
            if model is None and (int(x) & 0xFFFFFFFF) == int(idx, 16) and o.name(x).endswith(typ.upper().zfill(3)):
                if any(c is not None and c.GetType().Name == "teModelChunk_Skeleton" for c in chunks(x)):
                    model = x
    if model is None:
        sys.exit(f"no model {hero_model.parent.name} with a skeleton")
    cloth = next((c for c in chunks(model) if c is not None and c.GetType().Name == "teModelChunk_Cloth"), None)
    if cloth is None:
        sys.exit("the model has no cloth")
    bones = {}
    for piece in field(cloth, "Pieces"):
        driver_bone = {int(d): i for i, d in enumerate(field(piece, "BoneToDriverMap")) if int(d) >= 0}
        vertices = list(field(piece, "Vertices"))
        followers = [int(str(field(v, "FollowerBone"))) for v in vertices]
        for k, v in enumerate(vertices):
            follower = followers[k]
            if follower < 0:
                continue
            drivers = []
            for w in field(v, "DriverWeights"):
                weight = float(str(field(w, "Weight")))
                if weight > 0:
                    drivers.append([names[driver_bone[int(str(field(w, "DriverIdx")))]], round(weight, 5)])
            up = followers[int(str(field(v, "VerticalParent")))]
            bones[names[follower]] = {"drivers": drivers, "up": names[up] if up >= 0 and up != follower else None}
    out = {"hero": hero, "source": "game data via TankLib (tools/cloth_data.py): the model's Domino cloth", "bones": bones}
    target = ROOT / "src" / "main" / "resources" / "assets" / "overcraft" / "cloth" / f"{hero}.json"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(out, indent=1), encoding="utf8")
    used = sorted({d for b in bones.values() for d, _ in b["drivers"]})
    print("wrote", target, len(bones), "cloth bones; drivers:", used)


if __name__ == "__main__":
    main()
