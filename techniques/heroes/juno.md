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
  pair over her shoulders (x +-0.28, y +0.55) and rise apart on the fire. Her layers use that (`Skeleton.addAdditive`
  localOffsets); whether every additive clip wants it is #75 (the export's additive tracks mix conventions).
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
