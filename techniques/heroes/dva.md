# D.Va (hub)

Everything learned about D.Va, by subject. Hero 8 of the owner's 2026-10-28 list (issue #69), after Sigma: a **Tank, Initiator** (BZ
2026-02-10), **the second Tank** and **the first hero with two bodies**. Up: [Techniques](../README.md) · the method:
[reading_game_data](../game-data/reading_game_data.md) · researched the way Sigma's was (commit 1fb1dc2, branch feature/sigma:
[sigma](sigma.md), whose Tank role code she builds on) and Sojourn's ([sojourn](sojourn.md))

## Her notes and sources
- **Numbers:** [dva.md](../../tools/reference/dva.md): Blizzard's patch notes to 2026-11 and the wiki's revision 408749, each number with its
  game-data value. Her last change is BZ 2026-10-06 (missiles 11 -> 14 a second), the data build's own day, and the data has 14: the
  data's numbers are the live ones.
- **The data map:** [dva-map.md](../../tools/reference/dva-map.md): both bodies' kits and movers node by node (H / M / L); the detail and
  the pipeline in `E:\Overwatch Archive\research\dva\` (`work_kit\kit_dva.md` the pipeline, `kit_dva_mech.md` / `kit_dva_pilot.md` the
  readings, `work_kit\c_<g>.txt`, `work\clipmap_dva\`, `extract\`).
- **The plan:** [dva-plan.md](../../tools/reference/dva-plan.md): where the sources disagree, **what she needs that the mod lacks** (two bodies
  as one player, per-form health and ult meters, the mech's death as an eject, the Initiator sub-role, armor and shields together, a
  projectile-eating box, a hitscan shotgun, flight along the aim, the mech's box), what maps onto Sigma's Tank code, the order.
- **Clip maps:** `tools/clipmaps/dva.txt` (MEKA 3P, FBB0, 139 clips), `dva_1p.txt` (MEKA 1P, 10354, 75), `dva_pilot.txt` (pilot 3P, F2FF,
  100), `dva_pilot_1p.txt` (pilot 1P, F32B, 52), `dva_weapon.txt` (Light Gun, 10246, 11). Written by
  `research\dva\scripts\clipmaps_dva.py` (Sigma's writer: each clip named by the alias holding it), which also lists the clips a set names
  on the other body's model.

## What her data taught (2026-11-02)
- **A hero can be two entities.** The hero entity (040A) is the pilot; the MEKA is a **pet** (0407, STUPetComponent) with its own mover,
  health, graphs and weapon manager, spawned by Call Mech (026A #94). While linked, control and camera pass to the pet and the pilot rides
  attached, hidden and untargetable (026A #98). Crawl from the hero file and **every** entity it spawns (the pet's graphs aren't on the hero
  entity's list), and give each body its own values graph (33D9 the mech's, 348C the pilot's: `varvalues.py` now takes G from the
  environment).
- **The value table's names lie; the readers don't.** 1185 "11" is the Fusion pellet count, not a missile rate; 0404 "Micro Missile
  Cooldown" 14 is the missile fire rate (the 2026-10-06 patch); 03BE "Self-Destruct Damage Min" 250 is Call Mech's landing; 1B57 / 1B58
  "Boosters Duration" are the wreck's 1.0 s and 0.75 s; 0C6A "Self-Destruct Outer Radius" 3 is the fuse; 12C3 / 12C4 are perk XP. Name a
  value by the node that reads it (the reading_game_data rule, proven again).
- **StateModifyHealth's x60671788 mode is not always "percent".** Self-Destruct's 1000 is that mode as a flat amount (0B2F #280); Sigma's
  barrier regeneration read it as a share of max. Check the amount's scale (a 9999 heal, a 1000 blast) before calling it a share (M).
- **The two readings disagreed once, and the one that read the graph whole was right:** the mech pass found "no launch" for the eject; the
  pilot pass, reading 026A from its start, found #97 (a 9999 heal with 6 / 7 m/s knockback from a point ahead of the mech): heals carry
  knockback too. Split a hero's graphs between readers by body, then let the one that owns a transition settle it.
- **The mech is the hero default mover but its size** (2.3 m tall, 0.7 m radius); its 1P eye field is 1.74 m standing and crouched (the
  same number: no crouch). Sigma's 1P field was 2.1 against his head hardpoint's 2.06, so the field is a fair stand-in for the eye (M).

## Not yet
All of it: the plan's 13 steps. Effects (111 extracted), sounds, voice (set 57.05F, 1129 subtitled lines, both bodies the same set) and her
15 emotes are extracted but not mapped.
