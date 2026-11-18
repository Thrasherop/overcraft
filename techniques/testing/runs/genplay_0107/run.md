# General play before main, 2026-11-18 (0.3.103 -> 0.3.107), at a player's 2 GB heap

A fresh client from `next` (30a10c5) with `OC_XMX=2G`, a new survival world (`GenPlay107`, Normal), the full local pack.
Picks, the ofuda, Esc, G, F, digging, placing and the chest used the real mouse and keys (desktop control). Set-ups and the
weapon sweep used the harness. Changed since [genplay_0102](../genplay_0102/run.md): perk fixes (0.3.103), hero mode and
Minecraft mode fixes (0.3.104: no item use in hero play, F interacts as an empty hand), hitscan and the Ender Dragon
(0.3.105), point-blank shots (0.3.106), end crystals (0.3.107). Each was reproduced and replayed on its own first.
Screenshots `run/screenshots/gp107_*.png`, `fp107/<hero>.png`. Up: [testing](../../README.md).

- **On join:** the select screen. Sigma was picked with two real clicks (portrait, then Ready), and the hero HUD came up.
- **Every hero** fired at a still husk 6 m off with one press (0.2 s, or 1.2 s held for charge or spray weapons). Husk
  health afterwards: D.Va 7.39, Kiriko (kunai) 5.83, Lúcio 6.55, Jetpack Cat 5.74, Ana 14.41, Sombra 5.97, Brigitte 9.2,
  Illari 8.19, Juno 6.58, Sojourn 15.96, Pharah 1.11, Tracer 13.53, Cassidy 14.49, Junkrat 0.32, Torbjörn 3.47, Ashe 14.53,
  Widowmaker 15.78, Sigma 10.66. Mercy's staff and Lifeweaver's blossom heal, so they left it at 20.
  - Sigma's first turn did nothing because hero mode was off. My platform fill had put stone where the player stood, and
    HeroBody found no room for his body, so it switched to Minecraft mode with its "no room" message, as it should.
    `changehero` to the same hero doesn't turn hero mode back on. A test set-up mistake, not a bug.
- **Death:** three husks killed Kiriko in 6 s; the death screen and kill feed showed. **Respawn:** Kiriko, 20 health,
  hero mode. (Two later deaths were mine as well: the spawn under the platform, then leftover husks.)
- **Heal:** Kiriko's Healing Ofuda, three real right clicks, took a villager from 6 to 19.4. The harness's `key key.use`
  doesn't reach a hero's fire buttons (they're Overcraft's own key mappings), so use the real mouse.
- **Rejoin:** left at 8 health with real Esc and Save and Leave, came back through Singleplayer. Health 8.45 (her
  regeneration, not 20), still Kiriko, and the join's select screen offered. Esc kept Kiriko.
- **Minecraft mode (G, real key):** dirt placed with the right button and dug back with the left; a chest placed and opened.
- **Hero mode (G again):** F on the chest opened it as an empty hand would (0.3.104). Esc went back to the hero HUD.
- **F3+T at 2 GB:** 58 s until the last hero model loaded, no out-of-memory, 119 fps after.
- **Not tried this time** (done in genplay_0388 or the two-client runs, untouched since): crafting, ladders, a boat, the
  Nether, night spawns, a second player, perks picked by hand (0.3.103's were played on their own), the real End fight.
