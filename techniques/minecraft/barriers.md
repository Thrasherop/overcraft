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
