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
    py tools/build_asset_pack.py fonts <dir>               the HUD's fonts, from a folder holding Overwatch's font files
    py tools/build_asset_pack.py effects <dir>             the ability effects' texture masks, from the effects
                                                            extraction (<dir>/Kiriko/<Ability>/textures/<id>.png)
                                                            (FuturaNo2D-DemiBold.ttf, BigNoodleTooOblique.ttf)

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


def read_soundmaps():
    """The sound maps (tools/soundmaps/*.txt): sources {id: [files]}, mixing rules {event: [(source, delay, gain)]} and
    weights {(id, file): Overwatch's weight} (a source line's "| w=<weight>")."""
    sources, rules, weights = {}, {}, {}
    for soundmap in sorted((ROOT / "tools" / "soundmaps").glob("*.txt")):
        for line in soundmap.read_text(encoding="utf8").splitlines():
            line = line.split("#")[0].strip() if not line.lstrip().startswith("#") else ""
            if not line or "=" not in line:
                continue
            name, _, rest = (x.strip() for x in line.partition("="))
            if name.startswith("@"):
                layers = []
                for part in filter(None, (x.strip() for x in rest.split(" + "))):
                    gain = 0.0
                    delay = 0.0
                    pitch = 1.0
                    if ":" in part:
                        part, g = part.rsplit(":", 1)
                        gain = float(g)
                    if "@" in part:
                        part, d = part.split("@", 1)
                        delay = float(d)
                    if "*" in part:
                        part, p = part.split("*", 1)
                        pitch = float(p)
                    layers.append((part.strip(), delay, gain, pitch))
                rules[name[1:]] = layers
            else:
                file, _, weight = (x.strip() for x in rest.partition("| w="))
                sources.setdefault(name, []).append(file)
                if weight:
                    weights[(name, file)] = float(weight)
    return sources, rules, weights


def sample_rate(f):
    """The file's sample rate (ffprobe)."""
    probe = subprocess.run(["ffprobe", "-v", "error", "-select_streams", "a:0", "-show_entries", "stream=sample_rate",
                            "-of", "csv=p=0", str(f)], capture_output=True, text=True)
    return int(probe.stdout.strip() or 48000)


def mix(inputs, out):
    """Mixes [(file, delay s, gain dB, pitch)] into one mono Ogg Vorbis file with ffmpeg; peaks kept under full scale.
    A pitch other than 1 plays the file faster and higher (as Wwise's pitch does), by resampling."""
    cmd = ["ffmpeg", "-y", "-loglevel", "error"]
    for f, _, _, _ in inputs:
        cmd += ["-i", str(f)]
    chains = []
    for k, (f, delay, gain, pitch) in enumerate(inputs):
        ms = int(round(delay * 1000))
        rate = sample_rate(f)
        speed = f"asetrate={round(rate * pitch)},aresample={rate}," if pitch != 1 else ""
        chains.append(f"[{k}:a]{speed}aformat=channel_layouts=mono,adelay={ms}:all=1,volume={gain}dB[a{k}]")
    labels = "".join(f"[a{k}]" for k in range(len(inputs)))
    chains.append(f"{labels}amix=inputs={len(inputs)}:normalize=0:duration=longest,alimiter=limit=0.89[out]")
    cmd += ["-filter_complex", ";".join(chains), "-map", "[out]", "-ac", "1", "-c:a", "libvorbis", "-q:a", "5", str(out)]
    return subprocess.run(cmd, capture_output=True, text=True).returncode == 0


def add_sounds(source):
    """Builds each mapped sound event (tools/soundmaps/*.txt) into assets/overcraft/sounds/ and writes the pack's
    sounds.json, replacing the jar's vanilla stand-ins with the real recordings. An event with a mixing rule gets one
    file per variant of its first layer (up to MAX_VARIANTS), its other layers mixed in with ffmpeg; without ffmpeg,
    only the first layer is copied (no mixing). Events with no rule are their sources' files as they are."""
    import json
    import random
    import shutil as sh
    write_mcmeta()
    sources, rules, weights = read_soundmaps()
    have_ffmpeg = sh.which("ffmpeg") is not None
    if not have_ffmpeg:
        print("ffmpeg not found: copying each event's first layer only (no mixing, stereo files stay stereo)")
    used = {layer for layers in rules.values() for layer, _, _, _ in layers}
    events = dict(rules)
    for name in sources:
        if name not in used and name not in events:
            events[name] = [(name, 0.0, 0.0, 1.0)]
    out = PACK / "assets" / "overcraft" / "sounds"
    if out.exists():
        shutil.rmtree(out)
    rng = random.Random(1)
    sounds = {}
    for event, layers in sorted(events.items()):
        files = []
        if layers:
            first = sources.get(layers[0][0], [])
            # a mix keeps MAX_VARIANTS; a plain event (a voice pool) keeps every file: its lines are the point
            for i, base in enumerate(first[:MAX_VARIANTS] if len(layers) > 1 or event in rules else first):
                dest = out / event.replace(".", "/") / f"{i}.ogg"
                dest.parent.mkdir(parents=True, exist_ok=True)
                picks = [(Path(source) / base, layers[0][1], layers[0][2], layers[0][3])]
                for layer, delay, gain, pitch in layers[1:]:
                    options = sources.get(layer, [])
                    if options:
                        picks.append((Path(source) / options[(i + rng.randrange(len(options))) % len(options)], delay, gain,
                                      pitch))
                missing = [str(f) for f, _, _, _ in picks if not f.exists()]
                if missing:
                    print(f"missing for {event}: {missing[0]}")
                    continue
                name = f"overcraft:{event.replace('.', '/')}/{i}"
                # Overwatch's weight (0.1-1) as Minecraft's (a whole number, at least 1)
                weight = weights.get((layers[0][0], base))
                entry = {"name": name, "weight": max(1, round(weight * 10))} if weight is not None else name
                if have_ffmpeg and mix(picks, dest):
                    files.append(entry)
                elif not have_ffmpeg or picks[0][0].suffix == ".ogg":
                    shutil.copyfile(picks[0][0], dest)
                    files.append(entry)
        sounds[event] = {"replace": True, "subtitle": "subtitles.overcraft." + event, "sounds": files}
    (PACK / "assets" / "overcraft" / "sounds.json").write_text(json.dumps(sounds, indent=1), encoding="utf8")
    print(f"{len(sounds)} sound events, {sum(len(v['sounds']) for v in sounds.values())} files -> {out}")


# variants kept per sound event (Minecraft picks one at random each time)
MAX_VARIANTS = 8

# the HUD's faces (HudText): pack file -> (Overwatch's file, font/*.json entries: size in GUI units and oversampling).
# Minecraft only reads TrueType outlines, which these two are.
FONTS = {"futura_demibold.ttf": "FuturaNo2D-DemiBold.ttf", "big_noodle.ttf": "BigNoodleTooOblique.ttf"}
FONT_DEFS = {"hud": ("futura_demibold.ttf", 32, 1.5), "hud_small": ("futura_demibold.ttf", 16, 2),
             "hud_numbers": ("big_noodle.ttf", 32, 2), "hud_chat": ("futura_demibold.ttf", 10.5, 3)}


def add_fonts(source):
    """Copies the HUD's fonts into assets/overcraft/font/ and writes their font definitions. The jar's own
    definitions only point at Minecraft's font; the pack's come first, so these win where they have the letter."""
    import json
    write_mcmeta()
    out = PACK / "assets" / "overcraft" / "font"
    out.mkdir(parents=True, exist_ok=True)
    for name, original in FONTS.items():
        src = Path(source) / original
        if not src.exists():
            sys.exit(f"missing {src}")
        shutil.copyfile(src, out / name)
    for font, (file, size, oversample) in FONT_DEFS.items():
        definition = {"providers": [{"type": "ttf", "file": "overcraft:" + file, "size": size, "oversample": oversample}]}
        (out / (font + ".json")).write_text(json.dumps(definition, indent=1), encoding="utf8")
    print(f"{len(FONTS)} fonts -> {out}")


# the effects' masks (grey-scale; the mod colours them at load, client/effect/EffectArt): pack name -> (hero, ability
# folder, texture id, frames). Ids from the effects survey, tools/reference/kiriko-effects.md.
EFFECT_MASKS = {
    "glow": ("Kiriko", "HealingOfuda", "00000000A7B6", 1),
    "glow_hot": ("Kiriko", "HealingOfuda", "00000000FFC9", 1),
    "flower": ("Kiriko", "HealingOfuda", "000000030242", 1),
    "ribbon": ("Kiriko", "HealingOfuda", "000000030182", 1),
    # the flying ofuda: four frames of a fluttering talisman silhouette (effects 2F93 / 7E2D, on mesh 10942)
    "talisman_flight": ("Kiriko", "HealingOfuda", "00000003A1EE", 4),
    "noise": ("Kiriko", "HealingOfuda", "0000000299E8", 1),
    "band": ("Kiriko", "HealingOfuda", "00000002FF91", 1),
    "petal": ("Kiriko", "SwiftStep", "00000003267E", 1),
    "streak": ("Kiriko", "SwiftStep", "00000002E59A", 1),
    "spark": ("Kiriko", "Kunai", "000000001F35", 4),
    "glint": ("Kiriko", "Kunai", "000000020065", 1),
    "ring_line": ("Kiriko", "ProtectionSuzu", "000000003684", 1),
    "dome_noise": ("Kiriko", "ProtectionSuzu", "0000000306CE", 1),
    "speed_line": ("Kiriko", "KitsuneRush", "00000002C701", 1),
    "sparkle": ("Kiriko", "KitsuneRush", "000000003DC7", 1),
    "smoke": ("Kiriko", "KitsuneRush", "0000000377D5", 48),
    "dust": ("Kiriko", "WallClimb", "000000000BE5", 1),
}


# the effects' meshes: pack model name -> (hero, ability folder, model id), converted with tools/owm_static.py
EFFECT_MODELS = {"torii": ("Kiriko", "KitsuneRush", "00000001048D")}


def add_effects(source):
    """Copies the effects' masks into assets/overcraft/textures/effect/ (flipbooks as <name>_<frame>.png), and
    converts their meshes (the torii gate) into hero_models/."""
    write_mcmeta()
    out = PACK / "assets" / "overcraft" / "textures" / "effect"
    out.mkdir(parents=True, exist_ok=True)
    copied = 0
    for name, (hero, ability, texture, frames) in EFFECT_MASKS.items():
        for frame in range(frames):
            suffix = "" if frame == 0 else f"_{frame}"
            src = Path(source) / hero / ability / "textures" / f"{texture}{suffix}.png"
            if not src.exists():
                print(f"missing {name}: {src}")
                continue
            dest = out / (name + ("" if frames == 1 else f"_{frame}") + ".png")
            shutil.copyfile(src, dest)
            copied += 1
    print(f"{copied} effect textures -> {out}")
    for name, (hero, ability, model) in EFFECT_MODELS.items():
        src = Path(source) / hero / ability / "models" / "Models" / f"{model}.00C" / f"{model}.owmdl"
        if not src.exists():
            print(f"missing {name}: {src}")
            continue
        dest = MODELS / f"{name}.glb"
        cmd = [blender(), "--background", "--factory-startup", "--python", str(ROOT / "tools" / "owm_static.py"), "--",
               str(src), str(dest)]
        result = subprocess.run(cmd, capture_output=True, text=True)
        report = [line for line in result.stdout.splitlines() if line.startswith("OWM_")]
        print("\n".join(report) or result.stderr[-2000:])


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
    elif cmd == "fonts":
        add_fonts(sys.argv[2])
    elif cmd == "effects":
        add_effects(sys.argv[2])
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
