# Movement recorder for every hero (Overwatch Workshop)

Goes with `movement_recorder.txt`. One script for all the heroes: play any of them, switch whenever you like, and every
tick of every player is logged with their hero, so one session covers as many heroes as you have time for. It's a
Workshop script: it only reads what any Workshop script can (positions, speeds, buttons). Nothing touches Overwatch's
files or memory. It's built from the Lúcio wall-ride recorder's parts (which ran fine), plus a few values that one didn't
use (`Ability Charge`, `Ability Cooldown`, `Hero Of`, dummy bots): if Overwatch rejects a line, tell me which.

## Set it up
1. Open `movement_recorder.txt` and copy all of it.
2. In Overwatch: **Play → Custom Game → Create → Settings**, then the **paste** button (the clipboard icon at the top
   right of the settings screen). It sets Skirmish, no hero limit and instant respawns. No hero is forced: pick any,
   and change hero the usual way (in the spawn room).
3. **Before creating the game: Options → Gameplay → General**, in the Workshop part of the list, turn on **both**
   **Workshop Inspector** and **Workshop Inspector Log File** (the log file stays empty with only one of them on). The
   host's game writes the log to `Documents\Overwatch\Workshop\Log-<date>-<time>.txt` as you play. Moving to spectator
   and back starts a new file.
4. A map with long flat walls, open ground and some ledges: Ilios Ruins, Lijiang Night Market, Oasis City Center.

## Keys
- **F**: a numbered MARK (press it right after each try you want me to look at; note what it was).
- **Hold Crouch, then press F**: an ally bot (Ana) standing where you are, a target for Guardian Angel, Swift Step,
  Nano and the like. Make two or three in a line to fly between.
- **Hold Melee, then press F**: removes all the bots.
- **Hold Reload, then press F**: your ultimate full.

## What to play
For every hero, a minute of the basics first, then their own moves. Press F after each one.

**The basics (every hero):** these are what "crisp" is made of.
- From standing, hold W for two seconds, let go. Then the same backwards, and sideways.
- Run, then flick to the opposite direction (W to S, A to D) a few times.
- Run and stop with a tap of the opposite key.
- Jump standing, jump at a run, jump and hold a direction in the air, jump and let go of everything.
- Walk off a ledge without jumping; walk off a ledge and jump just after leaving it.
- Drop from something tall (2, 4 and 8 metres if the map has them).
- Crouch-walk, and crouch-jump.

**Then each hero's own** (the ones in Overcraft or planned for it):
- **Kiriko:** wall climb (hold Jump at a wall) to the top, and jumping off mid-climb; Swift Step to a bot; Kitsune Rush
  (run along it).
- **Lúcio:** the basics on both songs and with Amp It Up (the ground movement is what feels off now).
- **Jetpack Cat:** flight (hold right mouse) up, level, down; letting go mid-air; Lifeline.
- **Mercy:** Guardian Angel to a bot, pressing Jump (and Crouch) at different moments: before you start flying, early in
  the flight, right as you arrive, just after; Angelic Descent (hold Jump falling); Valkyrie flight.
- **Lifeweaver:** Rejuvenating Dash every direction, on the ground and in the air; Petal Platform rising with you on it.
- **Ana:** the basics, scoped (hold right mouse) and not.
- **Sombra:** Translocator thrown and used; walking and jumping in Stealth.
- **Brigitte:** Shield Bash from a walk and a run; Whip Shot.
- **Juno:** hover (hold Jump in the air), Glide Boost, landings.
- **Sojourn:** Power Slide, and Power Slide then Jump.
- **Sigma:** the basics; Gravitic Flux's float.
- **D.Va:** Boosters (straight, turning, into a wall), then out of the mech as the pilot.
- **Pharah:** hover (hold Jump), Jump Jet, Jet Dash, all of them chained.
- **Tracer:** Blink every direction, Recall.
- **Cassidy:** Combat Roll every direction.
- **Junkrat:** Concussion Mine jumps (one and two).

## Playing together
Everyone in the game is logged. Each line starts with the player's slot (**S0**, **S1**...), shown at the top left of
the readout: **note who was which slot**. The bots aren't logged. Keep a file to a few minutes per player (about 375
lines a second each): a new file every so often (spectator and back) is fine.

## Send back
- The `Log-....txt` files from `Documents\Overwatch\Workshop\` on the host's PC (or `OneDrive\Documents\...`). Not
  there? With Overwatch closed, this lists the newest Workshop logs anywhere in your user folder (PowerShell):
  `Get-ChildItem $env:USERPROFILE -Recurse -Filter 'Log-*.txt' -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 5 FullName, LastWriteTime`
- What each MARK was, with its S number (the log names the hero itself).
- Tell me when Overwatch is closed: nothing of mine runs or reads anything while it's open.

## What it records (each player, every tick: 62.5 a second)
- `S0 T`: time; on wall, on ground, in air; the hero.
- `S0 P`: position, velocity, the movement keys as a vector.
- `S0 B`: Jump and Crouch held; facing (pitch included); the nearest wall's normal and distance (eight level rays round
  you at chest height, 1.5 m; distance 9 means no wall).
- `S0 A`: primary and secondary fire held, crouching; ability 1, ability 2 and ultimate in use; ability 1 presses so far.
- `S0 K`: ability 1, ability 2, ultimate, melee and reload held; the resource meters of Jump, primary and secondary.
- `S0 C`: the resource meters of ability 1 and 2; their charges; their cooldowns.
- Bookmarks: `HERO`, `ATTACH` / `LEAVE` (walls), `TAKEOFF` / `LAND` (with air time and drop), `PRESS JUMP` / `RELEASE
  JUMP`, `PRESS CROUCH`, `PRESS ABILITY1` / `ABILITY2` / `ULTIMATE`, `ABILITY1` / `ABILITY2` / `ULT` `START` and `END`
  (with how long), `MARK`, `BOT`, `BOTS GONE`, `ULT FILL`.

The readout on the left shows the same live: slot, hero, time; wall / ground / air; speeds and height; velocity; input,
Jump, Crouch, pitch; abilities in use; resource meters; the wall; rides and marks; the last event. Everything on it is
live except the last-event line, which holds until the next event.
