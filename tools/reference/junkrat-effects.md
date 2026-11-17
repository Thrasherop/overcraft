# Junkrat's ability effects in the game data

What Junkrat's effects are made of in real Overwatch, where the extracted files are, and how each would be rebuilt in Minecraft
(a `JunkratFx` beside `TorbjornFx` / `IllariFx`; today the server sends vanilla `EXPLOSION` / `EXPLOSION_EMITTER` / `FLAME` / `CLOUD`
from `junkrat/Junkrat.java`, issue #150). Read 2026-11-17 from the archived offline build 154088. Overwatch and Battle.net were checked
closed (`Get-Process Overwatch, Battle.net`) before the TankLib run, and `owinit.py` refuses on its own; everything was read only,
offline, through TankLib/DataTool. Text only in the repo: masks, ramps and meshes stay in the owner's archive and reach the game through
the local asset pack. Format: [illari-effects.md](illari-effects.md) / [torbjorn-effects.md](torbjorn-effects.md). Data map:
[junkrat-map.md](junkrat-map.md); public numbers: [junkrat.md](junkrat.md); method: `techniques/rendering/rendering.md` "Effects and
glows".

Confidence: **H** read in the data, **M** inferred, **L** guess. "Size" is the spawn size (a 1 m quad at size 0.5 is 0.5 m; a ribbon's
is its width; a mesh's size scales its own extent). "Ours" marks what no data gives. `XXXX rN` = the colour LUT (last 4 hex digits of
`ramps\8...XXXX.json`) and its row; hex runs are that row at 0 / 25 / 50 / 75 / 100 %. "hp" = hardpoint. `uniform(a, b)` is printed
as read (M: a +- b, as Torbjörn's lives). `ECIN` / `ECSL` sizes are set by data flow from the entity (not decoded, L).

## Files and scripts
- `E:\Overwatch Archive\research\junkrat\fx\Junkrat\<Ability>\` (`FragLauncher`, `ConcussionMine`, `SteelTrap`, `RIPTire`,
  `TotalMayhem`, `Perks`): `textures\` (PNG, flipbook frames `_1.._n`), `ramps\` (float LUTs as JSON), `models\Models\<id>.00C\`
  (`.owmdl`), `manifest.json`. A texture used by several abilities is saved in each.
- `research\junkrat\work\`: `fx_cards_{frag,mine,trap,tire,mayhem}.txt` (one card per renderer: what this note is written from) and
  their `_compact.txt`, `fx_overview.txt`, `fx_summary2.txt|json`, `fx_meshes2.json`, `fx_entities.txt`, `fx_clip_effects.txt`,
  `fx_effects\` (decoded), `fx_extract_log.txt` / `fx_extract_log2.txt`, `fx_palettes2.txt` (the `JR_*` stops below).
- Scripts `research\junkrat\scripts\fx\`: D.Va's `fx_dva_*` copied as `fx_junkrat_*` (`fx_junkrat_extract.py` holds his effect list
  with each one's graph node). Run: `fx_junkrat_extract.py [Ability ...]` (kiriko-anim venv, `-I`); then under `py -I run.py`:
  `fx_junkrat_summary2.py`, `fx_junkrat_overview.py`, `fx_junkrat_card.py <ids>`, Torbjörn's `fx_torbjorn_cardsc.py <cards>` to
  compact, `fx_junkrat_pal.py` (new this pass: hue-clamped stops ending on their colour; rules in its header) and
  `..\..\pharah\scripts\fx\fx_pharah_rows.py junkrat <lut4 | t:texid>` (a LUT's rows in all four channels, or a colour-ramp texture).
- The first run (2026-11-16) stopped inside RIPTire; this pass re-ran RIPTire, TotalMayhem and Perks. Counts: **78 effects in the
  manifests (the list + sub-effects 4047, 35DC, 1647), 1 missing: 37AC** (0234 #417 at hp 0079, TankLib FileNotFound: a removed node,
  H). 27 draw something; the rest are sounds only or empty (1646, 16DC the bounce, 6599 / 6BA4 / 6BA8, 15F6, 2860 / 2B1E / 2B44, the
  fire clips 08B0 / 1710-1713, 2A0E the stick, 1F60 / 65F6 / 19DD / 26A3 / 3B1A / 659C, 2862 / 2B21, 17CD / 17FD / 16C2 / 5F58, 0526 /
  125E / 124C / 3827, the tire's roll / jump clips, 0F2B / 19A0 / 6C6F, 6511).

## His look
- **One fire family for everyone, a redder twin (H rows, M roles):** his fire is orange-gold; many renderers carry a second row of
  the same shape that is redder (frag glow 1FA3 r1 `#FF0000 > #FF6300 > #FFAD00 > #FFE100 > #FFFF00` / r2 `#FF0000 > ... > #FE9400`;
  tire fuse DA5F r0 / r1; mine flames 803B r1 / r2; tire ribbons 3422 r7 / r8). As Torbjörn's: the first = his side, the second =
  enemies (M). **Only the thrown trap's ribbon is clearly two-coloured** (D3BB r3 white > gold / r4 white > pink-red), and the mine's
  ribbons (AFB3 r2 olive-cream / 63C1 r2 dark red > cream). Draw fire rows added; smoke rows blended (`Fx.soft`).
- **Explosions share one kit (H):** the colour fireball flipbook **47C4** (11 frames, BC7 colour) x AFF (normals) for the frag, mine,
  tire and Mayhem bombs; flash **2613A** (frag 4-5 m, mine, tire 6-7.5 m); flame tongues **14F9** (2); sparks **2C009** (a 64x128
  streak); smoke **1DFCB** (32) x 1FC65 x 1DFCA; the scorch decal **22BE / 31E8 / 22C0**; a distortion sphere (18F81, not drawn).
- **Channel-shifted LUTs (M, as Tracer's):** 5688 / FEA8 / DCDF (the detonator: `#00D801...` printed, red > white in channels 2-4),
  ABBB (the big flash), 9154 (trap debris). Many rows are parameter rows (constant first channel, e.g. DB55 / E127 on the sparks,
  2DF2 on the smoke, B7FD): their colour is the other LUT on the renderer.
- **Hardpoints (H ids, M roles):** the launcher's **0006** (muzzle) and **006F / 007E / 007F** (the drum: reload sparks), all in
  `junkrat_weapon.glb`; his **000B** (left hand: the detonator, the mine and trap in hand); the frag's **0076**; the mine's and trap's
  **0011**; the tire's **0079** (the fuse, in `junkrat_tire.glb`) and 0004; the caught's **0001 / 0019 / 001A**; 00A4 roots (sounds).

## Frag Launcher (0015 / 022B; frag 0385, its child 03C9)
| Part | Effect | Rebuilt (replaces) |
|---|---|---|
| Launch | **1468** (0385 `m_A18274A3`, 1 s, at the frag's spawn = the muzzle) H: streak flashes **4C09 0.25 m and 0.18 m stretched x11-13, 0.09-0.13 s** (4286 r0 `#000000 > #FFBD00 > #FFC600 > #FFCF00 > #FFE400`); burst mesh **27B3** (0.2 x 0.17 x 0.2) x 11F7F (2) **3 at size 1-1.35**, 0.07-0.11 s, cone 10 deg (8197: tint 11, HDR white); smoke puff B08 x 5429 (4) on quad F78 **0.65 m, 0.34-0.52 s, 1 m/s**, stretch 11-13 (23E7 r1 greys); sparks 1F35 x colour ramp 3127, 0.1-0.17 m, 1.9-3.8 m/s, 0.15-0.38 s, stretch 4-9.5; two lights 0.2 / 0.33 s; sub-effect 4047 (sound). The fire clips 08B0 / 1710-1713 are empty (H) | at the posed muzzle 0006 from `weapon` (Junkrat.java:202-206, after `frag.fire`): the two streaks (`cat_soft`, `JR_FRAG_MUZZLE`), the burst mesh at size 1.2 (`jr_muzzle_burst`), one smoke (`ana_puff#4`, soft), 4 sparks; a third the size in his own view (ours, as Tracer's); no vanilla call today |
| In flight | **051B** (03C9 STUEffectComponent, at the frag's 0076) H: glow **18BAD** (4) 10 /s, 0.2 s, size data flow (1FA3 r1 / r2: red > orange, HDR 2.85); soft glow 4C09 5 /s 0.3 s (4967 r1 red > yellow); **sparks 1F35 39-71 /s**, 0.2-2 m/s, 0.08-0.13 m, cone 15 deg (9CE5 r1, HDR 5.4); **ribbon 7F5 x 2A480 x 4DB x 1EF26 0.55 m wide, 60 points/s, 0.77 s** (A8BE r8 orange-red > dark brown by age, r7 cream head) | per frag each frame (ProjectileModelRenderer case `FragEntity`, not a bomb): a 0.3 m glow on it (`jr_frag_glow#4`, ours size), `Fx.trail` 0.3 m / 0.77 s (`torb_trail`, `JR_FRAG_TRAIL`, soft; ours narrower than the data's 0.55 m: the frag is 0.2 m), a spark a tick (ours: a third) |
| Bounce | **16DC** (`m_93A2D5E5`): sounds only (H) | nothing new (`fragBounced` :244-245 keeps its sound) |
| Burst | **13C6** (0385 `m_6CB564CC`, 4 s; creature and world share it, H): fireball **47C4** (11, colour) x AFF **1.2 +- 0.25 m, 0.35 +- 0.15 s**, 0.8-1.5 m/s, stretch 3, cone 32 deg (5737 r2 `#FFFFFF > #FFE1AB > #FF7D77 > #FF615C`, HDR 12; r1 cream-peach); **sparks 2C009 20 at 1.4-8.9 m/s**, 0.25-0.45 s, 0.04-0.05 m, stretch 1.2-2, cone 95 deg (rows DB55 / E127 are parameters: colour `JR_SPARK`, M); **smoke 1DFCB (32) x 1FC65 x 1DFCA on quad 101B6, 1.25-1.53 m, from 0.067 s, 0.8-1.1 s, 2.7-6.3 m/s**, stretch 3.9, cone 90 deg (8B8E r2 rose-grey); **flash 2613A 4-5 m, 0.15-0.17 s** (70B9 r1 `#4A1800 > #D47300 > #FF9C00 > #FFFF00 > #FFFFFF`, HDR 5); scorch decal 22BE / 31E8 / 22C0 **1.5 x 2 m (M), 4 s** (C7EA tint (0.61, 0.66, 1.11): dark); distortion DBF 6.5 m 0.25 s; light 4.9 m 0.33 s | replaces `EXPLOSION` + 6 `FLAME` (Junkrat.java:263-264) with `Effects.play(level, "jr_frag_burst", at, normal)`: the flash 4.5 m at 0.3 alpha (ours), the fireball as a flipbook 1.2 m (`jr_fireball#11`, colour sprite, `JR_FRAG_FIRE`), 12 sparks (`jr_spark_streak`, ours: 20 read as busy), 2 smoke (`brigitte_smoke#32`, soft, `JR_FRAG_SMOKE`), the decal on a block face (`jr_scorch`), no distortion |
| Reload | weapon clips **08AD** (1P, 1.5 s) / **16AA** (3P, 1.43 s) on the launcher's 0006 / 006F / 007E / 007F H: **15 sparks 1F35 x 3127 at 1.2 s**, 0.5-1.2 m/s (3P 1-2.2), 0.04-0.07 m, 0.23-0.49 s (195E r0 yellow > red); **12 at 0.83 s** (3P 0.87 s), 0.9-1.3 m/s, spin (33D8 r0); a steam puff 4DB x 218F x B0B **60 /s + 3 from 0.23 s for 0.1 s** at 0006 -0.24 m (3P 90 /s at -0.49 m), 0.17-0.22 m, stretch 11-13 (218F light grey); an entity ref 10A2 (L: the drum's spin). 0543 / 1A0D (his clips): sounds only | `startReload` (:213, the `reload` sound :224): the two spark bursts at the posed drum 006F and the puff at 0006, scaled to the reload's length (ours, as Torbjörn's); `JR_RELOAD_SPARK`, `_B`; no vanilla call today |

## Concussion Mine (096A / 096B; mine 0F9B, cosmetic 109F)
| Part | Effect | Rebuilt (replaces) |
|---|---|---|
| Throw | clips **08D7** (1P 1DC4: the mine model 2959 in hand for 0.13 s) / 1A3A (3P): sounds only (H) | nothing (`mineCast` :300-303 keeps the sound) |
| Detonator lit | **625C** (096A #645 / #666 at his **000B**, held while a mine is out, M from the v2281 gate) H: glows A7B6 3 /s 0.68-1.23 s **0.08-0.23 m** and 4 /s 0.25-0.31 s 0.05-0.1 m; 4 stars 2F4F0 0.19-0.24 m 0.15-0.21 s; lights (DCDF / 5688 / FEA8 r1 shifted: **red to white**) | client: on every Junkrat whose `ConcussionMineEntity` is out (as `HeroAnimator` finds it, :1906), at his posed 000B: a 0.15 m glow (`glow`, `JR_DETONATOR`) pulsing 3 /s, ours alpha 0.8 |
| In flight / stuck | **156A** (109F STUEffectComponent, 096B #469, on the mine's **0011**, looping) H: **blinks**: 2D6CC **4 /s living 0.26 s, size 1** (CC40 r1 black > red > orange > yellow, HDR 4.9) and 37C36 4 /s 0.17 s; 2D6CC / 37C36 **2 /s 0.52 / 0.35 s, 0.5 m** (1976 r0 / 4FF9 r0 gold, HDR 2.3); 210FD 4 /s 0.26 s (2994 r1 red, HDR 21.6) and 2 /s 0.29 m; three ribbons DC90 x 800E9688 (C28 / C30) and 1344 (C36) **1.91 m (M: scaled by the mine), 60 points/s, 0.38 s** (AFB3 r2 olive-cream / 63C1 r2 dark red > cream: twins, M) | per mine each frame (ProjectileModelRenderer case `ConcussionMineEntity`): a red blink 0.25 m 4 /s (`jr_light`, `JR_MINE_LIGHT`) and a gold 0.35 m 2 /s (`lw_burst`, `JR_MINE_BLINK`) at its 0011 (ours sizes); while it flies, `Fx.trail` 0.15 m / 0.38 s (`sombra_trail_band`, `JR_MINE_TRAIL` / `_ENEMY`; ours width) |
| Stuck | **2A0E** (`m_306ABA15`, hp 0004): sounds only (H) | nothing (`mineStuck` :307-308) |
| Blast | **151A** (0F9B `m_6CB564CC` = 096B #301, 3 s, +0.2 m; the trap's #827 plays it too, L a non-live perk) H, sizes by data flow (ECIN, L: the 3 m radius): **fireball 47C4 x AFF 1 burst 0.4-0.55 s**, stretch 4, cone 20 deg (3301 r0 dark red > red-orange; 55DD r2 tint (8, 0.3, 0)); **flames 14F9 11 at 3.6-5.9 m/s all ways + 8 at 3-4 m/s**, 0.35 +- 0.15 s, stretch 2 (803B r1 orange > cream / r2 redder, HDR 9.6); glow 4C09 0.35 s (2855 r1); lit smoke 1DFCB 1 at 0.067 s, 0.4-0.65 s, 4.4-5.8 m/s, stretch 3.3-4.2 (D480 r0 red > gold); **sparks 2C009 25**, 0.2-0.5 s, 0.04-0.05 m, cone 95 deg (7975 r1 red > orange > yellow, HDR 5.4); flash 2613A 0.18-0.2 s (2A99); scorch decal 3 s; distortion; light 1.5 > 5.6 m 0.23 s. Its scale `1 + gv(00EA = [0, 0.3, 0.6])` (M: Stadium sizes; 1 live) | replaces `EXPLOSION` + 12 `CLOUD` (Junkrat.java:374-375) with `"jr_mine_blast"`: the flash 3 m (ours, the radius), fireball 1.5 m (`jr_fireball#11`), 10 flame tongues outward (`lw_thorn_flame#2`, `JR_MINE_FLAME`), 12 sparks (`jr_spark_streak`, `JR_SPARK`), one lit smoke (soft), the decal |
| 64C5 | 096B #506 after the blast, gated on perk 252E (not a live perk: Stadium, L): ring mesh 296B 1.35 m + glow A7B6 3.75 m | not drawn |

## Steel Trap (09BA / 09BB; trap 11A9, root status 09BC)
| Part | Effect | Rebuilt (replaces) |
|---|---|---|
| Throw | clips **1862** (1P: model 28CD in hand 0.07 s) / 1A3E / 186F: sounds only (H) | nothing (`throwTrap` :390-394) |
| Thrown | **6705** (11A9's child 6707, at its **0011**) H: **ribbon 31AF x 18E48 1.2 m wide, 30 points/s, 0.5 s** (D3BB r3 white > `#FFDA65 > #FFD853 > #FFD53B > #FFD300` / r4 white > `#FFAAAA`: twins, M; r1 tint (2, 0.5, 0)) | `Fx.trail` 0.3 m / 0.5 s (ours narrower: the trap is 0.6 m) behind the flying trap (case `SteelTrapEntity`, while not landed), `cat_cloud`, `JR_TRAP_THROWN` / `_ENEMY` |
| Set / armed | 17FD (#600, the armed state's begin) and 5F58: sounds only (H) | nothing (`trapLanded` :397-400) |
| Snap | **16E0** (09BB #561 at the end of the armed state #452, beside a health change and message 0053: M the snap; it may also be the break, L) H: **dust 1DFCB x 1FC65 5 at 2.3-3.2 m/s, 1.8-2.2 m, 0.5-1.4 s**, stretch 2.6, cone 45 deg (B980 r1 greys); **sparks 2C009 18 at 2-5 m/s**, 0.08-0.16 m, 0.5-1.4 s (529A is a parameter row: `JR_SPARK`, M); **metal bits 22FD (8, colour) x 22FE 15 at 3.1-5 m/s, 0.2-0.28 m, 0.7-0.94 s**, spin, cone 40 deg (9154 r0 shifted: black > orange) | at `trapSprung` (:427-435, no particle today): 2 dust (soft, `JR_TRAP_SMOKE`), 10 sparks, 8 bits (`jr_debris#8`, colour) |
| The chain | **16D5** (09BB #545 STU_9932D728 at hp 0011: a beam from the trap to the caught, M) H: ribbon E71 **0.117 m wide** on the chain set 2A8F (normals) / 2A92 (colour) / 2A90 / 2A91 (masks), length ECIM(73) (F309 r1 dark yellow > yellow, r0 red) | while it holds one: a 0.1 m band trap 0011 -> the caught's 0001 (`SombraFx.tracer`'s band with `jr_chain`), `JR_TRAP_CHAIN`; needs the caught synced (`SteelTrapEntity.caught()` is server-side: L) |
| On the caught | **19F4** (09BC #534, the root status, 5 s, hp **0001 / 0019 / 001A**) H: plus-marks mesh **E0AA** x 391F2 **3 > 6 /s** (data flow 5FD: M the root's time left), 0.82 +- 0.2 s, 0.57 +- 0.15 m/s, size 0.7-1.9, stretch 1 > 4.9 (33DC r0 orange, rising then falling); **a 2 m ring** (quads 801: 8FF5 x 5D0 and 8FF5 x 6429 x 4DB, held 5.8 s; 8FF5 a violet-blue colour ramp `#000003 > #5200AD > #7823DB > #715CF7 > #6D70FF`); wisps 2FF91 x 27E5D x 391F2 x 27E5E x 7A9 10 /s, 1.25 +- 0.5 m, 0.5 +- 0.4 s (5443 r3 violet); hemisphere mesh 963D x 299E1 x 299E9 x A7B6 (6592 r0 pale pink). 16C2 (#459): sounds | each rooted creature each tick (client, while `Junkrat.rooted`: its jump modifier is synced through attributes, M): the 2 m ring flat at its feet (`ana_ring`, `JR_TRAPPED_RING`), a plus mark every 4 ticks (`ana_plus`, `JR_TRAPPED`), a wisp every 2 ticks (`band`, `JR_TRAPPED_B`) |
| Released / broken | `trapReleased` :480-484 / `trapBroken` :487: no own effect beyond 16E0 (M) | the snap burst at a third on a break (ours) |

## RIP-Tire (0232 / 0233 / 0234; tire 039C, its child 0FDB)
| Part | Effect | Rebuilt (replaces) |
|---|---|---|
| Cast | 3P clip **1749** (32A9 tire_cast, 2.73 s) H: **at 1.3 s** (the fuse lit, M) sparks 1F35 x 22A8 **31-42 at 2.6-8.1 m/s**, 0.34-1.03 s (A13B r3 cream > brown, tint (13.4, 11.8, 3.7)); flame 22A8 x 4DB x 4837 (2) x AFF 5 at 0.16-1.6 m/s 0.16-0.42 m (4E34 r7 / 25CF); 4980 (50 frames) smoke 16 at 1.8-4.4 m/s 0.21-0.39 m (3796 r0 red > gold); at 0.6 s 16 flames 14F9 0.28-0.36 m (586F r0); a fuse ribbon F84 x 1DD4 / 1DD6 / 1DD3 0.025 m from 0.93 s for 0.83 s (B611 tint (4, 0, 0)); **decal 1647 at 0.57 s for 4.4 s**, 0.77 m under (EE8E); camera shakes at 0.5 / 1.87 s. Tire 0234 #272's **4557** (its anim begins: M the tire set down): 3 dust puffs **BE5** (colour) 1.1-1.2 m, 0.33-0.4 s, 0.9-1.25 m/s (98C8 r0 red-orange) | at `castTire` (:519, the `tire_cast` animation): the 1.3 s spark burst at his posed hand 000B (ours: 15 sparks), the 3 dust puffs where the tire appears (`dust`, soft); no decal (L, it is under him) |
| The tire | **156F** (0FDB STUEffectComponent, at the fuse **0079** + small offsets) H: **ribbons 7F5 x 1EF26 x 2A480 x 4DB: 0.99 m and 0.35 m (x2, one from 0.57 s) wide, 128 points/s, 0.77 s** (9804 r6 / 3422 r7 orange > black by age; 3422 r8 redder twin); fuse glows 4C09 0.2 m 5 /s 0.3 s (DA5F r0 red > yellow / r1 red); glow balls 1C7A x 3699E on mesh **1385 1.65 / 1.8 m**, 5 /s, 0.3 +- 0.2 s (B8EA r5 grey > orange > grey); **sparks 183A7 25-35 /s at 5-15 m/s**, 0.25-0.75 s, 0.05 m, stretch 1-2, cone 10 deg at (0, 0.15, -0.35) (D4A4 r1 orange-peach, HDR 10); two lights 5 m at 0079 +0.93 m | per tire each frame (`LifeweaverPropRenderer.tire` beside its model, at its posed 0079): `Fx.trail` 0.3 m / 0.77 s smoke (`torb_trail`, `JR_TIRE_TRAIL`, soft; ours narrower), a 0.2 m fuse glow 5 /s (`cat_soft`, `JR_TIRE_FUSE`), a 1 m glow ball at 0.3 alpha (`cat_sphere` / `glow`, `JR_TIRE_GLOW`), 3 sparks a tick (`cat_streak`, `JR_TIRE_SPARK`); replaces nothing (no particle today) |
| Jump / land / roll | tire clips 16E9-16ED, 161F / 161E (his drive / end): sounds and one camera shake only (H) | nothing (`driveTire` :595 / :606 keep the sounds) |
| Destroyed | **1994** (0233 #520 after game message 003E then a 0.12 s wait: M the tire shot down) H: debris 22FD (8) 20 at 3-7.7 m/s, 0.24-0.46 m; smoke 9EF (4, colour) x 4DB x B0B 10 at 3.4-7.5 m/s 1.6-2.1 m stretch 4.6; **40 sparks 1F35 x 31C0 at 4.2-5.3 m/s**, 0.33-0.65 s, 0.056 m (0957 r2 tint (11.6, 7.45, 4.22)); a material effect (the tire flashing) | at `tireBroken` (:636-638, sound only today): 8 bits (`jr_debris#8`), 3 smoke (`ana_smoke#4`, soft), 15 sparks |
| **The blast** | **15D2** (0233 #356 inside the explode state #409, 2 s, at 00A4 +0.32 m) H: **fireball 47C4 x AFF 1.3-2.7 m**, 0.4-0.53 s, stretch 4, cone 20 deg (FC57 r1 cream > peach HDR 12, 2945 r0 dark red > red); glow A7B6 **1 m** 0.18 s (2DC2 r0 brown > gold > cream, HDR 4); **flash 2613A 6-7.5 m, 0.3-0.32 s** (ABBB r0 shifted, HDR 5); **sphere 1298 on mesh 085B, 4 m, 0.2 s** (3E05 r0 red > orange, HDR 5); **ground ring 31C0 x 4D4 x 4DB x 4CB7 on mesh 296B (1.28 m) at size 2 = 2.6 m, 0.3-0.35 s** (B894 tints (5, 0.5, 0) > (4, 1, 0)); **flames 14F9 5 at 8.9-11.2 m/s**, 0.3 +- 0.1 s, 1.0 +- 0.7 m, all ways (E413 r1, HDR 9.6); **smoke 1DFCB 3 at 0.23 s, 2 m, 1-1.7 s, 4.9-9 m/s** (1BFA r3 by age); **sparks 2C009 10 at 3-18 m/s** + **7 debris at 6-16 m/s living 2.5-3.5 s** with sub-effect **35DC** (a glow 4C09 3-9 /s and a smoke ribbon 7F5 0.5 m 200 points/s 0.6 s behind each, M); scorch decal 22BE / 31E8 / 22C0; light 9.1 m 0.23 s; distortion C4A (8004DA1E). #403 / #492: sounds | replaces `EXPLOSION_EMITTER` + 40 `FLAME` (Junkrat.java:621-622) with `"jr_tire_blast"`: the 7 m flash at 0.25 alpha (ours), the 4 m sphere (`jr_sphere` mesh or `cat_sphere`, `JR_TIRE_SHELL`, 0.2 s), the ground ring (`brigitte_ring` mesh at 2, `jr_ring_noise`, `JR_TIRE_RING`), the fireball 2.5 m, 5 flame tongues, 3 smoke (soft), 10 sparks, 7 debris each with a short `Fx.trail` (ours: 0.15 m, 0.6 s, `JR_TIRE_TRAIL`), the decal |

## Total Mayhem (022A; death bomb 0515, its child 130A) and his perks
| Part | Effect | Rebuilt (replaces) |
|---|---|---|
| The drop | 0F2B (022A #21 / 51E8 #487), 19A0, 6C6F: sounds only (H) | nothing (`mayhem` :864-877 keeps `mayhem.drop`) |
| A bomb in flight | **1792** (130A) H: **ribbons 7F5 x 9ED (colour) x 4DB 0.38 m, 60 points/s, 1.5 s** (77EF r0 dark red > orange; 0CB7 r0 gold head by age); glow 29F7 x 2A02 **1.5 m** 5 /s 0.3 s (86D2 tints (3, 0.45, 0.125) / (2.97, 0, 0); 023F r0) and 29F7 x 2D7F 0.46 m; sparks 2C009 16-128 /s (data curve), 0.75-2 m/s, 0.04-0.08 m (A887 r0) and 39-71 /s (E490 r0) | per bomb (case `FragEntity` with `bomb()`): `Fx.trail` 0.15 m / 0.5 s (ours shorter: 3 bombs at once), a 0.5 m glow (`lucio_spark`, `JR_BOMB_GLOW`, ours size), a spark a tick |
| A bomb's burst | **14CC** (0515 `m_6CB564CC`, 2 s) H, sizes data flow: flash 29F7 0.33 s (0B6C r0 black > red > orange, HDR 8); fireball 47C4 x AFF 30 /s + 1, 0.33-0.48 s, stretch 3, cone 56 deg (AB03 tints (8, 7.2, 4.3) / (8, 0.74, 0.74)); **sparks 2C009 20 at 5.4-8.7 m/s**, all ways (FE9B r1 red > gold); distortion E20D (`torb_slag_blob`, not drawn); smoke 1DFCB 20 /s + 2 from 0.13 s, stretch 6; two lights 2.3 m 0.33 s | replaces `EXPLOSION` (Junkrat.java:257) with `"jr_bomb_burst"`: a 2 m flash (ours, the 2 m radius), a 1 m fireball, 8 sparks, 1 smoke |
| Nitro Boost (perk 252C) | **6740** (51E8 #442, 10 s on the tire's 0079 / 0004): smoke 30 /s + 6 for 0.27 s, the fuse ribbon again, sparks, the 1.65 m glow ball 7 > 0 /s over 1.17 s, **speed lines E99F (3) size 10 at 50 /s and 200 + 300 /s small** for 1.17 s, sparks 183A7 40 /s, a flare **mesh 14346 (0.95 x 0.39 x 1.65) at size 3, once, 1.2 s** at 0004 (0.8, 1.3) (397A r3 / r4 tint 15). 6511 (#410): sound. 6D9A (#489): his own screen (ECIM 40) | at Nitro (:677): the flare mesh (`jr_nitro_flare`, `JR_TIRE_FLAMES`) once, speed lines (`cat_speed_line#3`) for 1.2 s in the driver's view only |
| 15A7 | 0A1E #258 on a creature under knockback status 021D (hp 0001): a smoke trail 4F0 (4) x B08 16 > 8 /s, 1.5 > 2.9 m, stretch 2 (D7B7 r0) | not drawn (not asked; L which perk) |

## Where it hooks in (each vanilla call it replaces)
| Today (`src/main/java/dev/overcraft/...`) | Becomes |
|---|---|
| `junkrat/Junkrat.java:257` `EXPLOSION` (a Mayhem bomb) | `Effects.play(level, "jr_bomb_burst", at, ...)` (14CC) |
| `junkrat/Junkrat.java:263-264` `EXPLOSION` + 6 `FLAME` (a frag) | `"jr_frag_burst"` with the hit face's normal for the decal (13C6) |
| `junkrat/Junkrat.java:374-375` `EXPLOSION` + 12 `CLOUD` (the mine) | `"jr_mine_blast"` (151A), normal = the mine's `stuckFace` |
| `junkrat/Junkrat.java:621-622` `EXPLOSION_EMITTER` + 40 `FLAME` (the tire) | `"jr_tire_blast"` (15D2 + 35DC) |
| none: frag launch (`weapon` :202-206) | `"jr_frag_launch"` at the muzzle, entity = him (the client poses `junkrat_weapon.glb`'s 0006) (1468) |
| none: frag / bomb flight | `JunkratFx.fragFlight` from `ProjectileModelRenderer` (case `FragEntity`, `bomb()` picks 051B / 1792) |
| none: reload (`startReload` :213) | `"jr_reload"` (variant = ticks): 08AD in his view, 16AA for others, at the drum 006F and muzzle 0006 |
| none: the mine (flight, stuck) and the detonator | `JunkratFx.mineTick` (case `ConcussionMineEntity`: 156A at its 0011); the detonator glow (625C) in `JunkratFx.frame()` on every Junkrat with a mine out |
| none: trap thrown / snap / chain / caught | trail on the flying `SteelTrapEntity` (6705); `"jr_trap_snap"` at `trapSprung` :427 (16E0); the chain (16D5) needs `caught()` synced (L); the root look (19F4) on every `Junkrat.rooted` creature |
| none: tire cast / the tire / shot down / Nitro | `"jr_tire_cast"` at `castTire` :519 (1749 at 1.3 s, 4557); `JunkratFx.tireTick` from `LifeweaverPropRenderer.tire` (156F); `"jr_tire_broken"` at `tireBroken` :636 (1994); `"jr_nitro"` at :677 (6740) |
- Hardpoints to pose (HeroModelRenderer's `POINTS`, as `SojournFx.POINTS`): his `hardpoint_000B`; `junkrat_weapon.glb`'s
  `hardpoint_0006`, `hardpoint_006F`; `junkrat_tire.glb`'s `hardpoint_0079`; the mine's and trap's `hardpoint_0011`, the frag's
  `hardpoint_0076` (all present in the pack's glbs).
- Palettes: the `JR_*` arrays below in a `JunkratPalettes.java` (as `TorbjornPalettes`), `ART` / `ART_ENEMY` lists as `DvaFx`'s.

## Not found / not done
- **37AC** (0234 #417 at the fuse 0079) is not in the build (H missing). The tire's 3P roll / jump clips carry sounds only.
- Sizes set by data flow (ECIN / ECSL: the mine's and bombs' whole blasts, the frag's flight glows), the trap chain's length input
  ECIM(73), the 19F4 rate input 5FD: not decoded (L: drawn from the blast radii, ours).
- Which side sees the redder twin rows (M, no blue / red pair as Tracer's); the trap's #561 as snap vs break (M / L); 156A's 1.91 m
  ribbon width (M: scaled by the mine).
- Material effects (1994's tire flash), the distortion layers (18F81, C4A) and his own screen layers (6D9A, ECIM 40): not drawable.
- Non-live perk / Stadium gates: 64C5 (252E), the trap's #827 explosion (151A on the trap), 15A7.

## For the asset pack (sources: add `E:\Overwatch Archive\research\junkrat\fx`)
Proposed `EFFECT_MASKS` lines (`name: ("Junkrat", <Ability>, <12-hex id>, frames)`):
```
"jr_fireball": ("Junkrat", "FragLauncher", "0000000047C4", 11),      # colour fireball flipbook (frag, mine, tire, bombs)
"jr_flash": ("Junkrat", "FragLauncher", "00000002613A", 1),          # blast flash (frag 4-5 m, mine, tire 6-7.5 m)
"jr_spark_streak": ("Junkrat", "FragLauncher", "00000002C009", 1),   # 64x128 spark streak (every blast, the snap)
"jr_scorch": ("Junkrat", "FragLauncher", "0000000031E8", 1),         # scorch decal mask (with 22BE normals)
"jr_scorch_b": ("Junkrat", "FragLauncher", "0000000022C0", 1),
"jr_muzzle_flash": ("Junkrat", "FragLauncher", "000000011F7F", 2),   # on the 27B3 burst mesh
"jr_frag_glow": ("Junkrat", "FragLauncher", "000000018BAD", 4),      # the frag's glow in flight
"jr_light": ("Junkrat", "ConcussionMine", "00000002D6CC", 1),        # the mine's blink
"jr_ring_noise": ("Junkrat", "RIPTire", "000000004CB7", 1),          # ground ring (on mesh 296B)
"jr_debris": ("Junkrat", "RIPTire", "0000000022FD", 8),              # colour metal bits (snap, tire shot down)
"jr_glow_ball": ("Junkrat", "RIPTire", "00000003699E", 1),           # the tire's glow ball (on 1385, with cat_band 1C7A)
"jr_cast_flame": ("Junkrat", "RIPTire", "000000004837", 2),
"jr_cast_smoke": ("Junkrat", "RIPTire", "000000004980", 50),
"jr_chain": ("Junkrat", "SteelTrap", "000000002A90", 1),             # the trap chain (2A92 its colour, 2A8F normals)
"jr_smoke_ribbon": ("Junkrat", "TotalMayhem", "0000000009ED", 1),    # colour smoke ribbon (bombs; Pharah's 0554 too)
```
Reused (same ids already in `EFFECT_MASKS`): `cat_soft` 4C09, `spark#4` 1F35, `ana_puff#4` 5429, `torb_trail` 2A480, `mercy_noise` 4DB,
`ana_tube_line` 1EF26, `lw_thorn_flame#2` 14F9, `glow` A7B6, `brigitte_smoke#32` 1DFCB, `lucio_glow` 210FD, `lw_burst` 37C36,
`brigitte_flare_star` 2F4F0, `sombra_trail_band` DC90, `cat_strip` 1344, `brigitte_noise_fine` 4D4, `cat_cloud` 18E48, `ana_slash`
391F2, `ana_vignette` 5D0, `ana_ring` 6429, `band` 2FF91, `ana_frame` 27E5D, `mercy_plus` 27E5E, `cat_fade` 7A9,
`cat_noise` 299E1, `juno_cloud_noise` 299E9, `cat_band` 1C7A, `cat_streak` 183A7, `dust` BE5, `ana_smoke#4` 9EF, `dva_solid` 1298,
`sombra_smoke` 3E28, `lucio_spark` 29F7, `cat_speed_line#3` E99F, `dva_sd_smoke#4` 4F0, `cat_cone_line` 16E9D, `cat_ring_line` 299E3.
Not needed: normals / distortion AFF, B08, B0B, 1FC65, 22BE, 22FE, 1DD4, 18F81, C4A; colour ramps 31C0, 3127, 22A8, 2A02, 2D7F, 8FF5
(their colours are in the palettes below); 1DFCA (the smoke's second layer), 218F, 460, A.

Proposed `EFFECT_MODELS` lines (`name: ("Junkrat", <Ability>, <12-hex id>)`):
```
"jr_muzzle_burst": ("Junkrat", "FragLauncher", "0000000027B3"),   # the launch's burst, 0.2 x 0.17 x 0.2
"jr_sphere": ("Junkrat", "RIPTire", "00000000085B"),              # the tire blast's 4 m sphere (unit sphere)
"jr_nitro_flare": ("Junkrat", "Perks", "000000014346"),           # Nitro Boost's flare, 0.95 x 0.39 x 1.65
```
Reused models: `brigitte_ring` 296B (ground rings), `cat_sphere` 1385, `cat_bubble` DBF, `ana_plus` E0AA, `torb_hemisphere` 963D,
`torb_slag_blob` E20D. Not needed: quads 801 / F5B / F78 / 101B6, ribbon meshes 7F5 / E71 / 31AF / F84, the held props 2959 / 28CD.

Proposed palettes (`JunkratPalettes`, stops t, r, g, b; from `fx_junkrat_pal.py`: black at 0, HDR rows hue-clamped, a near-white top
dropped so a solid mask ends on the colour, falling rows reversed; "by age" rows are a ribbon's / smoke's colour over its life, head
first, for `Fx.trail` / `Fx.soft`, not mask palettes):
```
JR_FRAG_MUZZLE = {0,0,0,0, .25f,1,.51f,0, .5f,1,.566f,0, .75f,1,.621f,0, 1,1,.775f,0};   // 4286 r0
JR_FRAG_MUZZLE_SPARK = {0,0,0,0, .25f,.117f,.078f,0, .5f,.991f,.644f,0, .75f,.991f,.672f,.006f, 1,1,.863f,.44f};   // ramp tex 3127
JR_FRAG_MUZZLE_SMOKE = {0,0,0,0, .25f,.163f,.135f,.135f, .5f,.328f,.27f,.27f, .75f,.49f,.402f,.402f, 1,.655f,.537f,.537f};   // 23E7 r1
JR_FRAG_GLOW = {0,0,0,0, .25f,1,.098f,0, .5f,1,.215f,0, .75f,1,.293f,0, 1,1,.392f,0};   // 1FA3 r1 HDR 2.85
JR_FRAG_GLOW_ENEMY = {0,0,0,0, .25f,.998f,.075f,0, .5f,.996f,.149f,0, .75f,.994f,.223f,0, 1,.992f,.298f,0};   // 1FA3 r2
JR_FRAG_GLOW_B = {0,0,0,0, .25f,1,.252f,0, .5f,1,.545f,0, .75f,1,.754f,0, 1,.991f,1,0};   // 4967 r1 HDR 2.85
JR_FRAG_SPARK = {0,0,0,0, .25f,1,.265f,0, .5f,1,.333f,0, .75f,1,.444f,0, 1,1,.497f,0};   // 9CE5 r1 HDR 5.39
JR_FRAG_TRAIL = {0,1,.125f,0, .25f,1,.017f,0, .5f,.423f,.035f,0, .75f,.237f,.037f,.002f, 1,.125f,.042f,.004f};   // A8BE r8 by age HDR 4.61
JR_FRAG_FIRE = {0,0,0,0, .25f,1,.04f,.036f, .5f,1,.102f,.055f, .75f,1,.309f,.121f, 1,1,.439f,.162f};   // 5737 r2 HDR 12 reversed
JR_FRAG_FIRE_CORE = {0,0,0,0, .25f,1,.287f,.153f, .5f,1,.382f,.211f, .75f,1,.643f,.372f, 1,1,.904f,.533f};   // 5737 r1 HDR 10 reversed
JR_FRAG_FLASH = {0,0,0,0, .33f,.657f,.17f,0, .67f,1,.266f,0, 1,1,.55f,0};   // 70B9 r1 HDR 5 top dropped
JR_FRAG_SMOKE = {0,0,0,0, .25f,.312f,.144f,.144f, .5f,.429f,.243f,.243f, .75f,.495f,.349f,.349f, 1,.473f,.473f,.473f};   // 8B8E r2
JR_SPARK = {0,0,0,0, .25f,1,.244f,0, .5f,1,.36f,0, .75f,1,.465f,0, 1,1,.647f,.008f};   // 7975 r1 HDR 5.39
JR_RELOAD_SPARK = {0,0,0,0, .25f,1,.267f,.03f, .5f,1,.478f,.041f, .75f,1,.654f,.05f, 1,1,.8f,.058f};   // 195E r0 HDR 3.26 reversed
JR_RELOAD_SPARK_B = {0,0,0,0, .25f,1,.268f,.006f, .5f,1,.462f,.009f, .75f,1,.613f,.011f, 1,1,.734f,.012f};   // 33D8 r0 HDR 4.91 reversed
JR_MINE_FIRE = {0,0,0,0, .25f,1,.014f,0, .5f,1,.025f,0, .75f,1,.033f,0, 1,1,.04f,0};   // 3301 r0 HDR 2.45
JR_MINE_FLAME = {0,0,0,0, .33f,1,.254f,0, .67f,1,.444f,0, 1,1,.947f,0};   // 803B r1 HDR 9.56 top dropped
JR_MINE_FLAME_ENEMY = {0,0,0,0, .25f,1,.157f,0, .5f,1,.231f,0, .75f,1,.553f,0, 1,1,.948f,0};   // 803B r2 HDR 9.56
JR_MINE_GLOW = {0,0,0,0, .25f,1,.216f,0, .5f,1,.354f,0, .75f,1,.446f,0, 1,1,.775f,0};   // 2855 r1 HDR 3
JR_MINE_SMOKE = {0,0,0,0, .25f,1,.258f,0, .5f,1,.27f,0, .75f,1,.282f,0, 1,1,.286f,0};   // D480 r0 HDR 2.68
JR_MINE_LIGHT = {0,0,0,0, .25f,1,0,0, .5f,1,.367f,0, .75f,1,.749f,.009f, 1,1,.905f,.014f};   // CC40 r1 HDR 4.88
JR_MINE_BLINK = {0,0,0,0, .25f,.547f,.518f,0, .5f,1,.947f,0, .75f,1,.963f,.287f, 1,1,.982f,.611f};   // 1976 r0 HDR 2.33
JR_MINE_BLINK_RED = {0,0,0,0, .25f,1,0,0, .5f,1,0,0, .75f,1,.047f,0, 1,1,.905f,.014f};   // 2994 r1 HDR 21.6
JR_MINE_TRAIL = {0,0,0,0, .25f,1,.906f,.32f, .5f,1,.915f,.364f, .75f,1,.936f,.473f, 1,1,.948f,.533f};   // AFB3 r2 HDR 4.26
JR_MINE_TRAIL_ENEMY = {0,0,0,0, .25f,.845f,0,0, .5f,1,0,0, .75f,1,0,0, 1,1,.947f,.533f};   // 63C1 r2 HDR 10.4
JR_DETONATOR = {0,0,0,0, .33f,.702f,.008f,.002f, .67f,.717f,.015f,.003f, 1,.86f,.181f,.174f};   // 5688 r1 shift, top dropped
JR_TRAP_THROWN = {0,0,0,0, .33f,1,.668f,.043f, .67f,1,.685f,.086f, 1,1,.702f,.129f};   // D3BB r3 reversed, top dropped
JR_TRAP_THROWN_ENEMY = {0,0,0,0, .33f,1,.552f,.552f, .67f,1,.7f,.7f, 1,1,.85f,.85f};   // D3BB r4 reversed, top dropped
JR_TRAP_CHAIN = {0,0,0,0, .25f,.708f,.6f,0, .5f,.806f,.666f,0, .75f,.902f,.732f,0, 1,1,.799f,0};   // F309 r1
JR_TRAP_SMOKE = {0,0,0,0, .25f,.154f,.154f,.154f, .5f,.193f,.193f,.193f, .75f,.231f,.231f,.231f, 1,.27f,.27f,.27f};   // B980 r1
JR_TRAP_DEBRIS = {0,0,0,0, .25f,1,.666f,0, .5f,1,.666f,0, .75f,1,.667f,0, 1,1,.667f,0};   // 9154 r0 shift HDR 6.28
JR_TRAPPED = {0,0,0,0, .5f,.382f,.052f,0, 1,1,.172f,0};   // 33DC r0 (rises then falls: its samples sorted by brightness, ours)
JR_TRAPPED_RING = {0,0,0,0, .25f,.091f,0,.434f, .5f,.188f,.02f,.708f, .75f,.165f,.107f,.93f, 1,.153f,.162f,1};   // ramp tex 8FF5
JR_TRAPPED_B = {0,0,0,0, .25f,.309f,.098f,.412f, .5f,.554f,.273f,.691f, .75f,.764f,.438f,.92f, 1,.858f,.541f,1};   // 5443 r3
JR_TIRE_TRAIL = {0,1,.122f,0, .25f,.071f,.022f,.005f, .5f,.008f,.008f,.008f, .75f,.004f,.004f,.004f, 1,0,0,0};   // 9804 r6 by age HDR 7
JR_TIRE_TRAIL_ENEMY = {0,1,.017f,0, .25f,.071f,.005f,.005f, .5f,.008f,.008f,.008f, .75f,.004f,.004f,.004f, 1,0,0,0};   // 3422 r8 by age
JR_TIRE_FUSE = {0,0,0,0, .25f,1,.252f,0, .5f,1,.545f,0, .75f,1,.754f,0, 1,.991f,1,0};   // DA5F r0 HDR 2.85
JR_TIRE_FUSE_ENEMY = {0,0,0,0, .25f,1,.022f,0, .5f,1,.05f,0, .75f,1,.065f,0, 1,1,.086f,0};   // DA5F r1 HDR 2.85
JR_TIRE_SPARK = {0,0,0,0, .25f,1,.388f,.185f, .5f,1,.466f,.186f, .75f,1,.545f,.187f, 1,1,.624f,.188f};   // D4A4 r1 HDR 10 reversed
JR_TIRE_GLOW = {0,0,0,0, .25f,.676f,.283f,.161f, .5f,.944f,.28f,.074f, .75f,.619f,.283f,.18f, 1,.287f,.287f,.287f};   // B8EA r5
JR_TIRE_FIRE = {0,0,0,0, .25f,1,.381f,.153f, .5f,1,.468f,.232f, .75f,1,.634f,.382f, 1,1,.799f,.533f};   // FC57 r1 HDR 12 reversed
JR_TIRE_FIRE_B = {0,0,0,0, .25f,1,.028f,0, .5f,1,.029f,0, .75f,1,.03f,0, 1,1,.031f,0};   // 2945 r0 HDR 3.14
JR_TIRE_FLASH = {0,0,0,0, .25f,1,.305f,0, .5f,1,.378f,0, .75f,1,.542f,0, 1,1,.905f,.402f};   // 2DC2 r0 HDR 4
JR_TIRE_FLASH_BIG = {0,0,0,0, .25f,.657f,.17f,0, .5f,1,.266f,0, .75f,1,.544f,0, 1,1,.921f,.209f};   // ABBB r0 shift HDR 5
JR_TIRE_SHELL = {0,0,0,0, .25f,1,.188f,.023f, .5f,1,.219f,.022f, .75f,1,.295f,.02f, 1,1,.342f,.018f};   // 3E05 r0 HDR 5
JR_TIRE_RING = {0,0,0,0, .25f,1,.049f,0, .5f,1,.078f,0, .75f,1,.1f,0, 1,1,.1f,0};   // ramp tex 31C0 x B894 r0 tint (5, 0.5, 0)
JR_TIRE_FLAMES = {0,0,0,0, .33f,1,.254f,0, .67f,1,.444f,0, 1,1,.947f,0};   // E413 r1 HDR 9.56 top dropped
JR_TIRE_SMOKE = {0,1,.271f,0, .25f,.182f,.049f,0, .5f,0,0,0, .75f,0,0,0, 1,0,0,0};   // 1BFA r3 by age (draw soft, its grey from the mask)
JR_BOMB_TRAIL = {0,0,0,0, .25f,.468f,.044f,.001f, .5f,.615f,.087f,.001f, .75f,.76f,.13f,.002f, 1,.907f,.174f,.002f};   // 77EF r0
JR_BOMB_GLOW = {0,0,0,0, .25f,.805f,.023f,0, .5f,.917f,.049f,0, .75f,.983f,.075f,0, 1,1,.1f,0};   // 023F r0
JR_BOMB_GLOW_TINT = {0,0,0,0, .25f,1,0,0, .5f,1,.001f,0, .75f,1,.046f,0, 1,1,.15f,.041f};   // ramp tex 2A02 x 86D2 r2 tint
JR_BOMB_FLASH = {0,0,0,0, .25f,1,0,0, .5f,1,.046f,0, .75f,1,.188f,.004f, 1,1,.485f,.184f};   // 0B6C r0 HDR 8
JR_BOMB_SPARK = {0,0,0,0, .25f,1,0,0, .5f,1,.114f,0, .75f,1,.324f,.013f, 1,1,.617f,.136f};   // FE9B r1 HDR 5.39
```
The rest of `work\fx_palettes2.txt` (FRAG_TRAIL_HOT, TRAP_CHAIN_B, TIRE_TRAIL_B, BOMB_TRAIL_HOT) are alternates, not needed.
