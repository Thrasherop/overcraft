# Tracer in live Overwatch: numbers and sources (research, 2026-11-11)

**BZ** = Blizzard's live patch notes (`https://overwatch.blizzard.com/en-us/news/patch-notes/live/YYYY/MM/`, every month 2024-01 to
2026-11 read 2026-11-11; 2026-11 holds 11-03 and 11-05, neither touches Tracer or a global number she uses: **the last live number
change to her is BZ 2026-08-11** (Recall 13 -> 12 s, Chronal Dash out, Temporal Regen in); the last live change that names her at all
is BZ 2026-10-06 (hit volumes, and the global hitscan sizes)). **W** = the Overwatch wiki on Weird Gloop through its MediaWiki API
(`overwatch.weirdgloop.org/api.php`, read 2026-11-11): Tracer rev **410824** (timestamp 2026-11-12T00:58Z, i.e. after the date we
read it; Tracer/Stadium is transcluded for the Stadium tab only), Sub-Roles 406777, Roles 401659, Global Passive Abilities 408982,
Melee 402309, Movement speed 402311, Ultimate ability 395578, Critical hit 404694, Perks 354523. A "Damage falloff" page does not
exist on W. Her page carries a dated balance log back to 2018 that agrees with BZ except where noted in Gaps. **No game-data (D)
pass yet** for Tracer: every number here is public. Requests carried a generic user agent and no personal data; the raw pages stayed
in the scratchpad.

Confidence: **H** BZ and W agree (or W gives it and BZ's history leads to it); **M** one source (usually W alone); **L** sources
disagree, or derived.

Left out (not the live game): **Stadium** (W Tracer/Stadium; BZ 2025-09-16 on, including 2025-10-01 and 2026-06-16 Tracer powers /
Pulse Pistols 5.25: Stadium only), **Community Crafted** (BZ 2026-06-30: Ult Packs, Recall shared with allies, Pulse Bomb 3 s fuse /
7 m radius / 9999 to the stuck target / can be shot off), **Quick Play Hacked** (BZ 2026-07-30: healing reduction 15 %, Flanker packs
+50), April Fools (BZ 2024-04-01 Recall to past/future, 2025-04-01 Blink charges into enemies, 2026-04-01 Blink 100 m), 6v6
Experiment (BZ 2024-12-17 Pulse Pistols falloff 10 -> 12 m; W 6v6 tab), PvE/co-op (W owcoop: 60 ammo, 5 damage, bomb 550), console
aim-assist (BZ 2025-12 hotfix, 2026-02).

**Patches that touched her (live, 2024-2026)** (dates are the patch notes' titles; W agrees unless noted): **2024-02-13 (S9)** health
150 -> 175; global hitscan +0.05 m (Pulse Pistols) and slow-projectile +0.15 m (Pulse Bomb); ult costs +10 %; global regeneration
20 HP/s after 5 s; Damage role passive 20 % / 2 s; quick melee 30 -> 40; **2024-02-21** Pulse Pistols min falloff range 12 -> 10 m;
2024-03-08 role passive 20 -> 15 %; **2024-04-16** Recall 12 -> 13 s, Pulse Bomb base size 0.2 -> 0.1 m (total 0.25); 2024-04-30 role
passive 15 -> 20 %; 2024-05-14 regeneration 20 HP/s -> 10 HP/s + 5 % max; 2024-07-05 role passive half as strong on tanks;
**2024-08-23** Pulse Pistols damage 6 -> 5.5; **2024-11-12** Recall 13 -> 12 s (notes section dated 11-08); **2024-12-10** hitscan
small 0.05 -> 0.04 m, large projectile 0.15 -> 0.12 m, role passive 25 % / 3 s; **2025-01-21** spread -15 %; **2025-02-18 (S15)
perks** (Blink Packs, Blast from the Past, Flashback, Quantum Entanglement); **2025-03-18** Quantum Entanglement overhealth 3.5 -> 6 s;
**2025-04-22** Flashback 3 -> 2 Blinks, Quantum Entanglement +20 ammo (W dates this 03-18; BZ 04-22); **2025-06-24** Pulse Pistols
damage 5.5 -> 6, role passive 25 -> 30 %, regeneration delay 5 -> 6 s; **2025-07-22** spread 3.0 -> 3.5 deg; **2025-08-26** (notes
titled 08-27) Recall 12 -> 13 s, Blast from the Past out, Kinetic Reload in (20 ammo); 2025-10-14 Flashback reworked (+150 % Blink
recovery for 3 s); **2025-11-11** Pulse Pistols lose the global hitscan size; **2026-02-10 (Season 1)** sub-role **Flanker**, Damage
role passive removed (its healing cut made global), Pulse Pistols 6 -> 5.75 and the global size back, Chronal Dash +20 % Blink
distance, Flashback out, Blink Packs minor -> major; 2026-04-14 perk costs only; 2026-06-18 Kinetic Reload 20 -> 12 ammo;
**2026-08-11** Recall 13 -> 12 s, Chronal Dash out, Temporal Regen in, Flanker pack bonus 75 -> 50; **2026-10-06** global hitscan
sizes down (large 0.07 -> 0.04, small 0.04 -> 0.02 m), Tracer torso hit volume 22.4 -> 24, legs 22.4 -> 20.

## Base
- **Health 175**, no armor, no shields (BZ 2024-02-13 150 -> 175; W 175) **H**. The lowest base health in the game (W strategy).
- **Damage, sub-role Flanker** (BZ 2026-02-10 list: Anran, Genji, Reaper, Tracer, Vendetta, Venture; W Sub-Roles adds Shion) **H**.
- **Move speed 6.0 m/s** forward / sideways (W Movement speed: Tracer, Genji, Anran 6, everyone else 5.5; W Sub-Roles attributes it to
  the Flanker passive); backwards 90 % = 5.4, diagonal-back 93.33 % = 5.6; crouched 3 m/s (all heroes). **No BZ line gives it**: M
  (Gap 2). Jump impulse 5.72 m/s, 0.98 m peak, the same for all heroes (W).
- Quick melee **40**, 1 s cooldown, 2.5 m, active 0.128 s, 0.528 s recovery, slight knockback, pierces barriers (BZ 2024-02-13
  30 -> 40; W Melee) **H**.
- Perk XP: **minor 1575, major 3995** (W infobox) **M**.
- Crits: Pulse Pistols **can headshot**, 2.0x (W Pulse Pistols "headshot ✓"; W Critical hit: 2.0x default, Tracer not among the
  exceptions) **M**. Pulse Bomb cannot (W ✕).

## Role and sub-role passives
- **Damage role passive: none since BZ 2026-02-10** (removed). Its effect is now global: damage dealt cuts the target's healing
  received **-30 %** (**-15 %** on tanks) for **3 s**, no stacking, self-damage and objects as W lists (BZ 2026-02-10 -30 % / -15 %;
  BZ 2024-12-10 duration 2 -> 3 s, 2025-06-24 25 -> 30 %; W) **H** (W's global page marks it "removed": Gap 3).
- Global out-of-combat regeneration: after **6 s** without damage, **10 HP/s + 5 % of max HP/s** = 18.75 HP/s for her (BZ 2024-05-14
  rate, 2025-06-24 delay 5 -> 6 s, 2026-02-10 "6 s remains"; W Global Passive) **H**.
- **Flanker:** small health packs restore **+50** more (**125** instead of 75), large packs +50 (**300**; 250 base; irrelevant at
  175 max health) (BZ 2026-02-10 +75 -> BZ 2026-08-11 +50; W Sub-Roles 125 / 300) **H**. The "faster base speed" half of W's Flanker
  text is Gap 2. Her Blink Packs perk interacts: she can pick up a pack with full health if a Blink charge is missing (W).
- Ult charge: she **starts gaining charge right after casting**; the bomb's damage gives none (W Ultimate ability) **M**.

## Pulse Pistols (primary)
- **5.75 per bullet, two bullets per burst = 11.5 per burst**, **20 bursts/s** (1 per 0.05 s) = **40 bullets/s, 230 DPS** at full
  damage (BZ 2026-02-10 6 -> 5.75; W) **H**. History 6 / 5.5 / 6 / 5.75 above. Stadium's 5.25 does not apply.
- **Falloff 10 m -> 20 m, down to 30 % (1.725 per bullet, 3.45 per burst)** (BZ 2024-02-21 min range 12 -> 10 m; W infobox 10-20 m
  and 5.75 - 1.725; the 20 m end and the 30 % from W and BZ 2021 only) **M**. Curve shape: no source, linear assumed **L**.
- **Magazine 40**, **2 ammo per burst** (20 bursts, 1.0 s of fire) (W) **M**.
- **Reload 1 s**, ammo restored 0.686 s in (W tooltip; ambiguous like Pharah's) **M**.
- **Spread 3.5 deg max**, reached after 3 shots; **0.224 s recovery** if she stops firing (BZ 2025-07-22 3.0 -> 3.5, 2025-01-21 -15 %;
  W) **H** (the 3 shots / 0.224 s: M).
- **Hitscan, radius 0.02 m** (0 base + 0.02 global) (BZ 2026-10-06 small 0.04 -> 0.02; W 0.02) **H**. Between 2025-11-11 and
  2026-02-10 she had none (BZ), before that 0.04, before 2024-12-10 0.05.
- **Crit 2x on the head** (above). Hit volumes (BZ 2026-10-06, in game units, not meters): torso 24, legs 20; head and arms: no source.
- DPS with reloads: 120 (W).

## Blink (ability 1, Shift or secondary fire)
- **3 charges**, **3 s per charge** (cooldown 3 s) (W; no BZ change 2024-2026) **H**. Whether the 3 s timers run in parallel or in
  sequence: no source (Gap 4).
- **Distance 7.5 m**, instant: she appears at the destination, **0.096 s recovery** before the next Blink (W) **M**. Nothing in BZ
  2024-2026 changes it besides the removed Chronal Dash (+20 % = 9 m, BZ 2026-02-10 to 2026-08-11).
- **Direction: where she is moving**, not where she looks (W official description "teleport in the direction you are moving"; BZ
  2020-04-14 dev comments say she steers "towards the crosshair location" around corners: the two readings differ, Gap 5) **L**. Obstacle rules from BZ 2020-04-14 as W quotes them: she blinks around and over obstacles toward the crosshair, **cannot
  blink through a space she could not fit through, and cannot blink over an obstacle she could not jump over** with the jump
  starting from her height at the start **M**.
- Does not remove debuffs. Passes over a Steel Trap but cannot Blink out of one that has caught her (W) **M**.
- Standing still: no source (Gap 5).

## Recall (ability 2)
- **Rewinds 3 s**: position, facing, **health to the highest value in those 3 s** (not lower than her current), and **fully reloads**
  the Pulse Pistols (W) **M**. If the platform she stood on moved, she returns to its current spot, not the air (W).
- **Does not restore overhealth**; any overhealth she has now persists for its normal duration (W) **M**. Blink charges are not
  restored (Flashback is gone).
- **Phased and invulnerable for 0.9 s**, removes most negative status effects ("lesser cleanse"), **can be stunned before it starts**
  (W keywords) **M**.
- **Cooldown 12 s** (BZ 2026-08-11 13 -> 12; W) **H**. 3 s of rewind (W; BZ no change) **M**.
- Cast time before the vanish: no source (Gap 6).

## Pulse Bomb (ultimate, Q)
- **Cost 1375** (W; BZ 2024-02-13 +10 % globally, from a base no BZ page in range states) **M**.
- **Damage 350 at the centre falling to 70 at the edge**, plus **5 on sticking**; stick and blast are two instances, both reduced by
  armor (W; BZ 2019-08-13 300 -> 350) **M**. **Radius 2 m full -> 4 m min** (W; the Community Crafted note 4 -> 7 m confirms 4) **M**.
- **Self-damage 50 %: 175 at the centre -> 35** (W) **M**.
- **Fuse 1 s** from when it lands or sticks (W "explosion delay"; Community Crafted note 1 -> 3 s confirms 1) **M**/**H**.
- **Projectile 15 m/s, arc (gravity, rate unknown), radius 0.22 m** (0.1 base + 0.12 global) (BZ 2024-04-16 base 0.2 -> 0.1, 2024-12-10
  large 0.15 -> 0.12; W) **H** (15 m/s: M). **Cast 0.144 s throw + 0.192 s recovery** (W) **M**.
- **Sticks** to the first enemy hero it hits or else the first solid surface; hits an enemy for 5 and rides them (W) **M**. Can be
  destroyed **in the air** by Defense Matrix, Kinetic Grasp, Javelin Spin, Power Matrix, Kekkai Sanctuary, Trash Compactor and
  reflected by Deflect; **a stuck bomb and the explosion are not affected**; Amplification Matrix amplifies the stick and blast if the
  bomb passes through (W) **M**. Enemies cannot shoot it off in the live game (only Community Crafted).
- No crits. Hackable during the cast (W). Throw range: no source (Gap 7).

## Perks (BZ 2025-02-18 on; W)
- **Minor: Temporal Regen**: passive health regeneration starts **50 % faster** (BZ 2026-08-11; W's text says **100 % faster**, 6 -> 3
  s; Gap 1) **L**.
- **Minor: Kinetic Reload**: each enemy hero hit by quick melee grants **12 ammo** (BZ 2025-08-26 added at 20, 2026-06-18 20 -> 12; W)
  **H**.
- **Major: Blink Packs**: a health pack restores **1 Blink charge**; she can take a pack at full health if a charge is missing (BZ
  2025-02-18 as minor, 2026-02-10 moved to major; W) **H**.
- **Major: Quantum Entanglement**: Recall grants **50 overhealth and 20 ammo that decay over 6 s** (BZ 2025-02-18, 2025-03-18 3.5 -> 6 s,
  2025-04-22 +20 ammo; W) **H**. Decay curve: no source.
- Removed: Blast from the Past (2025-08-26), Flashback (2026-02-10), Chronal Dash (2026-08-11, was +20 %, 7.5 -> 9 m), Ult Packs
  (Community Crafted only).

## Hero options (W)
Blink UI (on / off): the charge indicator under the crosshair.

## Disagreements and gaps
1. **Temporal Regen:** BZ 2026-08-11 "50 % faster"; W "100 % faster", 6 s -> 3 s. 50 % would be 6 / 1.5 = 4 s. Ours: 3 s, the data (D #462 0.5 x 6 = W's 100 %); was "4 s (BZ)" before the data read.
2. **6.0 m/s:** W Movement speed and Sub-Roles say Tracer moves at 6; no BZ note states it (the Flanker lists in BZ 2026-02-10 and
   2026-08-11 name only health packs). A game-data pass would settle it. Ours: 6.0.
3. **Healing-reduction passive:** W's Global Passive page marks it "removed"; BZ never removes it and BZ 2026-07-30 (Hacked, not
   live) lowers it "from 30 % to 15 %". Ours: still on at 30 % / 15 % tanks, 3 s.
4. **Blink recharge:** one 3 s timer per charge, in sequence or in parallel: no source.
5. **Blink direction:** horizontal along move input per W; BZ 2020-04-14 says it steers to the crosshair location around corners and over
   ledges. Vertical aim, standing still (forward?), slopes: no source.
6. **Recall:** cast time before she vanishes; the exact health restored (highest in 3 s); whether the 0.9 s covers the whole animation.
7. **Pulse Bomb:** throw gravity / range, whether the fuse starts on stick or on landing for a moving target, falloff curve, behaviour
   on a moving ally, ult charge during the fuse.
8. **Pistol falloff:** the 20 m end and 30 % floor are W (and BZ 2021 for 12-20 m before the 2024 range change); curve shape unknown.
9. **Quantum Entanglement:** decay curves of overhealth and over-ammo; whether ammo above the magazine persists.
10. **Date mismatch:** W puts Flashback 3 -> 2 and QE +20 ammo on 2025-03-18; BZ has them 2025-04-22 (W's own other entry is the
    03-18 overhealth change).
11. **Ult cost 1375** and **perk XP** are W only; a game-data pass would settle both along with hit volume units.
