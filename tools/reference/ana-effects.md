# Ana's ability effects in the game data

What Ana's effects are made of in real Overwatch, where the extracted files are, and how to rebuild each one in
Minecraft. Read 2026-10-31 from the archived offline build 154088 (prometheus-2_25_0_0-154088). Overwatch and Battle.net
were checked closed before every session; everything was read only, offline, through TankLib/DataTool. Text only: the
textures and meshes are Overwatch assets and stay in the owner's archive. They reach the game only through the local
asset pack. The format is the one `lifeweaver-effects.md`, `mercy-effects.md`, `jetpack_cat-effects.md`,
`kiriko-effects.md` and `lucio-effects.md` describe; this file adds what is different for her. Her data map (graphs,
numbers, clips, effect ids by node): [ana-map.md](ana-map.md); the numbers: [ana.md](ana.md); the build plan:
[ana-plan.md](ana-plan.md).

Confidence after each number or claim: **H** read in the data, **M** inferred (one link guessed), **L** guess.
"Size" is the spawn size value: it scales the mesh (a 1 m quad at size 0.5 is 0.5 m; a ribbon's size is its width).
A range "a-b" is a random value between the two (Kiriko's `mode 5` / `mode 1` reading). "Stretch" is spawn attribute
6DFA (M: along the particle's axis or the shot). "Ours" marks what no data gives.

## Files

`E:\Overwatch Archive\research\ana\fx\Ana\<Ability>\`, with `<Ability>` one of `BioticRifle` (muzzle, both shots, hits,
scope, reload clips), `SleepDart` (cast, dart, hit, the sleep, Local Anesthetic), `BioticGrenade` (throw, flight, burst,
boost, anti-heal) or `NanoBoost` (cast, syringe, the boosted ally):
- `textures\<id>.png` (flipbooks: `<id>.png`, `<id>_1.png` ...), `ramps\<id>.json` (float LUTs, `[row][x][r,g,b,a]`),
  `models\Models\<id>.00C\` (effect meshes), `manifest.json` (per effect: emitters paired with their renderers,
  hardpoint table, spawn shapes, raw track nodes). Same layout as `research\lifeweaver\fx\Lifeweaver`.
- `Scope\` (empty `textures\`): the scope overlay search (below).
- Not under `extracted\`: pass `research\ana\fx` to `build_asset_pack.py effects` as one more source.
- Research files in `E:\Overwatch Archive\research\ana\work\`: `fx_cards.txt` (one compact card per renderer: mesh size,
  hardpoints, masks, colour rows with normalised hex and peak, emitters resolved: the file this note was written from),
  `fx_digest.txt` (every renderer with every LUT row, spawn attribute and renderer track: the file for any detail),
  `fx_brief.txt`, `fx_overview.txt` (one line per effect), `fx_summary2.txt|json`, `fx_meshes2.json`, `fx_palettes.txt`
  (the palettes below and the mask entries), `fx_sheets\tex_<Ability>.png` (every mask), `fx_sheets\meshes_<n>.png`
  (mesh wireframes), `fx_sheets\gui_ana.png` (her GUI images), `fx_effects\` (decoded effects), `fx_replacements.json`
  (every skin's effect swaps), `fx_scope.txt` / `fx_scope_stu.json` (the scope UI asset), `fx_extract_log.txt`.
- Scripts in `E:\Overwatch Archive\research\ana\scripts\fx\`: Lifeweaver's `fx_lw_*` copied as `fx_ana_*` with their
  paths retargeted (Lifeweaver's own are untouched); `fx_ana_extract.py` holds Ana's effect list (from
  `work\graph_effects.json`, the entity dumps and `fx_sounds_timed.json`); new: `fx_ana_scope.py` (the scope UI walk),
  `fx_ana_strips.py` (colour strip samples), `fx_ana_projbounds.py` (projectile model bounds), `fx_ana_guisheet.py`,
  `fx_ana_palpatch.py` (wrote Ana's rows into `fx_ana_palettes.py`). TankLib scripts run under the kiriko-anim venv's
  Python with `-I` through `run.py`; the others under `py -I run.py`. New files are read-only and listed with their
  sha256 in `fx\SHA256SUMS.txt`, `work\fx_SHA256SUMS.txt` and `scripts\fx\SHA256SUMS.txt`.

Counts: 82 effects extracted (24 with renderers, plus 4 reload clips holding the vial model; the rest sound only or
empty), 266 distinct textures (181 of them float LUTs), 15 meshes. Nothing missing.

## What's different from Lifeweaver, Mercy and the others (format)

- **Team twins everywhere (H rows, M roles).** Most renderers come in pairs drawn with the same mask and timing but a
  different LUT: a gold (or blue) one and a red / violet one. As in Overwatch's other heroes, the gold / blue one is the
  look her own team sees and the red-violet one the enemies' (M: no node picks between them that was decoded; the muzzle
  bulb's red twin and the grenade trail's red row follow the usual pattern). Several also carry a condition on dataflow
  `6B` (`lg0(dataflow(6B,1), ...)` in a ribbon's slot 3): L the team switch. **Draw the gold / blue twin only** (ours).
- **Her colours (H).** Biotic Rifle: **gold** (#FFD532 -> cream) with red-violet twins; Biotic Grenade: **violet with gold
  highlights** (#A200D0 -> #C93DF5 -> #F5DD45); the shots' damage on enemies: **purple** (#CA3BFF, HDR 10); Sleep Dart:
  **blue** (#3282FF) with lilac-white Zs; Local Anesthetic: **cyan-blue** (#73A1EE -> #26FDFD); Nano Boost: **Mercy's
  boost blue** (#1125FF -> #9ECAFF, the same row values as her boost beam) with **orange** (#FE4B00) accents.
- **Two hitscan "beams" built from particles (H).** The scoped shot (1A80) and Nano Boost's syringe (1A5F) draw flat
  quads (mesh 0801) whose spawn position is one input (`ECIN 0x48` / `0x27`: the start) and whose attribute 61F9CF82 is
  another (`ECIN 0x54` / `0x2C`: the end), with attribute 59DC556C a constant (0.2 / 0.15 / 0.08 m on the tracer; 0.16 to
  1.75 m on the syringe): a band from start to end, **59DC556C its width** (M), living 0.75 s (tracer) / 1-2 s (syringe).
- **Over-life rows (12BB LUTs) drive the Zs (M).** The sleeper's Zs read a 12BB row that is not colour but motion:
  x grows 0.86 -> 7.8 (their size multiplier) and another row runs (0, 0, 0) -> (-0.56, +1.36, -0.55) (their drift), the
  colour row lilac to white. Like Lifeweaver's petal age, read by age, not by mask.
- **Screen-space overlays (H data, L role).** Particles on mesh 0801 with spawn attribute 4E65CCF9 = `ECIM(40)` (the
  input Lifeweaver's dash flash had) appear in the sleep (1A58: a purple vignette D8F and rising Zs), the anti-heal (19F4:
  a vignette 05D0, a ring 6429, falling broken pluses) and Nano Boost (1A16 / 7A23: a 3.7 m disc 1BAC): L the affected
  player's own screen. Ours: a HUD tint, not world particles.
- **Plus-shaped mesh (H).** The heal boost's and anti-heal's icons are the mesh **E0AA**, a flat plus sign 7.2 cm across
  (103 vertices), with masks 27E5E (a soft plus glow, `mercy_plus` in the pack), 27E5D (a square frame), 3A0DE (a framed
  plus, inverted) and 391F2 (a diagonal slash, inverted).
- **No skin swaps on the default skin (H).** `GetReplacements` of her 164 skin themes (`fx_replacements.json`): the
  default theme 1921 (M: the one with no replacements; the hero's first two themes are 4879 and 1921) swaps nothing;
  themes 54E8 / 5571 swap the fire, scoped fire, dart cast, grenade throw, burst and nano cast effects (19FE, 1A92, 1A00,
  19FF, 1874, 1A12 -> 5371-5375 / 5349), 2C6A / 5CCB the 3P throw clip, 5A44 the rifle child 2B1E. Everything below is
  the default look.
- **Many of her effects are sound only (H):** rifle fire 19FE / 1A92, scope in / out / held 1A2F / 1A30 / 1A87, every hit
  confirm (1A14, 1A88, 1A89, 1A9A, 1A37, 1A43, 1A36, 7C77), the dart cast 1A00, the grenade throw 19FF / 6FC5 (19FA is
  empty), the heal boost's held 5485, the nano cast 1A12, the boost start / held 5FA8 / 5FAA, the sleep's held 1A1F, 67BF,
  7E60 / 7E70, 4010 (a sub-effect), and all her cast clips (dart, grenade, nano, fire, reload: sounds and camera shakes).

### Hardpoints

| Id | Where | Conf |
|---|---|---|
| 0006 of the rifle 159C | the muzzle: bone_009F, (0, 0.036, 0.900) in rifle space; flashes +5.45 cm along it, turned 90° | H |
| 0006 of the pistol 15CA | the Sleep Dart pistol's muzzle (0, 0.101, 0.165); flashes +2 cm along it | H |
| 0078 / 0075 | her left forearm (0.398, 1.176, -0.043): the nano syringe's launch (1A5E: both, L 0075's place) | H 0078, L 0075 |
| 0002 | Wrist_L: the grenade throw (19FA: empty) | H |
| 00A5 / 017E | her left hand / the rifle's vial slot: the reload vial (model 3677) | H |
| 0001 | the target's chest (Spine_3 + offset; 1.306 m on Ana): hit effects, boost, anti-heal, nano, numbness | H |
| 0012 | the target's head (1.570 m on Ana): the sleep's Zs (1A58) | H id, M which emitters |
| 0019 / 001A / 000E | other points on the target (anti-heal 19F4, nano 1A16) | H ids, L bones (limbs) |
| 0004 | the grenade's root: its trail (19FB) | H |
| 0011 | the sleep dart's trail (1A7D) | H id, L place (the dart model has only 0004) |
| 00A4 | the generic sound point | H |

### Meshes

| Id | Shape (model units, x size) | Used for |
|---|---|---|
| 0F5B / 0801 / 10F4E | 1 x 1 m quad (0801 two-sided) | sparks, glows, flares, beams (0801), screen overlays |
| 0E21 | 1 x 1 m quad with one edge at its origin (z 0..1) | nano flames and sparks licking off the body |
| DA82 | 1 x 1 m quad hung 0.5 m below its origin | the burst's rising tendrils |
| 07F5 / 099F | cross ribbon / 8-sided tube ribbon (1 x 1) | the shot's and dart's trails / the grenade's trail |
| **963D** | **hemisphere 1.976 x 0.988 x 2.0** (a bowl from y -0.988 to 0, rim at 0) | muzzle bulbs, the burst's domes, Local Anesthetic's dome, nano shells |
| 1385 / 0DBF | sphere d 0.984 | the burst's sphere / the enemy hit's bubbles, LA's range sphere |
| **C943** | lens disc 1.988 x 1.988 x 0.386 (y -0.177..0.209) | the enemy hit's swirling discs |
| 1BAC | flat disc 1.784 (r 0.892) with a hole r 0.294 | nano's overlay ring (L screen) |
| **E0AA** | flat plus sign 0.072 x 0.072 | heal boost / anti-heal icons |
| 15CAE | flattened sphere 0.97 x 0.97 x 0.588, centred | Local Anesthetic's shell |
| 3677 | the vial, 0.064 x 0.091 x 0.064 | reload clips (a model component) |
| Projectile models (entity, not effect) | shot 11741: a dart 0.032 x 0.16 m (centre 0.088 m behind its origin); sleep dart 11740: 0.019 x 0.094 m; grenade 11763: 0.10 x 0.094 x 0.148 m, centred (H) | the things in flight |

## Colours

sRGB hex from the LUT rows (linear, normalised by the row's peak where HDR), mask 0 to mask 1; HDR peak after. The
`EffectArt` palettes are at the end. Colour strips (BC1 / BC7 gradient textures) sampled with `fx_ana_strips.py`.

| Name | Where | Hex | Conf |
|---|---|---|---|
| **Rifle gold** | muzzle bulb 963D (19F8, 1A80) r5 (HDR 2) | #CE9E00 #FFD400 #FFFF00 #FFFF6E #FFFFFF | H |
| Rifle red (twin) | the same, r5 of 8003B5C8 | #CE0000 #FF0000 #FF7500 #FFC16E #FFFFFF | H colour, M role |
| Shot trail gold | strip 9414 x tint (5, 3.99, 1.66) | #000000 #FACD84 #FCE694 #FAF09F #FCF6D6 | H |
| Shot trail violet (twin) | strip 941E x tint (5, 1.29, 1.29) | #5200DB #6500FF #8927FF #A559FF #F7F3FF | H colour, M role |
| **Tracer gold** | 1A80 beams r4 (HDR 2) | #FFD532 #FFE336 #FFFF80 #FFFFD2 #FFFFFF | H |
| Tracer red-violet (twin) | 1A80 beams r4 | #FF3232 #EB37D4 #D73EFF #FFC4FF #FFFFFF | H colour, M role |
| **Heal hit gold** | 1A18 burst r1 (HDR 2.8) / glow / sparks (tint x20) | #FFF89E -> #FFFFFE / #FFC13E #FFD162 #FFDF7B #FFEC8E #FFF89F | H |
| **Harm purple** | 19FC lens discs r4 (HDR 10) | #341DB0 #CA3BFF #FF4EFF #FF7FFF #FFA1FF | H |
| Harm bubbles | 19FC spheres r3 / r4 | #4B00A5 -> #C460FF / #A5001E -> #FF60C9 | H values, M rows |
| Impact sparkle | 1A78 64B91 r4 (HDR 7, falls) | #FFFFFF #FF9CFF #D414FF #7839C4 | H |
| Impact flares | 29F7 x strip 9414 (tint 5) / strip 8FF5 (tint 10) | gold #FACD84..#FCF6D6 / #5200AD #7823DB #6D70FF | H, M twins |
| Impact decals | 4C0C r2 (HDR 5) | #FFB2FF violet / #FFFFFF -> #D6B27F gold | H |
| **Grenade trail** | 19FB r4 / r5 | #000000 #A200D0 #C93DF5 #CC6DDB #F5DD45 / ... #D70AE2 #FF1E1E | H, M twins |
| Burst gold dome | 1874 C31 r5 | #000000 #BC9400 #E2BC55 #F1D583 #FFEBA3 | H |
| Burst violet shell | 1874 C39 r2 (twin C49: -> #FD2626) | #000000 #5F177B #8324A9 #9E2DCA #FDE526 | H |
| Burst splash | 2B697 r4 (HDR 1.1) / gold r4 (HDR 3) | #E598FF #9400C7 #9400C7 #E496FF / #FFFFDD #FFBD00 #FFFFDD | H |
| Burst droplets | 1F35 r1 | gold #FFBD00 -> #FFFF00 (HDR 10) / violet #8F00B9 -> #C500FF | H |
| Burst splat decal | 2B697 r1 (HDR 10) | #9000B7 #CD41F4 #E69BB5 #FAC955 #FFFFFF (twin -> #F57175) | H |
| Burst glows | 4C09 r0 6 m / 7 m | #C800FF #D9AADB #EDE59E #FFED13 / #C800FF -> #FFC900 | H, M twins |
| **Boost plus** | 19F5 E0AA r4 | #FFE892 #FFED9E #FFF2A9 #FFF7B3 #FFFFFF | H |
| **Anti-heal plus** | 19F4 E0AA r1 (falls) | #B200F4 #B035EA #AE4BDF #AC5BD3 #AA69C7 | H |
| **Sleep blue** | dart glow 4C09 r1 (HDR 10); trail 1A7D C8 r1 #87C2FF -> #879CFF (HDR 20) | #3282FF #48B3FF #58D7FF #7BFFFF #99FFFF | H |
| Sleep trail orange (twin) | 1A7D C6 r1 (HDR 21) | #FF7833 -> #FF5233 | H colour, M role |
| Dart hit | 1A7C sparks tint (1.4, 2.51, 5) blue; over life #FF71FF -> #FF8400 (HDR 8.3) | | H |
| **Zs** | 1A58 98DA over life (12BB r3, HDR 1.3) | #000000 #9A8599 #D3B6D1 #FCD9FA #FFF7FF | H values, M reading |
| Sleep sparks | 1A58 1F35 tint (1.22, 3.76, 5.85); over life | #FFFF9D #FFFFA6 #FFE593 #FFB26B #FF0000 (HDR 5.7) | H |
| Sleep vignette | D8F r0 (screen) | #0000FF #CE00FF ... | H, L role |
| **Local Anesthetic** | 7DBF dome r0 / gas r7 / drops r3 | #000000 #402BC4 #73A1EE #74D9FE #26FDFD / #000150 #4A76D2 #65A4F1 #A2D7FF #D1F6FF | H |
| LA twins | dome r1, drops r4 | #C42B57 -> #FD2626 / #9F2C4C -> #FF87DA | H, M role |
| Numbed target | 3D2F wisps r1 (HDR 2) / streaks r1 | #D7FFFF #4FF7FF #0486FF #0F6DFF #52648F / #ECFFFF #84E8FE #20C0FF | H |
| **Nano blue** | 1A16 shells r4 / flames r5 (HDR 6) / body r4 (HDR 5) | #1125FF #2D6EFF #3D93FF #78B1FF #9ECAFF / #1125FF -> #FFFFFF | H |
| Nano red (twin) | 1A16 r4 / r5 | #BC0000 #FF0000 / #FF1111 -> #FFFFFF | H values, L role |
| Nano orange | syringe beam tint (11.3, 2.96, 0.74), strip 5DA3, launch puffs over life | #580000 #B30000 #FE0500 #FE4B00 #FEFEB1 / #9E0000 -> #FFF800 | H |
| Nano beam blue | 1A5F r2 (HDR 3), strip 6853, tint (0.61, 2.96, 12.7) | #0000FF #6077FF #8DAFFF #BEEAFF #FFFFFF / #5245FF #2182FF #00C1FF #00E8FF #DEFFFF | H |

## Biotic Rifle

| Part | Effect | Conf |
|---|---|---|
| Unscoped shot fired | **19FE** on the rifle hp 0006 (0B94 #179 / #549): sound only | H |
| **Unscoped muzzle flash** | **19F8**, the shot 15B7's `m_A18274A3` slot, on hp 0006 + sub-effect 4010 (sound) | H slot, M role (Mercy's) |
| **Unscoped dart in flight** | the model **11741** (shot entity 15B7) + child 15B8 -> **19F7** (trail); fly-by 1A07 (sound) | H |
| Unscoped hit, a hero | **1A78** (`m_AED73588`) + **1A95** (`m_707DF34C`, L which case) | H slots, M roles |
| Unscoped hit, the world | **1A79** (`m_BF7914D4`): flares + star decal 3 s | H slot, M role |
| Scoped shot fired | **1A92** on hp 0006 (#145 / #546): sound only | H |
| **Scoped hitscan** | entity 1615's own effect **1A80**: muzzle flash + **tracer beams**; same hit slots 1A78 / 1A95 / 1A79; 1A8C sound | H |
| **On the ally hit** | **1A18** on the ally's hp 0001 (0B95 #207, held for the 0.6 s status) + sounds 1A14 / 1A88 / 1A43 / 1A36 | H |
| **On the enemy hit** | **19FC** on the enemy's hp 0001 (0B95 #201, held) + sounds 1A89 / 1A37; Headhunter crit 7C77 (sound) | H |
| Scope in / out / held | 1A2F / 1A30 / 1A87: sounds only | H |
| **Scope overlay** | UI asset **0613.116** (0B94 #278, from v169 >= 0.8, local first-person player only): **no art found** (below) | H node, L art |
| Reload | 3P clip 3CE3 -> 1D40 (crouched 3CA0 -> 1D82): the **vial model 3677** in her left hand (hp 00A5) at 0.167 s for 0.233 s; rifle clip 3CE4 / 3D87 -> 1D3C / 1D72: the vial on the rifle's hp 017E from 0.4 s for 1.1 s; the rest sounds | H |
| Rifle children | 2860 (2D33) / 2B1E (3024): 0.033 s, empty | H |

**Unscoped muzzle flash, 19F8 (1 s; every emitter once at 0) (H):**
- **Bulbs:** the hemisphere 963D at size **0.07** (life 0.1 s) and **0.4** (0.79 m across, life 0.14 s), moving 0.5 m/s
  along the shot, masks 299E4 x 299E3 noise x B08 flow, **gold** (ANA_MUZZLE, HDR 2) and the red twin.
- **Flares along the shot** (on hp 0006 +5.45 cm, turned 90°): a soft glow 4C09 at size 0.25-0.35 (M: `mode 4`), two
  210FD glows (`lucio_glow`) **0.1-1.0 m**, stretched x7.5, life 0.22 s, tint (4, 2.36, 0.35) = warm gold (HDR 4).
- **Double flames** 2BDEF (a two-sided flame streak, 512 x 128): 0.1-0.5 m, 0.2-2 m, 0.2-2 m, stretched x7.5, life
  0.18-0.25 s, gold (r5 #977300 -> #FFF2D4) / red twin.
- No light. 1P: the rifle fire clips 3B8D / 3C9A / 3C3F hold only sounds.

**The dart in flight (H):** the model **11741** (a dart 3.2 cm thick, 16 cm long) at **125 m/s** (map), and **19F7** (1 s
loop): two **cross ribbons** (07F5) **0.03 m wide**, 60 points a second, each point living **0.1 s** (12.5 m behind it),
mask 18A1 (`cat_rope`: a soft line), coloured by a strip: **gold** 9414 x (5, 3.99, 1.66) and the violet twin 941E x
(5, 1.29, 1.29) (M twins: the gold one has the `dataflow 6B` slot). Sound 3847.

**Scoped hitscan, 1A80 (1 s) (H):**
- **The tracer:** quads (0801) from the start (ECIN 0x48: the muzzle, M) to the end (ECIN 0x54: the hit point, M), life
  **0.75 s**, masks B35 (a soft line across, 32 x 512) x 2A063 (dark at both ends, bright in the middle): **gold 0.2 m +
  gold 0.15 m** (ANA_TRACER, HDR 2), twins red-violet 0.08 m + 0.15 m. Fade over the 0.75 s (M: the curve rows).
- **Its muzzle:** the bulbs 963D at 0.4 (gold / red, life 0.14 s), two 210FD flares 0.1-1 m x7.5 (life 0.22 s), two
  2BDEF double flames 0.2-2 m x7.5 (life 0.18 / 0.25 s), all on hp 0006 +5.45 cm, aimed at the end point. Sound 4010.

**Hit on a hero, 1A78 (1 s) (H):** a sparkle 64B91 (a 12-frame star ring) **0.03-0.05 m** stretched x4, life 0.35-0.65 s,
ANA_IMPACT pink-violet (HDR 7); two flares 29F7 (`lucio_spark`) **0.40-0.42 m**, life 0.2 s, one with the violet strip
8FF5 (tint 10), one with the gold strip 9414 (tint 5) (M twins); a splash 8FE3 (10 frames) 0.19 m stretched x3, rising
1 m/s, life 0.22-0.28 s, gold strip. **1A95:** the two flares only. **1A79 (world, 3 s):** one flare, the splash, and a
**star decal** 4C0C (0.35 x 0.35, 0.1 deep) for 3 s, violet (HDR 5.3) / gold (HDR 5.1) twins.

**On the ally hit, 1A18 (4 s; all at the hit, on their hp 0001) (H):**
- A **burst** 4B64D x noise 04DB, 2 of them **1.5-2 m**, life 0.75-0.9 s, ANA_HEAL_BURST pale gold (HDR 2.8).
- A **soft glow** 4C09 **1 m**, life 1 s, ANA_HEAL_GLOW amber -> pale yellow (tint x5).
- **10-15 sparks** 0242 (`mercy_glow`) 0.37-0.57 m, stretched x2.5-3.5, **4.3 m/s**, life 0.2-0.43 s, gold (tint x20).
- Sound 3803.

**On the enemy hit, 19FC (4 s; held while the 0.6 s status runs, M) (H):**
- **Swirling lenses:** the lens disc C943 with the caustic web 1CB5D (`mercy_web`) x 18A1, **3-5 a second** plus 1, life
  0.5-0.9 s, **0.31 -> 0.99 m** across (size 0.154 -> 0.5 by dataflow 5FD, L: the damage share), ANA_HARM purple (HDR 10);
  a darker twin.
- **Bubbles:** sphere DBF with 1CB5D, **5-9 a second**, life 0.5-0.75 s, 0.2 -> 1 m (by 5FD), violet / crimson rows.
- **Drips:** the 2-frame blob 1F56 x 04DB, 1 a second plus 1-3, 0.54-1.5 m/s downward in a narrow cone, 0.02-0.07 m,
  stretched x1.3-2.2, life 0.75-1.15 s, yellow-green tint (L colour).
- Two lights (2.5 and 2). Sound 388D. Local Anesthetic's numbness plays this too (6123 #382).

**The scope overlay (L):** 0B94 #278 shows UI asset **0613.116** while the scope is at least 80% in (0.2 s after the press;
local first-person player only). `fx_ana_scope.py` read it (`fx_scope_stu.json`): an `STU_35DC2BEA` with base 05BE.116
and children (0A82.116, data bindings E615 / E77D / 9C89 .01C) and **no texture or model reference** at any depth. The
reticle and black surround are drawn by the UI system from assets this walk doesn't reach. Her GUI export (12 images,
`fx_sheets\gui_ana.png`: portraits, the three ability icons 9B36 Zs / 9B37 grenade / 9B38 syringe, the rifle's kill-feed
silhouette) holds no scope. **Not found.** The 1P rifle is hidden while fully scoped (map, M).

**Recipe:**
- Unscoped fire (third person; skip in first person, `rendering.md`): at the muzzle the gold bulb (the data's 0.79 m
  hemisphere; ours: a `lucio_glow` sprite 0.5 m, ANA_MUZZLE_N, life 0.14 s) and two thin gold
  streaks along the shot (`lucio_glow` as Shape.STREAK with `axis(aim)`, 0.3 x 1 m, life 0.2 s, ANA_MUZZLE_N) plus the
  double flame (`ana_muzzle_flame`, 1 m long, 0.25 m high, life 0.2 s). Additive: gold over bright ground washes out,
  blend it (`Fx.soft`) if it does.
- The dart: its model 11741 (as an extra, 0.16 m long, centred 0.088 m back along its axis) or a 0.15 m gold
  `lucio_spark` streak, with a `Fx.trail` of `cat_rope`, a gold strip palette (ANA_SHOT_TRAIL below), **0.03 m wide**
  (ours: 0.06 m so it reads at Minecraft distances), 0.1 s long (12.5 m at 125 m/s).
- Scoped fire: the same muzzle, then a gold band from the muzzle to the hit point (MercyFx's `band` pattern: Shape.STREAK
  at the midpoint, `axis(piece)`, `size(len, len)`, `aspect(width / len)`), **0.2 m** `ana_beam_line` x `ana_fade_v`
  (bake them as a PRODUCT: same UVs) with ANA_TRACER_N, plus a 0.15 m one, fading over **0.75 s**.
- Hit on an ally: on their chest a 1.5-2 m pale gold burst (`ana_burst`, ANA_HEAL_BURST_N, blended), a 1 m amber glow
  (`cat_soft`, ANA_HEAL_GLOW) and 10-15 gold sparks (`mercy_glow` as STREAK, 4.3 m/s, 0.2-0.4 s).
- Hit on an enemy: for the 0.6 s status a few purple swirling discs (`mercy_web` on Shape.FLAT facing the camera, 0.3 ->
  1 m, ANA_HARM_N, 3-5 a second) and purple bubbles (0.2-1 m, ANA_HARM_BUBBLE), 1-3 drips falling.
- Hit on a block: a 0.4 m gold flare (`lucio_spark`) and a 0.35 m gold star decal (`ana_star_decal`, Shape.FLAT on the
  face, `lift`) fading over 3 s; on a creature, the flare and a few tiny violet sparkles (`ana_sparkle`, 12 frames).
- Scope overlay: ours (a HUD texture: a circle with a crosshair and a dark surround), shown from 80% scoped (0.2 s).

## Sleep Dart

| Part | Effect | Conf |
|---|---|---|
| Cast | **1A00** (0BDD #114): sound only; clips 1D4B (3P) / 1CDD (1P): sounds; the pistol model 1175E on hp 0143 / 00A6 | H |
| 0.7 s | 5335 (#163): empty | H |
| **Muzzle** | **1A7B**, the dart's `m_A18274A3`, on the pistol's hp 0006 + 2 cm (+ 4010 sound) | H |
| **In flight** | the dart model **11740** + child 1614 -> **1A7D**; fly-by 1A8D (sound) | H |
| **Hit** | **1A7C** (`m_BF7914D4`, the only impact slot: walls and creatures) | H slot, M both |
| **Asleep** | **1A58** on the sleeper's hp 0012 (head) and 0001 (0BDE #246 / #337, held) + **1A1F** (sounds, 5 s) | H |
| Sleep start | 67BF (224E #564): sounds; the sleeper's fall / wake clips 1D6C / 1D6D: camera shakes + sound 92EE | H |

**Muzzle, 1A7B (H):** three flashes of the star flare 11DF2 (`mercy_star`) at **0.2, 0.7, 0.7 m**, stretched x7.5 (M:
along the shot), life **0.03 s** (one frame), pink-red (#FF000A -> #FF6281, HDR 4) and orange (#FF4400 -> #FF8971) rows
x a gold tint (4, 2.36, 0.35).

**In flight, 1A7D (3 s loop) (H):** the dart model 11740 (9.4 cm, 60 m/s, map); two **cross ribbons** (07F5) **0.05 m
wide**, 60 points a second, living **0.30 s** (18 m behind) and **0.15 s** (9 m), mask 1887E (a soft band bright across
its middle, 128 x 16), one **orange** (HDR 21), one **blue** (#87C2FF -> #879CFF, HDR 20) (M twins: the blue one has the
`dataflow 6B` slot, as the rifle's gold one); a **soft blue glow** 4C09 **1 m**, 4 a second, life 0.5 s, ANA_SLEEP_GLOW
navy -> blue (HDR 10), red tint twin.

**Hit, 1A7C (3 s) (H):** **10 sparks** (122C `lw_disc`) at **15-17 m/s** in a 9° cone off the surface, 0.05-0.07 m,
stretched x4, spinning, life 0.23-0.62 s, blue tint (1.4, 2.51, 5), over life rose -> red (HDR 8.3); twin red (HDR 38);
a **star burst decal** 3B80 0.35 x 0.35 for 2 s, random roll, blue (1.5, 2.6, 6) / red twin.

**Asleep, 1A58 (8 s, held for the sleep: 5 s, 3.5 on tanks) (H values, M reading of the 12BB rows):**
- **Zs:** the Z mask **98DA** (a white Z with a soft edge, 128²) x 18E48 (`cat_cloud`), on quads (0801), **1.85 a second**
  plus 1 at the start, each living **3 s**, spawned at the head (hp 0012, M), turned ±10°. Over their life: size **x0.86
  -> x7.8** of the spawn 0.042 (so about **0.04 -> 0.33 m**), drifting **(-0.56, +1.36, -0.55) m** (up 1.4 m and off to one
  side), colour **ANA_SLEEP_Z** black -> lilac -> white (HDR 1.3), alpha from the colour row (fading in from black, M).
- **Hit flash:** 2 star flares 3737 **1.2-1.5 m**, life 0.12-0.13 s, blue-white tint (1.9, 2.51, 5).
- **Spark burst:** **50 sparks** (1F35 = `spark`) **11-18 m/s** in every direction, 0.07 m stretched x5.7-6.8, life
  0.49-0.68 s, white-blue tint, over life pale yellow -> orange -> red (ANA_SLEEP_SPARK, HDR 5.7).
- **Drowsy haze:** puffs 5429 (4 frames) x B08, 5 a second for the first half then fading, 0.46-0.73 m, life 0.75-1.25 s,
  and 25 a second (life 1.5-2.5 s): faint olive-gold (peak 0.16; over life red -> yellow). Faint (M: barely visible).
- **The sleeper's screen (L):** rising Zs 10 a second plus 5 (`ECIM(40)`), a purple vignette D8F (`mercy_rim`, inverted:
  dark centre) for 8 s and a 0.5 s purple flash.
- No light. No wake effect (0BDE #231 sends 065D, map).

**Recipe:**
- Cast: nothing but sounds and the pistol in her right hand; at the shot a one-frame gold-pink flash at the pistol's
  muzzle (`mercy_star`, 0.2-0.7 m, life 0.03-0.05 s; skip in first person).
- The dart: its model 11740 (an extra) or a small blue `lucio_spark`; a `Fx.trail` 0.05 m wide (ours 0.08) 0.3 s long
  (18 m at 60 m/s), `ana_dart_band`, blue (`ANA_SLEEP_GLOW_N`, additive: blue reads well added), and a 1 m soft blue glow
  (`cat_soft`) every 5 ticks.
- Hit: 10 blue sparks (`lw_disc` as STREAK) 15 m/s off the surface, a 0.35 m blue star decal (`ana_star_burst`, FLAT,
  `lift`) for 2 s.
- The sleeper: one Z every 0.54 s from the top of their head (`ana_z` as a camera-facing sprite, ANA_SLEEP_Z by age), 0.04 m
  growing to 0.33 m, rising 1.4 m while drifting 0.6 m to one side over 3 s, fading out at the end (ours: fade 0.2 / 0.3);
  at the moment they fall asleep a 1.3 m blue-white flash (`ana_star`) and 50 sparks (`spark`, ANA_SLEEP_SPARK by age,
  11-18 m/s, blended: 50 additive sparks on one point sum to white). The sleeping player's own view: a purple screen-edge
  tint (HUD, ours) and Zs rising on the screen.

## Biotic Grenade

| Part | Effect | Conf |
|---|---|---|
| Throw | 19FA on hp 0002 (0B97 #68): **empty**; 19FF (#93) / 6FC5 (#196): sounds; clips 1D01 / 1CEC: camera shake + sound | H |
| **In flight** | the grenade model **11763** (clip AE8, L spin) + child 15BC -> **19FB** | H |
| **Burst** | **1874** (the grenade's `m_6CB564CC`, also 5791 #38) | H |
| Second slot | **647D** (`m_45FA9F53`): the same burst, smaller (L: the Biotic Bounce burst, or a burst without a hit) | H content, L role |
| **Ally boosted** | **19F5** (0B99 #225, held for the 3 s) + **5485** (#274: sounds only) | H |
| **Enemy anti-healed** | **19F4** (0C72 #227, held for the 3 s) | H |

**In flight, 19FB (1 s loop) (H):** an **8-sided tube ribbon** (099F) **0.12 m wide**, 60 points a second, each living
**0.5 s** (up to 15 m behind at 30 m/s), from hp 0004, masks 1EF26 (a 4 x 256 line, varying along the ribbon) x noise
299E2, **ANA_GRENADE_TRAIL** black -> violet -> gold (twin -> red). No glow, no light: the grenade itself (10 x 15 cm) carries it.

**Burst, 1874 (2 s; the burst point) (H):**
- **Domes:** the hemisphere 963D at **size 3.5 (6.9 m across, 3.5 m tall)**, life 0.5 s, noise 299E2 x 299E1 x B08,
  **ANA_BURST_DOME** gold; at **size 4.0 (7.9 m: the 4 m radius)** twice, life 0.4 s, flat mask 1298, **ANA_BURST_SHELL**
  black -> violet -> yellow; at 4.0 (life 0.5 s) and two at 2.5 (4.9 m, from 0.167 s, life 0.8 s), noise-masked, red ->
  ochre rows (L colour rows: parameters, M).
- A **sphere** 1385 at **4.5 (4.4 m)**, 2, life 0.4 s, ANA_BURST_SHELL_ENEMY violet -> red (M: the twin).
- **Splashes** (from 0.1 s): the 3-frame splatter 2B697 x 299E3 at **7 m** (1 + 3, 3 m/s), **6 m** (5 m/s), stretched x2,
  up to 90° off vertical, life 0.4 s, **ANA_SPLASH** lilac-violet and **ANA_SPLASH_GOLD**; **tendrils** 2BECA x 299E6 on the
  hung quad DA82 at 6.5-7.5 m rising 5 m/s, life 0.4 s, gold (HDR 10) with pink.
- **Droplets** (from 0.1 s): **33 gold** (1F35 x flow 1FB81, 0.03-0.05 m, stretched x1.5, **6-12 m/s**, up to 180°, life
  0.5-0.6 s, ANA_DROP_GOLD HDR 10) and **33 violet** (life 0.6-1.1 s, within 95°, ANA_DROP_PURPLE).
- **Glows:** 2 soft glows 4C09 at **6 m** and 2 at **7 m**, life 0.33 s, violet -> yellow (ANA_BURST_GLOW) / violet ->
  orange twin.
- **Ground splat decal** 2B697 from 0.1 s for 0.8 s, half extents **1.06 -> 2.02 m** (M: a 4 m splat), 10 deep,
  ANA_BURST_DECAL dark violet -> gold (HDR 10) / -> red twin.
- **Two lights** rising 1 -> 3.5 over 0.53 s, +1 m up. Sounds 387A, 384E, 384F.

**647D (2 s) (H):** from 0.1 s: domes 3.5 / 4.0 (life 0.4 / 0.5 s), splashes 4.5 / 5 / 4.5 m, the tendrils, **16 + 16**
droplets, one 6 m glow from 0.167 s, the decal twins, the lights, the same sounds: a smaller copy of 1874.

**Ally boosted, 19F5 (5 s loop, held while the +50% lasts 3 s) (H):** **plus signs** (mesh **E0AA**, 7.2 cm) at size 0.8
(**5.8 cm**), stretched **x2.5-3** upward, **20 a second**, rising **0.51-0.65 m/s**, life **0.3 s**, from the body (shape
S0 `ESGD`: M the body surface), masks 27E5E soft plus x 3A0DE framed plus x 27E5D frame x 07A9 fade, **ANA_BOOST_PLUS**
pale gold -> white. Nothing else (no screen, no light).

**Enemy anti-healed, 19F4 (5 s loop, held for the 3 s) (H):**
- **Broken pluses:** mesh E0AA (masks 391F2 slash), **3-6 a second** (by 5FD, L) from each of three emitters (hp 0001,
  0019, 001A: M), life 0.82-1.02 s, drifting **down** 0.57-0.71 m/s (M: the direction rows run -0.03 .. -0.32 in y),
  size 0.7-2.6 shrinking to 0.5-1.0 (5-19 cm -> 4-7 cm, M: `mode 1` keys), stretched x1 -> x5 over life, spinning
  0.2-0.5 rad/s, **ANA_ANTIHEAL** violet -> lilac (fades from bright).
- A **dim violet shell** (963D, masks 299E1 x 299E9 x A7B6), ANA_ANTIHEAL_AURA (peak 0.28): a faint aura.
- **The victim's screen (L):** a vignette 05D0 (inverted radial: bright corners) with the violet strip 8FF5 for 5.8 s,
  a ring 6429 x 04DB, falling broken pluses (E0AA, 2FF91 stripes) 10 a second. Sounds 5DDC, 387F.

**Recipe:**
- Throw: no flash (the data has none); the grenade model (11763, an extra, 0.15 m) spinning (ours) with a `Fx.trail`
  0.12 m wide (`mercy_band`, the tube mask `LifeweaverFx.TUBE` uses), 0.5 s long, ANA_GRENADE_TRAIL (violet at the head
  to gold at the tail, ours: palette by age), blended.
- Burst (at the hit point; size it to the 4 m radius): a gold dome (Shape.DOME **6.9 m**, `cat_noise`, ANA_BURST_DOME,
  blended at 35%, life 0.5 s, easeOut) and a violet-to-yellow shell (DOME **7.9 m**, life 0.4 s, faint); at 0.1 s four
  violet splashes (`ana_splat`, 3 frames, 5-7 m, FLAT facing up-ish, life 0.4 s) and one gold; 33 gold and 33 violet
  droplets (`spark`, 6-12 m/s, gravity, life 0.5-1.1 s; ours 15 + 15); two 6-7 m glows (`cat_soft`, ANA_BURST_GLOW, 0.33
  s); a 4 m violet-gold splat on the ground (`ana_splat` FLAT, `KirikoFx.groundY`, 0.8 s); a light flash (ours).
- On each boosted ally for 3 s: tiny pale-gold pluses (`mercy_plus` on a 6 x 17 cm streak, or the E0AA mesh) rising off
  their body, **one a tick** (20 a second), 0.3 s each.
- On each anti-healed enemy for 3 s: violet broken pluses (`mercy_plus` with `ana_slash`, or ours: a plus crossed by a
  slash, ANA_ANTIHEAL) drifting down, one every 3-6 ticks, ~1 s each, growing x5 tall as they fade; a faint violet aura
  (ours: skip). If the player is the victim: a violet screen-edge tint (HUD).

## Nano Boost

| Part | Effect | Conf |
|---|---|---|
| Cast on Ana | **1A12** (0B9E #147, at 0.13 s): sounds only; clips 1CFF (3P) / 1CFA (1P) / 1D71: sounds, empty | H |
| **Launch** | **1A5E** (the syringe 15E9's `m_A18274A3`) on her **hp 0078** (and 0075) | H |
| **The beam** | **1A5F** (the syringe's own effect): bands from her to the ally | H, M endpoints |
| Fly-by | 1A39: empty | H |
| **The boosted ally** | **1A16** (0B9F #215 / #294: others' view, M) or **7A23** (#324 / #326: their own view, M), held 8 s; **1A5B** (#229 / #290) for 2 s at the start; 5FA8 / 5FAA sounds | H |
| No target | 0186: sound | H |

**Launch, 1A5E (H):** on hp 0078 (her left forearm): **10 puffs** (the BC7 smoke flipbook 9EF, 4 frames, a colour sprite)
0.17-0.23 m stretched x2, drifting 0.13-0.63 m/s within 5° of the throw, life 0.2 s (and 10 more 0.4-0.62 m, 0.74-4.5 m/s),
over life dark red -> yellow (ANA_NANO_MUZZLE); **lens streaks** 5E9E (a horizontal flare, 512 x 256) **0.31-0.36 m** and
**1.8-1.86 m**, life 0.2 s, the blue strip 6E24 and the orange strip 5DA3 (tint x5); **10 glows** 0242 **0.71 m**, life
0.14 s, blue (0.2, 1.2, 5) and orange (4.53, 1.2, 0.29); **5 + 6 sparks** 29F7 0.01-0.04 m at 2-16 m/s, life 0.08-0.18 s,
blue (0.67, 2.82, 8.19). Blue and orange together (M: layers, the nano look; not twins).

**The beam, 1A5F (1 s) (H values, M endpoints):** flat bands (0801) from input 0x27 (M: her hp 0078) to input 0x2C (M:
the ally's hp 0001):
- **Cores 0.16 m:** masks 050C x 0510 (thin soft lines), one **orange** (tint 11.3, 2.96, 0.74: HDR 11), one **blue**
  (0.61, 2.96, 12.7: HDR 12.7), life **1 s**.
- **Glows:** 9ACC (a soft line, 32 x 256) x noise 05DF at **1.75 m** and **0.75 m** with the orange strip 5DA3, and
  **1.75 m** with the blue strip 6853, life **2 s**, alpha fading 1 -> 0 over life; **1.03 m** ANA_NANO_BEAM blue -> white
  (HDR 3), life 2 s.
- Two 1 m distortion bands (9A6 BC5): heat haze, skip.

**The boosted ally, 1A16 (8 s) (H values; team rows M):**
- **Body glow:** a material effect (noise 04DB, ANA_NANO_BODY navy -> pale blue, HDR 5; red twin).
- **Licking flames:** the 4-frame flame 7ADD x 16E9D x 52BC (blue strip) on edge-anchored quads (0E21) from the body
  surface, **30 a second**, 0.65-0.75 m, life 0.12-0.2 s; body sparks 1F35 **80 a second**, 0.05 m, stretched x0.3-0.7,
  life 0.07-0.16 s, blue tint (1.1, 1.71, 6.66).
- **Rising flame tongues:** 963D with 7ADD x B08 x 299E4, **10-12 a second**, 0.26-0.61 -> 0.11-0.14 (0.5-1.2 m), stretched
  x3, 2-3 m/s, spin 10, life 0.18-0.38 s, **ANA_NANO_FLAME** navy -> pale blue (HDR 6); red twin.
- **Long shells:** 963D (299E1 x 299E9 x A7B6) **14 -> 20 a second** (by 5FD, L), 0.36-0.73, stretched **x8-10**, spin
  4-6, life 0.89-0.99 s, upward, **ANA_NANO_BLUE**; red twin; at the start (0-0.5 s) a burst of 8 -> 16 more (x4-6).
- **Sparks:** 3 x 30 a second (1F35), 4-8 m/s, 0.03-0.05 m, stretched up to x5, spin 4-5, life 0.2-0.45 s,
  **ANA_NANO_SPARK** (HDR 6); red twin.
- **A ring** 1BAC (3.7 m) with 2B30 x 1BA5 x 2690, blue tint (1.1, 1.32, 6.66), 1.5 a second, life 1 s, `ECIM(40)` (L: the
  boosted player's screen, or the ground).
- Four lights 2 -> 3 (by 5FD), two only for the first 0.5 s. Spawn shapes `ESGD` / `ESGC` (M: the body's surface and
  bones, hp 0001 / 0019 / 001A / 000E).
- **Own view, 7A23 (8 s):** the material glow, the ring, the body sparks and licking flames only (blue).
- **Start, 1A5B (2 s, hp 0001):** a fountain of **50 sparks a second** (1F35 with the blue strip 73A1) 0.09-0.43 m,
  0.5-1.4 m/s up within 50°, life 0.18-0.46 s; heat-haze streaks (18F81, 384F: skip).

**Recipe:**
- Cast: at 0.13 s from her left forearm (hp 0078; ours 0.4 m left, 1.18 m up, forward of her) an orange-and-blue flash:
  a 1.8 m horizontal lens streak (`ana_lens` along the throw, orange strip palette), a 0.7 m blue glow (`mercy_glow`), a few
  orange-to-yellow smoke puffs (`ana_smoke`, NATURAL colour sprite, ANA_NANO_MUZZLE by age), 5-10 blue sparks.
- The beam: bands (`band` pattern) from her forearm to the ally's chest: a 0.16 m blue core and a 0.16 m orange core
  (`ana_nano_line`, ANA_NANO_BEAM_N / an orange palette) for 1 s, a 1 m blue glow band (`ana_nano_core`, ANA_NANO_BEAM) and
  a 0.75 m orange one fading over 2 s (ours: half the data's widths: 1.75 m reads as a wall).
- The boosted ally for 8 s: a blue body glow (ours: a translucent blue `rim` on their model, or a body-sized blue
  `cat_soft` sprite at 30%), 1-2 blue flame tongues a tick (`ana_flame`, 4 frames, 0.5-1 m, rising 2-3 m/s, ANA_NANO_FLAME_N),
  a long blue streak (`glow` as STREAK, 0.4-0.7 m stretched x8, life 0.9 s, ANA_NANO_BLUE) about every 1-1.5 ticks, blue sparks off the body (`spark`, 4-8 m/s), all
  additive (blue reads well added); a burst of 8-16 streaks and 2 s of a blue spark fountain at the start. Their own
  first-person view: a blue screen tint (HUD) and the sparks only.

## Perks

| Part | Effect | Conf |
|---|---|---|
| **Local Anesthetic**: the dart | 7773 = the sleep dart (same model, 1A7B / 1A8D / 1A7D) with impact **7DBF** in both slots | H |
| Splash (4 m, 0.15 s after the hit) | **3D2F** (51F0 #485) on each enemy in range + 6123 on them: **3D2F** (#378), **19FC** (#382, the enemy hit's purple), 1A37 / 7E70 / 7E60 (sounds) | H |
| **Biotic Bounce** | no effect of its own found; the grenade's second slot **647D** (L: the bounce burst) | L |
| Speed Serum | none (no effect node in 51F0 / 0B9F's #314-#317) | H |
| Headhunter | 7C77: sound only | H |
| Stadium (not perks) | 708A / 7089 (Falconer): sounds | H |

**Local Anesthetic's burst, 7DBF (1 s, at the dart's hit) (H):**
- A **dome** 963D at **1.75 (3.5 m across)**, 2, life 0.3 s, flat mask, **ANA_LA_DOME** black -> blue -> cyan (twin red).
- **Star flares** 3737 1-1.32 m, 2, life 0.2 s, tint (1.9, 2.51, 5) blue / (5, 1.91, 3.43) pink.
- **Swirls** 39B71 (2 frames) on quads, 10 at 0.75-1.25 m and 10 at 0.05-0.5 m, 2.5 m/s outward and flat, spin -5,
  life 0.5-1.25 s, pink-magenta rows.
- A **flattened sphere** 15CAE at size 2 (**1.94 x 1.18 m**), cracks FFDC x noise, life 0.5-0.7 s, rising 0.1 m/s,
  ANA_LA_SHELL cyan -> blue (HDR 3) / pink twin.
- A **splash** 2B697 0.5-5.7 m at +0.62 m, stretched x2, 1 m/s, life 0.6 s, blue / pink rows.
- **33 droplets** (1F35 x 1FB81) 0.05-0.15 m, **3-6 m/s** up to 180°, life 0.5-0.6 s, ANA_LA_DROP blue -> cyan / pink.
- **Gas:** 5 clouds (the 32-frame 1FC65 x 299E1 x B08 x 4C09 x 19167) **3 m**, **20 m/s** outward, life 0.69-0.85 s,
  **ANA_LA_GAS** navy -> pale blue.
- A **range sphere** DBF at **size 8 (7.9 m across: the 4 m radius)**, mask 05D0 (rim) x 19167, life 0.76-0.89 s, dim
  blue (peak 0.55, M).
- A light 1 -> 4.5 over 0.53 s at +1 m. Sounds 104AA-104AD.

**The numbed enemy, 3D2F (1 s loop, held for the 3 s, on hp 0001) (H):** **wisps** 18B18 (`cat_puff`) 0.2-1.2 m, **40 a
second** for the first 0.5 s of each loop, drawn **inward** at 1 m/s, spinning 10-11 rad/s, life 0.6 s, **ANA_LA_WISP**
pale cyan -> blue (HDR 2); **vertical streaks** 2C701 (`speed_line`) 3.5-4.5 m tall (a renderer track), 40 a second, life
1-1.1 s, **ANA_LA_STREAK** pale cyan -> blue; a body material effect (cells 2BE4A, cyan, HDR 8). Plus the enemy-hit purple
19FC on them.

**Recipe:**
- Local Anesthetic's hit: a 3.5 m cyan-blue dome flash (DOME, ANA_LA_DOME, 0.3 s), a 1.2 m blue star (`ana_star`), 20-30
  blue droplets 3-6 m/s (`spark`), 3-5 big blue gas puffs (`cat_soft` 3 m, ANA_LA_GAS, 20 m/s with strong drag, ours) and
  a faint 4 m-radius sphere outline (DOME 7.9 m at 15%, `mercy_rim`) showing the reach.
- Each numbed enemy for 3 s: cyan wisps pulled into their chest (`cat_puff`, ANA_LA_WISP_N, 2 a tick in the first half of
  each second), thin cyan vertical streaks on them (`speed_line`, 2 m, ours) and the purple enemy-hit swirls (19FC).
- Biotic Bounce's second burst: the grenade burst at about 60% scale (647D's sizes), ours.

## Practical notes for Overcraft

- Pre-colour the grey masks with the palettes below. Following `rendering.md`: blend the warm gold and violet-gold
  (`Fx.soft`: gold over grass turns lime added), keep the blues and cyans (sleep, nano, Local Anesthetic) additive; blend
  anything spawned densely in a light colour (the burst's droplets, the sleep's 50 sparks).
- Draw only the friendly twin (gold / blue); the red-violet rows are the enemies' view (M).
- HDR rows (the shot trail tints x5, the tracer, the grenade droplets x10, the nano beam x11-13, the sleep dart's trail
  x20) clamp to white: use the `_N` rows for thin bands and the clamped ones for hot cores.
- Skip the heat haze and distortions (B08 / 9A6 / 18F81 BC5 flow maps, 0B08 is a flow multiplier, not a mask) and the
  material effects (body overlays) unless drawn as a model tint.
- Several masks have no black floor (2B30 69, 0005D0 corner 249, 2FF91, 9EF): don't draw them alone on a quad.
- The screen overlays (`ECIM(40)`) belong in the HUD, not the world (Kitsune Rush's lesson in `rendering.md`).
- Sizes are real Overwatch metres; Ana's eyes are 1.57 m up at rest (map; her stance is lower in play) and Minecraft's
  player's 1.62: most numbers fit as they are; cap the 7-8 m burst shells to faint washes.

## Not found / not certain

- **The scope overlay's art:** 0613.116 holds only UI bindings; no texture is referenced (`fx_scope.txt`). Ours.
- Which twin each team sees (no decoded node; `dataflow 6B` is L the switch); which of 1A16 / 7A23 is which view (M, by
  content); whether the 1A5E / 1A5F orange and blue are both drawn (M: layers).
- The beams' endpoint inputs (`ECIN 0x48 / 0x54`, `0x27 / 0x2C`) and 59DC556C as the width: M.
- Stretch (6DFA) on the muzzle flames (x7.5 on 0.2-2 m sprites reads too long: M meaning), `mode 4` sizes (19F8 C30).
- Dataflow 5FD (the enemy hit's size, the anti-heal's rate, the nano rates: L), the Zs' 12BB rows as size and drift (M).
- 647D's trigger (`m_45FA9F53`: L the bounce burst), 1A95's case (`m_707DF34C`), 0011 / 0019 / 001A / 000E / 0075's bones.
- The grenade's spin (clip AE8, L), the burst domes' orientation (963D is stored as a bowl under y = 0; drawn as a dome, M).
- The default skin theme (1921 by its empty swap list, M).

## Masks (`tools/maskstat.py`)

| Mask | Size | min / max, corner / edge / centre (red) | Flag |
|---|---|---|---|
| **98DA** Z | 128² | 0 / 240, 0 / 11 / 227 | a soft-edged Z: the sleep |
| 2BDEF double flame | 512 x 128 | 0 / 255, 0 / 0 / 25 | two-sided flame streak (muzzle) |
| 8FE3 splash | 128² x 10 | 0 / 246, 0 / 0 / 120 | splash flipbook |
| 64B91 sparkle | 256² x 12 | 0 / 238, 0 / 0 / 231 | small star-ring flipbook |
| 4C0C star | 64² | 0 / 254, 0 / 1 / 252 | star decal |
| 4B64D burst | 512² | 0 / 255, 0 / 2 / 255 | radial burst |
| B35 beam line | 32 x 512 | 0 / 203, 0 / 179 / 159 | soft line across (tracer) |
| 2A063 fade | 32 x 256 | 0 / 255, 19 / 20 / 255 | dark ends, bright middle (floor 19) |
| 1F56 blob | 128² x 2 | 0 / 252, 0 / 0 / 175 | blob with a hole (drips) |
| 2B697 splatter | 256² x 3 | 0 / 239, 0 / 0 / 162 | splash flipbook (burst, decal) |
| 2BECA tendril | 256 x 512 | 0 / 252, 0 / 0 / 28 | thin vertical tendril |
| 1EF26 line | 4 x 256 | 0 / 255, 255 / 255 / 255 | a strip varying along V only (ribbon) |
| 27E5D frame | 128² | 0 / 255, **255** / 255 / **0** | **inverted** (white frame round a black square) |
| 3A0DE framed plus | 128² | 0 / 255, **255** / 255 / **0** | **inverted** (black plus on white) |
| 391F2 slash | 256² | 0 / 255, **255** / 255 / **0** | **inverted** (black diagonal band) |
| 05D0 vignette | 256² | 1 / 249, **249** / 141 / 1 | **inverted** radial (screen edge) |
| 6429 ring | 512² | 0 / 255, 0 / 0 / 0 | thin circle |
| 1887E band | 128 x 16 | 0 / 255, 0 / 255 / 255 | soft band, bright across its middle (dart ribbon) |
| 3737 star | 256² | 0 / 255, 0 / 1 / 255 | star flare with rays |
| 3B80 star burst | 256² | 0 / 253, 0 / 0 / 253 | spiky star |
| 5429 puff | 256² x 4 | 0 / 176, 0 / 0 / 158 | soft puff flipbook |
| 39B71 swirl | 512² x 2 | 0 / 173, 0 / 0 / 115 | wispy swirl |
| FFDC cracks | 256 x 128 | 0 / 255, 0 / 0 / 4 | crack web (a mesh texture) |
| 7ADD flame | 128² x 4 | 0 / 233, 0 / 0 / 230 | flame-tongue flipbook |
| 5E9E lens | 512 x 256 | 0 / 255, 0 / 0 / 255 | horizontal lens streak |
| 9ACC / 0965 / 0510 / 050C | 32-16 x 256 | soft vertical lines (0965, 9ACC bright centre; 050C dark centre) | beam strips |
| 9EF smoke | 256² x 4 | RGB 73-203, **alpha 0-253** | **colour sprite** (BC7, shape in alpha): NATURAL |
| 384F streak | 64 x 128 | 0 / 252, 0 / 0 / 166 | vertical soft streak (heat haze use) |
| 2B30 band | 256² | **69** / 255 | no black floor (a multiplier) |

Already in the pack and used here: `mercy_glow` (0242), `mercy_noise` (04DB), `cat_fade` (07A9), `lw_disc` (122C),
`cat_rope` (18A1), `spark` (1F35), `lucio_spark` (29F7), `cat_soft` (4C09), `glow` / `lw_glow` (A7B6), `mercy_rim` (0D8F),
`mercy_star` (11DF2), `mercy_web` (1CB5D), `cat_puff` (18B18), `cat_cloud` (18E48), `cat_glow_noise` (19167),
`lucio_glow` (210FD), `mercy_plus` (27E5E), `cat_noise` (299E1), `cat_ring_line` (299E3), `speed_line` (2C701), `band`
(2FF91), `cat_cone_line` (16E9D); mesh `cat_sphere` (1385).

## Textures to add (`build_asset_pack.py`, `EFFECT_MASKS`)

With `research\ana\fx` passed to `build_asset_pack.py effects` as a source (every file checked to exist,
`work\fx_palettes.txt`):

```python
    # Ana's (tools/reference/ana-effects.md): the sleep's Z, the rifle's muzzle double flame, its hit splash and sparkle
    # (flipbooks), the star decal, the heal burst, the tracer's line and length fade, the drip blob
    "ana_z": ("Ana", "SleepDart", "0000000098DA", 1),
    "ana_muzzle_flame": ("Ana", "BioticRifle", "00000002BDEF", 1),
    "ana_splash": ("Ana", "BioticRifle", "000000008FE3", 10),
    "ana_sparkle": ("Ana", "BioticRifle", "000000064B91", 12),
    "ana_star_decal": ("Ana", "BioticRifle", "000000004C0C", 1),
    "ana_burst": ("Ana", "BioticRifle", "00000004B64D", 1),
    "ana_beam_line": ("Ana", "BioticRifle", "000000000B35", 1),
    "ana_fade_v": ("Ana", "BioticRifle", "00000002A063", 1),
    "ana_blob": ("Ana", "BioticRifle", "000000001F56", 2),
    # the grenade: the splatter (3 frames), the tendril, the trail line; the boost / anti-heal icon parts (inverted:
    # frame, framed plus, slash) and the victim's screen vignette and ring (inverted / screen: HUD use)
    "ana_splat": ("Ana", "BioticGrenade", "00000002B697", 3),
    "ana_tendril": ("Ana", "BioticGrenade", "00000002BECA", 1),
    "ana_tube_line": ("Ana", "BioticGrenade", "00000001EF26", 1),
    "ana_frame": ("Ana", "BioticGrenade", "000000027E5D", 1),
    "ana_plus_cut": ("Ana", "BioticGrenade", "00000003A0DE", 1),
    "ana_slash": ("Ana", "BioticGrenade", "0000000391F2", 1),
    "ana_vignette": ("Ana", "BioticGrenade", "0000000005D0", 1),
    "ana_ring": ("Ana", "BioticGrenade", "000000006429", 1),
    # the sleep dart: its ribbon band, the hit's star flare and star burst, the drowsy puff (4 frames), Local
    # Anesthetic's swirl (2 frames) and cracks
    "ana_dart_band": ("Ana", "SleepDart", "00000001887E", 1),
    "ana_star": ("Ana", "SleepDart", "000000003737", 1),
    "ana_star_burst": ("Ana", "SleepDart", "000000003B80", 1),
    "ana_puff": ("Ana", "SleepDart", "000000005429", 4),
    "ana_swirl": ("Ana", "SleepDart", "000000039B71", 2),
    "ana_cracks": ("Ana", "SleepDart", "00000000FFDC", 1),
    # Nano Boost: the flame tongue (4 frames), the lens streak, the beam's lines, the launch smoke (a colour sprite:
    # NATURAL), the vertical streak
    "ana_flame": ("Ana", "NanoBoost", "000000007ADD", 4),
    "ana_lens": ("Ana", "NanoBoost", "000000005E9E", 1),
    "ana_nano_core": ("Ana", "NanoBoost", "000000009ACC", 1),
    "ana_nano_line": ("Ana", "NanoBoost", "000000000965", 1),
    "ana_nano_edge": ("Ana", "NanoBoost", "000000000510", 1),
    "ana_smoke": ("Ana", "NanoBoost", "0000000009EF", 4),
    "ana_streak_v": ("Ana", "NanoBoost", "00000000384F", 1),
```

`EffectArt` reads masks from the red channel: `ana_smoke` must be drawn with `Palette.NATURAL`; `ana_frame`,
`ana_plus_cut`, `ana_slash` and `ana_vignette` are inverted (bright where nothing should show): bake them inverted, or use
them only as HUD art. A PRODUCT worth baking: `ana_tracer` = (`ana_beam_line`, `ana_fade_v`) (one quad, same UVs).
Colour strips (gradient textures, as palettes not masks): 9414 gold, 941E violet, 8FF5 indigo, 5DA3 orange, 6853 cyan,
6E24 / 73A1 / 52BC blues (hex in the Colours table).

Meshes worth converting (`EFFECT_MODELS`, optional; `Fx.mesh` returns each 1 m wide standing on y = 0, so draw it at its
real width and lower it by the given amount to centre it):

```python
                 # Ana's (tools/reference/ana-effects.md): the boost / anti-heal plus (0.072 m wide, flat, centred), the
                 # enemy hit's lens disc (1.988 m wide, 0.386 tall, base 0.177 below its centre point), Local
                 # Anesthetic's flattened sphere (0.97 wide, 0.588 tall, centred: lower by 0.294 x size)
                 "ana_plus": ("Ana", "BioticGrenade", "00000000E0AA"),
                 "ana_lens_disc": ("Ana", "BioticRifle", "00000000C943"),
                 "ana_shell": ("Ana", "SleepDart", "000000015CAE"),
```

The hemisphere 963D is `Shape.DOME` (size = 1.976 x the data's size: its diameter); the spheres 1385 / 0DBF are
`cat_sphere` (0.984 m wide, centred: lower by 0.492 x size). The projectile models (shot 11741, sleep dart 11740,
grenade 11763) are entity models, not effects: add them through the hero model path (`HeroModels.EXTRAS`, `rendering.md`:
an effect mesh loads only if it is named there).

## Palettes (`EffectArt.Palette`)

Linear, clamped at 1, (stop, r, g, b). Rows that rise from black keep the game's positions (0, .25, .5, .75, 1); rows
that start coloured get black at 0 and the game's values at .25 / .5 / .75 / 1 (Lúcio's convention). HDR rows also get a
`_N` line normalised by their peak. Generated by `research\ana\scripts\fx\fx_ana_palettes.py` (`work\fx_palettes.txt`, which
also has the enemy twins: ANA_MUZZLE_ENEMY, ANA_TRACER_ENEMY(_B), ANA_GRENADE_TRAIL_ENEMY, ANA_BURST_*_ENEMY,
ANA_LA_DOME_ENEMY, ANA_NANO_*_ENEMY ...). The two shot-trail strips are sampled (sRGB to linear by hand, M).

```java
		/** Her rifle's muzzle bulb 963D (800F41F7 r5, HDR 2): gold to white. */
		ANA_MUZZLE(0, 0, 0, 0, .25f, 1, .658f, 0, .5f, 1, 1, 0, .75f, 1, 1, .156f, 1, 1, 1, 1),
		ANA_MUZZLE_N(0, 0, 0, 0, .25f, .579f, .329f, 0, .5f, .751f, .519f, 0, .75f, .874f, .715f, .078f, 1, 1, .89f, .659f),
		/** The unscoped shot's trail: strip 9414 (sampled) x the gold tint (5, 3.99, 1.66), normalised. */
		ANA_SHOT_TRAIL(0, 0, 0, 0, .25f, .956f, .487f, .077f, .5f, .973f, .631f, .098f, .75f, .956f, .695f, .115f, 1, .973f, .735f, .223f),
		/** The scoped tracer (80008917 r4, HDR 2): gold to cream. */
		ANA_TRACER(0, 0, 0, 0, .25f, 1, .767f, .037f, .5f, 1, 1, .217f, .75f, 1, 1, .643f, 1, 1, 1, 1),
		ANA_TRACER_N(0, 0, 0, 0, .25f, .576f, .383f, .018f, .5f, .75f, .535f, .108f, .75f, .924f, .74f, .322f, 1, 1, .896f, .571f),
		/** The ally hit's burst 4B64D (800AB80B r1, HDR 2.8), glow 4C09 and sparks (800342A1 r1): pale gold, amber. */
		ANA_HEAL_BURST_N(0, 0, 0, 0, .25f, .549f, .405f, .15f, .5f, .694f, .567f, .214f, .75f, .846f, .739f, .283f, 1, 1, .913f, .352f),
		ANA_HEAL_GLOW(0, 0, 0, 0, .25f, 1, .635f, .123f, .5f, 1, .736f, .198f, .75f, 1, .837f, .272f, 1, 1, .939f, .347f),
		/** The enemy hit's lens discs (8009CD1E r4, HDR 10) and bubbles (800025BA r3): purple, violet. */
		ANA_HARM_N(0, 0, 0, 0, .25f, .059f, .004f, .254f, .5f, .114f, .008f, .466f, .75f, .301f, .021f, .732f, 1, .497f, .036f, 1),
		ANA_HARM_BUBBLE(0, 0, 0, 0, .25f, .211f, 0, .64f, .5f, .285f, 0, .825f, .75f, .363f, .004f, 1, 1, .552f, .117f, 1),
		/** The shot's impact sparkle 64B91 (800C00E6 r4, HDR 7, falls) and the world decals (800E8A1D / 8007CA88 r2). */
		ANA_IMPACT_N(0, 0, 0, 0, .25f, .296f, .048f, .446f, .5f, .094f, .001f, .428f, .75f, .027f, .006f, .079f, 1, .027f, .006f, .079f),
		ANA_DECAL_GOLD_N(0, 0, 0, 0, .25f, 1, .65f, .233f, .5f, .985f, .644f, .23f, .75f, .562f, .368f, .137f, 1, .132f, .087f, .042f),
		/** The grenade's trail (8006AB57 r4): black to violet to gold. */
		ANA_GRENADE_TRAIL(0, 0, 0, 0, .25f, .363f, 0, .628f, .5f, .586f, .047f, .911f, .75f, .607f, .152f, .71f, 1, .913f, .723f, .059f),
		/** The burst's gold dome (80079014 r5), violet-yellow shell (8000C715 r2), splashes (8009749B / 80029CEC r4). */
		ANA_BURST_DOME(0, 0, 0, 0, .25f, .504f, .294f, 0, .5f, .759f, .504f, .091f, .75f, .878f, .666f, .227f, 1, 1, .831f, .366f),
		ANA_BURST_SHELL(0, 0, 0, 0, .25f, .114f, .009f, .199f, .5f, .228f, .018f, .398f, .75f, .34f, .026f, .594f, 1, .982f, .783f, .019f),
		ANA_SPLASH_N(0, 0, 0, 0, .25f, .268f, 0, .517f, .5f, .269f, 0, .519f, .75f, .269f, 0, .521f, 1, .702f, .275f, .984f),
		ANA_SPLASH_GOLD_N(0, 0, 0, 0, .25f, .333f, .17f, 0, .5f, .333f, .17f, 0, .75f, .333f, .17f, 0, 1, .999f, .745f, .242f),
		/** The burst's droplets (80085D4D r1, HDR 10 / 800280E0 r1), splat decal (80024EDE r1, HDR 10), glow (800F4D2E r0). */
		ANA_DROP_GOLD_N(0, 0, 0, 0, .25f, .425f, .26f, 0, .5f, .708f, .457f, 0, .75f, .852f, .611f, 0, 1, 1, .768f, 0),
		ANA_DROP_PURPLE(0, 0, 0, 0, .25f, .346f, 0, .614f, .5f, .417f, 0, .743f, .75f, .487f, 0, .871f, 1, .558f, 0, 1),
		ANA_BURST_DECAL(0, 0, 0, 0, .25f, .609f, .053f, .906f, .5f, .793f, .328f, .465f, .75f, .956f, .584f, .091f, 1, 1, 1, 1),
		ANA_BURST_GLOW(0, 0, 0, 0, .25f, .577f, 0, 1, .5f, .693f, .4f, .708f, .75f, .845f, .786f, .34f, 1, 1, .847f, .006f),
		/** The heal boost's plus (80091108 r4) and the anti-heal's broken plus (800533DC r1, falls). */
		ANA_BOOST_PLUS(0, 0, 0, 0, .25f, 1, .845f, .341f, .5f, 1, .887f, .396f, .75f, 1, .928f, .45f, 1, 1, 1, 1),
		ANA_ANTIHEAL(0, 0, 0, 0, .25f, .434f, .035f, .821f, .5f, .423f, .071f, .737f, .75f, .413f, .106f, .655f, 1, .402f, .141f, .571f),
		/** The sleep dart's glow (80034914 r1, HDR 10), the Zs over life (80053871 r3, 12BB, HDR 1.3), the sleep sparks over life (800277D9 r0, HDR 5.7, falls). */
		ANA_SLEEP_GLOW_N(0, 0, 0, 0, .25f, .006f, .045f, .202f, .5f, .01f, .068f, .304f, .75f, .02f, .139f, .621f, 1, .032f, .223f, 1),
		ANA_SLEEP_Z(0, 0, 0, 0, .25f, .325f, .233f, .32f, .5f, .651f, .466f, .64f, .75f, .971f, .695f, .955f, 1, 1, .928f, 1),
		ANA_SLEEP_SPARK(0, 0, 0, 0, .25f, 1, 1, .38f, .5f, 1, .783f, .293f, .75f, 1, .447f, .148f, 1, 1, 0, 0),
		/** Local Anesthetic's dome (8008512C r0), gas (800861DA r7), drops (8005EE9F r3), the numbed wisps (800747DE r1, HDR 2). */
		ANA_LA_DOME(0, 0, 0, 0, .25f, .051f, .024f, .552f, .5f, .172f, .359f, .856f, .75f, .174f, .693f, .994f, 1, .019f, .982f, .982f),
		ANA_LA_GAS(0, 0, 0, 0, .25f, .068f, .182f, .643f, .5f, .13f, .371f, .879f, .75f, .36f, .677f, 1, 1, .637f, .921f, 1),
		ANA_LA_DROP(0, 0, 0, 0, .25f, .03f, .167f, .347f, .5f, .094f, .437f, .763f, .75f, .172f, .672f, .934f, 1, .242f, .863f, 1),
		ANA_LA_WISP_N(0, 0, 0, 0, .25f, .038f, .46f, .703f, .5f, .001f, .118f, .929f, .75f, .002f, .076f, .555f, 1, .042f, .063f, .135f),
		/** Nano Boost: the ally's shells (8000CCD7 r4, Mercy's boost blue), flames (80007283 r5, HDR 6), sparks (800DAD00 r1), the beam (80049B54 r2, HDR 3), the launch puffs over life (800AE47A r0). */
		ANA_NANO_BLUE(0, 0, 0, 0, .25f, .026f, .156f, 1, .5f, .046f, .293f, 1, .75f, .187f, .44f, 1, 1, .342f, .59f, 1),
		ANA_NANO_FLAME_N(0, 0, 0, 0, .25f, .005f, .031f, .191f, .5f, .013f, .085f, .281f, .75f, .087f, .2f, .447f, 1, .342f, .591f, 1),
		ANA_NANO_BEAM(0, 0, 0, 0, .25f, .116f, .186f, 1, .5f, .266f, .427f, 1, .75f, .513f, .821f, 1, 1, 1, 1, 1),
		ANA_NANO_BEAM_N(0, 0, 0, 0, .25f, .039f, .062f, .348f, .5f, .089f, .142f, .399f, .75f, .171f, .274f, .515f, 1, .443f, .71f, 1),
		ANA_NANO_MUZZLE(0, 0, 0, 0, .25f, .537f, .181f, 0, .5f, .733f, .362f, 0, .75f, .893f, .601f, 0, 1, 1, .941f, 0),
```

Notes on the palettes: ANA_SHOT_TRAIL is the strip's sRGB samples (#FACD84 #FCE694 #FAF09F #FCF6D6) linearised and
multiplied by the tint's hue (5, 3.99, 1.66) / 5 (M: how the material combines them); an orange nano palette (strip 5DA3: #B30000 #FE0500 #FE4B00 #FEFEB1) is not generated
(add it the same way if the orange core is drawn). Rows that fall (ANA_IMPACT, ANA_ANTIHEAL, ANA_SLEEP_SPARK, the LA
streak) are bright at mask 0 or are over-life rows: use them by age. ANA_SLEEP_Z, ANA_SLEEP_SPARK and ANA_NANO_MUZZLE are
over-life tints (12BB), not mask palettes.
