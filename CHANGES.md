# Overcraft: changes by day

A day-by-day log of development: releases, features, bugs fixed, investigations, and what's waiting on the owner.
Newest day first. Issue numbers are on https://github.com/Thrasherop/overcraft/issues.

## 2026-10-26

**The hero HUD rebuilt like Overwatch's** (#8; spec: the owner's gameplay screenshots plus
`tools/reference/kiriko-reference.md`, a reference sub-agent's measurements from the same video at 1080p).
- Laid out in pixels of a 1080-high screen and scaled to the window, so it looks the same at any GUI scale; both side
  groups curve up toward the centre as Overwatch's do (2.6-4.4 degree tilts, one curve formula).
- Bottom left: the portrait cut to a shield with a light-blue border, a "+" box, `250 | 250` (max smaller and dimmer),
  one white pip per 25 health (a nearly full pip reads full), a name plate with a glowing blue underline.
- Bottom centre: the ult meter: a dark disc, 60 radial ticks filling clockwise from 12 in orange, the charge in
  italic Big Noodle; ready: a white disc with the fox emblem in a glowing blue ring, the Q label inverted; running:
  white ticks counting down the rush.
- Bottom right: ability boxes (ready off-white with a black icon, just used coral, cooling down dark with an orange
  fill rising and the whole seconds in an outlined italic number, a cyan flash when back), key labels under them;
  the ofuda and kunai panels between bright blue lines, `ammo | clip`, and a mouse with the fire button in orange.
- Middle: Overwatch's "+" crosshair instead of Minecraft's (and its attack indicator); the five-segment ofuda counter
  under it (one segment a volley, spent ones from the right as dim outlines, a white flash on the refill); Swift
  Step's target as cyan corner brackets in a diamond on the ally, with its key.
- Minecraft mode keeps Minecraft's HUD plus a small ult meter right of the hotbar and "G: HERO MODE".
- Chat moves up above the portrait in hero mode (it sat on the health).
- Fonts: Overwatch's own (Futura No.2 D DemiBold for numbers and labels, Big Noodle Too Oblique for big numbers),
  read from the player's install into the local pack (`build_asset_pack.py fonts`; a fonts sub-agent found them in
  the game data: `tools/reference/fonts.md`). The jar's font files only point at Minecraft's font; `.ttf`/`.otf` are
  now on the build's forbidden-assets list.
- Shapes (discs, rings, the ofuda segment, the mouse) are drawn at load as smooth masks (HudShapes): original, not
  from the game.

**Hero select like Overwatch's 2026 screen** (#7; spec: the reference's section 6, from the same video at t=500).
- The hero itself large in the middle: the real 3D model (the player drawn as the previewed hero, following the mouse)
  when the pack has it; otherwise the hero's card art fading out at the knees, or the bust (Sombra and Illari have no card
  art in the extraction). "HERO SELECT" top left in Big Noodle, the name huge top right with role, sub-role and health
  (sub-roles from the wiki's infoboxes: Kiriko medic, Lucio and Sombra tactician, Illari survivor; the wiki lists Sombra
  as a support since 2026, so her role stays as it was).
- The roster along the bottom in role groups with a role badge, header and rule; the pick framed in orange with its
  name tag; heroes not playable yet dimmed "SOON"; an orange READY button (grey "COMING SOON" for those). Key hints along
  the bottom: F1 hero details, Esc keep hero / play as yourself.
- Overwatch hides the abilities behind F1; here their icons and keys show under the name, and F1 adds names and
  descriptions in a panel.
- Played: the screen on joining (screenshots `select_v2`..`v6`), F1 details on and off, arrows through all four heroes
  (Kiriko's model, Lucio's faded card, Sombra's bust), READY by click and by Enter picks Kiriko and enters hero mode.

**First-person screen effects** (the reference, section 2):
- Swift Step: the screen's edges flash teal and streaks rush in as she steps.
- In Kitsune Rush's path: a cyan mist at the screen's edges, pulsing, and light streaks flying out past.
- Each ofuda throw: a small soft golden puff at her fingers.
- Effects fade out as they come near the camera (a petal from her own arrival covered the HUD); the hand puff opts out.
- Played: Swift Step and rush in first person (`scr_sheet`), ofuda in the open (`puff2`: first try was a big yellow blob
  with the talismans' own glow blazing at the hand; now small puffs and faded talismans near the camera).

**Her coat no longer clips through her legs** (owner: "kiri's fabric things being clipped into legs"). Overwatch moves
cloth bones only with physics, never with clips, so her 24 coat-tail chains (7-8 bones each, hanging from the hips)
stayed rigid while the thighs swung through them. New ClothSim: per frame, each chain is pinned at the waist, pulled
back toward its drape (firm at the top, loose at the hem), swings with gravity, trails behind her motion, keeps its bone
lengths, and is pushed out of capsules around the thighs and shins, lifting outward over a leg rather than parting around
it. The trouser cuffs (5-bone chains on the knees) are left as they are. Stiffness, damping and radii are ours (the
game's cloth settings aren't in what we read).
- Reproduced first: strafing in front view, her thigh passed through the coat panel (`cloth0_g`). After: the coat
  swings and trails while running (`cloth1_*`), and the strafing leg goes under the coat, not through it
  (`cloth2_f/g`). A first try pushed points straight off the leg and the chains parted around it: still clipping.
- **The Suzu bell no longer sits in her hand while she runs, strafes, jumps or falls**: those clips carry it (the props
  survey), the game hides it until the throw; now it shows only during the throw (seen in `cloth2_*`).
- Suzu's flash is smaller (a burst at her own feet whited out the screen in third person).

**Real Overwatch effects instead of Minecraft particles** (owner: "I noticed you are still using minecraft assets like
particle effects for ofuda trails. Thats not how ofuda look in game"). An effects sub-agent read Kiriko's effect files
from the archived build (their particles, ribbons, meshes, timings, colour ramps: `tools/reference/kiriko-effects.md`;
DataTool can't export effects, so it parsed them itself); `build_asset_pack.py effects` copies the masks and converts
the torii gate mesh (new `tools/owm_static.py`) into the local pack.
- New client effect system (client/effect): camera-facing sprites, flat decals and rings, streaks, a dome, meshes and
  ribbon trails, drawn additively through a registered pipeline after the world, on the game clock. Masks coloured at
  load with the game's gradient maps (teal, homing gold, kunai cyan, Suzu blue and gold).
- The server sends effect events (EffectPayload) instead of particles. Every Minecraft particle the mod used is gone
  (ofuda dust, kunai crits, Suzu dust/end rods, Swift Step cherry leaves, Kitsune Rush dust and soul fire, the melee
  sweep).
- Ofuda: a ribbon and a glow on each talisman, teal or gold when homing; a five-petal flower on the healed ally, a
  flower decal where it hits the ground. Kunai: cyan ribbon and lens glints; six cyan sparks off a wall. Suzu: ribbon
  and sparkles; the burst's dome, blue ground wave, flower, flash, and a thin gold ring held 5 s. Swift Step: teal rings,
  a petal burst, a flash at both ends. Kitsune Rush: teal smoke at the cast, **six glowing torii gates (the game's own
  mesh) rising as the fox passes**, ground ripples, speed lines on allies, petals from the fox, a puff when it vanishes.
  Wall climb: dust puffs at the hands.
- Swift Step now lands beside the ally on Kiriko's side (it put the camera inside the villager's head) when there's room.

Played through the harness (screenshots `run/screenshots/fx2_*`, `fx3_*`, `fx4_*`, `fx5_*`): ofuda homing on a
villager (gold), ofuda at the ground (teal flowers), kunai into a wall (sparks), Suzu at the ground (dome, wave, gold
ring), Swift Step to the villager from behind, Kitsune Rush from behind (gates, ripples), wall climb (dust). Fixed on the
way: glows stacked into a blown-out yellow blob (one glow per projectile now), Suzu's ring lit its whole 8 m disc (rings
use only their bright edge now), the gates' beams stood upright, the climb dust spawned inside the wall.
- Not yet: real sounds (being mapped), the effects' enemy-red view, Suzu's white outline on allies, the floating talisman
  slabs on the rush path, slash arcs at Swift Step's arrival.

**Overwatch's controls and menus in hero mode** (owner: "Hitting G toggles between all keybinds. So esc in minecraft
mode opens minecraft settings. Esc in hero mode opens overwatch settings").
- Hero mode has its own key set (HeroControls, saved in `config/overcraft-hero-controls.json`): move, jump, crouch,
  primary/secondary fire, abilities, ult, reload, quick melee, interact, change hero, Minecraft mode. In hero mode
  Minecraft's gameplay keys are silenced (moving, attack, use, pick, drop, inventory, offhand, sneak, sprint); in
  Minecraft mode the hero's are. Chat, menus, F-keys and the hotbar's number keys work in both. The scroll wheel no
  longer changes slots in hero mode (it dropped you out of hero mode).
- **Defaults are Overwatch's: LMB = Healing Ofuda, RMB = Kunai** (the wiki; the owner's reference streamer had them
  swapped, and so did Overcraft until now). Swap them in Options if you prefer.
- New Interact key (F, Overwatch's): doors, chests, buttons and villagers in hero mode, since the fire buttons don't.
- Esc in hero mode opens Overwatch's menu (big italic items: Resume, Options, Change Hero, Minecraft Menu, Save and
  Leave); Esc in Minecraft mode opens Minecraft's. Options: tabs (Controls; Video, Sound and Mouse open Minecraft's
  own screens), the hero's actions by section with the hero's names ("PRIMARY FIRE  HEALING OFUDA"), click a key box
  then press a key or mouse button (Esc cancels, Delete clears), shared keys in red, Restore Defaults.
- Minecraft's Controls screen now lists only G (hero mode) and H (change hero) under Overcraft.

Played in the dev client at 1920x1080 through the dev harness (the desktop was locked, so no real mouse; new harness
commands `press <key>` (a real key event through Minecraft's keyboard handler), `screenclick x y`, `window w h`,
`tutorial`, `ult <percent>`), screenshots `run/screenshots/hud_v2_*`, `menu_v1`, `options_v2`, `hud_rebound`:
- HUD at rest, ult 59% (ticks to 7 o'clock), Suzu cooling down (12, orange fill low), ofuda 2/10 (one segment lit,
  four outlines), health 175/250 (7 pips white, 3 dark), Minecraft mode (small meter, hint). Compared region by
  region with the owner's screenshots at 2-3x zoom.
- Keys: W moves 5.5 m/s; E throws Suzu with no inventory; LMB throws ofuda (10 to 4), RMB a kunai (15 to 14); G to
  Minecraft mode and back, staying put both ways; E in Minecraft mode opens the inventory and throws nothing;
  crouch; wall climb up an 8-block wall and over.
- Menus: Esc in hero mode opens the Overwatch menu, Options, rebinding Ability 2 to C (saved; C throws Suzu, E does
  nothing, the HUD label reads C), Restore Defaults, the Video tab and back, Minecraft Menu (vanilla pause), Change
  Hero (select screen); Esc in Minecraft mode opens vanilla pause; F opens a chest while RMB at it throws a kunai.
- Not tried: the real mouse and keyboard, scroll wheel, a second player, a controller, other GUI scales and window
  shapes than 1920x1080 and 854x480.

## 2026-10-25

**Kiriko's real model in game** (branch `feature/kiriko`; the model itself stays local, #6). Overwatch build 154088
downloaded to the archive drive and read with DataTool (offline, Overwatch closed); Kiriko's model, 330 clips, the
Kitsune fox, her weapons and 2265 voice lines extracted (`E:\Overwatch Archive\README.txt`). New tools:
`tools/owm_survey.py` (Blender + overtools' io_scene_owm: survey clips, export a .glb with picked clips) and
`tools/owm_previews.py`; which clip is which: `tools/clipmaps/kiriko.txt` (the owner spotted the wall climb).
- HeroAnimator: finds Overwatch skeletons' bones (`Spine_1`, `Head`) as well as ValveBiped; 8-direction run and
  crouch-walk (falling back to 4 directions); run/crouch clips play at a rate that matches the ground speed (the
  clips cover 3.4 / 2.7 m/s, heroes move 5.5 / 3); climbing draws the model 0.3 m back from the wall (Overwatch's
  climb has the hands 0.6 m ahead of the feet: in Minecraft they were inside the blocks).
- HeroModelRenderer: held props the current clip doesn't use (Kiriko's Suzu, ofuda) stay hidden instead of lying at
  her feet. HeroTexture: model textures filtered smoothly with mipmaps (DynamicTexture's nearest-neighbour sampling
  made a 2048 px texture look like pixel art).
- Exports at Overwatch's 30 fps (Blender's 24 made every clip 25% slow).
- Dev harness `state` reports the playing clip (`anim`).

Played in the dev client through the dev harness (keys, camera, screenshots in `run/screenshots/k_*.png`; desktop
control was declined from the phone, so **not yet played with the real mouse**), fresh flat world "KiriTest":
- Join: hero select shown; Kiriko picked (harness `hero kiriko`). Renders textured, right way round (front: face and
  mask; back: hair), feet on the ground, idle holding a kunai, nothing at her feet (k_41/k_42).
- Run forward, back, left, forward-right: each plays its own direction's clip mid-stride (k_sheet_run).
- Ctrl crouch: crouch idle, then crouch-walk lower than the run, higher than the crouch idle (k_sheet_heights).
- Jump and fall from 12 blocks: feet-tucked air pose, then `land`; no fall damage (health 20 before and after).
- LMB kunai: arm throw over the legs' pose, kunai flies, ammo 15 to 14. RMB ofuda: arm up with talismans, ofuda fly.
  E Suzu: flies, 12.9 s cooldown, no body animation (clip not found yet).
- Wall climb into a 10-13 block stone wall: `climb` steady for the whole climb (15 ticks sampled), hands reaching
  overhead at the wall (k_sheet_climb4); before the 0.3 m shift the arms were hidden in the wall (k_sheet_climb3).
- Water: wades with the run cycle (no swim pose). /kill: death screen; the body falls as the Minecraft player (#9).
- Rejoin: hero select again and the hero reset (#9 asks whether that's intended).
- Not tried: mobs, allies (Swift Step, ofuda healing), Kitsune Rush, respawn, slots 2-9, second player, the Nether.
- Filed: #7 (select screen like Overwatch's; its cards also overlap the kit text), #8 (HUD like Overwatch's), #9
  (animations still missing: death, swim, Suzu, Swift Step, ...).

**Later the same day** (still uncommitted, waiting for the owner's real-mouse play), played through the harness:
- Death (#9): the renderer remembers each player's hero, so a dying hero keeps the model and plays `death` (the weapon
  is gone the moment they die). Seen: she falls backward, then Minecraft's death puff (k_sheet_death2). Respawn
  (new harness command `respawn`, what the death screen's button does): back to `idle` as Kiriko, hero select opens.
- Climb over the top (#9): running out of wall while still rising plays `climb_top` (5-block wall: 13 ticks `climb`,
  then ~0.3 s `climb_top`, then airborne).
- Suzu throw and Swift Step clips (#9): found by which clips move the held props' bones (`throw_suzu` = AD82, an
  overhand lob; `swift_step` = A50A, arm thrust forward: both guesses until compared with footage).
- Kiriko's kit against real targets: kunai on a husk 20.0 -> 15.28 health (60 Overwatch damage less its 2 armor:
  exactly Minecraft's armor formula); ofuda heal a hurt villager 10.0 -> 20.0 in 2 s (the first try read a village
  villager by mistake: not a bug); Swift Step z -29.5 -> -23.5 next to the villager, 7 s cooldown; Suzu 14 s
  cooldown; Kitsune Rush with the ult filled by advancements (2725 = the cost): 7.15 m/s in the corridor (5.5 +30%).
- Kitsune Rush's fox: a KitsuneEntity (not saved, not hittable) follows the corridor's head at 20 m/s and vanishes in
  a puff when the run ends; clients draw it with the fox's Overwatch model galloping (hero_models/kitsune.glb, clips
  in tools/clipmaps/kitsune.txt), full bright. Seen from the side running out of Kiriko (k_sheet_rush).
- UI from the game's own art (#7, #8; owner: "Ui should use game assets as well"): `build_asset_pack.py ui` copies
  each hero's portrait, card, full-body art, ability and perk icons (DataTool extract-hero-icons) into the local pack,
  with .mcmeta blur for smooth scaling. Select screen: card art large on the right, ability icons in the kit, real
  portraits in the roster ("soon" greyed), key column sized to the longest key, descriptions only when there's room
  (k_102). HUD: portrait by the health, ability icons dimmed with the seconds left, kunai icon by the ammo (k_101).
  Without the pack, the old drawn placeholders.
- Seen, not fixed: Hero.SOMBRA is listed as a Support; she's a Damage hero in Overwatch.
- Quick melee, every hero's (key V, Overwatch's default): 40 damage to every enemy within 2.5 m in front (45 degree
  half-angle: ours), very small knockback, 1 s cooldown from the press, 0.528 s weapon recovery (wiki "Melee").
  Played on a husk: 20.0 -> 16.85 (40 less its armor), a second V 0.5 s later did nothing (cooldown), a third after
  the cooldown 16.85 -> 13.70; the animator played `melee_2`.
- Role badges from dump-ui-textures (tank, support, damage) beside the role on the select screen (k_105).
- Treading water plays the air pose (Overwatch has no swimming).
- First person: Overwatch's first-person clips aren't in the extraction (all 330 are 3rd-person; see HANDOFF).
- Filed #10: Mercy (owner).

**Owner's play notes, fixed** (owner played the jar; then real mouse/keys via desktop control where Windows allowed,
the harness otherwise; screenshots run/screenshots/k_12x-k_13x and the h_* captures in the session scratchpad):
- Projectiles drawn as flat camera-facing sprites: now the kunai/ofuda/Suzu Overwatch models (projectile_*.glb),
  long side along the flight. Suzu seen as its 3D bell in flight; kunai too fast to catch on camera.
- Climbing a short wall launched the model above it: climb_top's root motion (bone_0001 rises 2.2 m) is now ignored
  (Minecraft moves the player). 3-block wall: climb, climb_top, lands on top, model stays with the player.
- Kunai throw "jumps into her": the clip starts at the release (arm cocked behind, left hand); actions now swing out
  to their first frame over 0.1 s, then play. Seen: arm out, then sweeps forward.
- Idle was the "hackles up" pose (B00B); B033 tried next is airborne (feet 0.15-0.4 m up). Neither is right: a
  research agent is mapping her animation data (tools/clipmaps/kiriko-research.md when done).
- Crouch idle (CFF8) is a phone-typing emote (owner): dropped; standing crouched holds the crouch-walk pose.
- Minecraft mode (slots 2-9) showed Alex: the chosen hero now syncs to every client (attachment registered at mod
  start: Fabric agrees on synced attachments while connecting), so players keep their hero's model out of hero mode.
- Couldn't jump onto a 1-block step except facing it (wall climb): Overwatch's jump peaks at 0.98 m. Ledge assist
  (ours): in the air, pushing into a ledge at most 0.3 m above the feet lifts the hero onto it. Before: strafing
  into a step stuck at x 41.7; after: strafing, backing up and facing all cross it.
- Ofuda throw made her look up: that clip (B104, a guess) is dropped until the research says which is right.
- Real sounds: OcSounds + the jar's sounds.json (vanilla stand-ins), replaced by the local pack (`build_asset_pack.py
  sounds`, tools/soundmaps/). Kitsune Rush plays "Let the Kitsune guide you!" to Kiriko and allies and the Japanese
  line to enemies, from 64 blocks. Seen as the subtitle "Kiriko: Let the Kitsune guide you!"; not heard (no audio).
- First person (owner: "should look like gameplay"): hero mode draws Overwatch's first-person view model instead of
  Steve's hand. It's Kiriko's own model imported through the Gameplay1P entity (its model look textures only the
  arms/hands, which the 3P export dropped as material-less), exported as hero_models/kiriko_1p.glb with the 1P clips
  (tools/clipmaps/kiriko_1p.txt, from the research), placed with its Camera bone at the eye. Seen: both hands in
  front, ofuda fanned in one hand (k_150); kunai throw plays (k_153-155). Not yet: breathing/sway layers, walk bob,
  Minecraft's view bob/sway on the arms.
- Standing and crouched idles: A6F0 / AEE7 held at frame 0 (the research: their frame follows a game parameter,
  turn-in-place, and holds otherwise). Owner: "much much better".
- Not working with desktop control: F5 and two keys at once (W+Space) can't be sent; computer-use screenshots show
  the game as blank (plain screen captures work); Windows' text-input overlay can block clicks.

**Kiriko, first pass** (branch `feature/kiriko`, not on `main` yet: not played in-game). Compiles, unit tests pass
(units, run speeds, jump apex 0.98 m, 30 m/s fall cap, 0.1 s ground acceleration, Suzu's 7 s straight-up flight).
The dev client launched with the mod loaded, but couldn't be played this session (desktop control needs the owner's
approval in a live message), so **none of this has been played by hand yet**.
- Hero select screen on joining a world (Kiriko playable; Lúcio, Sombra, Illari shown as coming soon); H within 5
  blocks of your bed (or world spawn without one) to change heroes (owner); the pick on joining is free.
- Hero mode: G toggles (owner), or hotbar slot 1. The hero weapon lives in slot 1; vanilla hotbar, hearts, food,
  armor and XP bar give way to the hero HUD (health in Overwatch numbers, ammo, cooldowns, ult %).
- Overwatch movement in hero mode: 5.5 m/s (90% back, 93.3% diagonal back), ~0.1 s accel/stop, jump 5.72 m/s
  (0.98 m), gravity 16.7 up / 17.5 down easing to 30 m/s, Ctrl crouch 3 m/s, no sprint, no fall damage. Air control
  value is ours (none published). The jump uses trapezoid integration: Minecraft's gravity-then-move order peaked at
  0.83 m (caught by the unit test).
- Kiriko (numbers from the Oct 2026 patch notes and the wiki, cited in KirikoStats): kunai LMB (60, 2x head, 0.5 s,
  15 ammo, 90 m/s), Healing Ofuda RMB (12 x2, 7/s, 10 ammo, 0.9 s reload, 18 m/s, 30 m homing lock), Medic 40%
  self-heal, Swift Step (30 m, 7 s, 0.2 s cast, through walls), Protection Suzu (14 s, 4 m, 80 heal, cleanse, 0.5 s
  invulnerable), wall climb (8.3 m/s, 1 s, once per airtime), Kitsune Rush (2725 points, 32.2 x 10 m corridor for 8 s:
  +30% speed, +50% attack/reload, 2x cooldowns, 1 s linger). Global regen after 6 s.
- Ult charge: 5/s, 1 per damage/healing, and advancements (owner): task 10%, goal 25%, challenge 50% of the cost.
- Placeholder art (tools/gen_textures.py) and vanilla sounds until hero assets come from the owner's install.
- Local Overwatch asset pack (#6): the mod mounts `<game dir>/overcraft/overcraft-assets.zip` (dev: `local-assets/pack/`)
  as an always-on resource pack; `tools/build_asset_pack.py` converts a model with Blender into it (checked with a
  test cube); `gradlew build` fails if the jar contains a model/mesh/audio file (checked: a .glb in resources failed
  the build). Nothing renders the hero model yet: that comes with the first real model. DataTool v2.25.0.0 can't read
  the installed build 154504 yet (supports up to 154088): waiting on upstream.
- Issues #1-#5 filed (Juno, Sojourn, communication wheel, emotes, voice lines).

Project started. Fabric for Minecraft 26.3 (Loader 0.20.1, Fabric API 0.165.0+26.3, Loom 1.19); 26.x ships
unobfuscated, so there are no mappings to pick. CLAUDE.md carries over Lethalcraft's working rules.
