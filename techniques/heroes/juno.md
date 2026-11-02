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
