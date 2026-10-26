# Techniques that work

How Overcraft gets built: the methods that paid off, so they aren't rediscovered every session (owner, 2026-10-26).
Read with CLAUDE.md (the rules) and HANDOFF.md (the state). Add to it whenever something works, or doesn't.

## Finding what the game actually does (assets, animations)

- **Map a hero's animation data with a sub-agent before guessing.** Guessing clips by eye cost several rounds of
  owner corrections (the "hackles up" idle, a phone emote as the crouch idle, a recovery as the throw). A research
  sub-agent reading the game data through DataTool's own libraries (TankLib/TACTLib loaded with DataTool's bundled
  .NET runtime from Python, no SDK needed) mapped every clip to its state, layer, timing and alias in ~40 minutes:
  `E:\Overwatch Archive\research\kiriko-anim\` (map.json, scripts\) and `tools/clipmaps/kiriko-research.md`. Ask it to
  map **everything** (emotes, intros, sounds, effects), not just what's needed now: future work reuses it.
- **Then check the research by eye, and send it back when it disagrees.** Its first idle pick (A6F0) stepped 1.15 m
  when played on the clock. Asked how the node plays it, it found the time is sampled from a game parameter (held
  when not turning). Render every claim before wiring it in.
- **Contact sheets.** Render clips as frame strips and look at many at once (`tools/owm_survey.py --survey --shots=N`,
  `--side` for leg cycles; scratchpad `sheets.py` stacks strips). The owner spotted the wall climb on one. Send the
  owner sheets when stuck: a human recognizes poses instantly.
- **Render first-person clips from the Camera bone with its whole transform** (`tools/owm_fpsurvey.py`): position
  only puts the arms at the top of frame, because the clips turn and dip the camera.
- **Measure, don't eyeball, where possible:** direction of travel from the planted foot's slide (run clips), pose
  features (wrists above head, pelvis height) to find climbs and crouches, which clips move the held-prop bones (found
  the Suzu throw), root bone height (found climb_top's root motion).
- **Overwatch data quirks learned:** clips are 30 or 60 fps (export onto one timeline); `4-*` folder clips are
  additive deltas (they collapse alone; export neutral keys for unkeyed channels); first-person arms only get
  materials from the Gameplay1P entity's model look; props (kunai x3, ofuda, Suzu) are pieces parked at the origin
  until a clip puts them in a hand; cloth bones are never keyed (physics in the game).
- **Archive builds and extractions on E:, read-only, with checksums**, outside any game library, so updates can't
  undo the work (`E:\Overwatch Archive\README.txt`).

## Playing it (CLAUDE.md: play before claiming)

- **The dev harness** (`tools/dev.py`, `tools/devplay.sh`): keys, look, commands, screenshots, `state` with the
  playing clip (`anim`). Sample `state` every tick to see state machines (`climb` 13 ticks -> `climb_top` -> `jump`).
- **Real input with desktop control** when the owner approves it at the PC. Computer-use screenshots show the game
  as blank: capture the screen with GDI instead (scratchpad `burst.ps1`, timed bursts for fast animations). Can't
  send F5 or two held keys at once; Windows' text-input overlay can block clicks.
- **Bursts of screenshots** across an animation (0.05-0.3 s apart) and side-by-side sheets: one screenshot hides
  most animation bugs.
- **Reproduce the owner's report first** (stuck on a step only when strafing; Alex in Minecraft mode), then show the
  fix with the same steps.
- Test worlds: summon test mobs with `Tags:["x"]` and select `@e[tag=x]` (superflat villages, SNBT names keep their
  quotes); `/difficulty peaceful` while setting up (a slime killed the test once); gamerules are snake_case in 26.3
  (`send_command_feedback`).

## Comparing with the real game

- **The owner's gameplay screenshots are the spec.** Read them closely: HUD layout, the ofuda pips under the
  crosshair, which arm throws, how the arms sway. Zoom in, compare side by side with ours.
- **A reference sub-agent** collects how real gameplay looks (web, the wiki's MediaWiki API) into
  `tools/reference/kiriko-reference.md`.
- **Nitpick sub-agents**: give one our screenshots and the references, ask for every difference, however small
  (owner: "many things you'll see as a nit are a big deal for humans").

## Building

- Read Minecraft's decompiled 26.3 sources for every API (`.gradle/loom-cache/.../*-sources.jar`, extracted to the
  scratchpad): names and signatures changed a lot (GuiGraphicsExtractor, RenderPipelines, PoseStack.mulPose takes a
  matrix, knockback takes a damage source, MipmappedTexture exists).
- Synced Fabric attachments must be registered at mod start, not lazily from an event.
