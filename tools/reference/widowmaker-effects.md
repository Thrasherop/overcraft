# Widowmaker's ability effects in the game data

What Widowmaker's effects are made of in real Overwatch, where the extracted files are, and how `WidowmakerFx` rebuilds each in Minecraft
(#138; the server's messages come from `widowmaker/Widowmaker.java`, no vanilla particles). Read 2026-11-17 from the archived offline build
154088. Overwatch and Battle.net were checked closed (`Get-Process Overwatch, Battle.net`) before the TankLib run, and `owinit.py` refuses on
its own; everything was read only, offline, through TankLib/DataTool. Text only in the repo: masks and ramps stay in the owner's archive
and reach the game through the local asset pack. Format: [ashe-effects.md](ashe-effects.md) (copied from it); data map:
[widowmaker-map.md](widowmaker-map.md); assets: [widowmaker-assets.md](widowmaker-assets.md); method: `techniques/rendering/rendering.md`
"Effects and glows".

Confidence: **H** read in the data, **M** inferred, **L** guess. "Size" is the spawn size (a 1 m quad at size 0.5 is 0.5 m; a line's or
ribbon's is its width; a mesh's size scales its own extent). "Ours" marks what no data gives. `XXXX rN` = the colour LUT (last 4 hex digits
of `ramps\8...XXXX.json`) and its row. `uniform(a, b)` = a +- b (M). "hp" = hardpoint.

## Files and scripts
- `E:\Overwatch Archive\research\widowmaker\fx\Widowmaker\<Ability>\` (`Kiss`, `Hook`, `Mine`, `InfraSight`, `Perks`): `textures\` (PNG,
  flipbook frames `_1.._n`), `ramps\` (float LUTs as JSON), `models\`, `manifest.json`.
- `research\widowmaker\work_fx\`: `fx_cards_{kiss,hook,mine,perks}.txt` (one card per renderer: what this note is written from) and their
  `_compact.txt`, `fx_overview_log.txt` (one line per effect), `fx_summary2.txt|json`, `fx_meshes2.json`, `fx_entities.txt` (her, the
  Kiss's, the shots', the claw's, the mine's and the dart's effect slots and children), `fx_clip_effects.txt` (every clip of hers naming an
  08F), `fx_effects\` (decoded), `fx_extract_log.txt`, `fx_palettes2.txt` (the `WIDOW_*` stops below).
- Scripts `research\widowmaker\scripts\fx\`: Ashe's `fx_ashe_*` copied as `fx_widow_*` (each header says so; `fx_widow_extract.py` holds
  her effect list with each one's graph node / entity slot / clip; `fx_widow_pal.py`'s `PAL` is hers). Run: `fx_widow_extract.py`
  (kiriko-anim venv, `-I`); then under `py -I run.py`: `fx_widow_ents.py`, `fx_widow_clipfx.py` (reads `work_assets\clipmap.json`),
  `fx_widow_summary2.py`, `fx_widow_overview.py`, `fx_widow_card.py <ids>`, Torbjörn's `fx_torbjorn_cardsc.py` to compact, `fx_widow_pal.py`,
  `..\..\..\pharah\scripts\fx\fx_pharah_rows.py widowmaker <lut4>`.
- Sources of the list: `work_kit\c_<graph>.txt` (001B, 0056, 0057, 005A, 0067, 0073, 59BF, 59C0: every effect node), `work_fx\fx_entities.txt`
  (`fx_widow_ents.py` over 001C 001D 002A 004C 0107 004B 0108 60CB 00D3 00D9 00DA 00DB 00AF 00AE 6B75 13F6 7527 0161), `fx_clip_effects.txt`.
- Counts: **81 effects extracted** (with the sub-effects 18F7 / 18FB / 1FD6 / 18F9 / 4010 / 4047 / 18F8 / 6DC0), 0 missing. 29 draw
  something (counting four material effects and a prop model); the rest are sounds only or empty: the shot sound slots 008C / 008D, 013A / 7A0F / 0231 / 3493 / 3CB3 / 7B6D (001B), 2860 /
  2B1E (the Kiss's children), **2B22 (the scoped tracer 60CB's child: empty)**, 00E3 / 0158 / 7B67 / 0154 / 00E4-00E6 (the hook), **0425 the
  claw's hit and 7AD1 its child: empty**, 5F58 / 7CCD / 00F9 / 6037 / 1A1F / 67BF (the mine), 6DDC / 6DDD / 6DDE (Seeker Mine), every clip
  but 2D8A (the Kiss's scope-in) and 0078 (a reload prop).

## Her look
- **White and purple for her, red for enemies (H rows, M roles).** As Ashe's, most renderers are twins on one mask and size: the hip line
  3B92 r4 orange / 7ED6 r4 red, the scoped smoke 33BA r6 white / 609B r6 red-orange (0219 r6 / D44D r5 the same at 50 % and full), the hit
  6AA6 r2 white / 76F6 r2 red, the mine's burst 0665 r7 purple / A504 r7 red (and 5F6E / B62E, B7E9 / CD26, 429E / 017E), the poison's domes
  5185 r10 / 30D7 r9, the dart's ribbon F081 r2 / r3. The scoped core (35AC r4 / 818E r4), its glow (7C8A r2) and the scoped muzzle have no
  twin: one look for all (H).
- **Her lines are Sombra's tracer masks (H):** 510 x 50C on quad 801, widths from field `59DC556C` (M, as Ashe's / Cassidy's), length ECIN
  (M: muzzle to hit). Her hip hit **010F is Cassidy's Deadeye hit** (the same id, H).
- **HDR peaks (H):** the hip line 7x, the full core 10.7x, the scoped glow 5.5x, the hit sparks / stars 30x, the cast sparks 7x, the smoke 5x.
- **Hardpoints (H ids; M roles):** the Kiss's **0006** (muzzle: every muzzle renderer, lights at hp 6); her **0055** (the rope's start,
  005A #38 / #260 data flow, per the map) and **0029** (the hook's launch 0291 and the mine's cast 0285; M her launching hand: ours the
  posed 0055 / the mine's first position); the poison's **0001** / **0012** (1A58, not live); the Kiss's **007F** (2D8A, the scope glint).
- **First person vs third person:** one muzzle effect for both (H); only the rope differs (0277 3P / 33C5 1P, H). Ours: muzzles a third the
  size in her own view (as Ashe's), the same rope in both.

## Widow's Kiss (001B; hip shot 004C, scoped 0107 / 004B / 0108 by charge, hitscan)
| Part | Effect | Rebuilt |
|---|---|---|
| **Hip muzzle** | **18F7** (0066's sub-effect, at 0006; 1 s) H: flashes **299E1 x 3269F (4) 1.3 +- 0.75 m, 0.05-0.07 s** (9BF5 r5 gold, HDR 6; 1E64 r6 red, M the enemy row); glow **A7B6 0.3 m 0.05 s**; glow **2007A 0.2 m 0.03 s** (DFEE r0); flash 3269F **0.4 +- 0.15 m 0.1 s**; quad 801 0.2 +- 0.1 m 0.07 s (3F3F r6); lights 2 m | `hipMuzzle` at the posed 0006: the long flash (`widow_flash#4`, `WIDOW_MUZZLE` / `_ENEMY`, streak, ours half size), the round flash, `glow` and `widow_muzzle_glow`; a third in her own view (ours) |
| **Hip line** | **0066** (004C's STUEffectComponent) H: twins on 510 x 50C, **width 0.035 m, life 0.15 s**, 3B92 r4 / 7ED6 r4 (HDR 7) | `sombra_tracer` band 0.035 m, 0.15 s, `WIDOW_LINE` / `_ENEMY` |
| **Scoped muzzle** | **18FB** (< 50 %) / **1FD6** (50-99 %) / **18F9** (full) (at 0006) H: colour flash **2A02D (4, BC7 colour) 1.0 / 1.5 / 2.0 m, 0.053-0.06 s** (5FCF r1 x10) at -0.325 m; two side flashes 2A02D **0.61-0.75 / 1-1.14 / 1.25-1.39 m, 0.04-0.06 s at +-0.13 m**; flare **DA5B 1-1.5 / 1.25-1.75 / 1.5-2 m, 0.03 s** (86EF r0); lights 2 / 2.5 / 3 m; sub-effect 4045 / 4010 / 4047 (sounds) | `scopedMuzzle`: the three flashes (`widow_scoped_flash#4`, `NATURAL`: its own colours), the flare (`cat_flare`, `WIDOW_FLARE`; ours 0.05 s: 0.03 is under two frames); ours half size |
| **Scoped line, < 50 %** | **0118** (0107's STUEffectComponent) H: core **510 x 50C 0.158 m, 1 s** (35AC r4 orange); smoke bands **4D5 x B35 x 965 0.5 m (BFED r6) and 0.733 m (33BA r6 white / 609B r6 red), life 3 s**, alpha by age (r0's alpha 0.27 > 0) | core `sombra_tracer` at half width (ours) for 1 s, `WIDOW_CORE`; one blended band (`widow_smoke_band`, `WIDOW_SMOKE` / `_ENEMY`) 0.733 x 0.5 (ours, L: B35's cross-section) wide for 3 s, alpha 0.3, capped at 80 m (ours) |
| **Scoped line, 50-99 %** | **0067** (004B) H: core 510 0.158 m 1 s (818E r4, HDR 10.7); glow **965 0.1 m, 0.3 s** x2 (7C8A r2 warm white, HDR 5.5); smoke **0.933 m, 3 s** (6BA2 r6 white / 0219 r6 red); distortion 9A6 0.5 m (not drawn) | the same core, the glow band (`ana_nano_line`, `WIDOW_GLOW`) 0.1 m 0.3 s, one band 0.933 x 0.5 |
| **Scoped line, full** | **0117** (0108) H: core 0.158 m 1 s (818E r4); glow **0.5 m 0.3 s** (7C8A r2); smoke **1.375 m (889E / 8B64 r5 white) and 2.13 m (D44D r5 red), 3 s**; distortion 9A6 1 m (not drawn) | the core, a 0.5 m glow, two bands 1.375 / 2.13 x 0.5 at 0.3 / 0.15 alpha |
| Scoped tracer | 60CB (250 m/s): child 60CF = **2B22: empty** (H) | nothing (the line is the look) |
| **Hit, hip** | **010F** (004C `m_AED73588` and `m_BF7914D4`: one effect for both, H; Cassidy's Deadeye hit too): sparks **1BE6 (4) 2 at 2 m/s, 0.225-0.325 m, 0.05 s**, 0.05 m off (6AA6 r2 white / 76F6 r2 red, HDR 30); star **1BEC (2) on mesh EF7 0.19-0.29 m, 0.05 s** (0304 r2 / 2B70 r2); **decals 220A 0.1 m (0.035 m sides), 0.667 s, random turn** (35DA r2 yellow > red) | `"widow_hit"`: sparks (`sombra_impact_spark#4`, `WIDOW_HIT_SPARK` / `_ENEMY`, ours 0.1 s), star (`sombra_impact_star#2` on `torb_hit_shard_mesh` = EF7, `WIDOW_HIT_STAR` / `_ENEMY`), decal (`sombra_impact_burst`, `WIDOW_HIT_DECAL`) on a block face only (ours) |
| **Hit, scoped** | **0110 / 0111 / 0112** (0107 / 004B / 0108, the three slots each one effect): the same sparks and star 0.1 / 0.125 m off, **no decal** (H) | variant `HIT_SCOPED`: no decal; a block hit doesn't say which shot made it: the decal on blocks for both (L) |
| Scope (Take Aim) | **2D8A** (Kiss clip ads_in, 3P): a bubble DBF 0.06 m at the Kiss's 007F, a glint 4C09 x 16E9D 0.3 m and a lens 23C0 0.4 m at 0.067 s for 0.6 s (BC2B / 83F3 / 71FC rows) H; 1P clips 0037 / 0038: sounds | not drawn (no scope message reaches the client's effects; the HUD draws the scope) |
| Reload | clip 0078 (3P reload): a prop model 306C (0.075 m, 0.33-0.8 s; M the Kiss's clip piece, entity 13F6) H; 1P 002B: sounds | not drawn (the model's own) |

## Grappling Hook (005A; claw 00D3)
| Part | Effect | Rebuilt |
|---|---|---|
| **Rope** | **0277** (005A #38, 3P, 2 s, held while the claw is out) H: ribbon on mesh F84, **0.025 m**, on her own grey textures **1DD6 / 1DD3** (BC1 colour, sRGB 105-214) x 460 x 1DD4 (normals); **33C5** (#260, her 1P): the same ribbon on energy masks 1DD4 x 19167 x 29D41 (3842 r5 blue / r7 orange, HDR 3.5) | `WidowmakerFx.hook` each frame: a blended band (`widow_rope` = 1DD6, `WIDOW_ROPE`: its own greys) 0.025 m from the posed launcher 0055 to the claw; ours the 3P rope in her own view too |
| **Launch** | **0291** (005A #123, under the beam, at **hp 0029**) H: puff **1DC68 x 1DC1F 0.15-0.2 m, 0.35 s at 0.25-0.3 m/s, at 0.1 s** (C636 r4 purple); **6 sparks 1F35 at 5-8 m/s, 0.065 m, 0.09 s** (A089 r0 gold, HDR 7) | on the hook's first frame, at the launcher: the puff (`widow_cast_puff`, `WIDOW_CAST`) and 6 sparks (`spark#4`, `WIDOW_CAST_SPARK`) |
| Claw in flight / hit | 0154 (`m_A18274A3`): sounds; **0425 (hit) and 7AD1 (child 7527): empty**; 00E5 / 00E6 / 00E4 (flight / reel / pull entities): sounds (H) | nothing |
| Pull | **3398** (#261): a material effect on her (19167 x 29D41, EBEF rows) H | not drawn (material) |

## Venom Mine (0056 throw, mine 0057 on 00AE / thrown 00AF, poison 0073)
| Part | Effect | Rebuilt |
|---|---|---|
| **Throw** | **0285** (0056 #68 at **hp 0029**): the hook launch's puff and sparks (the same numbers, at 0.033 / 0.067 s) H | at the mine's first frame, its first position (ours: hp 0029 not posed) |
| **In flight / arming** | **00FE** (thrown 00AF's child 0161, and 0057 #118 on arming, H): glows **242 3.7 m (8EE5 r2 red) and 2.07 m (B4DA r2 blue), 0.25 s**, at +0.21 m; lights 3 m; sub-effect **18F8**: ribbons **AC4 0.5 m, 60 points/s, 0.5 s** (FD44 r1 violet, HDR 4.9; 74CA r0 pink) | thrown and again when `armed()`: one flash (`mercy_glow`), blue for her side 2.07 m, red for enemies 3.7 m (M: as a mine's light), alpha 0.6; in flight `Fx.trail` on `cassidy_ribbon` 0.3 m (ours of 0.5) / 0.5 s, `WIDOW_MINE_TRAIL` |
| Armed | nothing held in the effect data (H; the model's own light, L) | ours: a 0.35 m pulsing glow of the flash's colour (L) |
| **Burst** | **00EC** (0057 #76 / #117, 2 s) H: half dome **E531 (0.98 x 0.48 x 0.98) 5.4-5.9 m, 0.67 s** x 299E6 x 299E3 x 1C7A (0665 r7 purple / A504 r7 red); hemisphere **963D 2.4 +- 0.3, stretch 3, 0.92 +- 0.12 s** x 299E1 (5F6E r7 / B62E r7); sphere **85B 3 m, 0.17 s** x 299E3 (B7E9 r4 / CD26 r4); **3 flames 1CAFA on mesh E58C (5.7 x 5.1 x 6.7) 1.17 +- 0.5, 0.65 s** (429E r5 / 017E r5); lights 7 m 0.43 s | `"widow_mine_burst"`: the half dome on **`ashe_charge_dome` (B.O.B.'s 85D0, the same shape: E531 not exported)** x `pharah_flame_noise`, `torb_hemisphere` x `cat_noise`, `pharah_sphere` x `cat_ring_line`, 3 `torb_flame` sprites (ours: E58C not exported), `WIDOW_BURST_*` / `_ENEMY` |
| **Poison (on each target)** | **3FCA** (0073 #53, 5 s; 59C0 #53 the dart's the same) H: clouds **4F0 (4) 8 /s, 2 s, 0.56-0.78 m/s, 0.8 m** (A7B8 r3 purple); ring **52E8 on flat mesh 310C 2 m, 3 s** (6B93 r2 blue-violet); domes **DAF 10 /s, 0.76 +- 0.5 s, 0.65 +- 0.35 m, stretch 3, spin 1-2** (5185 r10 dim purple / 30D7 r9 red); bits **9487 (4) 8-14 /s, 1-2 s, 0.05-0.5 m, spin** (F0AC r1 violet) | `"widow_poison"` starts it on the target for 5 s: clouds (`dva_sd_smoke#4`, blended, `WIDOW_POISON_CLOUD`, alpha 0.35), the ring held at its feet (`cat_halo_ring`, `WIDOW_POISON_RING`), domes 4 /s as round sprites (`cat_noise`, `WIDOW_POISON_DOME` / `_ENEMY`; ours: not the DAF mesh), bits 6 /s (`widow_poison_bits#4`); the last 0.5 s fading (ours) |
| Reveal | **02FF** (0073 #62): a material effect only (the outline, H) | WidowReveal's outline (not here) |
| 1A58 / 1A1F | 0057 #141 ActionPlayScript 224E (the shared sleep-dart script, under 081F.134: Stadium, L): 8 s of particles at hp 0001 / 0012 | not drawn (not live) |

## Infra-Sight (0067)
| Part | Effect | Rebuilt |
|---|---|---|
| Cast / held | **0276** (#66): a material effect + a sound (H); clips 005A (1P) / 00A2 (3P): sounds; the reveal 0015.053 is a status effect running graph 006A (outlines) | nothing drawn (WidowReveal outlines; a screen tint #212 / #215 is HUD, L) |

## Perks: Seeker Mine (59BF, darts 6B75)
| Part | Effect | Rebuilt |
|---|---|---|
| **Dart** | **6DBF** (dart 6B75's child 6B7C; 59BF #118): 00FE's flashes (H); sub-effect **6DC0**: ribbons 75D on **187EB 0.155 m / 0.2 s** (F081 r2 purple > white, r3 red), **0.53 m / 0.59 s** (D0B1 r2 purple) and 0.71 m / 0.73 s; the dart model 61CA (not exported) | `"widow_dart"`: the flash at the mine at half size (ours), then per frame a dart at 15 m/s homing on the target's centre: two `Fx.trail`s on `lw_bokeh` (0.155 m / 0.2 s, 0.53 m / 0.59 s at 0.3; `WIDOW_DART_TRAIL` / `_ENEMY`; ours not the third), a 0.25 m head glow (ours) |
| **Dart hit** | **6CDD** (59BF #186 / #189, M: on arrival): 00EC's LUTs at **E531 1.1-1.6 m, 963D 0.45 +- 0.3, 85B 0.6 m, E58C 0.22-0.5** (H) | the burst at those sizes where the dart lands |
| Widow's Bite, Sniper's Instinct, Scoped Efficiency | no effect of their own (51EF: none, H) | nothing |

## Where it hooks in
| Message / call | Draws |
|---|---|
| `"widow_shot"` (variant `SHOT_HIP` / `_SOFT` / `_MID` / `_FULL`) | the muzzle at the posed 0006 (or the view model's in her own view), the line, a scoped shot's core, glow and smoke |
| `"widow_hit"` (`HIT_CREATURE` / `HIT_BLOCK` / `HIT_SCOPED`) | 010F / 0110-0112; the decal on a block |
| `"widow_mine_burst"` | 00EC |
| `"widow_poison"` (`POISON_MINE` / `POISON_DART`) | 3FCA on the target for 5 s (`frame()`) |
| `"widow_dart"` | 6DBF at the mine, the dart flying (`frame()`), 6CDD on arrival |
| `WidowmakerFx.hook` (ProjectileModelRenderer, each frame) | the rope 0277; 0291 on its first frame |
| `WidowmakerFx.mine` (each frame) | 0285 + 00FE thrown, 18F8 in flight, 00FE again on arming, ours the armed light |
| `"widow_reveal"` | handed to WidowReveal (outlines) |

## Not found / not done
- **Empty in the data (H):** the scoped tracer 60CB's 2B22, the claw's hit 0425 and child 7AD1, Infra-Sight's own particles (0276 is a
  material), the reveal 02FF (a material). No shell eject, no hook-pull particles.
- Twin roles (white / purple / blue = hers, red = enemies) are M throughout; the mine's flash pair red 3.7 m / blue 2.07 m is read as
  enemy / hers (M; both could be drawn together for a purple, L).
- Not drawable: distortion 9A6 (scoped lines), material effects (3398 the pull, 0276, 02FF), lights.
- Smoke band width x0.5 and 80 m cap, the core at half width, muzzles at half size, the armed glow, the dart head: ours.
- **Meshes not exported (Blender is locked):** E531 (the burst's half dome: drawn on 85D0 `ashe_charge_dome`), E58C (the burst's flame
  shell: drawn as `torb_flame` sprites), 310C (the poison's flat ring quad: a flat particle), the dart 61CA. When the export lock is free:
  add `"widow_burst_dome": ("Widowmaker", "Mine", "00000000E531")` and `"widow_burst_flame": ("Widowmaker", "Mine", "00000000E58C")` to
  `EFFECT_MODELS`, list them in `HeroModels.EXTRAS`, and switch `burst()` to them.
- Stadium / not live: 1A58 / 1A1F (script 224E), 67BF.

## For the asset pack (sources: add `E:\Overwatch Archive\research\widowmaker\fx`)
Added to `EFFECT_MASKS` (copied into `E:\claude\mods\overcraft\local-assets\pack` 2026-11-17 with `OC_ASSET_PACK` set to it and
`py tools/build_asset_pack.py effects --only widow_ "E:\Overwatch Archive\research\widowmaker\fx"`: 17 textures):
```
"widow_flash": ("Widowmaker", "Kiss", "00000003269F", 4),          # hip muzzle flash (18F7)
"widow_muzzle_glow": ("Widowmaker", "Kiss", "00000002007A", 1),    # hip muzzle glow (18F7 C25)
"widow_scoped_flash": ("Widowmaker", "Kiss", "00000002A02D", 4),   # scoped muzzle colour flash (BC7, drawn NATURAL)
"widow_rope": ("Widowmaker", "Hook", "000000001DD6", 1),           # the rope's own grey texture (0277)
"widow_cast": ("Widowmaker", "Hook", "00000001DC1F", 1),           # the cast puff's second mask (0291 / 0285)
"widow_poison_bits": ("Widowmaker", "Mine", "000000009487", 4),    # the poison's bits (3FCA C21)
```
`PRODUCTS`: `"widow_cast_puff": ("brigitte_soft_square", "widow_cast")` (1DC68 x 1DC1F), `"widow_smoke_band": ("cassidy_smoke_noise",
"ana_beam_line", "ana_nano_line")` (4D5 x B35 x 965). Reused (same ids already in `EFFECT_MASKS`): `sombra_tracer` (510 x 50C), `ana_nano_line`
965, `glow` A7B6, `cat_flare` DA5B, `sombra_impact_spark` 1BE6, `sombra_impact_star` 1BEC, `sombra_impact_burst` 220A, `brigitte_soft_square`
1DC68, `spark` 1F35, `mercy_glow` 242, `cassidy_ribbon` AC4, `pharah_flame_noise` 299E6, `cat_noise` 299E1, `cat_ring_line` 299E3,
`torb_flame` 1CAFA, `dva_sd_smoke` 4F0, `cat_halo_ring` 52E8, `lw_bokeh` 187EB. Reused models: `torb_hit_shard_mesh` EF7, `torb_hemisphere`
963D, `pharah_sphere` 85B, `ashe_charge_dome` 85D0 (for E531). Not needed: B08 / 9A6 / 1DD4 (normals, distortion), 1DD3 (the rope's second
texture), 460, 29D41 / 19167 (the 1P rope), 23C0 / 4C09 / 16E9D / DBF (the scope glint).

Palettes (`WidowmakerPalettes`, stops t, r, g, b; from `fx_widow_pal.py`: black at 0, HDR rows hue-clamped, a near-white top dropped, falling
rows reversed; each line's source row and note is in `work_fx\fx_palettes2.txt` and the array's comment). Aliases not given their own array:
`WIDOW_SMOKE_FULL` (8B64 r5) = `WIDOW_SMOKE`, `WIDOW_SMOKE_FULL_ENEMY` (D44D r5) = `WIDOW_SMOKE_ENEMY`, `WIDOW_CORE_FULL` (818E r4) =
`WIDOW_CORE` within 0.01; 74CA r0 (the mine's second ribbon) not drawn. `WIDOW_ROPE` is the rope texture's own row 0 (`tex:1DD6`).
