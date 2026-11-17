# Pharah's ability effects in the game data

What Pharah's effects are made of in real Overwatch, where the extracted files are, and how each would be rebuilt in Minecraft
(a `PharahFx` beside `TorbjornFx` / `DvaFx`; today the server sends vanilla `EXPLOSION` / `FLAME` / `EXPLOSION_EMITTER` / `CLOUD` from
`pharah/Pharah.java`, and her jets draw nothing; issue #150). Read 2026-11-17 from the archived offline build 154088. Overwatch and
Battle.net were checked closed (`Get-Process Overwatch, Battle.net`) before the TankLib run, and `owinit.py` refuses on its own;
everything was read only, offline, through TankLib/DataTool. Text only in the repo: masks, ramps and meshes stay in the owner's archive
and reach the game through the local asset pack. Format: [illari-effects.md](illari-effects.md) /
[torbjorn-effects.md](torbjorn-effects.md). Data map: [pharah-map.md](pharah-map.md); public numbers: [pharah.md](pharah.md); method:
`techniques/rendering/rendering.md` "Effects and glows".

Confidence: **H** read in the data, **M** inferred, **L** guess. "Size" is the spawn size (a 1 m quad at size 0.5 is 0.5 m; a ribbon's
is its width; a mesh's size scales its own extent). "Ours" marks what no data gives. `XXXX rN` = the colour LUT (last 4 hex digits of
`ramps\8...XXXX.json`) and its row; hex runs are that row at 0 / 25 / 50 / 75 / 100 %. "hp" = hardpoint. `uniform(a, b)` as read (M:
a +- b). `ECIN` sizes are set by data flow from the projectile (not decoded, L: the blast radius).

## Files and scripts
- `E:\Overwatch Archive\research\pharah\fx\Pharah\<Ability>\` (`RocketLauncher`, `ConcussiveBlast`, `JumpJet`, `JetDash`,
  `HoverJets`, `Barrage`, `Perks`): `textures\`, `ramps\`, `models\Models\<id>.00C\<id>.owmdl`, `manifest.json`.
- `research\pharah\work\`: `fx_cards_{rocket,concussive,jets,barrage}.txt` and their `_compact.txt`, `fx_overview.txt`,
  `fx_summary2.txt|json`, `fx_meshes2.json`, `fx_entities.txt` (rocket 0033, mini-rocket 0052, Salvo's 7769, Concussive 010F, hero
  child 130D), `fx_clip_effects.txt`, `fx_effects\`, `fx_extract_log.txt` / `fx_extract_log2.txt`, `fx_palettes2.txt` (the `PH_*`).
- Scripts `research\pharah\scripts\fx\`: Tracer's `fx_tracer_*` copied as `fx_pharah_*` (`fx_pharah_extract.py` holds her effect
  list with each one's graph node). Run: `fx_pharah_extract.py [Ability ...]` (kiriko-anim venv, `-I`); then under `py -I run.py`:
  `fx_pharah_summary2.py`, `fx_pharah_overview.py`, `fx_pharah_card.py <ids>`, Torbjörn's `fx_torbjorn_cardsc.py <cards>`;
  `fx_pharah_rows.py pharah <lut4 | t:texid>` (new: a LUT's rows in all four channels, or a colour-ramp texture's colours);
  `fx_pharah_pal.py` (her `PAL` list) read by `..\..\junkrat\scripts\fx\fx_junkrat_pal.py pharah` (the rules are in its header).
- The first run (2026-11-16) stopped inside Barrage; this pass re-ran Barrage and Perks. Counts: **48 effects in the manifests (the
  list + sub-effects 0554, 405B, 4010, 1902), 0 missing.** 18 draw something; the rest are sounds only or empty (2860 / 2B1E the
  launcher's children, her fire / reload / idle clips, 009B / 5F58, 05AC / 5921 / 5939, 00F7 (the hero's always-on child: empty),
  00A7, 00B7, 1FCB, 00A8, 6063 / 74B7).

## Her look
- **Rockets: gold for her side, a redder twin for enemies (H rows, M roles).** The launch, the rocket and its blast come as pairs
  (muzzle 9421 r2 cream > orange / 9E23 r2 pink > red; flame 0096 r4 / 8F66 r4; fireball 8911 r1 `#000000 > #DB7900 > #FEAD3C >
  #FCDC67 > #FEFFE1` / r2 `... > #FE893C > #FEB867 > #FFF9E1`; Barrage's pods 31C0 orange / 5DA3 red). Draw added.
- **Concussive Blast: blue for her side, red for enemies (H rows, M roles),** as Tracer's: 25AF r3 `#002FFF > #7880FF > #A9B0FF >
  #C6D0FF > #C9D3FF` / r4 `#FF0C00 > #FF7C78 > #FFADA9 > ... > #FFCDC9`, on every part of it.
- **Hover Jets burn blue, Jump Jet / Jet Dash orange (H).** The hover's colours are colour-ramp textures times constant HDR tints:
  ramp **3200** `#6099FF > #0D9AFF > #24B3FF > #99DFFF > #D9F4FF` x 7638 r2 (0.37, 3.94, 5.34) = cyan-blue; each hover renderer also
  carries an orange-red second LUT (5311 / C95C / 3924: L the enemy twin). The jumps use ramp **31C0** `#FF7F00 > #FFB600 > #FFE200 >
  #FFFF08 > #FFFFFF` x (4.86, 3.94, 1).
- **Channel-shifted LUTs (M, as Tracer's):** the launch's 4D5C / 8F66 / EDC2 / 31E4 r4, 170A / 5A3C r0, 64DC / 409F r0 carry their
  colour in channels 2-4 (4D5C r4 shifted = 0096 r4 exactly, H).
- **Hardpoints (H ids from her map and the cards, M roles):** the launcher's **0006** (muzzle, `pharah_weapon.glb`); her **0038**
  (left wrist: Concussive's muzzle); **0034 / 0035** (shoulder pods) and **0036 / 0037** (hip pods: Barrage, cycled 34 35 37 36 37 35
  34 36); **002C-0031** (the six jets: hover, jumps); **0004** (body: the jumps); 00A4 roots. All present in `pharah.glb`.

## Rocket Launcher (0023; rocket 0033, its child 015B; 35 m/s)
| Part | Effect | Rebuilt (replaces) |
|---|---|---|
| Launch | **0091** (0033 `m_A18274A3`, 1 s, at the launcher's **0006**) H: four flashes stretched **x7.5** along the shot, **0.2-0.29 s**: 220A (`sombra_impact_burst`) **0.5 m** (1P, M) / **0.96 m** (3P) (9421 r2 cream > orange, HDR 5; twin 9E23 r2); flame 299E6 x 1CB5D x 7ADD (4) 2 at **0.49 / 1.12 m** (0096 r4 black > gold > cream, HDR 2; twin 8F66 r4); glare 98D 0.47 / 0.97 m (170A r0 shifted; twin 5A3C); swirl 30E12 x 1CB5D 0.29 / 0.74 m (B8D9 r3; twin B891); flames at 0006 -0.1 m **0.92-1.03 m**, 0.21-0.29 s (EDC2 / 31E4 r4 shifted); **sparks 1F35 16-20 at 4-7.4 m/s**, 0.07 m, stretch 7.5, cone 75 deg, at 0.033 s (64DC r0 shifted gold; twin 409F); sub-effect 405B (sound). Her fire clips 008B / 1CA3 and the launcher's 008C / 0334-0338: sounds or empty (H) | at the posed muzzle 0006 from `launcher` (Pharah.java:295-297, after `rocket.fire`): one streak flash 0.5 m in her view / 1 m for others (`sombra_impact_burst`, `PH_MUZZLE`), the flame (`ph_flame_noise`, `PH_MUZZLE_FLAME`), 6 sparks (ours: a third), stretched along the aim; `_ENEMY` rows to enemies; no vanilla call today |
| In flight | **0078** (015B STUEffectComponent, 1.5 s looping M) H: glow 4C09 **0.4 m** 9 /s + 1, 0.14-0.28 s at -0.1 m (33D6 r1 black > orange > gold, HDR 2; tint (3, 2.9, 1.7)); flare 9EE **1 m** 7 /s + 2, 0.5-0.65 s (10A7 r1 brown > orange > yellow); exhaust flames 1CB5D x 299E6 x 7ADD **16 /s at 3-5 m/s, 0.99 +- 0.35 m, 0.66-0.73 s** (1E25 r5 dark red > red > gold) and 16 /s at 2 m/s, 0.95-1.06 m, 0.47-0.55 s (D8BD r5 gold); glow 2A3E1 1 m 5 /s (9489 r1); **smoke ribbons 7F5 x 2A480 x 2A063 0.7 m wide, 60 points/s, living 2 s** (749A r5 yellow > grey by age; the second B09C r5 orange > dark red), a thin ribbon 31AF x 299E1 x 299E3 x 2A063 **0.25 m, 2 s** (9422 r7 HDR 33 yellow > grey); two lights 3 m. Model 084C / look 07AF (in the pack: `projectile_pharah_rocket.glb`) | per rocket each frame (ProjectileModelRenderer case `PharahRocketEntity`, kind ROCKET): the 0.4 m glow on its tail (`cat_soft`, `PH_ROCKET_GLOW`), a 1 m flare at 0.4 alpha (`ph_flare`, ours), 2 exhaust flames a tick (ours: a third), `Fx.trail` 0.35 m / 1 s smoke (`torb_trail`, soft, `PH_ROCKET_TRAIL`; ours half the data's 0.7 m and 2 s: a stream of rockets would wall the view) and the thin 0.1 m hot streak (`cat_noise`, `PH_ROCKET_STREAK`); replaces nothing (no particle today) |
| Blast | **0115** (0033 `m_6CB564CC`, 2 s; sizes by data flow, L the 2.5 m radius) H: **fireball 2291 (12) four copies turned 90 deg, at 0.033 s, 0.37 +- 0.14 s**, 1-1.5 m/s, stretch 2-2.2 (8911 r1 / r2 twins; tint 4.7); streak flash 98C 0.25 s stretch 7.5 (DE90 r1 / r2); flare 22D0 0.25 s stretch 7.5 (BAA0 r1 / r2); **50 sparks 1F35 at 9.2-16.2 m/s**, 0.2-0.5 s, 0.01-0.023 m, stretch 2 (527D r1 black > orange > peach); **debris 1F35 x 22A8, 1-2 + 2-6 at 6.8-11 m/s, 1 +- 0.55 s, 0.16-0.28 m**, each with sub-effect **0554** (smoke ribbons 9ED x 4DB, 30 + 8 points/s, 1.39 s, width curve to ~1.46 m (M): E8FD r4 red > dark / 2826 r4 orange > grey); smoke 377D5 (48) x 1FC65 x B08 on quad 9B1, 0.86 +- 0.46 s, stretch 4.1-5.3 (E1D0 r3 beige); sphere 1BE0C on mesh DBF 0.15 s (A33A tint (3, 0.6, 0) / 494B red); two lights 3 m 0.33 s | replaces `EXPLOSION` + 6 `FLAME` (Pharah.java:609-611, kind ROCKET) with `Effects.play(level, "ph_rocket_burst", at, ...)`: a 2.5 m fireball flipbook (`dva_missile_fire#12`, `PH_BLAST_FIRE`) as 2 crossed copies (ours: 4 read as one blob), the streak flash 2 m, the flare, 20 sparks, 3 debris each with a 0.3 m / 1 s `Fx.trail` (`jr_smoke_ribbon`, `PH_DEBRIS_TRAIL`), one smoke (`smoke#48`, soft, `PH_BLAST_SMOKE`) |
| 5F3C | 0023 #727 on message 270D, gated on 0766.134 (not her base kit: Stadium, L): an 8.5 m DBF flash and a 22D0 0.42 m | not drawn |

## Concussive Blast (007B; projectile 010F, its child 0160; 60 m/s, 8 m)
| Part | Effect | Rebuilt (replaces) |
|---|---|---|
| Launch | **012F** (010F `m_A18274A3`, at its spawn +0.06 m = her wrist 0038) H: glows A7B6 **0.6 m and 0.3 m, 0.17 s** (114A r1 navy > `#2469FF > #6F8DFF > #95A7FF > #B2BEFF` / r2 red > salmon: blue / red twins); flash 299E9 x 299E3 x 1CB5E (4) 0.66 m (6C6B r3 blue / r4 red) and 0.33 m (22A5 r3 blue); sub-effect 4010 (sound). Clips 00AB / 03E0: sounds | at her posed 0038 from `concussive` (Pharah.java:348): the 0.6 m glow + the 0.66 m flash (`glow`, `juno_hit_ring#4`; `PH_CONC_LAUNCH` / `_ENEMY`) |
| In flight | **0132** (0160, 3 s) H: glow 4C09 **0.6 m** 8 /s + 2, 0.46 s at -0.22 m (2C95 r1 blue / r3 red, HDR 6); halo 9EE **1.9 m** 2 /s + 2, 1.06 s (762A r1 / r3); ribbon 75D x 1298 60 points/s, 0.3 s, width from data (L 1 m) at +0.31 m (40B0 r2 blue / r3 red, HDR 6); **sparks 1F35 x 52BC 135 /s at 2.9-7 m/s**, 0.35-0.48 m, 0.22-0.42 s, cone 15 deg (30D3 r0 yellow > red; tint (1.16, 1.82, 3) blue); **smoke ribbon 7F5 x 4DB x 1EF26 x 2A480 0.55 m, 60 points/s, 1 s** (4018 r8 white-blue > grey / r9 orange > grey: twins). Model 084C / 07AF (the rocket's, H) | per shot (case `PharahRocketEntity`, kind CONCUSSIVE): the 0.6 m glow and a 1.2 m halo at 0.3 alpha (ours), `Fx.trail` 0.3 m / 0.3 s (`dva_solid`, `PH_CONC_TRAIL`), a 0.3 m / 1 s smoke trail (soft, `PH_CONC_SMOKE_TRAIL`), 3 sparks a tick (ours: of 135 /s) |
| Burst | **0116** (010F `m_6CB564CC`, 1.5 s) H: **dome mesh 963D x 16E9D at size 1.5 (3 x 1.5 m), 0.44 s** (25AF r3 / r4); **swirl mesh ED1C (0.35 x 0.44 x 0.58) x 7A9 x 299E3 x 38E4A at size 27 = ~9 x 12 x 16 m, 0.33 s from 0.033 s** (D704 r5 blue-violet / r7 salmon); **ground wave mesh 96C1 (a 1.28 m disc) x 4A7E x 2CC98 x 1038 at size 10 = 12.8 m, 0.22 s from 0.067 s**, stretch 7.5 (373E r5 / r6, HDR 5); **shell 299E8 on mesh 085B size 12, 0.28 s** (F87C r3 / r4); core 299E9 on 1385 **3.5 m, 0.08 s** (D8A1 r3 / r4); dome 299E9 x 4DB on 963D **size 6**, 0.26 s (284E r4 / r5); **32 sparks 1F35 at 10-21 m/s**, 0.12 +- 0.21 s, 0.77 +- 0.28 m, stretch 0.5-1 (B660 / 437A: parameter rows, M: `PH_CONC_CORE`); distortion C4A (8004 2036 curve); two lights 3 > 10 m over 0.47 s | replaces `EXPLOSION_EMITTER` + 30 `CLOUD` (Pharah.java:654-655) with `"ph_conc_burst"`: the 12.8 m ground wave flat (`ph_disc` mesh with `ph_wave_band`, `PH_CONC_WAVE`, 0.22 s), the 12 m shell at 0.2 alpha (`ph_sphere` mesh or `cat_sphere`, `PH_CONC_SHELL`), the swirl (`ph_conc_swirl`, `PH_CONC_SWIRL`, 0.33 s), the 3 m dome, the 3.5 m core flash, 16 sparks; `_ENEMY` rows to enemies |

## Hover Jets (0025, hold jump), Jump Jet (0027), Jet Dash (4B92)
| Part | Effect | Rebuilt (replaces) |
|---|---|---|
| **Hovering** | **00F3** (0025 #72, held under #122 = she is hovering, H) at **each of the six jets 002C-0031** (+0.05 m), 1 s looping H: a flash 9EE x 3200 **1.5 m** once at the start, 0.29 s (tint 7638 r2 (0.37, 3.94, 5.34): blue); **flame 139C x 3200 15 /s, 0.2-0.24 s, 1.5 m/s, 0.58 m** (4DC7 r2 white > blue, HDR 8; second LUT 5311 r0 orange-red); **sparks 139C 60 /s, 0.1-0.14 s, 2-2.9 m/s, 0.19 m** (tint 00A0 r2 (0.23, 2.92, 6.16); C95C r0); **jet cone 5233 x 3200 60 /s + 8 from 0.33 s, 0.21 +- 0.13 s, 0.62 > 0.19 m, cone 40 deg** (1281 r2 blue, HDR 6.4; 3924 r0); two lights 1.29 m. 05AC (#126), 5921 (#723), 00A7: sounds | client `PharahFx.frame()`: every hovering Pharah, at her six posed jets: one jet cone a tick (`ph_jet`, `PH_HOVER_JET`, 0.4 m, ours), a flame every other tick (`mercy_soft`, `PH_HOVER_FLAME`, 0.5 m, down at 1.5 m/s), a spark every 2 ticks (`PH_HOVER_SPARK`), the 1.5 m flash at the start (`ph_flare`, `PH_HOVER`, 0.3 alpha); **needs a hover bit**: `s.pharahHovering` (Pharah.java:146-152) isn't in `sync()`'s mode (:704-706), add `MODE_HOVER` |
| Fuel out | **0185** (the hero's statescript component value; 5939 on #30 finished is sounds only) L the jets sputtering: 4-6 drips 1F5B / 1F5C (4) x 4DB at 2-3.3 m/s (6D72 r0 orange); 90 sparks EA42 (4) at 2.5-3 m/s stretch 6.3-7.9 (3DEB r0 red); flare quad F65D 0.41-0.46 m (D039 r4 gold); 3 glows 210FD 0.57-0.77 m (6D6E r0 orange); distortion; sub-effect 1902 (disc 327C 0.28 m, glints 3A4F8 0.1 m) | at `hover.empty` (Pharah.java:182): 10 sparks and 2 drips at her posed jets (ours), `PH_HOVER_FLAME_B`, drips `torb_drip#4`; L which event plays it |
| 6056 | 0025 #310, gated on 07B9.134 with a target track (not her base kit: Stadium, L): a landing slam (decal 3E7BD 3 x 5 m, ring 958 3 m, torus EBEF 1.75 m, 40 sparks F2F1) | not drawn |
| **Jump Jet** | **00F2** (0027 #55 on the impulse #36, 3 s) at the jets **002C-002F** and her **0004** H: flash 9EE x 31C0 **1.82 m, 0.3 s** (tint (4.86, 3.94, 1): orange-gold); ring mesh **1E8E (a 1.28 m disc) x 3AE1 (14 frames) at size 1.0, 0.34 s**; **flames 139C x 31C0 70 /s for 0.33 s, 4 m/s, 0.55 m, 0.2-0.24 s** (1B05 r2 tint 10: hot white-gold); **smoke 1DFCB x B08 x 1DFCA 180 /s for 0.67 s at 002C +0.7 m, 0.86-1.36 s, 2-3 m/s, 0.6 +- 0.58 m** (B2B1 r1 rose-grey, HDR 1.4); light 3 m 0.27 s at +0.74 m. Her clip 00B7: a camera shake and sounds | at `jumpJet` (Pharah.java:230-232): the flash at her posed 0004 below her, the ring flat under her (`ph_jet_ring#14` on `ph_jet_disc`), flames from the four jets for 0.33 s (`mercy_soft`, `PH_JUMP_FLAME`, 2 a tick, down at 4 m/s), smoke 0.67 s (`brigitte_smoke#32`, soft, `PH_JUMP_SMOKE`, ours: 2 a tick of 180 /s) |
| **Jet Dash** | **5910** (4B92 #55): **the same renderers, sizes and times as 00F2** (H) | the same at `jetDash` (Pharah.java:244-247), the flames along her dash instead of down (ours) |

## Barrage (002B) and Rocket Salvo (51E9 #691)
| Part | Effect | Rebuilt (replaces) |
|---|---|---|
| The pods | 3P clip **008A** (her barrage clip 384, 4.33 s) at the pods **0034-0037** and the jets **002C-0031** H: twin pairs on ramps 31C0 (orange, her side) / 5DA3 (red, M enemies): launch flashes 98D stretched x7.5 **0.1 m 5 /s** and 5E9C (3) 0.25-0.36 m 5 /s, 0.15 s, for 3.97 s; flashes 242 **0.3 m 3 /s** stretch 7.5 (7CE2 r2 / FAB3 r2, HDR 12 / 19); **flames 139C 70 /s from 0.37 s for 3.43 s, 2 m/s, 0.55 m** (9159 r2 tint 8) and small ones 50 /s 0.09 m; blue glows 4A7E x 27BFD on quad DFEC 20 /s 0.2 m all ways (809D r6, HDR 15); 1CB5E (4) on quad 801 1 m 2 /s (AA0C). 1P clip 00A8 and 1FCB (#71): sounds | client `PharahFx.frame()` while `MODE_BARRAGE` (already synced, Pharah.java:706): at each posed pod a 0.3 m flash per rocket from it (`mercy_glow`, `PH_POD_FLASH`), flames from the jets 1 a tick (`mercy_soft`, `PH_POD`), a blue glow at 0.5 alpha (`ph_pod_glow`, `PH_POD_GLOW`); ours: the flash timed to `barrage`'s shots (:374-377), not the clip's 5 /s |
| A mini-rocket | **0080** (0052's child 015F, 10 s; **Salvo's 7769 has the same child and hits, H**) H: glow 22A8 x 4DB x 29F7 8 /s 0.36 s, size data flow (FE81 r0 red > orange; tint 16); glow 242 x 20D0 20 /s 0.1 s (7E28 r0; tint (12.3, 0.67, 0.05)); **thin ribbon 31AF x 299E1 x 299E3 x 2A063 0.18 m, 120 points/s, 1 s** (B270 r7 HDR 33 yellow > grey by age); **ribbons 7F5 x 37021 x 2A063 0.3 m, 60 points/s, 0.3 s and 0.2 s** (F758 r3 / 1604 r3; CEDB r0 red) | per mini-rocket (case `PharahRocketEntity`, kind BARRAGE / SALVO): a 0.25 m glow (`lucio_spark`, `PH_MINI_FLAME`, ours size) and `Fx.trail` 0.1 m / 0.3 s (`cat_slash`, `PH_MINI_TRAIL`) |
| A hit | **0081** (0052 `m_AED73588` and `m_BF7914D4`: creature and world share it, 2 s; sizes data flow, L the 1.5 m radius) H: fireball 2291 (12) four copies at 0.033 s, 0.37 +- 0.14 s (7A23 r1 / F4AA r1 twins); **5-15 sparks 4F3 at 5-10.7 m/s**, 0.015 m, 0.15-0.35 s (67D0 r0 yellow > red); streak flashes 98C x 22A8 / 5DA3 x 98C 0.25 s stretch 7.5 (A280 / 1E18: HDR 28); smoke 3E28 x B08 x 22ED x2, 0.8 s, 4.3-5.8 m/s, stretch 4-5 (1A11 r0 orange > black) | replaces `EXPLOSION` (Pharah.java:609, kind BARRAGE / SALVO; no `FLAME` there, :610) with `"ph_mini_burst"`: a 1 m fireball (`dva_missile_fire#12`, `PH_MINI_HIT`), 4 sparks (`lucio_soft`), one smoke (soft); 75 of them in 2.5 s, so no light and half the sparks (ours) |
| Rocket Salvo | 51E9 #691 fires two 7769 from the launcher's 0006 (pharah-map); 6063 (#693 the volley finished) / 74B7 (#696): sounds only; **7769 has no launch effect slot** (H) | `salvo` (Pharah.java:315-326): the mini-rocket look and hits above; at the muzzle the rocket launch at half size (ours: none in the data) |

## Where it hooks in (each vanilla call it replaces)
| Today (`src/main/java/dev/overcraft/...`) | Becomes |
|---|---|
| `pharah/Pharah.java:609-611` `EXPLOSION` (+ 6 `FLAME` unless Barrage) | kind ROCKET: `Effects.play(level, "ph_rocket_burst", at, ...)` (0115 + 0554); BARRAGE / SALVO: `"ph_mini_burst"` (0081) |
| `pharah/Pharah.java:654-655` `EXPLOSION_EMITTER` + 30 `CLOUD` | `"ph_conc_burst"` (0116), team / enemy rows by the viewer |
| none: rocket launch (`launcher` :295) / Concussive launch (`concussive` :348) | `"ph_rocket_launch"` (0091) at her weapon's posed 0006; `"ph_conc_launch"` (012F) at her posed 0038 |
| none: rocket / mini-rocket / Concussive flight | `PharahFx.rocketFlight` from `ProjectileModelRenderer` (case `PharahRocketEntity`, `kind()` picks 0078 / 0080 / 0132) |
| none: hover (`hovered` :146) | client `PharahFx.frame()` on a new `MODE_HOVER` bit in `sync()` (:704): 00F3 at 002C-0031 |
| none: Jump Jet (:230) / Jet Dash (:244) | `"ph_jump_jet"` / `"ph_jet_dash"` (00F2 / 5910, identical), entity = her (the client poses 002C-002F, 0004) |
| none: Barrage pods (`barrage` :374-377) | `PharahFx.frame()` on `MODE_BARRAGE` (008A), and a pod flash per shot (`"ph_pod"`, variant = the pod index) |
| none: fuel out (`hover.empty` :182) | `"ph_hover_empty"` (0185, L) |
- Hardpoints to pose (HeroModelRenderer's `POINTS`): `hardpoint_002C` ... `hardpoint_0031`, `hardpoint_0034` ... `hardpoint_0037`,
  `hardpoint_0038`, `hardpoint_0004`; `pharah_weapon.glb`'s `hardpoint_0006` (all in the pack's glbs).
- Palettes: the `PH_*` arrays below in a `PharahPalettes.java`; `ART` / `ART_ENEMY` lists as `DvaFx`'s, the Concussive and hover
  rows picked blue (team) / red (enemy) by the viewer as `TracerFx` does.

## Not found / not done
- Sizes set by data flow (ECIN: the rocket and mini-rocket blasts, the mini-rocket's glow), the Concussive ribbon's width (sb1 0..1),
  the 0554 ribbon's width curve: not decoded (L: drawn from the radii, ours).
- Whether the hover's orange-red second LUTs (5311 / C95C / 3924) are the enemy twin (L) or a fuel / overfuel tone; which event plays
  0185 (L: fuel out); which of 0091's renderers are 1P / 3P (M from the two sizes: 0.5 vs 0.96 m).
- No team colour on the rocket's own smoke beyond the two ribbon rows (749A / B09C, M twins).
- Material effects and the distortion layers (C4A, 18F81, 1BE0C's), the Stadium-gated 5F3C and 6056, the fall-damage 01E4
  (STUFallingDamageComponent, generic: not hers): not drawn.

## For the asset pack (sources: add `E:\Overwatch Archive\research\pharah\fx`)
Proposed `EFFECT_MASKS` lines (`name: ("Pharah", <Ability>, <12-hex id>, frames)`):
```
"ph_streak": ("Pharah", "RocketLauncher", "00000000098C", 1),       # blast / mini-rocket streak flash (stretched x7.5)
"ph_flare": ("Pharah", "RocketLauncher", "0000000009EE", 1),        # 1-1.9 m flares (rocket, Concussive halo, hover / jump flash)
"ph_flame_noise": ("Pharah", "RocketLauncher", "0000000299E6", 1),  # launch / exhaust flame (with mercy_web 1CB5D, ana_flame#4 7ADD)
"ph_flare_line": ("Pharah", "RocketLauncher", "0000000022D0", 1),   # the blast's flare
"ph_glow_b": ("Pharah", "RocketLauncher", "00000002A3E1", 1),     # the rocket's 1 m glow
"ph_jet": ("Pharah", "HoverJets", "000000005233", 1),               # the hover jet cone
"ph_jet_ring": ("Pharah", "JumpJet", "000000003AE1", 14),           # Jump Jet / Jet Dash ring (on mesh 1E8E)
"ph_wave_band": ("Pharah", "ConcussiveBlast", "00000002CC98", 1),  # Concussive's ground wave band (on mesh 96C1)
"ph_swirl_noise": ("Pharah", "ConcussiveBlast", "000000038E4A", 1), # Concussive's swirl (on mesh ED1C)
"ph_pod_flash": ("Pharah", "Barrage", "000000005E9C", 3),
"ph_pod_glow": ("Pharah", "Barrage", "000000027BFD", 1),            # on quad DFEC
```
Reused (same ids already in `EFFECT_MASKS`): `dva_missile_fire#12` 2291, `sombra_impact_burst` 220A, `ana_flame#4` 7ADD, `mercy_web`
1CB5D, `lw_swirl` 30E12, `torb_glare` 98D, `spark#4` 1F35, `smoke#48` 377D5, `cat_soft` 4C09, `torb_trail` 2A480, `ana_fade_v` 2A063,
`cat_noise` 299E1, `cat_ring_line` 299E3, `mercy_glow` 242, `lucio_soft` 4F3, `lucio_spark` 29F7, `mercy_noise` 4DB, `sombra_smoke`
3E28, `mercy_soft` 139C, `cat_slash` 37021, `mercy_noise_b` 4A7E, `juno_hit_ring#4` 1CB5E, `glow` A7B6, `dva_solid` 1298,
`ana_tube_line` 1EF26, `cat_cone_line` 16E9D, `cat_fade` 7A9, `juno_ring_noise` 1038, `noise` 299E8, `juno_cloud_noise` 299E9,
`brigitte_smoke#32` 1DFCB, `torb_drip#4` 1F5B / `torb_drip_b#4` 1F5C, `cat_glow_noise` 19167, `cat_cloud` 18E48, `lucio_glow` 210FD,
`illari_shard#4` EA42 (when Illari's lands), and Junkrat's `jr_smoke_ribbon` 9ED (the debris trails 0554).
Not needed: colour ramps 3200, 31C0, 22A8, 5DA3, 20D0, 22ED, 6B35, 52BC (their colours are in the palettes); normals / distortion
1BE0C, 1FC65, B08, C4A, 18F81; 1DFCA; the Stadium-only 3E7BD, EC44, F2F1, EE4A, 7623; the fall-damage 633 / 634 / 709 / 713 / 81BA.

Proposed `EFFECT_MODELS` lines (`name: ("Pharah", <Ability>, <12-hex id>)`):
```
"ph_conc_swirl": ("Pharah", "ConcussiveBlast", "00000000ED1C"),  # Concussive's swirl, 0.35 x 0.44 x 0.58 (size 27)
"ph_disc": ("Pharah", "ConcussiveBlast", "0000000096C1"),        # flat 1.28 m disc (Concussive's ground wave, size 10)
"ph_jet_disc": ("Pharah", "JumpJet", "000000001E8E"),            # flat 1.28 m disc (the jumps' ring; same bounds as 96C1, other UVs)
"ph_sphere": ("Pharah", "ConcussiveBlast", "00000000085B"),      # unit sphere (Concussive's 12 m shell; Junkrat's jr_sphere is the same id: one name)
```
Reused models: `torb_hemisphere` 963D (Concussive's domes), `cat_sphere` 1385, `cat_bubble` DBF. Not needed: quads 801 / F5B / 9B1 /
F65D / DFEC, ribbon meshes 7F5 / 31AF / 75D; 958 / EBEF / 327C / 3460 / 8C7 (Stadium 6056, 0185's 1902, fall damage).

Proposed palettes (`PharahPalettes`, stops t, r, g, b; from `fx_junkrat_pal.py pharah`: black at 0, HDR rows hue-clamped, a near-white
top dropped so a solid mask ends on the colour, falling rows reversed; "by age" rows are a ribbon's colour over its life, head first,
for `Fx.trail`, not mask palettes):
```
PH_MUZZLE = {0,0,0,0, .25f,1,.597f,.28f, .5f,1,.838f,.502f, .75f,1,.95f,.605f, 1,1,1,.651f};   // 9421 r2 HDR 5 reversed
PH_MUZZLE_ENEMY = {0,0,0,0, .25f,1,.342f,.28f, .5f,1,.567f,.495f, .75f,1,.68f,.603f, 1,1,.73f,.651f};   // 9E23 r2 HDR 5 reversed
PH_MUZZLE_FLAME = {0,0,0,0, .25f,.848f,.431f,0, .5f,1,.66f,.098f, .75f,1,.82f,.323f, 1,1,.898f,.628f};   // 0096 r4 HDR 2
PH_MUZZLE_FLAME_ENEMY = {0,0,0,0, .25f,.848f,.043f,0, .5f,1,.219f,.098f, .75f,1,.46f,.324f, 1,1,.68f,.631f};   // 8F66 r4 shift HDR 2
PH_MUZZLE_GLARE = {0,0,0,0, .25f,.761f,.096f,0, .5f,1,.503f,0, .75f,1,.773f,.171f, 1,1,.898f,.628f};   // 170A r0 shift HDR 2
PH_MUZZLE_GLARE_ENEMY = {0,0,0,0, .25f,.761f,.096f,0, .5f,1,.147f,0, .75f,1,.325f,.171f, 1,1,.694f,.631f};   // 5A3C r0 shift HDR 2
PH_MUZZLE_SWIRL = {0,0,0,0, .25f,.761f,.096f,0, .5f,1,.503f,0, .75f,1,.773f,.171f, 1,1,.898f,.628f};   // B8D9 r3 HDR 2
PH_MUZZLE_SWIRL_ENEMY = {0,0,0,0, .25f,.761f,.022f,0, .5f,1,.02f,0, .75f,1,.245f,.171f, 1,1,.723f,.631f};   // B891 r3 HDR 2
PH_MUZZLE_SPARK = {0,0,0,0, .33f,.377f,.222f,0, .67f,.753f,.445f,0, 1,1,.705f,.183f};   // 64DC r0 shift top dropped
PH_MUZZLE_SPARK_ENEMY = {0,0,0,0, .33f,.377f,.019f,0, .67f,.753f,.039f,0, 1,1,.244f,.183f};   // 409F r0 shift top dropped
PH_ROCKET_GLOW = {0,0,0,0, .33f,.777f,.21f,0, .67f,1,.381f,0, 1,1,.9f,.353f};   // 33D6 r1 HDR 2 top dropped
PH_ROCKET_FLARE = {0,0,0,0, .25f,.537f,.089f,0, .5f,1,.171f,0, .75f,1,.174f,0, 1,1,.624f,0};   // 10A7 r1 HDR 2.02
PH_ROCKET_FLAME = {0,0,0,0, .25f,.422f,0,0, .5f,.764f,0,0, .75f,1,0,0, 1,1,.527f,.042f};   // 1E25 r5 HDR 1.47
PH_ROCKET_FLAME_B = {0,0,0,0, .25f,.969f,.341f,0, .5f,1,.501f,0, .75f,1,.776f,.173f, 1,1,.898f,.628f};   // D8BD r5 HDR 2
PH_ROCKET_GLOW_B = {0,0,0,0, .25f,.351f,.095f,0, .5f,.701f,.19f,0, .75f,1,.354f,0, 1,1,.96f,.757f};   // 9489 r1
PH_ROCKET_TRAIL = {0,1,.991f,.005f, .25f,1,.482f,.149f, .5f,.174f,.174f,.174f, .75f,.083f,.083f,.083f, 1,.068f,.068f,.068f};   // 749A r5 by age HDR 6
PH_ROCKET_TRAIL_ENEMY = {0,1,.353f,.11f, .25f,.767f,.311f,.311f, .5f,.209f,.034f,.034f, .75f,.209f,.034f,.034f, 1,.209f,.034f,.034f};   // B09C r5 by age HDR 6
PH_ROCKET_STREAK = {0,.93f,1,0, .25f,.638f,.542f,.451f, .5f,.359f,.359f,.359f, .75f,.217f,.217f,.217f, 1,.072f,.072f,.072f};   // 9422 r7 by age HDR 33
PH_BLAST_FIRE = {0,0,0,0, .25f,.71f,.192f,0, .5f,.991f,.419f,.045f, .75f,.976f,.714f,.135f, 1,.991f,1,.753f};   // 8911 r1
PH_BLAST_FIRE_ENEMY = {0,0,0,0, .25f,.71f,.089f,0, .5f,.992f,.252f,.045f, .75f,.987f,.479f,.135f, 1,1,.947f,.753f};   // 8911 r2
PH_BLAST_FLASH = {0,0,0,0, .33f,1,.013f,0, .67f,1,.123f,0, 1,1,.467f,0};   // DE90 r1 top dropped
PH_BLAST_FLASH_ENEMY = {0,0,0,0, .33f,1,.013f,0, .67f,1,.123f,0, 1,1,.289f,0};   // DE90 r2 top dropped
PH_BLAST_FLARE = {0,0,0,0, .25f,.646f,.175f,0, .5f,.995f,.352f,.025f, .75f,.982f,.555f,.087f, 1,.964f,1,.223f};   // BAA0 r1
PH_BLAST_FLARE_ENEMY = {0,0,0,0, .25f,.503f,.088f,0, .5f,1,.175f,0, .75f,.992f,.245f,.023f, 1,.982f,.381f,.074f};   // BAA0 r2
PH_BLAST_SPARK = {0,0,0,0, .25f,.798f,.132f,0, .5f,1,.365f,.154f, .75f,1,.603f,.349f, 1,1,.807f,.533f};   // 527D r1
PH_BLAST_SMOKE = {0,0,0,0, .25f,.251f,.171f,.112f, .5f,.502f,.341f,.223f, .75f,.749f,.509f,.333f, 1,1,.679f,.445f};   // E1D0 r3
PH_BLAST_DEBRIS = {0,0,0,0, .25f,.205f,.023f,0, .5f,1,.133f,0, .75f,1,.571f,.025f, 1,.991f,1,.578f};   // tex 22A8
PH_BLAST_SHELL = {0,0,0,0, .25f,.805f,0,0, .5f,.935f,0,0, .75f,.987f,0,0, 1,1,0,0};   // 494B r0
PH_DEBRIS_TRAIL = {0,1,.037f,0, .25f,1,.076f,0, .5f,.189f,.05f,0, .75f,.189f,.05f,0, 1,.189f,.05f,0};   // E8FD r4 by age HDR 5.57
PH_DEBRIS_TRAIL_B = {0,1,.506f,.014f, .25f,1,.199f,.082f, .5f,.05f,.05f,.05f, .75f,.05f,.05f,.05f, 1,.05f,.05f,.05f};   // 2826 r4 by age HDR 4.14
PH_CONC_LAUNCH = {0,0,0,0, .25f,.018f,.141f,1, .5f,.16f,.265f,1, .75f,.3f,.389f,1, 1,.443f,.514f,1};   // 114A r1
PH_CONC_LAUNCH_ENEMY = {0,0,0,0, .25f,.72f,.013f,.009f, .5f,.814f,.115f,.078f, .75f,.906f,.216f,.146f, 1,1,.318f,.216f};   // 114A r2
PH_CONC_LAUNCH_B = {0,0,0,0, .25f,.101f,.213f,1, .5f,.309f,.395f,1, .75f,.513f,.574f,1, 1,.721f,.757f,1};   // 6C6B r3
PH_CONC_LAUNCH_B_ENEMY = {0,0,0,0, .25f,1,0,0, .5f,1,0,0, .75f,1,.225f,.158f, 1,1,.485f,.342f};   // 6C6B r4
PH_CONC_GLOW = {0,0,0,0, .33f,0,.089f,.715f, .67f,.235f,.329f,1, 1,.625f,.67f,1};   // 2C95 r1 HDR 6 top dropped
PH_CONC_GLOW_ENEMY = {0,0,0,0, .25f,.715f,0,0, .5f,1,.05f,0, .75f,1,.353f,.256f, 1,1,.745f,.659f};   // 2C95 r3 HDR 6
PH_CONC_HALO = {0,0,0,0, .25f,.034f,.157f,1, .5f,.226f,.338f,1, .75f,.449f,.548f,1, 1,.776f,.855f,1};   // 762A r1 HDR 6
PH_CONC_HALO_ENEMY = {0,0,0,0, .25f,1,.006f,0, .5f,1,.043f,0, .75f,1,.085f,0, 1,1,.831f,.776f};   // 762A r3 HDR 6
PH_CONC_TRAIL = {0,0,0,0, .25f,.385f,.503f,1, .5f,.424f,.538f,1, .75f,.473f,.583f,1, 1,.503f,.61f,1};   // 40B0 r2 HDR 6 reversed
PH_CONC_TRAIL_ENEMY = {0,0,0,0, .25f,1,.139f,.139f, .5f,1,.188f,.188f, .75f,1,.227f,.227f, 1,1,.271f,.271f};   // 40B0 r3 HDR 2 reversed
PH_CONC_SMOKE_TRAIL = {0,.879f,.939f,1, .25f,.353f,.591f,.859f, .5f,.231f,.353f,.678f, .75f,.12f,.133f,.495f, 1,.195f,.195f,.195f};   // 4018 r8 by age
PH_CONC_SPARK = {0,0,0,0, .25f,1,.319f,.042f, .5f,1,.555f,.054f, .75f,1,.742f,.061f, 1,1,.892f,.067f};   // 30D3 r0 HDR 3.25 reversed
PH_CONC_DOME = {0,0,0,0, .25f,.188f,.215f,1, .5f,.396f,.435f,1, .75f,.565f,.63f,1, 1,.584f,.651f,1};   // 25AF r3
PH_CONC_DOME_ENEMY = {0,0,0,0, .25f,1,.203f,.187f, .5f,1,.419f,.395f, .75f,1,.591f,.565f, 1,1,.61f,.584f};   // 25AF r4
PH_CONC_SWIRL = {0,0,0,0, .25f,.149f,.147f,.377f, .5f,.299f,.295f,.753f, .75f,.416f,.472f,.981f, 1,.491f,.701f,1};   // D704 r5
PH_CONC_SWIRL_ENEMY = {0,0,0,0, .25f,.377f,.162f,.147f, .5f,.753f,.323f,.295f, .75f,.981f,.486f,.413f, 1,1,.658f,.491f};   // D704 r7
PH_CONC_WAVE = {0,0,0,0, .33f,.002f,.197f,1, .67f,.002f,.395f,1, 1,.32f,.695f,1};   // 373E r5 HDR 5 top dropped
PH_CONC_WAVE_ENEMY = {0,0,0,0, .33f,.778f,.017f,.001f, .67f,1,.037f,.002f, 1,1,.334f,.303f};   // 373E r6 HDR 4 top dropped
PH_CONC_SHELL = {0,0,0,0, .25f,0,.005f,.665f, .5f,.096f,.113f,1, .75f,.287f,.322f,1, 1,.637f,.665f,1};   // F87C r3
PH_CONC_SHELL_ENEMY = {0,0,0,0, .25f,.36f,.003f,0, .5f,.541f,.063f,.052f, .75f,.541f,.179f,.155f, 1,.541f,.364f,.345f};   // F87C r4
PH_CONC_CORE = {0,0,0,0, .25f,.154f,.183f,1, .5f,.309f,.337f,1, .75f,.461f,.489f,1, 1,.615f,.643f,1};   // D8A1 r3
PH_CONC_CORE_ENEMY = {0,0,0,0, .25f,1,.227f,.156f, .5f,1,.451f,.309f, .75f,1,.471f,.257f, 1,1,.491f,.205f};   // D8A1 r4
PH_CONC_DOME_B = {0,0,0,0, .25f,0,.005f,.665f, .5f,.096f,.113f,1, .75f,.287f,.322f,1, 1,.637f,.665f,1};   // 284E r4
PH_CONC_DOME_B_ENEMY = {0,0,0,0, .25f,.391f,0,0, .5f,.588f,.06f,.057f, .75f,.588f,.179f,.169f, 1,.588f,.396f,.375f};   // 284E r5
PH_HOVER = {0,0,0,0, .25f,0,.235f,1, .5f,.002f,.345f,1, .75f,.022f,.544f,1, 1,.048f,.667f,1};   // tex 3200 x 0.372,3.94,5.34 HDR 5.34
PH_HOVER_FLAME = {0,0,0,0, .25f,.133f,.262f,1, .5f,.196f,.377f,1, .75f,.494f,.626f,1, 1,.791f,.875f,1};   // 4DC7 r2 HDR 8 reversed
PH_HOVER_FLAME_B = {0,0,0,0, .25f,1,.088f,0, .5f,1,.049f,0, .75f,1,.025f,0, 1,1,.007f,0};   // 5311 r0 HDR 5.86
PH_HOVER_SPARK = {0,0,0,0, .25f,0,.151f,1, .5f,.001f,.222f,1, .75f,.012f,.35f,1, 1,.026f,.429f,1};   // tex 3200 x 0.229,2.92,6.16 HDR 6.16
PH_HOVER_SPARK_B = {0,0,0,0, .25f,1,.056f,.055f, .5f,1,.105f,.034f, .75f,1,.147f,.016f, 1,1,.186f,0};   // C95C r0 HDR 4.8
PH_HOVER_JET = {0,0,0,0, .25f,.035f,.246f,1, .5f,.024f,.316f,1, .75f,.089f,.457f,1, 1,.563f,.864f,1};   // 1281 r2 HDR 6.4 reversed
PH_HOVER_JET_B = {0,0,0,0, .25f,1,.06f,0, .5f,1,.058f,0, .75f,1,.056f,0, 1,1,.055f,0};   // 3924 r0 HDR 4.23
PH_JUMP_FLASH = {0,0,0,0, .25f,1,.398f,0, .5f,1,.629f,0, .75f,1,.811f,0, 1,1,.811f,.206f};   // tex 31C0 x 4.86,3.94,1 HDR 4.86
PH_JUMP_FLAME = {0,0,0,0, .33f,1,.491f,0, .67f,1,.776f,0, 1,1,1,.002f};   // tex 31C0 x 10,10,10 HDR 10 top dropped
PH_JUMP_SMOKE = {0,0,0,0, .25f,.44f,.134f,.134f, .5f,.81f,.268f,.268f, .75f,1,.367f,.367f, 1,1,.389f,.389f};   // B2B1 r1 HDR 1.37
PH_MINI_FLAME = {0,0,0,0, .25f,1,.046f,0, .5f,1,.074f,0, .75f,1,.092f,0, 1,1,.105f,0};   // FE81 r0 HDR 1.93
PH_MINI_GLOW = {0,0,0,0, .25f,.441f,.152f,0, .5f,.667f,.303f,0, .75f,.891f,.453f,0, 1,1,.542f,0};   // 7E28 r0 HDR 1.12
PH_MINI_TRAIL = {0,1,.707f,.054f, .25f,.275f,.275f,.275f, .5f,.215f,.215f,.215f, .75f,.155f,.155f,.155f, 1,.095f,.095f,.095f};   // F758 r3 by age HDR 10.3
PH_MINI_TRAIL_B = {0,.88f,1,0, .25f,1,1,1, .5f,.848f,.848f,.848f, .75f,.569f,.569f,.569f, 1,.286f,.286f,.286f};   // 1604 r3 by age HDR 6
PH_MINI_STREAK = {0,.93f,1,0, .25f,.439f,.343f,.252f, .5f,.117f,.117f,.117f, .75f,.095f,.095f,.095f, 1,.072f,.072f,.072f};   // B270 r7 by age HDR 33
PH_MINI_HIT = {0,0,0,0, .25f,.71f,.192f,0, .5f,.991f,.419f,.045f, .75f,.976f,.714f,.135f, 1,.991f,1,.753f};   // 7A23 r1
PH_MINI_HIT_ENEMY = {0,0,0,0, .25f,.71f,.014f,0, .5f,.992f,.111f,.045f, .75f,.987f,.329f,.135f, 1,.991f,1,.753f};   // F4AA r1
PH_MINI_HIT_SPARK = {0,0,0,0, .25f,1,.429f,.091f, .5f,1,.555f,.112f, .75f,1,.616f,.117f, 1,1,.652f,.121f};   // 67D0 r0 HDR 5.75 reversed
PH_MINI_HIT_SMOKE = {0,0,0,0, .25f,0,0,0, .5f,0,0,0, .75f,.838f,.221f,0, 1,1,.383f,0};   // 1A11 r0 HDR 2.29 reversed
PH_POD = {0,0,0,0, .33f,1,.491f,0, .67f,1,.776f,0, 1,1,1,.002f};   // tex 31C0 x 8,8,8 HDR 8 top dropped
PH_POD_FLASH = {0,0,0,0, .25f,1,.404f,0, .5f,1,.404f,0, .75f,1,.519f,.056f, 1,1,.75f,.167f};   // 7CE2 r2 HDR 12 reversed
PH_POD_FLASH_ENEMY = {0,0,0,0, .25f,1,.025f,0, .5f,1,.025f,0, .75f,1,.036f,.002f, 1,1,.054f,.005f};   // FAB3 r2 HDR 18.69 reversed
PH_POD_GLOW = {0,0,0,0, .25f,.043f,.269f,1, .5f,.048f,.301f,1, .75f,.236f,.487f,1, 1,.342f,.59f,1};   // 809D r6 HDR 14.99
```
