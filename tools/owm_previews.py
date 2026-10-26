"""Run by Blender (headless): render a quick preview of each DataTool model (.owmdl) in a folder, to tell them apart
(DataTool names models by id only).

    blender --background --factory-startup --python tools/owm_previews.py -- <Models dir> <out dir> [<id> ...]
"""
import math
import sys
from pathlib import Path

import addon_utils
import bpy
from mathutils import Vector

ADDON = "bl_ext.user_default.io_scene_owm"


def main():
    args = sys.argv[sys.argv.index("--") + 1:]
    models, out, only = Path(args[0]), Path(args[1]), set(args[2:])
    out.mkdir(parents=True, exist_ok=True)
    for path in sorted(models.glob("*/*.owmdl")):
        if only and not any(path.stem.startswith(i) for i in only):
            continue
        bpy.ops.wm.read_factory_settings(use_empty=True)
        addon_utils.enable(ADDON, default_set=True)
        bpy.ops.import_mesh.overtools2_model(directory=str(path.parent) + "/", files=[{"name": path.name}])
        meshes = [o for o in bpy.data.objects if o.type == "MESH"]
        if not meshes:
            print(f"OWM_PREVIEW {path.stem} no meshes")
            continue
        corners = [o.matrix_world @ Vector(c) for o in meshes for c in o.bound_box]
        lo = Vector(tuple(min(c[i] for c in corners) for i in range(3)))
        hi = Vector(tuple(max(c[i] for c in corners) for i in range(3)))
        size = max(hi - lo)
        scene = bpy.context.scene
        scene.render.engine = "BLENDER_WORKBENCH"
        scene.display.shading.color_type = "TEXTURE"
        scene.render.resolution_x = scene.render.resolution_y = 400
        cam = bpy.data.objects.new("cam", bpy.data.cameras.new("cam"))
        cam.data.type = "ORTHO"
        cam.data.ortho_scale = size * 1.2
        scene.collection.objects.link(cam)
        scene.camera = cam
        d = Vector((math.cos(-math.pi / 2 + 0.6), math.sin(-math.pi / 2 + 0.6), 0.3)).normalized()
        cam.location = (lo + hi) / 2 + d * size * 4
        cam.rotation_euler = ((lo + hi) / 2 - cam.location).to_track_quat("-Z", "Y").to_euler()
        scene.render.filepath = str(out / f"{path.stem}.png")
        bpy.ops.render.render(write_still=True)
        tris = sum(len(p.vertices) - 2 for o in meshes for p in o.data.polygons)
        rig = next((o for o in bpy.data.objects if o.type == "ARMATURE"), None)
        print(f"OWM_PREVIEW {path.stem} tris={tris} bones={len(rig.data.bones) if rig else 0} "
              f"size={tuple(round(v, 2) for v in hi - lo)}")


main()
