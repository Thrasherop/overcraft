# Jetpack Cat (#38): the plan

Numbers: `tools/reference/jetpack_cat.md` (public sources) -> `JetpackCatStats`; the game data map
`E:\Overwatch Archive\research\jetpack_cat\jetpack_cat-map.md` (wins where they differ). Branch `feature/jetpack-cat`.

## Where the data differs from the wiki (the data is used)
- A volley every 0.222 s (1 / 4.5; the wiki 4.48 a second); ammo back at 80% of the 1.25 s reload (1.0 s).
- Purr's cast knockback: full within 5 m, 0.3 of it at 10 m.
- Catnapper: a 0.35 s windup before the dive; the landing hits within 5.5 m (the wiki 6).
- Frenetic Flight: a 0.3 s burst (her mover's priority-101 state: max air speed 16, air accel 2, drag 2), then the
  sustain (priority 100: 6 m/s, air accel 6, max air 10, drag 1.25); a speed bonus ramping +25% -> +65% (+5% every
  0.04 s); energy 100, drain 40/s, refill 25/s after 0.75 s, a third of that carrying. The wiki's "17.8 m/s top speed"
  is its own measurement: ours to check against the data's states in play.

## Assets
- One model 0586.118 for both views: third person with look 2673 (entity Gameplay3P), first person with look 2674
  (Gameplay1P). Exported with `tools/owm_survey.py --glb --entity=... --keep=tools/clipmaps/jetpack_cat[_1p].txt`
  into the pack as `hero_models/jetpack_cat.glb` / `jetpack_cat_1p.glb`. No weapon entity: her paws are the guns.
- She hovers: the pelvis sits 0.55 m above her root.
- Clip maps: the data map's states; the flight blend's 8 directions named from the blend's axes and the survey's
  thumbnails (she leans into Y+, banks right on X+).

## Controls (Overwatch's, with the owner's mouse layout: the main attack on the left button)
- LMB: Biotic Pawjectiles (held). RMB: Frenetic Flight (held, burns energy). Shift: Lifeline. E: Purr. Q: Catnapper.
- Flight: W follows her aim (look up to climb), A/D strafe, Space up, Crouch down.

## Server (the kit)
- Pawjectiles: 4 paw shots (4 each, x2 headshots) + 1 centre shot (8, no crit) a volley, 60 m/s, 40 m, heal allies
  and hurt enemies, 24 ammo, reload 1.25 s (ammo at 1.0 s).
- Frenetic Flight: energy in the HeroState; its movement is client-side (the local player moves itself), its energy
  server-side, synced.
- Purr: 30 damage + knockback 15 out / 7 up in 10 m on cast; 4 pulses 0.95 s apart of 1 / 2 / 3 / 4 heals of 30 (18 on
  herself) in 10 m.
- Lifeline: toggle; the nearest ally in 8.5 m (Minecraft: players on her team who press Interact; tamed pets and
  villagers accept at once, ours) is towed on a 4 m rope, healed 25/s; she gets +30% speed. Ends on her primary fire,
  her ult, the key again, or the ally's jump (a launch).
- Catnapper: aim a ground spot up to 25 m, 0.35 s windup, dive at 30 m/s, land: 25 damage, knock-down 1.5 s in 5.5 m,
  the nearest enemy tethered and carried up to 4 s, healing herself 30/s.
- Minecraft: flying needs `mayfly` while she's in hero mode (a dedicated server kicks a player floating 4 s
  otherwise); the client's own creative-flight toggle stays off.

## Client
- Flight movement in OwMovement (her mover from the data: air accel 9.5, max air 12, drag 8 with no input; vertical
  speed isn't in the data: ours).
- Animator: hover idle (2-frame pose + bob loops), the 8-way flight loops by velocity relative to her facing, the
  boost pose additive while boosting, fire additive alternating paws, reload / melee / Purr / Catnapper / knock-down.
- HUD: ammo 24, Frenetic Flight's energy, Lifeline (Shift), Purr (E), Catnapper (ult 2575).

## Later
Sounds (a sound map from her banks, like Lúcio's), effects, voice (meow containers + Swedish jetpack lines), emotes.
