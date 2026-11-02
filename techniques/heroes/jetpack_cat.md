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
  1.5 s knock-down (Slowness and Weakness: no stun in Minecraft), then the nearest enemy hangs under her for up to 4 s,
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
