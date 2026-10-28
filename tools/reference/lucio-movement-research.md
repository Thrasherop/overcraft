# Lúcio's wall ride and the movement around it: reimplementations and measurements (web research, 2026-10-27)

Web research only. Overwatch was not run, touched or read. Builds on [lucio.md](lucio.md) (wiki and patch numbers)
and [movement.md](movement.md) (Overwatch's general mover numbers, including Lúcio's air-speed fields from the data).
Everything is paraphrased; nothing is copied verbatim from a source.

Status used in the tables: **C** = confirmed (Blizzard, or two independent sources agree), **1** = one source,
**G** = guess / ours (no source gives it). Minecraft units: 1 block = 1 m, 20 ticks/s, so m/s ÷ 20 = blocks/tick.
Overwatch steps at 62.5 Hz (16 ms; [movement.md](movement.md)), so "one tick on a wall" in Overwatch is a third of a
Minecraft tick.

---

## 1. Sources

Mechanics and numbers
- **[S1] Overwatch wiki, Lúcio**, read through the MediaWiki API (wikitext, 2026-10-27):
  https://overwatch.fandom.com/api.php?action=parse&page=L%C3%BAcio&prop=wikitext&format=json
  (page: https://overwatch.fandom.com/wiki/L%C3%BAcio). Wall Ride box, hero settings table, OW1/OW2 change logs.
- **[S1b] Overwatch wiki, Movement speed**: https://overwatch.fandom.com/wiki/Movement_speed (base speeds, jump 5.72
  m/s, gravity, Wall Ride "ignores backpedal penalty").
- **[S1c] Overwatch wiki, Lúcio/Stadium**: https://overwatch.fandom.com/wiki/L%C3%BAcio/Stadium (Stadium-only powers;
  listed only so they are not mistaken for the base kit).
- **[S2] Wallriding Academy, by the Lucio Surf League** (2025). The most detailed mechanics write-up found.
  All chapters on one page: https://learn.lucio.surf/print.html. Mechanics chapter:
  https://learn.lucio.surf/setup/physics.html. Controls: https://learn.lucio.surf/setup/controls.html.
- **[S3] Patch notes** (Blizzard's text, read through the wiki's patch pages and Liquipedia's API):
  - https://overwatch.fandom.com/wiki/January_24,_2017_Patch (backwards wall ride option; via S1 change log)
  - https://overwatch.fandom.com/wiki/April_11,_2017_Patch
  - https://overwatch.fandom.com/wiki/October_17,_2017_Patch
  - https://overwatch.fandom.com/wiki/May_3,_2018_Patch (rework and two wall ride bug fixes)
  - https://overwatch.fandom.com/wiki/November_17,_2020_Patch and the PTR notes
    https://overwatch.blizzard.com/en-us/news/patch-notes/ptr/2020/10/ (as reported by
    https://www.dexerto.com/overwatch/overwatch-oct-20-patch-fixes-wrecking-ball-changes-lucio-wallride-more-1436381/)
  - Liquipedia change log: https://liquipedia.net/overwatch/L%C3%BAcio (read via its API)
  - No base-game Wall Ride change in the OW2 change logs, 2022 to 2026-07-14 (S1).
- **[S4] PCGamesN, 2017** (Geoff Goodman on the wall-jump exploit fix and the +65% compensation):
  https://www.pcgamesn.com/overwatch/overwatch-lucio-wall-jump-speed-boost
- **[S5] Blizzard forums, 2022-10-14, "Lucio wall climb mechanic question"** (players, no Blizzard reply):
  https://us.forums.blizzard.com/en/overwatch/t/lucio-wall-climb-mechanic-question-intended/720537
- **[S6] Blizzard forums, 2026-03-08, "Lucio Wall Ride Issues"** (player bug report, no Blizzard reply):
  https://us.forums.blizzard.com/en/overwatch/t/lucio-wall-ride-issues/1007698
- **[S7] Blizzard forums, 2018, design direction of wall ride / rollouts** (players on the 2018 corner rework):
  https://us.forums.blizzard.com/en/overwatch/t/blizz-clarify-the-design-direction-of-lucios-wall-ride-rollouts/72980
- **[S8] Dexerto, bunny-hopping with Lúcio**:
  https://www.dexerto.com/overwatch/overwatch-shortcuts-bunny-hopping-techniques-move-faster-lucio-774918
- **[S9] Turboboost guide** (look up while riding to gain height): https://turboboost.gg/article/how-to-play-lucio-in-overwatch
- **[S10] Dignitas wall ride guide** (corner technique: strafe toward the turn and turn the mouse with it):
  https://dignitas.gg/articles/lucio-ability-usage-and-wall-ride-guide
- **[S11] Blizzard hero page** (official text: jump onto a wall to ride along it): https://overwatch.blizzard.com/en-us/heroes/lucio/

Code (all read at the commit given; none of these repos has a permissive licence: port the ideas, not the code)
- **[C1] Lucio Surf workshop source** (OverPy), TERRORW0LF/Overwatch-Workshop-Codes, no licence:
  https://github.com/TERRORW0LF/Overwatch-Workshop-Codes/tree/64032c59ff62d7eebf6e9084937f8dca1e98ab6f/Lucio%20Surf
  (`Shared/general.opy` "Curveboost Prevention" and the speed HUD, `Shared/game_setup.opy` categories); also
  `Fastest Rollouts/rollouts.opy` in the same repo.
- **[C2] Minewatch** (Minecraft 1.12 Forge mod with a Lúcio), 2piradians/Minewatch, custom licence:
  https://github.com/2piradians/Minewatch/blob/ebbc2324c123fcafc81fdc6749cde50ea0dc5e09/src/main/java/twopiradians/minewatch/common/hero/PassiveManager.java#L171-L292
  (called from `SetManager.updateSets`, `PlayerTickEvent` phase START, i.e. before vanilla movement).
- **[C3] LucioBall remake** (Unreal Engine, C++), jjonjung/LucioBall (fork of rouge-like/LucioBall), no licence:
  https://github.com/jjonjung/LucioBall/blob/715df70828ecb65a6c2933162d753c8f9329ede3/Source/LucioBall/Private/HGH/Player_Lucio.cpp#L93-L213
- **[C4] UCSD CSE125 group 2** (C++ Titanfall-style mover with documented "Lucio-style" phases), custom licence:
  plan https://github.com/ucsd-cse125-sp26/group2/blob/80a3a5ee331b0a5dbe775e1868345ad17549fce7/docs/old/physics-future-path.md
  (phases D, E, F), constants `src/ecs/physics/TitanfallConstants.hpp`, code `src/ecs/systems/MovementSystem.cpp`.
- **[C5] fps-unity** (Unity, comment says it implements Lúcio's wall ride), billgan1024/fps-unity, no licence:
  https://github.com/billgan1024/fps-unity/blob/12fcd63ff8c6b16b802053de0eddf5c190e4f44d/Assets/Scripts/Player.cs#L160-L312
  and L400-L441 (state changes on contact).
- Seen, low value: [C6] RoninRun `LucioSurf.cs` (Unity, Titanfall-like "surf", not Lúcio's):
  https://github.com/JonTo212/RoninRun/blob/9f8909b11218f5826f7b746cf0e6f79e7d0a221a/ArcherFPS/Assets/Scripts/LegacyScripts/LucioSurf.cs ;
  [C7] Wallglider (Unity, GPL-3.0, straight walls only): https://github.com/Dendrobyte/Wallglider ;
  [C8] Vanguard (2D side-scroller Lúcio): https://github.com/Orpheon/Vanguard/blob/9d1952d4c17b81ddb5a4ad656726918d87863d3b/src/ingameelements/heroes/lucio.cpp ;
  ZJUxjy/ow_challenge (TypeScript, a stub that only sets a 0.5 timer).

Workshop codes found but **not readable** (workshop.codes sits behind a Cloudflare "verify you are human" check, which
was not attempted; Wayback snapshots hold only the description, the script loads by script from `/<code>/snippet`,
which was never archived):
- **G90BS "Everyone Can Wallride"** (Provoxin, v1.0.0, 2021): gives every hero Lúcio's wall ride. This is the one
  that would be gold. https://workshop.codes/G90BS (snapshot with description only:
  http://web.archive.org/web/20210801104146/https://workshop.codes/G90BS)
- YVE2X "Overwatch 1 Classic" (lists "Accurate Wallride" for Lúcio): https://workshop.codes/YVE2X
- QFQM0 Lúcio's Pro Skater, 8B9FC comeback practice, JETH4 wall-riding aim trainer, W8P6A Lúcio tag (a wall-ride
  energy meter), 6MDYW wall-riding practice with a velocity readout, 779JS ring race; KVKKR Lucio Surf (its source is
  C1). Reddit could not be read (blocked to scripts and to the browser pane); YouTube descriptions/transcripts could
  not be read.

---

## 2. Code and workshop finds, in porting form

### C1. Lucio Surf (the league's own workshop mode)
The only find that is tuned against the real game (it constrains the real wall ride, it doesn't replace it).
- **Categories**: Standard = 100% move speed, 100% gravity. Gravspeed = 125% move speed, 80% gravity.
- **"Curveboost Prevention" rule, per tick while on a wall**: when `Is On Wall` turns true, remember
  `SpeedWall = max(horizontal speed now, 8.53)` (10.66 in Gravspeed). Every 0.016 s while still on the wall, if the
  horizontal speed is more than 0.01 above `SpeedWall`, apply an impulse of exactly the excess, pointing back along
  last frame's horizontal motion (so it cancels the gain).
  What it tells us about the real game: on a straight wall Lúcio's speed never climbs above
  max(speed at attach, wall speed); only curved walls make it climb (the "curve boost"), and the league removes that.
  It also pins the wall speed: 8.53 = 5.5 × (1 + 0.25 + 0.30), Speed Boost song plus Wall Ride, **added**, not
  multiplied; and 10.66 = 6.875 × 1.55, so both bonuses scale with base move speed.
- **Speed HUD thresholds** (colour bands, horizontal m/s): Standard 6.88 / 11 / 13.75; Gravspeed 8.6 / 13.15 / 17.2.
  6.875 is walking with the speed song, 13.75 is twice that (the point where his air speed starts to decay).
- Measuring helpers worth copying for our own tests: speed and position sampled every other frame
  (`PosPrev/PosNow/SpeedPrev/SpeedNow`).

### C2. Minewatch (Minecraft 1.12): the existing Minecraft precedent
Axis-aligned, simple, rides at the 2019 +40% speed. Per player tick, phase START (before vanilla `travel`):
- **Can ride if**: not hacked; no "ignored" block (barrier etc.) in the 3×3 around; not in water or lava;
  `collidedHorizontally`; not creative-flying; Jump **held**.
- **Start** (not riding yet and horizontal motion ≠ 0): turn the movement input (forward/strafe and yaw, as
  `moveRelative` does) into a world vector (x, z). If `motionX == 0` (blocked on X) the wall is EAST when input x > 0
  else WEST; else if `motionZ == 0` the wall is SOUTH when input z > 0 else NORTH. Ride direction = the sign of the
  input's other axis. Only starts if this wall face differs from the last one ridden (`prevWall`, cleared on landing):
  a crude same-wall rule.
- **Each tick while riding**: if the motion along the ride direction is 0 (blocked), skip; otherwise
  `motion = wallFace × 0.2 + rideDir × (7.7 / 20) + (0, 0.07, 0)` blocks/tick, overwriting motion. The push into the
  wall keeps `collidedHorizontally` true. Direction is fixed for the whole ride (no steering).
- **Rise**: motionY is set to 0.07 before vanilla moves and then applies gravity, so he climbs 0.07 b/t = **1.4 m/s,
  uncapped**, for as long as the ride lasts.
- **On release or losing the wall**: `motion += away × 0.2 + rideDir × 0.3 + (0, 0.4, 0)` b/t (6 m/s along, 8 m/s
  up: much stronger than Overwatch's +2.5 m/s and 6.54 m/s). Their "lower gravity" option is off while riding.
- 7.7 m/s = 5.5 × 1.4: written in the +40% era (2019-03 to 2019-10). Today's value is 7.15.

### C3. LucioBall remake (Unreal): the clearest "climb, then level" rise
Per frame, only while falling:
- **Detect**: sphere overlap of radius 0.5 m at the actor's position against static world; ride if hit and Space held.
- **First frame**: store `entryZ`, the wall normal (impact normal) and the entry velocity.
- **Direction**: `along = cross(normal, up)`, flipped so `dot(entryVelocity, along) > 0`; fixed for the ride (set by
  how he arrived, no steering).
- **Velocity**: horizontal interpolated to `along × WallRideSpeed` at interp speed 100 (effectively instant); gravity
  off. **Vertical: 1 m/s up while below `entryZ + 1 m`, then 0.** So he rises at 1 m/s to 1 m above where he
  attached and then rides level.
- **Wall jump** (Jump pressed again while riding): launch with `normal × P + up × P + current velocity` (P =
  `WallJumpPower`, set in a Blueprint, not readable); gravity back on.
- **Detach**: Space released or the sphere loses the wall: gravity back on, no impulse.

### C4. CSE125 group 2: corners and a decaying entry kick
Quake units (Titanfall-inspired), so the constants are not Overwatch's, but the structure is documented as
"Lucio-style":
- **Attach gate**: airborne; horizontal speed ≥ a minimum; at least a minimum height above the ground; Jump held;
  the wish direction (WASD turned by yaw) pushes into the wall (`dot(wish, −normal) ≥ 0.1`); the wall isn't the one
  just left (re-attach to it only with enough speed into it). Ride direction = `cross(up, normal)`, flipped to match
  horizontal velocity.
- **Entry**: keep horizontal speed; add an upward kick to vertical velocity, clamped.
- **Each tick**: forward = horizontal velocity projected on the wall plane (sign kept continuous); accelerate along
  it up to a max; **vertical velocity × exp(−dt / 0.45 s)**; remove velocity into the wall. Their plan says this
  gives about 1.0 to 1.4 m of climb in the first 0.4 s, then a flat ride, matched by eye to a Lúcio video (apex
  within about 10%). The shipped constants differ from the plan's (kick 440 vs 220), so treat it as a shape, not a
  number.
- **Corners**: the attachment is an anchor point on the wall mesh, walked along the surface each tick. Crossing an
  outside (convex) edge rotates the tangent into the next face keeping the speed; an inside (concave) edge folds the
  tangent into the new face, also keeping speed; no neighbour (end of the wall) gives a small push outward and a
  fall. A face turn over 90° in one tick detaches. The normal is kept continuous so 90° turns stay attached.
- **Detach**: Jump released → wall-jump impulse (up, plus away from the wall) and exit. No look-away detach. Exit
  below a minimum horizontal speed; 5 s hard cap on one wall.

### C5. fps-unity: steering by projecting camera-relative input on the wall
The only find whose steering matches what the owner describes (W/S and the camera choose the direction):
- **Enter** (in the air, touching a contact whose normal is exactly horizontal, Jump pressed this frame): raise
  vertical velocity to at least `wallBoostSpeed`; switch to wall gravity and wall drag.
- **Each physics step on the wall**: `moveDir` = WASD turned by the camera yaw; accelerate along
  `ProjectOnPlane(moveDir, wallNormal) × wallAcc`. Looking along the wall with W, or away with S, drives him one way;
  turning the camera round with W held reverses the projection, so he slows and goes back the other way. Camera roll
  = `tilt × sin(signed angle(wallNormal, camForward))`.
- **Detach**: Jump released → `+20 × wallNormal` (velocity change) and vertical speed raised to the jump speed;
  losing the wall while riding fires the same jump automatically. Tuning values live in the scene, not read.

### Others
- C6 RoninRun: halves downward velocity next to a wall, cancels sideways input, a small stick force (5/s into the
  wall), wall bounce `(up + normal) × 15`. A Titanfall-style surf, not Lúcio's.
- C7 Wallglider: `wallParallel = cross(normal, up)`, straight walls only.

---

## 3. What the sources say, by topic

### Attaching
- Start by jumping at a nearby wall and holding Jump; he rides for as long as it is held (S1). Academy: go near a
  vertical wall and press Jump; the ride lasts until Jump is released or a drop-off happens (S2).
- Hero options (S1 table): **Toggle wall ride** (off: hold to ride; on: press to start, press to stop).
  **Allow backwards wall ride**: with it, he can start in mid-air facing any way, without looking at the wall or
  moving first; without it, only in the direction he faces, and turning backwards during a ride does not end it.
  The Academy (S2) adds: without it he only latches while moving forwards, or standing still and facing the wall.
  **Default differs**: S1 says on, S2 says off (S3 2017-01-24 added it as a new option that, when enabled, lets him
  start while moving backwards; it doesn't state the default).
  **Wall ride cares about movement**: off (default) ignores where he looks; on detaches him when he looks too far
  away from the wall.
- Surfaces: vertical walls only; curved roofs mostly not (their edges and some spots are); some map-edge walls lack
  ride hitboxes (S2). Sombra's hack prevents it (S1).
- Since 2018 he no longer loses speed when landing on a wall from certain angles (S3 2018-05-03 bug fix).
- Low ledges (knee/waist high) and jumping up onto a ledge beside a wall: **no source**.

### Rise while riding
- Players: on a long enough wall he rides at a gradual upward angle (S5, 2022); aiming up or down while riding
  changes the elevation gradually as he goes (S6, 2026); guides say look up while riding to gain height (S9).
- Blizzard fixed "Lúcio could travel straight up using Wall Ride" (S3 2018-05-03): the climb is meant to be shallow.
- **No measured number anywhere.** Reimplementations: C3 1 m/s up to +1 m above the attach point, then level; C4 an
  exponentially decaying kick, about 1.0-1.4 m in 0.4 s then level (by eye); C2 1.4 m/s with no cap.

### Steering along the wall
- No source describes the steering function. Facts: backpedalling on the wall has no speed penalty (S1b), so S runs
  at full wall speed; turning round mid-ride doesn't end it (S1 options table); by default where he looks doesn't
  detach him (S1). Mouse movement changes direction while riding without WASD (S2, said mainly of jumps and air).
- Of the code, only C5 lets input and camera choose the direction (project camera-relative WASD on the wall plane),
  which matches the owner's description. C2 and C3 fix the direction at attach; C4 keeps the velocity's direction.

### Speed on the wall
- +30% of ground speed: 7.15 m/s; 8.525 with the speed song (S1, S3 2019-10-15); additive with the song (C1's 8.53
  and 10.66). With Amp It Up: 5.5 × (1 + 0.6 + 0.3) = 10.45 (computed). Not counted toward the +75% buff cap (S1).
- History: no wall bonus before 2017-04-11 (+20%), 40% from 2019-03-19, 30% since 2019-10-15 (S3).
- Above wall speed he is slowed toward it, not at once: a one-tick ride keeps almost all speed (S2 "skim"); holding
  the wall bleeds high speed (S2 "jump"). Below it he is brought up toward it, not at once: a one-tick ride from
  walking speed doesn't reach full wall speed (S2). On a straight wall speed never rises above max(entry, wall
  speed) (C1). Rates: unknown.

### Leaving the wall, the wall jump, and the cooldown
- Releasing Jump ends the ride (a "drop-off"). A second Jump within about 0.2 s (S3 2020-11-17, 0.2 s; S2, about
  0.23 s) gives the wall jump: **+2.5 m/s horizontal** (S1, S2) and a mid-air jump. With Wall Jump On Release (on by
  default since 2020-11-17) releasing Jump fires it; Crouch drops off without it, and releasing Jump within 0.2 s
  after the Crouch still jumps (S3). Without the option, a drop-off without a second press just falls (S2).
- Vertical speed of the wall jump: **6.54 m/s** looking straight ahead, **6.9** if forward was held beforehand,
  **4.12** looking down, **9.12** looking up; looking up or down gives almost no horizontal boost (S1). Looking ahead
  gives the most speed; up is higher and slower, down is flatter and slower (S2). A wall jump with no momentum (a
  "null jump") goes as high as looking straight up (S2). A plain ground jump is 5.72 m/s (S1b).
- Direction of the jump: where he faces, dragged back toward his current motion (a blend of the two); too steep an
  angle between them slows him drastically (S2).
- **0.5 s cooldown** after jumping off a wall, on every wall (S1 box, S2). **Same wall**: needs about 2 m between
  the drop-off point and the next attach point (S2; S3 2018 says "far enough"). A "wall" is one connected straight
  surface; a curve is many small walls, so they can be chained with no gap (S2).
- History: 2017 fix for an exploit (mashing Jump on a wall gave a super boost), then the post-ride boost +65%
  (2017-10-17); Goodman said the vertical part was not raised as much, so less lift than before (S4).

### In the air
- His air speed does not decay above 5.5 m/s until it passes **twice his ground speed** (11 m/s healing song,
  13.75-13.76 speed song, 17.6 amped) (S1; C1's 13.75 threshold agrees). Our data reading in
  [movement.md](movement.md): Lúcio's max air speed field is 11, his decay field 0.5/s against 2.0 for others.
- The 0.5 s cooldown plus that decay caps a chain at about **16 m/s** (standard, speed song) and about 21 m/s in
  Gravspeed (S2). Holding anything but W lowers his ground speed and so his air cap (S2).
- There is no timed "post-jump buff" in the base game: the +2.5 m/s is an impulse that lasts because his air speed
  isn't decayed below the cap. (Stadium powers add timed ones, S1c; Heroes of the Storm's 1.5 s is another game.)

### On the ground
- Landing and running loses the extra speed almost at once; jumping on the frame of landing keeps most of it (S8).
  About ten perfect bunny hops take him from top speed to walking speed (S2). Walking off a ledge stores one jump that
  doesn't count as touching the ground (S2).

### Corners, curves, ends
- Since 2018-05-03 he rides round inside and outside corners without leaving the wall, and Jump must be held for a
  minimum time before he can ride round a corner (S3; time not given). Uneven wall geometry no longer knocks him off
  at random (S7).
- A corner throws him off (a drop-off) depending on its **angle** (sharper is worse; two corners close together
  count as one larger angle) and the **speed** he takes it at (faster is worse; reached mostly by attaching just
  before the corner, since riding slows him to wall speed) (S2). No threshold angle in any source.
- He rounds corners smoothly if nothing is in the way and he keeps line of sight with the wall (S1 strategy).
- Corner climb: alternate between the two walls of an inside corner while looking up; 270° (outside) corners also
  work with care. Pole climb works because a round surface is many walls (S2).
- Small ridges in wall detail act as separate walls and can slow or detach him; players report unwanted boosts at
  wall ends (S6). What exactly happens at the end of a wall: **no source** (S2 treats every drop-off alike).

### Why chaining walls keeps or builds speed ("surf")
Each wall jump adds +2.5 m/s horizontally; his air speed is not decayed until twice his ground speed; holding a wall
bleeds speed down to wall speed, so the fastest chain touches each wall for one tick and jumps at once ("skim");
the 0.5 s cooldown and the decay above the cap set a ceiling (~16 m/s); touching the ground bleeds it. Geometry
redirects momentum: rounded corners give more than +2.5 m/s (curve boost), a slanted ceiling turns upward speed into
forward speed (ceiling boost), sliding down a steep non-walkable slope gains speed (ramp slide), slamming into an
upward slope turns speed into height (ramp jump), and a flat ceiling cancels upward speed (head hit) (S2, C1).

---

## 4. Merged mechanics table

| Mechanic | Value / behaviour | Source | Status |
|---|---|---|---|
| Start | in the air near a wall, Jump pressed or held; rides while held | S1, S2, S3 | C |
| Toggle option | press to start, press to stop (off by default) | S1 | 1 |
| Surfaces | vertical walls only; most curved roofs not | S2 | 1 |
| Facing to start | backwards option on: any way; off: moving forward or still and facing the wall | S1, S2, S3 2017 | C (default disputed: S1 on, S2 off) |
| Looking away while riding | doesn't detach by default; an option detaches "too far away" (angle not given) | S1 | 1 |
| Attach angle keeps speed | yes (2018 bug fix) | S3 | C |
| Low ledge / ledge-top avoidance | not found | none | G |
| Wall speed | ground × 1.30: 7.15 m/s (0.3575 b/t); speed song 8.525 (0.426 b/t); amp 10.45 | S1, S3 2019-10-15, C1 | C |
| Backpedal penalty on wall | none (S at full wall speed) | S1b | 1 |
| Speed cap | wall bonus not counted toward +75% | S1 | 1 |
| Over wall speed on wall | eased down to wall speed, not instantly; rate unknown | S2 | 1 |
| Under wall speed on wall | eased up to wall speed, not instantly; rate unknown | S2 | 1 |
| Gain on a straight wall | never above max(entry, wall speed) | C1 | 1 |
| Rise while riding | gradual upward angle on long walls; follows aim up/down; not straight up | S5, S6, S9, S3 2018 | C (behaviour), no number |
| Rise numbers | 1 m/s to +1 m then level (C3); ~1-1.4 m in 0.4 s, exp τ 0.45 s (C4); 1.4 m/s uncapped (C2) | reimplementations | G |
| Steering | direction along the wall from camera-relative input (W/S plus camera) | owner; C5 model | G (structure from C5) |
| Release Jump | ends the ride (drop-off) | S1, S2, S3 | C |
| Wall jump window | 0.2 s (S3) / ~0.23 s (S2) after the drop-off | S2, S3 | C |
| Wall Jump On Release | on by default; jump fires on release; Crouch drops without it | S1, S3 2020 | C |
| Wall jump horizontal | +2.5 m/s (0.125 b/t) | S1, S2 | C |
| Wall jump vertical | 6.54 ahead, 6.9 with W held, 4.12 looking down, 9.12 looking up (m/s) | S1 | 1 |
| Wall jump with pitch | up: higher, slower; down: flatter, slower; ahead: fastest | S1, S2 | C |
| Wall jump direction | facing blended with current motion; steep angle slows hard | S2 | 1 |
| Null jump | no-momentum wall jump = look-up height | S2 | 1 |
| Re-attach cooldown | 0.5 s, any wall | S1, S2 | C |
| Same wall | ≥ ~2 m between drop-off and new attach; a wall = one straight surface | S2, S3 2018 | C (behaviour), 1 (number) |
| Corners | inside and outside ridden since 2018; min Jump hold before cornering (time unknown) | S3 | C |
| Corner drop-off | grows with angle and speed; threshold unknown | S2 | 1 |
| Line of sight | must keep it with the wall to round corners | S1 | 1 |
| Air speed | not decayed until 2× ground speed (11 / 13.75 / 17.6) | S1, C1 | C |
| Air decay rate | Lúcio 0.5/s, others 2.0/s (data field, units inferred) | movement.md | 1 |
| Peak chain speed | ~16 m/s standard (speed song), ~21 m/s Gravspeed | S2 | 1 |
| Post-jump boost duration | no timer: kept in the air under the cap, lost on the ground | S1, S2, S8 | C (by combination) |
| Ground contact | bleeds speed quickly; ~10 perfect bhops from max to walk | S2, S8 | C |
| End of a wall | not found | none | G |
| Sombra hack | prevents wall riding | S1 | 1 |

---

## 5. What stayed unknown
- **Rise while riding**: rate, cap, and how strongly it follows pitch. No measurement exists online; only
  reimplementation guesses (C2, C3, C4).
- **Steering response**: how fast he reverses on the wall when the input flips, what happens holding W while facing
  straight into the wall, the angle at which "cares about movement" detaches.
- **On-wall acceleration and deceleration rates** toward wall speed.
- **Corner threshold** (angle × speed) and the minimum Jump hold before cornering.
- **Attach geometry**: reach, minimum wall height, low ledges, how a jump onto a ledge avoids grabbing the wall.
- **End of a wall**: drop-off with the 0.2 s window, or something else.
- Per the owner's real-data rule these should be looked for next in the pulled game data (DataTool, offline):
  [movement.md](movement.md) already found Lúcio's own air fields in the mover graphs, so his wall-ride graph may hold
  the rise, acceleration and corner values. The G90BS "Everyone Can Wallride" script needs a person to pass
  workshop.codes' human check and copy the snippet.

## 6. A per-tick sketch for Overcraft (ours, from the above)
Marked with the source of each line; anything unmarked is ours and should be replaced by data when found.
```
attach if: airborne, jump held (S1), not on cooldown (0.5 s, S1/S2), not hacked (S1),
           horizontal contact with a vertical face (S2), face != last wall within ~2 m of the drop-off (S2),
           [backwards option off: moving forward or still and facing it (S2)]
           [ours: min wall height and a free space above, to avoid grabbing low ledges]
on attach: keep horizontal speed (S3 2018); [rise: start a small upward velocity, eased out (C3/C4 shape)]
each tick: t = wall tangent; dir = sign(dot(camera-relative WASD, t)) (C5 model, owner);
           keep current direction when the input has no component along t (ours);
           target = t * dir * groundSpeed * (1 + 0.30 + songBonus) (S1, C1);
           speed eases toward target from above and below (S2), never above max(entry, target) on a flat wall (C1);
           push gently into the wall; cornering: rotate t into the new face keeping speed (C4),
           drop off if turn angle x speed is too high (S2)
on release (or Crouch): drop-off; within 0.2 s of it (S3), or at once with Wall Jump On Release (S1):
           +2.5 m/s horizontal along the facing/motion blend (S1, S2), vertical 4.12..9.12 by pitch (S1),
           start the 0.5 s cooldown (S1)
in the air: no decay up to 2x ground speed (S1)
```
