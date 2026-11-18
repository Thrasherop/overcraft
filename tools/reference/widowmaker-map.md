# Widowmaker data map, from Overwatch's game data (build 154088)

Read 2026-11-17 (#138) offline and read-only through TankLib/DataTool on the archived CASC copy `E:\Overwatch Archive\build-154088`
(`--online=false`), with Overwatch and Battle.net checked closed before every run, as [ashe-map.md](ashe-map.md). Confidence: **H** read in
the data, **M** one link inferred, **L** guess. Public numbers: [widowmaker.md](widowmaker.md) (written separately). The node-by-node reading
this map condenses: `E:\Overwatch Archive\research\widowmaker\work_kit\kit_widowmaker.md`; its graphs `work_kit\c_<g>.txt` / `decomp_<g>.txt`
/ `edges_<g>.txt`, values `work\varvalues_4BE6_out.txt`, `work\lostats_out.txt` (the in-game stat rows), `work\ents_widow_out.txt`,
`work_kit\herosettings.json`. Scripts: Ashe's, retargeted (`research\widowmaker\kscripts\`, `kscripts\kit\`; new `hero0_widow.py`,
`kit\herosettings_widow.py`). No Workshop recording yet.

## Ids
- Hero **000A.075**, entity **001C.003** (Gameplay3P), 1P **001D.003**; category 084E, tags 015A / 0426 / 0427 / 11A0 / 00F7 / **1188**
  (Sharpshooter). Graphs: **001B** Widow's Kiss and Take Aim (weapons manager 0015), **005A** Grappling Hook, **0056** Venom Mine throw,
  **0057** the mine (on entity 00AE), **0073** the mine's poison (on each target), **59BF** the Seeker Mine (replaces 0057), **59C0** the
  dart's poison, **0067** Infra-Sight (545C its voice lines, 605E stats), **51EF** perks, **4BE6** her values (Hero Stats 3361), **0043**
  quick melee (shared), 01C7 shared (Sharpshooter). Not hers / not live: 224E (Ana's Sleep Dart, under 081F.134), 25B4 / 0033 (shared
  pet), 3BFA (mine damage scaling, L), 261E (an effect on the mine), 0076 / 08B6 / 5016 / 004B (shared, not read).
- Buttons: PRIMARY xD128396E (001B #16 -> hip volley #17, or scoped #54 -> #52 / #745 / #750), SECONDARY xB42D90B7 (Take Aim: hold #291, or
  toggle #294 under hero option Toggle Zoom v7647), RELOAD x5D2A8B7F (#10), JUMP x4F81E1BD (unscope 001B #188; pull cancel 005A #24),
  ABILITY 1 xCBF0F815 (Grappling Hook 005A #159), ABILITY 2 x2ADDA7B1 (Venom Mine 0056 #84), ULTIMATE x2DFEDC52 (Infra-Sight 0067 #73),
  melee xD19BBFFC (0043).
- Loadouts: 0026 Widow's Kiss, 0025 Take Aim, 0024 Grappling Hook, 0023 Venom Mine, 0022 Infra-Sight, 0DCF Hero Stats, 0DDD Sub-role
  Sharpshooter. Perks (0060.177 order minor, minor, major, major; tiers as W): **25A7** Scoped Efficiency (0C32), **2736** Sniper's Instinct
  (0D3A) (minor); **2873** Widow's Bite (0E39), **2704** Seeker Mine (0D1E) (major).
- Models (skin Overwatch): body **011F.00C** 3P (look 012E, set **002A**), **013E.00C** 1P (look 0175, set **0016**); **Widow's Kiss 01D4.00C**
  (entity 002A, look 01DA, set **0029**) on hardpoint **000A**, var **12BB** (3P and 1P), muzzle hp 0006; on the Kiss: **306C.00C** (entity
  13F6, look 3BEF, at the Kiss's hp 0068, var 1AFF, no graph reads it: L the claw / scope piece); **grappling claw 0711.00C** (entity
  **00D3**, look 062E, no set, projectile visual; child 7527 effect 7AD1), fired from hp **0055** on her; **rope** = beam effect **0277.00D**
  (3P) / **33C5.00D** (1P) from her hp 0055 to the claw (005A #38 / #260); flight / reel / pull effect entities 00D9 / 00DA / 00DB;
  **Venom Mine 06F2.00C** (look 0605): thrown entity **00AF** (set **0074**, projectile visual, child 0161 effect 00FE), stuck entity **00AE**
  (set **0054**, pet "Venom Mine", health 1, graphs 0057 / 59BF + 25B4 / 0033), thrown from hp 002A; **Seeker Mine dart 61CA.00C** (entity
  **6B75**, set 0074), mine hp 0012 -> target hp 0001. Shots (no models): hip 004C; scoped 0107 (< 50 %), 004B (50 to < 100 %), 0108
  (100 %); scoped tracer 60CB (250 m/s).
- Clip maps (the other agent's, this pass didn't touch them): `tools/clipmaps/widowmaker.txt`, `widowmaker_1p.txt`, `widowmaker_kiss.txt`,
  `widowmaker_mine.txt`, `widowmaker_clips.txt`, `widowmaker_intro_clips.txt`. The aliases they should name are under "Anim aliases".

## The numbers (data, the node that reads it)
| | Data (value id, node) | Used |
|---|---|---|
| Health, melee, speed | **225** (0BA9, 4BE6 #1); melee 40 (02D0), 1 s (0043 #143); 5.5 m/s (202B); regen delay 6 s (0B0F); mover 2.0 m, r 0.5, crouch 1.25 H | |
| Hip damage, rate | **14** (1AAE -> v69694, volley 001B #17); **10 /s** (v8421 10 x v21541, #721 / #17) H | |
| Hip falloff | **20 -> 40 m** (1AC7 / 1AC6); floor v37674 (synced from the shared stats graph, value not readable, L; W 30 %) H/L | |
| Hip spread | **0 -> 3 deg** (v229 / 1E4D -> v230, #723) over **11 shots** (1E4E -> v23333 = volley m_AC20B1C1, M); recovery **4 /s** (#69) starting **0.2 s** after the last shot (v6618 0.2 -> 0 at 1 /s, #263 / #264 / #265): full to none 0.25 s H/M | |
| Hip size, crit | 0.04 m (28A2 -> v54890); crit field 0.0 = default **x2** (M) | |
| After hip fire | v173 = **0.15 s** on the volley's end (#60): no new press or charge until it runs out (H/M) | |
| Ammo | **35** (1AC5 -> v198, #699); hip 1 a shot; scoped **5** (1ACC -> v65941; **3** with Scoped Efficiency, 51EF #343) H | |
| Reload | **1.5 s** (1ABD, anim 0004 #172); **ammo back at 1.5 x 0.55 = 0.825 s** (#273 -> #271); no fire for the remaining 0.675 s (#269 v414); scope held off 1.35 s (#303); no interrupt node; R or empty (#10 / #4) H | |
| Scope in / out | **0.333 s** each (v169 ChaseVar rate 3, #120 / #123); no shot during either (#136 v414) H; anims 045D / 045E (0.5 s clips) | |
| Zoom | v172 = **30** (#44) applied at full scope (#139 -> #32 STU_75D1C308); L an FOV of 30 deg (Ashe 40) | |
| Scoped move | **-65 %**: v23184 += 0.65 (1E5A, #370) while fully scoped, v23184 the self-slow (01C7 #868 / #1158) M; the 0.3 / 0.3 at #115 is the zoomed **aim sensitivity** hero setting (v7648 / v24086), not speed H | |
| Charge | starts once fully scoped and the 0.5 s recovery is over (#124 / #132); v174 **1 -> 10 at 10 /s = 0.9 s**, linear (1AC0 / 1ABB / 1E77, #126 / #597); **resets to 1** whenever the chase stops: after each scoped shot and on unscope (#125) H | |
| Scoped damage | **12 x charge** (1ABA x v2489): **12** at no charge -> **120** full, linear; falloff **50 -> 70 m, floor 0.5** (1AC4 / 1AC3 / 1AD5); crit field 1CEA **1.25** = **2.5x** (M); hitscan, 0 m, range 400; tracer 60CB at 250 m/s H | |
| Scoped recovery | **0.5 s** (#2 v173 = 0.5 on the volley's end) H | |
| Under 50 % | entity **0107** (< 50 %) / 004B / 0108 by (charge - 1) / 9 (#729); trail widths 0.5 + 4 x charge / 10 (m_C5143E2E) H values, M look | |
| What drops the scope | **JUMP** while scoped (and v545, L) -> 0.5 s unscoped (#188 / #189 / #192); any state holding the weapon stack above priority 1 (#242 / #243: abilities, reload) M; reload start 1.35 s (#303) H; releasing the button H | |
| Grapple cast | press -> **0.15 s** wind-up (#101) -> hook **40 m/s** up to **20 m** (1AB0, #15) = 0.5 s (anim 003B 0.5 s) H | |
| Grapple pull | **20 m/s** (212B -> v1002, MoveTo #21) to the ledge point if one is found within 3 m of the hook point (#138), else the hook point; only if she is 2 m or more away (#266) H | |
| Grapple end | MoveTo arrives (fields 0.3 / 1.25 / 0.3 / 100: L tolerances) -> **0.5 s** end phase (#30) with **STU_1F5ED928 0.2** toward the point (M: vault / kept momentum) and anim 009C (0.6 s); runs with or without a ledge M | |
| Grapple jump | **JUMP cancels** from 0.02 s into the pull (#178 -> #24) and starts the cooldown (#179) H | |
| Grapple cooldown | **10 s** (1AAF), set **when the pull ends** (#160), on a jump cancel (#179) or an abort (#187) H; **a miss (or a hit under 2 m) reels the claw back at 120 m/s with no cooldown** (#288 / #162 / #22, no v61 node) H/M | |
| Venom Mine throw | anim 001E (1 s); **released 0.2 s** in (#41); **20 m/s**, gravity **9.8** (#109), size 0.2 m, life 10 s (volley #21) H | |
| Venom Mine cooldown | **15 s** (1AD7), set **0.8 s after the throw = 1.0 s after the press** (#42 -> #87) H | |
| Mine | **arms 1 s** (v2277, 0057 #44); **trigger 3 m** (1E96, #54), **burst 4 m** (1E97, #39), enemies only; **1 health** (#59); destroyed 1.5 s after it bursts (#68 / #70, M); **one at a time**: a new mine makes the old one destroy itself (messages 0040 / 003F, 0056 #52 / #57, 0057 #74) H | |
| Poison | **125 over 5 s** (1AD8 25 x 1B7E 5, 0073 #48, absolute total), loadout 0023, no crit (M); a second application doesn't add (v11844 divides, M) H | |
| Reveal | effect 02FF on each poisoned target while the poison runs (5.1 s, #69) and 1 s after a death, shown to the mine's owner only (#62 m_89C93A57 = owner != local) M | |
| Sticks to | stick probe r 0.19 (0057 #127), tag 02BD decides (L); W: surfaces only | |
| Infra-Sight | cost **1400** (0B01, M no reader); cast **0.65 s** (#76 / #75, anim 0029); ult charge spent at the cast's start (#105); **15 s** (1AD6 -> v567, #58); reveals every enemy within **200 m** of her (#13 TrackTargets) with 0015.053 (#38, M: outlines and health bars) to her team (#116 tags 0477, L whom); ends on her death (#113); no self buff found H/M | |
| Quick melee | 40 (02D0), anim 000D / 022A 0.76 s, hit 0.2 s in, 0.33 s more, 1 s cooldown (0043) H/M | |
| Sharpshooter | 01C7 #2832 on tag 1188: a critical hit on an enemy takes **damage x 0.0075 s** off **Grappling Hook** (005A #407 / #405); Venom Mine only with tag 1157 (not hers) H | |
| Scoped Efficiency | scoped cost **3** (51EF #343 v65941) H | |
| Sniper's Instinct | charge **x2** (v37621 = 1 added to the rate) for **2 s** after the pull ends (51EF #354 onFalse of v45962 -> #355 2 s; v45962 set during the pull, 005A #419) H/M | |
| Widow's Bite | charge max **12.5** (001B #742): 1 -> 12.5 in 1.15 s, **150** at full; **pierces while the charge is above 100 %** (#740 / #746 -> volley #52, pierce 99) H/M | |
| Seeker Mine | mine 59BF: arm 1 s, **10 m** (#164 / #165), a dart **6B75** per enemy at **15 m/s** (#175), **75 over 5 s** (59C0 #48), repeat 2 s (#184, M), **re-arm 8 s** (#82); no burst H | |
| Perk XP | 1410 (12E4) / 1410 + 1920 (12E5) = 3330 (4BE6 #8) M | |
| Not live? | 51EF #325: a scoped hit (id 2D4A) on an enemy takes **4 s x charge fraction** off Grappling Hook (message 2623), no perk gate found, in no public source (L: don't build) | |

Against the stat rows (lostats): 14 hip / 20-40 m / 3 deg / 11 shots / 35 ammo / 1.5 s; scoped 50-70 m / 0.5 / 5 ammo / 0.04 m; hook 20 m /
"17 / 20 m/s" (the graph reads only 212B 20; 1831 17 "Updraft Height" is in the row only) / 10 s; mine 15 s / 25 / 3 / 4 m / 5 s / health
"1000 / 1": the 054C 1000 is a stale value shared with B.O.B., the mine's pool is 1; Infra-Sight 15 s; Sharpshooter 0.75 %; health 225.

## Data vs the public sheet ([widowmaker.md](widowmaker.md))
| | Data | Public (W / BZ) | Keep | Conf |
|---|---|---|---|---|
| Health | 225 | 225 | 225 | H |
| Hip | 14, 10 /s, 20-40 m, x2 head | same, floor 30 % | data; floor 30 % from W (data unreadable) | H (floor L) |
| Hip spread | 3 deg, 11 shots, 0.2 s then 4 /s | 3 deg after 11 (BZ said 2) | data 3 deg (BZ's 2 never went in, as W says) | H |
| Reload | 1.5 s, ammo at 0.825 s | 1.5 s, ammo at 0.816 s | data 0.825 (W's 0.816 is a frame-measured 0.825) | H |
| Scope in / out | 0.333 s | 0.32 s | data 0.333 | H |
| Charge | 0.9 s, 12 -> 120 linear, from v174 = 1 | 0.9 s, 12 -> 120, +1.08 / % | same | H |
| Scoped falloff | 50-70 m to 0.5 | 50-70 m to 50 % | same | H |
| Scoped headshot | 1.25 x default 2 | 2.5x | 2.5x | M |
| Scoped move | v23184 += 0.65 | -65 % (1.925 m/s) | -65 % | H (value) / M (field) |
| Recovery | 0.5 s | 0.5 s | same | H |
| Grapple | 0.15 s wind-up, 40 m/s hook, 20 m, 20 m/s pull, cd on end, miss free | 20 m, 20 m/s, miss no cooldown, keeps momentum | data | H |
| Grapple cooldown | 10 s | 10 s | 10 | H |
| Mine throw | released 0.2 s in, cd 1.0 s after the press | 0 s cast + 0.992 s recovery | data (release 0.2 s); the 1.0 s matches W's recovery | M |
| Mine arm | 1 s from the mine's state | 1.4 s | data 1 s after it sticks (W's 1.4 likely from the press: 0.2 + flight + 1) | M |
| Mine radii, health, poison | 3 / 4 m, 1, 125 over 5 s | same | same | H |
| Mine reveal | to her, while poisoned (5.1 s) | to her, time not given | data | M |
| Infra-Sight | 0.65 s, 15 s, 200 m, 1400 | 0.64 s, 15 s, global, 1400 | data (200 m covers any map) | H (cost M) |
| Widow's Bite pierce | above 100 % | at 125 % | data (above 100 %), check in a Workshop log | M |
| Seeker Mine | 10 m, 75 / 5 s, 8 s re-arm | 10 m, 75, 8.2 s | data 8 s | H |
| Sniper's Instinct | x2 for 2 s from the pull's end | 100 % faster 2 s after the hook | same | H/M |
| Sharpshooter | crit x 0.0075 s off the hook | same | same | H |

## Anim aliases (for the clip maps)
| | Alias (graph #node) | Timing |
|---|---|---|
| deploy | 00A9 (0015) | 0.5 s |
| scope in / out | **045D** (001B #285) / **045E** (#286) | 0.5 s clips, priority 90; categories 003D / 003E while scoping, **0081** scoped pose (#202) |
| hip / scoped fire | no StateAnim: volley recoils x71E73343 (hip #17), x1B6E2DF5 (scoped #52 / #745 / #750) | |
| reload | **0004** (001B #172) | 1.5 s |
| hook fire | **003B** (005A #119) after category 0052 (0.15 s wind-up, #59) | 0.5 s |
| hook pull | **002B** (005A #120), category 0051 at its start (#61) | 3 s loop |
| hook end / vault | **009C** (005A #122) | 0.6 s |
| hook miss | category 0054 (005A #91) | |
| mine throw | **001E** (0056 #38) | 1 s, release 0.2 s |
| mine unfold (on 00AE) | 001E (0057 #78) | 2 s |
| ult cast | **0029** (0067 #20) | 0.65 s |
| quick melee | 000D / 022A (0043) | 0.76 s |

## Note for Ashe
Ashe's 14FC #565 STU_3716E1D4 (0.6 / 0.6, "or v16954 / v24101 if set") is the same node type as Widow's #115, which herosettings shows is
the **zoomed aim sensitivity**. [ashe-map.md](ashe-map.md)'s "Scoped move x0.6 (M)" is likely that, not a speed; her real scoped slow would be
a v23184 add like Widow's 1E5A (not checked in her graphs).

## Open
- Hip falloff floor (v37674), the zoom's unit, v545 (the jump-unscope flag), whether abilities unscope at priority 1.
- The grapple MoveTo's end fields and the 0.2 s end push; what the hook can grab (filters 0018.097 / tag 0356).
- The mine's stick rule (tag 02BD), poison stacking, the Seeker Mine's 2 s repeat, the 51EF #325 scoped-hit refund, who sees Infra-Sight.
