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
    py tools/build_asset_pack.py props <DataTool output dir> [hero]  the emotes' props (as the emote data names them), from the emote unlocks
    py tools/build_asset_pack.py parts <DataTool output dir> [hero]  the models weapon_parts.json names (magazines, vials), from the hero's skin folder
    py tools/build_asset_pack.py clips <DataTool output dir> [hero]  the clips-only files: each tools/clipmaps/<hero>[_1p]_clips.txt into hero_models/ (Blender)
    py tools/build_asset_pack.py glows <DataTool output dir> [<dir> ...]  every glow, from the materials (tools/glow_maps.py; no Blender, needs Pillow)
    py tools/build_asset_pack.py weapons <DataTool output dir> [<dir> ...]  parts, clips and glows, over every root (a root holds Heroes/<Hero>/Skin/...: the archive's
                                                            extracted\\154088 and research\\<hero>\\extract each hold some heroes), then zip's completeness check
    py tools/build_asset_pack.py effects [--only <prefix>] <dir> [<dir>...]  the ability effects' texture masks, from the effects
                                                            extraction (<dir>/Kiriko/<Ability>/textures/<id>.png); each
                                                            mask from the first folder that has it (Jetpack Cat's are in
                                                            "E:\Overwatch Archive\research\jetpack_cat\fx")
                                                            (FuturaNo2D-DemiBold.ttf, BigNoodleTooOblique.ttf)

The dev client (gradlew runClient) reads local-assets/pack/ directly.
"""
import os
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
# OC_ASSET_PACK (as gradlew runClient takes it) builds into another worktree's pack, e.g. the one the dev client loads
PACK = Path(os.environ["OC_ASSET_PACK"]) if os.environ.get("OC_ASSET_PACK") else ROOT / "local-assets" / "pack"
LOCAL = PACK.parent
MODELS = PACK / "assets" / "overcraft" / "hero_models"
HEROES = {"kiriko", "lucio", "sombra", "brigitte", "illari", "juno", "sojourn", "jetpack_cat", "mercy", "lifeweaver", "sigma", "ana", "dva", "pharah", "tracer", "cassidy", "junkrat"}
# DataTool's folder for each hero (its in-game name)
HERO_FOLDERS = {"kiriko": "Kiriko", "lucio": "Lúcio", "sombra": "Sombra", "illari": "Illari", "juno": "Juno",
                "sojourn": "Sojourn", "jetpack_cat": "Jetpack Cat", "mercy": "Mercy", "lifeweaver": "Lifeweaver",
                "ana": "Ana", "brigitte": "Brigitte", "sigma": "Sigma",
                "dva": "D.Va", "pharah": "Pharah", "tracer": "Tracer", "cassidy": "Cassidy", "junkrat": "Junkrat"}
GUI = PACK / "assets" / "overcraft" / "textures" / "gui" / "heroes"
# extract-hero-icons names files by id only; each hero has one of each kind, told apart by size (the same for every
# hero checked: Kiriko, Reaper). Two kinds are 256x256: the lower id is the portrait, the higher a closer crop.
ICON_SIZES = {(512, 512): "head", (80, 80): "silhouette_small", (652, 1080): "fullbody", (368, 500): "card",
              (508, 640): "silhouette", (200, 280): "bust"}
# role badges in DataTool's dump-ui-textures (<dir>/UITextures/UITextureDump/<id>.png), found by eye in build 154088:
# ids can change between builds
ROLE_ICONS = {"tank": "000000036FEB", "support": "000000036FEA", "damage": "000000036FE9"}
# the kill feed's icons in the UI texture dump: Quick Melee's fist (#33; its 88x109 visible area is the wiki's
# Quick_melee.png). The headshot's "small red target" wasn't found in this dump.
KILLFEED_ICONS = {"melee": "00000000E613"}

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
    """A resource-safe id: lower case ASCII letters, digits and underscores, accents folded ("Lúcio-oh's" ->
    "lucio_oh_s": Minecraft's ids allow no others)."""
    import unicodedata
    plain = "".join(c for c in unicodedata.normalize("NFKD", name) if not unicodedata.combining(c))
    return "".join(c if c.isascii() and c.isalnum() else "_" for c in plain.lower()).strip("_")


def add_ui(source, only=None):
    """Copies each hero's icons (or only that hero's: `ui <source> <hero>`) to textures/gui/heroes/<hero>/: portrait, portrait_close, head, fullbody, card, bust,
    silhouette(_small), abilities/<ability>.png, perks/<perk>.png."""
    icons = Path(source) / "HeroIcons"
    if not icons.is_dir():
        sys.exit(f"no HeroIcons in {source}: run DataTool extract-hero-icons into it first")
    write_mcmeta()
    for hero, folder in sorted(HERO_FOLDERS.items()):
        if only and hero != only:
            continue
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
    if only:
        return  # one hero's icons: the shared ones (roles, kill feed, Lucio's song) are left as they are
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
    feed = PACK / "assets" / "overcraft" / "textures" / "gui" / "killfeed"
    for name, texture in KILLFEED_ICONS.items():
        png = dump / f"{texture}.png"
        if png.exists():
            feed.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(png, feed / f"{name}.png")
            (feed / f"{name}.png.mcmeta").write_text('{"texture": {"blur": true}}\n', encoding="utf8")
        else:
            print(f"no kill feed {name} icon ({png})")
    song = PACK / "assets" / "overcraft" / "textures" / "gui" / "heroes" / "lucio" / "song"
    for name, (texture, box) in SONG_GLYPHS.items():
        png = dump / f"{texture}.png"
        if png.exists():
            song.mkdir(parents=True, exist_ok=True)
            glyph_crop(png, box, song / f"{name}.png")
            (song / f"{name}.png.mcmeta").write_text('{"texture": {"blur": true}}\n', encoding="utf8")
        else:
            print(f"no Lucio {name} song icon ({png})")


# Lúcio's song indicator under the crosshair (#61): the UI dump has no art of its own for it (a search of all 21,567
# textures, 2026-10-29); its icons are the glyphs inside his Healing Boost (3C5D) and Speed Boost (3C5C) icons: the
# pieces lying wholly inside each box (x0, y0, x1, y1), the rest of the icon masked out
SONG_GLYPHS = {"heal": ("000000003C5D", (43, 47, 83, 89)), "speed": ("000000003C5C", (42, 50, 89, 87))}


def glyph_crop(png, box, out):
    """The connected pieces of png's mask lying wholly inside box, their soft edges kept, as a 64 x 64 white mask."""
    from PIL import Image
    im = Image.open(png).convert("RGBA")
    mask = im.split()[3] if im.getextrema()[3][0] < 255 else im.convert("L")
    w, h = mask.size
    px = mask.load()
    x0, y0, x1, y1 = box
    seen = set()
    keep = set()
    for y in range(h):
        for x in range(w):
            if (x, y) in seen or px[x, y] < 64:
                continue
            piece = []
            stack = [(x, y)]
            seen.add((x, y))
            while stack:
                cx, cy = stack.pop()
                piece.append((cx, cy))
                for nx, ny in ((cx + 1, cy), (cx - 1, cy), (cx, cy + 1), (cx, cy - 1)):
                    if 0 <= nx < w and 0 <= ny < h and (nx, ny) not in seen and px[nx, ny] >= 64:
                        seen.add((nx, ny))
                        stack.append((nx, ny))
            xs = [p[0] for p in piece]
            ys = [p[1] for p in piece]
            if min(xs) >= x0 and max(xs) < x1 and min(ys) >= y0 and max(ys) < y1:
                keep.update(piece)
    # their soft edges: a pixel's neighbours
    edge = {(x + dx, y + dy) for x, y in keep for dx in (-1, 0, 1) for dy in (-1, 0, 1)}
    side = max(x1 - x0, y1 - y0) + 8
    glyph = Image.new("L", (side, side), 0)
    gp = glyph.load()
    ox = (side - (x1 - x0)) // 2 - x0
    oy = (side - (y1 - y0)) // 2 - y0
    for x, y in edge:
        if 0 <= x < w and 0 <= y < h and 0 <= x + ox < side and 0 <= y + oy < side:
            gp[x + ox, y + oy] = px[x, y]
    white = Image.new("L", (side, side), 255)
    Image.merge("RGBA", (white, white, white, glyph)).resize((64, 64), Image.LANCZOS).save(out)


# the sound maps' own subtitles: event -> text (read_soundmaps)
SUBTITLES = {}


def read_soundmaps():
    """The sound maps (tools/soundmaps/*.txt): sources {id: [files]}, mixing rules {event: [(source, delay, gain)]} and
    weights {(id, file): Overwatch's weight} (a source line's "| w=<weight>"). A "$<event> = <text>" line is the event's
    subtitle, for events the jar's lang doesn't name (the emotes' sounds and lines: SUBTITLES)."""
    sources, rules, weights = {}, {}, {}
    SUBTITLES.clear()
    for soundmap in sorted((ROOT / "tools" / "soundmaps").glob("*.txt")):
        for line in soundmap.read_text(encoding="utf8").splitlines():
            line = line.split("#")[0].strip() if not line.lstrip().startswith("#") else ""
            if not line or "=" not in line:
                continue
            name, _, rest = (x.strip() for x in line.partition("="))
            if name.startswith("$"):
                SUBTITLES[name[1:]] = rest
                continue
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
    lang = add_voicelines(source, out, sounds)
    for event, text in SUBTITLES.items():
        if event in sounds:
            lang[f"subtitles.overcraft.{event}"] = text
    (PACK / "assets" / "overcraft" / "sounds.json").write_text(json.dumps(sounds, indent=1), encoding="utf8")
    lang_file = PACK / "assets" / "overcraft" / "lang" / "en_us.json"
    lang_file.parent.mkdir(parents=True, exist_ok=True)
    lang_file.write_text(json.dumps(lang, indent=1, ensure_ascii=False), encoding="utf8")
    print(f"{len(sounds)} sound events, {sum(len(v['sounds']) for v in sounds.values())} files -> {out}")


# heroes whose collectible voice lines go in the pack: our id -> DataTool's hero folder name
VOICELINE_HEROES = {"kiriko": "Kiriko", "lucio": "Lúcio", "mercy": "Mercy", "juno": "Juno", "ana": "Ana", "sombra": "Sombra",
                    "brigitte": "Brigitte", "lifeweaver": "Lifeweaver", "sojourn": "Sojourn", "sigma": "Sigma", "dva": "D.Va", "pharah": "Pharah", "tracer": "Tracer", "cassidy": "Cassidy", "junkrat": "Junkrat"}
# a hero whose extraction (Unlocks/ and HeroVoice/) isn't under the build root: its folder relative to it, as the hero's
# soundmap reads her voice (Juno's DataTool extract-unlocks "Juno|voiceline=*" went to research\juno\extract, 27 unlocks)
VOICELINE_ROOTS = {"juno": "../../research/juno/extract", "ana": "../../research/ana/extract",
                   "sombra": "../../research/sombra/extract", "brigitte": "../../research/brigitte/extract",
                   "lifeweaver": "../../research/lifeweaver/extract",
                   "sojourn": "../../research/sojourn/extract",
                   "sigma": "../../research/sigma/extract",
                   "dva": "../../research/dva/extract",
                   "pharah": "../../research/pharah/extract",
                   "tracer": "../../research/tracer/extract",
                   "cassidy": "../../research/cassidy/extract",
                   "junkrat": "../../research/junkrat/extract"}   # each hero's extract-unlocks "<Hero>|voiceline=*" (#77)
# Heroes whose voice-line unlocks DataTool doesn't export (Jetpack Cat's: each is an effect with one voice stimulus that
# names one meow), listed from the data instead (E:\Overwatch Archive\research\jetpack_cat\gaps.md 1c, H): unlock name
# -> (group, the meow under HeroVoice/<folder>/Voicelines/(cat sounds), our subtitle: DataTool names them only "(cat
# sounds)"). Groups as DataTool names them for the other heroes: the unlock list's "Base" is "Overwatch".
VOICELINE_TABLES = {"jetpack_cat": ("Jetpack Cat", {
    "Innocent": ("Overwatch", "00000033F3C2", "(an innocent meow)"),          # C26B, stimulus 05A1
    "Inviting": ("Overwatch", "00000033F3CD", "(an inviting meow)"),          # C26C, 05A0
    "Begrudging": ("Overwatch", "00000033F3BD", "(a begrudging meow)"),       # C26D, 059A
    "Excited": ("Overwatch", "00000033F3BE", "(an excited meow)"),            # C26E, 05A2
    "Annoyed": ("Overwatch", "00000033F3C0", "(an annoyed meow)"),            # C26F, 059C
    "Shrugging": ("Overwatch", "0000003408F9", "(a shrugging meow)"),         # C270, 05A3
    "Aggressive": ("Overwatch", "00000033F3D0", "(an aggressive meow)"),      # C271, 059F
    "Pew Pew": ("Overwatch", "00000033F3D7", "(pew pew meows)"),              # C272, 059E
    "Singing": ("Overwatch", "00000033F3D6", "(singing meows)"),              # C27C, 19E7
    "Questioning": ("Overwatch", "00000033F3B5", "(a questioning meow)"),     # C27B, 19E5
    "Tumbling": ("2026: Season 1", "0000003402D4", "(tumbling meows)"),       # C118, 19D8
    "Taunting": ("Overwatch", "00000033F3BB", "(a taunting meow)"),           # C28E (a level unlock), 05A6
})}


def voice_text(file_name):
    """The spoken text DataTool puts in a voice file's name ("<id>.0B2-<text>.ogg"): any character a file name can't hold
    became "_" (a "?", the quotes round "_perfect world_", the colon after "(Swedish)"), so the .txt beside it is read
    first (spoken_text)."""
    import re
    text = file_name.split(".0B2-", 1)[-1].rsplit(".", 1)[0]
    text = re.sub(r"^\(([^)]+)\)_ ", r"() ", text)
    text = re.sub(r"(^|\s)_([^_]+)_(?=[\s.,!]|$)", r'""', text)
    return text.replace("_", "?")


def spoken_text(ogg):
    """A voice file's spoken text: the subtitle DataTool wrote beside it (--subtitles-with-sounds: "<name>.txt", UTF-16)
    when there is one, the exact line with its quotes and question marks (#77); else what its name says."""
    txt = ogg.with_suffix(".txt")
    if txt.exists():
        raw = txt.read_bytes()
        for enc in ("utf-16", "utf-16-le", "utf-8"):
            try:
                text = raw.decode(enc).strip().strip("﻿")
                if text:
                    return text
            except UnicodeDecodeError:
                pass
    return voice_text(ogg.name)


def add_voicelines(source, out, sounds):
    """Each hero's collectible voice lines (the communication wheel's voice page): DataTool's extract-unlocks
    "<Hero>|voiceline=*" puts them under Unlocks/Heroes/<Hero>/VoiceLine/<season>/<unlock name>/<id>.ogg, several files
    for an unlock with several takes ("Tarot Reading"). One sound event per unlock, "<hero>.voiceline.<slug>", its
    subtitle the spoken text (the unlock's name when it has several), and assets/overcraft/voicelines/<hero>.json
    listing them for the game. Returns the pack's lang entries (the unlock names and subtitles)."""
    import json
    lang = {}
    for hero, folder in VOICELINE_HEROES.items():
        root = Path(source) / VOICELINE_ROOTS.get(hero, ".")
        base = root / "Unlocks" / "Heroes" / folder / "VoiceLine"
        spoken = {}
        for f in (root / "HeroVoice" / folder / "Voicelines").glob("*.ogg"):
            if ".0B2-" in f.name or f.name.split(".", 1)[0] not in spoken:
                spoken[f.name.split(".", 1)[0]] = spoken_text(f)
        if not base.exists():
            print(f"no voice line unlocks for {hero}: run DataTool extract-unlocks <out>/Unlocks with {folder}|voiceline=*")
            continue
        listed = []
        seen_files = {}
        for unlock in sorted(d for d in base.glob("*/*") if d.is_dir()):
            name = unlock.name.replace("_", "?")
            key = slug(name)
            files = sorted(unlock.glob("*.ogg"))
            if not files:
                continue
            if key in {e["id"] for e in listed}:
                # the same unlock listed under two groups; two different lines with one name (Brigitte's Season 10 "Get
                # lost!" and Anniversary "Get Lost", 4A230 / 4A231) both stay, the second named by its group (#77)
                if files[0].name.split(".", 1)[0] in seen_files.get(key, ()):
                    continue
                key = key + "_" + slug(unlock.parent.name)
            seen_files.setdefault(key, set()).add(files[0].name.split(".", 1)[0])
            event = f"{hero}.voiceline.{key}"
            entries = []
            for i, f in enumerate(files):
                dest = out / event.replace(".", "/") / f"{i}.ogg"
                dest.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(f, dest)
                entries.append(f"overcraft:{event.replace('.', '/')}/{i}")
            sounds[event] = {"replace": True, "subtitle": "subtitles.overcraft." + event, "sounds": entries}
            text = spoken.get(files[0].name.split(".", 1)[0], name) if len(files) == 1 else name
            lang[f"overcraft.voiceline.{hero}.{key}"] = name
            lang[f"subtitles.overcraft.{event}"] = f"{folder}: {text}"
            listed.append({"id": key, "group": unlock.parent.name.replace("_", ":")})
        target = PACK / "assets" / "overcraft" / "voicelines" / f"{hero}.json"
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(listed, indent=1, ensure_ascii=False), encoding="utf8")
        print(f"{hero}: {len(listed)} voice lines")
    for hero, (folder, table) in VOICELINE_TABLES.items():
        listed = []
        meows = Path(source) / "HeroVoice" / folder / "Voicelines" / "(cat sounds)"
        for name, (group, file_id, text) in table.items():
            f = meows / f"{file_id}.03F.ogg"
            if not f.exists():
                print(f"{hero}: no file {f}")
                continue
            key = slug(name)
            event = f"{hero}.voiceline.{key}"
            dest = out / event.replace(".", "/") / "0.ogg"
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(f, dest)
            sounds[event] = {"replace": True, "subtitle": "subtitles.overcraft." + event,
                             "sounds": [f"overcraft:{event.replace('.', '/')}/0"]}
            lang[f"overcraft.voiceline.{hero}.{key}"] = name
            lang[f"subtitles.overcraft.{event}"] = f"{folder}: {text}"
            listed.append({"id": key, "group": group})
        target = PACK / "assets" / "overcraft" / "voicelines" / f"{hero}.json"
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(listed, indent=1, ensure_ascii=False), encoding="utf8")
        print(f"{hero}: {len(listed)} voice lines (from the table)")
    return lang


# variants kept per sound event (Minecraft picks one at random each time)
MAX_VARIANTS = 8

# the HUD's faces (HudText): pack file -> (Overwatch's file, font/*.json entries: size in GUI units and oversampling).
# Minecraft only reads TrueType outlines, which these two are.
# Overwatch's chat is BlizzardGlobal-Regular, not the HUD's Futura (the eighth critique, checked against the owner's frame)
FONTS = {"futura_demibold.ttf": "FuturaNo2D-DemiBold.ttf", "big_noodle.ttf": "BigNoodleTooOblique.ttf",
         "blizzard_global.ttf": "BlizzardGlobal-Regular.ttf"}
FONT_DEFS = {"hud": ("futura_demibold.ttf", 32, 1.5), "hud_small": ("futura_demibold.ttf", 16, 2),
             "hud_numbers": ("big_noodle.ttf", 32, 2), "hud_chat": ("blizzard_global.ttf", 8.4, 3.5)}


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
    # Lúcio's (tools/reference/lucio-effects.md): the pulse flipbook (a dot opening to a ring: Soundwave's discs, the
    # hits, the barrier's ground ring), the round's soft glow, a glow disc, the spark, the trails' soft band, the
    # rings' band and Amp It Up's sparkle (a colour sprite)
    "lucio_pulse": ("Lucio", "Soundwave", "000000003526", 9),
    "lucio_glow": ("Lucio", "SonicAmplifier", "0000000210FD", 1),
    "lucio_soft": ("Lucio", "SonicAmplifier", "0000000004F3", 1),
    "lucio_spark": ("Lucio", "SonicAmplifier", "0000000029F7", 1),
    "lucio_trail": ("Lucio", "Crossfade", "000000003A37", 1),
    "lucio_band": ("Lucio", "SonicAmplifier", "000000003F9D", 1),
    "lucio_sparkle": ("Lucio", "AmpItUp", "000000000BCD", 1),
    # Jetpack Cat's (tools/reference/jetpack_cat-effects.md): soft dot, star flare, ring, spark streak, cloud and
    # noise, the 4-frame burst, fade strip, the healing swirl and dot (colour sprites), soft ring
    "cat_soft": ("JetpackCat", "Pawjectiles", "000000004C09", 1),
    "cat_flare": ("JetpackCat", "Pawjectiles", "00000000DA5B", 1),
    "cat_ring": ("JetpackCat", "Pawjectiles", "000000003203", 1),
    "cat_streak": ("JetpackCat", "Pawjectiles", "0000000183A7", 1),
    "cat_cloud": ("JetpackCat", "Pawjectiles", "000000018E48", 1),
    "cat_noise": ("JetpackCat", "Pawjectiles", "0000000299E1", 1),
    "cat_burst": ("JetpackCat", "Pawjectiles", "00000001917E", 4),
    "cat_fade": ("JetpackCat", "Pawjectiles", "0000000007A9", 1),
    "cat_heal_swirl": ("JetpackCat", "Pawjectiles", "000000001268", 1),
    "cat_heal_dot": ("JetpackCat", "Pawjectiles", "00000000126A", 1),
    "cat_halo_ring": ("JetpackCat", "Pawjectiles", "0000000052E8", 1),
    # Purr: the burst-line streak, a soft ball, the range wall's fade strip
    "cat_purr_streak": ("JetpackCat", "Purr", "00000001D047", 1),
    "cat_soft_ball": ("JetpackCat", "Purr", "000000039115", 1),
    "cat_band": ("JetpackCat", "Purr", "000000001C7A", 1),
    # Lifeline: the paw-in-crosshair marker (left half: mirror it), four-point star, hex grid, speed lines, rope core
    "cat_paw_marker": ("JetpackCat", "Lifeline", "000000070542", 1),
    "cat_star": ("JetpackCat", "Lifeline", "00000000F55D", 1),
    "cat_hex": ("JetpackCat", "Lifeline", "000000018F9B", 1),
    "cat_speed_line": ("JetpackCat", "Lifeline", "00000000E99F", 3),
    "cat_rope": ("JetpackCat", "Lifeline", "0000000018A1", 1),
    # her second assets pass (research\jetpack_cat\assets2.md): the rope's braid (71B0) and glow strips, the hook cone's
    # and the rings' masks (FX2 = research\jetpack_cat\assets2\fx)
    "cat_rope_braid": ("JetpackCat", "Lifeline", "00000006E132", 1),
    "cat_glow_noise": ("JetpackCat", "Lifeline", "000000019167", 1),
    "cat_strip": ("JetpackCat", "Lifeline", "000000001344", 1),
    "cat_cone_noise": ("JetpackCat", "Lifeline", "00000002C41F", 1),
    "cat_cone_line": ("JetpackCat", "Lifeline", "000000016E9D", 1),
    "cat_ring_line": ("JetpackCat", "Lifeline", "0000000299E3", 1),
    # the rest of #59 (research\jetpack_cat\work\fx_digest.txt): the hook's bubbles' thin band 6C057 (71B1 C9), the
    # towed ally's arcs' masks (7454 C6 / C8: the round edge 71226, the noise 18F00, the strips BA19 and 39F16), which
    # bake_cat_arcs() lays on the arc; 18F00 is also Purr's range wall's (70F9)
    "cat_line_strip": ("JetpackCat", "Lifeline", "00000006C057", 1),
    "cat_arc_edge": ("JetpackCat", "Lifeline", "000000071226", 1),
    "cat_soft_noise": ("JetpackCat", "Lifeline", "000000018F00", 1),
    "cat_arc_strip": ("JetpackCat", "Lifeline", "00000000BA19", 1),
    "cat_arc_stripes": ("JetpackCat", "Lifeline", "000000039F16", 1),
    # Frenetic Flight and the jetpack: nozzle hot dot, trail strip, spark blobs
    "cat_glow_dot": ("JetpackCat", "FreneticFlight", "00000000A17F", 1),
    "cat_trail": ("JetpackCat", "FreneticFlight", "0000000219FA", 1),
    "cat_blob": ("JetpackCat", "FreneticFlight", "000000018BC2", 4),
    # Catnapper: the aim star, crater cracks (dark and glowing), the impact puff
    "cat_sparkle": ("JetpackCat", "Catnapper", "00000000F7A7", 1),
    "cat_crack": ("JetpackCat", "Catnapper", "00000006CBB9", 1),
    "cat_crack_glow": ("JetpackCat", "Catnapper", "00000006CA10", 1),
    "cat_puff": ("JetpackCat", "Catnapper", "000000018B18", 1),
    # her effects still undrawn after #59's first passes (research\jetpack_cat\work\fx_digest.txt and
    # assets2\work\fx_digest.txt): Claws Out's claw marks (74D0, FX2), the landing hit's flame burst and dust puff
    # (7256, FX2), Transport Shielding's hex edge (5513), the head slash's streak and the dive dome's strip (71B8), the
    # paw print's teardrop (709F), the cast bell's thin line (7373 C37)
    "cat_claws": ("JetpackCat", "ClawsOut", "0000000720CE", 1),
    "cat_claw_flame": ("JetpackCat", "Catnapper", "00000000F7D2", 4),
    "cat_dust_puff": ("JetpackCat", "Catnapper", "000000005429", 4),
    "cat_hex_edge": ("JetpackCat", "Lifeline", "000000018400", 1),
    "cat_slash": ("JetpackCat", "Catnapper", "000000037021", 1),
    "cat_dome_strip": ("JetpackCat", "Catnapper", "000000055A46", 1),
    "cat_teardrop": ("JetpackCat", "Pawjectiles", "000000000D89", 1),
    "cat_bell_line": ("JetpackCat", "Catnapper", "000000054A15", 1),

    # Mercy's (tools/reference/mercy-effects.md): cloud noise, the beams' wavy streaks, centre line, noise, long
    # streaks and band, a second noise, the beamed ally's rim (INVERTED: black centre)
    "mercy_noise": ("Mercy", "CaduceusStaff", "0000000004DB", 1),
    "mercy_beam_wave": ("Mercy", "CaduceusStaff", "00000002800B", 1),
    "mercy_beam_core": ("Mercy", "CaduceusStaff", "000000028016", 1),
    "mercy_beam_noise": ("Mercy", "CaduceusStaff", "000000019167", 1),
    "mercy_beam_streak": ("Mercy", "CaduceusStaff", "0000000207EC", 1),
    "mercy_band": ("Mercy", "CaduceusStaff", "000000001344", 1),
    "mercy_noise_b": ("Mercy", "CaduceusStaff", "000000004A7E", 1),
    "mercy_rim": ("Mercy", "CaduceusStaff", "000000000D8F", 1),
    # wing glows: soft ball, yellow wisps (a colour sprite: NATURAL)
    "mercy_soft": ("Mercy", "AngelicDescent", "00000000139C", 1),
    "mercy_wisp": ("Mercy", "AngelicDescent", "0000000013CC", 1),
    # the blaster: hot glow, muzzle flame burst, thin streak, star flare, impact disc (colour sprite)
    "mercy_glow": ("Mercy", "CaduceusBlaster", "000000000242", 1),
    "mercy_muzzle": ("Mercy", "CaduceusBlaster", "000000004B47", 1),
    "mercy_streak": ("Mercy", "CaduceusBlaster", "000000000D89", 1),
    "mercy_star": ("Mercy", "CaduceusBlaster", "000000011DF2", 1),
    "mercy_impact": ("Mercy", "CaduceusBlaster", "0000000020AC", 1),
    # Guardian Angel's trail (dim: max 165)
    "mercy_trail": ("Mercy", "GuardianAngel", "0000000013F9", 1),
    # Resurrect: lens streak, the halo ring and the gold teardrop (colour sprites), the plus cross
    "mercy_lens": ("Mercy", "Resurrect", "00000000CC29", 1),
    "mercy_halo": ("Mercy", "Resurrect", "000000001411", 1),
    "mercy_flame": ("Mercy", "Resurrect", "000000001369", 1),
    "mercy_plus": ("Mercy", "Resurrect", "000000027E5E", 1),
    # Valkyrie's feathers: soft bar, long spike; the wings' streaky noise; Flash Heal's beam web (1024²)
    "mercy_feather": ("Mercy", "Valkyrie", "000000011BA6", 1),
    "mercy_spike": ("Mercy", "Valkyrie", "000000011B97", 1),
    "mercy_wing_noise": ("Mercy", "Wings", "000000005A7F", 1),
    "mercy_web": ("Mercy", "FlashHeal", "00000001CB5D", 1),
    # Lifeweaver's (tools/reference/lifeweaver-effects.md; pass his research fx folder, research/lifeweaver/fx, as a
    # source): his hex sparkle, the petal, the vine's wisps and line, a thin ring, a star flare, a soft glow and disc, a
    # radial burst, a swirl ring, the leaf-flame wisp, the rainbow ring (a colour sprite)
    "lw_sparkle": ("Lifeweaver", "LifeGrip", "00000003EB7D", 1),
    "lw_petal": ("Lifeweaver", "LifeGrip", "00000003FD63", 1),
    "lw_wisp": ("Lifeweaver", "LifeGrip", "00000002FCA7", 1),
    "lw_vine_line": ("Lifeweaver", "LifeGrip", "00000002BE79", 1),
    "lw_ring": ("Lifeweaver", "LifeGrip", "00000002E8AF", 1),
    "lw_star": ("Lifeweaver", "LifeGrip", "00000000E221", 1),
    "lw_glow": ("Lifeweaver", "HealingBlossom", "00000000A7B6", 1),
    "lw_disc": ("Lifeweaver", "HealingBlossom", "00000000122C", 1),
    "lw_burst": ("Lifeweaver", "HealingBlossom", "000000037C36", 1),
    "lw_swirl": ("Lifeweaver", "HealingBlossom", "000000030E12", 1),
    "lw_leaf_flame": ("Lifeweaver", "HealingBlossom", "000000039C48", 1),
    "lw_prism": ("Lifeweaver", "HealingBlossom", "0000000017D8", 1),
    # the tree: flower-band wall, lens streak, upward cone, leaf flipbook, bokeh noise, vertical fade
    "lw_flower_band": ("Lifeweaver", "TreeOfLife", "000000040484", 1),
    "lw_lens": ("Lifeweaver", "TreeOfLife", "0000000023C0", 1),
    "lw_beam_cone": ("Lifeweaver", "TreeOfLife", "00000002D278", 1),
    "lw_leaf": ("Lifeweaver", "TreeOfLife", "00000003AC78", 4),
    "lw_bokeh": ("Lifeweaver", "TreeOfLife", "0000000187EB", 1),
    "lw_fade_v": ("Lifeweaver", "TreeOfLife", "00000003E4E5", 1),
    # thorns: the muzzle flame (2 frames), the hit burst (4 frames), the crystal grid (a mesh texture); the dash's plus
    # (a colour sprite: NATURAL)
    "lw_thorn_flame": ("Lifeweaver", "ThornVolley", "0000000014F9", 2),
    "lw_hit": ("Lifeweaver", "ThornVolley", "00000002F3C9", 4),
    "lw_crystal": ("Lifeweaver", "ThornVolley", "00000002A413", 1),
    "lw_plus": ("Lifeweaver", "RejuvenatingDash", "000000000BCD", 1),
    # Ana's (tools/reference/ana-effects.md): the sleep's Z, the rifle's muzzle double flame, its hit splash and sparkle
    # (flipbooks), the star decal, the heal burst, the tracer's line and length fade, the drip blob
    "ana_z": ("Ana", "SleepDart", "0000000098DA", 1),
    "ana_muzzle_flame": ("Ana", "BioticRifle", "00000002BDEF", 1),
    "ana_splash": ("Ana", "BioticRifle", "000000008FE3", 10),
    "ana_sparkle": ("Ana", "BioticRifle", "000000064B91", 12),
    "ana_star_decal": ("Ana", "BioticRifle", "000000004C0C", 1),
    "ana_burst": ("Ana", "BioticRifle", "00000004B64D", 1),
    "ana_beam_line": ("Ana", "BioticRifle", "000000000B35", 1),
    "ana_fade_v": ("Ana", "BioticRifle", "00000002A063", 1),
    "ana_blob": ("Ana", "BioticRifle", "000000001F56", 2),
    # the grenade: the splatter (3 frames), the tendril, the trail line; the boost / anti-heal icon parts (inverted:
    # frame, framed plus, slash) and the victim's screen vignette and ring (inverted / screen: HUD use)
    "ana_splat": ("Ana", "BioticGrenade", "00000002B697", 3),
    "ana_tendril": ("Ana", "BioticGrenade", "00000002BECA", 1),
    "ana_tube_line": ("Ana", "BioticGrenade", "00000001EF26", 1),
    "ana_frame": ("Ana", "BioticGrenade", "000000027E5D", 1),
    "ana_plus_cut": ("Ana", "BioticGrenade", "00000003A0DE", 1),
    "ana_slash": ("Ana", "BioticGrenade", "0000000391F2", 1),
    "ana_vignette": ("Ana", "BioticGrenade", "0000000005D0", 1),
    "ana_ring": ("Ana", "BioticGrenade", "000000006429", 1),
    # the sleep dart: its ribbon band, the hit's star flare and star burst, the drowsy puff (4 frames), Local
    # Anesthetic's swirl (2 frames) and cracks
    "ana_dart_band": ("Ana", "SleepDart", "00000001887E", 1),
    "ana_star": ("Ana", "SleepDart", "000000003737", 1),
    "ana_star_burst": ("Ana", "SleepDart", "000000003B80", 1),
    "ana_puff": ("Ana", "SleepDart", "000000005429", 4),
    "ana_swirl": ("Ana", "SleepDart", "000000039B71", 2),
    "ana_cracks": ("Ana", "SleepDart", "00000000FFDC", 1),
    # Nano Boost: the flame tongue (4 frames), the lens streak, the beam's lines, the launch smoke (a colour sprite:
    # NATURAL), the vertical streak
    "ana_flame": ("Ana", "NanoBoost", "000000007ADD", 4),
    "ana_lens": ("Ana", "NanoBoost", "000000005E9E", 1),
    "ana_nano_core": ("Ana", "NanoBoost", "000000009ACC", 1),
    "ana_nano_line": ("Ana", "NanoBoost", "000000000965", 1),
    "ana_nano_edge": ("Ana", "NanoBoost", "000000000510", 1),
    "ana_smoke": ("Ana", "NanoBoost", "0000000009EF", 4),
    "ana_streak_v": ("Ana", "NanoBoost", "00000000384F", 1),
    # Sombra's (tools/reference/sombra-effects.md; pass her research fx folder, research/sombra/fx, as a source). The
    # hack: the skull icon (filled, outline, and Hotfix's skull with a plus), the code-rain streak / digit column / dot
    # column, glyph flipbook (4), scanlines, vertical streak, the digital-rain wall and circuit-board textures
    "sombra_skull": ("Sombra", "Hack", "00000000AB9D", 1),
    "sombra_skull_outline": ("Sombra", "Hack", "00000001EDB3", 1),
    "sombra_skull_heal": ("Sombra", "Hotfix", "00000004B0C1", 1),
    "sombra_code_rain": ("Sombra", "Hack", "000000009F81", 1),
    "sombra_code_column": ("Sombra", "Hack", "00000000A4ED", 1),
    "sombra_code_dots": ("Sombra", "Hack", "00000000A4EC", 1),
    "sombra_glyphs": ("Sombra", "Cyberspace", "000000009F80", 4),
    "sombra_scanlines": ("Sombra", "Hack", "0000000010A3", 1),
    "sombra_streak_v": ("Sombra", "Cyberspace", "000000009BDF", 1),
    "sombra_code_wall": ("Sombra", "Cyberspace", "00000000A512", 1),
    "sombra_circuit": ("Sombra", "Cyberspace", "00000000AD57", 1),
    "sombra_circuit_disc": ("Sombra", "Cyberspace", "00000007B847", 1),
    "sombra_circuit_arms": ("Sombra", "EMP", "00000000A064", 1),
    "sombra_circuit_arms_small": ("Sombra", "Cyberspace", "000000068C88", 1),
    # pixel bars (13 / 10 frames), the EMP's "skyline" bars (14) and band (10), the Hotfix strand's pixel blocks, noise
    "sombra_pixel_bars": ("Sombra", "Hotfix", "00000000A66D", 13),
    "sombra_pixel_bars_b": ("Sombra", "Translocator", "00000000A629", 10),
    "sombra_skyline": ("Sombra", "EMP", "00000000A5B6", 14),
    "sombra_emp_band": ("Sombra", "EMP", "000000004FE0", 10),
    "sombra_ribbon_pixels": ("Sombra", "Hotfix", "00000002B731", 1),
    "sombra_pixels": ("Sombra", "Hotfix", "000000005D4E", 1),
    # the Machine Pistol: muzzle flame (6) and spike (2), the tracer's line and edge, the shared bullet impact's star
    # (2), spark (4: a colour sprite, NATURAL) and burst decal
    "sombra_muzzle_flame": ("Sombra", "MachinePistol", "00000000A25E", 6),
    "sombra_muzzle_spike": ("Sombra", "MachinePistol", "00000000A25F", 2),
    "sombra_tracer_line": ("Sombra", "MachinePistol", "000000000510", 1),
    "sombra_tracer_edge": ("Sombra", "MachinePistol", "00000000050C", 1),
    "sombra_impact_star": ("Sombra", "MachinePistol", "000000001BEC", 2),
    "sombra_impact_spark": ("Sombra", "MachinePistol", "000000001BE6", 4),
    "sombra_impact_burst": ("Sombra", "MachinePistol", "00000000220A", 1),
    # Cyberspace: the weakened arrow and chevron glow, grid, slashes, square frames, soft square, inverted diamond, the
    # trail band, thin line, ring burst, plus, the cube's glow, the zone's soft and tall rain and hex noise
    "sombra_arrow": ("Sombra", "Cyberspace", "0000000066E4", 1),
    "sombra_chevron_glow": ("Sombra", "Cyberspace", "000000027D0D", 1),
    "sombra_grid": ("Sombra", "Cyberspace", "00000002E4FB", 1),
    "sombra_slashes": ("Sombra", "Cyberspace", "0000000369A3", 1),
    "sombra_frame": ("Sombra", "Cyberspace", "00000001F343", 1),
    "sombra_frame_soft": ("Sombra", "Cyberspace", "0000000183FB", 1),
    "sombra_soft_square": ("Sombra", "Cyberspace", "00000000530F", 1),
    "sombra_diamond": ("Sombra", "Cyberspace", "0000000433DB", 1),
    "sombra_trail_band": ("Sombra", "Cyberspace", "00000000DC90", 1),
    "sombra_line_thin": ("Sombra", "Cyberspace", "00000001484C", 1),
    "sombra_ring_burst": ("Sombra", "Cyberspace", "0000000017E1", 1),
    "sombra_plus": ("Sombra", "Cyberspace", "000000003DC7", 1),
    "sombra_cube_glow": ("Sombra", "Cyberspace", "000000043075", 1),
    "sombra_rain_soft": ("Sombra", "Cyberspace", "00000003FEEE", 1),
    "sombra_rain_tall": ("Sombra", "Cyberspace", "00000000A4C0", 1),
    "sombra_hex_noise": ("Sombra", "Cyberspace", "0000000299E9", 1),
    # the Translocator and stealth: the beacon's light beam, smoke streak, the stealth dissolve wipe (6), the detection
    # ring's band of squares
    "sombra_beam_v": ("Sombra", "Translocator", "00000000138F", 1),
    "sombra_smoke": ("Sombra", "Translocator", "000000003E28", 1),
    "sombra_dissolve": ("Sombra", "Translocator", "00000000A7BF", 6),
    "sombra_square_band": ("Sombra", "Translocator", "000000043D04", 1),
    # Brigitte's (tools/reference/brigitte-effects.md; research\brigitte\fx). The flail: the swing arc's streak (2 frames)
    # and its edge line, the 3P chain band, the ground hit's dust (2 frames, a colour sprite with alpha), the spark
    # streak, the energy chain's squares
    "brigitte_swing_streak": ("Brigitte", "RocketFlail", "000000014E13", 2),
    "brigitte_swing_edge": ("Brigitte", "RocketFlail", "000000014E79", 1),
    "brigitte_chain_band": ("Brigitte", "RocketFlail", "000000013DB9", 1),
    "brigitte_dust": ("Brigitte", "RocketFlail", "000000014D1C", 2),
    "brigitte_spark_streak": ("Brigitte", "RocketFlail", "000000000DA0", 2),
    "brigitte_energy_squares": ("Brigitte", "RocketFlail", "000000029D41", 1),
    # Whip Shot's head: the flash ring on mesh 47D9, the 32-frame smoke
    "brigitte_whip_flash": ("Brigitte", "WhipShot", "0000000153AF", 1),
    "brigitte_smoke": ("Brigitte", "WhipShot", "00000001DFCB", 32),
    # the barrier field (material 5B2C of look 13917, copied from the 1064F export): its frame / edge mask, hex lines,
    # the 16-frame hex flicker and the sweep line; the break: hex outline and solid hex, blue / orange shards (colour
    # sprites, 4 frames), 10-frame chips, the shield-shaped glow
    "brigitte_barrier_frame": ("Brigitte", "BarrierShield", "000000014D13", 1),
    "brigitte_barrier_hex": ("Brigitte", "BarrierShield", "00000000116E", 1),
    "brigitte_barrier_flicker": ("Brigitte", "BarrierShield", "0000000010A7", 16),
    "brigitte_barrier_sweep": ("Brigitte", "BarrierShield", "0000000010D8", 1),
    "brigitte_hex_outline": ("Brigitte", "BarrierShield", "000000002B0E", 1),
    "brigitte_hex": ("Brigitte", "BarrierShield", "000000002B10", 1),
    "brigitte_shard_blue": ("Brigitte", "BarrierShield", "000000001F36", 4),
    "brigitte_shard_orange": ("Brigitte", "BarrierShield", "000000001F37", 4),
    "brigitte_chip": ("Brigitte", "BarrierShield", "000000005DB2", 10),
    "brigitte_shield_glow": ("Brigitte", "BarrierShield", "0000000017C0", 1),
    # Shield Bash and the stun: the shield's front frame, its soft shape and glow, the hex pattern, spark arcs (2),
    # debris and smoke (colour sprites, 4 frames each), the stun's halo band and star (colour), a thin vertical line
    "brigitte_shield_front": ("Brigitte", "ShieldBash", "000000015111", 1),
    "brigitte_shield_soft": ("Brigitte", "ShieldBash", "000000015262", 1),
    "brigitte_shield_shape": ("Brigitte", "ShieldBash", "0000000153F9", 1),
    "brigitte_hex_pattern": ("Brigitte", "ShieldBash", "0000000010A2", 1),
    "brigitte_spark_arcs": ("Brigitte", "ShieldBash", "000000015455", 2),
    "brigitte_debris": ("Brigitte", "ShieldBash", "0000000018A2", 4),
    "brigitte_smoke_puff": ("Brigitte", "ShieldBash", "0000000009EF", 4),
    "brigitte_stun_halo": ("Brigitte", "ShieldBash", "00000000247C", 1),
    "brigitte_stun_star": ("Brigitte", "ShieldBash", "00000000248B", 1),
    "brigitte_line_v": ("Brigitte", "ShieldBash", "000000015438", 1),
    "brigitte_noise_fine": ("Brigitte", "ShieldBash", "0000000004D4", 1),
    # Inspire's rising wisps (on the ring mesh 296B)
    "brigitte_inspire_wisps": ("Brigitte", "Inspire", "00000001535B", 1),
    # Rally: the armour pattern's two layers, the starburst flare, ground decal disc and ring, the fresnel circle, a soft
    # square and blobs, two clouds
    "brigitte_rally_armor_a": ("Brigitte", "Rally", "0000000144E5", 1),
    "brigitte_rally_armor_b": ("Brigitte", "Rally", "000000014486", 1),
    "brigitte_flare_star": ("Brigitte", "Rally", "00000002F4F0", 1),
    "brigitte_rally_disc": ("Brigitte", "Rally", "000000014C71", 1),
    "brigitte_rally_decal_ring": ("Brigitte", "Rally", "000000014D09", 1),
    "brigitte_fresnel": ("Brigitte", "Rally", "000000018400", 1),
    "brigitte_soft_square": ("Brigitte", "Rally", "00000001DC68", 1),
    "brigitte_soft_blob": ("Brigitte", "Rally", "000000018F00", 1),
    "brigitte_cloud_a": ("Brigitte", "Rally", "00000002C977", 1),
    "brigitte_cloud_b": ("Brigitte", "Rally", "00000002C978", 1),
    # Repair Pack: the heal swirl and dot (colour sprites), the trail's flame wisps, a soft spot and blob
    "brigitte_heal_swirl": ("Brigitte", "RepairPack", "000000001268", 1),
    "brigitte_heal_dot": ("Brigitte", "RepairPack", "00000000126A", 1),
    "brigitte_flame_wisps": ("Brigitte", "RepairPack", "0000000044F7", 1),
    "brigitte_soft_spot": ("Brigitte", "RepairPack", "000000018B18", 1),
    "brigitte_soft_blob_b": ("Brigitte", "RepairPack", "0000000174D8", 1),
    # Juno's (tools/reference/juno-effects.md; research\juno\fx). The Mediblaster: soft dot, lens flare, marble noise, the
    # hit ring (4 frames), star glow, the round's line and length fade, the reload beam fade, the ribbons' torn edge, a
    # cloud noise, a soft noise
    "juno_soft_dot": ("Juno", "Mediblaster", "00000000568C", 1),
    "juno_lens_flare": ("Juno", "Mediblaster", "000000037A79", 1),
    "juno_marble": ("Juno", "Mediblaster", "000000000754", 1),
    "juno_hit_ring": ("Juno", "Mediblaster", "00000001CB5E", 4),
    "juno_star_glow": ("Juno", "Mediblaster", "00000000266F", 1),
    "juno_tracer_line": ("Juno", "Mediblaster", "00000003A512", 1),
    "juno_tracer_fade": ("Juno", "Mediblaster", "000000047DD9", 1),
    "juno_beam_fade": ("Juno", "Mediblaster", "000000048E96", 1),
    "juno_ribbon_edge": ("Juno", "Mediblaster", "00000004C003", 1),
    "juno_cloud_noise": ("Juno", "Mediblaster", "0000000299E9", 1),
    "juno_noise_soft": ("Juno", "Mediblaster", "00000002BE4A", 1),
    # Pulsar Torpedoes: the thin ring, the lock add's wisps, the pods' streak band, the damage torpedo's dripping edge
    "juno_ring_thin": ("Juno", "PulsarTorpedoes", "000000000724", 1),
    "juno_lock_wisps": ("Juno", "PulsarTorpedoes", "00000000DDE5", 1),
    "juno_streak_band": ("Juno", "PulsarTorpedoes", "000000016D78", 1),
    "juno_drip_edge": ("Juno", "PulsarTorpedoes", "00000004C002", 1),
    # Hyper Ring: the ring look's (15AD6 with 1BABB / 1BC0D, material E786 / E8C3) wavy edge and band masks, star field
    # and noise; the disc flash's band and noise, the end streak, the sparkle (4 frames), a soft ring, a noise
    "juno_ring_edge": ("Juno", "HyperRing", "00000003FE22", 1),
    "juno_ring_band": ("Juno", "HyperRing", "00000003FD49", 1),
    "juno_stars": ("Juno", "HyperRing", "00000002A907", 1),
    "juno_ring_noise": ("Juno", "HyperRing", "000000001038", 1),
    "juno_disc_band": ("Juno", "HyperRing", "0000000190EC", 1),
    "juno_disc_noise": ("Juno", "HyperRing", "0000000299DF", 1),
    "juno_streak_v": ("Juno", "HyperRing", "000000049944", 1),
    "juno_sparkle": ("Juno", "HyperRing", "00000003C068", 4),
    "juno_ring_soft": ("Juno", "HyperRing", "00000002F3EC", 1),
    "juno_noise_c": ("Juno", "HyperRing", "0000000005DF", 1),
    # Orbital Ray: the ground circle and its disc, the ground glow strip, the column's two masks, the low wall's fade,
    # the corner glow, the cast's ribbon / glow / streak / fade, the star field, the ally's chevron glows and screen rim
    "juno_ray_circle": ("Juno", "OrbitalRay", "00000004DBCC", 1),
    "juno_ray_disc": ("Juno", "OrbitalRay", "000000016E7A", 1),
    "juno_ray_ground": ("Juno", "OrbitalRay", "00000000C495", 1),
    "juno_ray_column": ("Juno", "OrbitalRay", "000000031951", 1),
    "juno_ray_column_edge": ("Juno", "OrbitalRay", "00000004DBCD", 1),
    "juno_ray_wall_fade": ("Juno", "OrbitalRay", "000000038E1D", 1),
    "juno_ray_corner": ("Juno", "OrbitalRay", "00000004F8EB", 1),
    "juno_cast_ribbon": ("Juno", "OrbitalRay", "000000012201", 1),
    "juno_cast_glow": ("Juno", "OrbitalRay", "000000042955", 1),
    "juno_cast_streak": ("Juno", "OrbitalRay", "000000001C4F", 1),
    "juno_cast_fade": ("Juno", "OrbitalRay", "000000000D9B", 1),
    "juno_star_field": ("Juno", "OrbitalRay", "00000003C723", 1),
    "juno_chevron_glow": ("Juno", "OrbitalRay", "000000027D0D", 1),
    "juno_chevron_glow_b": ("Juno", "OrbitalRay", "000000027D0A", 1),
    "juno_screen_rim": ("Juno", "OrbitalRay", "0000000212A6", 1),
    "juno_screen_noise": ("Juno", "OrbitalRay", "00000002103C", 1),
    # Overboots / Glide Boost: the jet glow dot, the glide screen's thin fade strip
    "juno_jet_glow": ("Juno", "Overboots", "000000001603", 1),
    "juno_fade_thin": ("Juno", "GlideBoost", "000000019325", 1),
    # Sojourn's (tools/reference/sojourn-effects.md; research\sojourn\fx). The Railgun: the round's comet strip, the hits'
    # 3-frame bursts and stars, the thin line glow, the Overclock vents' stripes; the Charged Shot's beam line and its hit's
    # 14-frame flash; the slide jump's tailed spark; the Disruptor's soft dot, the field's swirl, noise and flame ring
    "sojourn_round": ("Sojourn", "Railgun", "00000001FDF6", 1),
    "sojourn_burst": ("Sojourn", "Railgun", "00000001F79A", 3),
    "sojourn_star": ("Sojourn", "Railgun", "00000001F796", 3),
    "sojourn_line": ("Sojourn", "Railgun", "00000000C061", 1),
    "sojourn_vents": ("Sojourn", "Railgun", "00000002A3EB", 1),
    "sojourn_beam": ("Sojourn", "ChargedShot", "00000002A5D0", 1),
    "sojourn_flash": ("Sojourn", "ChargedShot", "00000001F8BE", 14),
    "sojourn_spark_tail": ("Sojourn", "PowerSlide", "00000000023F", 1),
    "sojourn_dot": ("Sojourn", "Disruptor", "000000004A58", 1),
    "sojourn_swirl": ("Sojourn", "Disruptor", "000000003DB8", 1),
    "sojourn_field_noise": ("Sojourn", "Disruptor", "0000000299E5", 1),
    "sojourn_flames": ("Sojourn", "Disruptor", "000000029132", 1),
    # Sigma's (tools/reference/sigma-effects.md "Masks for the pack"; research\sigmax). Hyperspheres: the launch swirl,
    # the noisy wisp, the burst's lens streak, thin streak, star, radial rays, the wispy ring; Barrier: the horizontal line,
    # the soft square, the field's rim and its hex cells (for the baked field); Accretion: the rock chunks, the crater
    # cracks; Flux: the soft disc, the reticle's rings and the vortex ring
    "sigma_swirl": ("Sigma", "Hyperspheres", "000000020126", 1),
    "sigma_wisp": ("Sigma", "Hyperspheres", "0000000200E4", 1),
    "sigma_lens": ("Sigma", "Hyperspheres", "00000001FDB2", 1),
    "sigma_streak": ("Sigma", "Hyperspheres", "00000001FBCE", 1),
    "sigma_star": ("Sigma", "Hyperspheres", "0000000208CC", 1),
    "sigma_rays": ("Sigma", "Hyperspheres", "0000000007AB", 1),
    "sigma_wisp_ring": ("Sigma", "Hyperspheres", "00000001FC54", 1),
    "sigma_lens_line": ("Sigma", "Barrier", "00000002006C", 1),
    "sigma_soft_square": ("Sigma", "Barrier", "0000000208AE", 1),
    "sigma_barrier_rim": ("Sigma", "Barrier", "00000001FE0A", 1),
    "sigma_barrier_hex": ("Sigma", "Barrier", "000000018F9B", 1),
    "sigma_rock_chunk": ("Sigma", "Accretion", "00000002C0F8", 4),
    "sigma_crater": ("Sigma", "Accretion", "00000002048A", 1),
    "sigma_disc": ("Sigma", "GraviticFlux", "000000009489", 1),
    "sigma_flux_rings": ("Sigma", "GraviticFlux", "0000000202A6", 1),
    "sigma_flux_vortex": ("Sigma", "GraviticFlux", "0000000202B3", 1),
}


# the effects' meshes: pack model name -> (hero, ability folder, model id), converted with tools/owm_static.py
# masks the game draws mirrored across their right edge into the whole icon (Lifeline's ground marker 7432: the left
# half of a paw in a crosshair, research\jetpack_cat\assets2.md): <name>_full.png, twice as wide
MIRRORED = {"cat_paw_marker"}
# masks the game multiplies on one mesh, baked into one (the particle draws a single mask): <name>.png = the product of
# the listed masks, each stretched over the same UVs. Lifeline's rings (7490, research\jetpack_cat\assets2.md): the
# 299E3 noise times the 1C7A strip, which across the torus's tube (its V) lights the outer rim and fades top and bottom
PRODUCTS = {"cat_ring_band": ("cat_ring_line", "cat_band"),
            # Mercy's wing glows (17E9 / 019C: 139C x 13CC wisps x the noise 4DB)
            "mercy_wing_glow": ("mercy_soft", "mercy_wisp", "mercy_noise"),
            # her shots' trails (707A / 7079): the gold tube tracer 18E48 x 7A9, the blue flat ribbon 299E1 x 7A9
            "cat_tracer": ("cat_cloud", "cat_fade"),
            "cat_trail_ribbon": ("cat_noise", "cat_fade"),
            # the hook's bubbles (71B1 C9, sphere DBF): the noise 299E3 x 16E9D x the thin band 6C057 along the
            # sphere's V (its latitude): a noisy ring round the sphere's middle
            "cat_bubble_band": ("cat_ring_line", "cat_cone_line", "cat_line_strip"),
            # (Purr's range wall, cat_purr_wall_band, is baked by bake_cat_flame: its noises are tiled)
            # Catnapper's aim rings (71B3 C4): the strip 1C7A x the cloud 18E48; the aim lines' widest band (71AF C14):
            # the strip 1344 x 18E48
            "cat_nap_ring": ("cat_band", "cat_cloud"),
            "cat_nap_line": ("cat_strip", "cat_cloud"),
            # the rest of #59 (work\fx_digest.txt, assets2\work\fx_digest.txt): the paw print's quads (709F C46: the
            # teardrop D89 x 18E48), the landing's ground flash (71C2 C19: 4C09 x 16E9D), the head slash's arcs (71B8
            # C8: 37021 x 299E3 x 2C41F), the dive's domes (71B8 C10: 18F00 x 18A1 x 1C7A x 299E3; C12: 55A46 x
            # 16E9D), Claws Out's marks (74D0 C13:
            # 720CE x 18E48), the landing hit's sphere (7256 C13: 16E9D x 18A1 x 18E48 x 1C7A), the dive's strip trails
            # (7163 C96 / C103: 219FA x 16E9D), the cast's bell (7373 C37: 18E48 x 299E3 x 2C41F x 54A15)
            "cat_paw_toe": ("cat_teardrop", "cat_cloud"),
            "cat_ground_flash": ("cat_soft", "cat_cone_line"),
            "cat_slash_band": ("cat_slash", "cat_ring_line", "cat_cone_noise"),
            "cat_dome_glow": ("cat_soft_noise", "cat_rope", "cat_band", "cat_ring_line"),
            "cat_dome_band": ("cat_dome_strip", "cat_cone_line"),
            "cat_claw_marks": ("cat_claws", "cat_cloud"),
            "cat_shock_sphere": ("cat_cone_line", "cat_rope", "cat_cloud", "cat_band"),
            "cat_dive_band": ("cat_trail", "cat_cone_line"),
            "cat_bell_band": ("cat_cloud", "cat_ring_line", "cat_cone_noise", "cat_bell_line"),
            # the Tree of Life's wave shell (47E2, tools/reference/lifeweaver-effects.md): bokeh 187EB x vertical fade
            # 3E4E5 x wisps 2FCA7. The bokeh alone is grey all over: on the sphere round him it tinted the whole view
            "lw_tree_wave": ("lw_bokeh", "lw_fade_v", "lw_wisp"),
            # Ana's scoped tracer (1A80, tools/reference/ana-effects.md): its line 0B35 x its length fade 2A063
            "ana_tracer": ("ana_beam_line", "ana_fade_v"),
            # Sombra's pistol tracer (1998, tools/reference/sombra-effects.md): its line 510 x its edge 50C
            "sombra_tracer": ("sombra_tracer_line", "sombra_tracer_edge"),
            # Brigitte's (tools/reference/brigitte-effects.md): the swing arc's streak 14E13 x edge line 14E79 (26EB),
            # Rally's armour pattern 144E5 x 14486 (219D C104), the pack trail's spot 18B18 x flame wisps 44F7 (218D C13)
            "brigitte_swing_band": ("brigitte_swing_streak_0", "brigitte_swing_edge"),
            "brigitte_rally_armor": ("brigitte_rally_armor_a", "brigitte_rally_armor_b"),
            "brigitte_pack_trail": ("brigitte_soft_spot", "brigitte_flame_wisps"),
            # Juno's (tools/reference/juno-effects.md): the Mediblaster round 3A512 x 47DD9 (53B6 C8 / C10), the ally
            # torpedo's trail 2FCA7 x 7A9 (51E8 C39 / C47), the damage torpedo's trail 299E3 x 4C003 x 7A9 (5940 C15)
            "juno_tracer": ("juno_tracer_line", "juno_tracer_fade"),
            "juno_heal_trail": ("lw_wisp", "cat_fade"),
            "juno_dmg_trail": ("cat_ring_line", "juno_ribbon_edge", "cat_fade"),
            # Sojourn's field decal (2228 C21, tools/reference/sojourn-effects.md): the noise 299E5 x the swirl 3DB8
            "sojourn_field_decal": ("sojourn_field_noise", "sojourn_swirl"),
            # Sigma's (tools/reference/sigma-effects.md): his renderers multiply two masks where one alone is a full-frame
            # noise that drew as a square on a quad (found playing): the launch cone's wisp 200E4 rounded by the soft glow
            # 4C09 (ours: on its cone mesh CA08 the band 1C7A shapes it), the barrier's corner motes and the rock's flash
            # 4DB x 1FC54, the burst's flash 208CC x 7AB, the send streaks 16E9D x 4C09, Flux's glows on him 4C09 x 4DB
            "sigma_wisp_soft": ("sigma_wisp", "cat_soft"),
            "sigma_mote": ("mercy_noise", "sigma_wisp_ring"),
            "sigma_burst_flash": ("sigma_star", "sigma_rays"),
            "sigma_send_streak": ("cat_cone_line", "cat_soft"),
            "sigma_flux_glow": ("cat_soft", "mercy_noise"),
            # Levitation's streaks (65BF: 299E1 x 299E8 x A7B6 on its half-shell 963D): the noise rounded by the glow A7B6
            "sigma_levitate": ("cat_noise", "glow")}

# products stretched to full brightness (ours: the data's colours are HDR, and three masks multiplied left the hook's
# bubbles at a third, unseen in play)
STRETCHED = {"mercy_wing_glow", "cat_bubble_band", "cat_slash_band", "cat_dome_glow", "cat_shock_sphere", "cat_bell_band"}

EFFECT_MODELS = {"torii": ("Kiriko", "KitsuneRush", "00000001048D"),
                 # Jetpack Cat's effect meshes (research\jetpack_cat\assets2.md 2): Lifeline's and Catnapper's ground
                 # rings (a fat torus lying flat), the hook's cone, the towed ally's arcs, Purr's sphere, torus and wall
                 "cat_torus": ("JetpackCat", "Lifeline", "000000015A26"),
                 "cat_cone": ("JetpackCat", "Lifeline", "00000001B972"),
                 "cat_half_ring": ("JetpackCat", "Lifeline", "000000014874"),
                 # the hook's bubbles' sphere (71B1 C9)
                 "cat_bubble": ("JetpackCat", "Lifeline", "000000000DBF"),
                 "cat_sphere": ("JetpackCat", "Purr", "000000001385"),
                 "cat_purr_torus": ("JetpackCat", "Purr", "00000001BD54"),
                 "cat_purr_wall": ("JetpackCat", "Purr", "000000019181"),
                 # the jetpack's flame tube (7064 / 7373 / 7163), the cast's bell (7373 C37), the head slash's arc and
                 # dome (71B8 C8 / C10)
                 "cat_flame": ("JetpackCat", "Catnapper", "00000001B580"),
                 "cat_bell": ("JetpackCat", "Catnapper", "000000019370"),
                 "cat_slash_arc": ("JetpackCat", "Catnapper", "0000000152B9"),
                 "cat_dome": ("JetpackCat", "Catnapper", "00000001B2FF"),
                 # Lifeweaver's (tools/reference/lifeweaver-effects.md "Meshes"): the thorn, the platform's bloom outline,
                 # the vine's coil (the tree's growth vine too), Life Grip's bud on the ally and the flower at its point
                 "lw_thorn": ("Lifeweaver", "ThornVolley", "0000000122CA"),
                 "lw_flower_ring": ("Lifeweaver", "PetalPlatform", "0000000122DA"),
                 "lw_vine_coil": ("Lifeweaver", "LifeGrip", "000000012262"),
                 "lw_bud": ("Lifeweaver", "LifeGrip", "000000011D57"),
                 "lw_flower": ("Lifeweaver", "LifeGrip", "000000011D6E"),
                 # Ana's (tools/reference/ana-effects.md): the boost / anti-heal plus (0.072 m wide, flat, centred), the
                 # enemy hit's lens disc (1.988 m wide, 0.386 tall, base 0.177 below its centre point), Local
                 # Anesthetic's flattened sphere (0.97 wide, 0.588 tall, centred: lower by 0.294 x size)
                 "ana_plus": ("Ana", "BioticGrenade", "00000000E0AA"),
                 "ana_lens_disc": ("Ana", "BioticRifle", "00000000C943"),
                 "ana_shell": ("Ana", "SleepDart", "000000015CAE"),
                 # Sombra's (tools/reference/sombra-effects.md "Meshes"): the hack's curved code panel, the six-square
                 # flower, EMP's barrel band and arc, Cyberspace's hourglass tube, chevron and data cube, the flat ring,
                 # the open bowl and the detection ring's open cylinder
                 "sombra_panel_arc": ("Sombra", "Hack", "000000003BD6"),
                 "sombra_square_flower": ("Sombra", "EMP", "000000003A16"),
                 "sombra_band": ("Sombra", "EMP", "000000003BC3"),
                 "sombra_arc": ("Sombra", "EMP", "0000000039F8"),
                 "sombra_hourglass": ("Sombra", "Cyberspace", "00000001A988"),
                 "sombra_chevron": ("Sombra", "Cyberspace", "00000000DFDC"),
                 "sombra_cube": ("Sombra", "Cyberspace", "000000013EE4"),
                 "sombra_ring": ("Sombra", "EMP", "0000000011C1"),
                 "sombra_bowl": ("Sombra", "EMP", "0000000011C3"),
                 "sombra_cylinder": ("Sombra", "Translocator", "00000000129B"),
                 # Brigitte's (tools/reference/brigitte-effects.md "Meshes"): the flail's swing arcs (forehand 5F3F,
                 # backhand 5F42: a 6.5 m horseshoe band), the shield-shaped plate 5DF1 (the bash imprint, the break),
                 # the half dome 5F08 (Rally, the pack's arrival), the open band DE0 (the stun halo), Inspire's flat ring
                 # 296B, the shard B18, the whip head's ring 47D9 and Rally's flat ring 5BEE
                 "brigitte_swing_arc": ("Brigitte", "RocketFlail", "000000005F3F"),
                 "brigitte_swing_arc_back": ("Brigitte", "RocketFlail", "000000005F42"),
                 "brigitte_shield_plate": ("Brigitte", "ShieldBash", "000000005DF1"),
                 "brigitte_dome": ("Brigitte", "Rally", "000000005F08"),
                 "brigitte_band": ("Brigitte", "ShieldBash", "000000000DE0"),
                 "brigitte_ring": ("Brigitte", "Inspire", "00000000296B"),
                 "brigitte_shard": ("Brigitte", "ShieldBash", "000000000B18"),
                 "brigitte_whip_ring": ("Brigitte", "WhipShot", "0000000047D9"),
                 "brigitte_rally_ring": ("Brigitte", "Rally", "000000005BEE"),
                 # Juno's (tools/reference/juno-effects.md "Meshes"): the Hyper Ring 15AD6 (0.37 m ring, scale 25 = 9.25 m),
                 # the beam column 15A21 (0.9 x 5.6 m tube), the ray's low wall 1F85, the cast cone EEB, the torpedo's half
                 # shell 160EA, the pods' star burst 12F7D, a sphere D721, a thin torus EBEF, the flat disc 5E5E, the cast
                 # blade 14063 (the fat torus 15A26 is cat_torus already)
                 "juno_ring": ("Juno", "HyperRing", "000000015AD6"),
                 "juno_column": ("Juno", "OrbitalRay", "000000015A21"),
                 "juno_ray_wall": ("Juno", "OrbitalRay", "000000001F85"),
                 "juno_cone": ("Juno", "OrbitalRay", "000000000EEB"),
                 "juno_half_shell": ("Juno", "PulsarTorpedoes", "0000000160EA"),
                 "juno_pod_burst": ("Juno", "PulsarTorpedoes", "000000012F7D"),
                 "juno_sphere": ("Juno", "HyperRing", "00000000D721"),
                 "juno_torus_thin": ("Juno", "OrbitalRay", "00000000EBEF"),
                 "juno_disc": ("Juno", "HyperRing", "000000005E5E"),
                 "juno_cast_blade": ("Juno", "OrbitalRay", "000000014063")}


def bake_cat_arcs(out):
    """Jetpack Cat's arcs round the towed ally (Lifeline 7454, research\\jetpack_cat\\work\\fx_digest.txt): the half
    ring 14874 is planar-mapped (u = x + 0.5 across its 1 m, v from its top), a rectangle with a half disc cut out
    (radius 0.362 of its 0.5), so the masks round it. Each renderer layers four: C6 (the 0.35 s flash) the cloud 18E48,
    the round edge 71226, the noise 18F00 twice and the strip BA19; C8 (the 1 s lines) 71226, the noise 299E3, 18F00
    twice and the strip 39F16. The shader isn't decoded, so how they lie is read from their shapes (M): 71226 (a disc's
    edge round its corner) laid on each quarter round the arc's centre, one minus (the arc's outer rounding); the 4 x
    256 strips are profiles across the band, from the cut edge out; the noises at the mesh's UVs. The product is
    stretched to full brightness (ours: the data's colours are HDR). Writes cat_arc_flash.png and cat_arc_lines.png."""
    import math
    from PIL import Image
    names = ("cat_arc_edge", "cat_cloud", "cat_soft_noise", "cat_ring_line", "cat_arc_strip", "cat_arc_stripes")
    if not all((out / f"{n}.png").exists() for n in names):
        return 0
    m = {n: Image.open(out / f"{n}.png").convert("L") for n in names}

    def sample(name, u, v):
        im = m[name]
        x = min(max(int(u * im.width), 0), im.width - 1)
        y = min(max(int(v * im.height), 0), im.height - 1)
        return im.getpixel((x, y)) / 255

    inner, n = 0.362, 256
    made = 0
    for name, cloud, strip in (("cat_arc_flash", "cat_cloud", "cat_arc_strip"), ("cat_arc_lines", "cat_ring_line", "cat_arc_stripes")):
        values = []
        for y in range(n):
            for x in range(n):
                u, v = (x + 0.5) / n, (y + 0.5) / n
                mx, my = u - 0.5, 0.5 - v
                r = math.hypot(mx, my)
                if r < inner - 0.01:
                    values.append(0.0)
                    continue
                edge = 1 - sample("cat_arc_edge", abs(mx) * 2, 1 - abs(my) * 2)
                across = sample(strip, 0.5, (r - inner) / (0.5 - inner))
                noise = sample("cat_soft_noise", u, v)
                values.append(sample(cloud, u, v) * edge * noise * noise * across)
        top = max(values) or 1
        img = Image.new("L", (n, n))
        img.putdata([int(255 * min(1.0, val / top)) for val in values])
        img.save(out / f"{name}.png")
        made += 1
    return made


def bake_sigma_barrier(out):
    """Sigma's barrier field (material 7C75 on C015, whose UVs run 0-1 over the whole barrier: tools/reference/sigma-effects.md):
    the shader's soft rounded-rectangle rim 1FE0A stretched over it, plus its hex cells 18F9B tiled across it (4 across, 2.5
    down: the 6.7 x 4.18 m barrier's cells kept round; the shader's tiling isn't readable, ours) as a faint shimmer, and a dim
    floor so the middle still reads. Written as sigma_barrier_field.png (640 x 400), drawn through the LUT's rows."""
    from PIL import Image
    if not all((out / f"{n}.png").exists() for n in ("sigma_barrier_rim", "sigma_barrier_hex")):
        return 0
    rim = Image.open(out / "sigma_barrier_rim.png").convert("L").resize((640, 400), Image.BILINEAR)
    hexes = Image.open(out / "sigma_barrier_hex.png").convert("L")
    tile = hexes.resize((160, 160), Image.BILINEAR)
    values = []
    rp, tp = rim.load(), tile.load()
    for y in range(400):
        for x in range(640):
            r = rp[x, y] / 255
            h = tp[x % 160, y % 160] / 255
            values.append(min(1.0, 0.06 + 0.9 * r + 0.12 * h))
    img = Image.new("L", (640, 400))
    img.putdata([int(255 * v) for v in values])
    img.save(out / "sigma_barrier_field.png")
    return 1


def bake_cat_flame(out):
    """Jetpack Cat's flame tube (1B580 in 7064 / 7373 / 7163, research\jetpack_cat\workx_digest.txt): the tube's V runs
    from the nozzle (0) to its tip (1), its U once round it. Its masks multiplied: the strip 2C41F turned (its material
    row r1 (0, -1, 1): v' = 1 - v, M) so it's bright at the nozzle, the fade 7A9 along V, and the noise 299E3 at the
    row r3's scale (2 round, 0.25 along) scrolling along the tube (r3's -10: ours, 8 frames over one noise period,
    looped by the mod), stretched to full brightness. Writes cat_flame_tube_0..7.png. Also Transport Shielding's hexes
    (5513 C3): the grid 18F9B at its row r1's tiling (4.75 x 4) times the edge 18400: cat_hex_frame.png."""
    from PIL import Image
    names = ("cat_cone_noise", "cat_fade", "cat_ring_line", "cat_hex", "cat_hex_edge", "cat_band", "cat_soft_noise",
             "cat_glow_noise")
    if not all((out / f"{n}.png").exists() for n in names):
        return 0
    m = {n: Image.open(out / f"{n}.png").convert("L") for n in names}

    def sample(name, u, v):
        im = m[name]
        return im.getpixel((int(u % 1 * im.width) % im.width, int(v % 1 * im.height) % im.height)) / 255

    made = 0
    w, h, frames = 64, 128, 8
    for k in range(frames):
        values = []
        for y in range(h):
            v = (y + 0.5) / h
            along = sample("cat_cone_noise", 0.5, min(1 - v, 0.999)) * sample("cat_fade", 0.5, min(v, 0.999))
            for x in range(w):
                u = (x + 0.5) / w
                values.append(along * sample("cat_ring_line", u * 2, v * 0.25 + k / frames))
        top = max(values) or 1
        img = Image.new("L", (w, h))
        img.putdata([int(255 * min(1.0, val / top)) for val in values])
        img.save(out / f"cat_flame_tube_{k}.png")
        made += 1
    n = 256
    img = Image.new("L", (n, n))
    img.putdata([int(255 * sample("cat_hex", (x + 0.5) / n * 4.75, (y + 0.5) / n * 4)
                     * sample("cat_hex_edge", (x + 0.5) / n, (y + 0.5) / n)) for y in range(n) for x in range(n)])
    img.save(out / "cat_hex_frame.png")
    # Purr's range wall (70F9 C4, the cylinder 19181, V 0 at its top 4 m over her to 1 at 16 m under her): its strip 1C7A
    # along V, the noises 18F00 and 19167 at the 12BB row r0's tiling (4 x 4), and its 65BC row r4 read as strength
    # along V (M: its R and G each a bump, R at 0.4 and G at 0.6 of V, 0.07 / 0.01 at her own height): the wall is lit
    # 4-12 m under her, the ground under her as she flies, and faint at her height
    r4 = ((0, 0.07, 1.0, 0, 0, 0), (0, 0.01, 0.11, 1.0, 0.13, 0))

    def strength(v):
        k = min(int(v * 5), 4)
        f = v * 5 - k
        return min(1.0, sum(row[k] + (row[k + 1] - row[k]) * f for row in r4))

    m["cat_band"] = Image.open(out / "cat_band.png").convert("L")
    m["cat_soft_noise"] = Image.open(out / "cat_soft_noise.png").convert("L")
    m["cat_glow_noise"] = Image.open(out / "cat_glow_noise.png").convert("L")
    w, h = 512, 256
    img = Image.new("L", (w, h))
    img.putdata([int(255 * sample("cat_band", 0.5, (y + 0.5) / h) * strength((y + 0.5) / h)
                     * sample("cat_soft_noise", (x + 0.5) / w * 4, (y + 0.5) / h * 4)
                     * sample("cat_glow_noise", (x + 0.5) / w * 4, (y + 0.5) / h * 4)) for y in range(h) for x in range(w)])
    img.save(out / "cat_purr_wall_band.png")
    return made + 2


def add_effects(*sources, only=""):
    """Copies the effects' masks into assets/overcraft/textures/effect/ (flipbooks as <name>_<frame>.png), and
    converts their meshes (the torii gate) into hero_models/. Each from the first source folder that has it: Jetpack
    Cat's research pass wrote hers outside the read-only extraction (research\jetpack_cat\fx). With only, just the
    names starting with it (one hero's files into a pack other heroes' work shares: --only cat_)."""
    write_mcmeta()
    out = PACK / "assets" / "overcraft" / "textures" / "effect"
    out.mkdir(parents=True, exist_ok=True)
    copied = 0
    for name, (hero, ability, texture, frames) in EFFECT_MASKS.items():
        if not name.startswith(only):
            continue
        for frame in range(frames):
            suffix = "" if frame == 0 else f"_{frame}"
            src = next((Path(source) / hero / ability / "textures" / f"{texture}{suffix}.png" for source in sources
                        if (Path(source) / hero / ability / "textures" / f"{texture}{suffix}.png").exists()),
                       Path(sources[0]) / hero / ability / "textures" / f"{texture}{suffix}.png")
            if not src.exists():
                print(f"missing {name}: {src}")
                continue
            dest = out / (name + ("" if frames == 1 else f"_{frame}") + ".png")
            shutil.copyfile(src, dest)
            copied += 1
            if name in MIRRORED:
                from PIL import Image, ImageOps
                half = Image.open(dest)
                whole = Image.new(half.mode, (half.width * 2, half.height))
                whole.paste(half, (0, 0))
                whole.paste(ImageOps.mirror(half), (half.width, 0))
                whole.save(out / f"{name}_full.png")
    for name, parts in PRODUCTS.items():
        if name.startswith(only) and all((out / f"{part}.png").exists() for part in parts):
            from PIL import Image, ImageChops
            masks = [Image.open(out / f"{part}.png").convert("L") for part in parts]
            size = (max(m.width for m in masks), max(m.height for m in masks))
            product = masks[0].resize(size, Image.BILINEAR)
            for mask in masks[1:]:
                product = ImageChops.multiply(product, mask.resize(size, Image.BILINEAR))
            if name in STRETCHED:
                top = product.getextrema()[1] or 1
                product = product.point(lambda v: min(255, v * 255 // top))
            product.save(out / f"{name}.png")
            copied += 1
    if "cat_".startswith(only) or only.startswith("cat_"):
        copied += bake_cat_arcs(out)
        copied += bake_cat_flame(out)
    if "sigma_".startswith(only) or only.startswith("sigma_"):
        copied += bake_sigma_barrier(out)
    print(f"{copied} effect textures -> {out}")
    for name, (hero, ability, model) in EFFECT_MODELS.items():
        if not name.startswith(only):
            continue
        src = next((Path(source) / hero / ability / "models" / "Models" / f"{model}.00C" / f"{model}.owmdl"
                    for source in sources
                    if (Path(source) / hero / ability / "models" / "Models" / f"{model}.00C" / f"{model}.owmdl").exists()),
                   Path(sources[0]) / hero / ability / "models" / "Models" / f"{model}.00C" / f"{model}.owmdl")
        if not src.exists():
            print(f"missing {name}: {src}")
            continue
        dest = MODELS / f"{name}.glb"
        cmd = [blender(), "--background", "--factory-startup", "--python", str(ROOT / "tools" / "owm_static.py"), "--",
               str(src), str(dest)]
        result = subprocess.run(cmd, capture_output=True, text=True)
        report = [line for line in result.stdout.splitlines() if line.startswith("OWM_")]
        print("\n".join(report) or result.stderr[-2000:])


def add_props(source, only=None):
    """Converts every prop the heroes' emote data names (src/main/resources/assets/overcraft/emotes/<hero>.json, from
    tools/emote_data.py: techniques/emotes/emote_reproduction.md) into hero_models/prop_<model id, lower case>.glb, with the prop's own clips named by their
    ids, as the data names them. <source> is the extraction root holding Unlocks/Heroes/<Hero>/Emote. A prop with no
    clip of its own is a static mesh (like the torii gate); one with clips is exported with them, staged beside the
    model from its own Animations folder or the emote's (the fox's and the pod's are in the emote's)."""
    import json
    write_mcmeta()
    MODELS.mkdir(parents=True, exist_ok=True)
    for data in sorted((ROOT / "src" / "main" / "resources" / "assets" / "overcraft" / "emotes").glob("*.json")):
        hero = json.loads(data.read_text(encoding="utf8"))
        if only and hero["hero"] != only:
            # props <source> <hero>: just that hero's
            continue
        emotes = Path(source) / "Unlocks" / "Heroes" / HERO_FOLDERS.get(hero["hero"], hero["hero"].capitalize()) / "Emote"
        props = {}
        for emote in hero["emotes"].values():
            for clip in emote["clips"].values():
                for spawn in clip["spawns"]:
                    if spawn.get("model"):
                        clips = props.setdefault(spawn["model"], set())
                        if spawn.get("clip"):
                            clips.add(spawn["clip"])
        for model, clips in sorted(props.items()):
            name = f"prop_{model.lower()}"  # resource paths must be lower case
            padded = "%012X" % int(model, 16)
            found = sorted(emotes.glob(f"*/*/Models/{padded}.00C/{padded}.owmdl"))
            if not found:
                # a prop that is a child of the hero entity (Ana's teacup): in the hero's own skin folder
                found = sorted((Path(source) / "Heroes" / emotes.parent.name / "Skin").glob(
                    f"*/*/Models/{padded}.00C/{padded}.owmdl"))
            if not found:
                print(f"{name}: no model {padded} under {emotes}")
                continue
            src = found[0]
            dest = MODELS / f"{name}.glb"
            if not clips:
                cmd = [blender(), "--background", "--factory-startup", "--python", str(ROOT / "tools" / "owm_static.py"),
                       "--", str(src), str(dest)]
                result = subprocess.run(cmd, capture_output=True, text=True)
                report = [line for line in result.stdout.splitlines() if line.startswith("OWM_")]
                print(name + ": " + (" ".join(report) or result.stderr[-2000:]))
                continue
            work = LOCAL / "work" / name
            staged = work / "model"
            if staged.exists():
                # (the copy keeps the archive's read-only flags)
                shutil.rmtree(staged, onerror=lambda f, path, _e: (os.chmod(path, 0o666), f(path)))
            shutil.copytree(src.parent, staged)
            renames = {}
            for cid in sorted(clips):
                cpad = "%012X" % int(cid, 16)
                where = [p for p in staged.glob(f"Animations/**/{cpad}.owanimclip")]
                if not where:
                    for clip_src in emotes.glob(f"*/*/Animations/**/{cpad}.owanimclip"):
                        # <group>/<emote>/Animations/<folder>/... -> the model's Animations/<folder>/...
                        target = staged / Path(*clip_src.relative_to(emotes).parts[2:])
                        target.parent.mkdir(parents=True, exist_ok=True)
                        shutil.copyfile(clip_src, target)
                        where = [target]
                        break
                if not where:
                    print(f"{name}: no clip {cpad}")
                    continue
                folder = where[0].parent.relative_to(staged / "Animations").as_posix().replace("/", "-")
                renames[f"{folder}_{cpad}"] = cid
            cmd = [blender(), "--background", "--factory-startup", "--python", str(ROOT / "tools" / "owm_survey.py"), "--",
                   str(staged / src.name), str(work), "--glb", "--only=" + ",".join("%012X" % int(c, 16) for c in clips)]
            result = subprocess.run(cmd, capture_output=True, text=True)
            glb = work / f"{padded}.glb"
            if not glb.exists():
                print(f"{name}: export failed: {result.stderr[-2000:]}")
                continue
            # a prop another hero's emotes share keeps that hero's clips (Sombra's B9A7 and Sigma's AE48 on 1024E)
            before = work / f"{name}.before.glb"
            if dest.exists():
                shutil.copyfile(dest, before)
            shutil.copyfile(glb, dest)
            subprocess.run([sys.executable, str(ROOT / "tools" / "rename_glb_clips.py"), str(dest)]
                           + [f"{a}={b}" for a, b in renames.items()], check=True)
            if before.exists():
                subprocess.run([sys.executable, str(ROOT / "tools" / "merge_glb_clips.py"), str(dest), str(before)], check=True)
                before.unlink()
            print(f"{name}: {dest} (clips {', '.join(sorted(renames.values()))})")


def add_parts(source, only=None):
    """Converts every model src/main/resources/assets/overcraft/weapon_parts.json names by id (techniques/animation/weapon_parts.md)
    into hero_models/<model>.glb, a static mesh on its origin. <source> is the extraction root holding Heroes/<Hero>/Skin. Then
    tools/glow_maps.py for their glows."""
    import json
    write_mcmeta()
    MODELS.mkdir(parents=True, exist_ok=True)
    table = json.loads((ROOT / "src" / "main" / "resources" / "assets" / "overcraft" / "weapon_parts.json").read_text(encoding="utf8"))
    for hero, parts in table.items():
        if hero == "_" or (only and hero != only) or hero not in HERO_FOLDERS:
            continue
        done = set()
        for part in parts:
            if "id" not in part or part["model"] in done:
                continue
            done.add(part["model"])
            padded = "%012X" % int(part["id"], 16)
            found = sorted((Path(source) / "Heroes" / HERO_FOLDERS[hero] / "Skin").glob(f"*/*/Models/{padded}.00C/{padded}.owmdl"))
            if not found:
                print(f"{part['model']}: no model {padded} under {source}")
                continue
            dest = MODELS / f"{part['model']}.glb"
            cmd = [blender(), "--background", "--factory-startup", "--python", str(ROOT / "tools" / "owm_static.py"), "--",
                   str(found[0]), str(dest)]
            result = subprocess.run(cmd, capture_output=True, text=True)
            report = [line for line in result.stdout.splitlines() if line.startswith("OWM_")]
            print(part["model"] + ": " + (" ".join(report) or result.stderr[-2000:]))


def clip_ids(clip_map):
    """The clip ids a clip map names."""
    import re
    return [m.group(2) for line in clip_map.read_text(encoding="utf8").splitlines() for m in [re.match(r"\s*(\d+-\d+|emote)_([0-9A-F]+)\s*=", line)] if m]


def add_clips(source, only=None):
    """The clips-only files (techniques/animation/animation_import.md): each clip map named *_clips.txt, exported from the model folder that holds its first clip. Its emote_* clips come
    from the hero's emote folders."""
    write_mcmeta()
    MODELS.mkdir(parents=True, exist_ok=True)
    for clip_map in sorted((ROOT / "tools" / "clipmaps").glob("*_clips.txt")):
        name = clip_map.stem
        hero = name[:-len("_1p_clips")] if name.endswith("_1p_clips") else name[:-len("_clips")]
        if hero not in HERO_FOLDERS or (only and hero != only):
            continue
        ids = clip_ids(clip_map)
        skin = Path(source) / "Heroes" / HERO_FOLDERS[hero] / "Skin"
        if not skin.is_dir():
            continue
        # a map with no clips of its own only takes the emotes (--emotes): from the hero's model, found by the hero's own map
        look_for = ids or clip_ids(ROOT / "tools" / "clipmaps" / f"{hero}.txt")
        folder = next((p.parents[3] for clip in look_for for p in skin.glob(f"*/*/Models/*/Animations/*/*/{clip}.owanimclip")), None)
        if folder is None:
            print(f"{name}: no clip of its map under {skin}")
            continue
        model = next(folder.glob("*.owmdl"))
        work = LOCAL / "work" / name
        cmd = [blender(), "--background", "--factory-startup", "--python", str(ROOT / "tools" / "owm_survey.py"), "--", str(model), str(work), "--glb", "--clips-only",
               f"--keep={clip_map}", "--only=" + ",".join(ids or ["none"])]
        emotes = Path(source) / "Unlocks" / "Heroes" / HERO_FOLDERS[hero] / "Emote"
        if emotes.is_dir():
            cmd.append(f"--clips-from={emotes}" if ids else f"--emotes={emotes}")
        result = subprocess.run(cmd, capture_output=True, text=True)
        out = work / f"{model.stem}.glb"
        report = [line for line in result.stdout.splitlines() if line.startswith(("OWM_KEEP", "OWM_GLB"))]
        print(name + ": " + (" ".join(report) or result.stderr[-1000:]))
        if out.exists():
            shutil.copyfile(out, MODELS / f"{name}.glb")


def add_glows(*sources):
    subprocess.run([sys.executable, str(ROOT / "tools" / "glow_maps.py"), *sources], check=True, env=dict(os.environ, OC_ASSET_PACK=str(PACK)))


def missing():
    """What the pack lacks of this build's weapon work: the zip goes out either way, but a pack without these shows the player none of it (critique_parts F)."""
    import json
    table = json.loads((ROOT / "src" / "main" / "resources" / "assets" / "overcraft" / "weapon_parts.json").read_text(encoding="utf8"))
    gaps = [f"hero_models/{row['model']}.glb" for rows in table.values() if isinstance(rows, list) for row in rows if not (MODELS / f"{row['model']}.glb").exists()]
    for hero in sorted(HERO_FOLDERS):
        if (MODELS / f"{hero}.glb").exists() and not (MODELS / f"{hero}_glow.json").exists():
            gaps.append(f"hero_models/{hero}_glow.json")
    for name in ("mercy_clips", "jetpack_cat_clips", "lifeweaver_1p_clips"):
        if not (MODELS / f"{name}.glb").exists():
            gaps.append(f"hero_models/{name}.glb")
    if not (MODELS / "colour" / "000000008052.png").exists():
        gaps.append("hero_models/colour/000000008052.png (glows)")
    if not (PACK / "assets" / "overcraft" / "textures" / "effect" / "mercy_wing_glow.png").exists():
        gaps.append("textures/effect/mercy_wing_glow.png (effects)")
    return sorted(set(gaps))


def build_version():
    """This build's version (gradle.properties)."""
    for line in (ROOT / "gradle.properties").read_text(encoding="utf8").splitlines():
        if line.startswith("version="):
            return line.split("=", 1)[1].strip()
    return "0"


def write_version():
    """assets/overcraft/asset_pack.json: the build this pack was made with, which the game checks against the build it
    runs (Overcraft.ASSETS_NEEDED): an older pack is flagged in game (#21)."""
    import datetime
    import json
    out = PACK / "assets" / "overcraft" / "asset_pack.json"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps({"version": build_version(), "built": datetime.date.today().isoformat()}) + "\n", encoding="utf8")


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
        add_ui(sys.argv[2], sys.argv[3] if len(sys.argv) > 3 else None)
    elif cmd == "sounds":
        add_sounds(sys.argv[2])
    elif cmd == "fonts":
        add_fonts(sys.argv[2])
    elif cmd == "effects":
        args = sys.argv[2:]
        only = ""
        if args[:1] == ["--only"]:
            only, args = args[1], args[2:]
        add_effects(*args, only=only)
    elif cmd == "props":
        add_props(sys.argv[2], sys.argv[3] if len(sys.argv) > 3 else None)
    elif cmd == "parts":
        add_parts(sys.argv[2], sys.argv[3] if len(sys.argv) > 3 else None)
    elif cmd == "clips":
        add_clips(sys.argv[2], sys.argv[3] if len(sys.argv) > 3 else None)
    elif cmd == "glows":
        add_glows(*sys.argv[2:])
    elif cmd == "weapons":
        for root in sys.argv[2:]:
            add_parts(root)
            add_clips(root)
        add_glows(*sys.argv[2:])
        print("missing:", missing() or "nothing")
    elif cmd == "zip":
        if not PACK.exists():
            sys.exit("no pack yet: add a model first")
        gaps = missing()
        if gaps:
            print("WARNING, this pack lacks:", *gaps, "(py tools/build_asset_pack.py weapons <roots>)", sep="\n  ")
        write_version()
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
