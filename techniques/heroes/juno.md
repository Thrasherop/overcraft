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
