"""Run by Blender (headless): render a hero's first-person clips as the player sees them (from the model's Camera
bone, looking forward), a strip of frames per clip, to tell the first-person clips apart.

    blender --background --factory-startup --python tools/owm_fpsurvey.py -- <model.owmdl> <entity.owentity> <out dir>
        <clip id>[,<clip id>...] [--shots=6]

Writes <out dir>/<folder>_<clip id>.png (frames side by side) and prints one FP line per clip.
"""
import math
import sys
from importlib import import_module
from pathlib import Path

import addon_utils
import bpy
from mathutils import Matrix

ADDON = "bl_ext.user_default.io_scene_owm"


def main():
    args = sys.argv[sys.argv.index("--") + 1:]
    shots = int(next((a.split("=", 1)[1] for a in args if a.startswith("--shots=")), 6))
    args = [a for a in args if not a.startswith("--")]
    model, entity, out, ids = Path(args[0]), args[1], Path(args[2]), set(args[3].split(","))
    out.mkdir(parents=True, exist_ok=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    addon_utils.enable(ADDON, default_set=True)
    reader = import_module(ADDON + ".readers.OWAnimReader")
    blanim = import_module(ADDON + ".importer.blender.BLAnimation")
    bpy.ops.import_mesh.overtools2_entity(filepath=entity)
    rig = next(o for o in bpy.data.objects if o.type == "ARMATURE")
    for o in list(bpy.data.objects):
        if o.type == "MESH" and not any(s.material for s in o.material_slots):
            bpy.data.objects.remove(o)
    bpy.context.view_layer.objects.active = rig

    scene = bpy.context.scene
    scene.render.engine = "BLENDER_WORKBENCH"
    scene.display.shading.light = "STUDIO"
    scene.display.shading.color_type = "TEXTURE"
    scene.display.shading.background_type = "VIEWPORT"
    scene.display.shading.background_color = (0.55, 0.7, 0.9)
    scene.render.resolution_x, scene.render.resolution_y = 320, 180
    cam = bpy.data.objects.new("cam", bpy.data.cameras.new("cam"))
    cam.data.lens = 15  # about Overwatch's default 103 degree horizontal field of view at 16:9
    cam.data.clip_start = 0.01
    scene.collection.objects.link(cam)
    scene.camera = cam

    for path in sorted((model.parent / "Animations").rglob("*.owanimclip")):
        if path.stem not in ids:
            continue
        folder = path.parent.relative_to(model.parent / "Animations").as_posix().replace("/", "-")
        data = reader.read(str(path))
        frames = max(1, int(data.header.duration))
        action = blanim.importAction(data, rig, False)
        rig.animation_data.action = action
        if hasattr(rig.animation_data, "action_slot") and action.slots:
            rig.animation_data.action_slot = action.slots[0]
        images = []
        for k in range(shots):
            f = round(frames * k / max(1, shots - 1))
            scene.frame_set(f)
            # the Camera bone's whole transform: its +Y is up and +Z the view direction, a Blender camera looks
            # down its -Z with +Y up, so turn 180 degrees about Y
            bone = rig.matrix_world @ rig.pose.bones["Camera"].matrix
            cam.matrix_world = bone @ Matrix.Rotation(math.pi, 4, "Y")
            scene.render.filepath = str(out / f"_tmp{k}.png")
            bpy.ops.render.render(write_still=True)
            images.append(scene.render.filepath)
        strip(images, out / f"{folder}_{path.stem}.png")
        print(f"FP {folder}_{path.stem} frames={frames} fps={data.header.fps}")
        rig.animation_data.action = None
        bpy.data.actions.remove(action)


def strip(paths, dest):
    import numpy as np
    imgs = [bpy.data.images.load(p) for p in paths]
    w, h = imgs[0].size
    sheet = np.zeros((h, w * len(imgs), 4), dtype=np.float32)
    for i, img in enumerate(imgs):
        sheet[:, i * w:(i + 1) * w] = np.array(img.pixels[:], dtype=np.float32).reshape(h, w, 4)
        bpy.data.images.remove(img)
    out = bpy.data.images.new(dest.stem, w * len(imgs), h, alpha=True)
    out.pixels = sheet.ravel()
    out.filepath_raw = str(dest)
    out.file_format = "PNG"
    out.save()
    bpy.data.images.remove(out)


main()
