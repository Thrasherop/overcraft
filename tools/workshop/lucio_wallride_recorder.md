# Lúcio wall-ride recorder (Overwatch Workshop)

Goes with `lucio_wallride_recorder.txt`. It records Lúcio's movement in a custom game, tick by tick, so Overcraft's Lúcio
can be matched to the real one. It's a Workshop script: it only reads what any Workshop script can (positions, speeds,
buttons). Nothing touches Overwatch's files or memory.

## Set it up
1. Open `lucio_wallride_recorder.txt` and copy all of it.
2. In Overwatch: **Play → Custom Game → Create → Settings**, then the **paste** button (the clipboard icon at the top
   right of the settings screen). It sets Skirmish, no hero limit, instant respawns, and forces Lúcio.
   If Overwatch says a line doesn't parse, tell me which: the script hasn't been run yet.
3. **Options → Gameplay → General: turn on Workshop Inspector Log File** (if your build has it). Everything the script
   logs then goes to a text file in `Documents\Overwatch\Workshop\`.
4. Start the game on a map with long walls (Ilios Ruins, Lijiang Night Market, Oasis City Center).

## Playing together
- Everyone in the game is forced to Lúcio and logged. Each line starts with the player's slot (**S0**, **S1**...), shown
  at the top left of each player's readout: **note who was which slot**.
- **You host** (with the log file option on); others join. The log file is written on the host's machine.
- Keep sessions to a few minutes: with two players it's about 500 lines a second.

## What to play
Each a few times. Press **Interact (F)** right after one you want me to look at: it logs a numbered MARK and shows it on
screen. Note what each MARK was.
- A plain ride: jump at a wall, hold Jump, ride along, let go.
- Jump at a wall from a standstill, at a run, at full Speed Boost.
- Riding while looking up, looking down, looking along the wall.
- Jumping off the end, jumping off mid-wall (tap Jump), dropping off with Crouch.
- A corner: ride into an inside corner, round an outside corner.
- A chain: wall to wall across a gap, wall to floor to wall.
- A one-block-high ledge under a wall (what Minecraft's blocks make everywhere).
- **Note which song you started on** (the Workshop can't read it; Crossfade presses are counted).

## Send back
- The newest file from `Documents\Overwatch\Workshop\`.
- What each MARK was, with its S number, and the song each player started on.
- Tell me when Overwatch is closed: nothing of mine runs or reads anything while it's open.
- No log file option? Screenshots of the left-side readout during a ride, or a short screen recording of it.

## What it records (each player, every tick: 62.5 a second)
- `S0 T`: time; on wall, on ground, in air.
- `S0 P`: position, velocity, the movement keys as a vector.
- `S0 B`: Jump and Crouch held; facing (the look direction, pitch included); the nearest wall's normal and distance
  (eight level rays round you at chest height, 1.5 m; distance 9 means no wall).
- `S0 A`: primary and secondary fire held, crouching; Crossfade (Ability 1), Amp It Up (Ability 2) and Sound Barrier in
  use; how many times Crossfade was pressed.
- Bookmarks: `EVENT ATTACH` / `LEAVE` (each ride's start and end, with speeds and the height gained), `LAND`, `MARK`,
  `CROSSFADE`, `AMP`, `ULT`.

The on-screen readout shows the same live: slot, time, wall / ground, speeds, height, velocity, input, jump, pitch, the
wall's normal and distance, rides, marks, Crossfades, Amp, ult and the last event.
