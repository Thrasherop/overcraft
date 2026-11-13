# 0.3.41: the full real-input play before main (2026-11-13)

Up: [hand play](../../hand_play.md) · [map](../../../README.md).

This play used the real mouse and keys through computer-use, on `next` at 50e6cf0 from the main checkout (local asset
pack loaded). The harness (`tools/dev.py`) was used only for set-up commands and for reading state. To aim, the harness
turned the view toward a target (`execute anchored eyes facing entity`); every shot, ability and menu click was a real
input. The world was `full_play`: survival, Normal, real terrain (a jungle), with a 29 x 29 clearing at the spawnpoint.

## What was played, expected vs seen

| Step | Expected | Seen |
|---|---|---|
| Join a new world; Tracer from the select screen | Her pistols in first person, HUD 175, 40/40 | As expected |
| Midnight in the jungle, real spawns | A hard night | 17 zombies, then 51 creepers and more, gathered at the spawnpoint; Tracer died six times (vanilla 3 MC a hit about every 1.4 s, measured idle). Back as Tracer after each respawn, with full health and ammo |
| Tracer: held fire at a husk 12.5 m away; Blink; Recall | Spread at range; Blink 7.5 m forward; Recall back to 3 s before | 40 rounds did ≈98 OW at 12.5 m; Blink +7.5 m; Recall took her back 9.1 m |
| Tracer: firing while backing off (S held) | 5.5 m/s and still firing | A zombie went 20 → 8.2 MC; she backed 6.6 m in 1.2 s |
| Tracer: Pulse Bomb into three zombies | Kills | Two killed (kill feed, "Monster Hunter"), the third at 5.6 MC |
| H at the spawnpoint → Cassidy | Hero menu mid-game | His revolver, HUD 250, 6/6 |
| Cassidy: 4 shots at a skeleton | 70 a shot | Skeleton dead |
| Cassidy: Deadeye on two zombies, fired after 2 s | Both locked and killed | Both killed in one shot (kill feed) |
| H 10 m from the bed | Refused | "Go back to your bed to change heroes"; walked back, then H worked |
| Pharah: a rocket into three zombies | 120 direct + splash | 11.45 MC on the one hit, 5.2 and 4.1 of splash |
| Pharah: Space held 1.6 s, then Barrage | Rises at 4.66 m/s; Barrage holds her in the air | Rose 7.5 m at 0.233 blocks/tick; held at the top for about 2.5 s while firing; fell and landed with no damage. The zombies were burning in daylight, so Barrage's damage wasn't separable |
| D.Va from the hero menu | MEKA, 675 HP, Boosters and Micro Missiles on the HUD | As expected. **No model**: this machine's local pack has no D.Va model (nor Sigma's, Sojourn's and some supports'); a bare arm in first person and no portrait on the select screen. A local asset gap, not code |
| D.Va: Fusion Cannons 2 s, Micro Missiles, Boosters, Self-Destruct | Damage; the blast kills | Cannons took a zombie 20 → 4.7; Self-Destruct killed a zombie (kill feed). Baby D.Va at 63 of 175 afterwards: from a zombie, see the next row |
| D.Va: Self-Destruct with no mobs near (logged) | Her own blast doesn't hurt her (0.3.37) | 54 → 14 (MEKA gone, baby at full 14 of 14), no further loss. The old "2 health after eject" report did not appear |
| D.Va dies as baby, respawns | In her MEKA | Mode 0, 54 health |
| G → plain Minecraft: punch two jungle logs | Logs | Both broken; one dropped behind the other and was picked up after the second (vanilla reach) |
| Inventory grid: 2 logs → planks | Planks taken | **Failed: clicking the result slot (left, right, shift, held press, press-and-drag) takes nothing.** Q over the result does craft and drop it. A crafting table's result fails the same way. **The same on main (0.3.30)**, so not a regression in next. The 0.3.29 release play crafted by clicking. Open issue; it may be how computer-use's clicks arrive, which needs a check with a real mouse |
| Save and Quit; reopen the world | The hero kept, the select screen | D.Va highlighted, select screen open (the world was saved in Minecraft mode) |
| Junkrat from the select screen on rejoin | His launcher in first person (local pack) | As expected |
| The End, the dragon at 8 health, frozen with NoAI (a test aid), 5 frags | The kill scores and a POTG follows | VICTORY, the kill feed, "Free the End"; `+2500 High Score (kill minecraft:ender_dragon)`. With NoAI the death animation didn't advance (DragonDeathTime 0); once NoAI was removed it ran |
| Play of the Game | Card, replay, end screen, thaw | Intro card ("Play of the game, Kiriko, as Junkrat", his model) for 8.3 s, the replay with its banner for about 13 s, the end screen, the world thawed; back as Junkrat where he stood, 20 health |
| Pause → Highlights | Top 5 plays | Junkrat (the End), Tracer, Cassidy, Junkrat, D.Va, with portraits |
| Save and Leave, Quit Game | Clean exit | As expected |

## Mistakes of mine during the play (not bugs)

- My first aim helper aimed from the feet, so the shots passed over the target's head. Fixed with `anchored eyes`.
- I left test mobs alive between steps, and my round trips (3 to 5 s) let them reach the hero. Several deaths came
  from that. Clear every test mob as soon as its step is over.
- One quit click missed, and two clients ran at once for a short while. The newer one never got the harness port. I
  closed both windows and relaunched one client.

## Not tried

Sigma, Sojourn and the supports (not new since main); Junkrat's kit beyond frags (played on 24d22a1); D.Va's Defense
Matrix; Lúcio's wall jump (calibrated in 0.3.39); the blaze-rod and pearl pickup scoring (played in 0.3.39); a second
player; water, lava and the Nether; mining stone with a pickaxe, building and chests (blocked by the crafting result).

## Correction: the play ran from a partial asset pack

The owner noticed missing assets in this play's screenshots. The main checkout's pack lacked the models, portraits,
effect and UI textures of seven heroes, which were in the old shared pack (`overcraft_ana`). D.Va's missing model above
came from that, not from her code. The packs were merged afterwards (`techniques/building/building.md`, "One local asset
pack"). A recheck on the merged pack showed D.Va's model, every portrait and her ability icons on the select screen,
and her controls and cannons in first person. The rest of this play's outcomes don't depend on assets.
