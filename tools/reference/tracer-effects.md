# Tracer's ability effects in the game data

What Tracer's effects are made of in real Overwatch, where the extracted files are, and how each would be rebuilt in Minecraft
(a `TracerFx` beside `SojournFx`; today the server sends vanilla particles from `tracer/Tracer.java`, issue #150). Read
2026-11-16 from the archived offline build 154088. Overwatch and Battle.net were checked closed (`Get-Process Overwatch,
Battle.net`) before each TankLib run, and `owinit.py` refuses on its own; everything was read only, offline, through
TankLib/DataTool. Text only in the repo: the masks, ramps and meshes stay in the owner's archive and reach the game through the
local asset pack. Format: [sojourn-effects.md](sojourn-effects.md)'s. Data map: [tracer-map.md](tracer-map.md); hub:
[tracer](../../techniques/heroes/tracer.md); method: `techniques/rendering/rendering.md` "Effects and glows".

Confidence: **H** read in the data, **M** inferred, **L** guess. "Size" is the spawn size (a ribbon's is its width). "Ours"
marks what no data gives. `rows` name the LUT and its team / enemy rows (`ramps\<lut>.json`).

## Files and scripts
- `E:\Overwatch Archive\research\tracer\fx\Tracer\<Ability>\` (`PulsePistols`, `PulseBomb`, `Blink`, `Recall`): `textures\`,
  `ramps\`, `models\Models\<id>.00C\<id>.owmdl`, `manifest.json`.
- `research\tracer\work\`: `fx_cards.txt` (one card per renderer; `fx_cards_{shot,bomb,blink,recall}.txt`), `fx_overview.txt`,
  `fx_digest.txt`, `fx_summary2.txt|json`, `fx_entities.txt` (bullet 0018, pistol 0017, bomb 0045 and their children),
  `fx_clip_effects.txt` (137 clips with an animation effect: all locomotion / reload sounds but 000C), `fx_extract_log.txt`,
  `fx_effects\` (decoded effects), `fx_stu_ents.json`.
- Scripts `research\tracer\scripts\fx\`: Sojourn's `fx_sojourn_*` copied as `fx_tracer_*` (paths to `research\tracer`,
  `fx\Tracer`; `fx_tracer_extract.py` holds her effect list). Run: `py -I run.py fx_tracer_clipfx.py`, `... fx_tracer_ents.py
  work\fx_entities.txt 18 17 45 EA 10 4244 406B`, `fx_tracer_extract.py` (kiriko-anim venv, `-I`), then `fx_tracer_summary2.py`,
  `fx_tracer_digest.py`, `fx_tracer_overview.py`, `fx_tracer_card.py <ids>` under `py -I run.py`.
- Counts: **62 effects asked for, 35 found** (+ sub-effect 4010), **27 not in the build** (TankLib FileNotFound): pistols 3B20,
  3B21, 3FCF, 3FDF; bomb 458B, 458C, 458D, 3FF4, 3AEC, 3B27, 270A, 3FDB; Blink 3693, 3C95, 3AEB, 3C70, 3C98, 3F9E, 3FA2, 3FA3,
  2A1F, 4022; Recall 3AE1, 3B87, 4028, 403F, 4040 (Stadium / removed nodes, H missing). Entities 4244 (Recall #497 / #522)
  and 406B (0C1B #129) also missing. Of the 35, **14 draw something**; the rest are sounds only (see tracer sound maps) or
  empty (2860, 2B1E, 1900, 5075).

## Her look
- **Team blue, enemy red / orange (H rows, M roles).** Most renderers come as twins (same mask, sizes, hardpoint), one on a
  team LUT and one on an enemy LUT, or one LUT with team and enemy rows. Team: `#2447DE > #519EDF > #B5F5FF` (0967 r3, 5B49,
  50A1, 87FF, DCDA) with HDR tint (1.78, 2.58, 4); enemy: `#FF0000 > #FF6B0F > #FFFFFF` (8AB6, E430, F120) with tint (3.2, 0.62,
  0.33) or (1.55, 0.35, 0.35). Draw team rows added, enemy rows blended (`Fx.soft`, as Sojourn).
- **Channel-shifted LUTs (M):** the muzzle's 0847 / F66D / 7DEA / C594 / 700D / 0E90 cards print `#FF2447 > #00B5F5` and
  `#FFDE24 > #00FFED`; their first channel is not colour. Read channels 2-4: 0847 r0 is navy to `#75E8FF` (team), F66D r0 is
  `#BA0505` to `#FFD975` (enemy, red to cream). Build those palettes from `ramps\` channels 2-4, not the printed hex.
- **Hardpoints (H ids, M roles):** each pistol's muzzle **0006** (on pistol entity 0017, held at her 000A / 000B); her 0023
  (Blink), 01DA pelvis (Blink ribbons, +1.1 m), 001C / 00A4 (Recall start), 0004 body (Recall arrival); bomb in hand 0010
  (cosmetic 00EA, model 03BB / look 0395), launch 0003, bomb's own 0004 (its trail) and 0077 (blast). Bomb model **0B2C / look
  0B99** (entity 0045).

## Pulse Pistols (0016; bullet 0018, hitscan, both muzzles each shot)
| Part | Effect | Rebuilt |
|---|---|---|
| Muzzle per shot | **0076** (0018 `m_6CB564CC`, and inside 002C) at 0006, all 0.04-0.1 s (H): soft glow team A7B6 1.6 > 0.5 m (64AB, `#0013BC > #BECDFF`) / enemy 210FD 2.7-3.1 m (9A67 red) at +0.05 m; flash **20848** (3 frames) 0.37-1.24 m (team 0847 / enemy F66D) at +0.02 m, and 0.35-0.54 m (0A3C, 0.02-0.08 s); flash **207C2** (4) 0.32-0.42 m 0.08-0.1 s, and 3 at 0.19-0.33 m (C594 / 7DEA); flash **20847** (3) 4 at 0.26-0.32 m (0E90 / 700D) and 0.13-0.25 m; speeds 0.07-0.18 m/s forward; two lights 2.5 m at +0.55 m. H parts, M twins | at both pistols' posed 0006 each 0.05 s shot: one glow, one 20848, two 207C2, two 20847, one light; a third the size in her own view (ours, as Sojourn) |
| The line | **002C** (0018's STUEffectComponent): two renderers on quad 801 with masks **510** x **50C** and length curve 8006AADA, emitter ECIN (muzzle to hit, M), **0.1 s**, width field 0.03 m (M); team 0967 r4 tint (1.78, 2.58, 4) / enemy CDAD r4 (3.2, 0.62, 0.33) | `SombraFx.tracer`'s band (the same masks, `sombra_tracer_line` / `_edge`): 0.03 m wide, 0.1 s, HDR blue; one per pistol |
| Hit the world | **0114** (`m_BF7914D4`, 0.67 s) H: puff **59B2** (4 frames, colour sprite, grey tint) 0.23-0.29 m (M); flash **F55C** (4) team ramp 722B `#0048FB > #B5F5FF` (no enemy twin: blue for all, M); decals 220A **0.035 m** gold-orange (AEA5 r2, HDR 3.6) and 29F7 **0.2 m** cyan-blue (5C90 r2, HDR 4), both 0.67 s | puff, flash, the two decals (`sombra_impact_burst`, `lucio_spark` FLAT) |
| Hit a creature | `m_AED73588` **empty** (H): no hero hit of her own | ours: the 0114 flash only, no decal |
| Reload, idle | clip 000B / 0021 sounds only (H); pistol clip 66's **000C** (0.43 s in): glows F767 0.72 m, 8DB sparks at 0029 / 0032 (M: a pistol flourish) | not drawn (low) |
| 3E3F | perk 0123 / Stadium (H gate) | not drawn |

## Pulse Bomb (0034; entity 0045, graph 0C1B)
| Part | Effect | Rebuilt |
|---|---|---|
| The bomb | model 0B2C / 0B99 (H) plus **0171** (child 0164) on its 0004: a ribbon on mask **1298** x curve 800E9688, **0.53 m** wide, 60 points/s living **0.59 s**; team 7179 r5 `#0041FE > #FFFFFF > #0018FF` / enemy r6 `#FE0000 > #FFFFFF > #FF0000` (H) | the bomb model (her map's 160BB is exported, `research\tracer\export\bomb`; the entity names 0B2C, L which skin) and `Fx.trail` 0.53 m, 0.59 s, in flight and stuck |
| Fuse | **016E** (0C1B #552-557 held in the StateWaits, 2.93 s; fuse sounds 18AF / 5D7A / 5E79, H): three pulses at 0 / 0.33 / 0.67 s, mesh A1A with 1298, 0.3 s each, size from data flow (L: the bomb's size), team 6E4E r1 / enemy 9713 r2; glow 210FD **1.38 > 2.52 m** for 0.21 s from 0.3 s (14C0 / 662F r1); **32 sparks** 183A7 **inward at 16 m/s**, 0.05 m, 0.25 s from 0.1 s (DC96 r1 / 173A r1); 5 streaks 20995 0.18-0.36 m up at 0.3 s; distortion 85B (18F81 normals) size 10, 0.25 s; light 5.7 m for 1.5 s | the pulses as 0.6 m glows (ours size), the swelling glow, 32 sparks drawn in, the 5 streaks, a light; no distortion |
| Blast | **0153** (0C1B #137 on the detonation message, 1.17 s, at 0077; explosion sound 603F, H): **decal 20CE6 2.5 m** 1.17 s (155C r1 / r2); flare 210FD 0.73 s (47E7 r0 / r1, size data flow L); shells **meshes D8EE / D8F1** (F7D8 band x 4DB noise) size 1.47 at 0.1 s, 0.4 s (A97E team / 74A4 enemy); fireball spheres DBF x 460, 0.31 s and 0.14 s (85F0 r1 / r2, E200 r3 / r4); flipbook **1D113** (8) 0.27-0.35 s; smoke **2097D** (32 frames, colour 2097E) x6 living 0.93-1.56 s; streaks 20995 103/s 0.53-0.71 m 0.15-0.19 s; **100 sparks** 183A7 at **9.2-10.3 m/s**, 0.13-0.52 s, 0.03-0.05 m (orange 7440); distortion DBF; light 5.6 m 0.53 s | all but the distortion; shells as meshes if the pack carries D8EE / D8F1, else a 4 m ring (ours); sphere sizes to the 4 m radius (ours) |
| 0034 at hp 0011 | 0034 #584 / #619 play 0153 / 016E at 0011 too (L: on the stuck target) | the bomb's position |
| Throw | clips 0039 / 004D sounds only; the hand bomb 00EA has no effect (H) | - |

## Blink (0024 #46 StateBlinkEffect, hp 0023)
| Part | Effect | Rebuilt |
|---|---|---|
| Leaving | **018F** (`m_78EE3D16`): flash **214A5** (512x64 streak, R8) **4 m**, 0.21 s; team 73D0 r0 `#007ABC > #FFFFFF` / enemy r1 `#BC0000 > #FF9500` (H) | at the start point, facing the camera |
| The trail | **0190** (`m_3024B8FC`, 0.67 s): three ribbons on 01DA +1.1 m emitting 0-0.3 s: 2A063 x 1038 **0.2 m / 0.4 s** (02B8 r4 / r5), 1C7A x 1038 **1.0 m / 0.2 s** (8E75 r3 / r4, HDR 3), 2A063 x 19167 **0.33 m / 0.5 s** (46A2 r4 / r5); flash 214A5 **3 m 0.09 s** (5B49); sparks 4C09 burst **200 + 100/s, 4-4.2 m/s**, 0.2-0.3 s, 0.02-0.05 m (026B r2 / r3 tints); two body material flashes (5D3D blue / red); lights 3 m 0.3 s. H parts, M: the ribbons span start to end | three `Fx.trail`s fed the old and new pelvis +1.1 m (and her next 0.3 s), the flash at the end, 60 sparks (ours: a third), a light; no material flash |
| 3692 | perk 010F only (0024 #216, a Stadium power): rings 1BAC, sphere 3299, 40 sparks 15-18 m/s, a 1P layer (ECIM(40)) | not drawn |

## Recall (001A; rewind = STU_41B3FC2B, 0.9 s)
| Part | Effect | Rebuilt |
|---|---|---|
| Rewind starts | **00BA** (#24 onBegin, 2 s, her 001C / 00A4): flashes 29F7 **7 m** (198A red) and **3.5 m** (524D blue) 0.12 s at +1 m; F767 **1.45 m** 0.35 s (8AB6 / 50A1); F7A7 **0.84 m** at 0.4 s (E430 / DCDA); **60 sparks 4F3 drawn in at 5.5 m/s**, 0.05-0.08 m, 0.22-0.33 s (43CB / 05DB); 5-7 soft 4F3 size 1; **100 motes** 4F3 at 0.37 s, 0.03-0.08 m, living 0.45-1.14 s (7FB9 / 02B2); spheres 85B x 460 10/s for 0.2 s; distortion C4A 4 m; **decal 2C66C 0.48 x 0.75 m, 2 s** (F438 / 2A15); lights 3 m | where she leaves: the flashes (the 7 m one at 0.15 alpha, ours), sparks in, the motes, the decal |
| Her own screen | **6E9F** (#642 onBegin, 0.8 s, ECIM(40) = the local player's layer, H): vignette **5D0** 2 m (93F8 violet to cyan), pink 2-frame C44 0.52 m 20/s, distortion C4A | a 0.8 s screen tint with `ana_vignette` (as Ana's) |
| Arriving | **00BB** (#25 onEnd, 1 s, her 0004): glows 29F7 + D2D **1.35 m**, 4 + 40/s for 0.1 s, 0.31-0.4 s; **rings mesh 958** at +1.1 m rising 2.25 m/s, size 0.85, 10/s for 0.27 s, 0.23 s (0089 / 6755); shards mesh 961 (B95, 3 frames); F767 flashes (F120 / 87FF); two body material flashes (4C4D, 5D3D) | at the end point: the glows, 3 rings (FLAT, ours as quads if no mesh), the flash |
| The path between | not an effect: the engine's rewind (STU_41B3FC2B, H) | ours: motes along her 3 s history while she rewinds |

## Not done
- What sets the ECIN sizes (bomb pulses, blast flare / spheres): data flow inputs, not read (L: the 4 m radius).
- Material effects (Blink, Recall, her body flash) and the 18F81 / C4A distortion layers: not drawable; the hero-hit effect
  is empty. The channel-shifted LUT reading (above) is inferred from the team ramp's shape (M), not from the shader.
- The pistol clip 000C flourish, perk-only 3E3F / 3692, Stadium nodes (missing).

## For the asset pack (paths under `E:\Overwatch Archive\research\tracer\fx\Tracer\`)
- **New masks** (proposed `tracer_*` in `build_asset_pack.py` EFFECT_MASKS; `<Ability>\textures\<id>.png`, frames `_1`..):
  PulsePistols: `tracer_flash` 20848 (3), `tracer_flash_b` 207C2 (4), `tracer_flash_c` 20847 (3), `tracer_puff` 59B2 (4),
  `tracer_hit_flash` F55C (4). PulseBomb: `tracer_dot` 1298, `tracer_white` 0460, `tracer_streak` 20995, `tracer_shell` F7D8,
  `tracer_blast` 1D113 (8), `tracer_smoke` 2097D (32; colour 2097E), `tracer_blast_decal` 20CE6. Blink: `tracer_blink`
  214A5, `tracer_ribbon_noise` 1038. Recall: `tracer_glow` F767, `tracer_glow_d` D2D, `tracer_recall_decal` 2C66C,
  `tracer_shard` B95 (3), `tracer_pink` C44 (2).
- **Reused** (same ids): `glow` A7B6, `lucio_glow` 210FD, `sombra_tracer_line` 510, `sombra_tracer_edge` 50C,
  `sombra_impact_burst` 220A, `lucio_spark` 29F7, `cat_streak` 183A7, `mercy_noise` 4DB, `ana_fade_v` 2A063, `cat_band` 1C7A,
  `cat_glow_noise` 19167, `cat_soft` 4C09, `lucio_soft` 4F3, `cat_sparkle` F7A7, `ana_vignette` 5D0.
- **Ramps** (`<Ability>\ramps\<lut>.json`, palettes `TR_*`): pistols 8000465F0847, 800E952DF66D, 8005473E64AB, 800CE2D19A67,
  800B1789C594, 800F1F6B7DEA, 800B99180E90, 8007F937700D, 800ECAF10967, 8001789CCDAD, 8005EEB3722B, 8000752EAEA5,
  800C4C875C90; bomb 80027179, 47E7, A834, 85F0, E200, 155C, A97E, 74A4, 14C0, 662F, 6E4E, 9713, DC96, 173A, 7440 (full ids in
  the manifest); Blink 73D0, 02B8, 8E75, 46A2, 5B49, 026B; Recall 198A, 524D, 8AB6, 50A1, E430, DCDA, 43CB, 05DB, 7FB9, 02B2,
  F438, 93F8, 0089, 6755, F120, 87FF.
- **Meshes** (`<Ability>\models\Models\<id>.00C\<id>.owmdl`): PulseBomb **D8EE, D8F1** (blast shells), DBF, A1A, 085B;
  Recall **0958** (ring), 0961 (shard). Quads 0801 / 0F5B and ribbon meshes 075D / 31AF are not needed.
