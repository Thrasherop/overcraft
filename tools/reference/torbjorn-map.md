# Torbjörn data map, from Overwatch's game data (build 154088)

Read 2026-11-14 offline and read-only through TankLib/DataTool on the archived CASC copy `E:\Overwatch Archive\build-154088`
(2026-10-06), with Overwatch and Battle.net checked closed before every run, as [illari-map.md](illari-map.md) /
[junkrat-map.md](junkrat-map.md). Confidence: **H** read in the data, **M** one link inferred, **L** guess. Public numbers:
[torbjorn.md](torbjorn.md). The node-by-node reading this map condenses: `E:\Overwatch Archive\research\torbjorn\work_kit\kit_torbjorn.md`
(ids, values, Rivet Gun, Forge Hammer, perks, role), `kit_turret.md`, `kit_overload_ult.md`; graphs `work_kit\c_<g>.txt` /
`decomp_<g>.txt` / `edges_<g>.txt`, values `work\varvalues_38D0_out.txt` / `varvalues_38EA_out.txt`, `work\gamevalues_log.txt`,
`loadout_stats.json`. Scripts: Illari's, retargeted (`scripts\*_torbjorn.py`, `scripts\kit\`, `scripts\sounds\`). Any patch after the
build's date (2026-10-06) wins over these numbers: check torbjorn.md. No Workshop recording yet.

## Ids
- Hero **0006.075**, entity **28B6.003** (Gameplay3P), 1P **2A6D.003**; category 084E (Damage); tag **116F** = Specialist (01C7 #2567).
  Graphs: **1683** Rivet Gun (primary and alt; weapons manager 0015), **168C** Forge Hammer, **16C5** Deploy Turret, the turret's **1688**
  (state, build, levels, targeting reads) / **1689** (shots) / **38EA** (values), **1721** Overload, **16DA** Molten Core with the pool's
  **16DD** and the burn status 030C.053's **16DE**, **51F6** perks, **38D0** values, **1684** hero bookkeeping (kills, voice, UI), 01C7 shared.
- Entities: turret thrown **2931** -> deployed pet **28B7** (shot **2A69**); rivet **2A66**, pellet **13ED**; slag **2902**, pool pet
  **2903**. 6C56 / 749F / 78CD / 468F are turret variants behind .134s not in his perk list (Stadium, M).
- Buttons: PRIMARY xD128396E (Rivet Gun; hammer swing; slag), SECONDARY xB42D90B7 (alt fire; slag), RELOAD x5D2A8B7F, ABILITY 1 xCBF0F815
  (Deploy Turret; again: recall), INTERACT x19C8495B (recall), ABILITY 2 x2ADDA7B1 (Overload), ULTIMATE x2DFEDC52 (Molten Core);
  buffers 0.15. Weapon swap to the hammer: 168C #827 xD19BBFFC (L).
- Perks (005D.177): **265C** Hammer Time, **28BC** Pre-Heated (minor); **2501** Overloaded Turret, **2502** Anchor Bolts (major).
- Models (the classic skin's, from its .owentity files; base in brackets): 3P **EBD2** (08D5), 1P **EDA5** (0910), Rivet Gun **DB4F**
  (091B; entity 0147, hp 000A, var 1D35), Forge Hammer **EED9** (0A2A; entity 01BC, hp 000A var 12A4 in hand, hp 0010 var 1DAD), turret
  **1000C** (092E; deployed 28B7 and thrown 2931 alike). Rivets, pellets, turret shots, slag, pools and Overload are effects only: no model.
- Clip maps: [torbjorn.txt](../clipmaps/torbjorn.txt) (3P, 177 clips, set 0997 + the hero file's 0F17 / 0F37),
  [torbjorn_1p.txt](../clipmaps/torbjorn_1p.txt) (101, set 0998), [torbjorn_weapon.txt](../clipmaps/torbjorn_weapon.txt) (Rivet Gun, 18,
  set 008D), [torbjorn_hammer.txt](../clipmaps/torbjorn_hammer.txt) (6, set 008E, + March of the Turrets' 2),
  [torbjorn_turret.txt](../clipmaps/torbjorn_turret.txt) (49, set 0996), [torbjorn_clips.txt](../clipmaps/torbjorn_clips.txt) (emotes),
  [torbjorn_intro_clips.txt](../clipmaps/torbjorn_intro_clips.txt) (Heroic). Sound maps: [torbjorn.txt](../soundmaps/torbjorn.txt) (voice,
  38 events), [torbjorn_sfx.txt](../soundmaps/torbjorn_sfx.txt) (59 events). Emotes: `emotes/torbjorn.json` (14).

## Data
| | Data | Node | |
|---|---|---|---|
| Health | **225 + 75 armor** (0740 / 0741), melee 40 (02D0), speed 5.5 (202B); mover **1.7 m**, r 0.5, crouch 1.25 | 0DC8, 28B6 | H |
| Rivet primary | **70** (0742), 1 ammo, projectile 2A66 **70 m/s** (0745), gravity **9.8**, size 0.1 (290D), life 20 s | 1683 #171 / #299 | H |
| Rivet falloff, crit | none (mode lists 073A / 073B main 0 -> x1); m_66D29548 = 2 read as headshot x2 | #171 | M |
| Rivet fire rate | next shot **0.51 s** after (v173), x attack speed v21541 | #237 / #96 | H |
| Ammo, reload | **18** (0744), **2.0 s** (0747), ammo back at 0.5 x = **1.0 s**; R only when not full | #80 / #141 / #204 | H |
| Alt fire | **10 pellets x 12.5** (1AB1 / 0743), **120 m/s** (0746), no gravity, size 0.05 (28A5), life 10 s; ring of 1 + 4 + 5 | #178 | H/M |
| Alt spread, falloff | **4 deg** (1AB7); **10 -> 20 m** (1AB2 / 1AB3) to **x0.3** | #178 | H/M |
| Alt rate, cost | next shot **0.7 s** after; **2 ammo** (131B) | #238 / #405 | H |
| Forge Hammer | **70** (1AB6), swing **0.6 s** x attack speed, hits in a 0.2 s window; sphere r **1.0** at (0, 1.2, 1.5) m: reach ~2.5 m | 168C #374 / #350 / #256 | H/M |
| Hammer repair | **50** (08F8) to his turret within 1.5 m, when built and damaged | 168C #326 / #237 | H |
| Turret throw | anim 00F3 then 00A9; thrown **0.05 s** in, cast lock 0.5 s; **17 m/s** (10FA), gravity 20, life 60 s, hit 50, size 0.35 / 0.15 | 16C5 #475 / #463 / #465 | H/M |
| Turret build | level 1 -> 2 over **2.5 s** (091A), anim 00DC | 1688 #103 / #179 | H |
| Turret health | **225** (075D); a second pool of **75** (078C: armor or shields, L) | 38EA #163 | H/M |
| Turret shot | **12** (0765; 11 (0766) under an unread condition), **2 /s** (0.5 s); range 40 m (1B0C), shot reach 100 m (1AFC) | 1689 #161 / #219, 1688 #792 | H |
| Turret level 3 | +150 pool (M armor), **4 /s** (0830), barrels alternate hp 5 / 7, anim 00DE 1.7 s | 1688 #67 / #182 | H/M |
| Turret limit, recall | one; a new one destroys the old; recall on ABILITY 1 / INTERACT (not while building) | 16C5 #563 / #513 | H/M |
| Turret cooldown | **12 s** (07DF) at destruction or recall if in combat (fired in the last 1 s / damaged in the last 3 s, 1AF4), else **6 s**; 6 s set at the cast | 16C5 #600 / #466 / #531 | H/M |
| Turret targeting | see below | 1688 | H/L |
| Overload | **6 s** (0771) from the press; **100 overhealth** (1AB9), no decay; move **+30 %** (1AB8); attack speed **+43 %** (1ADB) rivet and hammer; reload **1.4 s** (x 0.7, 1B13); **+6 ammo** at once (1ABF) | 1721 #79 / #171, 1683 #398 | H |
| Overload cast, cooldown | **0.7 s** lock (anim 0100); **10 s** (0772) from its end | 1721 #88 / #243 | H |
| Molten Core cost | **2350** (0AFF; alt 4350) | 28B6 -> v338 | H |
| Molten Core cast | **0.75 s** (anim 01B8), then held (0175) **6 s** (074C) or until the slag is spent; end 0.35 s (01B9 / 05BA) | 16DA #462 / #467 / #416 | H |
| Slag | **10** (074E), PRIMARY or SECONDARY at **6.5 /s**, **40 m/s** (074D), gravity 30 (M), size 0.2 / world 0.1, life 10 s, hit **25** (1ADC) | 16DA #382 | H/M |
| Pool | forms in 0.5 s, lasts **10 s** (1AE1), radius **2.4 m** (1D4D), floor / wall / ceiling | 16DD #44 / #233 / #211 | H |
| Burn | **100 /s** (1ADF) + **190 /s** more to armor (1AE0), enemies inside; stacking not read | 16DE #148 / #147 | H/M |
| Anchor Bolts | throw x1.25 (21.25 m/s), sticks to walls / ceilings | 51F6 #354, 16C5 #465 | H/M |
| Overloaded Turret | turret at level 3 for Overload's 6 s (text: 5 s), then level 2; +40 % turret attack speed for 4 s after his hit (M) | 51F6 #361 / #444 / #339 | H/M |
| Hammer Time | +20 % move speed with the hammer out (not in Molten Core) | 51F6 #422 | H/M |
| Pre-Heated | Molten Core starts Overload, no cooldown | 1721 #284-#297 | H/M |
| Specialist | elimination -> reload speed **+50 %** for **3 s** (23D5 / 23D6) | 01C7 #2567 | H |
| Damage role | target hit by a Damage hero: a combat mod -0.3 (1A33) for 3 s (1E68), read as a healing cut | 01C7 #2702 / #881 / #610 | M/L |
| Perk XP | 1750 / 1750 + 2625 = 4375 (12B3 / 12B4) | 38D0 | H |

**Turret targeting (as the data has it):** the graphs never pick a target. 1688 reads the turret's current AI target (STU_A37A910D on
self), set by a native component. It fires only once built; its aim ray (StateTrackRay #293 from hp 0001) is filtered to enemies within
40 m; the shot flies to 100 m. Nothing in the graphs or in 28B7's components gives nearest-first, line-of-sight priority, priority on
Torbjörn's target, a lock-on time or a turn rate (L: those are the native AI's).

## Clips (the Java side's states)
Weapon branches as prefixes: plain = Rivet Gun (dfE4 default), `hammer_` (cat7D), `molten_` (cat8D, Molten Core), `overload_` (cat8E).
| | 3P (torbjorn.txt) | 1P (torbjorn_1p.txt) |
|---|---|---|
| idle | `idle` 7F8, `hammer_idle` D52, `molten_idle` 5FFF | `idle` 7D3, `hammer_idle` 936, `molten_idle` 5FCC |
| run | `run` F07 + 8-way (`run_back` F0D, `strafe_left` F13, ...), `hammer_run` E1A, `molten_run` 601E | `run` 7DD, `hammer_run` 940, `molten_run` 6138 |
| jump / fall | `jump` FBE, `jump_apex` FC0, `fall` FBF (+ `hammer_` / `molten_`) | `jump` 7D0, `fall` 7D1 (+ `hammer_` / `molten_`) |
| crouch | `crouch` 3A51, `crouch_walk` 12BB | `crouch` 7C7, `crouch_walk` D2A |
| fire | `add_fire` FCA, `add_overload_fire` 1107, `add_molten_fire` 619A | `add_fire` E2D, `add_overload_fire` 1109, `add_fire_alt` 7CB (L) |
| reload | `reload` FED (2.0 s) | `reload` 7D6 (2.2 s) |
| hammer | `hammer_swing` 1010 | `hammer_swing` D8E, `overload_hammer_swing` 37E9 |
| turret throw | `turret_throw` 1003 | `turret_throw` 5FB9 |
| Overload | `overload` FF9 (0.7 s) | `overload` F3A |
| Molten Core | `ult_cast` 60E4, `ult_end_2` 60FE | `ult_cast` 5FD2, `ult_end` 5FD3 |
| melee, draw | `melee` 109E, `draw` FFD, `hammer_draw` FFE | `melee` 7D4, `draw` 7D8, `hammer_draw` 93B |
| Rivet Gun | `idle` D11, `add_fire` E2E, `add_overload_fire` 110E, `reload` D0B (torbjorn_weapon.txt) | |
| turret | `build` 5F79, `upgrade` E46, `idle` E5D / `l2_idle` E5C / `l3_idle` E5F, `add_fire` E4E / `add_l2_fire` E5A / `add_l3_fire` E61, aim layers `add_*aim_tree*` (torbjorn_turret.txt; the level reading of dfE5 cat90 / 91 / 92 is M) | |

## In the local pack (`local-assets/pack`, never git)
- Models (`assets/overcraft/hero_models/`): `torbjorn.glb` (3P, 177 clips), `torbjorn_1p.glb` (101), `torbjorn_weapon.glb` (18),
  `torbjorn_hammer.glb` (8), `torbjorn_turret.glb` (49), `torbjorn_clips.glb` (62 emote clips), `torbjorn_intro_clips.glb` (`potg_intro`
  ... `potg_intro_4`, `potg_intro_28`); emote props `prop_a2a.glb` (the hammer), `prop_541b.glb`, `prop_4ef3.glb`, `prop_1a146.glb`; glows
  `torbjorn_glow.json`, `torbjorn_1p_glow.json`, `torbjorn_weapon_glow.json`, `torbjorn_hammer_glow.json`, `torbjorn_turret_glow.json`.
- UI (`textures/gui/heroes/torbjorn/`): `portrait.png`, `portrait_close.png`, `head.png`, `bust.png`, `card.png`, `fullbody.png`,
  `silhouette.png`, `silhouette_small.png`; `abilities/rivet_gun.png`, `secondary_fire.png`, `forge_hammer.png`, `deploy_turret.png`,
  `overload.png`, `molten_core.png`; `perks/anchor_bolts.png`, `overloaded_turret.png`, `hammer_time.png`, `pre_heated.png`.
- Sounds (event names the code plays): `torbjorn.rivet.fire` / `_3p` (M: no volley names one; the shot's one-shot slot 185B / 1896),
  `rivet.alt_fire` / `_3p` (L), `rivet.hit_enemy` / `_3p`, `rivet.hit_world`, `rivet.alt_hit_enemy` / `_3p`, `rivet.alt_hit_world`,
  `torbjorn.reload` / `_3p`, `draw_rivet` / `draw_hammer` (+ `_3p`); `torbjorn.hammer.swing` / `_3p`, `hammer.hit` / `_3p`, `hit_world`,
  `repair` / `_3p`, `repair_full`; `torbjorn.turret.throw` / `_3p`, `flight`, `land`, `deploy`, `build`, `build_step`, `fire`, `hit`,
  `upgrade`, `downgrade`, `damaged`, `destroyed` / `_3p`, `recall`, `overloaded`; `torbjorn.overload` / `_3p`, `overload.loop` / `_3p`,
  `overload.end`; `torbjorn.ult.cast` / `_3p`, `molten.loop` / `_3p`, `molten.fire` / `_3p`, `flight`, `hit`, `impact`, `pool`,
  `pool_loop`, `burn` / `_3p`, `burn_hit`. No melee, turret-acquire or pool-end sound in his data. Voice: `torbjorn.voice.ultimate` (his own,
  1862), `torbjorn.ult.friendly` (048B), `torbjorn.ult.enemy` (014A), `voice.overload`, `hammer_kill`, `turret_kill`, `turret_destroyed`,
  `turret_attacked`, `turret_repair` (M), `turret_upgrade`, `turret_build` (L), `enemy_turret_destroyed` and the shared moments.

## Open
- The turret's target choice, turn rate and acquire delay are native (not in the graphs): W / a Workshop recording decides them.
- The 11 vs 12 turret damage condition, the 75 / 150 pools' type, Anchor Bolts' 0.25 reader in 16C5, 1689 m_1DB42474 = 15.
- Molten Core: pools stacking (status 030C), 1AE2 = 100 and the slag's m_B4454CD7 = 20, a 4 m impact block with no damage.
- The intro set's clip order (L), 0794 / 0795 / 079E readers, the hammer swap button (xD19BBFFC, L), the Damage role's 0.3.
- Emotes: 14 in emote_data (all unlock groups); `torbjorn_clips.glb` holds every group's emote clips (62).
