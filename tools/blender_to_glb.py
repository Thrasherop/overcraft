"""Run by Blender (headless) from build_asset_pack.py: open or import a model, export it as one .glb (mesh, skeleton,
skin weights, textures, animations).

    blender --background --factory-startup --python tools/blender_to_glb.py -- <input> <output.glb>
    blender --background --factory-startup --python tools/blender_to_glb.py -- --test <output.glb>
"""
import sys
from pathlib import Path

import bpy


def main():
    args = sys.argv[sys.argv.index("--") + 1:]
    src, out = args[0], Path(args[1])
    out.parent.mkdir(parents=True, exist_ok=True)

    if src == "--test":
        # a cube, to check the pipeline without a real model
        bpy.ops.wm.read_factory_settings(use_empty=True)
        bpy.ops.mesh.primitive_cube_add(size=1)
    else:
        path = Path(src)
        ext = path.suffix.lower()
        if ext == ".blend":
            bpy.ops.wm.open_mainfile(filepath=str(path))
        else:
            bpy.ops.wm.read_factory_settings(use_empty=True)
            if ext == ".fbx":
                bpy.ops.import_scene.fbx(filepath=str(path))
            elif ext in (".gltf", ".glb"):
                bpy.ops.import_scene.gltf(filepath=str(path))
            elif ext == ".obj":
                bpy.ops.wm.obj_import(filepath=str(path))
            elif ext in (".smd", ".dmx", ".qc"):
                # needs the Blender Source Tools add-on
                bpy.ops.import_scene.smd(filepath=str(path))
            else:
                sys.exit(f"don't know how to import {ext}")

    bpy.ops.export_scene.gltf(
        filepath=str(out),
        export_format="GLB",
        export_yup=True,
        export_skins=True,
        export_animations=True,
        export_apply=False,
        export_image_format="AUTO",
    )
    meshes = sum(1 for o in bpy.data.objects if o.type == "MESH")
    armatures = sum(1 for o in bpy.data.objects if o.type == "ARMATURE")
    print(f"OVERCRAFT_EXPORT ok meshes={meshes} armatures={armatures} actions={len(bpy.data.actions)} -> {out}")


main()
