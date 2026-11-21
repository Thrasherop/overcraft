"""Run by Blender (headless) from build_asset_pack.py freak: the freak list's Tree of Life model (owner, 2026-11-20:
Upturned_Foot.stl) into one .glb the prop renderer draws. An STL is a bare mesh, in millimetres with Z up and no UVs,
colours or textures: it's cut to about TARGET_TRIS triangles (1.09 M in the owner's file: too many to draw each frame),
scaled to the tree's height (LifeweaverStats.TRUNK_HEIGHT, 6.5 m), set on the root (its base centred, its lowest point at 0), and
given one solid colour as a tiny texture (the renderer colours from a texture).

    blender --background --factory-startup --python tools/freak_model.py -- <model.stl> <out.glb> [<preview.png>]
"""
import sys
from pathlib import Path

import bpy
from mathutils import Vector

TARGET_TRIS = 40000
HEIGHT = 6.5
# skin (ours: the file has no colour)
COLOUR = (0.86, 0.66, 0.55, 1.0)


def main():
    args = sys.argv[sys.argv.index("--") + 1:]
    src, out = Path(args[0]), Path(args[1])
    bpy.ops.wm.read_factory_settings(use_empty=True)
    ext = src.suffix.lower()
    if ext == ".stl":
        bpy.ops.wm.stl_import(filepath=str(src))
    elif ext == ".obj":
        bpy.ops.wm.obj_import(filepath=str(src))
    elif ext in (".glb", ".gltf"):
        bpy.ops.import_scene.gltf(filepath=str(src))
    elif ext == ".fbx":
        bpy.ops.import_scene.fbx(filepath=str(src))
    else:
        sys.exit(f"don't know how to import {ext}")
    meshes = [o for o in bpy.data.objects if o.type == "MESH"]
    bpy.ops.object.select_all(action="DESELECT")
    for o in meshes:
        o.select_set(True)
    bpy.context.view_layer.objects.active = meshes[0]
    if len(meshes) > 1:
        bpy.ops.object.join()
    o = bpy.context.view_layer.objects.active
    bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)

    tris = sum(len(p.vertices) - 2 for p in o.data.polygons)
    if tris > TARGET_TRIS:
        d = o.modifiers.new("cut", "DECIMATE")
        d.ratio = TARGET_TRIS / tris
        bpy.ops.object.modifier_apply(modifier=d.name)

    cs = [o.matrix_world @ Vector(c) for c in o.bound_box]
    lo = Vector([min(c[i] for c in cs) for i in range(3)])
    hi = Vector([max(c[i] for c in cs) for i in range(3)])
    s = HEIGHT / (hi.z - lo.z)
    # its foot on the root: the middle of its lowest tenth (the owner's foot leans far out over its base, so the box's
    # middle would plant it off its ankle)
    base = [v.co for v in o.data.vertices if v.co.z < lo.z + (hi.z - lo.z) * 0.1]
    cx = (min(v.x for v in base) + max(v.x for v in base)) / 2
    cy = (min(v.y for v in base) + max(v.y for v in base)) / 2
    o.location = (-cx * s, -cy * s, -lo.z * s)
    o.scale = (s, s, s)
    bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)
    bpy.ops.object.shade_smooth()

    # one colour: a 4x4 image, every UV on it
    img = bpy.data.images.new("freak_skin", 4, 4)
    img.pixels = list(COLOUR) * 16
    img.pack()
    mat = bpy.data.materials.new("freak_skin")
    mat.use_nodes = True
    tex = mat.node_tree.nodes.new("ShaderNodeTexImage")
    tex.image = img
    bsdf = mat.node_tree.nodes["Principled BSDF"]
    mat.node_tree.links.new(tex.outputs["Color"], bsdf.inputs["Base Color"])
    o.data.materials.clear()
    o.data.materials.append(mat)
    if not o.data.uv_layers:
        o.data.uv_layers.new(name="UVMap")
    for loop in o.data.uv_layers[0].data:
        loop.uv = (0.5, 0.5)

    out.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.export_scene.gltf(filepath=str(out), export_format="GLB", export_yup=True, export_image_format="AUTO")
    cs = [o.matrix_world @ Vector(c) for c in o.bound_box]
    size = [round(max(c[i] for c in cs) - min(c[i] for c in cs), 2) for i in range(3)]
    print(f"OVERCRAFT_EXPORT ok tris={sum(len(p.vertices) - 2 for p in o.data.polygons)} size_m={size} -> {out}")
    if len(args) > 2:
        preview(Path(args[2]), o)


def preview(png, o):
    """A quick look from the front and a little above (Eevee), to check it stands the right way up."""
    scene = bpy.context.scene
    cam = bpy.data.objects.new("cam", bpy.data.cameras.new("cam"))
    scene.collection.objects.link(cam)
    cam.location = (14, -14, 9)
    direction = Vector((0, 0, HEIGHT / 2)) - cam.location
    cam.rotation_euler = direction.to_track_quat("-Z", "Y").to_euler()
    scene.camera = cam
    sun = bpy.data.objects.new("sun", bpy.data.lights.new("sun", "SUN"))
    sun.rotation_euler = (0.8, 0.2, 0.6)
    scene.collection.objects.link(sun)
    scene.render.resolution_x, scene.render.resolution_y = 640, 480
    scene.render.filepath = str(png)
    bpy.ops.render.render(write_still=True)


main()
