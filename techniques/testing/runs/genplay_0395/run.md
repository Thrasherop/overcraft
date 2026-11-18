# General play before main, 2026-11-18 (0.3.92 -> 0.3.95), at a player's 2 GB heap

A fresh client from `next` (59097d2) started with `OC_XMX=2G` (the Minecraft launcher's default heap: what players get), a new
survival world (`GenPlay095`, Normal), the full local pack, real mouse and keys unless marked. What changed since
[genplay_0392](../genplay_0392/run.md): clips share key times and still bones keep one key (0.3.93), old models freed before a
reload (0.3.94), model files streamed (0.3.95), all #166. Screenshots `run/screenshots/gp95_*.png`, `fp95/<hero>.png`,
`fp95_sheet.png`. Up: [testing](../../README.md).

- **On join:** the select screen came up; Mercy picked with two real clicks.
- **Every hero, from H** (Brigitte, Illari, Juno by `screenclick`: the dead click strip), fired at a still husk 6 m off: the
  same results as genplay_0392 (Sigma 1.31, Pharah 1.11, Tracer's four quick clicks 11.2, Lifeweaver 14.59, Ana 8.82,
  Brigitte 9.2, Illari 2.29, the rest killed; Mercy's staff left it at 20, the control). All 20 drew their first-person
  arms and weapon (`fp95_sheet.png`).
- **Heal:** Kiriko's ofuda, villager 6 -> 15.6.
- **Death and respawn:** three husks took her 20 -> 0; Respawn: Kiriko, 20, hero mode.
- **The emote wheel:** a real quick right click turned to EMOTES (0.3.92's fix); Pod Pet played with its pod.
- **F3+T in the played world at 2 GB:** 54 s, no out-of-memory; Kiriko and the emote's pod drawn after. (In Pod Pet's third,
  looping clip she stands and the pod hovers over her head: the same with no reload, so the emote's own.)
- **Leave and rejoin:** Save and Quit, Play (`screenclick`), the select screen with Kiriko; Ready; first person drawn.
- **Minecraft mode (G):** on the ground, a dirt block dug by hand into the hotbar and placed back with the use key; G back to
  hero mode, her arms drawn.
- **Not tried this time** (done in genplay_0388, untouched since): crafting, a chest, ladders, a boat, the Nether, night
  spawns, sprays, a second player.
