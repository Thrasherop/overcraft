# General play before main, 2026-11-18 (0.3.108 -> 0.3.113), at a player's 2 GB heap

A fresh client from `next` (09a7b35) with `OC_XMX=2G`, a new survival world (`GenPlay113`, Normal), the full local pack.
Changed since [genplay_0107](../genplay_0107/run.md): D.Va's armor under Shield System (0.3.108, #168), Play of the Game fixes
(0.3.109 to 0.3.113, #170: no damage while watching, no phantom ult on a hero change, replays keep their time of day, no hero
select after a showing, segments don't roll mid-play, a showing starts only its own replay, no flying kick for viewers, a
quitting viewer doesn't hold the others), and dev runs' temp files in `run/tmp`. Each was reproduced and replayed on its own
first. Screenshots `run/screenshots/gp113_*.png`, `fp113/<hero>.png`. Up: [testing](../../README.md).

**Input:** the owner was using the machine (another window in front), so this run used the harness only: `screenclick` for the
select screen and menus, `key`/`press` for keys. Real mouse and keys were used for these same steps in genplay_0107, and
again today in the separate POTG checks.

- **On join:** the select screen. D.Va picked (`screenclick` on the portrait, then Ready): hero mode, 675 health.
- **Every hero** fired at a still husk 6 m off: the same as genplay_0107 (Sigma 1.31, D.Va 4.13, Pharah 1.11, Junkrat 0.32,
  Cassidy 14.49, Widowmaker 15.78; Mercy's staff and Lifeweaver's blossom heal, so they left it at 20).
- **D.Va, Shield System (0.3.108):** Bunny Power and Shield System picked from the perk menu (40, 42), layer 425. A 95 hit
  on full shields took 95; a 50 hit with 5 shields left took 43 (5 + 45 - 7).
- **Death:** three husks killed Kiriko in 7 s. **Respawn:** Kiriko, 20 health, hero mode.
- **Heal:** three Healing Ofuda took a villager from 6 to 20.
- **Minecraft mode (G):** dirt placed with the right button. Digging didn't work through the harness: Minecraft keeps mining
  only while the mouse is captured, and it wasn't, with the owner's window in front. Real-mouse digging passed in
  genplay_0107, and nothing since touched Minecraft mode.
- **Play of the Game:** a scored Cassidy play (975), then a dragon killed properly in the End (health 1, then TNT: `/kill`
  skips the death event). Card, replay at 8.4 s, ended at 22.8 s, end screen held, world thawed. After: no hero select, hero
  mode on.
- **A watched highlight (0.3.109, 0.3.110):** from the Highlights menu, with a husk summoned beside the watcher. 20 health
  through the whole intro; replay at 5.2 s, ended at 18.9 s. After: no hero select, hero mode on, and the husk hit normally
  (20 -> 17).
- **Rejoin:** left at 11 health through the menu's Save and Leave (the menu showed 0.3.113), back with `openworld`: 11 health,
  Cassidy, hero mode. The join's select screen was offered; Esc kept Cassidy.
- **F3+T at 2 GB:** 61 s until the last hero model loaded, no out-of-memory, 96 fps after.
- **Real-input pass, after the owner left the PC** (same world, relaunched at 2 GB), real mouse and keys only:
  - **Select screen:** a portrait click and Ready (it picked Mercy: the window sat smaller than expected), hero mode on.
  - **Shots:** Cassidy (set by the harness) fired two left clicks at a still husk: 20 -> 8.98, 6 -> 4 rounds.
  - **Minecraft mode:** G, then dirt placed with the right button and dug with the left held (the item dropped); a chest
    placed and opened.
  - **Hero mode:** G back, then F on the chest opened it as an empty hand would (0.3.104); Esc went back to the hero HUD.
- **Not tried this time:** chests, crafting, ladders, boats, the Nether, night spawns, perks other than D.Va's, real-mouse
  digging (see above). Two players were checked separately today (LAN, the quitting-viewer case).
