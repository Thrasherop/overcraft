# Jetpack Cat (hub)

Everything learned about Jetpack Cat, by subject. The third hero, and the first who flies. Up:
[Techniques](../README.md) · the method: [animation_import](../animation/animation_import.md) · engine facts she
turned up: [minecraft_26](../minecraft/minecraft_26.md)

## Her notes and sources
- **The data map:** `E:\Overwatch Archive\research\jetpack_cat\jetpack_cat-map.md` (states, clips, abilities, movers,
  effects, voice; read 2026-10-27), with the graph digests in `work\digest_*.txt` (Purr is 5A7E, Frenetic Flight
  59E0, the jetpack 59DC, the volley 5ADB, Catnapper 5B7B).
- **Clip maps:** `tools/clipmaps/jetpack_cat.txt` (third person), `jetpack_cat_1p.txt` (first person). One model
  (0586.118) with two looks: third person 2673, first person 2674. No weapon entity: her paws are the guns.
- **Numbers:** `JetpackCatStats`, each with its source; `tools/reference/jetpack_cat.md` (the public sources),
  `tools/reference/jetpack_cat-plan.md` (the port's plan).

## Flight (OwMovement.fly)
- Her mover is a held override (graph 59DC): air acceleration 9.5, speed above 12 m/s decays, drag 8 a second with
  no input, the "fly" flag (no gravity). W follows the aim (look up to climb), S backs off at 4.95, A / D strafe,
  Space rises and Crouch sinks (the data's +1 / 0 / -1). Measured in play: 5.5 m/s every way, a stop in about 0.3 s.
- Frenetic Flight (59E0): a 0.3 s burst mover, then the sustain; the speed bonus ramps +25% to +65%; a push of 25 m/s²
  along the input. The push outran a soft cap to 29.5 m/s: the top speed is a hard stop at the wiki's measured 17.8
  (reached in about 0.3 s). After it, the speed eases back to 12 while W is held (the data's max air; not confirmed
  against the real game).
- Minecraft's side: she needs `abilities.mayfly` while in play (a dedicated server kicks a player floating 4 s), and
  never `flying` (Creative flight's damping). `JetpackCat.keepFlight` hands it back out of play.

## Her kit, as it plays (2026-10-28)
- A volley is 4 paws (4 each, double on the head) and a centre shot (8, no headshots): 24 to a body, 40 to a head
  (measured: 22.3 and 37.3 after a zombie's armour). Paws hit enemies at 0.19 m and allies at 0.31 m (centre 0.37 /
  0.62): `PawEntity.hitRadius` and an ally sweep before each move.
- Lifeline: the rope pulls the ally by her real movement (`getKnownMovement`), so a villager hangs 3-4 m under her and
  follows. It heals 25 a second and ends when she dies, leaves hero mode or changes hero.
- Purr: 30 damage at the cast, full to 5 m and 0.3 at 10 m; every wave of the 4 pulses lands, past the 4 s too (300 a
  Purr, 360 with Purrfect Form, measured).
- Catnapper: aim (a ring of the 5.5 m landing reach marks the spot), the 0.35 s windup, a 30 m/s dive, 25 damage and a
  1.5 s knock-down (`Sleep.knockdown`, #101 below), then the nearest enemy but a boss hangs under her for up to 4 s,
  its top 1 m below her feet (held at her feet, a zombie hit her the whole carry and killed her).
- Her quick melee's layers (`add_melee`, 132-0 19A96 / 97 / 98) are additive (bit 0x4 of the folder), and until #75 they
  were exported with rest values on the 189 channels the game doesn't key, which added her face, eyes and knees' rest
  offsets a second time. Mended in the pack ([additive_clips](../animation/additive_clips.md)).

## Effects and sounds (2026-10-28)
- **Effects** from `tools/reference/jetpack_cat-effects.md` (a research pass over her 75 effects): `JetpackCatFx`. Her
  colours are gold and blue/cyan, enemies see red and orange (`CAT_*` palettes). Shots: gold core in a blue halo, gold
  tracer, blue trail, sparks, a star flare at the paw; the centre shot prints a paw (five quads) on a creature. Purr: a
  dome bursting to 10 m, gold and blue rings each pulse. The rope, Lifeline's markers and Catnapper's aim come from her own data now (below).
  Catnapper's landing: a flash, smoke and glowing cracks.
- Her masks aren't in the extraction: `build_asset_pack.py effects <extraction> "E:\Overwatch
  Archive\research\jetpack_cat\fx"` takes them from her research folder.
- **Sounds** from `tools/soundmaps/jetpack_cat.txt` (29 rules, 30 voice moments): one-shots from the server (fire and
  reload for everyone, the rest hers), loops from her client (`JetpackCatSounds`), voice by moment (`Voice`).

## Lifeline and Catnapper from her data (2026-10-28, #55 / #59)
From `research/jetpack_cat/assets2.md` (the owner: "use less custom assets more of cat's actual assets"):
- **The rope** (71B0; Catnapper's 71C4 adds a 0.32 m strip): a braided cable (6E132) over two glows, a ball at each
  end, from her arm's tip (bone 3E67: `HeroModelRenderer`'s hardpoint 6) to her hook 2.2 m over the ally's feet (just
  over a mob's head). It flashes as it latches (72FA).
- **Seeking:** the paw marker under her (7432, the data's half icon mirrored whole by the pack builder), rings opening
  on it (7490: torus 15A26, noise times strip), the cone glow at her arm (6EA2, mesh 1B972).
- **Catnapper's aim:** rings (71B3: the strip alone), two ground glows (4C09), lines from her paws (71AF). A cancel
  takes the rings with it; after a confirm they stay to mark the landing.
- **The view:** in third person her own head hid the aim; the lifted ability view is in
  [minecraft_26](../minecraft/minecraft_26.md#a-camera-of-our-own). The data gives no camera placement for it (her
  flight graph's v479, 3.5 or 6.75, may be a camera distance: L).
- Still ours: the rings' growth (the data's 0 to 5 curve, L), the cone's place under the arm, leaving the 18E48 cloud
  off the aim rings. Her projectiles are still to move to her own assets (#59).

## The rest of #59: bubbles, arcs, the range ring setting (2026-11-02)
From `research\jetpack_cat\work\fx_digest.txt` (71B1, 7454, 70F9, 71B3, 71AF) and `assets2\settings_strings.json`:
- **The hook's bubbles (71B1 C9):** her sphere DBF (new pack model `cat_bubble`) at size 1.79 (1.76 m across), 5 a second,
  0.8 s, rising 1.578-1.789 m/s from 1.28 m under the hook. Its masks 299E3 x 16E9D x the thin strip 6C057, which lies
  along the sphere's V (its latitude: DBF's UVs read with `owmread`), so each bubble is a noisy ring round its middle; in
  play they read as rings rising round the towed one. The product is stretched to full brightness and drawn at 0.5 (ours:
  at the plain product they weren't seen; four overlap). Its 12BB row r2 (0 to 1) isn't a size curve: Purr's wall (70F9)
  has the same row over a 10 s life.
- **The arcs (7454):** the half ring 14874 is planar-mapped (u = x + 0.5, v from its top: the glb's UVs are the game's,
  DataTool flips V on export and the glTF export flips it back), a rectangle with a half disc of radius 0.362 cut out,
  so its masks must round it. C6 (0.35 s, at the start of each 1 s loop) layers 18E48, 71226, 18F00 twice and the strip
  BA19; C8 (1 s, from 0.067 s) 71226, 299E3, 18F00 twice and the strip 39F16. `bake_cat_arcs` lays them on (M, the shader
  isn't decoded): 71226 (a disc's soft edge round its corner) on each quarter round the arc's centre, one minus (the outer
  rounding); the strips across the band from the cut edge out (BA19: a sharp inner edge and a tail; 39F16: concentric
  lines). Read the other way (the strips along V, 71226 at its LUT row's UV scale 2 x 1) it made two faint horizontal
  smears, not arcs. Colours: the flash C6 r6 cyan (`CAT_ARC_FLASH`, ramping with the mask: `CAT_TETHER` saturates at a
  quarter and drew a solid blob), the lines C8 r8 slate to white (`CAT_ARC_LINES`). Both face as the towed one does
  (ECIM 40: M).
- **Hide Purr Range Ring:** her data's hero setting 123DB (var 74715, a switch, off by default) gates the wall at 5A7E
  #267: on, the wall effect is never made, so nobody sees it. `HeroOptions.hidePurrRing` goes to the server
  (`HeroOptionsPayload`), which sends it with "purr" (dx 1). The HERO tab of the options now lists each hero's own options
  under the hero played (before, Kiriko's showed under any hero's name).
- **Also from the data now:** the range wall's masks (1C7A x 18F00 x 19167), Catnapper's aim rings (1C7A x the 18E48 cloud,
  no longer left out), its lines (three ribbons per paw: 0.01 and 0.02 m of 299E3, 0.0455 m of 1344 x 18E48, was one
  0.045 m line of the rope's 18A1), its ground decals (0.45 m deep blue, and the 1 m one near black at half: a blended dark
  disc, was a 1 m blue glow).
- **Played** (dev client 2, world `cat59`, superflat, picked on the select screen, again after Save and Leave and a
  rejoin): a villager towed by day and at night (third person, as Lifeline forces it; a NoAI villager from the side:
  `c59_tow*.png`, `c59_side*.png`, `c59_night_tow.png`); Purr in first and third person with the setting off (the gold
  tint past 10 m, `c59_purr1p.png`, `c59_purr3p.png`), switched on in Options > HERO by the mouse (`c59_herotab_on.png`:
  no wall particle at all, `c59_purr_hidden.png`), and off again (the wall back); Catnapper's aim by day and night
  (`c59_nap1.png`, `c59_night_aim.png`), a cancel, and a confirmed dive onto a zombie and its carry (`c59_night_carry.png`);
  shots in both views (`c59_shot3p.png`). Screenshots in `run2/screenshots/` of the worktree (not committed).
- **Not tried:** a second player (the setting as another player sees it: the server path is the same one), an enemy
  Jetpack Cat's arcs and bubbles (red), the arcs from her own first person (Lifeline forces third person).
- **Still ours or unsettled:** the arcs' mask layout (M) and both effects' strengths; the wall's 0.35; the seeking rings'
  growth (L) and the cable's tiling (L). (The rest listed here then is done: next section.)

## The last of #59: every effect of hers drawn, from her data (2026-11-02)
From `research\jetpack_cat\workx_digest.txt` (7064, 7373, 7163, 71B8, 709F, 71C2, 70F9, 5513) and
`assets2\workx_digest.txt` (7256, 74D0). New pack files (`build_asset_pack.py`: `effects --only cat_ <FX1> <FX2>` writes
only hers into a shared pack): meshes `cat_flame` 1B580, `cat_bell` 19370, `cat_slash_arc` 152B9, `cat_dome` 1B2FF (also
in `HeroModels.EXTRAS`: an effect mesh not listed there never loads, `Fx.mesh` is null and nothing is drawn); masks 720CE,
F7D2, 5429, 18400, 37021, 55A46, D89, 54A15 and products of them; bakes `bake_cat_flame` (the flame tube's 8 frames, the
hexes, the range wall). Her new palettes are in `CatPalettes` (EffectArt's enum initializer is at the class file's 64 KB).
- **Jetpack flames (7064):** the tube 1B580 itself (0.128 m wide, 0.17 m long, its V 0 at the nozzle), three layers at the
  data's sizes (1.1 teal, 1.1 gold, 0.55 white-hot), the masks 2C41F x 7A9 x 299E3 baked into 8 frames of the noise moving
  down it, looped at 20 a second (the data's scroll rate -10 isn't usable as is: ours). Sparks: 50 gold and 45 pale a
  second of 0.011-0.014 m blobs stretched 6 along their way (their speed is an engine input: ours 1 m/s).
- **Spark sizes:** the shots' sparks at the data's 0.15-0.5 m (they're 1F35 blobs filling half the quad).
- **The paw print (709F):** the data's layout and sizes, no 5x: a print about 0.1 m across, the teardrop D89 x 18E48 in
  two sets (gold 0.09-0.27 m, blue 0.09-0.47 m). In play it's small and mostly behind the enemy-hit flare (as the data has
  it).
- **Catnapper's cast (7373):** flares of the flame tube on both nozzles, exhaust rings (1BD54, 1C7A x 18E48) shot out at
  6.9 m/s, a gold bell (19370 at 1.1, its wide end 0.43 m under her feet, narrow end up: the mesh as it stands, M).
- **The dive (7163)** from the end of the 0.35 s windup until `catnapper_land` (the server now sends her id with the cast and
  the landing): three tube trails and two strips per nozzle at the data's widths and lives, the spark bursts (30 at the
  start, 144 pale and 50 gold a second), gold exhaust rings 7 a second. Not drawn: its heat streaks (distortion) and its
  speed lines (placed by an engine input, ECIN, not decoded).
- **The head slash (71B8),** the dive's first 0.2 s: arcs 152B9 100 a second, 1.6 m ahead of her middle (its hardpoint 01DA
  isn't resolved: M), and two domes 1B2FF (2.58 m) 0.21 m ahead. At full strength the inner dome (55A46, a mostly white
  strip) was a solid cyan disc in play: drawn at 0.6 / 0.35 (ours).
- **The landing's ground flash (71C2 C19):** 21.7 m as the data has it, 4C09 x 16E9D, navy to #0004FF (800C2FC5 r3). By
  night it washes the whole view navy for its 0.25 s (`h2.png`); by day it hardly shows. The data's colour never goes past
  pure blue, so it doesn't whiten the view as the old 12 m cyan glow would have at that size.
- **The landing's hit on each enemy (7256):** the server plays `catnapper_hit` on each one it damages, thrown out from her: a
  0.26 m sphere, tan smoke and dust, a small flame burst, 2-4 sparks back toward her (the effect's own (0, 0.3, 0.98) and
  (0, 0, -1): M that its Z is away from her). Seen spawning on both zombies (`fxnear`), small at play distance.
- **Transport Shielding (5513):** only she sees it (the server sends it to her alone, M). The frame 5C75 is a square with an
  octagon cut out and its edge mask 18400 is black inside a circle, so it's hexes in a square's corners: drawn as a camera
  facing square at her chest from a camera off her (Lifeline and the carry lift the camera), 1.45 m before her eyes in true
  first person. It lasts while her absorption does (up to 3 s); her absorption reaches the client a tick after the effect,
  which at first ended it at once (found playing).
- **Claws Out (74D0):** three claw-mark quads (720CE x 18E48, 0.75 m: the data's 0.49-1.01 runs on an input not decoded,
  ours its middle) at the data's turn range, 5 sparks, at the clawed one's chest moved 0.4 m toward her (at the chest's
  middle the zombie's own body hid them, found playing). Teal for her side, pink-red for theirs.
- **Purr's wall at night (70F9):** reproduced first: past 10 m the ground and the sky at the horizon took about +40 of
  gold by night (`purr_1p_a.png`). The data has no 0.35 (ours); what it has: its noises tiled 4 x 4 (12BB r0) and a 65BC row
  r4 of two narrow bumps (R peaking at 0.4, G at 0.6 of its input, 0.07 / 0.01 at 0.2). Read as strength along the wall's V
  (0 at its top 4 m over her, M: the age reading has it dark for the first 2 s of a 4 s Purr, which doesn't fit), the wall
  is lit 4-12 m under her and faint at her own height. Baked into `cat_purr_wall_band`: on the ground the tint is now about
  +12 (`purr3_1p_ground_1s5.png`); flying 8 m up, the ring lies on the ground round her (`purr3_3p_air.png`).
- **Played** (dev client 2, world `cat59b`, superflat, picked on the select screen, again on each rejoin after a restart):
  Purr on the ground by night and day and 8 m up, first and third person; the jetpack flames close up by day and night,
  back and front (`flame_*`); a volley onto a NoAI zombie with the effects slowed (`shot1-4`, `fxnear`: the print's quads at
  the data's sizes); Catnapper by night and by day onto AI zombies, at /tick rate 3-4 and at full speed (`nap2_*`, `d*`,
  `e*`, `h*`): cast, windup rings, dive trails and sparks, the head slash's domes, the landing, the hit bursts on both
  zombies, the carry; Transport Shielding (picked in the perk menu) by Lifeline on a villager (`ts*`) and by the carry
  (`k3.png`); Claws Out by quick melee on a zombie in first person (`claws1.png`). Screenshots in `run2/screenshots/`.
- **Not tried:** an enemy Jetpack Cat (the red rows), a second player seeing her dive, Transport Shielding in true first
  person (Lifeline and the carry always lift the camera), Claws Out from the third person (the second swing was still on
  its 6 s cooldown). A `reload` of the resource packs ran the 3 GB client out of memory (hero models): restart instead.
- **Still ours:** the flame noise's speed; the jetpack sparks' speed; the 0.6 / 0.35 dome strengths; the wall's 0.35 and its
  r4 reading (M); Claws Out's size; where 01DA is; the hex overlay's place in first person; the bell's orientation.

## The body that was never drawn (0.3.16, found by the owner)
- 0.3.16 shipped an empty jetpack with eyes and a tongue: her fur material (shader 0x10D) has its colour texture only
  in its .owmat, which the exporter doesn't link, so the cat came out colourless and wasn't drawn. Every earlier check
  looked at the jetpack and the effects, never at whether the cat was there. Fixed in `owm_survey.py` (COLOUR_KEYS);
  the lesson is in [animation_import](../animation/animation_import.md). **Look at the whole model from every side
  after any export, and compare with the hero's portrait.**

## The gaps pass (2026-10-28: `E:\Overwatch Archive\research\jetpack_cat\gaps.md`)
- Settled from the data: her four emotes and their props, 12 voice-line unlocks, the nozzle hardpoints (the look 2673's
  own table: idle flames and boost trails now on them), the paws' hardpoints (000A / 000B), the hit sounds (73A4;
  centre shot on a block 093B; the paw shots make none on walls), the hit sizes (the wiki's), the rope's snap at 3
  rope lengths, the detach launch, the select screen's scripted run, her first-person rise and sink layers.
- Her emotes' "EntityControl 99" is the low byte of slot **7863** (Lúcio's mic): TankLib reads a slot id's low byte as
  the "action" ([mercy_emotes](../emotes/mercy_emotes.md)).

## Still ours or not in the data
- **Vertical flight speed:** not in any mover field (the engine's); ours 5.5 m/s, normalised with the other inputs.
- **Frenetic Flight's top speed:** the data reads two ways (22.5 or 20 m/s); the wiki's measured 17.8 is kept.
- **After Frenetic Flight:** the data's max air 12 holds while W is held; whether the game falls back to 5.5 is the
  engine's.
- The rope node's numbers (stiffness and the rest: unnamed), the Lifeline launch's direction F, two Catnapper fields,
  which ranged copy of a sound is allies' and which enemies', and the showcase's condition parameters.
- Carrying a "tank" 30% slower isn't done (Minecraft has no tanks).

## Her flight from the Workshop recording (#60, 2026-11-01)
The owner's recording (`E:\Overwatch Archive\research\movement\workshop-2026-11-01b\`, 48 s of her flying, sampled at
62.5 a second) against ours, the same moves played in the dev client (world `cat1`, `cat_ours.py` traces):

| | the recording | ours before | ours now |
|---|---|---|---|
| 0 to 5.5 m/s | 0.13 s, steps of 1.37 / 0.032 s (42.7 m/s/s = 7.75 x 5.5) | 0.12 s (9.5 x 5.5) | 0.13 s |
| no keys, from 5.5 | a straight line to 0 in 0.56 s (9.6 m/s/s), three stops | 3.58 after one tick, a long tail | 0.6 s, straight |
| backing off | 5.50 m/s | 4.95 | 5.5 |
| Space / Crouch | +5.5 / -5.5 m/s | the same | the same |
| boost from 5.5 | 9.5 at 0.2 s, 16.85 at 0.91, 17.36 at 1.01, 17.80 by 1.4 s | 17.8 at 0.45 s | 9.5 at 0.17, 16.26 at 0.84, 17.45 at 1.01, 17.8 by 1.6 |
| boost length | 2.5 s on a full tank | 2.5 s | 2.5 s |
| after it, keys held | slowing, cut to about 12 at 0.3 s (14.58 -> 11.54), down to 5.5 by 0.7 s | slowing to 12 and staying there | 17.32 ... 15.4, cut to 11.52 at 0.3 s, 5.5 by 1.0 s |
| after it, no keys | a straight line, about 8.5-9.6 m/s/s | stopped in 0.7 s (11.46 -> 7.46 in a tick) | a straight line, 1.9 s from 17.8 |

- **The rule that fits every run:** speeding up toward the wish at 7.75 x the wish speed a second, slowing in a straight
  line at 1.745 x; under Lifeline's +30% both 30% more (12.3 / 12.9 m/s/s measured, 12.5 predicted). The data's "drag 8"
  read as an exponential was what stopped her dead, the "funky" feel of #60. BZ Aug 11 2026 says her air acceleration
  went 7.7 -> 9.5 and the data holds 9.5, but the live game measures 7.75: the recording wins (what the game does now).
- **Found playing:** a forward of 7.15 m/s was Lifeline left on (a toggle: +30% while on, mode 16), not the flight; the
  recording's 7.15 stretches are the same.
- Ours (M): how fast a boost's direction follows the input (6 a second) and turns at cruise (the recording has no clean
  turn); the boost's first 0.2 s as one 20 m/s/s step (the three runs kick +1 to +3 m/s at different times).

## Catnapper's knock-down and what it carries (#101, 2026-11-04)
Up: [Techniques](../README.md) · who's an enemy: [allies](../minecraft/allies.md) · the hold: [minecraft_26](../minecraft/minecraft_26.md#holding-a-mob-still-without-its-saved-noai-flag-97-2026-11-04)
- **The cause:** Slowness X and Weakness X stand in for "knocked down (stun 1.5 s)" (W), written before `Sleep` existed. AI runs under them. **The fix:** `Sleep.knockdown`, the stun that keeps its push (Accretion's). She keeps the push of 2.8 m/s; so does the mob she carries, moved by `Sleep.slide` for its first 1.5 s.
- **A held creeper's fuse:** `Creeper.tick` runs the fuse outside its AI, so a creeper held mid-swell went off anyway. `Sleep.tick` now turns it back (`setSwellDir(-1)`) for Sleep Dart, Rally's stun and all, unless it's lit by flint and steel. Found reading `Creeper.java`; played below.
- **The carry:** the nearest `Allies.areaHits` target that isn't `Allies.isBoss` (Ender Dragon, Wither, Warden, Elder Guardian). **Ours:** her data and the wiki have no such rule (only tanks pulled 30% slower, and no Minecraft mob counts as a tank). A boss is still damaged and knocked down.
- **Played, old (985f73c) vs new, same script** (keys through DevServer, Resistance V on her, real AI mobs held by `movement_speed` 0 and each read by a line in the log):

| Case | Old | New |
|---|---|---|
| Three skeletons 3 m off, arrows in the first 1.5 s after she lands | 1 at 1.2 s, then on (1 at 2.1, 1 at 2.7, ...) (`rl_old_101sk`) | 0, all three listed held, then 3 at once at 2.1 s when it ends (`rl_new_101sk`) |
| A creeper with a 1.5 s fuse, 0.45 s into it at her confirm | gone by 1.6 s, the carry with it | held to 1.9 s and alive, goes off by 4.3 s (mode 4 through it) |
| Nearest a cow, a zombie further on | carried the cow up (y -63 -> -45) | the cow stays on the ground, 10 health; the zombie goes up |
| Nearest a Wither / Warden / Elder Guardian | carried each (-61 -> -49, -63 -> -44, -61 -> -44) | each stays; the zombie goes up. |
| From the air, onto two zombies, a skeleton, a cow and a stand (`rl_new_play_aim`, `_land`, `_carry`) | not played | landed at 1.2 s; all three mobs held 1.2 -> 2.5 s and free at 2.8; cow 10, stand up, zombies 18.08; rose to y -26 with one |

- **What a critic should check:** one run each: the arrow count is small (old 1 in 1.5 s against about 1 a second before she landed). Not played: a second player as the target (`Sleep.stun` freezes their keys and their client; the carry's speed is sent the old way, so being carried while stunned is unverified). A mob already asleep from a dart isn't turned into a knockdown (`Sleep.stun` returns), so it is tethered but `Sleep.tick` holds it still until it wakes. Water, lava, ladders, boats, the Nether: not tried. Flyers (a phantom, a blaze): not tried.

### Round 2 (critique_rules, 2026-11-05)
Keys through `KeyMapping.set`, not a real keyboard. **Without Resistance V:**
- **Landing on a creeper:** it's carried, its AI back at 1.5 s, and it goes off about 3.6 s after the landing, 1 m under her: **she dies** from 18 on the ground (18 -> 0), or is left with **1.5 of 18** flying up with it. Nothing changed: **the owner's call** whether the carry drops a creeper, holds its fuse or skips it.
- **Two zombies and a skeleton:** she took 18 -> 12 in the first 0.4 s (they hit her before she landed: the confirm's 0.35 s), then regenerated through the carry at full speed to 18. No harm while they were held.
- **Flyers keep their height** (NoAI, so only the knock-down moves them; a zombie nearer, so it's the carried one): phantom old -57 -> -60, new -57; blaze old -57 -> -59.8, new -57. **The Ender Dragon isn't pulled:** old -56 -> -95 in 1 s (through the bedrock), new stays -56. Slides feel water (not played).
- **A creeper in Gravitic Flux** (200 health, 3 s fuse): old went off 1.9 s after the confirm, new alive 7.5 s+ (`Sleep.holdFuse`).
- **The carry takes** no dead mob, shulker, rider or dart's sleeper (by code; a shulker not played).
