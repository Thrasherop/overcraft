# #139: Catnapper confirmed on the ground (2026-11-13)

Up: [Techniques](../../../README.md) · hero: [Jetpack Cat](../../../heroes/jetpack_cat.md#catnapper-from-the-ground-139-2026-11-13) ·
seen in: [push_109](../push_109/push_109.md#not-settled) · lesson: [minecraft_26](../../../minecraft/minecraft_26.md)

Branch `fix/catnap-ground` from `next` 5010167 (0.3.45). One dev client (`runClient`, `OC_ASSET_PACK` the main checkout's pack),
a fresh flat survival world per build (`cat139a` base, `cat139b`-`g` the fix's versions). Keys through the harness's key
mappings (`key ultimate` = Q, `key secondary_fire` = the left button, `key jump`), `trace` per client tick (columns: tick,
horizontal m/s, (unused), vertical m/s, y, ground, keys). The cat picked by `hero` in `cat139a`, by two clicks on her tile on
the select screen in the rest. Resistance on her from `cat139b` on (see v1). Aims by `look` or `execute anchored eyes facing`.

## Cause
`JetpackCat.catnapper`, `ULT_DIVE`, ended the dive on `player.onGround()`. A ServerPlayer's `onGround` is what her client
reports (`ServerGamePacketListenerImpl.handleMovePlayer`): standing, true, so the dive landed on its first tick. A spot within
one step (1.5 m) still moved her the ticks before the distance check: the "once it moved 3 m".

## What Overwatch does
Her graph 5B7B (`E:\Overwatch Archive\research\jetpack_cat\work\g_5B7B.txt`, `assets2\outline_5B7B_proxied.txt`; read offline,
Overwatch and Battle.net closed): after the 0.35 s wait (#296, v670), `StateMoveTo` #95 to `v431 + (0, 0.25, 0)` (the target
point, 0.25 m up) at v74441 (30 m/s), under a 1.5 s wait (#78). Nothing in it, its transition (#73, v478) or its abort (#72)
reads the ground (M: not every field of the node is named). The wiki: "Dive towards a ground location", no ground rule. So
from the ground she casts and goes along it to the spot; nothing says she lifts off first.

## Fix (final, v6)
- The ground no longer ends the dive. It goes to the spot + 0.25 m (the data) at 30 m/s, laid out when the windup ends (she's
  held still then, so the server's copy of her position is right): a direction and the metres left, the last part-step, then
  the landing; on arrival she's put on the spot (`connection.teleport`, rotation kept) if it's free.
- Ours: a wall in the way ends it (her client's `horizontalCollision`, from the dive's second tick); the 1.5 s limit stays.
- `diveTarget`: a wall's hit point is stepped back off its face 0.35 m before the ray down (it started inside the wall and
  stopped at once: the spot was on the wall, 1.6 m up). Floors as before.

Each version, and why the next:
| Version | Change | Seen |
|---|---|---|
| v1 | no ground check; aim + 0.25 | ground casts go; an air landing stopped **0.93 m up** (the distance check ends a step short) |
| v2 | steer by her server position, last part-step | **overshot** the 9.2 m spot to 13.5 m (3 ticks): that position lags her client |
| v3 | laid out at the windup's end | **short**: 7.5 of 9.2 m, 24 of 25 m (her client doesn't take exactly one speed packet a tick) |
| v4 | + onto the spot on arrival | exact: 9.19, 25.0, 3.47 m; air lands at the spot; but aimed level at a wall: **climbed 2 m up its face** |
| v5 | wall hit stepped back | wall right; the floor spot 0.35 m short too (stepped back on floors) |
| v6 | stepped back on walls only | all below |

## Before (base, `cat139a`)
| Case | Seen |
|---|---|
| Ground, aim 10 down (spot 9.2 m) | **no movement**, 45 ticks at y -60; ult 37 -> 11, spent ([before_ground10](before_ground10.txt)) |
| Ground, aim 25 down (spot 3.5 m) | 2 ticks at 30 m/s, 3 m: within a step, so it landed by distance ([before_ground25](before_ground25.txt)) |

These two were rebuilt from the session's output (I deleted the raw files by a slip); the rows are as printed.

## After
| Case | Build | Seen |
|---|---|---|
| Ground, aim 10 down | v6 | 30 m/s, rising 0.82 m/s, ends at the spot: 9.19 m, y -59.75 ([v6_ground10](v6_ground10.txt)); v4 the same ([v4_ground10](v4_ground10.txt)) |
| Ground, level (spot 25 m) | v4 | 16 ticks at 30 m/s and a part-step: 25.0 m ([v4_ground0](v4_ground0.txt)) |
| Ground, aim 25 down | v4 | 3.47 m ([v4_ground25](v4_ground25.txt)) |
| Ground onto a NoAI husk 8 m off and a DevBot Tracer (team blue, she red) | v6 | husk 20 -> 18.08 and carried (mode 4); bot 14 -> 12 (25 damage) read 0.75 s after the confirm, 12.83 a second later (Minecraft's own regeneration on the bot: food), pushed ([v6_targets](v6_targets.txt), [v6_freshbot2](v6_freshbot2.txt), `c139_v4_targets.png`) |
| Ground, a 1-block wall 4.5 m ahead, aim 6 down (spot past it) | v4 | 2 ticks, stopped 1.2 m short of the wall, landed ([v4_lowwall](v4_lowwall.txt)) |
| Ground, a 4-block wall 6.5 m ahead, level | v6 | along the floor to its foot, 0.16 m short, y -59.76 ([v6_tallwall](v6_tallwall.txt)); v4 climbed it ([v4_tallwall](v4_tallwall.txt)) |
| Air (jump 1.2 s, aim 40 down) | v5 | 24.6 across, 17.2 down (as before: 24.6 / 17.1), lands at the spot y -59.75 with no slide ([v5_air](v5_air.txt)) |
| Q then Q (cancel) | v4 | mode 1 -> 0, ult kept at 100% ([v4_cancel_air_targets](v4_cancel_air_targets.txt)) |
| Air onto a husk and a zombie | v4 | landed at the spot; husk 18.08, zombie hit and carried (13.08: it burned by day) (`c139_v4_air_targets.png`) |

Screenshots in the worktree's `run/screenshots/` (not committed): `c139_after_ground10.png`, `c139_targets_carry.png`,
`c139_v4_targets.png`, `c139_v4_air_targets.png`, `c139_v5_tallwall.png`.

Also seen: a big slime left from world creation was carried after a v1 landing and killed her (16 -> 0, "slain by Slime").
Not this change: the carry takes the nearest enemy. Animals spawned by chunk generation were carried too.

## Not settled
- On arrival she can snap back up to ~1.3 m (v6 ground 10: one tick of 26 m/s at the end, her client a step past the spot).
- She lands 0.25 m up and stays there (she flies); before, from the air she ended on the ground.
- A wall stops her up to 1.2 m before it (the low wall), and the landing's area is where the server has her then.
- Not tried: a second real client being dived on; slopes and stairs (a rise toward the spot is a wall to it); water; a
  spot on a ledge above her; night. Minecraft mode doesn't run Catnapper.
