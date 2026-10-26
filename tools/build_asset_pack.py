"""Builds the local Overwatch asset pack from model files on this machine. The pack is never committed or shipped
(CLAUDE.md): it lives in local-assets/ (gitignored) and the mod reads it from outside the jar.

    py tools/build_asset_pack.py add kiriko <model file>    convert a model (.blend/.fbx/.gltf/.glb/.obj/.smd) into the pack
    py tools/build_asset_pack.py zip                        local-assets/overcraft-assets.zip, for a real install:
                                                            copy it to <.minecraft>/overcraft/overcraft-assets.zip
    py tools/build_asset_pack.py test                       check the Blender pipeline with a cube
    py tools/build_asset_pack.py ui <DataTool output dir>  the heroes' portraits and ability/perk icons, from DataTool's
                                                            extract-hero-icons (<dir>/HeroIcons/<Hero>/...), and the role
                                                            badges from dump-ui-textures (<dir>/UITextures), for the UI
    py tools/build_asset_pack.py sounds <DataTool output dir>  the heroes' sounds and voice lines (tools/soundmaps/)

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
# DataTool's folder for each hero (its in-game name)
HERO_FOLDERS = {"kiriko": "Kiriko", "lucio": "Lúcio", "sombra": "Sombra", "illari": "Illari", "juno": "Juno",
                "sojourn": "Sojourn"}
GUI = PACK / "assets" / "overcraft" / "textures" / "gui" / "heroes"
# extract-hero-icons names files by id only; each hero has one of each kind, told apart by size (the same for every
# hero checked: Kiriko, Reaper). Two kinds are 256x256: the lower id is the portrait, the higher a closer crop.
ICON_SIZES = {(512, 512): "head", (80, 80): "silhouette_small", (652, 1080): "fullbody", (368, 500): "card",
              (508, 640): "silhouette", (200, 280): "bust"}
# role badges in DataTool's dump-ui-textures (<dir>/UITextures/UITextureDump/<id>.png), found by eye in build 154088:
# ids can change between builds
ROLE_ICONS = {"tank": "000000036FEB", "support": "000000036FEA", "damage": "000000036FE9"}

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


def png_size(path):
    with open(path, "rb") as f:
        head = f.read(24)
    return int.from_bytes(head[16:20], "big"), int.from_bytes(head[20:24], "big")


def slug(name):
    return "".join(c if c.isalnum() else "_" for c in name.lower()).strip("_")


def add_ui(source):
    """Copies each hero's icons to textures/gui/heroes/<hero>/: portrait, portrait_close, head, fullbody, card, bust,
    silhouette(_small), abilities/<ability>.png, perks/<perk>.png."""
    icons = Path(source) / "HeroIcons"
    if not icons.is_dir():
        sys.exit(f"no HeroIcons in {source}: run DataTool extract-hero-icons into it first")
    write_mcmeta()
    for hero, folder in sorted(HERO_FOLDERS.items()):
        src = icons / folder
        if not src.is_dir():
            print(f"{hero}: no icons ({src})")
            continue
        out = GUI / hero
        out.mkdir(parents=True, exist_ok=True)
        squares = []
        for png in sorted(src.glob("*.png")):
            size = png_size(png)
            if size == (256, 256):
                squares.append(png)
            elif size in ICON_SIZES:
                shutil.copyfile(png, out / f"{ICON_SIZES[size]}.png")
            else:
                print(f"{hero}: unknown icon {png.name} {size}")
        for name, png in zip(("portrait", "portrait_close"), sorted(squares, key=lambda p: int(p.stem, 16))):
            shutil.copyfile(png, out / f"{name}.png")
        for kind in ("Abilities", "Perks"):
            for png in sorted((src / kind).glob("*.png")):
                (out / kind.lower()).mkdir(exist_ok=True)
                shutil.copyfile(png, out / kind.lower() / f"{slug(png.stem)}.png")
        # smooth scaling: Minecraft samples GUI textures nearest-neighbour unless told to blur, and this art is drawn
        # much smaller than it is
        for png in out.rglob("*.png"):
            png.with_name(png.name + ".mcmeta").write_text('{"texture": {"blur": true}}\n', encoding="utf8")
        print(f"{hero}: {sum(1 for _ in out.rglob('*.png'))} images -> {out}")
    dump = Path(source) / "UITextures" / "UITextureDump"
    roles = PACK / "assets" / "overcraft" / "textures" / "gui" / "roles"
    for role, texture in ROLE_ICONS.items():
        png = dump / f"{texture}.png"
        if png.exists():
            roles.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(png, roles / f"{role}.png")
            (roles / f"{role}.png.mcmeta").write_text('{"texture": {"blur": true}}\n', encoding="utf8")
        else:
            print(f"no {role} icon ({png}): run DataTool dump-ui-textures into {source}/UITextures")


def add_sounds(source):
    """Copies each hero's mapped sounds (tools/soundmaps/<hero>.txt) into assets/overcraft/sounds/ and writes the
    pack's sounds.json, replacing the jar's vanilla stand-ins with the real recordings."""
    import json
    write_mcmeta()
    sounds = {}
    out = PACK / "assets" / "overcraft" / "sounds"
    for soundmap in sorted((ROOT / "tools" / "soundmaps").glob("*.txt")):
        for line in soundmap.read_text(encoding="utf8").splitlines():
            line = line.split("#")[0].strip()
            if not line:
                continue
            name, _, rel = (s.strip() for s in line.partition("="))
            src = Path(source) / rel
            if not src.exists():
                print(f"missing {name}: {src}")
                continue
            dest = out / (name.replace(".", "/") + ".ogg")
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(src, dest)
            sounds[name] = {"replace": True, "subtitle": "subtitles.overcraft." + name,
                            "sounds": ["overcraft:" + name.replace(".", "/")]}
    (PACK / "assets" / "overcraft" / "sounds.json").write_text(json.dumps(sounds, indent=1), encoding="utf8")
    print(f"{len(sounds)} sounds -> {out}")


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
    elif cmd == "ui":
        add_ui(sys.argv[2])
    elif cmd == "sounds":
        add_sounds(sys.argv[2])
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
