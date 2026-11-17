# Cassidy's ability effects in the game data

What Cassidy's effects are made of in real Overwatch, where the extracted files are, and how each would be rebuilt in Minecraft
(a `CassidyFx` beside `TracerFx` / `TorbjornFx`; today the server sends vanilla particles from `cassidy/Cassidy.java`, issue #150).
Read 2026-11-17 from the archived offline build 154088. Overwatch and Battle.net were checked closed (`Get-Process Overwatch,
Battle.net`) before each TankLib run, and `owinit.py` refuses on its own; everything was read only, offline, through TankLib/DataTool.
Text only in the repo: masks, ramps and meshes stay in the owner's archive and reach the game through the local asset pack. Format:
[torbjorn-effects.md](torbjorn-effects.md) / [tracer-effects.md](tracer-effects.md). His data: [cassidy-map.md](cassidy-map.md);
public numbers: [cassidy.md](cassidy.md); hub: [cassidy](../../techniques/heroes/cassidy.md); method:
`techniques/rendering/rendering.md` "Effects and glows".

Confidence: **H** read in the data, **M** inferred, **L** guess. "Size" is the spawn size (a 1 m quad at size 0.5 is 0.5 m; a
ribbon's or a line's is its width; a mesh's size scales its own extent). "Ours" marks what no data gives. `XXXX rN` = the colour LUT
(last 4 hex digits of `ramps\8...XXXX.json`) and its row. "hp" = hardpoint. Proposed palettes are `CS_*` (raw stops from
`fx_cassidy_palettes.py` in `work\fx_palettes.txt`; the list at the end is those, edited so each ends on its colour).

## Files and scripts
- `E:\Overwatch Archive\research\cassidy\fx\Cassidy\<Ability>\` (`Peacekeeper`, `FanTheHammer`, `SilverBullet`, `Reload`,
  `CombatRoll`, `Flashbang`, `Deadeye`): `textures\` (PNG, flipbook frames `_1.._n`), `ramps\` (float LUTs as JSON),
  `models\Models\<id>.00C\<id>.owmdl`, `manifest.json`. A texture shared by several abilities sits in each folder that uses it.
- `research\cassidy\work\`: `fx_cards_{peacekeeper,fan,silver,roll,flash,deadeye,deadeye_shot}.txt` (one card per renderer: what
  this note is written from) and their `_compact.txt`, `fx_overview.txt` (one line per effect), `fx_digest.txt`,
  `fx_summary2.txt|json`, `fx_meshes2.json` (mesh bounds), `fx_entities.txt` (shots 02A7 / 3B7B / 6AF1, flashbangs 037D / 65F2 /
  6D81, hero 02A3, revolver 02A6 and their children), `fx_entities_deadeye.txt` (Deadeye's shot 0420, 05A3's entity 0424),
  `fx_clip_effects.txt`, `fx_palettes.txt` (the `CS_*` stops), `models.json` (his models' hardpoint tables),
  `fx_extract_log.txt` / `fx_extract_log2.txt`, `fx_effects\` (decoded).
- Scripts `research\cassidy\scripts\fx\`: Tracer's `fx_tracer_*` copied as `fx_cassidy_*` (`fx_cassidy_extract.py` holds his effect
  list with each one's graph node; `fx_cassidy_palettes.py`'s `PAL` is now his). Run: `fx_cassidy_extract.py [Ability ...]`
  (kiriko-anim venv, `-I`); then under `py -I run.py`: `fx_cassidy_summary2.py`, `fx_cassidy_digest.py`, `fx_cassidy_overview.py`,
  `fx_cassidy_card.py <ids>`, `fx_cassidy_cardsc.py <cards>`, `fx_cassidy_palettes.py`. New this pass: `fx_cassidy_dumpents.py`
  (venv: dumps entity STUs into `dump_ss`), `..\model_cassidy_fx.py` (venv: hardpoint tables of 3P EFA3, 1P F082, revolver EECF,
  Flashbang 118EB into `work\models.json`), `..\lookhp_cassidy_fx.py` (text: their rest positions).
- Counts: **65 effects asked for, 65 extracted (+ sub-effects 057A, 356B, 6DD4, 4010), 0 missing.** 26 draw something (counting 05A3's prop entity and 9885's hand model); the rest are
  sounds only or empty (00F7, 2860 / 2B1E the revolver's children, 4010 a shared sound, the reload / roll / Deadeye clips but the
  revolver's 0402 / 05C0).

## His look
- **Team cyan, enemy orange-red (H rows, M roles), as Tracer's.** Every shot's line is a twin on the same mask: team **0FEF r4**
  cyan HDR 9.8 / enemy **C54D r4** orange to red HDR 10 (the hit flashes 3949 r1 blue / 606A r1 red, the hit decals 5C90 r2 / 6F1B r2
  the same). The muzzle flash, smoke and sparks have no twin: warm gold for everyone. Flashbang, Silver Bullet's armed glow and
  Deadeye's glows are LUTs with a cool row and a warm row (M: team / enemy). Draw team rows added, enemy rows added too (fire),
  smoke rows blended (`Fx.soft`).
- **Hardpoints (H positions from `work\models.json`, rest pose; M roles):** revolver EECF **0006** the muzzle (0.11 m up, 0.50 m
  ahead of its grip; every muzzle effect, the reload smoke); his **000A** Weapon_R (the revolver 02A6 rides there, 1.04 m up,
  0.54 m right); **0003** his right wrist (Silver Bullet's armed glow); **000B** Weapon_L (the Flashbang in his left hand, 1P clip
  9885); **0001** chest (1.44 m: Deadeye's glows; on a target, its shots' aim point and the bleed); **0012** head (1.70 m: Deadeye's
  eye glint, at (-0.05, -0.02, +0.10) and (-0.11, -0.05, +0.04) from it); **00B6** the cigar's tip (bone 05F2, 1.65 m, 0.19 m in
  front of his face); **0004** his root (Combat Roll's dust); **00A4** Spine_3 (sounds; on a hindered target, the stars' centre).
  Flashbang 118EB: **0076** 0.127 m along it (its ribbons).
- **Screen layers:** renderers with `4E65CCF9 = ECIM(40)` are the local player's own view (Deadeye's 5708 ring, the hindered
  player's D8F rim): drawn as a screen tint if at all.

## Peacekeeper (01D2; shot 02A7, hitscan; muzzle sub-effect 057A at the revolver's 0006)
| Part | Effect | Rebuilt (replaces) |
|---|---|---|
| **Muzzle** | **057A** (18D7's sub-effect, at **0006**; one per shot, all under 0.45 s, H): flash dome mesh **DAF** x 16E9D 0.3-0.9 m at +0.25 m, 0.075 s (050A / E21C r4: 1-channel, L); flame flipbook **1FB83 (32 frames)** x 5429 x B08, **4 at 2.9-3.1 m/s**, 0.5-0.65 m stretched x4.6, 0.125-0.45 s, at +0.7 m (9F06 r5 cream; 1FB83 is a colour flipbook, BC1); **cones mesh 80B8** x 2A2B9 / 29887 **1.94-2.15 m**, 0.04-0.06 s, at +0.04 m (6BC1 / 1E10 / D883 / ABDC / 22A0 r1: channel-shifted, M); **flashes 5C04 (4)** 2 at **1.1-1.2 m 0.08-0.09 s** (**3352 r1** brown > cream HDR 4.2) and 2 + 2 at 1.22-1.32 m 0.06 s, -0.07 m behind (4F22 r1 rust > amber / B2A1 r1 dark red > cream, M twins); sparks **1BCFB** 2 at 1.9-2.8 m/s 0.02-0.03 m (8370 r2 orange > cream) and 4 + 4 at **1.9-5.3 m/s**, 0.01-0.03 m, 0.02-0.18 s, cone +-40 deg (A264 r2 / 2205 r2); ring mesh **E700** x 2A2B9 x 7A9 x 30E11 0.7-0.8 m 0.03-0.04 s (7147 r6 orange > cream); smoke **B08 x 1C77 x 5429**, 5 at 1.5-1.7 m/s, 0.28-0.4 m stretched x4.6, 0.125-0.45 s, cone 27 deg (**B999 r7** cream > brown-grey, HDR 10) and 5 of 9EF x B08 0.36-0.48 m (C91F r3 blue-grey); lights 1.0 and 1-2 for 0.03 s; sub-effect 4010 = a shared sound | at the posed revolver 0006 each shot (a third the size in his own view, ours, as Tracer's): the dome, one flame, one cone, two 5C04 flashes (`CS_MUZZLE_FLASH`, `CS_MUZZLE_FLASH_C`), 4 sparks (`CS_MUZZLE_SPARK`), the ring, two smoke puffs (`Fx.soft`, `CS_MUZZLE_SMOKE`), a light. New (none today; the shot is `weapon` :206 / :216) |
| **The line** | **18D7** (02A7's STUEffectComponent, 1 s): two renderers on quad 801 with mask **510** (x 50C-less, length curve 8006AADA), emitter ECIN (muzzle to hit, M), **0.25 s**, width field 59DC556C **0.055 m** (M width); team **0FEF r4** / enemy **C54D r4** (H). Smoke along it: **4DB x B35 x 965 0.34 m wide, 0.87 s** (B2E7 r7 yellow head > grey, HDR 6.9) and **4D5 x 965 x B35 0.5 m wide, 1.0 s** x2 (FB26 r7 / DC8C r7) (H; widths M) | `SombraFx.tracer`'s band on `sombra_tracer_line`: 0.055 m, 0.25 s, `CS_LINE` / `CS_LINE_ENEMY` by the viewer's side; one soft smoke band 0.3 m wide (ours: thinner than 0.5) for 0.9 s, `CS_SMOKE_LINE` at 0.35 alpha (ours). Replaces `trail()`'s `SMOKE` dots (Cassidy.java:378) |
| **Hit (creature and world)** | **05AE** (02A7 / 3B7B / 6AF1 `m_AED73588` **and** `m_BF7914D4`: the same effect for both, H; 2 s): flashes **5B71 (4)** 8 at 0.5 m/s, **0.175-0.2 m, 0.19 s**, cone 30 deg (team **3949 r1** blue > cream / enemy **606A r1** red > yellow); shards mesh **EF7** x 5B70 (4) 2 at 0.25 m, 0.18 s, +0.15 m off the surface (275F r1 gold > cream / 2559 r1 red > yellow); **decal 29F7 0.2 m, 1.87 s**, random turn (5C90 r2 cyan > blue HDR 4 / 6F1B r2 orange > red HDR 6.4) (H) | where the shot stops: 4 flashes (ours: half), the two shards, the decal on a block only (ours: no decal on a creature, the data puts one on both); `CS_HIT*`. Replaces `trail()`'s `CRIT` x2 (Cassidy.java:380) |
| Shot sounds | 008C (`m_2170A015`), 013A, 0231, 3483, 6528: sounds only (H) | - |
| The cigar | **0612** (hero 02A3's child 045C, 10 s, looped M; hp **00B6**, (0, -0.023, +0.042)): one 2606 x 5DF x 2612 x 2608 quad **0.075 m**, 0.5 /s living 10 s, tint 28AB r0 (2, 0, 0.5) (H; the tint channel-shifted, L) | not asked: a 0.075 m ember at his posed 00B6 (ours: orange, `CS_MUZZLE_SPARK`'s top), skipped in his own view |
| Revolver | 2860 / 2B1E (02A6's children) empty; 00F7 (hero child 130D) empty; clips 1B26 / 9893 / 0453 camera shakes (H) | - |

## Fan the Hammer (01D2; shot 3B7B, 0.12 s apart until empty)
| Part | Effect | Rebuilt (replaces) |
|---|---|---|
| Each shot | **356A** (3B7B's STUEffectComponent) = **18D7** exactly (same masks, LUTs 0FEF / C54D / B2E7 / FB26 / DC8C, sizes, lives) (H) | the Peacekeeper line per shot; the smoke band only every other shot (ours: 6 overlapping bands in 0.6 s read as a wall). Replaces `trail()` (Cassidy.java:358 via :251) |
| Muzzle | **356B** = 057A but the flame on **90F1 r0** and the C52 flash on **BD91 r1** gold > cream (H) | the Peacekeeper muzzle with `CS_FAN_FLASH` |
| Hits | 05AE (above) | as Peacekeeper's |

## Silver Bullet (perk 2723; armed by SECONDARY at 01D2 #1147 / #1368, shot 6AF1, bleed 5FDA)
| Part | Effect | Rebuilt (replaces) |
|---|---|---|
| **Armed** | **6E6B** (#1249 / #1331, gated by perk 2723, 1 s re-held while armed M; hp **0003** his right wrist): glow 19167 x 4C09 **0.55-0.74 m**, 2 /s, living 0.15-0.52 then 1.5-1.75 s, at (-0.05, -0.05, +0.01) (**CB3F r4** navy > pale cyan HDR 2.9 / **r5** dark red > cream HDR 5); glow 18E48 x 4C09 **0.4 m**, 1 /s, living 5 s, at (0.03, -0.08, 0.03) (F63A r4 / r5) (H; rows M) | while `CassidyState.silverAim` (needs syncing to others: L), at his posed 0003: a 0.6 m soft glow at 0.4 alpha (ours) and the 0.4 m one, `CS_SILVER_ARMED` / `_ENEMY`; new |
| Armed, his own view | **25E1** (#1011 / #1415, under `self ==` switches: his view only, M): one flat ring mesh **5C75** x 18B9B size 2, held (no colour row, L). 6DEC / 6DED (#1247 / #1248, arm / disarm M): sounds (H) | not drawn (L what it is) |
| **The shot** | **6DB8** (6AF1's STUEffectComponent) = 18D7 but its smoke is **blue-headed and lives longer**: 4DB x B35 x 965 0.34 m **1.6 s** (**734D r7** blue > grey, HDR 16), 4D5 x 965 x B35 0.5 m **2.0 s** (90C6 r7) and **1.5 s** (57C7 r7 orange > brown, HDR 16); the line 0FEF / C54D r4 as Peacekeeper's (H). Muzzle **6DD4** = 057A exactly (H) | the line, one smoke band 0.35 m for 1.6 s (`CS_SILVER_LINE`), the muzzle; replaces `trail()` (Cassidy.java:358 via :213) |
| **The bleed** | **3488** (5FDA #4 on the target's **0001**, 1 s; the bleed runs 1.5 s: replayed or held, M): glows A7B6 x 37B2E sized by data flow 5FD **0.11 > 0.63 m** and **0.49 > 1.01 m** (M: grows with the bleed), 0.2-0.25 s, 1 /s + 1 (**F7C6 r0** plum > pink HDR 3.8 / 8CA8 r0 purple > magenta; their other rows 119B r3 / 0715 r3 black > blue-white: L which one draws); glow 4A7E x A7B6 x 46E5 **2.5 m**, 5 /s, 0.2-0.25 s (2B26 r3 blue-white, M low alpha); **5 sparks 1F35 at 3.8-4.4 m/s**, 0.02-0.05 m, stretch 2, spin -5, 0.2 s (**8FF6 r1** dark red > pink HDR 23); sounds 61EB / 627D / 6503 (H) | each bleeding target (`BLEEDS`), on each 0.2 s step (`tickBleeds` :410): a glow at its centre growing 0.3 > 0.8 m over the 1.5 s (ours: the two glows as one), 2 sparks (ours); `CS_BLEED`, `CS_BLEED_SPARK`; sent from `bleed` (:399) as `"cassidy_bleed"`, life 1.5 s; new (nothing marks it today) |
| Others | 4767 / 4768 (51E5 #343 / #377) sounds; **6BC3** (51E5 #376, Giddy Up): speed lines E99F size 10, 60 /s + 30 (his own view, M) (H) | not asked: not drawn |

## Reload (1.5 s; ammo at 0.8 s)
| Part | Effect | Rebuilt |
|---|---|---|
| Barrel smoke | revolver clips **0402** (1P `reload`, 1.5 s) / **05C0** (3P `reload_3p`, 1.63 s), at the revolver's **0006**: smoke 9EF (4) x B0A, **50 /s from 0.33 s for 0.67 s**, 0.67-0.87 s, **0.11-0.24 m**, drifting 0.06-0.11 m/s (dir (0, 0.016, 0): up, M) (4287 r0 peach; 2700 r0 navy > violet, L) (H) | from `startReload` (:267): at the posed 0006 from 0.33 to 1.0 s, a puff each tick (ours: 50 /s is 2.5 a tick; one), `Fx.soft("ana_smoke", CS_RELOAD_SMOKE)` 0.18 m rising, alpha 0.5 (ours); scaled to the reload's length |
| His clips | 03AC (1P), 048A / 050B (3P): sounds only (H) | - |

## Combat Roll (01F0; 0.4 s)
| Part | Effect | Rebuilt |
|---|---|---|
| **Dust** | **05E4** (01F0 #127, 0.5 s, hp **0004** his root): dust clouds **5429 (4) x B08 x 1FC65 (32 frames)**, **60 /s + 3 for 0.33 s**, **1.26-1.76 m**, 0.5-0.64 s, 1.24-1.55 m/s, dir (0, 0.75 up, -0.37 back) (**706F r2** black > cream HDR 1.3; r0 red > yellow and 5822 r0: L); kicks **DA0 (2) x B09** 26 /s from 0.067 to 0.3 s, 0.26-0.36 m, 0.19-0.35 s, 0.9-1.4 m/s, at (0, -0.21, -1.42) from 0004 (01B4 r0: 1-channel, L); **600 motes 242** at 4-4.2 m/s, 0.02-0.13 m, 0.34-0.44 s, directions by data flow (4A41 r2 tint (1.25, 0.71, 0.5) warm) (H; offsets as (x, y, z) M) | from `startRoll` (:461): each tick of the roll at his feet, 3 dust puffs (ours: 60 /s is 3 a tick) 1.5 m, `Fx.soft("cassidy_dust", CS_ROLL_DUST)` alpha 0.35 (ours), rising 1.4 m/s back along the roll; 40 motes at the start (ours: a fifteenth) on `mercy_glow`, warm (`CS_ROLL_DUST` top) |
| Streak | **not in the data**: 05E4 has no ribbon or streak; the roll clips 046C-0470 (3P) are camera shakes and sounds, 03AE / 0467-0469 (1P) sounds; 36B0 (#176) sounds (H) | none (the dust is the look) |

## Flashbang (01E9; thrown 037D, Bang Bang's second 65F2; hinder 0079.053 / graph 0228)
| Part | Effect | Rebuilt (replaces) |
|---|---|---|
| Throw | 1P clip **9885** (1294, 0.6 s): the Flashbang model 11D8 in his left hand **000B** for 0.13 s (H); 3P clips 050A / 1C54 / 1C55 and 727D (01E9 #223): sounds (H) | the model is the pack's `projectile_cassidy_flashbang` (HeroModels); nothing new |
| **In flight** | **0600** (037D / 65F2's child 0453, at the grenade's **0076**): two ribbons on mesh 75D with mask **AC4** (8x8, solid) x curve 800E9688, **0.5 m wide, 60 points /s, 0.3 s**, emitting 0-0.63 s and 0.067-0.63 s (ECA2 r2 tint (6.59, 0.92, 0.04) orange / DB42 r3 tint (3.5, 2.55, 0.94) warm yellow, both drawn, M) (H; width M) | per flashbang each frame (ProjectileModelRenderer, as `DvaFx.missileFlight`): `Fx.trail` 0.15 m wide (ours: 0.5 m on a 25 cm grenade reads as a slab), 0.3 s, `cassidy_ribbon` (AC4), `CS_FLASH_TRAIL`; new |
| **The bang** | **5C8C** (037D `m_6CB564CC`, 2 s, at its 00A4): flash **2D6C9 1.5 m, 0.26 s** (**FF88 r0** black > gold > pale yellow HDR 4.9 / **r1** rust > peach); flash **16E9D x 29887 2.0 m, 0.14 s** (A55C r4 blue / r3 yellow > rust); **hemisphere mesh 963D** (2 x 1 m) x 18E48 x 16E9D, 2 at size 1.0, **0.33 s** (E5D5 r1 gold / r2 red); 4 streaks 1FBCE x 19167 **0.37-0.5 m**, 0.06-0.12 s, 0.1 m/s (CC06 r5 navy > pale blue / r7 plum > yellow); two lights rising 1 > 5 over 0.33 s; sounds 5D7C / 071E (H parts; which row of each pair draws: M, the gold rows for his team) | at `flashBang` (:592): the 1.5 m flash (`CS_FLASH`), the 2 m flash, the dome mesh (`torb_hemisphere`, `CS_FLASH_DOME`), 4 streaks, a light; replaces `END_ROD` x20 / `ELECTRIC_SPARK` x30 (Cassidy.java:624-625) |
| Bang Bang's | **6541** (65F2 `m_6CB564CC`) the same, larger: 8 streaks 0.35-0.75 m, flashes **1.7 m** and **2.6 m**, a sphere **DBF at 3.45** (a 3.4 m ball: the 3 m blast) instead of the dome, lights 0.8 > 4; its rows swap the cool / warm roles of 5C8C (5C3D, 9A95, C7E3 r0 blue, CD31 r1 blue) (H; M the roles) | the same bang with the sphere (`cat_bubble`) and the larger sizes for the perk's second (variant 1) |
| **Hindered** | **05D9** (0228 #55, on the target's **00A4** + 0.5 m, 2 s; the hinder lasts 1.2 s: cut, M): **two band meshes DE0 x 4D4 x 247C, size 1-1.11, turning opposite ways once over the life** (A458 r4 tint 3: the colour texture's own); sparkles **248B 10 + 20 /s**, 0.025-0.083 m, 0.12-0.24 s (98A4 r2 tint 16.7) and 8.5 /s 0.04-0.14 m at +0.5 m (0BDE r1 tint 10); rim D8F 2 m on the hindered player's own screen (ECIM(40), 03C5 r1 blue); sounds 52F2 / 07C9 (H) | **the same meshes and masks as Brigitte's stun** (`BrigitteFx.stunned`: DE0 x 247C halos, 248B stars, `Palette.NATURAL`): call it for each hindered entity for its 1.2 s (`Hinder.apply`, Hinder.java:28, sends `"cassidy_hinder"`); new (nothing marks it today) |
| Stadium | 4257 / 4FF0 (Hot Potato 6D81) not live (H) | not drawn |

## Deadeye (022F, per target 01EE; shot 0420)
| Part | Effect | Rebuilt (replaces) |
|---|---|---|
| Cast | **03AE** (#423, 11 s: the "It's high noon" lines 0766 / 0767 / 1911), 062A (#429), 7221 (#648): sounds only; clips 0524 / 0526 / 0815 sounds (H) | - |
| **Held on him** | **0610** (#428, 10 s authored, cut at the end M; hp 0001 / 0012) H parts, M roles: **eye glint**: red stars **2F4F0 x 242 1.25 m**, held 6.5 s, at **0012 + (-0.05, -0.02, +0.10)** and (-0.11, -0.05, +0.04) (**EE96 r0** / 2100 r0 black > red > magenta > dark red > black) and stars **F7A7 0.8 m** held 5-8 s at the same point (7C32 r1 tan > white HDR 2.6 / 006E r1 dark red > white); **glows 139C 1.67-1.86 m** at 0001 +0.1 m, 4 /s, 1.5-1.75 s (4EEC r1 ochre > white HDR 2.65 / DFD2 r3 dark red > gold); **band mesh 129B** (0.95 m tube) x 1298 x 18A1 size 1.47 at +1.31 m, held (160D r0 tint (2.65, 0, 1) / 8E96 r3 orange > red; L where: above his head); **dust AFF x 923 (2) x A76 (4)** 2.2-3.3 /s, **1.0-2.3 m**, 1.7-2.0 s, 1.7-2.2 m/s sideways, at (1.16, 0.94, 1.61) and (-0.8, 0.79, 2.69) (D45E / 2C93 r7 tint (1.98, 1.3, 0.57) tan; 7C34 r7 (1.98, 0.57, 0.57) red) - the wind's dust before him (M); motes 242 50 /s 0.025-0.075 m (04FB r1 tint (0, 1.98, 1.3): channel-shifted, L); ring 5708 size 2 on his own screen (ECIM(40), CC74 r1 / r2); two lights 3.8 | `Fx.attach` per Cassidy while `deadeyeTicks > 0` (synced: the hero sync): the red glint (`brigitte_flare_star`, `CS_DEADEYE_EYE`, 0.6 m: ours, half the data's 1.25 m from 3 m away reads as his face lit) and the F7A7 star 0.4 m at his posed 0012 offset, skipped in his own view; the 1.75 m glow at 0001 at 0.15 alpha (ours); dust 2 /s 1.5 m (`cassidy_dust_cloud`, `Fx.soft`) to his side; the band not drawn (L); new (nothing today) |
| 05A3 | (#397, 10 s) spawns entity **0424**: a 7 cm prop model 12C8 (look 1615) at (6.6, -1, 3) (H; L what it is) | not drawn |
| **Lock marks** | **not effects**: the skull / mark is 01EE #158 **StateUXPresenter** (HUD, H); 0607 (#23) / 0608 (#59, before each shot) are sounds (H) | the existing `DeadeyeHud` |
| **A shot** | **060D** (0420's STUEffectComponent, at 0006): Peacekeeper's muzzle (057A's set) and line (0FEF r4 0.055 m 0.25 s, smoke B2E7 / FB26) **plus** a line 510 x **50C** 0.1 s (**59BA r4** amber > red HDR 10) and a **tube mesh 8C0** x 4D5 x 965 x B35, width 0.69, 1.0 s (06FF r6 orange > rust HDR 9.4); ECIN 6F (its own end point) (H) | per shot from `deadeyeShot` (:829): the muzzle, the line (`CS_LINE` / `CS_DEADEYE_LINE` by side), one wider soft band 0.3 m for 1 s (`CS_DEADEYE_TUBE`, ours: the tube as a band); replaces `trail()` (Cassidy.java:847) |
| Its hit | **010F** (0420 `m_AED73588`; no world slot, H): decals **220A 0.035 m** x2, 0.67 s (35DA r2 orange > red HDR 4); puffs **1BE6 (4)** 2 at 2 m/s, 0.23-0.33 m, 0.05 s (76F6 r2 tint (30, 1.5, 1) red / 6AA6 r2 white); shards **EF7 x 1BEC (2)** 0.19-0.29 m 0.05 s (2B70 r2 (30, 4.5, 1.25) / 0304 r2 (30, 26, 8)) (H) | at the target's 0001: two puffs (`sombra_impact_spark`), one shard (`torb_hit_shard_mesh` x `sombra_impact_star`); no decal on a creature (ours); replaces `trail()`'s `CRIT` (:380 via :847) |

## Where it hooks in (each vanilla call it replaces)
| Today (`src/main/java/dev/overcraft/...`) | Becomes |
|---|---|
| `cassidy/Cassidy.java:378` `SMOKE` dots in `trail()` (every shot: Peacekeeper :216, Silver Bullet :213, Fan the Hammer :251, Deadeye :847) | `Effects.play(level, "cassidy_shot", from, to - from, player, variant)` once per shot from `shot()` :358 / `deadeyeShot` :847, variant 0 Peacekeeper (18D7 + 057A), 1 Fan the Hammer (356A + 356B), 2 Silver Bullet (6DB8 + 6DD4), 3 Deadeye (060D); the client draws muzzle, line and smoke |
| `cassidy/Cassidy.java:380` `CRIT` x2 at the stop | `"cassidy_hit"` (05AE) at the stop with the hit face's normal (block: with the decal), or `"cassidy_deadeye_hit"` (010F) from `deadeyeShot` |
| `cassidy/Cassidy.java:624` `END_ROD` x20 and `:625` `ELECTRIC_SPARK` x30 in `flashBang` | `"cassidy_bang"` (5C8C; variant 1 = Bang Bang's 6541 when `share != 1`) |
| none: Flashbang flight (`entity/FlashbangEntity.java`, tick :82) | `CassidyFx.flashFlight` from `ProjectileModelRenderer` (case `FlashbangEntity`) (0600) |
| none: hindered (`cassidy/Hinder.java:28` `apply`) | `"cassidy_hinder"` to the target for 1.2 s, drawn by `BrigitteFx.stunned`'s look (05D9) |
| none: Silver Bullet armed (`CassidyState.silverAim`) / bleed (`bleed` :399, `tickBleeds` :410) | armed: client side off a synced flag (6E6B; L the sync); bleed: `"cassidy_bleed"` once from `bleed` (3488, life 1.5 s) |
| none: reload (`startReload` :267), Combat Roll (`startRoll` :461) | `"cassidy_reload"` (0402 / 05C0 smoke at 0006, variant = length in ticks); `"cassidy_roll"` (05E4, entity = him, for the 0.4 s) |
| none: Deadeye held (`startDeadeye` :631, `deadeye` :673) | client `CassidyFx.frame()`: each Cassidy in Deadeye (0610 at his posed 0012 / 0001) |
- Hardpoints to pose (HeroModelRenderer's `POINTS`, as `SojournFx.POINTS`): `hardpoint_0012`, `hardpoint_0001`, `hardpoint_0003`,
  `hardpoint_00B6`, `hardpoint_0004`; the revolver glb's `hardpoint_0006`; the Flashbang's `hardpoint_0076`.
- Palettes: the `CS_*` arrays below in `HeroPalettes` (or a `CassidyPalettes` if it outgrows it), `EffectArt.Palette` entries
  pointing at them; `ART` / `ART_ENEMY` lists as `TorbjornFx`'s.

## Not done / not found
- **Combat Roll's streak**: none in the data (dust only). **Deadeye's lock marks**: HUD, not effects. **The lock-on "high noon" sun /
  sky tint**: not in 022F's effects (L: the 05A3 prop or a post-process).
- Which row of each LUT pair draws (material parameter, not decoded): team / enemy roles are M throughout; the Flashbang's and
  Bang Bang's pairs swap cool / warm between the two effects (H), so which is "team" there is L.
- Channel-shifted LUTs (as Tracer's): the muzzle cones' 6BC1 / 1E10 / D883 / ABDC / 22A0, the dome's 050A / E21C r4, the cigar's
  28AB, Deadeye's motes 04FB / 96D6, the roll's 01B4: not given palettes (drawn from their twins or skipped).
- Sizes by data flow not decoded: the bleed glow's input 5FD, the roll motes' directions, the lines' end points (ECIN, M: the hit).
  The 0.055 / 0.34 / 0.5 / 0.69 m line widths are the `59DC556C` field read as width (M).
- 25E1 (his own view while armed) and 6BC3 (Giddy Up's speed lines), the 05A3 prop 12C8, Deadeye's 129B band: read, not drawn.
- Material effects / distortion: none found in his kit.

## What the builder adds to the pack (all under `E:\Overwatch Archive\research\cassidy\fx\Cassidy\`)
Masks (`EFFECT_MASKS`: name -> ("Cassidy", ability folder, 12-hex id, frames)), new `cassidy_*`:
```
    "cassidy_smoke_noise": ("Cassidy", "Peacekeeper", "0000000004D5", 1),
    "cassidy_hit_flash": ("Cassidy", "Peacekeeper", "000000005B71", 4),
    "cassidy_muzzle_flame": ("Cassidy", "Peacekeeper", "00000001FB83", 32),
    "cassidy_noise_b08": ("Cassidy", "Peacekeeper", "000000000B08", 1),   # Torbjorn's note names it torb_noise_b08: one name when both merge
    "cassidy_cone": ("Cassidy", "Peacekeeper", "00000002A2B9", 1),
    "cassidy_cone_b": ("Cassidy", "Peacekeeper", "000000029887", 1),
    "cassidy_flash": ("Cassidy", "Peacekeeper", "000000005C04", 4),
    "cassidy_spark": ("Cassidy", "Peacekeeper", "00000001BCFB", 1),
    "cassidy_ring_noise": ("Cassidy", "Peacekeeper", "000000030E11", 1),
    "cassidy_smoke_b": ("Cassidy", "Peacekeeper", "000000001C77", 1),
    "cassidy_cigar": ("Cassidy", "Peacekeeper", "000000002606", 1),
    "cassidy_noise_b0a": ("Cassidy", "Reload", "000000000B0A", 1),         # Torbjorn's torb_noise_b0a
    "cassidy_dust": ("Cassidy", "CombatRoll", "00000001FC65", 32),
    "cassidy_dust_noise": ("Cassidy", "CombatRoll", "000000000B09", 1),
    "cassidy_bang": ("Cassidy", "Flashbang", "00000002D6C9", 1),
    "cassidy_ribbon": ("Cassidy", "Flashbang", "000000000AC4", 1),
    "cassidy_bleed_glow": ("Cassidy", "SilverBullet", "000000037B2E", 1),
    "cassidy_bleed_noise": ("Cassidy", "SilverBullet", "0000000046E5", 1),
    "cassidy_dust_cloud": ("Cassidy", "Deadeye", "000000000AFF", 1),
    "cassidy_dust_cloud_b": ("Cassidy", "Deadeye", "000000000923", 2),
    "cassidy_dust_cloud_c": ("Cassidy", "Deadeye", "000000000A76", 4),
```
Reused (same ids already in EFFECT_MASKS): `sombra_tracer_line` 510, `sombra_tracer_edge` 50C, `mercy_noise` 4DB, `ana_beam_line`
B35, `ana_nano_line` 965, `torb_hit_shard` 5B70 (4), `lucio_spark` 29F7, `juno_noise_c` 5DF, `cat_cone_line` 16E9D, `cat_dust_puff` /
`ana_puff` 5429 (4), `cat_fade` 7A9, `ana_smoke` 9EF (4), `brigitte_spark_streak` DA0 (2), `mercy_glow` 242, `sigma_streak` 1FBCE,
`cat_glow_noise` 19167, `cat_cloud` 18E48, `brigitte_noise_fine` 4D4, `brigitte_stun_halo` 247C, `brigitte_stun_star` 248B,
`mercy_rim` D8F, `cat_soft` 4C09, `glow` A7B6, `mercy_noise_b` 4A7E, `spark` 1F35 (4), `cat_speed_line` E99F (3), `dva_solid` 1298,
`cat_rope` 18A1, `mercy_soft` 139C, `juno_cast_fade` D9B, `cat_sparkle` F7A7, `brigitte_flare_star` 2F4F0, `sombra_impact_burst` 220A,
`sombra_impact_spark` 1BE6 (4), `sombra_impact_star` 1BEC (2). Not needed: 2612 / 2608 (the cigar's other layers), 18B9B (25E1).

Meshes (`EFFECT_MODELS`: name -> ("Cassidy", ability, 12-hex id); `models\Models\<id>.00C\<id>.owmdl`):
```
    "cassidy_muzzle_cone": ("Cassidy", "Peacekeeper", "0000000080B8"),   # 1 x 0.35 m flash cone
    "cassidy_muzzle_ring": ("Cassidy", "Peacekeeper", "00000000E700"),   # 1 m flat ring, hole 0.16
    "cassidy_tube": ("Cassidy", "Deadeye", "0000000008C0"),              # Deadeye's shot volume (or drawn as a band)
```
Reused: `torb_dome` DAF, `torb_hit_shard_mesh` EF7, `torb_hemisphere` 963D, `cat_bubble` DBF, `brigitte_band` DE0, `sombra_cylinder`
129B. Not needed: quads 801 / F5B, ribbon mesh 75D, 5C75 (25E1), 5708 (own view), the prop 12C8, the Flashbang 11D8 (the pack has
`projectile_cassidy_flashbang`).

Ramps (`<Ability>\ramps\8...<id>.json`): Peacekeeper 0FEF r4 / C54D r4, B2E7 r7, FB26 r7, DC8C r7, 3352 r1, 4F22 r1, B2A1 r1, 9F06
r5, 8370 r2, A264 r2, 2205 r2, 7147 r6, B999 r7, C91F r3, 3949 r1 / 606A r1, 275F r1 / 2559 r1, 5C90 r2 / 6F1B r2, 28AB r0;
FanTheHammer BD91 r1, 90F1 r0; SilverBullet 734D r7, 90C6 r7, 57C7 r7, CB3F r4 / r5, F63A r4 / r5, F7C6 r0, 8CA8 r0, 8FF6 r1, 119B r3;
Reload 4287 r0; CombatRoll 706F r2; Flashbang FF88 r0 / r1, E5D5 r1 / r2, A55C r4 / r3, CC06 r5 / r7, ECA2 r2, DB42 r3; Deadeye EE96
r0, 4EEC r1, DFD2 r3, 7C32 r1, 006E r1, 8E96 r3, 59BA r4, 06FF r6, 35DA r2.

Proposed palettes (`HeroPalettes`, stops t, r, g, b; "/peak" = normalised by the row's HDR peak so the hue survives; "top = 75 %"
= the data's white HDR core dropped and its 75 % colour put at 1, ours; the rest as the row reads):
```
CS_LINE              = {0,0,0,0, .25f,0,.7f,.971f, .5f,0,.568f,.938f, .75f,0,.438f,.906f, 1,0,.306f,.878f};      // 0FEF r4 /peak: cyan to blue
CS_LINE_ENEMY        = {0,0,0,0, .25f,.957f,.147f,0, .5f,.913f,.059f,0, .75f,.896f,.021f,0, 1,.888f,.001f,0};   // C54D r4 /peak: orange to red
CS_DEADEYE_LINE      = {0,0,0,0, .25f,.972f,.271f,0, .5f,.944f,.18f,0, .75f,.916f,.091f,0, 1,.888f,.001f,0};    // 59BA r4 /peak
CS_SMOKE_LINE        = {0,0,0,0, .25f,.362f,.361f,.358f, .5f,.36f,.36f,.36f, .75f,.36f,.36f,.36f, 1,.36f,.36f,.36f}; // B2E7 r7 /peak: grey
CS_SILVER_LINE       = {0,0,0,0, .25f,.119f,.342f,.74f, .5f,.155f,.272f,.48f, .75f,.152f,.211f,.316f, 1,.15f,.15f,.15f}; // 734D r7 /peak
CS_DEADEYE_TUBE      = {0,0,0,0, .25f,1,.285f,.081f, .5f,1,.285f,.081f, .75f,1,.285f,.081f, 1,1,.285f,.081f};    // 06FF r6: orange
CS_MUZZLE_FLASH      = {0,0,0,0, .25f,1,.372f,0, .5f,1,.702f,.023f, 1,1,1,.106f};                                // 3352 r1, top = 75 %
CS_MUZZLE_FLASH_C    = {0,0,0,0, .25f,1,.127f,.035f, .5f,1,.285f,.077f, 1,1,.756f,.437f};                         // B2A1 r1, top = 75 %
CS_FAN_FLASH         = {0,0,0,0, .25f,1,.476f,.007f, .5f,1,.496f,.015f, .75f,1,.561f,.068f, 1,1,.879f,.52f};     // BD91 r1
CS_MUZZLE_SPARK      = {0,0,0,0, .25f,1,.259f,0, .5f,1,.393f,0, .75f,1,.525f,0, 1,1,.888f,.584f};                 // 8370 r2
CS_MUZZLE_RING       = {0,0,0,0, .25f,1,.379f,0, .5f,1,.61f,0, 1,1,.817f,.194f};                                  // 7147 r6, top = 75 %
CS_MUZZLE_SMOKE      = {0,0,0,0, .25f,1,1,.381f, .5f,1,1,.605f, .75f,1,.908f,.551f, 1,.862f,.669f,.407f};         // B999 r7: cream to tan
CS_HIT               = {0,0,0,0, .25f,.006f,.471f,1, .5f,.032f,.494f,.993f, 1,.512f,.71f,.83f};                   // 3949 r1, top = 75 %
CS_HIT_ENEMY         = {0,0,0,0, .25f,.917f,.01f,.003f, .5f,.997f,.021f,.005f, 1,1,.239f,.019f};                  // 606A r1, top = 75 %
CS_HIT_SHARD         = {0,0,0,0, .25f,1,.476f,.007f, .5f,1,.495f,.015f, .75f,1,.678f,.255f, 1,1,.879f,.52f};      // 275F r1
CS_HIT_SHARD_ENEMY   = {0,0,0,0, .25f,.917f,.01f,.003f, .5f,.997f,.021f,.005f, .75f,1,.239f,.019f, 1,.982f,1,.048f}; // 2559 r1
CS_HIT_DECAL         = {0,0,0,0, .25f,0,1,1, .5f,0,1,1, .75f,0,.559f,1, 1,0,0,1};                                 // 5C90 r2: cyan to blue
CS_HIT_DECAL_ENEMY   = {0,0,0,0, .25f,1,1,0, .5f,1,1,0, .75f,1,.559f,0, 1,1,0,0};                                 // 6F1B r2: yellow to red
CS_RELOAD_SMOKE      = {0,0,0,0, .25f,1,.284f,.214f, .5f,1,.352f,.248f, .75f,1,.419f,.283f, 1,1,.486f,.317f};     // 4287 r0: peach
CS_SILVER_ARMED      = {0,0,0,0, .25f,.002f,.014f,.18f, .5f,.01f,.387f,.599f, 1,.118f,1,1};                        // CB3F r4, top = 75 %
CS_SILVER_ARMED_ENEMY= {0,0,0,0, .25f,.076f,.001f,.001f, .5f,.325f,.013f,.005f, 1,1,.227f,.13f};                  // CB3F r5, top = 75 %
CS_BLEED             = {0,0,0,0, .25f,1,.282f,.869f, .5f,1,.231f,.933f, .75f,1,.167f,.973f, 1,1,.081f,.972f};     // F7C6 r0: pink to magenta
CS_BLEED_SPARK       = {0,0,0,0, .25f,.484f,0,0, .5f,.968f,0,0, 1,1,.132f,.132f};                                 // 8FF6 r1, top = 75 %
CS_ROLL_DUST         = {0,0,0,0, .25f,.33f,.293f,.252f, .5f,.661f,.586f,.504f, 1,.986f,.875f,.753f};              // 706F r2, top = 75 %
CS_FLASH             = {0,0,0,0, .25f,.419f,.245f,.005f, .5f,1,.748f,.016f, 1,1,1,.064f};                          // FF88 r0, top = 75 %
CS_FLASH_ENEMY       = {0,0,0,0, .25f,1,.052f,.013f, 1,1,.404f,.07f};                                             // FF88 r1, top = 50 %
CS_FLASH_DOME        = {0,0,0,0, .25f,.403f,.161f,0, .5f,.682f,.302f,0, 1,.879f,.485f,0};                         // E5D5 r1, top = 75 %
CS_FLASH_DOME_ENEMY  = {0,0,0,0, .25f,1,.158f,.071f, .5f,1,.256f,.123f, 1,1,.411f,.202f};                         // E5D5 r2, top = 75 %
CS_FLASH_RAYS        = {0,0,0,0, .25f,0,.158f,.887f, .5f,.044f,.284f,1, 1,.124f,.5f,1};                           // A55C r4, top = 75 %
CS_FLASH_STREAK      = {0,0,0,0, .25f,.052f,.125f,.42f, .5f,.033f,.243f,.733f, 1,.011f,.475f,1};                   // CC06 r5, top = 75 %
CS_FLASH_STREAK_ENEMY= {0,0,0,0, .25f,.898f,.21f,.025f, .5f,1,.403f,.061f, 1,1,1,.3f};                            // CC06 r7, top = 75 %
CS_FLASH_TRAIL       = {0,0,0,0, .25f,1,.139f,.006f, .5f,1,.139f,.006f, .75f,1,.139f,.006f, 1,1,.139f,.006f};     // ECA2 r2 /peak: orange
CS_FLASH_TRAIL_B     = {0,0,0,0, .25f,1,.728f,.269f, .5f,1,.728f,.269f, .75f,1,.728f,.269f, 1,1,.728f,.269f};     // DB42 r3 /peak: warm yellow
CS_DEADEYE_EYE       = {0,0,0,0, .25f,1,0,0, .5f,1,0,1, 1,.542f,0,0};                                             // EE96 r0, its closing black dropped (magenta at .5: L)
CS_DEADEYE_STAR      = {0,0,0,0, .25f,1,.708f,.255f, 1,1,.904f,.354f};                                            // 7C32 r1, top = 50 %
CS_DEADEYE_GLOW      = {0,0,0,0, .25f,1,.68f,0, 1,1,1,.264f};                                                     // 4EEC r1, top = 50 %
CS_DEADEYE_GLOW_ENEMY= {0,0,0,0, .25f,.557f,.002f,0, .5f,1,.121f,0, 1,1,.435f,.003f};                             // DFD2 r3, top = 75 %
CS_DEADEYE_DECAL     = {0,0,0,0, .25f,1,.74f,0, .5f,1,.48f,0, .75f,1,.225f,0, 1,1,0,0};                           // 35DA r2: yellow to red
```
Team / enemy pairs (M): `CS_LINE` / `CS_LINE_ENEMY` (Deadeye's second line `CS_DEADEYE_LINE` is its own), `CS_HIT` / `_ENEMY`,
`CS_HIT_SHARD` / `_ENEMY`, `CS_HIT_DECAL` / `_ENEMY`, `CS_SILVER_ARMED` / `_ENEMY`, `CS_FLASH` / `_ENEMY`, `CS_FLASH_DOME` /
`_ENEMY`, `CS_FLASH_RAYS` / (A55C r3, raw in `fx_palettes.txt`), `CS_FLASH_STREAK` / `_ENEMY`, `CS_DEADEYE_GLOW` / `_ENEMY`,
`CS_MUZZLE_FLASH` / `CS_MUZZLE_FLASH_C` (L: the second may only be a back layer). The hindered look keeps Brigitte's
`Palette.NATURAL` (colour textures 247C / 248B).
