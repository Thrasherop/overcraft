# Lúcio's wall ride, measured (the owner's Workshop recording, 2026-10-28)

The owner recorded Lúcio with `tools/workshop/lucio_wallride_recorder.txt` on Ilios Ruins: 601 s, 37,572 ticks at
62.5 a second, one player (S0), 151 rides, 19 marks. The log is kept outside the repo
(`E:\Overwatch Archive\research\lucio\workshop\Log-2026-10-28-22-22-46.txt`). Read with `tools/workshop/analysis/`
(`parse_log.py` first, then `rides.py`, `fit_wallride.py`, `ride_checks.py`, `ride_trace.py`).

What each recording was (the owner's list, numbered by mark):
1 a full run of Ruins on Speed Boost; 2 and 4 pretend fights (4 ends riding while facing backwards); 3 free riding;
5-7 three plain rides; 8-10 jumping at a wall from a standstill, a run and full Speed Boost; 11-13 riding looking
up, down and along; 14 a sanity check; 15 jumping off mid-wall; 16 dropping off with crouch; 17 riding inside a wall;
18 riding outside a wall; 19 a ride, a jump off and the ultimate; 20 Speed Boost on the ground.

## Speeds
| What | Measured | Note |
|---|---|---|
| Ground, Speed Boost | 6.87 m/s (median of 3,392 ticks) | 5.5 x 1.25 |
| Ground, Healing Boost | 5.50 | |
| Wall, Speed Boost | 8.53 | 5.5 x (1 + 0.25 + 0.30) |
| Wall, amped | 10.45 (rides 86, 99) | 5.5 x (1 + 0.6 + 0.3) |
| Up to wall speed | 6.87 -> 8.53 in 4 ticks: about 30 m/s² | |
| Square on (stall) | about 0.4 m/s until 0.3 s, then wall speed in about 0.13 s (ride 79: about 80 m/s²) | the way: the keys and camera then |
| Faster than wall speed | held for about 0.3 s, then down about 20 m/s² to wall speed (rides 59, 125, 127) | linear, not a decay |
| Corners | 90° turns and a 180° wrap (ride 141) at full speed | |

## Height on the wall (rides over 1.2 s, 7 of them, and every ride's first ticks)
- On the wall there's no gravity. For the first 0.304 s (19 ticks) he rises at **2.72 m/s**; arriving rising faster
  (a jump: 5.72), gravity (17.5) slows him to it first.
- From 0.304 to 0.43 s: **3.00 m/s**.
- Then it fades at **2 a second**: 2.6 at 0.5 s, 1.4 at 0.8, 0.95 at 1.0, 0.63 at 1.2, about 0.1 at 2 s.
- In all **about 2.7 m** (2.79 after 3 s, 2.97 on a 6.3 s ride). **Pitch makes no difference** (rides looking up 48°
  and down 41° rose the same).

## Leaving
- **Letting go of jump** (the launch tick, one tick after the release): 6.90 m/s up plus 2.5 times the look's
  vertical (pitch -0.29 gives 6.16, +0.48 gives 8.10), and the wall speed plus 2.5 along the look (looking back takes
  it away: ride 134). Looking level, the top is **1.41 m** above the launch. The 2.5 m/s goes along the look with
  forward held, along the keys with another key (ride 101, S: out from the wall), and along his motion with none,
  rise included (ride 135: half of it up).
- **Crouch, or the wall's end** with jump held: speed and upward speed kept, gravity from there.
- **The late jump**: letting go up to 0.21 s after the wall's end still wall-jumps; 0.24 s or later doesn't.
- **Between rides**: never under 0.52 s (the 0.5 s cooldown).

## Getting on
- Jump held on every attach.
- Wall distance at chest height on the attach tick (eight rays, 45° apart: they read up to 8% long): median 0.84 m from
  his middle, 90th percentile 1.06, so a wall within about 1 m. Riding, 0.5-0.6 m.
- The way: his run along the wall (43 of 49 rides of 0.3 s or more), else the keys and camera at the ride proper.

## The way is kept
Looking round doesn't turn him (ride 119 looked back the whole ride at 8.53). S doesn't brake: ride 78 held S facing
backwards and rode on at 8.53; ride 79 came square on and S sent him off backwards at full speed when the ride started.
To turn round he jumps off and back on (the owner).

## Not in this recording
The Healing Boost wall speed (7.15, from the formula); S held while facing the ride's way; a wall that ends with no
face round it in the open (Ruins' wall ends all had one or a drop).
