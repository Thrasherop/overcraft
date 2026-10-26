# Overwatch movement: the real numbers, and why Overcraft feels floaty

Research for the owner's report (2026-10-26): "Overcraft feels quite... floaty when compared to actual overwatch ... a bit
closer to minecraft than overwatch." Research only, nothing in the code changed.

Sources: the archived build 154088 read offline (Overwatch and Battle.net checked closed before every run), the
Overwatch wiki through its MediaWiki API, Blizzard developer posts and community Workshop measurements. Confidence:
**H** = official or read straight from the game data and matching a measurement, **M** = community measured, or a data
field whose meaning is inferred from strong evidence, **L** = inferred.

## Verdict

The jump arc is fine: Overcraft's jump already matches Overwatch to within one 50 ms tick. (Plain Minecraft's jump is
actually *snappier* than Overwatch's: 32 m/s² against 17.5.) What feels floaty is everything around the jump:

1. **The ground response is 2-3 times slower than Overwatch's.** Overwatch reaches full speed in about 0.05 s and
   reverses a strafe in about 0.06 s; ours takes 0.1 s and 0.2 s.
2. **Minecraft's camera bob, and FOV changes with speed buffs, are still on in hero mode.** Overwatch's camera does
   neither.
3. **Air control is weaker than Overwatch's**: 12 m/s² here against about 19 m/s².
4. **Long falls are slower**: ours eases toward 30 m/s along the wrong curve.
5. **Minecraft's 20 Hz tick adds latency**: input is read every 50 ms and the camera is drawn up to one more tick
   behind. Overwatch simulates every 16 ms.

## Overwatch's numbers

| Quantity | Overwatch | Source | Conf. |
|---|---|---|---|
| Simulation rate | 62.5 Hz (16 ms ticks) | Derek Mulder (Blizzard engineer) [5]; Workshop rates use /62.5 [8] | H |
| Run speed (forward and sideways) | 5.5 m/s (Tracer, Genji, Anran 6.0) | wiki [1]; data `m_CFD3E635`/`m_19452F15` | H |
| Backward / diagonal back | 4.95 m/s (90%) / 14/15 | wiki [1]; data `m_61D6B4E9` = 4.95 | H |
| Crouch speed | 3.0 m/s in every direction | wiki [1]; data `m_B470B8DA`, `m_AA6F4CE1`, `m_0DDD18C3` | H |
| Ground acceleration | 35 x wish speed = **~190 m/s²**: 0 to 5.5 m/s in **~0.05 s** | data `m_DAA0618B` = 35 (units inferred, below); "almost instantaneous, 0.1 s = 2 network frames" (20 Hz Workshop log) [6]; "acceleration is instant" [9] | M |
| Ground stop (no input) | friction 15/s, stop speed 4.5 m/s: 5.5 m/s to 0 in **~0.08-0.1 s** | data `m_E0A0FE25` = 15, `m_E91D1664` = 4.5; "OW2: almost all heroes stop in 0.1 s, except Lúcio and Echo" [6] | M |
| Strafe reversal (+5.5 to -5.5 m/s) | **~0.064 s** (friction and acceleration together) | simulated from the two rows above | M |
| Lúcio / Echo | friction 2 / 3, acceleration 15: stop in 0.6 / 0.4 s | data; "Lúcio stops in 0.6 s" [6]; "lower friction ... skates" [2] | M |
| Air acceleration | 3.5 x wish speed = **~19 m/s²**: 0 to 5.5 m/s sideways in 0.29 s, full reversal in 0.58 s (inside one jump) | data `m_E631BA84` = 3.5 (field confirmed by the Jetpack Cat patch value 9.5 [10]; units inferred) | M (field) / L (units) |
| Air speed above max | speed over 5.5 m/s decays in the air (rate 2/s); Lúcio's threshold is 11 m/s | wiki Lúcio [2]; data `m_031FE7EE` = 5.5 (Lúcio 11), `m_4E03E892` = 2.0 (Lúcio/Echo 0.5) | M / L (rate) |
| Air drag without input | 0.35/s (Jetpack Cat's flight 8.0) | data `m_8895A9C8` | L |
| Jump | **5.72 m/s up, apex 0.98 m** (~0.336 s to apex, ~0.67 s on flat ground) | wiki, Workshop measured [1]; matches 62.5 Hz stepping with g = 17.5 exactly | M |
| Jump field in the data | 6.2 for every hero (7.5 on three story NPCs) | data `m_6F25AA23` | L (meaning unsure) |
| Gravity (Workshop "Set Gravity" 100%) | **17.5 m/s²** at the start of a fall | wiki [1]; data `m_C2C40969` = 1.75 (x 10 = `m_6B7FDB90`); Jetpack Cat 0.3, flying enemies 0 | H |
| Fall curve | **v = 30·tanh(17.5·t/30)**, that is dv/dt = 17.5·(1 - (v/30)²) | fits the wiki's measured graph [1] within 1% at 1, 2, 3 and 4 s, for all four gravity settings; reaches 29.97 at 6.4 s ("about 6.4 s to max") | H |
| Terminal fall speed | 30 m/s, the same at any gravity | wiki [1]; data `m_B74245E8` = 30 on all 126 movers | H |
| Fall damage | none | wiki [1] | H |
| Air control mechanics | one fixed acceleration on the ground, one in the air; since 2018 a turn converts forward velocity into sideways velocity without slowing (no "brake, then strafe"); strafing against knockback works the same way | Geoff Goodman, 2018-01-17 [4] | H |
| Bunny hop | no gain: jumping keeps the run speed, air acceleration only heads toward the input speed, and extra speed decays | [2], [4], data | M |
| Max walkable slope | 45° | data `m_AEBAD1B0` | L |
| Step height | 0.5 m? (Torbjörn, Zenyatta, Ramattra 0.3) | data `m_08FFAA0A` | L |
| Camera | no walking bob and no speed FOV (only the first-person arms sway) | common knowledge, no written source found: **the owner can confirm at a glance** | M |

Units: `m_DAA0618B` = 35 can't be a plain m/s². That would take Lúcio (15) 0.37 s to reach full speed, but the
Workshop log measured every hero, Lúcio included, at about 0.1 s [6]. Read Source/Quake-style (accelerate x wish speed
x dt), 35 gives 0.05 s and Lúcio 0.08 s. The same reading keeps Jetpack Cat's flight consistent: air accel 9.5/s is
above her 8/s air drag, so she reaches her speed. As plain m/s² she would top out near 1.2 m/s. And the friction 15,
stop speed 4.5 pair reproduces Lúcio's measured 0.6 s stop exactly. So air 3.5 means about 19 m/s². If it were a
plain 3.5 m/s², Overwatch would have almost no air control, which contradicts Goodman's "simply short hold or tap it"
for small air adjustments [4].

Other community numbers seen: a gravity of 14 m/s² (Novikov [6]; his sampling was 20 Hz, and it conflicts with the
wiki's 17.5 and the data). Jetpack Cat's air acceleration went from 7.7 to 9.5 in August 2026 [10], and the data holds
9.5.

## Where this is in the game data

Every hero's gameplay entity (`STUHero.m_gameplayEntity`, e.g. Kiriko `000000003A46.003`) has a
**`STUCharacterMoverComponent`** (TankLib type 0x6EFDBD73). It holds 45 floats with hashed names. They were read for
all 126 entities that have one (54 heroes), and their meanings were inferred from which heroes differ and from the
abilities that override them. Abilities replace the whole component from statescript (a `STU_ED2B22BD` entry with a
priority in a state node), which is how the field meanings were confirmed:

- **Jetpack Cat** flight (graph `0000000059DC.01B`, priority 99): air accel 9.5 (= the patch note's value), max air
  speed 12, air drag 8.0, flag `m_6DB41D17` = 1 (flying). Her base mover has gravity scale 0.3. Graph `59E0`
  (priority 100): speed 6.0 (= patch "5.5 to 6") and air accel 6.0; priority 101: max air speed 16 and air accel 2.0
  (a fast dash).
- **Echo** Flight (`1BB6`): every speed 8.0 (= wiki "8 m/s in Flight"), air accel 10, max air speed 9.
- **Mercy** (`10DA`): air accel 8.0. **Kitsune** fox (`33DB`): friction, `m_4E03E892` and `m_8895A9C8` all x2.5.

| Offset | Field | Default | Varies | Reading | Conf. |
|---|---|---|---|---|---|
| 120 | `m_1972C687` | 2.0 | tanks 2.3, Winston/Sigma 2.5, Torbjörn 1.7 | capsule height (m) | M |
| 124/136 | `m_7A70E3E3`/`m_915FF368` | 0.5 | most tanks 0.7 | capsule radius | L |
| 132 | `m_03E04168` | 1.25 | Sigma 2.05, Domina 1.73 | crouch height? | L |
| 160 | `m_AEBAD1B0` | 45 | none | max walkable slope (°) | L |
| 164 | `m_08FFAA0A` | 0.5 | Torbjörn/Zenyatta/Ramattra 0.3 | step height? | L |
| 168/172/176 | `m_CFD3E635`/`m_61D6B4E9`/`m_19452F15` | 5.5/4.95/5.5 | Tracer, Genji, Anran 6/5.4/6; Echo Flight 8 | forward/back/strafe speed | H |
| 180-188 | `m_B470B8DA`... | 3.0 | none on heroes | crouch speeds | H |
| 192 | `m_DAA0618B` | 35 | Lúcio, Echo 15 | ground accelerate (x wish speed) | M |
| 196 | `m_E631BA84` | 3.5 | Echo Flight 10, Mercy 8, Jetpack Cat 9.5/6/2 | **air accelerate** (x wish speed) | M |
| 204 | `m_E0A0FE25` | 15 | Lúcio 2, Echo 3, fox 37.5 | ground friction (/s) | M |
| 208 | `m_6F25AA23` | 6.2 | NPCs 7.5 | jump? (measured 5.72) | L |
| 212 | `m_4E03E892` | 2.0 | Lúcio, Echo 0.5 | decay of air speed above max (/s) | L |
| 216 | `m_142DA8D4` | 0 | Doomfist, Venture 0.1; fliers 1.5-3 | control above max speed? | L |
| 220 | `m_031FE7EE` | 5.5 | Lúcio 11; Echo Flight 9; Jetpack Cat 10-16 | max air speed before decay | M |
| 224 | `m_8895A9C8` | 0.35 | Jetpack Cat 8/2/1.25, fox 0.875 | air drag (/s) | L |
| 232 | `m_C2C40969` | 1.75 | Jetpack Cat 0.3, flying enemies 0 | gravity scale (x 10 m/s²) | H |
| 236 | `m_B74245E8` | 30 | none | terminal fall speed | H |
| 252 | `m_E91D1664` | 4.5 | none | stop speed (for friction) | L |
| 292 | `m_D51C467D` | 50 | none | unknown acceleration-like | L |

Full per-field listing: `E:\Overwatch Archive\research\movement\mover_fields_by_hero.txt`; overrides:
`mover_overrides.txt`; raw values: `movers.json`; scripts: `scripts\` (`movers.py` reads every hero's mover through
the kiriko-anim `owinit.py`, which refuses to run while Overwatch or Battle.net runs; `crawl2.py` dumps a hero's
graphs; `overrides.py` lists the overrides; `sim.py` the timings below). A static string scan of the archived
`Overwatch.exe` found nothing about hero movement (only a rigid-body physics library's field names).

## Overcraft against Overwatch (OwMovement.java and what Minecraft adds)

Timings from `sim.py` (Overwatch's model at its 62.5 Hz; Overcraft at 20 Hz):

| Aspect | Overwatch | Overcraft now | Effect |
|---|---|---|---|
| Ground start 0 to 5.5 | 0.048 s | 0.10 s (`GROUND_ACCEL` 55, 2 ticks) | 2x slower |
| Ground stop | 0.08 s | 0.10 s | fine |
| Strafe reversal | 0.064 s | **0.20 s** (4 ticks) | 3x slower: ADAD feels mushy |
| Air: stop / reverse | 0.29 / 0.58 s | 0.46 / 0.92 s (`AIR_ACCEL` 12) | can't reverse within one jump (0.7 s) |
| Air above 5.5 m/s | decays (2/s) | kept unless the input pulls it down | knockback and boosts carry longer |
| Jump apex / airtime | 0.98 m, 0.336 / 0.672 s | 0.98 m, 0.35 / 0.70 s (14 ticks) | **fine** (within a tick) |
| Rising gravity | 17.5 | 16.7 (`GRAVITY_UP`) | no difference at 20 Hz ticks |
| Fall speed after 0.5 / 1 / 2 s | 8.5 / 15.8 / 24.7 m/s | 7.6 / 13.3 / 20.7 m/s (`gravity()` linear easing) | floaty drops |
| Fall 3 / 10 / 20 m | 0.59 / 1.10 / 1.61 s | 0.62 / 1.19 / 1.77 s | +5 to 10% hang time |
| Camera bob | none | Minecraft's `bobView` at full amplitude (`bob` = min(0.1, speed) is capped already at hero speed) | Steve-like bounce and roll on every step |
| FOV | fixed | `getFieldOfViewModifier`: (speed attribute / 0.1 + 1) / 2, so Kitsune Rush +30% widens the FOV x1.15 | Minecraft speed-FOV |
| Tick / input | 16 ms | 50 ms input sampling, plus the camera lerping one tick behind (`partialTick`) | ~50 ms extra delay on every start, stop and jump |
| Crouch at a ledge | walks off | stops at the edge (`Player.maybeBackOffFromEdge`, since hero crouch is Minecraft's shift) | Minecraft-ism |

The Minecraft code checked (26.3, decompiled): `LivingEntity.aiStep` (jump, noJumpDelay, input), `travel`/
`travelInAir` (fully replaced by the mixin, so Minecraft's friction and drag don't run), `Player.updateBob`,
`GameRenderer.renderLevel`/`bobView` (only the arms' call is redirected by `HandBobMixin`; the world camera's is
not), `AbstractClientPlayer.getFieldOfViewModifier`, `Camera.tickFov`, `Player.maybeBackOffFromEdge`.

## Ranked causes and fixes

1. **Ground response (M-H).** Use the data's Source-style ground model in `travel()` on the ground. First friction:
   `drop = max(speed, 4.5) * 15 * dt`, applied every ground tick. Then acceleration: `add = wishSpeed - dot(v, wishDir)`,
   and if it is positive, `v += wishDir * min(35 * wishSpeed * dt, add)`. At 20 Hz this gives full speed in 1 tick,
   reversal in 1 tick and a stop in 2 ticks, the same as Overwatch at Minecraft's resolution. Keep 5.5 as the
   ceiling (no gain). The cheaper version: `GROUND_ACCEL` from 55 to about 190 m/s² (35 x 5.5) for input, with no
   input still stopping in 2 ticks.
2. **Camera (M; the owner can confirm Overwatch has neither in seconds).** In hero view, skip `bobView` in
   `GameRenderer.renderLevel` too (the arms already skip it). Return 1.0 from `getFieldOfViewModifier`, so speed
   buffs don't change the FOV. Optionally skip the damage tilt of `bobHurt` too (Overwatch doesn't tilt on hits).
3. **Air control (M for the field, L for the units).** `AIR_ACCEL` from 12 to **19.25** (3.5 x the input speed),
   keeping today's model (approach the input velocity and never exceed the faster of current and input speed), which
   matches Goodman's description. Add: horizontal speed above 5.5 decays at 2.0/s in the air. Optional (L): 0.35/s
   air drag with no input. Ask the owner to check once in Overwatch (Workshop script below) before tuning further.
4. **Fall curve (H).** `gravity()` when falling: `GRAVITY_DOWN * (1 - (v/30)²)` in place of `(1 + v/30)`. The
   `fallSpeedTendsToTerminal` test still holds (30 m/s). Rising can stay 16.7 with 5.72 (identical in ticks), or
   become 17.5 with 5.86 m/s (also 0.98 m and 14 ticks) to match the data's single gravity.
5. **20 Hz latency (M, structural).** Constants can't fix this. One option, to try by hand: in hero view, draw the
   local camera one tick ahead (`pos + (pos - oldPos) * partialTick` in place of the lerp from `oldPos`). That
   removes 50 ms of visual delay, at the risk of the camera poking up to 0.275 m into a wall at full speed (disable it
   while `horizontalCollision`). Worth an experiment after 1-4, not before.
6. **Small things (L).** Hero crouch stops at ledges (override `isStayingOnGroundSurface` in hero controls).
   Overwatch's step height may be ~0.5 m against Minecraft's 0.6 (no change needed).

Each fix needs the hand play in CLAUDE.md: strafe ADAD on flat ground, jump-strafe, drop off 3-, 10- and 20-block
ledges, a Kitsune Rush, against zombies, with the old build for comparison.

## Measuring air acceleration in Overwatch (for the owner, Minecraft closed)

In a Workshop custom game (Practice Range), paste this, then run forward, jump, and hold S right after take-off. The
message gives the speed and the time it took to stop it. If the reading above is right, 5.5 m/s stops in about 0.29 s
(19 m/s²). If the units are plain m/s², it won't stop before landing (3.5 m/s² needs 1.6 s).

```
variables
{
	player:
		0: t0
		1: v0
		2: armed
}

rule("air brake start")
{
	event
	{
		Ongoing - Each Player;
		All;
		All;
	}

	conditions
	{
		Is In Air(Event Player) == True;
		Z Component Of(Throttle Of(Event Player)) < -0.5;
		Horizontal Speed Of(Event Player) > 4;
		Event Player.armed != True;
	}

	actions
	{
		Event Player.t0 = Total Time Elapsed;
		Event Player.v0 = Horizontal Speed Of(Event Player);
		Event Player.armed = True;
	}
}

rule("air brake end")
{
	event
	{
		Ongoing - Each Player;
		All;
		All;
	}

	conditions
	{
		Event Player.armed == True;
		Dot Product(Velocity Of(Event Player), Direction From Angles(Horizontal Facing Angle Of(Event Player), 0)) <= 0;
	}

	actions
	{
		Event Player.armed = False;
		Small Message(Event Player, Custom String("{0} m/s stopped in {1} s, still in air: {2}", Event Player.v0,
			Total Time Elapsed - Event Player.t0, Is In Air(Event Player)));
	}
}
```

Divide the speed by the time for the air deceleration. A result that lands ("still in air: False") doesn't count.
The same rules with `Is In Air == False` in place of `True` measure braking on the ground (S against a forward run:
about 0.03 s expected).

## Sources

1. Overwatch wiki, Movement speed (jumping and falling sections and the falling-speed graph added 2024-04-19 by Taqx2,
   Workshop measured): https://overwatch.fandom.com/wiki/Movement_speed (read through
   `https://overwatch.fandom.com/api.php?action=parse&page=Movement_speed&prop=wikitext&format=json`); graph
   `File:Falling_speed_graph.png` (copy in `E:\Overwatch Archive\research\movement\`).
2. Overwatch wiki, Lúcio (air speed above 5.5/11 m/s; "lower friction than the rest of the cast"):
   https://overwatch.fandom.com/wiki/L%C3%BAcio. Also Common Terms ("Air Control"), Wrecking Ball ("deceleration while
   airborne is lower"), November 13, 2018 Patch ("Made jump height consistent across hero roster"; Tracer accelerated
   slower and decelerated faster than others, fixed).
3. Overwatch wiki, Echo (Flight 8 m/s): https://overwatch.fandom.com/wiki/Echo
4. Geoff Goodman, "hero movement changes" (2018-01-17), quoted in full at
   https://www.icy-veins.com/forums/topic/35453-hero-movement-changes-detailed/
5. Derek Mulder, "Overwatch simulates (or ticks) every 16 milliseconds, or at 62.5Hz":
   https://us.forums.blizzard.com/en/overwatch/t/new-feature-high-precision-mouse-input-gameplay-option/422094
6. Boris Novikov, Workshop measurements (20 Hz log): https://medium.com/@dnnkeeper/recreating-overwatchs-player-movement-in-unity-b7c5d1aa8873
   (Russian original: https://dtf.ru/gamedev/3459694-remeik-overwatch-v-unity)
7. Workshop docs, Set Gravity ("percentage of regular movement gravity"): https://workshop.codes/wiki/articles/set-gravity
8. Workshop docs, Start Accelerating (Rate/62.5; horizontal speed capped at walking speed):
   https://workshop.codes/wiki/articles/start-accelerating
9. Blizzard forums, "Workshop code for movement acceleration" (players: acceleration is instant):
   https://us.forums.blizzard.com/en/overwatch/t/workshop-code-for-movement-acceleration/844611
10. Jetpack Cat, "Air acceleration increased from 7.7 to 9.5" (August 2026):
    https://www.dexerto.com/wikis/overwatch/all-hero-buffs-and-nerfs-in-overwatch-season-4/
