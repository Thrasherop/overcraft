# Workshop scripts: watching Overwatch from inside it

Scripts for Overwatch's own Workshop (a custom game), which the owner pastes into Overwatch and plays. Nothing here
touches Overwatch's files or memory: the Workshop is a game mode, and these only read what any Workshop script can
(positions, speeds, buttons). Overcraft never runs, and nothing here reads Overwatch's files, while Overwatch is open.

Each script comes with its own instructions (how to paste it, what to play, what to send back, what it records):
- **Lúcio's wall ride:** `lucio_wallride_recorder.txt`, instructions in [lucio_wallride_recorder.md](lucio_wallride_recorder.md).
- **Jetpack Cat's flight (#60):** `jetpack_cat_flight_recorder.txt`, instructions in
  [jetpack_cat_flight_recorder.md](jetpack_cat_flight_recorder.md).
- **Every hero's movement:** `movement_recorder.txt`, instructions in [movement_recorder.md](movement_recorder.md). No hero
  forced: play any, switch freely; each line names the hero. Adds every button, ability use with its start and end,
  resources, charges and cooldowns, take-offs and landings, and key combos for an ally bot and a full ult.

Both log every tick (62.5 a second) per player, each line tagged with the player's slot (S0, S1...), plus bookmarks
(rides, boosts, abilities, F marks). Neither has been run yet: a line Overwatch rejects is the first thing to fix.
