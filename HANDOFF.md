# Overcraft: handoff (2026-10-25, end of the first session)

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

## What's built (feature/kiriko), none of it played by hand yet
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
  Valve's SteamCMD, kept in the scratchpad), unpacked to `local-assets/raw/kiriko_josseda673/`. Converted:
  `hero_models/kiriko.glb` (53k tris, 231 bones, ValveBiped skeleton, 16 placeholder clips from
  tools/stopgap_anims.py), `kiriko_weapons.glb` (kunai, ofuda, suzu), `kitsune.glb` (the fox). Previews in
  `local-assets/previews/` (pose_sheet.png = the placeholder poses).
- DataTool v2.25.0.0+1151 (overtools/OWLib) in the old scratchpad (`...\scratchpad\datatool\DataTool.exe`; re-download
  from the GitHub release if gone). Always run it with `--online=false` (else it may fetch from Blizzard's CDN).
  It refuses the live install: build 154504 (Oct 20 patch) not supported; it supports up to 154088.
- Snapshot of the live install (build 154504): `E:\Overwatch Archive\build-154504\` (77.32 GB, 117 files, Steam
  buildid 25806476; README.txt there). For when DataTool catches up.

**In progress right now: the Oct 6 build (154088), which today's DataTool reads.**
The owner ran in the Steam console (2026-10-25):
```
download_depot 2357570 2357571 7267023877222461613    (game data, 76.5 GB)
download_depot 2357570 2437288 2963885447373827722    (308 MB)
```
Depot 2357572 (English, 1.28 GB) didn't change on Oct 20: manifest 5397310686684469249 is the one in the 154504
snapshot. Steam puts downloads in `C:\Program Files (x86)\Steam\steamapps\content\app_2357570\depot_<id>\` (2.2 GB
done at handoff). **Next steps:**
1. When both say "Depot download complete": assemble `E:\Overwatch Archive\build-154088\` = depot_2357571 +
   depot_2437288 + the English depot's files (copy them from build-154504; compare with that snapshot's layout: the
   live install is the three depots merged). Keep the folder structure Overwatch uses (.build.info etc.).
2. Confirm the build number (DataTool prints "Overwatch build prometheus-..."). If it isn't 154088, check OWLib's
   supported builds (TACTLib commits "[pro] cmf for <build>").
3. With Overwatch closed: `DataTool.exe "E:\Overwatch Archive\build-154088" list-heroes --online=false`, then
   extract Kiriko: `extract-unlocks` (model, skins; check its flags for animations/refpose) and look at what modes
   export her animations (DataTool's wiki: github.com/overtools/OWLib/wiki). Output is .owmdl/.owanim/.owmat etc.;
   Blender imports those with the io_scene_owm add-on (ships in the OWLib repo / release: check). Extract into
   `local-assets/raw/kiriko_owlib/`. Never commit any of it.
4. Convert to glTF with Blender (extend tools/blender_to_glb.py for .owmdl + .owanim, exporting all clips as glTF
   animations with their real names), replacing the stopgap `kiriko.glb`. Same for the weapons (and the Kitsune fox,
   probably an effect/NPC model).
5. Map Overwatch clip names to the animator's states (see HeroAnimator: idle, run, run_back, strafe_left,
   strafe_right, crouch_idle, crouch_walk, jump, fall, land, climb, throw_kunai, throw_ofuda, throw_suzu, swift_step,
   ult_cast): a name table per hero, and the head bone + upper-body root become per-model settings
   (HeroAnimator.UpperBody is hard-wired to ValveBiped names today).

## Current work: hero model + animation system (written, compiles, NOT yet run)
Owner wants Kiriko's own animations ("when crouching, Kiri crouches; Steve bends at the waist"), not Minecraft's.
Files (src/client/java/dev/overcraft/client/):
- `model/Glb.java`: .glb reader (JSON + BIN, accessors, embedded images).
- `model/SkinnedModel.java`: nodes/rest pose, skin + inverse bind matrices, pieces (one per material), clips.
- `model/Skeleton.java`: sample clips (linear/slerp/step), blend poses (with a bone mask), skin matrices, head turn.
- `model/HeroAnimator.java`: per-entity state machine (movement -> base clip, 0.15 s crossfades; actions layered on
  the upper body; clip fallbacks; head follows the look direction). Remote players: state from position deltas;
  climbing detected by steady ~climb-speed rise; actions arrive as `HeroActionPayload` (server: `Combat.animate`).
- `model/HeroModels.java`: loads hero_models/<hero>.glb on resource reload, textures as DynamicTextures (halved to
  <= 2048).
- `model/HeroModelRenderer.java` + `mixin/AvatarRendererMixin` (extract) + `mixin/LivingEntityRendererMixin`
  (submit, cancels vanilla): a player holding a HeroWeaponItem (= hero mode, visible to everyone) is drawn as the
  hero model, CPU-skinned, triangles emitted as quads with a doubled corner into `RenderTypes.entityCutout`.
  Model space: glTF Y-up metres, facing +Z; rotated by -bodyRot (yaw 0 = +Z/south). **Unverified guesses to check
  first in game:** facing direction, feet at y=0, scale, texture V orientation, normals/lighting, performance
  (53k tris CPU-skinned per frame).
- Known gaps: name tags/armor/held-item layers are skipped for hero-model players; no first-person arms/weapons yet;
  weapons model not attached to the hands; Kitsune fox model not used yet (Kitsune Rush is still particles).

## Dev harness (written, compiles, NOT yet run)
`gradlew runClient` passes `-Dovercraft.dev=true` and the asset pack path. `client/dev/DevServer.java` listens on
127.0.0.1:25599; `py tools/dev.py <commands...>` (see its docstring). Commands: state, newworld <name>
[creative|survival] (flat world via CreateWorldScreenAccessor), openworld, screen, close, key <mapping>
down|up|tap, look/turn, cmd </command>, screenshot <file.png> (-> run/screenshots/), perspective first|back|front,
hero <id>. Lets a session drive and screenshot the game without desktop control; still do real hand play with
computer use per CLAUDE.md.

**Suggested first steps for the new session:**
1. `gradlew runClient` (background). `py tools/dev.py "newworld Test creative"`, then `hero kiriko`,
   `perspective back`, screenshots: does Kiriko render, facing the right way, feet on the ground, textured?
   Then walk/crouch/jump/climb via `key` and screenshot each (does the right clip play?).
2. Fix what that shows. Then play by hand with computer use: the whole Kiriko checklist in CLAUDE.md (kunai on
   zombies, ofuda on a wolf/villager, Swift Step, Suzu, Kitsune Rush, wall climb, HUD, G toggle, H near bed, death and
   respawn, rejoin, plain Minecraft on slots 2-9). Write down what was tried in CHANGES.md.
3. Meanwhile the 154088 download -> DataTool extraction -> real model + clips (above).
4. Then: first-person view model (arms + weapons), weapons in hands, Kitsune fox model, name tags; perks; quick melee.
5. Merge feature/kiriko -> next -> main only after hand play (CLAUDE.md). Then Lúcio, Sombra, Illari (stats already
   researched: see CHANGES.md/old research, re-check patch notes), then Juno (#1), Sojourn (#2).

## Owner's decisions so far (all 2026-10-25)
- Fabric. Kiriko first, then Lúcio, Sombra, Illari, then Juno, Sojourn.
- LMB kunai, RMB ofuda (swapped from Overwatch's default on purpose).
- G toggles hero mode; H changes hero only near your bed; pick on join is free.
- Ult charge also from advancements. Movement/animations must feel like Overwatch, not Steve.
- Assets from his own install, never shipped; Overwatch never running alongside; ripped models OK as stopgaps
  (he downloads them; Steam Workshop SFM ports were the clean source; a Patreon link I gave was NSFW: vet links
  before giving them). Archive each build's install on the WD_BLACK drive (E:\Overwatch Archive\).
- Desktop control: he'll use a normal session so computer use works. SteamCMD logged in by him once would let a
  session download old builds itself (offered, not done).
