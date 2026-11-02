# D.Va in live Overwatch: numbers and sources (research, 2026-11-02)

**D** = the game data, build 154088 (2026-10-06), read offline and read-only on 2026-11-02 ([dva-map.md](dva-map.md); detail in
`E:\Overwatch Archive\research\dva\work_kit\kit_dva_mech.md` / `kit_dva_pilot.md`). **BZ** = Blizzard's live patch notes
(`https://overwatch.blizzard.com/en-us/news/patch-notes/live/YYYY/MM/`, every month 2022-10 to 2026-11, read 2026-11-02 from the copies
fetched for Sigma's pass the same day; the 2022-10-04 launch notes carry no hero numbers; 2026-11 has no notes yet: **the last patch is
2026-10-20**). **The last live change to D.Va is BZ 2026-10-06** (Season 5: Micro Missiles 11 -> 14 a second), the data build's own day:
**the data has 14** (1128 reads 0404 = 14), so the build holds it; nothing for her on 10-13 or 10-20, and she is not in 10-06's hit-volume
list. So where the data differs from a public number, the data wins. **W** = the Overwatch wiki on Weird Gloop through its MediaWiki API
(`overwatch.weirdgloop.org/api.php`, read 2026-11-02): D.Va rev **408749** (2026-11-02T16:46Z, edited the same day), D.Va/Stadium 403704
(names only), Sub-Roles 406777, Roles 401659, Hit points 401654 (Armor), Movement speed 402311, Status effect 408615, Ultimate ability
395578, Perks 354523, Global Passive Abilities 402069, Melee 402309, Critical hit 404694. Most of the other pages' D.Va rows transclude
her page, so they don't confirm it independently. Requests carried a generic user agent and no personal data; the raw pages stayed in the
scratchpad.

Each number gives D first, then BZ, then W. Confidence marks are the public sources' (written before the data pass): **H** BZ and W agree
(or W gives it and BZ's history leads to it); **M** one public source; **L** the sources disagree, or a guess. Where D settles a
disagreement it says so; [dva-plan.md](dva-plan.md) says which one Overcraft uses.

Left out (not the live game; names only): **Stadium** (W D.Va/Stadium 403704; her Stadium branches in the data are gated by 02xx.134 perks
not in her perk list), **Community Crafted** (BZ 2026-06-30), April Fools (2024, 2025, 2026), Overwatch Classic, Junkenstein's Lab, Quick
Play Hacked (BZ 2026-07-30), Mystery Madness (2026-10), the 2024-12-17 6v6 Experiment (6v6 numbers kept as such).

**Patches that touched her (live):** 2022-04-26 / 06-28 (OW2 beta, W: health 300 -> 450 -> 350, armor 300 -> 200 -> 300; Fusion slow 50 ->
40 %, spread 4 -> 3.5); 2022-10-04 (W: Boosters impact 10 -> 25, missiles cooldown 8 -> 7 s); **2022-11-17** spread 3.5 -> 3.75, Boosters
impact 25 -> 15, Call Mech -12 %; **2023-02-07** Role Queue mech health stays 350; 2023-05-09 options Hide Boosters Cancel Text / Timer;
**2023-08-10** Boosters cooldown 4 -> 3.5 s, **eject: 0.4 s damage immunity**; 2023-10-10 options Light Gun Input, Relative Light Gun aim
sensitivity; **2023-10-31** Fusion slow 40 -> 30 %, missile speed 40 -> 50; **2024-02-13 (S9)** pilot 150 -> 175, armor 300 -> 375,
Self-Destruct 900 -> 1000 and inner radius 4 -> 6 m, ult costs +10 %, quick melee 30 -> 40; 2024-02-21 / 03-12 Light Gun shot 0.25 -> 0.2 m
(+0.075 global); 2024-05-14 / 07-09 / 09-17 armor's flat reduction 5 -> 10 -> 7 (cap 50 %); **2024-06-20** spread 3.75 -> 3.375, Boosters
impact 15 -> 25; **2024-07-09** missiles explosion 4 -> 5.5, Defense Matrix 3 -> 3.5 s (**2024-08-20** back to 3), tank ult reduction 30
-> 40 %; **2024-09-17** armor 375 -> 325, health 200 -> 225; 2024-12-10 hitscan sizes; **2025-02-18 (S15) perks**; **2025-04-22** Bunny
Power, Extended Boosters; 2025-07-22 Heavy Rockets tuned; **2025-08-26** Heavy Rockets out, Precision Fusion in; 2025-09-16 / 2026-01-08
Precision Fusion fixes; 2025-11-11 / 2026-01-08 Extended Boosters +100 % -> +40 % damage; **2026-02-10 (Season 1)** health 375 -> 325 total
(RQ), sub-role **Initiator**, the Tank role passive reworked; **2026-03-10** Boosters 3.5 -> 4 s, Initiator 75 -> 60, not at full health;
**2026-05-12** missile impact 3 -> 2, Initiator from Boosters (not air time), 60 -> 50; **2026-06-16** Shield System 150 -> 100;
**2026-09-08** mech base health 175 -> 200 (RQ 350); **2026-10-06 (Season 5)** missiles 11 -> 14 a second; global hitscan sizes (small 0.04
-> 0.02 m: the Fusion pellets).

## Two bodies (D: how the data builds her)
- **The hero is the pilot**; **the MEKA is a pet entity** she controls while linked (D 0407 STUPetComponent; 026A #98 the link: control
  to the pet, she rides attached, untargetable, gravity off, her view slaved to its) **H**. No public source describes the build; W's
  "Pilot Form" / "Eject!" pages describe the behaviour. This is the one thing Overcraft has never had: [dva-plan.md](dva-plan.md).

## MEKA: base
- **Health 200 + 150 (the Tank role in Role Queue) = 350, armor 325: 675 in all** (D: values graph 33D9 #119 sets health 0377 **200**
  and armor 0376 **325** through 3361; 01C7 #1106 adds 1CA9 **150** to a tank; BZ 2026-09-08 175 -> 200, 2024-09-17 armor 325, 2026-02-10
  "+150"; W 200 / 325) **H**. 6v6: 110E 250 + 110F 300 = 550 (D 33D9 #195 under v66788; BZ 2026-02-10; W) **H**. Non-Role-Queue 5v5
  200 + 325 (derived) **L**.
- **Armor:** each hit 7 smaller, at most halved; beams -30 % (BZ 2024-09-17 7, 2023-08-10 cap; W Hit points) **H** (D: engine, not her
  graphs).
- **Tank, sub-role Initiator** (D loadouts 0347 "Role: Tank", 0DA7 "Subrole: Initiator"; hero tag 116D, 01C7 #2556 -> #2517; BZ
  2026-02-10; W) **H**.
- Move speed **5.5 m/s**, backward 90 % (D 33D9 #119 64E7 5.5; the mover on 0407 is the hero default in every field but its size; W)
  **M**. Jump 5.72 m/s, gravity 17.5 (D the default mover's; W) **M**. **No crouch** (D: no crouch button in any mech graph; 0276 #246
  mover flag x292F571E = 0, M; W) **M**.
- **Size:** mover height **2.3 m**, radius **0.7 m** (D 0407; the pilot 2.0 / 0.5); the model 2.62 m wide with the cannons, 2.43 m tall,
  2.03 m deep at rest (D FBB0 bounds); her first-person eye **1.74 m** (D 041F m_E29993A5 / m_816B830F 1.74 / 1.74, M meaning). No
  public number; not in 2026-10-06's hit-volume pass (BZ) **M**.
- Quick melee **40** (D 0043 reads 02D0 40 from 348C #102; 04F0 40 in 3361 #54; BZ 2024-02-13; W) **H**. Headshots x2, no crit
  reduction (Initiator, not Bruiser: W Critical hit; BZ 2026-02-10) **H**.
- Perk XP: **minor 1615, major 2325 more (3940)** (D 12C3 / 12C4 on 33D9 #194 / 348C #165, read by 527A #123; W 1615 / 3940) **H**.
- Falling off the map in the mech kills her outright (W) **M** (D: Self-Destruct's out-of-map check 0B2F #364, M).

## Fusion Cannons (primary, MEKA; 050A)
- **11 pellets** (D 1185 = 11 at #444, misnamed in the value table; W 11) **M**.
- **2 per pellet, falloff from 10 to 20 m** (D 0398 2 at #421; 039C 10 / 039B 20 at #447 / #448; W 2 -> 0.6) **M**; the floor 0.3 x
  (W 0.6) is likely 0303 0.3 (3361 #132), its var v37674 has no writer in the graphs read (L).
- **6.67 volleys a second** (D a 0.15 s wait #411 / #269 at speed v38610; W 0.15 s) **M**: about 146 a second.
- **Spread 3.375°** (D 0399 3.375; 6v6 1111 4; BZ 2024-06-20; W): one pellet in the middle, 4 at half the cone, 6 at its edge (D #444
  pattern, M that the rings are fractions of the cone) **H**.
- **-30 % move speed while firing** (D #376 v23184 += 1DB9 0.3), **none during Boosters** (gated by v87, which Boosters sets, 047E #241:
  M) (BZ 2023-10-31; W) **H**.
- Pellet radius **0.02 m** (D 28A3 0.02, the global "small" hitscan; BZ 2026-10-06 0.04 -> 0.02; W) **H**. Crits (D 03A6 1.0, the
  generic x2, M; W) **M**. Not while Defense Matrix is up (W) **M**.

## Defense Matrix (secondary, MEKA; 02CC)
- **Energy 100, drained 33 a second (about 3.03 s from full)** (D 1864 100, 1B49 33; 6v6 1B4A 50; BZ 2024-08-20 back to 3 s; W 3 s)
  **H**.
- **Back at 16 a second after 0.75 s** (D 03B7 16, 1B50 0.75 at #284 / #389: 6.25 s from empty; 6v6 21AE 14; W 16 % / 0.75 s, its text 1 s
  / 6 s) **M**. **Minimum use 0.5 s** (D #321; W 0.496) **M**. **Cooldown 1 s** (D 1865 1.0; W) **M**.
- **Range 10.25 m** (D 1B4E 10.25; W 10.3): one box **±1.25 m wide, ±1 m tall, from 0.8 to 10.25 m ahead** of her aim (D #205; the origin
  M) **M**.
- **Eats projectiles, not beams, melee or AoE**; what it eats is set on each projectile (tag 145), not in her graph (D; W's list) **M**.
- **Ult: 4 % of the damage it blocks** (D #255, M that it's ult charge; no public source) **L**.
- Toggle option (D v8175; W) **H**.

## Boosters (ability 1, MEKA; 047E)
- **12 m/s along her aim** (D 047E #158 a flight mover: 5.5 x (1 + 1285 1.18) = 12.0; W 12) **M**; **0.4 s to 2 s** (D 03AF 2 at #166,
  the earliest end #179 0.4 s; W) **M**. Hold option (D v12259; W) **H**.
- **Cooldown 4 s**, from 0.3 s after the flight ends (D 03A9 4 at #171; BZ 2026-03-10 3.5 -> 4; W 4) **H** (the 0.3 s: D only, M).
- **Impact 25** to each enemy once, in a 1.5 m sphere 1 m up, 2 m ahead (D 03AB 25 at #172; BZ 2024-06-20; W) **H**; **knockback 17
  horizontal + 4 up = 17.5 m/s** (D 03AC / 03AD; W 17.5) **M**.
- **Knockback taken -90 % while flying** (D 1E09 -0.9 at #243; W) **M**. Fusion (no slow), missiles, Defense Matrix and quick melee all
  usable during it (W; D v87) **M**. No turn-rate node found (D).
- **Starts Initiator's heal** (D #487 message 2B3B -> 01C7 #2953; BZ 2026-05-12) **H**.

## Micro Missiles (ability 2, MEKA; 1128)
- **18 missiles** (D 1B56 18; W 18; BZ's totals imply it) **H**, **14 a second** (D 0404 = 14 at #645, misnamed "Micro Missile Cooldown";
  BZ 2026-10-06 11 -> 14; W) **H**: 1.29 s a volley; a **0.25 s** cast first (D #652; W 0.16 s) **L**.
- **2 on impact + 5.5 splash**, full within 0.1 m, **x0.25 at 1.5 m** (D 03B0 2, 03B1 5.5, 03B6 0.1, 03B4 1.5, 1B51 0.25; BZ 2026-05-12 3
  -> 2, 2024-07-09 5.5; W 1.375 at the edge) **H**: 135 if all land. No knockback (D) **M**. Self-damage (W 5.5 -> 1.375; D flag not
  identified) **M**.
- **50 m/s**, radius **0.175 m** (D 03B5 50; 28D6 0.1 + 0.075; BZ 2023-10-31; W) **H**.
- **Cooldown 7 s** from the volley's end (D 03B3 7, 6v6 1112 8; W) **H** (the start: M).
- With Boosters, Defense Matrix, Fusion (W; D: their own graph) **M**.

## Self-Destruct (ultimate, MEKA; 0B2F)
- **Cost 1700** (D 0AE4 1700, alt 3080 another mode (L), read through the mech's schema entry; W) **M**. Charge carries over between mechs
  (D 0276 #88 / 026A #775: a new mech's charge starts from the pilot's stored v2511, M; no public source) **L**.
- Cast: she **ejects** (D #271 message 016C) and the mech flies on unmanned; **3 s fuse** (D 0C6A 3 at #410, misnamed "Outer Radius"; W)
  **M**.
- **1000 within 6 m, then down to 50 at 21 m** (D #280: 03BB 1000, 03BC 6, 03B9 50, 03BD 21: 50 + 950 x (21 - d) / 15; BZ 2024-02-13 900 ->
  1000, inner 4 -> 6 m; W 1000 -> **100**) **H** (max, inner) / **L** (the minimum: D 50, W 100).
- Targets in sight of its centre, 1.3 m up (D, M); none if it fell off the map (D #364, M). **Knockback 20 horizontal / 10 vertical =
  22.4 m/s** at the centre, ring factors 0.75 / 0.75 / 0.25 at 21 m (D 1B59 / 1B5A / 1B5B / 1B5C; W 7.8 -> 22.4) **M** (max) / **L** (min).
- **Call Mech full on detonation** (D #274 message 0592 -> 026A #887; W) **M**. The mech is removed 0.3 s after (D).

## Eject! (passive; 0276, 026A)
- When the mech dies: **1.0 s** in the wreck (Self-Destruct still usable: D 0276 #79 / #213, 1B57 1.0 misnamed "Boosters Duration"), then
  she is ejected (#84 message 016C); the wreck is gone **0.75 s** later (1B58) (H). W "1.8 s" may be these 1.75 s (L).
- The eject: **a full heal** (026A #97, 9999) and **a launch 6 m/s back, 7 m/s up** (9.2 m/s) from 1 m ahead of the mech's facing (D; no
  public number); **0.4 s untargetable** (D #880 x45AB6711, M) (**BZ 2023-08-10 0.4 s**; W 1.8 s) **L** -> D settles BZ's 0.4 s. Call Mech
  locked 1 s (D #640) and her charge from 0 (D #85: no charge given on eject; no public source).
- The Light Gun is full (D 026A #152 refills it while linked) **H**.

## PILOT (Pilot Form)
- **Health 175**, no armor, no tank bonus (D 348C #119 0268 175; BZ 2024-02-13 150 -> 175; W) **H**. D's role node (01C7 #1106) doesn't
  tell the pilot from the mech (Open: the gate isn't in the graphs read); BZ / W say the mech only.
- 5.5 m/s, can crouch; the default hero mover in every field (D 040A); eye **1.32 m**, crouched **0.8 m** (D 040B, M meaning; W) **M**.
- **Light Gun** (02B8): **14** a shot (D 03A2 at #326), **7 a second** (03A4), **20 rounds**, **1.4 s reload** (03A3; the magazine full at
  0.91 s, 0.65 x 1.4, #354), **50 m/s**, radius **0.275 m** (28D5 0.2 + 28A6 0.075), 5 s lifetime, **no falloff, no spread**, crits x2
  (03A7 1.0 unread: generic, M) (BZ 2024-02-21 / 03-12 size; W the rest) **M** (H for the size). Light Gun Input option (D v51472; BZ
  2023-10-10; W) **H**.
- Quick melee 40 (D 348C #102 02D0 40) **H**.

## Call Mech (ultimate, pilot; 026A)
- **Cost 280** (D 0AE3 280, alt 420 (L another mode), through the pilot's schema entry 0152; W) **M**; built from 5 a second plus damage
  as pilot (W; D: the engine's, not her graphs) **M**.
- **0.2 s cast** (no move, no jump: D #105 / #347), the mech spawns 1.5 m ahead (or on her spot if no ground there: a 4 m probe, ground
  within 2 m needed, else no cast and no cost) (D #94 / #256 / #455) (W "must be grounded"; 0.896 s startup + 2.032 s recovery) **M**.
- **Lands 0.9 s after the press: 250 within 3 m, knockback 15 + 3 = 15.3 m/s** (D 03BE 250 misnamed "Self-Destruct Damage Min" at #833,
  03BF 3 at #834, 1B5D 15 / 1B5E 3 at #949 / #950; W 250 / 3 m / 15.3 at 0.896 s) **M**; with Bunny Power 4.5 m, falling off from 3 m to 4.5
  (D #908 / #993 / #941; W 4.5 flat) **M**.
- **She drives it 1.95 s after the press** (D #431; W 0.896 + 2.032 = 2.93 s) **L** -> D. Interrupted (stun, knockdown, hack): the mech is
  removed and the charge spent (D #121 onAbort, M; W) **M**.
- A new mech each call: full health and armor (D) **H**.

## Passives
- **Tank role** (D 01C7, runs on both bodies): **+150** (#1106, 1CA9), **enemies' ult charge from her x0.6** (x0.8 6v6) (#1066), the
  role's healing reduction (#1308), regeneration (#963 / #966): as Sigma's ([sigma.md](sigma.md)) (BZ 2026-02-10; W) **H**.
- **Initiator** (D 01C7 tag 116D): Boosters' start (047E #487 -> message 2B3B -> #2953) heals **50 over 1 s** (#2539, 23D0 50 / 23D1 1),
  **not at full health** (#2857), then a **5 s** lockout (#2889, 257B 5) (BZ 2026-05-12 50 from Boosters, 2026-03-10 not at full; W 5 s) **H**
  (the 5 s M -> D settles it). Only the mech heals so (the pilot has no Boosters).
- Global: in-combat healing reduction 30 % (15 % on tanks), regeneration after 6 s (W Global Passive) **M**.

## Perks (live list 0042.177 = [253B, 25E6, 253E, 273E].134, D)
- **Minor: Bunny Power** (253B -> B11): **75 overhealth when the link drops**, held 10 s then decaying (D 514D #424-#427: 25 a second, M)
  and **Call Mech's radius x1.5 = 4.5 m** (D 026A #941 v38703 0.5) (BZ 2025-04-22; W) **H**.
- **Minor: Extended Boosters** (25E6 -> C84): **+0.5 s per enemy hero hit, at most 2 s left** (D 047E #458-#463) and **25 x 1.4 = 35**
  (D 514D #310 / #656) (BZ 2025-04-22, 2026-01-08; W) **H** (the 2 s cap: D only).
- **Major: Shield System** (253E -> B13): **100 health becomes shields** (D 514D #399 / #400 / #406: 100 + 150 + 100 shields + 325 armor)
  and **Defense Matrix gives back 25 % of what it absorbs as shields** (#402, message 2638) (BZ 2026-06-16; W) **H**.
- **Major: Precision Fusion** (273E -> D41): **Reload**: **3.1 s** of spread -75 % (ramped in 0.1 s), fire rate x2 (13.33/s), damage x0.5 (1
  a pellet), falloff x1.5 (15-30 m); Reload again after 0.4 s cancels; **cooldown 7.5 s** from its end (D 514D #594-#650 / #613-#616; BZ
  2025-08-26 spread / 3 s; W 8 s, 1 -> 0.3, 15-30 m, 13.33/s) **M**. Nothing ties it to the matrix's drain (BZ 2025-09-16's fix) (D).
- Removed: Heavy Rockets (253D.134; still in 1128 #713, unreachable: 7 rockets at 6/s, 5 + 22, 2 / 3 m, gravity 18) (D; BZ 2025-08-26).

## Hero options (D `herosettings_dva.json`, 0126.054)
Toggle Defense Matrix (off), Hold To Use Boosters (off), Hide Boosters Cancel Text (off), Hide Boosters Timer (off), **Light Gun Input**
(Primary / Secondary / Both, default Primary), Relative Light Gun Aim Sensitivity (1.0, 0.01-5.0) and its gyro twin; not in the public
list: **Weapon Mode** (v53156: Fusion Cannons / Light Gun / Both, L what it does), **Camera perspective while in the MEKA** (v67805:
Default / First / Third Person), six rumble sliders (BZ 2023-05-09 / 2023-10-10; W) **H**.

## Buttons (D)
PRIMARY xD128396E (Fusion 050A #364; Light Gun by option), SECONDARY xB42D90B7 (Defense Matrix 02CC #175), ABILITY 1 xCBF0F815 (Boosters
047E #78), ABILITY 2 x2ADDA7B1 (missiles 1128 #175), ULTIMATE x2DFEDC52 (Self-Destruct 0B2F #239, the wreck 0276 #213, Call Mech 026A #87),
RELOAD x5D2A8B7F (Light Gun; Precision Fusion 514D #615). Buffers 0.15 s. No crouch in the mech.

## Disagreements
1. **Eject immunity:** BZ 2023-08-10 0.4 s vs W 1.8 s. **D 0.4 s** (026A #880); 1.8 s matches nothing but the 1.75 s from mech death to the
   wreck's removal (L). D.
2. **Self-Destruct minimum:** D **50** at 21 m vs W 100. D.
3. **Call Mech radius:** W 3 m (its own history says 2.5 since 2021). **D 3 m**; Bunny Power's 4.5 m falls off from 3 m (W flat). D.
4. **Call Mech timing:** W 0.896 s to land, 2.93 s to control; **D 0.9 s and 1.95 s**. D.
5. **Precision Fusion:** D 3.1 s, cooldown **7.5 s**; W 3 s / 8 s. D. BZ 2025-09-16's matrix drain fix: no trace in D.
6. **Micro Missiles start:** D 0.25 s vs W 0.16 s. D (M: the wait may overlap the first missile).
7. **Self-Destruct minimum knockback:** W 7.8 m/s; D's ring factors don't give it either way they're read (Open, M).
8. **Defense Matrix regeneration:** W 0.75 s / 16 % (box) vs 1 s / 6 s (text). **D 0.75 s / 16**. D.
9. **Must be grounded (Call Mech):** W yes; D ground within 2 m below, and a failed placement keeps the charge. D.
10. **The +150 on the pilot:** BZ / W mech only; D's role node doesn't show the gate (Open). Ours: mech only (public).
11. **Fusion floor:** W 0.6 a pellet (30 %); D's floor var has no writer in the graphs read (0303 0.3 likely, L). W.
12. **New in D, no public source:** Boosters' cooldown 0.3 s after landing; Extended Boosters' 2 s cap; 4 % of blocked damage to her ult; the
    Self-Destruct charge carried between mechs (and growing 5/s while a pilot, M); the eject's launch and full heal; Call Mech's 1 s lock.
