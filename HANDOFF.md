# Overcraft: handoff (2026-10-27, overnight)

Read this with CLAUDE.md (the rules), CHANGES.md (the log, newest first: 2026-10-26 lists every step played) and
techniques/README.md (how the work gets done: a linked graph of notes). This file is the state of play and the path forward.

## Where things are
- Repo: https://github.com/Thrasherop/overcraft. Kiriko's work is on `feature/kiriko`, merged into `next`, then
  `main`. **Build 0.3.7 is on `main` (c67d2e4, 2026-10-27 night)**: the owner let me act as the repo's owner for the
  night, `next` deeply played by hand first (CHANGES.md 2026-10-27: LAN with a second client, then a general play in
  a fresh survival world). Its push makes the test build (GitHub pre-release). `production` untouched.
  Lúcio is on `feature/lucio` (work in progress, not merged).
- Issues: #1 Juno, #2 Sojourn, #3 communication wheel, #4 emotes, #5 voice lines, #6 Overwatch assets, #7 select
  screen, #8 HUD, #9 animations, #10 Mercy, #36 perks, #37 Lúcio, #38 Jetpack Cat, #39 Lifeweaver, #40 pings, #41
  sprays (later), #42 climb cancels reload, #43 hunger under the hidden food bar (the owner chose: it burns down to 4
  bars in hero mode; on `feature/kiriko`). `feature/kiriko` is ahead of `main` by two batches (CHANGES.md "After
  0.3.7"), each played; not yet through `next`.
- Git here: this checkout's ownership confuses git ("dubious ownership"); run it as
  `git -c safe.directory=E:/claude/mods/overcraft ...` (the global safe list wasn't changed).
- Memory: `C:\Users\ultra\.claude\projects\E--claude-mods-overcraft\memory\`.
- Overwatch archive (read-only, checksummed): `E:\Overwatch Archive\` (README.txt). Research notes there:
  `research\kiriko-anim` (animation map), `research\kiriko-effects`, `research\fonts`, `research\kiriko-sounds`.
- **Latest test bundle for the owner: `run/bundle-2026-10-26d/`** (fb8f70e: jar, rebuilt asset pack, INSTALL.txt;
  sent as five parts with JOIN.txt). It replaces the earlier ones (04:44, 05:29, 07ad5cc). Then newer jars alone, to
  drop over it (the pack didn't change): `run/bundle-2026-10-26e/` (17323a0), `run/bundle-2026-10-26f/` (latest).

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
  throws, casts and the reload layered on top, the aim pose sheet (A71F) leaning her with her aim; she faces her aim
  in third person; the first-person view model (`_1p`) with sway, walk bob and breathing; a **cloth simulation** for
  her coat tails (ClothSim: kept out of her legs); held props shown only when their clip uses them (the Suzu only in
  its throw). Clips are mended at load (single broken rotation keys: the breath clip's kicked her leg out).
- **HUD like Overwatch's** (HeroHud, client/hud): measured from the owner's screenshots and two critique rounds,
  Overwatch's fonts, curved sides; ally health markers, a kill feed, low-health edges, Overwatch-sized chat; Minecraft
  mode keeps vanilla's HUD plus a small ult meter.
- **Controls** (client/controls/HeroControls): hero mode has its own key set; G switches; Esc opens an Overwatch-style
  menu and options (rebinding); Interact on F. A HERO tab has Overwatch's hero options for Kiriko with Overwatch's
  defaults (HeroOptions): **Toggle Healing Ofuda is on by default, as in Overwatch** (one press throws the clip; turn it
  off there for hold-to-throw), sensitivities, auto climb, ofuda cancels reload, the ofuda counter.
- **Hero select** like Overwatch 2026's, her real 3D model in the middle.
- **Effects** (client/effect): Overwatch's own masks and torii mesh, drawn additively; the server sends EffectPayload
  events; no Minecraft particles left. First-person screen effects (Swift Step, Kitsune Rush).
- **Sounds**: 53 sound events (OcSounds), vanilla stand-ins in the jar, the game's recordings via the pack (layers
  mixed at build time, 1P/3P split by audience). **Voice lines** for the game's moments (Voice.say: ult ready, kills,
  multikill, melee, respawn, select, healing now and then) and her own hurt/death sounds (PlayerMixin).
- **Combat feedback**: hit markers at the crosshair (white, red crit, kill), a kill feed (weapon art or a fist or
  skull glyph; hero portraits, else mob faces from Minecraft's textures or a player's skin), ally health markers.
- **Hero options** (Options > HERO, HeroOptions): Overwatch's six for Kiriko with Overwatch's defaults. Saved per
  client; the server gets the three it acts on. The ult's charge is saved when a player leaves.
- **Performance**: her 67k-triangle model is drawn as triangles with back faces of closed pieces culled on the CPU
  (third person 31 -> 50 FPS on this machine's integrated GPU). Harness `timing` measures it.
- **Multiplayer checked** with a second dev client (`gradlew runClient2`): what an ally sees and hears.
- **Communication, for every hero** (`dev.overcraft.comms`, client/comms): the wheel on T (callouts, voice lines,
  emotes pages), voice lines and emotes equipped in the Overwatch menu's VOICE LINES, pings on the middle button
  (spot, warning, enemy with portrait and red outline). Teams (Blue/Red, `/overcraft team`, the
  `overcraft:allow_team_change` rule) decide who hears callouts and sees pings.
- **Emotes** play as their clips chain by pose (`EmotePlan`): a one-shot once (then first person), a dance or a kneel
  looping until she moves. The pack's emote clips include Overwatch's additive layers and the props' own clips in id
  order; they're left out by the chaining, not by the exporter.
- **Perks** (#36): Overwatch's minor/major perks, levels from damage, healing and a slow passive gain; LALT menu.

## Dev harness
`gradlew runClient` (`-Dovercraft.dev=true`); `py tools/dev.py <commands...>` or `. tools/devplay.sh`. Commands:
state, newworld <name> [creative] [normal], openworld, screen, close, respawn, key <mapping or hero action> down|up|tap, look/turn, cmd,
screenshot, perspective, hero <id>, subtitles on|off, and (session 3) **press <key.keyboard.x>** (a real key event:
Esc, Enter, F1), **screenclick x y** (GUI coordinates), **hover x y** (the game's cursor only), **window w h**,
**tutorial**, **ult <percent>**, **fx <effect> [variant]**, **select [bed]**, **soundinfo <event>**,
**connect host:port**, **players**. A second client:
`gradlew runClient` plays as "Kiriko" (a fixed name, so the same player across launches),
`gradlew runClient2` as "Viewer" (harness port 25600: `OC_DEV_PORT=25600 py tools/dev.py ...`; host with `/publish true 25565`).
The desktop can be locked; the harness still plays.
Harness lessons (2026-10-27): in 26.3 mouse buttons are numbered from 1 (`screenclick x y 1` left, `3` right); a
container slot only takes a click once `hover` has put the cursor on it; a `look` and an ability in the same tick
throw at the old aim (the server hasn't had the new pitch): `look`, `sleep 0.2`, then the key. `/publish false 25565`
opens a world to LAN without commands for the second player.

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
- Keep techniques/ up to date (2026-10-26; a linked graph since 2026-10-27). Commit only after playing in game.
- Desktop control: the owner approves computer use at the PC (not from the phone). SteamCMD: never type their
  password or Steam Guard code.
- Versions (owner, 2026-10-26, #21): every build handed to the owner gets its own version in `gradle.properties`
  (0.2.0 was the first), its CHANGES.md entry headed "Build <version>". When a build needs the asset pack rebuilt
  (clips renamed, sounds or textures added), raise `Overcraft.ASSETS_NEEDED` to that version too.
- Mojang's EULA: accepted for a local vanilla test server, to check the mod stays out of sight there (owner,
  2026-10-26, #32). Only for that: ask again before accepting anything else.
- Voice lines: which line plays and how often may come from the game data (each line's criteria, weight and
  cooldown, read offline: tools/reference/kiriko-voice.md), as no public source has them (owner, 2026-10-26, #13).
  The audio itself stays in the local pack like the models; only the numbers are in the code (Voice.WEIGHTS) and the
  sound maps.

## Questions for the owner
Open:
- **First-person hands (critique round 8 #1):** answered with a default-weapon screenshot at FOV 103 (2026-10-27): the
  arms are now 1.4 times the model's size (were 1.1), matching it. Worth the owner's look in play.
- **Ping sound:** still a note-block chime; Overwatch's isn't extracted.
- **Minecraft-mode arms:** each held item's grip and size were fitted by eye to about a dozen items (`HELD_KUNAI`,
  `HELD_OFUDA`, `HELD_MAX`, `HELD_SCALE`, `SPRITE_TURN` in HeroModelRenderer; the harness's `held`, `heldmax`,
  `heldscale` change them live). Odd items (a trident, a spyglass held, banners, modded models) may need their own.
- Sombra's role: the wiki lists her as a support since 2026, so she stays one (an old note here said damage).
- **Dragon music (#45, after the emotes):** Overwatch has no "about to win" music for one team. Its end-phase music
  plays for both teams: on 30 s left, and in overtime (an intro, then a loop until it ends;
  `tools/reference/music-round9.md`). The plan: the overtime cue from 1/3 of the dragon's health, quiet and rising as
  the health falls, then Victory's stingers, then the POTG cue (5B1F, a candidate to confirm by ear).
- **Emotes:** the umbrella's hardpoint (02A9) is in no extracted model, so it's inferred from her clip (it looks
  right). Two shared hand layers (12625, 12638) over Listen Up and Salty aren't read yet; meanwhile a weapon no hand
  holds isn't drawn there.

Answered (owner, 2026-10-27):
- Crosshair: Overwatch's default white, no colour option.
- Ping key: Overwatch's default (middle mouse; its other default, G, is hero mode's key here), rebindable.
- Notable pings: dropped items, villagers, structures (not from close by or inside), etc. Done (#40).
- Movement: the game data's mover numbers are right (the owner compared with Overwatch directly; the floatiness was
  Minecraft's surroundings).
- Reload: Overwatch shows no bar, only the count until the ammo is back; the ammo point from the game data. Done.
- Minecraft mode's arm: her real arms holding the item, the off hand's item in the ofuda hand; a setting for
  Minecraft's arm instead. Done.
- Toggle Healing Ofuda on by default, kunai throws alternating hands, On Fire's Overwatch 1 numbers: correct as is.
- Shadow: a body-shaped one. Done.
- Talisman backs: the paper's white, as its front. Done.
- (Answered by the data: the "high kick" in the Suzu throw was a broken key in the idle breath clip, now mended.)

## Next (the owner's order, 2026-10-26 night)
Kiriko is the flagship: "basically perfect" before any other hero ("if you're implementing lucio before 4am then you
didn't spend enough time learning and refining Kiriko"). Refactoring for later heroes is welcome along the way.
1. **Kiriko up to snuff:** the HUD (#8) and select screen (#7) against Overwatch's, missing animations (#9), Suzu
   without the gold ring area (#22: Overwatch shows only the white wave over those it touches and the ground), the
   critiques' open items (`tools/reference/critique-2026-10-26*.md`, each with a Status section), new critique rounds.
   Perks gain 1 point a second passively (owner); kills stay much faster.
2. **Voice lines (#5), emotes (#4) and the communication wheel (#3) as their own system**, not Kiriko's, in code too.
   Look up Overwatch 2's wheels first. Emotes: all of a hero's, with a settings menu to choose the equipped ones.
   Callouts reach teammates (scoreboard team; every player when there are no teams) at any distance; the voice line
   plays in the world to everyone nearby. With it: an easy way for players to change teams, and a game rule letting
   non-operators change teams (default on) (owner, 2026-10-26).
3. **Lúcio (#37)** (`feature/lucio` has his kit in progress; `tools/reference/lucio-plan.md`, `lucio.md`; his default
   skin is extracted to `E:\Overwatch Archive\extracted\154088\Heroes\Lúcio`): his animation research runs meanwhile.
   Build him after 1-2, turning "Kiriko in Minecraft" into "Overwatch heroes in Minecraft". LMB Sonic Amplifier, RMB
   Soundwave (owner: good); safe fall 8 blocks (owner). Every hero comes with its four perks (`Perk`, `Perks`).
4. Jetpack Cat (#38). 5. Lifeweaver (#39). Later: Sombra, Illari (re-check patch notes), Juno (#1), Sojourn (#2),
   Mercy (#10).
Once Kiriko is refined, update techniques/ with what was learned.
