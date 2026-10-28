# Workshop scripts: watching Overwatch from inside it

Scripts for Overwatch's own Workshop (a custom game), which the owner pastes into Overwatch and plays. Nothing here
touches Overwatch's files or memory: the Workshop is a game mode, and these only read what any Workshop script can
(positions, speeds, buttons). Overcraft never runs while Overwatch is open.

## lucio_wallride_recorder.txt: Lúcio's wall ride, frame by frame

What it's for: Overcraft's Lúcio rides walls from the numbers we have, but how a ride *feels* (when it catches, how
the speed settles, what a jump off does, how corners chain) is in the transitions. This script records them.

### Setting it up
1. Copy all of `lucio_wallride_recorder.txt`. In Overwatch: Play → Custom Game → Create → Settings, then the **Paste**
   button (the clipboard icon at the top right of the settings screen). It sets Skirmish, no hero limit and instant
   respawns, and forces Lúcio. If Overwatch says a line doesn't parse, tell me which: the script hasn't been run yet.
2. Options → Gameplay: turn on **Workshop Inspector Log File** if your build has it (I believe it's under General).
   Overwatch then writes everything the script logs to a text file in `Documents\Overwatch\Workshop\`. Without it,
   the log is still in the custom game's Workshop Inspector, and the readout is on screen.
3. Start the game and pick a map with long walls (Ilios Ruins, Lijiang Night Market, Oasis City Center, Watchpoint
   practice range walls).

### What to play
Each a few times, pressing **Interact (F)** right after one you want me to look at (it logs a numbered MARK and
shows it on screen; tell me what MARK 3 was):
- A plain ride: jump at a wall, hold Jump, ride along, let go.
- Jump at a wall from a standstill, at a run, at full Speed Boost.
- Riding: looking up, looking down, along the wall.
- Jumping off the end, jumping off mid-wall (tap Jump), dropping off with Crouch.
- A corner: ride into an inside corner, round an outside corner.
- A chain: wall to wall across a gap (the Ilios well), wall to floor to wall.
- The step: a one-step ledge under the wall (what our Minecraft blocks make everywhere).

### Sending it back
- The log file from `Documents\Overwatch\Workshop\` (the newest), and which MARK was which.
- **Tell me when Overwatch is closed** before I read anything: the rule is that nothing of ours runs or looks while
  it's open.
- If the log file option isn't there: screenshots of the left-side readout during a ride, or a short screen
  recording (OBS) of the readout while riding.

### What the readout shows
`t` time, `wall` / `ground` (Is On Wall, Is On Ground), `h` / `v` horizontal and vertical speed (m/s), `alt` height
above the ground, `vel` the velocity vector, `input` (the move keys as a vector), `jump` held, `pitch`, the wall's
normal, the count of rides and marks, and the last event (ATTACH, LEAVE, LAND, MARK). The log has one line set per
tick (62.5 a second), plus the first 0.4 s of each ride and the 0.6 s after leaving a wall in detail.
