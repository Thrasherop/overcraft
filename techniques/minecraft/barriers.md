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
- **What burst on contact now bursts on a barrier** (Biotic Grenade, Protection Suzu, Cyberspace: ours). They were dropped silently, at Sigma's since he was added. A round keeps its +1 energy (`RailgunShotEntity.hitBarrier`), Sigma's too, which never ran (the critique's So4).
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
  the data wasn't searched for a barrier contact on it). Grenade and Suzu bursts on a barrier: none played. Blasts (`blocking`) still ask that the victim is hers: a cow behind her takes a creeper's blast. Rock on a barrier with 22 left: should its burst stop as the hit that broke it does? Hack's sight test (the critique's B6) wasn't looked at.
