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
    py tools/build_asset_pack.py effects <dir> [<dir>...]  the ability effects' texture masks, from the effects
                                                            extraction (<dir>/Kiriko/<Ability>/textures/<id>.png); each
                                                            mask from the first folder that has it (Jetpack Cat's are in
                                                            "E:\Overwatch Archive\research\jetpack_cat\fx")
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
HEROES = {"kiriko", "lucio", "sombra", "illari", "juno", "sojourn", "jetpack_cat", "mercy"}
# DataTool's folder for each hero (its in-game name)
HERO_FOLDERS = {"kiriko": "Kiriko", "lucio": "Lúcio", "sombra": "Sombra", "illari": "Illari", "juno": "Juno",
                "sojourn": "Sojourn", "jetpack_cat": "Jetpack Cat", "mercy": "Mercy"}
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
    feed = PACK / "assets" / "overcraft" / "textures" / "gui" / "killfeed"
    for name, texture in KILLFEED_ICONS.items():
        png = dump / f"{texture}.png"
        if png.exists():
            feed.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(png, feed / f"{name}.png")
            (feed / f"{name}.png.mcmeta").write_text('{"texture": {"blur": true}}\n', encoding="utf8")
        else:
            print(f"no kill feed {name} icon ({png})")


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
VOICELINE_HEROES = {"kiriko": "Kiriko", "lucio": "Lúcio", "mercy": "Mercy"}
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
    """The spoken text DataTool puts in a voice file's name ("<id>.0B2-<text>.ogg"; "?" became "_")."""
    text = file_name.split(".0B2-", 1)[-1].rsplit(".", 1)[0]
    return text.replace("(Japanese)_ ", "(Japanese) ").replace("_", "?")


def add_voicelines(source, out, sounds):
    """Each hero's collectible voice lines (the communication wheel's voice page): DataTool's extract-unlocks
    "<Hero>|voiceline=*" puts them under Unlocks/Heroes/<Hero>/VoiceLine/<season>/<unlock name>/<id>.ogg, several files
    for an unlock with several takes ("Tarot Reading"). One sound event per unlock, "<hero>.voiceline.<slug>", its
    subtitle the spoken text (the unlock's name when it has several), and assets/overcraft/voicelines/<hero>.json
    listing them for the game. Returns the pack's lang entries (the unlock names and subtitles)."""
    import json
    lang = {}
    for hero, folder in VOICELINE_HEROES.items():
        base = Path(source) / "Unlocks" / "Heroes" / folder / "VoiceLine"
        spoken = {}
        for f in (Path(source) / "HeroVoice" / folder / "Voicelines").glob("*.ogg"):
            spoken[f.name.split(".", 1)[0]] = voice_text(f.name)
        if not base.exists():
            print(f"no voice line unlocks for {hero}: run DataTool extract-unlocks <out>/Unlocks with {folder}|voiceline=*")
            continue
        listed = []
        for unlock in sorted(d for d in base.glob("*/*") if d.is_dir()):
            name = unlock.name.replace("_", "?")
            key = slug(name)
            if key in {e["id"] for e in listed}:
                continue
            files = sorted(unlock.glob("*.ogg"))
            if not files:
                continue
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
    # Frenetic Flight and the jetpack: nozzle hot dot, trail strip, spark blobs
    "cat_glow_dot": ("JetpackCat", "FreneticFlight", "00000000A17F", 1),
    "cat_trail": ("JetpackCat", "FreneticFlight", "0000000219FA", 1),
    "cat_blob": ("JetpackCat", "FreneticFlight", "000000018BC2", 4),
    # Catnapper: the aim star, crater cracks (dark and glowing), the impact puff
    "cat_sparkle": ("JetpackCat", "Catnapper", "00000000F7A7", 1),
    "cat_crack": ("JetpackCat", "Catnapper", "00000006CBB9", 1),
    "cat_crack_glow": ("JetpackCat", "Catnapper", "00000006CA10", 1),
    "cat_puff": ("JetpackCat", "Catnapper", "000000018B18", 1),

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
}


# the effects' meshes: pack model name -> (hero, ability folder, model id), converted with tools/owm_static.py
EFFECT_MODELS = {"torii": ("Kiriko", "KitsuneRush", "00000001048D")}


def add_effects(*sources):
    """Copies the effects' masks into assets/overcraft/textures/effect/ (flipbooks as <name>_<frame>.png), and
    converts their meshes (the torii gate) into hero_models/. Each from the first source folder that has it: Jetpack
    Cat's research pass wrote hers outside the read-only extraction (research\jetpack_catx)."""
    write_mcmeta()
    out = PACK / "assets" / "overcraft" / "textures" / "effect"
    out.mkdir(parents=True, exist_ok=True)
    copied = 0
    for name, (hero, ability, texture, frames) in EFFECT_MASKS.items():
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
    print(f"{copied} effect textures -> {out}")
    for name, (hero, ability, model) in EFFECT_MODELS.items():
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
                shutil.rmtree(staged)
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
            shutil.copyfile(glb, dest)
            subprocess.run([sys.executable, str(ROOT / "tools" / "rename_glb_clips.py"), str(dest)]
                           + [f"{a}={b}" for a, b in renames.items()], check=True)
            print(f"{name}: {dest} (clips {', '.join(sorted(renames.values()))})")


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
        add_ui(sys.argv[2])
    elif cmd == "sounds":
        add_sounds(sys.argv[2])
    elif cmd == "fonts":
        add_fonts(sys.argv[2])
    elif cmd == "effects":
        add_effects(*sys.argv[2:])
    elif cmd == "props":
        add_props(sys.argv[2], sys.argv[3] if len(sys.argv) > 3 else None)
    elif cmd == "zip":
        if not PACK.exists():
            sys.exit("no pack yet: add a model first")
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
