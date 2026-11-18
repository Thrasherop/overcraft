# General play before main, 2026-11-18 (0.3.95 -> 0.3.97), at a player's 2 GB heap

A fresh client from `next` (bf751c2) with `OC_XMX=2G`, a new survival world (`GenPlay097`, Normal), the full local pack,
real mouse and keys unless marked. Changed since [genplay_0395](../genplay_0395/run.md): rotation keys as shorts (0.3.96),
piece-keyed caches cleared with the models and no on-demand loads mid-reload (0.3.97, from a critique), all #166.
Screenshots `run/screenshots/gp97_*.png`, `fp97/<hero>.png`, `fp97_sheet.png`. Up: [testing](../../README.md).

- **On join:** the select screen came up; Junkrat picked with two real clicks.
- **Every hero, from H** (Brigitte, Illari, Juno by `screenclick`), fired at a still husk 6 m off: the same results as
  genplay_0392 and _0395 (Sigma 1.31, Pharah 1.11, Tracer's four quick clicks 11.2, Lifeweaver 14.59, Ana 8.82,
  Brigitte 9.2, Illari 2.29, the rest killed; Mercy's staff left it at 20, the control). All 20 drew their first-person
  arms and weapon (`fp97_sheet.png`).
- **Heal:** Kiriko's ofuda, villager 6 -> 15.6.
- **Death and respawn:** three husks, 20 -> 14 -> 5 -> 0; Respawn: Kiriko, 20, hero mode.
- **The emote wheel:** a real quick right click turned to EMOTES; Pod Pet played with its pod.
- **F3+T at 2 GB in the played world:** 51 s, no out-of-memory; mesh pieces 433 -> 430 (flat), Kiriko and the pod drawn after.
- **Leave and rejoin:** Save and Quit, Play (`screenclick`), the select screen with Kiriko; Ready.
- **Minecraft mode (G):** on the ground, a block dug by hand and placed back with the use key; G back to hero mode.
- **Not tried this time** (done in genplay_0388, untouched since): crafting, a chest, ladders, a boat, the Nether, night
  spawns, sprays, a second player.
