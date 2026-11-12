# Tracer critic, d0c7e1f (2026-11-12)

Up: [Tracer hub](../../../heroes/tracer.md). One round, lean budget. One dev client (`-Xmx3G`, `gradlew runClient` from this worktree),
fresh flat survival world `crit_d0c`, Overwatch / Battle.net checked closed before the launch and the archive read. Input: the
harness's key mappings (`key ability_1 down` ...), **not** the real mouse and keys (computer-use couldn't be granted the dev window).
Raw stamped log: scratchpad `critic_runs.txt` (copied below where it matters); screenshots `run/screenshots/c1a_stairs.png`,
`c1e_door.png`, `c3_after5.png`, `c4_recall_nether_end.png`, `c4_death.png`. Client closed by `/save-all` + window close (clean
shutdown, worlds saved), lock released after it exited.

## Findings, ranked

### 1. Critical: Blink stops working after a few single Blinks; the server still takes the charge
`ClientTracer.blink` (src/client/java/dev/overcraft/client/ClientTracer.java:49-61) only resets `spent` when the synced count differs
from the count it saw at the *last press*. Blink once, wait for the recharge (count back to 3), Blink again: the count is 3 again, so
`spent` keeps growing; on the 4th such Blink `charges - spent == 0` and the client refuses to move her, while it still sends
`PRESS_ABILITY_1` (`input()` ORs it in unconditionally, :44-45) and the server's `Tracer.blink` takes the charge.
- Repro (C3): open field, facing +z, Shift then wait 3.5 s, five times. Seen: z 0.5 -> 8.0 -> 15.5 -> 23.0, then **23.0, 23.0** with the
  server's count going 3 -> 2 each time. Expected: 7.5 m every press. It stays broken until the count happens to change between two
  presses (two quick Blinks). Also hit by chance in C1e (a doorway "blocked" her: it was this, the doorway works, C2a).
- Under 150 ms lag (C7, `presstrace lag 150`, four presses 0.15 s apart): **15 m moved, 3 charges spent**.
- Root: the client counts on its own and its count isn't reconciled with the server's; and a press the client refuses is still
  sent. Nothing tests it.

### 2. High: Recall across a dimension change sends her to the old dimension's coordinates (she fell out of the world)
`Tracer.record` keeps positions only (Tracer.java:55, :167); nothing clears `tracerHistory` on a level change (no `OutOfPlay` reason
for it, HeroKit.java:34), and `recall()` teleports with `connection.teleport` inside the current level (:219).
- Repro (C4): stand at (40.5, -60, 23) in the Overworld 4 s, `/execute in minecraft:the_nether run tp @s 0.5 130 0.5` (a pad above the
  roof), E 1 s later. Seen: log `Kiriko fell out of the world` 01:27:14; respawn screen shows her at (40.5, -154, 23.5): the
  Overworld's x/z/y used in the Nether. Expected: no Recall into another dimension (clear the history on a level change, or keep the
  level in each Sample and drop other-level samples). A Nether portal does the same thing (same code path; not played separately).

### 3. High: Blink can't climb stairs or slopes more than 1.5 m above where she started
`Blink.destination` caps every climb at `BLINK_STEP` over the **start** (Blink.java:39), but follows the ground *down* without limit
(:53, per step). Asymmetric, and the hub's own source is a weak reading (`m_497165F8 1.5`, L).
- Repro (C1a): oak staircase +1 per block from z=2. Shift from z 0.5: stopped at **y +1.5, z 3.2** (2.7 m of 7.5). C1b, a half-block
  slope: stopped at **y +1.5, z 4.7**. Expected (W/BZ 2020-04-14 as quoted in tracer.md: she Blinks "over" what she could step/jump
  over; Overwatch's Blink follows ramps and stairs up): along the stairs. Needs an Overwatch side-by-side up a ramp, but a hard
  1.5 m ceiling over the start can't be right for a ramp.

### 4. Medium: no sliding along walls; a diagonal Blink into a wall goes ~0 m and wastes the charge
Blink.java:35-49: a blocked step only tries going *up*, never along the wall.
- Repro (C2d): pressed against a wall on her left (hold A), then W+A+Shift: z 0.5 -> ~1.2 beyond the walk, charge spent. tracer.md:84
  (W quoting BZ 2020-04-14): she blinks "around and over obstacles". Expected: slides along the wall for the rest of the 7.5 m.
  (Straight into a wall, A alone: no move, charge spent: plausibly Overwatch's own behaviour; not flagged.)

### 5. Medium (unverified, needs a side-by-side): the Pulse Bomb's drop makes mid-range sticks impossible
`BOMB_GRAVITY = 30` (TracerStats.java:109) is an M reading of `m_599AB8D7 = v7389 30 x max(1 - v69960, 0)` with v69960 unknown
(kit_tracer_weapon.md:93-95). At 15 m/s the bomb drops 2.4 m over 6 m.
- Repro (C5): zombie 6-7 m off, aimed straight at its eyes: the bomb hit the ground at **4.2 m** (z 4.73, t6). Over 6 m you must aim
  ~2.4 m above the head. Overwatch players routinely stick at 5-8 m aimed at the body. Ask the owner for a clip (throw level at a wall
  8 m off); until then treat 30 m/s² as L, not M.

### 6. Low: code-read only (not played)
- **Lingering potion of Healing = endless health packs**: HealOrHarmMixin's `applyEffectTick` hook fires on every re-application from
  an area cloud: +50 and a Blink charge each time (HealOrHarmMixin.java:22-25, Tracer.java:603-616).
- **Pulse Bomb outlives a hero switch / G**: `leavePlay` (Tracer.java:633-641) doesn't touch a thrown bomb; it still explodes as
  Tracer's after she's become someone else (`why.forGood()` says what she placed goes on a switch).
- **Recall's phase outlives an early end**: `endRecall(…, false)` on G / switch (:635) clears invisibility but the 0.9 s
  `Combat.phase` (:195) keeps running on the next hero / Minecraft mode.
- **Recall into a spot since filled** (a block placed where she stood 3 s ago) teleports her inside it (:219), suffocating after the
  phase. Not tried; Overwatch has no equivalent (W: moved platforms send her to their new place).
- **Self-kill message**: her own bomb kills her as "Kiriko was shot" (the hitscan source used to dodge difficulty scaling, :353).
  Seen in C6.
- **Spread reaches 3.5 on the 4th shot**, not the 3rd: bloom is read before it's raised (:431-432): shots get 0, 1.17, 2.33, 3.5.
  "0 to 3.5 over the first 3 shots" (TracerStats:48) reads as reached by shot 3. Check against the data node.

## Numbers vs sources (TracerStats against tools/reference/tracer.md and tracer-map.md; no fresh wiki/BZ fetch this round)
All numbers carry a source and match the reference notes, except:
- **Temporal Regen**: TracerStats:131 uses 3 s (D = W 100 %); tracer.md Gap 1 still says "Ours: 4 s (BZ)". The doc is stale; BZ
  2026-08-11 says 50 % (= 4 s). The data reading wins per the rule, but fix the doc and say so in TracerStats (it does) and tracer.md.
- **Spread recovery**: W 0.224 s; code holds 0.2 s then 0.25 s down (0.45 s from max) (TracerStats:51, M). A 2x mismatch with W.
- **Bomb gravity 30** (finding 5) and **BLINK_STEP 1.5** (finding 3): both L in practice, used as hard rules.
- `MUZZLE_*`, `BOMB_RIGHT/UP/AHEAD`, `QUANTUM_AMMO_DECAY`, the Healing-potion pack: marked ours. Fine.

## Mutations (local, reverted; `gradlew test` TracerMathTest + hero.* + OwMathTest)
Applied together: Recall goes to the newest sample instead of the oldest (`peekLast`); Blink's headroom check on the climb removed;
headshot multiplier x1. **All tests green.** Nothing tests `Blink.destination`, Recall, or the volley; only constants and the
falloff / splash math are covered. My play would catch the first two (Recall C-runs, C1d tunnel) but not the crit change. Suggest
pure tests for `Blink.destination` on a tiny fake level (stairs, wall slide, 2-high tunnel) and for Recall's choice of sample.

## Tried and fine
Blink through a 2-high, 1-wide tunnel (1-block ceiling): 7.5 m (C1d). Through an open doorway: 7.5 m (C2a, full charges). Open field
7.5 m. E then Shift 0.05 s later: no Blink, Recall normal, cooldown 12 (C8). Pulse Bomb on a zombie walking at her from 4 m: stuck
and rode it (bomb z 3.53 -> 2.48 as the zombie went 4.05 -> 3.01), blast killed it and her (175 at < 2 m: the Overwatch rule) (C6).

## Not tried
Real mouse and keys (owed); the Nether via a real portal (only `/execute in`); lava; stairs *fight*; Recall after G / switch / rejoin
(the builder did G and respawn); Quantum / Kinetic perks; HUD beyond a glance (Blink charges and 40/40 drawn, c1e_door.png); a
second player (out of scope); the wiki / BZ refetch for this head.
