# Juno in live Overwatch: numbers and sources (research, 2026-11-01)

**D** = the game data, build 154088 (2026-10-06), read offline and read-only: **left empty here** (`D _`), filled in by the
data pass. **BZ** = Blizzard's live patch notes (`https://overwatch.blizzard.com/en-us/news/patch-notes/live/YYYY/MM/`, read
2026-11-01: every month from 2022-10 to 2026-11, 2026-10 and 2026-11 fetched again on 2026-11-01; 2026-11 is empty: the last
patch is **2026-10-20**, a bug fix with nothing for her). **The last patch that touched her is BZ 2026-10-06** (Season 5: hit
volumes torso 19 -> 23, hands 8.5 -> 5, arms 9 -> 7, and the global "small" hitscan size 0.04 -> 0.02 m); **the last change
to her kit is BZ 2026-08-11** (Pulsar Torpedoes' minimum lock-on 0.5 -> 0.35 s). **Nothing for her on 2026-10-13 or 10-20**,
so the data's build (10-06) should hold every live value. **W** = the Overwatch wiki on Weird Gloop through its MediaWiki API
(`overwatch.weirdgloop.org/api.php`, read 2026-11-01: Juno revision **406865** of 2026-10-20T18:31Z, plus all 387 Juno
revisions since 2023-11 diffed field by field; Juno/Stadium 408513; and, read the same day for Brigitte: Sub-Roles 406777,
Global Passive Abilities 402069, Hit points 401654 (Armor and Overhealth redirect there), Movement speed 402311 (Movement
redirects there), Melee 402309, Status effect 408615 (Crowd Control and Knockback redirect there), Ultimate ability 395578,
Perks 354523). Mediblaster, Pulsar Torpedoes, Glide Boost, Hyper Ring and Orbital Ray redirect to hers; "Martian Mobility"
does not exist (her passive is **Martian Overboots**). Blizzard's hero page and Liquipedia were not read. Raw pages, the
revision dump, the field-by-field diff and the patch notes as text stayed in the scratchpad.

Each number gives D first (empty for now), then BZ, then W. Confidence (public sources only, to be revised once D is in):
**H** BZ and W agree (or W gives it and BZ's history leads to it); **M** a single public source; **L** the sources
disagree, or a guess. Where they differ, both are given here and in "Disagreements".

Left out (not the live game): **Stadium** (BZ from 2025-04-22, tuned 2025-05-01, 05-29, 06-24, 07-03, 07-09, 07-22, 07-24,
08-26, 09-16, 10-22, 11-11, 12-09, 2026-04-14, 04-23, 06-16, 06-25, 07-14; W Juno/Stadium: powers Fission Chamber, MediMaster,
Stinger, Medicinal Missiles, Pulsar Plus, PulStar Destroyers, Torpedo Glide, Blink Boosts, Heal Formation, Hyper Healer,
Marswalking, Stellar Focus; items Vantage Shot, Long Range Blaster, Pulse Spike, Boost Kick, Lux Loop, Red Promise
Regulator, Cosmic Coolant, Solar Shielding, Lock-on Shield, Boosted Rockets, Black Hole, Orbital Alignment), **Community
Crafted** (BZ 2026-06-30: Locked On replaced by Wormhole, Hyper Ring +50% range, -25% ally speed, slows enemies 35%; Orbital
Ray deals 60/s and travels toward Juno; Survivor gives +10 max health a minute instead of the regeneration, in that mode
only), April Fools (BZ 2025-04-01 Orbital Ray reverses gravity, quick melee knockback; 2026-04-01 Hyper Ring sideways, higher
jumps), Haunted Masquerade masks (BZ 2025-10-14: Mei Mask on Juno freezes slowed targets with Pulsar Torpedoes; Juno Mask on
Mei), the **hero trial** (BZ 2024-07-19 to 21: 250 HP, 28 -> 34 rounds/s, 75 + 60 torpedoes, Hyper Ring 16 s, ray 1.9 m/s),
Mystery Heroes (perks off since BZ 2026-04-14), **6v6** (BZ 2024-12-17 6v6 Experiment: torpedoes 10 -> 12 s, Hyper Ring 6 ->
4.5 s, Orbital Ray 10 -> 8 s; W: torpedoes back to 10 and ring to 6 s on 2025-03-18, "undocumented"; BZ 2026-04-14 "(6v6)
Cooldown increased from 10 to 12 seconds" for Pulsar Torpedoes, so **12 s in 6v6 now**; Locked On caps at 6 s there, W).
Her removed perks (Hyper Boost, Re-Boots, Master Blaster) are history only.

**Patches that touched her (live 5v5):** **2024-08-20 release** (Season 12: health 250 -> 225, burst 34 -> 28 rounds/s,
torpedoes 75 -> 85 impact and 60 -> 50 over time, no-lock cancel cooldown 5 -> 2 s, +5 m lock range forgiveness, Hyper Ring
+3.5 m range and 16 -> 14 s, ray 1.9 -> 2.25 m/s); 08-23 Mediblaster 7 -> 8, Glide Boost 8 -> 6 s; 09-03 Mediblaster 14.3 ->
15.5 rounds/s (back to 34 in the burst), 8 -> 7.5 damage, 7 -> 6.5 healing; 10-15 falloff 30-50 -> 20-35 m, healing 6.5 ->
6; 10-29 Orbital Ray 100 -> 85/s; 11-12 ult cost +10%. 2025-01-21 Orbital Ray 10 -> 8 s, +35 -> +30%; **02-18 perks added**
(Familiar Vitals, Hyper Boost minor; Master Blaster, Re-Boots major); 03-18 Re-Boots +25% Glide Boost duration; 03-25 ult
cost +10%; 04-22 Hyper Boost out, Re-Boots minor (no duration), Locked On new major (1.5 s); **07-22 falloff minimum 20 ->
25 m, Glide Boost's air acceleration reduced on direction changes after the burst**; **08-26 Re-Boots and Master Blaster
out, Locked On minor (1 s), Lift Off and Faster Blaster in**; 10-14 ult cost -8%; **11-11 Glide Boost air acceleration +50%
on direction changes**; **12-09 (Season 20) Mediblaster can crit**, friendly aim assist 100% (console). **2026-02-10 (Season
1): Survivor sub-role, support passive removed, healing reduction made global**; **04-14 Glide Boost +50% -> +40%**, (6v6)
torpedoes 10 -> 12 s; **06-16 Faster Blaster 20 -> 22 shots/s**; **08-11 lock-on minimum 0.5 -> 0.35 s**; **10-06 hit
volumes** and global hitscan size. Nothing on 2026-10-13 or 10-20. Bug fixes only: 2024-08-20, 08-23, 10-15 (Sigma's
overhealth from her shots), 12-19 (ray's damage boost stacking), 2025-02-25 (Master Blaster on Fortified Orisa), 06-03 (heal
sounds), 08-28 (falloff shorter than intended), 2026-03-10 (weapon variants on torpedoes).

## Base
- **75 health + 150 shields = 225, no armor** (D _; BZ 2024-08-20 "Base health reduced from 250 to 225"; W 75 / 150 since rev
  239188, 2024-08-21, 100 / 150 in the trial) **H** (total) / **M** (the split, W only).
- **Shields** (W Hit points): regenerate **30/s after 3 s without damage** (5.76 every 0.192 s), on top of the global
  regeneration; spent before health, healed after it **M**.
- **Support**, sub-role **Survivor** (D _; BZ 2026-02-10 "Heroes: Brigitte, Illari, Juno, Mizuki, Wuyang"; W) **H**.
- Speed **5.5 m/s**, backward 4.95, crouched 3 (D _; W Movement speed, the global walk) **M**. She skates on the ground (her
  floating walk was dropped in development because silent movement was "problematic", W citing the 2026-02-20 Behind the
  Sounds video) **M**. Global jump 5.72 m/s (0.98 m apex), gravity 17.5 m/s^2 easing to a 30 m/s fall, **no fall damage
  from any height** (W Movement speed) **M**.
- **Hit volumes** (no units given): **torso 23, hands 5, arms 7** (D _; BZ 2026-10-06 19 -> 23, 8.5 -> 5, 9 -> 7; W copies
  it) **H**. Head, legs: not given.
- **Quick melee**: the global one, **40 damage, 1 s cooldown, 2.5 m**, active 0.128 s, a very small knockback (D _; W Melee)
  **M**. Her own animation: a strike (the trailer's roundhouse kick was changed, W) **M**.
- Ult charge (W Ultimate ability): 5 points/s passively, 1 point per damage or healing, none from the global regeneration or
  ult damage/healing, half from damage to overhealth, -40% from damage/healing on tanks; none while Orbital Ray is active
  **M**.

## Martian Overboots (passive; movement) -- how she moves
- "While airborne, press Jump to double jump and hold Jump to hover." (BZ 2024-07-19; W).
- **Double jump: one extra jump in the air, 10.8 m/s up** (D _; W since rev 238119, 2024-07-20) **M** (nearly twice the
  ground jump's 5.72). Its height: not given (L, ours: 10.8^2 / (2 x 17.5) = 3.3 m above where it starts, ignoring the air
  drag of the falling formula; whether it replaces or adds to her vertical speed: not given).
- **Hover: holding Jump in the air caps her fall at 3 m/s** (D _; W "3 m/s (falling)", "causes her to fall slower when the
  jump input is held") **M**. **No time limit or fuel** is given by any source (W lists none; L: none). It does not climb,
  only slows the fall; she keeps her horizontal control.
- Options (W): **Toggle Martian Overboots Hover off** (hold; on = press again to stop), **Crouch activates Martian Overboots
  Hover off** (on = crouch also starts the hover in the air) **M**.
- In Glide Boost the jump counts as the double jump (she is airborne), so from the ground she gets **one** jump in a glide;
  a double jump before the glide plus the glide's no-gravity gives more height (W) **M**. Usable during Pulsar Torpedoes'
  lock-on without breaking it (W) **M**.
- **Lift Off (major perk) adds a third jump** (BZ 2025-08-26; W) **H**; its strength: not given (L: same as the double).
- W lists Martian Overboots, Lift Off and Glide Boost as her movement abilities (W Movement) **M**.

## Mediblaster (primary fire; hitscan burst)
- "Burst fire weapon that heals allies and damages enemies." (BZ 2024-07-19; W). The same shots damage enemies and heal
  allies (no separate heal fire); allies and enemies can body-block them (W) **M**.
- **7.5 damage / 6 healing a round, 12 rounds a burst = 90 / 72 a burst** (D _; BZ 2024-09-03 8 -> 7.5 damage, 2024-10-15
  6.5 -> 6 healing; W) **H**.
- **Rounds: 34/s in the burst** (0.029 s apart, a burst lasts about 0.35 s), **15.5/s overall = one burst every 0.774 s**
  (1.29 bursts/s) (D _; BZ 2024-09-03 "Fire rate increased from 14.3 to 15.5 (reverted to the 34 shots per second during the
  projectile volley)"; W) **H**. DPS 116.28 / HPS 93.02, 98.3 / 78.64 counting reloads (W) **M**.
- **Ammo 180 = 15 bursts, 12 a burst; reload 1.5 s** (D _; W) **M** (see Disagreements for 1.5 vs 1.8 s).
- **Falloff: full to 25 m, down to 30% at 35 m** and beyond, damage and healing alike (D _; BZ 2024-10-15 30-50 -> 20-35 m,
  2025-07-22 minimum 20 -> 25 m; W; the 30% floor W only) **H/M**.
- **Crits: 1.5x, on enemies only** (D _; BZ 2025-12-09 "Mediblaster can now deal critical damage against enemies."; W
  "1.5, damage only") **H/M**.
- Spread **0.85 degrees** (D _; W rev 262034) **M**. Shot size **0.02 m against enemies, 0.22 m for allies** (0.2 own + 0.02
  global) (D _; BZ 2026-10-06 small hitscan 0.04 -> 0.02; W rev 402079) **M**.
- The gun shows a happy face on an ally, an angry face on an enemy (W); its sound and look change at low ammo (BZ 2024-08-20)
  **M**. Friendly aim assist default 100% on console (BZ 2025-12-09) **M**.

## Pulsar Torpedoes (secondary fire; lock-on, homing projectiles)
- "Press Secondary Fire to lock on, then Primary Fire to fire homing projectiles. Heals allies over time and damages
  enemies." (BZ 2024-07-19; W).
- **Enemies: 85 damage a torpedo. Allies: 85 healing at once + 50 over 2.5 s** (20 HP/s, ticks of 3.84 every 0.192 s) (D _;
  BZ 2024-08-20 75 -> 85, over time 60 -> 50; W) **H**. No head hits (W) **M**.
- **Cooldown 10 s**, from when they're fired; **2 s if it ends or is cancelled without a lock** (D _; BZ 2024-08-20 "reduced
  from 5 seconds to 2 seconds"; W; 12 s in 6v6, BZ 2026-04-14) **H**.
- **Targeting range 40 m**, **+5 m forgiveness** for targets already being locked (D _; BZ 2024-08-20 "max 40 meters range";
  W) **H**. The projectiles have no range limit once fired (W) **M**.
- **Lock-on time 0.35 s at 5 m rising to 1 s at 40 m** (by distance), per target (D _; BZ 2026-08-11 minimum 0.5 -> 0.35; W)
  **H**. **Targeting window up to 4 s**; it fires by itself at the end if anything is locked (D _; W "0.38 - 4 seconds") **M**.
  Lock progress pauses (not resets) when a target leaves sight or the range (W) **M**.
- **Targets heroes only** (and B.O.B.), allies and enemies at once, in sight (D _; W) **M**. How many: **not given** (no
  source gives a maximum).
- Cast **0 + 0.304 s** recovery (W) **M**. **Channeled**: no shooting or other abilities while locking, but **Glide Boost
  and the jumps/hover work**; stun, knockdown or hack interrupt it; Orbital Ray ends it; secondary fire again cancels (W)
  **M**.
- Flight: homing; damage torpedoes hit the first enemy or object in the way and are stopped by terrain and barriers;
  healing torpedoes hit only their ally and follow them through terrain (W) **M**. Barriers don't stop the lock (W) **M**.
  Deflected ones don't home (W) **M**. Speed: not given.
- Options (W): **Toggle Pulsar Torpedoes on** (press again to fire; off = fire on release), **Swap Pulsar Torpedoes toggle
  button inputs off** (primary fires, secondary cancels) **M**.

## Glide Boost (ability 1; movement)
- "Glide horizontally with increased movement speed." (BZ 2024-07-19; W).
- **Duration 4 s, cooldown 6 s from when it ends**, no cast time (D _; BZ 2024-08-23 8 -> 6 s; W) **H** (duration **M**).
  Pressing it again ends it early (option **Toggle Glide Boost on**; off = ends on release) (W) **M**.
- **A dash at the start: +247% (19.1 m/s)**, not counted against the +75% speed cap (D _; W since rev 269101, 2025-10-28,
  "initial speed boost has been reduced at some point"; earlier W +390% / 40 m/s) **L**. The dash's length: not given.
- **Then +40% (7.7 m/s) for the rest**, which does count against the cap (D _; BZ 2026-04-14 "Movement speed bonus reduced
  from 50% to 40%."; W rev 308869) **H**. It goes the way she is moving (the held direction at the start, W) **M**.
- **No gravity and no vertical input while it runs**: purely horizontal; she rises or sinks only by gliding along slopes or
  walls (sideways along a wall, back and forth, to climb) (D _; W) **M**. She counts as airborne: no crouch; jumping uses the
  double jump (W) **M**. It handles doorways better (BZ 2024-08-20) **M**.
- **Air control: acceleration on direction changes reduced after the burst (BZ 2025-07-22), then +50% back (BZ 2025-11-11,
  "partial reversion")** **H** (no absolute numbers in any source).
- **-50% knockback received** while gliding (W rev 277060, "flight resistance") **M**.
- At the end she falls normally (hover and double jump still available if unspent) (W, by the rules above) **M**.

## Hyper Ring (ability 2; projectile, area)
- "Deploy a ring that increases movement speed of allies that pass through it." (BZ 2024-07-19; W).
- **+50% speed (8.25 m/s) for 3 s**, refreshed each time an ally touches the ring (D _; W) **M**.
- **Cooldown 14 s** from activation (D _; BZ 2024-08-20 16 -> 14; W) **H**. **Ring lasts 6 s** (D _; W) **M**.
- **Thrown up to 17 m**: a projectile that becomes the ring where it hits terrain or at its range (D _; BZ 2024-08-20 "Deploy
  range increased by 3.5 meters"; W 13.5 -> 17) **H**. **Ring radius 4 m** (8 m across) (D _; W rev 266781) **M**.
- **Allies within 5 m of her or the projectile's path get the boost too**, and she gets it on casting (W) **M**.
- Cast **0.24 + 0.304 s** (W) **M**. Heroes only (and B.O.B.) (W) **M**. Goes through barriers; not eaten by Defense
  Matrix, not reflected by Deflect (W's ignores_barrier / matrix / deflect / boost / window fields all set) **M**.
- Stacks with Glide Boost and other buffs **up to the +75% global cap** (W details) vs **ignores the cap** (W's field) **L**
  (see Disagreements). The ring stands upright facing her throw (live; sideways only in April Fools / Stadium Marswalking).

## Orbital Ray (ultimate)
- "Call down a ray that travels forward, healing allies and increasing their damage." (BZ 2024-07-19; W).
- **2900 points** (D _; W rev 268419, 2025-10-15, from the in-game advanced info panel; BZ only in percent: 2024-11-12 +10%,
  2025-03-25 +10%, 2025-10-14 -8%: 2600 x 1.1 x 1.1 x 0.92 = 2894) **H/M** (see Disagreements).
- **8 s** (D _; BZ 2025-01-21 10 -> 8; W) **H**. Cast **0.496 + 0.304 s**; crowd control in the startup cancels it (W) **M**.
- **Allies inside: 85 HP/s** (8.16 every 0.096 s; 680 over the full 8 s) (D _; BZ 2024-10-29 100 -> 85; W) **H**, and **+30%
  damage dealt**, lingering **1 s** after leaving (D _; BZ 2025-01-21 35 -> 30%; W; the linger W only) **H/M**. No damage
  to enemies in live (BZ 2026-06-30's 60/s is Community Crafted only) **H**.
- **Radius 8 m**; it starts at her, the beam lands about 4 m ahead after ~1 s, and moves **forward at 2.25 m/s, up to 18 m**
  (W "range 20 m") (D _; BZ 2024-08-20 1.9 -> 2.25 m/s; W) **H/M**. Straight ahead, no steering (W) **M**.
- **Through everything**: barriers, walls, ceilings and floors (a vertical column), not blocked by Defense Matrix, Deflect
  etc. (W) **M**. Its effects start at the end of the cast, before the beam visibly lands (W) **M**.

## Survivor (sub-role) and the global passives
- **Survivor** (BZ 2026-02-10: "Using a movement ability activates passive health regeneration."; W Sub-Roles: "Movement
  abilities start passive health regeneration."): the global regeneration starts at once and runs for **at least 0.25 s**
  even if she's hit (W) **M**. **Glide Boost** surely counts; whether **the double jump / hover** triggers it: not stated (W
  lists Martian Overboots among movement abilities; L). BZ 2026-06-16 and 08-11 fixed it triggering before damage and
  without a movement ability.
- **The support role passive is gone** (BZ 2026-02-10; W) **H**.
- **Regeneration: 10 HP/s + 5% of max a second after 6 s without damage**, **21.25 HP/s** for her (225 max) (D _; BZ
  2025-06-24 5 -> 6 s, 2026-02-10 "unchanged"; W) **H**. It refills shields too, on top of their own 30/s (W) **M**.
- **Damage dealt cuts the target's healing by 30% (15% on tanks) for 3 s**, global since BZ 2026-02-10 (W Global Passive's
  box flags it "removed": see Disagreements) **M**.

## Perks (minor at perk level 2, major at 3)
- **Perk XP 2110 to the minor, 5275 to the major** (D _; W rev 265621, 2025-09-16) **M**. BZ never changed hers (no Juno line
  in 2026-04-14's perk cost changes). XP ramps from minute 3 to x2 at minute 7 (W Perks) **M**.
- Minor, **Familiar Vitals** (BZ 2025-02-18): "Pulsar Torpedoes lock onto allies 35% faster." W "0.35 - 0.7 seconds" **H/L**
  (the 35% is H; W's range predates BZ 2026-08-11's 0.35 s minimum: 0.35 / 1.35 = 0.26 s, 1 / 1.35 = 0.74 s by arithmetic).
- Minor, **Locked On** (BZ 2025-04-22 major 1.5 s; 2025-08-26 minor, 1 s): "Reduce the cooldown of Pulsar Torpedoes by 1
  second for each enemy hit." **-1 s per enemy, at most 5 s** (half the 10 s) (D _; W rev 406811) **H/M**.
- Major, **Faster Blaster** (BZ 2025-08-26): "While using Glide Boost, Mediblaster fires continuously." **22 rounds/s**, one
  at a time, 1 ammo each (165 DPS / 132 HPS while firing) (D _; BZ 2026-06-16 20 -> 22; W) **H/M**.
- Major, **Lift Off** (BZ 2025-08-26): "Martian Overboots can now triple jump." (D _; W) **H**.
- Removed: **Hyper Boost** (minor: glide through Hyper Ring pushes her forward, BZ 2025-02-18 -> 04-22), **Re-Boots**
  (Glide Boost resets the double jump; +25% glide 2025-03-18 -> 04-22; removed 2025-08-26), **Master Blaster** (crits,
  removed 2025-08-26, base kit since 2025-12-09).

## Controls and hero options (W)
Primary fire: Mediblaster (fires Pulsar Torpedoes while locking). Secondary fire: Pulsar Torpedoes. Ability 1: Glide Boost.
Ability 2: Hyper Ring. Ultimate: Orbital Ray. Jump: Martian Overboots (double jump, hold to hover). Reload. Quick melee.
Options and defaults: Toggle Pulsar Torpedoes **on**; Toggle Glide Boost **on**; Toggle Martian Overboots Hover **off**;
Swap Pulsar Torpedoes toggle button inputs **off**; Crouch activates Martian Overboots Hover **off**; Hide Glide Boost cancel
text **off**. Default key letters: not in these sources.

## Lore and voice
**Juno Teo Minh**, 19, Martian (born on Mars, Project Red Promise; born March 22), medic and navigator, Overwatch agent;
English voice **Xanthe Huynh**; released **2024-08-20** (Season 12), the 41st hero (W; BZ 2024-08-20).

## For the Minecraft port to decide (no Minecraft equivalent)
- **Shields as their own pool**: 75 health + 150 shields (225 = 18 MC health at 250 = 20: 6 + 12), regenerating 30/s after
  3 s; MC has no regenerating shield HP.
- **Air jumps and hover on the jump key**: a second (third with Lift Off) jump in the air at 10.8 m/s (0.54 blocks/tick), and
  holding jump capping the fall at 3 m/s (0.15 blocks/tick); MC's jump key in the air does nothing (and double-tap is creative
  flight). Fall damage: none in Overwatch; the owner's rule (no fall damage when an ability lands her, #35) covers Glide Boost,
  but her safe height for plain falls is ours to set.
- **Glide Boost**: gravity off for 4 s, horizontal only, a 19.1 m/s dash then 7.7 m/s; "gliding along a slope or wall to climb"
  needs our own collision rule (MC's step height is 0.6 block); the reduced air acceleration has no number.
- **Sub-tick fire**: the burst is 34 rounds/s (12 in ~0.35 s) and Faster Blaster 22/s, both faster than 20 ticks/s: rounds
  must be batched per tick. Falloff 25-35 m is far beyond MC's usual reach.
- **"Heroes only"** targeting (Pulsar lock-on, Hyper Ring): which mobs count (hostile mobs? players, tamed pets, villagers as
  allies, as for the other supports). How many locks at once: ours (no source gives a cap).
- **Orbital Ray's +30% damage dealt** (MC's Strength is a flat +3, not a percent) and its column through blocks.
- **Hyper Ring** as a placed ring entity allies run through, upright, 4 m radius, 6 s; MC speed effects are per-level, not
  +50% for 3 s, and the +75% stacking cap is ours to enforce.
- Survivor: the regeneration timer has to be ours (MC's natural regeneration is food-based).

## Not given by any source
How many targets Pulsar Torpedoes can lock; the torpedoes' speed, turn rate and size; the double jump's height and whether it
sets or adds to her vertical speed; any hover time limit (none listed); Lift Off's third jump strength; Glide Boost's dash
length/duration and the air acceleration in numbers; Hyper Ring's projectile speed and the ring's orientation rule; Orbital
Ray's height and whether 20 m (W range) or 18 m (2.25 x 8) is its travel; her head and leg hit volumes and the units; whether
the double jump triggers Survivor; which Minecraft mobs would count as "heroes" or "tanks" (ours).

## Disagreements between the sources
- **Mediblaster reload:** W 1.5 s since an unsourced 2025-08-30 edit (rev 264801), 1.8 s before; no BZ line. D settles it.
- **Glide Boost's dash:** W +247% / 19.1 m/s since 2025-10-28 ("reduced at some point", no BZ line); before, +390% / 40 m/s
  (which doesn't even fit 5.5 m/s: +390% = 26.95 m/s). D settles it.
- **Hyper Ring and the speed cap:** W's field says it ignores the +75% cap, W's own text says it stacks up to it. With Glide
  Boost's +40% that is 9.625 m/s (capped) or 13.75 m/s (not). D settles it.
- **Ult cost:** W 2900 (from the advanced info panel, 2025-10-15) after 2600 -> 2850/2860 -> 3135 -> 2885 edits; BZ gives only
  percentages, which lead to 2894. The 2600 base is W's trial-era figure.
- **Familiar Vitals:** W 0.35 - 0.7 s predates the 2026-08-11 lock-on change; 35% faster gives about 0.26 - 0.74 s now.
- **Orbital Ray's reach:** W range 20 m vs W's own "travels up to 18 meters" (2.25 m/s x 8 s).
- **Hyper Ring duration in 6v6:** BZ 2024-12-17 (6v6 Experiment) cut it to 4.5 s; W says it went back to 6 s on 2025-03-18
  "undocumented". Not live 5v5 either way.
- **Healing reduction:** W's Global Passive box flags it "removed" while BZ treats it as live (as for Ana, Brigitte and Sombra).
