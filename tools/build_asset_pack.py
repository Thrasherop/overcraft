"""Builds the local Overwatch asset pack from model files on this machine. The pack is never committed or shipped
(CLAUDE.md): it lives in local-assets/ (gitignored) and the mod reads it from outside the jar.

    py tools/build_asset_pack.py add kiriko <model file>    convert a model (.blend/.fbx/.gltf/.glb/.obj/.smd) into the pack
    py tools/build_asset_pack.py zip                        local-assets/overcraft-assets.zip, for a real install:
                                                            copy it to <.minecraft>/overcraft/overcraft-assets.zip
    py tools/build_asset_pack.py test                       check the Blender pipeline with a cube

The dev client (gradlew runClient) reads local-assets/pack/ directly.
"""
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LOCAL = ROOT / "local-assets"
PACK = LOCAL / "pack"
MODELS = PACK / "assets" / "overcraft" / "hero_models"
HEROES = {"kiriko", "lucio", "sombra", "illari", "juno", "sojourn"}

BLENDER_CANDIDATES = [
    Path(r"C:\Program Files\Blender Foundation\Blender 5.2\blender.exe"),
    Path(r"C:\Program Files\Blender Foundation\Blender 5.1\blender.exe"),
    Path(r"C:\Program Files\Blender Foundation\Blender 5.0\blender.exe"),
]


def blender():
    found = shutil.which("blender")
    if found:
        return found
    for candidate in BLENDER_CANDIDATES:
        if candidate.exists():
            return str(candidate)
    sys.exit("Blender not found: install it or put blender on PATH")


def convert(src, out):
    cmd = [blender(), "--background", "--factory-startup", "--python", str(ROOT / "tools" / "blender_to_glb.py"), "--", src, str(out)]
    result = subprocess.run(cmd, capture_output=True, text=True)
    report = [line for line in result.stdout.splitlines() if "OVERCRAFT_EXPORT" in line or "Error" in line]
    print("\n".join(report) or result.stdout[-2000:])
    if result.returncode != 0 or not out.exists():
        print(result.stderr[-2000:])
        sys.exit("conversion failed")


def write_mcmeta():
    PACK.mkdir(parents=True, exist_ok=True)
    (PACK / "pack.mcmeta").write_text(
        '{"pack": {"description": "Overcraft: local Overwatch assets (not for sharing)", "min_format": 0, "max_format": 999}}\n',
        encoding="utf8")


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    cmd = sys.argv[1]
    if cmd == "test":
        out = LOCAL / "test" / "cube.glb"
        convert("--test", out)
        print("pipeline ok:", out, out.stat().st_size, "bytes")
    elif cmd == "add":
        hero, src = sys.argv[2].lower(), Path(sys.argv[3])
        if hero not in HEROES:
            sys.exit(f"unknown hero {hero}: one of {sorted(HEROES)}")
        if not src.exists():
            sys.exit(f"no such file: {src}")
        write_mcmeta()
        out = MODELS / f"{hero}.glb"
        convert(str(src), out)
        print("added:", out, out.stat().st_size // 1024, "KB")
    elif cmd == "zip":
        if not PACK.exists():
            sys.exit("no pack yet: add a model first")
        out = LOCAL / "overcraft-assets.zip"
        with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
            for f in PACK.rglob("*"):
                if f.is_file():
                    z.write(f, f.relative_to(PACK))
        print("wrote", out, "- copy it to <.minecraft>/overcraft/overcraft-assets.zip")
    else:
        sys.exit(__doc__)


if __name__ == "__main__":
    main()
