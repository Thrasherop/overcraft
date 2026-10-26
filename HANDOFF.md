# Overcraft: handoff (2026-10-26, after the overnight session)

Read this with CLAUDE.md (the rules), CHANGES.md (the log, newest first: 2026-10-26 lists every step played) and
TECHNIQUES.md (how the work gets done). This file is the state of play and the path forward.

## Where things are
- Repo: https://github.com/Thrasherop/overcraft. **All work is on `feature/kiriko`.** Session 3's commits are local
  only (not pushed): `git log origin/feature/kiriko..feature/kiriko`. Nothing goes to `main` until the owner's hand
  play (CLAUDE.md).
- Issues: #1 Juno, #2 Sojourn, #3 communication wheel, #4 emotes, #5 voice lines, #6 Overwatch assets, #7 select
  screen, #8 HUD, #9 animations, #10 Mercy.
- Memory: `C:\Users\ultra\.claude\projects\E--claude-mods-overcraft\memory\`.
- Overwatch archive (read-only, checksummed): `E:\Overwatch Archive\` (README.txt). Research notes there:
  `research\kiriko-anim` (animation map), `research\kiriko-effects`, `research\fonts`, `research\kiriko-sounds`.

## The local asset pack (never committed or shipped)
Overwatch closed first (`Get-Process Overwatch, Battle.net`). From the archive's extraction of build 154088:
```
py tools/build_asset_pack.py ui "E:\Overwatch Archive\extracted\154088"        # portraits, cards, icons, role badges
py tools/build_asset_pack.py sounds "E:\Overwatch Archive\extracted\154088"    # tools/soundmaps/*.txt
py tools/build_asset_pack.py fonts "E:\Overwatch Archive\extracted\154088\Fonts"
py tools/build_asset_pack.py effects "E:\Overwatch Archive\extracted\154088\Effects"   # Blender for the torii mesh
py tools/build_asset_pack.py zip                                                # -> local-assets/overcraft-assets.zip
```
Hero models: `tools/owm_survey.py ... --glb --keep=tools/clipmaps/kiriko.txt` (and `_1p`, `kitsune`), as in
CHANGES.md 2026-10-25. The dev client reads `local-assets/pack/`; a real install `<.minecraft>/overcraft/overcraft-assets.zip`.

## What's built (feature/kiriko), played through the dev harness
- **Kiriko's kit** (Kiriko.java; KirikoStats.java has every number and its source), Overwatch movement, 250 HP = 20.
- **Her model** (client/model): Overwatch's model and clips, CPU-skinned; idle/run/crouch/jump/climb/death states,
  throws and casts layered on top; the first-person view model (`_1p`) with sway, walk bob and breathing; a **cloth
  simulation** for her coat tails (ClothSim: kept out of her legs); held props shown only when their clip uses them
  (the Suzu only in its throw).
- **HUD like Overwatch's** (HeroHud, client/hud): measured from the owner's screenshots, Overwatch's fonts, curved
  sides; Minecraft mode keeps vanilla's HUD plus a small ult meter.
- **Controls** (client/controls/HeroControls): hero mode has its own key set; G switches; Esc opens an Overwatch-style
  menu and options (rebinding); Interact on F.
- **Hero select** like Overwatch 2026's, her real 3D model in the middle.
- **Effects** (client/effect): Overwatch's own masks and torii mesh, drawn additively; the server sends EffectPayload
  events; no Minecraft particles left. First-person screen effects (Swift Step, Kitsune Rush).
- **Sounds**: 24 sound events (OcSounds), vanilla stand-ins in the jar, the game's recordings via the pack.

## Dev harness
`gradlew runClient` (`-Dovercraft.dev=true`); `py tools/dev.py <commands...>` or `. tools/devplay.sh`. Commands:
state, newworld, openworld, screen, close, respawn, key <mapping or hero action> down|up|tap, look/turn, cmd,
screenshot, perspective, hero <id>, subtitles on|off, and (session 3) **press <key.keyboard.x>** (a real key event:
Esc, Enter, F1), **screenclick x y** (GUI coordinates), **window w h**, **tutorial**, **ult <percent>**,
**fx <effect> [variant]**, **select [bed]**. The desktop can be locked; the harness still plays.

## Overwatch assets (#6): state and path forward
Rules (CLAUDE.md): Overwatch closed whenever its files are read; never touch the client; assets live in
`local-assets/` (gitignored) and reach the game as a local resource pack; the build fails if the jar contains any.

**Done:**
- `LocalAssetPack` + `MinecraftPacksMixin`: mounts `-Dovercraft.assetPack` (dev: local-assets/pack/) or
  `<game dir>/overcraft/overcraft-assets.zip` as an always-on resource pack. `checkNoOverwatchAssets` guard (tested).
- Importer: `py tools/build_asset_pack.py add <hero> <file>` -> Blender 5.2 headless (tools/blender_to_glb.py) ->
  `local-assets/pack/assets/overcraft/hero_models/<hero>.glb`. Handles .blend/.fbx/.gltf/.obj/.smd and Source .mdl
  (via the **SourceIO 5.5.4** Blender add-on, installed in %APPDATA%\Blender Foundation\Blender\5.2\scripts\addons\
  SourceIO; it needs `directory` + `files`, not just `filepath`). Optional preview render and `--stopgap-anims`.
- **Stopgap model in place:** Josseda673's Steam Workshop SFM port (item 2878840310, downloaded anonymously with
  Valve's SteamCMD: `steamcmd +login anonymous +workshop_download_item 1840 2878840310 +quit`; the download is a zip
  named *_legacy.bin), unpacked to `local-assets/raw/kiriko_josseda673/`. Converted:
  `hero_models/kiriko.glb` (53k tris, 231 bones, ValveBiped skeleton, 16 placeholder clips from
  tools/stopgap_anims.py), `kiriko_weapons.glb` (kunai, ofuda, suzu), `kitsune.glb` (the fox). Previews in
  `local-assets/previews/` (pose_sheet.png = the placeholder poses).
- Tools kept on the archive drive, `E:\Overwatch Archive\tools\`: `DataTool-2.25.0.0\DataTool.exe` (overtools/OWLib
  v2.25.0.0+1151), `steamcmd\steamcmd.exe` (Valve, signed; already self-updated), `SourceIO-5.5.4.zip`. Always run it with `--online=false` (else it may fetch from Blizzard's CDN).
  It refuses the live install: build 154504 (Oct 20 patch) not supported; it supports up to 154088.
- Snapshot of the live install (build 154504): `E:\Overwatch Archive\build-154504\` (77.32 GB, 117 files, Steam
  buildid 25806476; README.txt there). For when DataTool catches up.

**Build 154088 extracted (session 2, 2026-10-25).** All on the archive drive, read-only, checksummed, outside any
Steam library (game updates can't touch it); `E:\Overwatch Archive\README.txt` says what's where.
- `build-154088\`: the Oct 6 install (prometheus-2_25_0_0-154088), from the Steam console's download_depot (the
  first try died when C: filled up; Steam then needed a restart: "No connection to content servers" = "No target
  sources connected" in content_log). English data.001.001 copied from build-154504.
- DataTool 2.25 reads it offline: `DataTool.exe "E:\Overwatch Archive\build-154088" <mode> ... --online=false
  --language=enUS`. The default skin is now called "Overwatch" (`"Kiriko|skin=Overwatch"`; "Classic" finds nothing).
- `extracted\154088\`: heroes.txt (every hero's kit + perk text), Heroes\Kiriko (extract-unlocks --extract-refpose),
  HeroVoice\Kiriko (2265 lines by category, with subtitles: issues #3, #5).
- Blender add-on: overtools' io_scene_owm 3.3.1 (installed as a Blender 5.2 extension; zip in `E:\Overwatch
  Archive\tools\`). `tools/owm_survey.py` imports a .owmdl + its clips, surveys them (thumbnails, motion numbers)
  and exports a .glb (`--keep=<clip map>` picks and renames clips; all 330 takes the glTF exporter >30 min).
  `tools/owm_previews.py` renders every model in a folder (DataTool names them by id only).
- Kiriko's models (in Heroes\Kiriko\Skin\Overwatch\Overwatch\Models): `00000000003A.118` = Kiriko (3rd person and
  1st person use the same model; 94.8k tris before dropping 7 material-less LOD shells; 566 bones, 469 named:
  Pelvis, Spine_1-3, Neck, Head, Clavicle/Arm/Elbow/Wrist/Weapon_L/R, Thigh/Knee/Foot/Toe_L/R, cloth_*, and a
  `Camera` bone = the 1st-person eye). Her held kunai x3 / ofuda / Suzu are submeshes on weapon bones (at the origin
  in the rest pose; the game shows/hides them). `0000000105FB` = the Kitsune fox (8 clips; exported and checked:
  `E:\Overwatch Archive\work\kitsune\`). `00000001096F` kunai, `000000010945` ofuda (its texture isn't picked up
  yet), `000000010946` Suzu. `0000000096F5` motorbike, `00000001CBA0` cat: emote props.
- Her 330 clips: folders `0-*` are full-body clips, `4-*` look like additive layers (garbage when applied alone),
  `128-32` = quick melee x3, `16-*` long (emotes/intros). Clip map: `tools/clipmaps/kiriko.txt` (8-direction run and
  crouch-walk confirmed by foot slide; jump/land, melee, death, knockdown, wall climb (`0-32_AC1F`) and its hop over
  the top (`0-32_AC20`) confirmed by eye; abilities are `cand_`). Run clips move ~3.4 m/s: play them ~1.6x for
  Overwatch's 5.5. The owner spotted the climb on a contact sheet: show them sheets when stuck.
- Ability clips (kunai/ofuda/Suzu throws, Swift Step, ult cast, reload) are guesses: render them as frame sequences
  and compare with gameplay footage. The Suzu throw and Swift Step were found by which clips move the held props'
  bones (scratchpad script props.py: worth keeping in tools/ if it's needed again).
- UI art: `py tools/build_asset_pack.py ui "E:\Overwatch Archive\extracted\154088"` (from extract-hero-icons) puts
  every hero's portrait/card/full-body art and ability/perk icons in the pack (`HeroArt` reads them).
  `extracted\154088\UITextures` (dump-ui-textures, ~20k files) is for role icons and Overwatch's HUD art.
- **First person** (solved in session 2): the Gameplay1P entity's clips and model look give the view-model arms
  (`hero_models/kiriko_1p.glb`, clip map `tools/clipmaps/kiriko_1p.txt`).
- **Effects, fonts, sounds** (session 3): see "The local asset pack" above; surveys in tools/reference/.

## Owner's decisions so far
- Fabric. Kiriko first, then Lúcio, Sombra, Illari, then Juno, Sojourn. Mercy added (#10).
- **LMB kunai, RMB ofuda** (swapped from Overwatch's default on purpose, 2026-10-25). The options screen names them
  Overwatch's way (primary fire = Healing Ofuda, on RMB).
- G toggles hero mode and switches key sets (2026-10-26); Esc in hero mode opens Overwatch's menu, in Minecraft mode
  Minecraft's. H changes hero only near your bed; the pick on join is free.
- Ult charge also from advancements. Movement and animations must feel like Overwatch, not Steve.
- Assets from the owner's own install, never shipped; Overwatch never running alongside. Research stops short of the
  tamper protection and the encryption. The UI, sounds and effects use the game's own assets wherever they exist.
- Keep TECHNIQUES.md up to date (2026-10-26). Commit only after playing in game.
- Desktop control: the owner approves computer use at the PC (not from the phone). SteamCMD: never type their
  password or Steam Guard code.

## Questions for the owner
- Sombra's role: the wiki lists her as a support since 2026, so she stays one (an old note here said damage).
- The third-person Suzu throw clip (AB96) has a high kick in it: is that the right clip?
- First-person kunai throws alternate hands (your screenshots); the reference agent read Blizzard's clips as one
  hand every throw. Kept your observation.

## Next
1. The owner's real-mouse hand play of all this (CLAUDE.md), then push feature/kiriko; merge to next/main after.
2. The critique sub-agent's list (tools/reference/critique-2026-10-26.md) and the sound map (soundmaps/kiriko-proposed.txt).
3. Effects still missing: the enemy-red view, Suzu's white outline on allies, the rush path's talisman slabs, Swift
   Step's slash arcs. Third-person aim-lean animation (the reload is in).
4. Then Lúcio, Sombra, Illari (re-check patch notes), Juno (#1), Sojourn (#2), Mercy (#10).
