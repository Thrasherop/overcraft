"""Run by Blender (headless) from build_asset_pack.py: open or import a model, export it as one .glb (mesh, skeleton,
skin weights, textures, animations).

    blender --background --factory-startup --python tools/blender_to_glb.py -- <input> <output.glb>
    blender --background --factory-startup --python tools/blender_to_glb.py -- --test <output.glb>
    ... -- <input> <output.glb> [<preview.png>] [--stopgap-anims]   (placeholder clips: tools/stopgap_anims.py)
"""
import sys
from pathlib import Path

import bpy


def main():
    args = sys.argv[sys.argv.index("--") + 1:]
    stopgap = "--stopgap-anims" in args
    args = [a for a in args if a != "--stopgap-anims"]
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
            elif ext == ".mdl":
                # compiled Source models (SFM ports): needs the SourceIO add-on; finds materials/ next to models/
                import addon_utils
                addon_utils.enable("SourceIO")
                # (it reads directory + files like Blender's file browser; filepath alone imports nothing)
                bpy.ops.sourceio.mdl(directory=str(path.parent) + "/", files=[{"name": path.name}], filepath=str(path),
                                     import_textures=True, write_qc=False, discover_resources=True)
            elif ext in (".smd", ".dmx", ".qc"):
                # needs the Blender Source Tools add-on
                bpy.ops.import_scene.smd(filepath=str(path))
            else:
                sys.exit(f"don't know how to import {ext}")

    if stopgap:
        sys.path.insert(0, str(Path(__file__).resolve().parent))
        import stopgap_anims
        rig = next(o for o in bpy.data.objects if o.type == "ARMATURE")
        stopgap_anims.add_clips(rig)

    bpy.ops.export_scene.gltf(
        filepath=str(out),
        export_animation_mode="NLA_TRACKS" if stopgap else "ACTIONS",
        export_format="GLB",
        export_yup=True,
        export_skins=True,
        export_animations=True,
        export_apply=False,
        export_image_format="AUTO",
    )
    meshes = [o for o in bpy.data.objects if o.type == "MESH"]
    armatures = sum(1 for o in bpy.data.objects if o.type == "ARMATURE")
    tris = sum(len(p.vertices) - 2 for o in meshes for p in o.data.polygons)
    bones = sum(len(o.data.bones) for o in bpy.data.objects if o.type == "ARMATURE")
    print(f"OVERCRAFT_EXPORT ok meshes={len(meshes)} tris={tris} armatures={armatures} bones={bones} "
          f"images={len(bpy.data.images)} actions={len(bpy.data.actions)} -> {out}")
    if len(args) > 2:
        preview(Path(args[2]), meshes)


def preview(png, meshes):
    """Renders the model from the front and at three-quarters, side by side, to look at."""
    import math
    from mathutils import Vector

    png.parent.mkdir(parents=True, exist_ok=True)
    corners = [o.matrix_world @ Vector(c) for o in meshes for c in o.bound_box]
    lo = Vector((min(c.x for c in corners), min(c.y for c in corners), min(c.z for c in corners)))
    hi = Vector((max(c.x for c in corners), max(c.y for c in corners), max(c.z for c in corners)))
    center = (lo + hi) / 2
    size = max(hi - lo)
    print(f"OVERCRAFT_BOUNDS height={hi.z - lo.z:.3f} width={hi.x - lo.x:.3f} depth={hi.y - lo.y:.3f}")

    scene = bpy.context.scene
    scene.render.engine = "BLENDER_EEVEE"
    scene.render.resolution_x = 900
    scene.render.resolution_y = 1100
    scene.render.film_transparent = False
    world = scene.world or bpy.data.worlds.new("World")
    scene.world = world
    world.use_nodes = True
    world.node_tree.nodes["Background"].inputs[0].default_value = (0.08, 0.1, 0.16, 1)
    world.node_tree.nodes["Background"].inputs[1].default_value = 0.6

    for name, energy, loc in (("key", 900, (2, -3, 3)), ("fill", 300, (-3, -2, 2)), ("rim", 500, (0, 3, 3))):
        light = bpy.data.objects.new(name, bpy.data.lights.new(name, "AREA"))
        light.data.energy = energy * size * size
        light.data.size = size
        light.location = center + Vector(loc) * size
        light.rotation_euler = (center - light.location).to_track_quat("-Z", "Y").to_euler()
        scene.collection.objects.link(light)

    cam = bpy.data.objects.new("cam", bpy.data.cameras.new("cam"))
    cam.data.lens = 70
    scene.collection.objects.link(cam)
    scene.camera = cam
    shots = []
    # SFM ports face -Y after import: the front, then a three-quarter view
    for i, angle in enumerate((-math.pi / 2, -math.pi / 2 + 0.7)):
        d = Vector((math.cos(angle), math.sin(angle), 0.12)).normalized()
        cam.location = center + d * size * 2.6
        cam.rotation_euler = (center - cam.location).to_track_quat("-Z", "Y").to_euler()
        path = png.with_name(f"{png.stem}_{i}.png")
        scene.render.filepath = str(path)
        bpy.ops.render.render(write_still=True)
        shots.append(path)
    print("OVERCRAFT_PREVIEW " + " ".join(str(s) for s in shots))


main()
