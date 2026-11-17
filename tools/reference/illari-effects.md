# Illari's ability effects in the game data

What Illari's effects are made of in real Overwatch, where the extracted files are, and how each would be rebuilt in Minecraft
(an `IllariFx` beside `TracerFx` / `DvaFx`; today the server sends vanilla particles from `illari/Illari.java`, issues #150 "everything
in illari ult", "illari reload sun ball", and #144). Read 2026-11-17 from the archived offline build 154088. Overwatch and Battle.net
were checked closed (`Get-Process Overwatch, Battle.net`) before each TankLib run, and `owinit.py` refuses on its own; everything was
read only, offline, through TankLib/DataTool. Text only in the repo: masks, ramps and meshes stay in the owner's archive and reach the
game through the local asset pack. Format: [tracer-effects.md](tracer-effects.md)'s. Data map: [illari-map.md](illari-map.md); public
numbers: [illari.md](illari.md); hub: [illari](../../techniques/heroes/illari.md); method: `techniques/rendering/rendering.md` "Effects
and glows".

Confidence: **H** read in the data, **M** inferred, **L** guess. "Size" is the spawn size (a 1 m quad at size 0.5 is 0.5 m; a ribbon's
is its width; a mesh's size scales its own extent). "Ours" marks what no data gives. `XXXX rN` = the colour LUT (last 4 hex digits of
`ramps\8...XXXX.json`) and its row; hex runs are that row at 0 / 25 / 50 / 75 / 100 %. "hp" = hardpoint.

## Files and scripts
- `E:\Overwatch Archive\research\illari\fx\Illari\<Ability>\` (`CaptiveSun`, `Reload`, `SolarRifle`, `HealingBeam`, `HealingPylon`,
  `Outburst`, `Perks`): `textures\` (PNG, flipbook frames `_1.._n`), `ramps\` (float LUTs as JSON), `models\Models\<id>.00C\<id>.owmdl`,
  `manifest.json`. A texture shared by several abilities sits in the first one extracted (mostly `CaptiveSun`).
- `research\illari\work\`: `fx_cards_{sun,reload,rifle,beam,pylon,outburst,perks}.txt` (one card per renderer: what this note is written
  from), `fx_overview.txt`, `fx_summary2.txt|json`, `fx_meshes2.json` (mesh bounds), `fx_entities.txt`, `fx_clip_effects.txt`,
  `fx_effects\` (decoded), `fx_extract_log{,2,3,4}.txt`.
- Scripts `research\illari\scripts\fx\` (Tracer's `fx_tracer_*` copied as `fx_illari_*`; `fx_illari_extract.py` holds her effect list
  with each one's graph node). Run: `fx_illari_extract.py [Ability ...]` with the kiriko-anim venv's python, `-I`; then under
  `py -I run.py`: `fx_illari_summary2.py`, `fx_illari_overview.py`, `fx_illari_card.py <ids>`.
- Counts: **109 effect entries in the manifests, none missing** (the list in `fx_illari_extract.py` plus sub-effects 4010, 4046 and
  4BA3 again under Perks). 48 draw something (renderers, lights, decals); the rest are sounds only or
  empty (4E92, 5261, 72EF, the 3P clip effects 546A / 5410 / 550D / 5685 / 568C / 56A4 / 56B7 / 56B8 / 53FD / 5567 / 522B / 56B6).

## Her look
- **One warm family: gold to white-hot, with a redder twin (M roles).** Most LUTs carry a gold row and a redder row of the same shape
  (74C6 r0 `#794A0F > #E08E24 > #F79D29 > #FEA12B > #FFEEB7` / r1 `#702A1A > #D9593B > #F26641 > #FC732F > #FF7C00`; D1CA r0 `#9C603F >
  #E2974F > #FBD092 > #FFEFCF > #FFFFFF` / r1 `#9C4A3F > #E2744F > #FBAD92 > #FFDBCF > #FFFFFF`; 983E r1 orange / r2 red). As Tracer's
  twins: gold = her side, red = enemies (M). Unlike Tracer there are no blue / red pairs: everyone sees a sun.
- **Hardpoints (H ids, M roles):** **0006** the rifle's muzzle (shot, charge, beam source, the armed sun, Solar Flare); **000B** her hand
  (reload orb, ult cast: Tracer's 000A / 000B are the hands); 0002 (ult cast's big fans, M: chest); **0158 015A 015B 015D 0169 016B 016C
  016E 016F 0171** the rifle's ten charge pips (mesh 13DE2 capsules, two rows of five); 0078 / 007A the rifle's two small glows
  (528E); **0081** the ball (4983); **0001** a creature's body centre (mark, detonation, Sunburn) and hers in flight with **0004**;
  **0448** the pylon's eye (beam source, heal shots); 0076 the pylon's base; 0012 Solar Flare's heal target; 003E the camera (local view).
- **Screen layers:** renderers with `4E65CCF9 = ECIM(40)` are the local player's own screen (Tracer's 6E9F): the flight's 5708 / 86CF
  rims, the mark's 5D0 vignette, Sunburn's rims. Drawn as a screen tint if at all (as `ana_vignette`).

## Captive Sun (graph 3CAB; ball 3CB1 entity 4AC0; mark 3DFF)
| Part | Effect | Rebuilt (replaces) |
|---|---|---|
| Cast (1.0-1.2 s anim) | **4EF3** (3CAB #893, 2.33 s, hp 000B + 0002) H: the sun forming in her hand: spheres DBF **0.08 m** (27EF r5 `#FFF2D1 > ... > #9C5F00`) and 0.15 m shell (8AC7 r9 `#A15D00 > #A5681B > #BE974E > #DEC471 > #FFED8E`) at 000B +0.15 m, from 0; glow 4A58 0.6 m 1 s (DF27 r1 `#000000 > #8F5F2A > #C69A3B > #E8D297 > #FFFDEC`); **fan mesh 13561** (0.18 x 1.23 x 1.38: sun rays) **3.5 m** 0.5 s at 0.2 s and **5 m** 0.75 s at 0002 (0DA4 r3 / r4 brown to cream, HDR 20); glow 4A58 **3.5 m** 1.1 s at 0.47 s; A17F / 74DEA flashes **1.3-1.5 m** at 0.47 / 0.5 s (D1CA r0 / 2666 r0); 3 hemispheres EC38 0.75 m at 0.27 s; ring 17E1 0.85-1 m 0.65 s at 0.37 s (F8F2 r0 / r1); A17F sparks 50 > 400 /s 0-0.17 s; distortion 18F81 1.5 m; **light 7 m** 0.17-1 s; at 1.17 s a 3203 ring 0.35 m and 74DEA 0.35-0.4 m at 000B. 1P clip **5140** (D592): the rifle's ten pips lit one by one 0.7-0.83 s (13DE2, 4388 r6 `#A15D00 > ... > #FFED8E`), sparks 200-225 /s at the muzzle | at her hand while `castSun` runs (line 847, the `sun.cast` sound): orb 0.08 / 0.15 m, the 3.5 / 5 m fans as `Fx.mesh` (or two crossed 74DEA stars, ours), the 3.5 m glow at 0.47 s, the ring, sparks, a light |
| Flight (6 s, 4E23) | **4E23** (3CAB #901, hp 0001 / 0004) H: **A7B6 glow 30 m** x3 living 7 s (BF0E r1 `#000000 > #834C08 > #AB7203 > #D49B2F > #FFC657`, very low alpha M: the sky light around her); 299E1 8 m x3 (7D6F r3); a 1 m flash at 0004 at 0.1 s (tint 15, 7.1, 1.6); A216 ring 0.15 m; **sparks 1F35 48 /s from 0.5 s**, 1.5-1.75 m/s, 0.1-0.13 m, 0.5-0.75 s (983E r1); 32 /s small ones 0.03-0.06 m; a burst of 175 + 100 /s A7B6 motes 4-4.2 m/s (13F5 r1 `#EED7A1 > #FFFFFF`); 13F75 smoke x10 at +0.5 m; 6389 shell 2-3 m 1.5 /s; 13AD1 flat ring 2 m; 14F9 flame x5 0.2-0.4 m; E99F speed lines size 10, 50 /s; 17D8 5 m 2 /s; screen rims 5708 / 86CF (ECIM 40, her view only); lights 5 m x2. **4FED** (#909, 30 s): three small lights only (her hand / chest, 0.5 / 0.02-0.05) | around her while `IllariState` flies (`sun.flight`, line 862): a 4 m soft glow (the 30 m one at 0.05 alpha, ours), sparks 48 /s, a light; the speed lines and rims only in her own view (ours: skip in 3P) |
| Ball ready at the rifle | **4EF2** (3CAB #894 "armed", 4 s, hp 0006 +0.2 m) H: **the sun held at the muzzle**: halo 36D6D **1.0 m** and 0.2 m flickering (rate 0 > 4, life 0.2 s; F4D7 r1 `#000000 > #3F1904 > #582708 > #6A2F0B > #FFF7BC`, HDR 6); spheres DBF 0.055 / 0.18 m and 0.05 / 0.17 m shells (27EF r5, 8AC7 r9); glows 4A58 **0.85 m** and 1.0 m (behind, -0.19 m), 568C 0.37 m, 1603 1.25 m, all held; 2F4F0 star 0.2 m 30-40 /s; **1A57 flare 10 m** 3-7 /s 0.25-0.75 s (8EBF r1 `#000000 > #795022 > #A76F32 > #DEA844 > #FFEFBD`); 2D6CA embers 64 /s 0.015-0.02 m (4516 r1 `#E30000 > #EB7000 > #F2A800 > #F8DC49 > #FFFFFF`); 656E ring 0.15 m 3 /s; 6389 shell 0.07-0.1 m; 13AD1 2 m ring x3; the ten pips lit (764C r6, tints (2, 2.5, 0) / (1, 2, 2.5)); light 1.5 m | at her muzzle while PRIMARY can throw (`sun.armed`, line 865): orb 0.18 m, halo 1 m, glow 0.85 m, the flare at 0.3 alpha (ours: 10 m is a lens flare), embers; refreshed each second |
| Throw | **4F06** (3CAB #898, 1 s, hp 0006) H: streaks 1D047 25-50 at **4-5 m/s** + 15-35 at **7-8 m/s**, 0.1-0.75 m, cone 13-18 deg (B8D4 r1 `#FF8500 > #FF9E44 > #FFBC5C > #FFD86D > #FFF27B`, HDR 3.7); flame cones 14F9 (2 frames) 0.5-0.75 m and **5 m** at 1-1.75 m/s (511F r1 `#FF8500 > ... > #FFFACD`); **rings 1E7F** 0.2-0.28 m at +0.55 / 0.75 / 1.0 / 1.5 m, 0.35-0.55 s (CB8E r3 / r4 orange); 3FAA8 flames 0.25-1.15 m at 5 m/s; flashes 299E8 **1 m** and **3 m** 0.05-0.16 s (FE12 r4 `#9C4A00 > ... > #FFF3A6`); lights 0.17 s | at the muzzle in `fireBall` (line 876, `sun.fire`): the two flashes, 4 rings along the aim, 20 streaks, the flame cone |
| The ball | **4983** (ball 4AC0 child 4D41, 3 s looping, hp 0081) H: model 10BAE (in the pack: `projectile_illari_sun.glb`) plus spheres DBF **0.55** / **0.5 m** (8CD6 r6 / FB3A r9), disc mesh **3FAE 2.9 m** 20 /s 0.15 s (CB5B r3 `#000000 > #593A19 > #7B5226 > #A87237 > #FFFEEF`), halo 36D6D **4.2 m** flicker (F4D7 r1), ring 656E **4.0 m** 3 /s 0.5 s (B254 r4), **glow 4A58 8.5 m** held (1777 r1), F767 1.5 m, 47D9 rings 0.5-0.75 m 5 /s (BF87 r5 `#9C8552 > ... > #FFEEB6`), embers 2D6CA 32 /s 1.5-2.5 m/s 0.04-0.1 m, 2B441 5.25 m 3-7 /s, 1603 4 m | on the ball entity (client, each frame): glow 8.5 m at 0.3 alpha (ours), halo 4.2 m, two disc meshes (or flat rings, ours), embers 32 /s; replaces nothing (the ball has no particles today) |
| Burst | **4E27** (3CB1 #632, **scale 1.25**, 4 s, hp 00A4) H: shock disc **85CD 16 m** 1.5-1.9 s (7CCA r4 / r5); flash 17D8 **16-21 m** 0.5-0.75 s (F2B1 r0 red); disc **5E5E 11 m** with 31951 rays 0.25-0.5 s (909E r3 `#190B03 > #553A1B > #876E36 > #FFF5D7`); flames 3FAA8 x6 0.3-5 m; star 55BA5 **2.5 m** (20DE r0 / r1); **sparks 3429 75-175 at 45-65 m/s**, 0.02-0.05 m, stretch 7-9; **60 embers 210FD rising**, 2-4 s, 0.35-0.45 m (3522 r1 `#7C4404 > #A97A4E > #CB9D6D > #E7C99B > #FFEBBC`); smoke 2606 4 m 2 /s 1.5-2.5 s; A7B6 3 m; distortion 18F81 2.25 m; decals **44656 2 m** (CF48 r1) and **19008 6 m** scorch 3.9 s (5F07 r2 dark), A7B6 **16 m** light decal 0.67 s; light 10 > 2 over 2.5 s. **514E** (#662, 6.7 s, the hit point): 1F85 ray walls 0.65 m 3 /s (5566 r3), two 1 m decals, lights 2 m | at `ballBurst` (line 954-955: `EXPLOSION_EMITTER` + 60 `FLAME`): all but the distortion; the scale 1.25 fits the 10 m splash (BZ) over the data's 8 m (M) |
| Mark (6 s BZ, 7 s data) | **4B50** (3DFF #439, 10 s, hp 0001) H, sizes driven by dataflow **5FD** (M: the stored damage toward 90): spheres DBF **0.2 > 0.3 m** (5D25 r6, 171E r9 `#A15D00 > #A5681B > #BE974E > #DEC471 > #FFED8E`), 0.245 > 0.354 m (CE6E r7); **3F2CB eight-point sun 1.5 > 4 m** 1.25 s at 0.03 s (E1EA); ring 17E1 1.5 > 4 m 0.75 s (03B8 r0 / r1); glow 242 **1.5 > 2 m** x3 held (39DE r1); DC86 disc 0.09 > 0.29 m; EC38 dome 0.09 > 1.09 m; embers 1F35 64 /s 0.35-0.55 m/s 0.012-0.02 m; light 3 m 3 s; on the marked player's own screen: 5D0 vignette 2 m 4 s and rims (ECIM 40) | replaces the `FLAME` above the marked creature every 4 ticks (line 742) and adds the 4 m sun stamp at `mark` (line 977): a 0.25 m orb + 1.75 m glow at its centre growing with its stored damage (ours: 0 > 90 maps to 0 > 1), embers |
| Detonation | **4E16** (3DFF #535, 2.93 s, hp 0001: the 0.8 s fuse) H: spheres DBF 0.07 > 0.35 m x3 0.85 s; glow 4A58 **0.35 > 1.15 m**; fans 13561 **2.5 > 4.75 m** 10 /s for 0.17 s; EC38 domes 2 > 3 m; embers 1F35 64 /s from 0.8 s at 3-4 m/s; 5D0 vignette 0.9 s (ECIM 40); lights. **4E17** (#536, 1.17 s, -0.15 m) H: the bang: sphere 85B 0.7-0.85 m (ACB8 r4), flare 1449F **2.35 m** and star 3A4F3 **2.25 m** 0.15-0.35 s (B352 / CCA5 r0); 17D8 4.5 m; flames 3FAA8 x15 0.3-2.75 m at 1.5 m/s; **sparks 1F35 175-275 at 35-50 m/s**, 0.02-0.03 m, stretch 8-10 (7538 r1 `#BC6234 > ... > #FFDA94`); embers 50-65 rising 2-3 s; EC38 4 m x3; A7B6 2.9 m; **shock disc 85CD 12 m** 0.5-0.75 s (332D r4); light 0.53 s. **4EC3** (#545, local view hp 003E): 15555 ring, sparks EA42 (4 frames) 15-35 at 4-7 m/s | `sunstruck` (lines 805-806: `EXPLOSION_EMITTER` + 30 `FLAME`) plays 4E17 at the target; 4E16 starts when the threshold is crossed (the 0.8 s wait, `m.goesAt`) |

## Reload (3A65 #2787 and #3083; 1.5 s, ammo back 1.08 s)
| Part | Effect | Rebuilt |
|---|---|---|
| The sun in her hand | **4F07** (1 s, hp 000B, offset (-0.03, 0.02, 0.03)) H: **orb**: spheres DBF **0.023 m** at 0.4 s (0.25 s) and **0.087 m** at 0.4 s (0.75 s; CEA1 r9 `#FFF4C7 > #E1CAAC > #BC946F > #9C6F41 > #704801`, HDR 5; 876B r0 white rim); swirl 2585 0.04-0.06 m and 0.15-0.17 m 0.4-0.8 s (5EA2 r2 `#C1C175 > ... > #FFFFDD`); glows A17F 0.07 m (from 0.37 s) and **0.21 m** (0.37-0.6 s; 9870 r1 `#000000 > #A57130 > #D09940 > #EBC17F > #FFF7AD`); rings 17E1 **0.1-0.2 m** 0.5 s at 0.13 s (74C6 r0 / r1); stars 74DEA 0.05-0.07 m at 0.4 s and **0.18-0.2 m** at 0.7 s; sparks A17F 50 > 400 /s 0.13-0.3 s; at **0.7 s** a flash: 10F4E quads DA5B 0.25 m, A7B6 0.15 m, 17D8 0.25 m (0.3-0.4 s) - the orb going into the rifle (M); light. 1P clip **5628** (DD5F): the ten pips relit at **1.0 s** (13DE2, 7234 r6 `#A15D00 > ... > #FF9671`), a flash 210FD 120-150 sparks at the muzzle at 0.77 s (0.25-0.4 m/s, stretch 9-13), E3E cones 0.03 / 0.035 m, 74DEA / DA5B 0.1-0.15 m, two 2B46A glints 0.2 m at 0.83 s | at her hand (3P) or rifle-side point (1P, ours: the muzzle's left, as `muzzle`) from `startReload` (line 218, no particle today): orb 0.09 m from 0.4 s, glow 0.21 m, ring, the 0.7 s flash, then a muzzle flash at 0.77 s |

## Solar Rifle (3A65; hitscan 4828; rifle child 53DA)
| Part | Effect | Rebuilt |
|---|---|---|
| Charging | **4F08** (3A65 #3104 past 10 %, 4 s, hp 0006 +0.03 m) H: pips lit at 0.33 s; sphere 13DE0 0.32-0.34 m held (D904 r3 `#A55B14 > ... > #FFCD6E`); EC38 0.075 m; 2866 cubes 0.05 / 0.075 m; flames 3FAA8 0.025-0.064 m 13-20 /s; glows 242 **0.35 m** 16-19 /s 0.5 s (5FAC r1 `#9B603E > #E09255 > #FAA956 > #FFC15F > #FEDEB2`) and 210FD 0.13-0.17 m 8.9 /s; 85B 0.05 m; motes A17F 150 > 25 /s; light 0.35 m. **521F** (#3096, full): flashes 5233 0.15-0.35 m (8C0) and 23C0 0.03-0.13 m, 0.17-0.25 s (5223 r0 `#9A4915 > ... > #FFF1DF`, HDR 10) | the muzzle glow 0.35 m growing with charge, a flash at full; local-only cosmetic (no vanilla call today) |
| Shot (muzzle) | **4BA3** (4828 m_A18274A3, 1 s, hp 0006) H: stars 2A2B9 **0.2 / 0.5 m** at +0.5 m 0.1-0.18 s (FC1E r0 `#7E2D00 > #BC4700 > #E75900 > #F9A200 > #FFF8CE`); 3FAA8 0.25-0.45 m; 2F4F0 0.05-0.15 m; flame cones 14F9 0.5-0.75 m and 0.8-1.13 m, 15-30 deg; streaks 1D047 5 at 3-4 m/s and 5-15 at 4-5 m/s; three lights 0.17 s | at `muzzle(player)` in `shot` (line 270): one star, the cone, a few streaks |
| The line | **48BE** (4828 STUEffectComponent): three quad renderers 801 with **43321** (tapered 128 x 512 streak) and **4174D**, each a band from muzzle to hit (ECIN, M), **0.25 s**, width fields 0.83 / 0.5 / 1.25 (L: x the default width); 2B74 r2 `#2D0C00 > #803D19 > #AB5526 > #E2763A > #FFEFC7`, 8FC1 r2 HDR 6.8 | replaces the `SHOT_DUST` trail (line 270): `SombraFx.tracer`'s band style with these masks, 0.25 s, ~0.05 m (ours), gold |
| Hit | **4BA1** (m_AED73588 and m_BF7914D4: creature and world share it) H: flash 299E8 **1 m** 0.2-0.35 s (FE12 r4); A7B6 0.45 m 0.35 s (8CB5 r0 `#7E2D00 > #C67300 > #E8A805 > #F8D695 > #FFF8CE`); 258D 0.02-0.09 m; sparks 8F2 12-18 at 0.75-1 m/s, stretch 5-7; 1CAFA 33-35 0.15-0.3 m 0.15-0.35 s; two lights 0.13 s | replaces the `CRIT` (line 271) |
| The rifle itself | **528E** (53DA, hp 0078 / 007A): two 0.065 m glows 7.7 /s and two 0.06 m spheres (0974 r9) | not drawn (the model's glow map covers it) |

## Healing beam (3A65 #1797 / #2641; 20 m)
| Part | Effect | Rebuilt |
|---|---|---|
| The beam | **4C29** (STU_9932D728, 2 s, held): **13 ribbons** from muzzle to target, widths **0.5, 0.38, 0.35, 0.3 (x3), 0.25, 0.2, 0.18, 0.15 (x2), 0.1, 0.05 m** on masks 20427 / 1EF26 / 3FE0D / 1344 / 43BE2 / 3E45B / 299E8; rows 0FC2 r9 `#FFF5C4 > #FBE29E > #E8CE8D > #C7AE74 > #9C8353`, r10 `#B4340A > #BB5424 > #C66F36 > #D7904F > #FFD88E`, 1E00 r6 `#FFF1B0 > ... > #9C6F18`, F4EF r4 `#A56C1A > ... > #FFE4B0` | replaces `BEAM_DUST` (line 313): three `Fx` bands (0.5 m soft 3FE0D, 0.3 m 20427 core, 0.1 m 1344 hot line, ours: three of 13) in IL_BEAM |
| At the muzzle | **4C7F** (hp 0006): hemispheres 99F / 7F5 0.6 / 0.7 / 0.35 m (5D83 r5 `#FFF1B0 > #FAD76D > #EDC154 > #CA9F3E > #9C6F18`); 210FD motes 25-50 /s 0.15-0.22 m; smoke 1DFCB 0.9-1.15 m 6 /s; a 13F85 tube; lights 3 / 2 m | a 0.6 m glow at the muzzle, motes |
| On the ally | **495B** (3.3 s): CC29 lens 0.15-0.2 m; EC38 0.25-0.5 m 8 /s; sparks 1F35 90 /s 0.5-0.75 m/s 0.2-0.3 m (7592 r1 `#E30000 > #EBA100 > #F2CC00 > #F8E26A > #FFFFFF`); smoke 1DFCB 0.4 m; 3A4F8 glints 0.1-0.12 m 10 /s; light 2 m | a soft gold glow and sparks on the healed |
| On the world | **4BA5**: A7B6 0.1-0.15 m 7-12 /s (082F r1) | a small glow where the beam ends |

## Healing Pylon (3DF4 / 3DF5 / 3DF6; pylon model 134D8 in the pack)
| Part | Effect | Rebuilt |
|---|---|---|
| Thrown | **5215** (4B4A child 5384, hp 0004): a ribbon 18E48 **0.65 m** wide, 30 points/s living 0.49 s (593F r4 `#381F00 > #634E00 > #725F00 > #D1BB09 > #FFF3A5`) | `Fx.trail` on the flying pylon |
| Building | **519B** (#857, 2 s, hp 0448): cylinders DE0 1.0 m (1.2 s) and 0.75 m (6DEE r4 `#000000 > #A4831A > #D4AC35 > #F3C74B > #FFE78E`, HDR 11); fan 13561 0.3-1 m; EC38 0.75 m x3; 4A58 1 m | at `pylonLanded` (line 453) |
| Standing | **4A53** (#255, 40 s, hp 0448 / 0076): spheres DBF 0.3 / 0.2 m (A185 r9 `#9C7B00 > ... > #FFE7AD`); glows 568C 0.5 m, 18E48 0.5-2.5 m 5 /s, A7B6 **2.35 m** 3 /s (8403 r1); 42CE5 rays 0.25-0.35 m 5-16 /s; DE0 rings 0.25 m at the base 10 /s and 1 /s; 2B441 1-4 m 2 /s; two 0.6 m light decals; lights 0.6 / 0.75 m | on the pylon entity (client): 0.5 m eye glow, the 2.35 m pulse, rays |
| Heal shot | **4D80** fired (sound), **4F66** (shot, hp 0448): ring 17E1 **2.25-2.55 m** 0.41 s (EE10 r0 `#724C0D > #E09D34 > #F6C274 > #FDDB9B > #FFEEB7`); **4C9A** (the shot, 2 s): sphere 1385 0.5 m, cones E3E 1.5 m (82C7 r4 / F0CB r4); **4E01** (3D81 #406, arrival, hp 0001): rings 17E1 0.35-0.5 m, EC38 **2.25-3.25 m**, 2A81E 0.2-0.3 m, sparks 3271 / 3F7D2 15-35 at 2-6.5 m/s, lights 2 > 4 m | replaces `BEAM_DUST` trail (line 523) with a moving orb (1385 0.5 m) and `HAPPY_VILLAGER` (line 536) with the 4E01 burst |
| Beam to target | **4C01** (3DF5 #787 / #882): ribbons 37041 **0.3 m** (81E7 r4 / r5) and 0.146 m (1355 r4 `#BC8900 > #B29122 > #C0AA41 > #D6C679 > #FFF4CD`) | while it heals: a 0.3 m band eye to target (ours: only during the shot's flight) |
| Range ring | **4CB7** (#804): EC38 dome **4 m** x11 0.57 s (CF90 r4) | not drawn (L: a placement preview) |
| Destroyed / removed | **51E9** (#860): sphere 1385 0.3-1.5 m, streaks 1CAFA x5 stretch 20, A7B6 **4 m** 0.38 s, light 1 > 5 m; **51EB** (#859): DBF 0.2 m, 2006C lens 0.1-1 m, 7AB 1.5 m, 2007A **4 m**, 1CAFA x3 | replace `EXPLOSION` (line 550) and `CRIT` on a hit (line 542: ours, the 51E9 sphere at 0.3 m) |

## Outburst (3B38) and the perks
| Part | Effect | Rebuilt |
|---|---|---|
| Burst | **479A** (3B38 #712, +1 m, 0.3 s) H: 4F3 1 m (D6D1 r0 `#805310 > #F09F28 > #FEAD39 > #FEC15E > #FCF0C8`); **rings EBEF 2.5 m** and A216 1.35 m at -1 m, EBEF 1.5 m at -1.25 m (8D2C r6 `#DD3D3D > #E96D41 > #F48C44 > #F9B15D > #FFFFFF`); sphere 1385 **8 m** 0.75 s (4E8A r6) and **7 m** 0.35 s; 1CB5D web sphere **16 m** 0.75 s (EC67 r1 `#FF5A00 > ... > #FFFB8B`); sparks 1F35 35-75 at 16-30 m/s, 3271 50-75 at 35-65 m/s, 8F2 100-175 at 20-50 m/s; 86CF screen rim; lights 6 > 13 m | replaces `FLAME` x30 + `BEAM_DUST` x30 (lines 635-636): the ground rings, an 8 m shell (`cat_sphere`) at 0.25 alpha (ours), sparks |
| Sunburn | **6578** (cast, #960): 17D8 4.5 m, 3FAA8 flames x15, 1F35 175-275 at 35-50 m/s, 2E8AF 15 m; **254F** (burn on the target, 10 s, hp 0001): sparks 1F35 4 x 20 /s 0.025-0.05 m; flames 4A7E / 4C09 / 4DB 0.5-0.75 m 7-10 /s; the target's screen rims | replaces `SMALL_FLAME` (line 698) |
| Solar Flare | **6CF2** (#481, heal, hp 0012) and **6D59** (#498, hp 0006): F71 cone 0.1 m, 18982 cones 0.35-0.6 m, 210FD streaks 13-19 at 15-50 m/s, 8C0 3 m flashes, smoke 1DFCB | replaces `END_ROD` (line 407) |

## Not found / not done
- What the dataflow inputs scale (5FD on the mark and detonation: M the stored damage; the burst's 1.25 scale node); the ECIN band
  widths of 48BE (L). Hardpoint 0002 and 000B's bones were not resolved against her skeleton (M from Tracer's).
- Material effects (Sunburn 27EF, pylon outline 032C), the distortion layers (18F81) and the screen-only layers: not drawable as particles.
- Which side sees the redder twin rows (M, no team-coded pair as Tracer's blue / red).
- 3P clip effects (ult_launch D48B, flight, ball_fire) are empty in the data: only the 1P clips (5140, 5628, 56AC) carry effects.

## For the asset pack (sources: add `E:\Overwatch Archive\research\illari\fx`)
Proposed `EFFECT_MASKS` lines (`name: ("Illari", <Ability>, <12-hex id>, frames)`):
```
"illari_star": ("Illari", "CaptiveSun", "000000074DEA", 1),         # 6-ray star (reload, cast, armed)
"illari_halo": ("Illari", "CaptiveSun", "000000036D6D", 1),         # the ball's / armed sun's flickering halo
"illari_band": ("Illari", "CaptiveSun", "0000000052F4", 1),         # soft band on the ball's discs 3FAE / 656E
"illari_burst_decal": ("Illari", "CaptiveSun", "000000044656", 1),  # burst ground mark 2 m
"illari_scorch": ("Illari", "CaptiveSun", "000000019008", 1),       # burst scorch 6 m
"illari_burst_star": ("Illari", "CaptiveSun", "000000055BA5", 1),   # burst star 2.5 m
"illari_sun_mark": ("Illari", "CaptiveSun", "00000003F2CB", 1),     # Sunstruck's eight-point sun
"illari_flare_line": ("Illari", "CaptiveSun", "00000001449F", 1),   # detonation flare 2.35 m
"illari_star_small": ("Illari", "CaptiveSun", "00000003A4F3", 1),   # detonation star 2.25 m
"illari_lens": ("Illari", "CaptiveSun", "000000001A57", 1),         # armed sun's 10 m flare
"illari_ember": ("Illari", "CaptiveSun", "00000002D6CA", 4),        # embers (ball, armed)
"illari_shard": ("Illari", "CaptiveSun", "00000000EA42", 4),        # local-view sparks
"illari_ring_thin": ("Illari", "CaptiveSun", "000000015555", 1),
"illari_flame": ("Illari", "CaptiveSun", "00000003FAA8", 1),        # flame tongue (rifle, burst, Outburst)
"illari_ring_dot": ("Illari", "CaptiveSun", "00000002B441", 1),
"illari_smoke": ("Illari", "CaptiveSun", "000000013F75", 2),
"illari_swirl": ("Illari", "CaptiveSun", "000000002585", 1),        # reload orb swirl
"illari_shot_line": ("Illari", "SolarRifle", "000000043321", 1),    # the shot's band
"illari_shot_edge": ("Illari", "SolarRifle", "00000004174D", 1),
"illari_muzzle_star": ("Illari", "Perks", "00000002A2B9", 1),
"illari_charge_flash": ("Illari", "SolarRifle", "000000005233", 1),
"illari_hit_glow": ("Illari", "SolarRifle", "00000000258D", 1),
"illari_hit_streak": ("Illari", "HealingPylon", "00000001CAFA", 1),
"illari_spark": ("Illari", "Outburst", "0000000008F2", 1),
"illari_beam_wave": ("Illari", "HealingBeam", "00000003FE0D", 1),   # beam's soft outer band
"illari_beam_noise": ("Illari", "HealingBeam", "000000043BE2", 1),
"illari_pylon_beam": ("Illari", "HealingPylon", "000000037041", 1),
"illari_pylon_rays": ("Illari", "HealingPylon", "000000042CE5", 1),
"illari_heal_spark": ("Illari", "HealingPylon", "000000003271", 4),
```
Reused (same ids already in the pack): `cat_glow_dot` A17F, `sombra_ring_burst` 17E1, `sojourn_dot` 4A58, `juno_soft_dot` 568C,
`juno_jet_glow` 1603, `glow` A7B6, `lucio_glow` 210FD, `lw_prism` 17D8, `mercy_glow` 242, `cat_flare` DA5B, `cat_ring` 3203,
`tracer_glow` F767, `spark#4` 1F35, `lw_thorn_flame#2` 14F9, `cat_purr_streak` 1D047, `brigitte_flare_star` 2F4F0, `noise` 299E8,
`cat_noise` 299E1, `cat_cloud` 18E48, `dva_matrix_edge` 20427, `ana_tube_line` 1EF26, `cat_strip` 1344, `mercy_lens` CC29,
`brigitte_smoke#32` 1DFCB, `lucio_soft` 4F3, `mercy_web` 1CB5D, `juno_ray_column` 31951, `sigma_rays` 7AB, `sigma_lens_line` 2006C,
`ana_vignette` 5D0.

Proposed `EFFECT_MODELS` lines (`name: ("Illari", <Ability>, <12-hex id>)`):
```
"illari_rays": ("Illari", "CaptiveSun", "000000013561"),       # sun-ray fan 0.18 x 1.23 x 1.38 (cast 3.5 / 5 m, detonation, pylon build)
"illari_sun_disc": ("Illari", "CaptiveSun", "000000003FAE"),   # the ball's flat disc, r 0.67
"illari_sun_ring": ("Illari", "CaptiveSun", "00000000656E"),   # the ball's ring, r 0.67, 0.2 tall
"illari_shock_disc": ("Illari", "CaptiveSun", "0000000085CD"), # burst / detonation shock disc (16 / 12 m)
"illari_dome": ("Illari", "CaptiveSun", "00000000EC38"),       # hemisphere (cast, mark, pylon)
"illari_ring_flat": ("Illari", "CaptiveSun", "000000001E7F"),  # throw rings
```
Reused models: `cat_bubble` DBF, `cat_sphere` 1385, `juno_disc` 5E5E, `juno_ray_wall` 1F85, `juno_torus_thin` EBEF,
`brigitte_whip_ring` 47D9, `brigitte_band` DE0. Not needed: quads 801 / F5B / 10F4E / 250C / 5708 / 86CF, ribbon meshes 7F5 / 99F / 31AF
/ E71, the pips 13DE2 (drawn as glows at the pip hardpoints), 13F85 (the beam's 21 m tube).

Proposed palettes (`HeroPalettes`, stops t, r, g, b as `TR_*`):
```
IL_SUN       = {0,0,0,0, .25f,.61f,.38f,.25f, .5f,.89f,.59f,.31f, .75f,.98f,.82f,.57f, 1,1,1,1};   // D1CA r0 / 74C6 r0 (gold)
IL_SUN_ENEMY = {0,0,0,0, .25f,.61f,.29f,.25f, .5f,.89f,.45f,.31f, .75f,.98f,.68f,.57f, 1,1,1,1};   // D1CA r1 (redder)
IL_FIRE      = {0,0,0,0, .25f,.44f,.16f,.10f, .5f,.85f,.35f,.23f, .75f,.99f,.45f,.18f, 1,1,.49f,0};  // 74C6 r1 (sparks, flames)
IL_BEAM      = {0,0,0,0, .25f,.71f,.20f,.04f, .5f,.78f,.44f,.21f, .75f,.84f,.56f,.31f, 1,1,.85f,.56f}; // 0FC2 r10
IL_SHOT      = {0,0,0,0, .25f,.50f,.24f,.10f, .5f,.67f,.33f,.15f, .75f,.89f,.46f,.23f, 1,1,.94f,.78f}; // 2B74 r2
IL_HEAL      = {0,0,0,0, .25f,.70f,.57f,.13f, .5f,.75f,.67f,.25f, .75f,.84f,.78f,.47f, 1,1,.96f,.80f}; // 1355 r4 (pylon)
IL_MARK      = {0,0,0,0, .25f,.65f,.41f,.11f, .5f,.75f,.59f,.31f, .75f,.87f,.77f,.44f, 1,1,.93f,.56f}; // 171E r9
```

## Her hair (#144): the white piece is hair material D830, not 8210
Read 2026-11-17 offline (Overwatch closed) from `research\illari\extract\Heroes\Illari\Skin\Overwatch\Overwatch\Models\000000000117.118`
(`.owmdl` submeshes, look `ModelLooks\DEFAULT-80A.owmat`, `Materials\*.owmat` statics) and the dev pack's `illari.glb`; screenshot
`overcraft_illari\run\screenshots\illari_fx_select.png`.
- **8210 is not her hair (H).** It is submesh 10 (112 v), the same strip as the eye shadow 82FB (submesh 11): x +-0.043, z 1.483-1.492
  m, skinned to the eyelid bones 0386 / 0387 / 0006 / 0007. Every hero's model carries 8210 (Tracer, Ashe, Juno, Sojourn, D.Va, Pharah,
  Cassidy, Junkrat, Torbjorn, Widowmaker...): shader 0x36 is io_scene_owm's "OWM: Blend", a soft eye-wetness layer over the lids. In the
  `.glb` it has no image and base colour (1, 1, 1, 0) MASK: at most a thin line under the eyes. Not drawing it (as `isEyeShadow` does
  82FB) is the fix if it shows; it has no colour to find.
- **The white mass is submesh 6, material D830 (H):** 2135 v, z 1.41-1.61 m, reaching 0.49 m behind the head, on head bones 0083 / 0084
  / 0012 / 05D9 - the big fan of hair in the screenshot. Shader **0x33** = io_scene_owm's "OW2 Hair". Its albedo slot 49E5C1E3 ("Hair
  Albedo + Alpha") is **44258, pure white (254-255) with the strands in its alpha**; the shader colours it from statics, so the `.glb`
  draws it white. Her dark hair (submeshes 12 / 13, D733, same shader) has a dark albedo 434C2 (0-74) and so looks right.
- **The colour is in the material's statics (H read, M which static is which).** Static **842EEB86** reads as the main hair colour
  across heroes: Brigitte `#A96A4C` (red-brown), Tracer `#8D8377` (brown), Junkrat `#C3B992` (blond), Ashe `#F4E8CC` (white), Illari's
  D733 `#333A3C` (near black) - each matches the hero's hair. For **D830: 842EEB86 = linear (0.947, 1.0, 0.319) = sRGB `#F9FF99`**
  (pale sun-yellow). Its second tint **EB3D0DAA = (0.5, 0.139, 0) = `#BC6800`** (orange; role M: root / shade - Ashe's is blue, so not
  simply "darker"), and **4C08EFCD = (0.973, 0.982, 0.165, 0.5) = `#FCFD71`** (yellow; role L, specular?). D733's for comparison:
  EB3D0DAA `#332B28`, 4C08EFCD (0.038, 0.040, 0.120). D830 also carries the glow ramp slot **5118206D = 800C076A8345** (row 1 white >
  yellow > orange `#FFFFFF > #FFFF86 > #FFEC11 > #FFBA00 > #D05D00`, row 0 black > grey) and a grey 4296F04A map 441B3 (107-157):
  M that this hair glows like a sun, gold to orange; the white-to-orange ramp is the hair shader's own (D733 has the same shape, 800F0CA20824).
- **Proposed fix:** bake D830's colour image as `glow_maps.py` does for Lucio's dreads (`HAIR_TINTS`), but from the data instead of by
  eye: `HAIR_TINTS["00000000D830"] = (249, 255, 153)` multiplied into 44258's alpha-carrying strands, shaded toward `#BC6800`
  (EB3D0DAA) at the dark end of the strand mask (M: 8B5A6290 / F4746568 slots; D830 has no strand map, so use 441B6's red channel or a
  flat tint, ours). Note `glow_maps.hair_colour` today only accepts shader 0xD9 with a black albedo; D830 is 0x33 with a white one, so
  the check needs a second branch. Not yet checked in the game.
