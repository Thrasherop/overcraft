# Brigitte's ability effects in the game data

What Brigitte's effects are made of in real Overwatch, where the extracted files are, and how to rebuild each one in
Minecraft. Read 2026-11-01 from the archived offline build 154088 (prometheus-2_25_0_0-154088). Overwatch and Battle.net
were checked closed (`Get-Process Overwatch, Battle.net`) before every TankLib run, and `owinit.py` refuses on its own;
everything was read only, offline, through TankLib/DataTool. Text only in the repo: the textures and meshes are Overwatch
assets and stay in the owner's archive; they reach the game only through the local asset pack. The format is
[sombra-effects.md](sombra-effects.md)'s (itself [ana-effects.md](ana-effects.md)'s); this file adds what is different
for her. Her data map: [brigitte-map.md](brigitte-map.md); numbers: [brigitte.md](brigitte.md); plan:
[brigitte-plan.md](brigitte-plan.md); the lesson: [reading_game_data](../../techniques/game-data/reading_game_data.md#where-a-melee-heros-effects-live-brigittes-pass-2026-11-01).

Confidence after each number or claim: **H** read in the data, **M** inferred (one link guessed), **L** guess. "Size" is the
spawn size value: it scales the mesh (a 1 m quad at size 0.5 is 0.5 m; a ribbon's size is its width). "a-b" is a random
value between the two. "Ours" marks what no data gives. Palettes: `BRIGITTE_*` (below and in
`E:\Overwatch Archive\research\brigitte\work\fx_palettes.txt`, every one starting at black at 0). Pack masks:
`brigitte_*` (end of this file).

## Files

`E:\Overwatch Archive\research\brigitte\fx\Brigitte\<Ability>\` with `<Ability>` one of `RocketFlail`, `WhipShot`,
`RepairPack`, `BarrierShield`, `ShieldBash` (the bash, its trail, hit and the stun), `Inspire`, `Rally`: `textures\`,
`ramps\` (float LUTs), `models\Models\<id>.00C\`, `manifest.json`. Same layout as Sombra's. `BarrierShield\models` also
holds the shield mesh 1064F exported **with the barrier look 13917** (its field masks copied into `textures\`), and
`ramps\800E79730886.json` the field material's LUT.

Research files in `E:\Overwatch Archive\research\brigitte\work\`: `fx_cards.txt` (one card per renderer: what this note
was written from), `fx_digest.txt`, `fx_overview.txt` (one line per effect), `fx_summary2.txt|json`, `fx_meshes2.json`,
`fx_palettes.txt`, `fx_sheets\tex_<Ability>.png` (every mask), `fx_sheets\meshes_<n>.png`, `fx_sheets\barrier_5B2C.png`,
`fx_effects\` (decoded effects), `fx_graph_effects.json` / `fx_graph_nodes.txt` (every effect her graphs name, with the
nodes leading in), `fx_entities.txt`, `fx_contactsets.json` / `fx_impacts.txt` (the hit contact sets), `fx_barrier_materials.txt`,
`fx_extract_log.txt`.

Scripts in `E:\Overwatch Archive\research\brigitte\scripts\fx\`: Sombra's `fx_sombra_*` copied as `fx_brigitte_*`, paths
retargeted (Sombra's own untouched; the `fx_sombra_*` files already in that folder are the data-map pass's unretargeted
seed copies, unused). `fx_brigitte_extract.py` holds her effect list, **including the clips' animation effects (08F)**.
New: `fx_brigitte_stu.py` (STU dump of any asset), `fx_brigitte_rawrefs.py` (asset refs in a non-STU file),
`fx_brigitte_submeshes.py` (submesh -> material key), `fx_brigitte_lut.py` (a look material's LUT). TankLib scripts run
under the kiriko-anim venv's Python with `-I`; the others under `py -I run.py`.

Counts: 125 effects extracted (72 her graphs and entities name, 53 clip effects); 31 with renderers, decals, material
effects or lights, the rest sound only or empty. Nothing missing in the extraction.

## What's different from Sombra and the others

- **Her graphs' effects are mostly sound (H).** The flail swing (219B / 219C), its release (223A), the hits (22C4 / 3612),
  the barrier raise / lower (21B6 / 21B9 / 21BA, 6F8D), its low health (0540), the bash start (21BB), the whip launch,
  return and hit (22AD / 22AE / 44EE), every Repair Pack cast / charge / heal tick, Inspire on allies (21EA / 2222) and
  Rally's start, hold and last 3 s (541F / 21C5 / 5420) are sound banks only. **The visuals are clip effects**: the swing
  arcs (clips 4E9E / 4E9F), the bash's jets (5073), the whip head's rocket flare (5276), Rally's flail-head burst (50B0).
- **No hero-owned hit effect on creatures (H).** The flail / whip / bash contact sets (061B, 061D, 0620, 075A) give
  per-surface sounds only (.04A -> .02C). A hit on a creature shows the shared damage response, not hers (ours: a small
  spark, the ground hit's sparks below).
- **Team twins in the LUT rows (H rows, M roles).** Gold / sky-blue rows for her team (her 1P swing uses only that row), red
  / orange for enemies; the barrier: blue (row 0) / red-orange (row 1), Overwatch's friendly-blue / enemy-red barrier rule.
- **1P vs 3P (M).** Under the graph input `v28985` (L: the local view) the chain and whip use an energy ribbon (29D41 squares
  x 19167 noise, BRIGITTE_ENERGY_BLUE / _GOLD); otherwise a plain chain ribbon (13DB9, BRIGITTE_CHAIN). The 1P swing clips
  (4F53 / 4F52) also trail energy ribbons.
- **Screen overlays (H data, L role)** (`ECIM(40)`): the healed ally's screen ring (2074), the stunned player's screen
  (220D), Rally's gold screen flash on her and on allies (219D / 21DE). Ours: HUD tints.

### Hardpoints

| Id | Where | Conf |
|---|---|---|
| 0079 | bone_12E2 at her pelvis height (1.126 m): the swing arcs' origin | H id, M (an animated pivot bone) |
| 0076 (handle 10648) | the chain's start on the flail handle (bone_0596, 0.314 m up the handle) | H |
| 0004 (head 2283) | the chain's end on the flying head | H |
| 0262 (head 10647) | 0.195 m below the head's bone: the rocket flare | H |
| 0256 | 1P head orientation bone: Rally's 1P burst | H |
| 0075 / 0077 | bone_1004 / 1007 on her back, (±0.2, 1.47, -0.19): the bash jets and trail | H, M (back jets) |
| 01D8 | bone_0F74, the shield mount (0.42, 1.39, 0.34): the bash hit flash | H |
| 007B / 007D | the dash speed lines (+(∓0.64, 2.2, 1)) | H id, L place |
| 0012 | the head: the stun halo (+0.5 m) | H |
| 0014 / 0001 | the ally's hips / chest: the pack arrives and heals there | H |
| 00A4 | the sound point | H |

### Meshes

| Id | Shape (model units, x size) | Used for | Pack model |
|---|---|---|---|
| **5F3F / 5F42** | a **horseshoe band** 4.5 x 6.9 m (forehand) / 4.6 x 7.2 (backhand), reaching **6.5 m** down -Y from its origin | the swing arcs (size 1) | `brigitte_swing_arc`, `brigitte_swing_arc_back` |
| 5D48 / 5D64 | the 1P arcs, 3.3 x 5.5 m | 1P swing | - |
| **5DF1** | a **shield-shaped plate** 0.59 x 0.91 (the shield's outline), 0.08 thick | the bash imprint (x1.25 / x2), the break (x2.8 / x2.5) | `brigitte_shield_plate` |
| **5F08** | a **half sphere** r 1 (2 m across, open on one side) | Rally's dome (size 8 = 16 m), the pack's arrival flash | `brigitte_dome` |
| DE0 | an open band (cylinder wall) 0.94 across, 1 tall | the stun halo, Rally | `brigitte_band` |
| 296B | flat ring, outer r 0.64, inner 0.27 | Inspire's ground ring (size 3) | `brigitte_ring` |
| 47D9 | a low ring 0.54 across, 0.19 tall | the whip head's flash | `brigitte_whip_ring` |
| 5BEE | flat ring 2 m, 0.43 thick | Rally's ground ring (size 8) | `brigitte_rally_ring` |
| B18 | a shard 0.2 x 0.19 | the bash hit's streaks | `brigitte_shard` |
| 5A46 / 99F / E71 | ribbon profiles | chains, the dash ribbon, the pack trail | - |
| F5B / 801 / 8C7 / 5C75 | quads / screen quads | sprites, overlays | - |
| 1064F | the shield, 21 bones, 2 submeshes; look 13917 = the barrier | the barrier field | `brigitte_shield.glb` (already in the pack) |

## Rocket Flail

| Part | Effect | Conf |
|---|---|---|
| Swing arc (3P) | clip 4E9E (forehand) effect **26EB**, 4E9F (backhand) **27CC**, 0.6 s, on hp 0079 | H |
| Swing (1P) | clips 4F53 / 4F52 effects **2624** / **262C** (arc 5D48 / 5D64 + two chain ribbons from hp 0004) | H |
| Chain while extended | **216C** (3P) / **33C1** (v28985), handle hp 0076 -> head 2283 hp 0004, 0.6 s, gated by v478 | H |
| Ground hit | **220B** at v1724, the point a 2 m ray finds straight down 6 m ahead (#611 / #614) | H |
| Hit on a creature | none of hers (above) | H |
| Swing / release / hit sounds | 219B / 219C / 223A / 22C4 / 3612 / 19C2 | H |

**Swing arc, 26EB / 27CC (H):** four renderers on the horseshoe mesh 5F3F (backhand 5F42), each **one spawn at 0.067 s**
(backhand 0.1 s), **size 1** (so 6.5 m reach, ~4.5 m wide), life **0.5 s** (masks 14E79 edge line x 14E13 streak x 18F81
flow) and **0.4 s** (14E13 x 4DB noise x 14E79, the coloured layers). Alpha over life rises then falls (0 -> 0.34 at 40% ->
0.45 at 60% -> 0 at the end, M: the 12BB / 65BC rows). Colours: team **BRIGITTE_SWING** (black, amber #FDBC0E, tan, sky
blue #8DD5FF, HDR 2) / enemy **BRIGITTE_SWING_ENEMY** (black, red, orange, pale gold); a violet-blue param row.

**Chain, 216C (3P) (H):** a ribbon (profile 5A46) **0.075 m wide**, from the handle to the flying head, held 0.6 s per swing,
mask 13DB9 (a band bright down its middle), **BRIGITTE_CHAIN** (olive -> yellow -> white, HDR 10) / enemy row dark red ->
pink. 33C1 (1P, M): the same ribbon with 29D41 squares x 19167 noise, **BRIGITTE_ENERGY_BLUE** and **_GOLD** (HDR 3.5).

**Ground hit, 220B (0.5 s) (H):** two dust sprites 14D1C (2 frames, colour with alpha) at **1 m** and **0.5 m**, **4 each**,
2.5-5 m/s, stretched x5, life 0.25-0.75 s, tinted **BRIGITTE_GROUND_DUST** (red -> gold, HDR 1.5); spark streaks DA0
**0.2-0.4 m** and 0.1-0.2 m, **15 a second** for 0.13 s, 2-4 m/s, stretched x3, rising (2 m -> -0.46 m offsets).

**Recipe:**
- Each swing (0.6 s, the hit window 0.2-0.4 s): from ~0.07 s draw `brigitte_swing_arc` (backhand `_back`, mirrored by the
  model) at her pelvis, facing her yaw, size 1 (6.5 m: matches the 6 m range), masked `brigitte_swing_band` (14E13 x
  14E79), additive, BRIGITTE_SWING_N for her team / _ENEMY_N, alpha 0 -> 0.45 (60% of life) -> 0 over 0.5 s. Ours: if the
  model is too heavy, a fan of `brigitte_swing_streak` STREAK quads along a 6 m arc.
- The chain: a 0.075 m `brigitte_chain_band` STREAK from her right hand to the flail head while the head is out (0.6 s),
  BRIGITTE_CHAIN_N; in first person BRIGITTE_ENERGY_BLUE over `brigitte_energy_squares`.
- When the swing's 6 m-ahead point is within 2 m of the ground: 4 `brigitte_dust` puffs 0.5-1 m (frame by age, NATURAL
  with the GROUND_DUST tint) and ~2 `brigitte_spark_streak` sparks rising 2-4 m/s for 0.13 s.
- On a creature: ours, 2 tiny orange `spark`s (no data).

## Whip Shot

| Part | Effect | Conf |
|---|---|---|
| Launch (0.2 s) | head clip **5276** effect **2770** on the head's hp 0262 (the rocket), 0.5 s; 3P clip 4F30 (2A65): sound | H clip, M state (the head's first whip clip) |
| Flight and return | the head entity 2283 moved by 1259 #640 + chain ribbons **216E / 22CF** (3P) or **33C2 / 33C3** (v28985), hp 0076 -> 0004, 1.2 s | H |
| Hit | **44EE** at the knockback: sound only; no visual | H |
| Return | 22AE held: sound; return clips 51CA / 5056 / 51EB / 51EC: sounds | H |
| Whiplash (perk) | 551F: 02B1 / 6582 / 6584: sounds | H |
| Others | 3398 (material effect on the flail, v28985: energy glow, M 1P), 7E48 / 7E7F (1259 #1028 StateMoveTo: L a Stadium pull) | H nodes, L role |

**Rocket flare, 2770 (0.5 s, from 0.2 s = the launch) (H):** on the head, four jets of sparks 4C09 x 5DF x 4DB **0.2-0.26 m**
at **64 a second**, 8.2-9.6 m/s backwards (four offsets ±0.15 m round the nozzle), life 0.05-0.1 s, **BRIGITTE_WHIP_SPARK**
(red-orange -> cream) and _ALT; two flash rings 47D9 (masks 153AF x 1484C) **size 1.5-1.65 (0.8-0.9 m)**, 25 a second,
life 0.3-0.35 s, violet -> peach (HDR 5); at 0.37 s **3 smoke puffs** DFCB (32 frames) **1.2-1.5 m** stretched x2.5-5, 2-4 m/s,
life 1-2 s, **BRIGITTE_WHIP_SMOKE** (grey-brown -> cream); 60 tiny sparks a second (1F35, 6 cm); two lights 2.

**Chain, 216E / 22CF (H):** ribbons 5A46 **0.06 m / 0.075 m** wide, 13DB9 band, BRIGITTE_CHAIN rows, 1.2 s (the whole flight
and return); 33C2 / 33C3 the energy versions (BRIGITTE_ENERGY_BLUE / _GOLD).

**Recipe:**
- At 0.2 s (launch): on the flying head, a rocket flare: 4-8 `cat_soft` sparks a tick shooting back 9 m/s (0.25 m, 1-2 ticks),
  BRIGITTE_WHIP_SPARK; a 0.85 m `brigitte_whip_ring` model (or `brigitte_whip_flash` sprite) for 0.3 s; at 0.37 s 3 grey
  `brigitte_smoke` puffs 1.2-1.5 m (frame by age over 1-2 s), BRIGITTE_WHIP_SMOKE, blended.
- In flight (80 m/s, 20 m) and back: the flail head model on a 0.06-0.075 m chain (`brigitte_chain_band` STREAK from her hand
  to the head), BRIGITTE_CHAIN_N; first person BRIGITTE_ENERGY_BLUE.
- The hit: no effect of its own (sound 44EE); the knockback reads. Ours: 2 orange sparks.

## Repair Pack

| Part | Effect | Conf |
|---|---|---|
| Throw | 1P clip 4F5A (27DA): sound; 0EDD casts (2EC3-2EC8, 009B, 5F58): sounds | H |
| **In flight** | projectile 22DB -> child 22DC -> **218D** (2 s loop, on hp 0014) | H |
| **Arrival** | **2F2E** on the ally's hp 0014 / 0001 (0EE1 #513; again for a second pack #514) | H |
| **Heal over time** | **2074** held on the ally (0EE1 #631, the 2 s chase) | H |
| Heal sounds | 217E / 2ECF / 2ED0 / 2ED3 / 2EDE / 434A | H |

**In flight, 218D (H):** a core glow A7B6 **0.5 m** (2 at once, life 0.7 s, refreshed), **BRIGITTE_PACK_CORE** (olive ->
cream, HDR 6); two ribbons behind it: **0.2 m** wide (profile E71, 64 points a second, life 0.9 s, 18B18 x 44F7 flame wisps,
**BRIGITTE_PACK_RIBBON** gold HDR 6 / _HOT orange) and **0.5 m** wide (life 1 s, 18B18 x 18E48, **BRIGITTE_PACK_TRAIL**
yellow); sparks 1F35 **120 -> 75 a second**, 0.15-0.3 m, 1-5 m/s, stretched x2, life 0.14-0.32 s, orange (tint (10, 4.4,
0)); a lens streak 23C0 **0.3 m** (BRIGITTE_PACK_LENS); a light 3. No model: the glow is the pack (H).

**Arrival, 2F2E (1 s) (H):** a lens streak 23C0 **0.2 -> 0.55 m** (by dataflow 5FD, L the heal), life 0.5 s, BRIGITTE_PACK_ARRIVE;
a dome flash 5F08 at **size 0.92 -> 1.5** (1.8-3 m), life 0.3 s, BRIGITTE_PACK_DOME (cream -> yellow -> orange, HDR 5);
two glows 174D8 **1 -> 2.2 m**, life 1 s, BRIGITTE_PACK_HEAL_GLOW; a light 1.6 -> 3 on the head.

**Heal over time, 2074 (held 2 s; on hp 0001) (H values, M roles):** a swirl 1268 (colour sprite, yellow) **1.2-1.5 m**, 3 at
the start then 1 a second, life 1 s; a sphere 85B **1-1.2 m** once a second (life 1.2 s, flat mask, blue-violet rows: L);
**rising dots** 126A (colour, yellow) **0.5-0.6 m**, **20 a second**, 0.07-0.42 m/s within 30°, spinning, life 0.6-1.3 s;
the healed player's screen ring 52E8 (`cat_halo_ring`, `ECIM(40)`), 3 a second, gold tint.

**Recipe:**
- The pack (55 m/s, homing): a 0.5 m `glow` (BRIGITTE_PACK_CORE_N, additive) with a `Fx.trail` 0.2 m of `brigitte_pack_trail`
  (BRIGITTE_PACK_RIBBON_N, 0.9 s; ours: 0.5 s) plus 2 orange `spark`s a tick and a 0.3 m `lw_lens` (BRIGITTE_PACK_LENS).
- Arrival at the ally's hips: a 2-3 m `brigitte_dome` flash (BRIGITTE_PACK_DOME_N, 0.3 s), a 1-2 m `brigitte_soft_blob_b`
  glow growing over 1 s (BRIGITTE_PACK_HEAL_GLOW_N), a 0.5 m `lw_lens` flash.
- While it heals (2 s): one 1.3 m `brigitte_heal_swirl` a second on their chest (NATURAL, blended), and 1 `brigitte_heal_dot`
  every tick drifting up 0.2 m/s; the healed player's screen: a gold `cat_halo_ring` edge (HUD).

## Barrier Shield

| Part | Effect | Conf |
|---|---|---|
| **The field** | the pet 2284 drawn with model 1064F and look **13917** (not an effect) | H |
| Raise / lower / button | 21B6 (6F8D in Rally) / 21B9 / 21BA: sounds; clips 4FF3 / 5118 / D591 / D5C4 / D5C5: empty or sounds | H |
| The fade in | 0EE4 #245 v6728 over 0.075 (M: seconds) | M |
| Hit flashes | **not found** as effects (L: the look's material effects .065, not decoded) | - |
| Below 50% / 25% | **0540**: sound only | H |
| **Break** | **268C** at the pet's position v431 (0EE4 #152) | H |
| Hand shield | 24C5 -> 24B8 -> 22B6: two lights 1.25 -> 2 | H |

**The field (H textures and keys, M blend / roles):** look 13917 maps the mesh's two submeshes: **Submesh_1** (478 vertices,
key 6B24EE2D2B083AFE: the field) -> material **5B2C**; **Submesh_0** (4348 vertices: the frame) -> **629A**, a shader-0x32
material with **no textures** (static inputs 0; header floats 0.5 grey: L a plain edge). 5B2C (shader 0x32, material data
9D04) samples:

| Key | Texture | What it is |
|---|---|---|
| D20435D3 / BD89FFFC | **14D13** 128² BC4 | the field's **frame**: a bright border with a small centre emblem (`brigitte_barrier_frame`) |
| 42BB2842 | **116E** 256² BC4 | **hex lines** (`brigitte_barrier_hex`) |
| 35BC18D4 | **10A7** 128² BC4 x16 | a 16-frame **hex-cell flicker** (`brigitte_barrier_flicker`) |
| 2480AE46 | **10D8** 128² BC4 | a wavy vertical **sweep line** (`brigitte_barrier_sweep`) |
| A26EC15C | 1161 256² BC5 | the hex normal map: refraction (skip) |
| 5118206D | **800E79730886** 256 x 3 float | **the colour LUT**: row 0 team, row 1 enemy, row 2 params |

Colours, read by the masks' brightness (M, as her effects): **team BRIGITTE_BARRIER** (LUT row 0: a dim red at 0, then navy
#353C75, blue #1785BF, **cyan #17E0F7** at full; its alpha rises 0.08 -> 0.25 -> 0.58 -> 0.88 -> 1); **enemy
BRIGITTE_BARRIER_ENEMY** (row 1: dark red, red #F33900, orange #FFA200, white; alpha 0 at 0 then 1). Blend: translucent,
added glow (M: the shader-0x32 family is her holograms' and Sombra's).

Size (M): the field is the skinned shield (rest pose 0.59 x 0.91 m) spread by its held pose (clip 5118); the break's silhouette
draws 5DF1 (the same outline, 0.59 x 0.91) at **size 2.8: 1.65 m wide x 2.56 m tall** (2.5: 1.47 x 2.28): read that as the
deployed barrier's size. x1.2 in Rally (the map).

**Break, 268C (1 s, all at 0) (H):** at the barrier (+(0, 0, -0.25), turned 90°): the **shield silhouette flash** 5DF1 at **2.8
and 2.5**, life **0.18 s**, BRIGITTE_BREAK_SHIELD (cream -> blue, HDR 4) / _ENEMY (cream -> red), and a glow 17C0 on it
(BRIGITTE_BREAK_GLOW / _ENEMY); **12 hexes** 2B0E / 2B10 (quad EE4) **0.4-0.65 m**, stretched x-5, 3-5 m/s, life 0.2-0.27 s,
BRIGITTE_BREAK_HEX (black, blue, black) / red; **50 shards** 1F36 (blue) / 1F37 (orange) colour sprites, 4 frames, **0.05-0.14 m**,
**4.4-9.4 m/s** in a 65° cone up, spinning, life 0.25-1 s; 8 + 8 glow streaks 139C x strips 21E1 (blue) / 21E2 (orange)
0.32-0.93 m, life 0.2 s; 16 + 16 flames 14F9 0.2-0.6 m; 8 chips 5DB2 (10 frames) 0.25-0.7 m, strips 2062 / 20D0. Sounds 1696 / 1697.

**Recipe:**
- The field (while up): the `brigitte_shield.glb` mesh's field submesh in its deployed pose (or a 1.65 x 2.56 m quad shaped
  by `brigitte_barrier_frame`), 1.5 m ahead of her chest, drawn translucent: `brigitte_barrier_hex` (lines) plus
  `brigitte_barrier_flicker` (frame = age x ~16 fps, ours) and a `brigitte_barrier_sweep` line scrolling up, all through
  BRIGITTE_BARRIER for her team / BRIGITTE_BARRIER_ENEMY for enemies, alpha ~0.5 (ours, from the LUT alpha); fade in over
  0.075 s at the raise. The frame submesh: ours, the plain shield look (13265) or a thin cyan edge.
- Hit flashes (ours, no data): brighten the hex layer for 0.1 s on each hit.
- Break: a 1.65 x 2.56 m `brigitte_shield_plate` flash (0.18 s, BRIGITTE_BREAK_SHIELD_N), 12 `brigitte_hex_outline` /
  `brigitte_hex` pieces 0.5 m flying 3-5 m/s (BRIGITTE_BREAK_HEX_N), 20-50 `brigitte_shard_blue` (enemy view:
  `_orange`) shards 5-14 cm thrown up 4-9 m/s with gravity, NATURAL, 0.25-1 s.

## Shield Bash (and its trail, hit, stun and shockwave)

| Part | Effect | Conf |
|---|---|---|
| Start | 21BB held in the 0.3 s dash: sound | H |
| **Jets** | clip **5073** effect **26A8** (0.2 s) at hp 0075 / 0077 (her back) and 0004 | H |
| **The dash** | **2207** (held 0.33 s, #646) | H |
| **Trail** | **2284.00D** on hp 0075 / 0077 from 0.06 s to 1 s after (#685 / #686) | H |
| **Hit** (creature or wall) | **2206** on hp 01D8 (#647 on the hit, #778 on a wall) | H |
| **Stun** (Rally only, and the clash) | **220D** passed to 224E, played on the target; 67BF at its end: sound | H |
| **Shockwave** (Rally) | **no effect node of its own** (#877); each target it hits gets 220D (#887 targets v1184 = #877's list) | H |
| On cooldown | 0186: the shared fail sound | H |

**Jets, 26A8 (0.2 s) (H):** at each back point: a flash 4C09 **0.2-0.23 m** (BRIGITTE_BASH_FLASH black -> gold -> white);
flames **64 a second** for 0.17 s, **8.2-9.6 m/s** straight out (+Z, 5° cone), 0.2-0.26 m, life 0.05-0.1 s (BRIGITTE_BASH_SPARK);
a ring 11C1 x 153A7 at 0.15-0.18 (0.2 m); spark arcs 15455 0.5 m, 15 a second; at her feet (hp 0004) dust 14D1C **0.6-1 m**,
8-12 + 50 a second, 2.5-5 m/s, stretched x5 (the GROUND_DUST rows) and debris 18A2 (colour, 4 frames) 5-10 cm, 35 a second.

**Dash, 2207 (0.33 s) (H):** a ribbon (profile 99F) **0.65 -> 0.4 m** wide, 60 points a second, life 0.2 s, flow 18F81,
BRIGITTE_BASH_RIBBON (L: black, magenta, violet, blue); **speed lines** E99F (`cat_speed_line`, 3 frames) **3.2 m** at hp 007B /
007D, 1-2 at 0.07 s, life 0.1-0.22 s, **BRIGITTE_BASH_SPEED** (sky blue -> grey, team) / _ENEMY (red); and 0.9-2.4 m ones
at (-0.3, 1.25, 0 / 2 m), 125 a second plus 6-7.

**Trail, 2284.00D (2 s) (H):** on the back points: smoke puffs 9EF (colour, 4 frames) **0.5 m** stretched x6.9, 3 a second,
life 0.5-1 s, violet-blue rows (L: params); a glow 242 (`mercy_glow`) 0.5 m for 1.15 s, orange (#FF9E1D, HDR 12); 3-6 sparks a
second 1F35 0.1-0.15 m drifting 0.1-0.3 m/s.

**Hit, 2206 (1 s) (H):** on the shield (hp 01D8): **7-9 streaks** (shard mesh B18) **size 4.5-6 (0.9-1.2 m long)**, stretched x6,
life 0.1-0.3 s, colour strip 2D7F (orange -> yellow) x BRIGITTE_BASH_STREAK; a **shield imprint** 5DF1 at **1.25 (0.74 x 1.14
m)**, life 0.17 s, **BRIGITTE_BASH_SHIELD** (blue, HDR 12) / _ENEMY (red), masks 10A2 hex pattern x 15262 soft shield; its
**edge** 5DF1 at 2.0 (1.2 x 1.8 m) mask 15111, 0.12-0.15 s, BRIGITTE_BASH_EDGE / _ENEMY; a flow layer 5DF1 x3 (18F81 x 153F9,
0.3 s, L distortion).

**Stun, 220D (2 s effect; the stun lasts 0.75 s, M stopped with it) (H):** on the target's head **+0.5 m**: two **halo bands**
DE0 (0.94 m across) at size 1-1.11, **counter-rotating one turn over the effect** (rotation 0 -> ±6.28), masks 4D4 noise x
247C (a colour band of streaks), tint 3, BRIGITTE_STUN_HALO (dim gold); **stars** 248B (colour) 2.5-8 cm, 10 at once + 0-20 a
second, life 0.12-0.24 s; a star glint 4-14 cm 8.5 a second; the stunned player's screen D8F 2 m (`ECIM(40)`). Sounds 46CC-46CE.

**Recipe:**
- The bash (0.3 s): two jets of 2-3 orange `spark`s a tick shooting back 9 m/s from her back (±0.2 m, chest height) with a
  0.2 m `cat_soft` flash (BRIGITTE_BASH_FLASH); 2-4 `brigitte_dust` puffs at her feet; a `cat_speed_line` 3 m on each side
  of her for 0.15 s (BRIGITTE_BASH_SPEED_N her team / _ENEMY_N). Trail: 3 `brigitte_smoke_puff`s a second (0.5 m, stretched)
  behind her for 1 s after.
- The hit: a 0.74 x 1.14 m `brigitte_shield_plate` imprint at her shield, `brigitte_hex_pattern` x `brigitte_shield_soft`,
  BRIGITTE_BASH_SHIELD_N (blue; enemies see red), 0.17 s, plus a 1.2 x 1.8 m `brigitte_shield_front` edge 0.13 s and 8
  `brigitte_shard` streaks 1 m long flying out (BRIGITTE_BASH_STREAK), 0.1-0.3 s.
- Stun (Rally's bash, the shockwave's targets, the clash): over the target's head (+0.5 m) a 1 m `brigitte_band` ring
  (`brigitte_stun_halo`, NATURAL, x BRIGITTE_STUN_HALO_N ours) and a second one counter-rotating, 1 turn / 2 s; a few
  `brigitte_stun_star` glints. For its 0.75 s.
- Shockwave (Rally): no wave of its own in the data: each target hit shows the stun. Ours: nothing more.

## Inspire

| Part | Effect | Conf |
|---|---|---|
| **The pulse** | **2182** at her + (0, 1, 0) at the pulse (12D8 #400) | H |
| On her | 2253: sound (also Inspiring Strike, 51F2 #469) | H |
| On hurt allies | 21EA (start) / 2222 (held): **sounds only** | H |

**2182 (0.5 s) (H):** one flat **ring** 296B (outer r 0.64, inner 0.27) at **size 3 (3.85 m across)**, 0.1 m below the spawn
point (her + 1 m), life **0.55 s**, masks 1535B (rising wisps) x 4DB noise, **BRIGITTE_INSPIRE** (yellow #FFF45D -> cream) and a
cream -> olive row (BRIGITTE_INSPIRE_RISE, HDR 20, falls). Nothing on the allies (H: sounds).

**Recipe:** at each pulse a 3.85 m `brigitte_ring` model (or a FLAT `brigitte_inspire_wisps` disc) round her waist, gold
(BRIGITTE_INSPIRE), additive, fading over 0.55 s. Allies: no visual (the heal number only).

## Rally

| Part | Effect | Conf |
|---|---|---|
| Start / held / last 3 s | 541F / 21C5 / 5420: **sounds only** | H |
| Cast clips | 4FE3 / 5237 / 535A / 4FE2 / 52CC / 535B: empty | H |
| **Flail-head burst** | head clip **50B0** effect **27F1** (3P, 1.2 s) / 1P **26C4** (4FD9, 1.33 s), on hp 0256 / 0076 | H |
| **The pulse** | **219D** on her after 0.5 s (#81, held; on hp 0076 / 01E8 / 0004) + sub-effect **220E** (ground decals) | H |
| On her and her barrier | **22AB** (material effects, 0.5 s) | H node, M look (gold) |
| **On each ally** | **21DE** (material effect + screen flash, 0.67 s) and 2202 (sound) | H |

**Flail-head burst, 27F1 (from 0.17 s) (H):** at the flail head: domes 5F08 **0.4 / 0.8** (0.8-1.6 m) life 0.5 s (flow 18F81,
cyan-white rows); **3 x 45 sparks** 1F35 2.5-5 cm, 0.5-5 m/s, stretched x4, life 0.25-1 s; lens streaks 23C0 **0.2 m / 1 m**
(BRIGITTE_RALLY_LENS gold / _ENEMY), flares 2F4F0 **0.35 / 2 m** (BRIGITTE_RALLY_FLARE gold -> white / _ENEMY); bokeh 187EB x
1DC68 0.2-0.75 m (BRIGITTE_RALLY_BUBBLES / _ENEMY); gold dome layers 0.175-1.25 (BRIGITTE_RALLY_GOLD / _ENEMY); two lights 2 ->
7 over 0.3 s.

**The pulse, 219D (0.5 s; 128 components) (H):** on her: the **dome** 5F08 at **size 8 (16 m across: the 8 m reach, M)**,
life 1 s, mask 4DB, **BRIGITTE_RALLY_DOME** (sky blue -> gold, team) / _ENEMY (peach -> red), plus size-3 and size-8 layers
(life 0.6-1 s); an **armour-pattern shell** 5F08 at **0.6 (1.2 m)** round her, masks 144E5 x 14486 (`brigitte_rally_armor`),
life 0.8 s, **BRIGITTE_RALLY_ARMOR** (gold) / red; bokeh shells 0.2-0.6; a **ground ring** 5BEE at **size 8 (16 m)**, mask
1484C, life 0.75 s; a band DE0; **rising sparks** 1F35 2.5-7.5 cm, **50 -> 1 a second** over the 0.5 s, 0.5-2 m/s, stretched x4,
life 0.25-1.5 s; flares 2F4F0 **0.5-2 m** and lens streaks 23C0 0.25-1 m at the start; her screen 5C75 (gold, `ECIM(40)`).
**Ground decals, 220E (0.5 s):** three decals **4 x 4 m** (1-2 deep), 14C71 disc / 14D09 ring x 18F00, **BRIGITTE_RALLY_DECAL**
(gold -> amber) / _ENEMY (orange -> red).

**On each ally, 21DE (0.67 s) (H):** a material effect on their body (masks 1298 flat x 1CA8C), **BRIGITTE_RALLY_ALLY** (black
-> brown -> amber -> gold, HDR 2; the red row for enemies, M) and a gold screen flash 5C75 (`ECIM(40)`), every time 12C1 grants
overhealth (every 0.5 s while it tops them up, M).

**Recipe:**
- At 0.17 s into the cast: at the flail head a 2 m gold `brigitte_flare_star` (BRIGITTE_RALLY_FLARE_N) and 1 m `lw_lens`,
  30-45 `spark`s 0.5-5 m/s, a 1 m gold dome flash.
- At 0.5 s (and held while Rally runs, the effect replays per its 0.5 s length, M): round her a 16 m `brigitte_dome` sweep
  (BRIGITTE_RALLY_DOME_N, alpha low, blended, 1 s), a 16 m `brigitte_rally_ring` flat ring, a 1.2 m `brigitte_rally_armor`
  shell on her (BRIGITTE_RALLY_ARMOR, 0.8 s), sparks rising; a 4 m gold `brigitte_rally_disc` / `brigitte_rally_decal_ring`
  on the ground (KirikoFx.groundY).
- On each ally topped up: a gold body tint (BRIGITTE_RALLY_ALLY, 0.67 s: the overlay as Sombra's teleport body flash) and a
  gold HUD flash on their screen.

## Palettes (EffectArt, `research\brigitte\work\fx_palettes.txt`)

56 lines, each `BRIGITTE_<NAME>(stop, r, g, b, ...)` with HDR rows also given `_N` (normalised by the peak). **Every palette
starts at exact black at 0** (rows that start coloured get black at 0 and the game's values at .25 / .5 / .75 / 1; rows that
start near black are forced to black) so the masks keep their shape. Names: SWING / _ENEMY / _1P, CHAIN / _ENEMY,
ENERGY_BLUE / _GOLD, GROUND_DUST, WHIP_SPARK / _ALT, WHIP_SMOKE, BARRIER / _ENEMY, BREAK_SHIELD / _GLOW / _HEX (+ _ENEMY),
BASH_FLASH / _SPARK / _SPEED (+ _ENEMY) / _RIBBON / _SHIELD (+ _ENEMY) / _EDGE (+ _ENEMY) / _STREAK, STUN_HALO, INSPIRE /
_RISE, PACK_RIBBON / _RIBBON_HOT / _TRAIL / _CORE / _LENS / _ARRIVE / _DOME / _HEAL_GLOW, RALLY_DOME / _GOLD / _ARMOR /
_BUBBLES / _FLARE / _LENS / _ALLY / _DECAL (+ _ENEMY). The script: `fx_brigitte_palettes.py` (Sombra's, with full LUT ids
and the black-at-0 rule enforced).

## In the pack

Built 2026-11-01 into the dev pack (`OC_ASSET_PACK=E:/claude/mods/overcraft_ana/local-assets/pack py
tools/build_asset_pack.py effects "E:\Overwatch Archive\research\brigitte\fx"`), entries in `tools/build_asset_pack.py`
(EFFECT_MASKS / PRODUCTS / EFFECT_MODELS, `brigitte_*`). All present.

- **Masks** (`textures/effect/`; flipbooks `_<frame>`): flail `brigitte_swing_streak` (2), `brigitte_swing_edge`,
  `brigitte_chain_band`, `brigitte_dust` (2, colour + alpha), `brigitte_spark_streak` (2), `brigitte_energy_squares`; whip
  `brigitte_whip_flash`, `brigitte_smoke` (32); barrier `brigitte_barrier_frame`, `brigitte_barrier_hex`,
  `brigitte_barrier_flicker` (16), `brigitte_barrier_sweep`, `brigitte_hex_outline`, `brigitte_hex`, `brigitte_shard_blue`
  (4, colour), `brigitte_shard_orange` (4, colour), `brigitte_chip` (10), `brigitte_shield_glow`; bash `brigitte_shield_front`,
  `brigitte_shield_soft`, `brigitte_shield_shape`, `brigitte_hex_pattern`, `brigitte_spark_arcs` (2), `brigitte_debris` (4,
  colour), `brigitte_smoke_puff` (4, colour), `brigitte_stun_halo` (colour), `brigitte_stun_star` (colour), `brigitte_line_v`,
  `brigitte_noise_fine`; Inspire `brigitte_inspire_wisps`; Rally `brigitte_rally_armor_a` / `_b`, `brigitte_flare_star`,
  `brigitte_rally_disc`, `brigitte_rally_decal_ring`, `brigitte_fresnel`, `brigitte_soft_square`, `brigitte_soft_blob`,
  `brigitte_cloud_a` / `_b`; Repair Pack `brigitte_heal_swirl` (colour), `brigitte_heal_dot` (colour), `brigitte_flame_wisps`,
  `brigitte_soft_spot`, `brigitte_soft_blob_b`.
- **Products:** `brigitte_swing_band` (14E13 x 14E79), `brigitte_rally_armor` (144E5 x 14486), `brigitte_pack_trail` (18B18 x
  44F7).
- **Shared masks already in the pack** her recipes use: `glow` (A7B6), `spark` (1F35), `cat_soft` (4C09), `mercy_glow` (242),
  `mercy_noise` (4DB), `lw_lens` (23C0), `lw_bokeh` (187EB), `cat_cloud` (18E48), `cat_halo_ring` (52E8), `cat_speed_line`
  (E99F), `sombra_line_thin` (1484C), `lw_thorn_flame` (14F9), `mercy_soft` (139C), `cat_glow_noise` (19167).
- **Models** (`hero_models/`): `brigitte_swing_arc`, `brigitte_swing_arc_back`, `brigitte_shield_plate`, `brigitte_dome`,
  `brigitte_band`, `brigitte_ring`, `brigitte_shard`, `brigitte_whip_ring`, `brigitte_rally_ring`.

## Not found

- **Hit visuals** on creatures for the flail, whip and bash: none of hers (contact sets are sounds; the shared damage
  response wasn't traced).
- **The barrier's hit flashes:** no effect node; the look 13917 carries 37 material-effect slots (.065 ids such as 054D /
  0553 with .127 materials B86D-B870, 6E23-6E30) that likely hold the damage / Rally / low-health looks, not decoded.
- **The barrier's exact deployed size:** the field is skinned; its pose in clip 5118 wasn't measured (M estimate from the
  break silhouette: 1.65 x 2.56 m). Measure `brigitte_shield.glb` posed by 5118 in Blender.
- **The frame material 629A**: no textures, no tint (L: a plain edge or hidden).
- **Which twin is whose:** no node that picks the gold-blue / red-orange rows was decoded (M from her 1P using the gold-blue
  row alone, and Overwatch's blue-friend / red-foe barrier rule).
- **`v28985`'s meaning** (M: the local / 1P view) for the energy-versus-chain ribbons.
- **Rally's held repeat:** 219D is 0.5 s long and held by #81 (M: it loops while Rally runs); 22AB's material rows were empty.
- **7E7F / 7E48** (1259 #1028 StateMoveTo): not placed in the PvP kit (L Stadium).
