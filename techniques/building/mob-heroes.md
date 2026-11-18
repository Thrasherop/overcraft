# A mob as a hero (enemy heroes, #46)

Up: [techniques map](../README.md) · [building](building.md). Plan and play record:
[enemy-heroes-plan](../../tools/reference/enemy-heroes-plan.md). Learned building and playing the first batch (Tracer, Cassidy,
Junkrat, Pharah), 2026-11-17.

## What a mob needs to be drawn as a hero
- **Any `LivingEntityRenderer` that calls `HeroModelRenderer.extract`** in its `extractRenderState` gets the full hero: the
  submit mixin (`LivingEntityRendererMixin`) cancels vanilla's draw when the state carries the hero's `DRAW`, and `HeroAnimator`
  takes locomotion from position deltas, `onGround` and `yHeadRot`, for any entity.
- **Which hero:** the synced `HeroManager.HERO` attachment on the mob (a held `HeroWeaponItem` also works, but only Kiriko has
  one). Before reusing HERO, audit its readers: every server-side one read it off a `Player` (or the local player), so a mob
  changed none of them. Re-check whenever a new reader is added.
- **One-shot clips** (`fire`, `reload`): `HeroActionPayload(entityId, clip)` to `PlayerLookup.tracking(mob)`
  (`Combat.animate(Mob, ...)`). Effects that loop "over players" (PharahFx's hover jets did) miss the mob: loop over living
  entities carrying the state instead.

## Team rules: the trap
- Implementing `Enemy` (a `Monster`) makes it an enemy of every player and a target of every hero ability.
- **But `Allies.playersAllied` is true when both teams are null**, so `Allies.canHurt / areaHits / isAlly(mob, player)` called
  with the *mob as the shooter* treat every team-less player as its ally: shots and splash pass through players. Batch 1 added one
  branch in `Allies.friendly` for an enemy-hero shooter (friendly = other non-player `Enemy`s only). Player rules unchanged.
- Kits' projectiles that only act for a `ServerPlayer` owner (FragEntity's bounce and burst, PharahRocketEntity's burst) do
  nothing for a mob owner: give them a mob branch (`EnemyBlasts`), after the player one.

## Damage
- A new damage type per kind (`enemy_shot`, `enemy_blast`) with `"scaling": "never"`: the mod's hitscan and blast types scale with
  difficulty when the attacker is a mob, which would apply the difficulty twice on top of the mod's own factor.
- Its `message_id` needs its own death message with `%2$s`: with an attacker, vanilla uses the base key with the attacker as the
  second argument ("was shot" named nobody until `death.attack.overcraft.enemy_shot` got one).
- Overwatch damage at the mod's 250 = 20 scale kills a plain player in seconds: at x 0.5 Junkrat did ~7.5 health a second. Measure
  in play (health read every 0.5 s) and tune against vanilla mobs (a skeleton's 2-3), not against the hero's numbers.

## Spawning
- No natural-spawn hook existed. `MobMixin` at the tail of `Mob.finalizeSpawn` (reason NATURAL, exact type), marking the mob and
  swapping it for the hero on the next server tick.
- **A cap must cover the whole spawn ring:** natural monsters spawn 24-128 m from a player, so "2 within 64 m of the spawn point"
  missed most spawns; count the heroes near every player within 128 m.

## Look and feel
- Minecraft draws a shown name through walls: an always-on name over a hostile was a wallhack. Show it only in line of sight.
- Movement abilities on a mob (Blink, rolls) must sweep their path; a 1.5 m step checked only at its end jumped doors and panes.
- Anything saved by vanilla (NoGravity) whose matching state isn't (Pharah's flight) breaks on reload: reset it on load.
