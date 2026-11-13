# Cassidy: the coordinator's real-input pass (633b3c4 + next 0.3.39, 2026-11-13)

Up: [Cassidy](../../../heroes/cassidy.md) · beside it: `critic_b79a4d1.md`

Fresh `gradlew runClient`, new survival world `rk_cassidy`. Cassidy picked by clicking his C on the select screen (card:
Damage, Sharpshooter, 250 health, LMB / RMB / LSHIFT / E / Q) and Ready. Setup (day, no monsters, a NoAI 500-health husk
8 m ahead, `/overcraft ult`, slimes cleared) through the harness; every action with the real mouse and keys. A state
logger polled the harness every ~40 ms.

| Step | Expected | Seen |
|---|---|---|
| HUD | 250 health, Roll LShift, Flashbang E, 6 rounds | as expected (5/6 after the click into the window) |
| That click, at the husk's head | 140 head (less armor) | -11.02 (138 OW) |
| 3 body shots, 0.6 s apart | 210 less armor | -16.53 (207 OW), 2 rounds left |
| Shift (Combat Roll) | ~6 m, cylinder full, 5 s cooldown from its end | 6.14 m, 6 rounds, 3.8 left 1.2 s after |
| E, then Shift 0.15 s later (the critic's desync, no lag) | Flashbang 75; roll refused on both sides | husk -5.91 (74 OW); no move, roll cooldown 0 |
| Q, Space held 0.3 s, then fire | no jump; one shot | y stayed -60.0; fired, ult spent |
| Q, 2.2 s, fire (no screenshot between) | about 300 (2 s at 150) less armor | -31.88 (398 OW) |

The last row is about 0.3 s of lock more than counted: the key and click latency and the lock running until the shot
leaves (shots go 0.066 s after their turn). The ramp itself is right; a frame-exact check would need the harness's trace.
A first try with a screenshot between Q and the click took about 4 s and dealt ~750, consistent with 150/300.

Not tried with real input: Fan the Hammer, the perks, the dragon, a hack or hinder on him, lag, death, a second player.
