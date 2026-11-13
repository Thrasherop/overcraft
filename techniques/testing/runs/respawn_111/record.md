# #111 again: Tracer at 11, 10, 7 after respawns (2026-11-13, next 5ca5da1, 0.3.42)

Up: [hand play: Respawning at 14-16 of 20](../../hand_play.md#respawning-at-14-16-of-20-111-2026-11-04) · [harness](../../harness.md).
**Not a respawn bug: nothing changed.** Every respawn read full 0.05 s after the Respawn button; the reported numbers
appear only when mobs stand at the spawnpoint, and then exactly (Normal's zombie: 3 a hit).

Setup: one dev client (`OC_ASSET_PACK` = the main checkout's pack), fresh flat survival worlds `resp1`, `resp2`, Normal,
`spawn_monsters false`, slimes killed (the first `/kill @e` ran during the loading screen and missed 23 of them: kill them
once the world is up). Driven by DevServer (`respawn` = the death screen's button); health is the client's synced value.
Scripts in `scripts/` (run with the client up: `py -I scripts/deaths.py tracer 6`).

| Step | Expected | Seen |
|---|---|---|
| `/kill @s` x6 as Tracer, no mobs (`deaths.py`) | 14 at 0.05 s, 1 s, 3 s | 14 / 14 / 14 every time |
| `/kill` x3 each: Sigma, D.Va, Kiriko, Lúcio, Juno, Brigitte, Sojourn, Junkrat | full, shields full | full every time (Sigma 48 + 250, Juno 18 + 150, Brigitte 20 + 75 armor). D.Va: the first `/kill` ejects (pilot 14), the next respawns her in a full mech 54 + 325. Sojourn once read 20 of 18 at 0.05 s (the client before the max-health sync), 18 by 1 s |
| Tracer, one death each (`causes.py`): lava, fall, fire + wither, wither, poison + hunger, a zombie's `mob_attack`, three live zombies, drowning, `/kill` in Minecraft mode | 14 at once | 14 at 0.05, 0.5, 1, 2, 3 s every time; food 20 |
| Pulse Bomb stuck at her feet, `/kill` 0.25 s later, respawn on the spot (`bomb.py`) x5 | 14 | 14 through 2.5 s (alive, her own bomb at her feet takes all 175: `BOMB_SELF` 0.5 x 350) |
| Killed mid-Recall (hurt 6 first, E, `/kill` 0.2 s in) x3; mid-Blink x3 | 14 | 14 / 14 / 14 |
| **Three zombies on the spawnpoint** (`mobspawn.py`), `/kill` x4, hero mode | full at once, then hits | 0.05 s: 14 every time; then **11** (1-1.5 s), 11, **8 / 7 / 5** by 3 s: the issue's 11, 10, 7 |
| Two zombies at the spawn, screenshots | | `r111_zombies_0p1s.png` 175/175, `r111_zombies_2p6s.png` 100/175 and burning (a daylight zombie sets its target alight); `r111_kill_noMobs_0p2s.png` 175/175 |

**Checked in code:** `HeroManager.onRespawn` -> `applyKit(fresh)` sets the max-health modifier, then health to max, then
the layer (shields/armor) full; vanilla `PlayerList.respawn` (26.3) builds the new player, `restoreFrom`, then
`setHealth(getHealth())` before Fabric's AFTER_RESPAWN, so nothing after it lowers health. 26.3's `ServerPlayer.hurtServer`
has **no spawn invulnerability**: a mob at the spawn hits at once, in vanilla too.

**A false lead to avoid:** a mid-run probe showed Minecraft's food regeneration healing Tracer (+0.83 every 0.5 s,
saturation falling 5 -> 0). It was Minecraft mode: the previous script's last step pressed G, and `hero tracer` for the
hero already picked seemingly didn't turn hero mode back on (inferred: `heroMode` wasn't read in that probe). With a debug line in `FoodDataMixin` (removed), `controls=true` and no
food healing in hero mode, after a respawn or not. **Read `heroMode` in the state before trusting a health curve.**

**Not tried:** a real second player's kill; Mercy's Resurrect; the 11-13 session's own setup (not recorded). **Open for
the owner** (as on 2026-11-04): whether heroes get spawn protection, since Minecraft has none.
