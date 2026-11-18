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

### Built (first batch, 2026-11-17, branch feature/enemy-heroes): compiled and unit-tested only, NOT played yet
Nothing below has been seen in a game. The builder could not run the client (another session owned it); the hand play is the
next step and every line here is a thing to check, not a claim.

**What exists**
- `entity/enemy/EnemyHeroEntity` (a `Monster`, so an `Enemy`: every hero ability takes it as an enemy, kills pay ult charge and
  reach the kill feed with the hero's portrait) and `EnemyTracer`, `EnemyCassidy`, `EnemyJunkrat`, `EnemyPharah`, registered as
  `overcraft:enemy_tracer`, `enemy_cassidy`, `enemy_junkrat`, `enemy_pharah` (MobCategory.MONSTER, a 0.6 x 1.8 box).
  **Dev / admin way in:** `/summon overcraft:enemy_tracer` (and the other three); no spawn eggs.
- Fighting: Minecraft's goals (float, wander, look, HurtByTarget ignoring other monsters, nearest player, iron golem, a player's
  pet) and one fight goal shaped like a skeleton's bow goal (walk in past its far range or out of sight, else strafe, backing off
  inside its near range), which then calls the kit.
- Kits, each number from its Stats class:
  - Tracer: 5-shot bursts (10 bullets) over 0.5 s, her own damage, falloff and crit (`Tracer.bulletDamage`), 40-round magazine
    and 1 s reload, her spread 3.5 degrees plus the difficulty's; Blink 7.5 m in 1.5 m steps, 3 charges at 3 s each, to close
    in from past 12 m (a 1-in-10 chance a tick) and sideways half the time she's hit; at least 1 s between Blinks.
  - Cassidy: one Peacekeeper shot a second (`Cassidy.peacekeeperDamage`: 70, falloff from 25 m, x2 head), six then 1.5 s reload,
    out to 60 m; Combat Roll (15 m/s for 0.4 s, 5 s cooldown) to one side when hit from within 6 m on the ground, reloading him.
  - Junkrat: his own `FragEntity`, lobbed on the low arc to the target's feet (`EnemyHeroStats.lob`), one each 0.667 s, five
    then 1.5 s reload; the burst is his numbers (45 direct, 80 splash 0.5-2 m, his push) through `EnemyBlasts.frag`.
  - Pharah: her own `PharahRocketEntity` at the target's feet, 0.8 s apart (0.55 after the last), six then 1.5 s reload;
    `EnemyBlasts.rocket` deals her numbers (40 direct, 80 splash 0.5-2.5 m, her push ring). Hover: in a fight she takes off
    (gravity off), eases to 4.5 m over the ground (rising at most her 4.66 m/s), drifts 3 m/s to keep 8-20 m, comes down after
    5 s at 2.5 m/s and waits her fuel's time on the ground (0.3 s + 100 / 45 a second = 2.5 s) before going up again. No
    fall damage from a landing she flew. Her synced PHARAH_STATE carries LOOK_HOVER for her pose.
- **Target rule (chosen):** `Allies.friendly` has one new branch, taken only when the shooter is an enemy hero: its side is every
  `Enemy` that isn't a player. So its shots and splashes pass through zombies and skeletons and take players, players' pets,
  villagers, golems and bystanders (as a player hero's area hits do). Players' own rules don't change (the branch is never
  reached with a player as the hero). Barriers stop its hitscan and splash (`Barriers.between`, `Barrier.shields`); player
  barriers take its splash.
- **Damage:** Overwatch damage x 0.4 / 0.5 / 0.65 on a player (Easy / Normal / Hard; other creatures take it whole), through
  `Combat.damage` with the blow's hero set. Two new damage types, `overcraft:enemy_shot` (as hitscan) and `overcraft:enemy_blast`
  (as blast, with its tags), both `"scaling": "never"`: the hitscan and blast types scale for a mob shooter, and the factor would
  have been applied twice.
- **Look:** the mob carries `HeroManager.HERO` (synced), so the client draws it with the hero's model and animator like a player
  hero (`EnemyHeroRenderer` calls `HeroModelRenderer.extract`; `LivingEntityRendererMixin` draws the model). Its clips ("fire",
  "reload", "blink", "roll_s1/3") go out through the new `Combat.animate(Mob, clip)`. Without the pack, a humanoid stand-in in
  Minecraft's default skin. Audit of the other `HeroManager.HERO` readers: all read it off a Player (HeroBody, PlayerMixin,
  Stealth, chat, inventory doll, HUD, sounds loops) or the local player (OwMovement.active), so a mob carrying it changes none.
  Name: "TRACER" etc. in red (lang file, `getTypeName`), shown within 24 m.
- **Spawning:** `MobMixin` injects at the tail of `Mob.finalizeSpawn`; `EnemyHeroSpawns.finalized` marks a NATURAL zombie,
  skeleton or husk (exact types) in the Overworld while `isDarkOutside()` and not Peaceful that won a 1-in-12 roll; on the
  server's next tick it's swapped (the hero made at its place and facing, finalized as NATURAL, the mob discarded), unless it's
  a jockey, the box doesn't fit, or a player within 64 m of it already has 2 enemy heroes within 64 m.
- Drops: 5 XP (a skeleton's) and 1-3 of redstone / iron nuggets / gunpowder / firework rockets.
- Tests: `EnemyHeroStatsTest` (difficulty factors, pause range, cap rule, the speed attribute against a zombie's, the lob
  landing on its target).

**Ours (no source gives them; open for the owner):** all of EnemyHeroStats (spawn chance, cap, damage factors, extra spread,
pauses, ranges kept, Tracer's halved fire rate and Blink habits, Cassidy's 1 s shot and roll trigger, Pharah's height, hover time,
drift and descent), the 60 m Cassidy reach, the muzzle offsets of Cassidy and Junkrat, Pharah's self-splash left out, the
pet-targeting goal, the 24 m name range, the stand-in skin. The run speeds are Overwatch's (5.5, Tracer 6.0) turned into
Minecraft's speed attribute by our measure of Minecraft's movement (44.05 x speed² m/s).

**Not done / could not do**
- No hand play at all (see the top). First things to check in game: they appear (summon each) and are drawn as their heroes
  with the pack, and as the stand-in without; the name tag shows; each fires at a player and a tamed wolf but not at a zombie;
  damage per hit matches (e.g. a Cassidy body shot at 10 m on Normal: 35 = 2.8 health); Junkrat's frags land at the feet
  rather than short or long; Pharah actually rises, holds 3-6 m, drifts, comes down and lands without damage (her flight is
  set by velocity each tick on a ground-navigating mob: the riskiest part); Tracer's Blink doesn't put her in a wall; Cassidy's
  roll moves him; the kill feed line when one kills a player and when a player kills one; natural night spawns (raise the
  odds with a debugger or many nights) and the cap.
- Pharah's jet flames and hover sound don't show: PharahFx and PharahSounds loop over players only.
- No Recall, Pulse Bomb, Flashbang, Deadeye, mines, trap, tire, Concussive Blast or Barrage (the plan's cut-down kits).
- Their hitscan isn't stopped by a Sigma / Brigitte shield in front of the target except through `Barriers.between`; whether
  that covers every barrier kind is untested.
- No Overwatch side-by-side or critique yet (the QA method's next steps).

## Played, 2026-11-17 (Normal, Torbjorn, real keys where marked)
- **Cassidy:** drawn with his model and a red name; shot from 12 m about once a second, 2.24 health a hit on Torbjorn (35 less his
  armour); killed him in ~10 shots. Death message "was shot" named nobody: fixed (enemy_shot's own message).
- **Tracer:** Blinked in to 5 m at my side; bursts, ~1.1 health a second at range, ~5.3 up close.
- **Junkrat:** frags 3.9-5 a hit at ~1.5 a second; knocked Torbjorn off the arena edge: "was doomed to fall by JUNKRAT".
- **Pharah** (after the retune): rose 4.5 m and hovered, rockets 2.9 a hit about every 0.8 s, came down as her hover ended.
- **Damage retuned** from 0.4 / 0.5 / 0.65 to 0.2 / 0.3 / 0.4 on those numbers (a 20-health player died in ~3 s to Junkrat).
- **Names through walls:** reproduced (JUNKRAT behind stone), fixed, replayed (hidden).
- **Natural spawns:** 90 s of midnight on Normal: 29 zombies, 19 skeletons, 16 creepers, 6 spiders, and 2 enemy heroes (Cassidy,
  Junkrat): the cap of 2 near a player held.
- **Not reproduced:** Tracer's Blink through a pane wall (she walked; fixed by reading). **Not tried:** a second player, a save and
  reload with Pharah mid-hover (fixed by reading), Pharah's jet flames (PharahFx loops players only: none on her), kill feed portraits.
