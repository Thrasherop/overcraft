# Sigma's ability effects in the game data

What Sigma's effects are made of in real Overwatch, where the extracted files are, and how each can be rebuilt in Minecraft
(`SigmaFx`, to write). Read 2026-11-02 from the archived offline build 154088. Overwatch and Battle.net were checked closed
(`Get-Process Overwatch, Battle.net`) before every TankLib run, and `owinit.py` refuses on its own; everything was read only,
offline, through TankLib/DataTool. Text only in the repo: the masks, ramps and meshes stay in the owner's archive and reach the
game through the local asset pack. The format is [sojourn-effects.md](sojourn-effects.md)'s (method:
[juno-effects.md](juno-effects.md)). His data map: [sigma-map.md](sigma-map.md); plan: [sigma-plan.md](sigma-plan.md); hub:
[sigma](../../techniques/heroes/sigma.md); rendering lessons: [rendering](../../techniques/rendering/rendering.md) "Effects and glows".

Confidence: **H** read in the data, **M** inferred, **L** guess. "Size" is the spawn size (a 1 m quad at size 0.5 is 0.5 m; a
mesh is its bounds times the size; a ribbon's size is its width). "Ours" marks what no data gives. Palettes: `SIG_*` (their stops
in `research\sigma\work\fx_palettes_sigma.txt`, Java-ready for a `SigmaPalettes` class), each a LUT row read from `ramps\`;
masks: `sigma_*` and reused ones (end). Node refs are `graph #uid` (sigma-map's). "Team / enemy": nearly every renderer comes as
a pair, a blue / lavender / white row and a red one (H rows, M roles: Overwatch's friend-blue / foe-red, as Juno's and Sojourn's
passes read it); `_ENEMY` palettes are the red ones.

## Files and scripts
- `E:\Overwatch Archive\research\sigma\fx\Sigma\<Ability>\` (`Hyperspheres`, `HandSpheres`, `Barrier`, `KineticGrasp`,
  `Accretion`, `GraviticFlux`, `Perks`): `textures\` (PNG masks, multi-frame as `_1.._n`), `ramps\` (float LUTs, `[row][x][rgba]`),
  `models\Models\` (effect meshes; the barrier C015 + look DD58 and the orb D7C9 + EBC9 in `Barrier`, the spheres D7DD + EBE7 in
  `HandSpheres`, the rock C5CA + E20A in `Accretion`), `manifest.json`.
- `research\sigma\work\`: `fx_cards.txt` (one card per renderer of the 39 drawn effects: what this note was written from),
  `fx_overview.txt` (one line per effect), `fx_digest.txt` (full LUT ids, every row), `fx_summary2.txt|json`, `fx_meshes2.json`,
  `fx_entities.txt` (his projectiles' and entities' effect slots and children), `fx_graph_effects.json` and `fx_nodes_out.txt`
  (every node that names an effect / entity, with what leads into it), `fx_clip_effects.txt` (158 clips with an animation
  effect), `fx_matdump.txt` (materials 7C75 and 7C98: shader, textures by type key, LUT rows), `fx_palettes_sigma.txt`,
  `fx_effects\<id>.json|txt` (decoded effects).
- Scripts `research\sigma\scripts\fx\`: Sojourn's `fx_sojourn_*` copied as `fx_sigma_*` (paths retargeted to `research\sigma`,
  `fx\Sigma`; history lines kept), `run.py`, `owinit.py`, `fx_effx.py`; `fx_sigma_extract.py` holds his effect list,
  `fx_sigma_graph.py` / `fx_sigma_nodes.py` his graph labels and list, `fx_sigma_palettes.py` his palettes (rewritten: Java
  output). Run: `run.py fx_sigma_graph.py work\fx_graph_effects.json`, `run.py fx_sigma_nodes.py work\fx_nodes_out.txt`,
  `run.py fx_sigma_ents.py work\fx_entities.txt 2D35 32B4 31A7 2D25 30EB 6866 18B7 18BF 324F 33E8 31A2`, `run.py
  fx_sigma_clipfx.py`; then under the kiriko-anim venv's Python with `-I`: `fx_sigma_extract.py` (all seven folders),
  `fx_sigma_matdump.py work\fx_matdump.txt 7C75 7C98`, `fx_sigma_lut.py Barrier 800567241D0F.004 ...` and `fx_sigma_lut.py
  HandSpheres 8000DFB0EAFE.004`; then `run.py fx_sigma_summary2.py`, `fx_sigma_digest.py`, `fx_sigma_overview.py >
  work\fx_overview.txt`, `fx_sigma_card.py <ids> > work\fx_cards.txt`, `fx_sigma_palettes.py`. The material masks of 7C75 / 7C98
  were copied from the skin extraction (`extract\...\Models\00000000C015.00C\Textures`, `0000000106B4.00C\Textures`).
- Counts: **135 effects extracted (the list's 131 + 4 sub-effects: 4010, 2D5B, 4046, 2EB5), none missing** (TankLib found every
  one, the Stadium ones included). 39 of them draw something; the rest are sounds only or empty.

## His look
- **Dark navy / indigo / lavender to white for his team, dark red to white for enemies (H rows, M roles).** His LUT rows rise
  from black through navy (#2D2D5F, #0002DE) or lavender (#7D7EBC) to white, often HDR 3-10; dark "anti-glow" rows (peak 0.1-0.25:
  the burst's streaks, the sphere's motes) sit beside them. Cyan / sky blue only on the barrier (its material) and Grasp's field.
- **Hardpoints (H ids, M roles):** spheres model 007C (each fire clip's glow; the hand spheres sit on his hp 000A Weapon_R);
  his 000B Weapon_L (3P) / 0002 (1P) for the barrier's hand effects, 0004 (3P) / 0002 (1P) for Grasp; effect-internal 0079
  (barrier send / recall, L: the left palm), 0313 (Grasp's orb, L: in front of the left palm), 01B6 bone_097F (Grasp's dish,
  Accretion's gather: 1.34 m up, in front of his chest), 0076 Head (Flux's body glow), 0001 a target's / his root (overhealth,
  lifted targets, Levitation); the barrier's 0029 / 0032 / 002A / 0033 (its four corners) and 01DA (the heal).
- Node hardpoint fields: `m_9E54CED9` is the 3P point, `m_E591AF01` the 1P one (1B55 #816: 1P null and 3P 02FD while held) (H).

## Hyperspheres (187F; projectiles 2D35 / 32B4, each with child 311A)
| Part | Effect | Rebuilt |
|---|---|---|
| Per volley | **2CE0** (187F #207; 6C27 instead with the Stadium power 25F9, #453): **sounds only** (4) (H) | - |
| Launch | **2BE1** (both spheres' `m_A18274A3`, hp 007C): a swirl 20126 **0.18-0.49 m stretched x11-13**, 0.18 s (`SIG_SPHERE_LAUNCH`, one row); a cone CA08 (2 x 2 x 0.71 m) **0.33-0.82**, 0.3 s, 200E4 x 1C7A (`SIG_SPHERE_LAUNCH_RING` / `_ENEMY`); lights 1 / 3 for 0.27-0.77 s; sub-effect 4010 (sound) | a stretched `sigma_swirl` along the shot, a 0.6 m `sigma_wisp` glow (the cone as a flat glow, ours); skip in his own view for the first 2 m (rendering.md) |
| Flight | **2BDE** (child 311A's effect, 10 s, follows the sphere): the **sphere body 85B (1 m) at size 0.22 = 0.22 m, 10/s, 0.15 s**, masks 5DF x 18E48 (`SIG_SPHERE_CORE` / `_ENEMY`, HDR 7); a **wide ribbon 1.0 m, 60/s, 0.25 s** (`SIG_SPHERE_TRAIL`, falls) and a **thin one 0.25 m, 0.32 s** (`SIG_SPHERE_TRAIL_THIN`, falls), both on the solid 1298 (their shape is the ribbon ramp across); a halo 4C09 **0.32 m, 5/s + 3, 0.34 s** (`SIG_SPHERE_HALO`); **dark motes** 1F35 0.16 m at 10-16/s, 3 m/s, 0.33 s (`SIG_SPHERE_MOTE`, peak 0.23); light 3 | the 0.22 m sphere `cat_bubble` (DBF) + a 0.32 m `cat_soft` halo each frame; `Fx.trail` 1.0 m (0.25 s) and 0.25 m (0.32 s) with a soft tube band (not 1298: solid); motes blended (`Fx.soft`, dark) |
| Burst (on a hero or the world, or after 0.43 s) | **2EAB** (both spheres' `m_6CB564CC`; the hit slot `m_AED73588` = 2E4B / 2E4A is **sounds only**, and there is no separate world-hit slot: the burst is the only hit look) (H): a **sphere DAB5 at 4.5 m**, 0.32-0.37 s, 200E4 x 1C7A (`SIG_BURST_SHELL`); a **flash 208CC x 7AB 8-9 m**, 0.25-0.3 s (`SIG_BURST_FLASH`); a **disc 11C1 (1.28 m) x 4.4-5.4**, 0.3-0.39 s, mask 1FDB2 (`SIG_BURST_RING`, HDR 10); **dark streaks** 19167 x 1FBCE 0.27-0.57 m stretched x20, 3+2 then 333/s falling to 0 over 0.1 s, 0.35-0.4 s (`SIG_BURST_STREAK`, peak 0.12); a refraction sphere 85B (1 m, BC5 normal 18F81, 0.2 s at 0.13 s); lights up to 5.8-6 | the 4.5 m shell (`cat_sphere` / DAB5) at low alpha (0.06-0.1, as Sojourn's field: ours), a 3 m `sigma_star` flash (the 8-9 m at 0.1 alpha, ours), the disc as a flat `sigma_lens` glow 2.5 m (ours), 6 dark streaks blended; **no refraction** (not drawable) |
| Spawn | **2911** / **2D0D** (`m_BAB9824B`): sounds only (H) | - |
| On the hand spheres per shot | the spheres' fire clips (714F / 6F78 / 70F7 / 7115) play **2D5B** on hp 007C (H): at 0.6 s a core DBF **0.085 m**, 0.35 s (`SIG_HAND_CORE`, HDR 5) and a **glow 174D8 0.68-0.83 m, 0.68 s** (`SIG_HAND_GLOW`); from 0.9 s sparkles B08 x 1FC54 **0.24-0.37 m, 7/s, 0.23 s** (`SIG_HAND_SPARK` / `_ENEMY`) and twinkles 1FBCE 0.11 m (`SIG_HAND_TWINKLE`), a bowtie 205F4 0.1 m; light 1 (M: the sphere re-forming in his hand) | at 0.6 s into each fire anim a 0.7 m `brigitte_soft_blob_b` glow and a 0.09 m core at each hand sphere, then a few 0.3 m sparkles until the next shot; not in his own view's hand pass (rendering.md: hidden) |
| 3P / 1P fire clips | 30F7 / 3106 / 3107, 324C-324E: sounds only (H) | - |

## The hand spheres' real look (31A7, model D7DD / skin 106B4)
- **Two spheres per model** (submeshes 0 / 1 at x ±0.04-0.16, **0.116 m cages**, key 532BFF0DAD51B722) **with a 0.052 m core
  inside each** (submeshes 2 / 3, key 6B24EE2D2B083AFE in the skin, 3060878AEBDEB872 in the base model) (H, `fx_sigma_submeshes`).
  The cages use his body material **ADC9** (colour textures: they draw today). **Material 7C98 is the cores'**, not the cages'
  (correction to [sigma](../../techniques/heroes/sigma.md), which put it on the cages) (H: model look 132E3 / EBE7 key -> material).
- **7C98 (H, `fx_matdump.txt`):** shader group B87768E79B35, masks **19167** (cloud noise 512), **18F00** (cloud noise 256), **5DF**
  (caustic web), LUT **8000DFB0EAFE** 256 x 2: row 0 parameters (R 0 -> 1, A a bell 0.06 -> 0.83 -> 0), **row 1 the colour: black,
  plum (0.03, 0, 0.002), violet (0.09, 0, 0.14), blue (0, 0.1, 0.49), with A rising 0 -> 1.4 -> 3.4 -> 1.35** (M: the emission
  strength; RGB x A peaks at (0.1, 0, 1.56) = vivid blue-violet near 0.88). Static inputs all 0; header tint 0.5 / 0.5 / 0.5 (L).
  So the cores are **a dark, swirling violet-to-blue energy (noise x caustics), brightest blue at their hottest** (M: the shader
  scrolls the noise through row 1). No effect makes them glow: their children's effects 2860 / 2B1E are **empty** (0 components,
  shared with Sojourn's gun children), and nothing else names 31A7 (H).
- Rebuilt: an additive glow at each core (two per hand model: the cage centres ±0.1 m on X from hp 000A), **0.05 m core + a
  0.12-0.15 m soft halo (ours)**, palette `SIG_HAND_MATERIAL` driven by a slow flicker (or `SIG_HAND_GLOW`'s brighter row,
  ours), masks `cat_glow_noise` (19167) / `juno_noise_c` (5DF) if a mesh is drawn. Plus the per-shot 2D5B glow above.

## Experimental Barrier (1B55 on him; pet 2D25 / graph 1A2A; model C015, material 7C75; emitter orbs 30EB / FC88)
| Part | Effect | Rebuilt |
|---|---|---|
| **The field itself** | **material 7C75 on C015** (one submesh, 6.69 x 4.17 x 0.73 m, **UV 0-1 over the whole barrier**) (H): shader EB04637DEAF0 with **1FE0A a soft rounded-rectangle rim** (bright border, dark middle), **18F9A a hex seam line** (Y junction), **18F9B hexagon cells of varying grey** (`cat_hex`), **548D1 a shatter / crack map** (BC5, red edges, green pieces), and **LUT 800567241D0F**: row 2 **blue (0.07, 0.34, 1.38) -> sky blue (0.31, 2.17, 3.4) -> white 6** (`SIG_BARRIER`), row 1 **dark red -> red 7 -> orange -> white 8** (`SIG_BARRIER_ENEMY`, M), row 0 parameters (G 1 -> 0 by 0.4, A -1 -> 1: L a fresnel / edge curve). Static inputs 0; header tint 0.73 x3 (L). The pet's own effect **2C8E** (child 31F1, always on) only adds **motes at the four corners** 0029 / 0032 / 002A / 0033: 4DB x 1FC54 **0.65-0.75 m, 2/s each, 0.66-1.3 s** (`SIG_BARRIER_MOTE`; a second set, `_ENEMY`) and two lights 6 | draw C015 with a **baked product texture** (new `PRODUCTS` entry `sigma_barrier_field` = rim 1FE0A over UV 0-1, plus the hex cells 18F9B tiled ~4 x 3 across it (ours: the shader's tiling isn't readable) with the seams 18F9A), added light in `SIG_BARRIER_N` (team) / `SIG_BARRIER_ENEMY_N`, alpha ~0.25 in the middle (ours) so the rim and hex lines read; the four corner motes as `mercy_noise` glows 0.7 m at 2/s each |
| Low health 50 % / 25 % | **457B** (1A2A #464 from #462 / #463): **sounds only** (H) | the look change is the shader's (M: 548D1's cracks by health); ours: blend in a `sigma_barrier_cracks` product below 50 % and more below 25 %, or tint toward the row's red end |
| Hits on it | none: 1A2A's damage message (#685) only feeds his ult / perk (H by search) | the shooter's own impacts (as now) |
| Sent (his hand) | **2DD2** (1B55 #781 while SECONDARY is held, 3P 000B / 1P 0002, offset (-0.05, 0, 0.1)): streaks 16E9D x 4C09 **0.49 m stretched x2, 6/s at 4.9 m/s, 0.67-1.3 s** (`SIG_BARRIER_SEND`); glints 1FC54 x 19167 0.12-0.37 m, 0.17 s at effect hp 0079 (`SIG_BARRIER_SEND_SPARK`) | 6 streaks a second off his left hand along the aim, a glint now and then |
| Moving | **2E34** (1A2A #714, under the mover #636): sounds only (H) | - |
| Held (with him) | **2DF5** (1B55 #964 under the hold anim #812): a 0.33 m glow (`SIG_BARRIER_HAND`) and a 0.51 m one (row 39B9 r1, grey-cyan), 0.15-0.25 s; the orbs 30EB at his 02FD-0300 | one 0.33 m lavender glow at the left hand while the anim plays (ours: once per anim) |
| Recall (his side) | **2DD7** (1B55 #792 on the recall state #723 / #722, 0.3 s): a 0.84 m glow 4C09 x 16E9D, 0.35 s + a 0.35 m one (`SIG_BARRIER_HAND`) | the 0.84 m glow at his left hand |
| Recall (the barrier) | 1A2A #675 / #691 (health >= 1, M: `op74(health, 1)`): **2C90** at the barrier aimed at him + 1.5 m: a flash 29F7 **2.0 m, 0.1 s** (`SIG_BARRIER_RECALL_FLASH`), glows 174D8 **2.0 / 3.0 m, 0.18-0.36 s** (`SIG_BARRIER_RECALL_GLOW`), a 2006C line 1.8 / 3.0 m, a 7.7 m 19270 x 208AE wash (BC5: L a distortion); then **2E68 x4** (#723-#726, at the four corners v431 / v1188-v1190, scaled to the barrier): streaks 1FBCE **1.0 m, 2-3 each at 20-30 m/s toward him, 0.19-0.33 s** (`SIG_BARRIER_FLYBACK`, falls); 2C78 (#715) corner motes + sounds; 2C96 sounds | a 2 m `lucio_spark` flash and a 2 m glow at the barrier, then 3 `sigma_streak` streaks from each corner flying to his hand at 25 m/s |
| Break | **4E26** (1A2A #668: stopped or held, health < 1, M), **6919** (#909: during recall), **2E66** (#693: while moving) (H nodes, M which) + **457A** sounds: **16 puffs 14F9 0.68-1.28 m** in a 65 deg cone, 0.2-0.35 s (`SIG_BARRIER_BREAK`), 8 more 139C, 8 chips 5DB2 (10 frames) 0.88-1.5 m, **50 sparks 1F35 0.08-0.1 m at 4.4-9.4 m/s, 0.25-1 s** (`SIG_BARRIER_BREAK_SPARK`, falls), **12 shards** on mesh EE4 (2B0F BC5 / 2B0E hex outline / 2B10 hex) **0.75-1.3 m at 3-5 m/s, 0.2-0.27 s** (`SIG_BARRIER_BREAK_STREAK`); 2E66 adds **a barrier-shaped flash, mesh A3C 6.79 x 4.25 m, size 0.8-1, 0.18 s** (17C0 `brigitte_shield_glow`: `SIG_BARRIER_SHATTER`, HDR 5; and 1298 tint (10, 6.5, 0)) | at the barrier: the A3C flash (new mesh `sigma_barrier_flash`, or C015 itself flashed white-blue for 0.18 s), 16 `lw_thorn_flame` puffs, 12 `brigitte_hex_outline` shards and 50 `spark`s, all spread over its 6.7 x 4.2 m face (ours: the data spawns from a point) |
| Destroyed, waits 5 s | no effect (H) | - |
| Hyper Regeneration's heal on it | **6E81** (1A2A #1043, on message 2961 = the perk's heal, #1040): streaks on mesh EE4 2B10 x 2B0E **0.95 m stretched, 16/s for 0.5 s, 0.34-0.66 s** at the barrier's 01DA (`SIG_BARRIER_HEAL`) and a material effect (not drawable) | 8 teal streaks rising over the barrier face |
| Emitter orbs 30EB (model D7C9 / FC88, 0.14 m) | his body material **ADC9** (textured) and the empty child effect 2860: **no glow of their own** (H) | the textured orbs as now |
| Clips (3163, 32E9-32EE, 3160 / 3161 / 31B8 / 31CC) | sounds only (H) | - |

## Kinetic Grasp (1D02, overhealth 1D03)
| Part | Effect | Rebuilt |
|---|---|---|
| The field (held 2 s) | **2BE4** (1D02 #471 under the 2 s chase #489; 3P hp 0004 / 1P 0002; inside, hp 01B6 and 0313) (H): **disc C770 (2.0 m flat) 2.8 m ahead of 01B6, size 4.1-4.5, 8/s, 0.37-0.49 s, facing back at him** (18A1 x 200E4, `SIG_GRASP_FIELD`, HDR 3) (M: 8-9 m across if sizes scale the mesh as for quads; the box it guards is 2 m x 2.7 m: check by eye); a **dish C943 0.52-0.83 m, 6/s** at 01B6 (`SIG_GRASP_DISH`); a **shell C62D (1.5 x 1.5 x 1.14 m) at 1.0, 2/s, 1 s** drifting back at 0.25 m/s (`SIG_GRASP_SHELL` / `_ENEMY`); at 0313 an **orb DBF 0.3 m** held (`SIG_GRASP_ORB`) and **glows 208CC 2.65-3.0 m at 10 -> 22/s by dataflow 64F** (M: the amount absorbed), 0.1-0.15 s (`SIG_GRASP_GLOW`); a haze 4C09 3 m, 2/s, 2 s (`SIG_GRASP_HAZE`); a 1 m star at 01B6; lights 3 | a flat disc 2.8 m ahead of his eyes (new mesh `sigma_grasp_disc` or a camera-facing `cat_rope`-banded quad), 2.7 m tall (the box, H) rather than 8 m (ours until checked), 8/s fading; the shell as `sigma_grasp_shell` at 1.0; the 0.3 m orb and its glow (rate up with the absorbed damage) at his left palm |
| Each absorbed projectile | **2C84** at the shot (1D02 #518 / #695): flares 208CC (~1.5 m, 0.14 s: REND values, M) (`SIG_GRASP_ABSORB`), a 4-frame ring 1CB5E on mesh 310C (`SIG_GRASP_ABSORB_RING`); **222B** / **222F** (#456, at the projectile, him / others: M a deflect flash): a flare CC29 1.48 m 0.48 s, a star 29F7 0.64 m, **58 sparks 1F35 at 4.4-7.6 m/s**, 2 bursts 3B80 0.7-1.1 m (teal-yellow / red-orange rows) + sounds; 2F31 / 2C46 / 2894 sounds | a 1.5 m `sigma_star` flare + the `juno_hit_ring` ring where the shot dies, 12 `spark`s (ours: fewer than 58) |
| Overhealth at the end | **2D3E** (1D03 #208, hp 0001): a **material effect on his body 2.17 s** (LUT 8011 row 1 plum -> violet -> blue -> sky blue, HDR 1.9: `SIG_OVERHEALTH`; row 2 orange: `_ENEMY`, M); **streaks** 2B0E / 2B0F / 2B10 **0.1-0.2 m at 300/s for 0.27 s, 0.5 s, 2 m above hp 0004** (`SIG_OVERHEALTH_STREAK`); at 0.73 s a **sphere DBF 2.0 m, 0.4-0.52 s** with a constant tint (0.13, 0.8, 2.32) (`SIG_OVERHEALTH_SHELL`) | a 2 m sky-blue bubble at 0.73 s, 80 short rising streaks round his upper body over 0.27 s (ours: fewer), and a faint violet glow on him for 2 s (the material effect, ours) |
| Kinetic Cycle | **6731** (#741 after message 2623): sounds only (H) | - |
| 2E19 / 2C26 / 6C74 (2603) / clips 31CF / 31D2 / 31D4 | sounds only; **3182** (1P grasp_start, hp 0002) the same material effect on his 1P arms (H) | - |

## Accretion (1BBB; rock 18B7, graph 1EB9, model C5CA)
| Part | Effect | Rebuilt |
|---|---|---|
| Gather (0.65 s cast) | **2C64** (1BBB #643 through the cast #535, hp 01B6; also the 1P clip 3065) (H): at 0.27 / 0.47 s **three flat quads 31D5 at size 4.0**, 174D8, 0.45 s (pink / yellow-green rows: L parameters); at 0.37 s **spheres DBF 1.58 m and 3.16 m, 0.45 s** (`SIG_ROCK_GATHER` / `_ENEMY`); at 0.5 s **dishes C943 1.32 m and 1.65 m**, 0.34-0.37 s (`SIG_ROCK_GATHER_DISC`); at 0.83-0.9 s a flat 1BAC **3.5 m** and a star 208CC **1.96 m**, 0.15 s (`SIG_ROCK_GATHER_FLASH`); lights 2 / 5 | a dark-navy 1.6 m sphere growing at 01B6 from 0.37 s, a 1.6 m `ana_lens_disc` dish at 0.5 s, a 3.5 m flash at release (0.83-0.9 s in the 1.5 s effect = just after the 0.65 s cast, M) |
| The rock | model **C5CA** + look E20A (H) | as now |
| Flight | **2D99** (child 3330, 10 s) (H): **two ribbons 1298, up to ~1.0 m wide, 30/s, 0.5 s** (`SIG_ROCK_TRAIL` / `_ENEMY`: a black-blue-black bell); **debris chunks** 2C0F8 (4 frames) on mesh 101B6, **0.12-0.22 m, 20/s (dataflow 56D), 4-4.9 m/s, 1.9-2.8 s, spin -13** (colour row L: a parameter; ours grey stone) | `Fx.trail` 1.0 m 0.5 s, soft band, `SIG_ROCK_TRAIL`; 20 `sigma_rock_chunk` bits a second falling off (blended grey, gravity: ours) |
| Impact / burst | **2C07** (18B7's `m_6CB564CC`, 4 s; 1EB9 #365 64C7 per target = sounds) (H): **flash 4DB x 1FC54, 0.25-0.35 s** (`SIG_ROCK_IMPACT_FLASH`, HDR 5); **2-3 stars 208CC 0.2-0.32 s** (`SIG_ROCK_IMPACT_GLOW`); **core sphere DBF 0.26 s** (`SIG_ROCK_CORE`, violet HDR 17); **puff bursts 14F9: 10 + 3-5 + 2-3 at 3.3-5.5 m/s, 0.08-0.15 s** (`SIG_ROCK_IMPACT_DUST` / `_EMBER`); **dust 245A / 5429 (4 frames) 5 x 0.7-1.7 m stretched x2, 2 s** (`SIG_ROCK_SMOKE`, brown); **10 + 10-15 + 4-5 rock chunks 2C0F8 0.25-0.39 m at 6-8.4 m/s in a 64 deg cone, 1.9-2.8 s**; 10 debris sprites 183B4 (BC7 colour, 2 frames) at 1.6-2.8 m/s; **a ground decal 2048A (crater cracks) for 3.9 s** (`SIG_ROCK_SCORCH` / `_ENEMY`); a distortion dome 958 (BC5 18F81, size 4, 0.2 s); light to 5.4 | a 2.5 m flash, 3 `sigma_star`s, 12 `lw_thorn_flame` puffs, 5 `ana_puff` dust clouds (blended brown), 15 `sigma_rock_chunk`s thrown up with gravity, a `sigma_crater` decal on the ground (`KirikoFx.groundY`) fading over 3.9 s; no dome (distortion) |
| Knockdown end | **67BF** (224E #564): sounds only (H) | - |
| Stadium | **69AB** (25F6, #366): a 0.3 s flash + 32 streaks inward at 39-50 m/s; **69EE** (2657, #406): two 7-8 m flashes (H) | not drawn (Stadium) |
| Clips | 3P / 1P cast and throw clips: sounds; the spheres' 7020 / 71CD play **2EB5** (2D5B's twin: the hand-sphere glow); the rock's own clips 74B3 / 738C (5 s, 32C2 / 32C3: sparkles and a ribbon at 3.2-4.2 s) are in no set read (L: a showcase / emote) | 2EB5 as 2D5B |

## Gravitic Flux (1E41; lifted targets 1DBC; reticle cosmetic 31A2)
| Part | Effect | Rebuilt |
|---|---|---|
| Aim (up to 5 s) | **2C9E** (child 31A3 of the cosmetic 31A2, 1E41 #1287 at the aimed point v432, 5 s): a **ground decal** 202A6 (concentric rings) x 9489 (soft disc) x 202B3 (ring with a dark eye) (`SIG_FLUX_RETICLE_DECAL`, HDR 10) and a **cylinder DE0** (0.944 m x 1 m) mask 18A1 (`SIG_FLUX_RETICLE`), both sized by statescript inputs (M: the 7.5 m x 15 m cylinder); **2C91** (#1286) is empty | a 15 m ring decal on the ground (`sigma_flux_rings`), and a faint `brigitte_band` cylinder 15 m across, ~3 m tall (ours), only for him (cosmetic, M) |
| His start, flight and every phase | **2C68** (#998 under #967, the 0.6 s start, and #974, every phase; hp 0076 Head, 6 s): **glows 4C09 x 4DB 0.66-1.33 m, 5/s, 0.65-1 s** (`SIG_FLUX_BODY` / `_ENEMY`), **halos 2006C 2.0 m, 5/s, 1 s** (`SIG_FLUX_BODY_HALO`, HDR 7), noise quads 0.5-1.0 m 5/s; light 5 (offset 2.0, M up) | 5 a second of lavender glows round his head and a 2 m `sigma_lens_line` halo, through the ult; not in his own view |
| Cast (1 s) | **2C6B** (#986 at v2478, the aimed point, under the cast #1117) (H): a **column cylinder DE0, 1C7A x 200E4, held 3 s** (`SIG_FLUX_COLUMN` / `_ENEMY`) and an **outer one, 6 s** (`SIG_FLUX_COLUMN_OUTER`), both sized by inputs (M: the 7.5 m radius, 15 m tall cylinder from 5 m below the point); **a ground decal** (`SIG_FLUX_DECAL`); **sparks 1F35, 64/s + 32 at 10-12 m/s up, 0.17-0.43 s** (`SIG_FLUX_SPARK`); lights to 8-10 | a 15 m wide, 15 m tall `brigitte_band` cylinder of blue light (alpha low, ours) with a `sigma_flux_vortex` decal, sparks rising all round its edge |
| Targets taken | **2C6A** (#1000 at v2478 when #979 tracks): rising **cylinders DE0 at 20 m/s (0.56 s)** (`SIG_FLUX_PULL`), a 0.41 s flash cylinder, **sparks at 23-25 and 40-41 m/s up** (`SIG_FLUX_PULL_SPARK`, falls), a decal 9489 0.83 s; lights 10 | a quick upward sweep of the cylinder and 40 fast sparks |
| Each lifted target (1DBC) | **2D88** (#557 at the lift's start, hp 0001): **3 discs 11C1 x 3.0, 0.22-0.27 s** (`SIG_LIFT_RING`), two 4-4.9 m glows 174D8 0.32 s; **2BEA** (#587, the target on the ground, M: v417 = in the air): dust 14F9 1.95-2.15 m (`SIG_LIFTOFF_DUST`), debris 245A / 5429 2-2.5 m at 15 m/s (`SIG_LIFTOFF_DEBRIS`), glows A7B6 0.5 m (`SIG_LIFTOFF_GLOW`), a dome 3299 x 2.0 rising at 4 m/s, a decal 2048A 1.5 m; **2BEB** (#588 while airborne, 2 s): **4 ribbons on hp 0001, 0.34 / 0.49 m wide, 20/s, 0.23-0.29 s** (`SIG_LIFT_TRAIL`, `SIG_LIFT_TRAIL_DARK`); **2BE7** (#617, held while lifted; also Hyper Strike's 54A5 #530): **discs 11C1 x 2.0-2.5, 1.5-2/s, 1-1.5 s** (`SIG_LIFTED_RING`), **glows 174D8 2-3.9 m, 4/s, 0.5 s** (`SIG_LIFTED_GLOW`), the same 4 ribbons for 0.4 s, and a **2 m screen layer** (ECIM(40), the lifted player's own view) | per target: 3 rings at the start, dust and debris if it stood on the ground, 2 ribbons following it (`Fx.trail` 0.34 / 0.49 m), a slow-pulsing 2 m ring + 3 m glow round it while lifted |
| Slam | **2CA2** (#590, the slam damage), **2E8D**, **2CA3**: **sounds only** (H by search: no slam visual in 1DBC / 1E41) | ours: a small `sigma_crater` decal + dust where each lands (none in the data) |
| 2BEF, 2E12, 2E18, 2D85, clips 30E1 / 30E2 / 3165 / 3166 / 3169 / 3167 / 32CE / 30F6 / 32CF / 3104 / 311E / 3120 / 3269 | sounds only or empty; 311F (spheres a65c) plays 2EB5 (the hand-sphere glow) (H) | - |

## Perks
| Perk | Effect | Rebuilt |
|---|---|---|
| **Levitation** (2577) | **65BF** (51E0 #413, hp 0001, through the 1.75 s): mesh 963D (2 x 1 x 2 m half-shell) with 299E1 x 299E8 x A7B6, **0.66-0.78 stretched x8-10, 3/s, 1.1 s, spinning 4-6** (`SIG_LEVITATE`, violet / `_ENEMY` row 6); **652D** (#407) sounds | 3 violet streaks a second rising round him (new mesh `sigma_levitate` or stretched `cat_noise` quads) |
| **Hyper Strike** (25AF) | **65C7** (#422 held while charged, 3P hp 0002 Wrist_L): glows 4C09 **0.5-0.94 m, 20/s, 0.28-0.41 s** (`SIG_HYPER_STRIKE`, row 1 `_ENEMY`), glows A7B6 0.5-1 m 20/s, dish sparks C943 **0.13 m, 7/s + 5/s** (`SIG_HYPER_STRIKE_SPARK`), lights; **6545** (charged) sounds; on the target **54A5**: 2BE7 (the lifted look) + 6550 / 6551 / 67C0 sounds | a 0.7 m `cat_soft` glow pulsing at his left wrist while charged; the lifted look on the struck target |
| **Kinetic Cycle** (2578) | 6731: sounds only | - |
| **Hyper Regeneration** (26B7) | 6E81 on the barrier (above) | as above |

## Missing, Stadium, not drawable
- **Missing from the build: none** (TankLib opened all 135; Sojourn's Overclock had 12 missing).
- **Stadium-gated (H by gating, not drawn):** 6C27 (25F9 third sphere's volley sounds), 69AB (25F6) and 69EE (2657) on the
  rock, the Stadium barrier pet 6866 (2657; same model and child 2C8E), 6C74 (2603, L Stadium: sounds).
- **Not drawable as particles:** the barrier's and the cores' looks are **material shaders** (7C75, 7C98: rebuilt from their
  masks and LUTs above); the material effects on bodies (2D3E / 3182 overhealth, 6E81 barrier heal); the **refraction /
  distortion** renderers (2EAB C25 sphere and 2C07 C75 dome with BC5 normal map 18F81; 2C90 C18 19270 x 208AE; 2BEA / 2C07 /
  2BE7 BC5 flow maps B08, 245A, 19270 used alongside); the 2 m ECIM(40) screen layer on a lifted player.
- The 127-type material slots on C015 / FC88 (71FC-7202, 6A9E-6AA3, F48C97DEC3DA with LUTs 80050D5E8716, 800460D490F2, ...) are
  shared hero material-effect slots (L: damage flash, hacked, ...), not Sigma's.

## Not done / open
- **Grasp's disc size** (8-9 m read as quads, against a 2 m x 2.7 m box): check in game. **Barrier hex tiling** (the shader's
  scale isn't readable; UV is 0-1 over the barrier): pick by eye. **Which barrier break** plays when (op74 / op71 on the health
  read as >= 1 / < 1: M). **2603** not identified. The rock clips 74B3 / 738C not placed. Enemy rows not seen from a second client.
- Not tried: anything in game (this pass is data only).

## Palettes
120 `SIG_*` palettes (+80 `_N` normalised twins) in `E:\Overwatch Archive\research\sigma\work\fx_palettes_sigma.txt`, written by
`fx_sigma_palettes.py`: part 1 is `static final float[] SIG_X = {...};` lines with a doc line each (what, effect / component,
LUT id + row, peak, falls, the game's hex at 0 / .25 / .5 / .75 / 1), shaped like `SojournPalettes`; part 2 the
`SIG_X(SigmaPalettes.SIG_X),` enum lines for `EffectArt.Palette`. Each LUT id + row is also in the tables above.

## Masks for the pack
Proposed `build_asset_pack.py` `EFFECT_MASKS` entries (`research\sigma\fx` as the source):
```
    # Sigma's (tools/reference/sigma-effects.md; research\sigma\fx). Hyperspheres: the launch swirl, the noisy wisp, the burst's
    # lens streak, thin streak, star, radial rays, wispy ring and bowtie; Barrier: the horizontal line, the soft square, the
    # field's rim and hex seam; Accretion: the rock chunks, the crater cracks, the debris puff; Flux: the soft disc, the reticle's
    # rings and the vortex ring
    "sigma_swirl": ("Sigma", "Hyperspheres", "000000020126", 1),
    "sigma_wisp": ("Sigma", "Hyperspheres", "0000000200E4", 1),
    "sigma_lens": ("Sigma", "Hyperspheres", "00000001FDB2", 1),
    "sigma_streak": ("Sigma", "Hyperspheres", "00000001FBCE", 1),
    "sigma_star": ("Sigma", "Hyperspheres", "0000000208CC", 1),
    "sigma_rays": ("Sigma", "Hyperspheres", "0000000007AB", 1),
    "sigma_wisp_ring": ("Sigma", "Hyperspheres", "00000001FC54", 1),
    "sigma_bowtie": ("Sigma", "Hyperspheres", "0000000205F4", 1),
    "sigma_lens_line": ("Sigma", "Barrier", "00000002006C", 1),
    "sigma_soft_square": ("Sigma", "Barrier", "0000000208AE", 1),
    "sigma_barrier_rim": ("Sigma", "Barrier", "00000001FE0A", 1),
    "sigma_barrier_seam": ("Sigma", "Barrier", "000000018F9A", 1),
    "sigma_rock_chunk": ("Sigma", "Accretion", "00000002C0F8", 4),
    "sigma_crater": ("Sigma", "Accretion", "00000002048A", 1),
    "sigma_puff": ("Sigma", "Accretion", "0000000183B4", 2),
    "sigma_disc": ("Sigma", "GraviticFlux", "000000009489", 1),
    "sigma_flux_rings": ("Sigma", "GraviticFlux", "0000000202A6", 1),
    "sigma_flux_vortex": ("Sigma", "GraviticFlux", "0000000202B3", 1),
```
- Products (`PRODUCTS`): `sigma_barrier_field` = `sigma_barrier_rim` with `cat_hex` (18F9B) and `sigma_barrier_seam` tiled over
  it (the tiling ours); `sigma_barrier_cracks` would need 548D1's red channel (BC5: not a mask as is).
- Reused (same ids): `cat_band` 1C7A, `juno_noise_c` 5DF, `cat_cloud` 18E48, `cat_soft` 4C09, `spark` 1F35 (4),
  `brigitte_soft_blob_b` 174D8, `cat_cone_line` 16E9D, `mercy_noise` 4DB, `lw_thorn_flame` 14F9 (2), `mercy_soft` 139C,
  `brigitte_chip` 5DB2 (10), `brigitte_hex_outline` 2B0E, `brigitte_hex` 2B10, `brigitte_shield_glow` 17C0, `lucio_spark` 29F7,
  `cat_rope` 18A1, `juno_hit_ring` 1CB5E (4), `ana_star_burst` 3B80, `mercy_lens` CC29, `ana_puff` 5429 (4), `mercy_noise_b` 4A7E,
  `glow` A7B6, `cat_noise` 299E1, `noise` 299E8, `juno_cloud_noise` 299E9, `cat_hex` 18F9B, `cat_soft_noise` 18F00,
  `cat_glow_noise` 19167.
- Not masks: 1298 (16 x 16) and 460 (8 x 8) are solid white (the ribbon / mesh shape is in its ramp or geometry: use a soft tube
  band); B08, 245A, 19270, 18F81, 2B0F, 548D1 are BC5 two-channel flow / normal maps.
- Meshes (`EFFECT_MODELS`), new: `sigma_cone` CA08 and `sigma_orb` 85B and `sigma_burst_sphere` DAB5 (Hyperspheres),
  `sigma_grasp_disc` C770 and `sigma_grasp_shell` C62D (KineticGrasp), `sigma_barrier_flash` A3C (Barrier), `sigma_dome` 3299
  (GraviticFlux), `sigma_levitate` 963D (Perks), `sigma_flat_flash` 1BAC (Accretion); reused: `cat_bubble` DBF, `ana_lens_disc`
  C943, `sombra_ring` 11C1, `brigitte_band` DE0, `juno_sphere` D721, `cat_sphere` 1385.
