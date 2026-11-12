# Pharah in live Overwatch: numbers and sources (research, 2026-11-11)

**BZ** = Blizzard's live patch notes (`https://overwatch.blizzard.com/en-us/news/patch-notes/live/YYYY/MM/`, every month 2024-01 to
2026-11 read 2026-11-11; 2026-11 holds 11-03 and 11-05, neither touches Pharah or a global number she uses: **the last live change to
her is BZ 2026-08-11** (Concussive Blast 7 -> 8 s)). **W** = the Overwatch wiki on Weird Gloop through its MediaWiki API
(`overwatch.weirdgloop.org/api.php`, read 2026-11-11): Pharah rev **408633** (2026-10-31T08:05Z; Hover Jets, Jump Jet, Barrage, Rocket
Launcher all redirect to it), Sub-Roles 406777, Roles 401659, Global Passive Abilities 408982, Melee 402309, Movement speed 402311,
Status effect 409421 (Knockback), Ultimate ability 395578, Critical hit 404694, Perks 354523. The knockback / movement / ultimate pages
transclude her page, so they don't confirm it independently. **No game-data (D) pass yet** for Pharah: every number here is public.
Requests carried a generic user agent and no personal data; the raw pages stayed in the scratchpad.

Confidence: **H** BZ and W agree (or W gives it and BZ's history leads to it); **M** one source (usually W alone); **L** sources disagree,
or derived.

Left out (not the live game): **Stadium** (W Pharah/Stadium; BZ 2025-08-27 on), **Community Crafted** (BZ 2026-06-30: Concussive Blast
pulls and slows there), April Fools (BZ 2024-04 Jet Land, 2025-04-08 pulls, 2026-04-01 no Hover Jets), Halloween (BZ 2024-10-29
Scare-ial Superiority), 6v6 Experiment, PvE/co-op (W owcoop tab).

**Patches that touched her (live, 2024-2026):** **2024-02-13 (S9 rework)** health 200 -> 250; rockets recovery 0.85 -> 0.8 s, speed 35
-> 40 m/s, self-damage 50 -> 25 %; **Jet Dash added** (secondary fire, 8 s); Jump Jet restores 50 % fuel and can overfill, height -20 %,
cooldown 10 -> 14 s; Concussive Blast 9 -> 7 s, explosion 30 damage, radius 8 -> 6 m, knockback -10 %; Barrage refills fuel; Hover Jets
speed +20 -> +40 %, fuel recharges only on landing, usable with no fuel to slow the fall; global +0.15 m on slow projectiles, ult costs
+10 %, quick melee 30 -> 40; **2024-03-12** fast-projectile bonus 0.1 -> 0.075 (Barrage's rockets); **2024-07-09** Concussive explosion
30 -> 0, radius 6 -> 8 m, knockback +11 %; **2024-07-12** health 250 -> 225; **2024-12-10** Jet Dash 8 -> 9 s, global large projectile
bonus 0.15 -> 0.12 m; **2025-02-18 (S15) perks** (Drift Thrusters, Helix Shields 75, Concussive Implosion, Fuel Stores); 2025-02-20
Helix fix; **2025-03-25** Helix 75 -> 125; **2025-05-20** Drift Thrusters move -20 %, Implosion pull -50 %; **2025-06-24** Helix regen
from direct hits; **2025-08-26/27** Implosion out, Sky Spy in; 2025-08-29 Barrage splash fix; **2026-02-10 (Season 1)** sub-role
**Recon** (5 s reveal), Damage role passive removed (its healing cut made global), Sky Spy out, **Rocket Salvo** in (2 x 40); 2026-02-25
Salvo fix; 2026-03-10 fix (no Barrage movement while tethered by Mizuki); **2026-04-14** Recon 5 -> 3.5 s, Hover Jets +40 -> +30 %,
**Barrage can move**, Barrage explosion 30 -> 25, Drift Thrusters out, **Concussive Force** in (50); **2026-05-12** Concussive Force 50 ->
30, Rocket Salvo 40 -> 30; 2026-06-18 Fuel Stores fix (overfuel while fuel decaying); **2026-08-11** Concussive Blast 7 -> 8 s;
2026-10-06 hit-volume pass and hitscan sizes (Pharah not named; travel-time projectiles unchanged).

## Base
- **Health 225**, no armor, no shields (BZ 2024-07-12 250 -> 225; W 225) **H**.
- **Damage, sub-role Recon** (BZ 2026-02-10 list: Pharah, Freja, Echo, Sombra; Sombra left for Support/Tactician BZ 2026-10-06; W Recon
  now Echo, Freja, Pharah, Sierra) **H**.
- Move speed **5.5 m/s** (W Movement speed, the hero default; no BZ change) **M**.
- Quick melee **40**, 1 s cooldown, 2.5 m (BZ 2024-02-13 30 -> 40; W Melee) **H**.
- Perk XP: **minor 1730, major 4330** (W infobox) **M**.
- Crits: **none** on Rocket Launcher, Concussive Blast, Barrage (W "headshot ✕" on each) **M**.

## Role and sub-role passives
- **Damage role passive: none since BZ 2026-02-10** (removed). Its effect is now global for all heroes: damage dealt cuts the target's
  healing received **-30 %** (**-15 %** on tanks) for **3 s**, no stacking (BZ 2026-02-10 -30 % / -15 %; W Global Passive 3 s) **H**.
- Global out-of-combat regeneration: after **6 s** without damage, **10 HP/s + 5 % of max HP/s** (W Global Passive; BZ 2026-02-10 "6 s
  remains") **M** (the rate W only).
- **Recon:** damaging an enemy **below half health reveals them through walls** for **3.5 s**, timed from your last damage while they stay
  under half (BZ 2026-02-10 5 s, 2026-04-14 5 -> 3.5; W 3.5) **H**.

## Rocket Launcher (primary)
- **Direct 40 + splash 80 = 120** on a direct hit (BZ 2020-04-06 split; W) **H**.
- **Splash 80 -> 24.6** at the edge; **full within 0.8 m, falloff to 2.5 m** (W) **M**; linear shape assumed (W says "decreases with
  distance"; no source gives the curve) **L**. Knockback falls with it, minimum 0 (BZ 2016-11-15; W).
- **Self-damage 25 %**: 20 -> 6.15 (BZ 2024-02-13 50 -> 25 %; W) **H**. Self-knockback applies.
- **Projectile 40 m/s**, radius **0.32 m** (0.2 base + 0.12 global) (BZ 2024-02-13 speed, 2024-12-10 0.12; W) **H**. No spread, no
  falloff with range, no gravity (W) **M**.
- **0.8 s recovery = 1.25 shots/s** (BZ 2024-02-13; W) **H**. DPS 150 firing, 114 with reloads (W).
- **6 rockets**; **reload 1.5 s** animation, the ammo restored 0.6 s into it per W's tooltip (wording unclear, L) (W) **M**; empty
  reload starts 0.25 s sooner (BZ 2022-06-28 beta).
- **Knockback: 7.5 m/s max on enemies, 14.2 m/s max on herself** (W) **M**.
- Matrix / Deflect / Kinetic Grasp can eat or return the rocket, not the explosion (W).

## Jump Jet (ability 1, Shift)
- **A press, not a held charge**: no BZ note in 2024-2026 makes it a charge; W lists no charge time, cast time none **H**.
- **18.4 m/s (7 horizontal, 17 vertical) for 0.85 s, about 8.4 m up**; steered by move input (W; BZ 2023-03-07 directional; BZ
  2024-02-13 height -20 %) **M**.
- **Cooldown 14 s** (BZ 2024-02-13 10 -> 14; W) **H**.
- **Fuel: +50 % over 0.9 s, may overfill** the max; the overfill lasts **6 s**; Hover Jets burn no fuel while it is filling (BZ
  2024-02-13; W) **H** (the 0.9 s / 6 s: M). A stun doesn't stop it once started (W).

## Jet Dash (secondary fire)
- **20.6 m/s (19 horizontal, 8 vertical) for 0.85 s** in her move direction, or her facing when still (W; BZ 2024-02-13) **M**. Distance:
  **no source**; 19 x 0.85 = about 16 m horizontal (derived, L).
- **Cooldown 9 s** (BZ 2024-12-10 8 -> 9; W) **H**.
- **No fuel in the base kit**; with Fuel Stores +50 % (below). Option: swap Jet Dash and Hover Jets inputs (W; BZ 2024-02-13 called it
  "Jet Dash on double jump") **H**.

## Concussive Blast (ability 2, E)
- **30 on a direct hit only, 0 splash** (BZ 2024-07-09 explosion 30 -> 0; BZ 2022-06-28 beta direct 30; W) **H**.
- **Radius 8 m** (BZ 2024-07-09 6 -> 8; W) **H**.
- **Knockback: direct 31.1 m/s (30 horizontal, 8 up); splash 20.6 m/s (20 horizontal, 5 up)**, mostly horizontal (W; BZ 2024-07-09
  +11 %) **M**. Falloff of the splash knockback with distance: no source.
- **Projectile 60 m/s**, radius **0.225 m** (0.15 + 0.075) (W) **M**.
- **Cast 0.16 s + 0.496 s recovery** (W) **M**.
- **Cooldown 8 s** (BZ 2026-08-11 7 -> 8; W) **H**. **1 charge** (no live source gives more; 2 is Stadium) **H**.
- **Knocks Pharah back too, no self-damage** (W) **M**; her self-knockback speed: no source. Breaks breakables (W).

## Hover Jets (passive, hold Jump in the air)
- **Fuel: a 0-100 % bar, 2 s of continuous flight**: **50 %/s** drain (W) **M**.
- **Regen 45 %/s, only on the ground, after 0.3 s**: 2.22 s empty to full (BZ 2024-02-13 landing needed; W) **H** (rates: M).
- **Vertical 4.66 m/s**; **horizontal +30 % = 7.15 m/s**, outside the global speed cap (BZ 2026-04-14 40 -> 30 %; W) **H** (4.66: M).
  Acceleration: no source.
- **With no fuel, holding it slows her fall to 8 m/s** (BZ 2024-02-13 usable empty to slow descent; W "8 m/s slow fall speed") **H**
  (the 8: M).
- Overfill from Jump Jet (+50 %, 6 s) and Fuel Stores (+100 % max overfuel). Toggle option (W).

## Barrage (ultimate, Q)
- **Cost 2200** (W; BZ 2024-02-13 +10 % globally, 2023-02-07 -5 %) **M**.
- **2.5 s**, **30 rockets/s** (W gives "1 per 0.032 s", which is 31.25/s, L) -> about 75 rockets (BZ 2020-08-13 3 -> 2.5 s; W) **M**.
- **Per rocket: 10 direct + 25 -> 7.5 splash** (35 max), radius **0.5 -> 1.5 m**; self 12.5 -> 3.75 (BZ 2026-04-14 explosion 30 -> 25;
  W) **H**. About 1050 DPS on one target (W).
- **45 m/s**, radius 0.075 m, **8° spread**, fixed pattern by Barrage number in the match (W) **M**.
- **She can move during it at -20 % = 4.4 m/s**, and turn; stays airborne if cast in the air (BZ 2026-04-14 can move; W 4.4 m/s from the
  Drift Thrusters numbers, BZ 2025-05-20 -20 %) **L** (BZ gives no speed; W's text still says stationary).
- **Refills fuel and ammo** on cast (BZ 2024-02-13 fuel; W both on activation; BZ 2018-08-09 said ammo after completion) **M**.
- **Channeled: no other abilities; stun / knockdown / hack ends it and the charge is spent**; no manual cancel (W) **M**.

## Perks (BZ 2025-02-18 on; W)
- **Minor: Concussive Force**: Concussive Blast's explosion deals **up to 30** (BZ 2026-04-14 added at 50, 2026-05-12 50 -> 30; W) **H**.
  Falloff: no source.
- **Minor: Helix Shields**: **125 health becomes shields** (225 = 100 health + 125 shields; the swap also restores up to 125 missing
  health); **direct rocket hits start shield regeneration** (BZ 2025-03-25 75 -> 125, 2025-06-24; W) **H**.
- **Major: Fuel Stores**: **Jet Dash grants 50 % fuel; max overfuel +100 %** (BZ 2025-02-18, fix 2026-06-18; W) **H**.
- **Major: Rocket Salvo**: after **a movement ability** (Jump Jet / Jet Dash; which count: no source), the next primary fire also shoots
  **2 spiraling mini-rockets, 30 each** (BZ 2026-02-10 40, 2026-05-12 40 -> 30; W, radius 0.015 m) **H**. W's "180 if all land" doesn't
  follow from 2 x 30 + 120 (L). Speed, splash: no source.
- Removed: Drift Thrusters (2026-04-14, into Barrage), Concussive Implosion (2025-08-26), Sky Spy (2025-08-26 -> 2026-02-10).

## Hero options (W)
Swap Jet Dash and Hover Jets input (off), Toggle Hover Jets (off), Relative aim sensitivity during Barrage (100 %, 0-500), Hover Jets fuel
colour.

## Disagreements and gaps
1. **Barrage movement:** BZ 2026-04-14 she can move; W's text says stationary but its infobox has -20 % / 4.4 m/s. Speed from W only.
2. **Barrage rate:** W 30/s vs its own 0.032 s interval (31.25/s).
3. **Rocket splash edge:** W text "reaching 0 % at the edge" vs its infobox 24.6 at 2.5 m (BZ 2019 / 2016 history supports a nonzero
   minimum). Ours: 24.6.
4. **Barrage ammo refill:** W on cast; BZ 2018 after completion (only BZ 2024-02-13's fuel refill is stated for cast).
5. **Rocket reload:** W "1.5 s, restores ammo after 0.6 s": unclear which; no BZ number.
6. **Rocket Salvo total 180** (W) doesn't add up.
7. **No source:** Jet Dash distance; vertical acceleration of Hover Jets; Concussive self-knockback and splash-knockback falloff; Rocket
   Salvo speed/splash; the falloff curve shape. A game-data pass would settle these.
