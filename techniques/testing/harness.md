# The dev harness

`tools/dev.py` talks to the dev client's DevServer (127.0.0.1:25599; runClient2 on 25600 with `OC_DEV_PORT`);
`tools/devplay.sh` adds `dev_wait_title`, `dev_open <world>` and `dev`. Up: [Techniques](../README.md) · playing:
[hand_play](hand_play.md) · looking: [visual_review](visual_review.md)

## Commands worth knowing
- **Moving and pressing:**
  - `key <action> down|up|tap` goes through Minecraft's key mappings.
  - `press key.keyboard.escape` sends a real key event through the KeyboardHandler, which works even with the
    desktop locked.
  - `screenclick x y [button]` clicks the open screen at GUI coordinates; `hover x y` puts the game's cursor there.
  - `mouse dx dy` goes through Minecraft's own `turnPlayer`, so it reaches the emote camera and the wheel.
- **Watching:**
  - `state` reports position, health, hero, ammo, the playing clip (`anim`), the emote clip and its time, the coat's
    lowest point, fps.
  - `screenshot <file>`, `window 1920 1080`, `perspective first|back|front`, `subtitles on|off`, `heard` (the sounds
    played since the last call).
- **Emotes:** `emote <id>`, `emoteat <s>|off` (every emote held at that time), `emotef5 on|off`. The wheel by hand:
  `key comms down`, `key primary_fire` (right click: the emote page), `mouse 0 -150` (the top slot), `key comms up`.
- **Effects:** `fxslow <share>` (0.08: about twelve times slower; `fxslow 1` back), and `state` reports `fx`, the
  live particles. `fxnear [x y z] r` lists the live ones near a point (texture, shape, age, size, alpha): the
  quickest way to tell "never spawned" from "spawned and not seen" (2026-10-28: Catnapper's rings were never spawned,
  its glows were spawned and hidden behind her head).
- **Cloth and rendering:** `clothfloor on|off`, `timing`, `shadows on|off`, `reload` (resource packs: a regenerated
  data file without a restart).
- **World:** `newworld`, `openworld`, `cmd <command>`, `respawn`, `chat <text>`, `connect host:port`.
- **Window:** `window 1280 720`; `focus` restores and raises it (SDL), though Windows may still keep it behind (see
  [hand_play](hand_play.md#real-input)).
- **Live knobs for anything placed by eye:** `held`, `heldmax`, `heldscale`, `swing`. Put the numbers in a public
  static array with a command that sets them, try each axis both ways in one batch of screenshots, then write the
  winners in as the defaults.

## Traps
- **Taps are shorter than a tick:** `key <action> tap` sets and clears within one frame, so code that reads `isDown()`
  once a tick never sees it. A double ping needs two holds of 0.1 s. Hold it: `down`, a 0.15-0.2 s sleep, `up`.
- Holding attack doesn't mine unless the window has the mouse (`continueAttack` checks `isMouseGrabbed`). Minecraft
  drops mouse movement while its window isn't active.
- **Mouse buttons are numbered from 1 in 26.3:** `screenclick x y 1` is a left click, `3` right; `0` falls through and
  does nothing. A container slot takes the click only once `hover x y` has put the cursor on it.
- **`look` and an ability in the same tick act on the old aim** (the server hasn't had the new rotation): `look`,
  `sleep 0.2`, then the key.
- **A screenshot is written a moment later:** sleep before reading the file.
- **A command's result in the log arrives a moment later:** count the lines before running it and wait for a new one.
  Mind game timings too: a wooden button pops back out after 1.5 s, a target's signal after 8 ticks.
- Minecraft's key for Enter is `key.keyboard.enter`; a wrong name is silently ignored.
- `/publish false 25565` opens a world to LAN (`publish false survival 25565` fails to parse).
- **Screen coordinates are GUI units:** at 1920x1080 (scale 4) the hero select's READY is `screenclick 240 244`. At
  854x480, the pause menu's Disconnect is `screenclick 213 174`.
- **A click that confirms something needs to be held** (`key attack down`, 0.1 s, `up`): the server reads held
  buttons once a tick, and a tap's down and up can both arrive between two ticks (Catnapper's confirm was missed).
- **`secondary_fire` is the left button:** the action names follow Kiriko (her secondary, the kunai, is on the left).
  For Jetpack Cat and Lucio the left button is the primary fire, so `key secondary_fire` confirmed a Catnapper meant
  to be cancelled (2026-10-28). Q cancels it.
- **Toggles toggle:** a second Q cancelled the aim the first started. Know the state before pressing a toggle again.
- **Aiming at something:** `py -I tools/aim.py <entity type> <height above its feet>` (with `OC_LOG` set to the game's
  log) prints the yaw and pitch from the eyes to the nearest one, for `look`.
- **Commands run by a dead player find no entities** (`kill @e[...]` says "No entity was found"): clear the world only
  while alive, and check it worked (`execute as @e run say left`). Leftover skeletons from one test shot the hero in the
  next, which looked like a respawn at half health (2026-10-28).
- **One connection at a time:** a script holding the harness socket open makes any other `dev` call (`tools/aim.py`)
  wait for its 40 s timeout. Do everything over the one connection (2026-10-29: a night fight stalled while
  zombies killed both players).
- **"At a bed" means the respawn point:** H in hero mode opens the select only near it, and a bed placed with
  `/setblock` isn't one until used. `/spawnpoint @s <bed>` stands in for clicking it.
- **NoAI mobs can't be moved at all:** a towed NoAI villager stayed put while the rope said towing. Tow and carry
  tests need mobs with AI, and those walk: a zombie walked under her and out of Catnapper's 5.5 m reach. Move the mob
  onto the mark in the same batch as the confirm.
- **Scripted aim lags moving mobs:** `aim.py` reads positions about 0.4 s before the shot, so a strafing skeleton is
  missed; a scripted night fight measures nothing about a kit. Use still (NoAI) targets for numbers.
- **Test worlds spawn mobs:** a superflat world has slimes; a hero hovering in place was killed twice mid-test. Turn
  spawning off and clear the world before measuring. In 26.3 the rule needs its namespace from a command:
  `cmd /gamerule minecraft:spawn_monsters false` (plain `spawn_mobs` was refused, "Incorrect argument"; 2026-10-29).
- **`respawn` only on the death screen:** on a living player it swaps in a new local player the server never made.
  The old one froze mid-fall, took no input (an ability press did nothing) and its model vanished; the log says
  "Received attachment change for unknown target". Check `health` is 0 first; else leave the world and rejoin
  (Lifeweaver, 2026-10-29).
- **Fall damage needs survival:** a world made in creative is creative again on reopening unless set; a "fixed" fall
  read 20 -> 20 in creative and proved nothing. Set `cmd /gamemode survival` in the same batch and check a plain fall
  still hurts (`cmd /tp @s ~ ~12 ~`) as the control.
- **Placed things stay:** a Petal Platform lies on the ground until used, and his key pressed again destroys it rather
  than throwing a new one. Before a throw, know whether one is out (`data get entity @e[type=...]`).
- **Killing the dev client:** by its specific PID only (`Get-CimInstance Win32_Process` with `fabric.dli.config` in
  the command line, then `Stop-Process -Id`); pattern kills are refused.
