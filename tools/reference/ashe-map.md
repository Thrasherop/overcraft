# Ashe data map, from Overwatch's game data (build 154088)

Read 2026-11-14 offline and read-only through TankLib/DataTool on the archived CASC copy `E:\Overwatch Archive\build-154088`
(2026-10-06), with Overwatch and Battle.net checked closed before every run, as [illari-map.md](illari-map.md). Confidence: **H** read
in the data, **M** one link inferred, **L** guess. Public numbers: [ashe.md](ashe.md) (written separately). The node-by-node reading this
map condenses: `E:\Overwatch Archive\research\ashe\work_kit\kit_ashe_hero.md` (her graphs) and `kit_ashe_bob_dynamite.md` (the dynamite
and B.O.B.); their graphs `work_kit\c_<g>.txt` / `decomp_<g>.txt` / `edges_<g>.txt`, values `work\varvalues_35A6_out.txt` (hers) and
`varvalues_35B3_out.txt` (B.O.B.'s), `work\gamevalues_log.txt`, `work\lostats_out.txt` (the in-game stat rows). Scripts: Illari's,
retargeted (`scripts\*_ashe.py`, `scripts\kit\`, `scripts\sounds\`). No Workshop recording yet.

## Ids
- Hero **0200.075**, entity **251C.003** (Gameplay3P), 1P **251D.003**; category 084E, tags 015A / 0426 / 0427 / 0661 / 11A0 / **1188**
  (Sharpshooter). Graphs: **14FC** The Viper and Take Aim (weapons manager 0015), **1513** Coach Gun (and Double-Barreled), **14FD**
  Dynamite throw, **1502** the dynamite (entity 2533), **150F** the burn (run on each target), **1536** B.O.B. call, **1537** B.O.B.'s
  brain, **153B** his arm cannons, **153A** his targeting (entity 25CB), **17F6** the knock-up's immobilize (on each target), **51E3**
  perks, **35A6** her values, **35B3** B.O.B.'s values, 14FF hero tips / voice moments, 3DCE ult-ready ping, 01C7 shared (Sharpshooter).
  Not hers: 28B7 / 43A0 (pets only shared 25B4 / 391E name), 2593 / 66FA (other heroes' entities reached through shared graphs).
- Buttons: PRIMARY xD128396E (hip shot 14FC #767; scoped shot #549; interrupts a reload #695), SECONDARY xB42D90B7 (Take Aim: hold #626,
  or toggle #629 under option v16955; interrupts a reload #704), RELOAD x5D2A8B7F (#547), ABILITY 1 xCBF0F815 (Coach Gun 1513 #84;
  Double-Barreled recast #515), ABILITY 2 x2ADDA7B1 (Dynamite 14FD #84 / #301; Remote Detonator 51E3 #368), ULTIMATE x2DFEDC52 (B.O.B.
  1536 #101).
- Loadouts: 020A The Viper, 0209 Take Aim, 020D Coach Gun, 020B Dynamite, 020C B.O.B., 0DB2 Hero Stats, 0DDD Sub-role Sharpshooter.
  Perks (004F.177 / loadouts): **2686** Remote Detonator (0CCF), **28B4** Double-Barreled (0E4F) (minor); **2566** Viper's Sting
  (0B3E), **2565** Airburst (0B40) (major) (the wiki and Perk.java; this line had the tiers swapped: critique 2026-11-17).
- Models (skin Overwatch): body **105D5.00C** 3P (base 7C02, look A49D, set 08E2), **105CE.00C** 1P (base 7C04, set 08E1); **The Viper
  1079F.00C** (entity 2523, base 7B28, set 0979) on hardpoint 00C9, var 4082; **Coach Gun 107A0.00C** (entity 2560, base 7B59, set 09AC)
  on 000B, var 40C1; **Dynamite 107A8.00C** (base 7C61): in her hand entity 2B66 on 000B, var 41F4; thrown entity **2533** (projectile,
  health component, contact set 067F, graphs 0033 + 1502); **B.O.B. 10800.00C** (pet entity **25C3**, base 7323, look B2E8, set 0966,
  mover 2.3 m / r 0.7, voice set 01CD empty). Shot visuals 280E / 2999 / 29A1 / 2B61 (projectile visuals, no models).
- Clip maps: [ashe.txt](../clipmaps/ashe.txt) (3P, 191 clips, set 08E2 + 0F60 / 0F2A / 09C6 / 0C88 / 4307), [ashe_1p.txt](../clipmaps/ashe_1p.txt)
  (90, 08E1), [ashe_viper.txt](../clipmaps/ashe_viper.txt) (20 + 5 emote clips, 0979), [ashe_coach_gun.txt](../clipmaps/ashe_coach_gun.txt) (3,
  09AC), [ashe_bob.txt](../clipmaps/ashe_bob.txt) (84, 0966 + the 8 run clips in no set), [ashe_clips.txt](../clipmaps/ashe_clips.txt) (emotes),
  [ashe_intro_clips.txt](../clipmaps/ashe_intro_clips.txt) (Heroic). No dynamite clip map: the dynamite has no set (static). Sound maps:
  [ashe.txt](../soundmaps/ashe.txt) (voice), [ashe_sfx.txt](../soundmaps/ashe_sfx.txt) (58 events). Emotes: `emotes/ashe.json` (20).

## The numbers (data, the node that reads it)
| | Data (value id, node) | Used |
|---|---|---|
| Health, melee, speed | 250 (0538, 35A6 #2), melee 40 (02D0), 5.5 m/s (202B); regen delay 6 s (0B0F); mover 2.0 m, r 0.5, crouch 1.25 H | |
| Hip damage | **35** (053A -> v3633, volley 14FC #770) H | |
| Scoped damage | **75** (0541 -> v2432, volley #555); 72 (286D) replaces it under v61743 (mode unknown, L) H | |
| Fire interval | hip **0.25 s** (#672, v20448 = v30010); scoped **0.65 s** (#773, v29977 = v30009), the timer paused while scoping in / out (#802, v8088: M) H/M | |
| Scope in / out | in **0.2 s** (#568, v169 rate 5, anim 045D), out **0.1666 s** (#569, anim 045E); no fire during either (v414) H | |
| Zoom | v172 = **40** (#554 / #552: L an FOV of 40 deg), full halfway through the scope-in M | |
| Scoped move | x**0.6** (#565 STU_3716E1D4 m_69A20070 / m_10705336, only at v169 >= 1) M | |
| Spread | hip **0 -> 2 deg** (1C9A / 1C99, v229 / v230); scoped none (#668); back to 0 at 2 /s (#677) after 1.5 s (#676); per-shot growth not in the graph H/M | |
| Falloff | hip **20-40 m** (1151 / 1150, v41493 / v33006), scoped **35-55 m** (1152 / 1153, v60729 / v60728), floor **x0.3** both H/M | |
| Shot size, crit | 0.04 m (28A2); crit field 0.0 = default x2 (M) | |
| Ammo | **12** (053B) H | |
| Reload | starts once v20448 <= 0.05; startup **0.5 s** (113B, #681 Wait, anim 0084 cat 0235 / 006E within 5 s of the last #717), **first shell at the startup's end** (#722), then each shell **0.25 s** (113C, #688 Wait), shell added at its end (#685); 12 from empty 0.5 + 11 x 0.25 = 3.25 s; PRIMARY / SECONDARY with ammo > 0 interrupt into an **end 0.3 s** (#693, anim cat 0082) H | |
| Coach Gun | **6** per pellet (0545) x **15** (1A44, v18199), **20 deg** (1307) in rings 1 / 3 / 4 / 7; pellet 0.4 (28DE, M); crit field 1 = no headshot (M) H | |
| Coach Gun push | self **18** (0EAF, #141); enemy pair m_E33EFEE1 **20** (literal) / m_583C208B **5**, push vector 20 (1319); x1 to 8 m, x0 at 15 m; ~1 s of 0.01 air control after (M) H | |
| Coach Gun timing | cast **0.55 s** (#419, anim 001E), blast **0.165 s** in (#109); cooldown **10 s** (131F) H | |
| Dynamite throw | thrown **0.15 s** in (14FD #41, anim 0479 0.66 s, recovery 0.4 s); **25 m/s** (28C9, #103 / #143), gravity **8** (v7389, M), size **0.1 m** (28C8), life 30 s; cooldown **12 s** (1320, #201) from 0.55 s after the press (#42 -> #87) H | |
| Dynamite fuse | **2.0 s** (1D41 x a Stadium scaler, 1502 #408 / #180), started on landing (message 05D4 sets v1949: M) H/M | |
| Dynamite triggers | fuse end; shot by Ashe only (message 00C8, owner check #166); its own death (v478, #112); message 059D; Remote Detonator; all -> explosion state #117 (0.3 s) H | |
| Explosion | **50** (0546) flat to **1.5 m** (1D40), linear to **20** (0547) at **5 m** (131E), none past (#133); self x**0.5** (#178); no knockback (#207 pushes physics props only); a direct block of 75 (BEC9, #103) in no stat row (L unused) H | |
| Burn | **100** (0548) over **5 s** (1A43) = 20 /s continuous, over-time mode (150F #143); self x0.5 (#239, M); a new burn from the same Ashe replaces the old (M) H | |
| Dynamite health | not read (its health component's value not found) | |
| B.O.B. health | **1000** (054C, 35B3 #2; alt 1500); no armor split in any graph (L) H | |
| B.O.B. cast | **0.5 s** (1536 #107 / #41, anim 0175); he spawns at her feet facing her forward at the cast's start (#149 / #115), acts 0.5 s later (1537 #351); ult charge spent at the cast's start (#106) H | |
| B.O.B. charge | **4 s** (1322, 1537 #363); **15 m/s** (mover #409 m_031FE7EE, against 5.5 standing: M), ~60 m max; stops on a wall (rays 2 m ahead at 0 / 0.75 m, #640 / #641, then 0.3 s), stuck (< 0.05 m per 0.032 s, #649), 0.8 s after an uppercut connects (#362 + #392), barrier-like targets (175 and stop, #603, L) H/M | |
| Knock-up | **120** (054E; 1537 #276 / #302 literal 120), at 2 m, a 4 m 90 deg cone (#252 / #404); pair m_583C208B **12 or 15** / m_E33EFEE1 **0.5** (which is grounded / flying L); immobilize **1.5 s**, **0.5 s** if flying (2405 / 2406, 17F6 #348 / #292) H | |
| B.O.B. guns | **17** (054D) at **8 /s** (0.125 s, 153B) = 136 DPS, spread 1.5 deg, range field 100 (M), no falloff, no headshot (M); x0.85 in some mode (L); targets picked by 153A (~40 m, L) H/M | |
| B.O.B. firing, life | firing **10 s** (1321, #157); despawn anim 0578 (0.5 s) then destroyed (#375 / #388), ~15 s after the cast; on death destroyed at once (effect 2671, #387 / #430); no heal block, no contest node (L) H | |
| Ult cost | **2450** (0ADE, alt 4480): no node of hers reads it; her only "Ultimate Charge Max" not shared with other heroes (M) | |
| Sharpshooter | 01C7 #2580 on tag **1188** -> #2878 v36246 = **0.0075** (23D7); a critical hit on an enemy (#2827) takes damage x 0.0075 s off the **ABILITY 1 (Coach Gun)** cooldown only (#2832, message 2623, applied at 1513 #451); the ABILITY 2 branch needs tag 1157 (not hers) H | |
| Viper's Sting | **25** (51E3 #344, over ~1 s M) after **2 scoped hits** on one target within **3 s** (445B); a scoped miss breaks the chain (M) H/M | |
| Airburst | dynamite radius x**1.4** while airborne (v65826 = 0.4, 51E3 #329, applied 35A6 #93 / #103: 7 / 2.1 m), **+6 ammo** up to the magazine (#359, message 2816) H | |
| Remote Detonator | ABILITY 2 again from 0.15 s after the throw while it lives (51E3 #368 / #369, message 291E) -> explodes **0.5 s** later (1502 #398) H | |
| Double-Barreled | a **2 s** recast window (v65709) after a Coach Gun knockback hit; the real cooldown keeps running (v13062), then v61 = min(v13062, 10 - 2) H/M | |
| Perk XP | 1750 (1296) / 1750 + 2630 (1297) = 4380 M | |

Against the stat rows (lostats): everything matches (35 / 75, 20-40 / 35-55 m, 0-2 deg, 0.5 / 0.25 s reload, Coach Gun 6 x 15 / 20 deg /
18 / 20 / 10 s, Dynamite 20-50 / 1.5-5 m / 100 over 5 s / 25 m/s / 0.1 m / 12 s, B.O.B. 1000 / 17 / 120 / 4 s / 10 s / 1.5 / 0.5 s).
The "Explosion Delay" row pairs 02FF 25 with 1D41 2.0, but no graph reads 02FF; the burn row's alt template says "per second" where the
graph spreads 100 over 5 s.

## Clips (the Java side's states)
| | 3P (ashe.txt) | 1P (ashe_1p.txt) |
|---|---|---|
| idle | `idle` 5E7C (alias 1 hip), `idle_ads` 5ED5 (scoped) | `idle` 5E3C (alias 9) |
| run | `run` 5EA2 + 8-way (`run_back`, `strafe_left`, ...); `run_ads` 5F1C + 8-way | `run` 5E3E, `run_ads` 5F47 |
| jump / fall | `jump` 5EFE, `fall` 5EFF (still); `jump_forward` 5FF2, `fall_forward` 5FF3; `jump_ads` / `fall_ads` | `jump` 5E47, `fall` 5E48; `jump_ads` / `fall_ads` |
| crouch | `crouch` 60AF, `crouch_ads` 6135, `crouch_walk` 6137 (+ 8-way), `crouch_walk_ads` | none of its own |
| fire | `add_fire` 5F50 (hip), `add_ads_fire` 5F77 (scoped) (M) | none in the body set (the Viper's `add_fire` / `add_ads_fire`) |
| ADS | `ads_in` 5F16, `ads_hold` 5FA7 (in no set: the onFinished), `ads_out` 5FA8 | `ads_in` 5A7B, `ads_hold` 60FF, `ads_out` 5A78 (L) |
| reload | `reload_start` 5F7B, `reload_loop` 5F7C (each shell), `reload_end` 5F7D (+ `_down` / `_up`) | `reload_start` 5A09, `reload_start_again` 60D8, `reload_loop` 603E / `reload_loop_2` 6020, `reload_end` 5A0B |
| Coach Gun | `coach_gun` 5F44 (+ `_down` / `_up`) | `coach_gun` 5AA9 |
| Dynamite | `dynamite_throw` 5FA1 (+ `_down` / `_up`) | `dynamite_throw` 5EF6 |
| ult cast | `ult_cast` 5FBB (+ `_down` / `_up`), `ult_cast_end` 5FCE | `ult_cast` 5FEE |
| melee | `melee` 5EFA (+ `_down` / `_up`) | `melee` 5E8C |
| The Viper | `idle` 5FB3, `add_fire` 5F57 (lever, hip), `add_ads_fire` 5F78, `reload_start` / `reload_loop` / `reload_end` (1P) and `_3p`; `emote_dynamite`, `emote_flourish`, `emote_take_a_knee` (+ `_2` / `_3`) | |
| Coach Gun model | `coach_gun` 6179 (3P), `add_coach_gun_1p` 5AAA | |
| B.O.B. | `spawn_drop` 5FE3 (L), `idle` 5ED2, `idle_fire` 5F1E (L), `charge` 0999 (+ `charge_2`..`_8`: an 8-way run in no set, L), `uppercut` 5F2E, `uppercut_end` 5F2F, `charge_stop` 5E78, `charge_timeout` 6062, `aim` 5FCA, `add_base_*` (gun recoil layers, L), `despawn` 604E, `stun` 5F49 | |
| Heroic | `potg_intro` B0BA -> `potg_intro_2` BC6A -> `potg_intro_3` BC6B (H sequence, set 0C88) | |

## In the local pack (`local-assets/pack`, never git)
- Models (`assets/overcraft/hero_models/`): `ashe.glb` (3P, 191 clips), `ashe_1p.glb` (90), `ashe_viper.glb` (25), `ashe_coach_gun.glb` (3),
  `ashe_bob.glb` (84), `projectile_ashe_dynamite.glb` (static), `ashe_clips.glb` (20 emotes, 87 clips), `ashe_intro_clips.glb` (`potg_intro`
  .. `potg_intro_3`); emote props `prop_7c61.glb` (the dynamite, clips 17150 / 17151 / 627E), `prop_85e2`, `prop_fc6d`, `prop_ad27`,
  `prop_1a146`, `prop_1ab3f`, `prop_1cba0`, `prop_1d532`; glows from the glows run.
- UI (`textures/gui/heroes/ashe/`): `portrait`, `portrait_close`, `head`, `bust`, `card`, `fullbody`, `silhouette(_small)`;
  `abilities/the_viper`, `take_aim`, `coach_gun`, `dynamite`, `b_o_b`; `perks/viper_s_sting`, `airburst`, `remote_detonator`, `double_barreled`.
- Sounds: `ashe.viper.fire` / `_3p` (hip), `viper.fire_ads` / `_3p`, `viper.hit_enemy` / `_3p`, `viper.hit_enemy_ads` / `_3p`, `viper.hit_world`,
  `viper.hit_world_ads`, `viper.whiz`, `viper.whiz_ads`; `ashe.ads.in` / `_3p`, `ads.out` / `_3p`; `ashe.reload.start` / `shell` / `end` (+ `_3p`);
  `ashe.coach_gun.fire` / `_3p`, `coach_gun.hit` / `_3p`, `coach_gun.hit_world`; `ashe.dynamite.throw` / `_3p`, `dynamite.fuse`, `dynamite.land`,
  `dynamite.explode` / `_3p`, `dynamite.remote`, `dynamite.ignite` / `_3p`, `dynamite.burn` / `_3p`, `dynamite.burn_stop` / `_3p`; `ashe.bob.cast` /
  `_3p`, `bob.land`, `bob.step`, `bob.charge`, `bob.charge_stop`, `bob.uppercut` / `_3p`, `bob.immobilize` / `_3p`, `bob.fire`, `bob.fire_stop`,
  `bob.hit_enemy` / `_3p`, `bob.hit_world`, `bob.death`; `ashe.melee` / `_3p`. Voice: `ashe.voice.ultimate` (1837), `ashe.ult.friendly` (0A90),
  `ashe.ult.enemy` (0A56), `voice.dynamite`, `dynamite_kill`, `dynamite_shot`, `coach_gun`, `coach_gun_kill`, `bob_end_kills`,
  `bob_end_no_kills`, `bob_destroyed` and the shared moments. B.O.B. has no voice (his set 01CD is empty); no spawn-drop or despawn sound.

## Open
- The ult cost (no reader found), the zoom's unit, the scoped move field pair, hip spread growth per shot, crit field 0 = x2.
- B.O.B.'s 15 m/s charge (one mover field), which knock-up number is grounded / flying, his armor (none in the graphs: the live game
  gives him armor), his charge-run clips (8 in no set, picked by length and footstep effects: L), his gun's range fields (40 / 36 / 11 / 100).
- The dynamite's health / hit size, whether the fuse waits for landing, burns refresh or stack; who besides Ashe can set it off (only
  her shots in the graph).
- The Heroic intro's C1B5 (group 0-36, L the Viper's twin of B0BA) is not exported onto the Viper.
