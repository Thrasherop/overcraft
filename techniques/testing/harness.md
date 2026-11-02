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
  Not only the harness's: at a low frame rate (10 fps, the window unfocused) the client runs two ticks in one frame and
  a real quick click is lost the same way (Juno's torpedo lock, 0 of 6 taps). Since the release play (2026-11-01,
  [juno](../heroes/juno.md#step-9-the-release-play)) the server holds a fire button pressed and let go between two ticks
  for one tick (`HeroManager.onInput` / `held`): 8 of 8 taps at 3-9 fps. Other inputs read as held (jump) still can.
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
- **A `tap` can fall between client ticks** (2026-10-29): jump read as held at tick time never saw a tap, so neither
  the jump nor Lifeweaver's dash happened. Hold keys the game reads as held for a tick or two (`key jump down`,
  `sleep 0.1`, `key jump up`).
- **Real time passes between your batches:** reading screenshots between two `dev.py` calls takes tens of seconds.
  A 12 s cooldown "dropping to 0 in seconds" and a platform "vanishing" were only that time passing (its 12 s raised
  time ran out). Log a timeline inside one call (a loop of `state` reads) before calling something a bug.
- **Effects too quick to see:** `fxslow 0.2` slows the effects only; `cmd /tick rate 4` slows the game (projectiles,
  entities) too. Use both for a projectile's flight, and `fx <effect> [variant] [enemy]` to play one moment 3 m ahead.
- **Killing the dev client:** by its specific PID only (`Get-CimInstance Win32_Process` with `fabric.dli.config` in
  the command line, then `Stop-Process -Id`); pattern kills are refused.
- **Killing the client loses the hero** (2026-11-02, Sojourn): the world keeps the player's data from its last save, so
  after `Stop-Process` a rejoin's select screen offered Kiriko, not the hero just played. To test a rejoin, Save and Leave
  first (pause menu at 854x480: Save and Leave is `screenclick 59 189 1`).
- **A `/fill` over the player suffocates them:** building a test stair where she stood cost 6 health inside the stone and
  looked like a slide's fall damage (2026-11-02). Move away (`/tp`) before filling, and read health before the test.
- **Two clients (2026-10-29):** wait for the second client's title screen before `connect` (connecting while its
  resources load crashed it). After `openworld`, click READY only once the world has loaded: clicked during the loading
  screen it doesn't take, and the hero select stays up (its keys then do nothing: check `"screen"` in `state`). A
  rejoining client shows its select screen too. Keep the second client's fire pointed away from the hero under test:
  Kiriko's kunai killed Ana mid-test, and `resistance` doesn't stop Overcraft's damage.
- **A health read right after a shot can come before the hit** (2026-11-02, Sojourn): a projectile 20 m off lands a tick or
  two later, and the command's line in the log later still. Sleep a second before the read, and count from the newest line.
- **Knockback moves the hero off the mark:** a stray mob pushed Sojourn 66 m off her spot, and every "miss" after was her
  aim at empty air. `/tp @s` back at the start of each batch (2026-11-02).
- **`aim.py` with no such mob left prints the hero's own position** (it reads the newest `entity data: [` line, and the
  failed read adds none): "d=0.62" over and over meant every target was dead (2026-11-02).
- **`trace` also logs the synced ammo, reload share and meter** (`a`, `r`, `m` at the end of each line): a weapon's rate,
  reload and energy over time in one call. Since 2026-11-02 also both cooldowns, the second ammo and the ult flag (`c1 c2 s
  u`): a slide's charges and recharge, Sharpshooter's refunds, an ult's length, tick by tick.
- **A `data get` read can land on the wrong line** (2026-11-02, Sojourn): reading the newest "entity data" line after a command,
  a read that timed out left its answer to be taken by the next one (a wolf "at 20" was the villager's read). Read one thing at
  a time and check the entity's name in the line ("Wolf has the following ..."), or use `attribute <sel> <attr> get`, whose
  line names the attribute.
- **Zombies burn at noon:** a plain zombie target lost 1 health a second in the sun and spoiled a damage read; test at
  midnight (`/time set midnight`) or give them fire resistance (2026-11-02).
- **Picking a perk by script:** `overcraft perks @s 3`, then `key perk_menu down`, `key secondary_fire` (the first of a tier)
  or `key primary_fire` (the second) held 0.15 s, `key perk_menu up`; again for the major (2026-11-02).
- **One connection for a whole test:** a small driver that keeps the harness socket open and reads the game log for each
  command's answer (exec'ing a test script with `c(cmd)`, `hp(tag)`) ran every step 5-7 measurement in one call each
  (2026-11-02, Sojourn; the driver lives in that session's scratchpad, `oc.py`: worth bringing into `tools/`).
- **A `/fill` reaching below the world's floor (y -64) fails as a whole** and says so only in the chat: a pit dug "to -70" in
  superflat was never dug (2026-11-02, Sojourn). Keep fills inside -64..319.
- **A boat summoned at a pool's floor stays under the water** and F can't reach it; she just wades. Summon it at the surface
  (`/summon minecraft:oak_boat <x> <surface - 0.3> <z>`) and read its `Pos` before aiming (2026-11-02).
- **On a fresh select screen, a click on the hero already chosen plays it** ("click again to play"): the READY click after
  it lands in the world (2026-11-02).
- **`reload` can run the dev client out of memory** (2026-11-02, Jetpack Cat): at `-Xmx3G` a resource reload re-reads every
  hero model and died with "Java heap space" mid-reload (the window froze on the Mojang screen and every `dev` call timed
  out). After changing the pack, restart the client instead.
