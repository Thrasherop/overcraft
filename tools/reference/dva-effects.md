# D.Va's ability effects in the game data

What D.Va's effects are made of in real Overwatch, where the extracted files are, and how each would be rebuilt in Minecraft
(a `DvaFx` beside `SojournFx`; the playtest in #150 found the matrix, Self-Destruct's field, the missiles and Boosters still on
Minecraft particles, and no melee). Read 2026-11-16 from the archived offline build 154088. Overwatch and Battle.net were checked
closed (`Get-Process Overwatch, Battle.net`) before the TankLib run, and `owinit.py` refuses on its own. Everything was read only,
offline, through TankLib/DataTool. The repo holds text only: masks, ramps and meshes stay in the owner's archive and reach the game
through the local asset pack. Format: [sojourn-effects.md](sojourn-effects.md). Her data: [dva-map.md](dva-map.md); plan:
[dva-plan.md](dva-plan.md); hub: [dva](../../techniques/heroes/dva.md); method: [rendering](../../techniques/rendering/rendering.md)
"Effects and glows".

Confidence: **H** read in the data, **M** inferred, **L** guess. "Size" is the spawn size (a 1 m quad at size 0.5 is 0.5 m; a
ribbon's size is its width; a mesh's size scales its own extent). "Ours" marks what no data gives. LUT `XXXX rN` = the colour ramp
(the last 4 hex digits of `ramps\8...XXXX.json`) and its row. Proposed palettes are `DVA_*` (stops written from those rows, as
`SojournPalettes`).

## Files and scripts
- `E:\Overwatch Archive\research\dva\fx\DVa\<Ability>\` (`DefenseMatrix`, `SelfDestruct`, `MicroMissiles`, `Melee`,
  `FusionCannons`, `Boosters`): `textures\` (PNG, flipbook frames `_1.._n`), `ramps\` (float LUTs as JSON), `models\Models\<id>.00C\`
  (`.owmdl`), `manifest.json`.
- `research\dva\work\`: `fx_cards.txt` (one card per renderer for 22 effects: what this note is written from), `fx_overview.txt`
  (one line per effect), `fx_digest.txt`, `fx_summary2.txt|json`, `fx_meshes2.json` (mesh bounds), `fx_entities.txt` (her
  projectiles' and mechs' effect slots), `fx_clip_effects.txt` (258 clips with an animation effect, named), `fx_extract_log.txt`.
- Scripts `research\dva\scripts\fx\`: Sojourn's `fx_sojourn_*` copied as `fx_dva_*`, paths retargeted (`fx_dva_extract.py` holds
  her effect list with each one's graph node). Run: `py -I run.py fx_dva_clipfx.py`; `py -I run.py fx_dva_ents.py
  ..\..\work\fx_entities.txt 1E8A 0C18 1000 408E 0407 041F`; `fx_dva_extract.py` (kiriko-anim venv, `-I`); then under `py -I run.py`:
  `fx_dva_summary2.py`, `fx_dva_digest.py`, `fx_dva_overview.py`, `fx_dva_card.py <ids>`. Effects found from
  `work\graph_effects.json` (sound pass) and the nodes in `work_kit\c_<graph>.txt`.
- Counts: **56 effects asked for, 56 extracted (+2 sub-effects 174B, 18FE), 0 missing**. Not followed: 18FE's own sub-effect 4047,
  Stadium-gated nodes (02CC 5F5B / 5F5C / 5F5D / 3CC0, 0B2F 3C64 / 674D / 5D3D / 6774-6776, 047E 3C46, 1128 3D0E / 641F).

## Her look
- **Team teal-green, enemy orange-red (H rows, M roles):** almost every renderer comes as a pair (matrix rings tint (0.14, 2.5,
  1.3) / (4, 0.38, 0.085); absorb beam A35B r3 #70FFB5 / F56B r3 #FFAC70; Self-Destruct D00D r1 teal->yellow / 22DE r1 red->yellow;
  missile flame 593A / 4450; Fusion tracer 07FE r4 / C2B9 r4). As Sojourn: enemy rows drawn blended (`Fx.soft`), team rows added.
- **Hardpoints on the MEKA (H ids from the graphs / ESHT, M roles):** **0079** the matrix and Boosters origin (2.0 m up, 0.73 m
  ahead), **0159** the matrix emitter glow, **003D / 003E** the arms the absorb beams leave from (graph picks 0228 / 0229
  alternately), **007B / 007D / 007E / 0080** the four Booster thrusters (were "missile tubes, L" in dva-map), 0078 / 007A the
  Booster trail ribbons, 007C the thruster nozzle glow, **0005 / 0007** the cannon tips (Fusion alternates on v58), **0075 / 0077**
  the missile muzzles (1128 #357), 0001 the mech root (Self-Destruct). Pellet / missile hp 0006 / 0080 / 0014 are on the projectiles.

## Defense Matrix (02CC; held while v87 > 0, #276)
| Part | Effect | Rebuilt |
|---|---|---|
| **The projected field** | **2D61** (#651 at hp 0079, scaled by v57568 = 1): tube mesh **D6D1** (1.92 x 9.77 x 1.96 m, narrowing to 0.3 m at the emitter, along -Y), masks **20427** (edges dark across it) x **2042B** (soft hump along it), tint (0.05, 1, 3) both teams (H); **seven ring frames D655** (0.96 m hexagonal ring, flat) at **0.35 / 1 / 2 / 4 / 6 / 8 / 9.5 m** ahead, sizes **0.75 / 1 / 1.25 / 1.5 / 1.75 / 2.0 / 0.75**, alpha falling with distance (rows BC -> 59), mask 1298 (solid; 7A9 fade at 8 m), team 55BC/26F6/E8EC.. r1 tint (0.14, 2.5, 1.3), enemy C89A/BBD0.. r1 (4, 0.38, 0.085) (H); two pulse rings D655 0.75 m flying out at **9 m/s, 2 a second** (C37 / C53) (H) | `Fx.attach` held: tube as `Fx.mesh("dva_matrix_tube")` along her aim, very low alpha (ours 0.08); the 7 rings as meshes at those distances, normal = aim; 2 pulses a second at 9 m/s; `DVA_MATRIX` / `DVA_MATRIX_ENEMY` |
| Field pulses | **2D5C** (#381 at hp 0079): mesh D653 (0.96 x 0.49 m) at 1 / 2 m, size 1.15-1.75 by dataflow 65C (0..200, L: energy), one flying out at 10 m/s, 2/s (H) | one pulse ring 2/s at 10 m/s (sizes ours: 1.15 -> 1.75 by energy) |
| Emitter glow | **08C3** (#214, hp **0159**): quads 2F4F0 (star flare) 0.25-0.75 m, 10/s, 0.15-0.25 s; team 6CD1 r3 / enemy B5D2 r3; 2 lights (H) | `brigitte_flare_star` 3 a tick at 0159 (from the mech model; 1P: the cockpit front, ours) |
| Absorb, the beam | **1506** (#208 / #587, each absorbed projectile, 0.167 s): from hp 003D / 003E (alternate) to the hit: ribbons mesh 797 **1.0 m wide** (mask 11C54) and E71 **0.2 m** (1038 x 1344), team A35B r3 + E40F r4 / enemy F56B r3 + 4301 r4; 3 lights 0.15 m at 003D; 4 sounds (H) | two `Fx.trail`s arm -> hit, 1.0 m and 0.2 m, 0.17 s (`dva_absorb_beam`, `juno_ring_noise`), each eaten projectile |
| Absorb, the hit | **1507** (#213): 10 sparks 4C09 0.15-0.2 m, 0.5-5 m/s, stretch 5, 0.1-1 s (team (0.02, 2, 1.4) / enemy (2, 0.09, 0.02) on E3C5 r0); flash flipbook **4AAA** (8) 0.2-0.3 m 0.2-0.35 s. **1670** (#222 / #586, when the projectile has tag 025D: L a bigger kind): 10 + 20/s sparks 122C 0.4 m at 5-10 m/s, flash 4AAA 1-1.1 m (H) | 1507 for every eaten projectile (`cat_soft`, `dva_absorb_flash`); 1670 for ghast fireballs / tridents (ours) |
| Cast | **1637** (#240): sounds only. 1P clips 334E / 331D (1945 / 1944): sounds; 338B (6ABE): camera shake x2 (H) | - |
| 5F59 (#492) / 5F57 (#623) / 5F5A | 5F59: on hp 0001, sparks 177F4 4/s, motes 176B 10-20/s 3 -> 0 m/s, a material effect (L: the mech's "active" shimmer); 5F57 / 5F5A sounds (H) | not drawn (material effects aren't) |

## Self-Destruct (0B2F)
| Part | Effect | Rebuilt |
|---|---|---|
| **The warning field** (3 s fuse) | **1702** (#419 held through StateWait #270, hp 0001; 4.5 s authored): **big flares 34DC growing 4 -> 10 m** (size curve 0:4, 0.5:10), rate 0 -> 20 -> 3 a second over the fuse, 0.1 s each (team 638F r0 #00F2BC / enemy AA5A r0 #F25E00) (H); flares 4A39 (20-frame) 1.5-2 m 4/s + 1-2 burst; 4A39 x 4D4 0.75-1.9 m, rate 0.5 -> 9.25 -> 18/s for 1.77 s; streaks 5438 1-1.5 m 4/s for 2.47 s (team D00D r1 / enemy 22DE r1); **sparks drawn in: 1C00 0.1-0.15 m at -20 -> -30 m/s, 50 -> 400 a second** for 2 s (team C808 / enemy B2F3); core glow spheres 85B 0.2-0.5 m 10/s for 3 s; spheres 85B **1.75-1.8 m** 5/s 0.2 s for 3 s (team (0, 4, 1.5) / enemy (4, 0.33, 0)); beams 44F6 0.3-0.5 m 15/s; lights rising 0 -> 8; a material effect on the mech (H) | `Fx.attach` on the mech for 3 s: the growing flare (4 -> 10 m, ours alpha 0.35), in-drawn sparks (ours: a fifth the count), the 1.8 m core pulse 5/s, 1-2 flares 4/s; `DVA_SD_FIELD`, `DVA_SD_SPARK` |
| **The blast** | **1703** (#269 at the fuse's end, hp 0001): flash sphere **1385 size 3** 0.6 s (A785 r3 / r5); shock ring mesh **2B63 size 6 (7.9 m)** 0.5 s at -0.75 m (E5A6, 6179 r2) and **2.38 (3.1 m)** 0.4 s (299E9 x 2ABC8); flare A7B6 0.5 m; **80 sparks 1F35 at 40-60 m/s**, 0.25-1 s, stretch 8; 80 motes 0.25-0.5 m/s 2-3 s; 2-6 streaks 20-30 m/s, 1.2 s; core E16E 1 m 0.3 s; 18F81 ball; flares 4A39 x 4D4 1.5-2 m 18 -> 0.5/s for 1.27 s from 0.3 s; 3 smoke B08 x 4F0 **3-3.5 m**, 2-2.5 s; 20/s glows 1038 0.26-0.31 m; **two scorch decals 2 m** (5502 + B0A, 2 s; 22BE / 31E8 / 22C0, 4 s); lights 8 -> 2 over 1.7 s; sub-effect **174B**: two ribbons 0.5 m wide (9ED colour strip x 4DB), 60/s, points 1.39 s (H). Sizes are authored, not scaled to the 21 m radius (M) | flash sphere (`cat_sphere`) 3 m, ring 7.9 m expanding, 40 sparks, smoke 3 puffs, decal (ours: a flat `Fx.particle` on the ground, as Sojourn's field decal), 2 rising trails |
| 1749 (#302) | two quads size 2, 2-3 s, ECIM(40) (L: the local player's screen layer, as Sojourn's 4DA2) | not drawn |
| Clips | 3P 3644 (190D) empty; 1P 33CF (1946) one sound (H) | - |

## Micro Missiles (1128; projectile 1E8A, 18 at 14/s, 50 m/s)
| Part | Effect | Rebuilt |
|---|---|---|
| Launch | **1FB0** (1E8A `m_A18274A3`): sphere DBF 1.25 m 0.1-0.15 s (A344), DBF 0.25-0.75 m 0.1-0.2 s (team 9044 green / enemy CD27 orange), smoke puffs 5429 2-4, 1-1.5 m, stretch 7, 0.4-0.7 s (H) | flash + 2 puffs at the muzzle hp 0075 / 0077 |
| **Flight** | **1FA4** (child 1E99): exhaust flame quads 11C86 0.2 / 0.4 m, 20/s, 0.2-0.24 s, 1 m/s back (team 593A / enemy 4450); flame cone mesh **535E** 0.25-0.35 / 0.4-0.6, 25/s, 0.05-0.1 s (D95A / A3F0); glows 1603 0.1 / 0.15 and 4B2E 0.12 / 0.2 at hp 0080 (0.06 m back); sparks 1F35 50/s at 1-5 m/s, 0.25-0.5 s; **smoke trail 4DB x 11BFC (B0B) 60/s 0.15-0.35 m, 0.4-0.6 s, grey (EE6C r6 #717171)** and 150/s 0.2-0.35 m 0.5-0.75 s; glows 139C 15-30/s at 5 m/s back; lights 3 (H). Which pair is team: M | per missile each tick: one flame 0.3 m, the cone mesh, a glow, smoke 0.25 m 0.5 s (a fifth: ours), 1 spark; `DVA_MISSILE`, `DVA_MISSILE_SMOKE` |
| **Hit** | **1FA5** (`m_6CB564CC` = `m_AED73588`): flash 4C0C 0.3-0.5 m 0.05-0.1 s; dome **5374** (2 m wide, 0.8 high) 0.5-0.6 and 0.5-0.8 at 0.13 s; hemisphere 963D 0.17-1.0; **ball 7CDA 1.75-2.25 m 0.15-0.2 s** (~ the 1.5 m splash); fire flipbook **2291** (12) 0.5-1.5 m, stretch 1.9-3.9, 0.15-0.25 s; 15 sparks 4-8 m/s 0.5-1.25 s; 2-3 sparks cone 80 deg; streak 4B2E stretch 6; light 2.5-3.5 for 0.23 s (team B908 r3 / F0E4 r6, enemy r4 / r7) (H) | ball 2 m 0.18 s, dome, fire 2291 1 m, 8 sparks, light; replaces `EXPLOSION` + `FLAME` |
| Cast / volley | 1F99 (#364), 4562 (#362), clips 33B7 / 33B6 / 33B8: sounds or empty (H) | - |

## Quick melee (shared 0043; 40 damage)
- **No visual effect in the data (H):** 0043's effects are a voice stimulus (0A2C) and a sound (6BD4); the melee clips' animation
  effects are empty (mech 3P 341B / 3580 / 357F -> 1CA9-1CAB, pilot 3P 3232 / 363B -> 1977 / 1975, pilot 1P 30B9 -> 1979, Light Gun
  3266 -> 1A6E). The hit's spark is the contact set's (mech 0744, pilot 05EA: not extracted).
- **What's missing is the animation:** mech 3P alias 0xD = **341B (2 s) with 3580 / 357F as the aim-down / up blend** (named `melee`,
  `melee_down`, `melee_up` in `tools/clipmaps/dva.txt`); pilot 3P `melee_stand` 3232 / `melee_move*` 363B 3231 3233 (0.8 s); pilot 1P
  `melee` 30B9 (0.87 s) + Light Gun `melee_sm0` 3266. **The MEKA 1P set 019B has no 0xD clip (H by search)**; its only unmapped clip
  with an effect, 33D0 (0.47 s, one sound, 1947) is the likeliest 1P mech punch (L). 0043 #145 also plays alias 0x22A (in no set
  read). The animator's generic lookup is by the names `melee` / `melee_2` / `melee_3`, so the pilot 3P names would miss it (M, not
  run). Sound: 6BD4.

## Fusion Cannons (050A; pellet 0C18, 11 a volley)
| Part | Effect | Rebuilt |
|---|---|---|
| Per shot | 34F5 / 34DA, 7FFB / 7FFC (#377 / #379 / #457 / #459, by cannon v58 = 0005 or 0007): sounds only (H) | - |
| Muzzle | **18FE** (sub-effect of the pellet's 10BB, hp 0006; L: per pellet or per volley): flame quads 2D278 0.4-0.5 m at 3 offsets 0.05 s (D285 r2 orange -> green); cones **2D85** 1-1.25 (0.72 x 0.83 m) and **EEB** 0.45-0.53 (`juno_cone`) 0.1 s; flipbook **4896** (4) 0.9-1.0 m x2; star 488C (4) 0.3-0.4 m; glows 1603 1.5 m, 21E2 0.5 m, 4B2E 0.38 m; 2 lights (H) | a flash at hp 0005 / 0007 per volley tick: cone + flipbook + glow (ours: once per tick, not per pellet) |
| Tracer | **10BB** (0C18 STUEffectComponent): three billboard-box meshes 2866, 0.1 s, masks 4B20 x 1298, team 07FE r4 #00FFBC / enemy C2B9 r4 #FF4300 (H) | `Fx.trail` 0.04 m (ours), 0.1 s, `dva_fusion_tracer` |
| Hit a hero / world | **15D3**: flames 14F9 (`lw_thorn_flame`) on strips 4B46 / 5AA0 (H, sizes not decoded). **147A**: decal 4C0C 0.075 m 0.67 s, flames, 3 shards mesh **1024** 1.5-3 stretch 6, 0.18-0.32 s (H) | small flame puff + 2 shards; replaces `CRIT` / `SMOKE` |
| 1580 / 15E3 / 00F7 | the mech's and 1P mech's lights 0.25 / 0.5; 00F7 empty (H) | - |

## Boosters (047E)
| Part | Effect | Rebuilt |
|---|---|---|
| **Flight** | **1492** (#162 held, 3 s authored): at **007B / 007D / 007E / 0080** each: flame quads 2D278 0.4 m 10/s and 4C26 (7) 1.0 m 20/s, 0.2-0.24 s, 1 m/s back (3AEC r2 green, 9190 r2 (0.1, 3.7, 1)); glows 1603 0.2 m 12/s; stretched DBF flames size 1 stretch 7.5 0.25 s (54EA (3, 0.6, 0) orange); sparks 1F35 50/s 4-6 m/s; nozzle mesh **292E** 0.375 at 007C (A093 r4); **trail ribbons 797 0.5 m wide from 0078 / 007A, 30/s, 0.25 s** (11D7 x 4DB, 5DDA r4 / r5 teal); light 2.0 (H) | four flames + glows a tick at the thrusters, 2 `Fx.trail`s 0.5 m; replaces `SMALL_FLAME`; `DVA_BOOST` |
| Cast / hit / clips | 1635, 5BF4, 68E6 (a Booster hit), 737C, 1P 3313 (194A): sounds only (H) | - |

## Not done
- Sizes of 15D3's quads, the matrix tube's alpha and the blast's in-game scale (authored sizes taken as is: M). Team vs enemy for the
  missiles' flight pairs (M). Material effects (5F59, 1702 C63) aren't drawn. The hit contact sets (melee, Fusion) not extracted.

## What the builder adds to the pack (all under `E:\Overwatch Archive\research\dva\fx\DVa\`)
Masks (`textures\<12-hex id>.png`; `(n)` = frames), new `dva_*`: DefenseMatrix `20427` dva_matrix_edge, `2042B` dva_matrix_hump,
`1298` dva_solid, `11C54` dva_absorb_beam, `4AAA` (8) dva_absorb_flash. SelfDestruct `34DC` dva_sd_flare, `4A39` (20) dva_sd_burst,
`5438` (4) dva_sd_streak, `1C00` dva_sd_spark_in, `44F6` dva_sd_beam, `460` dva_dot, `E16E` dva_sd_shock, `18F81` dva_sd_core,
`E5A6` dva_sd_ring, `2ABC8` dva_sd_ring_band, `5502` + `B0A` dva_sd_scorch, `B08` + `4F0` (4) dva_sd_smoke, `9ED` dva_sd_trail.
MicroMissiles `11C86` dva_missile_flame, `526C` dva_missile_cone, `11BFC` + `B0B` dva_smoke, `4B2E` (2) dva_flare, `2291` (12)
dva_missile_fire. FusionCannons `4B20` dva_fusion_tracer, `27D2` dva_fusion_cone, `4896` (4) dva_fusion_flash, `488C` (4)
dva_fusion_star, `21E2` dva_fusion_glow. Boosters `4C26` (7) dva_boost_burst, `11D7` dva_boost_trail.
Reused (same ids already in EFFECT_MASKS): `cat_fade` 7A9, `juno_ring_noise` 1038, `cat_strip` 1344, `cat_soft` 4C09, `lw_disc`
122C, `brigitte_flare_star` 2F4F0, `brigitte_noise_fine` 4D4, `cat_cone_line` 16E9D, `glow` A7B6, `spark` 1F35, `sojourn_field_noise`
/ `juno_cloud_noise` 299E9, `juno_jet_glow` 1603, `mercy_noise` 4DB, `mercy_soft` 139C, `cat_dust_puff` 5429, `ana_star_decal` 4C0C,
`lw_beam_cone` 2D278, `lw_thorn_flame` 14F9.
Meshes (`models\Models\<id>.00C\<id>.owmdl`, EFFECT_MODELS): DefenseMatrix `D6D1` dva_matrix_tube, `D655` dva_matrix_frame,
`D653` dva_matrix_pulse; SelfDestruct `2B63` dva_sd_ring, `85B` dva_sd_ball; MicroMissiles `535E` dva_missile_cone_mesh, `5374`
dva_missile_dome, `7CDA` dva_missile_ball; FusionCannons `2D85` dva_fusion_cone_mesh, `1024` dva_fusion_shard; Boosters `292E`
dva_boost_nozzle. Reused: `cat_sphere` 1385, `cat_bubble` DBF, `juno_cone` EEB.
Ramps (`ramps\8...<id>.json`, for the `DVA_*` palettes): DM 55BC r1 / C89A r1, 6CD1 r3 / B5D2 r3, A35B r3 / F56B r3, E40F r4 / 4301
r4, E3C5 r0; SD 638F r0 / AA5A r0, D00D r1 / 22DE r1, C808 r1 / B2F3 r1, A785 r3 / r5, 6179 r2; MM 593A / 4450 r0, EE6C r6, B908 r3 /
r4; Fusion 07FE r4 / C2B9 r4, D285 r2; Boosters 3AEC r2, 5DDA r4.
