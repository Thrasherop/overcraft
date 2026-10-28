# Jetpack Cat's ability effects in the game data

What Jetpack Cat's effects are made of in real Overwatch, where the extracted files are, and how to rebuild each one
in Minecraft. Read 2026-10-28 from the archived offline build 154088 (prometheus-2_25_0_0-154088). Overwatch and
Battle.net were checked closed before every session; everything was read only, offline, through TankLib/DataTool.
Text only: the textures and meshes are Overwatch assets and stay in the owner's archive. They reach the game only
through the local asset pack. The general format is the one `kiriko-effects.md` and `lucio-effects.md` describe; this
file adds what is different for her. Method and format notes: `E:\Overwatch Archive\research\jetpack_cat\effects-NOTES.md`.

Confidence after each number or claim: **H** read in the data, **M** inferred (one link guessed), **L** guess.
"Size" is the spawn size value: it scales the mesh (a 1 m quad at size 0.5 is 0.5 m). A range "a-b" is a random value
between the two (Kiriko's `mode 5` reading, checked here: the other reading gives negative sizes and lifetimes).

## Files

`E:\Overwatch Archive\research\jetpack_cat\fx\JetpackCat\<Ability>\`, with `<Ability>` one of `Pawjectiles`, `Purr`,
`Lifeline`, `FreneticFlight` (also holds the jetpack's idle visual) or `Catnapper`:
- `textures\<id>.png` (flipbooks split `<id>_1.png ...`), `ramps\<id>.json` (float LUTs, `[row][x][r,g,b,a]`),
  `models\Models\<id>.00C\` (effect meshes; the Lifeline hook 1BAD1 with its look 25B94), `manifest.json`.
- The folder has the same layout as `extracted\154088\Effects\Lucio`. **It is not under `extracted\` yet**: this pass
  could only write under `research\jetpack_cat\`. Copy `fx\JetpackCat` to `extracted\154088\Effects\JetpackCat` (or
  point `build_asset_pack.py effects` at `research\jetpack_cat\fx`) before using the mask lines at the end.
- Research files in `E:\Overwatch Archive\research\jetpack_cat\work\`: `fx_digest.txt` (every renderer with its emitters
  resolved: the file to read for any number below), `fx_summary2.txt`, `fx_meshes2.json`, `fx_palettes.txt`,
  `sheets\tex_<Ability>.png` (every mask), `sheets\meshes_<n>.png` (mesh wireframes). Scripts in `..\scripts\` (`fx_*.py`).

Counts: 75 effects extracted (33 with visuals, the rest sound only), 309 distinct textures (373 entries across the
five folders, 256 of them float LUTs), 30 distinct meshes. Nothing failed.

## What's new compared with Kiriko and Lúcio (format)

- **Her colour is gold plus blue/cyan, against red/orange for enemies (H colours, M roles).** Every coloured part has
  a 65BC float LUT with one friendly row and one red/orange row, as Kiriko's. The friendly row is gold-to-white for
  cores, heals and flames, blue-to-cyan for halos, rings, ropes and trails. There is no separate "own" row. A few parts
  have no red row (her paw-shot second core, Purr's range wall, the Lifeline/Catnapper arcs, the Catnapper marker):
  likely seen only by her side (L). The 12BB LUTs are over-life curves, not colours.
- **Emitters feed renderers by particle-system index, not by order (H).** Ribbons come before their emitter; one
  renderer often has one emitter per hardpoint (left and right nozzle). `fx_digest.txt` pairs them correctly.
- **`ECSb` value nodes** (new): `op(a, ..., z)` where the first arguments are 0 (or -0.0045) and the last is the real
  value, e.g. a size `(0, 0, 0.0836)`, a rate `(0, 20)`. The last value is used below (M: a quality switch).
- **Per-emitter offsets in the hardpoint table (H).** ESHT entries now carry a position, a rotation and a scale per
  emitter. That is how the paw print is laid out (Pawjectiles) and how the flames sit in the nozzles.
- **Decal sizes read (M):** three constants per decal, x and y extent and projection depth (Kiriko's NOTES had none).
- **Beams (H):** Lifeline's rope and Catnapper's paw lines are ribbons of exactly two points (ends 0 and 1) drawn by a
  beam node between a bone or hardpoint and a target. No sag is in the effect.
- **Jetpack hardpoints (H ids, L bones):** 0075 / 0077 (the two flame nozzles: every flame, glow and spark of the
  jetpack) and 0078 / 007A (the boost-trail and light points). They are not in her model's hardpoint table; the bone
  isn't resolved. Her model's 0433 / 044A / 0452 (behind the pelvis, `jetpack_cat-map.md`) are the candidates (L).
  Other generic hardpoints, by Lúcio's model: 0001 chest (Spine_3), 0004 root, 000A / 000B weapon R / L (her paws, M),
  0012 head, 00A4 chest.

### Meshes

| Id | Shape (model units, x size) | Used for |
|---|---|---|
| 0F5B / 0801 / 10F4E | 1 x 1 m quad | glows, sparks, flares, the paw-print quads |
| 31AF | tube ribbon, r 0.5 | paw-shot trails, Catnapper dive trails |
| 099F / 075D / 0797 | flat / 8-sided / single strip ribbon | shot trails / Frenetic Flight / Catnapper paw lines |
| 1385 / DBF / 1B761 / 085B | sphere r 0.5 | Purr spheres, Catnapper impact spheres |
| 507F / C637 / 1B2FF / 963D | hemisphere / dome | nozzle glows, Catnapper slash domes |
| 1B580 | open tube r 0.064, 0.17 long (UV scrolled) | jetpack flames |
| 19370 | flared bell r 1, 1.3 long | exhaust bells |
| 1C4C5 / 1BD54 / 1E7F | flat ring r 0.13 / torus ring r 0.19-0.71 / flat annulus r 0.42-0.64 | exhaust rings, Purr rings, wall-hit ring |
| 15A26 | fat torus, middle r 1.31, outer r 1.73, tube r 0.42 | muzzle rings, Catnapper marker, Lifeline pulses |
| 19181 / DE0 | open cylinder r 0.49, 1 tall | Purr range wall / Catnapper puff |
| 1B972 / 1F85 | cone r 0.12, 0.24 long / truncated cone | Lifeline glows / heat haze |
| 152B9 | curved strip 0.2 m (an arc) | Catnapper claw slashes |
| 14874 | flat half-ring band r 0.35-0.5 | Lifeline / Catnapper latch arcs |
| 5C75 / 8C7 | flat octagon frame / square frame | hex shield quad / healed-ally ring |

## Colours

sRGB hex from the LUT rows (linear, converted), dark (mask 0) to bright (mask 1). The `EffectArt` palettes are at the end.

| Name | Where | Hex | Conf |
|---|---|---|---|
| **Gold core** | paw/centre shot core 800DA2FE r1 | #FFCA00 #FFDB00 #FFF200 #FFFFA4 #FFFFFF | H |
| Gold tracer | paw shot tube trail 80067279 r5 (HDR 4.7) | #FFC100 #FFDC54 #FFFFA7 #FFFFF4 #FFFFFF | H |
| **Blue halo** | shot outer glow 8006FF69 r1 | #002FFF #0082FF #00C0FF #00EEFF #00FFFF | H |
| Spark | shot sparks 800D7AF2 r1 | #00E6FF #00E6FF #A8FFFF #FFFFFF #FFFF00 | H |
| Blue ribbon | shot flat trail 8004C26D r8 | #0049FF #0079FF #00CDF7 #00FFE8 #00FFC4 | H |
| Hit gold / hit blue | target hit 709F, from black | #000000 #BA8D00 #FDC100 #FFFF5C #FFFFA0 / #000000 #0002BA #0004FD #0088FF #00BAFF | H |
| **Heal gold** | heal hit 7075, Purr on allies 741D | #FFD534 #FFFFA4 #FFFFDB white | H |
| **Damage violet** | enemy hit 7077, Purr's hit 741C | #8334FF #7E69FF #7696FF #66C8FF #F2FFFF white | H, M role |
| Purr blue | 10 m sphere 800E8940 r6 (HDR 6.9) | #0015FF #00ACFF #00FFFF #00FFFF white | H |
| Purr gold | flash quads, nozzle glows, pulse spheres | #000000 #D5BF00 #FFEC55 #FFFFF7 white | H |
| Rope cyan | Lifeline rope core 800367D1 r2 (HDR 16) | #003EFF #00E9FF #00FFFF ... | H |
| Flame | jetpack flame core 8004B7CE r7 (HDR 11) | #0056CB #66FFFF #D8FFFF white | H |
| Boost cyan | Frenetic Flight ribbons 80048E45 r3 | white head, then #00F7FF | H |
| **Enemy** | the twin rows | red #FF0000 to white (cores), to orange #FFBE00 (blue parts), to #FF9F00 (trails), to yellow (rope) | H colour, M role |

## Biotic Pawjectiles

| Part | Effect | Conf |
|---|---|---|
| Paw shot in flight (6D19 -> child 6D1E) | 707A, 1 s loop | H |
| Centre shot in flight (6D1B -> child 6D1D) | 7079, 1 s loop | H |
| Muzzle flash | **728B** (paw) / **728C** (centre): the projectiles' spawn slot m_BAB9824B, 0.25-0.3 s at the origin | H slot, M that it's the muzzle |
| Weapon graph 5ADB | no visual. Shot 7847, per-paw sounds 7175 / 7176 (their hardpoints 0005 / 0007), reload 292F | H |
| Hit on a target (centre only) | **709F**, 2 s: the paw-print stamp | H slot |
| Centre, second impact slot m_AED73588 | **7077**, 1 s: blue/violet flash. Same parts and colours as Purr's enemy hit 741C | H, M: enemy hit |
| Centre, slot m_707DF34C | **7075**, 3 s: gold flash and ring. Same parts as Purr's healed-ally 741D | H, M: ally (heal) hit |
| Wall / world (paw and centre) | **7084**, 0.67 s | H |
| Paw shot on a target | nothing of its own (its target slot is empty) | H |
| Fly-by | 73A4: sound only | H |
| Ally being healed | 2074 held on the ally's chest 0001 by graph 5ADC (also used by Kiriko's graphs: the shared healing look) | H |

**Paw shot in flight, 707A** (no hardpoint: on the projectile):
- **Core:** two camera quads (F5B, mask 4C09 soft dot) at size 0.157 m, one each, held. Gold core (CAT_CORE) / red to
  white. The second has no red row (L: own view) (H).
- **Halo:** one quad 0.233 m, mask 4C09, blue to cyan (CAT_HALO) / red to orange (H).
- **Tube trail:** ribbon 31AF, 200 points a second, each living 0.07 s, width 0.084 m. Masks 18E48 (cloud) x 7A9
  (fade strip). Gold to white-hot (CAT_TRACER) / red to white. At 60 m/s a world-space trail is 4.2 m long (H values,
  M length). Its brightness curve (over age): 0 -> 0.99 at 6% -> 0.43 at 45% -> 0.24 at the end (H).
- **Flat trail:** ribbon 099F, 200 a second, life 0.12 s, width 0.15 m. Masks 299E1 (noise) x 7A9. Blue to aqua
  (CAT_RIBBON) / red to orange. 7.2 m at 60 m/s (M).
- **Sparks:** quad 0801 with the 4-frame blob 1F35 (Kiriko's `spark`), 20 a second, life 0.055-0.118 s, size
  0.15-0.50, no speed (left behind), from a disc. Cyan-blue to white to yellow (CAT_SPARK) / red to orange (H).
- No paw-shaped sprite: the paw shot is a gold dot in a blue halo with a gold tracer (H).

**Centre shot in flight, 7079:** the same parts, bigger: core 0.34 m (gold), halo 0.49 m (blue), tube trail 0.2 m wide
(life 0.1 s), flat trail 0.35 m wide (life 0.12 s), sparks 20 a second (life 0.055-0.18 s), plus a light (intensity
1, M) (H).

**Muzzle flash, 728B / 728C:** a 0.8 m star flare (DA5B) at a random angle, life 0.25 s. Pale yellow-green to cyan
(#E0FFCA -> #1CFFFF) / red to white. 728C adds two fat rings (15A26 at size 0.4 and 0.5: about 0.5 and 0.65 m
radius), life 0.3 s, 0.25 m out along the shot axis and turned to face along it: a gold one (#FFCA00 -> #FFFF00) and a
blue one (#002FFF -> #00FFFF); enemy view orange-red (H values, M axis).

**Target hit, 709F (the paw print):** everything bursts once (H):
- **The paw print:** three nested sets of five quads (10F4E): one pad and four toes. Layout from the hardpoint table:
  pad at (0, -0.02) and 0.048 behind, toes at (+-0.02, 0.03) and (+-0.03, 0.01), toes scaled 0.4545, all turned 90
  degrees about X so they stand across the shot. Masks 4A7E (noise), D89 (streak), 18E48. Pad sizes 0.09-0.27 m
  (life 0.22 s), 0.09-0.47 m and 0.06-0.85 m (life 0.18 s). Colours: gold to white (CAT_PAW_PRINT), blue to pale
  cyan, white; enemy red to white (H for the numbers; M that the offsets are metres: they make a print about 0.1 m
  wide at the smallest pad size).
- **Flash:** eight short glows and star flares (4C09, DA5B, noise 18F00), 0.19-0.85 m, life 0.08 s, alternating gold
  (CAT_STAMP_GOLD) and blue (CAT_STAMP_BLUE).
- **Streaks:** 3 streaks (D89) of 0.57-0.67 m, life 0.12 s, 1 m/s, gold to white; 3 flame bursts (1917E, 4 frames)
  0.95-1.05 m, life 0.15 s, blue to pale cyan.
- **Two lights:** 1 s (intensity 1.17 falling to 0.14) and 2 s (0.74 falling to 0.05).

**Enemy hit (M), 7077:** 5-10 spark streaks (0801 + 1F35, size 0.12-0.18, stretched x7, M) flung 9.9-11.9 m/s in all
directions, life 0.25-0.37 s, violet-blue to white; a 2.7 m star flare (DA5B, life 0.05-0.16 s) blue to cyan to
white (CAT_FLASH_BLUE); soft glows of 1.5 m and 2.4 m (life 0.05-0.2 s), pale blue and deep blue. Enemy view red (H).

**Ally (heal) hit (M), 7075:** the same 5-10 sparks in gold (CAT_HEAL_GOLD); a flat **ring** (0801 with ring mask
3203) of 2.3-2.5 m, life 0.21-0.23 s, cyan with a gold-white edge; a soft yellow glow 0.12-1.1 m (#FFFF66 -> white); a
light rising 0.5 -> 2.5 over 0.5 s (H).

**Wall hit, 7084:** two glows (0801, mask 210FD = `lucio_glow`) 0.14-0.53 m, life 0.07-0.16 s, deep blue to teal to
pale yellow; a flat ring on the surface (1E7F, r 0.42-0.64 x 0.19-0.40, so about 0.25 m), same life, teal to cyan to
white; 4 spark streaks (183A7) 0.04-0.07 m, 2.4-2.7 m/s in a 35-degree cone, life 0.14-0.17 s, blue to teal to yellow;
a **decal** (mask 4C09) 0.05 x 0.05 (0.25 deep) for 0.67 s, gold-white or white-blue (H values; M decal reading).

**Healed ally, 2074 (shared):** golden swirl sprites (1268, a colour sprite) 1.2-1.5 m, 1 a second plus 3, life 1 s,
random angle; yellow dot sprites (126A, colour) 0.49-0.61 m, 20 a second, rising 0.07-0.42 m/s in a 30-degree cone,
life 0.6-1.3 s, slow spin; a faint ring (8C7 + 52E8) 2 m, 3 a second; a 1-1.2 m sphere, cyan to white (H).

**Recipe:**
- Paw round (60 m/s): an additive gold 0.16 m core inside a 0.23 m blue halo, camera-facing, plus a 0.08 m gold tracer
  ribbon about 0.07 s of travel long (ours: 2-4 m, fading to the tail) and a fainter 0.15 m blue ribbon behind it.
  Drop 1-2 tiny cyan-yellow sparks per tick that stay where they spawn and live 0.05-0.12 s.
- Centre round: the same at 0.34 / 0.49 m with a 0.2 m tracer.
- At the paw that fired: a 0.8 m star flare for 0.25 s; for the centre shot add two rings (0.5 and 0.65 m radius, one
  gold, one blue) 0.25 m ahead, facing along the shot, for 0.3 s.
- Centre round hitting a mob: a paw print of five quads (pad + four toes, ours: 0.25 m tall, gold core, blue rim)
  standing across the shot, shrinking out over 0.2 s; a 0.8 m blue flash. On an enemy add 5-10 violet streaks at
  10 m/s; on an ally a 2.4 m gold ring and gold sparks.
- Hitting a block: a 0.3 m flat teal ring on the face, a 0.5 m flash, 4 short sparks at 2.5 m/s, and a 0.1 m glow
  decal fading over 0.67 s.
- Ally being healed: golden swirl sprites and rising yellow dots on them while the heal lasts.
- Enemy view: the same parts in red/orange (CAT_ENEMY_HOT for gold parts, CAT_ENEMY_ORANGE for blue parts).

## Purr

| Part | Effect | Conf |
|---|---|---|
| Cast | 71D7: **sound only** | H |
| The cast burst and the 5 s aura | **70ED**, held 5 s (graph 5A7E#203) | H |
| Each enemy hit by the cast's knockback | **741C** at their chest 0001 (5A7E#189 -> per target #276 -> #278) | H |
| Each pulse | **70F3** (visual, 5A7E#184) + 71D8 (sound) | H |
| Range wall | **70F9**, held (5A7E#204): a 10 m cylinder | H values, M role |
| Each healed ally | **741D** held 3 s at their chest 0001 (graph 5C32) + 73B1 sound | H |

**Cast, 70ED** (effect length 5 s):
- **Shock sphere:** 1385 at size 16.6 (r 8.2 m), life 0.45 s, centred 1.45 m below her root. Blue-violet with a white
  rim (rim-lit noise 19167, 4DB, 299E1, strip 6C057) (H).
- **Range sphere:** 1385 at size 20.0 (**r 9.9 m: the 10 m range**), also 1.45 m down, life 0.45 s. Blue to cyan to white
  (CAT_PURR_BLUE) / red to pink (H).
- **Body sphere:** 1B761 at size 2.7 (r 1.3 m), 0.84 m above the root, life 0.6 s, blue to white / red to white (H).
- **Burst lines:** 7 flat quads (0801, mask 1D047, a vertical streak) of 1 m, life 0.14-0.20 s, 0.33 m/s, random
  angle up to 120 degrees, gold (CAT_PURR_GOLD) / red (H).
- **Nozzle glows for the 5 s:** on 0075 / 0077 (0.05 m out, turned 90 degrees): small hemispheres (r 0.06, 0.09,
  0.08 m) and a 0.17 m glow quad, gold to white; three lights (1.8, 1.8 and 5.6) (H).

**Each pulse, 70F3** (4 pulses 0.95 s apart):
- Two spheres of about 1 m (1385 at 0.955) at 0.6 m up (her body), life 0.6 s, gold (#FFD300 -> #FFF791) (H).
- A ring (1BD54) at size 1.6 (r 1.1 m), 1.13 m above the root, life 0.3 s, gold to white / orange (H).
- A **big ring** (1BD54) at size 9 (r 6.4 m), same height, life 0.33 s, blue to cyan (CAT_PURR_RING) / red (H).
- Nozzle domes (963D, r 0.16 m) on 0075 / 0077, life 0.25 s, gold (H).

**Range wall, 70F9:** an open cylinder (19181) at size 20: 9.8 m radius, 20 m tall, centred 6 m down (so from 16 m
below to 4 m above her), held, with a vertical fade strip (1C7A) and noise. Gold to white (#FFC500 -> #FFFF96 ->
white), no enemy row (H values; M that it's the range shown to her side).

**Enemy hit by the cast, 741C** (sizes scaled by a data-flow input, M: distance or damage): 5-10 violet spark streaks
at 2.5-7.2 m/s, life 0.25-0.37 s; a star flare 1.8-3.7 m, life 0.05-0.16 s, blue to cyan to white; two soft glows
1.3-2.5 m and 1.8-3.3 m (pale blue, faint violet). Enemy view red (H).

**Healed ally, 741D (3 s):** 5-10 gold sparks (1.6 m/s and up, life 0.16-0.28 s, size 0.12-0.18), CAT_HEAL_GOLD; a ring
(3203) growing 1.5 -> 2.9 m over 0.21-0.23 s, cyan with a gold-white edge; a soft yellow glow 0.9-1.5 m (H).

**Recipe:**
- Cast: at her, a translucent blue sphere flashing out to 10 m in 0.45 s (scale 0.2 -> 1, fading), a violet one to
  8 m, a 2.6 m blue-white sphere on her body for 0.6 s, and 7 short gold streaks around her. Gold glows in the two
  jetpack nozzles for the 4 s.
- Each pulse: a 1 m gold flash on her body, a gold ring 1.1 m across her middle for 0.3 s and a flat blue ring
  sweeping out to 6.4 m (ours: grow it from 1 m) for 0.33 s.
- Each ally healed: gold sparks, a 2-3 m ring opening around their chest, a yellow glow.
- Each mob knocked back: a blue-violet flash at its chest with violet streaks.
- Optional: a faint gold cylinder wall at 10 m (seen by her) while Purr runs.

## Lifeline

| Part | Effect | Conf |
|---|---|---|
| Attach | 711A: **sound only**. The visual on the ally is **7255** (5A8F#1193) | H |
| The rope | **72FA**, 0.267 s loop, beam node 5A8F#1192 from her jetpack-arm bone **3E67** to the ally's hardpoint **0004** | H |
| The hook | cosmetic entity 6EC4 -> model **1BAD1** (look 25B94) | H |
| On the towed ally | **71B1** held (5A8F#965) | H |
| On her while towing | **6EA2** held at hardpoint 0283 | H, M role |
| Ground marker | **7432** held: a paw-in-crosshair decal | H |
| Latch arcs | 7454 held (the same as Catnapper's 7486) | H, L role |
| Pulse rings | 7490 held 1.42 s at hardpoint 01DA | H, L role |
| Hex quad | 5513 held 8 s on a condition (L: the Transport Shielding perk) | H, L |
| Loops 7113 / 72E4 / 7481 / 747D | **sound only** | H |

**The rope, 72FA:** two flat ribbons (099F), each of exactly two points (one per end), rebuilt every 0.267 s (H):
- **Core:** 0.178 m wide, mask 18A1 (a cross-section strip: a bright centre line). Blue to cyan, very bright (HDR 16:
  CAT_TETHER) / red to yellow (CAT_TETHER_ENEMY).
- **Glow:** 0.324 m wide, from 0.067 s into each loop, plain white (6CE87) times scrolling noise 299E3 (M). Blue to
  cyan (HDR 3.7) / red to orange.
- **No sag in the effect:** two points make a straight band (H). The rope's physics are a separate rope node
  (#1137: 30, 0, 2, 0.05, 20, 0.1, 0.1, 10, 0.2, 0.1, as in `jetpack_cat-map.md`); whether it bends the ribbon wasn't
  decoded (L). The rope's length is the tether's 4 m (map).
- **Hook 1BAD1:** 461 vertices, 0.13 m wide, 0.14 m long, on a 10-bone copy of her jetpack arm (0001 -> Pelvis ->
  Spine -> 017A -> 0B1E -> 0B21 -> 0B24 -> **3E67** -> 13DB -> 139D). Her own geoset 4 (461 vertices on bone_139D) is
  the same piece: the clasp leaves her jetpack and rides to the ally (M).

**On the towed ally, 71B1:** a glowing cone (1B972, r 0.17 m, 0.35 m long) 3 a second, life 1 s, flipped, 0.36 m below
hardpoint 0282; bubbles (DBF spheres, r 0.88 m) 5 a second, life 0.8 s, rising 1.6-1.8 m/s from 1.28 m below 0282; a
light of 3.3. Pale blue to cyan to white / pink (H; 0282 not resolved, L: their back).

**Attach on the ally, 7255:** about 13 star sparkles (F55D, 100 a second for 0.13 s) 0.03-0.06 m on a circle, life
0.17-0.27 s, blue to cyan; a cone flash (1B972 at 3.1: 0.37 m wide, 0.73 m long) life 0.3 s, pale blue to white; a
0.57 m glow (10F4E), life 0.3 s; a light of 2.9 for 0.37 s. Enemy view pink (H).

**On her while towing, 6EA2:** a cone glow like 71B1 at hardpoint 0283; **speed lines** (0801 with the 3-frame streak
E99F, size 10 = stretched) at a rate rising 0 -> 50 a second with an input (L: her speed), life 0.45-0.55 s, 4-5 m/s,
yellow-white; tiny sparks the same way; a light of 6 (H values).

**Ground marker, 7432:** a decal with the paw-in-crosshair mask 70542 (the left half of the icon: mirror it), extent 4
x 4, projected 17 m deep from 7.5 m below. Blue to cyan (CAT_MARKER), no enemy row (H values; M: half extents, an 8 m
marker; L: it marks the ally to be grabbed or her drop point).

**Recipe:**
- While towing: a straight additive band from her jetpack (ours: 0.3 m behind and above her centre) to the ally's
  feet: a 0.18 m bright cyan core and a 0.32 m soft blue glow with scrolling noise; enemy view red-yellow. Ours: let it
  sag a little with a 3-point curve if it looks stiff, the data has none.
- A small hook model (convert 1BAD1) at the ally's end.
- On the ally: cyan bubbles rising round them and a soft cone glow; on attach, a burst of blue star sparkles and a
  flash.
- On her: yellow-white speed lines while she moves fast.

## Frenetic Flight and the jetpack

| Part | Effect | Conf |
|---|---|---|
| Jetpack, always while flying | **7064**, 1 s loop (59DC#22; also entity 7859 on her menu/showcase entities) | H |
| Boost (held while boosting) | **7067**, 1 s loop (59E0#72) | H |
| Boost start / loops / regen, up / down, hum | 73C4, 72DA, 05AC, 74BA / 74BB, 6D22: **sound only** | H |

**Jetpack idle, 7064**, on the two nozzles 0075 / 0077 (each part twice, one per nozzle) (H):
- **Flames:** the open tube 1B580 (0.13 m wide, 0.17 m long, flame noise 2C41F / 7A9 / 299E3 scrolled) in three
  layers: size 1.1, 2 a second, life 2 s (steady), blue to teal; size 0.55, 2 a second, life 2 s, blue to cyan to
  white-hot (CAT_FLAME); size 1.1, 4 a second, life 1 s, gold (CAT_FLAME_GOLD). Enemy view red to white.
- **Domes:** 963D at r 0.03-0.04 m, 5 a second, life 0.4 s, cyan-white.
- **Sparks:** 50 and 45 a second, tiny (8BC2 blobs, 0.011-0.014), life 0.07-0.25 s, thrown down from 0.13 m below
  the nozzle; gold and pale blue / red.
- **Exhaust rings:** flat rings 1C4C5 (r 0.2 m and 0.05 m), 4 a second, life 0.35 s, drifting down 0.12 m/s,
  white-hot to cyan.
- Heat-haze cones (1F85 + distortion BC5 18F81): skip. On 0078 / 007A: a 0.08 m glow and lights of 0.58 and 0.14.

**Frenetic Flight, 7067** (H):
- **Four ribbon trails from 0078 / 007A:** 8-sided strips (075D), 60 points a second: two thin ones 0.073 m wide
  (life 0.2 s) and two wide ones 0.173 m (life 0.32 s). Mask 219FA (strip) x noise 16E9D. White at the head, cyan
  #00F7FF behind (CAT_BOOST) / white then orange (CAT_BOOST_ENEMY).
- **Nozzle glows:** quads 0.64 m (life 0.25 s) and 0.28 m (life 0.4 s), 3 a second, mask A17F (a small hot dot),
  pink-violet to pale cyan (#E385C6 #EDBAFF #C4F7FF #8EFFFF, HDR 5.5) / red to white.
- **Speed lines:** E99F streaks (size 10), 50 a second plus a burst of 50 at the start, life 0.2-0.3 s, 4-5 m/s,
  yellow-white; 100 tiny sparks a second (0.02-0.05 m) around her.
- **Start puffs:** spheres (1B761) of 0.48 m and 0.22 m from 0078 / 007A, life 0.3 s, 2.5-3.8 m/s backward, blue to
  white / red.
- **Exhaust bells:** 19370 at 0.08 (0.08 m wide) on 0075 / 0077, 4 a second, life 0.5 s, white.
- Two lights of 0.45.

**Recipe:**
- Jetpack idle: two small additive flame tubes (ours: 0.13 x 0.2 m, a gold outer layer over a blue-white core, UV
  scrolled down) at her two nozzles, flickering; 1-2 tiny gold sparks per tick per nozzle falling 1 m/s; a faint
  exhaust ring every 0.25 s.
- Boost: from the nozzles, two cyan ribbon trails each (0.07 and 0.17 m wide, white at the head) about 0.2-0.3 s of
  travel long; at the start two blue puffs flung back; yellow speed lines streaming past her while it lasts.

## Catnapper

| Part | Effect | Conf |
|---|---|---|
| Aiming, on her paws | **728D** held on 000A / 000B (targeting node 5B7B#102) | H |
| Target marker | entity **6DF5** -> **71B3**, at the target point (5B7B#333, shown while a target is found) | H |
| Paw lines to the target | **71AF** from 000B and 000A to the target point (5B7B#399/#400) | H |
| Cast (start of the 0.35 s windup) | **7373** (5B7B#432) | H |
| Dive | **7163** held from the end of the windup (#318); **71B8** 0.2 s (#338) | H |
| Landing | **71C2** (#143, with the 5 m explosion) + 724C sound | H |
| Tether | **72BE** at its start (#418), **7486** from +0.4 s (#448), 739E sound loop | H |

**Aiming, 728D (2 s loop on each paw):** a 0.28 m soft glow (4C09), 1 a second, life 1.5 s; a **0.51 m four-point
star** (F7A7) whose size pulses 0.69 -> 3 -> 0.69 -> 3 -> 0.69 over each 1 s (twice a second); a light of 0.82. Blue
to cyan to white / red (H).

**Target marker, 71B3:** **fat rings** (15A26 at 1.19: the ring's middle at r 1.55 m, outer edge r 2.05 m), 3 a second,
life 1.5 s (pulsing), 0.06 m up, fade strip 1C7A x 18E48, cyan to blue (CAT_NAP_MARKER); two glow decals (4C09):
0.45 x 0.45 and 1 x 1 (2 deep), deep blue; a light of 5. No enemy row (H; M: seen by her).

**Paw lines, 71AF (3 s):** three flat ribbons (0797) of two points each, from the paw to the target: 0.01 m (cyan,
HDR 4), 0.02 m (cyan to white) and 0.045 m (strip 1344, blue to pink-violet) (H). Ours: draw while aiming at a valid
target.

**Cast, 7373:** a jetpack blast: the nozzle flames flare (1B580 at 0.55 and 1.1, life 0.13-0.2 s); two tiny exhaust
rings per nozzle (1BD54, 0.05 / 0.07) shot back at 6.9 m/s, life 0.15 s; a 1.1 bell flame 0.43 m below (H).

**Dive, 7163 (from the nozzles, 125 components):** (H)
- **Six tube trails** (31AF), 120 points a second: 0.17 m (life 0.067 s), 0.21 m (0.14 s) and 0.56 m (0.13 s), cyan,
  blue and gold with white heads / orange.
- **Four strip trails** (075D), 60 a second: 0.073 m (0.15 s) and 0.17 m (0.2 s), cyan / orange.
- Flames 1B580 (0.55, 1.1) 2 a second; exhaust rings 7 a second at 6-7 and 11-12.5 m/s.
- **Spark bursts:** 30 + 30 at the start (8BC2, 0.011-0.068 m, 4.4-13.6 m/s in a 7-degree cone, life 0.1-0.2 s) and
  144 a second after (2.6-7.4 m/s); gold to yellow-white.
- **Speed streaks:** 1.2 m quads (10F4E, with distortion), 5 a second, 10-12 m/s backward, life 0.25 s.
- **71B8 (the first 0.2 s, on 01DA):** claw-slash arcs (152B9, size 2.6-4.6: 0.5-0.9 m arcs) 100 a second; two domes
  of r 1.3 m (1B2FF at 2.58), 0.21 m ahead, life 0.2 s; a light of 3.9. Cyan (HDR 17) / red to white.

**Landing, 71C2 (5 s):** (H)
- A 5.9 m flash (4C09), life 0.25 s, violet-pink to sky to cyan (CAT_NAP_IMPACT) / red to white.
- A **21.7 m** ground flash quad (4C09 x noise), life 0.25 s, deep blue / red; a 15 m starburst (69410), life 0.2 s.
- A 1.9 m sphere (1B761), life 0.3 s, blue to white.
- **15 debris puffs:** the smoke flipbook 377D5 (48 frames, Kiriko's `smoke`) with a distortion flipbook, size
  0.61-1.12 m, life 0.62-1.3 s, 11.6-18.9 m/s in a 90-degree cone upward.
- **Two crater decals,** 5 x 5 (4.1 deep): cracks CBB9 (5 s) and CA10 (2.47 s), dark with blue and red glowing edges;
  a light of 4.8 for 2.47 s. Read as half extents that is 10 m, the 5.5 m damage radius (M).

**Tether start, 72BE (1.5 s):** spheres of 4.4 m and 6.3 m (DBF), life 0.3 s, blue to white with gold; a 10.3 m flat
disc (0801 + 299E3), life 0.25 s; 15 sparks (0.9-3.1 m/s); a 4.9 m glow for 0.65 s; a light rising 1 -> 4.8 over
0.27 s (H). **Tether hold, 7486:** two half-ring arcs (14874 at 2: 0.7-1 m) with mask 71226, life 0.35 s and 1 s,
cyan to blue-white (H). The rope itself: the rope node (#391) has Lifeline's numbers but no effect of its own (H); the
only lines in the ultimate are 71AF's (M: draw Lifeline's rope look for it).

**Recipe:**
- Aiming: a pulsing 0.5 m cyan four-point star on each paw; at the target point, cyan rings about 3-4 m across pulsing
  out 3 times a second on the ground, a blue glow disc under them, and two thin cyan lines from the paws to it.
- Cast: the nozzle flames flare white for 0.2 s.
- Dive (30 m/s): thick cyan/gold trails from the nozzles, gold sparks, backward speed streaks.
- Landing: a 6 m violet-cyan flash, a 10 m blue ground flash, 15 smoke puffs thrown up 12-19 m/s, a 10 m cracked
  crater decal fading over 5 s (ours: a dark glowing crack texture), a light.
- Tether: a blue flash sphere growing to 6 m, then Lifeline's rope from her to the captured mob for up to 4 s.

## Practical notes for Overcraft

- Pre-colour the masks with the palettes below (`rgb = palette(mask)`), additive, depth-tested, no depth writes;
  enemies' view uses the red/orange twins. Colour sprites 1268 / 126A are already coloured: draw them as they are
  (`Palette.NATURAL`).
- Skip the distortion pieces (BC5 18F81, 939, AFF, FC65, CBB8).
- Keep the timings in seconds; the held effects loop every 1 s (the rope every 0.267 s).
- Masks already in the pack: `spark` (1F35), `lucio_glow` (210FD), `smoke` (377D5).

## Not found / not certain

- Which of 7075 / 7077 is the ally and the enemy hit (by content: 7075 = heal, 7077 = damage, M).
- The bone behind hardpoints 0075 / 0077 / 0078 / 007A / 0282 / 0283 / 01DA.
- Ribbon space (trail lengths assume world space), mesh-particle orientation, size-over-life of most rings (not
  decoded: the particle programs ECGP), light colours, the rope's sag, decal half vs full extents.
- Why some parts have no enemy row; 5513 (hex quad) and 7490 (pulse rings) roles.

## Textures to add (`build_asset_pack.py`, `EFFECT_MASKS`)

After copying `research\jetpack_cat\fx\JetpackCat` to `extracted\154088\Effects\JetpackCat`:

```python
    # Jetpack Cat's (tools/reference/jetpack_cat-effects.md): soft dot, star flare, ring, spark streak, cloud and
    # noise, the 4-frame burst, fade strip, the healing swirl and dot (colour sprites), soft ring
    "cat_soft": ("JetpackCat", "Pawjectiles", "000000004C09", 1),
    "cat_flare": ("JetpackCat", "Pawjectiles", "00000000DA5B", 1),
    "cat_ring": ("JetpackCat", "Pawjectiles", "000000003203", 1),
    "cat_streak": ("JetpackCat", "Pawjectiles", "0000000183A7", 1),
    "cat_cloud": ("JetpackCat", "Pawjectiles", "000000018E48", 1),
    "cat_noise": ("JetpackCat", "Pawjectiles", "0000000299E1", 1),
    "cat_burst": ("JetpackCat", "Pawjectiles", "00000001917E", 4),
    "cat_fade": ("JetpackCat", "Pawjectiles", "0000000007A9", 1),
    "cat_heal_swirl": ("JetpackCat", "Pawjectiles", "000000001268", 1),
    "cat_heal_dot": ("JetpackCat", "Pawjectiles", "00000000126A", 1),
    "cat_halo_ring": ("JetpackCat", "Pawjectiles", "0000000052E8", 1),
    # Purr: the burst-line streak, a soft ball, the range wall's fade strip
    "cat_purr_streak": ("JetpackCat", "Purr", "00000001D047", 1),
    "cat_soft_ball": ("JetpackCat", "Purr", "000000039115", 1),
    "cat_band": ("JetpackCat", "Purr", "000000001C7A", 1),
    # Lifeline: the paw-in-crosshair marker (left half: mirror it), four-point star, hex grid, speed lines, rope core
    "cat_paw_marker": ("JetpackCat", "Lifeline", "000000070542", 1),
    "cat_star": ("JetpackCat", "Lifeline", "00000000F55D", 1),
    "cat_hex": ("JetpackCat", "Lifeline", "000000018F9B", 1),
    "cat_speed_line": ("JetpackCat", "Lifeline", "00000000E99F", 3),
    "cat_rope": ("JetpackCat", "Lifeline", "0000000018A1", 1),
    # Frenetic Flight and the jetpack: nozzle hot dot, trail strip, spark blobs
    "cat_glow_dot": ("JetpackCat", "FreneticFlight", "00000000A17F", 1),
    "cat_trail": ("JetpackCat", "FreneticFlight", "0000000219FA", 1),
    "cat_blob": ("JetpackCat", "FreneticFlight", "000000018BC2", 4),
    # Catnapper: the aim star, crater cracks (dark and glowing), the impact puff
    "cat_sparkle": ("JetpackCat", "Catnapper", "00000000F7A7", 1),
    "cat_crack": ("JetpackCat", "Catnapper", "00000006CBB9", 1),
    "cat_crack_glow": ("JetpackCat", "Catnapper", "00000006CA10", 1),
    "cat_puff": ("JetpackCat", "Catnapper", "000000018B18", 1),
```

Meshes worth converting (`EFFECT_MODELS`, optional): the hook `"cat_hook": ("JetpackCat", "Lifeline", "00000001BAD1")`
(a look-textured model; its own textures are in `models\Models\00000001BAD1.00C\Textures`).

## Palettes (`EffectArt.Palette`)

Linear, clamped at 1, (stop, r, g, b). Rows that rise from black keep the game's positions (0, .25, .5, .75, 1); rows
that start coloured get black at 0 and the game's values at .25 / .5 / .75 / 1 (the convention of Lúcio's palettes).
Generated by `research\jetpack_cat\scripts\fx_palettes.py` (`work\fx_palettes.txt`).

```java
		/** Jetpack Cat's shot core glow (707A / 7079, LUT 800DA2FE r1): gold to white. */
		CAT_CORE(0, 0, 0, 0, .25f, 1, .707f, 0, .5f, 1, .887f, 0, .75f, 1, 1, .37f, 1, 1, 1, 1),
		/** Her paw shot's tube trail (80067279 r5, HDR 4.7): gold to white-hot. */
		CAT_TRACER(0, 0, 0, 0, .25f, 1, .716f, .088f, .5f, 1, 1, .387f, .75f, 1, 1, .902f, 1, 1, 1, 1),
		/** Her shots' outer glow (8006FF69 r1): blue to cyan. */
		CAT_HALO(0, 0, 0, 0, .25f, 0, .221f, 1, .5f, 0, .526f, 1, .75f, 0, .852f, 1, 1, 0, 1, 1),
		/** Her shots' sparks (800D7AF2 r1): cyan-blue to white to yellow. */
		CAT_SPARK(0, 0, 0, 0, .25f, 0, .788f, 1, .5f, .389f, 1, 1, .75f, 1, 1, 1, 1, 1, 1, 0),
		/** Her shots' flat trail (8004C26D r8): blue to aqua. */
		CAT_RIBBON(0, 0, 0, 0, .25f, 0, .19f, 1, .5f, 0, .609f, .928f, .75f, 0, 1, .805f, 1, 0, 1, .55f),
		/** Centre-shot hit flash, gold (709F, 80031F33 r1, from black). */
		CAT_STAMP_GOLD(0, 0, 0, 0, .25f, .491f, .265f, 0, .5f, .983f, .53f, 0, .75f, 1, 1, .106f, 1, 1, 1, .352f),
		/** Centre-shot hit flash, blue (709F, 8000708B r1, from black). */
		CAT_STAMP_BLUE(0, 0, 0, 0, .25f, 0, .001f, .491f, .5f, 0, .001f, .983f, .75f, 0, .244f, 1, 1, 0, .492f, 1),
		/** The paw print (709F, 8005126D r4, HDR 3.5): gold to white. */
		CAT_PAW_PRINT(0, 0, 0, 0, .25f, 1, .735f, .109f, .5f, 1, 1, .385f, .75f, 1, 1, .82f, 1, 1, 1, 1),
		/** Enemy-hit flash (7077, 8001A030 r5, HDR 6.4): blue to cyan to white. */
		CAT_FLASH_BLUE(0, 0, 0, 0, .25f, 0, 1, 1, .5f, 0, 1, 1, .75f, 0, 1, 1, 1, 1, 1, 1),
		/** Heal sparks (7075 / Purr 741D, 800BB7A6 r4, HDR 5): gold to white. */
		CAT_HEAL_GOLD(0, 0, 0, 0, .25f, 1, 1, .371f, .5f, 1, 1, .709f, .75f, 1, 1, 1, 1, 1, 1, 1),
		/** Enemy view of her gold parts (800DA2FE r2): red to orange to white. */
		CAT_ENEMY_HOT(0, 0, 0, 0, .25f, 1, .09f, 0, .5f, 1, .208f, 0, .75f, 1, .585f, .37f, 1, 1, 1, 1),
		/** Enemy view of her blue parts (8006FF69 r2): red to orange. */
		CAT_ENEMY_ORANGE(0, 0, 0, 0, .25f, 1, .086f, 0, .5f, 1, .219f, 0, .75f, 1, .363f, 0, 1, 1, .517f, 0),
		/** Purr's 10 m sphere (70ED, 800E8940 r6, HDR 6.9): blue to cyan to white. */
		CAT_PURR_BLUE(0, 0, 0, 0, .25f, 0, 1, 1, .5f, 0, 1, 1, .75f, 1, 1, 1, 1, 1, 1, 1),
		/** Purr's burst lines and nozzle glows (70ED, 800EB0AE r1, from black): gold to white. */
		CAT_PURR_GOLD(0, 0, 0, 0, .25f, .667f, .523f, 0, .5f, 1, .835f, .091f, .75f, 1, 1, .933f, 1, 1, 1, 1),
		/** Purr's 6.4 m pulse ring (70F3, 8001990A r8): blue to cyan. */
		CAT_PURR_RING(0, 0, 0, 0, .25f, 0, .528f, .974f, .5f, 0, .772f, 1, .75f, 0, 1, 1, 1, 0, 1, 1),
		/** Lifeline's rope core (72FA, 800367D1 r2, HDR 16): blue to cyan. */
		CAT_TETHER(0, 0, 0, 0, .25f, 0, 1, 1, .5f, 0, 1, 1, .75f, 0, 1, 1, 1, 0, 1, 1),
		/** Lifeline's rope as enemies see it (800367D1 r3): red to yellow. */
		CAT_TETHER_ENEMY(0, 0, 0, 0, .25f, 1, .131f, 0, .5f, 1, .263f, 0, .75f, 1, .629f, 0, 1, 1, 1, 0),
		/** Lifeline's paw marker decal (7432, 800333C5 r3): blue to cyan. */
		CAT_MARKER(0, 0, 0, 0, .25f, .001f, .175f, .976f, .5f, 0, .404f, .975f, .75f, 0, .671f, .992f, 1, .072f, 1, .991f),
		/** Jetpack flame core (7064, 8004B7CE r7, HDR 11): blue to cyan to white. */
		CAT_FLAME(0, 0, 0, 0, .25f, .731f, 1, 1, .5f, 1, 1, 1, .75f, 1, 1, 1, 1, 1, 1, 1),
		/** Jetpack outer flame (7064, 800DCB53 r7, from black): gold to pale yellow. */
		CAT_FLAME_GOLD(0, 0, 0, 0, .25f, 0, 0, 0, .5f, .433f, .256f, 0, .75f, .967f, .571f, 0, 1, .947f, .863f, .13f),
		/** Frenetic Flight's ribbons (7067, 80048E45 r3): cyan behind a white head (the head is the row's x = 0). */
		CAT_BOOST(0, 0, 0, 0, .25f, 0, .929f, 1, .5f, 0, .929f, 1, .75f, 0, .929f, 1, 1, 0, .929f, 1),
		/** Frenetic Flight's ribbons as enemies see them (80048E45 r4): orange. */
		CAT_BOOST_ENEMY(0, 0, 0, 0, .25f, 1, .347f, 0, .5f, 1, .347f, 0, .75f, 1, .347f, 0, 1, 1, .347f, 0),
		/** Catnapper's target rings (71B3, 80044C8A r4): cyan to blue. */
		CAT_NAP_MARKER(0, 0, 0, 0, .25f, 0, .393f, 1, .5f, 0, .067f, 1, .75f, 0, .067f, 1, 1, 0, .067f, 1),
		/** Catnapper's landing flash (71C2, 8006A53D r0, from black, HDR 7): violet-pink to sky to cyan. */
		CAT_NAP_IMPACT(0, 0, 0, 0, .25f, .307f, .109f, .744f, .5f, .237f, .218f, 1, .75f, .13f, .614f, 1, 1, .009f, 1, 1),
```

Notes on the palettes: CAT_NAP_MARKER's row runs bright cyan at the dim end to blue at the bright end (as the game's);
CAT_SPARK ends in pure yellow (no blue); several rows clamp to flat cyan or white because the game's are HDR (2-16):
`EffectArt` turns HDR into a paler top, so these are already at that ceiling.
