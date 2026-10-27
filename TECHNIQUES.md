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
- **An emote's clips aren't a playlist.** Its `Animations/` folder mixes the body's clips (`0/*`), additive or partial
  layers (`4/*`; `16/*` holds both layers and the long loops) and the prop's own clips (the same ids again under
  `Models/*/Animations/`), and id order isn't play order (Orion Dance's way in is its second). Chain them by pose
  instead: compare first and last frames over the core bones (mean 1 - |q.q'|): a one-shot starts and ends in the
  idle pose, an intro starts there, a loop's first frame is its last, an outro ends there. Joins measure 0.000 to
  0.007, non-joins 0.013 and up (`EmotePlan`; scratchpad `emoteplan.py` printed the plan of all 23 first).
- Looking for one of Overwatch's icons in the 21k-texture UI dump: filter by size, alpha share and whiteness, then
  sheet the survivors with their ids (scratchpad `iconsheet.py`). Matching a 20 px crop from a screenshot by shape
  doesn't work: at that size every blob scores well. Small UI glyphs may live in atlases; the chat channel icons
  weren't found as textures (drawn ours instead, `tools/gen_chat_icons.py`).

## Playing it (CLAUDE.md: play before claiming)

- **The dev harness** (`tools/dev.py`, `tools/devplay.sh`): keys, look, commands, screenshots, `state` with the
  playing clip (`anim`). Sample `state` every tick to see state machines (`climb` 13 ticks -> `climb_top` -> `jump`).
- **The harness's taps are shorter than a tick**: `key <action>` sets and clears the key within one frame, so code
  that reads `isDown()` once a tick (the perk menu's picks) never sees it. Hold it: `key <action> down`, a 0.2 s
  sleep, `key <action> up`. Holding attack doesn't mine unless the window has the mouse (Minecraft's
  `continueAttack` checks `isMouseGrabbed`).
- **Real input with desktop control** when the owner approves it at the PC. Computer-use screenshots show the game
  as blank: capture the screen with GDI instead (scratchpad `burst.ps1`, timed bursts for fast animations). Can't
  send F5 or two held keys at once; Windows' text-input overlay can block clicks.
- **Bursts of screenshots** across an animation (0.05-0.3 s apart) and side-by-side sheets: one screenshot hides
  most animation bugs.
- **Step a frozen game a tick at a time to catch a one-frame glitch**: `cmd /tick freeze`, then `cmd /tick step 1` and a
  screenshot, 60-70 times (one animation cycle), and a thumbnail sheet of them all. The leg kick at the end of the
  breath clip lasted one tick in 64: random screenshots missed it for hours, the stepped sheet showed it at once.
  When a fix "comes back", scan the data for every case, not just the one fixed (two broken keys, one mended).
- **Freeze the game for fast effects**: `cmd /tick freeze`, then `cmd /tick step 1` between screenshots. The effects
  run on the game clock, so they stop with it; a Suzu burst is over before the harness's next screenshot otherwise.
  The harness's `fx <effect>` plays one effect alone, which tells the effect apart from whatever else is going on.
- **A second player**: `gradlew runClient2` (a second dev client, harness port 25600), `/publish true 25565` on the
  first, `connect localhost:25565` on the second. See the hero as others do: third-person animation, the sounds
  others hear (subtitles on), effects, ally markers on a real player. Only the client drawing the change needs a
  restart for client-side work; server-side changes restart the host, and the second rejoins.
- **When a glitch shows up "at random" under different actions, look for something that runs underneath all of
  them.** The leg kick was blamed on three different clips; it was the idle breathing layer, broken keys every 3 s.
  Bursts of 40+ screenshots across a whole loop catch periodic glitches; then read the clip's keys (glb animation
  channels in Python) for single-frame jumps.
- **Measure frame rate as part of play** (`state` has `fps`; `timing reset` then `timing` gives our ms a frame;
  `timing stride N` draws every Nth triangle). Scale the suspect (triangles, effects) and watch the frame time: it shows
  whether the cost is ours (CPU) or downstream (upload, GPU). Window size changes rule fill rate in or out.
- **Suspect the test before the mod**: white pixel squares round a healed villager were Minecraft's death poofs from
  the villager the test script had just killed; burning "mobs" on the horizon were zombie horses in daylight.
- **Crop and enlarge** (`oc_crop.ps1`, nearest-neighbour at 2-6x) at native 1080p to judge HUD edges: a faint box
  round the volley bar only showed against the sky, zoomed.
- **Reproduce the owner's report first** (stuck on a step only when strafing; Alex in Minecraft mode), then show the
  fix with the same steps.
- **When the desktop is locked or can't take input**, the harness still types: `press key.keyboard.escape` sends a
  real key event through Minecraft's KeyboardHandler (Esc pauses, screens get keyPressed), `screenclick x y` clicks
  the open screen at GUI coordinates, `hover x y` puts the game's cursor there (hover states) without touching the
  real mouse. `window 1920 1080` matches the reference screenshots' size; check other shapes
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
- Check an engine assumption in its source before building on it: "the entity render types cull back faces" was
  wrong for the cutout pipeline (`withCull(false)` in RenderPipelines), and CPU culling built on it opened holes in her
  hair. A visual on/off comparison from several angles caught it.
- Additive light washes out over bright ground (gold over grass turns lime): keep additive for teal/cyan glows, blend
  warm colours (`Fx.soft`). And blend them in a colour picked on screen, not from the game's linear ramp: the ramp's
  bright end, gamma-encoded, is near-white cream (EffectArt's soft ramps).
- Faint mask values become strong colour once gamma-encoded and added (linear 0.17 is sRGB 115): a lens glint's barely
  visible halo drew as a crisp teal disc. Cut a mask's faint floor with a black point when only its core is meant to
  show.
- A camera-facing glow centred where something hit a surface sinks half into it and ends in a straight line: lift it
  along the surface normal by r sin(angle between view and normal) (`Particle.lift`), which clears the surface from any
  angle without floating far off it face on.
- Near-camera fades must scale with the sprite: a fixed 1.5 m fade let a 4.6 m glow two metres away cover the screen.
- When something draws nothing, test it alone and still: summon it motionless (summoned before `/tick freeze`, or it
  never reaches the client), look from several sides. Two "fixes" for a paper that wasn't showing were aimed at the
  wrong cause; the projectile models had never drawn at all (a prop-hiding rule for hero models matched them).
- Ask what the real thing looks like before building to a critique's guess: the critic wanted the ofuda's paper card
  shown; the owner (who plays) said thrown ofuda are glowing gold or blue talismans, and the effects survey agreed.
- The world's light reaches first-person models: at night Minecraft's lightmap turned the arms near-black purple. A
  view model wants a light floor (Overwatch's arms stay readable on night maps).
- A second key set: KeyMappings made but not registered with Options still get key events (KeyMapping's own maps),
  stay out of the Controls screen, and can be silenced or swapped with a mixin on isDown/consumeClick.
- **Don't compile while the dev client runs.** It loads classes lazily from the build output: a class first used
  after a recompile is the new one, its neighbours the old, and the game crashed mid-test (`NoSuchFieldError` on a
  renamed field). Stop the client, then build or `runClient`.
- 26.3 draws the first-person hands in their own pass (GameRenderer.renderItemInHand): the pose stack starts as the
  inverse of the view rotation, so a point from the arms' pose is camera-relative in the world's axes, not view space.
  Log a suspect space at two headings before using it: a point that turns with the view is in world axes.
- An effect that must sit on the first-person arms (the ofuda puff at a fingertip) follows a point recorded while the
  arms are drawn (`Fx.Particle.follow`); placed once in the world, it stayed behind as the hand moved on.
- A full-screen tint that seems to come from the HUD may be world particles at the camera: hide the HUD (F1) and
  measure again. Kitsune Rush's "mint grade" was eight 0.5-2 m smoke sprites spawned 0.6 m from the eye.
- A drawn-twice glow (a translucent body, then its added glow) can be hidden by draw order: the body drawn after the
  glow covered most of it. Make both passes added light, or the order matters.
- Read the mesh before changing its shading. The talisman's "grey back" was its own layer of triangles (368 on the
  front, 208 on the back) textured with Blizzard's baked shade (#CDCCCB to #756664 in atlas 31E3B), not a back face lit
  the wrong way round; a normal flip changed nothing there and, tested as the world's camera in the menu's
  orthographic, mirrored preview (`scale(s, s, -s)`), turned half her face dark.
- A hero's own sounds should be bound to them (`ClientboundSoundEntityPacket`, an EntityBoundSoundInstance on the
  client), not placed at a point: a point sound stays put while they teleport or dash, and fades behind them.
  In-the-head sounds (Overwatch's 2D ones): a client sound instance with `relative = true` and `Attenuation.NONE`.
- To check sounds, ask the client what it played: the harness's `heard` lists each sound since the last call, the
  file picked and its kind (fixed, bound to an entity, in the head). Minecraft re-announces looping sounds to
  listeners every 10 ticks: repeats in the list aren't restarts.
- The Ender Dragon is hit through its parts (EnderDragonPart, not a LivingEntity) and only with AI on; a summoned
  NoAI dragon never places its parts, so nothing can hit it.
- Minecraft's projectile hits ignore the projectile's size: `ProjectileUtil.computeMargin` widens the target boxes from
  0 to 0.3 m over the first 8 ticks. An Overwatch radius needs that margin replaced (`ProjectileUtilMixin`); test it
  with a still mob and shots aimed just inside and just outside the radius.
- Overwatch's shaders do work a glTF export loses. A one-colour texture on a blended piece over the eyes is a shadow
  the shader fades; an eyeball image with only the iris opaque gets its white from the shader. Check which bones a
  piece follows and what its texture holds before drawing it as is.
- Additive clips can carry big offsets, not just motion around zero: add_walk lifted the arms 85 px for as long as
  she moved. Measure a pose against still, not only how it moves.
- When a clip "looks wrong" to the owner, re-identify it from the game's own graph (alias chains, which sounds sit
  on it, length against the stat) and render strips. A whole action can be mislabelled: the "reload" was an ofuda
  throw, the real reload was filed as "equip". Renaming clips in a .glb doesn't need a Blender re-export
  (`tools/rename_glb_clips.py`); keep tools/clipmaps in step.
- A flat effect "on the ground" (a ring 8 m across) floats over any slope: lay it in short pieces, each at the top of
  the first solid block under open space near its point (`KirikoFx.groundY`), none where there's no ground in reach.
- A "feel" report (floaty, mushy) becomes numbers with a per-tick trace: `trace start`, hold keys with
  `press key.keyboard.w down` / `up`, then `trace` (horizontal speed from the position change, vertical speed, height,
  on ground). Run the same script on the old build and the new one (scratchpad mvtrace.sh pattern). For the camera,
  find the sky's edge in a few screenshots taken a frame apart: a still horizon in every frame means no bob.
- When a research note recommends a model, re-read its source sentence before building it: the movement note said
  "keep the approach model, it matches Goodman" while Goodman's post says the opposite (speed isn't slowed in a turn).
- A plain vanilla server for "the mod stays invisible" checks (#32): copy Loom's
  `~/.gradle/caches/fabric-loom/26.3/minecraft-server.jar` to a folder outside the repo, run it once with the Java 25
  Gradle downloaded (`~/.gradle/jdks/...25.../bin/java -jar server.jar --nogui`), set `eula=true` (the owner accepted
  Mojang's EULA for this, 2026-10-26), `online-mode=false` and a port in server.properties, and an ops.json with the
  dev player ("Kiriko", offline UUID = name-based MD5 of "OfflinePlayer:Kiriko"). Join with `dev "connect
  localhost:<port>"`; leave through the Game Menu's Disconnect (`screenclick 213 174` at 854x480).
- Damage tests on Peaceful with a full hunger bar heal behind your back (up to about 3 health a second): turn off
  `natural_health_regeneration` (26.x's name), or every fall or hit reads short. It looked like lost fall distance.
- A command's result in the log (`execute if ...` -> "Test passed/failed") arrives a moment later: count the lines
  before running it and wait for a new one, or you read the previous test's answer. Mind timings too: a wooden button
  pops back out after 1.5 s, a target's signal after 8 ticks.
- Minecraft's key for Enter is `key.keyboard.enter` (not `.return`); a wrong name is silently ignored.
- Projectiles pass through blocks with no collision box (buttons, plates): the hit lands on the block behind.
- Test courses in a superflat world: the player's feet at y -60 stand on blocks at y -61, so a "ground level" stair
  goes at y -60, not on a stone block at -60 (that's a 1-block wall).
- Fabric splits Minecraft's XP/locator bar in two: `INFO_BAR` (the background) and `INFO_BAR_FOREGROUND` (the XP fill
  and the locator bar's dot for each other player). Hiding only the first left a coloured square by the ult meter
  that showed only with a second player on LAN. Test HUD hiding with two players.
- Custom geometry (`submitCustomGeometry`) gets no outline from the entity's `outlineColor`: submit the mesh again
  with `renderType.outline()` (vanilla's outline pipeline: quads, position/texture/colour, the outline colour as vertex
  colour), as `DragonFireballRenderer` does. It lands in the outline phase; the post chain draws only the edge.
- A mixin that restyles every glyph of a line (the hero chat face) hides glyphs from other fonts as missing boxes.
  Replace only `FontDescription.DEFAULT`. For icons in text: a bitmap font provider for the glyph, a `space` provider
  for exact gaps (advance-1 spaces repeated make an indent of any width).

## Harness lessons (2026-10-27)
- 26.3 numbers mouse buttons from 1: `screenclick x y 1` is a left click, `3` right (`0` falls through to the hotbar
  check and does nothing). A container slot only takes the click once `hover x y` has put the cursor on it.
- `look` and an ability in the same tick act on the old aim (the server hasn't had the new rotation): Protection
  Suzu "failed to heal" twice for this. `look`, `sleep 0.2`, then the key.
- `/publish false 25565` opens a world to LAN for a second client without commands (`publish false survival 25565`
  fails to parse).
- Night in a normal survival world swarms the harness, which reacts a second late: it died three times in ten
  minutes. Fight one summoned mob at a time (`/gamerule spawn_mobs false`), keep wild nights for the general play.
- Hunger and saturation regeneration still run in hero mode (#43): read health within a second of a hit, or set
  `natural_health_regeneration` off, or a hit reads short.
- Check `git` with `-c safe.directory=E:/claude/mods/overcraft` on this checkout ("dubious ownership"); PowerShell
  mangles quotes in a here-string commit message: `git commit -F <file>`.
- `mouse dx dy` used to copy MouseHandler's routing (wheel pointer, else the view) and missed the emote camera the
  moment it was added: it now hands the movement to Minecraft's own `turnPlayer` (MouseHandlerInvoker), so whatever
  the redirect does is what the harness tests. Minecraft drops mouse movement while its window isn't active, which
  the dev window here usually isn't, so the real mouse can't reach it from the desktop tools.
- Build test floors with the floor first and the air above it second (`fill ~-7 ~-1 ~-7 ~7 ~-1 ~7 stone`, then
  `fill ~-7 ~ ~-7 ~7 ~5 ~7 air`): clearing first drops the player a tick before the floor goes in, the server and the
  client disagree on where she stands ("standing on air - force-sending blocks below"), and she falls through. On a
  jungle canopy that left a hole over the world spawn and a second death on respawn. A flat world avoids it all.
- A camera of our own (the emote orbit): inject at the TAIL of `Camera.alignWithEntity` and redo what vanilla does
  for third person with our angles: `setRotation`, `setPosition` at the pivot, `move(-getMaxZoom(distance), 0, 0)`
  (the block collision comes free). The camera type must still be a detached one, or Minecraft doesn't draw the
  player's own model; the mouse goes to the orbit through the same `turnPlayer` redirect as the wheel's pointer.

## Lessons (2026-10-27, second batch)
- **Overwatch's state logic answers timing questions.** The kunai's ammo point came from her weapon graph (statescript
  0x2171, in `E:\Overwatch Archive\research\kiriko-anim\dump_ss`): find the graph that plays the action's anim alias,
  then its Wait nodes and the SetVar literals their expressions read (there: 0.65, the "65%" of the patch notes).
- **Fit by eye with live knobs, not restarts.** For anything placed by eye (a held item's grip, a swing's axis, a
  size cap), put the numbers in a public static array with a harness command that sets them (`held`, `swing`,
  `heldmax`), try each axis both ways in one batch of screenshots, then write the winners in as the defaults.
- **Hide or show by sizes, measured.** `ItemStackRenderState.getModelBoundingBox()` gives an item's box as held (its
  display transform applied): its thin side tells a tool (across) from a sprite (along the view), and its longest side
  makes a size cap. A dev readout of the measured numbers found the cap that "didn't work" was working, just too big.
- **Overlapping flat triangles darken once in two passes:** mark the pixels in the frame's alpha (blend MIN, alpha
  only), then darken by the mark and reset it (colour times DST_ALPHA). Submit order is honoured across orders
  (`collector.order(n)`): order 0 first. A depth test alone speckles.
- **Expensive per-frame geometry gets a coarse copy.** Her shadow from her full 67k-triangle mesh cost 6 ms; vertices
  merged into 3.5 cm cells once per piece (kept triangles between three cells) look the same and cost 0.45 ms. Measure
  with the harness's `timing` and `shadows on|off`, alternating, with the inactive-window FPS limit off.
- **A per-tick fix can lose to the frame.** F5 was undone each client tick, but frames drew between the key and the
  tick: count the bad frames with a dev counter, show them with the fix off, then fix at the source (Options'
  setter).
- **Python edits to Java files: cut by marker, then diff.** Twice a slice taken from one marker to the next took a
  neighbouring method with it; `git diff | grep '^-'` before building finds it.

## Reading an emote from the game data (2026-10-27)
Porting Kiriko's emote props by eye took several rounds of owner corrections: the cat behind her, the board in her
hand, the donut never eaten, the umbrella stuck to her palm. The game data had every answer. Read it first (owner's
rule, CLAUDE.md); EMOTES.md has the emote rules, this is how the data was got at.
- **Each clip says what it spawns.** A clip's header names an animation effect (`teAnimation.Header.Effect`, an 08F),
  a chunked file read with `TankLib.teChunkedData`. Each `teEffectChunkComponent` (StartTime, Duration, and
  `Header.Unk`: the hardpoint) comes right before its component:
  - `teEffectComponentModel` (a model and its own clip);
  - `teEffectComponentEntity` (an entity: its definition names the model; a `STUProjectileVisualComponent` in it means
    it's thrown or dropped);
  - `teEffectComponentVoiceStimulus`, Sound, EntityControl.

  That gives which prop, on which hardpoint, from when to when in which clip. `tools/emote_data.py` reads it all into
  `assets/overcraft/emotes/<hero>.json`. Ids in effect headers carry type bits (a hardpoint 03C shows as 0xDC0...):
  mask with `& 0xFFFFFFFFFFFF`.
- **A model's full hardpoint table is in its STU chunk** (`teModelChunk_STU` -> `STUModel.m_hardPoints`: `m_EDF0511C`
  the hardpoint id, `m_888BCD8B` the bone, `m_position`, `m_rotation`). DataTool's .owmdl export writes only some of
  them. Kiriko's model has 39 and no 000A / 000B (the standard weapon hardpoints her emotes use). Lúcio's defines
  both. Her emotes hang them on the same bone with no offset; Lúcio's offset (180 degrees about X) turned her tea
  upside down.
- **Check an offset with a contact the emote must make.** A sip, a bite, a hand on a cat: `tools/emote_contacts.py`
  measures how near a point on the prop comes to her mouth over the clip, and which way the prop's +Y points.
  - The tea's straw with Lúcio's offset: never nearer than 0.55 m, upside down. On the bone itself: 3 cm at 7.27 s
    (her sip), upright.
  - The donut: 3.3 cm from her mouth at exactly 1.00 s, when the data's window ends. "Gone at 1.0 s" looked early,
    but that's her popping it in whole.

  Run it before overriding the data on a hunch, and before trusting a frame.
- **Our exported bones keep Overwatch's own frames.** World rotations from the raw .owmdl and from our glb match to
  three digits, so data offsets apply to the glb's nodes as they are. Blender's armature does not keep them: its bones
  point Y down the bone, which showed every hand hardpoint "turned a quarter about X" (a fix by eye was built on that).
  Never read frames from Blender's armature; read the raw reference pose (`OWModelReader`), or the glb.
- **Which clips are a prop's.** A clip whose bones are at least 80% hers (the union of her full-body clips' bones) is
  hers: 91-100%. Props' clips are 8-62% (fox, cat, umbrella, pod, the Whirlwind swirl), even when they share bone ids
  like the root. Bone count alone misleads: one of her own clips has 295 bones where the rest have 296.
- **A spawn with no clip of its own takes the prop clip of the same length.** Either as long as its window (starting
  with it), or as long as her clip (running with it: the pod's intro). Lengths agree to the frame.
- **Review at exact times, close up.**
  - Hold the emote at a time with the harness (`emoteat <s>`): sleeps between screenshots drift, and missed the
    donut's 0.73 s window.
  - Crop to her (`emote_frames.sh` in the scratchpad, crop 720,180-1200,900): at the full frame the upside-down tea
    read as "a cup in her hand".
- **Resource paths must be lower case.** A prop named by its upper-case id (`prop_105FB.glb`) threw out every
  resource pack at load. Ids are lower-cased into file names, and a bad name now skips just that model
  (`HeroModels`).
- **What hides a hero's weapons is in the blend tree, per clip.** Each clip has a `STUAnimGameData_PoseData`
  (`m_animationID` = the clip id) whose `m_geoSetFlags` are hide masks over time. Each submesh descriptor carries one
  geoset bit (`m_40`). Kneeling's 1 -> 3 -> 7 matched her putting her weapons away one by one, which checked the
  mapping.
- **`gradlew classes` doesn't compile the client source set.** It reported success on client code that didn't
  compile, three times. Check with `gradlew compileJava compileClientJava`.
- **Two callers of one per-frame clock starve each other.** Her pose is worked out more than once a frame, and the
  cloth took dt from the animator's shared `lastNow`: every call after the first got 0 and restarted it, so the coat
  never moved in emotes. A dev readout (the cloth's lowest point, stuck at its initial MAX_VALUE) found it in one try
  where screenshots hadn't. Give a simulation its own clock, and treat a repeat at the same time as a redraw.
