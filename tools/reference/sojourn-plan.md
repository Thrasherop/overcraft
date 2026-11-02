# Sojourn: the plan

Numbers: [sojourn.md](sojourn.md) (BZ, W and D per number) and the game data map [sojourn-map.md](sojourn-map.md) (D, with its detail
in `E:\Overwatch Archive\research\sojourn\work_kit\kit_sojourn.md`) -> `SojournStats`, each number with its source. Branch
`feature/sojourn` in its own worktree (issue #2, after Juno #1). Written 2026-11-01 from build 154088 (2026-10-06).

## Her role and the patches
- **Damage, Sharpshooter** (D 0DDD / 01C7; BZ 2026-02-10): **the first Damage hero in Overcraft** (`Hero.Role.DAMAGE` exists, no
  playable hero uses it yet; sub-role key `overcraft.subrole.sharpshooter` is new). Sharpshooter's passive: **critical damage to a
  hero takes 0.0075 s per point off Power Slide's cooldown** (D: ABILITY 1 only for her). The old Damage role passive is gone; its
  healing reduction is global (whatever Overcraft does for every hero).
- **No kit change since the data build** (BZ 2026-10-06 was hit volumes and the global hitscan sizes, which the data has; the last kit
  change, Friction Generators 75, is 2026-06-16), so the data's numbers are the live ones. Where the data and the public text
  disagree (below), the data's graphs decide, played.

## Where the data and the public sources disagree
- **Slide jump forward speed:** D adds **6.5 m/s along the slide** to her velocity (1E6F #411, M on "adds") on top of a 17 m/s rise; W
  gives a fixed +273 % (20.5 m/s) forward, which is that sum at full slide speed (14.3 + 6.5 = 20.8). Ours: D (add 6.5 to the slide's
  velocity at the moment of the jump), so a jump early in the ramp goes less far, as in the data. A Workshop recording settles it
  (Open).
- **Power Slide in the air:** D 1CFD 1.2 under the air override, M additive (1.6 + 1.2 = +280 %, W's number). Ours +280 %; Open
  until recorded.
- **Slide jump height:** no public number (W's 17 m/s only). D's gravity x1.4 for 0.25 s gives about 6.9 m (L, our arithmetic); without
  the 1.4, 8.3 m. Ours: D's (17 m/s, 1.4 x gravity for 5 ticks).
- **Railgun spread:** W 1.6 vertical / 0.64 horizontal; D only the 1.6 maximum and its 12-shot ramp. Ours: W's split (2.5:1) on D's
  ramp, noted M.
- **Sharpshooter:** W's infobox "0.75 % of base cooldown" vs BZ / W's table / D "0.0075 s per crit damage point". D.
- **Settled by D, no choice left:** Railgun 150 m/s and no falloff; Disruptor 3.5 s / 4 m / 90/s / 40 m/s for 0.75 s; ult 2300; Charged
  Shot 0.11 m; piercing base kit.

## What maps onto what Overcraft has
- **Hero entry:** `Hero.SOJOURN("sojourn", "Sojourn", Role.DAMAGE, "sharpshooter", SojournStats.HEALTH, ...)`, 225 health (18 MC
  health), no shields or armor; `HeroKit` `dev.overcraft.sojourn.Sojourn` (built like `brigitte/Brigitte.java` and `juno/`), `SojournStats`
  (each number with D / BZ / W), `SojournSounds` later; perks XP 1695 / 4235; four `Perk` entries (`SOJOURN_OVERCHARGED`,
  `SOJOURN_DECELERATION_FIELD`, `SOJOURN_DUAL_THRUSTERS`, `SOJOURN_FRICTION_GENERATORS`). **Three models**: 3P DCA7 (set 08C2), 1P arms
  DCA8 (08C1), and **the Railgun D7B4 as a weapon model** (set 0AB6) on her carrier bone_0606 (hp 00C9): the held-weapon route Brigitte's
  flail and Lúcio's / Mercy's weapons use, with the gun's own clip map `sojourn_weapon.txt` played on top of hers.
- **Movement:** the stock mover (`OwMovement`'s defaults: nothing new on foot, no passive). **Power Slide** is a new mover state in
  `OwMovement`, a hook like `dash` / `glide` (`slide`: the slide's state this tick or null), set by the client from the kit's sync:
  - 20 ticks (15 with Dual Thrusters), the speed bonus **1.6 x ramp** on the default mover (ramp 0.4 -> 1 over 4 ticks, down to 0.3 over
    the last 6), +1.2 more in the air; along her move input (forward with none), not counted against the +75 % cap; **fall capped at
    2 m/s** (Mercy's `descent` cap, 0.1 block/tick); a low pose (Minecraft's crouching box, 1.25 m as her crouch mover: ours); ends
    early against a wall (0.3 s, D) or on the jump.
  - **Slide jump on the jump key** while sliding with ground within 2 m (or 3 ticks after leaving it): vertical speed set to 17 m/s
    (0.85 blocks/tick), +6.5 m/s along the slide (Dual Thrusters 13.75 / 11), `gravityScale` 1.4 for 5 ticks, then the slide ends.
  - **No fall damage when the slide or its jump lands her** (CLAUDE.md, #35); plain falls from the default 3 m safe height.
  - Cooldown 7 s from the slide's end (two charges with Dual Thrusters, each 7 s); Sharpshooter's crit refund on top.
- **Railgun:** projectiles (`OwProjectile`, as Kiriko's kunai / Lúcio's sonic: `KunaiEntity` is the closest, fast, no drop, no
  falloff), **16 a second = 0.8 a tick** (an accumulator: four shots every five ticks on the exact schedule), 9 damage, x2 on a crit
  (the existing head band, `HEAD_DEPTH`), **150 m/s = 7.5 blocks a tick**, 0.175 m: swept collision along the step, as the kunai's.
  Spread 0 -> 1.6 degrees over 12 shots (0.64 sideways), back after 0.3 s. 45 rounds, reload 1.2 s (full at 0.96 s). **Energy** in the
  HUD's `meter` (0-100, 150 with Overcharged): +5 a creature hit, +10 a crit, +1 an object (ours: armor stands, end crystals, boats,
  minecarts; +2 crit), +1 a barrier (Brigitte's / Lifeweaver's); decay 0.75 a tick after 180 ticks without a gain.
- **Charged Shot (right button):** a **piercing hitscan ray** (Ana's scoped `hitscan`, extended to go through every enemy on the line
  until a block), 20 + energy damage, x1.5 on the head band, falloff 40-60 m to 50 %, 0.11 m; spends all energy, needs > 0; then 13
  ticks (0.65 s) before either fire. Tracer effect only.
- **Disruptor Shot (ability 2):** a 0.17 s cast, then a projectile from her left wrist at 40 m/s (2 blocks a tick) that opens a field on
  contact or after 15 ticks (30 m): **an area entity** (like Lifeweaver's / Mercy's placed effects), opening 2 ticks later, 70 ticks
  (3.5 s), radius 4 m, **90/s ignoring armor** (Minecraft's bypasses-armor damage tag), as an accumulator on the 0.192 s ticks or per
  tick (4.5 a tick). Deceleration Field: -25 % move speed inside (`Mover.slowed` / an attribute, gone on leaving). Cooldown 15 s from
  0.5 s after the launch.
- **Overclock (ultimate):** 0.75 s cast (15 ticks, no fire), then 8 s (160 ticks), then a 0.45 s tail: energy +5 a tick to the cap
  throughout, no on-hit gain, no decay. Overcharged: cap 150, draining to 100 at 1 a tick after. HUD: the meter filling; `ultActive`.
- **Sharpshooter:** in the kit's damage hook: on a crit against a living target (ours: any creature, as "hero"), Power Slide's cooldown
  -= damage x 0.0075 s.
- **Clips:** her locomotion is stock; the slide is start (0x5E4) -> body (0x72D, eight direction categories) -> slide jump (0x5F3), and
  reload / melee / Disruptor / the Overclock cast switch to their slide copies while sliding (df 0xE5 cat 0x24A). The fire clips play
  on the weapon layer (first shot 6F09, repeats 6F08; charged 6F0A); the 1P energy additive 7D6C weighted by energy / 100.

## Minecraft decisions (ours; defaults in place, the owner may change them: "Open")
- **What counts as a "hero" for energy and Sharpshooter:** every living creature and player (+5 / +10); "objects" (+1): non-living
  entities that take damage (armor stands, end crystals, boats, minecarts); barriers +1.
- **Crits:** the existing head band (`at.y >= eyeY - HEAD_DEPTH`, and an ender dragon's head part), as Kiriko's kunai.
- **The slide's hit box and step:** crouch height (1.25 m mover crouch), Minecraft's 0.6 block step; slopes climbed as on foot.
- **Charged Shot through blocks:** no (Overwatch's hitscan stops at terrain); through every creature on the line: yes.
- **Disruptor against blocks:** the field opens at the hit block's face; it hurts nothing of hers (no self-damage).
- **Keys:** Power Slide on ability 1, Disruptor on ability 2, the slide jump on the jump key, as Overwatch's defaults; no options to add
  (her data has none but aim sensitivity).

## Order (each step played by hand before the next; CLAUDE.md's hand play rules)
1. Hero entry (Damage, Sharpshooter), the three models, clip maps, select screen and HUD shell (the energy meter): pick her, look at her
   and the gun from every side in both views.
2. Power Slide and the slide jump on flat ground, slopes, steps, off ledges (the 2 m/s fall), into walls, in water; from the air; the
   jump's height and reach against the numbers; no fall damage after; the cooldown.
3. Railgun on zombies and skeletons (16/s, 150 m/s, spread, headshots), energy gain and decay, reload.
4. Charged Shot: damage by energy, headshots, piercing a line of zombies, falloff at range, the lockouts.
5. Disruptor Shot: the field on mobs, walls, the ground, at max range; its damage per second through armor.
6. Overclock with the meter, charged shots during it; Sharpshooter's refund on crits.
7. Perks (Overcharged, Deceleration Field, Dual Thrusters, Friction Generators).
8. Sounds, voice and effects from her data. 9. Emotes from the data. 10. Two players, a night, the general play; release.
