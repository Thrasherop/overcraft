# Jetpack Cat flight recorder (Overwatch Workshop)

Goes with `jetpack_cat_flight_recorder.txt`. It records Jetpack Cat's flight in a custom game, tick by tick, so
Overcraft's can be dialled in: her data has her mover's numbers, but not how the engine flies them (her up and down
speed, Frenetic Flight's real top speed, how she slows). It's a Workshop script: it only reads what any Workshop script
can (positions, speeds, buttons). Nothing touches Overwatch's files or memory.

## Set it up
1. Open `jetpack_cat_flight_recorder.txt` and copy all of it.
2. In Overwatch: **Play → Custom Game → Create → Settings**, then the **paste** button (the clipboard icon at the top
   right of the settings screen). It sets Skirmish, no hero limit, instant respawns, and forces Jetpack Cat.
   If Overwatch says a line doesn't parse, tell me which: the script hasn't been run yet.
3. **Before creating the game: Options → Gameplay → General**, in the Workshop part of the list, turn on **both**
   **Workshop Inspector** and **Workshop Inspector Log File** (the log file stays empty with only one of them on).
   The host's game then writes everything the script logs to `Documents\Overwatch\Workshop\Log-<date>-<time>.txt`,
   live as you play. Moving to spectator and back starts a new file.
   To check it's logging: open the Workshop Inspector in the game (the same settings list has a button for it).
   Lines starting S0, S1... should be scrolling there. If it's empty, the script isn't running: say so.
4. Start the game on a map with open space (and somewhere flat to land).

## Playing together
- Everyone in the game is forced to Jetpack Cat and logged. Each line starts with the player's slot (**S0**, **S1**...),
  shown at the top left of each player's readout: **note who was which slot**.
- **You host** (with the log file option on); others join. The log file is written on the host's machine.
- A second player is useful for Lifeline (towing an ally) and Catnapper.

## What to fly
Each a few times. Press **Interact (F)** right after one you want me to look at: it logs a numbered MARK and shows it on
screen. Note what each MARK was.
- Hover still for 2 s, then hold W for 2 s and let go (her top speed, how fast she gets there, how fast she stops).
  The same with S, A and D, and W while looking 45° up and 45° down.
- Hold Space for 2 s, then Ctrl for 2 s, from a hover (her up and down speed); then Space + W together.
- Frenetic Flight (right button) from a hover, held until the energy runs out; held 0.3 s; held 1 s then let go with W
  still held, and with nothing held (the speed after the boost).
- A turn: full speed one way, then hold the opposite key (how she brakes and turns).
- Lifeline on an ally, and Catnapper, once each (with a second player).

## Send back
- The newest `Log-....txt` files from `Documents\Overwatch\Workshop\` on the host's PC (all of them from the
  session, if spectating split it). If your Documents folder is synced by OneDrive, the folder is
  `OneDrive\Documents\Overwatch\Workshop\` instead. Not in either? With Overwatch closed, this lists the newest
  Workshop logs anywhere in your user folder (PowerShell):
  `Get-ChildItem $env:USERPROFILE -Recurse -Filter 'Log-*.txt' -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 5 FullName, LastWriteTime`
- What each MARK was, with its S number.
- Tell me when Overwatch is closed: nothing of mine runs or reads anything while it's open.
- No log file option? Screenshots of the left-side readout, or a short screen recording of it.

## What it records (each player, every tick: 62.5 a second)
- `S0 T`: time; on ground, in air; height above the ground.
- `S0 P`: position, velocity, the movement keys as a vector.
- `S0 B`: Jump and Crouch held; facing (the look direction, pitch included).
- `S0 A`: primary and secondary (Frenetic Flight) held, the boost's energy (if it reads 0 all the time, say so), and
  Lifeline, Purr and Catnapper in use.
- Bookmarks: `EVENT BOOST` / `BOOST END` (how long it was held, the distance, the energy left), `LIFELINE PRESS`, `ULT`,
  `LAND`, `MARK`.

The on-screen readout shows the same live: slot, time, ground / air, speeds, velocity, height, input, pitch, jump,
crouch, boost and energy, Lifeline, ult and marks.
