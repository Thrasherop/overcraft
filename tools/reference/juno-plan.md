# Juno: the plan

Numbers: [juno.md](juno.md) (BZ, W per number) and the game data map [juno-map.md](juno-map.md) (D, with its detail in
`E:\Overwatch Archive\research\juno\work_kit\kit_juno.md`) -> `JunoStats`, each number with its source. Branch `feature/juno` in
its own worktree (issue #1, hero 5 of 12 after Brigitte). Written 2026-11-01 from build 154088 (2026-10-06).

## Her role and the patches
- **Support, Survivor** (D 01C7; BZ): Survivor's passive starts the global regeneration on a movement ability: hers is **Glide
  Boost only** (D: message 26FD; the double jump and hover don't send it).
- **No kit change since the data build** (BZ 2026-10-06 was hit volumes only; 08-11 the torpedo lock 0.35 s, which the data has),
  so the data's numbers are the live ones. Where the data and the public text disagree (below), the data's graphs decide, played.

## Where the data and the public sources disagree
- **Glide Boost's dash:** D a 14 m/s horizontal burst added at the start (47B7 #589), then air max 9 and +40 % move speed; W
  19.1 m/s (unsourced edit; was 40 m/s). D.
- **Double jump:** D 1.8 x the jump (x1.8 on 4441 #570: about 10.3 m/s from 5.72, M); W 10.8 m/s (1.8 x 6.0: the mover's field
  6.2 vs the measured 5.72, `movement.md`). Ours 1.8 x our jump (10.3 m/s); a Workshop recording settles it (Open).
- **Hyper Ring radius:** D 4.5 m; W 4 m. D. Its "-1 s per ally through it" (522E #431) is ungated in the data and in no patch note:
  left out until a source shows it live.
- **Mediblaster falloff:** W full to 25 m, 30 % at 35 m; D has the values but no node reads them. D (no falloff), noted.
- **Torpedo targets:** no cap in D (M), none in any source: no cap.

## What maps onto what Overcraft has
- **Hero entry:** `Hero.JUNO`, support / survivor / 75 health + 150 shields (`Shields`, Lifeweaver's pool: 30/s after 3 s);
  `HeroKit` `dev.overcraft.juno.Juno`; perks XP 2110 / 5275; four `Perk` entries. One body model for both views (0159.118, looks
  0939 / 093A): the Mediblaster and the torpedo pod are geosets of her body (no weapon model).
- **Movement:** the stock mover (`OwMovement`'s defaults: nothing new on the ground). **Martian Overboots** in the mover: a second
  jump in the air from 0.1 s after leaving the ground (vertical speed reset, 1.8 x), once (twice with Lift Off), refilled on
  landing; **hover**: JUMP held in the air caps the fall at 3 m/s once 2.5 m of air is below her (Mercy's Angelic Descent cap,
  `OwMovement.descent`). **No fall damage** (W; D has no fall-damage node): falls never hurt her (CLAUDE.md: an ability lands her).
- **Glide Boost:** a mover state like Mercy's Guardian Angel (`OwMovement.glide`): the 14 m/s burst along her input, then 4 s
  level with gravity off (0 for 0.7 s and without input; an altitude hold that steps up over a block and follows drops), air
  accel 12, air max 9, +40 % speed, -50 % knockback; the double jump lifts her mid-glide; cancel with Shift after 0.4 s; 6 s
  cooldown from its end. Survivor's regeneration at its start.
- **Mediblaster:** projectiles (Ana's / Sombra's `OwProjectile`) in 12-round volleys at 34/s (two a tick and change: a fraction
  carried), a volley every 0.8 s, 7.5 to enemies (0.02 m) and 6 to allies (0.22 m: easier to hit); 180 rounds, reload 1.5 s.
- **Pulsar Torpedoes:** a lock-on (right button, toggle): every hero, player or creature (ours: any living ally or enemy, see
  below) in sight within 40 (+5) m near her crosshair gathers a lock in 0.35-1.0 s by distance, within 4 s; the left button (or
  the right again) fires one homing torpedo per target 0.08 s apart: allies 85 + 50 over 2.5 s, enemies 85. 10 s cooldown, 2 s with
  nothing locked.
- **Hyper Ring:** an entity flying 35 m/s for 0.5 s then hanging 6 s, a 4.5 m ring; each ally through it +50 % speed 3 s.
- **Orbital Ray:** an entity starting 4 m ahead, moving along her facing at up to 2.25 m/s on the ground for 8 s, 8 m radius:
  allies inside 85 HP/s and +30 % damage (`Boosts`, Mercy's damage boost route).

## Minecraft decisions (ours; defaults in place, the owner may change them: "Open")
- **What a "hero" is for the torpedoes:** players and every creature (ally: villagers, golems, tamed pets; enemy: hostile mobs).
- **The double jump on the jump key** in the air (Minecraft's own double-tap flight is creative-only and unchanged).
- **Burst fire faster than 20 ticks:** the 34/s volley lands as 1-2 projectiles a tick on its exact schedule.
- **Orbital Ray's column:** 8 m round, any height.

## Order (each step played by hand before the next; CLAUDE.md's hand play rules)
1. Hero entry, model, clip maps, select screen and HUD shell: pick her, look at her from every side in both views.
2. Movement: the double jump, the hover, no fall damage, Glide Boost on flat ground, over steps and drops, off cliffs, into
   walls; the jumps and glide combined; Survivor.
3. Mediblaster on zombies and a villager; reload.
4. Pulsar Torpedoes: locks on several mobs and allies, the 2 s and 10 s cooldowns.
5. Hyper Ring and Orbital Ray with allies.
6. Perks (Familiar Vitals, Locked On, Faster Blaster, Lift Off).
7. Sounds, voice and effects from her data. 8. Emotes from the data. 9. Two players, a night, the general play; release.
