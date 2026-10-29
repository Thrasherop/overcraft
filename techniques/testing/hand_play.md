# Hand play: playing it like a player

CLAUDE.md requires playing every change in the real game before claiming it. This is how that play works and what it
catches. Up: [Techniques](../README.md) · the tools: [harness](harness.md) · looking closely: [visual_review](visual_review.md)

## Catching what screenshots miss
- **Reproduce the owner's report first** (stuck on a step only when strafing; Alex in Minecraft mode), then show the fix
  with the same steps. Where possible, also show the fix switched off still fails (a dev toggle: `clothfloor off`,
  `emotef5 off`).
- **Bursts of screenshots** across an animation (0.05-0.3 s apart) and side-by-side sheets: one screenshot hides most
  animation bugs.
- **Step a frozen game a tick at a time to catch a one-frame glitch:** `cmd /tick freeze`, then `cmd /tick step 1`
  and a screenshot, 60-70 times (one animation cycle), and a thumbnail sheet of them all. The leg kick at the end of
  the breath clip lasted one tick in 64. Random screenshots missed it for hours; the stepped sheet showed it at once.
- **Freeze the game for fast effects** the same way: the effects run on the game clock, so they stop with it. The
  harness's `fx <effect>` plays one effect alone.
- **When a glitch shows up "at random" under different actions, look for something that runs underneath all of
  them.** The leg kick was blamed on three different clips; it was the idle breathing layer, broken every 3 s. Bursts
  of 40+ screenshots across a whole loop catch periodic glitches.
- **A dev readout beats screenshots for "is it even running?"** The coat's lowest point, stuck at its initial value,
  found the cloth clock bug in one try ([cloth](../animation/cloth.md)). A dev counter of bad frames found F5's
  one-frame flash. A per-tick fix can lose to the frame: fix it at the source.
- **A "feel" report (floaty, mushy) becomes numbers with a per-tick trace**: `trace start`, hold keys with
  `press key.keyboard.w down` / `up`, then `trace` (horizontal speed, vertical speed, height, on ground). Run the same
  script on the old build and the new one. For the camera, find the sky's edge in a few screenshots a frame apart: a
  still horizon in every frame means no bob.
- **Measure frame rate as part of play**:
  - `state` has `fps`;
  - `timing reset` then `timing` gives our milliseconds a frame;
  - `timing stride N` draws every Nth triangle.

  Scale the suspect (triangles, effects) and watch the frame time: it shows whether the cost is ours (CPU) or
  downstream (upload, GPU).
- **Suspect the test before the mod**: white pixel squares round a healed villager were Minecraft's death poofs from
  the villager the test script had just killed; burning "mobs" on the horizon were zombie horses in daylight.

## A second player
`gradlew runClient2` (a second dev client, harness port 25600), then `/publish false 25565` on the first and `connect
localhost:25565` on the second. See the hero as others do:
- third-person animation;
- the sounds others hear (subtitles on);
- effects;
- ally markers on a real player.

Only the client drawing the change needs a restart for client-side work. Server-side changes restart the host, and
the second client rejoins.

## Real input
With desktop control, when the owner approves it at the PC. Computer-use screenshots show the game as blank: capture
the screen with GDI instead (scratchpad `burst.ps1`, timed bursts for fast animations). It can't send F5 or two held
keys at once, and Windows' text-input overlay can block clicks.

To bring the dev client forward for real keys (2026-10-28): `(New-Object -ComObject WScript.Shell).AppActivate(<pid>)`
with its PID (found as for killing it). `open_application` and SDL's raise didn't get past Windows' focus lock, and
clicking the taskbar needs a separate grant. Then `hold_key` reaches the game; read the result with `dev state` and the
game's own screenshots. Mouse moves can't turn the view: SDL reads raw mouse input and the tool only places the
cursor. Turning with the real mouse still needs the owner.

## Test worlds
- **Summon test mobs** with `Tags:["x"]` and select `@e[tag=x]`.
- **While setting up,** use `/difficulty peaceful` (a slime killed the test once). Gamerules are snake_case in 26.3
  (`send_command_feedback`).
- **Damage tests:** a full hunger bar heals behind your back. Turn off `natural_health_regeneration`, or read health
  within a second of a hit.
- **Nights in a survival world swarm the harness,** which reacts a second late. Fight one summoned mob at a time
  (`/gamerule spawn_mobs false`); keep wild nights for the general play.
- **Zombies burn in daylight and iron golems kill them:** fight on a platform at sea, at night or with a helmeted
  zombie.
- **Superflat:** the player's feet at y -60 stand on blocks at y -61.
- **Build test floors with the floor first and the air above it second**, and only where the chunk is loaded (a fill
  "not loaded" does nothing, and she fell to her death). Clearing first drops the player a tick before the floor goes
  in. A floor in the sky (`fill` at y 119, standing at 120) makes a clean backdrop for frames.
- **A plain vanilla server** for "the mod stays invisible" checks: [minecraft_26](../minecraft/minecraft_26.md#a-plain-vanilla-server).
