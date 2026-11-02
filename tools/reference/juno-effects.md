# Juno's ability effects in the game data

What Juno's effects are made of in real Overwatch, where the extracted files are, and how to rebuild each one in Minecraft.
Read 2026-11-01 from the archived offline build 154088 (prometheus-2_25_0_0-154088). Overwatch and Battle.net were checked
closed (`Get-Process Overwatch, Battle.net`) before every TankLib and Blender run, and `owinit.py` refuses on its own;
everything was read only, offline, through TankLib/DataTool. Text only in the repo: the textures and meshes are Overwatch
assets and stay in the owner's archive; they reach the game only through the local asset pack. The format is
[brigitte-effects.md](brigitte-effects.md)'s (itself [sombra-effects.md](sombra-effects.md)'s and [ana-effects.md](ana-effects.md)'s);
this file adds what is different for her. Her data map: [juno-map.md](juno-map.md); numbers: [juno.md](juno.md); plan:
[juno-plan.md](juno-plan.md); the lessons: [reading_game_data](../../techniques/game-data/reading_game_data.md#a-ranged-healers-effects-junos-pass-2026-11-01).

Confidence after each number or claim: **H** read in the data, **M** inferred (one link guessed), **L** guess. "Size" is the
spawn size value: it scales the mesh (a 1 m quad at size 0.5 is 0.5 m; a ribbon's size is its width). "a-b" is a random value
between the two. "Ours" marks what no data gives. Palettes: `JUNO_*` (below and in
`E:\Overwatch Archive\research\juno\work\fx_palettes.txt`, every one starting at black at 0). Pack masks: `juno_*` (end of this
file).

## Files

`E:\Overwatch Archive\research\juno\fx\Juno\<Ability>\` with `<Ability>` one of `Mediblaster`, `PulsarTorpedoes`, `HyperRing`,
`OrbitalRay`, `Overboots` (double jump, hover, the boots), `GlideBoost`: `textures\`, `ramps\` (float LUTs), `models\Models\<id>.00C\`,
`manifest.json`. Same layout as Brigitte's. `HyperRing\models\Models\000000015AD6.00C` is the ring mesh exported **with its team look
1BABB**, `HyperRing\models_look_1BC0D\` the same mesh with the **enemy look 1BC0D**; the look's masks 3FE22 / 3FD49 / 2A907 / 1038 are
copied into `HyperRing\textures\`, and both looks' LUTs are in `HyperRing\ramps\` (80073F0A0946, 800D3BE4E436).

Research files in `E:\Overwatch Archive\research\juno\work\`: `fx_cards.txt` (one card per renderer: what this note was written
from), `fx_digest.txt`, `fx_overview.txt` (one line per effect), `fx_summary2.txt|json`, `fx_meshes2.json`, `fx_palettes.txt`,
`fx_sheets\tex_<Ability>.png` (every mask), `fx_sheets\meshes_<n>.png`, `fx_sheets\ring_look_15AD6.png`, `fx_effects\` (decoded
effects), `fx_graph_effects.json` / `fx_graph_nodes.txt` (every effect her graphs name, with the nodes leading in), `fx_entities.txt`,
`fx_clip_effects.txt` (every clip with an animation effect, 159), `fx_ring_owmats.txt` / `fx_ring_materials.txt` (the ring's two looks),
`fx_extract_log.txt`.

Scripts in `E:\Overwatch Archive\research\juno\scripts\fx\`: Brigitte's `fx_brigitte_*` copied as `fx_juno_*`, paths retargeted
(their older history lines kept as they were). `fx_juno_extract.py` holds her effect list, with the clips' animation effects (08F).
New: `fx_juno_clipfx.py` (clips with an 08F effect -> `fx_clip_effects.txt`), `fx_juno_savelook.py` (an effect mesh with a second
look). TankLib scripts run under the kiriko-anim venv's Python with `-I`; the others under `py -I run.py`.

Counts: 114 effects extracted (graphs, entities and 37 clip effects, plus sub-effects 4010 / 4057 / 5C36); 32 with renderers,
decals, material effects or lights, the rest sound only or empty. Nothing missing in the extraction.

## What's different from Brigitte and the others

- **Her look is cyan / yellow-white for her team, red for enemies, and nearly every LUT carries both rows (H rows, M roles).** The
  Hyper Ring settles it: the ring mesh is drawn with two looks, **1BABB** (LUT row 1 navy -> blue, HDR 1.78) and **1BC0D** (the same
  row red), picked per viewer. The particle LUTs pair a blue / cyan / gold-white row with a red one the same way (r3 / r4, r0 / r1 ...).
- **Her ability graphs hold the visuals, unlike Brigitte's (H).** The clips add little: reload (60BA / 5FA4), the double jump's feet
  jets (5F7B / 5FA9 -> 5C36), the torpedo pods' launch trails (62B6), the ult cast's ribbon (61AD). Jump / land / glide clips are camera
  shakes or empty.
- **The lock-on marker is a HUD widget, not an effect (H).** 4B99 #158 is a `StateUXPresenter` showing UX element **0727.116** at the
  target's hp 0001 with the lock progress (v10349 / v1723) and an ally / enemy flag (5BC2). 4B99's only effect, 5C17, is a sound.
- **The ray's column is drawn by offsets, not a tall mesh (H).** 54F9 places a 5.6 m tube (15A21) at **+200 / +250 m** and rings at
  **+50 / +100 / +150 / +196 m** above the ground point (hp 01DA): the beam comes from the sky.
- **Projectile slots read as Mercy's / Jetpack Cat's (H slots, M roles):** `m_A18274A3` muzzle, `m_2170A015` fly-by (sounds),
  `m_AED73588` hit a hero, `m_707DF34C` hit an ally, `m_BF7914D4` the world. The Mediblaster's enemy hit 5841 is **empty**: an enemy
  shows no hit of hers (the shared damage response).
- **Stadium looks are gated by `.134` powers (H):** 7E01 / 7AFA (501A #406 / #397 on power 2AFA.134), 64DF (4858, 0DEC / 2BBA),
  2E8D / 2BE7 (501A #369 / #370, 0DEF), 65B5 / 5540, the enemy torpedo 65C3's 6099. Left out.

### Hardpoints

| Id | Where | Conf |
|---|---|---|
| 0006 | the Mediblaster muzzle | H |
| 007C / 01B6 / 0159 / 000B | gun side (bone_2B98) / reload points | H id, M role |
| 01B5 | the gun piece 5ADB (bone_2B9D), swapped in the reload | H |
| 048D / 048E | the torpedo pods' launch points (bones 0182 / 0183, (±0.038, 1.351, -0.191)) | H |
| 051D / 051E | the pods' flaps (bones 3CFB / 3CFC): the 3P fire trails | H |
| 04C9 | the lock add's glint (8A7D) | H id, L place |
| 0002 | the Hyper Ring's launch (not in her model's table: L engine default, her hand) | H id, L place |
| 0078 / 007A | the ring launch's lights (5AE7) | H id, L place (hands) |
| 007B / 007E / 0080 / 0079 | the ult cast on her hand (5CCA, 61AD) | H id, M (hand / camera forward) |
| 01DA | the ray's ground point (54F9) | H |
| 0019 / 001A | the boots (5985) | H id, M |
| 001B / 001C | hover jets (5D10), sub-points 002C / 002E | H |
| 002C / 002E | the glide and double-jump jets (5A40, 5C36) | H |
| 0001 | a target's centre (torpedo heal, lock marker, ally aim) | H |
| 003D / 015C / 0142 | inside the torpedoes / ring entities (centre / nose) | H id, M |
| 00A4 | the sound point | H |

### Meshes

| Id | Shape (model units, x size) | Used for | Pack model |
|---|---|---|---|
| **15AD6** | a **flat ring** 0.37 m across, 0.056 thick (look 1BABB / 1BC0D) | the Hyper Ring, scale (25, 5, 25) = **9.25 m across** | `juno_ring` |
| 15B77 | the same ring (particle copy) | ring sprites at size 25.5 (9.4 m) | (use `juno_ring`) |
| **15A26** | a **fat torus** 3.46 m across, tube 0.85 | muzzle / hover / glide rings (size 0.03-0.05), the ray's rings (size 1.5-8) | `cat_torus` (in the pack) |
| **15A21** | a **tube** 0.9 m across, 5.6 m long (down -Z) | the ray's column (size 6), reload | `juno_column` |
| 1F85 | an open wall 1.18 across, 1 tall | the ray's low wall | `juno_ray_wall` |
| EEB | a cone 1.82 across, 2.88 long | the ult cast | `juno_cone` |
| 160EA | a **half sphere** 1 m | torpedo noses, the heal hit | `juno_half_shell` |
| 12F7D | a star of cones 2.2 m | the pods' charge burst | `juno_pod_burst` |
| D721 / DBF / 1385 / 85B | spheres 2 m / 1 m / 1 m / 1 m | ring end, torpedo cores, reload, flow | `juno_sphere` |
| EBEF | a thin torus 1 m | the ray's centre ring | `juno_torus_thin` |
| 5E5E | a flat disc 1 m (inner hole r 0.14) | the ring's disc flashes (size 16-18) | `juno_disc` |
| 14063 / D06 | curved blades 1.1 m | the ult cast | `juno_cast_blade` |
| 11E07 | a bow-tie 0.57 x 0.8 | the lock add's glint | - |
| E21 / F5B / 801 / 10F4E / DAE1 / 5708 | quads (E21 a cone-facing quad from its edge), a screen ring, a hex disc | sprites, jets, screen | - |
| E71 / 7F5 / 797 | ribbon profiles | trails | - |

## Mediblaster

| Part | Effect | Conf |
|---|---|---|
| **Muzzle** | **51DA** (538E `m_A18274A3`), on hp 0006, + 4010 (sound) | H slot, M role |
| **The round** | **53B6** (538E's effect component), 0.33 s, + 4057 (sound) | H |
| Fly-by | 5C5F: sound | H |
| **Hit an ally** | **58B4** (`m_707DF34C`) | H slot, M role |
| Hit an enemy | **5841** (`m_AED73588`): **empty** | H |
| **Hit the world** | **53B7** (`m_BF7914D4`): a decal | H |
| Volley | 5C2D (4445 #1211): sounds; 2B22 / 2B44: empty | H |
| **Reload** | clip F6C3 effect **60BA** (3P), E9B6 effect **5FA4** (1P) | H |

**Muzzle, 51DA (H):** two sets (C20-C26 one LUT row, C29-C35 a team / enemy pair; L which view). A tiny torus 15A26 at **size 0.036
(12 cm)**, 1.05 m/s forward, life 0.15 s, JUNO_MEDI_MUZZLE / _ENEMY; soft dots 568C **5.5-12 cm** and **0.3-0.36 m**; lens flares 37A79
**0.2 m** and **0.7 m**, life 0.05-0.1 s, **JUNO_MEDI_FLASH** (blue then yellow, HDR 2) / _ENEMY (red then peach); three lights 0.33 /
0.33 / 0.1 for 0.17 s at +0.11 m.

**The round, 53B6 (0.33 s) (H parts, M size):** two quads drawn along the flight with a line strip 3A512 x a length fade 47DD9 and a
float curve 8000A21D, life 0.2 s, an attribute 0.2 (M: the streak's length or width), **JUNO_MEDI_TRACER** (navy, blue, yellow, HDR 5) /
_ENEMY (dark red, orange) and a core **JUNO_MEDI_TRACER_CORE** (olive to gold, HDR 20). juno-map: projectiles 0.02 m (enemy) / 0.22 m
(ally); speed not in the data.

**Ally hit, 58B4 (H):** one ring flash 1CB5E (4 frames) x 4DB noise **2.37-2.55 m**, life 0.19-0.24 s, **JUNO_MEDI_ALLY_HIT** (black to
cyan, HDR 2); a star glow 266F **2-2.5 m**, life 0.22-0.27 s, JUNO_MEDI_ALLY_HIT_GLOW (pale yellow-green); a light 0.5 -> 2.5. (2.4 m
looks big for a 0.22 m round: M the quad's art fills a fraction.)

**World hit, 53B7 (0.5 s) (H):** a decal **0.5 x 0.5 m**, 0.1 deep, random spin, 0.27 s, masks 1CB5E x 266F, **JUNO_MEDI_WORLD** (black to
pale yellow) / _ENEMY (pink); two lights 0.27 -> 1.

**Reload, 60BA / 5FA4 (1.57 s) (H):** the gun piece entity **5ADB** shown 0.17-0.57 s (3P; 1P 0.23-0.87 and 0.33-0.87), sparks on a
2 cm torus 8 a second at 0.88 s rising 0.45 m/s, a short tube flash 15A21 size 0.1 at 0.9 s, a ribbon **0.1 m** wide on hp 0159, 200
points a second, 0.51-1.1 s, life 0.22 s (**JUNO_RELOAD_RIBBON** / _ENEMY), a stretched 8-10 cm sphere at 0.18 s, a light 0.13. 8 sounds.

**Recipe:**
- Each round: a 0.25 m `juno_tracer` STREAK (`juno_tracer_line` x `juno_tracer_fade`) along the shot, JUNO_MEDI_TRACER_N for her team,
  _ENEMY_N for the other; at the muzzle a 0.2 m `juno_lens_flare` + a 6-12 cm `juno_soft_dot`, 0.05-0.1 s (JUNO_MEDI_FLASH_N).
- On an ally: a 2.4 m `juno_hit_ring` flash (frame by age, 0.2 s, JUNO_MEDI_ALLY_HIT_N) and a 2.2 m `juno_star_glow`. Ours: scale both
  by 0.5 for a block-sized player if it reads too big.
- On an enemy: nothing of hers (ours: a 2-tick red `spark`). On blocks: a 0.5 m `juno_hit_ring` decal flat on the face, 0.27 s.
- Reload: hide / show her gun piece per 5ADB's window; a few cyan `spark`s at 0.9 s.

## Pulsar Torpedoes

| Part | Effect | Conf |
|---|---|---|
| **Lock-on marker** on targets | **no effect**: UX element **0727.116** on the target's hp 0001 (4B99 #158), progress v10349 / v1723; 5C17 (lock sound) | H |
| Lock HUD reticle | UX elements of 4695 (00DB / 0237 / 0240 / 0243.116): HUD, not world | H ids, L roles |
| **Pods charging** while locking | **5254** on hp 048D / 048E (4695 #214 / #862), sizes by lock progress (dataflow F6) | H, M input |
| Lock (3P add) | clip 199C9 effect **8A7D** (a glint on hp 04C9) / F0AF effect 70E2 (empty) | H |
| **Launch** | **5AF3** (534F / 54CB `m_A18274A3`) on hp 048D / 048E; 5199 (sounds) per volley | H |
| **Pods' trails at fire** | clip F0B1 effect **62B6** (3P), hp 048D / 048E / 051D / 051E | H |
| **Ally torpedo in flight** | **51E8** (534F -> child 5350), 3 s | H |
| **Enemy torpedo in flight** | **5940** (54CB -> child 5A5B), 3 s | H |
| **Hit an enemy** | **5B0D** (54CB `m_AED73588`) | H |
| **Hit an ally + heal over time** | **5B0C** on the ally's hp 0001 at the start of the heal (4795 #390, onBegin of the HoT wait #376) | H |
| Heal over time loop | **sound only** (56DD held 5 s, or 5A90 when the healer is the local player, #395) | H |
| Ally torpedo hits a wall | **5F45** at the ray-hit point, along the normal (4795 #459 / #460) | H |
| Ally torpedo hits a 003A-class target | **5F46** at the torpedo (#473) | H node, L case |
| Others | 520D / 520F / 5B68 / 56DC / 5F58 / 5C04 / 67AC / 68A8: sounds; 5210: empty | H |

**Pods charging, 5254 (1.67 s, held while locking) (H values, M "progress"):** per pod (two emitters each): a burst glow 37C36 growing
**0 -> 1 m** over the last 25 % of the input (4 a second, life 0.55-0.85 s, **JUNO_POD_GLOW** / _ENEMY); a star flare E221 up to **0.73
m** (3.4 a second, life 0.5); a thin ring 724 up to **0.28 m** in the second half; a lens flare 37A79 up to 0.73 m (6 a second, life
0.23-0.28); sparks 568C up to 20 a second drifting back; a star burst 12F7D up to size 0.12 (0.27 m); two ribbons (2FCA7) on the pods,
180-200 points a second for 0.3 s, life 0.1 (**JUNO_POD_RIBBON** / _ENEMY); four lights 0 -> 0.98.

**Launch, 5AF3 (H):** on each pod: soft dots 568C **0.35 m** and **0.64 m**, lens flares 37A79 **0.65 m** and **1.5 m**, life 0.05-0.1
s (**JUNO_TORP_LAUNCH** blue -> yellow / _ENEMY red -> peach); lights 0.8 / 0.2 for 0.53 s.

**3P fire trails, 62B6 (1.6 s) (H):** two ribbons 0.15 m wide (7A9 x 4C003), 31 points a second, life 0.3 s, from the pods / flaps;
flare sparks 2F4F0 150 a second for 0.63-0.7 s, 0.14-0.58 m, rising 0.1-0.6 m/s.

**Ally torpedo, 51E8 (3 s) (H):** core spheres DBF **0.25 / 0.28 / 0.5 m** (masks 754, **JUNO_TORP_HEAL_CORE** cream), burst glows
37C36 **0.64-0.68 m** 4 a second, life 0.57 s; a nose half-shell 160EA **0.7 m** (two, JUNO_TORP_HEAL_SHELL / _ENEMY); sparks 568C
**150 a second**, 2-2.2 m/s backwards, 0.24-0.41 m, life 0.32; **two trails 0.38 m and 0.76 m wide**, 200 points a second, life 0.3 s,
2FCA7 x 7A9 (**JUNO_TORP_HEAL_RIBBON** gold-white / _ENEMY red); lights 1 -> 3.

**Enemy torpedo, 5940 (3 s) (H):** a sphere 1385 **1 m** (18E48 x 7A9, white-blue); a nose half-shell 160EA **1 m** (JUNO_TORP_DMG_SHELL /
_ENEMY); a trail **1 m wide**, 200 points a second, life 0.3 s, 299E3 x 4C003 x 7A9 (**JUNO_TORP_DMG_RIBBON** navy -> pale blue /
_ENEMY red-orange); torus rings 15A26 size 1 (3.5 m, L) 12 a second shot back 10 m/s, life 0.6; lights 1 -> 3.

**Hit an enemy, 5B0D (H):** two star flares DA5B **2 m** and two lens flares 37A79 **1.5 m** (white -> blue / white -> red), life 0.3 s; a
ring 724 **2.25 m**, life 0.2 s (**JUNO_TORP_DMG_HIT** / _ENEMY); a light.

**Hit an ally, 5B0C (3 s, all at the start) (H):** two flares DA5B **1.5 m**, life 0.4 (**JUNO_TORP_HEAL_HIT** white -> yellow); 18-24
sparks 568C 0.35-0.84 m at 1-1.2 m/s; a big soft flash 29F7 **8 m** for 0.22-0.27 s; two lens flares 1 m; a half-shell 160EA 0.35 m
(JUNO_TORP_HEAL_HIT_SHELL); lights 0.5 -> 2.5. **No visual for the 2.5 s heal over time (H)**: only the held sound 56DD.

**Wall hit, 5F45 (0.67 s) (H):** a star 29F7 0.3-0.8 m growing x1.25, **10 -> 40 sparks** (by dataflow 9BD, L the torpedo's speed),
0.05-0.09 m, a 30E12 swirl x4 stretched x2, 0.3 s (JUNO_TORP_WALL gold-white); lights 1.

**Lock glint, 8A7D (H values, L role):** at 0.03-0.07 s on hp 04C9: 10 tiny sparks (5-12 mm, JUNO_TORP_LOCK_GLINT navy -> cyan) and a
0.8-size bow-tie 11E07 (0.45 x 0.64 m) for 0.5 s.

**Recipe:**
- Lock marker: a HUD element (ours: a cyan ring over each locked hero, filling with progress; a red one on enemies), as 0727.116 does.
- While locking: on her back (the pods, ±0.04 m, 1.35 m up, 0.19 m behind) two `juno_soft_dot` / `lw_star` glows growing 0 -> 0.7 m with
  the lock progress, JUNO_POD_GLOW_N.
- Each launch (one per target every 0.08 s): 0.65 m `juno_lens_flare` + 0.35 m `juno_soft_dot` at the pod, 0.1 s.
- Ally torpedo: a 0.3 m `juno_soft_dot` core (JUNO_TORP_HEAL_CORE), a `Fx.trail` 0.4 m wide of `juno_heal_trail`
  (JUNO_TORP_HEAL_RIBBON_N, life 0.3 s), 2-3 sparks a tick. Enemy torpedo: a 0.5 m core, a 1 m `juno_dmg_trail`
  (JUNO_TORP_DMG_RIBBON_N; enemies see _ENEMY_N).
- Hit an ally: 1.5 m `cat_flare` (JUNO_TORP_HEAL_HIT), 5 sparks, a 2-3 m soft flash 0.25 s (ours: 8 m is too big at block scale); the
  heal over time: no visual (ours: none). Hit an enemy: 2 m `cat_flare` + a 2.25 m `juno_ring_thin` 0.2 s. Wall: a 0.5 m flare and
  10 sparks.

## Hyper Ring

| Part | Effect | Conf |
|---|---|---|
| Cast | 52FE / 5D47 (4857 #1112 / #1205): sounds; clips F01A / F09E / E9BA (628A / 629A / 6297): sounds | H |
| **Launch flash** | **5AE7** (5530 `m_A18274A3`) at hp 0002 (+ lights on 0078 / 007A) | H |
| **In flight** | the ring entity 5530 has no visual of its own besides; **three cosmetic orbs** 5BAE (4857 #1180-#1183, each -> 5BAF -> **5B22**) | H, M role |
| Fly-by | 5F2F: sound | H |
| **The ring (6 s)** | **541C** held under #618 (both branches: v41.v76384 / v78580) in the ring's graph 4858 | H |
| **On each ally passing through** | **56DE** held by StateChaseVar #441 (the 3 s boost) on each ally (#438 targets v375) | H |
| **The ring's end** | **5D44** at #462 (onBegin, on message 0053 when the life ChaseVar #463 ends) | H, M "end" |
| Others | 5A8B / 5D54 / 53CA / 5A5C / 7AD4 / 7AD5: sounds | H |

**The ring, 541C (2 s, held for its life: M loops) (H):** two models **15AD6** drawn with **look 1BABB (team)** and **look 1BC0D
(enemy)**, scale **(25, 5, 25)**: a **9.25 m ring** (4.6 m radius), 0.28 m thick (M axis), facing her aim (M); and the same two at the
start with a pop-in scale curve 0 -> 12.8 over 0.08 s then on (C46 / C47, M). The look's material (shader 0x21, E786 / E8C3): masks
19167 / 1038 noise, **3FE22 / 3FD49 wavy band edges**, **2A907 star field**, 16E9D / 299E3, colour EF1F (pale cyan), LUT row 1
**JUNO_RING** (navy -> blue, HDR 1.78) / **JUNO_RING_ENEMY** (red, HDR 1.78), row 0 a param ramp. Around it: ring sprites 15B77 at **size
25.5** (3 a second, life 1.5 s; and 10 a second, life 0.75, a ring-segment 1640C), **JUNO_RING_SPARK** / _ENEMY; a **disc flash 5E5E
size 16** (16 m, 0.3 s, at the start, 190EC x 299DF, **JUNO_RING_DISC** / _ENEMY); a torus glow 15A26 size 0.6 (2 m) at the start
(**JUNO_RING_GLOW**) and one 2.3 (8 m) at 0.17 s lasting **8 s**; sparkles 3C068 (4 frames) **20-50 a second**, 3-5 m/s up, 0.21-0.26 m,
stretched x3, life 0.3-0.7; flares 2F4F0 / 17E1 / 2E8AF 0.7-2 m at the start; two lights 5.

**Launch, 5AE7 (H):** at the hand: soft flashes 37A79 / 568C **0.2 m / 1 m** life 0.05-0.1 (**JUNO_RING_LAUNCH** mint), rings 2F3EC /
17E1 / 2E8AF **1 m** (cyan) and **3 m** (violet-peach) for 0.15 s; six lights 0.22.

**Flying orbs, 5B22 (3 s each, three of them) (H values, M role):** a sphere DBF **1 m** (2BE4A, teal / pink rows), a trail **0.7 m**
wide, 200 points a second, life 0.2 s (299E3 x 1344, **JUNO_RING_ORB_RIBBON** / _ENEMY), sparks 2F4F0 up to 150 a second, 0.14-0.91 m,
life 0.1-0.4; a light 1 -> 3.

**On a boosted ally, 56DE (held 3 s) (H node, M look):** speed lines E99F (3 frames) **300 a second** + 200 at once, **2-5 cm**, 4 m/s,
life 0.27-0.37 s, **JUNO_RING_BOOST** (yellow -> green, HDR 25); and 50 a second (+30) at size 10, 4-5 m/s, life 0.22-0.32 (L: screen-size
lines for the boosted player).

**End, 5D44 (1.67 s) (H):** at 0.37 s two **disc flashes 5E5E size 18** (18 m, crossed ±90°, 0.3 s, JUNO_RING_DISC rows); 0-0.3 s
**streaks 49944 300 a second**, 0.26-0.58 m, pulled inward 2.4-3 m/s, stretched x3-5.5 (**JUNO_RING_END** / _ENEMY); at 0.6-0.8 s a
sphere flash D721 **3.2 m** (0.15 s), flares 2F4F0 **4-5 m** / A7B6 4 m (0.12 s), 25 sparks 0.3-0.6 m, flow spheres 85B 3-4 m.

**Recipe:**
- Throw (0.25 s into the 0.55 s cast): at her right hand a 1 m `juno_lens_flare` and a 1 m `lw_ring` flash, 0.15 s.
- Flight (35 m/s, 0.5 s): the ring model already at full size, plus 3 `juno_soft_dot` orbs (1 m) with 0.7 m `Fx.trail`s.
- Held (6 s): `juno_ring` (glb) at 9.25 m (it is 0.37 m: scale 25, 5, 25), facing her aim, masked `juno_ring_band` x `cat_glow_noise`
  scrolling, additive, JUNO_RING_N for her team / JUNO_RING_ENEMY_N for enemies; a few `juno_sparkle` rising off it (frame by age,
  3-5 m/s, 0.5 s). At its spawn a 16 m `juno_disc` flash (0.3 s, JUNO_RING_DISC_N, masked `juno_disc_band`).
- An ally through it: for the 3 s boost, 4-6 tiny `cat_speed_line`s a tick streaming past them (JUNO_RING_BOOST_N).
- End: an 18 m `juno_disc` flash at 0.37 s, inward `juno_streak_v`s for 0.3 s, a 3 m `juno_sphere` pop at 0.6-0.8 s.

## Orbital Ray

| Part | Effect | Conf |
|---|---|---|
| **Cast on her hand** | **5CCA** (4A44 #848 under StateWait #430, 0.8 s cast), hp 007B / 007E / 0080 | H |
| Cast (3P clip) | clips F67A / F153 effect **61AD**: a ribbon on hp 0080 at 0.5-0.7 s + lights; 1P EA1C **602F**: a light | H |
| Cast sounds | 54F8; 5658: empty | H |
| **The ray (landing, column, ground circle) for 8 s** | **54F9** held under StateTrackRay #84 (follows the ground, 40 m down from the ray's height), at hp 01DA | H |
| Hold / last seconds | 54F7 (held), 5527, 5AD2 (#22 at v29029 > 2.7): sounds | H |
| **Allies inside (heal and +30 % damage)** | **3169** held on each ally by 54A6 #297 (the per-target graph, while v65643 > 0) + 5D45 / 5E86 (sounds) | H |
| End | **no effect of its own**: the held 54F9 stops with #84 | H |
| Stadium | 7E01 (rain + red decals) / 7AFA (#406 / #397 on power 2AFA.134) | H |

**Cast, 5CCA (2 s; bursts at 0-0.47 s) (H):** on her hand: small cone EEB **size 0.06 / 0.09 (0.17-0.26 m long)** for 0.77 s
(**JUNO_RAY_CAST** blue -> cyan / _ENEMY orange, HDR 10); curved blades 14063 / D06 **0.07-0.18 (8-20 cm)** at 0.1-0.47 s (2A413 x 42955,
violet -> light blue); flares 2F4F0 / 4C09 **3-20 cm**; 50 tiny sparks a second 0.17-0.6 s. **3P ribbon 61AD:** 0.1 m wide, 10000 points
a second for 0.2 s from 0.5 s, life 0.1 (12201 x 4F3 x 2B469, **JUNO_RAY_CAST_RIBBON** / _ENEMY); lights 0.39.

**The ray, 54F9 (2 s, held: M loops for the 8 s) (H values):** at the ground point:
- **Ground circle decal** (C32) **8.5 x 8.5 m half-extents (17 m across)**, 4 m deep, masks **4DBCC (the circle edge) x 16E7A**,
  **JUNO_RAY_DECAL** (gold, pale blue-white) / _ENEMY (red-peach); a second decal (C31) growing **0.5 -> 8** (1038 / 29F7 / 4F8EB / 16E7A,
  2 deep, gold / blue tint rows).
- **Landing shockwave decal** (C41) growing **0 -> 3.5 -> 7 m** over 1.2 s, 7.5 deep, mask 139C (**JUNO_RAY_SHOCK** / _ENEMY).
- **Descending rings**: torus 15A26 at **size 8** (27.7 m across, L the visible band is narrower) at **+196, +150, +100, +50 m**, one each
  at 0, 0.07, 0.16, 0.26 s, life 1 s (754 x 1C7A); then rings size 1.5 (5.2 m) at +150 m, 4 a second, **falling 28 m/s**, life 5 s
  (**JUNO_RAY_RINGS** / _ENEMY).
- **The column**: tube 15A21 at **size 6** (5.4 m across, 34 m long) at **+250 m** (18E48 x 31951, **JUNO_RAY_COLUMN** pale blue -> yellow
  -> white) and at **+200 m** (4D4 x 4DBCD, **JUNO_RAY_COLUMN_EDGE** teal); a flare DA5B at +225 m.
- **Falling light** 49944 at +70 m, **200 + 150 a second**, falling **40-45 m/s**, **4-7 m** long, life 1-2 s (**JUNO_RAY_RAIN** /
  _ENEMY).
- On the ground: a glow quad 801 **size 3** (C495 x 4DB x 4C003, **JUNO_RAY_GROUND**), a low wall 1F85 size 1 (3 a second, life 3 s,
  38E1D x 31951, **JUNO_RAY_WALL** / _ENEMY), a torus size 1.5 at +0.05 m (3 a second, life 2), a thin torus EBEF 1 m at 0.97 s; two
  lights **10**.

**An ally in the ray, 3169 (1 s, held while inside) (H):** three **material effects** on the ally's body (mask 19167, **JUNO_RAY_ALLY**
blue HDR 5 and **JUNO_RAY_ALLY_GOLD** yellow rows); rising chevrons DFDC **0.75-1 m**, 8 a second, life 0.75-1.25, stretched x3-3.5
(27D0D x 27D0A, **JUNO_RAY_ALLY_RISE**, `ECIM(40)`: the ally's own view, M); their screen rim DAE1 (212A6 x 2042B x 2103C, `ECIM(40)`) 2 s.
One look for heal and damage boost: the +30 % is the same status (54A6 #299).

**Recipe:**
- Cast (0.8 s): a 0.2 m `juno_cone` / `juno_cast_glow` flash on her right hand (JUNO_RAY_CAST_N), `spark`s; 3P: a 0.1 m swipe ribbon.
- Landing (0.5 s in, 4 m ahead): four 8 m `cat_torus` rings dropping from 50-200 m up over 0.3 s, and a ground ring decal growing 0 -> 7 m
  over 1.2 s (`juno_ray_circle` flat, JUNO_RAY_SHOCK_N).
- For 8 s, moving at 2.25 m/s: a flat 17 m `juno_ray_circle` (x `juno_ray_disc`) on the ground (KirikoFx.groundY), additive,
  JUNO_RAY_DECAL_N / _ENEMY_N; a `juno_column` (or a 5.4 m wide STREAK) from the ground to the build limit with `juno_ray_column`
  scrolling down (JUNO_RAY_COLUMN_N); 2-3 `juno_streak_v`s a tick falling 40 m/s inside it (JUNO_RAY_RAIN_N); 5 m rings falling 28 m/s
  every 0.25 s.
- Allies inside: a blue body tint (the material effect: Sombra's body flash overlay, JUNO_RAY_ALLY) and 1 `juno_chevron_glow` a tick
  rising 1 m; their HUD: a blue rim (ours).
- End: stop drawing (no end effect).

## Martian Overboots (double jump, hover, the boots)

| Part | Effect | Conf |
|---|---|---|
| **Boots (always)** | **5985** on hp 001A / 0019 (±0.01 m), 4441 #339 / #340 under the graph entry's #344 (view flags v478 / v25461 / v10467: M) | H node, M when |
| **Double jump** | 53EB (#274 at the action #570): sounds; **clip effects** EE48 / F432.. **5F7B** (0.27 s) and the fall loop EE75 / F433.. **5FA9** (1 s) -> sub-effect **5C36** on hp 002C / 002E | H |
| 1P double jump | EB92 effect 6290: empty | H |
| **Hover** | **5D10** on hp 001C / 001B (sub-points 002C / 002E) under the hover node #382 (fall cap 3 m/s), #380 false branch (v545: L "not gliding") | H |
| Hover sounds | 5C0F (start), 5C10 (end, #383), 5D29 (held), 59EC / 5D31 / 5D4F / 5D60 | H |
| Jump / land / glide clips | 5F79 / 6271-6278 / 628D / 62A3 / 628E / 6279 / 626C: camera shakes + sounds | H |

**Boots, 5985 (1 s loop) (H):** a glow 2F4F0 **0.37 m** at (0, -0.08, +0.05), 12 a second, life 0.2-0.25 (**JUNO_BOOT_JET** navy -> teal ->
white, HDR 8); a ribbon **0.15 m** wide, 200 points a second, life 0.2 s (2FCA7 x 7A9, **JUNO_BOOT_RIBBON** pale blue -> navy, falls); a
light 0.42.

**Double jump, 5C36 (0.5 s, from the take-off and again in the fall loop) (H):** per foot jet (002C / 002E): a glow 1603 **0.15 m** 12 a
second, two flame cones E21 x 2D278 **1 m** long, 1 m/s, life 0.2-0.25 (**JUNO_BOOT_FLAME** blue -> white; **JUNO_BOOT_FLAME_VIOLET**);
lights 0.15 / 0.66.

**Hover, 5D10 (1 s loop) (H):** per jet: glow 1603 **0.15 m** 12 a second; flame cones **0.78 m**, 1 m/s down (JUNO_BOOT_FLAME_VIOLET);
sparks 1F35 **0.05-0.22 m**, **20 a second**, 1.1-3.1 m/s, life ~0.1 (**JUNO_BOOT_SPARK** cyan); under her a 17 cm torus 15A26 at -0.1 m,
5 a second, sinking 0.29 m/s, life 0.56 (**JUNO_HOVER_RING** / _ENEMY); lights 0.17 x2 + 0.6.

**Recipe:**
- Always (hero mode): at each boot a 0.35 m `juno_jet_glow` (JUNO_BOOT_JET_N, additive, flicker 12/s) and a thin `Fx.trail` 0.15 m of
  `lw_wisp` (life 0.2 s).
- Double jump: for 0.27 s (and while rising after it) two 1 m `lw_beam_cone` flames straight down from the feet (JUNO_BOOT_FLAME_N) with
  a 0.15 m glow.
- Hover (JUMP held, falling capped at 3 m/s): two 0.8 m flames down, 1 cyan `spark` a tick from each foot, a 17 cm `cat_torus` ring
  sinking under her every 0.2 s (JUNO_HOVER_RING_N).

## Glide Boost

| Part | Effect | Conf |
|---|---|---|
| Start | 0186 (fail), 59F5 (#527), 7523-7528: sounds; start clips: none | H |
| **Jets while gliding** | **5A40** on hp 002C / 002E (aim hp 0001) from 0.17 s after the start anim (#687 -> #686, while !v699) | H |
| **Speed lines** | **5963** (#680, same window): 3P lines + a 1P screen burst | H |
| Cancel available | 5843 (#545 at v215 >= 0.4 s): sound | H |
| End | 752C (#925 / #928): sounds | H |
| Glide trail | **none** (no ribbon in 5A40 / 5963; glide clips A12D-A131 empty) | H |
| Double jump in glide | clips F105 / F0D1 / ED2C (6292 / 6294 / 6298): empty or sound | H |
| Melee in the air | F087 effect 6288: empty | H |

**Jets, 5A40 (0.5 s loop) (H):** per jet: glow 1603 **0.3 m** 12 a second; flame cones E21 x 2D278 **1.0 m** and **1.4 m**, 12 a second,
1 m/s back, life 0.2-0.25 (JUNO_BOOT_FLAME / _VIOLET); a 17 cm torus 15A26 5 a second rising 1 m/s, life 0.56 (**JUNO_GLIDE_RING** /
_ENEMY); sparks 1F35 **50 a second**, 2.7-4.7 m/s, 0.05-0.22 m, life ~0.1 (JUNO_BOOT_SPARK); lights 0.2 / 1.0.

**Speed lines, 5963 (1 s loop) (H values, M view):** E99F lines **size 10**, 50 a second + 30 at once, 4-5 m/s, life 0.2-0.3 (yellow-green
param rows: L colour); a hex disc 5708 **size 2** once, life 0.5 s, masks 18F00 / 116E hex / 1484C / 19325, `ECIM(40)` (**the glider's
screen**: **JUNO_GLIDE_SCREEN** blue -> white).

**Recipe:**
- Gliding (up to 4 s): from each foot a 1-1.4 m `lw_beam_cone` flame streaming back (JUNO_BOOT_FLAME_N), a 0.3 m `juno_jet_glow`, 2 cyan
  `spark`s a tick shot back 3-4 m/s; no trail ribbon.
- Her own screen at the start: a blue hex flash (`brigitte_barrier_hex` x JUNO_GLIDE_SCREEN, 0.5 s) and `cat_speed_line`s past the edges.

## Palettes (EffectArt, `research\juno\work\fx_palettes.txt`)

83 lines, each `JUNO_<NAME>(stop, r, g, b, ...)` with HDR rows also given `_N` (normalised by the peak). **Every palette starts at exact
black at 0** (rows that start coloured get black at 0 and the game's values at .25 / .5 / .75 / 1; rows that start near black are forced
to black). Names: MEDI_MUZZLE / _FLASH / _TRACER (+ _ENEMY), MEDI_TRACER_CORE, MEDI_ALLY_HIT / _GLOW, MEDI_WORLD (+ _ENEMY),
RELOAD_SPARK / _RIBBON (+ _ENEMY); POD_GLOW / POD_RIBBON / POD_BURST, TORP_LAUNCH, TORP_HEAL_RIBBON / _SHELL, TORP_DMG_RIBBON / _SHELL /
_HIT (+ _ENEMY), TORP_HEAL_CORE, TORP_HEAL_HIT / _HIT_SHELL, TORP_WALL, TORP_LOCK_GLINT; RING / RING_SPARK / RING_DISC / RING_GLOW /
RING_ORB_RIBBON / RING_END (+ _ENEMY), RING_LAUNCH, RING_BOOST; RAY_DECAL / RAY_SHOCK / RAY_COLUMN / RAY_RINGS / RAY_GROUND / RAY_RAIN /
RAY_WALL / RAY_CAST / RAY_CAST_RIBBON (+ _ENEMY), RAY_COLUMN_EDGE, RAY_ALLY / _GOLD / _RISE; BOOT_JET, BOOT_FLAME / _VIOLET, BOOT_SPARK,
BOOT_RIBBON, HOVER_RING / GLIDE_RING (+ _ENEMY), GLIDE_SCREEN. Each line's comment gives the effect and component it came from and the
game's hex at 0 / .25 / .5 / .75 / 1 (trust those over the short descriptions). The script: `fx_juno_palettes.py`.

## In the pack

Built 2026-11-01 into the dev pack (`OC_ASSET_PACK=E:/claude/mods/overcraft_ana/local-assets/pack py tools/build_asset_pack.py effects
"E:\Overwatch Archive\research\juno\fx"`), entries in `tools/build_asset_pack.py` (EFFECT_MASKS / PRODUCTS / EFFECT_MODELS, `juno_*`).
All present: 52 files in `E:\claude\mods\overcraft_ana\local-assets\pack\assets\overcraft\textures\effect\`, 10 models in
`...\assets\overcraft\hero_models\`.

- **Masks** (`textures/effect/`; flipbooks `_<frame>`): Mediblaster `juno_soft_dot` (568C), `juno_lens_flare` (37A79), `juno_marble` (754),
  `juno_hit_ring` (1CB5E, 4), `juno_star_glow` (266F), `juno_tracer_line` (3A512), `juno_tracer_fade` (47DD9), `juno_beam_fade` (48E96),
  `juno_ribbon_edge` (4C003), `juno_cloud_noise` (299E9), `juno_noise_soft` (2BE4A); torpedoes `juno_ring_thin` (724), `juno_lock_wisps`
  (DDE5), `juno_streak_band` (16D78), `juno_drip_edge` (4C002); Hyper Ring `juno_ring_edge` (3FE22), `juno_ring_band` (3FD49), `juno_stars`
  (2A907), `juno_ring_noise` (1038), `juno_disc_band` (190EC), `juno_disc_noise` (299DF), `juno_streak_v` (49944), `juno_sparkle` (3C068,
  4), `juno_ring_soft` (2F3EC), `juno_noise_c` (5DF); Orbital Ray `juno_ray_circle` (4DBCC), `juno_ray_disc` (16E7A), `juno_ray_ground`
  (C495), `juno_ray_column` (31951), `juno_ray_column_edge` (4DBCD), `juno_ray_wall_fade` (38E1D), `juno_ray_corner` (4F8EB),
  `juno_cast_ribbon` (12201), `juno_cast_glow` (42955), `juno_cast_streak` (1C4F), `juno_cast_fade` (D9B), `juno_star_field` (3C723),
  `juno_chevron_glow` (27D0D), `juno_chevron_glow_b` (27D0A), `juno_screen_rim` (212A6), `juno_screen_noise` (2103C); boots / glide
  `juno_jet_glow` (1603), `juno_fade_thin` (19325).
- **Products:** `juno_tracer` (3A512 x 47DD9), `juno_heal_trail` (2FCA7 x 7A9 = `lw_wisp` x `cat_fade`), `juno_dmg_trail` (299E3 x 4C003 x
  7A9 = `cat_ring_line` x `juno_ribbon_edge` x `cat_fade`).
- **Shared masks already in the pack** her recipes use: `glow` (A7B6), `spark` (1F35), `cat_soft` (4C09), `cat_flare` (DA5B), `cat_fade`
  (7A9), `cat_band` (1C7A), `cat_glow_noise` (19167), `cat_strip` (1344), `cat_cone_line` (16E9D), `cat_ring_line` (299E3), `cat_cloud`
  (18E48), `cat_speed_line` (E99F), `cat_streak` (183A7), `cat_cone_noise` (2C41F), `mercy_noise` (4DB), `mercy_noise_b` (4A7E),
  `mercy_soft` (139C), `lw_wisp` (2FCA7), `lw_ring` (2E8AF), `lw_star` (E221), `lw_burst` (37C36), `lw_swirl` (30E12), `lw_beam_cone`
  (2D278), `lw_crystal` (2A413), `band` (2FF91), `lucio_spark` (29F7), `lucio_soft` (4F3), `brigitte_soft_blob` (18F00),
  `brigitte_flare_star` (2F4F0), `brigitte_noise_fine` (4D4), `brigitte_barrier_hex` (116E), `brigitte_whip_flash` (153AF),
  `sombra_line_thin` (1484C), `sombra_ring_burst` (17E1).
- **Models** (`hero_models/`): `juno_ring` (15AD6, with its look E786's textures), `juno_column` (15A21), `juno_ray_wall` (1F85),
  `juno_cone` (EEB), `juno_half_shell` (160EA), `juno_pod_burst` (12F7D), `juno_sphere` (D721), `juno_torus_thin` (EBEF), `juno_disc`
  (5E5E), `juno_cast_blade` (14063); the fat torus 15A26 is `cat_torus`.

## Not found

- **The lock-on marker's art**: it is UX element 0727.116 (HUD), not decoded; the 4695 / 47B7 / 4A44 UX elements (00DB / 0237 / 0240 /
  0243 / 01EE.116) likewise (L: the reticle, cooldown and ult HUD).
- **A heal-over-time visual on torpedoed allies**: only the hit flash 5B0C and a held sound (56DD) (H by search of 4795).
- **A glide trail**: none in 5A40 / 5963 or the glide clips (H).
- **The hero hit of the Mediblaster**: 5841 is empty (H); creatures show the shared damage response (not traced).
- **The ray's end**: no effect; held 54F9 stops (H). Whether 54F9 (2 s) loops over the 8 s or replays (M loops, as Brigitte's 219D).
- **Which view each Mediblaster muzzle set is** (one-row C20-C26 vs paired C29-C35: L 1P / 3P).
- **Size 10 speed lines** in 5963 / 56DE: L screen-space (no `ECIM(40)` on them).
- **v545 / v478 / v699 meanings** (the hover jets' and boots' gates, the glide jets' stop): M / L.
- **5F46's case** (target class 003A.06C: L a barrier or deployable).
- **Hardpoints 0002 / 0078 / 007A / 04C9** places (not in her model's table or not measured).

## Disagreements with juno.md

- **Hyper Ring radius**: juno.md says **4 m (8 m across)** (wiki, M). The data draws the ring at **9.25 m across (4.6 m radius)**
  (15AD6 0.37 m x scale 25, H) and its sprites at 9.4 m; juno-map's 4.5 m agrees with the data.
- **Orbital Ray**: juno.md "the beam lands about 4 m ahead after ~1 s"; the effects land it with the cast (cast effects 0-0.47 s, the
  landing rings from the ray's start; juno-map: 0.5 s). The circle decal is **8.5 m** half-extent (17 m across) for the 8 m radius (H).
- The torpedo / ray / ring numbers otherwise aren't carried by effects (no disagreement found).
