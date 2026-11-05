# Sigma in live Overwatch: numbers and sources (research, 2026-11-02)

**D** = the game data, build 154088 (2026-10-06), read offline and read-only on 2026-11-02 ([sigma-map.md](sigma-map.md); detail in
`E:\Overwatch Archive\research\sigma\work_kit\kit_sigma.md`). **BZ** = Blizzard's live patch notes
(`https://overwatch.blizzard.com/en-us/news/patch-notes/live/YYYY/MM/`, read 2026-11-02: every month from 2022-10 to 2026-11, served
as plain HTML; the 2022-10-04 launch notes are not in the archive; 2026-11 has no notes yet: **the last patch is 2026-10-20**). **The
last live change to Sigma is BZ 2026-07-14** (shields 275 -> 250, Hyper Regeneration 40 -> 30 %); **2026-10-06 (Season 5) has no
Sigma line** and he is not in its hit-volume list; 10-13 and 10-20 have nothing for him. So the data's build (10-06) should hold every
live value, and where it differs from a public number the data wins (the patch notes it would contradict are older). **W** = the
Overwatch wiki on Weird Gloop through its MediaWiki API (`overwatch.weirdgloop.org/api.php`, read 2026-11-02): Sigma rev **408635**
(2026-10-31T08:09Z), Sub-Roles 406777, Roles 401659 (Tank), Barrier 406358, Global Passive Abilities 402069, Hit points 401654,
Movement speed 402311, Melee 402309, Ultimate ability 395578, Perks 354523, Status effect 408615 (Knockback / Crowd control), Critical
hit 404694. No "Fall damage" or "Jump" pages exist. Requests carried a generic user agent and no personal data. The raw pages and the
patch notes as text stayed in the scratchpad.

Each number gives D first, then BZ, then W. Confidence marks are the public sources' (written before the data pass): **H** BZ and W
agree (or W gives it and BZ's history leads to it); **M** one public source; **L** the sources disagree, or a guess. Where they
differ, both are here and in "Disagreements"; where D settles one, it says so, and [sigma-plan.md](sigma-plan.md) says which one
Overcraft uses.

Left out (not the live game; names only): **Stadium** (BZ from 2025-06-24; W Sigma/Stadium rev 402045; his Stadium branches in the
data are gated by .134 perks not in his perk list: 25F3, 25F5, 25F6, 25F9, 25FC, 2600, 2601, 2603, 2604, 2605, 2606, 2657, 25E7, 257A),
**Community Crafted** (BZ 2026-06-30), April Fools (2024-04-01, 2025-04-01, 2026-04-01 Underwatch), Quick Play Hacked (BZ 2026-07-30),
Mystery Madness: Graveyard Games (Halloween 2026), the 2024-12-17 6v6 Experiment (used only as 6v6 history).

**Patches that touched him (live):** 2022-04-26 (OW2 beta, W only: shields 100 -> 200, Accretion 70 -> 100, barrier regen 120 -> 100);
**2023-02-07** Role Queue health stays 350, non-Role-Queue 350 -> 200; Flux cost +7 %; **2023-04-11** Accretion impact 60 -> 40 ("80
total"), knockdown 0.8 -> 1.1 s; 2023-05-09 Flux auto-casts when its timer ends (05-17 fix: endless Flux flight); 2023-07-11 Cassidy's
Magnetic Grenade stops Flux flight; **2023-08-10** ground deceleration raised to the standard hero's (his "floatier" controls had made
the tank knockback passive feel weaker); 2023-10-10 options (Barrier UI, Hide Flux timer, Flux confirm input), Grasp converts Ravenous
Vortex / Blizzard / Disruptor Shot; 2023-10-31 option "Rise During Gravitic Flux With Ability 3"; **2023-12-05** barrier regen
100 -> 85/s; **2024-02-13 (S9)** shields 200 -> 275, all ult costs +10 %, global projectile sizes, quick melee 30 -> 40;
**2024-04-16** barrier speed 16.5 -> 20 m/s; 2024-05-14 Flux needs no line of sight, 2024-05-24 reverted; **2024-07-09** Accretion
impact 40 -> 80 ("120 total"); **2024-09-17** Hyperspheres impact 25 -> 15, explosion 30 -> 40, no self-damage (fix: Flux %-damage hit
by damage reduction); 2024-12-10 (W) global "large" projectile radius 0.15 -> 0.12 m; **2025-02-18 (S15) perks** Kinetic Cycle, Massive
Impact (minor), Hyper Strike, Levitation (major); 2025-04-22 Levitation can be held; **2025-07-22** barrier redeploy 2 -> 1 s, Massive
Impact max at 30 m; **2025-08-26** Massive Impact out, Hyper Regeneration in (50 %); **2025-12-09** Hyperspheres impact 15 -> 25,
**2025-12-12** hotfix 25 -> 20; **2026-02-10 (Season 1)** barrier 700 -> 650, Hyper Regeneration 50 -> 40 %, sub-role **Stalwart**, the
Tank role passive reworked; **2026-04-14** Hyper Regeneration only from damage to enemy heroes; **2026-07-14** shields 275 -> 250,
Hyper Regeneration 40 -> 30 %. 2026-10-06: global hitscan sizes only (none of his weapons is hitscan). Bug fixes only: 2023-02-07,
2023-04-11, 2024-01-09, 2024-10-15.

## Base
- **Health 200 + 150 (the Tank role in Role Queue) = 350, shields 250, armor 0: 600 in all** (D: Hero Stats 0DA0 health 047C **200**
  (alt 300), shields 047D **250**, armor 047E 0, set by values graph 34C3 #11 / #100; the role graph 01C7 #1106 adds **v70468 = 1CA9
  150** to a tank (hero category 0850), M condition; BZ 2023-02-07 "350" in Role Queue, 2026-02-10 "Bonus Health +150", 2026-07-14
  shields "275 to 250"; W 200 / 350, shields 250) **H**. 6v6 / no Role Queue: 200 + 250 = 450 (W Roles 6v6) **M**.
- **Tank, sub-role Stalwart** (D loadouts 0347 "Role: Tank", 0DA5 "Subrole: Stalwart"; hero tag 116B is Stalwart's branch in 01C7
  #2570; BZ 2026-02-10; W) **H**.
- Move speed **5.5 m/s**, backward 90 % (D Hero Stats row 202B 5.5; mover on 2AB1 = the hero default in every field but its size: see
  the map; W Movement speed) **H**. Jump 5.72 m/s (0.98 m apex), gravity 17.5 m/s² to a 30 m/s fall (W Movement speed; D the default
  mover's) **M**.
- **His float has no gameplay effect** but silent footsteps (W strategy text) and his animation: the mover is the default one (D) **M**.
  BZ 2023-08-10 gave him the standard ground deceleration (D: equal to Juno's / every hero's) **H**.
- **Size:** mover height **2.5 m**, radius **0.7 m**, crouch height **2.05 m** (D 2AB1; the default hero 2.0 / 0.5 / 1.25); his model
  is 2.14 m tall and floats above the ground (D 106F7 bounds). No public number. Hit volumes: none published; not in the 2026-10-06
  hit-volume list (BZ) **M**.
- Fall damage: the default falling-damage component, no fall node in his graphs (D); no source names one (Overwatch has none) **L**.
- Perk XP: **minor 1955, major 2935 more (4890)** (D 12AA / 12AB on 34C3 #99; W 1955 / 4890) **H**.
- **Regeneration** (all heroes): after **6 s** without damage, **10/s + 5 % of max health a second** (D 01C7 #963 / #966: 0B0F 6,
  1CAD 0.05, 1CAE 10; W Global Passive) **H**.
- **Ult cost 2300** (D 0AFA 2300, alt 3800; W 2300) **H**.
- Quick melee **40** (D 02D0 / 04F0 40; BZ 2024-02-13 30 -> 40; W Melee: 1 s, 2.5 m, active 0.128 s) **H**.

## The Tank role (current, BZ 2026-02-10; D the shared role graph 01C7)
- **+150 health in Role Queue** (D 1CA9 150 via #1106; BZ; W) **H**.
- **Enemies gain 40 % less ult charge from damage and healing on a tank** (D 01C7 #1066: v46219 = **1CAA 0.6**, or **1CAB 0.8** under
  the mode flag v66788: M that 0.8 is 6v6; BZ 2026-02-10, 2024-07-09 30 -> 40 %; W "40% less", 6v6 20 %) **H**. It also slows perk
  progress (W Roles) **M**.
- **Healing reduction when hurt, 15 % on a tank** (built: `Combat.heal`, #103) (everyone's 30 % halved): D #1308 v25267 = 1A33 0.3 x **1CAC 0.5** for category 0850,
  for **1E68 3 s** (#881) (BZ 2026-02-10 baseline 30 %, tanks 15 %; W Global Passive marks the box "removed" while still editing it:
  see Disagreements) **H** (D settles it is in this build).
- Knockback and critical-hit resistance **left the role** in 2026-02-10 and went to the sub-roles (Stalwart: knockback; Bruiser: crits),
  so **Sigma takes full headshots (x2)** (BZ; W Critical hit) **H**. D: his tags have no Bruiser (1171 / 116F / 116E branches): M.

## Sub-role: Stalwart (BZ 2026-02-10; W Sub-Roles 406777; D 01C7 #2557 / #2558 / #2866 for tag 116B)
- **40 % knockback resistance** (D v69402 = **23C7 0.4**, read by #969 as the mover's knockback scale x(1 - 0.4) = 0.6, M field) **H**.
- **40 % slow resistance** (D #2866: a slow v23186 x (1 - **23C9 0.4**)) **H**.
- D also sets v69403 = 23CA 0.25 for him (the default is 1A32 0.25 too), read as a combat modifier -0.25 by #1280 under a condition
  (L: not knockback or slows; maybe a mode's) **L**. No Stalwart value change since 2026-02-10 (W) **M**.

## Hyperspheres (primary fire)
| Number | D (build 154088) | BZ / W | conf |
|---|---|---|---|
| Direct hit | **20** (0474; 187F #252 -> v2577, the volley's m_6396149F direct part) | BZ 2025-12-12 25 -> 20; W 20 | H |
| Explosion | **40** (0477; v11863), full within **1.0 m** (1B63), down to **x 0.5 at 2.85 m** (047A, ring factor 1AF5 0.5) | BZ 2024-09-17 40; W 40 -> 12, 1 -> 2.9 m | L (min) |
| Per sphere / volley | 60 / 120 | W | M |
| Speed | **50 m/s** (0478; 187F #456 v6368 = 50) | W **48** | L (D) |
| Size | **0.2 + the global 0.12 = 0.32 m** (28DA 0.2 + v54935; env 0.075) | W 0.32 m | H |
| Volley | **two spheres 0.2 s apart** (#175 -> #199 wait 0.2 -> #204), **then 1.3 s** (0479 "Reload Time", v173 = 1.3 chased to 0 at attack speed) = **1.5 s** a volley | W 1.5 s, 0.192 s apart | H (1.5) / M (0.2) |
| Range / lifetime | **21 m** (1AAB) + 0.7 x his scale: **lifetime = 21.7 m / 50 = 0.43 s**, then it bursts | W 22 m travel, range 21 | H |
| Drop | the volley's motion field m_599AB8D7 = **5.5** (the same field is Accretion's 18 m/s² gravity): M a slight drop | W none given | M |
| Pull | knockback horizontal **-0.75** (inward), vertical **1.0** (1B64 / 1B65): about 1.25 m/s | W 1.3 m/s inward (counts as knockback) | H |
| Ammo / crits / self | no ammo or reload; no crit; no self-damage (no self entry in the volley) | BZ 2024-09-17 no self-damage; W | H |
| Fire button | PRIMARY xD128396E, buffer 0.15 s | | H |

## Experimental Barrier (secondary fire)
| Number | D | BZ / W | conf |
|---|---|---|---|
| Health | **650** (048F; 1B55 #1080 v587) | BZ 2026-02-10 700 -> 650; W 650 (6v6 600) | H |
| Regeneration | **0.122 of its max a second = 79.3/s** (0490 0.122 x 650: 34C3 #15), **2 s after it is back with him** (1A2A #658, state 1 only) | **BZ 85/s** (2023-12-05) / **W 79.3/s** / W Barrier page 100/s | **L -> D settles it: 79.3** |
| Speed | **20 m/s** (0F04; 1A2A #636 v1002) along his aim | BZ 2024-04-16 16.5 -> 20; W 20 | H |
| Distance | **9999 m** (#636 m_7856F4BC: no limit); stops at a wall (a 0.9 m ray ahead, #638) | W unlimited, stops on walls | H |
| Cooldown after recall | **1 s** (0493; 1B55 #1041) | BZ 2025-07-22 2 -> 1; W 1 s (Barrier page still 2) | H |
| Cooldown if destroyed | **5 s** (1BC1 / 0373) | W 5 s | H |
| Size | the mesh C015 at rest: **6.70 m wide, 4.18 m tall**, curved 0.73 m deep (D model bounds, its clips may shape it: M) | W 4.5 m tall, 5 m at the poles, 6.5 m mid | M |
| Ult charge | **2 % of what it absorbs goes to his ult** (1A2A #683, as Brigitte's barrier) | none | M |
| Controls | hold SECONDARY to send, release to stop, press again to recall (1B55 states 1 held / 2 sent / 3 stopped / 4 recalled); options **Toggle Experimental Barrier** (off) and **Reload Recalls Experimental Barrier** (off) | W | H |

## Kinetic Grasp (ability 1)
| Number | D | BZ / W | conf |
|---|---|---|---|
| Duration | **2 s** (04AD; 1D02 #489) | W 2 s | H |
| Conversion | **60 % of absorbed damage** (07A1 0.6, alt 1.0) | W 60 % | H |
| Max overhealth | **400** (07A0) | W 400 | H |
| Overhealth timing | **given at the end** (#593 onFinished -> graph 1D03's pool); **decay after 3 s** (1D53 3, "decay delay" in the loadout row), then **5 a second** (1D02 #545 / #546, M) | W "slowly decays after a few seconds", no number | M (D gives the rate) |
| Cooldown | **12 s from the end** (0494; v61 = v472 at #555 / #242, at least 0.2 s after) | W 12 s | H |
| Box | from (-1, -1.6, 0.4) to (1, 1.1, 2.6) m about his eyes, x his scale (#274 StateDeflectProjectiles): **2 m wide, 2.7 m tall, 0.4 -> 2.6 m ahead** | W "3 m box in front, down to his feet" | M |
| Absorbs | projectiles only (StateDeflectProjectiles: hitscan and beams are not projectiles); his own ult charge **+5 % of what it absorbs** (#432) | W | M |
| Button | ABILITY 1 xCBF0F815 | | H |

## Accretion (ability 2)
| Number | D | BZ / W | conf |
|---|---|---|---|
| Impact | **80** (049F; 1BBB v476) | BZ 2024-07-09 40 -> 80; W 80 | H |
| Explosion | **40** (0495) full to **1.5 m** (0496), ring **2.5 m** (0497) with factor 1 (no falloff read) | W 40 -> 12, 2.5 m; self 20 -> 6 | L (falloff) |
| Knockdown | **1.1 s** (04A0) on the target the rock hits (#590: max(1.1, min(v567, distance x 0.11)); v567 only set by the removed Massive Impact 257A, 3.3) | BZ 2023-04-11 0.8 -> 1.1; W 1.1 direct only | H |
| Knockback | impact **6 / 3** m/s (horizontal / vertical: 1B67 / 1B68, = **6.7 m/s**), splash **5 / 3** (1B6A / 1B6B, 5.8 m/s, M which pair) | W 6.7 / 5.8 | H |
| Cast | **0.65 s** (1BBB #869 v670; anim 0x608), **-75 % move speed** (1B69 0.75 into v23184) and **no jump** while casting (#651) | W 0.64 s, -75 % | H |
| Projectile | **37 m/s** (0499), **gravity 18 m/s²** (v7389 = 18), hero size **0.9 m**, environment **0.3 m** (265F / 28C0), lifetime 30 s, from hp 01B6 | W 37 m/s, 0.9 m; BZ 2024-04-01 (April Fools) "37 to 30", "18 to 2" | H |
| Cooldown | **10 s**, starting **0.45 s after the throw** (0498; #669 chase 0.45 -> 0 then #570) | W 10 s | H |
| Ult charge | **+25 on a hit** (#683, M) | none | M |
| Button | ABILITY 2 x2ADDA7B1 | | H |

## Gravitic Flux (ultimate)
| Number | D | BZ / W | conf |
|---|---|---|---|
| Cost | **2300** (0AFA) | W 2300 | H |
| Targeting | up to **5 s** (04AE; 1E41 #1271), then it fires itself; range **35 m** (#1268 m_6386E7B4); confirm with PRIMARY (option "Gravitic Flux Confirmation Input": Primary / Secondary) | BZ 2023-05-09 auto-cast; W 5 s, 35 m | H |
| Area | **radius 7.5 m** (1867, alt 5.0), **height 15 m** (1DFB) from **5 m below** the aimed point (#977 `v2478 - (0, 5, 0)`) | W **8 m** radius, 15 m | **L -> D 7.5** |
| Activation | **1.0 s** (v670 = 1.0, #961; anim 0x691) | W 1 s | H |
| Lift | **2.5 s** (1868; v68035); targets: **50 damage** on lift (04AB), **held still 0.4 s** (23F2; 1DBC #586), gravity x0.1, pulled up (18 m/s then 5) | W 2.5 s, 50 lift damage | H |
| Slam | **50 % of the target's max health** (1AAD 0.5; 1DBC #577, m_FB3F6CA9: no damage modifiers) after the drop | W 50 %, ignores modifiers | H |
| After | **high gravity 0.6 s** (1DFC), **-50 % speed** (1DFA -0.5), gravity x22 (1DBC #581) | W 0.6 s, -50 % | H |
| His flight | gravity off, the flying mover at **5.5 m/s x 1.3 = 7.15 m/s** (1B66 +30 %), **-50 % knockback** (x0A04B51B -0.5, #1071); rise / fall on jump / crouch (option: ability 3 to rise) | W 7.15 m/s, -50 % | H |
| Button | ULTIMATE x2DFEDC52 | | H |

## Perks (live list 004B.177 = [2578, 26B7, 25AF, 2577].134, D)
- **Minor: Kinetic Cycle** (2578 -> loadout B77): at Grasp's **normal end**, **Accretion's cooldown -= absorbed x 0.02 s** (1D02 #738 /
  #737: -2 s per 100) (W -2 s per 100, only if it ends normally; BZ 2025-02-18 text) **H**.
- **Minor: Hyper Regeneration** (26B7 -> CE8): **30 % of Hyperspheres' damage to enemy heroes heals the barrier** (51E0 #434-#443 ->
  message 2961 -> 1A2A #1039 `amount x 0.3`) (BZ 2026-07-14 40 -> 30 %, 2026-04-14 heroes only; W 30 %) **H**.
- **Major: Hyper Strike** (25AF -> C1F): **every 5 direct Hypersphere hits** (51E0 v181 = 5, counted on the direct-hit id 9C8B), the
  next quick melee that lands **knocks the target away at 14 m/s** along his aim, lifted (graph 54A5 on the target) (W: +50 damage over
  0.37 s, 1 s levitation, **15 m/s**) **M** (D 14, the +50 is in 54A5 / loadout 02F5, not read).
- **Major: Levitation** (2577 -> B92): **hold jump in the air** (0.2 s after leaving the ground) to rise for **1.75 s** (51E0 #383),
  accelerating at 25 m/s² up to **3 m/s** (#382: (0, 25, 0), max 3: M fields), once until he lands (W: 2.3 m/s for 1.75 s, about 5 m;
  BZ 2025-04-22 hold) **M** (speed).
- Removed: Massive Impact (2025-02-18 to 2025-08-26; its 257A branch is still in 1BBB, not live).

## Disagreements
1. **Barrier regeneration:** BZ 85/s (2023-12-05) vs W 79.3/s vs W Barrier 100/s. **D: 0.122 of its max a second = 79.3/s** at 650
   (it was 85.4 at 700: the 2026-02-10 health cut lowered it without a line). D settles it.
2. **Hypersphere speed:** D **50 m/s** vs W 48. D.
3. **Hypersphere explosion falloff:** D x0.5 at the 2.85 m edge (20) vs W 40 -> 12 (x0.3) at 2.9 m. D (M on the ring field's reading).
4. **Gravitic Flux radius:** D **7.5 m** (alt 5) vs W 8 m. D.
5. **Accretion splash falloff:** D's rings carry factor 1 (40 to 2.5 m) vs W 40 -> 12. Not settled (M): the engine may apply its own
   falloff between the rings.
6. **Hyper Strike knockback:** D 14 m/s vs W 15. D.
7. **Levitation:** D 25 m/s² up to 3 m/s for 1.75 s (M fields) vs W 2.3 m/s. A Workshop recording settles it (Open).
8. **Barrier size:** D's mesh at rest 6.70 x 4.18 m vs W 6.5 / 5 x 4.5 m. M: the clips (set 0AEF, 2 clips) may stretch it.
9. **Barrier cooldown after recall:** BZ / W Sigma 1 s vs W Barrier page 2 s (stale). D 1 s.
10. **6v6 tank ult reduction:** W 20 %, the 2024-12-17 experiment 25 %; D 0.8 under the mode flag (20 %). D (M that the flag is 6v6).
11. **Healing reduction:** W Global Passive marks it removed; BZ shows no removal; D has it (30 %, tanks 15 %, 3 s). D.
12. **Volley spacing:** D 0.2 s vs W 0.192 s. D.
