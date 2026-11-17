# Torbjörn in live Overwatch: numbers and sources (research, 2026-11-14)

**BZ** = Blizzard's live patch notes (`https://overwatch.blizzard.com/en-us/news/patch-notes/live/YYYY/MM/`), every month 2025-09 to
2026-11 read 2026-11-14 for his lines (2024-01 to 2025-08 through W's dated log, which agrees with BZ line for line where both were
checked: 2025-09-16, 2026-02-10, 2026-09-08). 2026-11 holds no Torbjörn line. **His last live number change is BZ 2026-09-08**
(Rivet Gun alt spread 4.5 -> 4 degrees; Anchor Bolts' throw +50 % -> +25 %); the last line naming him is BZ 2026-10-06 (hit volumes:
head 23.3 -> 20, claw hand 15 -> 11). **W** = the Overwatch wiki through its MediaWiki API (`overwatch.weirdgloop.org/api.php`):
Torbjörn rev **409440** (2026-11-08T11:44Z). Requests carried a generic user agent and no personal data; the raw pages stayed in the
scratchpad. The game-data pass (build 154088) is [torbjorn-map.md](torbjorn-map.md); where they differ, `TorbjornStats` says which won.

Confidence: **H** BZ and W agree (or W gives it and BZ's history leads to it); **M** one source; **L** sources disagree.

Left out (not the live game): **Stadium** (BZ 2025-10 added him: All Grown Up, Turriplets, Positive Reinforcement, Hammer Throw,
Blacksmith, Swedish Sauna, Magmini ... tuning through 2026-07), **Community Crafted / June 2026 arcade** (BZ 2026-06: "Deploy Turret now
fires fast-moving rockets", "Overload increases primary fire projectile speed by 100%"), **April Fools** (BZ 2026-04-01: a second
Torbjörn on top), **Haunted Masquerade** masks (BZ 2025-10), 6v6 (BZ 2025-03-18 6v6 line), PvE (turret 15 damage).

**Patches that touched him (live)**: **2024-02-13** global projectile sizes; **2024-03-12** turret 11 -> 13, global +0.1 -> +0.075 m for
>50 m/s projectiles; **2024-08-20** health 250 -> 225, armor 50 -> 75; **2024-12-10** Forge Hammer 55 -> 70, turret build 3 -> 2.5 s,
turret health 225 -> 250; **2025-02-18** perks (Craftsman, Fully Loaded minor; Anchor Bolts +50 %, Overloaded Turret major);
**2025-03-18** turret health 250 -> 225, damage 13 -> 12; **2025-04-22** Overloaded Turret also when Overload precedes the build's end;
**2025-08-26** Craftsman out, **Hammer Time** (minor, +20 % with the hammer) in; **2025-09-16** Overload 5 -> 6 s, Molten Core 160 -> 190
DPS; **2026-02-10** Fully Loaded out, **Pre-Heated** (minor: Molten Core activates Overload) in; sub-role **Specialist** (eliminations: +50 %
reload speed for 3 s), the Damage role passive removed (its healing cut made global); **2026-02-18** fix to "the rate of his Primary
Fire and Turret fire"; **2026-09-08** alt spread 4.5 -> 4, Anchor Bolts 50 -> 25 %; **2026-10-06** hit volumes.

## The kit now (W rev 409440 unless noted)
| | Number | Conf |
|---|---|---|
| Health | 225 + 75 armor; sub-role Specialist | H |
| Perk XP | minor 1750, major 4375 (in all) | M |
| Rivet Gun (primary) | 70, headshot ✓, 70 m/s, 0.175 m (0.10 + 0.075), 1 per 0.51 s, 18 ammo, reload 2 s (ammo back at 0.992 s), no spread | H |
| Rivet Gun (secondary) | 10 pellets of 12.5 -> 3.75 (10-20 m), headshot ✓, 1 per 0.7 s, 2 ammo, 4° spread, 120 m/s, 0.05 m | H |
| Forge Hammer | 70, no headshot, 1.43 swings/s (0.352 s in + ~0.1 + 0.6 s), hit 0.192 s, 2.5 m; repairs his turret 50; swap 0.4 s; quick melee with it out swings it | M |
| Deploy Turret | 12 s cooldown (5 s redeploy), 225 health, 12 a shot, 4 shots/s (0.256 s), 40 m, thrown 17 m/s up to 15 m, 2.5 s build, 0.51 s recovery; attacks the closest enemy in sight until it's dead or out of sight, switching to an enemy his primary damages; in combat (dealt or took damage in 3 s) a redeploy waits longer | M |
| Overload | 10 s cooldown, 6 s, 100 overhealth, +30 % move (7.15 m/s, over the speed cap), primary 0.368 s, alt 0.5 s, hammer 0.6 s, reload 1.4 s, +6 ammo, 0.686 s cast | H |
| Molten Core | 2350, 0.592 s cast, 5.5 s at most, 10 shots, 40 m/s, 0.2 m, 25 on impact; pools 2.4 m, 10 s, 190 / s + 100 / s on armor, not stacking; bounce off walls; reloads the Rivet Gun | H |
| Hammer Time (minor) | +20 % move speed with Forge Hammer out (6.6 m/s) | H |
| Pre-Heated (minor) | Molten Core activates Overload | H |
| Anchor Bolts (major) | throw range +25 % (15 -> 18.75 m), attaches to walls and ceilings | H |
| Overloaded Turret (major) | Overload upgrades the turret for 5 s: +150 health (375), 3 missiles of 30 every 1.5 s, a 2 s upgrade first in which it can't fire | M |
