"""Run by Blender (headless): import a DataTool model (.owmdl) and its animation clips (.owanimclip) with overtools'
io_scene_owm add-on, then report on each clip so the gameplay ones (idle, run, jump...) can be told apart, and/or
export everything as one .glb. Works on files extracted from the player's own install (never committed).

    blender --background --factory-startup --python tools/owm_survey.py -- <model.owmdl> <out dir> [--survey] [--glb]

--survey: <out dir>/clips.json (per clip: folder, frames, fps, root travel, which body parts move) and
          <out dir>/thumbs/<clip>.png (the pose at 0, 1/3 and 2/3 of the clip, side by side).
--glb:    <out dir>/<model>.glb with the clips as glTF animations named <folder>_<clip id>.
--keep=<file>: only these clips in the .glb (one <folder>_<clip id> per line, optionally "= our name" after it, to
          rename it). Exporting all of a hero's ~330 clips takes Blender's glTF exporter well over half an hour.
--only=<id>,<id>: just these clips (by clip id), to try things on a few first.
--shots=<n>: poses per thumbnail strip (default 3).  --side: view from her side instead of the front.
--emotes=<dir>: also import the hero's emotes from DataTool's extract-unlocks "<Hero>|emote=*" (<dir> =
          Unlocks/Heroes/<Hero>/Emote): each emote's clips (<group>/<name>/Animations/**) as emote_<name>_<n>, in id
          order, kept in the .glb alongside --keep's clips. Their props (a bike, a skateboard) are not imported.
--entity=<file.owentity>: import this entity instead of the bare model (its model look decides which meshes get
          materials: Gameplay1P's gives the first-person arms). Clips still come from <model>'s Animations folder.
"""
import json
import math
import sys
from pathlib import Path

import addon_utils
import bpy
from mathutils import Vector

ADDON = "bl_ext.user_default.io_scene_owm"


def main():
    args = sys.argv[sys.argv.index("--") + 1:]
    flags = {a for a in args if a.startswith("--")}
    model, out = Path(args[0]), Path(args[1])
    out.mkdir(parents=True, exist_ok=True)

    bpy.ops.wm.read_factory_settings(use_empty=True)
    addon_utils.enable(ADDON, default_set=True)
    owm = sys.modules[ADDON]
    from importlib import import_module
    reader = import_module(ADDON + ".readers.OWAnimReader")
    blanim = import_module(ADDON + ".importer.blender.BLAnimation")

    entity = next((a.split("=", 1)[1] for a in args if a.startswith("--entity=")), None)
    if entity:
        # an entity (e.g. Gameplay1P) brings its own model look: the first-person arms only get materials from it
        bpy.ops.import_mesh.overtools2_entity(filepath=entity)
    else:
        bpy.ops.import_mesh.overtools2_model(directory=str(model.parent) + "/", files=[{"name": model.name}])
    # an entity's child entities come in as rigs of their own (Lúcio's Gameplay1P brings his Sonic Amplifier on its
    # hardpoint), and with a second rig the glTF exporter wrote no clips at all. The hero's rig is the one with the
    # most bones; the others go: a weapon is exported from its own model (tools/clipmaps/lucio_weapon.txt)
    rigs = sorted((o for o in bpy.data.objects if o.type == "ARMATURE"), key=lambda o: len(o.data.bones), reverse=True)
    rig = rigs[0]
    for other in rigs[1:]:
        for o in [o for o in bpy.data.objects if o.type == "MESH" and (o.parent == other or any(
                m.type == "ARMATURE" and m.object == other for m in o.modifiers))]:
            bpy.data.objects.remove(o)
        bpy.data.objects.remove(other)
    meshes = [o for o in bpy.data.objects if o.type == "MESH"]
    print(f"OWM_MODEL meshes={len(meshes)} bones={len(rig.data.bones)} "
          f"tris={sum(len(p.vertices) - 2 for o in meshes for p in o.data.polygons)}")
    bpy.context.view_layer.objects.active = rig
    rig.select_set(True)

    clips = sorted((model.parent / "Animations").rglob("*.owanimclip"))
    only = next((a.split("=", 1)[1].split(",") for a in args if a.startswith("--only=")), None)
    if only:
        clips = [c for c in clips if c.stem in only]
    report = []
    for path in clips:
        folder = path.parent.relative_to(model.parent / "Animations").as_posix().replace("/", "-")
        data = reader.read(str(path))
        if data is None:
            continue
        action = blanim.importAction(data, rig, False)
        action.name = f"{folder}_{path.stem}"
        action.use_fake_user = True
        if folder.startswith("4"):
            keyed = {b.name: (b.positions.keyframeCount > 0, b.rotations.keyframeCount > 0) for b in data.bones}
            neutral_unkeyed(rig, action, keyed, data.header.duration)
        report.append({"name": action.name, "folder": folder, "id": path.stem, "frames": data.header.duration,
                       "fps": data.header.fps, "bones": len(data.bones)})
        # clips come at 30 or 60 fps; the glTF exporter turns frames into seconds at the one scene rate, so every
        # clip goes onto a 30 fps timeline
        if abs(data.header.fps - EXPORT_FPS) > 0.01:
            retime(action, EXPORT_FPS / data.header.fps)
    emotes = next((a.split("=", 1)[1] for a in args if a.startswith("--emotes=")), None)
    if emotes:
        for emote in sorted(d for d in Path(emotes).glob("*/*") if (d / "Animations").is_dir()):
            name = "".join(c if c.isalnum() else "_" for c in emote.name.lower()).strip("_")
            for n, path in enumerate(sorted((emote / "Animations").rglob("*.owanimclip")), 1):
                data = reader.read(str(path))
                if data is None:
                    continue
                action = blanim.importAction(data, rig, False)
                action.name = f"emote_{name}_{n}"
                action.use_fake_user = True
                report.append({"name": action.name, "folder": "emote", "id": path.stem, "frames": data.header.duration,
                               "fps": data.header.fps, "bones": len(data.bones)})
                if abs(data.header.fps - EXPORT_FPS) > 0.01:
                    retime(action, EXPORT_FPS / data.header.fps)
    bpy.context.scene.render.fps = EXPORT_FPS
    print(f"OWM_CLIPS {len(report)}")

    if "--survey" in flags:
        shots = int(next((a.split("=", 1)[1] for a in args if a.startswith("--shots=")), 3))
        side = "--side" in flags
        survey(rig, meshes, report, out, shots, side)
        (out / "clips.json").write_text(json.dumps(report, indent=1), encoding="utf8")
    if "--glb" in flags:
        keep = next((a.split("=", 1)[1] for a in args if a.startswith("--keep=")), None)
        if keep:
            names = {}
            for line in Path(keep).read_text(encoding="utf8").splitlines():
                line = line.split("#")[0].strip()
                if line:
                    clip, _, ours = (s.strip() for s in line.partition("="))
                    names[clip] = ours or clip
            for action in list(bpy.data.actions):
                if action.name in names:
                    action.name = names[action.name]
                elif not action.name.startswith("emote_"):
                    bpy.data.actions.remove(action)
            print(f"OWM_KEEP {len(bpy.data.actions)} of {len(report)} clips")
        gltf_materials()
        # meshes with no material are alternate/LOD shells the game doesn't draw for this look
        for o in [o for o in bpy.data.objects if o.type == "MESH" and not any(s.material for s in o.material_slots)]:
            bpy.data.objects.remove(o)
        # and anything not on the skeleton (the add-on's shader library leaves an Icosphere behind)
        for o in [o for o in bpy.data.objects if o.type == "MESH" and (o.parent is None or o.parent.type != "ARMATURE")]:
            bpy.data.objects.remove(o)
        glb = out / (model.stem + ".glb")
        bpy.ops.export_scene.gltf(filepath=str(glb), export_format="GLB", export_yup=True, export_skins=True,
                                  export_animations=True, export_animation_mode="ACTIONS", export_apply=False,
                                  export_image_format="AUTO")
        print(f"OWM_GLB {glb} {glb.stat().st_size // 1024} KB")


def survey(rig, meshes, report, out, shots=3, side=False):
    """Per clip: how far the root and each body part travel, sampled every few frames; plus a thumbnail strip."""
    scene = bpy.context.scene
    bones = rig.pose.bones
    # body parts by their rest position (Overwatch bone names are numbers): the lowest bones are the feet, the
    # outermost the hands, the highest the head
    rest = {b.name: rig.matrix_world @ b.head_local for b in rig.data.bones}
    zs = sorted(p.z for p in rest.values())
    top, bottom = zs[-1], zs[0]
    height = top - bottom
    feet = [n for n, p in rest.items() if p.z < bottom + 0.12 * height]
    hands = sorted(rest, key=lambda n: -abs(rest[n].x))[:6]
    head = [n for n, p in rest.items() if p.z > top - 0.08 * height]
    roots = [b.name for b in rig.data.bones if b.parent is None]
    all_bones = list(rest)

    thumbs = out / "thumbs"
    thumbs.mkdir(exist_ok=True)
    setup_camera(scene, meshes, height)

    for entry in report:
        action = bpy.data.actions[entry["name"]]
        rig.animation_data_create()
        rig.animation_data.action = action
        if hasattr(rig.animation_data, "action_slot") and action.slots:
            rig.animation_data.action_slot = action.slots[0]
        frames = max(1, round(entry["frames"] * EXPORT_FPS / entry["fps"]))  # on the 30 fps timeline
        samples = sorted({0, frames} | set(range(0, frames, max(1, frames // 12))))

        def world(names, f):
            scene.frame_set(f)
            return [rig.matrix_world @ bones[n].head for n in names if n in bones]

        def travel(names):
            first = world(names, samples[0])
            paths = [0.0] * len(first)
            prev = first
            for f in samples[1:]:
                cur = world(names, f)
                for i, (a, b) in enumerate(zip(prev, cur)):
                    paths[i] += (b - a).length
                prev = cur
            return round(max(paths, default=0.0), 3)

        start, end = world(roots, samples[0]), world(roots, samples[-1])
        entry["root_move"] = [round(v, 3) for v in (end[0] - start[0])] if start else None
        # the whole body's move (Overwatch moves the character through a bone below the root)
        body0, body1 = centroid(world(all_bones, samples[0])), centroid(world(all_bones, samples[-1]))
        entry["body_move"] = [round(v, 3) for v in (body1 - body0)]
        entry["feet_travel"] = travel(feet)
        entry["hands_travel"] = travel(hands)
        entry["head_travel"] = travel(head)
        scene.frame_set(0)
        entry["height_f0"] = round(max(p.z for p in world(head, 0)) - bottom, 3) if head else None

        strip = []
        for k, f in enumerate(sorted({frames * i // shots for i in range(shots)})):
            follow(scene, centroid(world(all_bones, f)), height, side)
            scene.render.filepath = str(thumbs / f"_{k}.png")
            bpy.ops.render.render(write_still=True)
            strip.append(scene.render.filepath)
        join_strip(strip, thumbs / f"{entry['name']}.png")
        rig.animation_data.action = None
        print(f"OWM_SURVEY {entry['name']} frames={frames} body={entry['body_move']} feet={entry['feet_travel']} "
              f"hands={entry['hands_travel']}")


EXPORT_FPS = 30


def neutral_unkeyed(rig, action, keyed, frames):
    """Additive clips (the 4-* folders) hold deltas, not poses. The glTF exporter writes every bone, filling the ones a
    clip doesn't key with their rest pose, which as a delta would throw the skeleton apart (a wrist pushed out by the
    forearm's length): key every channel the clip lacks at a neutral delta instead (no offset, no turn from the
    parent), so it adds nothing. {@code keyed}: bone name -> (position keyed, rotation keyed)."""
    import bpy as _bpy
    _bpy.ops.object.mode_set(mode="POSE")
    rig.animation_data.action = action
    if hasattr(rig.animation_data, "action_slot") and action.slots:
        rig.animation_data.action_slot = action.slots[0]
    for bone in rig.pose.bones:
        bone.matrix_basis.identity()
    _bpy.context.view_layer.update()
    for bone in rig.pose.bones:
        has_pos, has_rot = keyed.get(bone.name, (False, False))
        if (has_pos and has_rot) or bone.parent is None:
            continue
        bone.matrix = bone.parent.matrix.copy()
        loc, rot = bone.location.copy(), bone.rotation_quaternion.copy()
        bone.matrix_basis.identity()
        for frame in (0, max(1, int(frames))):
            if not has_pos:
                bone.location = loc
                bone.keyframe_insert("location", frame=frame)
            if not has_rot:
                bone.rotation_quaternion = rot
                bone.keyframe_insert("rotation_quaternion", frame=frame)
        bone.matrix_basis.identity()
    rig.animation_data.action = None
    _bpy.ops.object.mode_set(mode="OBJECT")


def fcurves(action):
    if hasattr(action, "layers") and action.layers:
        for layer in action.layers:
            for strip in layer.strips:
                for bag in strip.channelbags:
                    yield from bag.fcurves
    else:
        yield from action.fcurves


def retime(action, scale):
    """Stretches an action's keys in time (a 60 fps clip onto a 30 fps timeline: scale 0.5)."""
    for fc in fcurves(action):
        for k in fc.keyframe_points:
            k.co.x *= scale
            k.handle_left.x *= scale
            k.handle_right.x *= scale
        fc.update()


def gltf_materials():
    """The add-on builds Overwatch shader groups, which glTF can't carry: rebuild each material as a Principled BSDF
    from the image feeding the group's colour input (and its alpha, for cut-out cloth and hair)."""
    for mat in bpy.data.materials:
        if not mat.use_nodes:
            continue
        nodes, links = mat.node_tree.nodes, mat.node_tree.links
        color = alpha = None
        for link in links:
            if link.from_node.type != "TEX_IMAGE" or link.to_node.type != "GROUP":
                continue
            socket = link.to_socket.name.lower()
            if color is None and any(k in socket for k in ("albedo", "color", "diffuse")) and "emission" not in socket:
                color = link.from_node
            elif alpha is None and "alpha" in socket:
                alpha = link.from_node
        if color is None:
            print(f"OWM_MATERIAL {mat.name}: no colour texture (inputs: "
                  f"{sorted({l.to_socket.name for l in links if l.to_node.type == 'GROUP'})})")
            continue
        out = next(n for n in nodes if n.type == "OUTPUT_MATERIAL")
        bsdf = nodes.new("ShaderNodeBsdfPrincipled")
        links.new(color.outputs[0], bsdf.inputs["Base Color"])
        if alpha is not None:
            links.new(alpha.outputs[1], bsdf.inputs["Alpha"])
        links.new(bsdf.outputs[0], out.inputs[0])


def centroid(points):
    return sum(points, Vector()) / max(1, len(points))


def setup_camera(scene, meshes, height):
    scene.render.engine = "BLENDER_WORKBENCH"
    scene.display.shading.light = "STUDIO"
    scene.display.shading.color_type = "MATERIAL"
    scene.render.resolution_x = 220
    scene.render.resolution_y = 300
    cam = bpy.data.objects.new("cam", bpy.data.cameras.new("cam"))
    cam.data.type = "ORTHO"
    cam.data.ortho_scale = height * 1.5
    scene.collection.objects.link(cam)
    scene.camera = cam


def follow(scene, center, height, side=False):
    """Points the camera at the body, from the front at three-quarters (Overwatch models face -Y after import), or
    from her left side (to see legs swing in walk cycles)."""
    cam = scene.camera
    angle = 0.0 if side else -math.pi / 2 + 0.6
    d = Vector((math.cos(angle), math.sin(angle), 0.15)).normalized()
    cam.location = center + d * height * 4
    cam.rotation_euler = (center - cam.location).to_track_quat("-Z", "Y").to_euler()


def join_strip(paths, dest):
    images = [bpy.data.images.load(p) for p in paths]
    w, h = images[0].size
    strip = bpy.data.images.new(dest.stem, w * len(images), h, alpha=True)
    pixels = [0.0] * (w * len(images) * h * 4)
    for i, img in enumerate(images):
        src = list(img.pixels)
        for y in range(h):
            row = src[y * w * 4:(y + 1) * w * 4]
            start = (y * w * len(images) + i * w) * 4
            pixels[start:start + w * 4] = row
    strip.pixels = pixels
    strip.filepath_raw = str(dest)
    strip.file_format = "PNG"
    strip.save()
    for img in images + [strip]:
        bpy.data.images.remove(img)


main()
