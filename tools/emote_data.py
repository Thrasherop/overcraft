"""Reads a hero's emotes from the game data (offline, read-only; Overwatch must be closed) and writes what the mod
needs to play their props: src/main/resources/assets/overcraft/emotes/<hero>.json. techniques/emotes/emote_reproduction.md says why and how.

    <research venv python> tools/emote_data.py <hero> <DataTool output dir> <hero model .owmdl> [<hero .glb>]

<hero .glb> defaults to the pack's (local-assets/pack/assets/overcraft/hero_models/<hero>.glb). The emote unlocks are
found by name in DataTool's unlock list (UNLOCKS below).

Runs under the research environment's Python (pythonnet + TankLib: E:\\Overwatch Archive\\research\\kiriko-anim\\venv),
since each clip's animation effect is read through TankLib. Blender reads the models' hardpoint tables.

What it writes, per emote (named as tools/owm_survey.py names them: emote_<slug>_<n>, n in path order):
- the windows in which a clip puts the hero's own weapon away ("stow": EntityControl components with no clip, Lúcio's
  Sonic Amplifier, an entity of its own) and, for a control that names a clip, that clip on the entity it spawned;
- each clip's sounds and voice lines ("events"): each Sound component of its animation effect (a 02C, when it starts in
  the clip and how long it's held: 0 a one-shot; Killer Beam's whoosh 4.33 s in) and each VoiceStimulus (an 078 and
  when: Killer Beam's "Killer Beam! ...or whatever" 2.53 s in). The mod plays them as <hero>.emote.s<id> and
  <hero>.emote.v<id> (tools/soundmaps/emotes.txt);
- each clip's game id and length, and the spawns in its animation effect (08F): a model or an entity, the hardpoint
  it hangs on (03C id), when it shows (start, duration in the clip) and the prop's own clip if the effect names one;
  an entity's model is resolved from its definition, and a projectile entity (STUProjectileVisualComponent) is
  marked to fall when its window ends (Mic Drop);
- a clip is hers when at least 80% of the bones it animates are her skeleton's (her clips: 91-100%; the fox's, the
  cat's, the umbrella's, the pod's, the whirlwind's: 8-62%), else it is a prop's ("skeleton": "prop");
- an entity spawn that names no clip of its own takes the prop clip in the emote's folder that lasts as long as the
  window (the fox: spawn 0.767 + 3.733 s, clip E921 3.733 s; "clipStart" 0.767, when in her clip it begins) or as her
  whole clip, running with it (the pod: spawn 0.067 + 4.333 s, clip 19C45 4.4 s, her clip 4.4 s; "clipStart" 0).
  This pairing is the data's lengths agreeing, not a field (techniques/emotes/kiriko_emotes.md, open);
- each clip's GeoSetFlags timeline ("geosets"), from the emote's blend tree set (unlock 0A5 -> m_emoteBlendTreeSet
  021 -> STUAnimGameData_PoseData, one per clip by m_animationID): masks sampled evenly over the clip (at most 45), a
  bit set hiding that geoset of her model. Kneeling's first clip goes 1 -> 3 -> 7 as she puts her ofuda, Suzu and
  kunai away; Laugh is 7 (her kunai, else in her mouth);
- "pieceGeosets": which geoset bit each of her stowed weapon pieces in our .glb is. Her model's submeshes each carry
  one geoset bit (SubmeshDescriptor.m_40: 1, 2, 4 ... 128); a .glb mesh is matched to the geoset with the most
  vertices not above its own (the export splits vertices along seams, never merges them): Kiriko's ofuda (372 vertices
  in the .glb) is geoset 1 (200), the Suzu (1371) 2 (1054), the three kunai (7458) 4 (6779). Only these stowed pieces
  are hidden by the masks: bit 8, set only in Pod Pet and Friendly Kitty, is her body's geoset, which can't be what
  it means there;
- the hardpoint table: each hardpoint id used, its bone and offset, from the hero's model. One her model doesn't
  define (Kiriko's has no 000A / 000B, Overwatch's standard weapon hardpoints: not in her model's own hardpoint list
  either, read through TankLib) hangs on the bone another model puts it on, with no offset ("borrowed"): Lucio's
  000A is Weapon_R turned 180 degrees about X, and on her that turn held the Skateboard's bubble tea upside down, its
  straw never nearer her mouth than 0.55 m; on the bone itself the straw reaches 3 cm from her mouth 7.27 s into the
  ride (her sip) and the cup stays upright (tools/emote_contacts.py; techniques/emotes/kiriko_emotes.md).
"""
import json
import os
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
# DataTool's unlock list (list-unlocks, JSON) for every hero: names the emote unlocks
UNLOCKS = Path("E:/Overwatch Archive/research/kiriko-anim/unlocks/unlocks_all_heroes.json")
# a stowed piece: small and parked at the model's origin (HeroModelRenderer.isProp's PROP_SIZE and STOWED_RADIUS)
PROP_SIZE, STOWED_RADIUS = 0.6, 0.3
RESEARCH = Path(r"E:\Overwatch Archive\research\kiriko-anim\scripts")
BLENDER = Path(r"C:\Program Files\Blender Foundation\Blender 5.2\blender.exe")

HARDPOINTS_PY = r'''
import sys, json, addon_utils, importlib
from mathutils import Quaternion, Vector, Matrix, Euler
addon_utils.enable("bl_ext.user_default.io_scene_owm", default_set=True)
r = importlib.import_module("bl_ext.user_default.io_scene_owm.readers.OWModelReader")
def q(t):
    t = list(t)
    return Euler(t).to_quaternion() if len(t) == 3 else Quaternion((t[3], t[0], t[1], t[2]))
out = {}
for p in sys.argv[sys.argv.index("--") + 1:]:
    try:
        m = r.read(p)
    except Exception:
        continue
    if not m or not m.refPoseBones:
        continue
    bones = m.refPoseBones
    by = {b.name: b for b in bones}
    cache = {}
    def world(name):
        if name in cache: return cache[name]
        b = by[name]
        M = Matrix.Translation(Vector(b.pos)) @ q(b.rot).to_matrix().to_4x4()
        par = b.parent
        if isinstance(par, int) and 0 <= par < len(bones) and bones[par].name != name:
            M = world(bones[par].name) @ M
        cache[name] = M
        return M
    for e in m.empties:
        hid = e.name.replace("hardpoint_", "").upper()
        if e.hardpoint not in by or hid in out.get(p, {}):
            continue
        H = Matrix.Translation(Vector(e.position)) @ q(e.rotation).to_matrix().to_4x4()
        rel = world(e.hardpoint).inverted() @ H
        t = rel.to_translation(); rq = rel.to_quaternion()
        out.setdefault(p, {})[hid] = {"bone": e.hardpoint, "t": [round(x, 4) for x in t],
                                      "q": [round(rq.x, 4), round(rq.y, 4), round(rq.z, 4), round(rq.w, 4)]}
print("HPJSON " + json.dumps(out))
'''


# Hardpoints no extracted model defines, inferred from the emote clips, per hero (labelled; replace when the data
# shows them).
INFERRED = {
    "kiriko": {
        # Rainy Stroll's umbrella: the only prop bone her clip keeps at her right hand (about 0.13 m from the wrist)
        # while the umbrella shows; its own clip holds the handle at its origin with the shaft along +Y
        "02A9": {"bone": "bone_2BAE", "t": [0, 0, 0], "q": [0, 0, 0, 1],
                 "inferred": "the prop bone her Rainy Stroll clip keeps at her right hand"},
    },
}


# an EntityControl's action byte that puts the hero's own weapon entity away (TankLib names only Show = 1 and PlayAnim =
# 4). 204 is the only byte on the controls whose windows match GeoSetFlags bit 1 (Lúcio's weapon strap) to a mask sample
# in every emote that hides his Sonic Amplifier; Listen Up, Salty and Mic Drop carry 254 / 16 / 99 and keep it out
# (research/lucio/emotes.md). M: the byte isn't decoded.
STOW = 204

# how each emote sound plays, decoded from its Wwise events (research/emote-sounds/sounds.json, written with
# tools/soundmaps/emotes.txt): copied onto each sound event as "loop" (restarted every so many seconds while held),
# "cut" (stopped after so many seconds by its own event), "chance" / "nth" (sounds only some plays), "audience" (own:
# only the emoting player hears it, their 2D copy; others: everyone else, the 3D twin) and "fade" (seconds it fades out
# when stopped)
EMOTE_SOUNDS = Path("E:/Overwatch Archive/research/emote-sounds/sounds.json")
# sounds that duplicate another at the same moment: Dance Party's music plays on two buses at once (49D9 and 4A52) and
# its intro hit twice (49DA and B1B9, the second adding only a duck state); one of each plays (sounds.json notes: M)
DUPLICATES = {"lucio": {"4A52", "B1B9"}}

# heroes whose weapon is an entity of its own, put away by the emotes' stow windows (EntityControl STOW): their pieces
# take their geosets one for one from the model's submeshes
STOWS = {"lucio": True}

# DataTool's folder for a hero whose name isn't our id capitalised
HERO_FOLDERS = {"lucio": "Lúcio"}


def plain(name):
    """A name without its accents, lower case ("Lúcio" -> "lucio")."""
    import unicodedata
    return "".join(c for c in unicodedata.normalize("NFKD", name) if not unicodedata.combining(c)).lower()


def slug(name):
    return "".join(c if c.isalnum() else "_" for c in name.lower()).strip("_")


def hardpoints(models):
    """{model path: {hardpoint id: {bone, t, q}}} read by Blender (offsets in the bone's own space)."""
    script = ROOT / "build" / "emote_hardpoints.py"
    script.parent.mkdir(exist_ok=True)
    script.write_text(HARDPOINTS_PY, encoding="utf8")
    found = {}
    for i in range(0, len(models), 40):
        r = subprocess.run([str(BLENDER), "--background", "--factory-startup", "--python", str(script), "--"]
                           + [str(m) for m in models[i:i + 40]], capture_output=True, text=True)
        for line in r.stdout.splitlines():
            if line.startswith("HPJSON "):
                found.update(json.loads(line[7:]))
    return found


def glb_meshes(path):
    """The .glb's meshes: name -> (vertex count, bounding size, centre's distance from the origin)."""
    import struct
    data = path.read_bytes()
    n = struct.unpack_from("<I", data, 12)[0]
    doc = json.loads(data[20:20 + n])
    out = {}
    for mesh in doc["meshes"]:
        count, lo, hi = 0, [1e9] * 3, [-1e9] * 3
        for prim in mesh["primitives"]:
            a = doc["accessors"][prim["attributes"]["POSITION"]]
            count += a["count"]
            lo = [min(x, y) for x, y in zip(lo, a["min"])]
            hi = [max(x, y) for x, y in zip(hi, a["max"])]
        size = sum((y - x) ** 2 for x, y in zip(lo, hi)) ** 0.5
        centre = sum(((x + y) / 2) ** 2 for x, y in zip(lo, hi)) ** 0.5
        out[mesh["name"]] = (count, size, centre)
    return out


def main():
    hero, source, hero_model = sys.argv[1].lower(), Path(sys.argv[2]), Path(sys.argv[3])
    hero_glb = Path(sys.argv[4]) if len(sys.argv) > 4 else ROOT / "local-assets" / "pack" / "assets" / "overcraft" / "hero_models" / f"{hero}.glb"
    sys.path.insert(0, str(RESEARCH))
    import owinit as o  # stops if Overwatch or Battle.net runs
    import System
    import TankLib
    import DataTool

    def guid(text, typ):
        return o.guid(int(text, 16), typ)

    def chunks_of(fx):
        s = o.TH.OpenFile(System.UInt64(o.u64(fx)))
        return [c for c in TankLib.teChunkedData(s).Chunks if c is not None]

    def get(obj, name):
        t = obj.GetType()
        f = t.GetField(name)
        return f.GetValue(obj) if f is not None else t.GetProperty(name).GetValue(obj)

    def gid(v):
        return None if v is None or int(o.u64(v)) == 0 else "%X" % (int(o.u64(v)) & 0xFFFFFFFFFFFF)

    def entity(eid):
        """An entity definition's model, and whether it flies off as a projectile."""
        data = json.dumps(o.instances(guid(eid, 0x003)))
        model = re.search(r'"m_model": "0*([0-9A-F]+)\.00C"', data)
        return (model.group(1) if model else None), "STUProjectileVisualComponent" in data

    def chunks_of_file(g):
        s = o.TH.OpenFile(System.UInt64(int(g)))
        return list(TankLib.teChunkedData(s).Chunks)

    def model_guid(folder):
        """The full id of a model DataTool exported as <idx>.<type> (the variant with a skeleton)."""
        idx, typ = folder.split(".")
        want, typ = int(idx, 16), typ.upper().zfill(3)
        for kv in DataTool.Program.TrackedFiles:
            for x in kv.Value:
                if (int(x) & 0xFFFFFFFF) == want and o.name(x).endswith(typ):
                    if any(c is not None and c.GetType().Name == "teModelChunk_Skeleton" for c in chunks_of_file(x)):
                        return x
        return None

    skeletons, clip_bone_cache = {}, {}

    def skeleton_ids(model):
        """A model's skeleton bone ids (16-bit), empty for a model with none."""
        if model not in skeletons:
            ids = set()
            try:
                for c in chunks_of_file(o.u64(guid(model, 0x00C))):
                    if c is not None and c.GetType().Name == "teModelChunk_Skeleton":
                        v = c.GetType().GetField("IDs").GetValue(c)
                        ids = set(int(x) & 0xFFFF for x in v) if v is not None else set()
            except Exception:
                pass
            skeletons[model] = ids
        return skeletons[model]

    def clip_bones(cid):
        """The bone ids (16-bit) a clip animates."""
        if cid not in clip_bone_cache:
            stream = o.TH.OpenFile(System.UInt64(o.u64(guid(cid, 0x006))))
            clip_bone_cache[cid] = set(int(b) & 0xFFFF for b in TankLib.teAnimation(stream).BoneList)
        return clip_bone_cache[cid]

    def walk(node, found):
        if isinstance(node, dict):
            if node.get("$type") == "STUAnimGameData_PoseData" and node.get("m_geoSetFlags"):
                found["%X" % node["m_animationID"]] = [g["m_CFB09B58"] for g in node["m_geoSetFlags"]]
            for v in node.values():
                walk(v, found)
        elif isinstance(node, list):
            for v in node:
                walk(v, found)

    # each emote's GeoSetFlags timelines, by clip id: unlock (by name) -> blend tree set -> pose data
    geosets = {}
    unlocks = json.loads(UNLOCKS.read_text(encoding="utf8"))
    emote_unlocks = []

    def collect(node):
        # the list nests unlocks in groups (level, loot box, other)
        if isinstance(node, dict):
            if node.get("Type") == "Emote":
                emote_unlocks.append(node)
            for v in node.values():
                collect(v)
        elif isinstance(node, list):
            for v in node:
                collect(v)

    collect(next((v for k, v in unlocks.items() if plain(k) == hero), {}))
    for u in emote_unlocks:
        root = o.instances(o.u64(guid(u["GUID"].split(".")[0], 0x0A5)))[0]
        bts = root.get("m_emoteBlendTreeSet")
        if bts:
            found = {}
            walk(o.instances(o.u64(guid(bts.split(".")[0], 0x021))), found)
            geosets[slug(u["Name"])] = found

    # which geoset bit each stowed piece of our .glb is (see above)
    piece_geosets = {}
    g = model_guid(hero_model.parent.name)
    if g is not None and hero_glb.exists():
        per = {}
        for c in chunks_of_file(g):
            if c is not None and c.GetType().Name == "teModelChunk_RenderMesh":
                best = {}
                for d in c.GetType().GetField("SubmeshDescriptors").GetValue(c):
                    key = (int(str(d.m_40)), int(str(d.Material)))
                    best[key] = max(best.get(key, 0), int(str(d.VerticesToDraw)))
                for (bit, _), verts in best.items():
                    per[bit] = per.get(bit, 0) + verts
        if any(c.GetType().Name == "teModelChunk_RenderMesh" for c in chunks_of_file(g) if c is not None) and STOWS.get(hero):
            # a hero whose weapon is an entity of its own (Lúcio): the .glb's Submesh_<i> is the model's submesh i (the
            # vertex counts agree one for one), so each piece takes its own geoset. His geoset 1 is the cable on his
            # weapon arm, weighted to Weapon_R: the masks hide it with the stowed Sonic Amplifier
            # (research/lucio/emotes.md); else it stretched to wherever the stowed weapon's bone was parked
            # the model's variants share its index (LOD sets): the one exported is the one whose submesh i has the .glb's
            # Submesh_i's vertex count (the export splits a few along seams: 4506 -> 4510, never merges)
            meshes = glb_meshes(hero_glb)
            by_index = {int(m.group(1)): c for n, (c, _, _) in meshes.items() if (m := re.match(r"Submesh_(\d+)\.", n))}

            def fits(d):
                return len(d) == len(by_index) and all(
                    i in by_index and v <= by_index[i] <= v * 1.02 + 2 for i, (_, v) in enumerate(d))
            descriptors = []
            want = int(hero_model.parent.name.split(".")[0], 16)
            for kv in DataTool.Program.TrackedFiles:
                for x in kv.Value:
                    if (int(x) & 0xFFFFFFFF) == want and o.name(x).endswith("00C"):
                        for c in chunks_of_file(x):
                            if c is not None and c.GetType().Name == "teModelChunk_RenderMesh":
                                d = [(int(str(m.m_40)), int(str(m.VerticesToDraw)))
                                     for m in c.GetType().GetField("SubmeshDescriptors").GetValue(c)]
                                if fits(d):
                                    descriptors = d
            for name, (count, size, centre) in meshes.items():
                m = re.match(r"Submesh_(\d+)\.", name)
                if m and int(m.group(1)) < len(descriptors):
                    bit, verts = descriptors[int(m.group(1))]
                    if bit >= 0:
                        piece_geosets[name] = bit
            print("submeshes (geoset, vertices):", descriptors)
        for name, (count, size, centre) in glb_meshes(hero_glb).items():
            if size <= PROP_SIZE and centre <= STOWED_RADIUS:
                under = [(v, bit) for bit, v in per.items() if v <= count]
                if under:
                    piece_geosets[name] = max(under)[1]
        print("geosets (vertices):", per, "pieces:", piece_geosets)

    emotes_dir = source / "Unlocks" / "Heroes" / HERO_FOLDERS.get(hero, hero.capitalize()) / "Emote"
    out = {"hero": hero, "source": "game data via TankLib (tools/emote_data.py)", "emotes": {}, "hardpoints": {},
           "pieceGeosets": piece_geosets}
    used = set()
    bone_ids = {}
    found = []
    for emote in sorted(d for d in emotes_dir.glob("*/*") if (d / "Animations").is_dir()):
        name = slug(emote.name)
        clips = {}
        others = []
        for n, path in enumerate(sorted((emote / "Animations").rglob("*.owanimclip")), 1):
            cid = path.stem.lstrip("0") or "0"
            s = o.TH.OpenFile(System.UInt64(o.u64(guid(cid, 0x006))))
            anim = TankLib.teAnimation(s)
            header = anim.Header
            info = {"id": cid, "duration": round(float(str(header.Duration)), 4), "bones": int(str(header.BoneCount)),
                    "folder": path.relative_to(emote / "Animations").parts[0], "spawns": [], "events": [], "stow": []}
            bone_ids[cid] = set(int(b) for b in anim.BoneList)
            clips[f"emote_{name}_{n}"] = info
            fx = header.Effect
            if gid(fx) is None:
                continue
            last = None
            controls = []
            for c in chunks_of(fx):
                kind = c.GetType().Name
                if kind == "teEffectChunkComponent":
                    h = get(c, "Header")
                    hp = int(str(get(h, "Unk"))) & 0xFFFFFFFFFFFF
                    last = ("%04X" % hp if hp else None, round(float(str(get(c, "StartTime"))), 4),
                            round(float(str(get(c, "Duration"))), 4))
                    continue
                if last is None or not kind.startswith("teEffectComponent"):
                    continue
                hp, start, dur = last
                last = None
                h = get(c, "Header")
                fields = {f.Name: f.GetValue(h) for f in h.GetType().GetFields()}
                if kind == "teEffectComponentModel":
                    clip = gid(fields.get("Animation"))
                    spawn = {"model": gid(fields.get("Model")), "hardpoint": hp, "start": start, "duration": dur,
                             "clip": clip, "clipStart": start if clip else None}
                    info["spawns"].append(spawn)
                elif kind == "teEffectComponentEntity":
                    model, projectile = entity(gid(fields.get("Entity")))
                    spawn = {"model": model, "entity": gid(fields.get("Entity")), "hardpoint": hp, "start": start,
                             "duration": dur, "clip": None, "falls": projectile}
                    info["spawns"].append(spawn)
                elif kind == "teEffectComponentVoiceStimulus":
                    info["events"].append({"voice": gid(fields.get("VoiceStimulus")), "at": start})
                    continue
                elif kind == "teEffectComponentEntityControl":
                    clip = gid(fields.get("Animation"))
                    action = fields.get("Action")
                    if clip is not None:
                        controls.append((clip, start))
                    elif gid(fields.get("Identifier")) is None and action is not None and int(System.Convert.ToInt32(action)) == STOW:
                        # no clip and no identifier: the hero's own weapon entity put away over the window
                        info["stow"].append([start, round(start + dur, 4)])
                    continue
                elif kind == "teEffectComponentSound":
                    # a sound (02C), from start, held for duration (0: a one-shot); the pack's <hero>.emote.s<id> event
                    # (tools/soundmaps/emotes.txt)
                    info["events"].append({"sound": gid(fields.get("Sound")), "at": start, "duration": dur, "hardpoint": hp})
                    continue
                else:
                    continue
                if hp:
                    used.add(hp)
            info["_controls"] = controls
        for info in clips.values():
            for clip, start in info.pop("_controls", []):
                # a control's clip plays on an entity this clip spawned: the one whose skeleton holds the clip's bones
                # (Lúcio's A Suave Gift spawns F559 and F698 together; its 7-bone B4D9 fits F559's), among those
                # spawned when the control starts if any (research/lucio/emotes.md: every pair checked that way)
                spawned = [x for x in info["spawns"] if x.get("entity")]
                same = [x for x in spawned if abs(x["start"] - start) < 0.02] or spawned
                if not same:
                    continue
                bones = clip_bones(clip)
                target = max(reversed(same), key=lambda x: len(bones & skeleton_ids(x["model"])) if x["model"] else -1)
                target["clip"], target["clipStart"], target["clipBy"] = clip, start, "control"
        for c in clips.values():
            if c["id"] in geosets.get(name, {}):
                c["geosets"] = geosets[name][c["id"]]
        out["emotes"][name] = {"clips": clips}
        found.append((name, clips))
    # her skeleton: every bone her full-body clips animate
    most = max(c["bones"] for _, clips in found for c in clips.values())
    hers = set().union(*(bone_ids[c["id"]] for _, clips in found for c in clips.values() if c["bones"] >= 0.9 * most))
    for name, clips in found:
        for c in clips.values():
            ids = bone_ids[c["id"]]
            c["skeleton"] = "hers" if not ids or len(ids & hers) >= 0.8 * len(ids) else "prop"
        others = {c["id"]: c["duration"] for c in clips.values() if c["skeleton"] == "prop"}
        for clip in clips.values():
            for spawn in clip["spawns"]:
                if spawn.get("entity") and spawn["clip"] is None:
                    # a prop clip as long as the window (starts with it), else as long as her clip (runs with it)
                    for length, begins in ((spawn["duration"], spawn["start"]), (clip["duration"], 0.0)):
                        match = [i for i, d in others.items() if abs(d - length) < 0.02]
                        # only clips whose bones the prop's skeleton holds (Dance Party's amplifier on the booth: 5677
                        # fits its 17 bones, the booth's own 5678 of the same length doesn't)
                        fitting = [i for i in match if bone_ids[i]
                                   and {b & 0xFFFF for b in bone_ids[i]} <= skeleton_ids(spawn["model"])]
                        match = fitting or match
                        if match:
                            spawn["clip"], spawn["clipStart"], spawn["clipBy"] = match[0], begins, "length"
                            if len(match) > 1:
                                # more than one prop clip that long: the first is taken, and said so
                                spawn["clipAmbiguous"] = match
                                print("ambiguous clip for", name, spawn["model"], match)
                            break
        print(name, {k: [(s["model"], s["hardpoint"], s["start"], s["duration"], s["clip"], s.get("clipStart"))
                         for s in v["spawns"]] for k, v in clips.items() if v["spawns"]})
    # hardpoints: the hero's own model first, then any other model that defines one it lacks
    table = hardpoints([hero_model]).get(str(hero_model), {})
    missing = sorted(used - set(table))
    if missing:
        models = sorted(source.rglob("*.owmdl"))
        for path, defs in hardpoints(models).items():
            for hid in list(missing):
                if hid in defs:
                    # the bone, not the other model's offset (see above)
                    table[hid] = {"bone": defs[hid]["bone"], "t": [0, 0, 0], "q": [0, 0, 0, 1],
                                  "borrowed": f"{Path(path).name} hangs it on this bone (its offset t {defs[hid]['t']} "
                                              f"q {defs[hid]['q']} not taken)"}
                    missing.remove(hid)
    # what no model defines: inferred from the clips, and said so (techniques/emotes/emote_reproduction.md)
    for hid in list(missing):
        if hid in INFERRED.get(hero, {}):
            table[hid] = INFERRED[hero][hid]
            missing.remove(hid)
    out["hardpoints"] = {k: table[k] for k in sorted(used) if k in table}
    out["unresolvedHardpoints"] = missing
    how = json.loads(EMOTE_SOUNDS.read_text(encoding="utf8")).get(hero, {}) if EMOTE_SOUNDS.exists() else {}
    for emote in out["emotes"].values():
        for clip in emote["clips"].values():
            clip["events"] = [e for e in clip["events"] if e.get("sound") not in DUPLICATES.get(hero, set())]
            for e in clip["events"]:
                h = how.get(e["sound"]) if e.get("sound") else None
                if not h:
                    continue
                if h.get("loops") and h.get("loop_length"):
                    e["loop"] = h["loop_length"]
                for ours, theirs in (("cut", "cut_at"), ("chance", "chance"), ("nth", "every_nth_play"), ("fade", "stop_fade")):
                    if h.get(theirs):
                        e[ours] = h[theirs]
                if h.get("audience") in ("own", "others"):
                    e["audience"] = h["audience"]
    target = ROOT / "src" / "main" / "resources" / "assets" / "overcraft" / "emotes" / f"{hero}.json"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(out, indent=1), encoding="utf8")
    print("wrote", target, "unresolved hardpoints:", missing)


if __name__ == "__main__":
    main()
