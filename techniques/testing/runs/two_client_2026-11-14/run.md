# Two-client pass, 2026-11-14 (fix/two-client)

Up: [Techniques](../../../README.md) · [harness](../../harness.md) · [hand_play](../../hand_play.md) ·
[jetpack_cat](../../../heroes/jetpack_cat.md) · [lucio](../../../heroes/lucio.md) · [reticle](../../../hud/reticle.md)

Issues #115, #89, #85, #141. Base `next` be735ef (0.3.48). Overwatch and Battle.net checked closed before each launch.
One lock holder (`.oc_client_lock`). Client 1 `runClient` ("Kiriko", host, harness 25599), client 2 `runClient2`
("Viewer", harness 25600), both with the dev pack (`OC_ASSET_PACK`). Client 1 opened each world to LAN
(`/publish false 25565`), client 2 `connect 127.0.0.1:25565`. Flat survival worlds `tc1` (old build), `tc2`-`tc4` (new),
peaceful, time 6000. A 6-high stone wall at x = -3, z 0..40.

**Input: harness only** (`KeyMapping.set` through DevServer, `screenclick`, `look`), driven from one script holding both
harness sockets (`two.py` in the session scratchpad: lines `1:<cmd>` / `2:<cmd>` / `sleep s`, so client 2's screenshots land
at known times during client 1's actions). **No real mouse or keys:** the one try at a real click (computer use, #141) was
stopped by the owner before it ran. Screenshots: client 1's in `run/screenshots/`, client 2's in `run2/screenshots/` (not
committed); raw logs `run2/c89_*.txt`, `run2/v115_after.txt`, `run2/c85_after.txt`.

## #115: others see Lúcio's wall ride
- **Old build (tc1).** Lúcio (client 1) W + Space held along the wall: his own state `wallride_left`. From the Viewer,
  8 m off at yaw -150: Lúcio drawn on the wall with the skate sparks (`v115_ride1-3`). The polish3 code (his
  `HeroInputPayload.wall` -> `LUCIO_WALL` attachment) already reached the second client.
- **Seen in numbers (new build, dev `players` now reports `wallSide`, `wallYaw` and the animator's clip of each other
  player).** Riding +z, wall on his left: the Viewer reads side -1, yaw 0, `wallride_left` at z 4.6, 7.1, 8.5 (a moving
  rider); 1.2 s after letting go, side 0, `jump_forward`. The other way (-z, wall on his right): side 1, yaw 180,
  `wallride_right` at z 35.4 and 33.5 (`v115_right1-2`).
- **Cause / fix:** none needed now: the issue predates polish3's sync, which had only been proved in two halves. Closed on
  this pass. Not tried: corners (yaw changing mid-ride), a late arrival with two real clients (polish3 did it with a bot),
  a dedicated server, his view of the Viewer.

## #89: a player on a truly flying cat's rope
- Client 1 Jetpack Cat, Shift (Lifeline on); the Viewer (Kiriko) 2 m off, prompt "F LIFELINE", F: "GRAPPLED / SPACE
  Detach" (`c89_caught`). The cat: Space 0.55 s and W 2.6 s. Both clients' `state` every 0.1 s (`c89_tow_old.txt`).
- **Expected:** the towed player trails and swings below her, no jolt or rubber band. **Seen:** lifted 0.35 s after she
  starts; trails her by 1.3-1.8 m across and 4.2-4.6 m below once she flies level at 7.1 m/s; the 2 m sideways start swings
  over and back (x 12.5 -> 9.27 -> 10.97 -> 10.44) and settles; speeds change smoothly tick to tick (vz 0.17 -> 0.36 per
  tick, no spikes). When she stops, the Viewer swings on 1.7 m past under her and back (pendulum).
- **6 m `/tp` of the cat while towing** (the old one-client test threw the player 4.5 m past and above her): the Viewer
  goes z 50.4 -> 56.1 over 1.3 s (2 m past her at most), rising to 3.3 m below her, never above, then settles
  (`c89_tp_old.txt`). No "moved too quickly" in the log.
- **Detach:** Space on the Viewer: launched vy 10.5 m/s and 8 m/s forward (`c89_detach_old.txt`), as the data. The server
  read the jump about 0.35 s after the key (state polls at 0.1 s; the hanging player slack-fell 0.65 m meanwhile).
- Views: the Viewer looking up sees her and the hook glow (`c89_lookup`); her own view shows the cyan rope down to him
  (`c89_catview`, the Viewer below the frame).
- **Cause / fix:** the rope (LifelineRope / ClientTow, #89's earlier work) behaves on a real second client; no change.
  **Not tried:** a dedicated server, an enemy's red rope, a movement ability on the rope (W: it should let go; not built),
  Catnapper's carry (still a point hold). Those move to their own issue.

## #85: the icon over a seeking Jetpack Cat for allies
- **Old build:** the Viewer 10 m from a seeking cat sees her paw marker and the hook glow, no icon (`c85_before`): the
  widget wasn't built.
- **Built:** `ReticleHud.seekingCats`: over each allied cat seeking (not towing, not the local player), Lifeline's icon on
  the ally disc, at her top + 0.7 m (5A8F #1066, template 011F) and at least 75 px above her head on screen so it clears
  her ally marker (ours).
- **First try** (0.7 m only): at 10 m the disc sat on her health pips (`c85_after`). Second: above the marker at 10 m
  (`c85_after10m`) but on the pips at 3 m (`c85_after3m`): the head was taken from her box's centre. Third (final build):
  the highest of her box's top corners, as HeroHud's marker does (tc4): the disc clear above her name and pips at 3 m
  (`c85_final3m`), 10 m (`c85_final10m`) and 25 m (`c85_final25m`); at 1 m below her, high on screen (`c85_final1m`).
- Gone while she tows (`c85_towed`); none on her own screen (`worldWidgets` ""); none for an enemy (the Viewer on red,
  she on blue: `c85_enemy`, `worldWidgets` ""); behind the Viewer: "cat behind", nothing drawn.

## #141: clicking Jetpack Cat's portrait
- `screenclick` on Lúcio's then the Cat's portrait (854x480, GUI 239,192): the preview changes to her (`s141_cat`); a
  second click plays her. From the join screen (tc2, tc3, tc4: `s141_join_cat`) and from the H menu at spawn
  (`s141_hmenu_cat`): she's picked each time. **Not reproduced.** The real-mouse check wasn't run (above). Seen on the way:
  H and the dev `hero` command do nothing 25 m from spawn ("too far": the change-hero rule), which can look like a failed
  pick.

## Lessons
- `pauseOnLostFocus:false` in both `run/options.txt` and `run2/options.txt` before a two-client pass: only one window has
  focus, and the other's pause screen eats the screenshots (every first-round Viewer shot was the pause menu).
- A backgrounded `connect` that's still running reconnects client 2 later, which reopens its select screen mid-test.
- Drive both clients from one script with a socket each; separate `dev.py` calls (or PowerShell jobs) start too late to
  catch a 1 s ride.
