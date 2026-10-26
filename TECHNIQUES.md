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
- **Freeze the game for fast effects**: `cmd /tick freeze`, then `cmd /tick step 1` between screenshots. The effects
  run on the game clock, so they stop with it; a Suzu burst is over before the harness's next screenshot otherwise.
  The harness's `fx <effect>` plays one effect alone, which tells the effect apart from whatever else is going on.
- **A second player**: `gradlew runClient2` (a second dev client, harness port 25600), `/publish true 25565` on the
  first, `connect localhost:25565` on the second. See the hero as others do: third-person animation, the sounds
  others hear (subtitles on), effects, ally markers on a real player. Only the client drawing the change needs a
  restart for client-side work; server-side changes restart the host, and the second rejoins.
- **Suspect the test before the mod**: white pixel squares round a healed villager were Minecraft's death poofs from
  the villager the test script had just killed; burning "mobs" on the horizon were zombie horses in daylight.
- **Crop and enlarge** (`oc_crop.ps1`, nearest-neighbour at 2-6x) at native 1080p to judge HUD edges: a faint box
  round the volley bar only showed against the sky, zoomed.
- **Reproduce the owner's report first** (stuck on a step only when strafing; Alex in Minecraft mode), then show the
  fix with the same steps.
- **When the desktop is locked or can't take input**, the harness still types: `press key.keyboard.escape` sends a
  real key event through Minecraft's KeyboardHandler (Esc pauses, screens get keyPressed), `screenclick x y` clicks
  the open screen at GUI coordinates. `window 1920 1080` matches the reference screenshots' size; check other shapes
  too (5:4 and 21:9 found the HUD overlapping itself).
- Test worlds: summon test mobs with `Tags:["x"]` and select `@e[tag=x]` (superflat villages, SNBT names keep their
  quotes); `/difficulty peaceful` while setting up (a slime killed the test once); gamerules are snake_case in 26.3
  (`send_command_feedback`).

## Comparing with the real game

- **The owner's screenshots live in the session transcript** (`~/.claude/projects/<project>/<session>.jsonl`):
  images in user messages, and in `attachment.prompt` for messages queued while working. A short script decodes them
  to files (scratchpad `owner_imgs3.py`) so they can be cropped and compared, not just remembered.
- **Lay the HUD out in pixels of the reference screenshot** (1920x1080) and scale to the window; then crop the same
  region of ours and the reference at 2-3x (scratchpad `crop.ps1`) and compare them side by side.
- **The owner's gameplay screenshots are the spec.** Read them closely: HUD layout, the ofuda pips under the
  crosshair, which arm throws, how the arms sway. Zoom in, compare side by side with ours.
- **A reference sub-agent** collects how real gameplay looks (web, the wiki's MediaWiki API) into
  `tools/reference/kiriko-reference.md`.
- **Nitpick sub-agents**: give one our screenshots and the references, ask for every difference, however small
  (owner: "many things you'll see as a nit are a big deal for humans").

## Before changing behaviour
- **Read HANDOFF.md's "owner's decisions" first.** A default that looks like a mistake may be the owner's choice: the
  fire buttons (LMB kunai, RMB ofuda) are swapped from Overwatch's on purpose, and were nearly "fixed" back.
- **Check the wiki before "fixing" a fact from memory**: Sombra looked misfiled as a support, but the wiki has her as
  one since 2026.

## Building

- Read Minecraft's decompiled 26.3 sources for every API (`.gradle/loom-cache/.../*-sources.jar`, extracted to the
  scratchpad): names and signatures changed a lot (GuiGraphicsExtractor, RenderPipelines, PoseStack.mulPose takes a
  matrix, knockback takes a damage source, MipmappedTexture exists).
- Synced Fabric attachments must be registered at mod start, not lazily from an event.
- 26.3's input codes are SDL's, not GLFW's (A = 4, W = 26, Space = 44, right mouse = 3): always use InputConstants'
  names, never remembered numbers (a `<= 1` mouse-button check silently failed).
- Minecraft's "ttf" font provider only takes TrueType outlines; CFF fonts (.otf "OTTO") are rejected at load. Fonts
  are placed by baseline at 7 units below the draw position at any size, which makes cap-height sizing easy.
- Minecraft's mipmapper (`MipmapGenerator`, AUTO strategy) treats any texture with transparency as a cutout: it
  rescales alpha and adds 0.025 to every pixel of the smaller mips. Fine for alpha-tested models; for soft masks drawn
  blended it shows as faint boxes and swollen glows. Pass `MipmapStrategy.MEAN` for those (`HeroTexture`).
- Additive light washes out over bright ground (gold over grass turns lime): keep additive for teal/cyan glows, blend
  warm colours (`Fx.soft`).
- A second key set: KeyMappings made but not registered with Options still get key events (KeyMapping's own maps),
  stay out of the Controls screen, and can be silenced or swapped with a mixin on isDown/consumeClick.
