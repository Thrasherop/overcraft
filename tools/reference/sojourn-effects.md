# Sojourn's ability effects in the game data

What Sojourn's effects are made of in real Overwatch, where the extracted files are, and how each is rebuilt in Minecraft
(`SojournFx`). Read 2026-11-02 from the archived offline build 154088. Overwatch and Battle.net were checked closed
(`Get-Process Overwatch, Battle.net`) before the TankLib run, and `owinit.py` refuses on its own; everything was read only,
offline, through TankLib/DataTool. Text only in the repo: the masks and ramps stay in the owner's archive and reach the game
through the local asset pack. The format is [juno-effects.md](juno-effects.md)'s, shorter. Her data map:
[sojourn-map.md](sojourn-map.md); sounds: [sojourn-sounds.md](sojourn-sounds.md); hub:
[sojourn](../../techniques/heroes/sojourn.md).

Confidence: **H** read in the data, **M** inferred, **L** guess. "Size" is the spawn size (a 1 m quad at size 0.5 is 0.5 m; a
ribbon's size is its width). "Ours" marks what no data gives. Palettes: `SOJ_*` (`EffectArt.Palette`, their stops in
`SojournPalettes`), each a LUT row read from `ramps\` (the renderer's colour row); masks: `sojourn_*` and reused ones (end).

## Files and scripts
- `E:\Overwatch Archive\research\sojourn\fx\Sojourn\<Ability>\` (`Railgun`, `ChargedShot`, `PowerSlide`, `Disruptor`,
  `Overclock`): `textures\`, `ramps\`, `models\`, `manifest.json`.
- `research\sojourn\work\`: `fx_cards.txt` (one card per renderer: what this note was written from), `fx_overview.txt` (one
  line per effect), `fx_digest.txt`, `fx_summary2.txt|json`, `fx_entities.txt` (her projectiles' slots and child entities),
  `fx_clip_effects.txt` (117 clips with an animation effect), `fx_extract_log.txt`.
- Scripts `research\sojourn\scripts\fx\`: Juno's `fx_juno_*` copied as `fx_sojourn_*` (paths retargeted to `fx\Sojourn`;
  `fx_sojourn_extract.py` holds her effect list). Run: `fx_sojourn_clipfx.py`, `fx_sojourn_ents.py work\fx_entities.txt 2F98
  2484 3216 31EF 23E6 387D 694C 2D20`, `fx_sojourn_extract.py` (kiriko-anim venv, `-I`), `fx_sojourn_summary2.py`,
  `fx_sojourn_digest.py`, `fx_sojourn_overview.py`, `fx_sojourn_card.py <ids>` (under `py -I run.py`). Her palettes were
  written by a small generator in the session's scratchpad from the same ramps (`SojournPalettes`' doc lines give each LUT
  id and row).
- Counts: 57 effects asked for, 45 extracted; **12 of Overclock's (28E9, 4143, 2838, 328A, 414D, 31CA, 2E8C, 2839, 31EC, 3FFA,
  413A, 422D) are not in the build** (TankLib: FileNotFound): the Stadium beam / blast ones, as the sound pass found (H).

## Her look
- **Team cyan / teal / cream, enemy red / orange (H rows, M roles):** nearly every renderer comes as a pair on one LUT (its
  team row and its enemy row: 2837 C46 / C54, 280D C6 / C4, 31E5 C23 / C29 ...). Enemy rows are drawn blended (`Fx.soft`),
  team rows added.
- **Hardpoints (H ids, M roles):** gun 0006 muzzle, 007C the energy glow / Overclock / reload point (on the Railgun model,
  `sojourn_weapon.glb`, with 0078-0080); her body 002C / 002E / 0030 / 0031 the slide's thrusters, 002D / 002F the slide jump,
  01DA pelvis, 001B / 001C feet (ours: the foot bones), 0002 her left wrist, 0004 her body (Overclock's material).

## Railgun (1A23; round 2F98)
| Part | Effect | Rebuilt |
|---|---|---|
| Muzzle per round | **2837** (1A23 #494 on 0006 and 2F98's `m_A18274A3`): flares 210FD 0.38-0.51 m + 0.55-2.5 m, 0.13 s (`SOJ_MUZZLE`); sparks 183A7 10-15 at 2-7.6 m/s (`SOJ_MUZZLE_SPARK`); lights | flares (the wide one at 0.6, ours), 6 sparks (ours); a third the size in her own view (ours) |
| The round | **280D** (2F98's child 312B): two ribbons 0.096 m wide on the comet strip **1FDF6**, points living 0.015-0.07 s, team 4B35 r4 / enemy D1E7 r2 | `Fx.trail` 0.096 m, 0.05 s, `sojourn_round`; a 0.14 m head glow (ours); from 3 m in her own view (step 3) |
| Hit | **31E5** (2F98's `m_AED73588` = `m_BF7914D4`: the same on a hero and the world): flare 210FD 0.57-2.3 m (`SOJ_HIT`), stars 1F79A 0.17-0.31 m, sparks 183A7 2.4-2.7 m/s | flare at 0.35 (ours: 16 a second stack), 2 stars, 4 sparks |
| Energy glow | **31E6** (the gun's child 3763, always held): glows on 007C sized by **dataflow C9 (energy / 100, M): 0.15 / 0.332 from 0.503 / 0.486 from 0.975** (C46), a 0.55-0.97 m glow (C61), sparks at 0078-0080; two material effects (not drawn) | one held glow by those steps, the wide one from half (alpha ours); none at 0 energy (ours) |
| 2833 (#428, energy >= 65) | **empty** (no renderer, no sound) (H) | - |
| Reload | gun clip 6FCB's **30A6** (1P): flash 210FD 0.05-0.27 m, 15 sparks, smoke at 007C at 0.08-0.1 s; 3P clip 70D5's 30C4 only spawns an entity (its magazine, L) | flash + 6 sparks, both views (ours for others) |
| 65D3 (#1496) | Overcharged's glows on 007C (M) | not drawn |

## Charged Shot (2484 hitscan; tracer 3216)
| Part | Effect | Rebuilt |
|---|---|---|
| Muzzle | **2834** (1A23 #910 on 0006): flare 210FD 1-2.5 m (`SOJ_CHARGED_MUZZLE`), the 14-frame flash **1F8BE** 0.4-0.68 m 0.29 s, sparks | flare, flash, 8 sparks |
| Beam | **2835** (2484's effect): line 2A5D0 renderers held 0.38-1.9 s, navy / teal / green to white (`SOJ_CHARGED_BEAM`) | one band 0.3 m wide (ours), 1.3 s |
| Tracer | **2BF0** (3216's child 312D; 250 m/s): glows 0.3-0.4 m at 256-500 a second (`SOJ_CHARGED_TRACER`, cream to blue, HDR 8), sparks 1F35 at 1669 a second, 4.4-9 m/s (white to steel blue) | a glow every 0.35 m, a spark every ~1 m, each appearing as the 250 m/s tracer passes |
| Hit | **22BF** (`m_AED73588` = `m_BF7914D4`): flare 3.15 m, flash 1F8BE 2.63 m, 27-29 sparks 2.6-6 m/s, smoke 1DFCA/B 0.9-2.6 m | at 0.6 of the flares, 14 sparks, 4 smoke puffs; on every one pierced and the block it stops at |
| Fainter at low energy | no data (the beam's own scaling not read) | ours: 0.45 + 0.55 x energy / 100 |

## Power Slide (1E6F)
| Part | Effect | Rebuilt |
|---|---|---|
| The slide | **2905** (#239 on 01DA, held): thruster glows 242 0.69 m on 002C / 002E / 0030 / 0031 13/s (`SOJ_SLIDE_JET`), streaks 1F79A, dust 1DFCA/B 1.06-1.2 m at 01DA / 0017, dust 14F9 1-1.45 m, sparks 183A7 from her feet (gold `SOJ_SLIDE_SPARK`) | all of them, each client tick; dust and sparks only on the ground (ours); none in her own view (ours) |
| Friction Generators | **6BB7** in place of 2905: the same plus a speed-line field E99F (size 10) | not drawn apart (2905's look) |
| Cosmetic 387D / 694C | her own model DCA7 with 3304 / 6BBB: the same parts plus speed lines E99F (L: the 1P screen's) | not drawn (no 1P speed lines) |
| Slide jump | **330E** (#443 on 002D / 002F): smoke plume 103/s at 10.6-14.4 m/s, glow 210FD 1.4 m, sparks 23F | 10 puffs at 3-6 m/s (ours), the glows, 16 sparks |
| 2CDF (#926), 7601, Dual Thrusters 65B6 / 65B7 | a halo on 0012 / her HUD colour / sounds only | not drawn |

## Disruptor Shot (1441; projectile 31EF; field 23E6, graph 1443)
| Part | Effect | Rebuilt |
|---|---|---|
| Launch | **2C92** (31EF's `m_A18274A3`): A17F 0.4 m and **6 m**, 0.15 s (`SOJ_DISRUPTOR_LAUNCH`) | both (the 6 m at 0.12 alpha, ours) |
| Flight | **2C8D** (31EF's child 31F0): line glows C061 0.6-1 m, glow A17F 0.7 m, motes 4A58 drawn in at 0.27-0.57 m/s (50/s) | all three |
| 6336 (5342 #54 on 0001) | a hit flash on the target (M) | not drawn |
| Field | **2228** (1443 #312, not Stadium): decal 299E5 x 3DB8 6 m (`SOJ_FIELD_DECAL`), line glows, core DBF, motes 75/s, the opening flash 4A58 4.33 m, sphere **1B761** at its radius (band 1C7A), the flame ring mesh **E22E** 11.8 m (29132 x 29149), lights | decal (baked product `sojourn_field_decal`), glows, core, motes (a third, 1.5x size: ours), flash, the sphere (`cat_sphere`, alpha 0.06: at 0.18 it was a milky solid dome); **no flame ring** (no mesh) |
| Per target | **4DA2** (motes on them + a 3.1 m screen layer for the local player, ECIM(40)) and **2BB6** / 2D44 (material effects) | not drawn (no second player); the burn sounds are wired |
| Deceleration Field | **6C02** (#327): sphere 1385 at size 8 every 1.25 s (`SOJ_DECEL`) | the 8 m sphere every 1.25 s |
| 70D8 (#358) | the band DE0 held 4 s (L which state) | not drawn |

## Overclock (2159)
| Part | Effect | Rebuilt |
|---|---|---|
| On her gun | **2880** (#539 on 0006 / 0007): glows 210FD 0.5-1.4 m 20/s (`SOJ_OC_GLOW`), core DBF 0.59 m, vent streaks on mesh E613 (2A3EB x 2A3E2) | glows and core on 007C; no vents (no mesh) |
| On her | **333B** (#533 on 0004): a material effect, cyan falling to black | a faint 1.9 m glow round her (ours), not in her own view |
| Stance | `add_overclock` (834A, df 0x1AC) | played (HeroAnimator) while the synced Overclock flag is on |
| 6FE3 / 6FE2 | sounds only | - |

## Not done
- **Her own view's gun glows** (energy, Overclock, reload) are drawn at the view model's 007C but **hidden by it**: the hand
  pass draws the gun after the world, depth cleared (`rendering.md`). Only their edges show. Open.
- Material effects (Overclock on her body, the Disruptor on its targets), the field's flame ring, the Overclock vents,
  the slide's 1P speed lines, the energy-state clips' (gun 7D86-7D88) looks (none: sounds only), the enemy rows from a second
  client (not checked: `runClient2` was another session's).

## Masks in the pack
New (`build_asset_pack.py` EFFECT_MASKS, `sojourn_*`): `sojourn_round` 1FDF6, `sojourn_burst` 1F79A (3), `sojourn_star` 1F796
(3), `sojourn_line` C061, `sojourn_vents` 2A3EB, `sojourn_beam` 2A5D0, `sojourn_flash` 1F8BE (14), `sojourn_spark_tail` 23F,
`sojourn_dot` 4A58, `sojourn_swirl` 3DB8, `sojourn_field_noise` 299E5, `sojourn_flames` 29132; product `sojourn_field_decal`.
Reused (the same ids): `lucio_glow` 210FD, `cat_streak` 183A7, `spark` 1F35, `mercy_glow` 242, `brigitte_smoke` 1DFCB,
`lw_thorn_flame` 14F9, `cat_glow_dot` A17F, `cat_puff` 18B18, `cat_band` 1C7A, `mercy_noise_b` 4A7E; mesh `cat_sphere` 1385.
