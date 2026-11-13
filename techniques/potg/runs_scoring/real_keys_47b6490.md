# Scoring: the coordinator's real-input pass (47b6490, 2026-11-12)

Up: [scoring_values](scoring_values.md) · [POTG design](../design.md)

Fresh `gradlew runClient`, new survival world `rk_scoring`, hero picked by clicking Ready on the select screen
(Kiriko). Setup through the harness (day, `spawn_monsters false`, one NoAI blaze 5 blocks ahead); every action after
that with the real mouse and keys.

| Step | Expected | Seen |
|---|---|---|
| Hold left mouse on the blaze ~6 s | blaze dies, +1000 (250 health + 750) | `+1000 High Score (kill minecraft:blaze)`, highlight 1250 with its damage |
| Walk (W) over its rod | +250 | `+250 High Score (pickup 1 minecraft:blaze_rod)` |
| Same 12 s window | kill and rod in one highlight | highlight 1285 (kill 1000, rod 250, a slime 13, the last damage) |
| G, then walk over two summoned rods | rods picked up, no score | 3 rods in the hotbar, no `[POTG]` line |
| Esc, Save and Quit, Quit Game | clean exit | clean |

Also seen: a stray kunai killed a small slime for +13 (expected: slime is worth its health share, no bonus).

Not tried with real input (the builder's harness run covers them): enderman, wither skeleton, crystals, eyes, portal,
first Nether/End entries, rejoin. No second player.
