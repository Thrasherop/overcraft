# General play before main, 2026-11-18 (0.3.117 -> 0.3.119), at a player's 2 GB heap

A fresh client from `next` (2565c59) with `OC_XMX=2G`, a new survival world (`GenPlay119`, Normal), the full local pack. The owner
was off the PC: the select screen and the shots used the real mouse; set-ups used the harness. Changed since
[genplay_0116](../genplay_0116/run.md), each played on its own first: enemy heroes leave at daybreak (0.3.117); the
`overcraft:enemy_hero_chance` game rule, Sombra's hack shutting down turrets (0.3.118); a hacked enemy hero uses no abilities
(0.3.119). Up: [testing](../../README.md).

- **On join:** the select screen; Sombra picked with two real clicks (portrait, Ready): hero mode.
- **Turret hack (0.3.118):** a kept enemy Torbjörn's turret fired 14 shots in 4 s at Sombra; after Hotfix's hack on it, 0 in
  the next 4 s.
- **Game rule (0.3.118):** `overcraft:enemy_hero_chance` read its default, 8.
- **Daybreak (0.3.117):** an enemy Cassidy summoned at midnight was gone 3 s after `time set noon`.
- **A real shot:** Cassidy, two real left clicks at a still husk 6 m off: 20 -> 8.98, as before.
- **Death:** three husks killed Kiriko in 6 s. **Respawn:** Kiriko, 20 health, hero mode.
- **Rejoin:** left at 11 health through the menu's Save and Leave, back with `openworld`: 11, Kiriko, hero mode; the join's
  select screen offered.
- **Not tried this time:** the hacked enemy's abilities (played on its own for 0.3.119: no turret thrown under a 10 s hack),
  the slim asset pack (dev only, for the owner's playtest), Minecraft mode, F3+T.
