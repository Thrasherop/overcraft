# Lúcio's ability effects in the game data

What Lúcio's effects are made of in real Overwatch, where the extracted files are, and how to rebuild each one in
Minecraft. Read 2026-10-27 from the archived offline build 154088 (prometheus-2_25_0_0-154088). Overwatch and
Battle.net were checked closed before every session; everything was read only, offline, through TankLib/DataTool.
Text only: the textures and meshes are Overwatch assets and stay in the owner's archive. They reach the game only
through the local asset pack. The general format (components, emitters, tracks, spawn values) is the one
`kiriko-effects.md` describes. This file adds what is different for Lúcio and doesn't repeat it.

Confidence after each number or claim: **H** read in the data, **M** inferred (one link guessed), **L** guess.
"Size" is the spawn size value. It scales the mesh (a 1 m quad at size 0.5 is 0.5 m), the same reading as Kiriko's
(M).

## Files

`E:\Overwatch Archive\extracted\154088\Effects\Lucio\<Ability>\`, with `<Ability>` one of `SonicAmplifier`,
`Soundwave`, `Crossfade`, `AmpItUp`, `SoundBarrier` or `WallRide`:

- `textures\<id>.png`: every texture the ability's effects use. Flipbooks are split into `<id>.png, <id>_1.png ...`.
- `ramps\<id>.json`: the float LUTs decoded, `[row][x][r,g,b,a]`, 256 wide.
- `models\Models\<id>.00C\<id>.owmdl`: the effect meshes, plus the wall-ride helper quad DCA5.
- `manifest.json`: for each effect, every component: start, duration, mesh, materials, textures (size, format,
  frames), emitter values, spawn values, the renderer and light tracks that could be resolved, and its hardpoint.

Counts: 47 effects (20 with visuals, 27 sound-only), 235 texture entries (341 PNG files; some ids repeat
across abilities), 144 decoded ramps and 39 meshes across the six folders. No texture or mesh failed. The research files are in `E:\Overwatch Archive\research\lucio\`:
- `work\fx_summary2.txt`: a readable digest of every emitter.
- `work\fx_strips.txt` and `work\fx_colours2.txt`: the colours as hex.
- `work\fx_esht.json`: the hardpoint tables.
- `work\sheets\`: contact sheets of the sprites, the colour strips (`ramps_<Ability>.png`) and mesh wireframes.
- `scripts\fx_extract.py`, `fx_summary2.py`, `fx_sheets.py`, `fx_meshes2.py`, `fx_strips.py`, `fx_colours2.py`,
  `fx_esht.py`, `fx_shapes.py`, `fx_graphfx2.py`: the scripts that made them.

Ids are hex indexes: effects are `.00D`, meshes `.00C`, textures `.004`, hardpoints `.03C`.

## What's new compared with Kiriko (format)

- **Song colour lives in small sRGB colour strips, not only in float LUTs (H).** These are the 512x8 / 256x4
  BC1/BC7 textures 3F95, 3FCE, 3F94, 3F9A, 35BC, 410E, 3126 and 3509. Each one is a horizontal gradient, the same in
  every row. The particle's grayscale mask indexes it. The float LUTs (hashes 65BC and 12BB) are mostly parameter
  rows: channel selectors, intensity curves and an HDR boost. A few rows are real gradients, with the team colour
  pair (green or red).
- **Each effect has a hardpoint table (H, new).** The top-level `ESHT` chunks start with a hardpoint GUID (03C),
  then the index of the component that uses it. For example, the song lights are on Foot_L 001C and Foot_R 001B.
  Kiriko's NOTES didn't decode this. `fx_esht.py` reads it.
- Every spawn shape in these effects is a point at the origin (`ESGP` all zeros). The ESHT entry is what places an
  emitter (H).
- Several effects carry **both song variants side by side**: Amp It Up, the wall ride and the perk bursts each have
  an amber set and a green set. The song picks which one shows. What does the picking wasn't decoded (M).

### Meshes

| Id | Shape (model units) | Used for |
|---|---|---|
| 0F5B / 0801 / 10F4E | 1x1 m quad (billboard / flat two-sided) | sprites, sparks, glows |
| 0F78 | flat disc, r 0.5 | flashes, Soundwave discs, distortion discs |
| 3FAE | thick flat ring, r 0.5-0.67, 0.1 thick | Soundwave rings |
| 1E7F / 96C1 / 1E8E | flat annulus, r 0.42-0.64 / thin, r 0.56-0.64 | Amp rings, hit rings |
| 19B6 | tiny flat annulus, r 0.035-0.069 | projectile rings, ult weapon rings |
| 19F2 | shallow cap, r 0.126 | hit and wall-jump rings |
| 0958 | dish or dome, r 0.49, 0.375 high | distortion pulses (with the BC5 flow map 18F81) |
| 2345 | open cylinder, r 0.47, 0.67 high | Sound Barrier bands (cast, perk, on allies) |
| 0DE0 | open cylinder, r 0.47, 1 high | crossfade columns 1D2C/1D2D |
| 0DBF / 085B / 1385 | sphere, r 0.5 | ult shock spheres, projectile halo, Amp puff |
| F3D | flat disc, r 0.368 | Sound Barrier ground disc on allies |
| 13CA / 0797 / 07F5 | ribbon strip 1x1 (07F5 is two crossed strips) | song skate trails, projectile trails |
| 2316 / 2318 / 240A | curved band about 0.7 x 0.15, sitting 1.24-1.72 out from the origin | heal / speed / amp "band" |
| DCA5 | flat 4 x 4 m quad | wall-ride helper entity 4EE5 (no effect material) |

The meshes are in the files' own axes. How a mesh particle is turned (which axis follows the spawn direction) wasn't
decoded (L). In the recipes, flat rings lie flat and discs face the direction of travel.

## Colours

The sRGB hex values come straight from the colour strips. Gradient rows from the LUTs are converted linear to sRGB.
The order is dark → mid → bright.

| Name | Where | Hex | Conf |
|---|---|---|---|
| **Healing Boost (amber)** | 3F95 (BC7), heal set of every song effect | #B37401 → #FFCD01 → #FBF701 → #FAFF62 | H |
| Healing Boost, second strip | 3FCE (BC1) | #B85800 → #FA8900 → #FFB600 → #F7F800 → #FAFE73 | H |
| **Speed Boost (green-cyan)** | 3F94 (BC7) | #01A250 → #01E767 → #0BFF90 → #52D8EA | H |
| Speed Boost, second strip | 3F9A (BC1) | #00934A → #00B755 → #00EC6B → #0BFE8F → #52D7E9 | H |
| **Lúcio team green** (weapon, Soundwave, barrier) | 35BC / 410E strips, LUT rows | #5FD900 → #7BEB00 → #A2FE00 → #F2FE5D → white; rounds #368200 → #4EB400 → #74FF00 | H |
| Soundwave green (HDR to white) | 800806D5 r1 | (0, .8, 0) → (0.07, 1.64, 0) → (1.3, 2.5, 0) → (4.1, 5, 2.8): green core, white-hot rim | H |
| **Enemy view (red)** | 3126 strip, the LUT twin rows | #5D0000 → #FC0000 → #FE5615 → #FEC5B0; rounds #BD2500 → #DE4200 → #FF5D00 → #FFFFAE | H for the colour, M that it's the enemy view |
| Sound Barrier, enemy | 800917A9 r4 | #870000 → #BA0000 → #E56947 → #FFF0B6 | H, M |
| Ult on allies, other strip | 3509 | #870100 → #E34500 → #FF8200 (orange) | H colour, L role |
| Crossfade column (to speed / to heal) | 1D2C r2 / 1D2D r2 | #32D822 → #E5FFD4 / #FFE64A → #FFF600 | H colour, L role |

**Heal is amber, speed is green (H).** The chain that shows it:
1. Graph 0266's switch on the song variable 1735 runs State#167 when it's true. That state holds music A90 and
   effect CD2. When it's false, State#168 runs, holding music A8F and effect CAB.
2. The ally graph 0267 tests the same variable. Its false branch writes the heal stacks (4733 and 6590), which feed
   the heal amount, variable 420. Its true branch writes the speed stacks (4734 and 6591).
3. So CAB is the heal song and CD2 is the speed song.
4. CAB is tinted by 3F95 (amber) and CD2 by 3F94 (green).

Check it against footage before shipping the colours (M that the screen shows this mapping).

## Sonic Amplifier

| Part | Effect | Conf |
|---|---|---|
| In flight (projectile 05FF → child entity 0600) | A3F, 1 s, looping | H |
| In flight, variant projectile 4307 → 4308 | 65B8: the same parts about 20% bigger, plus one more ring set (sprite FFDC). What fires it wasn't resolved (L: a perk or amped state) | H |
| Hit on a target | 0838, 0.33 s (sounds 5D68 and 6D20, sub-effect 405B with sound 8DE7) | H |
| Hit on the world, or expiry | 0AE0, 0.67 s | H |
| Muzzle | 097A: **sounds only** (5D67, 6D1F). No muzzle-flash visual was found. The weapon's child effects 2860 and 2B1E are empty | H |

**In flight, A3F** (each part's colour is a green/red team pair):
- **Core glow:** a 1.5 m camera-facing quad (F5B, sprite 210FD, a soft glow), one held per loop. Colour
  #9EB243 → #E2FF64, up to 3x HDR (H).
- **Shed rings, set 1:** 19B6 at size 2.2 (r 0.15 m), 16 per second, life 0.4 s, thrown backwards at 7 m/s.
  Sprites 42F0 and 3363 (strips). Green #368200 → #74FF00 (H).
- **Shed rings, set 2:** size 3.0 (r 0.21 m), 8 per second, life 0.5 s, 3 m/s backwards, sprite 190EC. Green
  #327A00 → #62DA00 → #F4FFC5 (H).
- **Halo:** a sphere (085B) at size 3, one at a time, life 3 s, on the projectile's hardpoint 01DA. Sprites 7A9 (a
  falloff strip) and 18E48. Green, 5x HDR. The size reads as r 1.5 m, so it is probably a very faint fresnel halo (L).
- **Two ribbon trails:** a flat strip (0797, texture 39B74, colour #87A44B → #E3FF80) and a cross strip (07F5,
  #597800 → #D3FF39). Width and length weren't decoded (L).
- **Light:** intensity 2. Its colour comes from an input that wasn't decoded (L: green).
- **Sound:** loop 2523 for Lúcio, 2524 for others (2-4 s).

**Hit on a target, 0838** (components on hardpoint 0006 of whatever was hit; H for the table, L for which bone it is
on the victim):
- A distortion disc 0.7 m across (F78 + BC5 18F81), life 0.1 s.
- Two flat ring flashes (96C1, r 0.38-0.59 m), life 0.15 s, each playing the 25-frame flipbook 3FF5.
- Two green HDR caps (19F2 at size 1.8 and 4.0, r 0.23 and 0.5 m), life 0.2 and 0.25 s, colour #3C8E00 → #8CFF00
  → white.
- Two soft glow discs (4F3, 0.5 m across), life 0.2 s, moving at 1.6 m/s.

**World hit or expiry, 0AE0:**
- One small puff (quad at size 0.13, sprite 139C), life 0.175 s, rising 0.9-1.1 m/s.
- **15 sparks** (29F7, 0.11 m), life 0.17-0.44 s, 1.5 m/s, random spin.
- A 2 m distortion flash (BC5 2A57C), life 0.14 s.
- Two copies of the set: one on green strip 35BC, one on red strip 3126 (H).

**Recipe:**
- Make the round a custom projectile renderer: an additive green 1.5 m soft-glow quad facing the camera, with a
  0.3 m bright core inside it (ours).
- Every render frame, while flying at 50 m/s, shed thin flat rings: 0.3 m wide, 16 per second, each living 0.4 s,
  drifting 7 m/s back along the flight path, fading out. Add a 0.4 m ring 8 times per second (life 0.5 s).
- Add a short green ribbon (about 1.5 m, 0.12 m wide: ours) that fades towards the tail.
- On an entity hit: two green rings growing from 0 to 0.5 m in 0.15-0.25 s, and a 0.5 m flash.
- On a block hit: 15 additive green sparks, 1.5 m/s, life 0.2-0.45 s, no gravity, and a 0.15 m puff.
- Enemy view: the same parts in red #FC0000 → #FE5615.

## Soundwave

How it plays: effect 05AD, 0.5 s, is played at hero hardpoint 0077 (bone_0597) by graph 0776#215 (H). Its
emitters are on hardpoint 0006, which is the Sonic Amplifier's muzzle (weapon DBA3, bone_009F) (H for the table, M
that it resolves to the weapon). Everything flies **forward along the aim (+Z)**. Sounds come from sub-effect 4046
(blast 8DE7, 0.4-0.6 s) and from 097A or 64D6. 02B1 plays a sound on each target and adds no visual (H).

Components:
- **Main blast disc:** F78 at size 1.37 (1.4 m across), life 0.3 s, flying at 6 m/s. It plays the 9-frame flipbook
  3526: concentric pulse frames (H).
- **Second disc:** quad 10F4E at size 2.7, life 0.4 s, 8 m/s (H).
- **Three thick rings:** 3FAE at sizes 0.9, 0.6 and 1.2 (r 0.6, 0.4 and 0.8 m), life 0.3, 0.25 and 0.35 s, speeds
  4, 2 and 6 m/s. Masks 3F9D (a band) times 1C77 (cloud noise) (H).
- **Fast sparks:** 50-60 per second for the 0.5 s, plus a burst of 10-15. They are tiny (0.006-0.036 m), live
  0.3-0.4 s and travel at **17-19 m/s**, so they read as streaks (H).
- **Two distortion pieces:** a disc of size 2.2 and a sphere of size 3 (BC5), life 0.2-0.3 s, 3 m/s (H).
- **Two glow discs:** 4F3 at 0.75 m and 2.5 m, life 0.4 and 0.2 s (H).
- **Two lights:** 0.33 s, intensity rising 1.4 → 3 (H).

Colour: green, HDR to a white rim. The pulses run #04E600 → #47FF00 → yellow → white; the rings run #00AA00 →
#7DCD00 → #EAFFC5. The rings' other row is orange-red, #AA2500 → #FFCFA1 (H colour, M that it's the enemy view).

**Recipe:**
- At the muzzle, along the look direction, spawn 3 flat **ring quads** facing forward. They start at 0.4-0.8 m,
  grow about 2x and fly forward at 2, 4 and 6 m/s, living 0.25-0.35 s.
- Add one 1.4 m **pulse disc** (concentric-rings texture, or animate a ring texture) at 6 m/s for 0.3 s, and a 2.7 m
  faint disc at 8 m/s for 0.4 s.
- Add **30 streak sparks** at 17 m/s, life 0.35 s, in a narrow cone, and a 0.75 m flash at the muzzle for 0.4 s.
- Everything is additive, green to white.
- The visible blast reaches about 1.2-3 m. The 7 m reach is gameplay, not the visual (H for the speeds and lives).

## Crossfade: the song aura (what a player sees most)

**On Lúcio:** CAB (heal) or CD2 (speed) is held for as long as that song plays. Each is a 1 s loop of 27
components (H). The parts:
- **Glowing skates:** a light on each foot (Foot_L 001C, Foot_R 001B), intensity 1.0, and one on the upper body at
  0076 (bone_017A, under Spine_3, about 0.2 m off the chest), intensity 0.74. H for the hardpoints and intensities;
  the colour comes from an input that wasn't decoded (M: the song colour).
- **Two ribbon trails:** strip 13CA, emitted 60 times a second, each point living 0.39 s, width about 0.5 (size
  0.508). Texture: noise 5DF times the soft vertical band 3A37 times the song strip. The hardpoint table lists the
  two feet without a component, so these are **trails from the skates** (M). Behind him as he moves, they show as two
  short amber (heal) or green (speed) streaks, about 0.4 s of travel long.
- **A beat pulse:** the distortion dish 0958 with flow map 18F81, 4 per second, life 0.6 s, size 0.13. Two glow
  sparks (29F7, size 0.11-0.13, life 0.3-0.35 s, random rotation) in the song colour. Their brightness curve jumps
  to 0.31 in the first 6% of the life and fades to 0 (H values; L for how large it looks: the sizes are small, and
  the renderer may scale them).
- **The band:** mesh 2316 (heal) or 2318 (speed) on the root hardpoint 0004. One per loop, starting 0.2 s into it,
  life 1.39 s, size 1.0 / 1.63. Masks 5DF, 4F3 and 29F7 in the song colour. It sits in the mesh's own space, 1.24-1.46 out from the origin
  along one axis. Where that lands on him (chest height in front of him, or on the ground ahead) depends on the
  unresolved axis question (L).
- **No big ring under him (H).** None of the 27 components is a ground ring or decal wider than about 0.7 m.

**To allies:**
- **Healing Boost and plain Speed Boost show no world visual on the ally.** Ally graph 0267 (status 0A4.053) plays
  no effect itself (H). The healing-received glow, if Overwatch shows one, comes from shared graphs, which weren't
  searched (L).
- **Speed Boost while Amp It Up runs:** subgraph 07C7#154 holds **4ADD** on the ally. 07C7 only starts it when the
  amped speed stack (6591) is set (H). 4ADD lasts 6 s and is **speed lines**:
  - 300 tiny streaks a second, after a first burst of 200, 0.02-0.05 m, life 0.27-0.37 s, flung at 4-4.2 m/s from
    two disc spawn shapes around the ally (H).
  - 50 more a second, after a burst of 30, life 0.22-0.32 s, 4-5 m/s, size value 10 (L: probably a stretch factor).
  - Both use sprite E99F (3 frames) and a renderer value of -8 (L: stretched along their velocity).
  - It has no colour strip, only channel selectors, so it shows white (M).
- The same subgraph sets the ally's speed after a 0.2 s wait (H).

**Swap flashes:** 1D2C (3 s, green) and 1D2D (1 s, yellow) are client-only and run on a game message (var 17221).
Each is two open cylinders (0DE0, r 0.47, 1 m tall, life 3 s) with strip 18A1, plus one 0.63 m glow quad (H). They
are probably the column you see when the song changes: green going to speed, yellow going to heal (L).

**Recipe:**
- While a song plays, draw two **additive light trails from the skates**: keep the last 0.4 s of each foot position,
  draw a 0.15-0.25 m wide (ours) camera-facing ribbon fading to the tail, and scroll a soft noise along it. Use amber
  #FFCD01 → #FAFF62 for heal and green #01E767 → #52D8EA for speed.
- Make the skates glow: a small additive glow quad on each foot, 0.3 m (ours).
- Each second, a beat: a 0.15 m spark at each foot, life 0.3 s, flaring fast and fading.
- No ground ring.
- Swapping songs: a 1 m tall translucent cylinder around him in the new colour, fading over 1 s.
- Allies under amped speed: white speed-line streaks around them, about 100-300 a second (ours: 30-60 per tick
  budget), life 0.1-0.35 s, 4 m/s, drawn as 0.02 x 0.3 m stretched quads.

## Amp It Up

Effect 075C is held while Amp runs: a 0.97 s loop of 26 emitters, for 3 s (H). Sound comes from A45. The music
doesn't change (see `voice_sounds_effects.md`).

It carries **both song sets** (3F95 / 3FCE amber and 3F94 / 3F9A green). The current song picks one (M). Its parts,
each listed once per loop:
- **Two flat rings:** 1E7F at size 0.25 and 1E8E at size 0.33 (r 0.16 and 0.21 m), life 0.45 s. They play the
  14-frame flipbook 3AE1 and use the amber strip 3FCE: expanding pulse rings (H).
- **A puff on the upper body:** sphere DBF on hardpoint 0076, size 0.375 (r 0.19 m), life 0.62-0.83 s (H).
- **12 sparkles:** colour sprite BCD (BC7, so it has its own colour), size 0.1, life 0.58-0.86 s, 0.7-2 m/s in a
  70° cone (H).
- **Glow quads:** 29F7 at sizes 0.22, 0.075, 0.5 and 0.13, life 0.3-0.58 s. A burst of 3 smoky glows (4DB / 3B24,
  0.22-0.34 m, life 0.48-0.66 s). A burst of 7 flipbook ripples (3FF5, 25 frames, size 0.75-1.21), life 0.31-0.76 s,
  0.03 m/s (H).
- **The band mesh 240A:** two copies (one amber, one green), life 1.5 s (H).

**Recipe:**
- While amped (3 s), every 0.97 s: two flat rings at his feet growing from 0.1 to 0.45 m in 0.45 s (ours: scale x3 for
  readability, L), in the song colour.
- A burst of 12 sparkles flung up 0.7-2 m/s, living 0.6-0.85 s.
- A 0.4 m glow puff at the chest.
- Speed up the skate-trail emission so the aura reads as stronger (ours).

## Sound Barrier

| Part | Effect | Conf |
|---|---|---|
| Cast | 11BF, 2 s (graph 073A#204) | H |
| Weapon glow | 1286 at the muzzle 0006, 1 s | H |
| Slam (landing) | 67CE, 1 s (073A#612) | H |
| On each shielded ally | **0D86**, held 8 s, on the ally's root 0004 (graph 073B#155, status 191.053) | H |
| Ally sounds | 1237 (10 s loop 28F7), 1132 (26F0 / 26F1, 2.67 s: gaining or losing the shield, L) | H |

**Cast, 11BF:**
- **Ground ring flash:** F78 at size 8 (8 m across), life 0.3 s, playing the 9-frame pulse flipbook 3526. Green
  #6FDA3C → #B6E8A3 → white (H).
- **Rising bands:** three cylinders (2345) at size 1 (0.94 m across, 0.67 m tall), life 0.7, 0.8 and 0.9 s, and one
  at size 2, life 0.2 s. Strip 3363 with mask 3FF7. Green #398700 → #52BA00 → #7CDC55 → white. Enemy: #870000 →
  #E56947 (H).
- **Two lights:** from 0.13 s to 1 s, intensity 1.6 → 5 (H).

**Weapon glow, 1286 (at the muzzle):**
- A 0.63 m glow (sprite 242, green strip 35BC), life 0.83 s.
- Sparks (1F35, 4 shapes, 0.07-0.18 m) at a rate climbing from 23 to 55 a second, life 0.31-0.38 s, at a negative
  speed of 6.0-6.7 m/s, so they fly inward (M).
- Small rings (19B6, 0.2 m) at 2.4 → 11 a second, life 0.58 s.
- Colour: very bright yellow-green, (7, 7.9, 3) (H).

**Slam, 67CE:**
- **Shock sphere:** DBF at size 7 (7 m across), life 0.3 s. Brightness curve 75 at 3% of the life, 2 at 20%, 0 at
  the end: a bright flash that fades (H).
- A sphere of 4 m, life 0.22 s.
- A 6 m distortion sphere, life 0.2 s.
- A noise sphere (085B, 7 m), life 0.28 s, orange-gold #D32500 → #FFD092 → #FFFCD1.
- A sphere (1385, 7 m), life 0.34 s.
- **Two lights:** 0.03-0.5 s, intensity 1 → 5 (H).

**On allies, 0D86 (the overhealth look):**
- **A material overlay** (01E1.065) on the ally's model for the whole effect. Its masks are noise 16E9D and the small
  tiles 1CA8C and 1298, so it is a shimmering skin layer (H for the overlay, L for its look).
- **Bands around them:** cylinder 2345 at size 1.1 (about 1 m across, 0.74 m tall), each living 1 s. They spawn on a
  sub-burst event (L). They use the green strip 410E, and a second set uses the orange strip 3509, with distortion
  7DC (H).
- **A ground disc:** F3D at size 2.89 (r 1.06 m), life 1.38 s, green strip 35BC (H).
- **Two lights,** intensity 2 (H).
- The effect is 8 s long, but the overhealth lasts 6 s. The ally graph's state ends it, so it runs as long as the
  shield (M).

**Recipe:**
- Cast: an additive flat ring on the ground growing to 8 m in 0.3 s, plus 3-4 green translucent cylinders 1 m wide
  rising and fading over 0.7-0.9 s around Lúcio.
- Landing: a 7 m sphere flash (bright white-green, fading in 0.3 s) and a camera-visible light pulse.
- Each shielded ally, for as long as the overhealth lasts:
  - render the entity a second time, slightly inflated (1.03x), with an additive green #A2FE00 shimmer at 25-40%
    alpha and scrolling noise;
  - a 1 m wide green band around their waist, re-spawned every second;
  - a faint 2 m green disc under their feet that pulses every 1.4 s.

## Wall Ride

E12 is held for the whole ride, as a 1 s loop (graph 0263#189). Its sounds are 241D (heard by others) and 241E
(Lúcio's own) (H). The emitters come in pairs, one per side (spawn shapes S0/S4, S2/S3, S5/S6). The hardpoint table
lists Foot_L 001C, Foot_R 001B and the chest 00A4 (H).
- **Skate sparks (amber set):** quads with masks 3F9D / 3F9C and strip 3FCE, 25 a second, life 0.18-0.31 s. The
  spawn size follows a curve from **0.15 to 0.44 over 0-0.67 of the 1 s loop**, then 0.83 at the end. They drift up
  and back, direction (0, 0.56, -0.58), at 0.025 m/s (H for the values; M that the curve runs on the effect's
  clock).
- **Wall ripples:** the 25-frame flipbook 3FF5 on green strip 3F9A, bursts of 3 plus 26 a second, life 0.18-0.19 s,
  size 0.19-0.32. Pushed out sideways, direction (-1.05, 0.02, 0.01), at 0.14-0.19 m/s (H).
- **Glow dots:** 29F7, 31.7 a second per emitter, life 0.18-0.36 s, size 0.2-0.55, 0.13-1.66 m/s from small
  spheres. One pair uses the speed strip 3F94, the other the heal strip 3FCE (H). The song picks which pair shows (M).

**Wall jump, 3FA1** (on the root 0004, graph 0263#577):
- 4 caps (19F2 at size 2.4-3.5, r 0.3-0.44 m), life 0.2-0.31 s, on the green strip 35BC.
- 3 distortion dishes (0958), life 0.3 s, flung at 1.7-5.9 m/s (H).

**Entity 4EE5** (made by 0263, on hardpoint 0001): its model DCA5 is a plain 4 x 4 m quad with no effect material.
Its child 5239 plays sound A3E8. It is a helper, not a visual (M).

**Recipe:**
- While riding, every tick: 1-2 glow dots per skate in the song colour (0.2-0.5 m, life 0.2-0.35 s, drifting off the
  wall at up to 1.6 m/s).
- 1 green flipbook-style ripple quad on the wall at each skate (0.2-0.3 m, life 0.2 s).
- Sparks that grow from 0.15 to 0.44 m over the first 0.7 s of a ride (M).
- Wall jump: 4 green rings 0.6-0.9 m across at his feet, life 0.25 s, plus a short distortion-less puff.

## Practical notes for Overcraft

- Pre-colour the masks offline, as for Kiriko (`rgb = strip(mask)`, `alpha = mask`). Each song effect needs two
  textures (amber and green). The team effects need green, plus red if enemies of the viewer ever see them.
- Draw everything additive, depth-tested, without depth writes. The distortion pieces (BC5 flow maps 18F81, 7DC and
  2A57C) bend the image behind them. Skip them, or replace them with a faint additive ring.
- Keep the timings in seconds from the tables above. Animate per render frame. The song effects loop every 1 s
  (Amp: 0.97 s).

## Not found / not certain

- Which axis of a mesh particle follows its spawn direction (M/L for the band 2316/2318/240A placement and the dish
  orientation). Ribbon width and length. Light colours (an `ECIN` input). Most renderer tracks: the u16 at ECPR 0x34
  is the component of its particle system (H), and the rest are only partly mapped.
- Why some sizes are so small (the 0958 pulse at 0.13, the 29F7 sparks at 0.11). The renderer may scale them (L).
- Why the projectile variant 4307/65B8 fires. The roles of 1D2C/1D2D and 3509 (orange). The `ECSb` spawn trigger in
  0D86. How each effect with both song sets picks one.
- A world visual for the base songs on allies, or a range ring: not in graphs 0266, 0267 or 07C7. Shared, HUD or
  outline systems weren't searched.
- Graph 3BF8 is a near copy of the wall-ride graph with effects 4B3D/4B3E/4B42/48AF/48B0 (another hero or mode, L).
  It wasn't extracted.
