# Mercy's ability effects in the game data

What Mercy's effects are made of in real Overwatch, where the extracted files are, and how to rebuild each one in
Minecraft. Read 2026-10-28 from the archived offline build 154088 (prometheus-2_25_0_0-154088). Overwatch and Battle.net
were checked closed before every session; everything was read only, offline, through TankLib/DataTool. Text only: the
textures and meshes are Overwatch assets and stay in the owner's archive. They reach the game only through the local
asset pack. The format is the one `jetpack_cat-effects.md`, `kiriko-effects.md` and `lucio-effects.md` describe; this
file adds what is different for her. Her data map (graphs, numbers, clips): `E:\Overwatch Archive\research\mercy\mercy-map.md`.

Confidence after each number or claim: **H** read in the data, **M** inferred (one link guessed), **L** guess.
"Size" is the spawn size value: it scales the mesh (a 1 m quad at size 0.5 is 0.5 m; a ribbon's size is its width).
A range "a-b" is a random value between the two (Kiriko's `mode 5` / `mode 1` reading, as Jetpack Cat's). "Ours" marks
what no data gives.

## Files

`E:\Overwatch Archive\research\mercy\fx\Mercy\<Ability>\`, with `<Ability>` one of `CaduceusStaff` (the staff head,
both beams and what an ally shows while beamed), `CaduceusBlaster`, `GuardianAngel`, `AngelicDescent`, `Resurrect`,
`FlashHeal`, `Valkyrie` or `Wings` (the wings at rest; it also holds 2106, Resurrect's glow on the dead ally):
- `textures\<id>.png` (flipbooks split `<id>_1.png ...`), `ramps\<id>.json` (float LUTs, `[row][x][r,g,b,a]`),
  `models\Models\<id>.00C\` (effect meshes), `manifest.json` (per effect: emitters paired with their renderers,
  hardpoint table, spawn shapes, raw track nodes). Same layout as `research\jetpack_cat\fx\JetpackCat`.
- Not under `extracted\` (this pass could only write under `research\mercy\`): pass `research\mercy\fx` to
  `build_asset_pack.py effects` as one more source (`add_effects(*sources)` takes the first folder that has the file).
- Research files in `E:\Overwatch Archive\research\mercy\work\`: `fx_digest.txt` (every renderer with its emitters
  resolved: the file to read for any number below), `fx_brief.txt` (short form), `fx_summary2.txt|json`,
  `fx_meshes2.json`, `fx_palettes.txt`, `fx_sheets\tex_<Ability>.png` (every mask), `fx_sheets\meshes_<n>.png`,
  `fx_effects\` (decoded effects), `fx_graphs\` (the two beam graphs 0079 / 006C, reflected), `fx_dump\` (STU dumps:
  the beam graphs, her skin theme, the beam status effects), `fx_replacements.json` (every skin's effect swaps),
  `fx_stuscan_log.txt` / `fx_refscan_*` (which graphs name the beams), `fx_ecmp_*.json` (raw component headers).
  Scripts in `..\scripts\fx_m_*.py` (Jetpack Cat's `fx_*` retargeted, plus `fx_m_stuscan.py`, `fx_m_repl.py`,
  `fx_m_brief.py`, `fx_m_compact.py`, `fx_m_palettes.py`). Every new file is read-only, listed with its sha256 in
  `fx\SHA256SUMS.txt`, `work\fx_SHA256SUMS.txt` and `scripts\fx_m_SHA256SUMS.txt`.

Counts: 83 effects extracted (46 with visuals, the rest sound only, empty, or Stadium), 387 distinct textures (448
entries across the eight folders, 302 of them float LUTs), 23 meshes. One effect is missing from the archive:
Resurrect's 29B6 (00CA#1067, FileNotFound).

## What's new compared with Jetpack Cat, Kiriko and Lúcio (format)

- **The beams are not in her staff graph (H).** The staff (006B) applies a *status effect* to the beam target:
  #1040 `STU_431F3FCC` applies **0019.053**, whose graph is **0079** (heal beam), #1041 applies **0016.053** -> graph
  **006C** (boost beam); status `STU_76F2C784`, m_7DAE8F10 = 5.0 (M: a 5 s refresh/timeout). Those graphs run *on the
  ally* and draw the beam back to her with `STU_9932D728` (Catnapper's / Flash Heal's beam node): source entity v27
  (Mercy), start entity v33533 = Mercy's v49 (her staff entity: 0079#353 / 006C#319), start hardpoint v13267 = **0006**
  (the staff head) or **0001** (her chest, M: in Valkyrie's 360° mode, flag 8075/v32885), end = the ally itself, hardpoint
  **0001** (chest). They also hold what the ally shows while beamed. Found by parsing all 5687 graphs (`fx_m_stuscan.py`:
  0079 and 006C are the only hero-side users; the rest are game-mode copies). The staff's held effects 0197 / 0198 are
  only the staff-head glow.
- **One colour row per LUT, not an ally/enemy pair (H).** Her 65BC LUTs are 3-10-row parameter textures: most rows are
  UV / curve parameters (one channel ramping, others constant); one row is the colour ramp (r3 in the beams, r5 in the
  rounds, r8 in Valkyrie's feathers...) and a constant row is an HDR tint (x1.6 to x12). Enemy colours come from **twin
  renderers** with their own LUT (the round, the muzzle flash, Valkyrie's feathers, Resurrect's glow) or, in a few
  LUTs, a second colour row (Valkyrie's sparks r5 / r6). The beams and the ally-side effects have **no red twin** (H): an
  enemy sees her beams in gold and blue (M).
- **Colour sprites (H).** 1268 (golden swirl), 126A (yellow dot), 13CC (yellow wisps), 20AC (orange impact disc), 1369
  (gold teardrop), 1411 (yellow ring: the halo) are BC7 sRGB sprites whose shape is in **alpha** (red channel ~255
  everywhere): draw them as they are (`Palette.NATURAL`), never through a grey-mask palette.
- **A skin-replacement map (H).** `DataTool.SaveLogic.Unlock.SkinTheme.GetReplacements` per skin theme
  (`fx_replacements.json`): her default skin (theme 2D5D, model DBEA) swaps models and clips but **no effect**; 0301 (the
  wings at rest) has 22 skin versions, 2B1E (staff child) 19, the beams 01F5 / 186F one (5CA2). Everything below is the
  default look.
- **Projectile slots differ (M).** Her round 0158's `m_A18274A3` (Jetpack Cat: hit on a target) holds 01E2, whose every
  emitter sits on hardpoint 0006 and draws a flame-shaped crossed-quad flash for 0.08-0.1 s: her **muzzle flash**. Both
  impact slots (`m_AED73588`, `m_BF7914D4`) hold 02F1: one impact for creatures and walls.
- **Correction to `mercy-map.md` (M):** the map calls 0197 the heal glow and 0198 the boost glow. By content it is the
  other way round: **0198** carries the gold healing sprites 1268 / 126A (the shared healing look, as Jetpack Cat's
  2074) and **0197** is blue (#1125FF -> white). 006B#990 picks 0198 on its true branch, 0197 on its false one, on v465
  (the beam mode); the expression wasn't decoded.
- **Two views per beam (H nodes, L roles).** Each beam graph picks one of two looks by comparing two entities
  (0079#159 / 006C#114, opcodes `29 0, 29 1, 76`, operands not decoded): heal **01F5** (+ 56D3) or **186F**, boost
  **02E0** or **1870**. 01F5's ribbons read dataflow 063C, which her own staff (006B#403) and Angelic Descent (0071#241)
  set true client-side: L that 01F5 / 02E0 are the owner's view and 186F / 1870 everyone else's. The recipes use 186F /
  1870.
- **Stadium is everywhere (H names).** Many branches are Stadium powers (conditions `.134` named in
  `work\conds134.json`): left out below, listed under each ability.

### Hardpoints

| Id | Where | Conf |
|---|---|---|
| 0006 | the staff head (staff entity, (0, -0.007, 0.383) on its Pelvis bone) / the blaster muzzle ((0, 0.095, 0.254)) | H (`mercy-map.md`) |
| 0001 / 00A4 | chest (Spine_3) / generic chest point: beam ends, ally effects | H ids, M bones |
| 002C / 002E | wing hardpoints left / right: wing motes, wing glows, Valkyrie sparks | H ids, M which wing |
| 002D / 002F | wing tips: Guardian Angel's thin trails | H ids, M |
| 007E / 0080, 0155 0157 0158 015A 015B 015D 0169 016B | wing roots and feather tips: Valkyrie's wing sheets and feathers | H ids, M roles |
| 003D / 003E | the generic beam start / end (ECAT nodes, as Jetpack Cat's beams) | H |
| 0002 | Resurrect's cast clip effects (L: her hand) | H id, L bone |
| 0075 | the blaster's reload clip effect (blaster bone 0089) | H |

### Meshes

| Id | Shape (model units, x size) | Used for |
|---|---|---|
| 0F5B / 0801 / 10F4E | 1 x 1 m quad | glows, sparks, flares, feathers |
| 0797 | single strip ribbon, 1 wide | both beams, Flash Heal's beam, the round's trail |
| 07F5 | cross ribbon (two strips) | Guardian Angel's trails, the round's tracer |
| 085B / 0DBF / 1385 | sphere r 0.5 | the boost staff-head glow, the revived ally's flash |
| 08C0 | three crossed planes, 1 m | the muzzle flash |
| 296B | flat annulus r 0.27-0.64 | the muzzle ring |
| 0CAE | flat annulus r 0.25-0.5 | the halo ring (with mask 1411) |
| DAE1 | flat 1 m square with a round hole r 0.35 | ground rings (boosted ally, Flash Heal, A Favor Honored) |
| DFDC | chevron, 0.084 x 0.066 m | the damage-boost arrows |
| DFEC | tapered strip 0.32 x 1 m | the boosted ally's spikes |
| E0AA | plus cross, 0.072 m | the revived ally's rising crosses |
| 5365 / 5366 | curved wing sheet, 1.26 x 0.98 m (upper / lower) | Valkyrie's wings |
| 1E76A | octagon quad 1 m | wing motes at rest |
| E3E | open cone r 0.7, 1 long | blaster reload (L) |
| B18 | small bent piece 0.2 m | Resurrect cast (L) |

## Colours

sRGB hex from the LUT rows (linear, converted; HDR rows normalised by their peak), dark (mask 0) to bright (mask 1).
The `EffectArt` palettes are at the end.

| Name | Where | Hex | Conf |
|---|---|---|---|
| **Heal gold** | heal beam 186F r3 (HDR 5-10) | #644903 #7A6811 #A19320 #C8BF7B #FFF5A2 | H |
| **Boost blue** | boost beam 1870 r3 (HDR 10-15) | #1125FF #2D6FFF #3D95FF #79B2FF #9ECAFF | H |
| Staff blue | staff head 0197 (HDR 15) | #1125FF #1E3BFF #3C6BFF #BDFFFF white | H |
| Staff gold | staff head 0198: colour sprites x tint (3, 2.76, 1.53) | warm gold #FFF6BD | H |
| **Round** | blaster 01E1 r5 | #FF6B00 #FFD800 #FFEF31 #FFF57D #FFFDC1 | H |
| Round, enemy | 01E1 twin r5 | #730000 #8C0000 #D60000 #FF421C #FF8B42 | H colour, M role |
| GA trail | colour strip 20D0 x tint (3.26, 3.26, 1.02) | #4C1919 #8C2803 #E90900 #FF8D00 #FFFEFF (before the tint) | H |
| Wing yellow | GA / Angelic Descent wing glows r4 | #FFFF1A #FFFF56 #FFFF74 #FFFF8A #FFFF9D | H |
| Wing gold | wing motes 0301 r5 (HDR 3) | #000000 #5B4701 #8C6F02 #C19A03 #FFFFA9 white | H |
| Valkyrie gold | feathers 40CD r8 (x1.6) | #FFD300 #FFE425 #FFF437 #FFFF6A #FFFFB3 | H |
| Valkyrie, enemy | 40CD twin r8 (x2.5) | #8B0000 #AA0000 #C40000 #E66300 #FFC11E | H colour, M role |
| Boosted blue | boosted ally 0100 r1 (HDR 3) | #06129C #1C4CB0 #327CD5 #73AAF3 #9ECAFF | H |
| Resurrect gold | 632D sparks r1 (x5.4), 2106 glow r0 (x4) | #FF8E00 #FFBA00 #FFDC00 #FFEB5C white | H |
| Resurrect, enemy | 2106 twin r0 | #000000 #B80000 #FB0000 #FF8C55 #FFC378 | H colour, M role |
| Flash Heal gold | 75A7 beam r4 (HDR 30, x2) | #281B01 #61550E #817218 #BCB78C #FFF9C4 | H |

## Caduceus Staff: the heal and boost beams

| Part | Effect | Conf |
|---|---|---|
| Staff head, heal on | **0198** held on the staff entity's hp 0006 (006B#994) + sounds 12B0 / 12B6 / 2945 | H, M which beam |
| Staff head, boost on | **0197** held on hp 0006 (006B#991) + sounds 12B1 / 2941 / 12B7 | H, M which beam |
| Beam on / off | 19CD / 19CE, 0B8B, 3704, 36AD, 7430: **sound only**; 0242 / 0243 / 0249 / 024A are empty | H |
| Heal beam | graph **0079** on the ally: **186F** (others' view, L) or **01F5** (own view, L) | H |
| Boost beam | graph **006C** on the ally: **1870** (others', L) or **02E0** (own, L) | H |
| Healed-up cue | 0241 and 02FA: **sound only**, on the ally when a ratio passes 0.95 (0079#119 -> #122 / #123; M: their health fraction) | H, M trigger |
| Perk / Stadium beams | 0079: 3695 / 3696 (conditions 0DA6 / 2C6B.134, unnamed); 006C: 6349 (0CA6.134, unnamed), 3DDC / 3DDD (Crepuscular Circle) | H, L roles |

**The beam's shape (H):** a two-point ribbon (spawn attribute 12DC15C5 = 0 at the start, 1 at the end, one point
each, life = the beam's own, ECIM 73) on mesh 0797, from Mercy's staff head (hp 0006 of her staff entity) to the ally's
chest (hp 0001); the effect loops every 3 s. **No curve in the data:** two points make a straight band. The beam node
has a flag set (m_32D6A8F7 = 1) and the ribbon renderers have undecoded ECCB tracks; whether either bends the band toward
her aim isn't decoded (L). The in-game beam is known to bend (from play, not data): ours, see the recipe.

**Heal beam, 186F** (4 ribbons, gold, no enemy row) (H):
- 0.2 m: mask 4DB (cloud noise) x 28016 (soft centre line, 32 x 128), gold #644903 -> #FFF5A2 (HDR 5).
- 0.1 m: the same masks, the same gold; its emitter's period is 1.47 s (M: a texture-scroll loop).
- **0.4 m**: 4DB x **2800B** (wavy streaks, 128 x 256), the brightest: #473302 -> #998C1E -> #FFF5A2 (HDR 10).
- 0.2 m: 4DB x 28016, period 0.67 s.
- 01F5 (own view, L) is the same four widths (0.4, 0.1, 0.2, 0.2 m) and masks, colour peaks 10 / 4 / 4 / 4, and its
  ribbons read dataflow 063C. With it the ally holds **56D3** (below).

**Boost beam, 1870** (3 ribbons, blue, no enemy row) (H):
- 0.15 m: **19167** (noise 512) x **1344** (soft band across, 256 x 8), #1125FF -> #3D95FF -> #9ECAFF (HDR 10).
- **0.5 m**: 19167 x **207EC** (long streaks, 128 x 512), deep blue #010449 -> #2C6FC1 -> #9ECAFF (HDR 15, x10 tint):
  the soft outer glow.
- 0.15 m: 4A7E (noise) x 1344, as the first. Sound 183F.
- 02E0 (own view, L): 0.05 m (4A7E x 1344), 0.5 m (207EC x 19167), 0.15 m (19167 x 1344), the same blues.

**Staff head, heal on, 0198 (1 s loop) (H):**
- Yellow dots (126A, a colour sprite whose dot sits in the top-right of the quad), 50 a second, life 0.82-1.23 s,
  0.1-0.175 m/s in a 50° cone, size 0.075.
- A golden swirl (1268, colour sprite) 6 a second, life 1 s, **size 2 m**, random angle; tint (3, 2.76, 1.53) = warm
  gold; a light of 1.5.

**Staff head, boost on, 0197 (1 s loop) (H):**
- A sphere (085B, flat white mask 1298) about 0.3 m, 5 a second, life 0.25 s, #1125FF -> white (HDR 15).
- Tiny sparks (A17F, 0.01-0.02 m), 50 a second, life 0.15-0.25 s, 4-6.5 m/s along the staff (direction (0, 1, 0), M).
- Soft quads 0.2 m (4A7E x 27BFD), 3 a second, life 1-1.5 s, 0.1 m/s, blue to white.
- Glints (A17F) 10 a second, 0.075-0.15 m, 0.5-0.75 m/s in a 50° cone, life 0.17-0.31 s; a light of 1.5 (hp 0005, M).

**The beamed ally (on graph 0079 / 006C, H links):**
- **Being healed:** **56D3** (only with 01F5, client only, on the ally's 0001): golden swirls (1268) 6 a second, life
  1 s, size growing from 1 m; yellow dots (126A) 25-50 a second, life 0.82-1.23 s, 0.1-0.175 m/s in a 50° cone,
  0.08-0.12; tint x3; a light of 1.5 (H). **2719** (both beams, 2.67 s loop): a body material effect plus a rim quad
  (mask D8F, an **inverted** soft circle) 2 m, 3 a second, life 1 s, drifting 0.56-0.78 m/s; its LUT rows are parameters
  only (L colour: the beam's). 434A: sounds only (00A4).
- **Damage boosted:** **0100** (on begin, held, ally's 0001): a 1 m blue flash (4C09 = `cat_soft`), 7 a second, life
  0.25 s (#06129C -> #9ECAFF); tapered spikes (mesh DFEC, 0.064 x 0.2 m at size 0.2) 6 a second, life 1-1.5 s, in all
  directions, blue (HDR 15, x2.58); sparks (A17F) 90 a second, 0.01-0.05 m, 1-2 m/s in a 50° cone, life 0.17-0.31 s.
  **3169** (held, unless v33781): a blue body shimmer (three material effects on noise 19167, #232DE7 -> #BFC2FF, HDR
  5); **rising chevrons** (mesh DFDC: 0.06-0.08 m at size 0.75-1, stretched x3-3.5 by attribute 6DFA, M) 8 a second,
  life 0.75-1.25 s, rising 1 m/s, spawned over the body (ECIN, M), #92C2FF -> white; a **2 m ground ring** (mesh DAE1,
  masks 212A6 inverted soft square / 2103C near-white cloud) life 2 s. Plus 2719.

**Recipe:**
- Each tick while she beams: from her staff head (ours: 0.45 m forward, 0.35 m right and 0.25 m up from her eyes in third
  person; in first person the view model's staff tip) to the ally's chest (1.1 m above their feet, ours), a band built
  of camera-facing streaks. Heal: a 0.4 m `mercy_beam_wave` layer (MERCY_HEAL_BEAM_N), two 0.2 m and one 0.1 m
  `mercy_beam_core` layers (MERCY_HEAL_BEAM_SOFT_N), all additive, the texture scrolling along the beam (ours: 1-2 m/s;
  the data's periods 3 / 1.47 / 0.67 s). Boost: a 0.5 m `mercy_beam_streak` glow (MERCY_BOOST_BEAM_GLOW_N) around two
  0.15 m `mercy_beam_noise` x `mercy_band` cores (MERCY_BOOST_BEAM_N).
- The bend (ours, L): build the band from 8-12 segments along a quadratic curve whose control point is 1/3 of the way
  along her look direction; the band snaps straight when she looks at the ally. Check against footage.
- Staff head: heal, a soft warm-gold swirl (`cat_heal_swirl`, NATURAL) of about 0.6 m (ours: the data's 2 m swirl is
  mostly transparent) and 2-3 yellow dots (`cat_heal_dot`) a tick drifting off it; boost, a 0.3 m blue glow
  (`cat_soft`, MERCY_STAFF_BLUE_N) with tiny blue sparks.
- On the healed ally: golden swirls and rising yellow dots round their chest (`cat_heal_swirl`, `cat_heal_dot`). On the
  boosted ally: a 1 m blue flash when the beam lands, then blue chevrons rising from their body (ours: a generated
  chevron mask, about 0.25 m, 8 a second, 1 m/s up, 1 s), a faint blue shimmer and a 2 m blue ground ring.
- Enemies see the same gold and blue (the data has no red beam).

## Caduceus Blaster

| Part | Effect | Conf |
|---|---|---|
| Volley | 006D#94 / #544-559 StateWeaponVolley: round 0158 from the blaster's hp 0006 (50 m/s, 100 in Valkyrie) | H |
| In flight | **01E1**, 1 s loop, on child entity 01BA | H |
| Muzzle flash | **01E2**, 0.33 s (round slot m_A18274A3; every emitter on hp 0006) | H slot, M role |
| Hit (creature or wall) | **02F1**, 0.67 s (slots m_AED73588 and m_BF7914D4) | H slot, M role |
| Fly-by | 0F03: sound only | H |
| Reload | clip CF0's effect **01FE.08F** on the blaster's hp 0075 (1.4 s); 670A (end) sound | H |
| Others | 0B8C, 60BE: sound; 3490 empty; 37D6 = Stadium Delayed Discharge (00D0.134) | H |

**In flight, 01E1** (gold, each part with a red enemy twin, H):
- **Core:** quads (masks 4DB x 11DF2 star) 0.45 m (the last of an `ECSb` pair 0.14 / 0.45, M), 6.6 a second plus 2,
  life 1-1.04 s, random angle, #FF6B00 -> #FFFDC1 (x5.4) / enemy #730000 -> #FF8B42.
- A second glow (29F7 flare x 4DB) 0.16 m, 4 a second plus 2.
- **Sparks** (29F7) 35-65 a second, life 0.09-0.44 s, 0.25-0.5 m/s, 0.12-0.18 m, spinning 1-3 rad/s.
- **Tracers:** a cross ribbon (07F5) 0.2 m wide, 60 points a second, each living 0.083 s (4.2 m behind at 50 m/s),
  masks F69D (band) x 754 / 4DB (noise), gold (no red twin, L: own view); a flat ribbon (0797) 0.217 m, life 0.067 s
  (3.3 m), gold / red twin.

**Muzzle flash, 01E2** (all on the muzzle, gold with red twins, H):
- **Flame bursts:** crossed planes (08C0) with mask **4B47** (a flame-shaped burst): 2 at 0.74-1.07 m, 8 at 1.2-1.62 m,
  5 at 0.48-0.71 m, 5 at 0.75-1.18 m, life 0.08-0.1 s, in a 60-64° cone.
- **Flares:** 29F7 1.18 m and 0.41 m, life 0.1 s (enemy twin 2.1 m).
- **Rings:** flat annuli (296B) 0.13-0.64 m, life 0.1-0.2 s (masks 4DB x 4CB7).
- **Sparks:** 5 + 10 thin streaks (D89) 0.12-0.4 m, 1.6-2.8 m/s, life 0.05-0.1 s; 4 flares (29F7) 0.2-0.4 m, 4.1-5.3
  m/s, life 0.1-0.15 s.

**Hit, 02F1** (H): an orange impact disc (20AC, colour sprite, x5) 0.3 m for 0.1 s; 12 tiny discs (20AC) 0.06-0.12 m
flung 0.8-5.7 m/s in a 45° cone off the surface, life 0.1-0.3 s, falling (-9.8, M); a soft glow (139C) 0.3 m, 0.1 s,
orange. No decal.

**Reload, 01FE.08F** (H values, L roles): quads (17E1) 0.3 m, 20 a second plus 5, life 1 s on the blaster's hp 0075; a
cone mesh (E3E) 0.13 m, 5 a second, life 0.5 s.

**Recipe:**
- Round (50 m/s): an additive 0.45 m gold core (`mercy_star` x MERCY_SHOT) in a 0.16 m hot centre (`lucio_spark`),
  a 0.2 m gold tracer ribbon about 0.08 s of travel long (ours: 3-4 m, fading to the tail), and 2-3 gold sparks a tick
  left behind (0.12-0.18 m, living 0.1-0.4 s). Enemy view: MERCY_SHOT_ENEMY.
- Muzzle (her first-person shots: skip or shrink, as Jetpack Cat's): a 0.8-1.2 m flame-shaped flash (`mercy_muzzle`,
  two crossed quads facing along the shot) for 0.1 s, a 1.2 m glow, and 10-15 tiny gold streaks at 2-5 m/s.
- Hit: a 0.3 m orange flash (`mercy_impact`, NATURAL) and 12 tiny orange sparks bouncing off the face, 0.1-0.3 s.

## Guardian Angel

| Part | Effect | Conf |
|---|---|---|
| Flight trails | **019B** (006E#255 / #1185) and **1FAF** (#339 / #1187), held | H, M which phase |
| Wing glows | **17E9** (#272 / #1184), held on 002C / 002E | H |
| Speed lines | **6BC3** (#1147), held | H |
| Others | 689F empty; 098A, 5F58, 009B, 0186 (failed cast): sound; clip effects 01AB / 01AF / 9807: sound | H |
| Target marker | **none in the effect data** (no GA effect is held on the target) | H |

**Trails, 019B / 1FAF** (H):
- A **broad trail**: cross ribbon (07F5) **1.5 m** wide, about 33 points a second, life 0.3 s (5 m behind at 17 m/s),
  masks 13F9 (a dim soft streak) x 4DB, colour strip 20D0 (#4C1919 -> #E90900 -> #FF8D00 -> white) times tint (3.26,
  3.26, 1.02): orange-gold to white. Its anchors: hp 0001 (chest) and the wing tips 002D / 002F (M which ribbon).
- Two **thin trails**: 07F5 ribbons 0.075 m, 60 points a second, life 0.3 s (0.4 s in 1FAF), masks 1344 band x 4DB,
  the same colour (1FAF's tint (3.26, 3.26, 0.20): more saturated gold).
- **Sparks:** 200 a second (139C soft dot), 0.15-0.2 m, life 0.2-0.3 s, 4-4.2 m/s (direction from an ECIN input, M:
  backward), pale yellow (HDR 10).

**Wing glows, 17E9** (H): on each wing (002C / 002E): soft glows (139C x 13CC wisps x 4DB) 1.5-2.1 m, 15 a second
(life 0.3-0.39 s, 0.47-0.52 m/s up in a 25° cone) and 20 a second (1.1-1.4 m/s); yellow dots (126A) 16 a second, 0.09,
life 0.2-0.4 s. Colour MERCY_WING_GLOW (yellow).

**Speed lines, 6BC3** (H values, M placement): streaks (E99F = `cat_speed_line`, size 10 = stretched) 60 a second plus
30 at the start, life 0.22-0.32 s, 4-5 m/s; glints 100 a second plus 120, 0.02-0.05 m, 4-4.2 m/s; pale yellow (#FFFF2C,
HDR 5-25). Spawn position and direction from ECIN inputs (M: a box around her, backward).

**Recipe:**
- While she flies: a 1.5 m soft orange-gold ribbon trail behind her chest about 0.3 s of travel long (`mercy_trail` or
  `cat_fade`, MERCY_GA_TRAIL_T), two 0.075 m gold ribbons from her wing tips (ours: 0.6 m to each side and 0.2 m behind
  her shoulders), soft yellow glows rising off both wings, and pale yellow speed lines streaming past (`cat_speed_line`,
  4-5 m/s backward, 3-4 a tick).
- Target marker: none in the data; the game marks the GA target in its UI (006E#910 StateUXPresenter, texture
  12F3.004, M). Ours: an outline or a small reticle over the targeted ally, UI only.

## Angelic Descent

| Part | Effect | Conf |
|---|---|---|
| Gliding (held Jump) | **019C** held (0071#138) | H |
| Start | 02F5: sound only | H |
| Stadium | 366C (Everlasting, 00A8.134), 377D (A Favor Honored, 00D1.134) | H |

**019C (1 s loop) (H):** on each wing (002C / 002E): soft glows (139C x 13CC x 4DB) **1.5-2.1 m**, 5 a second, life
0.45-0.54 s, rising 0.47-0.52 m/s in a 25° cone; a second set 5 a second at 1.1-1.4 m/s; yellow dots (126A) 16 a
second, 0.09, life 0.2-0.3 s, 0.13-0.32 m/s up. Yellow (MERCY_WING_GLOW).

**Recipe:** while she glides, one soft yellow glow (`mercy_soft` with `mercy_wisp` on top, about 1.2 m, additive, 50%)
every 4 ticks rising slowly off each wing (ours: 0.5 m to each side, 0.3 m behind and 0.5 m above her shoulders), and
1-2 tiny yellow dots (`cat_heal_dot`) a tick. Faint: it is a glow on the wings, not a trail.

## Resurrect

| Part | Effect | Conf |
|---|---|---|
| Cast (held) | 030F (00CA#635): **sound only** | H |
| Cast visuals on her | clip effects: 3P staff 4DAF / blaster 4DB0 -> 2551 / 2550 -> **632D**; 1P 4DAA -> **254D**, 4DAE -> **254E** | H |
| On the dead ally | **2106** via ActionPlayScript 127F (00CA#638, targets v536, inside StateChaseVar #617) | H link, M role |
| On the revived ally | shared graph 00CB: 017B (empty) -> **017D** (held, 2.97 s) + **019E** (held) | H, M role (as `mercy-map.md`) |
| Stadium | Ghost Explosion (0CA9.134): 0178 (cosmetic entity 014F), 5B87, 5C3C; Everlasting (00A8): 3665; 768C-7692, 74F4 (L) | H |
| Missing | 29B6 (00CA#1067) is not in the archive | H |

**Cast, 632D (3P, 2.27 s; hp 0002 = L her hand, and the wings 002C / 002E) (H):**
- During the cast: golden swirls (1268) 6 a second plus 12, 0.07-0.39 m (size curve), life 0.38-0.71 s; soft wing glows
  (139C / 13CC) 15 a second on each wing from 0.33 s, 0.93-1.53 m, rising 0.5 m/s; sparkles (242) 32 -> 64 a second
  from 0.17 s, 0.09-0.18 m; flame tongues (mesh B18 with 14F9, gold #E8E00C -> #FFF8D7, x2.4) 40 a second plus 12-14,
  life 0.33-0.45 s; glows (29F7, red-orange #A53F00 -> #E40000) 1.5 m, 10 a second.
- **At 1.63 s (the end of the 1.75 s cast):** 61 spark blobs (1F35) 2.1-4.4 m/s in all directions, life 0.33-0.73 s,
  orange to white (x5.4); a 3.3 m flare (29F7); a 4 m lens streak (CC29); two **rings** (mesh 0CAE, 0.4) life 0.32 s;
  gold teardrops (1369) 1.2-2.2 m. A light rising to 3.5.
- 1P (254D / 254E): the same with smaller sizes (sparks 0.036, lens 1 m) and no wings.

**On the dead ally, 2106 (2 s; gold, each part with a red twin) (H):**
- A **golden swirl** (1268) growing **5.1 -> 11.4 m** over 2 s (tint pale #FFE5B1; enemy #FF6720).
- A soft glow (A17F) 2.6 -> 5.5 m, 5.2 a second, life 0.86 s, black -> yellow -> white (x3).
- Sparkles (242) 50 a second, 0.09 -> 0.175 m, life 0.46-0.75 s, orange -> white (x10); more at 48 -> 100 a second
  from 0 to 1.47 s, rising 2.2 m/s (M).
- **At 1.67 s:** a 2.6 m glow (242, x4) and a 2.6 m horizontal lens streak (CC29), life 0.86 s.

**On the revived ally, 017D (2.97 s) and 019E (H):**
- 017D: a body material effect (2.17 s); the **halo**: two-three rings (mesh 0CAE with the yellow ring sprite **1411**)
  0.8-1.1 m, life 0.16-0.3 s (a flash, M: the halo); a 1.4 m white sphere (085B) for 0.3 s; golden swirls (1268) 12
  plus 6 a second, 1.4-2 m, life 0.38-0.71 s; soft glows 100 plus 31 a second, 1.3-1.9 m, life 1.2 s, rising 0.1-0.3
  m/s; **1000 soft dots a second** (4F3 = `lucio_soft`) 0.16 m, life 0.15-0.76 s, 0.4-1.7 m/s; lights of 3 from 1.7 s.
- 019E (1 s loop): **golden plus crosses** (mesh E0AA, size 3 = 0.22 m, masks 27E5E soft cross / 7A9) **25 a second**,
  life 0.25-0.75 s, rising 1.7-3.1 m/s in a 60° cone from their chest, orange -> yellow (#FFCD49 -> white, HDR 5); a
  1-1.2 m sphere (085B) once a second, life 1.2 s; sparks (242) 7 a second, 0.12 m, 1-1.8 m/s.

**Recipe:**
- Cast (1.75 s): golden swirls (`cat_heal_swirl`) and sparkles (`mercy_glow`, MERCY_RES_SPARK) round her hands, soft
  yellow glows rising off her wings; at the end a burst of 60 gold sparks (`spark#4`, 2-4 m/s), a 3 m flare and a 4 m
  horizontal lens streak (`mercy_lens`).
- On the dead ally during the cast: a golden swirl (`cat_heal_swirl`, NATURAL, 50% alpha) growing from 2 m to 5 m
  (ours: the data's 11 m fills the screen in Minecraft), a soft gold glow growing 2.6 -> 5.5 m, and rising gold
  sparkles; at the end a 2.6 m flash and lens streak. Enemy view: MERCY_RES_HALO_ENEMY.
- When they stand up: a halo (`mercy_halo`, NATURAL, flat ring about 1 m over their head, ours) flashing for 0.3 s, a
  1.4 m white flash, golden swirls and a cloud of soft gold dots rising; then for 1 s, golden plus crosses
  (`mercy_plus`, MERCY_PLUS) 1-2 a tick rising 2-3 m/s from their chest.

## Flash Heal

| Part | Effect | Conf |
|---|---|---|
| Cast | 64E1 (5D59#565), 65EA (#560): **sound only** | H |
| Staff head | **75A6** held on the staff entity's hp 0006 (#626) | H |
| Beam to the target | **75A7**, STU_9932D728 #630 / #683-687: from the staff's hp 0006 to targets v536[0..5], hp 0001 | H |
| Heal burst | **642D** held (#568, #697-701: one per target, M) | H, M where |
| Others | 5F58, 009B: sound | H |

**Staff head, 75A6 (1 s) (H):** a ring flash (4DB x 3203 = `cat_ring`) 1 m, life 0.6 s, gold -> white (HDR 5); 50
yellow dots (126A) 1-1.07, life 0.7-1.2 s, 0.1-0.175 m/s; a light of 1.

**Beam, 75A7 (1 s) (H):** one two-point ribbon (0797) 1.5 m wide, masks 183A7 (`cat_streak`) x 1CB5D (a fine web,
1024²), gold #281B01 -> #817218 -> #FFF9C4 (HDR 30, x2). Straight, from her staff head to the target's chest.

**Heal burst, 642D (1 s) (H values, M place: the target):** a ring flash (4DB x 3203) 1 m, life 0.6 s, gold -> white;
a ground ring (mesh DAE1) 2 m, life 2 s; a golden swirl (1268) **4 -> 15 m** in 0.6 s; yellow dots (126A) 60 plus 28-33
a second for 0.13 s, life 0.49-0.71 s, 0.78-0.86 m/s, size 1-2; a light rising 1.5 -> 4 over 0.6 s.

**Recipe:** at the cast, a straight 0.5 m gold band (ours: 1.5 m is the quad width of a mostly thin streak mask) from her
staff head to the ally's chest fading over 0.4 s (`cat_streak` x MERCY_FLASH_N); a 1 m gold ring flash at her staff;
on the ally a 1 m gold ring flash, a golden swirl opening from 1 to 4 m (ours: 15 m is too big), 20-30 yellow dots
popping out, and a 2 m gold ground ring.

## Valkyrie

| Part | Effect | Conf |
|---|---|---|
| Cast | 1FA0 (10DA#195): **sound only**; 3P cast clip 4A17's effect **2372.08F** (1.5 s) | H |
| Wing sheets, held 15 s | **1F7B** (10DA#182) | H |
| Feather glows, held | **40CD** (#480); 602B (#519) the same under a skin condition (0A2E.134) | H |
| Chained beams | the heal / boost beams above, started at her chest (hp 0001) when the 360° mode flag is set (0079#294 / 006C#450) | H nodes, M meaning |
| Other | 68D6 empty | H |

**Cast burst, 2372 (at 0.6 s into the 1.5 s cast) (H):** from each wing (002C / 002E): 43-44 spark blobs (1F35 = `spark`)
at 5.6-7.2 m/s in a 24° cone, life 0.68-1.12 s, gold #FFDC00 -> #FFF9AC (enemy red twin); 10 big soft glows (139C x
13CC wisps) at 8.7-14 m/s in a 21° cone, life 0.57-1 s, yellow.

**Wing sheets, 1F7B (2.97 s loop) (H):** the curved wing meshes 5365 / 5366 at **size 3.7** (about 4.6 x 3.6 m each),
one emitter per wing hardpoint (007E, 0080, 0155, 0157, 0158, 015A, 015B, 015D, 0169, 016B: 10 emitters, 5 per mesh),
each 1.5 a second, life 1.1-1.4 s, drifting up 0.07 m/s; masks 11B97 (a long thin spike) x 11BA6 (soft bar) x 4DB;
gold row r8 #FFD300 -> #FFFFB3 (x1.6), enemy row r9 #8B0000 -> #FFC11E; a wing material effect; spark blobs (1F35) on
002C / 002E, 1.7-4.1 m/s sideways (spread -0.35..0.4, M), life 0.68-1.12 s, gold / red rows r5 / r6; two lights of 2.4.

**Feather glows, 40CD (2.97 s loop) (H):** quads (10F4E) **3.7 m**, 2.2 a second on each of the 10 wing hardpoints,
life 0.76-1.07 s, masks 11B97 / 11BA6 / 4DB: gold (x1.6) with a red twin (x2.5); spark blobs as 1F7B's; four lights of
2.4.

**Recipe:**
- Cast (after 0.6 s): from each wing a burst of 40 gold sparks (`spark#4`, MERCY_VALK_SPARK) at 6 m/s in a narrow cone
  backward-up, and 10 big soft yellow glows thrown 9-14 m/s.
- While it lasts (15 s): on each side of her back, a fan of 5 long feather streaks (`mercy_spike` x `mercy_feather`,
  MERCY_VALK, additive) about 2-3.7 m long (ours: shorter in first person), each re-spawned every 0.5 s and living 1 s
  so they shimmer, plus gold sparks drifting off the wing tips. Enemy view: MERCY_VALK_ENEMY / _SPARK_ENEMY.
- Chained beams: draw the heal or boost band (above) from her chest to every ally the beam reaches.

## Her wings at rest

| Part | Effect | Conf |
|---|---|---|
| Always on | **0301** on entity 02B8 (a child of her hero entity 00E4: STUEffectComponent) | H |

**0301 (1 s loop) (H):** on the two wing hardpoints 002C / 002E (0.1 m up): motes on the octagon quad 1E76A, 5 a second
each, life 0.8-1.5 s, drifting 0.19-0.27 m/s in random directions (spread +-1.2), masks 39115 (soft ball =
`cat_soft_ball`) x 1038 (noise) x 5A7F (streaky noise); gold MERCY_WING_GOLD (#5B4701 -> #C19A03 -> #FFFFA9 -> white,
HDR 3). Spawn size 0 with the size from a renderer track (not decoded: L).

**Recipe:** each wing sheds a faint gold mote (`cat_soft_ball`, MERCY_WING_GOLD_N, ours 0.15-0.3 m) every 4 ticks,
drifting 0.2 m/s and fading over 1 s. Very faint; skip in first person.

## Practical notes for Overcraft

- Pre-colour the grey masks with the palettes below, additive, depth-tested, no depth writes. The colour sprites
  (1268, 126A, 13CC, 20AC, 1369, 1411) are drawn as they are (`Palette.NATURAL`): their shape is in alpha.
- HDR rows: clamped, a 5-30x row turns white past its first quarter. For the beams use the `_N` (normalised) rows to
  keep the gold and the blue (M: our choice; the game's bloom does the rest).
- Skip the distortion pieces (BC5 B0A, AFF, 0B08, 0B0A) and the material effects (body overlays): Minecraft has no
  per-entity shader here (ours: a faint additive shell or nothing).
- Times in seconds; held effects loop every 1 s (beams 3 s, Valkyrie 2.97 s).
- Masks already in the pack: `cat_soft` (4C09), `cat_glow_dot` (A17F), `cat_heal_swirl` (1268), `cat_heal_dot`
  (126A), `cat_ring` (3203), `cat_streak` (183A7), `cat_speed_line` (E99F), `cat_soft_ball` (39115), `cat_fade` (07A9),
  `spark` (1F35), `lucio_spark` (29F7), `lucio_soft` (04F3).

## Not found / not certain

- The beam's bend: the data has two points; the bend (if the engine makes one) is in the beam node or the undecoded ECCB
  renderer tracks (L). The heal / boost glow assignment of 0197 / 0198 (M, by colour); which beam look is whose view
  (01F5 / 186F, 02E0 / 1870: L); the conditions 0DA6 / 2C6B / 0CA6 .134 (unnamed: 3695, 3696, 6349).
- A Guardian Angel target marker in the world (none: UI only, M); which of 019B / 1FAF plays in which GA phase.
- Where 642D (Flash Heal's burst) plays (M: on each target) and why 5D59 has six beam nodes (M: Stadium multi-target).
- The bones behind 002C-002F, 007E / 0080, 0155-016B (wing bones, M) and 0002 (L: hand); the in-flight round's hp
  0006 (on the round, M).
- Sizes set by renderer tracks (the wing motes' size, the chevrons' stretch), ECIN spawn inputs (speed lines, chevrons),
  light colours, ribbon space.
- 29B6 (Resurrect, 00CA#1067) is not in the archive.

## Masks (`tools/maskstat.py`)

| Mask | Size | min / max, corner / edge / centre (red) | Flag |
|---|---|---|---|
| 04DB noise | 256² | 11 / 204, 112 / 46 / 161 | tiling cloud: a multiplier, not a shape |
| 2800B beam wave | 128 x 256 | 0 / 255, 0 / 81 / 85 | streaks along V |
| 28016 beam core | 32 x 128 | 0 / 252, 0 / 65 / 249 | bright centre line |
| 19167 noise | 512² | 18 / 241, 149 / 49 / 153 | tiling noise |
| 207EC streaks | 128 x 512 | 0 / 232, 0 / 0 / 38 | thin streaks |
| 1344 band | 256 x 8 | 0 / 255, 0 / 255 / 255 | strip (dark ends, bright middle) |
| 4A7E noise | 256² | 25 / 246, 79 / 27 / 173 | tiling noise |
| **0D8F rim** | 256² | 0 / 255, **255** / 107 / **0** | **INVERTED**: black centre, white corners |
| 139C soft | 256² | 0 / 245, 0 / 0 / 245 | soft ball |
| 13CC wisps | 128² | R 211-255, **alpha 0-245** | **colour sprite** (shape in alpha) |
| 0242 glow | 128² | 0 / 255, 0 / 0 / 255 | hot-centre glow |
| 4B47 muzzle | 128² | 0 / 255, 0 / 0 / 232 | flame burst |
| 0D89 streak | 64 x 256 | 0 / 213, 0 / 0 / 184 | thin streak |
| 11DF2 star | 128² | 0 / 255, 0 / 0 / 255 | star flare |
| 20AC impact | 128² | R 245-255, **alpha 0-255** | **colour sprite** |
| 13F9 trail | 128² | 0 / **165**, 0 / 14 / 24 | **dim** (max 165): scale up |
| CC29 lens | 256 x 128 | 0 / 255, 0 / 0 / 255 | horizontal lens streak |
| 1411 halo | 512² | R 250-255, **alpha 0-255** | **colour sprite** (yellow ring) |
| 1369 flame | 128 x 256 | R 253-255, **alpha 0-252** | **colour sprite** |
| 27E5E plus | 128² | 0 / 253, 0 / 0 / 253 | soft plus cross |
| 11BA6 feather | 64 x 256 | 0 / 228, 0 / 0 / 183 | soft bar |
| 11B97 spike | 256² | 0 / 255, 0 / 0 / 255 | long thin spike |
| 5A7F wing noise | 256² | 54 / 243, 105 / 61 / 147 | streaky noise |
| 1CB5D web | 1024² | 5 / 255, 35 / 155 / 60 | fine web noise (large file) |
| 1268 / 126A (`cat_heal_*`) | 512² / 128² | alpha shapes | **colour sprites**; 126A's dot is off-centre (top right) |

Not proposed, flagged: 27D0D / 27E5D / 212A6 (inverted frames), 2103C (near-white cloud, min 214: a multiply-style
texture), 1298 and 0460 (flat white: the mesh is the shape), 0724 (black in red).

## Textures to add (`build_asset_pack.py`, `EFFECT_MASKS`)

With `research\mercy\fx` passed to `build_asset_pack.py effects` as a source:

```python
    # Mercy's (tools/reference/mercy-effects.md): cloud noise, the beams' wavy streaks, centre line, noise, long
    # streaks and band, a second noise, the beamed ally's rim (INVERTED: black centre)
    "mercy_noise": ("Mercy", "CaduceusStaff", "0000000004DB", 1),
    "mercy_beam_wave": ("Mercy", "CaduceusStaff", "00000002800B", 1),
    "mercy_beam_core": ("Mercy", "CaduceusStaff", "000000028016", 1),
    "mercy_beam_noise": ("Mercy", "CaduceusStaff", "000000019167", 1),
    "mercy_beam_streak": ("Mercy", "CaduceusStaff", "0000000207EC", 1),
    "mercy_band": ("Mercy", "CaduceusStaff", "000000001344", 1),
    "mercy_noise_b": ("Mercy", "CaduceusStaff", "000000004A7E", 1),
    "mercy_rim": ("Mercy", "CaduceusStaff", "000000000D8F", 1),
    # wing glows: soft ball, yellow wisps (a colour sprite: NATURAL)
    "mercy_soft": ("Mercy", "AngelicDescent", "00000000139C", 1),
    "mercy_wisp": ("Mercy", "AngelicDescent", "0000000013CC", 1),
    # the blaster: hot glow, muzzle flame burst, thin streak, star flare, impact disc (colour sprite)
    "mercy_glow": ("Mercy", "CaduceusBlaster", "000000000242", 1),
    "mercy_muzzle": ("Mercy", "CaduceusBlaster", "000000004B47", 1),
    "mercy_streak": ("Mercy", "CaduceusBlaster", "000000000D89", 1),
    "mercy_star": ("Mercy", "CaduceusBlaster", "000000011DF2", 1),
    "mercy_impact": ("Mercy", "CaduceusBlaster", "0000000020AC", 1),
    # Guardian Angel's trail (dim: max 165)
    "mercy_trail": ("Mercy", "GuardianAngel", "0000000013F9", 1),
    # Resurrect: lens streak, the halo ring and the gold teardrop (colour sprites), the plus cross
    "mercy_lens": ("Mercy", "Resurrect", "00000000CC29", 1),
    "mercy_halo": ("Mercy", "Resurrect", "000000001411", 1),
    "mercy_flame": ("Mercy", "Resurrect", "000000001369", 1),
    "mercy_plus": ("Mercy", "Resurrect", "000000027E5E", 1),
    # Valkyrie's feathers: soft bar, long spike; the wings' streaky noise; Flash Heal's beam web (1024²)
    "mercy_feather": ("Mercy", "Valkyrie", "000000011BA6", 1),
    "mercy_spike": ("Mercy", "Valkyrie", "000000011B97", 1),
    "mercy_wing_noise": ("Mercy", "Wings", "000000005A7F", 1),
    "mercy_web": ("Mercy", "FlashHeal", "00000001CB5D", 1),
```

Every file was checked to exist (`work\fx_palettes.txt`). `EffectArt` reads masks from the red channel: `mercy_wisp`,
`mercy_impact`, `mercy_halo`, `mercy_flame` (and the reused `cat_heal_swirl` / `cat_heal_dot`) must be drawn with
`Palette.NATURAL`; `mercy_rim` must be inverted before use (or given a `BLACK_POINT`-style flip). No chevron texture
exists (the boost arrows are a mesh, DFDC, with an inverted frame mask): generate one, as `softCross` (ours).

Meshes worth converting (`EFFECT_MODELS`, optional): the chevron `"mercy_chevron": ("Mercy", "CaduceusStaff",
"00000000DFDC")`, the plus `"mercy_plus_mesh": ("Mercy", "Resurrect", "00000000E0AA")`, the wing sheets 5365 / 5366
(Valkyrie).

## Palettes (`EffectArt.Palette`)

Linear, clamped at 1, (stop, r, g, b). Rows that rise from black keep the game's positions (0, .25, .5, .75, 1); rows
that start coloured get black at 0 and the game's values at .25 / .5 / .75 / 1 (Lúcio's convention). HDR rows also get a
`_N` line normalised by their peak, which keeps the hue. Generated by `research\mercy\scripts\fx_m_palettes.py`
(`work\fx_palettes.txt`).

```java
		/** Mercy's heal beam 186F C14 (0.4 m ribbon; 8003A285 r3, HDR 10): gold to pale yellow. */
		MERCY_HEAL_BEAM(0, 0, 0, 0, .25f, 1, 1, .068f, .5f, 1, 1, .139f, .75f, 1, 1, 1, 1, 1, 1, 1),
		/** The same normalised by its peak: keeps the gold (use this for the beam). */
		MERCY_HEAL_BEAM_N(0, 0, 0, 0, .25f, .192f, .148f, .007f, .5f, .32f, .263f, .014f, .75f, .611f, .554f, .209f, 1, 1, .913f, .361f),
		/** Heal beam 186F C10/C12/C16 (0.2 / 0.1 / 0.2 m; 8006E3CC r3, HDR 5). */
		MERCY_HEAL_BEAM_SOFT(0, 0, 0, 0, .25f, .984f, .695f, .028f, .5f, 1, 1, .08f, .75f, 1, 1, .981f, 1, 1, 1, 1),
		MERCY_HEAL_BEAM_SOFT_N(0, 0, 0, 0, .25f, .197f, .139f, .006f, .5f, .358f, .295f, .016f, .75f, .576f, .523f, .196f, 1, 1, .913f, .361f),
		/** Boost beam 1870 C8/C12 (0.15 m; 8000D0E4 r3, HDR 10): blue to sky. */
		MERCY_BOOST_BEAM(0, 0, 0, 0, .25f, .266f, 1, 1, .5f, .476f, 1, 1, .75f, 1, 1, 1, 1, 1, 1, 1),
		MERCY_BOOST_BEAM_N(0, 0, 0, 0, .25f, .027f, .16f, 1, .5f, .048f, .301f, 1, .75f, .192f, .445f, 1, 1, .342f, .59f, 1),
		/** Boost beam 1870 C10 (0.5 m glow; 8000F404 r3, HDR 15): deep blue to sky. */
		MERCY_BOOST_BEAM_GLOW(0, 0, 0, 0, .25f, .193f, 1, 1, .5f, .382f, 1, 1, .75f, 1, 1, 1, 1, 1, 1, 1),
		MERCY_BOOST_BEAM_GLOW_N(0, 0, 0, 0, .25f, .013f, .081f, .301f, .5f, .025f, .161f, .535f, .75f, .181f, .373f, .766f, 1, .342f, .59f, 1),
		/** The staff head while boosting (0197 C18, 800FDFB8 r7, HDR 15): blue to pale cyan. */
		MERCY_STAFF_BLUE(0, 0, 0, 0, .25f, .125f, .752f, 1, .5f, .245f, 1, 1, .75f, .509f, 1, 1, 1, 1, 1, 1),
		MERCY_STAFF_BLUE_N(0, 0, 0, 0, .25f, .008f, .05f, .301f, .5f, .016f, .1f, .535f, .75f, .034f, .214f, .766f, 1, .342f, .59f, 1),
		/** The boosted ally's flash (0100 C8, 80080D4D r1, HDR 3): deep blue to sky. */
		MERCY_BOOSTED(0, 0, 0, 0, .25f, .037f, .222f, 1, .5f, .096f, .608f, 1, .75f, .517f, 1, 1, 1, 1, 1, 1),
		/** Her blaster round (01E1 C21, 80023619 r5): orange to pale yellow. */
		MERCY_SHOT(0, 0, 0, 0, .25f, 1, .695f, 0, .5f, 1, .866f, .032f, .75f, 1, .912f, .207f, 1, 1, .982f, .533f),
		/** Her round as enemies see it (01E1 C27, 800DCC79 r5): dark red to orange. */
		MERCY_SHOT_ENEMY(0, 0, 0, 0, .25f, .469f, 0, 0, .5f, .766f, 0, 0, .75f, 1, .055f, .012f, 1, 1, .258f, .054f),
		/** Guardian Angel's trail (colour strip 20D0 times the trail's tint (3.26, 3.26, 1.02), clamped). */
		MERCY_GA_TRAIL_T(0, .236f, .032f, .01f, .25f, .855f, .069f, .001f, .5f, 1, .009f, 0, .75f, 1, .868f, 0, 1, 1, 1, 1),
		/** Wing glows (GA 17E9 / Angelic Descent 019C, 800B92F6 r4): yellow to pale yellow. */
		MERCY_WING_GLOW(0, 0, 0, 0, .25f, 1, 1, .092f, .5f, 1, 1, .174f, .75f, 1, 1, .255f, 1, 1, 1, .337f),
		/** Wing motes at rest (0301, 80074C3E r5, HDR 3): black to gold to white. */
		MERCY_WING_GOLD(0, 0, 0, 0, .25f, .269f, .163f, .001f, .5f, .551f, .338f, .007f, .75f, 1, 1, .397f, 1, 1, 1, 1),
		MERCY_WING_GOLD_N(0, 0, 0, 0, .25f, .09f, .054f, 0, .5f, .184f, .113f, .002f, .75f, .517f, .417f, .132f, 1, 1, .913f, .392f),
		/** Valkyrie's feathers (40CD C19, 8009E3E3 r8, x1.6 in game): gold to pale yellow. */
		MERCY_VALK(0, 0, 0, 0, .25f, 1, .781f, .019f, .5f, 1, .911f, .038f, .75f, 1, 1, .143f, 1, 1, 1, .451f),
		/** Valkyrie's feathers as enemies see them (40CD C23, 800FA2A3 r8, x2.5): dark red to orange. */
		MERCY_VALK_ENEMY(0, 0, 0, 0, .25f, .405f, 0, 0, .5f, .552f, 0, 0, .75f, .794f, .124f, 0, 1, 1, .533f, .013f),
		/** Valkyrie's sparks (1F7B C26, 80060C6F r5): gold to pale yellow. */
		MERCY_VALK_SPARK(0, 0, 0, 0, .25f, 1, .724f, .015f, .5f, 1, .799f, .148f, .75f, 1, .873f, .279f, 1, 1, .947f, .412f),
		/** Valkyrie's sparks, enemy row (80060C6F r6): red to orange. */
		MERCY_VALK_SPARK_ENEMY(0, 0, 0, 0, .25f, 1, .076f, 0, .5f, 1, .152f, 0, .75f, 1, .227f, 0, 1, 1, .624f, .08f),
		/** Resurrect's burst sparks (632D C24, 800948DD r1, x5.4): orange to white. */
		MERCY_RES_SPARK(0, 0, 0, 0, .25f, 1, .495f, 0, .5f, 1, .72f, 0, .75f, 1, .829f, .106f, 1, 1, 1, 1),
		/** Resurrect's glow on the dead ally (2106 C29, 800B7236 r0, x4): black to yellow to white. */
		MERCY_RES_HALO(0, 0, 0, 0, .25f, .485f, .448f, 0, .5f, .971f, .895f, 0, .75f, 1, .959f, .403f, 1, 1, 1, .838f),
		/** The same as enemies see it (2106 C35, 80052D9B r0): black to red to peach. */
		MERCY_RES_HALO_ENEMY(0, 0, 0, 0, .25f, .485f, 0, 0, .5f, .971f, 0, 0, .75f, 1, .262f, .09f, 1, 1, .545f, .188f),
		/** The revived ally's rising crosses (019E C7, 800CBECB r2, HDR 5): orange to yellow to white. */
		MERCY_PLUS(0, 0, 0, 0, .25f, 1, .689f, .101f, .5f, 1, .767f, .136f, .75f, 1, .843f, .17f, 1, 1, 1, 1),
		/** Flash Heal's beam (75A7 C4, 8008E467 r4, HDR 30), normalised: brown-gold to pale yellow. */
		MERCY_FLASH_N(0, .021f, .011f, 0, .25f, .122f, .091f, .005f, .5f, .222f, .171f, .01f, .75f, .504f, .472f, .264f, 1, 1, .947f, .552f),
```

Notes on the palettes: the clamped HDR rows (MERCY_HEAL_BEAM, MERCY_BOOST_BEAM*, MERCY_STAFF_BLUE, MERCY_FLASH) go
white from a quarter up, as the game's bloom does; the `_N` rows keep the hue and are the ones to use for thin bands.
MERCY_FLASH_N starts slightly above black (the game's row does). The staff head's heal glow and the healed ally use the
colour sprites 1268 / 126A as they are, times the warm tint (3, 2.76, 1.53): no palette needed.
