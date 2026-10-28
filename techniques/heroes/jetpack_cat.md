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

## Effects and sounds (2026-10-28)
- **Effects** from `tools/reference/jetpack_cat-effects.md` (a research pass over her 75 effects): `JetpackCatFx`. Her
  colours are gold and blue/cyan, enemies see red and orange (`CAT_*` palettes). Shots: gold core in a blue halo, gold
  tracer, blue trail, sparks, a star flare at the paw; the centre shot prints a paw (five quads) on a creature. Purr: a
  dome bursting to 10 m, gold and blue rings each pulse. The rope: two straight bands (the data has no sag). Catnapper:
  pulsing target rings and paw lines while aiming, a flash, smoke and glowing cracks on landing.
- Her masks aren't in the extraction: `build_asset_pack.py effects <extraction> "E:\Overwatch
  Archive\research\jetpack_cat\fx"` takes them from her research folder.
- **Sounds** from `tools/soundmaps/jetpack_cat.txt` (29 rules, 30 voice moments): one-shots from the server (fire and
  reload for everyone, the rest hers), loops from her client (`JetpackCatSounds`), voice by moment (`Voice`).

## The body that was never drawn (0.3.16, found by the owner)
- 0.3.16 shipped an empty jetpack with eyes and a tongue: her fur material (shader 0x10D) has its colour texture only
  in its .owmat, which the exporter doesn't link, so the cat came out colourless and wasn't drawn. Every earlier check
  looked at the jetpack and the effects, never at whether the cat was there. Fixed in `owm_survey.py` (COLOUR_KEYS);
  the lesson is in [animation_import](../animation/animation_import.md). **Look at the whole model from every side
  after any export, and compare with the hero's portrait.**

## The gaps pass (2026-10-28: `E:\Overwatch Archiveesearch\jetpack_cat\gaps.md`)
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
