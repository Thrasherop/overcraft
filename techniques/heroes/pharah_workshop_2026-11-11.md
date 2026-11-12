# Pharah in the Workshop recording (2026-11-11)

The owner's Workshop recording of Pharah, read tick by tick against Overcraft's Pharah. Up: [Pharah](pharah.md) · the
method: [workshop_recordings](../game-data/workshop_recordings.md) · beside it: [Juno](juno.md) (her hover eases the same way).

## The logs and how they were read
- `local-assets/raw/workshop/Log-2026-11-11-23-46-52.txt` (gitignored, never committed): 54 s, Pharah from t 7.70 to 54.58
  (2930 ticks at 62.5 a second, times printed to 0.01 s, so a tick is 0.016 s and the rates below step by rows). No MARKs: the
  segments are the log's own events. `Log-2026-11-11-23-37-28.txt`: its last 5 s are Pharah (one Jet Dash from a run).
- **An AI bot (Hanzo) is in the same game, also tagged S0** (the owner's information; the recorder numbers slots per team).
  His T / P / B / A / K / C / X lines and his EVENTs interleave with hers over the same times (his rows: 250 health). Every
  number here is from her rows only: ticks by the hero on the T line, events by the T block they follow
  (`pharah_events.py`). The first look at the raw event list had mixed his in (an "ABILITY1 lasted 43.47" was his).
- **The K and C resource meters read 0 all through** (the Workshop's Ability Resource doesn't hold her fuel): no fuel meter.
  Fuel is read from where her rise stops with Jump still held (the push gone, the +30 % speed with it).
- The Workshop names: Shift = ABILITY1 = Jump Jet; E = ABILITY2 = Concussive Blast; Jet Dash is the right button (`fire2`,
  no ability slot: no in-use flag and no cooldown in the log); Q = ULT.
- Tools (all `py -I`, in `tools/workshop/analysis/`): `parse_log.py <log> --csv <dir> --sqlite`, then
  `pharah_trace.py <ticks.csv> [t0 t1] [--every N]` (her ticks as a table), `pharah_events.py <log>` (her events),
  `pharah_measure.py <ticks.csv> rates <row0> <row1> | hover | hoverfit | gravity | walk | fuel | fit`.

What she did (her events): Jet Dash standing (9.06, 29.79), short hovers, Concussive at her feet (11.50, 23.06, 32.43) and in
the air (43.73), Jump Jet in the air with Jump held (13.04), hovers to empty (to 17.5), rocket jumps (20.83, 35.10) the second
straight into Jump Jet (35.12), a jump held 4.7 s to empty and the 8 m/s fall to the ground (24.86-29.61), hovers, Jet Dash
strafing (46.13), reload key (47.07), Barrage cast falling in the air with Jump held (48.97-51.47), then Crouch and Jump
during it.

## Disagreements (log against ours)

| What | The log | Ours (PharahStats / code) and its source | Change |
|---|---|---|---|
| **Hover rise** | settles at **4.66 m/s** exactly (rows at 26.1-27.0: 4.66 for 1 s; every release 4.6-4.7) | `HOVER_RISE` 2.9 (D 0B99, M meaning); W 4.66 | `HOVER_RISE` 2.9 -> **4.66** (W agrees; the data's 2.9 isn't the rise) |
| **Hover's shape** | an ease toward 4.66: dv/dt = **6.0 x (4.66 - vy)** (gravity included), k 5.9-6.0 from vy -19.6 to +5 (`hoverfit`); above 4.66 it never pulls down harder than gravity (Jump held at 14-17 m/s in a jet: -17.5 exactly) | a push of 50 m/s² (55 falling) that stops at the limit (D 0B9A / #650, M) | a new `HOVER_EASE` = **6.0** /s: vy = 4.66 + (vy - 4.66) x e^(-6 dt), not below the gravity step; `HOVER_PUSH` / `HOVER_PUSH_FALLING` retired |
| **A jet turns the hover on without the key** | **no**: Jump Jet without Jump held falls at -17.5 to its end, even at 3.2 m/s (rows 346-388); Jet Dash without it, apex 1.72 m at 0.41 s then a plain fall | `JET_PUSH` 80 / `JET_PUSH_EMPTY` 30, "the hover on without the key" (D 0025 #698 / #694 / #29, H/M) | `ClientPharah.hover`: a jet gives the hover only with Jump held (then the normal ease, no fuel spent); `JET_PUSH`, `JET_PUSH_EMPTY` unused. Our dash's 16 m and the jet's 8.9 m came from this |
| **Hover from a ground press** | the press jumps (5.72 shown), **then 13 ticks (0.21 s) of plain gravity in the air**, then the hover (24.86 -> 25.07, 44.94, 47.63); no fuel spent meanwhile | `HOVER_FROM_GROUND` 0.2 (D #22, M) counted only while on the ground: airborne the next tick, ours hovers at once | count the 0.2 s from the press, wherever she is; the constant stays |
| **Barrage in the air** | **she keeps her height**: her speed zeroed at the cast (vy -4.81 -> 0), then no gravity at all: a flight at **\|v\| = 4.40 m/s** exactly (5.5 x 0.8) in 3D, Jump up, Crouch down (Crouch alone, keys held: 45° down, -3.11; Jump and Crouch: level; Jump alone with keys: about 20° up, +1.5) | she falls through it unless Jump is held (the hover, fuel held full) | Barrage: zero her speed at the cast, no gravity, a 3D move at the slowed run speed with Jump / Crouch as up / down (the up share with no move key isn't in the log) |
| **Concussive Blast's cooldown** | "in use" **0.66 s** (= cast 0.167 + recovery 0.5), the 8 s from its end: 8.66 s press to ready (cd 7.99 first shows at 44.38 for the 43.73 press; 6.19 left at 34.90 for 32.43) | set at the release, 0.167 s in (D 092B 8 s; BZ 2026-08-11) | start `CONCUSSIVE_COOLDOWN` at `CONCUSSIVE_CAST + CONCUSSIVE_RECOVERY` (or set it to 8 + 0.5 at the release) |
| **Jet Dash keeps some speed** | velocity = 19 along the keys + **0.3 x the horizontal velocity before** (from a 5.5 run: 20.65, residual 1.59 / 0.44 of 5.38 / 1.13; strafing from 6.1: residual (-1.35, 1.21) of (-4.34, 4.28)); vy set to 8 | "in place of what she was doing" (D 4B92 #36 / #545) | a new `JET_DASH_KEEP` = **0.3** (ours, from this log: look for the data field, as Jump Jet's x57643451 0.1) |
| **Fuel refill** | doesn't fit 45/s on the ground after 0.3 s: from 42.05 she hovered 0.65 + 0.39 + 0.62 + 0.37 = 2.03 s (100 fuel) to empty at 48.20, with only 0.24 s stints on the ground between, so she was full at 42.05 after 0.95 s on the ground; ours predicts 59 there. Best fits (`fit`): **100/s on the ground after 0.3 s**, or 35/s in the air too after 1.25 s | `FUEL_REGEN` 45 / `FUEL_REGEN_DELAY` 0.3, ground only (D 1E4F / 1E52, H/M; W) | **not yet**: one window decides it and the overfuel rules (M) feed it. Record a test: empty the tank, land, wait 0.5 s, take off and hover to empty; again at 1 s and 2 s |
| Empty-tank fall | the push fades over **0.35 s** (decel 0 -> 17.5 from 27.08 to 27.43, the data's #655 ramp), then a plain fall to about -3, then an ease onto **-8.0** (k about 3.5 /s, never faster than gravity) | a 7.5 m/s² push (net -10) to an 8 m/s cap (D #655 / 1E54) | small: 0 -> -7.5 m/s takes 0.74 s in the log, 0.75 s ours. Optional: the same ease as the hover's toward -8 at 3.5 /s |
| Rising gravity (every hero) | **17.5** going up as well as down (Pharah 226 ticks, Hanzo, D.Va: all 17.5) | `OwMovement.GRAVITY_UP` 16.7 (tuned so 20 Hz jump tops match) | not Pharah's to change; with 16.7 her jets rise higher than Overwatch's (17 m/s: 8.65 m against 8.4). Check with the other heroes' logs |
| Ground stop (every hero) | 5.5 -> 0 at a constant -1.32 a tick (82.7 m/s²) to the last 0.22 | `FRICTION` 15 x max(v, `STOP_SPEED` 4.5): 82.5 at 5.5, 67.5 under 4.5 | cross-hero: `STOP_SPEED` 5.5 would match (a 0.01 s difference) |

## Agreements

| What | The log | Ours and its source |
|---|---|---|
| Jet Dash | **19.00** m/s along the keys, forward (her facing) without; **8.00** up, set (a fall cancelled: -1.54 -> 8) | 19 / 8 (D 1E29 / 1E2A / 1E2B) |
| Jet Dash's distance and rise | 10.2 m in 0.85 s; apex **+1.72 m at 0.41 s** (8²/35 is 1.83: the first 0.2 s decelerate at about 20 m/s², unexplained); after it, plain air: speed over 5.5 decays at 1.9-2.0 /s | `AIR_DECAY` 2.0 above 5.5 (D mover) agrees; the 0.85 s state (D v9110, M) has no visible effect on her motion |
| Jump Jet | a **press**: launches the press tick at full strength, Shift held 0.24 / 0.30 s; in use **0.85 s**; vy = **17 + 0.1 x the rise she had** (4.33 -> 17.47; 11.44 -> 18.14); **+7** m/s added along the keys (6.97) | `JUMP_JET_UP` 17, the tenth kept (D x57643451), `JUMP_JET_ALONG` 7, `JET_TIME` 0.85 (D 0027) |
| Jump Jet's height | from a rise of 4.6 m/s: +8.85 m; from standing it'd be about 8.4 (17 m/s, 17.5 a tick-step) | ours 8.5-8.9 (the jet's push and the 16.7 gravity: above) |
| Cooldowns | Jump Jet **13.99** from the jet's end; Concussive **7.99** (from its end: above) | `JUMP_JET_COOLDOWN` 14 from the end (D 092C); `CONCUSSIVE_COOLDOWN` 8 (D 092B) |
| Concussive on herself | **20.0** m/s level away + **5.00** up, added to her own speed (5.5 run -> 25.5); no damage | `CONCUSSIVE_AWAY` 20 / `CONCUSSIVE_UP` 5, all of it, no damage (D) |
| Concussive's release | her launch 0.21-0.24 s after the press (at her feet) | `CONCUSSIVE_CAST` 0.167 (+ the flight) |
| Rocket jump | **12.00** m/s up, **20** damage to herself (225 -> 205) | ours measured 12 m/s, 20 (D 1D9C x 0B69, 0B68) |
| Hover drain | from full, 25.07 -> 27.08: **2.0 s** (50 a second) | `FUEL_DRAIN` 50 (D 1A6A) |
| Hover's horizontal speed | **7.15** with W (5.5 x 1.3), **6.44** backwards (4.95 x 1.3); back to the normal air speed when the tank empties | `HOVER_SPEED` 0.3 (BZ 2026-04-14) |
| Empty fall cap | **-7.99** settled, Jump held | `EMPTY_FALL` 8 (D 1E54) |
| Barrage | **2.50 s**; **4.40** m/s (x0.8); the hover's rise back at once after it (her fuel kept) | `BARRAGE_TIME` 2.5 (D 1A5D), `BARRAGE_SLOW` 0.2, fuel held full (D #634) |
| Walk | 5.5 m/s; from standing 3.08, 4.84, 5.50 (full in 3 ticks, 0.05 s) | `RUN_SPEED` 5.5, `GROUND_ACCELERATE` 35 |
| Falling gravity | 17.5, easing: -16.2 to -16.9 between -6 and -8 m/s | `GRAVITY_DOWN` 17.5 with the tanh ease (16.4 at -7.5) |
| Health, regeneration | 225; 6 s after damage, back at 21 a second (10 + 5 % of 225) | `HEALTH` 225; the global regeneration (W) |

## What the log can't answer
- **Does Concussive Blast cancel a reload?** No ammo channel (`res_fire1` is 0), and E was never pressed during a reload (the
  only reload key press is at 47.07, then Barrage refilled her). Still open: a recording firing 1-2 rockets, R, then E at
  once, then holding fire (count the rockets) answers it.
- **Overfuel's hold and decay** (`OVERFUEL_HOLD` 5, `OVERFUEL_DECAY` 30, `OVERFUEL_DECAY_HOVER` 80, M; W says the overfill lasts
  6 s): no meter, and no hover started 5 s after a jet. The refill question above tangles with it.
- **Jet Dash's cooldown** (9 s) and state: it's the right button, which the Workshop doesn't time.
- **Whether a jet spends fuel with Jump held:** the fit prefers "no" (ours), weakly.
- **Barrage with no key held** (she always held a move key, Jump or Crouch): whether she just floats still is likely but
  not shown.
- Jump Jet's fall cancel (a jet from a fall: both jets here started rising).

## Lessons for the recorder
- A bot is logged under the same slot tag as the player: split by the T line's hero, events by the block they sit in.
- The resource meters are 0 for Pharah: a fuel reading needs the script to log something else (none in the Workshop, so far).
- Each ability's "in use" length is where its cooldown starts: the Workshop's flag spans cast and recovery (Concussive 0.66).
