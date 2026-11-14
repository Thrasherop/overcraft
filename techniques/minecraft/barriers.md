# Barriers in Minecraft

How an Overwatch barrier stops Minecraft's projectiles, blasts and hits. Up: [Techniques](../README.md) · engine:
[minecraft_26](minecraft_26.md) · first built for [sigma](../heroes/sigma.md) (his Experimental Barrier, 2026-11-02), on
Brigitte's state-bound one ([brigitte](../heroes/brigitte.md))

## The shape
- **A barrier that moves on its own is an entity** (`dev.overcraft.hero.Barriers.Shape`: owner, centre, facing, width,
  height, take): Sigma's now; Reinhardt's, Winston's, Orisa's and Ramattra's later. An upright rectangle with a front; the
  mesh's bulge (Sigma's 0.73 m) is ignored (M). Brigitte's barrier stays her state, but her `Barrier.absorb` and
  `blocksBlast` ask `Barriers` first, so both go through `LivingEntityMixin` / `ServerExplosionMixin` once.
- **The entity is not the barrier's box.** An axis-aligned box can't stand for a 6.7 m plane at any yaw: the entity is a
  thin post (no collision, not pickable, not pushable, so creatures walk through it and Minecraft's projectiles never "hit"
  it) and the plane test is our own (`Barriers.crossing`: from the front to behind, inside the rectangle).
- **Only its owner's enemies are stopped** (`Barriers.stops`): his and his allies' shots pass both ways; a dispenser's arrow
  (no owner) is stopped.

## What stops where
- **Projectiles:** each tick the barrier checks every projectile near it: its last step (`xo, yo, zo` -> position) and its
  next one (position -> + motion). An arrow covers 3 m a tick, so checking only the position misses it. Crossing from the
  front: the barrier takes its damage (`Barriers.projectileDamage`: Minecraft's own numbers, e.g. an arrow's
  `ceil(speed x baseDamage)`, read through an accessor: 26.3 has no getter) and the projectile is stopped there. A
  player's arrow or trident drops (picked up again); a ghast's fireball or a wither skull bursts at the front (its blast,
  which the barrier then takes); a hero projectile does its own (`OwProjectile.hitBarrier`: Hyperspheres burst there,
  Disruptor Shot opens its field).
- **Blasts:** `ServerExplosionMixin` calls `Barriers.blast` before anyone is hurt: each enemy barrier with the blast's
  centre in front of it takes what an entity at its nearest point would (Minecraft's rule with full exposure). Behind it,
  nobody is pushed (`blocksBlast`) or hurt (the hit's source position crossing it returns 0 in `absorb`): the barrier took
  that blast once, not once per victim.
- **Hits with no projectile:** only a player's from past melee reach (a hero's hitscan shot); creatures' melee always
  passes (Overwatch's barriers don't stop melee).
- **Kinetic Grasp** uses the same pieces (`takes`, `projectileDamage`, `stop`) on a box before his eyes.

## Traps (found playing)
- **A shader material has no texture in the export** (Sigma's barrier, 7C75): the model renderer skips untextured pieces,
  so it drew nothing. Draw it as added light (`HeroModelRenderer.submitField`); added light ignores alpha, so the colour
  is its strength (at full it hid the world).
- **`kill @e[type=!player]` kills the barrier too:** clear the world with `type=!overcraft:sigma_barrier` while testing it.
- **`equipment:{head:...}` on a summoned skeleton replaces its bow:** it punches (and killed Sigma while screenshots were
  read). Give `mainhand` a bow too; `movement_speed` 0 keeps it still and still shooting. Over 15 m it doesn't shoot.
- **A skeleton on a pillar lobs over a barrier:** its arrow's arc passes above the 4.18 m top. Test from the ground.
- **His kit doesn't tick while he's dead** (HeroManager): anything of his in the world must notice itself (the barrier
  went home only on respawn).

## Every enemy hero projectile stops at Brigitte's too; a dart sleeps no one behind one (#104, #83, 2026-11-04)
- **The cause:** her barrier is her state, so it only asked "what's hitting her or her ally?" at the hurt (`Barrier.absorb`). `SleepDartEntity` then slept the target whatever the hit had done: barrier 300 -> 295 and she was
  asleep behind it. Accretion's knockdown and push had the same shape. A shot at anything not on her side (a zombie behind her) wasn't stopped at all, dart or pistol.
- **The fix, by rule:** `Barrier.tickProjectiles` now does for `OwProjectile`s what `Barriers.intercept` does for an entity barrier: last and next step, either side, she takes `barrierDamage`, it does `hitBarrier`. And
  `covering` and `shields` no longer ask that the victim is hers: an enemy's shot crossing it stops, whatever is behind (`Barriers.absorb` always did). A dart that gets to the hurt anyway (point blank) is removed by the absorb: the dart
  checks `isRemoved()` before the sleep.
- **Bursts: see round 2 below** (f294618 made these burst on a barrier, and they went through it; reverted). A round keeps its +1 energy (`RailgunShotEntity.hitBarrier`), Sigma's too, which never ran (the critique's So4).
- **Played with `bot` (a second player with no client: [harness](../testing/harness.md)), Ana or Sombra on team red, the bot on blue, old vs new:**

| Case | Old | New |
|---|---|---|
| Dart at her behind her barrier | barrier 295, **she's asleep, barrier down** (`rl_old_104a`) | barrier 295, awake, barrier up (`rl_new_104a`) |
| Dart at a zombie past her side, through the barrier | not measured cleanly (she was still asleep from the case before) | barrier 290, zombie 200 -> 200, no one asleep |
| Barrier down (control) | she sleeps | she sleeps |
| Sombra's pistol at her, 2 s (#83) | barrier 300 -> 170, her health 20 | the same |
| The pistol at a zombie behind her | barrier -6.5, **zombie 200 -> 189.8** | barrier -130, zombie 200 |
| Same team | no hit on barrier or her | the same |
| Two rounds at the zombie behind her | not played | barrier -18, energy 0 -> 2, zombie 200 |
| Accretion at her barrier with 22 left | not played | it breaks; the rock's burst (a hit of its own, after) then reaches her 20 -> 17.4 and the zombie -1.1; no knockdown |
| Dart at a zombie behind Sigma's barrier | barrier 645, zombie 200 | the same |

- **#83 needed no fix of its own for its issue's steps** (a fix before this branch: the hitscan rule); this branch only adds the shot at something not on her side.
- **What a critic should check:** a real second client: the bot has no eyes, so what the barrier's owner sees and hears, and whether the dart's stop shows on the shooter's screen, are unverified. A dart stopped at a barrier makes no burst or sound of its own (ours: Local Anesthetic doesn't burst there;
  the data wasn't searched for a barrier contact on it). Blasts (`blocking`) still ask that the victim is hers: a cow behind her takes a creeper's blast. Hack's sight test (the critique's B6) wasn't looked at.

## Round 2, after [critique_rules](../testing/critique_rules.md) (2026-11-05)
**What "played" means here (the critique's 3):** every run is the dev harness: keys through `KeyMapping.set` (`key` in DevServer), aim by `look` or `/execute ... facing`, not a real mouse or keyboard; the bot is a server-side player, no second client. Mobs, terrain, damage and hits are the real game's. Old = b88a2fa, new = the build after the critique; pictures: `overcraft_rules/run/screenshots` (`rl2_new_*`, `old/rl2_old_*`), gitignored.

- **Bursts:** `Barrier.shields(victim, attacker, from)`: all of a burst stops at an enemy barrier between its centre and the victim, damage, status, push (grenade, Suzu, Cyberspace, Local Anesthetic, the Disruptor, the rock, a sphere), and the grenade, Suzu and Cyberspace are dropped at a barrier they meet: the written rule, the wiki's "barriers block the explosion". A rock or a sphere still bursts at the front, and her barrier pays once (`Barrier.burst`). Grenade sent past its edge: lands behind it, and those there take it, status and damage alike (`burst` stills it first, so `Barrier.from` reads where it is).
- **The look-ahead:** a shot about to cross a barrier in its next step is stopped only if nothing comes first (`Barriers.reaches`: a block, or a creature it can hit, both barrier kinds). A shot at her back reaches her; a zombie pressed to the front is hit. All projectiles now, arrows too, so **no distance limit**, and `covering` and `shields` lost their 10 m.

| Case (bot Brigitte, team blue; I'm red) | Old | New |
|---|---|---|
| Grenade square on it, her and a zombie behind | barrier 300 -> 30, **both anti-healed** | 300 -> 210 (its 90), no one |
| Cyberspace square on it | barrier -122 (the zone beyond) | 300, zombie 200 |
| Suzu square on it, a hurt ally behind | ally 4.1 -> 12.4 | 4.1 -> 6.0 (regen only) |
| 12 kunai at her back | her 3, barrier 9 | her 12 |
| 12 rounds at her back | her 3, barrier 9 | her 12 |
| 12 of Lucio's at her back | her 7, barrier 5 | her 12 |
| 12 of Ana's darts at her back | not run (wrong key) | her 12 |
| 10 kunai at a zombie pressed to the front | zombie 2, barrier 8 | zombie 10 |
| Rock at it, two zombies behind | zombie pushed 0.75 m, barrier -135, she 17.6 | no push, barrier -120, she 19.1 (0.9: a still zombie 1.5 m off may have reached her: with no one near, 20 -> 20) |
| A sphere, the same | barrier -49, zombies pulled | barrier -55; `shields` said yes for each (logged); the small pull in the table's first run was a stray slime's and a second sphere's: not re-measured with both gone |
| Pistol at a zombie 1.5 to 19.5 m behind her (critique's 5) | -4.5 at 18 m, barrier 300 | barrier takes every burst (300 -> 170 at 1.5 m, -> 237 at 19.5 m, falloff), zombie 200 at all seven |
| Charged Shot, 19.5 m behind her | not run | barrier -85, zombie 200 |
| An arrow with no owner, the same line | not run | barrier -225 (its 18), zombie 200 |

- **What a critic should check, round 2:** a real second client; a Suzu at an enemy barrier: dropped by the rule for the grenade, no source was found for Suzu itself; a sphere's and rock's own push on someone in front of the barrier.

## The shared check, and the sweep of every hero (#104, #83, 2026-11-13)
Record: [run_2026-11-13](../testing/runs/barrier/run_2026-11-13.md). #104 and #83 held already on next (round 2 above); the sweep found the gaps.

**The shared check, by kind of hit** (all through `LivingEntityMixin` -> `Barrier.absorb` -> `Barriers.absorb` first):
- **A projectile** (an `OwProjectile`, an arrow): stopped in flight by `Barriers.intercept` (entity barriers) and `Barrier.heroProjectiles` (hers); it does its own there (`hitBarrier`). A point-blank one is stopped at the hurt. Opt out with `OwProjectile.passesBarriers()`.
- **A hitscan shot** (`Combat.hitscan`, any source with no direct entity, or the hero as it): judged from the shooter's eyes to the victim at the hurt. Tracer, Cassidy, Sombra, Juno's Mediblaster and torpedoes, D.Va's cannons, Ana's scoped shot, the Charged Shot all go this way.
- **A burst or area** (a hero's own loop over who's near): it must ask `Barrier.shields(victim, attacker, centre)` (both barrier kinds; `Barriers.between` alone misses Brigitte's) for everything it does, status and push too, and charge the barriers once (`Barriers.enemyBarriers` + `Barrier.burst`). Pharah's Concussive Blast and Cassidy's Flashbang asked `between` only: behind Brigitte's barrier the victim was thrown or hindered.
- **Through on purpose:** melee (`Barrier.meleeDepth`), Gravitic Flux, and `Barriers.pierce(() -> hit)`: what is already on someone (Silver Bullet's bleed, Local Anesthetic's numbing: their hitscan sources were judged from the shooter's eyes each tick, so stepping so a barrier was between stopped the damage) and what pierces (Disruptor's field).

**What passes barriers on purpose, and why** (the wiki's Barrier page rev 406358 unless said):
| What | Ours | Source |
|---|---|---|
| D.Va's Boosters | as melee: passes, doesn't damage the barrier (fixed: the barrier took its 25) | "ignore barriers, as if it was a melee attack. It cannot damage barriers" |
| Sojourn's Disruptor field | pierces; each enemy barrier in it takes each step (fixed: a barrier hid everyone behind it). Its shot still stops at one and opens there | Sojourn page rev 409400: "the AoE pierces barriers"; Barrier page: "pierces, and damages barriers" (the barrier's number: ours) |
| Total Mayhem's bombs | fly through (fixed: burst on it); their bursts still stop | "can bounce through barriers, but barriers block their explosions" |
| Concussion Mine, Steel Trap | stop once at it, then pass (fixed: stopped every tick) | Junkrat page rev 410907: "bounces once ... but can then pass/fall through it" |
| Translocator, RIP-Tire (an entity, not a projectile) | through; the tire's blast stops | the Barrier page |
| Juno's Hyper Ring, Orbital Ray; Kiriko's Swift Step, Kitsune Rush; Lifeweaver's Tree of Life, Life Grip, Sow the Seed; Ana's Nano Boost; Sombra's Hotfix, EMP; Mercy's beams | no barrier check (none needed) | the hero pages' `ignores_barrier = 1` / the Barrier page |
| Melee, Rocket Flail, Shield Bash, Gravitic Flux | through | the Barrier page |

**Stopped, as Overwatch:** every hitscan weapon and Deadeye (Cassidy page rev 409375: "Barriers ... can negate/block the shots"), Juno's torpedoes (Juno page rev 408771: "Barriers will block projectiles"), all hero projectiles incl. Healing Blossom (Lifeweaver page rev 410811) and Sleep Dart, Frag Launcher grenades, Pharah's rockets, D.Va's missiles and Light Gun, Pulse Bomb's and Self-Destruct's blasts, Concussive Blast, Flashbang. Allied barriers stop nothing (`Barriers.stops`, `Barrier.stops`; played: a pistol through an allied one hits).

**Left open:** Juno's torpedoes are judged from her eyes, not their curved path; Local Anesthetic's burst (`partial` on the wiki, unexplained) stays blocked; Lúcio's songs and Brigitte's Inspire on allies behind an enemy barrier ("does not affect allies behind enemy barriers") aren't blocked; Call Mech's landing is judged from D.Va's eyes; EMP doesn't destroy barriers.
