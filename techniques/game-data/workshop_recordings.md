# Workshop recordings: measuring what no source has

How a hero's movement gets measured in the real game, when neither the data nor any public source has the numbers.
Up: [Techniques](../README.md) · beside it: [reading_game_data](reading_game_data.md) · first used for:
[lucio](../heroes/lucio.md) · the result: `tools/reference/lucio-wallride-log.md`

## Why
The data's graphs name values but not always what they do (Lúcio's wall-ride graph 0263 holds no vertical speed), and
the wiki measures a few headline numbers. A Workshop script runs inside a custom game and can read any player's
position, velocity, buttons and facing every tick. It's allowed: it touches nothing but the game's own scripting
(CLAUDE.md: Overwatch must be closed only while anything of ours reads its files or runs the mod). One owner session
(2026-10-28, 151 Lúcio rides) overturned four of our guesses and confirmed six numbers.

## Writing the script (`tools/workshop/`)
- **Log every tick, one line per subject:** time and state; position, velocity, input; buttons, facing, the nearest
  wall; abilities. Tag each line with the player's slot, so a friend can play in the same game.
- **Bookmarks for the moments that matter,** in rules without waits (a wait makes a fast second event miss its own
  bookmark): on and off a wall, landing, abilities, and a MARK on Interact that the player presses after each try.
- **Ask for a list of what each mark was.** The owner's numbered list turned 600 s of log into 20 labelled experiments.
- **The readout's last-event line is not live.** A value shown in it (the jump held at the last wall leave) reads as
  stuck; say which fields are live in the instructions.
- **The log file:** Options, Gameplay, General: both Workshop Inspector and Workshop Inspector Log File, before
  creating the game. `Documents\Overwatch\Workshop\Log-<date>-<time>.txt` on the host's PC, written as it plays.

## Reading it (`tools/workshop/analysis/`)
- `parse_log.py` turns the log into ticks and events split by mark (regexes only: the log is data; run with -I).
- Then tables, not impressions: `rides.py` (a row per ride), `fit_wallride.py` (curves averaged across rides),
  `ride_checks.py` (rules), `ride_trace.py` (one ride tick by tick). Read the outliers' traces before trusting a fit:
  curved walls and bumps make dips that aren't rules.
- **Mind the tick convention.** The log's velocity is after that tick's gravity for a ground jump (5.72 shows for a
  6.0 impulse) but not for a wall jump's launch tick (6.90 shows). Compare tops of arcs, not first-tick speeds, then
  set our 20 Hz numbers to give the same top.
- **Check our own movement the same way:** a course in the dev world and `wallride_tests.py`-style scenarios printing
  the same numbers. A leave step that moved a whole tick without gravity was found only because the jump's top came
  out at 1.77 m against the recording's 1.41.
