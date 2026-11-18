# General play before main, 2026-11-18 (0.3.97 -> 0.3.102), at a player's 2 GB heap

A fresh client from `next` (d24381f) with `OC_XMX=2G`, a new survival world (`GenPlay102`, Normal), the full local pack,
real mouse and keys for picks and fire, the harness for set-ups. Changed since [genplay_0397](../genplay_0397/run.md): spray fixes
(0.3.98), comms wheel and loadout (0.3.99), hero select and rejoin (0.3.100), Coach Gun pellets and Mercy's boost credit
(0.3.101, #167), no ult charge from hurting allies (0.3.102, #168): each from an independent critique, reproduced and replayed
on its own. Screenshots `run/screenshots/gp102_*.png`, `fp102/<hero>.png`, `fp102_sheet.png`. Up: [testing](../../README.md).

- **On join:** the select screen; Pharah picked with two real clicks.
- **Every hero** (H; Brigitte, Illari, Juno by `screenclick`) fired at a still husk 6 m off: the same results as genplay_0397
  (Sigma 1.31, Pharah 1.11, Tracer's four quick clicks 11.2, Lifeweaver 14.59, Ana 8.82, Brigitte 9.2, Illari 2.29, the rest
  killed; Mercy's staff left it at 20). All 20 drew their first-person arms and weapon (`fp102_sheet.png`).
- **Heal:** Kiriko's ofuda, villager 6 -> 15.6. **Death:** three husks; Respawn: Kiriko, 20, hero mode.
- **The wheel (0.3.99):** a page turned with the left button held as T was let go: no kunai thrown (15 -> 15); a quick T tap
  sent "Good job!".
- **Sprays (0.3.98), at night:** a spray on a fence post's upper side still there after 2 s; a ceiling spray upright and dim.
- **F3+T at 2 GB:** 42 s, no out-of-memory.
- **Rejoin (0.3.100):** left at 7 health, back at 8.3 (her regeneration), not 20; Esc on the join screen, then H 2 km away:
  nothing; H at the bed: the select. (Rejoined with the harness `openworld`: the window had shrunk and the menu moved.)
- **Minecraft mode (G):** a block dug by hand and placed back; G back to hero mode.
- **Not tried this time** (done in genplay_0388, untouched since): crafting, a chest, ladders, a boat, the Nether, night spawns,
  a second player (the two-client checks of #167 ran separately).
