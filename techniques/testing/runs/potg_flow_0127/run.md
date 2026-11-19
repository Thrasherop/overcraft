# The whole Play of the Game, as a player gets it, on 0.3.127 (2026-11-18)

After the POTG review (#172, 0.3.120-0.3.126): the full flow in one run, single player (`GenPlay127`), Cassidy, harness
keys. Up: [testing](../../README.md).

1. **The End, the natural dragon and crystals removed;** four NoAI husks killed with Cassidy's shots one after another:
   `+250 High Score (kill minecraft:husk)` each, the window up to about 1950.
2. **A dragon at 5 health summoned, flying;** Deadeye (Q, then fire): `+150 High Score (ult)`, the dragon to 3.4 (its body
   takes a quarter). The fight music came on (`endMusic: true`) below a third.
3. **Shots aimed at it:** `the dragon died, killer Kiriko`; VICTORY's music (`potgMusic: true`, the fight music off).
4. **At the cue, 7.2 s later:** the chosen play is the run's best, `High Score 2541 as cassidy` (the husks, the ult, the kill);
   card, replay started 8.3 s into the card, "PLAY OF THE GAME CASSIDY" over the replay with the player card
   (`2026-11-18_19.42.46.png`: the husk ahead, the dragon above).
5. **End:** the replay done after 21.2 s, the world held for the end screen, then `end screen over (every end screen done)`
   7 s later; back in the world, 20 health, hero mode.

Not covered here (played on their own for #172): a watched highlight, a second player, a dedicated server.

## Resource reloads at a player's 2 GB heap (same build, same evening)
`OC_XMX=2G` (`-Xmx2G` on the command line), the full local pack, three reloads in a row through the harness's `reload`
(Minecraft's `reloadResourcePacks`, what F3+T calls; the desktop tool can't press F3+T as one chord): each done in 13 s, no
OutOfMemory or exception in the log, back at 117 fps as Cassidy. The process's working set: 5.7 GB at launch, 4.3, 5.0 and
5.1 GB after the three (no steady climb). #166's fix holds on 0.3.127.

## Hero mode on a ladder and in a boat (same build, same evening)
- **Ladder:** Cassidy in hero mode, holding forward against a 6-high ladder: at the top (y 121 to 127) within 3 s.
- **Boat:** on a pool (a solid stone block, its top layer water), `ride` into an oak boat, hero mode on; two Cassidy shots
  at a husk 7 m off at head height took it from 20 to 0.32 ("Kiriko started riding Oak Boat" in chat; `2026-11-18_19.59.36.png`).
- Getting there took three broken set-ups (a floating platform drained, a fill in a just-forceloaded area didn't land, a
  player parked on a pad that hadn't filled fell and drowned) and two false alarms: `state`'s `primaryAmmo` read 6 just after
  shots that had fired, and at night `@e[type=husk,limit=1]` picked a natural husk elsewhere. Lessons in harness.md.
- **Swimming:** Cassidy in the pool, hero mode on; two shots at a husk 7 m off: 20 -> 12.1 (one hit).
- **Lava:** stepped into a 2x2 lava pool in hero mode: 20 -> 16 -> 12 -> 8 over about a second, Minecraft's 4 every half
  second; hero mode stayed on. Teleported out to a spot past the stone block's edge, she fell about 11 m and died ("doomed to
  fall"): the fall, from my teleport, not the lava (her safe height is the hero's own, by design). Respawned: 20, hero mode.
