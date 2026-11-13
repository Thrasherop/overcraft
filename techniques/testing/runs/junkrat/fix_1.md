# Junkrat fix 1 (2026-11-13): the critic's five defects and three suspected

Up: [junkrat hub](../../../heroes/junkrat.md) · [map](../../../README.md). Lessons: [minecraft_26](../../../minecraft/minecraft_26.md)
(a root is ADD_MULTIPLIED_TOTAL; soft landings only on upward pushes).

Fresh flat survival worlds (`jr_fix_a` on af438ea for the repros, `jr_fix_b` on the fix), `spawn_monsters false`, one client.
Driven through `scripts/drive.py` (the harness's key mappings, not the OS mouse and keys); every line stamped in `runs.txt`
under the labels below. Scripts: `scripts/f1.txt` ... `f7.txt`, `p3.txt`, `p3c.txt`.

| # | Steps | Before (af438ea) | After |
|---|---|---|---|
| 1 | F1: /tp to y -40 over the -60 floor, ult 100, Q 0.15 s in; y every 0.5 s; Q again | y -40.546 for 4 s (held whole), then the fall at the blast: 20 -> 4 | falls at once (y -44.8, -52.7, down at 1.6 s, tire still out), 20 -> 3: the same as a plain 20 m fall (F3a) |
| 1 | F1b: Q on the ground, W + Space held 1 s | (not run) | the tire drove 12 m/s (0.60 a tick) and jumped (y -58.6); he stayed at 0.5 -60 0.5, no jump |
| 2 | F2: trap 2.9 m ahead, an AI baby husk walking in | sprung (100 -> 92) and walked on 2.92 -> 2.46 -> 0.02 -> -0.61 while held | sprung, held at z 4.118 till ~2.5 s, then walked on |
| 3 | F3a: a 20 m /tp fall | 20 -> 3 | 20 -> 3 |
| 3 | F3b: the fall, a frag at a NoGravity husk 4 m below, 0.4 s in (vy after the push -0.29..-0.33 a tick) | landed 20.0 | landed 3.0 |
| 3 | F3c/F3d: the mine jump at his feet; a frag jump off a wall | (J2d earlier: soft) | mine jump 20 -> 20 over a 13 m arc; frag hop 20 -> 20 |
| 3 | P3a / P3c (Pharah): a 20 m fall; the fall with a rocket at a husk 12 m below 0.9 s in (vy after -0.617) | 20 -> 1; with the rocket 17.07 (her own splash only, no fall) | 20 -> 1; with the rocket 17.07 -> 0.07 (the fall counted) |
| 3 | P3b (Pharah): a rocket 0.4 s in that leaves her rising (vy +0.06) | 16.4, soft | 16.4, soft (unchanged, by design) |
| 4 | F4: Q, then `/execute in the_nether run tp`; ult every 2 s | ult 0 for 12 s, still 0 back in the Overworld | tire ended at once (mode 0, tire_end); ult 18 -> 28.5 once his client had loaded the Nether |
| 5 | F5: a 1 m stone wall between him and the mine (3.1 m), the right button | launched 15 up / 15 back through it | unchanged: the data's test is distance only (below) |
| - | F6: mine and trap out in the Nether, /kill, respawn in the Overworld | mode 66 (mine and trap "out") after the respawn | mode 0 |
| - | F7: he dies 1 m from a 100-health NoAI husk | (J-runs: bombs go off) | the husk dead within 3 s (six bombs x 50), no errors in the log |

**The fixes** (one commit): his walk and jump off on the tire, not his body (`ClientJunkrat.inputsOff`, `KeyboardInputMixin`,
`OwMovement.noJump`; the keys still reach the server for the drive); `driveTire` no longer forgets his fall; the root
ADD_MULTIPLIED_TOTAL -1 (`Combat.buff(..., op)`), a rooted jump cancelled whatever Jump Boost adds, the root out of a
Stalwart's slow resistance (ours); self-push soft landings only when vy > 0 after the push (Junkrat's blasts, Pharah's rocket and
Concussive Blast); `clearWorld` ends the tire (ultActive 0); equip keeps the dimension so a respawn elsewhere clears the mine
and trap; a tire past 10.5 s of his game time goes off on his clock (one stopped beyond the simulation distance).

**#5, kept from the data:** 096A #297 (not hacked) -> #264 / #438 `op71(op94(self, v431), 3.5)` -> #262 / #439 the launch. No
TrackTargets, no sight filter (`E:\Overwatch Archive\research\junkrat\work_kit\c_096A.txt`); the blast on enemies is the one with a
filter. So the self-launch goes through a wall, as the data has it; the code says so at `detonateMine`.

**Unit tests** (`gradlew build` passed, `checkNoOverwatchAssets` included): `JunkratMathTest.rootHoldsABaby` (0.115 with a base
share), `tireOverdue`, `SoftLandingTest.aSelfPushOnlyWhenItLeavesThemRising`.

**Not tried:** the OS mouse and keys; Jump Boost against the root (code-read: `getJumpPower` adds it past the attribute; no second
player to trap); a Stalwart in the trap; a tire actually driven beyond the simulation distance (code-read and unit test only);
Concussive Blast while falling (same rule, not played); the tire cast in water or onto a slope. F4's Nether had no ground
loaded at y 120 for a few seconds and he fell through unloaded chunks before the server's tp (the client's loading, not the tire).
