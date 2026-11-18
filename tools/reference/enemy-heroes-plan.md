# Enemy heroes at night (#46): plan

Up: [techniques map](../../techniques/README.md). The owner's request (2026-10-27): at night, some hostile spawns are enemy heroes.
The owner's decision (2026-11-14, on #46): after the heroes and everything else; real mobs per hero with cut-down kits, Minecraft's
own pathing and targeting; start with 3-4 simple heroes (Tracer, Junkrat, Cassidy, Pharah), damage scaled down. Fake players were
considered and rejected (player list, sleep count, no pathing, fragile in multiplayer).

## What they are
- One `Monster` per hero (`EnemyHeroEntity` base, a subclass per hero), registered like the mod's other entities. Hostile to every
  player and to players' pets and allies (Allies: a `Monster` is an enemy already), hit by every hero ability, killed for ult charge
  and the kill feed like any mob.
- Drawn with the hero's own model from the player's asset pack (the same `hero_models/<hero>.glb` the player heroes use), with its
  locomotion and fire clips; without the pack, a plain stand-in (as every hero model falls back).
- Their names over their heads in red: "TRACER" etc. (ours), so a player can tell them from mobs at a glance.

## Defaults (ours until the owner says otherwise; the issue's open questions)
| Question | Default | Why |
|---|---|---|
| How often | 1 in 12 natural night spawns of a zombie, skeleton or husk in the Overworld becomes an enemy hero instead (8 %), never in Peaceful; never more than 2 enemy heroes within 64 m of a player | rare enough to be an event, frequent enough to meet one a night |
| Which | Tracer, Junkrat, Cassidy, Pharah, equally likely | the owner's first four |
| Health | their Overwatch health at the mod's scale (250 = 20): Tracer 14, Junkrat 20, Cassidy 20, Pharah 18 | the same scale as the heroes |
| Damage to a player | their Overwatch damage x 0.4 on Easy, x 0.5 on Normal, x 0.65 on Hard (ours) | "damage scaled down": a Minecraft player has 250 at the mod's scale and no healer |
| Aim | a skeleton's: they lead nothing, spread a few degrees more on Easy, and wait 0.5-1 s between bursts (ours) | beatable without hero mode |
| Drops | XP as a skeleton (5) plus 1-3 of a themed item (Tracer: redstone; Junkrat: gunpowder; Cassidy: iron nuggets; Pharah: firework rockets) (ours) | worth hunting, nothing rare |
| Healed by enemy supports | not in this batch (no enemy supports yet) | |

## Each hero's cut-down kit (numbers from their kit classes, already sourced there)
- **Tracer** (TracerStats): Pulse Pistols, 5.75 a bullet, bursts of 10 bullets (0.5 s) then a pause; Blink (7.5 m, 3 charges, 3 s
  each) to close in or dodge when hit. No Recall, no Pulse Bomb.
- **Cassidy** (CassidyStats): Peacekeeper 70 a shot, one shot every 1 s at range; Combat Roll (6 m) when hit at close range. No
  Flashbang, no Deadeye.
- **Junkrat** (JunkratStats): Frag Launcher (45 direct, 80 splash in 2 m), lobbed at the target's feet, 5 then a 1.5 s reload. No
  mines, trap or tire.
- **Pharah** (PharahStats): Rocket Launcher (40 direct, 80 splash in 2.5 m), hovers 3-6 m up (Hover Jets) while fighting, lands to
  refuel. No Concussive Blast, no Barrage.
Each uses the hero's own projectile entity or hitscan code where it accepts a non-player shooter, and its sounds and effects.

## Built and played (filled in as it goes)
