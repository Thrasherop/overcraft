# Torbjörn's ability effects in the game data

What Torbjörn's effects are made of in real Overwatch, where the extracted files are, and how each would be rebuilt in Minecraft
(a `TorbjornFx` beside `DvaFx` / `TracerFx`; the owner's #150 list after playtesting: "torb refuel lava", "torb lava floor", and his
shots still on Minecraft particles). Read 2026-11-17 from the archived offline build 154088. Overwatch and Battle.net were checked
closed (`Get-Process Overwatch, Battle.net`) before the TankLib run, and `owinit.py` refuses on its own. Everything was read only,
offline, through TankLib/DataTool. The repo holds text only: masks, ramps and meshes stay in the owner's archive and reach the game
through the local asset pack. Format: [dva-effects.md](dva-effects.md) / [tracer-effects.md](tracer-effects.md). His data:
[torbjorn-map.md](torbjorn-map.md); public numbers: [torbjorn.md](torbjorn.md); method: `techniques/rendering/rendering.md`
"Effects and glows".

Confidence: **H** read in the data, **M** inferred, **L** guess. "Size" is the spawn size (a 1 m quad at size 0.5 is 0.5 m; a
ribbon's size is its width; a mesh's size scales its own extent). "Ours" marks what no data gives. LUT `XXXX rN` = the colour ramp
(the last 4 hex digits of `ramps\8...XXXX.json`) and its row. Proposed palettes are `TB_*` (stops written by
`fx_torbjorn_palettes.py` from those rows, in `work\fx_palettes.txt`: copy them as they are).

## Which effect the owner meant
- **"torb refuel lava"**: two candidates, both read here. **Overload 26F1** (the forge on his back roaring: flames, smoke and sparks
  from the two vents for the 6 s) is the ability; **the reload clips 00E2 / 029D** (a molten glow and spark burst at his hand as the
  gun is fed: the "refuel") are the only other molten look on him. Overload is the likelier meaning (M); do both.
- **"torb lava floor"**: Molten Core's pool, **2754 / 274D** (lava blob meshes and embers for 10 s) after **26FB** (it forming), plus
  the slag in flight **2509** and the burn on enemies **254F**.

## Files and scripts
- `E:\Overwatch Archive\research\torbjorn\fx\Torbjorn\<Ability>\` (`RivetGun`, `Reload`, `ForgeHammer`, `Turret`, `Overload`,
  `MoltenCore`, `Hero`): `textures\` (PNG, flipbook frames `_1.._n`), `ramps\` (float LUTs as JSON), `models\Models\<id>.00C\`
  (`.owmdl`), `manifest.json`.
- `research\torbjorn\work\`: `fx_cards_{overload,molten,rivet,turret}.txt` (one card per renderer: what this note is written from)
  and their `_compact.txt` (one line per renderer), `fx_overview.txt` (one line per effect), `fx_digest.txt`,
  `fx_summary2.txt|json`, `fx_meshes2.json` (mesh bounds), `fx_entities.txt` (his projectiles', gun's, hammer's and turret's effect
  slots), `fx_clip_effects.txt`, `fx_palettes.txt` (the `TB_*` stops), `fx_extract_log.txt` / `fx_extract_log2.txt`.
- Scripts `research\torbjorn\scripts\fx\`: Tracer's `fx_tracer_*` copied as `fx_torbjorn_*` (`fx_torbjorn_extract.py` holds his
  effect list with each one's graph node). Run: `fx_torbjorn_extract.py [Ability ...]` (kiriko-anim venv, `-I`); then under
  `py -I run.py`: `fx_torbjorn_summary2.py`, `fx_torbjorn_digest.py`, `fx_torbjorn_overview.py`, `fx_torbjorn_card.py <ids>`,
  `fx_torbjorn_palettes.py` (its `PAL` list is now his: LUTs matched by their last 4 hex digits); `fx_torbjorn_cardsc.py <cards>`
  compacts a card file. The first run (2026-11-16) stopped inside Turret; this pass re-ran Turret, Overload, MoltenCore and Hero.
- Counts: **103 effects asked for, 103 extracted (+ sub-effects 4047, 34B0, 18B8 / 18B9 / 18BA), 0 missing.** 41 draw something
  (Hero's 3 not written up); the rest are sounds only or empty (2860 / 2B1E / 2862 / 2B21 / 2B22 weapon children, 175F, 2AE1, 8475,
  516C, 1CA5, 250A).

## His look
- **One palette for both sides, a redder twin for enemies (H rows, M roles):** his fire is orange-gold to his team; most renderers
  come as a pair whose second row is red-orange (Overload 7341 r4 #FF4000 > #FFFCBC / C882 r4 #FF0000 > #FFCA00; held flames 38C6 r4
  / 491F r4; muzzle 0576 r1 / DC98 r1). The pool's lava is nearly the same both ways (FE77 r3 / E741 r3). **Only the turret's aim
  ray is team-coloured** (2D6E r2 navy-white / E3C9 r2 red-white). Draw team rows added, enemy rows added too (all fire), and smoke
  rows blended (`Fx.soft`).
- **Hardpoints (H positions from his 3P model's socket table, M roles):** **0029 / 0032** the two forge vents on his back (1.48 m
  up, 0.38 m back, 0.28 m either side: Overload's and Molten Core's flames); **0075** his forge arm (0.75 m to the side, 0.58 m up:
  the slag's muzzle, kit 16DA #382, and Molten Core's glow); **000A / 000B** his hands (the reload's glow: 000A in 1P, 000B in 3P);
  00A4 the root (sounds); the Rivet Gun's **0006** (torbjorn_weapon.glb) the muzzle (2662's lights at +0.25 m). Turret (its model
  1000C): **0005 / 0007** the barrels, **0076** the eye (aim ray), **002B** the head (damage smoke), **0051** the base (build
  sparks), **0023** the back.

## Overload (1721; 26F1 held at #81 for the 6 s, fading over its last 0.3 s: kit_overload_ult.md)
| Part | Effect | Rebuilt |
|---|---|---|
| **Vent flames** | **26F1** at **0029 and 0032** (one emitter each, C19 / C21): flame quads **1CAFA** (a flame tongue) x B08 x 1CB5D, rate **15 -> 10 /s** over the life + 6 at once, life **0.28 +- 0.06 s**, size **0.42-0.66 m**, stretched **x2 -> x4** along their path, **1.5 -> 0.5 m/s up** (dir (0, 1, 0) +- 0.17), spin 0.5-1; team **7341 r4** / enemy **C882 r4** (H). Second layer 299E1 x 1CB5D the same at **12 -> 6 /s**, team **485E r6** / enemy **313B r6** (H) | each tick at both posed vents: ~0.7 flame (`torb_flame`, `TB_OVERLOAD`, 0.5 m, stretched x3 up, 0.28 s, rising 1 m/s) + 0.5 of the second layer (`TB_OVERLOAD_B`); `Fx.attach` per Torbjörn while `LOOK_OVERLOAD` |
| Heat smoke | 377D5 (**48-frame** `smoke`) x B08 at the vents, **8 -> 3 /s** + 3, **0.36-1.67 s**, **3.6-5 m/s up**, cone 15 deg, 0.19-0.91 m, stretch **6.9** (H); 2C42 r3 cream > grey, falls | 0.3 a tick, `Fx.soft("smoke#48", TB_OVERLOAD_SMOKE)`, alpha 0.4 (ours) |
| Sparks | 1F35 (`spark`), **~5-7 /s**, 0.51-0.76 s, **0.03-0.08 m**, ~1.4 m/s, cone +-25 deg (H); 64C2 r1 amber, HDR 10 (enemy C157 r1 red) | 1 every 3 ticks per vent, `TB_OVERLOAD_SPARK` |
| Heat haze | 18F81 x 4F3, 10 /s, 0.8-1 m (H: a distortion layer) | not drawn |
| Light, body | light 2.0 -> 1.0 over its life (H); material effect C35 (his body glowing, H) | not drawn (the model's own glow pass could tint; not asked) |
| Cast / end | clips 516C (3P FF9) / 1CA5 (1P 37F2) empty; 2668 (#175) and 6FD6 (#280, Stadium) sounds only (H) | - |

## Reload ("refuel"; his clips)
| Part | Effect | Rebuilt |
|---|---|---|
| **1P** | **00E2** (clip 7D6, 2.2 s; hp 000A the gun hand, 0075 the forge arm): molten glow **1B6D6 x 5DF 0.175 m** + 9489 0.17 m + soft glow **A7B6 0.51 m**, all from **0.1 s for 1.07 s** (one each, held; **48C3 r4** rust > yellow HDR 3, A23B r1); 242 0.23 m from 0.13 s; at **0.66 s** drips **1F5B (4) x 4DB x 1F5C (4)** 32 -> 12 /s for 0.4 s, 0.045-0.09 m, 2.2-2.9 m/s, 0.32 s (A988 r5 gold); at **0.8 s** sparks 1F35 **25 /s + 5 for 0.5 s**, 2.1-4.5 m/s, 0.04-0.06 m, stretch 1.5 (96EF r0); 139C 0.075 m; light 1.0 from 0.33 to 1.27 s (H) | in his own view, scaled by the reload's length (2.0 / 1.4 s): the glow at the gun hand, the drips and sparks at those times; `TB_RELOAD_GLOW`, `TB_RELOAD_GLOW_B` |
| **3P** | **029D** (clip FED, 2.0 s; hp **000B** + (0.06, 0, 0.2)): sphere 85B x 242 0.083 m and 1565 0.083 m (tint (0, 9, 4.9): L channel-shifted) for 1.67 s from 0.13 s; burst **5DF x 18B18 0.69 m, 6 + 30 /s at 0.8-0.9 s**, 0.45 s, ~1 m/s (**B981 r4** dark red > pale yellow, HDR 3); 242 0.23 m; light 1.5 from 0.3 to 1.47 s (H). Crouched **029A** (FD5) the same plus the 1P glow set at 0.4 s (H) | others see: a small glow at his posed 000B, the 0.69 m burst at 0.8 s (x0.7 when Overloaded); `TB_RELOAD_3P` |
| Gun | **0207** (gun clip D0B, 2.2 s): 242 0.31 m from 0.83 to 1.57 s; at 2.0 s 5 sparks 122C 2-15 m/s (H) | the 0.31 m glow at the gun's 0006 (ours: hp 0011 isn't in the weapon glb) |

## Molten Core (16DA, slag 2902, pool 2903 / 16DD, burn 030C / 16DE)
| Part | Effect | Rebuilt |
|---|---|---|
| Cast | clip 2B61 (3P 60E4) / 2B4A (1P 5FD2) -> **34B0** at **0.27 s**, hp **0075**: **64 sparks 1F35 at 4-7 m/s**, 0.29-0.74 s, 0.047-0.067 m (9935 r1 / 0AD2 r1); line flare 2006C x 299E1 **2 m** x2 0.28 s (F481 r3 / 28A3 r3); hemisphere mesh **963D** (2 x 1 m) 0.9 +- 0.3, 0.19 s (D3C7 r4 / F18C r4); flare 210FD 2 m 0.15 s (FCA8 r1 / 5FD2 r1); lights 0.4 -> 3.2 for 0.47 s (H). 26E6 / 26E4 / 26DC sounds; 2ADC / 2B65 camera shakes (H) | at the cast + 0.27 s at his posed 0075: 30 sparks (ours: half), the 2 m flare, the hemisphere; `TB_SLAG_LAUNCH`, `TB_MOLTEN_SPARK` |
| **Held glow** | **26E5** (16DA #447, held for the ult; authored 3 s, looped M) at **0075 and the vents 0029 / 0032**: sparks 1F35 **8-18 /s + 5-8**, 3-4 m/s, 0.03-0.05 m stretched to x5.5, 0.29-0.74 s (570C r1 / D0EB r1); glow **174D8 0.42-0.5 m** 9 /s + 3, 0.1-0.25 s (A206 r1 / 11EC r1); flames 1CAFA x 1CB5D x B08 **22 /s**, 0.41-0.59 m, 0.22-0.34 s, stretch 0.4 -> 1 (38C6 r4 / 491F r4); flames 16 /s 0.2-0.65 m stretch 2 -> 4 (FB79 r4 / 4E14 r4); 801 glows 12-16 /s 0.13-0.65 m 0.15-0.27 s (6231 r5 / F412 r5); line flare 2006C 0.84-1 m 16 /s 0.33 s at 0075 + 0.16 m (52FA r3 / D974 r3); drip blobs mesh **E20D** x B0A (one each, 3 s, size 0.064: F470 r6); dots 568C; light 2.0 at 0075 (H) | `Fx.attach` per Torbjörn while `LOOK_MOLTEN`: at 0075 a 0.45 m glow 9 /s, flames 22 /s (a third: ours), sparks 12 /s; at each vent the Overload flame at half rate (ours); `TB_MOLTEN_GLOW`, `TB_MOLTEN_FLAME`, `TB_MOLTEN_SPARK` |
| Launch | **26CC** (2902 `m_A18274A3`, at 0075): flare **1CB5D x B08 x 1CAFA 0.7-1.0 m stretched x4**, 0.18-0.21 s, 2 m/s (059C r5 / 0154 r5); glow 801 x 4C09 0.26-0.78 m 0.15-0.27 s (7624 r5 / A871 r5); sub-effect 4047 sound (H) | a flare along the shot at the slag's spawn; `TB_SLAG_LAUNCH` |
| **The slag in flight** | **2509** (2902's child 2904 STUEffectComponent): glow **174D8 1.0 m**, 3 at once + 1.5 /s, living 3 s (970A r2 tint (4.7, 0.37, 0.06) / 9B7A r2 (4.7, 1.55, 0.06)); blob mesh **E20D** x B0A x 4D4 (0.38 m mesh at size 0.15: L the slag's core, sizes may be scaled by the projectile); **drips 1F35 120-160 /s**, 0.1-0.18 m, 0.35-0.64 s, ~1 m/s falling (51C6 r0 orange); glows **A7B6 30 /s 0.26-0.99 m, 0.19 s** (FC52 r4 / 96AA r4); **4 ribbons mesh E71, ~1 m wide, 62 points /s, 0.26 s** (299E2 x 299E9 x 18B18 x B08; D35B r6 dark red > cream HDR 10, the fourth 09F8 r6) (H; ribbon width M) | per slag each frame (ProjectileModelRenderer, as `DvaFx.missileFlight`): one 0.6 m glow on it (ours smaller than 1 m: the slag is 0.2 m), drips 2 a tick (ours: a sixth), an A7B6 glow a tick, one `Fx.trail` 0.5 m wide, 0.26 s (`torb_ribbon_noise`, `TB_SLAG_TRAIL`); replaces `SlagEntity.tick`'s `FLAME` |
| Hit | **26B7** (2902 `m_BF7914D4`, 0.67 s): flash **2A81E** 0.3-0.8 m (data-flow size) 0.06-0.17 s x2; **15-17 sparks 1F35 at up to 4 m/s, 1-1.5 s**, 0.1-0.16 m, spin, cone 56-90 deg (H; its rows CEEB / E4B0 are channel-shifted: use `TB_RIVET_SPARK`, M) | the flash and 8 sparks where it lands |
| **Pool forming** | **26FB** (16DD #140, the 0.5 s wait, at the pool's 00A4): flash 1F35 **1.6 m** 0.175 s (C82B r1); dome mesh **DAF** (1 x 0.95 m) 0.57 m x2, 0.58 s (A71D r3 / F1FB r3); fire **1B709 x 4DB 1.5-1.73 m, 0.63-0.66 s, 1 m/s up** (FA25 r1 / 9EDF r1); flash **1B97A x 1B97B 1.66-1.96 m** (8FFB r3 / 20B7 r3); **15-17 sparks** 0.1-0.16 m, 0.78-1.15 s, cone 56-90 deg (AA7E r1 / 91D6 r1); 8 flashes 18B18 0.3-0.8 m 0.06-0.11 s (H) | at `pool()`: the dome, the 1.6 m fire puff (`TB_POOL_FIRE`), the 1.8 m flash, 10 sparks (ours); replaces the forming `SMOKE` |
| **The pool (the lava floor)** | **2754** / **274D** (16DD #203 / #206, the world pool; floor / wall / ceiling by the surface's normal, M) and **26FD / 266A** (#200 / #201, the pool on a moving host, M), all **10.5 s**: **lava blob meshes 83F7 (1.52 x 1.32 x 0.81 m), 885C (1.26 x 1.24 x 0.73), 887F (1.11 x 1.13 x 0.84)**, thin axis = the surface normal, size **0.65** (0.3-0.43 in 266A), one each per emitter, placed by a data-flow shape (ESGD: count and spread not decoded, L the 2.4 m disc); masks **561FC (a cracked crust)**, 2C977, 39115, 1B2B6, 16E9D, DA5B; team **FE77 r3** pale yellow > ochre > brown (falls: hot cracks, dark crust) / enemy E741 r3 (H). **Embers 9489 x 4F3 2-3 /s for 10 s**, 0.044 m, 0.54-1.41 s, **0.4-0.8 m/s up**, spin 1-2 (492B r0 dark red > gold); sparks 801 0.5-1 /s (0222 r0) (H). No decal: the blobs are the floor (H). 6F35 / 6F3C / 6F3E / 6F41 are perk 279B's (Stadium) look: not drawn | sent once from `pool()` ("torb_pool", life 10.5 s); the client keeps it (as DvaFx FIELDS) and each frame draws **7 blob meshes over the 2.4 m disc** (ours: a ring of 6 at 1.5 m + 1 in the middle, sizes 0.65 x mesh, flat, random turn), lava `TB_POOL_LAVA` on `torb_lava_crust`, plus embers 3 /s per blob (ours); fades the last 0.5 s (ours). Replaces `tickPools`' `LAVA` / `FLAME` |
| **Burning enemies** | **254F** (16DE #150, while 030C is on them, 10 s authored): sparks 1F35 **20 /s + 10**, 0.5 s, 0.025-0.05 m, stretch 2.5, all directions (05C8 r1 tint (10, 1.4, 0.34) orange); **flames 4A7E x 4C09 x 4DB 7-10 /s, 0.5-0.75 m, 0.75-1 s** (D2A3 r12 dark red > orange / 0454 r12 brown > gold, both on: two layers, M); **rings: mesh 86CF x D9B size 2 (a 2 m flat ring) 0.5 s once and 5708 x 190F7 size 2 held** (B8BD r6 / 5ECA r6 orange > cream); embers 1F35 20 /s + 25, 0.017-0.032 m, 0.5-1.5 s (A8A6 r0); 2 lights 3.0 (H; where on the body: 00A4 / unnamed, M at their feet). 27EF: material effect (body glow) not drawn; 2706 / 27B8 sounds | each burned enemy each tick: a flame (0.6 m, `TB_BURN`) every other tick and one spark; the 2 m ring at their feet while burning (`torb_burn_ring`, `TB_BURN_RING`); new (today nothing marks them) |

## Rivet Gun (1683; rivet 2A66 70 m/s, pellet 13ED 120 m/s, 10 a shot)
| Part | Effect | Rebuilt |
|---|---|---|
| **Rivet muzzle** | **2651** (2A66 `m_A18274A3`, at the launch: the gun's 0006, M): star **191DD (4) x 4DB 0.23-0.27 m and 0.13-0.17 m, 0.18 s** (F3DF r2 cream / C2F1 r2); flash **1924B (4) x 18F00 0.24-0.28 m, 0.135-0.165 s** (**0576 r1** brown > cream / DC98 r1); streak **1917E (4) 0.96 m stretched x4**, 0.15 s, 1 m/s (87EF r1 / 3B5E r1); smoke **19271 x B08 0.17-0.22 m stretch 4.6, 0.27 s**, 1.3 m/s, cone 16-24 deg (260E r4 greys) x2; sub-effect 4047 sound (H) | at the posed muzzle (a third in his own view, as Tracer's): star, flash, streak, one smoke; `TB_MUZZLE`, `TB_MUZZLE_B`, `TB_MUZZLE_SMOKE`; new (none today) |
| **The rivet** | **2653** (2A66's child 2A68): glowing slug mesh **83D6 (0.45 m long, tail along -Y) at size 1.25 and 1.0**, held for its flight (89BD r2 amber > yellow); **ribbon 797 0.2 m wide, 64 points /s, 0.25 s** (2A480 x 2A063; D950 r4 rust > gold HDR 11); thin ribbon 7F5 x 192B0 0.28 m, 0.15 s (CE8C r3); sparks shed 0.03-0.27 s after launch, 11 -> 83 -> 40 /s, 2.2 m/s, 0.21-0.39 s (9935 r1; enemy F9A4 r1 red) (H) | per rivet each frame: the slug mesh along its velocity (`torb_rivet`, `TB_RIVET`), `Fx.trail` 0.2 m / 0.25 s (`torb_trail`, `TB_RIVET_TRAIL`), sparks its first 0.25 s; replaces the placeholder item's look (OvercraftClient `torbjorn_rivet` stays as the model or goes, M) |
| **Rivet hit** | **2650** (both `m_AED73588` creature and `m_BF7914D4` world): sparks 1F35 **5-8** at ~6 m/s, 0.29-0.74 s, 0.047-0.067 m stretched x5 (9935 r1); flash **4C09 0.27-0.33 m, 0.08 s** (6B5C r1); 242 0.74 m x2 0.11 s (0BC8 r2 tint (30, 0, 0): L channel-shifted, draw as `TB_RIVET_HIT`); **10 sparks 122C at 15-17 m/s**, 0.54-0.94 s, stretch 4, cone 9 deg (07E0 r2 / 5AB6 r0) (H) | the flash, 6 sparks, 5 fast streaks back along the normal (ours: off the surface); replaces `CRIT` (RivetEntity:90) and `SMALL_FLAME` (:100) |
| **Pellet muzzle** | **2662** (13ED `m_A18274A3`, hp **0006**): stars 191DD x 4DB **0.65-0.99 m 0.14 s** and 0.48-0.83 m 0.11 s (F8D0 / 983A r1: channel-shifted, M); flash 1924B x 18F00 **0.38-0.42 m 0.106 s** (D3DC r1 / 21B8 r1); 3 streaks 1917E 0.57-0.77 m x4, 0.07-0.1 s (DBF4 r1 / 4967 r0); **2 lights 2-2.5 m at 0006 + 0.25 m, 0.17 s** (H) | once a volley (it is per pellet: 10 stacked, ours once); `TB_PELLET_MUZZLE`, `TB_MUZZLE_B` |
| **A pellet** | **1865** (13ED's child 1413): slug mesh **80BE (1.04 m, tail -0.71) at 0.72 +- 0.16** and 0.18-0.48 (D52D r2 amber > yellow); ribbons 7F5 x 192B0 **0.24 m, 0.17 s** and 0.15 m 0.09 s (66D1 r3 / 8E1F r3) (H) | per pellet: the slug at size 0.5 (ours: ten at 0.75 m read as a wall), `Fx.trail` 0.12 m / 0.17 s; `TB_PELLET`, `TB_PELLET_TRAIL` |
| Pellet hits | creature **05AF** (0.33 s): puff 44A8F (4) x 1CB5D x 299E3 ~0.2 m (DF03 r7 orange > peach); flat shard mesh **EF7** (1.0 x 0.31 m) at 0.1-0.22 x 2, 0.03-0.17 s (5B70 (4), 02A2 r1); smoke 3F25F x 44A8F 0.25-0.29 m 0.1-0.9 s cone 30 deg (1671 r10). World **265B**: 2-3 sparks 1F35 (9935 r1), 0.29-0.74 s (H) | creature: the puff + 2 shards; world: 2 sparks; `TB_PELLET_HIT`, `TB_RIVET_SPARK`; replaces the same two vanilla calls |
| Gun's own | 2860 / 2B1E (gun children) empty; 0DB1 / 76EF sounds; clip 1B26 a camera shake (H) | - |

## Deploy Turret (16C5 / 1688 / 1689; thrown 2931, deployed 28B7, shot 2A69)
| Part | Effect | Rebuilt |
|---|---|---|
| Thrown | **272D** (2931's child 2B41): ribbon 99F x 1298 **0.2 m wide, 62 points /s, 0.2 s** (D37C r1 cream > gold) (H) | `Fx.trail` 0.2 m / 0.2 s behind the flying turret; `TB_THROWN` |
| Building | clips **E41 (0239, 2.47 s)**, **E45 (023A, 1.73 s)**, **E46 upgrade (023B, 1.7 s)**: spark bursts 1F35 **10-17 at ~6 m/s**, 0.41-0.8 s, cone +-15 deg (E029 r1) at **0051** / **002B** / **0005 / 0007** at 0.9 / 1.9 / 2.17 s (E41), 0.13 / 0.57 / 0.73 s (E45), 0.27 / 0.47 / 0.63 s (E46); smoke 377D5 x B08 **10 /s + 10, 1.0-1.35 m**, 0.84-1.04 s, 1.5-1.7 m/s, stretch 4.6, cone 50 deg at 0051 from 0.73-1.03 s (4C98 r3) (H). Which clip plays when (build 5F79 = 2AB5 sounds only): L | at the build's and the upgrade's times: the spark bursts at the posed points, the smoke puffs (ours: a third) |
| Level poses | clips EAC / EAE / EB5 (base, l2, l3) -> **18B9 / 18B8 / 18BA**: flare DA5B stretched x7.5 **0.53 m** 0.25 s (C71F r2 orange HDR 28; l3 also 0.78-0.99 m, 4FD0); 139C 0.25 m 0.15 s; **50 sparks 4F3 at 5-12 m/s** (l3 8.8-15.8), 0.2-0.5 s, 0.01-0.02 m (F177 r0); 5 smoke 377D5 1.5-1.9 m 1-1.4 s (4C98) (H parts; L when: each level's arrival) | at each level change (deploy, upgrade): one burst at 0051 |
| **A shot** | **2654** (2A69's STUEffectComponent, 0.33 s; at the gun 0005 / 0007, M): **tracer quad 801, masks 510 x 50C (the `sombra_tracer_line` / `_edge` pair), 0.15 s, gun to hit (ECIN)**, tint D763 r4 **(10, 5.4, 0) orange HDR**; flash **4C09 0.58-0.93 m 0.1 s** (41B1 r0); flash 18F00 x 1917E (4) 1.0-1.47 m 0.1-0.14 s; flash 1924B 0.77-1.08 m 0.1-0.12 s at +0.12 m; **smoke 377D5 x B08 0.59-0.79 m, 0.32-0.48 s, ~6.8 m/s, stretch 4.5 -> 10** (3133 r4 greys); fire B08 x 3AF6 0.4-0.6 m 0.37-0.53 s (E076 r1); light 2.0 0.33 s (H; the line's width isn't in the data) | at the turret's barrel each shot: the line 0.04 m wide (ours), 0.15 s (`TB_TURRET_FIRE` HDR orange), the 0.75 m flash (`TB_TURRET_FLASH`), one smoke streak (`TB_TURRET_SMOKE`); replaces TurretEntity:345's `ELECTRIC_SPARK` / `SMOKE` dots. Its hit on the world: **265B** (2-3 sparks, as the pellet's) |
| Aim ray | **050A** (1688 #292, the StateTrackRay: from its eye **0076** to the target's 0001, while it holds a target, 0.4 s re-sent): ribbons 797 **0.45 m** (2662 x 193BE; **2D6E r2 navy > white / E3C9 r2 dark red > white**, HDR 10) and 0.65 m (C495 x 2662; 65CF / 9369); flare 20065 1.5 m 2 /s (BEF0 / A5AE r0) (H; ribbon widths as sizes, M) | a thin `Fx.trail` / line eye -> target while it has one (ours 0.05 m, alpha 0.5: the data's 0.45 m reads as a slab), the team row to its side; needs the target synced (TurretEntity has none today: L) |
| Damaged | **03A3** (1688 #218, when health / max < 0.5): smoke 377D5 x B08 **15 /s + 5, 1.0-1.2 s, 1.5-1.7 m/s up, 0.41-0.75 m**, stretch 4.6 (4C98 r3 cream > grey); 30 sparks 122C at 002B, 2-8 m/s once (H) | 1 smoke puff every 2 ticks at its posed 002B while under half health (client: its health); `TB_TURRET_DAMAGED` |
| Destroyed | **not found**: 0228 (#841) / 0229 (#91) / 0230 sounds only, 0519 a material effect (H that none draws) | keep today's `EXPLOSION` (Torbjorn:453) or the level burst 18BA as its stand-in (ours) |
| Others | 018C / 018B / 0225 / 03A4 / 03C7-03CB / 0240 / 0227 / 7086 / 0221 / 26ED / 6538 / 2AB5: sounds only or empty (H) | - |

## Forge Hammer (not asked; for completeness)
- **65A7** (168C #895 / #966, hp **0006** = the hammer's head: its hit, M): sparks 1F35 3-5 at 6-8 and 11.6-13.7 m/s, 0.08-0.18 s,
  0.09-0.11 m (254A r1); flashes 1924B 0.78-1.04 / 1.31-1.57 m and 174D8 0.81-1.16 / 1.78-2.13 m, 0.1-0.15 s (5D1B r3, 9928 r0);
  plus-mesh **E0AA** (`ana_plus`, 0.07 m) at 1.8 x2, 2.4-3 m/s; light 3.0 0.2 s (H). Hook: `Torbjorn.hammerHit` (today nothing).
  `TB_HAMMER_SPARK`, `TB_HAMMER_FLASH`. Swing clips 0219 / 02A9 / 1F39: sounds only.

## Where it hooks in (each vanilla call it replaces)
| Today (`src/main/java/...`) | Becomes |
|---|---|
| `entity/RivetEntity.java:90` `CRIT` x2 on a creature | `Effects.play(level, pellet ? "torb_pellet_hit" : "torb_rivet_hit", at, normal, ...)` (2650 / 05AF) |
| `entity/RivetEntity.java:100` `SMALL_FLAME` on a block | `"torb_rivet_hit"` (2650) / `"torb_pellet_world"` (265B) with the hit face's normal |
| `entity/SlagEntity.java:68` `FLAME` every other tick | nothing on the server: `TorbjornFx.slagFlight` from `ProjectileModelRenderer` (case `SlagEntity`, as DvaMissileEntity at :133) (2509) |
| `torbjorn/Torbjorn.java:683-685` pool `SMOKE` / `LAVA` / `FLAME` every 4 ticks | `pool()` (:666) sends `"torb_pool"` once (26FB at once, then 2754 for 10.5 s drawn by the client); the burn loop sends `"torb_burn"` per enemy burned (254F), the client holding it 0.3 s past the last |
| `torbjorn/Torbjorn.java:453` `EXPLOSION` (turret gone) | stays (no data visual) or the 18BA burst (ours) |
| `entity/TurretEntity.java:345` the `ELECTRIC_SPARK` / `SMOKE` dotted line | `"torb_turret_shot"` gun -> hit (2654), variant = barrel (0005 / 0007) |
| none: rivet / pellet launch (`Torbjorn.weapon` :175-192) | `"torb_muzzle"` (2651) / `"torb_alt_muzzle"` (2662) at the muzzle, entity = him (the client poses his weapon's 0006) |
| none: rivet / pellet flight | `TorbjornFx.rivetFlight` from `ProjectileModelRenderer` (case `RivetEntity`, `pellet()` picks 2653 / 1865) |
| none: Overload (`startOverload` :501) | client `TorbjornFx.frame()`: every Torbjörn whose `TORBJORN_LOOK` has `LOOK_OVERLOAD` (already synced to all), at his posed 0029 / 0032 (26F1); the last 0.3 s fade needs the end time (ours: fade on the bit's drop over 0.3 s) |
| none: Molten Core held (`ult` :594) / cast (`castUlt` :554) | the same `frame()` on `LOOK_MOLTEN` (26E5 at 0075 / 0029 / 0032); `"torb_ult_cast"` at the cast + 0.27 s (34B0); `"torb_slag_launch"` at `ult` :612-617 (26CC) |
| none: reload (`startReload` :229) | `"torb_reload"` (variant = length in ticks: 40 or 28 with Overload, 20 with the Specialist boost): 029D at his posed 000B for others, 00E2 in his own view |
| none: turret thrown / build / damaged / aim | client side off the turret entity: trail while thrown (272D), clip times while building (0239 / 023A / 023B), smoke under half health (03A3); the aim ray (050A) needs its target synced |
- Hardpoints to pose for him (HeroModelRenderer's `POINTS`, as `SojournFx.POINTS`): `hardpoint_0029`, `hardpoint_0032`,
  `hardpoint_0075`, `hardpoint_000A`, `hardpoint_000B`; the weapon glb's `hardpoint_0006`; the turret's `hardpoint_0005`,
  `hardpoint_0007`, `hardpoint_0076`, `hardpoint_002B`, `hardpoint_0051`.
- Palettes: his ~60 `TB_*` arrays in a `TorbjornPalettes.java` (as `SojournPalettes`: the enum outgrew its class file), the
  `EffectArt.Palette` entries pointing at them; `ART` / `ART_ENEMY` lists as `DvaFx`'s.

## Not done / not found
- **The turret's destruction** has no visual effect in the data (sounds only); its level-3 / Overloaded Turret missiles (the code's
  `missile` flag) have no separate shot effect in the files read (2A69 serves both, M).
- Sizes set by data flow (ECSd / ESGD) not decoded: the slag hit's flash, the pool blobs' count and spread over the disc (L: the
  2.4 m radius), the turret ray's length input ECIM(73). Ribbon "size" read as width (M: E71 at ~1 m for the slag, 797 at 0.45 m for
  the ray look too wide for the objects; drawn smaller, ours).
- Channel-shifted LUTs (as Tracer's): 2650's 0BC8 / 07E0 (tint (30-40, 0, 0)), 26B7's C70E / FC91 / CEEB, 2662's F8D0 / 983A,
  029D's 1748: read from their twins or `TB_RIVET_HIT` / `TB_RIVET_SPARK` (M).
- Material effects (Overload's body glow C35, the burn 27EF, turret 0519) and distortion layers (18F81) aren't drawn.
- Which build clip (E41 / E45 / E46) plays at which build step, and what triggers the level clips' bursts (L).
- Hero effects 01E4 (fall), 03B2 / 042B (02E7 / 0325 children at 0029 / 0032 / 01AB: L footstep or landing puffs) read but not
  written up (not asked).

## What the builder adds to the pack (all under `E:\Overwatch Archive\research\torbjorn\fx\Torbjorn\`)
Masks (`EFFECT_MASKS`: name -> ("Torbjorn", ability folder, 12-hex id, frames)), new `torb_*`:
- Overload: `torb_flame` 1CAFA (1), `torb_noise_b08` B08 (1; the same id D.Va's note names `dva_sd_smoke` with 4F0: one name
  when both merge).
- MoltenCore: `torb_lava_crust` 561FC, `torb_lava_b` 1B2B6, `torb_pool_fire` 1B709, `torb_pool_flash` 1B97A, `torb_pool_flash_b`
  1B97B, `torb_dome_noise` 1B72B, `torb_slag_hit` 2A81E, `torb_burn_ring` 190F7, `torb_noise_b0a` B0A, `torb_noise_4d6` 4D6,
  `torb_ribbon_noise` 299E2 (all 1 frame).
- RivetGun: `torb_star` 191DD (4), `torb_flash` 1924B (4), `torb_smoke_puff` 19271, `torb_ribbon` 192B0, `torb_trail` 2A480,
  `torb_hit_puff` 44A8F (4), `torb_hit_smoke` 3F25F, `torb_hit_shard` 5B70 (4).
- Reload: `torb_reload_glow` 1B6D6, `torb_drip` 1F5B (4), `torb_drip_b` 1F5C (4), `torb_dot` 1565.
- Turret: `torb_turret_fire` 3AF6, `torb_ray` 2662, `torb_ray_edge` 193BE, `torb_glare` 098D.
Reused (same ids already in EFFECT_MASKS): `spark` 1F35 (4), `smoke` 377D5 (48), `glow` A7B6, `mercy_web` 1CB5D, `cat_noise` 299E1,
`lucio_soft` 4F3, `brigitte_soft_blob_b` 174D8, `sigma_disc` 9489, `brigitte_cloud_a` 2C977, `cat_soft_ball` 39115, `cat_cone_line`
16E9D, `cat_flare` DA5B, `juno_soft_dot` 568C, `sigma_lens_line` 2006C, `lucio_glow` 210FD, `juno_cast_fade` D9B, `cat_cloud` 18E48,
`mercy_noise` 4DB, `mercy_noise_b` 4A7E, `cat_soft` 4C09, `brigitte_noise_fine` 4D4, `juno_disc_noise` 299DF, `dva_solid` 1298,
`juno_cloud_noise` 299E9, `sparkle` 3DC7, `cat_puff` 18B18, `cat_burst` 1917E (4), `cat_soft_noise` 18F00, `ana_fade_v` 2A063,
`mercy_glow` 242, `lw_disc` 122C, `cat_ring_line` 299E3, `juno_noise_c` 5DF, `mercy_soft` 139C, `sombra_tracer_line` 510,
`sombra_tracer_edge` 50C, `glint` 20065, `juno_ray_ground` C495.
Meshes (`EFFECT_MODELS`: name -> ("Torbjorn", ability, 12-hex id); `models\Models\<id>.00C\<id>.owmdl`): RivetGun `torb_rivet` 83D6,
`torb_pellet` 80BE, `torb_hit_shard_mesh` 0EF7; MoltenCore `torb_lava_a` 887F, `torb_lava_b_mesh` 83F7, `torb_lava_c` 885C,
`torb_slag_blob` E20D, `torb_burn_ring_mesh` 86CF, `torb_burn_ring_b` 5708, `torb_dome` 0DAF, `torb_hemisphere` 963D. Reused:
`ana_plus` E0AA. Quads 0801 / 0F5B and ribbon meshes 0797 / 07F5 / 0E71 / 099F are not needed.
Ramps (`ramps\8...<id>.json`; the `TB_*` stops in `work\fx_palettes.txt`): Overload 7341 r4 / C882 r4, 485E r6 / 313B r6, 2C42 r3,
64C2 r1; MoltenCore A206 r1 / 11EC r1, 38C6 r4 / 491F r4, 570C r1 / D0EB r1, 059C r5 / 0154 r5, FC52 r4 / 96AA r4, D35B r6 / 09F8
r6, 51C6 r0, FA25 r1 / 9EDF r1, 8FFB r3 / 20B7 r3, FE77 r3 / E741 r3, 492B r0, D2A3 r12, 0454 r12, B8BD r6; RivetGun 89BD r2, D950
r4, CE8C r3, 0576 r1 / DC98 r1, F3DF r2 / C2F1 r2, 260E r4, 9935 r1, 6B5C r1, D52D r2, 66D1 r3 / 8E1F r3, D3DC r1 / 21B8 r1, DF03
r7; Reload 48C3 r4, A23B r1, B981 r4; Turret 41B1 r0, E076 r1, 3133 r4, 2D6E r2 / E3C9 r2, 4C98 r3, D37C r1; ForgeHammer 254A r1,
5D1B r3.
