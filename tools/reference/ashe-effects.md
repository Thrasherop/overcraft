# Ashe's ability effects in the game data

What Ashe's effects are made of in real Overwatch, where the extracted files are, and how each would be rebuilt in Minecraft (an
`AsheFx` beside `CassidyFx` / `JunkratFx`; today the server sends vanilla `SMOKE` / `CRIT` / `CLOUD` / `FLAME` / `EXPLOSION` /
`SMALL_FLAME` from `ashe/Ashe.java` and `entity/DynamiteEntity.java`, issue #150). Read 2026-11-17 from the archived offline build
154088. Overwatch and Battle.net were checked closed (`Get-Process Overwatch, Battle.net`) before the TankLib run and before the mesh
export, and `owinit.py` refuses on its own; everything was read only, offline, through TankLib/DataTool. Text only in the repo: masks,
ramps and meshes stay in the owner's archive and reach the game through the local asset pack. Format:
[junkrat-effects.md](junkrat-effects.md) / [cassidy-effects.md](cassidy-effects.md) (the other hitscan Sharpshooter: same line,
muzzle and hit layout). Data map: [ashe-map.md](ashe-map.md); public numbers: [ashe.md](ashe.md); method:
`techniques/rendering/rendering.md` "Effects and glows".

Confidence: **H** read in the data, **M** inferred, **L** guess. "Size" is the spawn size (a 1 m quad at size 0.5 is 0.5 m; a line's
or ribbon's is its width; a mesh's size scales its own extent). "Ours" marks what no data gives. `XXXX rN` = the colour LUT (last 4 hex
digits of `ramps\8...XXXX.json`) and its row; hex runs are that row at 0 / 25 / 50 / 75 / 100 % (sRGB, so HDR rows print near white:
the `ASHE_*` stops below are hue-clamped). `uniform(a, b)` is printed as read (M: a +- b, as Torbjörn's / Junkrat's lives). "hp" =
hardpoint.

## Files and scripts
- `E:\Overwatch Archive\research\ashe\fx\Ashe\<Ability>\` (`Viper`, `Reload`, `CoachGun`, `Dynamite`, `BOB`, `Perks`): `textures\`
  (PNG, flipbook frames `_1.._n`), `ramps\` (float LUTs as JSON), `models\Models\<id>.00C\` (`.owmdl`), `manifest.json`. A texture
  used by several abilities is saved in each.
- `research\ashe\work\`: `fx_cards_{viper,coach,dynamite,bob,perks}.txt` (one card per renderer: what this note is written from) and
  their `_compact.txt`, `fx_overview.txt` (one line per effect), `fx_summary2.txt|json`, `fx_meshes2.json` (mesh bounds),
  `fx_entities.txt` (her, B.O.B.'s, the shots', the dynamite's and the guns' effect slots and children), `fx_clip_effects.txt` (every clip naming an 08F, with its clipmap state), `fx_effects\`
  (decoded), `fx_extract_log.txt`, `fx_palettes2.txt` (the `ASHE_*` stops below), `fx_sheets\new_masks.png` (the new masks).
- Scripts `research\ashe\scripts\fx\`: Junkrat's `fx_junkrat_*` copied as `fx_ashe_*` (each header says so;
  `fx_ashe_extract.py` holds her effect list with each one's graph node / entity slot / clip; `fx_ashe_pal.py`'s `PAL` is hers).
  Run: `fx_ashe_extract.py [Ability ...]` (kiriko-anim venv, `-I`); then under `py -I run.py`: `fx_ashe_clipfx.py`,
  `fx_ashe_summary2.py`, `fx_ashe_overview.py`, `fx_ashe_card.py <ids>`, Torbjörn's `fx_torbjorn_cardsc.py <cards>` to compact,
  `fx_ashe_pal.py`, and `..\..\pharah\scripts\fx\fx_pharah_rows.py ashe <lut4>` (a LUT's rows in all four channels).
- Sources of the list: `work\graph_effects.json` (her graphs' effect nodes, already made by the kit pass), `work_kit\kit_ashe_hero.md`
  / `kit_ashe_bob_dynamite.md` (node roles), `work\ents_ashe_out.txt` and `fx_entities.txt` (`fx_ashe_ents.py` over 251C 251D 2533
  2B66 25C3 25CB 280E 29A1 2999 2B61 2560 2523 498E: every effect they name is in the list, besides the shared 0082 / 00F7 / 01E4;
  the alt shot 43A7 has no dump).
- Counts: **113 effects asked for, 113 extracted (+ sub-effects 2458, 25E5, 25CE, 4046), 0 missing.** 25 draw something (counting a
  light, a screen layer and two material effects); the rest are sounds only or empty: 274C / 274B / 2655 / 27CB (the shots' sound
  slots), 013A / 0231 (shared shot sounds), 2444 / 5FE8 (a shell in), 2445 / 2704 (the lever, 0.3 s after a shot), 6558, 2860 / 2B1E
  / 2862 / 2B21 / 2B22 / 29E4 / 2B44 / 1900 (child entities), 275C / 2764 / 5F58 / 6556 / 4046 (Coach Gun), 2815 / 435C / 2625 / 2703
  / 26EF / 263E / 27B8 (Dynamite), 262C / 7449 / 744A / 6450 / 020D / **2661 the uppercut** / **2671 B.O.B.'s death** / 2717 / 2718 /
  **2613 his fire** / 2684, 450E / 65B4 / 6C9A (perks), and every throw / cast / reload / scope clip but 2BBA and 2B06.

## Her look
- **Gold for her, red for enemies (H rows, M roles).** Nearly every renderer is a twin on the same mask: one row orange-gold, one row
  redder (her lines 1262 r3 / 0B5A r3, B.O.B.'s 7441 r3 / 745C r3, muzzle cones 3773 / EFAD, the hit F573 r4 / 1BD2 r4, the fuse
  59D7 / 6FF7, the blast BC76 / BB62, the burn 0454 / D2A3, B.O.B.'s charge 47CC / 7B6E). As Junkrat's: the golder = her side, the
  redder = enemies (M; no blue / red pair as Tracer's or Cassidy's cyan). Draw fire rows added; smoke blended (`Fx.soft`).
- **Her hitscan kit = Cassidy's layout with her colours (H):** a line on mask **510** (quad 8C0, width field `59DC556C` **0.055 m**,
  length ECIN = muzzle to hit, M) plus a smoke band 16E9D x B35 x **18BB5**; a muzzle sub-effect at the Viper's **0006** with Cassidy's
  cone mesh **80B8** and Torbjörn's dome **DAF**; one hit effect **25CF** shared by the hip shot, the scoped shot, B.O.B.'s shots and
  the Coach Gun pellets (`m_AED73588` creature and `m_BF7914D4` world are the same effect, H).
- **The dynamite blast is meshes, not a flipbook (H):** a 13 m hemisphere 963D, an 11 m sphere 1385, 10.5 m / 10 m / 6 m fire balls
  **7CDA** (D.Va's missile ball), a 5 m dome DAF, 25 sparks; **no fireball flipbook, no smoke, no scorch decal** in the live 25C9
  (the 47C4 fireball + decal 22BE / 31E8 / 22C0 kit is only in 3924, a Stadium burn zone, L). The burn reuses **Torbjörn's** ground
  rings 86CF / 5708 with D9B / 190F7.
- **HDR peaks (H):** the lines 5-10x, muzzle sparks 30x (178E / 4723 r0: per-channel clamping would draw them yellow; hue-clamped
  they are red-orange, M), Coach Gun flames 15x, blast balls / burn flames 10x.
- **Hardpoints (H ids, all in the pack's glbs; M roles):** the Viper's **0006** (muzzle: 2458 / 25E5's renderers carry hp 6 with
  +0.2-0.4 m offsets along it), **0079** (in 2458's table, no renderer reads it: L the ejection port), **007C** (2704, the lever),
  **0076** (263A's 0.1 light); her **00C9** (the Viper's mount: shells 2444, the 3P reload glint), **0001** (a burning / held target's
  chest), **0014** (the knocked-up target); the Coach Gun's **0006**; the dynamite's **0076** (the fuse, 25C7) and **01AB** (263E,
  sound); B.O.B.'s **0004** (root: the charge glow at +1.2 up, +1.75 ahead), **0011** (a light), **0297 / 0298 / 0007** (his muzzles,
  cycling: 153B #315-#321) with alternates 02A5 / 02A6 / 0005. `ashe_viper.glb` has 0006 / 0076 / 0079 / 007C; `ashe_bob.glb` has
  0297 / 0298 / 0007 / 0004 / 0011; `projectile_ashe_dynamite.glb` has 0076 / 01AB.
- **First person vs third person (H):** one muzzle effect for both: the Viper entity 2523 is the child of both her 3P 251C and her 1P
  251D (hp 00C9), so 2458 / 25E5 play at the same 0006 in either view; the only 1P change in the data is the Viper's scale (14FC #715:
  1.176 / 1.05). The hip line has two gold renderers on different masks (C15: 510; C17: 8556 x 510, life 0.15 / 0.125 s): L one per
  view. Ours, as Tracer's / Cassidy's: the muzzle a third the size in her own view, the line from the posed muzzle in both.

## The Viper (14FC; hip shot 280E, scoped shot 29A1, hitscan; Take Aim = the scope)
| Part | Effect | Rebuilt (replaces) |
|---|---|---|
| **Hip muzzle** | **2458** (2456's sub-effect, at the Viper's **0006**; 1 s, every renderer under 0.6 s) H: core dome **DAF x 4DB 0.35 +- 0.2 m, 0.075 s** at +0.2 m (D0D5 r4 gold, HDR 5); dome **DAF x 16E9D 0.9 +- 0.3 m, 0.075 s** at +0.25 m (twins C412 r4 gold / A680 r4 red, HDR 2.5); **cones mesh 80B8 x 18BAD 1.0 +- 0.25 m, 0.05-0.06 s** (3773 r1 / EFAD r1, HDR 5); **flashes 18BAC (4) 1-1.25 m, 0.06-0.09 s** (57C5 r0 gold, 420C r0 cream); glow 18BAD 0.3 +- 0.2 m 0.05 s; **sparks 1F35: 10 at 2-10 m/s 0.01-0.02 m 0.1 +- 0.1 s cone 35 deg, + 10 + 10 at 2-10 m/s 0.02-0.03 m 0.15-0.3 s cone 40 deg** (178E r0 / 4723 r0, HDR 30, falling); **smoke 1DFCB x 1FC65 x 1DFCA x B08: 1 + (3 +- 2) + (3 +- 2) at 1-3 m/s, 0.4-0.6 m, 0.3-0.6 s, stretch 4.6** (8954 r1 / 764C r1 rose-grey); lights 3.5 m / 2.5 m; sub-effect 4010 (shared sound) | at the posed Viper 0006 each hip shot (`AsheFx.muzzle`, variant hip): the core dome (`torb_dome`, `ASHE_MUZZLE_CORE`), the dome (`torb_dome` x `cat_cone_line`, `ASHE_MUZZLE_DOME` / `_ENEMY`), one cone (`cassidy_muzzle_cone` x `jr_frag_glow#4`, `ASHE_MUZZLE_CONE`), two flashes (`ashe_flash#4`, `ASHE_MUZZLE_FLASH`, `_B`), 6 sparks (`spark#4`, `ASHE_MUZZLE_SPARK`, ours: a fifth), 2 smoke (`brigitte_smoke#32`, soft, `ASHE_MUZZLE_SMOKE`); a third the size in her own view (ours). None today |
| **Hip line** | **2456** (280E's STUEffectComponent, 1 s) H: lines on quad 8C0 mask **510** (+ 8556 on C17), **width 0.055 m, life 0.15 s (C15 / C19) / 0.125 s (C17)**, length ECIN (M: muzzle to hit); her **1262 r3** (HDR 10: orange-gold > deep orange, the 736B r3 the same) / enemy **0B5A r3** (HDR 5: orange > red); **smoke bands 16E9D x B35 x 18BB5, width 0.4 +- 0.2 m, life 0.25 +- 0.25 s** (933C r5 / 3C74 r6 by age: hot orange head then dark brown; enemy E133 r6 red head) | `SombraFx.tracer`'s band on `sombra_tracer_line`: 0.055 m, 0.15 s, `ASHE_LINE` / `ASHE_LINE_ENEMY` by the viewer's side; one soft band 0.3 m (ours: inside 0.2-0.6) for 0.3 s along it (`ashe_line_smoke` x `ana_beam_line`, `ASHE_SMOKE_LINE`, alpha 0.35, ours). Replaces `trail()`'s `SMOKE` dots (Ashe.java:411; callers :314 / :324 / :350) |
| **Scoped muzzle** | **25E5** (25E4's sub-effect, at 0006) H: as 2458 but bigger and longer: domes DAF x 16E9D **1.5 +- 0.2 m, 0.1 s** (5309 r4 / 0A9B r4) and DAF x 4DB 0.4 m (9FB2 r4); **cones 80B8 1.5 +- 0.25 m, 0.06-0.07 s** (2400 r1 / 6542 r1); flashes 18BAC **1.5 m**; 30 sparks (4723 r0); **smoke 1 + 3 + 3, 0.5-0.8 m, 1-1.5 s** (069B r1; 31F5 / 6548 r1); a distortion dome 5F08 0.4-0.5 m (not drawn); lights 3.5 / 2.5 m | variant scoped of `AsheFx.muzzle`: the same parts at the scoped sizes, `ASHE_SCOPED_CONE` / `_ENEMY`, `ASHE_SCOPED_SMOKE`, smoke living 1.2 s (ours: the mean); no distortion |
| **Scoped line** | **25E4** (29A1's STUEffectComponent) H: lines 510 **0.055 m, 0.15 s**: her **2F36 r3** (HDR 5: near white > orange: yellower than the hip line; 4E3F r3 the same with 8556) / enemy **0B5A r3**; **smoke bands 0.4 +- 0.2 m, life 0.75 +- 0.25 s** (81EC r0 / 7660 r0: orange head then grey; enemy E133 r6) | the same band, `ASHE_LINE_SCOPED`, and the smoke band for 0.75 s (`ASHE_SMOKE_LINE_SCOPED`); `shot()` knows `scoped` (Ashe.java:302) |
| **Hit (creature and world)** | **25CF** (280E / 29A1 / 2999 / 2B61 `m_AED73588` and `m_BF7914D4`: one effect for both, H; 0.5 s): **dome DAF x 4DB 0.25 +- 0.25 m, 0.1-0.15 s** (twins F573 r4 gold / 1BD2 r4 red, HDR 10); **cones 80B8 x 18BAD 0.75 +- 0.25 m, 0.075-0.125 s**, +0.025 m off the surface, along the normal (D6C3 r0, L: a channel-0 ramp); **decal 18BC2 (4) 0.1 m (f0; 0.05 / 0.05 / 0.1), 0.5 s, random turn** (D025 r2 orange / 1A50 r2 red, HDR 5) | `"ashe_hit"` where the shot stops, with the hit face's normal: the dome (`torb_dome`, `ASHE_HIT` / `_ENEMY`), one cone (`cassidy_muzzle_cone`, ours colour `ASHE_HIT`), the decal on a block face only (`cat_blob#4`, `ASHE_HIT_DECAL`; ours: none on creatures). Replaces `CRIT` x2 (Ashe.java:413). Also B.O.B.'s and the pellets' hits |
| Take Aim (scope) | scope in / out clips 2A4A / 2B17 (1P), 2B7B / 2B7C (3P): sounds only (H). **25E1** (14FC #637): mesh 5C75 x 18B9B (a dark vignette ring), size 2, living "forever" (ECCN 100000), `4E65CCF9 = ECIM(40)`: her own screen (H) | not drawn (the scope overlay is the HUD's; 18B9B could serve a vignette, ours) |
| Viper extras | 1P scoped-fire clip 2AB8 and 3P fire clips 2B8C / 2B90: sounds / a camera shake only (H; no shell eject: the lever-action spends no visible case). 263A (child 2A59 at hp 0076): a 0.1 light (H, L its use). 2445 / 2704 (lever, 0.3 s after a shot, hp 007C): sounds / empty | nothing |

## Reload (14FC #680-#694)
| Part | Effect | Rebuilt (replaces) |
|---|---|---|
| Shells | **2444** (#689 / #723 / #724 at her **00C9**, one per shell) / 5FE8 (a skin, 0C22.134): **sounds only** (H). 1P clips 2A47 / 2A48 / 2AE4 / 2AE5 / 2A49 and the Viper's 2B49 / 2B48 / 6CDE / 6CDD / 2A46 / 2B44 / 2B47 / 6CDF / 2B45: sounds or empty (H) | nothing (the shell sounds stay at `shell` :270) |
| 3P start glint | **2BBA** (3P clip 5F7B reload_start, at **00C9** +0.1 m): one **1BD72** sprite (a gold lens glint, BC7 colour) **1.55 m at 0.133 s for 0.3 s** (87AF r0: a parameter row; the colour is the texture's, M) | at `startReload` (:191), others' view only: one 1.5 m glint (`ashe_reload_glint`, colour sprite, alpha 0.5 ours) at her posed 00C9; no vanilla call today |

## Coach Gun (1513, ABILITY 1; pellets 2B61)
| Part | Effect | Rebuilt (replaces) |
|---|---|---|
| **The blast** | **2455** (1513 #139 at the Coach Gun's **0006**, at 0.165 s; 1 s) H: **flashes 8CF 1.7-2.2 m, 0.12-0.15 s** (D574 r1 / 291E r1 twins, HDR 5); glow 16E9D x A7B6 **1 m 0.125 s** (62A7 r0); **flare 18BAD 1 m 0.14 s** (6B6C r1); **the 3 m flash 18BAC, 0.13-0.145 s** at +0.17 m (A893 r1 / D285 r1); **spikes 18D9B (4): 15 at 5 m/s 0.1-0.2 m stretch 7.5 cone 45 deg** at -0.35 m (8335 r2 / 5AA8 r2, HDR 10) and 10 at 5-10 m/s 0.15-0.25 m; **pellet streaks 1F35 on quad E21: 30 + 30 at 2-15 m/s, 30 + 30 at 4-10.7 m/s and 30 at 8-17 m/s**, 0.07-0.4 s, stretch 2-5 (curve to 40), cone 25-45 deg (99A0 r0 / 5496 r0 / 2EF6 r0: gold > pink, HDR 8-15); **flame tongues 1CAFA: 12 + 12 at 1-10 m/s, 15 at 10-20 m/s, 0.1-0.2 m, 0.075-0.24 s** (7C18 r0 / 7860 r0, HDR 15); **smoke 1DFCB: 10 + 8 small 0.16-0.32 m 0.37-0.67 s and 2 + 7 + 7 big 1.25-2.5 m, 0.5-1 s, 1.1-2.4 m/s, stretch 3-4** (EAD9 r1 / EE2D r1 / 4BE4 r1 rose-grey); a distortion dome 5F08 2.5 m 0.33 s (not drawn); lights 5 m 0.1 s x3 | `"ashe_coach"` at the posed Coach Gun 0006 (entity = her, dir = her aim): the 2 m flash (`ashe_coach_glow`, `ASHE_COACH_FLASH` / `_ENEMY`), the 3 m flash at 0.3 alpha (`ashe_flash#4`, `ASHE_COACH_FLASH_BIG`), 8 spikes (`ashe_coach_spike#4`, `ASHE_COACH_SPARK`), 15 pellet streaks along the 20 deg cone (`spark#4` stretched, `ASHE_COACH_PELLET`; ours: one per pellet), 8 flame tongues (`torb_flame`, `ASHE_COACH_FLAME`), 3 smoke (`brigitte_smoke#32`, soft, `ASHE_COACH_SMOKE`). Replaces `CLOUD` x8 + `FLAME` x6 (Ashe.java:559-560 in `coachBlast` :507) |
| Pellets | 2B61 has no line (no STUEffectComponent): only a sound slot 27CB and the hit **25CF** (`m_BF7914D4`) (H) | `"ashe_hit"` (small: ours half size) where each pellet stops, world only; no tracer |
| Her launch | 2764 / 5F58 / 6556 / 275C: sounds; 3P clips 2AB1-2AB3: camera shakes (hp 0015) + sounds; 1P 2A44 sounds, 2A74 / 7DA4 / 98FC empty (H) | nothing (no trail in the data) |
| Double-Barreled (28B4) | no effect of its own (H: its nodes are 1513's / 51E3's with no effect) | the same `"ashe_coach"` on the second shot |

## Dynamite (14FD cast, projectile 2533 with 1502, burn 150F)
| Part | Effect | Rebuilt (replaces) |
|---|---|---|
| In hand / throw | 2B66 (in hand) child 3028 = **2B22: empty**; 2815 / 435C (14FD) sounds; clips 2AED (1P) / 2CD2 (3P): sounds (H) | nothing |
| **In flight (fuse)** | **25C7** (1502 #229, the false branch of #115 = **not landed**, at the stick's **0076**; looping 1 s) H: **fuse glows 242 50 /s, 0.05 s, 0.15-0.85 m** (59D7 r1 gold / 6FF7 r1 red, HDR 5); **two ribbons 797 x B35 x 16E9D x C495, 60 points/s, 0.75 s, size 1** (L: width scaled by the stick; 7D63 r0 / 448D r0 by age: hot head > black by half-life, HDR 3-8); **sparks 1F35 100 /s, 2.9-7 m/s, 0.025 m, 0.2-0.4 s**, cone 15 deg, gravity -9.8 (DFF9 r1 / E9D9 r1); **smoke 1DFCB 50 /s, 0.5-0.75 s, 0.5 +- 0.25 m, 0.1-0.5 m/s** (6A26 r3 / 829F r3 by age: bright then dark grey); two lights | per stick each frame while not landed (ProjectileModelRenderer case `DynamiteEntity`, at its posed 0076): a 0.4 m fuse glow 3 a tick (`mercy_glow`, `ASHE_DYN_FUSE` / `_ENEMY`), `Fx.trail` 0.1 m / 0.4 s (`ana_beam_line`, `ASHE_DYN_TRAIL`; ours narrower and shorter), 2 sparks a tick (ours: of 5), a smoke every 2 ticks (soft, `ASHE_DYN_SMOKE`). No vanilla call today |
| **Landed, lit** | **25C8** (1502 #227 inside #180, the true branch = **landed**, the fuse count) H: **glow 242 0.5 m, 0.4 /s + 1, living 2.5 s** (8B98 r1 gold / BDB8 r1 red); **swirl 19167 x 4C09 x 16E9D 25 /s, 0.25 +- 0.25 m, 0.75-1 s, spin 3** (77EC r8 by age: orange head > black; 6454 r0); **sparks 100 /s** as in flight (DFF9 / E9D9); lights (2625 / 2703 alongside: empty) | per landed stick each frame (same case, at 0076): the 0.5 m glow held (`mercy_glow`, `ASHE_DYN_LIT`), a swirl a tick (`cat_glow_noise`, soft, `ASHE_DYN_LIT_SWIRL`), 2 sparks a tick. Replaces `SMALL_FLAME` every 5 ticks (DynamiteEntity.java:104) |
| **Explosion** | **25C9** (1502 #114 when #117 explodes; scale 1, or 1.4 airborne with Airburst; 2 s; also B.O.B.'s Stadium blast 1537 #1110) H, sizes as read (mesh units ~1 m): **hemisphere 963D x 18E48 size 13, 0.3 s** (BC76 r2 brown > gold, HDR 5 / BB62 r2 red); **sphere 1385 x 16E9D x 19167 size 11, 0.3 s** (C390 r3 red > orange-gold, HDR 5); **fire balls 7CDA x 4DB x 1909A size 10.5, 0.5 s** (3CBA r4 / 26EE r4: dark red > orange > cream, HDR 10) and **7CDA size 10, 1 s and size 6, 0.75 s** (72C0 r4 / 5D67 r4 red > gold); **dome DAF x 16E9D 5 m, 0.5 s** (F21D r4 / 8BFC r4); **sparks 1F35 25 at 7-11 m/s, 1 s, 0.05-0.1 m, stretch 2.5, all ways** (FC12 r1 tint (10, 4.12, 0.34)); distortion DBF 13.5 m 0.5 s (not drawn); two lights 5 m 0.375 s. **No smoke, no flipbook fireball, no decal** (H). 26EF (#201): sounds | replaces `EXPLOSION` x2 + `FLAME` x40 (Ashe.java:682-683 in `explode` :649) with `Effects.play(level, "ashe_dynamite", at, normal, ..., scale)`: the 10 m hemisphere at 0.3 alpha (`torb_hemisphere` x `cat_cloud`, `ASHE_BLAST_SHELL` / `_ENEMY`, ours: 0.8 of the data's 13 to stay inside the 5 m radius), the sphere (`cat_sphere`, `ASHE_BLAST_SPHERE`), two fire balls 10 m / 6 m expanding over 0.5 / 0.75 s (`dva_missile_ball` x `mercy_noise` x `ashe_ball_fade`, `ASHE_BLAST_BALL`, `ASHE_BLAST_FIRE`), the 5 m dome (`torb_dome`, `ASHE_BLAST_DOME`), 15 sparks (`spark#4`, `ASHE_BLAST_SPARK`, ours: of 25); `scale` = 1.4 when Airburst made it airborne |
| **The burn (and on fire)** | **254F** (150F #149 on each target hit, at its **0001**, held 10 s, i.e. the 5 s burn, M) H: **flames 4DB x 4A7E x 4C09: 7 /s and 10 /s + 3, 0.5-0.75 m, 0.5-1.5 s** (twins 0454 r12 gold / D2A3 r12 red, HDR 10); **sparks 1F35 20 /s + 10, 0.5-1 m/s, 0.025-0.05 m, 0.5 s, stretch 2.5, spin** (05C8 / DBDF / FE83 r1 tints (10, 1.4-4.1, 0.34)); **ground rings: Torbjörn's 86CF x D9B and 5708 x 190F7, size 2** (B8BD r6 / 5ECA r6 / 1C5B r6 orange > cream); **embers 1F35 on quad 801 20 /s + 25, 0.017-0.032 m, 0.5-1.5 s** (A8A6 r0 cream); lights 3 m. **27EF** (#152, unless the target has tag 003A, M): a material effect, 10 s (26D1 r0: the body glowing). 27B8 (#153 at hp 01AC): sounds. **73B2** (#232, under 2855.134: a Stadium power, L): cylinders 129B / sphere 85B | each burning creature each tick (client, off the burn's sync, L; or server `"ashe_burn"` for the burn's length from `dot` :688): a flame every 2-3 ticks (`mercy_noise_b` x `cat_soft`, `ASHE_BURN_FLAME`), a spark a tick (`spark#4`, `ASHE_BURN_SPARK`), the 2 m ring at its feet (`torb_burn_ring_mesh` x `torb_burn_ring`, `ASHE_BURN_RING`), an ember every 2 ticks (`ASHE_BURN_EMBER`). Replaces `FLAME` every 4 ticks (Ashe.java:707 in `tickAll`). No material glow (not drawable) |
| Stadium burn zone | **3924** (14FD #187 before the #173 burn zone, L Stadium): flash 29F7 5 m, fireball **47C4 x2**, 80 + 80 sparks, flame rings 1E7F x 3AE1, decal 22BE / 31E8 / 22C0, smoke 218F x B0B x 4DB | not drawn (not live) |

## B.O.B. (1536 cast; 25C3 with 1537 / 153B / 153A; shot 2999; immobilize 17F6)
| Part | Effect | Rebuilt (replaces) |
|---|---|---|
| Cast / drop | 262C / 7449 / 744A / 6450 (1536): sounds; clips 2B6B (1P) / 2C16 (3P): sounds; B.O.B.'s spawn_drop 2AD5: **empty** (H) | nothing (no landing burst in the data) |
| **Charge** | **25CC** (1537 #204 inside the charge state #228 while v253 > 0, held, 3 s, at his **0004**) H: **front glow: half dome mesh 85D0 (0.99 x 0.50 x 0.99) x 4DB x 1B1C9 at (0, 1.2, 1.75) from his root, size 3, 2 /s + 1, living 1 s** (47CC r5 gold / 7B6E r5 red, HDR 5); **ribbon 7F5 x 5DF x 1344 at (0, 1.2, 0), 3 m wide, 60 points/s, 0.25 s** (9C50 r6 / 2C70 r6); lights 2.8 at hp 0011. Charge clips 0FA5-0FAC: camera shakes only (M: footfalls) | per B.O.B. while `phase() == CHARGE` (client, BobEntity's synced phase; LifeweaverPropRenderer-style beside his model): the 3 m half dome 1.75 m ahead at chest height, pulsing 2 /s (`ashe_charge_dome` mesh x `mercy_noise` x `ashe_charge_strip`, `ASHE_BOB_CHARGE` / `_ENEMY`, alpha 0.5 ours), `Fx.trail` 1.5 m wide / 0.25 s at 1.2 m (`juno_noise_c` x `cat_strip`, `ASHE_BOB_CHARGE_TRAIL`; ours: half the 3 m). No vanilla call today (BobEntity.java:188 plays `bob.charge`) |
| Stop / timeout | 2ADF / 2B37 clips: sounds (H) | nothing (:207) |
| Uppercut | **2661** (1537 #426 on the hit): **sounds only** (H); clips 2AB0 empty, 2B73 a camera shake + sounds | nothing of his own; the targets get 270F |
| **Knocked up** | **270F** (17F6 #307 on each target at its **0014**, at the immobilize's start; 0.83 s) H: **flashes 18BAD 2 m 0.2 s** (4C69 r1 gold / 43EC r1 red); **spikes 18D9B 5 + 5 at 1 m/s 0.15 m 0.2 s** cone 60 deg (2881 r1 / 5C69 r1); **streaks 183A7 and 1BCFB 16 + 16 at 5-10 m/s, 0.2-0.5 m, 0.1-0.3 s, all ways up** (A321 r1 / C1B4 r1, HDR 15); **dome DAF x 18E48 4 m 0.2 s** (2073 r3 / 8E08 r3) | `"ashe_bob_knock"` on each target from `bobUppercut` (Ashe.java:844), at its posed chest: the 2 m flash (`jr_frag_glow#4`, `ASHE_BOB_KNOCK`), 6 spikes (`ashe_coach_spike#4`), 10 streaks (`cassidy_spark` / `cat_streak`, `ASHE_BOB_KNOCK_STREAK`), the 4 m dome at 0.3 alpha (`torb_dome` x `cat_cloud`, `ASHE_BOB_KNOCK_DOME`) |
| Held (1.5 s / 0.5 s) | **3BF7** (17F6 #324 inside the immobilize wait, at **0001**) H: bits 177F4 x 1F56 x 30F5 (16) x 18AC4 **4 /s + 1, 0.12-0.17 m, 0.5-1 s, gravity** (84F2 r3 brown > gold); stars **176B** (a magenta-rimmed ring, colour) 10-20 /s, 1-1.5 s, speed 3 > 0, 0.1-0.13 m, stretch 5 (13EB r0 / 40C1 r0: parameters, M); a material effect (905A: blue-violet) | per held creature each tick (client; L the sync: as Junkrat's rooted look): a bit every 5 ticks (`ashe_dazed_bits#16`, `ASHE_BOB_DAZED`), a star a tick (`ashe_dazed_ring`, colour); no material |
| Barrier / special hit | **05D9** (1537 #596 ActionPlayScript on a barrier the charge meets, L): the shared stun look (rings DE0 x 4D4 x 247C, stars 248B, D8F own-screen rim): Cassidy's note draws it with `BrigitteFx.stunned` (H same id) | not needed (no barriers to hit in Minecraft) |
| **His shots: muzzle** | **25CE** (25CD's sub-effect: played at the shot's start = the muzzle **0297 / 0298 / 0007** in turn, M; 0.33 s) H: domes DAF x 4DB **1 +- 0.25 m, 0.1 s** (E99E r0 / A97C r0: parameter rows, L); **cones 80B8 x 18BAD 1.5 +- 0.25 m, 0.06-0.07 s** (204F r1 gold = `ASHE_SCOPED_CONE` / 6542 r1); flashes 18BAC **1.5 m 0.06-0.08 s** (420C r0 cream); **sparks 1F35 15 + 15 at 2-10 m/s, 0.04-0.05 m, 0.2-0.35 s** (FC0C r0); **smoke 13F75 (2, colour) x B08 3 + 3, 0.6-0.8 m, 0.25-0.75 s, 2.9-3.1 m/s, stretch 3-4** (807E r2 / BDA5 r0); distortion DBF 1 m (not drawn); lights 2.5 m. 2613 (153B #338, the fire): sounds only (H) | `"ashe_bob_shot"` (the line, below) carries the muzzle at its start: one cone (`cassidy_muzzle_cone`, `ASHE_SCOPED_CONE`), a flash (`ashe_flash#4`, `ASHE_MUZZLE_FLASH_B`), 4 sparks (`ASHE_BOB_MUZZLE_SPARK`), one smoke (`ashe_smoke_puff#2`, colour, soft); the start = his posed 0297 / 0298 / 0007 cycling per shot (`Ashe.bobShot` :817 today aims from `gun`) |
| **His shots: line** | **25CD** (2999's STUEffectComponent, 0.133 s) H: lines 510 on 8C0, **width 0.075 +- 0.05 m, life 0.15 s** (twins **7441 r3** orange, HDR 10 / **745C r3** red-orange); smoke bands 16E9D x B35 x 18BB5 **0.4 +- 0.2 m, 0.5 +- 0.25 s** (81EC r0 orange head > grey / E133 r6 red head) | `SombraFx.tracer`'s band 0.075 m, 0.15 s (`ASHE_BOB_LINE` / `_ENEMY`) and a 0.25 m soft band for 0.5 s (`ASHE_SMOKE_LINE_SCOPED`). Replaces `trail()` at Ashe.java:837 (`bobShot`) |
| His shots: hit | **25CF** (2999 `m_AED73588` / `m_BF7914D4`), as hers (H) | `"ashe_hit"` |
| Firing stance | clip 2BBD idle_fire: 4 camera shakes (M: each cannon's kick); 2AF6 aim: sounds (H) | nothing |
| Despawn | clip **2B06** (0.4 s): smoke 13F75 (2, colour) x B08 (F193 r0, L), glow 19167 x 16E9D x 4C09 (200B r0 HDR 20 / F91A r1 tint (2, 0.2, 0)), a camera shake at 0.2 s, **two material effects** (M: his dissolve) (H) | at `phase(DESPAWN)` (BobEntity.java:247-253): 3 smoke (`ashe_smoke_puff#2`, soft) and a 1.5 m glow (`cat_glow_noise`, ours colour `ASHE_BOB_CHARGE`) over his body; no dissolve |
| **Death** | **2671** (1537 #430 when he dies, then destroyed at once): **sounds only** (H) | nothing new in the data; ours if wanted: the despawn's smoke (BobEntity.java:160) |

## Perks (51E3)
| Part | Effect | Rebuilt (replaces) |
|---|---|---|
| **Viper's Sting (2566)** | **65A8** (51E3 #343 at the second scoped hit) H: **dome DAF x 16E9D 2.74 m, 0.3 s** (B0CB r4 dark red > gold, HDR 10); **sparks 1F35 20 at 6.2-10.2 m/s, 0.24-0.6 s, 0.02-0.08 m, stretch 3.8-6.4, all ways** (55E1 r1 red > yellow); **ball 7CDA 2.73 m, 0.7 s** (7506 r4 red > gold). 450E (#342 on the target): sounds | `"ashe_sting"` at the hit from `sting` (Ashe.java:397, when the 25 lands): the dome (`torb_dome`, `ASHE_STING_DOME`), 10 sparks (`ASHE_STING_SPARK`), the ball expanding (`dva_missile_ball`, `ASHE_STING_BALL`) |
| **Remote Detonator (2686)** | **6E62** (1502 #399 inside the 0.5 s wait #398 after the second press) H: flames 4C09 x 4DB x 4A7E **10 /s + 5, 0.37-0.62 m, 0.35-0.54 s, 0.6-1.4 m/s** (2BBE r11 dark red > cream, HDR 10); sparks 1F35 10 /s (FE83 r1 tint); glow 3253 (2) **1.19 m 3 /s, 0.5-1 s** (D6BE r0: grey-green, L a parameter row); light 3 m. 6C9A (#404): sounds | for the 0.5 s between the press and the blast, at the stick: a flame a tick (`mercy_noise_b`, `ASHE_DETONATOR_FLAME`), a spark a tick, a 1.2 m glow (`ashe_detonator_glow#2`, ours colour `ASHE_DYN_LIT`) |
| Airburst (2565) | **65B4** (1502 #389 / #394 when not landed): sounds only (H); the blast is 25C9 at scale 1.4 (35A6 #93 / #103) | `"ashe_dynamite"` with scale 1.4 |
| Double-Barreled (28B4) | none of its own (H) | the second `"ashe_coach"` |

## Where it hooks in (each vanilla call it replaces)
| Today (`src/main/java/dev/overcraft/...`) | Becomes |
|---|---|
| `ashe/Ashe.java:411` `SMOKE` dots in `trail()` (hip / scoped shots :314 / :324 / :350; B.O.B. :837) | `Effects.play(level, "ashe_shot", from, to - from, player, variant)` once per shot: variant 0 hip (2456 + 2458), 1 scoped (25E4 + 25E5), 2 B.O.B. (25CD + 25CE, entity = B.O.B., muzzle 0297 / 0298 / 0007 by shot count); the client draws the line from the posed muzzle (her Viper 0006, a third in her own view) |
| `ashe/Ashe.java:413` `CRIT` x2 at the stop | `"ashe_hit"` (25CF) at the stop with the hit face's normal (block: with the decal) |
| `ashe/Ashe.java:559-560` `CLOUD` x8 + `FLAME` x6 (`coachBlast` :507) | `"ashe_coach"` (2455) at the posed Coach Gun 0006, dir = her aim; pellet stops: `"ashe_hit"` small, world only |
| `ashe/Ashe.java:682-683` `EXPLOSION` x2 + `FLAME` x40 (`explode` :649) | `"ashe_dynamite"` (25C9), scale 1.4 with Airburst airborne |
| `ashe/Ashe.java:707` `FLAME` every 4 ticks (`tickAll`, the burn and the sting) | `"ashe_burn"` once from `dot` :688 for the burn's length (254F), entity = the target; the sting's dot gets nothing (65A8 at the hit instead) |
| `entity/DynamiteEntity.java:104` `SMALL_FLAME` every 5 ticks (landed) | `AsheFx.dynamiteTick` from `ProjectileModelRenderer` (case `DynamiteEntity`): 25C8 when `landed()`, 25C7 in flight, at its posed 0076 |
| none: reload (`startReload` :191) | `"ashe_reload"`: 2BBA's glint at her 00C9, others only |
| none: B.O.B. charge / despawn (`BobEntity` tick :188 / :247) | client per B.O.B. by synced phase: 25CC while charging; the 2B06 smoke at DESPAWN |
| none: uppercut (`bobUppercut` :844) | `"ashe_bob_knock"` per target (270F), then the held look (3BF7) for the 1.5 s / 0.5 s (L: the sync, as Junkrat's root) |
| none: Viper's Sting (`sting` :397), Remote Detonator (the 0.5 s wait) | `"ashe_sting"` (65A8); `"ashe_detonator"` (6E62) on the stick |
- Hardpoints to pose (HeroModelRenderer's `POINTS`, as `SojournFx.POINTS`): `ashe_viper.glb`'s `hardpoint_0006`; her `hardpoint_00C9`;
  `ashe_coach_gun.glb`'s `hardpoint_0006`; `projectile_ashe_dynamite.glb`'s `hardpoint_0076`; `ashe_bob.glb`'s `hardpoint_0297`,
  `hardpoint_0298`, `hardpoint_0007`, `hardpoint_0004`; targets' `0001` / `0014` (or their chest height, ours, for mobs).
- Palettes: the `ASHE_*` arrays below in an `AshePalettes.java` (as `JunkratPalettes`), `ART` / `ART_ENEMY` lists as `DvaFx`'s.

## Not found / not done
- **Silent in the data (H):** the uppercut (2661), B.O.B.'s death (2671), his fire (2613), the B.O.B. cast (262C) and his drop
  (2AD5), the reload shells (2444), the Coach Gun's launch, the dynamite in hand (2B22), Airburst (65B4). No shell-eject or lever
  particles anywhere in the Viper's clips.
- Twin roles (gold = hers, red = enemies) are M throughout; which of the hip line's two gold renderers (510 / 8556 x 510) is 1P and
  which 3P is L. 0079 in the muzzle's hardpoint table with no renderer on it: L.
- Sizes by data flow not decoded: the lines' ECIN ends (M: muzzle to hit), the burn's 73B2 input 5FD (Stadium anyway), the fuse
  ribbons' size 1 (L: scaled by the stick). The `59DC556C` widths read as width (M, as Cassidy's).
- Parameter / channel-shifted rows not given palettes: 87AF (reload glint), E99E / A97C (B.O.B.'s muzzle domes), 13EB / 40C1 (stars),
  2E97 / 474B (hit cones: `ASHE_HIT_CONE` from D6C3 is L), D6BE (`ASHE_DETONATOR_GLOW`, L: drawn with `ASHE_DYN_LIT` instead).
- Not drawable: distortion layers (5F08 on the scoped muzzle and Coach Gun, DBF on the blast and B.O.B.'s muzzle), material effects
  (27EF the burning body, 3BF7's, 2B06's), her own screen (25E1, 05D9's D8F rim).
- Stadium / non-live: 3924 (burn zone), 73B2 (2855.134), 5FE8 (0C22.134 skin), the alt shot 43A7 (not extracted, L a skin).

## For the asset pack (sources: add `E:\Overwatch Archive\research\ashe\fx`)
Proposed `EFFECT_MASKS` lines (`name: ("Ashe", <Ability>, <12-hex id>, frames)`):
```
"ashe_flash": ("Ashe", "Viper", "000000018BAC", 4),             # flame-shaped flash (muzzles, the Coach Gun's 3 m flash)
"ashe_line_smoke": ("Ashe", "Viper", "000000018BB5", 1),        # the shot smoke band's cross-section (32x256)
"ashe_line_mask": ("Ashe", "Viper", "000000008556", 1),         # the second line mask (16x256, near solid)
"ashe_reload_glint": ("Ashe", "Reload", "00000001BD72", 1),     # colour lens glint (BC7)
"ashe_coach_glow": ("Ashe", "CoachGun", "0000000008CF", 1),     # the blast's soft round flash
"ashe_coach_spike": ("Ashe", "CoachGun", "000000018D9B", 4),    # flame spikes (Coach Gun, knock-up)
"ashe_ball_fade": ("Ashe", "Dynamite", "00000001909A", 1),      # the fire balls' fade strip (16x256)
"ashe_smoke_puff": ("Ashe", "BOB", "000000013F75", 2),          # colour smoke puff (B.O.B.'s muzzle, despawn)
"ashe_charge_strip": ("Ashe", "BOB", "00000001B1C9", 1),        # the charge dome's gradient (16x256)
"ashe_dazed_ring": ("Ashe", "BOB", "00000000176B", 1),          # colour ring (the held target's stars)
"ashe_dazed_bits": ("Ashe", "BOB", "0000000030F5", 16),         # splat bits
"ashe_dazed_noise": ("Ashe", "BOB", "0000000177F4", 1),         # with 18AC4 (cloud noise) not needed
"ashe_detonator_glow": ("Ashe", "Perks", "000000003253", 2),    # Remote Detonator's soft glow
```
Reused (same ids already in `EFFECT_MASKS`): `sombra_tracer_line` 510, `ana_beam_line` B35, `cat_cone_line` 16E9D, `mercy_noise` 4DB,
`jr_frag_glow#4` 18BAD, `spark#4` 1F35, `brigitte_smoke#32` 1DFCB, `cassidy_dust#32` 1FC65, `cat_blob#4` 18BC2 (the hit decal),
`glow` A7B6, `torb_flame` 1CAFA, `mercy_glow` 242, `cat_glow_noise` 19167, `cat_soft` 4C09, `juno_ray_ground` C495, `cat_cloud`
18E48, `mercy_noise_b` 4A7E, `juno_cast_fade` D9B, `torb_burn_ring` 190F7, `juno_noise_c` 5DF, `cat_strip` 1344, `cat_streak` 183A7,
`cassidy_spark` 1BCFB, `ana_blob#2` 1F56, `brigitte_stun_star` 248B, `brigitte_stun_halo` 247C, `brigitte_noise_fine` 4D4, `mercy_rim`
D8F, `cat_band` 1C7A. Not needed: normals / distortion B08, 1DFCA, 18F81; 18B9B (25E1, her screen), 18AC4, the Stadium 3924's ramps
(22A8, 31C0, 3FCE, 5DBF, 3847, 218F, B0B, 3AE1).

`EFFECT_MODELS`: one new mesh, **already exported** (2026-11-17, Blender headless through `tools/owm_static.py`, the command
`build_asset_pack.py` runs) to `E:\claude\mods\overcraft\local-assets\pack\assets\overcraft\hero_models\ashe_charge_dome.glb` (820
vertices, 0.99 x 0.50 x 0.99 m, a half dome standing on y = 0; gitignored, not in git). The line to add so a rebuild keeps it, and the
name `HeroModels.EXTRAS` must list for `Fx.mesh` to load it:
```
"ashe_charge_dome": ("Ashe", "BOB", "0000000085D0"),   # B.O.B.'s charge front glow (25CC), 0.99 x 0.50 x 0.99
```
Reused models: `cassidy_muzzle_cone` 80B8 (muzzle and hit cones), `torb_dome` DAF (muzzle / hit / blast / sting domes),
`torb_hemisphere` 963D, `cat_sphere` 1385, `dva_missile_ball` 7CDA (blast and sting balls), `torb_burn_ring_mesh` 86CF,
`torb_burn_ring_b` 5708, `brigitte_dome` 5F08, `cat_bubble` DBF. Not needed: quads 801 / 8C0 / E21 / F5B / 5C75, ribbon meshes 797 /
7F5, the Stadium-only 3299 / 1E7F / 129B / 85B, 5C75.

Proposed palettes (`AshePalettes`, stops t, r, g, b; from `fx_ashe_pal.py`: black at 0, HDR rows hue-clamped, a near-white top
dropped so a solid mask ends on the colour, falling rows reversed; "by age" rows are a band's / ribbon's / smoke's colour over its
life, head first, for `Fx.trail` / `Fx.soft`, not mask palettes). Each line's source row is in `work\fx_palettes2.txt`:
```
ASHE_LINE = {0,0,0,0, .25f,1,.115f,.014f, .5f,1,.229f,.065f, .75f,1,.336f,.112f, 1,1,.434f,.156f};   // 1262 r3 HDR 10 reversed
ASHE_LINE_ENEMY = {0,0,0,0, .25f,1,.08f,.047f, .5f,1,.135f,.07f, .75f,1,.19f,.094f, 1,1,.246f,.117f};   // 0B5A r3 HDR 5 reversed
ASHE_LINE_SCOPED = {0,0,0,0, .25f,1,.159f,.035f, .5f,1,.431f,.164f, .75f,1,.685f,.285f, 1,1,.921f,.397f};   // 2F36 r3 HDR 5 reversed
ASHE_SMOKE_LINE = {0,1,.266f,.034f, .25f,.033f,.015f,0, .5f,.033f,.015f,0, .75f,.033f,.015f,0, 1,.033f,.015f,0};   // 933C r5 by age HDR 10
ASHE_SMOKE_LINE_ENEMY = {0,1,.034f,.034f, .25f,.033f,.015f,0, .5f,.033f,.015f,0, .75f,.033f,.015f,0, 1,.033f,.015f,0};   // E133 r6 by age
ASHE_SMOKE_LINE_SCOPED = {0,1,.266f,.034f, .25f,.05f,.05f,.05f, .5f,.05f,.05f,.05f, .75f,.05f,.05f,.05f, 1,.05f,.05f,.05f};   // 81EC r0 by age (also B.O.B.)
ASHE_MUZZLE_CORE = {0,0,0,0, .25f,1,.197f,0, .5f,1,.34f,0, .75f,1,.439f,0, 1,1,.439f,0};   // D0D5 r4 HDR 5 reversed
ASHE_MUZZLE_DOME = {0,0,0,0, .25f,1,.197f,0, .5f,1,.34f,0, .75f,1,.439f,0, 1,1,.439f,0};   // C412 r4 HDR 2.5 reversed
ASHE_MUZZLE_DOME_ENEMY = {0,0,0,0, .25f,1,.047f,0, .5f,1,.093f,0, .75f,1,.125f,0, 1,1,.125f,0};   // A680 r4 reversed
ASHE_MUZZLE_CONE = {0,0,0,0, .25f,1,.197f,.004f, .5f,1,.218f,.005f, .75f,1,.258f,.008f, 1,1,.624f,.086f};   // 3773 r1 HDR 5
ASHE_MUZZLE_CONE_ENEMY = {0,0,0,0, .25f,1,.001f,0, .5f,1,.093f,.04f, .75f,1,.325f,.187f, 1,1,.597f,.381f};   // EFAD r1 HDR 5
ASHE_SCOPED_CONE = {0,0,0,0, .25f,1,.197f,.004f, .5f,1,.218f,.005f, .75f,1,.258f,.008f, 1,1,.624f,.086f};   // 2400 r1 (B.O.B.'s 204F r1 the same)
ASHE_SCOPED_CONE_ENEMY = {0,0,0,0, .25f,1,0,0, .5f,1,.05f,.028f, .75f,1,.11f,.062f, 1,1,.558f,.381f};   // 6542 r1
ASHE_MUZZLE_FLASH = {0,0,0,0, .25f,.693f,.44f,.062f, .5f,1,.555f,.066f, .75f,1,.52f,.056f, 1,1,.5f,.05f};   // 57C5 r0 HDR 2
ASHE_MUZZLE_FLASH_B = {0,0,0,0, .25f,.626f,.626f,.275f, .5f,.751f,.751f,.351f, .75f,.875f,.875f,.425f, 1,1,1,.5f};   // 420C r0
ASHE_MUZZLE_SPARK = {0,0,0,0, .25f,1,.004f,0, .5f,1,.009f,0, .75f,1,.016f,0, 1,1,.033f,0};   // 178E r0 HDR 30 reversed
ASHE_MUZZLE_SPARK_B = {0,0,0,0, .25f,1,.009f,0, .5f,1,.019f,0, .75f,1,.029f,0, 1,1,.033f,0};   // 4723 r0 HDR 30 reversed
ASHE_MUZZLE_SMOKE = {0,0,0,0, .25f,.531f,.217f,.217f, .5f,.987f,.434f,.434f, .75f,1,.5f,.5f, 1,1,.605f,.605f};   // 8954 r1 HDR 1.37
ASHE_SCOPED_SMOKE = {0,0,0,0, .25f,.531f,.217f,.217f, .5f,.987f,.434f,.434f, .75f,1,.5f,.5f, 1,1,.605f,.605f};   // 069B r1 (same as ASHE_MUZZLE_SMOKE)
ASHE_HIT = {0,0,0,0, .25f,1,.197f,0, .5f,1,.34f,0, .75f,1,.439f,0, 1,1,.439f,0};   // F573 r4 HDR 10 reversed
ASHE_HIT_ENEMY = {0,0,0,0, .25f,1,.032f,0, .5f,1,.063f,0, .75f,1,.084f,0, 1,1,.084f,0};   // 1BD2 r4 HDR 10 reversed
ASHE_HIT_DECAL = {0,0,0,0, .25f,1,.084f,0, .5f,1,.088f,.002f, .75f,1,.252f,.064f, 1,1,.397f,.12f};   // D025 r2 HDR 5 reversed
ASHE_HIT_DECAL_ENEMY = {0,0,0,0, .25f,1,0,0, .5f,1,.001f,.001f, .75f,1,.064f,.064f, 1,1,.12f,.12f};   // 1A50 r2 reversed
ASHE_HIT_CONE = {0,0,0,0, .25f,.204f,0,0, .5f,.539f,0,0, .75f,.865f,0,0, 1,1,0,0};   // D6C3 r0 (L: a channel-0 ramp)
ASHE_COACH_FLASH = {0,0,0,0, .25f,1,.154f,.006f, .5f,1,.22f,.012f, .75f,1,.405f,.159f, 1,1,.738f,.473f};   // D574 r1 HDR 5
ASHE_COACH_FLASH_ENEMY = {0,0,0,0, .25f,1,.041f,.006f, .5f,1,.079f,.012f, .75f,1,.266f,.159f, 1,1,.63f,.473f};   // 291E r1
ASHE_COACH_GLOW = {0,0,0,0, .25f,1,.233f,.007f, .5f,1,.259f,.008f, .75f,1,.322f,.009f, 1,1,.361f,.01f};   // 62A7 r0 HDR 5
ASHE_COACH_SPARK = {0,0,0,0, .25f,.52f,.008f,0, .5f,1,.149f,.009f, .75f,1,.235f,.014f, 1,1,.652f,0};   // 8335 r2 HDR 10
ASHE_COACH_SPARK_ENEMY = {0,0,0,0, .25f,.52f,.004f,0, .5f,1,.046f,.009f, .75f,1,.07f,.014f, 1,1,.122f,0};   // 5AA8 r2
ASHE_COACH_FLARE = {0,0,0,0, .25f,1,.197f,.004f, .5f,1,.218f,.005f, .75f,1,.258f,.008f, 1,1,.624f,.086f};   // 6B6C r1 HDR 5
ASHE_COACH_PELLET = {0,0,0,0, .25f,1,.099f,.105f, .5f,1,.158f,.067f, .75f,1,.207f,.042f, 1,1,.251f,.024f};   // 99A0 r0 HDR 8 reversed
ASHE_COACH_PELLET_B = {0,0,0,0, .25f,1,.076f,.074f, .5f,1,.103f,.04f, .75f,1,.121f,.023f, 1,1,.133f,.013f};   // 5496 r0 HDR 15 reversed
ASHE_COACH_FLAME = {0,0,0,0, .25f,1,.334f,.067f, .5f,1,.501f,.05f, .75f,1,.6f,.04f, 1,1,.667f,.033f};   // 7C18 r0 HDR 15
ASHE_COACH_FLASH_BIG = {0,0,0,0, .25f,1,.301f,.053f, .5f,1,.49f,.055f, .75f,1,.612f,.04f, 1,1,.838f,.381f};   // A893 r1 HDR 5
ASHE_COACH_FLASH_BIG_ENEMY = {0,0,0,0, .25f,1,.033f,.015f, .5f,1,.066f,.031f, .75f,1,.195f,.122f, 1,1,.318f,.209f};   // D285 r1
ASHE_COACH_SMOKE = {0,0,0,0, .25f,.531f,.294f,.294f, .5f,.987f,.124f,.124f, .75f,1,.361f,.361f, 1,1,.605f,.605f};   // EAD9 r1 HDR 1.37
ASHE_DYN_FUSE = {0,0,0,0, .25f,1,.352f,.067f, .5f,1,.43f,.089f, .75f,1,.617f,.144f, 1,1,.73f,.178f};   // 59D7 r1 HDR 5 (DFF9 r1 sparks, 8B98 r1 landed the same)
ASHE_DYN_FUSE_ENEMY = {0,0,0,0, .25f,1,.125f,.067f, .5f,1,.167f,.089f, .75f,1,.27f,.144f, 1,1,.332f,.178f};   // 6FF7 r1 (E9D9 r1, BDB8 r1 the same)
ASHE_DYN_SPARK = {0,0,0,0, .25f,1,.352f,.067f, .5f,1,.43f,.089f, .75f,1,.617f,.144f, 1,1,.73f,.178f};   // DFF9 r1
ASHE_DYN_SPARK_ENEMY = {0,0,0,0, .25f,1,.125f,.067f, .5f,1,.167f,.089f, .75f,1,.27f,.144f, 1,1,.332f,.178f};   // E9D9 r1
ASHE_DYN_TRAIL = {0,1,.445f,.034f, .25f,.92f,.344f,.037f, .5f,0,0,0, .75f,0,0,0, 1,0,0,0};   // 7D63 r0 by age HDR 3
ASHE_DYN_TRAIL_ENEMY = {0,1,.153f,.034f, .25f,1,.09f,.04f, .5f,0,0,0, .75f,0,0,0, 1,0,0,0};   // 448D r0 by age HDR 8
ASHE_DYN_SMOKE = {0,1,.887f,.366f, .25f,.145f,.145f,.145f, .5f,.042f,.042f,.042f, .75f,.037f,.037f,.037f, 1,.037f,.037f,.037f};   // 6A26 r3 by age
ASHE_DYN_SMOKE_ENEMY = {0,1,.097f,.097f, .25f,.145f,.145f,.145f, .5f,.042f,.042f,.042f, .75f,.037f,.037f,.037f, 1,.037f,.037f,.037f};   // 829F r3 by age
ASHE_DYN_LIT = {0,0,0,0, .25f,1,.352f,.067f, .5f,1,.43f,.089f, .75f,1,.617f,.144f, 1,1,.73f,.178f};   // 8B98 r1
ASHE_DYN_LIT_ENEMY = {0,0,0,0, .25f,1,.125f,.067f, .5f,1,.167f,.089f, .75f,1,.27f,.144f, 1,1,.332f,.178f};   // BDB8 r1
ASHE_DYN_LIT_SWIRL = {0,1,.347f,.061f, .25f,.161f,.027f,.027f, .5f,.021f,.021f,.021f, .75f,.013f,.013f,.013f, 1,.004f,.004f,.004f};   // 77EC r8 by age
ASHE_BLAST_SHELL = {0,0,0,0, .25f,1,.321f,.025f, .5f,1,.38f,.025f, .75f,1,.523f,.027f, 1,1,.61f,.027f};   // BC76 r2 HDR 5
ASHE_BLAST_SHELL_ENEMY = {0,0,0,0, .25f,1,.069f,.025f, .5f,1,.084f,.025f, .75f,1,.121f,.027f, 1,1,.144f,.027f};   // BB62 r2
ASHE_BLAST_SPHERE = {0,0,0,0, .25f,1,.071f,.023f, .5f,1,.086f,.022f, .75f,1,.124f,.02f, 1,1,.147f,.018f};   // C390 r3 HDR 5
ASHE_BLAST_BALL = {0,0,0,0, .25f,.103f,.008f,.001f, .5f,.927f,.184f,.012f, .75f,1,.274f,.018f, 1,1,.611f,.041f};   // 3CBA r4 HDR 10 (F21D r4, B0CB r4 the same)
ASHE_BLAST_BALL_ENEMY = {0,0,0,0, .25f,.103f,.004f,.001f, .5f,.927f,.071f,.012f, .75f,1,.097f,.018f, 1,1,.181f,.041f};   // 26EE r4
ASHE_BLAST_DOME = {0,0,0,0, .25f,.103f,.008f,.001f, .5f,.927f,.184f,.012f, .75f,1,.274f,.018f, 1,1,.611f,.041f};   // F21D r4
ASHE_BLAST_DOME_ENEMY = {0,0,0,0, .25f,.103f,.004f,.001f, .5f,.927f,.071f,.012f, .75f,1,.1f,.018f, 1,1,.205f,.041f};   // 8BFC r4
ASHE_BLAST_FIRE = {0,0,0,0, .25f,1,.107f,.011f, .5f,1,.207f,.014f, .75f,1,.406f,.027f, 1,1,.61f,.041f};   // 72C0 r4 (7506 r4 the same)
ASHE_BLAST_SPARK = {0,0,0,0, .25f,1,.412f,.034f, .5f,1,.412f,.034f, .75f,1,.412f,.034f, 1,1,.412f,.034f};   // FC12 r1 tint (10, 4.12, 0.34)
ASHE_BURN_FLAME = {0,0,0,0, .25f,.504f,.135f,.008f, .5f,1,.451f,.029f, .75f,1,.519f,.034f, 1,1,.611f,.041f};   // 0454 r12 HDR 10
ASHE_BURN_FLAME_ENEMY = {0,0,0,0, .25f,.502f,.015f,.001f, .5f,1,.076f,.007f, .75f,1,.098f,.009f, 1,1,.12f,.011f};   // D2A3 r12
ASHE_BURN_SPARK = {0,0,0,0, .25f,1,.412f,.034f, .5f,1,.412f,.034f, .75f,1,.412f,.034f, 1,1,.412f,.034f};   // FE83 r1 tint
ASHE_BURN_RING = {0,0,0,0, .25f,.888f,.198f,0, .5f,1,.256f,0, .75f,1,1,.206f, 1,1,1,.733f};   // B8BD r6
ASHE_BURN_RING_B = {0,0,0,0, .25f,.789f,.143f,0, .5f,.86f,.16f,0, .75f,1,.771f,.113f, 1,1,1,.536f};   // 5ECA r6
ASHE_BURN_EMBER = {0,0,0,0, .25f,1,1,.342f, .5f,1,1,.503f, .75f,1,1,.645f, 1,1,1,.789f};   // A8A6 r0
ASHE_BOB_LINE = {0,0,0,0, .25f,1,.213f,.067f, .5f,1,.242f,.067f, .75f,1,.271f,.067f, 1,1,.3f,.067f};   // 7441 r3 HDR 10 reversed
ASHE_BOB_LINE_ENEMY = {0,0,0,0, .25f,1,.078f,.067f, .5f,1,.089f,.067f, .75f,1,.1f,.067f, 1,1,.112f,.067f};   // 745C r3 reversed
ASHE_BOB_MUZZLE_SPARK = {0,0,0,0, .25f,1,.055f,0, .5f,1,.112f,0, .75f,1,.172f,0, 1,1,.2f,0};   // FC0C r0 HDR 5 reversed
ASHE_BOB_MUZZLE_SMOKE = {0,0,0,0, .25f,.372f,.072f,.072f, .5f,.625f,.011f,.011f, .75f,.814f,.009f,.009f, 1,1,.009f,.009f};   // 807E r2 (L: tints the colour puff)
ASHE_BOB_CHARGE = {0,0,0,0, .25f,1,.388f,.004f, .5f,1,.453f,.005f, .75f,1,.565f,.007f, 1,1,.564f,.007f};   // 47CC r5 HDR 5
ASHE_BOB_CHARGE_ENEMY = {0,0,0,0, .25f,1,.116f,.004f, .5f,1,.15f,.005f, .75f,1,.209f,.007f, 1,1,.209f,.007f};   // 7B6E r5
ASHE_BOB_CHARGE_TRAIL = {0,0,0,0, .25f,1,.302f,.014f, .5f,1,.361f,.018f, .75f,1,.486f,.026f, 1,1,.552f,.031f};   // 9C50 r6 HDR 5
ASHE_BOB_CHARGE_TRAIL_ENEMY = {0,0,0,0, .25f,1,.116f,.004f, .5f,1,.15f,.005f, .75f,1,.209f,.007f, 1,1,.209f,.007f};   // 2C70 r6
ASHE_BOB_KNOCK = {0,0,0,0, .25f,1,.197f,.004f, .5f,1,.218f,.005f, .75f,1,.258f,.008f, 1,1,.624f,.086f};   // 4C69 r1 HDR 5
ASHE_BOB_KNOCK_ENEMY = {0,0,0,0, .25f,1,.036f,.004f, .5f,1,.048f,.005f, .75f,1,.071f,.008f, 1,1,.275f,.086f};   // 43EC r1
ASHE_BOB_KNOCK_STREAK = {0,0,0,0, .25f,1,.426f,.092f, .5f,1,.548f,.119f, .75f,1,.674f,.147f, 1,1,.799f,.175f};   // A321 r1 HDR 15 reversed
ASHE_BOB_KNOCK_STREAK_ENEMY = {0,0,0,0, .25f,1,.149f,.092f, .5f,1,.233f,.119f, .75f,1,.317f,.147f, 1,1,.402f,.175f};   // C1B4 r1 reversed
ASHE_BOB_KNOCK_DOME = {0,0,0,0, .25f,1,.521f,.163f, .5f,1,.591f,.209f, .75f,1,.66f,.254f, 1,1,.73f,.301f};   // 2073 r3 HDR 2
ASHE_BOB_KNOCK_DOME_ENEMY = {0,0,0,0, .25f,1,.205f,.163f, .5f,1,.293f,.209f, .75f,1,.38f,.254f, 1,1,.468f,.301f};   // 8E08 r3
ASHE_BOB_DAZED = {0,0,0,0, .25f,.201f,0,0, .5f,.401f,.053f,.002f, .75f,.599f,.214f,.011f, 1,.8f,.356f,.02f};   // 84F2 r3
ASHE_STING_DOME = {0,0,0,0, .25f,.103f,.008f,.001f, .5f,.927f,.184f,.012f, .75f,1,.274f,.018f, 1,1,.611f,.041f};   // B0CB r4 HDR 10
ASHE_STING_SPARK = {0,0,0,0, .25f,1,.206f,.018f, .5f,1,.376f,.018f, .75f,1,.561f,.018f, 1,1,.731f,.018f};   // 55E1 r1 HDR 1.23
ASHE_STING_BALL = {0,0,0,0, .25f,1,.107f,.011f, .5f,1,.207f,.014f, .75f,1,.406f,.027f, 1,1,.61f,.041f};   // 7506 r4
ASHE_DETONATOR_FLAME = {0,0,0,0, .25f,.382f,0,0, .5f,1,.155f,.065f, .75f,1,.362f,.151f, 1,1,.597f,.597f};   // 2BBE r11 HDR 10
```
Duplicates kept by role (the builder may alias them: `ASHE_SCOPED_CONE` = `ASHE_MUZZLE_CONE` = `ASHE_COACH_FLARE` = `ASHE_BOB_KNOCK`;
`ASHE_DYN_SPARK` / `ASHE_DYN_LIT` = `ASHE_DYN_FUSE`; `ASHE_BLAST_DOME` = `ASHE_STING_DOME` = `ASHE_BLAST_BALL`; `ASHE_STING_BALL` =
`ASHE_BLAST_FIRE`; `ASHE_SCOPED_SMOKE` = `ASHE_MUZZLE_SMOKE`; `ASHE_MUZZLE_DOME` = `ASHE_MUZZLE_CORE` = `ASHE_HIT`). `ASHE_DETONATOR_GLOW`
in `fx_palettes2.txt` is a parameter row (L): not proposed.
