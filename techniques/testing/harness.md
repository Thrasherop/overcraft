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
- **Ability views and stands (2026-11-02, #88):** `state` reports `camera` (Minecraft's view type, then AbilityCamera's
    on / off, leaving, blend and distance): trace it in a loop to time a view's blend. `turnclips [hero]` lists the clips
    the animator holds at frame 0 as turn-in-place stands.
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
- **runClient2's harness port is fixed at 25600** (build.gradle `-Dovercraft.devPort=25600`; `OC_DEV_PORT` only tells
  `dev.py` where to connect). Two agents both using runClient2 share one port: commands from another session landed in
  Sigma's client (2026-11-02). Know which client answers before trusting a result (`state`'s player name: Viewer on run2).
- **Hero mode's crouch is `key crouch`** (Ctrl), not `key sneak`: Shift is ability 1 (Sigma's Kinetic Grasp).
- **Superflat slimes come back after a restart** even with `spawn_monsters` false (a fresh slime chunk spawns them before the
  rule runs): Sigma died to them twice at 600 health. `difficulty peaceful` while looking, `normal` only for a mob test, then
  peaceful again; and `Save and Leave` (pause menu, GUI 66 213 at 1920x1080) before killing the client, or the hero is lost.
- **Measuring knockback:** `trace start`, the hit, `trace`: the first tick's horizontal speed after the hit, the same hit on a
  control hero (Kiriko). A creeper `{Fuse:1s,ignited:1b,NoAI:1b}` 3 m off, with Resistance V so the control lives; one run in
  three shows only the tick after the push (2.70 instead of 4.95): take the highest of a few.
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
- **Heap for one client with every hero's models: 3 GB** (2026-11-02, Sigma): `JAVA_TOOL_OPTIONS=-Xmx2G` ran out of memory
  loading the models (Juno's 46 MB glTF) and the client hung on its loading screen. A fresh `run` directory opens on the
  accessibility screen: `close` it before `newworld` / `openworld`.
- **Summoned test mobs** (2026-11-02): `equipment:{head:...}` replaces a skeleton's bow (it punches: give `mainhand` a bow
  too); `attributes:[{id:"minecraft:movement_speed",base:0.0}]` keeps a mob on its mark while it still shoots (NoAI ones
  don't); husks don't burn in daylight; armour changes every number (a husk's 2 points take 1.6-4.8% per hit: Minecraft's
  armour rule), so count it in the expected value. `kill @e[type=!player]` also removes a hero's own entities (Sigma's
  barrier). `trace` also prints Sigma's new spheres (`h`) and his barrier (`b x y z state share`) per tick.
- **A ravager isn't held by `movement_speed` 0** (2026-11-02, Sigma): one walked 15 m to Sigma and killed him mid-test. Summon ravagers
  NoAI (Overcraft's own lifts and knockdowns still move NoAI mobs: they move them by hand). A `state` health read between
  steps shows it early (his health was falling before the ult went off).
- **`reload` can run the dev client out of memory** (2026-11-02, Jetpack Cat): at `-Xmx3G` a resource reload re-reads every
  hero model and died with "Java heap space" mid-reload (the window froze on the Mojang screen and every `dev` call timed
  out). After changing the pack, restart the client instead.
- **A `/tp` of a seated player dismounts them** (2026-11-02, Sigma): a "re-aim" `/tp @s ~ ~ ~ yaw pitch` after boarding a
  boat left him standing beside it and "rowing" did nothing. Aim before boarding, or with `look`.
- **`positioned over world_surface` / `motion_blocking_no_leaves` land on water** (2026-11-02, Sigma): a "dry" spot for a
  portal was the sea, and he drowned twice. Build a stone platform in the air for a test site on normal terrain.
- **A `/spawnpoint` on a non-full block is "obstructed"**: the respawn went to world spawn with "You have no home bed or
  charged respawn anchor, or it was obstructed". Use an integer position on full ground.
- **Peaceful zeroes "always"-scaling damage to players** (explosion types: Accretion's burst) and regenerates them: test
  hero-on-player damage on normal with `natural_health_regeneration` off.
- **Interact reach is from the eyes:** Sigma (eye 2.06 m) couldn't board a boat about 3 m out that Kiriko boarded from the
  same spot; from the water's edge he did. Step closer before calling it a bug.
- **Two clients need ~7 GB free** (2026-11-02, Sigma): with another agent's third client up (1.2 GB free) a restarted
  second client hung on its loading screen. Check free memory (`Win32_OperatingSystem.FreePhysicalMemory`) before
  starting the pair, and don't restart one mid-test when memory is short.
- **A `/kill` is not always neutral** (2026-11-02, kill feed): before the GENERIC_KILL rule a `/kill @e` clearing the test
  world credited the hero whose blow had landed in the last 5 s (a stray feed line and ELIMINATED), and with
  `send_command_feedback` off a `/summon` with bad NBT fails without a word. Turn feedback on while setting up. See
  [kill_feed](../hud/kill_feed.md).
- **A command run by a dead player still answers oddly** (2026-11-04): a hero who fell (a `/tp` onto a platform not filled yet) stayed on the death screen while a script went on: `/fill` said "That position is not loaded", `/tp` "worked", and `Received attachment change for unknown target` filled the log. Read `health` between steps, fill before you `/tp`, and leave the world if it happens.
- **Leaving a world takes more than 6 s to free its lock** with a busy client: `openworld` right after failed ("locked a portion of the file") and left the old state showing. Wait 12 s and check `screen`.
- **`heard` matches:** the bed sound is `ana.sleep ->`; `ana.sleep.cast` is only the shot. A dart that missed still "sounds" right if you search "sleep".
- **`git stash` is one stack for every worktree** (2026-11-04): another agent pushed theirs over mine during a build, and my `stash pop` tried to apply theirs (conflicts; pop keeps the entry on a conflict, so nothing was lost). Don't stash in a worktree: copy the files aside, and if you must, name the entry by its message, never pop the top blind.
- **`bot`: a second player with no second client** (`DevBot`, 2026-11-04): a real `ServerPlayer` on a connection to nowhere: in the player list, a hero, a team, the server's hero tick; it takes hits as a player does. `bot add <name> <hero> <team|-> x y z yaw`, `bot secondary <name> on|off` (the right button held: Brigitte's barrier; Sigma's sends his), `bot read <name>` (health, barrier, asleep), `bot asleep` (everything held by `Sleep`, mobs too: a timeline of a stun), `bot remove <name>`. Your client draws it (her model, her barrier). It proves server rules only: it can't move, fire or see. Say "bot" in a record, never "two players".
- **A second client takes the port and yours answers nothing** (2026-11-04): two clients started within seconds: the second's DevServer dies with `BindException` in *its* log and every `dev` call goes to the *other* one, whichever worktree you're in. My scripts drove another agent's client for 2.5 minutes (created a world in it, left it) before the errors said so. Check for clients in the same command that launches, then wait for `DevServer on 127.0.0.1` in your own `run/logs/latest.log` before the first command.
- **A hero can only be switched near the respawn point:** `hero <id>` far from it is ignored without a word, and a whole script ran as the wrong hero (2026-11-04). `cmd /kill @s`, `respawn`, `hero`, and read `hero` from `state` before going on.
- **An arrow moves the hero:** skeletons knocked her several metres before Catnapper's landing, and "no one in reach" looked like a fix. `/tp` back in the same batch as the confirm. And the confirm isn't the landing: 0.35 s of windup, then the dive (`bot asleep` shows the moment).
- **Command feedback off blanks the log reads** (`out`, `/data`): turn it off only for a part that reads nothing.
- **`bot` reads** (2026-11-05): `bot set <name> <health> <barrier>`, `bot antiheal`, `bot self` (my fire score, ult, energy, last crit), `bot side <type>` (what `Allies` says of the nearest one, its target and brain target). Geometry traps it found: a thrown thing leaves from the right hand, so a shot "at the barrier's edge" flies past it; a Biotic Grenade arcs; put the aim square on the middle. **Two agents waiting on the same free slot launch together:** `rl_go.sh`'s check found `BindException` three times and sent nothing; the lesson of 2026-11-04 holds.
- **`bot` (2026-11-05):** `bot press <name> <ability1|ability2|ultimate|reload|melee>` runs its kit's own press along the way it faces; `bot charge`, `bot emote`, `bot move`, `bot seen <name>` (this client's animator of it), `bot showstate on|off` (#96's rule), `bot healcut <name>`. **A bot is made in the world's game mode:** in a creative world mobs ignore it and `/damage` says "invulnerable": `/gamemode survival <name>`. Peaceful refuses `/summon` of monsters. A mob's hit with no wait: `/damage <who> 2 minecraft:mob_attack by @e[type=minecraft:husk,limit=1]`.
- **A swing is shorter than a screenshot** (2026-11-05): eight shots "0.04 s apart" were really 0.25 s each and missed a whole flail. `/tick freeze`, the press, `/tick step 2`, then step 1 and a shot, at the real 20 tps; `/tick rate 5` changes what ticks do against frames.
- **Fix a state by a dev switch when the slot is short:** the client slot went to another agent's restarts in gaps of seconds, and two launches 3 s apart still happened to me (`BindException`; killed mine by PID, sent nothing). `bot showstate off` gave the before in the fixed client. A baseline: `git worktree add --detach ../x <commit>` and build it there, while the client holds the slot, and swap.
- **Edits to CRLF files by string replace silently do nothing:** my first `bot press` "committed" nothing (the file is CRLF, the pattern LF) and it showed only as the error list with no such case. Normalise the pattern, and read the diff.
- **Bots, more (2026-11-05, [polish3](polish3.md)):** a `/tp` of a bot does nothing (its client never answers the teleport), so aim and place with `bot add` (a new bot) or `bot move`. **A bot falls:** one added in the air stands on the floor half a second later, and a "carry from the air" was underground; build it a ledge. Its cooldowns are its own: `/overcraft reset` is yours; a new bot is quickest. `bot press <name> melee` doesn't reach `QuickMelee` (that's in `HeroManager.onInput`): `bot input <name> <held> <pressed> <extra>` is the real way in (16 quick melee, held 1 primary, 2 secondary); `bot perk`, `bot strike`, `bot wall <name> <side> <yaw>`, `bot wallme`, `bot pos`, `bot fixoff on|off`.
- **A wrong key name doesn't stop a script:** `key ability2` answers `{"error": ...}`, and a script ignoring it goes on. No Suzu flew in a whole "before" and it read as a reproduction (2026-11-05). Key names are the action ids: `ability_1`, `ability_2`, `quick_melee`. Log a cooldown beside every press. And a process check by `CommandLine -match fabric.dli.config` finds its own PowerShell: filter on `java`.
- **`bot` reads and traps (2026-11-05, [polish4](polish4.md)):** `bot cc <name|me>` (asleep, silenced, glowing, anti-heal, cut, weak, phased, speed, gravity, lifted, health, the game tick), `bot held <name|me>` (the looks the server keeps for late viewers, and what this client draws), `bot fix <119|rally> on|off`. **`bot add` over a name still in use orphans the old player:** it stays in the player list and isn't a bot any more, so "me" in `cc`, `side`, `self` reads *it*, and for the rest of the session the real client read `asleep False` while it slept. Remove first; restart if it happened. **A bot's state stays with its name across remove and add** (a UUID from the name): a second dart from a new `anaE` is on the first's 14 s cooldown and silently does nothing. Fresh name per use. **Rally's raised barrier stops a Suzu** thrown past it, and the shockwave's cone stuns a Kiriko standing ahead of her: both looked like a failing cleanse. **A `bot` call waits for the next server tick:** two in a loop read every 0.1 s, so a stun "lasted 0.9 s" for 0.75; read the tick (`cc`'s `now`) and count. **A real Suzu aimed by a flat `look` at 8 m takes 0.8 s:** aim down near the ally for a short status. **`sed -i` in Git Bash turns a CRLF file into LF.** **After the critique:** `bot cc` also reads `held`, `fire`, `effects`, `stealth`, `carriedBy`, position; `bot playing [text]` lists the sounds running in this client. **A hero's phase refuses `/kill`:** a `respawn` after that swaps in a broken local player. **Launch by `scripts/p4_launch.ps1`:** it writes the commit for `TREE` lines.
- **Second pass (2026-11-05):** `bot look <name> <yaw> <pitch>` (a `/tp` doesn't turn one either), `bot path <name> <vx> <vy> <vz> <ticks>` (carried by hand, its known movement set: a flying carrier as near as a bot gets). **Fights' creeper craters stay in the world:** a later test at the same spot stood in a pit and every shot missed: move to clean ground and fill it. **A harness `respawn` after a death in another dimension left the client on a broken local player** (max health stuck, hero mode false): restart it. Check who's who in a loop variable: `fixoff on` means the fix is off.
- **Three script traps (2026-11-05, [polish4](polish4.md)):** read a tagged entity with `sort=nearest,limit=1`, never `limit=1` alone: a stale one answers, which read as a game bug twice. Each request to the client takes 0.1 s or so: put the action first and the reads after, or a 0.75 s stun is over by the time five requests have gone (the action's late row is a free control). A `/execute if entity` read needs 0.3 s in a long session: at 0.08 s it read one mob short.
- **`bot shots`, `bot sight` (2026-11-05, [polish4](polish4.md)):** `bot shots`: each Overwatch projectile as this client and as the server has it, and each deployable's hacked state. `bot sight`: from the real player's eyes to each mob near, `Combat.inSight` and vanilla's eye-to-eye. **A menu left open swallows every `key`, and a script reads on regardless:** two whole files were void by it (`screen OverwatchMenuScreen` in each `hero` log line gave it away): `p4_lib.pick` won't go on with a screen up. Grep a raw file for it before citing it.
- **More traps (2026-11-05, [polish4](polish4.md)):** a `/tp` ends a soft landing even from mid-air, so it can't stand in for a climb. Lucio holding Space rides around a free wall's end and back, so to drop him let go or crouch where the wall runs on over nothing. Vines placed in open air break themselves: back them on a wall. `bot shottrace start`, then `bot shottrace`: each projectile's z per client tick, client and server. `p4_hlp`'s TREE line reads its own worktree's stamp: in another worktree it lies.
- **`bot cc` reads `softLanding`, `climbable`, `inWater` (2026-11-06, [polish4](polish4.md)).** A ride's fall read y, ground and soft together tells a landing from a plain fall: a ground read before the bottom means the plain one. A lock stays the launcher's: `scripts/p4_stop.ps1` removes it only if its creation time is what the launch wrote.
- **`pick` kills and respawns her at spawn:** a `/fill` after it and before `tp` lands in an unloaded chunk and does nothing, with no error in the row. Five rows of a slam test read "no slam" that way (2026-11-06). Go there first, or read the block back.
- **Hero mode's crouch is `key crouch` (Left Ctrl), not `key sneak`; the world's bottom is -64: a `/fill` below it does nothing, silently (2026-11-06, [polish4](polish4.md)).**
- **`presstrace` (2026-11-06, [sojourn](../heroes/sojourn.md#the-charged-shot-on-the-press-126-2026-11-06)):** `presstrace start`, `key x down`, `presstrace`: milliseconds on one clock from the press to the input sent, the server's shot, her sounds, the look, the clip, the next frame: a "not crisp" becomes a table. `presstrace energy <n>`, `ammo <n>`. Add `PressTrace.mark` for another hero. **Traps it found:** a client slowed to 10 fps (window not active) puts 100 ms on every server-sent look: read `fps` in the row. A hero falling 8 m five times with regeneration off died, and every later row read "lost press": guard health in the loop. `emote <id>` wants its name (`triumph`), not a clip's id: a wrong one answers ok and plays nothing: read `emoteClip`. Each request costs a frame: "0.55 s after" was likely past 0.65 s. **A rotation packet followed by a payload isn't ordered as you'd think:** the payload acted on the old aim 4 of 8 times at 30 fps (ours to carry the aim in the payload).
- **`presstrace`, more (2026-11-06):** `lag <ms>`: her hero payloads held both ways, dev only, not real latency. `frames on|off`, `sample <ms>` (the client thread's stack into the log: read a hitch, don't guess it), `art` (effect art still coming), `sounding <text>`: channels actually running. **`bot playing` lists a stopped sound for 20 ticks from its start (`SoundEngine.soundDeleteTime`):** it can't show a stop. **A 1000-health target dies in a long script,** and every later "no hit" reads as a bug: re-summon it. **At a dev lag, setting energy right after a press happens before the shot spends it:** wait a ping.
