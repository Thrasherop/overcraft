# General play before main, 2026-11-18 (0.3.88 -> 0.3.92)

A fresh client from `next`, a new survival world (`GenPlay091`, Normal), the full local pack, real mouse and keys unless
marked. What changed since [genplay_0388](../genplay_0388/run.md): first-person models and emote props load on first use
(#166), Grappling Hook lets go of a Jetpack Cat's rope (#142). Screenshots `run/screenshots/gp91_*.png` and
`run/screenshots/fp91/<hero>.png`. Up: [testing](../../README.md).

- **On join:** the select screen came up; Lucio picked with two real clicks (Ana and Sigma the last two times).
- **Every hero, from H** (the last three with `screenclick`: the dead click strip), each fired at a still husk 6 m off, and
  each one's first-person view screenshotted the moment it was picked: **all 20 drew their arms and weapon** (the contact
  sheet `fp91_sheet.png`), so the on-demand loading doesn't leave a hero bare.

| Hero | Result | Hero | Result |
|---|---|---|---|
| Lucio | killed | Kiriko | killed |
| Sigma | 20 -> 1.31 | Jetpack Cat | killed |
| D.Va | killed | Mercy (staff) | 20 -> 20 (the control) |
| Sojourn | killed | Mercy (Blaster, 2) | killed |
| Pharah | 20 -> 1.11 | Lifeweaver (right button) | 20 -> 14.59 |
| Tracer (4 quick clicks) | 20 -> 11.2 | Ana | 20 -> 8.82 |
| Cassidy | killed | Sombra | killed |
| Junkrat | killed | Brigitte | 20 -> 9.2 |
| Torbjorn | killed | Illari | 20 -> 2.29 |
| Ashe | killed | Juno | killed |
| Widowmaker | killed | | |

The numbers match 0.3.88's where both were partial (Sigma, Pharah, Lifeweaver, Ana, Brigitte, Illari).

- **Heal:** Kiriko's Healing Ofuda took a villager 6 -> 15.6.
- **Death and respawn:** three husks with AI took Kiriko 20 -> 14 -> 5 -> 0; Respawn: Kiriko, 20, hero mode on.
- **Leave and rejoin:** Save and Quit, the world list, Play (selected through `screenclick`: a real click there would have hit
  the taskbar); the select screen with Kiriko chosen; Ready; her first-person arms drew (`gp91_rejoin_fp.png`).
- **The emote wheel (BUG, fixed in 0.3.92):** T held (harness), a real right click to turn to the emote page: the wheel
  stayed on Communications twice (`gp91_wheel.png`, `gp91_wheel_quick.png`) and letting go sent "Need healing". Held 0.25 s
  the button worked (`gp91_wheel_held.png`). Cause: `CommsWheel` read only whether the button was down. Fixed with the
  consumed clicks (as Tracer's 0.3.81 fix and the spray key's). Replayed on the fix: the same quick click turned to EMOTES
  (`gp91_wheel_fixed.png`), pointing right picked Pod Pet, which played with its pod (loaded then, `prop_1b7dd`).
- **Minecraft mode (G):** on the ground, G to Minecraft mode (hearts, hunger, hotbar), G back to hero mode: both in first person.
  (A third-person view after the emote was the emote camera putting back the view from before it: mine, set for the emote.)
- **Not tried this time** (done in genplay_0388 on 0.3.88, untouched since): crafting, a chest, ladders, a boat, the Nether, night
  spawns, sprays. A second player: done separately ([two_client_2026-11-18](../two_client_2026-11-18/run.md)).
