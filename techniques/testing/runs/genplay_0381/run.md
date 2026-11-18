# General play before main, 2026-11-17 (0.3.80 -> 0.3.81)

A fresh survival world (`GenPlay080`, Normal), real mouse and keys unless marked. Up: [testing](../../README.md).

**Every hero picked and fired at a still husk 6 m off** (health read from the server):
- **On join:** the select screen came up, and Sigma was picked.
- **Mid-game, with H (the hero menu):** each of the other 19. The last three supports (Brigitte, Illari, Juno) were picked with the harness `screenclick`. Real clicks at screen x >= ~935 never reached the game; a window hidden by computer use sits there. The same strip swallowed the crafting result slot. The screen's own hit test works.

| Hero | Fired, and what it did |
|---|---|
| Sigma | Hyperspheres killed it; Accretion's cooldown started |
| D.Va | Fusion 20 -> 10.6; Boosters flew |
| Sojourn | 20 -> 0.72; Disruptor's cooldown started |
| Pharah | 20 -> 10.55; Concussive Blast's cooldown started |
| Cassidy | 20 -> 8.98 |
| Junkrat | 20 -> 0.32. A first try in the trees missed (its frags bounced) |
| Torbjörn | 20 -> 3.47 |
| Ashe | 20 -> 11.8 |
| Widowmaker | five hip shots, 20 -> 14.72 |
| Kiriko | 20 -> 5.83 |
| Lúcio | 20 -> 6.55 |
| Jetpack Cat | 20 -> 14.65 |
| Mercy | Blaster on 2: 20 -> 15.43 |
| Lifeweaver | Thorn Volley on the right button: 20 -> 11.97. His keys work: #146's "may look dead" note is answered |
| Ana | 20 -> 3.22 |
| Sombra | 3 rounds, 20 -> 18.55 |
| Brigitte | 20 -> 9.2 |
| Illari | 20 -> 3.14 |
| Juno | killed it |

- **Tracer: BUG, fixed in 0.3.81.** Four quick real clicks fired nothing (40/40). Her pistols read only `primaryHeld`, and a click whose press and release both reach the server between two of its ticks never shows held. She now also fires on `primaryFresh`. Replayed on the fix: the same clicks fired 40 -> 36 and the husk went 20 -> 18.27.
- **Heal:** Juno's Mediblaster took a villager ally from 6 to 17.5.
- **Deaths and respawns:** Sombra was slain by a zombie twice at night; the Respawn button brought her back as Sombra at full health. The night came because the 26.3 gamerule is `advance_time`, not `doDaylightCycle`.
- **Leave and rejoin:** after a full client restart, the select screen came up with the last hero (Juno) chosen.
- **Minecraft mode (G)** (plain Minecraft keys; the walking and turning went through the harness's Minecraft keys):
  - Mined a grass block by hand and picked up the dirt.
  - Placed the dirt (right click).
  - Swam in a river and climbed out.
  - Chopped 2 oak logs and a birch log by hand.
  - Crafted 8 oak planks in the 2x2 grid with real clicks, then a crafting table through the recipe book (the result taken with `screenclick`: dead strip).
  - Placed the table.
  - One chest was given by command (a 12-plank chest needed more logs than were left), placed with the use key, and opened. The 2 dirt went in with real clicks: chest slot 0 shows Dirt x2, and the inventory has none.
  - Health in Minecraft mode stays the hero's (Juno 18): by design (dva.md #3).
- **Not tried:** a second player, the Nether, ladders, boats, a crafted chest.
