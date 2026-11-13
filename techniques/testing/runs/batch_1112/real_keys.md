# Real-input pass: the pickup rule (72f33b8) and the Workshop calibration (787c826), 2026-11-12

Up: [Techniques](../../../README.md) · beside it: [D.Va](../../../heroes/dva.md), [Pharah](../../../heroes/pharah.md)

One build of `next` + both branches (`test/batch-1112`), fresh survival world `rk_batch`, a fresh `gradlew runClient`.
Heroes picked on the select screen (Kiriko) and the hero menu at the spawn point (Pharah, D.Va); setup through the
harness; every action with the real mouse and keys. A state logger polled the harness every ~40 ms for the numbers.

| Step | Expected | Seen |
|---|---|---|
| Kiriko kills a NoAI blaze, walks over its rod | +1000 kill, +250 pickup | +1000, +250 |
| G, drop the rod (Q), G, walk over it | picked up, no score (owner 2026-11-12) | in the inventory, no score line |
| Walk over a summoned rod | picked up, no score | 2 rods held, no score line |
| Pharah holds Space 3 s from the ground | rises at 4.66 m/s (WS), fuel ~2.2 s, safe landing | 0.233 blocks/tick = 4.66 m/s; fuel out ~2.2 s; fell ~11 m, 18.0 health kept |
| Pharah presses Space 0.08 s | 0.98 m top (WS) | +0.979 m |
| Run 1 s, release W | last step 0.1375 m, then still (#130) | 0.275 m steps, then 0.13, then still |
| D.Va Boosters (Shift) into a NoAI husk | 25 OW = 2.0 less the husk's armor | 20 -> 18.08 |

The calibration agent's "Boosters did 2.9 / 3.9" was its daylight zombies burning, not Boosters.

Not tried with real input: Pharah's Barrage in the air, Jet Dash, Concussive's cooldown, D.Va's eject (the agent's
harness runs cover them), Lúcio's wall jump, a pearl from an enderman, a second player. D.Va's post-eject 2-health drop
stays unreproduced.
