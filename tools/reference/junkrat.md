# Junkrat in live Overwatch: numbers and sources (research, 2026-11-13)

**BZ** = Blizzard's live patch notes (`https://overwatch.blizzard.com/en-us/news/patch-notes/live/YYYY/MM/`, every month 2024-01 to
2026-11 read 2026-11-13; 2026-11 holds only 11-03 and 11-05, and the only Junkrat line there is the Loot Hunt event).
**The last live number change to him is BZ 2026-09-08** (Concussion Mine 8 -> 7 s, Nitro Boost 125 -> 100 %); the last live change
that names him at all is BZ 2026-10-06 (hit volumes, and a fix: "reload speed reductions stacking twice on Frag Launcher").
**W** = the Overwatch wiki on Weird Gloop through its MediaWiki API (`overwatch.weirdgloop.org/api.php`): Junkrat rev **410886**
(2026-11-13T05:31Z, read 2026-11-13), Sub-Roles 406777, Global Passive Abilities 408982, Melee 402309, Movement speed 402311 (all read
2026-11-13). His ability pages (Frag Launcher, Concussion Mine, Steel Trap, RIP-Tire) are 2016 redirects to the hero page and Total
Mayhem is a disambiguation page: every number is on the hero page. His page carries a dated balance log that agrees with BZ 2024-2026
line for line. Every number here is public; the game-data pass (build 154088) is [junkrat-map.md](junkrat-map.md), which also says where the
data and these numbers agree. Requests carried a generic user agent and no
personal data; the raw pages stayed in the scratchpad.

Confidence: **H** BZ and W agree (or W gives it and BZ's history leads to it); **M** one source (usually W alone); **L** sources
disagree, or derived.

Left out (not the live game): **Stadium** (BZ 2025-06-24 added him with powers 2 Frag 2 Frurious, Big Bang, Soot Shaker, Bango!,
Bingo!, It's A(nother) Trap, Trap II Esquire, Slapnel, Hop Boom, Rainin' Lead and items; tuning 2025-07-08, 2025-09-04, 2025-09-16,
2025-10-14, 2025-11-11, 2026-04-14, 2026-06-16, 2026-07-14, 2026-08-11: Stadium only), **Community Crafted** (BZ 2026-06-30: health
250 -> 200, Frag Launcher 112.5 m/s and 30 / 50 detonating on impact, Steel Trap 2 charges 13 s bleed field, Concussion Mine explodes
under him for 90; arcade only, June 30 to July 13. Its "from" values do confirm the live numbers at the time: 250 HP, 30 m/s, 45 / 80,
Steel Trap 10 s and 100 HP, mine 120), **Haunted Masquerade** (BZ 2025-10-14 masks: "Enemies explode on death", gasoline mines),
**Quick Play Hacked** (BZ 2026-07-30: global healing cut 15 %; Specialist untouched), April Fools (BZ 2024-04-01 detonate grenades with
Secondary Fire, 2025-04-08 Totally Normalwatch 5 mines and invisible RIP-Tire, 2026-04-01 New Trap City: 3 traps for scrap), **6v6**
modes (no Junkrat-specific 6v6 line in range), PvE/co-op, Mystery Heroes (BZ 2026-04-14: perks off), console aim assist.

**Patches that touched him (live, 2024-2026)** (dates are the patch notes' titles; W agrees unless noted): **2024-02-13 (S9)** health
200 -> 250; Frag Launcher base projectile 0.25 -> 0.2 m plus the new global +0.15 m for travel-time shots at or under 50 m/s; ult costs
+10 %; regeneration for all; Damage role passive becomes a healing cut; quick melee 30 -> 40; **2024-02-21** Concussion Mine cooldown
8 -> 7 s, max damage 110 -> 120, min 20 -> 55; **2024-03-12** fix: Damage passive now applied by RIP-Tire;
**2024-04-16** fix: RIP-Tire no longer gets the self-healing (regeneration) passive; **2024-05-14** direct impact 40 -> 45 ("Total
damage increased from 120 to 125"); out-of-combat regen 10 HP/s + 5 % max; **2024-08-20** "Projectile size is now reduced by half after
the second ricochet", "Projectiles preserve slightly more velocity on ricochet"; **2024-11-12** Concussion Mine 7 -> 8 s; **2024-12-10**
global "Projectile radius large reduced from 0.15 to 0.12 meters"; role passive 25 % / 3 s; **2025-02-18 (S15) perks** (Aluminium
Frame, Nitro Boost minor; Frag Cannon, Tick Tock major); **2025-04-22** Aluminum Frame also reveals trapped enemies to allies, Tick Tock
text; **2025-06-24** role passive 30 %; **2025-07-22** Aluminum Frame now anti-heal instead of reveal, Tick Tock arm 0.5 -> 0.25 s;
**2025-08-26** Aluminum Frame and Tick Tock out, **Mine Recycling (minor)** and **Bomb Voyage (major, 35 %)** in; **2025-12-09** Frag
Launcher "Projectile speed increased from 25 to 30", Frag Cannon +40 -> +25 % and no longer -1 ammo; **2026-02-10 (Season 1)** sub-role
**Specialist**, Damage role passive removed (its healing cut made global); **2026-04-14** Mine Recycling minor -> major, Bomb Voyage
major -> minor at 35 -> 25 %; **2026-06-16** Bomb Voyage 25 -> 35 %; **2026-09-08** Nitro Boost "Bonus speed reduced from 125% to
100%", Concussion Mine 8 -> 7 s; **2026-10-06** torso hit volume 32 -> 29, legs 17.3 -> 20; fix for reload speed reductions stacking
twice on Frag Launcher. Nothing about him (live) in BZ 2024-01, -06, -07, -09, -10, 2025-01, -03, -05, -09 (Stadium fixes only), -10
(Stadium and masks only), -11 (Stadium and a skin fix), 2026-01 (skin fix), -03, -05, -07 (Stadium), -08 (Stadium), -11.

## Base
- **Health 250**, no armor, no shields (BZ 2024-02-13 200 -> 250; BZ 2026-06-30 Community Crafted "from 250" confirms it then; W 250)
  **H**. No later live change (BZ 2025-08-26 cut other heroes' health, not his).
- **Damage, sub-role Specialist** (BZ 2026-02-10 list: Bastion, Emre, Junkrat, Mei, Soldier: 76, Symmetra, Torbjörn; W the same) **H**.
- **Move speed 5.5 m/s** (W Movement speed: he has no speed entry of his own, only Nitro Boost for the tire; the global default) **H**.
  Backwards 90 %, crouched 3 m/s, jump as everyone (W, as in the Tracer note).
- Quick melee **40**, 1 s cooldown, 2.5 m, hitbox 0.13 s, 0.528 s recovery (BZ 2024-02-13 30 -> 40; W Melee) **H**.
- Perk XP: **minor 1675, major 4185** (W infobox) **M**.
- **Ult cost 2100** (W RIP-Tire "2100 points"; BZ 2024-02-13 +10 % global, no Junkrat ult cost line in range) **M**.
- **No crits anywhere in his kit**: Frag Launcher, Concussion Mine, Steel Trap, RIP-Tire, Total Mayhem all "headshot ✕" (W) **M**.
- Hit volumes (BZ 2026-10-06, game units): torso **29**, legs **20**; head and arms: no source.

## Role and sub-role passives
- **Damage role passive: none since BZ 2026-02-10.** Its effect is now global: damage dealt cuts the target's healing received **-30 %**
  (**-15 %** on tanks) for **3 s** (BZ 2026-02-10; W Global Passive, which marks it "removed": see the Tracer note) **H**. Self-damage
  does not apply it (W).
- Global out-of-combat regeneration: after **6 s** without damage, **10 HP/s + 5 % of max HP/s** = **22.5 HP/s** for him (BZ
  2024-05-14, 2026-02-10 "6 seconds remains unchanged"; W Global Passive) **H**. RIP-Tire does not get it (BZ 2024-04-16 fix).
- **Specialist:** "Eliminating an enemy briefly increases reload speed." **Reload Speed Bonus 50 %, Duration 3 s** (BZ 2026-02-10) **H**.
  W: "Reload speed is 1.5x faster after an elimination", "refreshed if another elimination is earned", an indicator right of the
  crosshair; its table turns 1.5 s into **1 s** (so reload time / 1.5, not x 0.5) **H**. It is the old (pre-2024) Damage role passive
  (W). No later live tuning (BZ 2026-03 subrole tuning and Hacked 2026-07-30 skip it). BZ 2026-10-06 fixed "reload speed reductions
  stacking twice on Frag Launcher" (so one 1.5x, not 2.25x). Whether it applies mid-reload, and how the 0.848 s ammo point scales
  (0.565 s if proportional): no source **L**.

## Total Mayhem (passive)
- **Verdict: Total Mayhem is live, and it is both parts**: W's current official description "Deals no damage to self with explosives.
  Drop bombs on death." **H** (W; no BZ line in range changed it; Stadium powers and BZ 2025-06-24 still name "Total Mayhem bomb").
- **Self-damage: none.** He takes **no damage from his own explosives** (Frag Launcher, Concussion Mine, RIP-Tire, the death bombs);
  he does take their **knockback** (W Total Mayhem, Frag Launcher "max, self" knockback, Concussion Mine "does not deal self-damage,
  but still applies the knockback") **H**. No BZ line 2024-2026 changed this (the only self-damage line in range is Pharah's 50 -> 25 %
  on 2024-02-13); it predates the BZ range (W's balance log before 2024 not read past 2022 for it) **M** for the date.
- **Death bombs: 6 grenades, 50 damage each, radius 2 m, detonate 0.7 s** after death (W) **M**. Dropped where he died, no inherited
  momentum (killed mid-air: they fall straight down); no crit; the grenades pass barriers, the explosions do not; can be eaten by
  Defense Matrix etc. and deflected (W) **M**. Falloff, spread pattern and whether they bounce: no source.

## Frag Launcher (primary)
- **Direct impact 45 + splash 80 = 125 on a direct hit** (BZ 2024-05-14 "Impact damage increased from 40 to 45 (Total damage increased
  from 120 to 125)"; W 45 / 125 total) **H**. Splash **80 -> 10**, **falloff from 0.5 m to 2 m** radius (W "0.5 - 2 meters (falloff
  start - max)", "80 - 10 (splash, enemy)") **M**. Curve linear assumed **L**.
- **Projectile speed 30 m/s** (BZ 2025-12-09 25 -> 30 "without altering the arc"; W 30) **H**. Gravity: no number anywhere. BZ says the
  arc did not change, so the gravity was scaled with the speed: for the same trajectory shape, g scales with v², x 1.44 of the old
  value **L** (derived; the old gravity itself has no source).
- **Projectile radius 0.32 m** (0.2 base + 0.12 global "large travel-time") (BZ 2024-02-13 base 0.25 -> 0.2 and +0.15 global,
  2024-12-10 large 0.15 -> 0.12; W "0.2 base + 0.12 global") **H**. **Halved to 0.16 m after the second bounce** (BZ 2024-08-20; W) **H**.
- **Detonation** (W) **M**: on contact with an enemy hitbox; after **2 bounces off the ground**; after bouncing off surroundings and then
  landing on the ground; or after **4.5 s** of flight. Before going off it plays an audio cue and **freezes in place 0.3 s**, unstoppable;
  a nade that bounced once explodes if it bounces again within **1.5 s**. It can bounce off walls and objects several times without
  exploding. **Velocity slightly reduced each bounce** (W; BZ 2024-08-20 "preserve slightly more velocity", no factor) **M**/**L**.
  How the wall/floor distinction is made (surface normal threshold): no source.
- **Fire rate 1.5 shots/s** (0.667 s), recovery stops while meleeing (W) **M**. **Ammo 5** (W; Frag Cannon's -1 removed BZ 2025-12-09)
  **H**. **Reload 1.5 s animation, ammo restored 0.848 s in** (W) **M**. No spread (W).
- **Knockback** (W) **M**: enemy max **9.4 m/s** (H 5, V 8); **self max 4.7 m/s** (H 2.5, V 4), no damage to him.
- In-air grenade can be eaten by Defense Matrix, Kinetic Grasp, Javelin Spin, Power Matrix, Kekkai Sanctuary, Trash Compactor and
  reflected by Deflect; the splash is not (W) **M**. No crit.

## Concussion Mine (ability 1, Shift; detonate with Secondary Fire)
- **Damage 120 -> 55** (falloff **1 m -> 3 m**) (BZ 2024-02-21 max 110 -> 120, min 20 -> 55; W "120 – 55", "1 - 3 meters") **H**
  (radii M). No damage to Junkrat (above).
- **Cooldown 7 s, 2 charges** (BZ 2026-09-08 8 -> 7, history 2024-02-21 8 -> 7, 2024-11-12 7 -> 8; W 7 s, charges 2) **H**.
- **Knockback 21.2 m/s (H 15, V 15)** to him and to enemies; everyone is thrown **up at 45°** whatever their position, horizontal part
  **away from the mine's centre** (W) **M**. Self-launch is the mobility use.
- **Thrown: 25 m/s, projectile radius 0.2 m**, arcing (W) **M**. Max range / fuse: none stated; it lasts **until detonated or
  replaced** (W). **Cast 0.128 s throw, 0.112 s recovery to detonate** (W) **M**. After the cast the flight path can still change (W).
- **Sticks to any terrain**; bounces off players and objects (not Petal Platform); bounces once off a barrier then can pass through;
  the explosion is blocked by barriers (W) **M**. **Health 25**: enemies can destroy it, no explosion (W) **M**. Reflected mines stop
  sticking and explode at once (W).
- **No arm time** in the base kit (the arm delay was only Tick Tock's, removed BZ 2025-08-26) **H**.
- **One mine out at a time** per W ("If Junkrat throws another mine while one is already deployed the first mine will instantly and
  silently disappear") **M**, though he holds 2 charges (Gap 3). Hacked: the remote is disabled (W).
- Interrupted before the mine exists: no charge spent (W).

## Steel Trap (ability 2, E)
- **100 damage** on trigger, **root 2.5 s** ("immobilize": no movement and no movement abilities) (W; BZ 2024-12-10 names it among
  "mobility locking abilities") **M**. No BZ number change in range; BZ 2026-06-30 Community Crafted "from 10s", "from 100" health
  confirms cooldown and health **H** for those two.
- **Cooldown 10 s** (W; BZ CC "from 10s") **H**. **Health 100** (W; BZ CC "from 100") **H**. Starts when thrown? not stated.
- **Arm time 0.75 s**, trigger **radius 1 m**, thrown **17 m/s**, projectile **radius 0.3 m**, cast instant + 0.33 s recovery (W)
  **M**. Lasts until triggered or replaced. **One at a time** (a second removes the first) (W) **M**.
- Sticks to ground and walkable surfaces; bounces off players, walls and other surfaces; bounces once off a barrier then falls through
  (W) **M**. An unarmed/untriggered trap can be destroyed by enemies (W). Junkrat sees it through walls and gets a cue when it triggers
  or is destroyed (W). Phased out / Particle Barrier stop it triggering but do not free; Fortify, Life Grip, Translocator (already in
  the air), Teleporter, Petal Platform and Ice Wall lifts break it (W) **M**.
- Max throw range: no number (Aluminium Frame's +50 % range, removed, implied one) **L**.

## RIP-Tire (ultimate, Q)
- **Cost 2100** (W) **M**.
- **Damage 600 -> 60**, radius **2 m to 10 m** (W writes "10 - 2 meters (falloff start - max)": Gap 4) **M**/**L**. Knockback **8.5
  m/s (H 6, V 6), enemies only** (W) **M**. No damage to Junkrat (passive).
- **Tire 12 m/s, health 100, max 10 s** (W) **M**. It **climbs walls** (hold Jump facing a wall, or automatically with the hero
  option) and **can jump** (W) **M**. Passes through barriers; the explosion is blocked by them (W).
- **Cast 1.58 s startup + 0.08 s recovery**; detonate (Primary or Secondary) **0 + 1.0 s recovery** (W) **M**. Interruptible in the
  startup; once he controls the tire, stuns or his death don't stop it: it can still be detonated (W). **Junkrat is rooted** while it
  runs, view on the tire (W) **M**. **Ends**: detonation by him, **explodes on its own at 10 s**, or **destroyed: no explosion**, the
  ability ends (W) **M**. "Reloads" keyword: Frag Launcher is reloaded (W) **M**.
- The tire gets no regeneration (BZ 2024-04-16 fix) and does apply the healing cut (BZ 2024-03-12 fix) **H**.

## Perks (BZ 2025-02-18 on; W)
- **Minor: Nitro Boost**: during RIP-Tire press Ability 1 (Shift) for "a quick boost of speed"; doing so **cuts the tire's damage
  50 %** (300 -> 30) (BZ 2025-02-18; W) **H**. Speed **+100 % (24 m/s)** (BZ 2026-09-08 125 -> 100 %; W +100 % / 24 m/s) **H**. Only the
  tire, not Junkrat (W Movement speed). Duration, one use or several, ramp: no source (Gap 5).
- **Minor: Bomb Voyage**: after launching with Concussion Mine, **+35 % attack speed for 3 s** (BZ 2025-08-26 major 35 %, 2026-04-14
  minor 25 %, 2026-06-16 25 -> 35 %; W) **H**. Does not stack; a new launch refreshes it (W) **M**. Whether "attack speed" covers reload:
  no source. What counts as "launching" (own-mine knockback only, airborne required?): no source.
- **Major: Mine Recycling**: "Eliminations with Concussion Mine restore one charge." (BZ 2026-04-14; 2025-08-26 "Killing blows"; W)
  **H**.
- **Major: Frag Cannon**: **Frag Launcher projectile speed +25 %** (30 -> **37.5 m/s**), no ammo cost (BZ 2025-12-09 40 -> 25 % and
  "No longer reduces ammo capacity"; W 37.5) **H**. Arc with it: if gravity is unchanged the throw flattens; no source **L**.
- Removed: Aluminium Frame (minor, 2025-08-26: Steel Trap throw range +50 %, last version trapped enemies cannot be healed), Tick Tock
  (major, 2025-08-26: mine strengthens 0.25 s after landing, +50 % damage, +50 % radius, +200 % health).

## Hero options (W)
- **RIP-Tire automatically climb walls** (off by default): off = hold Jump facing a wall to climb; on = facing a wall climbs.

## Disagreements and gaps
1. **Frag Launcher DPS:** W's own "180 while firing" is 120 x 1.5, the pre-2024-05-14 total; with 125 it is **187.5** (or 1.5/s is a
   rounded 1.44/s). Ours: 125 per direct hit, 0.667 s between shots.
2. **Bounce rules:** W's detonation list (2 ground bounces, wall-then-ground, 4.5 s, 0.3 s freeze, 1.5 s second-bounce window) has no
   BZ counterpart; BZ 2024-08-20 only says "reverted to how grenades used to ricochet in early Overwatch" with more velocity kept. The
   per-bounce speed factor, gravity and the wall/floor test are unknown. Ours: W's rules, a guessed restitution, gravity fitted to an
   arc (a D pass would settle all three).
3. **Two mine charges, one mine out:** W says a second mine makes the first vanish, which leaves the second charge useful only for two
   quick separate jumps/combos. Plausible but single-source. Ours: W's rule.
4. **RIP-Tire radius:** W "10 - 2 meters (falloff start - max)" is reversed against its own Frag (0.5 - 2) and Mine (1 - 3) entries;
   read as full 600 within 2 m falling to 60 at 10 m **L**. Falloff curve linear assumed.
5. **Nitro Boost:** BZ never gave the 125 % before cutting it; duration, how many boosts per ult, and whether the -50 % applies once
   used: no source. Ours: one boost, +100 % (24 m/s) for a short burst (duration ours), damage halved from then on.
6. **Steel Trap range, Concussion Mine range:** neither has a max distance; both are arcing throws at 17 / 25 m/s with unknown gravity.
7. **Specialist and the ammo point:** whether 1.5x applies to a reload already underway, and the scaled ammo-restore moment.
8. **Total Mayhem bombs:** falloff, scatter and bounce unknown; only 6 x 50, 2 m, 0.7 s.
9. **Ult cost 2100, perk XP 1675 / 4185, hit volume units** are W / BZ only; a game-data pass would settle them.
10. **Self-damage history:** "no self-damage" is W's current official text and nothing in BZ 2024-2026 touches it; when it was set is
    before the range read.
