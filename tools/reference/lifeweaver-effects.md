# Lifeweaver's ability effects in the game data

What Lifeweaver's effects are made of in real Overwatch, where the extracted files are, and how to rebuild each one in
Minecraft. Read 2026-10-29 from the archived offline build 154088 (prometheus-2_25_0_0-154088). Overwatch and Battle.net
were checked closed before every session; everything was read only, offline, through TankLib/DataTool. Text only: the
textures and meshes are Overwatch assets and stay in the owner's archive. They reach the game only through the local
asset pack. The format is the one `mercy-effects.md`, `jetpack_cat-effects.md`, `kiriko-effects.md` and `lucio-effects.md`
describe; this file adds what is different for him. His data map (graphs, numbers, clips, effect ids by node):
`E:\Overwatch Archive\research\lifeweaver\lifeweaver-map.md`.

Confidence after each number or claim: **H** read in the data, **M** inferred (one link guessed), **L** guess.
"Size" is the spawn size value: it scales the mesh (a 1 m quad at size 0.5 is 0.5 m; a ribbon's size is its width).
A range "a-b" is a random value between the two (Kiriko's `mode 5` / `mode 1` reading). An `ECSb` value `op(a, ..., z)`
is read as its last argument (Jetpack Cat's quality-switch reading, M). "Ours" marks what no data gives.

## Files

`E:\Overwatch Archive\research\lifeweaver\fx\Lifeweaver\<Ability>\`, with `<Ability>` one of `HealingBlossom` (the
charge, the throw, the blossom in flight, its heal, the reloads), `ThornVolley`, `PetalPlatform` (also Petal Protection
and the bullet-impact override 4DFE the platform and the tree share), `RejuvenatingDash`, `LifeGrip`, `TreeOfLife` (the
aim preview, the tree, the cast clips) or `Perks` (Superbloom, Sow the Seed):
- `textures\<id>.png` (flipbooks split `<id>_1.png ...`), `ramps\<id>.json` (float LUTs, `[row][x][r,g,b,a]`),
  `models\Models\<id>.00C\` (effect meshes), `manifest.json` (per effect: emitters paired with their renderers,
  hardpoint table, spawn shapes, raw track nodes). Same layout as `research\mercy\fx\Mercy`.
- Not under `extracted\`: pass `research\lifeweaver\fx` to `build_asset_pack.py effects` as one more source
  (`add_effects(*sources)` takes the first folder that has the file).
- Research files in `E:\Overwatch Archive\research\lifeweaver\work\`: `fx_digest.txt` (every renderer with its emitters
  resolved: the file to read for any number below), `fx_cards.txt` (the same, one compact card per renderer: mesh size,
  hardpoints with offsets, masks, colour rows with normalised hex and peak, emitters resolved; the file this note was
  written from), `fx_brief.txt`, `fx_overview.txt` (one line per effect), `fx_summary2.txt|json`, `fx_meshes2.json`,
  `fx_palettes.txt`, `fx_sheets\tex_<Ability>.png` (every mask), `fx_sheets\meshes_<n>.png` (mesh wireframes),
  `fx_effects\` (decoded effects), `fx_replacements.json` (every skin's effect swaps), `fx_extract_log.txt`.
  Scripts in `..\scripts\fx\` (`fx_lw_*.py`: Mercy's `fx_m_*` retargeted, plus `fx_lw_card.py`, `fx_lw_overview.py`, and
  `run.py`, which runs them under `py -I` with the user site added for Pillow). Every new file is read-only, listed with
  its sha256 in `fx\SHA256SUMS.txt`, `work\fx_SHA256SUMS.txt` and `scripts\fx\SHA256SUMS.txt`.

Counts: 130 effects extracted (64 with renderers, lights or material effects; the rest sound only or empty), 426
distinct textures (551 entries across the seven folders, 345 of them float LUTs), 34 meshes. Nothing missing.

## What's new compared with Mercy, Jetpack Cat, Kiriko and Lúcio (format)

- **Two palettes, by weapon (H colours, M roles).** Everything on the Healing Blossom side (the charging flower, the
  blossom in flight, its heal, the tree's waves) is **gold** (`#955C01` -> white sparkles, amber glows); everything else
  (Life Grip, the platform, thorns, the tree's preview and wither) is **magenta-pink** (`#950171` -> white). The hex
  sparkle **3EB7D** (a white hexagon in a soft glow) is in 41 of his effects, in one or the other colour. Enemy rows are
  red (`#000000` -> `#FF7777` -> white) in the same LUT, row 1 or the row after the colour (Kiriko's pairing).
- **Petals change colour as they age (H values, M reading).** The petal mask 3FD63 (a soft oval with a bright rim) on
  the curved sheet mesh D06 is in 12 effects. Its colour row is a cream gold that *falls* with x (#FFDB96 at 0 ->
  #BCA16D at 1, HDR 4), multiplied by a 12BB over-life row from cream (1.75, 1.64, 1.0) through pink (1.12, 0.34, 1.0) to
  crimson (1.0, 0.0, 0.04): a petal leaves cream-gold and blushes pink to red as it falls. Their r0 rows (`#619D8C`,
  `#7C9D8C` ...) are parameters (red constant, green / blue ramps), not colour.
- **Charge-driven effects (H nodes, M input).** The Healing Blossom effects read a dataflow input (`9BD`, and `14E`
  on the heal: 369A passes the charge as 014E, `lifeweaver-map.md`) for their sizes, rates and bursts: e.g. the blossom's
  core sphere is size 0 below 29% charge and 0.45 at full; its trailing rings start at 70%. So one effect covers every
  charge; the release effects 30D4 / 30D5 / 30D6 (by charge, 21C6#954) are **empty** (H: 0 components).
- **The vine is a beam node, as Mercy's beams (H).** 21EE#316 `STU_9932D728` draws **3110** from `m_3BC31C57` = v40827
  (the destination point in front of him, set once at the cast) to `m_2FE19ED6` = v46364, which starts at that point
  (#352 sets it to v431) and **chases the ally's hp 0014 at 88 m/s** (#354 StateChaseVar `m_9B0385B4` = 88): the vine
  grows from the landing point to the ally's pelvis. Two-point ribbon (spawn attribute 12DC15C5 = 0 / 1), no curve.
- **Projectile slots (H ids, M roles):** the thorn 3642 fills Mercy's slots (`m_A18274A3` 311A = muzzle flash,
  `m_2170A015` 4C47 = fly-by sound, `m_AED73588` / `m_BF7914D4` 311B = impacts); the blossom 35FD only `m_AED73588`
  30DB (sound) and `m_FE1DDCEB` 4D3E (a gold pop, L: when it is destroyed without healing); the platform / seed egg 450F /
  6F4C `m_306ABA15` **47B2** (the bloom burst, also played by 368D#406 at creation) and `m_BF7914D4` 4DA8 (sound), plus a
  list `m_C9E2149A` with 4219 (empty).
- **No skin swaps on the default skin (H).** `GetReplacements` of his 94 skin themes (`fx_replacements.json`): the
  default theme 2DA6 swaps nothing; some skins swap the thorns (3119 / 311A / 311B, 3 themes) and the tree's pulse sounds
  (4C96-4C98, 1). Everything below is the default look.
- **Many clip effects are empty or sound-only (H).** The release, charge, hand and growth clips (52D0, 4D72, 4B2E, 49EF,
  4CA4, 4B92-4B95, 4CB3, 4CB4, 5286, 5274, 5170, 5174, 5104) have no renderer; 4CAE / 4CD5 / 4C2B have 2-4 components of
  no drawn kind (L: model or entity control). The visible clip effects are the reloads, Life Grip's cast and the Tree of
  Life aim / cast.

### Hardpoints

| Id | Where | Conf |
|---|---|---|
| 000A | right hand (bone_004A): both weapons' muzzle, the charging glow, the throw / no-target / fire flashes | H (map) |
| 0003 / 0002 | right / left wrist (bone_003A / bone_001C): charging wisps and discs (0003); Life Grip cast, tree aim spirals | H |
| 000B | left hand: the 1P seed / platform egg (4A7E), tree cast clips | H |
| 007F / 0156 | the flower in his right hand (bone_3B32 / bone_008B, the flower's own table): charge glows and lights | H |
| 007B-007E, 0080, 0158-015C | the flower's petal bones (bone_3B10-3B19): the 1P reload lights (5173) | H |
| 0001 / 00A4 / 0014 | chest / chest (sounds) / pelvis (the vine's end on the ally) | H |
| 0004 | root: the dash (47B3), Life Grip's ally lights | H |
| 0169 016A 016B 016C 016E | the cloth / sash ends (bones 04CA, 006B, 04CB, 04D1, 04D0 under 3B2E / 3B60): the dash's petals and ribbons | H ids, M garment |
| 041F | bone_0109, behind his chest: the dash light | H |
| 003D / 003E | the generic beam start / end (the vine, the grip flower, the blossom child) | H |
| Platform 12D32: 0079, 007E 007F 0080 0155 0157, 00A4 | Pelvis (centre); the five petals at 1.79 m; +0.14 m | H |
| Tree 1267E: 0159, 015C | Spine_2 (pulse bursts, death); bone_0933 (spawn light) | H ids, L 015C's place |
| 01DA | not in any of his models: the root of the platform / tree / ally (M) | M |

### Meshes

| Id | Shape (model units, x size) | Used for |
|---|---|---|
| 0F5B / 0801 / 10F4E | 1 x 1 m quad | sparkles, glows, flares, streaks |
| 07F5 / 075D | cross ribbon / 8-sided tube ribbon | the vine, the blossom trail, cast wisps / thorn, platform, seed, dash trails |
| **D06** | curved sheet 1.11 x 1.14 m, 0.18 deep | **petals** (with mask 3FD63) |
| **12262** | twisting double helix, 0.48 x 0.48 x 1.0 m | the vine's coil (3110), the tree's growth vine (4E07, size 5) |
| **11D57** | closed flower bud, r ~0.9, 1.96 m tall with petal tips | Life Grip's protective bud on the ally (41E9) |
| 11D6D / 11D6E | tulip bud 0.085 m / flat five-petal flower 0.3 m | the flower that blooms at the grip's landing point (4A24) |
| **122CA** | thorn spike 0.072 x 0.287 m | the thorn in flight (3119) |
| **122DA** | flat five-petal outline, 3.83 x 4.02 m | the platform's bloom flash (47B2) |
| 11136 / 11362 | flower cup 0.57 m / truncated cone ring r 0.16 | petal-tip cups (4989) / splash rings (47B2, reloads) |
| 12C23 | the tree's silhouette, 4.9 x 5.3 m | the Tree of Life aim preview (4784) |
| DE0 | open cylinder r 0.47, 1 tall | the tree's range walls (size 25 preview, 17.5 held / spawn) |
| 1385 / 085B / DAF | sphere r 0.5 | tree waves (size 16-30), blossom core, heat haze (DAF, BC5) |
| F5F / 3FAE / 1BAC | thin ring r 0.2 / thin ring r 0.67 / flat disc r 0.89 with a hole | tree ground ring (size 40), blossom rings, grip ground disc |
| 12D4C | spiral, 1.55 x 1.59 m | tree aim swirls round his wrists (size 0.35) |
| 12F7D | spike star burst 2.3 m | tree spawn / pulse / death, reloads |
| 11E07 / 127B3 | twisted ring 0.57 x 0.8 / shallow cone disc 0.68 | the charging flower's swirl / under-flower disc |
| 18B3 / A68 | leaf shard 0.85 x 0.28 / rock 0.4 | platform / tree break-up |
| 126CC | egg 0.22 x 0.36 m | the platform egg / seed (projectile model, 1P hand) |
| 963D / C637 / 8C0 / 1A8AC-D | hemispheres / crossed planes / quarter discs | Petal Power (removed), flower cup, thorn muzzle |

## Colours

sRGB hex from the LUT rows (linear, normalised by the row's peak), mask 0 to mask 1. HDR peaks after the hex: clamped,
these rows are near white from a quarter up; the `EffectArt` palettes are at the end.

| Name | Where | Hex | Conf |
|---|---|---|---|
| **Pink** (his) | hex sparkles 3EB7D, r0 (HDR 5), in 41 effects (gold in the blossom ones) | #950171 #9E4A82 #B1719B #C892B6 #FFFFFF | H |
| Pink, enemy | the same LUTs, r1 | #000000 #6E2E2E #984343 #B75454 #FFFFFF | H colour, M role |
| **Gold** (the blossom's) | gold sparkles 3EB7D (472A ...), r0 (HDR 5) | #955C01 #9F782B #B59D47 #CCBD5F #FFFFFF | H |
| Blossom glow / amber | 30DE glows 0242 | #FFD759 -> #FFFDD5 / #F4A000 -> #FBF78E | H |
| Blossom trail | 30D0 ribbon r3 (HDR 10) | #000000 #544400 #71631E #9D9B54 #FFFFFF | H |
| Heal rings | 2E8AF rings r0 (HDR 2) | #E18D03 #EBB445 #F6D261 #FFEC77 #FFFFFF | H |
| **Petals** | 3FD63 on D06, r3 (HDR 4, falls) x over-life | #FFDB96 #EBCA8A #D5B67C #C2A671 #BCA16D; age: cream -> #D279C7 pink -> #C70028 | H values, M reading |
| **Vine** | 3110 ribbon r3 (HDR 20) | #7C5B7A #78586D #7C575D #AD9894 #FFFFFF (pale lilac-pink, white when clamped) | H |
| Vine coil | 3110 mesh 12262 r3 (HDR 5) | #6C016C #9F499B #C266BD #E58EDD #FFFFFF | H |
| Grip bud | 41E9 r1 | #500053 #882D86 #AD41A8 #DB59D4 #F8CBF4 | H values, M row |
| Grip flower | 4A24 r3 (HDR 1.6) | #69006C #9E349B #C34ABD #EA60E2 #FFD1FB | H |
| **Tree waves** | 43C1 / 47E2 spheres r9 (HDR 9.7) | #0B0112 #312B03 #9D8D07 #DFCD40 #FFF9AF | H |
| Tree waves, enemy | r10 | #0B0112 #310703 #9D2A07 #DF6840 #FFDEAF | H colour, M role |
| Tree ground ring | 47E2 ring r3 (HDR 2) | #000000 #B2A84F #BBB480 #C5C2AA #FFFFFF | H |
| Tree preview | 4784 silhouette r2 (HDR 3) | #79008C #AA4BBA #CF6ADE #EC80FC #FFFFFF | H |
| Tree wither | 4DCF sparkles r1 | #FFFFF5 #F8B8E3 #E09FD4 #C48ABA #B47FAB | H |
| **Thorn** | 3119 mesh r2 (HDR 30) | #2F0033 #5F1961 #774379 #B68CB8 #FFFFFF | H |
| Thorn trail | 3119 ribbon r3 (HDR 12, falls) | #FFFFFF #E16FD2 #AE44A4 #70006C #000000 | H |
| Thorn flash / hit | 311A flare r0 / 311B burst r0 | #FF0097 -> #F8CDFF / #6A00B9 #8E48A2 #C58486 #EFCE5F #FEFF00 | H, M hit row |
| Platform trails | 4216 / 4982 ribbons r2 (HDR 5) | #AA54A1 #A8539F #A953A0 #B65AAD #FFD8FA | H |
| Platform bloom | 47B2 outline 122DA r2 (HDR 5) | #000000 #493142 #6E4D65 #A7779A #FFFFFF | H |
| Petal tips raised | 4989 glow r0 / cup r5 | #EB34D2 -> #EBABE4 / #CB00B6 -> #FFFFFF | H |
| Dash ribbons | 47B3 r2 (HDR 3, falls) / r0 | #FFFFFF #F2C798 ... #D5AF85 / #000000 #D3009D #B000D7 #8000F8 #000000 | H values, L row |
| Speed lines | 2EB4 / 47B3 E99F, 12BB over life (x25) | #FEFF0D -> #38FB27 (yellow-white to green) | H values, M meaning |

## Life Grip

| Part | Effect | Conf |
|---|---|---|
| Cast on him | **3111** (21EF#693, at point v431): **sound only**; 3P cast clips CF84 / CF78 / CF83 -> **5275**, 1P C595 -> **4C98** | H |
| No target | 0186 (21EF#128 / #786): sound only (shared fail) | H |
| **The vine** | **3110** (21EE#316, beam node) from the destination point to the ally's **hp 0014**, growing at **88 m/s**; at most 1.5 s or until the pull ends; **not drawn if the ally starts within 4 m** (#366) | H |
| At the destination point | **4A24** held (21EE#369, `m_BD631144` = v40827) | H |
| On the ally, pulled | **2EB4** held (#314) | H |
| On the ally, protected | **318E** (#326) + **41E9** (#328) held | H |
| Vine lands | 48FE (#346): sound only, then his voice 11FF | H |

**The vine, 3110 (2 s loop) (H):**
- A two-point **cross ribbon** (07F5: two crossed strips, so it reads from every side) with the wispy streak mask
  **2FCA7** (soft vertical wisps, 128 x 256, tiling) and the line mask **2BE79** (an 8 x 256 strip: a soft line across
  the ribbon), colour **LW_VINE**: pale lilac-pink (4.0, 2.1, 3.9) at mask 0 to white at 1, HDR 20: on screen a bright
  white-pink band. Enemy row red to white (r4). **Width: not resolved** (spawn size 0, then an undecoded `ECSL` track /
  particle program on the size attribute: L). Ours: 0.25 m.
- **The coil:** the double-helix mesh **12262** (0.48 x 0.48 x 1.0 m) once at the effect origin (L: the destination
  point, where the node starts it), size 1, stretched x2 by attribute 6DFA (M: along its axis, so about 2 m), random
  roll, life 1.5 s, mask 3AE29 (a band with bright edges, a mesh texture: min 98, not a quad mask), colour
  **LW_VINE_COIL** magenta (0.75, 0, 0.75) -> white (HDR 5); enemy red.
- Sound 9884 twice (the vine's loop).

**At the destination point, 4A24 (2 s) (H):** a little flower blooms where the ally will land (hp 003D = the node's
point, M):
- A **tulip bud** (11D6D, 8.5 cm) at size 1.5 (13 cm) and a flat **five-petal flower** (11D6E, 0.3 m, laid flat:
  rotation -90°) at size 1, each once from 0.003 s, life 1.5 s, with a scale pulse (renderer curve 0 -> 1 at 0.04 s ->
  0.5 at 0.17 s -> -0.19 at 0.3 s -> 0 at 0.34 s, M: it pops open then settles), masks 3B17B (an arrow tip, 64 x 256) x
  2103C (near-white cloud), magenta **#69006C -> #FFD1FB** (HDR 1.6) / red.
- **At 0.56 s** (about when the pull starts, 0.35 s after the cast plus the vine's travel, M): 17-23 pink sparkles
  (3EB7D) 0.54-1.93 m/s, stretched x3-6, life 0.3-0.77 s; 10-16 thin rings (2E8AF: a soft circle outline) size 0.35,
  life 0.25-0.4 s, pink (#950194 -> white, HDR 5). Two lights of 1 at +0.96 m.

**On the ally while pulled, 2EB4 (6 s, held while pulled) (H):** speed lines (E99F = `cat_speed_line`, 3 frames):
300 a second plus 200 at the start, 0.02-0.05 m, life 0.27-0.37 s, 4-4.2 m/s; and stretched ones (size 10) 50 a second
plus 30, 4-5 m/s, life 0.22-0.32 s. Spawn position and direction from ECIN inputs (M: a box around them, backward).
Colour from the 12BB over-life rows: yellow-white to green (M).

**On the ally while protected, 41E9 + 318E (H values, M roles):**
- **41E9 (1 s loop): the flower bud.** The bud mesh **11D57** (a closed flower, petal tips on top, 1.78 x 1.96 m) once,
  on hp 003E (the beam end = the ally, M) **+1.0 m up, turned 180°**, size growing **1 -> 2** with dataflow 5FD (L: the
  protection's progress), masks 3B17B x 2103C, colour magenta-pink **#500053 -> #F8CBF4** (r1, alpha 1 -> 0: see-through
  at the bright end). The ally sits inside a pink bud.
- **318E (8 s, held):** petals (D06 + 3FD63) **50-100 a second**, life 0.3-0.77 s, **5 m/s**, size 0.06-0.17, stretched
  x0.45-8, spinning 1.6-6.2 rad/s, on their chest (hp 0001) turning -90° over life: a whirl of petals; pink sparkles
  50-100 a second at 5 m/s, stretched x1-4; soft mauve glows (0242) **3 -> 5 m** (by 5FD) 15 a second plus 20, life
  1-1.35 s, #8C6E89 -> white; a flat disc (1BAC, a ring with a hole) at size 2 (3.6 m) on the ground, life 2 s (colour row
  unresolved, L); two lights of 2.5 at 0.45 / 0.96 m above their root. Sound 9C3B.

**Cast, 5275 (3P, 1.03 s) / 4C98 (1P) (H):** on his left wrist (hp 0002, offset 4 / -8 cm): at 0.17 s three star
flares (E221) 0.76 m, life 1 s (orange-gold #7F1A09 -> #FFE0A6, HDR 4.9) and 50 tiny pink sparkles 0.02-0.08 m at
0.14-0.38 m/s; a light of 0.1.

**Recipe:**
- At the cast: a small pink-white flash (`lw_star`, 0.6 m) and a puff of pink sparkles at his left hand.
- The vine: each tick of the pull, a camera-facing band (two crossed strips) from the landing point (1.1 m in front of
  him, map) to the ally's pelvis (1.26 m above their feet), its far end advancing **88 m/s** from the landing point (so it
  reaches a 20 m ally in 0.23 s); `lw_wisp` x `lw_vine_line`, LW_VINE_N, additive, the wisps scrolling toward him (ours:
  2 m/s). Ours: 0.25 m wide; skip it if the ally started within 4 m; drop it at 1.5 s or when the pull ends. A pink coil
  (mesh 12262 as `lw_vine_coil`, LW_VINE_COIL_N, about 1-2 m long along the vine, ours) at the landing point for 1.5 s.
- At the landing point: a 13 cm pink bud and a 30 cm five-petal flower lying flat (meshes 11D6D / 11D6E, or a flat
  `lw_petal` star of five quads, ours) that pop open over 0.3 s; at 0.56 s a few pink rings (`lw_ring`) and sparkles.
- On the pulled ally: green-yellow speed lines (`cat_speed_line`) streaming backward, 3-5 a tick.
- While protected (the 0.75 s lock and the pull): the ally inside a translucent pink bud (mesh 11D57 at size 1-2, ours:
  1.2 m tall in Minecraft scale for a 1.8 m player) and a whirl of cream-to-pink petals (`lw_petal`, LW_PETAL x
  LW_PETAL_AGE) thrown off their chest at 5 m/s, 2-4 a tick, plus pink sparkle streaks.

## Healing Blossom

| Part | Effect | Conf |
|---|---|---|
| Charging (held) | **30DE** on hp 000A, offset (0, -0.025, 0.1) (21C6#724) + **30E1** (#725: sound only, 12 s) | H |
| Full charge | 48F6 (#1039 / #1417): sound only | H |
| Fire | **30E0** on hp 000A (#956) + 48B4 (#1038: sound only) | H |
| Release by charge | 30D6 (<= 0.3) / 30D5 (<= 0.9) / 30D4 (> 0.9) on hp 000A (#954, `op71` = <=): **empty** | H |
| No target | **30E6** on hp 000A (#963) | H |
| Passive charging in hand | **5E74** held (#1374) | H |
| Refill | 314A (#923): **empty** | H |
| **In flight** | child entity 4512 -> **30D0**; 4D69 -> 4C38 (sound only); 369A#444 4EA9 held (sound only) | H |
| Through terrain | **4CBF** where it enters and leaves terrain (369A#435 / #436) | H node, M meaning |
| **Heal on the ally** | **472A** on the ally's hp 0001 (369A#406, dataflow 014E = charge) + 4C37 (#448: sound only) | H |
| Projectile slots | 30DB (`m_AED73588`): sound only; **4D3E** (`m_FE1DDCEB`): a gold pop (L: destroyed unused) | H ids, L role |
| Reload | 3P clip CF50 -> **5237**, 1P C97E -> **4CD7** | H |

**The blossom in flight, 30D0 (1.67 s loop; the projectile has no model of its own, M) (H):**
- **Core:** a sphere (085B, r 0.5) with the noise 18F00 at size **0 below 29% charge, 0.28 at 30%, 0.42 at 65%, 0.45 at
  full** (so up to 0.45 m across), life 3 s (held), gold #583400 -> white (HDR 5).
- **Trail:** a cross ribbon (07F5) with the wisps 2FCA7, 64 points a second, **0.5 m wide at no charge -> 1 m at full**,
  each point living **0.1 s -> 0.25 s** with the charge (3 m behind at 30 m/s, 15 m at 60 m/s), **LW_BLOSSOM_TRAIL** gold
  (HDR 10).
- **Petals:** D06 + 3FD63, 30-40 a second, life 0.81-0.88 s, 1-3 m/s, size 0.02-0.15 over life, stretched x2,
  spinning 1-5 rad/s; cream gold blushing pink (LW_PETAL x LW_PETAL_AGE).
- **Gold sparkles** (3EB7D) **40 -> 100 a second** with the charge, life 0.4-0.85 s, size 0.055-0.09, stretched x2.
- **Above 70% charge:** thin rings (3FAE, r 0.67, noise BE4A) 10 a second at size 0 -> 1, shot **backward at 10 m/s**,
  life 1 s, gold (HDR 8); swirl rings (30E12) 50 a second, size 0 -> 2, life 0.33 s, facing along the flight.
- **Below 33% charge:** a 1 m four-point star sparkle (147C) once, gold #7C5800 -> #FFE0BA (HDR 5).
- Two lights of 1.5 -> 2 (by charge).

**The heal, 472A (1 s, on the ally's chest) (H values; every count scales with the charge 014E):**
- Two **star flares** (E221) 0.5 m, life 0.7 s, white -> orange-gold (#FFFFFF -> #FFB03B, HDR 4).
- **Gold rings** (2E8AF) **0 below 50% charge -> 10 at full**, size 0 -> 1 above 70%, life 0.25-0.4 s, facing the
  flight (dir 0, 0, 1), LW_HEAL_RING.
- **Petals** 0 -> **50** with the charge, 4-6 m/s, life 0.3-0.5 s, spinning.
- **Gold sparkles** 0 below 50% -> **80** at full, 4-7 m/s, stretched x3-6, life 0.3-0.77 s.
- Two lights 2 -> 4 at +0.17 m. Its sounds are 4C37 (9DB2).

**Charging, 30DE (1.67 s loop, on the hand and the flower) (H):**
- Two **glows** (0242) on hp 000A: 0.3 m pale gold (LW_BLOSSOM_GLOW) and 0.6 m amber (LW_BLOSSOM_AMBER), 20 a second
  each, life 1 s.
- Soft glows (A7B6) 0.22 m and star sparkles (2F4F0) 0.26-0.32 m at **1 -> 3 a second** with the charge, life
  0.47-0.76 s, at his wrist (hp 0003) and hand (000A), white-gold (HDR 6).
- **Wisps rising off the hand:** the leaf-flame mask 39C48 x noise 04D6, 8-12 a second, 0.26 m, life 1-1.5 s, drifting
  up 0.03 m/s, pale gold (#797250 -> white); tiny gold sparkle streaks (0.01-0.03 m) 20-40 a second.
- A faint shallow disc (127B3, 0.2 m) under the flower 2.5 a second, life 2 s, two of them (pale gold, HDR 5).
- **At full:** gold sparkles (0.05 m) 30-50 a second **only above 90%**; a 0.1 m glowing sphere on the flower (hp 0156)
  **only above 95%**, life 4 s.
- Each loop: a 0.2 m star flare at 0.97 s, a 0.17 m sphere pulse at 0.93 s, 10-30 gold sparkles at 1.03 s; a twisted
  ring (11E07) 6 cm round the flower root at 0.21 s; a material glow on the flower; a light on 007F rising with the
  charge to 1.

**Fire, 30E0 (0.33 s) (H):** two star flares (E221) **0.21 -> 0.45 m** with the charge, life 0.71 s, white to orange-gold;
**4 -> 60** tiny gold sparkle streaks (0.009-0.018 m, stretched x4); 10-16 gold rings (2E8AF) 0 -> 0.43 m; lights;
the flower's material glow. **No target, 30E6 (0.33 s) (H):** the two amber glows (0.21 -> 0.41 m and 0.44 -> 0.64 m by
charge) 20 a second, and 5 -> 50 gold sparkles drifting off spinning: a fizzle at the hand.

**Through terrain, 4CBF (0.67 s) (H):** a gold flare (29F7 = `lucio_spark`, tint (15, 13.2, 3.1)) **0.5 -> 1.25 m**
with the charge, life 0.2 s; 10 -> 40 gold sparkles; 4 swirl rings (30E12) 0 -> 2 m above 70%; 10-16 petals 1-3 m/s.
**4D3E (0.67 s):** the same gold flare 0.47 m, 10-16 gold sparkles 1-2 m/s spinning, a four-frame burst (2F3C9) 1-1.25
m, life 0.3 s; sound 9EB9.

**Passive charging, 5E74 (6 s, held while the blossom regrows in hand) (H):** on the flower (hp 0156): a tiny glowing
cup (hemisphere C637, 5-7 cm) 0 -> 2.5 a second above 50% charge; soft glows (A7B6) 0 -> 2.5 a second above 20%; gold
sparkles 20-30 a second at size 0 -> 0.05 above 50%; thin rings (F5F) 0 -> 4 a second above 50%, life 1.3-2 s, gold
blushing pink; the rainbow ring sprite 17D8 (a chromatic ring, BC7 colour) 0 -> 2 a second above 60%, 0 -> 0.12 m.
Faint: the flower glows a little more as it fills.

**Reload, 5237 (3P, 1.5 s) / 4CD7 (1P, 1.67 s) (H):** at the start (0.01 s) a cone ring (11362) 0.3 on his wrist; at
0.53 s (3P) / 0.07 s (1P) on the hand: a 0.3 m radial burst (37C36), a heat-haze (BC5 37C05, skip), a spike burst
(12F7D) at size 0.15 (0.35 m), the rainbow ring 0.26 m; at 0.93-1.03 s a second cone ring, soft dots and 10-30 gold
sparkles; a light. The blossom regrowing (M). Sounds 3 each.

**Recipe:**
- Charging: on his right hand (the flower, ours: 0.55 m right, 1.15 m up, 0.1 m forward at rest, or the posed hand
  bone), a pale-gold glow (`mercy_glow`, LW_BLOSSOM_GLOW) of 0.3 m inside a 0.6 m amber one (LW_BLOSSOM_AMBER), additive
  but faint (ours: 40%), a gold wisp (`lw_leaf_flame`) rising every 2-3 ticks; growing with the charge: 1-3 soft glows a
  second, and at full (100%) gold sparkles (`lw_sparkle`, LW_GOLD_N) rising off the flower. First person: the same on the
  view model's flower, smaller.
- Fire: two star flares (`lw_star`) 0.2-0.45 m by charge and up to 60 tiny gold sparks. No target: the amber glows
  fizzle out over 0.33 s.
- In flight: a gold sphere core 0.28-0.45 m (by charge; none below 29%, ours: a `lw_glow` quad instead of the sphere) with
  a gold trail band (`lw_wisp`, LW_BLOSSOM_TRAIL_N, 0.5-1 m wide, 0.1-0.25 s of travel long, ours: 3-15 m fading to the
  tail), 2-5 gold sparkles a tick and 1-2 petals a tick (`lw_petal`, LW_PETAL x LW_PETAL_AGE over their 0.85 s); above 70%
  a ring shed backward every 2 ticks (`lw_ring`).
- Where it passes through a block face: a gold flare 0.5-1.25 m and 10-40 sparkles.
- On the healed ally: two 0.5 m white-gold star flares, gold rings and up to 50 petals and 80 gold sparkles flung out at
  4-7 m/s, all scaled by the charge (none of the rings and sparkles below 50%).
- Passive charge: skip, or one faint gold mote a second on the flower.

## Tree of Life

| Part | Effect | Conf |
|---|---|---|
| Aim start / cancel | 4D76 (36A4#929) / 4D77 (#852): sound only | H |
| **Aim preview** | cosmetic entity 48D8: **4785** (#919: a material effect and sound 9FAB) + its child 48D9 -> **4784** | H |
| 3P aim / loop / cast clips | C4FF.. -> **51DD**, C4F4 -> **51DB**, C501.. -> **5210** | H |
| 1P aim / loop / cast / cancel | D465 -> **50AA**, D467 -> **50AC**, D488 / D520 -> **50B1** / **518A**, D513 -> 5104 (empty) | H |
| Spawn | **4E07** (36AA#497) + 4985 (#200: sound only) | H |
| Held (the tree alive) | **4248** (#294, a 1.75 s loop = the pulse interval) | H, M sync |
| **Sprout** | **43C1** (#346, with the 150 heal) | H |
| **Each pulse** | **47E2** (#372) + one of 4C96 (x6) / 4C97 / 4C98 (#441-#448: sound only) | H |
| On each healed ally | **43C6** (#351 / #368) | H |
| Damaged (below 50 / 25%) | **4D6E** (#429 / #431) | H |
| Death | **4D6D** / **4DA5** at hp 0159 (#438 / #437) | H; M which view (below) |
| Ending (1.67 s before) | **4DCF** / **4DD2** (#483 / #485), the leaning ending clip D1DC -> 4FBC (the same) | H; M views |
| Growth / pulse / ending clips | 4CAE, 4CD5, 4CB3, 4CB4, 5286: nothing drawn; 4D33: sound | H |

**The pulses, 47E2 (0.5 s) (H):**
- A **gold sphere shell** (1385, r 0.5) at **size 17 (8.4 m radius)**, once, life 0.5 s, speed 0.5, masks bokeh 187EB x
  vertical fade 3E4E5 x wisps 2FCA7, **LW_TREE_WAVE** black -> gold -> pale yellow (HDR 9.7) / enemy red (r10).
- A **flat ground ring** (F5F, r 0.2) at **size 40 (8.1 m radius)**, once, life 0.5 s, bokeh 187EB x gradient strip
  1F2A8, **LW_TREE_RING** pale gold (HDR 2).
- Note: the heal reaches 12 m (map); the visual ring is 8 m (H both).
- Each pulse's sound is 4C96 for pulses 1-6, 4C97, 4C98 (L wiring).

**The sprout, 43C1 (0.5 s) (H):** the same gold sphere shell at **size 16 -> 20** (8 -> 10 m radius) by dataflow 87F
(0..5, L), life 0.5 s, speed 0.5, masks noise 04DB, bokeh 187EB, band 81E7, wisps 12201; LW_TREE_WAVE.

**Held, 4248 (1.75 s loop, on the tree) (H):**
- **Each loop at 1.73 s** (just before each pulse at 1.75 s ... 14 s, M): **300-320 big gold sparkles** (3EB7D, 0.67-0.97
  m, stretched x0.1-1.1) from 0.89 m below hp 0159 (Spine_2), 1-2.5 m/s, life 1-2 s; at 1.72 s 4-5 soft dots (122C)
  0.12-0.42 m drifting 0.3-0.44 m/s.
- At 0.24 s: three radial bursts (37C36) 0.7-1 m on hp 0159; a 0.7 m sphere (085B, noise 18F00) gold; at 0.3 s the
  rainbow ring 17D8 1 m. At 1.17 s a spike burst (12F7D) at size 1 (2.3 m).
- **A cylinder wall** (DE0) at **size 17.5 (8.3 m radius)**, once at 0.003 s, life 2 s, the flower-band mask **40484**
  (three flower silhouettes over a vertical fade) x 2A063, **LW_TREE_WALL** olive gold (HDR 2).
- Gold sparkles 50-80 a second, size 0.38-1.38, moving inward / down 1-2 m/s, round hp 0159 turning -250° over life.
- A **sphere of size 30 (14.8 m radius)**, once, life unlimited, flat white mask 1298, **LW_TREE_DOME** gold (HDR 5):
  L what it shows (a range shell or a fog volume; probably nearly transparent).
- A **ground decal** 3.5 x 3.5 (soft disc 4C09 x noise 1038), tint (4, 4, 1.85) gold / (4, 1.31, 0.75) enemy orange,
  projected 2 m. Two material effects (the tree's glow); a light rising 1.1 -> 3. Sounds 9D76, 9D77, A099.

**Spawn, 4E07 (1.93 s) (H):** at 0.13 s: **a 5 m growth vine** (the helix 12262 at size 5) at the root (hp 01DA, -0.1
m), life 1.5 s, masks 2D278 (an upward soft cone) x 2BE79, gold-white (HDR 20); **167-187 big gold sparkles** (0.67-0.97
m) from 0.89 m below the root's hardpoint at **3-5 m/s**, life 1-2 s; three radial bursts (37C36) 1 m; the cylinder wall
(DE0 size 17.5) life 1 s; a light rising 1 -> 10 at hp 015C +0.2 m. At 0.43 s a spike burst (12F7D, 2.3 m). Two material
effects.

**On each healed ally, 43C6 (1 s) (H):** two star flares (E221) 1 m, life 0.7 s, white -> orange; 10-16 petals 4-6 m/s
spinning; 10-16 gold rings (2E8AF) at size 1; 10-16 gold sparkles 0.38-1.38 m at 1-3 m/s; two lights 1 -> 2 (0.54 s).
Sounds 9FB0, 5A35.

**Aim preview, 4784 (2 s loop, on 48D9) (H):**
- **A ghost of the tree:** the silhouette mesh **12C23** (4.9 x 5.3 m) at size 1.245 (about 6 x 6.6 m), 0.18 m down,
  turned 30°, life unlimited, masks 2103C x noise 299E3, pink-purple **#79008C -> white** (HDR 3).
- **The range cylinder:** DE0 at **size 25 = 11.8 m radius** (the 12 m heal range, M), life 10 s, the line mask 18A1
  (`cat_rope`), tint (1.7, 0.8, 1.7) pink.
- Pink sparkles 50-70 a second rising 1.76-3 m/s, stretched x1.7-6.9; a ground decal 3 x 3 (soft disc 4C09 x 1038).

**Damage and death (H values; views M by colour: the kit note's guess had them the other way):**
- **4D6E** (0.5 s, below 50% and 25%): 17-37 leaf shards (mesh 18B3, 0.2-0.35 m) 3-4.4 m/s in a 20° cone from +0.73 m,
  spinning 6-30 rad/s, crystal mask 2A413, pink (#F7D7FF -> #CF0EDD).
- **4D6D** (pink: his side's view, M) / **4DA5** (red: the enemies', M) (0.5 s, at hp 0159): a 2.5 m lens streak (23C0)
  0.2 s; 7-12 rocks (A68, 0.17-0.21 m) 3-5.9 m/s; 60-80 leaf shards; a spike burst; 60-80 big sparkles 2.9-4.9 m/s from
  -1 m; two 4 m orange spheres (1385, streak mask 369A3) life 0.25-0.38 s; a light 3 -> 7.9 at +2.75 m; heat haze (skip).
- **Ending, 4DCF (his side, M) / 4DD2 (red, M) (1.67 s):** at 0.8 s **300-320 floating sparkles** 0.67-0.97 m, pale
  pink-white **#FFFFF5 -> #F8B8E3 -> #B47FAB**, life 2.1-3.6 s, no speed (they hang in the air); 44-64 soft dots rising
  1-3 m/s; a light 1 -> 5 at +0.25 m; a material effect from 0.56 s (the tree withering, M).

**Caster clips (H):** aim start **51DD** (3P, 0.7 s): two wisp ribbons (07F5, 2FCA7) 0.18 m from his hands (000A /
000B) from 0.13 s for 0.17 s, life 0.2 s, gold. Aim loop **51DB** (1.67 s): a **spiral** (12D4C at size 0.35, about 0.55
m) round each wrist (0002 / 0003, tilted ±4 rad), 1 a second, life 2 s, gold-white (HDR 20); tiny gold sparkles 30-50 a
second on each hand. Cast **5210** (1 s): two wisp ribbons 0.55 m from his hands for 0.45 s, life 0.35 s; 100-130 gold
sparkles a second; **leaves** (the four-frame leaf flipbook **3AC78**) 30-40 a second, 0.05-0.07 m, 1-3 m/s, cream
blushing pink; a light. 1P: 50AA (aim: a burst, a 0.1 m swirl 11E07, flame wisps, the rainbow ring), 50AC (loop: a 0.15
spiral, sparkles), 50B1 / 518A (cast: a 0.3 m burst, 92-112 sparkles rising, a spiral).

**Recipe:**
- Aim: a translucent pink ghost of the tree where it will grow (ours: the tree's own model drawn additive pink at 30%,
  or a pink outline), a thin pink ring on the ground at 12 m (`cat_rope` laid flat in short pieces on the ground,
  `KirikoFx.groundY`), 2-3 pink sparkles a tick rising. Gold spirals turning round his wrists (`lw_beam_cone` quads in a
  ring, ours) while aiming.
- Cast: gold wisps from his hands and leaves (`lw_leaf`, 4 frames) flying off.
- Spawn: a 5 m gold spiral vine shooting up the trunk (mesh 12262 at size 5, or 20 `lw_sparkle` quads along a helix,
  ours) for 1.5 s, 170 big gold sparkles bursting 3-5 m/s from the trunk, a 2.3 m spike flash and a light.
- Each pulse (every 1.75 s from 1.75 s): a gold sphere shell expanding to 8.4 m in 0.5 s (ours: a ring of `lw_bokeh` x
  `lw_fade_v` quads, or a sphere mesh, additive, LW_TREE_WAVE_N, fading out) and a flat gold ring on the ground 8 m wide
  (`lw_ring` laid flat, LW_TREE_RING_N); just before it, 300 big gold sparkles from the trunk (ours: 30-60). The sprout
  is the same shell at 8-10 m. Between pulses: gold sparkles drifting in round the trunk (2-3 a tick) and a gold ground
  glow 3.5 m under it.
- On each healed ally: two 1 m white-orange flares, gold rings, petals and sparkles.
- Damage: leaf shards (`lw_crystal` on small quads or mesh 18B3, pink) at 50% and 25%. Death: a lens streak, rocks, 70
  leaf shards, sparkles and a 4 m orange flash. Ending (1.67 s before): 300 pale pink sparkles that hang in the air for
  2-3.6 s (ours: 40-80) while the tree withers.
- Enemy view: the same with the red rows (LW_TREE_WAVE_ENEMY ...).

## Petal Platform

| Part | Effect | Conf |
|---|---|---|
| Throw | 4218 on hp 0002 (2219#390): **sound only**; 1P throw clip C5D2 -> 4A7E: the egg model 126CC in his left hand (000B) for 0.13 s | H |
| **Thrown** | the egg 450F (model 126CC, 0.22 m) -> child 4510 -> **4216** | H |
| **Creation / bloom** | **47B2** (368D#406, also the egg's `m_306ABA15`) + 421A (#200: sound only) | H |
| **Armed** (untriggered) | **422B** (#306) + 48EF (#332: sound only) | H |
| Triggered | 4DE3 (#408): **sound only** | H |
| **Rise** | **4982** at the start (#339) on the five petals + 48F0 (#184: sound only) | H |
| **Raised** | 4A46 (#362: sound only) + **4989** on each petal hardpoint 007F / 0080 / 0155 / 007E / 0157 (#351-#355) | H |
| Last 5 s | 48F1 (#333: sound only), 499D (#359: empty) | H |
| Descent | 5E50 / 5E53 (#666 / #667): **sound only** | H |
| **Destroyed** | 4A47 (#300: sound only) + **499C** (#421, pink: his side, M) / **4E21** (#422, red, M) at pos - 0.5 m | H; M views |
| Petal Protection | **5152** (#776) | H |
| Hit by bullets | 4DFE (impact override on 4504 and 4525): sound only | H |
| Petal Power (removed perk 268A.134) | 6CB7, 6D9A, 6E86 (visual), 6E5B (sound): not described | H |

**Thrown, 4216 (1 s loop, on the egg) (H):** an **8-sided tube ribbon** (075D) **0.61 m wide**, 60 points a second,
each living **0.35 s** (17.5 m behind at 50 m/s), mask 2103C (near-white cloud) with a fade curve (R16 987C),
**LW_PLATFORM_TRAIL** pink (HDR 5) / red; a soft pink glow (A7B6) of **3 m** 2 a second plus 1, life 1 s, #520049 ->
white; petals 38-48 a second, life 0.96-1.48 s, 1-3 m/s; pink sparkles 56-86 a second, life 0.76-2 s, 1-2 m/s; two
lights; a material effect on the egg.

**Creation, 47B2 (2 s) (H):** the platform blooms:
- The **flower outline** (122DA: a flat five-petal ring, 3.83 x 4.02 m) at size 1 on hp 0079 (its pelvis) **+0.96 m**,
  once, life 1.1 s, crystal mask 2A413, **LW_PLATFORM_FLOWER** mauve to white (HDR 5).
- Two radial bursts (37C36) of **5 m** (life 0.27 s) and 2-3 m (0.72 s); a cone ring (11362) at size 10 (3.1 m wide,
  1.4 m tall) life 0.8 s, pink #7C265E -> white; a light rising to 4.6 over 0.8 s.
- 80-100 petals at 5 m/s (stretched x0.45-8) from 0.25 m below hp 0156 (L on the platform: the egg's flower point);
  80-100 pink sparkles at 5 m/s; **100 big pink sparkles** 0.35-0.53 m rising 1.2-4 m/s.

**Armed, 422B (2 s loop, until a rider triggers it) (H):** pink sparkle streaks 50-70 a second rising 1.76 m/s
(0.06-0.27 m, stretched x1.7-6.9) from 0.25 m below hp 0156; a **ground decal** 2.5 x 2.5 (M: half extents, a 5 m swirl)
with the swirl ring 30E12, turning a half turn every 2 s, tint (3.82, 1.78, 4) pink / (4, 0.58, 0.58) red; tall streaks
(1D047 = `cat_purr_streak`) at size 3 (3 m) 3 a second plus 4, life 1 s (colour rows unresolved, L); two lights of 2.

**Rise, 4982 (1.4 s) (H):** **five 8-sided tube ribbons, one per petal hardpoint** (007F, 007E, 0080, 0155, 0157; the
ESHT lists them, M which ribbon on which), **0.95 m wide**, 60 points a second, life **0.45 s** (at 10 m/s a 4.5 m trail
under each petal), mask 2103C, LW_PLATFORM_TRAIL pink; pink sparkles 30 a second from 1.17 s, 0.45-1 m/s.

**Raised, 4989 (1 s loop on each of the five petals) (H):** a **flower cup** (11136, 0.57 m) at size 1.78 (1 m) 1.5 a
second, life 1 s, rising 0.3 m/s, masks 2C41F x 16E9D, **LW_PETAL_CUP** magenta / red; a **soft glow** (210FD) 0.9-0.98
m, 2 a second, life 1 s, **LW_PETAL_GLOW** magenta; a heat haze (sphere DAF with BC5 18F81: skip); two lights of 0.64,
0.18 m below. Sound 98AE. All at 0.1 m down / -0.1 m along the petal (offset).

**Destroyed, 499C / 4E21 (2 s, at the platform - 0.5 m) (H):** a 2.5 m lens streak (23C0) 0.2 s; **28-41 leaf shards**
(18B3 at 0.23-0.41 = 0.2-0.35 m) 3-4.4 m/s in a 20° cone, spinning 18-23 rad/s, life 0.48-0.84 s; 80-100 sparkles and
80-100 petals at 5 m/s; a light 0 -> 4 in 0.2 s; heat haze (skip). 499C pink, 4E21 red.

**Petal Protection, 5152 (3 s, on each ally within 3 m, M) (H):** **gold dots** (126A = `cat_heal_dot`, a colour sprite)
30 a second plus 1, life 0.94-2.8 s, rising 0.27-0.52 m/s in a 50° cone, 0.04-0.11 m (tint x12 white; a second emitter
tinted (12, 0.74, 0.10): red-orange, M the enemy view); **plus crosses** (3DC7, the mask the pack calls `sparkle`: a
plus cross with an outline) 5-10 a second, 0.43-0.61 m, rising 0.3-1.3 m/s, life 0.9-1.2 s (colour: a (0, 6, 6) tint row,
cyan, L); two lights of 5 at +0.17 m.

**Recipe:**
- Throw: the egg (mesh 126CC, or a small pink-white `lw_glow`, ours) flying with a 0.6 m pink tube trail 0.35 s long
  (ours: 4-8 m, `lw_wisp` crossed strips, LW_PLATFORM_TRAIL_N), 3-4 pink sparkles and 1-2 petals a tick.
- Bloom (where it lands): a flat 4 m five-petal outline flash on the platform's top (mesh 122DA, or five `lw_petal` quads
  in a star, LW_PLATFORM_FLOWER_N) for 1.1 s, a 5 m radial burst (`lw_burst`) and 100 pink sparkles plus 100 petals
  thrown 5 m/s (ours: 30 + 30).
- Armed: a slowly turning pink swirl (`lw_swirl`) on its top 5 m wide, pink sparkle streaks rising (1 a tick).
- Rise: a pink trail under each of the five petal tips (the petal hardpoints, 1.79 m from the centre) for 0.45 s of
  travel (4.5 m). Raised: a magenta cup glow (`lw_glow`, LW_PETAL_GLOW, about 1 m, ours) at each petal tip twice a
  second, rising a little. Descent and the last 5 s: sound only.
- Destroyed: 30-40 pink leaf shards (`lw_crystal` small quads, or mesh 18B3) and 80 sparkles and petals at 5 m/s from
  0.5 m below it, a 2.5 m lens streak.
- Petal Protection: rising gold dots (`cat_heal_dot`) and a few plus crosses (`sparkle`) on allies within 3 m.

## Thorn Volley

| Part | Effect | Conf |
|---|---|---|
| Firing (held) | 4A9E (21C7#240): **sound only** | H |
| **In flight** | thorn 3642 -> child 3643 -> **3119** | H |
| **Muzzle flash** | **311A** (`m_A18274A3`), every emitter on hp 000A + sub-effect 4010 (sound) | H slot, M role |
| Fly-by | 4C47 (`m_2170A015`): sound only | H |
| **Hit** (creature or wall) | **311B** (`m_AED73588` and `m_BF7914D4`) | H slot, M role |
| 1P firing | clip CC95 -> 5173: ten lights of 0.05 on the flower's petal hardpoints; others empty | H |
| Reload | 3P C477 -> **523E**, 1P CFA0 / CDFC -> **4F2D** / **4EBB**: like the blossom's reload (bursts, rainbow ring, sparkles) | H |

**In flight, 3119 (1 s loop) (H):**
- **The thorn:** the spike mesh **122CA** (0.072 x 0.287 m) at size 1, life 3 s, crystal mask 2A413, **LW_THORN**
  purple-magenta to white (HDR 30) / red (r3); a second, larger thorn (size 1.3: 0.37 m) as a glow shell (#471149 ->
  white, HDR 30).
- **Two trails:** 8-sided tube ribbons, **0.04 m** wide with points living **0.21 s** (21 m behind at 100 m/s) and
  **0.05 m** with **0.12 s** (12 m), mask 2A413 with fade curves, **LW_THORN_TRAIL** white -> pink -> magenta -> black
  (falls: bright at the head, M), enemy red.

**Muzzle flash, 311A (0.33 s) (H):** a hot-pink flare (29F7) **1.18 m** and a 0.27 m one, life 0.1 s,
**LW_THORN_FLASH** #FF0097 -> #F8CDFF; crossed planes (8C0) with the two-frame flame burst **14F9**, 0.6-0.93 m and
0.75-1.18 m, life 0.08-0.1 s, along the shot (tint x4); thin streaks (1D047) 0.08-0.11 m and 0.2-0.4 m, 1.6-2.8 m/s
in 26° / 40° cones, life 0.05-0.1 s; two lights of 0.23.

**Hit, 311B (0.67 s) (H):** a burst (the four-frame star burst **2F3C9** x noise 299E9) **0.29-1.28 m**, stretched x7.5
(M: along the surface or the shot), life 0.09-0.34 s, 7 cm off the surface, **LW_THORN_HIT** purple -> gold -> yellow;
the same with a soft glow (A7B6); 2-5 pink sparkles at 5 m/s, stretched x1-4; two lights. No decal.

**Recipe:**
- Each thorn (100 m/s, two per volley, 11 volleys a second): a 0.3 m purple-pink thorn (mesh 122CA, or a stretched
  `lw_sparkle` / `lucio_spark` quad along the flight, LW_THORN_N) with a 0.05 m pink tracer 12-21 m long fading to
  magenta (ours: 3-5 m, it is 0.05 s of travel per tick).
- Muzzle (third person; skip in first person, as Jetpack Cat's): a 1 m hot-pink flash (`lw_thorn_flame` on two crossed
  quads along the shot, LW_THORN_FLASH) for 0.1 s and 3-5 thin pink streaks (`cat_purr_streak`).
- Hit: a 0.3-1.3 m purple-to-yellow burst (`lw_hit`, 4 frames over 0.2 s, LW_THORN_HIT) and 2-5 pink sparks.

## Rejuvenating Dash

| Part | Effect | Conf |
|---|---|---|
| Dash start | **47B3** on hp 0004 (36A2#294) + 4901 (#335: sound only) | H |
| If hurt | **3DEB** (#321) on hp 0001 | H |
| Fail | 0186: sound | H |

**47B3 (1 s; emitters live 0.26-0.36 s = the 0.25 s dash) (H):**
- **Five ribbon trails**, one per sash end (hp 0169, 016A, 016B, 016C, 016E: M which ribbon on which), 8-sided tube
  ribbons (075D) **0.5 m wide**, 60 points a second for 0.36 s, each living **0.15 s** (about 4.3 m behind at 28.9 m/s),
  flat mask AC4 with fade curves (R16 3E7B / 987C); colour rows: **LW_DASH** white -> cream gold (falls, HDR 3) and a
  magenta -> violet row (r0, dark at both ends: L along the ribbon).
- **Petals** from each of the five sash ends, 20 a second each for 0.26 s, life 0.31-0.78 s, 1-3 m/s, 0.045-0.18 m,
  stretched x2-5, spinning 1-6 rad/s (LW_PETAL x LW_PETAL_AGE).
- **Gold sparkles** 26-56 a second from each sash end, 1-2 m/s (#7A7134 -> white, HDR 5).
- **Speed lines** (E99F) 100 a second plus 100 for 0.26 s, 0.02-0.05 m, 1-1.2 m/s, life 0.27-0.37 s.
- A light of 1 behind his chest (041F) for 0.53 s; a material effect (a body flash).

**If hurt, 3DEB (1 s) (H values, M place):** **yellow plus crosses** (0BCD, a colour sprite: a grey plus with a white
outline, shape in alpha) 10 a second plus 20 for 0.23 s, rising 0.17-0.7 m/s, 0.07-0.125 m, tint (2, 2, 0); 10 bigger
ones 0.24-0.42 m at 2-6 m/s in a 20° cone upward, tint (6, 6, 1), spinning 3 rad/s; an inverted vignette (18B9B: black
centre, white corners) at size 2 with input ECIM(40) (L: a first-person screen-edge flash), purple. Sound 1811.

**Recipe:** during the 0.25 s dash: five cream-gold ribbons (`lw_wisp` or `cat_fade` strips, LW_DASH_N, 0.3 m wide ours)
trailing 4 m from his waist sash (ours: points 0.2-0.4 m from his hips and back, 0.6-1 m up), 1-2 petals and 2 gold
sparkles a tick from them, a few speed lines. If he was hurt: a puff of 20-30 small yellow plus crosses (`lw_plus`,
NATURAL) rising off his chest, 10 bigger ones thrown up 2-6 m/s.

## Perks

| Part | Effect | Conf |
|---|---|---|
| Superbloom | **6548** on the enemy's hp 0001 (51F4#370) + 653C / 657A (sound only) | H |
| Sow the Seed: throw | 73EE (51F4#521): sound only; the seed 6F4C (the egg 126CC) -> 6FA1 -> **7404**; its slots 47B2 / 4DA8 / 4219 (the platform egg's) | H |
| Seed on the ground | 5C5A: **46C0** (lifetime end, M), 4BEF (sound) | H ids, M role |
| Picked up | 5C5A: **4DFC** at the seed, 7401 (sound) and **472A** (the blossom heal) on the ally | H |
| Buff held | 73FC (5C5C#1340): sound only | H |
| Drop 4809 (another mode, L) | 480A -> 480B -> 46C1: not described | H ids |

**Superbloom, 6548 (0.67 s) (H):** two star flares (E221) **5 -> 6.6 m** (by input 5FD, L), life 0.3 s, white ->
red (r0) / white -> pink (r1: M his view); a burst (2F3C9 x 299E9) 2.8-5.3 m, stretched x7.5, life 0.09-0.34 s; 15-20
sparkles thrown 4.6-10.6 m/s, stretched x4.5-7.5, red (r0) / pink (r1).

**The seed in flight, 7404 (1 s) (H):** the platform egg's look in gold: an 8-sided tube ribbon 0.61 m, life 0.35 s,
gold #AA8C54 -> #FFF8D8 (HDR 5) / red; gold sparkles 56-86 a second; a soft gold glow (A7B6) 3 m 2 a second.
**On the ground (46C0, 1 s) / picked up (4DFC, 0.5 s) (H):** a 0.5 / 1 m star flare (white -> gold), a 2 m soft glow
(46C0), 27 petals 0.3-0.9 / 0.7-1.25 m/s, 17-23 gold sparkles 0.9-2.7 / 4-7 m/s stretched x4.7-7.7, gold rings (4DFC).

**Recipe:** Superbloom: a 5 m pink-white star flash on the enemy and 15-20 pink sparks at 5-10 m/s (enemy colours red,
seen by the enemy). Sow the Seed: the seed flies with a gold tube trail and sparkles (the platform throw in LW_GOLD);
on pickup a 1 m gold flare, petals and gold sparks, and the blossom's heal on the ally (472A).

## Practical notes for Overcraft

- Pre-colour the grey masks with the palettes below, additive, depth-tested, no depth writes. Two looks: gold
  (blossom, tree waves) and pink (grip, platform, thorns). Following `rendering.md`, blend the warm gold rather than add it
  over bright ground (`Fx.soft`), and keep the pink additive.
- HDR rows: the vine (20), thorns (30), tree waves (9.7) and the blossom trail (10) clamp to white; use the `_N` rows for
  thin bands, and the clamped ones for the hot cores.
- Petals: draw `lw_petal` with LW_PETAL and tint each petal toward LW_PETAL_AGE over its life (cream at birth, pink at
  half, crimson at the end, fading): that is the data's look (M).
- Skip the heat haze (BC5 37C05, 18F81, 7C05) and the material effects (body overlays); the flat white 1298 / CA8C are
  material inputs, not masks.
- Sizes here are real Overwatch metres; Minecraft's player is 1.8 m: Lifeweaver's model is 1.82 m to the head (map), so
  most numbers fit as they are; cap the 15-30 size spheres and 8 m rings to what reads (ours).
- Masks already in the pack: `mercy_glow` (0242), `lucio_spark` (29F7), `cat_glow_dot` (A17F), `cat_soft` (4C09),
  `cat_speed_line` (E99F, 3), `cat_heal_dot` (126A), `sparkle` (3DC7: a plus cross, Kiriko's name), `cat_fade` (07A9),
  `cat_rope` (18A1), `cat_purr_streak` (1D047), `cat_soft_ball` (39115), `cat_glow_noise` (19167), `mercy_noise` (04DB),
  `cat_ring_line` (299E3), `cat_band` (1C7A).

## Not found / not certain

- The vine's width (spawn size 0 and an undecoded `ECSL` / particle-program track: L) and where its coil spawns (L: the
  node's start point); whether the beam node bends it (Mercy's same question).
- Dataflow inputs other than the charge: 5FD (the grip bud's and Superbloom's size: L), 87F (the sprout's 16 -> 20: L).
- Which team view is which for 4D6D / 4DA5, 499C / 4E21, 4DCF / 4DD2 (by colour, M); the enemy rows of the blossom and
  tree (no red twin in most gold rows: M an enemy sees gold).
- The petal LUT reading (x = mask or age; M), the dash ribbons' colour row (L), Petal Protection's cross colour (L), the
  armed platform's tall streaks' colour (L), the size-30 tree sphere's role (L).
- 01DA's bone (M: the entity root), 015C's place on the tree (L), the sash garment behind 0169-016E (M).
- 4D3E's trigger (`m_FE1DDCEB`: L destroyed unused), 4219 (empty, in a list slot), 499D (2 components, nothing drawn).
- What the growth clips 4CAE / 4CD5 / 4C2B's components do (not renderers, sounds or lights: L model control).
- Petal Power's (removed) and drop 4809's effects were extracted but not described.

## Masks (`tools/maskstat.py`)

| Mask | Size | min / max, corner / edge / centre (red) | Flag |
|---|---|---|---|
| **3EB7D** hex sparkle | 256² | 0 / 255, 0 / 3 / 255 | white hexagon in a soft glow: his sparkle |
| **3FD63** petal | 256² | 0 / 255, 0 / 0 / 223 | soft oval with a bright rim |
| 2FCA7 wisps | 128 x 256 | 2 / 241, 7 / 142 / 143 | tiling streaks (ribbons), no black floor |
| 2BE79 line | 8 x 256 | 0 / 255, 0 / 0 / 135 | soft line across a ribbon |
| 3AE29 coil band | 256² | **98** / 255, **255** / 98 / 98 | mesh texture (bright edges, no black): not a quad mask |
| 2E8AF ring | 256² | 0 / 255, 0 / 0 / **0** | thin soft circle outline |
| E221 star | 256² | 0 / 255, 0 / 4 / 255 | star flare with rays |
| A7B6 glow | 256² | 0 / 240, 0 / 0 / 240 | soft ball |
| 122C disc | 64² | 0 / 255, 0 / 0 / 255 | soft-edged disc |
| 37C36 burst | 256² | 0 / 254, 0 / 0 / 253 | radial burst |
| 30E12 swirl | 256² | 0 / 255, 0 / 0 / 127 | swirling ring |
| 39C48 leaf flame | 256 x 384 | 0 / 255, 0 / 0 / 255 | flame / leaf shape |
| 17D8 prism ring | 128² | rainbow (BC7 colour), centre 0 | colour sprite (draw as is, additive) |
| 40484 flower band | 256² | 0 / 255, 0 / 0 / 116 | three flowers over a vertical fade (cylinder wall) |
| 23C0 lens | 512 x 256 | horizontal streak | lens streak |
| 2D278 beam cone | 128² | 0 / 255, 0 / 0 / 174 | upward soft cone |
| 3AC78 leaf | 128² x 4 | 0 / 255, 0 / 0 / 255 | leaf silhouette flipbook |
| 187EB bokeh | 256² | **50** / 255, 116 / 202 / 141 | noise multiplier (no black floor) |
| 3E4E5 fade | 256² | 0 / 255, 0 / 0 / 95 | vertical gradient |
| 14F9 flame | 256² x 2 | 0 / 251, 0 / 0 / 140 | spiky flame burst (muzzle) |
| 2F3C9 burst | 256² x 4 | 0 / 255, 0 / 0 / **0** | star burst flipbook (dark centre) |
| 2A413 crystal | 256² | **112** / 255, 230 / 221 / 255 | triangle grid: a mesh texture (no black floor) |
| **0BCD plus** | 128² | RGB 0-255, **alpha 0-255** | **colour sprite** (shape in alpha): NATURAL |
| 18B9B vignette | 256² | 252 corner / 0 centre | **INVERTED** (screen-edge); not proposed |

Not proposed, flagged: 2103C (near-white cloud, Mercy's note), 1298 / CA8C (flat white), the BC5 distortions (37C05,
18F81, 7C05), 3B17B (an arrow tip: the grip flower's mesh texture), 22EC / 800xxxxx (colour / curve strips).

## Textures to add (`build_asset_pack.py`, `EFFECT_MASKS`)

With `research\lifeweaver\fx` passed to `build_asset_pack.py effects` as a source:

```python
    # Lifeweaver's (tools/reference/lifeweaver-effects.md): his hex sparkle, the petal, the vine's wisps and line, a
    # thin ring, a star flare, a soft glow and disc, a radial burst, a swirl ring, the leaf-flame wisp, the rainbow ring
    # (a colour sprite: draw as is)
    "lw_sparkle": ("Lifeweaver", "LifeGrip", "00000003EB7D", 1),
    "lw_petal": ("Lifeweaver", "LifeGrip", "00000003FD63", 1),
    "lw_wisp": ("Lifeweaver", "LifeGrip", "00000002FCA7", 1),
    "lw_vine_line": ("Lifeweaver", "LifeGrip", "00000002BE79", 1),
    "lw_ring": ("Lifeweaver", "LifeGrip", "00000002E8AF", 1),
    "lw_star": ("Lifeweaver", "LifeGrip", "00000000E221", 1),
    "lw_glow": ("Lifeweaver", "HealingBlossom", "00000000A7B6", 1),
    "lw_disc": ("Lifeweaver", "HealingBlossom", "00000000122C", 1),
    "lw_burst": ("Lifeweaver", "HealingBlossom", "000000037C36", 1),
    "lw_swirl": ("Lifeweaver", "HealingBlossom", "000000030E12", 1),
    "lw_leaf_flame": ("Lifeweaver", "HealingBlossom", "000000039C48", 1),
    "lw_prism": ("Lifeweaver", "HealingBlossom", "0000000017D8", 1),
    # the tree: flower-band wall, lens streak, upward cone, leaf flipbook, bokeh noise, vertical fade
    "lw_flower_band": ("Lifeweaver", "TreeOfLife", "000000040484", 1),
    "lw_lens": ("Lifeweaver", "TreeOfLife", "0000000023C0", 1),
    "lw_beam_cone": ("Lifeweaver", "TreeOfLife", "00000002D278", 1),
    "lw_leaf": ("Lifeweaver", "TreeOfLife", "00000003AC78", 4),
    "lw_bokeh": ("Lifeweaver", "TreeOfLife", "0000000187EB", 1),
    "lw_fade_v": ("Lifeweaver", "TreeOfLife", "00000003E4E5", 1),
    # thorns: the muzzle flame (2 frames), the hit burst (4 frames), the crystal grid (a mesh texture); the dash's plus
    # (a colour sprite: NATURAL)
    "lw_thorn_flame": ("Lifeweaver", "ThornVolley", "0000000014F9", 2),
    "lw_hit": ("Lifeweaver", "ThornVolley", "00000002F3C9", 4),
    "lw_crystal": ("Lifeweaver", "ThornVolley", "00000002A413", 1),
    "lw_plus": ("Lifeweaver", "RejuvenatingDash", "000000000BCD", 1),
```

Every file was checked to exist (`work\fx_palettes.txt`). `EffectArt` reads masks from the red channel: `lw_plus` must be
drawn with `Palette.NATURAL`; `lw_prism` is a colour sprite (draw its RGB additive); `lw_crystal`, `lw_bokeh` and
`lw_wisp` have no black floor (multipliers: on a quad they draw a full square; use them on ribbons / meshes or with a
`BLACK_POINT`).

Meshes worth converting (`EFFECT_MODELS`, optional):

```python
                 # Lifeweaver's (tools/reference/lifeweaver-effects.md): the vine's coil / tree vine, the grip bud, the
                 # grip flower and bud, the thorn, the platform's bloom outline and petal-tip cup, the curved petal
                 # sheet, the tree's silhouette (aim preview), the wrist spiral, the spike burst
                 "lw_vine_coil": ("Lifeweaver", "LifeGrip", "000000012262"),
                 "lw_bud": ("Lifeweaver", "LifeGrip", "000000011D57"),
                 "lw_flower": ("Lifeweaver", "LifeGrip", "000000011D6E"),
                 "lw_tulip": ("Lifeweaver", "LifeGrip", "000000011D6D"),
                 "lw_thorn": ("Lifeweaver", "ThornVolley", "0000000122CA"),
                 "lw_flower_ring": ("Lifeweaver", "PetalPlatform", "0000000122DA"),
                 "lw_cup": ("Lifeweaver", "PetalPlatform", "000000011136"),
                 "lw_petal_sheet": ("Lifeweaver", "LifeGrip", "000000000D06"),
                 "lw_tree_ghost": ("Lifeweaver", "TreeOfLife", "000000012C23"),
                 "lw_spiral": ("Lifeweaver", "TreeOfLife", "000000012D4C"),
                 "lw_spike_burst": ("Lifeweaver", "TreeOfLife", "000000012F7D"),
```

(Remember `HeroModels.EXTRAS`, `rendering.md`: an effect mesh loads only if it is named there.)

## Palettes (`EffectArt.Palette`)

Linear, clamped at 1, (stop, r, g, b). Rows that rise from black keep the game's positions (0, .25, .5, .75, 1); rows
that start coloured get black at 0 and the game's values at .25 / .5 / .75 / 1 (Lúcio's convention). HDR rows also get a
`_N` line normalised by their peak, which keeps the hue. Generated by `research\lifeweaver\scripts\fx\fx_lw_palettes.py`
(`work\fx_palettes.txt`, which also has the enemy rows and the clamped forms of every `_N` here).

```java
		/** His hex sparkles 3EB7D (8005B0D22D52 r0, HDR 5): magenta-pink to white. */
		LW_PINK(0, 0, 0, 0, .25f, 1, .343f, 1, .5f, 1, .83f, 1, .75f, 1, 1, 1, 1, 1, 1, 1),
		LW_PINK_N(0, 0, 0, 0, .25f, .345f, .069f, .225f, .5f, .443f, .166f, .33f, .75f, .575f, .286f, .466f, 1, 1, 1, 1),
		/** The same, as enemies see them (r1): black to red to white. */
		LW_PINK_ENEMY_N(0, 0, 0, 0, .25f, .158f, .028f, .028f, .5f, .316f, .056f, .056f, .75f, .474f, .089f, .089f, 1, 1, .999f, .999f),
		/** Gold hex sparkles (800CF57A9184 r0, HDR 5): the blossom's side. */
		LW_GOLD(0, 0, 0, 0, .25f, 1, .951f, .125f, .5f, 1, 1, .321f, .75f, 1, 1, .567f, 1, 1, 1, 1),
		LW_GOLD_N(0, 0, 0, 0, .25f, .347f, .19f, .025f, .5f, .466f, .34f, .064f, .75f, .605f, .508f, .113f, 1, 1, 1, 1),
		/** The charging blossom's glows 0242 (30DE C33 / C37): pale gold, amber. */
		LW_BLOSSOM_GLOW(0, 0, 0, 0, .25f, 1, .755f, .242f, .5f, 1, .831f, .384f, .75f, 1, .906f, .523f, 1, 1, .982f, .665f),
		LW_BLOSSOM_AMBER(0, 0, 0, 0, .25f, .919f, .497f, .068f, .5f, .935f, .642f, .136f, .75f, .949f, .785f, .203f, 1, .964f, .93f, .27f),
		/** Petals 3FD63 (8007F2189D11 r3, HDR 4; the row falls: bright at mask 0). */
		LW_PETAL_N(0, 0, 0, 0, .25f, .831f, .588f, .253f, .5f, .661f, .469f, .202f, .75f, .537f, .38f, .164f, 1, .5f, .354f, .152f),
		/** The petals' over-life tint (800A664BB547 r0, 12BB): cream to pink to crimson; use by age, not by mask. */
		LW_PETAL_AGE_N(0, 0, 0, 0, .25f, .778f, .464f, .572f, .5f, .642f, .189f, .572f, .75f, .581f, .045f, .278f, 1, .572f, 0, .021f),
		/** Life Grip's vine 3110 (80072BB6FABB r3, HDR 20): lilac-pink to white. */
		LW_VINE_N(0, 0, 0, 0, .25f, .189f, .096f, .152f, .5f, .201f, .097f, .11f, .75f, .418f, .315f, .298f, 1, 1, 1, 1),
		/** The vine's coil 12262 (800B8E14B83B r3, HDR 5): magenta to white. */
		LW_VINE_COIL_N(0, 0, 0, 0, .25f, .347f, .067f, .33f, .5f, .545f, .134f, .511f, .75f, .787f, .271f, .721f, 1, 1, .999f, .999f),
		/** The blossom's flight trail 30D0 C25 (80098E9C7FFE r3, HDR 10): black to gold to white. */
		LW_BLOSSOM_TRAIL(0, 0, 0, 0, .25f, .898f, .585f, 0, .5f, 1, 1, .132f, .75f, 1, 1, .876f, 1, 1, 1, 1),
		LW_BLOSSOM_TRAIL_N(0, 0, 0, 0, .25f, .09f, .059f, 0, .5f, .168f, .126f, .013f, .75f, .337f, .327f, .088f, 1, 1, 1, 1),
		/** Gold ring flashes 2E8AF (800C725A38EE r0, HDR 2): heal, fire, tree heal, seed pickup. */
		LW_HEAL_RING_N(0, 0, 0, 0, .25f, .835f, .459f, .06f, .5f, .918f, .651f, .12f, .75f, 1, .84f, .184f, 1, 1, 1, 1),
		/** The tree's sprout / pulse sphere shells (800EF2EBAB76 r9, HDR 9.7): black to gold to pale yellow. */
		LW_TREE_WAVE_N(0, .003f, 0, .006f, .25f, .036f, .028f, .001f, .5f, .343f, .271f, .002f, .75f, .735f, .613f, .051f, 1, 1, .946f, .429f),
		LW_TREE_WAVE_ENEMY_N(0, .003f, 0, .006f, .25f, .036f, .002f, .001f, .5f, .343f, .023f, .002f, .75f, .735f, .139f, .051f, 1, 1, .728f, .429f),
		/** The pulse's ground ring (8004D4075AFE r3, HDR 2) and the flower-band wall (8001794386A1 r3, HDR 2). */
		LW_TREE_RING_N(0, 0, 0, 0, .25f, .452f, .398f, .079f, .5f, .5f, .459f, .217f, .75f, .56f, .537f, .401f, 1, 1, 1, 1),
		LW_TREE_WALL_N(0, 0, 0, 0, .25f, .167f, .159f, .046f, .5f, .297f, .285f, .092f, .75f, .464f, .447f, .149f, 1, 1, 1, 1),
		/** The thorn 122CA (8001A48833A6 r2, HDR 30): purple-magenta to white. */
		LW_THORN_N(0, 0, 0, 0, .25f, .115f, .01f, .122f, .5f, .185f, .058f, .192f, .75f, .469f, .264f, .478f, 1, 1, 1, 1),
		LW_THORN_ENEMY_N(0, 0, 0, 0, .25f, .1f, 0, 0, .5f, .128f, 0, 0, .75f, .394f, 0, 0, 1, 1, .999f, .999f),
		/** The thorn's tracer (8003062BE28B r3, HDR 12; falls: white at 0 to black at 1, reversed by age, M). */
		LW_THORN_TRAIL_N(0, 0, 0, 0, .25f, .749f, .158f, .643f, .5f, .418f, .056f, .368f, .75f, .162f, 0, .149f, 1, 0, 0, 0),
		/** The thorn's muzzle flash (80082222A07E r0) and hit burst (8009F205934A r0). */
		LW_THORN_FLASH(0, 0, 0, 0, .25f, 1, .058f, .588f, .5f, 1, .191f, .712f, .75f, .981f, .381f, .826f, 1, .939f, .61f, 1),
		LW_THORN_HIT(0, 0, 0, 0, .25f, .273f, .067f, .362f, .5f, .567f, .238f, .237f, .75f, .862f, .617f, .115f, 1, .991f, 1, 0),
		/** The platform's throw and rise trails (800EA70DA27E r2, HDR 5): pink to white; enemy r3. */
		LW_PLATFORM_TRAIL_N(0, 0, 0, 0, .25f, .391f, .086f, .347f, .5f, .398f, .087f, .353f, .75f, .469f, .103f, .416f, 1, 1, .687f, .956f),
		LW_PLATFORM_TRAIL_ENEMY_N(0, 0, 0, 0, .25f, .391f, .012f, .012f, .5f, .398f, .013f, .013f, .75f, .469f, .015f, .015f, 1, 1, .687f, .687f),
		/** The platform's bloom outline 122DA (8008CBB37314 r2, HDR 5): mauve to white. */
		LW_PLATFORM_FLOWER_N(0, 0, 0, 0, .25f, .067f, .032f, .055f, .5f, .16f, .076f, .133f, .75f, .387f, .183f, .322f, 1, 1, 1, 1),
		/** The raised platform's petal-tip glow (8009496E3E46 r0) and cup (8003C2A25A98 r5; enemy r6). */
		LW_PETAL_GLOW(0, 0, 0, 0, .25f, .828f, .127f, .676f, .5f, .824f, .221f, .707f, .75f, .823f, .318f, .741f, 1, .831f, .405f, .778f),
		LW_PETAL_CUP(0, 0, 0, 0, .25f, .684f, .167f, .588f, .5f, .77f, .334f, .709f, .75f, .856f, .498f, .828f, 1, 1, 1, 1),
		LW_PETAL_CUP_ENEMY(0, 0, 0, 0, .25f, .684f, .167f, .167f, .5f, .77f, .334f, .334f, .75f, .856f, .498f, .498f, 1, 1, 1, 1),
		/** The dash ribbons (800955CE7D8A r2, HDR 3, falls): white to cream gold (L which row). */
		LW_DASH_N(0, 0, 0, 0, .25f, .888f, .572f, .312f, .5f, .79f, .509f, .278f, .75f, .704f, .454f, .248f, 1, .667f, .43f, .234f),
```

Notes on the palettes: the clamped HDR rows (LW_PINK, LW_GOLD, LW_BLOSSOM_TRAIL, LW_VINE, LW_THORN ...) go white from a
quarter up, as the game's bloom does; the `_N` rows keep the hue and are the ones for thin bands and small sparkles.
Rows that fall (LW_PETAL, LW_THORN_TRAIL, LW_DASH) are bright at mask 0: give them a mask that is dark where they should
be bright, or use them by age (M). LW_PETAL_AGE is a tint over a particle's life, not a mask palette.
