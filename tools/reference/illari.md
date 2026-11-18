# Illari in live Overwatch: numbers and sources (research, 2026-11-14)

**BZ** = Blizzard's live patch notes (`https://overwatch.blizzard.com/en-us/news/patch-notes/live/YYYY/MM/`, every month 2024-01 to
2026-11 read 2026-11-14 by plain HTTP fetch, parsed to text; 2023 history comes from the wiki's log only).
**The last live number change to her is BZ 2026-11-03** (Solar Rifle max charge time 0.65 -> 0.575 s, Healing Pylon heal per
projectile 40 -> 50); before that BZ 2026-10-06 (hit volumes, global hitscan size 0.07 -> 0.04 m), BZ 2026-06-16 (Rapid Construction).
**W** = the Overwatch wiki on Weird Gloop through its MediaWiki API (`overwatch.weirdgloop.org/api.php`; the Fandom API answers 402):
Illari rev **408912** (2026-11-04T05:38Z, read 2026-11-14; its log has the 2026-11-03 patch), Sub-Roles 406777, Global Passive
Abilities 408982, Melee 402309, Movement speed 410920, Roles 401659 (all read 2026-11-14). All her abilities are on the hero page
(no separate pages used). Requests carried a generic user agent and no personal data; the raw pages stayed in the scratchpad.

Confidence: **H** BZ and W agree; **M** one source (usually W alone); **L** sources disagree, are stale, or derived.

**Her kit text now matches the one in the game data (Oct 2026)**: Solar Rifle "Long range auto-charging rifle", Secondary Fire
"Medium range healing beam that consumes solar energy", Outburst, Healing Pylon, Captive Sun, Survivor, and the four perks. Her launch
text (BZ 2023-08-10, via W's log) is the same wording.

## Your two questions
- **Sunstruck is not a passive.** It is the name of Captive Sun's mark effect only: a hit enemy is marked ("Sunstruck" duration 6 s);
  once the mark has taken 90 damage (or the target dies) it becomes SUNSTRUCK and explodes 0.8 s later (W; BZ 2024-01 "Sunstruck
  effect wouldn't detonate if the damage threshold was reached in the last 0.6 seconds"; BZ 2024-02-21; BZ 2023-08-24 via W
  "Sunstruck duration decreased from 7 to 6 seconds") **H**. Her passive slot holds only the Survivor sub-role passive (W).
  The only other "passive" ever named is April Fools: BZ 2024-04 (Solar Installed / "Solar Powered New Passive", a joke after
  Captive Sun), not live.
- **The charged healing shot never shipped.** At launch (BZ 2023-08-10) the secondary was already the healing beam. W's Development
  section: the first prototype secondary was a piercing burst heal with a long cooldown (like Sojourn's railgun), changed to a
  single-target shot with a shorter cooldown, then "the rifle's current secondary was settled upon" **M** (single source, developer
  talk, dates not given: pre-release only). No live patch replaced a shot by the beam.

## Left out (not the live game)
**April Fools** (BZ 2024-04 "Solar Installed" +20 % speed and 40 % faster charge; BZ 2025-04 primary heals and secondary damages,
range +50 %; BZ 2026-04-01 pylon reflects shots to enemies / allies, build 0.5 s, always 7 s cooldown), **Community Crafted**
(BZ 2026-06-30 notes: Sunburn "prevents Outburst from healing allies", Outburst 75 healing and 2 extra spins, Captive Sun hinders
enemies aiming at her; arcade only), **Stadium** (none for her in range), **6v6** (W's 6v6 tab: 2024-12-17 and 2025-03-18 entries
below), Mystery Heroes, PvE, console aim assist (BZ 2025-12 aim-assist notes name Ana, Juno, Illari).

## Patches that touched her (dates are the patch notes' titles; BZ and W agree unless noted)
2023-08-10 added (S6). 2023-08-24 pylon heal 40 -> 30, shields 75 -> 50; Captive Sun now blocked by barriers, size 1.5 -> 0.75 m,
Sunstruck 7 -> 6 s (W log). 2023-10-10 beam 120 -> 105 HPS. 2023-10-31 rifle size 0.1 -> 0.05 m; pylon base health 75 -> 50 (total
100), cooldown when destroyed 12 -> 15. 2024-01-09 charge no longer paused by secondary or Captive Sun; ammo 14 -> 16; Captive Sun
refills the beam energy and clears overheat; Sunstruck falloff removed. **2024-02-13 (S9)**: health 200 -> 250; global hitscan size
+0.08 m; ult costs +10 %; global regeneration (20 HP/s after 5 s then; Support delay halved); quick melee 30 -> 40. 2024-02-21
pylon cooldown 8 -> 6 s, "when destroyed by enemies" 15 -> 12 s; Sunstruck 100 -> 160 with falloff -25 % again. 2024-03-12 recovery
0.3 -> 0.2 s, max charge 0.7 -> 0.65 s. 2024-04-16 recovery 0.2 -> 0.25 s, beam 105 -> 115 HPS. 2024-06-20 Outburst damage 10 -> 25;
pylon heal 30 -> 40, half effective on herself, health 100 -> 125. 2024-07-09 allies no longer see the "Destroyed" alert.
2024-07-12 pylon cooldown 12 -> 14 s; Captive Sun cost +12 %. 2024-08-20 ammo 16 -> 14; Captive Sun environment collision 0.15 -> 0 m;
fix: beam hitting the environment near edges. 2024-08-23 rifle damage 75/25 -> 70/20 (W date; BZ 2024-08 lists it). 2024-12-10 hitscan
large 0.08 -> 0.07 m; Captive Sun radius 8 -> 10 m. **2024-12-19 (BZ title; W logs it 12-17 under 6v6 only)** rifle 70/20 -> 75/25,
pylon cooldown 14 -> 12 s. 2025-01-21 beam max energy 3 -> 3.5 s. **2025-02-18 (S15) perks**: Rapid Construction, Summer Solstice,
Solar Power, Sunburn (70). 2025-03-18 charging no longer paused by quick melee; Summer Solstice also +30 % attack speed; W's 6v6 tab
also logs "undocumented" 75/25 -> 70/20 and pylon 12 -> 14 s on this date. 2025-05-20 beam range 15 -> 20 m (BZ 2025-05 also fixed
Captive Sun not exploding after a deflect). 2025-08-26 Solar Power removed; Solar Flare "consume all energy, up to 120". 2025-10-14
Solar Flare cost 100 -> 50 %, flat 100 heal. 2025-12-09 Captive Sun size 0.75 -> 0.45 m. 2026-01-08 rifle 70/20 -> 75/25 (W: the
nerf "was reverted in both formats"). **2026-02-10 (Season 1: Conquest)**: Support role passive removed, sub-role Survivor,
beam regen 43 -> 35, Outburst 7 -> 8 s, Solar Flare no cost and 10 s cooldown. 2026-03-10 Summer Solstice 30 -> 20 %, Sunburn
70 -> 50, beam max 3.5 -> 3 s. 2026-05-12 rifle base size 0.05 -> 0 m (global bonus 0.07 stays). 2026-06-16 Rapid Construction
-1.5 -> -2 s. **2026-10-06** head hit volume 14.9 -> 18, torso 20 -> 23.5; global hitscan large 0.07 -> 0.04 m. **2026-11-03** max
charge 0.65 -> 0.575 s, pylon heal per projectile 40 -> 50. Nothing about her in BZ 2024-05, -09, -10, -11, 2025-06, -07, -09, -11,
2026-07, -08, -09 except the fixes noted.

## Base
- **Health 250**, no armor, no shields (BZ 2024-02-13 200 -> 250; W infobox 250) **H**. Role Support, sub-role **Survivor** (BZ
  2026-02-10 list "Brigitte, Illari, Juno, Mizuki, Wuyang"; W adds Doctrine and Wuyang) **H**.
- **Move speed 5.5 m/s** (global default; W Movement speed has no walking entry for her, only Captive Sun flight) **M**.
- Quick melee **40**, 1 s cooldown, 2.5 m, active 0.128 s (BZ 2024-02-13 30 -> 40; W Melee) **H**.
- **Ult cost 2975 points** (W Captive Sun); BZ gives only the +10 % (2024-02-13) and +12 % (2024-07-12) steps, no absolute number
  **M**.
- Perk XP: **minor 2355, major 5890** (W infobox) **M**.
- Hit volumes (BZ 2026-10-06, game units): head **18**, torso **23.5**; legs and arms: no source.
- Crits: **primary and only the Solar Rifle** (headshot x1.5, W "lesser critical") **M**. Beam, Outburst, pylon, Captive Sun: no
  headshot (W).

## Passives
- **Survivor** (sub-role): "Movement abilities start passive health regeneration." Using a movement ability (Outburst) triggers the
  global regeneration at once, for at least **0.25 s** if she is damaged afterwards (BZ 2026-02-10 text; W "0.25 seconds (minimum)")
  **H**. Whether Captive Sun's flight counts: no source (W Movement speed lists it as movement; the Survivor page names no ability).
- **Global out-of-combat regeneration**: after **6 s** without damage, **10 HP/s + 5 % of max HP/s** = **22.5 HP/s** for her
  (BZ 2026-02-10 "Global regeneration after 6 seconds remains unchanged"; BZ 2024-05-14 10 + 5 %; W Global Passive) **H**.
- **Support role passive: removed** BZ 2026-02-10 ("Support Role Passive: Removed"). It was "regenerate sooner" (3 s delay,
  2.5 s per BZ 2024-02-13 history); no longer applies **H**.
- **Healing cut on damage**: every damage dealt reduces the target's healing received **-30 %** (**-15 %** vs tanks) for **3 s**
  (BZ 2026-02-10; W) **H**.

## Solar Rifle, primary fire (hitscan, auto-charging)
- **Damage 25 (min charge) to 75 (full charge)**, linear with charge assumed **L**; **falloff from 30 m to 50 m down to 30 %**
  (75 -> 22.5, 25 -> 7.5) (BZ 2026-01-08 75 / 25; W "75 - 22.5", "25 - 7.5", "30 - 50 meters") **H** for damage, **M** for falloff.
- **Headshot x1.5** (W) **M** (BZ 2023 launch note via the strategy text only: "reduced critical damage multiplier").
- **Full charge 0.575 s**, **recovery 0.25 s** after each shot: full shot every **0.825 s** (1.21/s); min-charge shots at 0.5 s
  (W says 0.25 s charge for the min shot, 2/s) (BZ 2026-11-03 0.65 -> 0.575; BZ 2024-04-16 recovery 0.25; W) **H**. The charge starts
  on its own and **continues while using secondary fire, quick melee, abilities, Captive Sun** (BZ 2024-01-09, 2025-03-18; W) **H**.
- **Ammo 14, reload 1.5 s** (BZ 2024-08-20 16 -> 14; W reload 1.5) **H** for ammo, **M** for reload.
- **Projectile size 0.04 m** = 0 base (BZ 2026-05-12) + 0.04 global large hitscan (BZ 2026-10-06; W "0 base + 0.04 global") **H**.
  No spread (W).
- W DPS: 75 / 0.825 = **90.9 while firing**; with reload 14 shots / (11.55 + 1.5 s) **M**, W's table still shows the 0.65 s value;
  recompute ours **L**.
- Perk Summer Solstice's attack speed applies to charge and fire rate (W lists 93.24 DPS firing at max charge during flight) **M**.

## Solar Rifle, secondary fire (healing beam, hitscan beam)
- **Heal 115 per second** (BZ 2024-04-16 105 -> 115; W "1.84 every 0.016 s") **H**. Cannot headshot. Ticks at 62.5 Hz.
- **Range 20 m** (BZ 2025-05-20 15 -> 20; W) **H**. Beam radius 0.25 m (W) **M**. Ignores Matrix, Deflect, windows; can break fences
  and breakables (W) **M**.
- **Energy ("solar energy") 3 s full pool** (BZ 2026-03-10 3.5 -> 3 s; W "33.3 % per second cost") **H**. **Regeneration 35 %/s**
  (BZ 2026-02-10 "Resource regeneration reduced from 43 to 35"; W 35 %) **H**. Regen starts **0.6 s** after release (W) **M**.
  Emptied completely: an extra **0.4 s** before regen and the beam stays locked until full again (W) **M**.
- Prose on the wiki page ("2.3 s to recharge to full") is the old 43 %/s figure (100 / 43 = 2.3 s); at 35 %/s a full recharge is
  about **2.86 s** (derived) **L**. W's beam range and cost text are otherwise current.
- **Captive Sun refills the energy fully and clears the overheat** at launch (BZ 2024-01-09; W) **H**.
- Using primary fire or reloading does not interrupt the recharge; quick melee no longer pauses rifle charging (BZ 2025-03-18) **H**.
- Healing tickrate rises with Kitsune Rush; energy use does not (W) **M**.

## Outburst (ability 1, Shift)
- **Cooldown 8 s** (BZ 2026-02-10 7 -> 8) **H**. **Damage 25** (BZ 2024-06-20 10 -> 25) **H** (it hits enemies in range).
- **Knockback radius / effect radius 8 m**, knockback **18.7 m/s (H 18, V 5)** (W) **M**. Ignores shields (W strategy) **M**.
- **Launch speed** (W): **18.4 m/s** with horizontal input and no charge, **16.7 m/s** with input held (charged = higher), **6 m/s**
  standing and no charge, **13.5 m/s** standing and charged **M**. "Hold jump to go higher": holding the ability key or Jump while
  casting charges it (hero option "Hold Outburst to go higher", default on) **M**; the charge time itself: no source.
- Cast **0.208 s startup + 0.880 s recovery** (W) **M**. Does not interrupt the rifle's charge (W) **M**. Triggers Survivor
  regeneration (above).
- Fall damage does not exist in Overwatch; no source for any fall rule.

## Healing Pylon (ability 2, E)
- **Heal 50 per projectile** (BZ 2026-11-03 40 -> 50; 2024-06-20 30 -> 40; 2023-08-24 40 -> 30) **H**. One projectile every **0.8 s**
  (W "1.25 shots/s"): **62.5 HPS** to allies (W "Heal per second: 62.5") **M**. **Half to Illari herself** (BZ 2024-06-20): W's table
  says 20 over 0.45 s for self, stale from when the heal was 40; expected **25 per projectile** (31.25 HPS, which W's detail line
  also gives) **L**.
- **Health 75 + shields 50 = 125 total** (BZ 2024-06-20 100 -> 125; BZ 2023-08-24 shields 75 -> 50; BZ 2023-10-31 base 75 -> 50 then
  back via 2024-06-20; W 75 + 50) **H**.
- **Range 15 m** (W) **M**. Healing projectile speed **25 m/s**, travel-time, can be blocked by barriers, Matrix and others, and it is
  not hitscan (W) **M**. It prioritises the ally with the lowest health percentage in range (W) **M**. Pylon cannot be targeted by
  Visor, Deadeye, Biotic Orb (W).
- **Build / deploy time 1.3 s** before it starts to heal (Rapid Construction: 0.33 s) (W; BZ 2025-02-18 "builds 300 % faster") **M**.
- **Cooldown: 14 s, redeploy 7 s** (W; perk makes 12 / 5) **L**: the BZ history is 8 -> 6 s and "when destroyed by enemies" 15 -> 12
  s (BZ 2024-02-21), then 12 -> 14 s (BZ 2024-07-12), then 14 -> 12 s (BZ 2024-12-19), then an undocumented 12 -> 14 s on 2025-03-18
  (W's 6v6 log; the nerf of that date to damage was reverted 2026-01-08, the pylon change is not noted as reverted). Best reading:
  **14 s after it is destroyed by an enemy, 7 s when she removes it herself (Interact) after it was undamaged for 3 s, and no new
  pylon for 3 s after it is hit** (W text). The cooldown starts at activation (W). The BZ-only fact: base cooldown 6 s at 2024-02-21.
- **Placement**: thrown arc projectile **25 m/s**, cast **0.08 s + 0.816 s recovery**, it sticks to terrain and terrain-like objects
  (also payload; falls if its support goes), bounces off barriers, other deployables and fences (W) **M**. **Max range: no number**
  (the thrown arc at 25 m/s with unknown gravity). **Only one**: a new pylon destroys the old one (W) **H**. Unaffected by knockback;
  can be hacked by Sombra for 10 s (W) **M**. Sunburn / other details above.
- Hero option "Interact cancels Healing Pylon": Interact destroys the active pylon and the ability key redeploys immediately (default
  on); off = the ability key destroys it and must be pressed again (W) **M**.

## Captive Sun (ultimate, Q)
- **Cost 2975 points** (W) **M**.
- **Cast 1.2 s launch** (rising, flight starts) then the ball fires (W "0 + 1.2 s recovery" for the launch, then 0.016 s startup +
  0.592 s recovery for the ball) **M**. It is channeled: stun, knockdown or hack interrupts and spends the charge (W) **M**.
- **Flight 5.2 s** (Summer Solstice +3 s = 8.2 s) (W; BZ 2025-02-18 +3 s) **H**. **Launch speed 12 m/s, flight speed +45 %**
  (7.98 m/s extra, i.e. ~+45 % of 5.5 m/s; with Summer Solstice +65 %) (W; BZ 2025-02-18 "+20 % flight speed") **M**. Also affects
  vertical movement and ignores the backpedal penalty (W Movement speed) **M**. Rooted or hindered means she cannot fly for that time
  but the ability keeps running (W). **Knockback resistance 50 % in flight** (W) **M**.
- **The ball**: next primary fire fires it; automatic after **4 s** from launch; **projectile 30 m/s**, **radius 0.45 m** (BZ
  2025-12-09 0.75 -> 0.45; 2023-08-24 1.5 -> 0.75; BZ 2024-08-20 environment collision 0.15 -> 0 m, so it hits terrain only when its
  centre does) **H**. It **passes through enemies** and **explodes on a shield, terrain or after 10 s** (W) **M**. Barriers block it and
  its explosion since 2023-08-24 (W log). It can be eaten by Defense Matrix, Kinetic Grasp and the like and is reflected by Deflect;
  the splash and the Sunstruck explosion are not (BZ 2025-05 fixed it not exploding after a deflect; W) **M**.
- **Damage**: **5 direct, 50 splash** (W) **M**. **Impact radius 10 m** (BZ 2024-12-10 8 -> 10) **H**. Falloff: no source.
  Total on a direct hit assumed 55 (splash and direct both apply: W wording "damaged by the direct hit or splash") **L**.
- **Mark and slow**: every enemy damaged by the direct hit or splash is marked and slowed for **6 s** (BZ 2023-08-24 7 -> 6; W);
  **slow -40 % (3.3 m/s on 5.5)** decaying to 0 % at the end (W) **M**. Can be cleansed.
- **Sunstruck threshold 90**: the mark needs **90 damage after reductions** (or a kill) to become SUNSTRUCK; damage from the ball's own
  direct hit and splash does not count, another Illari's Sunstruck explosion does (W) **M**. One mark at a time; a second hit
  refreshes the duration; none is applied during the 0.8 s delay (W) **M**.
- **Sunstruck explosion 160 at the centre, -25 % at the edge (120)**; delay **0.8 s**; radius **1 m (full) to 5 m (falloff max)**
  (BZ 2024-02-13 / -21 160 with falloff -25 %; BZ 2024-01-09 removed the falloff, 2024-02-21 returned it; W "160 - 120",
  "1 - 5 meters", "0.8 seconds") **H** for damage and falloff, **M** for the radii and delay.
  Not affected by Amplification Matrix; ball direct damage and splash are (W) **M**. Cleanse before the threshold removes the mark;
  after SUNSTRUCK the explosion still happens (W).
- **Fix BZ 2024-04-16**: Captive Sun affecting targets through floors and ceilings.

## Perks (BZ 2025-02-18 on; W)
- **Minor: Rapid Construction**: "Healing Pylon builds 300% faster and its cooldown is reduced by 2 seconds." Build 1.3 -> 0.33 s,
  cooldown 14 -> 12 s, redeploy 7 -> 5 s (BZ 2026-06-16 1.5 -> 2 s; W) **H**.
- **Minor: Summer Solstice**: "Activating Captive Sun grants Illari 3 extra seconds of flight, 20% increased flight speed, and 20%
  increased attack speed." (BZ 2025-02-18, 2025-03-18 +30 % attack speed, 2026-03-10 30 -> 20 %; W) **H**. Whether "attack speed"
  speeds up the charge or only the fire rate: W's DPS lines imply both **L**.
- **Major: Solar Flare**: "Press Primary Fire while using Solar Rifle's healing beam to heal all allies in front of Illari for 100
  healing." **100 healing, 10 s cooldown, no energy cost**; cast 0.4 s startup + 0.4 s recovery; **20 m radius, 40 degree horizontal
  view cone, +-3 m height**; works while any energy is left (BZ 2025-08-26 "up to 120", 2025-10-14 flat 100 and 50 % cost, BZ
  2026-02-10 no cost and a 10 s cooldown; W) **H** for the 100, 10 s and no cost; **M** for the cone. Starting it starts the beam's
  energy regen delay (W) **M**. Ignores Matrix, Deflect, windows, boost (W).
- **Major: Sunburn**: "Outburst ignites enemies, dealing an additional 50 damage over 3 seconds." **50 total (16.67/s), ignores armor,
  cleansable, a damage-over-time effect** (BZ 2025-02-18 70, 2026-03-10 70 -> 50; W) **H**.
- Removed: **Solar Power** (major, 2025-02-18 to 2025-08-26: fully charged rifle hits grant 12.5 % of max energy, overfilling to 50 %).
  W "Removed in Season 18" (date mismatch; BZ 2025-08-26 is the removal) **L**.

## Hero options (W)
Toggle Healing Beam (hold vs toggle, default hold); Hold Outburst to go higher (default on); Solar Rifle UI charge element (default
on); relative aim sensitivity during the beam and during Captive Sun (default 100 %); Interact cancels Healing Pylon (default on);
Rise during Captive Sun with Ability 3 (default off).

## Gaps and disagreements
1. **Pylon cooldown 14 vs 7 vs 12 vs 6:** see above; BZ never states the live base cooldown after 2024-12-19 (12 s), W says 14 s from
   an undocumented 2025-03-18 change. Ours should use W's rule and flag it.
2. **Beam regeneration time:** W's prose (2.3 s) is the 43 %/s figure; 35 %/s means about 2.86 s.
3. **Pylon self heal:** W's 20 is stale; BZ 2024-06-20 says half, so 25 per projectile.
4. **Min-charge damage and the charge curve:** only the two endpoints (25 and 75) are sourced; W's DPS note uses a 0.25 s charge for
   the minimum shot. Linear interpolation is ours.
5. **Outburst:** no cast-charge time, hitbox shape or the actual jump height; launch speeds are W's.
6. **Captive Sun:** no falloff for the impact splash, no number for the flight vertical speed, and "attack speed" in Summer Solstice
   is not defined. The 90-damage threshold and 160 / 120 explosion are the best-sourced.
7. **Pylon max placement range and projectile gravity:** no source.
8. **W's Solar Power removal date (Season 18)** disagrees with BZ 2025-08-26.
9. **Ult cost 2975, perk XP 2355 / 5890, W only**; a game-data pass would settle them.
