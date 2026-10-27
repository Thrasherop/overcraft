# Lúcio in live Overwatch: numbers and sources (research, 2026-10-26)

For #37. Raw sources were saved in the session's scratchpad (`lucio\SOURCE_EXCERPTS.txt` has every quote with its
URL). **BZ** = Blizzard patch notes (https://overwatch.blizzard.com/en-us/news/patch-notes/live/YYYY/MM/), **HP** =
Blizzard's hero page, **W** = the wiki through its MediaWiki API (Lúcio page revision of 2026-10-06; wiki.gg 401).
Stadium, April Fools and limited-time modes are left out. Game data (DataTool heroes.txt) agrees on the kit and perks.

## Base
- Health 225, no armor or shields (BZ 2024-08-20). Support, sub-role Tactician (BZ 2026-02-10). 5.5 m/s.
- Head hit volume 13.2 -> 16 (BZ 2026-10-06). Lower ground friction than others, no number (W).

## Sonic Amplifier
- 22 per projectile (BZ 2026-07-14); 4 per volley, 0.125 s apart; a volley per 0.935 s (W).
- 20 ammo; reload 1.5 s, ammo back at 1.088 s (W). 50 m/s (W). Radius 0.22 m (0.10 + 0.12 global; BZ 2024-02-21,
  2024-12-10; W). No falloff, no spread, headshot 2x (W). Knockback exists, no number (BZ 2024-07-12: -10%).

## Soundwave
- 45 damage (BZ 2024-04-16), 5 s cooldown (BZ 2026-01-08), 7 m arc in front (W), knockback 17 m/s (13.73
  horizontal, 10.15 vertical; W), targets locked out of movement 0.45 s (BZ 2024-02-13), follows his aim up and
  down, no ammo, usable while reloading, 0.56 s before primary fire after it (W).

## Crossfade
- Healing Boost: 20 HP/s to allies (BZ 2025-06-24), 12 HP/s to himself (W; BZ 2026-05-12's "self-healing penalty is
  now correctly at 60%" would mean 8: conflict, W's 12 used). Speed Boost +25% (BZ 2019-10-15). Radius 12 m (BZ
  2018-08-09), needs line of sight, lingers 1 s after leaving (W). Swap: 0.4 s before the song changes, no cooldown.

## Amp It Up
- 3 s, 12 s cooldown after it ends, 0.69 s recovery, 12 m (W). Healing 56 HP/s (W, 2.8x base), speed +60% (BZ
  2019-10-15), no self-heal penalty while amped (BZ 2023-08-10).

## Wall Ride (W unless a BZ date is given)
- Jump at a wall and hold Jump to ride, for as long as it's held; can start backwards in mid-air (an option, on by
  default). Options: Toggle wall ride (off), "Wall ride cares about movement" (off), Wall Jump On Release (on).
- +30% speed on the wall (7.15 m/s; BZ 2019-10-15), ignores the speed cap and backpedal penalty. A 0.5 s cooldown.
- Letting go jumps off (with Wall Jump On Release); crouch drops off without a jump. Wall jump: +2.5 m/s
  horizontal; vertical 6.54 m/s looking ahead, 6.9 holding forward, 4.12 looking down, 9.12 looking up.
- Rides round inside and outside corners; can land back on the same wall once far enough away (BZ 2018-05-03).
- Air speed above 5.5 doesn't decay below double his ground speed (11 m/s healing song, 13.76 speed song, 17.6 amped).

## Sound Barrier
- 2900 points (W; BZ 2025-07-22 +9%). 625 overhealth (BZ 2026-05-12) for 6 s (BZ 2026-10-06), decaying from the
  start, about 104/s (W). 30 m radius. Cast 0.656 s + 0.832 s recovery, a small leap, completes on landing, 240%
  gravity while casting (BZ 2024-12-10), allies arriving within about 0.83 s still get it. Lúcio gets it too.

## Role passives
- Support role passive removed (BZ 2026-02-10); global regeneration after 6 s stays (BZ 2025-06-24).
- Tactician (BZ 2026-02-10): excess ult charge kept after the ult, up to +25%; -25% charge rate above 100%.

## Perks (HP; minor at level 2, major at level 3; W costs 2020 / 5050)
- Minor, Soundwave Rider: "Wall Riding empowers your next Soundwave, increasing its knockback by 25% and damage by
  50%." (BZ 2025-08-26; W: about 5 s of riding fills it.)
- Minor, Beat Drop: "Amp It Up is now active for 4 seconds after casting Sound Barrier." (BZ 2026-10-06)
- Major, Noise Violation: "Crossfade's range is increased by 150% while Amp It Up is active." (BZ 2025-02-18)
- Major, Accelerando: "Gain 15% attack speed while Lucio's Speed Song is active, tripled during Amp It Up." (BZ
  2026-04-14)

## Not given by any source (ours when used)
Projectile knockback; Soundwave's cone angle; Sound Barrier's line of sight, leap and exact centre; wall ride's
surfaces, steepness, attach range, drift, what the 0.5 s cooldown covers, corner timing; his friction; Beat Drop's
details.
