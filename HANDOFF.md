# Overcraft: handoff (2026-10-25, updated in the second session)

Read this with CLAUDE.md (the rules) and CHANGES.md (the log). This file is the state of play and the path forward.
Delete or refresh it once it's out of date.

## Why a new session
The first session was started by Dispatch (a "scheduled run"): the desktop-control approval card can't appear there,
so nothing was ever played by hand. **A normal Claude Code session in E:\claude\mods\overcraft fixes that.** When
asked, approve computer use for the dev client: `C:\Users\ultra\.gradle\jdks\eclipse_adoptium-25-amd64-windows.2\bin\java.exe`
(its window is "Minecraft* 26.3"; it's only matchable while running, by that exe path). Also allow Steam if the
Steam console should be driven.

## Where things are
- Repo: https://github.com/Thrasherop/overcraft. `main` has only the scaffold commit. **All work is on `feature/kiriko`**
  (pushed). Nothing goes to `main` until it's been played in-game (CLAUDE.md). CI (.github/workflows/build.yml) is
  green on the branch; a push to `main` makes a test pre-release.
- Uncommitted at handoff time: see "Current work" below (committed as a WIP commit at the end of this session).
- Issues: #1 Juno, #2 Sojourn, #3 communication wheel, #4 emotes, #5 voice lines, #6 Overwatch assets from the
  player's install (models/textures/sounds/animations). Labels copied from Lethalcraft.
- Memory: C:\Users\ultra\.claude\projects\E--claude-mods-overcraft\memory\ (asset safety rules, movement feel).
- Scheduled task `overcraft-datatool-watch` (daily 10:10): tells when DataTool supports build 154504; disable it once
  we no longer need it (the 154088 route below may make it moot).

## Tech stack (verified)
- Minecraft 26.3 (installed locally too), Fabric Loader 0.20.1, Fabric API 0.165.0+26.3, Loom 1.19-SNAPSHOT,
  Gradle 9.8.2. 26.x is unobfuscated (no mappings). Loom needs Gradle on Java 25: gradle/gradle-daemon-jvm.properties
  makes Gradle download it. System Java is 17.
- 26.x renames to know: `Identifier` (was ResourceLocation), `GuiGraphicsExtractor` + `extractRenderState` (was
  GuiGraphics/render), `Screen.extractRenderState/extractBackground`, `mc.gui.setScreen/screen()`, `EntityTypes`,
  input is SDL3 not GLFW (`InputConstants.KEY_*`, `InputConstants.Type.KEYBOARD`), `KeyMapping.Category`, renderers use
  render states + `SubmitNodeCollector` (`submitCustomGeometry`), `PoseStack.rotateDegrees(Axis, deg)`.
- Decompiled sources for lookups: `gradlew genSources`; extracted copies were in the old session's scratchpad
  (C:\Users\ultra\AppData\Local\Temp\claude\E--claude-mods-overcraft\77e872c0-...\scratchpad\mc and \fapi): re-extract
  if gone (Fabric API sources: download each module's -sources.jar from maven.fabricmc.net).

## What's built (feature/kiriko): played through the dev harness (CHANGES.md), not yet with the real mouse
- Hero select screen on joining a world (Kiriko playable; Lúcio/Sombra/Illari "coming soon"). H re-opens it within
  5 blocks of your bed/respawn point (or world spawn); the pick on joining is free (owner).
- Hero mode: G toggles (owner) or hotbar slot 1. Weapon item lives in slot 1; vanilla hotbar/hearts/food/XP hidden,
  hero HUD shown (health in OW numbers, ammo, cooldowns, ult %).
- Health: 250 OW = 20 MC (Ow.java). Global regen after 6 s. No fall damage in hero mode.
- Overwatch movement in hero mode (OwMovement + LivingEntityMixin): 5.5 m/s, 90% back, ~0.1 s accel, jump 5.72 m/s
  (0.98 m apex, trapezoid integration), gravity 16.7 up / 17.5 down to 30 m/s, Ctrl crouch 3 m/s, no sprint,
  Shift doesn't sneak (it's Swift Step). Air control value is ours.
- Kiriko's full kit (Kiriko.java, KirikoStats.java has every number + source): LMB kunai, RMB (hold) ofuda (homing
  lock), Shift Swift Step, E Protection Suzu, Q Kitsune Rush (particle corridor + buffs), R reload, wall climb, Medic
  40% self-heal. Projectiles: entity/{Kunai,Ofuda,Suzu}Entity. Allies: players (teams), pets, villagers, golems.
- Ult charge: 5/s, 1 per damage/heal (OW units), and advancements: task 10% / goal 25% / challenge 50% (owner).
- Unit tests (gradlew test): units, run speeds, jump apex, fall cap, accel, Suzu flight. All pass.

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
- **First person**: Overwatch's first-person animation set isn't in this extraction. All 330 clips are 3rd-person
  (rendered from the `Camera` bone, the eye sits inside the head; the `4-*` clips are additive layers whose bones
  collapse when applied alone). DataTool's SkinTheme walks Gameplay3P and names the STUFirstPersonComponent's entity
  Gameplay1P, but its clips didn't come out. Options: find how DataTool exports them, or "true first person" (her
  model from the Camera bone with the head hidden, 3rd-person clips).

## The hero model in game (feature/kiriko, uncommitted until the owner's real-mouse play)
Owner wants Kiriko's own animations ("when crouching, Kiri crouches; Steve bends at the waist"), not Minecraft's.
Files (src/client/java/dev/overcraft/client/model/):
- `Glb`, `SkinnedModel`, `Skeleton`: .glb reading, rest pose, skin, clips, sampling, blending, skin matrices.
- `HeroAnimator`: per-entity state machine: idle, 8-direction run and crouch-walk (rate matched to ground speed),
  jump/fall/land, climb (+0.3 m back from the wall) and climb_top, death; actions (throws, casts) on the upper body
  from `HeroActionPayload`. Bones by name: Overwatch (`Spine_1`, `Head`) or ValveBiped.
- `HeroModels`: loads hero_models/<hero>.glb and extras (kitsune.glb); `HeroTexture`: mipmapped, linear filtering.
- `HeroModelRenderer` (+ AvatarRendererMixin, LivingEntityRendererMixin): draws hero-mode players (and dying heroes)
  as their model, CPU-skinned; hides held props the current clip doesn't use (parked at the origin).
- `KitsuneRenderer` + `entity/KitsuneEntity`: Kitsune Rush's fox, moved by `KitsuneRush` along the corridor.
- Known gaps: name tags/armor layers skipped for hero-model players; no first-person arms (see above); no swim pose;
  quick melee not in the kit; Sombra is listed as a Support (she's Damage).

## Dev harness
`gradlew runClient` passes `-Dovercraft.dev=true` and the asset pack path. `client/dev/DevServer.java` listens on
127.0.0.1:25599; `py tools/dev.py <commands...>` (see its docstring). Commands: state (with `anim`, the playing
clip), newworld <name> [creative|survival], openworld, screen, close, respawn, key <mapping> down|up|tap,
look/turn, cmd </command>, screenshot <file.png> (-> run/screenshots/), perspective first|back|front, hero <id>.
Lessons: summon test mobs with `Tags:["x"]` and select `@e[tag=x]` (superflat worlds have villages, and SNBT names
keep their quotes); a fresh client logs "Hero model" before it's done loading: wait for the title screen; the
harness can't click, so real hand play still needs desktop control (the owner approves it at the PC).

**Next:**
1. The owner's real-mouse hand play of all of the above (CLAUDE.md), then commit (they asked: only after play).
2. First person (above). Role icons and HUD art from UITextures (#7, #8). Swim pose. Quick melee.
3. Merge feature/kiriko -> next -> main only after hand play (CLAUDE.md). Then Lúcio, Sombra, Illari (stats already
   researched: see CHANGES.md/old research, re-check patch notes), then Juno (#1), Sojourn (#2).

## Owner's decisions so far (all 2026-10-25)
- Fabric. Kiriko first, then Lúcio, Sombra, Illari, then Juno, Sojourn. Mercy added to the list (#10, session 2).
- LMB kunai, RMB ofuda (swapped from Overwatch's default on purpose).
- G toggles hero mode; H changes hero only near your bed; pick on join is free.
- Ult charge also from advancements. Movement/animations must feel like Overwatch, not Steve.
- Assets from the owner's own install, never shipped; Overwatch never running alongside; ripped models OK as
  stopgaps (the owner downloads them; Steam Workshop SFM ports were the clean source; a Patreon link I gave was NSFW:
  vet links before giving them). Archive each build's install on the WD_BLACK drive (E:\Overwatch Archive\).
- The UI uses the game's own art too (owner, session 2).
- Desktop control: the owner uses a normal session so computer use works (approved at the PC; it can't be approved
  from the phone). SteamCMD logged in by the owner once (`E:\Overwatch Archive\tools\steamcmd\steamcmd.exe +login
  <user>`, the owner types the password) would let a session download old builds itself with `+login <user>
  +download_depot 2357570 <depot> <manifest>` (offered, not done). Never type their password or Steam Guard code.
