# Sombra's ability effects in the game data

What Sombra's effects are made of in real Overwatch, where the extracted files are, and how to rebuild each one in
Minecraft. Read 2026-11-01 from the archived offline build 154088 (prometheus-2_25_0_0-154088). Overwatch and Battle.net
were checked closed before every session; everything was read only, offline, through TankLib/DataTool. Text only: the
textures and meshes are Overwatch assets and stay in the owner's archive. They reach the game only through the local
asset pack. The format is [ana-effects.md](ana-effects.md)'s (itself `lifeweaver-effects.md`, `mercy-effects.md`,
`jetpack_cat-effects.md`, `kiriko-effects.md` and `lucio-effects.md`'s); this file adds what is different for her. Her
data map (graphs, numbers, clips): [sombra-map.md](sombra-map.md); the numbers: [sombra.md](sombra.md); the build plan:
[sombra-plan.md](sombra-plan.md).

Confidence after each number or claim: **H** read in the data, **M** inferred (one link guessed), **L** guess.
"Size" is the spawn size value: it scales the mesh (a 1 m quad at size 0.5 is 0.5 m; a ribbon's size is its width).
A range "a-b" is a random value between the two. "Stretch" is spawn attribute 6DFA. "xA -> xB over life" is a 12BB
over-life row that multiplies the size (Ana's Zs, `ana-effects.md`). "Ours" marks what no data gives.

Her live kit only (the 2026-10-06 Support rework): Machine Pistol, Hotfix, Cyberspace, Translocator (+ stealth), EMP, and
the hacked status they put on targets. **Left out** (below): her old Hack / Virus / Opportunist, the Stadium-gated effect
nodes and the Community Crafted beacon's extras.

## Files

`E:\Overwatch Archive\research\sombra\fx\Sombra\<Ability>\`, with `<Ability>` one of `MachinePistol` (tracer, muzzle,
impact), `Hotfix` (the strands, the heal on an ally, the hack strands), `Hack` (the hacked status on heroes, health packs,
deployables), `Cyberspace` (cast, data cube in flight, burst, zone, weakened enemy), `Translocator` (beacon, teleport,
stealth, detection ring) or `EMP` (cast, pulse):
- `textures\<id>.png` (flipbooks: `<id>.png`, `<id>_1.png` ...), `ramps\<id>.json` (float LUTs, `[row][x][r,g,b,a]`),
  `models\Models\<id>.00C\` (effect meshes), `manifest.json` (per effect: emitters paired with their renderers,
  hardpoint table, spawn shapes, raw track nodes). Same layout as `research\ana\fx\Ana`.
- Not under `extracted\`: pass `research\sombra\fx` to `build_asset_pack.py effects` as the source (done, below).
- Research files in `E:\Overwatch Archive\research\sombra\work\`: `fx_cards.txt` (one compact card per renderer: the file
  this note was written from), `fx_digest.txt` (every renderer with every LUT row, spawn attribute and renderer track),
  `fx_overview.txt` (one line per effect), `fx_summary2.txt|json`, `fx_meshes2.json`, `fx_palettes.txt` (EffectArt
  palettes of the rows below), `fx_strips.txt` (the colour strips sampled), `fx_sheets\tex_<Ability>.png` (every mask),
  `fx_sheets\meshes_<n>.png` (mesh wireframes), `fx_effects\` (decoded effects), `fx_graph_effects.json` (every effect each
  of her graphs names, per node), `fx_graph_nodes.txt` (those nodes with their hardpoints and the nodes leading in),
  `fx_entities.txt` (her entities' effect slots), `fx_extract_log.txt`.
- Scripts in `E:\Overwatch Archive\research\sombra\scripts\fx\`: Ana's `fx_ana_*` copied as `fx_sombra_*` with their paths
  retargeted (Ana's own untouched); `fx_sombra_extract.py` holds Sombra's effect list; new: `fx_sombra_graph.py` (Ana's
  `fx_graph_ana.py` retargeted), `fx_sombra_nodes.py` (effect nodes with context), `fx_sombra_ents.py` (entity effect
  slots). TankLib scripts run under the kiriko-anim venv's Python with `-I`; the others under `py -I run.py`.

Counts: 72 effects extracted (43 with renderers, decals or material effects; the rest sound only or empty), 416
texture extractions over the six folders (259 of them float LUTs; shared ones repeat per folder), 39 mesh extractions
(25 distinct). Nothing missing.

## What's different from Ana and the others (format)

- **Team twins carried by colour strips (H strips, M roles).** Most of her renderers come in pairs with the same mask,
  timing and emitter but a different 256 x 8 colour strip (BC1 / BC7): a **purple** one and a **red-orange** one. Purple
  is her colour, so as in Ana's gold / red pairs the purple twin is what her own team sees and the red-orange one what
  enemies see (M). For the heals the pair is **gold / orange** (Hotfix on an ally, Cyberspace's allied side). **Draw the
  purple (or gold) twin only** (ours, as Ana's). The strips (`fx_strips.txt`, sampled at 0 / .25 / .5 / .75 / 1):

  | Strip | Hex | Twin role (M) |
  |---|---|---|
  | **A06C** (BC1) | #730181 #791DB5 #8344EF #9472FC #FFFFFF | her team: purple to lilac to white |
  | **A06B** (BC7) | #010101 #70016C #9722D1 #7E84FF #FEFEFF | her team: black to magenta to periwinkle |
  | A720 | #651B84 #651B84 #662194 #6D44CE #BDAAFF | her team (the hacked skull) |
  | **ADD0** (BC1) | #990000 #D30000 #FA4D00 #FFAA00 #FFFFFF | enemies: dark red to orange to white |
  | ADCF (BC7) | #000000 #B90000 #FD4A00 #FEAA00 #FEFEFC | enemies (A06B's twin) |
  | ADDD | #970000 #B50000 #CE0000 #F72B00 #FFA900 | enemies (A720 / A260's twin) |
  | 5AA0 | #E90000 #FC0000 #FF1905 #FF881E #FCFE3D | enemies (hack icon) |
  | 20D0 | #4C1919 #872703 #E60C00 #FF8D00 #FFFEFF | enemies (hack panels) |
  | **C377** (BC7) | #010101 #85681E #EABB34 #FDFF54 #FFFFFE | Hotfix on an ally: gold |
  | 3FCE | #B85800 #FA8900 #FFB500 #F7F800 #FAFE73 | Hotfix on an ally: amber to yellow |
  | 6E5C | #010101 #6E3901 #BE7401 #EFCC12 #FEFEFC | Hotfix's skull-plus icon: gold |
  | **A260** | #C500DE #EC606E #FFC92C #FFFF5D #FFFEFA | the pistol's muzzle: magenta to gold |
  | B70B | #010101 #DD1DCB #FFAD2B #FFFC46 #FEFEFC | the pistol's muzzle: magenta to gold |
  | 22ED | #555455 #848684 #B8B7B8 #E1E2E1 #FFFEFF | grey (the beacon's smoke) |

- **Over-life size rows make the big shapes (H rows, M reading).** EMP's 15 m pulse is a 2 m mesh: its band 3BC3 and
  spheres DBF / 1385 spawn at **size 2.05** and a 12BB row multiplies them **x1 -> x6.95 (10% of life) -> x14.5 (25%) ->
  x15** (800DB16E / 800E002E). So **2 m to 30 m across (the 15 m radius) in 0.25 s of a 0.5 s / 1 s life**, fast at first.
  The hacked skull grows **x0.82 -> x2.86** the same way (8005961C). Nothing in the graph passes a radius.
- **Per-target effects sit in the per-target branch (H).** Cyberspace's weakened look (7AE1) is played by 6102 #538 under
  #686, whose condition and targets are `self` = each enemy the zone tracks (the same branch sets the -50% and tag 2456).
  The hacked status (0B1F) is a graph run on the target, so all its effects are on the target.
- **Hotfix has two looks (H nodes, M roles).** 614C's State #2197 plays four **gold** strands (7C6E / 7C6D / 7D40 / 7D41)
  and their gold hits on the target (7FF0 / 8010 / 8011 / 8012); State #2198 plays four **purple** strands (1AC7 / 1B30 /
  1B31 / 1B32) with purple hits built in. Gold = Hotfix on an ally (the heal's colour, as 6570 on the healed ally), purple =
  the hack of a health pack or deployable (her colour, as the hacked status) (M: the two states are the two branches
  `kit_pistol_hotfix.md` reads at #2645 / #2646).
- **Screen overlays (H data, L role)** (`ECIM(40)`, as Ana's): the hacked hero's screen (1704 C17-C21), the weakened
  enemy's screen (7AE1 C21 / C27), the stealthed Sombra's screen (1A75). Ours: HUD tints, not world particles.
- **Her shared shapes:** a skull (AB9D filled, 1EDB3 outline, 4B0C1 with a plus), code rain (9F81 / A4ED / A4EC / A512 /
  A4C0), glyphs (9F80, 4 frames), circuit boards (AD57, 7B847), pixel bars (A66D, 13 frames; A629, 10), a ring of six
  squares (mesh 3A16 with the circuit arms 68C88 / A064), a 0.43 m data cube (mesh 13EE4).

### Hardpoints

| Id | Where | Conf |
|---|---|---|
| 0006 of the pistol 1221 | the muzzle: the shot's flashes (1999 ESHT 0006 / 0081) | H id, M place (0081: L, the ejection port) |
| 000A | Weapon_R: the pistol 1221 itself (0B1B #73) | H |
| 0002 | her left hand: Cyberspace's cast 7CFA and launch 51FD, the beacon's throw (0C19 #89) | H id, M hand |
| 0021 | the Hotfix strands' start (614C `m_F745334E`) | H id, L place (her left forearm device) |
| 051F | Hotfix's cast flash 7CA1 (614C #1788) | H id, L place |
| 0001 | the target's chest: Hotfix's end, the hacked icons (+0.4 / +0.855 m), Cyberspace's weakened arrows (-0.15 m) | H |
| 0012 | the head: the healed ally's held effect 6570 (+0.5 m) | H |
| 0014 | the hacked panels (-0.43 m), the zone's streaks, a pack's 30 s hack | H id, L place (the base / feet) |
| 0004 | the root: the weakened tubes (+0.25 m), stealth's sparkles (+1.1 m), EMP's ESHT, the beacon trail | H |
| 0016 | Hotfix's alternative end on some targets (614C #2185) | H id, L place |
| 003E | Hotfix's on-target hits (7FF0..8012 ESHT) | H id, L place |
| 01DA | the data cube's spark emitters (502E, 7C7E) | H id, L place (the cube's corners) |
| 00A4 | the generic sound point | H |

### Meshes

| Id | Shape (model units, x size) | Used for | Pack model |
|---|---|---|---|
| 0F5B / 0801 | 1 x 1 m quad (0801 two-sided) | glows, flares, icons, screen overlays, the tracer | - |
| 07F5 / 3AD0 / 13CA | cross ribbon / vertical ribbon side / flat ribbon (1 x 1) | Hotfix strands / Hotfix strands / beacon and Cyberspace trails | - |
| **13EE4** / 14C5B | a **cube** 0.958 m, centred | the Cyberspace data cube (0.45 / 0.37 / 0.35), its sparks | `sombra_cube` |
| **3A16** | **six 0.5 m squares in a ring**, flat, 1.35 x 1.39 | the circuit-arm flower (Cyberspace, EMP, Hotfix) | `sombra_square_flower` |
| **3BD6** | a **curved panel** (part of a cylinder) 0.92 x 0.31 x 0.32, base 0.19 above its origin | the hacked status's rising code panels | `sombra_panel_arc` |
| **3BC3** | a **barrel band** (cylinder wall, rounded) 0.98 across, 0.45 tall | EMP's expanding wall | `sombra_band` |
| 39F8 | an arc segment 0.19 x 0.08, flat | EMP's flying arcs | `sombra_arc` |
| **1A988** | an **hourglass tube** 1 across, 1 long (axis z 0..1) | the weakened enemy's rising rings | `sombra_hourglass` |
| **DFDC** | a **chevron** (V) 8.4 x 6.6 cm, flat | the weakened enemy's falling chevrons | `sombra_chevron` |
| 11C1 / 3159 / 1E7F / 197B | flat ring, outer r 0.64 (0.5 for 197B), inner ~0.45 | shockwave rings (EMP, teleport, hack, Hotfix) | `sombra_ring` (11C1) |
| 11C3 | an open bowl (hemisphere with a hole), 1 x 0.49 | EMP's flares | `sombra_bowl` |
| 129B | an open cylinder 0.95 across, 1 long (axis z) | the stealth detection ring | `sombra_cylinder` |
| 1385 / DBF / 85B | spheres d 0.98 / 0.98 / 1.0 | the zone, the burst, EMP's spheres | (Jetpack Cat's `cat_sphere` is 1385) |
| D72C | sphere d 0.238 | the zone's circuit sphere at size 38 (9 m) | - |
| 13AD1 / DAE1 / 310C | flat disc 2 m / square with a round hole / square frame with a round hole | the zone's ground rings, screen frames | - |
| EF7 | flat four-sided pyramid 1 x 0.31 | the bullet impact's star | - |
| Entity models | the beacon 1575 = model 3845 (already `sombra_beacon` in the pack); Cyberspace's projectile 7763 has **no model**: the cube is its effect | | H |

## Colours

sRGB hex from the LUT rows, normalised by the row's peak where HDR (mask 0 to mask 1, the card's reading); HDR peak after.
Strips above. The `EffectArt` palettes are in `research\sombra\work\fx_palettes.txt` (clamped stops and `_N` normalised
ones; names below in capitals).

| Name | Where | Hex | Conf |
|---|---|---|---|
| Tracer warm | 1998 bands, tint r4 (8, 0.98, 0.305) / twin (8, 0.3, 0.05): HDR 8 | #FF6236 / #FF3615 | H values, L which team |
| Muzzle magenta-gold | 1999 flames strip A260 / B70B; flare r0 #E96C00 -> #FFF100 (HDR 3.7) | see strips | H |
| Impact | 010F: tints (30, 1.53, 1) orange and (30, 30.2, 30) white; star tint (30, 4.5, 1.25) / (30, 26, 8) | | H (a shared impact) |
| **Hotfix strand gold** (SOMBRA_HOTFIX_STRAND) | 7C6E C9 r3 (HDR 3.9) | #785108 #7C5409 #8D712A #B3A955 #FFFBC6 | H, M role |
| Hotfix strand orange (twin) | 7C6E C13 r2 | #781F08 #7C2009 #8D402A #B17054 #FFDDC6 | H, M role |
| **Hack strand purple** (SOMBRA_HACK_STRAND) | 1B32 C21 r3 (HDR 3.9) | #2B0878 #2D097C #4E2A8D #8655B3 #EEC6FF | H, M role |
| Hack strand red (twin) | 1B32 C23 r3 | #780808 #7C0909 #8D2E2A #B05A53 #FFBFB5 | H, M role |
| **Healed ally gold** (SOMBRA_HOTFIX_HEAL) | 6570 C29 / C33 / C23 r7 / r2 (HDR 5) | #692A00 #8E5D0A #C79F1C #F0C927 #FEE98F | H |
| Healed ally orange (twin) | 6570 r8 / r3 (HDR 5) | #690500 #8E310A #C75F1C #F07B27 #FEC38F | H, M role |
| Healed ally glow | 6570 C37 r1 (HDR 4) | #000000 #834C08 #AB7203 #D49B2F #FFC657 | H |
| Hacked icon | 1704 C13 strip A720 (purple) / C15 ADDD (red); 1B2F / 1B8E rows #007FFF -> #072CFF (HDR 4) x strip | strips | H |
| Hacked panels | 1B8E / 2777 / 1AF8 3BD6 rows #00FFFF (mask) x strip A06C (purple) / 20D0 (red), tints (5, 1.92, 1.96) / (0, 2.79, 2.54) | strips | H, L which tint row |
| Deployable rings (7D35) | 6429 rows #B75AFF (HDR 4), #A05AFF -> #5DE3FF, #FF5A5A x2 | | H values, L role |
| **Cyberspace trail** (SOMBRA_CYBER_TRAIL) | 502E C25 r3 (HDR 5) / twin r4 | #B800FF #E132FF #FF50FF #FF70FF #FF96FF #FFB7FF / #FF1200 -> #FFFFB7 | H, M twins |
| **Data cube** (SOMBRA_CYBER_CUBE) | 502E C28 r1 / twin r2 | #1E061E #B20CD2 #E374FA #F1B8FD #FEE5FF / #1E0606 #A74511 #DA694B #FA9E90 #FAFAFF | H, M twins |
| Cube circuit layer | 502E C30 r3 gold (HDR 2) / twin r4 | #5A3E00 #705300 #D5B20B #EFD132 #FFF67A / #000000 #600B17 #E83051 #F6477F #FF569B | H, M twins |
| Cube inner layer | 502E C44 r1 / twin r2 (HDR 3) | #2D004A #330053 #3F0065 #B01DE9 #F27AFF / #4A0900 -> #FF7AA2 | H, M twins |
| Cube sparks | 502E C32 r1 (HDR 2) | #D838FF #D838FF #D838FF #F6C28E #FFDB32 (violet to gold) | H |
| Launch flash (51FD) | C24 r2 / C26 r2 / C33 r3 (HDR 3) | #9C00DF -> #D573FF / #A100FF -> #FBF5FF / #6F0072 #9E52D1 #CDAAF8 #F8F8FF | H |
| **Burst flare** (SOMBRA_CYBER_BURST) | 7CDB C17 r1 (HDR 3) / twin r2 | #2D004A #80009F #C233E1 #E96BF8 #FF8CFC / #000000 #70331F #9B492E #D36642 #FF836D | H, M twins |
| Burst gold flare | 7CDB C21 r1 (HDR 3) | #4A1200 #8A3400 #E05D00 #FAA73B #FFD656 | H |
| Burst decal | 7CDB C24 r6 / twin r7 (HDR 21) | #B746FE #BB47FE #F9B18F #F5C624 #B89200 / #FE4B46 -> #B81A00 | H |
| **Zone sphere** (SOMBRA_CYBER_SPHERE) | 7C7E C72 r3 (peak 0.76) / twin r4 (peak 0.16) | #9D0CA6 #AD0FB7 #BC11C7 #CA13D5 #D615E2 / #511A15 -> #712721 | H, M twins |
| **Zone gold** (SOMBRA_CYBER_GOLD) | 7C7E streaks / rain r7 / r6 (HDR 5) / twin r8 | #692A00 #8E5D0A #C79F1C #F0C927 #FEE98F / #690500 #8E310A #C75F1C #F07B27 #FEC38F | H, M twins |
| Zone circuit sphere | 7C7E C58 r9 (HDR 2.7) / twin r10 (HDR 3.8) | #261400 #855A00 #B17C12 #BF984E #FFE08F / #420036 #560046 #913C45 #CC6439 #FF8500 | H, M twins |
| **Zone plus** (SOMBRA_CYBER_PLUS) | 7C7E C74 r2 (HDR 3); twin tint (12, 2.25, 2.25) red | #875C00 #A0850C #AB9D57 #C0B46E #FFF5A0 | H, M twins |
| Zone ground decal | 7C7E C65 r4 (HDR 3) / r5 (HDR 6.6) | #1C0028 #7F513B #B2A482 #FCF4CE #FFFBD7 / #000000 #521A11 #A58D77 #EFDBC5 #FFECD7 | H |
| **Weakened arrows** (SOMBRA_WEAKENED) | 7AE1 C14 r2 (HDR 3) / r4 | #55008A #661B7F #7D3166 #9F464B #FF9BA3 / #550100 #670300 #962D1C #D35338 #FF6B4A | H, M which team |
| Weakened tubes | 7AE1 C16 r8 / C18 r8 | #9C0B43 -> #FFBA70 / #2B0095 #A80098 #E2019C #F15E77 #FF813C | H |
| Weakened chevrons | 7AE1 C24 r3 / r2 | #000000 #AD324E #DA667F #F7839D #FF92AD / #FF929F -> #FFFFFF | H |
| **Beacon trail** (SOMBRA_BEACON_TRAIL) | 1B0F C8 / C10 r3 (HDR 2.45) / twin r4 | #07001F #AA04E1 #D07DFF #E4BEFF #F4F6FF / #1F0700 #E12D04 #FF897D #FFC7BE #F4F6FF | H, M twins |
| Beacon idle | 19A3 strip A06B / ADCF; rows #8F00FF / #E200FF (mask-shaped) | strips | H |
| Beacon landing (256F) | C23 r1 / C25 r0; C27 r3; C39 r0 | #C300FF #923EFF #9B8BFF #D5D0FF #FFFFFF / #FF0000 -> #FFAB32; #0004FF #9D18FF #CC81FF #E7CDFF #FFFFFF | H, M twins |
| **Teleport** (SOMBRA_TELEPORT) | 1A76 / 5FE0 C17 r1 / twin r2 | #310063 #D92AF6 #E89DFF #EFD8FF #FFFFFF / #310063 #F54F34 #FFA69D #FFDBD8 #FFFFFF | H, M twins |
| Teleport pixel bars | 1A76 C23 r1 / twin r2 | #7900A7 #EA6EF9 #F4AAFF #F6D4FF #FFFFFF / #A74B00 #F9826E #FFB4AA #FFDAD4 #FFFFFF | H, M twins |
| **Her body at the teleport** (material) | 1B1E / 3683 r1 (HDR 12.6, falls) / twin r2 | #DA73FF #CB60EF #BA45DD #A12DC1 #7E0099 / #FF6B4E -> #9D3A00 | H, M twins |
| Stealth dissolve edge | 1AD9 / 1AD8 material r0 (HDR 6.5) | #6D0000 #CE0000 #F6502B #00DFB6 #000000 | H values, L reading |
| **Detection ring** (SOMBRA_DETECT_RING) | 4DD9 r3 (HDR 20) / r4 | #020006 #370067 #4F0190 #7700D4 #FFFFFF / #000000 #E30000 #FF0000 #E20000 | H, M twins |
| **EMP** | every layer coloured by strip A06C (purple) / ADD0 (red-orange) x tints 2.5-12 | strips | H |
| EMP cores | 1AC8 C53 / C57 r2 tints (10.5, 0.873, 0.249) orange / (2.71, 0.873, 10.5) blue-violet | | H, M twins |

## Machine Pistol

| Part | Effect | Conf |
|---|---|---|
| The pistol | entity **1221** on hp 000A (0B1B #73); its onBegin **0B8C**: sounds only; children 177A -> 1B86 (empty), 2D33 -> 2860, 3024 -> 2B1E (empty) | H |
| **Each shot** | the hitscan entity **1222**'s own effect **1998**: the tracer + sub-effect **1999** (the muzzle) | H |
| Fly-by | **008C** (1222 `m_2170A015`): sounds only | H |
| **Hit** (a creature and the world) | **010F** in both impact slots (`m_AED73588`, `m_BF7914D4`): a shared bullet impact | H |
| Reload / fire clips | not read (no effect node of hers names them; the clips' own effects: L, sounds) | - |

**The tracer, 1998 (H):** two flat bands (mesh 0801) from the shot's start (`ECIN 0x0F`: the muzzle, M) to its end
(attribute 61F9CF82 = `ECIN 0x14`: the hit point, M), **0.035 m wide** (59DC556C), living **0.15 s**, masks 510 (a soft
line, 32 x 256) x 50C (16 x 256), the length read through a fade row (r1: full to 75%, gone at the end); tints **(8, 0.98,
0.305)** and **(8, 0.3, 0.05)**: HDR 8 **warm orange** #FF6236 and **red-orange** #FF3615 (H values; L which is whose: both
warm, no purple).

**The muzzle, 1999 (1 s; every emitter once at 0) (H):** on hp 0006 (and 0081):
- **Four flames** in a cross: the 6-frame flame A25E (`sombra_muzzle_flame`) at four roll angles (0, 90, 180, 270°),
  **0.04-0.14 m** (and 0.07-0.19 m), life **0.07-0.1 s**, strip **A260** magenta -> gold (and ADDD red twin), rows HDR 3.7.
- **Spikes** A25F (2 frames, `sombra_muzzle_spike`) **0.2-0.43 m** and 0.04-0.14 m along the shot, life 0.08 s, strip A260.
- **Glows:** 29F7 (`lucio_spark`) **0.69-0.79 m** and **0.34-0.44 m** (strip B70B magenta -> gold), A17F (`cat_glow_dot`)
  **0.74-0.84 m** tint (1.61, 0.34, 0.03) amber, life **0.1 s**.
- **3 sparks** (1F35 = `spark`) at 2.5-4.3 m/s in a 20-40° cone, 0.07-0.11 m, life 0.1-0.12 s, orange-red (HDR 8.5).
- A light 2.5 (+0.6 m along the shot). Sub-effect 4010 (sounds).

**Hit, 010F (shared, 1 s) (H):** a burst decal 220A (`sombra_impact_burst`) **0.035 x 0.035 m** (0.1 deep), random roll,
fading in 0.67 s; 2 sparks 1BE6 (`sombra_impact_spark`, a 4-frame colour sprite: NATURAL) **0.23-0.33 m** at 2 m/s, life
**0.05 s**, tint orange (30, 1.53, 1); a star (mesh EF7, mask 1BEC `sombra_impact_star`, 2 frames) **0.19-0.29 m**, life
0.05 s, tint (30, 4.5, 1.25). Twins in white (30, 30, 30). No colour of hers: a generic bullet hit.

**Recipe:**
- Each shot (20 a second): from the muzzle to the hit point a **0.035 m** band (ours: 0.05 m so it reads), `sombra_tracer`
  (the 510 x 50C product) on Shape.STREAK as Ana's tracer (`axis(piece)`, midpoint, `aspect`), warm orange #FF6236
  (normalised), additive, **0.15 s**, fading from 75% of its life.
- At the muzzle (third person; skip in first person): one `sombra_muzzle_flame` sprite 0.15 m (pick the frame by age over
  0.1 s), coloured by the A260 strip (magenta at the base to gold), plus a 0.7 m `lucio_spark` glow for 0.1 s (B70B
  magenta-gold); every few shots 3 tiny orange `spark`s.
- On a hit: a 0.25 m orange `sombra_impact_spark` (NATURAL) for 1 tick and a 4 cm burst decal (`sombra_impact_burst`,
  FLAT on the face, `lift`) for 0.67 s; on creatures the spark only.

## Hotfix

| Part | Effect | Conf |
|---|---|---|
| Cast | **7CA1** on hp 051F (614C #1788, at the channel's end, M); sounds 7746 / 5F58 / 2C75 (aim), 7DF5 / 7DF6 (held) | H |
| **The strands to an ally** | **7C6E / 7C6D / 7D40 / 7D41** from hp 0021 to the target's hp 0001 (or 0016), in State #2197 | H nodes, M ally |
| On the ally as each strand lands | **7FF0 / 8010 / 8011 / 8012** (under each strand), on the target | H |
| **The strands to a pack / deployable** | **1AC7 / 1B30 / 1B31 / 1B32** (State #2198), purple, hits built in | H nodes, M role |
| **The heal on the ally** | 60FB: **6570** held (hp 0012 + 0.5 m and 0001) while healing; sounds 6566 (start), 7E5B (held), 7CF5 (end) | H |
| The hack that follows | the hacked status (0B1F, below) | H |

**Cast, 7CA1 (H):** on hp 051F (+0.2 m): 2 rings 1E7F (flat ring) at **0.2-0.28** (0.29-0.35 m across), life 0.15-0.25 s,
masks 1038 noise x C495 x 293F x 5D4E pixels, rows violet #41006A -> #FF8FF6 (HDR 5) and orange; the circuit-arm flower
3A16 (68C88) **0.1** (0.14 m), life 0.2-0.28 s, violet #540086 -> #FF8FF6; a 0.1 m soft blob 174D8, violet; a light 3.

**The strands (7C6E family, 0.57-1.27 s) (H):** each a **ribbon from hp 0021 to the target** (M: the two ends; the ribbon's
life is input `ECIM(73)`, L the channel), three layers: a cross ribbon (07F5) **0.07 m** with the pixel-block mask 2B731
(`sombra_ribbon_pixels`) x 1344 (`mercy_band`), and two flat ribbons (3AD0) **0.133 m** with 1344. They start **0, 0.133,
0.267, 0.4 s** after the lock (7C6E, 7C6D, 7D40, 7D41): **four strands one after another** across the 0.5 s channel. Gold
(SOMBRA_HOTFIX_STRAND, HDR 3.9) with the orange twin (M).

**Each strand's arrival on the ally, 7FF0 family (1 s) (H):** on the target (ESHT 003E, M its chest), from the strand's
start time for 0.57 s: a **0.8 m** glow 242 (`mercy_glow`), 5 a second, life 0.2-0.38 s, gold strips 3FCE / C377; **16-21
sparks a second** (A7B6 `glow`, 0.08-0.13 m, 3-4.6 m/s, spinning) orange (ADD0) / white; pixel bars A66D
(`sombra_pixel_bars`, 13 frames) **0.16-0.29 m** drifting 0.12-0.23 m/s, life 0.16-0.47 s, strips ADD0 / C377 (gold).

**The hack strands, 1AC7 family (1.27 s) (H):** the same three ribbons (purple SOMBRA_HACK_STRAND, red twin) plus the same
arrival layers built in, coloured by A06C / A06B (purple) and ADD0 / ADCF (red-orange): purple glows 0.8 m, sparks, pixel
bars. 1AC7 adds sounds on hp 0002 / 0006.

**On the healed ally, 6570 (held, up to 20 s; 60FB #307 on the head + 0.5 m) (H values, M reading):**
- **A skull-with-a-plus icon:** quads with the skull 4B0C1 (`sombra_skull_heal`) x code rain 9F81 x scanlines 10A3,
  strip 6E5C gold, **10 a second, life 0.75 s** (a steady flicker), size 0.1 x0.82 -> x2.86 over life (8005961C), from
  0.067 s; a twin AB9D skull layer with row #FF452B -> #007343 (L).
- At the start: the circuit-arm flower 3A16 **0.2-0.4** (0.3-0.55 m) for 0.75-0.83 s, gold (HDR 5) / orange; a ring flare
  17E1 (`sombra_ring_burst`) 0.12-0.17 m, gold; 15-35 sparks (2A8C5) 2-5 m/s, stretched x5-7, 0.03-0.06 m, amber #ECAB00.
- From 0.33 s, while it runs: a code wall A512 (`sombra_code_wall`) x 2D278 on hp 0001, **1 a second**, life 2 s, size
  **0.1 -> 0.75** by dataflow 5FD (L: the heal's progress), gold SOMBRA_HOTFIX_HEAL; streaks 9BDF (`sombra_streak_v`) x 5D4E
  x 1484C on hp 0014, **3 a second**, 0.2-0.25 m, stretched 0.5-2, rising, gold; a body glow A7B6 (`glow`) 3 at the start,
  size 5 -> 12 by 5FD, life 7 s, gold (HDR 4, SOMBRA_HOTFIX_GLOW).
- Lights 2.5 (held) and 2 -> 4 (the first 0.47 s, by 5FD).

**Recipe:**
- While she channels (0.5 s), four gold strands from her left forearm (ours: her left hand, chest height) to the ally's
  chest, one every 0.133 s: Lifeweaver's Life Grip ribbon / Mercy's beam pattern (a STREAK band between the two points
  each tick), `sombra_ribbon_pixels` 0.07 m over a 0.13 m `mercy_band`, SOMBRA_HOTFIX_STRAND_N, additive, each fading
  over ~0.5 s. As each lands: a 0.8 m gold `mercy_glow` on the ally's chest for 0.3 s, 3-4 orange-gold `glow` sparks,
  2-3 `sombra_pixel_bars` (frame by age) drifting up.
- On a health pack or enemy deployable: the same four strands in **purple** (SOMBRA_HACK_STRAND_N) with purple glows
  and bars (A06C colours).
- On the healed ally, while 60FB runs: a `sombra_skull_heal` icon 0.5 m above their head (camera-facing, gold 6E5C strip,
  ~0.1-0.3 m growing x2.8 over 0.75 s, a new one each 2 ticks: a flicker), gold vertical streaks (`sombra_streak_v`, 3 a
  second) rising off them, and at the start a 0.4 m gold `sombra_square_flower` (or `sombra_circuit_arms_small` sprite)
  with a `sombra_ring_burst`. A soft gold body glow (`glow`, blended) for the heal's length.

## The hacked status (0B1F on the target)

| Part | Effect | Conf |
|---|---|---|
| At the hack | **1A32** (#249) then **1BAE** (#360): sounds only; EMP's per-target 0B32: 1BAE / **1BCB** (sounds) | H |
| **Silenced hero** (EMP: 3 s) | **1704** held (#247 / #346 / #391 in the silence states #57 / #352 / #399, #240 / #438) and **1B8E** (#314 / #345) | H nodes, M role |
| Hero revealed (8 s) | **1B2F** held + **1B24** (sounds) (#308 / #768, #330 / #765) | H nodes, M role (`kit_pistol_hotfix.md`) |
| Third hero state | **2777** (#390 in #399) | H, L which |
| **Health pack hacked (30 s)** | **1AF8** (#285, 30 s long) and **25C4** (#385, 30 s) | M (the 30 s length matches the pack's 30 s) |
| Deployable hacked (10 s) | **7D35** on hp 0006 (#790) | L role |

**Silenced hero, 1704 (1 s loop) (H):**
- **The skull icon:** quads (0801), masks the skull **AB9D** (`sombra_skull`) x the outline skull 1EDB3
  (`sombra_skull_outline`) x code rain 9F81 x digit column A4ED x scanlines 10A3, **0.62 m** growing **x0.82 -> x2.86**
  over life (to 1.8 m), **10 a second**, life **0.75 s**: a steady, flickering purple skull. Strip **A720** purple (#651B84
  -> #BDAAFF) and the twin ADDD (red). On the target (ESHT 0001).
- **Their screen (L):** a square frame with a hole (310C) 2.25 x halo ring 52E8 x glyph flipbook 9F80 (4) for 1-1.5 s, and
  two 0.13 m skull quads (strip ADD0): `ECIM(40)`.

**Silenced / revealed hero, 1B8E / 1B2F / 2777 (1 s loops) (H):**
- **Skull flashes:** quads with AB9D x 9F81 x A4ED, **0.52 m**, **2 a second**, life 0.75 s, at hp 0001 **+0.4 m** (1B2F)
  or **+0.855 m** (1B8E / 2777: above the head, M), rows #007FFF -> #072CFF (HDR 4) x strips 5AA0 (red) / A06C (purple);
  1B2F adds one 0.71 m skull at the start.
- **Rising code panels:** the curved panel **3BD6** (`sombra_panel_arc`) at **1.2-1.56** (1.1-1.4 m wide), stretched
  0.5-2, **3 a second**, life **0.675 s**, on hp 0014 -0.43 m (L: round their feet), masks A4ED / A4EC digit columns x
  9BDF streak, strip A06C purple / 20D0 red: walls of code rising round them.

**Health pack hacked, 1AF8 / 25C4 (30 s) (H values, M role):** code panels 3BD6 **1.9-2.9** (1.8-2.7 m wide), 3 a second,
life 0.675 s, strip A06C / 20D0; **50 small glows a second** (A7B6 `glow`, 0.25-0.35 m, life 0.18-0.26 s, drifting up
0.067), strips A06C / ADD0; sounds 3B6E / 3ADA (30 s).

**Deployable, 7D35 (L role) (H values):** flat rings 3159 (1.28 m) with the ring mask 6429 (`ana_ring`), **2 a second**,
life 2 s, purple #B75AFF (HDR 4) / purple-to-cyan / red twins x the cyan row #59FFFF -> #FFFFFF, on hp 0006.

**Recipe:**
- A hacked creature (EMP's 3 s silence): a camera-facing `sombra_skull` (purple, A720 colours, 0.6 m growing to 1.8 m and
  fading over 0.75 s, a new one every 2 ticks) on its chest; every 0.33 s a curved code panel (`sombra_panel_arc` model,
  or a `sombra_code_column` sprite on Shape.FLAT standing up) 1.2-1.5 m wide rising from its feet in A06C purple; twice a
  second a 0.5 m skull flash above its head. The hacked player's own screen: a purple frame tint with `sombra_glyphs` (HUD).
- Revealed (8 s): the 0.5 m skull flash at chest + 0.4 m, twice a second (ours: through walls for her team is the outline
  rule, not an effect).
- A hacked health pack / deployable: the code panels (1.8-2.7 m) and 50 tiny `glow`s a second drifting up, purple, for
  the hack's length; a deployable: also purple `ana_ring`s (1.3 m) twice a second.

## Cyberspace

| Part | Effect | Conf |
|---|---|---|
| Cast | **7CFA** on hp 0002 at the cast start (610E #1589): sounds only | H |
| **Launch** (0.1 s) | **51FD** on hp 0002 (610E #1803) | H |
| **In flight** | the projectile 7763 (no model) -> child 7762 -> **502E**; fly-by **54D4** (sounds) | H |
| **Burst** | **7CDB** at the zone's start (6102 #614, onBegin of #413) | H |
| **The zone** (3 s) | **7C7E** held at the burst point v1629 (6102 #413) | H |
| Zone sounds | **7CFB** held (#622), **7DFF** when 0.8 s are left (#676) | H |
| **Weakened enemy** | **7AE1** held on each enemy inside (#538 in the per-target #686) | H node, M role |

**Launch, 51FD (H):** on hp 0002 (her left hand, small offsets): a square frame 1F343 (`sombra_frame`) x diamond 433DB
**0.2 m** (life 0.4 s) and **0.8 m** (0.15 s), violet rows; the circuit-arm flower 3A16 at 0.07-0.17 (life 0.45-0.6 s);
29F7 flares **0.21 m** and **1.75 m** (life 0.2 s), violet #9C00DF -> #D573FF; lens streaks 5E9E (`ana_lens`) **0.5 m** and
**1 m** (life 0.17 s), violet -> white; lights 0.2 / 1.1 / 1.1.

**In flight, 502E (3 s loop) (H):**
- **The data cube:** three nested cubes (mesh 13EE4, `sombra_cube`) at **0.45 / 0.37 / 0.35** (0.43 / 0.35 / 0.34 m),
  spinning (rotation keys to 13.1 and 36.7 rad over life: M ~2 and ~6 turns a second), mask 43075 (`sombra_cube_glow`) /
  circuit AD57 (`sombra_circuit`) / 530F (`sombra_soft_square`), SOMBRA_CYBER_CUBE violet / gold circuits / violet inner
  (twins rust / pink / red).
- **The trail:** a flat ribbon (13CA) **0.75 m wide**, 200 points a second, each living **0.5 s** (25 m behind at 50 m/s),
  masks DC90 (`sombra_trail_band`, a band bright in its middle) x 1484C (`sombra_line_thin`), SOMBRA_CYBER_TRAIL violet
  -> pink (HDR 5; twin red-orange), fading along its length.
- **Sparks:** small squares (530F on 13EE4) **35-48 a second** from its corners (hp 01DA, M), 0.02-0.06 m, 0.35-0.65 m/s,
  life 0.2-0.25 s, violet -> gold; and 35-48 a second at 2-5 m/s, 0.06-0.08 m, life 0.75 s (3-7 at the start).
- Two lights 1.5. Sounds A96F / A96E.

**Burst, 7CDB (0.5 s; at the burst point + 0.35 m) (H):**
- **A sphere** 85B at **size 8** (8 m across, M: the 5 m zone's core), life **1.35 s**, circuit AD57 x 2C41F x BA19, rows
  #841700 -> #FF5300 (HDR 5) and violet-gold #B846FF -> #F6C931 -> #7C6100.
- **Flares:** A76F **1-1.35 m** (life 0.2-0.3 s, SOMBRA_CYBER_BURST violet), DA5B (`cat_flare`) **1.5-1.85 m** (life
  0.15-0.25 s, gold #8A3400 -> #FFD656), 2A8C5 0.25 m.
- **15-32 square sparks** (530F on the cube mesh) 2-10 m/s every way, life 0.5-0.75 s, violet -> gold.
- **The flower** 3A16 at **1.35** (1.8 m) for 0.75-0.83 s, pink -> gold; **ground rings** 13AD1 (flat disc, 2 m) at
  **0.5-3.5** (1-7 m) at +0.5 m, life 0.35-0.45 s.
- A **ground decal** 17E1 (`sombra_ring_burst`) x AD57, **5.5 x 5.5 m** (5 deep), 0.5 s, violet-gold (twin red).
- Two lights 4.

**The zone, 7C7E (held 3 s; the effect runs 7 s) (H):**
- **The bubble:** a sphere 1385 at **size 12 (11.8 m across: ~5.9 m radius, M the 5 m zone)**, flat mask 1298, held for
  the zone, SOMBRA_CYBER_SPHERE purple (peak 0.76: translucent) / the twin dim brown-red (peak 0.16).
- **Circuit sphere:** D72C at **size 38 (9 m)**, mask AD57 x flow 18F81 (BC5) x 20427 x 299E3, a 1.5 s cycle, gold
  SOMBRA_CYBER_CIRCUIT (HDR 2.7) / twin plum -> orange.
- **The cube stays** at the centre: the three spinning cubes (0.43 / 0.35 / 0.34 m) and their sparks (10-55 a second, 2-7
  m/s, life 0.75-1.25 s, for the first 3.67 s).
- **Code rain:** streaks 9BDF x 3FEEE x 1484C **1-1.5 m**, stretched 0.5-2, **5 a second** rising 0.33 m/s, life
  0.68-1 s (on hp 0014, L); **falling rain** A4C0 (`sombra_rain_tall`) x 9BDF, **13-18 a second**, falling **4-8 m/s**,
  0.25-0.35 m stretched x3-4.5, life 0.35-0.65 s: SOMBRA_CYBER_GOLD gold (twin orange).
- **Code walls:** A512 x 299E3 x 2D278 quads **0.25-1.1 m**, stretched 0.5-2, 1 a second, life 1.5 s, violet -> gold /
  orange (two twins).
- **Ground rings:** flat disc 13AD1 with cloud 18E48 x 2ABC8 x 299E9 x glyphs 9F80, **0.25-0.65** (0.5-1.3 m) 2 a second,
  life 1-1.5 s; another at **0.5-3** (1-6 m) from 0.33 s, life 1-1.5 s; a 9 m glow A7B6 (`glow`), 2 a second, life 1.5 s,
  dim gold (peak 0.1).
- **Rising plus signs:** 3DC7 (`sombra_plus`), **3-7 a second**, **0.43-0.61 m**, rising 0.31-0.5 m/s within 25°, life
  0.9-1.17 s, SOMBRA_CYBER_PLUS gold (HDR 3) / twin red tint (M: the healing allies see gold).
- **Ground decals** for 3.67 s: 7B847 (`sombra_circuit_disc`, a circuit quarter-disc) x 39115 (`cat_soft_ball`), **5.75 x
  5.75 m** (8 deep), spinning (6.28 rad over life), cream-gold (SOMBRA_CYBER_DECAL) / red; and 4C09 (`cat_soft`) 5.75 m.
- Two lights 3.5 for the 7 s.

**Weakened enemy, 7AE1 (8 s loop, held while weakened, on the enemy) (H values, M role):**
- **Falling arrows:** 66E4 (`sombra_arrow`, an up arrow drawn turned down, M) on hp 0001 -0.15 m, **2 a second**, life
  0.5-1 s, 1-2.5 m/s, size 0.3 -> 0.5, SOMBRA_WEAKENED violet -> pink (HDR 3) / dark red -> salmon.
- **Rising tube rings:** the hourglass 1A988 (`sombra_hourglass`) on hp 0004 + 0.25 m, **0.5 + 1 a second**, life
  2.35-2.85 s, size 0.75 -> 1.75 (by 5FD, L), masks the grid 2E4FB (`sombra_grid`) / 16E9D x slashes 369A3
  (`sombra_slashes`) x 18A1, rows #9C0B43 -> #FFBA70 / #2B0095 -> #FF813C.
- **Falling chevrons:** DFDC (`sombra_chevron`), mask 27D0D (`sombra_chevron_glow`) x 7A9 x 27D0A, **8 a second**, life
  0.75-1.25 s, size 0.75-1 (6-8 cm) stretched x3-3.5, pink (SOMBRA_WEAKENED_CHEVRON).
- **Their screen (L):** a frame DAE1 (2 m) with 212A6 / 2042B / 2103C, 0.5 a second, life 3 s, dark crimson (peak 0.41);
  small squares 183FB 50 a second, 0.15-0.23 m, magenta -> gold (`ECIM(40)`).
- Two lights 2.5 (hp 0001 -0.55 m). Sounds 9379 / 93AD.

**Recipe:**
- Cast: nothing but sounds; at 0.1 s at her left hand a 0.8 m violet `sombra_frame` flash (0.15 s) and a 1.75 m violet
  `lucio_spark` flare (0.2 s); a `ana_lens` streak 1 m (0.17 s).
- The projectile (50 m/s): a **0.43 m violet cube** (`sombra_cube` model or three nested cube outlines; ours: a
  `sombra_cube_glow` billboard 0.45 m with a `sombra_circuit` one 0.37 m) spinning ~2 turns a second, a `Fx.trail` of
  `sombra_trail_band` (ours: 0.3 m wide: 0.75 m is a lot at Minecraft scale) 0.5 s long, SOMBRA_CYBER_TRAIL_N (violet to
  pink), additive; a few tiny violet-gold `sombra_soft_square` sparks a tick.
- Burst: an 8 m violet-orange sphere (Shape.SPHERE / DOME with `sombra_circuit`, blended, 0.5 s growing then fading), a
  1.3 m violet flare and a 1.7 m gold `cat_flare`, 20 violet-gold square sparks 2-10 m/s, a 1.8 m `sombra_square_flower`
  (pink to gold), a 5.5 m `sombra_ring_burst` decal on the ground (FLAT, `KirikoFx.groundY`) for 0.5 s.
- The zone (3 s, 5 m): a **translucent purple bubble** (sphere 10-11.8 m across, SOMBRA_CYBER_SPHERE, alpha ~0.3, blended),
  a slowly turning 11.5 m `sombra_circuit_disc` on the ground (cream-gold), the cube hanging in the middle, gold code rain
  (`sombra_rain_tall` STREAK, 15 a second, falling 6 m/s) and rising `sombra_streak_v` streaks, gold `sombra_plus` signs
  rising 0.4 m/s (3-7 a second), ground rings (`ring_line` / `sombra_ring`) spreading.
- Each enemy inside: violet-pink `sombra_arrow`s turned down, falling off their chest (2 a second), pink `sombra_chevron`s
  (8 a second, ~7 cm, stretched x3, falling), and one `sombra_hourglass` ring every second rising round them. If the player
  is weakened: a dark crimson screen-edge tint (HUD).

## Translocator and stealth

The build's beacon graphs 0C19 / 0C1A read as the Community Crafted mode's beacon (`sombra-map.md`); the effects below
are the ones they and the beacon entity 1575 play, which the live beacon (the same entity, M) shows too. Left out: the
Stadium-gated nodes (07xx.134: 36D7, 3EAD, 3B87, 151A, 1B38, 46A2 / 46A4, 46E9, 46EE).

| Part | Effect | Conf |
|---|---|---|
| Throw | from hp 0002 (0C19 #89): no effect node; 0186 is the shared "can't" sound | H |
| **Beacon in flight** | the beacon model 3845 (`sombra_beacon`) + child 1710 -> **1B0F** (trail) | H |
| **Beacon idle** | **19A3** held on it (0C1A #473) | H |
| Beacon landing / arming | **2579** (sounds) then **256F** (0C1A #338 / #342 at #477's begin; 0C19 #794 / #795) | H nodes, L which moment |
| Beacon material | **3398** (261E #369, while a flag on its owner holds) | H, L role |
| **Departure** | **1A76** at v7138 (where she was), facing the destination (0C19 #106) | H node, M place |
| **Arrival** | **5FE0** at the destination (beacon + 0.5 m up), facing back (0C19 #105) | H node, M place |
| **On her at the teleport** | **1B1E** (0C19 #167, on self) | H |
| After the move | **1B4F** (sounds) and **3683** (her body flash) when #359 ends | H |
| **Stealth in** (0.75 s) | **03E7** (sounds) then **1AD9** (420C #4 -> #162, at the fade's start) | H |
| Stealth held (6.5 s) | **1A75** held (#251, 10 s): her own screen; **16EA** held (#9): empty | H, L screen |
| **Stealth out** (0.3 s) | **03E8** (sounds, #260 at the 6.5 s end) and **1AD8** (#160, criteria 2263: the fade back) | H nodes, M which |
| Detected | **2593** (#253 / #259): sounds only | H |
| **Detection ring** | **4DD9** held while stealthed, unless the option "Hide Stealth Detection Ring" (v52253) is on (#959 -> #785) | H |

**Beacon trail, 1B0F (H):** three flat ribbons (13CA) **0.25 / 0.5 / 0.7 m wide**, 60 points a second, living **0.3 s**,
mask 3A37 (`lucio_trail`) x the length row 800E9688, SOMBRA_BEACON_TRAIL indigo -> violet -> white (HDR 2.45) / orange twin;
the 0.7 m one dimmer (#66018B -> #C106FF). From hp 0004 (the beacon's root).

**Beacon idle, 19A3 (3 s loop, at +0.18 m) (H):** pixel bars A66D (`sombra_pixel_bars`) **0.37-0.39 m**, 5 a second, rising
0.2-0.47 m/s, life 1.05-1.6 s; a vertical light beam 138F (`sombra_beam_v`) **0.43-0.66 m**, 5 a second, life 0.68-1.23 s;
a lens 23C0 (`lw_lens`) **0.6-0.82 m** and a glow 242 (`mercy_glow`) **1.68-1.9 m**, 3 a second each: all strip **A06B**
(purple) / ADCF (red-orange).

**Beacon landing, 256F (2 s, all at 0) (H):** two glow streaks 242 **0.06-0.63 m** stretched x1.9-3.9 within 70° (life 0.58 s,
violet -> white / red-orange); a ring 197B with the skyline flipbook A5B6 (`sombra_skyline`, 14) **0.52-0.55 m** stretched
x2 (life 0.18 s); a lens flare CC29 (`mercy_lens`) **0.27 m** (0.25 s); **16-26 sparks** (`spark`) 5-10.7 m/s within 140°,
life 0.15-0.35 s, violet / red; **3 smoke streaks** 3E28 (`sombra_smoke`) x B08 (strip 22ED grey) 0.65-0.84 m stretched
x6-7, spinning, 2.6-4.1 m/s, life 0.8-1.2 s; two lights 1.7.

**Departure 1A76 (2 s) / arrival 5FE0 (1 s) (H):** both at the point + 1 m, aimed along the move:
- at 0.233 s (the end of the move) a 29F7 flare **0.525 m** (life 0.22-0.26 s) and **7 pixel bars** A66D **0.93-1.42 m**
  flying **6.4-11.6 m/s** (life 0.25-0.46 s): SOMBRA_TELEPORT violet / coral;
- at 0: **5-7 soft glows** 4F3 (`lucio_soft`) and **3 rings** 11C1 with the 10-frame A629 (`sombra_pixel_bars_b`), life
  0.18-0.36 s; **40-42 sparks** 29F7 **0.35-0.51 m** moving **inward 3 m/s** (an implosion), life 0.22-0.33 s, violet
  (gold-red HDR 22 row);
- 1A76 only: **20 pixel bars** 0.38 m drifting 0.86 m/s (life 0.4 s) along the path (input 0x39, M);
- both: a 7.7 m distortion quad C4A (BC5 normal map): heat haze, skip.

**On her at the teleport, 1B1E (1 s) (H):** **40 pixel bars** (A66D) **0.48-0.59 m** round her, drifting 0.07-0.09 m/s,
spinning 0.37-0.79 rad/s, life 0.42-0.97 s, strips A06B / ADCF, x8; and a **material effect on her body**
(SOMBRA_TELEPORT_BODY: lilac -> purple, HDR 12.6, read by age: a bright flash fading, M). 3683 is that body flash alone.

**Stealth in, 1AD9 (2.67 s) (H):** a **material effect** with the 6-frame diagonal wipe A7BF (`sombra_dissolve`) over 1 s,
edge row #6D0000 -> #CE0000 -> #F6502B -> #00DFB6 (HDR 6.5, M: the dissolve edge, red to cyan); at **0.77 s** (the fade's
end) **500 tiny sparkles** (4F3 soft dots) **0.03-0.05 m**, life 0.14-0.73 s, at hp 0004 + 1.1 m (her middle), strip
**A06C** purple; 3-6 glints 21B8 (2 frames) 1-2 cm at 0.17 s. 1AD8 (stealth out): the wipe twice (in reverse, M), 1 s.

**Stealthed, 1A75 (H values, L role):** a 2 m screen quad (`ECIM(40)`), 3 a second, life 1 s, masks 4DB x 5E7, very dim red
(peak 0.13): her own view's tint while stealthed.

**Detection ring, 4DD9 (held, 1 s loop) (H values; M size):** the open cylinder 129B (`sombra_cylinder`) at size [1, 1],
masks 1C7A (`cat_band`) x 43D04 (`sombra_square_band`: three glowing squares in a band), **SOMBRA_DETECT_RING** black ->
violet -> white (HDR 20; red twin), life 1 s. The data scales the UX ring to **8 m across** (df 0639 = 2 x the 4 m radius:
`kit_translocator_stealth.md`; M that the effect takes it).

**Recipe:**
- Beacon in flight (72 m/s): the beacon model with three ribbons (`Fx.trail` of `lucio_trail` 0.25 / 0.5 / 0.7 m wide,
  0.3 s long; ours: one 0.4 m), SOMBRA_BEACON_TRAIL_N violet, additive.
- Beacon resting (if it ever rests): rising violet `sombra_pixel_bars` (5 a second), a 0.5 m `sombra_beam_v` and a 1.8 m
  `mercy_glow` pulse (3 a second) in A06B colours, 0.18 m above it. At landing: a violet flash (`mercy_lens` 0.27 m, two
  `mercy_glow` streaks), 20 violet `spark`s 5-10 m/s and 3 grey `sombra_smoke` streaks.
- The teleport: at the start point **and** the end point, 1 m up: 6 `lucio_soft` violet glows, 3 `sombra_ring`s with
  `sombra_pixel_bars_b`, 40 `lucio_spark`s 0.4 m converging 3 m/s (0.3 s); at 0.23 s a 0.5 m violet flare and 7 pixel bars
  shooting out 6-11 m/s along the move. On her: 40 `sombra_pixel_bars` 0.5 m round her body for ~0.7 s and a bright lilac
  body flash (ours: a violet overlay on her model fading over 1 s).
- Stealth in (0.75 s): her model wiped away along a diagonal (`sombra_dissolve` frames by fade progress; ours: alpha with a
  `sombra_dissolve` mask if the renderer can), then at 0.77 s a puff of **500 tiny purple sparkles** at her middle (ours:
  60-100 `lucio_soft` dots 4 cm, A06C purple, 0.15-0.7 s). Stealth out (0.3 s): the wipe back.
- Detection ring (while stealthed, option on by default): an open cylinder / band **8 m across** round her (`sombra_cylinder`
  or a ring of `sombra_square_band` quads), violet SOMBRA_DETECT_RING_N, a 1 s pulse, shown to her (M: the UX ring is hers).

## EMP

| Part | Effect | Conf |
|---|---|---|
| **Cast** (0.65 s) | **223E** at the cast start (0B31 #216, on self, up (0, 1, 0)) | H |
| **The pulse** (at 0.35 s) | **1AC8** (#215, on self) + **1B0E** (sounds), both at #218's end | H |
| Per target | 0B32: **1BAE** (sounds), **1BCB** (sounds, 6 s); the targets are hacked (0B1F, above) | H |
| Life Hack | **46A7** (#295): sounds only | H |

**Cast, 223E (0.33 s) (H):** **3 + 3 flat rings** 11C1 with the 10-frame band 4FE0 (`sombra_emp_band`), **1.18-1.9** (1.5-2.4
m across), life **0.2 s**, strips **A06C** purple / ADD0 red-orange x 4.

**The pulse, 1AC8 (2 s; at her, ESHT 0004) (H values; M growth reading):**
- **The wall:** the barrel band 3BC3 (`sombra_band`) with the skyline flipbook A5B6 (`sombra_skyline`, 14 frames), at
  **size 2.05 growing x1 -> x15** over its **0.5 s** life (to ~30 m across: the **15 m** reach), alpha fading 1 -> 0, strips
  A06C / ADD0 x 2.5-4, rolled at random, at -1 m (ground level, M).
- **The spheres:** DBF and 1385 with the circuit AD57 (`sombra_circuit`) at **size 2.05 growing x15** over **1 s** (from
  0.067 s), rows yellow-green masks x strips A06C / ADD0 x8: a circuit-lined dome sweeping out to 15 m.
- **Core flash:** spheres DBF **1.39** (life 0.71 s) with strips A06B, tints orange (10.5, 0.87, 0.25) / violet (2.71, 0.87,
  10.5); flares 29F7 **2.3 m** (life 0.5 s, x8); open bowls 11C3 **0.54-0.67** x2 (life 0.43-0.65 s), violet #B500FE ->
  #FE00FE x (13.3, 9.8, 37.8).
- **Circuit flower:** 3A16 with the circuit arms A064 (`sombra_circuit_arms`) x 18A1 at **size 6 (8 m)** growing x2 -> x3.1
  (to ~25 m), life **1.1 s**, x12.
- **Arcs:** 20 + 20 arcs 39F8 (`sombra_arc`) 0.26-0.56, life 0.32-0.35 s, masks 4DB x B204, orange-yellow (HDR 75) rows.
- **160 + 160 sparks** A17F (`cat_glow_dot`) **23.6-29.1 m/s** every way, 0.13-0.44 m stretched x1.9-3, life **0.65-1.38 s**
  (reach ~15-40 m), yellow -> red over life (HDR 13), strips ADD0 / A06C.
- **Rings** 11C1 with 4FE0 at 0.1 s, 0.55-1.43 (life 0.18-0.19 s), x4.
- **Two lights 8** for 1.3 s from 0.067 s.

**Recipe:**
- Cast: around her middle 3 purple `sombra_ring`s (or flat `sombra_emp_band` sprites) 1.5-2.4 m for 0.2 s.
- At 0.35 s, the pulse: a purple **expanding wall** (`sombra_band` model or a ring of `sombra_skyline` STREAK quads, frame
  by age) from 2 m to **30 m across in 0.25 s**, then holding and fading by 0.5 s (size factor 1 -> 7 -> 14.5 -> 15 at
  0 / 10% / 25% / 100%); a **circuit dome** (Shape.DOME `sombra_circuit`, A06C purple, blended) growing the same way over
  1 s; a 2.3 m bright flash and a 1.4 m violet core at her; an 8 m `sombra_square_flower` (circuit arms) spreading to
  25 m; ~60 (data: 320) `cat_glow_dot` streaks flying out 24-29 m/s, purple, 0.7-1.4 s; a light flash (ours).
- Each enemy hit: the hacked look (above) for its 3 s.

## Her tablet (389D) and the emote holograms: their materials

Read 2026-11-01 with `fx_sombra_owmats.py` (the exported .owmat slots, no game access) and `fx_sombra_matdump.py` (each
material's shader, material data, texture type keys, static inputs and float LUT rows, TankLib offline) ->
`research\sombra\work\fx_owmats.txt`, `fx_tablet_materials.txt`, sheets `fx_sheets\tablet_38EC.png`, `holo_3B1E.png`.

**The tablet uses one material, not 34 (H).** Model 389D has **one submesh** (`Submesh_0`, material key
0x186371A20AD5C032), and its only look (4890) maps that key to **material 38EC**. The other 33 .owmat files in its
`Materials` folder are not used by its mesh. They are effect materials (shader 0x26 with only a float LUT under key
2CE679FF and BC4 masks: 1298 flat, 04DB noise, 16E9D, 18E48, 299DF...) that DataTool collected alongside the model
(M: from the clips' animation effects). Their LUT rows hold parameters and paired colour rows (for example 8AB81146 r1
blue (0.02, 0.55, 2.46) -> (3.6, 6.6, 7.8) / r2 red; A7908D75 r1 yellow-green / r2 orange-red; `fx_tablet_materials.txt`).
None of them should be drawn on the tablet.

**Material 38EC** (shader 0x26, material data 6C26):

| Key | Texture | What it is | Conf |
|---|---|---|---|
| **58FF5009** | **9FE9** 512² BC7 sRGB | **the colour: draw this.** Purple hexagon panels with light code bars (mean #6C307B, up to #DEC2E5) | H key, M role (the same key as Lifeweaver's petal art) |
| **B02D5995** | **ADC3** 512² BC7 sRGB | the same layout darker (#482A4F), its **alpha** the panel's opacity: the background opaque, the hexagons ~25% and the bars clear. Use ADC3's alpha as 9FE9's alpha | H texture, M role |
| 0403F89A | ADC4 512² BC1 sRGB | a lighter lilac version (#CF9EDD to white): the glow / emissive layer | M |
| D569F1CA | A06B 256 x 8 strip | the purple colour strip (black -> magenta -> periwinkle -> white) her effects use: the hologram flicker / scan tint | M |
| A5030545 / A26EC15C | ADBD 64 x 128, 1BF7 256 x 128 | soft horizontal bands: the scan-line sweep and the edge fade | M |
| 49E57F4E | 9FE8 256² BC1, flat #BDE3E3 | constant (L: a PBR term, not colour) | L |
| 1E7BC53E | 8000D74709EF 512 x 1 RGBA8, white | a 1D ramp, all white | H |
| 0DB044E6 / A92DB409 | 000C (flat normal), 0460 (white 8²) | the engine's flat defaults | H |

Static inputs: no colour tint. The values are 2861D981 = 2.0 (L: glow strength), A2F512A2 = (0, 0.3),
EC149323 = (0.04, 0.04, 0.04) (a specular F0) and 5898279A = (0, 0, 0, 1). **Blend: draw it blended (translucent), with
9FE9 as the colour, ADC3's alpha as the alpha, plus a little self-light (ADC4 or 9FE9 x 2.0) so it glows.** The alpha
mask is what makes it read as a see-through screen. No node or flag read here says added or blended (M). The "dark
specks" are probably the alpha path: if the exporter's alpha comes from a slot other than ADC3, or treats the material as
a cut-out, most of the panel drops out.

**The emote holograms (shader 0x32, H textures and keys, M roles):**

| Emote | Model / material | Light mask (flipbook) | Colour | Others |
|---|---|---|---|---|
| Amused | 3CDA / **3AED** | **B34B** (E1A39CA9) | strip **A06C** (D20435D3) | scanlines 10A3 (E6CE58B0), bands B468 (A5030545), B345 (78AACD13), B33D / B33C strips, B33E 400² (42BB2842), LUT 800F96958A53 (5118206D: parameters) |
| Masterpiece | 3CF7 / **3B1E** | **B2AF** (CDE30B73, 6 frames: a crowd picture) | strip **A06C** (BD89FFFC) | **B2C9** (54EA5AC9, BC7 sRGB: the panel's white frame; its alpha is the panel's silhouette), glyphs 9F80 (BAE43BE5), code rain 9F81 (2480AE46), caustic noise B3E2 (4B0D6469), scanlines 10A3 (E1A39CA9), LUT 800B4F9E1790 (parameters) |
| Sit | 49BB / **47F0** | **A5B6** (42BB2842, 14 frames) | strip **A06C** (D569F1CA) | B2C9 frame (35BC18D4), glyphs 9F80 (D20435D3), code rain 9F81 (A5030545), scanlines 10A3 (54EA5AC9), LUT 800F297F6F0E (R16G16) |

**Their tint is the A06C strip**, the same purple strip her effects use: **#730181 #791DB5 #8344EF #9472FC #FFFFFF**
(sampled at 0, .25, .5, .75 and 1). All three static inputs are zero (F3B3EA6F, 842D389F, 7F4535E1), so no constant
tint. Read the strip by the mask's brightness, as the effects do (M): dim parts dark violet, the middle #8344EF, the
brightest near white. As one colour, **0x8344EF** is the strip's middle. Your 0xB65CFF placeholder is close but pinker
and lighter. **Blend: added (M).** The masks are BC4 light masks on black and nothing else gives them an alpha. In
Masterpiece and Sit, B2C9's alpha gives the panel's outline: use it as the panel's cut-out, and its RGB (the white
frame) added in the same strip colour.

## Left out, and why

- **Her old kit:** Hack (the old ability), Virus, Opportunist, the old Stealth ability: no live graph node of her kit plays
  their effects in this build beyond what the hacked status (0B1F) and the new kit share (H: the graphs above). Effects only
  her old loadouts named were not extracted.
- **Stadium:** every node behind a Stadium power id (`STU_22504CC2` with a 07xx.134): in 0B1F 3BF7, 3E3F, 3602, 457E, 4582,
  46AF, 46E5, 46EC, 46F0, 46FB, 4706; in 0C19 36D7, 3EAD, 3B87, 151A, 1B38, 46A2, 46A4, 46E9, 46EE; in 0B31 46F6; in 0B1B
  468D (H gating).
- **Community Crafted Translocator:** the build's beacon graphs read as that mode's (E again to teleport); its effects are
  kept above only where the live beacon shows them too (trail, idle, landing, teleport). The 5 HP beacon's damage / death
  effects: none found (H: no effect node on its health pool).
- Perks: 51EC 662F, 51A3 79A7 / 79A8 (not read), 46A7 (sounds).
- Shared graphs (0033, 0043, 004B, 01C7, 0164...): not hers.

## Not found

- **Which twin is whose:** no node that picks the purple / red-orange (or gold / orange) twin was decoded (M: purple = her
  team, as Ana's gold).
- **The pistol tracer's team colours:** both of 1998's tints are warm orange (L: Sombra's tracer may be tinted elsewhere,
  e.g. a material parameter; nothing purple in the shot's effect).
- **The Hotfix strands' exact path:** the ribbons' points come from inputs and `ECIM(73)` (L: the channel's own curve);
  hp 0021 and 003E's bones not read.
- **What 7D35 (on hp 0006) is for**, and 2777's state (L).
- **The stealth body look** (her translucent shimmer while stealthed): 16EA (held) is empty; the invisibility is
  STU_33A46033 (#202), a renderer state, not an effect (M). Ours: her model at low alpha for her team, hidden for enemies.
- **The live Translocator's own flow** (teleport on contact) plays from graphs this build doesn't hold (`sombra-map.md`).
- Firing / reload clip effects (08F): not traced (no ability graph names them; sounds, M).
