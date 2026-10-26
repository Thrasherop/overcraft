# Kiriko's ability effects in the game data

What Kiriko's effects are made of in real Overwatch, where the extracted files are, and how to rebuild each one in
Minecraft. Read 2026-10-26 from the archived offline build 154088 (prometheus-2_25_0_0-154088, Overwatch and
Battle.net closed, read only, nothing online). Text only: the textures and meshes are Overwatch assets and stay in
the owner's archive; they reach the game only through the local asset pack. Format details, scripts and the full
per-effect decode: `E:\Overwatch Archive\research\kiriko-effects\NOTES.md`, `summary.txt`, `effects\<guid>.txt`.
For what the effects look like on screen, see `kiriko-reference.md` section 4 (this file agrees with it).

## Files

`E:\Overwatch Archive\extracted\154088\Effects\Kiriko\<Ability>\` with `<Ability>` = `HealingOfuda`, `Kunai`,
`ProtectionSuzu`, `SwiftStep`, `KitsuneRush`, `WallClimb`:

- `textures\<id>.png`: every texture the ability's effects use. Texture arrays (flipbooks) are split into
  `<id>.png, <id>_1.png ... <id>_N.png`, one frame each.
- `ramps\<id>.json`: the colour/parameter look-up textures decoded to floats (`[row][x][r,g,b,a]`, 256 wide).
  Use these, not the PNGs of the same id, which are clamped.
- `models\Models\<id>.00C\<id>.owmdl`: effect meshes, plus the projectile models (with look, materials, textures).
- `KitsuneRush\fox_entity\`: the spirit fox: model 105FB, look, 8 animation clips (`.owanimclip`).
- `manifest.json`: every effect, every component (start, duration, mesh, material, textures with size/format,
  spawn values). `research\kiriko-effects\sheets\` has contact sheets of all of it.

Ids below are the hex index of the GUID (`30242` = `000000030242.004`, effects are `.00D`).

## How an Overwatch effect is built

An effect (`.00D`) is a list of components, each with a start time and duration (seconds from the effect's start)
and optionally a hardpoint (attach bone). A particle system is an **emitter** (spawn rate, burst count, lifetime,
speed, and per-particle spawn values: position from a shape, direction, size, rotation, spin) feeding a
**renderer**: billboard/mesh particles, a **ribbon** (trail strip), a **decal**, a **model**, a **light**, or a
**material overlay** applied to the model it is played on. Sounds are components too.

- **Textures are grayscale masks** (BC4: soft dots, petals, streaks, noise, flipbook smoke) plus a few BC5 flow/
  distortion maps (red/green swirls: they bend the image behind, skip them) and rare colour sprites (BC7).
- **Colour comes from gradient maps**: float LUT textures whose rows map mask brightness (0..1) to an HDR colour.
  Each comes as a team pair: **teal for the owner and allies, red for enemies** (gold for the locked-on Ofuda).
  Values above 1 mean "glows": render additive and clamp.
- Random values are either `base + random x range` (e.g. rotation -pi + rand x 2pi) or uniform between two values;
  below they are written as ranges.
- Numbers read with good confidence: component start/duration, burst counts, rates, lifetimes, sizes, colours,
  meshes, textures. Inferred from value patterns: which slot is speed/size/spin (see NOTES.md). Not found:
  ribbon width/length, light colour/radius, decal size (marked "ours" when a recipe needs them).

### Shared pieces

| Id | What it is | Used for |
|---|---|---|
| mesh 0F5B | 1x1 m camera-facing quad | almost every sprite particle |
| mesh 0797 / 31AF | flat ribbon strip / tube ribbon (r 0.5, 25 segments) | Ofuda + Suzu trails / Kunai trails |
| mesh 0F5F | thin torus ring, r 0.2 (scaled by size: size 7 = 1.4 m ring) | expanding rings everywhere |
| mesh 1BAC / 1E7F | flat ground annulus r 0.28-0.89 / r 0.42-0.64 | ground ripples (Swift Step, Kitsune path) / Suzu ground wave |
| mesh 963D | hemisphere dome r 1 | Suzu burst dome |
| mesh 103CD | flat five-petal flower, 2.5 m | Suzu burst flower |
| mesh 0D06 | small curved card | sakura petals (with sprite 3267E) |
| mesh 10ADC | half-cylinder shell | slash/swirl arcs (Swift Step, torii) |
| mesh 10942 | wavy paper sheet, 1.5 x 1.3 | the flying ofuda talisman |
| sprite 3267E (64) | sakura petal | petal confetti (Swift Step, Kitsune Rush, torii) |
| sprite 30242 (512) | five-petal flower | heal hit, Suzu ground decal, Swift Step flash |
| sprites A7B6, FFC9, 4C09, 139C | soft round glows | cores and flashes |
| sprites 2E59A, 2FF91, 2FFA3 | streak / diagonal band / flame-edge masks | ring and arc surfaces (scrolled) |
| sprites 299E0-299E9, 306CE, 19167 | noise | erosion inside rings, ribbons, dome |
| sprite 7A9 (4x256), 1C7A (16x256) | 1D fade strips | ring cross-section falloff |
| sprite 377D5 (64, 48 frames) | smoke puff flipbook | Kitsune Rush cast |
| sprite 1F35 (64, 4 frames) | blob/spark shapes | Kunai impact, Suzu flight sparkles |

### Colours (sRGB hex, linear values from the ramps)

| Name | Ramp values (dark -> mid -> bright) | Hex (dark / mid / bright) |
|---|---|---|
| Kiriko teal (most effects) | (0, .25, .22) -> (0, .5, .44) -> (0, 1, .87), often to x3 HDR | #008981 / #00BCB1 / #00FFF0, core to white |
| Kunai / Kitsune cyan | (0, .51, .45) -> (0, 1.24, 1.08) -> (3, 3, 3) | #00BDB3 / #00FFFF / #FFFFFF |
| Seeking-Ofuda gold | (.36, .24, 0) -> (.71, .49, 0) -> (6, 5.3, 2.5) | #A18700 / #DCB900 / white-gold |
| Suzu ring | constant (0.1, 2.0, 3.0) | #33D5FF, bright |
| Suzu ground mark | constant (3.2, 2.17, 0.1) | #FFD732, bright |
| Enemy view | (.25, .05, .05) -> (.5, .1, .1) -> (1, .21, .21) | #893F3F / #BC5959 / #FF7E7E |

Hex values are the linear ramp values converted to sRGB and clamped at 1; "core to white" means the HDR end of the
map is several times over 1, so the brightest pixels go white.

## Healing Ofuda

Two projectile versions exist, matching the wiki ("yellow = seeking, blue = no target"):

| | No target (entity 34EA) | Locked on an ally (entity 3FD0) |
|---|---|---|
| In flight | effect 2F93 | effect 7E2D |
| Hits an ally (healing received) | 4004 | 40C0 |
| Hits terrain / expires | 4111 (+ decal) | 3D4A (+ decal) |

- **In flight, 2F93 (teal):** one **paper talisman** mesh particle (mesh 10942, mask 3A1EE = 4 talisman
  silhouettes, streak 2FF91 scrolling over it), size 0.8, one alive at a time (1/s, 1 s life), plus a **ribbon
  trail** (strip 0797, texture 30182 = soft sheet brighter at the head, eroded by noise 299E8), teal map.
- **In flight, 7E2D (gold):** the same talisman + ribbon, plus a gold core glow (FFC9, life 1.5 s, size 1) and a
  tiny sphere sparkle (477E, noise 18F00, thin line 1F70F); colours gold -> hot white.
- **Hit on an ally, 4004 / 40C0 (this is the "healing received" visual):** a quick flash of soft glows (A7B6, size
  0.2-0.8, life 0.08-0.4 s, drifting 0.35-0.6 m/s) and in 40C0 a **five-petal flower** (30242, size 0.32, life
  0.34 s) and a short light (0.1-0.47 s). Teal (4004) or gold (40C0).
- **Terrain hit, 4111 / 3D4A:** a soft glow (size 0.3-1.3, life 0.08-0.4 s) and a **flower decal** on the surface
  (30242 with circle mask 3E08) for 0.73 s.
- **Hand effect 2074** (weapon subgraph; likely the throw/charge at the hand): a golden swirl sprite (1268, BC7
  colour), glowing dots (126A, 20/s), a ring (52E8).
- Models: held/thrown Ofuda 10945 (16 bones), the weapon atlas 31E3B (paper with red seal and kanji, also the
  Kunai and bell) is in its `Textures`.

**Recipe:** a projectile renderer that draws the talisman as a camera-facing (or velocity-aligned, slightly
fluttering) quad 0.25 x 0.75 m with the talisman mask tinted white/paper colour, plus an additive glow quad behind
it (teal, or gold when the ofuda is homing). The trail is a ribbon: keep the last ~8 interpolated positions
(per render frame, not per tick), build a camera-facing strip ~0.2 m wide (ours), UV.v along the length, alpha
fading to 0 at the tail, texture 30182 multiplied by the teal map. On an ally hit, spawn 2-3 additive glow
particles (life 0.1-0.4 s, rising slowly) and the flower sprite (0.32 m, 0.34 s, scale up and fade). On a block
hit, put the flower on the block face as a flat quad 0.01 above it, fading over 0.73 s.

## Kunai

- **In flight, 3BF2 (entity 4103):** a **tube ribbon trail** (mesh 31AF, fade strip 7A9 x noise 299E0) cyan ->
  white-hot, plus glint streaks (20065, a horizontal lens streak) emitted 16/s, life 0.1-0.17 s, size 0.42-0.46.
  The thrown model is 1096F (0.28 m, 13 bones).
- **Hits terrain, 3BF4:** 6 tiny sparks (sprite 1F35, 4 shapes; size 0.05-0.06, life 0.17-0.44 s, speed 3-5 m/s,
  spin -10 rad/s), cyan. Hitting a target (13B2) is sound only: the red hit sparks the reference mentions are the
  game's generic damage effect, not Kiriko's.
- **Variant 5C3A (fiery, exploding):** gold/orange trail 5F37 (two tube ribbons, embers CC29, smoke 1CB5D/7ADD) and
  an explosion 6099 (sphere shock DBF, flare 2613A, fireball DAF, 15 glow puffs, 10 streak sparks 2C009, smoke
  flipbooks 1DFCB/1DFCA/1FC65 of 32 frames, two lights). What selects it is in a part of the weapon graph not read (likely a perk); the base
  Kunai is 4103.

**Recipe:** the kunai model (or a 3D item) aligned to velocity, a short cyan tube/ribbon trail (~1-1.5 m, ours)
fading to the tail, an additive glint billboard at the head. On a block hit, 6 tiny additive sparks bursting out
3-5 m/s, gravity-free, fading over 0.2-0.45 s.

## Protection Suzu

- **Flight, 3CB2 (bell 3FD3, model 10946):** 2 ribbon trails (0797 + 30182, teal), 75/s sparkles (1F35, size
  0.15-0.3, life 0.25-0.64 s) left behind, and lights; 1.67 s. Flight sound 3E6F.
- **Burst, 360C (in the slot the Ofuda uses for terrain impacts; it has the ground decal) and 3D07 (the other
  impact slot, probably a direct hit on a player: bigger dome and rings, no flower or decal):**
  - **dome**: 2 hemispheres (963D) textured with noise 306CE, size 3 (5.5 in 3D07), life 0.3 s;
  - **ground wave**: flat annulus (1E7F) with soft bar 52F4, colour (0.1, 2, 3) = bright sky-blue, size 4.5 (9),
    life 0.25 s;
  - a **chromatic distortion ring** (17D8, rainbow ring; size 10 (20), life 0.6 s) and a flat **distortion flash**
    (C4A, size 8, 0.2 s);
  - 360C only: a **flower** mesh (103CD) size 2, life 0.5 s, and a **flower decal** (30242) for 0.53 s;
  - two lights, 0.5-0.83 s.
- **After the burst, 6089 (area entity 781F):** two ring decals (3684, a thin circle) gold (3.2, 2.17, 0.1) /
  red, held 5 s, and two lights for 5 s.
- **Allies hit, 3398:** a **material overlay** (03C3) for 1 s on their model: noise 19167 + tile grid 29D41. On
  screen this is the "bright white glowing silhouette" of the reference.

**Recipe:** at the impact point: an additive hemisphere (generate a UV sphere's top half in code, radius ~4 m to
match the ability, ours) scaling from 0.5 to 1 over 0.3 s while fading, noise texture scrolling; a flat ground ring
(code-generated annulus) expanding to ~4.5 m over 0.25 s, colour #33D5FF; a flower quad on the ground 2 m fading in
0.5 s; a faint gold circle on the ground for 5 s (flat quad, circle 3684). For the allies, render the entity again
with an additive white/teal layer (like Minecraft's glowing outline but filled) for 0.65 s.

## Swift Step

- **Departure, 3CC7 (played on Kiriko when she leaves; no position given), 3 s:** 2 rings (0F5F with streak 2E59A,
  size 7 = 1.4 m, life 0.3-0.35 s, teal, rising) and a 30/s ring stream of size 9 for ~0.1 s, **33 + 6 petal
  cards** (0D06 + 3267E, size 0.045-0.08, life 0.6-0.7 s / 0.14-0.22 s, speed 2-3 m/s in an upward cone), a
  distortion flash (C4A, size 4.5, 0.22 s), and the **material overlay** 0448 for 0.43 s on her model (noise 5DF:
  she dissolves).
- **Arrival, 3CC5 (played at a computed position, the destination), 3 s:** 2 rings pointing down, **33 + 20 + 12
  petals** (life 0.2-1.5 s, speed 1-3 m/s), a teal sphere puff (EFC0, size 0.6-0.8, life 0.24-0.48 s), **7 slash arcs** (10ADC with
  2FF91/2FFA3 bands, size 3-3.7, life 0.24-0.4 s, random rotation), 60/s ring sparks, two lights, overlay 0448.
- **On the target ally during the step, 3CC4 (hardpoint 0x21):** a teal flower flash (30242, size 1.4 and 0.35, life
  0.25-0.3 s, colour up to x33 HDR) and ground ripples (1BAC with fade strip 7A9, 20/s, life 0.2 s, size 2.1).
- 4491: overlay 0448 on Kiriko for 0.3 s. 6E4F (entity 6B25/6B26): two vertical light beams (138F, size 2, life
  0.3 s), rings and 6 petals.

**Recipe:** at both ends: 2-3 additive teal rings expanding from 0 to ~1.4 m in 0.3 s (code-generated thin torus
or a ring sprite), a burst of ~30 petal sprites (tinted teal-white, 0.05-0.08 m, flung up 1-3 m/s, falling slowly,
life 0.6-1.5 s), a soft flash. Fade Kiriko's model out over 0.4 s at the start and in over 0.4 s at the end (alpha
or a noise-dissolve shader). Optional: 5-7 crescent arcs (a half-ring quad) spinning at the arrival.

## Kitsune Rush

- **Cast, 3C5F (4 s):** smoke flipbook 377D5 (48 frames, size 0.5-2, life 0.4-0.5 s) teal, and rings (0F5F,
  30/s, size 1.5).
- **The fox, entity 3F54:** model 105FB (162 bones, 2 meshes; in model units 2.9 m nose to tail, 0.85 m tall,
  Z up like all `.owmdl`; fur cards and a blue body texture 312D5), look 13201, animation set 0D9D (8 clips in
  `fox_entity`; the looping ones AE68, AE6A, AE6B are the run-cycle candidates). Its child effect 3E3D sheds
  **33-66 petals per second** for 10 s (0D06 + 3267E, life 0.6-0.7 s). A separate pet entity 42BA (mover, no
  model) is created by the cast graph; what it does is not in the data read.
- **Torii gates, effect 3C82 (graph 31EE), 10.67 s each:** a **model component** = torii mesh 1048D (1.8 m wide,
  1.2 m tall in model units; look 12FF1: white/grey with a gradient ramp: a translucent glowing gate) plus slash
  arcs (10ADC, size 4-4.5 and 7-9, life 0.8-1.2 s), petals (2-7 per s, life 1-2.5 s, speed 8-11 m/s), and "+"
  shaped sparkles (3DC7, gold-white HDR, 3-6 per s, size 0.43-0.6, life 0.9-1.2 s). Six gate sounds 3DC8-3DCE go
  with it (one per gate).
- **The path (graph 33DB):** ring pulses 40AB / 40C6 / 40A2 (rings of size 3-9, life 0.2-0.35 s, teal,
  + lights, 40A2 adds petals and smoke), **floating talisman slabs** 4013/4014 (mesh 109BC = rounded card, size 1-2,
  life 11 s: the closest thing to the reference's "glyph tiles"; a dynamic variable picks one of the two), and
  **ground ripples** 2E87 (flat annulus 1BAC, 3/s + 2/s, life 1 s, size 2.1, 8 s).
- **Allies in the path (graph 2169 -> 2071, 52CF):** 3C65 = rising teal/gold open cylinders around them (DE0, 3/s,
  size 1.2 growing to 2.5, life 1 s); 3D2F = **speed lines** (vertical streak 2C701 on a quad, 40/s, size
  3.5-4.5, life 1-1.1 s, almost static) and spinning glow puffs (18B18, 40/s, size 0.2-1.2), plus material
  overlay 0445 (noise 2BE4A) on the ally.

**Recipe:** the fox as a custom entity renderer using the converted model (Blender pipeline, like the hero model)
with an additive/translucent cyan material and its run loop, moving 32 m along the cast direction while dropping
petals. Torii gates: a model (converted 1048D) rendered translucent cyan-white, placed every ~5 m along the path
(6 gates), each appearing with a ring burst and petals, held ~10 s then faded. Ground: spawn flat teal ripple
rings (2.1 m, 1 s) along the path 3/s, and optionally flat talisman quads lying on the ground for the duration.
Allies inside: speed-line quads around them and a faint teal cylinder wisp every 0.3 s.

## Wall climb

Effect 289 (1 s): 3 **dust puffs** (colour sprite BE5, warm tint (0.83, 0.53, 0.17) = #EAC072, flow map B08),
size 1.08-1.2, life 0.33-0.4 s, random rotation +-90 deg, drifting up. Recipe: Minecraft's own poof/dust
particle with that tint, 3 per climb step, is close enough.

## Healing received

Kiriko's heal shows only the Ofuda hit effect on the ally (4004 teal / 40C0 gold, see Healing Ofuda). The generic
"being healed" screen and outline feedback belongs to the shared hero graphs and was not searched.

## Practical notes for Overcraft

- Put the masks in the local asset pack (`build_asset_pack.py` handles models; textures can be copied into
  `local-assets/pack/assets/overcraft/textures/effect/...`). Pre-colour them offline when possible: apply the
  gradient map to the mask (`rgb = map(mask)`, `alpha = mask`), then one ordinary additive particle texture is
  enough in Minecraft.
- Render all glows additive (blend ONE, ONE), no lighting, no fog-darkening, depth test on, depth write off.
  Rings/domes/ribbons are cheap to generate in code; only the talisman, torii, fox, kunai and bell need the meshes.
- Keep timings from the tables (seconds), convert to ticks only for spawning; animate scale/alpha per render frame.
- Team colour: use teal for own/allied view, red (#FF7E7E family) if enemies of the viewer ever see them.

## Not found / not certain

- Ribbon width and length, light colours and radii, decal sizes, the meaning of several renderer slots.
- What picks Kunai 5C3A over 4103, Suzu burst 3D07 over 360C, slab 4013 over 4014.
- A ground decal or tile texture for the Kitsune path ("glyph tiles"): not in the path graphs read.
- Particle attribute names are hashes; their roles (size, speed, spin) are inferred from the values.
