# Juno (hub)

Everything learned about Juno, by subject. The ninth hero (issue #1), next on the owner's list after Brigitte: a Support,
Survivor. Up: [Techniques](../README.md) · the method: [reading_game_data](../game-data/reading_game_data.md) · measured
with: [workshop_recordings](../game-data/workshop_recordings.md) · built the way Brigitte was: [brigitte](brigitte.md)

## Her notes and sources
- **Numbers:** `tools/reference/juno.md` (Blizzard's patch notes to 2026-11 and the wiki's revision 406865; nothing has
  changed her kit since the data build: BZ 2026-10-06 was hit volumes only).
- **The data map:** `tools/reference/juno-map.md` (her kit and movement from her graphs, node by node); the detail in
  `E:\Overwatch Archive\research\juno\` (`work_kit\kit_juno.md`, `work\assets.md`, `work\clipmap.md`, `extract\`).
- **The plan:** `tools/reference/juno-plan.md` (where the sources disagree, what maps onto Overcraft, the order).
- **Her sounds and voice (read 2026-11-01, not yet wired or heard):**
  - The map is `tools/soundmaps/juno.txt`: 70 rules and 35 voice events, each with its 02C, effect, graph node and
    confidence.
  - The wiring list is `tools/reference/juno-sounds.md`.
  - What the pass taught (volley streams, loop beds, copies picked by the local player, the decompiler's duplicate uids):
    [sounds](../sound/sounds.md#juno-sounds).
- **Her movement in the real game:** the owner's Workshop recording of 2026-11-01 (`tools/workshop/movement_recorder.txt`),
  read by `tools/workshop/analysis/juno.py`; the log and its tables in
  `E:\Overwatch Archive\research\movement\workshop-2026-11-01\`.

## What the recording settled (2026-11-01: 25 double jumps, 7 glides, the hovers)
- **The double jump sets her vertical speed to 10.80 m/s**, every time, whatever she was doing (rising, falling, gliding),
  and keeps her horizontal speed: the wiki's number, not the data's "x1.8" read as 1.8 x our 5.72 jump (10.3). About 3.4 m
  up from a standstill. The earliest after leaving the ground: 0.16 s.
- **The hover brings her fall to exactly 3 m/s, eased, not clamped:** from -9.2 m/s it went -6.86, -5.67, -4.95 ... -3.1 in
  0.27 s (the gap shrinking about 16 per second). Only the tall drops caught it: the data's 2.5 m of air below her.
- **Glide Boost adds 14 m/s** (5.5 -> 19.13 one tick in; 5.81 -> 19.44): the data's number; the wiki's 19.1 is the total
  from a run. **It slows the same way every time:** about 3 % a tick (1.86 x her speed per second) down to 12.45 m/s, then a
  steady 23 m/s², to her glide speed 7.70 m/s (5.5 x 1.4) at 0.5 s. With no key held she keeps her speed: the glide goes
  on. Strafing steers it (the speed dips while it turns).
- **Climbing in a glide:** its vertical speed tops at 3.00 m/s; up a stair she rose 12.5 m in one glide.
- **The second press ends it** (3.41 s into one) and its 6 s cooldown was already counting at that press: from the end.
- Gliding from a fall cancels the fall (-8.57 -> 0).

## The model
- One body model for both views (0159.118, looks 0939 3P / 093A 1P): the gun and the torpedo pod are her geosets.
- **The `--entity` import crashed headless Blender** (the add-on's error popup has no window: `popup_menu_end`), as it had
  for every Brigitte model. `tools/owm_survey.py` now swaps the add-on's `createPopup` for a printed `OWM_POPUP` line, and
  the import goes on: her first-person look came out as its 8 meshes (25 without the entity).
- **A layer named "breathing" can be anything:** her clip map's `add_breathe` (EBAB, an M mapping) turns her spine 97 degrees
  and a dozen bones 90-175: played on her idle she flailed (gun overhead, bent double, found playing). Compared with
  Kiriko's and Brigitte's `add_breathe` (a few degrees each), her EC2F is the breathing layer; EBAB and EBB0 now keep names
  the code doesn't play (`tools/clipmaps/juno.txt`).

## Steps 1-2 (the shell and her movement), played
Dev client, flat world `juno1`, survival; measured with the harness's own velocity (`vx/vy/vz`), one reading a tick:
- Picked from the select screen (her model, "Support, Survivor, 75 health, 150 shields", six kit icons; her helmet's face
  showing after the glow fix); 18 Minecraft health (225); her HUD: Glide Boost (LShift), Hyper Ring (E), the torpedoes,
  the Mediblaster's 180; standing still, a steady idle (after the breathing fix: juno_idle / idle_sheet2.png).
- **Double jump:** a second press at about 0.25 s takes her to 4.46 m above the ground; one only. **Hover:** dropped from
  20 m holding jump, her fall settles at exactly -3.0 m/s (first it settled near -5.5: gravity still pulling under the
  easing, fixed); no fall damage at 20 m, hovering or not.
- **Glide Boost:** from a walk 5.5 -> 17.74 m/s on the first tick, 7.7 by 0.55 s (the recording's curve); with no keys
  7.7 from 0.5 s on (it first stayed at 14: the slowing only ran toward a pressed key, fixed); mid-air level for its 4 s,
  then a fall and the 6 s cooldown (which first never counted down: her kit didn't tick it, fixed); up an 8-step stair in
  one glide (7.85 m); a double jump mid-glide lifts her 3.5 m and she holds the new height; a second press at 1 s ends it
  (the cooldown from there), one at 0.2 s is ignored.
- Not yet: her weapons, Hyper Ring, Orbital Ray (steps 3-5); her animations for the double jump, hover and glide.

## Steps 3-4 (Mediblaster, Pulsar Torpedoes), played
Dev client, flat world `juno1`, survival, night; health read with `data get`:
- **Mediblaster:** one volley (the left button held 0.3 s) at a zombie's body 6 m off: 200 -> 193.29 (90 = 7.2, less a
  zombie's natural armor), 12 rounds spent; held 2 s: 36 rounds (3 volleys, one per 0.8 s); at its head x1.5 (10.1); a
  villager at 4 health: 9.76 (+5.76 = 72 exactly); a zombie 30 m off: 4.49 (65 % falloff, less armor); R: full again by
  0.9 s (0.75 s), from 96 rounds.
- **Falloff kept although the data's graphs don't read it:** Blizzard's patch notes changed it in 2024-10 and 2025-07, so it
  is live (the engine's, M); the patch notes win (CLAUDE.md).
- **Pulsar Torpedoes:** a villager (ally, 4 health) 8 m and a zombie (200) 10 m ahead, the right button, 1.2 s, the left
  button: the villager 11.92 at 0.8 s (85 + the start of 50 over 2.5 s) and 14.80 at its end (+10.8 = 135); the zombie
  193.31 (85, less armor). With nothing in view: the lock cancelled by the right button, a new one refused at 0.5 s and
  allowed after 2 s (the 2 s cooldown).
- Not yet: the lock's on-screen marks and the torpedoes' look (step 7: until then nothing shows); its aim cone (D 103,
  read as the field of view: L) to settle by play with several targets.

## Step 5 (Hyper Ring, Orbital Ray), played
- **Hyper Ring** thrown level ahead, then a run through it (it hangs 17.5 m out): her speed 5.5 -> 8.25 m/s once through it;
  its cooldown counting from the cast's end (9.2 s left 4.8 s after the press).
- **Orbital Ray** at full charge, a villager at 4 health 6 m ahead: 10.8 one second after it landed (+6.8 = 85 a second);
  a Mediblaster volley inside it at a zombie: 117 (90 x 1.3, less armor and the zombie burning in the noon sun); no ult
  charge while it runs; the villager full by its end.
- Ours: the ray's +30 % shares Mercy's damage-boost slot (the two don't add up yet); its speed ramps linear (the data's
  curve M).

## Step 6 (perks), played
Each perk against its control (`overcraft perks @s 1`: none), picked through the perk menu (perk key held, a click: the
left button for the first of a tier, the right for the second), dev client, flat world `juno1`, noon:
- **Familiar Vitals:** a villager at 4 health 30 m ahead, 0.7 s of lock, then the left click: 4 -> 11.9 (85 at once and
  the heal over time starting); without it 4 -> 4 and the 2 s cancel cooldown (30 m needs 0.81 s, 0.53 with the perk).
- **Locked On:** two zombies 8 m ahead locked 1.2 s and hit: the torpedoes' cooldown 6.4 s 1.5 s after firing; without it
  8.4 (-1 s an enemy hit).
- **Faster Blaster:** the left button held 2 s while gliding: the ammo falling a steady ~2.2 every 0.1 s (42 rounds: 22 a
  second, no gaps); without it volleys of 12 with the 0.45 s pauses between (36).
- **Lift Off:** three jump taps 0.35 s apart: the top 7.57 m above the ground; without it 4.23 (the third does nothing).
- **Found playing (the controls):** the left click that fired or cancelled the torpedoes also fired a Mediblaster volley
  on the next tick, even a 0.12 s tap: the 30 m villager came out +3.95 with nothing locked (a volley at 65 % falloff).
  The click that ends the lock now fires nothing until it's let go (`HeroState.junoLeftSpent`); played again: 4 -> 4.
- The zombies' extra damage (a few points over 85) is the noon sun burning them, not ours.

## Step 7a (her movement and ability clips), played
- **Base states, seen by every client:** the glide from a synced flag (`HeroManager.JUNO_GLIDE`), the double jump and the
  hover read from her motion (an upward kick in the air; a fall held at 3 m/s for 3 ticks), as Mercy's descent is. The
  animator played: a plain jump `jump > fall`; a double jump `double_jump > double_jump_fall`; held, `hover_start` once
  the fall settled at -3.0 m/s; the glide `glide_start_forward` for 0.57 s, then `glide_forward`. Front-view
  screenshots (`run/screenshots/juno_a_*.png`): boots together on the double jump, knees bent and arms out in the hover,
  a knee drawn up gliding.
- **Actions:** `ring` > `ring_throw`, `ult` > `ult_cast` (in the air `ult_cast_air`), the torpedoes' lock and fire as 3P base
  additives (df 0xAE7) and 1P clips (`torpedo_lock_start`, `torpedo_fire`); the glide's own action names play nothing
  (they used to start the 2 s glide loop as an action).
- **Found playing: her torpedo pods flew apart.** The lock's additive moves the pods (bones 3CFB / 3CFC, a mirrored pair:
  the second turned 180 degrees about X) about 0.55 m: added in the parent's frame one went over her head and one to her hip
  (`juno_b_lock.png`). Turned by each bone's own rotation first, as the clip's turns already are, they open as a mirror
  pair over her shoulders (x +-0.28, y +0.55) and rise apart on the fire. That was her layers' option first; since #75
  it is how every additive clip is added (the importer's own additive path does it, and mirrored pairs across the
  heroes agree: [additive_clips](../animation/additive_clips.md)).
- Ours (M): the glide's 8 ways by her motion, not her input (only her own client knows the input); the hover's ending
  layer and the torpedo hold layer (the lock isn't synced to others) not played; her 1P glide / hover layers not yet.

## Step 7b (her effects), played
`JunoFx` draws each moment of [juno-effects](../../tools/reference/juno-effects.md) with her 158 `JUNO_*` palettes
(`EffectArt`, from `fx_palettes.txt`; the `_N` rows uncommented). The server adds `juno_round` / `juno_round_block`
(each Mediblaster round), `juno_torpedo_wall`, `juno_ray_ally`, and `juno_locked` now names who locked (her marker shows
on her client only). Her pods, feet, muzzle, wrist and gun piece come from her posed bones (`JunoFx.POINTS`).
Played 2026-11-01 (dev pack; `run/screenshots/fx*_*.png`): rounds on a villager, the ground, a zombie; lock and volley at
both; the ring thrown, hanging, walked through (1P and 3P lines), its end; the ray cast, landing, held, walked into
(3P and 1P), from 40 m; the double jump, hover and glide. What playing taught:
- **34 rounds a second stack:** the ally hit at the data's 2.4 m (and at half) was a solid ball hiding the villager; 0.3x.
- **A quarter-circle mask:** the ray circle 4DBCC holds one quarter, its centre in a corner; on one quad it drew four
  scalloped arcs. Four quads turned round the centre (`JunoFx.quarter`) make the 17 m circle.
- **A tube's scrolling mask doesn't fit a quad or a still tube:** 31951 on the column mesh was a flat-ended white bar,
  on quads hard-sided; inside the column the camera-facing pieces whitened the view. Soft line pieces falling 40 m/s
  (its scroll), faded out from 4 m to 1 m off the axis.
- **Flat discs need their mesh:** the disc flash's band is a stripe in V (the radius); on a quad it drew a bar. 5E5E.
- **Mesh-only masks:** the chevron masks 27D0D / 27D0A are a frame and a ramp for DFDC; on quads they were squares.
- **Jets spawned per tick trail behind a rising body:** held on the foot (`Fx.attach`) they stay on her boots.
- Not done (ours / left): the lock marker is a world ring (the data's is HUD 0727.116); no 1P glide screen hex; no body
  material tint (a glow instead); the ray's ground circle is one flat at its point (floats on slopes); the reload's gun
  piece swap; enemies' red rows unseen (no second player).

## Step 7c (her sounds), played
Her 92 events ([juno-sounds](../../tools/reference/juno-sounds.md), `tools/soundmaps/juno.txt`) registered by
`OcSounds.JUNO_SOUNDS` / `JUNO_VOICE`, her own stimuli in `Voice`. The server's one-shots go through `juno.JunoSounds`
(as Brigitte's); everything that follows a state is her clients' (`client.JunoSounds`): the glide (synced
`JUNO_GLIDE`), the lock (a new synced `JUNO_LOCK`), the hover (her motion, read over 4 ticks for others), the torpedoes',
ring's and ray's loops at them as `JunoFx` flies them (`JunoFx.sounding`), the ray's inside loop and amp on this client's
player (`JunoFx.inRay`). Her double jump is sent by her client (`OwMovement.doubleJumped` -> `EXTRA_JUMP`), the server plays
the others' copy. Ult line: `ult.friendly` / `ult.enemy` through `HeroSounds.ultLine`; `voice.ultimate` (the same line)
unplayed.
Played 2026-11-01, dev pack, host "Kiriko" as Juno and `runClient2` "Viewer" over LAN, each sound read from both clients'
`heard` and the new dev `playing` (what is still sounding: the channel asked whether stopped), polled every 0.1-0.2 s:
- **Mediblaster** at a 200-health zombie, a hurt villager, a wall, Viewer as ally and as enemy (teams): one `volley` +
  `projectile` per volley 0.8 s apart, `volley_end` as it ends, 12 `hit` (+ `hit_enemy`) a volley in her head, Viewer
  `volley_3p` / `volley_end_3p` at her and 12 `hit_enemy_3p` at himself; `hit_block` for both; "Let me help!" on the
  villager's heal. R: `reload` + her line in her head, done 2.2 s later.
- **Pulsar Torpedoes:** lock start / loop in her head at the press (the loop again 3.19 s later), Viewer the _3p pair on
  her; a tick at each target, both stopped within a tick of the second lock (0.6 s before their files end); `lock_target`
  per lock; fire, line, one `launch` per torpedo, the flight loops following each torpedo and gone as it hit (enemy 0.12 s,
  ally 1.1 s); `hit` / `hit_3p`, `hit_block` on a wall put up after the lock; `heal_confirm` hers, `heal` in Viewer's head;
  "Support inbound!" with Viewer locked, "Pulsars inbound!" otherwise; Viewer `lock_end_3p`. Cancel with nothing locked:
  `fire`, the lock's loops stopped.
- **Hyper Ring:** cast + line at the press, `launch` / `appear` 0.25 s in, its hum following the ring, `ring.pass` in
  Viewer's head as he crossed it (`pass_3p` at a villager), `end` 5.85 s after the launch, the hum stopped at 6.0 s.
- **Orbital Ray:** cast + `ult.friendly` (Viewer an enemy: `ult.enemy`, no amp), the hum from the landing to 8.0 s, `ending`
  at 5.3 s, the inside loop and amp start / loop for her and Viewer inside, amp end and both loops stopped as the ray walked
  off her.
- **Boots and glide:** double jump (on the ground and mid-glide) hers and Viewer's copy; hover start / loop / end at the
  release, both sides; glide start / loop, `ending` with 0.4 s left (hers) and 3.0 s in (Viewer's), `end` at 4 s, loops
  stopped.
- **Stopping:** died mid-hover (loop gone in 0.1 s); hero switched mid-lock and mid-glide (loops gone); Viewer kicked
  mid-ray (all his loops gone). Voice: select, respawn, death, pain, melee (+ her 1P melee foley), elimination, ping.
- **Found playing:** another player's double jump read from motion missed some and doubled others (their steps arrive held,
  then caught up): now told by her own client. Their hover ended and restarted mid-hover the same way: their fall is read
  over 4 ticks and ends after 4 ticks out. `playing` at first showed stopped sounds for up to 1 s (the engine keeps a
  stopped channel until 1 s after its start): it now asks the channel.
- Not wired: `fire_alt` (L which branch), the Stadium ones, `idle.loop_3p`, `jump` / `jump_3p` / `land`, `perk.ring_refund`
  (L live), `voice.ping_ring` (no ring ping), Faster Blaster's stream (no volley while it fires). Not listened to for
  balance (24 hit sounds a volley in her head).

## Step 8 (her emotes), played
All 22 from the data ([juno_emotes](../emotes/juno_emotes.md)): 75 clips in `hero_models/juno_clips.glb`, her data
`emotes/juno.json` (`tools/emote_data.py juno`), the props (mic 1A146, pom-poms 1920B, sled 1AB3F, Galaxy Gazing's mobile
17A7C with its clips, Game Start's hologram 11DA3) in the pack. What it took:
- **Her emotes' mask 1 is the Mediblaster, not her geoset 1 (her body):** the mask-1 windows are the put-away windows of
  the gun cell's slot E709, and Cheer Routine's pom-pom goes in the hand the gun stays at. `emote_data.py` maps her weapon
  pieces to mask bits (gun 1, pods 2: M) and borrows 000A / 000B (Weapon_R / Weapon_L, Brigitte's) for the mic and the
  pom-poms.
- **Listen Up and Salty:** no mask, but the shared carrier puts E709 away; her gun now follows those windows
  (`stowPieces`, found playing: the gun by her head).
- **Shuffle played its 0.4 s intro only** (it starts 0.011 off her swaying idle, over `EmotePlan.SAME`): the nearest clip
  under 0.013 now starts an emote with none closer; no other hero's emote is in that gap.
- Played (dev client, world `junoemo`, picked from the select screen): every emote at four moments, the props held at
  their windows, three picked through the Voice Lines -> Emotes loadout and the communication wheel. Not yet: their
  sounds (done in step 8b, below), first person, Sledding's two other loops, Game Start's hologram colour (Sombra's violet).

## Step 8b (emote sounds, voice lines), played
Her emote sounds the way Sombra's and Brigitte's came ([sounds](../sound/sounds.md#juno-emote-sounds)): 48 02Cs and 21
voice stimuli from her emote clips' effects, `tools/soundmaps/emotes.txt` (Juno's section), the play rules (loop, fade,
chance, audience) copied into `emotes/juno.json` (16 events). Her 27 voice-line unlocks (DataTool `extract-unlocks
"Juno|voiceline=*"`, in `research\juno\extract`) in the pack: `build_asset_pack.py`'s `VOICELINE_HEROES` + `VOICELINE_ROOTS`.
Built in a scratch root and merged into the dev pack (only `juno.emote.*` / `juno.voiceline.*`, their lang keys and
`voicelines/juno.json`).
Played 2026-11-01, dev client, world `juno1`, Juno picked on the select screen, every emote started with the dev `emote`
and `heard` polled every 0.05 s from the command (times below include about 0.13 s of command latency):
- **Heart Wave** F496 0.13. **Bunny Hop** F869 at 0.60 and 3.75 (0.17 into each 3.2 s loop pass). **Orion Dance** FEC8 +
  FED2 0.13, FED4 0.34, FED5 0.40, loop FEC7 + FED4 + FED3 0.47 and again 5.55. **Listen Up** E548 0.13, E549 0.20 (the
  "others" E54A / E54B not heard by her own client, as meant). **Mic Drop** DA44 1.00-1.07; DA4F (chance 0.4) in 1 of 5
  plays, at 0.27. **Salty** EEA6 0.13. **Disapproving** BA89 0.13. **Giddy** BA85 0.20, lines 179F 0.33, 17A0 0.80,
  17A1 2.21. **Laser Training** BA87 0.13, 17A7 1.34, 17A8 1.80, 17A9 2.27, 17AA 2.74. **Heroic** B90B 0.13, 17A4 1.87.
  **Laugh Attack** BA86 0.13, 17A6 0.60. **Winding Down** BA8B + BA8A 0.20, 17AF 1.20, BA8A 1.87, loop BA88 2.87, BAA9
  2.94, BA8A 3.14, 17AB 3.68, 17AC 4.21, 17AD 8.35. **Game Start** C37F 0.20, 17AB 1.20, 17A1 2.07. **Release!** BC6D +
  BC6F + BCB3 0.13, 17AB 1.27, 17AC 2.81. **Galaxy Gazing** BFFB 0.13, BFFC 0.87, 17AB 1.00, loop BFFD 2.27, C045 (the
  mobile's prop clip) 3.01, again 7.15 / 7.88. **Crazy Choreography** C381 + C441 0.13, loop C380 + C440 0.67, 187D
  2.07, 187E 3.48, loop again 8.01. **Chun-Li Wins** C880 0.13, C8A4 1.40, 18CE 1.80, loop C8A4 4.07. **Spinning Bird
  Kick** C8A0 0.20, 18CA 0.47. **Cheer Routine** D8AC 0.33, loop D8AD + D8C4 1.61, 192E 1.94, AD52 2.14, 192D 4.01, again
  5.28 / 5.68 / 5.95 / 7.76. **Blooming** DABF 0.13. **Shuffle** 1965 4.81, E23F 5.01. **Sledding** F247 0.60, 19B0 1.00.
  Every one at its data time; no missing-sound warning in the log.
- Ending: Galaxy Gazing's BFFD loop stopped as a held jump ended the emote (`playing`), the one-shot C045 rang out.
- **Voice Lines:** Esc -> Voice Lines listed all 27 (screenshot `j8b_voicelines.png`), Seize the sol and Lightning kicks
  equipped up / right (`j8b_equipped.png`), then said from the communication wheel (T, left button to the voice page,
  pointer up / right): `juno.voiceline.seize_the_sol`, `juno.voiceline.lightning_kicks` in her head, subtitle "Juno:
  Lightning Kicks!" (`j8b_wheel_voice*.png`). Heroic from the wheel's emote page: B90B + 17A4 (`j8b_wheel_emote_playing.png`).
- Not heard: Winding Down's BAA1 (on the outro F38B, which we don't play) and Sledding's F232 (the sled's slide loop, on
  the 0.033 s clip 1AB5A that our plan never plays). Not checked: the "others" copies from a second client; a
  voice line's right-click preview in the list (the harness's right `screenclick` equipped the line instead: not tried
  with the real mouse). Not listened to for balance.

## Step 9 (the release play)
2026-11-01, worktree `overcraft_juno` (branch `feature/juno`, with #76 and #60 merged in). Dev client "Kiriko" (`OC_ASSET_PACK`
= the Ana checkout's pack) and `runClient2` "Viewer" over LAN (it needs `OC_ASSET_PACK` too, or it plays placeholders);
fresh worlds `rel1` (flat survival), `relnight` (normal terrain), `fix1` (flat). Health read with `data get`, speeds with
`trace`, sounds with `heard`; screenshots in `run/screenshots/` (r1-r8_*) and `run2/screenshots/` (v_*), each looked at.
- **Every way in:** picked on joining (r1_select_juno: model, "75 health, 150 shields", six kit icons), READY: her HUD
  225 (3 health + 7 shield pips), LShift / E / torpedo panel, 180 | 180, ult ring (r1_juno_hud). From H mid-game (near
  the bed only: "Change hero" greyed 40 m out), to Brigitte and back (r1_hmenu_juno, her perks kept). After dying (fell
  out of the world through a pool I'd dug to the void): back as Juno in hero mode, ult kept (r1_after_death). After
  Save and Leave and `openworld rel1`: the select screen with Juno chosen, "ESC keep hero" (r1_rejoin).
- **Mediblaster:** a villager at 4 health 6 m off +5.76 (72); a tamed wolf (tamed with bones by the use key in Minecraft
  mode: entity reach is 3 m) 10 -> 15.76 (72); a 200-health zombie at midnight -6.71 (90 less armor), its head -10.13
  (x1.5); a skeleton -7.2 (90); R: 180 again within 1 s. (At noon a zombie lost 7.71: it was burning.)
- **Pulsar Torpedoes, mixed:** two zombies, a skeleton and a villager at 4 locked at once (red rings on the enemies, blue
  on the villager: r2_torp_lock2); fired: the zombies -6.69 each 0.3 s on, the skeleton -6.8, the villager 12.16 at 1.1 s
  and 14.80 at the end (135). Again on the fixed build (below): the same numbers (r8_lock3).
- **Movement (trace):** double jump 4.47 m up (r2_doublejump, boot jets); jump held from 25 m: the hover at exactly
  -3.00 m/s; a 35 m fall without hovering: 18 -> 18. Glide: 5.5 -> 18.32 one tick in, 7.70 by 0.5 s, 4 s, its cooldown
  from its end (r2_glide). Up an 8-step stair onto a tower in one glide (r2_glide_slope); into a 7-high wall: up it at
  3.00 m/s while W was held, then held level (r2_glide_wall). Water: her jumps swim (Minecraft's water physics, no double
  jump out of it), and a glide started in water shows its pose but crawls at 1.9 m/s (r2_water_glide). At night: the
  hover and a glide over the `relnight` hills (r4_glided); the Nether below.
- **Perks through the perk menu** (Alt held, a click): Familiar Vitals, a villager 30 m ahead, 0.7 s of lock: 4 -> 12.32;
  Locked On, two zombies locked and hit: the cooldown 6.55 s 1.5 s after firing; Faster Blaster, fire held while gliding:
  180, 167, 156, 145, 133 every 0.5 s (about 22 a second, no gaps); Lift Off, three taps 0.35 s apart: 7.24 m up
  (r2_perk_minor, r2_perk_menu3b, r2_perk_fb, r2_perk_lo).
- **Two players, allies:** the ring thrown with Viewer 26 m ahead: his speed 5.50 -> 8.25 m/s the tick he crossed it,
  held while he ran (his own trace); thrown through him standing, 8.25 at once (r3_ring_hang; v_ring_through, its
  streaks in his view). Orbital Ray on Viewer at 10 (125 of 250): 17.48 at 1.5 s, full at 2.5 s; Kiriko's kunai from
  inside it at a zombie -6.14 (78 = 60 x 1.3, less armor) (r3_ray_cast: blue and gold). He heard her volley_3p,
  projectile, volley_end_3p, double_jump_3p, glide start_3p and loop_3p; she heard his kunai throw_3p and hit and her
  own pain line, and saw his Kiriko throw (r3_viewer_kunai, r3_juno_sees_viewer, v_sees_juno).
- **Enemies** (`overcraft team join blue` / `red`): a volley 20 -> 12.5 (90 and one head round); torpedoes 20 -> 13.2
  (85), her launch flash and torpedo red and orange in his view (v_torp1, v_torp2); her ray red and orange in his view,
  blue and gold in hers (v_ray_enemy2, r3_ray_enemy2), and no heal for him: 12 -> 12.
- **A night on `relnight`** (normal terrain; spawn on a tree top in a forest by the sea), Viewer beside her as Brigitte,
  a scripted fighter (`fighter.py` in the session's scratchpad: the nearest hostile by `data get`, the real fire keys,
  torpedoes every 10 s, the ray when charged, Viewer healed under 13, a glide away under 8): about 150 volleys and 25
  torpedo locks at zombies, skeletons, a creeper and spiders 5-30 m off, the ray once; she died four times (spiders and
  zombies at the spawn while the script idled, and drowned fighting in the sea), Viewer once; every respawn back as Juno
  in hero mode. It shows nothing broke over a night; it doesn't show how well she fights (from the tree top most
  volleys hit leaves).
- **The general play** (`fix1`): every hero from H at a 200-health zombie and a villager at 4 (r6_*): damage Kiriko 9.45,
  Lucio 10.27, Jetpack Cat 7.13, Mercy 0 on her staff and 7.62 on her blaster (2), Lifeweaver 4.46, Ana 5.59, Sombra
  6.29, Brigitte 7.2, Juno 6.71; each healed the villager. Minecraft mode (G): a stone block mined with an iron pickaxe
  (r6_mc_mined), planks crafted in the inventory grid (r6_crafted), a chest placed, logs stored, closed and reopened with
  them inside (r6_chest_reopen), planks built into a pillar (r6_built).
- **One case nobody tried: the Nether.** A lava lake under an obsidian platform: the ray cast onto the lava (its beams
  fall; its ground circle isn't seen, laid on the lake's floor under the lava: r7_ray_lava); off the platform with jump
  held, the double jump and the hover at -3.00 over the lava, a glide into the arena's netherrack wall, climbing it at
  3 m/s (r7_hover_lava); at the glide's end she fell into the lava: "tried to swim in lava", as anyone would.
- **#76 Shield Bash, again** (Brigitte, `fix1`): from a jump 25.85 m/s for 5 ticks with her fall going on, then 14.19
  bleeding in the air, 9.5 m (bashed at the jump's top; the recorded 8.1 started lower); from the ground 7.13 m (the
  record's); into a NoAI zombie 4 m ahead: stopped on it, 70 to it (20 -> 14.4); into a stone wall: stopped at its face.
- **#60 Jetpack Cat, again:** 0 -> 5.5 in about 0.15 s; no keys from 5.5: a straight line to 0 in 0.6 s; the boost from
  5.5: 9.85 at 0.25 s, 16.59 at 0.85, 17.8 by 1.6 s, 2.5 s long; keys held after it: 17.48 ... 15.56, cut to 11.68 at
  0.3 s, 5.5 by 0.9 s; no keys after it: a straight line from 17.8 to 0 in 1.85 s; backing off 5.50 (r5_cat_boost). All
  as the recording's table.

What playing found:
- **A quick click could be lost (fixed).** The torpedo lock's right-button tap through the harness started a lock 5 of 5
  times at 30 fps and 0 of 6 at 10 fps (the window unfocused): at a low frame rate the client runs two ticks in one
  frame and sends the press and the release back to back, both handled before the server's next tick, which never saw
  the button held. A real click shorter than a frame is lost the same way, for every hero's fire buttons.
  `HeroManager.onInput` now keeps a button pressed since the last tick held until that tick has run (`HeroState`
  `primaryFresh` / `primaryLetGo`, cleared by `HeroManager.held` after each hero tick). Played after the fix at 3-9 fps:
  8 of 8 taps locked; 5 of 5 left taps after a lock cancelled it (2 s cooldown) with no volley fired; a Mediblaster tap
  fired one volley (12), a 2 s hold three (36). The trap: [harness](../testing/harness.md).
- **Not fixed (for the owner): Minecraft mode's held item.** Her first person shows her empty right glove and the item
  tiny in the middle of the view (r6_mc_juno_pick); Brigitte, Mercy, Sombra, Lúcio and Lifeweaver show no item at all
  (r6_mc_brigitte_pick, r6_mc_mercy_pick, r6_mc_sombra_pick, r6_mc_lucio_pick, r6_mc_lifeweaver_pick); Kiriko and Ana
  hold theirs (Jetpack Cat uses Minecraft's arm by design). The item goes on `Weapon_L`
  (`HeroAnimator.UpperBody.weapon`: Kiriko's view model is mirrored, her right-of-view hand is `_L`), not the hand on the
  right for these models (Brigitte's flail is on `Weapon_R` in both views). Choosing the hand per model and placing the
  item in it is a pass of its own across six heroes (already so on `main` for all but Juno).
- **Not fixed:** a glide started in water does nothing but its pose; the ray's ground circle lies on the bottom under a
  liquid; the pause menu still reads 0.3.27 (the version bump belongs to the merge).
- **Not ours:** "Failed to save chunk" toasts in `relnight` (Windows' "Insufficient system resources" on E: while
  checking for an oversized-chunk file; no `.mcc` written); a kunai's knockback pushing her 3.4 m (Minecraft's, any hero; gone since #93: hero hits take no Minecraft knockback, [minecraft_26](../minecraft/minecraft_26.md#hero-hits-dont-push-93-2026-11-03)).

Not tried: a second player as Juno; boats, ladders, minecarts; the Hyper Ring on an enemy; her emotes and voice lines
(steps 8 and 8b); a real person's night (the scripted fighter is no player); crafting at a crafting table.
