# General play before main, 2026-11-18 (0.3.114 -> 0.3.116), at a player's 2 GB heap

A fresh client from `next` (be2462f) with `OC_XMX=2G`, a new survival world (`GenPlay116`, Normal), the full local pack. The owner
was off the PC: the select screen and every shot used the real mouse; set-ups (hero changes, aiming, mobs) used the harness.
Changed since [genplay_0113](../genplay_0113/run.md), all for the enemy heroes at night (#46), each reproduced and replayed on
its own first: gone on Peaceful, their hitscan charges a player's barrier (0.3.114); no Blink or roll while held or hacked
(0.3.115); a dead one's frags and rockets still burst (0.3.116, in FragEntity and PharahRocketEntity, which the player's
Junkrat and Pharah use too). Up: [testing](../../README.md).

- **On join:** the select screen; Junkrat picked with two real clicks (portrait, Ready): hero mode.
- **The player's own projectiles (the changed entities):** Junkrat's frag, one real click at a still husk 6 m off: 20 ->
  10.16. Pharah's rocket, one real click: 20 -> 10.55. As before.
- **A night fight:** midnight, an enemy Tracer penned 8 m off, Cassidy with real clicks. The first four rounds hit the pen's
  own fence (aimed at her middle, under the fence top: my set-up); aimed at her head, three more killed her: the kill feed
  showed it and her drops lay by the pen.
- **Peaceful (0.3.114):** with 2 enemy heroes (natural spawns) and 18 zombies loaded, Peaceful cleared both: 0 and 0 four
  seconds later.
- **Death:** three husks killed Kiriko in 5 s. **Respawn:** Kiriko, 20 health, hero mode.
- **Rejoin:** left at 11 health (the menu's Save and Leave), back with `openworld`: 11, Kiriko, hero mode; the join's select
  screen offered.
- **Not tried this time:** Minecraft mode (untouched since genplay_0113, which played it with real input), a player's barrier
  against an enemy (played on its own for 0.3.114), the other heroes' sweep, F3+T.
