"""Run by Blender (headless): import a static DataTool model (.owmdl: an effect mesh like Kitsune Rush's torii gate,
no skeleton) with overtools' io_scene_owm add-on and export it as a .glb, with its textures. Works on files extracted
from the player's own install (never committed).

    blender --background --factory-startup --python tools/owm_static.py -- <model.owmdl> <out.glb>
"""
import sys
from pathlib import Path

import addon_utils
import bpy

ADDON = "bl_ext.user_default.io_scene_owm"


def main():
    args = sys.argv[sys.argv.index("--") + 1:]
    model, out = Path(args[0]), Path(args[1])
    out.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    addon_utils.enable(ADDON, default_set=True)
    bpy.ops.import_mesh.overtools2_model(directory=str(model.parent) + "/", files=[{"name": model.name}])
    # the add-on's shader library leaves an Icosphere behind
    for o in [o for o in bpy.data.objects if o.type == "MESH" and o.name.startswith("Icosphere")]:
        bpy.data.objects.remove(o)
    for o in bpy.data.objects:
        if o.type == "MESH":
            dims = o.dimensions
            mats = [m.name for m in o.data.materials if m]
            print(f"OWM_MESH {o.name}: {len(o.data.vertices)} vertices, {dims.x:.2f} x {dims.y:.2f} x {dims.z:.2f} m, "
                  f"materials {mats}")
    for mat in bpy.data.materials:
        if mat.use_nodes:
            images = [n.image.name for n in mat.node_tree.nodes if n.type == "TEX_IMAGE" and n.image]
            print(f"OWM_MATERIAL {mat.name}: images {images}")
    plain_materials()
    bpy.ops.export_scene.gltf(filepath=str(out), export_format="GLB", export_yup=True, export_skins=False,
                              export_animations=False, export_apply=True, export_image_format="AUTO")
    print(f"OWM_GLB {out} {out.stat().st_size // 1024} KB")


def plain_materials():
    """glTF can't carry the add-on's Overwatch shader groups: give each material the first image it uses as its
    colour (effect meshes are tinted and lit by the mod anyway)."""
    for mat in bpy.data.materials:
        if not mat.use_nodes:
            continue
        nodes, links = mat.node_tree.nodes, mat.node_tree.links
        image = next((n for n in nodes if n.type == "TEX_IMAGE" and n.image), None)
        out = next((n for n in nodes if n.type == "OUTPUT_MATERIAL"), None)
        if image is None or out is None:
            continue
        bsdf = nodes.new("ShaderNodeBsdfPrincipled")
        links.new(image.outputs[0], bsdf.inputs["Base Color"])
        links.new(bsdf.outputs[0], out.inputs[0])


main()
