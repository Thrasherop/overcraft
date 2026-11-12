# D.Va in the owner's Workshop recording (2026-11-11)

What the owner's movement-recorder log holds about D.Va, measured and set against Overcraft's numbers. Up:
[D.Va's hub](dva.md) · the method: [workshop_recordings](../game-data/workshop_recordings.md) · the map: [Techniques](../README.md)

The log: `local-assets/raw/workshop/Log-2026-11-11-23-37-28.txt` (gitignored owner data, never committed). The scripts:
`tools/workshop/analysis/split_subjects.py` (new), `parse_log.py`, `dva.py` (new). Read-only analysis: no game code changed.

## Reproduce
```
py -I tools/workshop/analysis/split_subjects.py <log> <out> "Hanzo" "D.Va,Pharah"
py -I tools/workshop/analysis/parse_log.py <out>/S1.txt --csv <out>/p1 --sqlite      (and S0 -> p0 for the bot)
py -I tools/workshop/analysis/dva.py <out>/p1 --health
```

## First: two players in one slot tag
The log holds **two players, both tagged S0**: the owner (D.Va from t 7.74 to 157.77, then Pharah to the end) and the
game's AI bot (Hanzo, the whole 162 s; slot 0 of the other team). Their lines interleave every tick. Read by slot, they
look like "Hanzo for 78 s, then D.Va": they aren't. `split_subjects.py` gives each T block (T, P, B, A, K, C, X) to the
player its T line's hero names, and each EVENT line to the player whose same-tick P line has that velocity / position
(354 of 361 matched; the 7 left are vel-0 events, kept with the block before). So D.Va has **150 s** (9373 ticks), not 75.
The bot turned out useful: his health line shows what her abilities did to him.

No MARK events; segments are the log's own (ABILITY1/2, ULT, TAKEOFF/LAND, JUMP). In 150 s: 15 Boosters, 3 volleys of
Micro Missiles, 10 Defense Matrix uses, 4 Self-Destructs (all in the air, three during Boosters), 3 Call Mechs, ~30 jumps.

## Measured vs ours: where they disagree
| What | The log | Ours (source) | Verdict |
|---|---|---|---|
| Boosters' knockback **direction** | 17.0 m/s level + 4.0 up, pointing **from D.Va to the target** (40.14: push (-0.912, -0.41), the bot's offset from her (-0.912, -0.41), her flight (-0.98, -0.19); 33.66 and 78.85 the same, 2-8 deg closer to the offset than to the flight) | 17 + 4 along her flat look (`Boosters.hits`, D 03AC / 03AD) | **Differs**: the size agrees, the direction doesn't. A fourth hit (106.25) pushed only 5.7 level (the bot mid-move? unexplained) |
| The eject's launch | the pilot **keeps the mech's velocity** (the first tick after differs from the mech's by 0.3-0.9 m/s) **plus** 6.0 back (5.70 / 6.07 / 6.15 / 6.05 along her flat facing, sideways under 0.75) and 7.00 up, all four ejects | velocity **set** to 6 back, 7 up (`Dva.eject`, D 026A #97) | **Differs** in how: the numbers agree, ours drops the mech's momentum (after a boosted Self-Destruct she should fly on at up to 12 m/s) |
| Mech max health | 525 (200 + 325), the pilot 175 | 675 (200 + 150 tank bonus + 325) | **Not a disagreement to act on**: this custom game (Skirmish, no role limits) didn't give the tank bonus; the bot Hanzo had his base 250. Keep 675 (BZ 2026-02-10) |
| Ground stop and start (every hero) | stop: 5.50 -> 4.18 -> 2.86 -> 1.54 -> 0.22 -> 0, a flat 1.32 a tick = friction 15 on a **5.5** floor; start: 0 -> 3.08 -> 4.84 -> 5.50, which the same 5.5 floor gives exactly (a 4.5 floor gives 3.08 -> 5.08). The AI Hanzo stops the same way | `OwMovement.STOP_SPEED` 4.5 (D mover m_E91D1664) | **Differs** slightly (a stop in 0.067 s, not 0.083). Lúcio's log found his floor at his run speed too |

## Measured vs ours: where they agree
| What | The log | Ours (source) |
|---|---|---|
| Mech walk | 5.50 forward / sideways / diagonal; 4.95 back (x0.9), 5.13 diagonal back (x0.933) | `RUN_SPEED` 5.5, `BACK_FACTOR` 0.9, `BACK_DIAGONAL_FACTOR` 14/15 |
| Pilot walk | 5.50 | 5.5 |
| Fusion Cannons' slow | 3.85 forward (x0.70), 3.46 back (0.9 x 0.7) | `FUSION_SLOW` 0.3 |
| No Fusion slow while boosting | 249 ticks firing at 12.0 m/s | ours: no slow while boosting |
| Light Gun, Defense Matrix, Micro Missiles: no move slow | 5.50 (pilot firing); 5.50 / 4.95 / 5.13 by direction with the matrix up; 5.50 / 5.13 with missiles | none in ours |
| Jump | first tick 5.72 up (a 6.0 impulse after a tick of 17.5), top 0.98 m, 0.67 s on flat ground: mech and pilot alike | `JUMP_SPEED` 5.72, `GRAVITY_UP` 16.7: top 0.98 m, ~0.68 s |
| Gravity | 0.28 m/s a tick = 17.5 m/s² up and down; falling it eases off as 17.5 x (1 - (v/30)²): 7.8 m/s² at -22.5 m/s (a 21 m drop) | `GRAVITY_DOWN` 17.5, `TERMINAL_SPEED` 30. `GRAVITY_UP` 16.7 is lower than the log's 17.5 but pairs with the 5.72 start to give the same top: leave it (compare tops) |
| Air speed above 5.5 | decays at (speed - 5.5) x 1.94 a second, the same at 6 to 12 m/s (after each Boosters flight) | `AIR_DECAY` 2.0 |
| Boosters' speed | 11.99 m/s (3D), every flight | `BOOSTERS_SPEED` 5.5 x 2.18 = 11.99 |
| Boosters' aim | velocity = 12 x facing: vy - 12 fy median 0.00; up and down fully | along the view vector |
| Boosters' turn rate | none: velocity vs the same tick's facing 0.24 deg (median, 178 turning ticks), vs a tick earlier 4.4 deg; facing turned up to 3400 deg/s with no lag | none (no turn-rate node, D) |
| Boosters' start | 12 m/s on the first tick after the press, off the ground at once | the same |
| Boosters' flight | 2.02 s (11 uninterrupted flights); "in use" 2.29-2.31 s | `BOOSTERS_TIME` 2 |
| Ending early | a second Shift at +1.62 / +1.78 s ended the flight that tick; "in use" 0.29 s later | press ends it (from 0.4 s: untested, no press that early) |
| Its cooldown | 4.00 s, starting when "in use" ends (0.3 s after the flight) | `BOOSTERS_COOLDOWN` 4, `BOOSTERS_COOLDOWN_DELAY` 0.3 |
| Self-Destruct mid-flight | ends Boosters at once (in-use off the eject tick) | ours: the mech flies on (can't be seen: the mech isn't logged) |
| Boosters' damage | the bot took exactly **-25** once a flight (4 flights), with her 3.8-4.2 m away (feet to feet) | `BOOSTERS_DAMAGE` 25, sphere 1.5 m at 2 m ahead |
| Boosters' knockback size | 16.96 / 16.99 / 17.00 level, 4.00 up | `BOOSTERS_KNOCKBACK` 17, `_UP` 4 |
| Initiator heal | +50 over 0.96 s at Boosters' start, in 5 steps (8.8, 9.6 x 4), not at full health | `INITIATOR_HEAL` 50 over 1 s |
| Defense Matrix drain | 33.0-33.2 a second; 100 -> 0 in 3.02-3.03 s | `MATRIX_DRAIN` 33 |
| Its refill | 16.0 a second (15.9-16.0), from 0.73-0.75 s after it's down | `MATRIX_REGEN` 16, `_DELAY` 0.75 |
| Its minimum | a one-tick tap kept it up 0.48 s; taps inside that window didn't extend it | `MATRIX_MIN` 0.5 |
| Its cooldown | released 30.13, held again 30.86, came up 31.15 (1.02 s after release) by itself while held | `MATRIX_COOLDOWN` 1, starts while held |
| Matrix vs Fusion and missiles | no Fusion while it's up; it comes up and stays up during Micro Missiles | the same |
| Micro Missiles' volley | "in use" 1.46 s, all three; the last hit lands when it ends (102.91 at 16.7 m = launched at the end), so the first missile left 1.46 - 17/14 = **0.25 s** after the press | `MISSILES_CAST` 0.25, 18 at 14/s (0.25 + 17/14 = 1.46). This favours the data's 0.25 over the wiki's 0.16 (dva.md "Where the sources disagree") |
| A missile's damage | -7.50 a direct hit (2 + 5.5), smaller ones from splash alone; ~16 hits of 18 at 1-4 m, 7 at 12-16 m on the moving bot; hits ~0.07 s apart (14 a second) | `MISSILES_IMPACT` 2 + `MISSILES_SPLASH` 5.5 |
| Fusion Cannons | -22 a volley at 1.5 m (11 x 2), volleys 0.15 s apart, less with distance (3-5 at 15-17 m) | 11 x 2, `FUSION_INTERVAL` 0.15 |
| Self-Destruct's fuse | Call Mech's charge appears 2.99-3.00 s after the eject (all four), "in use" 3.31 s | `SELF_DESTRUCT_FUSE` 3 (+ `LINGER` 0.3); Call Mech's charge given at the blast |
| The eject itself | on the press's tick; pilot 175 / 175 | ours |
| The pilot after the eject | 175 the whole time, no drop at all in four ejects | 175 (the real-input pass's post-eject 2-health drop is ours alone: a bug) |
| Call Mech | cast 0.19 s after the press (also when pressed in the air), she stays put (moved 0.13-0.17 m), in the mech 1.93-1.94 s after the press, at full 525 | `CALL_CAST` 0.2, `CALL_LINK` 1.75 after the spawn = 1.95 |
| Global regeneration | from 6.1 s after the last damage, ~36 HP/s (10 + 5 % of 525 = 36.25) | `Combat.REGEN_DELAY` 6 s, 10 + 5 % |

## What the log can't answer
- **Missile paths**, the missiles' spread and the Matrix's box: projectiles aren't logged. The real-input pass's "missiles
  mostly miss a 12 m target" can only be set against the bot's damage: in the real game 7 of 18 hit him at 12-16 m while
  he moved, ~16 at 1-4 m. So a fair share of a volley does miss at 12 m in Overwatch too; a standing target missing
  most is still ours to look at.
- **Ult costs**: the ult meter reads 100 % whenever not spent (the session's fill), so 1700 / 280 can't be checked.
- **Micro Missiles' cooldown**: the Workshop's Ability Cooldown reads 0 for her E all session.
- **The Boosters minimum (0.4 s), knockback taken x0.1, Self-Destruct's blast and the Call Mech landing's damage**: not
  tried (no early press, nobody hit her mid-flight, the bot was never near a blast or a landing).
- **The mech after a mid-flight Self-Destruct**: only the pilot is logged.
- **"658 / 675 after G"**: G is ours; the log only shows the mech coming back at its full max every time.

## Changes I'd make
1. **Boosters' push from her, not along her look** (`Boosters.hits`): the flat direction from her position to the
   target, times `BOOSTERS_KNOCKBACK` 17, plus `BOOSTERS_KNOCKBACK_UP` 4. Constants unchanged.
2. **The eject adds to her velocity** (`Dva.eject`): `player.getDeltaMovement().add(back.x, Ow.speed(EJECT_UP), back.z)`
   in place of setting it (with the mech's boosted motion as her own at that tick). `EJECT_BACK` 6 / `EJECT_UP` 7 unchanged.
3. **`OwMovement.STOP_SPEED` 4.5 -> 5.5** (`RUN_SPEED`): both D.Va and the AI Hanzo stop on a 5.5 floor, Lúcio's log
   found his on his run speed. Every hero's: its own issue, played on several heroes.
4. Note in `DvaStats.MISSILES_CAST` that the Workshop log supports 0.25 (first missile 0.25 s after the press), and
   in dva.md's disagreements that the wiki's 0.16 is out.

No other constant needs changing.
